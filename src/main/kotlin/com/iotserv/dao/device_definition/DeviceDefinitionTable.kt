package com.iotserv.dao.device_definition

import org.jetbrains.exposed.dao.id.LongIdTable

object DeviceDefinitionTable : LongIdTable("device_definition") {
    val deviceName = varchar("device_name", length = 35)
    val deviceDescription = varchar("device_description", length = 255)
    val countDeviceSensors = integer("count_device_sensors")
}