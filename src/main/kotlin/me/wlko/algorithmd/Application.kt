package me.wlko.algorithmd

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import me.wlko.algorithmd.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val databaseURL = environment.config.property("firebase.database_url").getString()
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .setDatabaseUrl(databaseURL)
        .build()
    FirebaseApp.initializeApp(options)

    configureSecurity()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}