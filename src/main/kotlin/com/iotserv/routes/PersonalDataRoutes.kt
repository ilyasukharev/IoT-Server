package com.iotserv.routes

import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dto.*
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.attemptToChangePassword
import com.iotserv.utils.RoutesResponses.attemptToChangePersonalData
import com.iotserv.utils.RoutesResponses.attemptToResetPassword
import com.iotserv.utils.RoutesResponses.authorizationError
import com.iotserv.utils.RoutesResponses.codeIsWrong
import com.iotserv.utils.RoutesResponses.codeIsWrongOrNotVerified
import com.iotserv.utils.RoutesResponses.dataHasBeenAccepted
import com.iotserv.utils.RoutesResponses.dataWasSuccessfullyChanged
import com.iotserv.utils.RoutesResponses.passwordWasSuccessfullyChanged
import com.iotserv.utils.RoutesResponses.userIdWasNotFound
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
    val jwtCooker by inject<JwtCooker>()
    val kredsClient by inject<KredsClient>()
    val logger by inject<Logger>()

    authenticate("desktop-app") {
        route("/account") {
            route("/change") {
                post("/password") {
                    val data = call.receive<PasswordData>()
                    val clientIp = call.request.origin.remoteHost

                    call.principal<JWTPrincipal>()?.payload?.let { payload ->
                        val id = payload.getClaim("id").asLong()
                        logger.writeLog(attemptToChangePassword, "$id", SenderType.ID)

                        if (!personalDataManagementDao.updatePassword(id.toULong(), data)) {
                            logger.writeLog(userIdWasNotFound, "$id", SenderType.ID)
                            call.respond(HttpStatusCode.InternalServerError, PersonalResponseData(userIdWasNotFound))
                        } else {
                            logger.writeLog(passwordWasSuccessfullyChanged, "$id", SenderType.ID)
                            call.respond(
                                HttpStatusCode.Accepted,
                                PersonalResponseData(passwordWasSuccessfullyChanged, jwtCooker.buildToken(id))
                            )
                        }
                    } ?: run {
                        call.respond(
                            HttpStatusCode.NonAuthoritativeInformation,
                            PersonalResponseData(authorizationError)
                        )
                        logger.writeLog(authorizationError, clientIp, SenderType.IP_ADDRESS)
                    }
                }
                post {
                    val data = call.receive<PersonalData>()
                    val clientIp = call.request.origin.remoteHost

                    call.principal<JWTPrincipal>()?.payload?.let { payload ->
                        val id = payload.getClaim("id").asLong()
                        val token = jwtCooker.buildToken(id)

                        logger.writeLog(attemptToChangePersonalData, "$id", SenderType.ID)

                        if (!isClientAuthenticated(kredsClient, data.email)) {
                            logger.writeLog(codeIsWrongOrNotVerified, "$id", SenderType.ID)
                            return@post call.respond(
                                HttpStatusCode.Unauthorized,
                                PersonalResponseData(codeIsWrongOrNotVerified, token)
                            )
                        }

                        if (!personalDataManagementDao.updateAll(id.toULong(), data)) {
                            logger.writeLog(userIdWasNotFound, "$id", SenderType.ID)
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                PersonalResponseData(userIdWasNotFound, token)
                            )
                        } else {
                            logger.writeLog(dataWasSuccessfullyChanged, "$id", SenderType.ID)
                            call.respond(
                                HttpStatusCode.Accepted,
                                PersonalResponseData(dataWasSuccessfullyChanged, token)
                            )
                        }

                    } ?: run {
                        call.respond(
                            HttpStatusCode.NonAuthoritativeInformation,
                            PersonalResponseData(authorizationError)
                        )
                        logger.writeLog(authorizationError, clientIp, SenderType.IP_ADDRESS)
                    }
                }
            }
        }
    }

    route("account/password/reset") {
        post {
            val data = call.receive<EmailData>()
            val clientIp = call.request.origin.remoteHost
            logger.writeLog(attemptToResetPassword, clientIp, SenderType.IP_ADDRESS)

            if (!personalDataManagementDao.isUserExists(data.email)) {
                logger.writeLog(userNotFound, clientIp, SenderType.IP_ADDRESS)
                return@post call.respond(HttpStatusCode.BadRequest, AuthorizationResponseData(userNotFound))
            }

            if (!isClientAuthenticated(kredsClient, data.email)) {
                logger.writeLog(codeIsWrong, clientIp, SenderType.IP_ADDRESS)
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthorizationResponseData(codeIsWrongOrNotVerified)
                )
            }

            personalDataManagementDao.getId(data)?.let { userId ->
                logger.writeLog(dataHasBeenAccepted, clientIp, SenderType.IP_ADDRESS)
                call.respond(
                    HttpStatusCode.Accepted,
                    AuthorizationResponseData(dataHasBeenAccepted, jwtCooker.buildToken(userId.toLong()))
                )
            } ?: run {
                logger.writeLog(userIdWasNotFound, clientIp, SenderType.IP_ADDRESS)
                call.respond(HttpStatusCode.InternalServerError, AuthorizationResponseData(userIdWasNotFound))
            }
        }
    }
}