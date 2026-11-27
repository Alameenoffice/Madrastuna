package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * قائمة الأيقونات الموسمية الـ 6 لسطح المكتب وشاشة الإقلاع والشريط العلوي:
 * 1. الأيقونة الأكاديمية الملكية (الافتراضية الرسمية)
 * 2. أيقونة شهر رمضان المبارك (هلال وفانوس ذهبي ملكي)
 * 3. أيقونة رأس السنة الميلادية (تصميم احتفالي شتوي مع سنة 2027)
 * 4. أيقونة عيد الفطر وعيد الأضحى المبارك
 * 5. أيقونة المولد النبوي الشريف
 * 6. أيقونة يوم العلم / العيد الوطني العراقي
 */
enum class SeasonalDesktopIconPreset(
    val key: String,
    val arabicTitle: String,
    val arabicSubtitle: String,
    val aliasSuffix: String
) {
    OFFICIAL_ROYAL(
        key = "OFFICIAL_ROYAL",
        arabicTitle = "الأيقونة الأكاديمية الملكية (الافتراضية الرسمية)",
        arabicSubtitle = "الدرع الأكاديمي الملكي الذهبي مع قبعة التخرج والكتاب المفتوح",
        aliasSuffix = ".MainActivityDefault"
    ),
    RAMADAN_KARIM(
        key = "RAMADAN_KARIM",
        arabicTitle = "أيقونة شهر رمضان المبارك (هلال وفانوس ذهبي ملكي)",
        arabicSubtitle = "هلال ذهبي ملكي وفانوس مضيء بنفحات الشهر الفضيل",
        aliasSuffix = ".MainActivityRamadan"
    ),
    NEW_YEAR_2027(
        key = "NEW_YEAR_2027",
        arabicTitle = "أيقونة رأس السنة الميلادية (تصميم احتفالي شتوي مع سنة 2027)",
        arabicSubtitle = "تصميم احتفالي شتوي ملكي متألق مع شارة عام 2027",
        aliasSuffix = ".MainActivityNewYear"
    ),
    EID_MUBARAK(
        key = "EID_MUBARAK",
        arabicTitle = "أيقونة عيد الفطر وعيد الأضحى المبارك",
        arabicSubtitle = "زخرفة العيد الذهبية المبهجة مع الهلال والنجمة المتوهجة",
        aliasSuffix = ".MainActivityHonor"
    ),
    MAWLID_NABAWI(
        key = "MAWLID_NABAWI",
        arabicTitle = "أيقونة المولد النبوي الشريف",
        arabicSubtitle = "القبة الخضراء المنيرة والزخرفة الإسلامية الذهبية والكتاب المشرق",
        aliasSuffix = ".MainActivityMawlid"
    ),
    IRAQ_NATIONAL_DAY(
        key = "IRAQ_NATIONAL_DAY",
        arabicTitle = "أيقونة يوم العلم / العيد الوطني العراقي",
        arabicSubtitle = "الدرع الوطني الذهبي الموشح بألوان العلم العراقي وتاج الشموخ",
        aliasSuffix = ".MainActivityNational"
    );

    companion object {
        fun fromKeyOrAlias(raw: String?): SeasonalDesktopIconPreset {
            val clean = raw?.trim().orEmpty()
            entries.firstOrNull {
                it.key.equals(clean, ignoreCase = true) ||
                    it.aliasSuffix.equals(clean, ignoreCase = true)
            }?.let { return it }

            val lower = clean.lowercase(Locale.ROOT)
            return when {
                lower.contains("ramadan") -> RAMADAN_KARIM
                lower.contains("newyear") || lower.contains("new_year") || lower.contains("2027") -> NEW_YEAR_2027
                lower.contains("eid") || lower.contains("honor") -> EID_MUBARAK
                lower.contains("mawlid") -> MAWLID_NABAWI
                lower.contains("national") || lower.contains("iraq") || lower.contains("flag") -> IRAQ_NATIONAL_DAY
                else -> OFFICIAL_ROYAL
            }
        }
    }
}

/**
 * Native Android Activity-Alias & Dynamic Seasonal Icon Switcher.
 * Safely enables the selected occasion launcher alias and disables all other aliases
 * using PackageManager.DONT_KILL_APP, generates 512x512 & 192x192 PNG icons dynamically,
 * and updates the Splash Screen & Top Header live state.
 */
object AppIconSwitcherHelper {

    private const val PREFS_NAME = "madrasati_seasonal_icon_prefs"
    private const val KEY_ACTIVE_SEASONAL_ICON = "active_seasonal_icon_key"

    private val OCCASION_ALIASES = listOf(
        ".MainActivityDefault",
        ".MainActivityRamadan",
        ".MainActivityNewYear",
        ".MainActivityHonor",
        ".MainActivityMawlid",
        ".MainActivityNational"
    )

    var activeSeasonalPreset by mutableStateOf(SeasonalDesktopIconPreset.OFFICIAL_ROYAL)
        private set

    fun init(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_ACTIVE_SEASONAL_ICON, null)
            if (!saved.isNullOrBlank()) {
                activeSeasonalPreset = SeasonalDesktopIconPreset.fromKeyOrAlias(saved)
            }
        }
    }

    fun resolveAliasSuffix(themeKeyOrAlias: String): String {
        return SeasonalDesktopIconPreset.fromKeyOrAlias(themeKeyOrAlias).aliasSuffix
    }

    /**
     * يفعّل الأيقونة الموسمية المختارة بشكل دائم على سطح المكتب وشاشة الإقلاع والشريط العلوي
     * مع توليد نسخ عالية الدقة 512x512 و 192x192 PNG.
     */
    fun activateSeasonalDesktopIcon(context: Context, preset: SeasonalDesktopIconPreset) {
        activeSeasonalPreset = preset
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ACTIVE_SEASONAL_ICON, preset.key).apply()
        }
        generateDynamicHighResPngIcons(context, preset)
        applyAliasComponentState(context, preset.aliasSuffix)
    }

    fun changeLauncherIcon(context: Context, themeKey: String) {
        init(context)
        // إذا كان المطور قد اختار أيقونة موسمية محفوظة في التفضيلات، نحترم اختياره الدائم،
        // وإلا نحدّث وفق الـ themeKey الممرر.
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_ACTIVE_SEASONAL_ICON, null)
        val effectivePreset = if (!savedKey.isNullOrBlank() && (themeKey == "default" || themeKey.isBlank() || themeKey == "OFFICIAL")) {
            SeasonalDesktopIconPreset.fromKeyOrAlias(savedKey)
        } else {
            SeasonalDesktopIconPreset.fromKeyOrAlias(themeKey)
        }
        activeSeasonalPreset = effectivePreset
        applyAliasComponentState(context, effectivePreset.aliasSuffix)
    }

    private fun applyAliasComponentState(context: Context, targetAliasSuffix: String) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName

            val mainComp = ComponentName(pkg, "$pkg.MainActivity")
            if (pm.getComponentEnabledSetting(mainComp) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            val targetComp = ComponentName(pkg, "$pkg$targetAliasSuffix")
            if (pm.getComponentEnabledSetting(targetComp) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    targetComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            OCCASION_ALIASES.forEach { aliasSuffix ->
                if (aliasSuffix != targetAliasSuffix) {
                    val comp = ComponentName(pkg, "$pkg$aliasSuffix")
                    if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun generateDynamicHighResPngIcons(context: Context, preset: SeasonalDesktopIconPreset) {
        runCatching {
            val iconsDir = File(context.filesDir, "dynamic_launcher_icons")
            if (!iconsDir.exists()) iconsDir.mkdirs()
            listOf(192, 512).forEach { dim ->
                val bmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                paint.color = when (preset) {
                    SeasonalDesktopIconPreset.NEW_YEAR_2027 -> android.graphics.Color.parseColor("#172554")
                    else -> android.graphics.Color.parseColor("#042F24")
                }
                canvas.drawRect(0f, 0f, dim.toFloat(), dim.toFloat(), paint)
                val outFile = File(iconsDir, "icon_${preset.key.lowercase(Locale.ROOT)}_${dim}x${dim}.png")
                FileOutputStream(outFile).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                bmp.recycle()
            }
        }
    }

    fun setAppIcon(context: Context, aliasOrThemeKey: String) {
        val preset = SeasonalDesktopIconPreset.fromKeyOrAlias(aliasOrThemeKey)
        activateSeasonalDesktopIcon(context, preset)
    }
}

/**
 * معاينة متجهة فائقة الدقة (Ultra-HD Pure Vector Canvas) لأي أيقونة من الأيقونات الموسمية الـ 6،
 * تستخدم في بطاقات إعدادات المطور وفي الشريط العلوي وشاشة الإقلاع عند اختيار مناسبة موسمية.
 */
@Composable
fun SeasonalDesktopIconVectorBadge(
    preset: SeasonalDesktopIconPreset,
    size: Dp = 68.dp,
    cornerRadius: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val bgColors = when (preset) {
        SeasonalDesktopIconPreset.NEW_YEAR_2027 -> listOf(
            Color(0xFF1E3A8A),
            Color(0xFF172554),
            Color(0xFF090D16)
        )
        SeasonalDesktopIconPreset.RAMADAN_KARIM -> listOf(
            Color(0xFF065F46),
            Color(0xFF042F24),
            Color(0xFF011510)
        )
        else -> listOf(
            Color(0xFF0F705B),
            Color(0xFF0A4F40),
            Color(0xFF03221B)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.radialGradient(colors = bgColors))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF3B0),
                        Color(0xFFD4AF37),
                        Color(0xFF9A6F18)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSeasonalIconVectorContent(preset)
        }
    }
}

fun DrawScope.drawSeasonalIconVectorContent(preset: SeasonalDesktopIconPreset) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val u = size.minDimension / 108f

    // حلقة ذهبية ملكية خارجية موحدة
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xAAFFF3B0), Color(0x66D4AF37), Color(0x999A6F18)),
            start = Offset(w * 0.15f, h * 0.15f),
            end = Offset(w * 0.85f, h * 0.85f)
        ),
        radius = 38f * u,
        center = Offset(cx, cy),
        style = Stroke(width = 1.4f * u)
    )

    when (preset) {
        SeasonalDesktopIconPreset.OFFICIAL_ROYAL -> {
            // الدرع الأكاديمي الملكي + قبعة التخرج + الكتاب المفتوح بدقة متجهة حادة
            val shield = Path().apply {
                moveTo(cx, cy - 32f * u)
                lineTo(cx + 27f * u, cy - 24f * u)
                lineTo(cx + 27f * u, cy - 1f * u)
                cubicTo(cx + 27f * u, cy + 15f * u, cx + 15f * u, cy + 26f * u, cx, cy + 32f * u)
                cubicTo(cx - 15f * u, cy + 26f * u, cx - 27f * u, cy + 15f * u, cx - 27f * u, cy - 1f * u)
                lineTo(cx - 27f * u, cy - 24f * u)
                close()
            }
            drawPath(
                path = shield,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF3B0), Color(0xFFD4AF37), Color(0xFF9A6F18)),
                    start = Offset(cx - 27f * u, cy - 32f * u),
                    end = Offset(cx + 27f * u, cy + 32f * u)
                )
            )
            val innerShield = Path().apply {
                moveTo(cx, cy - 28f * u)
                lineTo(cx + 23f * u, cy - 21f * u)
                lineTo(cx + 23f * u, cy - 1f * u)
                cubicTo(cx + 23f * u, cy + 13f * u, cx + 13f * u, cy + 22f * u, cx, cy + 28f * u)
                cubicTo(cx - 13f * u, cy + 22f * u, cx - 23f * u, cy + 13f * u, cx - 23f * u, cy - 1f * u)
                lineTo(cx - 23f * u, cy - 21f * u)
                close()
            }
            drawPath(
                path = innerShield,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF117A63), Color(0xFF084C3D), Color(0xFF03261E)),
                    center = Offset(cx, cy - 4f * u),
                    radius = 30f * u
                )
            )
            // الكتاب المفتوح
            val bookPath = Path().apply {
                moveTo(cx, cy + 4f * u)
                cubicTo(cx - 7f * u, cy + 1f * u, cx - 14f * u, cy + 1f * u, cx - 18f * u, cy + 2.5f * u)
                lineTo(cx - 17f * u, cy + 15f * u)
                cubicTo(cx - 12f * u, cy + 13.5f * u, cx - 6f * u, cy + 14f * u, cx, cy + 17f * u)
                cubicTo(cx + 6f * u, cy + 14f * u, cx + 12f * u, cy + 13.5f * u, cx + 17f * u, cy + 15f * u)
                lineTo(cx + 18f * u, cy + 2.5f * u)
                cubicTo(cx + 14f * u, cy + 1f * u, cx + 7f * u, cy + 1f * u, cx, cy + 4f * u)
                close()
            }
            drawPath(path = bookPath, color = Color(0xFFFFFBEB))
            drawLine(
                color = Color(0xFFD4AF37),
                start = Offset(cx, cy + 3.5f * u),
                end = Offset(cx, cy + 17.5f * u),
                strokeWidth = 1.6f * u,
                cap = StrokeCap.Round
            )
            // قبعة التخرج الملكية
            val capTop = Path().apply {
                moveTo(cx, cy - 24f * u)
                lineTo(cx + 22f * u, cy - 15f * u)
                lineTo(cx, cy - 6f * u)
                lineTo(cx - 22f * u, cy - 15f * u)
                close()
            }
            drawPath(path = capTop, color = Color(0xFF062820))
            drawPath(
                path = capTop,
                color = Color(0xFFFDE047),
                style = Stroke(width = 1.3f * u, join = StrokeJoin.Round)
            )
            drawCircle(color = Color(0xFFFDE047), radius = 2.2f * u, center = Offset(cx, cy - 15f * u))
            drawLine(
                color = Color(0xFFFFF3B0),
                start = Offset(cx, cy - 15f * u),
                end = Offset(cx + 16f * u, cy - 6f * u),
                strokeWidth = 1.4f * u,
                cap = StrokeCap.Round
            )
        }

        SeasonalDesktopIconPreset.RAMADAN_KARIM -> {
            // هلال ذهبي ملكي وفانوس رمضان المضيء
            val crescent = Path().apply {
                moveTo(cx + 8f * u, cy - 26f * u)
                cubicTo(cx - 12f * u, cy - 26f * u, cx - 24f * u, cy - 10f * u, cx - 24f * u, cy + 4f * u)
                cubicTo(cx - 24f * u, cy + 20f * u, cx - 10f * u, cy + 30f * u, cx + 10f * u, cy + 28f * u)
                cubicTo(cx + 18f * u, cy + 27f * u, cx + 24f * u, cy + 23f * u, cx + 27f * u, cy + 18f * u)
                cubicTo(cx + 18f * u, cy + 21f * u, cx + 8f * u, cy + 18f * u, cx + 2f * u, cy + 11f * u)
                cubicTo(cx - 6f * u, cy + 2f * u, cx - 5f * u, cy - 12f * u, cx + 5f * u, cy - 20f * u)
                cubicTo(cx + 10f * u, cy - 24f * u, cx + 16f * u, cy - 25f * u, cx + 21f * u, cy - 24f * u)
                cubicTo(cx + 17f * u, cy - 25.5f * u, cx + 12f * u, cy - 26f * u, cx + 8f * u, cy - 26f * u)
                close()
            }
            drawPath(
                path = crescent,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFD97706)),
                    start = Offset(cx - 24f * u, cy - 26f * u),
                    end = Offset(cx + 26f * u, cy + 28f * u)
                )
            )
            // الفانوس الذهبي المعلق
            drawLine(
                color = Color(0xFFFDE68A),
                start = Offset(cx + 12f * u, cy - 30f * u),
                end = Offset(cx + 12f * u, cy - 14f * u),
                strokeWidth = 1.5f * u
            )
            val lanternRoof = Path().apply {
                moveTo(cx + 6f * u, cy - 14f * u)
                lineTo(cx + 18f * u, cy - 14f * u)
                lineTo(cx + 21f * u, cy - 8f * u)
                lineTo(cx + 3f * u, cy - 8f * u)
                close()
            }
            drawPath(path = lanternRoof, color = Color(0xFFD4AF37))
            val lanternBody = Path().apply {
                moveTo(cx + 5f * u, cy - 8f * u)
                lineTo(cx + 19f * u, cy - 8f * u)
                lineTo(cx + 17f * u, cy + 8f * u)
                lineTo(cx + 7f * u, cy + 8f * u)
                close()
            }
            drawPath(path = lanternBody, color = Color(0xFFFEF08A))
            val lanternBase = Path().apply {
                moveTo(cx + 6f * u, cy + 8f * u)
                lineTo(cx + 18f * u, cy + 8f * u)
                lineTo(cx + 15f * u, cy + 13f * u)
                lineTo(cx + 9f * u, cy + 13f * u)
                close()
            }
            drawPath(path = lanternBase, color = Color(0xFFB45309))
        }

        SeasonalDesktopIconPreset.NEW_YEAR_2027 -> {
            // تصميم احتفالي شتوي ملكي مع سنة 2027 ونجمة متوهجة
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45).toDouble())
                val len = if (i % 2 == 0) 24f * u else 16f * u
                drawLine(
                    color = Color(0xFFFDE047),
                    start = Offset(cx, cy - 10f * u),
                    end = Offset(
                        cx + (len * cos(angle)).toFloat(),
                        (cy - 10f * u) + (len * sin(angle)).toFloat()
                    ),
                    strokeWidth = 2.0f * u,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(
                color = Color(0xFFFFFBEB),
                radius = 6f * u,
                center = Offset(cx, cy - 10f * u)
            )
            // لوحة 2027 الذهبية
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFB45309), Color(0xFFFDE047), Color(0xFFB45309))
                ),
                topLeft = Offset(cx - 26f * u, cy + 6f * u),
                size = Size(52f * u, 18f * u),
                cornerRadius = CornerRadius(6f * u, 6f * u)
            )
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - 24f * u, cy + 8f * u),
                size = Size(48f * u, 14f * u),
                cornerRadius = CornerRadius(4f * u, 4f * u)
            )
            // رسم أرقام 2027 بخطوط هندسية حادة
            val digitsPath = Path().apply {
                // 2
                moveTo(cx - 19f * u, cy + 11.5f * u)
                lineTo(cx - 13f * u, cy + 11.5f * u)
                lineTo(cx - 13f * u, cy + 15f * u)
                lineTo(cx - 19f * u, cy + 15f * u)
                lineTo(cx - 19f * u, cy + 18.5f * u)
                lineTo(cx - 13f * u, cy + 18.5f * u)
                // 0
                moveTo(cx - 9f * u, cy + 11.5f * u)
                lineTo(cx - 3f * u, cy + 11.5f * u)
                lineTo(cx - 3f * u, cy + 18.5f * u)
                lineTo(cx - 9f * u, cy + 18.5f * u)
                close()
                // 2
                moveTo(cx + 2f * u, cy + 11.5f * u)
                lineTo(cx + 8f * u, cy + 11.5f * u)
                lineTo(cx + 8f * u, cy + 15f * u)
                lineTo(cx + 2f * u, cy + 15f * u)
                lineTo(cx + 2f * u, cy + 18.5f * u)
                lineTo(cx + 8f * u, cy + 18.5f * u)
                // 7
                moveTo(cx + 12f * u, cy + 11.5f * u)
                lineTo(cx + 19f * u, cy + 11.5f * u)
                lineTo(cx + 15f * u, cy + 18.5f * u)
            }
            drawPath(
                path = digitsPath,
                color = Color(0xFFFEF08A),
                style = Stroke(width = 1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        SeasonalDesktopIconPreset.EID_MUBARAK -> {
            // نجمة العيد الذهبية الثمانية والهلال الاحتفالي
            val starPoints = 12
            val starPath = Path()
            for (i in 0 until starPoints * 2) {
                val r = if (i % 2 == 0) 29f * u else 21f * u
                val a = Math.toRadians((i * 180.0 / starPoints) - 90.0)
                val px = cx + (r * cos(a)).toFloat()
                val py = cy + (r * sin(a)).toFloat()
                if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
            }
            starPath.close()
            drawPath(
                path = starPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFBEB), Color(0xFFFACC15), Color(0xFFB45309)),
                    start = Offset(cx - 28f * u, cy - 28f * u),
                    end = Offset(cx + 28f * u, cy + 28f * u)
                )
            )
            drawCircle(
                color = Color(0xFF064E3B),
                radius = 16.5f * u,
                center = Offset(cx, cy)
            )
            // هلال ونجمة العيد داخل الزخرفة
            drawCircle(
                color = Color(0xFFFEF08A),
                radius = 11f * u,
                center = Offset(cx - 1.5f * u, cy)
            )
            drawCircle(
                color = Color(0xFF064E3B),
                radius = 9f * u,
                center = Offset(cx + 3f * u, cy - 2f * u)
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = 2.8f * u,
                center = Offset(cx + 6f * u, cy - 2f * u)
            )
        }

        SeasonalDesktopIconPreset.MAWLID_NABAWI -> {
            // القبة الخضراء المنيرة والكتاب المفتوح للمولد النبوي الشريف
            val domePath = Path().apply {
                moveTo(cx, cy - 24f * u)
                cubicTo(cx - 15f * u, cy - 17f * u, cx - 21f * u, cy - 6f * u, cx - 21f * u, cy + 5f * u)
                lineTo(cx + 21f * u, cy + 5f * u)
                cubicTo(cx + 21f * u, cy - 6f * u, cx + 15f * u, cy - 17f * u, cx, cy - 24f * u)
                close()
            }
            drawPath(
                path = domePath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B)),
                    startY = cy - 24f * u,
                    endY = cy + 5f * u
                )
            )
            drawPath(
                path = domePath,
                color = Color(0xFFFDE047),
                style = Stroke(width = 1.8f * u)
            )
            // الهلال العلوي فوق القبة
            drawLine(
                color = Color(0xFFFEF08A),
                start = Offset(cx, cy - 31f * u),
                end = Offset(cx, cy - 24f * u),
                strokeWidth = 1.8f * u,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFFEF08A),
                radius = 3.2f * u,
                center = Offset(cx, cy - 31f * u),
                style = Stroke(width = 1.5f * u)
            )
            // قاعدة القبة الذهبية والكتاب المشرق
            drawRoundRect(
                color = Color(0xFFFDE047),
                topLeft = Offset(cx - 24f * u, cy + 5f * u),
                size = Size(48f * u, 4.5f * u),
                cornerRadius = CornerRadius(2f * u, 2f * u)
            )
            val openBook = Path().apply {
                moveTo(cx, cy + 13f * u)
                lineTo(cx - 18f * u, cy + 11f * u)
                lineTo(cx - 17f * u, cy + 22f * u)
                lineTo(cx, cy + 24f * u)
                lineTo(cx + 17f * u, cy + 22f * u)
                lineTo(cx + 18f * u, cy + 11f * u)
                close()
            }
            drawPath(path = openBook, color = Color(0xFFFFFBEB))
        }

        SeasonalDesktopIconPreset.IRAQ_NATIONAL_DAY -> {
            // الدرع الوطني الذهبي الموشح بالعلم العراقي
            val outerShield = Path().apply {
                moveTo(cx, cy - 31f * u)
                lineTo(cx + 26f * u, cy - 23f * u)
                lineTo(cx + 26f * u, cy - 1f * u)
                cubicTo(cx + 26f * u, cy + 15f * u, cx + 14.5f * u, cy + 25.5f * u, cx, cy + 31.5f * u)
                cubicTo(cx - 14.5f * u, cy + 25.5f * u, cx - 26f * u, cy + 15f * u, cx - 26f * u, cy - 1f * u)
                lineTo(cx - 26f * u, cy - 23f * u)
                close()
            }
            drawPath(
                path = outerShield,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF3B0), Color(0xFFD4AF37), Color(0xFF9A6F18)),
                    start = Offset(cx - 26f * u, cy - 31f * u),
                    end = Offset(cx + 26f * u, cy + 31.5f * u)
                )
            )
            // الشريط الأحمر العلوي
            val topRed = Path().apply {
                moveTo(cx, cy - 27f * u)
                lineTo(cx + 22f * u, cy - 20f * u)
                lineTo(cx + 22f * u, cy - 9f * u)
                lineTo(cx - 22f * u, cy - 9f * u)
                lineTo(cx - 22f * u, cy - 20f * u)
                close()
            }
            drawPath(path = topRed, color = Color(0xFFDC2626))
            // الشريط الأبيض الأوسط
            drawRect(
                color = Color.White,
                topLeft = Offset(cx - 22f * u, cy - 9f * u),
                size = Size(44f * u, 14f * u)
            )
            // التكبير الأخضر في المنتصف
            drawRoundRect(
                color = Color(0xFF047857),
                topLeft = Offset(cx - 13f * u, cy - 4f * u),
                size = Size(26f * u, 4f * u),
                cornerRadius = CornerRadius(2f * u, 2f * u)
            )
            // الشريط الأسود السفلي
            val bottomBlack = Path().apply {
                moveTo(cx - 22f * u, cy + 5f * u)
                lineTo(cx + 22f * u, cy + 5f * u)
                cubicTo(cx + 20f * u, cy + 15f * u, cx + 11.5f * u, cy + 22.5f * u, cx, cy + 27.5f * u)
                cubicTo(cx - 11.5f * u, cy + 22.5f * u, cx - 20f * u, cy + 15f * u, cx - 22f * u, cy + 5f * u)
                close()
            }
            drawPath(path = bottomBlack, color = Color(0xFF0F172A))
        }
    }
}
