package com.iotserv.dao.device_definition

import com.iotserv.dto.DeviceDefinitionData
import com.iotserv.utils.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

interface DeviceDefinitionManagement {
    suspend fun addNewDevice(data: DeviceDefinitionData): ULong?
    suspend fun isExists(deviceName: String): Boolean
    suspend fun getDeviceId(deviceName: String): ULong?
}

class DeviceDefinitionManagementImpl : DeviceDefinitionManagement {
    override suspend fun addNewDevice(data: DeviceDefinitionData): ULong? = dbQuery {
        DeviceDefinitionTable.insert {
            it[deviceName] = data.deviceName
            it[deviceDescription] = data.deviceDescription
            it[countDeviceSensors] = data.countDeviceSensors
        }.resultedValues?.singleOrNull()?.get(DeviceDefinitionTable.id)
    }

    override suspend fun isExists(deviceName: String): Boolean = dbQuery {
        DeviceDefinitionTable.select {
             DeviceDefinitionTable.deviceName eq deviceName
        }.limit(1).singleOrNull()?.let{true} ?: false
    }

    override suspend fun getDeviceId(deviceName: String): ULong? = dbQuery {
        DeviceDefinitionTable.select {
            DeviceDefinitionTable.deviceName eq deviceName
        }.limit(1).singleOrNull()?.get(DeviceDefinitionTable.id)
    }
}