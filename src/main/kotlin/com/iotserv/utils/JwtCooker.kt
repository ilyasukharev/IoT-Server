package com.iotserv.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import org.koin.core.component.KoinComponent
import java.util.*

interface JwtCooker {
    fun buildAccessJwt(id: Long): String
    fun buildRefreshJwt(id: Long): String
    fun verifyToken(): JWTVerifier
}

class JwtCookerImpl : JwtCooker, KoinComponent {
    private val config = HoconApplicationConfig(ConfigFactory.load())

    private val audience = config.property("jwt.audience").getString()
    private val issuer = config.property("jwt.issuer").getString()
    private val secret = config.property("jwt.secret").getString()
    private val accessExpireTime = config.property("jwt.accessExpireTime").getString().toLong()
    private val refreshExpireTime = config.property("jwt.refreshExpireTime").getString().toLong()

    override fun buildAccessJwt(id: Long): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("id", id)
        .withExpiresAt(Date(System.currentTimeMillis() + accessExpireTime))
        .sign(Algorithm.HMAC256(secret))

    override fun buildRefreshJwt(id: Long): String = JWT.create()
    .withAudience(audience)
    .withIssuer(issuer)
    .withClaim("id", id)
    .withClaim("type", "refresh")
    .withExpiresAt(Date(System.currentTimeMillis() + refreshExpireTime))
    .sign(Algorithm.HMAC256(secret))

    override fun verifyToken(): JWTVerifier {
        return JWT.require(Algorithm.HMAC256(secret))
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
    }
}