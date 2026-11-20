package com.example.reading

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * خريطة مغامرة "واحة القراءة الذكية":
 * مشهد علوي متحرك بتلال خضراء وشمس مبتسمة وغيوم سابحة، يتبعه مسار مغامرة متعرّج
 * يضم الدروس الـ 8 العراقية كجزر دائرية بارزة ومبهجة.
 */
@Composable
fun ReadingMapScreen(
    studentName: String,
    schoolCode: String,
    studentId: String,
    onLessonSelected: (ReadingLesson) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    progressOverride: StudentReadingProgress? = null
) {
    val context = LocalContext.current
    val progressEngine = remember(context) { ReadingProgressEngine(context) }

    var progress by remember(schoolCode, studentId) {
        mutableStateOf(
            progressOverride ?: progressEngine.loadLocalProgress(schoolCode, studentId)
        )
    }

    LaunchedEffect(progressOverride) {
        if (progressOverride != null) {
            progress = progressOverride
        }
    }

    LaunchedEffect(schoolCode, studentId) {
        if (progressOverride == null) {
            progress = progressEngine.loadLocalProgress(schoolCode, studentId)
            val merged = progressEngine.syncAndMergeProgress(schoolCode, studentId)
            progress = merged
        }
    }

    DisposableEffect(progressEngine) {
        onDispose { progressEngine.close() }
    }

    val lessons = ReadingCurriculum.lessons
    val recommendedLessonId = remember(progress) {
        lessons.firstOrNull { (progress.lessons[it.id]?.starsEarned ?: 0) == 0 }?.id
            ?: progress.lastLessonId
    }
    val displayName = studentName.trim().ifBlank { "بطل القراءة" }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFC8ECFF),
                            ReadingOasisColors.SkySoft,
                            Color(0xFFF3FBFF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // المشهد العلوي المتحرك مع التلال والشمس والغيوم وترحيب فصيح
                ReadingAdventureHeroHeader(
                    studentName = displayName,
                    totalStars = progress.totalStars,
                    maxStars = ReadingCurriculum.totalPossibleStars,
                    completedLessons = progress.completedLessonsCount,
                    totalLessons = lessons.size,
                    onBackClick = onBackClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                // مسار المغامرة المتعرّج (S-Curve) للدروس الثمانية
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    ReadingWindingAdventurePath(
                        lessons = lessons,
                        progress = progress,
                        recommendedLessonId = recommendedLessonId,
                        onLessonSelected = onLessonSelected
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // بطاقة تشجيعية في نهاية الخريطة
                Row(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ReadingOasisColors.White)
                        .border(2.dp, ReadingOasisColors.SunnyYellow.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayfulCrownCanvas(modifier = Modifier.size(38.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "كل درس تكمله يضيء 3 نجوم ذهبية!",
                            color = ReadingOasisColors.InkDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "اضغط على أي جزيرة لتبدأ القراءة والكتابة واللعب",
                            color = ReadingOasisColors.InkMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * المشهد العلوي المتحرك: سماء صافية، شمس مبتسمة تدور أشعتها، غيوم تسبح، تلال خضراء منحنية،
 * وبطاقة ترحيب رفيق القراءة "فصيح" مع عداد النجوم الذهبية.
 */
@Composable
private fun ReadingAdventureHeroHeader(
    studentName: String,
    totalStars: Int,
    maxStars: Int,
    completedLessons: Int,
    totalLessons: Int,
    onBackClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sky_scene")
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_spin"
    )
    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = -0.06f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_drift"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(295.dp)
    ) {
        // الخلفية المرسومة: السماء، الشمس، الغيوم، والتلال الخضراء المنحنية
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // تدرج السماء المشرقة
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF9BE2FF),
                        Color(0xFFD5F3FF),
                        ReadingOasisColors.SkySoft
                    )
                )
            )

            // الشمس المبتسمة مع أشعة تدور بنعومة
            val sunCenter = Offset(w * 0.16f, h * 0.28f)
            val sunRadius = minOf(w, h) * 0.12f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ReadingOasisColors.SunnyYellow.copy(alpha = 0.36f),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = sunRadius * 2.1f
                ),
                radius = sunRadius * 2.1f,
                center = sunCenter
            )
            rotate(degrees = sunRotation, pivot = sunCenter) {
                for (i in 0 until 10) {
                    val angle = (i * 36.0) * PI / 180.0
                    val start = Offset(
                        sunCenter.x + (cos(angle) * sunRadius * 1.18f).toFloat(),
                        sunCenter.y + (sin(angle) * sunRadius * 1.18f).toFloat()
                    )
                    val end = Offset(
                        sunCenter.x + (cos(angle) * sunRadius * 1.62f).toFloat(),
                        sunCenter.y + (sin(angle) * sunRadius * 1.62f).toFloat()
                    )
                    drawLine(
                        color = ReadingOasisColors.SunnyYellow,
                        start = start,
                        end = end,
                        strokeWidth = 4.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF59D), ReadingOasisColors.SunnyYellow),
                    center = Offset(sunCenter.x - sunRadius * 0.2f, sunCenter.y - sunRadius * 0.2f),
                    radius = sunRadius * 1.2f
                ),
                radius = sunRadius,
                center = sunCenter
            )
            // وجه الشمس المبتسم
            drawCircle(ReadingOasisColors.InkDark, radius = sunRadius * 0.10f, center = Offset(sunCenter.x - sunRadius * 0.30f, sunCenter.y - sunRadius * 0.12f))
            drawCircle(ReadingOasisColors.InkDark, radius = sunRadius * 0.10f, center = Offset(sunCenter.x + sunRadius * 0.30f, sunCenter.y - sunRadius * 0.12f))
            val sunSmile = Path().apply {
                moveTo(sunCenter.x - sunRadius * 0.28f, sunCenter.y + sunRadius * 0.18f)
                quadraticBezierTo(sunCenter.x, sunCenter.y + sunRadius * 0.44f, sunCenter.x + sunRadius * 0.28f, sunCenter.y + sunRadius * 0.18f)
            }
            drawPath(sunSmile, color = ReadingOasisColors.InkDark, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))

            // غيوم بيضاء ناعمة تسبح ببطء
            drawFluffyCloud(center = Offset(w * (0.48f + cloudDrift), h * 0.22f), scale = w * 0.14f, alpha = 0.90f)
            drawFluffyCloud(center = Offset(w * (0.82f - cloudDrift * 0.8f), h * 0.32f), scale = w * 0.11f, alpha = 0.82f)
            drawFluffyCloud(center = Offset(w * (0.30f - cloudDrift * 0.5f), h * 0.44f), scale = w * 0.08f, alpha = 0.65f)

            // التلة الخضراء الخلفية
            val backHill = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.26f, h * 0.56f, w * 0.58f, h * 0.60f, w, h * 0.74f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = backHill,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF86EFAC), Color(0xFF4ADE80))
                )
            )

            // التلة الخضراء الأمامية
            val frontHill = Path().apply {
                moveTo(0f, h * 0.84f)
                cubicTo(w * 0.32f, h * 0.70f, w * 0.68f, h * 0.94f, w, h * 0.78f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = frontHill,
                brush = Brush.verticalGradient(
                    colors = listOf(ReadingOasisColors.Mint, Color(0xFF16A34A))
                )
            )

            // اندماج سفلي لطيف مع خلفية الخريطة السماوية
            val bottomArc = Path().apply {
                moveTo(0f, h * 0.94f)
                quadraticBezierTo(w * 0.50f, h * 1.03f, w, h * 0.94f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(bottomArc, color = ReadingOasisColors.SkySoft)
        }

        // محتوى الشريط العلوي وبطاقة الترحيب
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // شريط علوي: زر الرجوع + عنوان الواحة + شارة النجوم الذهبية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayfulBouncyButton(
                    onClick = onBackClick,
                    containerColor = ReadingOasisColors.Coral,
                    shadowColor = ReadingOasisColors.CoralShadow,
                    cornerRadius = 18.dp,
                    shadowDepth = 4.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("reading_map_back_btn")
                ) {
                    PlayfulBackArrowCanvas(modifier = Modifier.size(22.dp))
                }

                Text(
                    text = "واحة القراءة الذكية",
                    color = ReadingOasisColors.InkDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // شارة عداد النجوم الذهبية البارزة
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ReadingOasisColors.White)
                        .border(2.5.dp, ReadingOasisColors.SunnyYellow, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("reading_map_stars_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PlayfulStarCanvas(filled = true, glowing = true, modifier = Modifier.size(24.dp))
                    Text(
                        text = "$totalStars / $maxStars",
                        color = ReadingOasisColors.InkDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // بطاقة الترحيب مع رفيق القراءة المبتسم "فصيح"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(26.dp))
                    .background(ReadingOasisColors.White.copy(alpha = 0.96f))
                    .border(2.5.dp, ReadingOasisColors.SkyBright.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReadingMascotCanvas(
                    isCelebrating = totalStars > 0,
                    modifier = Modifier.size(76.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "أهلاً بك يا $studentName!",
                        color = ReadingOasisColors.InkDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "أنا صديقك فصيح، هيا نكتشف الحروف والمقاطع في خريطة المغامرة!",
                        color = ReadingOasisColors.InkMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // شريط التقدم الملوّن
                    val ratio = if (totalLessons > 0) (completedLessons.toFloat() / totalLessons).coerceIn(0f, 1f) else 0f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE0F2FE))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio.coerceAtLeast(0.06f))
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(ReadingOasisColors.Mint, ReadingOasisColors.SunnyYellow)
                                        )
                                    )
                            )
                        }
                        Text(
                            text = "$completedLessons / $totalLessons دروس",
                            color = ReadingOasisColors.MintShadow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFluffyCloud(
    center: Offset,
    scale: Float,
    alpha: Float
) {
    val cloudPath = Path().apply {
        moveTo(center.x - scale * 0.9f, center.y + scale * 0.25f)
        cubicTo(
            center.x - scale * 1.15f, center.y - scale * 0.10f,
            center.x - scale * 0.72f, center.y - scale * 0.45f,
            center.x - scale * 0.38f, center.y - scale * 0.26f
        )
        cubicTo(
            center.x - scale * 0.18f, center.y - scale * 0.72f,
            center.x + scale * 0.42f, center.y - scale * 0.68f,
            center.x + scale * 0.48f, center.y - scale * 0.22f
        )
        cubicTo(
            center.x + scale * 0.95f, center.y - scale * 0.30f,
            center.x + scale * 1.12f, center.y + scale * 0.18f,
            center.x + scale * 0.78f, center.y + scale * 0.28f
        )
        quadraticBezierTo(center.x, center.y + scale * 0.34f, center.x - scale * 0.9f, center.y + scale * 0.25f)
        close()
    }
    drawPath(cloudPath, color = Color.White.copy(alpha = alpha))
}

/**
 * مسار مغامرة متعرّج (S-Curve) يربط الجزر الثمانية بخط منقّط مرسوم بـ Path.cubicTo.
 */
@Composable
private fun ReadingWindingAdventurePath(
    lessons: List<ReadingLesson>,
    progress: StudentReadingProgress,
    recommendedLessonId: String,
    onLessonSelected: (ReadingLesson) -> Unit
) {
    val stepHeight = 166.dp
    val islandDiameter = 110.dp
    val totalHeight = stepHeight * lessons.size + 36.dp

    // نسب الإزاحة الأفقية لصناعة مسار S-Curve واضح وجميل
    val xFractions = remember(lessons.size) {
        listOf(0.76f, 0.26f, 0.74f, 0.26f, 0.74f, 0.26f, 0.74f, 0.28f)
    }

    val islandColors = remember {
        listOf(
            ReadingOasisColors.Mint,
            ReadingOasisColors.Coral,
            ReadingOasisColors.SkyBright,
            ReadingOasisColors.Purple,
            ReadingOasisColors.Pink,
            ReadingOasisColors.RoyalBlue,
            ReadingOasisColors.SunnyYellow,
            Color(0xFF14B8A6)
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        val density = LocalDensity.current
        val maxW = maxWidth
        val maxWPx = with(density) { maxW.toPx() }
        val stepHPx = with(density) { stepHeight.toPx() }
        val startYPx = with(density) { 86.dp.toPx() }

        // مسار S-Curve منقّط مرسوم خلف الجزر بـ Path.cubicTo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centers = lessons.indices.map { idx ->
                val frac = xFractions[idx % xFractions.size]
                Offset(
                    x = maxWPx * frac,
                    y = startYPx + idx * stepHPx
                )
            }

            // رسم المسار التمهيدي الناعم كطريق مغامرة رملي-سماوي
            val fullRoadPath = Path()
            centers.forEachIndexed { idx, point ->
                if (idx == 0) {
                    fullRoadPath.moveTo(point.x, point.y)
                } else {
                    val prev = centers[idx - 1]
                    val midY = (prev.y + point.y) / 2f
                    fullRoadPath.cubicTo(
                        prev.x, midY,
                        point.x, midY,
                        point.x, point.y
                    )
                }
            }

            // خلفية الطريق الدافئة العريضة
            drawPath(
                path = fullRoadPath,
                color = Color(0xFFD2EEFF),
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )

            // خط الخطوات المنقّط اللطيف
            drawPath(
                path = fullRoadPath,
                color = Color.White,
                style = Stroke(
                    width = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 24f), 0f)
                )
            )

            // إبراز أجزاء المسار التي أتمّها الطفل بلون ذهبي مشرق
            for (i in 0 until centers.lastIndex) {
                val prevCompleted = (progress.lessons[lessons[i].id]?.starsEarned ?: 0) > 0
                if (prevCompleted) {
                    val prev = centers[i]
                    val next = centers[i + 1]
                    val midY = (prev.y + next.y) / 2f
                    val completedSegment = Path().apply {
                        moveTo(prev.x, prev.y)
                        cubicTo(prev.x, midY, next.x, midY, next.x, next.y)
                    }
                    drawPath(
                        path = completedSegment,
                        color = ReadingOasisColors.SunnyYellow,
                        style = Stroke(
                            width = 8.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 24f), 0f)
                        )
                    )
                }
            }

            // لمسات طبيعية طفولية خفيفة (نقاط خضراء وزهرات صغيرة بجانب المسار)
            val decorOffsets = listOf(
                Offset(size.width * 0.12f, startYPx * 0.9f) to ReadingOasisColors.Mint,
                Offset(size.width * 0.88f, startYPx + stepHPx * 1.4f) to ReadingOasisColors.SunnyYellow,
                Offset(size.width * 0.14f, startYPx + stepHPx * 3.3f) to ReadingOasisColors.Pink,
                Offset(size.width * 0.86f, startYPx + stepHPx * 5.2f) to ReadingOasisColors.Purple
            )
            decorOffsets.forEach { (pt, col) ->
                drawCircle(color = col.copy(alpha = 0.22f), radius = 14.dp.toPx(), center = pt)
                drawCircle(color = col.copy(alpha = 0.45f), radius = 6.dp.toPx(), center = pt)
            }
        }

        // الجزر الدائرية البارزة مع بطاقات معلومات الدروس بجانب كل جزيرة
        lessons.forEachIndexed { index, lesson ->
            val xFrac = xFractions[index % xFractions.size]
            val isIslandOnRightInScreen = xFrac > 0.5f
            val islandColor = islandColors[index % islandColors.size]
            val record = progress.lessons[lesson.id]
            val starsEarned = record?.starsEarned ?: 0
            val isRecommended = lesson.id == recommendedLessonId

            val rowTopOffset = 14.dp + stepHeight * index

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = rowTopOffset)
                    .height(stepHeight - 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // تذكّر أننا في وضع RTL: العنصر الأول في Row يظهر على اليمين، والعنصر الثاني يظهر على اليسار!
                if (isIslandOnRightInScreen) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        LessonIslandNode(
                            lesson = lesson,
                            islandColor = islandColor,
                            starsEarned = starsEarned,
                            isRecommended = isRecommended,
                            diameter = islandDiameter,
                            onClick = { onLessonSelected(lesson) }
                        )
                    }
                    LessonIslandLabelCard(
                        lesson = lesson,
                        accentColor = islandColor,
                        isRecommended = isRecommended,
                        onClick = { onLessonSelected(lesson) },
                        modifier = Modifier
                            .weight(1.08f)
                            .padding(end = 6.dp)
                    )
                } else {
                    LessonIslandLabelCard(
                        lesson = lesson,
                        accentColor = islandColor,
                        isRecommended = isRecommended,
                        onClick = { onLessonSelected(lesson) },
                        modifier = Modifier
                            .weight(1.08f)
                            .padding(start = 6.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        LessonIslandNode(
                            lesson = lesson,
                            islandColor = islandColor,
                            starsEarned = starsEarned,
                            isRecommended = isRecommended,
                            diameter = islandDiameter,
                            onClick = { onLessonSelected(lesson) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * جزيرة دائرية بارزة لكل درس تحمل الحرف الضخم، رسمة الدرس اللطيفة، و3 نجوم ذهبية تتوهج فوقها.
 */
@Composable
private fun LessonIslandNode(
    lesson: ReadingLesson,
    islandColor: Color,
    starsEarned: Int,
    isRecommended: Boolean,
    diameter: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "island_press"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "island_scale"
    )

    val pulseTransition = rememberInfiniteTransition(label = "island_beacon")
    val beaconScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )

    val shadowColor = ReadingOasisColors.darken(islandColor, 0.72f)
    val lettersDisplay = lesson.targetLetters.joinToString(" ")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag("reading_lesson_island_${lesson.order}")
    ) {
        // 3 نجوم ذهبية مقوّسة فوق الجزيرة
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            PlayfulStarCanvas(
                filled = starsEarned >= 1,
                glowing = starsEarned >= 1,
                modifier = Modifier
                    .size(22.dp)
                    .offset(y = 3.dp)
            )
            PlayfulStarCanvas(
                filled = starsEarned >= 2,
                glowing = starsEarned >= 2,
                modifier = Modifier.size(28.dp)
            )
            PlayfulStarCanvas(
                filled = starsEarned >= 3,
                glowing = starsEarned >= 3,
                modifier = Modifier
                    .size(22.dp)
                    .offset(y = 3.dp)
            )
        }

        // الجزيرة الدائرية البارزة بظلها الملموس
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(diameter + 10.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
        ) {
            if (isRecommended) {
                Box(
                    modifier = Modifier
                        .size(diameter)
                        .graphicsLayer {
                            scaleX = beaconScale
                            scaleY = beaconScale
                        }
                        .clip(CircleShape)
                        .border(4.dp, ReadingOasisColors.SunnyYellow.copy(alpha = 0.75f), CircleShape)
                )
            }

            // قاعدة الجزيرة السفلية السميكة
            Box(
                modifier = Modifier
                    .size(diameter)
                    .offset(y = 7.dp)
                    .clip(CircleShape)
                    .background(shadowColor)
            )

            // سطح الجزيرة العلوي الملون
            Box(
                modifier = Modifier
                    .size(diameter)
                    .offset(y = pressOffset)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                islandColor.copy(alpha = 0.90f),
                                islandColor
                            )
                        )
                    )
                    .border(4.dp, ReadingOasisColors.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // الحرف المستهدف بخط ضخم
                Text(
                    text = lettersDisplay,
                    color = ReadingOasisColors.White,
                    fontSize = if (lesson.targetLetters.size > 1) 30.sp else 44.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-6).dp)
                )

                // شارة رسمة الدرس اللطيفة في أسفل الجزيرة
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ReadingOasisColors.White.copy(alpha = 0.92f))
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LessonIllustrationCanvas(
                        lessonId = lesson.id,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * بطاقة العنوان الجانبية لكل جزيرة درس مع شارة "ابدأ هنا" ومعاينة المقاطع.
 */
@Composable
private fun LessonIslandLabelCard(
    lesson: ReadingLesson,
    accentColor: Color,
    isRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(ReadingOasisColors.White)
            .border(
                width = if (isRecommended) 2.5.dp else 1.5.dp,
                color = if (isRecommended) ReadingOasisColors.SunnyYellow else accentColor.copy(alpha = 0.40f),
                shape = cardShape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "الدرس ${lesson.order}",
                    color = ReadingOasisColors.darken(accentColor, 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (isRecommended) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ReadingOasisColors.SunnyYellow)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ابدأ هنا",
                        color = ReadingOasisColors.InkDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Text(
            text = lesson.title,
            color = ReadingOasisColors.InkDark,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // معاينة أول 3 مقاطع في الدرس
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            lesson.syllables.take(3).forEach { syl ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(syl.toComposeColor().copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = syl.text,
                        color = syl.toComposeColor(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
