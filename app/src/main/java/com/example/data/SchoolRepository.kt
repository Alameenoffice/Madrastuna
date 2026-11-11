package com.example.data

import android.content.Context
import com.example.model.*
import com.example.util.InputSanitizer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    fun isSchoolSecurityGatePassed(): Boolean {
        return prefs.getBoolean("school_security_gate_passed_v1", false)
    }

    fun getVerifiedGateSchoolCode(): String {
        return prefs.getString("verified_gate_school_code_v1", "")?.trim().orEmpty()
    }

    fun setSchoolSecurityGatePassed(passed: Boolean, schoolCode: String = "") {
        val cleanCode = InputSanitizer.sanitizeSchoolId(schoolCode)
        val editor = prefs.edit().putBoolean("school_security_gate_passed_v1", passed)
        if (cleanCode.isNotBlank()) {
            editor.putString("verified_gate_school_code_v1", cleanCode)
        }
        editor.apply()
    }

    fun loadCloudCurriculumBooksJson(): String? {
        return prefs.getString("global_cloud_curriculum_books_json", null)
    }

    fun saveCloudCurriculumBooksJson(rawJson: String, pushToCloud: Boolean = true) {
        if (rawJson.isBlank()) return
        prefs.edit().putString("global_cloud_curriculum_books_json", rawJson).apply()
        if (pushToCloud) {
            FirebaseCloudSync.pushGlobalCollectionAsync(
                collectionName = FirebaseConfigConstants.COL_CURRICULUM_BOOKS,
                docName = "registry",
                jsonPayload = rawJson
            )
        }
    }

    suspend fun syncCloudCurriculumBooks(): String? {
        val remoteJson = FirebaseCloudSync.fetchGlobalCollectionPayload(
            collectionName = FirebaseConfigConstants.COL_CURRICULUM_BOOKS,
            docName = "registry"
        )
        if (!remoteJson.isNullOrBlank() && remoteJson != "[]") {
            prefs.edit().putString("global_cloud_curriculum_books_json", remoteJson).apply()
            return remoteJson
        }
        return loadCloudCurriculumBooksJson()
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

            val holidaysObj = obj.optJSONObject("holidaysConfig") ?: obj.optJSONObject("holidays")
            val parsedHolidaysConfig = if (holidaysObj != null) {
                parseHolidaysConfigObject(holidaysObj, resolvedSchoolId)
            } else {
                loadHolidaysConfig(resolvedSchoolId)
            }

            val embeddedRatingsArr = obj.optJSONArray("teacherRatings")
            if (embeddedRatingsArr != null && embeddedRatingsArr.length() > 0 && resolvedSchoolId.isNotBlank()) {
                mergeEmbeddedTeacherRatingsIntoLocalCache(embeddedRatingsArr.toString(), resolvedSchoolId)
            }

            SchoolConfig(
                schoolId = resolvedSchoolId,
                isSetupCompleted = obj.optBoolean("isSetupCompleted", false),
                isActive = obj.optBoolean("isActive", true),
                createdAt = obj.optString("createdAt", "2026-10-29"),
                schoolName = obj.optString("schoolName", "مدرستي النموذجية"),
                address = ArabicTextNormalizer.cleanDefaultAddress(obj.optString("address", "")),
                schoolType = SchoolType.valueOf(obj.optString("schoolType", SchoolType.PRIVATE.name)),
                schoolStage = stage,
                ministryAffiliation = obj.optString("ministryAffiliation", "وزارة التربية").ifBlank { "وزارة التربية" },
                schoolGenderType = obj.optString("schoolGenderType", "مدرسة بنين").ifBlank { "مدرسة بنين" },
                registeredClasses = regClasses,
                registeredSections = regSections,
                registeredSubjects = regSubjects,
                subjectConfigs = subjectConfigs,
                logoBase64 = obj.optString("logoBase64", ""),
                academicYear = obj.optString("academicYear", "2026 - 2027"),
                phone = obj.optString("phone", "").let {
                    if (com.example.util.SecurityShieldHelper.isLegacyPlaceholderPhone(it)) "" else it
                },
                subscriptionExpiryDate = obj.optString("subscriptionExpiryDate", "2027-10-29").ifBlank { "2027-10-29" },
                featureSettings = parsedFeatures,
                urgentAlert = parsedUrgentAlert,
                holidaysConfig = parsedHolidaysConfig,
                isCanteenWalletEnabled = obj.optBoolean("isCanteenWalletEnabled", parsedFeatures.canteenWalletEnabled),
                principalFullName = obj.optString("principalFullName", "مدير المدرسة").ifBlank { "مدير المدرسة" },
                isSchoolOfficiallyActivated = obj.optBoolean("isSchoolOfficiallyActivated", false),
                schoolActivationCode = obj.optString("schoolActivationCode", resolvedSchoolId.ifBlank { "1001" }).ifBlank { resolvedSchoolId.ifBlank { "1001" } }
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

    fun getLastShownUrgentAlertId(schoolId: String = getActiveSchoolId()): String {
        val sid = schoolId.ifBlank { getActiveSchoolId() }
        if (sid.isBlank()) return ""
        return prefs.getString("last_shown_urgent_alert_$sid", "") ?: ""
    }

    fun setLastShownUrgentAlertId(signatureKey: String, schoolId: String = getActiveSchoolId()) {
        val sid = schoolId.ifBlank { getActiveSchoolId() }
        if (sid.isBlank()) return
        prefs.edit().putString("last_shown_urgent_alert_$sid", signatureKey).apply()
    }

    fun loadHolidaysConfig(schoolId: String = getActiveSchoolId()): SchoolHolidaysConfig {
        val sid = schoolId.ifBlank { getActiveSchoolId() }
        if (sid.isBlank()) return SchoolHolidaysConfig()
        val raw = prefs.getString(scopedKey(sid, FirebaseConfigConstants.COL_HOLIDAYS), null)
            ?: return SchoolHolidaysConfig(schoolId = sid)
        return parseHolidaysConfigJson(raw, sid)
    }

    fun parseHolidaysConfigJson(raw: String, fallbackSchoolId: String = getActiveSchoolId()): SchoolHolidaysConfig {
        return try {
            val obj = JSONObject(raw)
            parseHolidaysConfigObject(obj, fallbackSchoolId)
        } catch (_: Exception) {
            SchoolHolidaysConfig(schoolId = fallbackSchoolId)
        }
    }

    private fun parseHolidaysConfigObject(obj: JSONObject, fallbackSchoolId: String): SchoolHolidaysConfig {
        val weeklyDays = if (obj.has("weeklyDaysOff")) {
            jsonArrayToList(obj.optJSONArray("weeklyDaysOff"))
        } else {
            listOf("الجمعة", "السبت")
        }
        val officialArr = obj.optJSONArray("officialHolidays")
        val officialList = mutableListOf<OfficialHolidayItem>()
        if (officialArr != null) {
            for (i in 0 until officialArr.length()) {
                val o = officialArr.optJSONObject(i) ?: continue
                val dateIso = o.optString("dateIso", "").trim()
                if (dateIso.isNotBlank()) {
                    officialList.add(
                        OfficialHolidayItem(
                            id = o.optString("id", "hol_${dateIso}"),
                            dateIso = dateIso,
                            title = o.optString("title", "عطلة رسمية").ifBlank { "عطلة رسمية" },
                            createdAt = o.optString("createdAt", ""),
                            schoolId = o.optString("schoolId", fallbackSchoolId).ifBlank { fallbackSchoolId }
                        )
                    )
                }
            }
        }
        return SchoolHolidaysConfig(
            weeklyDaysOff = weeklyDays,
            officialHolidays = officialList.distinctBy { it.dateIso }.sortedByDescending { it.dateIso },
            updatedAt = obj.optLong("updatedAt", 0L),
            schoolId = obj.optString("schoolId", fallbackSchoolId).ifBlank { fallbackSchoolId }
        )
    }

    private fun holidaysConfigToJsonObject(cfg: SchoolHolidaysConfig, schoolId: String): JSONObject {
        val sid = schoolId.ifBlank { cfg.schoolId }
        val officialArr = JSONArray()
        cfg.officialHolidays.distinctBy { it.dateIso }.forEach { item ->
            officialArr.put(JSONObject().apply {
                put("id", item.id.ifBlank { "hol_${item.dateIso}" })
                put("dateIso", InputSanitizer.sanitize(item.dateIso.trim()))
                put("title", InputSanitizer.sanitize(item.title.ifBlank { "عطلة رسمية" }))
                put("createdAt", item.createdAt)
                put("schoolId", sid)
            })
        }
        return JSONObject().apply {
            put("schoolId", sid)
            put("weeklyDaysOff", JSONArray(cfg.weeklyDaysOff))
            put("officialHolidays", officialArr)
            put("updatedAt", if (cfg.updatedAt > 0L) cfg.updatedAt else System.currentTimeMillis())
        }
    }

    fun saveHolidaysConfig(cfg: SchoolHolidaysConfig, schoolId: String = getActiveSchoolId()) {
        val sid = schoolId.ifBlank { cfg.schoolId }.ifBlank { getActiveSchoolId() }
        if (sid.isBlank()) return
        val stamped = cfg.copy(schoolId = sid, updatedAt = System.currentTimeMillis())
        val holObj = holidaysConfigToJsonObject(stamped, sid)
        val holStr = holObj.toString()
        prefs.edit().putString(scopedKey(sid, FirebaseConfigConstants.COL_HOLIDAYS), holStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_HOLIDAYS, holStr)
        val currentCfg = loadSchoolConfig(sid)
        saveSchoolConfig(currentCfg.copy(schoolId = sid, holidaysConfig = stamped))
    }

    fun saveSchoolConfig(config: SchoolConfig) {
        val sid = config.schoolId.ifBlank { getActiveSchoolId() }.ifBlank {
            generateUniqueThreeDigitSchoolCode()
        }
        setActiveSchoolId(sid)

        val featJson = featureSettingsToJsonObject(config.featureSettings)
        val urgentAlertJson = urgentSchoolAlertToJsonObject(config.urgentAlert, sid)
        val holidaysJson = holidaysConfigToJsonObject(config.holidaysConfig, sid)
        val currentRatingsArr = teacherRatingsToJsonArray(loadTeacherRatingsDirectOnly(sid), sid)
        val obj = JSONObject().apply {
            put("schoolId", sid)
            put("isSetupCompleted", config.isSetupCompleted)
            put("isActive", config.isActive)
            put("createdAt", config.createdAt.ifBlank { "2026-10-29" })
            put("schoolName", InputSanitizer.sanitize(config.schoolName))
            put("address", InputSanitizer.sanitize(ArabicTextNormalizer.cleanDefaultAddress(config.address)))
            put("schoolType", config.schoolType.name)
            put("schoolStage", config.schoolStage.name)
            put("ministryAffiliation", config.ministryAffiliation.ifBlank { "وزارة التربية" })
            put("schoolGenderType", config.schoolGenderType.ifBlank { "مدرسة بنين" })
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
            put("holidaysConfig", holidaysJson)
            put("teacherRatings", currentRatingsArr)
            put("isCanteenWalletEnabled", config.isCanteenWalletEnabled)
            put("principalFullName", InputSanitizer.sanitize(config.principalFullName.ifBlank { "مدير المدرسة" }))
            put("isSchoolOfficiallyActivated", config.isSchoolOfficiallyActivated)
            put("schoolActivationCode", InputSanitizer.sanitizeSchoolId(config.schoolActivationCode.ifBlank { sid.ifBlank { "1001" } }))
        }
        val jsonStr = obj.toString()
        val featStr = featJson.toString()
        val urgentStr = urgentAlertJson.toString()
        val holStr = holidaysJson.toString()
        prefs.edit()
            .putString(scopedKey(sid, FirebaseConfigConstants.COL_SETTINGS), jsonStr)
            .putString(scopedKey(sid, FirebaseConfigConstants.COL_FEATURE_SETTINGS), featStr)
            .putString(scopedKey(sid, FirebaseConfigConstants.COL_URGENT_ALERT), urgentStr)
            .putString(scopedKey(sid, FirebaseConfigConstants.COL_HOLIDAYS), holStr)
            .apply()
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_SETTINGS, jsonStr)
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_FEATURE_SETTINGS, featStr)
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_URGENT_ALERT, urgentStr)
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_HOLIDAYS, holStr)

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
            put("studentActivities", f.studentActivitiesEnabled)
            put("canteenWalletEnabled", f.canteenWalletEnabled)
        }

        return JSONObject().apply {
            // Canonical 12+ keys
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
            put("studentActivitiesEnabled", f.studentActivitiesEnabled)
            put("canteenWalletEnabled", f.canteenWalletEnabled)
            put("teacherEvaluationEnabled", f.teacherEvaluationEnabled)
            put("showTopTeacherOnHome", f.showTopTeacherOnHome)
            put("autoAbsenceEnabled", f.autoAbsenceEnabled)
            put("autoAbsenceDelayMinutes", f.autoAbsenceDelayMinutes)
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
        val studentActivities = boolKey("studentActivities", "studentActivitiesEnabled")
        val canteenWallet = boolKey("canteenWalletEnabled", "isCanteenWalletEnabled")
        val teacherEvaluation = boolKey("teacherEvaluationEnabled", default = true)
        val showTopTeacherHome = boolKey("showTopTeacherOnHome", default = true)
        val autoAbsence = boolKey("autoAbsenceEnabled", default = true)
        val autoAbsenceDelay = when {
            o.has("autoAbsenceDelayMinutes") -> o.optInt("autoAbsenceDelayMinutes", 120)
            rawObj.has("autoAbsenceDelayMinutes") -> rawObj.optInt("autoAbsenceDelayMinutes", 120)
            else -> 120
        }.coerceIn(15, 720)

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
            lostAndFoundEnabled = lostAndSug,
            studentActivitiesEnabled = studentActivities,
            canteenWalletEnabled = canteenWallet,
            teacherEvaluationEnabled = teacherEvaluation,
            showTopTeacherOnHome = showTopTeacherHome,
            autoAbsenceEnabled = autoAbsence,
            autoAbsenceDelayMinutes = autoAbsenceDelay
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
                val role = runCatching {
                    UserRole.valueOf(o.optString("role", UserRole.STUDENT.name))
                }.getOrDefault(UserRole.STUDENT)

                val rawAssigned = o.optString("assignedClasses").trim()
                val fallbackClassesArr = o.optJSONArray("classes") ?: o.optJSONArray("grades")
                val fallbackSectionsArr = o.optJSONArray("sections")
                val unifiedAssignedClasses = if (rawAssigned.isNotBlank()) {
                    rawAssigned
                } else if (fallbackClassesArr != null && fallbackClassesArr.length() > 0) {
                    val clsList = jsonArrayToList(fallbackClassesArr).map { ArabicTextNormalizer.cleanPureClassName(it) }.filter { it.isNotBlank() }
                    val secList = jsonArrayToList(fallbackSectionsArr).map { ArabicTextNormalizer.cleanPureSectionName(it) }.filter { it.isNotBlank() }
                    val secsJoined = if (secList.isNotEmpty()) secList.joinToString("، ") else "أ"
                    clsList.joinToString(" | ") { c -> "$c ($secsJoined)" }
                } else if (role == UserRole.TEACHER) {
                    val gc = ArabicTextNormalizer.cleanPureClassName(o.optString("gradeClass"))
                    val sn = ArabicTextNormalizer.cleanPureSectionName(o.optString("sectionName", "أ")).ifBlank { "أ" }
                    if (gc.isNotBlank()) "$gc ($sn)" else ""
                } else {
                    ""
                }

                val (pureGradeClass, pureSectionName) = ArabicTextNormalizer.normalizeAndExtractClass(
                    o.optString("gradeClass", "الأول المتوسط"),
                    o.optString("sectionName", "أ")
                )

                val accTeacherRatings = o.optJSONArray("teacherRatings")
                if (accTeacherRatings != null && accTeacherRatings.length() > 0 && itemSchoolId.isNotBlank()) {
                    mergeEmbeddedTeacherRatingsIntoLocalCache(accTeacherRatings.toString(), itemSchoolId)
                }

                list.add(
                    UserAccount(
                        id = o.optString("id"),
                        username = o.optString("username"),
                        password = o.optString("password"),
                        fullName = o.optString("fullName"),
                        role = role,
                        isActive = o.optBoolean("isActive", true),
                        phone = o.optString("phone"),
                        specialty = o.optString("specialty"),
                        assignedClasses = unifiedAssignedClasses,
                        linkedStudentId = o.optString("linkedStudentId"),
                        gradeClass = pureGradeClass.ifBlank { "الأول المتوسط" },
                        sectionName = pureSectionName.ifBlank { "أ" },
                        address = ArabicTextNormalizer.cleanDefaultAddress(o.optString("address", "")),
                        photoBase64 = o.optString("photoBase64", ""),
                        schoolId = itemSchoolId,
                        canModerateActivities = o.optBoolean("canModerateActivities", false),
                        canManageCanteen = o.optBoolean("canManageCanteen", false),
                        gender = o.optString("gender", "").trim().let { if (it == "غير محدد") "" else it },
                        ageYears = o.optInt("ageYears", o.optInt("age", 0)).coerceAtLeast(0)
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
        val existingRatings = loadTeacherRatingsDirectOnly(schoolId)
        val arr = JSONArray()
        accounts.forEach { acc ->
            val (pureGradeClass, pureSectionName) = ArabicTextNormalizer.normalizeAndExtractClass(
                acc.gradeClass,
                acc.sectionName
            )
            val parsedEntries = acc.parsedClassAndSectionEntries
            val pureClassesList = acc.teacherAssignedClassesOnly
            val pureSectionsList = acc.teacherAssignedSectionsOnly
            val normalizedAssignedString = if (acc.role == UserRole.TEACHER && parsedEntries.isNotEmpty()) {
                parsedEntries.joinToString(" | ") { (cls, sec) ->
                    if (sec.isBlank()) cls else "$cls - $sec"
                }
            } else {
                acc.assignedClasses
            }
            arr.put(JSONObject().apply {
                put("id", acc.id)
                put("username", InputSanitizer.sanitizeUsername(acc.username))
                put("password", InputSanitizer.sanitize(acc.password))
                put("fullName", InputSanitizer.sanitize(acc.fullName))
                put("role", acc.role.name)
                put("isActive", acc.isActive)
                put("phone", InputSanitizer.sanitize(acc.phone))
                put("specialty", InputSanitizer.sanitize(acc.specialty))
                put("assignedClasses", InputSanitizer.sanitize(normalizedAssignedString))
                put("classes", JSONArray(pureClassesList))
                put("grades", JSONArray(pureClassesList))
                put("sections", JSONArray(pureSectionsList))
                put("linkedStudentId", acc.linkedStudentId)
                put("gradeClass", InputSanitizer.sanitize(pureGradeClass))
                put("sectionName", InputSanitizer.sanitize(pureSectionName))
                put("address", InputSanitizer.sanitize(ArabicTextNormalizer.cleanDefaultAddress(acc.address)))
                put("photoBase64", acc.photoBase64)
                put("schoolId", schoolId)
                put("canModerateActivities", acc.canModerateActivities)
                put("canManageCanteen", acc.canManageCanteen)
                put("gender", if (acc.isGenderSpecified) InputSanitizer.sanitize(acc.normalizedGenderLabel) else "")
                put("ageYears", acc.effectiveAgeYears)
                put("age", acc.effectiveAgeYears)
                if (acc.role == UserRole.TEACHER) {
                    val teacherSpecificRatings = existingRatings.filter {
                        it.teacherId == acc.id ||
                            (it.teacherName.isNotBlank() && it.teacherName.trim() == acc.fullName.trim())
                    }
                    put("teacherRatings", teacherRatingsToJsonArray(teacherSpecificRatings, schoolId))
                }
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
                val (pureGradeClass, pureSectionName) = ArabicTextNormalizer.normalizeAndExtractClass(
                    o.optString("gradeClass"),
                    o.optString("sectionName")
                )
                val dailyAttObj = o.optJSONObject("dailyAttendance")
                val dailyAttMap = linkedMapOf<String, String>()
                if (dailyAttObj != null) {
                    val keys = dailyAttObj.keys()
                    while (keys.hasNext()) {
                        val rawDateKey = keys.next()
                        val cleanDateKey = ArabicTextNormalizer.normalizeDateIso(rawDateKey.trim())
                        val rawStatusVal = dailyAttObj.optString(rawDateKey, "").trim().uppercase()
                        if (cleanDateKey.isNotEmpty() && rawStatusVal.isNotEmpty()) {
                            dailyAttMap[cleanDateKey] = rawStatusVal
                        }
                    }
                }
                val parsedTodayDate = ArabicTextNormalizer.normalizeDateIso(o.optString("todayStatusDate", ""))
                val parsedTodayStatus = o.optString("todayStatus", "").trim().uppercase()
                if (parsedTodayDate.isNotEmpty() && parsedTodayStatus.isNotEmpty() && !dailyAttMap.containsKey(parsedTodayDate)) {
                    dailyAttMap[parsedTodayDate] = parsedTodayStatus
                }
                val latestDateKey = dailyAttMap.keys.maxOrNull().orEmpty()
                val effectiveTodayDate = parsedTodayDate.ifEmpty { latestDateKey }
                val effectiveTodayStatus = when {
                    effectiveTodayDate.isNotEmpty() && dailyAttMap.containsKey(effectiveTodayDate) ->
                        dailyAttMap[effectiveTodayDate].orEmpty()
                    parsedTodayStatus.isNotEmpty() -> parsedTodayStatus
                    latestDateKey.isNotEmpty() -> dailyAttMap[latestDateKey].orEmpty()
                    else -> ""
                }
                val computedAbsenceDays = if (dailyAttMap.isNotEmpty()) {
                    dailyAttMap.values.count { it.equals("ABSENT", ignoreCase = true) }
                } else {
                    o.optInt("absenceDays", 0)
                }
                val computedExcusedDays = if (dailyAttMap.isNotEmpty()) {
                    dailyAttMap.values.count { it.equals("EXCUSED", ignoreCase = true) }
                } else {
                    o.optInt("excusedDays", 0)
                }
                val computedLateDays = if (dailyAttMap.isNotEmpty()) {
                    dailyAttMap.values.count { it.equals("LATE", ignoreCase = true) }
                } else {
                    o.optInt("lateDays", 0)
                }
                val dailySpentObj = o.optJSONObject("dailySpentMap")
                val parsedDailySpentMap = linkedMapOf<String, Int>()
                if (dailySpentObj != null) {
                    val dKeys = dailySpentObj.keys()
                    while (dKeys.hasNext()) {
                        val rawDKey = dKeys.next()
                        val cleanDKey = ArabicTextNormalizer.normalizeDateIso(rawDKey.trim())
                        val spentVal = dailySpentObj.optInt(rawDKey, 0).coerceAtLeast(0)
                        if (cleanDKey.isNotEmpty() && spentVal > 0) {
                            parsedDailySpentMap[cleanDKey] = spentVal
                        }
                    }
                }
                val txArr = o.optJSONArray("walletTransactions")
                val parsedWalletTxs = mutableListOf<CanteenWalletTransaction>()
                if (txArr != null) {
                    for (tIdx in 0 until txArr.length()) {
                        val tObj = txArr.optJSONObject(tIdx) ?: continue
                        val txType = runCatching {
                            CanteenTransactionType.valueOf(tObj.optString("type", CanteenTransactionType.TOP_UP.name))
                        }.getOrDefault(CanteenTransactionType.TOP_UP)
                        parsedWalletTxs.add(
                            CanteenWalletTransaction(
                                id = tObj.optString("id", "wtx_$tIdx"),
                                studentId = tObj.optString("studentId", o.optString("id")),
                                studentName = tObj.optString("studentName", o.optString("studentName")),
                                type = txType,
                                amount = tObj.optInt("amount", 0),
                                balanceAfter = tObj.optInt("balanceAfter", 0),
                                date = ArabicTextNormalizer.normalizeDateIso(tObj.optString("date", "")),
                                timestamp = tObj.optString("timestamp", ""),
                                actorName = tObj.optString("actorName", "كاشير الحانوت"),
                                note = tObj.optString("note", "")
                            )
                        )
                    }
                }
                val gateLogObj = o.optJSONObject("dailyGateLog")
                val parsedDailyGateLog = linkedMapOf<String, DailyGateLogEntry>()
                if (gateLogObj != null) {
                    val gKeys = gateLogObj.keys()
                    while (gKeys.hasNext()) {
                        val rawGKey = gKeys.next()
                        val cleanGKey = ArabicTextNormalizer.normalizeDateIso(rawGKey.trim())
                        val entryObj = gateLogObj.optJSONObject(rawGKey)
                        if (cleanGKey.isNotEmpty() && entryObj != null) {
                            val cIn = entryObj.optString("checkInTime", "").trim()
                            val cOut = entryObj.optString("checkOutTime", "").trim()
                            val gStatus = entryObj.optString(
                                "gateStatus",
                                if (cOut.isNotEmpty()) "departed" else "inside"
                            ).trim().lowercase()
                            val dNote = entryObj.optString("departureNote", "انصراف رسمي / مغادرة البوابة").trim()
                            if (cIn.isNotEmpty() || cOut.isNotEmpty()) {
                                parsedDailyGateLog[cleanGKey] = DailyGateLogEntry(
                                    checkInTime = cIn,
                                    checkOutTime = cOut,
                                    gateStatus = if (gStatus == "departed" || cOut.isNotEmpty()) "departed" else "inside",
                                    departureNote = dNote
                                )
                            }
                        }
                    }
                }
                val enrollmentStatus = StudentEnrollmentStatus.fromKeyOrName(
                    o.optString("enrollmentStatus", StudentEnrollmentStatus.ACTIVE.name)
                )
                val historyArr = o.optJSONArray("enrollmentHistory")
                val parsedEnrollmentHistory = mutableListOf<com.example.model.EnrollmentHistoryEntry>()
                if (historyArr != null) {
                    for (hIdx in 0 until historyArr.length()) {
                        val hObj = historyArr.optJSONObject(hIdx) ?: continue
                        parsedEnrollmentHistory.add(
                            com.example.model.EnrollmentHistoryEntry(
                                id = hObj.optString("id", "eh_$hIdx"),
                                status = StudentEnrollmentStatus.fromKeyOrName(
                                    hObj.optString("status", StudentEnrollmentStatus.ACTIVE.name)
                                ),
                                decisionDate = ArabicTextNormalizer.normalizeDateIso(hObj.optString("decisionDate", "")),
                                suspensionStartDate = ArabicTextNormalizer.normalizeDateIso(hObj.optString("suspensionStartDate", "")),
                                suspensionEndDate = ArabicTextNormalizer.normalizeDateIso(hObj.optString("suspensionEndDate", "")),
                                reason = hObj.optString("reason", ""),
                                orderNumber = hObj.optString("orderNumber", ""),
                                transferSchoolName = hObj.optString("transferSchoolName", ""),
                                actorName = hObj.optString("actorName", "المدير")
                            )
                        )
                    }
                }
                val archivedArr = o.optJSONArray("archivedYearlySummaries")
                val parsedArchivedSummaries = mutableListOf<ArchivedYearlySummary>()
                if (archivedArr != null) {
                    for (aIdx in 0 until archivedArr.length()) {
                        val aObj = archivedArr.optJSONObject(aIdx) ?: continue
                        parsedArchivedSummaries.add(
                            ArchivedYearlySummary(
                                academicYear = aObj.optString("academicYear", ""),
                                gradeClass = aObj.optString("gradeClass", ""),
                                sectionName = aObj.optString("sectionName", "أ"),
                                finalAveragePercentage = aObj.optInt("finalAveragePercentage", 0),
                                ratingLabel = aObj.optString("ratingLabel", ""),
                                resultStatus = aObj.optString("resultStatus", ""),
                                promotedToClass = aObj.optString("promotedToClass", ""),
                                archivedAt = aObj.optString("archivedAt", "")
                            )
                        )
                    }
                }
                list.add(
                    StudentRecord(
                        id = o.optString("id"),
                        studentName = o.optString("studentName"),
                        gradeClass = pureGradeClass,
                        sectionName = pureSectionName,
                        parentName = o.optString("parentName"),
                        parentPhone = o.optString("parentPhone"),
                        address = ArabicTextNormalizer.cleanDefaultAddress(o.optString("address", "")),
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
                        schoolId = itemSchoolId,
                        dailyAttendance = dailyAttMap,
                        todayStatus = effectiveTodayStatus,
                        todayStatusDate = effectiveTodayDate,
                        absenceDays = computedAbsenceDays,
                        excusedDays = computedExcusedDays,
                        lateDays = computedLateDays,
                        walletBalance = o.optInt("walletBalance", 0).coerceAtLeast(0),
                        dailySpendLimit = o.optInt("dailySpendLimit", 0).coerceAtLeast(0),
                        dailySpentMap = parsedDailySpentMap,
                        walletTransactions = parsedWalletTxs,
                        dailyGateLog = parsedDailyGateLog,
                        isWalletSuspended = o.optBoolean("isWalletSuspended", false),
                        enrollmentStatus = enrollmentStatus,
                        suspensionStartDate = ArabicTextNormalizer.normalizeDateIso(o.optString("suspensionStartDate", "")),
                        suspensionEndDate = ArabicTextNormalizer.normalizeDateIso(o.optString("suspensionEndDate", "")),
                        enrollmentStatusReason = o.optString("enrollmentStatusReason", o.optString("statusReason", "")),
                        transferredToSchoolName = o.optString("transferredToSchoolName", o.optString("transferSchoolName", "")),
                        enrollmentDecisionDate = ArabicTextNormalizer.normalizeDateIso(o.optString("enrollmentDecisionDate", "")),
                        enrollmentOrderNumber = o.optString("enrollmentOrderNumber", ""),
                        enrollmentHistory = parsedEnrollmentHistory,
                        archivedYearlySummaries = parsedArchivedSummaries,
                        gender = o.optString("gender", "").trim().let { if (it == "غير محدد") "" else it },
                        ageYears = o.optInt("ageYears", o.optInt("age", 0)).coerceAtLeast(0)
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
        students.filter { it.schoolId.isBlank() || it.schoolId == schoolId }.forEach { s ->
            val (pureGradeClass, pureSectionName) = ArabicTextNormalizer.normalizeAndExtractClass(
                s.gradeClass,
                s.sectionName
            )
            val cleanLikedBy = s.likedByUserIds.filter { it.isNotBlank() }.distinct()
            val finalHeartCount = if (cleanLikedBy.isNotEmpty() || s.heartCount == 0) cleanLikedBy.size else s.heartCount
            val normalizedAttMap = linkedMapOf<String, String>()
            s.dailyAttendance.forEach { (dKey, stVal) ->
                val cleanDKey = ArabicTextNormalizer.normalizeDateIso(dKey.trim())
                if (cleanDKey.isNotEmpty() && stVal.isNotBlank()) {
                    normalizedAttMap[cleanDKey] = stVal.trim().uppercase()
                }
            }
            val cleanTodayDate = ArabicTextNormalizer.normalizeDateIso(s.todayStatusDate)
            val cleanTodayStatus = s.todayStatus.trim().uppercase()
            if (cleanTodayDate.isNotEmpty() && cleanTodayStatus.isNotEmpty()) {
                normalizedAttMap[cleanTodayDate] = cleanTodayStatus
            }
            val dailyAttJson = JSONObject()
            normalizedAttMap.forEach { (dKey, stVal) ->
                dailyAttJson.put(dKey, stVal)
            }
            val latestDateKey = normalizedAttMap.keys.maxOrNull().orEmpty()
            val effectiveTodayDate = cleanTodayDate.ifEmpty { latestDateKey }
            val effectiveTodayStatus = when {
                effectiveTodayDate.isNotEmpty() && normalizedAttMap.containsKey(effectiveTodayDate) ->
                    normalizedAttMap[effectiveTodayDate].orEmpty()
                cleanTodayStatus.isNotEmpty() -> cleanTodayStatus
                latestDateKey.isNotEmpty() -> normalizedAttMap[latestDateKey].orEmpty()
                else -> ""
            }
            val calcAbsenceDays = if (normalizedAttMap.isNotEmpty()) {
                normalizedAttMap.values.count { it.equals("ABSENT", ignoreCase = true) }
            } else s.absenceDays
            val calcExcusedDays = if (normalizedAttMap.isNotEmpty()) {
                normalizedAttMap.values.count { it.equals("EXCUSED", ignoreCase = true) }
            } else s.excusedDays
            val calcLateDays = if (normalizedAttMap.isNotEmpty()) {
                normalizedAttMap.values.count { it.equals("LATE", ignoreCase = true) }
            } else s.lateDays
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("studentName", InputSanitizer.sanitize(s.studentName))
                put("gradeClass", InputSanitizer.sanitize(pureGradeClass))
                put("sectionName", InputSanitizer.sanitize(pureSectionName))
                put("parentName", InputSanitizer.sanitize(s.parentName))
                put("parentPhone", InputSanitizer.sanitize(s.parentPhone))
                put("address", InputSanitizer.sanitize(ArabicTextNormalizer.cleanDefaultAddress(s.address)))
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
                put("dailyAttendance", dailyAttJson)
                put("todayStatus", effectiveTodayStatus)
                put("todayStatusDate", effectiveTodayDate)
                put("absenceDays", calcAbsenceDays)
                put("excusedDays", calcExcusedDays)
                put("lateDays", calcLateDays)
                put("walletBalance", s.walletBalance.coerceAtLeast(0))
                put("dailySpendLimit", s.dailySpendLimit.coerceAtLeast(0))
                put("walletBarcode", s.walletBarcode)
                val dailySpentJson = JSONObject()
                s.dailySpentMap.forEach { (dKey, spentAmt) ->
                    val cleanDKey = ArabicTextNormalizer.normalizeDateIso(dKey.trim())
                    if (cleanDKey.isNotEmpty() && spentAmt > 0) {
                        dailySpentJson.put(cleanDKey, spentAmt)
                    }
                }
                put("dailySpentMap", dailySpentJson)
                val txJsonArr = JSONArray()
                s.walletTransactions.take(60).forEach { tx ->
                    txJsonArr.put(JSONObject().apply {
                        put("id", tx.id)
                        put("studentId", tx.studentId)
                        put("studentName", InputSanitizer.sanitize(tx.studentName))
                        put("type", tx.type.name)
                        put("amount", tx.amount)
                        put("balanceAfter", tx.balanceAfter)
                        put("date", ArabicTextNormalizer.normalizeDateIso(tx.date))
                        put("timestamp", InputSanitizer.sanitize(tx.timestamp))
                        put("actorName", InputSanitizer.sanitize(tx.actorName))
                        put("note", InputSanitizer.sanitize(tx.note))
                    })
                }
                put("walletTransactions", txJsonArr)
                val dailyGateJson = JSONObject()
                s.dailyGateLog.forEach { (dKey, gateEntry) ->
                    val cleanDKey = ArabicTextNormalizer.normalizeDateIso(dKey.trim())
                    if (cleanDKey.isNotEmpty() && (gateEntry.checkInTime.isNotBlank() || gateEntry.checkOutTime.isNotBlank())) {
                        dailyGateJson.put(
                            cleanDKey,
                            JSONObject().apply {
                                put("checkInTime", InputSanitizer.sanitize(gateEntry.checkInTime))
                                put("checkOutTime", InputSanitizer.sanitize(gateEntry.checkOutTime))
                                put("gateStatus", if (gateEntry.isDeparted) "departed" else "inside")
                                put("departureNote", InputSanitizer.sanitize(gateEntry.departureNote))
                            }
                        )
                    }
                }
                put("dailyGateLog", dailyGateJson)
                put("isWalletSuspended", s.isWalletSuspended)
                put("enrollmentStatus", s.enrollmentStatus.name)
                put("suspensionStartDate", ArabicTextNormalizer.normalizeDateIso(s.suspensionStartDate))
                put("suspensionEndDate", ArabicTextNormalizer.normalizeDateIso(s.suspensionEndDate))
                put("enrollmentStatusReason", InputSanitizer.sanitize(s.enrollmentStatusReason))
                put("statusReason", InputSanitizer.sanitize(s.enrollmentStatusReason))
                put("transferredToSchoolName", InputSanitizer.sanitize(s.transferredToSchoolName))
                put("transferSchoolName", InputSanitizer.sanitize(s.transferredToSchoolName))
                put("enrollmentDecisionDate", ArabicTextNormalizer.normalizeDateIso(s.enrollmentDecisionDate))
                put("enrollmentOrderNumber", InputSanitizer.sanitize(s.enrollmentOrderNumber))
                val historyJsonArr = JSONArray()
                s.enrollmentHistory.take(40).forEach { eh ->
                    historyJsonArr.put(JSONObject().apply {
                        put("id", eh.id)
                        put("status", eh.status.name)
                        put("decisionDate", ArabicTextNormalizer.normalizeDateIso(eh.decisionDate))
                        put("suspensionStartDate", ArabicTextNormalizer.normalizeDateIso(eh.suspensionStartDate))
                        put("suspensionEndDate", ArabicTextNormalizer.normalizeDateIso(eh.suspensionEndDate))
                        put("reason", InputSanitizer.sanitize(eh.reason))
                        put("orderNumber", InputSanitizer.sanitize(eh.orderNumber))
                        put("transferSchoolName", InputSanitizer.sanitize(eh.transferSchoolName))
                        put("actorName", InputSanitizer.sanitize(eh.actorName))
                    })
                }
                put("enrollmentHistory", historyJsonArr)
                val archivedJsonArr = JSONArray()
                s.archivedYearlySummaries.forEach { arch ->
                    archivedJsonArr.put(JSONObject().apply {
                        put("academicYear", InputSanitizer.sanitize(arch.academicYear))
                        put("gradeClass", InputSanitizer.sanitize(arch.gradeClass))
                        put("sectionName", InputSanitizer.sanitize(arch.sectionName))
                        put("finalAveragePercentage", arch.finalAveragePercentage)
                        put("ratingLabel", InputSanitizer.sanitize(arch.ratingLabel))
                        put("resultStatus", InputSanitizer.sanitize(arch.resultStatus))
                        put("promotedToClass", InputSanitizer.sanitize(arch.promotedToClass))
                        put("archivedAt", InputSanitizer.sanitize(arch.archivedAt))
                    })
                }
                put("archivedYearlySummaries", archivedJsonArr)
                put("gender", if (s.isGenderSpecified) InputSanitizer.sanitize(s.normalizedGenderLabel) else "")
                put("ageYears", s.effectiveAgeYears)
                put("age", s.effectiveAgeYears)
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
            val dedupMap = linkedMapOf<String, AttendanceEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val stuId = o.optString("studentId").trim()
                val cleanDate = ArabicTextNormalizer.normalizeDateIso(o.optString("date").trim())
                if (stuId.isEmpty() || cleanDate.isEmpty()) continue
                val deterministicId = "attendance_${cleanDate}_${stuId}"
                val entry = AttendanceEntry(
                    id = deterministicId,
                    studentId = stuId,
                    studentName = o.optString("studentName"),
                    gradeClass = o.optString("gradeClass"),
                    sectionName = o.optString("sectionName"),
                    date = cleanDate,
                    status = runCatching {
                        val rawStatus = o.optString("status", AttendanceStatus.PRESENT.name).trim().uppercase()
                        AttendanceStatus.valueOf(rawStatus)
                    }.getOrDefault(AttendanceStatus.PRESENT),
                    note = o.optString("note"),
                    schoolId = itemSid
                )
                // Keep first occurrence (latest prepended record) per (studentId, date)
                val key = "${stuId}__${cleanDate}"
                if (!dedupMap.containsKey(key)) {
                    dedupMap[key] = entry
                }
            }
            dedupMap.values.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAttendance(entries: List<AttendanceEntry>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val dedupMap = linkedMapOf<String, AttendanceEntry>()
        entries.filter { it.schoolId.isBlank() || it.schoolId == schoolId }.forEach { e ->
            val stuId = e.studentId.trim()
            val cleanDate = ArabicTextNormalizer.normalizeDateIso(e.date.trim())
            if (stuId.isNotEmpty() && cleanDate.isNotEmpty()) {
                val key = "${stuId}__${cleanDate}"
                if (!dedupMap.containsKey(key)) {
                    dedupMap[key] = e.copy(
                        id = "attendance_${cleanDate}_${stuId}",
                        studentId = stuId,
                        date = cleanDate,
                        schoolId = schoolId
                    )
                }
            }
        }
        val arr = JSONArray()
        dedupMap.values.forEach { e ->
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
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
                        schoolId = itemSid
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
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
                        attachmentImageBase64 = o.optString("attachmentImageBase64", ""),
                        schoolId = itemSid
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
                put("attachmentImageBase64", h.attachmentImageBase64)
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                list.add(
                    ExamScheduleItem(
                        id = o.optString("id"),
                        gradeClass = o.optString("gradeClass"),
                        subjectName = o.optString("subjectName"),
                        examType = o.optString("examType"),
                        examDate = ArabicTextNormalizer.normalizeDateIso(o.optString("examDate")),
                        dayName = o.optString("dayName"),
                        timeSlot = o.optString("timeSlot"),
                        topicsIncluded = o.optString("topicsIncluded"),
                        hallNumber = o.optString("hallNumber"),
                        schoolId = itemSid
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
                put("examDate", ArabicTextNormalizer.normalizeDateIso(InputSanitizer.sanitize(e.examDate)))
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

    private fun jsonArrayToIntList(arr: JSONArray?): List<Int> {
        if (arr == null) return emptyList()
        val list = mutableListOf<Int>()
        for (i in 0 until arr.length()) {
            list.add(arr.optInt(i, 0))
        }
        return list
    }

    fun parseGradesJson(raw: String, schoolId: String = getActiveSchoolId()): List<SubjectGrade> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SubjectGrade>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val gCls = o.optString("gradeClass")
                val defaultScale = GradingRules.defaultMaxScoreForClass(gCls)
                val scale = o.optInt("maxScoreScale", defaultScale).let { if (it == 10) 10 else 100 }
                val dScore = o.optInt("dailyScore", 0)
                val m1Score = o.optInt("month1Score", 0)
                val m2Score = o.optInt("month2Score", 0)
                val midScore = o.optInt("midYearScore", 0)
                val finScore = o.optInt("finalExamScore", 0)
                val extraDaily = jsonArrayToIntList(o.optJSONArray("extraDailyScores"))
                val extraMonthly = jsonArrayToIntList(o.optJSONArray("extraMonthlyScores"))
                val rawApprovedFlag = o.optBoolean("isApprovedByPrincipal", false)
                val approvedPeriodsArr = o.optJSONArray("approvedPeriods")
                val parsedApprovedPeriods = if (approvedPeriodsArr != null) {
                    jsonArrayToList(approvedPeriodsArr).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                } else if (rawApprovedFlag) {
                    buildSet {
                        if (dScore > 0) add(GradeAssessmentPeriod.DAILY.key)
                        if (m1Score > 0) add(GradeAssessmentPeriod.MONTH_1.key)
                        if (m2Score > 0) add(GradeAssessmentPeriod.MONTH_2.key)
                        if (midScore > 0) add(GradeAssessmentPeriod.MID_YEAR.key)
                        if ((extraMonthly.getOrNull(0) ?: 0) > 0) add(GradeAssessmentPeriod.MONTH_3.key)
                        if ((extraMonthly.getOrNull(1) ?: 0) > 0) add(GradeAssessmentPeriod.MONTH_4.key)
                        if (finScore > 0) add(GradeAssessmentPeriod.FINAL_EXAM.key)
                    }
                } else {
                    emptySet()
                }
                list.add(
                    SubjectGrade(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = gCls,
                        subjectName = o.optString("subjectName"),
                        dailyScore = dScore,
                        month1Score = m1Score,
                        month2Score = m2Score,
                        midYearScore = midScore,
                        finalExamScore = finScore,
                        maxScoreScale = scale,
                        extraDailyScores = extraDaily,
                        extraMonthlyScores = extraMonthly,
                        isApprovedByPrincipal = parsedApprovedPeriods.isNotEmpty() || rawApprovedFlag,
                        approvedPeriods = parsedApprovedPeriods,
                        schoolId = itemSid
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
                put("extraDailyScores", JSONArray(g.extraDailyScores))
                put("extraMonthlyScores", JSONArray(g.extraMonthlyScores))
                put("isApprovedByPrincipal", g.approvedPeriods.isNotEmpty() || g.isApprovedByPrincipal)
                put("approvedPeriods", JSONArray(g.approvedPeriods.toList()))
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
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
                        schoolId = itemSid
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
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
                        isReadByParent = o.optBoolean("isReadByParent", false),
                        readAtTimestamp = o.optString("readAtTimestamp", ""),
                        schoolId = itemSid
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
                put("isReadByParent", r.isReadByParent)
                put("readAtTimestamp", InputSanitizer.sanitize(r.readAtTimestamp))
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
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
                        schoolId = itemSid
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val likedList = jsonArrayToList(o.optJSONArray("likedBy") ?: o.optJSONArray("likedByUserIds")).distinct()
                val heartCount = if (likedList.isNotEmpty()) likedList.size else o.optInt("heartCount", 0)
                list.add(
                    SchoolEvent(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        category = o.optString("category"),
                        description = o.optString("description"),
                        date = o.optString("date"),
                        location = ArabicTextNormalizer.cleanDefaultAddress(o.optString("location", "")),
                        imageBase64 = o.optString("imageBase64", ""),
                        authorName = o.optString("authorName", "إدارة المدرسة"),
                        heartCount = heartCount,
                        likedByUserIds = likedList,
                        schoolId = itemSid,
                        schoolName = o.optString("schoolName", "")
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
        events.filter { it.schoolId.isBlank() || it.schoolId == schoolId }.forEach { ev ->
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
                put("schoolName", InputSanitizer.sanitize(ev.schoolName))
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
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
                        targetSection = o.optString("targetSection", ""),
                        targetTab = o.optInt("targetTab", 0),
                        targetItemId = o.optString("targetItemId", ""),
                        createdAt = o.optString("createdAt"),
                        isRead = o.optBoolean("isRead", false),
                        schoolId = itemSid,
                        isUrgent = o.optBoolean("isUrgent", false),
                        priority = o.optString("priority", "normal")
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
                put("targetSection", n.targetSection)
                put("targetTab", n.targetTab)
                put("targetItemId", n.targetItemId)
                put("createdAt", InputSanitizer.sanitize(n.createdAt))
                put("isRead", n.isRead)
                put("schoolId", schoolId)
                put("isUrgent", n.isUrgent)
                put("priority", n.priority)
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
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
                        schoolId = itemSid
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
        items.filter { it.schoolId.isBlank() || it.schoolId == schoolId }.forEach { item ->
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
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
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
                        schoolId = itemSid
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
                            address = ArabicTextNormalizer.cleanDefaultAddress(o.optString("address", "")),
                            schoolStage = o.optString("schoolStage", "ثانوية"),
                            schoolType = o.optString("schoolType", "مدرسة أهلية"),
                            ministryAffiliation = o.optString("ministryAffiliation", "وزارة التربية").ifBlank { "وزارة التربية" },
                            schoolGenderType = o.optString("schoolGenderType", "مدرسة بنين").ifBlank { "مدرسة بنين" },
                            createdAt = o.optString("createdAt", "2026-10-29"),
                            isActive = o.optBoolean("isActive", true),
                            adminUsername = o.optString("adminUsername", ""),
                            subscriptionExpiryDate = o.optString("subscriptionExpiryDate", "2027-10-29").ifBlank { "2027-10-29" },
                            isSchoolOfficiallyActivated = o.optBoolean("isSchoolOfficiallyActivated", false),
                            schoolActivationCode = o.optString("schoolActivationCode", sid.ifBlank { "1001" }).ifBlank { sid.ifBlank { "1001" } }
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
                put("ministryAffiliation", s.ministryAffiliation.ifBlank { "وزارة التربية" })
                put("schoolGenderType", s.schoolGenderType.ifBlank { "مدرسة بنين" })
                put("createdAt", s.createdAt)
                put("isActive", s.isActive)
                put("adminUsername", s.adminUsername)
                put("subscriptionExpiryDate", s.subscriptionExpiryDate.ifBlank { "2027-10-29" })
                put("isSchoolOfficiallyActivated", s.isSchoolOfficiallyActivated)
                put("schoolActivationCode", s.schoolActivationCode.ifBlank { s.schoolId.ifBlank { "1001" } })
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
            ministryAffiliation = config.ministryAffiliation.ifBlank { "وزارة التربية" },
            schoolGenderType = config.schoolGenderType.ifBlank { "مدرسة بنين" },
            createdAt = config.createdAt.ifBlank { "2026-10-29" },
            isActive = config.isActive,
            adminUsername = resolvedAdminUser,
            subscriptionExpiryDate = resolvedExpiry,
            isSchoolOfficiallyActivated = config.isSchoolOfficiallyActivated,
            schoolActivationCode = config.schoolActivationCode.ifBlank { config.schoolId.ifBlank { "1001" } }
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
                        senderName = o.optString("senderName", "إدارة المنصة التقنية").replace("المطور أحمد أبو رسلان", "إدارة المنصة التقنية").replace("أبو رسلان", "").trim().ifBlank { "إدارة المنصة التقنية" },
                        createdAt = o.optString("createdAt"),
                        targetSchoolId = o.optString("targetSchoolId", "ALL"),
                        iconKey = o.optString("iconKey", "BELL").ifBlank { "BELL" },
                        imageBase64 = o.optString("imageBase64", ""),
                        imageUrl = o.optString("imageUrl", "")
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
                put("iconKey", b.iconKey.ifBlank { "BELL" })
                put("imageBase64", b.imageBase64)
                put("imageUrl", InputSanitizer.sanitize(b.imageUrl))
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
     * Ultra-fast essential cloud fetch for first-time login (< 1 second):
     * Pulls ONLY `COL_SETTINGS`, `COL_USERS`, and `COL_SUBJECTS` in parallel so credentials
     * can be verified and the user enters the Dashboard immediately without waiting for 25+ collections.
     */
    suspend fun syncEssentialAuthFromCloud(schoolId: String): Boolean {
        val cleanSid = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSid.isBlank()) return false

        return kotlinx.coroutines.coroutineScope {
            val settingsDeferred = async {
                FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_SETTINGS)
            }
            val usersDeferred = async {
                FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_USERS)
            }
            val subjectsDeferred = async {
                FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, FirebaseConfigConstants.COL_SUBJECTS)
            }

            val settingsRaw = settingsDeferred.await()
            val usersRaw = usersDeferred.await()
            val subjectsRaw = subjectsDeferred.await()

            var foundAny = false
            val editor = prefs.edit()
            if (!settingsRaw.isNullOrBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SETTINGS), settingsRaw)
                foundAny = true
            }
            if (!usersRaw.isNullOrBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_USERS), usersRaw)
                foundAny = true
            }
            if (!subjectsRaw.isNullOrBlank()) {
                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SUBJECTS), subjectsRaw)
                foundAny = true
            }
            editor.apply()
            if (foundAny) {
                loadSchoolConfig(cleanSid)
                loadAccounts(cleanSid)
            }
            foundAny
        }
    }

    /**
     * Parallel background lazy sync of all remaining school sub-collections after instant login.
     */
    suspend fun syncRemainingSchoolCollectionsInBackground(schoolId: String) {
        val cleanSid = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSid.isBlank()) return

        val remainingCollections = listOf(
            FirebaseConfigConstants.COL_HONOR_ROLL,
            FirebaseConfigConstants.COL_ATTENDANCE,
            FirebaseConfigConstants.COL_GRADES,
            FirebaseConfigConstants.COL_EXAMS,
            FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK,
            FirebaseConfigConstants.COL_SCHEDULES_PERIODS,
            FirebaseConfigConstants.COL_NOTIFICATIONS,
            FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS,
            FirebaseConfigConstants.COL_HOLIDAYS,
            FirebaseConfigConstants.COL_FEATURE_SETTINGS,
            FirebaseConfigConstants.COL_URGENT_ALERT,
            FirebaseConfigConstants.COL_REPORTS,
            FirebaseConfigConstants.COL_MESSAGES,
            FirebaseConfigConstants.COL_LEAVE_REQUESTS,
            FirebaseConfigConstants.COL_TUITION_FEES,
            FirebaseConfigConstants.COL_EVENTS_GALLERY,
            FirebaseConfigConstants.COL_STUDY_MATERIALS,
            FirebaseConfigConstants.COL_DAILY_BEHAVIOR,
            FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS,
            FirebaseConfigConstants.COL_QUICK_QUIZZES,
            FirebaseConfigConstants.COL_DISMISSAL_CALLS,
            FirebaseConfigConstants.COL_LOST_AND_FOUND,
            FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS,
            FirebaseConfigConstants.COL_STUDENT_ACTIVITIES,
            FirebaseConfigConstants.COL_ACTIVITY_REPORTS,
            FirebaseConfigConstants.COL_USER_PROFILES,
            FirebaseConfigConstants.COL_TEACHER_RATINGS
        )

        // Fetch in parallel batches of 6 to maximize speed without saturating sockets
        remainingCollections.chunked(6).forEach { batch ->
            kotlinx.coroutines.coroutineScope {
                val results = batch.map { col ->
                    col to async {
                        FirebaseCloudSync.fetchSubCollectionPayload(cleanSid, col)
                    }
                }.map { (col, def) -> col to def.await() }

                val editor = prefs.edit()
                results.forEach { (col, payload) ->
                    if (!payload.isNullOrBlank()) {
                        when (col) {
                            FirebaseConfigConstants.COL_SCHEDULES_PERIODS -> {
                                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_SCHEDULES_PERIODS), payload)
                                editor.putString("${scopedKey(cleanSid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)}_periods", payload)
                            }
                            FirebaseConfigConstants.COL_EXAMS -> {
                                editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_EXAMS), payload)
                                editor.putString("${scopedKey(cleanSid, FirebaseConfigConstants.COL_GRADES)}_exams", payload)
                            }
                            FirebaseConfigConstants.COL_TEACHER_RATINGS -> {
                                if (payload != "[]") {
                                    editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_TEACHER_RATINGS), payload)
                                }
                            }
                            FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS -> {
                                if (payload != "[]") {
                                    editor.putString(scopedKey(cleanSid, FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS), payload)
                                }
                            }
                            else -> {
                                editor.putString(scopedKey(cleanSid, col), payload)
                            }
                        }
                    }
                }
                editor.apply()
            }
        }
    }

    /**
     * Pulls a school's sub-collections from Firebase (Firestore/RTDB) into local storage
     * so that logging in with a prefixed username (e.g. 105-1) from any phone works immediately.
     */
    suspend fun syncFullSchoolFromCloud(schoolId: String): Boolean {
        val essentialOk = syncEssentialAuthFromCloud(schoolId)
        syncRemainingSchoolCollectionsInBackground(schoolId)
        return essentialOk
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
                        note = o.optString("note").ifBlank {
                            o.optString("customNote").ifBlank { o.optString("details") }
                        },
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
            val sanitizedNote = InputSanitizer.sanitize(item.note)
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("studentId", item.studentId)
                put("studentName", InputSanitizer.sanitize(item.studentName))
                put("gradeClass", InputSanitizer.sanitize(item.gradeClass))
                put("sectionName", InputSanitizer.sanitize(item.sectionName))
                put("subjectName", InputSanitizer.sanitize(item.subjectName))
                put("behaviorType", item.behaviorType.name)
                put("note", sanitizedNote)
                put("customNote", sanitizedNote)
                put("details", sanitizedNote)
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
                val timestampsObj = o.optJSONObject("answerTimestamps")
                val timestampsMap = mutableMapOf<String, String>()
                if (timestampsObj != null) {
                    val keys = timestampsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val ts = timestampsObj.optString(k, "").trim()
                        if (ts.isNotEmpty()) timestampsMap[k] = ts
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
                        createdAt = ArabicTextNormalizer.normalizeDateIso(o.optString("createdAt")),
                        studentAnswers = answersMap,
                        answerTimestamps = timestampsMap,
                        points = o.optInt("points", 5).coerceAtLeast(1),
                        isClosed = o.optBoolean("isClosed", false),
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
            val tsObj = JSONObject()
            q.answerTimestamps.forEach { (stuId, ts) -> tsObj.put(stuId, ts) }
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
                put("createdAt", ArabicTextNormalizer.normalizeDateIso(q.createdAt))
                put("studentAnswers", ansObj)
                put("answerTimestamps", tsObj)
                put("points", q.points)
                put("isClosed", q.isClosed)
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
    // 15. STUDENT CREATIVE ACTIVITIES, VIOLATION REPORTS & USER PROFILES/SANCTIONS
    // =========================================================================

    fun loadStudentActivityPosts(schoolId: String = getActiveSchoolId()): List<StudentActivityPost> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES), null)
            ?: return emptyList()
        val posts = parseStudentActivityPostsJson(raw, schoolId)
        val subComments = loadStudentActivityCommentsSubCollection(schoolId)
        return mergeCommentsIntoPosts(posts, subComments)
    }

    fun loadStudentActivityCommentsSubCollection(schoolId: String = getActiveSchoolId()): List<StudentActivityComment> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS), null)
            ?: return emptyList()
        return parseStudentActivityCommentsJson(raw, schoolId)
    }

    fun parseStudentActivityCommentsJson(raw: String, schoolId: String = getActiveSchoolId()): List<StudentActivityComment> {
        return try {
            val commentsArr = JSONArray(raw)
            val commentsList = mutableListOf<StudentActivityComment>()
            for (cIdx in 0 until commentsArr.length()) {
                val co = commentsArr.getJSONObject(cIdx)
                val itemSid = co.optString("schoolId", "").trim()
                if (schoolId.isNotEmpty() && itemSid.isNotEmpty() && itemSid != schoolId) continue
                val cRole = runCatching {
                    UserRole.valueOf(co.optString("authorRole", UserRole.STUDENT.name))
                }.getOrDefault(UserRole.STUDENT)

                val repliesArr = co.optJSONArray("replies") ?: JSONArray()
                val repliesList = mutableListOf<StudentActivityReply>()
                for (rIdx in 0 until repliesArr.length()) {
                    val ro = repliesArr.getJSONObject(rIdx)
                    val rRole = runCatching {
                        UserRole.valueOf(ro.optString("authorRole", UserRole.STUDENT.name))
                    }.getOrDefault(UserRole.STUDENT)
                    repliesList.add(
                        StudentActivityReply(
                            id = ro.optString("id"),
                            commentId = ro.optString("commentId"),
                            postId = ro.optString("postId"),
                            authorUserId = ro.optString("authorUserId"),
                            authorName = ro.optString("authorName"),
                            authorRole = rRole,
                            authorGradeClass = if (rRole == UserRole.ADMIN || rRole == UserRole.TEACHER) "" else ro.optString("authorGradeClass", ""),
                            authorPhotoBase64 = ro.optString("authorPhotoBase64", ""),
                            text = ro.optString("text"),
                            createdAt = ro.optString("createdAt"),
                            createdAtMillis = ro.optLong("createdAtMillis", System.currentTimeMillis()),
                            syncStatus = ro.optString("syncStatus", "synced")
                        )
                    )
                }

                commentsList.add(
                    StudentActivityComment(
                        id = co.optString("id"),
                        postId = co.optString("postId"),
                        authorUserId = co.optString("authorUserId"),
                        authorName = co.optString("authorName"),
                        authorRole = cRole,
                        authorGradeClass = if (cRole == UserRole.ADMIN || cRole == UserRole.TEACHER) "" else co.optString("authorGradeClass", ""),
                        authorPhotoBase64 = co.optString("authorPhotoBase64", ""),
                        text = co.optString("text"),
                        createdAt = co.optString("createdAt"),
                        createdAtMillis = co.optLong("createdAtMillis", System.currentTimeMillis()),
                        replies = repliesList,
                        syncStatus = co.optString("syncStatus", "synced")
                    )
                )
            }
            commentsList
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun mergeCommentsIntoPosts(
        posts: List<StudentActivityPost>,
        subCollectionComments: List<StudentActivityComment>
    ): List<StudentActivityPost> {
        if (subCollectionComments.isEmpty()) return posts
        val groupedByPost = subCollectionComments.groupBy { it.postId }
        return posts.map { post ->
            val externalComments = groupedByPost[post.id].orEmpty()
            val merged = (post.comments + externalComments)
                .distinctBy { it.id }
                .sortedBy { it.createdAtMillis }
            post.copy(comments = merged)
        }
    }

    fun parseStudentActivityPostsJson(raw: String, schoolId: String = getActiveSchoolId()): List<StudentActivityPost> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<StudentActivityPost>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue

                val status = runCatching {
                    StudentActivityStatus.valueOf(o.optString("status", StudentActivityStatus.PENDING.name))
                }.getOrDefault(StudentActivityStatus.PENDING)

                val authorRole = runCatching {
                    UserRole.valueOf(o.optString("authorRole", UserRole.STUDENT.name))
                }.getOrDefault(UserRole.STUDENT)

                val likedList = jsonArrayToList(o.optJSONArray("likedByUserIds")).distinct().takeLast(500)
                val heartCount = maxOf(likedList.size, o.optInt("heartCount", 0))

                val legacyCommentsArr = o.optJSONArray("comments")
                val legacyComments = if (legacyCommentsArr != null) {
                    parseStudentActivityCommentsJson(legacyCommentsArr.toString(), schoolId)
                } else {
                    emptyList()
                }

                list.add(
                    StudentActivityPost(
                        id = o.optString("id"),
                        schoolId = itemSid,
                        schoolName = o.optString("schoolName", ""),
                        authorUserId = o.optString("authorUserId"),
                        authorStudentId = o.optString("authorStudentId", ""),
                        authorName = o.optString("authorName"),
                        authorRole = authorRole,
                        authorGradeClass = if (authorRole == UserRole.ADMIN || authorRole == UserRole.TEACHER) "" else o.optString("authorGradeClass", ""),
                        authorSectionName = if (authorRole == UserRole.ADMIN || authorRole == UserRole.TEACHER) "" else o.optString("authorSectionName", ""),
                        authorPhotoBase64 = o.optString("authorPhotoBase64", ""),
                        category = o.optString("category", "موهبة وإبداع علمي"),
                        title = o.optString("title"),
                        content = o.optString("content"),
                        imageBase64 = o.optString("imageBase64", ""),
                        status = status,
                        reviewedByName = o.optString("reviewedByName", ""),
                        createdAt = o.optString("createdAt"),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                        heartCount = heartCount,
                        likedByUserIds = likedList,
                        comments = legacyComments
                    )
                )
            }
            val subComments = loadStudentActivityCommentsSubCollection(schoolId)
            mergeCommentsIntoPosts(list, subComments)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveStudentActivityPosts(posts: List<StudentActivityPost>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val postsArr = JSONArray()
        val allCommentsArr = JSONArray()

        posts.filter { it.schoolId.isBlank() || it.schoolId == schoolId }.forEach { p ->
            val cleanLikes = p.likedByUserIds.filter { it.isNotBlank() }.distinct().takeLast(500)
            val finalHearts = if (cleanLikes.isNotEmpty() || p.heartCount == 0) cleanLikes.size else maxOf(cleanLikes.size, p.heartCount)
            val safeAuthorClass = if (p.authorRole == UserRole.ADMIN || p.authorRole == UserRole.TEACHER) "" else InputSanitizer.sanitize(p.authorGradeClass)
            val safeAuthorSec = if (p.authorRole == UserRole.ADMIN || p.authorRole == UserRole.TEACHER) "" else InputSanitizer.sanitize(p.authorSectionName)

            p.comments.forEach { c ->
                val repliesArr = JSONArray()
                c.replies.forEach { r ->
                    val safeReplyClass = if (r.authorRole == UserRole.ADMIN || r.authorRole == UserRole.TEACHER) "" else InputSanitizer.sanitize(r.authorGradeClass)
                    repliesArr.put(JSONObject().apply {
                        put("id", r.id)
                        put("commentId", r.commentId)
                        put("postId", p.id)
                        put("schoolId", schoolId)
                        put("authorUserId", r.authorUserId)
                        put("authorName", InputSanitizer.sanitize(r.authorName))
                        put("authorRole", r.authorRole.name)
                        put("authorGradeClass", safeReplyClass)
                        put("authorPhotoBase64", r.authorPhotoBase64)
                        put("text", InputSanitizer.sanitize(r.text))
                        put("createdAt", r.createdAt)
                        put("createdAtMillis", r.createdAtMillis)
                        put("syncStatus", r.syncStatus)
                    })
                }
                val safeCommentClass = if (c.authorRole == UserRole.ADMIN || c.authorRole == UserRole.TEACHER) "" else InputSanitizer.sanitize(c.authorGradeClass)
                allCommentsArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("postId", p.id)
                    put("schoolId", schoolId)
                    put("authorUserId", c.authorUserId)
                    put("authorName", InputSanitizer.sanitize(c.authorName))
                    put("authorRole", c.authorRole.name)
                    put("authorGradeClass", safeCommentClass)
                    put("authorPhotoBase64", c.authorPhotoBase64)
                    put("text", InputSanitizer.sanitize(c.text))
                    put("createdAt", c.createdAt)
                    put("createdAtMillis", c.createdAtMillis)
                    put("syncStatus", c.syncStatus)
                    put("replies", repliesArr)
                })
            }

            // Store post document WITHOUT embedding the heavy comments array inside the post document
            postsArr.put(JSONObject().apply {
                put("id", p.id)
                put("schoolId", schoolId)
                put("schoolName", InputSanitizer.sanitize(p.schoolName))
                put("authorUserId", p.authorUserId)
                put("authorStudentId", p.authorStudentId)
                put("authorName", InputSanitizer.sanitize(p.authorName))
                put("authorRole", p.authorRole.name)
                put("authorGradeClass", safeAuthorClass)
                put("authorSectionName", safeAuthorSec)
                put("authorPhotoBase64", p.authorPhotoBase64)
                put("category", InputSanitizer.sanitize(p.category))
                put("title", InputSanitizer.sanitize(p.title))
                put("content", InputSanitizer.sanitize(p.content))
                put("imageBase64", p.imageBase64)
                put("status", p.status.name)
                put("reviewedByName", InputSanitizer.sanitize(p.reviewedByName))
                put("createdAt", p.createdAt)
                put("createdAtMillis", p.createdAtMillis)
                put("heartCount", finalHearts)
                put("likedByUserIds", JSONArray(cleanLikes))
            })
        }

        val postsJsonStr = postsArr.toString()
        val commentsJsonStr = allCommentsArr.toString()
        prefs.edit()
            .putString(scopedKey(schoolId, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES), postsJsonStr)
            .putString(scopedKey(schoolId, FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS), commentsJsonStr)
            .apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES, postsJsonStr)
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS, commentsJsonStr)
    }

    fun loadActivityViolationReports(schoolId: String = getActiveSchoolId()): List<StudentActivityViolationReport> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_ACTIVITY_REPORTS), null)
            ?: return emptyList()
        return parseActivityViolationReportsJson(raw, schoolId)
    }

    fun parseActivityViolationReportsJson(raw: String, schoolId: String = getActiveSchoolId()): List<StudentActivityViolationReport> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<StudentActivityViolationReport>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val tType = runCatching {
                    ViolationTargetType.valueOf(o.optString("targetType", ViolationTargetType.POST.name))
                }.getOrDefault(ViolationTargetType.POST)
                val rRole = runCatching {
                    UserRole.valueOf(o.optString("reportedUserRole", UserRole.STUDENT.name))
                }.getOrDefault(UserRole.STUDENT)
                list.add(
                    StudentActivityViolationReport(
                        id = o.optString("id"),
                        schoolId = itemSid,
                        postId = o.optString("postId"),
                        commentId = o.optString("commentId", ""),
                        replyId = o.optString("replyId", ""),
                        targetType = tType,
                        reportedUserId = o.optString("reportedUserId"),
                        reportedStudentId = o.optString("reportedStudentId", ""),
                        reportedUserName = o.optString("reportedUserName"),
                        reportedUserRole = rRole,
                        reportedContentExcerpt = o.optString("reportedContentExcerpt"),
                        reporterUserId = o.optString("reporterUserId"),
                        reporterName = o.optString("reporterName"),
                        reason = o.optString("reason"),
                        createdAt = o.optString("createdAt"),
                        isResolved = o.optBoolean("isResolved", false),
                        resolutionActionNote = o.optString("resolutionActionNote", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveActivityViolationReports(reports: List<StudentActivityViolationReport>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        reports.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("schoolId", schoolId)
                put("postId", r.postId)
                put("commentId", r.commentId)
                put("replyId", r.replyId)
                put("targetType", r.targetType.name)
                put("reportedUserId", r.reportedUserId)
                put("reportedStudentId", r.reportedStudentId)
                put("reportedUserName", InputSanitizer.sanitize(r.reportedUserName))
                put("reportedUserRole", r.reportedUserRole.name)
                put("reportedContentExcerpt", InputSanitizer.sanitize(r.reportedContentExcerpt))
                put("reporterUserId", r.reporterUserId)
                put("reporterName", InputSanitizer.sanitize(r.reporterName))
                put("reason", InputSanitizer.sanitize(r.reason))
                put("createdAt", r.createdAt)
                put("isResolved", r.isResolved)
                put("resolutionActionNote", InputSanitizer.sanitize(r.resolutionActionNote))
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_ACTIVITY_REPORTS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_ACTIVITY_REPORTS, jsonStr)
    }

    fun loadUserProfilesAndSanctions(schoolId: String = getActiveSchoolId()): Map<String, UserActivityProfileAndSanctions> {
        if (schoolId.isBlank()) return emptyMap()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_USER_PROFILES), null)
            ?: return emptyMap()
        return parseUserProfilesAndSanctionsJson(raw, schoolId)
    }

    fun parseUserProfilesAndSanctionsJson(raw: String, schoolId: String = getActiveSchoolId()): Map<String, UserActivityProfileAndSanctions> {
        return try {
            val arr = JSONArray(raw)
            val map = mutableMapOf<String, UserActivityProfileAndSanctions>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val uid = o.optString("userId")
                if (uid.isNotBlank()) {
                    map[uid] = UserActivityProfileAndSanctions(
                        userId = uid,
                        schoolId = o.optString("schoolId", schoolId).ifBlank { schoolId },
                        bio = o.optString("bio", ""),
                        followingUserIds = jsonArrayToList(o.optJSONArray("followingUserIds")).distinct(),
                        mutePostingUntilMillis = o.optLong("mutePostingUntilMillis", 0L),
                        isPermanentPostingBan = o.optBoolean("isPermanentPostingBan", false),
                        mutePostingReason = o.optString("mutePostingReason", ""),
                        muteInteractionUntilMillis = o.optLong("muteInteractionUntilMillis", 0L),
                        isPermanentInteractionBan = o.optBoolean("isPermanentInteractionBan", false),
                        muteInteractionReason = o.optString("muteInteractionReason", ""),
                        warningCount = o.optInt("warningCount", 0),
                        lastWarningText = o.optString("lastWarningText", ""),
                        updatedAt = o.optString("updatedAt", "")
                    )
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveUserProfilesAndSanctions(profiles: Map<String, UserActivityProfileAndSanctions>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        profiles.values.forEach { p ->
            arr.put(JSONObject().apply {
                put("userId", p.userId)
                put("schoolId", schoolId)
                put("bio", InputSanitizer.sanitize(p.bio))
                put("followingUserIds", JSONArray(p.followingUserIds.distinct()))
                put("mutePostingUntilMillis", p.mutePostingUntilMillis)
                put("isPermanentPostingBan", p.isPermanentPostingBan)
                put("mutePostingReason", InputSanitizer.sanitize(p.mutePostingReason))
                put("muteInteractionUntilMillis", p.muteInteractionUntilMillis)
                put("isPermanentInteractionBan", p.isPermanentInteractionBan)
                put("muteInteractionReason", InputSanitizer.sanitize(p.muteInteractionReason))
                put("warningCount", p.warningCount)
                put("lastWarningText", InputSanitizer.sanitize(p.lastWarningText))
                put("updatedAt", p.updatedAt)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_USER_PROFILES), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_USER_PROFILES, jsonStr)
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

    // =========================================================================
    // 16. ENCRYPTED DEVELOPER CLOUD VAULT & SOVEREIGNTY BACKUP / DEEP DELETE
    // =========================================================================

    fun loadDeveloperVaultConfig(): DeveloperVaultConfig {
        val raw = prefs.getString("system_secure_developer_vault_json", null)
            ?: return DeveloperVaultConfig(
                usernameHash = com.example.util.SecurityShieldHelper.DEFAULT_DEVELOPER_CODE_HASH,
                passwordHash = com.example.util.SecurityShieldHelper.DEFAULT_DEVELOPER_PASS_HASH
            )
        return parseDeveloperVaultConfigJson(raw)
    }

    fun parseDeveloperVaultConfigJson(raw: String): DeveloperVaultConfig {
        return try {
            val o = JSONObject(raw)
            DeveloperVaultConfig(
                usernameHash = o.optString("usernameHash", com.example.util.SecurityShieldHelper.DEFAULT_DEVELOPER_CODE_HASH),
                passwordHash = o.optString("passwordHash", com.example.util.SecurityShieldHelper.DEFAULT_DEVELOPER_PASS_HASH),
                encryptedVaultPayload = o.optString("encryptedVaultPayload", ""),
                maskedDisplayLabel = o.optString("maskedDisplayLabel", "مطور النظام العام (مشفر سحابياً)"),
                updatedAt = o.optString("updatedAt", "")
            )
        } catch (_: Exception) {
            DeveloperVaultConfig(
                usernameHash = com.example.util.SecurityShieldHelper.DEFAULT_DEVELOPER_CODE_HASH,
                passwordHash = com.example.util.SecurityShieldHelper.DEFAULT_DEVELOPER_PASS_HASH
            )
        }
    }

    fun saveDeveloperVaultConfig(config: DeveloperVaultConfig) {
        val obj = JSONObject().apply {
            put("usernameHash", config.usernameHash)
            put("passwordHash", config.passwordHash)
            put("encryptedVaultPayload", config.encryptedVaultPayload)
            put("maskedDisplayLabel", InputSanitizer.sanitize(config.maskedDisplayLabel))
            put("updatedAt", config.updatedAt)
        }
        val jsonStr = obj.toString()
        prefs.edit().putString("system_secure_developer_vault_json", jsonStr).apply()
        FirebaseCloudSync.pushGlobalCollectionAsync(
            collectionName = FirebaseConfigConstants.COL_SYSTEM_SECURE_CONFIG,
            docName = FirebaseConfigConstants.DOC_DEVELOPER_VAULT,
            jsonPayload = jsonStr
        )
    }

    /**
     * Deeply deletes a school, all its subcollections, and its licenses from both local cache and Firebase Cloud.
     */
    fun deleteSchoolCompletely(schoolId: String) {
        val cleanSid = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSid.isBlank()) return

        val subCols = listOf(
            FirebaseConfigConstants.COL_SETTINGS,
            FirebaseConfigConstants.COL_USERS,
            FirebaseConfigConstants.COL_SUBJECTS,
            FirebaseConfigConstants.COL_ATTENDANCE,
            FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK,
            FirebaseConfigConstants.COL_GRADES,
            FirebaseConfigConstants.COL_HONOR_ROLL,
            FirebaseConfigConstants.COL_TUITION_FEES,
            FirebaseConfigConstants.COL_REPORTS,
            FirebaseConfigConstants.COL_MESSAGES,
            FirebaseConfigConstants.COL_EVENTS_GALLERY,
            FirebaseConfigConstants.COL_NOTIFICATIONS,
            FirebaseConfigConstants.COL_SCHEDULES_PERIODS,
            FirebaseConfigConstants.COL_EXAMS,
            FirebaseConfigConstants.COL_LEAVE_REQUESTS,
            FirebaseConfigConstants.COL_BUS_TRACKING,
            FirebaseConfigConstants.COL_STUDY_MATERIALS,
            FirebaseConfigConstants.COL_FEATURE_SETTINGS,
            FirebaseConfigConstants.COL_DAILY_BEHAVIOR,
            FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS,
            FirebaseConfigConstants.COL_QUICK_QUIZZES,
            FirebaseConfigConstants.COL_DISMISSAL_CALLS,
            FirebaseConfigConstants.COL_LOST_AND_FOUND,
            FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS,
            FirebaseConfigConstants.COL_URGENT_ALERT,
            FirebaseConfigConstants.COL_STUDENT_ACTIVITIES,
            FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS,
            FirebaseConfigConstants.COL_ACTIVITY_REPORTS,
            FirebaseConfigConstants.COL_USER_PROFILES,
            FirebaseConfigConstants.COL_HOLIDAYS
        )

        val editor = prefs.edit()
        subCols.forEach { sub ->
            editor.remove(scopedKey(cleanSid, sub))
        }
        editor.remove("${scopedKey(cleanSid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)}_periods")
        editor.remove("${scopedKey(cleanSid, FirebaseConfigConstants.COL_GRADES)}_exams")
        editor.remove("last_shown_urgent_alert_$cleanSid")
        if (getActiveSchoolId() == cleanSid) {
            editor.remove("active_school_id")
            editor.remove("active_user_session")
        }
        editor.apply()

        val allLicenses = loadSchoolLicenses()
        val matchedLicenseCodes = allLicenses.filter { it.usedBySchoolId == cleanSid }.map { it.code }
        val remainingLicenses = allLicenses.filterNot { it.usedBySchoolId == cleanSid }
        saveSchoolLicenses(remainingLicenses)

        val remainingSchools = loadRegisteredSchools().filterNot { it.schoolId == cleanSid }
        saveRegisteredSchools(remainingSchools)

        FirebaseCloudSync.deleteSchoolNodesFromCloudAsync(cleanSid, matchedLicenseCodes)
    }

    /**
     * Exports a complete JSON backup of all schools, their subcollections, licenses, and global configs.
     */
    fun exportFullSystemBackupJson(): String {
        val subCols = listOf(
            FirebaseConfigConstants.COL_SETTINGS,
            FirebaseConfigConstants.COL_USERS,
            FirebaseConfigConstants.COL_SUBJECTS,
            FirebaseConfigConstants.COL_ATTENDANCE,
            FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK,
            FirebaseConfigConstants.COL_GRADES,
            FirebaseConfigConstants.COL_HONOR_ROLL,
            FirebaseConfigConstants.COL_TUITION_FEES,
            FirebaseConfigConstants.COL_REPORTS,
            FirebaseConfigConstants.COL_MESSAGES,
            FirebaseConfigConstants.COL_EVENTS_GALLERY,
            FirebaseConfigConstants.COL_NOTIFICATIONS,
            FirebaseConfigConstants.COL_SCHEDULES_PERIODS,
            FirebaseConfigConstants.COL_EXAMS,
            FirebaseConfigConstants.COL_LEAVE_REQUESTS,
            FirebaseConfigConstants.COL_BUS_TRACKING,
            FirebaseConfigConstants.COL_STUDY_MATERIALS,
            FirebaseConfigConstants.COL_FEATURE_SETTINGS,
            FirebaseConfigConstants.COL_DAILY_BEHAVIOR,
            FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS,
            FirebaseConfigConstants.COL_QUICK_QUIZZES,
            FirebaseConfigConstants.COL_DISMISSAL_CALLS,
            FirebaseConfigConstants.COL_LOST_AND_FOUND,
            FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS,
            FirebaseConfigConstants.COL_URGENT_ALERT,
            FirebaseConfigConstants.COL_STUDENT_ACTIVITIES,
            FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS,
            FirebaseConfigConstants.COL_ACTIVITY_REPORTS,
            FirebaseConfigConstants.COL_USER_PROFILES,
            FirebaseConfigConstants.COL_HOLIDAYS,
            FirebaseConfigConstants.COL_TEACHER_RATINGS
        )

        val schoolsList = loadRegisteredSchools()
        val activeSid = getActiveSchoolId()
        val allSchoolIds = (schoolsList.map { it.schoolId } + listOfNotNull(activeSid.takeIf { it.isNotBlank() })).distinct()

        val schoolsDataObj = JSONObject()
        allSchoolIds.forEach { sid ->
            val schoolSubObj = JSONObject()
            subCols.forEach { sub ->
                val rawSub = getRawCachedSubCollection(sid, sub)
                if (!rawSub.isNullOrBlank()) {
                    schoolSubObj.put(sub, rawSub)
                }
            }
            schoolsDataObj.put(sid, schoolSubObj)
        }

        val rootBackup = JSONObject().apply {
            put("backupVersion", "3.0")
            put("exportedAt", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
            put("registeredSchoolsRaw", prefs.getString("global_registered_schools", "[]") ?: "[]")
            put("schoolLicensesRaw", prefs.getString("global_school_licenses", "[]") ?: "[]")
            put("developerBroadcastsRaw", prefs.getString("global_developer_broadcasts", "[]") ?: "[]")
            put("appUpdateConfigRaw", prefs.getString("global_app_update_config_json", "{}") ?: "{}")
            put("occasionThemeRaw", prefs.getString("global_occasion_theme_json", "{}") ?: "{}")
            put("developerVaultRaw", prefs.getString("system_secure_developer_vault_json", "{}") ?: "{}")
            put("schoolsData", schoolsDataObj)
        }
        return rootBackup.toString(2)
    }

    /**
     * Restores a complete JSON backup of all schools, subcollections, licenses, and global settings to local storage & Firebase Cloud.
     * Returns Result<Int> containing the number of restored schools.
     */
    fun restoreFullSystemBackupJson(rawJson: String): Result<Int> {
        return try {
            val root = JSONObject(rawJson)
            val regSchoolsRaw = root.optString("registeredSchoolsRaw", "")
            if (regSchoolsRaw.isNotBlank()) {
                val parsedSchools = parseRegisteredSchoolsJson(regSchoolsRaw)
                if (parsedSchools.isNotEmpty()) {
                    saveRegisteredSchools(parsedSchools)
                }
            }
            val licensesRaw = root.optString("schoolLicensesRaw", "")
            if (licensesRaw.isNotBlank()) {
                val parsedLicenses = parseSchoolLicensesJson(licensesRaw)
                if (parsedLicenses.isNotEmpty()) {
                    saveSchoolLicenses(parsedLicenses)
                }
            }
            val broadcastsRaw = root.optString("developerBroadcastsRaw", "")
            if (broadcastsRaw.isNotBlank()) {
                val parsedBroadcasts = parseDeveloperBroadcastsJson(broadcastsRaw)
                if (parsedBroadcasts.isNotEmpty()) {
                    saveDeveloperBroadcasts(parsedBroadcasts)
                }
            }
            val appUpdateRaw = root.optString("appUpdateConfigRaw", "")
            if (appUpdateRaw.isNotBlank() && appUpdateRaw != "{}") {
                saveAppUpdateConfig(parseAppUpdateConfigJson(appUpdateRaw))
            }
            val occasionRaw = root.optString("occasionThemeRaw", "")
            if (occasionRaw.isNotBlank() && occasionRaw != "{}") {
                saveOccasionThemeConfig(parseOccasionThemeConfigJson(occasionRaw))
            }
            val vaultRaw = root.optString("developerVaultRaw", "")
            if (vaultRaw.isNotBlank() && vaultRaw != "{}") {
                saveDeveloperVaultConfig(parseDeveloperVaultConfigJson(vaultRaw))
            }

            val schoolsDataObj = root.optJSONObject("schoolsData")
            var restoredSchoolsCount = 0
            if (schoolsDataObj != null) {
                val schoolKeys = schoolsDataObj.keys()
                while (schoolKeys.hasNext()) {
                    val sid = InputSanitizer.sanitizeSchoolId(schoolKeys.next())
                    if (sid.isBlank()) continue
                    val subObj = schoolsDataObj.optJSONObject(sid) ?: continue
                    val subKeys = subObj.keys()
                    var hasSubData = false
                    while (subKeys.hasNext()) {
                        val subName = subKeys.next()
                        val payloadStr = subObj.optString(subName, "")
                        if (payloadStr.isNotBlank()) {
                            saveRawCachedSubCollection(sid, subName, payloadStr)
                            FirebaseCloudSync.pushSubCollectionAsync(sid, subName, payloadStr)
                            hasSubData = true
                        }
                    }
                    if (hasSubData) {
                        restoredSchoolsCount++
                    }
                }
            }
            Result.success(maxOf(restoredSchoolsCount, loadRegisteredSchools().size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 11. Teacher Evaluation & Excellence Ratings (تقييم وتميّز الكادر التدريسي)
    // =========================================================================

    private fun loadTeacherRatingsDirectOnly(schoolId: String = getActiveSchoolId()): List<TeacherRatingRecord> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_TEACHER_RATINGS), null)
            ?: return emptyList()
        return parseTeacherRatingsJson(raw, schoolId)
    }

    private fun teacherRatingsToJsonArray(
        ratings: List<TeacherRatingRecord>,
        schoolId: String
    ): JSONArray {
        val arr = JSONArray()
        ratings.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("schoolId", schoolId)
                put("teacherId", r.teacherId)
                put("teacherName", InputSanitizer.sanitize(r.teacherName))
                put("parentId", r.parentId)
                put("parentName", InputSanitizer.sanitize(r.parentName))
                put("stars", r.stars.coerceIn(1, 5))
                put("praiseTag", InputSanitizer.sanitize(r.praiseTag))
                put("note", InputSanitizer.sanitize(r.note))
                put("createdAt", r.createdAt)
            })
        }
        return arr
    }

    private fun mergeTeacherRatingsLists(
        primary: List<TeacherRatingRecord>,
        secondary: List<TeacherRatingRecord>
    ): List<TeacherRatingRecord> {
        if (secondary.isEmpty()) return primary
        if (primary.isEmpty()) return secondary
        val combined = (primary + secondary)
            .distinctBy { r ->
                if (r.id.isNotBlank()) r.id else "${r.teacherId}_${r.parentId.lowercase()}"
            }
        return combined
    }

    fun mergeEmbeddedTeacherRatingsIntoLocalCache(rawJsonArrayStr: String, schoolId: String) {
        if (schoolId.isBlank() || rawJsonArrayStr.isBlank()) return
        val incoming = parseTeacherRatingsJson(rawJsonArrayStr, schoolId)
        if (incoming.isEmpty()) return
        val current = loadTeacherRatingsDirectOnly(schoolId)
        val merged = mergeTeacherRatingsLists(current, incoming)
        if (merged.size != current.size || current.isEmpty()) {
            val arrStr = teacherRatingsToJsonArray(merged, schoolId).toString()
            prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_TEACHER_RATINGS), arrStr).apply()
        }
    }

    fun loadTeacherRatings(schoolId: String = getActiveSchoolId()): List<TeacherRatingRecord> {
        if (schoolId.isBlank()) return emptyList()
        var merged = loadTeacherRatingsDirectOnly(schoolId)

        // Also check embedded teacherRatings in settings JSON if present
        prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_SETTINGS), null)?.let { settingsRaw ->
            runCatching {
                val obj = JSONObject(settingsRaw)
                val arr = obj.optJSONArray("teacherRatings")
                if (arr != null && arr.length() > 0) {
                    merged = mergeTeacherRatingsLists(merged, parseTeacherRatingsJson(arr.toString(), schoolId))
                }
            }
        }

        // Also check embedded teacherRatings in users JSON if present
        prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_USERS), null)?.let { usersRaw ->
            runCatching {
                val arr = JSONArray(usersRaw)
                for (i in 0 until arr.length()) {
                    val uObj = arr.optJSONObject(i) ?: continue
                    val tArr = uObj.optJSONArray("teacherRatings")
                    if (tArr != null && tArr.length() > 0) {
                        merged = mergeTeacherRatingsLists(merged, parseTeacherRatingsJson(tArr.toString(), schoolId))
                    }
                }
            }
        }

        return merged
    }

    fun parseTeacherRatingsJson(raw: String, schoolId: String = getActiveSchoolId()): List<TeacherRatingRecord> {
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TeacherRatingRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSchoolId = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSchoolId != schoolId) continue
                list.add(
                    TeacherRatingRecord(
                        id = o.optString("id"),
                        schoolId = itemSchoolId,
                        teacherId = o.optString("teacherId"),
                        teacherName = o.optString("teacherName"),
                        parentId = o.optString("parentId"),
                        parentName = o.optString("parentName"),
                        stars = o.optInt("stars", 5).coerceIn(1, 5),
                        praiseTag = o.optString("praiseTag", ""),
                        note = o.optString("note", ""),
                        createdAt = o.optString("createdAt", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveTeacherRatings(ratings: List<TeacherRatingRecord>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val mergedRatings = mergeTeacherRatingsLists(ratings, loadTeacherRatingsDirectOnly(schoolId))
        val arr = teacherRatingsToJsonArray(mergedRatings, schoolId)
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_TEACHER_RATINGS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_TEACHER_RATINGS, jsonStr)

        // Also persist inside schools/{schoolId}/settings and schools/{schoolId}/users in Cloud so ratings never get lost on reinstall
        val currentCfg = loadSchoolConfig(schoolId)
        if (currentCfg.schoolId.isNotBlank() || schoolId.isNotBlank()) {
            saveSchoolConfig(currentCfg.copy(schoolId = schoolId))
        }
        val currentAccs = loadAccounts(schoolId)
        if (currentAccs.isNotEmpty()) {
            saveAccounts(currentAccs, schoolId)
        }
    }

    fun getFirstAttendanceTimestampForDate(dateIso: String, schoolId: String = getActiveSchoolId()): Long {
        if (schoolId.isBlank() || dateIso.isBlank()) return 0L
        return prefs.getLong(scopedKey(schoolId, "first_attendance_ts_$dateIso"), 0L)
    }

    fun setFirstAttendanceTimestampForDate(dateIso: String, timestampMs: Long, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank() || dateIso.isBlank()) return
        prefs.edit().putLong(scopedKey(schoolId, "first_attendance_ts_$dateIso"), timestampMs).apply()
    }

    // =========================================================================
    // 18. schools/{schoolId}/issued_report_cards (الكارتات المدرسية الذكية الصادرة)
    // =========================================================================

    fun loadDismissedReportCardIds(schoolId: String = getActiveSchoolId()): Set<String> {
        if (schoolId.isBlank()) return emptySet()
        return prefs.getStringSet(scopedKey(schoolId, "dismissed_home_report_cards"), emptySet())?.toSet() ?: emptySet()
    }

    fun markReportCardDismissedLocally(cardId: String, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank() || cardId.isBlank()) return
        val current = loadDismissedReportCardIds(schoolId).toMutableSet()
        current.add(cardId)
        prefs.edit().putStringSet(scopedKey(schoolId, "dismissed_home_report_cards"), current).apply()
    }

    fun clearReportCardDismissedLocally(cardId: String, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank() || cardId.isBlank()) return
        val current = loadDismissedReportCardIds(schoolId).toMutableSet()
        if (current.remove(cardId)) {
            prefs.edit().putStringSet(scopedKey(schoolId, "dismissed_home_report_cards"), current).apply()
        }
    }

    fun loadIssuedReportCards(schoolId: String = getActiveSchoolId()): List<IssuedStudentReportCard> {
        if (schoolId.isBlank()) return emptyList()
        val raw = prefs.getString(scopedKey(schoolId, FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS), null)
            ?: return emptyList()
        return parseIssuedReportCardsJson(raw, schoolId)
    }

    fun parseIssuedReportCardsJson(raw: String, schoolId: String = getActiveSchoolId()): List<IssuedStudentReportCard> {
        return try {
            val localDismissedIds = loadDismissedReportCardIds(schoolId)
            val arr = JSONArray(raw)
            val list = mutableListOf<IssuedStudentReportCard>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemSid = o.optString("schoolId", schoolId).ifBlank { schoolId }
                if (schoolId.isNotEmpty() && itemSid != schoolId) continue
                val cardId = o.optString("id")
                val pType = runCatching {
                    ReportCardPeriodType.valueOf(o.optString("periodType", ReportCardPeriodType.COMPREHENSIVE_ALL.name))
                }.getOrDefault(ReportCardPeriodType.COMPREHENSIVE_ALL)
                val subArr = o.optJSONArray("subjects") ?: JSONArray()
                val subjects = mutableListOf<IssuedReportCardSubjectItem>()
                for (sIdx in 0 until subArr.length()) {
                    val sObj = subArr.optJSONObject(sIdx) ?: continue
                    subjects.add(
                        IssuedReportCardSubjectItem(
                            subjectName = sObj.optString("subjectName"),
                            score = sObj.optInt("score", 0),
                            maxScoreScale = sObj.optInt("maxScoreScale", 100),
                            isPassed = sObj.optBoolean("isPassed", true),
                            dailyScore = sObj.optInt("dailyScore", 0),
                            month1Score = sObj.optInt("month1Score", 0),
                            month2Score = sObj.optInt("month2Score", 0),
                            midYearScore = sObj.optInt("midYearScore", 0),
                            annualEffortScore = sObj.optInt("annualEffortScore", 0),
                            finalExamScore = sObj.optInt("finalExamScore", 0),
                            finalAverage = sObj.optInt("finalAverage", 0)
                        )
                    )
                }
                val isDismissed = o.optBoolean("isDismissedFromHome", false) || (cardId in localDismissedIds)
                list.add(
                    IssuedStudentReportCard(
                        id = cardId,
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        sectionName = o.optString("sectionName"),
                        periodType = pType,
                        subjects = subjects,
                        totalScore = o.optInt("totalScore", 0),
                        averagePercent = o.optInt("averagePercent", 0),
                        isOverallPassed = o.optBoolean("isOverallPassed", true),
                        failedSubjectsCount = o.optInt("failedSubjectsCount", 0),
                        issuedAt = o.optString("issuedAt"),
                        principalName = o.optString("principalName", "إدارة المدرسة"),
                        schoolName = o.optString("schoolName"),
                        isViewedByParent = o.optBoolean("isViewedByParent", false),
                        isDismissedFromHome = isDismissed,
                        schoolId = itemSid
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveIssuedReportCards(cards: List<IssuedStudentReportCard>, schoolId: String = getActiveSchoolId()) {
        if (schoolId.isBlank()) return
        val arr = JSONArray()
        cards.forEach { c ->
            val subArr = JSONArray()
            c.subjects.forEach { s ->
                subArr.put(JSONObject().apply {
                    put("subjectName", InputSanitizer.sanitize(s.subjectName))
                    put("score", s.score)
                    put("maxScoreScale", s.maxScoreScale)
                    put("isPassed", s.isPassed)
                    put("dailyScore", s.dailyScore)
                    put("month1Score", s.month1Score)
                    put("month2Score", s.month2Score)
                    put("midYearScore", s.midYearScore)
                    put("annualEffortScore", s.annualEffortScore)
                    put("finalExamScore", s.finalExamScore)
                    put("finalAverage", s.finalAverage)
                })
            }
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("studentId", c.studentId)
                put("studentName", InputSanitizer.sanitize(c.studentName))
                put("gradeClass", InputSanitizer.sanitize(c.gradeClass))
                put("sectionName", InputSanitizer.sanitize(c.sectionName))
                put("periodType", c.periodType.name)
                put("subjects", subArr)
                put("totalScore", c.totalScore)
                put("averagePercent", c.averagePercent)
                put("isOverallPassed", c.isOverallPassed)
                put("failedSubjectsCount", c.failedSubjectsCount)
                put("issuedAt", c.issuedAt)
                put("principalName", InputSanitizer.sanitize(c.principalName))
                put("schoolName", InputSanitizer.sanitize(c.schoolName))
                put("isViewedByParent", c.isViewedByParent)
                put("isDismissedFromHome", c.isDismissedFromHome)
                put("schoolId", schoolId)
            })
        }
        val jsonStr = arr.toString()
        prefs.edit().putString(scopedKey(schoolId, FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(schoolId, FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS, jsonStr)
    }

    fun loadOfficialPrimaryCurriculumBooks(): List<com.example.ui.screens.CurriculumBookItem> {
        return com.example.ui.screens.IraqiCurriculumCatalog.defaultPrimaryBooks
    }

    // =========================================================================
    // 19. MASTER SYSTEM ADMIN CONSOLE HELPERS (إدارة نظام مدرستي المركزي للمطور)
    // =========================================================================

    fun loadMasterDeveloperSecretCode(): String {
        val saved = prefs.getString("master_developer_secret_access_code", null)?.trim().orEmpty()
        return saved.ifBlank { "DEV-2026" }
    }

    fun saveMasterDeveloperSecretCode(newCode: String) {
        val clean = InputSanitizer.sanitize(newCode.trim()).ifBlank { "DEV-2026" }
        prefs.edit().putString("master_developer_secret_access_code", clean).apply()
        val payload = JSONObject().apply {
            put("secretAccessCode", clean)
            put("updatedAt", System.currentTimeMillis())
        }.toString()
        FirebaseCloudSync.pushGlobalCollectionAsync(
            collectionName = FirebaseConfigConstants.COL_SYSTEM_SECURE_CONFIG,
            docName = "master_console_secret",
            jsonPayload = payload
        )
    }

    fun savePlainAdminPasswordNote(schoolId: String, plainPassword: String) {
        val cleanSid = schoolId.trim()
        if (cleanSid.isBlank()) return
        prefs.edit().putString("master_admin_plain_pass_$cleanSid", plainPassword.trim()).apply()
    }

    fun loadPlainAdminPasswordNote(schoolId: String): String {
        val cleanSid = schoolId.trim()
        if (cleanSid.isBlank()) return "123456"
        return prefs.getString("master_admin_plain_pass_$cleanSid", null)?.takeIf { it.isNotBlank() } ?: "123456"
    }

    fun saveSchoolConfigForTargetSchool(config: SchoolConfig, targetSchoolId: String) {
        val sid = targetSchoolId.trim().ifBlank { config.schoolId.trim() }
        if (sid.isBlank()) return
        val previousActiveSid = getActiveSchoolId()
        saveSchoolConfig(config.copy(schoolId = sid))
        if (previousActiveSid.isNotBlank() && previousActiveSid != sid) {
            setActiveSchoolId(previousActiveSid)
        }
    }
}




