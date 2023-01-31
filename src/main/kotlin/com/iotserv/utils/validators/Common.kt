package com.iotserv.utils.validators

import com.iotserv.utils.RoutesResponses
import com.iotserv.utils.RoutesResponses.incorrectEmailFormat
import com.iotserv.utils.RoutesResponses.incorrectEmailLength
import com.iotserv.utils.RoutesResponses.incorrectPasswordLength

fun isEmailAvailable (email: String): String? {
    return if (!email.contains("@") || !email.contains(".")) {
        incorrectEmailFormat
    }
    else if (email.length > 50) {
        incorrectEmailLength
    }
    else null
}

fun isPasswordAvailable(pwd: String) : String? {
    return if (pwd.length < 8 || pwd.length > 50) incorrectPasswordLength
    else                                          null
}