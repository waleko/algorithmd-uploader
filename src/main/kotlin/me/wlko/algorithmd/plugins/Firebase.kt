package me.wlko.algorithmd.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.application.*

/**
 * Initializes firebase with default credentials and custom database url
 */
fun Application.configureFirebase() {
    val databaseURL = environment.config.property("firebase.database_url").getString()
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .setDatabaseUrl(databaseURL)
        .build()
    FirebaseApp.initializeApp(options)
}