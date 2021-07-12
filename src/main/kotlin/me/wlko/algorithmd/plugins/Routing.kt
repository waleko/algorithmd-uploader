package me.wlko.algorithmd.plugins

import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.request.*

fun Application.configureRouting() {


    routing {
        get("/") {
            call.respondText("Welcome to algorithmd API endpoint")
        }
        route("v1") {
            get("health") {
                call.respondText {"OK"}
            }
            authenticate {
                post("convertToken") {
                    call.respondText { "Hi ${call.subject()}" }
                }
            }
        }
        install(StatusPages) {
            exception<AuthenticationException> { cause ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> { cause ->
                call.respond(HttpStatusCode.Forbidden)
            }

        }
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
