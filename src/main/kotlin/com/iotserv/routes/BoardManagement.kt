package com.iotserv.routes

import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.utils.RoutesResponses.boardIdWasNotFound
import com.iotserv.utils.RoutesResponses.sendingBoardIDAccepted
import com.iotserv.utils.RoutesResponses.socketTimeout
import com.iotserv.utils.RoutesResponses.suchBoardIsListening
import com.iotserv.utils.RoutesResponses.unknownSocketCommand
import com.iotserv.utils.RoutesResponses.updateIsNull
import com.iotserv.utils.RoutesResponses.updateWasSent
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.ktor.ext.inject

//private suspend fun pingPongMechanism(socket: DefaultWebSocketServerSession, redis: KredsClient, boardId: String): Int {
//    //milliseconds
////    val pingInterval = 5000L
////    val pingTimeout = 5000L
////
////    val ping = "1"
////    val pong = "0"
////
////    while(true) {
////        while(socket.) {
////            println("Isnt empty")
////        }
////        println("is empty")
////    }
//}

fun Route.boardManagementRoutes() {
    val logger by inject<Logger>()
    val userDeviceDao by inject<UserDevice>()
    val kredsClient by inject<KredsClient>()
    var boardUUID = ""

    route("/management") {
        webSocket("/board") {
            pingInterval = null
            val ip = call.request.origin.remoteHost

            try {
                send(sendingBoardIDAccepted)
                logger.writeLog(sendingBoardIDAccepted, ip, SenderType.IP_ADDRESS_BOARD)

                boardUUID = receiveJsonData(this, true, logger, ip, null).boardIdentificationData!!.boardUUID

                if (!userDeviceDao.isBoardIdExists(boardUUID)) {
                    send(boardIdWasNotFound)
                    logger.writeLog(boardIdWasNotFound, ip, SenderType.IP_ADDRESS_BOARD)
                    return@webSocket
                }

                kredsClient.use {redis ->
                    "$boardUUID:isListening".let {key ->
                        if (redis.get(key) == "true") {
                            logger.writeLog(suchBoardIsListening, ip, SenderType.IP_ADDRESS_BOARD)
                            return@webSocket send(suchBoardIsListening)
                        } else {
                            redis.set(key, "true")
                        }
                    }
                }

//                withContext(Dispatchers.Default) {
//                    launch {
//                            pingPongMechanism(this@webSocket, kredsClient, boardUUID)
//                    }
//                }


                incoming.receiveAsFlow().collect {
                    (it as? Frame.Text)?.let { frame ->
                        when (frame.readText()) {
                            "update" -> {
                                kredsClient.use {redis ->
                                    "$boardUUID:updateStatement".let {key ->
                                        redis.get(key)?.let {statement ->
                                            send(statement)
                                            logger.writeLog(updateWasSent, ip, SenderType.IP_ADDRESS_BOARD)
                                            redis.del(key)
                                        } ?: send(updateIsNull)
                                    }
                                }
                            }

                            else -> {}
                        }

                    } ?: run {
                        send(unknownSocketCommand)
                        logger.writeLog(unknownSocketCommand, ip, SenderType.IP_ADDRESS_BOARD)
                        return@collect
                    }
                }
            } catch (e: Exception) {
                logger.writeLog(e.message.toString(), ip, SenderType.IP_ADDRESS_BOARD)
                kredsClient.use {redis ->
                    redis.del("$boardUUID:isListening")
                }
            }
        }
    }
}