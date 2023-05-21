package com.iotserv.routes.managements.client.rest

import com.iotserv.dto.ChangeDeviceData
import com.iotserv.dto.ClientManagementResponseData
import com.iotserv.exceptions.CustomExceptionsData
import io.bkbn.kompendium.core.metadata.GetInfo
import io.bkbn.kompendium.core.metadata.PutInfo
import io.bkbn.kompendium.json.schema.definition.TypeDefinition
import io.bkbn.kompendium.oas.payload.Parameter
import io.bkbn.kompendium.resources.NotarizedResource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.getDevicesDoc() {
    install(NotarizedResource<Devices>()) {
        tags = setOf("Управление клиентскими устройствами")
        get = GetInfo.builder {
            summary("Получение всех пользовательских устройств")
            description("Маршрут предназначен для получения всех пользовательских устройств")
            parameters = listOf(
                Parameter("offset", `in` = Parameter.Location.query, schema = TypeDefinition.LONG),
                Parameter("limit", `in` = Parameter.Location.query, schema = TypeDefinition.INT)
            )
            response {
                description("Возвращает список элементов")
                responseCode(HttpStatusCode.OK)
                responseType<ClientManagementResponseData>()
            }
            canRespond {
                description("ET01, ET02")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}

fun Route.getDetailDeviceDoc() {
    install(NotarizedResource<Devices.Id>()) {
        tags = setOf("Управление клиентскими устройствами")
        parameters = listOf (
            Parameter(name="id", `in` = Parameter.Location.path, schema = TypeDefinition.LONG)
        )
        get = GetInfo.builder {
            summary("Получение информации об устройстве")
            description("Получение детальной информации об устройстве")
            response {
                description("Возвращает информацию об устройстве")
                responseCode(HttpStatusCode.OK)
                responseType<ClientManagementResponseData>()
            }
            canRespond {
                description("ET01, ET02, EE01, EE04")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}


fun Route.getChangeDeviceDoc() {
    install(NotarizedResource<Devices.Id.Change>()) {
        tags = setOf("Управление клиентскими устройствами")
        parameters = listOf (
            Parameter(name="id", `in` = Parameter.Location.path, schema = TypeDefinition.LONG)
        )
        put = PutInfo.builder {
            summary("Изменение состояния устройства")
            description("Внесение изменений в текущее состояние пользовательского устройства")
            request {
                description("Изменяемые данные устройства")
                requestType<ChangeDeviceData>()
            }
            response {
                description("Возвращает сообщение 'device state has been updated'")
                responseCode(HttpStatusCode.OK)
                responseType<ClientManagementResponseData>()
            }
            canRespond {
                description("ET01, ET02, EE04, EE06, OE01, OE02")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}

fun Route.getResetDeviceStateDoc() {
    install(NotarizedResource<Devices.Id.Reset>()) {
        tags = setOf("Управление клиентскими устройствами")
        parameters = listOf (
            Parameter(name="id", `in` = Parameter.Location.path, schema = TypeDefinition.LONG)
        )
        get = GetInfo.builder {
            summary("Сброс состояния активности устройства")
            description("Принудительное прерывание состояния активного прослушивания устройства сервером")
            response {
                description("Возвращает сообщение 'Device listening state was reset'")
                responseCode(HttpStatusCode.OK)
                responseType<ClientManagementResponseData>()
            }
            canRespond {
                description("ET01, ET02, EE04")
                responseCode(HttpStatusCode.InternalServerError)
                responseType<CustomExceptionsData>()
            }
        }
    }
}