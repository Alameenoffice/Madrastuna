package com.example.reading

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * الشاشة الرئيسية المنسّقة لقسم "واحة القراءة الذكية":
 * تقرأ صف الطالب المسجل من قاعدة البيانات لتحديد المستوى القرائي الأنسب تلقائياً،
 * وتتيح التبديل بين المستويات الثلاثة مع دعم كامل لزر الرجوع وتنظيف محرك الصوت.
 */
@Composable
fun ReadingOasisScreen(
    schoolCode: String,
    studentId: String,
    studentName: String,
    studentGrade: String = "",
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val effectiveSchoolCode = schoolCode.ifBlank { "default_school" }
    val effectiveStudentId = studentId.ifBlank { "guest_student" }
    val effectiveStudentName = studentName.ifBlank { "بطل القراءة" }

    val defaultStudentLevel = remember(studentGrade) {
        ReadingCurriculum.detectLevelFromGrade(studentGrade)
    }
    val studentGradeLabel = remember(studentGrade, defaultStudentLevel) {
        ReadingCurriculum.formatGradeDisplay(studentGrade, defaultStudentLevel)
    }

    var activeLevel by remember(defaultStudentLevel) { mutableStateOf(defaultStudentLevel) }
    var activeLesson by remember { mutableStateOf<ReadingLesson?>(null) }

    val engine = remember(effectiveSchoolCode, effectiveStudentId, effectiveStudentName) {
        ReadingAudioAndProgressEngine(
            context = context,
            schoolCode = effectiveSchoolCode,
            studentId = effectiveStudentId,
            studentName = effectiveStudentName
        )
    }

    DisposableEffect(engine) {
        onDispose {
            engine.release()
        }
    }

    val progress by engine.progress.collectAsState()
    val isSpeaking by engine.isSpeaking.collectAsState()

    BackHandler {
        if (activeLesson != null) {
            activeLesson = null
        } else {
            onClose()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ProvideTextStyle(value = TextStyle(fontFamily = ReadingTajawalFont)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ReadingColors.SkyBackground)
            ) {
                AnimatedContent(
                    targetState = activeLesson,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "reading_oasis_nav"
                ) { currentLesson ->
                    if (currentLesson == null) {
                        ReadingMapScreen(
                            studentName = effectiveStudentName,
                            studentGradeLabel = studentGradeLabel,
                            activeLevel = activeLevel,
                            defaultStudentLevel = defaultStudentLevel,
                            progress = progress,
                            isSpeaking = isSpeaking,
                            onSelectLevel = { chosenLevel ->
                                engine.playTapPop()
                                activeLevel = chosenLevel
                                engine.speak("${chosenLevel.title}: ${chosenLevel.subtitle}")
                            },
                            onSelectLesson = { chosen ->
                                engine.playTapPop()
                                activeLesson = chosen
                                engine.speak("درس ${chosen.title}، ${chosen.letterNameSpoken}")
                            },
                            onGreetTap = {
                                engine.playTapPop()
                                engine.speak(
                                    "مرحباً يا $effectiveStudentName! أهلاً بك في واحة القراءة الذكية، أنت الآن في ${activeLevel.title}، هيا نلعب ونتعلّم معاً"
                                )
                            },
                            onBack = {
                                engine.playTapPop()
                                onClose()
                            }
                        )
                    } else {
                        ReadingLessonScreen(
                            lesson = currentLesson,
                            progress = progress,
                            isSpeaking = isSpeaking,
                            engine = engine,
                            onBackToMap = { activeLesson = null }
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة دخول طفولية مبهجة تُعرض في الشاشة الرئيسية لولي الأمر والطالب،
 * وتبرز شخصية فصيح والمستوى القرائي وعدد النجوم المكتسبة في واحة القراءة الذكية.
 */
@Composable
fun ReadingOasisHomeEntryCard(
    schoolCode: String,
    studentId: String,
    studentName: String,
    studentGrade: String = "",
    onOpenOasis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val earnedStars = remember(schoolCode, studentId) {
        ReadingAudioAndProgressEngine.loadCachedTotalStars(
            context = context,
            schoolCode = schoolCode.ifBlank { "default_school" },
            studentId = studentId.ifBlank { "guest_student" }
        )
    }
    val detectedLevel = remember(studentGrade) {
        ReadingCurriculum.detectLevelFromGrade(studentGrade)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.Transparent,
            shadowElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .border(2.5.dp, ReadingColors.SunYellow, RoundedCornerShape(26.dp))
                .clickable { onOpenOasis() }
                .testTag("home_reading_oasis_entry_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0EA5E9),
                                Color(0xFF38BDF8),
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReadingMascotCanvas(
                        modifier = Modifier.size(76.dp),
                        isCelebrating = earnedStars > 0
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "واحة القراءة الذكية",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${detectedLevel.title} (${detectedLevel.subtitle}) — تعلّم القراءة والكتابة مع فصيح",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.94f),
                            lineHeight = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ReadingColors.SunYellow
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    PlayfulStarIcon(filled = true, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "$earnedStars نجمة",
                                        fontFamily = ReadingTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = ReadingColors.InkDark
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "ابدأ المغامرة",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
