package com.iotserv.exceptions

import kotlinx.serialization.Serializable

@Serializable
data class CustomExceptionsData (val code: String, val message: String, val optional: List<String>? = null)

abstract class CustomExceptions (
    private val code: String,
    override val message: String,
    private val optional: List<String>?
) : Exception(message) {
    fun getFullDescription() = CustomExceptionsData(code, message, optional)
}

class AuthorizationException(code: String, message: String, optional: List<String>? = null): CustomExceptions(code, message, optional)
class ExposedException(code: String, message: String, optional: List<String>? = null): CustomExceptions(code, message, optional)
class TokenException(code: String, message: String, optional: List<String>? = null): CustomExceptions(code, message, optional)
class MailDeliverException(code: String, message: String, optional: List<String>? = null): CustomExceptions(code, message, optional)
class SocketException(message: String): Exception(message)
class OtherException(code: String, message:String, optional: List<String>? = null): CustomExceptions(code, message, optional)