package com.example.staffattendance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.model.FirebaseConfigConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * إعدادات النطاق الجغرافي للمدرسة (GPS Zone) الخاصة ببصمة الكوادر التدريسية والإدارية
 * المسار السحابي: schools/{schoolCode}/school_geofence_config.json
 */
data class SchoolGeofenceConfig(
    val isEnabled: Boolean = true,
    val locationLabel: String = "مقر المدرسة الرئيسي",
    val latitude: Double = 36.3489,
    val longitude: Double = 43.1577,
    val radiusMeters: Int = 120,
    val schoolWifiSsid: String = "",
    val workStartTime: String = "07:30",
    val lateAfterTime: String = "08:00",
    val workEndTime: String = "14:30",
    val updatedAt: Long = 0L
) {
    val isLocationConfigured: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    fun toJson(): JSONObject = JSONObject().apply {
        put("isEnabled", isEnabled)
        put("locationLabel", locationLabel)
        put("latitude", latitude)
        put("longitude", longitude)
        put("radiusMeters", radiusMeters.coerceIn(50, 300))
        put("schoolWifiSsid", schoolWifiSsid.trim())
        put("workStartTime", workStartTime.ifBlank { "07:30" })
        put("lateAfterTime", lateAfterTime.ifBlank { "08:00" })
        put("workEndTime", workEndTime.ifBlank { "14:30" })
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject?): SchoolGeofenceConfig {
            if (obj == null) return SchoolGeofenceConfig()
            return SchoolGeofenceConfig(
                isEnabled = obj.optBoolean("isEnabled", true),
                locationLabel = obj.optString("locationLabel", "مقر المدرسة الرئيسي")
                    .ifBlank { "مقر المدرسة الرئيسي" },
                latitude = obj.optDouble("latitude", 36.3489),
                longitude = obj.optDouble("longitude", 43.1577),
                radiusMeters = obj.optInt("radiusMeters", 120).coerceIn(50, 300),
                schoolWifiSsid = obj.optString("schoolWifiSsid", "").trim(),
                workStartTime = obj.optString("workStartTime", "07:30").ifBlank { "07:30" },
                lateAfterTime = obj.optString("lateAfterTime", "08:00").ifBlank { "08:00" },
                workEndTime = obj.optString("workEndTime", "14:30").ifBlank { "14:30" },
                updatedAt = obj.optLong("updatedAt", 0L)
            )
        }
    }
}

/**
 * سجل البصمة والحضور اليومي لعضو الكادر
 * المسار السحابي: schools/{schoolCode}/staff_attendance/{yyyy-MM-dd}/{staffId}.json
 */
data class StaffAttendanceRecord(
    val staffId: String,
    val staffName: String,
    val role: String = "معلم",
    val dateIso: String = StaffAttendanceDateUtils.todayIso(),
    val status: String = "present", // "present" | "late" | "absent" | "leave"
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val checkInTimestamp: Long = 0L,
    val checkOutTimestamp: Long = 0L,
    val recordMethod: String = "electronic_fingerprint_gps", // "electronic_fingerprint_gps" | "manual_admin"
    val distanceMeters: Int = 0,
    val adminNotes: String = "",
    val isManualOverride: Boolean = false,
    val updatedBy: String = "",
    val updatedAt: Long = 0L
) {
    val hasCheckedIn: Boolean
        get() = checkInTime.isNotBlank() || checkInTimestamp > 0L

    val hasCheckedOut: Boolean
        get() = checkOutTime.isNotBlank() || checkOutTimestamp > 0L

    fun toJson(): JSONObject = JSONObject().apply {
        put("staffId", staffId)
        put("staffName", staffName)
        put("role", role)
        put("dateIso", dateIso)
        put("status", status)
        put("checkInTime", checkInTime)
        put("checkOutTime", checkOutTime)
        put("checkInTimestamp", checkInTimestamp)
        put("checkOutTimestamp", checkOutTimestamp)
        put("recordMethod", recordMethod)
        put("distanceMeters", distanceMeters)
        put("adminNotes", adminNotes)
        put("isManualOverride", isManualOverride)
        put("updatedBy", updatedBy)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject?, fallbackDate: String = "", fallbackStaffId: String = ""): StaffAttendanceRecord? {
            if (obj == null) return null
            val sid = obj.optString("staffId", fallbackStaffId).ifBlank { fallbackStaffId }
            if (sid.isBlank()) return null
            return StaffAttendanceRecord(
                staffId = sid,
                staffName = obj.optString("staffName", ""),
                role = obj.optString("role", "معلم"),
                dateIso = obj.optString("dateIso", fallbackDate).ifBlank { fallbackDate },
                status = obj.optString("status", "present").ifBlank { "present" },
                checkInTime = obj.optString("checkInTime", ""),
                checkOutTime = obj.optString("checkOutTime", ""),
                checkInTimestamp = obj.optLong("checkInTimestamp", 0L),
                checkOutTimestamp = obj.optLong("checkOutTimestamp", 0L),
                recordMethod = obj.optString("recordMethod", "electronic_fingerprint_gps")
                    .ifBlank { "electronic_fingerprint_gps" },
                distanceMeters = obj.optInt("distanceMeters", 0),
                adminNotes = obj.optString("adminNotes", ""),
                isManualOverride = obj.optBoolean("isManualOverride", false),
                updatedBy = obj.optString("updatedBy", ""),
                updatedAt = obj.optLong("updatedAt", 0L)
            )
        }
    }
}

/**
 * طلب إجازة الكادر التدريسي والإداري
 * المسار السحابي: schools/{schoolCode}/staff_leaves/{leaveId}.json
 */
data class StaffLeaveRequest(
    val leaveId: String = "lv_${UUID.randomUUID().toString().take(8)}",
    val staffId: String,
    val staffName: String,
    val leaveType: String = "اعتيادية", // "اعتيادية" | "مرضية" | "زمنية"
    val startDate: String = StaffAttendanceDateUtils.todayIso(),
    val daysCount: Int = 1,
    val reason: String = "",
    val status: String = "pending", // "pending" | "approved" | "rejected"
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("leaveId", leaveId)
        put("staffId", staffId)
        put("staffName", staffName)
        put("leaveType", leaveType)
        put("startDate", startDate)
        put("daysCount", daysCount.coerceAtLeast(1))
        put("reason", reason)
        put("status", status)
        put("adminNote", adminNote)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(obj: JSONObject?, fallbackId: String = ""): StaffLeaveRequest? {
            if (obj == null) return null
            val lid = obj.optString("leaveId", fallbackId).ifBlank { fallbackId }
            val sid = obj.optString("staffId", "")
            if (lid.isBlank() || sid.isBlank()) return null
            return StaffLeaveRequest(
                leaveId = lid,
                staffId = sid,
                staffName = obj.optString("staffName", ""),
                leaveType = obj.optString("leaveType", "اعتيادية").ifBlank { "اعتيادية" },
                startDate = obj.optString("startDate", StaffAttendanceDateUtils.todayIso()),
                daysCount = obj.optInt("daysCount", 1).coerceAtLeast(1),
                reason = obj.optString("reason", ""),
                status = obj.optString("status", "pending").ifBlank { "pending" },
                adminNote = obj.optString("adminNote", ""),
                createdAt = obj.optLong("createdAt", 0L)
            )
        }
    }
}

data class ZoneProbeResult(
    val isLoading: Boolean = false,
    val isHoliday: Boolean = false,
    val holidayLabel: String = "",
    val isGeofenceEnabled: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val isGpsDisabled: Boolean = false,
    val isMockLocationDetected: Boolean = false,
    val insideZone: Boolean = false,
    val matchedByWifi: Boolean = false,
    val connectedWifiSsid: String = "",
    val distanceMeters: Int? = null,
    val allowedRadiusMeters: Int = 120,
    val deviceLatitude: Double? = null,
    val deviceLongitude: Double? = null,
    val statusHeadline: String = "جارٍ التحقق من نطاق المدرسة...",
    val statusDetail: String = ""
)

data class FingerprintActionOutcome(
    val success: Boolean,
    val isCheckOut: Boolean = false,
    val title: String,
    val message: String,
    val updatedRecord: StaffAttendanceRecord? = null,
    val requiresPermission: Boolean = false,
    val isMockDetected: Boolean = false
)

object StaffAttendanceDateUtils {
    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun currentMonthIso(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    fun nowTimeHHmm(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())

    fun isWeekend(dateIso: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = sdf.parse(dateIso) ?: return false
            val cal = Calendar.getInstance().apply { time = parsed }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            dow == Calendar.FRIDAY || dow == Calendar.SATURDAY
        } catch (_: Exception) {
            false
        }
    }

    fun getHolidayNoticeIfAny(dateIso: String, schoolHolidayLabel: String?): String? {
        if (!schoolHolidayLabel.isNullOrBlank()) {
            return "اليوم عطلة رسمية ($schoolHolidayLabel) • لا يتطلب تسجيل بصمة"
        }
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = sdf.parse(dateIso) ?: return null
            val cal = Calendar.getInstance().apply { time = parsed }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.FRIDAY -> "اليوم عطلة رسمية (الجمعة) • لا يتطلب تسجيل بصمة"
                Calendar.SATURDAY -> "اليوم عطلة رسمية (السبت) • لا يتطلب تسجيل بصمة"
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * يحوّل وقت بصيغة HH:mm إلى دقائق منذ منتصف الليل للمقارنة الدقيقة.
     */
    fun timeToMinutes(hhmm: String, fallbackMinutes: Int): Int {
        val parts = hhmm.trim().split(":")
        if (parts.size < 2) return fallbackMinutes
        val h = parts[0].toIntOrNull() ?: return fallbackMinutes
        val m = parts[1].toIntOrNull() ?: return fallbackMinutes
        return (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
    }

    /**
     * يولّد قائمة تواريخ متتابعة (yyyy-MM-dd) ابتداءً من startDate بعدد daysCount.
     */
    fun expandDateRange(startDateIso: String, daysCount: Int): List<String> {
        val safeDays = daysCount.coerceIn(1, 60)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val baseDate = try {
            sdf.parse(startDateIso)
        } catch (_: Exception) {
            null
        } ?: Date()
        val cal = Calendar.getInstance().apply { time = baseDate }
        return buildList {
            for (i in 0 until safeDays) {
                add(sdf.format(cal.time))
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }

    fun shiftMonth(yearMonth: String, deltaMonths: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance().apply {
            time = try {
                sdf.parse(yearMonth) ?: Date()
            } catch (_: Exception) {
                Date()
            }
            add(Calendar.MONTH, deltaMonths)
        }
        return sdf.format(cal.time)
    }

    fun formatArabicMonthTitle(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size < 2) return yearMonth
        val year = parts[0]
        val monthName = when (parts[1].toIntOrNull()) {
            1 -> "كانون الثاني"
            2 -> "شباط"
            3 -> "آذار"
            4 -> "نيسان"
            5 -> "أيار"
            6 -> "حزيران"
            7 -> "تموز"
            8 -> "آب"
            9 -> "أيلول"
            10 -> "تشرين الأول"
            11 -> "تشرين الثاني"
            12 -> "كانون الأول"
            else -> parts[1]
        }
        return "$monthName $year"
    }
}

/**
 * مخزن ومزامن سحابي فوري لبصمة الكوادر (Firebase RTDB REST + SharedPreferences Cache)
 * مع StateFlow حي لتحديث شارة الحالة اليومية على الشاشة الرئيسية فورياً.
 */
object StaffAttendanceCloudStore {
    private const val PREFS_NAME = "madrasati_staff_attendance_cache_v1"
    private const val CONNECT_TIMEOUT_MS = 7000
    private const val READ_TIMEOUT_MS = 9000

    private val _todayRecordsByStaffId = MutableStateFlow<Map<String, StaffAttendanceRecord>>(emptyMap())
    val todayRecordsByStaffId: StateFlow<Map<String, StaffAttendanceRecord>> = _todayRecordsByStaffId.asStateFlow()

    private val _geofenceConfigState = MutableStateFlow(SchoolGeofenceConfig())
    val geofenceConfigState: StateFlow<SchoolGeofenceConfig> = _geofenceConfigState.asStateFlow()

    private fun sanitizeSegment(raw: String, fallback: String): String {
        val cleaned = raw.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        return cleaned.ifBlank { fallback }
    }

    private fun buildUrl(schoolCode: String, relativePath: String): String {
        val base = FirebaseConfigConstants.DATABASE_URL.trimEnd('/')
        val safeSchool = URLEncoder.encode(sanitizeSegment(schoolCode, "default_school"), "UTF-8")
        return "$base/schools/$safeSchool/$relativePath"
    }

    private fun httpGetJson(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPutJson(urlString: String, jsonBody: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun cacheKey(schoolCode: String, suffix: String): String =
        "${sanitizeSegment(schoolCode, "default_school")}_$suffix"

    // -------------------------------------------------------------------------
    // 1. School Geofence Config
    // -------------------------------------------------------------------------
    suspend fun fetchGeofenceConfig(context: Context, schoolCode: String): SchoolGeofenceConfig =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = cacheKey(schoolCode, "geofence_config")
            val cached = prefs.getString(key, null)?.let {
                try {
                    SchoolGeofenceConfig.fromJson(JSONObject(it))
                } catch (_: Exception) {
                    null
                }
            }

            val raw = httpGetJson(buildUrl(schoolCode, "school_geofence_config.json"))
            val resolved = if (!raw.isNullOrBlank() && raw != "null") {
                try {
                    val parsed = SchoolGeofenceConfig.fromJson(JSONObject(raw))
                    prefs.edit().putString(key, parsed.toJson().toString()).apply()
                    parsed
                } catch (_: Exception) {
                    cached ?: SchoolGeofenceConfig()
                }
            } else {
                cached ?: SchoolGeofenceConfig()
            }
            _geofenceConfigState.value = resolved
            resolved
        }

    suspend fun saveGeofenceConfig(
        context: Context,
        schoolCode: String,
        config: SchoolGeofenceConfig
    ): Boolean = withContext(Dispatchers.IO) {
        val stamped = config.copy(updatedAt = System.currentTimeMillis())
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(cacheKey(schoolCode, "geofence_config"), stamped.toJson().toString()).apply()
        _geofenceConfigState.value = stamped
        httpPutJson(buildUrl(schoolCode, "school_geofence_config.json"), stamped.toJson().toString())
    }

    // -------------------------------------------------------------------------
    // 2. Daily Staff Attendance Records
    // -------------------------------------------------------------------------
    suspend fun fetchDailyAttendance(
        context: Context,
        schoolCode: String,
        dateIso: String
    ): Map<String, StaffAttendanceRecord> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(schoolCode, "daily_$dateIso")
        val raw = httpGetJson(buildUrl(schoolCode, "staff_attendance/${sanitizeSegment(dateIso, dateIso)}.json"))
        val effectiveJsonStr = if (!raw.isNullOrBlank() && raw != "null") {
            prefs.edit().putString(key, raw).apply()
            raw
        } else {
            prefs.getString(key, null)
        }
        val result = parseDailyRecordsMap(effectiveJsonStr, dateIso)
        if (dateIso == StaffAttendanceDateUtils.todayIso()) {
            _todayRecordsByStaffId.value = result
        }
        result
    }

    suspend fun fetchStaffRecordForDate(
        context: Context,
        schoolCode: String,
        dateIso: String,
        staffId: String
    ): StaffAttendanceRecord? = withContext(Dispatchers.IO) {
        val safeDate = sanitizeSegment(dateIso, dateIso)
        val safeStaff = sanitizeSegment(staffId, "staff")
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(schoolCode, "rec_${safeDate}_$safeStaff")

        val raw = httpGetJson(buildUrl(schoolCode, "staff_attendance/$safeDate/$safeStaff.json"))
        val record = if (!raw.isNullOrBlank() && raw != "null") {
            try {
                val parsed = StaffAttendanceRecord.fromJson(JSONObject(raw), dateIso, staffId)
                if (parsed != null) {
                    prefs.edit().putString(key, parsed.toJson().toString()).apply()
                }
                parsed
            } catch (_: Exception) {
                null
            }
        } else if (raw == "null") {
            null
        } else {
            prefs.getString(key, null)?.let {
                try {
                    StaffAttendanceRecord.fromJson(JSONObject(it), dateIso, staffId)
                } catch (_: Exception) {
                    null
                }
            }
        }

        if (dateIso == StaffAttendanceDateUtils.todayIso()) {
            val current = _todayRecordsByStaffId.value.toMutableMap()
            if (record != null) {
                current[staffId] = record
                current[safeStaff] = record
            }
            _todayRecordsByStaffId.value = current
        }
        record
    }

    suspend fun saveStaffAttendanceRecord(
        context: Context,
        schoolCode: String,
        record: StaffAttendanceRecord
    ): Boolean = withContext(Dispatchers.IO) {
        val safeDate = sanitizeSegment(record.dateIso, StaffAttendanceDateUtils.todayIso())
        val safeStaff = sanitizeSegment(record.staffId, "staff")
        val stamped = record.copy(updatedAt = System.currentTimeMillis())
        val jsonStr = stamped.toJson().toString()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(cacheKey(schoolCode, "rec_${safeDate}_$safeStaff"), jsonStr).apply()

        // تحديث ذاكرة اليوم فوراً لتنعكس على الشاشة الرئيسية وشارة SmartWelcomeProfileAvatar
        if (record.dateIso == StaffAttendanceDateUtils.todayIso()) {
            val updatedMap = _todayRecordsByStaffId.value.toMutableMap()
            updatedMap[record.staffId] = stamped
            updatedMap[safeStaff] = stamped
            _todayRecordsByStaffId.value = updatedMap
        }

        // تحديث الكاش اليومي المجمّع أيضاً
        val dailyKey = cacheKey(schoolCode, "daily_${record.dateIso}")
        val existingDailyObj = try {
            JSONObject(prefs.getString(dailyKey, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        existingDailyObj.put(safeStaff, stamped.toJson())
        prefs.edit().putString(dailyKey, existingDailyObj.toString()).apply()

        httpPutJson(
            buildUrl(schoolCode, "staff_attendance/$safeDate/$safeStaff.json"),
            jsonStr
        )
    }

    /**
     * يجلب جميع سجلات الحضور للشهر المحدد (مثلاً "2026-11") من شجرة staff_attendance.json
     */
    suspend fun fetchMonthlyAttendance(
        context: Context,
        schoolCode: String,
        yearMonth: String
    ): List<StaffAttendanceRecord> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cacheAllKey = cacheKey(schoolCode, "all_attendance_snapshot")
        val raw = httpGetJson(buildUrl(schoolCode, "staff_attendance.json"))
        val effectiveJson = if (!raw.isNullOrBlank() && raw != "null") {
            prefs.edit().putString(cacheAllKey, raw).apply()
            raw
        } else {
            prefs.getString(cacheAllKey, null)
        }
        if (effectiveJson.isNullOrBlank() || effectiveJson == "null") return@withContext emptyList()

        val list = mutableListOf<StaffAttendanceRecord>()
        try {
            val root = JSONObject(effectiveJson)
            val dateKeys = root.keys()
            while (dateKeys.hasNext()) {
                val dateKey = dateKeys.next()
                if (yearMonth.isNotBlank() && !dateKey.startsWith(yearMonth)) continue
                val dayObj = root.optJSONObject(dateKey) ?: continue
                val staffKeys = dayObj.keys()
                while (staffKeys.hasNext()) {
                    val sKey = staffKeys.next()
                    val rec = StaffAttendanceRecord.fromJson(dayObj.optJSONObject(sKey), dateKey, sKey)
                    if (rec != null) list.add(rec)
                }
            }
        } catch (_: Exception) {
        }
        list.sortedWith(compareByDescending<StaffAttendanceRecord> { it.dateIso }.thenBy { it.staffName })
    }

    // -------------------------------------------------------------------------
    // 3. Staff Leave Requests
    // -------------------------------------------------------------------------
    suspend fun fetchLeaveRequests(
        context: Context,
        schoolCode: String
    ): List<StaffLeaveRequest> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(schoolCode, "staff_leaves_all")
        val raw = httpGetJson(buildUrl(schoolCode, "staff_leaves.json"))
        val effectiveJson = if (!raw.isNullOrBlank() && raw != "null") {
            prefs.edit().putString(key, raw).apply()
            raw
        } else {
            prefs.getString(key, null)
        }
        if (effectiveJson.isNullOrBlank() || effectiveJson == "null") return@withContext emptyList()

        val result = mutableListOf<StaffLeaveRequest>()
        try {
            val root = JSONObject(effectiveJson)
            val keys = root.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val req = StaffLeaveRequest.fromJson(root.optJSONObject(id), id)
                if (req != null) result.add(req)
            }
        } catch (_: Exception) {
        }
        result.sortedByDescending { it.createdAt }
    }

    suspend fun submitLeaveRequest(
        context: Context,
        schoolCode: String,
        request: StaffLeaveRequest
    ): Boolean = withContext(Dispatchers.IO) {
        val safeId = sanitizeSegment(request.leaveId, "lv_${System.currentTimeMillis()}")
        val finalReq = request.copy(leaveId = safeId, createdAt = System.currentTimeMillis())
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(schoolCode, "staff_leaves_all")
        val existingObj = try {
            JSONObject(prefs.getString(key, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        existingObj.put(safeId, finalReq.toJson())
        prefs.edit().putString(key, existingObj.toString()).apply()

        httpPutJson(buildUrl(schoolCode, "staff_leaves/$safeId.json"), finalReq.toJson().toString())
    }

    suspend fun decideLeaveRequest(
        context: Context,
        schoolCode: String,
        request: StaffLeaveRequest,
        approved: Boolean,
        adminNote: String,
        adminName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val safeId = sanitizeSegment(request.leaveId, request.leaveId)
        val updatedReq = request.copy(
            status = if (approved) "approved" else "rejected",
            adminNote = adminNote.trim()
        )
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(schoolCode, "staff_leaves_all")
        val existingObj = try {
            JSONObject(prefs.getString(key, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        existingObj.put(safeId, updatedReq.toJson())
        prefs.edit().putString(key, existingObj.toString()).apply()

        val savedLeave = httpPutJson(
            buildUrl(schoolCode, "staff_leaves/$safeId.json"),
            updatedReq.toJson().toString()
        )

        // عند الموافقة: تسجيل الموظف تلقائياً كـ "مجاز" (leave) في سجل الحضور لكل يوم من أيام الإجازة
        if (approved) {
            val leaveDates = StaffAttendanceDateUtils.expandDateRange(request.startDate, request.daysCount)
            val noteText = buildString {
                append("إجازة ${request.leaveType} معتمدة (${request.daysCount} يوم)")
                if (adminNote.isNotBlank()) {
                    append(" • ")
                    append(adminNote.trim())
                }
            }
            for (dateIso in leaveDates) {
                val leaveRecord = StaffAttendanceRecord(
                    staffId = request.staffId,
                    staffName = request.staffName,
                    role = "معلم",
                    dateIso = dateIso,
                    status = "leave",
                    checkInTime = "",
                    checkOutTime = "",
                    checkInTimestamp = 0L,
                    checkOutTimestamp = 0L,
                    recordMethod = "manual_admin",
                    distanceMeters = 0,
                    adminNotes = noteText,
                    isManualOverride = true,
                    updatedBy = adminName.ifBlank { "الإدارة" },
                    updatedAt = System.currentTimeMillis()
                )
                saveStaffAttendanceRecord(context, schoolCode, leaveRecord)
            }
        }
        savedLeave
    }

    private fun parseDailyRecordsMap(raw: String?, dateIso: String): Map<String, StaffAttendanceRecord> {
        if (raw.isNullOrBlank() || raw == "null") return emptyMap()
        val map = LinkedHashMap<String, StaffAttendanceRecord>()
        try {
            val root = JSONObject(raw)
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val rec = StaffAttendanceRecord.fromJson(root.optJSONObject(k), dateIso, k)
                if (rec != null) {
                    map[rec.staffId] = rec
                    if (k != rec.staffId) {
                        map[k] = rec
                    }
                }
            }
        } catch (_: Exception) {
        }
        return map
    }
}

/**
 * محرك التحقق الفوري من النطاق الجغرافي ومضاد التزييف (Anti-Mock GPS) عبر LocationManager القياسي
 * بدون أي اعتماد على Google Play Services أو تتبع موقع بالخلفية.
 */
object ElectronicFingerprintZoneEngine {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isMockLocation(location: Location): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.isMock
            } else {
                @Suppress("DEPRECATION")
                location.isFromMockProvider
            }
        } catch (_: Exception) {
            false
        }
    }

    fun computeDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].roundToInt().coerceAtLeast(0)
    }

    @Suppress("DEPRECATION")
    fun getConnectedWifiSsid(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return ""
            val info = wifiManager.connectionInfo ?: return ""
            val raw = info.ssid?.trim().orEmpty().removePrefix("\"").removeSuffix("\"")
            if (raw.isBlank() || raw.equals("<unknown ssid>", ignoreCase = true) || raw == "0x") {
                ""
            } else {
                raw
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * يلتقط موقع الجهاز الفعلي لحظياً عند الطلب عبر Android LocationManager فقط (GPS + Network).
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchFreshDeviceLocation(
        context: Context,
        timeoutMs: Long = 7500L
    ): Location? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission(context)) return@withContext null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val providers = buildList {
            if (runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        if (providers.isEmpty()) return@withContext null

        // محاولة قراءة موقع حديث جداً من getCurrentLocation (API 30+) أو requestLocationUpdates
        val fresh = withTimeoutOrNull(timeoutMs) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val primaryProvider = providers.first()
                requestCurrentLocationApi30(context, lm, primaryProvider)
                    ?: providers.getOrNull(1)?.let { requestCurrentLocationApi30(context, lm, it) }
            } else {
                requestSingleLocationLegacy(lm, providers)
            }
        }

        if (fresh != null) return@withContext fresh

        // احتياطي آمن: أحدث موقع معروف خلال آخر 3 دقائق (مع استمرار فحص التزييف Anti-Mock)
        val now = System.currentTimeMillis()
        providers.mapNotNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }.filter { loc ->
            (now - loc.time) <= 180_000L || loc.time == 0L
        }.minByOrNull { it.accuracy }
            ?: providers.mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocationApi30(
        context: Context,
        lm: LocationManager,
        provider: String
    ): Location? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        try {
            lm.getCurrentLocation(
                provider,
                signal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (cont.isActive) {
                    cont.resume(location)
                }
            }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocationLegacy(
        lm: LocationManager,
        providers: List<String>
    ): Location? = suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                runCatching { lm.removeUpdates(this) }
                if (cont.isActive) cont.resume(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        try {
            providers.forEach { provider ->
                lm.requestLocationUpdates(provider, 500L, 0f, listener, Looper.getMainLooper())
            }
        } catch (_: Exception) {
            runCatching { lm.removeUpdates(listener) }
            if (cont.isActive) cont.resume(null)
        }
    }

    /**
     * فحص حالة النطاق الحالية لعرضها داخل البطاقة الزجاجية الحية في ترويسة المعلم
     */
    suspend fun probeCurrentZone(
        context: Context,
        config: SchoolGeofenceConfig,
        holidayLabel: String?
    ): ZoneProbeResult {
        val todayIso = StaffAttendanceDateUtils.todayIso()
        val holidayMsg = StaffAttendanceDateUtils.getHolidayNoticeIfAny(todayIso, holidayLabel)
        if (holidayMsg != null) {
            return ZoneProbeResult(
                isHoliday = true,
                holidayLabel = holidayMsg,
                isGeofenceEnabled = config.isEnabled,
                hasLocationPermission = hasLocationPermission(context),
                allowedRadiusMeters = config.radiusMeters,
                statusHeadline = "عطلة رسمية",
                statusDetail = holidayMsg
            )
        }

        if (!config.isEnabled) {
            return ZoneProbeResult(
                isGeofenceEnabled = false,
                hasLocationPermission = hasLocationPermission(context),
                allowedRadiusMeters = config.radiusMeters,
                statusHeadline = "البصمة متوقفة مؤقتاً",
                statusDetail = "قامت إدارة المدرسة بإيقاف البصمة الجغرافية مؤقتاً."
            )
        }

        if (!hasLocationPermission(context)) {
            return ZoneProbeResult(
                isGeofenceEnabled = true,
                hasLocationPermission = false,
                allowedRadiusMeters = config.radiusMeters,
                statusHeadline = "مطلوب إذن الموقع",
                statusDetail = "اسمح بالوصول إلى الموقع للتحقق من تواجدك داخل نطاق المدرسة."
            )
        }

        val connectedWifi = getConnectedWifiSsid(context)
        val wifiMatched = config.schoolWifiSsid.isNotBlank() &&
            connectedWifi.equals(config.schoolWifiSsid.trim(), ignoreCase = true)

        val location = fetchFreshDeviceLocation(context)
        if (location == null) {
            if (wifiMatched) {
                return ZoneProbeResult(
                    isGeofenceEnabled = true,
                    hasLocationPermission = true,
                    insideZone = true,
                    matchedByWifi = true,
                    connectedWifiSsid = connectedWifi,
                    distanceMeters = 0,
                    allowedRadiusMeters = config.radiusMeters,
                    statusHeadline = "داخل نطاق المدرسة (Wi-Fi)",
                    statusDetail = "تم التحقق عبر شبكة المدرسة المعتمدة: $connectedWifi"
                )
            }
            return ZoneProbeResult(
                isGeofenceEnabled = true,
                hasLocationPermission = true,
                isGpsDisabled = true,
                allowedRadiusMeters = config.radiusMeters,
                statusHeadline = "يرجى تفعيل GPS",
                statusDetail = "تعذّر التقاط موقعك الحالي. تأكد من تفعيل خدمة الموقع (GPS) ثم أعد المحاولة."
            )
        }

        if (isMockLocation(location)) {
            return ZoneProbeResult(
                isGeofenceEnabled = true,
                hasLocationPermission = true,
                isMockLocationDetected = true,
                insideZone = false,
                allowedRadiusMeters = config.radiusMeters,
                statusHeadline = "موقع وهمي مرفوض",
                statusDetail = "تعذّر قبول البصمة • تم اكتشاف تطبيق موقع وهمي"
            )
        }

        val distance = computeDistanceMeters(
            location.latitude,
            location.longitude,
            config.latitude,
            config.longitude
        )
        val inside = distance <= config.radiusMeters || wifiMatched

        return ZoneProbeResult(
            isGeofenceEnabled = true,
            hasLocationPermission = true,
            insideZone = inside,
            matchedByWifi = wifiMatched && distance > config.radiusMeters,
            connectedWifiSsid = connectedWifi,
            distanceMeters = distance,
            allowedRadiusMeters = config.radiusMeters,
            deviceLatitude = location.latitude,
            deviceLongitude = location.longitude,
            statusHeadline = if (inside) "داخل نطاق المدرسة" else "خارج نطاق المدرسة",
            statusDetail = if (inside) {
                if (wifiMatched && distance > config.radiusMeters) {
                    "متصل بشبكة المدرسة المعتمدة ($connectedWifi) • جاهز للبصمة"
                } else {
                    "تبعد $distance متراً عن مركز المدرسة (النطاق المسموح ${config.radiusMeters} متراً)"
                }
            } else {
                "أنت خارج نطاق المدرسة حالياً (تبعد $distance متراً عن المدرسة • النطاق المسموح ${config.radiusMeters} متراً). يرجى التواجد داخل المدرسة لإتمام البصمة."
            }
        )
    }

    /**
     * تنفيذ البصمة الإلكترونية (حضور أو انصراف) مع كافة الفحوصات الأمنية والجغرافية.
     */
    suspend fun performElectronicFingerprint(
        context: Context,
        schoolCode: String,
        staffId: String,
        staffName: String,
        staffRoleLabel: String,
        config: SchoolGeofenceConfig,
        existingTodayRecord: StaffAttendanceRecord?,
        holidayLabel: String?
    ): FingerprintActionOutcome {
        val todayIso = StaffAttendanceDateUtils.todayIso()
        val holidayMsg = StaffAttendanceDateUtils.getHolidayNoticeIfAny(todayIso, holidayLabel)
        if (holidayMsg != null) {
            return FingerprintActionOutcome(
                success = false,
                title = "اليوم عطلة رسمية",
                message = holidayMsg
            )
        }

        if (!config.isEnabled) {
            return FingerprintActionOutcome(
                success = false,
                title = "البصمة غير مفعّلة",
                message = "تم إيقاف البصمة الجغرافية مؤقتاً من قبل إدارة المدرسة."
            )
        }

        if (existingTodayRecord?.isManualOverride == true) {
            return FingerprintActionOutcome(
                success = false,
                title = "حالة اليوم محددة من الإدارة",
                message = "تم ضبط وتثبيت حالة حضورك اليوم يدوياً من قبل إدارة المدرسة ولا يمكن تعديلها تلقائياً."
            )
        }

        if (existingTodayRecord != null && existingTodayRecord.hasCheckedIn && existingTodayRecord.hasCheckedOut) {
            return FingerprintActionOutcome(
                success = false,
                title = "اكتمل دوامك اليوم",
                message = "تم تسجيل بصمة الحضور (${existingTodayRecord.checkInTime}) والانصراف (${existingTodayRecord.checkOutTime}) بنجاح."
            )
        }

        if (!hasLocationPermission(context)) {
            return FingerprintActionOutcome(
                success = false,
                title = "مطلوب إذن الموقع",
                message = "يرجى منح صلاحية الموقع (GPS) للتحقق من تواجدك داخل نطاق المدرسة.",
                requiresPermission = true
            )
        }

        val probe = probeCurrentZone(context, config, holidayLabel)
        if (probe.isMockLocationDetected) {
            return FingerprintActionOutcome(
                success = false,
                title = "تم رفض البصمة",
                message = "تعذّر قبول البصمة • تم اكتشاف تطبيق موقع وهمي",
                isMockDetected = true
            )
        }

        if (probe.isGpsDisabled) {
            return FingerprintActionOutcome(
                success = false,
                title = "خدمة الموقع غير متاحة",
                message = "يرجى تفعيل الموقع (GPS) في جهازك ثم إعادة الضغط لتسجيل البصمة."
            )
        }

        if (!probe.insideZone) {
            val dist = probe.distanceMeters ?: 0
            return FingerprintActionOutcome(
                success = false,
                title = "خارج نطاق المدرسة",
                message = "أنت خارج نطاق المدرسة حالياً (تبعد $dist متراً عن المدرسة • النطاق المسموح ${config.radiusMeters} متراً). يرجى التواجد داخل المدرسة لإتمام البصمة."
            )
        }

        val nowTime = StaffAttendanceDateUtils.nowTimeHHmm()
        val nowMillis = System.currentTimeMillis()
        val distanceMeters = probe.distanceMeters ?: 0

        val isCheckOut = existingTodayRecord != null && existingTodayRecord.hasCheckedIn && !existingTodayRecord.hasCheckedOut
        val newRecord = if (!isCheckOut) {
            val nowMin = StaffAttendanceDateUtils.timeToMinutes(nowTime, 8 * 60)
            val lateThresholdMin = StaffAttendanceDateUtils.timeToMinutes(config.lateAfterTime, 8 * 60)
            val computedStatus = if (nowMin > lateThresholdMin) "late" else "present"
            StaffAttendanceRecord(
                staffId = staffId,
                staffName = staffName,
                role = staffRoleLabel.ifBlank { "معلم" },
                dateIso = todayIso,
                status = computedStatus,
                checkInTime = nowTime,
                checkOutTime = "",
                checkInTimestamp = nowMillis,
                checkOutTimestamp = 0L,
                recordMethod = "electronic_fingerprint_gps",
                distanceMeters = distanceMeters,
                adminNotes = existingTodayRecord?.adminNotes.orEmpty(),
                isManualOverride = false,
                updatedBy = staffName,
                updatedAt = nowMillis
            )
        } else {
            existingTodayRecord!!.copy(
                checkOutTime = nowTime,
                checkOutTimestamp = nowMillis,
                distanceMeters = distanceMeters,
                updatedBy = staffName,
                updatedAt = nowMillis
            )
        }

        StaffAttendanceCloudStore.saveStaffAttendanceRecord(context, schoolCode, newRecord)

        return if (!isCheckOut) {
            val statusArabic = if (newRecord.status == "late") "متأخر" else "حاضر"
            FingerprintActionOutcome(
                success = true,
                isCheckOut = false,
                title = "تم تسجيل بصمة الحضور بنجاح",
                message = "الحالة: $statusArabic • الوقت: $nowTime • المسافة: $distanceMeters م",
                updatedRecord = newRecord
            )
        } else {
            FingerprintActionOutcome(
                success = true,
                isCheckOut = true,
                title = "تم تسجيل بصمة الانصراف بنجاح",
                message = "وقت الانصراف: $nowTime • اكتمل دوامك اليوم بنجاح.",
                updatedRecord = newRecord
            )
        }
    }

    /**
     * يستخرج خط العرض وخط الطول من إحداثيات مباشرة أو روابط خرائط Google Maps.
     */
    fun parseCoordinatesFromText(input: String): Pair<Double, Double>? {
        val text = input.trim()
        if (text.isBlank()) return null

        // نمط @lat,lng في روابط Google Maps
        val atRegex = Regex("""@(-?\d{1,2}\.\d+)\s*,\s*(-?\d{1,3}\.\d+)""")
        atRegex.find(text)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return lat to lng
            }
        }

        // نمط !3dLAT!4dLNG
        val bangRegex = Regex("""!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)""")
        bangRegex.find(text)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return lat to lng
            }
        }

        // نمط q=lat,lng أو ll=lat,lng أو إحداثيات مباشرة مفصولة بفاصلة أو مسافة
        val genericRegex = Regex("""(-?\d{1,2}\.\d+)\s*[,،\s]+\s*(-?\d{1,3}\.\d+)""")
        genericRegex.find(text)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull()
            val lng = m.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return lat to lng
            }
        }
        return null
    }
}
