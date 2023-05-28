package com.iotserv.routes.managements.board

import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.exceptions.ExposedException
import com.iotserv.exceptions.SocketException
import com.iotserv.plugins.stopBoardListening
import com.iotserv.plugins.traceClientActiveState
import com.iotserv.plugins.transactException
import com.iotserv.routes.connections.receiveJsonData
import com.iotserv.utils.DeviceSensorsHandler
import com.iotserv.utils.RoutesResponses.boardWasDeclined
import com.iotserv.utils.RoutesResponses.boardWasNotSubmit
import com.iotserv.utils.RoutesResponses.commandIsUnknown
import com.iotserv.utils.RoutesResponses.managingReady
import com.iotserv.utils.RoutesResponses.sendingBoardIDAccepted
import com.iotserv.utils.RoutesResponses.suchBoardIsListening
import com.iotserv.utils.RoutesResponses.suchBoardUUIDIsNotExists
import com.iotserv.utils.RoutesResponses.suchBoardUUIDIsNotExistsCode
import com.iotserv.utils.RoutesResponses.updateIsNull
import com.iotserv.utils.RoutesResponses.waitingClientSubmit
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import org.koin.ktor.ext.inject
import java.time.Duration
import kotlin.system.measureTimeMillis

fun Route.boardManagementRoutes() {
    val logger by inject<Logger>()
    val userDeviceDao by inject<UserDevice>()
    val kredsClient by inject<KredsClient>()
    var boardUUID = "Unknown board"

    route("/management") {
        webSocket("/board") {
            val ip = call.request.origin.remoteHost

            try {
                pingInterval = Duration.ofSeconds(350)

                incoming.receiveAsFlow().take(1).collect {frame ->
                    (frame as? Frame.Text)?.let {
                        if (it.readText() != "connect") throw SocketException (commandIsUnknown)

                    } ?: throw SocketException (commandIsUnknown)
                }


                send(sendingBoardIDAccepted)
                logger.writeLog(sendingBoardIDAccepted, ip, SenderType.IP_ADDRESS_BOARD)

                boardUUID = receiveJsonData(this, true, logger, ip, null).boardIdentificationData!!.boardUUID

                if (!userDeviceDao.isBoardIdExists(boardUUID)) {
                    throw ExposedException(suchBoardUUIDIsNotExistsCode, suchBoardUUIDIsNotExists)
                }

                val clientId = userDeviceDao.getBoardOwner(boardUUID)
                var submitted = false
                "$clientId:requestDeviceSubmit".let{key ->
                    kredsClient.use { redis ->
                        redis.set(key, "false")
                        redis.expire(key, 300U)
                    }
                }


                send(waitingClientSubmit)
                logger.writeLog(waitingClientSubmit, ip, SenderType.IP_ADDRESS_BOARD)


                repeat(300) {
                    val requestTime = measureTimeMillis {
                        "$clientId:requestDeviceSubmit".let {key ->
                            kredsClient.use {redis ->
                                val res = redis.get(key)

                                if (res == "accepted") {
                                    submitted = true
                                    return@repeat
                                }
                                else if (res == "declined") {
                                    throw SocketException(boardWasDeclined)
                                }
                            }
                        }
                    }
                    delay(1000 - requestTime)
                }

                if (!submitted) throw SocketException(boardWasNotSubmit)

                kredsClient.use {redis ->
                    "$boardUUID:isListening".let {key ->
                        if (redis.get(key) == "true") {
                            throw SocketException(suchBoardIsListening)
                        } else {
                            redis.set(key, "true")
                        }
                    }
                }

                userDeviceDao.getByBoardId(clientId, boardUUID).let{data->
                    val sensorsMap = DeviceSensorsHandler.deserializeToMap(data.state)

                    DeviceSensorsHandler.updateToDefaultValues(sensorsMap).let {newState ->
                        userDeviceDao.updateState(boardUUID, newState)
                    }
                }

                pingInterval = null
                logger.writeLog(managingReady, ip, SenderType.IP_ADDRESS_BOARD)
                send(managingReady)

                val channel = Channel<String>()
                launch { traceClientActiveState(channel, this@webSocket, boardUUID, kredsClient) }

                incoming.receiveAsFlow().collect {
                    it as? Frame.Text ?: throw SocketException (commandIsUnknown)

                    when (it.readText()) {
                        "update" -> {
                            kredsClient.use {redis ->
                                val value = redis.get("$boardUUID:updateStatement") ?: return@collect send(updateIsNull)
                                redis.del("$boardUUID:updateStatement")
                                send(value)
                            }
                        }
                        "0" -> {
                            channel.send("0")
                        }
                    }
                }
            } catch (e: SocketException) {
                transactException(e.message!!, ip, logger, this)
            } catch (e: ExposedException){
                transactException(e.message, ip, logger, this)
            } catch (e: Exception) {
                transactException(e.message ?: "", ip, logger, this)
            } finally {
                stopBoardListening(kredsClient, boardUUID)
            }
        }
    }
}