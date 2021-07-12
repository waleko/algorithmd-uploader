val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val appengine_version: String by project
val appengine_plugin_version: String by project
val gcp_project_id: String by project
val gce_logback_version: String by project


plugins {
    application
    kotlin("jvm") version "1.5.20"
    war
    id("com.google.cloud.tools.appengine") version "2.2.0"
}

appengine {
    deploy {
        projectId = gcp_project_id
        version = "1"
        stopPreviousVersion = true
        promote = true
    }
}

group = "me.wlko.algorithmd"
version = "0.0.1"
application {
    mainClass.set("me.wlko.algorithmd.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("io.ktor:ktor-server-jetty:$ktor_version")
    implementation("com.google.cloud:google-cloud-logging-logback:$gce_logback_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}