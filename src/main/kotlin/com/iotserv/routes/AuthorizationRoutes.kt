package com.iotserv.routes

import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dto.*
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses.authorizationHasBeenCompleted
import com.iotserv.utils.RoutesResponses.codeIsWrongOrNotVerified
import com.iotserv.utils.RoutesResponses.dataHasBeenAccepted
import com.iotserv.utils.RoutesResponses.userAlreadyExists
import com.iotserv.utils.RoutesResponses.userHasNotBeenAdded
import com.iotserv.utils.RoutesResponses.userIdWasNotFound
import com.iotserv.utils.RoutesResponses.userNotFound
import com.iotserv.utils.RoutesResponses.userNotFoundOrPasswordIsIncorrect
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.authorizationRoutes() {
    val jwtCooker by inject<JwtCooker>()
    val personalDataManagementDao by inject<PersonalDataManagement>()
    val kredsClient by inject<KredsClient>()

    route("/account") {
        route("/register") {
            post {
                val data = call.receive<RegistrationData>()

                if (personalDataManagementDao.isUserExists(data))
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthorizationResponseData(userAlreadyExists)
                    )

                else {
                    kredsClient.use {redis->
                        if (redis.get("${data.email}:authorization") != "true")
                            return@post call.respond(HttpStatusCode.Unauthorized, AuthorizationResponseData(codeIsWrongOrNotVerified))
                        else
                            redis.del("${data.email}:authorization")
                    }
                    personalDataManagementDao.writeNewUser(
                        RegistrationData(
                            data.number,
                            data.email,
                            data.password
                        )
                    )?.let {userId->

                        call.respond(
                            HttpStatusCode.Accepted,
                            AuthorizationResponseData(
                                dataHasBeenAccepted,
                                jwtCooker.buildToken(userId.toLong())
                            )
                        )

                    } ?: call.respond(AuthorizationResponseData(userHasNotBeenAdded))

                }
            }
        }

        route("/login") {
            post {
                val data= call.receive<LoginData>()

                personalDataManagementDao.verifyUserLoginAndPwd(data)?.let {
                    call.respond(
                        HttpStatusCode.Accepted,
                        AuthorizationResponseData(
                            authorizationHasBeenCompleted,
                            jwtCooker.buildToken(it.toLong())
                        )
                    )
                } ?: call.respond(
                    HttpStatusCode.BadRequest,
                    AuthorizationResponseData(userNotFoundOrPasswordIsIncorrect)
                )
            }
        }

        route("/password/reset") {
            post {
                val data = call.receive<EmailData>()

                if (!personalDataManagementDao.isUserExists(data.email)) {
                    call.respond(HttpStatusCode.BadRequest, AuthorizationResponseData(userNotFound))
                } else {
                    kredsClient.use {redis->
                        if (redis.get("${data.email}:authorization") != "true")
                            return@post call.respond(HttpStatusCode.Unauthorized, AuthorizationResponseData(codeIsWrongOrNotVerified))
                        else
                            redis.del("${data.email}:authorization")
                    }

                    val id = personalDataManagementDao.getId(data)
                        ?: return@post call.respond(
                            HttpStatusCode.InternalServerError,
                            AuthorizationResponseData(userIdWasNotFound)
                        )

                    call.respond(
                        HttpStatusCode.Accepted,
                        AuthorizationResponseData(dataHasBeenAccepted, jwtCooker.buildToken(id.toLong()))
                    )
                }
            }
        }
    }
}