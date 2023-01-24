package com.iotserv.dao.device_structure

import com.iotserv.dto.DeviceStructureData
import com.iotserv.utils.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

interface DeviceStructure {
    suspend fun isSensorExists(deviceId: ULong, sensorName: String): Boolean
    suspend fun addSensor(data: DeviceStructureData): Boolean
    suspend fun flushAll(deviceId: ULong): Boolean
}

class DeviceStructureIMPL : DeviceStructure {
    override suspend fun isSensorExists(deviceId: ULong, sensorName: String): Boolean = dbQuery {
        DeviceStructureTable.select {
            (DeviceStructureTable.id eq deviceId) and
            (DeviceStructureTable.sensorName eq sensorName)
        }.limit(1).singleOrNull()?.let{true} ?: false
    }

    override suspend fun addSensor(data: DeviceStructureData): Boolean = dbQuery {
        DeviceStructureTable.insert {
            it[id] = data.deviceId
            it[sensorName] = data.sensorName
            it[sensorStateType] = data.sensorStateType
        }.resultedValues != null
    }

    override suspend fun flushAll(deviceId: ULong): Boolean = dbQuery {
        while(true) {
            if (DeviceStructureTable.deleteWhere {id eq deviceId } <= 0) break;
        }
        true
    }

}