package com.iotserv.routes

import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dto.*
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.attemptToLogin
import com.iotserv.utils.RoutesResponses.attemptToRegister
import com.iotserv.utils.RoutesResponses.authorizationHasBeenCompleted
import com.iotserv.utils.RoutesResponses.codeIsWrongOrNotVerified
import com.iotserv.utils.RoutesResponses.successfullyRegistered
import com.iotserv.utils.RoutesResponses.userAlreadyExists
import com.iotserv.utils.RoutesResponses.userHasNotBeenAdded
import com.iotserv.utils.RoutesResponses.userNotFoundOrPasswordIsIncorrect
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

suspend fun isClientAuthenticated(redis: KredsClient, email: String): Boolean {
    redis.use {
        if (it.get("${email}:authorization") != "true") return false
        else {
            it.del("${email}:authorization")
            return true
        }
    }
}

fun Route.authorizationRoutes() {
    val jwtCooker by inject<JwtCooker>()
    val personalDataManagementDao by inject<PersonalDataManagement>()
    val kredsClient by inject<KredsClient>()
    val logger by inject<Logger>()

    route("/account") {
        route("/register") {
            post {
                val data = call.receive<RegistrationData>()
                val clientIp = call.request.origin.remoteHost
                logger.writeLog(attemptToRegister, clientIp, SenderType.IP_ADDRESS)


                if (personalDataManagementDao.isUserExists(data)) {
                    logger.writeLog(userAlreadyExists, clientIp, SenderType.IP_ADDRESS)
                    call.respond(HttpStatusCode.BadRequest, AuthorizationResponseData(userAlreadyExists))
                } else {
                    if (!isClientAuthenticated(kredsClient, data.email)) {
                        logger.writeLog(codeIsWrongOrNotVerified, clientIp, SenderType.IP_ADDRESS)
                        return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            AuthorizationResponseData(codeIsWrongOrNotVerified)
                        )
                    }

                    personalDataManagementDao.writeNewUser(
                        RegistrationData(data.number, data.email, data.password)
                    )?.let { userId ->
                        call.respond(
                            HttpStatusCode.Accepted,
                            AuthorizationResponseData(successfullyRegistered, jwtCooker.buildToken(userId.toLong()))
                        )
                        logger.writeLog(successfullyRegistered, clientIp, SenderType.IP_ADDRESS)

                    } ?: run {
                        call.respond(AuthorizationResponseData(userHasNotBeenAdded))
                        logger.writeLog(userHasNotBeenAdded, clientIp, SenderType.IP_ADDRESS)
                    }
                }
            }
        }

        route("/login") {
            post {
                val data = call.receive<LoginData>()
                val clientIp = call.request.origin.remoteHost
                logger.writeLog(attemptToLogin, clientIp, SenderType.IP_ADDRESS)

                personalDataManagementDao.verifyUserLoginAndPwd(data)?.let {
                    call.respond(
                        HttpStatusCode.Accepted,
                        AuthorizationResponseData(authorizationHasBeenCompleted, jwtCooker.buildToken(it.toLong()))
                    )
                    logger.writeLog(authorizationHasBeenCompleted, clientIp, SenderType.IP_ADDRESS)
                } ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthorizationResponseData(userNotFoundOrPasswordIsIncorrect)
                    )
                    logger.writeLog(userNotFoundOrPasswordIsIncorrect, clientIp, SenderType.IP_ADDRESS)
                }
            }
        }

    }
}