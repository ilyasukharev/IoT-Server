package com.iotserv.dao.users_devices

import com.iotserv.dto.UserDeviceData
import com.iotserv.exceptions.ExposedException
import com.iotserv.utils.DatabaseFactory.dbQuery
import com.iotserv.utils.RoutesResponses.ownerOfBoardWasNotFound
import com.iotserv.utils.RoutesResponses.ownerOfBoardWasNotFoundCode
import com.iotserv.utils.RoutesResponses.userOrDeviceNotFound
import com.iotserv.utils.RoutesResponses.userOrDeviceNotFoundCode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*

interface UserDevice {
    suspend fun isBoardUUIDExists(boardUUID: String): Boolean
    suspend fun saveNewDevice(data: UserDeviceData)
    suspend fun isBoardIdExists(boardId: String): Boolean
    suspend fun getAll(id: Long, limit: Int, offset: Long): List<UserDeviceData>
    suspend fun get(userId: Long, deviceId: Long): UserDeviceData
    suspend fun updateState(deviceId: Long, state: String): Boolean
    suspend fun getBoardOwner(boardId: String): Long
}

class UserDeviceImpl : UserDevice {

    /**
     * Important info: the boardUUID is unique.
     */
    override suspend fun isBoardUUIDExists(boardUUID: String): Boolean = dbQuery {
        UserDeviceManager.find {
            UserDevicesTable.boardId eq boardUUID
        }.limit(1).singleOrNull() != null
    }

    override suspend fun saveNewDevice(data: UserDeviceData): Unit = dbQuery {
        UserDeviceManager.new {
            userId = data.userId
            deviceId = data.deviceId
            state = data.state
            boardId = data.boardId
        }
    }

    override suspend fun isBoardIdExists(boardId: String): Boolean = dbQuery {
        UserDeviceManager.find {
            UserDevicesTable.boardId eq boardId
        }.limit(1).singleOrNull() != null
    }

    private fun resultRowToStructure(data: UserDeviceManager): UserDeviceData  =
        UserDeviceData (
            data.userId,
            data.deviceId,
            data.state,
            data.boardId
        )

    override suspend fun getAll(id: Long, limit: Int, offset: Long): List<UserDeviceData> = dbQuery {
        UserDeviceManager.find {
            UserDevicesTable.userId eq id
        }.limit(limit, offset).orderBy(Pair(UserDevicesTable.deviceId, SortOrder.ASC)).map {
            resultRowToStructure(it)
        }
    }

    override suspend fun get(userId: Long, deviceId: Long): UserDeviceData = dbQuery {
        UserDeviceManager.find {
            (UserDevicesTable.userId eq userId) and (UserDevicesTable.deviceId eq deviceId)
        }.limit(1).singleOrNull()?.let {
            UserDeviceData(it.userId, it.deviceId, it.state, it.boardId)
        } ?: throw ExposedException (
            userOrDeviceNotFoundCode,
            userOrDeviceNotFound,
            listOf("user: $userId", "device: $deviceId")
        )
    }

    override suspend fun updateState(deviceId: Long, state: String): Boolean = dbQuery {
        UserDevicesTable.update({UserDevicesTable.deviceId eq deviceId}) {
            it[UserDevicesTable.state] = state
        } > 0
    }

    override suspend fun getBoardOwner(boardId: String): Long = dbQuery {
        UserDeviceManager.find {
                UserDevicesTable.boardId eq boardId
            }.singleOrNull()?.userId ?: throw ExposedException(
                ownerOfBoardWasNotFoundCode, ownerOfBoardWasNotFound, listOf("boardId: $boardId")
            )
    }
}

class UserDeviceManager (id: EntityID<Long>): LongEntity(id) {
    companion object : LongEntityClass<UserDeviceManager>(UserDevicesTable)

    var userId by UserDevicesTable.userId
    var deviceId by UserDevicesTable.deviceId
    var state by UserDevicesTable.state
    var boardId by UserDevicesTable.boardId
}