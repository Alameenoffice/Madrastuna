package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay

enum class ZummarPortalTab(val title: String, val iconType: ZummarVectorIconType) {
    HOME_GATE("البوابة", ZummarVectorIconType.GOV_DOME),
    MAP_RADAR("الخريطة", ZummarVectorIconType.MAP_COMPASS),
    POWER_WATER("الكهرباء والماء", ZummarVectorIconType.ELECTRICITY_BOLT),
    HEALTH_CARE("الصحة", ZummarVectorIconType.HEALTH_SHIELD),
    MARKET_BAZAAR("السوق", ZummarVectorIconType.BAZAAR_STORE)
}

@Composable
fun ZummarSmartGateApp(
    schoolViewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialLoaded = remember { ZummarRepository.loadLocalState(context) }
    var users by remember { mutableStateOf(initialLoaded.users) }
    var zones by remember { mutableStateOf(initialLoaded.zones) }
    var markers by remember { mutableStateOf(initialLoaded.markers) }
    var products by remember { mutableStateOf(initialLoaded.products) }
    var bookings by remember { mutableStateOf(initialLoaded.bookings) }
    var messages by remember { mutableStateOf(initialLoaded.messages) }

    var currentUser by remember { mutableStateOf<ZummarUserAccount?>(null) }
    var activeTab by remember { mutableStateOf(ZummarPortalTab.HOME_GATE) }
    var isHeroGateOpen by remember { mutableStateOf(false) }

    // حالات الرسم والتثبيت على خريطة زمار
    var selectedMapLayer by remember { mutableStateOf(ZummarMapLayerFilter.ALL) }
    var mapDrawMode by remember { mutableStateOf(ZummarZoneDrawMode.NONE) }
    var draftPolygonPoints by remember { mutableStateOf<List<ZummarPoint>>(emptyList()) }
    var pickedMapCoord by remember { mutableStateOf(ZummarPoint(0.50f, 0.48f)) }
    var showSaveDrawnZoneDialog by remember { mutableStateOf(false) }
    var newDrawnZoneName by remember { mutableStateOf("") }

    // النوافذ الحوارية والبطاقات التفاعلية
    var selectedZoneForDialog by remember { mutableStateOf<ZummarNeighborhoodZone?>(null) }
    var selectedMarkerForDialog by remember { mutableStateOf<ZummarLandmarkMarker?>(null) }
    var activeChatMarker by remember { mutableStateOf<ZummarLandmarkMarker?>(null) }
    var activeBookingClinic by remember { mutableStateOf<ZummarLandmarkMarker?>(null) }
    var activeAddProductShop by remember { mutableStateOf<ZummarLandmarkMarker?>(null) }
    var showAddLandmarkDialog by remember { mutableStateOf(false) }
    var showAccountAuthDialog by remember { mutableStateOf(false) }
    var showAdminApprovalsDialog by remember { mutableStateOf(false) }
    var feedbackBanner by remember { mutableStateOf<ZummarFeedbackBanner?>(null) }

    fun triggerBanner(msg: String, isSuccess: Boolean = true) {
        feedbackBanner = ZummarFeedbackBanner(message = msg, isSuccess = isSuccess)
    }

    fun persistAll(
        newUsers: List<ZummarUserAccount> = users,
        newZones: List<ZummarNeighborhoodZone> = zones,
        newMarkers: List<ZummarLandmarkMarker> = markers,
        newProducts: List<ZummarProduct> = products,
        newBookings: List<ZummarBooking> = bookings,
        newMessages: List<ZummarChatMessage> = messages
    ) {
        users = newUsers
        zones = newZones
        markers = newMarkers
        products = newProducts
        bookings = newBookings
        messages = newMessages
        ZummarRepository.saveFullState(
            context = context,
            scope = scope,
            users = newUsers,
            zones = newZones,
            markers = newMarkers,
            products = newProducts,
            bookings = newBookings,
            messages = newMessages
        )
    }

    // فتح مصراعي بوابة زمار تلقائياً عند التشغيل ومزامنة السحابة
    LaunchedEffect(Unit) {
        delay(220)
        isHeroGateOpen = true
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

    LaunchedEffect(feedbackBanner?.id) {
        if (feedbackBanner != null) {
            delay(4200)
            feedbackBanner = null
        }
    }

    // دالة إطلاق تطبيق مدرستي الذكية المستقل عبر Intent رسمي
    fun launchStandaloneMadrasatiApp() {
        try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.example.madrasati")
                ?: pm.getLaunchIntentForPackage("com.madrasati.smart")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                triggerBanner(
                    msg = "تطبيق مدرستي الذكية المستقل غير مثبت حالياً على هذا الجهاز — يرجى تثبيت حزمة تطبيق مدرستي الذكية المستقلة",
                    isSuccess = false
                )
            }
        } catch (_: Exception) {
            triggerBanner(
                msg = "تعذر فتح تطبيق مدرستي الذكية المستقل، يرجى التأكد من تثبيته على الهاتف",
                isSuccess = false
            )
        }
    }

    val isSupremeAdmin = currentUser?.role == ZummarRole.SUPREME_ADMIN
    val canManageElectricity = isSupremeAdmin ||
        currentUser?.role == ZummarRole.ELECTRICITY_OFFICIAL ||
        currentUser?.role == ZummarRole.GENERATOR_OWNER
    val canManageWater = isSupremeAdmin || currentUser?.role == ZummarRole.WATER_OFFICIAL
    val canAddProduct = isSupremeAdmin || currentUser?.role == ZummarRole.MERCHANT
    val canPinLocation = currentUser != null

    val approvedMarkers = remember(markers, isSupremeAdmin) {
        if (isSupremeAdmin) markers else markers.filter { it.isApproved }
    }
    val pendingMarkers = remember(markers) { markers.filter { !it.isApproved } }
    val pendingUsers = remember(users) { users.filter { !it.isApproved } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Color(0xFFF4FAF7),
            topBar = {
                Surface(
                    color = ZummarEmerald,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ZummarNavy)
                                        .border(1.5.dp, ZummarAmberGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ZummarCustomIcon(
                                        type = ZummarVectorIconType.GOV_DOME,
                                        tint = ZummarAmberGold,
                                        modifier = Modifier.size(22.dp)
                                    )
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
                                            ?: "المنصة الرقمية الموحدة لخدمات ناحية زمار",
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.Bold,
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
                                        color = ZummarAmberGold,
                                        modifier = Modifier.clickable { showAdminApprovalsDialog = true }
                                    ) {
                                        Text(
                                            text = "الاعتمادات (${pendingMarkers.size + pendingUsers.size})",
                                            fontFamily = ZummarTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = ZummarNavy,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ZummarNavy,
                                    border = BorderStroke(1.2.dp, ZummarAmberGold),
                                    modifier = Modifier.clickable { showAccountAuthDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ZummarCustomIcon(
                                            type = if (isSupremeAdmin) ZummarVectorIconType.ADMIN_CROWN else ZummarVectorIconType.USER_BADGE,
                                            tint = ZummarAmberGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (currentUser == null) "حسابي / الإدارة" else "الحساب",
                                            fontFamily = ZummarTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
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
                    border = BorderStroke(1.dp, ZummarAmberGold.copy(alpha = 0.4f)),
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZummarPortalTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) ZummarEmerald else Color.Transparent,
                                border = if (selected) BorderStroke(1.dp, ZummarAmberGold) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .clickable { activeTab = tab }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    ZummarCustomIcon(
                                        type = tab.iconType,
                                        tint = if (selected) ZummarAmberGold else Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tab.title,
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (selected) ZummarAmberGold else Color.White.copy(alpha = 0.85f)
                                    )
                                }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (activeTab) {
                        ZummarPortalTab.HOME_GATE -> {
                            // 1. صرح بوابة زمار المعماري المتحرك (4 طبقات فنية)
                            InteractiveOpeningSmartGateCanvas(
                                isGateOpen = isHeroGateOpen,
                                onToggleGate = { isHeroGateOpen = !isHeroGateOpen }
                            )

                            // 2. شريط العنوان وشارة التزامن السحابي الفوري بدون أي تداخل نصي (Arrangement.SpaceBetween)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "القطاعات والخدمات الرسمية في ناحية زمار",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = ZummarEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = ZummarEmerald.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, ZummarEmerald.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(ZummarEmeraldLight)
                                        )
                                        Text(
                                            text = "تزامن سحابي فوري",
                                            fontFamily = ZummarTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = ZummarEmerald
                                        )
                                    }
                                }
                            }

                            // 3. بطاقة الانتقال إلى تطبيق مدرستي الذكية المستقل عبر Intent مع الأنيميشن التفاعلي للكتاب المفتوح
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = ZummarCardWhite,
                                border = BorderStroke(1.5.dp, ZummarAmberGold),
                                shadowElevation = 6.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CurriculumHeroBannerWithOpenBookAnimation(
                                        title = "تطبيق مدرستي الذكية (التعليم والمدارس)",
                                        subtitle = "بوابة التعليم الإلكتروني والبطاقات المدرسية والمناهج الرسمية لمدارس ناحية زمار",
                                        rightPillText = "تطبيق مستقل متكامل",
                                        leftPillText = "ربط مباشر",
                                        modifier = Modifier.clickable { launchStandaloneMadrasatiApp() }
                                    )
                                    Button(
                                        onClick = { launchStandaloneMadrasatiApp() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ZummarCustomIcon(
                                            type = ZummarVectorIconType.CHECK_VERIFIED,
                                            tint = ZummarAmberGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "الانتقال إلى تطبيق مدرستي الذكية",
                                            fontFamily = ZummarTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // 4. شبكة القطاعات الخدمية الأربعة بلوحات فنية مرسومة برمجياً (بدون إيموجي نصي)
                            val generatorsCount = approvedMarkers.count { it.type == ZummarMarkerType.GENERATOR }
                            val healthCount = approvedMarkers.count {
                                it.type == ZummarMarkerType.PHARMACY ||
                                    it.type == ZummarMarkerType.CLINIC ||
                                    it.type == ZummarMarkerType.LAB
                            }
                            val shopsCount = approvedMarkers.count { it.type == ZummarMarkerType.SHOP }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ZummarRichSectorCard(
                                    title = "الكهرباء والمولدات",
                                    subtitle = "الأحياء: ${zones.size} • المولدات: $generatorsCount",
                                    accentColor = ZummarAmberWarm,
                                    artContent = { ZummarElectricitySectorArtCanvas(Modifier.size(58.dp)) },
                                    onClick = { activeTab = ZummarPortalTab.POWER_WATER },
                                    modifier = Modifier.weight(1f)
                                )
                                ZummarRichSectorCard(
                                    title = "مديرية ماء زمار",
                                    subtitle = "جداول الضخ والصيانة (${zones.size})",
                                    accentColor = ZummarSkyCyan,
                                    artContent = { ZummarWaterSectorArtCanvas(Modifier.size(58.dp)) },
                                    onClick = { activeTab = ZummarPortalTab.POWER_WATER },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ZummarRichSectorCard(
                                    title = "الصحة والصيدليات",
                                    subtitle = "المراكز المسجلة: $healthCount",
                                    accentColor = ZummarEmeraldLight,
                                    artContent = { ZummarHealthEcgSectorArtCanvas(Modifier.size(58.dp)) },
                                    onClick = { activeTab = ZummarPortalTab.HEALTH_CARE },
                                    modifier = Modifier.weight(1f)
                                )
                                ZummarRichSectorCard(
                                    title = "سوق زمار الشامل",
                                    subtitle = "المتاجر: $shopsCount • العروض: ${products.size}",
                                    accentColor = ZummarAmberGold,
                                    artContent = { ZummarMarketStoreSectorArtCanvas(Modifier.size(58.dp)) },
                                    onClick = { activeTab = ZummarPortalTab.MARKET_BAZAAR },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 5. رادار الصيدليات الخافرة وملخص الأحياء المباشر
                            ZummarQuickOverviewSection(
                                zones = zones,
                                markers = approvedMarkers,
                                onSelectZone = { selectedZoneForDialog = it },
                                onSelectMarker = { selectedMarkerForDialog = it }
                            )
                        }

                        ZummarPortalTab.MAP_RADAR -> {
                            ZummarInteractiveMapSection(
                                zones = zones,
                                markers = approvedMarkers,
                                selectedLayer = selectedMapLayer,
                                onSelectLayer = { selectedMapLayer = it },
                                canDrawZones = canManageElectricity || canManageWater,
                                drawMode = mapDrawMode,
                                onChangeDrawMode = { newMode ->
                                    mapDrawMode = newMode
                                    if (newMode == ZummarZoneDrawMode.NONE) {
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
                                        showSaveDrawnZoneDialog = true
                                    } else {
                                        triggerBanner("يرجى تحديد 3 نقاط على الأقل على خريطة زمار لإغلاق حدود الحي", isSuccess = false)
                                    }
                                },
                                onZoneTapped = { selectedZoneForDialog = it },
                                onMarkerTapped = { selectedMarkerForDialog = it },
                                onMapCoordinatePicked = { coord ->
                                    pickedMapCoord = coord
                                    mapDrawMode = ZummarZoneDrawMode.NONE
                                    showAddLandmarkDialog = true
                                    triggerBanner("تم التقاط إحداثيات الموقع على خريطة زمار، أكمل البيانات للحفظ")
                                },
                                onRequestAddPinModal = {
                                    if (!canPinLocation) {
                                        showAccountAuthDialog = true
                                    } else {
                                        showAddLandmarkDialog = true
                                    }
                                }
                            )
                        }

                        ZummarPortalTab.POWER_WATER -> {
                            ZummarElectricityDepartmentSection(
                                zones = zones,
                                generators = approvedMarkers.filter { it.type == ZummarMarkerType.GENERATOR },
                                canManageElectricity = canManageElectricity,
                                onUpdateZonePower = { updated ->
                                    persistAll(newZones = zones.map { if (it.id == updated.id) updated else it })
                                    triggerBanner("تم تحديث حالة كهرباء ${updated.name} ومزامنتها")
                                },
                                onStartDrawZoneOnMap = {
                                    activeTab = ZummarPortalTab.MAP_RADAR
                                    mapDrawMode = ZummarZoneDrawMode.POLYGON_POINTS
                                    draftPolygonPoints = emptyList()
                                },
                                onAddGenerator = {
                                    showAddLandmarkDialog = true
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ZummarWaterDepartmentSection(
                                zones = zones,
                                canManageWater = canManageWater,
                                onUpdateZoneWater = { updated ->
                                    persistAll(newZones = zones.map { if (it.id == updated.id) updated else it })
                                    triggerBanner("تم تحديث جدول ضخ الماء لـ ${updated.name}")
                                },
                                onStartDrawWaterZone = {
                                    activeTab = ZummarPortalTab.MAP_RADAR
                                    mapDrawMode = ZummarZoneDrawMode.POLYGON_POINTS
                                    draftPolygonPoints = emptyList()
                                }
                            )
                        }

                        ZummarPortalTab.HEALTH_CARE -> {
                            Button(
                                onClick = {
                                    if (currentUser == null) showAccountAuthDialog = true
                                    else showAddLandmarkDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ZummarNavy),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ZummarCustomIcon(ZummarVectorIconType.PLUS_DRAW, ZummarAmberGold, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+ تسجيل صيدلية خافرة / عيادة طبيب / مختبر على خريطة زمار",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ZummarAmberGold
                                )
                            }

                            ZummarHealthDirectorySection(
                                healthMarkers = approvedMarkers.filter {
                                    it.type == ZummarMarkerType.PHARMACY ||
                                        it.type == ZummarMarkerType.CLINIC ||
                                        it.type == ZummarMarkerType.LAB
                                },
                                bookings = bookings,
                                canManageAll = isSupremeAdmin || currentUser?.role == ZummarRole.HEALTH_PROVIDER,
                                onToggleOnDuty = { target ->
                                    val updated = target.copy(isOnDuty = !target.isOnDuty)
                                    persistAll(newMarkers = markers.map { if (it.id == updated.id) updated else it })
                                    triggerBanner("تم تحديث حالة الخفارة لـ ${updated.title}")
                                },
                                onOpenChat = { activeChatMarker = it },
                                onOpenBooking = { activeBookingClinic = it }
                            )
                        }

                        ZummarPortalTab.MARKET_BAZAAR -> {
                            Button(
                                onClick = {
                                    if (currentUser == null) showAccountAuthDialog = true
                                    else showAddLandmarkDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ZummarCustomIcon(ZummarVectorIconType.BAZAAR_STORE, ZummarAmberGold, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+ تثبيت محل تجاري جديد في سوق زمار الشامل",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            ZummarBazaarSection(
                                shops = approvedMarkers.filter { it.type == ZummarMarkerType.SHOP },
                                products = products,
                                canAddProduct = canAddProduct,
                                onSelectShop = { selectedMarkerForDialog = it },
                                onAddProductToShop = { activeAddProductShop = it },
                                onMessageShop = { activeChatMarker = it }
                            )
                        }
                    }
                }

                // شريط الإشعارات العائم
                ZummarFloatingFeedbackBanner(
                    banner = feedbackBanner,
                    onDismiss = { feedbackBanner = null },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        // النوافذ التفاعلية المنبثقة
        selectedZoneForDialog?.let { zone ->
            ZummarZoneDetailsDialog(
                zone = zone,
                canManageElectricity = canManageElectricity,
                canManageWater = canManageWater,
                onUpdateZone = { updated ->
                    persistAll(newZones = zones.map { if (it.id == updated.id) updated else it })
                    triggerBanner("تم حفظ تحديثات ${updated.name} بنجاح")
                },
                onDismiss = { selectedZoneForDialog = null }
            )
        }

        selectedMarkerForDialog?.let { marker ->
            ZummarMarkerDetailsDialog(
                marker = marker,
                shopProducts = products.filter { it.shopMarkerId == marker.id },
                canAddProduct = canAddProduct,
                onOpenChat = {
                    val m = marker
                    selectedMarkerForDialog = null
                    activeChatMarker = m
                },
                onOpenClinicBooking = {
                    val m = marker
                    selectedMarkerForDialog = null
                    activeBookingClinic = m
                },
                onOpenAddProduct = {
                    val m = marker
                    selectedMarkerForDialog = null
                    activeAddProductShop = m
                },
                onDismiss = { selectedMarkerForDialog = null }
            )
        }

        activeBookingClinic?.let { clinic ->
            ZummarClinicBookingDialog(
                clinic = clinic,
                defaultPatientName = currentUser?.fullName ?: "",
                defaultPatientPhone = currentUser?.usernameOrPhone ?: "",
                onConfirmBooking = { newBooking ->
                    persistAll(newBookings = listOf(newBooking) + bookings)
                    activeBookingClinic = null
                    triggerBanner("تم تأكيد حجز الموعد (${newBooking.bookingCode}) في ${clinic.title}")
                },
                onDismiss = { activeBookingClinic = null }
            )
        }

        activeChatMarker?.let { target ->
            ZummarOnlineChatDialog(
                targetMarker = target,
                currentUser = currentUser,
                messages = messages.filter { it.targetMarkerId == target.id },
                onSendMessage = { txt, imgBase64 ->
                    val msg = ZummarChatMessage(
                        targetMarkerId = target.id,
                        targetTitle = target.title,
                        senderName = currentUser?.fullName ?: "مواطن من زمار",
                        senderRole = currentUser?.role?.arabicTitle ?: "مواطن",
                        messageText = txt,
                        attachmentBase64 = imgBase64,
                        timestampLabel = "الآن"
                    )
                    persistAll(newMessages = messages + msg)
                },
                onDismiss = { activeChatMarker = null }
            )
        }

        activeAddProductShop?.let { shop ->
            ZummarAddProductDialog(
                shop = shop,
                onPublishProduct = { newProd ->
                    persistAll(newProducts = listOf(newProd) + products)
                    activeAddProductShop = null
                    triggerBanner("تم نشر ${newProd.productName} في ${shop.title} بنجاح")
                },
                onDismiss = { activeAddProductShop = null }
            )
        }

        if (showSaveDrawnZoneDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDrawnZoneDialog = false },
                title = {
                    Text(
                        text = "تسمية الحي أو القاطع الجديد على خريطة زمار",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        color = ZummarNavy
                    )
                },
                text = {
                    OutlinedTextField(
                        value = newDrawnZoneName,
                        onValueChange = { newDrawnZoneName = it },
                        label = { Text("اسم الحي السكني أو القاطع", fontFamily = ZummarTajawalFont) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalName = newDrawnZoneName.trim().ifBlank { "حي زمار (${zones.size + 1})" }
                            val newZone = ZummarNeighborhoodZone(
                                name = finalName,
                                points = draftPolygonPoints
                            )
                            persistAll(newZones = zones + newZone)
                            newDrawnZoneName = ""
                            draftPolygonPoints = emptyList()
                            mapDrawMode = ZummarZoneDrawMode.NONE
                            showSaveDrawnZoneDialog = false
                            triggerBanner("تم حفظ ورسم ${newZone.name} على خريطة زمار بنجاح")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald)
                    ) {
                        Text("حفظ الحي على الخريطة", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDrawnZoneDialog = false }) {
                        Text("إلغاء", fontFamily = ZummarTajawalFont, color = ZummarCrimson)
                    }
                }
            )
        }

        if (showAddLandmarkDialog) {
            ZummarAddLandmarkMarkerDialog(
                isSupremeAdmin = isSupremeAdmin,
                initialCoord = pickedMapCoord,
                onRequestPickOnMap = {
                    showAddLandmarkDialog = false
                    activeTab = ZummarPortalTab.MAP_RADAR
                    mapDrawMode = ZummarZoneDrawMode.PICK_MARKER_LOCATION
                },
                onSaveMarker = { newMarker ->
                    val autoApprove = isSupremeAdmin ||
                        currentUser?.role == ZummarRole.ELECTRICITY_OFFICIAL ||
                        currentUser?.role == ZummarRole.WATER_OFFICIAL
                    val finalMarker = newMarker.copy(isApproved = autoApprove)
                    persistAll(newMarkers = markers + finalMarker)
                    showAddLandmarkDialog = false
                    triggerBanner(
                        if (autoApprove) "تم تثبيت ${finalMarker.title} على خريطة زمار فوراً"
                        else "تم إرسال موقع ${finalMarker.title} لاعتماد الإدارة العليا لبوابة زمار"
                    )
                },
                onDismiss = { showAddLandmarkDialog = false }
            )
        }

        if (showAdminApprovalsDialog) {
            ZummarAdminApprovalsDialog(
                pendingMarkers = pendingMarkers,
                pendingUsers = pendingUsers,
                onApproveMarker = { target ->
                    persistAll(newMarkers = markers.map { if (it.id == target.id) it.copy(isApproved = true) else it })
                    triggerBanner("تم اعتماد ونشر ${target.title} على خريطة زمار")
                },
                onApproveUser = { targetUser ->
                    persistAll(newUsers = users.map { if (it.id == targetUser.id) it.copy(isApproved = true) else it })
                    triggerBanner("تم اعتماد حساب ${targetUser.fullName}")
                },
                onDismiss = { showAdminApprovalsDialog = false }
            )
        }

        if (showAccountAuthDialog) {
            ZummarAccountAuthDialog(
                currentUser = currentUser,
                users = users,
                onLoginSuccess = { loggedIn ->
                    currentUser = loggedIn
                    showAccountAuthDialog = false
                    triggerBanner("مرحباً بك ${loggedIn.fullName} في بوابة زمار الذكية")
                },
                onRegisterAccount = { newAcc, autoMarker ->
                    val updatedUsers = users + newAcc
                    val updatedMarkers = if (autoMarker != null) markers + autoMarker else markers
                    persistAll(newUsers = updatedUsers, newMarkers = updatedMarkers)
                    currentUser = newAcc
                    showAccountAuthDialog = false
                    triggerBanner(
                        if (newAcc.isApproved) "تم إنشاء وتفعيل حسابك بنجاح"
                        else "تم إنشاء الحساب وإرسال طلب اعتماد موقعك للإدارة العليا"
                    )
                },
                onLogout = {
                    currentUser = null
                    showAccountAuthDialog = false
                    triggerBanner("تم تسجيل الخروج بنجاح")
                },
                onDismiss = { showAccountAuthDialog = false }
            )
        }
    }
}

@Composable
private fun ZummarRichSectorCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    artContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ZummarCardWhite,
        border = BorderStroke(1.4.dp, accentColor.copy(alpha = 0.65f)),
        shadowElevation = 5.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.16f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                artContent()
            }
            Text(
                text = title,
                fontFamily = ZummarTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = ZummarNavy
            )
            Text(
                text = subtitle,
                fontFamily = ZummarTajawalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = ZummarEmerald
            )
        }
    }
}
