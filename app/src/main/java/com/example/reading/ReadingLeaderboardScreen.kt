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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

private sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Ready(val entries: List<ReadingLeaderboardEntry>) : LeaderboardUiState
    data class OfflineError(val message: String) : LeaderboardUiState
}

/**
 * شاشة ساحة المنافسة "أبطال الصف" بروح ألعاب الأونلاين داخل واحة القراءة الذكية:
 * تعرض منصة التتويج للأوائل الثلاثة ثم قائمة كاملة لجميع طلبة الصف من المركز 4 حتى آخر طالب،
 * مع شريط مثبت في الأسفل لـ "موقعي في الصف" وإمكانية تغيير الشخصية الرمزية في أي وقت.
 */
@Composable
fun ReadingLeaderboardScreen(
    schoolCode: String,
    currentStudentId: String,
    currentStudentName: String,
    currentStudentGrade: String,
    progress: ReadingProgressState,
    engine: ReadingAudioAndProgressEngine,
    isStudentParticipant: Boolean = true,
    validStudentIds: Set<String> = emptySet(),
    excludedStaffIds: Set<String> = emptySet(),
    excludedStaffNames: Set<String> = emptySet(),
    onOpenAvatarPicker: () -> Unit,
    onBack: () -> Unit
) {
    val defaultGrade = remember(currentStudentGrade) {
        ReadingAudioAndProgressEngine.normalizePrimaryGrade(currentStudentGrade)
    }
    var selectedGrade by remember(defaultGrade) { mutableStateOf(defaultGrade) }
    var refreshNonce by remember { mutableIntStateOf(0) }
    var uiState by remember { mutableStateOf<LeaderboardUiState>(LeaderboardUiState.Loading) }

    // جلب قائمة أبطال الصف (للطلبة الفعليين فقط مع استبعاد المدير والكادر تلقائياً)
    LaunchedEffect(schoolCode, selectedGrade, refreshNonce, validStudentIds, excludedStaffIds) {
        uiState = LeaderboardUiState.Loading
        uiState = when (
            val result = ReadingAudioAndProgressEngine.fetchGradeLeaderboard(
                schoolCode = schoolCode,
                targetGrade = selectedGrade,
                validStudentIds = validStudentIds,
                excludedStaffIds = excludedStaffIds,
                excludedStaffNames = excludedStaffNames
            )
        ) {
            is LeaderboardFetchResult.Success -> LeaderboardUiState.Ready(result.entries)
            is LeaderboardFetchResult.Error -> LeaderboardUiState.OfflineError(result.message)
        }
    }

    // تحديث فوري لشخصية الطالب ونجومه داخل القائمة المعروضة فقط إذا كان الحساب طالباً فعلياً في نفس صفه
    val displayedEntries = remember(
        uiState,
        progress.avatarId,
        progress.totalStars,
        selectedGrade,
        defaultGrade,
        isStudentParticipant,
        currentStudentId
    ) {
        val ready = uiState as? LeaderboardUiState.Ready ?: return@remember emptyList()
        if (!isStudentParticipant || selectedGrade != defaultGrade) {
            ready.entries
        } else {
            val updated = ready.entries.map { entry ->
                if (entry.studentId == currentStudentId) {
                    val mergedStars = maxOf(entry.totalStars, progress.totalStars)
                    val rankInfo = resolveReadingHeroTitle(mergedStars)
                    entry.copy(
                        avatarId = progress.avatarId,
                        totalStars = mergedStars,
                        completedLessons = maxOf(entry.completedLessons, progress.completedLessonsCount),
                        level = rankInfo.level,
                        heroTitle = rankInfo.title,
                        nextLevelStars = rankInfo.nextLevelStars
                    )
                } else {
                    entry
                }
            }
            updated
                .sortedWith(
                    compareByDescending<ReadingLeaderboardEntry> { it.totalStars }
                        .thenByDescending { it.completedLessons }
                        .thenBy { it.studentName }
                )
                .mapIndexed { idx, item -> item.copy(rank = idx + 1) }
        }
    }

    val effectiveHighlightStudentId = if (isStudentParticipant) currentStudentId else ""
    val myEntryInGrade = remember(displayedEntries, effectiveHighlightStudentId) {
        if (effectiveHighlightStudentId.isBlank()) null
        else displayedEntries.firstOrNull { it.studentId == effectiveHighlightStudentId }
    }
    val entryDirectlyAhead = remember(displayedEntries, myEntryInGrade) {
        val me = myEntryInGrade ?: return@remember null
        if (me.rank <= 1) null else displayedEntries.getOrNull(me.rank - 2)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF7DD3FC),
                            Color(0xFFE0F2FE),
                            Color(0xFFF8FAFC)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1) الترويسة الاحتفالية الطفولية + زر شخصيتي (للطلبة) + زر التحديث
                item(key = "leaderboard_header") {
                    LeaderboardHeroHeader(
                        selectedGrade = selectedGrade,
                        currentAvatarId = progress.avatarId,
                        showAvatarButton = isStudentParticipant,
                        onBack = onBack,
                        onRefresh = {
                            engine.playTapPop()
                            refreshNonce++
                        },
                        onOpenAvatarPicker = {
                            engine.playTapPop()
                            onOpenAvatarPicker()
                        }
                    )
                }

                // 2) شريط اختيار الصف الدراسي (من الأول حتى السادس الابتدائي)
                item(key = "grade_selector") {
                    GradeLeagueSelectorBar(
                        selectedGrade = selectedGrade,
                        studentDefaultGrade = if (isStudentParticipant) defaultGrade else "",
                        onSelectGrade = { grade ->
                            engine.playTapPop()
                            selectedGrade = grade
                        }
                    )
                }

                // 3) حالات التحميل / الخطأ / القائمة الفارغة / منصة التتويج وبقية الطلبة
                when (val state = uiState) {
                    LeaderboardUiState.Loading -> {
                        item(key = "loading_card") {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = ReadingColors.SkyBlue,
                                        strokeWidth = 4.dp,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "جاري تحميل ساحة أبطال $selectedGrade...",
                                        fontFamily = ReadingTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = ReadingColors.InkDark
                                    )
                                }
                            }
                        }
                    }

                    is LeaderboardUiState.OfflineError -> {
                        item(key = "offline_card") {
                            LeaderboardEmptyOrErrorCard(
                                title = "لا يوجد اتصال بساحة الأبطال",
                                message = state.message,
                                actionLabel = "إعادة المحاولة",
                                onAction = {
                                    engine.playTapPop()
                                    refreshNonce++
                                }
                            )
                        }
                    }

                    is LeaderboardUiState.Ready -> {
                        if (displayedEntries.isEmpty()) {
                            item(key = "empty_card") {
                                LeaderboardEmptyOrErrorCard(
                                    title = "ساحة $selectedGrade بانتظار أول بطل!",
                                    message = "ابدأ اللعب واجمع نجماتك الأولى في واحة القراءة لتتصدر منصة التتويج الذهبية!",
                                    actionLabel = "تحديث القائمة",
                                    onAction = {
                                        engine.playTapPop()
                                        refreshNonce++
                                    }
                                )
                            }
                        } else {
                            // منصة التتويج للأوائل الثلاثة (1 في المنتصف، 2 يميناً، 3 يساراً)
                            item(key = "top_three_podium") {
                                ChampionsPodiumSection(
                                    first = displayedEntries.getOrNull(0),
                                    second = displayedEntries.getOrNull(1),
                                    third = displayedEntries.getOrNull(2),
                                    currentStudentId = effectiveHighlightStudentId
                                )
                            }

                            // قسم بقية طلبة الصف (من المركز 4 حتى آخر طالب)
                            val restEntries = displayedEntries.drop(3)
                            if (restEntries.isNotEmpty()) {
                                item(key = "rest_section_header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "ترتيب بقية أبطال الصف",
                                            fontFamily = ReadingTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = ReadingColors.InkDark
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.White
                                        ) {
                                            Text(
                                                text = "إجمالي ${displayedEntries.size} بطل",
                                                fontFamily = ReadingTajawalFont,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = ReadingColors.SkyBlueShadow,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                items(
                                    items = restEntries,
                                    key = { entry -> "${entry.rank}_${entry.studentId}" }
                                ) { entry ->
                                    LeaderboardStudentRowCard(
                                        entry = entry,
                                        isCurrentStudent = effectiveHighlightStudentId.isNotBlank() &&
                                            entry.studentId == effectiveHighlightStudentId
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4) شريط "موقعي في الصف" للطلبة أو شريط المتابعة الإشرافية للمدير والكادر
            MyRankStickyBottomBar(
                selectedGrade = selectedGrade,
                isStudentParticipant = isStudentParticipant,
                totalStudentsInGrade = displayedEntries.size,
                isViewingOwnGrade = isStudentParticipant && selectedGrade == defaultGrade,
                studentName = currentStudentName,
                avatarId = progress.avatarId,
                localStars = progress.totalStars,
                myEntry = myEntryInGrade,
                aheadEntry = entryDirectlyAhead,
                onOpenAvatarPicker = {
                    engine.playTapPop()
                    onOpenAvatarPicker()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LeaderboardHeroHeader(
    selectedGrade: String,
    currentAvatarId: String,
    showAvatarButton: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAvatarPicker: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "leaderboard_sky")
    val twinkle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stars_twinkle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0284C7),
                        Color(0xFF0EA5E9),
                        Color(0xFF38BDF8)
                    )
                )
            )
    ) {
        // نجوم متلألئة في الخلفية السماوية
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val starCoords = listOf(
                Offset(w * 0.12f, h * 0.28f),
                Offset(w * 0.28f, h * 0.72f),
                Offset(w * 0.50f, h * 0.20f),
                Offset(w * 0.74f, h * 0.78f),
                Offset(w * 0.88f, h * 0.30f)
            )
            starCoords.forEachIndexed { idx, pos ->
                val alpha = (0.35f + 0.55f * ((sin(twinkle + idx * 1.1f) + 1f) * 0.5f))
                drawCircle(
                    color = Color(0xFFFFF7B2).copy(alpha = alpha),
                    radius = 5.5f + (idx % 2) * 3f,
                    center = pos
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    testTagValue = "leaderboard_back_btn"
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PlayfulBackArrowIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            text = "رجوع",
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showAvatarButton) {
                        // زر "شخصيتي" يعرض شخصية الطالب الرمزية ويفتح نافذة الاختيار
                        PlayfulBouncyButton(
                            onClick = onOpenAvatarPicker,
                            backgroundColor = Color.White,
                            shadowColor = Color(0xFFCBD5E1),
                            cornerRadius = 20.dp,
                            lipDepth = 4.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            testTagValue = "leaderboard_my_avatar_btn"
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ReadingAvatarCanvas(
                                    avatarId = currentAvatarId,
                                    size = 30.dp,
                                    animated = false
                                )
                                Text(
                                    text = "شخصيتي",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = ReadingColors.InkDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // زر التحديث الفوري
                    PlayfulBouncyButton(
                        onClick = onRefresh,
                        backgroundColor = ReadingColors.SunYellow,
                        shadowColor = ReadingColors.SunShadow,
                        cornerRadius = 18.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier.size(48.dp),
                        testTagValue = "leaderboard_refresh_btn"
                    ) {
                        PlayfulRefreshIcon(tint = ReadingColors.InkDark, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                PlayfulTrophyIcon(modifier = Modifier.size(44.dp), animated = true)
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "ساحة أبطال الصف",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                    Text(
                        text = "دوري نجوم القراءة • $selectedGrade",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.94f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeLeagueSelectorBar(
    selectedGrade: String,
    studentDefaultGrade: String,
    onSelectGrade: (String) -> Unit
) {
    val grades = ReadingAudioAndProgressEngine.PRIMARY_GRADES_LIST
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grades.forEachIndexed { idx, grade ->
            val selected = grade == selectedGrade
            val isMine = grade == studentDefaultGrade
            PlayfulBouncyButton(
                onClick = { onSelectGrade(grade) },
                backgroundColor = if (selected) ReadingColors.BerryPurple else Color.White,
                shadowColor = if (selected) ReadingColors.BerryShadow else Color(0xFFCBD5E1),
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                testTagValue = "leaderboard_grade_tab_$idx"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = grade.replace(" الابتدائي", ""),
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (selected) Color.White else ReadingColors.InkDark,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (isMine) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) ReadingColors.SunYellow else ReadingColors.SkyBackground
                        ) {
                            Text(
                                text = "صفّي",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                color = ReadingColors.InkDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * منصة التتويج للأوائل الثلاثة:
 * المركز الأول في المنتصف على منصة ذهبية مرتفعة مع تاج يطفو ويلمع،
 * المركز الثاني يميناً (أول عنصر في صف RTL) على منصة فضية،
 * المركز الثالث يساراً (آخر عنصر في صف RTL) على منصة برونزية.
 */
@Composable
private fun ChampionsPodiumSection(
    first: ReadingLeaderboardEntry?,
    second: ReadingLeaderboardEntry?,
    third: ReadingLeaderboardEntry?,
    currentStudentId: String
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .border(2.dp, ReadingColors.SunYellow.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, start = 10.dp, end = 10.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "منصة أبطال الصدارة",
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = ReadingColors.InkDark
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // يميناً في RTL: المركز الثاني (فضية)
                PodiumColumnSlot(
                    rank = 2,
                    entry = second,
                    isCurrentStudent = second?.studentId == currentStudentId,
                    podiumHeight = 96.dp,
                    avatarSize = 66.dp,
                    podiumColors = listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF94A3B8)),
                    badgeBg = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )

                // المنتصف: المركز الأول (ذهبية مرتفعة + تاج يطفو)
                PodiumColumnSlot(
                    rank = 1,
                    entry = first,
                    isCurrentStudent = first?.studentId == currentStudentId,
                    podiumHeight = 128.dp,
                    avatarSize = 84.dp,
                    podiumColors = listOf(Color(0xFFFFF7B2), ReadingColors.SunYellow, Color(0xFFD97706)),
                    badgeBg = Color(0xFFB45309),
                    isChampion = true,
                    modifier = Modifier.weight(1.15f)
                )

                // يساراً في RTL: المركز الثالث (برونزية)
                PodiumColumnSlot(
                    rank = 3,
                    entry = third,
                    isCurrentStudent = third?.studentId == currentStudentId,
                    podiumHeight = 78.dp,
                    avatarSize = 62.dp,
                    podiumColors = listOf(Color(0xFFFED7AA), Color(0xFFEA580C), Color(0xFF9A3412)),
                    badgeBg = Color(0xFF9A3412),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PodiumColumnSlot(
    rank: Int,
    entry: ReadingLeaderboardEntry?,
    isCurrentStudent: Boolean,
    podiumHeight: Dp,
    avatarSize: Dp,
    podiumColors: List<Color>,
    badgeBg: Color,
    isChampion: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (isChampion && entry != null) {
            FloatingGoldenCrownCanvas(modifier = Modifier.size(width = 48.dp, height = 34.dp))
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (entry != null) {
            Box(contentAlignment = Alignment.BottomCenter) {
                ReadingAvatarCanvas(
                    avatarId = entry.avatarId,
                    size = avatarSize,
                    animated = true
                )
                if (isCurrentStudent) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ReadingColors.SunYellow,
                        shadowElevation = 3.dp,
                        modifier = Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50))
                    ) {
                        Text(
                            text = "أنت",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = ReadingColors.InkDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.studentName,
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isChampion) 14.sp else 12.5.sp,
                color = ReadingColors.InkDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = entry.heroTitle,
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ReadingColors.InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            // مكان شاغر برسالة تشجيعية لطيفة
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .border(2.dp, Color(0xFFCBD5E1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                PlayfulStarIcon(filled = false, modifier = Modifier.size(avatarSize * 0.45f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "مكان بانتظار بطل!",
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ReadingColors.InkSoft,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // جسم المنصة المتدرّج
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(Brush.verticalGradient(podiumColors))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(vertical = 8.dp, horizontal = 4.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isChampion) 34.dp else 28.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isChampion) 17.sp else 14.sp,
                        color = Color.White
                    )
                }

                if (entry != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.90f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            PlayfulStarIcon(filled = true, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${entry.totalStars}",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = ReadingColors.InkDark
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * بطاقة طالب واحد في قائمة بقية أبطال الصف (من المركز 4 حتى آخر طالب).
 */
@Composable
private fun LeaderboardStudentRowCard(
    entry: ReadingLeaderboardEntry,
    isCurrentStudent: Boolean
) {
    val rankInfo = resolveReadingHeroTitle(entry.totalStars)
    val levelProgress = if (rankInfo.nextLevelStars == null) {
        1f
    } else {
        val span = (rankInfo.nextLevelStars - rankInfo.minStars).coerceAtLeast(1)
        ((entry.totalStars - rankInfo.minStars).toFloat() / span.toFloat()).coerceIn(0.08f, 1f)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isCurrentStudent) Color(0xFFFFFBEB) else Color.White,
        shadowElevation = if (isCurrentStudent) 7.dp else 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .border(
                width = if (isCurrentStudent) 2.5.dp else 1.2.dp,
                color = if (isCurrentStudent) ReadingColors.SunYellow else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("leaderboard_row_rank_${entry.rank}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1) شارة رقم التسلسل الحقيقي
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentStudent) ReadingColors.SunYellow else ReadingColors.SkyBackground
                    )
                    .border(
                        1.5.dp,
                        if (isCurrentStudent) ReadingColors.SunShadow else ReadingColors.SkyBlue.copy(alpha = 0.45f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = ReadingColors.InkDark
                )
            }

            // 2) الشخصية الرمزية المختارة
            ReadingAvatarCanvas(
                avatarId = entry.avatarId,
                size = 52.dp,
                animated = isCurrentStudent
            )

            // 3 + 4) اسم الطالب ولقبه وشريط التقدم نحو المستوى القادم
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.studentName,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ReadingColors.InkDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrentStudent) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ReadingColors.SunYellow
                        ) {
                            Text(
                                text = "أنت",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = ReadingColors.InkDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${entry.heroTitle} • المستوى ${entry.level}",
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ReadingColors.InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(levelProgress)
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ReadingColors.SkyBlue, ReadingColors.MeadowGreen)
                                )
                            )
                    )
                }
            }

            // 5) عداد النجمات الذهبية
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFFBEB),
                modifier = Modifier.border(1.5.dp, ReadingColors.SunYellow, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlayfulStarIcon(filled = true, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${entry.totalStars}",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ReadingColors.InkDark
                    )
                }
            }
        }
    }
}

private fun formatArabicStarsCount(count: Int): String = when (count) {
    1 -> "نجمة واحدة"
    2 -> "نجمتان"
    in 3..10 -> "$count نجمات"
    else -> "$count نجمة"
}

/**
 * شريط "موقعي في الصف" المثبت في أسفل الشاشة مع رسالة تحفيزية تحسب الفارق مع الزميل السابق مباشرة.
 */
@Composable
private fun MyRankStickyBottomBar(
    selectedGrade: String,
    isStudentParticipant: Boolean,
    totalStudentsInGrade: Int,
    isViewingOwnGrade: Boolean,
    studentName: String,
    avatarId: String,
    localStars: Int,
    myEntry: ReadingLeaderboardEntry?,
    aheadEntry: ReadingLeaderboardEntry?,
    onOpenAvatarPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveStars = maxOf(localStars, myEntry?.totalStars ?: 0)
    val rankInfo = resolveReadingHeroTitle(effectiveStars)

    val headlineText = when {
        !isStudentParticipant -> "متابعة ساحة أبطال $selectedGrade (حصرية للطلبة)"
        myEntry != null -> "موقعي في الصف: المركز ${myEntry.rank} • ${rankInfo.title}"
        isViewingOwnGrade -> "$studentName • ${rankInfo.title} ($effectiveStars نجمة)"
        else -> "أنت تستعرض الآن دوري $selectedGrade"
    }

    val motivationalText = when {
        !isStudentParticipant ->
            "حسابات الإدارة والكادر لا تشارك في المنافسة وتظهر هنا أسماء الطلبة الفعليين فقط."
        myEntry != null && myEntry.rank == 1 ->
            "أنت بطل الصف الأول الآن، حافظ على الصدارة!"
        myEntry != null && aheadEntry != null -> {
            val needed = (aheadEntry.totalStars - myEntry.totalStars + 1).coerceAtLeast(1)
            "أنت في المركز ${myEntry.rank} • باقي ${formatArabicStarsCount(needed)} لتصل للمركز ${myEntry.rank - 1}!"
        }
        isViewingOwnGrade ->
            "أكمل درساً جديداً واجمع نجماتك لتصعد في قائمة أبطال $selectedGrade!"
        else ->
            "اضغط على صفّك في الأعلى لمشاهدة موقعك الحقيقي بين أصدقائك!"
    }

    Surface(
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = Color.White,
        shadowElevation = 14.dp,
        modifier = modifier
            .border(
                width = 2.dp,
                color = if (isStudentParticipant) ReadingColors.SunYellow else ReadingColors.SkyBlue,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            )
            .testTag("leaderboard_my_rank_bottom_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isStudentParticipant) {
                Box(
                    modifier = Modifier
                        .clickable { onOpenAvatarPicker() }
                        .testTag("leaderboard_bottom_avatar_tap")
                ) {
                    ReadingAvatarCanvas(
                        avatarId = avatarId,
                        size = 54.dp,
                        animated = true
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ReadingColors.SkyBackground)
                        .border(1.5.dp, ReadingColors.SkyBlue.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    PlayfulTrophyIcon(modifier = Modifier.size(28.dp), animated = false)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = headlineText,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = ReadingColors.InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = motivationalText,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = ReadingColors.SkyBlueShadow
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isStudentParticipant) ReadingColors.SunYellow else ReadingColors.SkyBackground
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlayfulStarIcon(filled = true, modifier = Modifier.size(17.dp))
                    Text(
                        text = if (isStudentParticipant) "$effectiveStars" else "$totalStudentsInGrade بطل",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isStudentParticipant) 14.sp else 12.5.sp,
                        color = ReadingColors.InkDark
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEmptyOrErrorCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(2.dp, ReadingColors.SkyBlue.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayfulTrophyIcon(modifier = Modifier.size(66.dp), animated = true)
            Text(
                text = title,
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = ReadingColors.InkDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = ReadingColors.InkSoft,
                textAlign = TextAlign.Center
            )
            PlayfulBouncyButton(
                onClick = onAction,
                backgroundColor = ReadingColors.SkyBlue,
                shadowColor = ReadingColors.SkyBlueShadow,
                cornerRadius = 18.dp,
                lipDepth = 4.dp,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                testTagValue = "leaderboard_retry_btn"
            ) {
                Text(
                    text = actionLabel,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}
