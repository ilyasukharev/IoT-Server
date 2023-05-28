package com.iotserv.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommonDeviceData(
    val deviceId: Long,
    val deviceName: String,
    val deviceDescription: String,
    val boardId: String
)

@Serializable
data class DetailDeviceData (
    val deviceId: Long,
    val deviceName: String,
    val sensorsState: HashMap<String, String>,
)

@Serializable
data class ChangeDeviceData (
    val boardId: String,
    val sensor: String,
    val state: String,
)

@Serializable
data class BoardIdData (
    val boardId: String
)

@Serializable
data class ClientManagementResponseData (
    val msg: String? = null,
    val deviceInfo: DetailDeviceData? = null,
    val deviceListInfo: List<CommonDeviceData>? = null,
)