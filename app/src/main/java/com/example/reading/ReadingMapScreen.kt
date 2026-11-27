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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.reading.qiraati.QiraatiBookIconCanvas
import com.example.reading.qiraati.ReadingBlueBirdCanvas

/**
 * الشاشة الرئيسية لخريطة مغامرة الحروف في واحة القراءة الذكية
 * مع تدرّج ذكي للمستويات الثلاثة حسب الصف الدراسي للطالب ومبدّل أنيق للمراجعة.
 */
@Composable
fun ReadingMapScreen(
    studentName: String,
    studentGradeLabel: String,
    activeLevel: ReadingLevel,
    defaultStudentLevel: ReadingLevel,
    progress: ReadingProgressState,
    isSpeaking: Boolean,
    onSelectLevel: (ReadingLevel) -> Unit,
    onSelectLesson: (ReadingLesson) -> Unit,
    onGreetTap: () -> Unit,
    onBack: () -> Unit,
    onOpenNewQiraati: () -> Unit = {},
    onOpenAvatarPicker: () -> Unit = {},
    onOpenLeaderboard: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val lessons = ReadingCurriculum.lessonsForLevel(activeLevel)
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
            activeLevel = activeLevel,
            studentGradeLabel = studentGradeLabel,
            isShowingStudentDefault = activeLevel == defaultStudentLevel,
            totalStars = progress.totalStars,
            avatarId = progress.avatarId,
            isSpeaking = isSpeaking,
            onGreetTap = onGreetTap,
            onOpenAvatarPicker = onOpenAvatarPicker,
            onOpenLeaderboard = onOpenLeaderboard,
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(8.dp))

        // بطاقة الدخول المبهجة لكتاب "قراءتي الجديدة — الصف الأول الابتدائي"
        NewQiraatiMapEntryBanner(
            isGradeOnePriority = defaultStudentLevel == ReadingLevel.LEVEL_1,
            onOpenNewQiraati = onOpenNewQiraati
        )

        Spacer(modifier = Modifier.height(10.dp))

        // شريط التبديل الأنيق بين المستويات الثلاثة (الأول/الثاني — الثالث/الرابع — الخامس/السادس)
        ReadingLevelSwitcherBar(
            activeLevel = activeLevel,
            defaultStudentLevel = defaultStudentLevel,
            onSelectLevel = onSelectLevel
        )

        Spacer(modifier = Modifier.height(10.dp))

        // عنوان الخريطة التشجيعي للمستوى المختار
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
                text = "${activeLevel.title}: ${activeLevel.subtitle}",
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = ReadingColors.InkDark
            )
        }

        Text(
            text = "اضغط على أي جزيرة لتبدأ التعلّم واللعب وجمع النجوم",
            fontFamily = ReadingTajawalFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ReadingColors.InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp)
        )

        // خريطة جزر المستوى على مسار متعرج منقط متوهج
        AdventureIslandsPath(
            lessons = lessons,
            progress = progress,
            onSelectLesson = onSelectLesson
        )

        Spacer(modifier = Modifier.height(34.dp))
    }
}

@Composable
private fun NewQiraatiMapEntryBanner(
    isGradeOnePriority: Boolean,
    onOpenNewQiraati: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        shadowElevation = if (isGradeOnePriority) 9.dp else 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = if (isGradeOnePriority) 3.dp else 2.dp,
                color = ReadingColors.SunYellow,
                shape = RoundedCornerShape(26.dp)
            )
            .testTag("reading_map_new_qiraati_entry_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF7A59),
                            Color(0xFFF59E0B),
                            Color(0xFF0EA5E9)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QiraatiBookIconCanvas(modifier = Modifier.size(66.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ReadingColors.SunYellow
                            ) {
                                Text(
                                    text = if (isGradeOnePriority) "موصى به لصفك" else "متاح للمعلم وولي الأمر",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = ReadingColors.InkDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "كتاب قراءتي الجديدة • الصف الأول الابتدائي",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.5.sp,
                            lineHeight = 23.sp,
                            color = Color.White
                        )
                        Text(
                            text = "وحدات الكتاب الرسمي بألعاب البازل والتحليل والمحادثة",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ReadingBlueBirdCanvas(
                        bannerText = "أقرأ",
                        modifier = Modifier.height(52.dp)
                    )

                    PlayfulBouncyButton(
                        onClick = onOpenNewQiraati,
                        backgroundColor = Color.White,
                        shadowColor = Color(0xFFCBD5E1),
                        cornerRadius = 18.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        testTagValue = "reading_map_open_new_qiraati_btn"
                    ) {
                        Text(
                            text = "افتح الكتاب",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFFD14B2B),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingLevelSwitcherBar(
    activeLevel: ReadingLevel,
    defaultStudentLevel: ReadingLevel,
    onSelectLevel: (ReadingLevel) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .border(1.5.dp, ReadingColors.SkyBlue.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "اختر المرحلة القرائية للمراجعة أو التحدّي:",
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = ReadingColors.InkDark
                )
                if (activeLevel != defaultStudentLevel) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ReadingColors.SkyBackground
                    ) {
                        Text(
                            text = "وضع المراجعة",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = ReadingColors.SkyBlueShadow,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReadingLevel.entries.forEach { level ->
                    val selected = level == activeLevel
                    PlayfulBouncyButton(
                        onClick = { onSelectLevel(level) },
                        backgroundColor = if (selected) level.accentColor else Color(0xFFF8FAFC),
                        shadowColor = if (selected) level.shadowColor else Color(0xFFCBD5E1),
                        cornerRadius = 16.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        testTagValue = "reading_level_switch_${level.name.lowercase()}"
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = level.title,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = if (selected) Color.White else ReadingColors.InkDark,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = level.shortGradesLabel,
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (selected) Color.White.copy(alpha = 0.94f) else ReadingColors.InkSoft,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingMapHeroHeader(
    studentName: String,
    activeLevel: ReadingLevel,
    studentGradeLabel: String,
    isShowingStudentDefault: Boolean,
    totalStars: Int,
    avatarId: String,
    isSpeaking: Boolean,
    onGreetTap: () -> Unit,
    onOpenAvatarPicker: () -> Unit,
    onOpenLeaderboard: () -> Unit,
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

    val levelBadgeText = if (isShowingStudentDefault) {
        "${activeLevel.title} • $studentGradeLabel"
    } else {
        "${activeLevel.title} • ${activeLevel.fullGradesLabel}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(312.dp)
    ) {
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

            // شمس مبتسمة بأشعة دوّارة وهالة دافئة
            val sunCenter = Offset(w * 0.16f, h * 0.25f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF7B2).copy(alpha = 0.65f), Color.Transparent),
                    center = sunCenter,
                    radius = 62f
                ),
                radius = 62f,
                center = sunCenter
            )
            rotate(degrees = sunRotation, pivot = sunCenter) {
                for (i in 0 until 10) {
                    val angle = i * (2.0 * PI / 10.0)
                    val start = Offset(
                        sunCenter.x + (32f * cos(angle)).toFloat(),
                        sunCenter.y + (32f * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        sunCenter.x + (47f * cos(angle)).toFloat(),
                        sunCenter.y + (47f * sin(angle)).toFloat()
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
            drawCircle(color = ReadingColors.InkDark, radius = 2.6f, center = Offset(sunCenter.x - 8f, sunCenter.y - 4f))
            drawCircle(color = ReadingColors.InkDark, radius = 2.6f, center = Offset(sunCenter.x + 8f, sunCenter.y - 4f))
            val sunSmile = Path().apply {
                moveTo(sunCenter.x - 8f, sunCenter.y + 5f)
                quadraticBezierTo(sunCenter.x, sunCenter.y + 13f, sunCenter.x + 8f, sunCenter.y + 5f)
            }
            drawPath(sunSmile, color = ReadingColors.CoralOrange, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // غيمتان بيضاوان تتحركان بنعومة
            val c1x = w * 0.56f + cloudDrift
            val c1y = h * 0.20f
            drawCircle(color = Color.White.copy(alpha = 0.88f), radius = 19f, center = Offset(c1x, c1y))
            drawCircle(color = Color.White.copy(alpha = 0.88f), radius = 24f, center = Offset(c1x + 20f, c1y - 6f))
            drawCircle(color = Color.White.copy(alpha = 0.88f), radius = 18f, center = Offset(c1x + 42f, c1y))

            val c2x = w * 0.78f - cloudDrift * 0.7f
            val c2y = h * 0.34f
            drawCircle(color = Color.White.copy(alpha = 0.78f), radius = 15f, center = Offset(c2x, c2y))
            drawCircle(color = Color.White.copy(alpha = 0.78f), radius = 20f, center = Offset(c2x + 16f, c2y - 4f))
            drawCircle(color = Color.White.copy(alpha = 0.78f), radius = 14f, center = Offset(c2x + 32f, c2y))

            // التلال الخضراء المنحنية
            val backHill = Path().apply {
                moveTo(0f, h * 0.79f)
                cubicTo(w * 0.25f, h * 0.60f, w * 0.65f, h * 0.64f, w, h * 0.75f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(backHill, color = ReadingColors.HillLightGreen)

            val frontHill = Path().apply {
                moveTo(0f, h * 0.87f)
                cubicTo(w * 0.32f, h * 0.74f, w * 0.72f, h * 0.94f, w, h * 0.81f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                frontHill,
                brush = Brush.verticalGradient(
                    colors = listOf(ReadingColors.HillDeepGreen, ReadingColors.SkyBackground),
                    startY = h * 0.76f,
                    endY = h
                )
            )
        }

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
                        PlayfulBackArrowIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(
                            text = "رجوع",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
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
                        testTagValue = "reading_map_leaderboard_btn"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            PlayfulTrophyIcon(modifier = Modifier.size(22.dp), animated = true)
                            Text(
                                text = "أبطال الصف",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = ReadingColors.InkDark,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier.testTag("reading_map_stars_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            PlayfulStarIcon(filled = true, modifier = Modifier.size(22.dp))
                            Text(
                                text = "$totalStars",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ReadingColors.InkDark
                            )
                        }
                    }
                }
            }

            // شارة المستوى والصف الدراسي المسجّل للطالب
            Surface(
                shape = RoundedCornerShape(50),
                color = activeLevel.accentColor,
                shadowElevation = 5.dp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .border(2.dp, Color.White, RoundedCornerShape(50))
                    .testTag("reading_map_level_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayfulCrownIcon(modifier = Modifier.size(18.dp))
                    Text(
                        text = levelBadgeText,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // بطاقة ترحيب شخصية الطالب الرمزية القابلة للنقر لتغيير الشخصية
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenAvatarPicker() }
                        .testTag("reading_map_avatar_badge")
                ) {
                    ReadingAvatarCanvas(
                        avatarId = avatarId,
                        size = 86.dp,
                        animated = true
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ReadingColors.SunYellow,
                        shadowElevation = 3.dp
                    ) {
                        Text(
                            text = "غيّر شخصيتي",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp,
                            color = ReadingColors.InkDark,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .weight(1f)
                        .border(2.dp, Color.White, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "مرحباً يا $studentName!",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = ReadingColors.InkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "أنا صديقك فصيح في واحة القراءة الذكية، هيا نجمع النجوم معاً!",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = ReadingColors.InkSoft,
                            lineHeight = 18.sp
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
        }
    }
}

@Composable
private fun AdventureIslandsPath(
    lessons: List<ReadingLesson>,
    progress: ReadingProgressState,
    onSelectLesson: (ReadingLesson) -> Unit
) {
    val xFractions = listOf(0.50f, 0.24f, 0.74f, 0.25f, 0.75f, 0.24f, 0.74f, 0.50f)
    val rowSpacingDp = 168.dp
    val islandDiameterDp = 128.dp
    val topPaddingDp = 18.dp
    val totalHeightDp = topPaddingDp + rowSpacingDp * lessons.size + 30.dp

    val infinite = rememberInfiniteTransition(label = "island_glow_pulse")
    val glowPulse by infinite.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeightDp)
            .padding(horizontal = 12.dp)
    ) {
        val mapWidth = maxWidth

        // مسار متعرج منقط متوهج يربط الجزر
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
                // هالة مضيئة ناعمة تحت المسار
                drawPath(
                    path = path,
                    color = ReadingColors.SunYellow.copy(alpha = 0.20f),
                    style = Stroke(width = 34f, cap = StrokeCap.Round)
                )
                // طبقة المسار البيضاء الناعمة
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.92f),
                    style = Stroke(width = 22f, cap = StrokeCap.Round)
                )
                // الخط الذهبي المنقط المتلألئ
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

        lessons.forEachIndexed { index, lesson ->
            val frac = xFractions[index % xFractions.size]
            val cardWidth = 166.dp
            val xOffset = (mapWidth * frac - cardWidth / 2f).coerceIn(0.dp, (mapWidth - cardWidth).coerceAtLeast(0.dp))
            val yOffset = topPaddingDp + rowSpacingDp * index
            val starsEarned = progress.starsForLesson(lesson.id)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(cardWidth)
                    .offset(x = xOffset, y = yOffset)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(134.dp)
                ) {
                    // هالة توهج حي ناعمة حول كل جزيرة
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension * 0.48f * glowPulse
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    lesson.islandColor.copy(alpha = 0.34f),
                                    lesson.islandColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.5f),
                                radius = radius
                            ),
                            radius = radius
                        )
                    }

                    PlayfulBouncyButton(
                        onClick = { onSelectLesson(lesson) },
                        backgroundColor = lesson.islandColor,
                        shadowColor = lesson.islandShadowColor,
                        cornerRadius = 62.dp,
                        lipDepth = 6.dp,
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier.size(120.dp),
                        testTagValue = "reading_lesson_island_${lesson.id}"
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // حلقة زجاجية ناعمة وحافة فاخرة داخل الجزيرة
                            Box(
                                modifier = Modifier
                                    .size(102.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                LessonMiniIllustrationCanvas(
                                    artType = lesson.artType,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = lesson.letter,
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = when {
                                        lesson.letter.length >= 4 -> 18.sp
                                        lesson.letter.length > 2 -> 22.sp
                                        else -> 32.sp
                                    },
                                    color = Color.White,
                                    lineHeight = 34.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    modifier = Modifier.border(
                        width = 1.5.dp,
                        color = lesson.islandColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(18.dp)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${lesson.number}. ${lesson.title}",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = ReadingColors.InkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
