package com.iotserv.routes.managements.client.rest

import com.iotserv.dao.device_definition.DeviceDefinitionManagement
import com.iotserv.dao.device_structure.DeviceStructure
import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.dto.*
import com.iotserv.exceptions.ExposedException
import com.iotserv.exceptions.OtherException
import com.iotserv.utils.DeviceSensorsHandler.deserializeToMap
import com.iotserv.utils.DeviceSensorsHandler.getUpdateState
import com.iotserv.utils.RoutesResponses.deviceDataWasSent
import com.iotserv.utils.RoutesResponses.deviceIsNotListening
import com.iotserv.utils.RoutesResponses.deviceIsNotListeningCode
import com.iotserv.utils.RoutesResponses.deviceListeningStateWasReset
import com.iotserv.utils.RoutesResponses.deviceStateHasBeenUpdated
import com.iotserv.utils.RoutesResponses.deviceStateHasNotBeenUpdated
import com.iotserv.utils.RoutesResponses.deviceStateHasNotBeenUpdatedCode
import com.iotserv.utils.RoutesResponses.deviceWasNotFound
import com.iotserv.utils.RoutesResponses.deviceWasNotFoundCode
import com.iotserv.utils.RoutesResponses.listWasSent
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.resources.put
import io.ktor.server.resources.post

fun Route.clientManagementRoutes() {
    val logger by inject<Logger>()
    val userDeviceDao by inject<UserDevice>()
    val deviceStructureDao by inject<DeviceStructure>()
    val deviceDefinitionDao by inject<DeviceDefinitionManagement>()
    val kredsClient by inject<KredsClient>()

    authenticate("desktop-app") {
        getDevicesDoc()
        get<Devices> { device ->
            call.principal<JWTPrincipal>()!!.payload.let { payload ->
                val id = payload.getClaim("id").asLong()

                userDeviceDao.getAll(id, device.limit!!, device.offset!!).let { userDevices ->
                    userDevices.map {
                        val deviceInfo = deviceDefinitionDao.getDeviceInfo(it.deviceId)
                        CommonDeviceData(it.deviceId, deviceInfo.deviceName, deviceInfo.deviceDescription, it.boardId)
                    }.let { commonDevicesDataList ->
                        logger.writeLog(listWasSent, "$id", SenderType.ID)
                        call.respond(
                            HttpStatusCode.OK,
                            ClientManagementResponseData(deviceListInfo = commonDevicesDataList)
                        )
                    }
                }
            }
        }
        getDetailDeviceDoc()
        post<Devices.Id> { device ->
            call.principal<JWTPrincipal>()!!.payload.let { payload ->
                val userId = payload.getClaim("id").asLong()
                val requestData = call.receive<BoardIdData>()

                userDeviceDao.getByBoardId(userId, requestData.boardId).let {userDeviceData ->
                    if (userDeviceData.deviceId != device.id)
                        throw ExposedException(deviceWasNotFoundCode, deviceWasNotFound)

                    val deviceDefinition = deviceDefinitionDao.getDeviceInfo(device.id)
                    val sensorsMap = deserializeToMap(userDeviceData.state)

                    DetailDeviceData(device.id, deviceDefinition.deviceName, sensorsMap).let { data ->
                        logger.writeLog(deviceDataWasSent, "$userId", SenderType.ID)
                        call.respond(HttpStatusCode.OK, ClientManagementResponseData(deviceInfo = data))
                    }
                }
            }
        }

        getChangeDeviceDoc()
        put<Devices.Id.Change> { device ->
            call.principal<JWTPrincipal>()!!.payload.let { payload ->
                val userId = payload.getClaim("id").asLong()
                val data = call.receive<ChangeDeviceData>()

                val deviceData = userDeviceDao.getByBoardId(userId, data.boardId)

                kredsClient.use { redis ->
                    if (redis.get("${deviceData.boardId}:isListening") != "true") {
                        logger.writeLog(deviceIsNotListening, "$userId", SenderType.ID)
                        throw OtherException(
                            deviceIsNotListeningCode,
                            deviceIsNotListening,
                            listOf("boardUUID: ${deviceData.boardId}")
                        )
                    }
                }


                deviceStructureDao.getSensorType(device.parent.id, data.sensor).let { sensorType ->
                    val state = getUpdateState(sensorType, data.state, data.sensor, deviceData.state)

                    if (!userDeviceDao.updateState(deviceData.boardId, state)) {
                        logger.writeLog(deviceStateHasNotBeenUpdated, "$userId", SenderType.ID)
                        throw ExposedException(deviceStateHasNotBeenUpdatedCode, deviceStateHasNotBeenUpdated)
                    }

                    kredsClient.use { redis ->
                        "${deviceData.boardId}:updateStatement".let {
                            redis.set(it, "${data.sensor}:${data.state}")
                            redis.expire(it, 3600U)
                        }
                    }

                    logger.writeLog(deviceStateHasBeenUpdated, "$userId", SenderType.ID)
                    call.respond(HttpStatusCode.OK, ClientManagementResponseData(msg = deviceStateHasBeenUpdated))
                }
            }
        }
        getResetDeviceStateDoc()
        post <Devices.Reset> {
            call.principal<JWTPrincipal>()!!.payload.let { payload ->
                val userId = payload.getClaim("id").asLong()
                val requestData = call.receive<BoardIdData>()

                userDeviceDao.getByBoardId(
                    userId,
                    requestData.boardId
                ).let {deviceData ->
                    kredsClient.use { redis ->
                        redis.del("${deviceData.boardId}:isListening")
                    }
                    logger.writeLog(deviceListeningStateWasReset, "$userId", SenderType.ID)
                    call.respond(HttpStatusCode.OK, ClientManagementResponseData(msg = deviceListeningStateWasReset))
                }
            }
        }
    }
}


