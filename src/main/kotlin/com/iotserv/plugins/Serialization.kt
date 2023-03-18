package com.iotserv.plugins

import io.bkbn.kompendium.oas.serialization.KompendiumSerializersModule
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                isLenient = true
                ignoreUnknownKeys = true
                serializersModule = KompendiumSerializersModule.module
            }
        )
    }
}
