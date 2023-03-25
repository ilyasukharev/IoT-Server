package com.iotserv.plugins

import com.iotserv.exceptions.*
import io.github.crackthecodeabhi.kreds.connection.KredsConnectionException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respondText(status = HttpStatusCode.BadRequest, text = cause.reasons.single())
        }
        exception<KredsConnectionException> { call, cause ->
            call.respondText(status = HttpStatusCode.InternalServerError, text = cause.message.toString())
        }
        exception<NumberFormatException> {call, cause ->
            call.respondText(status = HttpStatusCode.BadRequest, text = cause.message.toString())
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.getFullDescription())
        }
        exception<TokenException> {call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.getFullDescription())
        }
        exception<ExposedException> {call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.getFullDescription())
        }
        exception<MailDeliverException> {call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.getFullDescription())
        }
        exception<OtherException> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.getFullDescription())
        }
    }
}