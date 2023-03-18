package com.iotserv.dto

import kotlinx.serialization.Serializable

@Serializable
data class SocketConnectionResponseData(
    val message: String,
)

@Serializable
data class BoardConnectionData (
    val boardIdentificationData: BoardIdentificationData? = null,
    val controlDeviceData: ControlDeviceData? = null,
)
@Serializable
data class BoardIdentificationData(
    val boardUUID: String
)

@Serializable
data class ControlDeviceData(
    val deviceName: String,
    val deviceDescription: String,
    val sensorsList: List<String>,
    val statesTypesList: List<String>,
)

data class DeviceDefinitionData(
    val deviceName: String,
    val deviceDescription: String,
    val countDeviceSensors: Int
)

data class DeviceStructureData(
    val deviceId: Long,
    val sensorName: String,
    val sensorStateType: String,
)


data class UserDeviceData(
    val userId: Long,
    val deviceId: Long,
    val state: String,
    val boardId: String
)
