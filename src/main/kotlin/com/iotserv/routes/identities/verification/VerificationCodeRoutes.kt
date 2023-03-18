package com.iotserv.routes.identities.verification

import  com.iotserv.dto.*
import com.iotserv.exceptions.AuthorizationException
import com.iotserv.utils.MailCooker
import com.iotserv.utils.RoutesResponses.codeIsRight
import com.iotserv.utils.RoutesResponses.codeIsWrong
import com.iotserv.utils.RoutesResponses.confirmCodeIsNotRight
import com.iotserv.utils.RoutesResponses.confirmCodeIsNotRightCode
import com.iotserv.utils.RoutesResponses.connectionTimeWasUp
import com.iotserv.utils.RoutesResponses.connectionTimeWasUpCode
import com.iotserv.utils.RoutesResponses.sendingEmailWithVerifyCode
import com.iotserv.utils.RoutesResponses.verifyCodeWasSent
import com.iotserv.utils.logger.Logger
import com.iotserv.utils.logger.SenderType
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun Route.verificationCodeRoutes() {
    val kredsClient by inject<KredsClient>()
    val logger by inject<Logger>()

    route("/code") {
        post("/send") {
            val email = call.receive<EmailData>().email

            MailCooker.generateRandomCode().let { generatedCode ->
                launch {
                    logger.writeLog(sendingEmailWithVerifyCode, call.request.origin.remoteHost, SenderType.IP_ADDRESS)
                    MailCooker.sendResetEmail(email, generatedCode)
                }

                kredsClient.use { redis ->
                    redis.set("$email:verificationCode", "$generatedCode")
                    redis.expire("$email:verificationCode", 1000U)
                }
                call.respond(HttpStatusCode.Accepted, AuthorizationResponseData(verifyCodeWasSent))
            }

        }
        post("/verify") {
            val data = call.receive<VerificationCodeData>()
            val clientIp = call.request.origin.remoteHost

            kredsClient.use { redis ->
                redis.get("${data.email}:verificationCode")
            }?.let { rightCode ->

                if (rightCode.toInt() != data.code) {
                    logger.writeLog(codeIsWrong, clientIp, SenderType.IP_ADDRESS)
                    throw AuthorizationException (confirmCodeIsNotRightCode, confirmCodeIsNotRight, listOf("Incorrect code: ${data.code}"))
                }

                kredsClient.use { redis ->
                    redis.del("${data.email}:verificationCode")
                    redis.set("${data.email}:authorization", "true")
                    redis.expire("${data.email}:authorization", 1000U)
                }

                logger.writeLog(codeIsRight, clientIp, SenderType.IP_ADDRESS)
                call.respond(HttpStatusCode.Accepted, AuthorizationResponseData(codeIsRight))

            } ?: run {
                logger.writeLog(connectionTimeWasUp, clientIp, SenderType.IP_ADDRESS)
                throw AuthorizationException(connectionTimeWasUpCode, connectionTimeWasUp)
            }
        }
    }
}