package com.example.data

import android.content.Context
import com.example.model.*
import com.example.util.InputSanitizer
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SchoolRepository(context: Context) {

    // Using v2 clean multi-tenant storage so pre-existing demo records do not pollute newly founded schools
    private val prefs = context.getSharedPreferences("madrasati_multitenant_v2", Context.MODE_PRIVATE)

    fun isOnboardingSeen(): Boolean = prefs.getBoolean("onboarding_seen", false)

    fun setOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean("onboarding_seen", seen).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
    }

    fun getActiveSchoolId(): String {
        return prefs.getString("active_school_id", "")?.trim().orEmpty()
    }

    fun setActiveSchoolId(schoolId: String) {
        val clean = InputSanitizer.sanitizeSchoolId(schoolId)
        prefs.edit().putString("active_school_id", clean).apply()
    }

    private fun scopedKey(schoolId: String, subCollection: String): String {
        val sid = schoolId.ifBlank { getActiveSchoolId() }.ifBlank { "default_school" }
        return FirebaseConfigConstants.schoolCollectionPath(sid, subCollection)
    }

    fun getRawCachedSubCollection(schoolId: String, subCollection: String): String? {
        if (schoolId.isBlank()) return null
        return prefs.getString(scopedKey(schoolId, subCollection), null)
    }

    fun saveRawCachedSubCollection(schoolId: String, subCollection: String, jsonPayload: String) {
        if (schoolId.isBlank()) return
        val editor = prefs.edit().putString(scopedKey(schoolId, subCollection), jsonPayload)
        if (subCollection == FirebaseConfigConstants.COL_SCHEDULES_PERIODS) {
            editor.putString("${scopedKey(schoolId, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)}_periods", jsonPayload)
        } else if (subCollection == FirebaseConfigConstants.COL_EXAMS) {
            editor.putString("${scopedKey(schoolId, FirebaseConfigConstants.COL_GRADES)}_exams", jsonPayload)
        }
        editor.apply()
    }

    // =========================================================================
    // SESSION MANAGEMENT & VALIDATION
    // =========================================================================

    fun saveSession(session: UserSession) {
        val obj = JSONObject().apply {
            put("userId", session.userId)
            put("username", session.username)
            put("role", session.role.name)
            put("schoolId", session.schoolId)
            put("sessionToken", session.sessionToken)
            put("createdAt", session.createdAt)
        }
        prefs.edit().putString("active_user_session", obj.toString()).apply()
    }

    fun loadSession(): UserSession? {
        val raw = prefs.getString("active_user_session", null) ?: return null
        return try {
            val o = JSONObject(raw)
            val userId = o.optString("userId")
            val schoolId = o.optString("schoolId")
            if (userId.isBlank() || schoolId.isBlank()) return null
            UserSession(
                userId = userId,
                username = o.optString("username"),
                role = UserRole.valueOf(o.optString("role", UserRole.STUDENT.name)),
                schoolId = schoolId,
                sessionToken = o.optString("sessionToken"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession() {
        prefs.edit().remove("active_user_session").apply()
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i).trim()
            if (s.isNotEmpty()) list.add(s)
        }
        return list
    }

    // =========================================================================
    // 1. schools/{schoolId}/settings & schools/{schoolId}/subjects
    // =========================================================================

    fun loadSchoolConfig(schoolId: String = getActiveSchoolId()): SchoolConfig {
        if (schoolId.isBlank()) return SchoolConfig()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_SETTINGS), null)
            ?: return SchoolConfig()
        return parseSchoolConfigJson(raw, schoolId)
    }

    fun parseSchoolConfigJson(raw: String, fallbackSchoolId: String = ""): SchoolConfig {
        return try {
            val obj = JSONObject(raw)
            val stage = try {
                SchoolStage.valueOf(obj.optString("schoolStage", SchoolStage.SECONDARY.name))
            } catch (e: Exception) {
                SchoolStage.SECONDARY
            }
            val regClasses = jsonArrayToList(obj.optJSONArray("registeredClasses")).ifEmpty { stage.defaultClasses }
            val regSections = jsonArrayToList(obj.optJSONArray("registeredSections")).ifEmpty { listOf("أ", "ب", "ج") }
            val regSubjects = jsonArrayToList(obj.optJSONArray("registeredSubjects")).ifEmpty { stage.defaultSubjects }
            val resolvedSchoolId = obj.optString("schoolId", fallbackSchoolId).ifBlank { fallbackSchoolId }

            val subjectConfigs = loadSubjectConfigs(resolvedSchoolId).ifEmpty {
                regSubjects.mapIndexed { index, subName ->
                    SubjectConfigItem(
                        id = "sub_${index + 1}",
                        name = subName,
                        assignedClasses = regClasses,
                        defaultMaxScore = 100,
                        schoolId = resolvedSchoolId
                    )
                }
            }

            val featObj = obj.optJSONObject("featureSettings") ?: obj.optJSONObject("enabledFeatures")
            val parsedFeatures = if (featObj != null) {
                parseFeatureSettingsObject(featObj)
            } else {
                loadFeatureSettings(resolvedSchoolId)
            }

            val urgentAlertObj = obj.optJSONObject("urgentAlert")
            val parsedUrgentAlert = if (urgentAlertObj != null) {
                parseUrgentSchoolAlertObject(urgentAlertObj, resolvedSchoolId)
            } else {
                loadUrgentSchoolAlert(resolvedSchoolId)
            }

            SchoolConfig(
                schoolId = resolvedSchoolId,
                isSetupCompleted = obj.optBoolean("isSetupCompleted", false),
                isActive = obj.optBoolean("isActive", true),
                createdAt = obj.optString("createdAt", "2026-10-29"),
                schoolName = obj.optString("schoolName", "مدرستي النموذجية"),
                address = obj.optString("address", "الموصل - زمار"),
                schoolType = SchoolType.valueOf(obj.optString("schoolType", SchoolType.PRIVATE.name)),
                schoolStage = stage,
                registeredClasses = regClasses,
                registeredSections = regSections,
                registeredSubjects = regSubjects,
                subjectConfigs = subjectConfigs,
                logoBase64 = obj.optString("logoBase64", ""),
                academicYear = obj.optString("academicYear", "2026 - 2027"),
                phone = obj.optString("phone", "07512362567"),
                subscriptionExpiryDate = obj.optString("subscriptionExpiryDate", "2027-10-29").ifBlank { "2027-10-29" },
                featureSettings = parsedFeatures,
                urgentAlert = parsedUrgentAlert
            )
        } catch (e: Exception) {
            SchoolConfig()
        }
    }

    fun loadUrgentSchoolAlert(schoolId: String = getActiveSchoolId()): UrgentSchoolAlert {
        val sid = schoolId.ifBlank { getActiveSchoolId() }
        if (sid.isBlank()) return UrgentSchoolAlert()
        val raw = prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_URGENT_ALERT), null)
            ?: return UrgentSchoolAlert(schoolId = sid)
        return parseUrgentSchoolAlertJson(raw, sid)
    }

    fun parseUrgentSchoolAlertJson(raw: String, fallbackSchoolId: String = getActiveSchoolId()): UrgentSchoolAlert {
        return try {
            val obj = JSONObject(raw)
            parseUrgentSchoolAlertObject(obj, fallbackSchoolId)
        } catch (_: Exception) {
            UrgentSchoolAlert(schoolId = fallbackSchoolId)
        }
    }

    private fun parseUrgentSchoolAlertObject(obj: JSONObject, fallbackSchoolId: String): UrgentSchoolAlert {
        val msg = obj.optString("message", "")
        val active = obj.optBoolean("isActive", false) && msg.isNotBlank()
        return UrgentSchoolAlert(
            id = obj.optString("id", "urgent_alert"),
            schoolId = obj.optString("schoolId", fallbackSchoolId).ifBlank { fallbackSchoolId },
            message = msg,
            updatedAt = obj.optString("updatedAt", ""),
            authorName = obj.optString("authorName", "إدارة المدرسة"),
            isActive = active,
            signatureKey = obj.optString("signatureKey", "${obj.optString("updatedAt", "")}_${msg.hashCode()}")
        )
    }

    private fun urgentSchoolAlertToJsonObject(alert: UrgentSchoolAlert, schoolId: String): JSONObject {
        val sid = schoolId.ifBlank { alert.schoolId }
        val active = alert.isActive && alert.message.isNotBlank()
        val sig = alert.signatureKey.ifBlank { "${alert.updatedAt}_${alert.message.hashCode()}" }
        return JSONObject().apply {
            put("id", alert.id.ifBlank { "urgent_alert" })
            put("schoolId", sid)
            put("message", InputSanitizer.sanitize(alert.message))
            put("updatedAt", alert.updatedAt)
            put("authorName", InputSanitizer.sanitize(alert.authorName.ifBlank { "إدارة المدرسة" }))
            put("isActive", active)
            put("signatureKey", sig)
        }
    }

    fun saveUrgentSchoolAlert(alert: UrgentSchoolAlert, schoolId: String = getActiveSchoolId()) {
        val sid = schoolId.ifBlank { alert.schoolId }.ifBlank { getActiveSchoolId() }
        if (sid.isBlank()) return
        val alertObj = urgentSchoolAlertToJsonObject(alert.copy(schoolId = sid), sid)
        val alertStr = alertObj.toString()
        prefs.edit().putString(scopedKey(sid, FirebaseConfigConstants.COL_URGENT_ALERT), alertStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_URGENT_ALERT, alertStr)
        val currentCfg = loadSchoolConfig(sid)
        saveSchoolConfig(currentCfg.copy(schoolId = sid, urgentAlert = alert.copy(schoolId = sid)))
    }

    fun saveSchoolConfig(config: SchoolConfig) {
        val sid = config.schoolId.ifBlank { getActiveSchoolId() }.ifBlank {
            generateUniqueThreeDigitSchoolCode()
        }
        setActiveSchoolId(sid)

        val featJson = featureSettingsToJsonObject(config.featureSettings)
        val urgentAlertJson = urgentSchoolAlertToJsonObject(config.urgentAlert, sid)
        val obj = JSONObject().apply {
            put("schoolId", sid)
            put("isSetupCompleted", config.isSetupCompleted)
            put("isActive", config.isActive)
            put("createdAt", config.createdAt.ifBlank { "2026-10-29" })
            put("schoolName", InputSanitizer.sanitize(config.schoolName))
            put("address", InputSanitizer.sanitize(config.address.ifBlank { "الموصل - زمار" }))
            put("schoolType", config.schoolType.name)
            put("schoolStage", config.schoolStage.name)
            put("registeredClasses", JSONArray(config.registeredClasses))
            put("registeredSections", JSONArray(config.registeredSections))
            put("registeredSubjects", JSONArray(config.registeredSubjects))
            put("logoBase64", config.logoBase64)
            put("academicYear", config.academicYear)
            put("phone", config.phone)
            put("subscriptionExpiryDate", config.subscriptionExpiryDate.ifBlank { "2027-10-29" })
            put("featureSettings", featJson)
            put("enabledFeatures", featJson)
            put("urgentAlert", urgentAlertJson)
        }
        val jsonStr = obj.toString()
        val featStr = featJson.toString()
        val urgentStr = urgentAlertJson.toString()
        prefs.edit()
            .putString(scopedKey(sid, FirebaseConfigConstants.COL_SETTINGS), jsonStr)
            .putString(scopedKey(sid, FirebaseConfigConstants.COL_FEATURE_SETTINGS), featStr)
            .putString(scopedKey(sid, FirebaseConfigConstants.COL_URGENT_ALERT), urgentStr)
            .apply()
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_SETTINGS, jsonStr)
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_FEATURE_SETTINGS, featStr)
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_URGENT_ALERT, urgentStr)

        if (config.subjectConfigs.isNotEmpty()) {
            saveSubjectConfigs(config.subjectConfigs, sid)
        }
    }

    fun saveFeatureSettings(settings: SchoolFeatureSettings, schoolId: String = getActiveSchoolId()) {
        val sid = schoolId.ifBlank { getActiveSchoolId() }
        if (sid.isBlank()) return
        val currentCfg = loadSchoolConfig(sid)
        saveSchoolConfig(currentCfg.copy(schoolId = sid, featureSettings = settings))
    }

    private fun featureSettingsToJsonObject(f: SchoolFeatureSettings): JSONObject {
        val bus = f.busTrackingEnabled && f.busAlertsEnabled || (f.busAlertsEnabled != f.busTrackingEnabled && f.busAlertsEnabled)
        val study = f.studyMaterialsEnabled && f.studyLibraryEnabled || (f.studyLibraryEnabled != f.studyMaterialsEnabled && f.studyLibraryEnabled)
        val qr = f.digitalIdEnabled && f.qrScannerEnabled && f.qrAttendanceEnabled || (f.qrScannerEnabled != f.digitalIdEnabled && f.qrScannerEnabled) || (f.qrAttendanceEnabled != f.digitalIdEnabled && f.qrAttendanceEnabled)
        val lostAndSug = f.lostAndFoundEnabled

        val enabledFeaturesMap = JSONObject().apply {
            put("busTracking", bus)
            put("honorRoll", f.honorRollEnabled)
            put("directMessaging", f.directChatEnabled)
            put("digitalLibrary", study)
            put("leaveRequests", f.leaveRequestsEnabled)
            put("qrAttendance", qr)
            put("tuitionInstallments", f.tuitionEnabled)
            put("photoHomework", f.homeworkSubmissionEnabled)
            put("dailyBehavior", f.dailyBehaviorEnabled)
            put("smartDismissal", f.dismissalCallEnabled)
            put("quickQuizzes", f.quickQuizzesEnabled)
            put("lostAndFoundSuggestions", lostAndSug)
        }

        return JSONObject().apply {
            // Canonical 12 keys
            put("busTracking", bus)
            put("honorRoll", f.honorRollEnabled)
            put("directMessaging", f.directChatEnabled)
            put("digitalLibrary", study)
            put("leaveRequests", f.leaveRequestsEnabled)
            put("qrAttendance", qr)
            put("tuitionInstallments", f.tuitionEnabled)
            put("photoHomework", f.homeworkSubmissionEnabled)
            put("dailyBehavior", f.dailyBehaviorEnabled)
            put("smartDismissal", f.dismissalCallEnabled)
            put("quickQuizzes", f.quickQuizzesEnabled)
            put("lostAndFoundSuggestions", lostAndSug)
            put("enabledFeatures", enabledFeaturesMap)

            // Full SchoolFeatureSettings properties & aliases
            put("honorRollEnabled", f.honorRollEnabled)
            put("eventsGalleryEnabled", f.eventsGalleryEnabled)
            put("directChatEnabled", f.directChatEnabled)
            put("leaveRequestsEnabled", f.leaveRequestsEnabled)
            put("tuitionEnabled", f.tuitionEnabled)
            put("busTrackingEnabled", bus)
            put("busAlertsEnabled", bus)
            put("studyMaterialsEnabled", study)
            put("studyLibraryEnabled", study)
            put("quickQuizzesEnabled", f.quickQuizzesEnabled)
            put("dailyBehaviorEnabled", f.dailyBehaviorEnabled)
            put("homeworkSubmissionEnabled", f.homeworkSubmissionEnabled)
            put("dismissalCallEnabled", f.dismissalCallEnabled)
            put("lostAndFoundEnabled", lostAndSug)
            put("suggestionsBoxEnabled", lostAndSug)
            put("dismissalAndLostFoundEnabled", f.dismissalCallEnabled || lostAndSug)
            put("digitalIdEnabled", qr)
            put("qrScannerEnabled", qr)
            put("qrAttendanceEnabled", qr)
        }
    }

    private fun parseFeatureSettingsObject(rawObj: JSONObject): SchoolFeatureSettings {
        val o = rawObj.optJSONObject("featureSettings")
            ?: rawObj.optJSONObject("enabledFeatures")
            ?: rawObj

        fun boolKey(primary: String, vararg fallbacks: String, default: Boolean = true): Boolean {
            if (o.has(primary)) return o.optBoolean(primary, default)
            if (rawObj.has(primary)) return rawObj.optBoolean(primary, default)
            for (fb in fallbacks) {
                if (o.has(fb)) return o.optBoolean(fb, default)
                if (rawObj.has(fb)) return rawObj.optBoolean(fb, default)
            }
            return default
        }

        val bus = boolKey("busTracking", "busAlertsEnabled", "busTrackingEnabled")
        val honor = boolKey("honorRoll", "honorRollEnabled")
        val chat = boolKey("directMessaging", "directChatEnabled")
        val study = boolKey("digitalLibrary", "studyLibraryEnabled", "studyMaterialsEnabled")
        val leave = boolKey("leaveRequests", "leaveRequestsEnabled")
        val qr = boolKey("qrAttendance", "qrScannerEnabled", "qrAttendanceEnabled", "digitalIdEnabled")
        val tuition = boolKey("tuitionInstallments", "tuitionEnabled")
        val photoHw = boolKey("photoHomework", "homeworkSubmissionEnabled")
        val behavior = boolKey("dailyBehavior", "dailyBehaviorEnabled")
        val smartDismissal = boolKey("smartDismissal", "dismissalCallEnabled", "dismissalAndLostFoundEnabled")
        val quizzes = boolKey("quickQuizzes", "quickQuizzesEnabled")
        val lostAndSug = boolKey("lostAndFoundSuggestions", "lostAndFoundEnabled", "suggestionsBoxEnabled", "dismissalAndLostFoundEnabled")
        val eventsGallery = boolKey("eventsGalleryEnabled")

        return SchoolFeatureSettings(
            honorRollEnabled = honor,
            eventsGalleryEnabled = eventsGallery,
            directChatEnabled = chat,
            leaveRequestsEnabled = leave,
            tuitionEnabled = tuition,
            busTrackingEnabled = bus,
            busAlertsEnabled = bus,
            studyMaterialsEnabled = study,
            studyLibraryEnabled = study,
            quickQuizzesEnabled = quizzes,
            dailyBehaviorEnabled = behavior,
            dismissalAndLostFoundEnabled = smartDismissal || lostAndSug,
            suggestionsBoxEnabled = lostAndSug,
            digitalIdEnabled = qr,
            qrScannerEnabled = qr,
            qrAttendanceEnabled = qr,
            homeworkSubmissionEnabled = photoHw,
            dismissalCallEnabled = smartDismissal,
            lostAndFoundEnabled = lostAndSug
        )
    }

    fun loadFeatureSettings(schoolId: String = getActiveSchoolId()): SchoolFeatureSettings {
        if (schoolId.isBlank()) return SchoolFeatureSettings()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_FEATURE_SETTINGS), null)
            ?: return SchoolFeatureSettings()
        return parseFeatureSettingsJson(raw)
    }

    fun parseFeatureSettingsJson(raw: String): SchoolFeatureSettings {
        return try {
            parseFeatureSettingsObject(JSONObject(raw))
        } catch (_: Exception) {
            SchoolFeatureSettings()
        }
    }

    fun isSideDrawerEnabled(): Boolean = prefs.getBoolean("side_drawer_enabled", true)

    fun setSideDrawerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("side_drawer_enabled", enabled).apply()
    }

    fun loadSubjectConfigs(schoolId: String = getActiveSchoolId()): List<SubjectConfigItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_SUBJECTS), null)
            ?: return emptyList()
        return parseSubjectConfigsJson(raw, schoolId)
    }

    fun parseSubjectConfigsJson(raw: String, schoolId: String = getActiveSchoolId()): List<SubjectConfigItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SubjectConfigItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    SubjectConfigItem(
                        id = o.optString("id", "sub_${UUID.randomUUID().toString().take(5)}"),
                        name = o.optString("name"),
                        assignedClasses = jsonArrayToList(o.optJSONArray("assignedClasses")),
                        defaultMaxScore = o.optInt("defaultMaxScore", 100),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSubjectConfigs(subjects: List<SubjectConfigItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        subjects.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("name", InputSanitizer.sanitize(s.name))
                put("assignedClasses", JSONArray(s.assignedClasses))
                put("defaultMaxScore", s.defaultMaxScore)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_SUBJECTS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_SUBJECTS, jsonStr)
    }

    // =========================================================================
    // 2. schools/{schoolId}/users (Accounts & Students)
    // =========================================================================

    fun loadAccounts(schoolId: String = getActiveSchoolId()): List<UserAccount> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_USERS), null)
            ?: return emptyList()
        return parseAccountsJson(raw, schoolId)
    }

    fun parseAccountsJson(raw: String, schoolId: String = getActiveSchoolId()): List<UserAccount> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<UserAccount>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSchoolId = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSchoolId != schoolId) continue
                list.add(
                    UserAccount(
                        id = o.optString("id"),
                        username = o.optString("username"),
                        password = o.optString("password"),
                        fullName = o.optString("fullName"),
                        role = UserRole.valueOf(o.optString("role", UserRole.STUDENT.name)),
                        isActive = o.optBoolean("isActive", true),
                        phone = o.optString("phone"),
                        specialty = o.optString("specialty"),
                        assignedClasses = o.optString("assignedClasses"),
                        linkedStudentId = o.optString("linkedStudentId"),
                        gradeClass = o.optString("gradeClass", "الأول المتوسط"),
                        sectionName = o.optString("sectionName", "أ"),
                        address = o.optString("address", "الموصل - زمار"),
                        photoBase64 = o.optString("photoBase64", ""),
                        schoolId = itemSchoolId
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAccounts(accounts: List<UserAccount>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        accounts.forEach { acc ->
            arr.put(JSONObject().apply {
                put("id", acc.id)
                put("username", InputSanitizer.sanitizeUsername(acc.username))
                put("password", InputSanitizer.sanitize(acc.password))
                put("fullName", InputSanitizer.sanitize(acc.fullName))
                put("role", acc.role.name)
                put("isActive", acc.isActive)
                put("phone", InputSanitizer.sanitize(acc.phone))
                put("specialty", InputSanitizer.sanitize(acc.specialty))
                put("assignedClasses", InputSanitizer.sanitize(acc.assignedClasses))
                put("linkedStudentId", acc.linkedStudentId)
                put("gradeClass", InputSanitizer.sanitize(acc.gradeClass))
                put("sectionName", InputSanitizer.sanitize(acc.sectionName))
                put("address", InputSanitizer.sanitize(acc.address))
                put("photoBase64", acc.photoBase64)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_USERS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_USERS, jsonStr)
    }

    fun loadStudents(schoolId: String = getActiveSchoolId()): List<StudentRecord> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_HONOR_ROLL), null)
            ?: return emptyList()
        return parseStudentsJson(raw, schoolId)
    }

    fun parseStudentsJson(raw: String, schoolId: String = getActiveSchoolId()): List<StudentRecord> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<StudentRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSchoolId = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSchoolId != schoolId) continue
                list.add(
                    StudentRecord(
                        id = o.optString("id"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        parentName = o.optString("parentName"),
                        parentPhone = o.optString("parentPhone"),
                        address = o.optString("address", "الموصل - زمار"),
                        studentUsername = o.optString("studentUsername"),
                        studentPassword = o.optString("studentPassword"),
                        parentUsername = o.optString("parentUsername"),
                        parentPassword = o.optString("parentPassword"),
                        isActive = o.optBoolean("isActive", true),
                        photoBase64 = o.optString("photoBase64", ""),
                        excellencePoints = o.optInt("excellencePoints", 90),
                        isOnHonorRoll = o.optBoolean("isOnHonorRoll", false),
                        honorTitle = o.optString("honorTitle", "وسام التفوق الذهبي"),
                        heartCount = run {
                            val likedList = jsonArrayToList(o.optJSONArray("likedBy") ?: o.optJSONArray("likedByUserIds")).distinct()
                            if (likedList.isNotEmpty()) likedList.size else o.optInt("heartCount", 0)
                        },
                        likedByUserIds = jsonArrayToList(o.optJSONArray("likedBy") ?: o.optJSONArray("likedByUserIds")).distinct(),
                        isBusSubscribed = o.optBoolean("isBusSubscribed", false),
                        busRouteName = o.optString("busRouteName", "الخط الصباحي والمسائي"),
                        schoolId = itemSchoolId
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveStudents(students: List<StudentRecord>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        students.forEach { s ->
            val cleanLikedBy = s.likedByUserIds.filter { it.isNotBlank() }.distinct()
            val finalHeartCount = if (cleanLikedBy.isNotEmpty() || s.heartCount == 0) cleanLikedBy.size else s.heartCount
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("studentName", InputSanitizer.sanitize(s.studentName))
                put("gradeClass", InputSanitizer.sanitize(s.gradeClass))
                put("sectionName", InputSanitizer.sanitize(s.sectionName))
                put("parentName", InputSanitizer.sanitize(s.parentName))
                put("parentPhone", InputSanitizer.sanitize(s.parentPhone))
                put("address", InputSanitizer.sanitize(s.address))
                put("studentUsername", InputSanitizer.sanitizeUsername(s.studentUsername))
                put("studentPassword", InputSanitizer.sanitize(s.studentPassword))
                put("parentUsername", InputSanitizer.sanitizeUsername(s.parentUsername))
                put("parentPassword", InputSanitizer.sanitize(s.parentPassword))
                put("isActive", s.isActive)
                put("photoBase64", s.photoBase64)
                put("excellencePoints", s.excellencePoints)
                put("isOnHonorRoll", s.isOnHonorRoll)
                put("honorTitle", InputSanitizer.sanitize(s.honorTitle))
                put("heartCount", finalHeartCount)
                put("likedBy", JSONArray(cleanLikedBy))
                put("likedByUserIds", JSONArray(cleanLikedBy))
                put("isBusSubscribed", s.isBusSubscribed)
                put("busRouteName", InputSanitizer.sanitize(s.busRouteName))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_HONOR_ROLL), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_HONOR_ROLL, jsonStr)
    }

    // =========================================================================
    // 3. schools/{schoolId}/attendance
    // =========================================================================

    fun loadAttendance(schoolId: String = getActiveSchoolId()): List<AttendanceEntry> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_ATTENDANCE), null)
            ?: return emptyList()
        return parseAttendanceJson(raw, schoolId)
    }

    fun parseAttendanceJson(raw: String, schoolId: String = getActiveSchoolId()): List<AttendanceEntry> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AttendanceEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    AttendanceEntry(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        date = o.optString("date"),
                        status = AttendanceStatus.valueOf(o.optString("status", AttendanceStatus.PRESENT.name)),
                        note = o.optString("note"),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAttendance(entries: List<AttendanceEntry>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("studentId", e.studentId)
                put("studentName", InputSanitizer.sanitize(e.studentName))
                put("gradeClass", InputSanitizer.sanitize(e.gradeClass))
                put("sectionName", InputSanitizer.sanitize(e.sectionName))
                put("date", InputSanitizer.sanitize(e.date))
                put("status", e.status.name)
                put("note", InputSanitizer.sanitize(e.note))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_ATTENDANCE), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_ATTENDANCE, jsonStr)
    }

    // =========================================================================
    // 4. schools/{schoolId}/schedules_homework
    // =========================================================================

    fun loadSchedules(schoolId: String = getActiveSchoolId()): List<SchedulePeriod> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_SCHEDULES_PERIODS), null)
            ?: prefs.getString("${scopedKey(schoolId, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)}_periods", null)
            ?: return emptyList()
        return parseSchedulesJson(raw, schoolId)
    }

    fun parseSchedulesJson(raw: String, schoolId: String = getActiveSchoolId()): List<SchedulePeriod> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SchedulePeriod>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    SchedulePeriod(
                        id = o.optString("id"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        dayOfWeek = o.optString("dayOfWeek"),
                        periodNumber = o.optInt("periodNumber", 1),
                        subjectName = o.optString("subjectName"),
                        teacherName = o.optString("teacherName"),
                        timeSlot = o.optString("timeSlot"),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSchedules(schedules: List<SchedulePeriod>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        schedules.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("gradeClass", InputSanitizer.sanitize(s.gradeClass))
                put("sectionName", InputSanitizer.sanitize(s.sectionName))
                put("dayOfWeek", InputSanitizer.sanitize(s.dayOfWeek))
                put("periodNumber", s.periodNumber)
                put("subjectName", InputSanitizer.sanitize(s.subjectName))
                put("teacherName", InputSanitizer.sanitize(s.teacherName))
                put("timeSlot", InputSanitizer.sanitize(s.timeSlot))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit()
            .putString(scopedKey(schoolId, FirebaseConfigConstants.COL_SCHEDULES_PERIODS), jsonStr)
            .putString("${scopedKey(schoolId, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)}_periods", jsonStr)
            .apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_SCHEDULES_PERIODS, jsonStr)
    }

    fun loadHomeworks(schoolId: String = getActiveSchoolId()): List<HomeworkItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK), null)
            ?: return emptyList()
        return parseHomeworksJson(raw, schoolId)
    }

    fun parseHomeworksJson(raw: String, schoolId: String = getActiveSchoolId()): List<HomeworkItem> {
        return try {
            val arr = if (raw.trim().startsWith("{")) {
                JSONObject(raw).optJSONArray("homeworks") ?: JSONArray()
            } else {
                JSONArray(raw)
            }
            val list = mutableListOf<HomeworkItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    HomeworkItem(
                        id = o.optString("id"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        subjectName = o.optString("subjectName"),
                        teacherName = o.optString("teacherName"),
                        dailyPreparation = o.optString("dailyPreparation"),
                        homeworkDetails = o.optString("homeworkDetails"),
                        dueDate = o.optString("dueDate"),
                        createdAt = o.optString("createdAt"),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHomeworks(items: List<HomeworkItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { h ->
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("gradeClass", InputSanitizer.sanitize(h.gradeClass))
                put("sectionName", InputSanitizer.sanitize(h.sectionName))
                put("subjectName", InputSanitizer.sanitize(h.subjectName))
                put("teacherName", InputSanitizer.sanitize(h.teacherName))
                put("dailyPreparation", InputSanitizer.sanitize(h.dailyPreparation))
                put("homeworkDetails", InputSanitizer.sanitize(h.homeworkDetails))
                put("dueDate", InputSanitizer.sanitize(h.dueDate))
                put("createdAt", InputSanitizer.sanitize(h.createdAt))
                put("schoolId", schoolId)
            })
        }
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK), arr.toString()).apply()
        syncSchedulesAndHomeworksBundle(schoolId)
    }

    private fun syncSchedulesAndHomeworksBundle(schoolId: String) {
        val hwStr = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK), "[]") ?: "[]"
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK, hwStr)
    }

    // =========================================================================
    // 5. schools/{schoolId}/grades & Exam Schedules
    // =========================================================================

    fun loadExamSchedules(schoolId: String = getActiveSchoolId()): List<ExamScheduleItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_EXAMS), null)
            ?: prefs.getString("${scopedKey(schoolId, FirebaseConfigConstants.COL_GRADES)}_exams", null)
            ?: return emptyList()
        return parseExamSchedulesJson(raw, schoolId)
    }

    fun parseExamSchedulesJson(raw: String, schoolId: String = getActiveSchoolId()): List<ExamScheduleItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ExamScheduleItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ExamScheduleItem(
                        id = o.optString("id"),
                        gradeClass = o.optString("gradeClass"),
                        subjectName = o.optString("subjectName"),
                        examType = o.optString("examType"),
                        examDate = o.optString("examDate"),
                        dayName = o.optString("dayName"),
                        timeSlot = o.optString("timeSlot"),
                        topicsIncluded = o.optString("topicsIncluded"),
                        hallNumber = o.optString("hallNumber"),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveExamSchedules(exams: List<ExamScheduleItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        exams.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("gradeClass", InputSanitizer.sanitize(e.gradeClass))
                put("subjectName", InputSanitizer.sanitize(e.subjectName))
                put("examType", InputSanitizer.sanitize(e.examType))
                put("examDate", InputSanitizer.sanitize(e.examDate))
                put("dayName", InputSanitizer.sanitize(e.dayName))
                put("timeSlot", InputSanitizer.sanitize(e.timeSlot))
                put("topicsIncluded", InputSanitizer.sanitize(e.topicsIncluded))
                put("hallNumber", InputSanitizer.sanitize(e.hallNumber))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit()
            .putString(scopedKey(schoolId, FirebaseConfigConstants.COL_EXAMS), jsonStr)
            .putString("${scopedKey(schoolId, FirebaseConfigConstants.COL_GRADES)}_exams", jsonStr)
            .apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_EXAMS, jsonStr)
    }

    fun loadGrades(schoolId: String = getActiveSchoolId()): List<SubjectGrade> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_GRADES), null)
            ?: return emptyList()
        return parseGradesJson(raw, schoolId)
    }

    fun parseGradesJson(raw: String, schoolId: String = getActiveSchoolId()): List<SubjectGrade> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SubjectGrade>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val gCls = o.optString("gradeClass")
                val defaultScale = GradingRules.defaultMaxScoreForClass(gCls)
                val scale = o.optInt("maxScoreScale", defaultScale).let { if (it == 10) 10 else 100 }
                list.add(
                    SubjectGrade(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = gCls,
                        subjectName = o.optString("subjectName"),
                        dailyScore = o.optInt("dailyScore", 0),
                        month1Score = o.optInt("month1Score", 0),
                        month2Score = o.optInt("month2Score", 0),
                        midYearScore = o.optInt("midYearScore", 0),
                        finalExamScore = o.optInt("finalExamScore", 0),
                        maxScoreScale = scale,
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveGrades(grades: List<SubjectGrade>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        grades.forEach { g ->
            arr.put(JSONObject().apply {
                put("id", g.id)
                put("studentId", g.studentId)
                put("studentName", InputSanitizer.sanitize(g.studentName))
                put("gradeClass", InputSanitizer.sanitize(g.gradeClass))
                put("subjectName", InputSanitizer.sanitize(g.subjectName))
                put("dailyScore", g.dailyScore)
                put("month1Score", g.month1Score)
                put("month2Score", g.month2Score)
                put("midYearScore", g.midYearScore)
                put("finalExamScore", g.finalExamScore)
                put("maxScoreScale", g.maxScoreScale)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_GRADES), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_GRADES, jsonStr)
    }

    // =========================================================================
    // 6. schools/{schoolId}/tuition_fees
    // =========================================================================

    fun loadTuitions(schoolId: String = getActiveSchoolId()): List<TuitionRecord> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_TUITION_FEES), null)
            ?: return emptyList()
        return parseTuitionsJson(raw, schoolId)
    }

    fun parseTuitionsJson(raw: String, schoolId: String = getActiveSchoolId()): List<TuitionRecord> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TuitionRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val instArr = o.optJSONArray("installments") ?: JSONArray()
                val installments = mutableListOf<TuitionInstallment>()
                for (j in 0 until instArr.length()) {
                    val io = instArr.getJSONObject(j)
                    installments.add(
                        TuitionInstallment(
                            id = io.optString("id"),
                            amount = io.optLong("amount"),
                            paymentDate = io.optString("paymentDate"),
                            receiptNumber = io.optString("receiptNumber"),
                            note = io.optString("note")
                        )
                    )
                }
                list.add(
                    TuitionRecord(
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        parentName = o.optString("parentName"),
                        parentPhone = o.optString("parentPhone"),
                        totalFee = o.optLong("totalFee", 1500000L),
                        nextDueDate = o.optString("nextDueDate", "2026-11-15"),
                        installments = installments,
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTuitions(records: List<TuitionRecord>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        records.forEach { r ->
            val instArr = JSONArray()
            r.installments.forEach { inst ->
                instArr.put(JSONObject().apply {
                    put("id", inst.id)
                    put("amount", inst.amount)
                    put("paymentDate", InputSanitizer.sanitize(inst.paymentDate))
                    put("receiptNumber", InputSanitizer.sanitize(inst.receiptNumber))
                    put("note", InputSanitizer.sanitize(inst.note))
                })
            }
            arr.put(JSONObject().apply {
                put("studentId", r.studentId)
                put("studentName", InputSanitizer.sanitize(r.studentName))
                put("gradeClass", InputSanitizer.sanitize(r.gradeClass))
                put("sectionName", InputSanitizer.sanitize(r.sectionName))
                put("parentName", InputSanitizer.sanitize(r.parentName))
                put("parentPhone", InputSanitizer.sanitize(r.parentPhone))
                put("totalFee", r.totalFee)
                put("nextDueDate", InputSanitizer.sanitize(r.nextDueDate))
                put("installments", instArr)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_TUITION_FEES), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_TUITION_FEES, jsonStr)
    }

    // =========================================================================
    // 7. schools/{schoolId}/reports & schools/{schoolId}/messages
    // =========================================================================

    fun loadReports(schoolId: String = getActiveSchoolId()): List<ParentReport> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_REPORTS), null)
            ?: return emptyList()
        return parseReportsJson(raw, schoolId)
    }

    fun parseReportsJson(raw: String, schoolId: String = getActiveSchoolId()): List<ParentReport> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ParentReport>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ParentReport(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        parentName = o.optString("parentName"),
                        parentPhone = o.optString("parentPhone"),
                        reportType = ReportType.valueOf(o.optString("reportType", ReportType.APPRECIATION.name)),
                        title = o.optString("title"),
                        details = o.optString("details"),
                        senderName = o.optString("senderName"),
                        createdAt = o.optString("createdAt"),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReports(reports: List<ParentReport>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        reports.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("studentId", r.studentId)
                put("studentName", InputSanitizer.sanitize(r.studentName))
                put("gradeClass", InputSanitizer.sanitize(r.gradeClass))
                put("parentName", InputSanitizer.sanitize(r.parentName))
                put("parentPhone", InputSanitizer.sanitize(r.parentPhone))
                put("reportType", r.reportType.name)
                put("title", InputSanitizer.sanitize(r.title))
                put("details", InputSanitizer.sanitize(r.details))
                put("senderName", InputSanitizer.sanitize(r.senderName))
                put("createdAt", InputSanitizer.sanitize(r.createdAt))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_REPORTS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_REPORTS, jsonStr)
    }

    fun loadChatMessages(schoolId: String = getActiveSchoolId()): List<ChatMessage> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_MESSAGES), null)
            ?: return emptyList()
        return parseChatMessagesJson(raw, schoolId)
    }

    fun parseChatMessagesJson(raw: String, schoolId: String = getActiveSchoolId()): List<ChatMessage> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        senderId = o.optString("senderId", ""),
                        senderName = o.optString("senderName"),
                        senderRole = runCatching {
                            UserRole.valueOf(o.optString("senderRole", UserRole.ADMIN.name))
                        }.getOrDefault(UserRole.ADMIN),
                        recipientId = o.optString("recipientId", ""),
                        recipientName = o.optString("recipientName", ""),
                        recipientRole = runCatching {
                            UserRole.valueOf(o.optString("recipientRole", UserRole.PARENT.name))
                        }.getOrDefault(UserRole.PARENT),
                        conversationKey = o.optString("conversationKey", ""),
                        messageText = o.optString("messageText"),
                        timestamp = o.optString("timestamp"),
                        imageBase64 = o.optString("imageBase64", ""),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveChatMessages(messages: List<ChatMessage>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("studentId", m.studentId)
                put("studentName", InputSanitizer.sanitize(m.studentName))
                put("senderId", m.senderId)
                put("senderName", InputSanitizer.sanitize(m.senderName))
                put("senderRole", m.senderRole.name)
                put("recipientId", m.recipientId)
                put("recipientName", InputSanitizer.sanitize(m.recipientName))
                put("recipientRole", m.recipientRole.name)
                put("conversationKey", m.conversationKey)
                put("messageText", InputSanitizer.sanitize(m.messageText))
                put("timestamp", InputSanitizer.sanitize(m.timestamp))
                put("imageBase64", m.imageBase64)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_MESSAGES), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_MESSAGES, jsonStr)
    }

    // =========================================================================
    // 8. schools/{schoolId}/events_gallery & schools/{schoolId}/notifications
    // =========================================================================

    fun loadEvents(schoolId: String = getActiveSchoolId()): List<SchoolEvent> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_EVENTS_GALLERY), null)
            ?: return emptyList()
        return parseEventsJson(raw, schoolId)
    }

    fun parseEventsJson(raw: String, schoolId: String = getActiveSchoolId()): List<SchoolEvent> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SchoolEvent>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val likedList = jsonArrayToList(o.optJSONArray("likedBy") ?: o.optJSONArray("likedByUserIds")).distinct()
                val heartCount = if (likedList.isNotEmpty()) likedList.size else o.optInt("heartCount", 0)
                list.add(
                    SchoolEvent(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        category = o.optString("category"),
                        description = o.optString("description"),
                        date = o.optString("date"),
                        location = o.optString("location", "الموصل - زمار"),
                        imageBase64 = o.optString("imageBase64", ""),
                        authorName = o.optString("authorName", "إدارة المدرسة"),
                        heartCount = heartCount,
                        likedByUserIds = likedList,
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveEvents(events: List<SchoolEvent>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        events.forEach { ev ->
            val cleanLikedBy = ev.likedByUserIds.filter { it.isNotBlank() }.distinct()
            val finalHeartCount = if (cleanLikedBy.isNotEmpty() || ev.heartCount == 0) cleanLikedBy.size else ev.heartCount
            arr.put(JSONObject().apply {
                put("id", ev.id)
                put("title", InputSanitizer.sanitize(ev.title))
                put("category", InputSanitizer.sanitize(ev.category))
                put("description", InputSanitizer.sanitize(ev.description))
                put("date", InputSanitizer.sanitize(ev.date))
                put("location", InputSanitizer.sanitize(ev.location))
                put("imageBase64", ev.imageBase64)
                put("authorName", InputSanitizer.sanitize(ev.authorName))
                put("heartCount", finalHeartCount)
                put("likedBy", JSONArray(cleanLikedBy))
                put("likedByUserIds", JSONArray(cleanLikedBy))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_EVENTS_GALLERY), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_EVENTS_GALLERY, jsonStr)
    }

    fun loadNotifications(schoolId: String = getActiveSchoolId()): List<AppNotification> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_NOTIFICATIONS), null)
            ?: return emptyList()
        return parseNotificationsJson(raw, schoolId)
    }

    fun parseNotificationsJson(raw: String, schoolId: String = getActiveSchoolId()): List<AppNotification> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AppNotification>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    AppNotification(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        body = o.optString("body"),
                        type = NotificationType.valueOf(o.optString("type", NotificationType.GENERAL.name)),
                        targetStudentId = o.optString("targetStudentId", ""),
                        targetUserId = o.optString("targetUserId", ""),
                        targetGradeClass = o.optString("targetGradeClass", ""),
                        targetSectionName = o.optString("targetSectionName", ""),
                        targetRole = o.optString("targetRole", ""),
                        senderUserId = o.optString("senderUserId", ""),
                        createdAt = o.optString("createdAt"),
                        isRead = o.optBoolean("isRead", false),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveNotifications(notifications: List<AppNotification>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        notifications.forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("title", InputSanitizer.sanitize(n.title))
                put("body", InputSanitizer.sanitize(n.body))
                put("type", n.type.name)
                put("targetStudentId", n.targetStudentId)
                put("targetUserId", n.targetUserId)
                put("targetGradeClass", InputSanitizer.sanitize(n.targetGradeClass))
                put("targetSectionName", InputSanitizer.sanitize(n.targetSectionName))
                put("targetRole", n.targetRole)
                put("senderUserId", n.senderUserId)
                put("createdAt", InputSanitizer.sanitize(n.createdAt))
                put("isRead", n.isRead)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_NOTIFICATIONS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_NOTIFICATIONS, jsonStr)
    }

    // =========================================================================
    // 8b. schools/{schoolId}/leave_requests & schools/{schoolId}/study_materials
    // =========================================================================

    fun loadLeaveRequests(schoolId: String = getActiveSchoolId()): List<LeaveRequestItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_LEAVE_REQUESTS), null)
            ?: return emptyList()
        return parseLeaveRequestsJson(raw, schoolId)
    }

    fun parseLeaveRequestsJson(raw: String, schoolId: String = getActiveSchoolId()): List<LeaveRequestItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<LeaveRequestItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    LeaveRequestItem(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        parentName = o.optString("parentName"),
                        parentPhone = o.optString("parentPhone"),
                        leaveType = runCatching {
                            LeaveType.valueOf(o.optString("leaveType", LeaveType.SICK.name))
                        }.getOrDefault(LeaveType.SICK),
                        leaveDate = o.optString("leaveDate"),
                        durationDays = o.optInt("durationDays", 1).coerceAtLeast(1),
                        reason = o.optString("reason"),
                        status = runCatching {
                            LeaveRequestStatus.valueOf(o.optString("status", LeaveRequestStatus.PENDING.name))
                        }.getOrDefault(LeaveRequestStatus.PENDING),
                        adminResponseNote = o.optString("adminResponseNote", ""),
                        createdAt = o.optString("createdAt"),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLeaveRequests(items: List<LeaveRequestItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("studentId", item.studentId)
                put("studentName", InputSanitizer.sanitize(item.studentName))
                put("gradeClass", InputSanitizer.sanitize(item.gradeClass))
                put("sectionName", InputSanitizer.sanitize(item.sectionName))
                put("parentName", InputSanitizer.sanitize(item.parentName))
                put("parentPhone", InputSanitizer.sanitize(item.parentPhone))
                put("leaveType", item.leaveType.name)
                put("leaveDate", InputSanitizer.sanitize(item.leaveDate))
                put("durationDays", item.durationDays)
                put("reason", InputSanitizer.sanitize(item.reason))
                put("status", item.status.name)
                put("adminResponseNote", InputSanitizer.sanitize(item.adminResponseNote))
                put("createdAt", InputSanitizer.sanitize(item.createdAt))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_LEAVE_REQUESTS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_LEAVE_REQUESTS, jsonStr)
    }

    fun loadStudyMaterials(schoolId: String = getActiveSchoolId()): List<StudyMaterialItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_STUDY_MATERIALS), null)
            ?: return emptyList()
        return parseStudyMaterialsJson(raw, schoolId)
    }

    fun parseStudyMaterialsJson(raw: String, schoolId: String = getActiveSchoolId()): List<StudyMaterialItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<StudyMaterialItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    StudyMaterialItem(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        subjectName = o.optString("subjectName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName", ""),
                        teacherName = o.optString("teacherName"),
                        description = o.optString("description"),
                        fileUrl = o.optString("fileUrl", ""),
                        imageBase64 = o.optString("imageBase64", ""),
                        materialType = o.optString("materialType", "ملخص دراسي / ملزمة PDF"),
                        createdAt = o.optString("createdAt"),
                        schoolId = o.optString("schoolId", schoolId)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveStudyMaterials(items: List<StudyMaterialItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("title", InputSanitizer.sanitize(m.title))
                put("subjectName", InputSanitizer.sanitize(m.subjectName))
                put("gradeClass", InputSanitizer.sanitize(m.gradeClass))
                put("sectionName", InputSanitizer.sanitize(m.sectionName))
                put("teacherName", InputSanitizer.sanitize(m.teacherName))
                put("description", InputSanitizer.sanitize(m.description))
                put("fileUrl", m.fileUrl.trim())
                put("imageBase64", m.imageBase64)
                put("materialType", InputSanitizer.sanitize(m.materialType))
                put("createdAt", InputSanitizer.sanitize(m.createdAt))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_STUDY_MATERIALS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_STUDY_MATERIALS, jsonStr)
    }

    // =========================================================================
    // 9. FULL SCHOOL BACKUP & RESTORE (JSON) FOR ADMIN ONLY
    // =========================================================================

    fun exportFullSchoolBackupJson(schoolId: String = getActiveSchoolId()): String {
        val sid = schoolId.ifBlank { getActiveSchoolId() }
        val root = JSONObject().apply {
            put("backupVersion", 2)
            put("schoolId", sid)
            put("exportedAt", System.currentTimeMillis())
            put("firebaseProjectId", FirebaseConfigConstants.PROJECT_ID)
            put("settings", JSONObject(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_SETTINGS), "{}") ?: "{}"))
            put("subjects", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_SUBJECTS), "[]") ?: "[]"))
            put("users", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_USERS), "[]") ?: "[]"))
            put("honor_roll", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_HONOR_ROLL), "[]") ?: "[]"))
            put("attendance", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_ATTENDANCE), "[]") ?: "[]"))
            put("schedules_homework", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK), "[]") ?: "[]"))
            put("schedules_periods", JSONArray(prefs.getString("${scopedKey(sid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)}_periods", "[]") ?: "[]"))
            put("grades", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_GRADES), "[]") ?: "[]"))
            put("exams", JSONArray(prefs.getString("${scopedKey(sid, FirebaseConfigConstants.COL_GRADES)}_exams", "[]") ?: "[]"))
            put("tuition_fees", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_TUITION_FEES), "[]") ?: "[]"))
            put("reports", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_REPORTS), "[]") ?: "[]"))
            put("messages", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_MESSAGES), "[]") ?: "[]"))
            put("events_gallery", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_EVENTS_GALLERY), "[]") ?: "[]"))
            put("notifications", JSONArray(prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_NOTIFICATIONS), "[]") ?: "[]"))
        }
        return root.toString(2)
    }

    fun importFullSchoolBackupJson(rawBackupJson: String, currentSchoolId: String = getActiveSchoolId()): Result<String> {
        return try {
            val root = JSONObject(rawBackupJson.trim())
            val sid = root.optString("schoolId", currentSchoolId).ifBlank { currentSchoolId }
            if (sid.isBlank()) {
                return Result.failure(IllegalArgumentException("ملف النسخة الاحتياطية لا يحتوي على معرّف مدرسة صالح (schoolId)"))
            }
            setActiveSchoolId(sid)

            val editor = prefs.edit()
            root.optJSONObject("settings")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_SETTINGS), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_SETTINGS, s)
            }
            root.optJSONArray("subjects")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_SUBJECTS), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_SUBJECTS, s)
            }
            root.optJSONArray("users")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_USERS), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_USERS, s)
            }
            root.optJSONArray("honor_roll")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_HONOR_ROLL), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_HONOR_ROLL, s)
            }
            root.optJSONArray("attendance")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_ATTENDANCE), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_ATTENDANCE, s)
            }
            root.optJSONArray("schedules_homework")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK, s)
            }
            root.optJSONArray("schedules_periods")?.let {
                editor.putString("${scopedKey(sid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)}_periods", it.toString())
            }
            root.optJSONArray("grades")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_GRADES), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_GRADES, s)
            }
            root.optJSONArray("exams")?.let {
                editor.putString("${scopedKey(sid, FirebaseConfigConstants.COL_GRADES)}_exams", it.toString())
            }
            root.optJSONArray("tuition_fees")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_TUITION_FEES), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_TUITION_FEES, s)
            }
            root.optJSONArray("reports")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_REPORTS), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_REPORTS, s)
            }
            root.optJSONArray("messages")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_MESSAGES), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_MESSAGES, s)
            }
            root.optJSONArray("events_gallery")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_EVENTS_GALLERY), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_EVENTS_GALLERY, s)
            }
            root.optJSONArray("notifications")?.let {
                val s = it.toString()
                editor.putString(scopedKey(sid, FirebaseConfigConstants.COL_NOTIFICATIONS), s)
                FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_NOTIFICATIONS, s)
            }
            editor.apply()
            Result.success(sid)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("فشل استعادة النسخة الاحتياطية: تأكد من صحة صيغة JSON"))
        }
    }

    // =========================================================================
    // 10. DEVELOPER SUPER-ADMIN PANEL: LICENSES (school_licenses) & SCHOOLS REGISTRY
    // =========================================================================

    fun generateUniqueThreeDigitSchoolCode(): String {
        val existingIds = loadRegisteredSchools().map { it.schoolId }.toSet()
        for (attempt in 0 until 200) {
            val candidate = (101..999).random().toString()
            if (candidate !in existingIds) return candidate
        }
        return (101..999).random().toString()
    }

    fun loadSchoolLicenses(): List<SchoolLicense> {
        val raw = prefs.getString("global_school_licenses", null) ?: return emptyList()
        return parseSchoolLicensesJson(raw)
    }

    fun parseSchoolLicensesJson(raw: String): List<SchoolLicense> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SchoolLicense>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val code = o.optString("code").trim()
                if (code.isNotEmpty()) {
                    list.add(
                        SchoolLicense(
                            code = code,
                            used = o.optBoolean("used", false),
                            createdAt = o.optString("createdAt", "2026-10-29"),
                            usedBySchoolId = o.optString("usedBySchoolId", ""),
                            usedBySchoolName = o.optString("usedBySchoolName", ""),
                            usedAt = o.optString("usedAt", "")
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSchoolLicenses(licenses: List<SchoolLicense>) {
        val arr = JSONArray()
        licenses.forEach { lic ->
            arr.put(JSONObject().apply {
                put("code", lic.code)
                put("used", lic.used)
                put("createdAt", lic.createdAt)
                put("usedBySchoolId", lic.usedBySchoolId)
                put("usedBySchoolName", lic.usedBySchoolName)
                put("usedAt", lic.usedAt)
            })
            FirebaseCloudSync.pushSingleLicenseDocumentAsync(
                code = lic.code,
                used = lic.used,
                createdAt = lic.createdAt,
                usedBySchoolId = lic.usedBySchoolId,
                usedBySchoolName = lic.usedBySchoolName,
                usedAt = lic.usedAt
            )
        }
        val jsonStr = arr.toString()
        prefs.edit().putString("global_school_licenses", jsonStr).apply()
        FirebaseCloudSync.pushGlobalCollectionAsync(
            collectionName = FirebaseConfigConstants.COL_SCHOOL_LICENSES,
            docName = "registry",
            jsonPayload = jsonStr
        )
    }

    fun loadRegisteredSchools(): List<RegisteredSchoolSummary> {
        val raw = prefs.getString("global_registered_schools", null) ?: return emptyList()
        return parseRegisteredSchoolsJson(raw)
    }

    fun parseRegisteredSchoolsJson(raw: String): List<RegisteredSchoolSummary> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<RegisteredSchoolSummary>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val sid = o.optString("schoolId").trim()
                if (sid.isNotEmpty()) {
                    list.add(
                        RegisteredSchoolSummary(
                            schoolId = sid,
                            schoolName = o.optString("schoolName", "مدرستي النموذجية"),
                            address = o.optString("address", "الموصل - زمار"),
                            schoolStage = o.optString("schoolStage", "ثانوية"),
                            schoolType = o.optString("schoolType", "مدرسة أهلية"),
                            createdAt = o.optString("createdAt", "2026-10-29"),
                            isActive = o.optBoolean("isActive", true),
                            adminUsername = o.optString("adminUsername", ""),
                            subscriptionExpiryDate = o.optString("subscriptionExpiryDate", "2027-10-29").ifBlank { "2027-10-29" }
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRegisteredSchools(schools: List<RegisteredSchoolSummary>) {
        val arr = JSONArray()
        schools.forEach { s ->
            arr.put(JSONObject().apply {
                put("schoolId", s.schoolId)
                put("schoolName", InputSanitizer.sanitize(s.schoolName))
                put("address", InputSanitizer.sanitize(s.address))
                put("schoolStage", s.schoolStage)
                put("schoolType", s.schoolType)
                put("createdAt", s.createdAt)
                put("isActive", s.isActive)
                put("adminUsername", s.adminUsername)
                put("subscriptionExpiryDate", s.subscriptionExpiryDate.ifBlank { "2027-10-29" })
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString("global_registered_schools", jsonStr).apply()
        FirebaseCloudSync.pushGlobalCollectionAsync(
            collectionName = FirebaseConfigConstants.COL_SCHOOLS_REGISTRY,
            docName = "registry",
            jsonPayload = jsonStr
        )
    }

    fun upsertRegisteredSchool(config: SchoolConfig, adminUsername: String = "") {
        val current = loadRegisteredSchools().toMutableList()
        val existingIdx = current.indexOfFirst { it.schoolId == config.schoolId }
        val resolvedAdminUser = if (adminUsername.isNotBlank()) {
            adminUsername
        } else if (existingIdx >= 0) {
            current[existingIdx].adminUsername
        } else {
            ""
        }
        val resolvedExpiry = config.subscriptionExpiryDate.ifBlank {
            if (existingIdx >= 0) current[existingIdx].subscriptionExpiryDate else "2027-10-29"
        }
        val summary = RegisteredSchoolSummary(
            schoolId = config.schoolId,
            schoolName = config.schoolName,
            address = config.address,
            schoolStage = config.schoolStage.arabicLabel,
            schoolType = config.schoolType.arabicLabel,
            createdAt = config.createdAt.ifBlank { "2026-10-29" },
            isActive = config.isActive,
            adminUsername = resolvedAdminUser,
            subscriptionExpiryDate = resolvedExpiry
        )
        if (existingIdx >= 0) {
            current[existingIdx] = summary
        } else {
            current.add(0, summary)
        }
        saveRegisteredSchools(current)
    }

    fun loadDeveloperBroadcasts(): List<DeveloperBroadcastItem> {
        val raw = prefs.getString("global_developer_broadcasts", null) ?: return emptyList()
        return parseDeveloperBroadcastsJson(raw)
    }

    fun parseDeveloperBroadcastsJson(raw: String): List<DeveloperBroadcastItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<DeveloperBroadcastItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    DeveloperBroadcastItem(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        message = o.optString("message"),
                        senderName = o.optString("senderName", "المطور أحمد أبو رسلان"),
                        createdAt = o.optString("createdAt"),
                        targetSchoolId = o.optString("targetSchoolId", "ALL")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDeveloperBroadcasts(items: List<DeveloperBroadcastItem>) {
        val jsonStr = serializeDeveloperBroadcastsJson(items)
        prefs.edit().putString("global_developer_broadcasts", jsonStr).apply()
        FirebaseCloudSync.pushGlobalCollectionAsync(
            collectionName = FirebaseConfigConstants.COL_DEV_BROADCASTS,
            docName = "registry",
            jsonPayload = jsonStr
        )
    }

    fun saveDeveloperBroadcastsLocalOnly(items: List<DeveloperBroadcastItem>) {
        val jsonStr = serializeDeveloperBroadcastsJson(items)
        prefs.edit().putString("global_developer_broadcasts", jsonStr).apply()
    }

    private fun serializeDeveloperBroadcastsJson(items: List<DeveloperBroadcastItem>): String {
        val arr = JSONArray()
        items.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("title", InputSanitizer.sanitize(b.title))
                put("message", InputSanitizer.sanitize(b.message))
                put("senderName", InputSanitizer.sanitize(b.senderName))
                put("createdAt", InputSanitizer.sanitize(b.createdAt))
                put("targetSchoolId", b.targetSchoolId)
            })
        }
        return arr.toString()
    }

    fun loadDismissedBroadcastIds(): Set<String> {
        return prefs.getStringSet("dismissed_dev_broadcast_ids", emptySet())?.toSet() ?: emptySet()
    }

    fun saveDismissedBroadcastIds(ids: Set<String>) {
        prefs.edit().putStringSet("dismissed_dev_broadcast_ids", ids).apply()
    }

    /**
     * Resolves a school's real name (`schoolName`) from local `schools/{schoolId}/settings` cache or registry.
     */
    fun resolveSchoolNameLocally(schoolId: String): String? {
        val cleanSid = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSid.isBlank()) return null

        val rawSettings = prefs.getString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SETTINGS), null)
        if (!rawSettings.isNullOrBlank()) {
            try {
                val obj = JSONObject(rawSettings)
                val name = obj.optString("schoolName").trim()
                if (name.isNotEmpty()) return name
            } catch (_: Exception) {
            }
        }

        val fromRegistry = loadRegisteredSchools()
            .firstOrNull { it.schoolId == cleanSid }
            ?.schoolName
            ?.trim()
        if (!fromRegistry.isNullOrEmpty()) return fromRegistry

        return null
    }

    /**
     * Fetches the school's real `schoolName` from `schools/{schoolId}/settings` in Firestore/RTDB
     * (or falls back to local cache / schools registry).
     */
    suspend fun fetchSchoolNameFromSettings(schoolId: String): String? {
        val cleanSid = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSid.isBlank()) return null

        resolveSchoolNameLocally(cleanSid)?.let { return it }

        val settingsPayload = FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_SETTINGS)
        if (!settingsPayload.isNullOrBlank()) {
            prefs.edit().putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SETTINGS), settingsPayload).apply()
            try {
                val obj = JSONObject(settingsPayload)
                val name = obj.optString("schoolName").trim()
                if (name.isNotEmpty()) return name
            } catch (_: Exception) {
            }
        }

        val registryPayload = FirebaseCloudSync.fetchGlobalCollectionPayload(
            collectionName = FirebaseConfigConstants.COL_SCHOOLS_REGISTRY,
            docName = "registry"
        )
        if (!registryPayload.isNullOrBlank()) {
            prefs.edit().putString("global_registered_schools", registryPayload).apply()
            val parsed = parseRegisteredSchoolsJson(registryPayload)
            val matchedName = parsed.firstOrNull { it.schoolId == cleanSid }?.schoolName?.trim()
            if (!matchedName.isNullOrEmpty()) return matchedName
        }

        return null
    }

    /**
     * Pulls a school's sub-collections from Firebase (Firestore/RTDB) into local storage
     * so that logging in with a prefixed username (e.g. 105-1) from any phone works immediately.
     */
    suspend fun syncFullSchoolFromCloud(schoolId: String): Boolean {
        val cleanSid = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSid.isBlank()) return false

        var foundAny = false
        val editor = prefs.edit()

        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_SETTINGS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SETTINGS), it)
                foundAny = true
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_SUBJECTS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SUBJECTS), it)
                foundAny = true
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_USERS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_USERS), it)
                foundAny = true
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_HONOR_ROLL)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_HONOR_ROLL), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_ATTENDANCE)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_ATTENDANCE), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_SCHEDULES_PERIODS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SCHEDULES_PERIODS), it)
                editor.putString("${scopedKey(cleanSid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)}_periods", it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_GRADES)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_GRADES), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_EXAMS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_EXAMS), it)
                editor.putString("${scopedKey(cleanSid, FirebaseConfigConstants.COL_GRADES)}_exams", it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_TUITION_FEES)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_TUITION_FEES), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_REPORTS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_REPORTS), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_MESSAGES)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_MESSAGES), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_EVENTS_GALLERY)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_EVENTS_GALLERY), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_NOTIFICATIONS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_NOTIFICATIONS), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_LEAVE_REQUESTS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_LEAVE_REQUESTS), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_STUDY_MATERIALS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_STUDY_MATERIALS), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_FEATURE_SETTINGS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_FEATURE_SETTINGS), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_DAILY_BEHAVIOR)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_DAILY_BEHAVIOR), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_QUICK_QUIZZES)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_QUICK_QUIZZES), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_DISMISSAL_CALLS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_DISMISSAL_CALLS), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_LOST_AND_FOUND)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_LOST_AND_FOUND), it)
            }
        }
        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS)?.let {
            if (it.isNotBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS), it)
            }
        }

        editor.apply()
        return foundAny
    }

    // =========================================================================
    // 14. FIELD FEATURES & DEVELOPER GLOBAL CONFIGS
    // =========================================================================

    fun loadDailyBehaviors(schoolId: String = getActiveSchoolId()): List<DailyBehaviorLogItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_DAILY_BEHAVIOR), null)
            ?: return emptyList()
        return parseDailyBehaviorsJson(raw, schoolId)
    }

    fun parseDailyBehaviorsJson(raw: String, schoolId: String = getActiveSchoolId()): List<DailyBehaviorLogItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<DailyBehaviorLogItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val bType = try {
                    DailyBehaviorType.valueOf(o.optString("behaviorType", DailyBehaviorType.EXCELLENT_PARTICIPATION.name))
                } catch (_: Exception) {
                    DailyBehaviorType.EXCELLENT_PARTICIPATION
                }
                list.add(
                    DailyBehaviorLogItem(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        subjectName = o.optString("subjectName", "عام"),
                        behaviorType = bType,
                        note = o.optString("note"),
                        teacherName = o.optString("teacherName"),
                        date = o.optString("date"),
                        schoolId = itemSid
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDailyBehaviors(items: List<DailyBehaviorLogItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("studentId", item.studentId)
                put("studentName", InputSanitizer.sanitize(item.studentName))
                put("gradeClass", InputSanitizer.sanitize(item.gradeClass))
                put("sectionName", InputSanitizer.sanitize(item.sectionName))
                put("subjectName", InputSanitizer.sanitize(item.subjectName))
                put("behaviorType", item.behaviorType.name)
                put("note", InputSanitizer.sanitize(item.note))
                put("teacherName", InputSanitizer.sanitize(item.teacherName))
                put("date", item.date)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_DAILY_BEHAVIOR), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_DAILY_BEHAVIOR, jsonStr)
    }

    fun loadHomeworkSubmissions(schoolId: String = getActiveSchoolId()): List<HomeworkSubmissionItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS), null)
            ?: return emptyList()
        return parseHomeworkSubmissionsJson(raw, schoolId)
    }

    fun parseHomeworkSubmissionsJson(raw: String, schoolId: String = getActiveSchoolId()): List<HomeworkSubmissionItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<HomeworkSubmissionItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val st = try {
                    HomeworkSubmissionStatus.valueOf(o.optString("status", HomeworkSubmissionStatus.PENDING_REVIEW.name))
                } catch (_: Exception) {
                    HomeworkSubmissionStatus.PENDING_REVIEW
                }
                list.add(
                    HomeworkSubmissionItem(
                        id = o.optString("id"),
                        homeworkId = o.optString("homeworkId"),
                        subjectName = o.optString("subjectName"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        note = o.optString("note"),
                        photoBase64 = o.optString("photoBase64"),
                        submittedAt = o.optString("submittedAt"),
                        status = st,
                        teacherFeedback = o.optString("teacherFeedback"),
                        homeworkScore = o.optString("homeworkScore"),
                        reviewedByTeacher = o.optString("reviewedByTeacher"),
                        schoolId = itemSid
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHomeworkSubmissions(items: List<HomeworkSubmissionItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("homeworkId", item.homeworkId)
                put("subjectName", InputSanitizer.sanitize(item.subjectName))
                put("studentId", item.studentId)
                put("studentName", InputSanitizer.sanitize(item.studentName))
                put("gradeClass", InputSanitizer.sanitize(item.gradeClass))
                put("sectionName", InputSanitizer.sanitize(item.sectionName))
                put("note", InputSanitizer.sanitize(item.note))
                put("photoBase64", item.photoBase64)
                put("submittedAt", item.submittedAt)
                put("status", item.status.name)
                put("teacherFeedback", InputSanitizer.sanitize(item.teacherFeedback))
                put("homeworkScore", InputSanitizer.sanitize(item.homeworkScore))
                put("reviewedByTeacher", InputSanitizer.sanitize(item.reviewedByTeacher))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS, jsonStr)
    }

    fun loadQuickQuizzes(schoolId: String = getActiveSchoolId()): List<QuickQuizItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_QUICK_QUIZZES), null)
            ?: return emptyList()
        return parseQuickQuizzesJson(raw, schoolId)
    }

    fun parseQuickQuizzesJson(raw: String, schoolId: String = getActiveSchoolId()): List<QuickQuizItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<QuickQuizItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val qType = try {
                    QuickQuizType.valueOf(o.optString("quizType", QuickQuizType.MULTIPLE_CHOICE.name))
                } catch (_: Exception) {
                    QuickQuizType.MULTIPLE_CHOICE
                }
                val answersObj = o.optJSONObject("studentAnswers")
                val answersMap = mutableMapOf<String, Int>()
                if (answersObj != null) {
                    val keys = answersObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        answersMap[k] = answersObj.optInt(k, 0)
                    }
                }
                list.add(
                    QuickQuizItem(
                        id = o.optString("id"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        subjectName = o.optString("subjectName"),
                        teacherName = o.optString("teacherName"),
                        questionText = o.optString("questionText"),
                        quizType = qType,
                        options = jsonArrayToList(o.optJSONArray("options")),
                        correctOptionIndex = o.optInt("correctOptionIndex", 0),
                        createdAt = o.optString("createdAt"),
                        studentAnswers = answersMap,
                        schoolId = itemSid
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveQuickQuizzes(items: List<QuickQuizItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { q ->
            val ansObj = JSONObject()
            q.studentAnswers.forEach { (stuId, idx) -> ansObj.put(stuId, idx) }
            arr.put(JSONObject().apply {
                put("id", q.id)
                put("gradeClass", InputSanitizer.sanitize(q.gradeClass))
                put("sectionName", InputSanitizer.sanitize(q.sectionName))
                put("subjectName", InputSanitizer.sanitize(q.subjectName))
                put("teacherName", InputSanitizer.sanitize(q.teacherName))
                put("questionText", InputSanitizer.sanitize(q.questionText))
                put("quizType", q.quizType.name)
                put("options", JSONArray(q.options))
                put("correctOptionIndex", q.correctOptionIndex)
                put("createdAt", q.createdAt)
                put("studentAnswers", ansObj)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_QUICK_QUIZZES), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_QUICK_QUIZZES, jsonStr)
    }

    fun loadDismissalCalls(schoolId: String = getActiveSchoolId()): List<DismissalCallItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_DISMISSAL_CALLS), null)
            ?: return emptyList()
        return parseDismissalCallsJson(raw, schoolId)
    }

    fun parseDismissalCallsJson(raw: String, schoolId: String = getActiveSchoolId()): List<DismissalCallItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<DismissalCallItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                list.add(
                    DismissalCallItem(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        parentName = o.optString("parentName"),
                        parentPhone = o.optString("parentPhone"),
                        requestedAtTime = o.optString("requestedAtTime"),
                        status = o.optString("status", "بانتظار خروج الطالب"),
                        isHandled = o.optBoolean("isHandled", false),
                        schoolId = itemSid
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveDismissalCalls(items: List<DismissalCallItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("studentId", c.studentId)
                put("studentName", InputSanitizer.sanitize(c.studentName))
                put("gradeClass", InputSanitizer.sanitize(c.gradeClass))
                put("sectionName", InputSanitizer.sanitize(c.sectionName))
                put("parentName", InputSanitizer.sanitize(c.parentName))
                put("parentPhone", InputSanitizer.sanitize(c.parentPhone))
                put("requestedAtTime", c.requestedAtTime)
                put("status", InputSanitizer.sanitize(c.status))
                put("isHandled", c.isHandled)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_DISMISSAL_CALLS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_DISMISSAL_CALLS, jsonStr)
    }

    fun loadLostAndFoundItems(schoolId: String = getActiveSchoolId()): List<LostAndFoundItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_LOST_AND_FOUND), null)
            ?: return emptyList()
        return parseLostAndFoundJson(raw, schoolId)
    }

    fun parseLostAndFoundJson(raw: String, schoolId: String = getActiveSchoolId()): List<LostAndFoundItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<LostAndFoundItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val st = try {
                    LostAndFoundStatus.valueOf(o.optString("status", LostAndFoundStatus.AT_ADMINISTRATION.name))
                } catch (_: Exception) {
                    LostAndFoundStatus.AT_ADMINISTRATION
                }
                list.add(
                    LostAndFoundItem(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        description = o.optString("description"),
                        foundLocation = o.optString("foundLocation"),
                        date = o.optString("date"),
                        photoBase64 = o.optString("photoBase64"),
                        status = st,
                        publisherName = o.optString("publisherName", "إدارة المدرسة"),
                        schoolId = itemSid
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLostAndFoundItems(items: List<LostAndFoundItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", InputSanitizer.sanitize(item.title))
                put("description", InputSanitizer.sanitize(item.description))
                put("foundLocation", InputSanitizer.sanitize(item.foundLocation))
                put("date", item.date)
                put("photoBase64", item.photoBase64)
                put("status", item.status.name)
                put("publisherName", InputSanitizer.sanitize(item.publisherName))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_LOST_AND_FOUND), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_LOST_AND_FOUND, jsonStr)
    }

    fun loadPrivateSuggestions(schoolId: String = getActiveSchoolId()): List<PrivateSuggestionItem> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS), null)
            ?: return emptyList()
        return parsePrivateSuggestionsJson(raw, schoolId)
    }

    fun parsePrivateSuggestionsJson(raw: String, schoolId: String = getActiveSchoolId()): List<PrivateSuggestionItem> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PrivateSuggestionItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                list.add(
                    PrivateSuggestionItem(
                        id = o.optString("id"),
                        senderName = o.optString("senderName"),
                        senderRoleLabel = o.optString("senderRoleLabel"),
                        category = o.optString("category", "مقترح تطويري"),
                        title = o.optString("title"),
                        message = o.optString("message"),
                        createdAt = o.optString("createdAt"),
                        adminReply = o.optString("adminReply"),
                        schoolId = itemSid
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePrivateSuggestions(items: List<PrivateSuggestionItem>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("senderName", InputSanitizer.sanitize(item.senderName))
                put("senderRoleLabel", InputSanitizer.sanitize(item.senderRoleLabel))
                put("category", InputSanitizer.sanitize(item.category))
                put("title", InputSanitizer.sanitize(item.title))
                put("message", InputSanitizer.sanitize(item.message))
                put("createdAt", item.createdAt)
                put("adminReply", InputSanitizer.sanitize(item.adminReply))
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS, jsonStr)
    }

    fun loadAppUpdateConfig(): AppUpdateConfig {
        val raw = prefs.getString("global_app_update_config_json", null) ?: return AppUpdateConfig()
        return parseAppUpdateConfigJson(raw)
    }

    fun parseAppUpdateConfigJson(raw: String): AppUpdateConfig {
        return try {
            val o = JSONObject(raw)
            val isBroadcast = if (o.has("isUpdateBroadcastActive")) {
                o.optBoolean("isUpdateBroadcastActive", false)
            } else {
                o.optBoolean("isUpdateActive", false)
            }
            AppUpdateConfig(
                latestVersionCode = o.optInt("latestVersionCode", 2),
                latestVersionName = o.optString("latestVersionName", "2.5.0"),
                apkDownloadUrl = o.optString("apkDownloadUrl", "https://github.com/madrasati-smart/releases/latest/download/app-debug.apk"),
                releaseNotes = o.optString("releaseNotes", ""),
                isForceUpdate = o.optBoolean("isForceUpdate", false),
                isUpdateBroadcastActive = isBroadcast,
                updatedAt = o.optString("updatedAt", "2026-10-29")
            )
        } catch (_: Exception) {
            AppUpdateConfig()
        }
    }

    fun saveAppUpdateConfig(config: AppUpdateConfig) {
        val obj = JSONObject().apply {
            put("latestVersionCode", config.latestVersionCode)
            put("latestVersionName", InputSanitizer.sanitize(config.latestVersionName))
            put("apkDownloadUrl", config.apkDownloadUrl.trim())
            put("releaseNotes", InputSanitizer.sanitize(config.releaseNotes))
            put("isForceUpdate", config.isForceUpdate)
            put("isUpdateBroadcastActive", config.isUpdateBroadcastActive)
            put("isUpdateActive", config.isUpdateBroadcastActive)
            put("updatedAt", config.updatedAt)
        }
        val jsonStr = obj.toString()
        prefs.edit().putString("global_app_update_config_json", jsonStr).apply()
        FirebaseCloudSync.pushRootNodeAsync(FirebaseConfigConstants.COL_APP_UPDATE_CONFIG, jsonStr)
    }

    fun loadOccasionThemeConfig(): OccasionThemeConfig {
        val raw = prefs.getString("global_occasion_theme_json", null) ?: return OccasionThemeConfig()
        return parseOccasionThemeConfigJson(raw)
    }

    fun parseOccasionThemeConfigJson(raw: String): OccasionThemeConfig {
        return try {
            val o = JSONObject(raw)
            val tType = try {
                OccasionThemeType.valueOf(o.optString("themeType", OccasionThemeType.OFFICIAL.name))
            } catch (_: Exception) {
                OccasionThemeType.OFFICIAL
            }
            val iconKey = OccasionIconKey.fromKey(o.optString("selectedIconKey", OccasionIconKey.CROWN.name))
            OccasionThemeConfig(
                isActive = o.optBoolean("isActive", false),
                themeType = tType,
                selectedIconKey = iconKey.name,
                customTitle = o.optString("customTitle", ""),
                customBannerText = o.optString("customBannerText", ""),
                customIconBase64 = o.optString("customIconBase64", ""),
                customIconUrl = o.optString("customIconUrl", ""),
                primaryColorHex = o.optString("primaryColorHex", tType.defaultPrimaryHex),
                gradientColorHex = o.optString("gradientColorHex", tType.defaultGradientHex),
                launcherAliasName = o.optString("launcherAliasName", iconKey.launcherAliasName),
                updatedAt = o.optString("updatedAt", "2026-10-29")
            )
        } catch (_: Exception) {
            OccasionThemeConfig()
        }
    }

    fun saveOccasionThemeConfig(config: OccasionThemeConfig) {
        val iconKey = OccasionIconKey.fromKey(config.selectedIconKey)
        val obj = JSONObject().apply {
            put("isActive", config.isActive)
            put("themeType", config.themeType.name)
            put("selectedIconKey", iconKey.name)
            put("customTitle", InputSanitizer.sanitize(config.customTitle))
            put("customBannerText", InputSanitizer.sanitize(config.customBannerText))
            put("customIconBase64", config.customIconBase64)
            put("customIconUrl", config.customIconUrl.trim())
            put("primaryColorHex", config.primaryColorHex.ifBlank { config.themeType.defaultPrimaryHex })
            put("gradientColorHex", config.gradientColorHex.ifBlank { config.themeType.defaultGradientHex })
            put("launcherAliasName", config.launcherAliasName.ifBlank { iconKey.launcherAliasName })
            put("updatedAt", config.updatedAt)
        }
        val jsonStr = obj.toString()
        prefs.edit().putString("global_occasion_theme_json", jsonStr).apply()
        FirebaseCloudSync.pushRootNodeAsync(FirebaseConfigConstants.COL_OCCASION_THEME, jsonStr)
    }

    // =========================================================================
    // BIOMETRIC FINGERPRINT SAVED CREDENTIALS (Encrypted Local Session Storage)
    // =========================================================================

    private val BIOMETRIC_CIPHER_KEY = "MADRASATI_BIOMETRIC_SHIELD_2026_ABU_RASLAN"

    private fun encryptBiometricPayload(plainText: String): String {
        return try {
            val keyBytes = BIOMETRIC_CIPHER_KEY.toByteArray(Charsets.UTF_8)
            val inputBytes = plainText.toByteArray(Charsets.UTF_8)
            val xored = ByteArray(inputBytes.size) { i ->
                (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            android.util.Base64.encodeToString(xored, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    private fun decryptBiometricPayload(cipherBase64: String): String {
        return try {
            val keyBytes = BIOMETRIC_CIPHER_KEY.toByteArray(Charsets.UTF_8)
            val decoded = android.util.Base64.decode(cipherBase64, android.util.Base64.NO_WRAP)
            val plain = ByteArray(decoded.size) { i ->
                (decoded[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            String(plain, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun loadBiometricCredentials(): BiometricSavedCredentials? {
        val rawCipher = prefs.getString("biometric_saved_credentials", null) ?: return null
        if (rawCipher.isBlank()) return null
        return try {
            val jsonStr = if (rawCipher.trim().startsWith("{")) rawCipher else decryptBiometricPayload(rawCipher)
            if (jsonStr.isBlank()) return null
            val o = JSONObject(jsonStr)
            val creds = BiometricSavedCredentials(
                username = o.optString("username", ""),
                password = o.optString("password", ""),
                fullName = o.optString("fullName", ""),
                roleArabic = o.optString("roleArabic", ""),
                schoolId = o.optString("schoolId", ""),
                schoolName = o.optString("schoolName", ""),
                isDeveloper = o.optBoolean("isDeveloper", false),
                enabledAt = o.optString("enabledAt", "")
            )
            if (creds.isValid) creds else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveBiometricCredentials(creds: BiometricSavedCredentials) {
        val obj = JSONObject().apply {
            put("username", creds.username.trim())
            put("password", creds.password)
            put("fullName", InputSanitizer.sanitize(creds.fullName))
            put("roleArabic", InputSanitizer.sanitize(creds.roleArabic))
            put("schoolId", creds.schoolId.trim())
            put("schoolName", InputSanitizer.sanitize(creds.schoolName))
            put("isDeveloper", creds.isDeveloper)
            put("enabledAt", creds.enabledAt)
        }
        val encrypted = encryptBiometricPayload(obj.toString())
        prefs.edit().putString("biometric_saved_credentials", encrypted).apply()
    }

    fun clearBiometricCredentials() {
        prefs.edit().remove("biometric_saved_credentials").apply()
    }

    // =========================================================================
    // CLEAN STATE DEFAULTS (Empty Lists - No pre-existing fake students/data)
    // =========================================================================

    fun defaultStudents(): List<StudentRecord> = emptyList()
    fun defaultTeachers(): List<UserAccount> = emptyList()
    fun defaultAttendance(): List<AttendanceEntry> = emptyList()
    fun defaultSchedules(): List<SchedulePeriod> = emptyList()
    fun defaultHomeworks(): List<HomeworkItem> = emptyList()
    fun defaultExamSchedules(): List<ExamScheduleItem> = emptyList()
    fun defaultGrades(): List<SubjectGrade> = emptyList()
    fun defaultTuitions(): List<TuitionRecord> = emptyList()
    fun defaultReports(): List<ParentReport> = emptyList()
    fun defaultChatMessages(): List<ChatMessage> = emptyList()
    fun defaultEvents(): List<SchoolEvent> = emptyList()
    fun defaultNotifications(): List<AppNotification> = emptyList()
}
