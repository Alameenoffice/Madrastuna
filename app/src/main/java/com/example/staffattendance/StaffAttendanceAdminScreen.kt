package com.example.staffattendance

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppSection
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.components.DailyStatusBadgeSpec
import com.example.ui.components.ModernCalendarDatePickerField
import com.example.ui.components.SmartWelcomeProfileAvatar
import com.example.ui.components.ThreeDHeroHeaderCard
import com.example.ui.components.formatCleanDate
import com.example.ui.components.resolveSmartAvatarPersona
import com.example.ui.theme.DeepEmerald
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceAdminScreen(
    viewModel: SchoolViewModel
) {
    BackHandler {
        viewModel.navigateToSection(AppSection.DASHBOARD)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val schoolConfig by viewModel.schoolConfig.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    val schoolCode = remember(schoolConfig.schoolId) {
        schoolConfig.schoolId.ifBlank { "default_school" }
    }
    val adminName = currentUser?.fullName.orEmpty().ifBlank { "مدير المدرسة" }

    // قائمة الكوادر التدريسية (استبعاد سائقي النقل المدرسي كما في تقييم الكادر)
    val teachingStaff = remember(accounts, schoolConfig) {
        accounts.filter {
            it.role == UserRole.TEACHER && !it.isDriverInSchoolConfig(schoolConfig)
        }.sortedBy { it.fullName }
    }

    // حالة إعدادات النطاق الجغرافي
    var isZoneSettingsExpanded by remember { mutableStateOf(false) }
    var isZoneEnabled by remember { mutableStateOf(true) }
    var locationLabel by remember { mutableStateOf("مقر المدرسة الرئيسي") }
    var zoneLat by remember { mutableDoubleStateOf(36.3489) }
    var zoneLon by remember { mutableDoubleStateOf(43.1577) }
    var zoneRadiusMeters by remember { mutableIntStateOf(120) }
    var wifiSsid by remember { mutableStateOf("") }
    var workStartTime by remember { mutableStateOf("07:30") }
    var lateAfterTime by remember { mutableStateOf("08:00") }
    var workEndTime by remember { mutableStateOf("14:30") }
    var pasteCoordsInput by remember { mutableStateOf("") }
    var isSavingZone by remember { mutableStateOf(false) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    var zoneFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // حالة التبويبات الثلاثة (0: الحضور اليومي، 1: طلبات الإجازات، 2: السجل الشهري)
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedDateIso by remember { mutableStateOf(StaffAttendanceDateUtils.todayIso()) }
    var selectedMonthIso by remember { mutableStateOf(StaffAttendanceDateUtils.currentMonthIso()) }

    var dailyRecordsMap by remember { mutableStateOf<Map<String, StaffAttendanceRecord>>(emptyMap()) }
    var allLeaveRequests by remember { mutableStateOf<List<StaffLeaveRequest>>(emptyList()) }
    var monthlyRecords by remember { mutableStateOf<List<StaffAttendanceRecord>>(emptyList()) }
    var isLoadingData by remember { mutableStateOf(true) }

    // التحكم اليدوي (ModalBottomSheet)
    var editingStaffTarget by remember { mutableStateOf<Pair<UserAccount, StaffAttendanceRecord?>?>(null) }
    var reviewingLeaveTarget by remember { mutableStateOf<Pair<StaffLeaveRequest, Boolean>?>(null) }
    var expandedMonthlyStaffId by remember { mutableStateOf<String?>(null) }

    fun loadAdminData(loadConfig: Boolean = false) {
        scope.launch {
            isLoadingData = true
            if (loadConfig) {
                val cfg = StaffAttendanceCloudStore.fetchGeofenceConfig(context, schoolCode)
                isZoneEnabled = cfg.isEnabled
                locationLabel = cfg.locationLabel
                zoneLat = cfg.latitude
                zoneLon = cfg.longitude
                zoneRadiusMeters = cfg.radiusMeters
                wifiSsid = cfg.schoolWifiSsid
                workStartTime = cfg.workStartTime
                lateAfterTime = cfg.lateAfterTime
                workEndTime = cfg.workEndTime
            }
            dailyRecordsMap = StaffAttendanceCloudStore.fetchDailyAttendance(context, schoolCode, selectedDateIso)
            allLeaveRequests = StaffAttendanceCloudStore.fetchLeaveRequests(context, schoolCode)
            monthlyRecords = StaffAttendanceCloudStore.fetchMonthlyAttendance(context, schoolCode, selectedMonthIso)
            isLoadingData = false
        }
    }

    fun captureAdminCurrentGps() {
        scope.launch {
            isCapturingLocation = true
            zoneFeedbackMessage = null
            val loc = ElectronicFingerprintZoneEngine.fetchFreshDeviceLocation(context)
            isCapturingLocation = false
            if (loc != null) {
                zoneLat = loc.latitude
                zoneLon = loc.longitude
                zoneFeedbackMessage = "تم التقاط موقعك الحالي بنجاح (${String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude)})"
            } else {
                zoneFeedbackMessage = "تعذّر التقاط موقع GPS حالياً. تأكد من تفعيل خدمة الموقع أو حدّد المدرسة مباشرة على الخريطة."
            }
        }
    }

    val adminLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            captureAdminCurrentGps()
        } else {
            zoneFeedbackMessage = "لم يتم منح إذن الموقع. يمكنك تحديد موقع المدرسة يدوياً من الخريطة أو لصق الإحداثيات."
        }
    }

    LaunchedEffect(schoolCode) {
        loadAdminData(loadConfig = true)
    }

    LaunchedEffect(selectedDateIso) {
        dailyRecordsMap = StaffAttendanceCloudStore.fetchDailyAttendance(context, schoolCode, selectedDateIso)
    }

    LaunchedEffect(selectedMonthIso) {
        monthlyRecords = StaffAttendanceCloudStore.fetchMonthlyAttendance(context, schoolCode, selectedMonthIso)
    }

    // إحصائيات اليوم للأقسام الأربعة
    val dailyStats = remember(teachingStaff, dailyRecordsMap) {
        var present = 0
        var late = 0
        var leave = 0
        var absent = 0
        teachingStaff.forEach { staff ->
            val rec = dailyRecordsMap[staff.id]
            when (rec?.status?.lowercase(Locale.US)) {
                "present" -> present++
                "late" -> late++
                "leave" -> leave++
                else -> absent++
            }
        }
        listOf(present, late, leave, absent)
    }

    val pendingLeavesCount = remember(allLeaveRequests) {
        allLeaveRequests.count { it.status == "pending" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. الترويسة الزمردية مع وحدة البصمة والنبض الجغرافي + Crossfade
        item {
            ThreeDHeroHeaderCard(
                title = "حضور وإجازات الكوادر",
                subtitle = "إدارة البصمة الإلكترونية والنطاق الجغرافي والتحكم الإداري",
                rightPillText = "الكوادر: ${teachingStaff.size}",
                rightPillIcon = Icons.Filled.VerifiedUser,
                leftPillText = "نطاق ${zoneRadiusMeters} م",
                leftPillIcon = Icons.Filled.GpsFixed,
                customTrailingContent = {
                    AdminStaffFingerprintHeaderWidget()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. بطاقة إعدادات النطاق الجغرافي القابلة للطي (GPS Zone + خريطة Leaflet التفاعلية)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isZoneSettingsExpanded = !isZoneSettingsExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("admin_geofence_expand_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DeepEmerald.copy(alpha = 0.12f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "النطاق الجغرافي",
                                    tint = DeepEmerald,
                                    modifier = Modifier
                                        .padding(9.dp)
                                        .size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "إعدادات النطاق الجغرافي للمدرسة (GPS Zone)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isZoneEnabled) {
                                        "مفعّل • النطاق $zoneRadiusMeters م • ${locationLabel}"
                                    } else {
                                        "متوقف مؤقتاً • اضغط للتعديل والتفعيل"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isZoneEnabled) DeepEmerald else StaffAttendancePalette.LeaveAmber
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isZoneSettingsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "طي أو فتح الإعدادات",
                            tint = DeepEmerald
                        )
                    }

                    AnimatedVisibility(visible = isZoneSettingsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // مفتاح تفعيل/إيقاف البصمة الجغرافية
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تفعيل البصمة الجغرافية للكوادر",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "التحقق من تواجد الكادر داخل دائرة المدرسة عند البصمة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isZoneEnabled,
                                    onCheckedChange = { isZoneEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = DeepEmerald
                                    ),
                                    modifier = Modifier.testTag("admin_geofence_enable_switch")
                                )
                            }

                            // خريطة Leaflet التفاعلية (قمر صناعي + شوارع + بحث + سحب الدبوس + دائرة النطاق)
                            SchoolGeofenceLeafletMap(
                                latitude = zoneLat,
                                longitude = zoneLon,
                                radiusMeters = zoneRadiusMeters,
                                onCoordinatesChanged = { newLat, newLon ->
                                    zoneLat = newLat
                                    zoneLon = newLon
                                    zoneFeedbackMessage = null
                                }
                            )

                            // أزرار المساعدة السريعة: التقاط موقعي الحالي + عرض الإحداثيات الحالية
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (ElectronicFingerprintZoneEngine.hasLocationPermission(context)) {
                                            captureAdminCurrentGps()
                                        } else {
                                            adminLocationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    enabled = !isCapturingLocation,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("admin_capture_my_location_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.2.dp, DeepEmerald)
                                ) {
                                    if (isCapturingLocation) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = DeepEmerald
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.MyLocation,
                                            contentDescription = "التقاط موقعي الحالي",
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "التقاط موقعي الحالي",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepEmerald
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DeepEmerald.copy(alpha = 0.08f)
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = String.format(Locale.US, "%.4f, %.4f", zoneLat, zoneLon),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                textDirection = TextDirection.Ltr
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = DeepEmerald,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                                        )
                                    }
                                }
                            }

                            // حقل ذكي للصق إحداثيات أو رابط خرائط Google Maps
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = pasteCoordsInput,
                                    onValueChange = { pasteCoordsInput = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_paste_coords_input"),
                                    label = { Text("لصق إحداثيات أو رابط خرائط") },
                                    placeholder = { Text("مثال: 36.3489, 43.1577 أو رابط Maps") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Link,
                                            contentDescription = "رابط خرائط",
                                            tint = DeepEmerald
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
                                )
                                Button(
                                    onClick = {
                                        val parsed = ElectronicFingerprintZoneEngine.parseCoordinatesFromText(pasteCoordsInput)
                                        if (parsed != null) {
                                            zoneLat = parsed.first
                                            zoneLon = parsed.second
                                            zoneFeedbackMessage = "تم استخراج الإحداثيات وتحريك الدبوس على الخريطة بنجاح."
                                            pasteCoordsInput = ""
                                        } else {
                                            zoneFeedbackMessage = "تعذّر قراءة الإحداثيات من النص الملصق. تأكد من وجود خط العرض والطول."
                                        }
                                    },
                                    enabled = pasteCoordsInput.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("admin_parse_coords_btn")
                                ) {
                                    Text("تطبيق", fontWeight = FontWeight.Bold)
                                }
                            }

                            // سلايدر نصف القطر (50 - 300 متر) مع تحديث الدائرة على الخريطة لحظياً
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "نصف قطر النطاق المسموح",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DeepEmerald.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "$zoneRadiusMeters متراً",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepEmerald,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Slider(
                                    value = zoneRadiusMeters.toFloat(),
                                    onValueChange = { zoneRadiusMeters = it.roundToInt().coerceIn(50, 300) },
                                    valueRange = 50f..300f,
                                    steps = 24,
                                    colors = SliderDefaults.colors(
                                        thumbColor = DeepEmerald,
                                        activeTrackColor = DeepEmerald,
                                        inactiveTrackColor = DeepEmerald.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.testTag("admin_radius_slider")
                                )
                            }

                            // تسمية الموقع + شبكة Wi-Fi الاحتياطية الاختيارية
                            OutlinedTextField(
                                value = locationLabel,
                                onValueChange = { locationLabel = it },
                                label = { Text("تسمية موقع المدرسة") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
                            )

                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("اسم شبكة Wi-Fi المدرسة (احتياطي اختياري)") },
                                placeholder = { Text("اتركه فارغاً للاعتماد على GPS فقط") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Wifi,
                                        contentDescription = "شبكة المدرسة",
                                        tint = DeepEmerald
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
                            )

                            // أوقات الدوام الرسمي والتأخير
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = workStartTime,
                                    onValueChange = { workStartTime = it },
                                    label = { Text("بدء الدوام") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
                                )
                                OutlinedTextField(
                                    value = lateAfterTime,
                                    onValueChange = { lateAfterTime = it },
                                    label = { Text("التأخير بعد") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
                                )
                                OutlinedTextField(
                                    value = workEndTime,
                                    onValueChange = { workEndTime = it },
                                    label = { Text("الانصراف") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
                                )
                            }

                            if (zoneFeedbackMessage != null) {
                                StatusNoticeBanner(
                                    icon = Icons.Filled.CheckCircle,
                                    accent = DeepEmerald,
                                    title = "تحديث النطاق",
                                    message = zoneFeedbackMessage.orEmpty()
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isSavingZone = true
                                        val cfg = SchoolGeofenceConfig(
                                            isEnabled = isZoneEnabled,
                                            locationLabel = locationLabel.trim().ifBlank { "مقر المدرسة الرئيسي" },
                                            latitude = zoneLat,
                                            longitude = zoneLon,
                                            radiusMeters = zoneRadiusMeters,
                                            schoolWifiSsid = wifiSsid.trim(),
                                            workStartTime = workStartTime.trim().ifBlank { "07:30" },
                                            lateAfterTime = lateAfterTime.trim().ifBlank { "08:00" },
                                            workEndTime = workEndTime.trim().ifBlank { "14:30" }
                                        )
                                        val ok = StaffAttendanceCloudStore.saveGeofenceConfig(context, schoolCode, cfg)
                                        isSavingZone = false
                                        zoneFeedbackMessage = if (ok) {
                                            "تم حفظ وتثبيت نطاق المدرسة سحابياً بنجاح."
                                        } else {
                                            "تم الحفظ محلياً وسيتم المزامنة فور توفر الاتصال."
                                        }
                                    }
                                },
                                enabled = !isSavingZone,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("admin_save_geofence_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                            ) {
                                if (isSavingZone) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Save,
                                        contentDescription = "حفظ النطاق",
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "حفظ وتثبيت نطاق المدرسة",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. شريط التبويبات الثلاثة المتوازن (Modifier.weight(1f))
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BalancedTabPill(
                    title = "الحضور اليومي",
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f),
                    testTag = "admin_staff_tab_daily"
                )
                BalancedTabPill(
                    title = if (pendingLeavesCount > 0) "الإجازات ($pendingLeavesCount)" else "طلبات الإجازات",
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f),
                    testTag = "admin_staff_tab_leaves"
                )
                BalancedTabPill(
                    title = "السجل الشهري",
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    modifier = Modifier.weight(1f),
                    testTag = "admin_staff_tab_monthly"
                )
            }
        }

        // =====================================================================
        // التبويب 1: الحضور اليومي
        // =====================================================================
        if (activeTab == 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernCalendarDatePickerField(
                        label = "تاريخ السجل",
                        selectedDateIso = selectedDateIso,
                        onDateSelected = { newIso, _ -> selectedDateIso = newIso },
                        modifier = Modifier.weight(1f),
                        testTag = "admin_daily_date_picker"
                    )
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { loadAdminData(loadConfig = false) }
                            .testTag("admin_refresh_daily_btn"),
                        shape = RoundedCornerShape(12.dp),
                        color = DeepEmerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "تحديث السجل",
                                tint = DeepEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 4 بطاقات إحصائية متساوية (حاضرون، متأخرون، مجازون، غائبون)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard(
                        title = "حاضرون",
                        count = dailyStats[0],
                        color = StaffAttendancePalette.PresentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "متأخرون",
                        count = dailyStats[1],
                        color = StaffAttendancePalette.LateOrange,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "مجازون",
                        count = dailyStats[2],
                        color = StaffAttendancePalette.LeaveAmber,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "غائبون",
                        count = dailyStats[3],
                        color = StaffAttendancePalette.AbsentCoral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isLoadingData && teachingStaff.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DeepEmerald)
                    }
                }
            } else if (teachingStaff.isEmpty()) {
                item {
                    EmptyStaffSectionCard(
                        title = "لا توجد حسابات للكوادر التدريسية حالياً",
                        subtitle = "أضف حسابات المعلمين من قسم إدارة الحسابات لمتابعة بصمتهم وحضورهم اليومي."
                    )
                }
            } else {
                items(teachingStaff, key = { it.id }) { staff ->
                    val rec = dailyRecordsMap[staff.id]
                    AdminDailyStaffRowCard(
                        staff = staff,
                        record = rec,
                        schoolGenderType = schoolConfig.schoolGenderType,
                        onManualEditClick = { editingStaffTarget = staff to rec },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // =====================================================================
        // التبويب 2: طلبات الإجازات
        // =====================================================================
        if (activeTab == 1) {
            if (allLeaveRequests.isEmpty()) {
                item {
                    EmptyStaffSectionCard(
                        title = "لا توجد طلبات إجازة من الكوادر",
                        subtitle = "عندما يقدّم أي معلم طلب إجازة سيظهر هنا للموافقة أو الرفض مع التسجيل التلقائي في السجل."
                    )
                }
            } else {
                items(allLeaveRequests, key = { it.leaveId }) { req ->
                    val staffAccount = teachingStaff.firstOrNull { it.id == req.staffId }
                    AdminLeaveRequestCard(
                        request = req,
                        staffAccount = staffAccount,
                        schoolGenderType = schoolConfig.schoolGenderType,
                        onApproveClick = { reviewingLeaveTarget = req to true },
                        onRejectClick = { reviewingLeaveTarget = req to false },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // =====================================================================
        // التبويب 3: السجل الشهري
        // =====================================================================
        if (activeTab == 2) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedMonthIso = StaffAttendanceDateUtils.shiftMonth(selectedMonthIso, -1) },
                            modifier = Modifier.testTag("admin_month_prev_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "الشهر السابق",
                                tint = DeepEmerald
                            )
                        }
                        Text(
                            text = StaffAttendanceDateUtils.formatArabicMonthTitle(selectedMonthIso),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepEmerald
                        )
                        IconButton(
                            onClick = { selectedMonthIso = StaffAttendanceDateUtils.shiftMonth(selectedMonthIso, 1) },
                            modifier = Modifier.testTag("admin_month_next_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "الشهر التالي",
                                tint = DeepEmerald
                            )
                        }
                    }
                }
            }

            if (teachingStaff.isEmpty()) {
                item {
                    EmptyStaffSectionCard(
                        title = "لا توجد بيانات كوادر للعرض الشهري",
                        subtitle = "سيظهر ملخص الحضور والتأخير والإجازات لكل عضو كادر شهرياً."
                    )
                }
            } else {
                items(teachingStaff, key = { "monthly_${it.id}" }) { staff ->
                    val staffMonthRecs = remember(monthlyRecords, staff.id) {
                        monthlyRecords.filter { it.staffId == staff.id }
                    }
                    AdminStaffMonthlySummaryCard(
                        staff = staff,
                        records = staffMonthRecs,
                        schoolGenderType = schoolConfig.schoolGenderType,
                        isExpanded = expandedMonthlyStaffId == staff.id,
                        onToggleExpand = {
                            expandedMonthlyStaffId = if (expandedMonthlyStaffId == staff.id) null else staff.id
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // نافذة التعديل الإداري اليدوي (ModalBottomSheet)
    editingStaffTarget?.let { (staff, existingRec) ->
        AdminManualOverrideBottomSheet(
            staff = staff,
            dateIso = selectedDateIso,
            existingRecord = existingRec,
            adminName = adminName,
            onDismiss = { editingStaffTarget = null },
            onSave = { updatedRecord ->
                scope.launch {
                    StaffAttendanceCloudStore.saveStaffAttendanceRecord(context, schoolCode, updatedRecord)
                    editingStaffTarget = null
                    loadAdminData(loadConfig = false)
                }
            }
        )
    }

    // نافذة اعتماد أو رفض طلب الإجازة مع ملاحظة إدارية
    reviewingLeaveTarget?.let { (leaveReq, approve) ->
        AdminLeaveDecisionBottomSheet(
            request = leaveReq,
            isApprove = approve,
            onDismiss = { reviewingLeaveTarget = null },
            onConfirm = { adminNote ->
                scope.launch {
                    StaffAttendanceCloudStore.decideLeaveRequest(
                        context = context,
                        schoolCode = schoolCode,
                        request = leaveReq,
                        approved = approve,
                        adminNote = adminNote,
                        adminName = adminName
                    )
                    reviewingLeaveTarget = null
                    loadAdminData(loadConfig = false)
                }
            }
        )
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminDailyStaffRowCard(
    staff: UserAccount,
    record: StaffAttendanceRecord?,
    schoolGenderType: String,
    onManualEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveStatus = record?.status ?: "absent"
    val statusColor = staffStatusColor(effectiveStatus)
    val persona = remember(staff.id, staff.gender, staff.fullName, schoolGenderType) {
        resolveSmartAvatarPersona(
            role = staff.role,
            gender = staff.gender,
            schoolGenderType = schoolGenderType,
            fullName = staff.fullName,
            isBusDriver = false
        )
    }
    val miniBadge = remember(effectiveStatus, statusColor) {
        DailyStatusBadgeSpec(
            label = staffStatusArabicLabel(effectiveStatus),
            ringColors = listOf(statusColor.copy(alpha = 0.7f), statusColor),
            pillBackground = statusColor,
            pillBorder = Color.White,
            pillTextColor = Color.White,
            dotColor = Color.White,
            pulseDot = effectiveStatus == "present"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmartWelcomeProfileAvatar(
                    photoSource = staff.photoBase64,
                    contentDescription = "صورة ${staff.fullName}",
                    persona = persona,
                    statusBadge = miniBadge,
                    onClick = onManualEditClick,
                    avatarSize = 56.dp,
                    showStatusBadge = false
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = staff.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = staff.specialty.ifBlank { "عضو كادر تدريسي" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.13f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.28f))
                ) {
                    Text(
                        text = staffStatusArabicLabel(effectiveStatus),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // تفاصيل وقت الحضور والانصراف وطريقة التوثيق
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = "وقت الحضور",
                            tint = DeepEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "حضور: ${record?.checkInTime?.ifBlank { "--:--" } ?: "--:--"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "وقت الانصراف",
                            tint = DeepEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "انصراف: ${record?.checkOutTime?.ifBlank { "--:--" } ?: "--:--"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (record != null) {
                    val methodText = if (!record.isManualOverride && record.recordMethod == "electronic_fingerprint_gps") {
                        "بصمة إلكترونية • ${record.distanceMeters} م"
                    } else {
                        "تعديل إداري"
                    }
                    Text(
                        text = methodText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (record.isManualOverride) StaffAttendancePalette.LeaveAmber else DeepEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!record?.adminNotes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StaffAttendancePalette.LeaveAmber.copy(alpha = 0.09f)
                ) {
                    Text(
                        text = "ملاحظة الإدارة: ${record?.adminNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onManualEditClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.45f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("admin_edit_staff_${staff.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "تعديل أو ملاحظة",
                        tint = DeepEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تعديل / ملاحظة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminLeaveRequestCard(
    request: StaffLeaveRequest,
    staffAccount: UserAccount?,
    schoolGenderType: String,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = leaveStatusColor(request.status)
    val persona = remember(staffAccount?.id, request.staffName, schoolGenderType) {
        resolveSmartAvatarPersona(
            role = UserRole.TEACHER,
            gender = staffAccount?.gender,
            schoolGenderType = schoolGenderType,
            fullName = request.staffName,
            isBusDriver = false
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val leaveAvatarBadge = remember(request.status, statusColor) {
                    DailyStatusBadgeSpec(
                        label = leaveStatusArabicLabel(request.status),
                        ringColors = listOf(statusColor.copy(alpha = 0.7f), statusColor),
                        pillBackground = statusColor,
                        pillBorder = Color.White,
                        pillTextColor = Color.White,
                        dotColor = Color.White,
                        pulseDot = false
                    )
                }
                SmartWelcomeProfileAvatar(
                    photoSource = staffAccount?.photoBase64,
                    contentDescription = "صورة ${request.staffName}",
                    persona = persona,
                    statusBadge = leaveAvatarBadge,
                    onClick = {},
                    avatarSize = 50.dp,
                    showStatusBadge = false
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.staffName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "إجازة ${request.leaveType} • ${request.daysCount} يوم • تبدأ ${formatCleanDate(request.startDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.13f)
                ) {
                    Text(
                        text = leaveStatusArabicLabel(request.status),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (request.reason.isNotBlank()) {
                Text(
                    text = "سبب الطلب: ${request.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (request.adminNote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "ملاحظة الإدارة: ${request.adminNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (request.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApproveClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("approve_leave_${request.leaveId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StaffAttendancePalette.PresentEmerald)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "موافقة",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("موافقة واعتماد", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRejectClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("reject_leave_${request.leaveId}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, StaffAttendancePalette.AbsentCoral)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "رفض",
                            tint = StaffAttendancePalette.AbsentCoral,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "رفض الطلب",
                            fontWeight = FontWeight.Bold,
                            color = StaffAttendancePalette.AbsentCoral
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStaffMonthlySummaryCard(
    staff: UserAccount,
    records: List<StaffAttendanceRecord>,
    schoolGenderType: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presentCount = remember(records) { records.count { it.status == "present" } }
    val lateCount = remember(records) { records.count { it.status == "late" } }
    val leaveCount = remember(records) { records.count { it.status == "leave" } }
    val absentCount = remember(records) { records.count { it.status == "absent" } }

    val persona = remember(staff.id, staff.gender, staff.fullName, schoolGenderType) {
        resolveSmartAvatarPersona(
            role = staff.role,
            gender = staff.gender,
            schoolGenderType = schoolGenderType,
            fullName = staff.fullName,
            isBusDriver = false
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val monthlyBadge = remember {
                    DailyStatusBadgeSpec(
                        label = "الكادر",
                        ringColors = listOf(DeepEmerald.copy(alpha = 0.75f), StaffAttendancePalette.PresentEmerald),
                        pillBackground = DeepEmerald,
                        pillBorder = Color.White,
                        pillTextColor = Color.White,
                        dotColor = Color.White,
                        pulseDot = false
                    )
                }
                SmartWelcomeProfileAvatar(
                    photoSource = staff.photoBase64,
                    contentDescription = "صورة ${staff.fullName}",
                    persona = persona,
                    statusBadge = monthlyBadge,
                    onClick = onToggleExpand,
                    avatarSize = 50.dp,
                    showStatusBadge = false
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = staff.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniCountPill("حضور $presentCount", StaffAttendancePalette.PresentEmerald)
                        MiniCountPill("تأخير $lateCount", StaffAttendancePalette.LateOrange)
                        MiniCountPill("إجازة $leaveCount", StaffAttendancePalette.LeaveAmber)
                        if (absentCount > 0) {
                            MiniCountPill("غياب $absentCount", StaffAttendancePalette.AbsentCoral)
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "عرض التفصيل",
                    tint = DeepEmerald
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    if (records.isEmpty()) {
                        Text(
                            text = "لا توجد سجلات موثّقة لهذا الشهر.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    } else {
                        records.forEach { rec ->
                            val c = staffStatusColor(rec.status)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = c.copy(alpha = 0.06f),
                                border = BorderStroke(0.8.dp, c.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = formatCleanDate(rec.dateIso),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${staffStatusArabicLabel(rec.status)} • ${
                                                recordMethodArabicLabel(rec.recordMethod, rec.isManualOverride)
                                            }",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = c
                                        )
                                    }
                                    Text(
                                        text = "الحضور: ${rec.checkInTime.ifBlank { "--:--" }}  |  الانصراف: ${rec.checkOutTime.ifBlank { "--:--" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (rec.adminNotes.isNotBlank()) {
                                        Text(
                                            text = "ملاحظة: ${rec.adminNotes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
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

@Composable
private fun MiniCountPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminManualOverrideBottomSheet(
    staff: UserAccount,
    dateIso: String,
    existingRecord: StaffAttendanceRecord?,
    adminName: String,
    onDismiss: () -> Unit,
    onSave: (StaffAttendanceRecord) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val statuses = remember {
        listOf(
            "present" to "حاضر",
            "late" to "متأخر",
            "leave" to "مجاز",
            "absent" to "غائب"
        )
    }

    var selectedStatus by remember { mutableStateOf(existingRecord?.status ?: "present") }
    var checkInTime by remember {
        mutableStateOf(
            existingRecord?.checkInTime?.ifBlank {
                if (selectedStatus == "present" || selectedStatus == "late") "07:45" else ""
            } ?: "07:45"
        )
    }
    var checkOutTime by remember { mutableStateOf(existingRecord?.checkOutTime.orEmpty()) }
    var adminNotes by remember { mutableStateOf(existingRecord?.adminNotes.orEmpty()) }
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
                text = "التحكم الإداري اليدوي • ${staff.fullName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "التاريخ: ${formatCleanDate(dateIso)} • أي حفظ هنا يثبّت الحالة بأولوية إدارية.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                statuses.forEach { (key, label) ->
                    val isSelected = selectedStatus == key
                    val c = staffStatusColor(key)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedStatus = key
                                if ((key == "absent" || key == "leave")) {
                                    checkInTime = ""
                                    checkOutTime = ""
                                } else if (checkInTime.isBlank()) {
                                    checkInTime = StaffAttendanceDateUtils.nowTimeHHmm()
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) c else c.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (isSelected) c else c.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else c
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = checkInTime,
                    onValueChange = { checkInTime = it },
                    label = { Text("وقت الحضور (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
                )
                OutlinedTextField(
                    value = checkOutTime,
                    onValueChange = { checkOutTime = it },
                    label = { Text("وقت الانصراف (HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
                )
            }

            OutlinedTextField(
                value = adminNotes,
                onValueChange = { adminNotes = it },
                label = { Text("ملاحظة إدارية (مثال: تكليف رسمي خارج المدرسة / عذر مقبول)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_manual_override_notes_input"),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepEmerald)
            )

            Button(
                onClick = {
                    isSaving = true
                    val now = System.currentTimeMillis()
                    onSave(
                        StaffAttendanceRecord(
                            staffId = staff.id,
                            staffName = staff.fullName,
                            role = staff.specialty.ifBlank { "معلم" },
                            dateIso = dateIso,
                            status = selectedStatus,
                            checkInTime = checkInTime.trim(),
                            checkOutTime = checkOutTime.trim(),
                            checkInTimestamp = if (checkInTime.isNotBlank()) (existingRecord?.checkInTimestamp?.takeIf { it > 0L } ?: now) else 0L,
                            checkOutTimestamp = if (checkOutTime.isNotBlank()) (existingRecord?.checkOutTimestamp?.takeIf { it > 0L } ?: now) else 0L,
                            recordMethod = "manual_admin",
                            distanceMeters = existingRecord?.distanceMeters ?: 0,
                            adminNotes = adminNotes.trim(),
                            isManualOverride = true,
                            updatedBy = adminName,
                            updatedAt = now
                        )
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_manual_override_save_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
            ) {
                Text(
                    text = "حفظ وتثبيت التعديل الإداري",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminLeaveDecisionBottomSheet(
    request: StaffLeaveRequest,
    isApprove: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (adminNote: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var adminNote by remember { mutableStateOf("") }
    val accent = if (isApprove) StaffAttendancePalette.PresentEmerald else StaffAttendancePalette.AbsentCoral

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
                text = if (isApprove) "اعتماد طلب الإجازة" else "رفض طلب الإجازة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = "${request.staffName} • إجازة ${request.leaveType} لمدة ${request.daysCount} يوم ابتداءً من ${formatCleanDate(request.startDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isApprove) {
                Text(
                    text = "سيتم تسجيل الموظف تلقائياً بحالة (مجاز) في سجل الحضور لجميع أيام الإجازة المحددة.",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeepEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedTextField(
                value = adminNote,
                onValueChange = { adminNote = it },
                label = { Text("ملاحظة الإدارة (اختياري)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent)
            )

            Button(
                onClick = { onConfirm(adminNote.trim()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_leave_decision_confirm_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(
                    text = if (isApprove) "تأكيد الموافقة وتسجيل الإجازة" else "تأكيد رفض الطلب",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
