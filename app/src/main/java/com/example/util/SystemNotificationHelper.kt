package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
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

    fun sanitize(input: String, maxLength: Int = 1000): String {
        return input
            .replace(dangerousPattern, "")
            .trim()
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
 * chat message, official report, or attendance/grade notification arrives.
 */
object SystemNotificationHelper {

    private const val CHANNEL_ID = "madrasati_realtime_alerts_channel"
    private const val CHANNEL_NAME = "إشعارات منصة مدرستي الفورية"
    private const val CHANNEL_DESC = "تنبيهات الدروس والواجبات والرسائل والدرجات والغيابات"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 280L, 140L, 280L)
                setSound(soundUri, null)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showSystemPushNotification(
        context: Context,
        title: String,
        body: String,
        tagId: String = ""
    ) {
        try {
            ensureChannelCreated(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

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

            val notifId = if (tagId.isNotEmpty()) tagId.hashCode() else Random.nextInt(10000, 99999)
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: Exception) {
            // Ignore security or runtime exceptions in restricted test environments
        }
    }
}
