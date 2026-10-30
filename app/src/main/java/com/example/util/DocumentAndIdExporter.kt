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
import com.example.model.AttendanceEntry
import com.example.model.AttendanceStatus
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
     */
    fun renderStudentIdCardBitmap(
        student: StudentRecord,
        schoolConfig: SchoolConfig,
        badges: List<StudentAutoBadge> = emptyList()
    ): Bitmap {
        val width = 1280
        val height = 820
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_OFF_WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 36f, 36f, bgPaint)

        // Header luxury emerald gradient
        val headerHeight = 225f
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
        canvas.drawRect(0f, headerHeight - 10f, width.toFloat(), headerHeight + 4f, goldPaint)

        // School Logo or Crest in top-right
        val logoDecoded = decodeBase64Bitmap(schoolConfig.logoBase64)
        val logoCenterX = width - 115f
        val logoCenterY = 110f
        val logoRadius = 68f
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
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("مدرستي", logoCenterX, logoCenterY + 14f, initialsPaint)
        }

        // Header Text (Right-aligned adjacent to logo)
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LIGHT_GOLD
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val textRightX = width - 210f
        canvas.drawText(schoolConfig.schoolName.ifBlank { "منصة مدرستي النموذجية" }, textRightX, 82f, headerTitlePaint)
        canvas.drawText(
            "بطاقة الهوية المدرسية الرسمية • ${schoolConfig.schoolStage.arabicLabel}",
            textRightX,
            130f,
            headerSubPaint
        )
        canvas.drawText(
            "العام الدراسي: ${schoolConfig.academicYear}  |  ${schoolConfig.address}",
            textRightX,
            175f,
            headerSubPaint
        )

        // Left side of header: School Code badge
        val badgeRect = RectF(48f, 52f, 290f, 140f)
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
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "كود المدرسة: ${schoolConfig.schoolId.ifBlank { "100" }}",
            badgeRect.centerX(),
            badgeRect.centerY() + 10f,
            badgeTextPaint
        )

        // Student Photo Box on the Right Side
        val photoBoxRect = RectF(width - 310f, 275f, width - 65f, 555f)
        canvas.drawRoundRect(
            RectF(photoBoxRect.left - 6f, photoBoxRect.top - 6f, photoBoxRect.right + 6f, photoBoxRect.bottom + 6f),
            28f,
            28f,
            goldPaint
        )
        canvas.drawRoundRect(photoBoxRect, 24f, 24f, circlePaint)

        val studentPhoto = decodeBase64Bitmap(student.photoBase64)
        if (studentPhoto != null) {
            val saveCount = canvas.save()
            val clipPath = Path().apply {
                addRoundRect(photoBoxRect, 24f, 24f, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(studentPhoto, null, photoBoxRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restoreToCount(saveCount)
        } else {
            val placeholderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFE6F2EC.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(photoBoxRect, 24f, 24f, placeholderBg)
            val initials = student.fullNameWithFather.trim().take(1).ifBlank { "ط" }
            val avatarTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 110f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initials, photoBoxRect.centerX(), photoBoxRect.centerY() + 36f, avatarTextPaint)
        }

        // Student Details in the Center-Right
        val infoRightX = photoBoxRect.left - 42f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_TEXT
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROYAL_GREEN
            textSize = 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("اسم الطالب الكامل:", infoRightX, 305f, labelPaint)
        canvas.drawText(student.fullNameWithFather, infoRightX, 355f, valuePaint)

        canvas.drawText("الصف والشعبة الدراسية:", infoRightX, 415f, labelPaint)
        canvas.drawText("${student.gradeClass}  —  شعبة (${student.sectionName})", infoRightX, 462f, subValuePaint)

        canvas.drawText("كود الطالب الموحد / هاتف ولي الأمر:", infoRightX, 520f, labelPaint)
        canvas.drawText(
            "${student.studentUsername}   |   ${student.parentPhone.ifBlank { "غير مسجل" }}",
            infoRightX,
            564f,
            subValuePaint
        )

        // Badges strip if student has auto-badges or honor title
        val badgeTitles = buildList {
            if (student.isOnHonorRoll) add(student.honorTitle)
            badges.forEach { add(it.title) }
        }.distinct()

        if (badgeTitles.isNotEmpty()) {
            val badgeStripRect = RectF(390f, 595f, infoRightX, 658f)
            val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_LIGHT_GOLD
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(badgeStripRect, 18f, 18f, stripPaint)
            canvas.drawRoundRect(badgeStripRect, 18f, 18f, badgeBorderPaint)

            val stripTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 25f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(
                "الأوسمة: " + badgeTitles.joinToString(" • "),
                badgeStripRect.right - 20f,
                badgeStripRect.centerY() + 9f,
                stripTextPaint
            )
        }

        // QR Code Box on the Left Side
        val qrSize = 300
        val qrBitmap = generateQrCodeBitmap(student.qrCodeValue, qrSize)
        val qrLeft = 58f
        val qrTop = 270f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        val qrCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_EMERALD
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("امسح رمز QR لتسجيل الحضور الفوري", qrLeft + qrSize / 2f, qrTop + qrSize + 36f, qrCaptionPaint)

        val qrIdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED_TEXT
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(student.qrCodeValue, qrLeft + qrSize / 2f, qrTop + qrSize + 68f, qrIdPaint)

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
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "هوية مدرسية معتمدة صادرة عبر منصة مدرستي الذكية  •  هاتف الإدارة: ${schoolConfig.phone}",
            width / 2f,
            footerTop + 54f,
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
        badges: List<StudentAutoBadge> = emptyList()
    ): Result<String> {
        return try {
            val bitmap = renderStudentIdCardBitmap(student, schoolConfig, badges)
            val safeName = student.fullNameWithFather.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_")
            val fileName = "Madrasati_ID_${safeName}_${System.currentTimeMillis()}.png"

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
        badges: List<StudentAutoBadge> = emptyList()
    ): Result<String> {
        return try {
            val idBitmap = renderStudentIdCardBitmap(student, schoolConfig, badges)
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "بطاقة الهوية المدرسية الذكية — جاهزة للطباعة أو الكبس الحراري",
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
     * Exports an official Student Report Card (بطاقة الدرجات / الشهادة المدرسية) with school header,
     * logo, subject grades table, and principal signature.
     */
    fun exportStudentReportCardPdf(
        context: Context,
        student: StudentRecord,
        grades: List<SubjectGrade>,
        schoolConfig: SchoolConfig,
        principalName: String = "مدير المدرسة"
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
                documentTitle = "بطاقة الدرجات الرسمية (الشهادة المدرسية)",
                subtitle = "الطالب: ${student.fullNameWithFather}  |  الصف: ${student.gradeClass} (${student.sectionName})"
            )

            val tableTop = 165f
            val left = 35f
            val right = 560f
            val rowHeight = 28f

            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(left, tableTop, right, tableTop + 32f), 8f, 8f, headerBgPaint)

            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_LIGHT_GOLD
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            // Columns (RTL from right to left): المادة | يومي | ش1 | ش2 | نصف السنة | السعي | النهائي | المعدل | التقدير
            val cols = floatArrayOf(560f, 440f, 395f, 350f, 305f, 250f, 200f, 150f, 100f, 35f)
            val headers = listOf("المادة الدراسية", "يومي", "ش1", "ش2", "نصف السنة", "السعي", "النهائي", "المعدل", "التقدير")
            for (i in headers.indices) {
                val cx = (cols[i] + cols[i + 1]) / 2f
                canvas.drawText(headers[i], cx, tableTop + 20f, thPaint)
            }

            val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DARK_TEXT
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFD5E3DC.toInt()
                strokeWidth = 1f
            }

            var currentY = tableTop + 32f
            grades.forEachIndexed { idx, g ->
                if (idx % 2 == 0) {
                    val zebraPaint = Paint().apply { color = COLOR_OFF_WHITE }
                    canvas.drawRect(left, currentY, right, currentY + rowHeight, zebraPaint)
                }
                val cells = listOf(
                    g.subjectName,
                    g.dailyScore.toString(),
                    g.month1Score.toString(),
                    g.month2Score.toString(),
                    g.midYearScore.toString(),
                    g.annualEffortScore.toString(),
                    g.finalExamScore.toString(),
                    "${g.finalAverage}/${g.maxScoreScale}",
                    g.ratingLabel
                )
                for (i in cells.indices) {
                    val cx = (cols[i] + cols[i + 1]) / 2f
                    canvas.drawText(cells[i], cx, currentY + 18f, rowTextPaint)
                }
                currentY += rowHeight
                canvas.drawLine(left, currentY, right, currentY, linePaint)
            }

            // Overall average summary
            val overallAvg = if (grades.isNotEmpty()) grades.map { it.normalizedPercentage }.average().toInt() else 0
            val summaryTop = (currentY + 24f).coerceAtMost(700f)
            val summaryBox = RectF(left, summaryTop, right, summaryTop + 46f)
            val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_LIGHT_GOLD
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(summaryBox, 10f, 10f, summaryPaint)

            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(
                "المعدل العام المئوي للطالب: $overallAvg%   |   عدد المواد المرصودة: ${grades.size}",
                right - 20f,
                summaryTop + 28f,
                boldPaint
            )

            drawOfficialPdfFooter(canvas, schoolConfig, principalName)
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
     * Exports the Monthly Attendance & Absence Report (تصدير كشف الغياب الشهري) as an official PDF.
     */
    fun exportMonthlyAttendanceReportPdf(
        context: Context,
        schoolConfig: SchoolConfig,
        gradeClass: String,
        sectionName: String,
        monthPrefix: String,
        students: List<StudentRecord>,
        attendanceList: List<AttendanceEntry>,
        principalName: String = "مدير المدرسة"
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
                documentTitle = "كشف الحضور والغياب الشهري الرسمي",
                subtitle = "الصف: $gradeClass — شعبة ($sectionName)  |  الشهر: $monthPrefix"
            )

            val left = 35f
            val right = 560f
            val tableTop = 165f
            val rowHeight = 26f

            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(left, tableTop, right, tableTop + 30f), 8f, 8f, headerBgPaint)

            val cols = floatArrayOf(560f, 360f, 280f, 200f, 120f, 35f)
            val headers = listOf("اسم الطالب الكامل", "أيام الحضور", "أيام الغياب", "تأخر", "مجاز بعذر")
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_LIGHT_GOLD
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            for (i in headers.indices) {
                canvas.drawText(headers[i], (cols[i] + cols[i + 1]) / 2f, tableTop + 19f, thPaint)
            }

            val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DARK_TEXT
                textSize = 11f
                textAlign = Paint.Align.CENTER
            }
            val linePaint = Paint().apply { color = 0xFFD5E3DC.toInt(); strokeWidth = 1f }

            var currentY = tableTop + 30f
            students.take(22).forEachIndexed { idx, stu ->
                val stuRecords = attendanceList.filter {
                    it.studentId == stu.id && (monthPrefix.isBlank() || it.date.startsWith(monthPrefix))
                }
                val presentCount = stuRecords.count { it.status == AttendanceStatus.PRESENT }
                val absentCount = stuRecords.count { it.status == AttendanceStatus.ABSENT }
                val lateCount = stuRecords.count { it.status == AttendanceStatus.LATE }
                val excusedCount = stuRecords.count { it.status == AttendanceStatus.EXCUSED }

                if (idx % 2 == 0) {
                    val zebra = Paint().apply { color = COLOR_OFF_WHITE }
                    canvas.drawRect(left, currentY, right, currentY + rowHeight, zebra)
                }
                val cells = listOf(
                    stu.fullNameWithFather,
                    presentCount.toString(),
                    absentCount.toString(),
                    lateCount.toString(),
                    excusedCount.toString()
                )
                for (i in cells.indices) {
                    canvas.drawText(cells[i], (cols[i] + cols[i + 1]) / 2f, currentY + 17f, rowPaint)
                }
                currentY += rowHeight
                canvas.drawLine(left, currentY, right, currentY, linePaint)
            }

            drawOfficialPdfFooter(canvas, schoolConfig, principalName)
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
     * Exports the Tuition & Installments Statement (تصدير كشف الأقساط الدراسية) as an official PDF.
     */
    fun exportTuitionStatementPdf(
        context: Context,
        schoolConfig: SchoolConfig,
        tuitions: List<TuitionRecord>,
        principalName: String = "مدير المدرسة"
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
                subtitle = "إجمالي السجلات: ${tuitions.size} طالب  |  العام الدراسي: ${schoolConfig.academicYear}"
            )

            val fmt = NumberFormat.getNumberInstance(Locale.US)
            val left = 35f
            val right = 560f
            val tableTop = 165f
            val rowHeight = 26f

            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(left, tableTop, right, tableTop + 30f), 8f, 8f, headerBgPaint)

            val cols = floatArrayOf(560f, 390f, 290f, 195f, 105f, 35f)
            val headers = listOf("اسم الطالب", "الصف", "القسط الكلي", "المدفوع", "المتبقي")
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_LIGHT_GOLD
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            for (i in headers.indices) {
                canvas.drawText(headers[i], (cols[i] + cols[i + 1]) / 2f, tableTop + 19f, thPaint)
            }

            val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DARK_TEXT
                textSize = 10.5f
                textAlign = Paint.Align.CENTER
            }
            val linePaint = Paint().apply { color = 0xFFD5E3DC.toInt(); strokeWidth = 1f }

            var currentY = tableTop + 30f
            tuitions.take(20).forEachIndexed { idx, rec ->
                if (idx % 2 == 0) {
                    val zebra = Paint().apply { color = COLOR_OFF_WHITE }
                    canvas.drawRect(left, currentY, right, currentY + rowHeight, zebra)
                }
                val cells = listOf(
                    rec.studentName,
                    "${rec.gradeClass} (${rec.sectionName})",
                    "${fmt.format(rec.totalFee)} د.ع",
                    "${fmt.format(rec.paidAmount)} د.ع",
                    "${fmt.format(rec.remainingAmount)} د.ع"
                )
                for (i in cells.indices) {
                    canvas.drawText(cells[i], (cols[i] + cols[i + 1]) / 2f, currentY + 17f, rowPaint)
                }
                currentY += rowHeight
                canvas.drawLine(left, currentY, right, currentY, linePaint)
            }

            val totalFeeSum = tuitions.sumOf { it.totalFee }
            val paidSum = tuitions.sumOf { it.paidAmount }
            val remSum = tuitions.sumOf { it.remainingAmount }

            val sumBox = RectF(left, (currentY + 16f).coerceAtMost(700f), right, (currentY + 56f).coerceAtMost(740f))
            val sumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_LIGHT_GOLD }
            canvas.drawRoundRect(sumBox, 8f, 8f, sumPaint)

            val sumTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(
                "الإجمالي الكلي: ${fmt.format(totalFeeSum)} د.ع  |  المستلم: ${fmt.format(paidSum)} د.ع  |  المتبقي: ${fmt.format(remSum)} د.ع",
                right - 16f,
                sumBox.centerY() + 4f,
                sumTextPaint
            )

            drawOfficialPdfFooter(canvas, schoolConfig, principalName)
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

    private fun drawOfficialPdfHeader(
        canvas: Canvas,
        schoolConfig: SchoolConfig,
        documentTitle: String,
        subtitle: String
    ) {
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 595f, 135f,
                COLOR_DEEP_EMERALD, COLOR_ROYAL_GREEN, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, 595f, 135f, headerPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GOLD }
        canvas.drawRect(0f, 131f, 595f, 136f, goldPaint)

        val logo = decodeBase64Bitmap(schoolConfig.logoBase64)
        if (logo != null) {
            val dst = RectF(500f, 22f, 565f, 87f)
            canvas.drawBitmap(logo, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        val schoolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LIGHT_GOLD
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        val rightX = if (logo != null) 485f else 555f
        canvas.drawText(schoolConfig.schoolName, rightX, 42f, schoolPaint)
        canvas.drawText(documentTitle, rightX, 72f, titlePaint)
        canvas.drawText(subtitle, rightX, 102f, subPaint)
    }

    private fun drawOfficialPdfFooter(
        canvas: Canvas,
        schoolConfig: SchoolConfig,
        principalName: String
    ) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val footerTop = 760f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            strokeWidth = 2f
        }
        canvas.drawLine(35f, footerTop, 560f, footerTop, linePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DARK_TEXT
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("تاريخ الإصدار: $dateStr", 560f, footerTop + 26f, textPaint)
        canvas.drawText("ختم المدرسة الرسمي", 330f, footerTop + 26f, textPaint)

        val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_EMERALD
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("مدير المدرسة: ${principalName.ifBlank { "إدارة المدرسة" }}", 35f, footerTop + 26f, leftPaint)
        canvas.drawText("${schoolConfig.schoolName} • ${schoolConfig.address}", 35f, footerTop + 46f, leftPaint)
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

            canvas.drawColor(COLOR_OFF_WHITE)
            drawOfficialPdfHeader(
                canvas = canvas,
                schoolConfig = schoolConfig,
                documentTitle = "وصل قبض قسط دراسي رقمي رسمي",
                subtitle = "رقم الوصل: ${installment.receiptNumber} • التاريخ: ${installment.paymentDate}"
            )

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = COLOR_GOLD
            }
            val receiptRect = RectF(40f, 155f, 555f, 530f)
            canvas.drawRoundRect(receiptRect, 18f, 18f, cardPaint)
            canvas.drawRoundRect(receiptRect, 18f, 18f, borderPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_MUTED_TEXT
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DARK_TEXT
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }

            val rows = listOf(
                "رقم وصل القبض الرسمي:" to installment.receiptNumber,
                "تاريخ تسديد الدفعة:" to installment.paymentDate,
                "اسم الطالب الكامل:" to tuitionRecord.studentName,
                "الصف والشعبة:" to "${tuitionRecord.gradeClass} (${tuitionRecord.sectionName})",
                "اسم ولي الأمر:" to tuitionRecord.parentName,
                "مبلغ الدفعة المستلمة:" to "${installment.amount} دينار عراقي",
                "ملاحظات الدفعة:" to installment.note.ifBlank { "دفعة قسط دراسي" },
                "إجمالي القسط السنوي:" to "${tuitionRecord.totalFee} د.ع",
                "مجموع المبالغ الواصلة:" to "${tuitionRecord.paidAmount} د.ع",
                "المبلغ المتبقي في الذمة:" to "${tuitionRecord.remainingAmount} د.ع"
            )

            var y = 195f
            rows.forEachIndexed { idx, (label, value) ->
                canvas.drawText(label, 530f, y, labelPaint)
                canvas.drawText(value, 65f, y, valuePaint)
                if (idx < rows.lastIndex) {
                    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFFE2E8F0.toInt()
                        strokeWidth = 1f
                    }
                    canvas.drawLine(60f, y + 10f, 535f, y + 10f, divPaint)
                }
                y += 33f
            }

            // Stamp QR verification on receipt
            val receiptQrPayload = "RECEIPT|${schoolConfig.schoolId}|${installment.receiptNumber}|${tuitionRecord.studentName}|${installment.amount}|${installment.paymentDate}"
            val qrBmp = generateQrCodeBitmap(receiptQrPayload, 140)
            canvas.drawBitmap(qrBmp, 65f, 560f, null)

            val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_EMERALD
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("وصل رقمي موثق بختم QR من إدارة (${schoolConfig.schoolName})", 545f, 615f, stampPaint)
            canvas.drawText("العنوان: ${schoolConfig.address} • العام الدراسي: ${schoolConfig.academicYear}", 545f, 642f, labelPaint)

            drawOfficialPdfFooter(canvas, schoolConfig, principalName)
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
}
