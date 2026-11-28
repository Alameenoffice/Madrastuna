package com.example.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AppSection
import kotlin.random.Random

/**
 * Input Sanitizer & Session Validator utility to guarantee clean text inputs
 * and prevent script/injection characters before saving to local or Firebase storage.
 */
object InputSanitizer {
    private val dangerousPattern = Regex("[<>\"'`\\\\;\\x00-\\x1F]")
    private val classicEmojiPattern = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+|[\\u2600-\\u27BF\\uFE0F]")

    fun stripEmojis(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input
            .replace("🔒", "")
            .replace(classicEmojiPattern, "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    fun sanitize(input: String, maxLength: Int = 1000): String {
        return stripEmojis(input.replace(dangerousPattern, ""))
            .take(maxLength)
    }

    fun sanitizeUsername(input: String): String {
        return input
            .replace(Regex("[^A-Za-z0-9_.\\-@\\u0600-\\u06FF]"), "")
            .trim()
            .take(60)
    }

    fun sanitizeSchoolId(input: String): String {
        return input
            .replace(Regex("[^A-Za-z0-9_\\-]"), "")
            .trim()
            .take(64)
    }
}

/**
 * Fires external Android System Push Notifications in the phone's top status bar
 * with High-Importance channels, sound, vibration, programmatic vector BigPictureStyle banners,
 * direct action buttons ("انضمام للبث الآن"), and persisted deduplication.
 */
object SystemNotificationHelper {

    const val EXTRA_TARGET_SECTION = "extra_target_section"
    const val EXTRA_LIVE_ROOM_ID = "extra_live_room_id"
    const val EXTRA_AUTO_ENTER_ROOM = "extra_auto_enter_room"

    private const val CHANNEL_ID = "madrasati_realtime_alerts_channel"
    private const val CHANNEL_NAME = "إشعارات منصة مدرستي الفورية"
    private const val CHANNEL_DESC = "تنبيهات الدروس والواجبات والرسائل والدرجات والغيابات"

    private const val LIVE_BROADCAST_CHANNEL_ID = "madrasati_live_broadcast_channel"
    private const val LIVE_BROADCAST_CHANNEL_NAME = "إشعارات البث الصوتي المباشر"
    private const val LIVE_BROADCAST_CHANNEL_DESC = "تنبيهات انطلاق البثوث الصوتية المباشرة ودعوات الدخول العاجلة"

    private const val ANNOUNCEMENTS_CHANNEL_ID = "madrasati_announcements_channel"
    private const val ANNOUNCEMENTS_CHANNEL_NAME = "تعميمات الإدارة والمنصة"
    private const val ANNOUNCEMENTS_CHANNEL_DESC = "تبليغات إدارة المدرسة العاجلة وتعميمات المنصة الرسمية"

    private const val DEDUP_PREFS_NAME = "madrasati_notification_dedup_prefs"
    private const val KEY_DISPATCHED_KEYS = "dispatched_broadcast_notif_keys"
    private const val KEY_ASKED_NOTIF_PERMISSION = "asked_post_notifications_rationale_v1"
    private const val KEY_ASKED_BATTERY_OPT = "asked_battery_optimization_guide_v1"

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAskedNotificationPermission(context: Context): Boolean {
        return context.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ASKED_NOTIF_PERMISSION, false)
    }

    fun markAskedNotificationPermission(context: Context) {
        context.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ASKED_NOTIF_PERMISSION, true)
            .apply()
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } catch (_: Exception) {
            true
        }
    }

    fun shouldShowBatteryOptimizationPrompt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        if (isIgnoringBatteryOptimizations(context)) return false
        return !context.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ASKED_BATTERY_OPT, false)
    }

    fun markBatteryOptimizationPromptShown(context: Context) {
        context.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ASKED_BATTERY_OPT, true)
            .apply()
    }

    /**
     * Opens Android's standard Battery Optimization settings screen cleanly (Play Policy compliant,
     * requires zero restricted permissions) so the user can allow unrestricted background alerts.
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return runCatching {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val mainChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 280L, 140L, 280L)
                setSound(soundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val broadcastChannel = NotificationChannel(
                LIVE_BROADCAST_CHANNEL_ID,
                LIVE_BROADCAST_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = LIVE_BROADCAST_CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 240L, 120L, 240L, 120L, 240L)
                setSound(soundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val announcementsChannel = NotificationChannel(
                ANNOUNCEMENTS_CHANNEL_ID,
                ANNOUNCEMENTS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ANNOUNCEMENTS_CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 260L, 120L, 260L)
                setSound(soundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager?.createNotificationChannel(mainChannel)
            manager?.createNotificationChannel(broadcastChannel)
            manager?.createNotificationChannel(announcementsChannel)
        }
    }

    fun isKeyAlreadyNotified(context: Context, dedupKey: String): Boolean {
        if (dedupKey.isBlank()) return false
        val prefs = context.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_DISPATCHED_KEYS, emptySet()).orEmpty()
        return dedupKey in existing
    }

    fun markKeyNotified(context: Context, dedupKey: String) {
        if (dedupKey.isBlank()) return
        val prefs = context.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_DISPATCHED_KEYS, emptySet()).orEmpty().toMutableList()
        if (dedupKey !in existing) {
            existing.add(dedupKey)
            val trimmed = if (existing.size > 200) existing.takeLast(150) else existing
            prefs.edit().putStringSet(KEY_DISPATCHED_KEYS, trimmed.toSet()).apply()
        }
    }

    /**
     * Shows an external Android system notification for a Live Audio Broadcast start or Urgent Join Invite,
     * complete with programmatic emerald-gold vector BigPictureStyle banner, large icon, direct
     * "انضمام للبث الآن" deep-link action button, and strict persisted deduplication.
     */
    fun showLiveBroadcastNotificationOnce(
        context: Context,
        dedupKey: String,
        title: String,
        body: String,
        isUrgentInvite: Boolean = false,
        roomId: String = "",
        roomTitle: String = "",
        hostName: String = ""
    ): Boolean {
        if (dedupKey.isNotBlank() && isKeyAlreadyNotified(context, dedupKey)) {
            return false
        }
        try {
            ensureChannelCreated(context)
            if (!hasNotificationPermission(context)) {
                return false
            }

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val baseReqCode = (dedupKey.ifBlank { title }.hashCode() and 0x3FFFFFFF)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_live_broadcast", true)
                putExtra(EXTRA_TARGET_SECTION, AppSection.LIVE_AUDIO_BROADCAST.name)
                if (roomId.isNotBlank()) {
                    putExtra(EXTRA_LIVE_ROOM_ID, roomId)
                    putExtra(EXTRA_AUTO_ENTER_ROOM, true)
                }
            }
            val contentPendingIntent = PendingIntent.getActivity(context, baseReqCode, launchIntent, pendingFlags)

            val joinActionIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_live_broadcast", true)
                putExtra(EXTRA_TARGET_SECTION, AppSection.LIVE_AUDIO_BROADCAST.name)
                putExtra(EXTRA_LIVE_ROOM_ID, roomId)
                putExtra(EXTRA_AUTO_ENTER_ROOM, true)
            }
            val joinActionPendingIntent = PendingIntent.getActivity(
                context,
                baseReqCode + 1,
                joinActionIntent,
                pendingFlags
            )

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val cleanTitle = InputSanitizer.stripEmojis(title).ifBlank { "بث صوتي مباشر الآن" }
            val cleanBody = InputSanitizer.stripEmojis(body)

            val resolvedBannerTitle = InputSanitizer.stripEmojis(
                roomTitle.ifBlank { cleanBody.substringBefore("•").trim().ifBlank { cleanTitle } }
            )
            val resolvedHostName = InputSanitizer.stripEmojis(
                hostName.ifBlank {
                    cleanBody.substringAfter("بإشراف", "").trim().ifBlank { "إدارة المدرسة" }
                }
            )

            val bannerBitmap: Bitmap? = runCatching {
                LiveBroadcastBannerBitmapRenderer.renderLiveBroadcastBanner(
                    title = resolvedBannerTitle,
                    hostName = resolvedHostName,
                    isUrgentInvite = isUrgentInvite
                )
            }.getOrNull()

            val largeIconBitmap: Bitmap? = runCatching {
                LiveBroadcastBannerBitmapRenderer.renderLiveBroadcastLargeIcon(
                    isUrgentInvite = isUrgentInvite
                )
            }.getOrNull()

            val builder = NotificationCompat.Builder(context, LIVE_BROADCAST_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(cleanTitle)
                .setContentText(cleanBody)
                .setColor(0xFF047857.toInt())
                .setPriority(
                    if (isUrgentInvite) NotificationCompat.PRIORITY_MAX
                    else NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    if (isUrgentInvite) NotificationCompat.CATEGORY_CALL
                    else NotificationCompat.CATEGORY_EVENT
                )
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSoundUri)
                .setVibrate(
                    if (isUrgentInvite) longArrayOf(0L, 260L, 120L, 260L, 120L, 260L)
                    else longArrayOf(0L, 220L, 120L, 220L)
                )
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(0, "انضمام للبث الآن", joinActionPendingIntent)

            if (largeIconBitmap != null) {
                builder.setLargeIcon(largeIconBitmap)
            }
            if (bannerBitmap != null) {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bannerBitmap)
                        .bigLargeIcon(null as Bitmap?)
                        .setBigContentTitle(cleanTitle)
                        .setSummaryText(cleanBody)
                )
            } else {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(cleanBody))
            }

            val notifId = if (dedupKey.isNotBlank()) dedupKey.hashCode() else Random.nextInt(10000, 99999)
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            if (dedupKey.isNotBlank()) {
                markKeyNotified(context, dedupKey)
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Shows a deduplicated high-importance external notification for Developer/Platform Announcements
     * or Principal Urgent School Alerts (works both in foreground and from background JobService).
     */
    fun showAnnouncementNotificationOnce(
        context: Context,
        dedupKey: String,
        title: String,
        body: String,
        isUrgentAdmin: Boolean = false
    ): Boolean {
        if (dedupKey.isNotBlank() && isKeyAlreadyNotified(context, dedupKey)) {
            return false
        }
        try {
            ensureChannelCreated(context)
            if (!hasNotificationPermission(context)) return false

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_SECTION, AppSection.DASHBOARD.name)
            }
            val reqCode = (dedupKey.ifBlank { title }.hashCode() and 0x3FFFFFFF)
            val pendingIntent = PendingIntent.getActivity(context, reqCode, launchIntent, pendingFlags)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val cleanTitle = InputSanitizer.stripEmojis(title).ifBlank {
                if (isUrgentAdmin) "تبليغ عاجل من إدارة المدرسة" else "تعميم إدارة المنصة"
            }
            val cleanBody = InputSanitizer.stripEmojis(body)

            val builder = NotificationCompat.Builder(context, ANNOUNCEMENTS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(cleanTitle)
                .setContentText(cleanBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cleanBody))
                .setColor(0xFF047857.toInt())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0L, 260L, 120L, 260L))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notifId = if (dedupKey.isNotBlank()) dedupKey.hashCode() else Random.nextInt(10000, 99999)
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            if (dedupKey.isNotBlank()) {
                markKeyNotified(context, dedupKey)
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Plays a gentle, non-intrusive chime when an urgent in-app broadcast invite arrives.
     */
    fun playGentleInviteChime(context: Context) {
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 240)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { toneGen.release() }
            }, 380L)
        }.onFailure {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                ringtone?.play()
            }
        }
    }

    fun showSystemPushNotification(
        context: Context,
        title: String,
        body: String,
        tagId: String = "",
        notificationId: String = ""
    ) {
        try {
            ensureChannelCreated(context)

            if (!hasNotificationPermission(context)) return

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingFlags)

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setColor(0xFF047857.toInt())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0L, 280L, 140L, 280L))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val effectiveKey = notificationId.ifBlank { tagId }
            val notifId = if (effectiveKey.isNotEmpty()) effectiveKey.hashCode() else Random.nextInt(10000, 99999)
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: Exception) {
            // Ignore security or runtime exceptions in restricted test environments
        }
    }

    fun showUrgentExternalNotification(
        context: Context,
        schoolName: String,
        urgentMessage: String,
        signatureKey: String
    ) {
        val cleanSchool = schoolName.trim().ifBlank { "المدرسة" }
        val title = "تنبيه عاجل من إدارة $cleanSchool"
        showAnnouncementNotificationOnce(
            context = context,
            dedupKey = "urgent_${signatureKey.ifBlank { urgentMessage.hashCode().toString() }}",
            title = title,
            body = urgentMessage.trim(),
            isUrgentAdmin = true
        )
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        android.os.VibrationEffect.createWaveform(
                            longArrayOf(0L, 300L, 120L, 300L),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0L, 300L, 120L, 300L), -1)
                }
            }
        } catch (_: Exception) {
        }
    }
}
