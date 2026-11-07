package com.example.util

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Official 4-Language Support for the Iraqi Educational Platform (MADRASATI):
 * 1. Arabic (العربية) - RTL
 * 2. Kurdish (کوردی - Sorani) - RTL
 * 3. Turkmen (Türkmençe - Iraqi Turkmen Latin) - LTR
 * 4. English (English) - LTR
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val subtitleName: String,
    val shortCodeLabel: String,
    val badgeSymbol: String,
    val isRtl: Boolean
) {
    ARABIC("ar", "العربية", "اللغة العربية الرسمية", "عربي", "ع", true),
    KURDISH("ku", "کوردی", "زمانی کوردی فەرمی", "کوردی", "ک", true),
    TURKMEN("tr", "Türkmençe", "Resmi Türkmen Dili", "Türkmen", "TR", false),
    ENGLISH("en", "English", "Official English Language", "EN", "EN", false);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            val clean = code?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.code == clean } ?: ARABIC
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ARABIC }

data class QuadTranslation(
    val ar: String,
    val ku: String,
    val tr: String,
    val en: String
) {
    fun forLanguage(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> ar
        AppLanguage.KURDISH -> ku
        AppLanguage.TURKMEN -> tr
        AppLanguage.ENGLISH -> en
    }
}

/**
 * Direct helper for 4-language inline translation anywhere in Kotlin or Compose.
 */
fun tr(
    ar: String,
    ku: String,
    tr: String,
    en: String,
    lang: AppLanguage = AppLocalizationEngine.currentLanguage
): String = when (lang) {
    AppLanguage.ARABIC -> ar
    AppLanguage.KURDISH -> ku
    AppLanguage.TURKMEN -> tr
    AppLanguage.ENGLISH -> en
}

object AppLocalizationEngine {
    private const val PREFS_NAME = "madrasati_language_prefs"
    private const val KEY_LANGUAGE_CODE = "selected_language_code"

    var currentLanguageState by mutableStateOf(AppLanguage.ARABIC)
        private set

    val currentLanguage: AppLanguage
        get() = currentLanguageState

    var isLanguageModalOpen by mutableStateOf(false)

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedCode = prefs.getString(KEY_LANGUAGE_CODE, AppLanguage.ARABIC.code)
            currentLanguageState = AppLanguage.fromCode(savedCode)
        } catch (_: Exception) {
        }
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        currentLanguageState = language
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE_CODE, language.code).apply()
        } catch (_: Exception) {
        }
    }

    /**
     * Core Exact & Phrase Dictionary (Arabic -> QuadTranslation(ar, ku, tr, en)).
     * Covers every navigation section, role, grade, section, subject, exam period,
     * smart gate, holiday lock, principal approval, attendance status, report card, and PDF string.
     */
    private val rawTranslations: List<QuadTranslation> = listOf(
        // =====================================================================
        // 1. PLATFORM IDENTITY, LOGIN, ONBOARDING & ROLES
        // =====================================================================
        QuadTranslation("مدرستي", "قوتابخانەکەم", "Mektebim", "MADRASATI"),
        QuadTranslation("منصة مدرستي", "پلاتفۆرمی قوتابخانەکەم", "Mektebim Platformu", "Madrasati Platform"),
        QuadTranslation("منصة مدرستي الذكية", "پلاتفۆرمی زیرەکی قوتابخانەکەم", "Akıllı Mektebim Platformu", "Madrasati Smart Platform"),
        QuadTranslation("منصة مدرستي النموذجية", "پلاتفۆرمی نموونەیی قوتابخانەکەم", "Örnek Mektebim Platformu", "Madrasati Model Platform"),
        QuadTranslation("مدرستي الذكية النموذجية", "قوتابخانە زیرەکە نموونەییەکەم", "Akıllı Örnek Mektebim", "Madrasati Smart Model School"),
        QuadTranslation("مدرستي النموذجية", "قوتابخانەی نموونەیی", "Örnek Mektebim", "Madrasati Model School"),
        QuadTranslation("جمهورية العراق • وزارة التربية", "کۆماری عێراق • وەزارەتی پەروەردە", "Irak Cumhuriyeti • Eğitim Bakanlığı", "Republic of Iraq • Ministry of Education"),
        QuadTranslation("وزارة التربية", "وەزارەتی پەروەردە", "Eğitim Bakanlığı", "Ministry of Education"),
        QuadTranslation("وزارة التربية العراقية", "وەزارەتی پەروەردەی عێراق", "Irak Eğitim Bakanlığı", "Iraqi Ministry of Education"),
        QuadTranslation("تسجيل الدخول", "چوونەژوورەوە", "Giriş Yap", "Sign In"),
        QuadTranslation("دخول للمنصة", "چوونەناو پلاتفۆرم", "Platforma Giriş", "Enter Platform"),
        QuadTranslation("تسجيل الخروج", "چوونەدەرەوە", "Çıkış Yap", "Sign Out"),
        QuadTranslation("خروج", "دەرچوون", "Çıkış", "Exit"),
        QuadTranslation("اسم المستخدم", "ناوی بەکارهێنەر", "Kullanıcı Adı", "Username"),
        QuadTranslation("اسم المستخدم أو الكود", "ناوی بەکارهێنەر یان کۆد", "Kullanıcı Adı veya Kod", "Username or Code"),
        QuadTranslation("كلمة المرور", "وشەی تێپەڕ", "Şifre", "Password"),
        QuadTranslation("كود المدرسة", "کۆدی قوتابخانە", "Okul Kodu", "School Code"),
        QuadTranslation("رمز التفعيل", "کۆدی چالاککردن", "Aktivasyon Kodu", "Activation Code"),
        QuadTranslation("تفعيل مدرسة جديدة", "چالاککردنی قوتابخانەی نوێ", "Yeni Okul Etkinleştir", "Activate New School"),
        QuadTranslation("الدخول ببصمة الإصبع", "چوونەژوورەوە بە پەنجەمۆر", "Parmak İzi ile Giriş", "Biometric Sign In"),
        QuadTranslation("تذكر بيانات الدخول", "بیرهێنانەوەی زانیارییەکانی چوونەژوورەوە", "Giriş Bilgilerini Hatırla", "Remember Credentials"),
        QuadTranslation("المدير", "بەڕێوەبەر", "Müdir", "Principal"),
        QuadTranslation("مدير المدرسة", "بەڕێوەبەری قوتابخانە", "Okul Müdiri", "School Principal"),
        QuadTranslation("إدارة المدرسة", "کارگێڕی قوتابخانە", "Okul Yönetimi", "School Administration"),
        QuadTranslation("كادر تدريسي", "ستافی وانەبێژی", "Öğretim Kadrosu", "Teaching Staff"),
        QuadTranslation("المعلم", "مامۆستا", "Mugallym", "Teacher"),
        QuadTranslation("المدرس", "مامۆستا", "Mugallym", "Teacher"),
        QuadTranslation("المعلمين", "مامۆستایان", "Mugallymlar", "Teachers"),
        QuadTranslation("المدرسين", "مامۆستایان", "Mugallymlar", "Teachers"),
        QuadTranslation("معلم", "مامۆستا", "Mugallym", "Teacher"),
        QuadTranslation("مدرس", "مامۆستا", "Mugallym", "Teacher"),
        QuadTranslation("طالب", "قوتابی", "Okuwçy", "Student"),
        QuadTranslation("الطالب", "قوتابی", "Okuwçy", "Student"),
        QuadTranslation("الطلبة", "قوتابییان", "Okuwçylar", "Students"),
        QuadTranslation("الطلاب", "قوتابییان", "Okuwçylar", "Students"),
        QuadTranslation("ولي أمر", "بەخێوکەر", "Weli", "Parent"),
        QuadTranslation("ولي الأمر", "بەخێوکەر", "Weli", "Parent"),
        QuadTranslation("أولياء الأمور", "بەخێوکەران", "Weliler", "Parents"),
        QuadTranslation("اللغة", "زمان", "Dil", "Language"),
        QuadTranslation("اختيار لغة المنصة", "هەڵبژاردنی زمانی پلاتفۆرم", "Platform Dilini Seçin", "Select Platform Language"),
        QuadTranslation("تغيير اللغة الفوري", "گۆڕینی دەستبەجێی زمان", "Anında Dil Değiştirme", "Instant Language Switch"),

        // =====================================================================
        // 2. ALL 15 NAVIGATION SECTIONS & TABS
        // =====================================================================
        QuadTranslation("الرئيسية", "سەرەکی", "Baş Sahypa", "Home"),
        QuadTranslation("موجز الدوام اليومي", "پوختەی دەوامی ڕۆژانە", "Günlük Devam Özeti", "Daily Operations Radar"),
        QuadTranslation("الحضور والغياب", "ئامادەبوون و نەهاتن", "Gatnaşyk we Gelmezlik", "Attendance & Absence"),
        QuadTranslation("البوابة الذكية والباركود", "دەروازەی زیرەک و بارکۆد", "Akıllı Kapı ve Barkod", "Smart Gate & Barcode"),
        QuadTranslation("الجدول والواجبات", "خشتە و ئەرکەکان", "Tertip we Öý Işleri", "Schedule & Homework"),
        QuadTranslation("المناهج الدراسية", "پڕۆگرامەکانی خوێندن", "Okul Müfredatı", "Curriculum Books"),
        QuadTranslation("الامتحانات والدرجات", "تاقیکردنەوە و نمرەکان", "Synaglar we Bahalar", "Exams & Grades"),
        QuadTranslation("السجل الأكاديمي والإحصائي الشامل", "تۆماری ئەکادیمی و ئاماری گشتگیر", "Kapsamlı Akademik ve İstatistik Kaydı", "Comprehensive Academic Archive"),
        QuadTranslation("السجل الأكاديمي الشامل", "تۆماری ئەکادیمی گشتگیر", "Kapsamlı Akademik Kayıt", "Academic Archive"),
        QuadTranslation("إصدار الكارتات والنتائج", "دەرکردنی کارتی نمرە و ئەنجامەکان", "Karne ve Sonuç Belgesi", "Report Cards & Results"),
        QuadTranslation("لوحة الشرف", "تەختەی شەرەف", "Onur Tablosu", "Honor Roll"),
        QuadTranslation("تقييم الكادر التدريسي", "هەڵسەنگاندنی مامۆستایان", "Öğretmen Değerlendirmesi", "Teacher Evaluation"),
        QuadTranslation("تقييم الكادر", "هەڵسەنگاندنی ستاف", "Kadro Değerlendirmesi", "Staff Evaluation"),
        QuadTranslation("نشاطات الطلبة", "چالاکییەکانی قوتابییان", "Öğrenci Etkinlikleri", "Student Activities"),
        QuadTranslation("مجتمع ونشاطات الطلبة", "کۆمەڵگە و چالاکی قوتابییان", "Öğrenci Topluluğu ve Etkinlikleri", "Student Community & Activities"),
        QuadTranslation("الأقساط الدراسية", "کرێی خوێندن", "Okul Taksitleri", "Tuition & Fees"),
        QuadTranslation("الحانوت والمحفظة", "فرۆشگا و جزدان", "Kantin ve Cüzdan", "Canteen & Wallet"),
        QuadTranslation("الحانوت والمحفظة الذكية", "فرۆشگا و جزدانی زیرەک", "Akıllı Kantin ve Cüzdan", "Smart Canteen & Wallet"),
        QuadTranslation("البلاغات والتواصل", "ڕاپۆرت و پەیوەندی", "Bildirimler ve İletişim", "Reports & Messaging"),
        QuadTranslation("الفعاليات والأخبار", "بۆنە و هەواڵەکان", "Etkinlikler ve Haberler", "Events & News"),
        QuadTranslation("إدارة الحسابات", "بەڕێوەبردنی هەژمارەکان", "Hesap Yönetimi", "Manage Accounts"),
        QuadTranslation("الإعدادات", "ڕێکخستنەکان", "Ayarlar", "Settings"),
        QuadTranslation("الإعدادات والعطل", "ڕێکخستنەکان و پشووەکان", "Ayarlar ve Tatiller", "Settings & Holidays"),

        // =====================================================================
        // 3. SCHOOL STAGES, TYPES, CLASSES (1..12) & SECTIONS
        // =====================================================================
        QuadTranslation("مدرسة حكومية", "قوتابخانەی حکومی", "Devlet Okulu", "Public School"),
        QuadTranslation("مدرسة أهلية", "قوتابخانەی ناحکومی (ئەهلی)", "Özel Okul", "Private School"),
        QuadTranslation("مدرسة ابتدائية", "قوتابخانەی سەرەتایی", "İlkokul", "Primary School"),
        QuadTranslation("مدرسة متوسطة", "قوتابخانەی ناوەندی", "Ortaokul", "Middle School"),
        QuadTranslation("مدرسة إعدادية", "قوتابخانەی ئامادەیی", "Lise (Hazırlık)", "Preparatory School"),
        QuadTranslation("ثانوية", "دواناوەندی", "Ortaöğretim / Lise", "Secondary School"),
        QuadTranslation("المرحلة الابتدائية", "قۆناغی سەرەتایی", "İlkokul Kademesi", "Primary Stage"),
        QuadTranslation("المرحلة المتوسطة", "قۆناغی ناوەندی", "Ortaokul Kademesi", "Middle Stage"),
        QuadTranslation("المرحلة الإعدادية", "قۆناغی ئامادەیی", "Lise Kademesi", "Preparatory Stage"),
        QuadTranslation("مدرسة بنين", "قوتابخانەی کوڕان", "Erkek Okulu", "Boys School"),
        QuadTranslation("مدرسة بنات", "قوتابخانەی کچان", "Kız Okulu", "Girls School"),
        QuadTranslation("مدرسة مختلطة", "قوتابخانەی تێکەڵاو", "Karma Okul", "Co-ed School"),

        // Official Iraqi Grades
        QuadTranslation("الأول الابتدائي", "یەکەمی سەرەتایی", "1. Sınıf (İlkokul)", "Grade 1 Primary"),
        QuadTranslation("الثاني الابتدائي", "دووەمی سەرەتایی", "2. Sınıf (İlkokul)", "Grade 2 Primary"),
        QuadTranslation("الثالث الابتدائي", "سێیەمی سەرەتایی", "3. Sınıf (İlkokul)", "Grade 3 Primary"),
        QuadTranslation("الرابع الابتدائي", "چوارەمی سەرەتایی", "4. Sınıf (İlkokul)", "Grade 4 Primary"),
        QuadTranslation("الخامس الابتدائي", "پێنجەمی سەرەتایی", "5. Sınıf (İlkokul)", "Grade 5 Primary"),
        QuadTranslation("السادس الابتدائي", "شەشەمی سەرەتایی", "6. Sınıf (İlkokul)", "Grade 6 Primary"),
        QuadTranslation("الأول المتوسط", "یەکەمی ناوەندی", "1. Ortaokul (7. Sınıf)", "Grade 7 (1st Middle)"),
        QuadTranslation("الثاني المتوسط", "دووەمی ناوەندی", "2. Ortaokul (8. Sınıf)", "Grade 8 (2nd Middle)"),
        QuadTranslation("الثالث المتوسط", "سێیەمی ناوەندی", "3. Ortaokul (9. Sınıf)", "Grade 9 (3rd Middle)"),
        QuadTranslation("الرابع الإعدادي", "چوارەمی ئامادەیی", "4. Lise (10. Sınıf)", "Grade 10 Preparatory"),
        QuadTranslation("الرابع العلمي", "چوارەمی زانستی", "4. Fen (10. Sınıf)", "Grade 10 Scientific"),
        QuadTranslation("الرابع الأدبي", "چوارەمی وێژەیی", "4. Edebiyat (10. Sınıf)", "Grade 10 Literary"),
        QuadTranslation("الخامس الإعدادي", "پێنجەمی ئامادەیی", "5. Lise (11. Sınıf)", "Grade 11 Preparatory"),
        QuadTranslation("الخامس العلمي", "پێنجەمی زانستی", "5. Fen (11. Sınıf)", "Grade 11 Scientific"),
        QuadTranslation("الخامس الأدبي", "پێنجەمی وێژەیی", "5. Edebiyat (11. Sınıf)", "Grade 11 Literary"),
        QuadTranslation("السادس الإعدادي", "شەشەمی ئامادەیی", "6. Lise (12. Sınıf)", "Grade 12 Preparatory"),
        QuadTranslation("السادس العلمي", "شەشەمی زانستی", "6. Fen (12. Sınıf)", "Grade 12 Scientific"),
        QuadTranslation("السادس الأدبي", "شەشەمی وێژەیی", "6. Edebiyat (12. Sınıf)", "Grade 12 Literary"),

        // Sections
        QuadTranslation("الصف", "پۆل", "Sınıf", "Grade"),
        QuadTranslation("الشعبة", "لق", "Şube", "Section"),
        QuadTranslation("شعبة", "لقی", "Şube", "Section"),
        QuadTranslation("الصف والشعبة", "پۆل و لق", "Sınıf ve Şube", "Grade & Section"),
        QuadTranslation("الصف والشعبة الدراسية:", "پۆل و لقی خوێندن:", "Sınıf ve Şube:", "Grade & Section:"),
        QuadTranslation("جميع الصفوف", "هەموو پۆلەکان", "Tüm Sınıflar", "All Grades"),
        QuadTranslation("جميع الشعب", "هەموو لقەکان", "Tüm Şubeler", "All Sections"),
        QuadTranslation("جميع الصفوف المسندة", "هەموو پۆلە سپێردراوەکان", "Atanan Tüm Sınıflar", "All Assigned Grades"),
        QuadTranslation("الكل", "هەموو", "Tümü", "All"),

        // =====================================================================
        // 4. CURRICULUM SUBJECTS & READER CONTROLS
        // =====================================================================
        QuadTranslation("التربية الإسلامية", "پەروەردەی ئیسلامی", "İslam Eğitimi", "Islamic Education"),
        QuadTranslation("القرآن الكريم والتربية الإسلامية", "قورئانی پیرۆز و پەروەردەی ئیسلامی", "Kur'an-ı Kerim ve İslam Eğitimi", "Holy Quran & Islamic Education"),
        QuadTranslation("اللغة العربية", "زمانی عەرەبی", "Arap Dili", "Arabic Language"),
        QuadTranslation("قراءتي", "خوێندنەوەکەم (قراءتي)", "Okumam (Kıraati)", "My Reading (Arabic)"),
        QuadTranslation("القراءة العربية", "خوێندنەوەی عەرەبی", "Arapça Okuma", "Arabic Reading"),
        QuadTranslation("قواعد اللغة العربية", "ڕێزمانی زمانی عەرەبی", "Arapça Dilbilgisi", "Arabic Grammar"),
        QuadTranslation("اللغة الإنكليزية", "زمانی ئینگلیزی", "İngiliz Dili", "English Language"),
        QuadTranslation("الرياضيات", "بیرکاری", "Matematik", "Mathematics"),
        QuadTranslation("العلوم", "زانستەکان", "Fen Bilimleri", "Sciences"),
        QuadTranslation("الاجتماعيات", "کۆمەڵایەتی", "Sosyal Bilgiler", "Social Studies"),
        QuadTranslation("التربية الفنية والرياضية", "پەروەردەی هونەری و وەرزشی", "Sanat ve Beden Eğitimi", "Art & Physical Education"),
        QuadTranslation("التربية الأخلاقية", "پەروەردەی ڕەوشتی", "Ahlak Eğitimi", "Moral Education"),
        QuadTranslation("الفيزياء", "فیزیا", "Fizik", "Physics"),
        QuadTranslation("الكيمياء", "کیمیا", "Kimya", "Chemistry"),
        QuadTranslation("الأحياء", "زیندەزانی", "Biyoloji", "Biology"),
        QuadTranslation("الحاسوب", "کۆمپیوتەر", "Bilgisayar", "Computer Science"),
        QuadTranslation("اللغة الكردية", "زمانی کوردی", "Kürt Dili", "Kurdish Language"),
        QuadTranslation("اللغة الفرنسية", "زمانی فەڕەنسی", "Fransız Dili", "French Language"),
        QuadTranslation("التاريخ", "مێژوو", "Tarih", "History"),
        QuadTranslation("الجغرافية", "جوگرافیا", "Coğrafya", "Geography"),
        QuadTranslation("الاقتصاد", "ئابووری", "Ekonomi", "Economics"),
        QuadTranslation("علم الاجتماع", "کۆمەڵناسی", "Sosyoloji", "Sociology"),
        QuadTranslation("الفلسفة وعلم النفس", "فەلسەفە و دەروونناسی", "Felsefe ve Psikoloji", "Philosophy & Psychology"),
        QuadTranslation("جرائم حزب البعث", "تاوانەکانی حزبی بەعس", "Baas Partisi Suçları", "Baath Regime Crimes"),
        QuadTranslation("فتح الكتاب", "کردنەوەی کتێب", "Kitabı Aç", "Open Book"),
        QuadTranslation("قراءة الكتاب", "خوێندنەوەی کتێب", "Kitabı Oku", "Read Book"),
        QuadTranslation("تحميل الكتاب", "داگرتنی کتێب", "Kitabı İndir", "Download Book"),
        QuadTranslation("تم التحميل مسبقاً (فتح فوري)", "پێشتر داگیراوە (کردنەوەی دەستبەجێ)", "Önceden İndirildi (Anında Aç)", "Downloaded (Instant Open)"),
        QuadTranslation("محفوظ أوفلاين", "پاشەکەوتکراوی ئۆفلاین", "Çevrimdışı Kayıtlı", "Saved Offline"),
        QuadTranslation("حذف النسخة المحفوظة", "سڕینەوەی وەشانی پاشەکەوتکراو", "Kayıtlı Kopyayı Sil", "Delete Cached Copy"),
        QuadTranslation("إعادة المحاولة", "هەوڵبدەرەوە", "Yeniden Dene", "Retry"),
        QuadTranslation("رجوع للمناهج", "گەڕانەوە بۆ پڕۆگرامەکان", "Müfredata Dön", "Back to Curriculum"),
        QuadTranslation("السابق", "پێشوو", "Önceki", "Previous"),
        QuadTranslation("التالي", "دواتر", "Sonraki", "Next"),
        QuadTranslation("تكبير", "گەورەکردن", "Büyüt", "Zoom In"),
        QuadTranslation("تصغير", "بچووککردنەوە", "Küçült", "Zoom Out"),
        QuadTranslation("جاري تهيئة الكتاب من خادم المناهج...", "ئامادەکردنی کتێب لە ڕاژەی پڕۆگرامەکان...", "Kitap müfredat sunucusundan hazırlanıyor...", "Preparing book from curriculum server..."),

        // =====================================================================
        // 5. ATTENDANCE, SMART GATE, HOLIDAY LOCKS & ENROLLMENT STATUSES
        // =====================================================================
        QuadTranslation("حاضر", "ئامادەبوو", "Geldi (Hazır)", "Present"),
        QuadTranslation("غائب", "نەهاتوو", "Gelmedi (Yok)", "Absent"),
        QuadTranslation("متأخر", "دواکەوتوو", "Geç Kaldı", "Late"),
        QuadTranslation("مجاز", "مۆڵەتپێدراو", "İzinli", "Excused"),
        QuadTranslation("عطلة رسمية", "پشووی فەرمی", "Resmi Tatil", "Official Holiday"),
        QuadTranslation("بانتظار الرصد", "لە چاوەڕوانی تۆمارکردن", "Kayıt Bekliyor", "Pending Record"),
        QuadTranslation("قيد الرصد", "لەژێر تۆمارکردن", "Kaydediliyor", "Pending Entry"),
        QuadTranslation("مستمر بالدوام", "بەردەوام لە دەوام", "Devam Ediyor", "Active Enrollment"),
        QuadTranslation("مستمر", "بەردەوام", "Devam Ediyor", "Ongoing"),
        QuadTranslation("فصل مؤقت", "دەرکردنی کاتی", "Geçici Uzaklaştırma", "Temporary Suspension"),
        QuadTranslation("فصل دائمي", "دەرکردنی هەمیشەیی", "Kalıcı İhraç", "Expelled"),
        QuadTranslation("ترقين قيد", "سڕینەوەی تۆمار (تەرقین قەید)", "Kayıt Silme", "Deregistered"),
        QuadTranslation("منقول إلى مدرسة أخرى", "گواستراوە بۆ قوتابخانەیەکی تر", "Başka Okula Nakil", "Transferred"),
        QuadTranslation("تسجيل دخول البوابة (حضور)", "تۆمارکردنی هاتنەژوورەوەی دەروازە (ئامادەبوون)", "Kapı Giriş Kaydı (Devam)", "Gate Check-In (Arrival)"),
        QuadTranslation("تسجيل دخول (حضور)", "تۆماری هاتنەژوورەوە", "Giriş Kaydı", "Check-In"),
        QuadTranslation("تسجيل مغادرة البوابة (انصراف)", "تۆمارکردنی دەرچوون لە دەروازە (ڕۆیشتن)", "Kapı Çıkış Kaydı (Ayrılış)", "Gate Check-Out (Departure)"),
        QuadTranslation("تسجيل مغادرة (انصراف)", "تۆماری دەرچوون", "Çıkış Kaydı", "Check-Out"),
        QuadTranslation("داخل المدرسة", "لەناو قوتابخانە", "Okul İçinde", "Inside School"),
        QuadTranslation("غادر المدرسة", "قوتابخانەی بەجێهێشت", "Okuldan Ayrıldı", "Departed School"),
        QuadTranslation("البوابة الذكية مقفلة اليوم بسبب عطلة رسمية", "دەروازەی زیرەک ئەمڕۆ داخراوە بەهۆی پشووی فەرمی", "Akıllı Kapı bugün resmi tatil nedeniyle kapalıdır", "Smart Gate is locked today due to an official holiday"),
        QuadTranslation("رصد الحضور مقفل اليوم بسبب عطلة رسمية", "تۆماری ئامادەبوون ئەمڕۆ داخراوە بەهۆی پشووی فەرمی", "Yoklama kaydı bugün resmi tatil nedeniyle kapalıdır", "Attendance recording is locked today due to an official holiday"),

        // =====================================================================
        // 6. EXAM PERIODS, PRINCIPAL GRADE APPROVAL & REPORT CARDS
        // =====================================================================
        QuadTranslation("التقييم اليومي", "هەڵسەنگاندنی ڕۆژانە", "Günlük Değerlendirme", "Daily Assessment"),
        QuadTranslation("اليومي", "ڕۆژانە", "Günlük", "Daily"),
        QuadTranslation("الشهر الأول", "مانگی یەکەم", "1. Ay Sınavı", "First Month"),
        QuadTranslation("شهر 1", "مانگی ١", "1. Ay", "Month 1"),
        QuadTranslation("الشهر الثاني", "مانگی دووەم", "2. Ay Sınavı", "Second Month"),
        QuadTranslation("شهر 2", "مانگی ٢", "2. Ay", "Month 2"),
        QuadTranslation("نصف السنة", "نیوەی ساڵ", "Yarıyıl (Ara Sınav)", "Mid-Year Exam"),
        QuadTranslation("الشهر الثالث", "مانگی سێیەم", "3. Ay Sınavı", "Third Month"),
        QuadTranslation("شهر 3", "مانگی ٣", "3. Ay", "Month 3"),
        QuadTranslation("الشهر الرابع", "مانگی چوارەم", "4. Ay Sınavı", "Fourth Month"),
        QuadTranslation("شهر 4", "مانگی ٤", "4. Ay", "Month 4"),
        QuadTranslation("السعي السنوي", "کۆششی ساڵانە (سەعی)", "Yıllık Çaba Puanı", "Annual Effort"),
        QuadTranslation("السعي", "کۆشش", "Yıllık Çaba", "Effort"),
        QuadTranslation("الامتحان النهائي", "تاقیکردنەوەی کۆتایی", "Final Sınavı", "Final Exam"),
        QuadTranslation("النهائي", "کۆتایی", "Final", "Final"),
        QuadTranslation("المعدل النهائي", "تێکڕای کۆتایی", "Genel Ortalama", "Final Average"),
        QuadTranslation("المعدل", "تێکڕا", "Ortalama", "Average"),
        QuadTranslation("النتيجة", "ئەنجام", "Sonuç", "Result"),
        QuadTranslation("النتيجة النهائية", "ئەنجامی کۆتایی", "Nihai Sonuç", "Final Result"),
        QuadTranslation("التقدير", "پلەبەندی", "Derece", "Rating"),
        QuadTranslation("ممتاز", "نایاب", "Pekiyi", "Excellent"),
        QuadTranslation("جيد جداً", "زۆر باش", "İyi", "Very Good"),
        QuadTranslation("جيد", "باش", "Orta-İyi", "Good"),
        QuadTranslation("متوسط", "مامناوەند", "Orta", "Average"),
        QuadTranslation("مقبول", "پەسەند", "Geçer", "Pass"),
        QuadTranslation("ناجح", "سەرکەوتوو", "Başarılı (Geçti)", "Passed"),
        QuadTranslation("ناجح ومتفوق", "سەرکەوتوو و لێهاتوو", "Başarılı ve Üstün", "Passed with Distinction"),
        QuadTranslation("مكمل", "ئیكمال (تەواوکەر)", "Bütünleme", "Supplementary"),
        QuadTranslation("راسب", "کەوتوو", "Başarısız (Kaldı)", "Failed"),
        QuadTranslation("راسب / يحتاج دعم", "کەوتوو / پێویستی بە پاڵپشتییە", "Kaldı / Destek Gerekli", "Failed / Needs Support"),
        QuadTranslation("مصادقة المدير الذكية على الدرجات", "پەسەندکردنی زیرەکی نمرەکان لەلایەن بەڕێوەبەرەوە", "Müdürün Akıllı Not Onayı", "Smart Principal Grade Approval"),
        QuadTranslation("مصادقة المدير", "پەسەندکردنی بەڕێوەبەر", "Müdir Onayı", "Principal Approval"),
        QuadTranslation("مصادق ومعتمد", "پەسەندکراو و بڕواپێکراو", "Onaylandı veResmileşti", "Approved & Certified"),
        QuadTranslation("بانتظار مصادقة المدير", "لە چاوەڕوانی پەسەندکردنی بەڕێوەبەر", "Müdir Onayı Bekleniyor", "Awaiting Principal Approval"),
        QuadTranslation("تجاوز النقص والمصادقة استثنائياً", "تێپەڕاندنی کەموکوڕی و پەسەندکردنی نائاسایی", "Eksikleri Geç ve İstisnai Onayla", "Override Missing & Approve"),
        QuadTranslation("إصدار كارت الدرجات", "دەرکردنی کارتی نمرەکان", "Not Karnesi Oluştur", "Issue Report Card"),
        QuadTranslation("تصدير PDF", "هەناردەکردنی PDF", "PDF İndir / Yazdır", "Export PDF"),
        QuadTranslation("حفظ كصورة عالية الدقة", "پاشەکەوتکردن وەک وێنەی کوالێتی بەرز", "Yüksek Çözünürlüklü Resim Kaydet", "Save High-Res PNG"),

        // =====================================================================
        // 7. COMMON BUTTONS, ACTIONS, DAYS OF WEEK & GENERAL UI
        // =====================================================================
        QuadTranslation("حفظ", "پاشەکەوتکردن", "Kaydet", "Save"),
        QuadTranslation("جاري الحفظ...", "لە پاشەکەوتکردندایە...", "Kaydediliyor...", "Saving..."),
        QuadTranslation("إلغاء", "پاشگەزبوونەوە", "İptal", "Cancel"),
        QuadTranslation("إغلاق", "داخستن", "Kapat", "Close"),
        QuadTranslation("تأكيد", "پشتڕاستکردنەوە", "Onayla", "Confirm"),
        QuadTranslation("تعديل", "دەستکاری", "Düzenle", "Edit"),
        QuadTranslation("حذف", "سڕینەوە", "Sil", "Delete"),
        QuadTranslation("إضافة", "زیادکردن", "Ekle", "Add"),
        QuadTranslation("بحث", "گەڕان", "Ara", "Search"),
        QuadTranslation("تحديث", "نوێکردنەوە", "Yenile", "Refresh"),
        QuadTranslation("مشاركة", "هاوبەشکردن", "Paylaş", "Share"),
        QuadTranslation("طباعة", "چاپکردن", "Yazdır", "Print"),
        QuadTranslation("معاينة الصورة", "پێشبینینی وێنە", "Resim Önizleme", "Image Preview"),
        QuadTranslation("الأحد", "یەکشەممە", "Pazar", "Sunday"),
        QuadTranslation("الاثنين", "دووشەممە", "Pazartesi", "Monday"),
        QuadTranslation("الإثنين", "دووشەممە", "Pazartesi", "Monday"),
        QuadTranslation("الثلاثاء", "سێشەممە", "Salı", "Tuesday"),
        QuadTranslation("الأربعاء", "چوارشەممە", "Çarşamba", "Wednesday"),
        QuadTranslation("الخميس", "پێنجشەممە", "Perşembe", "Thursday"),
        QuadTranslation("الجمعة", "هەینی", "Cuma", "Friday"),
        QuadTranslation("السبت", "شەممە", "Cumartesi", "Saturday"),

        // =====================================================================
        // 8. PDF REPORTS, ID CARDS, ARCHIVE, TUITION & CANTEEN WALLET
        // =====================================================================
        QuadTranslation("بطاقة الهوية المدرسية الرسمية", "کارتی ناسنامەی فەرمی قوتابخانە", "Resmi Okul Kimlik Kartı", "Official School ID Card"),
        QuadTranslation("بطاقة محفظة الحانوت الذكية", "کارتی جزدانی زیرەکی فرۆشگا", "Akıllı Kantin Cüzdan Kartı", "Smart Canteen Wallet Card"),
        QuadTranslation("بطاقة محفظة الحانوت", "کارتی جزدانی فرۆشگا", "Kantin Cüzdan Kartı", "Canteen Wallet Pass"),
        QuadTranslation("هوية الحضور الأكاديمية", "ناسنامەی ئامادەبوونی ئەکادیمی", "Akademik Devam Kimliği", "Academic Attendance ID"),
        QuadTranslation("اسم الطالب الكامل:", "ناوی تەواوی قوتابی:", "Öğrencinin Tam Adı:", "Student Full Name:"),
        QuadTranslation("اسم الطالب الكامل", "ناوی تەواوی قوتابی", "Öğrencinin Tam Adı", "Student Full Name"),
        QuadTranslation("اسم الطالب", "ناوی قوتابی", "Öğrenci Adı", "Student Name"),
        QuadTranslation("اسم ولي الأمر", "ناوی بەخێوکەر", "Veli Adı", "Parent Name"),
        QuadTranslation("كود الطالب الموحد / هاتف ولي الأمر:", "کۆدی قوتابی / مۆبایلی بەخێوکەر:", "Öğrenci Kodu / Veli Telefonu:", "Student Code / Parent Phone:"),
        QuadTranslation("امسح رمز QR لتسجيل الحضور الفوري", "کۆدی QR بپشکنە بۆ تۆمارکردنی دەستبەجێی ئامادەبوون", "Anında yoklama için QR kodunu tarayın", "Scan QR Code for Instant Attendance"),
        QuadTranslation("باركود محفظة الحانوت المدرسي", "بارکۆدی جزدانی فرۆشگای قوتابخانە", "Okul Kantin Cüzdan Barkodu", "School Canteen Wallet Barcode"),
        QuadTranslation("صالح للدفع الإلكتروني في الحانوت", "گونجاوە بۆ پارەدانی ئەلیکترۆنی لە فرۆشگا", "Kantinde elektronik ödeme için geçerlidir", "Valid for Digital Canteen Payment"),
        QuadTranslation("بصمة حضور ذكية معتمدة للمدرسة", "مۆری ئامادەبوونی زیرەکی باوەڕپێکراوی قوتابخانە", "Okul onaylı akıllı devam imzası", "Certified Smart School Attendance Pass"),
        QuadTranslation("شحن رصيد", "بارگاویکردنەوەی باڵانس", "Bakiye Yükleme", "Balance Top-Up"),
        QuadTranslation("شراء من الحانوت", "کڕین لە فرۆشگا", "Kantinden Satın Alma", "Canteen Purchase"),
        QuadTranslation("محاولة شراء مرفوضة", "هەوڵی کڕینی ڕەتکراوە", "Reddedilen Satın Alma", "Declined Purchase"),
        QuadTranslation("توقيع المرشد / المعاون الإداري", "واژۆی ڕێنمایکار / یاریدەدەری کارگێڕی", "Rehber / Müdür Yardımcısı İmzası", "Counselor / Deputy Signature"),
        QuadTranslation("الختم الرسمي للمدرسة", "مۆری فەرمی قوتابخانە", "Okul Resmi Mührü", "Official School Stamp"),
        QuadTranslation("معتمد إلكترونياً", "پەسەندکراوی ئەلیکترۆنی", "Elektronik Onaylı", "Electronically Certified"),
        QuadTranslation("التوقيع الرسمي: مصادق ومعتمد", "واژۆی فەرمی: پەسەندکراو و بڕواپێکراو", "Resmi İmza: Onaylandı", "Official Signature: Approved"),
        QuadTranslation("شعار", "لۆگۆی", "Okul", "School"),
        QuadTranslation("المدرسة", "قوتابخانە", "Logosu", "Emblem"),
        QuadTranslation("كشف الحضور والغياب الشهري الرسمي", "لیستی فەرمی ئامادەبوون و نەهاتنی مانگانە", "Resmi Aylık Devam ve Devamsızlık Cetveli", "Official Monthly Attendance Report"),
        QuadTranslation("كشف الأقساط والدفعات المالية الرسمي", "لیستی فەرمی کرێی خوێندن و پارەدانەکان", "Resmi Taksit ve Mali Ödeme Cetveli", "Official Tuition & Payments Statement"),
        QuadTranslation("وصل قبض قسط دراسي رقمي رسمي", "پسوولەی فەرمی دیجیتاڵی وەرگرتنی کرێی خوێندن", "Resmi Dijital Okul Taksit Makbuzu", "Official Digital Tuition Receipt"),
        QuadTranslation("السجل الشامل للطالب (الأرشيف الشهري والسنوي)", "تۆماری گشتگیری قوتابی (ئەرشیفی مانگانە و ساڵانە)", "Kapsamlı Öğrenci Kaydı (Aylık ve Yıllık Arşiv)", "Comprehensive Student Archive"),
        QuadTranslation("تقرير الترحيل السنوي ونتائج نهاية العام الدراسي", "ڕاپۆرتی پەڕینەوەی ساڵانە و ئەنجامەکانی کۆتایی ساڵ", "Yıllık Sınıf Geçme ve Yıl Sonu Sonuç Raporu", "Annual Promotion & End-of-Year Report"),
        QuadTranslation("السجل الإحصائي العام للصفوف والشعب الدراسية", "تۆماری ئاماری گشتی پۆل و لقەکانی خوێندن", "Sınıf ve Şubeler Genel İstatistik Raporu", "General Class & Section Statistical Report")
    )

    private val exactMap: Map<String, QuadTranslation> by lazy {
        (rawTranslations + AppLocalizationDictionaryExt.extendedPhrases).associateBy { it.ar.trim() }
    }

    /**
     * Ordered phrase replacements (longest first) for compound/dynamic sentences.
     */
    private val phraseReplacements: List<QuadTranslation> by lazy {
        (rawTranslations + AppLocalizationDictionaryExt.extendedPhrases + listOf(
            QuadTranslation("العام الدراسي:", "ساڵی خوێندن:", "Eğitim Yılı:", "Academic Year:"),
            QuadTranslation("العام الدراسي", "ساڵی خوێندن", "Eğitim Yılı", "Academic Year"),
            QuadTranslation("كود المدرسة:", "کۆدی قوتابخانە:", "Okul Kodu:", "School Code:"),
            QuadTranslation("كود الطالب:", "کۆدی قوتابی:", "Öğrenci Kodu:", "Student Code:"),
            QuadTranslation("كود ولي الأمر:", "کۆدی بەخێوکەر:", "Veli Kodu:", "Parent Code:"),
            QuadTranslation("تاريخ الإصدار:", "بەرواری دەرچوون:", "Düzenleme Tarihi:", "Issue Date:"),
            QuadTranslation("رقم الوثيقة:", "ژمارەی بەڵگەنامە:", "Belge No:", "Doc No:"),
            QuadTranslation("الفترة:", "ماوە:", "Dönem:", "Period:"),
            QuadTranslation("الصف:", "پۆل:", "Sınıf:", "Grade:"),
            QuadTranslation("الشعبة:", "لق:", "Şube:", "Section:"),
            QuadTranslation("الطالب:", "قوتابی:", "Öğrenci:", "Student:"),
            QuadTranslation("المادة الدراسية", "بابەتی خوێندن", "Ders Adı", "Subject"),
            QuadTranslation("المادة", "بابەت", "Ders", "Subject"),
            QuadTranslation("عدد المواد الدراسية", "ژمارەی بابەتەکان", "Ders Sayısı", "Total Subjects"),
            QuadTranslation("المواد الناجح بها", "بابەتە سەرکەوتووەکان", "Geçilen Dersler", "Passed Subjects"),
            QuadTranslation("المعدل العام المئوي", "تێکڕای گشتی سەدی", "Genel Yüzde Ortalama", "Overall Percentage"),
            QuadTranslation("إجمالي طلاب الشعبة", "کۆی قوتابییانی لق", "Şube Öğrenci Sayısı", "Section Students"),
            QuadTranslation("نسبة المواظبة العامة", "ڕێژەی پابەندبوونی گشتی", "Genel Devam Oranı", "Attendance Rate"),
            QuadTranslation("مجموع أيام الحضور", "کۆی ڕۆژانی ئامادەبوون", "Toplam Devam Günü", "Total Present Days"),
            QuadTranslation("مجموع الغياب والإجازات", "کۆی نەهاتن و مۆڵەتەکان", "Toplam Devamsızlık ve İzin", "Total Absent & Excused"),
            QuadTranslation("أيام الحضور", "ڕۆژانی ئامادەبوون", "Devam Günleri", "Present Days"),
            QuadTranslation("أيام الغياب", "ڕۆژانی نەهاتن", "Devamsızlık", "Absent Days"),
            QuadTranslation("تأخر / مجاز", "دواکەوتوو / مۆڵەت", "Geç / İzinli", "Late / Excused"),
            QuadTranslation("نسبة الالتزام", "ڕێژەی پابەندبوون", "Devam Oranı", "Commitment"),
            QuadTranslation("مصادقة مدير المدرسة:", "پەسەندکردنی بەڕێوەبەری قوتابخانە:", "Okul Müdiri Onayı:", "Principal Approval:"),
            QuadTranslation("بطاقة الدرجات الرسمية", "کارتی فەرمی نمرەکان", "Resmi Not Karnesi", "Official Report Card"),
            QuadTranslation("الكارت الشامل لجميع الفصول", "کارتی گشتگیر بۆ هەموو وەرزەکان", "Tüm Dönemler Kapsamlı Karne", "Comprehensive All-Terms Card"),
            QuadTranslation("الأوسمة:", "مەدالیاکان:", "Madalyalar:", "Badges:"),
            QuadTranslation("وسام التفوق الذهبي", "مەدالیای زێڕینی سەرکەوتن", "Altın Başarı Madalyası", "Golden Excellence Medal"),
            QuadTranslation("وسام الالتزام الذهبي", "مەدالیای زێڕینی پابەندبوون", "Altın Devam Madalyası", "Golden Commitment Medal"),
            QuadTranslation("هوية مدرسية معتمدة صادرة عبر منصة مدرستي الذكية", "ناسنامەی قوتابخانەی باوەڕپێکراو دەرچوو لە پلاتفۆرمی زیرەکی قوتابخانەکەم", "Akıllı Mektebim Platformu tarafından düzenlenen onaylı okul kimliği", "Certified School ID issued via Madrasati Smart Platform"),
            QuadTranslation("هاتف الإدارة:", "مۆبایلی کارگێڕی:", "Yönetim Tel:", "Admin Phone:"),
            QuadTranslation("غير مسجل", "تۆمارنەکراو", "Kayıtsız", "Not Registered"),
            QuadTranslation("إشادة يومية", "ستایشی ڕۆژانە", "Günlük Övgü", "Daily Praise"),
            QuadTranslation("مشارك ممتاز", "بەشداربووی نایاب", "Mükemmel Katılım", "Excellent Participation"),
            QuadTranslation("نسي الكتاب", "کتێبی بیرچووە", "Kitap Unuttu", "Forgot Book"),
            QuadTranslation("لم يحل الواجب", "ئەرکی جێبەجێ نەکردووە", "Ödev Yapmadı", "Missed Homework"),
            QuadTranslation("ملاحظة سلوكية", "تێبینی ڕەفتاری", "Davranış Notu", "Behavior Note"),
            QuadTranslation("إجازة مرضية", "مۆڵەتی نەخۆشی", "Hastalık İzni", "Sick Leave"),
            QuadTranslation("إجازة زمنية", "مۆڵەتی کاتی", "Saatlik İzin", "Time Leave"),
            QuadTranslation("عذر عائلي", "بیانووی خێزانی", "Ailevi Mazeret", "Family Excuse"),
            QuadTranslation("قيد المراجعة", "لەژێر پێداچوونەوە", "İnceleniyor", "Pending Review"),
            QuadTranslation("تمت الموافقة", "پەسەندکرا", "Onaylandı", "Approved"),
            QuadTranslation("مرفوضة", "ڕەتکرایەوە", "Reddedildi", "Rejected"),
            QuadTranslation("شكر وتقدير", "سوپاس و پێزانین", "Teşekkür ve Takdir", "Appreciation"),
            QuadTranslation("ملاحظة علمية", "تێبینی زانستی", "Akademik Not", "Academic Note"),
            QuadTranslation("تنبيه سلوكي", "ئاگادارکردنەوەی ڕەفتاری", "Davranış Uyarısı", "Behavior Alert"),
            QuadTranslation("استدعاء ولي أمر", "بانگهێشتی بەخێوکەر", "Veli Çağrısı", "Parent Summons"),
            QuadTranslation("د.ع", "د.ع", "IQD", "IQD"),
            QuadTranslation("دينار عراقي", "دیناری عێراقی", "Irak Dinarı", "Iraqi Dinar"),
            QuadTranslation("مادة", "بابەت", "Ders", "Subject(s)"),
            QuadTranslation("يوم", "ڕۆژ", "Gün", "Day(s)")
        )).distinctBy { it.ar }.sortedByDescending { it.ar.length }
    }

    /**
     * Checks whether [c] is an Arabic letter/diacritic or alphanumeric word character.
     * Used by the Safe Token Replacement Rule so dictionary keys NEVER slice the middle of an Arabic word
     * (e.g. preventing "حاضر" from matching inside "الحاضرون", "من" inside "الأنظمة", "مدرس" inside "للمدرسة", or "يومي" inside "يومية").
     */
    private fun isWordBoundaryChar(c: Char): Boolean {
        if (c in '\uE000'..'\uF8FF') return true // Private-use placeholder boundary
        if (c in '\u0600'..'\u06FF' || c in '\u0750'..'\u077F' || c in '\u08A0'..'\u08FF') {
            // Arabic punctuation like ، ؛ ؟ are boundaries; Arabic letters/diacritics are word chars
            return c == '،' || c == '؛' || c == '؟' || c == '٪' || c == '٬' || c == '٫'
        }
        return !Character.isLetterOrDigit(c)
    }

    /**
     * Safe Token Replacement Rule (Longest-Match-First & Full Word Tokenization):
     * Replaces occurrences of [arabicPhrase] in [source] ONLY when bounded by word boundaries
     * (start/end of string, spaces, or punctuation) and substitutes a protected placeholder token
     * so already-translated segments are never re-processed or corrupted.
     */
    private fun replaceWholePhraseWithProtectedToken(
        source: String,
        arabicPhrase: String,
        translatedValue: String,
        protectedTokens: MutableList<String>
    ): String {
        if (arabicPhrase.isEmpty() || !source.contains(arabicPhrase)) return source
        val sb = StringBuilder(source.length + 16)
        var searchFrom = 0
        val phraseLen = arabicPhrase.length
        while (searchFrom < source.length) {
            val idx = source.indexOf(arabicPhrase, searchFrom)
            if (idx < 0) {
                sb.append(source, searchFrom, source.length)
                break
            }
            val endIdx = idx + phraseLen
            val leftOk = idx == 0 || isWordBoundaryChar(source[idx - 1]) || isWordBoundaryChar(arabicPhrase.first())
            val rightOk = endIdx == source.length || isWordBoundaryChar(source[endIdx]) || isWordBoundaryChar(arabicPhrase.last())

            if (leftOk && rightOk) {
                sb.append(source, searchFrom, idx)
                val tokenIndex = protectedTokens.size
                protectedTokens.add(translatedValue)
                sb.append('\uE000').append(tokenIndex).append('\uE001')
                searchFrom = endIdx
            } else {
                sb.append(source, searchFrom, idx + 1)
                searchFrom = idx + 1
            }
        }
        return sb.toString()
    }

    private val pageOfRegex = Regex("""صفحة\s+(\d+)\s+من\s+(\d+)""")

    /**
     * Translates any Arabic string (exact UI label, compound sentence, grade, section, subject, status, or PDF text)
     * into the active language (`ARABIC`, `KURDISH`, `TURKMEN`, `ENGLISH`).
     */
    fun localize(raw: String, lang: AppLanguage = currentLanguageState): String {
        if (lang == AppLanguage.ARABIC || raw.isBlank()) return raw
        val trimmed = raw.trim()

        // 1. Direct Exact Match
        exactMap[trimmed]?.let { return it.forLanguage(lang) }

        // 2. Single-letter Arabic section names ("أ", "ب", "ج", "د", "هـ", "و")
        when (trimmed) {
            "أ", "ا" -> return when (lang) {
                AppLanguage.ARABIC -> "أ"
                AppLanguage.KURDISH -> "ئـ"
                AppLanguage.TURKMEN, AppLanguage.ENGLISH -> "A"
            }
            "ب" -> return when (lang) {
                AppLanguage.ARABIC, AppLanguage.KURDISH -> "ب"
                AppLanguage.TURKMEN, AppLanguage.ENGLISH -> "B"
            }
            "ج" -> return when (lang) {
                AppLanguage.ARABIC, AppLanguage.KURDISH -> "ج"
                AppLanguage.TURKMEN, AppLanguage.ENGLISH -> "C"
            }
            "د" -> return when (lang) {
                AppLanguage.ARABIC, AppLanguage.KURDISH -> "د"
                AppLanguage.TURKMEN, AppLanguage.ENGLISH -> "D"
            }
            "هـ", "ه" -> return when (lang) {
                AppLanguage.ARABIC, AppLanguage.KURDISH -> "هـ"
                AppLanguage.TURKMEN, AppLanguage.ENGLISH -> "E"
            }
            "و" -> return when (lang) {
                AppLanguage.ARABIC, AppLanguage.KURDISH -> "و"
                AppLanguage.TURKMEN, AppLanguage.ENGLISH -> "F"
            }
            "ت" -> return when (lang) {
                AppLanguage.ARABIC -> "ت"
                AppLanguage.KURDISH -> "ز"
                AppLanguage.TURKMEN -> "No"
                AppLanguage.ENGLISH -> "#"
            }
            "ش1" -> return when (lang) {
                AppLanguage.ARABIC -> "ش1"
                AppLanguage.KURDISH -> "م١"
                AppLanguage.TURKMEN -> "A1"
                AppLanguage.ENGLISH -> "M1"
            }
            "ش2" -> return when (lang) {
                AppLanguage.ARABIC -> "ش2"
                AppLanguage.KURDISH -> "م٢"
                AppLanguage.TURKMEN -> "A2"
                AppLanguage.ENGLISH -> "M2"
            }
            "يومي" -> return when (lang) {
                AppLanguage.ARABIC -> "يومي"
                AppLanguage.KURDISH -> "ڕۆژانە"
                AppLanguage.TURKMEN -> "Günlük"
                AppLanguage.ENGLISH -> "Daily"
            }
        }

        // 3. Compound & Dynamic Pattern Replacement (Longest-Match-First + Protected Word Boundaries)
        var working = raw

        // Format "صفحة X من Y" cleanly before general tokenization
        if (working.contains("صفحة") && working.contains("من")) {
            working = pageOfRegex.replace(working) { match ->
                val p = match.groupValues[1]
                val total = match.groupValues[2]
                when (lang) {
                    AppLanguage.ARABIC -> "صفحة $p من $total"
                    AppLanguage.KURDISH -> "لاپەڕە $p لە $total"
                    AppLanguage.TURKMEN -> "Sahypa $p / $total"
                    AppLanguage.ENGLISH -> "Page $p of $total"
                }
            }
        }

        val protectedTokens = mutableListOf<String>()

        // Replace parenthesized or explicit section labels first
        if (lang == AppLanguage.TURKMEN || lang == AppLanguage.ENGLISH) {
            working = working
                .replace("(أ)", "(A)")
                .replace("(ب)", "(B)")
                .replace("(ج)", "(C)")
                .replace("(د)", "(D)")
            val secPrefix = if (lang == AppLanguage.TURKMEN) "Şube" else "Section"
            working = replaceWholePhraseWithProtectedToken(working, "شعبة أ", "$secPrefix A", protectedTokens)
            working = replaceWholePhraseWithProtectedToken(working, "شعبة ب", "$secPrefix B", protectedTokens)
            working = replaceWholePhraseWithProtectedToken(working, "شعبة ج", "$secPrefix C", protectedTokens)
            working = replaceWholePhraseWithProtectedToken(working, "شعبة د", "$secPrefix D", protectedTokens)
        } else if (lang == AppLanguage.KURDISH) {
            working = replaceWholePhraseWithProtectedToken(working, "شعبة أ", "لقی ئـ", protectedTokens)
            working = replaceWholePhraseWithProtectedToken(working, "شعبة ب", "لقی ب", protectedTokens)
            working = replaceWholePhraseWithProtectedToken(working, "شعبة ج", "لقی ج", protectedTokens)
            working = replaceWholePhraseWithProtectedToken(working, "شعبة د", "لقی د", protectedTokens)
        }

        for (entry in phraseReplacements) {
            if (working.contains(entry.ar)) {
                working = replaceWholePhraseWithProtectedToken(
                    source = working,
                    arabicPhrase = entry.ar,
                    translatedValue = entry.forLanguage(lang),
                    protectedTokens = protectedTokens
                )
            }
        }

        if (protectedTokens.isNotEmpty()) {
            val restored = StringBuilder(working.length + 32)
            var i = 0
            while (i < working.length) {
                if (working[i] == '\uE000') {
                    val endMarker = working.indexOf('\uE001', i + 1)
                    if (endMarker > i + 1) {
                        val tokenIdx = working.substring(i + 1, endMarker).toIntOrNull()
                        if (tokenIdx != null && tokenIdx in protectedTokens.indices) {
                            restored.append(protectedTokens[tokenIdx])
                            i = endMarker + 1
                            continue
                        }
                    }
                }
                restored.append(working[i])
                i++
            }
            working = restored.toString()
        }

        return working
    }
}

/**
 * 3D Glassmorphism Circular Language Badge (38.dp Circle):
 * Displays the active language code (`ع` for Arabic, `ک` for Kurdish, `TR` for Turkmen, `EN` for English)
 * inside a sleek 38.dp glass circle and opens the 4-Language Selection Dialog on tap.
 */
@Composable
fun LanguageSwitcherGlassCapsule(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val activeLang = LocalAppLanguage.current
    val circleSize = 38.dp
    Surface(
        modifier = modifier
            .requiredSize(circleSize)
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable { AppLocalizationEngine.isLanguageModalOpen = true }
            .testTag("language_switcher_capsule"),
        shape = CircleShape,
        color = Color(0x33FFFFFF),
        border = BorderStroke(1.2.dp, Color(0xCCF5D061)),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0D9488).copy(alpha = 0.90f),
                            Color(0xFF064E3B).copy(alpha = 0.95f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = activeLang.badgeSymbol,
                style = MaterialTheme.typography.labelLarge,
                fontSize = if (activeLang.badgeSymbol.length == 1) 14.5.sp else 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFFDF7),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 3D Glassmorphism 4-Language Selection Modal (Arabic, Kurdish, Turkmen, English).
 * Switching the language updates the entire platform and layout direction (RTL/LTR) in real time.
 */
@Composable
fun LanguageSelectionGlassModal() {
    if (!AppLocalizationEngine.isLanguageModalOpen) return
    val context = LocalContext.current
    val activeLang = LocalAppLanguage.current

    Dialog(
        onDismissRequest = { AppLocalizationEngine.isLanguageModalOpen = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA021713))
                .clickable { AppLocalizationEngine.isLanguageModalOpen = false }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .shadow(24.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF042F2E)),
                border = BorderStroke(1.5.dp, Color(0xFFD4AF37))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF03221C),
                                    Color(0xFF084C3E),
                                    Color(0xFF052E25)
                                )
                            )
                        )
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 3D Glassmorphism MADRASATI Header Capsule
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x28FFFFFF),
                        border = BorderStroke(1.dp, Color(0xAAF5D061))
                    ) {
                        androidx.compose.material3.Text(
                            text = "MADRASATI • عربي | کوردی | Türkmençe | EN",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp
                        )
                    }

                    androidx.compose.material3.Text(
                        text = tr(
                            ar = "اختيار لغة المنصة الرسمية",
                            ku = "هەڵبژاردنی زمانی فەرمی پلاتفۆرم",
                            tr = "Resmi Platform Dilini Seçin",
                            en = "Select Official Platform Language",
                            lang = activeLang
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    androidx.compose.material3.Text(
                        text = tr(
                            ar = "يتم تحويل جميع الشاشات والأزرار والمناهج والشهادات والاتجاه (RTL / LTR) فوراً",
                            ku = "هەموو شاشە و دوگمە و پڕۆگرام و بڕوانامەکان دەستبەجێ دەگۆڕێن",
                            tr = "Tüm ekranlar, düğmeler, müfredat ve karneler anında seçilen dile çevrilir",
                            en = "All screens, buttons, curriculum, report cards & layout direction switch instantly",
                            lang = activeLang
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    AppLanguage.entries.forEach { langOption ->
                        val isSelected = langOption == activeLang
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    AppLocalizationEngine.setLanguage(context, langOption)
                                    AppLocalizationEngine.isLanguageModalOpen = false
                                }
                                .testTag("select_lang_${langOption.code}"),
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) Color(0x38F5D061) else Color(0x1FFFFFFF),
                            border = BorderStroke(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) Color(0xFFF5D061) else Color(0x44FFFFFF)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    androidx.compose.material3.Text(
                                        text = langOption.nativeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color(0xFFFFFDF7) else Color.White
                                    )
                                    androidx.compose.material3.Text(
                                        text = langOption.subtitleName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) Color(0xFFFDE68A) else Color(0xFFCCFBF1),
                                        fontSize = 11.5.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) Color(0xFFD4AF37) else Color(0x33000000),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFFDF7) else Color(0x55FFFFFF))
                                ) {
                                    androidx.compose.material3.Text(
                                        text = if (isSelected) {
                                            tr("مفعّلة الآن", "چالاکە", "Aktif", "Active", langOption)
                                        } else {
                                            langOption.shortCodeLabel
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color(0xFF03221C) else Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .clickable { AppLocalizationEngine.isLanguageModalOpen = false }
                            .testTag("close_language_modal_btn"),
                        shape = RoundedCornerShape(50),
                        color = Color(0x28FFFFFF),
                        border = BorderStroke(1.dp, Color(0x66FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = tr(
                                    ar = "إغلاق",
                                    ku = "داخستن",
                                    tr = "Kapat",
                                    en = "Close",
                                    lang = activeLang
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
