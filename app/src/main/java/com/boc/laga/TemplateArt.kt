package com.boc.laga

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

/**
 * Satu template gaya komik/manga: tiga papan (kiri, kanan, bawah) + bintang.
 * style: 0 = sinar menyebar + titik halftone, 1 = garis kecepatan + titik, 2 = garis miring + titik.
 */
class Tpl(
    val name: String, val style: Int,
    val leftBase: Int, val leftLine: Int, val leftDot: Int,
    val rightBase: Int, val rightLine: Int, val rightDot: Int,
    val botBase: Int, val botLine: Int, val botDot: Int,
    val border: Int, val star: Int
)

/** Template papan skor komik digambar sepenuhnya dengan Canvas; latar transparan. */
object Templates {
    val list = listOf(
        // 1. Manga putih (seperti contoh)
        Tpl("Manga Putih", 0,
            Color.parseColor("#F4F4F4"), Color.parseColor("#BDBDBD"), Color.parseColor("#8E8E8E"),
            Color.parseColor("#F4F4F4"), Color.parseColor("#BDBDBD"), Color.parseColor("#8E8E8E"),
            Color.parseColor("#F3ECCB"), Color.parseColor("#D8CFA6"), Color.parseColor("#B8AD7C"),
            Color.parseColor("#0A0A0A"), Color.parseColor("#F5B301")),
        // 2. Komik biru - merah
        Tpl("Komik Biru - Merah", 1,
            Color.parseColor("#D6E8FF"), Color.parseColor("#4A90E2"), Color.parseColor("#2F6FC4"),
            Color.parseColor("#FFD9D9"), Color.parseColor("#E24A4A"), Color.parseColor("#C42F2F"),
            Color.parseColor("#FFF3C4"), Color.parseColor("#F0C94A"), Color.parseColor("#D9A400"),
            Color.parseColor("#0A0A0A"), Color.parseColor("#FFC107")),
        // 3. Pop gelap
        Tpl("Pop Gelap", 2,
            Color.parseColor("#22222B"), Color.parseColor("#FFD54F"), Color.parseColor("#FF4081"),
            Color.parseColor("#22222B"), Color.parseColor("#00E5FF"), Color.parseColor("#B455FF"),
            Color.parseColor("#2E2E3A"), Color.parseColor("#FF9800"), Color.parseColor("#FFD54F"),
            Color.parseColor("#FFFFFF"), Color.parseColor("#FFD54F"))
    )

    private const val BW = 1200f   // lebar dasar
    private const val BH = 232f    // tinggi dasar

    fun render(t: Tpl, leftName: String, rightName: String, bottom: String, nameColor: Int, bottomColor: Int, width: Int = 1200): Bitmap {
        val k = width / BW
        val bmp = Bitmap.createBitmap(width, (BH * k).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.scale(k, k)

        val lb = RectF(6f, 4f, 406f, 102f)
        val rb = RectF(794f, 4f, 1194f, 102f)
        val bb = RectF(385f, 120f, 815f, 224f)

        panel(cv, lb, t.leftBase, t.leftLine, t.leftDot, t, 0)
        panel(cv, rb, t.rightBase, t.rightLine, t.rightDot, t, 1)
        panel(cv, bb, t.botBase, t.botLine, t.botDot, t, 2)
        stars(cv, t, bb)

        val l = leftName.trim().uppercase(); val r = rightName.trim().uppercase(); val b = bottom.trim().uppercase()
        text(cv, l, lb.centerX(), lb.centerY(), lb.width() - 50f, 86f, nameColor)
        text(cv, r, rb.centerX(), rb.centerY(), rb.width() - 50f, 86f, nameColor)
        text(cv, b, bb.centerX(), bb.centerY() + 8f, bb.width() - 70f, 80f, bottomColor)
        return bmp
    }

    // ---------- papan ----------
    private fun panel(cv: Canvas, r: RectF, base: Int, line: Int, dot: Int, t: Tpl, seed: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL; p.color = Color.argb(110, 0, 0, 0)
        cv.drawRect(r.left + 5f, r.top + 6f, r.right + 5f, r.bottom + 6f, p)

        cv.save(); cv.clipRect(r)
        p.color = base; cv.drawRect(r, p)
        when (t.style) {
            0 -> { burst(cv, r, line, seed); dots(cv, r, dot, true) }
            1 -> { speed(cv, r, line, seed); dots(cv, r, dot, seed == 0) }
            else -> { stripes(cv, r, line); dots(cv, r, dot, false) }
        }
        cv.restore()

        p.style = Paint.Style.STROKE; p.strokeJoin = Paint.Join.MITER
        if (t.border == Color.WHITE || t.border == Color.parseColor("#FFFFFF")) {
            p.strokeWidth = 9f; p.color = Color.BLACK; cv.drawRect(r, p)
            p.strokeWidth = 4f; p.color = t.border; cv.drawRect(r, p)
        } else {
            p.strokeWidth = 6f; p.color = t.border; cv.drawRect(r, p)
        }
    }

    private fun burst(cv: Canvas, r: RectF, color: Int, seed: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)) }
        val cx = r.centerX() + if (seed == 0) -r.width() * 0.2f else r.width() * 0.2f
        val cy = r.centerY()
        val big = r.width() * 2f
        for (i in 0 until 44) {
            val a = Math.toRadians(i * 360.0 / 44)
            val w = Math.toRadians(if (i % 3 == 0) 3.2 else 1.6)
            val path = Path()
            path.moveTo(cx, cy)
            path.lineTo(cx + (big * cos(a - w)).toFloat(), cy + (big * sin(a - w)).toFloat())
            path.lineTo(cx + (big * cos(a + w)).toFloat(), cy + (big * sin(a + w)).toFloat())
            path.close(); cv.drawPath(path, p)
        }
    }

    private fun speed(cv: Canvas, r: RectF, color: Int, seed: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; this.color = Color.argb(170, Color.red(color), Color.green(color), Color.blue(color)) }
        val n = 15
        for (i in 0 until n) {
            val y = r.top + (i + 0.5f) * r.height() / n
            p.strokeWidth = 2f + (i * 7 % 5) * 1.5f
            val off = ((i * 37 + seed * 11) % 100) / 100f
            val x0 = r.left + off * r.width() * 0.55f
            cv.drawLine(x0, y, r.right + 10f, y, p)
        }
    }

    private fun stripes(cv: Canvas, r: RectF, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; this.color = Color.argb(120, Color.red(color), Color.green(color), Color.blue(color)); strokeWidth = 8f }
        var x = r.left - r.height()
        while (x < r.right) { cv.drawLine(x, r.bottom, x + r.height(), r.top, p); x += 28f }
    }

    private fun dots(cv: Canvas, r: RectF, color: Int, graded: Boolean) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)) }
        val sp = 14f
        var row = 0
        var y = r.top + 4f
        while (y < r.bottom) {
            var x = r.left + if (row % 2 == 0) 4f else 4f + sp / 2f
            while (x < r.right) {
                val f = if (graded) Math.pow(((x - r.left) / r.width()).toDouble(), 2.2).toFloat() else 0.3f
                val rad = 5.2f * f
                if (rad > 0.7f) cv.drawCircle(x, y, rad, p)
                x += sp
            }
            y += sp * 0.5f; row++
        }
    }

    // ---------- bintang ----------
    private fun stars(cv: Canvas, t: Tpl, bb: RectF) {
        val cx = bb.centerX()
        val dx = floatArrayOf(-104f, -52f, 0f, 52f, 104f)
        val dy = floatArrayOf(10f, -6f, -20f, -6f, 10f)
        val rad = floatArrayOf(25f, 31f, 40f, 31f, 25f)
        val rot = floatArrayOf(-18f, -9f, 0f, 9f, 18f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until 5) {
            val path = starPath(cx + dx[i], bb.top + dy[i], rad[i], rot[i])
            p.style = Paint.Style.STROKE; p.strokeJoin = Paint.Join.ROUND; p.strokeWidth = 8f; p.color = Color.argb(240, 0, 0, 0)
            cv.drawPath(path, p)
            p.style = Paint.Style.FILL; p.color = t.star
            cv.drawPath(path, p)
            p.style = Paint.Style.FILL; p.color = Color.argb(90, 255, 255, 255)
            cv.drawCircle(cx + dx[i] - rad[i] * 0.18f, bb.top + dy[i] - rad[i] * 0.2f, rad[i] * 0.18f, p)
        }
    }

    private fun starPath(cx: Float, cy: Float, r: Float, rotDeg: Float): Path {
        val path = Path()
        val rot = Math.toRadians(rotDeg.toDouble())
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) r else r * 0.46f
            val a = -Math.PI / 2 + i * Math.PI / 5 + rot
            val x = cx + (rr * cos(a)).toFloat(); val y = cy + (rr * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close(); return path
    }

    // ---------- teks ----------
    private fun text(cv: Canvas, s: String, cx: Float, cy: Float, maxW: Float, startSize: Float, color: Int) {
        if (s.isBlank()) return
        val tp = Paint(Paint.ANTI_ALIAS_FLAG)
        tp.typeface = Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC)
        var size = startSize
        tp.textSize = size
        while (tp.measureText(s) > maxW && size > 18f) { size -= 2f; tp.textSize = size }
        val fm = tp.fontMetrics
        val y = cy - (fm.ascent + fm.descent) / 2f
        val w = tp.measureText(s)
        val lum = 0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)
        tp.style = Paint.Style.STROKE; tp.strokeJoin = Paint.Join.ROUND; tp.strokeWidth = size * 0.2f
        tp.color = if (lum < 70f) Color.WHITE else Color.argb(245, 0, 0, 0)
        cv.drawText(s, cx - w / 2f, y, tp)
        tp.style = Paint.Style.FILL; tp.color = color
        cv.drawText(s, cx - w / 2f, y, tp)
    }
}
