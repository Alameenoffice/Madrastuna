package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DocumentAndIdExporter
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
            ThreeDHeroHeaderCard(
                title = "نخبة الطلبة الأوائل والمجتهدين",
                subtitle = "تفتخر إدارة (${schoolConfig.schoolName}) بتكريم كوكبة التفوق والإبداع العلمي",
                rightPillText = "لوحة الشرف • ${schoolConfig.schoolName}",
                rightPillIcon = Icons.Filled.EmojiEvents,
                leftPillText = schoolConfig.academicYear,
                leftPillIcon = Icons.Filled.WorkspacePremium,
                illustrationType = ThreeDIllustrationType.ROYAL_HONOR_SHIELD
            )
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
                        containerColor = Color(0xFFECFDF5)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
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
                                    tint = Color(0xFF059669)
                                )
                                Text(
                                    text = "إضافة سريعة للوحة الشرف (بحث بالاسم)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }
                            NoWrapBadge(
                                text = "اختيار بضغطة واحدة",
                                icon = Icons.Filled.EmojiEvents,
                                containerColor = Color(0xFFD1FAE5),
                                contentColor = Color(0xFF059669)
                            )
                        }

                        OutlinedTextField(
                            value = honorSearchQuery,
                            onValueChange = { honorSearchQuery = it },
                            label = { Text("ابحث باسم الطالب أو اسم الأب لإضافته للوحة الشرف...") },
                            placeholder = { Text("اكتب اسم الطالب ليظهر فوراً وتختاره بضغطة واحدة") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF059669)) },
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
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF059669),
                                unfocusedBorderColor = Color(0xFFA7F3D0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        if (honorSearchQuery.isNotBlank() || filteredNominatableStudents.isNotEmpty()) {
                            Text(
                                text = if (honorSearchQuery.isNotBlank()) {
                                    "نتائج البحث (${filteredNominatableStudents.size}) — اضغط على اسم الطالب لإضافته فوراً:"
                                } else {
                                    "طلبة مرشحون للإضافة إلى لوحة الشرف (اضغط لمنح وسام الشرف):"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF065F46)
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
                                                containerColor = Color.White
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFD1FAE5))
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
                                                        color = Color(0xFF1E293B)
                                                    )
                                                    Text(
                                                        text = "${s.gradeClass} • شعبة (${s.sectionName})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFECFDF5),
                                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
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
                                                            tint = Color(0xFF059669),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = if (s.isOnHonorRoll) "تعديل الوسام" else "منح وسام الشرف",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF059669),
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
                        color = Color(0xFF059669).copy(alpha = 0.45f),
                        shape = RoundedCornerShape(22.dp)
                    ),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val autoBadges = viewModel.getStudentAutoBadges(stu.id)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            GoldenVectorMedal(rank = rank)

                            StudentAvatarWithOverlayBadge(
                                base64Data = stu.photoBase64,
                                contentDescription = stu.fullNameWithFather,
                                badges = autoBadges,
                                fallbackHonorTitle = stu.honorTitle,
                                isOnHonorRoll = true,
                                fallbackIcon = Icons.Filled.School,
                                avatarSize = 56.dp,
                                boxWidth = 64.dp,
                                tint = Color(0xFF059669)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = stu.fullNameWithFather,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "الصف: ${stu.gradeClass} • شعبة (${stu.sectionName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${stu.excellencePoints} نقطة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Full-width comfortable row for the Honor Reason so it never clips ("سبب التكريم")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MilitaryTech,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "سبب التكريم: ${stu.honorTitle.ifBlank { "التفوق والالتزام المثالي" }}",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFD1FAE5))

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
                                tint = Color(0xFF059669),
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
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF059669))
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
                        label = { Text("عنوان وسام الشرف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إظهار في لوحة الشرف")
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
 * Helper functions for accounting number formatting (1,500,000 د.ع)
 * and date formatting (20261115 or 2026-11-15 -> 2026/11/15).
 */
private fun formatAccountingIqd(amount: Long): String {
    val formatted = NumberFormat.getNumberInstance(Locale.US).format(amount)
    return "$formatted د.ع"
}

private fun formatAccountingDate(rawDate: String): String {
    val cleaned = rawDate.trim()
    if (cleaned.isBlank()) return "غير محدد"
    val digitsOnly = cleaned.filter { it.isDigit() }
    if (digitsOnly.length == 8) {
        val y = digitsOnly.substring(0, 4)
        val m = digitsOnly.substring(4, 6)
        val d = digitsOnly.substring(6, 8)
        return "$y/$m/$d"
    }
    return cleaned.replace("-", "/")
}

/**
 * Section 6: Tuition Fees System Screen (نظام الأقساط والحسابات المالية)
 * Redesigned into a Luxury Banking & Accounting Hub (Premium Financial & Tuition Hub).
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
    ) {
        // 1) Emerald Financial Hero Banner + 3 Banking Stat Cards + Wide PDF Export Button
        item(key = "emerald_financial_hero_hub") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmeraldFinancialHeroBanner()

                // 3 Luxury Banking Stat Cards directly below the banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancialStatChip(
                        label = "إجمالي الأقساط",
                        amount = formatAccountingIqd(totalFeesAll),
                        color = Color(0xFF0F172A),
                        accentBarColor = Color(0xFFD97706),
                        icon = Icons.Filled.AccountBalance,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialStatChip(
                        label = "مجموع الواصل",
                        amount = formatAccountingIqd(totalPaidAll),
                        color = Color(0xFF047857),
                        accentBarColor = Color(0xFF10B981),
                        icon = Icons.Filled.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialStatChip(
                        label = "إجمالي المتبقي",
                        amount = formatAccountingIqd(totalRemainingAll),
                        color = Color(0xFF9F1239),
                        accentBarColor = Color(0xFFE11D48),
                        icon = Icons.Filled.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Wide & Elegant Financial Statement PDF Export Button
                Button(
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
                        .height(48.dp)
                        .testTag("export_tuition_report_pdf_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF064E3B),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصدير كشف الأقساط المالي الرسمي PDF",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2) Student Tuition Cards
        items(visibleTuitions, key = { it.studentId }) { record ->
            val progress = if (record.totalFee > 0L) {
                (record.paidAmount.toFloat() / record.totalFee.toFloat()).coerceIn(0f, 1f)
            } else 1f
            val percentagePaid = (progress * 100f).roundToInt().coerceIn(0, 100)
            val formattedDueDate = formatAccountingDate(record.nextDueDate)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.22f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Student Name (Bold), Class & Parent below, and Golden/Amber Due Date Capsule on Left
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = record.studentName,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${record.gradeClass} (${record.sectionName}) • ولي الأمر: ${record.parentName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Golden / Amber Due Date Capsule Badge (or Fully Paid Emerald Capsule)
                        val isFullyPaid = record.remainingAmount <= 0L
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isFullyPaid) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                            border = BorderStroke(
                                1.dp,
                                if (isFullyPaid) Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFFF59E0B).copy(alpha = 0.65f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullyPaid) Icons.Filled.CheckCircle else Icons.Filled.EventRepeat,
                                    contentDescription = null,
                                    tint = if (isFullyPaid) Color(0xFF047857) else Color(0xFFB45309),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isFullyPaid) {
                                        "مسدد بالكامل"
                                    } else {
                                        "تاريخ الاستحقاق: $formattedDueDate"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isFullyPaid) Color(0xFF065F46) else Color(0xFF92400E),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Dual-Color Visual Progress Bar with Payment Percentage
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مؤشر إنجاز التسديد المالي",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF0D8A6A).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "تم تسديد $percentagePaid%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                        ) {
                            val barW = size.width
                            val barH = size.height
                            // Remaining track (warm amber/rose tint)
                            drawRoundRect(
                                color = Color(0xFFFEE2E2),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(barH / 2f)
                            )
                            // Paid segment (dual-color emerald & gold gradient)
                            val fillW = (barW * progress).coerceIn(0f, barW)
                            if (fillW > 0f) {
                                drawRoundRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF064E3B),
                                            Color(0xFF0D8A6A),
                                            Color(0xFFD97706)
                                        )
                                    ),
                                    size = Size(fillW, barH),
                                    cornerRadius = CornerRadius(barH / 2f)
                                )
                            }
                        }
                    }

                    // 3 Equal-Sized Adjacent Mini-Boxes for Amounts (Zero cramped text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Box 1: القسط الكلي (Dark bold font)
                        StudentAmountMiniBox(
                            title = "القسط الكلي",
                            formattedAmount = formatAccountingIqd(record.totalFee),
                            amountColor = Color(0xFF0F172A),
                            containerColor = Color(0xFFF8FAFC),
                            borderColor = Color(0xFFCBD5E1),
                            modifier = Modifier.weight(1f)
                        )
                        // Box 2: المبلغ الواصل (Emerald font)
                        StudentAmountMiniBox(
                            title = "المبلغ الواصل",
                            formattedAmount = formatAccountingIqd(record.paidAmount),
                            amountColor = Color(0xFF047857),
                            containerColor = Color(0xFFECFDF5),
                            borderColor = Color(0xFF6EE7B7),
                            modifier = Modifier.weight(1f)
                        )
                        // Box 3: المبلغ المتبقي (Refined Burgundy/Red font)
                        StudentAmountMiniBox(
                            title = "المبلغ المتبقي",
                            formattedAmount = formatAccountingIqd(record.remainingAmount),
                            amountColor = Color(0xFF9F1239),
                            containerColor = Color(0xFFFFF1F2),
                            borderColor = Color(0xFFFECDD3),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Digital Receipts Log: Official Receipt Ticket Cards
                    if (record.installments.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFF064E3B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "سجل الدفعات والوصولات الرقمية الرسمية (${record.installments.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF064E3B),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            record.installments.forEach { inst ->
                                OfficialReceiptTicketCard(
                                    installment = inst,
                                    onOpenReceiptPdf = {
                                        selectedReceiptPair = record to inst
                                    },
                                    onShareWhatsApp = {
                                        val msg = """
                                            وصل قبض قسط دراسي رقم (${inst.receiptNumber})
                                            الصادر من: ${schoolConfig.schoolName}
                                            • اسم الطالب: ${record.studentName} (${record.gradeClass})
                                            • المبلغ المستلم: ${formatAccountingIqd(inst.amount)}
                                            • تاريخ الدفعة: ${formatAccountingDate(inst.paymentDate)}
                                            • إجمالي المدفوع: ${formatAccountingIqd(record.paidAmount)}
                                            • المبلغ المتبقي: ${formatAccountingIqd(record.remainingAmount)}
                                            شكراً لتعاونكم الكريم.
                                        """.trimIndent()
                                        ExternalIntents.sendWhatsAppMessage(context, record.parentPhone, msg)
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Action Buttons: Complete & Unclipped ("تسجيل دفعة جديدة" & "إرسال إشعار ووصل")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canRecordPayment && record.remainingAmount > 0L) {
                            Button(
                                onClick = { selectedTuitionForPayment = record },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("add_payment_${record.studentId}"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF064E3B),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCard,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "تسجيل دفعة جديدة",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val latestReceipt = record.installments.firstOrNull()?.receiptNumber ?: "REC-1000"
                                val msg = """
                                    كشف ووصل القسط الدراسي ($latestReceipt)
                                    من إدارة (${schoolConfig.schoolName})
                                    • الطالب: ${record.studentName} (${record.gradeClass})
                                    • القسط السنوي الكلي: ${formatAccountingIqd(record.totalFee)}
                                    • مجموع الدفعات الواصلة: ${formatAccountingIqd(record.paidAmount)}
                                    • المبلغ المتبقي: ${formatAccountingIqd(record.remainingAmount)}
                                    • تاريخ الاستحقاق القادم: ${formatAccountingDate(record.nextDueDate)}
                                """.trimIndent()
                                ExternalIntents.sendWhatsAppMessage(context, record.parentPhone, msg)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D8A6A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "إرسال إشعار ووصل",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
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
                        val cleanReceiptAddr = ArabicTextNormalizer.cleanDefaultAddress(schoolConfig.address)
                        if (cleanReceiptAddr.isNotBlank()) {
                            Text(
                                text = "العنوان: $cleanReceiptAddr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = GoldAccent.copy(alpha = 0.4f))
                        Text("• اسم الطالب: ${tuitionRec.studentName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("• الصف والشعبة: ${tuitionRec.gradeClass} (${tuitionRec.sectionName})", style = MaterialTheme.typography.bodySmall)
                        Text("• ولي الأمر: ${tuitionRec.parentName}", style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(color = GoldAccent.copy(alpha = 0.4f))
                        Text(
                            text = "المبلغ المستلم في هذا الوصل: ${formatAccountingIqd(installment.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen
                        )
                        Text("• تاريخ الدفعة: ${formatAccountingDate(installment.paymentDate)}", style = MaterialTheme.typography.bodySmall)
                        Text("• إجمالي القسط السنوي: ${formatAccountingIqd(tuitionRec.totalFee)}", style = MaterialTheme.typography.bodySmall)
                        Text("• مجموع الواصل الكلي: ${formatAccountingIqd(tuitionRec.paidAmount)}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "• الرصيد المتبقي: ${formatAccountingIqd(tuitionRec.remainingAmount)}",
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
        var amountText by remember { mutableStateOf<String>("250000") }
        var paymentDate by remember { mutableStateOf<String>(ArabicCalendarDateHelper.todayIsoDate()) }
        var nextDue by remember { mutableStateOf<String>(rec.nextDueDate.ifBlank { ArabicCalendarDateHelper.todayIsoDate() }) }
        var note by remember { mutableStateOf<String>("دفعة قسط دراسي") }

        AlertDialog(
            onDismissRequest = { selectedTuitionForPayment = null },
            title = { Text("تسجيل دفعة قسط: ${rec.studentName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "المبلغ المتبقي الحالي: ${formatAccountingIqd(rec.remainingAmount)}",
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
                    ModernCalendarDatePickerField(
                        selectedDateIso = paymentDate,
                        label = "تاريخ الدفع",
                        onDateSelected = { selectedIso, _ -> paymentDate = selectedIso }
                    )
                    ModernCalendarDatePickerField(
                        selectedDateIso = nextDue,
                        label = "تاريخ الاستحقاق القادم",
                        onDateSelected = { selectedIso, _ -> nextDue = selectedIso }
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

/**
 * Royal Emerald Financial Hero Banner (#0D8A6A to #064E3B) with subtle dot grid, curved bottom wave,
 * two frosted glass pills ("الإدارة المالية للمدرسة" & "حسابات الأقساط والوصولات"),
 * title ("نظام الأقساط والحسابات المالية"), subtitle ("إدارة الدفعات، الوصولات الرقمية، ومستحقات الطلبة بدقة مصرفية"),
 * and on the left an animated Bezier vector Canvas depicting an emerald & gold digital vault/wallet, smart card, and pulsing financial indicator.
 */
@Composable
private fun EmeraldFinancialHeroBanner() {
    val bannerWaveShape = remember {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val bottomCurve = (h * 0.085f).coerceIn(14f, 26f)
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h - bottomCurve)
            quadraticBezierTo(
                w * 0.50f, h + bottomCurve * 0.85f,
                0f, h - bottomCurve * 0.75f
            )
            close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = bannerWaveShape,
                ambientColor = Color(0xFF064E3B).copy(alpha = 0.35f),
                spotColor = Color(0xFF064E3B).copy(alpha = 0.40f)
            )
            .clip(bannerWaveShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D8A6A),
                        Color(0xFF064E3B)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 600f)
                )
            )
    ) {
        // Halftone dot grid + subtle internal wave sheen
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val sheenWave = Path().apply {
                moveTo(0f, h * 0.70f)
                cubicTo(
                    w * 0.32f, h * 0.56f,
                    w * 0.68f, h * 0.88f,
                    w, h * 0.72f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = sheenWave,
                color = Color(0xFF042F24).copy(alpha = 0.25f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF34D399).copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.20f, h * 0.52f),
                    radius = w * 0.42f
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.20f, h * 0.52f)
            )

            val step = 11.dp.toPx()
            val dotRadius = 1.5.dp.toPx()
            for (row in 0..6) {
                for (col in 0..7) {
                    val alpha = ((1f - (row + col) / 14f) * 0.20f).coerceIn(0.03f, 0.20f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(w * 0.04f + col * step, h * 0.10f + row * step)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(w * 0.96f - col * step, h * 0.86f - row * step)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: 2 Frosted Glass Capsules (Right: الإدارة المالية للمدرسة, Left: حسابات الأقساط والوصولات)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "الإدارة المالية للمدرسة",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "حسابات الأقساط والوصولات",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Middle Row: Title & Subtitle on Right + Bezier Animated Digital Vault & Smart Card on Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "نظام الأقساط والحسابات المالية",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "إدارة الدفعات، الوصولات الرقمية، ومستحقات الطلبة بدقة مصرفية",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                AnimatedFinancialVaultBezierIllustration(
                    modifier = Modifier.size(106.dp)
                )
            }
        }
    }
}

/**
 * Self-contained Bezier Vector Animation depicting an Emerald & Gold Digital Vault/Wallet,
 * a Floating Smart Card with Golden Chip, and a Pulsing Financial Chart Indicator.
 */
@Composable
private fun AnimatedFinancialVaultBezierIllustration(
    modifier: Modifier = Modifier.size(106.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "financial_vault_bezier")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vault_float_phase"
    )
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault_pulse_factor"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.52f
        val bobY = sin(floatPhase) * (h * 0.032f)
        val cardBobY = cos(floatPhase) * (h * 0.038f)

        // 1. Radiant Golden/Emerald Halo behind the vault
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFDE68A).copy(alpha = 0.35f),
                    Color(0xFF10B981).copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = (w * 0.46f) * pulseFactor
            ),
            radius = (w * 0.46f) * pulseFactor,
            center = Offset(cx, cy)
        )

        // 2. Bezier Digital Vault / Luxury Wallet Body
        val vaultPath = Path().apply {
            moveTo(w * 0.14f, h * 0.40f + bobY)
            cubicTo(
                w * 0.14f, h * 0.28f + bobY,
                w * 0.26f, h * 0.26f + bobY,
                w * 0.50f, h * 0.26f + bobY
            )
            cubicTo(
                w * 0.74f, h * 0.26f + bobY,
                w * 0.86f, h * 0.28f + bobY,
                w * 0.86f, h * 0.40f + bobY
            )
            lineTo(w * 0.86f, h * 0.76f + bobY)
            cubicTo(
                w * 0.86f, h * 0.86f + bobY,
                w * 0.74f, h * 0.88f + bobY,
                w * 0.50f, h * 0.88f + bobY
            )
            cubicTo(
                w * 0.26f, h * 0.88f + bobY,
                w * 0.14f, h * 0.86f + bobY,
                w * 0.14f, h * 0.76f + bobY
            )
            close()
        }
        drawPath(
            path = vaultPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF042F24),
                    Color(0xFF065F46),
                    Color(0xFF0D8A6A)
                ),
                start = Offset(w * 0.14f, h * 0.26f),
                end = Offset(w * 0.86f, h * 0.88f)
            )
        )
        drawPath(
            path = vaultPath,
            color = Color(0xFFFBBF24),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // 3. Floating Golden & Emerald Smart Card emerging from the top of the vault
        val cardPath = Path().apply {
            val topY = h * 0.14f + cardBobY
            val bottomY = h * 0.48f + cardBobY
            moveTo(w * 0.24f, topY + h * 0.05f)
            cubicTo(
                w * 0.24f, topY,
                w * 0.32f, topY,
                w * 0.52f, topY
            )
            cubicTo(
                w * 0.72f, topY,
                w * 0.80f, topY,
                w * 0.80f, topY + h * 0.05f
            )
            lineTo(w * 0.80f, bottomY)
            lineTo(w * 0.24f, bottomY)
            close()
        }
        drawPath(
            path = cardPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFEF08A),
                    Color(0xFFF59E0B),
                    Color(0xFFD97706)
                ),
                start = Offset(w * 0.24f, h * 0.14f),
                end = Offset(w * 0.80f, h * 0.48f)
            )
        )
        // Smart Card EMV Chip
        drawRoundRect(
            color = Color(0xFF064E3B),
            topLeft = Offset(w * 0.31f, h * 0.23f + cardBobY),
            size = Size(w * 0.13f, h * 0.09f),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        // Smart Card contactless wave lines
        drawLine(
            color = Color(0xFF042F24).copy(alpha = 0.65f),
            start = Offset(w * 0.50f, h * 0.27f + cardBobY),
            end = Offset(w * 0.72f, h * 0.27f + cardBobY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 4. Vault Clasp & Pulsing Financial Indicator Curve on the Front
        val chartPath = Path().apply {
            moveTo(w * 0.23f, h * 0.72f + bobY)
            cubicTo(
                w * 0.36f, h * 0.68f + bobY,
                w * 0.44f, h * 0.54f + bobY,
                w * 0.56f, h * 0.60f + bobY
            )
            cubicTo(
                w * 0.64f, h * 0.64f + bobY,
                w * 0.70f, h * 0.50f + bobY,
                w * 0.77f, h * 0.48f + bobY
            )
        }
        drawPath(
            path = chartPath,
            color = Color(0xFF34D399),
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Pulsing Golden Indicator Node at the tip of the financial curve
        val nodeCenter = Offset(w * 0.77f, h * 0.48f + bobY)
        drawCircle(
            color = Color(0xFFFDE68A).copy(alpha = 0.45f),
            radius = 7.dp.toPx() * pulseFactor,
            center = nodeCenter
        )
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = 3.8.dp.toPx(),
            center = nodeCenter
        )
    }
}

@Composable
private fun FinancialStatChip(
    label: String,
    amount: String,
    color: Color,
    accentBarColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentBarColor.copy(alpha = 0.32f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentBarColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentBarColor)
            )
        }
    }
}

@Composable
private fun StudentAmountMiniBox(
    title: String,
    formattedAmount: String,
    amountColor: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OfficialReceiptTicketCard(
    installment: TuitionInstallment,
    onOpenReceiptPdf: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(17.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${installment.receiptNumber} • ${formatAccountingIqd(installment.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "تاريخ الدفعة: ${formatAccountingDate(installment.paymentDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        maxLines = 1
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpenReceiptPdf,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color(0xFF064E3B).copy(alpha = 0.30f)),
                    modifier = Modifier.testTag("view_digital_receipt_btn_${installment.receiptNumber}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFF064E3B),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "وصل PDF",
                            color = Color(0xFF064E3B),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    onClick = onShareWhatsApp,
                    shape = RoundedCornerShape(8.dp),
                    color = WhatsAppGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "مشاركة واتساب",
                            color = WhatsAppGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

