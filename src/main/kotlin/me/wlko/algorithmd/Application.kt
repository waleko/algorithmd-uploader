package me.wlko.algorithmd

import io.ktor.application.*
import io.ktor.server.jetty.*
import me.wlko.algorithmd.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureFirebase()
    configureSecurity()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}