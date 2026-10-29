package com.example.data

import android.content.Context
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

class SchoolRepository(context: Context) {

    private val prefs = context.getSharedPreferences("madrasati_zummar_db_v1", Context.MODE_PRIVATE)

    fun isOnboardingSeen(): Boolean = prefs.getBoolean("onboarding_seen", false)

    fun setOnboardingSeen(seen: Boolean) {
        prefs.edit().putBoolean("onboarding_seen", seen).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("dark_mode", false)

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean("dark_mode", dark).apply()
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

    fun loadSchoolConfig(): SchoolConfig {
        val raw = prefs.getString("school_config", null) ?: return SchoolConfig()
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

            SchoolConfig(
                isSetupCompleted = obj.optBoolean("isSetupCompleted", false),
                schoolName = obj.optString("schoolName", "مدرستي النموذجية"),
                address = obj.optString("address", "الموصل - زمار"),
                schoolType = SchoolType.valueOf(obj.optString("schoolType", SchoolType.PRIVATE.name)),
                schoolStage = stage,
                registeredClasses = regClasses,
                registeredSections = regSections,
                registeredSubjects = regSubjects,
                logoBase64 = obj.optString("logoBase64", ""),
                academicYear = obj.optString("academicYear", "2026 - 2027"),
                phone = obj.optString("phone", "07512362567")
            )
        } catch (e: Exception) {
            SchoolConfig()
        }
    }

    fun saveSchoolConfig(config: SchoolConfig) {
        val obj = JSONObject().apply {
            put("isSetupCompleted", config.isSetupCompleted)
            put("schoolName", config.schoolName)
            put("address", config.address.ifBlank { "الموصل - زمار" })
            put("schoolType", config.schoolType.name)
            put("schoolStage", config.schoolStage.name)
            put("registeredClasses", JSONArray(config.registeredClasses))
            put("registeredSections", JSONArray(config.registeredSections))
            put("registeredSubjects", JSONArray(config.registeredSubjects))
            put("logoBase64", config.logoBase64)
            put("academicYear", config.academicYear)
            put("phone", config.phone)
        }
        prefs.edit().putString("school_config", obj.toString()).apply()
    }

    fun loadAccounts(): List<UserAccount> {
        val raw = prefs.getString("user_accounts", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<UserAccount>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
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
                        photoBase64 = o.optString("photoBase64", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAccounts(accounts: List<UserAccount>) {
        val arr = JSONArray()
        accounts.forEach { acc ->
            arr.put(JSONObject().apply {
                put("id", acc.id)
                put("username", acc.username)
                put("password", acc.password)
                put("fullName", acc.fullName)
                put("role", acc.role.name)
                put("isActive", acc.isActive)
                put("phone", acc.phone)
                put("specialty", acc.specialty)
                put("assignedClasses", acc.assignedClasses)
                put("linkedStudentId", acc.linkedStudentId)
                put("gradeClass", acc.gradeClass)
                put("sectionName", acc.sectionName)
                put("address", acc.address)
                put("photoBase64", acc.photoBase64)
            })
        }
        prefs.edit().putString("user_accounts", arr.toString()).apply()
    }

    fun loadStudents(): List<StudentRecord> {
        val raw = prefs.getString("students", null) ?: return defaultStudents()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<StudentRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
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
                        honorTitle = o.optString("honorTitle", "وسام التفوق الذهبي")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultStudents()
        }
    }

    fun saveStudents(students: List<StudentRecord>) {
        val arr = JSONArray()
        students.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("studentName", s.studentName)
                put("gradeClass", s.gradeClass)
                put("sectionName", s.sectionName)
                put("parentName", s.parentName)
                put("parentPhone", s.parentPhone)
                put("address", s.address)
                put("studentUsername", s.studentUsername)
                put("studentPassword", s.studentPassword)
                put("parentUsername", s.parentUsername)
                put("parentPassword", s.parentPassword)
                put("isActive", s.isActive)
                put("photoBase64", s.photoBase64)
                put("excellencePoints", s.excellencePoints)
                put("isOnHonorRoll", s.isOnHonorRoll)
                put("honorTitle", s.honorTitle)
            })
        }
        prefs.edit().putString("students", arr.toString()).apply()
    }

    fun loadAttendance(): List<AttendanceEntry> {
        val raw = prefs.getString("attendance", null) ?: return defaultAttendance()
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
                        note = o.optString("note")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultAttendance()
        }
    }

    fun saveAttendance(entries: List<AttendanceEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("studentId", e.studentId)
                put("studentName", e.studentName)
                put("gradeClass", e.gradeClass)
                put("sectionName", e.sectionName)
                put("date", e.date)
                put("status", e.status.name)
                put("note", e.note)
            })
        }
        prefs.edit().putString("attendance", arr.toString()).apply()
    }

    fun loadSchedules(): List<SchedulePeriod> {
        val raw = prefs.getString("schedules", null) ?: return defaultSchedules()
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
                        timeSlot = o.optString("timeSlot")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultSchedules()
        }
    }

    fun saveSchedules(schedules: List<SchedulePeriod>) {
        val arr = JSONArray()
        schedules.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("gradeClass", s.gradeClass)
                put("sectionName", s.sectionName)
                put("dayOfWeek", s.dayOfWeek)
                put("periodNumber", s.periodNumber)
                put("subjectName", s.subjectName)
                put("teacherName", s.teacherName)
                put("timeSlot", s.timeSlot)
            })
        }
        prefs.edit().putString("schedules", arr.toString()).apply()
    }

    fun loadHomeworks(): List<HomeworkItem> {
        val raw = prefs.getString("homeworks", null) ?: return defaultHomeworks()
        return try {
            val arr = JSONArray(raw)
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
                        createdAt = o.optString("createdAt")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultHomeworks()
        }
    }

    fun saveHomeworks(items: List<HomeworkItem>) {
        val arr = JSONArray()
        items.forEach { h ->
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("gradeClass", h.gradeClass)
                put("sectionName", h.sectionName)
                put("subjectName", h.subjectName)
                put("teacherName", h.teacherName)
                put("dailyPreparation", h.dailyPreparation)
                put("homeworkDetails", h.homeworkDetails)
                put("dueDate", h.dueDate)
                put("createdAt", h.createdAt)
            })
        }
        prefs.edit().putString("homeworks", arr.toString()).apply()
    }

    fun loadExamSchedules(): List<ExamScheduleItem> {
        val raw = prefs.getString("exam_schedules", null) ?: return defaultExamSchedules()
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
                        hallNumber = o.optString("hallNumber")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultExamSchedules()
        }
    }

    fun saveExamSchedules(exams: List<ExamScheduleItem>) {
        val arr = JSONArray()
        exams.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("gradeClass", e.gradeClass)
                put("subjectName", e.subjectName)
                put("examType", e.examType)
                put("examDate", e.examDate)
                put("dayName", e.dayName)
                put("timeSlot", e.timeSlot)
                put("topicsIncluded", e.topicsIncluded)
                put("hallNumber", e.hallNumber)
            })
        }
        prefs.edit().putString("exam_schedules", arr.toString()).apply()
    }

    fun loadGrades(): List<SubjectGrade> {
        val raw = prefs.getString("grades", null) ?: return defaultGrades()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SubjectGrade>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    SubjectGrade(
                        id = o.optString("id"),
                        studentId = o.optString("studentId"),
                        studentName = o.optString("studentName"),
                        gradeClass = o.optString("gradeClass"),
                        subjectName = o.optString("subjectName"),
                        dailyScore = o.optInt("dailyScore", 90),
                        month1Score = o.optInt("month1Score", 88),
                        month2Score = o.optInt("month2Score", 92),
                        midYearScore = o.optInt("midYearScore", 90),
                        finalExamScore = o.optInt("finalExamScore", 94)
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultGrades()
        }
    }

    fun saveGrades(grades: List<SubjectGrade>) {
        val arr = JSONArray()
        grades.forEach { g ->
            arr.put(JSONObject().apply {
                put("id", g.id)
                put("studentId", g.studentId)
                put("studentName", g.studentName)
                put("gradeClass", g.gradeClass)
                put("subjectName", g.subjectName)
                put("dailyScore", g.dailyScore)
                put("month1Score", g.month1Score)
                put("month2Score", g.month2Score)
                put("midYearScore", g.midYearScore)
                put("finalExamScore", g.finalExamScore)
            })
        }
        prefs.edit().putString("grades", arr.toString()).apply()
    }

    fun loadTuitions(): List<TuitionRecord> {
        val raw = prefs.getString("tuitions", null) ?: return defaultTuitions()
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
                        installments = installments
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultTuitions()
        }
    }

    fun saveTuitions(records: List<TuitionRecord>) {
        val arr = JSONArray()
        records.forEach { r ->
            val instArr = JSONArray()
            r.installments.forEach { inst ->
                instArr.put(JSONObject().apply {
                    put("id", inst.id)
                    put("amount", inst.amount)
                    put("paymentDate", inst.paymentDate)
                    put("receiptNumber", inst.receiptNumber)
                    put("note", inst.note)
                })
            }
            arr.put(JSONObject().apply {
                put("studentId", r.studentId)
                put("studentName", r.studentName)
                put("gradeClass", r.gradeClass)
                put("sectionName", r.sectionName)
                put("parentName", r.parentName)
                put("parentPhone", r.parentPhone)
                put("totalFee", r.totalFee)
                put("nextDueDate", r.nextDueDate)
                put("installments", instArr)
            })
        }
        prefs.edit().putString("tuitions", arr.toString()).apply()
    }

    fun loadReports(): List<ParentReport> {
        val raw = prefs.getString("reports", null) ?: return defaultReports()
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
                        createdAt = o.optString("createdAt")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultReports()
        }
    }

    fun saveReports(reports: List<ParentReport>) {
        val arr = JSONArray()
        reports.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("studentId", r.studentId)
                put("studentName", r.studentName)
                put("gradeClass", r.gradeClass)
                put("parentName", r.parentName)
                put("parentPhone", r.parentPhone)
                put("reportType", r.reportType.name)
                put("title", r.title)
                put("details", r.details)
                put("senderName", r.senderName)
                put("createdAt", r.createdAt)
            })
        }
        prefs.edit().putString("reports", arr.toString()).apply()
    }

    fun loadChatMessages(): List<ChatMessage> {
        val raw = prefs.getString("chat_messages", null) ?: return defaultChatMessages()
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
                        timestamp = o.optString("timestamp")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultChatMessages()
        }
    }

    fun saveChatMessages(messages: List<ChatMessage>) {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("studentId", m.studentId)
                put("studentName", m.studentName)
                put("senderName", m.senderName)
                put("senderRole", m.senderRole.name)
                put("messageText", m.messageText)
                put("timestamp", m.timestamp)
            })
        }
        prefs.edit().putString("chat_messages", arr.toString()).apply()
    }

    fun loadEvents(): List<SchoolEvent> {
        val raw = prefs.getString("events", null) ?: return defaultEvents()
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
                        authorName = o.optString("authorName", "إدارة المدرسة")
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultEvents()
        }
    }

    fun saveEvents(events: List<SchoolEvent>) {
        val arr = JSONArray()
        events.forEach { ev ->
            arr.put(JSONObject().apply {
                put("id", ev.id)
                put("title", ev.title)
                put("category", ev.category)
                put("description", ev.description)
                put("date", ev.date)
                put("location", ev.location)
                put("imageBase64", ev.imageBase64)
                put("authorName", ev.authorName)
            })
        }
        prefs.edit().putString("events", arr.toString()).apply()
    }

    fun loadNotifications(): List<AppNotification> {
        val raw = prefs.getString("notifications", null) ?: return defaultNotifications()
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
                        targetStudentId = o.optString("targetStudentId"),
                        createdAt = o.optString("createdAt"),
                        isRead = o.optBoolean("isRead", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            defaultNotifications()
        }
    }

    fun saveNotifications(notifications: List<AppNotification>) {
        val arr = JSONArray()
        notifications.forEach { n ->
            arr.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("body", n.body)
                put("type", n.type.name)
                put("targetStudentId", n.targetStudentId)
                put("createdAt", n.createdAt)
                put("isRead", n.isRead)
            })
        }
        prefs.edit().putString("notifications", arr.toString()).apply()
    }

    fun defaultStudents(): List<StudentRecord> = listOf(
        StudentRecord(
            id = "stu_1",
            studentName = "يوسف محمود الجبوري",
            gradeClass = "الأول المتوسط",
            sectionName = "أ",
            parentName = "محمود صالح الجبوري",
            parentPhone = "07512362567",
            address = "الموصل - زمار",
            studentUsername = "yousif1",
            studentPassword = "123",
            parentUsername = "parent_yousif",
            parentPassword = "123",
            isActive = true,
            excellencePoints = 98,
            isOnHonorRoll = true,
            honorTitle = "المركز الأول - وسام النخبة الذهبي"
        ),
        StudentRecord(
            id = "stu_2",
            studentName = "مريم خالد الشمري",
            gradeClass = "الأول المتوسط",
            sectionName = "أ",
            parentName = "خالد عبد الله الشمري",
            parentPhone = "07512362567",
            address = "الموصل - زمار",
            studentUsername = "maryam1",
            studentPassword = "123",
            parentUsername = "parent_maryam",
            parentPassword = "123",
            isActive = true,
            excellencePoints = 96,
            isOnHonorRoll = true,
            honorTitle = "المركز الثاني - وسام الإبداع العلمي"
        ),
        StudentRecord(
            id = "stu_3",
            studentName = "عبد الرحمن طارق الدليمي",
            gradeClass = "الثاني المتوسط",
            sectionName = "أ",
            parentName = "طارق ياسين الدليمي",
            parentPhone = "07512362567",
            address = "الموصل - زمار",
            studentUsername = "abdulrahman2",
            studentPassword = "123",
            parentUsername = "parent_abdulrahman",
            parentPassword = "123",
            isActive = true,
            excellencePoints = 95,
            isOnHonorRoll = true,
            honorTitle = "أول الصف الثاني المتوسط - وسام التميز"
        ),
        StudentRecord(
            id = "stu_4",
            studentName = "زهراء فراس العبيدي",
            gradeClass = "الثالث المتوسط",
            sectionName = "ب",
            parentName = "فراس حامد العبيدي",
            parentPhone = "07512362567",
            address = "الموصل - زمار",
            studentUsername = "zahraa3",
            studentPassword = "123",
            parentUsername = "parent_zahraa",
            parentPassword = "123",
            isActive = true,
            excellencePoints = 97,
            isOnHonorRoll = true,
            honorTitle = "المركز الأول - درع التفوق الوزاري"
        )
    )

    fun defaultTeachers(): List<UserAccount> = listOf(
        UserAccount(
            id = "tch_1",
            username = "teacher_math",
            password = "123",
            fullName = "أ. عمر قاسم الحديثي",
            role = UserRole.TEACHER,
            isActive = true,
            phone = "07512362567",
            specialty = "الرياضيات، الفيزياء",
            assignedClasses = "الأول المتوسط - أ، الثاني المتوسط - أ",
            address = "الموصل - زمار"
        ),
        UserAccount(
            id = "tch_2",
            username = "teacher_arabic",
            password = "123",
            fullName = "أ. سارة نبيل الموصلي",
            role = UserRole.TEACHER,
            isActive = true,
            phone = "07512362567",
            specialty = "اللغة العربية، التربية الإسلامية",
            assignedClasses = "الأول المتوسط - أ، الثالث المتوسط - ب",
            address = "الموصل - زمار"
        )
    )

    fun defaultAttendance(): List<AttendanceEntry> = listOf(
        AttendanceEntry(
            id = "att_1",
            studentId = "stu_1",
            studentName = "يوسف محمود الجبوري",
            gradeClass = "الأول المتوسط",
            sectionName = "أ",
            date = "2026-10-29",
            status = AttendanceStatus.PRESENT,
            note = "التزام مبكر وممتاز"
        ),
        AttendanceEntry(
            id = "att_2",
            studentId = "stu_2",
            studentName = "مريم خالد الشمري",
            gradeClass = "الأول المتوسط",
            sectionName = "أ",
            date = "2026-10-29",
            status = AttendanceStatus.PRESENT,
            note = "حضور منتظم"
        ),
        AttendanceEntry(
            id = "att_3",
            studentId = "stu_3",
            studentName = "عبد الرحمن طارق الدليمي",
            gradeClass = "الثاني المتوسط",
            sectionName = "أ",
            date = "2026-10-29",
            status = AttendanceStatus.ABSENT,
            note = "لم يحضر الحصة الأولى"
        ),
        AttendanceEntry(
            id = "att_4",
            studentId = "stu_4",
            studentName = "زهراء فراس العبيدي",
            gradeClass = "الثالث المتوسط",
            sectionName = "ب",
            date = "2026-10-29",
            status = AttendanceStatus.LATE,
            note = "تأخر 10 دقائق"
        )
    )

    fun defaultSchedules(): List<SchedulePeriod> = listOf(
        SchedulePeriod("sch_1", "الأول المتوسط", "أ", "الأحد", 1, "الرياضيات", "أ. عمر قاسم الحديثي", "08:00 - 08:45"),
        SchedulePeriod("sch_2", "الأول المتوسط", "أ", "الأحد", 2, "اللغة العربية", "أ. سارة نبيل الموصلي", "08:50 - 09:35"),
        SchedulePeriod("sch_3", "الأول المتوسط", "أ", "الأحد", 3, "اللغة الإنكليزية", "أ. زياد طلال", "09:40 - 10:25"),
        SchedulePeriod("sch_4", "الأول المتوسط", "أ", "الأحد", 4, "الفيزياء", "أ. عمر قاسم الحديثي", "10:40 - 11:25"),
        SchedulePeriod("sch_5", "الأول المتوسط", "أ", "الاثنين", 1, "التربية الإسلامية", "أ. سارة نبيل الموصلي", "08:00 - 08:45"),
        SchedulePeriod("sch_6", "الأول المتوسط", "أ", "الاثنين", 2, "الرياضيات", "أ. عمر قاسم الحديثي", "08:50 - 09:35"),
        SchedulePeriod("sch_7", "الأول المتوسط", "أ", "الاثنين", 3, "الاجتماعيات", "أ. مهند الحمداني", "09:40 - 10:25"),
        SchedulePeriod("sch_8", "الثاني المتوسط", "أ", "الأحد", 1, "الفيزياء", "أ. عمر قاسم الحديثي", "08:00 - 08:45"),
        SchedulePeriod("sch_9", "الثالث المتوسط", "ب", "الأحد", 1, "اللغة العربية", "أ. سارة نبيل الموصلي", "08:00 - 08:45"),
        SchedulePeriod("sch_10", "الأول المتوسط", "أ", "الخميس", 1, "الحاسوب", "أ. أحمد أبو رسلان", "08:00 - 08:45")
    )

    fun defaultHomeworks(): List<HomeworkItem> = listOf(
        HomeworkItem(
            id = "hw_1",
            gradeClass = "الأول المتوسط",
            sectionName = "أ",
            subjectName = "الرياضيات",
            teacherName = "أ. عمر قاسم الحديثي",
            dailyPreparation = "الفصل الثاني: الأعداد النسبية والعمليات عليها (ص 34 - 38)",
            homeworkDetails = "حل تمارين تأكد من فهمك صفحة 39 في الدفتر المدرسي (الأسئلة 1 إلى 8)",
            dueDate = "2026-10-30",
            createdAt = "اليوم 09:15 ص"
        ),
        HomeworkItem(
            id = "hw_2",
            gradeClass = "الأول المتوسط",
            sectionName = "أ",
            subjectName = "اللغة العربية",
            teacherName = "أ. سارة نبيل الموصلي",
            dailyPreparation = "قواعد اللغة العربية: أقسام الكلام وعلامات الفعل المضارع",
            homeworkDetails = "كتابة خمس جمل مفيدة مع إعرابها إعراباً مفصلاً في دفتر القواعد",
            dueDate = "2026-10-30",
            createdAt = "اليوم 10:00 ص"
        ),
        HomeworkItem(
            id = "hw_3",
            gradeClass = "الثاني المتوسط",
            sectionName = "أ",
            subjectName = "الفيزياء",
            teacherName = "أ. عمر قاسم الحديثي",
            dailyPreparation = "الحركة الخطية والسرعة والتعجيل",
            homeworkDetails = "مراجعة القوانين وحل المسائل الحسابية في نهاية الفصل الأول",
            dueDate = "2026-10-31",
            createdAt = "أمس 11:30 ص"
        )
    )

    fun defaultExamSchedules(): List<ExamScheduleItem> = listOf(
        ExamScheduleItem(
            id = "ex_1",
            gradeClass = "الأول المتوسط",
            subjectName = "الرياضيات",
            examType = "امتحان الشهر الأول",
            examDate = "2026-11-03",
            dayName = "الثلاثاء",
            timeSlot = "08:30 صباحاً",
            topicsIncluded = "الفصل الأول والفصل الثاني بالكامل",
            hallNumber = "قاعة الفراهيدي (1)"
        ),
        ExamScheduleItem(
            id = "ex_2",
            gradeClass = "الأول المتوسط",
            subjectName = "اللغة العربية",
            examType = "امتحان الشهر الأول",
            examDate = "2026-11-05",
            dayName = "الخميس",
            timeSlot = "08:30 صباحاً",
            topicsIncluded = "المبتدأ والخبر + كان وأخواتها + الإملاء",
            hallNumber = "قاعة الفراهيدي (1)"
        ),
        ExamScheduleItem(
            id = "ex_3",
            gradeClass = "الثاني المتوسط",
            subjectName = "الفيزياء",
            examType = "امتحان الشهر الأول",
            examDate = "2026-11-04",
            dayName = "الأربعاء",
            timeSlot = "09:30 صباحاً",
            topicsIncluded = "الفصل الأول والفصل الثاني",
            hallNumber = "قاعة ابن الهيثم (2)"
        )
    )

    fun defaultGrades(): List<SubjectGrade> = listOf(
        SubjectGrade("grd_1", "stu_1", "يوسف محمود الجبوري", "الأول المتوسط", "الرياضيات", 98, 96, 99, 97, 99),
        SubjectGrade("grd_2", "stu_1", "يوسف محمود الجبوري", "الأول المتوسط", "اللغة العربية", 95, 94, 96, 95, 97),
        SubjectGrade("grd_3", "stu_1", "يوسف محمود الجبوري", "الأول المتوسط", "اللغة الإنكليزية", 97, 95, 98, 96, 98),
        SubjectGrade("grd_4", "stu_1", "يوسف محمود الجبوري", "الأول المتوسط", "الفيزياء", 99, 97, 98, 99, 100),
        SubjectGrade("grd_5", "stu_2", "مريم خالد الشمري", "الأول المتوسط", "الرياضيات", 95, 94, 97, 96, 96),
        SubjectGrade("grd_6", "stu_2", "مريم خالد الشمري", "الأول المتوسط", "اللغة العربية", 98, 96, 97, 98, 99),
        SubjectGrade("grd_7", "stu_3", "عبد الرحمن طارق الدليمي", "الثاني المتوسط", "الرياضيات", 92, 90, 94, 93, 95),
        SubjectGrade("grd_8", "stu_4", "زهراء فراس العبيدي", "الثالث المتوسط", "اللغة العربية", 97, 96, 98, 97, 98)
    )

    fun defaultTuitions(): List<TuitionRecord> = listOf(
        TuitionRecord(
            studentId = "stu_1",
            studentName = "يوسف محمود الجبوري",
            gradeClass = "الأول المتوسط",
            sectionName = "أ",
            parentName = "محمود صالح الجبوري",
            parentPhone = "07512362567",
            totalFee = 1500000L,
            nextDueDate = "2026-11-15",
            installments = listOf(
                TuitionInstallment("inst_1", 500000L, "2026-09-15", "REC-1001", "الدفعة الأولى عند التسجيل"),
                TuitionInstallment("inst_2", 500000L, "2026-10-15", "REC-1042", "الدفعة الثانية لشهر تشرين الأول")
            )
        ),
        TuitionRecord(
            studentId = "stu_2",
            studentName = "مريم خالد الشمري",
            gradeClass = "الأول المتوسط",
            sectionName = "أ",
            parentName = "خالد عبد الله الشمري",
            parentPhone = "07512362567",
            totalFee = 1500000L,
            nextDueDate = "2026-11-10",
            installments = listOf(
                TuitionInstallment("inst_3", 750000L, "2026-09-20", "REC-1015", "دفعة القسط الأول")
            )
        ),
        TuitionRecord(
            studentId = "stu_3",
            studentName = "عبد الرحمن طارق الدليمي",
            gradeClass = "الثاني المتوسط",
            sectionName = "أ",
            parentName = "طارق ياسين الدليمي",
            parentPhone = "07512362567",
            totalFee = 1600000L,
            nextDueDate = "2026-11-05",
            installments = listOf(
                TuitionInstallment("inst_4", 600000L, "2026-09-18", "REC-1009", "دفعة القسط التأسيسي")
            )
        ),
        TuitionRecord(
            studentId = "stu_4",
            studentName = "زهراء فراس العبيدي",
            gradeClass = "الثالث المتوسط",
            sectionName = "ب",
            parentName = "فراس حامد العبيدي",
            parentPhone = "07512362567",
            totalFee = 1750000L,
            nextDueDate = "2026-12-01",
            installments = listOf(
                TuitionInstallment("inst_5", 1750000L, "2026-09-10", "REC-1002", "تسديد كامل القسط السنوي مع خصم التفوق")
            )
        )
    )

    fun defaultReports(): List<ParentReport> = listOf(
        ParentReport(
            id = "rep_1",
            studentId = "stu_1",
            studentName = "يوسف محمود الجبوري",
            gradeClass = "الأول المتوسط",
            parentName = "محمود صالح الجبوري",
            parentPhone = "07512362567",
            reportType = ReportType.APPRECIATION,
            title = "كتاب شكر وتقدير لتفوق الطالب العلمي",
            details = "تتقدم إدارة المدرسة بجزيل الشكر لولي أمر الطالب يوسف محمود الجبوري لحصوله على الدرجة الكاملة في الاختبار الأسبوعي لمادة الرياضيات والعلوم.",
            senderName = "الإدارة العامة",
            createdAt = "اليوم 10:30 ص"
        ),
        ParentReport(
            id = "rep_2",
            studentId = "stu_3",
            studentName = "عبد الرحمن طارق الدليمي",
            gradeClass = "الثاني المتوسط",
            parentName = "طارق ياسين الدليمي",
            parentPhone = "07512362567",
            reportType = ReportType.ACADEMIC_NOTE,
            title = "متابعة تحضير مادة الفيزياء",
            details = "نرجو من ولي الأمر الكريم متابعة حل الواجبات البيتية الخاصة بالفصل الأول وتحضير الدرس القادم.",
            senderName = "أ. عمر قاسم الحديثي",
            createdAt = "أمس 12:15 م"
        )
    )

    fun defaultChatMessages(): List<ChatMessage> = listOf(
        ChatMessage(
            id = "msg_1",
            studentId = "stu_1",
            studentName = "يوسف محمود الجبوري",
            senderName = "الإدارة العامة",
            senderRole = UserRole.ADMIN,
            messageText = "السلام عليكم ورحمة الله، نرحب بكم في نافذة التواصل المباشر لمنصة مدرستي في الموصل - زمار.",
            timestamp = "09:00 ص"
        ),
        ChatMessage(
            id = "msg_2",
            studentId = "stu_1",
            studentName = "يوسف محمود الجبوري",
            senderName = "محمود صالح الجبوري (ولي أمر)",
            senderRole = UserRole.PARENT,
            messageText = "وعليكم السلام ورحمة الله، بارك الله بجهودكم الكريمة في متابعة أبنائنا الطلبة.",
            timestamp = "09:12 ص"
        )
    )

    fun defaultEvents(): List<SchoolEvent> = listOf(
        SchoolEvent(
            id = "ev_1",
            title = "مهرجان التفوق السنوي وتكريم أوائل الطلبة في زمار",
            category = "احتفالية وتكريم",
            description = "أقامت إدارة المدرسة حفل تكريم الطلبة الأوائل والمجتهدين بحضور أولياء الأمور الكرام وتوزيع الأوسمة الذهبية وشهادات التقدير.",
            date = "2026-10-28",
            location = "الموصل - زمار (القاعة الكبرى)",
            authorName = "الإدارة العامة"
        ),
        SchoolEvent(
            id = "ev_2",
            title = "انطلاق المسابقة العلمية والمنهجية بين الشعب الدراسية",
            category = "نشاط علمي",
            description = "انطلقت اليوم فعاليات المسابقة العلمية في مواد الرياضيات والعلوم واللغة العربية لتعزيز روح التنافس الإيجابي بين الطلبة.",
            date = "2026-10-25",
            location = "الموصل - زمار (مختبر العلوم)",
            authorName = "اللجنة العلمية"
        ),
        SchoolEvent(
            id = "ev_3",
            title = "حملة التشجير البيئي وتجميل حدائق المدرسة",
            category = "نشاط لا صفي",
            description = "بمشاركة الكادر التدريسي والطلبة المتميزين تم غرس أكثر من 100 شجرة وزهرة في باحات المدرسة.",
            date = "2026-10-20",
            location = "الموصل - زمار",
            authorName = "لجنة النشاط المدرسي"
        )
    )

    fun defaultNotifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "notif_1",
            title = "تنبيه غياب يومي",
            body = "تم تسجيل غياب الطالب عبد الرحمن طارق الدليمي لهذا اليوم 2026-10-29. يرجى مراجعة الإدارة.",
            type = NotificationType.ATTENDANCE,
            targetStudentId = "stu_3",
            createdAt = "منذ 15 دقيقة",
            isRead = false
        ),
        AppNotification(
            id = "notif_2",
            title = "واجب بيتي جديد في الرياضيات",
            body = "نشر أ. عمر قاسم الحديثي تحضير وواجب مادة الرياضيات للصف الأول المتوسط (شعبة أ).",
            type = NotificationType.HOMEWORK,
            targetStudentId = "stu_1",
            createdAt = "منذ ساعة",
            isRead = false
        ),
        AppNotification(
            id = "notif_3",
            title = "تحديث كشف الدرجات الشهري",
            body = "تم رصد درجات الشهر الأول والسعي السنوي وتحديث لوحة الشرف الذهبية.",
            type = NotificationType.GRADES,
            targetStudentId = "",
            createdAt = "منذ ساعتين",
            isRead = false
        ),
        AppNotification(
            id = "notif_4",
            title = "إشعار استحقاق القسط الدراسي",
            body = "تذكير ودي بموعد القسط القادم بتاريخ 2026-11-15 مع إمكانية استلام وصل الدفع عبر واتساب.",
            type = NotificationType.TUITION,
            targetStudentId = "stu_1",
            createdAt = "اليوم صباحاً",
            isRead = false
        )
    )
}
