package com.iotserv.plugins

import com.iotserv.utils.JwtCooker
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val jwtCooker by inject<JwtCooker>()

    install (Authentication) {
        jwt("desktop-app") {
            verifier(jwtCooker.verifyToken())
            validate {jwtCooker.validate(it)}


            challenge { _,_ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}