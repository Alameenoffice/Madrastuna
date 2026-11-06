package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CurriculumBookItem
import com.example.model.CurriculumGradeSection
import com.example.model.CurriculumStageTab
import com.example.model.SchoolStage
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * قسم "المناهج الدراسية" المتكامل لجميع المراحل الدراسية (الابتدائية، المتوسطة، والإعدادية)
 * مصمم بالكامل وفق سياسة حظر الأيقونات الكلاسيكية (Zero Classic Icons Policy)
 * مع الاعتماد الحصري على التصميم الزجاجي النقي (3D Glassmorphism) والنصوص الصافية الفخمة،
 * وعارض PDF مباشر داخل التطبيق (In-App Online PDF Viewer) بدون تنزيل أو شغل مساحة الهاتف.
 */
@Composable
fun CurriculumScreen(
    viewModel: SchoolViewModel
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val allBooks by viewModel.globalCurriculumBooks.collectAsState()

    val defaultInitialStage = remember(schoolConfig.schoolStage) {
        when (schoolConfig.schoolStage) {
            SchoolStage.PRIMARY -> CurriculumStageTab.PRIMARY
            SchoolStage.MIDDLE -> CurriculumStageTab.MIDDLE
            SchoolStage.PREPARATORY, SchoolStage.SECONDARY -> CurriculumStageTab.PREPARATORY
        }
    }

    var selectedStage by remember(defaultInitialStage) { mutableStateOf(defaultInitialStage) }
    var selectedGradeFilter by remember(selectedStage) { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var activeBookForViewer by remember { mutableStateOf<CurriculumBookItem?>(null) }

    val stageGrades = remember(selectedStage) { selectedStage.orderedGrades }

    val groupedGradeSections = remember(allBooks, selectedStage, selectedGradeFilter, searchQuery) {
        val cleanQuery = searchQuery.trim()
        val stageBooks = allBooks.filter { it.stage == selectedStage }
        val targetGrades = if (selectedGradeFilter == "الكل" || selectedGradeFilter !in stageGrades) {
            stageGrades
        } else {
            listOf(selectedGradeFilter)
        }

        targetGrades.mapNotNull { gradeName ->
            val booksForGrade = stageBooks
                .filter { it.gradeTitle.trim() == gradeName.trim() }
                .filter { book ->
                    cleanQuery.isBlank() ||
                        book.bookTitle.contains(cleanQuery, ignoreCase = true) ||
                        book.gradeTitle.contains(cleanQuery, ignoreCase = true)
                }
                .sortedBy { it.sortOrder }

            if (booksForGrade.isEmpty() && cleanQuery.isNotBlank()) {
                null
            } else {
                CurriculumGradeSection(
                    stage = selectedStage,
                    gradeTitle = gradeName,
                    books = booksForGrade
                )
            }
        }
    }

    val totalStageBooksCount = remember(allBooks, selectedStage) {
        allBooks.count { it.stage == selectedStage }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("curriculum_screen_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // 1. ترويسة زجاجية ثلاثية الأبعاد صافية بدون أي أيقونات كلاسيكية (Pure 3D Glassmorphism Hero Header)
        item(key = "curriculum_pure_glass_hero") {
            PureGlassCurriculumHeroHeader(
                totalBooksCount = allBooks.size,
                activeStage = selectedStage,
                activeStageBooksCount = totalStageBooksCount
            )
        }

        // 2. تبويبات المراحل الدراسية الثلاث بتصميم 3D Glassmorphism نقي
        item(key = "curriculum_stage_tabs") {
            PureGlassCurriculumStageTabsBar(
                selectedStage = selectedStage,
                isDarkMode = isDarkMode,
                onSelectStage = { newStage ->
                    selectedStage = newStage
                    selectedGradeFilter = "الكل"
                }
            )
        }

        // 3. شريط البحث الزجاجي وفلاتر الصفوف الدراسية للمرحلة المختارة
        item(key = "curriculum_grade_filters_and_search") {
            PureGlassGradeFilterAndSearchCard(
                stage = selectedStage,
                grades = stageGrades,
                selectedGrade = selectedGradeFilter,
                searchQuery = searchQuery,
                isDarkMode = isDarkMode,
                onSelectGrade = { selectedGradeFilter = it },
                onSearchQueryChange = { searchQuery = it }
            )
        }

        // 4. قوائم الصفوف والكتب المنهجية الرسمية الموحدة
        if (groupedGradeSections.isEmpty()) {
            item(key = "curriculum_empty_search_state") {
                PureGlassEmptyCurriculumState(
                    searchQuery = searchQuery,
                    onResetFilter = {
                        searchQuery = ""
                        selectedGradeFilter = "الكل"
                    }
                )
            }
        } else {
            items(groupedGradeSections, key = { "${it.stage.key}_${it.gradeTitle}" }) { gradeSection ->
                PureGlassGradeCurriculumCard(
                    gradeSection = gradeSection,
                    isDarkMode = isDarkMode,
                    onOpenBook = { book -> activeBookForViewer = book }
                )
            }
        }
    }

    // 5. عارض الكتب المنهجية المباشر داخل التطبيق بدون تنزيل (In-App Online PDF Viewer)
    activeBookForViewer?.let { targetBook ->
        InAppOnlineCurriculumPdfViewerDialog(
            book = targetBook,
            onDismiss = { activeBookForViewer = null }
        )
    }
}

@Composable
private fun PureGlassCurriculumHeroHeader(
    totalBooksCount: Int,
    activeStage: CurriculumStageTab,
    activeStageBooksCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF059669).copy(alpha = 0.28f),
                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.34f)
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color(0xFFA7F3D0).copy(alpha = 0.45f),
                    Color(0xFFFDE68A).copy(alpha = 0.40f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF044E3A),
                            Color(0xFF057A55),
                            Color(0xFF0F3D5E),
                            Color(0xFF1E293B)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Glass Capsules Row (Pure Typography)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.34f))
                    ) {
                        Text(
                            text = "المكتبة الأكاديمية الرسمية الموحدة",
                            color = Color(0xFFA7F3D0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFD97706).copy(alpha = 0.26f),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.50f))
                    ) {
                        Text(
                            text = "تصفح فوري مباشر • $totalBooksCount كتاب",
                            color = Color(0xFFFEF3C7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Main Luxury Title & Description
                Text(
                    text = "المناهج الدراسية",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "تصفح كافة الكتب المنهجية المعتمدة للمراحل الابتدائية والمتوسطة والإعدادية بشكل فوري ومباشر داخل المنصة دون الحاجة لتنزيل الملفات أو شغل مساحة الهاتف.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.90f),
                    lineHeight = 20.sp
                )

                // Active Stage Summary Strip
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = activeStage.arabicTitle,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = activeStage.description,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD1FAE5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "${activeStage.orderedGrades.size} صفوف • $activeStageBooksCount منهج",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
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
private fun PureGlassCurriculumStageTabsBar(
    selectedStage: CurriculumStageTab,
    isDarkMode: Boolean,
    onSelectStage: (CurriculumStageTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFF059669).copy(alpha = 0.16f),
                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.20f)
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f),
        border = BorderStroke(
            1.2.dp,
            if (isDarkMode) Color(0xFF334155) else Color(0xFFD1FAE5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "اختر المرحلة الدراسية:",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46),
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurriculumStageTab.entries.forEach { stageTab ->
                    val selected = selectedStage == stageTab
                    val activeGradient = when (stageTab) {
                        CurriculumStageTab.PRIMARY -> listOf(Color(0xFF059669), Color(0xFF047857))
                        CurriculumStageTab.MIDDLE -> listOf(Color(0xFF0F3D5E), Color(0xFF1E3A8A))
                        CurriculumStageTab.PREPARATORY -> listOf(Color(0xFF047857), Color(0xFF0F3D5E))
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectStage(stageTab) }
                            .testTag("curriculum_stage_tab_${stageTab.key}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) {
                            Color.Transparent
                        } else if (isDarkMode) {
                            Color(0xFF1E293B).copy(alpha = 0.75f)
                        } else {
                            Color(0xFFF1F5F9).copy(alpha = 0.85f)
                        },
                        border = BorderStroke(
                            width = if (selected) 1.4.dp else 1.dp,
                            color = if (selected) {
                                Color(0xFFA7F3D0)
                            } else if (isDarkMode) {
                                Color(0xFF334155)
                            } else {
                                Color(0xFFCBD5E1)
                            }
                        ),
                        shadowElevation = if (selected) 8.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (selected) {
                                        Modifier.background(Brush.linearGradient(activeGradient))
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = stageTab.arabicTitle,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) {
                                    Color.White
                                } else if (isDarkMode) {
                                    Color(0xFFE2E8F0)
                                } else {
                                    Color(0xFF0F172A)
                                },
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${stageTab.orderedGrades.size} صفوف رسمية",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) {
                                    Color(0xFFD1FAE5)
                                } else if (isDarkMode) {
                                    Color(0xFF94A3B8)
                                } else {
                                    Color(0xFF475569)
                                },
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PureGlassGradeFilterAndSearchCard(
    stage: CurriculumStageTab,
    grades: List<String>,
    selectedGrade: String,
    searchQuery: String,
    isDarkMode: Boolean,
    onSelectGrade: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFF059669).copy(alpha = 0.12f),
                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.16f)
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.94f),
        border = BorderStroke(
            1.2.dp,
            if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pure 3D Glassmorphic Search Field (Zero Classic Icons)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.2.dp,
                    if (searchQuery.isNotBlank()) Color(0xFF059669)
                    else if (isDarkMode) Color(0xFF334155)
                    else Color(0xFFCBD5E1)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "ابحث باسم الكتاب أو المادة في ${stage.arabicTitle}...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            ),
                            cursorBrush = SolidColor(Color(0xFF059669)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("curriculum_search_input")
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFDC2626).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSearchQueryChange("") }
                        ) {
                            Text(
                                text = "مسح البحث",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Grade Pills Row
            Text(
                text = "صفوف ${stage.arabicTitle}:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
            )

            val allFilterPills = remember(grades) { listOf("الكل") + grades }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(allFilterPills, key = { it }) { gradeOption ->
                    val isSelected = selectedGrade == gradeOption
                    val displayLabel = if (gradeOption == "الكل") "جميع صفوف المرحلة (${grades.size})" else gradeOption
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) {
                            Color.Transparent
                        } else if (isDarkMode) {
                            Color(0xFF1E293B)
                        } else {
                            Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            width = 1.1.dp,
                            color = if (isSelected) Color(0xFF10B981)
                            else if (isDarkMode) Color(0xFF334155)
                            else Color(0xFFCBD5E1)
                        ),
                        shadowElevation = if (isSelected) 5.dp else 0.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectGrade(gradeOption) }
                            .testTag("curriculum_grade_pill_$gradeOption")
                    ) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF059669), Color(0xFF0F3D5E))
                                            )
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayLabel,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) {
                                    Color.White
                                } else if (isDarkMode) {
                                    Color(0xFFE2E8F0)
                                } else {
                                    Color(0xFF1E293B)
                                },
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
private fun PureGlassGradeCurriculumCard(
    gradeSection: CurriculumGradeSection,
    isDarkMode: Boolean,
    onOpenBook: (CurriculumBookItem) -> Unit
) {
    val headerGradient = when (gradeSection.stage) {
        CurriculumStageTab.PRIMARY -> listOf(Color(0xFF065F46), Color(0xFF059669))
        CurriculumStageTab.MIDDLE -> listOf(Color(0xFF0F3D5E), Color(0xFF1E3A8A))
        CurriculumStageTab.PREPARATORY -> listOf(Color(0xFF047857), Color(0xFF0F3D5E))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF059669).copy(alpha = 0.16f),
                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.22f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.97f),
        border = BorderStroke(
            1.3.dp,
            if (isDarkMode) Color(0xFF334155) else Color(0xFFD1FAE5)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Grade Glass Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(headerGradient))
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = gradeSection.gradeTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "${gradeSection.stage.arabicTitle} • الصياغة الأكاديمية الرسمية الموحدة",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD1FAE5)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f))
                    ) {
                        Text(
                            text = "${gradeSection.books.size} كتب منهجية",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Books List inside Grade Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                gradeSection.books.forEachIndexed { idx, bookItem ->
                    PureGlassCurriculumBookRow(
                        orderNumber = idx + 1,
                        book = bookItem,
                        isDarkMode = isDarkMode,
                        onOpenBook = { onOpenBook(bookItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PureGlassCurriculumBookRow(
    orderNumber: Int,
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    onOpenBook: () -> Unit
) {
    val badgeCode = orderNumber.toString().padStart(2, '0')
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenBook() }
            .testTag("curriculum_book_card_${book.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = 1.1.dp,
            brush = Brush.horizontalGradient(
                colors = if (isDarkMode) {
                    listOf(Color(0xFF059669).copy(alpha = 0.55f), Color(0xFF334155))
                } else {
                    listOf(Color(0xFFA7F3D0), Color(0xFFBFDBFE))
                }
            )
        ),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pure 3D Glassmorphic Typographic Number Badge (No Classic Icons)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.70f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF059669), Color(0xFF0F3D5E))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCode,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = book.bookTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "${book.gradeTitle} • تصفح إلكتروني فوري مباشر",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Pure 3D Glassmorphic Action Capsule Button ("تصفح الكتاب الآن" — Zero Classic Icons)
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                border = BorderStroke(1.1.dp, Color(0xFFA7F3D0).copy(alpha = 0.75f)),
                shadowElevation = 5.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onOpenBook() }
                    .testTag("curriculum_open_book_btn_${book.id}")
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF059669), Color(0xFF047857), Color(0xFF0F3D5E))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تصفح الكتاب",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PureGlassEmptyCurriculumState(
    searchQuery: String,
    onResetFilter: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.94f),
        border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "لا توجد كتب مطابقة لعبارة البحث ($searchQuery)",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            Text(
                text = "يمكنك مسح البحث لاستعراض جميع الكتب المنهجية الرسمية المعتمدة للمرحلة.",
                fontSize = 12.sp,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF059669),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onResetFilter() }
            ) {
                Text(
                    text = "عرض كافة المناهج الدراسية",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                )
            }
        }
    }
}

/**
 * عارض المناهج الفوري المباشر داخل التطبيق (In-App Online PDF Viewer)
 * يتصفح ملفات PDF سحابياً مباشرة داخل التطبيق دون تنزيلها إلى ذاكرة الهاتف الدائمة،
 * مع دعم وضعين متكاملين:
 * 1) التصفح السحابي المباشر (Web Stream Reader)
 * 2) عارض الصفحات التفاعلي فائق الدقة (In-Memory Native Page Streamer)
 * مصمم بالكامل وفق سياسة Zero Classic Icons Policy (نصوص صافية وتصميم 3D Glassmorphism).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppOnlineCurriculumPdfViewerDialog(
    book: CurriculumBookItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // 0 = التصفح السحابي المباشر (Embedded Web Stream), 1 = عارض الصفحات التفاعلي (In-Memory Page Renderer)
    var viewerMode by remember { mutableIntStateOf(0) }
    var webLoadingProgress by remember { mutableIntStateOf(15) }
    var isWebLoading by remember { mutableStateOf(true) }

    // Native In-Memory Stream state (Mode 1)
    var isNativeStreaming by remember(book.pdfUrl, viewerMode) { mutableStateOf(false) }
    var nativeStreamProgress by remember(book.pdfUrl, viewerMode) { mutableIntStateOf(0) }
    var nativeStreamError by remember(book.pdfUrl, viewerMode) { mutableStateOf<String?>(null) }
    var pageCount by remember(book.pdfUrl) { mutableIntStateOf(0) }
    var currentPageIndex by remember(book.pdfUrl) { mutableIntStateOf(0) }
    var currentPageBitmap by remember(book.pdfUrl, currentPageIndex) { mutableStateOf<Bitmap?>(null) }
    var zoomScale by remember(currentPageIndex) { mutableFloatStateOf(1f) }
    var offsetX by remember(currentPageIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(currentPageIndex) { mutableFloatStateOf(0f) }

    var pdfRendererRef by remember { mutableStateOf<PdfRenderer?>(null) }
    var parcelFdRef by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var ephemeralTempFileRef by remember { mutableStateOf<File?>(null) }

    DisposableEffect(book.pdfUrl) {
        onDispose {
            runCatching { pdfRendererRef?.close() }
            runCatching { parcelFdRef?.close() }
            runCatching { ephemeralTempFileRef?.delete() }
        }
    }

    LaunchedEffect(book.pdfUrl, viewerMode) {
        if (viewerMode == 1 && pdfRendererRef == null && !isNativeStreaming) {
            isNativeStreaming = true
            nativeStreamError = null
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    val tempFile = File(context.cacheDir, "ephemeral_curriculum_stream_${book.id.hashCode()}.pdf")
                    ephemeralTempFileRef = tempFile
                    val url = URL(book.pdfUrl)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 25000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    }
                    val totalBytes = conn.contentLength.coerceAtLeast(1)
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var downloaded = 0L
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (totalBytes > 1) {
                                    nativeStreamProgress = ((downloaded * 100L) / totalBytes).toInt().coerceIn(1, 99)
                                }
                            }
                            output.flush()
                        }
                    }
                    val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    // Unlink ephemeral cache file handle immediately where supported or on close
                    parcelFdRef = pfd
                    pdfRendererRef = renderer
                    pageCount = renderer.pageCount
                    currentPageIndex = 0
                    isNativeStreaming = false
                } catch (e: Exception) {
                    isNativeStreaming = false
                    nativeStreamError = "تعذر البث المباشر للصفحات، يمكنك القراءة عبر وضع التصفح السحابي المباشر."
                } finally {
                    conn?.disconnect()
                }
            }
        }
    }

    LaunchedEffect(pdfRendererRef, currentPageIndex, pageCount) {
        val renderer = pdfRendererRef
        if (renderer != null && pageCount > 0 && currentPageIndex in 0 until pageCount) {
            withContext(Dispatchers.IO) {
                runCatching {
                    synchronized(renderer) {
                        renderer.openPage(currentPageIndex).use { page ->
                            val targetW = (page.width * 2.2f).toInt().coerceIn(720, 2048)
                            val targetH = (page.height * 2.2f).toInt().coerceIn(960, 2800)
                            val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            currentPageBitmap = bmp
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF042F2E),
                            Color(0xFF0F172A),
                            Color(0xFF020617)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("in_app_curriculum_pdf_viewer_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. شريط الترويسة الزجاجي العلوي للعارض الفوري (Zero Classic Icons)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFA7F3D0).copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.30f),
                                Color(0xFFFDE68A).copy(alpha = 0.45f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = book.bookTitle,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${book.gradeTitle} • ${book.stage.arabicTitle} • تصفح مباشر بدون تنزيل",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFA7F3D0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // زر إغلاق العارض الزجاجي النقي (بدون أيقونة X تقليدية)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFDC2626).copy(alpha = 0.28f),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onDismiss() }
                                    .testTag("curriculum_pdf_viewer_close_btn")
                            ) {
                                Text(
                                    text = "إغلاق العارض",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // أزرار التبديل بين التصفح السحابي المباشر وعارض الصفحات الفوري
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "التصفح السحابي المباشر",
                                1 to "عارض الصفحات التفاعلي"
                            ).forEach { (modeIdx, modeLabel) ->
                                val selected = viewerMode == modeIdx
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) Color(0xFF059669) else Color.White.copy(alpha = 0.10f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) Color(0xFFA7F3D0) else Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { viewerMode = modeIdx }
                                ) {
                                    Text(
                                        text = modeLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) Color.White else Color(0xFFE2E8F0),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. مساحة عرض الكتاب المباشر
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .border(1.2.dp, Color(0xFFA7F3D0).copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                ) {
                    if (viewerMode == 0) {
                        val encodedPdfUrl = remember(book.pdfUrl) {
                            URLEncoder.encode(book.pdfUrl.trim(), "UTF-8")
                        }
                        val onlineReaderUrl = remember(encodedPdfUrl) {
                            "https://docs.google.com/gview?embedded=true&url=$encodedPdfUrl"
                        }

                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    }
                                    // حظر تنزيل أي ملف إلى مساحة تخزين الهاتف نهائياً
                                    setDownloadListener { _, _, _, _, _ ->
                                        // Blocked intentionally: In-App Online Viewing Only
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            webLoadingProgress = newProgress
                                            isWebLoading = newProgress < 95
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val target = request?.url?.toString().orEmpty()
                                            if (target.endsWith(".pdf", ignoreCase = true)) {
                                                val enc = URLEncoder.encode(target, "UTF-8")
                                                view?.loadUrl("https://docs.google.com/gview?embedded=true&url=$enc")
                                                return true
                                            }
                                            return false
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isWebLoading = false
                                        }
                                    }
                                    loadUrl(onlineReaderUrl)
                                }
                            },
                            update = { webView ->
                                if (webView.url == null) {
                                    webView.loadUrl(onlineReaderUrl)
                                }
                            }
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isWebLoading,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF044E3A).copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "جاري فتح صفحات المنهج الدراسي سحابياً...",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "$webLoadingProgress%",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFA7F3D0)
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (webLoadingProgress / 100f).coerceIn(0.08f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = Color(0xFF10B981),
                                        trackColor = Color.White.copy(alpha = 0.22f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Mode 1: Native In-Memory Streaming Page Renderer
                        when {
                            isNativeStreaming -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.2.dp, Color(0xFFA7F3D0).copy(alpha = 0.50f)),
                                        modifier = Modifier
                                            .fillMaxWidth(0.86f)
                                            .padding(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(22.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "جاري البث المباشر لصفحات الكتاب في الذاكرة المؤقتة",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = if (nativeStreamProgress > 0) {
                                                    "نسبة البث المباشر: $nativeStreamProgress% (دون حفظ في ذاكرة الهاتف)"
                                                } else {
                                                    "يتصل بالخادم الأكاديمي المباشر..."
                                                },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFA7F3D0),
                                                textAlign = TextAlign.Center
                                            )
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(50)),
                                                color = Color(0xFF10B981),
                                                trackColor = Color.White.copy(alpha = 0.20f)
                                            )
                                        }
                                    }
                                }
                            }
                            nativeStreamError != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color.White.copy(alpha = 0.10f),
                                        border = BorderStroke(1.2.dp, Color(0xFFFDE68A).copy(alpha = 0.50f)),
                                        modifier = Modifier
                                            .fillMaxWidth(0.88f)
                                            .padding(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(22.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = nativeStreamError.orEmpty(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF059669),
                                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable { viewerMode = 0 }
                                            ) {
                                                Text(
                                                    text = "الانتقال إلى التصفح السحابي المباشر",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                val bmp = currentPageBitmap
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFF8FAFC))
                                        .pointerInput(currentPageIndex) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                                                zoomScale = newScale
                                                if (newScale > 1f) {
                                                    offsetX = (offsetX + pan.x).coerceIn(-600f, 600f)
                                                    offsetY = (offsetY + pan.y).coerceIn(-800f, 800f)
                                                } else {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "${book.bookTitle} - صفحة ${currentPageIndex + 1}",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    scaleX = zoomScale
                                                    scaleY = zoomScale
                                                    translationX = offsetX
                                                    translationY = offsetY
                                                }
                                        )
                                    } else {
                                        Text(
                                            text = "جاري تجهيز الصفحة ${currentPageIndex + 1}...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. شريط التحكم السفلي الزجاجي للتنقل والتكبير (Zero Classic Icons)
                if (viewerMode == 1 && pageCount > 0) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (currentPageIndex > 0) Color(0xFF059669) else Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = currentPageIndex > 0) {
                                        if (currentPageIndex > 0) currentPageIndex -= 1
                                    }
                            ) {
                                Text(
                                    text = "الصفحة السابقة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f)
                                            if (zoomScale == 1f) {
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "تصغير -",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }

                                Text(
                                    text = "صفحة ${currentPageIndex + 1} من $pageCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFA7F3D0)
                                )

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            zoomScale = (zoomScale + 0.25f).coerceAtMost(3.5f)
                                        }
                                ) {
                                    Text(
                                        text = "تكبير +",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (currentPageIndex < pageCount - 1) Color(0xFF059669) else Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = currentPageIndex < pageCount - 1) {
                                        if (currentPageIndex < pageCount - 1) currentPageIndex += 1
                                    }
                            ) {
                                Text(
                                    text = "الصفحة التالية",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * لوحة إدارة وتحديث روابط المناهج الدراسية السحابية (Developer-Only Curriculum Manager)
 * حصرية للمطور فقط داخل DeveloperControlPanelScreen — لا تظهر أبداً للمدير أو المعلم أو الطالب أو ولي الأمر.
 * مصممة بالكامل وفق سياسة Zero Classic Icons Policy (تصميم زجاجي نقي 3D Glassmorphism ونصوص صافية).
 */
@Composable
fun DeveloperCurriculumManagerSection(
    viewModel: SchoolViewModel
) {
    val allBooks by viewModel.globalCurriculumBooks.collectAsState()
    var selectedStage by remember { mutableStateOf(CurriculumStageTab.PRIMARY) }
    var selectedGrade by remember(selectedStage) { mutableStateOf(selectedStage.orderedGrades.first()) }
    var editingBook by remember { mutableStateOf<CurriculumBookItem?>(null) }
    var showAddBookForm by remember { mutableStateOf(false) }
    var newBookTitle by remember { mutableStateOf("") }
    var newBookUrl by remember { mutableStateOf("") }

    val gradeBooks = remember(allBooks, selectedStage, selectedGrade) {
        allBooks
            .filter { it.stage == selectedStage && it.gradeTitle.trim() == selectedGrade.trim() }
            .sortedBy { it.sortOrder }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF059669).copy(alpha = 0.18f),
                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.22f)
            )
            .testTag("developer_curriculum_manager_card"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(
            1.4.dp,
            Brush.linearGradient(
                listOf(Color(0xFF059669), Color(0xFF0F3D5E), Color(0xFFD97706))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Glass Header Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF044E3A), Color(0xFF059669), Color(0xFF0F3D5E))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إدارة وتحديث روابط المناهج الدراسية سحابياً",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    text = "حصري للمطور فقط",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFA7F3D0),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "العقدة السحابية المركزية (global_curriculum_config) — تحديث الروابط هنا ينعكس فوراً في كافة المدارس دون صلاحية تعديل للمدير.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }
            }

            // Stage Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurriculumStageTab.entries.forEach { st ->
                    val isSel = selectedStage == st
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) Color(0xFF059669) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFFA7F3D0) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedStage = st
                                selectedGrade = st.orderedGrades.first()
                            }
                    ) {
                        Text(
                            text = st.shortTitle,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSel) Color.White else Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }

            // Grade Selector Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(selectedStage.orderedGrades, key = { it }) { gr ->
                    val isSel = selectedGrade == gr
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSel) Color(0xFF0F3D5E) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFF93C5FD) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedGrade = gr }
                    ) {
                        Text(
                            text = gr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSel) Color.White else Color(0xFF1E293B),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Books in Selected Grade for Developer Link Management
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                gradeBooks.forEachIndexed { idx, book ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}. ${book.bookTitle}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = book.pdfUrl,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF059669),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFF059669)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { editingBook = book }
                            ) {
                                Text(
                                    text = "تحديث الرابط",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Developer Action Buttons (Add Book / Reset Defaults)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF059669),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showAddBookForm = !showAddBookForm }
                ) {
                    Text(
                        text = if (showAddBookForm) "إغلاق نموذج الإضافة" else "إضافة منهج جديد للصف",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.developerResetCurriculumToOfficialDefaults() }
                ) {
                    Text(
                        text = "استعادة الروابط الرسمية",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    )
                }
            }

            if (showAddBookForm) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "إضافة أو تحديث كتاب ضمن ($selectedGrade):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46)
                        )
                        OutlinedTextField(
                            value = newBookTitle,
                            onValueChange = { newBookTitle = it },
                            label = { Text("اسم الكتاب المنهجي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newBookUrl,
                            onValueChange = { newBookUrl = it },
                            label = { Text("رابط PDF المباشر (https://...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF047857),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.developerAddCurriculumBook(
                                        stage = selectedStage,
                                        gradeTitle = selectedGrade,
                                        bookTitle = newBookTitle,
                                        pdfUrl = newBookUrl
                                    )
                                    if (newBookTitle.isNotBlank() && newBookUrl.startsWith("http")) {
                                        newBookTitle = ""
                                        newBookUrl = ""
                                        showAddBookForm = false
                                    }
                                }
                        ) {
                            Text(
                                text = "حفظ وتعميم في السحابة المركزية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    editingBook?.let { target ->
        var editTitle by remember(target.id) { mutableStateOf(target.bookTitle) }
        var editUrl by remember(target.id) { mutableStateOf(target.pdfUrl) }

        AlertDialog(
            onDismissRequest = { editingBook = null },
            title = {
                Text(
                    text = "تحديث رابط المنهج (${target.gradeTitle})",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("اسم الكتاب الرسمي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("رابط PDF السحابي المباشر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF059669),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            viewModel.developerUpdateCurriculumBookUrl(
                                bookId = target.id,
                                newBookTitle = editTitle,
                                newPdfUrl = editUrl
                            )
                            editingBook = null
                        }
                ) {
                    Text(
                        text = "حفظ وتعميم الرابط سحابياً",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBook = null }) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
