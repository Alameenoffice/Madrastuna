package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.*
import com.example.ui.components.AdminSubscriptionAndBroadcastBanner
import com.example.ui.components.Base64AvatarImage
import com.example.ui.components.DigitalStudentIdDialog
import com.example.ui.components.ParentMultiChildSwitcherBar
import com.example.ui.components.VectorActionButton
import com.example.ui.screens.*
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuccessGreen
import com.example.util.DocumentAndIdExporter
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val schoolViewModel: SchoolViewModel = viewModel()
            val isDarkMode by schoolViewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MadrasatiRootApp(viewModel = schoolViewModel)
            }
        }
    }
}

@Composable
fun MadrasatiRootApp(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val onboardingSeen by viewModel.onboardingSeen.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDeveloperAuthenticated by viewModel.isDeveloperAuthenticated.collectAsState()
    val isSchoolSetupUnlocked by viewModel.isSchoolSetupUnlocked.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()
    val discoveredSchoolName by viewModel.discoveredSchoolName.collectAsState()
    val loginConnectingMessage by viewModel.loginConnectingMessage.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(toastAlert?.id) {
        if (toastAlert != null) {
            delay(3600)
            viewModel.dismissToastAlert()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !onboardingSeen -> {
                OnboardingScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onFinishOnboarding = { viewModel.completeOnboarding() }
                )
            }
            isDeveloperAuthenticated -> {
                DeveloperControlPanelScreen(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() }
                )
            }
            isSchoolSetupUnlocked -> {
                SchoolSetupScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onCancelSetup = { viewModel.cancelSchoolSetupAndReturn() },
                    onCompleteSetup = { schoolName, address, schoolType, logoBase64, adminFullName, adminUsername, adminPassword, schoolStage ->
                        viewModel.setupSchoolAndAdmin(
                            schoolName = schoolName,
                            address = address,
                            schoolType = schoolType,
                            logoBase64 = logoBase64,
                            adminFullName = adminFullName,
                            adminUsername = adminUsername,
                            adminPassword = adminPassword,
                            schoolStage = schoolStage
                        )
                    }
                )
            }
            currentUser == null -> {
                UnifiedLoginScreen(
                    schoolConfig = schoolConfig,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onLoginAttempt = { u, p -> viewModel.login(u, p) },
                    onVerifyActivationCode = { code, callback ->
                        viewModel.verifyActivationCodeAndUnlockSetup(code, callback)
                    },
                    discoveredSchoolName = discoveredSchoolName,
                    loginConnectingMessage = loginConnectingMessage,
                    onUsernameChanged = { u -> viewModel.previewSchoolNameByUsername(u) }
                )
            }
            else -> {
                AuthenticatedMainPlatform(viewModel = viewModel)
            }
        }

        // Global Toast / Snackbar Completion Alert Banner (Green on Success, Red with Reason on Error)
        AnimatedVisibility(
            visible = toastAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .zIndex(99f)
        ) {
            val alert = toastAlert
            if (alert != null) {
                Surface(
                    color = if (alert.isSuccess) SuccessGreen else DangerRed,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_toast_alert_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (alert.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = alert.message,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissToastAlert() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إغلاق التنبيه",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMainPlatform(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeSection by viewModel.activeSection.collectAsState()
    val allNotifications by viewModel.notifications.collectAsState()
    val latestBannerAlert by viewModel.latestBannerAlert.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val developerBroadcasts by viewModel.developerBroadcasts.collectAsState()

    val notifications = remember(allNotifications, currentUser, allStudents) {
        viewModel.getScopedNotificationsForUser(currentUser)
    }

    val parentChildren = remember(allStudents, currentUser) {
        viewModel.getChildrenForParent(currentUser)
    }
    val selectedParentChild = remember(parentChildren, selectedParentChildId, currentUser) {
        parentChildren.firstOrNull { it.id == selectedParentChildId }
            ?: parentChildren.firstOrNull { it.id == currentUser?.linkedStudentId }
            ?: parentChildren.firstOrNull()
    }

    var idCardDialogStudent by remember { mutableStateOf<StudentRecord?>(null) }

    var showNotificationsSheet by remember { mutableStateOf(false) }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    // BackHandler to return to Dashboard from any secondary section
    BackHandler(enabled = activeSection != AppSection.DASHBOARD) {
        viewModel.navigateToSection(AppSection.DASHBOARD)
    }

    val allSections = remember(schoolConfig.schoolType, currentUser) {
        buildList {
            add(AppSection.DASHBOARD to Icons.Filled.Dashboard)
            add(AppSection.ATTENDANCE to Icons.Filled.HowToReg)
            add(AppSection.SCHEDULE_HOMEWORK to Icons.Filled.CalendarMonth)
            add(AppSection.EXAMS_GRADES to Icons.Filled.Assessment)
            add(AppSection.HONOR_ROLL to Icons.Filled.EmojiEvents)
            if (schoolConfig.schoolType == SchoolType.PRIVATE && currentUser?.role != UserRole.TEACHER) {
                add(AppSection.TUITION_FEES to Icons.Filled.AccountBalanceWallet)
            }
            add(AppSection.REPORTS_CHAT to Icons.Filled.Forum)
            add(AppSection.EVENTS_NEWS to Icons.Filled.PhotoLibrary)
            if (currentUser?.role == UserRole.ADMIN) {
                add(AppSection.ADMIN_ACCOUNTS to Icons.Filled.ManageAccounts)
            }
            add(AppSection.SETTINGS to Icons.Filled.Settings)
        }
    }

    val bottomNavItems = remember(schoolConfig.schoolType) {
        listOf(
            AppSection.DASHBOARD to Icons.Filled.Dashboard,
            AppSection.ATTENDANCE to Icons.Filled.HowToReg,
            AppSection.SCHEDULE_HOMEWORK to Icons.Filled.MenuBook,
            AppSection.EXAMS_GRADES to Icons.Filled.Assessment,
            AppSection.HONOR_ROLL to Icons.Filled.EmojiEvents
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Main Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Base64AvatarImage(
                            base64Data = schoolConfig.logoBase64,
                            fallbackIcon = Icons.Filled.School,
                            contentDescription = schoolConfig.schoolName,
                            size = 42.dp
                        )
                        Column {
                            Text(
                                text = schoolConfig.schoolName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentUser?.role?.arabicTitle ?: ""} • ${schoolConfig.address}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Notification Bell with Numeric Badge
                        IconButton(
                            onClick = {
                                showNotificationsSheet = true
                                viewModel.markAllNotificationsRead()
                            },
                            modifier = Modifier.testTag("top_notifications_bell")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = DangerRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = "$unreadCount",
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "مركز الإشعارات الفورية",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Dark / Light Mode Toggle
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("top_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "الوضع الليلي والنهاري",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Settings Shortcut
                        IconButton(
                            onClick = { viewModel.navigateToSection(AppSection.SETTINGS) },
                            modifier = Modifier.testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "الإعدادات",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Top Bar Logout Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DangerRed.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clickable { viewModel.logout() }
                                .testTag("top_logout_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "تسجيل الخروج",
                                    tint = DangerRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "خروج",
                                    color = DangerRed,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Scrollable Section Pills Strip for instant access to all 8 sections + Admin Accounts + Settings
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allSections, key = { it.first.name }) { (section, icon) ->
                        val selected = activeSection == section
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .clickable { viewModel.navigateToSection(section) }
                                .testTag("nav_pill_${section.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = section.arabicTitle,
                                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = section.arabicTitle,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                bottomNavItems.forEach { (section, icon) ->
                    val selected = activeSection == section
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.navigateToSection(section) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = section.arabicTitle
                            )
                        },
                        label = {
                            Text(
                                text = section.arabicTitle,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.testTag("bottom_nav_${section.name}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Admin 14-day Subscription Expiry Warning & Developer Broadcasts Banner
            if (currentUser?.role == UserRole.ADMIN) {
                AdminSubscriptionAndBroadcastBanner(
                    schoolConfig = schoolConfig,
                    developerBroadcasts = developerBroadcasts
                )
            }

            // Parent Multi-Child Switcher Bar + Quick Student ID Card Button
            if (currentUser?.role == UserRole.PARENT && parentChildren.isNotEmpty()) {
                ParentMultiChildSwitcherBar(
                    children = parentChildren,
                    selectedStudent = selectedParentChild,
                    onSelectStudent = { stuId -> viewModel.selectParentChild(stuId) },
                    onOpenStudentIdCard = { stu -> idCardDialogStudent = stu }
                )
            }

            // Instant Alert Banner when a new notification arrives
            AnimatedVisibility(visible = latestBannerAlert != null) {
                val alert = latestBannerAlert
                if (alert != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = GoldAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = alert.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.dismissBannerAlert() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Active Section Screen Content
            Box(modifier = Modifier.weight(1f)) {
                when (activeSection) {
                    AppSection.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onOpenNotificationsDialog = {
                            showNotificationsSheet = true
                            viewModel.markAllNotificationsRead()
                        }
                    )
                    AppSection.ATTENDANCE -> DailyAttendanceScreen(viewModel = viewModel)
                    AppSection.SCHEDULE_HOMEWORK -> ScheduleAndHomeworkScreen(viewModel = viewModel)
                    AppSection.EXAMS_GRADES -> ExamsAndGradesScreen(viewModel = viewModel)
                    AppSection.HONOR_ROLL -> HonorRollScreen(viewModel = viewModel)
                    AppSection.TUITION_FEES -> TuitionFeesScreen(viewModel = viewModel)
                    AppSection.REPORTS_CHAT -> ReportsAndChatScreen(viewModel = viewModel)
                    AppSection.EVENTS_NEWS -> EventsAndNewsScreen(viewModel = viewModel)
                    AppSection.ADMIN_ACCOUNTS -> AdminAccountsScreen(viewModel = viewModel)
                    AppSection.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Full Notification History Dialog (سجل الإشعارات الفورية الكامل)
    if (showNotificationsSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationsSheet = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("سجل الإشعارات الفورية الكامل")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = notif.type.arabicLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = notif.createdAt,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = notif.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = notif.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "إغلاق السجل",
                    icon = Icons.Filled.Check,
                    onClick = { showNotificationsSheet = false }
                )
            }
        )
    }

    idCardDialogStudent?.let { targetStu ->
        val stuBadges = remember(targetStu.id, allStudents) {
            viewModel.getStudentAutoBadges(targetStu.id)
        }
        DigitalStudentIdDialog(
            student = targetStu,
            schoolConfig = schoolConfig,
            badges = stuBadges,
            onSavePng = {
                val res = DocumentAndIdExporter.saveStudentIdCardAsHighResPng(
                    context = context,
                    student = targetStu,
                    schoolConfig = schoolConfig,
                    badges = stuBadges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم حفظ الهوية المدرسية بدقة عالية (PNG) في المعرض جاهزة للطباعة أو الكبس الحراري",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert("تعذر حفظ الصورة: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onExportPdf = {
                val res = DocumentAndIdExporter.exportStudentIdCardPdf(
                    context = context,
                    student = targetStu,
                    schoolConfig = schoolConfig,
                    badges = stuBadges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم تصدير وفتح نافذة طباعة الهوية المدرسية بصيغة PDF بنجاح",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert("تعذر تصدير PDF: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onDismiss = { idCardDialogStudent = null }
        )
    }
}
