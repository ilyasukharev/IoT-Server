package com.iotserv.plugins

import com.iotserv.routes.connections.connectionRoutes
import com.iotserv.routes.identities.authentication.configureAuthenticate
import com.iotserv.routes.identities.authorization.authorizationRoutes
import com.iotserv.routes.identities.verification.verificationCodeRoutes
import com.iotserv.routes.managements.board.boardManagementRoutes
import com.iotserv.routes.managements.client.clientManagementRoutes
import com.iotserv.routes.personal_data.personalDataRoutes
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.bkbn.kompendium.core.attribute.KompendiumAttributes.openApiSpec
import io.bkbn.kompendium.core.plugin.NotarizedApplication
import io.bkbn.kompendium.core.routes.redoc
import io.bkbn.kompendium.oas.OpenApiSpec
import io.bkbn.kompendium.oas.component.Components
import io.bkbn.kompendium.oas.info.Info
import io.bkbn.kompendium.oas.security.BasicAuth
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

suspend fun transactException(msg: String, ip: Any, logger: Logger, socket: WebSocketServerSession) {
    val senderType = when(ip) {
        is Long -> SenderType.ID
        else    -> SenderType.IP_ADDRESS_BOARD
    }

    logger.writeLog(msg, ip.toString(), senderType)
    socket.send(msg)
    socket.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, msg))
}

suspend fun isClientAuthenticated(redis: KredsClient, email: String): Boolean {
    redis.use {
        return it.get("${email}:authorization") == "true"
    }
}

fun Application.configureRouting() {
    install(Resources)
    install(NotarizedApplication()) {
        spec =  OpenApiSpec (
            openapi = "3.0.1",
            info = Info (
                title = "Iot documentation",
                version = "0.0.1"
            ),
            components = Components (
                securitySchemes = mutableMapOf("desktop-app" to BasicAuth())
            )
        )


        openApiJson = {
            route("/openapi.json") {
                get {
                    call.respond(
                        HttpStatusCode.OK,
                        this@route.application.attributes[openApiSpec]
                    )
                }
            }
        }
    }
    routing {
        redoc(pageTitle = "Iot - server API", path = "/docs")

        authorizationRoutes()
        personalDataRoutes()
        verificationCodeRoutes()
        connectionRoutes()
        clientManagementRoutes()
        boardManagementRoutes()
        configureAuthenticate()
    }
}
