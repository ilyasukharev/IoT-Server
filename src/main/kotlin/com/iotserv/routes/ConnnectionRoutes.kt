package com.iotserv.routes

import com.iotserv.dao.device_definition.DeviceDefinitionManagement
import com.iotserv.dao.device_structure.DeviceStructure
import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.dto.*
import com.iotserv.utils.DeviceSensorsHandler
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.arrivedBoardIDIsIncorrect
import com.iotserv.utils.RoutesResponses.arrivedSettingsIsIncorrect
import com.iotserv.utils.RoutesResponses.attemptToConnect2Sides
import com.iotserv.utils.RoutesResponses.authorizationBoardError
import com.iotserv.utils.RoutesResponses.authorizationError
import com.iotserv.utils.RoutesResponses.boardConnected
import com.iotserv.utils.RoutesResponses.boardIDSuccessfullyReceived
import com.iotserv.utils.RoutesResponses.boardWasFound
import com.iotserv.utils.RoutesResponses.boardWasNotFound
import com.iotserv.utils.RoutesResponses.clientConnected
import com.iotserv.utils.RoutesResponses.dataWereSuccessfullyWrote
import com.iotserv.utils.RoutesResponses.deviceHasNotBeenAdded
import com.iotserv.utils.RoutesResponses.deviceHasNotBeenAddedToUserBase
import com.iotserv.utils.RoutesResponses.deviceIdWasNotFound
import com.iotserv.utils.RoutesResponses.flushingDeviceSensors
import com.iotserv.utils.RoutesResponses.integrityObjectsViolation
import com.iotserv.utils.RoutesResponses.noBoardConnection
import com.iotserv.utils.RoutesResponses.searchingTheClient
import com.iotserv.utils.RoutesResponses.sendingBoardIDAccepted
import com.iotserv.utils.RoutesResponses.sendingSettingsAccepted
import com.iotserv.utils.RoutesResponses.serializationFailed
import com.iotserv.utils.RoutesResponses.settingsSuccessfullyReceived
import com.iotserv.utils.RoutesResponses.successfullyBoardConnection
import com.iotserv.utils.RoutesResponses.suchDeviceIdAlreadyRegistered
import com.iotserv.utils.RoutesResponses.suchSocketAlreadyExists
import com.iotserv.utils.RoutesResponses.unknownSocketCommand
import com.iotserv.utils.RoutesResponses.userIdWasNotFound
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.ktor.ext.inject

suspend fun receiveJsonData(
    socket: WebSocketServerSession,
    isBoardId: Boolean,
    logger: Logger,
    ip: String?,
    id: Long?
): BoardConnectionData {
    if (ip == null && id == null) throw Exception("Can`t logging because ip and id is null")

    var senderData = ""
    var senderType = SenderType.ID

    ip?.let {
        senderData = it
        senderType = SenderType.IP_ADDRESS_BOARD
    } ?: run {
        senderData = "$id"
        senderType = SenderType.ID
    }


    while (true) {
        try {
            val data = socket.receiveDeserialized<BoardConnectionData>()
            if (isBoardId) {
                data.boardIdentificationData?.let {
                    socket.send(boardIDSuccessfullyReceived)
                    logger.writeLog(boardIDSuccessfullyReceived, senderData, senderType)
                    return data
                } ?: run {
                    socket.send(arrivedBoardIDIsIncorrect)
                    logger.writeLog(arrivedBoardIDIsIncorrect, senderData, senderType)
                }

            } else {
                data.controlDeviceData?.let {
                    socket.send(settingsSuccessfullyReceived)
                    logger.writeLog(settingsSuccessfullyReceived, senderData, senderType)
                    return data
                } ?: run {
                    socket.send(arrivedSettingsIsIncorrect)
                    logger.writeLog(arrivedSettingsIsIncorrect, senderData, senderType)
                }
            }
        } catch (e: Exception) {
            socket.send(serializationFailed)
            logger.writeLog(serializationFailed, senderData, senderType)
            logger.writeLog(e.message.toString(), senderData, senderType)
        }
    }
}

fun Route.connectionRoutes() {
    val kredsClient by inject<KredsClient>()
    val jwtCooker by inject<JwtCooker>()
    val deviceDefinitionManagementDao by inject<DeviceDefinitionManagement>()
    val deviceStructureDao by inject<DeviceStructure>()
    val userDeviceDao by inject<UserDevice>()
    val logger by inject<Logger>()

    route("/connection") {
        webSocket("/board") {
            try {
                val ip = call.request.origin.remoteHost
                logger.writeLog(boardConnected, ip, SenderType.IP_ADDRESS_BOARD)

                incoming.receiveAsFlow().collect {
                    it as? Frame.Text ?: run {
                        logger.writeLog(unknownSocketCommand, ip, SenderType.IP_ADDRESS_BOARD)
                        send(unknownSocketCommand)
                        return@collect
                    }

                    when (it.readText()) {
                        "connect" -> {
                            logger.writeLog(sendingBoardIDAccepted, ip, SenderType.IP_ADDRESS_BOARD)
                            send(sendingBoardIDAccepted)

                            val boardUUID =
                                receiveJsonData(this, true, logger, ip, null).boardIdentificationData!!.boardUUID
                            send(attemptToConnect2Sides)
                            logger.writeLog(attemptToConnect2Sides, ip, SenderType.IP_ADDRESS_BOARD)

                            kredsClient.use { redis ->
                                if (redis.get("$boardUUID:contact") != null) {
                                    logger.writeLog(suchSocketAlreadyExists, ip, SenderType.IP_ADDRESS_BOARD)
                                    send(suchSocketAlreadyExists)
                                    return@collect
                                } else {
                                    redis.set("$boardUUID:contact", "false")
                                    redis.expire("$boardUUID:contact", 90U)
                                }
                            }

                            repeat(60) {
                                send(searchingTheClient)
                                delay(1000)
                                kredsClient.use { redis ->
                                    if (redis.get("$boardUUID:contact") == "true" && redis.get("$boardUUID:clientID") != null) {
                                        redis.del("$boardUUID:contact")
                                        redis.set("$boardUUID:verify", "true")
                                        redis.expire("$boardUUID:verify", 3600U)
                                        logger.writeLog(successfullyBoardConnection, ip, SenderType.IP_ADDRESS_BOARD)
                                        send(successfullyBoardConnection)
                                        return@collect
                                    }
                                }
                            }
                            logger.writeLog(noBoardConnection, ip, SenderType.IP_ADDRESS_BOARD)
                            send(noBoardConnection)
                        }

                        "check" -> {
                            logger.writeLog(sendingBoardIDAccepted, ip, SenderType.IP_ADDRESS_BOARD)
                            send(sendingBoardIDAccepted)

                            val boardUUID =
                                receiveJsonData(this, true, logger, ip, null).boardIdentificationData!!.boardUUID

                            kredsClient.use { redis ->
                                if (redis.get("$boardUUID:verify") != "true" || redis.get("$boardUUID:clientID") == null) {
                                    logger.writeLog(authorizationBoardError, ip, SenderType.IP_ADDRESS_BOARD)
                                    send(authorizationBoardError)
                                    return@collect
                                }
                            }

                            var userId: Long = 0

                            kredsClient.use { redis ->
                                redis.get("$boardUUID:clientID")?.toLong()?.let { id ->
                                    userId = id
                                    redis.del("$boardUUID:verify")
                                    redis.del("$boardUUID:clientID")
                                } ?: run {
                                    send(userIdWasNotFound)
                                    logger.writeLog(userIdWasNotFound, ip, SenderType.IP_ADDRESS_BOARD)
                                    return@collect
                                }
                            }

                            logger.writeLog(sendingSettingsAccepted, ip, SenderType.IP_ADDRESS_BOARD)
                            send(sendingSettingsAccepted)

                            val data = receiveJsonData(this, false, logger, ip, null).controlDeviceData!!

                            if (!deviceDefinitionManagementDao.isExists(data.deviceName)) {
                                deviceDefinitionManagementDao.addNewDevice(
                                    DeviceDefinitionData(
                                        data.deviceName,
                                        data.deviceDescription,
                                        data.sensorsList.size.toUInt()
                                    )
                                )?.let { deviceId ->
                                    logger.writeLog(flushingDeviceSensors, ip, SenderType.IP_ADDRESS_BOARD)
                                    deviceStructureDao.flushAll(deviceId)

                                    repeat(data.sensorsList.size) { i ->
                                        if (!deviceStructureDao.addSensor(
                                                DeviceStructureData(
                                                    deviceId,
                                                    data.sensorsList[i],
                                                    data.statesTypesList[i]
                                                )
                                            )
                                        ) {
                                            logger.writeLog(integrityObjectsViolation, ip, SenderType.IP_ADDRESS_BOARD)
                                            logger.writeLog(flushingDeviceSensors, ip, SenderType.IP_ADDRESS_BOARD)
                                            send(integrityObjectsViolation)
                                            deviceDefinitionManagementDao.removeDevice(deviceId)
                                            deviceStructureDao.flushAll(deviceId)
                                            return@collect
                                        }
                                    }


                                } ?: run {
                                    send(deviceHasNotBeenAdded)
                                    logger.writeLog(deviceHasNotBeenAdded, ip, SenderType.IP_ADDRESS_BOARD)
                                    return@collect
                                }
                            }

                            val deviceId = deviceDefinitionManagementDao.getDeviceId(data.deviceName) ?: run {
                                logger.writeLog(deviceIdWasNotFound, ip, SenderType.IP_ADDRESS_BOARD)
                                send(deviceIdWasNotFound)
                                return@collect
                            }

                            if (userDeviceDao.isExists(userId.toULong(), deviceId)) {
                                logger.writeLog(suchDeviceIdAlreadyRegistered, ip, SenderType.IP_ADDRESS_BOARD)
                                send(suchDeviceIdAlreadyRegistered)
                                return@collect
                            }

                            val state = DeviceSensorsHandler.serializeWithDefaultValues(data.sensorsList, data.statesTypesList)

                            if (!userDeviceDao.saveNewDevice(UserDeviceData(userId.toULong(), deviceId, state, boardUUID))) {
                                logger.writeLog(deviceHasNotBeenAdded, ip, SenderType.IP_ADDRESS_BOARD)
                                send(deviceHasNotBeenAddedToUserBase)
                                return@collect
                            }
                            logger.writeLog(dataWereSuccessfullyWrote, ip, SenderType.IP_ADDRESS_BOARD)
                            send(dataWereSuccessfullyWrote)
                        }

                        else -> send(unknownSocketCommand)
                    }
                }
            } catch (e: Exception) {
                println(e.message)
                close()
            }
        }
        authenticate("desktop-app") {
            webSocket("/app") {
                try {
                    val id = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asLong() ?: run {
                        logger.writeLog(authorizationError, call.request.origin.remoteHost, SenderType.ID)
                        sendSerialized(SocketConnectionResponseData(authorizationError))
                        return@webSocket
                    }

                    logger.writeLog(clientConnected, "$id", SenderType.ID)
                    val token = jwtCooker.buildToken(id)

                    incoming.receiveAsFlow().collect {
                        it as? Frame.Text ?: run {
                            logger.writeLog(unknownSocketCommand, "$id", SenderType.ID)
                            sendSerialized(SocketConnectionResponseData(unknownSocketCommand, token))
                            return@collect
                        }

                        when (it.readText()) {
                            "connect" -> {
                                logger.writeLog(sendingBoardIDAccepted, "$id", SenderType.ID)
                                send(sendingBoardIDAccepted)

                                val boardUUID =
                                    receiveJsonData(this, true, logger, null, id).boardIdentificationData!!.boardUUID
                                logger.writeLog(attemptToConnect2Sides, "$id", SenderType.ID)

                                repeat(60) {
                                    delay(1000)
                                    kredsClient.use { redis ->
                                        if (redis.get("$boardUUID:contact") != null) {
                                            redis.set("$boardUUID:contact", "true")
                                            redis.expire("$boardUUID:contact", 90U)

                                            redis.set("$boardUUID:clientID", "$id")
                                            redis.expire("$boardUUID:clientID", 3690U)
                                            logger.writeLog(boardWasFound, "$id", SenderType.ID)
                                            sendSerialized(SocketConnectionResponseData(boardWasFound, token))
                                            return@collect
                                        }
                                    }
                                }
                                logger.writeLog(boardWasNotFound, "$id", SenderType.ID)
                                sendSerialized(SocketConnectionResponseData(boardWasNotFound, token))
                            }

                            else -> sendSerialized(SocketConnectionResponseData(unknownSocketCommand, token))
                        }
                    }
                } catch (e: Exception) {
                    println(e.message)
                    close()
                }

            }
        }
    }
}