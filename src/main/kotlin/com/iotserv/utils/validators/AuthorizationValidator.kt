package com.iotserv.utils.validators

import com.iotserv.dto.*
import com.iotserv.utils.RoutesResponses
import io.ktor.server.plugins.requestvalidation.*

fun validateRegistrationData(data: RegistrationData): ValidationResult {
    if (data.number.length < 11 || data.number.length > 11) {
        return ValidationResult.Invalid(RoutesResponses.incorrectNumberLength)
    }

    data.number.forEach {
        if (!Character.isDigit(it)) {
            return ValidationResult.Invalid(RoutesResponses.incorrectNumberFormat)
        }
    }

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

