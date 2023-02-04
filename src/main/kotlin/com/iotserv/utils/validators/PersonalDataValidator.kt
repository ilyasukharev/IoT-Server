package com.iotserv.utils.validators

import com.iotserv.dto.PasswordData
import com.iotserv.dto.PersonalData
import com.iotserv.dto.RegistrationData
import io.ktor.server.plugins.requestvalidation.*

fun validatePersonalData(data: PersonalData): ValidationResult =
    validateRegistrationData(RegistrationData(data.number, data.email, data.password))

fun validatePasswordData(data: PasswordData): ValidationResult {
    isPasswordAvailable(data.password)?.let {
        return ValidationResult.Invalid(it)
    } ?: return ValidationResult.Valid
}
