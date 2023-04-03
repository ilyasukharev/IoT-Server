package com.iotserv.routes.identities.authorization

import com.iotserv.dto.AuthorizationResponseData
import com.iotserv.dto.LoginData
import com.iotserv.dto.RegistrationData
import com.iotserv.exceptions.CustomExceptionsData
import io.bkbn.kompendium.core.metadata.PostInfo
import io.bkbn.kompendium.core.plugin.NotarizedRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.registerDoc() {
    install(NotarizedRoute()){
        tags = setOf("Идентификация")
        post = PostInfo.builder {
            summary("Регистрация пользователя в системе")
            description("Отправляя данные в теле пост запроса, система регистрирует " +
            "пользователя в системе. До регистрации необходимо пройти верификацию по имейлу.")
            request {
                description("Данные для регистрации")
                requestType<RegistrationData>()
            }
            response {
                description("Возвращает два токена: access и refresh")
                responseCode(HttpStatusCode.Accepted)
                responseType<AuthorizationResponseData>()
            }
            canRespond {
                description("EA03, EA04")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}
fun Route.loginDoc() {
    install(NotarizedRoute()) {
        tags = setOf("Идентификация")
        post = PostInfo.builder {
            summary("Авторизация пользователя в системе")
            description("Отправляя данные в теле пост запроса, система авторизует " +
                    "пользователя.")
            request {
                description("Данные для входа")
                requestType<LoginData>()
            }
            response {
                description("Возвращает два токена: access и refresh и сообщение 'Authorization has been completed'")
                responseCode(HttpStatusCode.Accepted)
                responseType<AuthorizationResponseData>()
            }
            canRespond {
                description("EA03, EA05")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}