package com.example.util

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.data.FirebaseCloudSync
import com.example.livebroadcast.LiveAudioRoom
import com.example.livebroadcast.LiveRoomAudienceType
import com.example.model.ArabicTextNormalizer
import com.example.model.FirebaseConfigConstants
import com.example.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Persists lightweight logged-in user/school metadata so Android background jobs
 * can accurately evaluate target audiences and deliver external notifications
 * even when the app UI is completely closed or after device reboot.
 */
data class BackgroundSessionSnapshot(
    val isLoggedIn: Boolean,
    val schoolId: String,
    val schoolName: String,
    val userId: String,
    val userRole: String,
    val studentGrades: Set<String>
)

object BackgroundSessionStore {
    private const val PREFS_NAME = "madrasati_bg_alert_session_v1"
    private const val KEY_LOGGED_IN = "is_logged_in"
    private const val KEY_SCHOOL_ID = "school_id"
    private const val KEY_SCHOOL_NAME = "school_name"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_STUDENT_GRADES = "student_grades"

    fun saveActiveSession(
        context: Context,
        schoolId: String,
        schoolName: String,
        userId: String,
        userRole: UserRole,
        studentGrades: Set<String>
    ) {
        val cleanSid = InputSanitizer.sanitizeSchoolId(schoolId)
        if (cleanSid.isBlank() || userId.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_SCHOOL_ID, cleanSid)
            .putString(KEY_SCHOOL_NAME, schoolName.trim())
            .putString(KEY_USER_ID, userId.trim())
            .putString(KEY_USER_ROLE, userRole.name)
            .putStringSet(KEY_STUDENT_GRADES, studentGrades.filter { it.isNotBlank() }.toSet())
            .apply()
    }

    fun clearSession(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun loadSession(context: Context): BackgroundSessionSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return BackgroundSessionSnapshot(
            isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false),
            schoolId = prefs.getString(KEY_SCHOOL_ID, "").orEmpty(),
            schoolName = prefs.getString(KEY_SCHOOL_NAME, "").orEmpty(),
            userId = prefs.getString(KEY_USER_ID, "").orEmpty(),
            userRole = prefs.getString(KEY_USER_ROLE, "").orEmpty(),
            studentGrades = prefs.getStringSet(KEY_STUDENT_GRADES, emptySet()).orEmpty()
        )
    }
}

/**
 * Pure Android SDK background cloud monitor (zero external dependencies).
 * Checks Firebase RTDB directly for:
 * 1) Active Live Audio Broadcasts & Principal Urgent Join Invites (with BigPictureStyle + Join Action)
 * 2) Principal Urgent School Alerts
 * 3) Platform / Developer Announcements
 */
object BackgroundAlertPollEngine {

    private const val CONNECT_TIMEOUT_MS = 6500
    private const val READ_TIMEOUT_MS = 6500

    suspend fun checkAndDispatchAlerts(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val session = BackgroundSessionStore.loadSession(appContext)
        if (!session.isLoggedIn || session.schoolId.isBlank() || session.userId.isBlank()) {
            return@withContext
        }
        if (!SystemNotificationHelper.hasNotificationPermission(appContext)) {
            return@withContext
        }

        val nowMs = System.currentTimeMillis()
        checkLiveBroadcastsAndUrgentInvites(appContext, session, nowMs)
        checkPrincipalUrgentAlert(appContext, session)
        checkDeveloperPlatformBroadcasts(appContext, session)
    }

    private fun checkLiveBroadcastsAndUrgentInvites(
        context: Context,
        session: BackgroundSessionSnapshot,
        nowMs: Long
    ) {
        try {
            val url =
                "${FirebaseConfigConstants.DATABASE_URL}/schools/${session.schoolId}/${FirebaseConfigConstants.COL_LIVE_ROOMS}.json"
            val raw = httpGetString(url) ?: return
            if (raw.isBlank() || raw == "null") return

            val rootObj = JSONObject(raw)
            val keys = rootObj.keys()
            val isAdmin = session.userRole.equals(UserRole.ADMIN.name, ignoreCase = true)

            while (keys.hasNext()) {
                val roomKey = keys.next()
                val roomObj = rootObj.optJSONObject(roomKey) ?: continue
                val room = LiveAudioRoom.fromJson(
                    obj = roomObj,
                    fallbackId = roomKey,
                    fallbackSchoolId = session.schoolId
                ) ?: continue

                if (!room.isLive) continue
                if (!isAdmin && room.isUserBanned(session.userId)) continue

                // 1) Urgent Join Invite targeting this user
                room.urgentInvites
                    .filter { inv ->
                        inv.targetUserId.equals(session.userId, ignoreCase = true) &&
                            (nowMs - inv.createdAt) < 30L * 60L * 1000L
                    }
                    .maxByOrNull { it.createdAt }
                    ?.let { invite ->
                        SystemNotificationHelper.showLiveBroadcastNotificationOnce(
                            context = context,
                            dedupKey = "urgent_invite_${invite.id}",
                            title = "تنبيه عاجل للدخول للبث المباشر",
                            body = "تدعوك ${invite.senderName} للانضمام فوراً إلى «${invite.roomTitle.ifBlank { room.title }}»",
                            isUrgentInvite = true,
                            roomId = room.id,
                            roomTitle = invite.roomTitle.ifBlank { room.title },
                            hostName = invite.senderName.ifBlank { room.hostName }
                        )
                    }

                // 2) New Live Broadcast Room targeting this user
                val isFreshRoom = (nowMs - room.effectiveStartedAt) < 6L * 60L * 60L * 1000L
                val isEligibleAudience = isSessionEligibleForRoom(room, session)
                if (isFreshRoom && isEligibleAudience && !room.hostId.equals(session.userId, ignoreCase = true)) {
                    SystemNotificationHelper.showLiveBroadcastNotificationOnce(
                        context = context,
                        dedupKey = "live_room_${room.id}_${session.userId}",
                        title = "بث صوتي مباشر الآن",
                        body = "${room.title} • بإشراف ${room.hostName}",
                        isUrgentInvite = false,
                        roomId = room.id,
                        roomTitle = room.title,
                        hostName = room.hostName
                    )
                }
            }
        } catch (_: Exception) {
            // Silent resilience on restricted networks
        }
    }

    private fun isSessionEligibleForRoom(
        room: LiveAudioRoom,
        session: BackgroundSessionSnapshot
    ): Boolean {
        if (session.userRole.equals(UserRole.ADMIN.name, ignoreCase = true)) return true
        return when (LiveRoomAudienceType.fromKey(room.targetAudience)) {
            LiveRoomAudienceType.ALL -> true
            LiveRoomAudienceType.STAFF -> session.userRole.equals(UserRole.TEACHER.name, ignoreCase = true)
            LiveRoomAudienceType.CLASS -> {
                if (session.userRole.equals(UserRole.TEACHER.name, ignoreCase = true)) {
                    true
                } else {
                    val targetGrade = room.targetGrade.trim()
                    targetGrade.isBlank() || session.studentGrades.any { grade ->
                        ArabicTextNormalizer.matchesClass(grade, targetGrade)
                    }
                }
            }
        }
    }

    private suspend fun checkPrincipalUrgentAlert(
        context: Context,
        session: BackgroundSessionSnapshot
    ) {
        try {
            val payload = FirebaseCloudSync.fetchSubCollectionPayload(
                schoolId = session.schoolId,
                subCollection = FirebaseConfigConstants.COL_URGENT_ALERT
            ) ?: return
            if (payload.isBlank() || payload == "null") return

            val obj = JSONObject(payload)
            val isActive = obj.optBoolean("isActive", false)
            val message = InputSanitizer.stripEmojis(obj.optString("message", "")).trim()
            val updatedAt = obj.optString("updatedAt", "").trim()
            if (!isActive || message.isBlank()) return

            val cleanSchool = session.schoolName.ifBlank { "المدرسة" }
            val dedupKey = "bg_urgent_school_alert_${session.schoolId}_${updatedAt}_${message.hashCode()}"
            SystemNotificationHelper.showAnnouncementNotificationOnce(
                context = context,
                dedupKey = dedupKey,
                title = "تبليغ عاجل من إدارة $cleanSchool",
                body = message,
                isUrgentAdmin = true
            )
        } catch (_: Exception) {
        }
    }

    private suspend fun checkDeveloperPlatformBroadcasts(
        context: Context,
        session: BackgroundSessionSnapshot
    ) {
        try {
            val payload = FirebaseCloudSync.fetchGlobalCollectionPayload(
                collectionName = FirebaseConfigConstants.COL_DEV_BROADCASTS,
                docName = "registry"
            ) ?: return
            if (payload.isBlank() || payload == "null") return

            val arr = JSONArray(payload)
            if (arr.length() == 0) return

            val isAdmin = session.userRole.equals(UserRole.ADMIN.name, ignoreCase = true)
            for (i in 0 until minOf(arr.length(), 4)) {
                val obj = arr.optJSONObject(i) ?: continue
                val isActive = obj.optBoolean("isActive", true)
                if (!isActive) continue

                val targetSchool = obj.optString("targetSchoolId", "ALL").trim()
                val schoolMatches = targetSchool.equals("ALL", ignoreCase = true) ||
                    targetSchool.isBlank() ||
                    targetSchool.equals(session.schoolId, ignoreCase = true)
                if (!schoolMatches) continue

                val targetRole = obj.optString("targetRole", "ALL").trim()
                val roleMatches = targetRole.equals("ALL", ignoreCase = true) ||
                    targetRole.isBlank() ||
                    targetRole.equals(session.userRole, ignoreCase = true) ||
                    (isAdmin && targetRole.equals("ADMIN", ignoreCase = true))
                if (!roleMatches) continue

                val id = obj.optString("id", "").trim()
                val title = InputSanitizer.stripEmojis(obj.optString("title", "")).trim()
                val message = InputSanitizer.stripEmojis(obj.optString("message", "")).trim()
                val createdAt = obj.optString("createdAt", "").trim()
                if (title.isBlank() && message.isBlank()) continue

                val dedupKey = "bg_dev_broadcast_${id.ifBlank { "${title.hashCode()}_$createdAt" }}"
                SystemNotificationHelper.showAnnouncementNotificationOnce(
                    context = context,
                    dedupKey = dedupKey,
                    title = title.ifBlank { "تعميم إدارة المنصة" },
                    body = message.ifBlank { title },
                    isUrgentAdmin = false
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun httpGetString(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) return null
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

/**
 * Native Android JobService running lightweight periodic & follow-up cloud alert checks
 * even when the app UI is completely closed — zero external libraries required.
 */
class BackgroundAlertJobService : JobService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        val jobParams = params ?: return false
        activeJob?.cancel()
        activeJob = serviceScope.launch {
            try {
                BackgroundAlertPollEngine.checkAndDispatchAlerts(applicationContext)
                // If this was the quick follow-up job and the user is still logged in,
                // chain another follow-up check smoothly for timely background alerts.
                if (jobParams.jobId == BackgroundAlertMonitorScheduler.JOB_ID_QUICK_FOLLOWUP) {
                    val session = BackgroundSessionStore.loadSession(applicationContext)
                    if (session.isLoggedIn && SystemNotificationHelper.hasNotificationPermission(applicationContext)) {
                        BackgroundAlertMonitorScheduler.scheduleQuickFollowUp(
                            context = applicationContext,
                            delayMs = BackgroundAlertMonitorScheduler.FOLLOWUP_INTERVAL_MS
                        )
                    }
                }
            } finally {
                jobFinished(jobParams, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        activeJob?.cancel()
        return true
    }
}

/**
 * Schedules and manages persistent Android JobScheduler tasks for closed-app external notifications.
 */
object BackgroundAlertMonitorScheduler {
    const val JOB_ID_PERIODIC = 47201
    const val JOB_ID_QUICK_FOLLOWUP = 47202
    private const val PERIODIC_INTERVAL_MS = 15L * 60L * 1000L // 15 min minimum required by JobScheduler
    const val FOLLOWUP_INTERVAL_MS = 3L * 60L * 1000L // 3 min chained follow-up for prompt live broadcast alerts

    fun scheduleAll(context: Context) {
        schedulePeriodicMonitoring(context)
        scheduleQuickFollowUp(context, delayMs = 90_000L)
    }

    fun schedulePeriodicMonitoring(context: Context) {
        try {
            val appContext = context.applicationContext
            val scheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val existing = scheduler.allPendingJobs.firstOrNull { it.id == JOB_ID_PERIODIC }
            if (existing != null) return

            val component = ComponentName(appContext, BackgroundAlertJobService::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID_PERIODIC, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIODIC_INTERVAL_MS)
                .setPersisted(true)
                .build()
            scheduler.schedule(jobInfo)
        } catch (_: Exception) {
        }
    }

    fun scheduleQuickFollowUp(context: Context, delayMs: Long = FOLLOWUP_INTERVAL_MS) {
        try {
            val appContext = context.applicationContext
            val scheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val component = ComponentName(appContext, BackgroundAlertJobService::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID_QUICK_FOLLOWUP, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(delayMs.coerceAtLeast(60_000L))
                .setOverrideDeadline((delayMs + 180_000L).coerceAtLeast(240_000L))
                .setPersisted(true)
                .build()
            scheduler.schedule(jobInfo)
        } catch (_: Exception) {
        }
    }

    fun cancelAll(context: Context) {
        try {
            val scheduler = context.applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            scheduler.cancel(JOB_ID_PERIODIC)
            scheduler.cancel(JOB_ID_QUICK_FOLLOWUP)
        } catch (_: Exception) {
        }
    }
}

/**
 * Boot & App-Update BroadcastReceiver that automatically re-arms background alert monitoring
 * immediately after device restart or app update when a user session is active.
 */
class AlertBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context?.applicationContext ?: return
        val action = intent?.action.orEmpty()
        val isSupportedAction = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        if (!isSupportedAction) return

        val session = BackgroundSessionStore.loadSession(ctx)
        if (session.isLoggedIn && session.schoolId.isNotBlank()) {
            SystemNotificationHelper.ensureChannelCreated(ctx)
            BackgroundAlertMonitorScheduler.scheduleAll(ctx)
        }
    }
}
