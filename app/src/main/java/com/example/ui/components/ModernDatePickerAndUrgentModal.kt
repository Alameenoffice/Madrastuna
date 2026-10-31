package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.UrgentSchoolAlert
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Helper to compute the Arabic day name (e.g. "الأحد", "الإثنين") from a "YYYY-MM-DD" date string.
 */
object ArabicCalendarDateHelper {
    val arabicMonths = listOf(
        1 to "كانون الثاني (1)",
        2 to "شباط (2)",
        3 to "آذار (3)",
        4 to "نيسان (4)",
        5 to "أيار (5)",
        6 to "حزيران (6)",
        7 to "تموز (7)",
        8 to "آب (8)",
        9 to "أيلول (9)",
        10 to "تشرين الأول (10)",
        11 to "تشرين الثاني (11)",
        12 to "كانون الأول (12)"
    )

    fun todayIsoString(): String {
        val cal = Calendar.getInstance()
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun todayIsoDate(): String = todayIsoString()

    fun plusDaysIsoString(days: Int): String {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun getArabicDayName(dateIso: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.isLenient = false
            val parsed = sdf.parse(dateIso.trim().take(10)) ?: return "الأحد"
            val cal = Calendar.getInstance().apply { time = parsed }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "الأحد"
                Calendar.MONDAY -> "الإثنين"
                Calendar.TUESDAY -> "الثلاثاء"
                Calendar.WEDNESDAY -> "الأربعاء"
                Calendar.THURSDAY -> "الخميس"
                Calendar.FRIDAY -> "الجمعة"
                Calendar.SATURDAY -> "السبت"
                else -> "الأحد"
            }
        } catch (_: Exception) {
            "الأحد"
        }
    }

    fun arabicDayOfWeek(dateIso: String): String = getArabicDayName(dateIso)

    fun formatFullArabicDateWithDay(dateIso: String): String {
        val cleanIso = dateIso.trim().take(10)
        return "${getArabicDayName(cleanIso)} • $cleanIso"
    }

    fun parseYearMonthDay(dateIso: String): Triple<Int, Int, Int> {
        return try {
            val clean = dateIso.trim().take(10)
            val parts = clean.split("-")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull() ?: 2026
                val m = (parts[1].toIntOrNull() ?: 10).coerceIn(1, 12)
                val d = (parts[2].toIntOrNull() ?: 1).coerceIn(1, 31)
                Triple(y, m, d)
            } else {
                val cal = Calendar.getInstance()
                Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            }
        } catch (_: Exception) {
            val cal = Calendar.getInstance()
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }
    }

    fun daysInMonth(year: Int, month1Based: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month1Based - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun formatIso(year: Int, month1Based: Int, day: Int): String {
        val maxD = daysInMonth(year, month1Based)
        val safeD = day.coerceIn(1, maxD)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month1Based.coerceIn(1, 12), safeD)
    }
}

/**
 * Unified Interactive Modern Date Picker Field (محرك التقويم التفاعلي الموحد لاختيار اليوم والشهر والسنة بلمسة واحدة)
 */
@Composable
fun ModernCalendarDatePickerField(
    label: String,
    selectedDateIso: String,
    onDateSelected: (newDateIso: String, arabicDayName: String) -> Unit,
    modifier: Modifier = Modifier,
    showDayNameBadge: Boolean = true,
    testTag: String = "modern_date_picker_field"
) {
    var showPickerModal by remember { mutableStateOf(false) }
    val displayDate = selectedDateIso.ifBlank { ArabicCalendarDateHelper.todayIsoString() }
    val dayName = remember(displayDate) { ArabicCalendarDateHelper.getArabicDayName(displayDate) }

    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showPickerModal = true }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = DeepEmerald.copy(alpha = 0.06f),
        border = BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = label,
                    tint = DeepEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepForestGreen,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Filled.EventAvailable,
                contentDescription = label,
                tint = DeepEmerald,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    if (showPickerModal) {
        ModernCalendarDatePickerDialog(
            title = label,
            initialDateIso = displayDate,
            onDismiss = { showPickerModal = false },
            onConfirmDate = { chosenIso, chosenDayName ->
                onDateSelected(chosenIso, chosenDayName)
                showPickerModal = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModernCalendarDatePickerDialog(
    title: String,
    initialDateIso: String,
    onDismiss: () -> Unit,
    onConfirmDate: (dateIso: String, arabicDayName: String) -> Unit
) {
    val initialTriple = remember(initialDateIso) {
        ArabicCalendarDateHelper.parseYearMonthDay(initialDateIso)
    }
    var selectedYear by remember(initialTriple) { mutableIntStateOf(initialTriple.first) }
    var selectedMonth by remember(initialTriple) { mutableIntStateOf(initialTriple.second) }
    var selectedDay by remember(initialTriple) { mutableIntStateOf(initialTriple.third) }

    val maxDays = remember(selectedYear, selectedMonth) {
        ArabicCalendarDateHelper.daysInMonth(selectedYear, selectedMonth)
    }
    val safeDay = selectedDay.coerceIn(1, maxDays)
    val previewIso = remember(selectedYear, selectedMonth, safeDay) {
        ArabicCalendarDateHelper.formatIso(selectedYear, selectedMonth, safeDay)
    }
    val previewDayName = remember(previewIso) {
        ArabicCalendarDateHelper.getArabicDayName(previewIso)
    }

    val yearsList = remember { (2024..2030).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = DeepEmerald
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Selected Date Preview Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = DeepEmerald,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "التاريخ المحدد:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "$previewDayName • $previewIso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccent
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.EventAvailable,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Quick Jump Shortcuts (اليوم / غداً / بعد أسبوع / بعد سنة)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val quickOptions = listOf(
                        "اليوم" to 0,
                        "غداً" to 1,
                        "بعد 3 أيام" to 3,
                        "بعد أسبوع" to 7,
                        "بعد شهر" to 30,
                        "بعد سنة" to 365
                    )
                    items(quickOptions) { (qLabel, daysOffset) ->
                        val targetIso = ArabicCalendarDateHelper.plusDaysIsoString(daysOffset)
                        FilterChip(
                            selected = previewIso == targetIso,
                            onClick = {
                                val (y, m, d) = ArabicCalendarDateHelper.parseYearMonthDay(targetIso)
                                selectedYear = y
                                selectedMonth = m
                                selectedDay = d
                            },
                            label = { Text(qLabel, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepEmerald.copy(alpha = 0.16f),
                                selectedLabelColor = DeepEmerald
                            )
                        )
                    }
                }

                // 1. Year Selector
                Text(
                    text = "السنة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestGreen
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(yearsList) { yr ->
                        FilterChip(
                            selected = selectedYear == yr,
                            onClick = { selectedYear = yr },
                            label = {
                                Text(
                                    text = "$yr",
                                    fontWeight = if (selectedYear == yr) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                // 2. Month Selector with Prev/Next arrows + scrollable chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الشهر:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestGreen
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonth > 1) selectedMonth -= 1 else {
                                    selectedMonth = 12
                                    selectedYear -= 1
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "الشهر السابق")
                        }
                        Text(
                            text = ArabicCalendarDateHelper.arabicMonths.firstOrNull { it.first == selectedMonth }?.second ?: "$selectedMonth",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                        IconButton(
                            onClick = {
                                if (selectedMonth < 12) selectedMonth += 1 else {
                                    selectedMonth = 1
                                    selectedYear += 1
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "الشهر التالي")
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ArabicCalendarDateHelper.arabicMonths) { (mNum, mLabel) ->
                        FilterChip(
                            selected = selectedMonth == mNum,
                            onClick = { selectedMonth = mNum },
                            label = { Text(mLabel, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                // 3. Interactive Day Grid (1 .. maxDays)
                Text(
                    text = "اختر اليوم من الشهر (1 - $maxDays):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestGreen
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 7
                ) {
                    for (dayNum in 1..maxDays) {
                        val isChosen = safeDay == dayNum
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { selectedDay = dayNum },
                            shape = CircleShape,
                            color = if (isChosen) DeepEmerald else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            contentColor = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurface,
                            border = if (isChosen) BorderStroke(1.5.dp, GoldAccent) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$dayNum",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isChosen) FontWeight.ExtraBold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            VectorActionButton(
                text = "اعتماد التاريخ ($previewIso)",
                icon = Icons.Filled.CheckCircle,
                onClick = { onConfirmDate(previewIso, previewDayName) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Modal Dialog for Principal's Urgent School Alert ("إدارة التنبيه العاجل للمدرسة")
 * Accessible directly from the Principal's Side Drawer or from the active banner's "تعديل التنبيه" button.
 */
@Composable
fun UrgentSchoolAlertModalDialog(
    currentAlert: UrgentSchoolAlert,
    onDismiss: () -> Unit,
    onPublishOrUpdate: (String) -> Unit,
    onStopOrDelete: () -> Unit
) {
    var draftMessage by remember(currentAlert.message, currentAlert.isActive) {
        mutableStateOf(currentAlert.message)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldAccent.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = GoldDeep,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "إدارة التنبيه العاجل للمدرسة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentAlert.isActive && currentAlert.message.isNotBlank()) "حالة التنبيه الآن: منشور ونشط في الرئيسية" else "حالة التنبيه الآن: متوقف ومخفي",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (currentAlert.isActive && currentAlert.message.isNotBlank()) DeepEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "يظهر هذا التنبيه العاجل في أعلى الشاشة الرئيسية لجميع حسابات المدرسة (المعلمين، الطلاب، وأولياء الأمور) فور نشره، ويختفي تماماً عند إيقافه.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = draftMessage,
                    onValueChange = { draftMessage = it },
                    label = { Text("نص التنبيه العاجل للمدرسة (مثال: غداً عطلة رسمية / تأجيل امتحان...)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("modal_urgent_alert_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                if (currentAlert.isActive && currentAlert.message.isNotBlank()) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = {
                            onStopOrDelete()
                            draftMessage = ""
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_stop_urgent_alert_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إيقاف وحذف التنبيه العاجل نهائياً",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            VectorActionButton(
                text = if (currentAlert.isActive && currentAlert.message.isNotBlank()) "حفظ وتحديث التنبيه" else "نشر التنبيه العاجل الآن",
                icon = Icons.Filled.NotificationsActive,
                onClick = {
                    if (draftMessage.isNotBlank()) {
                        onPublishOrUpdate(draftMessage.trim())
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("modal_publish_urgent_alert_btn")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * Custom Crisp Linear Vector SVG Icon for "نشاطات الطلبة" (Social Network / Community Hub)
 * Interconnected people community nodes + interactive sharing/chat bubble lines (Zero lightbulb / Zero stars)
 */
val StudentActivitiesLinearVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "StudentActivitiesSocialCommunityHubVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Central Community Person Head (Circle)
            moveTo(9.2f, 10.2f)
            curveTo(10.86f, 10.2f, 12.2f, 8.86f, 12.2f, 7.2f)
            curveTo(12.2f, 5.54f, 10.86f, 4.2f, 9.2f, 4.2f)
            curveTo(7.54f, 4.2f, 6.2f, 5.54f, 6.2f, 7.2f)
            curveTo(6.2f, 8.86f, 7.54f, 10.2f, 9.2f, 10.2f)
            close()

            // Central Community Person Shoulders
            moveTo(3.4f, 19.6f)
            curveTo(3.4f, 16.4f, 5.9f, 13.9f, 9.2f, 13.9f)
            curveTo(12.5f, 13.9f, 15.0f, 16.4f, 15.0f, 19.6f)

            // Secondary Connected Peer Head (Right)
            moveTo(16.6f, 11.4f)
            curveTo(17.85f, 11.4f, 18.85f, 10.4f, 18.85f, 9.15f)
            curveTo(18.85f, 7.9f, 17.85f, 6.9f, 16.6f, 6.9f)
            curveTo(15.6f, 6.9f, 14.75f, 7.55f, 14.45f, 8.45f)

            // Secondary Connected Peer Shoulders
            moveTo(16.4f, 14.5f)
            curveTo(19.0f, 14.7f, 21.0f, 16.7f, 21.0f, 19.4f)

            // Interactive Sharing / Social Chat Bubble Badge (Top-Right Hub)
            moveTo(15.2f, 2.5f)
            lineTo(20.2f, 2.5f)
            curveTo(21.1f, 2.5f, 21.8f, 3.2f, 21.8f, 4.1f)
            lineTo(21.8f, 6.2f)
            curveTo(21.8f, 7.1f, 21.1f, 7.8f, 20.2f, 7.8f)
            lineTo(17.6f, 7.8f)
            lineTo(15.8f, 9.2f)
            lineTo(15.8f, 7.8f)
            lineTo(15.2f, 7.8f)
            curveTo(14.3f, 7.8f, 13.6f, 7.1f, 13.6f, 6.2f)
            lineTo(13.6f, 4.1f)
            curveTo(13.6f, 3.2f, 14.3f, 2.5f, 15.2f, 2.5f)
            close()
        }
    }.build()
}

@Composable
fun StudentActivitiesVectorIcon(
    tint: Color = DeepEmerald,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = StudentActivitiesLinearVector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/**
 * Calm Royal Emerald & Gold Holiday Status Banner displayed in Dashboard & Attendance Screen
 * whenever the selected date or today is a weekly or official school holiday.
 */
@Composable
fun OfficialHolidayBannerCard(
    holidayLabel: String,
    dateIso: String,
    isAdmin: Boolean = false,
    onManageHolidaysClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("official_holiday_banner_card"),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF042F2E),
                            DeepEmerald,
                            Color(0xFF065F46)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldAccent.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "اليوم عطلة ($holidayLabel)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "التاريخ: ${ArabicCalendarDateHelper.formatFullArabicDateWithDay(dateIso)} • يوم عطلة رسمية (موثق في السجل)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (isAdmin && onManageHolidaysClick != null) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onManageHolidaysClick() }
                                .testTag("banner_manage_holidays_btn"),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "إدارة العطل",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal Dialog for the School Principal to manage:
 * 1) Fixed Weekly Days Off (أيام العطلة الأسبوعية الثابتة: الأحد..السبت)
 * 2) Official / Emergency Holidays on specific dates (تثبيت يوم معين كعطلة رسمية/طارئة)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchoolHolidaysManagementModalDialog(
    holidaysConfig: com.example.model.SchoolHolidaysConfig,
    onToggleWeeklyDay: (String) -> Unit,
    onSaveOfficialHoliday: (holidayId: String?, dateIso: String, title: String) -> Unit,
    onDeleteOfficialHoliday: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val allWeekDays = remember {
        listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    }
    var selectedHolidayDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var holidayTitleInput by remember { mutableStateOf("عطلة رسمية") }
    var editingHolidayId by remember { mutableStateOf<String?>(null) }

    val quickOccasionPresets = remember {
        listOf("عطلة رسمية", "مناسبة وطنية", "عطلة جوية", "عطلة نصف السنة", "مناسبة دينية")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = DeepEmerald.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = DeepEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "إدارة العطل الأسبوعية والرسمية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald
                    )
                    Text(
                        text = "توثيق تلقائي في سجلات حضور جميع الطلبة ومنع احتساب الغياب",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. أيام العطلة الأسبوعية الثابتة
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DeepEmerald.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "1. أيام العطلة الأسبوعية الثابتة:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepEmerald
                        )
                        Text(
                            text = "اضغط لتحديد أو إلغاء أيام العطلة الأسبوعية للمدرسة (تُقفل أزرار الحضور تلقائياً في هذه الأيام):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allWeekDays.forEach { dayLabel ->
                                val normDay = com.example.model.ArabicTextNormalizer.normalizeText(dayLabel)
                                val isSelected = holidaysConfig.weeklyDaysOff.any {
                                    com.example.model.ArabicTextNormalizer.normalizeText(it) == normDay
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onToggleWeeklyDay(dayLabel) }
                                        .testTag("weekly_holiday_chip_$dayLabel"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DeepEmerald else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) GoldAccent else DeepEmerald.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = GoldAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = dayLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. تثبيت يوم معين كعطلة رسمية / طارئة
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GoldAccent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (editingHolidayId == null) "2. تثبيت يوم معين كعطلة رسمية / طارئة:" else "2. تعديل بيانات العطلة الرسمية:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepEmerald
                        )

                        ModernCalendarDatePickerField(
                            selectedDateIso = selectedHolidayDate,
                            onDateSelected = { selectedIso, _ -> selectedHolidayDate = selectedIso },
                            label = "تاريخ العطلة",
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = holidayTitleInput,
                            onValueChange = { holidayTitleInput = it },
                            label = { Text("سبب / مسمى العطلة الرسمية") },
                            placeholder = { Text("مثال: عطلة رسمية، مناسبة وطنية، عطلة جوية...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("official_holiday_title_input")
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickOccasionPresets.forEach { preset ->
                                val active = holidayTitleInput.trim() == preset
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { holidayTitleInput = preset },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) DeepEmerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSaveOfficialHoliday(editingHolidayId, selectedHolidayDate, holidayTitleInput)
                                    editingHolidayId = null
                                    holidayTitleInput = "عطلة رسمية"
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_official_holiday_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (editingHolidayId == null) "تثبيت وتوثيق العطلة في السجل" else "حفظ تعديل العطلة",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            if (editingHolidayId != null) {
                                OutlinedButton(
                                    onClick = {
                                        editingHolidayId = null
                                        holidayTitleInput = "عطلة رسمية"
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("إلغاء", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. قائمة العطل الرسمية المثبتة
                if (holidaysConfig.officialHolidays.isNotEmpty()) {
                    Text(
                        text = "العطل الرسمية والطارئة المثبتة (${holidaysConfig.officialHolidays.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        holidaysConfig.officialHolidays.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.18f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepEmerald
                                        )
                                        Text(
                                            text = ArabicCalendarDateHelper.formatFullArabicDateWithDay(item.dateIso),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    editingHolidayId = item.id
                                                    selectedHolidayDate = item.dateIso
                                                    holidayTitleInput = item.title
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = DeepEmerald.copy(alpha = 0.10f)
                                        ) {
                                            Text(
                                                text = "تعديل",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepEmerald,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteOfficialHoliday(item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "حذف العطلة",
                                                tint = DangerRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold, color = DeepEmerald)
            }
        }
    )
}


