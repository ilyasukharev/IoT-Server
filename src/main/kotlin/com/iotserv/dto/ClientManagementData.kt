package com.iotserv.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommonDeviceData(
    val deviceId: ULong,
    val deviceName: String,
    val deviceDescription: String,
)

@Serializable
data class DetailsDeviceData (
    val deviceId: ULong,
    val deviceName: String,
    val sensorsState: HashMap<String, String>,
)

@Serializable
data class ChangeDeviceData (
    val deviceId: ULong,
    val sensor: String,
    val state: String,
)

@Serializable
data class ClientManagementResponseData (
    val msg: String,
    val deviceInfo: DetailsDeviceData? = null,
    val deviceListInfo: List<CommonDeviceData>? = null,
    val token: String? = null
)