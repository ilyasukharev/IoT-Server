package com.iotserv.dao.users_devices

import org.jetbrains.exposed.sql.Table

object UserDevicesTable : Table("user_devices") {
    private val noteId = ulong("note_id").autoIncrement()
    val userId = ulong("user_id")
    val deviceId = ulong("device_id")
    val state = varchar("device_state", length = 255)

    override val primaryKey = PrimaryKey(noteId)
}