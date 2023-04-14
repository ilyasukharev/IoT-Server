package com.iotserv.dao.device_structure

import com.iotserv.dto.DeviceStructureData
import com.iotserv.exceptions.ExposedException
import com.iotserv.utils.DatabaseFactory.dbQuery
import com.iotserv.utils.RoutesResponses.sensorWasNotFound
import com.iotserv.utils.RoutesResponses.sensorWasNotFoundCode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

interface DeviceStructure {
    suspend fun addSensor(data: DeviceStructureData)
    suspend fun getSensorType(deviceId: Long, sensorName: String): String
}

class DeviceStructureIMPL : DeviceStructure {
    override suspend fun addSensor(data: DeviceStructureData): Unit = dbQuery {
        DeviceStructureManager.new {
            deviceId = data.deviceId
            sensorName = data.sensorName
            sensorStateType = data.sensorStateType
        }
    }

    override suspend fun getSensorType(deviceId: Long, sensorName: String): String = dbQuery {
        DeviceStructureManager.find {
            (DeviceStructureTable.deviceId eq deviceId) and (DeviceStructureTable.sensorName eq sensorName)
        }.limit(1).singleOrNull()?.sensorStateType
            ?: throw ExposedException(sensorWasNotFoundCode, sensorWasNotFound, listOf(sensorName))
    }
}

class DeviceStructureManager (id: EntityID<Long>): LongEntity(id) {
    companion object : LongEntityClass<DeviceStructureManager>(DeviceStructureTable)

    var deviceId by DeviceStructureTable.deviceId
    var sensorName by DeviceStructureTable.sensorName
    var sensorStateType by DeviceStructureTable.sensorStateType
}