package me.wlko.algorithmd

import com.auth0.jwt.JWT
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import me.wlko.algorithmd.plugins.configureRouting
import me.wlko.algorithmd.plugins.configureSecurity
import me.wlko.algorithmd.plugins.configureSerialization
import me.wlko.algorithmd.utils.*
import org.junit.Assert.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.time.Duration


const val MAX_AMOUNT_QUOTA = 2
const val MAX_UPLOAD_SIZE_KB = 3

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {

    private fun ApplicationEnvironment.defaultTestConfig() = config(
        mapOf(
            "jwt.audience" to "https://api.algorithmd.wlko.me",
            "jwt.domain" to "https://algorithmd.eu.auth0.com/",
            "jwt.realm" to "Test server"
        )
    )

    private val defaultQuota = UploadQuota(0, MAX_AMOUNT_QUOTA, MAX_UPLOAD_SIZE_KB)

    lateinit var db: FirebaseDatabase
    lateinit var auth: FirebaseAuth

    @BeforeAll
    fun configureFirebase() {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setDatabaseUrl("http://localhost:9000")
            .build()
        /**
         * Be sure to have specified following environment variables:
         *
         * * GOOGLE_APPLICATION_CREDENTIALS (firebase still needs some credentials)
         * * GCLOUD_PROJECT
         * * FIREBASE_DATABASE_EMULATOR_HOST
         * * FIREBASE_AUTH_EMULATOR_HOST
         */
        FirebaseApp.initializeApp(options)
        db = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        // check connection to db
        assertTimeoutPreemptively(
            Duration.ofSeconds(60),
            "Could not connect to db. Check firebase emulator connection"
        ) {
            runBlocking {
                db.getReference("/limits/defaultLimit").setValueSuspend(defaultQuota)
            }
        }
    }

    /**
     * Default initialization steps for application testing
     */
    private fun Application.defaultModuleFunction() {
        configureSerialization()
        configureTestAuthentication()
        configureRouting()
    }

    private fun <R> withDefaultTestApplication(test: TestApplicationEngine.() -> R) =
        withTestApplication({ defaultModuleFunction() }, test)

    /**
     * Test homepage response
     */
    @Test
    fun testHomepage() {
        withDefaultTestApplication {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }

    /**
     * Test health page
     */
    @Test
    fun testHealth() {
        withDefaultTestApplication {
            handleRequest(HttpMethod.Get, "/v1/health").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("OK", response.content)
            }
        }
    }

    /**
     * Performs convert token request for [subject]
     */
    private fun TestApplicationEngine.convertToken(subject: String) =
        handleRequest(HttpMethod.Post, "/v1/convertToken") {
            val token = JWTUtils.getTokenForSubject(subject)
            addHeader(HttpHeaders.Authorization, "Bearer $token")
        }

    /**
     * Test convert token
     */
    @Nested
    inner class ConvertTokenTests {
        /**
         * Tests simple token conversion (auth0 -> firebase)
         */
        @Test
        fun testConvertToken() {
            withDefaultTestApplication {
                // get random subject
                val subject = newUUID()
                convertToken(subject).apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    assertTrue(response.contentType().match(ContentType.Application.Json))
                    // get returned firebase token
                    val map = Gson().fromJson(response.content, Map::class.java)
                    val jwt = map["token"].toString()
                    // decode token
                    val jwtDecoded = JWT.decode(jwt)
                    // check properties
                    assertEquals("firebase-auth-emulator@example.com", jwtDecoded.issuer)
                    assertEquals(subject, jwtDecoded.getClaim("uid").asString())
                }
            }
        }

        /**
         * Test real expired auth0 token
         */
        @Test
        fun testExpiredConvertToken() {
            withTestApplication({
                environment.defaultTestConfig()
                configureSerialization()
                configureSecurity()
                configureRouting()
            }) {
                val expiredToken =
                    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IkdrQnhpTnMxdVR0YzY1dXZBTkR3dyJ9.eyJpc3MiOiJodHRwczovL2FsZ29yaXRobWQuZXUuYXV0aDAuY29tLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTA2MDMzMTM4OTkyOTI2MTkxODc1IiwiYXVkIjpbImh0dHBzOi8vYXBpLmFsZ29yaXRobWQud2xrby5tZSIsImh0dHBzOi8vYWxnb3JpdGhtZC5ldS5hdXRoMC5jb20vdXNlcmluZm8iXSwiaWF0IjoxNjI2MzYzOTM4LCJleHAiOjE2MjY0NTAzMzgsImF6cCI6IlgzcmlIbFJDbWJaVTFIVjJzcGxoQnN3Q1p2STZwMm9OIiwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCBvZmZsaW5lX2FjY2VzcyJ9.HeMDbeJYvUcWfHSt06OhY1svQqP9XC3RJ9oPQp_9K5kcozmCarM_Cx5jn3iCfXCzv0JbOCri7ARFjIjAiVWO-zXoAr7V6CUUdQrRWLITkT-6MJaQqCrVQm-2mwknvRj1dGV0kohi4CgZ-UctyhClplX1T2OQ28MlD2_0l3DCwGB_vJqCwqzkB9gEXO-Kmo981RjL-mGSIsX9O61xc30uVjlmclLkOUG_foEtIVaKrMw0ChoAiOAvsSUy3AcO-JPXTtHWY6H5ZCj89fhQGJ2lpzGHfxsJELTJUrwUbecqGO1ujvOlIqZ_ZZ8TKvktjiHw3eXs_WBXe5bk1WA5LV8Bzw"
                handleRequest(HttpMethod.Post, "/v1/convertToken") {
                    addHeader(HttpHeaders.Authorization, "Bearer $expiredToken")
                }.apply {
                    assertEquals(HttpStatusCode.Unauthorized, response.status())
                }
            }
        }
    }

    /**
     * Perform upload request
     */
    private fun TestApplicationEngine.requestUpload(subject: String, newCodeRecord: NewCodeRecord) =
        handleRequest(HttpMethod.Post, "/v1/upload") {
            // get authorized token
            val token = JWTUtils.getTokenForSubject(subject)
            // set headers
            addHeader(HttpHeaders.Authorization, "Bearer $token")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            // set body
            setBody(Gson().toJson(newCodeRecord))
        }

    /**
     * Upload tests
     */
    @Nested
    inner class UploadTests {
        /**
         * Test uploading of various [NewCodeRecord] instances. They all should be uploaded successfully.
         */
        @TestFactory
        fun upload(): Collection<DynamicTest> {
            return mapOf(
                "Simple" to NewCodeRecord(
                    newUUID(),
                    "python",
                    listOf(newUUID(), newUUID(), newUUID()),
                    newUUID(),
                    generateLongText(5, 53)
                ),
                "Simple no tags" to NewCodeRecord(
                    newUUID(),
                    newUUID(),
                    emptyList(),
                    newUUID(),
                    generateLongText(3, 12)
                ),
                "Content longer than preview" to NewCodeRecord(
                    newUUID(),
                    newUUID(),
                    randomTags(10),
                    newUUID(),
                    generateLongText(12, 103)
                )
            ).map { (name: String, newCodeRecord: NewCodeRecord) ->
                dynamicTest("Bad Request test ($name)") {
                    withDefaultTestApplication {
                        // get subject
                        val subject = newUUID()
                        // upload
                        val uid = requestUpload(subject, newCodeRecord).run {
                            assertEquals(HttpStatusCode.OK, response.status())
                            assertTrue(response.contentType().match(ContentType.Application.Json))
                            // get returned record uid
                            val map = Gson().fromJson(response.content, Map::class.java)
                            return@run map["uid"].toString()
                        }
                        // verify fullCodeRecord
                        runBlocking {
                            // get full record by uid
                            val fullCodeRecord = db.getReference("/records/${uid}")
                                .readSingle<FullCodeRecord>()
                                .also { assertNotNull(it) }
                                ?: error("")
                            // check integrity
                            assertTrue(fullCodeRecord.matches(newCodeRecord))
                        }
                        // verify codeRecord
                        runBlocking {
                            // get all preview records for subject
                            val codeRecords = db.getReference("/users/${subject}/records").readList<CodeRecord>()
                            // find only with correct uid (should be just 1)
                            val matching = codeRecords.filter { it.uid == uid }
                            assertEquals(1, matching.size)
                            val codeRecord = matching[0]
                            // check integrity
                            assertTrue(codeRecord.matches(newCodeRecord))
                        }
                    }
                }
            }
        }

        /**
         * Test uploading invalid data
         */
        @TestFactory
        fun testBadRequestUpload(): Collection<DynamicTest> {
            return mapOf(
                "Long title" to NewCodeRecord(
                    generateLine(1000),
                    newUUID(),
                    emptyList(),
                    newUUID(),
                    generateLongText(10, 20)
                ),
                "No title" to NewCodeRecord("", newUUID(), emptyList(), newUUID(), generateLongText(10, 20)),
                "Long language" to NewCodeRecord(
                    newUUID(),
                    generateLine(1000),
                    listOf(newUUID()),
                    newUUID(),
                    generateLongText(10, 20)
                ),
                "No language" to NewCodeRecord(newUUID(), "", emptyList(), newUUID(), generateLongText(10, 20)),
                "Long filename" to NewCodeRecord(
                    newUUID(),
                    newUUID(),
                    emptyList(),
                    generateLine(1000),
                    generateLongText(10, 20)
                ),
                "No filename" to NewCodeRecord(newUUID(), newUUID(), emptyList(), "", generateLongText(10, 20)),
                "Long content [quota exceeded]" to NewCodeRecord(
                    newUUID(),
                    newUUID(),
                    emptyList(),
                    newUUID(),
                    generateLongText(1000, 2000)
                ),
                "Too many tags" to NewCodeRecord(
                    newUUID(),
                    newUUID(),
                    exactTags(1000),
                    newUUID(),
                    generateLongText(1, 4)
                ),
            ).map { (name: String, newCodeRecord: NewCodeRecord) ->
                dynamicTest("Bad Request test ($name)") {
                    withDefaultTestApplication {
                        // get subject
                        val subject = newUUID()
                        // upload
                        requestUpload(subject, newCodeRecord).apply {
                            assertEquals(HttpStatusCode.BadRequest, response.status())
                            // verify no code records
                            runBlocking {
                                val codeRecords = db.getReference("/users/${subject}/records").readList<CodeRecord>()
                                assertTrue(codeRecords.isEmpty())
                            }
                        }
                    }
                }
            }
        }

        /**
         * Try uploading [NewCodeRecord] with some fields missing
         */
        @Test
        fun missingFieldTest() {
            withDefaultTestApplication {
                // get subject
                val subject = newUUID()
                // make a new record
                val newCodeRecord = NewCodeRecord(
                    newUUID(),
                    "python",
                    listOf(newUUID(), newUUID(), newUUID()),
                    newUUID(),
                    generateLongText(12, 103)
                )
                // custom upload
                handleRequest(HttpMethod.Post, "/v1/upload") {
                    // get authorized token
                    val token = JWTUtils.getTokenForSubject(subject)
                    // add valid headers
                    addHeader(HttpHeaders.Authorization, "Bearer $token")
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    // drop `language` field
                    val map = parseObjectNotNull(newCodeRecord, Map::class)
                    val mapWithRemoved = map.filterKeys { it != "language" }
                    // set body with missing field
                    setBody(Gson().toJson(mapWithRemoved))
                }.apply {
                    assertEquals(HttpStatusCode.BadRequest, response.status())
                    // verify no code records
                    runBlocking {
                        val codeRecords = db.getReference("/users/${subject}/records").readList<CodeRecord>()
                        assertTrue(codeRecords.isEmpty())
                    }
                }
            }
        }

        /**
         * Try uploading more records than allowed
         */
        @Test
        fun uploadMoreThanQuota() {
            // Function to generate similar code records
            fun generateNewCodeRecord() = NewCodeRecord(
                newUUID(),
                newUUID(),
                randomTags(10),
                newUUID(),
                generateLongText(12, 103)
            )

            withDefaultTestApplication {
                // get subject
                val subject = newUUID()
                // check that user quota is correctly set
                assertTrue(MAX_AMOUNT_QUOTA > 0)
                // upload maximum allowed times
                repeat(MAX_AMOUNT_QUOTA) {
                    requestUpload(subject, generateNewCodeRecord()).apply {
                        // check success
                        assertEquals(HttpStatusCode.OK, response.status())
                    }
                }
                // try uploading over the quota
                requestUpload(subject, generateNewCodeRecord()).apply {
                    // should fail
                    assertEquals(HttpStatusCode.BadRequest, response.status())
                }
                // check number of records
                runBlocking {
                    val records = db.getReference("/users/$subject/records").readList<CodeRecord>()
                    assertEquals(MAX_AMOUNT_QUOTA, records.size)
                }
            }
        }
    }

    private fun TestApplicationEngine.requestDownload(codeRecordUID: String) =
        handleRequest(HttpMethod.Get, "/v1/download/$codeRecordUID") {}

    /**
     * Test downloading record as a file
     */
    @Nested
    inner class DownloadTests {
        /**
         * Upload record and download it
         */
        @Test
        fun uploadAndDownload() {
            withDefaultTestApplication {
                // get subject
                val subject = newUUID()
                // new record
                val newCodeRecord = NewCodeRecord(
                    newUUID(),
                    "python",
                    listOf(newUUID(), newUUID(), newUUID()),
                    newUUID(),
                    generateLongText(12, 103)
                )
                // upload
                val uid = requestUpload(subject, newCodeRecord).run {
                    assertEquals(HttpStatusCode.OK, response.status())
                    val map = Gson().fromJson(response.content, Map::class.java)
                    return@run map["uid"].toString()
                }
                // download
                requestDownload(uid).apply {
                    // check status
                    assertEquals(HttpStatusCode.OK, response.status())
                    // check required headers for downloading
                    val expectedHeader = ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        newCodeRecord.filename
                    ).toString()
                    assertEquals(expectedHeader, response.headers[HttpHeaders.ContentDisposition])
                    // check file content
                    assertEquals(newCodeRecord.full_content, response.content)
                }
            }
        }

        /**
         * Try to download non-existing file. Should return 404.
         */
        @Test
        fun downloadNonExisting() {
            withDefaultTestApplication {
                val uid = newUUID()
                requestDownload(uid).apply {
                    assertEquals(HttpStatusCode.NotFound, response.status())
                }
            }
        }
    }
}