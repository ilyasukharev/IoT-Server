package com.iotserv.routes.connections

import com.iotserv.dao.device_definition.DeviceDefinitionManagement
import com.iotserv.dao.device_structure.DeviceStructure
import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.dto.*
import com.iotserv.exceptions.ExposedException
import com.iotserv.exceptions.SocketException
import com.iotserv.plugins.transactException
import com.iotserv.utils.DeviceSensorsHandler
import com.iotserv.utils.RoutesResponses.arrivedBoardIDIsIncorrect
import com.iotserv.utils.RoutesResponses.arrivedSettingsIsIncorrect
import com.iotserv.utils.RoutesResponses.attemptToConnect2Sides
import com.iotserv.utils.RoutesResponses.boardConnected
import com.iotserv.utils.RoutesResponses.boardIDSuccessfullyReceived
import com.iotserv.utils.RoutesResponses.boardIsNotVerifiedOrUserNotFound
import com.iotserv.utils.RoutesResponses.boardWasFound
import com.iotserv.utils.RoutesResponses.clientConnected
import com.iotserv.utils.RoutesResponses.clientsWasNotConnected
import com.iotserv.utils.RoutesResponses.commandIsUnknown
import com.iotserv.utils.RoutesResponses.dataWereSuccessfullyWrote
import com.iotserv.utils.RoutesResponses.searchingTheClient
import com.iotserv.utils.RoutesResponses.sendingBoardIDAccepted
import com.iotserv.utils.RoutesResponses.sendingSettingsAccepted
import com.iotserv.utils.RoutesResponses.serializationFailed
import com.iotserv.utils.RoutesResponses.settingsSuccessfullyReceived
import com.iotserv.utils.RoutesResponses.successfullyBoardConnection
import com.iotserv.utils.RoutesResponses.suchBoardAlreadyListening
import com.iotserv.utils.RoutesResponses.suchBoardUUIDAlreadyExists
import com.iotserv.utils.RoutesResponses.suchBoardUUIDAlreadyExistsCode
import com.iotserv.utils.RoutesResponses.suchDeviceAlreadyRegisteredByUser
import com.iotserv.utils.RoutesResponses.unknownSocketCommand
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
import java.time.Duration

suspend fun receiveJsonData(socket: WebSocketServerSession, isBoardId: Boolean, logger: Logger, ip: String?, id: Long?): BoardConnectionData {

    var senderType = SenderType.ID

    val senderData = ip?.let {
        senderType = SenderType.IP_ADDRESS_BOARD
        it
    } ?: "$id"


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
        }
    }
}

fun Route.connectionRoutes() {
    val kredsClient by inject<KredsClient>()
    val deviceDefinitionManagementDao by inject<DeviceDefinitionManagement>()
    val deviceStructureDao by inject<DeviceStructure>()
    val userDeviceDao by inject<UserDevice>()
    val logger by inject<Logger>()

    route("/connection") {
        webSocket("/board") {
            pingInterval = Duration.ofSeconds(300)
            val ip = call.request.origin.remoteHost
            try {
                logger.writeLog(boardConnected, ip, SenderType.IP_ADDRESS_BOARD)
                incoming.receiveAsFlow().collect {
                    it as? Frame.Text ?: throw SocketException (commandIsUnknown)

                    when (it.readText()) {
                        "connect" -> {
                            logger.writeLog(sendingBoardIDAccepted, ip, SenderType.IP_ADDRESS_BOARD)
                            send(sendingBoardIDAccepted)

                            val boardUUID = receiveJsonData(this, true, logger, ip, null).boardIdentificationData!!.boardUUID

                            if (userDeviceDao.isBoardUUIDExists(boardUUID)) {
                                throw ExposedException(suchBoardUUIDAlreadyExistsCode, suchBoardUUIDAlreadyExists)
                            }

                            send(attemptToConnect2Sides)
                            logger.writeLog(attemptToConnect2Sides, ip, SenderType.IP_ADDRESS_BOARD)

                            kredsClient.use { redis ->
                                redis.get("$boardUUID:contact")?.let {
                                    throw SocketException(suchBoardAlreadyListening)
                                } ?: run {
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
                                        return@collect send(successfullyBoardConnection)
                                    }
                                }
                            }
                            throw SocketException(clientsWasNotConnected)
                        }

                        "check" -> {
                            logger.writeLog(sendingBoardIDAccepted, ip, SenderType.IP_ADDRESS_BOARD)
                            send(sendingBoardIDAccepted)

                            val boardUUID = receiveJsonData(this, true, logger, ip, null).boardIdentificationData!!.boardUUID

                            val userId = kredsClient.use { redis ->
                                Pair(redis.get("$boardUUID:verify"), redis.get("$boardUUID:clientID")).let { pair ->
                                    if (pair.first != "true" || pair.second == null)  throw SocketException(boardIsNotVerifiedOrUserNotFound)
                                    else {
                                        redis.del("$boardUUID:verify")
                                        redis.del("$boardUUID:clientID")
                                        pair.second!!.toLong()
                                    }
                                }
                            }

                            logger.writeLog(sendingSettingsAccepted, ip, SenderType.IP_ADDRESS_BOARD)
                            send(sendingSettingsAccepted)

                            val data = receiveJsonData(this, false, logger, ip, null).controlDeviceData!!

                            if (!deviceDefinitionManagementDao.isExists(data.deviceName)) {
                                deviceDefinitionManagementDao.addNewDevice(
                                    DeviceDefinitionData (data.deviceName, data.deviceDescription, data.sensorsList.size)
                                ).let { deviceId ->
                                    repeat(data.sensorsList.size) { i ->
                                        deviceStructureDao.addSensor (
                                            DeviceStructureData (deviceId, data.sensorsList[i], data.statesTypesList[i])
                                        )
                                    }
                                }
                            }

                            val deviceId = deviceDefinitionManagementDao.getDeviceId(data.deviceName)

                            if (userDeviceDao.isBoardUUIDExists(boardUUID)) {
                                throw SocketException(suchDeviceAlreadyRegisteredByUser)
                            }

                            val state = DeviceSensorsHandler.serializeWithDefaultValues(data.sensorsList, data.statesTypesList)

                            userDeviceDao.saveNewDevice(UserDeviceData(userId, deviceId, state, boardUUID))

                            logger.writeLog(dataWereSuccessfullyWrote, ip, SenderType.IP_ADDRESS_BOARD)
                            send(dataWereSuccessfullyWrote)
                            close()
                        }

                        else -> send(unknownSocketCommand)
                    }
                }
            } catch (e: SocketException) {
                transactException(e.message!!, ip, logger, this)
            } catch(e: ExposedException) {
                transactException(e.message, ip, logger, this)
            } catch (e: Exception) {
                transactException((e.message ?: "null"), ip, logger, this)
            }
        }
        authenticate("desktop-app") {
            webSocket("/app") {
                pingInterval = Duration.ofSeconds(300)
                val id = call.principal<JWTPrincipal>()!!.payload.getClaim("id").asLong()
                try {
                    logger.writeLog(clientConnected, "$id", SenderType.ID)

                    incoming.receiveAsFlow().collect {
                        it as? Frame.Text ?: throw SocketException (commandIsUnknown)

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
                                            send(boardWasFound)
                                        }
                                    }
                                }
                                throw SocketException(clientsWasNotConnected)
                            }

                            else -> send(unknownSocketCommand)
                        }
                    }
                } catch(e: SocketException) {
                    transactException(e.message!!, id, logger, this)
                } catch (e: Exception) {
                    transactException(e.message ?: "", id, logger, this)
                }

            }
        }
    }
}