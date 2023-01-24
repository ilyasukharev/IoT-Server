package com.iotserv.routes

import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dto.AuthorizationResponseData
import com.iotserv.dto.PasswordData
import com.iotserv.dto.PersonalData
import com.iotserv.dto.PersonalResponseData
import com.iotserv.utils.JwtCooker
import com.iotserv.utils.RoutesResponses
import com.iotserv.utils.RoutesResponses.codeIsWrongOrNotVerified
import com.iotserv.utils.RoutesResponses.dataWasSuccessfullyChanged
import com.iotserv.utils.RoutesResponses.passwordWasSuccessfullyChanged
import com.iotserv.utils.RoutesResponses.userIdWasNotFound
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.personalRoutes() {
    val personalDataManagementDao by inject<PersonalDataManagement>()
    val jwtCooker by inject<JwtCooker>()
    val kredsClient by inject<KredsClient>()

    authenticate("desktop-app") {
        route("/account/change") {
            post("/password") {
                val data = call.receive<PasswordData>()

                call.principal<JWTPrincipal>()?.payload?.let { payload ->
                    val id = payload.getClaim("id").asLong()

                    if (!personalDataManagementDao.updatePassword(id.toULong(), data))
                        call.respond(HttpStatusCode.InternalServerError, PersonalResponseData(userIdWasNotFound))
                    else
                        call.respond(
                            HttpStatusCode.Accepted,
                            PersonalResponseData(passwordWasSuccessfullyChanged, jwtCooker.buildToken(id))
                        )


                } ?: call.respond(
                    HttpStatusCode.NonAuthoritativeInformation,
                    PersonalResponseData(RoutesResponses.authorizationError)
                )
            }
            post {
                val data = call.receive<PersonalData>()

                kredsClient.use { redis ->
                    if (redis.get("${data.email}:authorization") != "true")
                        return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            AuthorizationResponseData(
                                codeIsWrongOrNotVerified
                            )
                        )
                    else
                        redis.del("${data.email}:authorization")
                }

                call.principal<JWTPrincipal>()?.payload?.let { payload ->
                    val id = payload.getClaim("id").asLong()

                    if (!personalDataManagementDao.updateAll(id.toULong(), data))
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            PersonalResponseData(userIdWasNotFound)
                        )
                    else {
                        call.respond(
                            HttpStatusCode.Accepted,
                            PersonalResponseData(dataWasSuccessfullyChanged, jwtCooker.buildToken(id))
                        )
                    }

                } ?: call.respond(
                    HttpStatusCode.NonAuthoritativeInformation,
                    PersonalResponseData(RoutesResponses.authorizationError)
                )
            }
        }
    }
}