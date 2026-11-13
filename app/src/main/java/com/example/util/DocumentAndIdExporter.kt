package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import com.example.model.AnnualPromotionReport
import com.example.model.AttendanceEntry
import com.example.model.AttendanceStatus
import com.example.model.ClassComprehensiveStatisticalReport
import com.example.model.DailyBehaviorLogItem
import com.example.model.ExamScheduleItem
import com.example.model.LeaveRequestItem
import com.example.model.SchoolConfig
import com.example.model.StudentAutoBadge
import com.example.model.StudentRecord
import com.example.model.SubjectGrade
import com.example.model.TuitionInstallment
import com.example.model.TuitionRecord
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentAndIdExporter {

    private const val COLOR_DEEP_EMERALD = 0xFF0A2E1F.toInt()
    private const val COLOR_ROYAL_GREEN = 0xFF0F4C35.toInt()
    private const val COLOR_GOLD = 0xFFD4AF37.toInt()
    private const val COLOR_LIGHT_GOLD = 0xFFFBF3D5.toInt()
    private const val COLOR_OFF_WHITE = 0xFFF7FBF8.toInt()
    private const val COLOR_DARK_TEXT = 0xFF102A1E.toInt()
    private const val COLOR_MUTED_TEXT = 0xFF4A6659.toInt()

    @Volatile
    var activeWalletPassExportMode: Boolean = false

    private fun loadCairoTypeface(context: Context?, style: Int): Typeface {
        if (context != null) {
            try {
                val resId = when (style) {
                    Typeface.BOLD -> com.example.R.font.cairo_bold
                    else -> com.example.R.font.cairo_regular
                }
                val tf = androidx.core.content.res.ResourcesCompat.getFont(context, resId)
                    ?: androidx.core.content.res.ResourcesCompat.getFont(context, com.example.R.font.cairo)
                if (tf != null) {
                    return Typeface.create(tf, style)
                }
            } catch (_: Exception) {
            }
        }
        return Typeface.create(Typeface.DEFAULT, style)
    }

    private fun drawFittedRightText(
        canvas: Canvas,
        text: String,
        rightX: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        val localized = AppLocalizationEngine.localize(text)
        val originalSize = paint.textSize
        var currentSize = originalSize
        while (currentSize > 16f && paint.measureText(localized) > maxWidth) {
            currentSize -= 1.5f
            paint.textSize = currentSize
        }
        canvas.drawText(localized, rightX, y, paint)
        paint.textSize = originalSize
    }

    private fun drawFittedCenterText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        val localized = AppLocalizationEngine.localize(text)
        val originalSize = paint.textSize
        var currentSize = originalSize
        while (currentSize > 13f && paint.measureText(localized) > maxWidth) {
            currentSize -= 1f
            paint.textSize = currentSize
        }
        canvas.drawText(localized, centerX, y, paint)
        paint.textSize = originalSize
    }

    /**
     * Generates a 100% real ISO/IEC 18004 standard QR Code Bitmap in pure black on a pure white background
     * so any standard camera or QR scanner app reads the student's payload immediately.
     */
    fun generateQrCodeBitmap(content: String, sizePx: Int = 420): Bitmap {
        return StandardQrCodeHelper.generateStandardQrBitmap(content = content, sizePx = sizePx)
    }

    /**
     * Renders a high-resolution (1280 x 820 px) official Student ID Badge Bitmap
     * ready for saving as PNG or embedding into PDF for thermal lamination.
     * Supports both Academic Attendance ID mode and Canteen Wallet Pass mode with zero text overlap.
     */
    fun renderStudentIdCardBitmap(
        student: StudentRecord,
        schoolConfig: SchoolConfig,
        badges: List<StudentAutoBadge> = emptyList(),
        isWalletMode: Boolean = activeWalletPassExportMode,
        context: Context? = null
    ): Bitmap {
        val width = 1280
        val height = 820
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val boldTypeface = loadCairoTypeface(context, Typeface.BOLD)
        val regularTypeface = loadCairoTypeface(context, Typeface.NORMAL)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OFF_WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 36f, 36f, bgPaint)

        // Header luxury emerald gradient
        val headerHeight = 220f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), headerHeight,
                COLOR_DEEP_EMERALD, COLOR_ROYAL_GREEN, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), headerHeight), 36f, 36f, headerPaint)
        canvas.drawRect(0f, headerHeight - 36f, width.toFloat(), headerHeight, headerPaint)

        // Gold accent stripe under header
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, headerHeight - 8f, width.toFloat(), headerHeight + 4f, goldPaint)

        // School Logo or Crest in top-right
        val logoDecoded = decodeBase64Bitmap(schoolConfig.logoBase64)
        val logoCenterX = width - 110f
        val logoCenterY = 108f
        val logoRadius = 64f
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(logoCenterX, logoCenterY, logoRadius + 5f, goldPaint)
        canvas.drawCircle(logoCenterX, logoCenterY, logoRadius, circlePaint)

        if (logoDecoded != null) {
            val saveCount = canvas.save()
            val clipPath = Path().apply {
                addCircle(logoCenterX, logoCenterY, logoRadius - 4f, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            val dst = RectF(
                logoCenterX - logoRadius + 4f,
                logoCenterY - logoRadius + 4f,
                logoCenterX + logoRadius - 4f,
                logoCenterY + logoRadius - 4f
            )
            canvas.drawBitmap(logoDecoded, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restoreToCount(saveCount)
        } else {
            val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 36f
                typeface = boldTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("مدرستي", logoCenterX, logoCenterY + 12f, initialsPaint)
        }

        // Header Text (Right-aligned adjacent to logo)
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LIGHT_GOLD
            textSize = 25f
            typeface = regularTypeface
            textAlign = Paint.Align.RIGHT
        }
        val textRightX = width - 200f
        val maxHeaderWidth = textRightX - 310f
        drawFittedRightText(
            canvas,
            schoolConfig.schoolName.ifBlank { "منصة مدرستي النموذجية" },
            textRightX,
            78f,
            maxHeaderWidth,
            headerTitlePaint
        )
        val subtitleText = if (isWalletMode) {
            "بطاقة محفظة الحانوت الذكية • ${schoolConfig.schoolStage.arabicLabel}"
        } else {
            "بطاقة الهوية المدرسية الرسمية • ${schoolConfig.schoolStage.arabicLabel}"
        }
        drawFittedRightText(canvas, subtitleText, textRightX, 126f, maxHeaderWidth, headerSubPaint)

        val cleanHeaderAddr = com.example.model.ArabicTextNormalizer.cleanDefaultAddress(schoolConfig.address)
        val yearLine = if (cleanHeaderAddr.isNotBlank()) {
            "العام الدراسي: ${schoolConfig.academicYear}  |  $cleanHeaderAddr"
        } else {
            "العام الدراسي: ${schoolConfig.academicYear}"
        }
        drawFittedRightText(canvas, yearLine, textRightX, 170f, maxHeaderWidth, headerSubPaint)

        // Left side of header: School Code badge
        val badgeRect = RectF(46f, 48f, 290f, 134f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33D4AF37
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgeBgPaint)
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgeBorderPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LIGHT_GOLD
            textSize = 24f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        drawFittedCenterText(
            canvas,
            "كود المدرسة: ${schoolConfig.schoolId.ifBlank { "100" }}",
            badgeRect.centerX(),
            badgeRect.centerY() + 9f,
            badgeRect.width() - 24f,
            badgeTextPaint
        )

        // Mode indicator pill under school code in header
        val modePillRect = RectF(46f, 146f, 290f, 192f)
        val modePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x26FFFFFF
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(modePillRect, 14f, 14f, modePillPaint)
        val modePillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        drawFittedCenterText(
            canvas,
            if (isWalletMode) "بطاقة محفظة الحانوت" else "هوية الحضور الأكاديمية",
            modePillRect.centerX(),
            modePillRect.centerY() + 7f,
            modePillRect.width() - 16f,
            modePillTextPaint
        )

        // =========================================================================
        // ZONE 1 (RIGHT): Student Photo Box + Student Username Pill (x = 965..1215)
        // =========================================================================
        val photoBoxRect = RectF(965f, 262f, 1215f, 532f)
        canvas.drawRoundRect(
            RectF(photoBoxRect.left - 5f, photoBoxRect.top - 5f, photoBoxRect.right + 5f, photoBoxRect.bottom + 5f),
            26f,
            26f,
            goldPaint
        )
        canvas.drawRoundRect(photoBoxRect, 22f, 22f, circlePaint)

        val studentPhoto = decodeBase64Bitmap(student.photoBase64)
        if (studentPhoto != null) {
            val saveCount = canvas.save()
            val clipPath = Path().apply {
                addRoundRect(photoBoxRect, 22f, 22f, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(studentPhoto, null, photoBoxRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restoreToCount(saveCount)
        } else {
            val placeholderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFE6F2EC.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(photoBoxRect, 22f, 22f, placeholderBg)
            val initials = student.fullNameWithFather.trim().take(1).ifBlank { "ط" }
            val avatarTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 104f
                typeface = boldTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initials, photoBoxRect.centerX(), photoBoxRect.centerY() + 34f, avatarTextPaint)
        }

        // Student Code Pill directly beneath Photo Box (x = 965..1215, y = 552..608)
        val codeBadgeRect = RectF(965f, 552f, 1215f, 608f)
        val codeBadgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE6F4EA.toInt()
            style = Paint.Style.FILL
        }
        val codeBadgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROYAL_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(codeBadgeRect, 16f, 16f, codeBadgeBg)
        canvas.drawRoundRect(codeBadgeRect, 16f, 16f, codeBadgeBorder)
        val codeBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_EMERALD
            textSize = 23f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        drawFittedCenterText(
            canvas,
            "كود الطالب: ${student.studentUsername}",
            codeBadgeRect.centerX(),
            codeBadgeRect.centerY() + 8f,
            codeBadgeRect.width() - 20f,
            codeBadgeTextPaint
        )

        // =========================================================================
        // ZONE 2 (CENTER-RIGHT): Student Details & Badges (strictly x = 415..930)
        // =========================================================================
        val infoRightX = 930f
        val infoLeftLimitX = 415f
        val maxInfoWidth = infoRightX - infoLeftLimitX

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_TEXT
            textSize = 24f
            typeface = regularTypeface
            textAlign = Paint.Align.RIGHT
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 36f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        val subValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROYAL_GREEN
            textSize = 29f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }

        // Row 1: Student Full Name on a single clean line
        canvas.drawText(AppLocalizationEngine.localize("اسم الطالب الكامل:"), infoRightX, 292f, labelPaint)
        drawFittedRightText(canvas, student.fullNameWithFather, infoRightX, 340f, maxInfoWidth, valuePaint)

        // Row 2: Grade & Section
        canvas.drawText(AppLocalizationEngine.localize("الصف والشعبة الدراسية:"), infoRightX, 398f, labelPaint)
        drawFittedRightText(
            canvas,
            "${student.gradeClass}  —  شعبة (${student.sectionName})",
            infoRightX,
            444f,
            maxInfoWidth,
            subValuePaint
        )

        // Row 3: Static Student Code & Parent Phone (Wallet Balance/Limit/Spent are NEVER printed on physical/saved cards)
        val cleanParentPhone = student.parentPhone.trim().takeIf {
            it.isNotEmpty() && !SecurityShieldHelper.isLegacyPlaceholderPhone(it)
        } ?: "غير مسجل"
        canvas.drawText(AppLocalizationEngine.localize("كود الطالب الموحد / هاتف ولي الأمر:"), infoRightX, 502f, labelPaint)
        drawFittedRightText(
            canvas,
            "${student.studentUsername}   |   $cleanParentPhone",
            infoRightX,
            548f,
            maxInfoWidth,
            subValuePaint
        )

        // Row 4: Badges strip strictly within x = 415..930 (NEVER touches Left QR Zone!)
        val badgeTitles = buildList {
            if (student.isOnHonorRoll && student.honorTitle.isNotBlank()) add(student.honorTitle)
            badges.forEach { add(it.title) }
            if (isEmpty()) add("وسام الالتزام الذهبي")
        }.distinct()

        val badgeStripRect = RectF(infoLeftLimitX, 582f, infoRightX, 650f)
        val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LIGHT_GOLD
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeStripRect, 18f, 18f, stripPaint)
        canvas.drawRoundRect(badgeStripRect, 18f, 18f, badgeBorderPaint)

        val stripTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_EMERALD
            textSize = 24f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        drawFittedRightText(
            canvas,
            "الأوسمة: " + badgeTitles.joinToString(" • "),
            badgeStripRect.right - 18f,
            badgeStripRect.centerY() + 9f,
            badgeStripRect.width() - 36f,
            stripTextPaint
        )

        // =========================================================================
        // ZONE 3 (LEFT): Self-Contained QR Code Card (strictly x = 46..382)
        // =========================================================================
        val qrCardRect = RectF(46f, 252f, 382f, 682f)
        val qrCardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val qrCardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(qrCardRect, 24f, 24f, qrCardBgPaint)
        canvas.drawRoundRect(qrCardRect, 24f, 24f, qrCardBorderPaint)

        val activeQrPayload = if (isWalletMode) student.walletBarcode else student.qrCodeValue
        val qrSize = 248
        val qrBitmap = generateQrCodeBitmap(activeQrPayload, qrSize)
        val qrLeft = qrCardRect.centerX() - qrSize / 2f
        val qrTop = 266f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        val qrCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_EMERALD
            textSize = 21f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        val qrCaption = if (isWalletMode) {
            "باركود محفظة الحانوت المدرسي"
        } else {
            "امسح رمز QR لتسجيل الحضور الفوري"
        }
        drawFittedCenterText(
            canvas,
            qrCaption,
            qrCardRect.centerX(),
            544f,
            qrCardRect.width() - 24f,
            qrCaptionPaint
        )

        // Sleek inner pill for the QR code string (MADRASATI|... or WALLET-...)
        val codePillRect = RectF(60f, 562f, 368f, 610f)
        val codePillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OFF_WHITE
            style = Paint.Style.FILL
        }
        val codePillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x330F4C35
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(codePillRect, 12f, 12f, codePillBg)
        canvas.drawRoundRect(codePillRect, 12f, 12f, codePillBorder)

        val qrIdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        drawFittedCenterText(
            canvas,
            activeQrPayload,
            codePillRect.centerX(),
            codePillRect.centerY() + 6f,
            codePillRect.width() - 20f,
            qrIdPaint
        )

        val qrSubNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_TEXT
            textSize = 18f
            typeface = regularTypeface
            textAlign = Paint.Align.CENTER
        }
        drawFittedCenterText(
            canvas,
            if (isWalletMode) "صالح للدفع الإلكتروني في الحانوت" else "بصمة حضور ذكية معتمدة للمدرسة",
            qrCardRect.centerX(),
            650f,
            qrCardRect.width() - 24f,
            qrSubNotePaint
        )

        // Bottom official footer bar
        val footerTop = height - 92f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_EMERALD
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, footerTop, width.toFloat(), height.toFloat(), footerPaint)
        canvas.drawRect(0f, footerTop, width.toFloat(), footerTop + 5f, goldPaint)

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 23f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        val cleanSchoolPhone = schoolConfig.phone.trim().takeIf {
            it.isNotEmpty() && !SecurityShieldHelper.isLegacyPlaceholderPhone(it)
        }
        val footerText = if (cleanSchoolPhone != null) {
            "هوية مدرسية معتمدة صادرة عبر منصة مدرستي الذكية  •  هاتف الإدارة: $cleanSchoolPhone"
        } else {
            "هوية مدرسية معتمدة صادرة عبر منصة مدرستي الذكية  •  العام الدراسي: ${schoolConfig.academicYear}"
        }
        drawFittedCenterText(
            canvas,
            footerText,
            width / 2f,
            footerTop + 54f,
            width - 90f,
            footerTextPaint
        )

        // Outer luxury frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        canvas.drawRoundRect(RectF(4f, 4f, width - 4f, height - 4f), 36f, 36f, framePaint)

        return bitmap
    }

    /**
     * Saves the rendered high-resolution Student ID card as a PNG image in the device gallery (MediaStore).
     */
    fun saveStudentIdCardAsHighResPng(
        context: Context,
        student: StudentRecord,
        schoolConfig: SchoolConfig,
        badges: List<StudentAutoBadge> = emptyList(),
        isWalletMode: Boolean = activeWalletPassExportMode
    ): Result<String> {
        return try {
            runCatching {
                context.getSharedPreferences("madrasati_multitenant_v2", Context.MODE_PRIVATE)
                    .edit()
                    .putString("last_viewed_or_saved_student_qr", student.qrCodeValue)
                    .putString("last_viewed_or_saved_student_id", student.id)
                    .apply()
            }
            val bitmap = renderStudentIdCardBitmap(student, schoolConfig, badges, isWalletMode, context)
            val safeName = student.fullNameWithFather.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
            val modePrefix = if (isWalletMode) "Wallet" else "ID"
            val fileName = "Madrasati_${modePrefix}_${safeName}_${student.id}_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Madrasati_IDs")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("تعذر إنشاء ملف الصورة في المعرض"))
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.success("Pictures/Madrasati_IDs/$fileName")
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Madrasati_IDs")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Result.success(file.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports the Student ID card as a standard printable PDF document and saves it to Downloads
     * (and opens Android PrintManager if available).
     */
    fun exportStudentIdCardPdf(
        context: Context,
        student: StudentRecord,
        schoolConfig: SchoolConfig,
        badges: List<StudentAutoBadge> = emptyList(),
        isWalletMode: Boolean = activeWalletPassExportMode
    ): Result<String> {
        return try {
            val idBitmap = renderStudentIdCardBitmap(student, schoolConfig, badges, isWalletMode, context)
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val boldTypeface = loadCairoTypeface(context, Typeface.BOLD)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 18f
                typeface = boldTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                if (isWalletMode) {
                    "بطاقة محفظة الحانوت المدرسية الذكية — جاهزة للطباعة أو الكبس الحراري"
                } else {
                    "بطاقة الهوية المدرسية الذكية — جاهزة للطباعة أو الكبس الحراري"
                },
                595 / 2f,
                65f,
                titlePaint
            )

            // Draw standard ID card size on A4 sheet
            val cardDst = RectF(55f, 100f, 540f, 410f)
            canvas.drawBitmap(idBitmap, null, cardDst, Paint(Paint.FILTER_BITMAP_FLAG))

            // Draw cut guide around card
            val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_MUTED_TEXT
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(RectF(47f, 92f, 548f, 418f), 18f, 18f, dashedPaint)

            // Back of card instructions box below
            val backRect = RectF(55f, 455f, 540f, 740f)
            val backBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_OFF_WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(backRect, 16f, 16f, backBgPaint)
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_GOLD
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRoundRect(backRect, 16f, 16f, borderPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DARK_TEXT
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val rightX = backRect.right - 24f
            canvas.drawText("تعليمات استخدام البطاقة المدرسية الذكية (الوجه الخلفي):", rightX, 495f, textPaint)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("1. يحمل الطالب هذه الهوية يومياً لإبرازها عند بوابة المدرسة وعند تسجيل الحضور بالـ QR.", rightX, 528f, textPaint)
            canvas.drawText("2. عند مسح رمز الـ QR يصل إشعار فوري لولي الأمر بلحظة دخول الطالب للمدرسة.", rightX, 556f, textPaint)
            canvas.drawText("3. كود الطالب: ${student.studentUsername}  |  كود ولي الأمر: ${student.parentUsername}", rightX, 584f, textPaint)
            canvas.drawText("4. في حال العثور على هذه البطاقة يرجى تسليمها إلى إدارة (${schoolConfig.schoolName}).", rightX, 612f, textPaint)
            canvas.drawText("5. رقم هاتف الإدارة للتواصل: ${schoolConfig.phone}  —  العنوان: ${schoolConfig.address}", rightX, 640f, textPaint)

            pdfDocument.finishPage(page)

            val safeName = student.fullNameWithFather.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
            val fileName = "Student_ID_${safeName}_${System.currentTimeMillis()}.pdf"
            val savedPath = savePdfAndTriggerPrint(
                context = context,
                pdfDocument = pdfDocument,
                fileName = fileName,
                jobName = "هوية الطالب - ${student.fullNameWithFather}"
            )
            pdfDocument.close()
            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports an official Student Report Card (بطاقة الدرجات / الشهادة المدرسية) with official double border,
     * 3-column ministry header, 4 summary KPI boxes, full grid table with pass/fail indicators, and 3-box signature/stamp footer.
     */
    fun exportStudentReportCardPdf(
        context: Context,
        student: StudentRecord,
        grades: List<SubjectGrade>,
        schoolConfig: SchoolConfig,
        principalName: String = "إدارة المدرسة",
        periodLabel: String = "الكارت الشامل لجميع الفصول"
    ): Result<String> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            drawOfficialPdfHeader(
                canvas = canvas,
                schoolConfig = schoolConfig,
                documentTitle = "بطاقة الدرجات الرسمية ($periodLabel)",
                subtitle = "الطالب: ${student.fullNameWithFather}  •  الصف: ${student.gradeClass} (${student.sectionName})",
                periodLabel = periodLabel,
                docNumber = "RC-${student.id.takeLast(4).uppercase()}"
            )

            val passedCount = grades.count { g ->
                val minPass = if (g.maxScoreScale <= 10) 5 else 50
                g.finalAverage >= minPass
            }
            val failedCount = (grades.size - passedCount).coerceAtLeast(0)
            val overallAvg = if (grades.isNotEmpty()) grades.map { it.normalizedPercentage }.average().toInt() else 0
            val finalDecision = if (grades.isEmpty()) "قيد الرصد" else if (failedCount == 0) "ناجح ومتفوق" else "مكمل ($failedCount مادة)"

            drawPdfFourKpiStrip(
                canvas = canvas,
                topY = 148f,
                kpis = listOf(
                    PdfKpiBoxData("عدد المواد الدراسية", "${grades.size} مادة", 0xFF065F46.toInt(), 0xFFECFDF5.toInt()),
                    PdfKpiBoxData("المواد الناجح بها", "$passedCount مادة", 0xFF15803D.toInt(), 0xFFF0FDF4.toInt()),
                    PdfKpiBoxData("المعدل العام المئوي", "$overallAvg%", 0xFFD97706.toInt(), 0xFFFFFBEB.toInt()),
                    PdfKpiBoxData(
                        "النتيجة النهائية",
                        finalDecision,
                        if (failedCount == 0) 0xFF047857.toInt() else 0xFFB91C1C.toInt(),
                        if (failedCount == 0) 0xFFECFDF5.toInt() else 0xFFFEF2F2.toInt()
                    )
                )
            )

            // Structured Multi-Column Grid Table
            val cols = floatArrayOf(560f, 534f, 428f, 386f, 344f, 302f, 250f, 198f, 146f, 92f, 35f)
            val headers = listOf("ت", "المادة الدراسية", "يومي", "ش1", "ش2", "نصف السنة", "السعي", "النهائي", "المعدل", "النتيجة")

            val rows = grades.mapIndexed { idx, g ->
                val minPass = if (g.maxScoreScale <= 10) 5 else 50
                val isPass = g.finalAverage >= minPass
                listOf(
                    (idx + 1).toString(),
                    g.subjectName,
                    g.dailyScore.toString(),
                    g.month1Score.toString(),
                    g.month2Score.toString(),
                    g.midYearScore.toString(),
                    g.annualEffortScore.toString(),
                    g.finalExamScore.toString(),
                    "${g.finalAverage}/${g.maxScoreScale}",
                    if (isPass) "ناجح (${g.ratingLabel})" else "راسب / يحتاج دعم"
                )
            }

            drawStructuredPdfGridTable(
                canvas = canvas,
                topY = 212f,
                cols = cols,
                headers = headers,
                rows = rows.ifEmpty {
                    listOf(listOf("-", "لم يتم رصد درجات المواد حتى الآن", "-", "-", "-", "-", "-", "-", "-", "قيد الرصد"))
                },
                rowHeight = 25f,
                rightAlignColIndex = 1
            )

            drawOfficialPdfFooter(canvas, schoolConfig, principalName, pageNumber = 1, totalPages = 1)
            pdfDocument.finishPage(page)

            val safeName = student.fullNameWithFather.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
            val fileName = "ReportCard_${safeName}_${System.currentTimeMillis()}.pdf"
            val savedPath = savePdfAndTriggerPrint(
                context = context,
                pdfDocument = pdfDocument,
                fileName = fileName,
                jobName = "بطاقة الدرجات - ${student.fullNameWithFather}"
            )
            pdfDocument.close()
            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports the Monthly Attendance & Absence Report (تصدير كشف الغياب الشهري) as an official tabular PDF.
     */
    fun exportMonthlyAttendanceReportPdf(
        context: Context,
        schoolConfig: SchoolConfig,
        gradeClass: String,
        sectionName: String,
        monthPrefix: String,
        students: List<StudentRecord>,
        attendanceList: List<AttendanceEntry>,
        principalName: String = "إدارة المدرسة"
    ): Result<String> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val cleanMonth = monthPrefix.ifBlank { "كامل العام الدراسي" }
            drawOfficialPdfHeader(
                canvas = canvas,
                schoolConfig = schoolConfig,
                documentTitle = "كشف الحضور والغياب الشهري الرسمي",
                subtitle = "الصف: $gradeClass — شعبة ($sectionName)  •  الفترة: $cleanMonth",
                periodLabel = cleanMonth,
                docNumber = "AT-${System.currentTimeMillis() % 10000}"
            )

            val relevantRecords = attendanceList.filter { rec ->
                students.any { it.id == rec.studentId } && (monthPrefix.isBlank() || rec.date.startsWith(monthPrefix))
            }
            val totalPresent = relevantRecords.count { it.status == AttendanceStatus.PRESENT }
            val totalAbsent = relevantRecords.count { it.status == AttendanceStatus.ABSENT }
            val totalExcused = relevantRecords.count { it.status == AttendanceStatus.EXCUSED || it.status == AttendanceStatus.LATE }
            val attendanceRate = if (relevantRecords.isEmpty()) 100 else ((totalPresent + totalExcused) * 100) / relevantRecords.size.coerceAtLeast(1)

            drawPdfFourKpiStrip(
                canvas = canvas,
                topY = 148f,
                kpis = listOf(
                    PdfKpiBoxData("إجمالي طلاب الشعبة", "${students.size} طالب", 0xFF065F46.toInt(), 0xFFECFDF5.toInt()),
                    PdfKpiBoxData("نسبة المواظبة العامة", "$attendanceRate%", 0xFF15803D.toInt(), 0xFFF0FDF4.toInt()),
                    PdfKpiBoxData("مجموع أيام الحضور", "$totalPresent يوم", 0xFFD97706.toInt(), 0xFFFFFBEB.toInt()),
                    PdfKpiBoxData("مجموع الغياب والإجازات", "${totalAbsent + totalExcused} يوم", 0xFFB91C1C.toInt(), 0xFFFEF2F2.toInt())
                )
            )

            val cols = floatArrayOf(560f, 530f, 345f, 270f, 200f, 130f, 35f)
            val headers = listOf("ت", "اسم الطالب الكامل", "أيام الحضور", "أيام الغياب", "تأخر / مجاز", "نسبة الالتزام")
            val rows = students.take(20).mapIndexed { idx, stu ->
                val stuRecords = attendanceList.filter {
                    it.studentId == stu.id && (monthPrefix.isBlank() || it.date.startsWith(monthPrefix))
                }
                val presentCount = stuRecords.count { it.status == AttendanceStatus.PRESENT }
                val absentCount = stuRecords.count { it.status == AttendanceStatus.ABSENT }
                val lateOrExcused = stuRecords.count { it.status == AttendanceStatus.LATE || it.status == AttendanceStatus.EXCUSED }
                val stuRate = if (stuRecords.isEmpty()) 100 else ((presentCount + lateOrExcused) * 100) / stuRecords.size.coerceAtLeast(1)
                listOf(
                    (idx + 1).toString(),
                    stu.fullNameWithFather,
                    presentCount.toString(),
                    absentCount.toString(),
                    lateOrExcused.toString(),
                    "$stuRate%"
                )
            }

            drawStructuredPdfGridTable(
                canvas = canvas,
                topY = 212f,
                cols = cols,
                headers = headers,
                rows = rows.ifEmpty {
                    listOf(listOf("-", "لا توجد سجلات طلاب في هذه الشعبة", "0", "0", "0", "100%"))
                },
                rowHeight = 24f,
                rightAlignColIndex = 1
            )

            drawOfficialPdfFooter(canvas, schoolConfig, principalName, pageNumber = 1, totalPages = 1)
            pdfDocument.finishPage(page)

            val fileName = "Monthly_Attendance_${System.currentTimeMillis()}.pdf"
            val savedPath = savePdfAndTriggerPrint(
                context = context,
                pdfDocument = pdfDocument,
                fileName = fileName,
                jobName = "كشف الغياب الشهري - $gradeClass"
            )
            pdfDocument.close()
            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports the Tuition & Installments Statement (تصدير كشف الأقساط الدراسية) as an official tabular PDF.
     */
    fun exportTuitionStatementPdf(
        context: Context,
        schoolConfig: SchoolConfig,
        tuitions: List<TuitionRecord>,
        principalName: String = "إدارة المدرسة"
    ): Result<String> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            drawOfficialPdfHeader(
                canvas = canvas,
                schoolConfig = schoolConfig,
                documentTitle = "كشف الأقساط والدفعات المالية الرسمي",
                subtitle = "إجمالي السجلات: ${tuitions.size} طالب  •  العام الدراسي: ${schoolConfig.academicYear}",
                periodLabel = schoolConfig.academicYear,
                docNumber = "FIN-${System.currentTimeMillis() % 10000}"
            )

            val fmt = NumberFormat.getNumberInstance(Locale.US)
            val totalFeeSum = tuitions.sumOf { it.totalFee }
            val paidSum = tuitions.sumOf { it.paidAmount }
            val remSum = tuitions.sumOf { it.remainingAmount }

            drawPdfFourKpiStrip(
                canvas = canvas,
                topY = 148f,
                kpis = listOf(
                    PdfKpiBoxData("إجمالي الطلبة المسجلين", "${tuitions.size} طالب", 0xFF065F46.toInt(), 0xFFECFDF5.toInt()),
                    PdfKpiBoxData("مجموع الأقساط الكلي", "${fmt.format(totalFeeSum)} د.ع", 0xFF1E3A8A.toInt(), 0xFFEFF6FF.toInt()),
                    PdfKpiBoxData("المبالغ المستلمة", "${fmt.format(paidSum)} د.ع", 0xFF15803D.toInt(), 0xFFF0FDF4.toInt()),
                    PdfKpiBoxData("المبالغ المتبقية", "${fmt.format(remSum)} د.ع", 0xFFD97706.toInt(), 0xFFFFFBEB.toInt())
                )
            )

            val cols = floatArrayOf(560f, 532f, 380f, 290f, 198f, 108f, 35f)
            val headers = listOf("ت", "اسم الطالب", "الصف والشعبة", "القسط الكلي", "المدفوع", "المتبقي")
            val rows = tuitions.take(19).mapIndexed { idx, rec ->
                listOf(
                    (idx + 1).toString(),
                    rec.studentName,
                    "${rec.gradeClass} (${rec.sectionName})",
                    "${fmt.format(rec.totalFee)} د.ع",
                    "${fmt.format(rec.paidAmount)} د.ع",
                    "${fmt.format(rec.remainingAmount)} د.ع"
                )
            }

            drawStructuredPdfGridTable(
                canvas = canvas,
                topY = 212f,
                cols = cols,
                headers = headers,
                rows = rows.ifEmpty {
                    listOf(listOf("-", "لا توجد سجلات أقساط مسجلة", "-", "0 د.ع", "0 د.ع", "0 د.ع"))
                },
                rowHeight = 24f,
                rightAlignColIndex = 1
            )

            drawOfficialPdfFooter(canvas, schoolConfig, principalName, pageNumber = 1, totalPages = 1)
            pdfDocument.finishPage(page)

            val fileName = "Tuition_Statement_${System.currentTimeMillis()}.pdf"
            val savedPath = savePdfAndTriggerPrint(
                context = context,
                pdfDocument = pdfDocument,
                fileName = fileName,
                jobName = "كشف الأقساط الدراسية - ${schoolConfig.schoolName}"
            )
            pdfDocument.close()
            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class PdfKpiBoxData(
        val label: String,
        val value: String,
        val accentColor: Int,
        val bgColor: Int
    )

    /**
     * Draws the Royal Official Double Border around the A4 page + 3-column Ministry/School Header + Prominent Report Badge.
     */
    private fun drawOfficialPdfHeader(
        canvas: Canvas,
        schoolConfig: SchoolConfig,
        documentTitle: String,
        subtitle: String,
        periodLabel: String = schoolConfig.academicYear,
        docNumber: String = "DOC-${(System.currentTimeMillis() / 1000) % 100000}"
    ) {
        // 1. Official Double Border around entire A4 page (595 x 842)
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF064E3B.toInt() // Royal Emerald #064E3B
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt() // Royal Gold #D97706
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(RectF(16f, 16f, 579f, 826f), 10f, 10f, outerBorderPaint)
        canvas.drawRoundRect(RectF(21f, 21f, 574f, 821f), 7f, 7f, innerBorderPaint)

        // 2. Top Header Banner inside Double Border
        val headerRect = RectF(26f, 26f, 569f, 138f)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left, headerRect.top, headerRect.right, headerRect.bottom,
                0xFF064E3B.toInt(), 0xFF065F46.toInt(), Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 8f, 8f, headerPaint)

        val goldLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(28f, 134f, 567f, 138f, goldLinePaint)

        // Right Column (School Name, Academic Year, Ministry Affiliation)
        val rightColPaintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val rightColPaintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFDE68A.toInt()
            textSize = 9.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(AppLocalizationEngine.localize("جمهورية العراق • وزارة التربية"), 558f, 46f, rightColPaintSub)
        canvas.drawText(AppLocalizationEngine.localize(schoolConfig.schoolName.ifBlank { "مدرستي الذكية النموذجية" }), 558f, 66f, rightColPaintBold)
        canvas.drawText(AppLocalizationEngine.localize("العام الدراسي: ${schoolConfig.academicYear}"), 558f, 84f, rightColPaintSub)

        // Left Column (Issue Date, Document Number, Period)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val leftColPaintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val leftColPaintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFDE68A.toInt()
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(AppLocalizationEngine.localize("تاريخ الإصدار: $dateStr"), 36f, 46f, leftColPaintBold)
        canvas.drawText(AppLocalizationEngine.localize("رقم الوثيقة: $docNumber"), 36f, 65f, leftColPaintSub)
        canvas.drawText(AppLocalizationEngine.localize("الفترة: ${periodLabel.take(22)}"), 36f, 84f, leftColPaintSub)

        // Center Column (School Emblem/Logo + Prominent Report Title Badge)
        val logo = decodeBase64Bitmap(schoolConfig.logoBase64)
        val centerX = 297.5f
        val emblemCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
            style = Paint.Style.FILL
        }
        val emblemStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawCircle(centerX, 55f, 22f, emblemCirclePaint)
        canvas.drawCircle(centerX, 55f, 22f, emblemStrokePaint)

        if (logo != null) {
            val dst = RectF(centerX - 18f, 37f, centerX + 18f, 73f)
            canvas.drawBitmap(logo, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            val emblemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFDE68A.toInt()
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(AppLocalizationEngine.localize("شعار"), centerX, 53f, emblemTextPaint)
            canvas.drawText(AppLocalizationEngine.localize("المدرسة"), centerX, 65f, emblemTextPaint)
        }

        // Prominent Center Title Badge inside Header
        val badgeRect = RectF(75f, 92f, 520f, 128f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF042F2E.toInt()
            style = Paint.Style.FILL
        }
        val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.3f
        }
        canvas.drawRoundRect(badgeRect, 8f, 8f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 8f, 8f, badgeStrokePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFDE68A.toInt()
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(AppLocalizationEngine.localize(documentTitle), centerX, 108f, titlePaint)
        canvas.drawText(AppLocalizationEngine.localize(subtitle), centerX, 122f, subtitlePaint)
    }

    /**
     * Draws 4 colored KPI summary boxes side-by-side in RTL order.
     */
    private fun drawPdfFourKpiStrip(
        canvas: Canvas,
        topY: Float,
        kpis: List<PdfKpiBoxData>
    ) {
        if (kpis.isEmpty()) return
        val leftMargin = 35f
        val rightMargin = 560f
        val gap = 8f
        val totalWidth = rightMargin - leftMargin
        val boxWidth = (totalWidth - gap * 3f) / 4f
        val boxHeight = 52f

        kpis.take(4).forEachIndexed { idx, kpi ->
            // RTL placement: index 0 on rightmost
            val boxRight = rightMargin - idx * (boxWidth + gap)
            val boxLeft = boxRight - boxWidth
            val rect = RectF(boxLeft, topY, boxRight, topY + boxHeight)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = kpi.bgColor
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = kpi.accentColor
                style = Paint.Style.STROKE
                strokeWidth = 1.4f
            }
            val topBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = kpi.accentColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
            canvas.drawRoundRect(RectF(boxLeft, topY, boxRight, topY + 5f), 4f, 4f, topBarPaint)

            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = kpi.accentColor
                textSize = 13.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val lblPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DARK_TEXT
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            drawFittedCenterText(canvas, kpi.value, rect.centerX(), topY + 25f, boxWidth - 10f, valPaint)
            drawFittedCenterText(canvas, kpi.label, rect.centerX(), topY + 42f, boxWidth - 10f, lblPaint)
        }
    }

    /**
     * Draws a full structured multi-column grid table with Dark Emerald header (#065F46),
     * Zebra-striped rows (#FFFFFF & #F0FDF4), and full horizontal + vertical cell grid borders.
     */
    private fun drawStructuredPdfGridTable(
        canvas: Canvas,
        topY: Float,
        cols: FloatArray,
        headers: List<String>,
        rows: List<List<String>>,
        rowHeight: Float = 24f,
        rightAlignColIndex: Int = -1
    ): Float {
        val tableRight = cols.first()
        val tableLeft = cols.last()
        val headerHeight = 28f

        // Table Header Background (#065F46)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF065F46.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(tableLeft, topY, tableRight, topY + headerHeight, headerBgPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val gridBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        val headerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10B981.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }

        for (i in headers.indices) {
            val colRight = cols[i]
            val colLeft = cols[i + 1]
            val cx = (colRight + colLeft) / 2f
            val colW = (colRight - colLeft).coerceAtLeast(16f)
            drawFittedCenterText(canvas, headers[i], cx, topY + 18f, colW - 4f, headerTextPaint)
            if (i > 0) {
                canvas.drawLine(colRight, topY, colRight, topY + headerHeight, headerDividerPaint)
            }
        }

        var currentY = topY + headerHeight
        val maxBottomY = 710f

        rows.forEachIndexed { rowIdx, cells ->
            if (currentY + rowHeight > maxBottomY) return@forEachIndexed

            val rowBgColor = if (rowIdx % 2 == 0) Color.WHITE else 0xFFF0FDF4.toInt()
            val rowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = rowBgColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, rowBgPaint)

            for (i in cells.indices) {
                if (i + 1 >= cols.size) break
                val colRight = cols[i]
                val colLeft = cols[i + 1]
                val colW = (colRight - colLeft).coerceAtLeast(14f)
                val cellText = cells[i]

                val isWarningCell = cellText.contains("راسب") || cellText.contains("مفصول") || cellText.contains("يحتاج دعم")
                val isSuccessCell = cellText.contains("ناجح") || cellText.contains("مستمر") || cellText.contains("مرحّل")

                val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = when {
                        isWarningCell -> 0xFFB91C1C.toInt()
                        isSuccessCell -> 0xFF065F46.toInt()
                        else -> COLOR_DARK_TEXT
                    }
                    textSize = 9.5f
                    typeface = Typeface.create(
                        Typeface.DEFAULT,
                        if (i == 0 || i == rightAlignColIndex || isWarningCell || isSuccessCell) Typeface.BOLD else Typeface.NORMAL
                    )
                    textAlign = Paint.Align.CENTER
                }

                val cx = (colRight + colLeft) / 2f
                drawFittedCenterText(canvas, cellText, cx, currentY + (rowHeight * 0.66f), colW - 6f, cellPaint)

                // Vertical cell line
                canvas.drawLine(colRight, currentY, colRight, currentY + rowHeight, gridBorderPaint)
            }
            // Leftmost vertical line & horizontal bottom line
            canvas.drawLine(tableLeft, currentY, tableLeft, currentY + rowHeight, gridBorderPaint)
            canvas.drawLine(tableLeft, currentY + rowHeight, tableRight, currentY + rowHeight, gridBorderPaint)
            currentY += rowHeight
        }

        // Outer table frame
        val outerTablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF064E3B.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }
        canvas.drawRect(tableLeft, topY, tableRight, currentY, outerTablePaint)
        return currentY
    }

    /**
     * Draws the Official 3-Box Footer (Counselor/Deputy Signature on Right, Official School Stamp in Center,
     * Principal Approval on Left) + Page Numbering.
     */
    private fun drawOfficialPdfFooter(
        canvas: Canvas,
        schoolConfig: SchoolConfig,
        principalName: String,
        pageNumber: Int = 1,
        totalPages: Int = 1
    ) {
        val footerTop = 724f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt()
            strokeWidth = 1.6f
        }
        canvas.drawLine(35f, footerTop, 560f, footerTop, linePaint)

        val boxTop = footerTop + 8f
        val boxBottom = 796f
        val boxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF8FAFC.toInt()
            style = Paint.Style.FILL
        }
        val boxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFCBD5E1.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF064E3B.toInt()
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_TEXT
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // 1. Right Box: توقيع المرشد / المعاون
        val rightBox = RectF(395f, boxTop, 560f, boxBottom)
        canvas.drawRoundRect(rightBox, 8f, 8f, boxBgPaint)
        canvas.drawRoundRect(rightBox, 8f, 8f, boxBorderPaint)
        canvas.drawText(AppLocalizationEngine.localize("توقيع المرشد / المعاون الإداري"), rightBox.centerX(), boxTop + 18f, headerTextPaint)
        canvas.drawText("...........................", rightBox.centerX(), boxTop + 44f, subTextPaint)

        // 2. Center Box: الختم الرسمي للمدرسة (Circular/Box Stamp Frame)
        val centerBox = RectF(215f, boxTop, 380f, boxBottom)
        canvas.drawRoundRect(centerBox, 8f, 8f, boxBgPaint)
        val stampDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD97706.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.3f
        }
        canvas.drawRoundRect(centerBox, 8f, 8f, stampDashPaint)
        canvas.drawCircle(centerBox.centerX(), centerBox.centerY() + 6f, 20f, stampDashPaint)
        canvas.drawText(AppLocalizationEngine.localize("الختم الرسمي للمدرسة"), centerBox.centerX(), boxTop + 16f, headerTextPaint)
        canvas.drawText(AppLocalizationEngine.localize("معتمد إلكترونياً"), centerBox.centerX(), centerBox.centerY() + 9f, subTextPaint)

        // 3. Left Box: مصادقة مدير المدرسة: [اسم المدير]
        val leftBox = RectF(35f, boxTop, 200f, boxBottom)
        canvas.drawRoundRect(leftBox, 8f, 8f, boxBgPaint)
        canvas.drawRoundRect(leftBox, 8f, 8f, boxBorderPaint)
        val cleanPrincipal = principalName.ifBlank { "إدارة المدرسة" }
        drawFittedCenterText(canvas, "مصادقة مدير المدرسة: $cleanPrincipal", leftBox.centerX(), boxTop + 18f, leftBox.width() - 12f, headerTextPaint)
        canvas.drawText(AppLocalizationEngine.localize("التوقيع الرسمي: مصادق ومعتمد"), leftBox.centerX(), boxTop + 44f, subTextPaint)

        // Bottom Page Numbering & School Address Bar
        val pageNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF064E3B.toInt()
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            AppLocalizationEngine.localize("صفحة $pageNumber من $totalPages   •   ${schoolConfig.schoolName} (${schoolConfig.address})"),
            297.5f,
            812f,
            pageNumPaint
        )
    }

    private fun savePdfAndTriggerPrint(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String,
        jobName: String
    ): String {
        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { out ->
            pdfDocument.writeTo(out)
        }

        var savedDisplayLocation = cacheFile.absolutePath
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Madrasati_Reports")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        pdfDocument.writeTo(out)
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    savedDisplayLocation = "Downloads/Madrasati_Reports/$fileName"
                }
            }
        } catch (_: Exception) {
            // Keep cacheFile fallback
        }

        // Also open Android system Print / Save-as-PDF preview if PrintManager is available
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val adapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(fileName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            if (destination != null) {
                                FileInputStream(cacheFile).use { input ->
                                    FileOutputStream(destination.fileDescriptor).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        } catch (_: Exception) {
            // Print dialog is optional when running headless or without Activity context
        }

        return savedDisplayLocation
    }

    private fun decodeBase64Bitmap(base64Str: String): Bitmap? {
        if (base64Str.isBlank()) return null
        return try {
            val clean = base64Str.substringAfter("base64,", base64Str)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves any Base64 image from the Global Image Lightbox directly to the user's device gallery (MediaStore).
     */
    fun saveBase64ImageToGallery(
        context: Context,
        base64Data: String,
        title: String = "Madrasati_Photo",
        titlePrefix: String = title
    ): Result<String> {
        return try {
            val bitmap = decodeBase64Bitmap(base64Data)
                ?: return Result.failure(IllegalArgumentException("الصورة غير صالحة للحفظ"))
            val safeTitle = titlePrefix.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").ifBlank { "Madrasati_Photo" }
            val fileName = "${safeTitle}_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Madrasati_Images")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("تعذر إنشاء ملف الصورة في المعرض"))
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                Result.success("تم حفظ الصورة في المعرض: Pictures/Madrasati_Images/$fileName")
            } else {
                val outFile = File(context.cacheDir, fileName)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                Result.success("تم حفظ الصورة: ${outFile.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports a luxury dated Digital Tuition Receipt (وصل قبض رقمي رسمي) for a single installment as PDF & opens Print/Save dialog.
     */
    fun exportSingleInstallmentReceiptPdf(
        context: Context,
        schoolConfig: SchoolConfig,
        tuitionRecord: TuitionRecord,
        installment: TuitionInstallment,
        principalName: String = "إدارة المدرسة"
    ): Result<String> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)
            drawOfficialPdfHeader(
                canvas = canvas,
                schoolConfig = schoolConfig,
                documentTitle = "وصل قبض قسط دراسي رقمي رسمي",
                subtitle = "رقم الوصل: ${installment.receiptNumber} • التاريخ: ${installment.paymentDate}",
                periodLabel = installment.paymentDate,
                docNumber = installment.receiptNumber
            )

            val fmt = NumberFormat.getNumberInstance(Locale.US)
            drawPdfFourKpiStrip(
                canvas = canvas,
                topY = 148f,
                kpis = listOf(
                    PdfKpiBoxData("مبلغ الدفعة المستلمة", "${fmt.format(installment.amount)} د.ع", 0xFF065F46.toInt(), 0xFFECFDF5.toInt()),
                    PdfKpiBoxData("إجمالي القسط السنوي", "${fmt.format(tuitionRecord.totalFee)} د.ع", 0xFF1E3A8A.toInt(), 0xFFEFF6FF.toInt()),
                    PdfKpiBoxData("مجموع الواصل", "${fmt.format(tuitionRecord.paidAmount)} د.ع", 0xFF15803D.toInt(), 0xFFF0FDF4.toInt()),
                    PdfKpiBoxData("المتبقي في الذمة", "${fmt.format(tuitionRecord.remainingAmount)} د.ع", 0xFFD97706.toInt(), 0xFFFFFBEB.toInt())
                )
            )

            val cols = floatArrayOf(560f, 380f, 35f)
            val headers = listOf("البيان الرسمي للوصل", "التفاصيل المعتمدة")
            val rows = listOf(
                listOf("رقم وصل القبض الرسمي", installment.receiptNumber),
                listOf("تاريخ تسديد الدفعة", installment.paymentDate),
                listOf("اسم الطالب الكامل", tuitionRecord.studentName),
                listOf("الصف والشعبة", "${tuitionRecord.gradeClass} (${tuitionRecord.sectionName})"),
                listOf("اسم ولي الأمر", tuitionRecord.parentName),
                listOf("مبلغ الدفعة المستلمة", "${fmt.format(installment.amount)} دينار عراقي"),
                listOf("ملاحظات الدفعة", installment.note.ifBlank { "دفعة قسط دراسي معتمدة" }),
                listOf("إجمالي القسط السنوي", "${fmt.format(tuitionRecord.totalFee)} د.ع"),
                listOf("مجموع المبالغ الواصلة", "${fmt.format(tuitionRecord.paidAmount)} د.ع"),
                listOf("المبلغ المتبقي في الذمة", "${fmt.format(tuitionRecord.remainingAmount)} د.ع")
            )

            val tableBottom = drawStructuredPdfGridTable(
                canvas = canvas,
                topY = 212f,
                cols = cols,
                headers = headers,
                rows = rows,
                rowHeight = 28f,
                rightAlignColIndex = 0
            )

            // Stamp QR verification on receipt
            val receiptQrPayload = "RECEIPT|${schoolConfig.schoolId}|${installment.receiptNumber}|${tuitionRecord.studentName}|${installment.amount}|${installment.paymentDate}"
            val qrBmp = generateQrCodeBitmap(receiptQrPayload, 115)
            canvas.drawBitmap(qrBmp, 45f, (tableBottom + 16f).coerceAtMost(595f), null)

            val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("وصل رقمي موثق بختم QR من إدارة (${schoolConfig.schoolName})", 550f, (tableBottom + 55f).coerceAtMost(640f), stampPaint)

            drawOfficialPdfFooter(canvas, schoolConfig, principalName, pageNumber = 1, totalPages = 1)
            pdfDocument.finishPage(page)

            val fileName = "Tuition_Receipt_${installment.receiptNumber}_${System.currentTimeMillis()}.pdf"
            val path = savePdfAndTriggerPrint(
                context = context,
                pdfDocument = pdfDocument,
                fileName = fileName,
                jobName = "وصل قبض ${installment.receiptNumber} - ${tuitionRecord.studentName}"
            )
            pdfDocument.close()
            Result.success(path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports the Comprehensive Student Archive Report (السجل الشامل للطالب - الأرشيف الشهري والسنوي) as an official tabular PDF.
     */
    fun exportComprehensiveStudentArchivePdf(
        context: Context,
        schoolConfig: SchoolConfig,
        student: StudentRecord,
        monthLabel: String = "الكل (السنة كاملة)",
        attendanceMap: Map<String, AttendanceStatus> = emptyMap(),
        leaveRequests: List<LeaveRequestItem> = emptyList(),
        exams: List<ExamScheduleItem> = emptyList(),
        grades: List<SubjectGrade> = emptyList(),
        behaviors: List<DailyBehaviorLogItem> = emptyList(),
        principalName: String = "إدارة المدرسة"
    ): Result<String> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val presentCount = attendanceMap.values.count { it == AttendanceStatus.PRESENT }
            val absentCount = attendanceMap.values.count { it == AttendanceStatus.ABSENT }
            val lateCount = attendanceMap.values.count { it == AttendanceStatus.LATE }
            val excusedCount = attendanceMap.values.count { it == AttendanceStatus.EXCUSED }.coerceAtLeast(leaveRequests.size)
            val avgGrade = if (grades.isNotEmpty()) grades.map { it.normalizedPercentage }.average().toInt() else 0

            drawOfficialPdfHeader(
                canvas = canvas,
                schoolConfig = schoolConfig,
                documentTitle = "السجل الشامل للطالب (الأرشيف الشهري والسنوي)",
                subtitle = "الطالب: ${student.fullNameWithFather}  •  الصف: ${student.gradeClass} (${student.sectionName})",
                periodLabel = monthLabel,
                docNumber = "ARC-${student.id.takeLast(4).uppercase()}"
            )

            drawPdfFourKpiStrip(
                canvas = canvas,
                topY = 148f,
                kpis = listOf(
                    PdfKpiBoxData("أيام الحضور الفعلية", "$presentCount يوم", 0xFF065F46.toInt(), 0xFFECFDF5.toInt()),
                    PdfKpiBoxData("الغياب والإجازات", "غياب $absentCount | إجازة $excusedCount", 0xFFB91C1C.toInt(), 0xFFFEF2F2.toInt()),
                    PdfKpiBoxData("المعدل الأكاديمي العام", "$avgGrade%", 0xFFD97706.toInt(), 0xFFFFFBEB.toInt()),
                    PdfKpiBoxData("حالة القيد", student.enrollmentStatus.arabicLabel, 0xFF1E3A8A.toInt(), 0xFFEFF6FF.toInt())
                )
            )

            val gradeCols = floatArrayOf(560f, 530f, 405f, 345f, 285f, 225f, 165f, 100f, 35f)
            val gradeHeaders = listOf("ت", "المادة الدراسية", "ش1", "ش2", "نصف السنة", "السعي", "النهائي", "المعدل والتقدير")
            val gradeRows = grades.take(10).mapIndexed { idx, gr ->
                listOf(
                    (idx + 1).toString(),
                    gr.subjectName,
                    gr.month1Score.toString(),
                    gr.month2Score.toString(),
                    gr.midYearScore.toString(),
                    gr.annualEffortScore.toString(),
                    gr.finalExamScore.toString(),
                    "${gr.finalAverage}/${gr.maxScoreScale} (${gr.ratingLabel})"
                )
            }

            val afterGradesY = drawStructuredPdfGridTable(
                canvas = canvas,
                topY = 212f,
                cols = gradeCols,
                headers = gradeHeaders,
                rows = gradeRows.ifEmpty {
                    listOf(listOf("-", "لا توجد درجات مرصودة في هذه الفترة", "-", "-", "-", "-", "-", "قيد الرصد"))
                },
                rowHeight = 22f,
                rightAlignColIndex = 1
            )

            val behCols = floatArrayOf(560f, 470f, 355f, 220f, 35f)
            val behHeaders = listOf("التاريخ", "المادة", "التقييم السلوكي والعلمي", "ملاحظات المعلم")
            val behRows = behaviors.take(7).map { beh ->
                listOf(
                    beh.date,
                    beh.subjectName,
                    beh.behaviorType.arabicTitle,
                    beh.note.ifBlank { "متابعة صفية منتظمة" }
                )
            }
            if ((afterGradesY + 60f) < 700f) {
                drawStructuredPdfGridTable(
                    canvas = canvas,
                    topY = afterGradesY + 14f,
                    cols = behCols,
                    headers = behHeaders,
                    rows = behRows.ifEmpty {
                        listOf(listOf("-", "-", "سلوك الطالب منتظم ومنضبط", "لا توجد مخالفات مسجلة"))
                    },
                    rowHeight = 22f,
                    rightAlignColIndex = 3
                )
            }

            drawOfficialPdfFooter(canvas, schoolConfig, principalName, pageNumber = 1, totalPages = 1)
            pdfDocument.finishPage(page)

            val fileName = "Student_Comprehensive_Archive_${student.id}_${System.currentTimeMillis()}.pdf"
            val savedPath = savePdfAndTriggerPrint(
                context = context,
                pdfDocument = pdfDocument,
                fileName = fileName,
                jobName = "السجل الشامل للطالب - ${student.fullNameWithFather}"
            )
            pdfDocument.close()
            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports the Official Annual Promotion & End-of-Year Principal Report as a structured tabular PDF.
     */
    fun exportAnnualPromotionReportPdf(
        context: Context,
        schoolConfig: SchoolConfig,
        report: AnnualPromotionReport,
        principalName: String = "إدارة المدرسة"
    ): Result<String> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            drawOfficialPdfHeader(
                canvas = canvas,
                schoolConfig = schoolConfig,
                documentTitle = "تقرير الترحيل السنوي ونتائج نهاية العام الدراسي",
                subtitle = "الترحيل من (${report.previousAcademicYear}) إلى (${report.newAcademicYear})  •  نسبة النجاح العامة: ${report.overallPassRatePercent}%",
                periodLabel = "${report.previousAcademicYear} ← ${report.newAcademicYear}",
                docNumber = "PRM-${System.currentTimeMillis() % 10000}"
            )

            drawPdfFourKpiStrip(
                canvas = canvas,
                topY = 148f,
                kpis = listOf(
                    PdfKpiBoxData("إجمالي الطلبة", "${report.totalStudentsEvaluated} طالب", 0xFF065F46.toInt(), 0xFFECFDF5.toInt()),
                    PdfKpiBoxData("الناجحون المرحّلون", "${report.promotedCount} طالب", 0xFF15803D.toInt(), 0xFFF0FDF4.toInt()),
                    PdfKpiBoxData("الباقون للإعادة", "${report.repeatingCount} طالب", 0xFFB91C1C.toInt(), 0xFFFEF2F2.toInt()),
                    PdfKpiBoxData("معدل النجاح العام", "${report.overallPassRatePercent}%", 0xFFD97706.toInt(), 0xFFFFFBEB.toInt())
                )
            )

            val classCols = floatArrayOf(560f, 530f, 405f, 290f, 225f, 160f, 95f, 35f)
            val classHeaders = listOf("ت", "الصف الحالي", "الترحيل إلى", "الإجمالي", "ناجح", "معيد", "نسبة النجاح")
            val classRows = report.classBreakdowns.take(9).mapIndexed { idx, cb ->
                listOf(
                    (idx + 1).toString(),
                    cb.gradeClass,
                    cb.nextGradeClass,
                    cb.totalCount.toString(),
                    cb.passedCount.toString(),
                    cb.repeatingCount.toString(),
                    "${cb.passRatePercent}%"
                )
            }

            val afterClassTableY = drawStructuredPdfGridTable(
                canvas = canvas,
                topY = 212f,
                cols = classCols,
                headers = classHeaders,
                rows = classRows.ifEmpty {
                    listOf(listOf("-", "لا توجد صفوف مسجلة", "-", "0", "0", "0", "0%"))
                },
                rowHeight = 22f,
                rightAlignColIndex = 1
            )

            val stuCols = floatArrayOf(560f, 530f, 350f, 240f, 155f, 35f)
            val stuHeaders = listOf("ت", "اسم الطالب الكامل", "المرحلة والصف", "المعدل السنوي", "قرار الترحيل النهائي")
            val allItems = (report.promotedStudents + report.repeatingStudents + report.pendingGradesStudents).take(10)
            val stuRows = allItems.mapIndexed { idx, item ->
                val decision = when {
                    item.isPendingGrades -> "بانتظار الرصد (${item.completedSubjectsCount}/${item.requiredSubjectsCount})"
                    item.graduated -> "متخرج بنجاح (${item.toGradeClass})"
                    item.passed -> "ناجح ومرحّل إلى ${item.toGradeClass}"
                    else -> "معيد في ${item.fromGradeClass}"
                }
                listOf(
                    (idx + 1).toString(),
                    item.studentName,
                    item.fromGradeClass,
                    if (item.isPendingGrades) "قيد الرصد" else "${item.finalAverage}%",
                    decision
                )
            }

            if ((afterClassTableY + 60f) < 705f && stuRows.isNotEmpty()) {
                drawStructuredPdfGridTable(
                    canvas = canvas,
                    topY = afterClassTableY + 12f,
                    cols = stuCols,
                    headers = stuHeaders,
                    rows = stuRows,
                    rowHeight = 21f,
                    rightAlignColIndex = 1
                )
            }

            drawOfficialPdfFooter(canvas, schoolConfig, principalName, pageNumber = 1, totalPages = 1)
            pdfDocument.finishPage(page)

            val fileName = "Annual_Promotion_Report_${System.currentTimeMillis()}.pdf"
            val savedPath = savePdfAndTriggerPrint(
                context = context,
                pdfDocument = pdfDocument,
                fileName = fileName,
                jobName = "تقرير الترحيل السنوي - ${schoolConfig.schoolName}"
            )
            pdfDocument.close()
            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports the Official General Class & Section Comprehensive Statistical Report as a Luxury Official Tabular PDF.
     */
    fun exportClassComprehensiveStatisticalReportPdf(
        context: Context,
        schoolConfig: SchoolConfig,
        reports: List<ClassComprehensiveStatisticalReport>,
        periodLabel: String,
        principalName: String = "إدارة المدرسة"
    ): Result<String> {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            drawOfficialPdfHeader(
                canvas = canvas,
                schoolConfig = schoolConfig,
                documentTitle = "السجل الإحصائي العام للصفوف والشعب الدراسية",
                subtitle = "الفترة: $periodLabel  •  العام الدراسي: ${schoolConfig.academicYear}",
                periodLabel = periodLabel,
                docNumber = "STAT-${System.currentTimeMillis() % 10000}"
            )

            val totalStudentsAll = reports.sumOf { it.totalStudents }
            val avgAttendanceAll = if (reports.isEmpty()) 100 else reports.map { it.attendanceRatePercent }.average().toInt()
            val avgPassRateAll = if (reports.isEmpty()) 0 else reports.map { it.passRatePercent }.average().toInt()

            // 4 Summary KPI Boxes: إجمالي الطلبة | عدد الصفوف والشعب | نسبة المواظبة العامة | معدل النجاح العام
            drawPdfFourKpiStrip(
                canvas = canvas,
                topY = 148f,
                kpis = listOf(
                    PdfKpiBoxData("إجمالي الطلبة", "$totalStudentsAll طالب", 0xFF065F46.toInt(), 0xFFECFDF5.toInt()),
                    PdfKpiBoxData("عدد الصفوف والشعب", "${reports.size} صف/شعبة", 0xFF1E3A8A.toInt(), 0xFFEFF6FF.toInt()),
                    PdfKpiBoxData("نسبة المواظبة العامة", "$avgAttendanceAll%", 0xFF15803D.toInt(), 0xFFF0FDF4.toInt()),
                    PdfKpiBoxData("معدل النجاح العام", "$avgPassRateAll%", 0xFFD97706.toInt(), 0xFFFFFBEB.toInt())
                )
            )

            // Structured Multi-Column Grid Table:
            // ت | الصف والشعبة | العدد الكلي | نسبة الحضور | الغياب والإجازات | معدل الدرجات | نسبة النجاح | حالة القيد
            val cols = floatArrayOf(560f, 535f, 430f, 376f, 322f, 244f, 190f, 134f, 35f)
            val headers = listOf(
                "ت",
                "الصف والشعبة",
                "العدد الكلي",
                "نسبة الحضور",
                "الغياب والإجازات",
                "معدل الدرجات",
                "نسبة النجاح",
                "حالة القيد"
            )

            val rows = reports.take(19).mapIndexed { idx, rep ->
                val statusSummary = buildString {
                    append("مستمر ${rep.activeStudentsCount}")
                    if (rep.suspendedStudentsCount > 0) append(" | مؤقت ${rep.suspendedStudentsCount}")
                    if (rep.expelledStudentsCount > 0) append(" | مفصول ${rep.expelledStudentsCount}")
                    if (rep.transferredStudentsCount > 0) append(" | منقول ${rep.transferredStudentsCount}")
                }
                listOf(
                    (idx + 1).toString(),
                    "${rep.gradeClass} (${rep.sectionName})",
                    "${rep.totalStudents} طالب",
                    "${rep.attendanceRatePercent}%",
                    "غ:${rep.absentDaysTotal} / إ:${rep.excusedDaysTotal}",
                    if (rep.pendingGradesCount == rep.totalStudents && rep.totalStudents > 0) "قيد الرصد" else "${rep.averageGradePercent}%",
                    "${rep.passRatePercent}%",
                    statusSummary
                )
            }

            drawStructuredPdfGridTable(
                canvas = canvas,
                topY = 212f,
                cols = cols,
                headers = headers,
                rows = rows.ifEmpty {
                    listOf(
                        listOf("-", "لا توجد صفوف أو شعب مسجلة", "0", "100%", "0 / 0", "0%", "0%", "مستمر 0")
                    )
                },
                rowHeight = 25f,
                rightAlignColIndex = 1
            )

            drawOfficialPdfFooter(canvas, schoolConfig, principalName, pageNumber = 1, totalPages = 1)
            pdfDocument.finishPage(page)

            val fileName = "Class_Statistical_Report_${System.currentTimeMillis()}.pdf"
            val savedPath = savePdfAndTriggerPrint(
                context = context,
                pdfDocument = pdfDocument,
                fileName = fileName,
                jobName = "السجل الإحصائي العام للصفوف والشعب - ${schoolConfig.schoolName}"
            )
            pdfDocument.close()
            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

