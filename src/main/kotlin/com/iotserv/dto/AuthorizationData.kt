package com.iotserv.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthorizationResponseData (
    val msg: String,
    val token: String? = null
)

@Serializable
data class RegistrationData(
    val number: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginData(
    val email: String,
    val password: String,
)

@Serializable
data class EmailData(val email: String)

@Serializable
data class VerifyCodeData(
    val email: String,
    val code: Int
)


