/**
 *  Copyright 2017 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Cooper 5-Scene Keypad", namespace: "smartthings", author: "SmartThings") {
		capability "Actuator"
		capability "Refresh"
		capability "Sensor"
		//zw:L type:0202 mfr:001A prod:574D model:0000 ver:2.05 zwv:2.78 lib:01 cc:87,77,86,22,2D,85,72,21,70
		fingerprint mfr: "001A", prod:"574D", model:"0000", deviceJoinName: "Cooper 5-Scene Keypad"
	}

	tiles(scale: 2) {
		standardTile("state", "device.state", width: 4, height: 2) {
			state "connected", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2, backgroundColor: "#00a0dc") {
			state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		childDeviceTiles("outlets")

		main "state"
	}
}

def installed() {
	log.debug "Installed $device.displayName"
	addChildSwitches()
	def cmds = []
	//Associate hub to groups 1-5, and set a scene for each group
	//Device will sometimes respond with ApplicationBusy with STATUS_TRY_AGAIN_IN_WAIT_TIME_SECONDS
	//this can happen for any of associationSet and sceneControllerConfSet commands even with intervals over 6000ms
	//As this process will take a while, we use controller's LED indicators to display progress.
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 0).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:1, sceneId: 1).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 1).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationSet(groupingIdentifier:2, nodeId:[zwaveHubNodeId]).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:2, sceneId: 2).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 3).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:3, sceneId: 3).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 7).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationSet(groupingIdentifier:4, nodeId:[zwaveHubNodeId]).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:4, sceneId: 4).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 15).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationSet(groupingIdentifier:5, nodeId:[zwaveHubNodeId]).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:5, sceneId: 5).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 31).format())
	cmds << new physicalgraph.device.HubAction(zwave.manufacturerSpecificV2.manufacturerSpecificGet().format())
	//use runIn to schedule the initialize method in case updated method below is also sending commands to the device
	sendHubCommand(cmds, 3100)
	runIn(50, "initialize", [overwrite: true])  // Allow set up to finish and acknowledged before proceeding
}

def updated() {
	// If not set update ManufacturerSpecific data
	if (!getDataValue("manufacturer")) {
		runIn(48, "initialize", [overwrite: true])  // installation may still be running
	} else {
	//If controller ignored some of associationSet and sceneControllerConfSet commands
	//and failsafe integrated into initialize() did not manage to fix it,
	//user can enter device settings and press save until controller starts to respond
	//correctly
		initialize()
	}
}

def initialize() {
	if (!childDevices) {
		addChildSwitches()
	}
	def cmds = []
	//Check if Hub is associated to groups responsible for all five switches
	//We do this, because most likely some of associationSet and sceneControllerConfSet commands were ignored
	//As this process will take a while, we use controller's LED indicators to display progress.
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 0).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationGet(groupingIdentifier:1).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfGet(groupId:1).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 1).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationGet(groupingIdentifier:2).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfGet(groupId:2).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 3).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationGet(groupingIdentifier:3).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfGet(groupId:3).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 7).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationGet(groupingIdentifier:4).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfGet(groupId:4).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 15).format())
	cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationGet(groupingIdentifier:5).format())
	cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfGet(groupId:5).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 31).format())
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: 0).format())
	//Make sure cloud is in sync with device
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorGet().format())
	if (!getDataValue("manufacturer")) {
		cmds << new physicalgraph.device.HubAction(zwave.manufacturerSpecificV2.manufacturerSpecificGet().format())
	}
	//Long interval to make it possible to process association set commands if necessary
	sendHubCommand(cmds, 3000)
}

def refresh() {
	def cmds = []
	// when using All Off feature, controller will often not respond to Indicator Get command
	// using delayed refresh proves more reliable in this case
	cmds << new physicalgraph.device.HubAction("delay 500")
	cmds << new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorGet().format())
	//Indicator returns number which is a bit representation of current state of switches
	sendHubCommand(cmds, 500)
}

def parse(String description) {
	def cmd = zwave.parse(description)
	if (cmd) {
		zwaveEvent(cmd)
	}
	return []
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	if (cmd.manufacturerName) {
		updateDataValue("manufacturer", cmd.manufacturerName)
	}
	if (cmd.productTypeId) {
		updateDataValue("productTypeId", cmd.productTypeId.toString())
	}
	// productId 0 is a valid value
	if (cmd.productId != null) {
		updateDataValue("productId", cmd.productId.toString())
	}
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	if (cmd.nodeId.any { it == zwaveHubNodeId }) {
		sendEvent(descriptionText: "$device.displayName is associated in group ${cmd.groupingIdentifier}")
	} else {
		// We're not associated properly to this group, try setting association two times
		def cmds = []
		//Set Association for this group
		cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:[zwaveHubNodeId]).format())
		cmds << new physicalgraph.device.HubAction(zwave.associationV1.associationSet(groupingIdentifier:cmd.groupingIdentifier, nodeId:[zwaveHubNodeId]).format())
		sendHubCommand(cmds, 1500)
	}
}


def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	if (cmd.value == 0) {
		//Device sends this event when any switch is turned off
		//Most reliable way to know which switches are still "on" is to check their status
		refresh()
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
	//we do not support dimming duration
	setSwitchState(cmd.sceneId, "on")
}

def zwaveEvent(physicalgraph.zwave.commands.indicatorv1.IndicatorReport cmd) {
	//cmd.value (0-31) is a binary representation of current switch state
	//switch 1 - first bit
	setSwitchState(1, (cmd.value & 1)? "on" : "off")
	//switch 2 - second bit
	setSwitchState(2, (cmd.value & 2)? "on" : "off")
	//switch 3 - third bit
	setSwitchState(3, (cmd.value & 4)? "on" : "off")
	//switch 4 - fourth bit
	setSwitchState(4, (cmd.value & 8)? "on" : "off")
	//switch 5 - fifth bit
	setSwitchState(5, (cmd.value & 16)? "on" : "off")
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStartLevelChange cmd) {
	//Not supported
	//We have no way to set and/or retrieve multilevel state of each button
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd) {
	//Not supported
	//We have no way to set and/or retrieve multilevel state of each switch
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationBusy cmd) {
	//we have no way of knowing which command was ignored
}

def zwaveEvent(physicalgraph.zwave.commands.scenecontrollerconfv1.SceneControllerConfReport cmd) {
	if (cmd.groupId != cmd.sceneId) {
		//Scene not set up properly for this association group. Try setting it two more times.
		def cmds = []
		cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration: 0, groupId: cmd.groupId, sceneId: cmd.groupId).format())
		cmds << new physicalgraph.device.HubAction(zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration: 0, groupId: cmd.groupId, sceneId: cmd.groupId).format())
		sendHubCommand(cmds, 1500)
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "Unexpected zwave command $cmd"
}

//method created to make child class reusable
void childOn(deviceNetworkId) {
	def switchId = deviceNetworkId?.split("/")[1]
	//this may override other local switch states if cloud is out of sync
	updateLocalSwitchState()
}

//method created to make child class reusable
void childOff(deviceNetworkId) {
	def switchId = deviceNetworkId?.split("/")[1]
	//this may override other local switch states if cloud is out of sync
	updateLocalSwitchState()
}

private setSwitchState(switchId, state) {
	String childDni = "${device.deviceNetworkId}/$switchId"
	def child = childDevices.find{it.deviceNetworkId == childDni}
	if (!child) {
		log.error "Child device $childDni not found"
	}
	//send event only if state changed
	if(child?.device.currentState("switch")?.value != state) {
		child?.sendEvent(name: "switch", value: "$state", descriptionText: "$child.displayName was switched $state")
	}
}

private updateLocalSwitchState() {
	def binarySwitchState = 0;
	def multiplier = 1;
	for (int i = 1; i <= 5; ++i) {
		String childDni = "${device.deviceNetworkId}/$i"
		def child = childDevices.find{it.deviceNetworkId == childDni}
		if (child?.device.currentState("switch")?.value == "on") {
			binarySwitchState += multiplier
		}
		multiplier *= 2
	}
	sendHubCommand new physicalgraph.device.HubAction(zwave.indicatorV1.indicatorSet(value: binarySwitchState).format())
}

private addChildSwitches() {
	for (i in 1..5) {
		String childDni = "${device.deviceNetworkId}/$i"
		def child = childDevices.find{it.deviceNetworkId == childDni}
		addChildDevice("Child Switch", childDni, null,
				[completedSetup: true, label: "$device.displayName switch $i",
				 isComponent: true, componentName: "switch$i", componentLabel: "Switch $i"])
	}
}
