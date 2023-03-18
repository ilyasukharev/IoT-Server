package com.iotserv.routes.identities.verification

import com.iotserv.dto.AuthorizationResponseData
import com.iotserv.dto.EmailData
import com.iotserv.dto.VerificationCodeData
import com.iotserv.exceptions.CustomExceptionsData
import io.bkbn.kompendium.core.metadata.PostInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.plugin.NotarizedRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.sendingCodeDoc() {
    install(NotarizedRoute()) {
        tags = setOf("Идентификация")
        post = PostInfo.builder {
            summary("Отправление верификационного кода")
            description("Отправление верификационного кода на имейл вложенный в тело запроса")
            request {
                description("Электронная почта")
                requestType<EmailData>()
            }
            response {
                description("Возвращает сообщение 'verify code was sent'")
                responseCode(HttpStatusCode.Accepted)
                responseType<AuthorizationResponseData>()
            }
            canRespond {
                description("EMD01, EMD02")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}

fun Route.verifyCodeDoc() {
    install(NotarizedRoute()) {
        tags = setOf("Идентификация")
        post = PostInfo.builder {
            summary("Потверждение верификационного кода")
            description("Потверждение действительного верификационного кода с кодом, отправленным в теле запроса")
            request {
                description("Электронная почта и код верификации")
                requestType<VerificationCodeData>()
            }
            response {
                description("Возвращает сообщение 'code is right'")
                responseCode(HttpStatusCode.Accepted)
                responseType<AuthorizationResponseData>()
            }
            canRespond (
                    listOf (
                        ResponseInfo.builder {
                            description("Код подтверждения не может быть конвертирован в число")
                            responseCode(HttpStatusCode.BadRequest)
                            responseType<String>()
                        },
                        ResponseInfo.builder {
                            description("EA01, EA02")
                            responseCode(HttpStatusCode.InternalServerError)
                            responseType<CustomExceptionsData>()
                        }
                    )
            )
        }
    }
}