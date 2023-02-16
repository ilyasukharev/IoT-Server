package com.iotserv.routes

import com.iotserv.dto.AuthenticateResponseData
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.accessTokenWasUpdated
import com.iotserv.utils.RoutesResponses.authorizationError
import com.iotserv.utils.RoutesResponses.notEnoughAuthorizationInfo
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.configureAuthenticate() {
    val jwtCooker by inject<JwtCooker>()
    val logger by inject<Logger>()

    authenticate ("desktop-app") {
        route("/token") {
            get("/update") {
                call.principal<JWTPrincipal>()?.payload?.let {payload ->
                    val id = payload.getClaim("id").asLong()
                    val type = payload.getClaim("type")?.asString()

                    if (type == null || type != "refresh") {
                        logger.writeLog(notEnoughAuthorizationInfo, "$id", SenderType.ID)
                        call.respond(HttpStatusCode.NonAuthoritativeInformation, AuthenticateResponseData(notEnoughAuthorizationInfo))
                    } else {
                        logger.writeLog(accessTokenWasUpdated, "$id", SenderType.ID)
                        call.respond(HttpStatusCode.OK, AuthenticateResponseData(accessTokenWasUpdated, jwtCooker.buildAccessJwt(id)))
                    }

                } ?: run {
                    logger.writeLog(authorizationError, call.request.origin.remoteHost, SenderType.IP_ADDRESS)
                    call.respond(HttpStatusCode.NonAuthoritativeInformation, AuthenticateResponseData(authorizationError))
                }
            }
        }
    }

}