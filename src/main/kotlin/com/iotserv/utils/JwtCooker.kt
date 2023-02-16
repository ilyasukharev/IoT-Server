package com.iotserv.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.iotserv.dao.personal_data.PersonalDataManagement
import com.typesafe.config.ConfigFactory
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

interface JwtCooker {
    fun buildAccessJwt(id: Long): String
    fun buildRefreshJwt(id: Long): String
    fun verifyToken(): JWTVerifier
    suspend fun validate(jwtCredential: JWTCredential): Boolean
}

class JwtCookerImpl : JwtCooker, KoinComponent {
    private val personalDataDao by inject <PersonalDataManagement> ()
    private val config = HoconApplicationConfig(ConfigFactory.load())

    private val audience = config.property("jwt.audience").getString()
    private val issuer = config.property("jwt.issuer").getString()
    private val secret = config.property("jwt.secret").getString()

    private val accessExpireTime = 1800000
    private val refreshExpireTime = 172800000

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

    override suspend fun validate(jwtCredential: JWTCredential): Boolean {
        jwtCredential.payload.getClaim("id")?.asLong()?.let {userId ->
            return personalDataDao.isUserExists(userId.toULong())
        } ?: return false
    }
}