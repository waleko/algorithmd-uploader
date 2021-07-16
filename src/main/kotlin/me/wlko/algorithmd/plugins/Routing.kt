package me.wlko.algorithmd.plugins

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import me.wlko.algorithmd.utils.readSingle
import me.wlko.algorithmd.utils.setValueSuspend
import java.util.*

fun Application.configureRouting() {


    routing {
        get("/") {
            call.respondText("Welcome to algorithmd API endpoint")
        }
        route("v1") {
            /**
             * Simple health check
             */
            get("health") {
                call.respondText { "OK" }
            }

            get("download/{uid}") {
                // get code uid
                val uid: String = call.parameters["uid"] ?: throw BadRequestException("No file uid supplied")

                // get full code record
                val db = FirebaseDatabase.getInstance()
                val fullCodeRecord: FullCodeRecord? = db.getReference("/records/${uid}").readSingle()

                // if no reference is present, return null
                if (fullCodeRecord == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                // send code record as file
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        fullCodeRecord.info.filename
                    )
                        .toString()
                )
                call.respondText { fullCodeRecord.full_content }
            }

            /**
             * Auth0 JWT Authentication
             */
            authenticate {
                /**
                 * Generates a custom firebase token for an authorized auth0 client
                 */
                post("convertToken") {
                    val subject = call.jwtSubject()
                    val firebaseToken = FirebaseAuth.getInstance().createCustomToken(subject)
                    call.respond(mapOf("token" to firebaseToken))
                }

                /**
                 * Uploads code to realtime database for an authorized auth0 client
                 *
                 * TODO: implement per-user quota check
                 */
                post("upload") {
                    val subject = call.jwtSubject()

                    // receive body
                    val newCodeRecord = call.receive<NewCodeRecord>()
                    // validate received data
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
                    // generate an uuid for code
                    val uuid = UUID.randomUUID().toString()
                    // generate code record (with preview content only)
                    val codeRecord = CodeRecord(newCodeRecord, uuid)
                    // generate full code record (containing full content)
                    val fullCodeRecord = FullCodeRecord(newCodeRecord.full_content, codeRecord)

                    // save to realtime database
                    val db = FirebaseDatabase.getInstance()
                    db.getReference("/records/${uuid}").setValueSuspend(fullCodeRecord)
                    db.getReference("/users/${subject}/records").push().setValueSuspend(codeRecord)

                    // return uuid of code
                    call.respond(mapOf("uid" to uuid))
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
            exception<SerializationException> { cause ->
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

/**
 * Data model representing client's request to save a code fragment
 */
@Serializable
data class NewCodeRecord(
    val title: String,
    val language: String,
    val tagItems: List<String>,
    val filename: String,
    val full_content: String
)

/**
 * Data model representing code record with preview content
 * limited to 10 lines and 100 columns (for faster list load times)
 */
@Serializable
data class CodeRecord(
    val uid: String,
    val title: String,
    val language: String,
    val preview_content: String,
    val tagItems: List<String>,
    val filename: String
) {
    constructor(newCodeRecord: NewCodeRecord, uid: String, previewLines: Int = 10, previewColumns: Int = 100) : this(
        uid,
        newCodeRecord.title,
        newCodeRecord.language,
        newCodeRecord.full_content
            .split('\n')
            .take(previewLines) // limit lines
            .joinToString("\n") {
                // limit columns
                if (it.length < previewColumns)
                    it
                else
                    "${it.take(previewColumns)}..."
            },
        newCodeRecord.tagItems,
        newCodeRecord.filename
    )
}

/**
 * Data model representing code record with full content
 */
@Serializable
data class FullCodeRecord(
    val full_content: String,
    val info: CodeRecord
)