package com.iotserv.dao.users_devices

import com.iotserv.dto.UserDeviceData
import com.iotserv.utils.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*

interface UserDevice {
    suspend fun isExists(userId: ULong, deviceId: ULong): Boolean
    suspend fun saveNewDevice(data: UserDeviceData): Boolean
    suspend fun isBoardIdExists(boardId: String): Boolean
    suspend fun getAll(id: ULong): List<UserDeviceData>
    suspend fun get(deviceId: ULong): UserDeviceData?
    suspend fun updateState(deviceId: ULong, state: String): Boolean
}

class UserDeviceImpl : UserDevice {
    override suspend fun isExists(userId: ULong, deviceId: ULong): Boolean = dbQuery {
        UserDevicesTable.select {
            (UserDevicesTable.userId eq userId) and
            (UserDevicesTable.deviceId eq deviceId)
        }.limit(1).singleOrNull() != null
    }

    override suspend fun saveNewDevice(data: UserDeviceData): Boolean = dbQuery {
        UserDevicesTable.insert {
            it[userId] = data.userId
            it[deviceId] = data.deviceId
            it[state] = data.state
            it[boardId] = data.boardId
        }.resultedValues != null
    }

    override suspend fun isBoardIdExists(boardId: String): Boolean = dbQuery {
        UserDevicesTable.slice(UserDevicesTable.boardId).select {
            UserDevicesTable.boardId eq boardId
        }.limit(1).singleOrNull() != null
    }

    private fun resultRowToStructure(row: ResultRow): UserDeviceData  =
        UserDeviceData(
            row[UserDevicesTable.userId],
            row[UserDevicesTable.deviceId],
            row[UserDevicesTable.state],
            row[UserDevicesTable.boardId]
        )

    override suspend fun getAll(id: ULong): List<UserDeviceData> = dbQuery {
        UserDevicesTable.selectAll().map {resultRowToStructure(it) }
    }

    override suspend fun get(deviceId: ULong): UserDeviceData? = dbQuery {
        UserDevicesTable.select {
            UserDevicesTable.deviceId eq deviceId
        }.limit(1).singleOrNull()?.let {
            UserDeviceData(
                it[UserDevicesTable.userId],
                it[UserDevicesTable.deviceId],
                it[UserDevicesTable.state],
                it[UserDevicesTable.boardId]
            )
        }
    }

    override suspend fun updateState(deviceId: ULong, state: String): Boolean = dbQuery {
        UserDevicesTable.update({UserDevicesTable.deviceId eq deviceId}) {
            it[UserDevicesTable.state] = state
        } > 0
    }

}