package com.iotserv.dao.users_devices

import org.jetbrains.exposed.dao.id.LongIdTable

object UserDevicesTable : LongIdTable("user_devices") {
    val userId = long("user_id")
    val deviceId = long("device_id")
    val state = varchar("device_state", length = 255)
    val boardId = varchar("board_id", length = 40)
}