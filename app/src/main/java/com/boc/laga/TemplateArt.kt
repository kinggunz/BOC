package com.boc.laga

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

/** Satu template papan skor: warna sisi kiri & kanan (terang + gelap). */
class Tpl(val name: String, val left: Int, val leftDark: Int, val right: Int, val rightDark: Int)

/** Template papan skor digambar sepenuhnya dengan Canvas; latar transparan. */
object Templates {
    val list = listOf(
        Tpl("Biru - Merah", Color.parseColor("#2F80FF"), Color.parseColor("#08188A"), Color.parseColor("#FF3B3B"), Color.parseColor("#7A0808")),
        Tpl("Emas - Hitam", Color.parseColor("#FFB300"), Color.parseColor("#6B3E00"), Color.parseColor("#6A6A78"), Color.parseColor("#09090D")),
        Tpl("Hijau - Ungu", Color.parseColor("#17E67C"), Color.parseColor("#04472A"), Color.parseColor("#B455FF"), Color.parseColor("#33085E"))
    )

    private const val BW = 1200f   // lebar dasar
    private const val BH = 270f    // tinggi dasar
    private const val BAR = 190f   // tinggi bilah utama

    private fun mix(a: Int, b: Int): Int = Color.rgb(
        (Color.red(a) + Color.red(b)) / 2, (Color.green(a) + Color.green(b)) / 2, (Color.blue(a) + Color.blue(b)) / 2)

    fun render(t: Tpl, leftName: String, rightName: String, bottom: String, nameColor: Int, bottomColor: Int, width: Int = 1200): Bitmap {
        val k = width / BW
        val bmp = Bitmap.createBitmap(width, (BH * k).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.scale(k, k)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val mid = BW / 2f
        val bar = RectF(4f, 4f, BW - 4f, BAR)

        // ---- bilah utama ----
        val clip = Path().apply { addRoundRect(bar, 18f, 18f, Path.Direction.CW) }
        cv.save(); cv.clipPath(clip)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, mid, 0f, t.left, t.leftDark, Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, mid, BAR, p)
        p.shader = LinearGradient(BW, 0f, mid, 0f, t.right, t.rightDark, Shader.TileMode.CLAMP)
        cv.drawRect(mid, 0f, BW, BAR, p)
        // sinar diagonal
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        var i = 0
        var x = 20f
        while (x < mid - 80f) {
            p.strokeWidth = if (i % 3 == 0) 7f else 3f
            p.color = Color.argb(if (i % 3 == 0) 70 else 45, 255, 255, 255)
            cv.drawLine(x, BAR, x + 70f, 0f, p)
            cv.drawLine(BW - x, BAR, BW - x - 70f, 0f, p)
            x += 46f; i++
        }
        // cahaya di tengah
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(mid, BAR / 2f, 300f, Color.argb(110, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, BW, BAR, p)
        p.shader = null
        // gerbang hitam di tengah
        val gate = Path().apply {
            moveTo(mid - 78f, 0f); lineTo(mid + 78f, 0f); lineTo(mid + 40f, BAR); lineTo(mid - 40f, BAR); close()
        }
        p.color = Color.argb(235, 8, 8, 12); cv.drawPath(gate, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = Color.argb(220, 255, 255, 255)
        cv.drawLine(mid - 78f, 0f, mid - 40f, BAR, p)
        cv.drawLine(mid + 78f, 0f, mid + 40f, BAR, p)
        // petir tipis
        p.strokeWidth = 4f; p.color = Color.argb(210, 255, 255, 255); p.strokeJoin = Paint.Join.ROUND
        val bolt1 = Path().apply { moveTo(mid - 86f, 20f); lineTo(mid - 110f, 70f); lineTo(mid - 92f, 78f); lineTo(mid - 124f, 150f) }
        val bolt2 = Path().apply { moveTo(mid + 86f, 20f); lineTo(mid + 112f, 72f); lineTo(mid + 92f, 82f); lineTo(mid + 126f, 152f) }
        cv.drawPath(bolt1, p); cv.drawPath(bolt2, p)
        // kilap atas
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 0f, BAR * 0.5f, Color.argb(70, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, BW, BAR * 0.5f, p)
        p.shader = null
        cv.restore()
        // tepi bilah
        p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.color = Color.argb(200, 0, 0, 0)
        cv.drawRoundRect(bar, 18f, 18f, p)
        p.strokeWidth = 3f; p.color = Color.argb(150, 255, 255, 255)
        cv.drawRoundRect(bar, 18f, 18f, p)

        // ---- nama tim ----
        val tp = Paint(Paint.ANTI_ALIAS_FLAG)
        tp.typeface = Typeface.create("sans-serif-black", Typeface.ITALIC)
        val l = leftName.trim().uppercase(); val r = rightName.trim().uppercase()
        val maxW = 440f
        var size = 118f
        tp.textSize = size
        while ((tp.measureText(l) > maxW || tp.measureText(r) > maxW) && size > 24f) { size -= 3f; tp.textSize = size }
        val fm = tp.fontMetrics
        val by = (bar.top + BAR) / 2f - (fm.ascent + fm.descent) / 2f - 4f
        drawOutlined(cv, tp, l, mid / 2f - 20f, by, nameColor, size)
        drawOutlined(cv, tp, r, mid + mid / 2f + 20f, by, nameColor, size)

        // ---- pil bawah ----
        val pw = 520f; val ph = 92f
        val pill = RectF(mid - pw / 2f, BAR - 22f, mid + pw / 2f, BAR - 22f + ph)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(pill.left, 0f, pill.right, 0f, intArrayOf(t.left, mix(t.left, t.right), t.right), null, Shader.TileMode.CLAMP)
        cv.drawRoundRect(pill, ph / 2f, ph / 2f, p)
        p.shader = LinearGradient(0f, pill.top, 0f, pill.centerY(), Color.argb(110, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
        cv.drawRoundRect(RectF(pill.left + 10f, pill.top + 4f, pill.right - 10f, pill.centerY()), ph / 3f, ph / 3f, p)
        p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 8f; p.color = Color.argb(200, 0, 0, 0)
        cv.drawRoundRect(pill, ph / 2f, ph / 2f, p)
        p.strokeWidth = 3.5f; p.color = Color.argb(220, 255, 255, 255)
        cv.drawRoundRect(pill, ph / 2f, ph / 2f, p)

        val bp = Paint(Paint.ANTI_ALIAS_FLAG)
        bp.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val bt = bottom.trim().uppercase()
        var bs = 56f
        bp.textSize = bs
        while (bp.measureText(bt) > pw - 90f && bs > 18f) { bs -= 2f; bp.textSize = bs }
        val bfm = bp.fontMetrics
        drawOutlined(cv, bp, bt, mid, pill.centerY() - (bfm.ascent + bfm.descent) / 2f, bottomColor, bs)
        return bmp
    }

    /** Teks berkontur gelap, rata tengah di cx. */
    private fun drawOutlined(cv: Canvas, tp: Paint, s: String, cx: Float, y: Float, color: Int, size: Float) {
        val w = tp.measureText(s)
        tp.style = Paint.Style.STROKE; tp.strokeJoin = Paint.Join.ROUND; tp.strokeWidth = size * 0.16f
        tp.color = Color.argb(235, 0, 0, 0)
        cv.drawText(s, cx - w / 2f, y, tp)
        tp.style = Paint.Style.FILL; tp.color = color
        cv.drawText(s, cx - w / 2f, y, tp)
    }
}
