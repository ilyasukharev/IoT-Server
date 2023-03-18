package com.iotserv.utils.validators

import com.iotserv.dto.*
import com.iotserv.utils.RoutesResponses
import io.ktor.server.plugins.requestvalidation.*

fun validateRegistrationData(data: RegistrationData): ValidationResult {
    isEmailAvailable(data.email)?.let {
        return ValidationResult.Invalid(it)
    }

    isPasswordAvailable(data.password)?.let {
        return ValidationResult.Invalid(it)
    }

    return ValidationResult.Valid
}

fun validateLoginData(data: LoginData): ValidationResult {
    isEmailAvailable(data.email)?.let {
        return ValidationResult.Invalid(it)
    }

    isPasswordAvailable(data.password)?.let {
        return ValidationResult.Invalid(it)
    }

    return ValidationResult.Valid
}

