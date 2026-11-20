package com.example.reading

import androidx.compose.ui.graphics.Color

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

enum class LessonArtType {
    DADA,
    DAR,
    DOOR,
    BABA,
    MAMA,
    NOOR,
    BOOQ,
    WAZ
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
 * إحداثي نسبي (من 0 إلى 1) لنقطة بداية كتابة الحرف العربي على السبورة.
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
    val traceStartPoints: Map<String, TraceStartPoint>,
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

    val lessons: List<ReadingLesson> = listOf(
        // الدرس 1: دادا (حرف د)
        ReadingLesson(
            id = "lesson_1_dada",
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
            traceStartPoints = mapOf(
                "د" to TraceStartPoint(0.56f, 0.30f)
            ),
            challenges = listOf(
                ChallengeQuestion(
                    promptLabel = "اضغط على الكلمة التي تسمعها",
                    spokenTarget = "دادا",
                    correctOption = "دادا",
                    options = listOf("دادا", "دو", "دي")
                ),
                ChallengeQuestion(
                    promptLabel = "أين المقطع الذي سمعته؟",
                    spokenTarget = "دا",
                    correctOption = "دا",
                    options = listOf("دِ", "دا", "دو")
                ),
                ChallengeQuestion(
                    promptLabel = "اختر المقطع الصحيح",
                    spokenTarget = "دي",
                    correctOption = "دي",
                    options = listOf("دَ", "دا", "دي")
                )
            )
        ),

        // الدرس 2: دار (حرف ر)
        ReadingLesson(
            id = "lesson_2_dar",
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
            traceStartPoints = mapOf(
                "ر" to TraceStartPoint(0.58f, 0.34f)
            ),
            challenges = listOf(
                ChallengeQuestion(
                    promptLabel = "اضغط على الكلمة التي تسمعها",
                    spokenTarget = "دار",
                    correctOption = "دار",
                    options = listOf("دار", "دادا", "ري")
                ),
                ChallengeQuestion(
                    promptLabel = "أين المقطع الذي سمعته؟",
                    spokenTarget = "را",
                    correctOption = "را",
                    options = listOf("رو", "را", "دا")
                ),
                ChallengeQuestion(
                    promptLabel = "اختر المقطع الصحيح",
                    spokenTarget = "رو",
                    correctOption = "رو",
                    options = listOf("رَ", "ري", "رو")
                )
            )
        ),

        // الدرس 3: دور (حرف و)
        ReadingLesson(
            id = "lesson_3_door",
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
            traceStartPoints = mapOf(
                "و" to TraceStartPoint(0.56f, 0.32f)
            ),
            challenges = listOf(
                ChallengeQuestion(
                    promptLabel = "اضغط على الكلمة التي تسمعها",
                    spokenTarget = "دور",
                    correctOption = "دور",
                    options = listOf("دار", "دور", "دادا")
                ),
                ChallengeQuestion(
                    promptLabel = "أين المقطع الذي سمعته؟",
                    spokenTarget = "دو",
                    correctOption = "دو",
                    options = listOf("دو", "را", "وَ")
                ),
                ChallengeQuestion(
                    promptLabel = "اختر المقطع الصحيح",
                    spokenTarget = "رو",
                    correctOption = "رو",
                    options = listOf("دا", "رو", "وَ")
                )
            )
        ),

        // الدرس 4: بابا (حرف ب)
        ReadingLesson(
            id = "lesson_4_baba",
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
            traceStartPoints = mapOf(
                "ب" to TraceStartPoint(0.74f, 0.42f)
            ),
            challenges = listOf(
                ChallengeQuestion(
                    promptLabel = "اضغط على الكلمة التي تسمعها",
                    spokenTarget = "بابا",
                    correctOption = "بابا",
                    options = listOf("بابا", "دار", "دور")
                ),
                ChallengeQuestion(
                    promptLabel = "أين كلمة باب؟",
                    spokenTarget = "باب",
                    correctOption = "باب",
                    options = listOf("دادا", "باب", "بي")
                ),
                ChallengeQuestion(
                    promptLabel = "اختر المقطع الذي سمعته",
                    spokenTarget = "بو",
                    correctOption = "بو",
                    options = listOf("با", "بَ", "بو")
                )
            )
        ),

        // الدرس 5: ماما (حرف م)
        ReadingLesson(
            id = "lesson_5_mama",
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
            traceStartPoints = mapOf(
                "م" to TraceStartPoint(0.58f, 0.36f)
            ),
            challenges = listOf(
                ChallengeQuestion(
                    promptLabel = "اضغط على الكلمة التي تسمعها",
                    spokenTarget = "ماما",
                    correctOption = "ماما",
                    options = listOf("بابا", "ماما", "نام")
                ),
                ChallengeQuestion(
                    promptLabel = "أين الكلمة التي سمعتها؟",
                    spokenTarget = "نام",
                    correctOption = "نام",
                    options = listOf("نام", "دار", "مي")
                ),
                ChallengeQuestion(
                    promptLabel = "اختر المقطع الصحيح",
                    spokenTarget = "مو",
                    correctOption = "مو",
                    options = listOf("ما", "مَ", "مو")
                )
            )
        ),

        // الدرس 6: نور ونوري (حرفا ن، ي)
        ReadingLesson(
            id = "lesson_6_noor",
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
                ChallengeQuestion(
                    promptLabel = "اضغط على الكلمة التي تسمعها",
                    spokenTarget = "نور",
                    correctOption = "نور",
                    options = listOf("نور", "دار", "باب")
                ),
                ChallengeQuestion(
                    promptLabel = "أين كلمة نوري؟",
                    spokenTarget = "نوري",
                    correctOption = "نوري",
                    options = listOf("داري", "نوري", "ماما")
                ),
                ChallengeQuestion(
                    promptLabel = "اختر الكلمة التي سمعتها",
                    spokenTarget = "داري",
                    correctOption = "داري",
                    options = listOf("نور", "داري", "دور")
                )
            )
        ),

        // الدرس 7: بوق نديم (حرف ق)
        ReadingLesson(
            id = "lesson_7_booq",
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
            traceStartPoints = mapOf(
                "ق" to TraceStartPoint(0.62f, 0.34f)
            ),
            challenges = listOf(
                ChallengeQuestion(
                    promptLabel = "اضغط على الكلمة التي تسمعها",
                    spokenTarget = "بوق",
                    correctOption = "بوق",
                    options = listOf("بوق", "باب", "نور")
                ),
                ChallengeQuestion(
                    promptLabel = "أين كلمة نديم؟",
                    spokenTarget = "نديم",
                    correctOption = "نديم",
                    options = listOf("بقر", "نديم", "نوري")
                ),
                ChallengeQuestion(
                    promptLabel = "اختر الكلمة التي سمعتها",
                    spokenTarget = "بقر",
                    correctOption = "بقر",
                    options = listOf("بوق", "بقر", "دار")
                )
            )
        ),

        // الدرس 8: وز وز (حرف ز)
        ReadingLesson(
            id = "lesson_8_waz",
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
            traceStartPoints = mapOf(
                "ز" to TraceStartPoint(0.58f, 0.34f)
            ),
            challenges = listOf(
                ChallengeQuestion(
                    promptLabel = "اضغط على الكلمة التي تسمعها",
                    spokenTarget = "وز",
                    correctOption = "وز",
                    options = listOf("وز", "بوق", "دار")
                ),
                ChallengeQuestion(
                    promptLabel = "أين كلمة زيد؟",
                    spokenTarget = "زيد",
                    correctOption = "زيد",
                    options = listOf("زورق", "زيد", "نديم")
                ),
                ChallengeQuestion(
                    promptLabel = "اختر الكلمة التي سمعتها",
                    spokenTarget = "زورق",
                    correctOption = "زورق",
                    options = listOf("وز", "نوري", "زورق")
                )
            )
        )
    )
}
