package com.iotserv.utils.validators

import com.iotserv.utils.RoutesResponses.incorrectEmailFormat
import com.iotserv.utils.RoutesResponses.incorrectEmailLength
import com.iotserv.utils.RoutesResponses.incorrectPasswordFormat
import java.util.regex.Pattern

fun isEmailAvailable (email: String): String? {
    val emailPattern = Pattern.compile("^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")

    return if (!emailPattern.matcher(email).matches()) {
        incorrectEmailFormat
    }
    else if (email.length > 50) {
        incorrectEmailLength
    }
    else null
}

fun isPasswordAvailable(pwd: String) : String? {
    val passwordPattern = Pattern.compile("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,49}$")

    return if (!passwordPattern.matcher(pwd).matches()) incorrectPasswordFormat
    else                                                null
}