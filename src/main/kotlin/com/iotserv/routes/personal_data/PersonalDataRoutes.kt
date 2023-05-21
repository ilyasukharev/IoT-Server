package com.iotserv.routes.personal_data

import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dto.*
import com.iotserv.exceptions.AuthorizationException
import com.iotserv.exceptions.ExposedException
import com.iotserv.plugins.isClientAuthenticated
import com.iotserv.utils.RoutesResponses.clientIsNotAuthenticated
import com.iotserv.utils.RoutesResponses.clientIsNotAuthenticatedCode
import com.iotserv.utils.RoutesResponses.dataWasSuccessfullyChanged
import com.iotserv.utils.RoutesResponses.userNotFound
import com.iotserv.utils.RoutesResponses.userNotFoundCode
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
            changeUserData()

            put {
                val data = call.receive<PersonalData>()

                call.principal<JWTPrincipal>()!!.payload.let { payload ->
                    val id = payload.getClaim("id").asLong()

                    if (!personalDataManagementDao.updateAll(id, data)) {
                        logger.writeLog(userNotFound, "$id", SenderType.ID)
                        throw ExposedException(userNotFoundCode, userNotFound, listOf("userId: $id"))
                    }

                    logger.writeLog(dataWasSuccessfullyChanged, "$id", SenderType.ID)
                    call.respond(HttpStatusCode.OK, PersonalResponseData(dataWasSuccessfullyChanged))
                }
            }
        }
    }

    route("/account/change/password") {
        changeUserPassword()
        put {
            val data = call.receive<PersonalData>()
            val clientIp = call.request.origin.remoteHost

            if (!isClientAuthenticated(kredsClient, data.email)) {
                logger.writeLog(clientIsNotAuthenticated, clientIp, SenderType.IP_ADDRESS)
                throw AuthorizationException(clientIsNotAuthenticatedCode, clientIsNotAuthenticated)
            }

            if (!personalDataManagementDao.updatePassword(data.email, data.password)) {
                logger.writeLog(userNotFound, clientIp, SenderType.IP_ADDRESS)
                throw ExposedException(userNotFoundCode, userNotFound, listOf("userIp: $clientIp"))
            } else {
                logger.writeLog(dataWasSuccessfullyChanged, clientIp, SenderType.IP_ADDRESS)
                call.respond(HttpStatusCode.OK, PersonalResponseData(dataWasSuccessfullyChanged))
            }
        }
    }
}