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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.reading.qiraati.NewQiraatiNavHost

private sealed interface OasisViewMode {
    data object Map : OasisViewMode
    data object NewQiraatiBook : OasisViewMode
    data object Leaderboard : OasisViewMode
    data class Lesson(val lesson: ReadingLesson) : OasisViewMode
}

/**
 * الشاشة الرئيسية المنسّقة لقسم "واحة القراءة الذكية":
 * تقرأ صف الطالب المسجل من قاعدة البيانات لتحديد المستوى القرائي الأنسب تلقائياً،
 * وتتيح الدخول لكتاب قراءتي الجديدة وساحة أبطال الصف واختيار الشخصية الرمزية
 * والتبديل بين المستويات الثلاثة مع دعم كامل لزر الرجوع وتنظيف محرك الصوت.
 */
@Composable
fun ReadingOasisScreen(
    schoolCode: String,
    studentId: String,
    studentName: String,
    studentGrade: String = "",
    isStudentParticipant: Boolean = true,
    validStudentIds: Set<String> = emptySet(),
    excludedStaffIds: Set<String> = emptySet(),
    excludedStaffNames: Set<String> = emptySet(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    var viewMode by remember { mutableStateOf<OasisViewMode>(OasisViewMode.Map) }
    var modeBeforeLeaderboard by remember { mutableStateOf<OasisViewMode>(OasisViewMode.Map) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showSoundSettings by remember { mutableStateOf(false) }

    val engine = remember(
        effectiveSchoolCode,
        effectiveStudentId,
        effectiveStudentName,
        studentGradeLabel,
        isStudentParticipant
    ) {
        ReadingAudioAndProgressEngine(
            context = context,
            schoolCode = effectiveSchoolCode,
            studentId = effectiveStudentId,
            studentName = effectiveStudentName,
            studentGrade = studentGradeLabel,
            isStudentParticipant = isStudentParticipant
        )
    }

    DisposableEffect(engine, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> engine.pauseBackgroundAudio()
                Lifecycle.Event.ON_RESUME -> engine.resumeBackgroundAudio()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            engine.release()
        }
    }

    val progress by engine.progress.collectAsState()
    val isSpeaking by engine.isSpeaking.collectAsState()
    val soundSettings by engine.soundSettings.collectAsState()

    fun openLeaderboardFrom(origin: OasisViewMode) {
        engine.playTapPop()
        modeBeforeLeaderboard = origin
        viewMode = OasisViewMode.Leaderboard
    }

    fun openSoundSettingsDialog() {
        engine.playTapPop()
        showSoundSettings = true
    }

    BackHandler(enabled = showSoundSettings || showAvatarPicker || viewMode !is OasisViewMode.NewQiraatiBook) {
        when {
            showSoundSettings -> showSoundSettings = false
            showAvatarPicker -> showAvatarPicker = false
            viewMode is OasisViewMode.Leaderboard -> viewMode = modeBeforeLeaderboard
            viewMode is OasisViewMode.Lesson -> viewMode = OasisViewMode.Map
            viewMode is OasisViewMode.Map -> onClose()
            viewMode is OasisViewMode.NewQiraatiBook -> viewMode = OasisViewMode.Map
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
                    targetState = viewMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "reading_oasis_nav"
                ) { currentMode ->
                    when (currentMode) {
                        OasisViewMode.Map -> ReadingMapScreen(
                            studentName = effectiveStudentName,
                            studentGradeLabel = studentGradeLabel,
                            activeLevel = activeLevel,
                            defaultStudentLevel = defaultStudentLevel,
                            progress = progress,
                            isSpeaking = isSpeaking,
                            onSelectLevel = { chosenLevel ->
                                engine.playTapPop()
                                activeLevel = chosenLevel
                                engine.speakSequence(chosenLevel.title, chosenLevel.subtitle)
                            },
                            onSelectLesson = { chosen ->
                                engine.playTapPop()
                                viewMode = OasisViewMode.Lesson(chosen)
                                engine.speakSequence("درس", chosen.title, chosen.letterNameSpoken)
                            },
                            onGreetTap = {
                                engine.playTapPop()
                                engine.speakSequence(
                                    "أهلاً بك في واحة القراءة الذكية!",
                                    activeLevel.title,
                                    "هيا نلعب ونتعلّم معاً!"
                                )
                            },
                            onBack = {
                                engine.playTapPop()
                                onClose()
                            },
                            onOpenNewQiraati = {
                                engine.playTapPop()
                                engine.speak("أهلاً بك في كتاب قراءتي الجديدة للصف الأول الابتدائي")
                                viewMode = OasisViewMode.NewQiraatiBook
                            },
                            onOpenAvatarPicker = {
                                if (isStudentParticipant) {
                                    engine.playTapPop()
                                    showAvatarPicker = true
                                }
                            },
                            onOpenLeaderboard = {
                                openLeaderboardFrom(OasisViewMode.Map)
                            },
                            soundSettings = soundSettings,
                            onOpenSoundSettings = { openSoundSettingsDialog() },
                            onBirdTap = {
                                engine.playTapPop()
                                engine.speak("أقرأ")
                            },
                            isStudentParticipant = isStudentParticipant
                        )

                        OasisViewMode.NewQiraatiBook -> NewQiraatiNavHost(
                            studentName = effectiveStudentName,
                            progress = progress,
                            isSpeaking = isSpeaking,
                            engine = engine,
                            onBackToOasis = { viewMode = OasisViewMode.Map },
                            onOpenAvatarPicker = {
                                if (isStudentParticipant) {
                                    engine.playTapPop()
                                    showAvatarPicker = true
                                }
                            },
                            onOpenLeaderboard = {
                                openLeaderboardFrom(OasisViewMode.NewQiraatiBook)
                            },
                            soundSettings = soundSettings,
                            onOpenSoundSettings = { openSoundSettingsDialog() }
                        )

                        OasisViewMode.Leaderboard -> ReadingLeaderboardScreen(
                            schoolCode = effectiveSchoolCode,
                            currentStudentId = effectiveStudentId,
                            currentStudentName = effectiveStudentName,
                            currentStudentGrade = studentGradeLabel,
                            progress = progress,
                            engine = engine,
                            isStudentParticipant = isStudentParticipant,
                            validStudentIds = validStudentIds,
                            excludedStaffIds = excludedStaffIds,
                            excludedStaffNames = excludedStaffNames,
                            onOpenAvatarPicker = {
                                if (isStudentParticipant) {
                                    engine.playTapPop()
                                    showAvatarPicker = true
                                }
                            },
                            onBack = {
                                engine.playTapPop()
                                viewMode = modeBeforeLeaderboard
                            }
                        )

                        is OasisViewMode.Lesson -> ReadingLessonScreen(
                            lesson = currentMode.lesson,
                            progress = progress,
                            isSpeaking = isSpeaking,
                            engine = engine,
                            soundSettings = soundSettings,
                            onOpenSoundSettings = { openSoundSettingsDialog() },
                            onBackToMap = { viewMode = OasisViewMode.Map }
                        )
                    }
                }

                if (showAvatarPicker && isStudentParticipant) {
                    ReadingAvatarPickerDialog(
                        currentAvatarId = progress.avatarId,
                        onSelectAvatar = { chosenAvatarId ->
                            val avatar = ReadingAvatarCatalog.byId(chosenAvatarId)
                            engine.playSuccessChime()
                            engine.saveAndSyncAvatar(
                                schoolCode = effectiveSchoolCode,
                                studentId = effectiveStudentId,
                                studentName = effectiveStudentName,
                                grade = studentGradeLabel,
                                avatarId = avatar.id
                            )
                            engine.speakSequence("اختيار رائع يا بطل!", avatar.name)
                        },
                        onDismiss = {
                            engine.playTapPop()
                            showAvatarPicker = false
                        }
                    )
                }

                if (showSoundSettings) {
                    ReadingSoundSettingsDialog(
                        settings = soundSettings,
                        onToggleSfx = { enabled ->
                            engine.setSfxEnabled(enabled)
                            if (enabled) engine.playTapPop()
                        },
                        onToggleBgMusic = { enabled ->
                            engine.setBgMusicEnabled(enabled)
                            engine.playTapPop()
                        },
                        onVolumeChange = { volume ->
                            engine.setBgMusicVolume(volume)
                        },
                        onDismiss = {
                            engine.playTapPop()
                            showSoundSettings = false
                        }
                    )
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
