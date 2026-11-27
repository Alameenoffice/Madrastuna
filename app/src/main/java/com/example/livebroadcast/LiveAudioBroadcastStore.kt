package com.example.livebroadcast

import android.content.Context
import com.example.model.ArabicTextNormalizer
import com.example.model.FirebaseConfigConstants
import com.example.model.StudentRecord
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.util.InputSanitizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID

const val DEFAULT_ZEGO_APP_ID: Long = 986873491L
const val DEFAULT_ZEGO_SERVER_SECRET: String = "7c1e4b9376dda6298e1b97d8d35671c3"

enum class LiveRoomAudienceType(
    val key: String,
    val arabicLabel: String
) {
    STAFF("staff", "الكوادر فقط"),
    ALL("all", "المدرسة كلها"),
    CLASS("class", "صف محدّد");

    companion object {
        fun fromKey(raw: String?): LiveRoomAudienceType {
            val clean = raw.orEmpty().trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { it.key == clean } ?: ALL
        }
    }
}

data class LiveRoomParticipant(
    val userId: String,
    val fullName: String,
    val role: String = "TEACHER",
    val gender: String = "",
    val avatarUrl: String = "",
    val joinedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("userId", userId)
        put("fullName", fullName)
        put("role", role)
        put("gender", gender)
        put("avatarUrl", avatarUrl)
        put("joinedAt", joinedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject, fallbackUserId: String = ""): LiveRoomParticipant? {
            val uid = obj.optString("userId").ifBlank { fallbackUserId }.trim()
            if (uid.isBlank()) return null
            val name = obj.optString("fullName").trim().ifBlank { "مشارك" }
            return LiveRoomParticipant(
                userId = uid,
                fullName = name,
                role = obj.optString("role", "TEACHER").trim(),
                gender = obj.optString("gender").trim(),
                avatarUrl = obj.optString("avatarUrl").trim(),
                joinedAt = obj.optLong("joinedAt", System.currentTimeMillis())
            )
        }
    }
}

data class LiveRoomUrgentInvite(
    val id: String,
    val roomId: String,
    val roomTitle: String,
    val targetUserId: String,
    val targetUserName: String,
    val senderName: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("roomId", roomId)
        put("roomTitle", roomTitle)
        put("targetUserId", targetUserId)
        put("targetUserName", targetUserName)
        put("senderName", senderName)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): LiveRoomUrgentInvite? {
            val id = obj.optString("id").trim()
            val roomId = obj.optString("roomId").trim()
            val targetUserId = obj.optString("targetUserId").trim()
            if (id.isBlank() || roomId.isBlank() || targetUserId.isBlank()) return null
            return LiveRoomUrgentInvite(
                id = id,
                roomId = roomId,
                roomTitle = obj.optString("roomTitle").trim(),
                targetUserId = targetUserId,
                targetUserName = obj.optString("targetUserName").trim(),
                senderName = obj.optString("senderName").trim().ifBlank { "إدارة المدرسة" },
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}

data class LiveAudioRoom(
    val id: String,
    val schoolId: String,
    val title: String,
    val hostId: String,
    val hostName: String,
    val hostRole: String = "ADMIN",
    val hostGender: String = "",
    val hostAvatarUrl: String = "",
    val status: String = "live", // "live" | "ended"
    val createdAt: Long = System.currentTimeMillis(),
    val createdAtIso: String = "",
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val targetAudience: String = "all", // "staff" | "all" | "class"
    val targetGrade: String = "",
    val targetSection: String = "",
    val coHosts: List<String> = emptyList(),
    val activeParticipants: Map<String, LiveRoomParticipant> = emptyMap(),
    val participantIdsHistory: List<String> = emptyList(),
    val mutedUserIds: List<String> = emptyList(),
    val kickedUsers: Map<String, Long> = emptyMap(),
    val bannedUserIds: List<String> = emptyList(),
    val bannedUsersInfo: Map<String, String> = emptyMap(),
    val urgentInvites: List<LiveRoomUrgentInvite> = emptyList()
) {
    val isLive: Boolean
        get() = status.equals("live", ignoreCase = true)

    val effectiveStartedAt: Long
        get() = if (startedAt > 0L) startedAt else createdAt

    val totalParticipantsCount: Int
        get() = buildSet {
            if (hostId.isNotBlank()) add(hostId)
            addAll(participantIdsHistory.filter { it.isNotBlank() })
            addAll(activeParticipants.keys.filter { it.isNotBlank() })
        }.size

    fun isUserMuted(userId: String): Boolean {
        if (userId.isBlank()) return false
        return mutedUserIds.any { it.equals(userId, ignoreCase = true) }
    }

    fun isUserBanned(userId: String): Boolean {
        if (userId.isBlank()) return false
        return bannedUserIds.any { it.equals(userId, ignoreCase = true) }
    }

    fun kickedAtForUser(userId: String): Long {
        if (userId.isBlank()) return 0L
        return kickedUsers.entries.firstOrNull { it.key.equals(userId, ignoreCase = true) }?.value ?: 0L
    }

    fun formattedDurationArabic(): String {
        val startMs = effectiveStartedAt
        if (endedAt > startMs && startMs > 0L) {
            return formatSecondsToArabicDuration(((endedAt - startMs) / 1000L).coerceAtLeast(1L))
        }
        return "مكتمل • ${resolvedCreatedDateForDisplay().substringAfter(" ", "").ifBlank { resolvedCreatedDateForDisplay() }}"
    }

    val audienceType: LiveRoomAudienceType
        get() = LiveRoomAudienceType.fromKey(targetAudience)

    val audienceDisplayLabel: String
        get() = when (audienceType) {
            LiveRoomAudienceType.STAFF -> "الكوادر فقط"
            LiveRoomAudienceType.ALL -> "المدرسة كلها"
            LiveRoomAudienceType.CLASS -> {
                val grade = targetGrade.trim()
                val sec = targetSection.trim()
                when {
                    grade.isBlank() -> "صف محدّد"
                    sec.isBlank() || sec == "الكل" -> grade
                    else -> "$grade - شعبة $sec"
                }
            }
        }

    fun buildZegoRoomId(): String {
        val cleanSchool = sanitizeAsciiIdentifier(schoolId, fallback = "school")
        val cleanDoc = sanitizeAsciiIdentifier(id, fallback = "room")
        return "madrasati_${cleanSchool}_${cleanDoc}".take(64)
    }

    fun resolvedCreatedDateForDisplay(): String {
        if (createdAtIso.isNotBlank()) return createdAtIso
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return fmt.format(Date(effectiveStartedAt))
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("schoolId", schoolId)
        put("title", title)
        put("hostId", hostId)
        put("hostName", hostName)
        put("hostRole", hostRole)
        put("hostGender", hostGender)
        put("hostAvatarUrl", hostAvatarUrl)
        put("status", status)
        put("createdAt", createdAt)
        put("createdAtIso", resolvedCreatedDateForDisplay())
        put("startedAt", effectiveStartedAt)
        if (endedAt > 0L) {
            put("endedAt", endedAt)
        }
        put("updatedAt", updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis())
        put("targetAudience", targetAudience)
        put("targetGrade", targetGrade)
        put("targetSection", targetSection)
        put("coHosts", JSONArray().apply { coHosts.forEach { put(it) } })
        put("activeParticipants", JSONObject().apply {
            activeParticipants.forEach { (uid, p) ->
                put(uid, p.toJson())
            }
        })
        put("participantIdsHistory", JSONArray().apply { participantIdsHistory.distinct().forEach { put(it) } })
        put("mutedUserIds", JSONArray().apply { mutedUserIds.distinct().forEach { put(it) } })
        put("kickedUsers", JSONObject().apply {
            kickedUsers.forEach { (uid, ts) -> put(uid, ts) }
        })
        put("bannedUserIds", JSONArray().apply { bannedUserIds.distinct().forEach { put(it) } })
        put("bannedUsersInfo", JSONObject().apply {
            bannedUsersInfo.forEach { (uid, name) -> put(uid, name) }
        })
        put("urgentInvites", JSONArray().apply { urgentInvites.forEach { put(it.toJson()) } })
    }

    companion object {
        fun formatSecondsToArabicDuration(totalSeconds: Long): String {
            val safeSec = totalSeconds.coerceAtLeast(1L)
            val hours = (safeSec / 3600L).toInt()
            val minutes = ((safeSec % 3600L) / 60L).toInt()
            val seconds = (safeSec % 60L).toInt()

            val parts = mutableListOf<String>()
            if (hours > 0) {
                parts.add(
                    when (hours) {
                        1 -> "ساعة واحدة"
                        2 -> "ساعتان"
                        in 3..10 -> "$hours ساعات"
                        else -> "$hours ساعة"
                    }
                )
            }
            if (minutes > 0) {
                parts.add(
                    when (minutes) {
                        1 -> "دقيقة واحدة"
                        2 -> "دقيقتان"
                        in 3..10 -> "$minutes دقائق"
                        else -> "$minutes دقيقة"
                    }
                )
            }
            if (seconds > 0 || parts.isEmpty()) {
                parts.add(
                    when (seconds) {
                        1 -> "ثانية واحدة"
                        2 -> "ثانيتان"
                        in 3..10 -> "$seconds ثوانٍ"
                        else -> "$seconds ثانية"
                    }
                )
            }
            return parts.joinToString(" و")
        }

        fun fromJson(obj: JSONObject, fallbackId: String = "", fallbackSchoolId: String = ""): LiveAudioRoom? {
            val id = obj.optString("id").ifBlank { fallbackId }.trim()
            val title = obj.optString("title").trim()
            if (id.isBlank() || title.isBlank()) return null

            fun readStringArray(key: String): List<String> {
                val list = mutableListOf<String>()
                val arr = obj.optJSONArray(key)
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.optString(i).trim()
                        if (item.isNotBlank()) list.add(item)
                    }
                }
                return list.distinct()
            }

            val participantsMap = LinkedHashMap<String, LiveRoomParticipant>()
            val participantsObj = obj.optJSONObject("activeParticipants")
            if (participantsObj != null) {
                val pKeys = participantsObj.keys()
                while (pKeys.hasNext()) {
                    val key = pKeys.next()
                    val pObj = participantsObj.optJSONObject(key) ?: continue
                    LiveRoomParticipant.fromJson(pObj, fallbackUserId = key)?.let { p ->
                        participantsMap[p.userId] = p
                    }
                }
            }

            val kickedMap = LinkedHashMap<String, Long>()
            val kickedObj = obj.optJSONObject("kickedUsers")
            if (kickedObj != null) {
                val kKeys = kickedObj.keys()
                while (kKeys.hasNext()) {
                    val key = kKeys.next()
                    val ts = kickedObj.optLong(key, 0L)
                    if (key.isNotBlank() && ts > 0L) kickedMap[key] = ts
                }
            }

            val bannedInfoMap = LinkedHashMap<String, String>()
            val bannedInfoObj = obj.optJSONObject("bannedUsersInfo")
            if (bannedInfoObj != null) {
                val bKeys = bannedInfoObj.keys()
                while (bKeys.hasNext()) {
                    val key = bKeys.next()
                    val name = bannedInfoObj.optString(key).trim()
                    if (key.isNotBlank() && name.isNotBlank()) bannedInfoMap[key] = name
                }
            }

            val invitesList = mutableListOf<LiveRoomUrgentInvite>()
            val invitesArr = obj.optJSONArray("urgentInvites")
            if (invitesArr != null) {
                for (i in 0 until invitesArr.length()) {
                    val invObj = invitesArr.optJSONObject(i) ?: continue
                    LiveRoomUrgentInvite.fromJson(invObj)?.let { invitesList.add(it) }
                }
            }

            val createdMs = obj.optLong("createdAt", 0L).takeIf { it > 0L } ?: System.currentTimeMillis()
            val startedMs = obj.optLong("startedAt", 0L).takeIf { it > 0L } ?: createdMs
            val createdIso = obj.optString("createdAtIso").ifBlank {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(createdMs))
            }
            val hostId = obj.optString("hostId").trim()
            val historyIds = readStringArray("participantIdsHistory").ifEmpty {
                buildList {
                    if (hostId.isNotBlank()) add(hostId)
                    addAll(participantsMap.keys)
                }.distinct()
            }

            return LiveAudioRoom(
                id = id,
                schoolId = obj.optString("schoolId").ifBlank { fallbackSchoolId }.trim(),
                title = title,
                hostId = hostId,
                hostName = obj.optString("hostName").trim(),
                hostRole = obj.optString("hostRole", "ADMIN").trim(),
                hostGender = obj.optString("hostGender").trim(),
                hostAvatarUrl = obj.optString("hostAvatarUrl").trim(),
                status = obj.optString("status", "live").trim().lowercase(Locale.ROOT),
                createdAt = createdMs,
                createdAtIso = createdIso,
                startedAt = startedMs,
                endedAt = obj.optLong("endedAt", 0L).coerceAtLeast(0L),
                updatedAt = obj.optLong("updatedAt", createdMs).coerceAtLeast(0L),
                targetAudience = obj.optString("targetAudience", "all").trim().lowercase(Locale.ROOT),
                targetGrade = obj.optString("targetGrade").trim(),
                targetSection = obj.optString("targetSection").trim(),
                coHosts = readStringArray("coHosts"),
                activeParticipants = participantsMap,
                participantIdsHistory = historyIds,
                mutedUserIds = readStringArray("mutedUserIds"),
                kickedUsers = kickedMap,
                bannedUserIds = readStringArray("bannedUserIds"),
                bannedUsersInfo = bannedInfoMap,
                urgentInvites = invitesList
            )
        }
    }
}

data class ZegoPlatformConfig(
    val appId: Long = DEFAULT_ZEGO_APP_ID,
    val serverSecret: String = DEFAULT_ZEGO_SERVER_SECRET,
    val isCustomFromCloud: Boolean = false
)

fun sanitizeAsciiIdentifier(raw: String?, fallback: String = "u"): String {
    val cleaned = raw.orEmpty().trim().replace(Regex("[^A-Za-z0-9_]"), "")
    if (cleaned.isNotBlank()) return cleaned.take(40)
    val hash = (raw.orEmpty().hashCode().toLong() and 0xFFFFFFFFL).toString(36)
    return "${fallback}_$hash"
}

fun buildCleanZegoUserId(user: UserAccount?): String {
    val source = user?.id?.ifBlank { user.username } ?: "guest"
    val asciiOnly = source.replace(Regex("[^A-Za-z0-9]"), "")
    if (asciiOnly.length >= 3) return "u_${asciiOnly.take(28)}"
    val hash = (source.hashCode().toLong() and 0xFFFFFFFFL).toString(36)
    return "usr_$hash"
}

/**
 * Automatic Role Policy:
 * - Room creator (hostId) or any user inside coHosts -> ZegoUIKitPrebuilt.Host
 * - Everyone else -> ZegoUIKitPrebuilt.Audience
 */
fun isUserHostOrCoHost(room: LiveAudioRoom, user: UserAccount?): Boolean {
    if (user == null) return false
    if (user.id.isNotBlank() && user.id == room.hostId) return true
    if (user.id.isNotBlank() && room.coHosts.any { it.equals(user.id, ignoreCase = true) }) return true
    return false
}

/**
 * Strict Audience Scoping Policy:
 * - Host, co-hosts, and school principal (ADMIN) always see the room.
 * - "all"   -> visible to all members of this school.
 * - "staff" -> visible ONLY to ADMIN & TEACHER (never STUDENT or PARENT).
 * - "class" -> visible ONLY to students/parents belonging to targetGrade (and targetSection if specified),
 *              plus teachers assigned to that grade (or listed in coHosts).
 */
fun isUserIncludedInRoom(
    room: LiveAudioRoom,
    user: UserAccount?,
    linkedStudent: StudentRecord?,
    parentChildren: List<StudentRecord> = emptyList()
): Boolean {
    if (user == null) return false
    if (isUserHostOrCoHost(room, user)) return true
    if (user.role == UserRole.ADMIN) return true

    return when (room.audienceType) {
        LiveRoomAudienceType.ALL -> true
        LiveRoomAudienceType.STAFF -> user.role == UserRole.TEACHER
        LiveRoomAudienceType.CLASS -> {
            val targetGrade = room.targetGrade.trim()
            val targetSection = room.targetSection.trim().takeUnless { it == "الكل" }.orEmpty()
            if (targetGrade.isBlank()) return true
            when (user.role) {
                UserRole.STUDENT -> {
                    val stuGrade = linkedStudent?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass
                    val stuSection = linkedStudent?.sectionName?.ifBlank { user.sectionName } ?: user.sectionName
                    matchesGradeAndSection(stuGrade, stuSection, targetGrade, targetSection)
                }
                UserRole.PARENT -> {
                    val candidates = buildList {
                        addAll(parentChildren)
                        if (linkedStudent != null && parentChildren.none { it.id == linkedStudent.id }) {
                            add(linkedStudent)
                        }
                    }
                    if (candidates.isNotEmpty()) {
                        candidates.any { child ->
                            matchesGradeAndSection(child.gradeClass, child.sectionName, targetGrade, targetSection)
                        }
                    } else {
                        matchesGradeAndSection(user.gradeClass, user.sectionName, targetGrade, targetSection)
                    }
                }
                UserRole.TEACHER -> {
                    val assigned = user.assignedClasses.trim()
                    if (assigned.isBlank()) false
                    else {
                        val normTarget = ArabicTextNormalizer.normalizeText(targetGrade)
                        val normAssigned = ArabicTextNormalizer.normalizeText(assigned)
                        normAssigned.contains(normTarget)
                    }
                }
                UserRole.ADMIN -> true
            }
        }
    }
}

private fun matchesGradeAndSection(
    actualGrade: String,
    actualSection: String,
    targetGrade: String,
    targetSection: String
): Boolean {
    val gradeMatches = ArabicTextNormalizer.matchesClass(actualGrade, targetGrade) ||
        actualGrade.trim().equals(targetGrade.trim(), ignoreCase = true)
    if (!gradeMatches) return false
    if (targetSection.isBlank()) return true
    val normActualSec = ArabicTextNormalizer.normalizeSection(actualSection)
    val normTargetSec = ArabicTextNormalizer.normalizeSection(targetSection)
    return normActualSec == normTargetSec || actualSection.trim().equals(targetSection.trim(), ignoreCase = true)
}

/**
 * Isolated Real-Time Firebase Cloud Store for Live Audio Broadcasts.
 * Persists rooms, real-time participants, moderation controls (mute/kick/ban),
 * urgent join invites, duration timestamps, and history deletions under:
 *   - Firestore: schools/{schoolId}/live_rooms/{roomId}
 *   - RTDB:      schools/{schoolId}/live_rooms/{roomId}.json (with live SSE stream for instant updates)
 * Persists global ZEGOCLOUD settings under:
 *   - Firestore: platform_settings/zego
 *   - RTDB:      platform_settings/zego.json
 */
object LiveAudioBroadcastCloudStore {
    private const val PREFS_NAME = "madrasati_live_audio_broadcast_prefs"
    private const val KEY_ZEGO_APP_ID = "zego_cloud_app_id"
    private const val KEY_ZEGO_SECRET = "zego_cloud_server_secret"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _rooms = MutableStateFlow<List<LiveAudioRoom>>(emptyList())
    val rooms: StateFlow<List<LiveAudioRoom>> = _rooms.asStateFlow()

    private val _zegoConfig = MutableStateFlow(ZegoPlatformConfig())
    val zegoConfig: StateFlow<ZegoPlatformConfig> = _zegoConfig.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Prevents stale Firestore reads from resurrecting a room that was just deleted by the Principal
    private val recentlyDeletedRoomIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    @Volatile
    private var observedSchoolId: String = ""
    private var sseJob: Job? = null
    private var pollJob: Job? = null

    fun startObservingSchool(context: Context, rawSchoolId: String) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        if (cleanSchoolId.isBlank()) return
        val appCtx = context.applicationContext

        loadCachedZegoConfig(appCtx)
        if (observedSchoolId != cleanSchoolId) {
            observedSchoolId = cleanSchoolId
            _rooms.value = loadCachedRooms(appCtx, cleanSchoolId)
            sseJob?.cancel()
            pollJob?.cancel()

            sseJob = scope.launch {
                runRealtimeSseLoop(appCtx, cleanSchoolId)
            }
            pollJob = scope.launch {
                fetchZegoConfig(appCtx)
                refreshRoomsFromCloud(appCtx, cleanSchoolId)
                while (isActive) {
                    delay(8_000L)
                    refreshRoomsFromCloud(appCtx, cleanSchoolId)
                }
            }
        } else {
            scope.launch {
                refreshRoomsFromCloud(appCtx, cleanSchoolId)
            }
        }
    }

    fun stopObserving() {
        observedSchoolId = ""
        sseJob?.cancel()
        pollJob?.cancel()
        sseJob = null
        pollJob = null
    }

    suspend fun refreshRoomsFromCloud(context: Context, rawSchoolId: String): List<LiveAudioRoom> =
        withContext(Dispatchers.IO) {
            val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
            if (cleanSchoolId.isBlank()) return@withContext emptyList()
            _isSyncing.value = true
            try {
                val merged = coroutineScope {
                    val rtdbDeferred = async { fetchRoomsFromRtdb(cleanSchoolId) }
                    val fsDeferred = async { fetchRoomsFromFirestore(cleanSchoolId) }
                    val rtdbRooms = rtdbDeferred.await()
                    val fsRooms = fsDeferred.await()
                    mergeRoomLists(rtdbRooms, fsRooms)
                }
                if (merged != null) {
                    val sorted = merged
                        .filterNot { it.id in recentlyDeletedRoomIds }
                        .sortedByDescending { it.createdAt }
                    saveCachedRooms(context.applicationContext, cleanSchoolId, sorted)
                    if (observedSchoolId.isBlank() || observedSchoolId == cleanSchoolId) {
                        _rooms.value = sorted
                    }
                    sorted
                } else {
                    _rooms.value
                }
            } finally {
                _isSyncing.value = false
            }
        }

    suspend fun createLiveRoom(
        context: Context,
        rawSchoolId: String,
        title: String,
        hostUser: UserAccount,
        audienceType: LiveRoomAudienceType,
        targetGrade: String = "",
        targetSection: String = "",
        coHosts: List<String> = emptyList()
    ): LiveAudioRoom? = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        val cleanTitle = InputSanitizer.sanitize(title).trim()
        if (cleanSchoolId.isBlank() || cleanTitle.isBlank()) return@withContext null

        val nowMs = System.currentTimeMillis()
        val nowIso = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(nowMs))
        val docId = "room_${nowMs}_${UUID.randomUUID().toString().replace("-", "").take(6)}"

        val hostParticipant = LiveRoomParticipant(
            userId = hostUser.id,
            fullName = hostUser.fullName.ifBlank { hostUser.role.arabicTitle },
            role = hostUser.role.name,
            gender = hostUser.gender,
            avatarUrl = hostUser.photoBase64,
            joinedAt = nowMs
        )

        val room = LiveAudioRoom(
            id = docId,
            schoolId = cleanSchoolId,
            title = cleanTitle,
            hostId = hostUser.id,
            hostName = hostUser.fullName.ifBlank { hostUser.role.arabicTitle },
            hostRole = hostUser.role.name,
            hostGender = hostUser.gender,
            hostAvatarUrl = hostUser.photoBase64,
            status = "live",
            createdAt = nowMs,
            createdAtIso = nowIso,
            startedAt = nowMs,
            endedAt = 0L,
            updatedAt = nowMs,
            targetAudience = audienceType.key,
            targetGrade = if (audienceType == LiveRoomAudienceType.CLASS) targetGrade.trim() else "",
            targetSection = if (audienceType == LiveRoomAudienceType.CLASS) targetSection.trim() else "",
            coHosts = coHosts.filter { it.isNotBlank() && it != hostUser.id }.distinct(),
            activeParticipants = if (hostUser.id.isNotBlank()) mapOf(hostUser.id to hostParticipant) else emptyMap(),
            participantIdsHistory = listOfNotNull(hostUser.id.takeIf { it.isNotBlank() })
        )

        val updated = (listOf(room) + _rooms.value.filterNot { it.id == room.id })
            .sortedByDescending { it.createdAt }
        _rooms.value = updated
        saveCachedRooms(context.applicationContext, cleanSchoolId, updated)

        persistRoomToCloud(cleanSchoolId, room)
        room
    }

    suspend fun recordParticipantJoined(
        context: Context,
        rawSchoolId: String,
        roomId: String,
        user: UserAccount
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        if (cleanSchoolId.isBlank() || roomId.isBlank() || user.id.isBlank()) return@withContext false
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext false
        if (!existing.isLive || existing.isUserBanned(user.id)) return@withContext false

        val nowMs = System.currentTimeMillis()
        val participant = LiveRoomParticipant(
            userId = user.id,
            fullName = user.fullName.ifBlank { user.role.arabicTitle },
            role = user.role.name,
            gender = user.gender,
            avatarUrl = user.photoBase64,
            joinedAt = existing.activeParticipants[user.id]?.joinedAt ?: nowMs
        )
        val updatedParticipants = existing.activeParticipants.toMutableMap().apply {
            put(user.id, participant)
        }
        val updatedHistory = (existing.participantIdsHistory + user.id).distinct()
        val updatedRoom = existing.copy(
            activeParticipants = updatedParticipants,
            participantIdsHistory = updatedHistory,
            updatedAt = nowMs
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, updatedRoom)
    }

    suspend fun recordParticipantLeft(
        context: Context,
        rawSchoolId: String,
        roomId: String,
        userId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        if (cleanSchoolId.isBlank() || roomId.isBlank() || userId.isBlank()) return@withContext false
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext false
        if (!existing.activeParticipants.containsKey(userId)) return@withContext true

        val updatedParticipants = existing.activeParticipants.toMutableMap().apply {
            remove(userId)
        }
        val updatedRoom = existing.copy(
            activeParticipants = updatedParticipants,
            updatedAt = System.currentTimeMillis()
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, updatedRoom)
    }

    suspend fun toggleMuteParticipant(
        context: Context,
        rawSchoolId: String,
        roomId: String,
        targetUserId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext false
        if (targetUserId.isBlank()) return@withContext false
        val alreadyMuted = existing.isUserMuted(targetUserId)
        val nextMuted = if (alreadyMuted) {
            existing.mutedUserIds.filterNot { it.equals(targetUserId, ignoreCase = true) }
        } else {
            (existing.mutedUserIds + targetUserId).distinct()
        }
        val updatedRoom = existing.copy(
            mutedUserIds = nextMuted,
            updatedAt = System.currentTimeMillis()
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, updatedRoom)
    }

    suspend fun kickParticipant(
        context: Context,
        rawSchoolId: String,
        roomId: String,
        targetUserId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext false
        if (targetUserId.isBlank()) return@withContext false
        val nowMs = System.currentTimeMillis()
        val nextKicked = existing.kickedUsers.toMutableMap().apply {
            put(targetUserId, nowMs)
        }
        val nextParticipants = existing.activeParticipants.toMutableMap().apply {
            remove(targetUserId)
        }
        val updatedRoom = existing.copy(
            activeParticipants = nextParticipants,
            kickedUsers = nextKicked,
            updatedAt = nowMs
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, updatedRoom)
    }

    suspend fun banParticipant(
        context: Context,
        rawSchoolId: String,
        roomId: String,
        targetUserId: String,
        targetFullName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext false
        if (targetUserId.isBlank()) return@withContext false
        val nowMs = System.currentTimeMillis()
        val nextBanned = (existing.bannedUserIds + targetUserId).distinct()
        val nextBannedInfo = existing.bannedUsersInfo.toMutableMap().apply {
            put(targetUserId, targetFullName.ifBlank { existing.activeParticipants[targetUserId]?.fullName ?: "مستخدم" })
        }
        val nextParticipants = existing.activeParticipants.toMutableMap().apply {
            remove(targetUserId)
        }
        val nextMuted = existing.mutedUserIds.filterNot { it.equals(targetUserId, ignoreCase = true) }
        val updatedRoom = existing.copy(
            activeParticipants = nextParticipants,
            bannedUserIds = nextBanned,
            bannedUsersInfo = nextBannedInfo,
            mutedUserIds = nextMuted,
            updatedAt = nowMs
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, updatedRoom)
    }

    suspend fun unbanParticipant(
        context: Context,
        rawSchoolId: String,
        roomId: String,
        targetUserId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext false
        if (targetUserId.isBlank()) return@withContext false
        val nextBanned = existing.bannedUserIds.filterNot { it.equals(targetUserId, ignoreCase = true) }
        val nextBannedInfo = existing.bannedUsersInfo.toMutableMap().apply {
            remove(targetUserId)
        }
        val updatedRoom = existing.copy(
            bannedUserIds = nextBanned,
            bannedUsersInfo = nextBannedInfo,
            updatedAt = System.currentTimeMillis()
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, updatedRoom)
    }

    suspend fun sendUrgentInvites(
        context: Context,
        rawSchoolId: String,
        roomId: String,
        targets: List<UserAccount>,
        senderName: String
    ): List<LiveRoomUrgentInvite> = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext emptyList()
        if (!existing.isLive || targets.isEmpty()) return@withContext emptyList()

        val nowMs = System.currentTimeMillis()
        val cleanSender = InputSanitizer.sanitize(senderName).ifBlank { "إدارة المدرسة" }
        val newInvites = targets.filter { it.id.isNotBlank() }.map { user ->
            LiveRoomUrgentInvite(
                id = "inv_${nowMs}_${UUID.randomUUID().toString().replace("-", "").take(6)}",
                roomId = existing.id,
                roomTitle = existing.title,
                targetUserId = user.id,
                targetUserName = user.fullName.ifBlank { user.role.arabicTitle },
                senderName = cleanSender,
                createdAt = nowMs
            )
        }
        val mergedInvites = (existing.urgentInvites + newInvites).takeLast(60)
        val updatedRoom = existing.copy(
            urgentInvites = mergedInvites,
            updatedAt = nowMs
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, updatedRoom)
        newInvites
    }

    suspend fun updateRoomCoHosts(
        context: Context,
        rawSchoolId: String,
        roomId: String,
        newCoHosts: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext false
        val distinctCoHosts = newCoHosts.filter { it.isNotBlank() && it != existing.hostId }.distinct()
        val updatedRoom = existing.copy(
            coHosts = distinctCoHosts,
            updatedAt = System.currentTimeMillis()
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, updatedRoom)
    }

    suspend fun endLiveRoom(
        context: Context,
        rawSchoolId: String,
        roomId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        val existing = _rooms.value.firstOrNull { it.id == roomId } ?: return@withContext false
        val nowMs = System.currentTimeMillis()
        val startMs = existing.effectiveStartedAt
        val endedAtMs = if (existing.endedAt > startMs) existing.endedAt else maxOf(nowMs, startMs + 1_000L)
        val finalizedHistory = buildList {
            if (existing.hostId.isNotBlank()) add(existing.hostId)
            addAll(existing.participantIdsHistory)
            addAll(existing.activeParticipants.keys)
        }.distinct()
        val endedRoom = existing.copy(
            status = "ended",
            startedAt = startMs,
            endedAt = endedAtMs,
            updatedAt = nowMs,
            activeParticipants = emptyMap(),
            participantIdsHistory = finalizedHistory
        )
        commitRoomLocallyAndCloud(context, cleanSchoolId, endedRoom)
    }

    /**
     * Permanently deletes a single ended broadcast from RTDB, Firestore, and local cache (Principal-only).
     */
    suspend fun deleteRoomFromHistory(
        context: Context,
        rawSchoolId: String,
        roomId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        if (cleanSchoolId.isBlank() || roomId.isBlank()) return@withContext false
        recentlyDeletedRoomIds.add(roomId)

        val remaining = _rooms.value.filterNot { it.id == roomId }
        _rooms.value = remaining
        saveCachedRooms(context.applicationContext, cleanSchoolId, remaining)

        coroutineScope {
            val rtdbDel = async { deleteRoomFromRtdb(cleanSchoolId, roomId) }
            val fsDel = async { deleteRoomFromFirestore(cleanSchoolId, roomId) }
            rtdbDel.await() || fsDel.await()
        }
    }

    /**
     * Permanently clears all ended broadcasts from history in RTDB, Firestore, and local cache (Principal-only).
     * Active live rooms are preserved untouched.
     */
    suspend fun clearAllEndedHistory(
        context: Context,
        rawSchoolId: String
    ): Int = withContext(Dispatchers.IO) {
        val cleanSchoolId = InputSanitizer.sanitizeSchoolId(rawSchoolId)
        if (cleanSchoolId.isBlank()) return@withContext 0

        val endedRooms = _rooms.value.filter { !it.isLive }
        if (endedRooms.isEmpty()) return@withContext 0

        endedRooms.forEach { recentlyDeletedRoomIds.add(it.id) }
        val remaining = _rooms.value.filter { it.isLive }
        _rooms.value = remaining
        saveCachedRooms(context.applicationContext, cleanSchoolId, remaining)

        coroutineScope {
            endedRooms.map { room ->
                async {
                    deleteRoomFromRtdb(cleanSchoolId, room.id)
                    deleteRoomFromFirestore(cleanSchoolId, room.id)
                }
            }.forEach { it.await() }
        }
        endedRooms.size
    }

    private suspend fun commitRoomLocallyAndCloud(
        context: Context,
        cleanSchoolId: String,
        updatedRoom: LiveAudioRoom
    ): Boolean {
        val updatedList = _rooms.value.map { if (it.id == updatedRoom.id) updatedRoom else it }
        _rooms.value = updatedList
        saveCachedRooms(context.applicationContext, cleanSchoolId, updatedList)
        return persistRoomToCloud(cleanSchoolId, updatedRoom)
    }

    private suspend fun persistRoomToCloud(cleanSchoolId: String, room: LiveAudioRoom): Boolean = coroutineScope {
        val rtdbOk = async { writeRoomToRtdb(cleanSchoolId, room) }
        val fsOk = async { writeRoomToFirestore(cleanSchoolId, room) }
        rtdbOk.await() || fsOk.await()
    }

    suspend fun fetchZegoConfig(context: Context): ZegoPlatformConfig = withContext(Dispatchers.IO) {
        loadCachedZegoConfig(context.applicationContext)
        val cloudConfig = coroutineScope {
            val fsDeferred = async { fetchZegoConfigFromFirestore() }
            val rtdbDeferred = async { fetchZegoConfigFromRtdb() }
            fsDeferred.await() ?: rtdbDeferred.await()
        }
        val resolved = if (cloudConfig != null && cloudConfig.appId > 0L && cloudConfig.serverSecret.isNotBlank()) {
            saveCachedZegoConfig(context.applicationContext, cloudConfig.appId, cloudConfig.serverSecret)
            cloudConfig.copy(isCustomFromCloud = true)
        } else {
            val cached = _zegoConfig.value
            if (cached.appId > 0L && cached.serverSecret.isNotBlank()) cached
            else ZegoPlatformConfig(DEFAULT_ZEGO_APP_ID, DEFAULT_ZEGO_SERVER_SECRET, isCustomFromCloud = false)
        }
        _zegoConfig.value = resolved
        resolved
    }

    suspend fun saveZegoConfig(
        context: Context,
        appId: Long,
        serverSecret: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSecret = serverSecret.trim()
        if (appId <= 0L || cleanSecret.isBlank()) return@withContext false
        val nowMs = System.currentTimeMillis()
        val rtdbUrl = "${FirebaseConfigConstants.DATABASE_URL}/platform_settings/zego.json"
        val rtdbPayload = JSONObject().apply {
            put("appId", appId)
            put("serverSecret", cleanSecret)
            put("updatedAt", nowMs)
        }.toString()

        val fsUrl =
            "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/platform_settings/zego?key=${FirebaseConfigConstants.API_KEY}"
        val fsPayload = JSONObject().apply {
            put("fields", JSONObject().apply {
                put("appId", JSONObject().put("integerValue", appId.toString()))
                put("serverSecret", JSONObject().put("stringValue", cleanSecret))
                put("updatedAt", JSONObject().put("integerValue", nowMs.toString()))
            })
        }.toString()

        val saved = coroutineScope {
            val rtdbOk = async { executeHttpWrite(rtdbUrl, "PUT", rtdbPayload) }
            val fsOk = async { executeHttpWrite(fsUrl, "PATCH", fsPayload) }
            rtdbOk.await() || fsOk.await()
        }
        saveCachedZegoConfig(context.applicationContext, appId, cleanSecret)
        _zegoConfig.value = ZegoPlatformConfig(
            appId = appId,
            serverSecret = cleanSecret,
            isCustomFromCloud = (appId != DEFAULT_ZEGO_APP_ID || cleanSecret != DEFAULT_ZEGO_SERVER_SECRET)
        )
        saved
    }

    private suspend fun runRealtimeSseLoop(context: Context, cleanSchoolId: String) {
        val sseUrl = "${FirebaseConfigConstants.DATABASE_URL}/schools/$cleanSchoolId/${FirebaseConfigConstants.COL_LIVE_ROOMS}.json"
        while (currentCoroutineContext().isActive && observedSchoolId == cleanSchoolId) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(sseUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 45_000
                    useCaches = false
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                if (conn.responseCode in 200..299) {
                    BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                        var currentEvent = ""
                        while (currentCoroutineContext().isActive && observedSchoolId == cleanSchoolId) {
                            val line = reader.readLine() ?: break
                            when {
                                line.startsWith("event:") -> currentEvent = line.substringAfter("event:").trim()
                                line.startsWith("data:") -> {
                                    val data = line.substringAfter("data:").trim()
                                    if (currentEvent == "put" || currentEvent == "patch") {
                                        handleRealtimeSseEvent(context, cleanSchoolId, data)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Reconnect smoothly after backoff
            } finally {
                conn?.disconnect()
            }
            if (currentCoroutineContext().isActive && observedSchoolId == cleanSchoolId) {
                delay(3_000L)
            }
        }
    }

    private fun handleRealtimeSseEvent(context: Context, cleanSchoolId: String, rawData: String) {
        runCatching {
            if (rawData.isBlank() || rawData == "null") return
            val envelope = JSONObject(rawData)
            val path = envelope.optString("path", "/")
            val dataVal = envelope.opt("data")
            if (path == "/" || path.isBlank()) {
                val parsed = parseRoomsMapFromRtdbValue(dataVal, cleanSchoolId)
                    .filterNot { it.id in recentlyDeletedRoomIds }
                val sorted = parsed.sortedByDescending { it.createdAt }
                _rooms.value = sorted
                saveCachedRooms(context, cleanSchoolId, sorted)
            } else {
                // Sub-path changed (e.g. "/room_123" or moderation/invite update) -> fast refresh
                scope.launch {
                    val refreshed = fetchRoomsFromRtdb(cleanSchoolId)
                    if (refreshed != null) {
                        val sorted = refreshed
                            .filterNot { it.id in recentlyDeletedRoomIds }
                            .sortedByDescending { it.createdAt }
                        _rooms.value = sorted
                        saveCachedRooms(context, cleanSchoolId, sorted)
                    } else {
                        refreshRoomsFromCloud(context, cleanSchoolId)
                    }
                }
            }
        }
    }

    private fun fetchRoomsFromRtdb(cleanSchoolId: String): List<LiveAudioRoom>? {
        val url = "${FirebaseConfigConstants.DATABASE_URL}/schools/$cleanSchoolId/${FirebaseConfigConstants.COL_LIVE_ROOMS}.json"
        val raw = executeHttpGet(url) ?: return null
        if (raw.isBlank() || raw.trim() == "null") return emptyList()
        return runCatching {
            val root = JSONObject(raw)
            parseRoomsMapFromRtdbValue(root, cleanSchoolId)
        }.getOrNull()
    }

    private fun parseRoomsMapFromRtdbValue(value: Any?, cleanSchoolId: String): List<LiveAudioRoom> {
        if (value !is JSONObject) return emptyList()
        val result = mutableListOf<LiveAudioRoom>()
        val keys = value.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val itemObj = value.optJSONObject(key) ?: continue
            LiveAudioRoom.fromJson(itemObj, fallbackId = key, fallbackSchoolId = cleanSchoolId)?.let {
                result.add(it)
            }
        }
        return result
    }

    private fun fetchRoomsFromFirestore(cleanSchoolId: String): List<LiveAudioRoom>? {
        val url =
            "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/schools/$cleanSchoolId/${FirebaseConfigConstants.COL_LIVE_ROOMS}?pageSize=100&key=${FirebaseConfigConstants.API_KEY}"
        val raw = executeHttpGet(url) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val docs = root.optJSONArray("documents") ?: return@runCatching emptyList()
            val result = mutableListOf<LiveAudioRoom>()
            for (i in 0 until docs.length()) {
                val doc = docs.optJSONObject(i) ?: continue
                val docName = doc.optString("name")
                val docId = docName.substringAfterLast("/").trim()
                val fields = doc.optJSONObject("fields") ?: continue
                parseRoomFromFirestoreFields(fields, docId, cleanSchoolId)?.let { result.add(it) }
            }
            result
        }.getOrNull()
    }

    private fun parseRoomFromFirestoreFields(
        fields: JSONObject,
        docId: String,
        cleanSchoolId: String
    ): LiveAudioRoom? {
        fun str(key: String, def: String = ""): String =
            fields.optJSONObject(key)?.optString("stringValue", def) ?: def

        fun lng(key: String, def: Long = 0L): Long {
            val f = fields.optJSONObject(key) ?: return def
            return f.optString("integerValue").toLongOrNull()
                ?: f.optLong("integerValue", def)
        }

        fun strArray(key: String): List<String> {
            val list = mutableListOf<String>()
            val values = fields.optJSONObject(key)
                ?.optJSONObject("arrayValue")
                ?.optJSONArray("values")
            if (values != null) {
                for (i in 0 until values.length()) {
                    val v = values.optJSONObject(i)?.optString("stringValue").orEmpty().trim()
                    if (v.isNotBlank()) list.add(v)
                }
            }
            return list.distinct()
        }

        val title = str("title").trim()
        if (docId.isBlank() || title.isBlank()) return null

        val participantsMap = LinkedHashMap<String, LiveRoomParticipant>()
        val rawParticipants = str("activeParticipantsJson")
        if (rawParticipants.isNotBlank()) {
            runCatching {
                val obj = JSONObject(rawParticipants)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val item = obj.optJSONObject(k) ?: continue
                    LiveRoomParticipant.fromJson(item, fallbackUserId = k)?.let { participantsMap[it.userId] = it }
                }
            }
        }

        val kickedMap = LinkedHashMap<String, Long>()
        val rawKicked = str("kickedUsersJson")
        if (rawKicked.isNotBlank()) {
            runCatching {
                val obj = JSONObject(rawKicked)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val ts = obj.optLong(k, 0L)
                    if (k.isNotBlank() && ts > 0L) kickedMap[k] = ts
                }
            }
        }

        val bannedInfoMap = LinkedHashMap<String, String>()
        val rawBannedInfo = str("bannedUsersInfoJson")
        if (rawBannedInfo.isNotBlank()) {
            runCatching {
                val obj = JSONObject(rawBannedInfo)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val name = obj.optString(k).trim()
                    if (k.isNotBlank() && name.isNotBlank()) bannedInfoMap[k] = name
                }
            }
        }

        val invitesList = mutableListOf<LiveRoomUrgentInvite>()
        val rawInvites = str("urgentInvitesJson")
        if (rawInvites.isNotBlank()) {
            runCatching {
                val arr = JSONArray(rawInvites)
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    LiveRoomUrgentInvite.fromJson(item)?.let { invitesList.add(it) }
                }
            }
        }

        val createdMs = lng("createdAt", System.currentTimeMillis()).takeIf { it > 0L } ?: System.currentTimeMillis()
        val startedMs = lng("startedAt", createdMs).takeIf { it > 0L } ?: createdMs
        val createdIso = str("createdAtIso").ifBlank {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(createdMs))
        }
        val hostId = str("hostId")
        val historyIds = strArray("participantIdsHistory").ifEmpty {
            buildList {
                if (hostId.isNotBlank()) add(hostId)
                addAll(participantsMap.keys)
            }.distinct()
        }

        return LiveAudioRoom(
            id = str("id", docId).ifBlank { docId },
            schoolId = str("schoolId", cleanSchoolId).ifBlank { cleanSchoolId },
            title = title,
            hostId = hostId,
            hostName = str("hostName"),
            hostRole = str("hostRole", "ADMIN"),
            hostGender = str("hostGender"),
            hostAvatarUrl = str("hostAvatarUrl"),
            status = str("status", "live").lowercase(Locale.ROOT),
            createdAt = createdMs,
            createdAtIso = createdIso,
            startedAt = startedMs,
            endedAt = lng("endedAt", 0L).coerceAtLeast(0L),
            updatedAt = lng("updatedAt", createdMs).coerceAtLeast(0L),
            targetAudience = str("targetAudience", "all").lowercase(Locale.ROOT),
            targetGrade = str("targetGrade"),
            targetSection = str("targetSection"),
            coHosts = strArray("coHosts"),
            activeParticipants = participantsMap,
            participantIdsHistory = historyIds,
            mutedUserIds = strArray("mutedUserIds"),
            kickedUsers = kickedMap,
            bannedUserIds = strArray("bannedUserIds"),
            bannedUsersInfo = bannedInfoMap,
            urgentInvites = invitesList
        )
    }

    private fun writeRoomToRtdb(cleanSchoolId: String, room: LiveAudioRoom): Boolean {
        val url =
            "${FirebaseConfigConstants.DATABASE_URL}/schools/$cleanSchoolId/${FirebaseConfigConstants.COL_LIVE_ROOMS}/${room.id}.json"
        return executeHttpWrite(url, "PUT", room.toJson().toString())
    }

    private fun deleteRoomFromRtdb(cleanSchoolId: String, roomId: String): Boolean {
        val url =
            "${FirebaseConfigConstants.DATABASE_URL}/schools/$cleanSchoolId/${FirebaseConfigConstants.COL_LIVE_ROOMS}/$roomId.json"
        return executeHttpDelete(url)
    }

    private fun writeRoomToFirestore(cleanSchoolId: String, room: LiveAudioRoom): Boolean {
        val url =
            "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/schools/$cleanSchoolId/${FirebaseConfigConstants.COL_LIVE_ROOMS}/${room.id}?key=${FirebaseConfigConstants.API_KEY}"

        fun toFsArray(list: List<String>): JSONObject = JSONObject().put(
            "arrayValue",
            JSONObject().put("values", JSONArray().apply {
                list.distinct().forEach { item -> put(JSONObject().put("stringValue", item)) }
            })
        )

        val activeParticipantsJson = JSONObject().apply {
            room.activeParticipants.forEach { (uid, p) -> put(uid, p.toJson()) }
        }.toString()
        val kickedUsersJson = JSONObject().apply {
            room.kickedUsers.forEach { (uid, ts) -> put(uid, ts) }
        }.toString()
        val bannedInfoJson = JSONObject().apply {
            room.bannedUsersInfo.forEach { (uid, name) -> put(uid, name) }
        }.toString()
        val urgentInvitesJson = JSONArray().apply {
            room.urgentInvites.forEach { put(it.toJson()) }
        }.toString()

        val body = JSONObject().apply {
            put("fields", JSONObject().apply {
                put("id", JSONObject().put("stringValue", room.id))
                put("schoolId", JSONObject().put("stringValue", room.schoolId))
                put("title", JSONObject().put("stringValue", room.title))
                put("hostId", JSONObject().put("stringValue", room.hostId))
                put("hostName", JSONObject().put("stringValue", room.hostName))
                put("hostRole", JSONObject().put("stringValue", room.hostRole))
                put("hostGender", JSONObject().put("stringValue", room.hostGender))
                put("hostAvatarUrl", JSONObject().put("stringValue", room.hostAvatarUrl))
                put("status", JSONObject().put("stringValue", room.status))
                put("createdAt", JSONObject().put("integerValue", room.createdAt.toString()))
                put("createdAtIso", JSONObject().put("stringValue", room.resolvedCreatedDateForDisplay()))
                put("startedAt", JSONObject().put("integerValue", room.effectiveStartedAt.toString()))
                if (room.endedAt > 0L) {
                    put("endedAt", JSONObject().put("integerValue", room.endedAt.toString()))
                }
                put("updatedAt", JSONObject().put("integerValue", room.updatedAt.takeIf { it > 0L }?.toString() ?: System.currentTimeMillis().toString()))
                put("targetAudience", JSONObject().put("stringValue", room.targetAudience))
                put("targetGrade", JSONObject().put("stringValue", room.targetGrade))
                put("targetSection", JSONObject().put("stringValue", room.targetSection))
                put("coHosts", toFsArray(room.coHosts))
                put("participantIdsHistory", toFsArray(room.participantIdsHistory))
                put("mutedUserIds", toFsArray(room.mutedUserIds))
                put("bannedUserIds", toFsArray(room.bannedUserIds))
                put("activeParticipantsJson", JSONObject().put("stringValue", activeParticipantsJson))
                put("kickedUsersJson", JSONObject().put("stringValue", kickedUsersJson))
                put("bannedUsersInfoJson", JSONObject().put("stringValue", bannedInfoJson))
                put("urgentInvitesJson", JSONObject().put("stringValue", urgentInvitesJson))
            })
        }.toString()
        return executeHttpWrite(url, "PATCH", body)
    }

    private fun deleteRoomFromFirestore(cleanSchoolId: String, roomId: String): Boolean {
        val url =
            "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/schools/$cleanSchoolId/${FirebaseConfigConstants.COL_LIVE_ROOMS}/$roomId?key=${FirebaseConfigConstants.API_KEY}"
        return executeHttpDelete(url)
    }

    private fun fetchZegoConfigFromFirestore(): ZegoPlatformConfig? {
        val url =
            "https://firestore.googleapis.com/v1/projects/${FirebaseConfigConstants.PROJECT_ID}/databases/(default)/documents/platform_settings/zego?key=${FirebaseConfigConstants.API_KEY}"
        val raw = executeHttpGet(url) ?: return null
        return runCatching {
            val fields = JSONObject(raw).optJSONObject("fields") ?: return@runCatching null
            val appIdObj = fields.optJSONObject("appId")
            val appId = appIdObj?.optString("integerValue")?.toLongOrNull()
                ?: appIdObj?.optString("stringValue")?.toLongOrNull()
                ?: 0L
            val secret = fields.optJSONObject("serverSecret")?.optString("stringValue").orEmpty().trim()
            if (appId > 0L && secret.isNotBlank()) {
                ZegoPlatformConfig(appId = appId, serverSecret = secret, isCustomFromCloud = true)
            } else null
        }.getOrNull()
    }

    private fun fetchZegoConfigFromRtdb(): ZegoPlatformConfig? {
        val url = "${FirebaseConfigConstants.DATABASE_URL}/platform_settings/zego.json"
        val raw = executeHttpGet(url) ?: return null
        if (raw.isBlank() || raw.trim() == "null") return null
        return runCatching {
            val obj = JSONObject(raw)
            val appId = obj.optLong("appId", 0L).takeIf { it > 0L }
                ?: obj.optString("appId").toLongOrNull()
                ?: 0L
            val secret = obj.optString("serverSecret").trim()
            if (appId > 0L && secret.isNotBlank()) {
                ZegoPlatformConfig(appId = appId, serverSecret = secret, isCustomFromCloud = true)
            } else null
        }.getOrNull()
    }

    private fun mergeRoomLists(
        rtdbRooms: List<LiveAudioRoom>?,
        fsRooms: List<LiveAudioRoom>?
    ): List<LiveAudioRoom>? {
        if (rtdbRooms == null && fsRooms == null) return null
        // When RTDB succeeded, treat RTDB as primary live truth for deletions of ended rooms
        val map = LinkedHashMap<String, LiveAudioRoom>()
        if (rtdbRooms != null && fsRooms != null) {
            val rtdbIds = rtdbRooms.map { it.id }.toSet()
            // Include Firestore rooms that exist in RTDB or are live
            fsRooms.forEach { fsRoom ->
                if (fsRoom.id in rtdbIds || fsRoom.isLive) {
                    map[fsRoom.id] = fsRoom
                }
            }
        } else {
            fsRooms.orEmpty().forEach { map[it.id] = it }
        }

        rtdbRooms.orEmpty().forEach { rtdbRoom ->
            val existing = map[rtdbRoom.id]
            if (existing == null) {
                map[rtdbRoom.id] = rtdbRoom
            } else {
                val fresher = if (rtdbRoom.updatedAt >= existing.updatedAt) rtdbRoom else existing
                val resolvedStatus = if (!rtdbRoom.isLive || !existing.isLive) "ended" else "live"
                val resolvedEndedAt = maxOf(rtdbRoom.endedAt, existing.endedAt)
                val mergedHistory = (rtdbRoom.participantIdsHistory + existing.participantIdsHistory).distinct()
                val mergedInvites = (rtdbRoom.urgentInvites + existing.urgentInvites)
                    .distinctBy { it.id }
                    .sortedBy { it.createdAt }
                    .takeLast(60)
                map[rtdbRoom.id] = fresher.copy(
                    status = resolvedStatus,
                    endedAt = resolvedEndedAt,
                    participantIdsHistory = mergedHistory,
                    urgentInvites = mergedInvites
                )
            }
        }
        return map.values.toList()
    }

    private fun executeHttpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7_000
                readTimeout = 7_000
                useCaches = false
                setRequestProperty("Cache-Control", "no-store, no-cache")
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

    private fun executeHttpWrite(urlStr: String, method: String, body: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun executeHttpDelete(urlStr: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8_000
                readTimeout = 8_000
                useCaches = false
            }
            val code = conn.responseCode
            code in 200..299 || code == 404
        } catch (_: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun loadCachedRooms(context: Context, cleanSchoolId: String): List<LiveAudioRoom> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("rooms_$cleanSchoolId", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    LiveAudioRoom.fromJson(obj, fallbackSchoolId = cleanSchoolId)?.let { add(it) }
                }
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    private fun saveCachedRooms(context: Context, cleanSchoolId: String, rooms: List<LiveAudioRoom>) {
        val arr = JSONArray().apply { rooms.forEach { put(it.toJson()) } }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("rooms_$cleanSchoolId", arr.toString())
            .apply()
    }

    private fun loadCachedZegoConfig(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appId = prefs.getLong(KEY_ZEGO_APP_ID, 0L)
        val secret = prefs.getString(KEY_ZEGO_SECRET, "").orEmpty().trim()
        if (appId > 0L && secret.isNotBlank()) {
            _zegoConfig.value = ZegoPlatformConfig(
                appId = appId,
                serverSecret = secret,
                isCustomFromCloud = (appId != DEFAULT_ZEGO_APP_ID || secret != DEFAULT_ZEGO_SERVER_SECRET)
            )
        }
    }

    private fun saveCachedZegoConfig(context: Context, appId: Long, secret: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_ZEGO_APP_ID, appId)
            .putString(KEY_ZEGO_SECRET, secret)
            .apply()
    }
}
