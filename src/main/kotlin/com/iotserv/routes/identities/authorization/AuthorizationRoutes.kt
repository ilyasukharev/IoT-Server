package com.iotserv.routes.identities.authorization

import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dto.*
import com.iotserv.exceptions.AuthorizationException
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.authorizationHasBeenCompleted
import com.iotserv.utils.RoutesResponses.clientIsNotAuthenticated
import com.iotserv.utils.RoutesResponses.clientIsNotAuthenticatedCode
import com.iotserv.utils.RoutesResponses.successfullyRegistered
import com.iotserv.utils.RoutesResponses.userAlreadyExists
import com.iotserv.utils.RoutesResponses.userAlreadyExistsCode
import com.iotserv.utils.RoutesResponses.userNotFoundOrPasswordIsIncorrect
import com.iotserv.utils.RoutesResponses.userNotFoundOrPasswordIsIncorrectCode
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
        if (it.get("${email}:authorization") != "true") {
            return false
        } else {
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

                if (!isClientAuthenticated(kredsClient, data.email)) {
                    logger.writeLog(clientIsNotAuthenticated, clientIp, SenderType.IP_ADDRESS)
                    throw AuthorizationException(clientIsNotAuthenticatedCode, clientIsNotAuthenticated)
                }

                if (personalDataManagementDao.isUserExists(data.email)) {
                    logger.writeLog(userAlreadyExists, clientIp, SenderType.IP_ADDRESS)
                    throw AuthorizationException(userAlreadyExistsCode, userAlreadyExists)
                }

                personalDataManagementDao.writeNewUser(data).let { userId ->
                    logger.writeLog(successfullyRegistered, clientIp, SenderType.IP_ADDRESS)
                    call.respond (
                        HttpStatusCode.Accepted,
                        AuthorizationResponseData (
                            accessToken = jwtCooker.buildAccessJwt(userId),
                            refreshToken = jwtCooker.buildRefreshJwt(userId)
                        )
                    )
                }
            }
        }

        route("/login") {
            post {
                val data = call.receive<LoginData>()
                val clientIp = call.request.origin.remoteHost

                if (!personalDataManagementDao.isPasswordCorrect(data)) {
                    logger.writeLog(userNotFoundOrPasswordIsIncorrect, clientIp, SenderType.IP_ADDRESS)
                    throw AuthorizationException (
                        userNotFoundOrPasswordIsIncorrectCode,
                        userNotFoundOrPasswordIsIncorrect,
                        listOf("user: ${data.email}", "password: ${data.password}")
                    )
                }

                personalDataManagementDao.getId(data.email).let {userId ->
                    logger.writeLog(authorizationHasBeenCompleted, clientIp, SenderType.IP_ADDRESS)
                    call.respond(
                        HttpStatusCode.Accepted,
                        AuthorizationResponseData (
                            authorizationHasBeenCompleted,
                            jwtCooker.buildAccessJwt(userId),
                            jwtCooker.buildRefreshJwt(userId)
                        )
                    )
                }
            }
        }
    }
}