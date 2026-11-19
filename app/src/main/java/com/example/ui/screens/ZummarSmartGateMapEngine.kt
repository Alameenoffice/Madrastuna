package com.example.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

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
    POLYGON_POINTS("تحديد النقاط المتصلة"),
    FREEHAND_LASSO("الرسم الحر بالإصبع"),
    PICK_MARKER_LOCATION("تثبيت موقع فعلي على الخريطة")
}

object ZummarRealTileCache {
    // إحداثيات ناحية زمار الحقيقية في محافظة نينوى: 36.6575° N, 42.6065° E
    const val ZUMMAR_LAT = 36.6575
    const val ZUMMAR_LON = 42.6065
    const val DEFAULT_ZOOM = 15

    private val memoryCache = ConcurrentHashMap<String, ImageBitmap>()

    fun latLonToTileXY(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val n = 1 shl zoom
        val x = ((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val y = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()
        return Pair(x, y)
    }

    fun getCachedTile(isSatellite: Boolean, z: Int, x: Int, y: Int): ImageBitmap? {
        val key = "${if (isSatellite) "sat" else "osm"}_${z}_${x}_${y}"
        return memoryCache[key]
    }

    suspend fun fetchTileIfNeeded(isSatellite: Boolean, z: Int, x: Int, y: Int): ImageBitmap? {
        val key = "${if (isSatellite) "sat" else "osm"}_${z}_${x}_${y}"
        memoryCache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val urlStr = if (isSatellite) {
                    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
                } else {
                    "https://tile.openstreetmap.org/$z/$x/$y.png"
                }
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "ZummarSmartGate/2.0 (Android; Nineveh-Zummar)")
                }
                if (conn.responseCode in 200..299) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    conn.disconnect()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    if (bmp != null) {
                        memoryCache[key] = bmp
                        return@withContext bmp
                    }
                }
                conn.disconnect()
                null
            } catch (_: Exception) {
                null
            }
        }
    }
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
    onRequestAddPinModal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isSatelliteMode by remember { mutableStateOf(false) }
    var tileLoadTick by remember { mutableIntStateOf(0) }

    val centerTileXY = remember {
        ZummarRealTileCache.latLonToTileXY(
            ZummarRealTileCache.ZUMMAR_LAT,
            ZummarRealTileCache.ZUMMAR_LON,
            ZummarRealTileCache.DEFAULT_ZOOM
        )
    }

    LaunchedEffect(isSatelliteMode) {
        val (cx, cy) = centerTileXY
        for (dy in -1..1) {
            for (dx in -1..1) {
                val loaded = ZummarRealTileCache.fetchTileIfNeeded(
                    isSatellite = isSatelliteMode,
                    z = ZummarRealTileCache.DEFAULT_ZOOM,
                    x = cx + dx,
                    y = cy + dy
                )
                if (loaded != null) {
                    tileLoadTick++
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "zummar_map_dynamics")
    val electricFlowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "electric_flow_phase"
    )
    val waterWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "water_wave_phase"
    )
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // شريط التبديل بين خريطة الشوارع وصور القمر الصناعي لناحية زمار + تثبيت موقع
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (!isSatelliteMode) ZummarEmerald else ZummarCardWhite,
                border = BorderStroke(1.2.dp, ZummarEmerald),
                modifier = Modifier.clickable { isSatelliteMode = false }
            ) {
                Text(
                    text = "خريطة شوارع زمار الحقيقية",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (!isSatelliteMode) Color.White else ZummarEmerald,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSatelliteMode) ZummarEmerald else ZummarCardWhite,
                border = BorderStroke(1.2.dp, ZummarEmerald),
                modifier = Modifier.clickable { isSatelliteMode = true }
            ) {
                Text(
                    text = "صورة القمر الصناعي (زمار)",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isSatelliteMode) Color.White else ZummarEmerald,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFD97706),
                modifier = Modifier.clickable { onRequestAddPinModal() }
            ) {
                Text(
                    text = "+ تثبيت موقع فعلي على الخريطة",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

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
                        if (selected) Color(0xFFD97706) else ZummarEmerald.copy(alpha = 0.25f)
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

        // شريط أدوات رسم وتحديد الأحياء الجغرافية (Polygon Tap Mode + Freehand Lasso Mode)
        if (canDrawZones) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZummarEmerald,
                border = BorderStroke(1.2.dp, Color(0xFFD97706)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.POLYGON_LASSO, Color(0xFFFBBF24), Modifier.size(20.dp))
                        Text(
                            text = "أداة رسم وتحديد الأحياء والقواطع الجغرافية في ناحية زمار",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
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
                                color = if (isCurrent) Color(0xFFD97706) else Color(0xFF043927),
                                modifier = Modifier.clickable { onChangeDrawMode(mode) }
                            ) {
                                Text(
                                    text = mode.arabicLabel,
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        if (draftPoints.size >= 3) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981),
                                modifier = Modifier.clickable { onFinishZoneDrawing() }
                            ) {
                                Text(
                                    text = "حفظ وتسمية الحي (${draftPoints.size} نقطة)",
                                    fontFamily = ZummarTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        if (draftPoints.isNotEmpty()) {
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
                color = Color(0xFFD97706),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "انقر الآن مباشرة على خريطة ناحية زمار لتثبيت الموقع الفعلي بدقة",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // لوحة خريطة ناحية زمار التفاعلية المضمونة 100% (500.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isSatelliteMode) Color(0xFF0A1E1C) else Color(0xFFECF6F1))
                .border(2.dp, ZummarEmerald, RoundedCornerShape(24.dp))
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
                    .pointerInput(drawMode) {
                        if (drawMode == ZummarZoneDrawMode.NONE) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newZoom = (zoomScale * zoom).coerceIn(1f, 3.2f)
                                zoomScale = newZoom
                                if (newZoom > 1f) {
                                    val maxPanX = size.width * (newZoom - 1f)
                                    val maxPanY = size.height * (newZoom - 1f)
                                    panOffset = Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, 40f),
                                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, 40f)
                                    )
                                } else {
                                    panOffset = Offset.Zero
                                }
                            }
                        }
                    }
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
                                        if (draftPoints.isEmpty() || hypot(
                                                (draftPoints.last().x - normX).toDouble(),
                                                (draftPoints.last().y - normY).toDouble()
                                            ) > 0.018
                                        ) {
                                            onAddDraftPoint(ZummarPoint(normX, normY))
                                        }
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

                                    val hitMarker = approvedMarkers.minByOrNull { m ->
                                        hypot((m.mapX - normX).toDouble(), (m.mapY - normY).toDouble())
                                    }
                                    if (hitMarker != null && hypot((hitMarker.mapX - normX).toDouble(), (hitMarker.mapY - normY).toDouble()) < 0.068) {
                                        onMarkerTapped(hitMarker)
                                        return@detectTapGestures
                                    }

                                    val hitZone = zones.firstOrNull { z -> isPointInPolygon(pt, z.points) }
                                    if (hitZone != null) {
                                        onZoneTapped(hitZone)
                                    }
                                }
                            }
                        }
                    }
            ) {
                val _tick = tileLoadTick
                val w = size.width * zoomScale
                val h = size.height * zoomScale
                val ox = panOffset.x
                val oy = panOffset.y

                // 1. الطبقة الأولى: بلاطات خريطة زمار الحقيقية (OpenStreetMap / Esri Satellite)
                val (cx, cy) = centerTileXY
                val tileW = w / 3f
                val tileH = h / 3f
                var anyRealTileDrawn = false
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val bmp = ZummarRealTileCache.getCachedTile(
                            isSatellite = isSatelliteMode,
                            z = ZummarRealTileCache.DEFAULT_ZOOM,
                            x = cx + dx,
                            y = cy + dy
                        )
                        if (bmp != null) {
                            anyRealTileDrawn = true
                            val dstX = (ox + (dx + 1) * tileW).roundToInt()
                            val dstY = (oy + (dy + 1) * tileH).roundToInt()
                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(dstX, dstY),
                                dstSize = IntSize(tileW.roundToInt() + 2, tileH.roundToInt() + 2),
                                alpha = if (isSatelliteMode) 0.92f else 0.96f
                            )
                        }
                    }
                }

                // 2. الطبقة الأساسية الفورية عالية الدقة لمخطط ناحية زمار الجغرافي (تمنع أي شاشة فارغة 100%)
                val vectorOverlayAlpha = if (anyRealTileDrawn) 0.55f else 1.0f
                if (!anyRealTileDrawn) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = if (isSatelliteMode) {
                                listOf(Color(0xFF0A221E), Color(0xFF0F2E26), Color(0xFF091B18))
                            } else {
                                listOf(Color(0xFFE6F4EE), Color(0xFFF2F9F5), Color(0xFFDCEEE5))
                            }
                        ),
                        topLeft = Offset(ox, oy),
                        size = Size(w, h)
                    )
                }

                // شبكة الإحداثيات الجغرافية الحقيقية لناحية زمار (36.6575°N, 42.6065°E)
                val gridStrokeColor = if (isSatelliteMode) Color.White.copy(alpha = 0.10f) else ZummarEmerald.copy(alpha = 0.10f)
                for (i in 1..8) {
                    drawLine(
                        color = gridStrokeColor,
                        start = Offset(ox + w * (i / 9f), oy),
                        end = Offset(ox + w * (i / 9f), oy + h),
                        strokeWidth = 1.2f
                    )
                    drawLine(
                        color = gridStrokeColor,
                        start = Offset(ox, oy + h * (i / 9f)),
                        end = Offset(ox + w, oy + h * (i / 9f)),
                        strokeWidth = 1.2f
                    )
                }

                // مجرى نهر دجلة وبحيرة سد الموصل المحاذي لناحية زمار شرقاً وشمالاً
                val tigrisRiverPath = Path().apply {
                    moveTo(ox + w * 0.02f, oy + h * 0.14f)
                    cubicTo(
                        ox + w * 0.30f, oy + h * 0.04f,
                        ox + w * 0.64f, oy + h * 0.22f,
                        ox + w * 0.98f, oy + h * 0.10f
                    )
                }
                drawPath(
                    path = tigrisRiverPath,
                    color = Color(0xFF0284C7).copy(alpha = 0.42f * vectorOverlayAlpha),
                    style = Stroke(width = 28.dp.toPx() * zoomScale, cap = StrokeCap.Round)
                )
                drawPath(
                    path = tigrisRiverPath,
                    color = Color(0xFF38BDF8).copy(alpha = 0.82f * vectorOverlayAlpha),
                    style = Stroke(width = 5.dp.toPx() * zoomScale, cap = StrokeCap.Round)
                )

                // الطريق العام الرئيسي (طريق زمار - ربيعة وطريق زمار - تلعفر/الموصل) والتقاطعات
                val highwayHorizontal = Path().apply {
                    moveTo(ox + w * 0.02f, oy + h * 0.52f)
                    cubicTo(
                        ox + w * 0.35f, oy + h * 0.47f,
                        ox + w * 0.66f, oy + h * 0.56f,
                        ox + w * 0.98f, oy + h * 0.49f
                    )
                }
                val highwayVertical = Path().apply {
                    moveTo(ox + w * 0.49f, oy + h * 0.12f)
                    cubicTo(
                        ox + w * 0.51f, oy + h * 0.44f,
                        ox + w * 0.47f, oy + h * 0.72f,
                        ox + w * 0.53f, oy + h * 0.97f
                    )
                }
                val secondaryArtery = Path().apply {
                    moveTo(ox + w * 0.16f, oy + h * 0.26f)
                    cubicTo(
                        ox + w * 0.38f, oy + h * 0.38f,
                        ox + w * 0.62f, oy + h * 0.66f,
                        ox + w * 0.86f, oy + h * 0.82f
                    )
                }
                val roadColor = if (isSatelliteMode) Color(0xFFFDE68A).copy(alpha = 0.45f * vectorOverlayAlpha)
                else Color(0xFF475569).copy(alpha = 0.45f * vectorOverlayAlpha)
                drawPath(highwayHorizontal, roadColor, style = Stroke(width = 9.dp.toPx() * zoomScale, cap = StrokeCap.Round))
                drawPath(highwayVertical, roadColor, style = Stroke(width = 8.dp.toPx() * zoomScale, cap = StrokeCap.Round))
                drawPath(secondaryArtery, roadColor, style = Stroke(width = 5.dp.toPx() * zoomScale, cap = StrokeCap.Round))

                // فلكة وتقاطع مركز ناحية زمار
                drawCircle(
                    color = Color(0xFFD97706).copy(alpha = 0.65f * vectorOverlayAlpha),
                    radius = 9.dp.toPx() * zoomScale,
                    center = Offset(ox + w * 0.495f, oy + h * 0.515f)
                )

                val zoneTitlePaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                }

                // 3. رسم الأحياء والقواطع السكنية مع التفاعل البصري الحي (الكهرباء والماء)
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
                        val isMaintenance = zone.powerStatus == PowerStatus.MAINTENANCE
                        val isWaterActive = zone.waterStatus == WaterStatus.PUMPING_ON

                        val fillAlpha = when {
                            selectedLayer == ZummarMapLayerFilter.WATER -> if (isWaterActive) 0.34f else 0.16f
                            isMaintenance -> (0.20f + 0.18f * sin(electricFlowPhase * 2f * PI.toFloat()).absoluteValue)
                            isPowerActive -> 0.30f
                            else -> 0.15f
                        }

                        val baseZoneColor = when (selectedLayer) {
                            ZummarMapLayerFilter.WATER -> Color(zone.waterStatus.colorHex).copy(alpha = fillAlpha)
                            else -> Color(zone.powerStatus.colorHex).copy(alpha = fillAlpha)
                        }

                        drawPath(polyPath, color = baseZoneColor)

                        // منحنيات ونبضات التيار الكهربائي الكهرمانية داخل الحي المضاء
                        if ((selectedLayer == ZummarMapLayerFilter.ALL || selectedLayer == ZummarMapLayerFilter.ELECTRICITY) && isPowerActive) {
                            clipPath(polyPath) {
                                val minX = zone.points.minOf { it.x } * w + ox
                                val maxX = zone.points.maxOf { it.x } * w + ox
                                val minY = zone.points.minOf { it.y } * h + oy
                                val maxY = zone.points.maxOf { it.y } * h + oy
                                val spanY = (maxY - minY).coerceAtLeast(24f)

                                for (lineIdx in 0..3) {
                                    val yBase = minY + spanY * (0.20f + lineIdx * 0.20f)
                                    val shift = sin((electricFlowPhase * 2f * PI + lineIdx).toFloat()) * 14f
                                    val elecCurve = Path().apply {
                                        moveTo(minX, yBase)
                                        cubicTo(
                                            (minX + maxX) * 0.35f, yBase - 16f + shift,
                                            (minX + maxX) * 0.65f, yBase + 16f - shift,
                                            maxX, yBase
                                        )
                                    }
                                    drawPath(
                                        path = elecCurve,
                                        color = if (zone.powerStatus == PowerStatus.NATIONAL_ON) Color(0xFFFBBF24).copy(alpha = 0.82f)
                                        else Color(0xFF34D399).copy(alpha = 0.82f),
                                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        // أمواج المياه الفيروزية المتدفقة داخل الحي عند فتح ضخ الماء
                        if ((selectedLayer == ZummarMapLayerFilter.ALL || selectedLayer == ZummarMapLayerFilter.WATER) && isWaterActive) {
                            clipPath(polyPath) {
                                val minX = zone.points.minOf { it.x } * w + ox
                                val maxX = zone.points.maxOf { it.x } * w + ox
                                val minY = zone.points.minOf { it.y } * h + oy
                                val maxY = zone.points.maxOf { it.y } * h + oy
                                val spanY = (maxY - minY).coerceAtLeast(24f)

                                for (waveIdx in 0..3) {
                                    val yWave = minY + spanY * (0.22f + waveIdx * 0.20f)
                                    val waveOffset = sin(waterWavePhase + waveIdx) * 12f
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
                                        color = Color(0xFF00B4D8).copy(alpha = 0.78f),
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        val strokeColor = if (selectedLayer == ZummarMapLayerFilter.WATER) {
                            Color(zone.waterStatus.colorHex)
                        } else {
                            Color(zone.powerStatus.colorHex)
                        }
                        drawPath(
                            path = polyPath,
                            color = strokeColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        val centerX = ox + (zone.points.map { it.x }.average().toFloat()) * w
                        val centerY = oy + (zone.points.map { it.y }.average().toFloat()) * h
                        drawContext.canvas.nativeCanvas.drawText(
                            zone.name,
                            centerX,
                            centerY,
                            zoneTitlePaint
                        )
                    }
                }

                // 4. رسم المضلع الجاري رسمه حالياً (Polygon Tap / Freehand Lasso)
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
                    drawPath(draftPath, color = Color(0xFFD97706).copy(alpha = 0.30f))
                    drawPath(draftPath, color = Color(0xFFD97706), style = Stroke(width = 3.dp.toPx()))
                    draftPoints.forEach { pt ->
                        drawCircle(
                            color = Color(0xFFD97706),
                            radius = 6.dp.toPx(),
                            center = Offset(ox + pt.x * w, oy + pt.y * h)
                        )
                    }
                }

                // 5. رسم الدبابيس الفيكتورية المخصصة لكل معلم بدون تراكب النصوص فوق الخريطة
                approvedMarkers.forEach { marker ->
                    val mx = ox + marker.mapX * w
                    val my = oy + marker.mapY * h

                    val markerColor = when (marker.type) {
                        ZummarMarkerType.PHARMACY -> Color(0xFF10B981)
                        ZummarMarkerType.CLINIC -> Color(0xFF0284C7)
                        ZummarMarkerType.LAB -> Color(0xFF06B6D4)
                        ZummarMarkerType.SHOP -> Color(0xFFD97706)
                        ZummarMarkerType.GENERATOR -> Color(0xFFF59E0B)
                        ZummarMarkerType.GOV_OFFICE -> ZummarEmerald
                    }

                    // هالة رادار زمردية للصيدليات الخافرة الآن في زمار
                    if (marker.type == ZummarMarkerType.PHARMACY && marker.isOnDuty) {
                        drawCircle(
                            color = Color(0xFF10B981).copy(alpha = (1.8f - radarPulse).coerceIn(0.15f, 0.58f)),
                            radius = 20.dp.toPx() * radarPulse,
                            center = Offset(mx, my - 12.dp.toPx())
                        )
                    }

                    // دبوس الموقع الفيكتوري الملكي
                    val pinPath = Path().apply {
                        moveTo(mx, my + 2.dp.toPx())
                        cubicTo(
                            mx - 15.dp.toPx(), my - 10.dp.toPx(),
                            mx - 14.dp.toPx(), my - 28.dp.toPx(),
                            mx, my - 28.dp.toPx()
                        )
                        cubicTo(
                            mx + 14.dp.toPx(), my - 28.dp.toPx(),
                            mx + 15.dp.toPx(), my - 10.dp.toPx(),
                            mx, my + 2.dp.toPx()
                        )
                        close()
                    }
                    drawPath(pinPath, color = markerColor)
                    drawPath(pinPath, color = Color.White, style = Stroke(width = 2.2.dp.toPx()))
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(mx, my - 16.dp.toPx()))
                }
            }

            // شارة الإحداثيات الجغرافية الرسمية لناحية زمار في أعلى اليمين
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ZummarEmerald.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFFD97706)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "ناحية زمار • 36.6575°N, 42.6065°E",
                    fontFamily = ZummarTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // أزرار التكبير والتصغير وإعادة التمركز
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ZummarEmerald.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { zoomScale = (zoomScale + 0.3f).coerceAtMost(3.2f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ZummarCustomIcon(ZummarVectorIconType.ZOOM_IN, Color.White, Modifier.size(18.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = ZummarEmerald.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            zoomScale = (zoomScale - 0.3f).coerceAtLeast(1f)
                            if (zoomScale == 1f) panOffset = Offset.Zero
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ZummarCustomIcon(ZummarVectorIconType.ZOOM_OUT, Color.White, Modifier.size(18.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = ZummarEmerald.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ZummarCustomIcon(ZummarVectorIconType.RESET_VIEW, Color(0xFFFBBF24), Modifier.size(18.dp))
                    }
                }
            }

            // مفتاح دلالات الألوان أسفل الخريطة
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ZummarNavy.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(Color(0xFFFBBF24), "وطني / متجر")
                    LegendDot(Color(0xFF10B981), "مولدة / خافرة")
                    LegendDot(Color(0xFF00B4D8), "ضخ ماء / عيادة")
                    LegendDot(ZummarCrimson, "عطل وصيانة")
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
