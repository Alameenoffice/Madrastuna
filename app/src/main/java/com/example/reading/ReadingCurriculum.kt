package com.example.reading

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.ArabicFontLoader

/**
 * خط تجوال (Tajawal) المعتمد في تطبيق مدرستي الذكية لجميع نصوص وحروف ومقاطع واحة القراءة.
 */
val ReadingTajawalFont: FontFamily
    get() = ArabicFontLoader.cachedFontFamily

/**
 * الهوية اللونية الطفولية المبهجة لقسم واحة القراءة الذكية.
 */
object ReadingColors {
    val SkyBackground = Color(0xFFECF8FF)
    val SkyTop = Color(0xFFB8E6FF)
    val SkyMid = Color(0xFFDDF3FF)
    val CardSurface = Color(0xFFFFFFFF)
    val NotebookPaper = Color(0xFFFFFEF7)

    val SunYellow = Color(0xFFFFC83D)
    val SunShadow = Color(0xFFD99400)

    val CoralOrange = Color(0xFFFF7A59)
    val CoralShadow = Color(0xFFD14B2B)

    val MeadowGreen = Color(0xFF22C55E)
    val MeadowShadow = Color(0xFF15803D)
    val HillLightGreen = Color(0xFF86EFAC)
    val HillDeepGreen = Color(0xFF4ADE80)

    val BerryPurple = Color(0xFF8B5CF6)
    val BerryShadow = Color(0xFF6233C7)

    val CandyPink = Color(0xFFEC4899)
    val CandyShadow = Color(0xFFBE185D)

    val SkyBlue = Color(0xFF38BDF8)
    val SkyBlueShadow = Color(0xFF0284C7)

    val InkDark = Color(0xFF1E293B)
    val InkSoft = Color(0xFF475569)
    val StarGold = Color(0xFFFFC83D)
    val StarEmpty = Color(0xFFCBD5E1)
}

/**
 * المستويات القرائية الثلاثة المتدرجة حسب الصف الدراسي للطالب في قاعدة البيانات.
 */
enum class ReadingLevel(
    val title: String,
    val subtitle: String,
    val shortGradesLabel: String,
    val fullGradesLabel: String,
    val accentColor: Color,
    val shadowColor: Color,
    val learnCardsHeading: String,
    val trainHeading: String,
    val sentenceHeading: String,
    val assemblePrompt: String,
    val writePromptPrefix: String
) {
    LEVEL_1(
        title = "المستوى الأول",
        subtitle = "التأسيس المقطعي",
        shortGradesLabel = "الأول والثاني",
        fullGradesLabel = "الصف الأول والثاني الابتدائي",
        accentColor = ReadingColors.CoralOrange,
        shadowColor = ReadingColors.CoralShadow,
        learnCardsHeading = "مقاطع الحرف — اضغط على كل مقطع لتسمعه",
        trainHeading = "قطار المقاطع",
        sentenceHeading = "جملة الدرس",
        assemblePrompt = "اضغط على المقاطع بالترتيب لتكوين الكلمة",
        writePromptPrefix = "ابدأ من النقطة الخضراء وتتبّع حرف"
    ),
    LEVEL_2(
        title = "المستوى الثاني",
        subtitle = "الطلاقة والتشكيل",
        shortGradesLabel = "الثالث والرابع",
        fullGradesLabel = "الصف الثالث والرابع الابتدائي",
        accentColor = ReadingColors.BerryPurple,
        shadowColor = ReadingColors.BerryShadow,
        learnCardsHeading = "أمثلة التشكيل والقاعدة — اضغط لتسمع",
        trainHeading = "قطار الكلمات والجمل",
        sentenceHeading = "فقرة القراءة المشكّلة",
        assemblePrompt = "اضغط على الكلمات بالترتيب لتكوين الجملة الصحيحة",
        writePromptPrefix = "ابدأ من النقطة الخضراء وتتبّع الكلمة"
    ),
    LEVEL_3(
        title = "المستوى الثالث",
        subtitle = "الإتقان اللغوي والإملاء",
        shortGradesLabel = "الخامس والسادس",
        fullGradesLabel = "الصف الخامس والسادس الابتدائي",
        accentColor = ReadingColors.MeadowGreen,
        shadowColor = ReadingColors.MeadowShadow,
        learnCardsHeading = "نماذج الإتقان اللغوي — اضغط لتسمع",
        trainHeading = "تركيب العبارات والجمل",
        sentenceHeading = "نص القراءة الجهرية والفهم",
        assemblePrompt = "رتّب التراكيب بدقة لبناء العبارة السليمة",
        writePromptPrefix = "تتبّع الكلمة بإملاء صحيح فوق الخط الرمادي"
    )
}

enum class LessonArtType {
    DADA,
    DAR,
    DOOR,
    BABA,
    MAMA,
    NOOR,
    BOOQ,
    WAZ,
    OPEN_BOOK_STARS,
    GLOWING_QUILL,
    TELESCOPE
}

data class ColoredSyllable(
    val text: String,
    val spokenText: String,
    val color: Color,
    val shadowColor: Color
)

data class SegmentedWord(
    val fullWord: String,
    val spokenWord: String,
    val syllables: List<ColoredSyllable>
)

/**
 * إحداثي نسبي (من 0 إلى 1) لنقطة بداية كتابة الحرف أو الكلمة العربية على السبورة.
 */
data class TraceStartPoint(
    val xFraction: Float,
    val yFraction: Float
)

data class ChallengeQuestion(
    val promptLabel: String,
    val spokenTarget: String,
    val correctOption: String,
    val options: List<String>
)

data class ReadingLesson(
    val id: String,
    val level: ReadingLevel,
    val number: Int,
    val title: String,
    val letter: String,
    val traceLetters: List<String>,
    val letterNameSpoken: String,
    val artType: LessonArtType,
    val islandColor: Color,
    val islandShadowColor: Color,
    val syllables: List<ColoredSyllable>,
    val words: List<SegmentedWord>,
    val sentence: String? = null,
    val sentenceSpoken: String? = null,
    val traceStartPoints: Map<String, TraceStartPoint> = emptyMap(),
    val challenges: List<ChallengeQuestion>
)

object ReadingCurriculum {

    private val palettePairs = listOf(
        ReadingColors.CoralOrange to ReadingColors.CoralShadow,
        ReadingColors.SkyBlue to ReadingColors.SkyBlueShadow,
        ReadingColors.MeadowGreen to ReadingColors.MeadowShadow,
        ReadingColors.BerryPurple to ReadingColors.BerryShadow,
        ReadingColors.CandyPink to ReadingColors.CandyShadow,
        ReadingColors.SunYellow to ReadingColors.SunShadow
    )

    private fun syl(text: String, spoken: String, colorIndex: Int): ColoredSyllable {
        val (bg, shadow) = palettePairs[colorIndex % palettePairs.size]
        return ColoredSyllable(
            text = text,
            spokenText = spoken,
            color = bg,
            shadowColor = shadow
        )
    }

    /**
     * يحدد المستوى القرائي الأنسب تلقائياً استناداً إلى صف الطالب المسجل في قاعدة بيانات Firebase.
     */
    fun detectLevelFromGrade(rawGrade: String?): ReadingLevel {
        val normalized = rawGrade.orEmpty()
            .trim()
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')

        if (normalized.isBlank()) return ReadingLevel.LEVEL_1

        // الصفوف المتوسطة أو الإعدادية تفتح تلقائياً على المستوى الثالث المتقدم
        if (normalized.contains("متوسط") || normalized.contains("اعدادي") || normalized.contains("ثانوي")) {
            return ReadingLevel.LEVEL_3
        }

        return when {
            normalized.contains("خامس") || normalized.contains("سادس") ||
                normalized.contains("5") || normalized.contains("6") ||
                normalized.contains("٥") || normalized.contains("٦") -> ReadingLevel.LEVEL_3

            normalized.contains("ثالث") || normalized.contains("رابع") ||
                normalized.contains("3") || normalized.contains("4") ||
                normalized.contains("٣") || normalized.contains("٤") -> ReadingLevel.LEVEL_2

            else -> ReadingLevel.LEVEL_1
        }
    }

    /**
     * يُرجع وصفاً عربياً أنيقاً لصف الطالب لعرضه في شارة أعلى الخريطة.
     */
    fun formatGradeDisplay(rawGrade: String?, fallbackLevel: ReadingLevel): String {
        val cleaned = rawGrade.orEmpty().trim()
        if (cleaned.isBlank()) return fallbackLevel.fullGradesLabel
        return if (cleaned.startsWith("الصف")) cleaned else "الصف $cleaned"
    }

    fun lessonsForLevel(level: ReadingLevel): List<ReadingLesson> = when (level) {
        ReadingLevel.LEVEL_1 -> level1Lessons
        ReadingLevel.LEVEL_2 -> level2Lessons
        ReadingLevel.LEVEL_3 -> level3Lessons
    }

    // =========================================================================
    // المستوى الأول — التأسيس المقطعي (الصف الأول والثاني الابتدائي — 8 دروس)
    // =========================================================================
    val level1Lessons: List<ReadingLesson> = listOf(
        ReadingLesson(
            id = "lesson_1_dada",
            level = ReadingLevel.LEVEL_1,
            number = 1,
            title = "دادا",
            letter = "د",
            traceLetters = listOf("د"),
            letterNameSpoken = "حرفُ الدّال، دَ",
            artType = LessonArtType.DADA,
            islandColor = ReadingColors.CoralOrange,
            islandShadowColor = ReadingColors.CoralShadow,
            syllables = listOf(
                syl("دا", "دا", 0),
                syl("دو", "دو", 1),
                syl("دي", "دي", 2),
                syl("دَ", "دَ", 3),
                syl("دُ", "دُ", 4),
                syl("دِ", "دِ", 5)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "دادا",
                    spokenWord = "دادا",
                    syllables = listOf(
                        syl("دا", "دا", 0),
                        syl("دا", "دا", 1)
                    )
                )
            ),
            sentence = "دادا دادا",
            sentenceSpoken = "دادا، دادا",
            traceStartPoints = mapOf("د" to TraceStartPoint(0.56f, 0.30f)),
            challenges = listOf(
                ChallengeQuestion("اضغط على الكلمة التي تسمعها", "دادا", "دادا", listOf("دادا", "دو", "دي")),
                ChallengeQuestion("أين المقطع الذي سمعته؟", "دا", "دا", listOf("دِ", "دا", "دو")),
                ChallengeQuestion("اختر المقطع الصحيح", "دي", "دي", listOf("دَ", "دا", "دي"))
            )
        ),
        ReadingLesson(
            id = "lesson_2_dar",
            level = ReadingLevel.LEVEL_1,
            number = 2,
            title = "دار",
            letter = "ر",
            traceLetters = listOf("ر"),
            letterNameSpoken = "حرفُ الرّاء، رَ",
            artType = LessonArtType.DAR,
            islandColor = ReadingColors.MeadowGreen,
            islandShadowColor = ReadingColors.MeadowShadow,
            syllables = listOf(
                syl("را", "را", 2),
                syl("رو", "رو", 1),
                syl("ري", "ري", 3),
                syl("رَ", "رَ", 0)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "دار",
                    spokenWord = "دار",
                    syllables = listOf(
                        syl("دا", "دا", 0),
                        syl("ر", "رْ", 2)
                    )
                )
            ),
            sentence = "دادا دار",
            sentenceSpoken = "دادا دار",
            traceStartPoints = mapOf("ر" to TraceStartPoint(0.58f, 0.34f)),
            challenges = listOf(
                ChallengeQuestion("اضغط على الكلمة التي تسمعها", "دار", "دار", listOf("دار", "دادا", "ري")),
                ChallengeQuestion("أين المقطع الذي سمعته؟", "را", "را", listOf("رو", "را", "دا")),
                ChallengeQuestion("اختر المقطع الصحيح", "رو", "رو", listOf("رَ", "ري", "رو"))
            )
        ),
        ReadingLesson(
            id = "lesson_3_door",
            level = ReadingLevel.LEVEL_1,
            number = 3,
            title = "دور",
            letter = "و",
            traceLetters = listOf("و"),
            letterNameSpoken = "حرفُ الواو، وَ",
            artType = LessonArtType.DOOR,
            islandColor = ReadingColors.SkyBlue,
            islandShadowColor = ReadingColors.SkyBlueShadow,
            syllables = listOf(
                syl("دو", "دو", 1),
                syl("رو", "رو", 3),
                syl("وَ", "وَ", 0)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "دور",
                    spokenWord = "دور",
                    syllables = listOf(
                        syl("دو", "دو", 1),
                        syl("ر", "رْ", 2)
                    )
                )
            ),
            sentence = "دار دور",
            sentenceSpoken = "دار، دور",
            traceStartPoints = mapOf("و" to TraceStartPoint(0.56f, 0.32f)),
            challenges = listOf(
                ChallengeQuestion("اضغط على الكلمة التي تسمعها", "دور", "دور", listOf("دار", "دور", "دادا")),
                ChallengeQuestion("أين المقطع الذي سمعته؟", "دو", "دو", listOf("دو", "را", "وَ")),
                ChallengeQuestion("اختر المقطع الصحيح", "رو", "رو", listOf("دا", "رو", "وَ"))
            )
        ),
        ReadingLesson(
            id = "lesson_4_baba",
            level = ReadingLevel.LEVEL_1,
            number = 4,
            title = "بابا",
            letter = "ب",
            traceLetters = listOf("ب"),
            letterNameSpoken = "حرفُ الباء، بَ",
            artType = LessonArtType.BABA,
            islandColor = ReadingColors.BerryPurple,
            islandShadowColor = ReadingColors.BerryShadow,
            syllables = listOf(
                syl("با", "با", 3),
                syl("بو", "بو", 1),
                syl("بي", "بي", 4),
                syl("بَ", "بَ", 0)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "بابا",
                    spokenWord = "بابا",
                    syllables = listOf(
                        syl("با", "با", 3),
                        syl("با", "با", 0)
                    )
                ),
                SegmentedWord(
                    fullWord = "باب",
                    spokenWord = "باب",
                    syllables = listOf(
                        syl("با", "با", 1),
                        syl("ب", "بْ", 2)
                    )
                )
            ),
            sentence = "دار بابا",
            sentenceSpoken = "دار بابا",
            traceStartPoints = mapOf("ب" to TraceStartPoint(0.74f, 0.42f)),
            challenges = listOf(
                ChallengeQuestion("اضغط على الكلمة التي تسمعها", "بابا", "بابا", listOf("بابا", "دار", "دور")),
                ChallengeQuestion("أين كلمة باب؟", "باب", "باب", listOf("دادا", "باب", "بي")),
                ChallengeQuestion("اختر المقطع الذي سمعته", "بو", "بو", listOf("با", "بَ", "بو"))
            )
        ),
        ReadingLesson(
            id = "lesson_5_mama",
            level = ReadingLevel.LEVEL_1,
            number = 5,
            title = "ماما",
            letter = "م",
            traceLetters = listOf("م"),
            letterNameSpoken = "حرفُ الميم، مَ",
            artType = LessonArtType.MAMA,
            islandColor = ReadingColors.CandyPink,
            islandShadowColor = ReadingColors.CandyShadow,
            syllables = listOf(
                syl("ما", "ما", 4),
                syl("مو", "مو", 3),
                syl("مي", "مي", 1),
                syl("مَ", "مَ", 2)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "ماما",
                    spokenWord = "ماما",
                    syllables = listOf(
                        syl("ما", "ما", 4),
                        syl("ما", "ما", 0)
                    )
                ),
                SegmentedWord(
                    fullWord = "نام",
                    spokenWord = "نام",
                    syllables = listOf(
                        syl("نا", "نا", 1),
                        syl("م", "مْ", 3)
                    )
                )
            ),
            sentence = "دادا نام",
            sentenceSpoken = "دادا نام",
            traceStartPoints = mapOf("م" to TraceStartPoint(0.58f, 0.36f)),
            challenges = listOf(
                ChallengeQuestion("اضغط على الكلمة التي تسمعها", "ماما", "ماما", listOf("بابا", "ماما", "نام")),
                ChallengeQuestion("أين الكلمة التي سمعتها؟", "نام", "نام", listOf("نام", "دار", "مي")),
                ChallengeQuestion("اختر المقطع الصحيح", "مو", "مو", listOf("ما", "مَ", "مو"))
            )
        ),
        ReadingLesson(
            id = "lesson_6_noor",
            level = ReadingLevel.LEVEL_1,
            number = 6,
            title = "نور ونوري",
            letter = "ن، ي",
            traceLetters = listOf("ن", "ي"),
            letterNameSpoken = "حرفُ النّون نَ، وحرفُ الياء يَ",
            artType = LessonArtType.NOOR,
            islandColor = ReadingColors.SunYellow,
            islandShadowColor = ReadingColors.SunShadow,
            syllables = listOf(
                syl("نا", "نا", 0),
                syl("نو", "نو", 1),
                syl("ني", "ني", 3),
                syl("ري", "ري", 2)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "نور",
                    spokenWord = "نور",
                    syllables = listOf(
                        syl("نو", "نو", 1),
                        syl("ر", "رْ", 2)
                    )
                ),
                SegmentedWord(
                    fullWord = "نوري",
                    spokenWord = "نوري",
                    syllables = listOf(
                        syl("نو", "نو", 3),
                        syl("ري", "ري", 4)
                    )
                ),
                SegmentedWord(
                    fullWord = "داري",
                    spokenWord = "داري",
                    syllables = listOf(
                        syl("دا", "دا", 0),
                        syl("ري", "ري", 2)
                    )
                )
            ),
            sentence = "دار نوري",
            sentenceSpoken = "دارُ نوري",
            traceStartPoints = mapOf(
                "ن" to TraceStartPoint(0.68f, 0.36f),
                "ي" to TraceStartPoint(0.62f, 0.34f)
            ),
            challenges = listOf(
                ChallengeQuestion("اضغط على الكلمة التي تسمعها", "نور", "نور", listOf("نور", "دار", "باب")),
                ChallengeQuestion("أين كلمة نوري؟", "نوري", "نوري", listOf("داري", "نوري", "ماما")),
                ChallengeQuestion("اختر الكلمة التي سمعتها", "داري", "داري", listOf("نور", "داري", "دور"))
            )
        ),
        ReadingLesson(
            id = "lesson_7_booq",
            level = ReadingLevel.LEVEL_1,
            number = 7,
            title = "بوق نديم",
            letter = "ق",
            traceLetters = listOf("ق"),
            letterNameSpoken = "حرفُ القاف، قَ",
            artType = LessonArtType.BOOQ,
            islandColor = ReadingColors.CoralOrange,
            islandShadowColor = ReadingColors.CoralShadow,
            syllables = listOf(
                syl("قا", "قا", 0),
                syl("قو", "قو", 1),
                syl("قي", "قي", 3),
                syl("قَ", "قَ", 2)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "بوق",
                    spokenWord = "بوق",
                    syllables = listOf(
                        syl("بو", "بو", 1),
                        syl("ق", "قْ", 0)
                    )
                ),
                SegmentedWord(
                    fullWord = "نديم",
                    spokenWord = "نديم",
                    syllables = listOf(
                        syl("نَـ", "نَ", 2),
                        syl("دي", "دي", 3),
                        syl("م", "مْ", 4)
                    )
                ),
                SegmentedWord(
                    fullWord = "بقر",
                    spokenWord = "بقر",
                    syllables = listOf(
                        syl("بَـ", "بَ", 0),
                        syl("قَـ", "قَ", 1),
                        syl("ر", "رْ", 2)
                    )
                )
            ),
            sentence = "بوق نديم",
            sentenceSpoken = "بوقُ نديم",
            traceStartPoints = mapOf("ق" to TraceStartPoint(0.62f, 0.34f)),
            challenges = listOf(
                ChallengeQuestion("اضغط على الكلمة التي تسمعها", "بوق", "بوق", listOf("بوق", "باب", "نور")),
                ChallengeQuestion("أين كلمة نديم؟", "نديم", "نديم", listOf("بقر", "نديم", "نوري")),
                ChallengeQuestion("اختر الكلمة التي سمعتها", "بقر", "بقر", listOf("بوق", "بقر", "دار"))
            )
        ),
        ReadingLesson(
            id = "lesson_8_waz",
            level = ReadingLevel.LEVEL_1,
            number = 8,
            title = "وز وز",
            letter = "ز",
            traceLetters = listOf("ز"),
            letterNameSpoken = "حرفُ الزّاي، زَ",
            artType = LessonArtType.WAZ,
            islandColor = ReadingColors.MeadowGreen,
            islandShadowColor = ReadingColors.MeadowShadow,
            syllables = listOf(
                syl("زا", "زا", 2),
                syl("زو", "زو", 1),
                syl("زي", "زي", 4),
                syl("زَ", "زَ", 0)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "وز",
                    spokenWord = "وز",
                    syllables = listOf(
                        syl("وَ", "وَ", 0),
                        syl("ز", "زْ", 2)
                    )
                ),
                SegmentedWord(
                    fullWord = "زيد",
                    spokenWord = "زيد",
                    syllables = listOf(
                        syl("زَ", "زَ", 1),
                        syl("يـ", "يْ", 3),
                        syl("د", "دْ", 0)
                    )
                ),
                SegmentedWord(
                    fullWord = "زورق",
                    spokenWord = "زورق",
                    syllables = listOf(
                        syl("زَ", "زَ", 2),
                        syl("و", "وْ", 1),
                        syl("رَ", "رَ", 4),
                        syl("ق", "قْ", 3)
                    )
                )
            ),
            sentence = "وز وز دار زيد",
            sentenceSpoken = "وَز وَز، دارُ زَيد",
            traceStartPoints = mapOf("ز" to TraceStartPoint(0.58f, 0.34f)),
            challenges = listOf(
                ChallengeQuestion("اضغط على الكلمة التي تسمعها", "وز", "وز", listOf("وز", "بوق", "دار")),
                ChallengeQuestion("أين كلمة زيد؟", "زيد", "زيد", listOf("زورق", "زيد", "نديم")),
                ChallengeQuestion("اختر الكلمة التي سمعتها", "زورق", "زورق", listOf("وز", "نوري", "زورق"))
            )
        )
    )

    // =========================================================================
    // المستوى الثاني — الطلاقة والتشكيل (الصف الثالث والرابع الابتدائي — 6 دروس)
    // =========================================================================
    val level2Lessons: List<ReadingLesson> = listOf(
        ReadingLesson(
            id = "l2_1_tanween",
            level = ReadingLevel.LEVEL_2,
            number = 1,
            title = "التنوين بأنواعه",
            letter = "ً ٌ ٍ",
            traceLetters = listOf("قَلَمٌ", "كِتاباً"),
            letterNameSpoken = "درس التنوين بأنواعه: تنوين الفتح، وتنوين الضم، وتنوين الكسر",
            artType = LessonArtType.OPEN_BOOK_STARS,
            islandColor = ReadingColors.BerryPurple,
            islandShadowColor = ReadingColors.BerryShadow,
            syllables = listOf(
                syl("قَلَماً", "قَلَماً", 0),
                syl("قَلَمٌ", "قَلَمٌ", 1),
                syl("قَلَمٍ", "قَلَمٍ", 2),
                syl("كِتاباً", "كِتاباً", 3),
                syl("شَمسٌ", "شَمسٌ", 4),
                syl("دَرسٍ", "دَرسٍ", 5)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "قَرَأَ التِّلميذُ كِتاباً",
                    spokenWord = "قَرَأَ التِّلميذُ كِتاباً",
                    syllables = listOf(
                        syl("قَرَأَ", "قَرَأَ", 0),
                        syl("التِّلميذُ", "التِّلميذُ", 1),
                        syl("كِتاباً", "كِتاباً", 3)
                    )
                ),
                SegmentedWord(
                    fullWord = "أَشرَقَت شَمسٌ جَميلَةٌ",
                    spokenWord = "أَشرَقَت شَمسٌ جَميلَةٌ",
                    syllables = listOf(
                        syl("أَشرَقَت", "أَشرَقَت", 2),
                        syl("شَمسٌ", "شَمسٌ", 5),
                        syl("جَميلَةٌ", "جَميلَةٌ", 4)
                    )
                )
            ),
            sentence = "قَرَأَ التِّلميذُ كِتاباً مُفيداً، وَكَتَبَ بِقَلَمٍ جَميلٍ في دَفتَرٍ نَظيفٍ.",
            sentenceSpoken = "قَرَأَ التِّلميذُ كِتاباً مُفيداً، وَكَتَبَ بِقَلَمٍ جَميلٍ في دَفتَرٍ نَظيفٍ.",
            challenges = listOf(
                ChallengeQuestion("اختر الكلمة المنوّنة بتنوين الفتح", "كِتاباً", "كِتاباً", listOf("كِتاباً", "قَلَمٌ", "دَرسٍ")),
                ChallengeQuestion("أين الكلمة التي فيها تنوين الضم؟", "شَمسٌ", "شَمسٌ", listOf("قَلَماً", "شَمسٌ", "قَلَمٍ")),
                ChallengeQuestion("اختر الكلمة المنوّنة بتنوين الكسر", "قَلَمٍ", "قَلَمٍ", listOf("كِتاباً", "شَمسٌ", "قَلَمٍ"))
            )
        ),
        ReadingLesson(
            id = "l2_2_shadda",
            level = ReadingLevel.LEVEL_2,
            number = 2,
            title = "الشدّة والتضعيف",
            letter = "ّ",
            traceLetters = listOf("مُعَلِّم", "أُمِّي"),
            letterNameSpoken = "درس الشدّة والتضعيف، الحرف المشدد يُنطق مرتين بوضوح",
            artType = LessonArtType.GLOWING_QUILL,
            islandColor = ReadingColors.CoralOrange,
            islandShadowColor = ReadingColors.CoralShadow,
            syllables = listOf(
                syl("مُعَلِّم", "مُعَلِّم", 0),
                syl("دَرَّسَ", "دَرَّسَ", 1),
                syl("أُمِّي", "أُمِّي", 4),
                syl("الصَّفّ", "الصَّفّ", 2),
                syl("نَظَّفَ", "نَظَّفَ", 3),
                syl("جَدِّي", "جَدِّي", 5)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "شَرَحَ المُعَلِّمُ الدَّرسَ",
                    spokenWord = "شَرَحَ المُعَلِّمُ الدَّرسَ",
                    syllables = listOf(
                        syl("شَرَحَ", "شَرَحَ", 0),
                        syl("المُعَلِّمُ", "المُعَلِّمُ", 3),
                        syl("الدَّرسَ", "الدَّرسَ", 1)
                    )
                ),
                SegmentedWord(
                    fullWord = "أُحِبُّ أُمِّي كَثيراً",
                    spokenWord = "أُحِبُّ أُمِّي كَثيراً",
                    syllables = listOf(
                        syl("أُحِبُّ", "أُحِبُّ", 4),
                        syl("أُمِّي", "أُمِّي", 0),
                        syl("كَثيراً", "كَثيراً", 2)
                    )
                )
            ),
            sentence = "شَرَحَ المُعَلِّمُ الدَّرسَ بِلُطفٍ، فَتَعَلَّمَ التَّلاميذُ نُطقَ الشَّدَّةِ بِإِتقانٍ.",
            sentenceSpoken = "شَرَحَ المُعَلِّمُ الدَّرسَ بِلُطفٍ، فَتَعَلَّمَ التَّلاميذُ نُطقَ الشَّدَّةِ بِإِتقانٍ.",
            challenges = listOf(
                ChallengeQuestion("اختر الكلمة التي تحتوي على شدّة", "مُعَلِّم", "مُعَلِّم", listOf("مُعَلِّم", "قَلَم", "باب")),
                ChallengeQuestion("أين الكلمة التي سمعتها؟", "أُمِّي", "أُمِّي", listOf("جَدِّي", "أُمِّي", "الصَّفّ")),
                ChallengeQuestion("اختر الفعل المشدّد الصحيح", "دَرَّسَ", "دَرَّسَ", listOf("دَرَّسَ", "كَتَبَ", "ذَهَبَ"))
            )
        ),
        ReadingLesson(
            id = "l2_3_shams_qamar",
            level = ReadingLevel.LEVEL_2,
            number = 3,
            title = "اللام الشمسية والقمرية",
            letter = "الـ",
            traceLetters = listOf("الشَّمس", "القَمَر"),
            letterNameSpoken = "درس اللام الشمسية واللام القمرية",
            artType = LessonArtType.NOOR,
            islandColor = ReadingColors.SunYellow,
            islandShadowColor = ReadingColors.SunShadow,
            syllables = listOf(
                syl("الشَّمسُ", "الشَّمسُ", 5),
                syl("القَمَرُ", "القَمَرُ", 1),
                syl("الصَّباحُ", "الصَّباحُ", 0),
                syl("الكِتابُ", "الكِتابُ", 3),
                syl("النُّورُ", "النُّورُ", 4),
                syl("المَدرَسَةُ", "المَدرَسَةُ", 2)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "تُشرِقُ الشَّمسُ صَباحاً",
                    spokenWord = "تُشرِقُ الشَّمسُ صَباحاً",
                    syllables = listOf(
                        syl("تُشرِقُ", "تُشرِقُ", 0),
                        syl("الشَّمسُ", "الشَّمسُ", 5),
                        syl("صَباحاً", "صَباحاً", 2)
                    )
                ),
                SegmentedWord(
                    fullWord = "يُنيرُ القَمَرُ السَّماءَ",
                    spokenWord = "يُنيرُ القَمَرُ السَّماءَ",
                    syllables = listOf(
                        syl("يُنيرُ", "يُنيرُ", 1),
                        syl("القَمَرُ", "القَمَرُ", 3),
                        syl("السَّماءَ", "السَّماءَ", 4)
                    )
                )
            ),
            sentence = "في الصَّباحِ تُشرِقُ الشَّمسُ عَلى المَدرَسَةِ، وَفي اللَّيلِ يُنيرُ القَمَرُ السَّماءَ.",
            sentenceSpoken = "في الصَّباحِ تُشرِقُ الشَّمسُ عَلى المَدرَسَةِ، وَفي اللَّيلِ يُنيرُ القَمَرُ السَّماءَ.",
            challenges = listOf(
                ChallengeQuestion("اختر كلمة تبدأ بلام شمسية", "الشَّمسُ", "الشَّمسُ", listOf("القَمَرُ", "الشَّمسُ", "الكِتابُ")),
                ChallengeQuestion("اختر كلمة تبدأ بلام قمرية", "القَمَرُ", "القَمَرُ", listOf("الشَّمسُ", "النُّورُ", "القَمَرُ")),
                ChallengeQuestion("أين كلمة المدرسة ذات اللام القمرية؟", "المَدرَسَةُ", "المَدرَسَةُ", listOf("الصَّباحُ", "المَدرَسَةُ", "النُّورُ"))
            )
        ),
        ReadingLesson(
            id = "l2_4_taa",
            level = ReadingLevel.LEVEL_2,
            number = 4,
            title = "التاء المربوطة والمفتوحة",
            letter = "ة / ت",
            traceLetters = listOf("مَدرَسَة", "بَيت"),
            letterNameSpoken = "درس التاء المربوطة والتاء المفتوحة في آخر الكلمة",
            artType = LessonArtType.BABA,
            islandColor = ReadingColors.SkyBlue,
            islandShadowColor = ReadingColors.SkyBlueShadow,
            syllables = listOf(
                syl("مَدرَسَة", "مَدرَسَة", 1),
                syl("حَديقَة", "حَديقَة", 2),
                syl("وَردَة", "وَردَة", 4),
                syl("بَيت", "بَيت", 0),
                syl("بِنت", "بِنت", 3),
                syl("وَقت", "وَقت", 5)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "حَديقَةُ المَدرَسَةِ جَميلَةٌ",
                    spokenWord = "حَديقَةُ المَدرَسَةِ جَميلَةٌ",
                    syllables = listOf(
                        syl("حَديقَةُ", "حَديقَةُ", 2),
                        syl("المَدرَسَةِ", "المَدرَسَةِ", 1),
                        syl("جَميلَةٌ", "جَميلَةٌ", 4)
                    )
                ),
                SegmentedWord(
                    fullWord = "فَتَحَت البِنتُ الكِتابَ",
                    spokenWord = "فَتَحَت البِنتُ الكِتابَ",
                    syllables = listOf(
                        syl("فَتَحَت", "فَتَحَت", 0),
                        syl("البِنتُ", "البِنتُ", 3),
                        syl("الكِتابَ", "الكِتابَ", 1)
                    )
                )
            ),
            sentence = "زَرَعَتِ البِنتُ وَردَةً في حَديقَةِ المَدرَسَةِ، وَسَقَتها في الوَقتِ المُناسِبِ.",
            sentenceSpoken = "زَرَعَتِ البِنتُ وَردَةً في حَديقَةِ المَدرَسَةِ، وَسَقَتها في الوَقتِ المُناسِبِ.",
            challenges = listOf(
                ChallengeQuestion("اختر الكلمة المنتهية بتاء مربوطة", "مَدرَسَة", "مَدرَسَة", listOf("بَيت", "مَدرَسَة", "وَقت")),
                ChallengeQuestion("اختر الكلمة المنتهية بتاء مفتوحة", "بَيت", "بَيت", listOf("حَديقَة", "وَردَة", "بَيت")),
                ChallengeQuestion("كيف تُكتب كلمة وردة بشكل صحيح؟", "وَردَة", "وَردَة", listOf("وَردَة", "وَردَت", "وَردا"))
            )
        ),
        ReadingLesson(
            id = "l2_5_sentences",
            level = ReadingLevel.LEVEL_2,
            number = 5,
            title = "بناء الجمل القصيرة",
            letter = "جملة",
            traceLetters = listOf("وَطَنٌ", "يَكتُبُ"),
            letterNameSpoken = "درس بناء الجمل القصيرة المفيدة باللغة العربية السليمة",
            artType = LessonArtType.DAR,
            islandColor = ReadingColors.MeadowGreen,
            islandShadowColor = ReadingColors.MeadowShadow,
            syllables = listOf(
                syl("يَكتُبُ", "يَكتُبُ", 1),
                syl("يَقرَأُ", "يَقرَأُ", 2),
                syl("التِّلميذُ", "التِّلميذُ", 3),
                syl("النَّشيطُ", "النَّشيطُ", 0),
                syl("العِراقُ", "العِراقُ", 5),
                syl("وَطَنٌ", "وَطَنٌ", 4)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "يَكتُبُ التِّلميذُ واجِبَهُ",
                    spokenWord = "يَكتُبُ التِّلميذُ واجِبَهُ",
                    syllables = listOf(
                        syl("يَكتُبُ", "يَكتُبُ", 1),
                        syl("التِّلميذُ", "التِّلميذُ", 3),
                        syl("واجِبَهُ", "واجِبَهُ", 2)
                    )
                ),
                SegmentedWord(
                    fullWord = "العِراقُ وَطَنٌ جَميلٌ",
                    spokenWord = "العِراقُ وَطَنٌ جَميلٌ",
                    syllables = listOf(
                        syl("العِراقُ", "العِراقُ", 2),
                        syl("وَطَنٌ", "وَطَنٌ", 0),
                        syl("جَميلٌ", "جَميلٌ", 5)
                    )
                )
            ),
            sentence = "يَذهَبُ التِّلميذُ النَّشيطُ إِلى مَدرَسَتِهِ كُلَّ صَباحٍ لِيَتَعَلَّمَ وَيَخدِمَ وَطَنَهُ العِراقَ.",
            sentenceSpoken = "يَذهَبُ التِّلميذُ النَّشيطُ إِلى مَدرَسَتِهِ كُلَّ صَباحٍ لِيَتَعَلَّمَ وَيَخدِمَ وَطَنَهُ العِراقَ.",
            challenges = listOf(
                ChallengeQuestion("أكمل الجملة: يَكتُبُ التِّلميذُ ...", "واجِبَهُ", "واجِبَهُ", listOf("واجِبَهُ", "البَحرَ", "النَّجمَ")),
                ChallengeQuestion("اختر الجملة التامة المفيدة", "العِراقُ وَطَنٌ جَميلٌ", "العِراقُ وَطَنٌ جَميلٌ", listOf("العِراقُ وَطَنٌ جَميلٌ", "في الصَّباحِ", "عَلى")),
                ChallengeQuestion("مَن يذهب إلى المدرسة كل صباح؟", "التِّلميذُ", "التِّلميذُ", listOf("التِّلميذُ", "القَمَرُ", "الزَّورَقُ"))
            )
        ),
        ReadingLesson(
            id = "l2_6_stories",
            level = ReadingLevel.LEVEL_2,
            number = 6,
            title = "فقرات قصصية مشكّلة",
            letter = "قصة",
            traceLetters = listOf("دِجلَة", "نَخلَة"),
            letterNameSpoken = "درس قراءة وفهم الفقرات القصصية المشكّلة",
            artType = LessonArtType.TELESCOPE,
            islandColor = ReadingColors.CandyPink,
            islandShadowColor = ReadingColors.CandyShadow,
            syllables = listOf(
                syl("دِجلَةُ", "دِجلَةُ", 1),
                syl("الفُراتُ", "الفُراتُ", 2),
                syl("النَّخلَةُ", "النَّخلَةُ", 5),
                syl("العُصفورُ", "العُصفورُ", 0),
                syl("البُستانُ", "البُستانُ", 3),
                syl("الخَيرُ", "الخَيرُ", 4)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "غَرَّدَ العُصفورُ فَوقَ النَّخلَةِ",
                    spokenWord = "غَرَّدَ العُصفورُ فَوقَ النَّخلَةِ",
                    syllables = listOf(
                        syl("غَرَّدَ", "غَرَّدَ", 0),
                        syl("العُصفورُ", "العُصفورُ", 1),
                        syl("فَوقَ", "فَوقَ", 3),
                        syl("النَّخلَةِ", "النَّخلَةِ", 2)
                    )
                ),
                SegmentedWord(
                    fullWord = "نَهرُ دِجلَةَ عَذبٌ",
                    spokenWord = "نَهرُ دِجلَةَ عَذبٌ",
                    syllables = listOf(
                        syl("نَهرُ", "نَهرُ", 1),
                        syl("دِجلَةَ", "دِجلَةَ", 2),
                        syl("عَذبٌ", "عَذبٌ", 5)
                    )
                )
            ),
            sentence = "عَلى ضِفافِ نَهرِ دِجلَةَ وَقَفَ عُصفورٌ صَغيرٌ فَوقَ نَخلَةٍ عالِيَةٍ، يُغَرِّدُ بِفَرَحٍ وَيَشكُرُ اللهَ عَلى نِعمَةِ الماءِ وَالخَيرِ.",
            sentenceSpoken = "عَلى ضِفافِ نَهرِ دِجلَةَ وَقَفَ عُصفورٌ صَغيرٌ فَوقَ نَخلَةٍ عالِيَةٍ، يُغَرِّدُ بِفَرَحٍ وَيَشكُرُ اللهَ عَلى نِعمَةِ الماءِ وَالخَيرِ.",
            challenges = listOf(
                ChallengeQuestion("أين وقف العصفور الصغير في القصة؟", "فَوقَ نَخلَةٍ", "فَوقَ نَخلَةٍ", listOf("فَوقَ نَخلَةٍ", "داخِلَ الصَّفِّ", "فَوقَ الجَبَلِ")),
                ChallengeQuestion("ما اسم النهر المذكور في القصة؟", "دِجلَةُ", "دِجلَةُ", listOf("دِجلَةُ", "البَحرُ", "السَّحابُ")),
                ChallengeQuestion("كيف كان العصفور يُغرّد؟", "بِفَرَحٍ", "بِفَرَحٍ", listOf("بِفَرَحٍ", "بِحُزنٍ", "بِخَوفٍ"))
            )
        )
    )

    // =========================================================================
    // المستوى الثالث — الإتقان اللغوي والإملاء (الصف الخامس والسادس الابتدائي — 6 دروس)
    // =========================================================================
    val level3Lessons: List<ReadingLesson> = listOf(
        ReadingLesson(
            id = "l3_1_mid_hamza",
            level = ReadingLevel.LEVEL_3,
            number = 1,
            title = "الهمزة المتوسطة",
            letter = "ئ ؤ أ",
            traceLetters = listOf("سَأَلَ", "فُؤاد"),
            letterNameSpoken = "درس الهمزة المتوسطة وكتابتها وفق أقوى الحركات",
            artType = LessonArtType.GLOWING_QUILL,
            islandColor = ReadingColors.MeadowGreen,
            islandShadowColor = ReadingColors.MeadowShadow,
            syllables = listOf(
                syl("بِئر", "بِئر", 1),
                syl("فُؤاد", "فُؤاد", 0),
                syl("سَأَلَ", "سَأَلَ", 2),
                syl("مُؤمِن", "مُؤمِن", 3),
                syl("قارِئَة", "قارِئَة", 4),
                syl("مَسأَلَة", "مَسأَلَة", 5)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "سَأَلَ الطّالِبُ أُستاذَهُ",
                    spokenWord = "سَأَلَ الطّالِبُ أُستاذَهُ",
                    syllables = listOf(
                        syl("سَأَلَ", "سَأَلَ", 2),
                        syl("الطّالِبُ", "الطّالِبُ", 1),
                        syl("أُستاذَهُ", "أُستاذَهُ", 3)
                    )
                ),
                SegmentedWord(
                    fullWord = "حَلَّ فُؤادٌ المَسأَلَةَ",
                    spokenWord = "حَلَّ فُؤادٌ المَسأَلَةَ",
                    syllables = listOf(
                        syl("حَلَّ", "حَلَّ", 0),
                        syl("فُؤادٌ", "فُؤادٌ", 1),
                        syl("المَسأَلَةَ", "المَسأَلَةَ", 4)
                    )
                )
            ),
            sentence = "سَأَلَ فُؤادٌ أُستاذَهُ عَن قاعِدَةِ الهَمزَةِ المُتَوَسِّطَةِ، فَأَجابَ: نَكتُبُها عَلى ما يُناسِبُ الحَرَكَةَ الأَقوى.",
            sentenceSpoken = "سَأَلَ فُؤادٌ أُستاذَهُ عَن قاعِدَةِ الهَمزَةِ المُتَوَسِّطَةِ، فَأَجابَ: نَكتُبُها عَلى ما يُناسِبُ الحَرَكَةَ الأَقوى.",
            challenges = listOf(
                ChallengeQuestion("اختر الرسم الإملائي الصحيح لكلمة بئر", "بِئر", "بِئر", listOf("بِئر", "بِأر", "بِؤر")),
                ChallengeQuestion("كيف تُكتب كلمة فؤاد بشكل سليم؟", "فُؤاد", "فُؤاد", listOf("فُئاد", "فُؤاد", "فُأاد")),
                ChallengeQuestion("اختر الكلمة الصحيحة إملائياً", "سَأَلَ", "سَأَلَ", listOf("سَئَلَ", "سَؤَلَ", "سَأَلَ"))
            )
        ),
        ReadingLesson(
            id = "l3_2_end_hamza",
            level = ReadingLevel.LEVEL_3,
            number = 2,
            title = "الهمزة المتطرفة",
            letter = "ء",
            traceLetters = listOf("سَماء", "ضَوء"),
            letterNameSpoken = "درس الهمزة المتطرفة في آخر الكلمة وتناسبها مع حركة الحرف السابق",
            artType = LessonArtType.NOOR,
            islandColor = ReadingColors.SunYellow,
            islandShadowColor = ReadingColors.SunShadow,
            syllables = listOf(
                syl("سَماء", "سَماء", 1),
                syl("شاطِئ", "شاطِئ", 2),
                syl("لُؤلُؤ", "لُؤلُؤ", 3),
                syl("يَقرَأ", "يَقرَأ", 0),
                syl("هُدوء", "هُدوء", 4),
                syl("ضَوء", "ضَوء", 5)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "السَّماءُ صافِيَةٌ اليَومَ",
                    spokenWord = "السَّماءُ صافِيَةٌ اليَومَ",
                    syllables = listOf(
                        syl("السَّماءُ", "السَّماءُ", 1),
                        syl("صافِيَةٌ", "صافِيَةٌ", 2),
                        syl("اليَومَ", "اليَومَ", 5)
                    )
                ),
                SegmentedWord(
                    fullWord = "يَقرَأُ الطّالِبُ بِهُدوءٍ",
                    spokenWord = "يَقرَأُ الطّالِبُ بِهُدوءٍ",
                    syllables = listOf(
                        syl("يَقرَأُ", "يَقرَأُ", 0),
                        syl("الطّالِبُ", "الطّالِبُ", 3),
                        syl("بِهُدوءٍ", "بِهُدوءٍ", 1)
                    )
                )
            ),
            sentence = "تَأَمَّلَ الطّالِبُ السَّماءَ الصّافِيَةَ عَلى شاطِئِ الفُراتِ، وَأَخَذَ يَقرَأُ بِهُدوءٍ تَحتَ ضَوءِ القَمَرِ.",
            sentenceSpoken = "تَأَمَّلَ الطّالِبُ السَّماءَ الصّافِيَةَ عَلى شاطِئِ الفُراتِ، وَأَخَذَ يَقرَأُ بِهُدوءٍ تَحتَ ضَوءِ القَمَرِ.",
            challenges = listOf(
                ChallengeQuestion("اختر الرسم الصحيح لكلمة شاطئ", "شاطِئ", "شاطِئ", listOf("شاطِئ", "شاطِء", "شاطِأ")),
                ChallengeQuestion("لماذا كُتبت الهمزة على السطر في ضَوء؟", "قَبلَها سُكون", "قَبلَها سُكون", listOf("قَبلَها سُكون", "قَبلَها كَسرَة", "قَبلَها ضَمَّة")),
                ChallengeQuestion("اختر الرسم الإملائي الصحيح", "يَقرَأ", "يَقرَأ", listOf("يَقرَء", "يَقرَأ", "يَقرَئ"))
            )
        ),
        ReadingLesson(
            id = "l3_3_punctuation",
            level = ReadingLevel.LEVEL_3,
            number = 3,
            title = "علامات الترقيم",
            letter = "؟ ! ،",
            traceLetters = listOf("سُؤال", "فاصِلَة"),
            letterNameSpoken = "درس علامات الترقيم لتنظيم القراءة الجهرية وتوضيح المعنى",
            artType = LessonArtType.OPEN_BOOK_STARS,
            islandColor = ReadingColors.SkyBlue,
            islandShadowColor = ReadingColors.SkyBlueShadow,
            syllables = listOf(
                syl("نُقطَة .", "النُّقطَة في نهاية الجملة", 1),
                syl("فاصِلَة ،", "الفاصِلَة بين الجمل المترابطة", 2),
                syl("استِفهام ؟", "عَلامَةُ الاِستِفهام لِلسُّؤال", 3),
                syl("تَعَجُّب !", "عَلامَةُ التَّعَجُّب لِلدهشَة", 0),
                syl("نُقطَتان :", "النُّقطَتان بَعدَ القَول", 4)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "ما أَجمَلَ وَطَنَنا العِراقَ!",
                    spokenWord = "ما أَجمَلَ وَطَنَنا العِراقَ",
                    syllables = listOf(
                        syl("ما أَجمَلَ", "ما أَجمَلَ", 0),
                        syl("وَطَنَنا", "وَطَنَنا", 2),
                        syl("العِراقَ!", "العِراقَ", 5)
                    )
                ),
                SegmentedWord(
                    fullWord = "هَل قَرَأتَ دَرسَكَ اليَومَ؟",
                    spokenWord = "هَل قَرَأتَ دَرسَكَ اليَومَ",
                    syllables = listOf(
                        syl("هَل قَرَأتَ", "هَل قَرَأتَ", 3),
                        syl("دَرسَكَ", "دَرسَكَ", 1),
                        syl("اليَومَ؟", "اليَومَ", 4)
                    )
                )
            ),
            sentence = "قالَ المُعَلِّمُ: هَل تَعلَمونَ أَهَمِّيَّةَ عَلاماتِ التَّرقيمِ؟ إِنَّها تُنَظِّمُ القِراءَةَ، وَما أَروَعَ الكَلامَ المُرتَّبَ!",
            sentenceSpoken = "قالَ المُعَلِّمُ: هَل تَعلَمونَ أَهَمِّيَّةَ عَلاماتِ التَّرقيمِ؟ إِنَّها تُنَظِّمُ القِراءَةَ، وَما أَروَعَ الكَلامَ المُرتَّبَ!",
            challenges = listOf(
                ChallengeQuestion("ما العلامة المناسبة في نهاية السؤال؟", "؟", "علامة الاستفهام", listOf("؟", ".", "!")),
                ChallengeQuestion("ما العلامة المناسبة بعد جملة ما أجمل الربيع؟", "!", "علامة التعجب", listOf("،", "؟", "!")),
                ChallengeQuestion("ما العلامة التي نضعها بعد قالَ المعلم؟", ":", "النقطتان الرأسيتان", listOf(":", "؟", "!"))
            )
        ),
        ReadingLesson(
            id = "l3_4_dad_za",
            level = ReadingLevel.LEVEL_3,
            number = 4,
            title = "الضاد والظاء",
            letter = "ض / ظ",
            traceLetters = listOf("ضَوء", "ظِلّ"),
            letterNameSpoken = "درس التمييز في النطق والإملاء بين حرف الضاد وحرف الظاء",
            artType = LessonArtType.BOOQ,
            islandColor = ReadingColors.CoralOrange,
            islandShadowColor = ReadingColors.CoralShadow,
            syllables = listOf(
                syl("ضَوء", "ضَوء", 5),
                syl("ظِلّ", "ظِلّ", 3),
                syl("حَضَرَ", "حَضَرَ", 2),
                syl("حَفِظَ", "حَفِظَ", 1),
                syl("أَرض", "أَرض", 0),
                syl("نَظيف", "نَظيف", 4)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "حَفِظَ الطّالِبُ القَصيدَةَ",
                    spokenWord = "حَفِظَ الطّالِبُ القَصيدَةَ",
                    syllables = listOf(
                        syl("حَفِظَ", "حَفِظَ", 1),
                        syl("الطّالِبُ", "الطّالِبُ", 3),
                        syl("القَصيدَةَ", "القَصيدَةَ", 2)
                    )
                ),
                SegmentedWord(
                    fullWord = "حَضَرَ الضَّيفُ في المَوعِدِ",
                    spokenWord = "حَضَرَ الضَّيفُ في المَوعِدِ",
                    syllables = listOf(
                        syl("حَضَرَ", "حَضَرَ", 0),
                        syl("الضَّيفُ", "الضَّيفُ", 2),
                        syl("في المَوعِدِ", "في المَوعِدِ", 1)
                    )
                )
            ),
            sentence = "جَلَسَ الطّالِبُ في ظِلِّ الشَّجَرَةِ يَحفَظُ دَرسَهُ، بَينَما أَضاءَ ضَوءُ الشَّمسِ أَرضَ المَدرَسَةِ النَّظيفَةَ.",
            sentenceSpoken = "جَلَسَ الطّالِبُ في ظِلِّ الشَّجَرَةِ يَحفَظُ دَرسَهُ، بَينَما أَضاءَ ضَوءُ الشَّمسِ أَرضَ المَدرَسَةِ النَّظيفَةَ.",
            challenges = listOf(
                ChallengeQuestion("اختر الكتابة الصحيحة لكلمة ظلّ الشجرة", "ظِلّ", "ظِلّ", listOf("ظِلّ", "ضِلّ", "ذِلّ")),
                ChallengeQuestion("كيف تُكتب كلمة ضوء الشمس؟", "ضَوء", "ضَوء", listOf("ظَوء", "ضَوء", "ذَوء")),
                ChallengeQuestion("اختر الكلمة الصحيحة بمعنى صان وتذكّر", "حَفِظَ", "حَفِظَ", listOf("حَفِضَ", "حَفِظَ", "حَضَرَ"))
            )
        ),
        ReadingLesson(
            id = "l3_5_science_iraq",
            level = ReadingLevel.LEVEL_3,
            number = 5,
            title = "نخيل العراق وثروته",
            letter = "علم",
            traceLetters = listOf("نَخيل", "تُمور"),
            letterNameSpoken = "فقرة علمية للقراءة الجهرية السريعة عن نخيل العراق وثروته الغذائية",
            artType = LessonArtType.TELESCOPE,
            islandColor = ReadingColors.BerryPurple,
            islandShadowColor = ReadingColors.BerryShadow,
            syllables = listOf(
                syl("النَّخيل", "النَّخيل", 2),
                syl("التُّمور", "التُّمور", 5),
                syl("البَصرَة", "البَصرَة", 1),
                syl("الطّاقَة", "الطّاقَة", 0),
                syl("المُناخ", "المُناخ", 3),
                syl("التُّربَة", "التُّربَة", 4)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "تَشتَهِرُ البَصرَةُ بِبَساتينِ النَّخيلِ",
                    spokenWord = "تَشتَهِرُ البَصرَةُ بِبَساتينِ النَّخيلِ",
                    syllables = listOf(
                        syl("تَشتَهِرُ", "تَشتَهِرُ", 1),
                        syl("البَصرَةُ", "البَصرَةُ", 2),
                        syl("بِبَساتينِ النَّخيلِ", "بِبَساتينِ النَّخيلِ", 5)
                    )
                ),
                SegmentedWord(
                    fullWord = "التَّمرُ غِذاءٌ صِحِّيٌّ مُفيدٌ",
                    spokenWord = "التَّمرُ غِذاءٌ صِحِّيٌّ مُفيدٌ",
                    syllables = listOf(
                        syl("التَّمرُ", "التَّمرُ", 0),
                        syl("غِذاءٌ", "غِذاءٌ", 2),
                        syl("صِحِّيٌّ", "صِحِّيٌّ", 3)
                    )
                )
            ),
            sentence = "يُعَدُّ النَّخيلُ مِن أَبرَزِ ثَرَواتِ العِراقِ الطَّبيعِيَّةِ، وَتَشتَهِرُ البَصرَةُ بِأَجودِ أَنواعِ التُّمورِ الغَنِيَّةِ بِالمَعادِنِ وَالطّاقَةِ، كَما تُساعِدُ أَشجارُ النَّخيلِ في تَلطيفِ المُناخِ.",
            sentenceSpoken = "يُعَدُّ النَّخيلُ مِن أَبرَزِ ثَرَواتِ العِراقِ الطَّبيعِيَّةِ، وَتَشتَهِرُ البَصرَةُ بِأَجودِ أَنواعِ التُّمورِ الغَنِيَّةِ بِالمَعادِنِ وَالطّاقَةِ، كَما تُساعِدُ أَشجارُ النَّخيلِ في تَلطيفِ المُناخِ.",
            challenges = listOf(
                ChallengeQuestion("بماذا تتميز التمور وفق الفقرة العلمية؟", "المَعادِن وَالطّاقَة", "بالمعادن والطاقة", listOf("المَعادِن وَالطّاقَة", "الأَخشاب", "الأَمطار")),
                ChallengeQuestion("ما دور أشجار النخيل البيئي المذكور في النص؟", "تَلطيفُ المُناخِ", "تلطيف المناخ", listOf("تَلطيفُ المُناخِ", "بِناءُ السُّفُنِ", "تَجميدُ الماءِ")),
                ChallengeQuestion("ما المدينة المذكورة ببساتين النخيل في النص؟", "البَصرَة", "البصرة", listOf("البَصرَة", "القَمَر", "المُحيط"))
            )
        ),
        ReadingLesson(
            id = "l3_6_literature_baghdad",
            level = ReadingLevel.LEVEL_3,
            number = 6,
            title = "بوابة الحضارة والعلم",
            letter = "أدب",
            traceLetters = listOf("بَغداد", "حَضارَة"),
            letterNameSpoken = "فقرة أدبية عراقية للقراءة المعبرة وفهم المقروء عن بغداد وحضارة الرافدين",
            artType = LessonArtType.WAZ,
            islandColor = ReadingColors.CandyPink,
            islandShadowColor = ReadingColors.CandyShadow,
            syllables = listOf(
                syl("بَغداد", "بَغداد", 3),
                syl("الرّافِدَين", "الرّافِدَين", 1),
                syl("الحَضارَة", "الحَضارَة", 5),
                syl("الحِكمَة", "الحِكمَة", 0),
                syl("العُلَماء", "العُلَماء", 2),
                syl("المَعرِفَة", "المَعرِفَة", 4)
            ),
            words = listOf(
                SegmentedWord(
                    fullWord = "بَغدادُ مَدينَةُ السَّلامِ",
                    spokenWord = "بَغدادُ مَدينَةُ السَّلامِ",
                    syllables = listOf(
                        syl("بَغدادُ", "بَغدادُ", 3),
                        syl("مَدينَةُ", "مَدينَةُ", 1),
                        syl("السَّلامِ", "السَّلامِ", 2)
                    )
                ),
                SegmentedWord(
                    fullWord = "العِلمُ نورٌ يَهدي العُقولَ",
                    spokenWord = "العِلمُ نورٌ يَهدي العُقولَ",
                    syllables = listOf(
                        syl("العِلمُ", "العِلمُ", 5),
                        syl("نورٌ", "نورٌ", 0),
                        syl("يَهدي العُقولَ", "يَهدي العُقولَ", 4)
                    )
                )
            ),
            sentence = "عَلى أَرضِ الرّافِدَينِ وُلِدَت أَولى حُروفِ الكِتابَةِ في التّاريخِ، وَفي بَغدادَ ازدهَرَ بَيتُ الحِكمَةِ الَّذي جَمَعَ العُلَماءَ فَأَنارَ العِراقُ طَريقَ المَعرِفَةِ لِلعالَمِ.",
            sentenceSpoken = "عَلى أَرضِ الرّافِدَينِ وُلِدَت أَولى حُروفِ الكِتابَةِ في التّاريخِ، وَفي بَغدادَ ازدهَرَ بَيتُ الحِكمَةِ الَّذي جَمَعَ العُلَماءَ فَأَنارَ العِراقُ طَريقَ المَعرِفَةِ لِلعالَمِ.",
            challenges = listOf(
                ChallengeQuestion("أين وُلدت أولى حروف الكتابة في التاريخ؟", "أَرضُ الرّافِدَينِ", "أرض الرافدين", listOf("أَرضُ الرّافِدَينِ", "القُطبُ الشَّمالي", "صَحراءُ المَرّيخ")),
                ChallengeQuestion("ما اسم الصرح العلمي الذي ازدهر في بغداد؟", "بَيتُ الحِكمَةِ", "بيت الحكمة", listOf("بَيتُ الحِكمَةِ", "سوقُ الزُّهورِ", "بُرجُ الرِّياحِ")),
                ChallengeQuestion("ماذا أنار العراق للعالم وفق الفقرة؟", "طَريقَ المَعرِفَةِ", "طريق المعرفة", listOf("طَريقَ المَعرِفَةِ", "أَرضَ الغاباتِ", "مَوجَ البِحارِ"))
            )
        )
    )

    // توافق خلفي مع أي استدعاء سابق لقائمة الدروس الافتراضية
    val lessons: List<ReadingLesson>
        get() = level1Lessons
}
