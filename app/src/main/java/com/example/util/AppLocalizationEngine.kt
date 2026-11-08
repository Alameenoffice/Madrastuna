package com.example.util

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * المنصة تعتمد اللغة العربية الفصحى حصراً وباتجاه RTL ثابت (من اليمين إلى اليسار).
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val isRtl: Boolean
) {
    ARABIC("ar", "العربية", true);

    companion object {
        fun fromCode(code: String?): AppLanguage = ARABIC
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ARABIC }

object AppLocalizationEngine {
    var currentLanguageState by mutableStateOf(AppLanguage.ARABIC)
        private set

    val currentLanguage: AppLanguage
        get() = AppLanguage.ARABIC

    fun init(context: Context) {
        currentLanguageState = AppLanguage.ARABIC
    }

    fun localize(rawText: String, language: AppLanguage = AppLanguage.ARABIC): String = rawText
}
