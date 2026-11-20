package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ==========================================
// 1. نافذة تفاصيل الحي السكني والتحكم بالكهرباء والماء والصيانة
// ==========================================
@Composable
fun ZummarZoneDetailsDialog(
    zone: ZummarNeighborhoodZone,
    canManageElectricity: Boolean,
    canManageWater: Boolean,
    onUpdateZone: (ZummarNeighborhoodZone) -> Unit,
    onDismiss: () -> Unit
) {
    var powerStatus by remember(zone) { mutableStateOf(zone.powerStatus) }
    var faultReason by remember(zone) { mutableStateOf(zone.faultReason) }
    var expectedDate by remember(zone) { mutableStateOf(zone.expectedRestoreDate.ifBlank { "2026-11-17" }) }
    var expectedTime by remember(zone) { mutableStateOf(zone.expectedRestoreTime.ifBlank { "06:00 مساءً" }) }
    var outageDuration by remember(zone) { mutableStateOf(zone.outageDurationHours.ifBlank { "ساعتان" }) }

    var waterStatus by remember(zone) { mutableStateOf(zone.waterStatus) }
    var waterOpenTime by remember(zone) { mutableStateOf(zone.waterOpenTime) }
    var waterDuration by remember(zone) { mutableStateOf(zone.waterDurationHours) }
    var waterMaintNote by remember(zone) { mutableStateOf(zone.waterMaintenanceNote) }

    var showCalendarPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.6.dp, ZummarEmerald),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بطاقة خدمات ${zone.name}",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = ZummarNavy
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        ZummarCustomIcon(ZummarVectorIconType.CLOSE_X, ZummarNavy, Modifier.size(18.dp))
                    }
                }

                // ملخص للمواطن
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ZummarSurfaceBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "حالة الكهرباء الحالية: ${powerStatus.arabicLabel}",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            color = ZummarNavy
                        )
                        if (powerStatus == PowerStatus.MAINTENANCE || faultReason.isNotBlank()) {
                            Text(
                                text = "سبب العطل/الصيانة: ${faultReason.ifBlank { "صيانة محولة الشبكة" }}",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ZummarCrimson
                            )
                            Text(
                                text = "العودة المتوقعة: $expectedDate الساعة $expectedTime (المدة: $outageDuration)",
                                fontFamily = ZummarTajawalFont,
                                fontSize = 12.sp,
                                color = ZummarNavy
                            )
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Text(
                            text = "حالة ضخ الماء: ${waterStatus.arabicLabel}",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            color = ZummarEmerald
                        )
                        Text(
                            text = "أوقات فتح الماء: $waterOpenTime • المدة: $waterDuration",
                            fontFamily = ZummarTajawalFont,
                            fontSize = 12.sp,
                            color = ZummarNavy
                        )
                    }
                }

                if (canManageElectricity) {
                    Text(
                        text = "إدارة كهرباء الحي وجدولة الصيانة:",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ZummarNavy
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PowerStatus.entries.forEach { st ->
                            val selected = powerStatus == st
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Color(st.colorHex) else Color(0xFFE2E8F0),
                                modifier = Modifier.clickable { powerStatus = st }
                            ) {
                                Text(
                                    text = st.arabicLabel,
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (selected && st != PowerStatus.NATIONAL_ON) Color.White else ZummarNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = faultReason,
                        onValueChange = { faultReason = it },
                        label = { Text("سبب العطل أو الصيانة (يظهر للمواطنين)", fontFamily = ZummarTajawalFont, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showCalendarPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            ZummarCustomIcon(ZummarVectorIconType.CALENDAR_PICKER, ZummarEmerald, Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("التاريخ: $expectedDate", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        OutlinedTextField(
                            value = expectedTime,
                            onValueChange = { expectedTime = it },
                            label = { Text("وقت العودة", fontFamily = ZummarTajawalFont, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = outageDuration,
                        onValueChange = { outageDuration = it },
                        label = { Text("مدة الإطفاء / الصيانة المتوقعة", fontFamily = ZummarTajawalFont, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (canManageWater) {
                    Text(
                        text = "إدارة ضخ الماء للحي:",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ZummarEmerald
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WaterStatus.entries.forEach { ws ->
                            val selected = waterStatus == ws
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Color(ws.colorHex) else Color(0xFFE2E8F0),
                                modifier = Modifier.clickable { waterStatus = ws }
                            ) {
                                Text(
                                    text = ws.arabicLabel,
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (selected) Color.White else ZummarNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = waterOpenTime,
                        onValueChange = { waterOpenTime = it },
                        label = { Text("أوقات فتح ضخ الماء", fontFamily = ZummarTajawalFont, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إغلاق", fontFamily = ZummarTajawalFont, color = ZummarCrimson)
                    }
                    if (canManageElectricity || canManageWater) {
                        Button(
                            onClick = {
                                onUpdateZone(
                                    zone.copy(
                                        powerStatus = powerStatus,
                                        faultReason = faultReason,
                                        expectedRestoreDate = expectedDate,
                                        expectedRestoreTime = expectedTime,
                                        outageDurationHours = outageDuration,
                                        waterStatus = waterStatus,
                                        waterOpenTime = waterOpenTime,
                                        waterDurationHours = waterDuration,
                                        waterMaintenanceNote = waterMaintNote
                                    )
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("حفظ التعديلات", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showCalendarPicker) {
        ZummarCalendarDatePickerDialog(
            title = "تحديد تاريخ إكمال الصيانة وعودة التيار",
            onDateSelected = { picked ->
                expectedDate = picked
                showCalendarPicker = false
            },
            onDismiss = { showCalendarPicker = false }
        )
    }
}

// ==========================================
// 2. نافذة تفاصيل المعلم / المتجر / الصيدلية / العيادة عند النقر في الخريطة
// ==========================================
@Composable
fun ZummarMarkerDetailsDialog(
    marker: ZummarLandmarkMarker,
    shopProducts: List<ZummarProduct>,
    canAddProduct: Boolean,
    onOpenChat: () -> Unit,
    onOpenClinicBooking: () -> Unit,
    onOpenAddProduct: () -> Unit,
    onDismiss: () -> Unit
) {
    var storefrontOpen by remember { mutableStateOf(false) }
    LaunchedEffect(marker.id) {
        storefrontOpen = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.6.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (marker.type == ZummarMarkerType.SHOP) {
                    InteractiveOpeningStorefrontCanvas(
                        shopName = marker.title,
                        isOpen = storefrontOpen
                    )
                }

                Text(
                    text = marker.title,
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = ZummarNavy
                )
                Text(
                    text = "${marker.type.arabicLabel} • ${marker.subtitleOrOwner}",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ZummarEmerald
                )
                Text(
                    text = "أوقات الدوام: ${marker.workingHours}",
                    fontFamily = ZummarTajawalFont,
                    fontSize = 12.sp,
                    color = ZummarNavyMid
                )
                if (marker.amperePriceIqd.isNotBlank()) {
                    Text(
                        text = "سعر الأمبير المعتمد: ${marker.amperePriceIqd}",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = ZummarAmberWarm
                    )
                }

                // عرض بضائع المتجر إذا كان محلاً تجارياً
                if (marker.type == ZummarMarkerType.SHOP) {
                    Text(
                        text = "البضائع والأسعار المتوفرة داخل المحل (${shopProducts.size}):",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ZummarNavy
                    )
                    if (shopProducts.isEmpty()) {
                        Text(
                            text = "لم يقم التاجر بإضافة بضائع بعد؛ يمكنك مراسلته مباشرة للاستفسار.",
                            fontFamily = ZummarTajawalFont,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        shopProducts.forEach { prod ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ZummarSurfaceBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.productName, fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = ZummarNavy)
                                        Text(prod.description, fontFamily = ZummarTajawalFont, fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = ZummarAmberGold) {
                                        Text(
                                            text = prod.priceIqd,
                                            fontFamily = ZummarTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = ZummarNavy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canAddProduct) {
                        Button(
                            onClick = onOpenAddProduct,
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarAmberGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ إضافة بضاعة وسعر لهذا المحل", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = ZummarNavy)
                        }
                    }
                }

                // أزرار التفاعل المباشر للمواطن
                if (marker.type == ZummarMarkerType.PHARMACY || marker.type == ZummarMarkerType.LAB || marker.type == ZummarMarkerType.SHOP) {
                    Button(
                        onClick = onOpenChat,
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.CHAT_MESSAGE, ZummarAmberGold, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (marker.type == ZummarMarkerType.SHOP) "مراسلة التاجر أونلاين / طلب شراء" else "مراسلة الصيدلية أونلاين وإرسال الوصفة",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                if (marker.type == ZummarMarkerType.CLINIC || marker.type == ZummarMarkerType.LAB) {
                    Button(
                        onClick = onOpenClinicBooking,
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarAmberGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.CALENDAR_PICKER, ZummarNavy, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حجز موعد إلكتروني عبر التقويم",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            color = ZummarNavy
                        )
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("إغلاق البطاقة", fontFamily = ZummarTajawalFont, color = ZummarCrimson)
                }
            }
        }
    }
}

// ==========================================
// 3. نافذة الحجز الإلكتروني في عيادات الأطباء مع التقويم التفاعلي
// ==========================================
@Composable
fun ZummarClinicBookingDialog(
    clinic: ZummarLandmarkMarker,
    defaultPatientName: String,
    defaultPatientPhone: String,
    onConfirmBooking: (ZummarBooking) -> Unit,
    onDismiss: () -> Unit
) {
    var patientName by remember { mutableStateOf(defaultPatientName) }
    var patientPhone by remember { mutableStateOf(defaultPatientPhone) }
    var selectedDate by remember { mutableStateOf("2026-11-18") }
    var selectedTime by remember { mutableStateOf("05:00 مساءً") }
    var notes by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.5.dp, ZummarEmerald),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "حجز موعد إلكتروني — ${clinic.title}",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = ZummarNavy
                )
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("اسم المريض الثلاثي", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = patientPhone,
                    onValueChange = { patientPhone = it },
                    label = { Text("رقم الهاتف للتأكيد", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showCalendar = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ZummarCustomIcon(ZummarVectorIconType.CALENDAR_PICKER, ZummarEmerald, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تاريخ الموعد المختار: $selectedDate (اضغط للتغيير)",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        color = ZummarEmerald
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("04:30 عصراً", "05:30 مساءً", "06:30 مساءً", "07:30 مساءً", "08:30 مساءً").forEach { slot ->
                        val active = selectedTime == slot
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ZummarEmerald else Color(0xFFE2E8F0),
                            modifier = Modifier.clickable { selectedTime = slot }
                        ) {
                            Text(
                                text = slot,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (active) Color.White else ZummarNavy,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("الأعراض أو ملاحظات للطبيب (اختياري)", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", fontFamily = ZummarTajawalFont, color = ZummarCrimson)
                    }
                    Button(
                        onClick = {
                            val booking = ZummarBooking(
                                clinicMarkerId = clinic.id,
                                clinicName = clinic.title,
                                doctorSpecialty = clinic.subtitleOrOwner,
                                patientName = patientName.ifBlank { "مراجع من زمار" },
                                patientPhone = patientPhone.ifBlank { "07700000000" },
                                appointmentDate = selectedDate,
                                appointmentTime = selectedTime,
                                notes = notes
                            )
                            onConfirmBooking(booking)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("تأكيد وإصدار بطاقة الحجز", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showCalendar) {
        ZummarCalendarDatePickerDialog(
            title = "اختر يوم المراجعة الطبية",
            onDateSelected = { picked ->
                selectedDate = picked
                showCalendar = false
            },
            onDismiss = { showCalendar = false }
        )
    }
}

// ==========================================
// 4. نافذة المراسلة الفورية أونلاين مع إرفاق صورة الوصفة الطبية أو المنتج
// ==========================================
@Composable
fun ZummarOnlineChatDialog(
    targetMarker: ZummarLandmarkMarker,
    currentUser: ZummarUserAccount?,
    messages: List<ZummarChatMessage>,
    onSendMessage: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var attachedImageBase64 by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedImageBase64 = ZummarRepository.encodeUriToBase64(context, uri)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.6.dp, ZummarEmerald),
            modifier = Modifier.fillMaxWidth()
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
                            text = "محادثة فورية — ${targetMarker.title}",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = ZummarNavy
                        )
                        Text(
                            text = "اسأل عن توفر الدواء أو البضاعة أو أرسل صورة الوصفة الطبية",
                            fontFamily = ZummarTajawalFont,
                            fontSize = 11.sp,
                            color = ZummarEmerald
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        ZummarCustomIcon(ZummarVectorIconType.CLOSE_X, ZummarNavy, Modifier.size(18.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZummarSurfaceBg)
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        Text(
                            text = "ابدأ المحادثة الآن مع ${targetMarker.title}...",
                            fontFamily = ZummarTajawalFont,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        messages.forEach { msg ->
                            val imgBmp = remember(msg.attachmentBase64) {
                                ZummarRepository.decodeBase64ToImageBitmap(msg.attachmentBase64)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ZummarEmerald.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ZummarEmerald.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${msg.senderName} (${msg.senderRole}) • ${msg.timestampLabel}",
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = ZummarEmerald
                                    )
                                    if (imgBmp != null) {
                                        Image(
                                            bitmap = imgBmp,
                                            contentDescription = "مرفق الصورة",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    }
                                    Text(
                                        text = msg.messageText,
                                        fontFamily = ZummarTajawalFont,
                                        fontSize = 13.sp,
                                        color = ZummarNavy
                                    )
                                }
                            }
                        }
                    }
                }

                if (attachedImageBase64.isNotBlank()) {
                    Text(
                        text = "تم إرفاق صورة من المعرض بنجاح",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ZummarEmerald
                    )
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("اكتب رسالتك أو اسم الدواء/البضاعة المطلوبة...", fontFamily = ZummarTajawalFont, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.CAMERA_PHOTO, ZummarEmerald, Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرفاق صورة وصفة", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            if (textInput.isNotBlank() || attachedImageBase64.isNotBlank()) {
                                onSendMessage(
                                    textInput.ifBlank { "مرفق صورة وصفة طبية / طلب" },
                                    attachedImageBase64
                                )
                                textInput = ""
                                attachedImageBase64 = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إرسال الآن", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. نافذة نشر منتج أو بضاعة جديدة في سوق زمار الشامل
// ==========================================
@Composable
fun ZummarAddProductDialog(
    shop: ZummarLandmarkMarker,
    onPublishProduct: (ZummarProduct) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var prodName by remember { mutableStateOf("") }
    var priceIqd by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var imgBase64 by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imgBase64 = ZummarRepository.encodeUriToBase64(context, uri)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.6.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "نشر بضاعة وسعر في ${shop.title}",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = ZummarNavy
                )
                OutlinedTextField(
                    value = prodName,
                    onValueChange = { prodName = it },
                    label = { Text("اسم المنتج أو البضاعة", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceIqd,
                    onValueChange = { priceIqd = it },
                    label = { Text("السعر بالدينار العراقي (مثال: 15,000 د.ع)", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("وصف المنتج والعرض المتوفر", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ZummarCustomIcon(ZummarVectorIconType.CAMERA_PHOTO, ZummarEmerald, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (imgBase64.isBlank()) "اختيار صورة البضاعة من المعرض" else "تم إرفاق صورة البضاعة بنجاح",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", fontFamily = ZummarTajawalFont, color = ZummarCrimson)
                    }
                    Button(
                        onClick = {
                            if (prodName.isNotBlank()) {
                                onPublishProduct(
                                    ZummarProduct(
                                        shopMarkerId = shop.id,
                                        shopName = shop.title,
                                        productName = prodName.trim(),
                                        priceIqd = priceIqd.ifBlank { "حسب الطلب" },
                                        description = desc.ifBlank { "متوفر الآن في ${shop.title}" },
                                        imageBase64 = imgBase64
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("نشر في المتجر والسوق", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. نافذة تثبيت موقع جديد على خريطة زمار (مولدة / دائرة حكومية / صيدلية / عيادة / محل)
// ==========================================
@Composable
fun ZummarAddLandmarkMarkerDialog(
    isSupremeAdmin: Boolean,
    initialCoord: ZummarPoint,
    onRequestPickOnMap: () -> Unit,
    onSaveMarker: (ZummarLandmarkMarker) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var ownerOrSubtitle by remember { mutableStateOf("") }
    var selectedType by remember {
        mutableStateOf(if (isSupremeAdmin) ZummarMarkerType.GENERATOR else ZummarMarkerType.SHOP)
    }
    var workingHours by remember { mutableStateOf("08:00 ص - 10:00 م") }
    var amperePrice by remember { mutableStateOf("12,000 د.ع للأمبير") }
    var isOnDuty by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.6.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تثبيت موقع خدمي أو تجاري على خريطة زمار",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = ZummarNavy
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ZummarMarkerType.entries.forEach { t ->
                        val active = selectedType == t
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ZummarEmerald else Color(0xFFE2E8F0),
                            modifier = Modifier.clickable { selectedType = t }
                        ) {
                            Text(
                                text = t.arabicLabel,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (active) Color.White else ZummarNavy,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onRequestPickOnMap,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ZummarCustomIcon(ZummarVectorIconType.MAP_COMPASS, ZummarEmerald, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تحديد الموقع بالنقر المباشر على خريطة زمار (${(initialCoord.x * 100).toInt()}%, ${(initialCoord.y * 100).toInt()}%)",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("الاسم الرسمي (مثال: مولدة النور / صيدلية الشفاء)", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = ownerOrSubtitle,
                    onValueChange = { ownerOrSubtitle = it },
                    label = { Text("اسم المالك / الاختصاص / الوصف", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = workingHours,
                    onValueChange = { workingHours = it },
                    label = { Text("ساعات العمل والدوام", fontFamily = ZummarTajawalFont) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (selectedType == ZummarMarkerType.GENERATOR) {
                    OutlinedTextField(
                        value = amperePrice,
                        onValueChange = { amperePrice = it },
                        label = { Text("سعر الأمبير بالدينار العراقي", fontFamily = ZummarTajawalFont) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", fontFamily = ZummarTajawalFont, color = ZummarCrimson)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSaveMarker(
                                    ZummarLandmarkMarker(
                                        title = title.trim(),
                                        subtitleOrOwner = ownerOrSubtitle.ifBlank { selectedType.arabicLabel },
                                        type = selectedType,
                                        mapX = initialCoord.x,
                                        mapY = initialCoord.y,
                                        workingHours = workingHours,
                                        isApproved = isSupremeAdmin,
                                        isOnDuty = selectedType == ZummarMarkerType.PHARMACY && isOnDuty,
                                        amperePriceIqd = if (selectedType == ZummarMarkerType.GENERATOR) amperePrice else ""
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("حفظ وتثبيت على الخريطة", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. نافذة طلبات الاعتماد بانتظار موافقة المطور والإدارة العليا
// ==========================================
@Composable
fun ZummarAdminApprovalsDialog(
    pendingMarkers: List<ZummarLandmarkMarker>,
    pendingUsers: List<ZummarUserAccount>,
    onApproveMarker: (ZummarLandmarkMarker) -> Unit,
    onApproveUser: (ZummarUserAccount) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.6.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "طلبات بانتظار موافقة الإدارة العليا لبوابة زمار",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = ZummarNavy
                )

                if (pendingMarkers.isEmpty() && pendingUsers.isEmpty()) {
                    Text(
                        text = "جميع الحسابات والمواقع المسجلة معتمدة حالياً.",
                        fontFamily = ZummarTajawalFont,
                        fontSize = 13.sp,
                        color = ZummarEmerald
                    )
                }

                pendingMarkers.forEach { m ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ZummarSurfaceBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.title, fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = ZummarNavy)
                                Text("${m.type.arabicLabel} • ${m.subtitleOrOwner}", fontFamily = ZummarTajawalFont, fontSize = 12.sp, color = ZummarEmerald)
                            }
                            Button(
                                onClick = { onApproveMarker(m) },
                                colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald)
                            ) {
                                Text("موافقة ونشر", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                pendingUsers.forEach { u ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ZummarSurfaceBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.fullName, fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = ZummarNavy)
                                Text(u.role.arabicTitle, fontFamily = ZummarTajawalFont, fontSize = 12.sp, color = ZummarEmerald)
                            }
                            Button(
                                onClick = { onApproveUser(u) },
                                colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald)
                            ) {
                                Text("اعتماد الحساب", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ZummarNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// 8. نظام الحسابات المستقل وتسجيل الدخول وإنشاء الحساب (بدون Google/Gmail)
// ==========================================
@Composable
fun ZummarAccountAuthDialog(
    currentUser: ZummarUserAccount?,
    users: List<ZummarUserAccount>,
    onLoginSuccess: (ZummarUserAccount) -> Unit,
    onRegisterAccount: (ZummarUserAccount, ZummarLandmarkMarker?) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    var isRegisterTab by remember { mutableStateOf(false) }
    var usernameOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(ZummarRole.CITIZEN) }
    var specialtyOrShopTitle by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.8.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مركز حسابات بوابة زمار الذكية",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = ZummarNavy
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        ZummarCustomIcon(ZummarVectorIconType.CLOSE_X, ZummarNavy, Modifier.size(18.dp))
                    }
                }

                if (currentUser != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ZummarNavy,
                        border = BorderStroke(1.2.dp, ZummarAmberGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentUser.fullName,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = ZummarAmberGold
                            )
                            Text(
                                text = "الصفة: ${currentUser.role.arabicTitle}",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "معرف الحساب/الهاتف: ${currentUser.usernameOrPhone}",
                                fontFamily = ZummarTajawalFont,
                                fontSize = 12.sp,
                                color = ZummarCyanLight
                            )
                        }
                    }

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.LOGOUT_DOOR, Color.White, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الخروج من الحساب", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isRegisterTab = false; errorMsg = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isRegisterTab) ZummarEmerald else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "تسجيل الدخول",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (!isRegisterTab) Color.White else ZummarNavy
                            )
                        }
                        Button(
                            onClick = { isRegisterTab = true; errorMsg = "" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRegisterTab) ZummarEmerald else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "إنشاء حساب جديد",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isRegisterTab) Color.White else ZummarNavy
                            )
                        }
                    }

                    if (!isRegisterTab) {
                        OutlinedTextField(
                            value = usernameOrPhone,
                            onValueChange = { usernameOrPhone = it },
                            label = { Text("اسم المستخدم أو رقم الهاتف", fontFamily = ZummarTajawalFont) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور", fontFamily = ZummarTajawalFont) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (errorMsg.isNotBlank()) {
                            Text(errorMsg, fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ZummarCrimson)
                        }

                        Button(
                            onClick = {
                                val u = usernameOrPhone.trim()
                                val p = password.trim()
                                val match = users.firstOrNull {
                                    it.usernameOrPhone.equals(u, ignoreCase = true) && it.password == p
                                }
                                if (match != null) {
                                    onLoginSuccess(match)
                                } else {
                                    errorMsg = "بيانات الدخول غير صحيحة، يرجى التحقق من اسم المستخدم وكلمة المرور."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("دخول إلى بوابة زمار الذكية", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    } else {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("الاسم الكامل أو اسم المؤسسة", fontFamily = ZummarTajawalFont) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = usernameOrPhone,
                            onValueChange = { usernameOrPhone = it },
                            label = { Text("اسم المستخدم أو رقم الهاتف", fontFamily = ZummarTajawalFont) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور", fontFamily = ZummarTajawalFont) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            text = "اختر نوع الحساب والصفة في ناحية زمار:",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = ZummarNavy
                        )

                        ZummarRole.entries.filter { it != ZummarRole.SUPREME_ADMIN }.forEach { roleOption ->
                            val selected = selectedRole == roleOption
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) ZummarEmerald.copy(alpha = 0.14f) else ZummarSurfaceBg,
                                border = BorderStroke(1.2.dp, if (selected) ZummarEmerald else Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRole = roleOption }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = roleOption.arabicTitle,
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = ZummarNavy
                                    )
                                    Text(
                                        text = roleOption.badgeDesc,
                                        fontFamily = ZummarTajawalFont,
                                        fontSize = 11.sp,
                                        color = ZummarEmerald
                                    )
                                }
                            }
                        }

                        if (selectedRole == ZummarRole.HEALTH_PROVIDER || selectedRole == ZummarRole.MERCHANT || selectedRole == ZummarRole.GENERATOR_OWNER) {
                            OutlinedTextField(
                                value = specialtyOrShopTitle,
                                onValueChange = { specialtyOrShopTitle = it },
                                label = {
                                    Text(
                                        when (selectedRole) {
                                            ZummarRole.HEALTH_PROVIDER -> "اسم الصيدلية / العيادة والاختصاص"
                                            ZummarRole.GENERATOR_OWNER -> "اسم المولدة الأهلية وسعر الأمبير"
                                            else -> "اسم المحل التجاري ونوع البضائع"
                                        },
                                        fontFamily = ZummarTajawalFont
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                if (fullName.isNotBlank() && usernameOrPhone.isNotBlank() && password.isNotBlank()) {
                                    val needsApproval = selectedRole == ZummarRole.HEALTH_PROVIDER ||
                                        selectedRole == ZummarRole.MERCHANT ||
                                        selectedRole == ZummarRole.GENERATOR_OWNER
                                    val newAcc = ZummarUserAccount(
                                        fullName = fullName.trim(),
                                        usernameOrPhone = usernameOrPhone.trim(),
                                        password = password.trim(),
                                        role = selectedRole,
                                        isApproved = !needsApproval,
                                        specialtyOrCategory = specialtyOrShopTitle
                                    )
                                    val autoMarker = if (needsApproval) {
                                        val markerType = when (selectedRole) {
                                            ZummarRole.HEALTH_PROVIDER -> ZummarMarkerType.PHARMACY
                                            ZummarRole.GENERATOR_OWNER -> ZummarMarkerType.GENERATOR
                                            else -> ZummarMarkerType.SHOP
                                        }
                                        ZummarLandmarkMarker(
                                            title = specialtyOrShopTitle.ifBlank { fullName.trim() },
                                            subtitleOrOwner = fullName.trim(),
                                            type = markerType,
                                            mapX = (30..70).random() / 100f,
                                            mapY = (30..70).random() / 100f,
                                            phoneOrUsername = usernameOrPhone.trim(),
                                            isApproved = false,
                                            isOnDuty = selectedRole == ZummarRole.HEALTH_PROVIDER
                                        )
                                    } else null
                                    onRegisterAccount(newAcc, autoMarker)
                                } else {
                                    errorMsg = "يرجى ملء الاسم ورقم الهاتف/المستخدم وكلمة المرور."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إنشاء الحساب وتفعيله الآن", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
