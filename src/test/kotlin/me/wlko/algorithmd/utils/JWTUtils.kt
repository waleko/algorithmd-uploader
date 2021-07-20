package me.wlko.algorithmd.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object JWTUtils {
    val algorithm = Algorithm.HMAC256("secret")
    val audience = "algorithmd-uploader-test"
    val issuer = "algorithmd-uploader"

    /**
     * Generates JWT for given [subject] for mocked authentication
     */
    fun getTokenForSubject(subject: String): String {
        return JWT.create()
            .withSubject(subject)
            .withAudience(audience)
            .withIssuer(issuer)
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .sign(algorithm)
    }
}

/**
 * Configures mock jwt authentication for testing
 */
fun Application.configureTestAuthentication() {
    authentication {
        jwt {
            verifier(
                JWT
                    .require(JWTUtils.algorithm)
                    .withAudience(JWTUtils.audience)
                    .withIssuer(JWTUtils.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(JWTUtils.audience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}