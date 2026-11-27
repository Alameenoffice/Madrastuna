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
    MADRASATI_SCHOOL_FLAG,
    MADRASATI_GATE_ARRIVAL,
    MADRASATI_YARD_LINEUP,
    MADRASATI_CLASSROOM_LESSON,
    WATANI_CLASSROOM_MAP_FLAG,
    WATANI_NATIONAL_DAY_PARTY,
    WATANI_THREE_IRAQI_PALMS,
    WATANI_IRAQ_MAP_AND_FLAG,
    SIHATI_HYGIENE_TOOLS,
    SIHATI_HAND_WASHING,
    SIHATI_HEALTHY_BREAKFAST,
    SIHATI_PARK_PLAY_KITE_BIKE,
    UPCOMING_SCHOOL,
    UPCOMING_CITY_VILLAGE,
    UPCOMING_HEALTH,
    UPCOMING_NATURE,
    UPCOMING_IRAQ
}

/**
 * شخصية المرافق التفاعلي في فقرة "أقرأ" وترويسة الوحدة.
 */
enum class QiraatiMascotStyle(val key: String) {
    BLUE_BIRD("bird"),
    BLUE_PEN("pen"),
    BEE("bee")
}

/**
 * مفاتيح الرسوم التوضيحية المصوّرة لكلمات الأنشطة التعليمية.
 */
enum class QiraatiWordArtKey {
    // كلمات الوحدة الأولى (ب، م، د، ف)
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

    // كلمات الوحدة الثانية (ر، س، ن، ت، ة، تنوين الضم)
    FOOT,
    CARROT,
    MONKEY,
    SHARPENER,
    SHEEP,
    BED,
    WHISTLE,
    BOAT,
    RICE,
    FEATHER,
    BRUSH,
    POND,
    BASKET,
    TURTLE,
    CURTAIN,
    NAIL,
    CLOCK,
    LAPTOP,
    CROCODILE,
    BLACKBOARD,
    SUGAR,
    SQUIRREL,
    MARKET,
    RULER,
    DRESS,
    EAGLE,
    DOTS,
    STARS,
    HORSE,
    FIRE,
    SOLDIERS,
    TIGERS,
    GIRL,
    HAZELNUT,
    SICKLE,
    DATES,
    APPLE,
    SPIDER,
    JACKET,
    CROWN,
    BERRY,
    FIG,
    STAMP,
    MUSEUM,
    SEEDLINGS,
    ERASER,
    BACKPACK,
    SCHOOL,
    SCHOOLGIRL,
    BOOK,
    MEAT,
    BOARD,
    KNIGHT,
    FISH,
    ROSE,
    SHIP,
    PAINTING,

    // كلمات الوحدة الثالثة (و، ي، ق، ل، ط، ع، تنوين الفتح)
    FACES,
    PILLOW,
    BUCKET,
    WAVE,
    FRUITS,
    PEACOCK,
    COLORING,
    MEDICINE,
    BOW,
    PEACH,
    PUMPKIN,
    PRAYING,
    HAND,
    FLUTE,
    LEMON,
    CUCUMBER,
    BIRDS,
    MILLIONS,
    WATER_SPRING,
    CLAPPING,
    RESORTS,
    JASMINE,
    EGGS,
    CARDAMOM,
    OIL,
    HAT,
    SHARK,
    PENGUIN,
    FALCON,
    COINS,
    FLOUR,
    COTTON,
    STORY,
    SCORPION,
    FIELD,
    TOY,
    TONGUE,
    GAZELLE,
    DOG,
    PLAYER,
    HEARTS,
    SCIENTIST,
    THREE,
    BALLOON,
    PLAYGROUND,
    PEPPER,
    DOCTOR,
    TAILOR,
    CAPTAIN,
    PERFUMES,
    TRAIN,
    RECTANGLE,
    HAMMER,
    KITCHEN,
    IRAQ_FLAG,
    BONES,
    FINGER,
    TORCH,
    OUD,
    STICKS,
    GRAPES,
    OSTRICH,
    FOX,
    MINT,
    LEAF,

    // كلمات الوحدة الرابعة (ح، ش، ز — صحتي وبيئتي)
    DOVE,
    GRAINS,
    SHOE,
    WALL,
    WHALE,
    SWEETS,
    BELT,
    WALLET,
    COAL,
    THERMOMETER,
    CANDLE,
    WINDOW,
    SAIL,
    APRICOT,
    TREES,
    STREET,
    PEELS,
    SAWS,
    CHALK,
    GRASS,
    COMB,
    WATER_BOTTLE,
    BUTTON,
    GOAT,
    SNAIL,
    ISLAND,
    OLIVES,
    DECORATION,
    FLOWERS,
    VASE,
    TOOTHBRUSH,

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
    val allScenes: List<QiraatiStoryScene>
        get() = listOf(listeningScene) + conversationScenes
}

/**
 * نشيد الوحدة الختامي (مثل: نشيد مَدْرَسَتِي في الوحدة الثانية).
 */
data class QiraatiUnitChant(
    val id: String,
    val title: String,
    val verses: List<String>,
    val subtitle: String = "استمع للنشيد وردّده بصوت جميل مع أصدقائك"
)

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
 * نشاط 2: زوج المد القصير مقابل المد الطويل (أو زوج حركات التاء المربوطة داخل التفاحات).
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
 * عنصر لعبة "أضع دائرة حول الكلمات التي تحوي المهارة المستهدفة (ة ، ـة)".
 */
data class QiraatiCircleFilterWord(
    val word: String,
    val containsTarget: Boolean,
    val artKey: QiraatiWordArtKey = QiraatiWordArtKey.GENERIC_WORD,
    val spokenText: String = word
)

/**
 * عنصر لعبة اختيار الشكل المناسب (ة أم ـة) لإكمال الكلمة ونطقها.
 */
data class QiraatiSuffixChoiceItem(
    val wordStem: String,
    val options: List<String> = listOf("ة", "ـة"),
    val correctOption: String,
    val completedWord: String,
    val artKey: QiraatiWordArtKey = QiraatiWordArtKey.GENERIC_WORD,
    val spokenText: String = completedWord
)

/**
 * فرع واحد ضمن لعبة قطع البازل المتشابكة:
 * يدعم بسلاسة دمج قطعتين (basePiece + attachedPiece) أو 3 قطع مقطعية (pieces).
 */
data class QiraatiPuzzleBranch(
    val attachedPiece: String,
    val resultWord: String,
    val spokenText: String = resultWord,
    val pieces: List<String> = emptyList()
) {
    fun resolvedPieces(basePiece: String): List<String> =
        if (pieces.size >= 2) pieces else listOf(basePiece, attachedPiece)
}

/**
 * نشاط 6: لعبة قطع البازل المتشابكة (يدعم 2 أو 3 قطع مقطعية مع الكلمة الناتجة).
 */
data class QiraatiPuzzleItem(
    val basePiece: String,
    val branches: List<QiraatiPuzzleBranch>,
    val promptHint: String = "اربط قطع البازل معاً لتكوين الكلمة"
)

/**
 * نشاط تركيب الكلمة من المقاطع ثم إكمال الجملة ذات الفراغ (مثل نشاط تنوين الفتح في الوحدة الثالثة).
 */
data class QiraatiSentenceFillItem(
    val syllables: List<String>,
    val targetWord: String,
    val sentenceWithBlank: String,
    val completedSentence: String,
    val artKey: QiraatiWordArtKey = QiraatiWordArtKey.GENERIC_WORD,
    val spokenWord: String = targetWord,
    val spokenSentence: String = completedSentence
)

/**
 * درس حرف أو مهارة متكامل وفق تسلسل أنشطة كتاب "قراءتي الجديدة — الصف الأول الابتدائي" العراقي الرسمي.
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
    val tracingForms: List<String> = cloudForms,
    val circleFilterWords: List<QiraatiCircleFilterWord> = emptyList(),
    val suffixChoiceItems: List<QiraatiSuffixChoiceItem> = emptyList(),
    val isAppleVowelsStyle: Boolean = false,
    val tanweenListenWords: List<QiraatiIntroWord> = emptyList(),
    val sentenceFillItems: List<QiraatiSentenceFillItem> = emptyList()
)

/**
 * نموذج الوحدة الدراسية القابل للتوسّع في كتاب "قراءتي الجديدة — الصف الأول الابتدائي".
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
        ?: QiraatiStoryArtKey.USRATY_FAMILY_HOME,
    val unitChant: QiraatiUnitChant? = null,
    val mascotStyle: QiraatiMascotStyle = QiraatiMascotStyle.BLUE_BIRD
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
 * يدعم أيضاً إبراز علامة التشكيل نفسها (مثل تنوين الضم 'ٌ' مع الحرف الحامل لها).
 */
fun findArabicLetterRanges(word: String, targetChar: Char): List<QiraatiHighlightRange> {
    val ranges = mutableListOf<QiraatiHighlightRange>()
    if (targetChar.isArabicDiacritic()) {
        word.forEachIndexed { idx, ch ->
            if (ch == targetChar) {
                val start = (idx - 1).coerceAtLeast(0)
                var end = idx + 1
                // في تنوين الفتح (مثل: عَلَمًا / بَابًا) نضم الألف التابعة مباشرة لتتلوّن كاملة بالأحمر
                if (targetChar == '\u064B' && end < word.length && word[end] == 'ا') {
                    end++
                }
                ranges.add(QiraatiHighlightRange(startInclusive = start, endExclusive = end))
            }
        }
        return ranges
    }
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
    val cleanInputSegment = segment.replace("ـ", "")
    val exactIndex = word.indexOf(cleanInputSegment)
    if (exactIndex >= 0) {
        var end = exactIndex + cleanInputSegment.length
        while (end < word.length && word[end].isArabicDiacritic()) {
            end++
        }
        return listOf(QiraatiHighlightRange(startInclusive = exactIndex, endExclusive = end))
    }
    val cleanSegment = cleanInputSegment.filterNot { it.isArabicDiacritic() }
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
