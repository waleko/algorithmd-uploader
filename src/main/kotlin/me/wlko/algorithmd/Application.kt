package me.wlko.algorithmd

import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import me.wlko.algorithmd.plugins.*

fun Application.module() {
    configureRouting()
    configureSecurity()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}

fun main() {
    embeddedServer(Jetty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}
