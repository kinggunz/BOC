package com.boc.laga

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout

/** Ikon yang digambar sendiri dengan Canvas (tanpa emoji, tanpa file gambar). */
enum class Ic { NONE, PLUS, TRASH, PLAY, STOP, IMAGE, GAME, MOVE, CHECK, LEFT, RIGHT, EYE, PHONE, LAND, LAUNCH, CANVAS }

object Cv {
    fun dp(c: Context, v: Float): Float = v * c.resources.displayMetrics.density

    fun shade(color: Int, f: Float): Int =
        Color.argb(Color.alpha(color), (Color.red(color) * f).toInt().coerceIn(0, 255),
            (Color.green(color) * f).toInt().coerceIn(0, 255), (Color.blue(color) * f).toInt().coerceIn(0, 255))

    /** Gambar ikon berpusat di (cx, cy), s = setengah ukuran. Warna diambil dari p.color. */
    fun icon(cv: Canvas, ic: Ic, cx: Float, cy: Float, s: Float, p: Paint) {
        val oldStyle = p.style; val oldW = p.strokeWidth
        p.strokeWidth = s * 0.28f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        val path = Path()
        val r = RectF()
        when (ic) {
            Ic.PLUS -> {
                p.style = Paint.Style.STROKE
                cv.drawLine(cx - s * 0.8f, cy, cx + s * 0.8f, cy, p)
                cv.drawLine(cx, cy - s * 0.8f, cx, cy + s * 0.8f, p)
            }
            Ic.TRASH -> {
                p.style = Paint.Style.STROKE
                cv.drawLine(cx - s * 0.85f, cy - s * 0.55f, cx + s * 0.85f, cy - s * 0.55f, p)
                cv.drawLine(cx - s * 0.3f, cy - s * 0.55f, cx - s * 0.3f, cy - s * 0.9f, p)
                cv.drawLine(cx + s * 0.3f, cy - s * 0.55f, cx + s * 0.3f, cy - s * 0.9f, p)
                cv.drawLine(cx - s * 0.3f, cy - s * 0.9f, cx + s * 0.3f, cy - s * 0.9f, p)
                path.moveTo(cx - s * 0.65f, cy - s * 0.55f)
                path.lineTo(cx - s * 0.5f, cy + s * 0.9f)
                path.lineTo(cx + s * 0.5f, cy + s * 0.9f)
                path.lineTo(cx + s * 0.65f, cy - s * 0.55f)
                cv.drawPath(path, p)
            }
            Ic.PLAY -> {
                p.style = Paint.Style.FILL
                path.moveTo(cx - s * 0.6f, cy - s * 0.85f)
                path.lineTo(cx + s * 0.9f, cy)
                path.lineTo(cx - s * 0.6f, cy + s * 0.85f)
                path.close(); cv.drawPath(path, p)
            }
            Ic.STOP -> {
                p.style = Paint.Style.FILL
                r.set(cx - s * 0.7f, cy - s * 0.7f, cx + s * 0.7f, cy + s * 0.7f)
                cv.drawRoundRect(r, s * 0.2f, s * 0.2f, p)
            }
            Ic.IMAGE -> {
                p.style = Paint.Style.STROKE
                r.set(cx - s * 0.95f, cy - s * 0.8f, cx + s * 0.95f, cy + s * 0.8f)
                cv.drawRoundRect(r, s * 0.25f, s * 0.25f, p)
                path.moveTo(cx - s * 0.95f, cy + s * 0.5f)
                path.lineTo(cx - s * 0.25f, cy - s * 0.1f)
                path.lineTo(cx + s * 0.25f, cy + s * 0.35f)
                path.lineTo(cx + s * 0.5f, cy + s * 0.1f)
                path.lineTo(cx + s * 0.95f, cy + s * 0.5f)
                cv.drawPath(path, p)
                p.style = Paint.Style.FILL
                cv.drawCircle(cx + s * 0.4f, cy - s * 0.35f, s * 0.15f, p)
            }
            Ic.GAME -> {
                p.style = Paint.Style.STROKE
                r.set(cx - s * 1.0f, cy - s * 0.6f, cx + s * 1.0f, cy + s * 0.6f)
                cv.drawRoundRect(r, s * 0.55f, s * 0.55f, p)
                cv.drawLine(cx - s * 0.6f, cy, cx - s * 0.2f, cy, p)
                cv.drawLine(cx - s * 0.4f, cy - s * 0.2f, cx - s * 0.4f, cy + s * 0.2f, p)
                p.style = Paint.Style.FILL
                cv.drawCircle(cx + s * 0.3f, cy, s * 0.12f, p)
                cv.drawCircle(cx + s * 0.6f, cy - s * 0.15f, s * 0.12f, p)
            }
            Ic.MOVE -> {
                p.style = Paint.Style.STROKE
                cv.drawLine(cx - s, cy, cx + s, cy, p)
                cv.drawLine(cx, cy - s, cx, cy + s, p)
                val a = s * 0.35f
                chevron(cv, p, cx, cy - s, 0f, -1f, a); chevron(cv, p, cx, cy + s, 0f, 1f, a)
                chevron(cv, p, cx - s, cy, -1f, 0f, a); chevron(cv, p, cx + s, cy, 1f, 0f, a)
            }
            Ic.CHECK -> {
                p.style = Paint.Style.STROKE
                path.moveTo(cx - s * 0.85f, cy + s * 0.05f)
                path.lineTo(cx - s * 0.25f, cy + s * 0.65f)
                path.lineTo(cx + s * 0.9f, cy - s * 0.6f)
                cv.drawPath(path, p)
            }
            Ic.LEFT -> {
                p.style = Paint.Style.STROKE
                path.moveTo(cx + s * 0.4f, cy - s * 0.8f); path.lineTo(cx - s * 0.4f, cy); path.lineTo(cx + s * 0.4f, cy + s * 0.8f)
                cv.drawPath(path, p)
            }
            Ic.RIGHT -> {
                p.style = Paint.Style.STROKE
                path.moveTo(cx - s * 0.4f, cy - s * 0.8f); path.lineTo(cx + s * 0.4f, cy); path.lineTo(cx - s * 0.4f, cy + s * 0.8f)
                cv.drawPath(path, p)
            }
            Ic.EYE -> {
                p.style = Paint.Style.STROKE
                r.set(cx - s * 1.0f, cy - s * 0.6f, cx + s * 1.0f, cy + s * 0.6f)
                cv.drawOval(r, p)
                p.style = Paint.Style.FILL
                cv.drawCircle(cx, cy, s * 0.25f, p)
            }
            Ic.PHONE -> {
                p.style = Paint.Style.STROKE
                r.set(cx - s * 0.6f, cy - s * 0.95f, cx + s * 0.6f, cy + s * 0.95f)
                cv.drawRoundRect(r, s * 0.25f, s * 0.25f, p)
                cv.drawLine(cx - s * 0.15f, cy + s * 0.65f, cx + s * 0.15f, cy + s * 0.65f, p)
            }
            Ic.LAND -> {
                p.style = Paint.Style.STROKE
                r.set(cx - s * 0.95f, cy - s * 0.6f, cx + s * 0.95f, cy + s * 0.6f)
                cv.drawRoundRect(r, s * 0.25f, s * 0.25f, p)
                cv.drawLine(cx + s * 0.65f, cy - s * 0.15f, cx + s * 0.65f, cy + s * 0.15f, p)
            }
            Ic.LAUNCH -> {
                p.style = Paint.Style.STROKE
                cv.drawCircle(cx, cy, s * 0.95f, p)
                p.style = Paint.Style.FILL
                path.moveTo(cx - s * 0.3f, cy - s * 0.5f)
                path.lineTo(cx + s * 0.55f, cy)
                path.lineTo(cx - s * 0.3f, cy + s * 0.5f)
                path.close(); cv.drawPath(path, p)
            }
            Ic.CANVAS -> {
                p.style = Paint.Style.STROKE
                r.set(cx - s * 1.0f, cy - s * 0.85f, cx + s * 1.0f, cy + s * 0.25f)
                cv.drawRoundRect(r, s * 0.2f, s * 0.2f, p)
                cv.drawLine(cx, cy - s * 0.85f, cx, cy + s * 0.25f, p)
                r.set(cx - s * 0.55f, cy + s * 0.55f, cx + s * 0.55f, cy + s * 0.95f)
                cv.drawRoundRect(r, s * 0.2f, s * 0.2f, p)
            }
            Ic.NONE -> {}
        }
        p.style = oldStyle; p.strokeWidth = oldW
    }

    private fun chevron(cv: Canvas, p: Paint, x: Float, y: Float, dx: Float, dy: Float, a: Float) {
        // kepala panah searah (dx, dy) di titik (x, y)
        val px = -dy; val py = dx
        cv.drawLine(x, y, x - dx * a + px * a, y - dy * a + py * a, p)
        cv.drawLine(x, y, x - dx * a - px * a, y - dy * a - py * a, p)
    }
}

/** Tombol yang seluruh tampilannya digambar dengan Canvas. */
class CButton(
    ctx: Context, label: String = "", icon: Ic = Ic.NONE, fill: Int = Color.parseColor("#2A2A2E"),
    private val heightDp: Float = 48f, private val textSp: Float = 14f
) : View(ctx) {
    var label: String = label; set(v) { field = v; invalidate() }
    var icon: Ic = icon; set(v) { field = v; invalidate() }
    var fill: Int = fill; set(v) { field = v; invalidate() }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }
    private val rect = RectF()
    private var shine: Shader? = null

    init { isClickable = true; isFocusable = true }

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(getDefaultSize(Cv.dp(context, 100f).toInt(), w), Cv.dp(context, heightDp).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        shine = LinearGradient(0f, 0f, 0f, h * 0.6f, Color.argb(46, 255, 255, 255), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP)
    }

    override fun drawableStateChanged() { super.drawableStateChanged(); invalidate() }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val lip = Cv.dp(context, 2.5f)
        val rad = h * 0.28f
        val down = isPressed
        val top = if (down) lip * 0.6f else 0f
        // alas (bayangan tebal di bawah)
        p.style = Paint.Style.FILL; p.shader = null
        p.color = Cv.shade(fill, 0.55f)
        rect.set(0f, lip, w, h); cv.drawRoundRect(rect, rad, rad, p)
        // permukaan
        p.color = if (down) Cv.shade(fill, 0.85f) else fill
        rect.set(0f, top, w, h - lip + top); cv.drawRoundRect(rect, rad, rad, p)
        // kilau atas
        p.shader = shine; cv.save(); cv.translate(0f, top); cv.drawRoundRect(rect.apply { offset(0f, -top) }, rad, rad, p); cv.restore(); p.shader = null
        // garis tepi
        p.style = Paint.Style.STROKE; p.strokeWidth = Cv.dp(context, 1f); p.color = Color.argb(60, 255, 255, 255)
        rect.set(0f, top, w, h - lip + top); rect.inset(p.strokeWidth / 2, p.strokeWidth / 2)
        cv.drawRoundRect(rect, rad, rad, p)

        // isi: ikon + teks
        val cy = (h - lip) / 2f + top
        val icS = Cv.dp(context, 9f)
        val gap = Cv.dp(context, 8f)
        var size = Cv.dp(context, textSp) * 1f
        tp.textSize = size
        val maxW = w - Cv.dp(context, 20f) - (if (icon != Ic.NONE) icS * 2 + gap else 0f)
        while (label.isNotEmpty() && tp.measureText(label) > maxW && size > Cv.dp(context, 9f)) { size -= 1f; tp.textSize = size }
        val tw = if (label.isEmpty()) 0f else tp.measureText(label)
        val iw = if (icon != Ic.NONE) icS * 2 else 0f
        val total = iw + (if (iw > 0 && tw > 0) gap else 0f) + tw
        var x = (w - total) / 2f
        p.color = Color.WHITE; p.style = Paint.Style.STROKE
        if (icon != Ic.NONE) { Cv.icon(cv, icon, x + icS, cy, icS, p); x += iw + (if (tw > 0) gap else 0f) }
        if (tw > 0) {
            tp.color = Color.WHITE
            val fm = tp.fontMetrics
            cv.drawText(label, x, cy - (fm.ascent + fm.descent) / 2f, tp)
        }
    }
}

/** Saklar digambar sendiri: kartu gelap + teks + sakelar. */
class CToggle(ctx: Context, private val label: String) : View(ctx) {
    var checked: Boolean = false; set(v) { field = v; invalidate() }
    var onChange: ((Boolean) -> Unit)? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = Cv.dp(ctx, 14f) }
    private val rect = RectF()

    init {
        isClickable = true
        setOnClickListener { checked = !checked; onChange?.invoke(checked) }
    }

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(getDefaultSize(Cv.dp(context, 100f).toInt(), w), Cv.dp(context, 56f).toInt())
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.style = Paint.Style.FILL; p.color = Color.parseColor("#1B1B1F")
        rect.set(0f, 0f, w, h); cv.drawRoundRect(rect, h * 0.28f, h * 0.28f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = Cv.dp(context, 1f); p.color = Color.parseColor("#2C2C32")
        rect.inset(p.strokeWidth / 2, p.strokeWidth / 2); cv.drawRoundRect(rect, h * 0.28f, h * 0.28f, p)

        val tw = Cv.dp(context, 46f); val th = Cv.dp(context, 26f)
        val pad = Cv.dp(context, 16f)
        val tx = w - pad - tw; val ty = (h - th) / 2f
        p.style = Paint.Style.FILL
        p.color = if (checked) Color.parseColor("#E53935") else Color.parseColor("#3A3A40")
        rect.set(tx, ty, tx + tw, ty + th); cv.drawRoundRect(rect, th / 2, th / 2, p)
        val kr = th / 2 - Cv.dp(context, 3f)
        val kx = if (checked) tx + tw - th / 2 else tx + th / 2
        p.color = Color.WHITE; cv.drawCircle(kx, ty + th / 2, kr, p)

        val avail = tx - pad * 1.5f
        val t = TextUtils.ellipsize(label, android.text.TextPaint(tp), avail, TextUtils.TruncateAt.END).toString()
        val fm = tp.fontMetrics
        cv.drawText(t, pad, h / 2f - (fm.ascent + fm.descent) / 2f, tp)
    }
}

/** Kotak info satu baris dengan ikon (mis. game terpilih). */
class CInfo(ctx: Context, private val ic: Ic) : View(ctx) {
    var text: String = ""; set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CFCFD4"); textSize = Cv.dp(ctx, 13f) }
    private val rect = RectF()

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(getDefaultSize(Cv.dp(context, 100f).toInt(), w), Cv.dp(context, 46f).toInt())
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.style = Paint.Style.FILL; p.color = Color.parseColor("#17171B")
        rect.set(0f, 0f, w, h); cv.drawRoundRect(rect, h * 0.3f, h * 0.3f, p)
        val s = Cv.dp(context, 9f); val pad = Cv.dp(context, 14f)
        p.style = Paint.Style.STROKE; p.color = Color.parseColor("#E53935")
        Cv.icon(cv, ic, pad + s, h / 2f, s, p)
        val x = pad + s * 2 + Cv.dp(context, 10f)
        val t = TextUtils.ellipsize(text, android.text.TextPaint(tp), w - x - pad, TextUtils.TruncateAt.END).toString()
        val fm = tp.fontMetrics
        cv.drawText(t, x, h / 2f - (fm.ascent + fm.descent) / 2f, tp)
    }
}

/** Judul bagian: batang merah kecil + teks. */
class CCaption(ctx: Context, private val text: String) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9A9AA2"); textSize = Cv.dp(ctx, 12f); typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.12f
    }
    private val rect = RectF()

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(getDefaultSize(Cv.dp(context, 100f).toInt(), w), Cv.dp(context, 30f).toInt())
    }

    override fun onDraw(cv: Canvas) {
        val h = height.toFloat()
        p.style = Paint.Style.FILL; p.color = Color.parseColor("#E53935")
        rect.set(Cv.dp(context, 2f), h * 0.3f, Cv.dp(context, 6f), h * 0.7f)
        cv.drawRoundRect(rect, Cv.dp(context, 2f), Cv.dp(context, 2f), p)
        val fm = tp.fontMetrics
        cv.drawText(text.uppercase(), Cv.dp(context, 14f), h / 2f - (fm.ascent + fm.descent) / 2f, tp)
    }
}

/** Kepala halaman: logo + judul + subjudul, digambar di Canvas. */
class CHeader(ctx: Context, private val title: String, private val sub: String) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val logo = ctx.getDrawable(R.drawable.ic_logo)

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(getDefaultSize(Cv.dp(context, 100f).toInt(), w), Cv.dp(context, 88f).toInt())
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val rad = Cv.dp(context, 20f)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, w, h, Color.parseColor("#2A1416"), Color.parseColor("#17171B"), Shader.TileMode.CLAMP)
        rect.set(0f, 0f, w, h); cv.drawRoundRect(rect, rad, rad, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = Cv.dp(context, 1f); p.color = Color.parseColor("#3A2326")
        rect.inset(p.strokeWidth / 2, p.strokeWidth / 2); cv.drawRoundRect(rect, rad, rad, p)

        val ls = Cv.dp(context, 56f).toInt(); val lx = Cv.dp(context, 16f).toInt(); val ly = ((h - ls) / 2f).toInt()
        logo?.setBounds(lx, ly, lx + ls, ly + ls); logo?.draw(cv)

        val x = lx + ls + Cv.dp(context, 14f)
        tp.typeface = Typeface.DEFAULT_BOLD; tp.color = Color.WHITE; tp.textSize = Cv.dp(context, 21f)
        cv.drawText(title, x, h / 2f - Cv.dp(context, 2f), tp)
        tp.typeface = Typeface.DEFAULT; tp.color = Color.parseColor("#A9A9B0"); tp.textSize = Cv.dp(context, 12.5f)
        cv.drawText(sub, x, h / 2f + Cv.dp(context, 18f), tp)
    }
}

/** Wadah vertikal dengan kartu bergaya yang digambar di Canvas. */
class CPanel(ctx: Context, private val fillColor: Int = Color.parseColor("#1B1B1F"), private val border: Int = Color.parseColor("#2C2C32")) : LinearLayout(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        orientation = VERTICAL
        setWillNotDraw(false)
        val pad = Cv.dp(ctx, 12f).toInt(); setPadding(pad, pad, pad, pad)
    }

    override fun onDraw(cv: Canvas) {
        val rad = Cv.dp(context, 20f)
        p.style = Paint.Style.FILL; p.color = fillColor
        rect.set(0f, 0f, width.toFloat(), height.toFloat()); cv.drawRoundRect(rect, rad, rad, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = Cv.dp(context, 1f); p.color = border
        rect.inset(p.strokeWidth / 2, p.strokeWidth / 2); cv.drawRoundRect(rect, rad, rad, p)
    }
}

/** Daftar template: tiap template digambar sebagai gambar kecil, yang dipilih diberi bingkai. */
class CTemplatePicker(ctx: Context) : View(ctx) {
    var bitmaps: List<android.graphics.Bitmap> = emptyList(); set(v) { field = v; requestLayout(); invalidate() }
    var names: List<String> = emptyList()
    var selected: Int = 0; set(v) { field = v; invalidate() }
    var onSelect: ((Int) -> Unit)? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = Cv.dp(ctx, 12f); typeface = Typeface.DEFAULT_BOLD }
    private val rect = RectF()
    private val imgPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private fun itemH(w: Float): Float {
        val b = bitmaps.firstOrNull() ?: return Cv.dp(context, 60f)
        return (w - Cv.dp(context, 16f)) * b.height / b.width + Cv.dp(context, 16f)
    }
    private fun block(w: Float) = itemH(w) + Cv.dp(context, 26f)

    override fun onMeasure(w: Int, h: Int) {
        val ww = MeasureSpec.getSize(w).toFloat()
        setMeasuredDimension(getDefaultSize(Cv.dp(context, 100f).toInt(), w),
            (block(ww) * maxOf(1, bitmaps.size)).toInt())
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val ih = itemH(w); val bl = block(w)
        for (i in bitmaps.indices) {
            val top = i * bl
            val sel = i == selected
            p.style = Paint.Style.FILL; p.color = Color.parseColor(if (sel) "#201417" else "#141417")
            rect.set(0f, top, w, top + ih); cv.drawRoundRect(rect, Cv.dp(context, 14f), Cv.dp(context, 14f), p)
            val pad = Cv.dp(context, 8f)
            val img = RectF(pad, top + pad, w - pad, top + ih - pad)
            imgPaint.alpha = if (sel) 255 else 150
            cv.drawBitmap(bitmaps[i], null, img, imgPaint)
            p.style = Paint.Style.STROKE; p.strokeWidth = Cv.dp(context, if (sel) 2.5f else 1f)
            p.color = Color.parseColor(if (sel) "#E53935" else "#2C2C32")
            rect.set(0f, top, w, top + ih); rect.inset(p.strokeWidth / 2, p.strokeWidth / 2)
            cv.drawRoundRect(rect, Cv.dp(context, 14f), Cv.dp(context, 14f), p)
            tp.color = Color.parseColor(if (sel) "#FFFFFF" else "#8A8A92")
            val nm = (if (sel) "Dipilih  |  " else "") + names.getOrElse(i) { "" }
            cv.drawText(nm, Cv.dp(context, 6f), top + ih + Cv.dp(context, 17f), tp)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_UP && bitmaps.isNotEmpty()) {
            val i = (e.y / block(width.toFloat())).toInt()
            if (i in bitmaps.indices) { selected = i; onSelect?.invoke(i) }
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}

/** Deretan warna bulat untuk dipilih. */
class CSwatches(ctx: Context, private val colors: IntArray) : View(ctx) {
    var selected: Int = colors.firstOrNull() ?: Color.WHITE; set(v) { field = v; invalidate() }
    var onPick: ((Int) -> Unit)? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(getDefaultSize(Cv.dp(context, 100f).toInt(), w), Cv.dp(context, 52f).toInt())
    }

    private fun cx(i: Int): Float = width * (i + 0.5f) / colors.size

    override fun onDraw(cv: Canvas) {
        val r = minOf(Cv.dp(context, 17f), width / colors.size / 2f - Cv.dp(context, 3f))
        for (i in colors.indices) {
            p.style = Paint.Style.FILL; p.color = colors[i]
            cv.drawCircle(cx(i), height / 2f, r, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = Cv.dp(context, 1f); p.color = Color.argb(90, 255, 255, 255)
            cv.drawCircle(cx(i), height / 2f, r, p)
            if (colors[i] == selected) {
                p.strokeWidth = Cv.dp(context, 2.5f); p.color = Color.parseColor("#E53935")
                cv.drawCircle(cx(i), height / 2f, r + Cv.dp(context, 4f), p)
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            val i = (e.x / width * colors.size).toInt().coerceIn(0, colors.size - 1)
            selected = colors[i]; onPick?.invoke(colors[i]); performClick()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
