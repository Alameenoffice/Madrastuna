package com.example.staffattendance

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppSection
import com.example.ui.components.ModernCalendarDatePickerField
import com.example.ui.components.ThreeDHeroHeaderCard
import com.example.ui.components.formatCleanDate
import com.example.ui.theme.DeepEmerald
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffSelfAttendanceScreen(
    viewModel: SchoolViewModel
) {
    BackHandler {
        viewModel.navigateToSection(AppSection.DASHBOARD)
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val schoolConfig by viewModel.schoolConfig.collectAsStateWithLifecycle()
    val todayRecordsMap by StaffAttendanceCloudStore.todayRecordsByStaffId.collectAsStateWithLifecycle()
    val shiftSchedule by StaffAttendanceCloudStore.shiftScheduleState.collectAsStateWithLifecycle()

    val schoolCode = remember(schoolConfig.schoolId) {
        schoolConfig.schoolId.ifBlank { "default_school" }
    }
    val staffId = currentUser?.id.orEmpty().ifBlank { "staff_unknown" }
    val staffName = currentUser?.fullName.orEmpty().ifBlank { "عضو الكادر" }
    val staffRoleLabel = currentUser?.specialty?.ifBlank { "معلم" } ?: "معلم"
    val todayIso = remember { StaffAttendanceDateUtils.todayIso() }
    val currentMonthIso = remember { StaffAttendanceDateUtils.currentMonthIso() }
    val todayHolidayLabel = remember(todayIso) { viewModel.getHolidayLabelForDate(todayIso) }
    val holidayNotice = remember(todayIso, todayHolidayLabel, shiftSchedule.weekendDays) {
        StaffAttendanceDateUtils.getHolidayNoticeIfAny(todayIso, todayHolidayLabel, shiftSchedule.weekendDays)
    }

    var geofenceConfig by remember { mutableStateOf(SchoolGeofenceConfig()) }
    var zoneProbe by remember { mutableStateOf(ZoneProbeResult(isLoading = true)) }
    var todayRecord by remember { mutableStateOf<StaffAttendanceRecord?>(null) }
    var monthlyHistory by remember { mutableStateOf<List<StaffAttendanceRecord>>(emptyList()) }
    var myLeaveRequests by remember { mutableStateOf<List<StaffLeaveRequest>>(emptyList()) }

    var isScanningFingerprint by remember { mutableStateOf(false) }
    var feedbackBanner by remember { mutableStateOf<FingerprintActionOutcome?>(null) }
    var showLeaveSheet by remember { mutableStateOf(false) }
    var selectedHistoryTab by remember { mutableIntStateOf(0) } // 0 = السجل الشهري, 1 = طلبات الإجازة
    var currentNowMinutes by remember { mutableIntStateOf(StaffAttendanceDateUtils.nowMinutes()) }

    // مزامنة فورية إذا حدّث المدير سجل اليوم أو إعدادات الدوام الموحّدة
    val liveTodayRecord = todayRecordsMap[staffId] ?: todayRecord
    val todayResolvedStatus = remember(liveTodayRecord, myLeaveRequests, shiftSchedule, todayHolidayLabel, todayIso, currentNowMinutes) {
        StaffAttendanceStatusEngine.resolveStatus(
            dateIso = todayIso,
            record = liveTodayRecord,
            hasApprovedLeaveForDate = StaffAttendanceStatusEngine.isLeaveApprovedForDate(staffId, todayIso, myLeaveRequests),
            shiftSchedule = shiftSchedule,
            schoolHolidayLabel = todayHolidayLabel,
            todayIso = todayIso,
            nowMinutes = currentNowMinutes
        )
    }

    fun refreshAllData(probeGps: Boolean = true) {
        scope.launch {
            if (probeGps) {
                zoneProbe = zoneProbe.copy(isLoading = true)
            }
            val cfg = StaffAttendanceCloudStore.fetchGeofenceConfig(context, schoolCode)
            geofenceConfig = cfg
            val shiftCfg = StaffAttendanceCloudStore.fetchShiftScheduleConfig(context, schoolCode)
            currentNowMinutes = StaffAttendanceDateUtils.nowMinutes()
            val rec = StaffAttendanceCloudStore.fetchStaffRecordForDate(context, schoolCode, todayIso, staffId)
            todayRecord = rec
            if (probeGps) {
                zoneProbe = ElectronicFingerprintZoneEngine.probeCurrentZone(
                    context = context,
                    config = cfg,
                    holidayLabel = todayHolidayLabel,
                    shiftSchedule = shiftCfg
                )
            }
            val allMonth = StaffAttendanceCloudStore.fetchMonthlyAttendance(context, schoolCode, currentMonthIso)
            monthlyHistory = allMonth.filter { it.staffId == staffId }
            val allLeaves = StaffAttendanceCloudStore.fetchLeaveRequests(context, schoolCode)
            myLeaveRequests = allLeaves.filter { it.staffId == staffId }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            refreshAllData(probeGps = true)
        } else {
            zoneProbe = zoneProbe.copy(
                isLoading = false,
                hasLocationPermission = false,
                statusHeadline = "لم يُمنح إذن الموقع",
                statusDetail = "يرجى السماح بالوصول للموقع (GPS) للتحقق من تواجدك داخل المدرسة."
            )
        }
    }

    LaunchedEffect(schoolCode, staffId) {
        refreshAllData(probeGps = true)
    }

    // مزامنة لحظية مستمرة لإعدادات الدوام الموحّدة في Firebase وتحديث عدّاد المهلة المتبقية
    LaunchedEffect(schoolCode) {
        while (true) {
            delay(20_000L)
            currentNowMinutes = StaffAttendanceDateUtils.nowMinutes()
            StaffAttendanceCloudStore.fetchShiftScheduleConfig(context, schoolCode)
        }
    }

    val fingerprintStage = remember(isScanningFingerprint, liveTodayRecord, holidayNotice, zoneProbe) {
        when {
            isScanningFingerprint -> FingerprintVisualStage.SCANNING
            holidayNotice != null -> FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY
            liveTodayRecord?.isManualOverride == true -> FingerprintVisualStage.COMPLETED_DAY
            liveTodayRecord != null && liveTodayRecord.hasCheckedIn && liveTodayRecord.hasCheckedOut ->
                FingerprintVisualStage.COMPLETED_DAY
            liveTodayRecord != null && liveTodayRecord.hasCheckedIn -> FingerprintVisualStage.READY_CHECK_OUT
            !geofenceConfig.isEnabled -> FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY
            else -> FingerprintVisualStage.READY_CHECK_IN
        }
    }

    val primaryButtonSubtitle = remember(fingerprintStage, liveTodayRecord, holidayNotice) {
        when {
            isScanningFingerprint -> "جارٍ التحقق من الموقع وتوثيق البصمة..."
            holidayNotice != null -> holidayNotice
            liveTodayRecord?.isManualOverride == true -> "تم ضبط حالة اليوم يدوياً من قبل إدارة المدرسة"
            liveTodayRecord != null && liveTodayRecord.hasCheckedIn && liveTodayRecord.hasCheckedOut ->
                "اكتمل دوامك اليوم بنجاح • الحضور ${liveTodayRecord.checkInTime} | الانصراف ${liveTodayRecord.checkOutTime}"
            liveTodayRecord != null && liveTodayRecord.hasCheckedIn ->
                "تم تسجيل حضورك الساعة ${liveTodayRecord.checkInTime} • اضغط لتسجيل بصمة الانصراف"
            else -> "اضغط لتسجيل بصمة الحضور داخل النطاق الجغرافي للمدرسة"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. الترويسة الزمردية مع مؤشر النطاق الجغرافي الحي
        item {
            val shiftPillLabel = if (shiftSchedule.isConfigured) {
                val range12h = StaffAttendanceDateUtils.formatShiftRangeArabic12h(
                    shiftSchedule.shiftStart,
                    shiftSchedule.shiftEnd
                )
                "دوام ${shiftSchedule.resolvedShiftType.arabicLabel} • $range12h"
            } else {
                "بانتظار ضبط الدوام"
            }
            ThreeDHeroHeaderCard(
                title = "البصمة الإلكترونية والحضور",
                subtitle = "$staffName • ${geofenceConfig.locationLabel}",
                rightPillText = shiftPillLabel,
                rightPillIcon = Icons.Filled.Schedule,
                leftPillText = formatCleanDate(todayIso),
                leftPillIcon = Icons.Filled.VerifiedUser,
                customTrailingContent = {
                    StaffZoneLiveIndicatorHeaderWidget(
                        probe = zoneProbe,
                        onRefreshClick = {
                            if (!ElectronicFingerprintZoneEngine.hasLocationPermission(context)) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                refreshAllData(probeGps = true)
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. شريط تنبيه العطلة الرسمية أو التحكم الإداري أو إذن الموقع
        if (holidayNotice != null || liveTodayRecord?.isManualOverride == true || !zoneProbe.hasLocationPermission) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (holidayNotice != null) {
                        StatusNoticeBanner(
                            icon = Icons.Filled.EventNote,
                            accent = Color(0xFF0284C7),
                            title = "عطلة رسمية",
                            message = holidayNotice
                        )
                    }

                    if (liveTodayRecord?.isManualOverride == true) {
                        StatusNoticeBanner(
                            icon = Icons.Filled.Shield,
                            accent = StaffAttendancePalette.LeaveAmber,
                            title = "حالة اليوم مثبتة من الإدارة (${staffStatusArabicLabel(liveTodayRecord.status)})",
                            message = liveTodayRecord.adminNotes.ifBlank {
                                "تم تعديل وتثبيت سجل اليوم يدوياً بواسطة إدارة المدرسة."
                            }
                        )
                    }

                    if (holidayNotice == null && !zoneProbe.hasLocationPermission) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = StaffAttendancePalette.LeaveAmber.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, StaffAttendancePalette.LeaveAmber.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOff,
                                    contentDescription = "إذن الموقع",
                                    tint = StaffAttendancePalette.LeaveAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "يلزم تفعيل إذن الموقع (GPS)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "يُستخدم الموقع لحظياً فقط عند الضغط على البصمة للتأكد من وجودك داخل المدرسة.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("staff_grant_location_btn")
                                ) {
                                    Text("تفعيل", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. بطاقة "البصمة الإلكترونية الذكية" المركزية
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.16f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "البصمة الإلكترونية الذكية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "التحقق الفوري عبر النطاق الجغرافي للمدرسة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (zoneProbe.insideZone) {
                                StaffAttendancePalette.PresentEmerald.copy(alpha = 0.12f)
                            } else {
                                StaffAttendancePalette.LeaveAmber.copy(alpha = 0.12f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (zoneProbe.matchedByWifi) Icons.Filled.Wifi else Icons.Filled.LocationOn,
                                    contentDescription = "حالة النطاق",
                                    tint = if (zoneProbe.insideZone) StaffAttendancePalette.PresentEmerald else StaffAttendancePalette.LeaveAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = zoneProbe.statusHeadline,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (zoneProbe.insideZone) StaffAttendancePalette.PresentEmerald else StaffAttendancePalette.LeaveAmber
                                )
                            }
                        }
                    }

                    // بطاقة معلومات الدوام الموحّد المدمجة أعلى زر البصمة مع حالة المهلة المتبقية بوضوح
                    StaffEmbeddedShiftScheduleCard(
                        shiftSchedule = shiftSchedule,
                        todayResolvedStatus = todayResolvedStatus,
                        liveTodayRecord = liveTodayRecord,
                        holidayNotice = holidayNotice,
                        todayHolidayLabel = todayHolidayLabel,
                        nowMinutes = currentNowMinutes,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // زر البصمة الدائري الكبير
                    val canTriggerFingerprint = holidayNotice == null &&
                        geofenceConfig.isEnabled &&
                        liveTodayRecord?.isManualOverride != true &&
                        !(liveTodayRecord != null && liveTodayRecord.hasCheckedIn && liveTodayRecord.hasCheckedOut)

                    ElectronicFingerprintScanButton(
                        stage = fingerprintStage,
                        enabled = canTriggerFingerprint,
                        onClick = {
                            if (!ElectronicFingerprintZoneEngine.hasLocationPermission(context)) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                                return@ElectronicFingerprintScanButton
                            }
                            scope.launch {
                                isScanningFingerprint = true
                                feedbackBanner = null
                                val outcome = ElectronicFingerprintZoneEngine.performElectronicFingerprint(
                                    context = context,
                                    schoolCode = schoolCode,
                                    staffId = staffId,
                                    staffName = staffName,
                                    staffRoleLabel = staffRoleLabel,
                                    config = geofenceConfig,
                                    existingTodayRecord = liveTodayRecord,
                                    holidayLabel = todayHolidayLabel,
                                    shiftSchedule = shiftSchedule
                                )
                                isScanningFingerprint = false
                                feedbackBanner = outcome
                                if (outcome.success) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    todayRecord = outcome.updatedRecord
                                    refreshAllData(probeGps = true)
                                } else if (outcome.requiresPermission) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    zoneProbe = ElectronicFingerprintZoneEngine.probeCurrentZone(
                                        context = context,
                                        config = geofenceConfig,
                                        holidayLabel = todayHolidayLabel,
                                        shiftSchedule = shiftSchedule
                                    )
                                }
                            }
                        }
                    )

                    Text(
                        text = when (fingerprintStage) {
                            FingerprintVisualStage.SCANNING -> "جارٍ المسح والتحقق..."
                            FingerprintVisualStage.READY_CHECK_IN -> "اضغط لتسجيل بصمة الحضور"
                            FingerprintVisualStage.READY_CHECK_OUT -> "اضغط لتسجيل بصمة الانصراف"
                            FingerprintVisualStage.COMPLETED_DAY ->
                                if (liveTodayRecord?.isManualOverride == true) {
                                    "تم التثبيت الإداري: ${staffStatusArabicLabel(liveTodayRecord.status)}"
                                } else {
                                    "اكتمل دوامك اليوم بنجاح"
                                }
                            FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY ->
                                if (holidayNotice != null) "عطلة رسمية" else "البصمة متوقفة"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = primaryButtonSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // ملخص بصمة اليوم (الحضور / الانصراف / الحالة)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TodayTimeStatChip(
                            label = "بصمة الحضور",
                            value = liveTodayRecord?.checkInTime?.takeIf { it.isNotBlank() }?.let {
                                StaffAttendanceDateUtils.formatTimeArabic12h(it)
                            } ?: "--:--",
                            accent = StaffAttendancePalette.PresentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        TodayTimeStatChip(
                            label = "بصمة الانصراف",
                            value = liveTodayRecord?.checkOutTime?.takeIf { it.isNotBlank() }?.let {
                                StaffAttendanceDateUtils.formatTimeArabic12h(it)
                            } ?: "--:--",
                            accent = DeepEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        TodayTimeStatChip(
                            label = "حالة اليوم",
                            value = todayResolvedStatus.arabicLabel,
                            accent = resolvedStaffStatusColor(todayResolvedStatus),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // رسالة نتيجة آخر محاولة بصمة (نجاح، خارج النطاق مع المسافة الدقيقة، أو رفض موقع وهمي)
                    AnimatedVisibility(visible = feedbackBanner != null) {
                        feedbackBanner?.let { outcome ->
                            val bannerColor = when {
                                outcome.success -> StaffAttendancePalette.PresentEmerald
                                outcome.isMockDetected -> StaffAttendancePalette.AbsentCoral
                                else -> StaffAttendancePalette.LateOrange
                            }
                            StatusNoticeBanner(
                                icon = if (outcome.success) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                                accent = bannerColor,
                                title = outcome.title,
                                message = outcome.message
                            )
                        }
                    }

                    // تفصيل المسافة الدقيقة إذا كان المستخدم خارج النطاق
                    if (!zoneProbe.isLoading && !zoneProbe.isHoliday && zoneProbe.hasLocationPermission && !zoneProbe.insideZone && feedbackBanner == null) {
                        StatusNoticeBanner(
                            icon = Icons.Filled.LocationOff,
                            accent = if (zoneProbe.isMockLocationDetected) StaffAttendancePalette.AbsentCoral else StaffAttendancePalette.LateOrange,
                            title = zoneProbe.statusHeadline,
                            message = zoneProbe.statusDetail
                        )
                    }
                }
            }
        }

        // 4. زر تقديم طلب إجازة واضح ومباشر
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showLeaveSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("staff_request_leave_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddCircleOutline,
                        contentDescription = "تقديم طلب إجازة",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تقديم طلب إجازة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. شريط التبويب الموزون (السجل الشهري | طلبات الإجازة)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BalancedTabPill(
                    title = "سجل الحضور الشهري (${monthlyHistory.size})",
                    selected = selectedHistoryTab == 0,
                    onClick = { selectedHistoryTab = 0 },
                    modifier = Modifier.weight(1f),
                    testTag = "staff_tab_monthly_log"
                )
                BalancedTabPill(
                    title = "طلبات الإجازة (${myLeaveRequests.size})",
                    selected = selectedHistoryTab == 1,
                    onClick = { selectedHistoryTab = 1 },
                    modifier = Modifier.weight(1f),
                    testTag = "staff_tab_my_leaves"
                )
            }
        }

        // 6. محتوى السجل الشهري أو طلبات الإجازة
        if (selectedHistoryTab == 0) {
            if (monthlyHistory.isEmpty()) {
                item {
                    EmptyStaffSectionCard(
                        title = "لا توجد بصمات مسجلة لهذا الشهر بعد",
                        subtitle = "ستظهر هنا جميع بصمات الحضور والانصراف والتعديلات الإدارية الخاصة بك لشهر ${
                            StaffAttendanceDateUtils.formatArabicMonthTitle(currentMonthIso)
                        }."
                    )
                }
            } else {
                items(monthlyHistory, key = { "${it.dateIso}_${it.staffId}" }) { rec ->
                    StaffMonthlyRecordCard(
                        record = rec,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            if (myLeaveRequests.isEmpty()) {
                item {
                    EmptyStaffSectionCard(
                        title = "لا توجد طلبات إجازة مقدمة",
                        subtitle = "يمكنك تقديم طلب إجازة اعتيادية أو مرضية أو زمنية ومتابعة قرار الإدارة من هنا."
                    )
                }
            } else {
                items(myLeaveRequests, key = { it.leaveId }) { leave ->
                    StaffLeaveHistoryCard(
                        leave = leave,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // نافذة تقديم طلب إجازة
    if (showLeaveSheet) {
        StaffLeaveRequestBottomSheet(
            staffId = staffId,
            staffName = staffName,
            onDismiss = { showLeaveSheet = false },
            onSubmit = { req ->
                scope.launch {
                    StaffAttendanceCloudStore.submitLeaveRequest(context, schoolCode, req)
                    showLeaveSheet = false
                    selectedHistoryTab = 1
                    refreshAllData(probeGps = false)
                }
            }
        )
    }
}

@Composable
private fun StaffEmbeddedShiftScheduleCard(
    shiftSchedule: SchoolShiftScheduleConfig,
    todayResolvedStatus: ResolvedStaffDayStatus,
    liveTodayRecord: StaffAttendanceRecord?,
    holidayNotice: String?,
    todayHolidayLabel: String?,
    nowMinutes: Int,
    modifier: Modifier = Modifier
) {
    val isHoliday = holidayNotice != null || todayResolvedStatus == ResolvedStaffDayStatus.HOLIDAY
    val isEvening = shiftSchedule.resolvedShiftType == SchoolShiftType.EVENING
    val range12h = StaffAttendanceDateUtils.formatShiftRangeArabic12h(
        shiftSchedule.shiftStart,
        shiftSchedule.shiftEnd
    )

    val shiftHeadline = when {
        isHoliday -> "اليوم عطلة رسمية"
        !shiftSchedule.isConfigured -> "دوام اليوم: بانتظار ضبط الإدارة"
        else -> "دوام اليوم: ${shiftSchedule.resolvedShiftType.arabicLabel} • $range12h"
    }

    data class GraceBannerInfo(
        val badgeText: String,
        val detailText: String,
        val accentColor: Color,
        val badgeBg: Color,
        val badgeBorder: Color
    )

    val graceInfo = when {
        isHoliday -> GraceBannerInfo(
            badgeText = "عطلة رسمية",
            detailText = todayHolidayLabel?.let { "مناسبة اليوم: $it • لا يتطلب تسجيل بصمة" }
                ?: "عطلة أسبوعية معتمدة في جدول الدوام الموحّد",
            accentColor = StaffAttendancePalette.HolidayTeal,
            badgeBg = Color(0xFFECFEFF),
            badgeBorder = Color(0xFFA5F3FC)
        )
        !shiftSchedule.isConfigured -> GraceBannerInfo(
            badgeText = "بانتظار الضبط",
            detailText = "سيظهر توقيت الدوام ومهل التأخير والغياب فور تثبيتها من مدير المدرسة",
            accentColor = StaffAttendancePalette.NeutralSlate,
            badgeBg = Color(0xFFF1F5F9),
            badgeBorder = Color(0xFFCBD5E1)
        )
        todayResolvedStatus == ResolvedStaffDayStatus.LEAVE -> GraceBannerInfo(
            badgeText = "مجاز اليوم",
            detailText = "لديك إجازة معتمدة اليوم • لا يُحتسب تأخير أو غياب",
            accentColor = StaffAttendancePalette.LeaveAmber,
            badgeBg = Color(0xFFFFFBEB),
            badgeBorder = Color(0xFFFDE68A)
        )
        liveTodayRecord?.isManualOverride == true -> GraceBannerInfo(
            badgeText = "مثبّت إدارياً",
            detailText = "حالة الدوام المثبّتة من الإدارة: ${todayResolvedStatus.arabicLabel}",
            accentColor = resolvedStaffStatusColor(todayResolvedStatus),
            badgeBg = resolvedStaffStatusBgColor(todayResolvedStatus),
            badgeBorder = resolvedStaffStatusBorderColor(todayResolvedStatus)
        )
        liveTodayRecord != null && liveTodayRecord.hasCheckedIn && liveTodayRecord.hasCheckedOut -> GraceBannerInfo(
            badgeText = "اكتمل الدوام",
            detailText = "تم توثيق الحضور (${StaffAttendanceDateUtils.formatTimeArabic12h(liveTodayRecord.checkInTime)}) والانصراف (${StaffAttendanceDateUtils.formatTimeArabic12h(liveTodayRecord.checkOutTime)})",
            accentColor = StaffAttendancePalette.PresentEmerald,
            badgeBg = Color(0xFFECFDF5),
            badgeBorder = Color(0xFFA7F3D0)
        )
        liveTodayRecord != null && liveTodayRecord.hasCheckedIn -> {
            val remToEnd = (shiftSchedule.shiftEndMinutes - nowMinutes).coerceAtLeast(0)
            val isLateCheckIn = todayResolvedStatus == ResolvedStaffDayStatus.LATE
            GraceBannerInfo(
                badgeText = if (isLateCheckIn) "حاضر بتأخير" else "حاضر في الموعد",
                detailText = if (remToEnd > 0) {
                    "المتبقي على نهاية الدوام: ${formatStaffRemainingDuration(remToEnd)}"
                } else {
                    "انتهى وقت الدوام الرسمي • يمكنك تسجيل بصمة الانصراف"
                },
                accentColor = if (isLateCheckIn) StaffAttendancePalette.LateOrange else StaffAttendancePalette.PresentEmerald,
                badgeBg = if (isLateCheckIn) Color(0xFFFFF7ED) else Color(0xFFECFDF5),
                badgeBorder = if (isLateCheckIn) Color(0xFFFED7AA) else Color(0xFFA7F3D0)
            )
        }
        nowMinutes < shiftSchedule.shiftStartMinutes -> {
            val remToStart = shiftSchedule.shiftStartMinutes - nowMinutes
            GraceBannerInfo(
                badgeText = "لم يبدأ الدوام",
                detailText = "المتبقي لبدء الدوام: ${formatStaffRemainingDuration(remToStart)}",
                accentColor = StaffAttendancePalette.NeutralSlate,
                badgeBg = Color(0xFFF1F5F9),
                badgeBorder = Color(0xFFCBD5E1)
            )
        }
        nowMinutes <= shiftSchedule.lateDeadlineMinutes -> {
            val remBeforeLate = (shiftSchedule.lateDeadlineMinutes - nowMinutes).coerceAtLeast(0)
            GraceBannerInfo(
                badgeText = "ضمن مهلة الحضور",
                detailText = if (remBeforeLate > 0) {
                    "المتبقي قبل احتساب التأخير: ${formatStaffRemainingDuration(remBeforeLate)}"
                } else {
                    "الدقيقة الأخيرة قبل احتساب التأخير • سجّل بصمتك الآن"
                },
                accentColor = StaffAttendancePalette.PresentEmerald,
                badgeBg = Color(0xFFECFDF5),
                badgeBorder = Color(0xFFA7F3D0)
            )
        }
        nowMinutes < shiftSchedule.absenceDeadlineMinutes -> {
            val remBeforeAbsent = shiftSchedule.absenceDeadlineMinutes - nowMinutes
            GraceBannerInfo(
                badgeText = "مهلة قبل الغياب",
                detailText = "المتبقي قبل احتساب الغياب: ${formatStaffRemainingDuration(remBeforeAbsent)}",
                accentColor = StaffAttendancePalette.LeaveAmber,
                badgeBg = Color(0xFFFFFBEB),
                badgeBorder = Color(0xFFFDE68A)
            )
        }
        else -> {
            val hoursLabel = StaffAttendanceDateUtils.formatArabicAbsenceHours(shiftSchedule.absenceAfterHours)
            GraceBannerInfo(
                badgeText = "انتهت مهلة الحضور",
                detailText = "انقضت مهلة احتساب الغياب ($hoursLabel من بدء الدوام)",
                accentColor = StaffAttendancePalette.AbsentCoral,
                badgeBg = Color(0xFFFEF2F2),
                badgeBorder = Color(0xFFFECACA)
            )
        }
    }

    val goldAccent = Color(0xFFD97706)
    val goldLight = Color(0xFFF59E0B)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .testTag("staff_embedded_shift_info_card"),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF4FBF7),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    DeepEmerald.copy(alpha = 0.24f),
                    goldAccent.copy(alpha = 0.34f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val barW = 3.8.dp.toPx()
                    val barX = if (layoutDirection == LayoutDirection.Rtl) size.width - barW else 0f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(goldLight, goldAccent, DeepEmerald)
                        ),
                        topLeft = Offset(barX, 8.dp.toPx()),
                        size = Size(barW, (size.height - 16.dp.toPx()).coerceAtLeast(0f)),
                        cornerRadius = CornerRadius(barW, barW)
                    )
                }
                .padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFECFDF5)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    StaffShiftGeometricIconCanvas(
                        isHoliday = isHoliday,
                        isEvening = isEvening,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = shiftHeadline,
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF064E3B),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (shiftSchedule.isConfigured && !isHoliday) {
                        val absHoursText = StaffAttendanceDateUtils.formatArabicAbsenceHours(shiftSchedule.absenceAfterHours)
                        Text(
                            text = "مهلة التأخير: ${shiftSchedule.lateGraceMinutes} د • الغياب بعد $absHoursText",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = graceInfo.badgeBg,
                    border = BorderStroke(1.dp, graceInfo.badgeBorder)
                ) {
                    Text(
                        text = graceInfo.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = graceInfo.accentColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = graceInfo.badgeBg.copy(alpha = 0.72f),
                border = BorderStroke(0.9.dp, graceInfo.badgeBorder.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "حالة المهلة المتبقية",
                        tint = graceInfo.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = graceInfo.detailText,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffShiftGeometricIconCanvas(
    isHoliday: Boolean,
    isEvening: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = size.minDimension * 0.42f

        when {
            isHoliday -> {
                val frameLeft = w * 0.14f
                val frameTop = h * 0.22f
                val frameW = w * 0.72f
                val frameH = h * 0.66f
                drawRoundRect(
                    color = Color(0xFF0E7490).copy(alpha = 0.12f),
                    topLeft = Offset(frameLeft, frameTop),
                    size = Size(frameW, frameH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = DeepEmerald,
                    topLeft = Offset(frameLeft, frameTop),
                    size = Size(frameW, frameH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                drawLine(
                    color = Color(0xFFD97706),
                    start = Offset(frameLeft + 2.dp.toPx(), frameTop + frameH * 0.30f),
                    end = Offset(frameLeft + frameW - 2.dp.toPx(), frameTop + frameH * 0.30f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // مسمارا التقويم العلويان
                drawLine(
                    color = DeepEmerald,
                    start = Offset(frameLeft + frameW * 0.28f, frameTop - 3.dp.toPx()),
                    end = Offset(frameLeft + frameW * 0.28f, frameTop + 2.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = DeepEmerald,
                    start = Offset(frameLeft + frameW * 0.72f, frameTop - 3.dp.toPx()),
                    end = Offset(frameLeft + frameW * 0.72f, frameTop + 2.dp.toPx()),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // علامة وسطية هادئة
                drawCircle(
                    color = Color(0xFFD97706),
                    radius = 2.4.dp.toPx(),
                    center = Offset(cx, frameTop + frameH * 0.65f)
                )
            }
            isEvening -> {
                // هالة مسائية زمردية ناعمة
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF059669).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = r * 1.15f
                    ),
                    radius = r * 1.15f,
                    center = Offset(cx, cy)
                )
                // هلال هندسي مصقول (بدون نجمات ثلاثية)
                val crescentPath = Path().apply {
                    moveTo(cx + r * 0.25f, cy - r * 0.84f)
                    cubicTo(
                        cx - r * 0.68f, cy - r * 0.72f,
                        cx - r * 0.72f, cy + r * 0.72f,
                        cx + r * 0.32f, cy + r * 0.84f
                    )
                    cubicTo(
                        cx - r * 0.20f, cy + r * 0.44f,
                        cx - r * 0.20f, cy - r * 0.44f,
                        cx + r * 0.25f, cy - r * 0.84f
                    )
                    close()
                }
                drawPath(
                    path = crescentPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFFD97706))
                    )
                )
                drawPath(
                    path = crescentPath,
                    color = Color(0xFFD97706).copy(alpha = 0.65f),
                    style = Stroke(width = 1.1.dp.toPx(), join = StrokeJoin.Round)
                )
                // قوس هالة رفيع متوازن
                drawArc(
                    color = DeepEmerald.copy(alpha = 0.35f),
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.92f, cy - r * 0.92f),
                    size = Size(r * 1.84f, r * 1.84f),
                    style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            else -> {
                // شمس هندسية ناعمة للدوام الصباحي
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.26f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = r * 1.15f
                    ),
                    radius = r * 1.15f,
                    center = Offset(cx, cy)
                )
                for (i in 0 until 8) {
                    val angle = (i * 45.0) * PI / 180.0
                    val innerR = r * 0.66f
                    val outerR = r * 0.98f
                    drawLine(
                        color = if (i % 2 == 0) Color(0xFFD97706) else DeepEmerald.copy(alpha = 0.78f),
                        start = Offset(
                            cx + (innerR * cos(angle)).toFloat(),
                            cy + (innerR * sin(angle)).toFloat()
                        ),
                        end = Offset(
                            cx + (outerR * cos(angle)).toFloat(),
                            cy + (outerR * sin(angle)).toFloat()
                        ),
                        strokeWidth = 1.7.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                    ),
                    radius = r * 0.46f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = DeepEmerald.copy(alpha = 0.65f),
                    radius = r * 0.46f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.1.dp.toPx())
                )
            }
        }
    }
}

private fun formatStaffRemainingDuration(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(1)
    val hours = safe / 60
    val mins = safe % 60
    val hoursPart = when (hours) {
        0 -> ""
        1 -> "ساعة واحدة"
        2 -> "ساعتين"
        in 3..10 -> "$hours ساعات"
        else -> "$hours ساعة"
    }
    val minsPart = when (mins) {
        0 -> ""
        1 -> "دقيقة واحدة"
        2 -> "دقيقتين"
        in 3..10 -> "$mins دقائق"
        else -> "$mins دقيقة"
    }
    return when {
        hoursPart.isNotEmpty() && minsPart.isNotEmpty() -> "$hoursPart و$minsPart"
        hoursPart.isNotEmpty() -> hoursPart
        else -> minsPart
    }
}

@Composable
private fun TodayTimeStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun StatusNoticeBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
internal fun BalancedTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) DeepEmerald else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) DeepEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StaffMonthlyRecordCard(
    record: StaffAttendanceRecord,
    modifier: Modifier = Modifier
) {
    val statusColor = staffStatusColor(record.status)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCleanDate(record.dateIso),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = staffStatusArabicLabel(record.status),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = "الحضور",
                        tint = DeepEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "الحضور: ${record.checkInTime.ifBlank { "--:--" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "الانصراف",
                        tint = DeepEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "الانصراف: ${record.checkOutTime.ifBlank { "--:--" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GppGood,
                        contentDescription = "طريقة التوثيق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = recordMethodArabicLabel(record.recordMethod, record.isManualOverride),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!record.isManualOverride && record.distanceMeters >= 0 && record.hasCheckedIn) {
                    Text(
                        text = "المسافة: ${record.distanceMeters} م",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (record.adminNotes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StaffAttendancePalette.LeaveAmber.copy(alpha = 0.09f)
                ) {
                    Text(
                        text = "ملاحظة الإدارة: ${record.adminNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffLeaveHistoryCard(
    leave: StaffLeaveRequest,
    modifier: Modifier = Modifier
) {
    val badgeColor = leaveStatusColor(leave.status)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجازة ${leave.leaveType} • ${leave.daysCount} يوم",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.13f)
                ) {
                    Text(
                        text = leaveStatusArabicLabel(leave.status),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "تاريخ البدء: ${formatCleanDate(leave.startDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (leave.reason.isNotBlank()) {
                Text(
                    text = "السبب: ${leave.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (leave.adminNote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "رد الإدارة: ${leave.adminNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyStaffSectionCard(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(DeepEmerald.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EventNote,
                    contentDescription = title,
                    tint = DeepEmerald,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffLeaveRequestBottomSheet(
    staffId: String,
    staffName: String,
    onDismiss: () -> Unit,
    onSubmit: (StaffLeaveRequest) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val leaveTypes = remember { listOf("اعتيادية", "مرضية", "زمنية") }
    var selectedType by remember { mutableStateOf("اعتيادية") }
    var startDateIso by remember { mutableStateOf(StaffAttendanceDateUtils.todayIso()) }
    var daysCount by remember { mutableIntStateOf(1) }
    var reason by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "تقديم طلب إجازة رسمي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "نوع الإجازة",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leaveTypes.forEach { type ->
                    val isSelected = selectedType == type
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedType = type },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DeepEmerald else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) DeepEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            ModernCalendarDatePickerField(
                label = "تاريخ بدء الإجازة",
                selectedDateIso = startDateIso,
                onDateSelected = { newIso, _ -> startDateIso = newIso },
                modifier = Modifier.fillMaxWidth(),
                testTag = "leave_request_start_date_picker"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عدد الأيام",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (daysCount > 1) daysCount -= 1 },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        text = "$daysCount يوم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald
                    )
                    OutlinedButton(
                        onClick = { if (daysCount < 30) daysCount += 1 },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("سبب الإجازة / التوضيح") },
                placeholder = { Text("اكتب سبباً مختصراً للإدارة...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leave_request_reason_input"),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepEmerald
                )
            )

            Button(
                onClick = {
                    isSaving = true
                    onSubmit(
                        StaffLeaveRequest(
                            staffId = staffId,
                            staffName = staffName,
                            leaveType = selectedType,
                            startDate = startDateIso,
                            daysCount = daysCount,
                            reason = reason.trim().ifBlank { "طلب إجازة $selectedType" },
                            status = "pending"
                        )
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("leave_request_submit_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "إرسال الطلب إلى إدارة المدرسة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
