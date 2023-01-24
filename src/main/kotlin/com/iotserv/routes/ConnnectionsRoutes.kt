package com.iotserv.routes

import com.iotserv.dao.device_definition.DeviceDefinitionManagement
import com.iotserv.dao.device_structure.DeviceStructure
import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.dto.*
import com.iotserv.utils.DeviceStatementsCooker
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.DataWereSuccessfullyWrote
import com.iotserv.utils.RoutesResponses.authorizationError
import com.iotserv.utils.RoutesResponses.boardWasFound
import com.iotserv.utils.RoutesResponses.boardWasNotFound
import com.iotserv.utils.RoutesResponses.deviceHasNotBeenAdded
import com.iotserv.utils.RoutesResponses.deviceHasNotBeenAddedToUserBase
import com.iotserv.utils.RoutesResponses.deviceIdWasNotFound
import com.iotserv.utils.RoutesResponses.integrityObjectsViolation
import com.iotserv.utils.RoutesResponses.noConnection
import com.iotserv.utils.RoutesResponses.sendingSettingsAccepted
import com.iotserv.utils.RoutesResponses.settingsSuccessfullyReceived
import com.iotserv.utils.RoutesResponses.successfullyConnection
import com.iotserv.utils.RoutesResponses.suchDeviceIdAlreadyRegistered
import com.iotserv.utils.RoutesResponses.suchSocketAlreadyExists
import com.iotserv.utils.RoutesResponses.unknownSocketCommand
import com.iotserv.utils.RoutesResponses.userIdIsNotFound
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Route.connectionRoutes() {
    val kredsClient by inject<KredsClient>()
    val jwtCooker by inject<JwtCooker>()
    val deviceDefinitionManagementDao by inject<DeviceDefinitionManagement>()
    val deviceStructureDao by inject<DeviceStructure>()
    val userDeviceDao by inject<UserDevice>()

    route("/connection") {
        webSocket("/board") {
            try {
                val ip = call.request.local.remoteHost

                incoming.receiveAsFlow().collect {
                    if (it !is Frame.Text) {
                        send(unknownSocketCommand)
                        return@collect
                    }

                    when (it.readText()) {
                        "connect" -> {
                            launch {
                                kredsClient.use { redis ->
                                    if (redis.get("$ip:con") != null) {
                                        send(suchSocketAlreadyExists)
                                        return@launch
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
                                            return@launch
                                        }
                                    }
                                }
                                send(noConnection)
                            }
                        }

                        "check" -> {
                            launch {
                                kredsClient.use { redis ->
                                    if (redis.get("$ip:verify") != "true" || redis.get("$ip:id") == null) {
                                        send(authorizationError)
                                        return@launch
                                    }
                                }
                                val userId: Long
                                kredsClient.use { redis ->
                                    val id = redis.get("$ip:id")?.toLong() ?: return@launch send(userIdIsNotFound)
                                    userId = id
                                    redis.del("$ip:verify")
                                }

                                send(sendingSettingsAccepted)
                                val data = receiveDeserialized<ControlDeviceData>()
                                send(settingsSuccessfullyReceived)

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
                                                return@launch
                                            }
                                        }
                                    } ?: send(deviceHasNotBeenAdded)
                                }

                                val deviceId = deviceDefinitionManagementDao.getDeviceId(data.deviceName)
                                    ?: return@launch send(deviceIdWasNotFound)

                                if (userDeviceDao.isExists(userId.toULong(), deviceId)) {
                                    send(suchDeviceIdAlreadyRegistered)
                                    return@launch
                                }

                                if (!userDeviceDao.saveNewDevice(
                                    UserDeviceData(
                                        userId.toULong(),
                                        deviceId,
                                        DeviceStatementsCooker.generateStatement(data.sensorsList, data.statesTypesList)
                                    )
                                )) {
                                    send(deviceHasNotBeenAddedToUserBase)
                                    return@launch
                                }
                            }
                            send(DataWereSuccessfullyWrote)
                        }


                        else -> send(unknownSocketCommand)
                    }
                }
            } catch (e: Exception) {
                println(e.message)
            }

        }
        authenticate("desktop-app") {
            webSocket("/app") {
                val id = call.principal<JWTPrincipal>()?.payload?.getClaim("id")?.asLong()
                    ?: return@webSocket sendSerialized(SocketConnectionResponseData(authorizationError))

                try {
                    val ip = call.request.local.remoteHost
                    val token = jwtCooker.buildToken(id)

                    incoming.receiveAsFlow().collect {
                        if (it !is Frame.Text)  {
                            sendSerialized(SocketConnectionResponseData(unknownSocketCommand, token))
                            return@collect
                        }

                        when(it.readText()) {
                            "connect" -> {
                                launch {
                                    repeat(60) {
                                        delay(1000)
                                        kredsClient.use {redis ->
                                            if (redis.get("$ip:con") != null) {
                                                redis.set("$ip:con", "true")
                                                redis.expire("$ip:con", 90U)

                                                redis.set("$ip:id", "$id")
                                                redis.expire("$ip:id", 3690U)
                                                sendSerialized(SocketConnectionResponseData(boardWasFound, token))
                                                return@launch
                                            }
                                        }
                                    }
                                    sendSerialized(SocketConnectionResponseData(boardWasNotFound, token))
                                }
                            }
                            else -> sendSerialized(SocketConnectionResponseData(unknownSocketCommand, token))
                        }
                    }


                } catch (e: Exception) {
                    println(e.message)
                }

            }
        }
    }
}