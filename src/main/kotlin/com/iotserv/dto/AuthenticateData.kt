package com.iotserv.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticateResponseData (
    val message: String,
    val accessToken: String? = null,
)