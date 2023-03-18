package com.iotserv.utils.validators

import com.iotserv.dto.ChangePasswordData
import com.iotserv.dto.PersonalData
import com.iotserv.dto.RegistrationData
import io.ktor.server.plugins.requestvalidation.*

fun validatePersonalData(data: PersonalData): ValidationResult =
    validateRegistrationData(RegistrationData(data.email, data.password))

fun validateChangePasswordData(data: ChangePasswordData): ValidationResult {
    isEmailAvailable(data.email)?.let {
        ValidationResult.Invalid(it)
    }

    isPasswordAvailable(data.password)?.let {
        return ValidationResult.Invalid(it)
    } ?: return ValidationResult.Valid
}
