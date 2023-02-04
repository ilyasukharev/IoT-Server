package com.iotserv.routes

import com.iotserv.dao.users_devices.UserDevice
import com.iotserv.utils.RoutesResponses.boardIdWasNotFound
import com.iotserv.utils.RoutesResponses.unknownSocketCommand
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.ktor.ext.inject

fun Route.boardManagement() {
    val logger by inject<Logger>()
    val userDeviceDao by inject<UserDevice>()

    route("/management") {
        webSocket("/board") {
            val ip = call.request.origin.remoteHost
            try {
                val boardUUID = receiveJsonData(this, true, logger, ip, null).boardIdentificationData!!.boardUUID

                if (!userDeviceDao.isBoardIdExists(boardUUID)) {
                    send(boardIdWasNotFound)
                }


                incoming.receiveAsFlow().collect {
                    (it as? Frame.Text)?.let { frame ->
                        when (frame.readText()) {
                            "" -> {

                            }

                            "" -> {

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
            }
        }
    }
}