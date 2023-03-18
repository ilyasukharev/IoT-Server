package com.iotserv.routes.identities.authentication

import com.iotserv.dto.AuthenticateResponseData
import com.iotserv.exceptions.TokenException
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.accessTokenWasUpdated
import com.iotserv.utils.RoutesResponses.tokenTypeIsMissing
import com.iotserv.utils.RoutesResponses.tokenTypeIsMissingCode
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.configureAuthenticate() {
    val jwtCooker by inject<JwtCooker>()
    val logger by inject<Logger>()

    authenticate ("desktop-app") {
        route("/token/update") {
            documentation()
            get {
                call.principal<JWTPrincipal>()!!.payload.let {payload ->
                    val id = payload.getClaim("id").asLong()
                    payload.getClaim("type")?.asString().let {
                        if (it == null || it != "refresh") {
                            logger.writeLog(tokenTypeIsMissing, "$id", SenderType.ID)
                            throw TokenException(tokenTypeIsMissingCode, tokenTypeIsMissing)
                        }
                    }

                    logger.writeLog(accessTokenWasUpdated, "$id", SenderType.ID)
                    call.respond(HttpStatusCode.OK, AuthenticateResponseData(accessTokenWasUpdated, jwtCooker.buildAccessJwt(id)))
                }
            }
        }
    }
}