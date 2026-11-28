package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.PI
import kotlin.math.sin

/**
 * Programmatic Vector-to-Bitmap Renderer for Live Audio Broadcast external Android notifications.
 * Generates high-resolution, crisp emerald-mint-gold acoustic wave banners (for BigPictureStyle)
 * and matching circular broadcast icons with zero external image assets or libraries.
 */
object LiveBroadcastBannerBitmapRenderer {

    private const val BANNER_WIDTH = 960
    private const val BANNER_HEIGHT = 440
    private const val ICON_SIZE = 192

    /**
     * Renders a rich 960x440 wide vector banner matching the "مدرستي الذكية" emerald-mint-gold identity:
     * - Deep emerald-to-teal linear gradient with subtle radial mint & gold ambient glow
     * - 3 layered translucent acoustic sine waves + soft vertical gradient fill
     * - Left-side (RTL end) translucent glass emblem with a crisp vector broadcast microphone & sound rings
     * - Right-aligned (RTL start) status badge, Arabic broadcast title, host name, and subtle brand mark
     */
    fun renderLiveBroadcastBanner(
        title: String,
        hostName: String,
        isUrgentInvite: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(BANNER_WIDTH, BANNER_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = BANNER_WIDTH.toFloat()
        val h = BANNER_HEIGHT.toFloat()

        val cleanTitle = InputSanitizer.stripEmojis(title).ifBlank { "جلسة بث صوتي مباشر" }
        val cleanHost = InputSanitizer.stripEmojis(hostName).ifBlank { "إدارة المدرسة" }

        // 1. Rich Emerald-Teal Platform Gradient Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(
                    Color.parseColor("#033D2E"),
                    Color.parseColor("#04624B"),
                    Color.parseColor("#0A4E5A")
                ),
                floatArrayOf(0f, 0.56f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Ambient Luminous Mint & Warm Gold Radial Glows
        val mintGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.19f, h * 0.48f, h * 0.68f,
                intArrayOf(
                    Color.argb(66, 52, 211, 153),
                    Color.argb(18, 167, 243, 208),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, mintGlowPaint)

        val goldGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.85f, h * 0.16f, h * 0.60f,
                intArrayOf(
                    Color.argb(58, 243, 213, 141),
                    Color.argb(16, 217, 164, 65),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.50f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, goldGlowPaint)

        // 3. Layered Acoustic Vector Waves across Full Width
        drawAcousticWaves(canvas, w, h)

        // 4. Left-Side (RTL End) Glass Broadcast Microphone Emblem
        drawMicrophoneStudioEmblem(
            canvas = canvas,
            cx = w * 0.175f,
            cy = h * 0.50f,
            radius = 96f,
            isUrgentInvite = isUrgentInvite
        )

        // 5. Right-Aligned (RTL Start) Status Pill Badge
        val rightMargin = w - 48f
        val pillText = if (isUrgentInvite) "دعوة عاجلة للبث المباشر" else "بث صوتي مباشر الآن"
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val pillTextWidth = badgeTextPaint.measureText(pillText)
        val dotDiameter = 14f
        val pillPadH = 24f
        val pillHeight = 50f
        val pillWidth = pillTextWidth + dotDiameter + 18f + pillPadH * 2f
        val pillRight = rightMargin
        val pillLeft = pillRight - pillWidth
        val pillTop = 44f
        val pillRect = RectF(pillLeft, pillTop, pillRight, pillTop + pillHeight)

        val pillFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isUrgentInvite) {
                Color.argb(85, 220, 38, 38)
            } else {
                Color.argb(48, 255, 255, 255)
            }
        }
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = if (isUrgentInvite) {
                Color.argb(190, 252, 165, 165)
            } else {
                Color.argb(150, 243, 213, 141)
            }
        }
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillFillPaint)
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillBorderPaint)

        // Pulsing live dot inside pill (at right end for Arabic RTL reading start)
        val dotCx = pillRight - pillPadH - dotDiameter / 2f
        val dotCy = pillTop + pillHeight / 2f
        val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(105, 248, 113, 113)
        }
        val dotCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF4444")
        }
        canvas.drawCircle(dotCx, dotCy, dotDiameter * 0.92f, dotGlowPaint)
        canvas.drawCircle(dotCx, dotCy, dotDiameter * 0.50f, dotCorePaint)

        val fmBadge = badgeTextPaint.fontMetrics
        val badgeBaseline = dotCy - (fmBadge.ascent + fmBadge.descent) / 2f
        canvas.drawText(pillText, dotCx - dotDiameter - 8f, badgeBaseline, badgeTextPaint)

        // 6. Broadcast Title (up to 2 crisp RTL lines inside right text zone)
        val textAreaLeft = w * 0.34f
        val textAreaWidth = (rightMargin - textAreaLeft).toInt().coerceAtLeast(320)
        val titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val truncatedTitle = if (cleanTitle.length > 62) cleanTitle.take(59).trimEnd() + "..." else cleanTitle
        val titleLayout = StaticLayout.Builder
            .obtain(truncatedTitle, 0, truncatedTitle.length, titleTextPaint, textAreaWidth)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setMaxLines(2)
            .setLineSpacing(4f, 1.08f)
            .build()

        val titleTopY = pillTop + pillHeight + 26f
        canvas.save()
        canvas.translate(textAreaLeft, titleTopY)
        titleLayout.draw(canvas)
        canvas.restore()

        // 7. Host Name & Subtle Brand Caption
        val hostY = (titleTopY + titleLayout.height + 36f).coerceAtMost(h - 74f)
        val hostPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.parseColor("#A7F3D0")
            textSize = 27f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val hostLabel = "بإشراف: $cleanHost"
        val trimmedHost = if (hostLabel.length > 44) hostLabel.take(41).trimEnd() + "..." else hostLabel
        canvas.drawText(trimmedHost, rightMargin, hostY, hostPaint)

        // 8. Refined Bottom Gold-Mint Accent Bar + Platform Signature
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w, 0f,
                intArrayOf(
                    Color.argb(40, 167, 243, 208),
                    Color.argb(210, 217, 164, 65),
                    Color.argb(230, 243, 213, 141),
                    Color.argb(90, 52, 211, 153)
                ),
                floatArrayOf(0f, 0.35f, 0.72f, 1f),
                Shader.TileMode.CLAMP
            )
            strokeWidth = 5f
        }
        canvas.drawLine(0f, h - 2.5f, w, h - 2.5f, accentPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.argb(185, 243, 213, 141)
            textSize = 21f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("مدرستي الذكية • البث الصوتي المباشر", rightMargin, h - 24f, brandPaint)

        return bitmap
    }

    /**
     * Renders a crisp 192x192 circular emerald-gold broadcast large icon for NotificationCompat.setLargeIcon.
     */
    fun renderLiveBroadcastLargeIcon(isUrgentInvite: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = ICON_SIZE / 2f
        val cy = ICON_SIZE / 2f
        val r = ICON_SIZE * 0.48f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, ICON_SIZE.toFloat(), ICON_SIZE.toFloat(),
                intArrayOf(
                    Color.parseColor("#044E3B"),
                    Color.parseColor("#066A4E"),
                    Color.parseColor("#059669")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, r, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            color = if (isUrgentInvite) Color.parseColor("#FCA5A5") else Color.parseColor("#F3D58D")
        }
        canvas.drawCircle(cx, cy, r - 3f, borderPaint)

        drawMicrophoneGlyph(canvas, cx, cy, scale = 0.78f, isUrgentInvite = isUrgentInvite)
        return bitmap
    }

    private fun drawAcousticWaves(canvas: Canvas, w: Float, h: Float) {
        val baseY = h * 0.74f
        val fillPath = Path()
        val wave1 = Path()
        val wave2 = Path()
        val wave3 = Path()
        val steps = 96
        val dx = w / steps

        for (i in 0..steps) {
            val x = i * dx
            val norm = x / w
            val envelope = 0.48f + 0.52f * sin(norm * PI).toFloat()
            val y1 = baseY + sin(norm * 4.2 * PI + 0.4).toFloat() * (46f * envelope)
            val y2 = baseY - 12f + sin(norm * 3.0 * PI + 1.8).toFloat() * (34f * envelope)
            val y3 = baseY + 14f + sin(norm * 5.6 * PI + 2.9).toFloat() * (24f * envelope)

            if (i == 0) {
                fillPath.moveTo(0f, h)
                fillPath.lineTo(x, y1)
                wave1.moveTo(x, y1)
                wave2.moveTo(x, y2)
                wave3.moveTo(x, y3)
            } else {
                fillPath.lineTo(x, y1)
                wave1.lineTo(x, y1)
                wave2.lineTo(x, y2)
                wave3.lineTo(x, y3)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        val areaFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, baseY - 50f, 0f, h,
                Color.argb(42, 52, 211, 153),
                Color.argb(6, 4, 78, 59),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(fillPath, areaFillPaint)

        val stroke2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.2f
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(72, 167, 243, 208)
        }
        val stroke3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(96, 243, 213, 141)
        }
        val stroke1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.2f
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(122, 52, 211, 153)
        }

        canvas.drawPath(wave2, stroke2)
        canvas.drawPath(wave3, stroke3)
        canvas.drawPath(wave1, stroke1)
    }

    private fun drawMicrophoneStudioEmblem(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        isUrgentInvite: Boolean
    ) {
        // Concentric acoustic signal rings
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        ringPaint.strokeWidth = 2.2f
        ringPaint.color = Color.argb(56, 167, 243, 208)
        canvas.drawCircle(cx, cy, radius * 1.24f, ringPaint)

        ringPaint.strokeWidth = 2.6f
        ringPaint.color = if (isUrgentInvite) {
            Color.argb(95, 252, 165, 165)
        } else {
            Color.argb(85, 243, 213, 141)
        }
        canvas.drawCircle(cx, cy, radius * 1.04f, ringPaint)

        // Translucent frosted emerald glass disc
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx - radius * 0.2f, cy - radius * 0.25f, radius * 1.1f,
                intArrayOf(
                    Color.argb(72, 255, 255, 255),
                    Color.argb(30, 16, 185, 129),
                    Color.argb(52, 3, 61, 46)
                ),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius * 0.86f, discPaint)

        val discBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
            color = Color.argb(135, 243, 213, 141)
        }
        canvas.drawCircle(cx, cy, radius * 0.86f, discBorderPaint)

        drawMicrophoneGlyph(canvas, cx, cy, scale = 1.0f, isUrgentInvite = isUrgentInvite)
    }

    private fun drawMicrophoneGlyph(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float,
        isUrgentInvite: Boolean
    ) {
        val micW = 30f * scale
        val micH = 52f * scale
        val micTop = cy - 34f * scale
        val micRect = RectF(cx - micW / 2f, micTop, cx + micW / 2f, micTop + micH)

        // Microphone capsule fill
        val micFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                micRect.left, micRect.top, micRect.right, micRect.bottom,
                Color.WHITE,
                Color.parseColor("#D1FAE5"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(micRect, micW / 2f, micW / 2f, micFillPaint)

        // Subtle gold grille slats on mic head
        val grillePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(130, 4, 120, 87)
            strokeWidth = 2.6f * scale
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0..2) {
            val gy = micRect.top + micH * (0.28f + i * 0.17f)
            canvas.drawLine(cx - micW * 0.24f, gy, cx + micW * 0.24f, gy, grillePaint)
        }

        // U-cradle & stand
        val cradlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f * scale
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#F3D58D")
        }
        val cradleRadius = 26f * scale
        val cradleTop = micTop + micH * 0.42f
        val cradleArc = RectF(
            cx - cradleRadius,
            cradleTop - cradleRadius * 0.25f,
            cx + cradleRadius,
            cradleTop + cradleRadius * 1.48f
        )
        canvas.drawArc(cradleArc, 0f, 180f, false, cradlePaint)

        val stemTop = cradleArc.bottom
        val stemBottom = stemTop + 16f * scale
        canvas.drawLine(cx, stemTop, cx, stemBottom, cradlePaint)
        canvas.drawLine(cx - 17f * scale, stemBottom, cx + 17f * scale, stemBottom, cradlePaint)

        // Left & right radiating sound wave arcs
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.6f * scale
            strokeCap = Paint.Cap.ROUND
            color = if (isUrgentInvite) Color.parseColor("#FCA5A5") else Color.parseColor("#A7F3D0")
        }
        val arcCenterY = micTop + micH * 0.45f
        val rInner = 34f * scale
        val rOuter = 47f * scale
        val innerRect = RectF(cx - rInner, arcCenterY - rInner, cx + rInner, arcCenterY + rInner)
        val outerRect = RectF(cx - rOuter, arcCenterY - rOuter, cx + rOuter, arcCenterY + rOuter)

        canvas.drawArc(innerRect, -34f, 68f, false, arcPaint)
        canvas.drawArc(innerRect, 146f, 68f, false, arcPaint)
        arcPaint.alpha = 165
        canvas.drawArc(outerRect, -28f, 56f, false, arcPaint)
        canvas.drawArc(outerRect, 152f, 56f, false, arcPaint)
    }
}
