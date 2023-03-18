package com.iotserv.utils.validators

import com.iotserv.dto.PersonalData
import com.iotserv.dto.RegistrationData
import io.ktor.server.plugins.requestvalidation.*

fun validatePersonalData(data: PersonalData): ValidationResult =
    validateRegistrationData(RegistrationData(data.email, data.password))