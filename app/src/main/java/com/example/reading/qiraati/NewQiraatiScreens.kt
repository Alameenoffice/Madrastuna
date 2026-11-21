package com.example.reading.qiraati

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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.reading.KidsCelebrationCanvas
import com.example.reading.PlayfulBackArrowIcon
import com.example.reading.PlayfulBouncyButton
import com.example.reading.PlayfulCheckIcon
import com.example.reading.PlayfulCrayonIcon
import com.example.reading.PlayfulCrownIcon
import com.example.reading.PlayfulEraserIcon
import com.example.reading.PlayfulMicIcon
import com.example.reading.PlayfulSpeakerIcon
import com.example.reading.PlayfulStarIcon
import com.example.reading.PlayfulTrophyIcon
import com.example.reading.ReadingAudioAndProgressEngine
import com.example.reading.ReadingAvatarCanvas
import com.example.reading.ReadingColors
import com.example.reading.ReadingProgressState
import com.example.reading.ReadingTajawalFont
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * مسارات التنقل الداخلية لقسم كتاب "قراءتي الجديدة — الصف الأول الابتدائي":
 * الدرس / القصة ← مسار الوحدة ← مكتبة الوحدات ← واحة القراءة الذكية.
 */
private sealed interface QiraatiRoute {
    data object UnitsLibrary : QiraatiRoute
    data class UnitPath(val unit: QiraatiUnit) : QiraatiRoute
    data class StoryLesson(val unit: QiraatiUnit) : QiraatiRoute
    data class LetterLesson(val unit: QiraatiUnit, val lesson: QiraatiLetterLesson) : QiraatiRoute
    data class UnitChant(val unit: QiraatiUnit) : QiraatiRoute
}

/**
 * المنسّق الرئيسي لتجربة كتاب "قراءتي الجديدة — الصف الأول الابتدائي" داخل واحة القراءة الذكية.
 */
@Composable
fun NewQiraatiNavHost(
    studentName: String,
    progress: ReadingProgressState,
    isSpeaking: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onBackToOasis: () -> Unit,
    onOpenAvatarPicker: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {}
) {
    var route by remember { mutableStateOf<QiraatiRoute>(QiraatiRoute.UnitsLibrary) }

    fun navigateUp() {
        engine.playTapPop()
        route = when (val current = route) {
            is QiraatiRoute.LetterLesson -> QiraatiRoute.UnitPath(current.unit)
            is QiraatiRoute.StoryLesson -> QiraatiRoute.UnitPath(current.unit)
            is QiraatiRoute.UnitChant -> QiraatiRoute.UnitPath(current.unit)
            is QiraatiRoute.UnitPath -> QiraatiRoute.UnitsLibrary
            QiraatiRoute.UnitsLibrary -> {
                onBackToOasis()
                QiraatiRoute.UnitsLibrary
            }
        }
    }

    BackHandler {
        navigateUp()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AnimatedContent(
            targetState = route,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "new_qiraati_nav"
        ) { currentRoute ->
            when (currentRoute) {
                QiraatiRoute.UnitsLibrary -> NewQiraatiUnitsScreen(
                    studentName = studentName,
                    progress = progress,
                    onOpenUnit = { unit ->
                        engine.playTapPop()
                        engine.speak(unit.title)
                        route = QiraatiRoute.UnitPath(unit)
                    },
                    onTapUpcoming = { unit ->
                        engine.playTapPop()
                        engine.speak("${unit.title} ستتوفر قريباً يا بطل")
                    },
                    onOpenAvatarPicker = onOpenAvatarPicker,
                    onOpenLeaderboard = onOpenLeaderboard,
                    onBack = { navigateUp() }
                )

                is QiraatiRoute.UnitPath -> NewQiraatiUnitPathScreen(
                    unit = currentRoute.unit,
                    progress = progress,
                    onOpenStory = {
                        engine.playTapPop()
                        engine.speak("الاستماع والمحادثة: ${currentRoute.unit.shortTitle}")
                        route = QiraatiRoute.StoryLesson(currentRoute.unit)
                    },
                    onOpenLetterLesson = { lesson ->
                        engine.playTapPop()
                        engine.speak("درس ${lesson.letterName}")
                        route = QiraatiRoute.LetterLesson(currentRoute.unit, lesson)
                    },
                    onOpenChant = {
                        val chant = currentRoute.unit.unitChant
                        if (chant != null) {
                            engine.playTapPop()
                            engine.speak(chant.title)
                            route = QiraatiRoute.UnitChant(currentRoute.unit)
                        }
                    },
                    onBack = { navigateUp() }
                )

                is QiraatiRoute.StoryLesson -> {
                    val story = currentRoute.unit.storySection
                    if (story != null) {
                        NewQiraatiStoryScreen(
                            unit = currentRoute.unit,
                            story = story,
                            progress = progress,
                            isSpeaking = isSpeaking,
                            engine = engine,
                            onBack = { navigateUp() }
                        )
                    } else {
                        LaunchedEffect(Unit) { navigateUp() }
                    }
                }

                is QiraatiRoute.LetterLesson -> NewQiraatiLetterScreen(
                    unit = currentRoute.unit,
                    lesson = currentRoute.lesson,
                    progress = progress,
                    isSpeaking = isSpeaking,
                    engine = engine,
                    onBack = { navigateUp() }
                )

                is QiraatiRoute.UnitChant -> {
                    val chant = currentRoute.unit.unitChant
                    if (chant != null) {
                        NewQiraatiChantScreen(
                            unit = currentRoute.unit,
                            chant = chant,
                            progress = progress,
                            isSpeaking = isSpeaking,
                            engine = engine,
                            onBack = { navigateUp() }
                        )
                    } else {
                        LaunchedEffect(Unit) { navigateUp() }
                    }
                }
            }
        }
    }
}

/**
 * حساب عدد الدروس المنجزة داخل الوحدة (الاستماع والمحادثة + دروس الحروف + نشيد الوحدة).
 */
private fun countCompletedUnitItems(unit: QiraatiUnit, progress: ReadingProgressState): Pair<Int, Int> {
    val storyDone = unit.storySection?.let { if (progress.starsForLesson(it.id) > 0) 1 else 0 } ?: 0
    val storyTotal = if (unit.storySection != null) 1 else 0
    val lettersDone = unit.letterLessons.count { progress.starsForLesson(it.id) > 0 }
    val chantDone = unit.unitChant?.let { if (progress.starsForLesson(it.id) > 0) 1 else 0 } ?: 0
    val chantTotal = if (unit.unitChant != null) 1 else 0
    return (storyDone + lettersDone + chantDone) to (storyTotal + unit.letterLessons.size + chantTotal)
}

// ============================================================================
// 1) شاشة مكتبة وحدات "قراءتي الجديدة — الصف الأول الابتدائي"
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewQiraatiUnitsScreen(
    studentName: String,
    progress: ReadingProgressState,
    onOpenUnit: (QiraatiUnit) -> Unit,
    onTapUpcoming: (QiraatiUnit) -> Unit,
    onBack: () -> Unit,
    onOpenAvatarPicker: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {}
) {
    val units = NewQiraatiRepository.allUnits

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBAE6FD),
                        Color(0xFFECF8FF),
                        Color(0xFFFFFBEB)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // شريط علوي: زر الرجوع + عداد نجوم الطفل
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayfulBouncyButton(
                onClick = onBack,
                backgroundColor = ReadingColors.CoralOrange,
                shadowColor = ReadingColors.CoralShadow,
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                testTagValue = "qiraati_units_back_btn"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayfulBackArrowIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        text = "واحة القراءة",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlayfulBouncyButton(
                    onClick = onOpenLeaderboard,
                    backgroundColor = ReadingColors.SunYellow,
                    shadowColor = ReadingColors.SunShadow,
                    cornerRadius = 18.dp,
                    lipDepth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    testTagValue = "qiraati_units_leaderboard_btn"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PlayfulTrophyIcon(modifier = Modifier.size(20.dp), animated = true)
                        Text(
                            text = "أبطال الصف",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = ReadingColors.InkDark,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    modifier = Modifier.testTag("qiraati_units_stars_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PlayfulStarIcon(filled = true, modifier = Modifier.size(21.dp))
                        Text(
                            text = "${progress.totalStars}",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = ReadingColors.InkDark
                        )
                    }
                }
            }
        }

        // ترويسة طفولية فخمة باسم الكتاب الرسمي
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            shadowElevation = 7.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(2.5.dp, ReadingColors.SunYellow, RoundedCornerShape(28.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0284C7),
                                Color(0xFF0EA5E9),
                                Color(0xFF38BDF8)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenAvatarPicker() }
                            .testTag("qiraati_units_avatar_badge")
                    ) {
                        ReadingAvatarCanvas(
                            avatarId = progress.avatarId,
                            size = 74.dp,
                            animated = true
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ReadingColors.SunYellow
                        ) {
                            Text(
                                text = "شخصيتي",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = ReadingColors.InkDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ReadingColors.SunYellow
                        ) {
                            Text(
                                text = "المنهج العراقي التفاعلي",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                color = ReadingColors.InkDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "قراءتي الجديدة — الصف الأول الابتدائي",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            lineHeight = 26.sp,
                            color = Color.White
                        )
                        Text(
                            text = "مرحباً يا $studentName! اختر وحدتك وانطلق في ألعاب البازل والتحليل والمحادثة",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            color = Color.White.copy(alpha = 0.94f)
                        )
                    }
                }
            }
        }

        // بطاقة الوحدات المتوفرة (الوحدة الأولى: أسرتي)
        units.filter { it.isAvailable }.forEach { unit ->
            val (completed, total) = countCompletedUnitItems(unit, progress)
            val progressFraction = if (total > 0) completed.toFloat() / total.toFloat() else 0f

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 7.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.5.dp, unit.theme.primaryColor.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                    .testTag("qiraati_unit_card_${unit.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = unit.theme.primaryColor
                        ) {
                            Text(
                                text = unit.unitBadge,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "أنجزت $completed من $total دروس",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = ReadingColors.MeadowShadow,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // مشهد قصة الوحدة الترحيبي
                    QiraatiStorySceneCanvas(
                        artKey = unit.coverArtKey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                    )

                    Text(
                        text = unit.title,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = ReadingColors.InkDark
                    )
                    Text(
                        text = unit.description,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = ReadingColors.InkSoft
                    )

                    // شريط التقدم المبهج داخل الوحدة
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction.coerceIn(0.08f, 1f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(unit.theme.primaryColor, ReadingColors.SunYellow)
                                    )
                                )
                        )
                    }

                    // شارات حروف الوحدة الأربعة
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        unit.letterLessons.forEach { lesson ->
                            val stars = progress.starsForLesson(lesson.id)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = lesson.accentColor.copy(alpha = 0.14f),
                                modifier = Modifier.border(
                                    1.5.dp,
                                    lesson.accentColor.copy(alpha = 0.55f),
                                    RoundedCornerShape(16.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = lesson.targetLetter,
                                        fontFamily = ReadingTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = lesson.shadowColor
                                    )
                                    PlayfulStarIcon(filled = stars > 0, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    PlayfulBouncyButton(
                        onClick = { onOpenUnit(unit) },
                        backgroundColor = unit.theme.primaryColor,
                        shadowColor = unit.theme.shadowColor,
                        cornerRadius = 22.dp,
                        lipDepth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        testTagValue = "qiraati_open_unit_${unit.id}"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayfulCrownIcon(modifier = Modifier.size(22.dp))
                            Text(
                                text = "ادخل إلى ${unit.title}",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // عنوان الوحدات القادمة (خريطة التوسع المستقبلية)
        Text(
            text = "الوحدات القادمة في رحلتنا:",
            fontFamily = ReadingTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = ReadingColors.InkDark,
            modifier = Modifier.padding(top = 4.dp)
        )

        units.filterNot { it.isAvailable }.forEach { upcoming ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.90f),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, upcoming.themeColor.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
                    .clickable { onTapUpcoming(upcoming) }
                    .testTag("qiraati_upcoming_unit_${upcoming.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(upcoming.themeColor.copy(alpha = 0.16f))
                            .border(2.dp, upcoming.themeColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${upcoming.unitNumber}",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = upcoming.theme.shadowColor
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = upcoming.title,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                color = ReadingColors.InkDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ReadingColors.SunYellow.copy(alpha = 0.28f)
                            ) {
                                Text(
                                    text = upcoming.comingSoonLabel ?: "قريباً",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    color = ReadingColors.SunShadow,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (upcoming.plannedLetters.isNotEmpty()) {
                            Text(
                                text = "حروف الوحدة: ${upcoming.plannedLetters.joinToString("  •  ")}",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = ReadingColors.InkSoft
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ============================================================================
// 2) شاشة مسار الوحدة المرئي (الاستماع والمحادثة ← جزر الحروف ب، م، د، ف)
// ============================================================================
@Composable
fun NewQiraatiUnitPathScreen(
    unit: QiraatiUnit,
    progress: ReadingProgressState,
    onOpenStory: () -> Unit,
    onOpenLetterLesson: (QiraatiLetterLesson) -> Unit,
    onBack: () -> Unit,
    onOpenChant: () -> Unit = {}
) {
    val totalNodes = (if (unit.storySection != null) 1 else 0) +
        unit.letterLessons.size +
        (if (unit.unitChant != null) 1 else 0)
    val xFractions = listOf(0.50f, 0.26f, 0.74f, 0.26f, 0.74f, 0.50f)
    val rowSpacingDp = 166.dp
    val topPaddingDp = 18.dp
    val totalHeightDp = topPaddingDp + rowSpacingDp * totalNodes + 44.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        unit.theme.softSurfaceColor,
                        Color(0xFFECF8FF),
                        Color(0xFFF0FDF4)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // شريط علوي
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayfulBouncyButton(
                onClick = onBack,
                backgroundColor = unit.theme.primaryColor,
                shadowColor = unit.theme.shadowColor,
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                testTagValue = "qiraati_unit_path_back_btn"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayfulBackArrowIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        text = "الوحدات",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayfulStarIcon(filled = true, modifier = Modifier.size(22.dp))
                    Text(
                        text = "${progress.totalStars} نجمة",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = ReadingColors.InkDark
                    )
                }
            }
        }

        // بطاقة عنوان الوحدة
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 5.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(2.dp, unit.theme.primaryColor.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReadingBlueBirdCanvas(
                    bannerText = "",
                    modifier = Modifier.size(72.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = unit.title,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = ReadingColors.InkDark
                    )
                    Text(
                        text = if (unit.unitChant != null) {
                            "ابدأ بقصة الاستماع والمحادثة، ثم تعلّم دروس الوحدة واختمها بالنشيد الجميل"
                        } else {
                            "ابدأ بقصة الاستماع والمحادثة، ثم تعلّم دروس الوحدة بالترتيب"
                        },
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = ReadingColors.InkSoft
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // مسار الوحدة المنحني
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightDp)
                .padding(horizontal = 12.dp)
        ) {
            val mapWidth = maxWidth

            Canvas(modifier = Modifier.fillMaxSize()) {
                val centers = (0 until totalNodes).map { idx ->
                    val frac = xFractions[idx % xFractions.size]
                    Offset(
                        size.width * frac,
                        topPaddingDp.toPx() + idx * rowSpacingDp.toPx() + 62.dp.toPx()
                    )
                }
                if (centers.size >= 2) {
                    val path = Path().apply {
                        moveTo(centers.first().x, centers.first().y)
                        for (i in 0 until centers.lastIndex) {
                            val p0 = centers[i]
                            val p1 = centers[i + 1]
                            val midY = (p0.y + p1.y) * 0.5f
                            cubicTo(p0.x, midY, p1.x, midY, p1.x, p1.y)
                        }
                    }
                    drawPath(path, color = Color.White, style = Stroke(width = 24f, cap = StrokeCap.Round))
                    drawPath(
                        path,
                        color = unit.theme.primaryColor.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 9f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f), 0f)
                        )
                    )
                }
            }

            var nodeIdx = 0

            // محطة 0: الاستماع والمحادثة (أنا أتحدث)
            unit.storySection?.let { story ->
                val frac = xFractions[nodeIdx % xFractions.size]
                val cardW = 198.dp
                val xOff = (mapWidth * frac - cardW / 2f).coerceIn(0.dp, (mapWidth - cardW).coerceAtLeast(0.dp))
                val yOff = topPaddingDp + rowSpacingDp * nodeIdx
                val storyStars = progress.starsForLesson(story.id)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(cardW)
                        .offset(x = xOff, y = yOff)
                ) {
                    PlayfulBouncyButton(
                        onClick = onOpenStory,
                        backgroundColor = ReadingColors.SunYellow,
                        shadowColor = ReadingColors.SunShadow,
                        cornerRadius = 30.dp,
                        lipDepth = 6.dp,
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier.size(width = 186.dp, height = 104.dp),
                        testTagValue = "qiraati_story_station_btn"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QiraatiBookIconCanvas(modifier = Modifier.size(52.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "الاستماع والمحادثة",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5.sp,
                                    color = ReadingColors.InkDark
                                )
                                Text(
                                    text = "أنا أتحدث • ${unit.shortTitle}",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ReadingColors.InkDark.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (s in 1..3) {
                            PlayfulStarIcon(filled = s <= storyStars, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                nodeIdx++
            }

            // جزر الحروف الأربعة (ب، م، د، ف)
            unit.letterLessons.forEach { lesson ->
                val frac = xFractions[nodeIdx % xFractions.size]
                val cardW = 166.dp
                val xOff = (mapWidth * frac - cardW / 2f).coerceIn(0.dp, (mapWidth - cardW).coerceAtLeast(0.dp))
                val yOff = topPaddingDp + rowSpacingDp * nodeIdx
                val stars = progress.starsForLesson(lesson.id)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(cardW)
                        .offset(x = xOff, y = yOff)
                ) {
                    PlayfulBouncyButton(
                        onClick = { onOpenLetterLesson(lesson) },
                        backgroundColor = lesson.accentColor,
                        shadowColor = lesson.shadowColor,
                        cornerRadius = 60.dp,
                        lipDepth = 6.dp,
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier.size(116.dp),
                        testTagValue = "qiraati_letter_island_${lesson.id}"
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f))
                                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = lesson.targetLetter,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 40.sp,
                                    lineHeight = 46.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = lesson.cloudForms.joinToString("  "),
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier.border(
                            1.5.dp,
                            lesson.accentColor.copy(alpha = 0.35f),
                            RoundedCornerShape(16.dp)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = lesson.letterName,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = ReadingColors.InkDark
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                for (s in 1..3) {
                                    PlayfulStarIcon(filled = s <= stars, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
                nodeIdx++
            }

            // محطة ختامية احتفالية: نشيد الوحدة (مثل: نشيد مَدْرَسَتِي)
            unit.unitChant?.let { chant ->
                val cardW = 220.dp
                val xOff = (mapWidth * 0.50f - cardW / 2f).coerceIn(0.dp, (mapWidth - cardW).coerceAtLeast(0.dp))
                val yOff = topPaddingDp + rowSpacingDp * nodeIdx
                val chantStars = progress.starsForLesson(chant.id)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(cardW)
                        .offset(x = xOff, y = yOff)
                ) {
                    PlayfulBouncyButton(
                        onClick = onOpenChant,
                        backgroundColor = ReadingColors.BerryPurple,
                        shadowColor = ReadingColors.BerryShadow,
                        cornerRadius = 30.dp,
                        lipDepth = 6.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.size(width = 212.dp, height = 108.dp),
                        testTagValue = "qiraati_unit_chant_btn_${chant.id}"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChantMusicalCanvas(
                                isPlaying = true,
                                modifier = Modifier.size(56.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = chant.title,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "استمع وردّد النشيد",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ReadingColors.SunYellow
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (s in 1..3) {
                            PlayfulStarIcon(filled = s <= chantStars, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2.5) شاشة نشيد الوحدة التفاعلية ("نشيد مَدْرَسَتِي")
// ============================================================================
@Composable
fun NewQiraatiChantScreen(
    unit: QiraatiUnit,
    chant: QiraatiUnitChant,
    progress: ReadingProgressState,
    isSpeaking: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onBack: () -> Unit
) {
    var activeVerseIdx by remember(chant.id) { mutableIntStateOf(-1) }
    var isRecitingAll by remember(chant.id) { mutableStateOf(false) }
    var reciteTrigger by remember(chant.id) { mutableIntStateOf(0) }
    var showCelebration by remember { mutableStateOf(false) }

    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            delay(2200L)
            showCelebration = false
        }
    }

    LaunchedEffect(reciteTrigger) {
        if (reciteTrigger > 0) {
            isRecitingAll = true
            chant.verses.forEachIndexed { idx, verse ->
                activeVerseIdx = idx
                engine.speak(verse)
                val waitMs = (2400L + verse.length * 42L).coerceIn(2800L, 4800L)
                delay(waitMs)
            }
            isRecitingAll = false
            activeVerseIdx = -1
            engine.playVictoryFanfare()
            engine.recordActivityWin(chant.id, "chant_listen")
            showCelebration = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFECFCCB), Color(0xFFE0F2FE), Color(0xFFFFFBEB))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // شريط علوي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayfulBouncyButton(
                    onClick = {
                        isRecitingAll = false
                        onBack()
                    },
                    backgroundColor = unit.theme.primaryColor,
                    shadowColor = unit.theme.shadowColor,
                    cornerRadius = 18.dp,
                    lipDepth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    testTagValue = "qiraati_chant_back_btn"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PlayfulBackArrowIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            text = unit.shortTitle,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val stars = progress.starsForLesson(chant.id)
                        for (s in 1..3) {
                            PlayfulStarIcon(filled = s <= stars, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // بطاقة ترويسة النشيد مع النوتات الموسيقية والنجوم المتحركة
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.5.dp, ReadingColors.BerryPurple.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChantMusicalCanvas(
                        isPlaying = isRecitingAll || isSpeaking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                    )
                    Text(
                        text = chant.title,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        lineHeight = 36.sp,
                        color = ReadingColors.InkDark,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = chant.subtitle,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = ReadingColors.InkSoft,
                        textAlign = TextAlign.Center
                    )

                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            reciteTrigger++
                        },
                        backgroundColor = if (isRecitingAll) ReadingColors.CoralOrange else ReadingColors.MeadowGreen,
                        shadowColor = if (isRecitingAll) ReadingColors.CoralShadow else ReadingColors.MeadowShadow,
                        cornerRadius = 22.dp,
                        lipDepth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        testTagValue = "qiraati_chant_listen_all_btn"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayfulSpeakerIcon(
                                isSpeaking = isRecitingAll || isSpeaking,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (isRecitingAll) "جاري إنشاد الأبيات..." else "استمع للنشيد كاملاً",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // أبيات النشيد الأربعة بألوان طفولية مشرقة وإضاءة متتابعة
            val verseAccents = remember {
                listOf(
                    Color(0xFF22C55E),
                    Color(0xFF0EA5E9),
                    Color(0xFFFF7A59),
                    Color(0xFF8B5CF6)
                )
            }
            chant.verses.forEachIndexed { idx, verse ->
                val isActive = activeVerseIdx == idx
                val accent = verseAccents[idx % verseAccents.size]
                val verseScale by animateFloatAsState(
                    targetValue = if (isActive) 1.04f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "chant_verse_scale_$idx"
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isActive) Color(0xFFFFFBEB) else Color.White,
                    shadowElevation = if (isActive) 9.dp else 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = verseScale
                            scaleY = verseScale
                        }
                        .border(
                            width = if (isActive) 3.dp else 1.8.dp,
                            color = if (isActive) ReadingColors.SunYellow else accent.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            activeVerseIdx = idx
                            engine.playTapPop()
                            engine.speak(verse)
                        }
                        .testTag("qiraati_chant_verse_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isActive) ReadingColors.SunYellow else accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = if (isActive) ReadingColors.InkDark else Color.White
                            )
                        }

                        Text(
                            text = verse,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            lineHeight = 38.sp,
                            color = ReadingColors.InkDark,
                            modifier = Modifier.weight(1f)
                        )

                        PlayfulSpeakerIcon(
                            isSpeaking = isActive,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        KidsCelebrationCanvas(visible = showCelebration)
    }
}

// ============================================================================
// 3) شاشة الاستماع والمحادثة التفاعلية ("أسرتي — أنا أتحدث")
// ============================================================================
@Composable
fun NewQiraatiStoryScreen(
    unit: QiraatiUnit,
    story: UnitStorySection,
    progress: ReadingProgressState,
    isSpeaking: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showCelebration by remember { mutableStateOf(false) }
    var activeQuestionIdx by remember { mutableIntStateOf(-1) }
    var isListeningVoice by remember { mutableStateOf(false) }
    var voiceFeedback by remember { mutableStateOf("اضغط على المايكروفون وتحدث بصوتك الجميل عن الصور!") }

    val speechRecognizer = remember(context) {
        runCatching {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else null
        }.getOrNull()
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            runCatching { speechRecognizer?.destroy() }
        }
    }

    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            delay(2000L)
            showCelebration = false
        }
    }

    fun startTalkRecognition(questionIndex: Int) {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            voiceFeedback = "أحسنت يا بطل! استمر في التحدث ومشاركة أفكارك مع أسرتك"
            engine.playSuccessChime()
            engine.recordActivityWin(story.id, "story_talk_$questionIndex")
            showCelebration = true
            return
        }
        runCatching {
            activeQuestionIdx = questionIndex
            isListeningVoice = true
            voiceFeedback = "أنا أستمع إليك الآن يا بطل... تحدّث بصوت واضح"
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListeningVoice = false
                }
                override fun onError(error: Int) {
                    isListeningVoice = false
                    voiceFeedback = "رائع! يمكنك المحاولة مرة أخرى أو الاستماع للقصة"
                }
                override fun onResults(results: Bundle?) {
                    isListeningVoice = false
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    voiceFeedback = if (spoken.isNotBlank()) {
                        "أحسنت يا بطل! سمعتك تقول: «$spoken»"
                    } else {
                        "تعبير جميل يا بطل! أحسنت الحديث عن أسرتك"
                    }
                    engine.playVictoryFanfare()
                    engine.recordActivityWin(story.id, "story_talk_$questionIndex")
                    engine.speak("أحسنت يا بطل! حديثك عن أسرتك جميل جداً")
                    showCelebration = true
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        }.onFailure {
            isListeningVoice = false
            voiceFeedback = "تعبير جميل يا بطل! يمكنك التحدث مع معلمك أو والديك عن الصور"
        }
    }

    var pendingQuestionIndex by remember { mutableIntStateOf(0) }
    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startTalkRecognition(pendingQuestionIndex)
        } else {
            voiceFeedback = "يمكنك التعبير بصوتك أمام معلمك أو والديك والاستماع للقصة!"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFECF8FF), Color(0xFFF0FDF4))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // شريط علوي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayfulBouncyButton(
                    onClick = onBack,
                    backgroundColor = unit.theme.primaryColor,
                    shadowColor = unit.theme.shadowColor,
                    cornerRadius = 18.dp,
                    lipDepth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    testTagValue = "qiraati_story_back_btn"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlayfulBackArrowIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(
                            text = "رجوع",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val stars = progress.starsForLesson(story.id)
                        for (s in 1..3) {
                            PlayfulStarIcon(filled = s <= stars, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // عنوان درس الاستماع والمحادثة وزر استمع للقصة الكاملة
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ReadingColors.SunYellow, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = story.headerLabel,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = unit.theme.shadowColor
                    )
                    Text(
                        text = story.title,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = ReadingColors.InkDark
                    )

                    QiraatiStorySceneCanvas(
                        artKey = story.listeningScene.artKey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    )

                    Text(
                        text = story.listeningScene.narrationText,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 28.sp,
                        color = ReadingColors.InkDark
                    )

                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            val fullStory = (listOf(story.listeningScene.narrationText) + story.interactiveStoryLines)
                                .joinToString(" ")
                            engine.speak(fullStory)
                            engine.recordActivityWin(story.id, "story_listen")
                        },
                        backgroundColor = ReadingColors.SkyBlue,
                        shadowColor = ReadingColors.SkyBlueShadow,
                        cornerRadius = 20.dp,
                        lipDepth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        testTagValue = "qiraati_story_listen_full_btn"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayfulSpeakerIcon(isSpeaking = isSpeaking, tint = Color.White, modifier = Modifier.size(22.dp))
                            Text(
                                text = "استمع للقصة كاملة",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // مشاهد المحادثة الثلاثة
            Text(
                text = "مشاهد المحادثة المصوّرة:",
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = ReadingColors.InkDark
            )

            story.conversationScenes.forEach { scene ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, ReadingColors.SkyBlue.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = scene.title,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ReadingColors.InkDark
                        )
                        QiraatiStorySceneCanvas(
                            artKey = scene.artKey,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(144.dp)
                        )
                        Text(
                            text = scene.narrationText,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 25.sp,
                            color = ReadingColors.InkSoft
                        )
                        PlayfulBouncyButton(
                            onClick = {
                                engine.playTapPop()
                                engine.speak("${scene.audioDescription} ${scene.narrationText}")
                            },
                            backgroundColor = ReadingColors.MeadowGreen,
                            shadowColor = ReadingColors.MeadowShadow,
                            cornerRadius = 16.dp,
                            lipDepth = 4.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            testTagValue = "qiraati_story_scene_listen_${scene.sceneNumber}"
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PlayfulSpeakerIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "اسمع وصف الصورة ${scene.sceneNumber}",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // قسم أسئلة "أنا أتحدث" مع زر المايكروفون الاختياري الآمن
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFFF5F3FF),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ReadingColors.BerryPurple.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "أنا أتحدث — عبّر بصوتك الجميل",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = ReadingColors.BerryShadow
                    )
                    Text(
                        text = voiceFeedback,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = ReadingColors.InkDark
                    )

                    story.iSpeakQuestions.forEachIndexed { idx, question ->
                        val isListeningThis = isListeningVoice && activeQuestionIdx == idx
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ReadingColors.BerryPurple),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontFamily = ReadingTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = question,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5.sp,
                                    lineHeight = 22.sp,
                                    color = ReadingColors.InkDark,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            engine.playTapPop()
                                            engine.speak(question)
                                        }
                                )

                                PlayfulBouncyButton(
                                    onClick = {
                                        engine.playTapPop()
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasPerm) {
                                            startTalkRecognition(idx)
                                        } else {
                                            pendingQuestionIndex = idx
                                            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    backgroundColor = if (isListeningThis) ReadingColors.CoralOrange else ReadingColors.BerryPurple,
                                    shadowColor = if (isListeningThis) ReadingColors.CoralShadow else ReadingColors.BerryShadow,
                                    cornerRadius = 16.dp,
                                    lipDepth = 4.dp,
                                    contentPadding = PaddingValues(10.dp),
                                    modifier = Modifier.size(48.dp),
                                    testTagValue = "qiraati_story_mic_btn_$idx"
                                ) {
                                    PlayfulMicIcon(tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        KidsCelebrationCanvas(visible = showCelebration)
    }
}

// ============================================================================
// 4) شاشة درس الحرف التفاعلي بـ 6 محطات ممتعة متدرجة
// ============================================================================
private enum class QiraatiLessonStation(
    val stepNumber: Int,
    val shortTitle: String,
    val accentColor: Color,
    val shadowColor: Color
) {
    INTRO_WORDS(1, "أتعرف", Color(0xFFFF7A59), Color(0xFFD14B2B)),
    VOWELS(2, "المدود", Color(0xFF0EA5E9), Color(0xFF0369A1)),
    SUKOON(3, "الساكن", Color(0xFFEC4899), Color(0xFFBE185D)),
    ANALYSIS(4, "حلّل", Color(0xFF22C55E), Color(0xFF15803D)),
    PUZZLE(5, "البازل", Color(0xFF8B5CF6), Color(0xFF6233C7)),
    READ_AND_WRITE(6, "أقرأ وأكتب", Color(0xFFF59E0B), Color(0xFFB45309))
}

/**
 * تلوين الحرف المستهدف أو المقطع باللون الأحمر الساطع مع الحفاظ التام على التشكيل وخط تجوال.
 */
private fun buildRedHighlightedText(
    word: String,
    ranges: List<QiraatiHighlightRange>,
    highlightColor: Color = Color(0xFFE11D48)
): AnnotatedString = buildAnnotatedString {
    append(word)
    ranges.forEach { range ->
        val safeStart = range.startInclusive.coerceIn(0, word.length)
        val safeEnd = range.endExclusive.coerceIn(safeStart, word.length)
        if (safeStart < safeEnd) {
            addStyle(
                style = SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.ExtraBold
                ),
                start = safeStart,
                end = safeEnd
            )
        }
    }
}

@Composable
fun NewQiraatiLetterScreen(
    unit: QiraatiUnit,
    lesson: QiraatiLetterLesson,
    progress: ReadingProgressState,
    isSpeaking: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onBack: () -> Unit
) {
    val visibleStations = remember(lesson.id) {
        buildList {
            add(QiraatiLessonStation.INTRO_WORDS)
            if (lesson.vowelPairs.isNotEmpty() || lesson.vowelWords.isNotEmpty() || lesson.tanweenListenWords.isNotEmpty()) {
                add(QiraatiLessonStation.VOWELS)
            }
            if (lesson.sukoonItems.isNotEmpty() || lesson.circleFilterWords.isNotEmpty() || lesson.suffixChoiceItems.isNotEmpty()) {
                add(QiraatiLessonStation.SUKOON)
            }
            if (lesson.analysisItems.isNotEmpty()) {
                add(QiraatiLessonStation.ANALYSIS)
            }
            if (lesson.puzzleItems.isNotEmpty()) {
                add(QiraatiLessonStation.PUZZLE)
            }
            add(QiraatiLessonStation.READ_AND_WRITE)
        }
    }

    fun stationLabelFor(station: QiraatiLessonStation): String = when (station) {
        QiraatiLessonStation.VOWELS -> when {
            lesson.isAppleVowelsStyle -> "تفاحات الحركات"
            lesson.tanweenListenWords.isNotEmpty() -> "استمع للتنوين"
            else -> station.shortTitle
        }
        QiraatiLessonStation.SUKOON -> when {
            lesson.circleFilterWords.isNotEmpty() || lesson.suffixChoiceItems.isNotEmpty() -> "ألعاب (ة ، ـة)"
            else -> station.shortTitle
        }
        else -> station.shortTitle
    }

    fun nextStationAfter(current: QiraatiLessonStation): QiraatiLessonStation {
        val idx = visibleStations.indexOf(current)
        return visibleStations.getOrNull(idx + 1) ?: visibleStations.last()
    }

    var activeStation by remember(lesson.id) { mutableStateOf(visibleStations.first()) }
    var showCelebration by remember { mutableStateOf(false) }

    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            delay(2000L)
            showCelebration = false
        }
    }

    fun triggerWin(pillarKey: String, speechPraise: String) {
        engine.playVictoryFanfare()
        engine.recordActivityWin(lesson.id, pillarKey)
        engine.speak(speechPraise)
        showCelebration = true
    }

    val activeStepIndex = (visibleStations.indexOf(activeStation).takeIf { it >= 0 } ?: 0) + 1

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE0F2FE), Color(0xFFF8FAFC), Color(0xFFFFFBEB))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // شريط علوي للدرس
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayfulBouncyButton(
                    onClick = onBack,
                    backgroundColor = lesson.accentColor,
                    shadowColor = lesson.shadowColor,
                    cornerRadius = 18.dp,
                    lipDepth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    testTagValue = "qiraati_letter_back_btn"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PlayfulBackArrowIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            text = unit.shortTitle,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = lesson.letterName,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = ReadingColors.InkDark
                    )
                    Text(
                        text = "محطة $activeStepIndex من ${visibleStations.size} • ${stationLabelFor(activeStation)}",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ReadingColors.InkSoft
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val stars = progress.starsForLesson(lesson.id)
                        for (s in 1..3) {
                            PlayfulStarIcon(filled = s <= stars, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // شريط المحطات العلوي الملون
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleStations.forEachIndexed { idx, station ->
                    val selected = station == activeStation
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            activeStation = station
                        },
                        backgroundColor = if (selected) station.accentColor else Color.White,
                        shadowColor = if (selected) station.shadowColor else Color(0xFFCBD5E1),
                        cornerRadius = 18.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(54.dp),
                        testTagValue = "qiraati_station_tab_${station.stepNumber}"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.White.copy(alpha = 0.25f) else station.accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = stationLabelFor(station),
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = if (selected) Color.White else ReadingColors.InkDark,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // محتوى المحطة المختارة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = activeStation,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "qiraati_station_content"
                ) { station ->
                    when (station) {
                        QiraatiLessonStation.INTRO_WORDS -> Station1IntroWords(
                            lesson = lesson,
                            engine = engine,
                            nextLabel = "التالي: ${stationLabelFor(nextStationAfter(station))}",
                            onNextStation = { activeStation = nextStationAfter(station) }
                        )

                        QiraatiLessonStation.VOWELS -> Station2And3Vowels(
                            lesson = lesson,
                            engine = engine,
                            nextLabel = "التالي: ${stationLabelFor(nextStationAfter(station))}",
                            onNextStation = { activeStation = nextStationAfter(station) }
                        )

                        QiraatiLessonStation.SUKOON -> Station4Sukoon(
                            lesson = lesson,
                            engine = engine,
                            onSpecialGameWin = { key, praise -> triggerWin(key, praise) },
                            nextLabel = "التالي: ${stationLabelFor(nextStationAfter(station))}",
                            onNextStation = { activeStation = nextStationAfter(station) }
                        )

                        QiraatiLessonStation.ANALYSIS -> Station5AnalysisGame(
                            lesson = lesson,
                            engine = engine,
                            onWordAnalyzed = { word ->
                                triggerWin("qiraati_analysis", "ممتاز يا بطل! حلّلت كلمة $word بنجاح")
                            },
                            onNextStation = { activeStation = nextStationAfter(station) }
                        )

                        QiraatiLessonStation.PUZZLE -> Station6PuzzleGame(
                            lesson = lesson,
                            engine = engine,
                            onPuzzleSolved = { word ->
                                triggerWin("qiraati_puzzle", "رائع يا بطل! ركّبت كلمة $word")
                            },
                            onNextStation = { activeStation = nextStationAfter(station) }
                        )

                        QiraatiLessonStation.READ_AND_WRITE -> Station7ReadAndWrite(
                            lesson = lesson,
                            isSpeaking = isSpeaking,
                            engine = engine,
                            onReadSuccess = { word ->
                                triggerWin("qiraati_read", "أحسنت يا بطل! قراءتك لكلمة $word ممتازة")
                            },
                            onWriteSuccess = { target ->
                                triggerWin("qiraati_write", "بطل الخط العربي! كتبت $target بدقة رائعة")
                            }
                        )
                    }
                }
            }
        }

        KidsCelebrationCanvas(visible = showCelebration)
    }
}

// ============================================================================
// المحطة 1: أتعرف الحرف (نشاط 1 — الغيمة العائمة + الكلمات المصورة باللون الأحمر)
// ============================================================================
@Composable
private fun Station1IntroWords(
    lesson: QiraatiLetterLesson,
    engine: ReadingAudioAndProgressEngine,
    onNextStation: () -> Unit,
    nextLabel: String = "التالي: المد القصير والطويل"
) {
    var activeWordIndex by remember(lesson.id) { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // غيمة الحرف العائمة
        LetterCloudCanvas(
            cloudForms = lesson.cloudForms,
            accentColor = lesson.accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .clickable {
                    engine.playTapPop()
                    engine.speak(lesson.letterName)
                }
                .testTag("qiraati_letter_cloud_${lesson.id}")
        )

        Text(
            text = "اضغط على كل بطاقة لتسمع الكلمة وتشاهد ${lesson.letterName} باللون الأحمر",
            fontFamily = ReadingTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.5.sp,
            color = ReadingColors.InkDark,
            textAlign = TextAlign.Center
        )

        // شبكة بطاقات الكلمات المصورة (صفوف ثنائية منظمة)
        val rows = lesson.introWords.chunked(2)
        rows.forEachIndexed { rowIdx, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEachIndexed { colIdx, item ->
                    val globalIdx = rowIdx * 2 + colIdx
                    val isBounced = activeWordIndex == globalIdx
                    val cardScale by animateFloatAsState(
                        targetValue = if (isBounced) 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "intro_word_scale_$globalIdx"
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isBounced) Color(0xFFFFF7ED) else Color.White,
                        shadowElevation = if (isBounced) 8.dp else 4.dp,
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                            }
                            .border(
                                width = if (isBounced) 2.5.dp else 1.5.dp,
                                color = if (isBounced) lesson.accentColor else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                activeWordIndex = globalIdx
                                engine.playTapPop()
                                engine.speak(item.spokenText)
                            }
                            .testTag("qiraati_intro_word_$globalIdx")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QiraatiWordArtCanvas(
                                artKey = item.artKey,
                                modifier = Modifier.size(66.dp)
                            )
                            Text(
                                text = buildRedHighlightedText(item.word, item.highlightRanges),
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                lineHeight = 36.sp,
                                color = ReadingColors.InkDark,
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = lesson.accentColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = item.position.arabicLabel,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = lesson.shadowColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        StationNextFooterButton(
            label = nextLabel,
            onClick = {
                engine.playTapPop()
                onNextStation()
            }
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ============================================================================
// المحطة 2: المد القصير والطويل (نشاط 2 و 3 — الموجة القصيرة والموجة الذهبية الممتدة)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Station2And3Vowels(
    lesson: QiraatiLetterLesson,
    engine: ReadingAudioAndProgressEngine,
    onNextStation: () -> Unit,
    nextLabel: String = "التالي: المقطع الساكن"
) {
    var activePairIdx by remember(lesson.id) { mutableIntStateOf(-1) }
    var activeIsLong by remember(lesson.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            lesson.isAppleVowelsStyle -> {
                Text(
                    text = "اضغط على كل تفاحة حمراء لتسمع صوت التاء المربوطة مع الحركات!",
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp,
                    lineHeight = 23.sp,
                    color = ReadingColors.InkDark,
                    textAlign = TextAlign.Center
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    lesson.vowelPairs.forEachIndexed { idx, pair ->
                        val isActive = activePairIdx == idx
                        AppleVowelCanvas(
                            primaryForm = pair.shortSyllable,
                            secondaryForm = pair.longSyllable,
                            vowelLabel = pair.shortVowelLabel,
                            isActive = isActive,
                            modifier = Modifier
                                .size(width = 148.dp, height = 148.dp)
                                .clickable {
                                    activePairIdx = idx
                                    engine.playSuccessChime()
                                    engine.speak(pair.spokenShort)
                                }
                                .testTag("qiraati_apple_vowel_$idx")
                        )
                    }
                }
            }

            lesson.tanweenListenWords.isNotEmpty() -> {
                Text(
                    text = "استمع جيداً لصوت تنوين الضم (ـٌ) في آخر كل كلمة!",
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp,
                    lineHeight = 23.sp,
                    color = ReadingColors.InkDark,
                    textAlign = TextAlign.Center
                )

                lesson.tanweenListenWords.chunked(2).forEachIndexed { rIdx, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEachIndexed { cIdx, item ->
                            val globalIdx = rIdx * 2 + cIdx
                            val isActive = activePairIdx == globalIdx
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = if (isActive) Color(0xFFFFFBEB) else Color.White,
                                shadowElevation = if (isActive) 8.dp else 4.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isActive) 2.5.dp else 1.8.dp,
                                        color = if (isActive) ReadingColors.SunYellow else lesson.accentColor.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable {
                                        activePairIdx = globalIdx
                                        engine.playSuccessChime()
                                        engine.speak(item.spokenText)
                                    }
                                    .testTag("qiraati_tanween_listen_$globalIdx")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QiraatiWordArtCanvas(artKey = item.artKey, modifier = Modifier.size(64.dp))
                                    Text(
                                        text = buildRedHighlightedText(item.word, item.highlightRanges),
                                        fontFamily = ReadingTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp,
                                        lineHeight = 38.sp,
                                        color = ReadingColors.InkDark,
                                        textAlign = TextAlign.Center
                                    )
                                    PlayfulSpeakerIcon(
                                        isSpeaking = isActive,
                                        tint = lesson.accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Text(
                    text = "قارن بين الصوت القصير والصوت الطويل الممتد",
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp,
                    color = ReadingColors.InkDark,
                    textAlign = TextAlign.Center
                )

                // نشاط 2: أزواج المد القصير مقابل المد الطويل مع موجات صوتية بصرية
                lesson.vowelPairs.forEachIndexed { idx, pair ->
            val shortActive = activePairIdx == idx && !activeIsLong
            val longActive = activePairIdx == idx && activeIsLong

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ReadingColors.SkyBlue.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // زر المد القصير
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlayfulBouncyButton(
                            onClick = {
                                activePairIdx = idx
                                activeIsLong = false
                                engine.playTapPop()
                                engine.speak(pair.spokenShort)
                            },
                            backgroundColor = ReadingColors.SkyBlue,
                            shadowColor = ReadingColors.SkyBlueShadow,
                            cornerRadius = 20.dp,
                            lipDepth = 5.dp,
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            testTagValue = "qiraati_vowel_short_$idx"
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = pair.shortSyllable,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp,
                                    lineHeight = 42.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = pair.shortVowelLabel,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                            }
                        }
                        VowelWaveIndicatorCanvas(
                            isLongVowel = false,
                            isActive = shortActive,
                            accentColor = ReadingColors.SkyBlue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }

                    // زر المد الطويل
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlayfulBouncyButton(
                            onClick = {
                                activePairIdx = idx
                                activeIsLong = true
                                engine.playSuccessChime()
                                engine.speak(pair.spokenLong)
                            },
                            backgroundColor = ReadingColors.CoralOrange,
                            shadowColor = ReadingColors.CoralShadow,
                            cornerRadius = 20.dp,
                            lipDepth = 5.dp,
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            testTagValue = "qiraati_vowel_long_$idx"
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = pair.longSyllable,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp,
                                    lineHeight = 42.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = pair.longVowelLabel,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                            }
                        }
                        VowelWaveIndicatorCanvas(
                            isLongVowel = true,
                            isActive = longActive,
                            accentColor = ReadingColors.SunYellow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }
                }
            }
                }
            }
        }

        // نشاط 3: كلمات تطبيقية على المد القصير والطويل (إن وُجدت)
        if (lesson.vowelWords.isNotEmpty()) {
            Text(
                text = "كلمات تطبيقية على المد القصير والمد الطويل:",
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = ReadingColors.InkDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        lesson.vowelWords.chunked(2).forEachIndexed { rIdx, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEachIndexed { cIdx, vw ->
                    val accent = if (vw.isLongVowel) ReadingColors.CoralOrange else ReadingColors.SkyBlue
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                            .clickable {
                                engine.playTapPop()
                                engine.speak(vw.spokenText)
                            }
                            .testTag("qiraati_vowel_word_${rIdx * 2 + cIdx}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QiraatiWordArtCanvas(artKey = vw.artKey, modifier = Modifier.size(56.dp))
                            Text(
                                text = buildRedHighlightedText(vw.word, vw.highlightRanges),
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 23.sp,
                                lineHeight = 34.sp,
                                color = ReadingColors.InkDark,
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accent.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = vw.vowelLabel,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ReadingColors.InkDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        StationNextFooterButton(
            label = nextLabel,
            onClick = {
                engine.playTapPop()
                onNextStation()
            }
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ============================================================================
// المحطة 3: المقطع الساكن (نشاط 4) أو ألعاب التاء المربوطة التفاعلية (ضع دائرة + اختر ة أم ـة)
// ============================================================================
@Composable
private fun Station4Sukoon(
    lesson: QiraatiLetterLesson,
    engine: ReadingAudioAndProgressEngine,
    onNextStation: () -> Unit,
    onSpecialGameWin: (String, String) -> Unit = { _, _ -> },
    nextLabel: String = "التالي: لعبة حلّل الكلمة"
) {
    if (lesson.circleFilterWords.isNotEmpty() || lesson.suffixChoiceItems.isNotEmpty()) {
        TaaMarboutaInteractiveGamesStation(
            lesson = lesson,
            engine = engine,
            onGameWin = onSpecialGameWin,
            nextLabel = nextLabel,
            onNextStation = onNextStation
        )
        return
    }

    var jumpedIdx by remember(lesson.id) { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFFDF2F8),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.8.dp, ReadingColors.CandyPink.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
        ) {
            Text(
                text = "اضغط على أي بطاقة ليقفز المقطع الساكن باللون الأحمر وينطق بوضوح!",
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.5.sp,
                lineHeight = 22.sp,
                color = ReadingColors.CandyShadow,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp)
            )
        }

        lesson.sukoonItems.forEachIndexed { idx, item ->
            val isJumped = jumpedIdx == idx
            val jumpY by animateDpAsState(
                targetValue = if (isJumped) (-10).dp else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "sukoon_jump_$idx"
            )
            val badgeScale by animateFloatAsState(
                targetValue = if (isJumped) 1.15f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "sukoon_scale_$idx"
            )

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = if (isJumped) 8.dp else 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isJumped) 2.5.dp else 1.5.dp,
                        color = if (isJumped) Color(0xFFE11D48) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .clickable {
                        jumpedIdx = idx
                        engine.playSuccessChime()
                        engine.speak("${item.sukoonSyllable} .. ${item.spokenText}")
                    }
                    .testTag("qiraati_sukoon_card_$idx")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    QiraatiWordArtCanvas(artKey = item.artKey, modifier = Modifier.size(72.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = buildRedHighlightedText(item.word, item.highlightRanges),
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            lineHeight = 42.sp,
                            color = ReadingColors.InkDark
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFE11D48),
                                shadowElevation = if (isJumped) 8.dp else 3.dp,
                                modifier = Modifier
                                    .offset(y = jumpY)
                                    .graphicsLayer {
                                        scaleX = badgeScale
                                        scaleY = badgeScale
                                    }
                            ) {
                                Text(
                                    text = item.sukoonSyllable,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    lineHeight = 34.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "+",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = ReadingColors.InkSoft
                            )

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = item.remainingPart,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    lineHeight = 32.sp,
                                    color = ReadingColors.InkDark,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    PlayfulSpeakerIcon(
                        isSpeaking = isJumped,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        StationNextFooterButton(
            label = nextLabel,
            onClick = {
                engine.playTapPop()
                onNextStation()
            }
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

/**
 * ألعاب التاء المربوطة التفاعلية في الوحدة الثانية:
 * 1) لعبة "ضع دائرة حول الكلمات التي تحوي (ة ، ـة)" مع رسم دائرة خضراء مضيئة على Canvas أو اهتزاز أفقي لطيف.
 * 2) لعبة "اختر الشكل الصحيح (ة أم ـة)" لإكمال الكلمات وسماع نطقها والاحتفال بالنجوم.
 */
@Composable
private fun TaaMarboutaInteractiveGamesStation(
    lesson: QiraatiLetterLesson,
    engine: ReadingAudioAndProgressEngine,
    onGameWin: (String, String) -> Unit,
    nextLabel: String,
    onNextStation: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val foundCircleIndices = remember(lesson.id) { mutableStateListOf<Int>() }
    var circleWinTriggered by remember(lesson.id) { mutableStateOf(false) }
    val totalCorrectCircles = remember(lesson.id) { lesson.circleFilterWords.count { it.containsTarget } }

    val completedSuffixIndices = remember(lesson.id) { mutableStateMapOf<Int, Boolean>() }
    var suffixWinTriggered by remember(lesson.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1) لعبة ضع دائرة حول الكلمات التي تحوي (ة ، ـة)
        if (lesson.circleFilterWords.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ReadingColors.MeadowGreen.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ضع دائرة حول الكلمات التي تحوي (ة ، ـة)",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                        color = ReadingColors.InkDark,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "وجدت ${foundCircleIndices.size} من $totalCorrectCircles كلمات صحيحة",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ReadingColors.MeadowShadow
                    )

                    lesson.circleFilterWords.chunked(2).forEachIndexed { rIdx, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEachIndexed { cIdx, item ->
                                val idx = rIdx * 2 + cIdx
                                val isCircled = idx in foundCircleIndices
                                val shakeOffset = remember(lesson.id, idx) { Animatable(0f) }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer { translationX = shakeOffset.value }
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(if (isCircled) Color(0xFFDCFCE7) else Color(0xFFF8FAFC))
                                        .border(
                                            width = if (isCircled) 2.5.dp else 1.5.dp,
                                            color = if (isCircled) ReadingColors.MeadowGreen else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .clickable {
                                            if (item.containsTarget) {
                                                if (idx !in foundCircleIndices) {
                                                    foundCircleIndices.add(idx)
                                                }
                                                engine.playSuccessChime()
                                                engine.speak(item.spokenText)
                                                if (foundCircleIndices.size >= totalCorrectCircles && !circleWinTriggered) {
                                                    circleWinTriggered = true
                                                    onGameWin(
                                                        "qiraati_circle_filter",
                                                        "أحسنت يا بطل! وجدت كل كلمات التاء المربوطة بنجاح"
                                                    )
                                                }
                                            } else {
                                                engine.playGentleTryAgain()
                                                engine.speak("هذه الكلمة لا تحتوي على تاء مربوطة، حاول مرة أخرى")
                                                scope.launch {
                                                    for (dx in listOf(-14f, 14f, -10f, 10f, -5f, 5f, 0f)) {
                                                        shakeOffset.animateTo(dx, tween(45))
                                                    }
                                                }
                                            }
                                        }
                                        .padding(10.dp)
                                        .testTag("qiraati_circle_word_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        QiraatiWordArtCanvas(artKey = item.artKey, modifier = Modifier.size(54.dp))
                                        Text(
                                            text = item.word,
                                            fontFamily = ReadingTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 23.sp,
                                            lineHeight = 34.sp,
                                            color = if (isCircled) ReadingColors.MeadowShadow else ReadingColors.InkDark,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // الدائرة الخضراء المضيئة المرسومة على Canvas عند الإجابة الصحيحة
                                    if (isCircled) {
                                        Canvas(modifier = Modifier.matchParentSize()) {
                                            val pad = 8f
                                            drawOval(
                                                color = ReadingColors.MeadowGreen.copy(alpha = 0.22f),
                                                topLeft = Offset(pad, pad),
                                                size = androidx.compose.ui.geometry.Size(size.width - pad * 2, size.height - pad * 2),
                                                style = Stroke(width = 14f)
                                            )
                                            drawOval(
                                                color = ReadingColors.MeadowGreen,
                                                topLeft = Offset(pad, pad),
                                                size = androidx.compose.ui.geometry.Size(size.width - pad * 2, size.height - pad * 2),
                                                style = Stroke(width = 6f, cap = StrokeCap.Round)
                                            )
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 2) لعبة اختر الشكل الصحيح (ة أم ـة) لإكمال الكلمة
        if (lesson.suffixChoiceItems.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, ReadingColors.BerryPurple.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "اختر الشكل الصحيح (ة أم ـة) لإكمال كل كلمة",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                        color = ReadingColors.InkDark,
                        textAlign = TextAlign.Center
                    )

                    lesson.suffixChoiceItems.forEachIndexed { idx, item ->
                        val solved = completedSuffixIndices[idx] == true
                        val shakeOffset = remember(lesson.id, "suf_$idx") { Animatable(0f) }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (solved) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationX = shakeOffset.value }
                                .border(
                                    width = if (solved) 2.5.dp else 1.5.dp,
                                    color = if (solved) ReadingColors.MeadowGreen else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(22.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QiraatiWordArtCanvas(artKey = item.artKey, modifier = Modifier.size(52.dp))

                                Text(
                                    text = if (solved) item.completedWord else item.wordStem,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    lineHeight = 36.sp,
                                    color = if (solved) ReadingColors.MeadowShadow else ReadingColors.InkDark,
                                    modifier = Modifier.weight(1f)
                                )

                                if (solved) {
                                    PlayfulBouncyButton(
                                        onClick = {
                                            engine.playTapPop()
                                            engine.speak(item.spokenText)
                                        },
                                        backgroundColor = ReadingColors.MeadowGreen,
                                        shadowColor = ReadingColors.MeadowShadow,
                                        cornerRadius = 16.dp,
                                        lipDepth = 4.dp,
                                        contentPadding = PaddingValues(10.dp),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        PlayfulSpeakerIcon(tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item.options.forEachIndexed { oIdx, opt ->
                                            PlayfulBouncyButton(
                                                onClick = {
                                                    if (opt == item.correctOption) {
                                                        completedSuffixIndices[idx] = true
                                                        engine.playSuccessChime()
                                                        engine.speak(item.spokenText)
                                                        if (completedSuffixIndices.size >= lesson.suffixChoiceItems.size && !suffixWinTriggered) {
                                                            suffixWinTriggered = true
                                                            onGameWin(
                                                                "qiraati_suffix_choice",
                                                                "بطل رائع! أكملت كل كلمات التاء المربوطة بالشكل الصحيح"
                                                            )
                                                        }
                                                    } else {
                                                        engine.playGentleTryAgain()
                                                        scope.launch {
                                                            for (dx in listOf(-12f, 12f, -8f, 8f, 0f)) {
                                                                shakeOffset.animateTo(dx, tween(45))
                                                            }
                                                        }
                                                    }
                                                },
                                                backgroundColor = if (oIdx == 0) ReadingColors.CoralOrange else ReadingColors.SkyBlue,
                                                shadowColor = if (oIdx == 0) ReadingColors.CoralShadow else ReadingColors.SkyBlueShadow,
                                                cornerRadius = 16.dp,
                                                lipDepth = 4.dp,
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                                modifier = Modifier.defaultMinSize(minWidth = 52.dp, minHeight = 50.dp),
                                                testTagValue = "qiraati_suffix_opt_${idx}_$oIdx"
                                            ) {
                                                Text(
                                                    text = opt,
                                                    fontFamily = ReadingTajawalFont,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 24.sp,
                                                    lineHeight = 32.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        StationNextFooterButton(
            label = nextLabel,
            onClick = {
                engine.playTapPop()
                onNextStation()
            }
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ============================================================================
// المحطة 4: لعبة حلّل الكلمة (نشاط 5 — الدبوس الأحمر + الدوائر ذات الأرجل الكرتونية)
// ============================================================================
private data class AnalysisChoiceTile(
    val uid: Int,
    val text: String,
    val color: Color,
    val shadow: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Station5AnalysisGame(
    lesson: QiraatiLetterLesson,
    engine: ReadingAudioAndProgressEngine,
    onWordAnalyzed: (String) -> Unit,
    onNextStation: () -> Unit
) {
    var itemIndex by remember(lesson.id) { mutableIntStateOf(0) }
    val item = lesson.analysisItems[itemIndex % lesson.analysisItems.size]

    val tilePalette = remember {
        listOf(
            ReadingColors.CoralOrange to ReadingColors.CoralShadow,
            ReadingColors.SkyBlue to ReadingColors.SkyBlueShadow,
            ReadingColors.BerryPurple to ReadingColors.BerryShadow,
            ReadingColors.CandyPink to ReadingColors.CandyShadow
        )
    }

    val choices = remember(lesson.id, itemIndex) {
        val correctTiles = item.syllables.mapIndexed { idx, syl ->
            val (bg, sh) = tilePalette[idx % tilePalette.size]
            AnalysisChoiceTile(idx, syl, bg, sh)
        }
        val poolFromVowels = lesson.vowelPairs.flatMap { listOf(it.shortSyllable, it.longSyllable) }
        val poolFromOtherAnalysis = lesson.analysisItems.flatMap { it.syllables }
        val fallbackPool = listOf("مَـ", "بَا", "رُ", "سَـ", "ةُ")
        val extraCandidates = (poolFromVowels + poolFromOtherAnalysis + fallbackPool)
            .distinct()
            .filter { cand -> item.syllables.none { it == cand } }
            .take(2)
            .mapIndexed { idx, syl ->
                val (bg, sh) = tilePalette[(idx + 2) % tilePalette.size]
                AnalysisChoiceTile(100 + idx, syl, bg, sh)
            }
        (correctTiles + extraCandidates).shuffled()
    }

    var placedTiles by remember(lesson.id, itemIndex) { mutableStateOf<List<AnalysisChoiceTile>>(emptyList()) }
    var feedback by remember(lesson.id, itemIndex) { mutableStateOf("اضغط على المقاطع بالترتيب لتضعها في الدوائر الراقصة") }
    var solved by remember(lesson.id, itemIndex) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // اختيار الكلمة داخل النشاط
        if (lesson.analysisItems.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                lesson.analysisItems.forEachIndexed { idx, cand ->
                    val selected = idx == itemIndex
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            itemIndex = idx
                            engine.speak(cand.spokenText)
                        },
                        backgroundColor = if (selected) ReadingColors.MeadowGreen else Color.White,
                        shadowColor = if (selected) ReadingColors.MeadowShadow else Color(0xFFCBD5E1),
                        cornerRadius = 16.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cand.word,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (selected) Color.White else ReadingColors.InkDark
                        )
                    }
                }
            }
        }

        // بطاقة الكلمة المثبّتة بدبوس أحمر لطيف
        RedPinWordCard(
            word = item.word,
            subtitle = feedback,
            isSolved = solved,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    engine.playTapPop()
                    engine.speak(item.spokenText)
                }
        )

        // الدوائر ذات الأرجل الكرتونية (FlowRow لدعم كلمات من 2 أو 3 أو 4 مقاطع بسلاسة)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val bubbleW = if (item.syllables.size >= 4) 88.dp else 104.dp
            val bubbleH = if (item.syllables.size >= 4) 114.dp else 126.dp
            item.syllables.forEachIndexed { slotIdx, _ ->
                val placed = placedTiles.getOrNull(slotIdx)
                WalkingSyllableBubbleCanvas(
                    syllableText = placed?.text,
                    isFilled = placed != null,
                    isCelebrating = solved,
                    accentColor = ReadingColors.MeadowGreen,
                    modifier = Modifier.size(width = bubbleW, height = bubbleH)
                )
            }
        }

        // المقاطع المبعثرة للاختيار
        if (!solved) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                choices.forEach { tile ->
                    val used = placedTiles.any { it.uid == tile.uid }
                    PlayfulBouncyButton(
                        onClick = {
                            if (used || solved) return@PlayfulBouncyButton
                            val nextIdx = placedTiles.size
                            val expected = item.syllables.getOrNull(nextIdx)
                            if (expected != null && tile.text == expected) {
                                val nextPlaced = placedTiles + tile
                                placedTiles = nextPlaced
                                engine.playSuccessChime()
                                engine.speak(tile.text)
                                if (nextPlaced.size == item.syllables.size) {
                                    solved = true
                                    feedback = "أحسنت يا بطل! حلّلت الكلمة بنجاح"
                                    onWordAnalyzed(item.word)
                                } else {
                                    feedback = "رائع! اختر المقطع التالي"
                                }
                            } else {
                                engine.playGentleTryAgain()
                                feedback = "حاول مرة أخرى يا بطل، اسمع الكلمة جيداً"
                            }
                        },
                        backgroundColor = tile.color,
                        shadowColor = tile.shadow,
                        enabled = !used,
                        cornerRadius = 22.dp,
                        lipDepth = 5.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 84.dp, minHeight = 72.dp),
                        testTagValue = "qiraati_analysis_tile_${tile.uid}"
                    ) {
                        Text(
                            text = tile.text,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            lineHeight = 36.sp,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            PlayfulBouncyButton(
                onClick = {
                    engine.playTapPop()
                    if (itemIndex + 1 < lesson.analysisItems.size) {
                        itemIndex += 1
                    } else {
                        onNextStation()
                    }
                },
                backgroundColor = ReadingColors.MeadowGreen,
                shadowColor = ReadingColors.MeadowShadow,
                cornerRadius = 22.dp,
                lipDepth = 5.dp,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                testTagValue = "qiraati_analysis_next_btn"
            ) {
                Text(
                    text = if (itemIndex + 1 < lesson.analysisItems.size) "الكلمة التالية" else "انتقل إلى لعبة البازل",
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ============================================================================
// المحطة 5: لعبة ركّب البازل المتشابك (نشاط 6 — انزلاق مرن وتعشيق القطعتين)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Station6PuzzleGame(
    lesson: QiraatiLetterLesson,
    engine: ReadingAudioAndProgressEngine,
    onPuzzleSolved: (String) -> Unit,
    onNextStation: () -> Unit
) {
    var puzzleIndex by remember(lesson.id) { mutableIntStateOf(0) }
    val puzzle = lesson.puzzleItems[puzzleIndex % lesson.puzzleItems.size]

    var selectedBranchIdx by remember(lesson.id, puzzleIndex) { mutableIntStateOf(-1) }
    val completedBranches = remember(lesson.id, puzzleIndex) { mutableStateListOf<Int>() }

    val selectedBranch = puzzle.branches.getOrNull(selectedBranchIdx)
    val isInterlocked = selectedBranch != null

    // حركة انزلاق مرنة تدمج القطعتين عند اختيار القطعة المتفرعة
    val slideGap by animateDpAsState(
        targetValue = if (isInterlocked) (-26).dp else 18.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "puzzle_interlock_gap"
    )

    val branchColors = remember {
        listOf(
            ReadingColors.CandyPink to ReadingColors.CandyShadow,
            ReadingColors.SkyBlue to ReadingColors.SkyBlueShadow,
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
        if (lesson.puzzleItems.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                lesson.puzzleItems.forEachIndexed { idx, p ->
                    val active = idx == puzzleIndex
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            puzzleIndex = idx
                        },
                        backgroundColor = if (active) ReadingColors.BerryPurple else Color.White,
                        shadowColor = if (active) ReadingColors.BerryShadow else Color(0xFFCBD5E1),
                        cornerRadius = 16.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "بازل ${p.basePiece}",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (active) Color.White else ReadingColors.InkDark
                        )
                    }
                }
            }
        }

        val maxPiecesInPuzzle = puzzle.branches.maxOfOrNull { it.resolvedPieces(puzzle.basePiece).size } ?: 2
        val isThreePiecePuzzle = maxPiecesInPuzzle >= 3

        Text(
            text = if (isThreePiecePuzzle) {
                "اضغط على بطاقة المقاطع لتتعشّق القطع الثلاث معاً وتكوّن الكلمة!"
            } else {
                "اضغط على قطعة البازل الملونة لتتعشّق مع القطعة الأساسية وتكوّن الكلمة!"
            },
            fontFamily = ReadingTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.5.sp,
            color = ReadingColors.InkDark,
            textAlign = TextAlign.Center
        )

        // مسرح تعشيق قطع البازل (يدعم قطعتين أو 3 قطع متشابكة بسلاسة)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (isInterlocked) Color(0xFFF0FDF4) else Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.5.dp,
                    color = if (isInterlocked) ReadingColors.MeadowGreen else ReadingColors.BerryPurple.copy(alpha = 0.40f),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val resolved = selectedBranch?.resolvedPieces(puzzle.basePiece)
                if (isThreePiecePuzzle) {
                    val p0 = resolved?.getOrNull(0) ?: puzzle.basePiece
                    val p1 = resolved?.getOrNull(1) ?: "؟"
                    val p2 = resolved?.getOrNull(2) ?: "؟"
                    val (midBg, midSh) = if (selectedBranchIdx >= 0) {
                        branchColors[selectedBranchIdx % branchColors.size]
                    } else {
                        Color(0xFF94A3B8) to Color(0xFF64748B)
                    }
                    val (endBg, endSh) = if (selectedBranchIdx >= 0) {
                        branchColors[(selectedBranchIdx + 1) % branchColors.size]
                    } else {
                        Color(0xFF94A3B8) to Color(0xFF64748B)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JigsawPuzzlePieceCanvas(
                            text = p0,
                            pieceColor = ReadingColors.CoralOrange,
                            shadowColor = ReadingColors.CoralShadow,
                            interlockSide = PuzzleInterlockSide.LEFT_TAB,
                            isHighlighted = isInterlocked,
                            modifier = Modifier
                                .size(width = 104.dp, height = 92.dp)
                                .clickable {
                                    engine.playTapPop()
                                    engine.speak(p0)
                                }
                        )
                        Spacer(modifier = Modifier.width(if (isInterlocked) 0.dp else 4.dp))
                        JigsawPuzzlePieceCanvas(
                            text = p1,
                            pieceColor = midBg,
                            shadowColor = midSh,
                            interlockSide = PuzzleInterlockSide.MIDDLE_TAB_AND_SOCKET,
                            isHighlighted = isInterlocked,
                            modifier = Modifier
                                .offset(x = if (isInterlocked) 14.dp else 0.dp)
                                .size(width = 104.dp, height = 92.dp)
                        )
                        Spacer(modifier = Modifier.width(if (isInterlocked) 0.dp else 4.dp))
                        JigsawPuzzlePieceCanvas(
                            text = p2,
                            pieceColor = endBg,
                            shadowColor = endSh,
                            interlockSide = PuzzleInterlockSide.RIGHT_SOCKET,
                            isHighlighted = isInterlocked,
                            modifier = Modifier
                                .offset(x = if (isInterlocked) 26.dp else 0.dp)
                                .size(width = 104.dp, height = 92.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        JigsawPuzzlePieceCanvas(
                            text = puzzle.basePiece,
                            pieceColor = ReadingColors.CoralOrange,
                            shadowColor = ReadingColors.CoralShadow,
                            interlockSide = PuzzleInterlockSide.LEFT_TAB,
                            isHighlighted = isInterlocked,
                            modifier = Modifier
                                .size(width = 138.dp, height = 104.dp)
                                .clickable {
                                    engine.playTapPop()
                                    engine.speak(puzzle.basePiece)
                                }
                        )

                        Spacer(modifier = Modifier.width(slideGap.coerceAtLeast(0.dp)))

                        val (branchBg, branchSh) = if (selectedBranchIdx >= 0) {
                            branchColors[selectedBranchIdx % branchColors.size]
                        } else {
                            Color(0xFF94A3B8) to Color(0xFF64748B)
                        }

                        JigsawPuzzlePieceCanvas(
                            text = selectedBranch?.attachedPiece ?: "؟",
                            pieceColor = branchBg,
                            shadowColor = branchSh,
                            interlockSide = PuzzleInterlockSide.RIGHT_SOCKET,
                            isHighlighted = isInterlocked,
                            modifier = Modifier
                                .offset(x = if (isInterlocked) 18.dp else 0.dp)
                                .size(width = 138.dp, height = 104.dp)
                        )
                    }
                }

                if (selectedBranch != null) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ReadingColors.SunYellow,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .border(2.5.dp, Color.White, RoundedCornerShape(22.dp))
                            .clickable {
                                engine.playTapPop()
                                engine.speak(selectedBranch.spokenText)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlayfulSpeakerIcon(tint = ReadingColors.InkDark, modifier = Modifier.size(22.dp))
                            Text(
                                text = selectedBranch.resultWord,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp,
                                lineHeight = 44.sp,
                                color = ReadingColors.InkDark
                            )
                        }
                    }
                } else {
                    Text(
                        text = "اختر قطعة من الأسفل لدمجها مع «${puzzle.basePiece}»",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ReadingColors.InkSoft
                    )
                }
            }
        }

        // القطع المتفرعة القابلة للاختيار
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            puzzle.branches.forEachIndexed { idx, branch ->
                val (bg, sh) = branchColors[idx % branchColors.size]
                val isDone = idx in completedBranches
                val pieces = branch.resolvedPieces(puzzle.basePiece)
                val buttonTitle = if (pieces.size >= 3) {
                    pieces.drop(1).joinToString(" + ")
                } else {
                    branch.attachedPiece
                }
                PlayfulBouncyButton(
                    onClick = {
                        selectedBranchIdx = idx
                        if (idx !in completedBranches) {
                            completedBranches.add(idx)
                        }
                        onPuzzleSolved(branch.resultWord)
                    },
                    backgroundColor = bg,
                    shadowColor = sh,
                    cornerRadius = 22.dp,
                    lipDepth = 5.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 96.dp, minHeight = 76.dp),
                    testTagValue = "qiraati_puzzle_branch_$idx"
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = buttonTitle,
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (pieces.size >= 3) 22.sp else 28.sp,
                            lineHeight = 34.sp,
                            color = Color.White
                        )
                        if (isDone) {
                            Text(
                                text = branch.resultWord,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                    }
                }
            }
        }

        StationNextFooterButton(
            label = "التالي: بطاقة أقرأ وسبورة الكتابة",
            onClick = {
                engine.playTapPop()
                onNextStation()
            }
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ============================================================================
// المحطة 6: أقرأ وأكتب (بطاقة العصفور الأزرق "أقرأ" + سبورة التتبع الدقيقة بالـ Bitmap)
// ============================================================================
private data class QiraatiStroke(
    val color: Color,
    val points: List<Offset>
)

private data class QiraatiGuideLayout(
    val textSizePx: Float,
    val drawX: Float,
    val drawY: Float,
    val bounds: Rect
)

private data class QiraatiTraceEval(
    val passed: Boolean,
    val coverage: Float,
    val precision: Float
)

private fun resolveQiraatiTajawalTypeface(context: Context): Typeface {
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

private fun computeQiraatiGuideLayout(
    targetText: String,
    boardW: Float,
    boardH: Float,
    typeface: Typeface
): QiraatiGuideLayout {
    val minDim = min(boardW, boardH)
    var candidateSize = minDim * 0.62f
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = candidateSize
        textAlign = AndroidPaint.Align.LEFT
    }
    val bounds = Rect()
    paint.getTextBounds(targetText, 0, targetText.length, bounds)

    val maxW = boardW * 0.78f
    val maxH = boardH * 0.70f
    if (bounds.width() > maxW && bounds.width() > 0) {
        candidateSize *= (maxW / bounds.width().toFloat())
    }
    if (bounds.height() > maxH && bounds.height() > 0) {
        candidateSize *= (maxH / bounds.height().toFloat())
    }
    paint.textSize = candidateSize
    paint.getTextBounds(targetText, 0, targetText.length, bounds)

    return QiraatiGuideLayout(
        textSizePx = candidateSize,
        drawX = boardW * 0.5f - bounds.exactCenterX(),
        drawY = boardH * 0.5f - bounds.exactCenterY(),
        bounds = bounds
    )
}

private fun evaluateQiraatiTracingBitmap(
    targetText: String,
    strokes: List<QiraatiStroke>,
    boardSize: IntSize,
    typeface: Typeface
): QiraatiTraceEval {
    if (strokes.isEmpty() || boardSize.width <= 0 || boardSize.height <= 0) {
        return QiraatiTraceEval(false, 0f, 0f)
    }
    val maskSize = 160
    val scaleX = maskSize.toFloat() / boardSize.width.toFloat()
    val scaleY = maskSize.toFloat() / boardSize.height.toFloat()
    val minBoardDim = min(boardSize.width, boardSize.height).toFloat()

    val layout = computeQiraatiGuideLayout(
        targetText = targetText,
        boardW = boardSize.width.toFloat(),
        boardH = boardSize.height.toFloat(),
        typeface = typeface
    )

    val guideExact = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)
    val guideTolerance = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)
    val userCov = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)
    val userCore = Bitmap.createBitmap(maskSize, maskSize, Bitmap.Config.ARGB_8888)

    return try {
        val gCanvas = AndroidCanvas(guideExact)
        gCanvas.save()
        gCanvas.scale(scaleX, scaleY)
        gCanvas.drawText(
            targetText,
            layout.drawX,
            layout.drawY,
            AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = layout.textSizePx
                color = AndroidColor.WHITE
                style = AndroidPaint.Style.FILL
            }
        )
        gCanvas.restore()

        val tCanvas = AndroidCanvas(guideTolerance)
        tCanvas.save()
        tCanvas.scale(scaleX, scaleY)
        tCanvas.drawText(
            targetText,
            layout.drawX,
            layout.drawY,
            AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = layout.textSizePx
                color = AndroidColor.WHITE
                style = AndroidPaint.Style.FILL_AND_STROKE
                strokeWidth = minBoardDim * 0.14f
                strokeJoin = AndroidPaint.Join.ROUND
                strokeCap = AndroidPaint.Cap.ROUND
            }
        )
        tCanvas.restore()

        fun renderStrokes(targetBitmap: Bitmap, strokeW: Float) {
            val c = AndroidCanvas(targetBitmap)
            val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                style = AndroidPaint.Style.STROKE
                strokeWidth = strokeW
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }
            val dotP = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                style = AndroidPaint.Style.FILL
            }
            strokes.forEach { stroke ->
                val pts = stroke.points
                if (pts.isEmpty()) return@forEach
                if (pts.size == 1) {
                    c.drawCircle(pts[0].x * scaleX, pts[0].y * scaleY, strokeW * 0.5f, dotP)
                } else {
                    val path = AndroidPath().apply {
                        moveTo(pts[0].x * scaleX, pts[0].y * scaleY)
                        for (i in 1 until pts.size) {
                            val px = pts[i - 1].x * scaleX
                            val py = pts[i - 1].y * scaleY
                            val cx = pts[i].x * scaleX
                            val cy = pts[i].y * scaleY
                            quadTo(px, py, (px + cx) * 0.5f, (py + cy) * 0.5f)
                        }
                        lineTo(pts.last().x * scaleX, pts.last().y * scaleY)
                    }
                    c.drawPath(path, p)
                }
            }
        }

        renderStrokes(userCov, 19f)
        renderStrokes(userCore, 10f)

        val total = maskSize * maskSize
        val gArr = IntArray(total)
        val tArr = IntArray(total)
        val ucArr = IntArray(total)
        val coreArr = IntArray(total)
        guideExact.getPixels(gArr, 0, maskSize, 0, 0, maskSize, maskSize)
        guideTolerance.getPixels(tArr, 0, maskSize, 0, 0, maskSize, maskSize)
        userCov.getPixels(ucArr, 0, maskSize, 0, 0, maskSize, maskSize)
        userCore.getPixels(coreArr, 0, maskSize, 0, 0, maskSize, maskSize)

        var guidePx = 0
        var coveredPx = 0
        var userPx = 0
        var inBoundsPx = 0
        for (i in 0 until total) {
            val isG = (gArr[i] ushr 24) > 80
            val isT = (tArr[i] ushr 24) > 80
            val isUC = (ucArr[i] ushr 24) > 80
            val isCore = (coreArr[i] ushr 24) > 80
            if (isG) {
                guidePx++
                if (isUC) coveredPx++
            }
            if (isCore) {
                userPx++
                if (isT) inBoundsPx++
            }
        }

        if (guidePx < 30 || userPx < 45) {
            QiraatiTraceEval(false, 0f, 0f)
        } else {
            val cov = coveredPx.toFloat() / guidePx.toFloat()
            val prec = inBoundsPx.toFloat() / userPx.toFloat()
            QiraatiTraceEval(cov >= 0.52f && prec >= 0.58f, cov, prec)
        }
    } finally {
        guideExact.recycle()
        guideTolerance.recycle()
        userCov.recycle()
        userCore.recycle()
    }
}

private fun stripArabicDiacritics(raw: String): String =
    raw.filterNot { it.isArabicDiacritic() }.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").trim()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Station7ReadAndWrite(
    lesson: QiraatiLetterLesson,
    isSpeaking: Boolean,
    engine: ReadingAudioAndProgressEngine,
    onReadSuccess: (String) -> Unit,
    onWriteSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val tajawalTypeface = remember(context) { resolveQiraatiTajawalTypeface(context) }

    // --- بطاقة العصفور الأزرق "أقرأ" مع المايكروفون الاختياري ---
    var selectedReadWordIdx by remember(lesson.id) { mutableIntStateOf(0) }
    val currentReadWord = lesson.readAloudWords[selectedReadWordIdx % lesson.readAloudWords.size]
    var readStatus by remember(lesson.id) { mutableStateOf("اضغط على الكلمات لتسمعها، أو اقرأها بصوتك للعصفور الأزرق!") }
    var isListeningRead by remember { mutableStateOf(false) }

    val speechRecognizer = remember(context) {
        runCatching {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else null
        }.getOrNull()
    }

    DisposableEffect(speechRecognizer) {
        onDispose { runCatching { speechRecognizer?.destroy() } }
    }

    fun startVoiceReadCheck() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            readStatus = "اضغط على كل كلمة لتسمعها وتقرأها بصوت عالٍ"
            engine.speak(currentReadWord)
            return
        }
        runCatching {
            isListeningRead = true
            readStatus = "اقرأ كلمة «$currentReadWord» بصوت واضح يا بطل..."
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListeningRead = false }
                override fun onError(error: Int) {
                    isListeningRead = false
                    readStatus = "حاول القراءة مرة أخرى أو اضغط على الكلمة لتسمعها"
                }
                override fun onResults(results: Bundle?) {
                    isListeningRead = false
                    val spokenList = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val cleanTarget = stripArabicDiacritics(currentReadWord)
                    val matched = spokenList.any { cand ->
                        val cleanCand = stripArabicDiacritics(cand)
                        cleanCand.contains(cleanTarget) || cleanTarget.contains(cleanCand) && cleanCand.length >= 2
                    }
                    if (matched) {
                        readStatus = "رائع يا بطل! قرأت «$currentReadWord» بشكل صحيح!"
                        onReadSuccess(currentReadWord)
                    } else {
                        engine.playGentleTryAgain()
                        readStatus = "قريب جداً! استمع إلى «$currentReadWord» ثم حاول مرة أخرى"
                        engine.speak(currentReadWord)
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
            isListeningRead = false
            readStatus = "اضغط على الكلمات لتسمعها وتتدرب على قراءتها"
        }
    }

    val readPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceReadCheck()
        else readStatus = "اضغط على الكلمات لتسمعها وتقرأها مع العصفور الأزرق"
    }

    // --- سبورة التتبع الدقيقة بالـ Bitmap 160x160 ---
    var formIdx by remember(lesson.id) { mutableIntStateOf(0) }
    val activeTraceTarget = lesson.tracingForms[formIdx % lesson.tracingForms.size]
    val crayonColors = remember {
        listOf(
            ReadingColors.CoralOrange,
            ReadingColors.SkyBlue,
            ReadingColors.MeadowGreen,
            ReadingColors.BerryPurple
        )
    }
    var selectedCrayon by remember { mutableStateOf(crayonColors.first()) }
    val completedStrokes = remember(lesson.id, formIdx) { mutableStateListOf<QiraatiStroke>() }
    val activePoints = remember(lesson.id, formIdx) { mutableStateListOf<Offset>() }
    var boardPixelSize by remember { mutableStateOf(IntSize.Zero) }
    var writeHint by remember(lesson.id, formIdx) {
        mutableStateOf("تتبّع الحرف ($activeTraceTarget) فوق الخط الرمادي بإصبعك")
    }
    var isWriteWarning by remember(lesson.id, formIdx) { mutableStateOf(false) }

    val pulse = rememberInfiniteTransition(label = "qiraati_dot_pulse")
    val dotR by pulse.animateFloat(
        initialValue = 10f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qiraati_dot_r"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // بطاقة العصفور الأزرق "أقرأ"
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.5.dp, ReadingColors.SkyBlue.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReadingBlueBirdCanvas(
                    bannerText = "أقرأ",
                    isCelebrating = isListeningRead
                )

                Text(
                    text = readStatus,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = ReadingColors.InkSoft,
                    textAlign = TextAlign.Center
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    lesson.readAloudWords.forEachIndexed { idx, word ->
                        val selected = idx == selectedReadWordIdx
                        PlayfulBouncyButton(
                            onClick = {
                                selectedReadWordIdx = idx
                                engine.playTapPop()
                                engine.speak(word)
                            },
                            backgroundColor = if (selected) ReadingColors.SkyBlue else Color(0xFFF0F9FF),
                            shadowColor = if (selected) ReadingColors.SkyBlueShadow else Color(0xFFBAE6FD),
                            cornerRadius = 20.dp,
                            lipDepth = 4.dp,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            testTagValue = "qiraati_read_word_$idx"
                        ) {
                            Text(
                                text = word,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                lineHeight = 36.sp,
                                color = if (selected) Color.White else ReadingColors.InkDark
                            )
                        }
                    }
                }

                PlayfulBouncyButton(
                    onClick = {
                        engine.playTapPop()
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) startVoiceReadCheck()
                        else readPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    backgroundColor = if (isListeningRead) ReadingColors.CoralOrange else ReadingColors.BerryPurple,
                    shadowColor = if (isListeningRead) ReadingColors.CoralShadow else ReadingColors.BerryShadow,
                    cornerRadius = 18.dp,
                    lipDepth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    testTagValue = "qiraati_read_mic_btn"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlayfulMicIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (isListeningRead) "جاري الاستماع..." else "اقرأ «$currentReadWord» بصوتك",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // قسم سبورة التتبع والكتابة الدقيقة بالـ Bitmap
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.5.dp, ReadingColors.SunYellow, RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سبورة الكتابة السحرية",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = ReadingColors.InkDark
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        lesson.tracingForms.forEachIndexed { idx, form ->
                            val selected = idx == formIdx
                            PlayfulBouncyButton(
                                onClick = {
                                    engine.playTapPop()
                                    formIdx = idx
                                    engine.speak(lesson.letterName)
                                },
                                backgroundColor = if (selected) lesson.accentColor else Color(0xFFF1F5F9),
                                shadowColor = if (selected) lesson.shadowColor else Color(0xFFCBD5E1),
                                cornerRadius = 14.dp,
                                lipDepth = 3.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = form,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = if (selected) Color.White else ReadingColors.InkDark
                                )
                            }
                        }
                    }
                }

                Text(
                    text = writeHint,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = if (isWriteWarning) ReadingColors.CoralShadow else ReadingColors.InkSoft,
                    textAlign = TextAlign.Center
                )

                // لوحة الرسم
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .onSizeChanged { boardPixelSize = it }
                        .clip(RoundedCornerShape(24.dp))
                        .background(ReadingColors.NotebookPaper)
                        .border(4.dp, ReadingColors.SunYellow, RoundedCornerShape(24.dp))
                        .pointerInput(selectedCrayon, activeTraceTarget) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isWriteWarning = false
                                    activePoints.clear()
                                    activePoints.add(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    activePoints.add(change.position)
                                },
                                onDragEnd = {
                                    if (activePoints.isNotEmpty()) {
                                        completedStrokes.add(QiraatiStroke(selectedCrayon, activePoints.toList()))
                                        activePoints.clear()
                                    }
                                },
                                onDragCancel = {
                                    if (activePoints.isNotEmpty()) {
                                        completedStrokes.add(QiraatiStroke(selectedCrayon, activePoints.toList()))
                                        activePoints.clear()
                                    }
                                }
                            )
                        }
                        .testTag("qiraati_writing_board_canvas")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        listOf(0.24f, 0.44f, 0.64f, 0.82f).forEachIndexed { i, frac ->
                            drawLine(
                                color = if (i == 2) ReadingColors.CoralOrange.copy(alpha = 0.35f) else ReadingColors.SkyBlue.copy(alpha = 0.30f),
                                start = Offset(16f, h * frac),
                                end = Offset(w - 16f, h * frac),
                                strokeWidth = if (i == 2) 4f else 2.5f
                            )
                        }

                        val layout = computeQiraatiGuideLayout(activeTraceTarget, w, h, tajawalTypeface)
                        val guidePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            typeface = tajawalTypeface
                            textSize = layout.textSizePx
                            color = AndroidColor.argb(56, 30, 41, 59)
                            style = AndroidPaint.Style.FILL
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            activeTraceTarget,
                            layout.drawX,
                            layout.drawY,
                            guidePaint
                        )

                        // نقطة البداية الخضراء النابضة يمين الحرف
                        val rightStart = Offset(
                            (layout.drawX + layout.bounds.right - layout.bounds.width() * 0.10f).coerceIn(w * 0.18f, w * 0.86f),
                            h * 0.48f
                        )
                        drawCircle(ReadingColors.MeadowGreen.copy(alpha = 0.28f), dotR * 1.55f, rightStart)
                        drawCircle(ReadingColors.MeadowGreen, 11f, rightStart)
                        drawCircle(Color.White, 4f, rightStart)

                        fun drawSmooth(color: Color, pts: List<Offset>) {
                            if (pts.isEmpty()) return
                            if (pts.size == 1) {
                                drawCircle(color, 13f, pts.first())
                                return
                            }
                            val p = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    val prev = pts[i - 1]
                                    val curr = pts[i]
                                    quadraticBezierTo(prev.x, prev.y, (prev.x + curr.x) * 0.5f, (prev.y + curr.y) * 0.5f)
                                }
                                lineTo(pts.last().x, pts.last().y)
                            }
                            drawPath(p, color = color, style = Stroke(26f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }

                        completedStrokes.forEach { drawSmooth(it.color, it.points) }
                        drawSmooth(selectedCrayon, activePoints)
                    }
                }

                // ألوان الأقلام الشمعية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    crayonColors.forEachIndexed { idx, col ->
                        val selected = selectedCrayon == col
                        Box(
                            modifier = Modifier
                                .size(if (selected) 52.dp else 46.dp)
                                .clip(CircleShape)
                                .background(if (selected) col.copy(alpha = 0.22f) else Color.White)
                                .border(if (selected) 3.dp else 1.5.dp, if (selected) col else Color(0xFFCBD5E1), CircleShape)
                                .clickable {
                                    engine.playTapPop()
                                    selectedCrayon = col
                                }
                                .testTag("qiraati_crayon_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayfulCrayonIcon(crayonColor = col, modifier = Modifier.size(30.dp))
                        }
                    }
                }

                // أزرار التحكم بالسبورة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            completedStrokes.clear()
                            activePoints.clear()
                            isWriteWarning = false
                            writeHint = "تتبّع الحرف ($activeTraceTarget) فوق الخط الرمادي بإصبعك"
                        },
                        backgroundColor = ReadingColors.CandyPink,
                        shadowColor = ReadingColors.CandyShadow,
                        cornerRadius = 18.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f),
                        testTagValue = "qiraati_write_clear_btn"
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
                                color = Color.White
                            )
                        }
                    }

                    PlayfulBouncyButton(
                        onClick = {
                            engine.playTapPop()
                            engine.speak(lesson.letterName)
                        },
                        backgroundColor = ReadingColors.SkyBlue,
                        shadowColor = ReadingColors.SkyBlueShadow,
                        cornerRadius = 18.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f),
                        testTagValue = "qiraati_write_hear_btn"
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
                                color = Color.White
                            )
                        }
                    }

                    PlayfulBouncyButton(
                        onClick = {
                            val eval = evaluateQiraatiTracingBitmap(
                                targetText = activeTraceTarget,
                                strokes = completedStrokes.toList(),
                                boardSize = boardPixelSize,
                                typeface = tajawalTypeface
                            )
                            if (eval.passed) {
                                isWriteWarning = false
                                writeHint = "ممتاز يا بطل! كتبت ($activeTraceTarget) بدقة رائعة"
                                onWriteSuccess(activeTraceTarget)
                            } else {
                                engine.playGentleTryAgain()
                                isWriteWarning = true
                                writeHint = "حاول تتبّع الحرف الرمادي بدقة يا بطل!"
                                engine.speak("حاول تتبع الحرف الرمادي بدقة يا بطل")
                            }
                        },
                        backgroundColor = ReadingColors.MeadowGreen,
                        shadowColor = ReadingColors.MeadowShadow,
                        cornerRadius = 18.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1.35f),
                        testTagValue = "qiraati_write_check_btn"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            PlayfulCheckIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = "أنهيت الكتابة",
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StationNextFooterButton(
    label: String,
    onClick: () -> Unit
) {
    PlayfulBouncyButton(
        onClick = onClick,
        backgroundColor = ReadingColors.MeadowGreen,
        shadowColor = ReadingColors.MeadowShadow,
        cornerRadius = 22.dp,
        lipDepth = 5.dp,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontFamily = ReadingTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}
