package com.example.reading

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.Rect
import android.graphics.Typeface
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import kotlin.math.hypot
import kotlin.math.min
import kotlinx.coroutines.delay

/**
 * شاشة الدرس التفاعلي المتكيفة مع المستويات القرائية الثلاثة بخط تجوال (Tajawal):
 * 1) تعلّم (المقاطع / التشكيل / القواعد + قطار الكلمات والجمل + فقرة القراءة)
 * 2) ركّب (تركيب الكلمات للمستوى الأول، وتركيب الجمل والتراكيب للمستويين الثاني والثالث)
 * 3) اكتب (سبورة القلم السحري بتحقق هندسي حقيقي عبر Bitmap 160x160 للحرف أو الكلمة الكاملة)
 * 4) تحدّي (مغامرة النجوم والإملاء وفهم المقروء + قراءة صوتية اختيارية)
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

    fun triggerVictory(
        pillarKey: String,
        rewardKey: String,
        banner: String,
        repeatBanner: String = "أحسنت! أعدت التدريب بنجاح، تم احتساب نجوم هذا النشاط مسبقاً",
        vararg spokenParts: String
    ): Boolean {
        val awarded = engine.recordActivityWin(
            lessonId = lesson.id,
            pillarKey = pillarKey,
            rewardKey = rewardKey,
            stars = 1
        )
        if (awarded) {
            engine.playVictoryFanfare()
            celebrationBannerText = banner
        } else {
            engine.playSuccessChime()
            celebrationBannerText = repeatBanner
        }
        engine.speakSequence(*spokenParts)
        showCelebration = true
        return awarded
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
            ReadingLessonTopBar(
                lesson = lesson,
                totalStars = progress.totalStars,
                lessonStars = progress.starsForLesson(lesson.id),
                onBackToMap = {
                    engine.playTapPop()
                    onBackToMap()
                }
            )

            ReadingLessonTabsBar(
                activeTab = activeTab,
                onSelectTab = { newTab ->
                    engine.playTapPop()
                    activeTab = newTab
                }
            )

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
                            onWordCompleted = { fullText ->
                                triggerVictory(
                                    pillarKey = "assemble",
                                    rewardKey = "assemble_${lesson.id}_${fullText}",
                                    banner = "أحسنت يا بطل! أتممت التركيب بنجاح",
                                    repeatBanner = "أحسنت! أعدت التدريب بنجاح، تم احتساب نجوم هذه الكلمة مسبقاً",
                                    "أحسنت يا بطل!",
                                    fullText
                                )
                            }
                        )

                        ReadingLessonTab.WRITE -> MagicWritingBoardTab(
                            lesson = lesson,
                            isSpeaking = isSpeaking,
                            engine = engine,
                            onWritingFinished = { writtenTarget ->
                                val prefix = if (writtenTarget.length <= 2) "حرف" else "كلمة"
                                triggerVictory(
                                    pillarKey = "write",
                                    rewardKey = "write_${lesson.id}_${writtenTarget}",
                                    banner = "رائع يا بطل! ربحت نجمة الكتابة",
                                    repeatBanner = "أحسنت! أعدت التدريب بنجاح، تم احتساب نجوم هذا الحرف مسبقاً",
                                    "رائع جداً!",
                                    prefix,
                                    writtenTarget
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
                                    rewardKey = "challenge_${lesson.id}_${wordText}",
                                    banner = "ممتاز يا بطل! ربحت نجمة ذهبية",
                                    repeatBanner = "أحسنت! أعدت التدريب بنجاح، تم احتساب نجوم هذا التحدي مسبقاً",
                                    "ممتاز يا بطل!",
                                    wordText
                                )
                            }
                        )
                    }
                }
            }
        }

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
                            fontFamily = ReadingTajawalFont,
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
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = lesson.title,
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = ReadingColors.InkDark
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..3) {
                    PlayfulStarIcon(filled = i <= lessonStars, modifier = Modifier.size(16.dp))
                }
            }
        }

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
                    fontFamily = ReadingTajawalFont,
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
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp),
                testTagValue = "reading_tab_${tab.name.lowercase()}"
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PlayfulTabIcon(
                        tab = tab,
                        tint = contentTint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = if (selected) Color.White else ReadingColors.InkDark,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ============================================================================
// التبويب 1: تعلّم
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
        // بطاقة محور الدرس العملاقة وبجانبها فصيح مع موجات صوتية
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (lesson.level != ReadingLevel.LEVEL_1) {
                                LessonMiniIllustrationCanvas(
                                    artType = lesson.artType,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = lesson.letter,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = when {
                                    lesson.level != ReadingLevel.LEVEL_1 -> 28.sp
                                    lesson.letter.length > 2 -> 52.sp
                                    else -> 90.sp
                                },
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
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
                        text = if (lesson.level == ReadingLevel.LEVEL_1) {
                            "اضغط على الحرف لتسمعه"
                        } else {
                            "اضغط لتسمع شرح الدرس"
                        },
                        fontFamily = ReadingTajawalFont,
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
                                text = "استمع الآن",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = lesson.islandColor
                            )
                        }
                    }
                }
            }
        }

        // بطاقات المقاطع / الأمثلة المشكّلة بمساحة واسعة (minWidth = 76.dp, height = 76.dp)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = lesson.level.learnCardsHeading,
                fontFamily = ReadingTajawalFont,
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
                    val isLongExample = syl.text.length > 3
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            engine.speak(syl.spokenText)
                        },
                        backgroundColor = syl.color,
                        shadowColor = syl.shadowColor,
                        cornerRadius = 22.dp,
                        lipDepth = 5.dp,
                        contentPadding = PaddingValues(
                            horizontal = if (isLongExample) 14.dp else 4.dp,
                            vertical = 4.dp
                        ),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 76.dp, minHeight = 76.dp)
                            .height(76.dp),
                        testTagValue = "reading_syllable_btn_$idx"
                    ) {
                        Text(
                            text = syl.text,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isLongExample) 22.sp else 28.sp,
                            lineHeight = 40.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // قطار المقاطع / الكلمات والجمل
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = lesson.level.trainHeading,
                fontFamily = ReadingTajawalFont,
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

        // جملة الدرس أو الفقرة القصصية/العلمية المشكّلة
        if (!lesson.sentence.isNullOrBlank()) {
            val isParagraph = lesson.sentence.length > 36
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFFFFBEB),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ReadingColors.SunYellow, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lesson.level.sentenceHeading,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = ReadingColors.SunShadow
                        )
                        PlayfulBouncyButton(
                            onClick = {
                                engine.playTapPop()
                                engine.speak(lesson.sentenceSpoken ?: lesson.sentence)
                            },
                            backgroundColor = ReadingColors.SunYellow,
                            shadowColor = ReadingColors.SunShadow,
                            cornerRadius = 18.dp,
                            lipDepth = 4.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            testTagValue = "reading_sentence_play_btn"
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PlayfulSpeakerIcon(tint = ReadingColors.InkDark, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "استمع للقراءة",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = ReadingColors.InkDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Text(
                        text = lesson.sentence,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isParagraph) 20.sp else 28.sp,
                        lineHeight = if (isParagraph) 34.sp else 40.sp,
                        color = ReadingColors.InkDark
                    )
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
        targetValue = if (isWholeWordHighlighted) 1.08f else 1f,
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
                        targetValue = if (isLit) 1.10f else 1f,
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
                                .defaultMinSize(minWidth = 64.dp, minHeight = 56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isLit) ReadingColors.SunYellow else syl.color)
                                .border(
                                    width = if (isLit) 3.dp else 0.dp,
                                    color = if (isLit) ReadingColors.CoralOrange else Color.Transparent,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = syl.text,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (syl.text.length > 4) 20.sp else 26.sp,
                                lineHeight = 36.sp,
                                color = if (isLit) ReadingColors.InkDark else Color.White
                            )
                        }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isWholeWordHighlighted) ReadingColors.SunYellow else ReadingColors.SkyBackground,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp)
                        .graphicsLayer {
                            scaleX = wholeScale
                            scaleY = wholeScale
                        }
                ) {
                    Text(
                        text = word.fullWord,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (word.fullWord.length > 10) 20.sp else 28.sp,
                        lineHeight = 34.sp,
                        color = ReadingColors.InkDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                PlayfulBouncyButton(
                    onClick = onPlaySegmented,
                    backgroundColor = ReadingColors.MeadowGreen,
                    shadowColor = ReadingColors.MeadowShadow,
                    cornerRadius = 18.dp,
                    lipDepth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
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
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// التبويب 2: ركّب (تركيب الكلمات للمستوى الأول، وتركيب الجمل للمستويين الثاني والثالث)
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
    val isSentenceLevel = lesson.level != ReadingLevel.LEVEL_1

    val availableTiles = remember(lesson.id, wordIndex) {
        val required = currentWord.syllables.mapIndexed { idx, syl -> AssembleTile(idx, syl) }
        val distractors = lesson.syllables
            .filter { cand -> currentWord.syllables.none { it.text == cand.text } }
            .take(if (isSentenceLevel) 1 else 2)
            .mapIndexed { idx, syl -> AssembleTile(100 + idx, syl) }
        (required + distractors).shuffled()
    }

    var placedTiles by remember(lesson.id, wordIndex) { mutableStateOf<List<AssembleTile>>(emptyList()) }
    var feedbackMessage by remember(lesson.id, wordIndex) { mutableStateOf(lesson.level.assemblePrompt) }
    var isWordSolved by remember(lesson.id, wordIndex) { mutableStateOf(false) }

    LaunchedEffect(lesson.id, wordIndex) {
        delay(260L)
        engine.speakSequence(
            if (isSentenceLevel) "ركّب الجملة" else "ركّب كلمة",
            currentWord.spokenWord
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
        if (lesson.words.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                lesson.words.forEachIndexed { idx, _ ->
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
                            text = if (isSentenceLevel) "جملة ${idx + 1}" else "كلمة ${idx + 1}",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (selected) Color.White else ReadingColors.InkDark
                        )
                    }
                }
            }
        }

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
                        fontFamily = ReadingTajawalFont,
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
                                text = if (isSentenceLevel) "اسمع الجملة" else "اسمع الكلمة",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

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
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (currentWord.fullWord.length > 12) 28.sp else 48.sp,
                        lineHeight = 44.sp,
                        color = ReadingColors.MeadowShadow,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "أحسنت يا بطل!",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = ReadingColors.CoralOrange
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentWord.syllables.forEachIndexed { slotIdx, expectedSyl ->
                            val placed = placedTiles.getOrNull(slotIdx)
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = if (isSentenceLevel) 92.dp else 84.dp, minHeight = 72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (placed != null) placed.syllable.color else ReadingColors.SkyBackground
                                    )
                                    .border(
                                        width = 3.dp,
                                        color = if (placed != null) placed.syllable.shadowColor else expectedSyl.color.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = placed?.syllable?.text ?: "؟",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isSentenceLevel) 22.sp else 30.sp,
                                    lineHeight = 36.sp,
                                    color = if (placed != null) Color.White else ReadingColors.InkSoft.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isWordSolved) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                availableTiles.forEach { tile ->
                    val alreadyUsed = placedTiles.any { it.uid == tile.uid }
                    val isLongTile = tile.syllable.text.length > 3
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
                                    feedbackMessage = "أحسنت يا بطل! ${currentWord.fullWord}"
                                    onWordCompleted(currentWord.fullWord)
                                } else {
                                    feedbackMessage = "رائع! اختر الجزء التالي"
                                }
                            } else {
                                engine.playGentleTryAgain()
                                feedbackMessage = "حاول مرة أخرى يا بطل، اسمع جيداً"
                            }
                        },
                        backgroundColor = tile.syllable.color,
                        shadowColor = tile.syllable.shadowColor,
                        enabled = !alreadyUsed,
                        cornerRadius = 22.dp,
                        lipDepth = 5.dp,
                        contentPadding = PaddingValues(
                            horizontal = if (isLongTile) 16.dp else 4.dp,
                            vertical = 4.dp
                        ),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 78.dp, minHeight = 76.dp)
                            .height(76.dp),
                        testTagValue = "reading_assemble_tile_${tile.uid}"
                    ) {
                        Text(
                            text = tile.syllable.text,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isLongTile) 22.sp else 28.sp,
                            lineHeight = 40.sp,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        } else {
            PlayfulBouncyButton(
                onClick = {
                    engine.playTapPop()
                    if (wordIndex + 1 < lesson.words.size) {
                        wordIndex += 1
                    } else {
                        placedTiles = emptyList()
                        isWordSolved = false
                        feedbackMessage = "هيا نلعب من جديد!"
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
                    text = if (wordIndex + 1 < lesson.words.size) {
                        if (isSentenceLevel) "الجملة التالية" else "الكلمة التالية"
                    } else {
                        "العب مرة أخرى"
                    },
                    fontFamily = ReadingTajawalFont,
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
// التبويب 3: اكتب (سبورة القلم السحري + تقييم هندسي حقيقي عبر Bitmap 160x160)
// ============================================================================
private data class DrawnStroke(
    val color: Color,
    val points: List<Offset>
)

private data class TracingEvaluationResult(
    val passed: Boolean,
    val coverageRatio: Float,
    val inBoundsPrecisionRatio: Float
)

private data class GuideTextLayout(
    val textSizePx: Float,
    val drawX: Float,
    val drawY: Float,
    val bounds: Rect
)

/**
 * تحميل خط Tajawal كـ Typeface أصلي لرسم الحرف أو الكلمة الإرشادية على السبورة وقناع التحقق بنفس المحاذاة تماماً.
 */
private fun resolveTajawalTypeface(context: Context): Typeface {
    val cachedBold = File(context.filesDir, "Tajawal-ExtraBold.ttf")
        .takeIf { it.exists() && it.length() > 10_000L }
        ?: File(context.filesDir, "Tajawal-Bold.ttf")
            .takeIf { it.exists() && it.length() > 10_000L }
    if (cachedBold != null) {
        runCatching { return Typeface.createFromFile(cachedBold) }
    }
    return runCatching {
        val base = ResourcesCompat.getFont(context, R.font.tajawal)
        if (base != null) Typeface.create(base, Typeface.BOLD) else Typeface.DEFAULT_BOLD
    }.getOrDefault(Typeface.DEFAULT_BOLD)
}

/**
 * حساب حجم وتوسيط الحرف المفرد أو الكلمة الكاملة بدقة متطابقة بين السبورة وقناع التقييم 160x160.
 */
private fun computeGuideTextLayout(
    targetText: String,
    boardWidth: Float,
    boardHeight: Float,
    typeface: Typeface
): GuideTextLayout {
    val minBoardDim = min(boardWidth, boardHeight)
    var candidateSize = minBoardDim * if (targetText.length <= 2) 0.62f else 0.38f
    val measurePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = candidateSize
        textAlign = AndroidPaint.Align.LEFT
    }
    val bounds = Rect()
    measurePaint.getTextBounds(targetText, 0, targetText.length, bounds)

    val maxAllowedW = boardWidth * 0.82f
    val maxAllowedH = boardHeight * 0.72f
    if (bounds.width() > maxAllowedW && bounds.width() > 0) {
        candidateSize *= (maxAllowedW / bounds.width().toFloat())
    }
    if (bounds.height() > maxAllowedH && bounds.height() > 0) {
        candidateSize *= (maxAllowedH / bounds.height().toFloat())
    }

    measurePaint.textSize = candidateSize
    measurePaint.getTextBounds(targetText, 0, targetText.length, bounds)

    val drawX = boardWidth * 0.5f - bounds.exactCenterX()
    val drawY = boardHeight * 0.5f - bounds.exactCenterY()
    return GuideTextLayout(
        textSizePx = candidateSize,
        drawX = drawX,
        drawY = drawY,
        bounds = bounds
    )
}

/**
 * دالة التقييم الهندسي الحقيقي لكتابة الطفل باستخدام Bitmap بحجم 160x160 في الذاكرة:
 * - تحسب نسبة التغطية (coverage >= 52%) من جسم الحرف أو الكلمة الإرشادية.
 * - تحسب نسبة الدقة داخل الحدود (precision >= 58%) لمنع نجاح الخربشة العشوائية أو الرسم خارج الحرف.
 */
private fun evaluateLetterTracingWithBitmap(
    targetText: String,
    strokes: List<DrawnStroke>,
    boardSize: IntSize,
    typeface: Typeface
): TracingEvaluationResult {
    if (strokes.isEmpty() || boardSize.width <= 0 || boardSize.height <= 0) {
        return TracingEvaluationResult(passed = false, coverageRatio = 0f, inBoundsPrecisionRatio = 0f)
    }

    val maskSize = 160
    val scaleX = maskSize.toFloat() / boardSize.width.toFloat()
    val scaleY = maskSize.toFloat() / boardSize.height.toFloat()
    val minBoardDim = min(boardSize.width, boardSize.height).toFloat()

    val layout = computeGuideTextLayout(
        targetText = targetText,
        boardWidth = boardSize.width.toFloat(),
        boardHeight = boardSize.height.toFloat(),
        typeface = typeface
    )

    val guideExactBitmap = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)
    val guideToleranceBitmap = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)
    val userCoverageBitmap = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)
    val userCoreBitmap = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)

    return try {
        val guideCanvas = AndroidCanvas(guideExactBitmap)
        guideCanvas.save()
        guideCanvas.scale(scaleX, scaleY)
        val guidePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = layout.textSizePx
            color = AndroidColor.WHITE
            style = AndroidPaint.Style.FILL
        }
        guideCanvas.drawText(targetText, layout.drawX, layout.drawY, guidePaint)
        guideCanvas.restore()

        val toleranceCanvas = AndroidCanvas(guideToleranceBitmap)
        toleranceCanvas.save()
        toleranceCanvas.scale(scaleX, scaleY)
        val tolerancePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = layout.textSizePx
            color = AndroidColor.WHITE
            style = AndroidPaint.Style.FILL_AND_STROKE
            strokeWidth = minBoardDim * if (targetText.length <= 2) 0.14f else 0.12f
            strokeJoin = AndroidPaint.Join.ROUND
            strokeCap = AndroidPaint.Cap.ROUND
        }
        toleranceCanvas.drawText(targetText, layout.drawX, layout.drawY, tolerancePaint)
        toleranceCanvas.restore()

        fun renderUserStrokes(targetBitmap: Bitmap, strokeWidthPx: Float) {
            val canvas = AndroidCanvas(targetBitmap)
            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                style = AndroidPaint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }
            val dotPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                style = AndroidPaint.Style.FILL
            }
            strokes.forEach { stroke ->
                val pts = stroke.points
                if (pts.isEmpty()) return@forEach
                if (pts.size == 1) {
                    canvas.drawCircle(pts[0].x * scaleX, pts[0].y * scaleY, strokeWidthPx * 0.5f, dotPaint)
                } else {
                    val path = AndroidPath().apply {
                        moveTo(pts[0].x * scaleX, pts[0].y * scaleY)
                        for (i in 1 until pts.size) {
                            val prevX = pts[i - 1].x * scaleX
                            val prevY = pts[i - 1].y * scaleY
                            val currX = pts[i].x * scaleX
                            val currY = pts[i].y * scaleY
                            quadTo(prevX, prevY, (prevX + currX) * 0.5f, (prevY + currY) * 0.5f)
                        }
                        lineTo(pts.last().x * scaleX, pts.last().y * scaleY)
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }

        val coverageStroke = if (targetText.length <= 2) 19f else 15f
        val coreStroke = if (targetText.length <= 2) 10f else 8f
        renderUserStrokes(userCoverageBitmap, strokeWidthPx = coverageStroke)
        renderUserStrokes(userCoreBitmap, strokeWidthPx = coreStroke)

        val totalPixels = maskSize * maskSize
        val guidePixelsArr = IntArray(totalPixels)
        val tolPixelsArr = IntArray(totalPixels)
        val userCovArr = IntArray(totalPixels)
        val userCoreArr = IntArray(totalPixels)

        guideExactBitmap.getPixels(guidePixelsArr, 0, maskSize, 0, 0, maskSize, maskSize)
        guideToleranceBitmap.getPixels(tolPixelsArr, 0, maskSize, 0, 0, maskSize, maskSize)
        userCoverageBitmap.getPixels(userCovArr, 0, maskSize, 0, 0, maskSize, maskSize)
        userCoreBitmap.getPixels(userCoreArr, 0, maskSize, 0, 0, maskSize, maskSize)

        var guideCount = 0
        var coveredGuideCount = 0
        var userCoreCount = 0
        var inBoundsUserCount = 0

        for (i in 0 until totalPixels) {
            val isGuide = (guidePixelsArr[i] ushr 24) > 80
            val isTol = (tolPixelsArr[i] ushr 24) > 80
            val isUserCov = (userCovArr[i] ushr 24) > 80
            val isUserCore = (userCoreArr[i] ushr 24) > 80

            if (isGuide) {
                guideCount++
                if (isUserCov) coveredGuideCount++
            }
            if (isUserCore) {
                userCoreCount++
                if (isTol) inBoundsUserCount++
            }
        }

        if (guideCount < 30 || userCoreCount < 45) {
            TracingEvaluationResult(passed = false, coverageRatio = 0f, inBoundsPrecisionRatio = 0f)
        } else {
            val coverage = coveredGuideCount.toFloat() / guideCount.toFloat()
            val precision = inBoundsUserCount.toFloat() / userCoreCount.toFloat()
            val passed = coverage >= 0.52f && precision >= 0.58f
            TracingEvaluationResult(
                passed = passed,
                coverageRatio = coverage,
                inBoundsPrecisionRatio = precision
            )
        }
    } finally {
        guideExactBitmap.recycle()
        guideToleranceBitmap.recycle()
        userCoverageBitmap.recycle()
        userCoreBitmap.recycle()
    }
}

private fun computeTotalStrokeLengthPx(
    strokes: List<DrawnStroke>,
    activePts: List<Offset>
): Float {
    var total = 0f
    fun accumulate(pts: List<Offset>) {
        for (i in 1 until pts.size) {
            total += hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
        }
    }
    strokes.forEach { accumulate(it.points) }
    accumulate(activePts)
    return total
}

@Composable
private fun MagicWritingBoardTab(
    lesson: ReadingLesson,
    isSpeaking: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onWritingFinished: (String) -> Boolean
) {
    val context = LocalContext.current
    val tajawalTypeface = remember(context) { resolveTajawalTypeface(context) }

    var selectedLetterIdx by remember(lesson.id) { mutableIntStateOf(0) }
    val activeTarget = lesson.traceLetters[selectedLetterIdx % lesson.traceLetters.size]
    val explicitStartPoint = lesson.traceStartPoints[activeTarget]

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
    var boardPixelSize by remember { mutableStateOf(IntSize.Zero) }

    var guidanceMessage by remember(lesson.id, selectedLetterIdx) {
        mutableStateOf("${lesson.level.writePromptPrefix} ($activeTarget)")
    }
    var isWarningFeedback by remember(lesson.id, selectedLetterIdx) { mutableStateOf(false) }
    var startDotFlashActive by remember(lesson.id, selectedLetterIdx) { mutableStateOf(false) }
    var hasCompletedCurrentStep by remember(lesson.id, selectedLetterIdx) { mutableStateOf(false) }
    var isSubmittingStep by remember(lesson.id, selectedLetterIdx) { mutableStateOf(false) }

    val totalDrawnLengthPx = computeTotalStrokeLengthPx(completedStrokes, activePoints)
    val hasSufficientDrawing = (completedStrokes.isNotEmpty() || activePoints.size >= 2) && totalDrawnLengthPx >= 72f

    LaunchedEffect(startDotFlashActive) {
        if (startDotFlashActive) {
            delay(1700L)
            startDotFlashActive = false
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "start_dot_pulse")
    val startDotRadius by pulseTransition.animateFloat(
        initialValue = 10f,
        targetValue = 19f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (startDotFlashActive) 260 else 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_radius"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (lesson.traceLetters.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                lesson.traceLetters.forEachIndexed { idx, target ->
                    val selected = idx == selectedLetterIdx
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            selectedLetterIdx = idx
                            engine.speak(target)
                        },
                        backgroundColor = if (selected) ReadingColors.BerryPurple else Color.White,
                        shadowColor = if (selected) ReadingColors.BerryShadow else Color(0xFFCBD5E1),
                        cornerRadius = 16.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "اكتب: $target",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (selected) Color.White else ReadingColors.InkDark
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isWarningFeedback) Color(0xFFFFF1E6) else Color.White.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.8.dp,
                    color = if (isWarningFeedback) ReadingColors.CoralOrange else ReadingColors.SkyBlue.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Text(
                text = guidanceMessage,
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (isWarningFeedback) ReadingColors.CoralShadow else ReadingColors.InkDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { boardPixelSize = it }
                .clip(RoundedCornerShape(28.dp))
                .background(ReadingColors.NotebookPaper)
                .border(5.dp, ReadingColors.SunYellow, RoundedCornerShape(28.dp))
                .pointerInput(selectedColor, activeTarget) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isWarningFeedback = false
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

                val guideLayout = computeGuideTextLayout(
                    targetText = activeTarget,
                    boardWidth = w,
                    boardHeight = h,
                    typeface = tajawalTypeface
                )

                val guidePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                    typeface = tajawalTypeface
                    textSize = guideLayout.textSizePx
                    color = AndroidColor.argb(56, 30, 41, 59)
                    style = AndroidPaint.Style.FILL
                    textAlign = AndroidPaint.Align.LEFT
                }
                drawContext.canvas.nativeCanvas.drawText(
                    activeTarget,
                    guideLayout.drawX,
                    guideLayout.drawY,
                    guidePaint
                )

                // نقطة البداية الخضراء النابضة (مخصصة للحرف أو تلقائية لبداية الكلمة العربية من اليمين)
                val dotPos = if (explicitStartPoint != null) {
                    Offset(w * explicitStartPoint.xFraction, h * explicitStartPoint.yFraction)
                } else {
                    val rightEdgeX = (guideLayout.drawX + guideLayout.bounds.right - guideLayout.bounds.width() * 0.08f)
                        .coerceIn(w * 0.15f, w * 0.88f)
                    Offset(rightEdgeX, h * 0.46f)
                }
                drawCircle(
                    color = if (startDotFlashActive) {
                        ReadingColors.SunYellow.copy(alpha = 0.55f)
                    } else {
                        ReadingColors.MeadowGreen.copy(alpha = 0.28f)
                    },
                    radius = if (startDotFlashActive) startDotRadius * 2.15f else startDotRadius * 1.65f,
                    center = dotPos
                )
                drawCircle(
                    color = ReadingColors.MeadowGreen,
                    radius = if (startDotFlashActive) 15f else 12f,
                    center = dotPos
                )
                drawCircle(color = Color.White, radius = 4.5f, center = dotPos)

                val drawStrokeWidth = if (activeTarget.length <= 2) 28f else 20f
                fun drawSmoothStroke(strokeColor: Color, pts: List<Offset>) {
                    if (pts.isEmpty()) return
                    if (pts.size == 1) {
                        drawCircle(color = strokeColor, radius = drawStrokeWidth * 0.5f, center = pts.first())
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
                            width = drawStrokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                completedStrokes.forEach { stroke ->
                    drawSmoothStroke(stroke.color, stroke.points)
                }
                drawSmoothStroke(selectedColor, activePoints)

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
                    PlayfulCrayonIcon(crayonColor = crayonColor, modifier = Modifier.size(32.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayfulBouncyButton(
                onClick = {
                    engine.playTapPop()
                    completedStrokes.clear()
                    activePoints.clear()
                    isWarningFeedback = false
                    hasCompletedCurrentStep = false
                    isSubmittingStep = false
                    guidanceMessage = "${lesson.level.writePromptPrefix} ($activeTarget)"
                },
                backgroundColor = ReadingColors.CandyPink,
                shadowColor = ReadingColors.CandyShadow,
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
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
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            PlayfulBouncyButton(
                onClick = {
                    engine.playTapPop()
                    if (activeTarget.length <= 2) {
                        engine.speakSequence("حرف", activeTarget)
                    } else {
                        engine.speak(activeTarget)
                    }
                },
                backgroundColor = ReadingColors.SkyBlue,
                shadowColor = ReadingColors.SkyBlueShadow,
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
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
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            val hasNextLetter = lesson.traceLetters.size > 1 && selectedLetterIdx + 1 < lesson.traceLetters.size
            val doneButtonLabel = when {
                !hasCompletedCurrentStep -> "أنهيت الكتابة"
                hasNextLetter -> "التالي"
                else -> "أعد التدريب"
            }
            val doneButtonBg = when {
                !hasCompletedCurrentStep -> ReadingColors.MeadowGreen
                hasNextLetter -> ReadingColors.SkyBlue
                else -> ReadingColors.BerryPurple
            }
            val doneButtonShadow = when {
                !hasCompletedCurrentStep -> ReadingColors.MeadowShadow
                hasNextLetter -> ReadingColors.SkyBlueShadow
                else -> ReadingColors.BerryShadow
            }

            PlayfulBouncyButton(
                onClick = {
                    if (hasCompletedCurrentStep) {
                        engine.playTapPop()
                        if (hasNextLetter) {
                            selectedLetterIdx += 1
                            engine.speak(lesson.traceLetters[selectedLetterIdx % lesson.traceLetters.size])
                        } else {
                            completedStrokes.clear()
                            activePoints.clear()
                            hasCompletedCurrentStep = false
                            isSubmittingStep = false
                            isWarningFeedback = false
                            guidanceMessage = "${lesson.level.writePromptPrefix} ($activeTarget)"
                        }
                        return@PlayfulBouncyButton
                    }

                    if (isSubmittingStep || !hasSufficientDrawing) {
                        return@PlayfulBouncyButton
                    }

                    isSubmittingStep = true
                    val strokesToEval = if (activePoints.isNotEmpty()) {
                        completedStrokes.toList() + DrawnStroke(selectedColor, activePoints.toList())
                    } else {
                        completedStrokes.toList()
                    }
                    val result = evaluateLetterTracingWithBitmap(
                        targetText = activeTarget,
                        strokes = strokesToEval,
                        boardSize = boardPixelSize,
                        typeface = tajawalTypeface
                    )
                    if (result.passed) {
                        hasCompletedCurrentStep = true
                        isSubmittingStep = false
                        isWarningFeedback = false
                        val awarded = onWritingFinished(activeTarget)
                        guidanceMessage = if (awarded) {
                            "ممتاز يا بطل! كتبت ($activeTarget) بدقة رائعة"
                        } else {
                            "أحسنت! أعدت التدريب بنجاح، تم احتساب نجوم هذا الحرف مسبقاً"
                        }
                    } else {
                        isSubmittingStep = false
                        engine.playGentleTryAgain()
                        isWarningFeedback = true
                        startDotFlashActive = true
                        guidanceMessage = if (result.inBoundsPrecisionRatio < 0.58f) {
                            "حاول تتبّع الحرف الرمادي بدقة يا بطل! ابقَ فوق الخط الرمادي"
                        } else {
                            "حاول تتبّع الحرف الرمادي بدقة يا بطل! أكمل بقية الكتابة"
                        }
                        engine.speak("حاول تتبّع الحرف الرمادي بدقة يا بطل!")
                    }
                },
                backgroundColor = doneButtonBg,
                shadowColor = doneButtonShadow,
                enabled = if (hasCompletedCurrentStep) true else (hasSufficientDrawing && !isSubmittingStep),
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1.45f),
                testTagValue = "reading_write_done_btn"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PlayfulCheckIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = doneButtonLabel,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ============================================================================
// التبويب 4: تحدّي (مغامرة النجوم والإملاء وفهم المقروء + قراءة صوتية اختيارية)
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
                    challengeStatus = "تحدث الآن واقرأ الإجابة بصوتك الجميل..."
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
                        normCand.contains(normalizedTarget) || (normalizedTarget.contains(normCand) && normCand.isNotEmpty())
                    }
                    if (matched) {
                        solvedCurrent = true
                        challengeStatus = "أبدعت يا بطل! نطقت (${question.correctOption}) بشكل سليم"
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
        if (lesson.level == ReadingLevel.LEVEL_1) {
            engine.speak(question.spokenTarget)
        } else {
            engine.speak(question.promptLabel)
        }
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
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ReadingColors.BerryPurple
                    )
                    Text(
                        text = challengeStatus,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ReadingColors.InkDark
                    )
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            engine.speak(
                                if (lesson.level == ReadingLevel.LEVEL_1) question.spokenTarget else question.promptLabel
                            )
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
                                text = "اسمع السؤال",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = ReadingColors.InkDark
                            )
                        }
                    }
                }
            }
        }

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
                                engine.playEncouragement(isSuccess = false)
                                challengeStatus = "حاول مرة أخرى يا بطل!"
                            }
                        },
                        backgroundColor = effectiveBg,
                        shadowColor = effectiveShadow,
                        cornerRadius = 38.dp,
                        lipDepth = 6.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(122.dp),
                        testTagValue = "reading_challenge_balloon_$idx"
                    ) {
                        Text(
                            text = optionText,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = when {
                                optionText.length >= 12 -> 15.sp
                                optionText.length >= 7 -> 18.sp
                                optionText.length >= 4 -> 22.sp
                                else -> 30.sp
                            },
                            lineHeight = if (optionText.length >= 7) 24.sp else 36.sp,
                            color = if (solvedCurrent && isCorrectChoice) ReadingColors.InkDark else Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
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
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        }

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
                    fontFamily = ReadingTajawalFont,
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
