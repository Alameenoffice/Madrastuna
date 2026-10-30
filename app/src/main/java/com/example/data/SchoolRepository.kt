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
                phone = obj.optString("phone", "07512362567")
            )
        } catch (e: Exception) {
            SchoolConfig()
        }
    }

    fun saveSchoolConfig(config: SchoolConfig) {
        val sid = config.schoolId.ifBlank { getActiveSchoolId() }.ifBlank {
            generateUniqueThreeDigitSchoolCode()
        }
        setActiveSchoolId(sid)

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
        }
        val jsonStr = obj.toString()
        prefs.edit().putString(scopedKey(sid, FirebaseConfigConstants.COL_SETTINGS), jsonStr).apply()
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_SETTINGS, jsonStr)

        if (config.subjectConfigs.isNotEmpty()) {
            saveSubjectConfigs(config.subjectConfigs, sid)
        }
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
                        heartCount = o.optInt("heartCount", 0),
                        likedByUserIds = jsonArrayToList(o.optJSONArray("likedByUserIds")),
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
                put("heartCount", s.heartCount)
                put("likedByUserIds", JSONArray(s.likedByUserIds))
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
                        senderName = o.optString("senderName"),
                        senderRole = UserRole.valueOf(o.optString("senderRole", UserRole.ADMIN.name)),
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
                put("senderName", InputSanitizer.sanitize(m.senderName))
                put("senderRole", m.senderRole.name)
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
            arr.put(JSONObject().apply {
                put("id", ev.id)
                put("title", InputSanitizer.sanitize(ev.title))
                put("category", InputSanitizer.sanitize(ev.category))
                put("description", InputSanitizer.sanitize(ev.description))
                put("date", InputSanitizer.sanitize(ev.date))
                put("location", InputSanitizer.sanitize(ev.location))
                put("imageBase64", ev.imageBase64)
                put("authorName", InputSanitizer.sanitize(ev.authorName))
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
                            adminUsername = o.optString("adminUsername", "")
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
        val summary = RegisteredSchoolSummary(
            schoolId = config.schoolId,
            schoolName = config.schoolName,
            address = config.address,
            schoolStage = config.schoolStage.arabicLabel,
            schoolType = config.schoolType.arabicLabel,
            createdAt = config.createdAt.ifBlank { "2026-10-29" },
            isActive = config.isActive,
            adminUsername = resolvedAdminUser
        )
        if (existingIdx >= 0) {
            current[existingIdx] = summary
        } else {
            current.add(0, summary)
        }
        saveRegisteredSchools(current)
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

        editor.apply()
        return foundAny
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
