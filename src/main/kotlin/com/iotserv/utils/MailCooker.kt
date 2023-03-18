package com.iotserv.utils

import com.iotserv.exceptions.MailDeliverException
import com.iotserv.utils.RoutesResponses.messageAlreadyBuilt
import com.iotserv.utils.RoutesResponses.messageAlreadyBuiltCode
import com.iotserv.utils.RoutesResponses.messageSendingException
import com.iotserv.utils.RoutesResponses.messageSendingExceptionCode
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail
import java.lang.IllegalStateException

object MailCooker {
    private val config = HoconApplicationConfig(ConfigFactory.load())

    private val hostProp = config.property("mail.hostname").getString()
    private val smtpPortProp = config.property("mail.smtpport").getString().toInt()
    private val userNameProp = config.property("mail.login").getString()
    private val isSSLOnConnectProp = config.property("mail.isSSLOnConnect").getString().toBoolean()
    private val userPasswordProp = config.property("mail.password").getString()
    private val messageTitle = config.property("mail.messageTitle").getString()
    private val messagePatternProp = config.property("mail.messagePattern").getString()

    fun generateRandomCode(): Int = (111111..999999).random()

    fun sendResetEmail(email: String, code: Int) {
        try {
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
        } catch (e: IllegalStateException) {
            throw MailDeliverException(messageAlreadyBuiltCode, messageAlreadyBuilt)
        } catch (e: EmailException) {
            throw MailDeliverException(messageSendingExceptionCode, messageSendingException)
        }


    }
}