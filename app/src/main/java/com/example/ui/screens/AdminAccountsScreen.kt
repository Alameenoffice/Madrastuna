package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminAccountsScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val students by viewModel.students.collectAsState()

    val registeredClasses = schoolConfig.registeredClasses.ifEmpty { SchoolStage.SECONDARY.defaultClasses }
    val registeredSections = schoolConfig.registeredSections.ifEmpty { listOf("أ", "ب", "ج") }
    val registeredSubjects = schoolConfig.registeredSubjects.ifEmpty { SchoolStage.SECONDARY.defaultSubjects }

    // Search & Filters (شريط بحث فوري + فلتر حسب الدور + فلتر حسب الصف)
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("الكل") } // الكل, معلم, ولي أمر, طالب
    var selectedClassFilter by remember { mutableStateOf("الكل") }

    var showTeacherDialog by remember { mutableStateOf(false) }
    var editingTeacher by remember { mutableStateOf<UserAccount?>(null) }

    var showStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentRecord?>(null) }

    val teachers = remember(accounts, searchQuery, selectedClassFilter) {
        accounts.filter { acc ->
            acc.role == UserRole.TEACHER &&
                (searchQuery.isBlank() ||
                    acc.fullName.contains(searchQuery, ignoreCase = true) ||
                    acc.username.contains(searchQuery, ignoreCase = true) ||
                    acc.specialty.contains(searchQuery, ignoreCase = true)) &&
                (selectedClassFilter == "الكل" || acc.canTeacherAccessClassSection(selectedClassFilter, null))
        }
    }

    val filteredStudents = remember(students, searchQuery, selectedClassFilter) {
        students.filter { stu ->
            val qMatch = searchQuery.isBlank() ||
                stu.studentName.contains(searchQuery, ignoreCase = true) ||
                stu.parentName.contains(searchQuery, ignoreCase = true) ||
                stu.studentUsername.contains(searchQuery, ignoreCase = true) ||
                stu.parentUsername.contains(searchQuery, ignoreCase = true)
            val clsMatch = selectedClassFilter == "الكل" || stu.gradeClass == selectedClassFilter
            qMatch && clsMatch
        }
    }

    val showTeachersList = selectedRoleFilter == "الكل" || selectedRoleFilter == "معلم"
    val showStudentsParentsList = selectedRoleFilter == "الكل" || selectedRoleFilter == "ولي أمر" || selectedRoleFilter == "طالب"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Header Card
        item {
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
                            text = schoolConfig.schoolStage.arabicLabel,
                            icon = Icons.Filled.School,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    }

                    Text(
                        text = "مرتبط ديناميكياً بالصفوف والشعب المسجلة (${registeredClasses.size} صفوف). اختر الصفوف والمواد للمعلمين وأولياء الأمور والطلاب.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VectorActionButton(
                            text = "إضافة معلم",
                            icon = Icons.Filled.PersonAdd,
                            onClick = {
                                editingTeacher = null
                                showTeacherDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "add_teacher_button"
                        )
                        VectorActionButton(
                            text = "إضافة طالب وولي أمر",
                            icon = Icons.Filled.GroupAdd,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                editingStudent = null
                                showStudentDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "add_student_parent_button"
                        )
                    }
                }
            }
        }

        // Instant Search Bar + Role Filter + Class Filter
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("بحث فوري بالاسم أو اسم المستخدم...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("accounts_search_bar"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Role Filter (الكل، معلم، ولي أمر، طالب)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("الدور:", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("الكل", "معلم", "ولي أمر", "طالب")) { roleOption ->
                                FilterChip(
                                    selected = selectedRoleFilter == roleOption,
                                    onClick = { selectedRoleFilter = roleOption },
                                    label = { Text(roleOption, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }

                    // Registered Class Filter (حسب الصف المسجل)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Class,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("الصف:", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("الكل") + registeredClasses) { clsOption ->
                                FilterChip(
                                    selected = selectedClassFilter == clsOption,
                                    onClick = { selectedClassFilter = clsOption },
                                    label = { Text(clsOption, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Teachers List Section
        if (showTeachersList) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حسابات الكادر التدريسي (${teachers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
                                        text = "المواد المسندة: ${teacher.specialty}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "الصفوف والشعب المسندة: ${teacher.assignedClasses}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        editingTeacher = teacher
                                        showTeacherDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "تعديل صفوف ومواد المعلم",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = teacher.isActive,
                                    onCheckedChange = { viewModel.toggleAccountActive(teacher.id) }
                                )
                            }
                        }

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
                                Text(
                                    text = "المستخدم: ${teacher.username}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "كلمة المرور: ${teacher.password}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        VectorActionButton(
                            text = "إرسال بيانات الدخول للمعلم عبر واتساب",
                            icon = Icons.Filled.Chat,
                            containerColor = WhatsAppGreen,
                            onClick = {
                                val msg = """
                                    السلام عليكم أستاذنا الفاضل ${teacher.fullName}
                                    إدارة (${schoolConfig.schoolName} - ${schoolConfig.address}) ترحب بكم في منصة مدرستي.
                                    بيانات الدخول الخاصة بحساب المعلم:
                                    • كود المدرسة: ${schoolConfig.schoolId}
                                    • المواد المسندة: ${teacher.specialty}
                                    • الصفوف والشعب المسندة: ${teacher.assignedClasses}
                                    • اسم المستخدم الكامل: ${teacher.username}
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

        // Parents & Students List Section
        if (showStudentsParentsList) {
            item {
                Text(
                    text = "حسابات أولياء الأمور والطلاب (${filteredStudents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            items(filteredStudents, key = { it.id }) { stu ->
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
                                        text = "الصف المسجل: ${stu.gradeClass} (شعبة ${stu.sectionName})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "ولي الأمر: ${stu.parentName} | هاتف: ${stu.parentPhone}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        editingStudent = stu
                                        showStudentDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "تعديل بيانات الطالب وولي الأمر",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = stu.isActive,
                                    onCheckedChange = { viewModel.toggleStudentAndParentActive(stu.id) }
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedRoleFilter != "طالب") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "ولي الأمر: ${stu.parentUsername}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "كلمة المرور: ${stu.parentPassword}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                if (selectedRoleFilter == "الكل") {
                                    HorizontalDivider()
                                }
                                if (selectedRoleFilter != "ولي أمر") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "الطالب: ${stu.studentUsername}",
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
                        }

                        VectorActionButton(
                            text = "إرسال بيانات الحساب لولي الأمر عبر واتساب",
                            icon = Icons.Filled.Chat,
                            containerColor = WhatsAppGreen,
                            onClick = {
                                val msg = """
                                    السلام عليكم السيد ولي أمر الطالب (${stu.studentName}) المحترم،
                                    تهديكم إدارة (${schoolConfig.schoolName} - ${schoolConfig.address}) أطيب التحيات.
                                    تم تفعيل حسابات المتابعة اليومية في منصة مدرستي (كود المدرسة: ${schoolConfig.schoolId}):
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

    // Add / Edit Teacher Dialog with Multi-Select Checkboxes for Registered Classes/Sections & Subjects
    if (showTeacherDialog) {
        val currentEditing = editingTeacher
        var name by remember(currentEditing) { mutableStateOf(currentEditing?.fullName ?: "") }
        var phone by remember(currentEditing) { mutableStateOf(currentEditing?.phone ?: "07512362567") }
        var username by remember(currentEditing) {
            mutableStateOf(
                currentEditing?.username ?: FirebaseConfigConstants.applySchoolPrefix(
                    schoolConfig.schoolId,
                    "t${teachers.size + 1}"
                )
            )
        }
        var password by remember(currentEditing) { mutableStateOf(currentEditing?.password ?: (1000..9999).random().toString()) }

        // Multi-select subjects from registeredSubjects
        val initialSubjects = remember(currentEditing) {
            currentEditing?.teacherSubjectsList?.toSet() ?: setOf(registeredSubjects.firstOrNull() ?: "الرياضيات")
        }
        var selectedSubjects by remember(currentEditing) { mutableStateOf(initialSubjects) }

        // Multi-select class-section combinations from registeredClasses × registeredSections
        val allClassSectionOptions = remember(registeredClasses, registeredSections) {
            registeredClasses.flatMap { cls ->
                registeredSections.map { sec -> "$cls - $sec" }
            }
        }
        val initialClassSections = remember(currentEditing, allClassSectionOptions) {
            val existing = currentEditing?.teacherClassSectionPairs?.toSet() ?: emptySet()
            if (existing.isNotEmpty()) existing else setOfNotNull(allClassSectionOptions.firstOrNull())
        }
        var selectedClassSections by remember(currentEditing) { mutableStateOf(initialClassSections) }

        AlertDialog(
            onDismissRequest = { showTeacherDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentEditing == null) "إضافة معلم وتحديد صفوفه ومادته" else "تعديل بيانات وصلاحيات المعلم")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 440.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("اسم المعلم الثلاثي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            text = "اختر المواد الدراسية المسندة للمعلم (اختيار متعدد):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            registeredSubjects.forEach { subj ->
                                val isChecked = subj in selectedSubjects
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        selectedSubjects = if (isChecked && selectedSubjects.size > 1) {
                                            selectedSubjects - subj
                                        } else {
                                            selectedSubjects + subj
                                        }
                                    },
                                    label = { Text(subj, maxLines = 1, softWrap = false) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "اختر الصفوف والشعب المسندة من الصفوف المسجلة (اختيار متعدد):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allClassSectionOptions.forEach { pairLabel ->
                                val isChecked = pairLabel in selectedClassSections
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        selectedClassSections = if (isChecked && selectedClassSections.size > 1) {
                                            selectedClassSections - pairLabel
                                        } else {
                                            selectedClassSections + pairLabel
                                        }
                                    },
                                    label = { Text(pairLabel, maxLines = 1, softWrap = false) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم واتساب المعلم") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("اسم المستخدم") },
                                placeholder = { Text("مثال: 1 أو ali") },
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
                        Text(
                            text = "اسم المستخدم المعتمد مع كود المدرسة: ${FirebaseConfigConstants.applySchoolPrefix(schoolConfig.schoolId, username)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = if (currentEditing == null) "اعتماد وتفعيل الحساب" else "حفظ التعديلات",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        if (name.isNotBlank()) {
                            val specialtyStr = selectedSubjects.joinToString("، ")
                            val classesStr = selectedClassSections.joinToString("، ")
                            if (currentEditing == null) {
                                viewModel.addTeacherAccount(name, specialtyStr, classesStr, phone, username, password)
                            } else {
                                viewModel.updateTeacherAccount(currentEditing.id, name, specialtyStr, classesStr, phone, username, password)
                            }
                            showTeacherDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showTeacherDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Add / Edit Student & Parent Dialog with Dynamic Registered Class & Section Dropdowns
    if (showStudentDialog) {
        val currentStu = editingStudent
        var studentName by remember(currentStu) { mutableStateOf(currentStu?.studentName ?: "") }
        var gradeClass by remember(currentStu, registeredClasses) {
            mutableStateOf(currentStu?.gradeClass?.takeIf { it in registeredClasses } ?: registeredClasses.first())
        }
        var sectionName by remember(currentStu, registeredSections) {
            mutableStateOf(currentStu?.sectionName?.takeIf { it in registeredSections } ?: registeredSections.first())
        }
        var parentName by remember(currentStu) { mutableStateOf(currentStu?.parentName ?: "") }
        var parentPhone by remember(currentStu) { mutableStateOf(currentStu?.parentPhone ?: "07512362567") }
        var address by remember(currentStu) { mutableStateOf(currentStu?.address ?: "الموصل - زمار") }
        var totalFeeText by remember { mutableStateOf("1500000") }
        var photoBase64 by remember(currentStu) { mutableStateOf(currentStu?.photoBase64 ?: "") }
        var sUser by remember(currentStu) {
            mutableStateOf(
                currentStu?.studentUsername ?: FirebaseConfigConstants.applySchoolPrefix(
                    schoolConfig.schoolId,
                    "${students.size + 1}"
                )
            )
        }
        var sPass by remember(currentStu) { mutableStateOf(currentStu?.studentPassword ?: (1000..9999).random().toString()) }
        var pUser by remember(currentStu) {
            mutableStateOf(
                currentStu?.parentUsername ?: FirebaseConfigConstants.applySchoolPrefix(
                    schoolConfig.schoolId,
                    "p${students.size + 1}"
                )
            )
        }
        var pPass by remember(currentStu) { mutableStateOf(currentStu?.parentPassword ?: (1000..9999).random().toString()) }

        var classDropdownExpanded by remember { mutableStateOf(false) }
        var sectionDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showStudentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentStu == null) "إضافة طالب وولي أمره" else "تعديل بيانات الطالب وولي الأمر")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 440.dp)
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
                        Text(
                            text = "اختر الصف والشعبة من الصفوف المسجلة:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        // Dynamic Registered Class Dropdown
                        ExposedDropdownMenuBox(
                            expanded = classDropdownExpanded,
                            onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = gradeClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الصف الدراسي المسجل") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = classDropdownExpanded,
                                onDismissRequest = { classDropdownExpanded = false }
                            ) {
                                registeredClasses.forEach { clsOption ->
                                    DropdownMenuItem(
                                        text = { Text(clsOption) },
                                        onClick = {
                                            gradeClass = clsOption
                                            classDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        // Dynamic Registered Section Dropdown
                        ExposedDropdownMenuBox(
                            expanded = sectionDropdownExpanded,
                            onExpandedChange = { sectionDropdownExpanded = !sectionDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = "شعبة $sectionName",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الشعبة المسجلة") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = sectionDropdownExpanded,
                                onDismissRequest = { sectionDropdownExpanded = false }
                            ) {
                                registeredSections.forEach { secOption ->
                                    DropdownMenuItem(
                                        text = { Text("شعبة $secOption") },
                                        onClick = {
                                            sectionName = secOption
                                            sectionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
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
                    if (schoolConfig.schoolType == SchoolType.PRIVATE && currentStu == null) {
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
                        Text(
                            text = "البادئة التلقائية: ولي الأمر (${FirebaseConfigConstants.applySchoolPrefix(schoolConfig.schoolId, pUser)}) • الطالب (${FirebaseConfigConstants.applySchoolPrefix(schoolConfig.schoolId, sUser)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = if (currentStu == null) "إنشاء وتفعيل الحسابين" else "حفظ التعديلات",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        if (studentName.isNotBlank() && parentName.isNotBlank()) {
                            if (currentStu == null) {
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
                            } else {
                                viewModel.updateStudentAndParentAccounts(
                                    studentId = currentStu.id,
                                    studentName = studentName,
                                    gradeClass = gradeClass,
                                    sectionName = sectionName,
                                    parentName = parentName,
                                    parentPhone = parentPhone,
                                    address = address,
                                    studentUsername = sUser,
                                    studentPassword = sPass,
                                    parentUsername = pUser,
                                    parentPassword = pPass
                                )
                            }
                            showStudentDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showStudentDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
