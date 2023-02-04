package com.iotserv

import com.iotserv.plugins.*
import com.iotserv.utils.DatabaseFactory
import io.ktor.server.application.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


fun Application.module() {
    configureSockets()
    configureRouting()
    configureSerialization()
    configureKoin()
    configureSecurity()
    configureValidator()
    configureStatusPages()

    DatabaseFactory.initPostgreSQL()
}

