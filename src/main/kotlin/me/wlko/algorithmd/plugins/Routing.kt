package me.wlko.algorithmd.plugins

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.request.*
import io.ktor.response.*
import java.util.*

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
                    val subject = call.jwtSubject()
                    val firebaseToken = FirebaseAuth.getInstance().createCustomToken(subject)
                    call.respond(mapOf("token" to firebaseToken))
                }
                post("upload") {
                    val newCodeRecord = call.receive<NewCodeRecord>()
                    newCodeRecord.run {
                        if (title.isEmpty() || title.length > 100)
                            throw BadRequestException("Title invalid")
                        if (language.isEmpty())
                            throw BadRequestException("Language invalid")
                        if (filename.isEmpty() || filename.length > 100)
                            throw BadRequestException("Filename invalid")
                        if (tagItems.size > 100)
                            throw BadRequestException("Tag items invalid")
                        if (full_content.isEmpty())
                            throw BadRequestException("No content")
                    }
                    val uuid = UUID.randomUUID().toString()
                    val codeRecord = CodeRecord(newCodeRecord, uuid)
                    val fullCodeRecord = FullCodeRecord(newCodeRecord.full_content, codeRecord)
                    call.respond(mapOf("uid" to uuid))

                    val db = FirebaseDatabase.getInstance()
                    db.getReference("/records/${uuid}").setValueAsync(fullCodeRecord)
                    db.getReference("/users/${subject}/records").push().setValueAsync(codeRecord)
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

data class NewCodeRecord(
    val title: String,
    val language: String,
    val tagItems: List<String>,
    val filename: String,
    val full_content: String
)

data class CodeRecord(
    val uid: String,
    val title: String,
    val language: String,
    val preview_content: String,
    val tagItems: List<String>,
    val filename: String
) {
    constructor(newCodeRecord: NewCodeRecord, uid: String, preview_lines: Int = 10) : this(
        uid,
        newCodeRecord.title,
        newCodeRecord.language,
        newCodeRecord.full_content
            .split('\n')
            .take(10)
            .joinToString("\n"),
        newCodeRecord.tagItems,
        newCodeRecord.filename
    )
}

data class FullCodeRecord(
    val full_content: String,
    val info: CodeRecord
)