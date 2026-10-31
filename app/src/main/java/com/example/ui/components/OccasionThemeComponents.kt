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
import androidx.compose.material3.Text
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

    val infiniteTransition = rememberInfiniteTransition(label = "urgent_alert_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFD97706).copy(alpha = pulseAlpha),
                        Color(0xFF059669).copy(alpha = pulseAlpha),
                        Color(0xFFD97706).copy(alpha = pulseAlpha)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("urgent_school_alert_banner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF064E3B),
                            Color(0xFF047857),
                            Color(0xFF065F46)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.22f))
                            .border(1.dp, Color(0xFFFCD34D), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "تنبيه عاجل للمدرسة",
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFF59E0B),
                                contentColor = Color(0xFF042F2E)
                            ) {
                                Text(
                                    text = "تنبيه عاجل للمدرسة",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (alert.updatedAt.isNotBlank()) {
                                Text(
                                    text = alert.updatedAt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }
                        if (alert.authorName.isNotBlank()) {
                            Text(
                                text = "صادر عن: ${alert.authorName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismissForUser,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .testTag("dismiss_urgent_alert_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق التنبيه بعد قراءته",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEditRequest,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_urgent_alert_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color(0xFFFDE68A)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تعديل التنبيه",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onDeleteAlert,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_delete_urgent_alert_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF7F1D1D).copy(alpha = 0.45f),
                            contentColor = Color(0xFFFECACA)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حذف / إيقاف التنبيه",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4
    ) {
        OccasionIconKey.entries.forEach { iconKey ->
            val isSelected = !hasCustomImage && selectedIconKey == iconKey
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectIcon(iconKey) }
                    .testTag("select_occasion_vector_icon_${iconKey.name}"),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) Color(0xFF064E3B) else Color.White,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xFFD1FAE5)
                ),
                shadowElevation = if (isSelected) 4.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.14f)
                                else Color(0xFF042F2E)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        OccasionVectorEmblem(
                            themeType = OccasionThemeType.OFFICIAL,
                            iconKey = iconKey,
                            customIconBase64 = "",
                            size = 34.dp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = iconKey.arabicLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) Color(0xFFFDE68A) else Color(0xFF064E3B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
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
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFDE68A).copy(alpha = 0.35f),
                                Color(0xFF064E3B).copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFF59E0B), CircleShape)
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

                // Outer golden aura ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDE68A).copy(alpha = 0.38f * glowPulse),
                            Color(0xFFD97706).copy(alpha = 0.14f * glowPulse),
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
                        colors = listOf(Color(0xFF065F46), Color(0xFF022C22))
                    ),
                    radius = radius * 0.82f,
                    center = center
                )

                // Luxury double gold border
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFFF59E0B))
                    ),
                    radius = radius * 0.82f,
                    center = center,
                    style = Stroke(width = radius * 0.065f)
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
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
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
    drawPath(path = crownPath, brush = goldBrush)
    drawRoundRect(
        brush = goldBrush,
        topLeft = Offset(w * 0.27f, h * 0.66f),
        size = Size(w * 0.46f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawCircle(color = Color(0xFFFFFBEB), radius = w * 0.032f, center = Offset(w * 0.50f, h * 0.28f))
    drawCircle(color = Color(0xFFFDE68A), radius = w * 0.025f, center = Offset(w * 0.23f, h * 0.37f))
    drawCircle(color = Color(0xFFFDE68A), radius = w * 0.025f, center = Offset(w * 0.77f, h * 0.37f))
}

private fun DrawScope.drawRamadanLanternEmblem(w: Float, h: Float) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
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
    drawPath(path = crescentPath, brush = goldBrush)
    drawStar(
        center = Offset(w * 0.63f, h * 0.38f),
        outerRadius = w * 0.085f,
        innerRadius = w * 0.038f,
        color = Color(0xFFFFFBEB)
    )
}

private fun DrawScope.drawGraduationCapEmblem(w: Float, h: Float) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
    )
    val capTop = Path().apply {
        moveTo(w * 0.50f, h * 0.30f)
        lineTo(w * 0.78f, h * 0.44f)
        lineTo(w * 0.50f, h * 0.58f)
        lineTo(w * 0.22f, h * 0.44f)
        close()
    }
    drawPath(capTop, brush = goldBrush)
    val skullCap = Path().apply {
        moveTo(w * 0.34f, h * 0.52f)
        lineTo(w * 0.34f, h * 0.66f)
        quadraticTo(w * 0.50f, h * 0.74f, w * 0.66f, h * 0.66f)
        lineTo(w * 0.66f, h * 0.52f)
        lineTo(w * 0.50f, h * 0.60f)
        close()
    }
    drawPath(skullCap, color = Color(0xFFF59E0B))
    drawLine(
        color = Color(0xFFFFFBEB),
        start = Offset(w * 0.72f, h * 0.47f),
        end = Offset(w * 0.72f, h * 0.66f),
        strokeWidth = w * 0.03f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color(0xFFFFFBEB),
        radius = w * 0.032f,
        center = Offset(w * 0.72f, h * 0.67f)
    )
}

private fun DrawScope.drawTrophyLaurelEmblem(w: Float, h: Float) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
    )
    val cupPath = Path().apply {
        moveTo(w * 0.34f, h * 0.30f)
        lineTo(w * 0.66f, h * 0.30f)
        lineTo(w * 0.61f, h * 0.52f)
        quadraticTo(w * 0.50f, h * 0.61f, w * 0.39f, h * 0.52f)
        close()
    }
    drawPath(cupPath, brush = goldBrush)
    drawLine(
        color = Color(0xFFFBBF24),
        start = Offset(w * 0.50f, h * 0.56f),
        end = Offset(w * 0.50f, h * 0.68f),
        strokeWidth = w * 0.055f
    )
    drawRoundRect(
        brush = goldBrush,
        topLeft = Offset(w * 0.36f, h * 0.67f),
        size = Size(w * 0.28f, h * 0.065f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
    drawStar(
        center = Offset(w * 0.50f, h * 0.42f),
        outerRadius = w * 0.065f,
        innerRadius = w * 0.028f,
        color = Color(0xFF042F2E)
    )
}

private fun DrawScope.drawShieldEmblem(w: Float, h: Float) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
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
    drawPath(shield, brush = goldBrush)
    drawStar(
        center = Offset(w * 0.50f, h * 0.50f),
        outerRadius = w * 0.11f,
        innerRadius = w * 0.045f,
        color = Color(0xFF042F2E)
    )
}

private fun DrawScope.drawNationalBannerEmblem(w: Float, h: Float) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
    )
    drawLine(
        brush = goldBrush,
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
    drawPath(flagPath, brush = goldBrush)
    drawStar(
        center = Offset(w * 0.53f, h * 0.42f),
        outerRadius = w * 0.06f,
        innerRadius = w * 0.025f,
        color = Color(0xFF042F2E)
    )
}

private fun DrawScope.drawOpenBookEmblem(w: Float, h: Float) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
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
    drawPath(leftPage, brush = goldBrush)
    drawPath(rightPage, brush = goldBrush)
    drawLine(
        color = Color(0xFF042F2E),
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
        color = Color(0xFFFBBF24)
    )
    drawStar(
        center = Offset(w * 0.50f, h * 0.50f),
        outerRadius = w * 0.13f,
        innerRadius = w * 0.055f,
        color = Color(0xFFFFFBEB)
    )
}

private fun DrawScope.drawCalendarEmblem(w: Float, h: Float) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
    )
    drawRoundRect(
        brush = goldBrush,
        topLeft = Offset(w * 0.28f, h * 0.31f),
        size = Size(w * 0.44f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )
    drawLine(
        color = Color(0xFF042F2E),
        start = Offset(w * 0.28f, h * 0.43f),
        end = Offset(w * 0.72f, h * 0.43f),
        strokeWidth = w * 0.03f
    )
    // Rings on top
    drawLine(
        color = Color(0xFFFFFBEB),
        start = Offset(w * 0.38f, h * 0.26f),
        end = Offset(w * 0.38f, h * 0.35f),
        strokeWidth = w * 0.04f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFFBEB),
        start = Offset(w * 0.62f, h * 0.26f),
        end = Offset(w * 0.62f, h * 0.35f),
        strokeWidth = w * 0.04f,
        cap = StrokeCap.Round
    )
    // Grid dots
    val dotColor = Color(0xFF042F2E)
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
            color = Color(0xFFFBBF24),
            radius = petalRadius,
            center = Offset(px, py)
        )
    }
    drawCircle(
        color = Color(0xFFFFFBEB),
        radius = w * 0.08f,
        center = center
    )
    // Stem
    drawLine(
        color = Color(0xFF34D399),
        start = Offset(w * 0.50f, h * 0.60f),
        end = Offset(w * 0.50f, h * 0.75f),
        strokeWidth = w * 0.035f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawBellEmblem(w: Float, h: Float) {
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
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
    drawPath(bellPath, brush = goldBrush)
    drawCircle(
        color = Color(0xFFFFFBEB),
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
        .getOrDefault(Color(0xFF064E3B))
    val c2 = runCatching { Color(android.graphics.Color.parseColor(gradientHex)) }
        .getOrDefault(Color(0xFF059669))
    return Brush.horizontalGradient(colors = listOf(c1, c2))
}

/**
 * Compact Luxury Occasion Banner shown on User Screens ONLY when `config.isActive == true`
 * (or when `forcePreview == true` inside the Developer Control Panel).
 */
@Composable
fun LiveOccasionBannerCard(
    config: OccasionThemeConfig,
    modifier: Modifier = Modifier,
    forcePreview: Boolean = false
) {
    // Hidden by default unless explicitly activated by the developer (or previewed in Dev Panel)
    if (!config.isActive && !forcePreview) return

    val startColor = remember(config.effectivePrimaryHex) {
        runCatching { Color(android.graphics.Color.parseColor(config.effectivePrimaryHex)) }
            .getOrDefault(Color(0xFF042F2E))
    }
    val endColor = remember(config.effectiveGradientHex) {
        runCatching { Color(android.graphics.Color.parseColor(config.effectiveGradientHex)) }
            .getOrDefault(Color(0xFF047857))
    }
    val accentColor = Color(0xFFF59E0B)
    val selectedIconKey = remember(config.selectedIconKey) {
        OccasionIconKey.fromKey(config.selectedIconKey)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.7f),
                        Color(0xFFFDE68A),
                        accentColor.copy(alpha = 0.7f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("live_occasion_banner_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(startColor, endColor)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OccasionVectorEmblem(
                    themeType = config.themeType,
                    iconKey = selectedIconKey,
                    customIconBase64 = config.customIconBase64,
                    size = 56.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = config.effectiveTitle,
                                color = Color(0xFFFDE68A),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = config.effectiveBannerText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.90f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
