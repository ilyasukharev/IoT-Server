package com.iotserv.routes

import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dto.*
import com.iotserv.utils.RoutesResponses.authorizationError
import com.iotserv.utils.RoutesResponses.dataWasSuccessfullyChanged
import com.iotserv.utils.RoutesResponses.passwordWasSuccessfullyChanged
import com.iotserv.utils.RoutesResponses.userIsNotAuthenticated
import com.iotserv.utils.RoutesResponses.userNotFound
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.personalDataRoutes() {
    val personalDataManagementDao by inject<PersonalDataManagement>()
    val kredsClient by inject<KredsClient>()
    val logger by inject<Logger>()

    authenticate("desktop-app") {
        route("/account/change") {
            post {
                val data = call.receive<PersonalData>()
                val clientIp = call.request.origin.remoteHost

                call.principal<JWTPrincipal>()?.payload?.let { payload ->
                    val id = payload.getClaim("id").asLong()

                    if (!isClientAuthenticated(kredsClient, data.email)) {
                        logger.writeLog(userIsNotAuthenticated, "$id", SenderType.ID)
                        return@post call.respond(HttpStatusCode.Unauthorized, PersonalResponseData(userIsNotAuthenticated))
                    }

                    if (!personalDataManagementDao.updateAll(id.toULong(), data)) {
                        logger.writeLog(userNotFound, "$id", SenderType.ID)
                        return@post call.respond(HttpStatusCode.BadRequest, PersonalResponseData(userNotFound))
                    }

                    logger.writeLog(dataWasSuccessfullyChanged, "$id", SenderType.ID)
                    call.respond(HttpStatusCode.Accepted, PersonalResponseData(dataWasSuccessfullyChanged))

                } ?: run {
                    call.respond(HttpStatusCode.NonAuthoritativeInformation, PersonalResponseData(authorizationError))
                    logger.writeLog(authorizationError, clientIp, SenderType.IP_ADDRESS)
                }
            }
        }
    }

    route("/account/change/password") {
        post {
            val data = call.receive<ChangePasswordData>()
            val clientIp = call.request.origin.remoteHost

            if (!isClientAuthenticated(kredsClient, data.email)) {
                logger.writeLog(userIsNotAuthenticated, clientIp, SenderType.IP_ADDRESS)
                return@post call.respond(HttpStatusCode.Unauthorized, AuthorizationResponseData(userIsNotAuthenticated))
            }

            if (!personalDataManagementDao.updatePassword(data.email, data.password)) {
                logger.writeLog(userNotFound, clientIp, SenderType.ID)
                call.respond(HttpStatusCode.InternalServerError, PersonalResponseData(userNotFound))
            } else {
                logger.writeLog(passwordWasSuccessfullyChanged, clientIp, SenderType.IP_ADDRESS)
                call.respond(HttpStatusCode.OK, PersonalResponseData(passwordWasSuccessfullyChanged))
            }
        }
    }
}