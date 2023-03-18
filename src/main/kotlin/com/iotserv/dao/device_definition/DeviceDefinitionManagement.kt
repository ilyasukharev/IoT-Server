package com.iotserv.dao.device_definition

import com.iotserv.dto.DeviceDefinitionData
import com.iotserv.exceptions.ExposedException
import com.iotserv.utils.DatabaseFactory.dbQuery
import com.iotserv.utils.RoutesResponses.deviceWasNotFound
import com.iotserv.utils.RoutesResponses.deviceWasNotFoundCode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere

interface DeviceDefinitionManagement {
    suspend fun addNewDevice(data: DeviceDefinitionData): Long
    suspend fun isExists(deviceName: String): Boolean
    suspend fun getDeviceId(deviceName: String): Long
    suspend fun removeDevice(deviceId: Long): Boolean
    suspend fun getDeviceInfo (deviceId: Long): DeviceDefinitionData
}

class DeviceDefinitionManagementImpl : DeviceDefinitionManagement {
    override suspend fun addNewDevice(data: DeviceDefinitionData): Long = dbQuery {
        DeviceDefinitionManager.new {
            this.deviceName = data.deviceName
            this.deviceDescription = data.deviceDescription
            this.countDeviceSensors = data.countDeviceSensors
        }.id.value
    }

    override suspend fun isExists(deviceName: String): Boolean = dbQuery {
        DeviceDefinitionManager.find {
            DeviceDefinitionTable.deviceName eq deviceName
        }.limit(1).singleOrNull() != null
    }

    override suspend fun getDeviceId(deviceName: String): Long = dbQuery {
        DeviceDefinitionManager.find {
            DeviceDefinitionTable.deviceName eq deviceName
        }.limit(1).singleOrNull()?.id?.value
            ?: throw ExposedException(deviceWasNotFoundCode, deviceWasNotFound, listOf(deviceName))
    }

    /**
     * Be careful when using this function.
     */
    override suspend fun removeDevice(deviceId: Long): Boolean = dbQuery {
        DeviceDefinitionTable.deleteWhere {
            DeviceDefinitionTable.id eq deviceId
        } > 0
    }

    override suspend fun getDeviceInfo (deviceId: Long): DeviceDefinitionData = dbQuery {
        DeviceDefinitionManager.findById(deviceId)?.let {
            DeviceDefinitionData (it.deviceName, it.deviceDescription, it.countDeviceSensors)
        } ?: throw ExposedException(deviceWasNotFoundCode, deviceWasNotFound, listOf("$deviceId"))
    }
}

class DeviceDefinitionManager (id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DeviceDefinitionManager>(DeviceDefinitionTable)

    var deviceName by DeviceDefinitionTable.deviceName
    var deviceDescription by DeviceDefinitionTable.deviceDescription
    var countDeviceSensors by DeviceDefinitionTable.countDeviceSensors
}