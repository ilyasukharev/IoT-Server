package com.iotserv.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.config.ConfigFactory
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import java.util.*

interface JwtCooker {
    fun buildToken(id: Long) : String
    fun verifyToken() : JWTVerifier
    fun validate(jwtCredential: JWTCredential) : JWTPrincipal
}

class JwtCookerImpl : JwtCooker {
    private val config = HoconApplicationConfig(ConfigFactory.load())

    private val audience = config.property("jwt.audience").getString()
    private val issuer = config.property("jwt.issuer").getString()
    private val secret = config.property("jwt.secret").getString()
    private val tokenDuration: Long = 86400000    //24hours


    override fun buildToken(id: Long): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("id", id)
            .withExpiresAt(Date(System.currentTimeMillis() + tokenDuration))
            .sign(Algorithm.HMAC256(secret))
    }

    override fun verifyToken(): JWTVerifier {
        return JWT.require(Algorithm.HMAC256(secret))
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
    }

    override fun validate(jwtCredential: JWTCredential): JWTPrincipal {
        //TODO Проверка на существующий айди в базе
        return JWTPrincipal(jwtCredential.payload)
    }

}