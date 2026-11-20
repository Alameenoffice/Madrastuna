package com.example.reading

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class ReadingLessonTab(
    val title: String,
    val color: Color,
    val shadowColor: Color,
    val testTag: String
) {
    LEARN("تعلّم", ReadingOasisColors.SkyBright, ReadingOasisColors.RoyalBlueShadow, "reading_tab_learn"),
    BUILD("ركّب", ReadingOasisColors.Purple, ReadingOasisColors.PurpleShadow, "reading_tab_build"),
    WRITE("اكتب", ReadingOasisColors.Coral, ReadingOasisColors.CoralShadow, "reading_tab_write"),
    CHALLENGE("تحدّي", ReadingOasisColors.Pink, ReadingOasisColors.PinkShadow, "reading_tab_challenge")
}

/**
 * شاشة الدرس التفاعلي في "واحة القراءة الذكية":
 * تضم 4 أنشطة مبهجة للأطفال (تعلّم | ركّب | اكتب | تحدّي) مع نطق مقطعي متزامن
 * وحفظ محلي وسحابي تلقائي لنجوم الطفل.
 */
@Composable
fun ReadingLessonScreen(
    lesson: ReadingLesson,
    schoolCode: String,
    studentId: String,
    studentName: String = "",
    onBackClick: () -> Unit,
    onProgressUpdated: (StudentReadingProgress) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioEngine = remember(context) { ReadingAudioEngine(context) }
    val progressEngine = remember(context) { ReadingProgressEngine(context) }

    var selectedTab by remember(lesson.id) { mutableStateOf(ReadingLessonTab.LEARN) }
    var celebrationTrigger by remember { mutableIntStateOf(0) }
    var isMascotCelebrating by remember { mutableStateOf(false) }

    var studentProgress by remember(schoolCode, studentId, lesson.id) {
        mutableStateOf(progressEngine.loadLocalProgress(schoolCode, studentId))
    }
    val lessonStars = studentProgress.lessons[lesson.id]?.starsEarned ?: 0

    DisposableEffect(audioEngine, progressEngine) {
        onDispose {
            audioEngine.release()
            progressEngine.close()
        }
    }

    fun triggerJoyCelebration(newStarsTarget: Int, practicedWordId: String? = null, tracedLetter: String? = null) {
        celebrationTrigger += 1
        isMascotCelebrating = true
        scope.launch {
            delay(1750L)
            isMascotCelebrating = false
        }
        val effectiveStars = maxOf(lessonStars, newStarsTarget.coerceIn(1, 3))
        val updated = progressEngine.recordLessonProgress(
            schoolCode = schoolCode,
            studentId = studentId,
            lessonId = lesson.id,
            starsEarned = effectiveStars,
            isCompleted = true,
            practicedWords = if (practicedWordId != null) setOf(practicedWordId) else emptySet(),
            tracedLetters = if (tracedLetter != null) setOf(tracedLetter) else emptySet(),
            syncInBackground = true
        )
        studentProgress = updated
        onProgressUpdated(updated)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD7F2FF),
                            ReadingOasisColors.SkySoft,
                            Color(0xFFF6FCFF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // الشريط العلوي الطفولي (زر الرجوع + اسم الدرس + عداد النجوم)
                ReadingLessonTopBar(
                    lesson = lesson,
                    earnedStars = lessonStars,
                    onBackClick = {
                        audioEngine.stop()
                        onBackClick()
                    }
                )

                // التبويبات الأربعة الكبيرة والملونة
                ReadingLessonTabsRow(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        audioEngine.stop()
                        audioEngine.playSyllableTapTone()
                        selectedTab = tab
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // محتوى النشاط التفاعلي المحدد
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (selectedTab) {
                        ReadingLessonTab.LEARN -> LearnSyllableTrainTab(
                            lesson = lesson,
                            audioEngine = audioEngine,
                            isMascotCelebrating = isMascotCelebrating,
                            onWordPracticed = { wordId ->
                                val updated = progressEngine.recordPracticeActivity(
                                    schoolCode = schoolCode,
                                    studentId = studentId,
                                    lessonId = lesson.id,
                                    practicedWordId = wordId,
                                    syncInBackground = true
                                )
                                studentProgress = updated
                                onProgressUpdated(updated)
                            }
                        )

                        ReadingLessonTab.BUILD -> BuildWordPuzzleTab(
                            lesson = lesson,
                            audioEngine = audioEngine,
                            isMascotCelebrating = isMascotCelebrating,
                            onWordAssembled = { wordId ->
                                triggerJoyCelebration(
                                    newStarsTarget = maxOf(1, lessonStars),
                                    practicedWordId = wordId
                                )
                            }
                        )

                        ReadingLessonTab.WRITE -> MagicWritingBoardTab(
                            lesson = lesson,
                            audioEngine = audioEngine,
                            onWritingFinished = { letter ->
                                triggerJoyCelebration(
                                    newStarsTarget = maxOf(2, lessonStars),
                                    tracedLetter = letter
                                )
                            }
                        )

                        ReadingLessonTab.CHALLENGE -> StarChallengeTab(
                            lesson = lesson,
                            studentName = studentName,
                            audioEngine = audioEngine,
                            isMascotCelebrating = isMascotCelebrating,
                            onAnswerCelebrated = {
                                celebrationTrigger += 1
                                isMascotCelebrating = true
                                scope.launch {
                                    delay(1450L)
                                    isMascotCelebrating = false
                                }
                            },
                            onChallengeCompleted = { earnedChallengeStars ->
                                triggerJoyCelebration(newStarsTarget = earnedChallengeStars)
                            }
                        )
                    }
                }
            }

            // طبقة الاحتفال بالنجوم والبالونات فوق الشاشة عند الفوز
            KidsCelebrationCanvas(
                triggerKey = celebrationTrigger,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ReadingLessonTopBar(
    lesson: ReadingLesson,
    earnedStars: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PlayfulBouncyButton(
            onClick = onBackClick,
            containerColor = ReadingOasisColors.Coral,
            shadowColor = ReadingOasisColors.CoralShadow,
            cornerRadius = 18.dp,
            shadowDepth = 4.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.testTag("reading_lesson_back_btn")
        ) {
            PlayfulBackArrowCanvas(modifier = Modifier.size(22.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ReadingOasisColors.White)
                    .border(2.dp, ReadingOasisColors.SkyBright.copy(alpha = 0.5f), CircleShape)
                    .padding(4.dp)
            ) {
                LessonIllustrationCanvas(lessonId = lesson.id, modifier = Modifier.fillMaxSize())
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    color = ReadingOasisColors.InkDark,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lesson.subtitle,
                    color = ReadingOasisColors.InkMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // شارة النجوم الثلاث للدرس
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(ReadingOasisColors.White)
                .border(2.dp, ReadingOasisColors.SunnyYellow, RoundedCornerShape(18.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("reading_lesson_stars_badge"),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..3) {
                PlayfulStarCanvas(
                    filled = earnedStars >= i,
                    glowing = earnedStars >= i,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ReadingLessonTabsRow(
    selectedTab: ReadingLessonTab,
    onSelectTab: (ReadingLessonTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReadingLessonTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val bgColor = if (isSelected) tab.color else ReadingOasisColors.White
            val shColor = if (isSelected) tab.shadowColor else Color(0xFFC5DFF0)
            val txtColor = if (isSelected) ReadingOasisColors.White else ReadingOasisColors.InkDark
            val iconColor = if (isSelected) ReadingOasisColors.White else tab.color

            PlayfulBouncyButton(
                onClick = { onSelectTab(tab) },
                containerColor = bgColor,
                shadowColor = shColor,
                contentColor = txtColor,
                cornerRadius = 18.dp,
                shadowDepth = if (isSelected) 5.dp else 4.dp,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag(tab.testTag)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    when (tab) {
                        ReadingLessonTab.LEARN -> PlayfulBookIconCanvas(
                            color = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                        ReadingLessonTab.BUILD -> PlayfulPuzzleIconCanvas(
                            color = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                        ReadingLessonTab.WRITE -> PlayfulCrayonCanvas(
                            color = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                        ReadingLessonTab.CHALLENGE -> PlayfulCrownCanvas(
                            color = if (isSelected) ReadingOasisColors.SunnyYellow else tab.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = tab.title,
                        color = txtColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================================================
// تبويب 1: "تعلّم" — الحرف العملاق + بطاقات المقاطع + قطار المقاطع والنطق
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearnSyllableTrainTab(
    lesson: ReadingLesson,
    audioEngine: ReadingAudioEngine,
    isMascotCelebrating: Boolean,
    onWordPracticed: (String) -> Unit
) {
    val isSpeaking by audioEngine.isSpeaking.collectAsState()
    var selectedLetterIdx by remember(lesson.id) { mutableIntStateOf(0) }
    var letterRippleActive by remember { mutableStateOf(false) }
    var activeWordId by remember { mutableStateOf<String?>(null) }
    var activeSyllableIndex by remember { mutableIntStateOf(-1) }
    var isFullWordHighlighted by remember { mutableStateOf(false) }
    var activeSentenceWordIdx by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    val currentLetter = lesson.targetLetters.getOrElse(selectedLetterIdx) { lesson.targetLetters.first() }
    val lessonAccent = lesson.primaryColor()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // اختيار الحرف إذا كان للدرس أكثر من حرف (مثل درس نور ونوري: ن، ي)
        if (lesson.targetLetters.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                lesson.targetLetters.forEachIndexed { idx, letter ->
                    val chosen = idx == selectedLetterIdx
                    PlayfulBouncyButton(
                        onClick = {
                            selectedLetterIdx = idx
                            audioEngine.speakText(letter)
                        },
                        containerColor = if (chosen) ReadingOasisColors.Purple else ReadingOasisColors.White,
                        contentColor = if (chosen) ReadingOasisColors.White else ReadingOasisColors.InkDark,
                        cornerRadius = 18.dp,
                        shadowDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("حرف ($letter)", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // بطاقة الحرف الجديد العملاقة (92.sp) بجانب الرفيق "فصيح" مع حلقات التموّج الصوتي
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(ReadingOasisColors.White)
                .border(3.dp, ReadingOasisColors.SunnyYellow, RoundedCornerShape(30.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // بطاقة الحرف القابلة للنقر والتموّج الصوتي
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(164.dp)
                        .clickable(role = Role.Button) {
                            letterRippleActive = true
                            audioEngine.playSyllableTapTone()
                            audioEngine.speakText(currentLetter) {
                                letterRippleActive = false
                            }
                            scope.launch {
                                delay(1600L)
                                letterRippleActive = false
                            }
                        }
                        .testTag("reading_giant_letter_card")
                ) {
                    SoundRippleRingsCanvas(
                        isActive = letterRippleActive || isSpeaking,
                        color = lessonAccent,
                        modifier = Modifier.fillMaxSize()
                    )

                    val letterScale by animateFloatAsState(
                        targetValue = if (letterRippleActive) 1.09f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "giant_letter_scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(134.dp)
                            .graphicsLayer {
                                scaleX = letterScale
                                scaleY = letterScale
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        lessonAccent.copy(alpha = 0.22f),
                                        lessonAccent.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(3.dp, lessonAccent, RoundedCornerShape(28.dp))
                    ) {
                        Text(
                            text = currentLetter,
                            color = lessonAccent,
                            fontSize = 92.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // الرفيق فصيح بجانب الحرف مع زر الاستماع
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingMascotCanvas(
                        isCelebrating = isMascotCelebrating || letterRippleActive,
                        modifier = Modifier.size(96.dp)
                    )
                    Text(
                        text = "اضغط على الحرف لتسمعه!",
                        color = ReadingOasisColors.InkDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    PlayfulBouncyButton(
                        onClick = {
                            letterRippleActive = true
                            audioEngine.speakText(currentLetter) {
                                letterRippleActive = false
                            }
                        },
                        containerColor = ReadingOasisColors.SunnyYellow,
                        shadowColor = ReadingOasisColors.SunnyShadow,
                        contentColor = ReadingOasisColors.InkDark,
                        cornerRadius = 18.dp,
                        shadowDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        PlayfulSpeakerCanvas(
                            color = ReadingOasisColors.InkDark,
                            isPulsing = letterRippleActive,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("اسمع الحرف", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // قسم المقاطع الملونة
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "مقاطع الدرس الملوّنة",
                color = ReadingOasisColors.InkDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                lesson.syllables.forEachIndexed { idx, syllable ->
                    val sylColor = syllable.toComposeColor()
                    PlayfulBouncyButton(
                        onClick = {
                            audioEngine.playSyllableTapTone()
                            audioEngine.speakSyllable(syllable)
                        },
                        containerColor = sylColor,
                        cornerRadius = 22.dp,
                        shadowDepth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("reading_syllable_btn_$idx")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = syllable.text,
                                color = ReadingOasisColors.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = syllable.type.arabicLabel,
                                color = ReadingOasisColors.White.copy(alpha = 0.90f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // قسم قطار المقاطع للكلمات
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "قطار المقاطع والكلمات",
                color = ReadingOasisColors.InkDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            lesson.words.forEach { word ->
                val isThisWordPlaying = activeWordId == word.id
                SyllableTrainWordCard(
                    word = word,
                    activeSyllableIndex = if (isThisWordPlaying) activeSyllableIndex else -1,
                    isFullWordHighlighted = isThisWordPlaying && isFullWordHighlighted,
                    onSyllableTap = { syl ->
                        audioEngine.playSyllableTapTone()
                        audioEngine.speakSyllable(syl)
                    },
                    onPlaySegmented = {
                        activeWordId = word.id
                        isFullWordHighlighted = false
                        activeSyllableIndex = -1
                        audioEngine.speakWordBySyllables(
                            word = word,
                            onActiveSyllableIndexChanged = { idx ->
                                activeSyllableIndex = idx
                            },
                            onFullWordSpeaking = {
                                isFullWordHighlighted = true
                            },
                            onCompleted = {
                                isFullWordHighlighted = false
                                activeSyllableIndex = -1
                                onWordPracticed(word.id)
                            }
                        )
                    }
                )
            }
        }

        // جملة الدرس القرائية
        if (lesson.sentences.isNotEmpty()) {
            val sentence = lesson.sentences.first()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(ReadingOasisColors.White)
                    .border(2.dp, ReadingOasisColors.Mint.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "هيا نقرأ جملة الدرس",
                    color = ReadingOasisColors.MintShadow,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sentence.words.forEachIndexed { idx, w ->
                        val active = activeSentenceWordIdx == idx
                        val bg = if (active) ReadingOasisColors.SunnyYellow else Color(0xFFF0F9FF)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(bg)
                                .border(
                                    2.dp,
                                    if (active) ReadingOasisColors.Coral else ReadingOasisColors.SkyBright.copy(alpha = 0.4f),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { audioEngine.speakText(w.spokenWord) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = w.fullWord,
                                color = ReadingOasisColors.InkDark,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                PlayfulBouncyButton(
                    onClick = {
                        audioEngine.speakSentence(
                            sentence = sentence,
                            onActiveWordIndexChanged = { activeSentenceWordIdx = it },
                            onCompleted = { activeSentenceWordIdx = -1 }
                        )
                    },
                    containerColor = ReadingOasisColors.Mint,
                    shadowColor = ReadingOasisColors.MintShadow,
                    cornerRadius = 18.dp,
                    shadowDepth = 4.dp
                ) {
                    PlayfulSpeakerCanvas(
                        color = ReadingOasisColors.White,
                        isPulsing = activeSentenceWordIdx >= 0,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("اسمع الجملة كاملة", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

/**
 * بطاقة كلمة مصممة كـ "عربات قطار المقاطع"؛ تقفز كل عربة للأعلى وتضيء بالتزامن مع نطق مقطعها.
 */
@Composable
private fun SyllableTrainWordCard(
    word: SegmentedWord,
    activeSyllableIndex: Int,
    isFullWordHighlighted: Boolean,
    onSyllableTap: (ColoredSyllable) -> Unit,
    onPlaySegmented: () -> Unit
) {
    val fullScale by animateFloatAsState(
        targetValue = if (isFullWordHighlighted) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "full_word_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(ReadingOasisColors.White)
            .border(
                width = if (isFullWordHighlighted) 3.dp else 2.dp,
                color = if (isFullWordHighlighted) ReadingOasisColors.SunnyYellow else Color(0xFFD5EDFF),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // صف قطار المقاطع: قاطرة مبهجة + عربات المقاطع الملونة
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // سكة حديدية طفولية أسفل العجلات
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.BottomCenter)
            ) {
                drawLine(
                    color = Color(0xFF94A3B8).copy(alpha = 0.45f),
                    start = Offset(12f, size.height * 0.6f),
                    end = Offset(size.width - 12f, size.height * 0.6f),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                // قاطرة القطار المرسومة بـ Canvas في مقدمة القطار (يميناً في RTL)
                TrainLocomotiveCanvas(modifier = Modifier.size(width = 58.dp, height = 62.dp))

                word.syllables.forEachIndexed { idx, syllable ->
                    val isActive = activeSyllableIndex == idx
                    val jumpY by animateDpAsState(
                        targetValue = if (isActive) (-14).dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "car_jump_$idx"
                    )
                    val carScale by animateFloatAsState(
                        targetValue = if (isActive) 1.12f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "car_scale_$idx"
                    )

                    val carColor = syllable.toComposeColor()

                    // وصلة الربط بين العربات
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(6.dp)
                            .offset(y = (-16).dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF64748B))
                    )

                    // عربة المقطع الملونة مع عجلاتها
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset(y = jumpY)
                            .graphicsLayer {
                                scaleX = carScale
                                scaleY = carScale
                            }
                            .clickable(role = Role.Button) { onSyllableTap(syllable) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .widthIn(min = 60.dp)
                                .height(54.dp)
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                                .background(carColor)
                                .border(
                                    width = if (isActive) 3.5.dp else 1.5.dp,
                                    color = if (isActive) ReadingOasisColors.SunnyYellow else Color.White.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
                                )
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = syllable.text,
                                color = ReadingOasisColors.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        // عجلتا العربة
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.offset(y = (-3).dp)
                        ) {
                            TrainWheelCanvas(modifier = Modifier.size(14.dp))
                            TrainWheelCanvas(modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // الكلمة الكاملة + زر "اسمع بالتقطيع"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = fullScale
                        scaleY = fullScale
                    }
            ) {
                Text(
                    text = word.fullWord,
                    color = if (isFullWordHighlighted) ReadingOasisColors.Coral else ReadingOasisColors.InkDark,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                if (word.hint.isNotBlank()) {
                    Text(
                        text = word.hint,
                        color = ReadingOasisColors.InkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            PlayfulBouncyButton(
                onClick = onPlaySegmented,
                containerColor = ReadingOasisColors.SkyBright,
                shadowColor = ReadingOasisColors.RoyalBlueShadow,
                cornerRadius = 18.dp,
                shadowDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.testTag("reading_play_segmented_${word.id}")
            ) {
                PlayfulSpeakerCanvas(
                    color = ReadingOasisColors.White,
                    isPulsing = activeSyllableIndex >= 0 || isFullWordHighlighted,
                    modifier = Modifier.size(20.dp)
                )
                Text("اسمع بالتقطيع", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun TrainLocomotiveCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // سحابة بخار صغيرة لطيفة
        drawCircle(Color(0xFFBAE6FD), radius = w * 0.11f, center = Offset(w * 0.72f, h * 0.12f))
        drawCircle(Color(0xFFE0F2FE), radius = w * 0.08f, center = Offset(w * 0.56f, h * 0.09f))
        // المدخنة
        drawRoundRect(
            color = ReadingOasisColors.Coral,
            topLeft = Offset(w * 0.62f, h * 0.22f),
            size = Size(w * 0.18f, h * 0.24f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // كابينة السائق
        drawRoundRect(
            color = ReadingOasisColors.SunnyYellow,
            topLeft = Offset(w * 0.08f, h * 0.26f),
            size = Size(w * 0.42f, h * 0.50f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        // نافذة الكابينة
        drawRoundRect(
            color = Color(0xFFE0F2FE),
            topLeft = Offset(w * 0.15f, h * 0.34f),
            size = Size(w * 0.26f, h * 0.20f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // مقدمة القاطرة
        drawRoundRect(
            color = ReadingOasisColors.Coral,
            topLeft = Offset(w * 0.44f, h * 0.42f),
            size = Size(w * 0.46f, h * 0.34f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        // مصباح أمامي مضيء
        drawCircle(ReadingOasisColors.SunnyYellow, radius = w * 0.07f, center = Offset(w * 0.89f, h * 0.58f))
        // العجلات
        drawCircle(ReadingOasisColors.InkDark, radius = w * 0.12f, center = Offset(w * 0.28f, h * 0.82f))
        drawCircle(ReadingOasisColors.SunnyYellow, radius = w * 0.05f, center = Offset(w * 0.28f, h * 0.82f))
        drawCircle(ReadingOasisColors.InkDark, radius = w * 0.12f, center = Offset(w * 0.70f, h * 0.82f))
        drawCircle(ReadingOasisColors.SunnyYellow, radius = w * 0.05f, center = Offset(w * 0.70f, h * 0.82f))
    }
}

@Composable
private fun TrainWheelCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(ReadingOasisColors.InkDark, radius = size.minDimension * 0.48f, center = c)
        drawCircle(ReadingOasisColors.SunnyYellow, radius = size.minDimension * 0.20f, center = c)
    }
}

// ============================================================================
// تبويب 2: "ركّب" — لعبة تجميع الكلمة من المقاطع المبعثرة
// ============================================================================

private data class ShuffledSyllableTile(
    val tileId: Int,
    val syllable: ColoredSyllable
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildWordPuzzleTab(
    lesson: ReadingLesson,
    audioEngine: ReadingAudioEngine,
    isMascotCelebrating: Boolean,
    onWordAssembled: (String) -> Unit
) {
    var wordIndex by remember(lesson.id) { mutableIntStateOf(0) }
    val currentWord = lesson.words[wordIndex % lesson.words.size]

    val availableTiles = remember(lesson.id, wordIndex) {
        val wordTiles = currentWord.syllables.mapIndexed { idx, syl -> ShuffledSyllableTile(idx, syl) }
        // إضافة مقطع مشتّت واحد من الدرس عندما تكون الكلمة من مقطعين لزيادة المتعة والتعلم
        val distractor = lesson.syllables.firstOrNull { cand ->
            currentWord.syllables.none { it.text == cand.text }
        }
        val combined = if (distractor != null) {
            wordTiles + ShuffledSyllableTile(wordTiles.size, distractor)
        } else {
            wordTiles
        }
        combined.shuffled()
    }

    val placedTiles = remember(lesson.id, wordIndex) { mutableStateListOf<ShuffledSyllableTile>() }
    var isWordSolved by remember(lesson.id, wordIndex) { mutableStateOf(false) }
    var feedbackMessage by remember(lesson.id, wordIndex) {
        mutableStateOf("استمع للكلمة ثم اضغط على المقاطع بالترتيب الصحيح!")
    }
    var wrongShakeTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(lesson.id, wordIndex) {
        delay(220L)
        audioEngine.speakText(currentWord.spokenWord)
    }

    val shakeOffset by animateDpAsState(
        targetValue = if (wrongShakeTrigger % 2 == 1) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "wrong_shake"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // رأس اللعبة: فصيح + عداد الكلمات + زر الاستماع
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(ReadingOasisColors.White)
                .border(2.dp, ReadingOasisColors.Purple.copy(alpha = 0.40f), RoundedCornerShape(26.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReadingMascotCanvas(
                isCelebrating = isMascotCelebrating || isWordSolved,
                modifier = Modifier.size(72.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "الكلمة ${wordIndex + 1} من ${lesson.words.size}",
                    color = ReadingOasisColors.Purple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = feedbackMessage,
                    color = ReadingOasisColors.InkDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            PlayfulBouncyButton(
                onClick = { audioEngine.speakText(currentWord.spokenWord) },
                containerColor = ReadingOasisColors.SunnyYellow,
                shadowColor = ReadingOasisColors.SunnyShadow,
                contentColor = ReadingOasisColors.InkDark,
                cornerRadius = 18.dp,
                shadowDepth = 4.dp,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.testTag("reading_build_listen_btn")
            ) {
                PlayfulSpeakerCanvas(color = ReadingOasisColors.InkDark, modifier = Modifier.size(24.dp))
            }
        }

        // خانات التجميع الملونة الفارغة التي تمتلئ عند النقر الصحيح
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = shakeOffset)
                .clip(RoundedCornerShape(28.dp))
                .background(ReadingOasisColors.White)
                .border(
                    width = 3.dp,
                    color = if (isWordSolved) ReadingOasisColors.Mint else Color(0xFFD6EEFF),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(vertical = 22.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentWord.syllables.forEachIndexed { slotIndex, expected ->
                    val placed = placedTiles.getOrNull(slotIndex)
                    val slotColor = expected.toComposeColor()

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .widthIn(min = 74.dp)
                            .height(74.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (placed != null) placed.syllable.toComposeColor()
                                else slotColor.copy(alpha = 0.12f)
                            )
                            .border(
                                width = 3.dp,
                                color = if (placed != null) ReadingOasisColors.SunnyYellow else slotColor.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 14.dp)
                    ) {
                        if (placed != null) {
                            Text(
                                text = placed.syllable.text,
                                color = ReadingOasisColors.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Text(
                                text = "${slotIndex + 1}",
                                color = slotColor.copy(alpha = 0.55f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // ظهور الكلمة كاملة مدمجة عند الإكمال
            AnimatedVisibility(
                visible = isWordSolved,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(ReadingOasisColors.Mint.copy(alpha = 0.15f))
                            .border(2.5.dp, ReadingOasisColors.Mint, RoundedCornerShape(22.dp))
                            .padding(horizontal = 28.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = currentWord.fullWord,
                            color = ReadingOasisColors.MintShadow,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "أحسنت يا بطل! ركّبت الكلمة بنجاح",
                        color = ReadingOasisColors.MintShadow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // بطاقات المقاطع المبعثرة الكبيرة للضغط عليها بالترتيب
        if (!isWordSolved) {
            Text(
                text = "اختر المقطع التالي:",
                color = ReadingOasisColors.InkDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableTiles.forEach { tile ->
                    val alreadyUsed = placedTiles.any { it.tileId == tile.tileId }
                    PlayfulBouncyButton(
                        onClick = {
                            if (alreadyUsed || isWordSolved) return@PlayfulBouncyButton
                            val nextIndex = placedTiles.size
                            val expectedSyllable = currentWord.syllables[nextIndex]
                            if (tile.syllable.text == expectedSyllable.text) {
                                placedTiles.add(tile)
                                audioEngine.playSyllableTapTone()
                                if (placedTiles.size == currentWord.syllables.size) {
                                    isWordSolved = true
                                    feedbackMessage = "أحسنت يا بطل! أبدعت في تركيب الكلمة"
                                    audioEngine.playSuccessChime()
                                    audioEngine.speakText("أحسنت يا بطل! ${currentWord.spokenWord}")
                                    onWordAssembled(currentWord.id)
                                } else {
                                    feedbackMessage = "رائع! اختر المقطع التالي"
                                    audioEngine.speakSyllable(tile.syllable)
                                }
                            } else {
                                wrongShakeTrigger += 1
                                feedbackMessage = "حاول مرة أخرى يا بطل! استمع للكلمة جيداً"
                                audioEngine.speakText("حاول مرة أخرى")
                            }
                        },
                        enabled = !alreadyUsed,
                        containerColor = tile.syllable.toComposeColor(),
                        cornerRadius = 22.dp,
                        shadowDepth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 16.dp),
                        modifier = Modifier.testTag("reading_build_tile_${tile.tileId}")
                    ) {
                        Text(
                            text = tile.syllable.text,
                            color = ReadingOasisColors.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        } else {
            // زر الانتقال للكلمة التالية أو إعادة اللعب
            PlayfulBouncyButton(
                onClick = {
                    if (wordIndex < lesson.words.lastIndex) {
                        wordIndex += 1
                    } else {
                        wordIndex = 0
                        placedTiles.clear()
                        isWordSolved = false
                        feedbackMessage = "هيا نلعب من جديد!"
                        audioEngine.speakText(lesson.words[0].spokenWord)
                    }
                },
                containerColor = ReadingOasisColors.Mint,
                shadowColor = ReadingOasisColors.MintShadow,
                cornerRadius = 22.dp,
                shadowDepth = 5.dp,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                modifier = Modifier.testTag("reading_build_next_btn")
            ) {
                PlayfulStarCanvas(filled = true, modifier = Modifier.size(22.dp))
                Text(
                    text = if (wordIndex < lesson.words.lastIndex) "الكلمة التالية" else "أعد التركيب",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ============================================================================
// تبويب 3: "اكتب" — سبورة القلم السحري مع الدليل الشفاف وبريق النجوم
// ============================================================================

private data class CrayonStroke(
    val color: Color,
    val points: List<Offset>
)

private data class MagicSparkle(
    val position: Offset,
    val color: Color,
    val createdAtMs: Long,
    val radius: Float
)

@Composable
private fun MagicWritingBoardTab(
    lesson: ReadingLesson,
    audioEngine: ReadingAudioEngine,
    onWritingFinished: (String) -> Unit
) {
    val guides = lesson.letterGuides
    var guideIdx by remember(lesson.id) { mutableIntStateOf(0) }
    val currentGuide = guides[guideIdx % guides.size]

    val crayonPalette = remember {
        listOf(
            ReadingOasisColors.Coral,
            ReadingOasisColors.Mint,
            ReadingOasisColors.Purple,
            ReadingOasisColors.RoyalBlue
        )
    }
    var selectedCrayon by remember { mutableStateOf(crayonPalette.first()) }
    val completedStrokes = remember(lesson.id, guideIdx) { mutableStateListOf<CrayonStroke>() }
    val activeStrokePoints = remember(lesson.id, guideIdx) { mutableStateListOf<Offset>() }
    val sparkles = remember(lesson.id, guideIdx) { mutableStateListOf<MagicSparkle>() }
    var boardNotice by remember(lesson.id, guideIdx) { mutableStateOf(currentGuide.directionHint) }

    val infiniteTransition = rememberInfiniteTransition(label = "start_dot_pulse")
    val startDotPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "green_dot_scale"
    )

    // تنظيف النجوم السحرية القديمة تدريجياً
    LaunchedEffect(sparkles.size) {
        if (sparkles.isNotEmpty()) {
            delay(650L)
            val cutoff = System.currentTimeMillis() - 620L
            sparkles.removeAll { it.createdAtMs < cutoff }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // منتقي الحرف إذا احتوى الدرس على حرفين
        if (guides.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                guides.forEachIndexed { idx, g ->
                    val chosen = idx == guideIdx
                    PlayfulBouncyButton(
                        onClick = {
                            guideIdx = idx
                            audioEngine.speakText(g.letterName)
                        },
                        containerColor = if (chosen) ReadingOasisColors.Coral else ReadingOasisColors.White,
                        contentColor = if (chosen) ReadingOasisColors.White else ReadingOasisColors.InkDark,
                        cornerRadius = 18.dp,
                        shadowDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(g.letterName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // شريط التوجيه البسيط
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ReadingOasisColors.White)
                .border(2.dp, ReadingOasisColors.Coral.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlayfulCrayonCanvas(color = selectedCrayon, modifier = Modifier.size(28.dp))
            Text(
                text = boardNotice,
                color = ReadingOasisColors.InkDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // سبورة الدفتر المدرسي للرسم بالإصبع عبر Path.quadraticBezierTo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(ReadingOasisColors.CreamPaper)
                .border(4.dp, ReadingOasisColors.SunnyYellow, RoundedCornerShape(28.dp))
                .testTag("reading_writing_canvas_board")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentGuide.letter, selectedCrayon) {
                        detectDragGestures(
                            onDragStart = { startPos ->
                                activeStrokePoints.clear()
                                activeStrokePoints.add(startPos)
                                sparkles.add(
                                    MagicSparkle(
                                        position = startPos,
                                        color = ReadingOasisColors.SunnyYellow,
                                        createdAtMs = System.currentTimeMillis(),
                                        radius = 10f
                                    )
                                )
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val pos = change.position
                                activeStrokePoints.add(pos)
                                if (activeStrokePoints.size % 3 == 0) {
                                    sparkles.add(
                                        MagicSparkle(
                                            position = Offset(
                                                pos.x + ((activeStrokePoints.size % 5) - 2) * 8f,
                                                pos.y - 10f
                                            ),
                                            color = if (activeStrokePoints.size % 2 == 0) {
                                                ReadingOasisColors.SunnyYellow
                                            } else {
                                                selectedCrayon
                                            },
                                            createdAtMs = System.currentTimeMillis(),
                                            radius = 7f + (activeStrokePoints.size % 4) * 2f
                                        )
                                    )
                                    if (sparkles.size > 28) {
                                        sparkles.removeAt(0)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (activeStrokePoints.isNotEmpty()) {
                                    completedStrokes.add(
                                        CrayonStroke(
                                            color = selectedCrayon,
                                            points = activeStrokePoints.toList()
                                        )
                                    )
                                    activeStrokePoints.clear()
                                }
                            },
                            onDragCancel = {
                                activeStrokePoints.clear()
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // حلقات دفتر مدرسي في الأعلى
                val ringCount = 9
                for (i in 1..ringCount) {
                    val rx = w * (i.toFloat() / (ringCount + 1))
                    drawCircle(Color(0xFFCBD5E1), radius = 5.dp.toPx(), center = Offset(rx, 14.dp.toPx()))
                }

                // سطور الدفتر المدرسي الواضحة
                val baselineY = h * currentGuide.baselineRatio
                val topGuideY = h * 0.30f
                val midGuideY = (topGuideY + baselineY) / 2f
                val lowerGuideY = h * 0.85f

                drawLine(
                    color = Color(0xFF93C5FD).copy(alpha = 0.55f),
                    start = Offset(18.dp.toPx(), topGuideY),
                    end = Offset(w - 18.dp.toPx(), topGuideY),
                    strokeWidth = 1.6.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF93C5FD).copy(alpha = 0.45f),
                    start = Offset(18.dp.toPx(), midGuideY),
                    end = Offset(w - 18.dp.toPx(), midGuideY),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
                // سطر الكتابة الأساسي
                drawLine(
                    color = ReadingOasisColors.Coral.copy(alpha = 0.55f),
                    start = Offset(18.dp.toPx(), baselineY),
                    end = Offset(w - 18.dp.toPx(), baselineY),
                    strokeWidth = 2.4.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF93C5FD).copy(alpha = 0.40f),
                    start = Offset(18.dp.toPx(), lowerGuideY),
                    end = Offset(w - 18.dp.toPx(), lowerGuideY),
                    strokeWidth = 1.4.dp.toPx()
                )

                // الحرف المفرّغ كدليل إرشادي شفاف (Alpha = 0.22f)
                val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ReadingOasisColors.InkDark.copy(alpha = 0.22f).toArgb()
                    textSize = h * 0.62f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    currentGuide.letter,
                    w * 0.50f,
                    baselineY,
                    letterPaint
                )

                // مسار إرشادي منقّط لطيف لنقاط التتبع
                if (currentGuide.strokePath.size >= 2) {
                    val guidePath = Path()
                    currentGuide.strokePath.forEachIndexed { idx, pt ->
                        val px = w * pt.x
                        val py = h * pt.y
                        if (idx == 0) guidePath.moveTo(px, py)
                        else guidePath.lineTo(px, py)
                    }
                    drawPath(
                        path = guidePath,
                        color = ReadingOasisColors.SkyBright.copy(alpha = 0.30f),
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 14f), 0f)
                        )
                    )
                }

                // نقطة البداية الخضراء النابضة التي تدل الطفل من أين يبدأ
                val startCenter = Offset(w * currentGuide.startPoint.x, h * currentGuide.startPoint.y)
                drawCircle(
                    color = ReadingOasisColors.Mint.copy(alpha = 0.28f),
                    radius = 18.dp.toPx() * startDotPulse,
                    center = startCenter
                )
                drawCircle(
                    color = ReadingOasisColors.Mint,
                    radius = 10.dp.toPx(),
                    center = startCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = startCenter
                )

                // رسم خطوط القلم الشمعي المكتملة عبر Path.quadraticBezierTo
                val strokeStyle = Stroke(
                    width = 16.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                completedStrokes.forEach { stroke ->
                    drawSmoothQuadraticStroke(stroke.points, stroke.color, strokeStyle)
                }
                // الخط النشط أثناء حركة الإصبع
                if (activeStrokePoints.isNotEmpty()) {
                    drawSmoothQuadraticStroke(activeStrokePoints, selectedCrayon, strokeStyle)
                }

                // نجوم بريق خفيفة تتبع حركة الإصبع
                sparkles.forEach { sp ->
                    val star = Path().apply {
                        val r = sp.radius
                        moveTo(sp.position.x, sp.position.y - r)
                        quadraticBezierTo(sp.position.x, sp.position.y, sp.position.x + r, sp.position.y)
                        quadraticBezierTo(sp.position.x, sp.position.y, sp.position.x, sp.position.y + r)
                        quadraticBezierTo(sp.position.x, sp.position.y, sp.position.x - r, sp.position.y)
                        quadraticBezierTo(sp.position.x, sp.position.y, sp.position.x, sp.position.y - r)
                        close()
                    }
                    drawPath(star, color = sp.color.copy(alpha = 0.85f))
                }
            }
        }

        // 4 ألوان أقلام شمعية زاهية
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            crayonPalette.forEachIndexed { idx, color ->
                val isSelected = selectedCrayon == color
                val liftY by animateDpAsState(
                    targetValue = if (isSelected) (-8).dp else 0.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "crayon_lift_$idx"
                )
                PlayfulBouncyButton(
                    onClick = {
                        selectedCrayon = color
                        audioEngine.playSyllableTapTone()
                    },
                    containerColor = if (isSelected) color else ReadingOasisColors.White,
                    shadowColor = if (isSelected) ReadingOasisColors.darken(color) else Color(0xFFCBD5E1),
                    cornerRadius = 18.dp,
                    shadowDepth = 4.dp,
                    contentPadding = PaddingValues(10.dp),
                    modifier = Modifier
                        .offset(y = liftY)
                        .testTag("reading_crayon_color_$idx")
                ) {
                    PlayfulCrayonCanvas(
                        color = if (isSelected) ReadingOasisColors.White else color,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // أزرار التحكم: مسح | اسمع | أنهيت الكتابة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayfulBouncyButton(
                onClick = {
                    completedStrokes.clear()
                    activeStrokePoints.clear()
                    sparkles.clear()
                    boardNotice = "تم مسح السبورة، ابدأ من النقطة الخضراء!"
                    audioEngine.playSyllableTapTone()
                },
                containerColor = ReadingOasisColors.Pink,
                shadowColor = ReadingOasisColors.PinkShadow,
                cornerRadius = 18.dp,
                shadowDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("reading_write_clear_btn")
            ) {
                PlayfulEraserCanvas(modifier = Modifier.size(20.dp))
                Text("مسح", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }

            PlayfulBouncyButton(
                onClick = {
                    audioEngine.speakText("${currentGuide.letterName}. ${currentGuide.letter}")
                },
                containerColor = ReadingOasisColors.SunnyYellow,
                shadowColor = ReadingOasisColors.SunnyShadow,
                contentColor = ReadingOasisColors.InkDark,
                cornerRadius = 18.dp,
                shadowDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("reading_write_listen_btn")
            ) {
                PlayfulSpeakerCanvas(color = ReadingOasisColors.InkDark, modifier = Modifier.size(20.dp))
                Text("اسمع", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }

            PlayfulBouncyButton(
                onClick = {
                    if (completedStrokes.isEmpty()) {
                        boardNotice = "ارسم الحرف بإصبعك فوق السبورة أولاً يا بطل!"
                        audioEngine.speakText("اكتب الحرف بإصبعك أولاً يا بطل")
                    } else {
                        boardNotice = "أحسنت يا بطل! خطك جميل جداً وحصلت على نجمة!"
                        audioEngine.playSuccessChime()
                        audioEngine.speakText("أحسنت يا بطل! خطك جميل جداً!")
                        onWritingFinished(currentGuide.letter)
                    }
                },
                containerColor = ReadingOasisColors.Mint,
                shadowColor = ReadingOasisColors.MintShadow,
                cornerRadius = 18.dp,
                shadowDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1.35f)
                    .testTag("reading_write_finish_btn")
            ) {
                PlayfulCheckCanvas(modifier = Modifier.size(20.dp))
                Text("أنهيت الكتابة", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothQuadraticStroke(
    points: List<Offset>,
    color: Color,
    style: Stroke
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color = color, radius = style.width / 2f, center = points.first())
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
        val last = points.last()
        lineTo(last.x, last.y)
    }
    drawPath(path = path, color = color, style = style)
}

// ============================================================================
// تبويب 4: "تحدّي" — مغامرة النجوم بالبالونات الملونة وزر "اقرأها بصوتك"
// ============================================================================

private data class ReadingChallengeQuestion(
    val promptLabel: String,
    val targetDisplay: String,
    val targetSpoken: String,
    val options: List<String>
)

@Composable
private fun StarChallengeTab(
    lesson: ReadingLesson,
    studentName: String,
    audioEngine: ReadingAudioEngine,
    isMascotCelebrating: Boolean,
    onAnswerCelebrated: () -> Unit,
    onChallengeCompleted: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val questions = remember(lesson.id) { buildChallengeQuestions(lesson) }
    var currentIndex by remember(lesson.id) { mutableIntStateOf(0) }
    var correctCount by remember(lesson.id) { mutableIntStateOf(0) }
    var isFinished by remember(lesson.id) { mutableStateOf(false) }
    var statusBanner by remember(lesson.id) { mutableStateOf("استمع جيداً واختر البالون الصحيح!") }
    var lockedAfterAnswer by remember(lesson.id, currentIndex) { mutableStateOf(false) }
    var isListeningSpeech by remember { mutableStateOf(false) }

    val currentQuestion = questions[currentIndex % questions.size]

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
        }
    }

    LaunchedEffect(lesson.id, currentIndex, isFinished) {
        if (!isFinished) {
            delay(250L)
            audioEngine.speakText(currentQuestion.targetSpoken)
        }
    }

    fun handleCorrectChoice(spokenSuccess: Boolean = false) {
        if (lockedAfterAnswer || isFinished) return
        lockedAfterAnswer = true
        correctCount += 1
        statusBanner = if (spokenSuccess) {
            "صوتك رائع يا بطل! نطقتها بشكل صحيح!"
        } else {
            "ممتاز يا بطل! إجابة صحيحة!"
        }
        audioEngine.playSuccessChime()
        audioEngine.speakText("أحسنت يا بطل! ${currentQuestion.targetSpoken}")
        onAnswerCelebrated()

        scope.launch {
            delay(1300L)
            if (currentIndex < questions.lastIndex) {
                currentIndex += 1
                lockedAfterAnswer = false
                statusBanner = "استمع للسؤال التالي واختر البالون الصحيح!"
            } else {
                isFinished = true
                val stars = when {
                    correctCount >= questions.size -> 3
                    correctCount >= (questions.size / 2).coerceAtLeast(1) -> 2
                    else -> 1
                }
                audioEngine.playLessonCompleteFanfare()
                onChallengeCompleted(stars)
            }
        }
    }

    fun startArabicSpeechRecognition() {
        val available = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)
        if (!available) {
            statusBanner = "ميزة التعرّف الصوتي غير متوفرة على هذا الجهاز، يمكنك المتابعة باختيار البالون!"
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            statusBanner = "الميكروفون غير مفعّل حالياً، يمكنك المتابعة باختيار البالون الصحيح يا بطل!"
            return
        }

        runCatching {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListeningSpeech = true
                    statusBanner = "تكلّم الآن يا بطل... أنا أستمع إليك!"
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListeningSpeech = false
                }

                override fun onError(error: Int) {
                    isListeningSpeech = false
                    statusBanner = "لم أسمعك بوضوح، جرّب مرة أخرى أو اختر البالون!"
                }

                override fun onResults(results: Bundle?) {
                    isListeningSpeech = false
                    val spokenList = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val expectedNorm = normalizeArabicForMatch(currentQuestion.targetDisplay)
                    val matched = spokenList.any { heard ->
                        val heardNorm = normalizeArabicForMatch(heard)
                        heardNorm.contains(expectedNorm) || expectedNorm.contains(heardNorm)
                    }
                    if (matched) {
                        handleCorrectChoice(spokenSuccess = true)
                    } else {
                        statusBanner = "محاولة جميلة! استمع للكلمة وجرّب نطقها أو اختر البالون"
                        audioEngine.speakText(currentQuestion.targetSpoken)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            recognizer.startListening(intent)
        }.onFailure {
            isListeningSpeech = false
            statusBanner = "يمكنك المتابعة باختيار البالون الصحيح يا بطل!"
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startArabicSpeechRecognition()
        } else {
            statusBanner = "صوتك رائع! يمكنك تفعيل الميكروفون لاحقاً أو المتابعة باختيار البالون الصحيح"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isFinished) {
            // بطاقة التتويج النهائية للبطل
            val finalStars = when {
                correctCount >= questions.size -> 3
                correctCount >= (questions.size / 2).coerceAtLeast(1) -> 2
                else -> 1
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(ReadingOasisColors.White)
                    .border(3.dp, ReadingOasisColors.SunnyYellow, RoundedCornerShape(30.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PlayfulCrownCanvas(modifier = Modifier.size(68.dp))
                ReadingMascotCanvas(isCelebrating = true, modifier = Modifier.size(110.dp))
                Text(
                    text = "مبارك يا ${studentName.trim().ifBlank { "بطل القراءة" }}!",
                    color = ReadingOasisColors.InkDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "أجبت على $correctCount من ${questions.size} بنجاح وحفظنا نجومك الذهبية!",
                    color = ReadingOasisColors.InkMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..3) {
                        PlayfulStarCanvas(
                            filled = finalStars >= i,
                            glowing = finalStars >= i,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                PlayfulBouncyButton(
                    onClick = {
                        currentIndex = 0
                        correctCount = 0
                        isFinished = false
                        lockedAfterAnswer = false
                        statusBanner = "هيا نلعب مغامرة النجوم من جديد!"
                    },
                    containerColor = ReadingOasisColors.Purple,
                    shadowColor = ReadingOasisColors.PurpleShadow,
                    cornerRadius = 22.dp,
                    shadowDepth = 5.dp,
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    modifier = Modifier.testTag("reading_challenge_replay_btn")
                ) {
                    Text("أعد التحدّي", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        } else {
            // بطاقة السؤال وزر الاستماع
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(ReadingOasisColors.White)
                    .border(2.5.dp, ReadingOasisColors.Pink.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReadingMascotCanvas(
                    isCelebrating = isMascotCelebrating,
                    modifier = Modifier.size(72.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "السؤال ${currentIndex + 1} من ${questions.size} • ${currentQuestion.promptLabel}",
                        color = ReadingOasisColors.Pink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = statusBanner,
                        color = ReadingOasisColors.InkDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                PlayfulBouncyButton(
                    onClick = { audioEngine.speakText(currentQuestion.targetSpoken) },
                    containerColor = ReadingOasisColors.SunnyYellow,
                    shadowColor = ReadingOasisColors.SunnyShadow,
                    contentColor = ReadingOasisColors.InkDark,
                    cornerRadius = 18.dp,
                    shadowDepth = 4.dp,
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.testTag("reading_challenge_listen_btn")
                ) {
                    PlayfulSpeakerCanvas(color = ReadingOasisColors.InkDark, modifier = Modifier.size(24.dp))
                }
            }

            // البالونات الثلاثة الملونة للاختيار
            val balloonColors = remember {
                listOf(
                    ReadingOasisColors.Coral,
                    ReadingOasisColors.Purple,
                    ReadingOasisColors.Mint
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentQuestion.options.forEachIndexed { optIdx, optionText ->
                    FloatingChallengeBalloon(
                        text = optionText,
                        balloonColor = balloonColors[optIdx % balloonColors.size],
                        phaseOffset = optIdx * 0.9f,
                        onClick = {
                            if (lockedAfterAnswer) return@FloatingChallengeBalloon
                            if (optionText == currentQuestion.targetDisplay) {
                                handleCorrectChoice(spokenSuccess = false)
                            } else {
                                statusBanner = "قريب جداً! استمع مرة أخرى وحاول يا بطل"
                                audioEngine.speakText("حاول مرة أخرى. ${currentQuestion.targetSpoken}")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reading_challenge_balloon_$optIdx")
                    )
                }
            }

            // زر "اقرأها بصوتك" الاختياري باستخدام Android SpeechRecognizer مع حماية كاملة للصلاحية
            PlayfulBouncyButton(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        startArabicSpeechRecognition()
                    } else {
                        runCatching {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }.onFailure {
                            statusBanner = "يمكنك المتابعة باختيار البالون الصحيح يا بطل!"
                        }
                    }
                },
                containerColor = if (isListeningSpeech) ReadingOasisColors.Mint else ReadingOasisColors.SkyBright,
                shadowColor = if (isListeningSpeech) ReadingOasisColors.MintShadow else ReadingOasisColors.RoyalBlueShadow,
                cornerRadius = 22.dp,
                shadowDepth = 5.dp,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.testTag("reading_challenge_speak_btn")
            ) {
                PlayfulMicCanvas(
                    isListening = isListeningSpeech,
                    color = ReadingOasisColors.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isListeningSpeech) "أستمع لصوتك الآن..." else "اقرأها بصوتك",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * بالون طفولي عائم مرسوم بـ Canvas Path.cubicTo يحمل خيار الإجابة.
 */
@Composable
private fun FloatingChallengeBalloon(
    text: String,
    balloonColor: Color,
    phaseOffset: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "balloon_float")
    val floatWave by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "balloon_y"
    )
    val dy = (floatWave * cos(phaseOffset)).dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(176.dp)
            .offset(y = dy)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.50f
            val cy = h * 0.42f
            val rx = w * 0.46f
            val ry = h * 0.36f

            // خيط البالون المتمايل
            val stringPath = Path().apply {
                moveTo(cx, cy + ry + 6.dp.toPx())
                quadraticBezierTo(cx + 12.dp.toPx(), h * 0.86f, cx - 8.dp.toPx(), h * 0.96f)
            }
            drawPath(
                path = stringPath,
                color = Color(0xFF94A3B8),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )

            // عقدة البالون السفلية
            val knot = Path().apply {
                moveTo(cx - 8.dp.toPx(), cy + ry + 8.dp.toPx())
                lineTo(cx, cy + ry - 2.dp.toPx())
                lineTo(cx + 8.dp.toPx(), cy + ry + 8.dp.toPx())
                close()
            }
            drawPath(knot, color = ReadingOasisColors.darken(balloonColor, 0.80f))

            // جسم البالون المنحني
            val balloonPath = Path().apply {
                moveTo(cx, cy - ry)
                cubicTo(cx + rx * 1.15f, cy - ry, cx + rx * 1.12f, cy + ry * 0.65f, cx, cy + ry)
                cubicTo(cx - rx * 1.12f, cy + ry * 0.65f, cx - rx * 1.15f, cy - ry, cx, cy - ry)
                close()
            }
            drawPath(
                path = balloonPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        balloonColor.copy(alpha = 0.86f),
                        balloonColor,
                        ReadingOasisColors.darken(balloonColor, 0.82f)
                    ),
                    center = Offset(cx - rx * 0.25f, cy - ry * 0.30f),
                    radius = maxOf(rx, ry) * 1.4f
                )
            )

            // لمعة ضوئية بيضاء على سطح البالون
            val highlight = Path().apply {
                moveTo(cx - rx * 0.46f, cy - ry * 0.42f)
                quadraticBezierTo(cx - rx * 0.56f, cy - ry * 0.08f, cx - rx * 0.40f, cy + ry * 0.10f)
            }
            drawPath(
                path = highlight,
                color = Color.White.copy(alpha = 0.42f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Text(
            text = text,
            color = ReadingOasisColors.White,
            fontSize = if (text.length <= 2) 30.sp else 23.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .offset(y = (-14).dp)
                .padding(horizontal = 8.dp)
        )
    }
}

private fun buildChallengeQuestions(lesson: ReadingLesson): List<ReadingChallengeQuestion> {
    val allWordsPool = ReadingCurriculum.lessons.flatMap { it.words }.map { it.fullWord }.distinct()
    val allSyllablesPool = ReadingCurriculum.lessons.flatMap { it.syllables }.map { it.text }.distinct()
    val list = mutableListOf<ReadingChallengeQuestion>()

    // أسئلة من كلمات الدرس
    lesson.words.forEach { w ->
        val distractors = allWordsPool
            .filter { it != w.fullWord }
            .shuffled()
            .take(2)
        list.add(
            ReadingChallengeQuestion(
                promptLabel = "اختر الكلمة الصحيحة",
                targetDisplay = w.fullWord,
                targetSpoken = w.spokenWord,
                options = (listOf(w.fullWord) + distractors).shuffled()
            )
        )
    }

    // إكمال المجموعة إلى 4 أسئلة من مقاطع الدرس
    lesson.syllables.take(3).forEach { syl ->
        if (list.size < 4) {
            val distractors = (lesson.syllables.map { it.text } + allSyllablesPool)
                .distinct()
                .filter { it != syl.text }
                .shuffled()
                .take(2)
            list.add(
                ReadingChallengeQuestion(
                    promptLabel = "اختر المقطع الصحيح",
                    targetDisplay = syl.text,
                    targetSpoken = syl.ttsCleanText,
                    options = (listOf(syl.text) + distractors).shuffled()
                )
            )
        }
    }

    return list.ifEmpty {
        listOf(
            ReadingChallengeQuestion(
                promptLabel = "اختر المقطع الصحيح",
                targetDisplay = lesson.syllables.first().text,
                targetSpoken = lesson.syllables.first().ttsCleanText,
                options = lesson.syllables.take(3).map { it.text }.shuffled()
            )
        )
    }
}

private fun normalizeArabicForMatch(input: String): String {
    return input
        .replace(Regex("[\u064B-\u065F\u0670ـ]"), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace(Regex("\\s+"), "")
        .trim()
}
