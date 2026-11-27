package com.example.reading.qiraati

import androidx.compose.ui.graphics.Color

/**
 * بيانات الوحدة الثانية الكاملة: "الوحدة الثانية: مدرستي"
 * من كتاب قراءتي الجديدة — الصف الأول الابتدائي (المنهج العراقي الرسمي)
 * مضبوطة بالتشكيل الكامل وشاملة لدروس: ر، س، ن، ت، التاء المربوطة (ة ، ـة)، تنوين الضم (ـٌ)، ونشيد "مَدْرَسَتِي".
 */
object Unit2MadrasatiData {

    private val unitTheme = QiraatiUnitTheme(
        primaryColor = Color(0xFF16A34A),
        shadowColor = Color(0xFF15803D),
        softSurfaceColor = Color(0xFFF0FDF4),
        badgeColor = Color(0xFF38BDF8),
        accentHighlightRed = Color(0xFFE11D48),
        gradientStart = Color(0xFF84CC16),
        gradientEnd = Color(0xFF0EA5E9)
    )

    // =========================================================================
    // الاستماع والمحادثة ("مدرستي — أنا أتحدث")
    // =========================================================================
    val storySection: UnitStorySection = UnitStorySection(
        id = "unit2_story_madrasati",
        headerLabel = "الدرس الأول والثاني • الاستماع والمحادثة",
        title = "مَدْرَسَتِي الْجَمِيلَةُ — أَنَا أَتَحَدَّثُ",
        listeningScene = QiraatiStoryScene(
            id = "madrasati_scene_listening",
            sceneNumber = 0,
            title = "مشهد الاستماع: مَدْرَسَتِي وَالْعَلَمُ الْعِرَاقِيُّ",
            audioDescription = "نرى في الصورة مبنى المدرسة الجميل النظيف، يرفرف فوقه العلم العراقي بألوانه الزاهية، والتلاميذ يدخلون بابتسامة ونشاط في الصباح الباكر.",
            narrationText = "مَدْرَسَتِي جَمِيلَةٌ وَنَظِيفَةٌ، يَرْفْرِفُ فَوْقَهَا الْعَلَمُ الْعِرَاقِيُّ عَالِياً، وَيَدْخُلُهَا التَّلَامِيذُ بِفَرَحٍ وَنَشَاطٍ كُلَّ صَبَاحٍ.",
            artKey = QiraatiStoryArtKey.MADRASATI_SCHOOL_FLAG,
            isListeningMainScene = true
        ),
        conversationScenes = listOf(
            QiraatiStoryScene(
                id = "madrasati_scene_1_gate",
                sceneNumber = 1,
                title = "الصورة الأولى: الْوُصُولُ إِلَى بَوَّابَةِ الْمَدْرَسَةِ",
                audioDescription = "يصل التلاميذ إلى بوابة المدرسة بابتسامة مشرقة وحقائبهم المدرسية المنظمة، ويحيّي بعضهم بعضاً بأدب ومحبة.",
                narrationText = "يَصِلُ التَّلَامِيذُ إِلَى بَوَّابَةِ الْمَدْرَسَةِ بِابْتِسَامَةٍ جَمِيلَةٍ وَنَشَاطٍ كَبِيرٍ لِبِدَايَةِ يَوْمٍ دِرَاسِيٍّ مُمْتِعٍ.",
                artKey = QiraatiStoryArtKey.MADRASATI_GATE_ARRIVAL
            ),
            QiraatiStoryScene(
                id = "madrasati_scene_2_yard",
                sceneNumber = 2,
                title = "الصورة الثانية: اصْطِفَافُ التَّلَامِيذِ فِي سَاحَةِ الْمَدْرَسَةِ",
                audioDescription = "يصطف التلاميذ بنظام جميل في ساحة المدرسة مع المعلمة لتحية العلم والاستماع إلى التوجيهات الصباحية.",
                narrationText = "يَصْطَفُّ التَّلَامِيذُ بِنِظَامٍ فِي سَاحَةِ الْمَدْرَسَةِ مَعَ الْمُعَلِّمَةِ؛ نُحَيِّي الْعَلَمَ وَنَتَعَلَّمُ النِّظَامَ وَالتَّعَاوُنَ.",
                artKey = QiraatiStoryArtKey.MADRASATI_YARD_LINEUP
            ),
            QiraatiStoryScene(
                id = "madrasati_scene_3_classroom",
                sceneNumber = 3,
                title = "الصورة الثالثة: دَاخِلَ الْغُرْفَةِ الصَّفِّيَّةِ",
                audioDescription = "تشرح المعلمة الدرس بلطف ووضوح على السبورة داخل الغرفة الصفية، والتلاميذ يستمعون باهتمام ويرفعون أيديهم بأدب للمشاركة.",
                narrationText = "تَشْرَحُ الْمُعَلِّمَةُ الدَّرْسَ لِلتَّلَامِيذِ دَاخِلَ الْغُرْفَةِ الصَّفِّيَّةِ، نَسْتَمِعُ بِانْتِبَاهٍ وَنَحْتَرِمُ مُعَلِّمَتَنَا وَزُمَلَاءَنَا.",
                artKey = QiraatiStoryArtKey.MADRASATI_CLASSROOM_LESSON
            )
        ),
        interactiveStoryLines = listOf(
            "فِي الصَّبَاحِ أَذْهَبُ إِلَى مَدْرَسَتِي بِنَشَاطٍ وَابْتِسَامَةٍ.",
            "نَصْطَفُّ بِنِظَامٍ فِي سَاحَةِ الْمَدْرَسَةِ تَحْتَ الْعَلَمِ الْعِرَاقِيِّ.",
            "دَاخِلَ الصَّفِّ نَسْتَمِعُ لِشَرْحِ الْمُعَلِّمَةِ وَنُشَارِكُ بِأَدَبٍ.",
            "أُحِبُّ مَدْرَسَتِي وَأُحَافِظُ عَلَى نَظَافَتِهَا وَأَتَعَاوَنُ مَعَ زُمَلَائِي."
        ),
        iSpeakQuestions = listOf(
            "أصف ما أشاهده في كل صورة.",
            "لماذا يجتمع التلاميذ في ساحة المدرسة؟",
            "تخيل نفسك في هذه الغرفة الصفية؛ ما الأعمال التي تقوم بها؟ قدّم نصيحة لزميلك تحثه فيها على حسن التعامل مع المعلمة والزملاء.",
            "كوّن قصةً من الصور التي أمامك."
        )
    )

    // =========================================================================
    // الدرس 1 — حرف الراء (ر)
    // =========================================================================
    val raLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit2_lesson_ra",
        unitNumber = 2,
        lessonOrder = 1,
        letterName = "حرف الراء",
        targetLetter = "ر",
        cloudForms = listOf("ر", "ـر"),
        accentColor = Color(0xFF16A34A),
        shadowColor = Color(0xFF15803D),
        introWords = listOf(
            QiraatiIntroWord("رَمْلُ", 'ر', LetterPosition.INITIAL, QiraatiWordArtKey.SAND),
            QiraatiIntroWord("رِجْلُ", 'ر', LetterPosition.INITIAL, QiraatiWordArtKey.FOOT),
            QiraatiIntroWord("جَزَرُ", 'ر', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.CARROT),
            QiraatiIntroWord("قِرْدُ", 'ر', LetterPosition.MEDIAL, QiraatiWordArtKey.MONKEY),
            QiraatiIntroWord("مِبْرَاةُ", 'ر', LetterPosition.MEDIAL, QiraatiWordArtKey.SHARPENER),
            QiraatiIntroWord("خَرُوْفُ", 'ر', LetterPosition.MEDIAL, QiraatiWordArtKey.SHEEP),
            QiraatiIntroWord("سَرِيْرُ", 'ر', LetterPosition.MULTIPLE, QiraatiWordArtKey.BED)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("رَ", "رَا", "فتحة", "مد بالألف"),
            QiraatiVowelPair("رُ", "رُوْ", "ضمة", "مد بالواو"),
            QiraatiVowelPair("رِ", "رِيْ", "كسرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("صَفَّارَةُ", isLongVowel = false, vowelLabel = "مد قصير (فتحة)", highlightedSegment = "رَ", artKey = QiraatiWordArtKey.WHISTLE),
            QiraatiVowelWord("قَارِبُ", isLongVowel = false, vowelLabel = "مد قصير (كسرة)", highlightedSegment = "رِ", artKey = QiraatiWordArtKey.BOAT),
            QiraatiVowelWord("رُزُّ", isLongVowel = false, vowelLabel = "مد قصير (ضمة)", highlightedSegment = "رُ", artKey = QiraatiWordArtKey.RICE),
            QiraatiVowelWord("مِبْرَاةُ", isLongVowel = true, vowelLabel = "مد طويل (ألف)", highlightedSegment = "رَا", artKey = QiraatiWordArtKey.SHARPENER),
            QiraatiVowelWord("خَرُوْفُ", isLongVowel = true, vowelLabel = "مد طويل (واو)", highlightedSegment = "رُوْ", artKey = QiraatiWordArtKey.SHEEP),
            QiraatiVowelWord("رِيْشَةُ", isLongVowel = true, vowelLabel = "مد طويل (ياء)", highlightedSegment = "رِيْ", artKey = QiraatiWordArtKey.FEATHER)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("قِرْدُ", sukoonSyllable = "قِرْ", remainingPart = "دُ", artKey = QiraatiWordArtKey.MONKEY),
            QiraatiSukoonItem("فُرْشَاةُ", sukoonSyllable = "فُرْ", remainingPart = "شَاةُ", artKey = QiraatiWordArtKey.BRUSH),
            QiraatiSukoonItem("بِرْكَةُ", sukoonSyllable = "بِرْ", remainingPart = "كَةُ", artKey = QiraatiWordArtKey.POND)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("رِيْمُ", listOf("رِيْ", "مُ")),
            QiraatiAnalysisItem("رَمَى", listOf("رَ", "مَى")),
            QiraatiAnalysisItem("مَرَامُ", listOf("مَـ", "رَا", "مُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "را",
                branches = listOf(
                    QiraatiPuzzleBranch(
                        attachedPiece = "فِـ + دُ",
                        resultWord = "رَافِدُ",
                        pieces = listOf("را", "فِـ", "دُ")
                    ),
                    QiraatiPuzzleBranch(
                        attachedPiece = "مِي",
                        resultWord = "رَامِي",
                        pieces = listOf("را", "مِي")
                    )
                )
            )
        ),
        readAloudWords = listOf("بَابَا رَامِي", "مَامَا رِيْمُ", "دَادَا مَرَامُ"),
        tracingLetter = "ر",
        tracingForms = listOf("ر", "ـر")
    )

    // =========================================================================
    // الدرس 2 — حرف السين (س ، سـ)
    // =========================================================================
    val seenLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit2_lesson_seen",
        unitNumber = 2,
        lessonOrder = 2,
        letterName = "حرف السين",
        targetLetter = "س",
        cloudForms = listOf("س", "سـ"),
        accentColor = Color(0xFF0EA5E9),
        shadowColor = Color(0xFF0369A1),
        introWords = listOf(
            QiraatiIntroWord("سَلَّةُ", 'س', LetterPosition.INITIAL, QiraatiWordArtKey.BASKET),
            QiraatiIntroWord("سُلَحْفَاةُ", 'س', LetterPosition.INITIAL, QiraatiWordArtKey.TURTLE),
            QiraatiIntroWord("سِتَارَةُ", 'س', LetterPosition.INITIAL, QiraatiWordArtKey.CURTAIN),
            QiraatiIntroWord("شَمْسُ", 'س', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.SUN),
            QiraatiIntroWord("مِسْمَارُ", 'س', LetterPosition.MEDIAL, QiraatiWordArtKey.NAIL),
            QiraatiIntroWord("سَاعَةُ", 'س', LetterPosition.INITIAL, QiraatiWordArtKey.CLOCK),
            QiraatiIntroWord("حَاسُوْبُ", 'س', LetterPosition.MEDIAL, QiraatiWordArtKey.LAPTOP),
            QiraatiIntroWord("تَمَاسِيْحُ", 'س', LetterPosition.MEDIAL, QiraatiWordArtKey.CROCODILE)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("سَـ", "سَا", "فتحة", "مد بالألف"),
            QiraatiVowelPair("سُـ", "سُوْ", "ضمة", "مد بالواو"),
            QiraatiVowelPair("سِـ", "سِيْ", "كسرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("سَبُّوْرَةُ", isLongVowel = false, vowelLabel = "مد قصير (فتحة)", highlightedSegment = "سَ", artKey = QiraatiWordArtKey.BLACKBOARD),
            QiraatiVowelWord("سُكَّرُ", isLongVowel = false, vowelLabel = "مد قصير (ضمة)", highlightedSegment = "سُ", artKey = QiraatiWordArtKey.SUGAR),
            QiraatiVowelWord("سِنْجَابُ", isLongVowel = false, vowelLabel = "مد قصير (كسرة)", highlightedSegment = "سِ", artKey = QiraatiWordArtKey.SQUIRREL),
            QiraatiVowelWord("سَاعَةُ", isLongVowel = true, vowelLabel = "مد طويل (ألف)", highlightedSegment = "سَا", artKey = QiraatiWordArtKey.CLOCK),
            QiraatiVowelWord("سُوْقُ", isLongVowel = true, vowelLabel = "مد طويل (واو)", highlightedSegment = "سُوْ", artKey = QiraatiWordArtKey.MARKET),
            QiraatiVowelWord("تَمَاسِيْحُ", isLongVowel = true, vowelLabel = "مد طويل (ياء)", highlightedSegment = "سِيْ", artKey = QiraatiWordArtKey.CROCODILE)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("مِسْطَرَةُ", sukoonSyllable = "مِسْـ", remainingPart = "طَرَةُ", artKey = QiraatiWordArtKey.RULER),
            QiraatiSukoonItem("فُسْتَانُ", sukoonSyllable = "فُسْـ", remainingPart = "تَانُ", artKey = QiraatiWordArtKey.DRESS),
            QiraatiSukoonItem("نَسْرُ", sukoonSyllable = "نَسْـ", remainingPart = "رُ", artKey = QiraatiWordArtKey.EAGLE)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("سَارَ", listOf("سَا", "رَ")),
            QiraatiAnalysisItem("سَافَرَ", listOf("سَا", "فَـ", "رَ")),
            QiraatiAnalysisItem("دَرَسَ", listOf("دَ", "رَ", "سَ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "مِسْـ",
                branches = listOf(
                    QiraatiPuzzleBranch(
                        attachedPiece = "مَا + رُ",
                        resultWord = "مِسْمَارُ",
                        pieces = listOf("مِسْـ", "مَا", "رُ")
                    )
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "فَا",
                branches = listOf(
                    QiraatiPuzzleBranch(
                        attachedPiece = "رِ + سُ",
                        resultWord = "فَارِسُ",
                        pieces = listOf("فَا", "رِ", "سُ")
                    )
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "رَ",
                branches = listOf(
                    QiraatiPuzzleBranch(
                        attachedPiece = "سَـ + مَ",
                        resultWord = "رَسَمَ",
                        pieces = listOf("رَ", "سَـ", "مَ")
                    )
                )
            )
        ),
        readAloudWords = listOf("بَابَا رَامِي رَسَمَ مُوْسَى", "فَارِسُ رَسَمَ مَامَا"),
        tracingLetter = "س",
        tracingForms = listOf("س", "سـ")
    )

    // =========================================================================
    // الدرس 3 — حرف النون (ن ، نـ)
    // =========================================================================
    val noonLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit2_lesson_noon",
        unitNumber = 2,
        lessonOrder = 3,
        letterName = "حرف النون",
        targetLetter = "ن",
        cloudForms = listOf("ن", "نـ"),
        accentColor = Color(0xFF8B5CF6),
        shadowColor = Color(0xFF6233C7),
        introWords = listOf(
            QiraatiIntroWord("نَمْلُ", 'ن', LetterPosition.INITIAL, QiraatiWordArtKey.ANT),
            QiraatiIntroWord("نِقَاطُ", 'ن', LetterPosition.INITIAL, QiraatiWordArtKey.DOTS),
            QiraatiIntroWord("نُجُوْمُ", 'ن', LetterPosition.INITIAL, QiraatiWordArtKey.STARS),
            QiraatiIntroWord("حِصَانُ", 'ن', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.HORSE),
            QiraatiIntroWord("فِنْجَانُ", 'ن', LetterPosition.MULTIPLE, QiraatiWordArtKey.CUP),
            QiraatiIntroWord("نَارُ", 'ن', LetterPosition.INITIAL, QiraatiWordArtKey.FIRE),
            QiraatiIntroWord("فَانُوْسُ", 'ن', LetterPosition.MEDIAL, QiraatiWordArtKey.LANTERN),
            QiraatiIntroWord("نِيْرَانُ", 'ن', LetterPosition.MULTIPLE, QiraatiWordArtKey.FIRE)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("نَـ", "نَا", "فتحة", "مد بالألف"),
            QiraatiVowelPair("نُـ", "نُوْ", "ضمة", "مد بالواو"),
            QiraatiVowelPair("نِـ", "نِيْ", "كسرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("نَسْرُ", isLongVowel = false, vowelLabel = "مد قصير (فتحة)", highlightedSegment = "نَ", artKey = QiraatiWordArtKey.EAGLE),
            QiraatiVowelWord("نُمُوْرُ", isLongVowel = false, vowelLabel = "مد قصير (ضمة)", highlightedSegment = "نُ", artKey = QiraatiWordArtKey.TIGERS),
            QiraatiVowelWord("نِقَاطُ", isLongVowel = false, vowelLabel = "مد قصير (كسرة)", highlightedSegment = "نِ", artKey = QiraatiWordArtKey.DOTS),
            QiraatiVowelWord("نَارُ", isLongVowel = true, vowelLabel = "مد طويل (ألف)", highlightedSegment = "نَا", artKey = QiraatiWordArtKey.FIRE),
            QiraatiVowelWord("جُنُوْدُ", isLongVowel = true, vowelLabel = "مد طويل (واو)", highlightedSegment = "نُوْ", artKey = QiraatiWordArtKey.SOLDIERS),
            QiraatiVowelWord("جَنِيْنُ", isLongVowel = true, vowelLabel = "مد طويل (ياء)", highlightedSegment = "نِيْ", artKey = QiraatiWordArtKey.CHILD)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("بِنْتُ", sukoonSyllable = "بِنْـ", remainingPart = "تُ", artKey = QiraatiWordArtKey.GIRL),
            QiraatiSukoonItem("بُنْدُقُ", sukoonSyllable = "بُنْـ", remainingPart = "دُقُ", artKey = QiraatiWordArtKey.HAZELNUT),
            QiraatiSukoonItem("مِنْجَلُ", sukoonSyllable = "مِنْـ", remainingPart = "جَلُ", artKey = QiraatiWordArtKey.SICKLE)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("نُوْرُ", listOf("نُوْ", "رُ")),
            QiraatiAnalysisItem("مَنَارُ", listOf("مَـ", "نَا", "رُ")),
            QiraatiAnalysisItem("سِنَانُ", listOf("سِـ", "نَا", "نُ")),
            QiraatiAnalysisItem("رَنِيْنُ", listOf("رَ", "نِيْ", "نُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "نَسْـ",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "رُ", resultWord = "نَسْرُ", pieces = listOf("نَسْـ", "رُ"))
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "نَا",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "رُ", resultWord = "نَارُ", pieces = listOf("نَا", "رُ"))
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "نُـ",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "مُوْ + رُ", resultWord = "نُمُوْرُ", pieces = listOf("نُـ", "مُوْ", "رُ"))
                )
            )
        ),
        readAloudWords = listOf("فِي مَرْسَمِ سِنَانَ", "رَسَمَ فَارِسُ دَادَا", "رَسَمَ سِنَانُ بَابَا"),
        tracingLetter = "ن",
        tracingForms = listOf("ن", "نـ")
    )

    // =========================================================================
    // الدرس 4 — حرف التاء (ت ، تـ)
    // =========================================================================
    val taLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit2_lesson_ta",
        unitNumber = 2,
        lessonOrder = 4,
        letterName = "حرف التاء",
        targetLetter = "ت",
        cloudForms = listOf("ت", "تـ"),
        accentColor = Color(0xFFFF7A59),
        shadowColor = Color(0xFFD14B2B),
        introWords = listOf(
            QiraatiIntroWord("تَمْرُ", 'ت', LetterPosition.INITIAL, QiraatiWordArtKey.DATES),
            QiraatiIntroWord("تُفَّاحَةُ", 'ت', LetterPosition.INITIAL, QiraatiWordArtKey.APPLE),
            QiraatiIntroWord("تِمْسَاحُ", 'ت', LetterPosition.INITIAL, QiraatiWordArtKey.CROCODILE),
            QiraatiIntroWord("عَنْكَبُوْتُ", 'ت', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.SPIDER),
            QiraatiIntroWord("سِتْرَةُ", 'ت', LetterPosition.MEDIAL, QiraatiWordArtKey.JACKET),
            QiraatiIntroWord("تَاجُ", 'ت', LetterPosition.INITIAL, QiraatiWordArtKey.CROWN),
            QiraatiIntroWord("تُوْتُ", 'ت', LetterPosition.MULTIPLE, QiraatiWordArtKey.BERRY),
            QiraatiIntroWord("تِيْنُ", 'ت', LetterPosition.INITIAL, QiraatiWordArtKey.FIG)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("تَ", "تَا", "فتحة", "مد بالألف"),
            QiraatiVowelPair("تُ", "تُوْ", "ضمة", "مد بالواو"),
            QiraatiVowelPair("تِ", "تِيْ", "كسرة", "مد بالياء")
        ),
        vowelWords = listOf(
            QiraatiVowelWord("تَمْرُ", isLongVowel = false, vowelLabel = "مد قصير (فتحة)", highlightedSegment = "تَ", artKey = QiraatiWordArtKey.DATES),
            QiraatiVowelWord("تُفَّاحَةُ", isLongVowel = false, vowelLabel = "مد قصير (ضمة)", highlightedSegment = "تُ", artKey = QiraatiWordArtKey.APPLE),
            QiraatiVowelWord("تِمْسَاحُ", isLongVowel = false, vowelLabel = "مد قصير (كسرة)", highlightedSegment = "تِ", artKey = QiraatiWordArtKey.CROCODILE),
            QiraatiVowelWord("تَاجُ", isLongVowel = true, vowelLabel = "مد طويل (ألف)", highlightedSegment = "تَا", artKey = QiraatiWordArtKey.CROWN),
            QiraatiVowelWord("تُوْتُ", isLongVowel = true, vowelLabel = "مد طويل (واو)", highlightedSegment = "تُوْ", artKey = QiraatiWordArtKey.BERRY),
            QiraatiVowelWord("تِيْنُ", isLongVowel = true, vowelLabel = "مد طويل (ياء)", highlightedSegment = "تِيْ", artKey = QiraatiWordArtKey.FIG)
        ),
        sukoonItems = listOf(
            QiraatiSukoonItem("خَتْمُ", sukoonSyllable = "خَتْ", remainingPart = "مُ", artKey = QiraatiWordArtKey.STAMP),
            QiraatiSukoonItem("مُتْحَفُ", sukoonSyllable = "مُتْ", remainingPart = "حَفُ", artKey = QiraatiWordArtKey.MUSEUM),
            QiraatiSukoonItem("شَتْلَاتُ", sukoonSyllable = "شَتْ", remainingPart = "لَاتُ", artKey = QiraatiWordArtKey.SEEDLINGS)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("سِرْتُ", listOf("سِرْ", "تُ")),
            QiraatiAnalysisItem("تُوْتُ", listOf("تُوْ", "تُ")),
            QiraatiAnalysisItem("فَاتِنُ", listOf("فَا", "تِ", "نُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "تَمْ",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "رُ", resultWord = "تَمْرُ", pieces = listOf("تَمْ", "رُ"))
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "تُـ",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "مُوْ + رُ", resultWord = "تُمُوْرُ", pieces = listOf("تُـ", "مُوْ", "رُ"))
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "تِيْ",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "نُ", resultWord = "تِيْنُ", pieces = listOf("تِيْ", "نُ"))
                )
            )
        ),
        readAloudWords = listOf(
            "فِي بُسْتَانِ فَاتِنَ",
            "سَارَ بَابَا سَارَتْ مَامَا",
            "فِي بُسْتَانِ فَاتِنَ تِيْنُ"
        ),
        tracingLetter = "ت",
        tracingForms = listOf("ت", "تـ")
    )

    // =========================================================================
    // الدرس 5 — التاء المربوطة (ة ، ـة) بأسلوب التفاحات ولعبتي الدائرة والاختيار
    // =========================================================================
    val taaMarboutaLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit2_lesson_taa_marbouta",
        unitNumber = 2,
        lessonOrder = 5,
        letterName = "التاء المربوطة",
        targetLetter = "ة",
        cloudForms = listOf("ة", "ـة"),
        accentColor = Color(0xFFEC4899),
        shadowColor = Color(0xFFBE185D),
        introWords = listOf(
            QiraatiIntroWord("مِبْرَاةُ", 'ة', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.SHARPENER),
            QiraatiIntroWord("مِمْحَاةُ", 'ة', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.ERASER),
            QiraatiIntroWord("مِسْطَرَةُ", 'ة', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.RULER),
            QiraatiIntroWord("حَقِيْبَةُ", 'ة', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.BACKPACK),
            QiraatiIntroWord("مَدْرَسَةُ", 'ة', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.SCHOOL),
            QiraatiIntroWord("تِلْمِيْذَةُ", 'ة', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.SCHOOLGIRL)
        ),
        vowelPairs = listOf(
            QiraatiVowelPair("ةَ", "ـةَ", "فتحة منفصلة", "فتحة متصلة", spokenShort = "تَ", spokenLong = "تَ"),
            QiraatiVowelPair("ةُ", "ـةُ", "ضمة منفصلة", "ضمة متصلة", spokenShort = "تُ", spokenLong = "تُ"),
            QiraatiVowelPair("ةِ", "ـةِ", "كسرة منفصلة", "كسرة متصلة", spokenShort = "تِ", spokenLong = "تِ")
        ),
        vowelWords = emptyList(),
        sukoonItems = emptyList(),
        circleFilterWords = listOf(
            QiraatiCircleFilterWord("بَنَاتُ", containsTarget = false, artKey = QiraatiWordArtKey.GIRL),
            QiraatiCircleFilterWord("سَمَكَةُ", containsTarget = true, artKey = QiraatiWordArtKey.FISH),
            QiraatiCircleFilterWord("لَوْحَةُ", containsTarget = true, artKey = QiraatiWordArtKey.PAINTING),
            QiraatiCircleFilterWord("دُمْيَةُ", containsTarget = true, artKey = QiraatiWordArtKey.DOLL),
            QiraatiCircleFilterWord("تُوْتُ", containsTarget = false, artKey = QiraatiWordArtKey.BERRY)
        ),
        suffixChoiceItems = listOf(
            QiraatiSuffixChoiceItem("سَفِيْنَـ", listOf("ة", "ـة"), "ـة", "سَفِيْنَةُ", QiraatiWordArtKey.SHIP),
            QiraatiSuffixChoiceItem("وَرْدَ", listOf("ة", "ـة"), "ة", "وَرْدَةُ", QiraatiWordArtKey.ROSE),
            QiraatiSuffixChoiceItem("مِسْطَرَ", listOf("ة", "ـة"), "ة", "مِسْطَرَةُ", QiraatiWordArtKey.RULER),
            QiraatiSuffixChoiceItem("نَمْلَـ", listOf("ة", "ـة"), "ـة", "نَمْلَةُ", QiraatiWordArtKey.ANT),
            QiraatiSuffixChoiceItem("لَوْحَـ", listOf("ة", "ـة"), "ـة", "لَوْحَةُ", QiraatiWordArtKey.PAINTING)
        ),
        isAppleVowelsStyle = true,
        analysisItems = listOf(
            QiraatiAnalysisItem("سَارَةُ", listOf("سَا", "رَ", "ةُ")),
            QiraatiAnalysisItem("دُمْيَةُ", listOf("دُمْ", "يَـ", "ةُ")),
            QiraatiAnalysisItem("سَفِيْنَةُ", listOf("سَـ", "فِيْ", "نَـ", "ةُ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "سَا",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "رَةُ", resultWord = "سَارَةُ", pieces = listOf("سَا", "رَةُ")),
                    QiraatiPuzzleBranch(attachedPiece = "وَةُ", resultWord = "سَاوَةُ", pieces = listOf("سَا", "وَةُ"))
                )
            )
        ),
        readAloudWords = listOf(
            "فِي مَدْرَسَةِ سَارَةَ",
            "سَارَةُ تَرْسِمُ",
            "دِيْمَةُ تَدْرُسُ",
            "سَرْوَةُ تَبْتَسِمُ"
        ),
        tracingLetter = "ة",
        tracingForms = listOf("ة", "ـة")
    )

    // =========================================================================
    // الدرس 6 — تنوين الضم (ـٌ)
    // =========================================================================
    val tanweenDammLesson: QiraatiLetterLesson = QiraatiLetterLesson(
        id = "unit2_lesson_tanween_damm",
        unitNumber = 2,
        lessonOrder = 6,
        letterName = "تنوين الضم",
        targetLetter = "ـٌ",
        cloudForms = listOf("ـٌ", "بٌ"),
        accentColor = Color(0xFFF59E0B),
        shadowColor = Color(0xFFB45309),
        introWords = listOf(
            QiraatiIntroWord("قِرْدٌ", 'ٌ', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.MONKEY),
            QiraatiIntroWord("كِتَابٌ", 'ٌ', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.BOOK),
            QiraatiIntroWord("لَحْمٌ", 'ٌ', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.MEAT),
            QiraatiIntroWord("حَقِيْبَةٌ", 'ٌ', LetterPosition.FINAL_CONNECTED, QiraatiWordArtKey.BACKPACK)
        ),
        vowelPairs = emptyList(),
        vowelWords = emptyList(),
        sukoonItems = emptyList(),
        tanweenListenWords = listOf(
            QiraatiIntroWord("مُدِيْرَةٌ", 'ٌ', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.TEACHER),
            QiraatiIntroWord("بَابٌ", 'ٌ', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.DOOR),
            QiraatiIntroWord("لَوْحٌ", 'ٌ', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.BOARD),
            QiraatiIntroWord("فَارِسٌ", 'ٌ', LetterPosition.FINAL_ISOLATED, QiraatiWordArtKey.KNIGHT)
        ),
        analysisItems = listOf(
            QiraatiAnalysisItem("سُوْرٌ", listOf("سُوْ", "رٌ")),
            QiraatiAnalysisItem("سِرْبٌ", listOf("سِرْ", "بٌ"))
        ),
        puzzleItems = listOf(
            QiraatiPuzzleItem(
                basePiece = "تُوْ",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "تٌ", resultWord = "تُوْتٌ", pieces = listOf("تُوْ", "تٌ"))
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "تِيْ",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "نٌ", resultWord = "تِيْنٌ", pieces = listOf("تِيْ", "نٌ"))
                )
            ),
            QiraatiPuzzleItem(
                basePiece = "تَمْ",
                branches = listOf(
                    QiraatiPuzzleBranch(attachedPiece = "رٌ", resultWord = "تَمْرٌ", pieces = listOf("تَمْ", "رٌ"))
                )
            )
        ),
        readAloudWords = listOf(
            "سُوْرُ مَدْرَسَتِي مَتِيْنٌ",
            "فِي بُسْتَانِ فَاتِنَ تُوْتٌ",
            "وَتِيْنٌ وَتَمْرٌ"
        ),
        tracingLetter = "بٌ",
        tracingForms = listOf("بٌ", "رٌ")
    )

    // =========================================================================
    // نشيد الوحدة الختامي ("مَدْرَسَتِي")
    // =========================================================================
    val madrasatiChant: QiraatiUnitChant = QiraatiUnitChant(
        id = "unit2_chant_madrasati",
        title = "نشيد مَدْرَسَتِي",
        subtitle = "استمع إلى نشيد الوحدة الثانية وردّد الأبيات بفرح وحماس!",
        verses = listOf(
            "مَدْرَسَتِي مَدْرَسَتِي",
            "أُحِبُّهَا كَإِخْوَتِي",
            "فِيهَا وَجَدْتُ رَاحَتِي",
            "وَفَرْحَتِي وَعِزَّتِي"
        )
    )

    // =========================================================================
    // كائن الوحدة الثانية الكامل جاهز للتسجيل في NewQiraatiRepository
    // =========================================================================
    val unit: QiraatiUnit = QiraatiUnit(
        id = "unit_2_madrasati",
        unitNumber = 2,
        unitBadge = "الوحدة الثانية",
        title = "الوحدة الثانية: مدرستي",
        shortTitle = "مدرستي",
        description = "قصة مدرستي والمحادثة مع دروس: الراء، السين، النون، التاء، التاء المربوطة، تنوين الضم، ونشيد مَدْرَسَتِي",
        themeColor = unitTheme.primaryColor,
        theme = unitTheme,
        storySection = storySection,
        letterLessons = listOf(
            raLesson,
            seenLesson,
            noonLesson,
            taLesson,
            taaMarboutaLesson,
            tanweenDammLesson
        ),
        isAvailable = true,
        coverArtKey = QiraatiStoryArtKey.MADRASATI_SCHOOL_FLAG,
        unitChant = madrasatiChant
    )
}
