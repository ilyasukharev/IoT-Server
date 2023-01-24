package com.iotserv.dao.device_structure

import org.jetbrains.exposed.sql.Table

object DeviceStructureTable : Table("device_structure") {
    private val noteId = ulong("note_id").autoIncrement()
    val id = ulong("device_id")
    val sensorName = varchar("sensor_name", length = 35)
    val sensorStateType = varchar("sensor_state", length = 35)

    override val primaryKey = PrimaryKey(noteId)
}