package com.iotserv.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

interface JwtCooker {
    fun buildToken(id: Long) : String
    fun verifyToken() : JWTVerifier
    fun validate(jwtCredential: JWTCredential) : JWTPrincipal
}

class JwtCookerImpl : JwtCooker, KoinComponent {

    private val environment by inject<ApplicationEnvironment>()

    private val audience = environment.config.property("jwt.audience").getString()
    private val issuer = environment.config.property("jwt.issuer").getString()
    private val secret = environment.config.property("jwt.secret").getString()
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