package com.iotserv.dao.device_definition

import org.jetbrains.exposed.sql.Table

object DeviceDefinitionTable : Table("device_definition") {
    val id = ulong ("device_id").autoIncrement()
    val deviceName = varchar("device_name", length = 35)
    val deviceDescription = varchar("device_description", length = 255)
    val countDeviceSensors = uinteger("count_device_sensors")

    override val primaryKey = PrimaryKey(id)
}