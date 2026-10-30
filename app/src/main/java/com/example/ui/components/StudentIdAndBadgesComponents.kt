package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.DeveloperBroadcastItem
import com.example.model.SchoolConfig
import com.example.model.StudentAutoBadge
import com.example.model.StudentRecord
import com.example.ui.theme.CardSurfaceWhite
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldFoil
import com.example.ui.theme.RoyalGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMutedGreen
import com.example.ui.theme.WarmGoldLight
import com.example.ui.theme.WarningAmber
import com.example.util.DocumentAndIdExporter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentAutoBadgesRow(
    badges: List<StudentAutoBadge>,
    modifier: Modifier = Modifier
) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        badges.forEach { badge ->
            val bgColor = if (badge.isGoldAttendance) {
                Color(0xFFECFDF5)
            } else {
                DeepEmerald.copy(alpha = 0.08f)
            }
            val borderColor = if (badge.isGoldAttendance) {
                EmeraldAccent.copy(alpha = 0.45f)
            } else {
                DeepEmerald.copy(alpha = 0.28f)
            }
            val iconCircleBg = if (badge.isGoldAttendance) {
                DeepEmerald.copy(alpha = 0.14f)
            } else {
                EmeraldAccent.copy(alpha = 0.18f)
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(iconCircleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (badge.isGoldAttendance) Icons.Default.Verified else Icons.Default.EmojiEvents,
                            contentDescription = badge.title,
                            tint = DeepEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                    if (badge.subtitle.isNotBlank()) {
                        Text(
                            text = "• ${badge.subtitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEmerald.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DigitalStudentIdCardView(
    student: StudentRecord,
    schoolConfig: SchoolConfig,
    badges: List<StudentAutoBadge>,
    onSavePng: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qrBitmap = remember(student.qrCodeValue) {
        DocumentAndIdExporter.generateQrCodeBitmap(student.qrCodeValue, 320).asImageBitmap()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceWhite),
        border = BorderStroke(2.dp, GoldFoil),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Official Emerald & Gold Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(DeepForestGreen, DeepEmerald, RoyalGreen)
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarOrPhotoView(
                            base64Image = schoolConfig.logoBase64,
                            fallbackName = schoolConfig.schoolName,
                            size = 52.dp,
                            borderColor = GoldFoil
                        )
                        Column {
                            Text(
                                text = schoolConfig.schoolName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "بطاقة الهوية المدرسية الذكية • ${schoolConfig.schoolStage.arabicLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGoldLight,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "العام الدراسي: ${schoolConfig.academicYear} | ${schoolConfig.address}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GoldFoil.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, GoldFoil)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = GoldFoil,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "كود ${schoolConfig.schoolId.ifBlank { "100" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGoldLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Gold accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(GoldFoil)
            )

            // Main ID Card Body: Student Photo + Info + QR Code
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Student Photo + Badges
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AvatarOrPhotoView(
                            base64Image = student.photoBase64,
                            fallbackName = student.fullNameWithFather,
                            size = 84.dp,
                            borderColor = GoldFoil
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = DeepEmerald.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = student.studentUsername,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Student Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "الاسم الكامل للطالب:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedGreen
                        )
                        Text(
                            text = student.fullNameWithFather,
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepForestGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "${student.gradeClass} — شعبة (${student.sectionName})",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (student.parentPhone.isNotBlank()) {
                            Text(
                                text = "هاتف ولي الأمر: ${student.parentPhone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedGreen
                            )
                        }
                        if (student.isBusSubscribed) {
                            Text(
                                text = "مشترك في الخط المدرسي (${student.busRouteName})",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // QR Code Box
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, GoldFoil),
                            shadowElevation = 2.dp
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "رمز QR للطالب ${student.fullNameWithFather}",
                                modifier = Modifier
                                    .size(96.dp)
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Text(
                            text = "بصمة الحضور QR",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Auto-badges row inside the ID card
                if (badges.isNotEmpty()) {
                    StudentAutoBadgesRow(badges = badges)
                }

                // QR Code text value pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = DeepForestGreen.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, DeepForestGreen.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "معرّف المسح الضوئي:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedGreen
                            )
                        }
                        Text(
                            text = student.qrCodeValue,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = DeepForestGreen
                        )
                    }
                }

                HorizontalDivider(color = DeepEmerald.copy(alpha = 0.12f))

                // Two Explicit Action Buttons Below the ID Card
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSavePng,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_id_png_button_${student.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = GoldFoil,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حفظ الهوية في الجهاز (صورة عالية الدقة PNG)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_id_pdf_button_${student.id}"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, GoldFoil),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DeepForestGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = DeepEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "طباعة / تصدير PDF (بحجم البطاقة القياسي)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DigitalStudentIdDialog(
    student: StudentRecord,
    schoolConfig: SchoolConfig,
    badges: List<StudentAutoBadge>,
    onSavePng: () -> Unit,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = CardSurfaceWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = DeepEmerald
                        )
                        Text(
                            text = "بطاقة الهوية المدرسية الرقمية",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepForestGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextMutedGreen
                        )
                    }
                }

                DigitalStudentIdCardView(
                    student = student,
                    schoolConfig = schoolConfig,
                    badges = badges,
                    onSavePng = onSavePng,
                    onExportPdf = onExportPdf
                )
            }
        }
    }
}

@Composable
fun QrAttendanceScannerDialog(
    students: List<StudentRecord>,
    selectedDate: String,
    onScanQrCode: (String) -> Unit,
    onScanStudentDetailed: (String) -> StudentRecord? = { code ->
        onScanQrCode(code)
        val trimmed = code.trim()
        val parts = trimmed.split("|")
        val extractedId = if (parts.size >= 3 && parts[0].equals("MADRASATI", ignoreCase = true)) {
            parts[2].trim()
        } else {
            trimmed
        }
        students.firstOrNull {
            it.qrCodeValue.equals(trimmed, ignoreCase = true) ||
                it.id.equals(extractedId, ignoreCase = true) ||
                it.studentUsername.equals(trimmed, ignoreCase = true)
        }
    },
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var qrInput by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf("") }
    var showLiveCamera by remember { mutableStateOf(true) }
    var lastScannedStudent by remember { mutableStateOf<StudentRecord?>(null) }
    var lastScanTimeText by remember { mutableStateOf("") }

    fun handleQrDetected(rawCode: String) {
        val clean = rawCode.trim()
        if (clean.isBlank()) return
        com.example.util.StandardQrCodeHelper.playScanBeepAndVibrate(context)
        val matched = onScanStudentDetailed(clean)
        if (matched != null) {
            lastScannedStudent = matched
            lastScanTimeText = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale("ar")).format(java.util.Date())
        }
    }

    val filteredStudents = remember(students, searchFilter) {
        val q = searchFilter.trim()
        if (q.isEmpty()) students
        else students.filter {
            it.fullNameWithFather.contains(q, ignoreCase = true) ||
                it.studentUsername.contains(q, ignoreCase = true) ||
                it.qrCodeValue.contains(q, ignoreCase = true) ||
                it.gradeClass.contains(q, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = CardSurfaceWhite,
            border = BorderStroke(1.5.dp, EmeraldAccent.copy(alpha = 0.65f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
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
                                shape = CircleShape,
                                color = DeepEmerald
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color(0xFFA7F3D0),
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "الماسح الضوئي الفعلي بالكاميرا (QR Scanner)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepForestGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "يقرأ رمز QR الحقيقي لهوية الطالب ويسجله (حاضر) ليوم $selectedDate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedGreen
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextMutedGreen
                            )
                        }
                    }
                }

                // Live Camera QR Scanner Header & Toggle Button (Fixed alignment, no text clipping)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showLiveCamera) "الكاميرا الحية نشطة — وجّه العدسة للباركود" else "الكاميرا متوقفة مؤقتاً",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepEmerald,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showLiveCamera = !showLiveCamera },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showLiveCamera) "إيقاف الكاميرا" else "تشغيل الكاميرا",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (showLiveCamera) {
                    item {
                        com.example.util.StandardQrCodeHelper.LiveCameraQrScannerBox(
                            onQrCodeScanned = { scannedValue ->
                                handleQrDetected(scannedValue)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            students = students
                        )
                    }
                }

                // Instant Scanned Student Confirmation Card
                lastScannedStudent?.let { scannedStu ->
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scanned_student_confirmation_card"),
                            shape = RoundedCornerShape(16.dp),
                            color = SuccessGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, SuccessGreen)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AvatarOrPhotoView(
                                    base64Image = scannedStu.photoBase64,
                                    fallbackName = scannedStu.fullNameWithFather,
                                    size = 54.dp,
                                    borderColor = SuccessGreen
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "تم التعرف على الطالب وتسجيل حضوره فوراً!",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = DeepForestGreen,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        text = scannedStu.fullNameWithFather,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "${scannedStu.gradeClass} — شعبة (${scannedStu.sectionName}) • وقت الوصول: $lastScanTimeText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepEmerald,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "تم إرسال إشعار فوري لولي الأمر (${scannedStu.parentName.ifBlank { "ولي الأمر" }}) بوصول الطالب للمدرسة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Direct QR code scanner / reader input
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = qrInput,
                            onValueChange = { qrInput = it },
                            label = { Text("قارئ الباركود أو كود الهوية (MADRASATI|...)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("qr_attendance_input"),
                            leadingIcon = {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = DeepEmerald)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )

                        Button(
                            onClick = {
                                if (qrInput.isNotBlank()) {
                                    handleQrDetected(qrInput.trim())
                                    qrInput = ""
                                }
                            },
                            enabled = qrInput.isNotBlank(),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("confirm_qr_scan_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepEmerald,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFA7F3D0))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    HorizontalDivider(color = DeepEmerald.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "المسح السريع لهويات الطلاب المسجلين عند البوابة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepForestGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = searchFilter,
                        onValueChange = { searchFilter = it },
                        label = { Text("بحث سريع باسم الطالب أو الصف...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = DeepEmerald)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(filteredStudents, key = { it.id }) { stu ->
                    val isJustScanned = lastScannedStudent?.id == stu.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleQrDetected(stu.qrCodeValue) }
                            .testTag("qr_quick_scan_${stu.id}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isJustScanned) SuccessGreen.copy(alpha = 0.12f) else DeepEmerald.copy(alpha = 0.05f),
                        border = BorderStroke(
                            1.dp,
                            if (isJustScanned) SuccessGreen else DeepEmerald.copy(alpha = 0.18f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                AvatarOrPhotoView(
                                    base64Image = stu.photoBase64,
                                    fallbackName = stu.fullNameWithFather,
                                    size = 38.dp,
                                    borderColor = if (isJustScanned) SuccessGreen else DeepEmerald
                                )
                                Column {
                                    Text(
                                        text = stu.fullNameWithFather,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${stu.gradeClass} (${stu.sectionName})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedGreen
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isJustScanned) SuccessGreen else DeepEmerald
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isJustScanned) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = if (isJustScanned) Color.White else Color(0xFFA7F3D0),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isJustScanned) "تم المسح" else "مسح QR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact, calm, 60%-smaller single-row glassmorphic pill bar for Student and Parent accounts.
 * - Uses calm dark emerald-slate colors instead of bright gold/orange.
 * - Right side: Shows child switcher chips ONLY if `isParentRole && children.size > 1`.
 *   If the parent has only 1 child, or if the user is a Student (`!isParentRole`), hides switcher
 *   buttons completely and shows a calm compact student label.
 * - Left side: Sleek compact button "بطاقة الهوية" that opens the Digital ID popup dialog.
 */
@Composable
fun ParentMultiChildSwitcherBar(
    children: List<StudentRecord>,
    selectedStudent: StudentRecord?,
    onSelectStudent: (String) -> Unit,
    onOpenStudentIdCard: (StudentRecord) -> Unit,
    modifier: Modifier = Modifier,
    isParentRole: Boolean = true,
    showDigitalIdButton: Boolean = true
) {
    if (children.isEmpty()) return
    val activeChild = selectedStudent ?: children.first()
    val hasMultipleChildren = isParentRole && children.size > 1

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("compact_student_parent_top_bar"),
        shape = RoundedCornerShape(50),
        color = Color(0xFF0A2E22).copy(alpha = 0.90f),
        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.32f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right Side: Multi-child compact chips (ONLY if parent has >1 child) OR calm single student info
            if (hasMultipleChildren) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    children.forEach { child ->
                        val isSelected = child.id == activeChild.id
                        Surface(
                            modifier = Modifier
                                .clickable { onSelectStudent(child.id) }
                                .testTag("switch_child_chip_${child.id}"),
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) {
                                EmeraldAccent.copy(alpha = 0.25f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldAccent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.14f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                AvatarOrPhotoView(
                                    base64Image = child.photoBase64,
                                    fallbackName = child.fullNameWithFather,
                                    size = 20.dp,
                                    borderColor = if (isSelected) EmeraldAccent else Color.White.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = child.studentName.ifBlank { child.fullNameWithFather },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f),
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    AvatarOrPhotoView(
                        base64Image = activeChild.photoBase64,
                        fallbackName = activeChild.fullNameWithFather,
                        size = 22.dp,
                        borderColor = EmeraldAccent.copy(alpha = 0.7f)
                    )
                    Text(
                        text = activeChild.fullNameWithFather,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "• ${activeChild.gradeClass} (${activeChild.sectionName})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showDigitalIdButton) {
                Spacer(modifier = Modifier.width(8.dp))

                // Left Side: Sleek compact "بطاقة الهوية" pill button
                Surface(
                    modifier = Modifier
                        .clickable { onOpenStudentIdCard(activeChild) }
                        .testTag("parent_bar_open_id_button"),
                    shape = RoundedCornerShape(50),
                    color = DeepEmerald.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "بطاقة الهوية",
                            tint = Color(0xFFA7F3D0),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "بطاقة الهوية",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the 14-day Subscription Expiry Warning Banner and Developer Broadcasts
 * exclusively to the School Principal (ADMIN), with an immediate (×) close/dismiss button.
 */
@Composable
fun AdminSubscriptionAndBroadcastBanner(
    schoolConfig: SchoolConfig,
    developerBroadcasts: List<DeveloperBroadcastItem>,
    modifier: Modifier = Modifier,
    dismissedBroadcastIds: Set<String> = emptySet(),
    onDismissBroadcast: (String) -> Unit = {}
) {
    var isExpiryWarningDismissed by remember { mutableStateOf(false) }
    var isBroadcastBannerDismissed by remember { mutableStateOf(false) }

    val daysRemaining = schoolConfig.daysUntilExpiry()
    val showExpiryWarning = schoolConfig.isWithinRenewalWarningWindow() && !isExpiryWarningDismissed
    val visibleBroadcasts = remember(developerBroadcasts, dismissedBroadcastIds, isBroadcastBannerDismissed) {
        if (isBroadcastBannerDismissed) {
            emptyList()
        } else {
            developerBroadcasts.filterNot { bc ->
                val key = bc.id.ifBlank { "${bc.title}_${bc.createdAt}" }
                bc.id in dismissedBroadcastIds || key in dismissedBroadcastIds
            }
        }
    }
    val latestBroadcast = visibleBroadcasts.firstOrNull()

    if (!showExpiryWarning && latestBroadcast == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showExpiryWarning) {
            val isExpired = daysRemaining <= 0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isExpired) DangerRed.copy(alpha = 0.12f) else DeepEmerald.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, if (isExpired) DangerRed else DeepEmerald.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (isExpired) DangerRed else DeepEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isExpired) {
                                "انتهى اشتراك المدرسة (${schoolConfig.subscriptionExpiryDate}) — يرجى التواصل مع المطور للتجديد"
                            } else {
                                "تنبيه تجديد الاشتراك: متبقي ($daysRemaining) يوم على انتهاء اشتراك المدرسة (${schoolConfig.subscriptionExpiryDate})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepForestGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "لتجديد رخصة المدرسة يرجى التواصل مع المطور أحمد أبو رسلان (07512362567)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedGreen
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = DeepForestGreen.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { isExpiryWarningDismissed = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق التنبيه",
                                tint = DeepForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (latestBroadcast != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_developer_broadcast_banner"),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepEmerald.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = DeepEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تعميم مدراء المدارس: ${latestBroadcast.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepForestGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${latestBroadcast.message}  •  (${latestBroadcast.createdAt})",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEmerald
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = DeepEmerald.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                isBroadcastBannerDismissed = true
                                developerBroadcasts.forEach { bc ->
                                    val key = bc.id.ifBlank { "${bc.title}_${bc.createdAt}" }
                                    onDismissBroadcast(key)
                                    if (bc.id.isNotBlank()) {
                                        onDismissBroadcast(bc.id)
                                    }
                                }
                            }
                            .testTag("dismiss_developer_broadcast_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "تجاهل وإغلاق التعميم",
                                tint = DeepForestGreen,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarOrPhotoView(
    base64Image: String,
    fallbackName: String,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    borderColor: Color = GoldFoil
) {
    Base64AvatarImage(
        base64Data = base64Image,
        fallbackIcon = Icons.Default.Badge,
        contentDescription = fallbackName,
        size = size,
        tint = borderColor
    )
}

