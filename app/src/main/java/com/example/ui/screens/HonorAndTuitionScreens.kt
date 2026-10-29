package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel

/**
 * Section 5: Celebratory Golden Honor Roll Screen (لوحة الشرف والطلبة المجتهدون)
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

    val canEdit = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val honorStudents = remember(students, selectedClass) {
        students
            .filter { it.isOnHonorRoll && (selectedClass == "الكل" || it.gradeClass == selectedClass) }
            .sortedByDescending { it.excellencePoints }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // Golden Celebratory Banner
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(22.dp)
                            )
                            NoWrapBadge(
                                text = "لوحة الشرف الذهبية • ${schoolConfig.schoolName}",
                                icon = Icons.Filled.EmojiEvents,
                                containerColor = Color(0xFFFDE68A),
                                contentColor = Color(0xFF451A03)
                            )
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(22.dp)
                            )
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

        // Class Filter Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val classesList = listOf("الكل") + viewModel.availableClasses
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

        // Honor Roll Student Cards with Golden Vector Medals
        itemsIndexed(honorStudents, key = { _, s -> s.id }) { index, stu ->
            val rank = index + 1
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(GoldAccent, Color(0xFFFDE68A), GoldDeep)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                contentDescription = stu.studentName,
                                size = 54.dp,
                                tint = GoldAccent
                            )

                            Column {
                                Text(
                                    text = stu.studentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "الصف: ${stu.gradeClass} • شعبة (${stu.sectionName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                NoWrapBadge(
                                    text = stu.honorTitle,
                                    icon = Icons.Filled.MilitaryTech,
                                    containerColor = GoldAccent.copy(alpha = 0.18f),
                                    contentColor = GoldDeep
                                )
                            }
                        }

                        // Excellence Points Pill
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
                                    imageVector = Icons.Filled.Stars,
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

                    if (canEdit) {
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

        // Section for adding other students to the Honor Roll
        if (canEdit) {
            item {
                val nonHonor = students.filter { !it.isOnHonorRoll }
                if (nonHonor.isNotEmpty()) {
                    Text(
                        text = "ترشيح طلبة إضافيين للوحة الشرف:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    nonHonor.forEach { s ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${s.studentName} (${s.gradeClass})", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { editingStudent = s }) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = GoldAccent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("منح وسام الشرف", maxLines = 1, softWrap = false)
                                }
                            }
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
            title = { Text("تحديث وسام التميز: ${stu.studentName}") },
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
 * Automatically visible/enabled when schoolType == PRIVATE and hidden when PUBLIC.
 */
@Composable
fun TuitionFeesScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val tuitions by viewModel.tuitions.collectAsState()

    var selectedTuitionForPayment by remember { mutableStateOf<TuitionRecord?>(null) }

    val canRecordPayment = currentUser?.role == UserRole.ADMIN

    val visibleTuitions = remember(tuitions, currentUser) {
        when (currentUser?.role) {
            UserRole.PARENT, UserRole.STUDENT -> tuitions.filter {
                it.studentId == currentUser?.linkedStudentId || currentUser?.linkedStudentId.isNullOrBlank()
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
        // Summary Financial Banner
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
                                text = "نظام الأقساط الدراسية (${schoolConfig.schoolType.arabicLabel})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        NoWrapBadge(
                            text = "مفعل تلقائياً",
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
                }
            }
        }

        // Student Tuition Cards
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

                    // Installments History List
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
                                    text = "سجل الدفعات والوصولات:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                record.installments.forEach { inst ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${inst.receiptNumber} • ${inst.amount} د.ع (${inst.paymentDate})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
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
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Chat,
                                                contentDescription = null,
                                                tint = WhatsAppGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "إرسال الوصل واتساب",
                                                color = WhatsAppGreen,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
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
