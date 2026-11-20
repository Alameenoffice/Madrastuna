package com.example.reading

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * الشاشة المنسّقة الرئيسية لقسم «واحة القراءة الذكية».
 * تدير التنقل السلس بين خريطة المغامرة (ReadingMapScreen) وشاشة الدرس التفاعلي (ReadingLessonScreen)
 * مع دعم زر الرجوع وإيقاف وتنظيف محرك النطق تلقائياً عند مغادرة الدرس أو القسم.
 */
@Composable
fun ReadingOasisScreen(
    schoolCode: String,
    studentId: String,
    studentName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progressEngine = remember(context) { ReadingProgressEngine(context) }

    var selectedLesson by remember { mutableStateOf<ReadingLesson?>(null) }
    var currentProgress by remember(schoolCode, studentId) {
        mutableStateOf(progressEngine.loadLocalProgress(schoolCode, studentId))
    }

    LaunchedEffect(schoolCode, studentId) {
        currentProgress = progressEngine.loadLocalProgress(schoolCode, studentId)
        currentProgress = progressEngine.syncAndMergeProgress(schoolCode, studentId)
    }

    DisposableEffect(progressEngine) {
        onDispose {
            progressEngine.close()
        }
    }

    // زر الرجوع في النظام يعود من الدرس إلى الخريطة، ومن الخريطة إلى شاشة مدرستي السابقة
    BackHandler {
        if (selectedLesson != null) {
            selectedLesson = null
        } else {
            onBackClick()
        }
    }

    AnimatedContent(
        targetState = selectedLesson,
        transitionSpec = {
            (fadeIn(tween(240)) + slideInHorizontally(tween(260)) { fullW ->
                if (targetState != null) -fullW / 5 else fullW / 5
            }).togetherWith(
                fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { fullW ->
                    if (targetState != null) fullW / 5 else -fullW / 5
                }
            )
        },
        modifier = modifier.fillMaxSize(),
        label = "reading_oasis_navigation"
    ) { activeLesson ->
        if (activeLesson == null) {
            ReadingMapScreen(
                studentName = studentName,
                schoolCode = schoolCode,
                studentId = studentId,
                progressOverride = currentProgress,
                onLessonSelected = { lesson ->
                    selectedLesson = lesson
                },
                onBackClick = onBackClick
            )
        } else {
            ReadingLessonScreen(
                lesson = activeLesson,
                schoolCode = schoolCode,
                studentId = studentId,
                studentName = studentName,
                onBackClick = {
                    selectedLesson = null
                },
                onProgressUpdated = { updated ->
                    currentProgress = updated
                }
            )
        }
    }
}

/**
 * بطاقة دخول جذابة وواضحة لواحة القراءة الذكية مخصصة للشاشة الرئيسية لولي الأمر والطالب.
 * تعرض ترحيباً باسم الطالب وعداد نجومه الحالية وتفتح القسم مباشرة.
 */
@Composable
fun ReadingOasisHomeEntryCard(
    schoolCode: String,
    studentId: String,
    studentName: String,
    onOpenReadingOasis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progressEngine = remember(context) { ReadingProgressEngine(context) }
    var progress by remember(schoolCode, studentId) {
        mutableStateOf(progressEngine.loadLocalProgress(schoolCode, studentId))
    }

    LaunchedEffect(schoolCode, studentId) {
        progress = progressEngine.loadLocalProgress(schoolCode, studentId)
    }

    DisposableEffect(progressEngine) {
        onDispose { progressEngine.close() }
    }

    val cleanName = studentName.trim().ifBlank { "بطل القراءة" }
    val cardShape = RoundedCornerShape(26.dp)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .clip(cardShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFCCF0FF),
                            ReadingOasisColors.SkySoft,
                            Color(0xFFFFF8DC)
                        )
                    )
                )
                .border(2.5.dp, ReadingOasisColors.SunnyYellow, cardShape)
                .clickable(role = Role.Button, onClick = onOpenReadingOasis)
                .testTag("dashboard_reading_oasis_entry_card")
        ) {
            // تلة خضراء لطيفة وشمس خفيفة في خلفية البطاقة
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ReadingOasisColors.SunnyYellow.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.12f, h * 0.18f),
                        radius = w * 0.28f
                    ),
                    radius = w * 0.28f,
                    center = Offset(w * 0.12f, h * 0.18f)
                )
                val hill = Path().apply {
                    moveTo(0f, h * 0.86f)
                    cubicTo(w * 0.30f, h * 0.68f, w * 0.72f, h * 0.96f, w, h * 0.78f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(hill, color = ReadingOasisColors.Mint.copy(alpha = 0.16f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReadingMascotCanvas(
                    isCelebrating = progress.totalStars > 0,
                    modifier = Modifier.size(74.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "واحة القراءة الذكية",
                            color = ReadingOasisColors.InkDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "هيا نقرأ ونكتب معاً يا $cleanName بالطريقة المقطعية!",
                        color = ReadingOasisColors.InkMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(ReadingOasisColors.White)
                                .border(1.5.dp, ReadingOasisColors.SunnyYellow, RoundedCornerShape(14.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PlayfulStarCanvas(filled = true, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${progress.totalStars} / ${ReadingCurriculum.totalPossibleStars}",
                                color = ReadingOasisColors.InkDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        PlayfulBouncyButton(
                            onClick = onOpenReadingOasis,
                            containerColor = ReadingOasisColors.Mint,
                            shadowColor = ReadingOasisColors.MintShadow,
                            cornerRadius = 16.dp,
                            shadowDepth = 4.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                            modifier = Modifier.testTag("dashboard_reading_oasis_start_btn")
                        ) {
                            PlayfulBookIconCanvas(
                                color = ReadingOasisColors.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ابدأ المغامرة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
