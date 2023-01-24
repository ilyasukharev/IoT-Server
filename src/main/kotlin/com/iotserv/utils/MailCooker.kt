package com.iotserv.utils

import io.ktor.server.application.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object MailCooker : KoinComponent{
    private val env by inject<ApplicationEnvironment>()

    fun generateRandomCode() : Int = (111111..999999).random()

    fun sendResetEmail(email: String, code: Int) {
        val hostProp = env.config.property("mail.hostname").getString()
        val smtpPortProp = env.config.property("mail.smtpport").getString().toInt()
        val userNameProp = env.config.property("mail.login").getString()
        val isSSLOnConnectProp = env.config.property("mail.isSSLOnConnect").getString().toBoolean()
        val userPasswordProp = env.config.property("mail.password").getString()
        val messageTitle = env.config.property("mail.messageTitle").getString()
        val messagePatternProp = env.config.property("mail.messagePattern").getString()

        SimpleEmail().apply {
            this.hostName = hostProp
            this.setSmtpPort(smtpPortProp)
            this.setAuthenticator(DefaultAuthenticator(userNameProp, userPasswordProp))
            this.isSSLOnConnect = isSSLOnConnectProp
            this.setFrom(userNameProp)
            this.subject = messageTitle
            this.setMsg(String.format(messagePatternProp, code))
            this.addTo(email)
            this.send()
        }
    }
}