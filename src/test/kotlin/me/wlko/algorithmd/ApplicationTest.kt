package me.wlko.algorithmd

import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.auth.*
import io.ktor.util.*
import io.ktor.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.features.*
import org.slf4j.event.*
import io.ktor.gson.*
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.server.testing.*
import me.wlko.algorithmd.plugins.*
import org.junit.Assert.assertEquals
import org.junit.Test

fun ApplicationEnvironment.config(values: Map<String, String>) {
    (config as MapApplicationConfig).apply {
        values.forEach { (path, value) ->
            put(path, value)
        }
    }
}

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({
            environment.config(mapOf(
                "jwt.audience" to "https://api.algorithmd.wlko.me",
                "jwt.domain" to "https://algorithmd.eu.auth0.com/",
                "jwt.realm" to "Test server"
            ))
            configureSecurity()
            configureRouting()
        }) {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }
}