package com.iotserv.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthorizationResponseData (
    val msg: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

@Serializable
data class RegistrationData(
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
data class VerificationCodeData(
    val email: String,
    val code: Int
)


