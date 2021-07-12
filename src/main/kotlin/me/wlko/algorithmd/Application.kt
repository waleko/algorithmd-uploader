package me.wlko.algorithmd

import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import me.wlko.algorithmd.plugins.*

fun main() {
    embeddedServer(Jetty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureSecurity()
        configureHTTP()
        configureMonitoring()
        configureSerialization()
    }.start(wait = true)
}
