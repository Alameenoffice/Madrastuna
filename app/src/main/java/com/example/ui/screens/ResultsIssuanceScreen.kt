package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSection
import com.example.model.ArabicTextNormalizer
import com.example.model.ReportCardPeriodType
import com.example.model.UserRole
import com.example.ui.components.ThreeDHeroHeaderCard
import com.example.util.DocumentAndIdExporter
import com.example.viewmodel.SchoolViewModel

/**
 * القسم المستقل «إصدار النتائج» (Standalone Results & Report Cards Issuance Section):
 * - بطاقة ترحيبية زمردية متدرجة بحافة سفلية متموّجة متطابقة مع هوية المنصة مع وحدة الشهادة المتجهة.
 * - بطاقات نظيفة (أبيض نقي نهاراً / سطوح داكنة ليلاً) لاختيار الصف والشعبة والفصل الدراسي مع البحث.
 * - كامل منطق المعاينة، الإصدار الجماعي، الإرسال الفردي، السحب/الإلغاء، وطباعة/تصدير PDF دون فقدان أي وظيفة.
 */
@Composable
fun ResultsIssuanceScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme

    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val issuedReportCards by viewModel.issuedReportCards.collectAsState()
    val activeCelebrationCard by viewModel.activeCelebrationReportCard.collectAsState()
    val deepLinkTarget by viewModel.deepLinkTarget.collectAsState()

    val isPrincipal = currentUser?.role == UserRole.ADMIN
    val allowedClasses = remember(currentUser, schoolConfig, students) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    var selectedClass by remember(currentUser?.id, allowedClasses) {
        mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط")
    }
    val allowedSections = remember(selectedClass, currentUser, schoolConfig, students) {
        viewModel.getAllowedSectionsForClass(selectedClass, currentUser)
    }
    var selectedSection by remember(currentUser?.id, selectedClass, allowedSections) {
        mutableStateOf(allowedSections.firstOrNull() ?: "أ")
    }
    var selectedReportCardPeriod by remember { mutableStateOf(ReportCardPeriodType.COMPREHENSIVE_ALL) }
    var searchStudentQuery by remember { mutableStateOf("") }

    LaunchedEffect(deepLinkTarget, issuedReportCards) {
        val target = deepLinkTarget
        if (target != null && target.section == AppSection.RESULTS_ISSUANCE) {
            if (target.itemId.startsWith("rc_")) {
                issuedReportCards.firstOrNull { it.id == target.itemId }?.let { card ->
                    viewModel.openCelebrationReportCardModal(card)
                }
            }
            viewModel.clearDeepLinkTarget()
        }
    }

    // Active Celebration / Official Report Card Preview & PDF Export Modal
    activeCelebrationCard?.let { cardToView ->
        val matchedStu = students.firstOrNull { it.id == cardToView.studentId }
        val stuGrades = grades.filter { it.studentId == cardToView.studentId }
        LuxuryInteractiveReportCardModal(
            reportCard = cardToView,
            schoolConfig = schoolConfig,
            onDismiss = { viewModel.closeCelebrationReportCardModal() },
            onPrintPdf = {
                if (matchedStu != null) {
                    val res = DocumentAndIdExporter.exportStudentReportCardPdf(
                        context = context,
                        student = matchedStu,
                        grades = stuGrades,
                        schoolConfig = schoolConfig,
                        principalName = cardToView.principalName,
                        periodLabel = cardToView.periodType.arabicTitle
                    )
                    viewModel.showBannerMessage(res)
                }
            }
        )
    }

    val allScopedStudents = remember(students, currentUser) {
        viewModel.getScopedStudentsForUser(currentUser)
    }
    val hubClassStudents = remember(allScopedStudents, selectedClass, selectedSection, searchStudentQuery) {
        val cleanQuery = searchStudentQuery.trim()
        allScopedStudents.filter { stu ->
            ArabicTextNormalizer.matchesClass(stu.gradeClass, selectedClass) &&
                ArabicTextNormalizer.matchesSection(stu.sectionName, selectedSection) &&
                (cleanQuery.isBlank() ||
                    stu.studentName.contains(cleanQuery, ignoreCase = true) ||
                    stu.fullNameWithFather.contains(cleanQuery, ignoreCase = true))
        }
    }
    val studentPreviewCards = remember(hubClassStudents, grades, selectedReportCardPeriod, schoolConfig) {
        hubClassStudents.map { stu ->
            stu to viewModel.buildStudentReportCardPreview(stu, selectedReportCardPeriod)
        }
    }
    val totalClassStudentsCount = studentPreviewCards.size
    val passedStudentsCount = studentPreviewCards.count { it.second.isOverallPassed }
    val failedStudentsCount = (totalClassStudentsCount - passedStudentsCount).coerceAtLeast(0)

    val edgeToEdgeWaveModifier = Modifier.layout { measurable, constraints ->
        val bleedPx = 32.dp.roundToPx()
        val fullW = (constraints.maxWidth + bleedPx).coerceAtLeast(constraints.minWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = fullW, maxWidth = fullW))
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    val emeraldPrimary = if (isDark) Color(0xFF10B981) else Color(0xFF047857)
    val emeraldAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val surfaceBorder = if (isDark) Color(0xFF34D399).copy(alpha = 0.24f) else Color(0xFF059669).copy(alpha = 0.18f)
    val chipContainerUnselected = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.55f) else Color(0xFFF3FAF7)
    val chipLabelUnselected = if (isDark) colorScheme.onSurface else Color(0xFF064E3B)

    val resultsChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Color(0xFF059669),
        selectedLabelColor = Color.White,
        containerColor = chipContainerUnselected,
        labelColor = chipLabelUnselected
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("results_issuance_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // 1) Edge-to-Edge Emerald Waved Welcome Hero Banner with Vector Certificate Emblem
        item(key = "results_issuance_hero") {
            ThreeDHeroHeaderCard(
                title = "إصدار النتائج والشهادات",
                subtitle = "استخراج ومعاينة وإصدار الكارتات المدرسية الرسمية وحساب النجاح والرسوب آلياً وإرسالها لأولياء الأمور",
                rightPillText = "$selectedClass • شعبة $selectedSection",
                rightPillIcon = Icons.Filled.WorkspacePremium,
                leftPillText = selectedReportCardPeriod.shortLabel,
                leftPillIcon = Icons.Filled.Verified,
                customTrailingContent = {
                    ReportCardCertificateHeaderWidget()
                },
                modifier = edgeToEdgeWaveModifier
            )
        }

        // 2) Clean Filter & Selection Card (Class, Section, Term/Period, Search, KPIs, Bulk Action)
        item(key = "results_issuance_filters_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 3.dp else 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color(0xFF047857).copy(alpha = 0.12f),
                        spotColor = Color(0xFF047857).copy(alpha = 0.14f)
                    )
                    .testTag("results_issuance_filters_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(1.1.dp, surfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = emeraldAccent.copy(alpha = if (isDark) 0.18f else 0.11f),
                            border = BorderStroke(0.9.dp, emeraldAccent.copy(alpha = 0.30f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = emeraldAccent,
                                modifier = Modifier
                                    .padding(7.dp)
                                    .size(18.dp)
                            )
                        }
                        Text(
                            text = "تحديد الصف والشعبة والفصل الدراسي:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Grade Class Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allowedClasses) { cls ->
                            FilterChip(
                                selected = selectedClass == cls,
                                onClick = {
                                    selectedClass = cls
                                    val nextSecs = viewModel.getAllowedSectionsForClass(cls, currentUser)
                                    if (selectedSection !in nextSecs) {
                                        selectedSection = nextSecs.firstOrNull() ?: "أ"
                                    }
                                },
                                colors = resultsChipColors,
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedClass == cls,
                                    borderColor = emeraldAccent.copy(alpha = 0.24f),
                                    selectedBorderColor = Color(0xFF047857)
                                ),
                                label = {
                                    Text(
                                        text = cls,
                                        fontWeight = if (selectedClass == cls) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }

                    // Section Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "الشعبة:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = emeraldPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allowedSections) { sec ->
                                FilterChip(
                                    selected = selectedSection == sec,
                                    onClick = { selectedSection = sec },
                                    colors = resultsChipColors,
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedSection == sec,
                                        borderColor = emeraldAccent.copy(alpha = 0.24f),
                                        selectedBorderColor = Color(0xFF047857)
                                    ),
                                    label = {
                                        Text(
                                            text = "شعبة $sec",
                                            fontWeight = if (selectedSection == sec) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Term / Report Card Period Selector
                    Text(
                        text = "الفصل الدراسي / نوع النتيجة:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = emeraldPrimary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReportCardPeriodType.entries) { pType ->
                            FilterChip(
                                selected = selectedReportCardPeriod == pType,
                                onClick = { selectedReportCardPeriod = pType },
                                colors = resultsChipColors,
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedReportCardPeriod == pType,
                                    borderColor = emeraldAccent.copy(alpha = 0.24f),
                                    selectedBorderColor = Color(0xFF047857)
                                ),
                                label = {
                                    Text(
                                        text = pType.arabicTitle,
                                        fontWeight = if (selectedReportCardPeriod == pType) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }

                    // Student Quick Search Input
                    OutlinedTextField(
                        value = searchStudentQuery,
                        onValueChange = { searchStudentQuery = it },
                        placeholder = {
                            Text(
                                text = "ابحث باسم الطالب لتصفية النتائج...",
                                fontSize = 12.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "بحث عن طالب",
                                tint = emeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchStudentQuery.isNotBlank()) {
                                IconButton(onClick = { searchStudentQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "مسح البحث",
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = emeraldAccent,
                            unfocusedBorderColor = surfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("results_issuance_search_input")
                    )

                    // 3 KPI Summary Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResultsSummaryKpiTile(
                            value = "$totalClassStudentsCount",
                            label = "إجمالي طلاب الصف",
                            bgColor = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.55f) else Color(0xFFF8FAFC),
                            borderColor = if (isDark) colorScheme.outlineVariant else Color(0xFFCBD5E1),
                            valueColor = emeraldPrimary,
                            labelColor = colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        ResultsSummaryKpiTile(
                            value = "$passedStudentsCount",
                            label = "عدد الناجحين",
                            bgColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5),
                            borderColor = if (isDark) Color(0xFF34D399).copy(alpha = 0.45f) else Color(0xFF6EE7B7),
                            valueColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                            labelColor = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46),
                            modifier = Modifier.weight(1f)
                        )
                        ResultsSummaryKpiTile(
                            value = "$failedStudentsCount",
                            label = "الراسبين / المكملين",
                            bgColor = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                            borderColor = if (isDark) Color(0xFFF87171).copy(alpha = 0.45f) else Color(0xFFFCA5A5),
                            valueColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                            labelColor = if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Bulk Issue & Send Button (Principal only)
                    if (isPrincipal) {
                        Button(
                            onClick = {
                                viewModel.issueAndSendClassReportCardsBulk(
                                    gradeClass = selectedClass,
                                    sectionName = selectedSection,
                                    periodType = selectedReportCardPeriod
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF047857),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_bulk_issue_report_cards")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إصدار وإرسال النتائج لجميع طلاب الصف دفعة واحدة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 3) Empty State if No Students Match
        if (studentPreviewCards.isEmpty()) {
            item(key = "results_issuance_empty") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    border = BorderStroke(1.dp, surfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = emeraldAccent,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "لا يوجد طلاب مطابقون للبحث في $selectedClass (شعبة $selectedSection)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "اختر صفاً أو شعبة أخرى لاستعراض وإصدار النتائج والكارتات المدرسية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 4) Student Result Cards with Preview, Direct PDF Print/Export, Issue/Resend & Revoke
        items(studentPreviewCards, key = { "${it.first.id}_${selectedReportCardPeriod.name}" }) { (stu, previewCard) ->
            val alreadyIssued = issuedReportCards.any {
                it.studentId == stu.id && it.periodType == selectedReportCardPeriod
            }
            val stuGrades = remember(grades, stu.id) { grades.filter { it.studentId == stu.id } }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_student_card_${stu.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                border = BorderStroke(
                    1.1.dp,
                    if (previewCard.isOverallPassed) {
                        if (isDark) Color(0xFF34D399).copy(alpha = 0.42f) else Color(0xFF6EE7B7)
                    } else {
                        if (isDark) Color(0xFFF87171).copy(alpha = 0.45f) else Color(0xFFFCA5A5)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 1.dp else 2.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stu.fullNameWithFather,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${stu.gradeClass} (${stu.sectionName}) • المعدل: ${previewCard.averagePercent}% • المجموع: ${previewCard.totalScore}",
                                fontSize = 11.5.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (previewCard.isOverallPassed) {
                                if (isDark) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFFECFDF5)
                            } else {
                                if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEF2F2)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (previewCard.isOverallPassed) Color(0xFF10B981) else Color(0xFFF87171)
                            )
                        ) {
                            Text(
                                text = if (previewCard.isOverallPassed) {
                                    "ناجح"
                                } else {
                                    "مكمل / راسب (${previewCard.failedSubjectsCount} مواد)"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (previewCard.isOverallPassed) {
                                    if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                } else {
                                    if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Subject Mini Score Pills
                    if (previewCard.subjects.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(previewCard.subjects) { subItem ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (subItem.isPassed) {
                                        if (isDark) Color(0xFF064E3B).copy(alpha = 0.42f) else Color(0xFFF0FDF4)
                                    } else {
                                        if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.38f) else Color(0xFFFEF2F2)
                                    },
                                    border = BorderStroke(
                                        0.9.dp,
                                        if (subItem.isPassed) {
                                            if (isDark) Color(0xFF34D399).copy(alpha = 0.40f) else Color(0xFFA7F3D0)
                                        } else {
                                            if (isDark) Color(0xFFF87171).copy(alpha = 0.45f) else Color(0xFFFECACA)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = "${subItem.subjectName}: ${subItem.score} / ${subItem.maxScoreScale}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (subItem.isPassed) {
                                            if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                        } else {
                                            if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Primary Actions Row: Preview + Direct PDF Print/Export
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.openCelebrationReportCardModal(previewCard) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, emeraldAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("btn_preview_result_${stu.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = emeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "معاينة الكارت",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = emeraldPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val res = DocumentAndIdExporter.exportStudentReportCardPdf(
                                    context = context,
                                    student = stu,
                                    grades = stuGrades,
                                    schoolConfig = schoolConfig,
                                    principalName = previewCard.principalName,
                                    periodLabel = selectedReportCardPeriod.arabicTitle
                                )
                                viewModel.showBannerMessage(res)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.1.dp, colorScheme.outlineVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("btn_export_pdf_result_${stu.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = emeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "طباعة / PDF",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Principal Issuance & Revoke Actions Row
                    if (isPrincipal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.issueAndSendStudentReportCard(stu, selectedReportCardPeriod)
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (alreadyIssued) Color(0xFF047857) else Color(0xFF059669),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp)
                                    .testTag("btn_send_result_${stu.id}")
                            ) {
                                Icon(
                                    imageVector = if (alreadyIssued) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (alreadyIssued) "تم الإرسال (إعادة إرسال)" else "إصدار وإرسال لولي الأمر",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (alreadyIssued) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.revokeStudentReportCard(stu, selectedReportCardPeriod)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.1.dp, Color(0xFFDC2626).copy(alpha = 0.55f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .heightIn(min = 46.dp)
                                        .testTag("btn_revoke_result_${stu.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Undo,
                                        contentDescription = "سحب النتيجة",
                                        tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "سحب",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
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

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ResultsSummaryKpiTile(
    value: String,
    label: String,
    bgColor: Color,
    borderColor: Color,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
