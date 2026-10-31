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
import java.text.SimpleDateFormat
import java.util.Calendar
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
 * Custom Crisp Linear Vector SVG Icon for "نشاطات الطلبة" (Smart Creative Idea Lightbulb - no stars)
 */
val StudentActivitiesLinearVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "StudentActivitiesSmartBulbVector",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Smart Lightbulb Dome & Neck
            moveTo(9f, 16.2f)
            lineTo(8.3f, 14.4f)
            curveTo(6.9f, 13.1f, 6.2f, 11.4f, 6.2f, 9.5f)
            curveTo(6.2f, 6.3f, 8.8f, 3.8f, 12f, 3.8f)
            curveTo(15.2f, 3.8f, 17.8f, 6.3f, 17.8f, 9.5f)
            curveTo(17.8f, 11.4f, 17.1f, 13.1f, 15.7f, 14.4f)
            lineTo(15f, 16.2f)
            close()

            // Inner Creative Filament
            moveTo(10.3f, 11.2f)
            lineTo(12f, 9.2f)
            lineTo(13.7f, 11.2f)
            moveTo(12f, 9.2f)
            lineTo(12f, 14.2f)

            // Base Screw Threads
            moveTo(9.4f, 19f)
            lineTo(14.6f, 19f)
            moveTo(10.4f, 21.5f)
            lineTo(13.6f, 21.5f)

            // Subtle Idea Rays (Top & Sides)
            moveTo(12f, 1.4f)
            lineTo(12f, 2.3f)
            moveTo(4.2f, 5.2f)
            lineTo(5.1f, 5.9f)
            moveTo(19.8f, 5.2f)
            lineTo(18.9f, 5.9f)
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

