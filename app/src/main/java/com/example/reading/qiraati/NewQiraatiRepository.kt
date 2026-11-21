package com.example.reading.qiraati

import androidx.compose.ui.graphics.Color

/**
 * المستودع المركزي القابل للتوسّع لكتاب "قراءتي الجديدة — الصف الأول الابتدائي".
 *
 * لإضافة أي وحدة جديدة مستقبلاً (مثل الوحدة الثانية: مدرستي):
 * 1. أنشئ ملف بيانات واحد (مثل Unit2MadrasatiData.kt) يعرّف `val unit: QiraatiUnit`.
 * 2. استبدل عنصر الوحدة القادمة هنا بـ `Unit2MadrasatiData.unit` دون تعديل أي ملف آخر.
 */
object NewQiraatiRepository {

    const val BOOK_TITLE: String = "قراءتي الجديدة"
    const val BOOK_SUBTITLE: String = "الصف الأول الابتدائي — المنهج العراقي"

    private val upcomingUnitsCatalog: List<QiraatiUnit> = listOf(
        QiraatiUnit(
            id = "unit_2_madrasati",
            unitNumber = 2,
            unitBadge = "الوحدة الثانية",
            title = "الوحدة الثانية: مدرستي",
            shortTitle = "مدرستي",
            description = "قريباً — رحلة مشوّقة داخل الصف وساحة المدرسة مع مجموعة جديدة من الحروف والمقاطع",
            themeColor = Color(0xFF0EA5E9),
            theme = QiraatiUnitTheme(
                primaryColor = Color(0xFF0EA5E9),
                shadowColor = Color(0xFF0369A1),
                softSurfaceColor = Color(0xFFF0F9FF),
                badgeColor = Color(0xFF38BDF8)
            ),
            storySection = null,
            letterLessons = emptyList(),
            isAvailable = false,
            comingSoonLabel = "قريباً",
            plannedLetters = listOf("ن", "ر", "ل", "س"),
            coverArtKey = QiraatiStoryArtKey.UPCOMING_SCHOOL
        ),
        QiraatiUnit(
            id = "unit_3_madinati",
            unitNumber = 3,
            unitBadge = "الوحدة الثالثة",
            title = "الوحدة الثالثة: مدينتي وقريتي",
            shortTitle = "مدينتي وقريتي",
            description = "قريباً — حكايات الجيران والطرقات والحقول الجميلة مع أنشطة التهجئة والتحليل",
            themeColor = Color(0xFF22C55E),
            theme = QiraatiUnitTheme(
                primaryColor = Color(0xFF22C55E),
                shadowColor = Color(0xFF15803D),
                softSurfaceColor = Color(0xFFF0FDF4),
                badgeColor = Color(0xFF4ADE80)
            ),
            storySection = null,
            letterLessons = emptyList(),
            isAvailable = false,
            comingSoonLabel = "قريباً",
            plannedLetters = listOf("ق", "ت", "ز", "ك"),
            coverArtKey = QiraatiStoryArtKey.UPCOMING_CITY_VILLAGE
        ),
        QiraatiUnit(
            id = "unit_4_sihati",
            unitNumber = 4,
            unitBadge = "الوحدة الرابعة",
            title = "الوحدة الرابعة: صحتي وسلامتي",
            shortTitle = "صحتي وسلامتي",
            description = "قريباً — عادات النظافة والغذاء الصحي والنشاط اليومي مع حروف جديدة ممتعة",
            themeColor = Color(0xFF8B5CF6),
            theme = QiraatiUnitTheme(
                primaryColor = Color(0xFF8B5CF6),
                shadowColor = Color(0xFF6233C7),
                softSurfaceColor = Color(0xFFF5F3FF),
                badgeColor = Color(0xFFA78BFA)
            ),
            storySection = null,
            letterLessons = emptyList(),
            isAvailable = false,
            comingSoonLabel = "قريباً",
            plannedLetters = listOf("ط", "ح", "ص", "ش"),
            coverArtKey = QiraatiStoryArtKey.UPCOMING_HEALTH
        ),
        QiraatiUnit(
            id = "unit_5_biati",
            unitNumber = 5,
            unitBadge = "الوحدة الخامسة",
            title = "الوحدة الخامسة: بيئتي الجميلة",
            shortTitle = "بيئتي الجميلة",
            description = "قريباً — الأشجار والأنهار والطيور في ربوع بلادنا مع تدريبات القراءة الطلقة",
            themeColor = Color(0xFFF59E0B),
            theme = QiraatiUnitTheme(
                primaryColor = Color(0xFFF59E0B),
                shadowColor = Color(0xFFB45309),
                softSurfaceColor = Color(0xFFFFFBEB),
                badgeColor = Color(0xFFFCD34D)
            ),
            storySection = null,
            letterLessons = emptyList(),
            isAvailable = false,
            comingSoonLabel = "قريباً",
            plannedLetters = listOf("ع", "خ", "ج", "ذ"),
            coverArtKey = QiraatiStoryArtKey.UPCOMING_NATURE
        ),
        QiraatiUnit(
            id = "unit_6_watani",
            unitNumber = 6,
            unitBadge = "الوحدة السادسة",
            title = "الوحدة السادسة: وطني العراق",
            shortTitle = "وطني العراق",
            description = "قريباً — ختام الرحلة مع قصص الوطن الجميل وإتقان جميع الحروف العربية",
            themeColor = Color(0xFFEC4899),
            theme = QiraatiUnitTheme(
                primaryColor = Color(0xFFEC4899),
                shadowColor = Color(0xFFBE185D),
                softSurfaceColor = Color(0xFFFDF2F8),
                badgeColor = Color(0xFFF472B6)
            ),
            storySection = null,
            letterLessons = emptyList(),
            isAvailable = false,
            comingSoonLabel = "قريباً",
            plannedLetters = listOf("غ", "ظ", "ض", "ث"),
            coverArtKey = QiraatiStoryArtKey.UPCOMING_IRAQ
        )
    )

    /**
     * جميع وحدات خريطة كتاب "قراءتي الجديدة" (المتوفرة حالياً + الوحدات القادمة).
     */
    val allUnits: List<QiraatiUnit> = listOf(
        Unit1UsratyData.unit
    ) + upcomingUnitsCatalog

    /** الوحدات المتوفرة والجاهزة للتعلّم حالياً. */
    val availableUnits: List<QiraatiUnit>
        get() = allUnits.filter { it.isAvailable }

    /** الوحدات القادمة (قريباً) لإظهار خريطة مشوّقة قابلة للتوسّع. */
    val upcomingUnits: List<QiraatiUnit>
        get() = allUnits.filterNot { it.isAvailable }

    /** جميع دروس الحروف المتوفرة حالياً عبر الوحدات المفعّلة. */
    val allAvailableLessons: List<QiraatiLetterLesson>
        get() = availableUnits.flatMap { it.letterLessons }

    fun getUnitById(unitId: String): QiraatiUnit? =
        allUnits.firstOrNull { it.id == unitId }

    fun getUnitByNumber(unitNumber: Int): QiraatiUnit? =
        allUnits.firstOrNull { it.unitNumber == unitNumber }

    fun getLessonById(lessonId: String): QiraatiLetterLesson? =
        allAvailableLessons.firstOrNull { it.id == lessonId }
}
