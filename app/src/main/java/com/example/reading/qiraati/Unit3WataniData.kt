package com.example.reading.qiraati

import androidx.compose.ui.graphics.Color

/**
 * بيانات الوحدة الثالثة ("وَطَنِي") الكاملة والدقيقة من كتاب
 * "قراءتي الجديدة — الصف الأول الابتدائي" (المنهج العراقي التفاعلي).
 *
 * تحتوي الوحدة على:
 * - قسم الاستماع والمحادثة (أنا أتحدث — وَطَنِي) بمشاهد خريطة العراق وعلمه والنخيل والعيد الوطني.
 * - الدروس السبعة كاملةً بالتشكيل الدقيق:
 *   1. حرف الواو غير المدية (و)
 *   2. حرف الياء غير المدية (ي)
 *   3. حرف القاف (ق ، قـ ، ـقـ)
 *   4. حرف اللام (ل ، لـ)
 *   5. حرف الطاء (ط)
 *   6. حرف العين (ع ، عـ ، ـعـ ، ـع)
 *   7. تنوين الفتح (ـً ، اً)
 * - نشيد الوحدة الختامي ("نَحْنُ أَطْفَالُ الْعِرَاقْ").
 */
object Unit3WataniData {

    private val wataniTheme = QiraatiUnitTheme(
        primaryColor = Color(0xFF0284C7),
        shadowColor = Color(0xFF0369A1),
        softSurfaceColor = Color(0xFFE0F2FE),
        badgeColor = Color(0xFFF59E0B),
        accentHighlightRed = Color(0xFFE11D48),
        gradientStart = Color(0xFF0284C7),
        gradientEnd = Color(0xFF0EA5E9)
    )

    // =========================================================================
    // قسم الاستماع والمحادثة: "وَطَنِي — أنا أتحدث"
    // =========================================================================
    private val storySection = UnitStorySection(
        id = "qiraati_u3_story_watani",
        headerLabel = "الاستماع والمحادثة",
        title = "وَطَنِي — أَنَا أَتَحَدَّثُ",
        listeningScene = QiraatiStoryScene(
            id = "u3_scene_listen",
            sceneNumber = 0,
            title = "خَرِيْطَةُ وَطَنِي الْعِرَاقِ",
            audioDescription = "المُعَلِّمُ وَالتَّلَامِيْذُ فِي الصَّفِّ يُشِيْرُوْنَ بِفَخْرٍ إِلَى خَرِيْطَةِ الْعِرَاقِ وَعَلَمِهِ الْمَرْسُوْمِ عَلَى السَّبُّوْرَةِ.",
            narrationText = "جَلَسَ التَّلَامِيْذُ فِي الصَّفِّ يَسْتَمِعُوْنَ إِلَى مُعَلِّمِهِمْ وَهُوَ يُشِيْرُ إِلَى خَرِيْطَةِ الْعِرَاقِ وَعَلَمِهِ الْمَرْفُوْعِ عَلَى السَّبُّوْرَةِ وَيَقُوْلُ: هَذَا وَطَنُنَا الْحَبِيْبُ الْعِرَاقُ.",
            artKey = QiraatiStoryArtKey.WATANI_CLASSROOM_MAP_FLAG,
            isListeningMainScene = true
        ),
        conversationScenes = listOf(
            QiraatiStoryScene(
                id = "u3_scene_1",
                sceneNumber = 1,
                title = "الِاحْتِفَالُ بِالْعِيْدِ الْوَطَنِيِّ",
                audioDescription = "تَلَامِيْذُ الصَّفِّ الْأَوَّلِ يَحْتَفِلُوْنَ بِالْعِيْدِ الْوَطَنِيِّ دَاخِلَ الصَّفِّ بِالْبَالُوْنَاتِ الْمُلَوَّنَةِ وَالصُّوَرِ الْجَمِيْلَةِ.",
                narrationText = "زَيَّنَ التَّلَامِيْذُ صَفَّهُمْ بِالْبَالُوْنَاتِ وَصُوَرِ الْوَطَنِ، وَاحْتَفَلُوْا مَعًا بِالْعِيْدِ الْوَطَنِيِّ بِفَرَحٍ وَسُرُوْرٍ.",
                artKey = QiraatiStoryArtKey.WATANI_NATIONAL_DAY_PARTY
            ),
            QiraatiStoryScene(
                id = "u3_scene_2",
                sceneNumber = 2,
                title = "نَخِيْلُ الْعِرَاقِ الْبَاسِقُ",
                audioDescription = "ثَلَاثُ نَخَلَاتٍ عِرَاقِيَّاتٍ بَاسِقَاتٍ خَضْرَاءَ تَتَمَايَلُ سَعَفَاتُهَا تَحْتَ الشَّمْسِ الذَّهَبِيَّةِ.",
                narrationText = "يَتَمَيَّزُ وَطَنُنَا الْعِرَاقُ بِنَخِيْلِهِ الْبَاسِقِ الْجَمِيْلِ الَّذِيْ يُعْطِيْنَا التَّمْرَ اللَّذِيْذَ وَيُزَيِّنُ بِسَاتِيْنَ بِلَادِنَا.",
                artKey = QiraatiStoryArtKey.WATANI_THREE_IRAQI_PALMS
            ),
            QiraatiStoryScene(
                id = "u3_scene_3",
                sceneNumber = 3,
                title = "عَلَمُ وَخَرِيْطَةُ الْعِرَاقِ",
                audioDescription = "خَرِيْطَةُ الْعِرَاقِ الْحَبِيْبِ يَلْتَفُّ حَوْلَهَا الْعَلَمُ الْعِرَاقِيُّ بِأَلْوَانِهِ الْأَحْمَرِ وَالْأَبْيَضِ وَالْأَسْوَدِ وَالْأَخْضَرِ.",
                narrationText = "نُحِبُّ وَطَنَنَا الْعِرَاقَ وَنَحْتَرِمُ عَلَمَهُ الْمَرْفُوْعَ، وَنَعْمَلُ بِنَشَاطٍ وَاجْتِهَادٍ لِنَبْنِيَ بِلَادَنَا الْجَمِيْلَةَ.",
                artKey = QiraatiStoryArtKey.WATANI_IRAQ_MAP_AND_FLAG
            )
        ),
        interactiveStoryLines = listOf(
            "أَنَا أُحِبُّ وَطَنِي الْعِرَاقَ الْجَمِيْلَ.",
            "يُشِيْرُ الْمُعَلِّمُ إِلَى خَرِيْطَةِ الْعِرَاقِ وَعَلَمِهِ فِي الصَّفِّ.",
            "نَحْتَفِلُ مَعَ أَصْدِقَائِنَا بِالْعِيْدِ الْوَطَنِيِّ بِالْبَالُوْنَاتِ وَالْأَعْلَامِ.",
            "فِي وَطَنِنَا نَخِيْلٌ بَاسِقٌ وَبَسَاتِيْنُ خَضْرَاءُ وَمَدَارِسُ نَظِيْفَةٌ."
        ),
        iSpeakQuestions = listOf(
            "أصف ما أراه في كل صورة.",
            "أكوّن قصة قصيرة من الصور الثلاث.",
            "ما اسم المنطقة التي تسكنها؟ وما الذي يميزها؟",
            "ما واجبنا تجاه وطننا العراق؟"
        )
    )

    // =========================================================================
    // الدرس 1: حرف الواو غير المدية (و • غير المدية)
    // =========================================================================
    private val wawLesson = QiraatiLetterLesson(
        id = "qiraati_u3_l1_waw",
        unitNumber = 3,
        lessonOrder = 1,
        letterName = "حرف الواو غير المدية (و)",
        targetLetter = "و",
        cloudForms = listOf("و", "غير المدية"),
        accentColor = Color(0xFF0EA5E9),
        shadowColor = Color(0xFF0369A1),
        introWords = listOf(
            QiraatiIntroWord("وَرْدَةٌ", 'و', LetterPosition.INITIAL, QiraatiWordArtKey.ROSE),
            QiraatiIntroWord("وُجُوْهٌ", 'و', LetterPosition.INITIAL, QiraatiWordArtKey.FACES),
            QiraatiIntroWord("وِسَادَةٌ", 'و', LetterPosition.INITIAL, QiraatiWordArtKey.PILLOW),
            QiraatiIntroWord("دَلْوٌ", 'و', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.BUCKET),
            QiraatiIntroWord("مَوْجَةٌ", 'و', LetterPosition.MEDIAL, QiraatiWordArtKey.WAVE),
            QiraatiIntroWord("فَوَاكِهٌ", 'و', LetterPosition.MEDIAL, QiraatiWordArtKey.FRUITS),
            QiraatiIntroWord("طَاوُوْسٌ", 'و', LetterPosition.MEDIAL, QiraatiWordArtKey.PEACOCK),
            QiraatiIntroWord("تَلْوِيْنٌ", 'و', LetterPosition.MEDIAL, QiraatiWordArtKey.COLORING)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("وَ", "وَا", "فتحة قصيرة", "مد بالألف", "وَ", "وَا"),
            QiraatiVowelPair("وُ", "وُوْ", "ضمة قصيرة", "مد بالواو", "وُ", "وُوْ"),
            QiraatiVowelPair("وِ", "وِيْ", "كسرة قصيرة", "مد بالياء", "وِ", "وِيْ")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("وَجْهٌ", false, "مد قصير (وَ)", "وَ", QiraatiWordArtKey.FACES),
            QiraatiVowelWord("وُرُوْدٌ", false, "مد قصير (وُ)", "وُ", QiraatiWordArtKey.ROSE),
            QiraatiVowelWord("وِعَاءٌ", false, "مد قصير (وِ)", "وِ", QiraatiWordArtKey.BUCKET),
            QiraatiVowelWord("دَوَاءٌ", true, "مد طويل (وَا)", "وَا", QiraatiWordArtKey.MEDICINE),
            QiraatiVowelWord("طَاوُوْسٌ", true, "مد طويل (وُوْ)", "وُوْ", QiraatiWordArtKey.PEACOCK),
            QiraatiVowelWord("تَلْوِيْنٌ", true, "مد طويل (وِيْ)", "وِيْ", QiraatiWordArtKey.COLORING)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("قَوْسٌ", "قَوْ", "سٌ", QiraatiWordArtKey.BOW),
            QiraatiSukoonItem("خَوْخٌ", "خَوْ", "خٌ", QiraatiWordArtKey.PEACH),
            QiraatiSukoonItem("مَوْزٌ", "مَوْ", "زٌ", QiraatiWordArtKey.BANANA)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("وَرْدَةٌ", listOf("وَرْ", "دَ", "ةٌ")),
            QiraatiAnalysisItem("وِسَادَةٌ", listOf("وِ", "سَا", "دَ", "ةٌ")),
            QiraatiAnalysisItem("وَدُوْدٌ", listOf("وَ", "دُوْ", "دٌ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "وَ",
                branches = listOf(
                    QiraatiPuzzleBranch("سَـ + نُ", "وَسَنُ", pieces = listOf("وَ", "سَـ", "نُ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة وَسَنُ"
            ),
            QiraatiPuzzleItem(
                basePiece = "وِ",
                branches = listOf(
                    QiraatiPuzzleBranch("دَا + دُ", "وِدَادُ", pieces = listOf("وِ", "دَا", "دُ")),
                    QiraatiPuzzleBranch("سَا + مُ", "وِسَامُ", pieces = listOf("وِ", "سَا", "مُ"))
                ),
                promptHint = "اربط المقاطع مع (وِ) لتكوين وِدَادُ وَوِسَامُ"
            )
        ),
        readAloudWords = listOf(
            "وِسَامٌ وَلَدٌ ، وَسَنُ بِنْتٌ",
            "وِسَامٌ مِنْ مَيْسَانَ",
            "وَسَنُ مِنْ نِيْنَوَى"
        ),
        tracingLetter = "و",
        tracingForms = listOf("و", "وَ", "وُ", "وِ")
    )

    // =========================================================================
    // الدرس 2: حرف الياء غير المدية (ي • غير المدية)
    // =========================================================================
    private val yaLesson = QiraatiLetterLesson(
        id = "qiraati_u3_l2_ya",
        unitNumber = 3,
        lessonOrder = 2,
        letterName = "حرف الياء غير المدية (ي)",
        targetLetter = "ي",
        cloudForms = listOf("ي", "غير المدية"),
        accentColor = Color(0xFF16A34A),
        shadowColor = Color(0xFF15803D),
        introWords = listOf(
            QiraatiIntroWord("يَقْطِيْنٌ", 'ي', LetterPosition.INITIAL, QiraatiWordArtKey.PUMPKIN),
            QiraatiIntroWord("يُصَلِّي", 'ي', LetterPosition.INITIAL, QiraatiWordArtKey.PRAYING),
            QiraatiIntroWord("يَدٌ", 'ي', LetterPosition.INITIAL, QiraatiWordArtKey.HAND),
            QiraatiIntroWord("نَاي", 'ي', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.FLUTE),
            QiraatiIntroWord("لَيْمُوْنٌ", 'ي', LetterPosition.MEDIAL, QiraatiWordArtKey.LEMON),
            QiraatiIntroWord("خِيَارٌ", 'ي', LetterPosition.MEDIAL, QiraatiWordArtKey.CUCUMBER),
            QiraatiIntroWord("طُيُوْرٌ", 'ي', LetterPosition.MEDIAL, QiraatiWordArtKey.BIRDS),
            QiraatiIntroWord("مَلَايِيْنٌ", 'ي', LetterPosition.MEDIAL, QiraatiWordArtKey.MILLIONS)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("يَـ", "يَا", "فتحة قصيرة", "مد بالألف", "يَ", "يَا"),
            QiraatiVowelPair("يُـ", "يُوْ", "ضمة قصيرة", "مد بالواو", "يُ", "يُوْ"),
            QiraatiVowelPair("يِـ", "يِيْ", "كسرة قصيرة", "مد بالياء", "يِ", "يِيْ")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("يَنْبُوْعٌ", false, "مد قصير (يَـ)", "يَ", QiraatiWordArtKey.WATER_SPRING),
            QiraatiVowelWord("يُصَفِّقُ", false, "مد قصير (يُـ)", "يُ", QiraatiWordArtKey.CLAPPING),
            QiraatiVowelWord("مَصَايِفُ", false, "مد قصير (يِـ)", "يِ", QiraatiWordArtKey.RESORTS),
            QiraatiVowelWord("يَاسَمِيْنُ", true, "مد طويل (يَا)", "يَا", QiraatiWordArtKey.JASMINE),
            QiraatiVowelWord("طُيُوْرٌ", true, "مد طويل (يُوْ)", "يُوْ", QiraatiWordArtKey.BIRDS),
            QiraatiVowelWord("مَلَايِيْنُ", true, "مد طويل (يِيْ)", "يِيْ", QiraatiWordArtKey.MILLIONS)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("بَيْضٌ", "بَيْـ", "ضٌ", QiraatiWordArtKey.EGGS),
            QiraatiSukoonItem("هَيْلٌ", "هَيْـ", "لٌ", QiraatiWordArtKey.CARDAMOM),
            QiraatiSukoonItem("زَيْتٌ", "زَيْـ", "تٌ", QiraatiWordArtKey.OIL)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("يَدٌ", listOf("يَـ", "دٌ")),
            QiraatiAnalysisItem("يَسَارٌ", listOf("يَـ", "سَا", "رٌ")),
            QiraatiAnalysisItem("يَسِيْرُ", listOf("يَـ", "سِيْ", "رُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "مَرْ",
                branches = listOf(
                    QiraatiPuzzleBranch("سَـ + مٌ", "مَرْسَمٌ", pieces = listOf("مَرْ", "سَـ", "مٌ")),
                    QiraatiPuzzleBranch("يَـ + مُ", "مَرْيَمُ", pieces = listOf("مَرْ", "يَـ", "مُ"))
                ),
                promptHint = "اربط المقاطع مع (مَرْ) لتكوين مَرْسَمٌ وَمَرْيَمُ"
            ),
            QiraatiPuzzleItem(
                basePiece = "يُوْ",
                branches = listOf(
                    QiraatiPuzzleBranch("سُـ + فُ", "يُوْسُفُ", pieces = listOf("يُوْ", "سُـ", "فُ")),
                    QiraatiPuzzleBranch("نُـ + سُ", "يُوْنُسُ", pieces = listOf("يُوْ", "نُـ", "سُ"))
                ),
                promptHint = "اربط المقاطع مع (يُوْ) لتكوين يُوْسُفُ وَيُوْنُسُ"
            )
        ),
        readAloudWords = listOf(
            "بَابَا وَيُوْنُسُ يَسِيْرَانِ فِي مَدِيْنَتِنَا",
            "فِي مَدِيْنَتِنَا بُيُوْتٌ وَمَدْرَسَةٌ ."
        ),
        tracingLetter = "ي",
        tracingForms = listOf("ي", "يـ", "ـيـ", "ـي")
    )

    // =========================================================================
    // الدرس 3: حرف القاف (ق ، قـ ، ـقـ)
    // =========================================================================
    private val qafLesson = QiraatiLetterLesson(
        id = "qiraati_u3_l3_qaf",
        unitNumber = 3,
        lessonOrder = 3,
        letterName = "حرف القاف (ق)",
        targetLetter = "ق",
        cloudForms = listOf("ق", "قـ", "ـقـ"),
        accentColor = Color(0xFF8B5CF6),
        shadowColor = Color(0xFF6233C7),
        introWords = listOf(
            QiraatiIntroWord("قَلَمٌ", 'ق', LetterPosition.INITIAL, QiraatiWordArtKey.PEN),
            QiraatiIntroWord("قُبَّعَةٌ", 'ق', LetterPosition.INITIAL, QiraatiWordArtKey.HAT),
            QiraatiIntroWord("قِرْشٌ", 'ق', LetterPosition.INITIAL, QiraatiWordArtKey.SHARK),
            QiraatiIntroWord("بِطْرِيْقٌ", 'ق', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.PENGUIN),
            QiraatiIntroWord("صَقْرٌ", 'ق', LetterPosition.MEDIAL, QiraatiWordArtKey.FALCON),
            QiraatiIntroWord("قَارِبٌ", 'ق', LetterPosition.INITIAL, QiraatiWordArtKey.BOAT),
            QiraatiIntroWord("نُقُوْدٌ", 'ق', LetterPosition.MEDIAL, QiraatiWordArtKey.COINS),
            QiraatiIntroWord("دَقِيْقٌ", 'ق', LetterPosition.MULTIPLE, QiraatiWordArtKey.FLOUR)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("قَـ", "قَا", "فتحة قصيرة", "مد بالألف", "قَ", "قَا"),
            QiraatiVowelPair("قُـ", "قُوْ", "ضمة قصيرة", "مد بالواو", "قُ", "قُوْ"),
            QiraatiVowelPair("قِـ", "قِيْ", "كسرة قصيرة", "مد بالياء", "قِ", "قِيْ")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("بَقَرَةٌ", false, "مد قصير (قَـ)", "قَ", QiraatiWordArtKey.COW),
            QiraatiVowelWord("قُطْنٌ", false, "مد قصير (قُـ)", "قُ", QiraatiWordArtKey.COTTON),
            QiraatiVowelWord("قِصَّةٌ", false, "مد قصير (قِـ)", "قِ", QiraatiWordArtKey.STORY),
            QiraatiVowelWord("بُرْتُقَالٌ", true, "مد طويل (قَا)", "قَا", QiraatiWordArtKey.ORANGE),
            QiraatiVowelWord("نُقُوْدٌ", true, "مد طويل (قُوْ)", "قُوْ", QiraatiWordArtKey.COINS),
            QiraatiVowelWord("حَقِيْبَةٌ", true, "مد طويل (قِيْ)", "قِيْ", QiraatiWordArtKey.BACKPACK)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("صَقْرٌ", "صَقْـ", "رٌ", QiraatiWordArtKey.FALCON),
            QiraatiSukoonItem("حَقْلٌ", "حَقْـ", "لٌ", QiraatiWordArtKey.FIELD),
            QiraatiSukoonItem("عَقْرَبٌ", "عَقْـ", "رَبٌ", QiraatiWordArtKey.SCORPION)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("سَقْفٌ", listOf("سَقْ", "فٌ")),
            QiraatiAnalysisItem("قَرْيَةٌ", listOf("قَرْ", "يَـ", "ةٌ")),
            QiraatiAnalysisItem("مِنْقَارٌ", listOf("مِنْ", "قَا", "رٌ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "نَـ",
                branches = listOf(
                    QiraatiPuzzleBranch("قِيْ + بٌ", "نَقِيْبٌ", pieces = listOf("نَـ", "قِيْ", "بٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة نَقِيْبٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "قَا",
                branches = listOf(
                    QiraatiPuzzleBranch("رِ + بٌ", "قَارِبٌ", pieces = listOf("قَا", "رِ", "بٌ")),
                    QiraatiPuzzleBranch("دِ + مٌ", "قَادِمٌ", pieces = listOf("قَا", "دِ", "مٌ"))
                ),
                promptHint = "اربط المقاطع مع (قَا) لتكوين قَارِبٌ وَقَادِمٌ"
            )
        ),
        readAloudWords = listOf(
            "قُرْبَ مَدِيْنَتِنَا قَرْيَةُ رَوَانَ",
            "فِي قَرْيَةِ رَوَانَ بُسْتَانُ فَاتِنَ وَسُوْقٌ ."
        ),
        tracingLetter = "ق",
        tracingForms = listOf("ق", "قـ", "ـقـ")
    )

    // =========================================================================
    // الدرس 4: حرف اللام (ل ، لـ)
    // =========================================================================
    private val lamLesson = QiraatiLetterLesson(
        id = "qiraati_u3_l4_lam",
        unitNumber = 3,
        lessonOrder = 4,
        letterName = "حرف اللام (ل)",
        targetLetter = "ل",
        cloudForms = listOf("ل", "لـ"),
        accentColor = Color(0xFFFF7A59),
        shadowColor = Color(0xFFD14B2B),
        introWords = listOf(
            QiraatiIntroWord("لَحْمٌ", 'ل', LetterPosition.INITIAL, QiraatiWordArtKey.MEAT),
            QiraatiIntroWord("لُعْبَةٌ", 'ل', LetterPosition.INITIAL, QiraatiWordArtKey.TOY),
            QiraatiIntroWord("لِسَانٌ", 'ل', LetterPosition.INITIAL, QiraatiWordArtKey.TONGUE),
            QiraatiIntroWord("غَزَالٌ", 'ل', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.GAZELLE),
            QiraatiIntroWord("كَلْبٌ", 'ل', LetterPosition.MEDIAL, QiraatiWordArtKey.DOG),
            QiraatiIntroWord("لَاعِبٌ", 'ل', LetterPosition.INITIAL, QiraatiWordArtKey.PLAYER),
            QiraatiIntroWord("قُلُوْبٌ", 'ل', LetterPosition.MEDIAL, QiraatiWordArtKey.HEARTS),
            QiraatiIntroWord("حَلِيْبٌ", 'ل', LetterPosition.MEDIAL, QiraatiWordArtKey.MILK)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("لَـ", "لَا", "فتحة قصيرة", "مد بالألف", "لَ", "لَا"),
            QiraatiVowelPair("لُـ", "لُوْ", "ضمة قصيرة", "مد بالواو", "لُ", "لُوْ"),
            QiraatiVowelPair("لِـ", "لِيْ", "كسرة قصيرة", "مد بالياء", "لِ", "لِيْ")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("قَلَمٌ", false, "مد قصير (لَـ)", "لَ", QiraatiWordArtKey.PEN),
            QiraatiVowelWord("لُعْبَةٌ", false, "مد قصير (لُـ)", "لُ", QiraatiWordArtKey.TOY),
            QiraatiVowelWord("عَالِمٌ", false, "مد قصير (لِـ)", "لِ", QiraatiWordArtKey.SCIENTIST),
            QiraatiVowelWord("ثَلَاثَةٌ", true, "مد طويل (لَا)", "لَا", QiraatiWordArtKey.THREE),
            QiraatiVowelWord("بَالُوْنٌ", true, "مد طويل (لُوْ)", "لُوْ", QiraatiWordArtKey.BALLOON),
            QiraatiVowelWord("حَلِيْبٌ", true, "مد طويل (لِيْ)", "لِيْ", QiraatiWordArtKey.MILK)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("مَلْعَبٌ", "مَلْـ", "عَبٌ", QiraatiWordArtKey.PLAYGROUND),
            QiraatiSukoonItem("دَلْوٌ", "دَلْـ", "وٌ", QiraatiWordArtKey.BUCKET),
            QiraatiSukoonItem("فُلْفُلٌ", "فُلْـ", "فُلٌ", QiraatiWordArtKey.PEPPER)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("رَمْلٌ", listOf("رَمْ", "لٌ")),
            QiraatiAnalysisItem("نَمْلَةٌ", listOf("نَمْ", "لَـ", "ةٌ")),
            QiraatiAnalysisItem("بَلَدِي", listOf("بَـ", "لَـ", "دِي"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "قَا",
                branches = listOf(
                    QiraatiPuzzleBranch("لَ", "قَالَ", pieces = listOf("قَا", "لَ"))
                ),
                promptHint = "اربط القطعتين لتكوين كلمة قَالَ"
            ),
            QiraatiPuzzleItem(
                basePiece = "بِـ",
                branches = listOf(
                    QiraatiPuzzleBranch("لَا + دِي", "بِلَادِي", pieces = listOf("بِـ", "لَا", "دِي"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة بِلَادِي"
            ),
            QiraatiPuzzleItem(
                basePiece = "مَـ",
                branches = listOf(
                    QiraatiPuzzleBranch("لَا + بِـ + سُ", "مَلَابِسُ", pieces = listOf("مَـ", "لَا", "بِـ", "سُ"))
                ),
                promptHint = "اربط المقاطع الأربعة معاً لتكوين كلمة مَلَابِسُ"
            )
        ),
        readAloudWords = listOf(
            "قَالَتْ نَوَالُ : فِي بَلَدِي بَيْتِي .",
            "قَالَتْ فَاتِنُ : فِيْ بَلَدِي بُسْتَانِي ."
        ),
        tracingLetter = "ل",
        tracingForms = listOf("ل", "لـ")
    )

    // =========================================================================
    // الدرس 5: حرف الطاء (ط)
    // =========================================================================
    private val taaLesson = QiraatiLetterLesson(
        id = "qiraati_u3_l5_taa",
        unitNumber = 3,
        lessonOrder = 5,
        letterName = "حرف الطاء (ط)",
        targetLetter = "ط",
        cloudForms = listOf("ط", "طـ"),
        accentColor = Color(0xFFEC4899),
        shadowColor = Color(0xFFBE185D),
        introWords = listOf(
            QiraatiIntroWord("طَبِيْبٌ", 'ط', LetterPosition.INITIAL, QiraatiWordArtKey.DOCTOR),
            QiraatiIntroWord("طُيُوْرٌ", 'ط', LetterPosition.INITIAL, QiraatiWordArtKey.BIRDS),
            QiraatiIntroWord("طِفْلٌ", 'ط', LetterPosition.INITIAL, QiraatiWordArtKey.CHILD),
            QiraatiIntroWord("خَيَّاطٌ", 'ط', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.TAILOR),
            QiraatiIntroWord("بِطْرِيْقٌ", 'ط', LetterPosition.MEDIAL, QiraatiWordArtKey.PENGUIN),
            QiraatiIntroWord("قُبْطَانٌ", 'ط', LetterPosition.MEDIAL, QiraatiWordArtKey.CAPTAIN),
            QiraatiIntroWord("عُطُوْرٌ", 'ط', LetterPosition.MEDIAL, QiraatiWordArtKey.PERFUMES),
            QiraatiIntroWord("يَقْطِيْنٌ", 'ط', LetterPosition.MEDIAL, QiraatiWordArtKey.PUMPKIN)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("طَ", "طَا", "فتحة قصيرة", "مد بالألف", "طَ", "طَا"),
            QiraatiVowelPair("طُ", "طُوْ", "ضمة قصيرة", "مد بالواو", "طُ", "طُوْ"),
            QiraatiVowelPair("طِ", "طِيْ", "كسرة قصيرة", "مد بالياء", "طِ", "طِيْ")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("طَبْلٌ", false, "مد قصير (طَ)", "طَ", QiraatiWordArtKey.DRUM),
            QiraatiVowelWord("طُيُوْرٌ", false, "مد قصير (طُ)", "طُ", QiraatiWordArtKey.BIRDS),
            QiraatiVowelWord("طِفْلٌ", false, "مد قصير (طِ)", "طِ", QiraatiWordArtKey.CHILD),
            QiraatiVowelWord("قِطَارٌ", true, "مد طويل (طَا)", "طَا", QiraatiWordArtKey.TRAIN),
            QiraatiVowelWord("عُطُوْرٌ", true, "مد طويل (طُوْ)", "طُوْ", QiraatiWordArtKey.PERFUMES),
            QiraatiVowelWord("مُسْتَطِيْلٌ", true, "مد طويل (طِيْ)", "طِيْ", QiraatiWordArtKey.RECTANGLE)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("مِطْرَقَةٌ", "مِطْ", "رَقَةٌ", QiraatiWordArtKey.HAMMER),
            QiraatiSukoonItem("بِطْرِيْقٌ", "بِطْ", "رِيْقٌ", QiraatiWordArtKey.PENGUIN),
            QiraatiSukoonItem("مَطْبَخٌ", "مَطْ", "بَخٌ", QiraatiWordArtKey.KITCHEN)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("قُبْطَانٌ", listOf("قُبْ", "طَا", "نٌ")),
            QiraatiAnalysisItem("طَرِيْقٌ", listOf("طَ", "رِيْ", "قٌ")),
            QiraatiAnalysisItem("بِسَاطٌ", listOf("بِـ", "سَا", "طٌ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "فَـ",
                branches = listOf(
                    QiraatiPuzzleBranch("طُوْ + رٌ", "فَطُوْرٌ", pieces = listOf("فَـ", "طُوْ", "رٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة فَطُوْرٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "طَا",
                branches = listOf(
                    QiraatiPuzzleBranch("لِـ + بٌ", "طَالِبٌ", pieces = listOf("طَا", "لِـ", "بٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة طَالِبٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "قِـ",
                branches = listOf(
                    QiraatiPuzzleBranch("طَا + رٌ", "قِطَارٌ", pieces = listOf("قِـ", "طَا", "رٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة قِطَارٌ"
            )
        ),
        readAloudWords = listOf(
            "فَاطِمَةُ فِي مَدْرَسَتِي",
            "تَقُوْلُ فَاطِمَةُ : رَسَمْتُ بِقَلَمِي وَطَنِي ، وَطَنِي بَيْتِي ."
        ),
        tracingLetter = "ط",
        tracingForms = listOf("ط", "طَ", "طُ", "طِ")
    )

    // =========================================================================
    // الدرس 6: حرف العين (ع ، عـ ، ـعـ ، ـع)
    // =========================================================================
    private val ainLesson = QiraatiLetterLesson(
        id = "qiraati_u3_l6_ain",
        unitNumber = 3,
        lessonOrder = 6,
        letterName = "حرف العين (ع)",
        targetLetter = "ع",
        cloudForms = listOf("ع", "عـ", "ـعـ", "ـع"),
        accentColor = Color(0xFF0D9488),
        shadowColor = Color(0xFF0F766E),
        introWords = listOf(
            QiraatiIntroWord("عَلَمٌ", 'ع', LetterPosition.INITIAL, QiraatiWordArtKey.IRAQ_FLAG),
            QiraatiIntroWord("عُصْفُوْرٌ", 'ع', LetterPosition.INITIAL, QiraatiWordArtKey.SPARROW),
            QiraatiIntroWord("عِظَامٌ", 'ع', LetterPosition.INITIAL, QiraatiWordArtKey.BONES),
            QiraatiIntroWord("إِصْبَعٌ", 'ع', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.FINGER),
            QiraatiIntroWord("شُعْلَةٌ", 'ع', LetterPosition.MEDIAL, QiraatiWordArtKey.TORCH),
            QiraatiIntroWord("عَالِمٌ", 'ع', LetterPosition.INITIAL, QiraatiWordArtKey.SCIENTIST),
            QiraatiIntroWord("عُوْدٌ", 'ع', LetterPosition.INITIAL, QiraatiWordArtKey.OUD),
            QiraatiIntroWord("عِيْدَانٌ", 'ع', LetterPosition.INITIAL, QiraatiWordArtKey.STICKS)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("عَـ", "عَا", "فتحة قصيرة", "مد بالألف", "عَ", "عَا"),
            QiraatiVowelPair("عُـ", "عُوْ", "ضمة قصيرة", "مد بالواو", "عُ", "عُوْ"),
            QiraatiVowelPair("عِـ", "عِيْ", "كسرة قصيرة", "مد بالياء", "عِ", "عِيْ")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("عَلَمٌ", false, "مد قصير (عَـ)", "عَ", QiraatiWordArtKey.IRAQ_FLAG),
            QiraatiVowelWord("عُصْفُوْرٌ", false, "مد قصير (عُـ)", "عُ", QiraatiWordArtKey.SPARROW),
            QiraatiVowelWord("عِنَبٌ", false, "مد قصير (عِـ)", "عِ", QiraatiWordArtKey.GRAPES),
            QiraatiVowelWord("نَعَامَةٌ", true, "مد طويل (عَا)", "عَا", QiraatiWordArtKey.OSTRICH),
            QiraatiVowelWord("عُوْدٌ", true, "مد طويل (عُوْ)", "عُوْ", QiraatiWordArtKey.OUD),
            QiraatiVowelWord("عِيْدَانٌ", true, "مد طويل (عِيْ)", "عِيْ", QiraatiWordArtKey.STICKS)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("ثَعْلَبٌ", "ثَعْـ", "لَبٌ", QiraatiWordArtKey.FOX),
            QiraatiSukoonItem("نَعْنَاعٌ", "نَعْـ", "نَاعٌ", QiraatiWordArtKey.MINT),
            QiraatiSukoonItem("لُعْبَةٌ", "لُعْـ", "بَةٌ", QiraatiWordArtKey.TOY)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("عَلَمٌ", listOf("عَـ", "لَـ", "مٌ")),
            QiraatiAnalysisItem("عِرَاقٌ", listOf("عِـ", "رَا", "قٌ")),
            QiraatiAnalysisItem("نَلْعَبُ", listOf("نَلْـ", "عَـ", "بُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "عَـ",
                branches = listOf(
                    QiraatiPuzzleBranch("سَـ + لٌ", "عَسَلٌ", pieces = listOf("عَـ", "سَـ", "لٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة عَسَلٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "عَا",
                branches = listOf(
                    QiraatiPuzzleBranch("لِـ + مٌ", "عَالِمٌ", pieces = listOf("عَا", "لِـ", "مٌ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة عَالِمٌ"
            ),
            QiraatiPuzzleItem(
                basePiece = "تَـ",
                branches = listOf(
                    QiraatiPuzzleBranch("عُوْ + دُ", "تَعُوْدُ", pieces = listOf("تَـ", "عُوْ", "دُ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة تَعُوْدُ"
            ),
            QiraatiPuzzleItem(
                basePiece = "يَـ",
                branches = listOf(
                    QiraatiPuzzleBranch("عُوْ + دُ", "يَعُوْدُ", pieces = listOf("يَـ", "عُوْ", "دُ"))
                ),
                promptHint = "اربط المقاطع الثلاثة لتكوين كلمة يَعُوْدُ"
            )
        ),
        readAloudWords = listOf(
            "يَرْفَعُ عَامِرٌ عَلَمَ وَطَنِي ،",
            "عَلَمُ وَطَنِي يُرَفْرِفُ فَوْقَ مَدْرَسَتِي ."
        ),
        tracingLetter = "ع",
        tracingForms = listOf("ع", "عـ", "ـعـ", "ـع")
    )

    // =========================================================================
    // الدرس 7: تنوين الفتح (ـً ، اً)
    // =========================================================================
    private val tanweenFathLesson = QiraatiLetterLesson(
        id = "qiraati_u3_l7_tanween_fath",
        unitNumber = 3,
        lessonOrder = 7,
        letterName = "تنوين الفتح (ـً ، اً)",
        targetLetter = "ـً",
        cloudForms = listOf("ـً", "اً"),
        accentColor = Color(0xFFF59E0B),
        shadowColor = Color(0xFFB45309),
        introWords = listOf(
            QiraatiIntroWord("عَلَمًا", '\u064B', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.IRAQ_FLAG),
            QiraatiIntroWord("بُوْمَةً", '\u064B', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.OWL),
            QiraatiIntroWord("بَابًا", '\u064B', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.DOOR),
            QiraatiIntroWord("وَرَقَةً", '\u064B', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.LEAF)
        ),
        vowelPairs = emptyList(),
        vowelWords = emptyList(),
        sukoonItems = emptyList(),
        tanweenListenWords = listOf(
            QiraatiIntroWord(
                word = "قَطَفَتْ فَاطِمَةُ وَرْدَةً .",
                targetChar = '\u064B',
                position = LetterPosition.FINAL_CONNECTED,
                artKey = QiraatiWordArtKey.ROSE,
                spokenText = "قَطَفَتْ فَاطِمَةُ وَرْدَةً"
            ),
            QiraatiIntroWord(
                word = "رَفَعَ عَامِرٌ عَلَمًا .",
                targetChar = '\u064B',
                position = LetterPosition.FINAL_CONNECTED,
                artKey = QiraatiWordArtKey.IRAQ_FLAG,
                spokenText = "رَفَعَ عَامِرٌ عَلَمًا"
            )
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("قَلَمًا", listOf("قَـ", "لَـ", "مًا")),
            QiraatiAnalysisItem("وَرَقَةً", listOf("وَ", "رَ", "قَـ", "ةً"))
        ),
        puzzleItems = emptyList(),
        sentenceFillItems = listOf(
            QiraatiSentenceFillItem(
                syllables = listOf("بَيْـ", "تًا"),
                targetWord = "بَيْتًا",
                sentenceWithBlank = "بَنَى بَابَا .........",
                completedSentence = "بَنَى بَابَا بَيْتًا",
                artKey = QiraatiWordArtKey.HOUSE
            ),
            QiraatiSentenceFillItem(
                syllables = listOf("مَدْ", "رَ", "سَـ", "ةً"),
                targetWord = "مَدْرَسَةً",
                sentenceWithBlank = "رَسَمَ سِنَانُ .........",
                completedSentence = "رَسَمَ سِنَانُ مَدْرَسَةً",
                artKey = QiraatiWordArtKey.SCHOOL
            )
        ),
        readAloudWords = listOf(
            "رَفَعَ عَامِرٌ عَلَمًا فَوْقَ مَدْرَسَتِنَا",
            "يَبْقَى عَلَمُ وَطَنِنَا مَرْفُوْعًا وَعَالِيًا ."
        ),
        tracingLetter = "اً",
        tracingForms = listOf("اً", "ةً", "مًا", "بًا")
    )

    // =========================================================================
    // نشيد الوحدة الختامي: "نَحْنُ أَطْفَالُ الْعِرَاقْ"
    // =========================================================================
    private val wataniChant = QiraatiUnitChant(
        id = "qiraati_u3_chant_watani",
        title = "نَحْنُ أَطْفَالُ الْعِرَاقْ",
        subtitle = "استمع لنشيد وطننا العراق وردّده بحبٍّ وفخر",
        verses = listOf(
            "نَحْنُ أَطْفَالُ الْعِرَاقْ • نَهْتِفُ يَحْيَى الْعِرَاقْ",
            "وَطَنِي وَالعَيْشُ فِيْه • طَيِّبٌ حُلْوُ الْمَذَاقْ",
            "نَحْنُ أَطْفَالُ الْعِرَاقْ • نَحْنُ أَطْفَالُ الْعِرَاقْ"
        )
    )

    val unit: QiraatiUnit = QiraatiUnit(
        id = "unit_3_watani",
        unitNumber = 3,
        unitBadge = "الوحدة الثالثة",
        title = "الوحدة الثالثة: وطني",
        shortTitle = "وطني",
        description = "قصة وطني العراق مع حروف الواو والياء والقاف واللام والطاء والعين وتنوين الفتح ونشيد نحن أطفال العراق",
        themeColor = wataniTheme.primaryColor,
        theme = wataniTheme,
        storySection = storySection,
        letterLessons = listOf(
            wawLesson,
            yaLesson,
            qafLesson,
            lamLesson,
            taaLesson,
            ainLesson,
            tanweenFathLesson
        ),
        isAvailable = true,
        coverArtKey = QiraatiStoryArtKey.WATANI_CLASSROOM_MAP_FLAG,
        unitChant = wataniChant,
        mascotStyle = QiraatiMascotStyle.BLUE_PEN
    )
}
