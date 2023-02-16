package com.iotserv.utils.validators

import com.iotserv.dto.EmailData
import com.iotserv.dto.VerificationCodeData
import com.iotserv.utils.RoutesResponses
import com.iotserv.utils.RoutesResponses.incorrectCodeFormat
import io.ktor.server.plugins.requestvalidation.*

fun validateVerificationCodeData(data: VerificationCodeData): ValidationResult {
    if (data.code.toString().length != 6)
        return ValidationResult.Invalid(RoutesResponses.incorrectCodeLength)

    data.code.toString().forEach {
        if (!Character.isDigit(it)) {
            return ValidationResult.Invalid(incorrectCodeFormat)
        }
    }

    isEmailAvailable(data.email)?.let {
        return ValidationResult.Invalid(it)
    } ?: return ValidationResult.Valid
}

fun validateEmailData(data: EmailData): ValidationResult {
    isEmailAvailable(data.email)?.let {
        return ValidationResult.Invalid(it)
    } ?: return ValidationResult.Valid
}
