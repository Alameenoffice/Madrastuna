package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ExternalIntents {

    const val DEVELOPER_PHONE = "07512362567"
    const val DEVELOPER_WHATSAPP_URL = "https://wa.me/9647512362567"

    fun dialPhoneNumber(context: Context, phoneNumber: String = DEVELOPER_PHONE) {
        try {
            val cleanPhone = phoneNumber.trim()
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الاتصال لـ $phoneNumber", Toast.LENGTH_SHORT).show()
        }
    }

    fun openDeveloperWhatsApp(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_WHATSAPP_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح واتساب المطور", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendWhatsAppMessage(context: Context, rawPhone: String, message: String) {
        try {
            val digitsOnly = rawPhone.filter { it.isDigit() }
            val formattedPhone = when {
                digitsOnly.startsWith("964") -> digitsOnly
                digitsOnly.startsWith("07") -> "964" + digitsOnly.removePrefix("0")
                digitsOnly.isNotEmpty() -> "964$digitsOnly"
                else -> "9647512362567"
            }
            val encodedMessage = Uri.encode(message)
            val url = "https://wa.me/$formattedPhone?text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تم تجهيز رسالة واتساب: $message", Toast.LENGTH_LONG).show()
        }
    }
}
