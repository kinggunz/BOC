package com.boc.laga

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Pratinjau banyak overlay: ketuk = pilih, geser = posisi, cubit = ukuran (5%..300%). */
class PreviewView(c: Context, private val aspect: Float) : View(c) {
    companion object { const val MAX_SCALE = 300f; const val MIN_SCALE = 5f }
    class Item(var bmp: Bitmap, val layer: Layer) {
        var frames: List<java.io.File> = emptyList()   // frame video untuk diputar
        var idx = 0
        var cur: Bitmap? = null
    }

    var items = mutableListOf<Item>()
    var active = 0
    var bg: Bitmap? = null            // gambar latar referensi (hanya di editor)
    var landscape = false
    var fullScreen = false
    var interactive = true
    var onChange: (() -> Unit)? = null
    var onSelect: (() -> Unit)? = null

    private var sf = 50f
    private val frame = RectF()
    private val d = resources.displayMetrics.density
    private val pBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E1E1E") }
    private val pEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935"); style = Paint.Style.STROKE; strokeWidth = 3 * d }
    private val pImg = Paint(Paint.FILTER_BITMAP_FLAG)
    private val pBox = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1.5f * d
        pathEffect = DashPathEffect(floatArrayOf(10 * d, 8 * d), 0f); alpha = 170 }
    private val pGuide = Paint().apply { color = Color.parseColor("#FFEB3B"); strokeWidth = 1.5f * d }
    private val pFaint = Paint().apply { color = Color.parseColor("#33FFFFFF"); strokeWidth = 1f }
    private var snapX = false
    private var snapY = false
    private var lx = 0f
    private var ly = 0f

    private fun cur() = items.getOrNull(active)

    private val tick = object : Runnable {
        override fun run() {
            var any = false
            for (item in items) {
                if (item.frames.size > 1) {
                    item.idx = (item.idx + 1) % item.frames.size
                    val b = try { BitmapFactory.decodeFile(item.frames[item.idx].path) } catch (e: Exception) { null }
                    if (b != null) item.cur = b
                    any = true
                }
            }
            if (any) invalidate()
            postDelayed(this, 100)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); postDelayed(tick, 100) }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }

    fun selectItem(i: Int) {
        if (items.isEmpty()) { active = 0; return }
        active = i.coerceIn(0, items.size - 1)
        sf = items[active].layer.scale.toFloat()
        invalidate()
    }

    fun setScale(v: Int) { sf = v.toFloat(); cur()?.layer?.scale = v; invalidate() }

    private val sd = ScaleGestureDetector(c, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(det: ScaleGestureDetector): Boolean {
            val it = cur() ?: return true
            sf = (sf * det.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
            it.layer.scale = sf.toInt()
            invalidate(); onChange?.invoke(); return true
        }
    })

    private fun calcFrame() {
        if (fullScreen) { frame.set(0f, 0f, width.toFloat(), height.toFloat()); return }
        val ar = if (landscape) aspect else 1f / aspect
        val pad = 8 * d
        var w = width - 2 * pad; var h = height - 2 * pad
        if (w / ar > h) w = h * ar else h = w / ar
        val cx = width / 2f; val cy = height / 2f
        frame.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
    }

    private fun imgRect(it: Item): RectF {
        val b = it.bmp; val l = it.layer
        val box = min(frame.width(), frame.height()) * l.scale / 100f
        val r = min(box / b.width, box / b.height)
        val w = b.width * r; val h = b.height * r
        val cx = frame.left + l.x(landscape) * frame.width()
        val cy = frame.top + l.y(landscape) * frame.height()
        return RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
    }

    override fun onDraw(c: Canvas) {
        calcFrame()
        val rad = if (fullScreen) 0f else 18 * d
        c.drawRoundRect(frame, rad, rad, pBg)
        c.save(); c.clipRect(frame)
        bg?.let {
            val s = max(frame.width() / it.width, frame.height() / it.height)
            val sw = frame.width() / s; val sh = frame.height() / s
            val src = Rect(((it.width - sw) / 2).toInt(), ((it.height - sh) / 2).toInt(),
                ((it.width + sw) / 2).toInt(), ((it.height + sh) / 2).toInt())
            c.drawBitmap(it, src, frame, pImg)
        }
        c.drawLine(frame.centerX(), frame.top, frame.centerX(), frame.bottom, pFaint)
        c.drawLine(frame.left, frame.centerY(), frame.right, frame.centerY(), pFaint)
        items.forEachIndexed { i, it ->
            val r = imgRect(it)
            c.drawBitmap(it.cur ?: it.bmp, null, r, pImg)
            if (i == active && interactive) c.drawRect(r, pBox)
        }
        if (snapX) c.drawLine(frame.centerX(), frame.top, frame.centerX(), frame.bottom, pGuide)
        if (snapY) c.drawLine(frame.left, frame.centerY(), frame.right, frame.centerY(), pGuide)
        c.restore()
        if (!fullScreen) c.drawRoundRect(frame, rad, rad, pEdge)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!interactive) return super.onTouchEvent(e)
        parent?.requestDisallowInterceptTouchEvent(true)
        calcFrame()
        sd.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lx = e.x; ly = e.y
                for (i in items.indices.reversed()) {
                    if (imgRect(items[i]).contains(e.x, e.y)) {
                        if (i != active) { selectItem(i); onSelect?.invoke() }
                        break
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> { lx = e.x; ly = e.y }
            MotionEvent.ACTION_POINTER_UP -> { val i = if (e.actionIndex == 0) 1 else 0; lx = e.getX(i); ly = e.getY(i) }
            MotionEvent.ACTION_MOVE -> {
                val it = cur()
                if (it != null && !sd.isInProgress && e.pointerCount == 1 && frame.width() > 0) {
                    val l = it.layer
                    var nx = (l.x(landscape) + (e.x - lx) / frame.width()).coerceIn(0f, 1f)
                    var ny = (l.y(landscape) + (e.y - ly) / frame.height()).coerceIn(0f, 1f)
                    lx = e.x; ly = e.y
                    snapX = abs(nx - 0.5f) < 0.02f; if (snapX) nx = 0.5f
                    snapY = abs(ny - 0.5f) < 0.02f; if (snapY) ny = 0.5f
                    l.setPos(landscape, nx, ny)
                    invalidate(); onChange?.invoke()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { snapX = false; snapY = false; invalidate() }
        }
        return true
    }
}
