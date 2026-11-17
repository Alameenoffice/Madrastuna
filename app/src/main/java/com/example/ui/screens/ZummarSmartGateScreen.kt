package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ZummarMainTab(val arabicTitle: String, val iconType: ZummarVectorIconType) {
    MAP_AND_GATE("خريطة وبوابة زمار", ZummarVectorIconType.MAP_COMPASS),
    ELECTRICITY("الكهرباء والمولدات", ZummarVectorIconType.ELECTRICITY_BOLT),
    WATER("دائرة الماء", ZummarVectorIconType.WATER_WAVE),
    HEALTH("الصحة والخافرة", ZummarVectorIconType.HEALTH_SHIELD),
    BAZAAR("سوق زمار الشامل", ZummarVectorIconType.BAZAAR_STORE)
}

@Composable
fun ZummarSmartGateApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialData = remember { ZummarRepository.loadLocalState(context) }
    var users by remember { mutableStateOf(initialData.users) }
    var zones by remember { mutableStateOf(initialData.zones) }
    var markers by remember { mutableStateOf(initialData.markers) }
    var products by remember { mutableStateOf(initialData.products) }
    var bookings by remember { mutableStateOf(initialData.bookings) }
    var messages by remember { mutableStateOf(initialData.messages) }

    var currentUser by remember { mutableStateOf<ZummarUserAccount?>(null) }
    var activeTab by remember { mutableStateOf(ZummarMainTab.MAP_AND_GATE) }
    var selectedMapLayer by remember { mutableStateOf(ZummarMapLayerFilter.ALL) }
    var isGateOpen by remember { mutableStateOf(true) }

    // حالة الرسم الجغرافي وتثبيت المواقع على خريطة زمار
    var drawMode by remember { mutableStateOf(ZummarZoneDrawMode.NONE) }
    var draftPolygonPoints by remember { mutableStateOf<List<ZummarPoint>>(emptyList()) }
    var showNameZoneDialog by remember { mutableStateOf(false) }
    var pendingPickedCoordinate by remember { mutableStateOf(ZummarPoint(0.50f, 0.50f)) }

    // النوافذ المنبثقة التفاعلية
    var showAccountDialog by remember { mutableStateOf(false) }
    var selectedZoneForSheet by remember { mutableStateOf<ZummarNeighborhoodZone?>(null) }
    var selectedMarkerForSheet by remember { mutableStateOf<ZummarLandmarkMarker?>(null) }
    var showAddProductDialogForShop by remember { mutableStateOf<ZummarLandmarkMarker?>(null) }
    var showBookClinicDialog by remember { mutableStateOf<ZummarLandmarkMarker?>(null) }
    var latestIssuedBooking by remember { mutableStateOf<ZummarBooking?>(null) }
    var activeChatMarker by remember { mutableStateOf<ZummarLandmarkMarker?>(null) }
    var showAdminAddMarkerDialog by remember { mutableStateOf(false) }
    var showPendingApprovalsDialog by remember { mutableStateOf(false) }

    // شريط التغذية الراجعة العائم
    var feedbackBanner by remember { mutableStateOf<ZummarFeedbackBanner?>(null) }

    fun triggerBanner(msg: String, isSuccess: Boolean = true) {
        feedbackBanner = ZummarFeedbackBanner(message = msg, isSuccess = isSuccess)
    }

    fun persistAll() {
        ZummarRepository.saveFullState(
            context = context,
            scope = scope,
            users = users,
            zones = zones,
            markers = markers,
            products = products,
            bookings = bookings,
            messages = messages
        )
    }

    LaunchedEffect(feedbackBanner?.id) {
        if (feedbackBanner != null) {
            delay(3800)
            feedbackBanner = null
        }
    }

    LaunchedEffect(Unit) {
        val remote = ZummarRepository.fetchRemoteStateIfAvailable(context)
        if (remote != null) {
            users = remote.users
            zones = remote.zones
            markers = remote.markers
            products = remote.products
            bookings = remote.bookings
            messages = remote.messages
        }
    }

    val canManageElectricity = currentUser?.role == ZummarRole.SUPREME_ADMIN || currentUser?.role == ZummarRole.ELECTRICITY_OFFICIAL
    val canManageWater = currentUser?.role == ZummarRole.SUPREME_ADMIN || currentUser?.role == ZummarRole.WATER_OFFICIAL
    val canDrawZones = canManageElectricity || canManageWater
    val isSupremeAdmin = currentUser?.role == ZummarRole.SUPREME_ADMIN

    val pendingMarkersCount = remember(markers, users) {
        markers.count { !it.isApproved } + users.count { !it.isApproved }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ZummarSurfaceBg,
            topBar = {
                Surface(
                    color = ZummarNavy,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ZummarEmerald)
                                        .border(1.2.dp, ZummarAmberGold, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ZummarCustomIcon(ZummarVectorIconType.MAP_COMPASS, ZummarAmberGold, Modifier.size(22.dp))
                                }
                                Column {
                                    Text(
                                        text = "بوابة زمار الذكية",
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = currentUser?.let { "${it.fullName} • ${it.role.arabicTitle}" }
                                            ?: "منصة ناحية زمار الخدمية والتجارية الموحدة",
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = ZummarAmberGold
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSupremeAdmin) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (pendingMarkersCount > 0) ZummarCrimson else ZummarEmerald,
                                        border = BorderStroke(1.dp, ZummarAmberGold),
                                        modifier = Modifier.clickable { showPendingApprovalsDialog = true }
                                    ) {
                                        Text(
                                            text = "الاعتمادات ($pendingMarkersCount)",
                                            fontFamily = ZummarTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ZummarEmerald,
                                    border = BorderStroke(1.dp, ZummarAmberGold.copy(alpha = 0.8f)),
                                    modifier = Modifier
                                        .clickable { showAccountDialog = true }
                                        .testTag("zummar_account_auth_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        ZummarCustomIcon(ZummarVectorIconType.USER_BADGE, ZummarAmberGold, Modifier.size(16.dp))
                                        Text(
                                            text = if (currentUser == null) "حسابي / تسجيل الدخول" else "حسابي",
                                            fontFamily = ZummarTajawalFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = ZummarNavy,
                    border = BorderStroke(1.dp, ZummarEmerald.copy(alpha = 0.5f)),
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZummarMainTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) ZummarEmerald else Color.Transparent)
                                    .clickable { activeTab = tab }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                ZummarCustomIcon(
                                    type = tab.iconType,
                                    tint = if (selected) ZummarAmberGold else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = tab.arabicTitle,
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    color = if (selected) ZummarAmberGold else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // البوابة المعمارية الترحيبية المفتوحة وشريط الحساب السريع
                    item {
                        InteractiveOpeningSmartGateCanvas(
                            isGateOpen = isGateOpen,
                            onToggleGate = { isGateOpen = !isGateOpen }
                        )
                    }

                    // شريط الإجراءات السريعة للمطور والإدارة العليا أو أصحاب المهن
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ZummarEmerald,
                                border = BorderStroke(1.dp, ZummarAmberGold),
                                modifier = Modifier.clickable { showAccountDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    ZummarCustomIcon(ZummarVectorIconType.USER_BADGE, ZummarAmberGold, Modifier.size(18.dp))
                                    Text(
                                        text = "حسابي / تسجيل الدخول وإنشاء حساب",
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            if (isSupremeAdmin || currentUser?.role == ZummarRole.MERCHANT || currentUser?.role == ZummarRole.HEALTH_PROVIDER) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ZummarNavy,
                                    border = BorderStroke(1.dp, ZummarAmberGold),
                                    modifier = Modifier.clickable { showAdminAddMarkerDialog = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                    ) {
                                        ZummarCustomIcon(ZummarVectorIconType.PLUS_DRAW, ZummarAmberGold, Modifier.size(18.dp))
                                        Text(
                                            text = if (isSupremeAdmin) "+ إضافة مولدة / دائرة / معلم للخريطة" else "+ تثبيت موقعي على خريطة زمار",
                                            fontFamily = ZummarTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = ZummarAmberGold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // عرض الخريطة التفاعلية في تبويب الخريطة أو عند اختيار موقع أو رسم حي
                    if (activeTab == ZummarMainTab.MAP_AND_GATE || drawMode != ZummarZoneDrawMode.NONE) {
                        item {
                            ZummarInteractiveMapSection(
                                zones = zones,
                                markers = markers,
                                selectedLayer = selectedMapLayer,
                                onSelectLayer = { selectedMapLayer = it },
                                canDrawZones = canDrawZones,
                                drawMode = drawMode,
                                onChangeDrawMode = { mode ->
                                    drawMode = mode
                                    if (mode == ZummarZoneDrawMode.NONE) {
                                        draftPolygonPoints = emptyList()
                                    }
                                },
                                draftPoints = draftPolygonPoints,
                                onAddDraftPoint = { pt ->
                                    draftPolygonPoints = draftPolygonPoints + pt
                                },
                                onClearDraftPoints = { draftPolygonPoints = emptyList() },
                                onFinishZoneDrawing = {
                                    if (draftPolygonPoints.size >= 3) {
                                        showNameZoneDialog = true
                                    } else {
                                        triggerBanner("يرجى تحديد 3 نقاط على الأقل لرسم حدود الحي الجغرافي", isSuccess = false)
                                    }
                                },
                                onZoneTapped = { zone ->
                                    selectedZoneForSheet = zone
                                },
                                onMarkerTapped = { marker ->
                                    selectedMarkerForSheet = marker
                                },
                                onMapCoordinatePicked = { pt ->
                                    pendingPickedCoordinate = pt
                                    drawMode = ZummarZoneDrawMode.NONE
                                    triggerBanner("تم التقاط إحداثيات الموقع على خريطة زمار بنجاح")
                                    showAdminAddMarkerDialog = true
                                }
                            )
                        }
                    }

                    // محتوى القسم النشط
                    when (activeTab) {
                        ZummarMainTab.MAP_AND_GATE -> {
                            item {
                                ZummarQuickOverviewSection(
                                    zones = zones,
                                    markers = markers.filter { it.isApproved },
                                    onSelectZone = { selectedZoneForSheet = it },
                                    onSelectMarker = { selectedMarkerForSheet = it }
                                )
                            }
                        }

                        ZummarMainTab.ELECTRICITY -> {
                            item {
                                ZummarElectricityDepartmentSection(
                                    zones = zones,
                                    generators = markers.filter { it.isApproved && it.type == ZummarMarkerType.GENERATOR },
                                    canManageElectricity = canManageElectricity,
                                    onUpdateZonePower = { updatedZone ->
                                        zones = zones.map { if (it.id == updatedZone.id) updatedZone else it }
                                        persistAll()
                                        triggerBanner("تم تحديث حالة الكهرباء في ${updatedZone.name}: ${updatedZone.powerStatus.arabicLabel}")
                                    },
                                    onStartDrawZoneOnMap = {
                                        activeTab = ZummarMainTab.MAP_AND_GATE
                                        selectedMapLayer = ZummarMapLayerFilter.ELECTRICITY
                                        drawMode = ZummarZoneDrawMode.POLYGON_POINTS
                                        triggerBanner("انقر على خريطة زمار لتحديد رؤوس مضلع الحي السكني")
                                    },
                                    onAddGenerator = { showAdminAddMarkerDialog = true }
                                )
                            }
                        }

                        ZummarMainTab.WATER -> {
                            item {
                                ZummarWaterDepartmentSection(
                                    zones = zones,
                                    canManageWater = canManageWater,
                                    onUpdateZoneWater = { updatedZone ->
                                        zones = zones.map { if (it.id == updatedZone.id) updatedZone else it }
                                        persistAll()
                                        triggerBanner("تم تحديث حالة ضخ الماء في ${updatedZone.name}: ${updatedZone.waterStatus.arabicLabel}")
                                    },
                                    onStartDrawWaterZone = {
                                        activeTab = ZummarMainTab.MAP_AND_GATE
                                        selectedMapLayer = ZummarMapLayerFilter.WATER
                                        drawMode = ZummarZoneDrawMode.POLYGON_POINTS
                                        triggerBanner("انقر على الخريطة لرسم قاطع ضخ الماء في ناحية زمار")
                                    }
                                )
                            }
                        }

                        ZummarMainTab.HEALTH -> {
                            item {
                                ZummarHealthDirectorySection(
                                    healthMarkers = markers.filter {
                                        it.isApproved && (it.type == ZummarMarkerType.PHARMACY || it.type == ZummarMarkerType.CLINIC || it.type == ZummarMarkerType.LAB)
                                    },
                                    bookings = bookings,
                                    canManageAll = isSupremeAdmin || currentUser?.role == ZummarRole.HEALTH_PROVIDER,
                                    onToggleOnDuty = { target ->
                                        markers = markers.map {
                                            if (it.id == target.id) it.copy(isOnDuty = !it.isOnDuty) else it
                                        }
                                        persistAll()
                                        triggerBanner("تم تحديث حالة الخفارة لـ ${target.title}")
                                    },
                                    onOpenChat = { activeChatMarker = it },
                                    onOpenBooking = { showBookClinicDialog = it }
                                )
                            }
                        }

                        ZummarMainTab.BAZAAR -> {
                            item {
                                ZummarBazaarSection(
                                    shops = markers.filter { it.isApproved && it.type == ZummarMarkerType.SHOP },
                                    products = products,
                                    canAddProduct = isSupremeAdmin || currentUser?.role == ZummarRole.MERCHANT,
                                    onSelectShop = { selectedMarkerForSheet = it },
                                    onAddProductToShop = { shop -> showAddProductDialogForShop = shop },
                                    onMessageShop = { shop -> activeChatMarker = shop }
                                )
                            }
                        }
                    }
                }

                // شريط التغذية الراجعة العائم في أعلى الشاشة
                ZummarFloatingFeedbackBanner(
                    banner = feedbackBanner,
                    onDismiss = { feedbackBanner = null },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        // ==========================================
        // النوافذ الحوارية والبطاقات التفاعلية الكاملة
        // ==========================================

        // 1. نافذة تسمية وحفظ الحي الجغرافي المرسوم على خريطة زمار
        if (showNameZoneDialog) {
            var newZoneName by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showNameZoneDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ZummarCardWhite,
                    border = BorderStroke(1.5.dp, ZummarEmerald)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تسمية الحي أو القاطع الجغرافي المرسوم",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ZummarNavy
                        )
                        OutlinedTextField(
                            value = newZoneName,
                            onValueChange = { newZoneName = it },
                            label = { Text("اسم الحي في ناحية زمار (مثال: حي العروبة)", fontFamily = ZummarTajawalFont) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { showNameZoneDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", fontFamily = ZummarTajawalFont, color = ZummarCrimson)
                            }
                            Button(
                                onClick = {
                                    val finalName = newZoneName.trim().ifBlank { "حي زمار الجديد ${zones.size + 1}" }
                                    val created = ZummarNeighborhoodZone(
                                        name = finalName,
                                        points = draftPolygonPoints
                                    )
                                    zones = zones + created
                                    draftPolygonPoints = emptyList()
                                    drawMode = ZummarZoneDrawMode.NONE
                                    showNameZoneDialog = false
                                    persistAll()
                                    triggerBanner("تم حفظ مضلع '$finalName' على خريطة زمار بنجاح")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("حفظ الحي", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 2. نافذة تفاصيل الحي السكني (الكهرباء + الماء + الصيانة) عند النقر عليه في الخريطة
        selectedZoneForSheet?.let { zone ->
            ZummarZoneDetailsDialog(
                zone = zone,
                canManageElectricity = canManageElectricity,
                canManageWater = canManageWater,
                onUpdateZone = { updated ->
                    zones = zones.map { if (it.id == updated.id) updated else it }
                    selectedZoneForSheet = updated
                    persistAll()
                    triggerBanner("تم حفظ تحديثات ${updated.name} فوراً")
                },
                onDismiss = { selectedZoneForSheet = null }
            )
        }

        // 3. نافذة تفاصيل المعلم / الصيدلية / العيادة / المحل التجاري مع انفتاح واجهة المتجر
        selectedMarkerForSheet?.let { marker ->
            ZummarMarkerDetailsDialog(
                marker = marker,
                shopProducts = products.filter { it.shopMarkerId == marker.id },
                canAddProduct = isSupremeAdmin || currentUser?.role == ZummarRole.MERCHANT,
                onOpenChat = {
                    selectedMarkerForSheet = null
                    activeChatMarker = marker
                },
                onOpenClinicBooking = {
                    selectedMarkerForSheet = null
                    showBookClinicDialog = marker
                },
                onOpenAddProduct = {
                    selectedMarkerForSheet = null
                    showAddProductDialogForShop = marker
                },
                onDismiss = { selectedMarkerForSheet = null }
            )
        }

        // 4. نافذة الحجز الإلكتروني في عيادات الأطباء مع التقويم التفاعلي
        showBookClinicDialog?.let { clinic ->
            ZummarClinicBookingDialog(
                clinic = clinic,
                defaultPatientName = currentUser?.fullName ?: "",
                defaultPatientPhone = currentUser?.usernameOrPhone ?: "",
                onConfirmBooking = { booking ->
                    bookings = listOf(booking) + bookings
                    showBookClinicDialog = null
                    latestIssuedBooking = booking
                    persistAll()
                    triggerBanner("تم تثبيت حجزك الإلكتروني في ${clinic.title} برقم ${booking.bookingCode}")
                },
                onDismiss = { showBookClinicDialog = null }
            )
        }

        // 5. بطاقة الحجز الرقمية الرسمية الصادرة للمريض
        latestIssuedBooking?.let { booking ->
            Dialog(onDismissRequest = { latestIssuedBooking = null }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ZummarNavy,
                    border = BorderStroke(2.dp, ZummarAmberGold)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.CHECK_VERIFIED, ZummarAmberGold, Modifier.size(42.dp))
                        Text(
                            text = "بطاقة حجز موعد طبي إلكتروني — بوابة زمار",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ZummarAmberGold,
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Text("العيادة: ${booking.clinicName}", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("الاختصاص: ${booking.doctorSpecialty}", fontFamily = ZummarTajawalFont, color = ZummarCyanLight)
                        Text("اسم المراجع: ${booking.patientName}", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("التاريخ والوقت: ${booking.appointmentDate} • ${booking.appointmentTime}", fontFamily = ZummarTajawalFont, color = ZummarAmberGold)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ZummarEmerald
                        ) {
                            Text(
                                text = "رمز الحجز المعتمد: ${booking.bookingCode}",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        Button(
                            onClick = { latestIssuedBooking = null },
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarAmberGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق والاحتفاظ بالبطاقة", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = ZummarNavy)
                        }
                    }
                }
            }
        }

        // 6. نافذة المراسلة الفورية أونلاين (للصيدليات والمختبرات والتجار) مع إرفاق صورة وصفة/منتج
        activeChatMarker?.let { targetMarker ->
            ZummarOnlineChatDialog(
                targetMarker = targetMarker,
                currentUser = currentUser,
                messages = messages.filter { it.targetMarkerId == targetMarker.id },
                onSendMessage = { text, imgBase64 ->
                    val timeFmt = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
                    val newMsg = ZummarChatMessage(
                        targetMarkerId = targetMarker.id,
                        targetTitle = targetMarker.title,
                        senderName = currentUser?.fullName ?: "مواطن من زمار",
                        senderRole = currentUser?.role?.arabicTitle ?: "مواطن",
                        messageText = text,
                        attachmentBase64 = imgBase64,
                        timestampLabel = timeFmt
                    )
                    messages = messages + newMsg
                    persistAll()
                    triggerBanner("تم إرسال رسالتك إلى ${targetMarker.title} بنجاح")
                },
                onDismiss = { activeChatMarker = null }
            )
        }

        // 7. نافذة نشر منتج أو بضاعة جديدة في سوق زمار الشامل
        showAddProductDialogForShop?.let { shop ->
            ZummarAddProductDialog(
                shop = shop,
                onPublishProduct = { prod ->
                    products = listOf(prod) + products
                    showAddProductDialogForShop = null
                    persistAll()
                    triggerBanner("تم نشر '${prod.productName}' في ${shop.title} وسوق زمار الشامل")
                },
                onDismiss = { showAddProductDialogForShop = null }
            )
        }

        // 8. نافذة تثبيت معلم جديد (مولدة أهلية / دائرة حكومية / صيدلية / عيادة / محل) على الخريطة
        if (showAdminAddMarkerDialog) {
            ZummarAddLandmarkMarkerDialog(
                isSupremeAdmin = isSupremeAdmin,
                initialCoord = pendingPickedCoordinate,
                onRequestPickOnMap = {
                    showAdminAddMarkerDialog = false
                    activeTab = ZummarMainTab.MAP_AND_GATE
                    drawMode = ZummarZoneDrawMode.PICK_MARKER_LOCATION
                },
                onSaveMarker = { newMarker ->
                    markers = markers + newMarker
                    showAdminAddMarkerDialog = false
                    persistAll()
                    val msg = if (newMarker.isApproved) {
                        "تم تثبيت '${newMarker.title}' على خريطة زمار بنجاح"
                    } else {
                        "تم إرسال طلب تثبيت '${newMarker.title}' بانتظار موافقة إدارة بوابة زمار"
                    }
                    triggerBanner(msg)
                },
                onDismiss = { showAdminAddMarkerDialog = false }
            )
        }

        // 9. نافذة طلبات الاعتماد بانتظار موافقة المطور والإدارة العليا
        if (showPendingApprovalsDialog) {
            ZummarAdminApprovalsDialog(
                pendingMarkers = markers.filter { !it.isApproved },
                pendingUsers = users.filter { !it.isApproved },
                onApproveMarker = { m ->
                    markers = markers.map { if (it.id == m.id) it.copy(isApproved = true) else it }
                    persistAll()
                    triggerBanner("تمت الموافقة على ظهور '${m.title}' في خريطة زمار")
                },
                onApproveUser = { u ->
                    users = users.map { if (it.id == u.id) it.copy(isApproved = true) else it }
                    persistAll()
                    triggerBanner("تم اعتماد حساب '${u.fullName}' رسمياً")
                },
                onDismiss = { showPendingApprovalsDialog = false }
            )
        }

        // 10. نافذة تسجيل الدخول وإنشاء حساب مستقل تماماً بدون Google/Gmail
        if (showAccountDialog) {
            ZummarAccountAuthDialog(
                currentUser = currentUser,
                users = users,
                onLoginSuccess = { loggedIn ->
                    currentUser = loggedIn
                    showAccountDialog = false
                    triggerBanner("أهلاً بك يا ${loggedIn.fullName} في بوابة زمار الذكية")
                },
                onRegisterAccount = { newAccount, createdMarker ->
                    users = users + newAccount
                    if (createdMarker != null) {
                        markers = markers + createdMarker
                    }
                    currentUser = newAccount
                    showAccountDialog = false
                    persistAll()
                    val note = if (newAccount.isApproved) {
                        "تم إنشاء حسابك وتفعيله فوراً في بوابة زمار الذكية"
                    } else {
                        "تم إنشاء حسابك وإرسال موقعك على الخريطة بانتظار موافقة الإدارة العليا"
                    }
                    triggerBanner(note)
                },
                onLogout = {
                    currentUser = null
                    showAccountDialog = false
                    triggerBanner("تم تسجيل الخروج من الحساب بنجاح")
                },
                onDismiss = { showAccountDialog = false }
            )
        }
    }
}
