package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel

@Composable
fun AdminAccountsScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val students by viewModel.students.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = قسم المعلمين, 1 = قسم أولياء الأمور والطلاب
    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }

    val teachers = remember(accounts) {
        accounts.filter { it.role == UserRole.TEACHER }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ManageAccounts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "إدارة الحسابات والمستخدمين",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    NoWrapBadge(
                        text = "حصرياً للإدارة",
                        icon = Icons.Filled.VerifiedUser,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                }

                Text(
                    text = "توليد وتفعيل حسابات المعلمين وأولياء الأمور والطلاب وإرسال بيانات الدخول مباشرة عبر واتساب.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-Tabs: قسم المعلمين | قسم أولياء الأمور والطلاب
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isTeachersTab = selectedTab == 0
            Button(
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_teachers_accounts"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTeachersTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isTeachersTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "قسم المعلمين (${teachers.size})",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_students_parents_accounts"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isTeachersTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!isTeachersTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.FamilyRestroom, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "أولياء الأمور والطلاب (${students.size})",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Teachers Section
            VectorActionButton(
                text = "إضافة معلم جديد وتوليد بيانات الدخول",
                icon = Icons.Filled.PersonAdd,
                onClick = { showAddTeacherDialog = true },
                modifier = Modifier.fillMaxWidth(),
                testTag = "add_teacher_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(teachers, key = { it.id }) { teacher ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Base64AvatarImage(
                                        base64Data = teacher.photoBase64,
                                        fallbackIcon = Icons.Filled.CoPresent,
                                        contentDescription = teacher.fullName,
                                        size = 48.dp
                                    )
                                    Column {
                                        Text(
                                            text = teacher.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "الاختصاص: ${teacher.specialty} | الصفوف: ${teacher.assignedClasses}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Active / Inactive Account Toggle Switch
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (teacher.isActive) "مفعل" else "موقوف",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (teacher.isActive) SuccessGreen else MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Switch(
                                        checked = teacher.isActive,
                                        onCheckedChange = { viewModel.toggleAccountActive(teacher.id) }
                                    )
                                }
                            }

                            // Generated Credentials Box
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "المستخدم: ${teacher.username}",
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Key,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "كلمة المرور: ${teacher.password}",
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Send Teacher Credentials via WhatsApp Button
                            VectorActionButton(
                                text = "إرسال بيانات الدخول للمعلم عبر واتساب",
                                icon = Icons.Filled.Chat,
                                containerColor = WhatsAppGreen,
                                onClick = {
                                    val msg = """
                                        السلام عليكم أستاذنا الفاضل ${teacher.fullName}
                                        إدارة (${schoolConfig.schoolName} - ${schoolConfig.address}) ترحب بكم في منصة مدرستي.
                                        بيانات الدخول الخاصة بحساب المعلم:
                                        • الاختصاص: ${teacher.specialty}
                                        • الصفوف المسندة: ${teacher.assignedClasses}
                                        • اسم المستخدم: ${teacher.username}
                                        • كلمة المرور: ${teacher.password}
                                    """.trimIndent()
                                    ExternalIntents.sendWhatsAppMessage(context, teacher.phone, msg)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else {
            // Parents & Students Section
            VectorActionButton(
                text = "إضافة طالب وولي أمر وتوليد بيانات الدخول",
                icon = Icons.Filled.GroupAdd,
                onClick = { showAddStudentDialog = true },
                modifier = Modifier.fillMaxWidth(),
                testTag = "add_student_parent_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(students, key = { it.id }) { stu ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Base64AvatarImage(
                                        base64Data = stu.photoBase64,
                                        fallbackIcon = Icons.Filled.School,
                                        contentDescription = stu.studentName,
                                        size = 50.dp
                                    )
                                    Column {
                                        Text(
                                            text = stu.studentName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "الصف: ${stu.gradeClass} (شعبة ${stu.sectionName}) | ولي الأمر: ${stu.parentName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "العنوان: ${stu.address} | الهاتف: ${stu.parentPhone}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (stu.isActive) "مفعل" else "موقوف",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (stu.isActive) SuccessGreen else MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Switch(
                                        checked = stu.isActive,
                                        onCheckedChange = { viewModel.toggleStudentAndParentActive(stu.id) }
                                    )
                                }
                            }

                            // Parent & Student Credentials Grid
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "حساب ولي الأمر: ${stu.parentUsername}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "كلمة المرور: ${stu.parentPassword}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "حساب الطالب: ${stu.studentUsername}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "كلمة المرور: ${stu.studentPassword}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }

                            VectorActionButton(
                                text = "إرسال بيانات الحساب لولي الأمر عبر واتساب",
                                icon = Icons.Filled.Chat,
                                containerColor = WhatsAppGreen,
                                onClick = {
                                    val msg = """
                                        السلام عليكم السيد ولي أمر الطالب (${stu.studentName}) المحترم،
                                        تهديكم إدارة (${schoolConfig.schoolName} - ${schoolConfig.address}) أطيب التحيات.
                                        تم تفعيل حسابات المتابعة اليومية في منصة مدرستي:
                                        • الصف والشعبة: ${stu.gradeClass} - شعبة (${stu.sectionName})
                                        • حساب ولي الأمر: المستخدم (${stu.parentUsername}) | الرمز (${stu.parentPassword})
                                        • حساب الطالب: المستخدم (${stu.studentUsername}) | الرمز (${stu.studentPassword})
                                    """.trimIndent()
                                    ExternalIntents.sendWhatsAppMessage(context, stu.parentPhone, msg)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Teacher Dialog
    if (showAddTeacherDialog) {
        var name by remember { mutableStateOf("") }
        var specialty by remember { mutableStateOf("اللغة العربية") }
        var classes by remember { mutableStateOf("الأول المتوسط، الثاني المتوسط") }
        var phone by remember { mutableStateOf("07512362567") }
        var username by remember { mutableStateOf("tch_${(100..999).random()}") }
        var password by remember { mutableStateOf((1000..9999).random().toString()) }

        AlertDialog(
            onDismissRequest = { showAddTeacherDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة معلم وتوليد حسابه")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المعلم الثلاثي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = specialty,
                        onValueChange = { specialty = it },
                        label = { Text("الاختصاص والمادة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = classes,
                        onValueChange = { classes = it },
                        label = { Text("الصفوف المسندة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم واتساب المعلم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("اسم المستخدم") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TextButton(
                        onClick = {
                            username = "tch_${(100..999).random()}"
                            password = (1000..9999).random().toString()
                        }
                    ) {
                        Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("توليد تلقائي جديد", maxLines = 1, softWrap = false)
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "اعتماد وتفعيل الحساب",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addTeacherAccount(name, specialty, classes, phone, username, password)
                            showAddTeacherDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddTeacherDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Student & Parent Dialog
    if (showAddStudentDialog) {
        var studentName by remember { mutableStateOf("") }
        var gradeClass by remember { mutableStateOf(viewModel.availableClasses.first()) }
        var sectionName by remember { mutableStateOf("أ") }
        var parentName by remember { mutableStateOf("") }
        var parentPhone by remember { mutableStateOf("07512362567") }
        var address by remember { mutableStateOf("الموصل - زمار") }
        var totalFeeText by remember { mutableStateOf("1500000") }
        var photoBase64 by remember { mutableStateOf("") }
        var sUser by remember { mutableStateOf("stu_${(100..999).random()}") }
        var sPass by remember { mutableStateOf((1000..9999).random().toString()) }
        var pUser by remember { mutableStateOf("par_${(100..999).random()}") }
        var pPass by remember { mutableStateOf((1000..9999).random().toString()) }

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة طالب وولي أمره")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = studentName,
                            onValueChange = { studentName = it },
                            label = { Text("اسم الطالب الرباعي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = gradeClass,
                                onValueChange = { gradeClass = it },
                                label = { Text("الصف الدراسي") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                            OutlinedTextField(
                                value = sectionName,
                                onValueChange = { sectionName = it },
                                label = { Text("الشعبة") },
                                singleLine = true,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = parentName,
                            onValueChange = { parentName = it },
                            label = { Text("اسم ولي الأمر") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = parentPhone,
                            onValueChange = { parentPhone = it },
                            label = { Text("رقم واتساب ولي الأمر") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان السكني") },
                            placeholder = { Text("الموصل - زمار") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (schoolConfig.schoolType == SchoolType.PRIVATE) {
                        item {
                            OutlinedTextField(
                                value = totalFeeText,
                                onValueChange = { totalFeeText = it.filter { c -> c.isDigit() } },
                                label = { Text("القسط السنوي الكلي (د.ع)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        CompressedPhotoPickerButton(
                            label = "صورة الطالب (ضغط تلقائي 800px)",
                            currentBase64 = photoBase64,
                            onImageCompressed = { photoBase64 = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            text = "بيانات الدخول المولدة لولي الأمر والطالب:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = pUser,
                                onValueChange = { pUser = it },
                                label = { Text("مستخدم ولي الأمر") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pPass,
                                onValueChange = { pPass = it },
                                label = { Text("الرمز") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = sUser,
                                onValueChange = { sUser = it },
                                label = { Text("مستخدم الطالب") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sPass,
                                onValueChange = { sPass = it },
                                label = { Text("الرمز") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "إنشاء وتفعيل الحسابين",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        if (studentName.isNotBlank() && parentName.isNotBlank()) {
                            viewModel.addStudentAndParentAccounts(
                                studentName = studentName,
                                gradeClass = gradeClass,
                                sectionName = sectionName,
                                parentName = parentName,
                                parentPhone = parentPhone,
                                address = address.ifBlank { "الموصل - زمار" },
                                totalFee = totalFeeText.toLongOrNull() ?: 1500000L,
                                photoBase64 = photoBase64,
                                studentUsername = sUser,
                                studentPassword = sPass,
                                parentUsername = pUser,
                                parentPassword = pPass
                            )
                            showAddStudentDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
