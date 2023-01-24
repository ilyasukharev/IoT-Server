package com.iotserv.routes

import com.iotserv.dao.personal_data.PersonalDataManagement
import com.iotserv.dto.*
import com.iotserv.utils.MailCooker
import com.iotserv.utils.RoutesResponses.codeIsRight
import com.iotserv.utils.RoutesResponses.codeIsWrong
import com.iotserv.utils.RoutesResponses.connectionTimeWasUp
import com.iotserv.utils.RoutesResponses.exceptionEmailMessageSending
import com.iotserv.utils.RoutesResponses.sendingEmailException
import com.iotserv.utils.RoutesResponses.verifyCodeWasSent
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Route.verificationCodeRoutes() {
    val kredsClient by inject<KredsClient>()

    route("/code") {
        post("/send") {
            val email = call.receive<EmailData>().email

            try {
                MailCooker.generateRandomCode().let { randomCode ->
                    try {
                        MailCooker.sendResetEmail(email, randomCode)
                    } catch (e: Exception) {
                        return@post call.respond(HttpStatusCode.InternalServerError, AuthorizationResponseData(exceptionEmailMessageSending))
                    }
                    kredsClient.use { redis ->
                        redis.set("$email:verificationCode", "$randomCode")
                        redis.expire("$email:verificationCode", 1000U)
                    }
                    call.respond(HttpStatusCode.Accepted, AuthorizationResponseData(verifyCodeWasSent))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, AuthorizationResponseData(sendingEmailException))
            }

        }
        post("/verify") {
            val data = call.receive<VerifyCodeData>()

            kredsClient.use{redis->
                redis.get("${data.email}:verificationCode")
            }?.let {rightCode->
                if (rightCode.toInt() == data.code) {
                    kredsClient.use {redis->
                        redis.set("${data.email}:authorization", "true")
                        redis.expire("${data.email}:authorization", 1000U)
                    }
                    call.respond(HttpStatusCode.Accepted, AuthorizationResponseData(codeIsRight))
                }
                else call.respond(HttpStatusCode.BadRequest, AuthorizationResponseData(codeIsWrong))
            } ?: call.respond(HttpStatusCode.NotFound, AuthorizationResponseData(connectionTimeWasUp))
        }
    }
}