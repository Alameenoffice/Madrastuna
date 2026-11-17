package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

enum class ZummarMapLayerFilter(val arabicLabel: String) {
    ALL("كل الطبقات"),
    ELECTRICITY("الكهرباء والمولدات"),
    WATER("ضخ الماء"),
    HEALTH("الصحة والخافرة"),
    BAZAAR("سوق زمار"),
    GOV("الدوائر الحكومية")
}

enum class ZummarZoneDrawMode(val arabicLabel: String) {
    NONE("تصفح الخريطة"),
    POLYGON_POINTS("تحديد نقاط المضلع"),
    FREEHAND_LASSO("رسم حر بالإصبع"),
    PICK_MARKER_LOCATION("تثبيت موقع على الخريطة")
}

fun isPointInPolygon(pt: ZummarPoint, polygon: List<ZummarPoint>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val xi = polygon[i].x
        val yi = polygon[i].y
        val xj = polygon[j].x
        val yj = polygon[j].y
        val intersect = ((yi > pt.y) != (yj > pt.y)) &&
            (pt.x < (xj - xi) * (pt.y - yi) / ((yj - yi).takeIf { it != 0f } ?: 0.00001f) + xi)
        if (intersect) inside = !inside
        j = i
    }
    return inside
}

@Composable
fun ZummarInteractiveMapSection(
    zones: List<ZummarNeighborhoodZone>,
    markers: List<ZummarLandmarkMarker>,
    selectedLayer: ZummarMapLayerFilter,
    onSelectLayer: (ZummarMapLayerFilter) -> Unit,
    canDrawZones: Boolean,
    drawMode: ZummarZoneDrawMode,
    onChangeDrawMode: (ZummarZoneDrawMode) -> Unit,
    draftPoints: List<ZummarPoint>,
    onAddDraftPoint: (ZummarPoint) -> Unit,
    onClearDraftPoints: () -> Unit,
    onFinishZoneDrawing: () -> Unit,
    onZoneTapped: (ZummarNeighborhoodZone) -> Unit,
    onMarkerTapped: (ZummarLandmarkMarker) -> Unit,
    onMapCoordinatePicked: (ZummarPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "zummar_map_dynamics")
    val electricFlowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "electric_flow_phase"
    )
    val waterWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "water_wave_phase"
    )
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // شريط فلترة طبقات خريطة ناحية زمار
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZummarMapLayerFilter.entries.forEach { layer ->
                val selected = selectedLayer == layer
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) ZummarEmerald else ZummarCardWhite,
                    border = BorderStroke(
                        1.2.dp,
                        if (selected) ZummarAmberGold else ZummarEmerald.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.clickable { onSelectLayer(layer) }
                ) {
                    Text(
                        text = layer.arabicLabel,
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (selected) Color.White else ZummarNavy,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // شريط أدوات رسم وتحديد الأحياء الجغرافية للمسؤولين والإدارة العليا
        if (canDrawZones) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZummarNavy,
                border = BorderStroke(1.dp, ZummarAmberGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZummarCustomIcon(ZummarVectorIconType.POLYGON_LASSO, ZummarAmberGold, Modifier.size(20.dp))
                            Text(
                                text = "+ رسم وتحديد حي / بقعة جغرافية على الخريطة",
                                fontFamily = ZummarTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ZummarZoneDrawMode.NONE,
                            ZummarZoneDrawMode.POLYGON_POINTS,
                            ZummarZoneDrawMode.FREEHAND_LASSO
                        ).forEach { mode ->
                            val isCurrent = drawMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) ZummarAmberGold else Color(0xFF1E293B),
                                modifier = Modifier.clickable { onChangeDrawMode(mode) }
                            ) {
                                Text(
                                    text = mode.arabicLabel,
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isCurrent) ZummarNavy else Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        if (draftPoints.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ZummarEmeraldLight,
                                modifier = Modifier.clickable { onFinishZoneDrawing() }
                            ) {
                                Text(
                                    text = "حفظ وتسمية الحي (${draftPoints.size} نقطة)",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = ZummarNavy,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ZummarCrimson,
                                modifier = Modifier.clickable { onClearDraftPoints() }
                            ) {
                                Text(
                                    text = "مسح النقاط",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (drawMode == ZummarZoneDrawMode.PICK_MARKER_LOCATION) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ZummarAmberGold,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "انقر الآن على أي نقطة داخل خريطة ناحية زمار لتثبيت الموقع الجغرافي بدقة",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = ZummarNavy,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // لوحة خريطة ناحية زمار الجغرافية التفاعلية
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0B192C))
                .border(1.8.dp, ZummarEmeraldMid.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
        ) {
            val approvedMarkers = remember(markers, selectedLayer) {
                markers.filter { m ->
                    m.isApproved && when (selectedLayer) {
                        ZummarMapLayerFilter.ALL -> true
                        ZummarMapLayerFilter.ELECTRICITY -> m.type == ZummarMarkerType.GENERATOR
                        ZummarMapLayerFilter.WATER -> false
                        ZummarMapLayerFilter.HEALTH -> m.type == ZummarMarkerType.PHARMACY || m.type == ZummarMarkerType.CLINIC || m.type == ZummarMarkerType.LAB
                        ZummarMapLayerFilter.BAZAAR -> m.type == ZummarMarkerType.SHOP
                        ZummarMapLayerFilter.GOV -> m.type == ZummarMarkerType.GOV_OFFICE
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(drawMode, zoomScale, panOffset, zones, approvedMarkers) {
                        when (drawMode) {
                            ZummarZoneDrawMode.FREEHAND_LASSO -> {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val normX = ((offset.x - panOffset.x) / (size.width * zoomScale)).coerceIn(0.02f, 0.98f)
                                        val normY = ((offset.y - panOffset.y) / (size.height * zoomScale)).coerceIn(0.02f, 0.98f)
                                        onAddDraftPoint(ZummarPoint(normX, normY))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val pos = change.position
                                        val normX = ((pos.x - panOffset.x) / (size.width * zoomScale)).coerceIn(0.02f, 0.98f)
                                        val normY = ((pos.y - panOffset.y) / (size.height * zoomScale)).coerceIn(0.02f, 0.98f)
                                        onAddDraftPoint(ZummarPoint(normX, normY))
                                    }
                                )
                            }
                            else -> {
                                detectTapGestures { tapOffset ->
                                    val normX = ((tapOffset.x - panOffset.x) / (size.width * zoomScale)).coerceIn(0.02f, 0.98f)
                                    val normY = ((tapOffset.y - panOffset.y) / (size.height * zoomScale)).coerceIn(0.02f, 0.98f)
                                    val pt = ZummarPoint(normX, normY)

                                    if (drawMode == ZummarZoneDrawMode.POLYGON_POINTS) {
                                        onAddDraftPoint(pt)
                                        return@detectTapGestures
                                    }
                                    if (drawMode == ZummarZoneDrawMode.PICK_MARKER_LOCATION) {
                                        onMapCoordinatePicked(pt)
                                        return@detectTapGestures
                                    }

                                    // فحص النقر على المعالم والمحلات والصيدليات أولاً
                                    val hitMarker = approvedMarkers.minByOrNull { m ->
                                        hypot((m.mapX - normX).toDouble(), (m.mapY - normY).toDouble())
                                    }
                                    if (hitMarker != null && hypot((hitMarker.mapX - normX).toDouble(), (hitMarker.mapY - normY).toDouble()) < 0.065) {
                                        onMarkerTapped(hitMarker)
                                        return@detectTapGestures
                                    }

                                    // فحص النقر داخل مضلع حي من أحياء زمار
                                    val hitZone = zones.firstOrNull { z -> isPointInPolygon(pt, z.points) }
                                    if (hitZone != null) {
                                        onZoneTapped(hitZone)
                                    }
                                }
                            }
                        }
                    }
            ) {
                val w = size.width * zoomScale
                val h = size.height * zoomScale
                val ox = panOffset.x
                val oy = panOffset.y

                // 1. شبكة إحداثيات الخريطة الجغرافية لناحية زمار
                val gridStepX = w / 10f
                val gridStepY = h / 10f
                for (i in 1..9) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(ox + i * gridStepX, oy),
                        end = Offset(ox + i * gridStepX, oy + h),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(ox, oy + i * gridStepY),
                        end = Offset(ox + w, oy + i * gridStepY),
                        strokeWidth = 1f
                    )
                }

                // 2. مجرى نهر دجلة وبحيرة سد الموصل المحاذي لناحية زمار
                val tigrisRiverPath = Path().apply {
                    moveTo(ox + w * 0.02f, oy + h * 0.14f)
                    cubicTo(
                        ox + w * 0.28f, oy + h * 0.04f,
                        ox + w * 0.62f, oy + h * 0.22f,
                        ox + w * 0.98f, oy + h * 0.10f
                    )
                }
                drawPath(
                    path = tigrisRiverPath,
                    color = ZummarSkyCyan.copy(alpha = 0.32f),
                    style = Stroke(width = 24.dp.toPx() * zoomScale, cap = StrokeCap.Round)
                )
                drawPath(
                    path = tigrisRiverPath,
                    color = ZummarCyanLight.copy(alpha = 0.75f),
                    style = Stroke(width = 4.dp.toPx() * zoomScale, cap = StrokeCap.Round)
                )

                // 3. الشوارع الرئيسية في ناحية زمار (الشارع العام وطريق تلعفر - زمار - ربيعة)
                val mainRoadH = Path().apply {
                    moveTo(ox + w * 0.02f, oy + h * 0.52f)
                    cubicTo(
                        ox + w * 0.35f, oy + h * 0.48f,
                        ox + w * 0.65f, oy + h * 0.56f,
                        ox + w * 0.98f, oy + h * 0.50f
                    )
                }
                val mainRoadV = Path().apply {
                    moveTo(ox + w * 0.48f, oy + h * 0.12f)
                    cubicTo(
                        ox + w * 0.50f, oy + h * 0.45f,
                        ox + w * 0.46f, oy + h * 0.70f,
                        ox + w * 0.52f, oy + h * 0.96f
                    )
                }
                drawPath(mainRoadH, Color(0xFF334155), style = Stroke(width = 8.dp.toPx() * zoomScale, cap = StrokeCap.Round))
                drawPath(mainRoadV, Color(0xFF334155), style = Stroke(width = 7.dp.toPx() * zoomScale, cap = StrokeCap.Round))

                val labelPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                    textSize = 11.sp.toPx()
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                // 4. رسم مضلعات الأحياء السكنية مع أنيميشن الكهرباء والماء
                zones.forEach { zone ->
                    if (zone.points.size >= 3) {
                        val polyPath = Path().apply {
                            val first = zone.points.first()
                            moveTo(ox + first.x * w, oy + first.y * h)
                            for (i in 1 until zone.points.size) {
                                val pt = zone.points[i]
                                lineTo(ox + pt.x * w, oy + pt.y * h)
                            }
                            close()
                        }

                        val isPowerActive = zone.powerStatus == PowerStatus.NATIONAL_ON || zone.powerStatus == PowerStatus.GENERATOR_ON
                        val isWaterActive = zone.waterStatus == WaterStatus.PUMPING_ON

                        val baseZoneColor = when (selectedLayer) {
                            ZummarMapLayerFilter.WATER -> Color(zone.waterStatus.colorHex).copy(alpha = 0.26f)
                            else -> Color(zone.powerStatus.colorHex).copy(alpha = if (isPowerActive) 0.25f else 0.14f)
                        }

                        drawPath(polyPath, color = baseZoneColor)

                        // رسم منحنيات التيار الكهربائي المتدفقة داخل الحي المضاء وومضاته
                        if ((selectedLayer == ZummarMapLayerFilter.ALL || selectedLayer == ZummarMapLayerFilter.ELECTRICITY) && isPowerActive) {
                            clipPath(polyPath) {
                                val minX = zone.points.minOf { it.x } * w + ox
                                val maxX = zone.points.maxOf { it.x } * w + ox
                                val minY = zone.points.minOf { it.y } * h + oy
                                val maxY = zone.points.maxOf { it.y } * h + oy
                                val spanY = (maxY - minY).coerceAtLeast(20f)

                                for (lineIdx in 0..2) {
                                    val yBase = minY + spanY * (0.25f + lineIdx * 0.25f)
                                    val shift = sin((electricFlowPhase * 2f * PI + lineIdx).toFloat()) * 12f
                                    val elecCurve = Path().apply {
                                        moveTo(minX, yBase)
                                        cubicTo(
                                            (minX + maxX) * 0.35f, yBase - 14f + shift,
                                            (minX + maxX) * 0.65f, yBase + 14f - shift,
                                            maxX, yBase
                                        )
                                    }
                                    drawPath(
                                        path = elecCurve,
                                        color = if (zone.powerStatus == PowerStatus.NATIONAL_ON) ZummarAmberGold.copy(alpha = 0.68f)
                                        else ZummarEmeraldLight.copy(alpha = 0.68f),
                                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        // رسم أمواج ضخ الماء المتدفقة داخل الحي عند فتح الماء
                        if ((selectedLayer == ZummarMapLayerFilter.ALL || selectedLayer == ZummarMapLayerFilter.WATER) && isWaterActive) {
                            clipPath(polyPath) {
                                val minX = zone.points.minOf { it.x } * w + ox
                                val maxX = zone.points.maxOf { it.x } * w + ox
                                val minY = zone.points.minOf { it.y } * h + oy
                                val maxY = zone.points.maxOf { it.y } * h + oy
                                val spanY = (maxY - minY).coerceAtLeast(20f)

                                for (waveIdx in 0..2) {
                                    val yWave = minY + spanY * (0.32f + waveIdx * 0.24f)
                                    val waveOffset = sin(waterWavePhase + waveIdx) * 10f
                                    val waterCurve = Path().apply {
                                        moveTo(minX, yWave)
                                        cubicTo(
                                            (minX + maxX) * 0.33f, yWave + waveOffset,
                                            (minX + maxX) * 0.66f, yWave - waveOffset,
                                            maxX, yWave
                                        )
                                    }
                                    drawPath(
                                        path = waterCurve,
                                        color = ZummarSkyCyan.copy(alpha = 0.65f),
                                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        // إطار المضلع
                        val strokeColor = when {
                            selectedLayer == ZummarMapLayerFilter.WATER -> Color(zone.waterStatus.colorHex)
                            else -> Color(zone.powerStatus.colorHex)
                        }
                        drawPath(
                            path = polyPath,
                            color = strokeColor,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // اسم الحي وحالته في مركز المضلع
                        val centerX = ox + (zone.points.map { it.x }.average().toFloat()) * w
                        val centerY = oy + (zone.points.map { it.y }.average().toFloat()) * h
                        drawContext.canvas.nativeCanvas.drawText(
                            zone.name,
                            centerX,
                            centerY,
                            labelPaint
                        )
                    }
                }

                // 5. رسم المضلع الجاري رسمه حالياً من قبل المسؤول
                if (draftPoints.isNotEmpty()) {
                    val draftPath = Path().apply {
                        val first = draftPoints.first()
                        moveTo(ox + first.x * w, oy + first.y * h)
                        for (i in 1 until draftPoints.size) {
                            val pt = draftPoints[i]
                            lineTo(ox + pt.x * w, oy + pt.y * h)
                        }
                        if (draftPoints.size >= 3) close()
                    }
                    drawPath(draftPath, color = ZummarAmberGold.copy(alpha = 0.28f))
                    drawPath(draftPath, color = ZummarAmberGold, style = Stroke(width = 3.dp.toPx()))
                    draftPoints.forEach { pt ->
                        drawCircle(
                            color = ZummarAmberGold,
                            radius = 6.dp.toPx(),
                            center = Offset(ox + pt.x * w, oy + pt.y * h)
                        )
                    }
                }

                // 6. رسم المعالم والمحلات والصيدليات الخافرة والمولدات والدوائر الحكومية
                approvedMarkers.forEach { marker ->
                    val mx = ox + marker.mapX * w
                    val my = oy + marker.mapY * h

                    val markerColor = when (marker.type) {
                        ZummarMarkerType.PHARMACY -> ZummarEmeraldLight
                        ZummarMarkerType.CLINIC -> ZummarSkyCyan
                        ZummarMarkerType.LAB -> Color(0xFF38BDF8)
                        ZummarMarkerType.SHOP -> ZummarAmberGold
                        ZummarMarkerType.GENERATOR -> ZummarAmberWarm
                        ZummarMarkerType.GOV_OFFICE -> Color(0xFFE2E8F0)
                    }

                    // هالة رادار متوهجة للصيدليات الخافرة
                    if (marker.type == ZummarMarkerType.PHARMACY && marker.isOnDuty) {
                        drawCircle(
                            color = ZummarEmeraldLight.copy(alpha = (1.7f - radarPulse).coerceIn(0.12f, 0.55f)),
                            radius = 16.dp.toPx() * radarPulse,
                            center = Offset(mx, my)
                        )
                    }

                    // دبوس الموقع المعماري المرسوم برمجياً
                    val pinPath = Path().apply {
                        moveTo(mx, my + 4.dp.toPx())
                        cubicTo(
                            mx - 14.dp.toPx(), my - 10.dp.toPx(),
                            mx - 12.dp.toPx(), my - 26.dp.toPx(),
                            mx, my - 26.dp.toPx()
                        )
                        cubicTo(
                            mx + 12.dp.toPx(), my - 26.dp.toPx(),
                            mx + 14.dp.toPx(), my - 10.dp.toPx(),
                            mx, my + 4.dp.toPx()
                        )
                        close()
                    }
                    drawPath(pinPath, color = markerColor)
                    drawPath(pinPath, color = ZummarNavy, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(color = ZummarNavy, radius = 4.5.dp.toPx(), center = Offset(mx, my - 15.dp.toPx()))

                    drawContext.canvas.nativeCanvas.drawText(
                        marker.title.take(18),
                        mx,
                        my + 18.dp.toPx(),
                        labelPaint
                    )
                }
            }

            // أزرار التكبير والتصغير وإعادة الضبط فوق الخريطة
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ZummarNavy.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, ZummarAmberGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ZummarCustomIcon(ZummarVectorIconType.ZOOM_IN, ZummarAmberGold, Modifier.size(18.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = ZummarNavy.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, ZummarAmberGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f)
                            if (zoomScale == 1f) panOffset = Offset.Zero
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ZummarCustomIcon(ZummarVectorIconType.ZOOM_OUT, ZummarAmberGold, Modifier.size(18.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = ZummarNavy.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, ZummarSkyCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ZummarCustomIcon(ZummarVectorIconType.RESET_VIEW, ZummarSkyCyan, Modifier.size(18.dp))
                    }
                }
            }

            // مفتاح دلالات ألوان الخريطة في الأسفل
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ZummarNavy.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(ZummarAmberGold, "وطني / محل")
                    LegendDot(ZummarEmeraldLight, "مولدة / خافرة")
                    LegendDot(ZummarSkyCyan, "ضخ ماء / عيادة")
                    LegendDot(ZummarCrimson, "صيانة")
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontFamily = ZummarTajawalFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
