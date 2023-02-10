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
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.authorizationError
import com.iotserv.utils.RoutesResponses.clientHasNotSuchDevice
import com.iotserv.utils.RoutesResponses.deviceDataWasSent
import com.iotserv.utils.RoutesResponses.deviceStateHasBeenUpdated
import com.iotserv.utils.RoutesResponses.deviceStateWasNotBeenUpdated
import com.iotserv.utils.RoutesResponses.deviceWasNotFound
import com.iotserv.utils.RoutesResponses.idIsNotFound
import com.iotserv.utils.RoutesResponses.listIsEmpty
import com.iotserv.utils.RoutesResponses.listWasArrived
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
    val jwtCooker by inject<JwtCooker>()
    val deviceDefinitionDao by inject<DeviceDefinitionManagement>()
    val kredsClient by inject<KredsClient>()

    route("/management") {
        authenticate ("desktop-app"){
            route("/user/sensors") {
                get {
                    call.principal<JWTPrincipal>()?.payload?.let { payload ->
                        val id = payload.getClaim("id").asLong()
                        val token = jwtCooker.buildToken(id)

                        userDeviceDao.getAll(id.toULong()).let {list ->
                            if (list.isEmpty()) {
                                call.respond(HttpStatusCode.InternalServerError, ClientManagementResponseData(listIsEmpty, token = token))
                                logger.writeLog(listIsEmpty, "$id", SenderType.ID)
                                return@get
                            }
                            val deviceDefinitionsList = mutableListOf<CommonDeviceData>()

                            repeat (list.size) {
                                val deviceInfo = deviceDefinitionDao.getDeviceInfo(list[it].deviceId) ?: run {
                                    call.respond(HttpStatusCode.InternalServerError, ClientManagementResponseData(deviceWasNotFound, token = token))
                                    logger.writeLog(deviceWasNotFound, "$id", SenderType.ID)
                                    return@get
                                }
                                deviceDefinitionsList.add (CommonDeviceData(list[it].deviceId, deviceInfo.deviceName, deviceInfo.deviceDescription))
                            }

                            call.respond(HttpStatusCode.OK, ClientManagementResponseData(listWasArrived, deviceListInfo = deviceDefinitionsList, token = token))
                            logger.writeLog(listWasArrived, "$id", SenderType.ID)
                        }

                    } ?: run {
                        call.respond(HttpStatusCode.NonAuthoritativeInformation, ClientManagementResponseData(authorizationError))
                        logger.writeLog(authorizationError, call.request.origin.remoteHost, SenderType.IP_ADDRESS)
                    }
                }
                get("/{id?}") {
                    val deviceId = call.parameters["id"]?.toULong() ?: run {
                        call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(idIsNotFound))
                        logger.writeLog(idIsNotFound, call.request.local.remoteHost, SenderType.ID)
                        return@get
                    }

                    call.principal<JWTPrincipal>()?.payload?.let { payload ->
                        val userId = payload.getClaim("id").asLong()
                        val token = jwtCooker.buildToken(userId)

                        if (!userDeviceDao.isExists(userId.toULong(), deviceId)) {
                            call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(clientHasNotSuchDevice, token = token))
                            logger.writeLog(clientHasNotSuchDevice, "$userId", SenderType.ID)
                            return@get
                        }

                        if (!deviceDefinitionDao.isExists(deviceId)) {
                            call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(deviceWasNotFound, token = token))
                            logger.writeLog(deviceWasNotFound, "$userId", SenderType.ID)
                            return@get
                        }

                        val state = userDeviceDao.get(deviceId)!!.state
                        val deviceDefinition = deviceDefinitionDao.getDeviceInfo(deviceId)!!
                        val sensorStates = deserializeToMap(state)

                        DetailsDeviceData(deviceId, deviceDefinition.deviceName, sensorStates).let {data ->
                            call.respond(HttpStatusCode.OK, ClientManagementResponseData(deviceDataWasSent, data, token = token))
                            logger.writeLog(deviceDataWasSent, "$userId", SenderType.ID)
                        }

                    } ?: run {
                        call.respond(HttpStatusCode.NonAuthoritativeInformation, ClientManagementResponseData(authorizationError))
                        logger.writeLog(authorizationError, call.request.origin.remoteHost, SenderType.IP_ADDRESS)
                    }

                }
                post("/change") {
                    val data = call.receive<ChangeDeviceData>()

                    call.principal<JWTPrincipal>()?.payload?.let {payload ->
                        val userId = payload.getClaim("id").asLong()
                        val token = jwtCooker.buildToken(userId)

                        if (!userDeviceDao.isExists(userId.toULong(), data.deviceId)) {
                            call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(clientHasNotSuchDevice, token = token))
                            logger.writeLog(clientHasNotSuchDevice, "$userId", SenderType.ID)
                            return@post
                        }
                        val deviceState = userDeviceDao.get(data.deviceId)!!.state

                        if (!deviceStructureDao.isSensorExists(data.deviceId, data.sensor)) {
                            call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(sensorIsNotExists, token = token))
                            logger.writeLog(sensorIsNotExists, "$userId", SenderType.ID)
                            return@post
                        }

                        deviceStructureDao.getSensorType(data.deviceId, data.sensor)!!.let {sensorType ->
                            val state = getUpdateState(sensorType, data.state, data.sensor, deviceState) ?: run {
                                call.respond(HttpStatusCode.BadRequest, ClientManagementResponseData(typeOfSensorStateIsNotCorrect, token = token))
                                logger.writeLog(typeOfSensorStateIsNotCorrect, "$userId", SenderType.ID)
                                return@post
                            }

                            if (!userDeviceDao.updateState(data.deviceId, state)) {
                                call.respond(HttpStatusCode.InternalServerError, ClientManagementResponseData(deviceStateWasNotBeenUpdated, token = token))
                                logger.writeLog(deviceStateWasNotBeenUpdated, "$userId", SenderType.ID)
                                return@post
                            }

                            kredsClient.use {redis ->
                                "$userId:updateStatement".let {
                                    redis.set(it, state)
                                    redis.expire(it, 3600U)
                                }
                            }
                            call.respond(HttpStatusCode.OK, ClientManagementResponseData(deviceStateHasBeenUpdated, token = token))
                            logger.writeLog(deviceStateHasBeenUpdated, "$userId", SenderType.ID)
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
