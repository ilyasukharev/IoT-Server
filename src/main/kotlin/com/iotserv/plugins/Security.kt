package com.iotserv.plugins

import com.iotserv.exceptions.TokenException
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.tokenIsNotValidOrHasExpired
import com.iotserv.utils.RoutesResponses.tokenIsNotValidOrHasExpiredCode
import com.iotserv.utils.RoutesResponses.uuidWasNotFound
import com.iotserv.utils.RoutesResponses.uuidWasNotFoundCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val jwtCooker by inject<JwtCooker>()

    install(Authentication) {
        jwt("desktop-app") {
            verifier(jwtCooker.verifyToken())

            validate {
                if (it.payload.getClaim("id") != null)  JWTPrincipal(it.payload)
                else                                           throw TokenException(uuidWasNotFoundCode, uuidWasNotFound)
            }

            challenge { _, _ ->
                throw TokenException(tokenIsNotValidOrHasExpiredCode, tokenIsNotValidOrHasExpired)
            }
        }
    }
}