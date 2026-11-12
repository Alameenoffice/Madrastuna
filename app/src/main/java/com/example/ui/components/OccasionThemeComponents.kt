package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
                    ThreeDIllustrationBox(
                        type = iconKey.toThreeDIllustrationType(),
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
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val center = Offset(w / 2f, h / 2f)
                val radius = (w.coerceAtMost(h) / 2f)

                // Soft emerald aura ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFA7F3D0).copy(alpha = 0.40f * glowPulse),
                            Color(0xFF10B981).copy(alpha = 0.14f * glowPulse),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Medallion background
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF047857), Color(0xFF059669))
                    ),
                    radius = radius * 0.82f,
                    center = center
                )

                // Clean emerald/white border
                drawCircle(
                    color = Color(0xFFA7F3D0),
                    radius = radius * 0.82f,
                    center = center,
                    style = Stroke(width = radius * 0.055f)
                )

                when (resolvedIconKey) {
                    OccasionIconKey.CROWN -> drawCrownEmblem(w, h)
                    OccasionIconKey.CRESCENT -> drawRamadanLanternEmblem(w, h)
                    OccasionIconKey.GRADUATION_CAP -> drawGraduationCapEmblem(w, h)
                    OccasionIconKey.TROPHY -> drawTrophyLaurelEmblem(w, h)
                    OccasionIconKey.SHIELD -> drawShieldEmblem(w, h)
                    OccasionIconKey.FLAG -> drawNationalBannerEmblem(w, h)
                    OccasionIconKey.BOOK -> drawOpenBookEmblem(w, h)
                    OccasionIconKey.STAR -> drawRoyalStarEmblem(w, h)
                    OccasionIconKey.CALENDAR -> drawCalendarEmblem(w, h)
                    OccasionIconKey.ROSE -> drawRoseEmblem(w, h)
                    OccasionIconKey.BELL -> drawBellEmblem(w, h)
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
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFEF3C7), Color(0xFFFBBF24), Color(0xFFD97706))
    )
    // Academic Laurel Wreath & Royal Star Crest (No Trophy Cup)
    val leftBranch = Path().apply {
        moveTo(w * 0.50f, h * 0.74f)
        cubicTo(w * 0.30f, h * 0.68f, w * 0.22f, h * 0.46f, w * 0.34f, h * 0.28f)
    }
    val rightBranch = Path().apply {
        moveTo(w * 0.50f, h * 0.74f)
        cubicTo(w * 0.70f, h * 0.68f, w * 0.78f, h * 0.46f, w * 0.66f, h * 0.28f)
    }
    drawPath(leftBranch, brush = goldBrush, style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
    drawPath(rightBranch, brush = goldBrush, style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
    drawStar(
        center = Offset(w * 0.50f, h * 0.48f),
        outerRadius = w * 0.16f,
        innerRadius = w * 0.07f,
        color = Color(0xFFFEF3C7),
        points = 8
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
 * Resolves the canonical `seasonalThemeId` ("RAMADAN", "EID", "NEW_YEAR", "MAWLID", "NATIONAL_DAY")
 * from `OccasionThemeConfig` so each occasion renders its bespoke Bézier vector scene.
 */
fun resolveSeasonalThemeId(config: OccasionThemeConfig): String {
    val combined = "${config.customTitle} ${config.customBannerText} ${config.launcherAliasName} ${config.themeType.name} ${config.selectedIconKey}".uppercase()
    return when {
        combined.contains("MAWLID") || combined.contains("مولد") || combined.contains("نبوي") || combined.contains("الرسول") -> "MAWLID"
        combined.contains("NATIONAL") || combined.contains("وطني") || combined.contains("عراق") || config.themeType == OccasionThemeType.NATIONAL_DAY || config.selectedIconKey.equals("FLAG", ignoreCase = true) -> "NATIONAL_DAY"
        combined.contains("EID") || combined.contains("عيد") || combined.contains("فطر") || combined.contains("أضحى") || combined.contains("اضحى") -> "EID"
        combined.contains("RAMADAN") || combined.contains("رمضان") || config.themeType == OccasionThemeType.RAMADAN_EID || config.selectedIconKey.equals("CRESCENT", ignoreCase = true) -> "RAMADAN"
        combined.contains("NEW_YEAR") || combined.contains("سنة") || combined.contains("عام") || config.themeType == OccasionThemeType.NEW_YEAR -> "NEW_YEAR"
        config.selectedIconKey.equals("ROSE", ignoreCase = true) -> "MAWLID"
        config.selectedIconKey.equals("STAR", ignoreCase = true) -> "EID"
        else -> "NEW_YEAR"
    }
}

/**
 * Pro Bézier Curve Vector Animation (`SeasonalBezierVectorAnimation`)
 * Built exclusively with `Path.cubicTo` & `quadraticBezierTo`, 3-stop `LinearGradient` & `RadialGradient`,
 * and smooth continuous physics (`infiniteRepeatable` with `sin` and `cos`) for every seasonal occasion:
 *   1. `RAMADAN`: 3D golden crescent + ornate heritage lantern swinging with pendulum angle + pulsing glow + 8-point stars.
 *   2. `EID`: Slowly rotating Islamic geometric patterns + swaying Eid lanterns/crescents + rising golden light particles.
 *   3. `NEW_YEAR`: New Year emblem with glowing orbits + golden & emerald shooting stars drawing curved Bézier trails.
 *   4. `MAWLID`: Architectural Bézier vector of the Prophet's Green Dome (القبة الخضراء النبوية الشريفة) & ornate minaret with expanding نورانية aura.
 *   5. `NATIONAL_DAY`: Authentic Iraqi flag fluttering with real sine-wave Bézier path (Red, White, Black + green "الله أكبر") inside a golden laurel wreath.
 */
@Composable
fun SeasonalBezierVectorAnimation(
    seasonalThemeId: String,
    modifier: Modifier = Modifier.size(96.dp)
) {
    val infinite = rememberInfiniteTransition(label = "seasonal_bezier_anim")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val swing by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swing"
    )

    val cleanId = seasonalThemeId.trim().uppercase()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val minDim = w.coerceAtMost(h)

        val gold3Stop = Brush.linearGradient(
            colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        val emerald3Stop = Brush.linearGradient(
            colors = listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // Ambient 3-stop radial aura behind all scenes
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFDE68A).copy(alpha = 0.34f * pulse.coerceIn(0.7f, 1.1f)),
                    Color(0xFF10B981).copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = minDim * 0.52f * pulse
            ),
            radius = minDim * 0.52f * pulse,
            center = Offset(cx, cy)
        )

        when {
            cleanId.contains("RAMADAN") -> {
                // 1. RAMADAN: 3D Golden Crescent + Swinging Heritage Lantern (Pendulum) + 8-point Twinkling Stars
                val crescentPath = Path().apply {
                    moveTo(w * 0.28f, h * 0.20f)
                    cubicTo(w * 0.05f, h * 0.36f, w * 0.08f, h * 0.76f, w * 0.42f, h * 0.88f)
                    cubicTo(w * 0.68f, h * 0.94f, w * 0.88f, h * 0.78f, w * 0.86f, h * 0.58f)
                    cubicTo(w * 0.74f, h * 0.74f, w * 0.46f, h * 0.78f, w * 0.30f, h * 0.62f)
                    cubicTo(w * 0.16f, h * 0.48f, w * 0.18f, h * 0.30f, w * 0.28f, h * 0.20f)
                    close()
                }
                drawPath(path = crescentPath, brush = gold3Stop)

                // Pendulum swinging ornate lantern suspended from top right curve
                val pivotX = w * 0.56f
                val pivotY = h * 0.14f
                val pendulumAngle = swing * 0.22f
                val chainLen = h * 0.20f
                val lanternTopX = pivotX + sin(pendulumAngle) * chainLen
                val lanternTopY = pivotY + cos(pendulumAngle) * chainLen

                drawLine(
                    brush = gold3Stop,
                    start = Offset(pivotX, pivotY),
                    end = Offset(lanternTopX, lanternTopY),
                    strokeWidth = minDim * 0.025f,
                    cap = StrokeCap.Round
                )

                // Warm inner glow expanding from lantern heart
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF08A).copy(alpha = 0.85f),
                            Color(0xFFF59E0B).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(lanternTopX, lanternTopY + h * 0.16f),
                        radius = minDim * 0.22f * pulse
                    ),
                    radius = minDim * 0.22f * pulse,
                    center = Offset(lanternTopX, lanternTopY + h * 0.16f)
                )

                // Lantern dome & body with cubicTo
                val lanternDome = Path().apply {
                    moveTo(lanternTopX - w * 0.09f, lanternTopY + h * 0.07f)
                    quadraticBezierTo(lanternTopX, lanternTopY - h * 0.02f, lanternTopX + w * 0.09f, lanternTopY + h * 0.07f)
                    close()
                }
                drawPath(lanternDome, brush = gold3Stop)

                val lanternBody = Path().apply {
                    moveTo(lanternTopX - w * 0.10f, lanternTopY + h * 0.07f)
                    cubicTo(
                        lanternTopX - w * 0.14f, lanternTopY + h * 0.16f,
                        lanternTopX - w * 0.11f, lanternTopY + h * 0.25f,
                        lanternTopX - w * 0.06f, lanternTopY + h * 0.29f
                    )
                    lineTo(lanternTopX + w * 0.06f, lanternTopY + h * 0.29f)
                    cubicTo(
                        lanternTopX + w * 0.11f, lanternTopY + h * 0.25f,
                        lanternTopX + w * 0.14f, lanternTopY + h * 0.16f,
                        lanternTopX + w * 0.10f, lanternTopY + h * 0.07f
                    )
                    close()
                }
                drawPath(lanternBody, brush = emerald3Stop)
                drawPath(lanternBody, brush = gold3Stop, style = Stroke(width = minDim * 0.024f))

                // Glowing core flame inside lantern
                val flamePath = Path().apply {
                    val fx = lanternTopX
                    val fy = lanternTopY + h * 0.19f
                    moveTo(fx, fy - h * 0.06f)
                    quadraticBezierTo(fx + w * 0.04f, fy, fx, fy + h * 0.04f)
                    quadraticBezierTo(fx - w * 0.04f, fy, fx, fy - h * 0.06f)
                    close()
                }
                drawPath(flamePath, color = Color(0xFFFEF9C3))

                // Twinkling 8-point stars
                val starScale1 = (0.7f + 0.3f * sin(phase)).coerceIn(0.4f, 1.1f)
                val starScale2 = (0.7f + 0.3f * cos(phase)).coerceIn(0.4f, 1.1f)
                drawStar(
                    center = Offset(w * 0.80f, h * 0.24f),
                    outerRadius = minDim * 0.075f * starScale1,
                    innerRadius = minDim * 0.030f * starScale1,
                    color = Color(0xFFFEF08A),
                    points = 8
                )
                drawStar(
                    center = Offset(w * 0.24f, h * 0.44f),
                    outerRadius = minDim * 0.055f * starScale2,
                    innerRadius = minDim * 0.022f * starScale2,
                    color = Color(0xFFFDE68A),
                    points = 8
                )
            }

            cleanId.contains("EID") -> {
                // 2. EID: Rotating Islamic 8-fold geometric rosette + swaying festive lanterns & rising golden light particles
                val numPetals = 8
                val geoPath = Path()
                for (i in 0 until numPetals) {
                    val a1 = phase * 0.35f + (i * 2.0 * PI / numPetals).toFloat()
                    val a2 = a1 + (PI / numPetals).toFloat()
                    val rOuter = minDim * 0.40f
                    val rInner = minDim * 0.22f
                    val x1 = cx + cos(a1) * rInner
                    val y1 = cy + sin(a1) * rInner
                    val xTip = cx + cos((a1 + a2) * 0.5f) * rOuter
                    val yTip = cy + sin((a1 + a2) * 0.5f) * rOuter
                    val x2 = cx + cos(a2) * rInner
                    val y2 = cy + sin(a2) * rInner
                    if (i == 0) geoPath.moveTo(x1, y1) else geoPath.lineTo(x1, y1)
                    geoPath.quadraticBezierTo(xTip, yTip, x2, y2)
                }
                geoPath.close()
                drawPath(geoPath, brush = emerald3Stop)
                drawPath(geoPath, brush = gold3Stop, style = Stroke(width = minDim * 0.025f))

                // Swaying festive Eid crescent & dual lanterns
                val swayX = sin(phase) * w * 0.03f
                val eidCrescent = Path().apply {
                    moveTo(cx - w * 0.14f + swayX, cy - h * 0.04f)
                    cubicTo(cx - w * 0.18f + swayX, cy + h * 0.14f, cx + w * 0.04f + swayX, cy + h * 0.22f, cx + w * 0.18f + swayX, cy + h * 0.06f)
                    cubicTo(cx + w * 0.06f + swayX, cy + h * 0.14f, cx - w * 0.08f + swayX, cy + h * 0.08f, cx - w * 0.14f + swayX, cy - h * 0.04f)
                    close()
                }
                drawPath(eidCrescent, brush = gold3Stop)

                // Rising golden light particles
                for (p in 0..4) {
                    val progressP = ((phase / (2f * PI.toFloat())) + p * 0.2f) % 1f
                    val px = w * (0.22f + p * 0.14f) + sin(phase + p) * w * 0.03f
                    val py = h * (0.82f - progressP * 0.64f)
                    val pRadius = minDim * (0.022f + (1f - progressP) * 0.018f)
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = (1f - progressP).coerceIn(0.2f, 0.95f)),
                        radius = pRadius,
                        center = Offset(px, py)
                    )
                }
                drawStar(
                    center = Offset(cx + swayX, cy - h * 0.10f),
                    outerRadius = minDim * 0.11f * pulse.coerceIn(0.85f, 1.1f),
                    innerRadius = minDim * 0.045f,
                    color = Color(0xFFFEF9C3),
                    points = 8
                )
            }

            cleanId.contains("MAWLID") -> {
                // 4. MAWLID: Architectural Bézier vector of the Prophet's Green Dome (القبة الخضراء النبوية الشريفة) & Minaret + expanding نورانية aura
                for (ring in 1..2) {
                    val ringRadius = minDim * (0.28f + ring * 0.10f) * pulse.coerceIn(0.88f, 1.12f)
                    drawCircle(
                        color = Color(0xFF6EE7B7).copy(alpha = 0.28f / ring),
                        radius = ringRadius,
                        center = Offset(cx, h * 0.54f),
                        style = Stroke(width = minDim * 0.02f)
                    )
                }

                // Ornate Minaret on the right
                val minaretPath = Path().apply {
                    moveTo(w * 0.72f, h * 0.78f)
                    lineTo(w * 0.73f, h * 0.34f)
                    quadraticBezierTo(w * 0.76f, h * 0.22f, w * 0.79f, h * 0.34f)
                    lineTo(w * 0.80f, h * 0.78f)
                    close()
                }
                drawPath(minaretPath, brush = gold3Stop)

                // Prophet's Green Dome (القبة الخضراء) with authentic pointed ogee Bézier arch
                val domePath = Path().apply {
                    moveTo(w * 0.22f, h * 0.74f)
                    cubicTo(w * 0.22f, h * 0.52f, w * 0.36f, h * 0.36f, w * 0.48f, h * 0.24f)
                    cubicTo(w * 0.60f, h * 0.36f, w * 0.74f, h * 0.52f, w * 0.74f, h * 0.74f)
                    close()
                }
                drawPath(domePath, brush = emerald3Stop)
                drawPath(domePath, brush = gold3Stop, style = Stroke(width = minDim * 0.026f))

                // Golden crescent finial atop the Green Dome
                drawCircle(
                    brush = gold3Stop,
                    radius = minDim * 0.042f,
                    center = Offset(w * 0.48f, h * 0.19f)
                )

                // Base arcade wall
                val baseWall = Path().apply {
                    moveTo(w * 0.16f, h * 0.74f)
                    quadraticBezierTo(w * 0.50f, h * 0.71f, w * 0.84f, h * 0.74f)
                    lineTo(w * 0.84f, h * 0.84f)
                    lineTo(w * 0.16f, h * 0.84f)
                    close()
                }
                drawPath(baseWall, brush = gold3Stop)
            }

            cleanId.contains("NATIONAL") -> {
                // 5. NATIONAL_DAY: Authentic Iraqi Flag fluttering with real Sine-Wave Bézier path + Golden Laurel Wreath
                // Golden Laurel Wreath Curves
                val leftWreath = Path().apply {
                    moveTo(cx, h * 0.88f)
                    cubicTo(w * 0.14f, h * 0.80f, w * 0.08f, h * 0.34f, w * 0.28f, h * 0.16f)
                }
                val rightWreath = Path().apply {
                    moveTo(cx, h * 0.88f)
                    cubicTo(w * 0.86f, h * 0.80f, w * 0.92f, h * 0.34f, w * 0.72f, h * 0.16f)
                }
                drawPath(leftWreath, brush = gold3Stop, style = Stroke(width = minDim * 0.035f, cap = StrokeCap.Round))
                drawPath(rightWreath, brush = gold3Stop, style = Stroke(width = minDim * 0.035f, cap = StrokeCap.Round))

                // Golden flagpole
                drawLine(
                    brush = gold3Stop,
                    start = Offset(w * 0.24f, h * 0.22f),
                    end = Offset(w * 0.24f, h * 0.82f),
                    strokeWidth = minDim * 0.036f,
                    cap = StrokeCap.Round
                )

                val wave1 = sin(phase) * h * 0.045f
                val wave2 = cos(phase) * h * 0.045f
                val flagLeft = w * 0.26f
                val flagRight = w * 0.78f
                val stripeH = h * 0.12f

                fun buildWaveStripe(topY: Float, botY: Float): Path = Path().apply {
                    moveTo(flagLeft, topY)
                    cubicTo(
                        flagLeft + (flagRight - flagLeft) * 0.35f, topY + wave1,
                        flagLeft + (flagRight - flagLeft) * 0.70f, topY - wave2,
                        flagRight, topY + wave1 * 0.6f
                    )
                    lineTo(flagRight, botY + wave1 * 0.6f)
                    cubicTo(
                        flagLeft + (flagRight - flagLeft) * 0.70f, botY - wave2,
                        flagLeft + (flagRight - flagLeft) * 0.35f, botY + wave1,
                        flagLeft, botY
                    )
                    close()
                }

                // Top Red Stripe
                drawPath(buildWaveStripe(h * 0.28f, h * 0.28f + stripeH), color = Color(0xFFDC2626))
                // Middle White Stripe
                drawPath(buildWaveStripe(h * 0.28f + stripeH, h * 0.28f + stripeH * 2f), color = Color(0xFFFFFFFF))
                // Bottom Black Stripe
                drawPath(buildWaveStripe(h * 0.28f + stripeH * 2f, h * 0.28f + stripeH * 3f), color = Color(0xFF111827))

                // Kufic Green "الله أكبر" calligraphic vector strokes in the center of the white stripe
                val midY = h * 0.28f + stripeH * 1.5f + wave1 * 0.25f
                val kuficPath = Path().apply {
                    moveTo(w * 0.36f, midY + h * 0.02f)
                    lineTo(w * 0.48f, midY + h * 0.02f)
                    moveTo(w * 0.45f, midY - h * 0.03f)
                    lineTo(w * 0.45f, midY + h * 0.02f)
                    moveTo(w * 0.54f, midY + h * 0.02f)
                    lineTo(w * 0.68f, midY + h * 0.02f)
                    moveTo(w * 0.64f, midY - h * 0.03f)
                    lineTo(w * 0.64f, midY + h * 0.02f)
                }
                drawPath(
                    kuficPath,
                    color = Color(0xFF059669),
                    style = Stroke(width = minDim * 0.028f, cap = StrokeCap.Round)
                )
            }

            else -> {
                // 3. NEW_YEAR: New Year Crest surrounded by light orbits & shooting stars drawing Animated Bézier Trails
                val orbitPath = Path().apply {
                    moveTo(w * 0.14f, h * 0.62f)
                    cubicTo(w * 0.24f, h * 0.22f, w * 0.76f, h * 0.22f, w * 0.86f, h * 0.62f)
                    cubicTo(w * 0.76f, h * 0.84f, w * 0.24f, h * 0.84f, w * 0.14f, h * 0.62f)
                    close()
                }
                drawPath(
                    orbitPath,
                    brush = gold3Stop,
                    style = Stroke(width = minDim * 0.024f)
                )

                // Animated Bézier shooting star trail
                val meteorT = (sin(phase) + 1f) * 0.5f
                val trailPath = Path().apply {
                    moveTo(w * 0.18f, h * 0.74f)
                    cubicTo(
                        w * 0.34f, h * 0.30f,
                        w * 0.62f, h * 0.24f,
                        w * (0.45f + meteorT * 0.38f), h * (0.48f - meteorT * 0.22f)
                    )
                }
                drawPath(
                    trailPath,
                    brush = gold3Stop,
                    style = Stroke(width = minDim * 0.036f, cap = StrokeCap.Round)
                )

                // Central Emerald & Gold Academic Shield/Star Burst
                val crestPath = Path().apply {
                    moveTo(cx, h * 0.22f)
                    cubicTo(w * 0.68f, h * 0.26f, w * 0.72f, h * 0.54f, cx, h * 0.76f)
                    cubicTo(w * 0.28f, h * 0.54f, w * 0.32f, h * 0.26f, cx, h * 0.22f)
                    close()
                }
                drawPath(crestPath, brush = emerald3Stop)
                drawPath(crestPath, brush = gold3Stop, style = Stroke(width = minDim * 0.028f))

                drawStar(
                    center = Offset(cx, h * 0.47f),
                    outerRadius = minDim * 0.14f * pulse.coerceIn(0.88f, 1.12f),
                    innerRadius = minDim * 0.055f,
                    color = Color(0xFFFEF08A),
                    points = 8
                )
                drawStar(
                    center = Offset(w * (0.45f + meteorT * 0.38f), h * (0.48f - meteorT * 0.22f)),
                    outerRadius = minDim * 0.085f,
                    innerRadius = minDim * 0.035f,
                    color = Color(0xFFFEF9C3),
                    points = 8
                )
            }
        }
    }
}

/**
 * Clean Bright Occasion / Seasonal Banner shown on Login & Dashboard when `config.isActive == true`
 * (or when `forcePreview == true`).
 * Uses `SeasonalBezierVectorAnimation` exclusively (strictly NO trophy cup!).
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

    val titleText = config.effectiveTitle.ifBlank { "مناسبة وتهنئة عامة" }
    val bodyText = config.effectiveBannerText
    val resolvedIcon = OccasionIconKey.fromKey(config.selectedIconKey)
    val seasonalThemeId = resolveSeasonalThemeId(config)
    val formattedDate = com.example.model.ArabicTextNormalizer.formatSystemBroadcastDateTime(config.updatedAt)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_occasion_banner_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF064E3B),
                                Color(0xFF047857),
                                Color(0xFF059669)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f),
                                border = BorderStroke(0.8.dp, Color(0xFFA7F3D0).copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = resolvedIcon.arabicLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFEF08A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            if (formattedDate.isNotBlank()) {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD1FAE5).copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )

                        if (bodyText.isNotBlank()) {
                            Text(
                                text = bodyText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFECFDF5),
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Box(contentAlignment = Alignment.TopEnd) {
                        SeasonalBezierVectorAnimation(
                            seasonalThemeId = seasonalThemeId,
                            modifier = Modifier.size(88.dp)
                        )
                        if (!forcePreview) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.22f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { isLocallyDismissed = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (config.customIconBase64.isNotBlank() || config.customIconUrl.isNotBlank()) {
            Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                DeveloperAttachedImageBannerSection(
                    imageBase64 = config.customIconBase64,
                    imageUrl = config.customIconUrl
                )
            }
        }
    }
}

