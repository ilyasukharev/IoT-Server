package com.iotserv.routes.managements.client.websocket

import  com.iotserv.exceptions.SocketException
import com.iotserv.plugins.traceClientActiveState
import com.iotserv.plugins.transactException
import com.iotserv.utils.RoutesResponses.clientConnected
import com.iotserv.utils.RoutesResponses.commandIsUnknown
import com.iotserv.utils.RoutesResponses.deviceWasDeclined
import com.iotserv.utils.RoutesResponses.deviceWasSubmitted
import com.iotserv.utils.RoutesResponses.submittingRequestReceived
import com.iotserv.utils.RoutesResponses.updateIsNull
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import java.time.Duration
import kotlin.system.measureTimeMillis

private suspend fun setState(state: String, clientId: Long, redis: KredsClient) {
    "$clientId:requestDeviceSubmit".let{key ->
        redis.use { redis ->
            redis.set(key, state)
            redis.expire(key, 90U)
        }
    }
}

private suspend fun deviceSubmitting(clientId: Long, redis: KredsClient, channel: Channel<String>, logger: Logger, socket: DefaultWebSocketServerSession) {
    repeat(90) {
        val time = measureTimeMillis {
            var response = channel.tryReceive().getOrNull()

            if (response != null) {
                redis.use { redis ->
                    if (redis.get("$clientId:requestDeviceSubmit") != "null") {
                        response = null
                    }
                }
            }

            if (response == "submit")  {
                setState("accepted", clientId, redis)

                socket.send(deviceWasSubmitted)
                logger.writeLog(deviceWasSubmitted, "$clientId", SenderType.ID)
            }
            else if (response == "decline") {
                setState("declined", clientId, redis)

                socket.send(deviceWasDeclined)
                logger.writeLog(deviceWasDeclined, "$clientId", SenderType.ID)
            }

        }
        delay(1000 - time)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Route.clientSocketManagement() {
    val kredsClient by inject<KredsClient>()
    val logger by inject<Logger>()
    authenticate("desktop-app") {
        webSocket("/management/app/updates") {
            pingInterval = null
            val clientId = call.principal<JWTPrincipal>()!!.payload.getClaim("id").asLong()

            try {
                pingInterval = Duration.ofSeconds(350)
                logger.writeLog(clientConnected, "$clientId", SenderType.ID)

                incoming.receiveAsFlow().take(1).collect {frame ->
                    (frame as? Frame.Text)?.let {
                        if (it.readText() != "connect") throw SocketException (commandIsUnknown)

                    } ?: throw SocketException (commandIsUnknown)
                }



                val pingChanel = Channel<String>()
                var clientResponseChanel = Channel<String>()

                pingInterval = null
                launch { traceClientActiveState(pingChanel, this@webSocket, null, kredsClient) }


                incoming.receiveAsFlow().collect {frame ->
                    frame as? Frame.Text ?: throw SocketException(commandIsUnknown)

                    when (frame.readText()) {
                        "update" -> {
                            kredsClient.use {redis ->
                                "$clientId:requestDeviceSubmit".let {key ->
                                    if (redis.get(key) == "false") {
                                        if (!clientResponseChanel.isEmpty) {
                                            clientResponseChanel = Channel()
                                        }
                                        redis.set(key, "null")
                                        launch { deviceSubmitting(clientId, kredsClient, clientResponseChanel, logger, this@webSocket) }
                                        send(submittingRequestReceived)
                                        logger.writeLog(submittingRequestReceived, "$clientId", SenderType.ID)
                                    } else {
                                        send(updateIsNull)
                                    }
                                }
                            }
                        }
                        "submit" -> clientResponseChanel.send("submit")
                        "decline" -> clientResponseChanel.send("decline")
                        "0"      -> pingChanel.send("0")
                    }

                }


            } catch (e: SocketException) {
                transactException(e.message ?: "", clientId, logger, this)
            } catch (e: Exception) {
                transactException(e.message ?: "", clientId, logger, this)
            }
        }
    }
}