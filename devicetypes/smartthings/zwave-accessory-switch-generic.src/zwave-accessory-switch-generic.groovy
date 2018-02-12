/**
 *  Copyright 2018 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition(name: "Z-Wave Accessory Switch Generic", namespace: "smartthings", author: "SmartThings", ocfDeviceType: "oic.d.switch", runLocally: true, minHubCoreVersion: '000.019.00012', executeCommandsLocally: true) {
		capability "Actuator"
		capability "Health Check"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Light"

		fingerprint mfr: "001A", prod: "4441", model: "0000", deviceJoinName: "Eaton RF Accessory Dimmer"
		fingerprint mfr: "001A", prod: "5352", model: "0000", deviceJoinName: "Eaton RF Accessory Switch"
	}

	// simulator metadata
	simulator {
		status "on": "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"
	}

	// tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		main "switch"
		details(["switch", "refresh"])
	}
}

def installed() {
	// Device-Watch simply pings if no device events received for checkInterval duration of 32min = 2 * 15min + 2min lag time
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	response(refresh())
}

def updated() {
	// Device-Watch simply pings if no device events received for checkInterval duration of 32min = 2 * 15min + 2min lag time
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
}

def getCommandClassVersions() {
	[
		0x20: 1,  // Basic
		0x56: 1,  // Crc16Encap
		0x70: 1,  // Configuration
	]
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, commandClassVersions)
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	if (getDataValue("MSR").equals("001A-4441-0000")) {
		// Eaton dimmer sends unsolicited basic report with wrong value
		// so we need to ignore it for device to work properly.
		[:]
	} else {
		// please update type if any other accessory switch device uses unsolicited BasicReport
		[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
	}
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	log.debug "manufacturerId:   $cmd.manufacturerId"
	log.debug "manufacturerName: $cmd.manufacturerName"
	log.debug "productId:        $cmd.productId"
	log.debug "productTypeId:    $cmd.productTypeId"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	updateDataValue("manufacturer", cmd.manufacturerName)
	createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	def versions = commandClassVersions
	def version = versions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		getSwitchStateZwaveCommand()
	])
}

def off() {
	// take dimmer ramp time into account for accessory dimmer
	// if more accessory dimmers are added, this MSR check should
	// be changed to some additional data value flag which can be set
	// in ManufacturerSpecificReport handler method
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		getSwitchStateZwaveCommand()
	], getDataValue("MSR").equals("001A-4441-0000") ? 5000 : 100)
}

def poll() {
	refresh()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	refresh()
}

def refresh() {
	def commands = []
	commands << getSwitchStateZwaveCommand()
	if (getDataValue("MSR") == null) {
		commands << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(commands)
}

def getSwitchStateZwaveCommand() {
	//Eaton Accessory switch 001A-5352-0000 could use indicatorGet
	if (getDataValue("MSR").equals("001A-4441-0000")) {
		//Eaton dimmer
		zwave.switchMultilevelV1.switchMultilevelGet().format()
	} else {
		//most devices should be able to handle this
		zwave.basicV1.basicGet().format()
	}
}
