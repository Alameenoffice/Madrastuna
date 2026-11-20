package com.example.reading

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * الشاشة الرئيسية لخريطة مغامرة الحروف في واحة القراءة الذكية.
 */
@Composable
fun ReadingMapScreen(
    studentName: String,
    progress: ReadingProgressState,
    isSpeaking: Boolean,
    onSelectLesson: (ReadingLesson) -> Unit,
    onGreetTap: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val lessons = ReadingCurriculum.lessons
    val displayStudent = studentName.trim().ifBlank { "بطل القراءة" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ReadingColors.SkyTop,
                        ReadingColors.SkyBackground,
                        Color(0xFFF3FBF4)
                    )
                )
            )
            .verticalScroll(scrollState)
            .navigationBarsPadding()
    ) {
        // المشهد العلوي المتحرك: سماء، شمس مبتسمة، غيوم عائمة، تلال خضراء، وفصيح
        ReadingMapHeroHeader(
            studentName = displayStudent,
            totalStars = progress.totalStars,
            isSpeaking = isSpeaking,
            onGreetTap = onGreetTap,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(8.dp))

        // عنوان الخريطة التشجيعي
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PlayfulCrownIcon(modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "خريطة مغامرة الحروف",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = ReadingColors.InkDark
            )
        }

        Text(
            text = "اضغط على أي جزيرة لتبدأ التعلّم واللعب",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ReadingColors.InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp)
        )

        // خريطة الجزر الثمانية على مسار متعرج منقط (S-Curve)
        AdventureIslandsPath(
            lessons = lessons,
            progress = progress,
            onSelectLesson = onSelectLesson
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ReadingMapHeroHeader(
    studentName: String,
    totalStars: Int,
    isSpeaking: Boolean,
    onGreetTap: () -> Unit,
    onBack: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "map_sky_anim")
    val sunRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_spin"
    )
    val cloudDrift by infinite.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_drift"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(274.dp)
    ) {
        // خلفية السماء والشمس والغيوم والتلال الخضراء المرسومة على Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7DD3FC),
                        Color(0xFFBAE6FD),
                        ReadingColors.SkyBackground
                    )
                )
            )

            // شمس مبتسمة بأشعة دوّارة في الزاوية اليسرى العلوية
            val sunCenter = Offset(w * 0.16f, h * 0.28f)
            rotate(degrees = sunRotation, pivot = sunCenter) {
                for (i in 0 until 10) {
                    val angle = i * (2.0 * PI / 10.0)
                    val start = Offset(
                        sunCenter.x + (32f * cos(angle)).toFloat(),
                        sunCenter.y + (32f * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        sunCenter.x + (46f * cos(angle)).toFloat(),
                        sunCenter.y + (46f * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = ReadingColors.SunYellow,
                        start = start,
                        end = end,
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF7B2), ReadingColors.SunYellow, Color(0xFFF59E0B)),
                    center = sunCenter,
                    radius = 30f
                ),
                radius = 27f,
                center = sunCenter
            )
            // وجه الشمس المبتسم
            drawCircle(color = ReadingColors.InkDark, radius = 2.6f, center = Offset(sunCenter.x - 8f, sunCenter.y - 4f))
            drawCircle(color = ReadingColors.InkDark, radius = 2.6f, center = Offset(sunCenter.x + 8f, sunCenter.y - 4f))
            val sunSmile = Path().apply {
                moveTo(sunCenter.x - 8f, sunCenter.y + 5f)
                quadraticBezierTo(sunCenter.x, sunCenter.y + 13f, sunCenter.x + 8f, sunCenter.y + 5f)
            }
            drawPath(sunSmile, color = ReadingColors.CoralOrange, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // غيمتان بيضاوان تتحركان ببطء
            val c1x = w * 0.56f + cloudDrift
            val c1y = h * 0.22f
            drawCircle(color = Color.White.copy(alpha = 0.88f), radius = 19f, center = Offset(c1x, c1y))
            drawCircle(color = Color.White.copy(alpha = 0.88f), radius = 24f, center = Offset(c1x + 20f, c1y - 6f))
            drawCircle(color = Color.White.copy(alpha = 0.88f), radius = 18f, center = Offset(c1x + 42f, c1y))

            val c2x = w * 0.78f - cloudDrift * 0.7f
            val c2y = h * 0.36f
            drawCircle(color = Color.White.copy(alpha = 0.78f), radius = 15f, center = Offset(c2x, c2y))
            drawCircle(color = Color.White.copy(alpha = 0.78f), radius = 20f, center = Offset(c2x + 16f, c2y - 4f))
            drawCircle(color = Color.White.copy(alpha = 0.78f), radius = 14f, center = Offset(c2x + 32f, c2y))

            // تل أخضر خلفي ناعم
            val backHill = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.25f, h * 0.58f, w * 0.65f, h * 0.62f, w, h * 0.74f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(backHill, color = ReadingColors.HillLightGreen)

            // تل أخضر أمامي مبهج
            val frontHill = Path().apply {
                moveTo(0f, h * 0.86f)
                cubicTo(w * 0.32f, h * 0.72f, w * 0.72f, h * 0.94f, w, h * 0.80f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                frontHill,
                brush = Brush.verticalGradient(
                    colors = listOf(ReadingColors.HillDeepGreen, ReadingColors.SkyBackground),
                    startY = h * 0.75f,
                    endY = h
                )
            )
        }

        // الشريط العلوي: زر الرجوع وعداد النجوم الذهبية
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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
                    testTagValue = "reading_map_back_btn"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlayfulBackArrowIcon(
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "رجوع",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                // عداد النجوم الذهبية البارز
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.testTag("reading_map_stars_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlayfulStarIcon(
                            filled = true,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "$totalStars نجمة",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ReadingColors.InkDark
                        )
                    }
                }
            }

            // بطاقة ترحيب فصيح بالتلميذ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReadingMascotCanvas(
                    modifier = Modifier.size(104.dp),
                    isCelebrating = totalStars > 0 && isSpeaking,
                    isSpeaking = isSpeaking
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "مرحباً يا $studentName!",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = ReadingColors.InkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "أنا صديقك فصيح في واحة القراءة الذكية، هيا نجمع النجوم معاً!",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ReadingColors.InkSoft,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        PlayfulBouncyButton(
                            onClick = onGreetTap,
                            backgroundColor = ReadingColors.SkyBlue,
                            shadowColor = ReadingColors.SkyBlueShadow,
                            cornerRadius = 16.dp,
                            lipDepth = 4.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            testTagValue = "reading_map_greet_btn"
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PlayfulSpeakerIcon(
                                    isSpeaking = isSpeaking,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "اسمع تحية فصيح",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
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

@Composable
private fun AdventureIslandsPath(
    lessons: List<ReadingLesson>,
    progress: ReadingProgressState,
    onSelectLesson: (ReadingLesson) -> Unit
) {
    // نمط التعرج الأفقي (S-Curve) لنقاط الجزر الثمانية
    val xFractions = listOf(0.50f, 0.25f, 0.72f, 0.26f, 0.74f, 0.25f, 0.72f, 0.50f)
    val rowSpacingDp = 158.dp
    val islandDiameterDp = 124.dp
    val topPaddingDp = 16.dp
    val totalHeightDp = topPaddingDp + rowSpacingDp * lessons.size + 28.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeightDp)
            .padding(horizontal = 12.dp)
    ) {
        val mapWidth = maxWidth

        // مسار متعرج منقط مرسوم بـ Path.cubicTo يربط الجزر الثمانية
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centers = lessons.indices.map { idx ->
                val frac = xFractions[idx % xFractions.size]
                val cx = size.width * frac
                val cy = topPaddingDp.toPx() + idx * rowSpacingDp.toPx() + islandDiameterDp.toPx() * 0.5f
                Offset(cx, cy)
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
                // ظل الطريق الترابي اللطيف
                drawPath(
                    path = path,
                    color = Color(0xFFFFFFFF).copy(alpha = 0.85f),
                    style = Stroke(width = 22f, cap = StrokeCap.Round)
                )
                // الخط المنقط الملون فوق الطريق
                drawPath(
                    path = path,
                    color = ReadingColors.SunYellow,
                    style = Stroke(
                        width = 10f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 18f), 0f)
                    )
                )
            }
        }

        // وضع كل جزيرة دائرية بارزة على موضعها في المسار
        lessons.forEachIndexed { index, lesson ->
            val frac = xFractions[index % xFractions.size]
            val cardWidth = 156.dp
            val xOffset = (mapWidth * frac - cardWidth / 2f).coerceIn(0.dp, (mapWidth - cardWidth).coerceAtLeast(0.dp))
            val yOffset = topPaddingDp + rowSpacingDp * index
            val starsEarned = progress.starsForLesson(lesson.id)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(cardWidth)
                    .offset(x = xOffset, y = yOffset)
            ) {
                // الجزيرة الدائرية البارزة مع الحرف العملاق والرسم المصغر
                PlayfulBouncyButton(
                    onClick = { onSelectLesson(lesson) },
                    backgroundColor = lesson.islandColor,
                    shadowColor = lesson.islandShadowColor,
                    cornerRadius = 60.dp,
                    lipDepth = 6.dp,
                    contentPadding = PaddingValues(6.dp),
                    modifier = Modifier.size(116.dp),
                    testTagValue = "reading_lesson_island_${lesson.number}"
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // حلقة بيضاء ناعمة داخل الجزيرة
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            LessonMiniIllustrationCanvas(
                                artType = lesson.artType,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = lesson.letter,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (lesson.letter.length > 2) 26.sp else 36.sp,
                                color = Color.White,
                                lineHeight = 38.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // شارة اسم الدرس ورقمه
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${lesson.number}. ${lesson.title}",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = ReadingColors.InkDark,
                            maxLines = 1
                        )
                        // النجوم الثلاث للدرس
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            for (starIdx in 1..3) {
                                PlayfulStarIcon(
                                    filled = starIdx <= starsEarned,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
