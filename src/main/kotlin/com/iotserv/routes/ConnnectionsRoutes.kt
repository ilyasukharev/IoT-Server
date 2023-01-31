package com.iotserv.routes

import com.iotserv.dao.device_definition.DeviceDefinitionManagement
import com.iotserv.dao.device_structure.DeviceStructure
import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.dto.*
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.DataWereSuccessfullyWrote
import com.iotserv.utils.RoutesResponses.arrivedDataIsIncorrect
import com.iotserv.utils.RoutesResponses.attemptToConnect2Sides
import com.iotserv.utils.RoutesResponses.authorizationError
import com.iotserv.utils.RoutesResponses.boardConnected
import com.iotserv.utils.RoutesResponses.boardWasFound
import com.iotserv.utils.RoutesResponses.boardWasNotFound
import com.iotserv.utils.RoutesResponses.clientConnected
import com.iotserv.utils.RoutesResponses.deviceHasNotBeenAdded
import com.iotserv.utils.RoutesResponses.deviceHasNotBeenAddedToUserBase
import com.iotserv.utils.RoutesResponses.deviceIdWasNotFound
import com.iotserv.utils.RoutesResponses.generatedDefaultStateIsNull
import com.iotserv.utils.RoutesResponses.integrityObjectsViolation
import com.iotserv.utils.RoutesResponses.noConnection
import com.iotserv.utils.RoutesResponses.sendingSettingsAccepted
import com.iotserv.utils.RoutesResponses.settingsSuccessfullyReceived
import com.iotserv.utils.RoutesResponses.successfullyConnection
import com.iotserv.utils.RoutesResponses.suchDeviceIdAlreadyRegistered
import com.iotserv.utils.RoutesResponses.suchSocketAlreadyExists
import com.iotserv.utils.RoutesResponses.unknownSocketCommand
import com.iotserv.utils.RoutesResponses.userIdIsNotFound
import com.iotserv.utils.logger.FileLogger
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
                    if (it !is Frame.Text) {
                        send(unknownSocketCommand)
                        return@collect
                    }

                    when (it.readText()) {
                        "connect" -> {
                            logger.writeLog(attemptToConnect2Sides, ip, SenderType.IP_ADDRESS_BOARD)

                            kredsClient.use { redis ->
                                if (redis.get("$ip:con") != null) {
                                    logger.writeLog(suchSocketAlreadyExists, ip, SenderType.IP_ADDRESS_BOARD)
                                    send(suchSocketAlreadyExists)
                                    return@collect
                                } else {
                                    redis.set("$ip:con", "false")
                                    redis.expire("$ip:con", 90U)
                                }
                            }

                            repeat(60) {
                                delay(1000)
                                kredsClient.use { redis ->
                                    if (redis.get("$ip:con") == "true" && redis.get("$ip:id") != null) {
                                        redis.del("$ip:con")
                                        redis.set("$ip:verify", "true")
                                        redis.expire("$ip:verify", 3600U)
                                        send(successfullyConnection)
                                        return@collect
                                    }
                                }
                            }
                            send(noConnection)
                        }

                        "check" -> {
                            kredsClient.use { redis ->
                                if (redis.get("$ip:verify") != "true" || redis.get("$ip:id") == null) {
                                    send(authorizationError)
                                    return@collect
                                }
                            }
                            val userId: Long
                            kredsClient.use { redis ->
                                userId = redis.get("$ip:id")?.toLong() ?: return@collect send(userIdIsNotFound)
                                redis.del("$ip:verify")
                            }

                            send(sendingSettingsAccepted)

                            var data: ControlDeviceData
                            while(true) {
                                try {
                                    data = receiveDeserialized()
                                    break
                                } catch (e: Exception) {
                                    send(arrivedDataIsIncorrect)
                                }
                            }

                            send(settingsSuccessfullyReceived)

                            val state = StringBuilder()

                            if (!deviceDefinitionManagementDao.isExists(data.deviceName)) {
                                deviceDefinitionManagementDao.addNewDevice(
                                    DeviceDefinitionData(
                                        data.deviceName,
                                        data.deviceDescription,
                                        data.sensorsList.size.toUInt()
                                    )
                                )?.let { deviceId ->
                                    for (i in 0 until data.sensorsList.size) {
                                        if (deviceStructureDao.isSensorExists(deviceId, data.sensorsList[i])) {
                                            deviceStructureDao.flushAll(deviceId)
                                            break
                                        }
                                    }

                                    for (i in 0 until data.sensorsList.size) {
                                        if (!deviceStructureDao.addSensor(
                                                DeviceStructureData(
                                                    deviceId,
                                                    data.sensorsList[i],
                                                    data.statesTypesList[i]
                                                )
                                            )) {
                                            send(integrityObjectsViolation)
                                            deviceStructureDao.flushAll(deviceId)
                                            return@collect
                                        }
                                        else state.append("${data.sensorsList[i]}:${data.statesTypesList[i]};")
                                    }
                                } ?: return@collect send(deviceHasNotBeenAdded)
                            }

                            val deviceId = deviceDefinitionManagementDao.getDeviceId(data.deviceName)
                                ?: return@collect send(deviceIdWasNotFound)

                            if (userDeviceDao.isExists(userId.toULong(), deviceId)) {
                                send(suchDeviceIdAlreadyRegistered)
                                return@collect
                            }

                            if (state.toString() == "") {
                                send(generatedDefaultStateIsNull)
                                return@collect
                            }

                            if (!userDeviceDao.saveNewDevice(
                                    UserDeviceData(
                                        userId.toULong(),
                                        deviceId,
                                        state.toString()
                                    )
                                )) {
                                send(deviceHasNotBeenAddedToUserBase)
                                return@collect
                            }
                            send(DataWereSuccessfullyWrote)
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
                val id = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asLong()
                    ?: return@webSocket sendSerialized(SocketConnectionResponseData(authorizationError))

                logger.writeLog(clientConnected, "$id", SenderType.ID)

                try {
                    val ip = call.request.origin.remoteHost
                    val token = jwtCooker.buildToken(id)

                    incoming.receiveAsFlow().collect {
                        if (it !is Frame.Text)  {
                            sendSerialized(SocketConnectionResponseData(unknownSocketCommand, token))
                            return@collect
                        }

                        when(it.readText()) {
                            "connect" -> {
                                logger.writeLog(attemptToConnect2Sides, "$id", SenderType.ID)

                                repeat(60) {
                                    delay(1000)
                                    kredsClient.use {redis ->
                                        if (redis.get("$ip:con") != null) {
                                            redis.set("$ip:con", "true")
                                            redis.expire("$ip:con", 90U)

                                            redis.set("$ip:id", "$id")
                                            redis.expire("$ip:id", 3690U)
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