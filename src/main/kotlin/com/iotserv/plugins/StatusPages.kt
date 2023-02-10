package com.iotserv.plugins

import com.iotserv.utils.MailCooker
import com.iotserv.utils.RoutesResponses.redisIsNotConnect
import io.github.crackthecodeabhi.kreds.connection.KredsConnectionException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respondText(status = HttpStatusCode.BadRequest, text = cause.reasons.single())
        }
        exception<KredsConnectionException> { call, cause ->
            MailCooker.sendMessageWithServerWarnings(cause.message.toString())
            call.respondText(status = HttpStatusCode.InternalServerError, text = redisIsNotConnect)
        }
        exception<NumberFormatException> {call, cause ->
            call.respondText(status = HttpStatusCode.BadRequest, text = cause.message.toString())
        }
    }
}