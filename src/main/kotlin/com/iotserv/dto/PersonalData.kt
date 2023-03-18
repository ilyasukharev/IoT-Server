package com.iotserv.dto

import kotlinx.serialization.Serializable


@Serializable
data class PersonalData (
    val email: String,
    val password: String
)

@Serializable
data class PersonalResponseData (
    val msg: String,
)