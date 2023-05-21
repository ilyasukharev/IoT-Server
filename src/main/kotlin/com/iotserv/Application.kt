package com.iotserv

import com.iotserv.plugins.*
import com.iotserv.utils.DatabaseFactory
import io.ktor.server.application.*


fun main(args: Array<String>): Unit = io.ktor.server.jetty.EngineMain.main(args)


fun Application.module() {
    configureKoin()
    configureSecurity()
    configureSockets()
    configureRouting()
    configureSerialization()
    configureValidator()
    configureStatusPages()

    DatabaseFactory.initPostgreSQL(environment.config)
}
