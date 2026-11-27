package com.example.reading.qiraati

import androidx.compose.ui.graphics.Color

/**
 * بيانات الوحدة الأولى الكاملة: "الوحدة الأولى: أسرتي"
 * من كتاب قراءتي الجديدة — الصف الأول الابتدائي (المنهج العراقي الرسمي)
 * مضبوطة بالتشكيل الكامل ومهيكلة وفق تسلسل أنشطة الكتاب الستة + بطاقة أقرأ + التتبع والكتابة.
 */
object Unit1UsratyData {

    private val unitTheme = QiraatiUnitTheme(
        primaryColor = Color(0xFFFF7A59),
        shadowColor = Color(0xFFD14B2B),
        softSurfaceColor = Color(0xFFFFF4EE),
        badgeColor = Color(0xFFFFC83D),
        accentHighlightRed = Color(0xFFE11D48),
        gradientStart = Color(0xFFFF8E6E),
        gradientEnd = Color(0xFFFF6542)
    )

    // =========================================================================
    // الدرس الأول والثاني: الاستماع والمحادثة — أنا أتحدث
    // =========================================================================
    val storySection: UnitStorySection = UnitStorySection(
        id = "unit1_story_usraty",
        headerLabel = "الدرس الأول والثاني • الاستماع والمحادثة",
        title = "أُسْرَتِي الْمُتَحَابَّةُ — أَنَا أَتَحَدَّثُ",
        listeningScene = QiraatiStoryScene(
            id = "usraty_scene_listening",
            sceneNumber = 0,
            title = "مشهد الاستماع: أُسْرَتِي أَمَامَ بَيْتِنَا الْجَمِيلِ",
            audioDescription = "نرى في الصورة أسرةً عراقيةً متحابةً وسعيدةً، تضمّ الأبَ والأمَّ والولدَ والبنتَ، يقفون معاً بابتسامةٍ دافئةٍ أمامَ بيتِهم الجميلِ المُحاطِ بالأشجارِ والزهور.",
            narrationText = "أَنَا أُحِبُّ أُسْرَتِي كَثِيراً؛ أَبِي وَأُمِّي وَأَخِي وَأُخْتِي. نَعِيشُ مَعاً فِي بَيْتٍ جَمِيلٍ يَمْلَؤُهُ الْحُبُّ وَالتَّعَاوُنُ وَالِاحْتِرَامُ.",
            artKey = QiraatiStoryArtKey.USRATY_FAMILY_HOME,
            isListeningMainScene = true
        ),
        conversationScenes = listOf(
            QiraatiStoryScene(
                id = "usraty_scene_1_kitchen",
                sceneNumber = 1,
                title = "الصورة الأولى: مُسَاعَدَةُ الْأُمِّ فِي الْمَطْبَخِ",
                audioDescription = "الأبناء يتعاونون بسعادة لمساعدة الأم في المطبخ في ترتيب المائدة وإعداد الطعام اللذيذ للأسرة.",
                narrationText = "يُسَاعِدُ الْأَبْنَاءُ أُمَّهُمْ فِي الْمَطْبَخِ بِحُبٍّ وَنَشَاطٍ، فَالْتَّعَاوُنُ يَجْعَلُ الْعَمَلَ سَهْلاً وَمُمْتِعاً.",
                artKey = QiraatiStoryArtKey.USRATY_KITCHEN_HELP
            ),
            QiraatiStoryScene(
                id = "usraty_scene_2_reading",
                sceneNumber = 2,
                title = "الصورة الثانية: جَلْسَةُ الْقِرَاءَةِ الْعَائِلِيَّةِ",
                audioDescription = "تجتمع الأسرة في غرفة الجلوس الهادئة، حيث يقرأ الأب والأم قصةً جميلةً مع الأولاد ويستمتعون بالتعلّم معاً.",
                narrationText = "تَجْتَمِعُ الْأُسْرَةُ فِي غُرْفَةِ الْجُلُوسِ لِجَلْسَةِ الْقِرَاءَةِ الْعَائِلِيَّةِ، نَسْتَمِعُ لِلْقِصَصِ وَنَتَعَلَّمُ كَلِمَاتٍ جَدِيدَةً.",
                artKey = QiraatiStoryArtKey.USRATY_FAMILY_READING
            ),
            QiraatiStoryScene(
                id = "usraty_scene_3_grandma",
                sceneNumber = 3,
                title = "الصورة الثالثة: مُسَاعَدَةُ الْجَدَّةِ عِنْدَ بَابِ الْمَنْزِلِ",
                audioDescription = "الطفل المهذّب يستقبل جدّته بحنان عند باب المنزل ويمدّ لها يده ليساعدها في الدخول وحمل أغراضها.",
                narrationText = "الْطِّفْلُ يُسَاعِدُ جَدَّتَهُ عِنْدَ بَابِ الْمَنْزِلِ بِلُطْفٍ وَاحْتِرَامٍ، فَنَحْنُ نُحِبُّ كِبَارَنَا وَنَرْعَاهُمْ دَائِماً.",
                artKey = QiraatiStoryArtKey.USRATY_HELPING_GRANDMA
            )
        ),
        interactiveStoryLines = listOf(
            "أُسْرَتِي مُتَحَابَّةٌ وَسَعِيدَةٌ تَعِيشُ فِي بَيْتٍ جَمِيلٍ.",
            "يَتَعَاوَنُ الْأَبْنَاءُ فِي مُسَاعَدَةِ الْأُمِّ دَاخِلَ الْمَطْبَخِ.",
            "فِي الْمَسَاءِ تَجْتَمِعُ الْأُسْرَةُ لِلْقِرَاءَةِ فِي غُرْفَةِ الْجُلُوسِ.",
            "وَنَحْتَرِمُ جَدَّتَنَا وَنُسَاعِدُهَا بِلُطْفٍ عِنْدَ بَابِ الْمَنْزِلِ."
        ),
        iSpeakQuestions = listOf(
            "أصف ما أراه في كل صورة",
            "أكوّن قصة قصيرة من الصور الثلاث",
            "كيف أتعامل مع كل فرد من أفراد أسرتي؟"
        )
    )

    // =========================================================================
    // درس حرف الباء (ب ، بـ)
    // =========================================================================
    val baLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit1_lesson_ba",
        unitNumber = 1,
        lessonOrder = 1,
        letterName = "حرف الباء",
        targetLetter = "ب",
        cloudForms = listOf("ب", "بـ"),
        accentColor = Color(0xFFFF7A59),
        shadowColor = Color(0xFFD14B2B),
        introWords = listOf(
            QiraatiIntroWord("بَقَرَةُ", 'ب', LetterPosition.INITIAL, QiraatiWordArtKey.COW),
            QiraatiIntroWord("بُرْتُقَالُ", 'ب', LetterPosition.INITIAL, QiraatiWordArtKey.ORANGE),
            QiraatiIntroWord("بِسَاطُ", 'ب', LetterPosition.INITIAL, QiraatiWordArtKey.CARPET),
            QiraatiIntroWord("حَلِيبُ", 'ب', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.MILK),
            QiraatiIntroWord("خُبْزُ", 'ب', LetterPosition.MEDIAL, QiraatiWordArtKey.BREAD),
            QiraatiIntroWord("بَابُ", 'ب', LetterPosition.MULTIPLE, QiraatiWordArtKey.DOOR),
            QiraatiIntroWord("بُوْقُ", 'ب', LetterPosition.INITIAL, QiraatiWordArtKey.BUGLE),
            QiraatiIntroWord("رَبِيعُ", 'ب', LetterPosition.MEDIAL, QiraatiWordArtKey.SPRING)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("بَ", "بَا", "فتحة", "مد بالألف"),
            QiraatiVowelPair("بُ", "بُوْ", "ضمة", "مد بالواو"),
            QiraatiVowelPair("بِ", "بِيْ", "كسرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("بَصَلُ", isLongVowel = false, vowelLabel = "مد قصير (فتحة)", highlightedSegment = "بَ", artKey = QiraatiWordArtKey.ONION),
            QiraatiVowelWord("بُرْتُقَالُ", isLongVowel = false, vowelLabel = "مد قصير (ضمة)", highlightedSegment = "بُ", artKey = QiraatiWordArtKey.ORANGE),
            QiraatiVowelWord("بِطِّيْخُ", isLongVowel = false, vowelLabel = "مد قصير (كسرة)", highlightedSegment = "بِ", artKey = QiraatiWordArtKey.WATERMELON),
            QiraatiVowelWord("بَابُ", isLongVowel = true, vowelLabel = "مد طويل (ألف)", highlightedSegment = "بَا", artKey = QiraatiWordArtKey.DOOR),
            QiraatiVowelWord("بُوْقُ", isLongVowel = true, vowelLabel = "مد طويل (واو)", highlightedSegment = "بُوْ", artKey = QiraatiWordArtKey.BUGLE),
            QiraatiVowelWord("رَبِيعُ", isLongVowel = true, vowelLabel = "مد طويل (ياء)", highlightedSegment = "بِي", artKey = QiraatiWordArtKey.SPRING)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("طَبْلُ", sukoonSyllable = "طَبْ", remainingPart = "لُ", artKey = QiraatiWordArtKey.DRUM),
            QiraatiSukoonItem("جُبْنُ", sukoonSyllable = "جُبْ", remainingPart = "نُ", artKey = QiraatiWordArtKey.CHEESE),
            QiraatiSukoonItem("شِبْلُ", sukoonSyllable = "شِبْ", remainingPart = "لُ", artKey = QiraatiWordArtKey.LION_CUB)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("بَابُ", listOf("بَا", "بُ")),
            QiraatiAnalysisItem("بَابَا", listOf("بَا", "بَا")),
            QiraatiAnalysisItem("بَابِي", listOf("بَا", "بِي"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "بَا",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "بُ", resultWord = "بَابُ"),
                    QiraatiPuzzleBranch(attachedPiece = "بَا", resultWord = "بَابَا"),
                    QiraatiPuzzleBranch(attachedPiece = "بِي", resultWord = "بَابِي")
                )
            )
        ),
        readAloudWords = listOf("بَابُ", "بَابَا", "بَابِي"),
        tracingLetter = "ب",
        tracingForms = listOf("ب", "بـ")
    )

    // =========================================================================
    // درس حرف الميم (م ، مـ)
    // =========================================================================
    val meemLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit1_lesson_meem",
        unitNumber = 1,
        lessonOrder = 2,
        letterName = "حرف الميم",
        targetLetter = "م",
        cloudForms = listOf("م", "مـ"),
        accentColor = Color(0xFFEC4899),
        shadowColor = Color(0xFFBE185D),
        introWords = listOf(
            QiraatiIntroWord("مَامَا", 'م', LetterPosition.MULTIPLE, QiraatiWordArtKey.MAMA),
            QiraatiIntroWord("مَوْزُ", 'م', LetterPosition.INITIAL, QiraatiWordArtKey.BANANA),
            QiraatiIntroWord("مِقَصُّ", 'م', LetterPosition.INITIAL, QiraatiWordArtKey.SCISSORS),
            QiraatiIntroWord("قَلَمُ", 'م', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.PEN),
            QiraatiIntroWord("شَمْسُ", 'م', LetterPosition.MEDIAL, QiraatiWordArtKey.SUN),
            QiraatiIntroWord("مَاءُ", 'م', LetterPosition.INITIAL, QiraatiWordArtKey.WATER),
            QiraatiIntroWord("بُوْمُ", 'م', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.OWL),
            QiraatiIntroWord("رُمَّانُ", 'م', LetterPosition.MEDIAL, QiraatiWordArtKey.POMEGRANATE)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("مَ", "مَا", "فتحة", "مد بالألف"),
            QiraatiVowelPair("مُ", "مُوْ", "ضمة", "مد بالواو"),
            QiraatiVowelPair("مِ", "مِيْ", "كسرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("مَطَرُ", isLongVowel = false, vowelLabel = "مد قصير (فتحة)", highlightedSegment = "مَ", artKey = QiraatiWordArtKey.RAIN),
            QiraatiVowelWord("مُعَلِّمُ", isLongVowel = false, vowelLabel = "مد قصير (ضمة)", highlightedSegment = "مُ", artKey = QiraatiWordArtKey.TEACHER),
            QiraatiVowelWord("مِفْتَاحُ", isLongVowel = false, vowelLabel = "مد قصير (كسرة)", highlightedSegment = "مِ", artKey = QiraatiWordArtKey.KEY),
            QiraatiVowelWord("مَاءُ", isLongVowel = true, vowelLabel = "مد طويل (ألف)", highlightedSegment = "مَا", artKey = QiraatiWordArtKey.WATER),
            QiraatiVowelWord("بُوْمُ", isLongVowel = true, vowelLabel = "مد طويل (واو)", highlightedSegment = "بُوْ", artKey = QiraatiWordArtKey.OWL),
            QiraatiVowelWord("مِيْزَانُ", isLongVowel = true, vowelLabel = "مد طويل (ياء)", highlightedSegment = "مِيْ", artKey = QiraatiWordArtKey.SCALE)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("شَمْسُ", sukoonSyllable = "شَمْ", remainingPart = "سُ", artKey = QiraatiWordArtKey.SUN),
            QiraatiSukoonItem("رَمْلُ", sukoonSyllable = "رَمْ", remainingPart = "لُ", artKey = QiraatiWordArtKey.SAND),
            QiraatiSukoonItem("نَمْلُ", sukoonSyllable = "نَمْ", remainingPart = "لُ", artKey = QiraatiWordArtKey.ANT)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("مَامَا", listOf("مَا", "مَا")),
            QiraatiAnalysisItem("بُوْمُ", listOf("بُوْ", "مُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "مَا",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "مَا", resultWord = "مَامَا")
                )
            )
        ),
        readAloudWords = listOf("بَابَا", "مَامَا", "بُوْمُ"),
        tracingLetter = "م",
        tracingForms = listOf("م", "مـ")
    )

    // =========================================================================
    // درس حرف الدال (د ، ـد)
    // =========================================================================
    val dalLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit1_lesson_dal",
        unitNumber = 1,
        lessonOrder = 3,
        letterName = "حرف الدال",
        targetLetter = "د",
        cloudForms = listOf("د", "ـد"),
        accentColor = Color(0xFF0EA5E9),
        shadowColor = Color(0xFF0369A1),
        introWords = listOf(
            QiraatiIntroWord("دَلَّةُ", 'د', LetterPosition.INITIAL, QiraatiWordArtKey.DALLAH),
            QiraatiIntroWord("دُبُّ", 'د', LetterPosition.INITIAL, QiraatiWordArtKey.BEAR),
            QiraatiIntroWord("دِرْهَمُ", 'د', LetterPosition.INITIAL, QiraatiWordArtKey.DIRHAM),
            QiraatiIntroWord("أَسَدُ", 'د', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.LION),
            QiraatiIntroWord("قِدْرُ", 'د', LetterPosition.MEDIAL, QiraatiWordArtKey.POT),
            QiraatiIntroWord("دَارُ", 'د', LetterPosition.INITIAL, QiraatiWordArtKey.HOUSE),
            QiraatiIntroWord("دُوْدُ", 'د', LetterPosition.MULTIPLE, QiraatiWordArtKey.WORM),
            QiraatiIntroWord("دِيْكُ", 'د', LetterPosition.INITIAL, QiraatiWordArtKey.ROOSTER)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("دَ", "دا", "فتحة", "مد بالألف"),
            QiraatiVowelPair("دُ", "دُوْ", "ضمة", "مد بالواو"),
            QiraatiVowelPair("دِ", "دِيْ", "كسرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("دُمْيَةُ", isLongVowel = false, vowelLabel = "مد قصير (ضمة)", highlightedSegment = "دُ", artKey = QiraatiWordArtKey.DOLL),
            QiraatiVowelWord("دَرَجُ", isLongVowel = false, vowelLabel = "مد قصير (فتحة)", highlightedSegment = "دَ", artKey = QiraatiWordArtKey.STAIRS),
            QiraatiVowelWord("دِبْسُ", isLongVowel = false, vowelLabel = "مد قصير (كسرة)", highlightedSegment = "دِ", artKey = QiraatiWordArtKey.DATE_SYRUP),
            QiraatiVowelWord("دَارُ", isLongVowel = true, vowelLabel = "مد طويل (ألف)", highlightedSegment = "دَا", artKey = QiraatiWordArtKey.HOUSE),
            QiraatiVowelWord("دُوْرُ", isLongVowel = true, vowelLabel = "مد طويل (واو)", highlightedSegment = "دُوْ", artKey = QiraatiWordArtKey.HOUSES),
            QiraatiVowelWord("دِيْكُ", isLongVowel = true, vowelLabel = "مد طويل (ياء)", highlightedSegment = "دِيْ", artKey = QiraatiWordArtKey.ROOSTER)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("بَدْرُ", sukoonSyllable = "بَدْ", remainingPart = "رُ", artKey = QiraatiWordArtKey.FULL_MOON),
            QiraatiSukoonItem("قِدْرُ", sukoonSyllable = "قِدْ", remainingPart = "رُ", artKey = QiraatiWordArtKey.POT),
            QiraatiSukoonItem("هُدْهُدُ", sukoonSyllable = "هُدْ", remainingPart = "هُدُ", artKey = QiraatiWordArtKey.HOOPOE)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("دَادَا", listOf("دَا", "دَا")),
            QiraatiAnalysisItem("دُوْدُ", listOf("دُوْ", "دُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "دَا",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "دَا", resultWord = "دَادَا"),
                    QiraatiPuzzleBranch(attachedPiece = "مَ", resultWord = "دَامَ")
                )
            )
        ),
        readAloudWords = listOf("بَابَا", "مَامَا", "دَادَا"),
        tracingLetter = "د",
        tracingForms = listOf("د", "ـد")
    )

    // =========================================================================
    // درس حرف الفاء (ف ، فـ)
    // =========================================================================
    val faLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit1_lesson_fa",
        unitNumber = 1,
        lessonOrder = 4,
        letterName = "حرف الفاء",
        targetLetter = "ف",
        cloudForms = listOf("ف", "فـ"),
        accentColor = Color(0xFF22C55E),
        shadowColor = Color(0xFF15803D),
        introWords = listOf(
            QiraatiIntroWord("فَهْدُ", 'ف', LetterPosition.INITIAL, QiraatiWordArtKey.CHEETAH),
            QiraatiIntroWord("فُرْنُ", 'ف', LetterPosition.INITIAL, QiraatiWordArtKey.OVEN),
            QiraatiIntroWord("فِنْجَانُ", 'ف', LetterPosition.INITIAL, QiraatiWordArtKey.CUP),
            QiraatiIntroWord("فَانُوْسُ", 'ف', LetterPosition.INITIAL, QiraatiWordArtKey.LANTERN),
            QiraatiIntroWord("عُصْفُوْرُ", 'ف', LetterPosition.MEDIAL, QiraatiWordArtKey.SPARROW),
            QiraatiIntroWord("فِيْلُ", 'ف', LetterPosition.INITIAL, QiraatiWordArtKey.ELEPHANT),
            QiraatiIntroWord("فَمُ", 'ف', LetterPosition.INITIAL, QiraatiWordArtKey.MOUTH),
            QiraatiIntroWord("دُفُوْفُ", 'ف', LetterPosition.MULTIPLE, QiraatiWordArtKey.TAMBOURINES)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("فَ", "فا", "فتحة", "مد بالألف"),
            QiraatiVowelPair("فُ", "فُوْ", "ضمة", "مد بالواو"),
            QiraatiVowelPair("فِ", "فِيْ", "كسرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("فَهْدُ", isLongVowel = false, vowelLabel = "مد قصير (فتحة)", highlightedSegment = "فَ", artKey = QiraatiWordArtKey.CHEETAH),
            QiraatiVowelWord("فُرْنُ", isLongVowel = false, vowelLabel = "مد قصير (ضمة)", highlightedSegment = "فُ", artKey = QiraatiWordArtKey.OVEN),
            QiraatiVowelWord("فِنْجَانُ", isLongVowel = false, vowelLabel = "مد قصير (كسرة)", highlightedSegment = "فِ", artKey = QiraatiWordArtKey.CUP),
            QiraatiVowelWord("فَانُوْسُ", isLongVowel = true, vowelLabel = "مد طويل (ألف)", highlightedSegment = "فَا", artKey = QiraatiWordArtKey.LANTERN),
            QiraatiVowelWord("عُصْفُوْرُ", isLongVowel = true, vowelLabel = "مد طويل (واو)", highlightedSegment = "فُوْ", artKey = QiraatiWordArtKey.SPARROW),
            QiraatiVowelWord("فِيْلُ", isLongVowel = true, vowelLabel = "مد طويل (ياء)", highlightedSegment = "فِيْ", artKey = QiraatiWordArtKey.ELEPHANT)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("ضِفْدَعُ", sukoonSyllable = "ضِفْ", remainingPart = "دَعُ", artKey = QiraatiWordArtKey.FROG),
            QiraatiSukoonItem("حَفْلَةُ", sukoonSyllable = "حَفْ", remainingPart = "لَةُ", artKey = QiraatiWordArtKey.PARTY),
            QiraatiSukoonItem("طِفْلُ", sukoonSyllable = "طِفْ", remainingPart = "لُ", artKey = QiraatiWordArtKey.CHILD)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("فَمُ", listOf("فَ", "مُ")),
            QiraatiAnalysisItem("مُفِيْدُ", listOf("مُ", "فِيْ", "دُ")),
            QiraatiAnalysisItem("دُفُوْفُ", listOf("دُ", "فُوْ", "فُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "فَا",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "دِي", resultWord = "فَادِي")
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "بَا",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "دِي", resultWord = "بَادِي")
                )
            )
        ),
        readAloudWords = listOf("فَمُ", "فَادِي", "مُفِيْدُ"),
        tracingLetter = "ف",
        tracingForms = listOf("ف", "فـ")
    )

    // =========================================================================
    // كائن الوحدة الأولى الكامل جاهز للتسجيل في NewQiraatiRepository
    // =========================================================================
    val unit: QiraatiUnit = QiraatiUnit(
        id = "unit_1_usraty",
        unitNumber = 1,
        unitBadge = "الوحدة الأولى",
        title = "الوحدة الأولى: أسرتي",
        shortTitle = "أسرتي",
        description = "قصة الاستماع والمحادثة مع دروس الحروف: الباء والميم والدال والفاء",
        themeColor = unitTheme.primaryColor,
        theme = unitTheme,
        storySection = storySection,
        letterLessons = listOf(
            baLesson,
            meemLesson,
            dalLesson,
            faLesson
        ),
        isAvailable = true,
        coverArtKey = QiraatiStoryArtKey.USRATY_FAMILY_HOME
    )
}
