package me.wlko.algorithmd.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.auth.*
import io.ktor.util.*
import io.ktor.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import java.util.concurrent.TimeUnit

fun Application.configureSecurity() {

    authentication {
        jwt {
            val jwkAudience = environment.config.property("jwt.audience").getString()
            val jwkIssuer = environment.config.property("jwt.domain").getString()
            val jwkProvider = JwkProviderBuilder(jwkIssuer)
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(5, 1, TimeUnit.MINUTES)
                .build()
            realm = environment.config.property("jwt.realm").getString()
            verifier(jwkProvider, jwkIssuer)
            validate { credential ->
                if (credential.payload.audience.contains(jwkAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }

}

fun ApplicationCall.jwtSubject(): String {
    return (authentication.principal as JWTPrincipal).payload.subject
}