package com.iotserv.routes.personal_data

import com.iotserv.dto.PersonalData
import com.iotserv.dto.PersonalResponseData
import com.iotserv.exceptions.CustomExceptionsData
import io.bkbn.kompendium.core.metadata.PostInfo
import io.bkbn.kompendium.core.plugin.NotarizedRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.changeUserData() {
    install(NotarizedRoute()) {
        tags = setOf("Пользовательские данные")
        post = PostInfo.builder {
            summary("Изменение пользовательских данных")
            description("Изменяет пользовательские данные, хранящиеся в базе, на переданные в тело запроса")
            request {
                description("Имейл и пароль")
                requestType<PersonalData>()
            }
            response {
                description("Вернет сообщение 'Data was successfully changed'")
                responseCode(HttpStatusCode.OK)
                responseType<PersonalResponseData>()
            }
            canRespond {
                description("ET01, ET02, EE03")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}

fun Route.changeUserPassword() {
    install(NotarizedRoute()) {
        tags = setOf("Пользовательские данные")
        post = PostInfo.builder {
            summary("Сброс пользовательского пароля")
            description("Изменяет действующий пользовательский пароль на тот, что был передан в теле запроса")
            request {
                description("Имейл и пароль")
                requestType<PersonalData>()
            }
            response {
                description("Вернет сообщение 'Data was successfully changed'")
                responseCode(HttpStatusCode.OK)
                responseType<PersonalResponseData>()
            }
            canRespond {
                description("EA04, EE03")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}