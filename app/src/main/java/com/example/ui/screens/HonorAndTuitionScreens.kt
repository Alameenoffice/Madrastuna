package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DocumentAndIdExporter
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel

/**
 * Section 5: Celebratory Golden Honor Roll Screen (لوحة الشرف والطلبة المجتهدون)
 * Publicly visible to all roles, with editing scoped to Admin and Teacher (for their assigned classes).
 */
@Composable
fun HonorRollScreen(
    viewModel: SchoolViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()

    var selectedClass by remember { mutableStateOf("الكل") }
    var editingStudent by remember { mutableStateOf<StudentRecord?>(null) }
    var honorSearchQuery by remember { mutableStateOf("") }

    val canEdit = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val honorStudents = remember(students, selectedClass) {
        students
            .filter { it.isOnHonorRoll && (selectedClass == "الكل" || it.gradeClass == selectedClass) }
            .sortedByDescending { it.excellencePoints }
    }

    val scopedEditableStudents = remember(students, currentUser) {
        viewModel.getScopedStudentsForUser(currentUser)
    }

    val filteredNominatableStudents = remember(scopedEditableStudents, honorSearchQuery, selectedClass) {
        val q = honorSearchQuery.trim()
        scopedEditableStudents.filter { s ->
            val matchesClass = selectedClass == "الكل" || s.gradeClass == selectedClass
            val matchesSearch = if (q.isEmpty()) {
                !s.isOnHonorRoll
            } else {
                s.fullNameWithFather.contains(q, ignoreCase = true) ||
                    s.studentName.contains(q, ignoreCase = true) ||
                    s.parentName.contains(q, ignoreCase = true) ||
                    s.gradeClass.contains(q, ignoreCase = true)
            }
            matchesClass && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF451A03),
                                    Color(0xFFB45309),
                                    Color(0xFFD97706)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFDE68A).copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.65f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "لوحة الشرف الذهبية • ${schoolConfig.schoolName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFFDE68A),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = "نخبة الطلبة الأوائل والمجتهدين",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )

                        Text(
                            text = "تفتخر إدارة المدرسة في (${schoolConfig.address}) بتكريم كوكبة التفوق والإبداع العلمي",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }
        }

        // Class Filter Row from Registered Classes
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val classesList = listOf("الكل") + schoolConfig.registeredClasses
                items(classesList) { cls ->
                    FilterChip(
                        selected = selectedClass == cls,
                        onClick = { selectedClass = cls },
                        label = { Text(cls, maxLines = 1, softWrap = false) },
                        leadingIcon = {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        // Fast Search by Student Name for Admin / Teacher to add or update students on the Honor Roll with one tap
        if (canEdit) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonSearch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "إضافة سريعة للوحة الشرف (بحث بالاسم)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            NoWrapBadge(
                                text = "اختيار بضغطة واحدة",
                                icon = Icons.Filled.EmojiEvents,
                                containerColor = GoldAccent.copy(alpha = 0.22f),
                                contentColor = GoldDeep
                            )
                        }

                        OutlinedTextField(
                            value = honorSearchQuery,
                            onValueChange = { honorSearchQuery = it },
                            label = { Text("ابحث باسم الطالب أو اسم الأب لإضافته للوحة الشرف...") },
                            placeholder = { Text("اكتب اسم الطالب ليظهر فوراً وتختاره بضغطة واحدة") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (honorSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { honorSearchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "مسح البحث")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("honor_student_search_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        if (honorSearchQuery.isNotBlank() || filteredNominatableStudents.isNotEmpty()) {
                            Text(
                                text = if (honorSearchQuery.isNotBlank()) {
                                    "نتائج البحث (${filteredNominatableStudents.size}) — اضغط على اسم الطالب لإضافته فوراً:"
                                } else {
                                    "طلبة مرشحون للإضافة إلى لوحة الشرف (اضغط لمنح وسام الشرف):"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val displayCandidates = if (honorSearchQuery.isNotBlank()) {
                                filteredNominatableStudents
                            } else {
                                filteredNominatableStudents.take(6)
                            }

                            if (displayCandidates.isEmpty()) {
                                Text(
                                    text = "لا يوجد طالب مطابق لاسم البحث ضمن صلاحيتك",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    displayCandidates.forEach { s ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { editingStudent = s }
                                                .testTag("honor_nominate_student_${s.id}"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = s.fullNameWithFather,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${s.gradeClass} • شعبة (${s.sectionName})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = GoldAccent.copy(alpha = 0.2f),
                                                    modifier = Modifier.clickable { editingStudent = s }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.EmojiEvents,
                                                            contentDescription = null,
                                                            tint = GoldDeep,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = if (s.isOnHonorRoll) "تعديل الوسام" else "منح وسام الشرف",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = GoldDeep,
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
                    }
                }
            }
        }

        itemsIndexed(honorStudents, key = { _, s -> s.id }) { index, stu ->
            val rank = index + 1
            val canEditThisStudent = canEdit && stu.id in scopedEditableStudents.map { it.id }
            val isLikedByMe = currentUser?.id != null && currentUser?.id in stu.likedByUserIds

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(GoldAccent, Color(0xFFFDE68A), GoldDeep)
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            GoldenVectorMedal(rank = rank)

                            Base64AvatarImage(
                                base64Data = stu.photoBase64,
                                fallbackIcon = Icons.Filled.School,
                                contentDescription = stu.fullNameWithFather,
                                size = 60.dp,
                                tint = GoldAccent
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = stu.fullNameWithFather,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "الصف: ${stu.gradeClass} • شعبة (${stu.sectionName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                NoWrapBadge(
                                    text = "سبب التكريم: ${stu.honorTitle}",
                                    icon = Icons.Filled.MilitaryTech,
                                    containerColor = GoldAccent.copy(alpha = 0.18f),
                                    contentColor = GoldDeep
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = GoldDeep,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${stu.excellencePoints} نقطة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF78350F),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    val autoBadges = viewModel.getStudentAutoBadges(stu.id)
                    if (autoBadges.isNotEmpty()) {
                        StudentAutoBadgesRow(badges = autoBadges)
                    }

                    HorizontalDivider(color = GoldAccent.copy(alpha = 0.22f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = GoldDeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "شارِك التهنئة والتشجيع للطالب المتفوق",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HonorHeartReactionButton(
                            heartCount = stu.heartCount,
                            isLikedByMe = isLikedByMe,
                            onReact = { viewModel.reactWithHeartToHonorStudent(stu.id) },
                            testTag = "honor_heart_button_${stu.id}"
                        )
                    }

                    if (canEditThisStudent) {
                        OutlinedButton(
                            onClick = { editingStudent = stu },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تحديث نقاط التميز أو وسام الشرف",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingStudent != null) {
        val stu = editingStudent!!
        var points by remember { mutableStateOf(stu.excellencePoints.toString()) }
        var honorTitle by remember { mutableStateOf(stu.honorTitle) }
        var onHonor by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { editingStudent = null },
            title = { Text("تحديث وسام التميز: ${stu.fullNameWithFather}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = points,
                        onValueChange = { points = it.filter { c -> c.isDigit() } },
                        label = { Text("نقاط التميز (50 - 100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = honorTitle,
                        onValueChange = { honorTitle = it },
                        label = { Text("عنوان الوسام الذهبي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إظهار في لوحة الشرف الذهبية")
                        Switch(checked = onHonor, onCheckedChange = { onHonor = it })
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "حفظ التكريم",
                    icon = Icons.Filled.WorkspacePremium,
                    onClick = {
                        viewModel.updateStudentHonorRoll(
                            studentId = stu.id,
                            isOnHonorRoll = onHonor,
                            excellencePoints = points.toIntOrNull() ?: 95,
                            honorTitle = honorTitle
                        )
                        editingStudent = null
                        honorSearchQuery = ""
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { editingStudent = null }) { Text("إلغاء") }
            }
        )
    }
}

/**
 * Section 6: Tuition Fees System Screen (نظام الأقساط الدراسية)
 * Strictly isolates Parent/Student so they see ONLY their own student's tuition record.
 */
@Composable
fun TuitionFeesScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val students by viewModel.students.collectAsState()
    val tuitions by viewModel.tuitions.collectAsState()

    var selectedTuitionForPayment by remember { mutableStateOf<TuitionRecord?>(null) }
    var selectedReceiptPair by remember { mutableStateOf<Pair<TuitionRecord, TuitionInstallment>?>(null) }

    val canRecordPayment = currentUser?.role == UserRole.ADMIN
    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT

    val visibleTuitions = remember(tuitions, currentUser, selectedParentChildId, students) {
        when (currentUser?.role) {
            UserRole.PARENT, UserRole.STUDENT -> {
                val activeStuIds = viewModel.getScopedStudentsForUser(currentUser).map { it.id }.toSet()
                tuitions.filter { it.studentId in activeStuIds }
            }
            else -> tuitions
        }
    }

    val totalFeesAll = remember(visibleTuitions) { visibleTuitions.sumOf { it.totalFee } }
    val totalPaidAll = remember(visibleTuitions) { visibleTuitions.sumOf { it.paidAmount } }
    val totalRemainingAll = remember(visibleTuitions) { visibleTuitions.sumOf { it.remainingAmount } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isParentOrStudent) "كشف الأقساط الخاص بالطالب" else "نظام الأقساط الدراسية (${schoolConfig.schoolType.arabicLabel})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        NoWrapBadge(
                            text = if (isParentOrStudent) "خاص" else "مفعل تلقائياً",
                            icon = Icons.Filled.Verified,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinancialStatChip(
                            label = "القسط الكلي",
                            amount = "$totalFeesAll د.ع",
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        FinancialStatChip(
                            label = "الدفعات الواصلة",
                            amount = "$totalPaidAll د.ع",
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        FinancialStatChip(
                            label = "المبلغ المتبقي",
                            amount = "$totalRemainingAll د.ع",
                            color = DangerRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (canRecordPayment) {
                        OutlinedButton(
                            onClick = {
                                val msg = DocumentAndIdExporter.exportTuitionStatementPdf(
                                    context = context,
                                    tuitions = visibleTuitions,
                                    schoolConfig = schoolConfig
                                )
                                viewModel.showBannerMessage(msg)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_tuition_report_pdf_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير كشف الأقساط المالي الرسمي (PDF / طباعة)")
                        }
                    }
                }
            }
        }

        items(visibleTuitions, key = { it.studentId }) { record ->
            val progress = if (record.totalFee > 0) {
                (record.paidAmount.toFloat() / record.totalFee.toFloat()).coerceIn(0f, 1f)
            } else 1f

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.studentName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${record.gradeClass} (${record.sectionName}) • ولي الأمر: ${record.parentName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        NoWrapBadge(
                            text = if (record.remainingAmount == 0L) "مسدد بالكامل" else "استحقاق: ${record.nextDueDate}",
                            icon = if (record.remainingAmount == 0L) Icons.Filled.CheckCircle else Icons.Filled.EventRepeat,
                            containerColor = if (record.remainingAmount == 0L) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.18f),
                            contentColor = if (record.remainingAmount == 0L) SuccessGreen else GoldDeep
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = SuccessGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الكلي: ${record.totalFee} د.ع",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "الواصل: ${record.paidAmount} د.ع",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen
                        )
                        Text(
                            text = "المتبقي: ${record.remainingAmount} د.ع",
                            style = MaterialTheme.typography.labelLarge,
                            color = DangerRed
                        )
                    }

                    if (record.installments.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "سجل الدفعات والوصولات الرقمية الرسمية:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                record.installments.forEach { inst ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${inst.receiptNumber} • ${inst.amount} د.ع (${inst.paymentDate})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            TextButton(
                                                onClick = { selectedReceiptPair = record to inst },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.testTag("view_digital_receipt_btn_${inst.receiptNumber}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ReceiptLong,
                                                    contentDescription = null,
                                                    tint = DeepEmerald,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "وصل رقمي PDF",
                                                    color = DeepEmerald,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    val msg = """
                                                        وصل قبض قسط دراسي رقم (${inst.receiptNumber})
                                                        الصادر من: ${schoolConfig.schoolName} (${schoolConfig.address})
                                                        • اسم الطالب: ${record.studentName} (${record.gradeClass})
                                                        • المبلغ المستلم: ${inst.amount} دينار عراقي
                                                        • تاريخ الدفعة: ${inst.paymentDate}
                                                        • إجمالي المدفوع: ${record.paidAmount} د.ع
                                                        • المبلغ المتبقي: ${record.remainingAmount} د.ع
                                                        شكراً لتعاونكم الكريم.
                                                    """.trimIndent()
                                                    ExternalIntents.sendWhatsAppMessage(context, record.parentPhone, msg)
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Chat,
                                                    contentDescription = null,
                                                    tint = WhatsAppGreen,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "واتساب",
                                                    color = WhatsAppGreen,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canRecordPayment && record.remainingAmount > 0L) {
                            VectorActionButton(
                                text = "تسجيل دفعة جديدة",
                                icon = Icons.Filled.AddCard,
                                onClick = { selectedTuitionForPayment = record },
                                modifier = Modifier.weight(1f),
                                testTag = "add_payment_${record.studentId}"
                            )
                        }

                        VectorActionButton(
                            text = "إرسال وصل دفع عبر واتساب",
                            icon = Icons.Filled.Chat,
                            containerColor = WhatsAppGreen,
                            onClick = {
                                val latestReceipt = record.installments.firstOrNull()?.receiptNumber ?: "REC-1000"
                                val msg = """
                                    كشف ووصل القسط الدراسي (${latestReceipt})
                                    من إدارة (${schoolConfig.schoolName} - ${schoolConfig.address})
                                    • الطالب: ${record.studentName} (${record.gradeClass})
                                    • القسط السنوي الكلي: ${record.totalFee} د.ع
                                    • مجموع الدفعات الواصلة: ${record.paidAmount} د.ع
                                    • المبلغ المتبقي: ${record.remainingAmount} د.ع
                                    • تاريخ الاستحقاق القادم: ${record.nextDueDate}
                                """.trimIndent()
                                ExternalIntents.sendWhatsAppMessage(context, record.parentPhone, msg)
                            },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }

    selectedReceiptPair?.let { (tuitionRec, installment) ->
        AlertDialog(
            onDismissRequest = { selectedReceiptPair = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = DeepEmerald)
                    Text(
                        text = "وصل قبض رقمي رسمي (${installment.receiptNumber})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DeepEmerald.copy(alpha = 0.07f),
                    border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${schoolConfig.schoolName} (${schoolConfig.schoolType.arabicLabel})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepEmerald
                        )
                        Text(
                            text = "العنوان: ${schoolConfig.address}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(color = GoldAccent.copy(alpha = 0.4f))
                        Text("• اسم الطالب: ${tuitionRec.studentName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("• الصف والشعبة: ${tuitionRec.gradeClass} (${tuitionRec.sectionName})", style = MaterialTheme.typography.bodySmall)
                        Text("• ولي الأمر: ${tuitionRec.parentName}", style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(color = GoldAccent.copy(alpha = 0.4f))
                        Text(
                            text = "المبلغ المستلم في هذا الوصل: ${installment.amount} د.ع",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen
                        )
                        Text("• تاريخ الدفعة: ${installment.paymentDate}", style = MaterialTheme.typography.bodySmall)
                        Text("• إجمالي القسط السنوي: ${tuitionRec.totalFee} د.ع", style = MaterialTheme.typography.bodySmall)
                        Text("• مجموع الواصل الكلي: ${tuitionRec.paidAmount} د.ع", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "• الرصيد المتبقي: ${tuitionRec.remainingAmount} د.ع",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (tuitionRec.remainingAmount == 0L) SuccessGreen else DangerRed
                        )
                        Text("• البيان: ${installment.note}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val msg = DocumentAndIdExporter.exportSingleInstallmentReceiptPdf(
                            context = context,
                            tuitionRecord = tuitionRec,
                            installment = installment,
                            schoolConfig = schoolConfig
                        )
                        viewModel.showBannerMessage(msg)
                        selectedReceiptPair = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White),
                    modifier = Modifier.testTag("print_single_receipt_pdf_btn")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طباعة / حفظ الوصل (PDF)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReceiptPair = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (selectedTuitionForPayment != null) {
        val rec = selectedTuitionForPayment!!
        var amountText by remember { mutableStateOf("250000") }
        var paymentDate by remember { mutableStateOf("2026-10-29") }
        var nextDue by remember { mutableStateOf("2026-12-01") }
        var note by remember { mutableStateOf("دفعة قسط دراسي") }

        AlertDialog(
            onDismissRequest = { selectedTuitionForPayment = null },
            title = { Text("تسجيل دفعة قسط: ${rec.studentName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "المبلغ المتبقي الحالي: ${rec.remainingAmount} د.ع",
                        style = MaterialTheme.typography.labelLarge,
                        color = DangerRed
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                        label = { Text("مبلغ الدفعة الواصلة (د.ع)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = { paymentDate = it },
                        label = { Text("تاريخ الدفع") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nextDue,
                        onValueChange = { nextDue = it },
                        label = { Text("تاريخ الاستحقاق القادم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("ملاحظات الوصل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "تأكيد الدفعة وإشعار ولي الأمر",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        val amt = amountText.toLongOrNull() ?: 0L
                        if (amt > 0L) {
                            viewModel.addTuitionPayment(
                                studentId = rec.studentId,
                                amount = amt,
                                paymentDate = paymentDate,
                                note = note,
                                nextDueDate = nextDue
                            )
                            selectedTuitionForPayment = null
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { selectedTuitionForPayment = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun FinancialStatChip(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
