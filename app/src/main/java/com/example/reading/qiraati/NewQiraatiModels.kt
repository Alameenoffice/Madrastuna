package com.example.reading.qiraati

import androidx.compose.ui.graphics.Color

/**
 * ثيم لوني طفولي مبهج خاص بكل وحدة في كتاب "قراءتي الجديدة — الصف الأول الابتدائي".
 */
data class QiraatiUnitTheme(
    val primaryColor: Color,
    val shadowColor: Color,
    val softSurfaceColor: Color,
    val badgeColor: Color,
    val accentHighlightRed: Color = Color(0xFFE11D48),
    val gradientStart: Color = primaryColor,
    val gradientEnd: Color = shadowColor
)

/**
 * مفاتيح المشاهد المصوّرة لقسم الاستماع والمحادثة (أنا أتحدث) في كل وحدة.
 */
enum class QiraatiStoryArtKey {
    USRATY_FAMILY_HOME,
    USRATY_KITCHEN_HELP,
    USRATY_FAMILY_READING,
    USRATY_HELPING_GRANDMA,
    UPCOMING_SCHOOL,
    UPCOMING_CITY_VILLAGE,
    UPCOMING_HEALTH,
    UPCOMING_NATURE,
    UPCOMING_IRAQ
}

/**
 * مفاتيح الرسوم التوضيحية المصوّرة لكلمات الأنشطة التعليمية.
 */
enum class QiraatiWordArtKey {
    // كلمات حرف الباء
    COW,
    ORANGE,
    CARPET,
    MILK,
    BREAD,
    DOOR,
    BUGLE,
    SPRING,
    ONION,
    WATERMELON,
    DRUM,
    CHEESE,
    LION_CUB,

    // كلمات حرف الميم
    MAMA,
    BANANA,
    SCISSORS,
    PEN,
    SUN,
    WATER,
    OWL,
    POMEGRANATE,
    RAIN,
    TEACHER,
    KEY,
    SCALE,
    SAND,
    ANT,

    // كلمات حرف الدال
    DALLAH,
    BEAR,
    DIRHAM,
    LION,
    POT,
    HOUSE,
    WORM,
    ROOSTER,
    DOLL,
    STAIRS,
    DATE_SYRUP,
    HOUSES,
    FULL_MOON,
    HOOPOE,

    // كلمات حرف الفاء
    CHEETAH,
    OVEN,
    CUP,
    LANTERN,
    SPARROW,
    ELEPHANT,
    MOUTH,
    TAMBOURINES,
    FROG,
    PARTY,
    CHILD,

    // مفتاح عام قابل لإعادة الاستخدام في الوحدات القادمة
    GENERIC_WORD
}

/**
 * موضع الحرف المستهدف داخل الكلمة في نشاط 1.
 */
enum class LetterPosition(val arabicLabel: String) {
    INITIAL("أول الكلمة"),
    MEDIAL("وسط الكلمة"),
    FINAL_CONNECTED("آخر الكلمة متصل"),
    FINAL_ISOLATED("آخر الكلمة منفصل"),
    MULTIPLE("متكرر في الكلمة")
}

/**
 * نطاق فهرسة دقيق (بداية شاملة ونهاية غير شاملة) لتلوين الحرف المستهدف مع حركته باللون الأحمر.
 */
data class QiraatiHighlightRange(
    val startInclusive: Int,
    val endExclusive: Int
)

/**
 * مشهد مصوّر ضمن درس الاستماع والمحادثة (أنا أتحدث).
 */
data class QiraatiStoryScene(
    val id: String,
    val sceneNumber: Int,
    val title: String,
    val audioDescription: String,
    val narrationText: String,
    val artKey: QiraatiStoryArtKey,
    val isListeningMainScene: Boolean = false
)

/**
 * قسم الاستماع والمحادثة (أنا أتحدث) في بداية كل وحدة من كتاب قراءتي الجديدة.
 */
data class UnitStorySection(
    val id: String,
    val headerLabel: String,
    val title: String,
    val listeningScene: QiraatiStoryScene,
    val conversationScenes: List<QiraatiStoryScene>,
    val interactiveStoryLines: List<String>,
    val iSpeakQuestions: List<String>
) {
    /** جميع المشاهد مرتّبة بدءاً من مشهد الاستماع الرئيسي ثم مشاهد المحادثة. */
    val allScenes: List<QiraatiStoryScene>
        get() = listOf(listeningScene) + conversationScenes
}

/**
 * نشاط 1: كلمة مصوّرة مع تحديد مواضع الحرف المستهدف لتلوينه باللون الأحمر أثناء العرض.
 */
data class QiraatiIntroWord(
    val word: String,
    val targetChar: Char,
    val position: LetterPosition,
    val artKey: QiraatiWordArtKey,
    val highlightRanges: List<QiraatiHighlightRange> = findArabicLetterRanges(word, targetChar),
    val spokenText: String = word
)

/**
 * نشاط 2: زوج المد القصير مقابل المد الطويل (فتحة/ألف، ضمة/واو، كسرة/ياء).
 */
data class QiraatiVowelPair(
    val shortSyllable: String,
    val longSyllable: String,
    val shortVowelLabel: String,
    val longVowelLabel: String,
    val spokenShort: String = shortSyllable,
    val spokenLong: String = longSyllable
)

/**
 * نشاط 3: كلمة تطبيقية على المد القصير أو المد الطويل.
 */
data class QiraatiVowelWord(
    val word: String,
    val isLongVowel: Boolean,
    val vowelLabel: String,
    val highlightedSegment: String,
    val artKey: QiraatiWordArtKey,
    val highlightRanges: List<QiraatiHighlightRange> = findSegmentRanges(word, highlightedSegment),
    val spokenText: String = word
)

/**
 * نشاط 4: كلمة تحتوي على مقطع ساكن مع عزل المقطع الساكن لإبرازه باللون الأحمر (مثل: طَبْلُ -> طَبْ).
 */
data class QiraatiSukoonItem(
    val word: String,
    val sukoonSyllable: String,
    val remainingPart: String,
    val artKey: QiraatiWordArtKey,
    val highlightRanges: List<QiraatiHighlightRange> = findSegmentRanges(word, sukoonSyllable),
    val spokenText: String = word
)

/**
 * نشاط 5: كلمة للتحليل الصوتي مع مقاطعها الصحيحة للعبة دوائر التحليل.
 */
data class QiraatiAnalysisItem(
    val word: String,
    val syllables: List<String>,
    val spokenText: String = word
)

/**
 * فرع واحد ضمن لعبة قطع البازل المتشابكة (القطعة المتفرعة + الكلمة الناتجة).
 */
data class QiraatiPuzzleBranch(
    val attachedPiece: String,
    val resultWord: String,
    val spokenText: String = resultWord
)

/**
 * نشاط 6: لعبة قطع البازل المتشابكة (القطعة الأساسية + القطع المتفرعة + الكلمات الناتجة).
 */
data class QiraatiPuzzleItem(
    val basePiece: String,
    val branches: List<QiraatiPuzzleBranch>,
    val promptHint: String = "اربط القطعة الأساسية مع القطعة المناسبة لتكوين الكلمة"
)

/**
 * درس حرف متكامل وفق تسلسل أنشطة كتاب "قراءتي الجديدة — الصف الأول الابتدائي" العراقي الرسمي.
 */
data class QiraatiLetterLesson(
    val id: String,
    val unitNumber: Int,
    val lessonOrder: Int,
    val letterName: String,
    val targetLetter: String,
    val cloudForms: List<String>,
    val accentColor: Color,
    val shadowColor: Color,
    val introWords: List<QiraatiIntroWord>,
    val vowelPairs: List<QiraatiVowelPair>,
    val vowelWords: List<QiraatiVowelWord>,
    val sukoonItems: List<QiraatiSukoonItem>,
    val analysisItems: List<QiraatiAnalysisItem>,
    val puzzleItems: List<QiraatiPuzzleItem>,
    val readAloudWords: List<String>,
    val tracingLetter: String,
    val tracingForms: List<String> = cloudForms
)

/**
 * نموذج الوحدة الدراسية القابل للتوسّع في كتاب "قراءتي الجديدة — الصف الأول الابتدائي".
 * يمكن إضافة أي وحدة جديدة (الثانية، الثالثة...) بملف بيانات واحد ينتج كائن QiraatiUnit.
 */
data class QiraatiUnit(
    val id: String,
    val unitNumber: Int,
    val unitBadge: String,
    val title: String,
    val shortTitle: String,
    val description: String,
    val themeColor: Color,
    val theme: QiraatiUnitTheme,
    val storySection: UnitStorySection?,
    val letterLessons: List<QiraatiLetterLesson>,
    val isAvailable: Boolean = true,
    val comingSoonLabel: String? = null,
    val plannedLetters: List<String> = letterLessons.map { it.targetLetter },
    val coverArtKey: QiraatiStoryArtKey = storySection?.listeningScene?.artKey
        ?: QiraatiStoryArtKey.USRATY_FAMILY_HOME
)

/**
 * دالة مساعدة تتحقق إن كان المحرف من علامات التشكيل العربية (فتحة، ضمة، كسرة، سكون، شدة، تنوين...).
 */
fun Char.isArabicDiacritic(): Boolean {
    val code = this.code
    return code in 0x064B..0x065F || code == 0x0670
}

/**
 * تحديد جميع مواضع ظهور الحرف المستهدف داخل الكلمة مع ضمّ حركات التشكيل التابعة له مباشرة
 * لضمان تلوين الحرف وحركته باللون الأحمر بدقة تامة.
 */
fun findArabicLetterRanges(word: String, targetChar: Char): List<QiraatiHighlightRange> {
    val ranges = mutableListOf<QiraatiHighlightRange>()
    var index = 0
    while (index < word.length) {
        if (word[index] == targetChar) {
            var end = index + 1
            while (end < word.length && word[end].isArabicDiacritic()) {
                end++
            }
            ranges.add(QiraatiHighlightRange(startInclusive = index, endExclusive = end))
            index = end
        } else {
            index++
        }
    }
    return ranges
}

/**
 * تحديد موضع مقطع محدد (مثل المقطع الساكن أو مقطع المد) داخل الكلمة بدقة مع مراعاة التشكيل.
 */
fun findSegmentRanges(word: String, segment: String): List<QiraatiHighlightRange> {
    if (segment.isEmpty()) return emptyList()
    val exactIndex = word.indexOf(segment)
    if (exactIndex >= 0) {
        var end = exactIndex + segment.length
        while (end < word.length && word[end].isArabicDiacritic()) {
            end++
        }
        return listOf(QiraatiHighlightRange(startInclusive = exactIndex, endExclusive = end))
    }
    // مطابقة احتياطية بتجاهل اختلاف التشكيل إن وُجد مع الحفاظ على فهارس الكلمة الأصلية
    val cleanSegment = segment.filterNot { it.isArabicDiacritic() }
    if (cleanSegment.isEmpty()) return emptyList()

    val baseCharPositions = mutableListOf<Int>()
    val cleanWordBuilder = StringBuilder()
    word.forEachIndexed { idx, ch ->
        if (!ch.isArabicDiacritic()) {
            baseCharPositions.add(idx)
            cleanWordBuilder.append(ch)
        }
    }
    val matchPos = cleanWordBuilder.indexOf(cleanSegment)
    if (matchPos < 0) return emptyList()

    val startIdx = baseCharPositions[matchPos]
    val lastBaseIdx = baseCharPositions[matchPos + cleanSegment.length - 1]
    var endIdx = lastBaseIdx + 1
    while (endIdx < word.length && word[endIdx].isArabicDiacritic()) {
        endIdx++
    }
    return listOf(QiraatiHighlightRange(startInclusive = startIdx, endExclusive = endIdx))
}
