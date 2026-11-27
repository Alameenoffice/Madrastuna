package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OccasionIconKey
import com.example.model.OccasionThemeConfig
import com.example.model.OccasionThemeType
import com.example.model.UrgentSchoolAlert
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Official Crisp Vector SVG Icon for "دفتر المتابعة اليومي والسلوك"
 * (Clipboard / Notebook with Checkmark ✓ and lines — ClipboardCheck / NotebookPen).
 */
val ClipboardCheckVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ClipboardCheckVectorIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Clipboard body + top clip
        path(
            fill = SolidColor(Color.Black),
            pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero
        ) {
            // Outer clipboard rounded frame
            moveTo(19f, 3f)
            horizontalLineTo(14.82f)
            curveTo(14.4f, 1.84f, 13.3f, 1f, 12f, 1f)
            curveTo(10.7f, 1f, 9.6f, 1.84f, 9.18f, 3f)
            horizontalLineTo(5f)
            curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
            verticalLineTo(19f)
            curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
            horizontalLineTo(19f)
            curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
            verticalLineTo(5f)
            curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
            close()
            // Top clip center hole
            moveTo(12f, 3f)
            curveTo(12.55f, 3f, 13f, 3.45f, 13f, 4f)
            curveTo(13f, 4.55f, 12.55f, 5f, 12f, 5f)
            curveTo(11.45f, 5f, 11f, 4.55f, 11f, 4f)
            curveTo(11f, 3.45f, 11.45f, 3f, 12f, 3f)
            close()
            // Inner notebook page cutout
            moveTo(19f, 19f)
            horizontalLineTo(5f)
            verticalLineTo(5f)
            horizontalLineTo(7f)
            verticalLineTo(7f)
            horizontalLineTo(17f)
            verticalLineTo(5f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            close()
            // Checkmark ✓ inside the notebook
            moveTo(10.6f, 16.2f)
            lineTo(7.4f, 13.0f)
            lineTo(8.8f, 11.6f)
            lineTo(10.6f, 13.4f)
            lineTo(15.2f, 8.8f)
            lineTo(16.6f, 10.2f)
            lineTo(10.6f, 16.2f)
            close()
        }
    }.build()
}

/**
 * Progressive Infinite-Scroll Sentinel + Fallback "عرض المزيد" Button.
 * Automatically triggers [onLoadMore] when this footer enters the Compose viewport on scroll,
 * while also displaying a clean luxury button for immediate tap.
 */
@Composable
fun ProgressiveInfiniteScrollFooter(
    visibleCount: Int,
    totalCount: Int,
    step: Int,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: String = "عنصر"
) {
    if (visibleCount >= totalCount) return
    val remaining = (totalCount - visibleCount).coerceAtLeast(0)
    val nextBatch = remaining.coerceAtMost(step)

    // Viewport intersection auto-trigger on scroll down
    LaunchedEffect(visibleCount, totalCount) {
        delay(220L)
        onLoadMore()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(
            onClick = onLoadMore,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFECFDF5).copy(alpha = 0.7f),
                contentColor = Color(0xFF065F46)
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            modifier = Modifier.testTag("progressive_load_more_button")
        ) {
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "عرض المزيد",
                tint = Color(0xFF059669),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "عرض المزيد ($nextBatch من أصل $remaining $itemLabel متبقية)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF065F46)
            )
        }
    }
}

/**
 * Urgent School Alert Banner displayed at the top of the Dashboard for all members of the school
 * (Teachers, Students, Parents, and Admin) with a dismiss (×) button and direct Admin Edit/Delete controls.
 */
@Composable
fun UrgentSchoolAlertBannerCard(
    alert: UrgentSchoolAlert,
    isAdmin: Boolean,
    onDismissForUser: () -> Unit,
    onEditRequest: () -> Unit,
    onDeleteAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!alert.isActive || alert.message.isBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "urgent_bell_ring")
    val bellRotation by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_rotation"
    )
    val bellPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 840, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_pulse_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("urgent_school_alert_banner_card"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // على اليمين: أيقونة تنبيه خطية زمردية صافية داخل مربع ناعم (32.dp)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFD1FAE5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = "تنبيه عاجل",
                    tint = Color(0xFF059669),
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = bellRotation
                            scaleX = bellPulseScale
                            scaleY = bellPulseScale
                        }
                )
            }

            // في الوسط: العنوان كشارة زمردية رقيقة + نص الرسالة بلون كحلي/رمادي مريح
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFD1FAE5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Text(
                        text = "تنبيه عاجل",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF065F46),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // على اليسار: أزرار الإجراءات وزر إغلاق (X) صغير وأنيق لإخفائه فوراً
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD1FAE5))
                            .clickable(onClick = onEditRequest)
                            .testTag("admin_edit_urgent_alert_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "تعديل التنبيه",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEE2E2).copy(alpha = 0.75f))
                            .clickable(onClick = onDeleteAlert)
                            .testTag("admin_delete_urgent_alert_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "إيقاف التنبيه",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD1FAE5).copy(alpha = 0.6f))
                        .clickable(onClick = onDismissForUser)
                        .testTag("dismiss_urgent_alert_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق التنبيه",
                        tint = Color(0xFF065F46),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * School Principal (Admin) Tool Card for publishing, editing, or deleting/stopping the Urgent School Alert.
 */
@Composable
fun AdminUrgentSchoolAlertManagerCard(
    currentAlert: UrgentSchoolAlert,
    isEditingRequested: Boolean,
    onEditRequestConsumed: () -> Unit,
    onPublishOrUpdate: (String) -> Unit,
    onDeleteOrStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var draftMessage by remember(currentAlert.message, currentAlert.isActive) {
        mutableStateOf(if (currentAlert.isActive) currentAlert.message else "")
    }
    var statusFeedback by remember { mutableStateOf("") }

    LaunchedEffect(isEditingRequested) {
        if (isEditingRequested) {
            draftMessage = currentAlert.message
            expanded = true
            onEditRequestConsumed()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_urgent_alert_manager_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            if (currentAlert.isActive && currentAlert.message.isNotBlank()) Color(0xFFF59E0B)
            else Color(0xFF10B981).copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = "التنبيه العاجل للمدرسة",
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "التنبيه العاجل للمدرسة (إدارة المدير)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF064E3B)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (currentAlert.isActive && currentAlert.message.isNotBlank())
                                    Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (currentAlert.isActive && currentAlert.message.isNotBlank()) "منشور حالياً" else "غير مفعل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentAlert.isActive && currentAlert.message.isNotBlank())
                                        Color(0xFFB45309) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "نشر أو تعديل أو إيقاف تنبيه عاجل يظهر أعلى الشاشة لجميع منتسبي مدرستك فوراً",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_urgent_alert_manager_button")
                ) {
                    Text(
                        text = if (expanded) "إخفاء الأداة" else if (currentAlert.isActive) "تعديل / إيقاف" else "نشر تنبيه عاجل",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = draftMessage,
                        onValueChange = {
                            draftMessage = it
                            statusFeedback = ""
                        },
                        label = { Text("نص التنبيه العاجل للمدرسة (مثل: غداً عطلة رسمية / تأجيل امتحان...)") },
                        placeholder = { Text("اكتب نص التنبيه العاجل ليظهر فوراً لجميع المعلمين والطلاب وأولياء الأمور في مدرستك...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("urgent_school_alert_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    if (statusFeedback.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = statusFeedback,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
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
                                val trimmed = draftMessage.trim()
                                if (trimmed.isNotEmpty()) {
                                    onPublishOrUpdate(trimmed)
                                    statusFeedback = if (currentAlert.isActive) {
                                        "تم تحديث نص التنبيه العاجل ومزامنته لحظياً لجميع حسابات المدرسة."
                                    } else {
                                        "تم نشر التنبيه العاجل أعلى الشاشة لجميع منتسبي المدرسة فوراً."
                                    }
                                }
                            },
                            enabled = draftMessage.trim().isNotEmpty(),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("publish_or_update_urgent_alert_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF047857),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (currentAlert.isActive && currentAlert.message.isNotBlank())
                                    Icons.Filled.Edit else Icons.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentAlert.isActive && currentAlert.message.isNotBlank())
                                    "تعديل التنبيه المنشور" else "نشر التنبيه العاجل فوراً",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (currentAlert.isActive && currentAlert.message.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    onDeleteOrStop()
                                    draftMessage = ""
                                    statusFeedback = "تم حذف وإيقاف التنبيه العاجل وإزالته من شاشات جميع المستخدمين فوراً."
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stop_urgent_alert_manager_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFFEF2F2),
                                    contentColor = Color(0xFFB91C1C)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "حذف / إيقاف التنبيه",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 11-Icon Luxury Vector SVG Selector Grid for the Developer Occasion Theme form.
 * Allows selecting any of the 11 Vector SVG icons with a single tap:
 * (تاج، هلال، قبعة تخرج، كأس، درع، علم، كتاب، نجمة، تقويم، وردة، جرس)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OccasionVectorIconSelectorGrid(
    selectedIconKey: OccasionIconKey,
    hasCustomImage: Boolean,
    onSelectIcon: (OccasionIconKey) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2
    ) {
        OccasionIconKey.primary3DBroadcastChoices.forEach { iconKey ->
            val isSelected = selectedIconKey == iconKey
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectIcon(iconKey) }
                    .testTag("select_occasion_vector_icon_${iconKey.name}"),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Color(0xFFECFDF5) else Color.White,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF059669) else Color(0xFFD1FAE5)
                ),
                shadowElevation = if (isSelected) 4.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedOccasionVectorCanvas(
                        iconKey = iconKey,
                        titleHint = iconKey.arabicLabel,
                        modifier = Modifier.size(44.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = iconKey.arabicLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) Color(0xFF065F46) else Color(0xFF1E293B),
                                maxLines = 2,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders any of the 11 Developer Alert Icons in pure Emerald (0xFF059669) inside a soft Emerald box (0xFFD1FAE5).
 */
@Composable
fun DeveloperSelectedIconBox(
    iconKey: OccasionIconKey,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFD1FAE5)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height
            val emeraldColor = Color(0xFF059669)
            val deepEmeraldColor = Color(0xFF047857)
            val emeraldBrush = Brush.linearGradient(
                colors = listOf(emeraldColor, deepEmeraldColor)
            )
            when (iconKey) {
                OccasionIconKey.CROWN -> {
                    val crownPath = Path().apply {
                        moveTo(w * 0.18f, h * 0.72f)
                        lineTo(w * 0.14f, h * 0.36f)
                        lineTo(w * 0.36f, h * 0.52f)
                        lineTo(w * 0.50f, h * 0.24f)
                        lineTo(w * 0.64f, h * 0.52f)
                        lineTo(w * 0.86f, h * 0.36f)
                        lineTo(w * 0.82f, h * 0.72f)
                        close()
                    }
                    drawPath(path = crownPath, brush = emeraldBrush)
                    drawRoundRect(
                        brush = emeraldBrush,
                        topLeft = Offset(w * 0.18f, h * 0.76f),
                        size = Size(w * 0.64f, h * 0.09f),
                        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                    )
                }
                OccasionIconKey.CRESCENT -> {
                    val crescentPath = Path().apply {
                        val cx = w * 0.48f
                        val cy = h * 0.50f
                        val rOuter = w * 0.32f
                        val rInner = w * 0.25f
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                cx - rOuter,
                                cy - rOuter,
                                cx + rOuter,
                                cy + rOuter
                            )
                        )
                        val cutout = Path().apply {
                            addOval(
                                androidx.compose.ui.geometry.Rect(
                                    cx - rInner + w * 0.12f,
                                    cy - rInner - h * 0.04f,
                                    cx + rInner + w * 0.12f,
                                    cy + rInner - h * 0.04f
                                )
                            )
                        }
                        op(this, cutout, androidx.compose.ui.graphics.PathOperation.Difference)
                    }
                    drawPath(path = crescentPath, brush = emeraldBrush)
                    drawStar(
                        center = Offset(w * 0.68f, h * 0.34f),
                        outerRadius = w * 0.11f,
                        innerRadius = w * 0.048f,
                        color = emeraldColor
                    )
                }
                OccasionIconKey.GRADUATION_CAP -> {
                    val capTop = Path().apply {
                        moveTo(w * 0.50f, h * 0.22f)
                        lineTo(w * 0.88f, h * 0.42f)
                        lineTo(w * 0.50f, h * 0.62f)
                        lineTo(w * 0.12f, h * 0.42f)
                        close()
                    }
                    drawPath(capTop, brush = emeraldBrush)
                    val skullCap = Path().apply {
                        moveTo(w * 0.28f, h * 0.54f)
                        lineTo(w * 0.28f, h * 0.72f)
                        quadraticTo(w * 0.50f, h * 0.84f, w * 0.72f, h * 0.72f)
                        lineTo(w * 0.72f, h * 0.54f)
                        lineTo(w * 0.50f, h * 0.65f)
                        close()
                    }
                    drawPath(skullCap, color = deepEmeraldColor)
                }
                OccasionIconKey.TROPHY -> {
                    val cupPath = Path().apply {
                        moveTo(w * 0.26f, h * 0.22f)
                        lineTo(w * 0.74f, h * 0.22f)
                        lineTo(w * 0.66f, h * 0.54f)
                        quadraticTo(w * 0.50f, h * 0.68f, w * 0.34f, h * 0.54f)
                        close()
                    }
                    drawPath(cupPath, brush = emeraldBrush)
                    drawLine(
                        color = emeraldColor,
                        start = Offset(w * 0.50f, h * 0.58f),
                        end = Offset(w * 0.50f, h * 0.76f),
                        strokeWidth = w * 0.08f
                    )
                    drawRoundRect(
                        brush = emeraldBrush,
                        topLeft = Offset(w * 0.30f, h * 0.74f),
                        size = Size(w * 0.40f, h * 0.09f),
                        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                    )
                }
                OccasionIconKey.SHIELD -> {
                    val shield = Path().apply {
                        moveTo(w * 0.50f, h * 0.16f)
                        lineTo(w * 0.80f, h * 0.28f)
                        lineTo(w * 0.80f, h * 0.52f)
                        quadraticTo(w * 0.76f, h * 0.76f, w * 0.50f, h * 0.86f)
                        quadraticTo(w * 0.24f, h * 0.76f, w * 0.20f, h * 0.52f)
                        lineTo(w * 0.20f, h * 0.28f)
                        close()
                    }
                    drawPath(shield, brush = emeraldBrush)
                    drawStar(
                        center = Offset(w * 0.50f, h * 0.50f),
                        outerRadius = w * 0.15f,
                        innerRadius = w * 0.06f,
                        color = Color.White
                    )
                }
                OccasionIconKey.FLAG -> {
                    drawLine(
                        brush = emeraldBrush,
                        start = Offset(w * 0.24f, h * 0.18f),
                        end = Offset(w * 0.24f, h * 0.84f),
                        strokeWidth = w * 0.065f,
                        cap = StrokeCap.Round
                    )
                    val flagPath = Path().apply {
                        moveTo(w * 0.27f, h * 0.22f)
                        cubicTo(w * 0.44f, h * 0.16f, w * 0.60f, h * 0.30f, w * 0.80f, h * 0.24f)
                        lineTo(w * 0.80f, h * 0.54f)
                        cubicTo(w * 0.60f, h * 0.60f, w * 0.44f, h * 0.46f, w * 0.27f, h * 0.54f)
                        close()
                    }
                    drawPath(flagPath, brush = emeraldBrush)
                }
                OccasionIconKey.BOOK -> {
                    val leftPage = Path().apply {
                        moveTo(w * 0.50f, h * 0.28f)
                        quadraticTo(w * 0.33f, h * 0.22f, w * 0.16f, h * 0.28f)
                        lineTo(w * 0.16f, h * 0.74f)
                        quadraticTo(w * 0.33f, h * 0.68f, w * 0.50f, h * 0.74f)
                        close()
                    }
                    val rightPage = Path().apply {
                        moveTo(w * 0.50f, h * 0.28f)
                        quadraticTo(w * 0.67f, h * 0.22f, w * 0.84f, h * 0.28f)
                        lineTo(w * 0.84f, h * 0.74f)
                        quadraticTo(w * 0.67f, h * 0.68f, w * 0.50f, h * 0.74f)
                        close()
                    }
                    drawPath(leftPage, brush = emeraldBrush)
                    drawPath(rightPage, brush = emeraldBrush)
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.50f, h * 0.26f),
                        end = Offset(w * 0.50f, h * 0.75f),
                        strokeWidth = w * 0.045f
                    )
                }
                OccasionIconKey.STAR -> {
                    drawStar(
                        center = Offset(w * 0.50f, h * 0.50f),
                        outerRadius = w * 0.36f,
                        innerRadius = w * 0.16f,
                        color = emeraldColor
                    )
                }
                OccasionIconKey.CALENDAR -> {
                    drawRoundRect(
                        brush = emeraldBrush,
                        topLeft = Offset(w * 0.20f, h * 0.24f),
                        size = Size(w * 0.60f, h * 0.56f),
                        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.20f, h * 0.40f),
                        end = Offset(w * 0.80f, h * 0.40f),
                        strokeWidth = w * 0.045f
                    )
                }
                OccasionIconKey.ROSE -> {
                    val center = Offset(w * 0.50f, h * 0.44f)
                    val petalRadius = w * 0.13f
                    val ringDist = w * 0.13f
                    for (i in 0 until 6) {
                        val angle = (i * 60.0) * PI / 180.0
                        val px = center.x + (ringDist * cos(angle)).toFloat()
                        val py = center.y + (ringDist * sin(angle)).toFloat()
                        drawCircle(
                            color = emeraldColor,
                            radius = petalRadius,
                            center = Offset(px, py)
                        )
                    }
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.09f,
                        center = center
                    )
                }
                OccasionIconKey.BELL -> {
                    val bellPath = Path().apply {
                        moveTo(w * 0.50f, h * 0.18f)
                        cubicTo(w * 0.30f, h * 0.18f, w * 0.26f, h * 0.38f, w * 0.26f, h * 0.58f)
                        lineTo(w * 0.18f, h * 0.68f)
                        lineTo(w * 0.82f, h * 0.68f)
                        lineTo(w * 0.74f, h * 0.58f)
                        cubicTo(w * 0.74f, h * 0.38f, w * 0.70f, h * 0.18f, w * 0.50f, h * 0.18f)
                        close()
                    }
                    drawPath(bellPath, brush = emeraldBrush)
                    drawCircle(
                        color = deepEmeraldColor,
                        radius = w * 0.065f,
                        center = Offset(w * 0.50f, h * 0.77f)
                    )
                }
            }
        }
    }
}

/**
 * Displays an attached image (from `imageBase64` or `imageUrl`) inside a Developer Alert/Broadcast banner
 * with rounded corners (`RoundedCornerShape(12.dp)`) and opens a full-screen zoomable dialog when tapped.
 */
@Composable
fun DeveloperAttachedImageBannerSection(
    imageBase64: String,
    imageUrl: String = "",
    modifier: Modifier = Modifier
) {
    val decodedBase64Bitmap = remember(imageBase64) {
        if (imageBase64.isNotBlank()) {
            runCatching {
                val clean = imageBase64.substringAfter("base64,", imageBase64)
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else null
    }

    var remoteBitmap by remember(imageUrl) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }

    LaunchedEffect(imageUrl, decodedBase64Bitmap) {
        if (decodedBase64Bitmap == null && imageUrl.isNotBlank() &&
            (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))
        ) {
            val fetched = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val conn = (java.net.URL(imageUrl).openConnection() as java.net.HttpURLConnection).apply {
                        connectTimeout = 7000
                        readTimeout = 7000
                        instanceFollowRedirects = true
                    }
                    val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                    conn.disconnect()
                    bmp
                }.getOrNull()
            }
            remoteBitmap = fetched
        }
    }

    val activeBitmap = decodedBase64Bitmap ?: remoteBitmap
    if (activeBitmap == null) return

    var showEnlargedDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { showEnlargedDialog = true }
            .testTag("developer_alert_attached_image")
    ) {
        Image(
            bitmap = activeBitmap,
            contentDescription = "صورة مرفقة مع تنبيه المطور",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
        ) {
            Text(
                text = "اضغط لتكبير الصورة",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF065F46),
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }

    if (showEnlargedDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEnlargedDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showEnlargedDialog = false }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معاينة الصورة المرفقة",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { showEnlargedDialog = false }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color(0xFF065F46),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Image(
                            bitmap = activeBitmap,
                            contentDescription = "معاينة مكبرة للصورة المرفقة",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bespoke Vector SVG Emblem for the 11 luxury icons or custom uploaded image.
 */
@Composable
fun OccasionVectorEmblem(
    themeType: OccasionThemeType,
    customIconBase64: String = "",
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
    iconKey: OccasionIconKey? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "occasion_emblem_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val decodedBitmap = remember(customIconBase64) {
        if (customIconBase64.isNotBlank()) {
            runCatching {
                val clean = customIconBase64.substringAfter("base64,", customIconBase64)
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else null
    }

    val resolvedIconKey = iconKey ?: when (themeType) {
        OccasionThemeType.OFFICIAL -> OccasionIconKey.CROWN
        OccasionThemeType.RAMADAN_EID -> OccasionIconKey.CRESCENT
        OccasionThemeType.NEW_YEAR -> OccasionIconKey.GRADUATION_CAP
        OccasionThemeType.ACADEMIC_EXCELLENCE -> OccasionIconKey.TROPHY
        OccasionThemeType.NATIONAL_DAY -> OccasionIconKey.FLAG
        OccasionThemeType.CUSTOM_MANUAL -> OccasionIconKey.CROWN
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (decodedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFD1FAE5))
                    .border(1.5.dp, Color(0xFF059669), CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = "شعار المناسبة المخصص",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        } else {
            AnimatedOccasionVectorCanvas(
                iconKey = resolvedIconKey,
                themeType = themeType,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Public Animated Bezier Vector Canvas for Occasions & Broadcasts (بدون الكأس نهائياً):
 * Draws a bespoke animated Bezier illustration for each occasion:
 * - رمضان (RAMADAN_EID / CRESCENT): هلال ذهبي وفانوس متأرجح بنعومة
 * - الأعياد (ROSE / STAR): زخارف احتفالية وأقمار مضيئة متلألئة
 * - رأس السنة (NEW_YEAR / CALENDAR / GRADUATION_CAP): شارة العام الجديد وشهب ذهبية متحركة
 * - المولد النبوي الشريف (CROWN / TROPHY / BOOK): القبة الخضراء النبوية والمنارة المضيئة (بدلاً من الكأس)
 * - العيد الوطني العراقي (NATIONAL_DAY / FLAG / SHIELD): العلم العراقي يرفرف بانسيابية
 */
@Composable
fun AnimatedOccasionVectorCanvas(
    modifier: Modifier = Modifier.size(72.dp),
    iconKey: OccasionIconKey = OccasionIconKey.CRESCENT,
    themeType: OccasionThemeType = OccasionThemeType.OFFICIAL,
    titleHint: String = ""
) {
    val infiniteTransition = rememberInfiniteTransition(label = "animated_occasion_vector_canvas")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "occasion_wave_phase"
    )
    val swingAngle by infiniteTransition.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "occasion_lantern_swing"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "occasion_glow_pulse"
    )

    // Determine which of the 5 bespoke occasion scenes to draw based on titleHint, themeType, or iconKey (never drawing a trophy)
    val sceneKind = remember(iconKey, themeType, titleHint) {
        val hint = titleHint.trim()
        when {
            hint.contains("رمضان") -> 1 // Ramadan Crescent & Swinging Lantern
            hint.contains("عيد الفطر") || hint.contains("عيد الأضحى") || hint.contains("الأعياد") || hint.contains("عيدكم") -> 2 // Eids Ornaments & Glowing Moons
            hint.contains("السنة") || hint.contains("عام") -> 3 // New Year Badge & Golden Meteors
            hint.contains("المولد") || hint.contains("النبوي") -> 4 // Mawlid Green Dome & Minaret
            hint.contains("الوطني") || hint.contains("العراق") -> 5 // Iraqi Flag Waving
            themeType == OccasionThemeType.NATIONAL_DAY || iconKey == OccasionIconKey.FLAG || iconKey == OccasionIconKey.SHIELD -> 5
            themeType == OccasionThemeType.NEW_YEAR || iconKey == OccasionIconKey.CALENDAR || iconKey == OccasionIconKey.GRADUATION_CAP -> 3
            iconKey == OccasionIconKey.ROSE || iconKey == OccasionIconKey.STAR -> 2
            iconKey == OccasionIconKey.CROWN || iconKey == OccasionIconKey.TROPHY || iconKey == OccasionIconKey.BOOK -> 4
            themeType == OccasionThemeType.RAMADAN_EID || iconKey == OccasionIconKey.CRESCENT -> 1
            else -> 4 // Default to Green Dome & Minaret / Illuminated Emblem (never trophy)
        }
    }

    Canvas(modifier = modifier.testTag("animated_occasion_vector_canvas")) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.50f
        val floatY = sin(phase) * (h * 0.028f)

        // Soft Emerald & Gold Medallion Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF34D399).copy(alpha = 0.32f * pulseGlow.coerceAtMost(1f)),
                    Color(0xFFFDE68A).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(cx, cy)
        )

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0D8A6A), Color(0xFF047857), Color(0xFF064E3B))
            ),
            radius = w * 0.42f,
            center = Offset(cx, cy)
        )
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFA7F3D0))
            ),
            radius = w * 0.42f,
            center = Offset(cx, cy),
            style = Stroke(width = (w * 0.032f).coerceAtLeast(1.5f))
        )

        when (sceneKind) {
            1 -> {
                // رمضان: هلال ذهبي وفانوس متأرجح بنعومة (Crescent Moon & Swinging Lantern)
                val crescentPath = Path().apply {
                    val mCx = w * 0.43f
                    val mCy = h * 0.50f + floatY * 0.5f
                    val rOuter = w * 0.25f
                    val rInner = w * 0.195f
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            mCx - rOuter,
                            mCy - rOuter,
                            mCx + rOuter,
                            mCy + rOuter
                        )
                    )
                    val cutout = Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                mCx - rInner + w * 0.095f,
                                mCy - rInner - h * 0.03f,
                                mCx + rInner + w * 0.095f,
                                mCy + rInner - h * 0.03f
                            )
                        )
                    }
                    op(this, cutout, androidx.compose.ui.graphics.PathOperation.Difference)
                }
                drawPath(
                    path = crescentPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFD97706))
                    )
                )

                // Swinging Ramadan Lantern (فانوس متأرجح)
                val pivotX = w * 0.64f
                val pivotY = h * 0.24f
                rotate(degrees = swingAngle, pivot = Offset(pivotX, pivotY)) {
                    drawLine(
                        color = Color(0xFFFEF08A),
                        start = Offset(pivotX, pivotY),
                        end = Offset(pivotX, h * 0.38f),
                        strokeWidth = (w * 0.025f).coerceAtLeast(1.2f),
                        cap = StrokeCap.Round
                    )
                    val domePath = Path().apply {
                        moveTo(pivotX - w * 0.08f, h * 0.42f)
                        quadraticBezierTo(pivotX, h * 0.34f, pivotX + w * 0.08f, h * 0.42f)
                        close()
                    }
                    drawPath(domePath, color = Color(0xFFF59E0B))

                    val bodyPath = Path().apply {
                        moveTo(pivotX - w * 0.08f, h * 0.42f)
                        lineTo(pivotX - w * 0.10f, h * 0.53f)
                        lineTo(pivotX - w * 0.06f, h * 0.64f)
                        lineTo(pivotX + w * 0.06f, h * 0.64f)
                        lineTo(pivotX + w * 0.10f, h * 0.53f)
                        lineTo(pivotX + w * 0.08f, h * 0.42f)
                        close()
                    }
                    drawPath(
                        path = bodyPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
                        )
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.90f),
                        radius = w * 0.042f * pulseGlow.coerceIn(0.85f, 1.15f),
                        center = Offset(pivotX, h * 0.52f)
                    )
                }
            }

            2 -> {
                // الأعياد: زخارف إسلامية وأقمار مضيئة متلألئة (Festive Ornaments & Glowing Moons)
                rotate(degrees = sin(phase) * 8f, pivot = Offset(cx, cy)) {
                    for (i in 0 until 8) {
                        val a = (i * 45.0) * PI / 180.0
                        val px = cx + cos(a).toFloat() * (w * 0.22f)
                        val py = cy + sin(a).toFloat() * (h * 0.22f)
                        drawCircle(
                            color = if (i % 2 == 0) Color(0xFFFEF08A) else Color(0xFF6EE7B7),
                            radius = w * 0.045f * pulseGlow.coerceIn(0.85f, 1.1f),
                            center = Offset(px, py)
                        )
                    }
                }
                // Central festive crescent & star ornament
                val eidMoon = Path().apply {
                    val rOut = w * 0.18f
                    val rIn = w * 0.14f
                    addOval(androidx.compose.ui.geometry.Rect(cx - rOut, cy - rOut, cx + rOut, cy + rOut))
                    val cut = Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                cx - rIn + w * 0.06f,
                                cy - rIn - h * 0.03f,
                                cx + rIn + w * 0.06f,
                                cy + rIn - h * 0.03f
                            )
                        )
                    }
                    op(this, cut, androidx.compose.ui.graphics.PathOperation.Difference)
                }
                drawPath(
                    path = eidMoon,
                    brush = Brush.linearGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B)))
                )
                drawStar(
                    center = Offset(cx + w * 0.08f, cy - h * 0.06f),
                    outerRadius = w * 0.085f * pulseGlow.coerceIn(0.9f, 1.12f),
                    innerRadius = w * 0.038f,
                    color = Color.White
                )
            }

            3 -> {
                // رأس السنة: شارة العام الجديد وشهب ذهبية متحركة (New Year Emblem & Golden Shooting Meteors)
                val badgePath = Path().apply {
                    moveTo(cx, h * 0.22f + floatY)
                    lineTo(w * 0.72f, h * 0.34f + floatY)
                    lineTo(w * 0.68f, h * 0.64f + floatY)
                    quadraticBezierTo(cx, h * 0.78f + floatY, w * 0.32f, h * 0.64f + floatY)
                    lineTo(w * 0.28f, h * 0.34f + floatY)
                    close()
                }
                drawPath(
                    path = badgePath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF047857), Color(0xFF0F3D5E))
                    )
                )
                drawPath(
                    path = badgePath,
                    color = Color(0xFFFEF08A),
                    style = Stroke(width = (w * 0.03f).coerceAtLeast(1.4f), join = StrokeJoin.Round)
                )
                drawStar(
                    center = Offset(cx, h * 0.48f + floatY),
                    outerRadius = w * 0.14f * pulseGlow.coerceIn(0.9f, 1.12f),
                    innerRadius = w * 0.06f,
                    color = Color(0xFFFEF08A)
                )

                // Golden shooting meteors (شهب ذهبية)
                val meteorShift = ((phase / (2f * PI).toFloat()) % 1f)
                for (m in 0..2) {
                    val prog = (meteorShift + m * 0.33f) % 1f
                    val startX = w * (0.76f - prog * 0.48f)
                    val startY = h * (0.22f + m * 0.16f + prog * 0.22f)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFEF08A), Color.Transparent),
                            start = Offset(startX, startY),
                            end = Offset(startX + w * 0.14f, startY - h * 0.09f)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(startX + w * 0.14f, startY - h * 0.09f),
                        strokeWidth = (w * 0.028f).coerceAtLeast(1.4f),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.024f,
                        center = Offset(startX, startY)
                    )
                }
            }

            4 -> {
                // المولد النبوي الشريف: القبة الخضراء والمنارة المضيئة (Green Dome & Minaret)
                val baseLineY = h * 0.72f + floatY * 0.4f

                // Minaret on the right (المنارة)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFFDF7), Color(0xFFD1FAE5))),
                    topLeft = Offset(w * 0.64f, h * 0.32f + floatY * 0.4f),
                    size = Size(w * 0.09f, h * 0.40f),
                    cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
                )
                // Minaret balcony & spire
                drawRoundRect(
                    color = Color(0xFFFEF08A),
                    topLeft = Offset(w * 0.625f, h * 0.40f + floatY * 0.4f),
                    size = Size(w * 0.12f, h * 0.032f),
                    cornerRadius = CornerRadius(w * 0.01f, w * 0.01f)
                )
                val spirePath = Path().apply {
                    moveTo(w * 0.64f, h * 0.32f + floatY * 0.4f)
                    lineTo(w * 0.685f, h * 0.21f + floatY * 0.4f)
                    lineTo(w * 0.73f, h * 0.32f + floatY * 0.4f)
                    close()
                }
                drawPath(spirePath, color = Color(0xFF10B981))

                // Green Dome (القبة الخضراء)
                val domeCenterX = w * 0.44f
                val domeBaseY = h * 0.56f + floatY * 0.4f
                val domePath = Path().apply {
                    moveTo(domeCenterX - w * 0.20f, domeBaseY)
                    cubicTo(
                        domeCenterX - w * 0.20f, h * 0.38f + floatY * 0.4f,
                        domeCenterX - w * 0.06f, h * 0.30f + floatY * 0.4f,
                        domeCenterX, h * 0.25f + floatY * 0.4f
                    )
                    cubicTo(
                        domeCenterX + w * 0.06f, h * 0.30f + floatY * 0.4f,
                        domeCenterX + w * 0.20f, h * 0.38f + floatY * 0.4f,
                        domeCenterX + w * 0.20f, domeBaseY
                    )
                    close()
                }
                drawPath(
                    path = domePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF065F46))
                    )
                )
                drawPath(
                    path = domePath,
                    color = Color(0xFFFEF08A),
                    style = Stroke(width = (w * 0.022f).coerceAtLeast(1.2f))
                )

                // Golden Crescent Finial atop Green Dome
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = w * 0.028f * pulseGlow.coerceIn(0.9f, 1.15f),
                    center = Offset(domeCenterX, h * 0.22f + floatY * 0.4f)
                )

                // Courtyard base wall with golden arches
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFFFDF7), Color(0xFFE2E8F0))),
                    topLeft = Offset(w * 0.24f, domeBaseY),
                    size = Size(w * 0.50f, baseLineY - domeBaseY),
                    cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
                )
                for (archIdx in 0..2) {
                    val ax = w * (0.30f + archIdx * 0.12f)
                    drawRoundRect(
                        color = Color(0xFF047857),
                        topLeft = Offset(ax, domeBaseY + h * 0.04f),
                        size = Size(w * 0.065f, baseLineY - domeBaseY - h * 0.04f),
                        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                    )
                }
            }

            else -> {
                // العيد الوطني: العلم العراقي يرفرف بانسيابية (Smoothly Waving Iraqi Flag)
                val poleX = w * 0.26f
                drawLine(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFD97706))),
                    start = Offset(poleX, h * 0.20f),
                    end = Offset(poleX, h * 0.80f),
                    strokeWidth = (w * 0.042f).coerceAtLeast(2f),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = w * 0.036f,
                    center = Offset(poleX, h * 0.19f)
                )

                val flagLeft = poleX + w * 0.02f
                val flagRight = w * 0.78f
                val flagW = flagRight - flagLeft
                val stripeH = h * 0.12f
                val wave1 = sin(phase) * (h * 0.038f)
                val wave2 = cos(phase) * (h * 0.038f)

                fun buildBandPath(topBaseY: Float, bottomBaseY: Float): Path = Path().apply {
                    moveTo(flagLeft, topBaseY)
                    cubicTo(
                        flagLeft + flagW * 0.35f, topBaseY + wave1,
                        flagLeft + flagW * 0.68f, topBaseY - wave2,
                        flagRight, topBaseY + wave1 * 0.7f
                    )
                    lineTo(flagRight, bottomBaseY + wave1 * 0.7f)
                    cubicTo(
                        flagLeft + flagW * 0.68f, bottomBaseY - wave2,
                        flagLeft + flagW * 0.35f, bottomBaseY + wave1,
                        flagLeft, bottomBaseY
                    )
                    close()
                }

                val topY = h * 0.27f
                // Top Red Band
                drawPath(buildBandPath(topY, topY + stripeH), color = Color(0xFFDC2626))
                // Middle White Band
                drawPath(buildBandPath(topY + stripeH, topY + stripeH * 2f), color = Color.White)
                // Bottom Black Band
                drawPath(buildBandPath(topY + stripeH * 2f, topY + stripeH * 3f), color = Color(0xFF0F172A))

                // Green "الله أكبر" Kufic Emblem strokes in center of White Band
                val midBandY = topY + stripeH * 1.5f + (wave1 - wave2) * 0.25f
                for (k in 0..2) {
                    val kx = flagLeft + flagW * (0.28f + k * 0.20f)
                    drawLine(
                        color = Color(0xFF059669),
                        start = Offset(kx - w * 0.04f, midBandY),
                        end = Offset(kx + w * 0.04f, midBandY),
                        strokeWidth = (w * 0.026f).coerceAtLeast(1.4f),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF059669),
                        start = Offset(kx, midBandY - stripeH * 0.28f),
                        end = Offset(kx, midBandY + stripeH * 0.18f),
                        strokeWidth = (w * 0.022f).coerceAtLeast(1.2f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCrownEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    val crownPath = Path().apply {
        moveTo(w * 0.27f, h * 0.64f)
        lineTo(w * 0.23f, h * 0.40f)
        lineTo(w * 0.38f, h * 0.51f)
        lineTo(w * 0.50f, h * 0.31f)
        lineTo(w * 0.62f, h * 0.51f)
        lineTo(w * 0.77f, h * 0.40f)
        lineTo(w * 0.73f, h * 0.64f)
        close()
    }
    drawPath(path = crownPath, brush = whiteBrush)
    drawRoundRect(
        brush = whiteBrush,
        topLeft = Offset(w * 0.27f, h * 0.66f),
        size = Size(w * 0.46f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawCircle(color = Color.White, radius = w * 0.032f, center = Offset(w * 0.50f, h * 0.28f))
    drawCircle(color = Color(0xFFD1FAE5), radius = w * 0.025f, center = Offset(w * 0.23f, h * 0.37f))
    drawCircle(color = Color(0xFFD1FAE5), radius = w * 0.025f, center = Offset(w * 0.77f, h * 0.37f))
}

private fun DrawScope.drawRamadanLanternEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    val crescentPath = Path().apply {
        val cx = w * 0.48f
        val cy = h * 0.50f
        val rOuter = w * 0.24f
        val rInner = w * 0.19f
        addOval(
            androidx.compose.ui.geometry.Rect(
                cx - rOuter,
                cy - rOuter,
                cx + rOuter,
                cy + rOuter
            )
        )
        val cutout = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    cx - rInner + w * 0.09f,
                    cy - rInner - h * 0.03f,
                    cx + rInner + w * 0.09f,
                    cy + rInner - h * 0.03f
                )
            )
        }
        op(this, cutout, androidx.compose.ui.graphics.PathOperation.Difference)
    }
    drawPath(path = crescentPath, brush = whiteBrush)
    drawStar(
        center = Offset(w * 0.63f, h * 0.38f),
        outerRadius = w * 0.085f,
        innerRadius = w * 0.038f,
        color = Color.White
    )
}

private fun DrawScope.drawGraduationCapEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    val capTop = Path().apply {
        moveTo(w * 0.50f, h * 0.30f)
        lineTo(w * 0.78f, h * 0.44f)
        lineTo(w * 0.50f, h * 0.58f)
        lineTo(w * 0.22f, h * 0.44f)
        close()
    }
    drawPath(capTop, brush = whiteBrush)
    val skullCap = Path().apply {
        moveTo(w * 0.34f, h * 0.52f)
        lineTo(w * 0.34f, h * 0.66f)
        quadraticTo(w * 0.50f, h * 0.74f, w * 0.66f, h * 0.66f)
        lineTo(w * 0.66f, h * 0.52f)
        lineTo(w * 0.50f, h * 0.60f)
        close()
    }
    drawPath(skullCap, color = Color(0xFFD1FAE5))
    drawLine(
        color = Color.White,
        start = Offset(w * 0.72f, h * 0.47f),
        end = Offset(w * 0.72f, h * 0.66f),
        strokeWidth = w * 0.03f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color.White,
        radius = w * 0.032f,
        center = Offset(w * 0.72f, h * 0.67f)
    )
}

private fun DrawScope.drawTrophyLaurelEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    val cupPath = Path().apply {
        moveTo(w * 0.34f, h * 0.30f)
        lineTo(w * 0.66f, h * 0.30f)
        lineTo(w * 0.61f, h * 0.52f)
        quadraticTo(w * 0.50f, h * 0.61f, w * 0.39f, h * 0.52f)
        close()
    }
    drawPath(cupPath, brush = whiteBrush)
    drawLine(
        color = Color.White,
        start = Offset(w * 0.50f, h * 0.56f),
        end = Offset(w * 0.50f, h * 0.68f),
        strokeWidth = w * 0.055f
    )
    drawRoundRect(
        brush = whiteBrush,
        topLeft = Offset(w * 0.36f, h * 0.67f),
        size = Size(w * 0.28f, h * 0.065f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    drawStar(
        center = Offset(w * 0.50f, h * 0.42f),
        outerRadius = w * 0.065f,
        innerRadius = w * 0.028f,
        color = Color(0xFF047857)
    )
}

private fun DrawScope.drawShieldEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    val shield = Path().apply {
        moveTo(w * 0.50f, h * 0.26f)
        lineTo(w * 0.72f, h * 0.34f)
        lineTo(w * 0.72f, h * 0.52f)
        quadraticTo(w * 0.70f, h * 0.68f, w * 0.50f, h * 0.76f)
        quadraticTo(w * 0.30f, h * 0.68f, w * 0.28f, h * 0.52f)
        lineTo(w * 0.28f, h * 0.34f)
        close()
    }
    drawPath(shield, brush = whiteBrush)
    drawStar(
        center = Offset(w * 0.50f, h * 0.50f),
        outerRadius = w * 0.11f,
        innerRadius = w * 0.045f,
        color = Color(0xFF047857)
    )
}

private fun DrawScope.drawNationalBannerEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    drawLine(
        brush = whiteBrush,
        start = Offset(w * 0.32f, h * 0.27f),
        end = Offset(w * 0.32f, h * 0.74f),
        strokeWidth = w * 0.045f,
        cap = StrokeCap.Round
    )
    val flagPath = Path().apply {
        moveTo(w * 0.34f, h * 0.30f)
        cubicTo(w * 0.46f, h * 0.25f, w * 0.58f, h * 0.36f, w * 0.72f, h * 0.31f)
        lineTo(w * 0.72f, h * 0.53f)
        cubicTo(w * 0.58f, h * 0.58f, w * 0.46f, h * 0.47f, w * 0.34f, h * 0.53f)
        close()
    }
    drawPath(flagPath, brush = whiteBrush)
    drawStar(
        center = Offset(w * 0.53f, h * 0.42f),
        outerRadius = w * 0.06f,
        innerRadius = w * 0.025f,
        color = Color(0xFF047857)
    )
}

private fun DrawScope.drawOpenBookEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    val leftPage = Path().apply {
        moveTo(w * 0.50f, h * 0.36f)
        quadraticTo(w * 0.37f, h * 0.31f, w * 0.25f, h * 0.36f)
        lineTo(w * 0.25f, h * 0.66f)
        quadraticTo(w * 0.37f, h * 0.61f, w * 0.50f, h * 0.66f)
        close()
    }
    val rightPage = Path().apply {
        moveTo(w * 0.50f, h * 0.36f)
        quadraticTo(w * 0.63f, h * 0.31f, w * 0.75f, h * 0.36f)
        lineTo(w * 0.75f, h * 0.66f)
        quadraticTo(w * 0.63f, h * 0.61f, w * 0.50f, h * 0.66f)
        close()
    }
    drawPath(leftPage, brush = whiteBrush)
    drawPath(rightPage, brush = whiteBrush)
    drawLine(
        color = Color(0xFF047857),
        start = Offset(w * 0.50f, h * 0.35f),
        end = Offset(w * 0.50f, h * 0.67f),
        strokeWidth = w * 0.03f
    )
}

private fun DrawScope.drawRoyalStarEmblem(w: Float, h: Float) {
    drawStar(
        center = Offset(w * 0.50f, h * 0.50f),
        outerRadius = w * 0.25f,
        innerRadius = w * 0.11f,
        color = Color.White
    )
    drawStar(
        center = Offset(w * 0.50f, h * 0.50f),
        outerRadius = w * 0.13f,
        innerRadius = w * 0.055f,
        color = Color(0xFFD1FAE5)
    )
}

private fun DrawScope.drawCalendarEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    drawRoundRect(
        brush = whiteBrush,
        topLeft = Offset(w * 0.28f, h * 0.31f),
        size = Size(w * 0.44f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )
    drawLine(
        color = Color(0xFF047857),
        start = Offset(w * 0.28f, h * 0.43f),
        end = Offset(w * 0.72f, h * 0.43f),
        strokeWidth = w * 0.03f
    )
    // Rings on top
    drawLine(
        color = Color.White,
        start = Offset(w * 0.38f, h * 0.26f),
        end = Offset(w * 0.38f, h * 0.35f),
        strokeWidth = w * 0.04f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White,
        start = Offset(w * 0.62f, h * 0.26f),
        end = Offset(w * 0.62f, h * 0.35f),
        strokeWidth = w * 0.04f,
        cap = StrokeCap.Round
    )
    // Grid dots
    val dotColor = Color(0xFF047857)
    for (row in 0..1) {
        for (col in 0..2) {
            drawCircle(
                color = dotColor,
                radius = w * 0.025f,
                center = Offset(w * (0.38f + col * 0.12f), h * (0.52f + row * 0.10f))
            )
        }
    }
}

private fun DrawScope.drawRoseEmblem(w: Float, h: Float) {
    val center = Offset(w * 0.50f, h * 0.46f)
    val petalRadius = w * 0.10f
    val ringDist = w * 0.10f
    for (i in 0 until 6) {
        val angle = (i * 60.0) * PI / 180.0
        val px = center.x + (ringDist * cos(angle)).toFloat()
        val py = center.y + (ringDist * sin(angle)).toFloat()
        drawCircle(
            color = Color.White,
            radius = petalRadius,
            center = Offset(px, py)
        )
    }
    drawCircle(
        color = Color(0xFFA7F3D0),
        radius = w * 0.08f,
        center = center
    )
    // Stem
    drawLine(
        color = Color(0xFFD1FAE5),
        start = Offset(w * 0.50f, h * 0.60f),
        end = Offset(w * 0.50f, h * 0.75f),
        strokeWidth = w * 0.035f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawBellEmblem(w: Float, h: Float) {
    val whiteBrush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFECFDF5))
    )
    val bellPath = Path().apply {
        moveTo(w * 0.50f, h * 0.27f)
        cubicTo(w * 0.36f, h * 0.27f, w * 0.33f, h * 0.42f, w * 0.33f, h * 0.55f)
        lineTo(w * 0.27f, h * 0.63f)
        lineTo(w * 0.73f, h * 0.63f)
        lineTo(w * 0.67f, h * 0.55f)
        cubicTo(w * 0.67f, h * 0.42f, w * 0.64f, h * 0.27f, w * 0.50f, h * 0.27f)
        close()
    }
    drawPath(bellPath, brush = whiteBrush)
    drawCircle(
        color = Color.White,
        radius = w * 0.048f,
        center = Offset(w * 0.50f, h * 0.69f)
    )
}

private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    points: Int = 5
) {
    val path = Path()
    val angleStep = PI / points
    var angle = -PI / 2
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += angleStep
    }
    path.close()
    drawPath(path, color = color, style = Fill)
}

fun occasionThemeBrush(primaryHex: String, gradientHex: String): Brush {
    val c1 = runCatching { Color(android.graphics.Color.parseColor(primaryHex)) }
        .getOrDefault(Color(0xFF047857))
    val c2 = runCatching { Color(android.graphics.Color.parseColor(gradientHex)) }
        .getOrDefault(Color(0xFF059669))
    return Brush.horizontalGradient(colors = listOf(c1, c2))
}

/**
 * Clean Bright Occasion / Developer Alert Banner shown on Login & Dashboard when `config.isActive == true`
 * (or when `forcePreview == true` inside the Developer Control Panel).
 * Displays the developer's chosen custom icon (`OccasionIconKey`) in pure Emerald (`0xFF059669`)
 * and renders any attached image (`customIconBase64` / `customIconUrl`) with rounded corners and click-to-enlarge.
 */
@Composable
fun LiveOccasionBannerCard(
    config: OccasionThemeConfig,
    modifier: Modifier = Modifier,
    forcePreview: Boolean = false
) {
    var isLocallyDismissed by remember(
        config.effectiveTitle,
        config.effectiveBannerText,
        config.selectedIconKey,
        config.customIconBase64,
        config.customIconUrl,
        config.updatedAt
    ) {
        mutableStateOf(false)
    }
    if ((!config.isActive || isLocallyDismissed) && !forcePreview) return

    val titleText = config.effectiveTitle.ifBlank { "تنبيه عام" }
    val bodyText = config.effectiveBannerText
    val resolvedIcon = OccasionIconKey.fromKey(config.selectedIconKey)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_occasion_banner_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DeveloperSystemBroadcastBanner(
            title = titleText,
            subtitle = bodyText,
            badgeText = resolvedIcon.arabicLabel,
            iconKey = resolvedIcon,
            illustrationType = resolvedIcon.toThreeDIllustrationType(),
            onDismiss = if (!forcePreview) {
                { isLocallyDismissed = true }
            } else {
                null
            }
        )

        if (config.customIconBase64.isNotBlank() || config.customIconUrl.isNotBlank()) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DeveloperAttachedImageBannerSection(
                    imageBase64 = config.customIconBase64,
                    imageUrl = config.customIconUrl
                )
            }
        }
    }
}
