package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
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
 * Formats an Iraqi Dinar amount with standard thousand separators: e.g. 1500000 -> "1,500,000 د.ع"
 */
fun formatTuitionIqdAmount(amount: Long): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val formatter = DecimalFormat("#,###", symbols)
    return "${formatter.format(amount)} د.ع"
}

/**
 * Formats concatenated or ISO dates cleanly: e.g. "20261115" or "2026-11-15" -> "2026/11/15"
 */
fun formatTuitionDateDisplay(rawDate: String): String {
    val trimmed = rawDate.trim()
    if (trimmed.isBlank()) return "غير محدد"
    val digitsOnly = trimmed.filter { it.isDigit() }
    if (digitsOnly.length == 8) {
        val y = digitsOnly.substring(0, 4)
        val m = digitsOnly.substring(4, 6)
        val d = digitsOnly.substring(6, 8)
        return "$y/$m/$d"
    }
    return trimmed.replace("-", "/")
}

/**
 * Section 6: Luxury Royal Emerald Banking & Accounting Tuition Dashboard (نظام الأقساط والحسابات المالية)
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
    val isDarkMode by viewModel.isDarkMode.collectAsState()

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

    var selectedClassFilter by remember { mutableStateOf("جميع الصفوف") }
    var selectedSectionFilter by remember { mutableStateOf("الكل") }
    var selectedPaymentStatusFilter by remember { mutableStateOf("الجميع") }
    var tuitionSearchQuery by remember { mutableStateOf("") }

    val availableClasses = remember(schoolConfig.registeredClasses, visibleTuitions, students) {
        val rawClasses = (schoolConfig.registeredClasses + visibleTuitions.map { it.gradeClass } + students.map { it.gradeClass })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        listOf("جميع الصفوف") + rawClasses
    }

    val availableSections = remember(schoolConfig.registeredSections, visibleTuitions, students, selectedClassFilter) {
        val scopedForSections = if (selectedClassFilter == "جميع الصفوف") {
            visibleTuitions
        } else {
            visibleTuitions.filter { it.gradeClass.trim() == selectedClassFilter }
        }
        val rawSecs = (scopedForSections.map { it.sectionName } + schoolConfig.registeredSections)
            .map { it.trim().removePrefix("شعبة").trim().ifBlank { "أ" } }
            .distinct()
        listOf("الكل") + rawSecs
    }

    val paymentStatusOptions = remember {
        listOf("الجميع", "مكتمل التسديد", "متبقي بذمته", "مستحق الدفع")
    }

    val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }

    val filteredTuitions = remember(
        visibleTuitions,
        selectedClassFilter,
        selectedSectionFilter,
        selectedPaymentStatusFilter,
        tuitionSearchQuery,
        todayIso
    ) {
        val q = tuitionSearchQuery.trim()
        visibleTuitions.filter { rec ->
            val matchesClass = selectedClassFilter == "جميع الصفوف" || rec.gradeClass.trim() == selectedClassFilter
            val cleanSec = rec.sectionName.trim().removePrefix("شعبة").trim().ifBlank { "أ" }
            val matchesSection = selectedSectionFilter == "الكل" || cleanSec == selectedSectionFilter
            val matchesStatus = when (selectedPaymentStatusFilter) {
                "مكتمل التسديد" -> rec.remainingAmount <= 0L
                "متبقي بذمته" -> rec.remainingAmount > 0L
                "مستحق الدفع" -> rec.remainingAmount > 0L && (
                    rec.paidAmount == 0L ||
                        rec.nextDueDate.isBlank() ||
                        rec.nextDueDate <= todayIso
                    )
                else -> true
            }
            val matchesSearch = q.isEmpty() ||
                rec.studentName.contains(q, ignoreCase = true) ||
                rec.parentName.contains(q, ignoreCase = true) ||
                rec.gradeClass.contains(q, ignoreCase = true) ||
                rec.installments.any { inst ->
                    inst.receiptNumber.contains(q, ignoreCase = true) ||
                        inst.note.contains(q, ignoreCase = true)
                }
            matchesClass && matchesSection && matchesStatus && matchesSearch
        }
    }

    val groupedTuitions = remember(filteredTuitions) {
        filteredTuitions.groupBy { rec ->
            val cls = rec.gradeClass.trim().ifBlank { "صف غير محدد" }
            val sec = rec.sectionName.trim().removePrefix("شعبة").trim().ifBlank { "أ" }
            cls to sec
        }
    }

    val totalFeesAll = remember(filteredTuitions) { filteredTuitions.sumOf { it.totalFee } }
    val totalPaidAll = remember(filteredTuitions) { filteredTuitions.sumOf { it.paidAmount } }
    val totalRemainingAll = remember(filteredTuitions) { filteredTuitions.sumOf { it.remainingAmount } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 44.dp)
    ) {
        // 1) Royal Emerald Banking Header Banner (#0D8A6A to #064E3B) with Dot Matrix & Animated Bezier Vault/Wallet Vector
        item(key = "tuition_royal_emerald_hero_banner") {
            RoyalTuitionAccountingHeroBanner(
                schoolName = schoolConfig.schoolName,
                schoolTypeLabel = schoolConfig.schoolType.arabicLabel,
                isParentOrStudent = isParentOrStudent,
                totalFeesFormatted = formatTuitionIqdAmount(totalFeesAll),
                totalPaidFormatted = formatTuitionIqdAmount(totalPaidAll),
                totalRemainingFormatted = formatTuitionIqdAmount(totalRemainingAll),
                canExportStatement = canRecordPayment,
                onExportStatementPdf = {
                    val msg = DocumentAndIdExporter.exportTuitionStatementPdf(
                        context = context,
                        tuitions = filteredTuitions.ifEmpty { visibleTuitions },
                        schoolConfig = schoolConfig
                    )
                    viewModel.showBannerMessage(msg)
                }
            )
        }

        // 2) Global Class & Section Hierarchy Filter + Payment Status + Quick Search Card (Admin & Staff)
        if (!isParentOrStudent) {
            item(key = "tuition_hierarchy_filter_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tuition_hierarchy_filter_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(
                        1.2.dp,
                        if (isDarkMode) Color(0xFF334155) else Color(0xFFD1FAE5)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Filter Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "فلترة وفرز السجلات المالية (الصف • الشعبة • الحالة)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF064E3B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    text = "${filteredTuitions.size} طالب",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // A) Class Selection LazyRow (شريط اختيار الصف الدراسي)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = "الصف الدراسي:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(availableClasses) { clsOption ->
                                    val selected = selectedClassFilter == clsOption
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) {
                                            Color(0xFF047857)
                                        } else if (isDarkMode) {
                                            Color(0xFF0F172A)
                                        } else {
                                            Color(0xFFF1F5F9)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) Color(0xFFFDE68A) else Color(0xFFCBD5E1).copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                selectedClassFilter = clsOption
                                                selectedSectionFilter = "الكل"
                                            }
                                    ) {
                                        Text(
                                            text = clsOption,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (selected) {
                                                Color.White
                                            } else if (isDarkMode) {
                                                Color(0xFFE2E8F0)
                                            } else {
                                                Color(0xFF1E293B)
                                            },
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // B) Section Selection Chips (شريط اختيار الشعبة)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = "الشعبة الدراسية:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(availableSections) { secOption ->
                                    val selected = selectedSectionFilter == secOption
                                    val displayLabel = if (secOption == "الكل") "الكل" else "شعبة $secOption"
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) {
                                            Color(0xFF059669)
                                        } else if (isDarkMode) {
                                            Color(0xFF0F172A)
                                        } else {
                                            Color(0xFFF8FAFC)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) Color(0xFFA7F3D0) else Color(0xFFCBD5E1).copy(alpha = 0.65f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedSectionFilter = secOption }
                                    ) {
                                        Text(
                                            text = displayLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (selected) {
                                                Color.White
                                            } else if (isDarkMode) {
                                                Color(0xFFCBD5E1)
                                            } else {
                                                Color(0xFF334155)
                                            },
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // C) Payment Status Filter Bar (فلتر حالة الدفع)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = "حالة تسديد القسط:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(paymentStatusOptions) { statusOpt ->
                                    val selected = selectedPaymentStatusFilter == statusOpt
                                    val activeBg = when (statusOpt) {
                                        "مكتمل التسديد" -> Color(0xFF059669)
                                        "متبقي بذمته" -> Color(0xFFD97706)
                                        "مستحق الدفع" -> DangerRed
                                        else -> Color(0xFF0F3D5E)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) {
                                            activeBg
                                        } else if (isDarkMode) {
                                            Color(0xFF0F172A)
                                        } else {
                                            Color(0xFFF1F5F9)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) Color.White.copy(alpha = 0.5f) else Color(0xFFCBD5E1).copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { selectedPaymentStatusFilter = statusOpt }
                                    ) {
                                        Text(
                                            text = statusOpt,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (selected) {
                                                Color.White
                                            } else if (isDarkMode) {
                                                Color(0xFFE2E8F0)
                                            } else {
                                                Color(0xFF1E293B)
                                            },
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // D) Quick Search Input (حقل بحث سريع باسم الطالب أو ولي الأمر أو رقم الوصل)
                        OutlinedTextField(
                            value = tuitionSearchQuery,
                            onValueChange = { tuitionSearchQuery = it },
                            placeholder = {
                                Text(
                                    text = "ابحث باسم الطالب أو اسم ولي الأمر أو رقم الوصل (مثال: REC-1001)...",
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF059669)
                                )
                            },
                            trailingIcon = {
                                if (tuitionSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { tuitionSearchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "مسح البحث"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tuition_quick_search_input")
                        )
                    }
                }
            }
        }

        // Empty state when filter matches 0 records
        if (filteredTuitions.isEmpty()) {
            item(key = "tuition_empty_filter_state") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "لا توجد سجلات أقساط مطابقة لخيارات الفلترة أو البحث الحالية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = if (isDarkMode) Color.White else Color(0xFF064E3B)
                        )
                    }
                }
            }
        }

        // 3) Grouped Student Banking Tuition Cards by Class & Section
        groupedTuitions.forEach { (classSectionPair, groupRecords) ->
            val (groupClass, groupSection) = classSectionPair

            if (!isParentOrStudent) {
                item(key = "tuition_group_header_${groupClass}_${groupSection}") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Class,
                                    contentDescription = null,
                                    tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "$groupClass - شعبة ($groupSection) • العدد: ${groupRecords.size} طالب",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF064E3B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val completedInGroup = groupRecords.count { it.remainingAmount <= 0L }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isDarkMode) Color(0xFF047857) else Color.White,
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Text(
                                    text = "مسدد: $completedInGroup/${groupRecords.size}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF047857),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            items(groupRecords, key = { it.studentId }) { record ->
                val progress = if (record.totalFee > 0) {
                    (record.paidAmount.toFloat() / record.totalFee.toFloat()).coerceIn(0f, 1f)
                } else 1f
                val formattedDueDate = formatTuitionDateDisplay(record.nextDueDate)
                val isFullyPaid = record.remainingAmount <= 0L

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (isFullyPaid) {
                            Color(0xFF10B981).copy(alpha = 0.45f)
                        } else if (isDarkMode) {
                            Color(0xFF334155)
                        } else {
                            Color(0xFFD1FAE5)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Student Header Row + Formatted Due Date Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = record.studentName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${record.gradeClass} (${record.sectionName}) • ولي الأمر: ${record.parentName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            NoWrapBadge(
                                text = if (isFullyPaid) "مسدد بالكامل" else "استحقاق: $formattedDueDate",
                                icon = if (isFullyPaid) Icons.Filled.CheckCircle else Icons.Filled.EventRepeat,
                                containerColor = if (isFullyPaid) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                                contentColor = if (isFullyPaid) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                        }

                        // Smooth Progress Bar with Percentage Pill
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "نسبة تسديد القسط السنوي",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = Color(0xFF059669),
                                trackColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )
                        }

                        // 3 Adjacent Mini-Boxes for Amounts (القسط الكلي • المبلغ الواصل • المبلغ المتبقي)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudentMiniAmountBox(
                                label = "القسط الكلي",
                                amountText = formatTuitionIqdAmount(record.totalFee),
                                accentColor = Color(0xFF0F3D5E),
                                containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                borderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1),
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f)
                            )
                            StudentMiniAmountBox(
                                label = "المبلغ الواصل",
                                amountText = formatTuitionIqdAmount(record.paidAmount),
                                accentColor = Color(0xFF059669),
                                containerColor = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5),
                                borderColor = Color(0xFF10B981).copy(alpha = 0.45f),
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f)
                            )
                            StudentMiniAmountBox(
                                label = "المبلغ المتبقي",
                                amountText = formatTuitionIqdAmount(record.remainingAmount),
                                accentColor = if (isFullyPaid) Color(0xFF059669) else DangerRed,
                                containerColor = if (isFullyPaid) {
                                    if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
                                } else {
                                    if (isDarkMode) Color(0xFF450A0A).copy(alpha = 0.45f) else Color(0xFFFEF2F2)
                                },
                                borderColor = if (isFullyPaid) {
                                    Color(0xFF10B981).copy(alpha = 0.45f)
                                } else {
                                    DangerRed.copy(alpha = 0.35f)
                                },
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Clean Receipt Cards for Installments Log (Zero Number or Date Wrapping)
                        if (record.installments.isNotEmpty()) {
                            Surface(
                                color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "سجل الدفعات والوصولات الرقمية الرسمية (${record.installments.size}):",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    record.installments.forEach { inst ->
                                        val formattedInstAmount = formatTuitionIqdAmount(inst.amount)
                                        val formattedInstDate = formatTuitionDateDisplay(inst.paymentDate)

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isDarkMode) Color(0xFF334155) else Color(0xFFD1FAE5)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Top Full-Width Row: Receipt Number + Full Date + Unbroken Single-Line Amount
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFECFDF5),
                                                            border = BorderStroke(0.8.dp, Color(0xFFA7F3D0))
                                                        ) {
                                                            Text(
                                                                text = inst.receiptNumber,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFF047857),
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                                                        ) {
                                                            Text(
                                                                text = "التاريخ: $formattedInstDate",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = formattedInstAmount,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF059669),
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                // Bottom Balanced Row: Note on Right + Action Buttons ("وصل PDF" & "واتساب") on Left
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = inst.note.ifBlank { "دفعة قسط دراسي" },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(end = 8.dp)
                                                    )

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xFFECFDF5),
                                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                                            modifier = Modifier
                                                                .clickable { selectedReceiptPair = record to inst }
                                                                .testTag("view_digital_receipt_btn_${inst.receiptNumber}")
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.ReceiptLong,
                                                                    contentDescription = null,
                                                                    tint = DeepEmerald,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                                Text(
                                                                    text = "وصل PDF",
                                                                    color = DeepEmerald,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    maxLines = 1,
                                                                    softWrap = false
                                                                )
                                                            }
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = WhatsAppGreen.copy(alpha = 0.12f),
                                                            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.4f)),
                                                            modifier = Modifier.clickable {
                                                                val msg = """
                                                                    وصل قبض قسط دراسي رقم (${inst.receiptNumber})
                                                                    الصادر من: ${schoolConfig.schoolName}
                                                                    • اسم الطالب: ${record.studentName} (${record.gradeClass})
                                                                    • المبلغ المستلم: ${formatTuitionIqdAmount(inst.amount)}
                                                                    • تاريخ الدفعة: ${formatTuitionDateDisplay(inst.paymentDate)}
                                                                    • إجمالي المدفوع: ${formatTuitionIqdAmount(record.paidAmount)}
                                                                    • المبلغ المتبقي: ${formatTuitionIqdAmount(record.remainingAmount)}
                                                                    شكراً لتعاونكم الكريم.
                                                                """.trimIndent()
                                                                ExternalIntents.sendWhatsAppMessage(context, record.parentPhone, msg)
                                                            }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Chat,
                                                                    contentDescription = null,
                                                                    tint = WhatsAppGreen,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                                Text(
                                                                    text = "واتساب",
                                                                    color = WhatsAppGreen,
                                                                    fontSize = 11.sp,
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
                                    }
                                }
                            }
                        }

                        // Bottom Action Buttons with Full Text and Zero Clipping:
                        // "تسجيل دفعة جديدة" and "إرسال إشعار ووصل"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canRecordPayment && record.remainingAmount > 0L) {
                                Button(
                                    onClick = { selectedTuitionForPayment = record },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp)
                                        .testTag("add_payment_${record.studentId}"),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF047857),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "تسجيل دفعة جديدة",
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
                                        • القسط السنوي الكلي: ${formatTuitionIqdAmount(record.totalFee)}
                                        • مجموع الدفعات الواصلة: ${formatTuitionIqdAmount(record.paidAmount)}
                                        • المبلغ المتبقي: ${formatTuitionIqdAmount(record.remainingAmount)}
                                        • تاريخ الاستحقاق القادم: ${formatTuitionDateDisplay(record.nextDueDate)}
                                    """.trimIndent()
                                    ExternalIntents.sendWhatsAppMessage(context, record.parentPhone, msg)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppGreen,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "إرسال إشعار ووصل",
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
                            text = "المبلغ المستلم في هذا الوصل: ${formatTuitionIqdAmount(installment.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen
                        )
                        Text("• تاريخ الدفعة: ${formatTuitionDateDisplay(installment.paymentDate)}", style = MaterialTheme.typography.bodySmall)
                        Text("• إجمالي القسط السنوي: ${formatTuitionIqdAmount(tuitionRec.totalFee)}", style = MaterialTheme.typography.bodySmall)
                        Text("• مجموع الواصل الكلي: ${formatTuitionIqdAmount(tuitionRec.paidAmount)}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "• الرصيد المتبقي: ${formatTuitionIqdAmount(tuitionRec.remainingAmount)}",
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
        var paymentDate by remember { mutableStateOf(ArabicCalendarDateHelper.todayIsoDate()) }
        var nextDue by remember { mutableStateOf(rec.nextDueDate.ifBlank { ArabicCalendarDateHelper.todayIsoDate() }) }
        var note by remember { mutableStateOf("دفعة قسط دراسي") }

        AlertDialog(
            onDismissRequest = { selectedTuitionForPayment = null },
            title = { Text("تسجيل دفعة قسط: ${rec.studentName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "المبلغ المتبقي الحالي: ${formatTuitionIqdAmount(rec.remainingAmount)}",
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
 * Royal Emerald Gradient Header Banner (#0D8A6A to #064E3B) for the Tuition & Accounting Screen:
 * - Halftone dot matrix grid & flowing wave highlight
 * - Two top frosted capsules: "الإدارة المالية للمدرسة" & "حسابات الأقساط والوصولات"
 * - White main title: "نظام الأقساط والحسابات المالية"
 * - Left side animated Bezier vector Canvas representing a digital emerald & gold vault/wallet with pulsing financial indicator
 * - 3 glassmorphic summary stat boxes + official PDF statement button
 */
@Composable
private fun RoyalTuitionAccountingHeroBanner(
    schoolName: String,
    schoolTypeLabel: String,
    isParentOrStudent: Boolean,
    totalFeesFormatted: String,
    totalPaidFormatted: String,
    totalRemainingFormatted: String,
    canExportStatement: Boolean,
    onExportStatementPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val waveHeroShape = remember {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val topWaveDepth = (h * 0.025f).coerceIn(4f, 10f)
            val bottomWaveDepth = (h * 0.055f).coerceIn(10f, 20f)

            moveTo(0f, topWaveDepth * 0.45f)
            cubicTo(
                w * 0.32f, -topWaveDepth * 0.25f,
                w * 0.68f, topWaveDepth * 1.15f,
                w, topWaveDepth * 0.25f
            )
            lineTo(w, h - bottomWaveDepth)
            quadraticBezierTo(
                w * 0.50f, h + bottomWaveDepth * 0.85f,
                0f, h - bottomWaveDepth * 0.85f
            )
            close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = waveHeroShape,
                ambientColor = Color(0xFF064E3B).copy(alpha = 0.35f),
                spotColor = Color(0xFF0D8A6A).copy(alpha = 0.40f)
            )
            .clip(waveHeroShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D8A6A),
                        Color(0xFF047857),
                        Color(0xFF064E3B)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(980f, 720f)
                )
            )
    ) {
        // Halftone Dot Matrix & Ambient Glow Canvas
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val innerWave = Path().apply {
                moveTo(0f, h * 0.70f)
                cubicTo(
                    w * 0.35f, h * 0.58f,
                    w * 0.70f, h * 0.86f,
                    w, h * 0.72f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = innerWave,
                color = Color(0xFF022C22).copy(alpha = 0.28f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF34D399).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.20f, h * 0.42f),
                    radius = w * 0.42f
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.20f, h * 0.42f)
            )

            val step = 11.dp.toPx()
            val dotRadius = 1.6.dp.toPx()
            for (row in 0..6) {
                for (col in 0..7) {
                    val alpha = ((1f - (row + col) / 14f) * 0.22f).coerceIn(0.03f, 0.22f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(w * 0.04f + col * step, h * 0.08f + row * step)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(w * 0.96f - col * step, h * 0.88f - row * step)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Two Frosted Glass Capsules ("الإدارة المالية للمدرسة" & "حسابات الأقساط والوصولات")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f, fill = false),
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.01f))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
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

            // Middle Row: White Title & Subtitle on Right + Animated Bezier Digital Vault/Wallet on Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "نظام الأقساط والحسابات المالية",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = if (isParentOrStudent) {
                            "كشف الأقساط الدراسية والوصولات الرقمية المعتمدة • $schoolName"
                        } else {
                            "لوحة المحاسبة المالية الموحدة ومتابعة الدفعات والوصولات • $schoolName ($schoolTypeLabel)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                AnimatedEmeraldGoldVaultWalletCanvas(
                    modifier = Modifier.size(102.dp)
                )
            }

            // Summary Row: 3 Adjacent Banking Stat Boxes inside the Emerald Hero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinancialStatChip(
                    label = "القسط الكلي",
                    amount = totalFeesFormatted,
                    color = Color.White,
                    badgeDotColor = Color(0xFF93C5FD),
                    modifier = Modifier.weight(1f)
                )
                FinancialStatChip(
                    label = "المبلغ الواصل",
                    amount = totalPaidFormatted,
                    color = Color(0xFFA7F3D0),
                    badgeDotColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
                FinancialStatChip(
                    label = "المبلغ المتبقي",
                    amount = totalRemainingFormatted,
                    color = Color(0xFFFDE68A),
                    badgeDotColor = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
            }

            if (canExportStatement) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onExportStatementPdf() }
                        .testTag("export_tuition_report_pdf_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تصدير كشف الأقساط المالي الرسمي (PDF)",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
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
}

/**
 * Animated Bezier Vector Canvas representing an Emerald & Gold Digital Vault / Wallet
 * with a pulsing financial trend indicator on the left side of the Tuition Hero Banner.
 */
@Composable
private fun AnimatedEmeraldGoldVaultWalletCanvas(
    modifier: Modifier = Modifier.size(102.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emerald_gold_vault_wallet_anim")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vault_float_phase"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "financial_indicator_pulse"
    )

    Canvas(modifier = modifier.testTag("tuition_vault_wallet_vector_canvas")) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.52f
        val floatY = sin(floatPhase) * (h * 0.03f)

        // 1. Ambient Emerald & Gold Halo Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFDE68A).copy(alpha = 0.34f),
                    Color(0xFF34D399).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(cx, cy + floatY),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(cx, cy + floatY)
        )

        // 2. Floating Golden Coin / Card emerging from the top of the digital wallet
        val coinCenterY = h * 0.24f + floatY * 1.3f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309)),
                center = Offset(cx - w * 0.06f, coinCenterY - h * 0.02f),
                radius = w * 0.16f
            ),
            radius = w * 0.13f,
            center = Offset(cx - w * 0.04f, coinCenterY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = w * 0.095f,
            center = Offset(cx - w * 0.04f, coinCenterY),
            style = Stroke(width = 1.3.dp.toPx())
        )

        // 3. Emerald & Gold Bezier Digital Vault / Wallet Body
        val walletLeft = w * 0.14f
        val walletRight = w * 0.86f
        val walletTop = h * 0.31f + floatY
        val walletBottom = h * 0.80f + floatY

        val walletBackFlap = Path().apply {
            moveTo(walletLeft + w * 0.04f, walletTop)
            cubicTo(
                w * 0.36f, walletTop - h * 0.09f,
                w * 0.64f, walletTop - h * 0.09f,
                walletRight - w * 0.04f, walletTop
            )
            lineTo(walletRight - w * 0.04f, walletTop + h * 0.12f)
            lineTo(walletLeft + w * 0.04f, walletTop + h * 0.12f)
            close()
        }
        drawPath(
            path = walletBackFlap,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF065F46), Color(0xFF047857))
            )
        )

        // Main Sculpted Bezier Vault Body
        val vaultBodyPath = Path().apply {
            moveTo(walletLeft, walletTop + h * 0.06f)
            quadraticBezierTo(walletLeft, walletTop, walletLeft + w * 0.08f, walletTop)
            lineTo(walletRight - w * 0.08f, walletTop)
            quadraticBezierTo(walletRight, walletTop, walletRight, walletTop + h * 0.06f)
            lineTo(walletRight, walletBottom - h * 0.06f)
            quadraticBezierTo(walletRight, walletBottom, walletRight - w * 0.08f, walletBottom)
            lineTo(walletLeft + w * 0.08f, walletBottom)
            quadraticBezierTo(walletLeft, walletBottom, walletLeft, walletBottom - h * 0.06f)
            close()
        }
        drawPath(
            path = vaultBodyPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF10B981),
                    Color(0xFF059669),
                    Color(0xFF064E3B)
                ),
                start = Offset(walletLeft, walletTop),
                end = Offset(walletRight, walletBottom)
            )
        )
        drawPath(
            path = vaultBodyPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFA7F3D0))
            ),
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        )

        // 4. Golden Vault Clasp & Dial on Right Side of Wallet
        val claspTop = (walletTop + walletBottom) * 0.5f - h * 0.08f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFD97706))
            ),
            topLeft = Offset(walletRight - w * 0.22f, claspTop),
            size = Size(w * 0.25f, h * 0.16f),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF064E3B),
            radius = w * 0.038f,
            center = Offset(walletRight - w * 0.12f, claspTop + h * 0.08f)
        )

        // 5. Pulsing Financial Bezier Indicator Curve + Glowing Node Inside Vault
        val chartStartX = walletLeft + w * 0.09f
        val chartEndX = walletRight - w * 0.26f
        val chartBaseY = walletBottom - h * 0.10f
        val chartPeakY = walletTop + h * 0.13f

        val indicatorPath = Path().apply {
            moveTo(chartStartX, chartBaseY)
            cubicTo(
                chartStartX + (chartEndX - chartStartX) * 0.35f,
                chartBaseY - h * 0.16f,
                chartStartX + (chartEndX - chartStartX) * 0.58f,
                chartBaseY - h * 0.04f,
                chartEndX,
                chartPeakY
            )
        }
        drawPath(
            path = indicatorPath,
            color = Color(0xFFFEF08A),
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Pulsing financial beacon at the tip of the rising curve
        drawCircle(
            color = Color(0xFFFEF08A).copy(alpha = 0.38f),
            radius = w * 0.068f * pulseScale,
            center = Offset(chartEndX, chartPeakY)
        )
        drawCircle(
            color = Color(0xFF34D399),
            radius = w * 0.036f,
            center = Offset(chartEndX, chartPeakY)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.018f,
            center = Offset(chartEndX, chartPeakY)
        )
    }
}

@Composable
private fun StudentMiniAmountBox(
    label: String,
    amountText: String,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode && accentColor == Color(0xFF0F3D5E)) Color.White else accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinancialStatChip(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier,
    badgeDotColor: Color = color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = badgeDotColor)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.90f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.5.sp,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
