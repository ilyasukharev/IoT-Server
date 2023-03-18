package com.iotserv.dao.device_structure

import org.jetbrains.exposed.dao.id.LongIdTable

object DeviceStructureTable : LongIdTable("device_structure") {
    val deviceId = long("device_id")
    val sensorName = varchar("sensor_name", length = 35)
    val sensorStateType = varchar("sensor_state", length = 35)
}