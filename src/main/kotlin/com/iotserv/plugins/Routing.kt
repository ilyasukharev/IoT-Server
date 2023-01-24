package com.iotserv.plugins

import com.iotserv.routes.*
import io.ktor.server.routing.*
import io.ktor.server.application.*


fun Application.configureRouting() {
    routing {
        authorizationRoutes()
        personalRoutes()
        verificationCodeRoutes()
        connectionRoutes()
    }
}
