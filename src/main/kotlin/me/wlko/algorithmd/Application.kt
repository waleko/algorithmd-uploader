package me.wlko.algorithmd

import com.google.firebase.FirebaseApp
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import me.wlko.algorithmd.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    FirebaseApp.initializeApp();
    configureSecurity()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}