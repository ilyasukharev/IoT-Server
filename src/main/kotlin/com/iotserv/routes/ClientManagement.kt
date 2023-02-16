package com.iotserv.routes

import com.iotserv.dao.device_definition.DeviceDefinitionManagement
import com.iotserv.dao.device_structure.DeviceStructure
import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.dto.ChangeDeviceData
import com.iotserv.dto.ClientManagementResponseData
import com.iotserv.dto.CommonDeviceData
import com.iotserv.dto.DetailsDeviceData
import com.iotserv.utils.DeviceSensorsHandler.deserializeToMap
import com.iotserv.utils.DeviceSensorsHandler.getUpdateState
import com.iotserv.utils.RoutesResponses.authorizationError
import com.iotserv.utils.RoutesResponses.clientHasNotSuchDevice
import com.iotserv.utils.RoutesResponses.deviceDataWasSent
import com.iotserv.utils.RoutesResponses.deviceStateHasBeenUpdated
import com.iotserv.utils.RoutesResponses.deviceStateWasNotBeenUpdated
import com.iotserv.utils.RoutesResponses.deviceWasNotFound
import com.iotserv.utils.RoutesResponses.listIsEmpty
import com.iotserv.utils.RoutesResponses.listWasSent
import com.iotserv.utils.RoutesResponses.sensorIsNotExists
import com.iotserv.utils.RoutesResponses.typeOfSensorStateIsNotCorrect
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.clientManagementRoutes() {
    val logger by inject<Logger>()
    val userDeviceDao by inject<UserDevice>()
    val deviceStructureDao by inject<DeviceStructure>()
    val deviceDefinitionDao by inject<DeviceDefinitionManagement>()
    val kredsClient by inject<KredsClient>()

    route("/management") {
        authenticate ("desktop-app"){
            route("/user/sensors") {
                get {
                    call.principal<JWTPrincipal>()?.payload?.let { payload ->
                        val id = payload.getClaim("id").asLong()

                        userDeviceDao.getAll(id.toULong()).let {userDevices ->
                            if (userDevices.isEmpty()) {
                                logger.writeLog(listIsEmpty, "$id", SenderType.ID)
                                return@get call.respond(HttpStatusCode.OK, ClientManagementResponseData(listIsEmpty))
                            }

                            userDevices.map {
                                val deviceInfo = deviceDefinitionDao.getDeviceInfo(it.deviceId) ?: run {
                                    logger.writeLog(deviceWasNotFound, "$id", SenderType.ID)
                                    return@get call.respond(HttpStatusCode.InternalServerError, ClientManagementResponseData(deviceWasNotFound))
                                }
                                CommonDeviceData(it.deviceId, deviceInfo.deviceName, deviceInfo.deviceDescription)
                            }.let { commonDevicesDataList ->
                                logger.writeLog(listWasSent, "$id", SenderType.ID)
                                call.respond(HttpStatusCode.OK, ClientManagementResponseData(listWasSent, deviceListInfo = commonDevicesDataList))
                            }
                        }

                    } ?: run {
                        call.respond(HttpStatusCode.NonAuthoritativeInformation, ClientManagementResponseData(authorizationError))
                        logger.writeLog(authorizationError, call.request.origin.remoteHost, SenderType.IP_ADDRESS)
                    }
                }
                get("/{id}") {
                    val deviceId = call.parameters["id"]!!.toULong()

                    call.principal<JWTPrincipal>()?.payload?.let { payload ->
                        val userId = payload.getClaim("id").asLong()

                        val deviceDefinition = deviceDefinitionDao.getDeviceInfo(deviceId) ?: run {
                            call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(deviceWasNotFound))
                            logger.writeLog(deviceWasNotFound, "$userId", SenderType.ID)
                            return@get
                        }

                        val state = userDeviceDao.get(deviceId)?.state ?: run {
                            call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(clientHasNotSuchDevice))
                            logger.writeLog(clientHasNotSuchDevice, "$userId", SenderType.ID)
                            return@get
                        }

                        val sensorStates = deserializeToMap(state)

                        DetailsDeviceData(deviceId, deviceDefinition.deviceName, sensorStates).let {data ->
                            call.respond(HttpStatusCode.OK, ClientManagementResponseData(deviceDataWasSent, data))
                            logger.writeLog(deviceDataWasSent, "$userId", SenderType.ID)
                        }

                    } ?: run {
                        call.respond(HttpStatusCode.NonAuthoritativeInformation, ClientManagementResponseData(authorizationError))
                        logger.writeLog(authorizationError, call.request.origin.remoteHost, SenderType.IP_ADDRESS)
                    }
                }
                post("/change/{id}") {
                    val deviceId = call.parameters["id"]!!.toULong()
                    val data = call.receive<ChangeDeviceData>()

                    call.principal<JWTPrincipal>()?.payload?.let {payload ->
                        val userId = payload.getClaim("id").asLong()

                        val deviceData = userDeviceDao.get(deviceId) ?: run {
                            logger.writeLog(clientHasNotSuchDevice, "$userId", SenderType.ID)
                            return@post call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(clientHasNotSuchDevice))
                        }


                        deviceStructureDao.getSensorType(deviceId, data.sensor)?.let {sensorType ->

                            val state = getUpdateState(sensorType, data.state, data.sensor, deviceData.state) ?: run {
                                call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(typeOfSensorStateIsNotCorrect))
                                logger.writeLog(typeOfSensorStateIsNotCorrect, "$userId", SenderType.ID)
                                return@post
                            }

                            if (!userDeviceDao.updateState(deviceId, state)) {
                                call.respond(HttpStatusCode.InternalServerError, ClientManagementResponseData(deviceStateWasNotBeenUpdated))
                                logger.writeLog(deviceStateWasNotBeenUpdated, "$userId", SenderType.ID)
                                return@post
                            }

                            kredsClient.use {redis ->
                                "${deviceData.boardId}:updateStatement".let {
                                    redis.set(it, "${data.sensor}:${data.state}")
                                    redis.expire(it, 3600U)
                                }
                            }

                            call.respond(HttpStatusCode.OK, ClientManagementResponseData(deviceStateHasBeenUpdated))
                            logger.writeLog(deviceStateHasBeenUpdated, "$userId", SenderType.ID)

                        } ?: run {
                            logger.writeLog(sensorIsNotExists, "$userId", SenderType.ID)
                            call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(sensorIsNotExists))
                        }

                    } ?: run {
                        call.respond(HttpStatusCode.NonAuthoritativeInformation, ClientManagementResponseData(authorizationError))
                        logger.writeLog(authorizationError, call.request.origin.remoteHost, SenderType.IP_ADDRESS)
                    }
                }
            }
        }
    }
}
