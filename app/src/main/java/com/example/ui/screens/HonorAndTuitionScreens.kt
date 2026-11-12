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
 * دالة مساعدة لتنسيق المبالغ المالية محاسبياً بفواصل الآلاف (مثال: 1,500,000 د.ع بدلاً من 1500000 د.ع)
 */
fun formatAccountingAmount(amount: Long): String {
    val formatted = String.format(Locale.US, "%,d", amount)
    return "$formatted د.ع"
}

/**
 * دالة مساعدة لتنسيق التواريخ المحاسبية وإصلاح التواريخ الملتصقة (مثال: 20261115 أو 2026-11-15 ⬅️ 2026/11/15)
 */
fun formatAccountingDate(rawDate: String): String {
    val trimmed = rawDate.trim()
    if (trimmed.isBlank()) return "غير محدد"
    val digitsOnly = trimmed.filter { it.isDigit() }
    if (digitsOnly.length == 8) {
        val year = digitsOnly.substring(0, 4)
        val month = digitsOnly.substring(4, 6)
        val day = digitsOnly.substring(6, 8)
        return "$year/$month/$day"
    }
    val dateRegex = Regex("""(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})""")
    val match = dateRegex.find(trimmed)
    if (match != null) {
        val year = match.groupValues[1]
        val month = match.groupValues[2].padStart(2, '0')
        val day = match.groupValues[3].padStart(2, '0')
        return "$year/$month/$day"
    }
    return trimmed.replace("-", "/")
}

/**
 * Section 6: Premium Financial & Tuition Hub (نظام الأقساط والحسابات المالية)
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
        // 1) البنر الترويسي الزمردي الملكي (Emerald Financial Hero Banner)
        item(key = "emerald_financial_hero_banner") {
            EmeraldFinancialHeroBanner()
        }

        // 3 بطاقات إحصائية مصرفية فخمة + زر تصدير كشف الأقساط المالي الرسمي PDF
        item(key = "banking_kpi_summary_section") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.2.dp, Color(0xFFA7F3D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BankingKpiStatCard(
                            label = "إجمالي الأقساط",
                            formattedAmount = formatAccountingAmount(totalFeesAll),
                            accentColor = Color(0xFF0F172A),
                            containerColor = Color(0xFFF8FAFC),
                            borderColor = Color(0xFFCBD5E1),
                            icon = Icons.Filled.AccountBalance,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        BankingKpiStatCard(
                            label = "مجموع الواصل",
                            formattedAmount = formatAccountingAmount(totalPaidAll),
                            accentColor = Color(0xFF047857),
                            containerColor = Color(0xFFECFDF5),
                            borderColor = Color(0xFF6EE7B7),
                            icon = Icons.Filled.Verified,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        BankingKpiStatCard(
                            label = "إجمالي المتبقي",
                            formattedAmount = formatAccountingAmount(totalRemainingAll),
                            accentColor = Color(0xFF9F1239),
                            containerColor = Color(0xFFFFF1F2),
                            borderColor = Color(0xFFFECDD3),
                            icon = Icons.Filled.AccountBalanceWallet,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    if (canRecordPayment) {
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
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تصدير كشف الأقساط المالي الرسمي PDF",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3) بطاقات الأقساط المالية للطلبة (Student Tuition Cards)
        items(visibleTuitions, key = { it.studentId }) { record ->
            StudentTuitionBankingCard(
                record = record,
                schoolName = schoolConfig.schoolName,
                canRecordPayment = canRecordPayment,
                onRecordNewPayment = { selectedTuitionForPayment = record },
                onOpenDigitalReceipt = { inst -> selectedReceiptPair = record to inst },
                onSendReceiptWhatsApp = { inst ->
                    val formattedInstAmt = formatAccountingAmount(inst.amount)
                    val formattedInstDate = formatAccountingDate(inst.paymentDate)
                    val formattedPaid = formatAccountingAmount(record.paidAmount)
                    val formattedRem = formatAccountingAmount(record.remainingAmount)
                    val msg = """
                        وصل قبض قسط دراسي رقم (${inst.receiptNumber})
                        الصادر من: ${schoolConfig.schoolName}
                        • اسم الطالب: ${record.studentName} (${record.gradeClass})
                        • المبلغ المستلم: $formattedInstAmt
                        • تاريخ الدفعة: $formattedInstDate
                        • إجمالي المدفوع: $formattedPaid
                        • المبلغ المتبقي: $formattedRem
                        شكراً لتعاونكم الكريم.
                    """.trimIndent()
                    ExternalIntents.sendWhatsAppMessage(context, record.parentPhone, msg)
                },
                onSendStatementWhatsApp = {
                    val latestReceipt = record.installments.firstOrNull()?.receiptNumber ?: "REC-1000"
                    val formattedTotal = formatAccountingAmount(record.totalFee)
                    val formattedPaid = formatAccountingAmount(record.paidAmount)
                    val formattedRem = formatAccountingAmount(record.remainingAmount)
                    val formattedDue = formatAccountingDate(record.nextDueDate)
                    val msg = """
                        كشف ووصل القسط الدراسي ($latestReceipt)
                        من إدارة (${schoolConfig.schoolName})
                        • الطالب: ${record.studentName} (${record.gradeClass})
                        • القسط السنوي الكلي: $formattedTotal
                        • مجموع الدفعات الواصلة: $formattedPaid
                        • المبلغ المتبقي: $formattedRem
                        • تاريخ الاستحقاق القادم: $formattedDue
                    """.trimIndent()
                    ExternalIntents.sendWhatsAppMessage(context, record.parentPhone, msg)
                }
            )
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
                            text = "المبلغ المستلم في هذا الوصل: ${formatAccountingAmount(installment.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen
                        )
                        Text("• تاريخ الدفعة: ${formatAccountingDate(installment.paymentDate)}", style = MaterialTheme.typography.bodySmall)
                        Text("• إجمالي القسط السنوي: ${formatAccountingAmount(tuitionRec.totalFee)}", style = MaterialTheme.typography.bodySmall)
                        Text("• مجموع الواصل الكلي: ${formatAccountingAmount(tuitionRec.paidAmount)}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "• الرصيد المتبقي: ${formatAccountingAmount(tuitionRec.remainingAmount)}",
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
                        text = "المبلغ المتبقي الحالي: ${formatAccountingAmount(rec.remainingAmount)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9F1239)
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
                                paymentDate = formatAccountingDate(paymentDate),
                                note = note,
                                nextDueDate = formatAccountingDate(nextDue)
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
 * 1) البنر الترويسي الزمردي الملكي (Emerald Financial Hero Banner):
 * - تدرج زمردي ملكي (#0D8A6A إلى #064E3B) مع الشبكة النقطية الخفيفة والمنحنى السفلي.
 * - كبسولتان زجاجيتان في الأعلى: يميناً (الإدارة المالية للمدرسة) ويساراً (حسابات الأقساط والوصولات).
 * - العنوان الأبيض الكبير يميناً: نظام الأقساط والحسابات المالية
 *   وتحته: إدارة الدفعات، الوصولات الرقمية، ومستحقات الطلبة بدقة مصرفية
 * - في يسار البنر: رسم فيكتور متحرك بتقنية بيزيير (Canvas) يمثل محفظة/خزينة رقمية زمردية وذهبية مع بطاقة ذكية ومؤشر مالي نابض.
 */
@Composable
private fun EmeraldFinancialHeroBanner(
    modifier: Modifier = Modifier
) {
    val bannerWaveShape = remember {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val bottomWaveDepth = (h * 0.095f).coerceIn(16f, 30f)

            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h - bottomWaveDepth)
            quadraticBezierTo(
                w * 0.50f,
                h + bottomWaveDepth * 0.85f,
                0f,
                h - bottomWaveDepth * 0.85f
            )
            close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = bannerWaveShape,
                ambientColor = Color(0xFF064E3B).copy(alpha = 0.35f),
                spotColor = Color(0xFF064E3B).copy(alpha = 0.42f)
            )
            .clip(bannerWaveShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0D8A6A),
                        Color(0xFF064E3B)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(950f, 650f)
                )
            )
    ) {
        // الشبكة النقطية الخفيفة والتموج الداخلي
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val innerWave = Path().apply {
                moveTo(0f, h * 0.70f)
                cubicTo(
                    w * 0.35f, h * 0.58f,
                    w * 0.70f, h * 0.88f,
                    w, h * 0.72f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = innerWave,
                color = Color(0xFF042F24).copy(alpha = 0.30f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFDE68A).copy(alpha = 0.22f),
                        Color(0xFF34D399).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.18f, h * 0.54f),
                    radius = w * 0.42f
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.18f, h * 0.54f)
            )

            val step = 12.dp.toPx()
            val dotRadius = 1.5.dp.toPx()
            for (row in 0..7) {
                for (col in 0..9) {
                    val alpha = ((1f - (row + col) / 16f) * 0.20f).coerceIn(0.04f, 0.20f)
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
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // كبسولتان زجاجيتان في الأعلى: يميناً (الإدارة المالية للمدرسة) ويساراً (حسابات الأقساط والوصولات)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            // العنوان والوصف يميناً + الرسم الفيكتوري المتحرك بتقنية بيزيير يساراً
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
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 19.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                EmeraldFinancialVaultVectorAnimation(
                    modifier = Modifier.size(104.dp)
                )
            }
        }
    }
}

/**
 * رسم فيكتور متحرك بتقنية بيزيير (Canvas) يمثل محفظة/خزينة رقمية زمردية وذهبية مع بطاقة ذكية ومؤشر مالي نابض.
 */
@Composable
private fun EmeraldFinancialVaultVectorAnimation(
    modifier: Modifier = Modifier.size(104.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emerald_financial_vault_anim")
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
        initialValue = 0.86f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "financial_indicator_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.54f
        val floatY = sin(floatPhase) * (h * 0.03f)
        val cardFloatY = cos(floatPhase) * (h * 0.035f)

        // هالة زمردية وذهبية خلف الخزينة الرقمية
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFDE68A).copy(alpha = 0.36f),
                    Color(0xFF10B981).copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = w * 0.48f * pulseScale
            ),
            radius = w * 0.48f * pulseScale,
            center = Offset(cx, cy)
        )

        // 1. البطاقة الذكية الذهبية المنبثقة أعلى الخزينة (Smart Chip Card) بمنحنيات بيزيير
        val cardLeft = w * 0.18f
        val cardRight = w * 0.84f
        val cardTop = h * 0.13f + cardFloatY
        val cardBottom = h * 0.50f + cardFloatY

        val smartCardPath = Path().apply {
            moveTo(cardLeft + w * 0.06f, cardTop)
            quadraticBezierTo(cardRight * 0.55f, cardTop - h * 0.03f, cardRight - w * 0.04f, cardTop + h * 0.03f)
            quadraticBezierTo(cardRight, cardTop + h * 0.04f, cardRight, cardTop + h * 0.10f)
            lineTo(cardRight, cardBottom)
            lineTo(cardLeft, cardBottom)
            lineTo(cardLeft, cardTop + h * 0.06f)
            quadraticBezierTo(cardLeft, cardTop, cardLeft + w * 0.06f, cardTop)
            close()
        }
        drawPath(
            path = smartCardPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706)),
                start = Offset(cardLeft, cardTop),
                end = Offset(cardRight, cardBottom)
            )
        )
        drawPath(
            path = smartCardPath,
            color = Color.White.copy(alpha = 0.70f),
            style = Stroke(width = 1.4.dp.toPx(), join = StrokeJoin.Round)
        )

        // شريحة البطاقة الذكية (EMV Smart Chip)
        drawRoundRect(
            color = Color(0xFFFEF3C7),
            topLeft = Offset(cardLeft + w * 0.08f, cardTop + h * 0.08f),
            size = Size(w * 0.14f, h * 0.10f),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF92400E),
            topLeft = Offset(cardLeft + w * 0.08f, cardTop + h * 0.08f),
            size = Size(w * 0.14f, h * 0.10f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        // 2. جسم المحفظة / الخزينة الزمردية الملكية بمنحنيات بيزيير (Bezier Digital Vault Body)
        val vaultLeft = w * 0.10f
        val vaultRight = w * 0.90f
        val vaultTop = h * 0.35f + floatY
        val vaultBottom = h * 0.86f + floatY

        val vaultBodyPath = Path().apply {
            moveTo(vaultLeft, vaultTop + h * 0.07f)
            quadraticBezierTo(vaultLeft, vaultTop, vaultLeft + w * 0.08f, vaultTop)
            cubicTo(
                w * 0.36f, vaultTop + h * 0.02f,
                w * 0.64f, vaultTop - h * 0.02f,
                vaultRight - w * 0.08f, vaultTop
            )
            quadraticBezierTo(vaultRight, vaultTop, vaultRight, vaultTop + h * 0.07f)
            lineTo(vaultRight, vaultBottom - h * 0.07f)
            quadraticBezierTo(vaultRight, vaultBottom, vaultRight - w * 0.08f, vaultBottom)
            lineTo(vaultLeft + w * 0.08f, vaultBottom)
            quadraticBezierTo(vaultLeft, vaultBottom, vaultLeft, vaultBottom - h * 0.07f)
            close()
        }
        drawPath(
            path = vaultBodyPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF042F24),
                    Color(0xFF065F46),
                    Color(0xFF0D8A6A)
                ),
                start = Offset(vaultLeft, vaultTop),
                end = Offset(vaultRight, vaultBottom)
            )
        )
        drawPath(
            path = vaultBodyPath,
            color = Color(0xFFFDE68A),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // قفل الخزينة الذهبي الجانبي (Golden Vault Clasp)
        val claspTop = vaultTop + (vaultBottom - vaultTop) * 0.30f
        val claspHeight = (vaultBottom - vaultTop) * 0.40f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
            ),
            topLeft = Offset(vaultRight - w * 0.22f, claspTop),
            size = Size(w * 0.24f, claspHeight),
            cornerRadius = CornerRadius(8.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF064E3B),
            radius = 3.2.dp.toPx(),
            center = Offset(vaultRight - w * 0.13f, claspTop + claspHeight * 0.5f)
        )

        // 3. المؤشر المالي النابض بمنحنى بيزيير صاعد (Pulsing Financial Bezier Indicator)
        val chartStart = Offset(vaultLeft + w * 0.09f, vaultBottom - h * 0.11f)
        val chartMid1 = Offset(vaultLeft + w * 0.24f, vaultBottom - h * 0.26f)
        val chartMid2 = Offset(vaultLeft + w * 0.36f, vaultBottom - h * 0.16f)
        val chartEnd = Offset(vaultLeft + w * 0.50f, vaultTop + h * 0.13f)

        val indicatorPath = Path().apply {
            moveTo(chartStart.x, chartStart.y)
            cubicTo(
                chartMid1.x, chartMid1.y,
                chartMid2.x, chartMid2.y,
                chartEnd.x, chartEnd.y
            )
        }
        drawPath(
            path = indicatorPath,
            color = Color(0xFF6EE7B7),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // نقطة النبض المالي المضيئة في قمة المؤشر
        drawCircle(
            color = Color(0xFFFDE68A).copy(alpha = 0.40f),
            radius = 6.5.dp.toPx() * pulseScale,
            center = chartEnd
        )
        drawCircle(
            color = Color(0xFFFDE68A),
            radius = 3.5.dp.toPx(),
            center = chartEnd
        )
    }
}

/**
 * بطاقة إحصائية مصرفية فخمة (إجمالي الأقساط • مجموع الواصل • إجمالي المتبقي)
 */
@Composable
private fun BankingKpiStatCard(
    label: String,
    formattedAmount: String,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * 3) بطاقة القسط المالي للطالب بتصميم مصرفي فخم (Student Tuition Card)
 */
@Composable
private fun StudentTuitionBankingCard(
    record: TuitionRecord,
    schoolName: String,
    canRecordPayment: Boolean,
    onRecordNewPayment: () -> Unit,
    onOpenDigitalReceipt: (TuitionInstallment) -> Unit,
    onSendReceiptWhatsApp: (TuitionInstallment) -> Unit,
    onSendStatementWhatsApp: () -> Unit
) {
    val progress = if (record.totalFee > 0L) {
        (record.paidAmount.toFloat() / record.totalFee.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val paidPercentage = (progress * 100f).roundToInt().coerceIn(0, 100)
    val formattedDueDate = formatAccountingDate(record.nextDueDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // رأس البطاقة: اسم الطالب بخط عريض وواضح، تحته الصف وولي الأمر، وفي اليسار شارة استحقاق ذهبية/كهرمانية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = record.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${record.gradeClass} (${record.sectionName}) • ولي الأمر: ${record.parentName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }

                // شارة استحقاق أنيقة بتصميم كبسولة ذهبية/كهرمانية (تاريخ الاستحقاق: 2026/11/15)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (record.remainingAmount == 0L) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (record.remainingAmount == 0L) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (record.remainingAmount == 0L) Icons.Filled.CheckCircle else Icons.Filled.EventRepeat,
                            contentDescription = null,
                            tint = if (record.remainingAmount == 0L) Color(0xFF047857) else Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (record.remainingAmount == 0L && formattedDueDate == "مكتمل") {
                                "مسدد بالكامل"
                            } else {
                                "تاريخ الاستحقاق: $formattedDueDate"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (record.remainingAmount == 0L) Color(0xFF047857) else Color(0xFFB45309),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // شريط تقدم بصري مزدوج الألوان مع نسبة التسديد المئوية (مثلاً تم تسديد 17%)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مؤشر التسديد المالي للقسط السنوي",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Text(
                            text = "تم تسديد $paidPercentage%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Dual-color visual progress bar (Track: Warm Amber/Rose • Fill: Emerald-to-Gold Gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFEF3C7),
                                    Color(0xFFFEE2E2)
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceAtLeast(0.03f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF064E3B),
                                        Color(0xFF0D8A6A),
                                        Color(0xFFF59E0B)
                                    )
                                )
                            )
                    )
                }
            }

            // 3 صناديق مصغرة متجاورة ومتساوية الأبعاد داخل البطاقة:
            // صندوق 1: القسط الكلي • صندوق 2: المبلغ الواصل • صندوق 3: المبلغ المتبقي
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudentAmountMiniBox(
                    title = "القسط الكلي",
                    formattedAmount = formatAccountingAmount(record.totalFee),
                    amountColor = Color(0xFF0F172A),
                    containerColor = Color(0xFFF8FAFC),
                    borderColor = Color(0xFFCBD5E1),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                StudentAmountMiniBox(
                    title = "المبلغ الواصل",
                    formattedAmount = formatAccountingAmount(record.paidAmount),
                    amountColor = Color(0xFF047857),
                    containerColor = Color(0xFFECFDF5),
                    borderColor = Color(0xFFA7F3D0),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                StudentAmountMiniBox(
                    title = "المبلغ المتبقي",
                    formattedAmount = formatAccountingAmount(record.remainingAmount),
                    amountColor = Color(0xFF9F1239),
                    containerColor = Color(0xFFFFF1F2),
                    borderColor = Color(0xFFFECDD3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // سجل الوصولات الرقمية: بطاقة إيصال رسمية مصغرة (Official Receipt Ticket) لكل وصل
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
                            text = "سجل الدفعات والوصولات الرقمية الرسمية (${record.installments.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF064E3B),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    record.installments.forEach { inst ->
                        OfficialReceiptTicketCard(
                            installment = inst,
                            onViewPdfReceipt = { onOpenDigitalReceipt(inst) },
                            onShareWhatsApp = { onSendReceiptWhatsApp(inst) }
                        )
                    }
                }
            }

            // الأزرار السفلية المتناسقة بدون أي قص للنصوص:
            // "تسجيل دفعة جديدة" (زمردي ملكي) و "إرسال إشعار ووصل" (أخضر أنيق)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canRecordPayment && record.remainingAmount > 0L) {
                    Button(
                        onClick = onRecordNewPayment,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("add_payment_${record.studentId}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF064E3B),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCard,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تسجيل دفعة جديدة",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Button(
                    onClick = onSendStatementWhatsApp,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF15803D),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إرسال إشعار ووصل",
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 12.5.sp,
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

/**
 * صندوق مصغر للمبالغ داخل بطاقة الطالب (القسط الكلي • المبلغ الواصل • المبلغ المتبقي)
 */
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
        shape = RoundedCornerShape(14.dp),
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * بطاقة إيصال رسمية مصغرة (Official Receipt Ticket) منسقة التاريخ والرقم مع أزرار نظيفة (وصل PDF و مشاركة واتساب)
 */
@Composable
private fun OfficialReceiptTicketCard(
    installment: TuitionInstallment,
    onViewPdfReceipt: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF6EE7B7))
                    ) {
                        Text(
                            text = installment.receiptNumber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = "التاريخ: ${formatAccountingDate(installment.paymentDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = formatAccountingAmount(installment.amount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF047857),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onViewPdfReceipt,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("view_digital_receipt_btn_${installment.receiptNumber}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "وصل PDF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    onClick = onShareWhatsApp,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "مشاركة واتساب",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
