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
import kotlinx.serialization.SerializationException
import me.wlko.algorithmd.CodeRecord
import me.wlko.algorithmd.FullCodeRecord
import me.wlko.algorithmd.NewCodeRecord
import me.wlko.algorithmd.utils.decrementQuota
import me.wlko.algorithmd.utils.incrementAndGetUploadQuota
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
                 */
                post("upload") {
                    val subject = call.jwtSubject()

                    // Receive body & validation

                    val newCodeRecord = call.receive<NewCodeRecord>()
                    // simple body validation
                    newCodeRecord.runCatching { simpleValidate() }.onFailure {
                        throw BadRequestException(it.message.orEmpty())
                    }

                    // get quota
                    val db = FirebaseDatabase.getInstance()
                    // increment current_amount for quota (thereby reserving space it)
                    // get quota for further validation
                    val quota = incrementAndGetUploadQuota(db, subject)
                    // validate against quota
                    newCodeRecord.runCatching { quotaValidate(quota) }.onFailure {
                        decrementQuota(db, subject)
                        throw BadRequestException(it.message.orEmpty())

                    }

                    // Saving code record

                    // generate an uuid for code
                    val uuid = UUID.randomUUID().toString()
                    // generate code record (with preview content only)
                    val codeRecord = CodeRecord(newCodeRecord, uuid)
                    // generate full code record (containing full content)
                    val fullCodeRecord = FullCodeRecord(newCodeRecord.full_content, codeRecord)

                    // save to realtime database
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
