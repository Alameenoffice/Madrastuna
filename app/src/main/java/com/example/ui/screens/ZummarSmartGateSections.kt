package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZummarQuickOverviewSection(
    zones: List<ZummarNeighborhoodZone>,
    markers: List<ZummarLandmarkMarker>,
    onSelectZone: (ZummarNeighborhoodZone) -> Unit,
    onSelectMarker: (ZummarLandmarkMarker) -> Unit
) {
    val onDutyPharmacies = remember(markers) {
        markers.filter { it.type == ZummarMarkerType.PHARMACY && it.isOnDuty }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // بطاقة رادار الصيدليات الخافرة اليوم في زمار
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ZummarEmerald,
            border = BorderStroke(1.2.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZummarCustomIcon(ZummarVectorIconType.HEALTH_SHIELD, ZummarAmberGold, Modifier.size(22.dp))
                    Text(
                        text = "رادار الصيدليات الخافرة اليوم في ناحية زمار (${onDutyPharmacies.size})",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                onDutyPharmacies.forEach { pharm ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ZummarNavy.copy(alpha = 0.85f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMarker(pharm) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pharm.title,
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = ZummarAmberGold
                                )
                                Text(
                                    text = "${pharm.subtitleOrOwner} • ${pharm.workingHours}",
                                    fontFamily = ZummarTajawalFont,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ZummarEmeraldLight
                            ) {
                                Text(
                                    text = "مراسلة أونلاين",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = ZummarNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ملخص حالة أحياء زمار (الكهرباء والماء)
        Text(
            text = "حالة الكهرباء والماء المباشرة في أحياء زمار (انقر على أي حي للتفاصيل):",
            fontFamily = ZummarTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = ZummarNavy
        )

        zones.forEach { zone ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZummarCardWhite,
                border = BorderStroke(1.2.dp, Color(zone.powerStatus.colorHex).copy(alpha = 0.55f)),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectZone(zone) }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = zone.name,
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = ZummarNavy
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(zone.powerStatus.colorHex).copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color(zone.powerStatus.colorHex))
                            ) {
                                Text(
                                    text = zone.powerStatus.arabicLabel,
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ZummarNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(zone.waterStatus.colorHex).copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color(zone.waterStatus.colorHex))
                            ) {
                                Text(
                                    text = zone.waterStatus.arabicLabel,
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = ZummarNavy,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (zone.powerStatus == PowerStatus.MAINTENANCE && zone.faultReason.isNotBlank()) {
                        Text(
                            text = "تنبيه صيانة الكهرباء: ${zone.faultReason} (العودة المتوقعة: ${zone.expectedRestoreDate} ${zone.expectedRestoreTime})",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ZummarCrimson
                        )
                    }
                    Text(
                        text = "جدول ضخ الماء: ${zone.waterOpenTime} (${zone.waterDurationHours})",
                        fontFamily = ZummarTajawalFont,
                        fontSize = 12.sp,
                        color = ZummarEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun ZummarElectricityDepartmentSection(
    zones: List<ZummarNeighborhoodZone>,
    generators: List<ZummarLandmarkMarker>,
    canManageElectricity: Boolean,
    onUpdateZonePower: (ZummarNeighborhoodZone) -> Unit,
    onStartDrawZoneOnMap: () -> Unit,
    onAddGenerator: () -> Unit
) {
    var faultEditingZone by remember { mutableStateOf<ZummarNeighborhoodZone?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ZummarNavy,
            border = BorderStroke(1.5.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ZummarCustomIcon(ZummarVectorIconType.ELECTRICITY_BOLT, ZummarAmberGold, Modifier.size(26.dp))
                    Column {
                        Text(
                            text = "قسم دائرة كهرباء زمار والمولدات الأهلية",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "متابعة حية لساعات التشغيل الوطني والمولدات وجداول الصيانة لكل حي",
                            fontFamily = ZummarTajawalFont,
                            fontSize = 12.sp,
                            color = ZummarAmberGold
                        )
                    }
                }

                if (canManageElectricity) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartDrawZoneOnMap,
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarAmberGold),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "+ رسم حي على الخريطة",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = ZummarNavy
                            )
                        }
                        Button(
                            onClick = onAddGenerator,
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "+ إضافة مولدة أهلية",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        zones.forEach { zone ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZummarCardWhite,
                border = BorderStroke(1.4.dp, Color(zone.powerStatus.colorHex)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = zone.name,
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ZummarNavy
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(zone.powerStatus.colorHex)
                        ) {
                            Text(
                                text = zone.powerStatus.arabicLabel,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = if (zone.powerStatus == PowerStatus.NATIONAL_ON) ZummarNavy else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (zone.powerStatus == PowerStatus.MAINTENANCE || zone.faultReason.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ZummarCrimson.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, ZummarCrimson.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "سبب العطل/الصيانة: ${zone.faultReason.ifBlank { "صيانة دورية للشبكة" }}",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ZummarCrimson
                                )
                                Text(
                                    text = "تاريخ ووقت عودة التيار: ${zone.expectedRestoreDate} • ${zone.expectedRestoreTime} (مدة الانقطاع: ${zone.outageDurationHours})",
                                    fontFamily = ZummarTajawalFont,
                                    fontSize = 12.sp,
                                    color = ZummarNavy
                                )
                            }
                        }
                    }

                    if (canManageElectricity) {
                        Text(
                            text = "تحكم فوري بحالة كهرباء ${zone.name}:",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ZummarEmerald
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PowerStatus.entries.forEach { st ->
                                val active = zone.powerStatus == st
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) Color(st.colorHex) else Color(0xFFE2E8F0),
                                    modifier = Modifier.clickable {
                                        if (st == PowerStatus.MAINTENANCE) {
                                            faultEditingZone = zone
                                        } else {
                                            onUpdateZonePower(zone.copy(powerStatus = st, faultReason = ""))
                                        }
                                    }
                                ) {
                                    Text(
                                        text = st.arabicLabel,
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (active && st != PowerStatus.NATIONAL_ON) Color.White else ZummarNavy,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // قائمة المولدات الأهلية المسجلة في ناحية زمار
        Text(
            text = "المولدات الأهلية المعتمدة في ناحية زمار (${generators.size}):",
            fontFamily = ZummarTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = ZummarNavy
        )
        generators.forEach { gen ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ZummarCardWhite,
                border = BorderStroke(1.2.dp, ZummarAmberWarm),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.GENERATOR_TURBINE, ZummarAmberWarm, Modifier.size(26.dp))
                        Column {
                            Text(gen.title, fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = ZummarNavy)
                            Text("${gen.subtitleOrOwner} • ${gen.workingHours}", fontFamily = ZummarTajawalFont, fontSize = 12.sp, color = ZummarEmerald)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ZummarAmberGold.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = gen.amperePriceIqd.ifBlank { "12,000 د.ع" },
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = ZummarNavy,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // نافذة جدولة عطل وصيانة الكهرباء بالتقويم التفاعلي
    faultEditingZone?.let { targetZone ->
        ZummarZoneDetailsDialog(
            zone = targetZone.copy(powerStatus = PowerStatus.MAINTENANCE),
            canManageElectricity = true,
            canManageWater = false,
            onUpdateZone = { updated ->
                onUpdateZonePower(updated)
                faultEditingZone = null
            },
            onDismiss = { faultEditingZone = null }
        )
    }
}

@Composable
fun ZummarWaterDepartmentSection(
    zones: List<ZummarNeighborhoodZone>,
    canManageWater: Boolean,
    onUpdateZoneWater: (ZummarNeighborhoodZone) -> Unit,
    onStartDrawWaterZone: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ZummarNavy,
            border = BorderStroke(1.5.dp, ZummarSkyCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ZummarCustomIcon(ZummarVectorIconType.WATER_WAVE, ZummarSkyCyan, Modifier.size(26.dp))
                    Column {
                        Text(
                            text = "قسم مديرية ماء ناحية زمار",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "مواعيد فتح ضخ الماء ومدة التوزيع وتنبيهات صيانة الأنابيب لكل قاطع",
                            fontFamily = ZummarTajawalFont,
                            fontSize = 12.sp,
                            color = ZummarCyanLight
                        )
                    }
                }
                if (canManageWater) {
                    Button(
                        onClick = onStartDrawWaterZone,
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarSkyCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "+ رسم وتحديد قاطع ضخ ماء جديد على الخريطة",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            color = ZummarNavy
                        )
                    }
                }
            }
        }

        zones.forEach { zone ->
            var openTimeText by remember(zone.id, zone.waterOpenTime) { mutableStateOf(zone.waterOpenTime) }
            var durationText by remember(zone.id, zone.waterDurationHours) { mutableStateOf(zone.waterDurationHours) }
            var maintNote by remember(zone.id, zone.waterMaintenanceNote) { mutableStateOf(zone.waterMaintenanceNote) }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZummarCardWhite,
                border = BorderStroke(1.4.dp, Color(zone.waterStatus.colorHex)),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = zone.name,
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = ZummarNavy
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(zone.waterStatus.colorHex)
                        ) {
                            Text(
                                text = zone.waterStatus.arabicLabel,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = "وقت فتح الضخ: ${zone.waterOpenTime} • المدة: ${zone.waterDurationHours}",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ZummarEmerald
                    )

                    if (zone.waterMaintenanceNote.isNotBlank()) {
                        Text(
                            text = "تنبيه صيانة الماء: ${zone.waterMaintenanceNote}",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ZummarCrimson
                        )
                    }

                    if (canManageWater) {
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WaterStatus.entries.forEach { ws ->
                                val active = zone.waterStatus == ws
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) Color(ws.colorHex) else Color(0xFFE2E8F0),
                                    modifier = Modifier.clickable {
                                        onUpdateZoneWater(
                                            zone.copy(
                                                waterStatus = ws,
                                                waterOpenTime = openTimeText,
                                                waterDurationHours = durationText,
                                                waterMaintenanceNote = maintNote
                                            )
                                        )
                                    }
                                ) {
                                    Text(
                                        text = ws.arabicLabel,
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (active) Color.White else ZummarNavy,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = openTimeText,
                                onValueChange = { openTimeText = it },
                                label = { Text("وقت الفتح", fontFamily = ZummarTajawalFont, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = durationText,
                                onValueChange = { durationText = it },
                                label = { Text("مدة الضخ", fontFamily = ZummarTajawalFont, fontSize = 11.sp) },
                                modifier = Modifier.weight(0.7f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = maintNote,
                            onValueChange = { maintNote = it },
                            label = { Text("ملاحظة صيانة أو كسر أنبوب (اختياري)", fontFamily = ZummarTajawalFont, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                onUpdateZoneWater(
                                    zone.copy(
                                        waterOpenTime = openTimeText,
                                        waterDurationHours = durationText,
                                        waterMaintenanceNote = maintNote
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ جدول ضخ الماء لهذا الحي", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZummarHealthDirectorySection(
    healthMarkers: List<ZummarLandmarkMarker>,
    bookings: List<ZummarBooking>,
    canManageAll: Boolean,
    onToggleOnDuty: (ZummarLandmarkMarker) -> Unit,
    onOpenChat: (ZummarLandmarkMarker) -> Unit,
    onOpenBooking: (ZummarLandmarkMarker) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ZummarEmerald,
            border = BorderStroke(1.5.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ZummarCustomIcon(ZummarVectorIconType.HEALTH_SHIELD, ZummarAmberGold, Modifier.size(28.dp))
                Column {
                    Text(
                        text = "خريطة زمار الصحية والصيدليات الخافرة",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "مراسلة الصيدليات أونلاين وإرسال الوصفة الطبية وحجز المواعيد في العيادات",
                        fontFamily = ZummarTajawalFont,
                        fontSize = 12.sp,
                        color = ZummarAmberGold
                    )
                }
            }
        }

        healthMarkers.forEach { item ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZummarCardWhite,
                border = BorderStroke(
                    1.4.dp,
                    if (item.isOnDuty) ZummarEmeraldLight else ZummarSkyCyan
                ),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ZummarNavy
                            )
                            Text(
                                text = "${item.type.arabicLabel} • ${item.subtitleOrOwner}",
                                fontFamily = ZummarTajawalFont,
                                fontSize = 12.sp,
                                color = ZummarEmerald
                            )
                        }
                        if (item.type == ZummarMarkerType.PHARMACY) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (item.isOnDuty) ZummarEmeraldLight else Color(0xFFE2E8F0),
                                modifier = Modifier.clickable(enabled = canManageAll) { onToggleOnDuty(item) }
                            ) {
                                Text(
                                    text = if (item.isOnDuty) "خافرة الآن" else "غير خافرة",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = ZummarNavy,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (item.specialty.isNotBlank()) {
                        Text(
                            text = "الاختصاص والخدمات: ${item.specialty}",
                            fontFamily = ZummarTajawalFont,
                            fontSize = 12.sp,
                            color = ZummarNavy
                        )
                    }
                    Text(
                        text = "أوقات الدوام: ${item.workingHours}",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ZummarNavyMid
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenChat(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            ZummarCustomIcon(ZummarVectorIconType.CHAT_MESSAGE, ZummarAmberGold, Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (item.type == ZummarMarkerType.PHARMACY) "مراسلة الصيدلية أونلاين" else "مراسلة واستفسار",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        if (item.type == ZummarMarkerType.CLINIC || item.type == ZummarMarkerType.LAB) {
                            Button(
                                onClick = { onOpenBooking(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = ZummarAmberGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                ZummarCustomIcon(ZummarVectorIconType.CALENDAR_PICKER, ZummarNavy, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "حجز موعد إلكتروني",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = ZummarNavy
                                )
                            }
                        }
                    }
                }
            }
        }

        if (bookings.isNotEmpty()) {
            Text(
                text = "حجوزات المواعيد الطبية المسجلة (${bookings.size}):",
                fontFamily = ZummarTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = ZummarNavy
            )
            bookings.forEach { bk ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ZummarNavy,
                    border = BorderStroke(1.dp, ZummarAmberGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bk.clinicName, fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, color = ZummarAmberGold)
                            Text("المراجع: ${bk.patientName} • الموعد: ${bk.appointmentDate} (${bk.appointmentTime})", fontFamily = ZummarTajawalFont, fontSize = 12.sp, color = Color.White)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = ZummarEmerald) {
                            Text(
                                text = bk.bookingCode,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZummarBazaarSection(
    shops: List<ZummarLandmarkMarker>,
    products: List<ZummarProduct>,
    canAddProduct: Boolean,
    onSelectShop: (ZummarLandmarkMarker) -> Unit,
    onAddProductToShop: (ZummarLandmarkMarker) -> Unit,
    onMessageShop: (ZummarLandmarkMarker) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ZummarNavy,
            border = BorderStroke(1.5.dp, ZummarAmberGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ZummarCustomIcon(ZummarVectorIconType.BAZAAR_STORE, ZummarAmberGold, Modifier.size(28.dp))
                Column {
                    Text(
                        text = "سوق زمار الشامل والواجهات التجارية المفتوحة",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "تصفح محلات ناحية زمار وبضائعها وأسعارها بالدينار العراقي وتواصل مع التاجر مباشرة",
                        fontFamily = ZummarTajawalFont,
                        fontSize = 12.sp,
                        color = ZummarAmberGold
                    )
                }
            }
        }

        Text(
            text = "المحلات والمتاجر المعتمدة في سوق زمار (${shops.size}):",
            fontFamily = ZummarTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = ZummarNavy
        )

        shops.forEach { shop ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZummarCardWhite,
                border = BorderStroke(1.3.dp, ZummarAmberGold),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectShop(shop) }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shop.title,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ZummarNavy
                            )
                            Text(
                                text = "${shop.subtitleOrOwner} • ${shop.workingHours}",
                                fontFamily = ZummarTajawalFont,
                                fontSize = 12.sp,
                                color = ZummarEmerald
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ZummarEmerald
                        ) {
                            Text(
                                text = "فتح المتجر",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onMessageShop(shop) },
                            colors = ButtonDefaults.buttonColors(containerColor = ZummarNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("مراسلة التاجر أونلاين / طلب شراء", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ZummarAmberGold)
                        }
                        if (canAddProduct) {
                            Button(
                                onClick = { onAddProductToShop(shop) },
                                colors = ButtonDefaults.buttonColors(containerColor = ZummarAmberGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ نشر بضاعة وسعر", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = ZummarNavy)
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "أحدث البضائع والعروض المنشورة في سوق زمار (${products.size}):",
            fontFamily = ZummarTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = ZummarNavy
        )

        products.forEach { prod ->
            val decodedImg = remember(prod.imageBase64) {
                ZummarRepository.decodeBase64ToImageBitmap(prod.imageBase64)
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZummarCardWhite,
                border = BorderStroke(1.dp, ZummarEmerald.copy(alpha = 0.35f)),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (decodedImg != null) {
                        Image(
                            bitmap = decodedImg,
                            contentDescription = prod.productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.productName, fontFamily = ZummarTajawalFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ZummarNavy)
                            Text("المتجر: ${prod.shopName}", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ZummarEmerald)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ZummarAmberGold
                        ) {
                            Text(
                                text = prod.priceIqd,
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = ZummarNavy,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Text(prod.description, fontFamily = ZummarTajawalFont, fontSize = 12.sp, color = ZummarNavyMid)
                }
            }
        }
    }
}
