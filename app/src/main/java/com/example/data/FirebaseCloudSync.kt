package com.example.data

import com.example.model.FirebaseConfigConstants
import com.example.util.InputSanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Multi-Tenant Firebase Cloud Sync Manager (Firestore + Realtime Database REST API).
 * Strictly isolates every school's data under:
 *   schools/{schoolId}/settings
 *   schools/{schoolId}/users
 *   schools/{schoolId}/subjects
 *   schools/{schoolId}/attendance
 *   schools/{schoolId}/schedules_homework
 *   schools/{schoolId}/grades
 *   schools/{schoolId}/honor_roll
 *   schools/{schoolId}/tuition_fees
 *   schools/{schoolId}/reports
 *   schools/{schoolId}/messages
 *   schools/{schoolId}/events_gallery
 *   schools/{schoolId}/notifications
 */
object FirebaseCloudSync {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val localWriteTimestamps = ConcurrentHashMap<String, Long>()

    fun wasRecentlyWrittenLocally(
        schoolId: String,
        subCollection: String,
        windowMs: Long = 4500L
    ): Boolean {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSchoolId.isBlank()) return false
        val lastWrite = localWriteTimestamps["$cleanSchoolId/$subCollection"] ?: return false
        return (System.currentTimeMillis() - lastWrite) in 0..windowMs
    }

    fun pushSubCollectionAsync(
        schoolId: String,
        subCollection: String,
        jsonPayload: String
    ) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSchoolId.isBlank()) return

        localWriteTimestamps["$cleanSchoolId/$subCollection"] = System.currentTimeMillis()

        syncScope.launch {
            try {
                // 1. Sync to Firestore under schools/{schoolId}/{subCollection}/state
                val firestoreUrl =
                    "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/schools/$cleanSchoolId/$subCollection/state?key=${FirebaseConfigConstants.API_KEY}"

                val firestoreBody = JSONObject().apply {
                    put("fields", JSONObject().apply {
                        put("schoolId", JSONObject().put("stringValue", cleanSchoolId))
                        put("collection", JSONObject().put("stringValue", subCollection))
                        put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                        put("payloadJson", JSONObject().put("stringValue", jsonPayload))
                    })
                }.toString()

                executeHttpWrite(firestoreUrl, "PATCH", firestoreBody)

                // 2. Mirror to Firebase Realtime Database under schools/{schoolId}/{subCollection}.json
                val rtdbUrl =
                    "${FirebaseConfigConstants.DATABASE_URL}/schools/$cleanSchoolId/$subCollection.json"
                val rtdbBody = JSONObject().apply {
                    put("schoolId", cleanSchoolId)
                    put("updatedAt", System.currentTimeMillis())
                    put("payloadJson", jsonPayload)
                }.toString()

                executeHttpWrite(rtdbUrl, "PUT", rtdbBody)
            } catch (_: Exception) {
                // Offline-tolerant: local school-scoped persistence remains intact
            }
        }
    }

    suspend fun fetchSubCollectionPayload(
        schoolId: String,
        subCollection: String
    ): String? = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSchoolId.isBlank()) return@withContext null

        // Try Firestore first
        try {
            val firestoreUrl =
                "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/schools/$cleanSchoolId/$subCollection/state?key=${FirebaseConfigConstants.API_KEY}"
            val response = executeHttpGet(firestoreUrl)
            if (!response.isNullOrBlank()) {
                val root = JSONObject(response)
                val fields = root.optJSONObject("fields")
                val payloadObj = fields?.optJSONObject("payloadJson")
                val payload = payloadObj?.optString("stringValue", "") ?: ""
                if (payload.isNotBlank()) return@withContext payload
            }
        } catch (_: Exception) {
        }

        // Fallback to Realtime Database
        try {
            val rtdbUrl =
                "${FirebaseConfigConstants.DATABASE_URL}/schools/$cleanSchoolId/$subCollection.json"
            val response = executeHttpGet(rtdbUrl)
            if (!response.isNullOrBlank() && response != "null") {
                val root = JSONObject(response)
                val payload = root.optString("payloadJson", "")
                if (payload.isNotBlank()) return@withContext payload
            }
        } catch (_: Exception) {
        }

        null
    }

    /**
     * Pushes a root-level collection payload (e.g. school_licenses or schools registry)
     * to Firestore and Realtime Database.
     */
    fun pushGlobalCollectionAsync(
        collectionName: String,
        docName: String = "registry",
        jsonPayload: String
    ) {
        val cleanCollection = InputSanitizer.sanitizeSchoolId(collectionName)
        val cleanDoc = InputSanitizer.sanitizeSchoolId(docName)
        if (cleanCollection.isBlank() || cleanDoc.isBlank()) return

        syncScope.launch {
            try {
                val firestoreUrl =
                    "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/$cleanCollection/$cleanDoc?key=${FirebaseConfigConstants.API_KEY}"

                val firestoreBody = JSONObject().apply {
                    put("fields", JSONObject().apply {
                        put("collection", JSONObject().put("stringValue", cleanCollection))
                        put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                        put("payloadJson", JSONObject().put("stringValue", jsonPayload))
                    })
                }.toString()

                executeHttpWrite(firestoreUrl, "PATCH", firestoreBody)

                val rtdbUrl =
                    "${FirebaseConfigConstants.DATABASE_URL}/${cleanCollection}_$cleanDoc.json"
                val rtdbBody = JSONObject().apply {
                    put("collection", cleanCollection)
                    put("updatedAt", System.currentTimeMillis())
                    put("payloadJson", jsonPayload)
                }.toString()

                executeHttpWrite(rtdbUrl, "PUT", rtdbBody)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Also writes each individual license document under school_licenses/{code} in Firestore & RTDB
     * with explicit fields (code, used, createdAt, usedBySchoolId, usedBySchoolName).
     */
    fun pushSingleLicenseDocumentAsync(
        code: String,
        used: Boolean,
        createdAt: String,
        usedBySchoolId: String = "",
        usedBySchoolName: String = "",
        usedAt: String = ""
    ) {
        val cleanCode = InputSanitizer.sanitizeSchoolId(code)
        if (cleanCode.isBlank()) return

        syncScope.launch {
            try {
                val firestoreUrl =
                    "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/${FirebaseConfigConstants.COL_SCHOOL_LICENSES}/$cleanCode?key=${FirebaseConfigConstants.API_KEY}"

                val firestoreBody = JSONObject().apply {
                    put("fields", JSONObject().apply {
                        put("code", JSONObject().put("stringValue", cleanCode))
                        put("used", JSONObject().put("booleanValue", used))
                        put("createdAt", JSONObject().put("stringValue", createdAt))
                        put("usedBySchoolId", JSONObject().put("stringValue", usedBySchoolId))
                        put("usedBySchoolName", JSONObject().put("stringValue", usedBySchoolName))
                        put("usedAt", JSONObject().put("stringValue", usedAt))
                    })
                }.toString()

                executeHttpWrite(firestoreUrl, "PATCH", firestoreBody)

                val rtdbUrl =
                    "${FirebaseConfigConstants.DATABASE_URL}/${FirebaseConfigConstants.COL_SCHOOL_LICENSES}/$cleanCode.json"
                val rtdbBody = JSONObject().apply {
                    put("code", cleanCode)
                    put("used", used)
                    put("createdAt", createdAt)
                    put("usedBySchoolId", usedBySchoolId)
                    put("usedBySchoolName", usedBySchoolName)
                    put("usedAt", usedAt)
                }.toString()

                executeHttpWrite(rtdbUrl, "PUT", rtdbBody)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun fetchGlobalCollectionPayload(
        collectionName: String,
        docName: String = "registry"
    ): String? = withContext(Dispatchers.IO) {
        val cleanCollection = InputSanitizer.sanitizeSchoolId(collectionName)
        val cleanDoc = InputSanitizer.sanitizeSchoolId(docName)
        if (cleanCollection.isBlank() || cleanDoc.isBlank()) return@withContext null

        try {
            val firestoreUrl =
                "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/$cleanCollection/$cleanDoc?key=${FirebaseConfigConstants.API_KEY}"
            val response = executeHttpGet(firestoreUrl)
            if (!response.isNullOrBlank()) {
                val root = JSONObject(response)
                val fields = root.optJSONObject("fields")
                val payload = fields?.optJSONObject("payloadJson")?.optString("stringValue", "") ?: ""
                if (payload.isNotBlank()) return@withContext payload
            }
        } catch (_: Exception) {
        }

        try {
            val rtdbUrl =
                "${FirebaseConfigConstants.DATABASE_URL}/${cleanCollection}_$cleanDoc.json"
            val response = executeHttpGet(rtdbUrl)
            if (!response.isNullOrBlank() && response != "null") {
                val root = JSONObject(response)
                val payload = root.optString("payloadJson", "")
                if (payload.isNotBlank()) return@withContext payload
            }
        } catch (_: Exception) {
        }

        null
    }

    private fun executeHttpWrite(urlStr: String, method: String, jsonBody: String) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                if (method == "PATCH") {
                    requestMethod = "POST"
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                } else {
                    requestMethod = method
                }
                connectTimeout = 7000
                readTimeout = 7000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }
            conn.responseCode
        } catch (_: Exception) {
        } finally {
            conn?.disconnect()
        }
    }

    private fun executeHttpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
