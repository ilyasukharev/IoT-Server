package com.iotserv.plugins

import com.iotserv.dto.*
import com.iotserv.utils.validators.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*


fun Application.configureValidator() {
    install(RequestValidation) {
        validate<RegistrationData> {
            validateRegistrationData(it)
        }
        validate<LoginData> {
            validateLoginData(it)
        }
        validate<PersonalData> {
            validatePersonalData(it)
        }
        validate<EmailData> {
            validateEmailData(it)
        }
        validate<VerificationCodeData> {
            validateVerificationCodeData(it)
        }
    }
}