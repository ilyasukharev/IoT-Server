package com.iotserv.dto

import kotlinx.serialization.Serializable

@Serializable
data class SocketConnectionResponseData(
    val message: String,
    val token: String? = null,
)

@Serializable
data class ControlDeviceData(
    val id: ULong,
    val deviceName: String,
    val deviceDescription: String,
    val sensorsList: List<String>,
    val statesTypesList: List<String>,
)

data class DeviceDefinitionData(
    val deviceName: String,
    val deviceDescription: String,
    val countDeviceSensors: UInt
)

data class DeviceStructureData(
    val deviceId: ULong,
    val sensorName: String,
    val sensorStateType: String,
)

data class UserDeviceData(
    val userId: ULong,
    val deviceId: ULong,
    val state: String,
)
