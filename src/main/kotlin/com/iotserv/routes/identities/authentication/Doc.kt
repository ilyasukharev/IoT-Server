package com.iotserv.routes.identities.authentication

import com.iotserv.dto.AuthenticateResponseData
import com.iotserv.exceptions.CustomExceptionsData
import io.bkbn.kompendium.core.metadata.GetInfo
import io.bkbn.kompendium.core.plugin.NotarizedRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.documentation() {
    install(NotarizedRoute()) {
        tags = setOf("Идентификация")
        get = GetInfo.builder {
            summary("Получение апдейт - токена")
            description("Возвращает апдейт - токен при отправлении рефреш токена")
            response {
                description("Возвращает сам токен и сообщение об успешной отправке: 'Access token was updated'")
                responseCode(HttpStatusCode.OK)
                responseType<AuthenticateResponseData>()
            }
            canRespond {
                description("ET01, ET02, ET03")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}