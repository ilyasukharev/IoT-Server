package com.iotserv.routes.managements.board

import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.exceptions.ExposedException
import com.iotserv.exceptions.SocketException
import com.iotserv.plugins.transactException
import com.iotserv.routes.connections.receiveJsonData
import com.iotserv.utils.RoutesResponses.commandIsUnknown
import com.iotserv.utils.RoutesResponses.sendingBoardIDAccepted
import com.iotserv.utils.RoutesResponses.socketTimeoutResponse
import com.iotserv.utils.RoutesResponses.suchBoardIsListening
import com.iotserv.utils.RoutesResponses.suchBoardUUIDIsNotExists
import com.iotserv.utils.RoutesResponses.suchBoardUUIDIsNotExistsCode
import com.iotserv.utils.RoutesResponses.updateIsNull
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

private suspend fun stopBoardListening(redis: KredsClient, boardUUID: String) {
    redis.use { it.del("$boardUUID:isListening") }
}

private suspend fun traceClientActiveState(channel: Channel<String>, socket: DefaultWebSocketServerSession, boardUUID: String, redis: KredsClient) {
    val ping = "1"
    val pong = "0"
    val timeoutResponse = 15000L
    val timeoutPing = 15000L

    while(true) {
        socket.send(ping)
        var x = 0L
        var isPong = false
        while(x < timeoutResponse && !isPong) {
            channel.tryReceive().getOrNull()?.let {
                if (it == pong) {
                    isPong = true
                }
                return@let
            }
            x += 1000
            delay(1000)
        }
        if (x >= timeoutResponse) {
            stopBoardListening(redis, boardUUID)
            socket.send(socketTimeoutResponse)
            socket.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, socketTimeoutResponse))
        }
        delay(timeoutPing)
    }
}

fun Route.boardManagementRoutes() {
    val logger by inject<Logger>()
    val userDeviceDao by inject<UserDevice>()
    val kredsClient by inject<KredsClient>()
    lateinit var boardUUID: String

    route("/management") {
        webSocket("/board") {
            val ip = call.request.origin.remoteHost

            try {
                pingInterval = null

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

                kredsClient.use {redis ->
                    "$boardUUID:isListening".let {key ->
                        if (redis.get(key) == "true") {
                            throw SocketException(suchBoardIsListening)
                        } else {
                            redis.set(key, "true")
                        }
                    }
                }

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
                stopBoardListening(kredsClient, boardUUID)
                transactException(e.message!!, ip, logger, this)
            } catch (e: ExposedException){
                stopBoardListening(kredsClient, boardUUID)
                transactException(e.message, ip, logger, this)
            } catch (e: Exception) {
                stopBoardListening(kredsClient, boardUUID)
                transactException(e.message ?: "", ip, logger, this)
            }
        }
    }
}