package me.wlko.algorithmd

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import me.wlko.algorithmd.plugins.configureRouting
import me.wlko.algorithmd.plugins.configureSecurity
import me.wlko.algorithmd.plugins.configureSerialization
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

    private fun ApplicationEnvironment.defaultTestConfig() = config(
        mapOf(
            "jwt.audience" to "https://api.algorithmd.wlko.me",
            "jwt.domain" to "https://algorithmd.eu.auth0.com/",
            "jwt.realm" to "Test server"
        )
    )

    private fun Application.defaultModuleFunction() {
        environment.defaultTestConfig()
        configureSerialization()
        authentication { basic { skipWhen { true } } }
        configureRouting()
    }

    private fun <R> withDefaultTestApplication(test: TestApplicationEngine.() -> R) =
        withTestApplication({ defaultModuleFunction() }, test)

    @Test
    fun testHealth() {
        withDefaultTestApplication {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }

    @Test
    fun testWorkingAuthentication() {
        withTestApplication({
            environment.defaultTestConfig()
            configureSerialization()
            configureSecurity()
            configureRouting()
        }) {
            handleRequest(HttpMethod.Post, "/v1/convertToken") {
                addHeader(
                    "Authorization",
                    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IkdrQnhpTnMxdVR0YzY1dXZBTkR3dyJ9.eyJpc3MiOiJodHRwczovL2FsZ29yaXRobWQuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTA2MDMzMTM4OTkyOTI2MTkxODc1IiwiYXVkIjpbImh0dHBzOi8vYXBpLmFsZ29yaXRobWQud2xrby5tZSIsImh0dHBzOi8vYWxnb3JpdGhtZC5ldS5hdXRoMC5jb20vdXNlcmluZm8iXSwiaWF0IjoxNjI2MzYzOTM4LCJleHAiOjE2MjY0NTAzMzgsImF6cCI6IlgzcmlIbFJDbWJaVTFIVjJzcGxoQnN3Q1p2STZwMm9OIiwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCBvZmZsaW5lX2FjY2VzcyJ9.HeMDbeJYvUcWfHSt06OhY1svQqP9XC3RJ9oPQp_9K5kcozmCarM_Cx5jn3iCfXCzv0JbOCri7ARFjIjAiVWO-zXoAr7V6CUUdQrRWLITkT-6MJaQqCrVQm-2mwknvRj1dGV0kohi4CgZ-UctyhClplX1T2OQ28MlD2_0l3DCwGB_vJqCwqzkB9gEXO-Kmo981RjL-mGSIsX9O61xc30uVjlmclLkOUG_foEtIVaKrMw0ChoAiOAvsSUy3AcO-JPXTtHWY6H5ZCj89fhQGJ2lpzGHfxsJELTJUrwUbecqGO1ujvOlIqZ_ZZ8TKvktjiHw3eXs_WBXe5bk1WA5LV8Bzw"
                )
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }
}