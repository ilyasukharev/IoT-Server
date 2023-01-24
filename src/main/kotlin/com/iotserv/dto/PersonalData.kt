package com.iotserv.dto

import kotlinx.serialization.Serializable

@Serializable
data class PasswordData (val password: String)

@Serializable
data class PersonalData(
    val email: String,
    val number: String,
    val password: String
)

@Serializable
data class PersonalResponseData (
    val msg: String,
    val token: String? = null
)