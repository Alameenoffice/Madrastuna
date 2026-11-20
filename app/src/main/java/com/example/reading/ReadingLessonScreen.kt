package com.example.reading

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/**
 * شاشة الدرس التفاعلي ذات الركائز الأربع:
 * 1) تعلّم (قطار المقاطع والنطق)
 * 2) ركّب (لعبة تجميع الكلمة)
 * 3) اكتب (سبورة القلم السحري)
 * 4) تحدّي (مغامرة النجوم)
 */
@Composable
fun ReadingLessonScreen(
    lesson: ReadingLesson,
    progress: ReadingProgressState,
    isSpeaking: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onBackToMap: () -> Unit
) {
    var activeTab by remember(lesson.id) { mutableStateOf(ReadingLessonTab.LEARN) }
    var showCelebration by remember(lesson.id) { mutableStateOf(false) }
    var celebrationBannerText by remember(lesson.id) { mutableStateOf("") }

    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            delay(2600L)
            showCelebration = false
        }
    }

    fun triggerVictory(pillarKey: String, spokenPraise: String, banner: String) {
        engine.recordActivityWin(lesson.id, pillarKey)
        engine.playVictoryFanfare()
        engine.speak(spokenPraise)
        celebrationBannerText = banner
        showCelebration = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ReadingColors.SkyTop,
                        ReadingColors.SkyBackground,
                        Color(0xFFF8FCFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // الشريط العلوي الطفولي للدرس
            ReadingLessonTopBar(
                lesson = lesson,
                totalStars = progress.totalStars,
                lessonStars = progress.starsForLesson(lesson.id),
                onBackToMap = {
                    engine.playTapPop()
                    onBackToMap()
                }
            )

            // أزرار التبويبات الأربعة الملونة البارزة
            ReadingLessonTabsBar(
                activeTab = activeTab,
                onSelectTab = { newTab ->
                    engine.playTapPop()
                    activeTab = newTab
                }
            )

            // محتوى التبويب النشط
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    label = "reading_tab_content"
                ) { tab ->
                    when (tab) {
                        ReadingLessonTab.LEARN -> LearnSyllablesTab(
                            lesson = lesson,
                            isSpeaking = isSpeaking,
                            isCelebrating = showCelebration,
                            engine = engine
                        )

                        ReadingLessonTab.ASSEMBLE -> AssembleWordTab(
                            lesson = lesson,
                            isSpeaking = isSpeaking,
                            isCelebrating = showCelebration,
                            engine = engine,
                            onWordCompleted = { fullWord ->
                                triggerVictory(
                                    pillarKey = "assemble",
                                    spokenPraise = "أحسنت يا بطل! $fullWord",
                                    banner = "أحسنت يا بطل! ركّبت كلمة ($fullWord)"
                                )
                            }
                        )

                        ReadingLessonTab.WRITE -> MagicWritingBoardTab(
                            lesson = lesson,
                            isSpeaking = isSpeaking,
                            engine = engine,
                            onWritingFinished = { letter ->
                                triggerVictory(
                                    pillarKey = "write",
                                    spokenPraise = "رائع يا بطل! خطك جميل جداً في كتابة حرف $letter",
                                    banner = "رائع يا بطل! ربحت نجمة الكتابة"
                                )
                            }
                        )

                        ReadingLessonTab.CHALLENGE -> StarChallengeTab(
                            lesson = lesson,
                            isSpeaking = isSpeaking,
                            isCelebrating = showCelebration,
                            engine = engine,
                            onChallengeWon = { wordText ->
                                triggerVictory(
                                    pillarKey = "challenge",
                                    spokenPraise = "ممتاز يا بطل! إجابة صحيحة، $wordText",
                                    banner = "ممتاز يا بطل! ربحت نجمة ذهبية"
                                )
                            }
                        )
                    }
                }
            }
        }

        // طبقة الاحتفال المتناثرة فوق الشاشة عند الفوز
        if (showCelebration) {
            KidsCelebrationCanvas(
                visible = true,
                modifier = Modifier.fillMaxSize()
            )
            if (celebrationBannerText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ReadingColors.SunYellow,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 68.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlayfulCrownIcon(modifier = Modifier.size(28.dp))
                        Text(
                            text = celebrationBannerText,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ReadingColors.InkDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingLessonTopBar(
    lesson: ReadingLesson,
    totalStars: Int,
    lessonStars: Int,
    onBackToMap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PlayfulBouncyButton(
            onClick = onBackToMap,
            backgroundColor = ReadingColors.CoralOrange,
            shadowColor = ReadingColors.CoralShadow,
            cornerRadius = 18.dp,
            lipDepth = 4.dp,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            testTagValue = "reading_lesson_back_btn"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PlayfulBackArrowIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                Text(
                    text = "الخريطة",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }

        // عنوان الدرس والنجوم الثلاث الخاصة به
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "درس: ${lesson.title}",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = ReadingColors.InkDark
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..3) {
                    PlayfulStarIcon(filled = i <= lessonStars, modifier = Modifier.size(16.dp))
                }
            }
        }

        // عداد مجموع النجوم
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                PlayfulStarIcon(filled = true, modifier = Modifier.size(22.dp))
                Text(
                    text = "$totalStars",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = ReadingColors.InkDark
                )
            }
        }
    }
}

@Composable
private fun ReadingLessonTabsBar(
    activeTab: ReadingLessonTab,
    onSelectTab: (ReadingLessonTab) -> Unit
) {
    val tabConfigs = listOf(
        Triple(ReadingLessonTab.LEARN, "تعلّم", ReadingColors.SkyBlue to ReadingColors.SkyBlueShadow),
        Triple(ReadingLessonTab.ASSEMBLE, "ركّب", ReadingColors.MeadowGreen to ReadingColors.MeadowShadow),
        Triple(ReadingLessonTab.WRITE, "اكتب", ReadingColors.BerryPurple to ReadingColors.BerryShadow),
        Triple(ReadingLessonTab.CHALLENGE, "تحدّي", ReadingColors.CoralOrange to ReadingColors.CoralShadow)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabConfigs.forEach { (tab, label, colors) ->
            val selected = activeTab == tab
            val bg = if (selected) colors.first else Color.White
            val shadow = if (selected) colors.second else Color(0xFFCBD5E1)
            val contentTint = if (selected) Color.White else colors.first

            PlayfulBouncyButton(
                onClick = { onSelectTab(tab) },
                backgroundColor = bg,
                shadowColor = shadow,
                cornerRadius = 18.dp,
                lipDepth = if (selected) 5.dp else 4.dp,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f),
                testTagValue = "reading_tab_${tab.name.lowercase()}"
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PlayfulTabIcon(
                        tab = tab,
                        tint = contentTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (selected) Color.White else ReadingColors.InkDark,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================================================
// التبويب 1: تعلّم (بطاقة الحرف العملاق + مقاطع الحرف + قطار المقاطع والنطق)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearnSyllablesTab(
    lesson: ReadingLesson,
    isSpeaking: Boolean,
    isCelebrating: Boolean,
    engine: ReadingAudioAndProgressEngine
) {
    val scrollState = rememberScrollState()
    var letterRippleTick by remember { mutableIntStateOf(0) }
    var activeWordKey by remember { mutableStateOf<String?>(null) }
    var activeSyllableIdx by remember { mutableIntStateOf(-1) }
    var wholeWordHighlighted by remember { mutableStateOf(false) }

    val rippleAnim = rememberInfiniteTransition(label = "letter_ripple_anim")
    val pulseScale by rippleAnim.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale"
    )
    val pulseAlpha by rippleAnim.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    LaunchedEffect(letterRippleTick) {
        if (letterRippleTick > 0) {
            delay(1400L)
            letterRippleTick = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة الحرف الجديد العملاق (92sp) وبجانبه فصيح مع دوائر الموجات الصوتية
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, lesson.islandColor.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .clickable {
                    letterRippleTick += 1
                    engine.playTapPop()
                    engine.speak(lesson.letterNameSpoken)
                }
                .testTag("reading_giant_letter_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(144.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // موجات صوتية نابضة عند الضغط على الحرف
                    if (letterRippleTick > 0 || isSpeaking) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension * 0.42f * pulseScale
                            drawCircle(
                                color = lesson.islandColor.copy(alpha = pulseAlpha),
                                radius = r,
                                style = Stroke(width = 7f)
                            )
                            drawCircle(
                                color = ReadingColors.SunYellow.copy(alpha = pulseAlpha * 0.8f),
                                radius = r * 0.78f,
                                style = Stroke(width = 5f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(lesson.islandColor, lesson.islandColor.copy(alpha = 0.82f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lesson.letter,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (lesson.letter.length > 2) 54.sp else 92.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReadingMascotCanvas(
                        modifier = Modifier.size(84.dp),
                        isCelebrating = isCelebrating,
                        isSpeaking = isSpeaking || letterRippleTick > 0
                    )
                    Text(
                        text = "اضغط على الحرف لتسمعه",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ReadingColors.InkDark,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ReadingColors.SkyBackground
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            PlayfulSpeakerIcon(
                                isSpeaking = letterRippleTick > 0,
                                tint = lesson.islandColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "استمع للحرف",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = lesson.islandColor
                            )
                        }
                    }
                }
            }
        }

        // أزرار المقاطع الملونة القافزة
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "مقاطع الحرف — اضغط على كل مقطع لتسمعه",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = ReadingColors.InkDark
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                lesson.syllables.forEachIndexed { idx, syl ->
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            engine.speak(syl.spokenText)
                        },
                        backgroundColor = syl.color,
                        shadowColor = syl.shadowColor,
                        cornerRadius = 22.dp,
                        lipDepth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        testTagValue = "reading_syllable_btn_$idx"
                    ) {
                        Text(
                            text = syl.text,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // قطار المقاطع لكل كلمة في الدرس
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "قطار المقاطع",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = ReadingColors.InkDark
            )

            lesson.words.forEach { word ->
                val isThisWordActive = activeWordKey == word.fullWord
                SyllableTrainWordCard(
                    word = word,
                    activeSyllableIndex = if (isThisWordActive) activeSyllableIdx else -1,
                    isWholeWordHighlighted = isThisWordActive && wholeWordHighlighted,
                    onTapSyllable = { syl ->
                        engine.playTapPop()
                        engine.speak(syl.spokenText)
                    },
                    onPlaySegmented = {
                        engine.playTapPop()
                        activeWordKey = word.fullWord
                        engine.speakSegmentedWord(
                            word = word,
                            onActiveSyllableIndex = { idx -> activeSyllableIdx = idx },
                            onWholeWordActive = { whole -> wholeWordHighlighted = whole },
                            onFinished = {
                                activeSyllableIdx = -1
                                wholeWordHighlighted = false
                                activeWordKey = null
                            }
                        )
                    }
                )
            }
        }

        // جملة الدرس إن وُجدت
        if (!lesson.sentence.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFFFFBEB),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ReadingColors.SunYellow, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "جملة الدرس",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ReadingColors.SunShadow
                        )
                        Text(
                            text = lesson.sentence,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = ReadingColors.InkDark
                        )
                    }
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            engine.speak(lesson.sentenceSpoken ?: lesson.sentence)
                        },
                        backgroundColor = ReadingColors.SunYellow,
                        shadowColor = ReadingColors.SunShadow,
                        cornerRadius = 18.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        testTagValue = "reading_sentence_play_btn"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlayfulSpeakerIcon(tint = ReadingColors.InkDark, modifier = Modifier.size(20.dp))
                            Text(
                                text = "اقرأ",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ReadingColors.InkDark
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun SyllableTrainWordCard(
    word: SegmentedWord,
    activeSyllableIndex: Int,
    isWholeWordHighlighted: Boolean,
    onTapSyllable: (ColoredSyllable) -> Unit,
    onPlaySegmented: () -> Unit
) {
    val wholeScale by animateFloatAsState(
        targetValue = if (isWholeWordHighlighted) 1.14f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "whole_word_scale"
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // صف القطار: القاطرة + عربات المقاطع المترابطة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayfulTrainEngineCanvas(
                    modifier = Modifier.size(width = 66.dp, height = 56.dp)
                )

                word.syllables.forEachIndexed { idx, syl ->
                    val isLit = idx == activeSyllableIndex
                    val carJump by animateDpAsState(
                        targetValue = if (isLit) (-10).dp else 0.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "train_car_jump_$idx"
                    )
                    val carScale by animateFloatAsState(
                        targetValue = if (isLit) 1.12f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "train_car_scale_$idx"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset(y = carJump)
                            .graphicsLayer {
                                scaleX = carScale
                                scaleY = carScale
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onTapSyllable(syl) }
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isLit) ReadingColors.SunYellow else syl.color)
                                .border(
                                    width = if (isLit) 3.dp else 0.dp,
                                    color = if (isLit) ReadingColors.CoralOrange else Color.Transparent,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = syl.text,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                color = if (isLit) ReadingColors.InkDark else Color.White
                            )
                        }
                        // عجلتا العربة الصغيرتان
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(ReadingColors.InkDark)
                            )
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(ReadingColors.InkDark)
                            )
                        }
                    }

                    if (idx < word.syllables.lastIndex) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 18.dp)
                                .width(10.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ReadingColors.InkSoft)
                        )
                    }
                }
            }

            // السطر السفلي: الكلمة كاملة + زر اسمع بالتقطيع
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isWholeWordHighlighted) ReadingColors.SunYellow else ReadingColors.SkyBackground,
                    modifier = Modifier.graphicsLayer {
                        scaleX = wholeScale
                        scaleY = wholeScale
                    }
                ) {
                    Text(
                        text = word.fullWord,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp,
                        color = ReadingColors.InkDark,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }

                PlayfulBouncyButton(
                    onClick = onPlaySegmented,
                    backgroundColor = ReadingColors.MeadowGreen,
                    shadowColor = ReadingColors.MeadowShadow,
                    cornerRadius = 18.dp,
                    lipDepth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    testTagValue = "reading_play_segmented_${word.fullWord}"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlayfulSpeakerIcon(
                            isSpeaking = activeSyllableIndex >= 0 || isWholeWordHighlighted,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "اسمع بالتقطيع",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// التبويب 2: ركّب (لعبة تجميع الكلمة من المقاطع)
// ============================================================================
private data class AssembleTile(
    val uid: Int,
    val syllable: ColoredSyllable
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssembleWordTab(
    lesson: ReadingLesson,
    isSpeaking: Boolean,
    isCelebrating: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onWordCompleted: (String) -> Unit
) {
    var wordIndex by remember(lesson.id) { mutableIntStateOf(0) }
    val currentWord = lesson.words[wordIndex % lesson.words.size]

    val availableTiles = remember(lesson.id, wordIndex) {
        val required = currentWord.syllables.mapIndexed { idx, syl -> AssembleTile(idx, syl) }
        val distractors = lesson.syllables
            .filter { cand -> currentWord.syllables.none { it.text == cand.text } }
            .take(2)
            .mapIndexed { idx, syl -> AssembleTile(100 + idx, syl) }
        (required + distractors).shuffled()
    }

    var placedTiles by remember(lesson.id, wordIndex) { mutableStateOf<List<AssembleTile>>(emptyList()) }
    var feedbackMessage by remember(lesson.id, wordIndex) { mutableStateOf("اضغط على المقاطع بالترتيب لتكوين الكلمة") }
    var isWordSolved by remember(lesson.id, wordIndex) { mutableStateOf(false) }

    LaunchedEffect(lesson.id, wordIndex) {
        delay(260L)
        engine.speak("ركّب كلمة ${currentWord.spokenWord}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // شريط اختيار الكلمة إذا كان في الدرس أكثر من كلمة
        if (lesson.words.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                lesson.words.forEachIndexed { idx, w ->
                    val selected = idx == wordIndex
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            wordIndex = idx
                        },
                        backgroundColor = if (selected) ReadingColors.BerryPurple else Color.White,
                        shadowColor = if (selected) ReadingColors.BerryShadow else Color(0xFFCBD5E1),
                        cornerRadius = 16.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "كلمة ${idx + 1}",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (selected) Color.White else ReadingColors.InkDark
                        )
                    }
                }
            }
        }

        // بطاقة فصيح وسماع الكلمة المطلوبة
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReadingMascotCanvas(
                    modifier = Modifier.size(76.dp),
                    isCelebrating = isWordSolved || isCelebrating,
                    isSpeaking = isSpeaking
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = feedbackMessage,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isWordSolved) ReadingColors.MeadowShadow else ReadingColors.InkDark
                    )
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            engine.speak(currentWord.spokenWord)
                        },
                        backgroundColor = ReadingColors.SkyBlue,
                        shadowColor = ReadingColors.SkyBlueShadow,
                        cornerRadius = 16.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        testTagValue = "reading_assemble_hear_btn"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlayfulSpeakerIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = "اسمع الكلمة",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // مربعات التجميع الملونة (الهدف)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (isWordSolved) Color(0xFFDCFCE7) else Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 3.dp,
                    color = if (isWordSolved) ReadingColors.MeadowGreen else ReadingColors.SkyBlue.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isWordSolved) {
                    Text(
                        text = currentWord.fullWord,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 54.sp,
                        color = ReadingColors.MeadowShadow
                    )
                    Text(
                        text = "أحسنت يا بطل!",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = ReadingColors.CoralOrange
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        currentWord.syllables.forEachIndexed { slotIdx, expectedSyl ->
                            val placed = placedTiles.getOrNull(slotIdx)
                            Box(
                                modifier = Modifier
                                    .size(width = 86.dp, height = 74.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (placed != null) placed.syllable.color else ReadingColors.SkyBackground
                                    )
                                    .border(
                                        width = 3.dp,
                                        color = if (placed != null) placed.syllable.shadowColor else expectedSyl.color.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = placed?.syllable?.text ?: "؟",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp,
                                    color = if (placed != null) Color.White else ReadingColors.InkSoft.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // بطاقات المقاطع المبعثرة الكبيرة أسفل اللوحة
        if (!isWordSolved) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                availableTiles.forEach { tile ->
                    val alreadyUsed = placedTiles.any { it.uid == tile.uid }
                    PlayfulBouncyButton(
                        onClick = {
                            if (alreadyUsed || isWordSolved) return@PlayfulBouncyButton
                            val nextIndex = placedTiles.size
                            val expected = currentWord.syllables.getOrNull(nextIndex)
                            if (expected != null && tile.syllable.text == expected.text) {
                                val nextList = placedTiles + tile
                                placedTiles = nextList
                                engine.playSuccessChime()
                                engine.speak(tile.syllable.spokenText)
                                if (nextList.size == currentWord.syllables.size) {
                                    isWordSolved = true
                                    feedbackMessage = "أحسنت يا بطل! ركّبت كلمة ${currentWord.fullWord}"
                                    onWordCompleted(currentWord.fullWord)
                                } else {
                                    feedbackMessage = "رائع! اختر المقطع التالي"
                                }
                            } else {
                                engine.playGentleTryAgain()
                                feedbackMessage = "حاول مرة أخرى يا بطل، اسمع الكلمة جيداً"
                            }
                        },
                        backgroundColor = tile.syllable.color,
                        shadowColor = tile.syllable.shadowColor,
                        enabled = !alreadyUsed,
                        cornerRadius = 22.dp,
                        lipDepth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        testTagValue = "reading_assemble_tile_${tile.uid}"
                    ) {
                        Text(
                            text = tile.syllable.text,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // زر الانتقال للكلمة التالية أو إعادة اللعب
            PlayfulBouncyButton(
                onClick = {
                    engine.playTapPop()
                    if (wordIndex + 1 < lesson.words.size) {
                        wordIndex += 1
                    } else {
                        placedTiles = emptyList()
                        isWordSolved = false
                        feedbackMessage = "هيا نركّب الكلمة من جديد!"
                        engine.speak(currentWord.spokenWord)
                    }
                },
                backgroundColor = ReadingColors.MeadowGreen,
                shadowColor = ReadingColors.MeadowShadow,
                cornerRadius = 22.dp,
                lipDepth = 5.dp,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                testTagValue = "reading_assemble_next_btn"
            ) {
                Text(
                    text = if (wordIndex + 1 < lesson.words.size) "الكلمة التالية" else "العب مرة أخرى",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ============================================================================
// التبويب 3: اكتب (سبورة القلم السحري)
// ============================================================================
private data class DrawnStroke(
    val color: Color,
    val points: List<Offset>
)

@Composable
private fun MagicWritingBoardTab(
    lesson: ReadingLesson,
    isSpeaking: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onWritingFinished: (String) -> Unit
) {
    var selectedLetterIdx by remember(lesson.id) { mutableIntStateOf(0) }
    val activeLetter = lesson.traceLetters[selectedLetterIdx % lesson.traceLetters.size]
    val startPoint = lesson.traceStartPoints[activeLetter] ?: TraceStartPoint(0.58f, 0.34f)

    val crayonColors = remember {
        listOf(
            ReadingColors.CoralOrange,
            ReadingColors.SkyBlue,
            ReadingColors.MeadowGreen,
            ReadingColors.BerryPurple
        )
    }
    var selectedColor by remember { mutableStateOf(crayonColors.first()) }
    val completedStrokes = remember(lesson.id, selectedLetterIdx) { mutableStateListOf<DrawnStroke>() }
    val activePoints = remember(lesson.id, selectedLetterIdx) { mutableStateListOf<Offset>() }
    var lastFingerOffset by remember(lesson.id, selectedLetterIdx) { mutableStateOf<Offset?>(null) }

    val pulseTransition = rememberInfiniteTransition(label = "start_dot_pulse")
    val startDotRadius by pulseTransition.animateFloat(
        initialValue = 10f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_radius"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // اختيار الحرف إذا كان الدرس يضم حرفين مثل (ن، ي)
        if (lesson.traceLetters.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                lesson.traceLetters.forEachIndexed { idx, ch ->
                    val selected = idx == selectedLetterIdx
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            selectedLetterIdx = idx
                            engine.speak("حرف $ch")
                        },
                        backgroundColor = if (selected) ReadingColors.BerryPurple else Color.White,
                        shadowColor = if (selected) ReadingColors.BerryShadow else Color(0xFFCBD5E1),
                        cornerRadius = 16.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "اكتب حرف: $ch",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (selected) Color.White else ReadingColors.InkDark
                        )
                    }
                }
            }
        } else {
            Text(
                text = "ابدأ من النقطة الخضراء وتتبّع حرف ($activeLetter) بإصبعك",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = ReadingColors.InkDark,
                textAlign = TextAlign.Center
            )
        }

        // سبورة الدفتر المدرسي المخططة بأسطر واضحة وإطار كرتوني
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(ReadingColors.NotebookPaper)
                .border(5.dp, ReadingColors.SunYellow, RoundedCornerShape(28.dp))
                .pointerInput(selectedColor, activeLetter) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activePoints.clear()
                            activePoints.add(offset)
                            lastFingerOffset = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pt = change.position
                            activePoints.add(pt)
                            lastFingerOffset = pt
                        },
                        onDragEnd = {
                            if (activePoints.isNotEmpty()) {
                                completedStrokes.add(DrawnStroke(selectedColor, activePoints.toList()))
                                activePoints.clear()
                            }
                            lastFingerOffset = null
                        },
                        onDragCancel = {
                            if (activePoints.isNotEmpty()) {
                                completedStrokes.add(DrawnStroke(selectedColor, activePoints.toList()))
                                activePoints.clear()
                            }
                            lastFingerOffset = null
                        }
                    )
                }
                .testTag("reading_magic_writing_canvas")
        ) {
            // أسطر الدفتر المدرسي + نقطة البداية الخضراء النابضة
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val lineYs = listOf(0.24f, 0.44f, 0.64f, 0.82f)
                lineYs.forEachIndexed { idx, frac ->
                    drawLine(
                        color = if (idx == 2) ReadingColors.CoralOrange.copy(alpha = 0.35f) else ReadingColors.SkyBlue.copy(alpha = 0.32f),
                        start = Offset(18f, h * frac),
                        end = Offset(w - 18f, h * frac),
                        strokeWidth = if (idx == 2) 4f else 2.5f
                    )
                }
            }

            // الحرف الإرشادي المفرّغ/الشفاف للتتبع (alpha = 0.22f)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 210.sp,
                    color = ReadingColors.InkDark.copy(alpha = 0.22f),
                    textAlign = TextAlign.Center
                )
            }

            // طبقة الرسم العريض الناعم + النقطة الخضراء النابضة + شرارات النجوم خلف الإصبع
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotPos = Offset(size.width * startPoint.xFraction, size.height * startPoint.yFraction)
                // هالة النقطة الخضراء النابضة
                drawCircle(
                    color = ReadingColors.MeadowGreen.copy(alpha = 0.28f),
                    radius = startDotRadius * 1.65f,
                    center = dotPos
                )
                drawCircle(
                    color = ReadingColors.MeadowGreen,
                    radius = 12f,
                    center = dotPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5f,
                    center = dotPos
                )

                fun drawSmoothStroke(strokeColor: Color, pts: List<Offset>) {
                    if (pts.isEmpty()) return
                    if (pts.size == 1) {
                        drawCircle(color = strokeColor, radius = 14f, center = pts.first())
                        return
                    }
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) {
                            val prev = pts[i - 1]
                            val curr = pts[i]
                            val midX = (prev.x + curr.x) * 0.5f
                            val midY = (prev.y + curr.y) * 0.5f
                            quadraticBezierTo(prev.x, prev.y, midX, midY)
                        }
                        lineTo(pts.last().x, pts.last().y)
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = 28f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                completedStrokes.forEach { stroke ->
                    drawSmoothStroke(stroke.color, stroke.points)
                }
                drawSmoothStroke(selectedColor, activePoints)

                // شرارات نجوم خفيفة تتبع حركة إصبع الطفل أثناء الكتابة
                lastFingerOffset?.let { tip ->
                    val offsets = listOf(
                        Offset(tip.x - 24f, tip.y - 22f),
                        Offset(tip.x + 26f, tip.y - 14f),
                        Offset(tip.x - 16f, tip.y + 26f)
                    )
                    offsets.forEachIndexed { idx, pos ->
                        drawCircle(
                            color = if (idx % 2 == 0) ReadingColors.SunYellow else ReadingColors.CandyPink,
                            radius = 6f - idx,
                            center = pos
                        )
                    }
                }
            }
        }

        // اختيار الألوان الأربعة الزاهية (أقلام التلوين)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            crayonColors.forEachIndexed { idx, crayonColor ->
                val isSelected = selectedColor == crayonColor
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 54.dp else 48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) crayonColor.copy(alpha = 0.22f) else Color.White)
                        .border(
                            width = if (isSelected) 3.dp else 1.5.dp,
                            color = if (isSelected) crayonColor else Color(0xFFCBD5E1),
                            shape = CircleShape
                        )
                        .clickable {
                            engine.playTapPop()
                            selectedColor = crayonColor
                        }
                        .testTag("reading_crayon_color_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    PlayfulCrayonIcon(
                        crayonColor = crayonColor,
                        modifier = Modifier.size(32.dp)
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
                    engine.playTapPop()
                    completedStrokes.clear()
                    activePoints.clear()
                },
                backgroundColor = ReadingColors.CandyPink,
                shadowColor = ReadingColors.CandyShadow,
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
                testTagValue = "reading_write_clear_btn"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PlayfulEraserIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "مسح",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            PlayfulBouncyButton(
                onClick = {
                    engine.playTapPop()
                    engine.speak("حرف $activeLetter")
                },
                backgroundColor = ReadingColors.SkyBlue,
                shadowColor = ReadingColors.SkyBlueShadow,
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f),
                testTagValue = "reading_write_hear_btn"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PlayfulSpeakerIcon(isSpeaking = isSpeaking, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "اسمع",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            PlayfulBouncyButton(
                onClick = {
                    if (completedStrokes.isEmpty()) {
                        engine.playGentleTryAgain()
                        engine.speak("ارسم الحرف بإصبعك أولاً يا بطل")
                    } else {
                        onWritingFinished(activeLetter)
                    }
                },
                backgroundColor = ReadingColors.MeadowGreen,
                shadowColor = ReadingColors.MeadowShadow,
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.weight(1.35f),
                testTagValue = "reading_write_done_btn"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PlayfulCheckIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "أنهيت الكتابة",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================================================
// التبويب 4: تحدّي (مغامرة النجوم بالبالونات + قراءة صوتية اختيارية آمنة)
// ============================================================================
@Composable
private fun StarChallengeTab(
    lesson: ReadingLesson,
    isSpeaking: Boolean,
    isCelebrating: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onChallengeWon: (String) -> Unit
) {
    val context = LocalContext.current
    var questionIndex by remember(lesson.id) { mutableIntStateOf(0) }
    val question = lesson.challenges[questionIndex % lesson.challenges.size]
    var challengeStatus by remember(lesson.id, questionIndex) {
        mutableStateOf(question.promptLabel)
    }
    var solvedCurrent by remember(lesson.id, questionIndex) { mutableStateOf(false) }
    var isListeningVoice by remember { mutableStateOf(false) }

    // SpeechRecognizer اختياري وآمن تماماً
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            }
            speechRecognizer = null
        }
    }

    fun startArabicVoiceRead() {
        runCatching {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                challengeStatus = "ميزة الميكروفون غير متوفرة على هذا الجهاز، يمكنك اختيار البالون الصحيح"
                return
            }
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListeningVoice = true
                    challengeStatus = "تحدث الآن واقرأ الكلمة بصوتك الجميل..."
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListeningVoice = false
                }
                override fun onError(error: Int) {
                    isListeningVoice = false
                    challengeStatus = "لم أسمع الصوت بوضوح، حاول مرة أخرى أو اضغط على البالون"
                }
                override fun onResults(results: Bundle?) {
                    isListeningVoice = false
                    val spokenList = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val normalizedTarget = normalizeArabicForMatch(question.correctOption)
                    val matched = spokenList.any { candidate ->
                        val normCand = normalizeArabicForMatch(candidate)
                        normCand.contains(normalizedTarget) || normalizedTarget.contains(normCand) && normCand.isNotEmpty()
                    }
                    if (matched) {
                        solvedCurrent = true
                        challengeStatus = "أبدعت يا بطل! قرأت (${question.correctOption}) بصوتك بشكل صحيح"
                        onChallengeWon(question.correctOption)
                    } else {
                        engine.playGentleTryAgain()
                        challengeStatus = "قريب جداً يا بطل! حاول قراءة (${question.correctOption}) مرة أخرى"
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
            isListeningVoice = false
            challengeStatus = "يمكنك الاستمتاع بالتحدي عبر الضغط على البالونات الملونة"
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startArabicVoiceRead()
        } else {
            challengeStatus = "يمكنك اللعب بالضغط على البالونات الملونة مباشرة"
        }
    }

    LaunchedEffect(lesson.id, questionIndex) {
        delay(280L)
        engine.speak(question.spokenTarget)
    }

    val balloonColors = remember {
        listOf(
            ReadingColors.CoralOrange to ReadingColors.CoralShadow,
            ReadingColors.BerryPurple to ReadingColors.BerryShadow,
            ReadingColors.MeadowGreen to ReadingColors.MeadowShadow
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // بطاقة فصيح والسؤال الصوتي
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReadingMascotCanvas(
                    modifier = Modifier.size(80.dp),
                    isCelebrating = solvedCurrent || isCelebrating,
                    isSpeaking = isSpeaking || isListeningVoice
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "سؤال ${questionIndex + 1} من ${lesson.challenges.size}",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ReadingColors.BerryPurple
                    )
                    Text(
                        text = challengeStatus,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ReadingColors.InkDark
                    )
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            engine.speak(question.spokenTarget)
                        },
                        backgroundColor = ReadingColors.SunYellow,
                        shadowColor = ReadingColors.SunShadow,
                        cornerRadius = 16.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        testTagValue = "reading_challenge_hear_btn"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlayfulSpeakerIcon(
                                isSpeaking = isSpeaking,
                                tint = ReadingColors.InkDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "اسمع المطلوب",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = ReadingColors.InkDark
                            )
                        }
                    }
                }
            }
        }

        // البالونات الملونة الثلاثة للاختيار
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            question.options.forEachIndexed { idx, optionText ->
                val (bg, shadow) = balloonColors[idx % balloonColors.size]
                val isCorrectChoice = optionText == question.correctOption
                val effectiveBg = if (solvedCurrent && isCorrectChoice) ReadingColors.SunYellow else bg
                val effectiveShadow = if (solvedCurrent && isCorrectChoice) ReadingColors.SunShadow else shadow

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    PlayfulBouncyButton(
                        onClick = {
                            if (solvedCurrent) return@PlayfulBouncyButton
                            if (isCorrectChoice) {
                                solvedCurrent = true
                                challengeStatus = "ممتاز يا بطل! الإجابة هي ($optionText)"
                                onChallengeWon(optionText)
                            } else {
                                engine.playGentleTryAgain()
                                engine.speak("حاول مرة أخرى، اسمع جيداً: ${question.spokenTarget}")
                                challengeStatus = "حاول مرة أخرى يا بطل!"
                            }
                        },
                        backgroundColor = effectiveBg,
                        shadowColor = effectiveShadow,
                        cornerRadius = 46.dp,
                        lipDepth = 6.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(116.dp),
                        testTagValue = "reading_challenge_balloon_$idx"
                    ) {
                        Text(
                            text = optionText,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (optionText.length >= 4) 24.sp else 32.sp,
                            color = if (solvedCurrent && isCorrectChoice) ReadingColors.InkDark else Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    // خيط البالون المتموج المرسوم بـ Canvas
                    Canvas(modifier = Modifier.size(width = 24.dp, height = 34.dp)) {
                        val stringPath = Path().apply {
                            moveTo(size.width * 0.5f, 0f)
                            quadraticBezierTo(size.width * 0.85f, size.height * 0.45f, size.width * 0.45f, size.height)
                        }
                        drawPath(
                            stringPath,
                            color = ReadingColors.InkSoft.copy(alpha = 0.55f),
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        // زر اختياري: اقرأها بصوتك (مع فحص إذن RECORD_AUDIO بأمان)
        PlayfulBouncyButton(
            onClick = {
                engine.playTapPop()
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    startArabicVoiceRead()
                } else {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            backgroundColor = if (isListeningVoice) ReadingColors.CandyPink else ReadingColors.SkyBlue,
            shadowColor = if (isListeningVoice) ReadingColors.CandyShadow else ReadingColors.SkyBlueShadow,
            cornerRadius = 22.dp,
            lipDepth = 5.dp,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth(),
            testTagValue = "reading_challenge_voice_btn"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayfulMicIcon(
                    isListening = isListeningVoice,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isListeningVoice) "أستمع إليك الآن..." else "اقرأها بصوتك",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        }

        // زر التحدي التالي بعد الفوز
        if (solvedCurrent) {
            PlayfulBouncyButton(
                onClick = {
                    engine.playTapPop()
                    questionIndex = (questionIndex + 1) % lesson.challenges.size
                },
                backgroundColor = ReadingColors.MeadowGreen,
                shadowColor = ReadingColors.MeadowShadow,
                cornerRadius = 22.dp,
                lipDepth = 5.dp,
                contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp),
                testTagValue = "reading_challenge_next_btn"
            ) {
                Text(
                    text = if (questionIndex + 1 < lesson.challenges.size) "التحدي التالي" else "تحدّي جديد",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

private fun normalizeArabicForMatch(raw: String): String {
    return raw.trim()
        .replace(Regex("[\u064B-\u0652\u0640]"), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .replace(" ", "")
}
