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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
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
import com.example.model.AttendanceStatus
import com.example.model.CanteenTransactionType
import com.example.model.CanteenWalletTransaction
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
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
            Surface(
                shape = RoundedCornerShape(50),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 0.dp,
                modifier = Modifier.height(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (badge.isGoldAttendance) Icons.Default.Verified else Icons.Default.EmojiEvents,
                        contentDescription = badge.title,
                        tint = DeepEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = badge.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
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
    initialWalletMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isWalletPassMode by remember(student.id, initialWalletMode) { mutableStateOf(initialWalletMode) }
    val todayIso = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    val todayAttendanceLabel = remember(student, todayIso) {
        val raw = student.dailyAttendance[todayIso]
            ?: if (com.example.model.ArabicTextNormalizer.normalizeDateIso(student.todayStatusDate) == todayIso) student.todayStatus else ""
        when (raw.trim().uppercase()) {
            "ABSENT" -> "غائب"
            "EXCUSED" -> "مجاز رسمياً"
            "LATE" -> "متأخر"
            "HOLIDAY" -> "عطلة رسمية"
            else -> "حاضر"
        }
    }
    val activeQrString = if (isWalletPassMode) student.walletBarcode else student.qrCodeValue
    val qrBitmap = remember(activeQrString) {
        DocumentAndIdExporter.generateQrCodeBitmap(activeQrString, 320).asImageBitmap()
    }
    val spentToday = remember(student, todayIso) { student.getSpentToday(todayIso) }

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
                                text = if (isWalletPassMode) {
                                    "بطاقة محفظة الحانوت الذكية • ${schoolConfig.schoolStage.arabicLabel}"
                                } else {
                                    "بطاقة الهوية المدرسية الذكية • ${schoolConfig.schoolStage.arabicLabel}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGoldLight,
                                fontWeight = FontWeight.SemiBold
                            )
                            val cleanCardAddr = com.example.model.ArabicTextNormalizer.cleanDefaultAddress(schoolConfig.address)
                            Text(
                                text = if (cleanCardAddr.isNotBlank()) {
                                    "العام الدراسي: ${schoolConfig.academicYear} | $cleanCardAddr"
                                } else {
                                    "العام الدراسي: ${schoolConfig.academicYear}"
                                },
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

            // Main ID Card Body: Capsule Mode Switcher + Student Photo + Info + QR Code
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Capsule Mode Switcher: (1) هوية الحضور الأكاديمية | (2) بطاقة محفظة الحانوت
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    color = DeepForestGreen.copy(alpha = 0.07f),
                    border = BorderStroke(1.dp, GoldFoil.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { isWalletPassMode = false }
                                .testTag("id_card_mode_academic_${student.id}"),
                            shape = RoundedCornerShape(50),
                            color = if (!isWalletPassMode) DeepEmerald else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = if (!isWalletPassMode) GoldFoil else DeepForestGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "هوية الحضور الأكاديمية",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (!isWalletPassMode) Color.White else DeepForestGreen,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { isWalletPassMode = true }
                                .testTag("id_card_mode_wallet_${student.id}"),
                            shape = RoundedCornerShape(50),
                            color = if (isWalletPassMode) DeepEmerald else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isWalletPassMode) GoldFoil else DeepForestGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "بطاقة محفظة الحانوت",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isWalletPassMode) Color.White else DeepForestGreen,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

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
                        if (!isWalletPassMode) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DeepForestGreen.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "حالة الدوام اليوم: $todayAttendanceLabel",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepForestGreen,
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
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, GoldFoil.copy(alpha = 0.75f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "الرصيد المتوفر: ${student.walletBalance} د.ع",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "الحد اليومي: ${if (student.dailySpendLimit > 0) "${student.dailySpendLimit} د.ع" else "مفتوح"} • المصروف اليوم: $spentToday د.ع",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepEmerald,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
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
                            text = if (isWalletPassMode) "باركود محفظة الحانوت" else "بصمة الحضور QR",
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
                                imageVector = if (isWalletPassMode) Icons.Default.AccountBalanceWallet else Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isWalletPassMode) "باركود الدفع بالحانوت:" else "معرّف المسح الضوئي:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedGreen
                            )
                        }
                        Text(
                            text = activeQrString,
                            style = MaterialTheme.typography.labelSmall,
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
    todayAttendanceLabel: String = "",
    todayIso: String = "",
    initialWalletMode: Boolean = false,
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
                            text = "بطاقة هوية الطالب الذكية ومحفظة الحانوت",
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
                    onExportPdf = onExportPdf,
                    initialWalletMode = initialWalletMode
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
                            text = "لتجديد رخصة المدرسة يرجى التواصل مع الدعم الفني للمنصة",
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

/**
 * Smart Canteen E-Wallet & POS Dialog (for Principal and Authorized Teacher/Cashier).
 * Provides:
 * 1. POS Cashier ("نقطة بيع الحانوت - مسح باركود الشراء"):
 *    - Live camera barcode scanner + student quick search/selection
 *    - Student wallet summary (current balance, parent's daily limit, spent today, remaining today)
 *    - Quick capsule purchase buttons (250, 500, 750, 1,000, 1,500, 2,000 د.ع) + manual input
 *    - Real-time evaluation:
 *      * Case 1 (Sufficient balance & within daily limit): Green button "قبول العملية وخصم المبلغ"
 *      * Case 2 (Insufficient balance or exceeds daily limit): Red banner "رفض العملية" with reason + "رفض العملية وإشعار ولي الأمر"
 * 2. Top-Up Wallet ("شحن رصيد طالب"):
 *    - Quick capsule buttons (1,000, 2,000, 5,000, 10,000, 25,000 د.ع) + manual input + "شحن المحفظة سحابياً"
 * 3. Canteen Transactions Log ("سجل الحركات المالية")
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartCanteenPosAndWalletDialog(
    students: List<StudentRecord>,
    todayIso: String,
    onTopUpWallet: (studentId: String, amountIqd: Int, note: String, onComplete: (Boolean, String) -> Unit) -> Unit,
    onProcessPurchase: (studentId: String, amountIqd: Int, itemSummary: String, onComplete: (Boolean, String) -> Unit) -> Unit,
    onFindStudentByQr: (String) -> StudentRecord?,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = POS Purchase, 1 = Top-Up Wallet, 2 = Transactions
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    val selectedStudent = remember(students, selectedStudentId) {
        students.firstOrNull { it.id == selectedStudentId } ?: students.firstOrNull()
    }
    var searchFilter by remember { mutableStateOf("") }
    var showLiveCameraScanner by remember { mutableStateOf(false) }
    var barcodeManualInput by remember { mutableStateOf("") }

    var purchaseAmountText by remember { mutableStateOf("500") }
    var purchaseItemSummary by remember { mutableStateOf("مشتريات من حانوت المدرسة") }
    var topUpAmountText by remember { mutableStateOf("5000") }
    var topUpNote by remember { mutableStateOf("شحن رصيد نقدي من كاشير الحانوت") }

    var feedbackBannerMessage by remember { mutableStateOf<String?>(null) }
    var feedbackBannerSuccess by remember { mutableStateOf(true) }

    val filteredStudents = remember(students, searchFilter) {
        val q = searchFilter.trim()
        if (q.isEmpty()) students
        else students.filter {
            it.fullNameWithFather.contains(q, ignoreCase = true) ||
                it.gradeClass.contains(q, ignoreCase = true) ||
                it.walletBarcode.contains(q, ignoreCase = true) ||
                it.id.contains(q, ignoreCase = true)
        }
    }

    fun handleScannedWalletCode(rawCode: String) {
        val found = onFindStudentByQr(rawCode)
        if (found != null) {
            selectedStudentId = found.id
            feedbackBannerSuccess = true
            feedbackBannerMessage = "تم التعرف على محفظة الطالب: ${found.fullNameWithFather} — الرصيد: ${"%,d".format(java.util.Locale.US, found.walletBalance)} د.ع"
        } else {
            feedbackBannerSuccess = false
            feedbackBannerMessage = "لم يتم العثور على طالب مطابق للباركود الممسوح: $rawCode"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DeepEmerald.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "نظام كاشير ومحفظة الحانوت الذكية",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepForestGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "نقطة بيع الباركود وشحن الأرصدة المتزامن سحابياً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedGreen
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = DeepForestGreen)
                        }
                    }
                }

                // Capsule Mode Switcher
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        color = DeepEmerald.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tabs = listOf(
                                Triple(0, "نقطة بيع الحانوت", Icons.Default.ShoppingCart),
                                Triple(1, "شحن رصيد طالب", Icons.Default.AddCard),
                                Triple(2, "سجل الحركات", Icons.Default.History)
                            )
                            tabs.forEach { (idx, title, icon) ->
                                val isSelected = activeTab == idx
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            activeTab = idx
                                            feedbackBannerMessage = null
                                        }
                                        .testTag("canteen_tab_$idx"),
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) DeepEmerald else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) GoldFoil else DeepForestGreen,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White else DeepForestGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Feedback Banner
                if (feedbackBannerMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (feedbackBannerSuccess) SuccessGreen.copy(alpha = 0.12f) else DangerRed.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.2.dp,
                                if (feedbackBannerSuccess) SuccessGreen else DangerRed
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (feedbackBannerSuccess) Icons.Default.CheckCircle else Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (feedbackBannerSuccess) SuccessGreen else DangerRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = feedbackBannerMessage.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (feedbackBannerSuccess) DeepForestGreen else DangerRed,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Live Camera Barcode Scanner Toggle + Manual Barcode Reader
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "مسح باركود محفظة الطالب (WALLET-...)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Surface(
                                    modifier = Modifier.clickable { showLiveCameraScanner = !showLiveCameraScanner },
                                    shape = RoundedCornerShape(50),
                                    color = if (showLiveCameraScanner) DangerRed else DeepEmerald
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showLiveCameraScanner) Icons.Default.Close else Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (showLiveCameraScanner) "إغلاق الكاميرا" else "مسح بالكاميرا",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (showLiveCameraScanner) {
                                com.example.util.StandardQrCodeHelper.LiveCameraQrScannerBox(
                                    onQrCodeScanned = { code: String ->
                                        handleScannedWalletCode(code)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    students = students
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = barcodeManualInput,
                                    onValueChange = { barcodeManualInput = it },
                                    label = { Text("قارئ باركود المحفظة أو بحث بالاسم...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = {
                                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = DeepEmerald)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Button(
                                    onClick = {
                                        if (barcodeManualInput.isNotBlank()) {
                                            handleScannedWalletCode(barcodeManualInput.trim())
                                            barcodeManualInput = ""
                                        }
                                    },
                                    enabled = barcodeManualInput.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                                ) {
                                    Text("قراءة", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick Student Selector Chips
                            OutlinedTextField(
                                value = searchFilter,
                                onValueChange = { searchFilter = it },
                                label = { Text("تصفية قائمة الطلاب بالاسم أو الصف...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = DeepEmerald)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filteredStudents.forEach { stu ->
                                    val isSel = stu.id == selectedStudent?.id
                                    Surface(
                                        modifier = Modifier
                                            .clickable {
                                                selectedStudentId = stu.id
                                                feedbackBannerMessage = null
                                            }
                                            .testTag("canteen_select_student_${stu.id}"),
                                        shape = RoundedCornerShape(50),
                                        color = if (isSel) DeepEmerald else Color.White,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSel) GoldFoil else DeepEmerald.copy(alpha = 0.28f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AvatarOrPhotoView(
                                                base64Image = stu.photoBase64,
                                                fallbackName = stu.fullNameWithFather,
                                                size = 22.dp,
                                                borderColor = if (isSel) GoldFoil else DeepEmerald
                                            )
                                            Column {
                                                Text(
                                                    text = stu.fullNameWithFather,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSel) Color.White else DeepForestGreen,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "${"%,d".format(java.util.Locale.US, stu.walletBalance)} د.ع • ${stu.gradeClass}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSel) Color(0xFFA7F3D0) else TextMutedGreen,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Student Wallet Card
                if (selectedStudent != null) {
                    val spentToday = selectedStudent.getSpentToday(todayIso)
                    val dailyLimit = selectedStudent.dailySpendLimit
                    val remainingAllowedToday = if (dailyLimit > 0) (dailyLimit - spentToday).coerceAtLeast(0) else selectedStudent.walletBalance

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF0A2E22),
                            border = BorderStroke(1.5.dp, GoldFoil.copy(alpha = 0.65f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AvatarOrPhotoView(
                                            base64Image = selectedStudent.photoBase64,
                                            fallbackName = selectedStudent.fullNameWithFather,
                                            size = 44.dp,
                                            borderColor = GoldFoil
                                        )
                                        Column {
                                            Text(
                                                text = selectedStudent.fullNameWithFather,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "${selectedStudent.gradeClass} (${selectedStudent.sectionName}) • ${selectedStudent.walletBarcode}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = GoldFoil.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, GoldFoil.copy(alpha = 0.6f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "الرصيد المتوفر",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "${"%,d".format(java.util.Locale.US, selectedStudent.walletBalance)} د.ع",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = GoldFoil,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "الحد اليومي من الأب",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.72f),
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = if (dailyLimit > 0) "${"%,d".format(java.util.Locale.US, dailyLimit)} د.ع" else "مفتوح",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFFDE68A),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "المصروف اليوم",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.72f),
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "${"%,d".format(java.util.Locale.US, spentToday)} د.ع",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "المتبقي المسموح اليوم",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.72f),
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = if (dailyLimit > 0) "${"%,d".format(java.util.Locale.US, remainingAllowedToday)} د.ع" else "${"%,d".format(java.util.Locale.US, selectedStudent.walletBalance)} د.ع",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFA7F3D0),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tab 0: POS Cashier Purchase
                    if (activeTab == 0) {
                        val parsedPurchaseAmount = purchaseAmountText.filter { it.isDigit() }.toIntOrNull() ?: 0
                        val isInsufficientBalance = parsedPurchaseAmount > selectedStudent.walletBalance
                        val isExceedingDailyLimit = dailyLimit > 0 && (spentToday + parsedPurchaseAmount) > dailyLimit
                        val canApprovePurchase = parsedPurchaseAmount > 0 && !isInsufficientBalance && !isExceedingDailyLimit

                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "اختر مبلغ الشراء السريع (د.ع):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val quickPrices = listOf(250, 500, 750, 1000, 1500, 2000)
                                        quickPrices.forEach { price ->
                                            val isSelectedPrice = parsedPurchaseAmount == price
                                            Surface(
                                                modifier = Modifier
                                                    .clickable { purchaseAmountText = price.toString() }
                                                    .testTag("pos_quick_price_$price"),
                                                shape = RoundedCornerShape(50),
                                                color = if (isSelectedPrice) DeepEmerald else Color.White,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelectedPrice) GoldFoil else DeepEmerald.copy(alpha = 0.35f)
                                                )
                                            ) {
                                                Text(
                                                    text = "${"%,d".format(java.util.Locale.US, price)} د.ع",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isSelectedPrice) Color.White else DeepForestGreen,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = purchaseAmountText,
                                            onValueChange = { purchaseAmountText = it.filter { ch -> ch.isDigit() } },
                                            label = { Text("مبلغ الشراء (د.ع)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = purchaseItemSummary,
                                            onValueChange = { purchaseItemSummary = it },
                                            label = { Text("بيان المشتريات") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    // Case 1 vs Case 2 Live Status Evaluation
                                    if (parsedPurchaseAmount > 0) {
                                        if (canApprovePurchase) {
                                            Button(
                                                onClick = {
                                                    onProcessPurchase(
                                                        selectedStudent.id,
                                                        parsedPurchaseAmount,
                                                        purchaseItemSummary
                                                    ) { ok, msg ->
                                                        feedbackBannerSuccess = ok
                                                        feedbackBannerMessage = msg
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .testTag("pos_approve_purchase_btn"),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = DeepEmerald,
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFA7F3D0))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "قبول العملية وخصم المبلغ (${"%,d".format(java.util.Locale.US, parsedPurchaseAmount)} د.ع)",
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        } else {
                                            val rejectReasonText = if (isInsufficientBalance) {
                                                "رفض العملية: الرصيد غير كافٍ (المطلوب ${"%,d".format(java.util.Locale.US, parsedPurchaseAmount)} د.ع والمتوفر ${"%,d".format(java.util.Locale.US, selectedStudent.walletBalance)} د.ع)"
                                            } else {
                                                "رفض العملية: تجاوز السقف اليومي المسموح من ولي الأمر (${"%,d".format(java.util.Locale.US, dailyLimit)} د.ع — المصروف اليوم ${"%,d".format(java.util.Locale.US, spentToday)} د.ع)"
                                            }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                color = DangerRed.copy(alpha = 0.10f),
                                                border = BorderStroke(1.2.dp, DangerRed)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Block,
                                                            contentDescription = null,
                                                            tint = DangerRed,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Text(
                                                            text = rejectReasonText,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = DangerRed,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            onProcessPurchase(
                                                                selectedStudent.id,
                                                                parsedPurchaseAmount,
                                                                purchaseItemSummary
                                                            ) { ok, msg ->
                                                                feedbackBannerSuccess = ok
                                                                feedbackBannerMessage = msg
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(46.dp)
                                                            .testTag("pos_reject_and_notify_parent_btn"),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = DangerRed,
                                                            contentColor = Color.White
                                                        )
                                                    ) {
                                                        Icon(Icons.Default.WarningAmber, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "تثبيت رفض العملية وإرسال إشعار فوري لولي الأمر",
                                                            fontWeight = FontWeight.ExtraBold
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

                    // Tab 1: Top-Up Student Wallet
                    if (activeTab == 1) {
                        val parsedTopUp = topUpAmountText.filter { it.isDigit() }.toIntOrNull() ?: 0
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.28f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "اختر فئة شحن الرصيد السريع (د.ع):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val quickTopUps = listOf(1000, 2000, 5000, 10000, 25000)
                                        quickTopUps.forEach { amt ->
                                            val isSelectedAmt = parsedTopUp == amt
                                            Surface(
                                                modifier = Modifier
                                                    .clickable { topUpAmountText = amt.toString() }
                                                    .testTag("topup_quick_amt_$amt"),
                                                shape = RoundedCornerShape(50),
                                                color = if (isSelectedAmt) DeepEmerald else Color.White,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelectedAmt) GoldFoil else DeepEmerald.copy(alpha = 0.35f)
                                                )
                                            ) {
                                                Text(
                                                    text = "${"%,d".format(java.util.Locale.US, amt)} د.ع",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isSelectedAmt) Color.White else DeepForestGreen,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = topUpAmountText,
                                            onValueChange = { topUpAmountText = it.filter { ch -> ch.isDigit() } },
                                            label = { Text("مبلغ الشحن (د.ع)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = topUpNote,
                                            onValueChange = { topUpNote = it },
                                            label = { Text("ملاحظة الشحن") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (parsedTopUp > 0) {
                                                onTopUpWallet(selectedStudent.id, parsedTopUp, topUpNote) { ok, msg ->
                                                    feedbackBannerSuccess = ok
                                                    feedbackBannerMessage = msg
                                                }
                                            }
                                        },
                                        enabled = parsedTopUp > 0,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("confirm_topup_wallet_btn"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DeepEmerald,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.AddCard, contentDescription = null, tint = GoldFoil)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "شحن المحفظة سحابياً (+${"%,d".format(java.util.Locale.US, parsedTopUp)} د.ع)",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tab 2: Transactions Log for Selected Student
                    if (activeTab == 2) {
                        val txList = selectedStudent.walletTransactions
                        if (txList.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = DeepEmerald.copy(alpha = 0.05f)
                                ) {
                                    Text(
                                        text = "لا توجد حركات مالية مسجلة في محفظة الطالب حتى الآن.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMutedGreen,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(txList.take(30), key = { it.id }) { tx ->
                                val isTopUp = tx.type == CanteenTransactionType.TOP_UP
                                val isRejected = tx.type == CanteenTransactionType.PURCHASE_REJECTED
                                val badgeColor = when {
                                    isTopUp -> SuccessGreen
                                    isRejected -> DangerRed
                                    else -> DeepEmerald
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = badgeColor.copy(alpha = 0.07f),
                                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${tx.type.arabicLabel} — ${"%,d".format(java.util.Locale.US, tx.amount)} د.ع",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isRejected) DangerRed else DeepForestGreen,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = tx.note,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMutedGreen
                                            )
                                            Text(
                                                text = "${tx.timestamp} • بواسطة: ${tx.actorName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMutedGreen,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(
                                            text = "الرصيد: ${"%,d".format(java.util.Locale.US, tx.balanceAfter)} د.ع",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepEmerald,
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
}

/**
 * Parent & Student Smart Canteen E-Wallet Card.
 * - Shows current balance in IQD, spent today, and daily spend limit.
 * - Allows Parent to configure and save "الحد الأقصى للشراء اليومي (د.ع)" with 1 click.
 * - Allows Student & Parent to open the Wallet Barcode Pass ("باركود محفظة الحانوت") and inspect the full transaction history.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentStudentCanteenWalletCard(
    student: StudentRecord,
    todayIso: String,
    isParentRole: Boolean,
    onUpdateDailyLimit: (Int, (Boolean, String) -> Unit) -> Unit,
    onOpenWalletBarcodePass: () -> Unit,
    modifier: Modifier = Modifier
) {
    var limitInput by remember(student.id, student.dailySpendLimit) {
        mutableStateOf(student.dailySpendLimit.toString())
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showTransactions by remember { mutableStateOf(false) }
    val spentToday = student.getSpentToday(todayIso)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("parent_student_canteen_wallet_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2E22)),
        border = BorderStroke(1.5.dp, GoldFoil.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                            .background(GoldFoil.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GoldFoil,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "محفظة حانوت المدرسة الذكية",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${student.fullNameWithFather} • ${student.walletBarcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clickable { onOpenWalletBarcodePass() }
                        .testTag("open_wallet_qr_pass_btn"),
                    shape = RoundedCornerShape(50),
                    color = GoldFoil,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = DeepForestGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "باركود الحانوت",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepForestGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, GoldFoil.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "الرصيد الحالي",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${"%,d".format(java.util.Locale.US, student.walletBalance)} د.ع",
                            style = MaterialTheme.typography.titleSmall,
                            color = GoldFoil,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "الحد اليومي المسموح",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = if (student.dailySpendLimit > 0) {
                                "${"%,d".format(java.util.Locale.US, student.dailySpendLimit)} د.ع"
                            } else {
                                "مفتوح"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ما تم صرفه اليوم",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${"%,d".format(java.util.Locale.US, spentToday)} د.ع",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFA7F3D0),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Parent Daily Spend Limit Controls
            if (isParentRole) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.07f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "تحديد الحد الأقصى للشراء اليومي للطالب (د.ع) — (0 = مفتوح بدون سقف):",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val quickLimits = listOf(0, 1000, 1500, 2000, 3000, 5000)
                            val parsedCur = limitInput.filter { it.isDigit() }.toIntOrNull() ?: 0
                            quickLimits.forEach { lim ->
                                val isSel = parsedCur == lim
                                Surface(
                                    modifier = Modifier
                                        .clickable {
                                            limitInput = lim.toString()
                                            onUpdateDailyLimit(lim) { _, msg -> statusMessage = msg }
                                        }
                                        .testTag("parent_quick_limit_$lim"),
                                    shape = RoundedCornerShape(50),
                                    color = if (isSel) GoldFoil else Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSel) GoldFoil else Color.White.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Text(
                                        text = if (lim == 0) "مفتوح (0)" else "${"%,d".format(java.util.Locale.US, lim)} د.ع",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSel) DeepForestGreen else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = limitInput,
                                onValueChange = { limitInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("الحد اليومي (د.ع)", color = Color.White.copy(alpha = 0.8f)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = {
                                    val newLim = limitInput.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    onUpdateDailyLimit(newLim) { _, msg -> statusMessage = msg }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = GoldFoil)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ السقف", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!statusMessage.isNullOrBlank()) {
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Toggle Transaction History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTransactions = !showTransactions },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFA7F3D0),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "سجل حركات الشحن والشراء من الحانوت (${student.walletTransactions.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (showTransactions) "إخفاء السجل" else "عرض السجل",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldFoil,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (showTransactions) {
                if (student.walletTransactions.isEmpty()) {
                    Text(
                        text = "لا توجد حركات مالية مسجلة في المحفظة حتى الآن.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        student.walletTransactions.take(15).forEach { tx ->
                            val isRejected = tx.type == CanteenTransactionType.PURCHASE_REJECTED
                            val isTopUp = tx.type == CanteenTransactionType.TOP_UP
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isRejected -> DangerRed.copy(alpha = 0.6f)
                                        isTopUp -> SuccessGreen.copy(alpha = 0.6f)
                                        else -> GoldFoil.copy(alpha = 0.35f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tx.type.arabicLabel}: ${"%,d".format(java.util.Locale.US, tx.amount)} د.ع",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when {
                                                isRejected -> Color(0xFFFCA5A5)
                                                isTopUp -> Color(0xFFA7F3D0)
                                                else -> Color.White
                                            },
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${tx.note} • ${tx.timestamp}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = "${"%,d".format(java.util.Locale.US, tx.balanceAfter)} د.ع",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldFoil,
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


