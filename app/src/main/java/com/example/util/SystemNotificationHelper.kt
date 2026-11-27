package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
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
 * with alert sound and vibration whenever a new lesson/preparation, homework,
 * chat message, official report, attendance/grade alert, or live audio broadcast arrives.
 */
object SystemNotificationHelper {

    private const val CHANNEL_ID = "madrasati_realtime_alerts_channel"
    private const val CHANNEL_NAME = "إشعارات منصة مدرستي الفورية"
    private const val CHANNEL_DESC = "تنبيهات الدروس والواجبات والرسائل والدرجات والغيابات"

    private const val LIVE_BROADCAST_CHANNEL_ID = "madrasati_live_broadcast_channel"
    private const val LIVE_BROADCAST_CHANNEL_NAME = "إشعارات البث الصوتي المباشر"
    private const val LIVE_BROADCAST_CHANNEL_DESC = "تنبيهات انطلاق البثوث الصوتية المباشرة ودعوات الدخول العاجلة"

    private const val DEDUP_PREFS_NAME = "madrasati_notification_dedup_prefs"
    private const val KEY_DISPATCHED_KEYS = "dispatched_broadcast_notif_keys"
    private const val KEY_ASKED_NOTIF_PERMISSION = "asked_post_notifications_rationale_v1"

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

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val mainChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 280L, 140L, 280L)
                setSound(soundUri, null)
            }
            val broadcastChannel = NotificationChannel(
                LIVE_BROADCAST_CHANNEL_ID,
                LIVE_BROADCAST_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = LIVE_BROADCAST_CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 240L, 120L, 240L)
                setSound(soundUri, null)
            }
            manager?.createNotificationChannel(mainChannel)
            manager?.createNotificationChannel(broadcastChannel)
        }
    }

    private fun isKeyAlreadyNotified(context: Context, dedupKey: String): Boolean {
        if (dedupKey.isBlank()) return false
        val prefs = context.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_DISPATCHED_KEYS, emptySet()).orEmpty()
        return dedupKey in existing
    }

    private fun markKeyNotified(context: Context, dedupKey: String) {
        if (dedupKey.isBlank()) return
        val prefs = context.getSharedPreferences(DEDUP_PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_DISPATCHED_KEYS, emptySet()).orEmpty().toMutableList()
        if (dedupKey !in existing) {
            existing.add(dedupKey)
            // Keep bounded size to prevent unbounded SharedPreferences growth
            val trimmed = if (existing.size > 160) existing.takeLast(120) else existing
            prefs.edit().putStringSet(KEY_DISPATCHED_KEYS, trimmed.toSet()).apply()
        }
    }

    /**
     * Shows an external Android system notification for a Live Audio Broadcast start or Urgent Join Invite,
     * with strict persisted deduplication so the exact same broadcast/invite never fires twice.
     */
    fun showLiveBroadcastNotificationOnce(
        context: Context,
        dedupKey: String,
        title: String,
        body: String,
        isUrgentInvite: Boolean = false
    ): Boolean {
        if (dedupKey.isNotBlank() && isKeyAlreadyNotified(context, dedupKey)) {
            return false
        }
        try {
            ensureChannelCreated(context)
            if (!hasNotificationPermission(context)) {
                return false
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_live_broadcast", true)
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val requestCode = (dedupKey.ifBlank { title }.hashCode() and 0x7FFFFFFF)
            val pendingIntent = PendingIntent.getActivity(context, requestCode, launchIntent, pendingFlags)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val cleanTitle = InputSanitizer.stripEmojis(title).ifBlank { "بث صوتي مباشر" }
            val cleanBody = InputSanitizer.stripEmojis(body)

            val builder = NotificationCompat.Builder(context, LIVE_BROADCAST_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(cleanTitle)
                .setContentText(cleanBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cleanBody))
                .setPriority(
                    if (isUrgentInvite) NotificationCompat.PRIORITY_MAX
                    else NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                    if (isUrgentInvite) NotificationCompat.CATEGORY_CALL
                    else NotificationCompat.CATEGORY_EVENT
                )
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(defaultSoundUri)
                .setVibrate(
                    if (isUrgentInvite) longArrayOf(0L, 260L, 120L, 260L, 120L, 260L)
                    else longArrayOf(0L, 220L, 120L, 220L)
                )
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
                .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        showSystemPushNotification(
            context = context,
            title = title,
            body = urgentMessage.trim(),
            tagId = "urgent_${signatureKey.ifBlank { urgentMessage.hashCode().toString() }}"
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
