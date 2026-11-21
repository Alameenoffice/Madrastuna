package com.example.reading.qiraati

import androidx.compose.ui.graphics.Color

/**
 * بيانات الوحدة الرابعة الكاملة من كتاب "قراءتي الجديدة — الصف الأول الابتدائي" (المنهج العراقي):
 * «الوحدة الرابعة: صحتي وبيئتي»
 *
 * تشمل:
 * - الاستماع والمحادثة (صحتي وبيئتي — أنا أتحدث: حوار أدوات النظافة + 3 مشاهد + 4 أسئلة).
 * - الدروس الثلاثة الكاملة بدقة التشكيل:
 *   1) حرف الحاء (ح ، حـ)
 *   2) حرف الشين (ش ، شـ)
 *   3) حرف الزاي (ز)
 * - الشخصية المرافقة: النحلة المبتسمة مع القلم (mascotStyle = QiraatiMascotStyle.BEE).
 */
object Unit4SihatiWaBiatiData {

    private val storySection = UnitStorySection(
        id = "qiraati_u4_story_sihati",
        headerLabel = "الاستماع والمحادثة",
        title = "صِحَّتِي وَبِيْئَتِي — أَنَا أَتَحَدَّثُ",
        listeningScene = QiraatiStoryScene(
            id = "u4_scene_0_listen",
            sceneNumber = 0,
            title = "أَدَوَاتُ النَّظَافَةِ الشَّخْصِيَّةِ",
            audioDescription = "تَتَحَدَّثُ الْأُمُّ مَعَ طِفْلِهَا عَنْ أَدَوَاتِ النَّظَافَةِ الشَّخْصِيَّةِ: الْمِقَصِّ، وَالْمِشْطِ، وَمِقْرَاضِ الْأَظَافِرِ، وَالْمِنْشَفَةِ النَّظِيْفَةِ، وَفُرْشَاةِ وَمَعْجُوْنِ الْأَسْنَانِ.",
            narrationText = "سَأَلَ الطِّفْلُ أُمَّهُ: كَيْفَ أَحْمِي صِحَّتِي كُلَّ يَوْمٍ؟ أَجَابَتِ الْأُمُّ بِلُطْفٍ: نَسْتَعْمِلُ أَدَوَاتِ النَّظَافَةِ الشَّخْصِيَّةِ بِنِظَامٍ؛ الْمِشْطَ لِتَسْرِيْحِ الشَّعْرِ، وَمِقْرَاضَ الْأَظَافِرِ وَالْمِقَصَّ بِحَذَرٍ، وَالْمِنْشَفَةَ النَّظِيْفَةَ، وَفُرْشَاةَ وَمَعْجُوْنَ الْأَسْنَانِ صَبَاحًا وَمَسَاءً.",
            artKey = QiraatiStoryArtKey.SIHATI_HYGIENE_TOOLS,
            isListeningMainScene = true
        ),
        conversationScenes = listOf(
            QiraatiStoryScene(
                id = "u4_scene_1",
                sceneNumber = 1,
                title = "غَسْلُ الْيَدَيْنِ بِالْمَاءِ وَالصَّابُوْنِ",
                audioDescription = "طِفْلٌ يَغْسِلُ يَدَيْهِ بِالْمَاءِ النَّظِيْفِ وَالصَّابُوْنِ بِعِنَايَةٍ قَبْلَ الطَّعَامِ وَبَعْدَهُ.",
                narrationText = "يَغْسِلُ الطِّفْلُ يَدَيْهِ جَيِّدًا بِالْمَاءِ وَالصَّابُوْنِ لِيُحَافِظَ عَلَى نَظَافَتِهِ وَصِحَّتِهِ.",
                artKey = QiraatiStoryArtKey.SIHATI_HAND_WASHING
            ),
            QiraatiStoryScene(
                id = "u4_scene_2",
                sceneNumber = 2,
                title = "إِفْطَارٌ صِحِّيٌّ مَعَ الْأُسْرَةِ",
                audioDescription = "أُسْرَةٌ سَعِيْدَةٌ تَتَنَاوَلُ وَجْبَةَ إِفْطَارٍ صِحِّيَّةً تَضُمُّ الْحَلِيْبَ وَالْجُبْنَ وَالزَّيْتُوْنَ وَالْفَوَاكِهَ الطَّازَجَةَ مَعًا.",
                narrationText = "تَجْتَمِعُ الْأُسْرَةُ حَوْلَ مَائِدَةِ الْإِفْطَارِ الصِّحِّيِّ لِيَبْدَأَ الْجَمِيْعُ يَوْمَهُمْ بِنَشَاطٍ وَحَيَوِيَّةٍ.",
                artKey = QiraatiStoryArtKey.SIHATI_HEALTHY_BREAKFAST
            ),
            QiraatiStoryScene(
                id = "u4_scene_3",
                sceneNumber = 3,
                title = "اللَّعِبُ فِي الْحَدِيْقَةِ وَالْهَوَاءِ الطَّلْقِ",
                audioDescription = "أَفْرَادُ الْأُسْرَةِ يَمْرَحُوْنَ وَيَلْعَبُوْنَ فِي الْهَوَاءِ الطَّلْقِ بِالْحَدِيْقَةِ بِالدَّرَّاجَةِ وَالطَّائِرَةِ الْوَرَقِيَّةِ.",
                narrationText = "فِي الْحَدِيْقَةِ الْخَضْرَاءِ النَّظِيْفَةِ يَرْكَبُ الطِّفْلُ دَرَّاجَتَهُ وَتُحَلِّقُ الطَّائِرَةُ الْوَرَقِيَّةُ فِي السَّمَاءِ الصَّافِيَةِ.",
                artKey = QiraatiStoryArtKey.SIHATI_PARK_PLAY_KITE_BIKE
            )
        ),
        interactiveStoryLines = listOf(
            "تَتَحَدَّثُ الْأُمُّ مَعَ طِفْلِهَا عَنْ أَدَوَاتِ النَّظَافَةِ الشَّخْصِيَّةِ: الْمِقَصِّ، وَالْمِشْطِ، وَمِقْرَاضِ الْأَظَافِرِ، وَالْمِنْشَفَةِ، وَفُرْشَاةِ وَمَعْجُوْنِ الْأَسْنَانِ.",
            "يَغْسِلُ الطِّفْلُ يَدَيْهِ بِالْمَاءِ وَالصَّابُوْنِ بِعِنَايَةٍ كُلَّ يَوْمٍ.",
            "تَتَنَاوَلُ الْأُسْرَةُ وَجْبَةَ إِفْطَارٍ صِحِّيَّةً مَعًا بِنَشَاطٍ وَفَرَحٍ.",
            "يَمْرَحُ أَفْرَادُ الْأُسْرَةِ فِي الْهَوَاءِ الطَّلْقِ بِالْحَدِيْقَةِ بِالدَّرَّاجَةِ وَالطَّائِرَةِ الْوَرَقِيَّةِ، وَيُحَافِظُوْنَ عَلَى نَظَافَةِ بِيْئَتِهِمْ."
        ),
        iSpeakQuestions = listOf(
            "أصف ما أراه في كل صورة.",
            "أُكَوِّنُ قصة قصيرة من الصور.",
            "كيف أحافظ على صحتي؟",
            "أتحدث عن النظافة والصحة والعناية بها."
        )
    )

    // =========================================================================
    // الدرس 1: حرف الحاء (ح ، حـ)
    // =========================================================================
    private val haLesson = QiraatiLetterLesson(
        id = "qiraati_u4_l1_ha",
        unitNumber = 4,
        lessonOrder = 1,
        letterName = "حرف الحاء (ح)",
        targetLetter = "ح",
        cloudForms = listOf("ح", "حـ"),
        accentColor = Color(0xFFEC4899),
        shadowColor = Color(0xFFBE185D),
        introWords = listOf(
            QiraatiIntroWord("حَمَامَةٌ", 'ح', LetterPosition.INITIAL, QiraatiWordArtKey.DOVE),
            QiraatiIntroWord("حُبُوْبٌ", 'ح', LetterPosition.INITIAL, QiraatiWordArtKey.GRAINS),
            QiraatiIntroWord("حِذَاءٌ", 'ح', LetterPosition.INITIAL, QiraatiWordArtKey.SHOE),
            QiraatiIntroWord("مِفْتَاحٌ", 'ح', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.KEY),
            QiraatiIntroWord("لَحْمٌ", 'ح', LetterPosition.MEDIAL, QiraatiWordArtKey.MEAT),
            QiraatiIntroWord("حَائِطٌ", 'ح', LetterPosition.INITIAL, QiraatiWordArtKey.WALL),
            QiraatiIntroWord("حُوْتٌ", 'ح', LetterPosition.INITIAL, QiraatiWordArtKey.WHALE),
            QiraatiIntroWord("طَحِيْنٌ", 'ح', LetterPosition.MEDIAL, QiraatiWordArtKey.FLOUR)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("حَ", "حَا", "فتحة قصيرة", "مد بالألف"),
            QiraatiVowelPair("حُ", "حُوْ", "ضمة قصيرة", "مد بالواو"),
            QiraatiVowelPair("حِ", "حِيْ", "كسرة قصيرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("حَلْوَى", false, "مد قصير (حَ)", "حَ", QiraatiWordArtKey.SWEETS),
            QiraatiVowelWord("حُبُوْبٌ", false, "مد قصير (حُ)", "حُ", QiraatiWordArtKey.GRAINS),
            QiraatiVowelWord("حِزَامٌ", false, "مد قصير (حِ)", "حِ", QiraatiWordArtKey.BELT),
            QiraatiVowelWord("حَائِطٌ", true, "مد طويل (حَا)", "حَا", QiraatiWordArtKey.WALL),
            QiraatiVowelWord("حُوْتٌ", true, "مد طويل (حُوْ)", "حُوْ", QiraatiWordArtKey.WHALE),
            QiraatiVowelWord("طَحِيْنٌ", true, "مد طويل (حِيْ)", "حِيْ", QiraatiWordArtKey.FLOUR)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("مَحْفَظَةٌ", "مَحْـ", "فَظَةٌ", QiraatiWordArtKey.WALLET),
            QiraatiSukoonItem("فَحْمٌ", "فَحْـ", "مٌ", QiraatiWordArtKey.COAL),
            QiraatiSukoonItem("مِحْرَارٌ", "مِحْـ", "رَارٌ", QiraatiWordArtKey.THERMOMETER)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("نَحْلٌ", listOf("نَحْ", "لٌ")),
            QiraatiAnalysisItem("حَقْلٌ", listOf("حَقْ", "لٌ")),
            QiraatiAnalysisItem("حَبْلٌ", listOf("حَبْ", "لٌ")),
            QiraatiAnalysisItem("تِمْسَاحٌ", listOf("تِمْ", "سَا", "حٌ")),
            QiraatiAnalysisItem("حَنَانٌ", listOf("حَـ", "نَا", "نٌ")),
            QiraatiAnalysisItem("مِفْتَاحٌ", listOf("مِفْ", "تَا", "حٌ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "سَـ",
                branches = listOf(
                    QiraatiPuzzleBranch("حَا + بٌ", "سَحَابٌ", pieces = listOf("سَـ", "حَا", "بٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة سَحَابٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "حَا",
                branches = listOf(
                    QiraatiPuzzleBranch("مِـ + دٌ", "حَامِدٌ", pieces = listOf("حَا", "مِـ", "دٌ")),
                    QiraatiPuzzleBranch("تِـ + مٌ", "حَاتِمٌ", pieces = listOf("حَا", "تِـ", "مٌ")),
                    QiraatiPuzzleBranch("رِ + سٌ", "حَارِسٌ", pieces = listOf("حَا", "رِ", "سٌ"))
                ),
                promptHint = "اربط المقطع (حَا) مع المقاطع الملونة لتكوين الكلمات"
            )
        ),
        readAloudWords = listOf(
            "يُطْعِمُ حَسَنٌ حَمَامًا ،",
            "قَالَ حَسَنٌ : نُرْفِقُ بِحَمَامِنَا ، وَلَاْ نَحْبِسُ حَيْوَانًا ."
        ),
        tracingLetter = "ح",
        tracingForms = listOf("ح", "حـ", "حَ", "حُ")
    )

    // =========================================================================
    // الدرس 2: حرف الشين (ش ، شـ)
    // =========================================================================
    private val sheenLesson = QiraatiLetterLesson(
        id = "qiraati_u4_l2_sheen",
        unitNumber = 4,
        lessonOrder = 2,
        letterName = "حرف الشين (ش)",
        targetLetter = "ش",
        cloudForms = listOf("ش", "شـ"),
        accentColor = Color(0xFF06B6D4),
        shadowColor = Color(0xFF0E7490),
        introWords = listOf(
            QiraatiIntroWord("شَمْعَةٌ", 'ش', LetterPosition.INITIAL, QiraatiWordArtKey.CANDLE),
            QiraatiIntroWord("شُبَّاكٌ", 'ش', LetterPosition.INITIAL, QiraatiWordArtKey.WINDOW),
            QiraatiIntroWord("شِرَاعٌ", 'ش', LetterPosition.INITIAL, QiraatiWordArtKey.SAIL),
            QiraatiIntroWord("مِشْمِشٌ", 'ش', LetterPosition.MULTIPLE, QiraatiWordArtKey.APRICOT),
            QiraatiIntroWord("أَشْجَارٌ", 'ش', LetterPosition.MEDIAL, QiraatiWordArtKey.TREES),
            QiraatiIntroWord("شَارِعٌ", 'ش', LetterPosition.INITIAL, QiraatiWordArtKey.STREET),
            QiraatiIntroWord("قُشُوْرٌ", 'ش', LetterPosition.MEDIAL, QiraatiWordArtKey.PEELS),
            QiraatiIntroWord("مَنَاشِيْرُ", 'ش', LetterPosition.MEDIAL, QiraatiWordArtKey.SAWS)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("شَـ", "شَا", "فتحة قصيرة", "مد بالألف", "شَ", "شَا"),
            QiraatiVowelPair("شُـ", "شُوْ", "ضمة قصيرة", "مد بالواو", "شُ", "شُوْ"),
            QiraatiVowelPair("شِـ", "شِيْ", "كسرة قصيرة", "مد بالياء", "شِ", "شِيْ")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("شَمْسٌ", false, "مد قصير (شَـ)", "شَ", QiraatiWordArtKey.SUN),
            QiraatiVowelWord("شِبْلٌ", false, "مد قصير (شِـ)", "شِ", QiraatiWordArtKey.LION_CUB),
            QiraatiVowelWord("شُبَّاكٌ", false, "مد قصير (شُـ)", "شُ", QiraatiWordArtKey.WINDOW),
            QiraatiVowelWord("شَارِعٌ", true, "مد طويل (شَا)", "شَا", QiraatiWordArtKey.STREET),
            QiraatiVowelWord("قُشُوْرٌ", true, "مد طويل (شُوْ)", "شُوْ", QiraatiWordArtKey.PEELS),
            QiraatiVowelWord("طَبَاشِيْرُ", true, "مد طويل (شِيْ)", "شِيْ", QiraatiWordArtKey.CHALK)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("مِشْعَلٌ", "مِشْـ", "عَلٌ", QiraatiWordArtKey.TORCH),
            QiraatiSukoonItem("عُشْبٌ", "عُشْـ", "بٌ", QiraatiWordArtKey.GRASS),
            QiraatiSukoonItem("مِشْطٌ", "مِشْـ", "طٌ", QiraatiWordArtKey.COMB)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("مِنْشَارٌ", listOf("مِنْ", "شَا", "رٌ")),
            QiraatiAnalysisItem("شَارِعٌ", listOf("شَا", "رِ", "عٌ")),
            QiraatiAnalysisItem("قُشُوْرٌ", listOf("قُـ", "شُوْ", "رٌ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "فَـ",
                branches = listOf(
                    QiraatiPuzzleBranch("رَا + شٌ", "فَرَاشٌ", pieces = listOf("فَـ", "رَا", "شٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة فَرَاشٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "شَمْـ",
                branches = listOf(
                    QiraatiPuzzleBranch("سٌ", "شَمْسٌ", pieces = listOf("شَمْـ", "سٌ"))
                ),
                promptHint = "اربط القطعتين لتكوين كلمة شَمْسٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "شَمْـ",
                branches = listOf(
                    QiraatiPuzzleBranch("عَـ + ةٌ", "شَمْعَةٌ", pieces = listOf("شَمْـ", "عَـ", "ةٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة شَمْعَةٌ"
            )
        ),
        readAloudWords = listOf(
            "شِيْرِيْنُ تَحْمِلُ مِشْمِشًا وَوَرْدًا .",
            "تَقُوْلُ مَامَا : نَسِيْمُ حَدِيْقَتِنَا حُلْوٌ ، وَشَمْسُنَا مُشْرِقَةٌ ."
        ),
        tracingLetter = "ش",
        tracingForms = listOf("ش", "شـ", "شَ", "شُ")
    )

    // =========================================================================
    // الدرس 3: حرف الزاي (ز)
    // =========================================================================
    private val zayLesson = QiraatiLetterLesson(
        id = "qiraati_u4_l3_zay",
        unitNumber = 4,
        lessonOrder = 3,
        letterName = "حرف الزاي (ز)",
        targetLetter = "ز",
        cloudForms = listOf("ز", "ـز"),
        accentColor = Color(0xFF10B981),
        shadowColor = Color(0xFF047857),
        introWords = listOf(
            QiraatiIntroWord("زَهْرَةٌ", 'ز', LetterPosition.INITIAL, QiraatiWordArtKey.ROSE),
            QiraatiIntroWord("زُجَاجَةُ مَاءٍ", 'ز', LetterPosition.INITIAL, QiraatiWordArtKey.WATER_BOTTLE),
            QiraatiIntroWord("زِرٌّ", 'ز', LetterPosition.INITIAL, QiraatiWordArtKey.BUTTON),
            QiraatiIntroWord("مَاعِزٌ", 'ز', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.GOAT),
            QiraatiIntroWord("مِزْمَارٌ", 'ز', LetterPosition.MEDIAL, QiraatiWordArtKey.FLUTE),
            QiraatiIntroWord("غَزَالٌ", 'ز', LetterPosition.MEDIAL, QiraatiWordArtKey.GAZELLE),
            QiraatiIntroWord("حَلَزُوْنٌ", 'ز', LetterPosition.MEDIAL, QiraatiWordArtKey.SNAIL),
            QiraatiIntroWord("جَزِيْرَةٌ", 'ز', LetterPosition.MEDIAL, QiraatiWordArtKey.ISLAND)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("زَ", "زَا", "فتحة قصيرة", "مد بالألف"),
            QiraatiVowelPair("زُ", "زُوْ", "ضمة قصيرة", "مد بالواو"),
            QiraatiVowelPair("زِ", "زِيْ", "كسرة قصيرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("زَيْتُوْنٌ", false, "مد قصير (زَ)", "زَ", QiraatiWordArtKey.OLIVES),
            QiraatiVowelWord("زُجَاجَةُ مَاءٍ", false, "مد قصير (زُ)", "زُ", QiraatiWordArtKey.WATER_BOTTLE),
            QiraatiVowelWord("زِرٌّ", false, "مد قصير (زِ)", "زِ", QiraatiWordArtKey.BUTTON),
            QiraatiVowelWord("غَزَالٌ", true, "مد طويل (زَا)", "زَا", QiraatiWordArtKey.GAZELLE),
            QiraatiVowelWord("حَلَزُوْنٌ", true, "مد طويل (زُوْ)", "زُوْ", QiraatiWordArtKey.SNAIL),
            QiraatiVowelWord("زِيْنَةٌ", true, "مد طويل (زِيْ)", "زِيْ", QiraatiWordArtKey.DECORATION)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("مِزْمَارٌ", "مِزْ", "مَارٌ", QiraatiWordArtKey.FLUTE),
            QiraatiSukoonItem("أَزْهَارٌ", "أَزْ", "هَارٌ", QiraatiWordArtKey.FLOWERS),
            QiraatiSukoonItem("مِزْهَرِيَّةٌ", "مِزْ", "هَرِيَّةٌ", QiraatiWordArtKey.VASE)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("زَوْرَقٌ", listOf("زَوْ", "رَ", "قٌ")),
            QiraatiAnalysisItem("زَيْتُوْنٌ", listOf("زَيْ", "تُوْ", "نٌ")),
            QiraatiAnalysisItem("مِزْمَارٌ", listOf("مِزْ", "مَا", "رٌ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "زَوْ",
                branches = listOf(
                    QiraatiPuzzleBranch("رَ + قٌ", "زَوْرَقٌ", pieces = listOf("زَوْ", "رَ", "قٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة زَوْرَقٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "زَيْ",
                branches = listOf(
                    QiraatiPuzzleBranch("تُوْ + نٌ", "زَيْتُوْنٌ", pieces = listOf("زَيْ", "تُوْ", "نٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة زَيْتُوْنٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "مِزْ",
                branches = listOf(
                    QiraatiPuzzleBranch("مَا + رٌ", "مِزْمَارٌ", pieces = listOf("مِزْ", "مَا", "رٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة مِزْمَارٌ"
            )
        ),
        readAloudWords = listOf(
            "زَيْنَبُ تَشْرَبُ مَاءً نَظِيْفًا وَتَأْكُلُ زَيْتُوْنًا .",
            "نُحَافِظُ عَلَى صِحَّتِنَا وَبِيْئَتِنَا ."
        ),
        tracingLetter = "ز",
        tracingForms = listOf("ز", "ـز", "زَ", "زُ")
    )

    val unit: QiraatiUnit = QiraatiUnit(
        id = "unit_4_sihati_wa_biati",
        unitNumber = 4,
        unitBadge = "الوحدة الرابعة",
        title = "الوحدة الرابعة: صحتي وبيئتي",
        shortTitle = "صحتي وبيئتي",
        description = "النظافة الشخصية والغذاء الصحي والبيئة الجميلة مع حروف (ح ، ش ، ز) برفقة النحلة المبتسمة",
        themeColor = Color(0xFFEC4899),
        theme = QiraatiUnitTheme(
            primaryColor = Color(0xFFEC4899),
            shadowColor = Color(0xFFBE185D),
            softSurfaceColor = Color(0xFFFDF2F8),
            badgeColor = Color(0xFF06B6D4),
            gradientStart = Color(0xFFEC4899),
            gradientEnd = Color(0xFF06B6D4)
        ),
        storySection = storySection,
        letterLessons = listOf(
            haLesson,
            sheenLesson,
            zayLesson
        ),
        isAvailable = true,
        comingSoonLabel = null,
        plannedLetters = listOf("ح", "ش", "ز"),
        coverArtKey = QiraatiStoryArtKey.SIHATI_HYGIENE_TOOLS,
        unitChant = null,
        mascotStyle = QiraatiMascotStyle.BEE
    )
}
