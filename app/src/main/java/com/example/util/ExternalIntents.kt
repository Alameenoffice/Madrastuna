package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast

object ExternalIntents {

    const val DEVELOPER_PHONE = ""
    const val DEVELOPER_WHATSAPP_URL = "https://wa.me/"

    /**
     * Triggers an immediate tactile haptic vibration (180ms) on Canteen POS purchases,
     * wallet recharges, or important action confirmations.
     */
    fun triggerCanteenHapticVibration(context: Context, durationMs: Long = 180L) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            durationMs,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
            // Ignore vibration failures on devices without vibrator hardware
        }
    }

    fun dialPhoneNumber(context: Context, phoneNumber: String = DEVELOPER_PHONE) {
        try {
            val cleanPhone = phoneNumber.trim()
            if (cleanPhone.isEmpty()) {
                Toast.makeText(context, "يرجى إدخال رقم الهاتف أولاً", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDeveloperWhatsApp(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_WHATSAPP_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendWhatsAppMessage(context: Context, rawPhone: String, message: String) {
        try {
            val digitsOnly = rawPhone.filter { it.isDigit() }
            val formattedPhone = when {
                digitsOnly.startsWith("964") -> digitsOnly
                digitsOnly.startsWith("07") -> "964" + digitsOnly.removePrefix("0")
                digitsOnly.isNotEmpty() -> "964$digitsOnly"
                else -> ""
            }
            val encodedMessage = Uri.encode(message)
            val url = if (formattedPhone.isNotEmpty()) {
                "https://wa.me/$formattedPhone?text=$encodedMessage"
            } else {
                "https://wa.me/?text=$encodedMessage"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تم تجهيز رسالة واتساب: $message", Toast.LENGTH_LONG).show()
        }
    }
}

