package com.iotserv.dao.users_devices

import com.iotserv.dto.UserDeviceData
import com.iotserv.utils.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

interface UserDevice {
    suspend fun isExists(userId: ULong, deviceId: ULong): Boolean
    suspend fun saveNewDevice(data: UserDeviceData): Boolean
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
        }.resultedValues != null
    }

}