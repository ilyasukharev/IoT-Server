package com.iotserv.utils.validators

import com.iotserv.dto.VerifyCodeData
import com.iotserv.utils.RoutesResponses
import com.iotserv.utils.RoutesResponses.incorrectCodeFormat
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

fun validateVerificationCode(data: VerifyCodeData): ValidationResult {
   if (data.code.toString().length != 6)
       return ValidationResult.Invalid(RoutesResponses.incorrectCodeLength)

    data.code.toString().forEach {
        if (!Character.isDigit(it)) {
            return ValidationResult.Invalid(incorrectCodeFormat)
        }
    }

    isEmailAvailable(data.email)?.let {reason->
        return ValidationResult.Invalid(reason)
    }

    return ValidationResult.Valid
}
