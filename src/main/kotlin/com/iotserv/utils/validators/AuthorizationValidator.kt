package com.iotserv.utils.validators

import com.iotserv.dto.*
import com.iotserv.utils.RoutesResponses
import io.ktor.server.plugins.requestvalidation.*

private fun isEmailAvailable (email: String): String? {
    return if (!email.contains("@") || !email.contains(".")) {
        RoutesResponses.incorrectEmailFormat
    }
    else if (email.length > 50) {
        RoutesResponses.incorrectEmailLength
    }
    else null
}

private fun isPasswordAvailable(pwd: String) : String? {
    return if (pwd.length < 8 || pwd.length > 50)
        RoutesResponses.incorrectPasswordLength
    else
        null
}

fun validateRegistrationData(data: RegistrationData) : ValidationResult {
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

fun validateLoginData(data: LoginData) : ValidationResult {
    isEmailAvailable(data.email)?.let {
        return ValidationResult.Invalid(it)
    }

    return ValidationResult.Valid
}

fun validateEmailData(data: EmailData) : ValidationResult =
    isEmailAvailable(data.email)?.let {
         ValidationResult.Invalid(it)
    } ?:  ValidationResult.Valid

fun validatePasswordData(data: PasswordData): ValidationResult =
    isPasswordAvailable(data.password)?.let {
        ValidationResult.Invalid(it)
    } ?: ValidationResult.Valid

