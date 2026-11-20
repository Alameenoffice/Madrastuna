package com.example.reading

import androidx.compose.ui.graphics.Color

/**
 * نوع المقطع الصوتي وفق الطريقة المقطعية في منهج القراءة العراقي.
 */
enum class SyllableType(val arabicLabel: String) {
    LONG_ALIF("مد بالألف"),
    LONG_WAW("مد بالواو"),
    LONG_YA("مد بالياء"),
    SHORT_FATHA("مقطع مفتوح"),
    SHORT_DAMMA("مقطع مضموم"),
    SHORT_KASRA("مقطع مكسور"),
    CONSONANT("حرف مفرد")
}

/**
 * لوحة ألوان واضحة وحيوية ومناسبة للأطفال لتمييز المقاطع الصوتية بصرياً.
 */
object ReadingSyllablePalette {
    const val EMERALD = 0xFF059669L
    const val ROYAL_BLUE = 0xFF2563EBL
    const val CORAL_ORANGE = 0xFFEA580CL
    const val VIOLET = 0xFF7C3AEDL
    const val ROSE = 0xFFE11D48L
    const val TEAL = 0xFF0D9488L
    const val AMBER = 0xFFD97706L
    const val INDIGO = 0xFF4F46E5L
}

/**
 * نقطة إرشادية مُطبّعة (من 0.0 إلى 1.0) داخل لوحة التتبع لتعليم الطفل بداية ومسار كتابة الحرف.
 */
data class WritingGuidePoint(
    val x: Float,
    val y: Float,
    val stepNumber: Int = 1,
    val isDot: Boolean = false
)

/**
 * دليل كتابة الحرف العربي: يحدد نقطة البداية، مسار التتبع، النقاط، وتوجيه كتابي مبسط للطفل.
 */
data class LetterWritingGuide(
    val letter: String,
    val letterName: String,
    val startPoint: WritingGuidePoint,
    val strokePath: List<WritingGuidePoint>,
    val dotPoints: List<WritingGuidePoint> = emptyList(),
    val directionHint: String,
    val baselineRatio: Float = 0.68f
)

/**
 * مقطع صوتي ملوّن مع نص عرض مرئي ونص نطقي مضبوط لمحرك النطق العربي.
 */
data class ColoredSyllable(
    val text: String,
    val spokenText: String = text,
    val colorArgb: Long,
    val type: SyllableType = SyllableType.CONSONANT
) {
    /** تحويل قيمة اللون الرقمية إلى كائن Color جاهز لواجهات Compose. */
    fun toComposeColor(): Color = Color(colorArgb)

    /** نص منظف من التطويل (ـ) لضمان نطق سليم عبر محرك TTS العربي. */
    val ttsCleanText: String
        get() = spokenText.replace("ـ", "").trim()
}

/**
 * كلمة مقطّعة إلى مقاطعها الملونة مع شكلها الكامل وتوضيح مبسط للطفل.
 */
data class SegmentedWord(
    val id: String,
    val fullWord: String,
    val spokenWord: String = fullWord,
    val syllables: List<ColoredSyllable>,
    val hint: String = ""
)

/**
 * جملة قرائية مكونة من كلمات مقطّعة ومناسبة لمستوى الدرس.
 */
data class ReadingSentence(
    val fullSentence: String,
    val words: List<SegmentedWord>
)

/**
 * نموذج درس متكامل ضمن منهج واحة القراءة الذكية.
 */
data class ReadingLesson(
    val id: String,
    val order: Int,
    val title: String,
    val subtitle: String,
    val targetLetters: List<String>,
    val primaryColorArgb: Long,
    val syllables: List<ColoredSyllable>,
    val words: List<SegmentedWord>,
    val sentences: List<ReadingSentence>,
    val letterGuides: List<LetterWritingGuide>,
    val maxStars: Int = 3
) {
    fun primaryColor(): Color = Color(primaryColorArgb)
}

/**
 * المنهج العراقي المتدرج لتعليم القراءة بالطريقة المقطعية (8 دروس أساسية متكاملة).
 */
object ReadingCurriculum {

    // --- تعريف الكلمات الأساسية المشتركة لإعادة استخدامها بدقة في الجمل ---
    private val wordDada = SegmentedWord(
        id = "w_dada",
        fullWord = "دادا",
        spokenWord = "دادا",
        syllables = listOf(
            ColoredSyllable("دا", "دا", ReadingSyllablePalette.EMERALD, SyllableType.LONG_ALIF),
            ColoredSyllable("دا", "دا", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_ALIF)
        ),
        hint = "أول كلمة نتعلمها في القراءة"
    )

    private val wordDar = SegmentedWord(
        id = "w_dar",
        fullWord = "دار",
        spokenWord = "دار",
        syllables = listOf(
            ColoredSyllable("دا", "دا", ReadingSyllablePalette.EMERALD, SyllableType.LONG_ALIF),
            ColoredSyllable("ر", "رْ", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.CONSONANT)
        ),
        hint = "البيت الجميل الذي نسكن فيه"
    )

    private val wordDoor = SegmentedWord(
        id = "w_door",
        fullWord = "دور",
        spokenWord = "دُور",
        syllables = listOf(
            ColoredSyllable("دو", "دو", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_WAW),
            ColoredSyllable("ر", "رْ", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.CONSONANT)
        ),
        hint = "جمع دار أي بيوت كثيرة"
    )

    private val wordBaba = SegmentedWord(
        id = "w_baba",
        fullWord = "بابا",
        spokenWord = "بابا",
        syllables = listOf(
            ColoredSyllable("با", "با", ReadingSyllablePalette.VIOLET, SyllableType.LONG_ALIF),
            ColoredSyllable("با", "با", ReadingSyllablePalette.ROSE, SyllableType.LONG_ALIF)
        ),
        hint = "أبي الغالي"
    )

    private val wordBab = SegmentedWord(
        id = "w_bab",
        fullWord = "باب",
        spokenWord = "باب",
        syllables = listOf(
            ColoredSyllable("با", "با", ReadingSyllablePalette.VIOLET, SyllableType.LONG_ALIF),
            ColoredSyllable("ب", "بْ", ReadingSyllablePalette.TEAL, SyllableType.CONSONANT)
        ),
        hint = "مدخل الدار"
    )

    private val wordMama = SegmentedWord(
        id = "w_mama",
        fullWord = "ماما",
        spokenWord = "ماما",
        syllables = listOf(
            ColoredSyllable("ما", "ما", ReadingSyllablePalette.ROSE, SyllableType.LONG_ALIF),
            ColoredSyllable("ما", "ما", ReadingSyllablePalette.AMBER, SyllableType.LONG_ALIF)
        ),
        hint = "أمي الحبيبة"
    )

    private val wordNam = SegmentedWord(
        id = "w_nam",
        fullWord = "نام",
        spokenWord = "نامَ",
        syllables = listOf(
            ColoredSyllable("نا", "نا", ReadingSyllablePalette.TEAL, SyllableType.LONG_ALIF),
            ColoredSyllable("م", "مْ", ReadingSyllablePalette.ROSE, SyllableType.CONSONANT)
        ),
        hint = "خلد إلى النوم الهادئ"
    )

    private val wordNoor = SegmentedWord(
        id = "w_noor",
        fullWord = "نور",
        spokenWord = "نُور",
        syllables = listOf(
            ColoredSyllable("نو", "نو", ReadingSyllablePalette.INDIGO, SyllableType.LONG_WAW),
            ColoredSyllable("ر", "رْ", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.CONSONANT)
        ),
        hint = "الضياء والاسم الجميل"
    )

    private val wordNoori = SegmentedWord(
        id = "w_noori",
        fullWord = "نوري",
        spokenWord = "نوري",
        syllables = listOf(
            ColoredSyllable("نو", "نو", ReadingSyllablePalette.INDIGO, SyllableType.LONG_WAW),
            ColoredSyllable("ري", "ري", ReadingSyllablePalette.EMERALD, SyllableType.LONG_YA)
        ),
        hint = "اسم صديقنا في الدرس"
    )

    private val wordDari = SegmentedWord(
        id = "w_dari",
        fullWord = "داري",
        spokenWord = "داري",
        syllables = listOf(
            ColoredSyllable("دا", "دا", ReadingSyllablePalette.EMERALD, SyllableType.LONG_ALIF),
            ColoredSyllable("ري", "ري", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_YA)
        ),
        hint = "بيتي الذي أحبه"
    )

    private val wordBooq = SegmentedWord(
        id = "w_booq",
        fullWord = "بوق",
        spokenWord = "بُوق",
        syllables = listOf(
            ColoredSyllable("بو", "بو", ReadingSyllablePalette.VIOLET, SyllableType.LONG_WAW),
            ColoredSyllable("ق", "قْ", ReadingSyllablePalette.AMBER, SyllableType.CONSONANT)
        ),
        hint = "آلة تصدر صوتاً جميلاً"
    )

    private val wordNadeem = SegmentedWord(
        id = "w_nadeem",
        fullWord = "نديم",
        spokenWord = "نَديم",
        syllables = listOf(
            ColoredSyllable("نَـ", "نَ", ReadingSyllablePalette.TEAL, SyllableType.SHORT_FATHA),
            ColoredSyllable("دي", "دي", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_YA),
            ColoredSyllable("م", "مْ", ReadingSyllablePalette.ROSE, SyllableType.CONSONANT)
        ),
        hint = "صديقنا نديم"
    )

    private val wordBaqar = SegmentedWord(
        id = "w_baqar",
        fullWord = "بقر",
        spokenWord = "بَقَر",
        syllables = listOf(
            ColoredSyllable("بَـ", "بَ", ReadingSyllablePalette.VIOLET, SyllableType.SHORT_FATHA),
            ColoredSyllable("قَـ", "قَ", ReadingSyllablePalette.AMBER, SyllableType.SHORT_FATHA),
            ColoredSyllable("ر", "رْ", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.CONSONANT)
        ),
        hint = "حيوانات لطيفة تعطينا الحليب"
    )

    private val wordWaz = SegmentedWord(
        id = "w_waz",
        fullWord = "وز",
        spokenWord = "وَز",
        syllables = listOf(
            ColoredSyllable("وَ", "وَ", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.SHORT_FATHA),
            ColoredSyllable("ز", "زْ", ReadingSyllablePalette.EMERALD, SyllableType.CONSONANT)
        ),
        hint = "طائر يسبح في الماء"
    )

    private val wordZaid = SegmentedWord(
        id = "w_zaid",
        fullWord = "زيد",
        spokenWord = "زَيْد",
        syllables = listOf(
            ColoredSyllable("زَ", "زَ", ReadingSyllablePalette.EMERALD, SyllableType.SHORT_FATHA),
            ColoredSyllable("يـ", "يْ", ReadingSyllablePalette.INDIGO, SyllableType.CONSONANT),
            ColoredSyllable("د", "دْ", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.CONSONANT)
        ),
        hint = "صديقنا زيد"
    )

    private val wordZawraq = SegmentedWord(
        id = "w_zawraq",
        fullWord = "زورق",
        spokenWord = "زَوْرَق",
        syllables = listOf(
            ColoredSyllable("زَ", "زَ", ReadingSyllablePalette.EMERALD, SyllableType.SHORT_FATHA),
            ColoredSyllable("و", "وْ", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.CONSONANT),
            ColoredSyllable("رَ", "رَ", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.SHORT_FATHA),
            ColoredSyllable("ق", "قْ", ReadingSyllablePalette.VIOLET, SyllableType.CONSONANT)
        ),
        hint = "قارب صغير يسير في النهر"
    )

    // --- أدلة كتابة الحروف مع نقاط البداية ومسار اليد ---
    private val guideDal = LetterWritingGuide(
        letter = "د",
        letterName = "حرف الدال",
        startPoint = WritingGuidePoint(0.58f, 0.34f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.58f, 0.34f, 1),
            WritingGuidePoint(0.67f, 0.48f, 2),
            WritingGuidePoint(0.66f, 0.64f, 3),
            WritingGuidePoint(0.52f, 0.68f, 4),
            WritingGuidePoint(0.34f, 0.67f, 5)
        ),
        directionHint = "ابدأ من الأعلى يميناً، انزل بقوس لطيف إلى السطر ثم اتجه يساراً"
    )

    private val guideRa = LetterWritingGuide(
        letter = "ر",
        letterName = "حرف الراء",
        startPoint = WritingGuidePoint(0.62f, 0.46f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.62f, 0.46f, 1),
            WritingGuidePoint(0.64f, 0.62f, 2),
            WritingGuidePoint(0.56f, 0.76f, 3),
            WritingGuidePoint(0.42f, 0.83f, 4),
            WritingGuidePoint(0.30f, 0.80f, 5)
        ),
        directionHint = "ابدأ فوق السطر قليلاً ثم انزل بهلال رشيق تحت السطر نحو اليسار"
    )

    private val guideWaw = LetterWritingGuide(
        letter = "و",
        letterName = "حرف الواو",
        startPoint = WritingGuidePoint(0.56f, 0.50f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.56f, 0.50f, 1),
            WritingGuidePoint(0.47f, 0.44f, 2),
            WritingGuidePoint(0.48f, 0.58f, 3),
            WritingGuidePoint(0.60f, 0.58f, 4),
            WritingGuidePoint(0.57f, 0.74f, 5),
            WritingGuidePoint(0.42f, 0.84f, 6),
            WritingGuidePoint(0.30f, 0.81f, 7)
        ),
        directionHint = "ارسم دائرة صغيرة فوق السطر ثم انزل بذيل مقوّس يشبه حرف الراء"
    )

    private val guideBa = LetterWritingGuide(
        letter = "ب",
        letterName = "حرف الباء",
        startPoint = WritingGuidePoint(0.76f, 0.48f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.76f, 0.48f, 1),
            WritingGuidePoint(0.74f, 0.66f, 2),
            WritingGuidePoint(0.50f, 0.68f, 3),
            WritingGuidePoint(0.28f, 0.66f, 4),
            WritingGuidePoint(0.24f, 0.50f, 5)
        ),
        dotPoints = listOf(
            WritingGuidePoint(0.50f, 0.80f, stepNumber = 6, isDot = true)
        ),
        directionHint = "ابدأ من اليمين نزولاً إلى السطر، امشِ يساراً واصعد قليلاً، ثم ضع نقطة واحدة بالأسفل"
    )

    private val guideMeem = LetterWritingGuide(
        letter = "م",
        letterName = "حرف الميم",
        startPoint = WritingGuidePoint(0.58f, 0.44f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.58f, 0.44f, 1),
            WritingGuidePoint(0.68f, 0.52f, 2),
            WritingGuidePoint(0.58f, 0.60f, 3),
            WritingGuidePoint(0.44f, 0.56f, 4),
            WritingGuidePoint(0.40f, 0.72f, 5),
            WritingGuidePoint(0.40f, 0.86f, 6)
        ),
        directionHint = "ارسم حلقة صغيرة فوق السطر ثم اسحب خطاً مستقيمًا نحو الأسفل"
    )

    private val guideNoon = LetterWritingGuide(
        letter = "ن",
        letterName = "حرف النون",
        startPoint = WritingGuidePoint(0.70f, 0.46f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.70f, 0.46f, 1),
            WritingGuidePoint(0.68f, 0.70f, 2),
            WritingGuidePoint(0.50f, 0.80f, 3),
            WritingGuidePoint(0.32f, 0.70f, 4),
            WritingGuidePoint(0.30f, 0.48f, 5)
        ),
        dotPoints = listOf(
            WritingGuidePoint(0.50f, 0.44f, stepNumber = 6, isDot = true)
        ),
        directionHint = "ارسم صحناً عميقاً ينزل قليلاً تحت السطر، ثم ضع نقطة واحدة في الوسط بالأعلى"
    )

    private val guideYa = LetterWritingGuide(
        letter = "ي",
        letterName = "حرف الياء",
        startPoint = WritingGuidePoint(0.64f, 0.40f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.64f, 0.40f, 1),
            WritingGuidePoint(0.52f, 0.42f, 2),
            WritingGuidePoint(0.58f, 0.54f, 3),
            WritingGuidePoint(0.64f, 0.70f, 4),
            WritingGuidePoint(0.46f, 0.80f, 5),
            WritingGuidePoint(0.30f, 0.70f, 6),
            WritingGuidePoint(0.28f, 0.54f, 7)
        ),
        dotPoints = listOf(
            WritingGuidePoint(0.44f, 0.89f, stepNumber = 8, isDot = true),
            WritingGuidePoint(0.54f, 0.89f, stepNumber = 9, isDot = true)
        ),
        directionHint = "ارسم رأس البطة من اليمين، انزل بصحن جميل تحت السطر، ثم ضع نقطتين في الأسفل"
    )

    private val guideQaf = LetterWritingGuide(
        letter = "ق",
        letterName = "حرف القاف",
        startPoint = WritingGuidePoint(0.64f, 0.46f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.64f, 0.46f, 1),
            WritingGuidePoint(0.56f, 0.38f, 2),
            WritingGuidePoint(0.56f, 0.52f, 3),
            WritingGuidePoint(0.66f, 0.54f, 4),
            WritingGuidePoint(0.64f, 0.74f, 5),
            WritingGuidePoint(0.46f, 0.82f, 6),
            WritingGuidePoint(0.30f, 0.72f, 7),
            WritingGuidePoint(0.28f, 0.54f, 8)
        ),
        dotPoints = listOf(
            WritingGuidePoint(0.52f, 0.28f, stepNumber = 9, isDot = true),
            WritingGuidePoint(0.64f, 0.28f, stepNumber = 10, isDot = true)
        ),
        directionHint = "ارسم دائرة صغيرة فوق السطر، انزل بصحن عميق مثل النون، وضع نقطتين فوق الرأس"
    )

    private val guideZay = LetterWritingGuide(
        letter = "ز",
        letterName = "حرف الزاي",
        startPoint = WritingGuidePoint(0.62f, 0.46f, stepNumber = 1),
        strokePath = listOf(
            WritingGuidePoint(0.62f, 0.46f, 1),
            WritingGuidePoint(0.64f, 0.62f, 2),
            WritingGuidePoint(0.56f, 0.76f, 3),
            WritingGuidePoint(0.42f, 0.83f, 4),
            WritingGuidePoint(0.30f, 0.80f, 5)
        ),
        dotPoints = listOf(
            WritingGuidePoint(0.58f, 0.32f, stepNumber = 6, isDot = true)
        ),
        directionHint = "ارسم هلالاً جميلاً مثل حرف الراء تماماً، ثم ضع فوقه نقطة واحدة"
    )

    /**
     * الدروس الثمانية الأساسية لمنهج القراءة العراقي المتدرج بالطريقة المقطعية.
     */
    val lessons: List<ReadingLesson> = listOf(
        // الدرس 1: دادا
        ReadingLesson(
            id = "lesson_01_dada",
            order = 1,
            title = "دادا",
            subtitle = "الدرس الأول • حرف الدال (د)",
            targetLetters = listOf("د"),
            primaryColorArgb = ReadingSyllablePalette.EMERALD,
            syllables = listOf(
                ColoredSyllable("دا", "دا", ReadingSyllablePalette.EMERALD, SyllableType.LONG_ALIF),
                ColoredSyllable("دو", "دو", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_WAW),
                ColoredSyllable("دي", "دي", ReadingSyllablePalette.VIOLET, SyllableType.LONG_YA),
                ColoredSyllable("دَ", "دَ", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.SHORT_FATHA),
                ColoredSyllable("دُ", "دُ", ReadingSyllablePalette.TEAL, SyllableType.SHORT_DAMMA),
                ColoredSyllable("دِ", "دِ", ReadingSyllablePalette.ROSE, SyllableType.SHORT_KASRA)
            ),
            words = listOf(wordDada),
            sentences = listOf(
                ReadingSentence(
                    fullSentence = "دادا دادا",
                    words = listOf(wordDada, wordDada)
                )
            ),
            letterGuides = listOf(guideDal)
        ),

        // الدرس 2: دار
        ReadingLesson(
            id = "lesson_02_dar",
            order = 2,
            title = "دار",
            subtitle = "الدرس الثاني • حرف الراء (ر)",
            targetLetters = listOf("ر"),
            primaryColorArgb = ReadingSyllablePalette.CORAL_ORANGE,
            syllables = listOf(
                ColoredSyllable("را", "را", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.LONG_ALIF),
                ColoredSyllable("رو", "رو", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_WAW),
                ColoredSyllable("ري", "ري", ReadingSyllablePalette.EMERALD, SyllableType.LONG_YA),
                ColoredSyllable("رَ", "رَ", ReadingSyllablePalette.VIOLET, SyllableType.SHORT_FATHA)
            ),
            words = listOf(wordDar),
            sentences = listOf(
                ReadingSentence(
                    fullSentence = "دادا دار",
                    words = listOf(wordDada, wordDar)
                )
            ),
            letterGuides = listOf(guideRa)
        ),

        // الدرس 3: دور
        ReadingLesson(
            id = "lesson_03_door",
            order = 3,
            title = "دور",
            subtitle = "الدرس الثالث • حرف الواو (و)",
            targetLetters = listOf("و"),
            primaryColorArgb = ReadingSyllablePalette.ROYAL_BLUE,
            syllables = listOf(
                ColoredSyllable("دو", "دو", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_WAW),
                ColoredSyllable("رو", "رو", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.LONG_WAW),
                ColoredSyllable("وَ", "وَ", ReadingSyllablePalette.TEAL, SyllableType.SHORT_FATHA)
            ),
            words = listOf(wordDoor),
            sentences = listOf(
                ReadingSentence(
                    fullSentence = "دار دور",
                    words = listOf(wordDar, wordDoor)
                )
            ),
            letterGuides = listOf(guideWaw)
        ),

        // الدرس 4: بابا
        ReadingLesson(
            id = "lesson_04_baba",
            order = 4,
            title = "بابا",
            subtitle = "الدرس الرابع • حرف الباء (ب)",
            targetLetters = listOf("ب"),
            primaryColorArgb = ReadingSyllablePalette.VIOLET,
            syllables = listOf(
                ColoredSyllable("با", "با", ReadingSyllablePalette.VIOLET, SyllableType.LONG_ALIF),
                ColoredSyllable("بو", "بو", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_WAW),
                ColoredSyllable("بي", "بي", ReadingSyllablePalette.EMERALD, SyllableType.LONG_YA),
                ColoredSyllable("بَ", "بَ", ReadingSyllablePalette.ROSE, SyllableType.SHORT_FATHA)
            ),
            words = listOf(wordBaba, wordBab),
            sentences = listOf(
                ReadingSentence(
                    fullSentence = "دار بابا",
                    words = listOf(wordDar, wordBaba)
                )
            ),
            letterGuides = listOf(guideBa)
        ),

        // الدرس 5: ماما
        ReadingLesson(
            id = "lesson_05_mama",
            order = 5,
            title = "ماما",
            subtitle = "الدرس الخامس • حرف الميم (م)",
            targetLetters = listOf("م"),
            primaryColorArgb = ReadingSyllablePalette.ROSE,
            syllables = listOf(
                ColoredSyllable("ما", "ما", ReadingSyllablePalette.ROSE, SyllableType.LONG_ALIF),
                ColoredSyllable("مو", "مو", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_WAW),
                ColoredSyllable("مي", "مي", ReadingSyllablePalette.VIOLET, SyllableType.LONG_YA),
                ColoredSyllable("مَ", "مَ", ReadingSyllablePalette.AMBER, SyllableType.SHORT_FATHA)
            ),
            words = listOf(wordMama, wordNam),
            sentences = listOf(
                ReadingSentence(
                    fullSentence = "دادا نام",
                    words = listOf(wordDada, wordNam)
                )
            ),
            letterGuides = listOf(guideMeem)
        ),

        // الدرس 6: نور ونوري
        ReadingLesson(
            id = "lesson_06_noor_noori",
            order = 6,
            title = "نور ونوري",
            subtitle = "الدرس السادس • حرفا النون والياء (ن، ي)",
            targetLetters = listOf("ن", "ي"),
            primaryColorArgb = ReadingSyllablePalette.INDIGO,
            syllables = listOf(
                ColoredSyllable("نا", "نا", ReadingSyllablePalette.TEAL, SyllableType.LONG_ALIF),
                ColoredSyllable("نو", "نو", ReadingSyllablePalette.INDIGO, SyllableType.LONG_WAW),
                ColoredSyllable("ني", "ني", ReadingSyllablePalette.VIOLET, SyllableType.LONG_YA),
                ColoredSyllable("ري", "ري", ReadingSyllablePalette.EMERALD, SyllableType.LONG_YA)
            ),
            words = listOf(wordNoor, wordNoori, wordDari),
            sentences = listOf(
                ReadingSentence(
                    fullSentence = "دار نوري",
                    words = listOf(wordDar, wordNoori)
                )
            ),
            letterGuides = listOf(guideNoon, guideYa)
        ),

        // الدرس 7: بوق نديم
        ReadingLesson(
            id = "lesson_07_booq_nadeem",
            order = 7,
            title = "بوق نديم",
            subtitle = "الدرس السابع • حرف القاف (ق)",
            targetLetters = listOf("ق"),
            primaryColorArgb = ReadingSyllablePalette.AMBER,
            syllables = listOf(
                ColoredSyllable("قا", "قا", ReadingSyllablePalette.AMBER, SyllableType.LONG_ALIF),
                ColoredSyllable("قو", "قو", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_WAW),
                ColoredSyllable("قي", "قي", ReadingSyllablePalette.TEAL, SyllableType.LONG_YA),
                ColoredSyllable("قَ", "قَ", ReadingSyllablePalette.CORAL_ORANGE, SyllableType.SHORT_FATHA)
            ),
            words = listOf(wordBooq, wordNadeem, wordBaqar),
            sentences = listOf(
                ReadingSentence(
                    fullSentence = "بوق نديم",
                    words = listOf(wordBooq, wordNadeem)
                )
            ),
            letterGuides = listOf(guideQaf)
        ),

        // الدرس 8: وز وز
        ReadingLesson(
            id = "lesson_08_waz",
            order = 8,
            title = "وز وز",
            subtitle = "الدرس الثامن • حرف الزاي (ز)",
            targetLetters = listOf("ز"),
            primaryColorArgb = ReadingSyllablePalette.TEAL,
            syllables = listOf(
                ColoredSyllable("زا", "زا", ReadingSyllablePalette.TEAL, SyllableType.LONG_ALIF),
                ColoredSyllable("زو", "زو", ReadingSyllablePalette.ROYAL_BLUE, SyllableType.LONG_WAW),
                ColoredSyllable("زي", "زي", ReadingSyllablePalette.INDIGO, SyllableType.LONG_YA),
                ColoredSyllable("زَ", "زَ", ReadingSyllablePalette.EMERALD, SyllableType.SHORT_FATHA)
            ),
            words = listOf(wordWaz, wordZaid, wordZawraq),
            sentences = listOf(
                ReadingSentence(
                    fullSentence = "زورق زيد",
                    words = listOf(wordZawraq, wordZaid)
                )
            ),
            letterGuides = listOf(guideZay)
        )
    )

    /** البحث عن درس بواسطة المعرّف. */
    fun getLessonById(lessonId: String): ReadingLesson? =
        lessons.firstOrNull { it.id == lessonId }

    /** جلب الدرس التالي في التسلسل التدريجي. */
    fun getNextLesson(currentLessonId: String): ReadingLesson? {
        val idx = lessons.indexOfFirst { it.id == currentLessonId }
        return if (idx in 0 until lessons.lastIndex) lessons[idx + 1] else null
    }

    /** إجمالي النجوم الممكنة لجميع دروس المنهج. */
    val totalPossibleStars: Int
        get() = lessons.sumOf { it.maxStars }
}
