package com.boc.laga

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

/** Editor LAYAR PENUH: banyak overlay, gambar latar referensi, posisi & ukuran. */
class EditorActivity : Activity() {
    private val d by lazy { resources.displayMetrics.density }
    private fun dp(v: Int) = (v * d).toInt()
    private lateinit var pv: PreviewView
    private lateinit var seek: SeekBar
    private lateinit var lblSize: TextView
    private lateinit var lblLayer: TextView
    private lateinit var swRm: Switch
    private lateinit var panel: View
    private lateinit var bT: CButton
    private lateinit var bL: CButton
    private lateinit var layers: MutableList<Layer>
    private var land = false
    private var wasRunning = false
    private var uiBusy = false

    private fun rounded(c: String, r: Int = 12) = GradientDrawable().apply {
        setColor(Color.parseColor(c)); cornerRadius = r * d }

    private fun btn(t: String, ic: Ic = Ic.NONE, onClick: () -> Unit) =
        CButton(this, t, ic, Color.parseColor("#CC2A2A2E"), 40f, 13f).apply { setOnClickListener { onClick() } }
    private fun rowOf(vararg v: View) = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        v.forEach { addView(it, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        wasRunning = OverlayService.running
        if (wasRunning) stopService(Intent(this, OverlayService::class.java))

        layers = Util.layers(this)
        pv = PreviewView(this, 1f).apply {
            fullScreen = true
            bg = Util.loadBg(this@EditorActivity)
            layers.forEach { l -> Util.loadBitmap(this@EditorActivity, l)?.let { items.add(PreviewView.Item(it, l)) } }
            selectItem(0)
            onChange = { save(); refreshPanel() }
            onSelect = { refreshPanel() }
        }
        val root = FrameLayout(this)
        root.addView(pv, FrameLayout.LayoutParams(-1, -1))

        // ---- bar atas ----
        bT = btn("Tegak", Ic.PHONE) { setLand(false) }
        bL = btn("Landscape", Ic.LAND) { setLand(true) }
        val eye = btn("", Ic.EYE) { panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        val done = btn("Selesai", Ic.CHECK) { done() }.apply { fill = Color.parseColor("#CC2E7D32") }
        root.addView(rowOf(bT, bL, eye, done), FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply {
            setMargins(dp(10), dp(34), dp(10), 0) })

        // ---- panel bawah ----
        lblLayer = TextView(this).apply { setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.CENTER }
        val prev = btn("", Ic.LEFT) { cycle(-1) }
        val next = btn("", Ic.RIGHT) { cycle(1) }
        val layerRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(prev, LinearLayout.LayoutParams(dp(56), -2))
            addView(lblLayer, LinearLayout.LayoutParams(0, -2, 1f))
            addView(next, LinearLayout.LayoutParams(dp(56), -2))
        }
        swRm = Switch(this).apply {
            text = "Hapus background gambar ini"; setTextColor(Color.WHITE); textSize = 13f
            setOnCheckedChangeListener { _, on -> if (!uiBusy) toggleRm(on) }
        }
        lblSize = TextView(this).apply { setTextColor(Color.WHITE); textSize = 14f }
        seek = SeekBar(this).apply {
            min = PreviewView.MIN_SCALE.toInt(); max = PreviewView.MAX_SCALE.toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, v: Int, u: Boolean) {
                    if (u) { pv.setScale(v); save(); refreshPanel() }
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        }
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = rounded("#B0000000", 16)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            addView(TextView(this@EditorActivity).apply {
                text = "Ketuk gambar = pilih • Geser = posisi • Cubit/slider = ukuran"
                setTextColor(Color.parseColor("#BBBBBB")); textSize = 11f; gravity = Gravity.CENTER })
            addView(layerRow)
            addView(rowOf(btn("Overlay", Ic.PLUS) { pick(1) }, btn("Hapus", Ic.TRASH) { deleteActive() }, btn("Latar", Ic.IMAGE) { bgMenu() }))
            addView(swRm); addView(lblSize); addView(seek)
        }
        root.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            setMargins(dp(12), 0, dp(12), dp(24)) })

        setContentView(root)
        hideBars()
        setLand(false)
    }

    @Suppress("DEPRECATION")
    private fun hideBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    private fun setLand(l: Boolean) {
        land = l
        requestedOrientation = if (l) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        pv.landscape = l
        bT.fill = Color.parseColor(if (!l) "#E53935" else "#CC2A2A2E")
        bL.fill = Color.parseColor(if (l) "#E53935" else "#CC2A2A2E")
        refreshPanel(); pv.invalidate()
    }

    private fun refreshPanel() {
        val n = pv.items.size
        val it = pv.items.getOrNull(pv.active)
        lblLayer.text = if (it == null) "Belum ada overlay"
            else "Overlay ${pv.active + 1} / $n" + (if (it.layer.video) " (video)" else "")
        if (it == null) return
        swRm.visibility = if (it.layer.video) View.GONE else View.VISIBLE
        uiBusy = true
        swRm.isChecked = it.layer.removeBg
        seek.progress = it.layer.scale
        uiBusy = false
        lblSize.text = "Ukuran: ${it.layer.scale}%"
    }

    private fun save() { Util.saveLayers(this, layers) }

    private fun cycle(dir: Int) {
        val n = pv.items.size; if (n == 0) return
        pv.selectItem((pv.active + dir + n) % n); refreshPanel()
    }

    private fun toggleRm(on: Boolean) {
        val it = pv.items.getOrNull(pv.active) ?: return
        it.layer.removeBg = on
        Util.loadBitmap(this, it.layer)?.let { b -> it.bmp = b }
        save(); pv.invalidate()
    }

    private fun deleteActive() {
        val it = pv.items.getOrNull(pv.active) ?: return
        if (pv.items.size <= 1) { Toast.makeText(this, "Minimal harus ada 1 overlay", Toast.LENGTH_SHORT).show(); return }
        Util.deleteLayer(this, layers, it.layer)
        pv.items.removeAt(pv.active)
        pv.selectItem(0); refreshPanel()
    }

    private fun pick(code: Int) {
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
            if (code == 1) { type = "*/*"; putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")) }
            else type = "image/*"
        }, code)
    }

    private fun bgMenu() {
        if (!Util.hasBg(this)) { pick(2); return }
        AlertDialog.Builder(this).setTitle("Gambar latar").setItems(arrayOf("Ganti latar", "Hapus latar")) { _, w ->
            if (w == 0) pick(2) else { Util.clearBg(this); pv.bg = null; pv.invalidate() }
        }.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (res != RESULT_OK) return
        if (req == 1) {
            val l = Util.newLayer(this, layers, uri)
            val b = l?.let { Util.loadBitmap(this, it) }
            if (l == null || b == null) { Toast.makeText(this, "Gagal membuka file", Toast.LENGTH_SHORT).show(); return }
            pv.items.add(PreviewView.Item(b, l))
            pv.selectItem(pv.items.size - 1); refreshPanel()
        } else if (req == 2) {
            if (Util.setBg(this, uri)) { pv.bg = Util.loadBg(this); pv.invalidate() }
            else Toast.makeText(this, "Gagal membuka gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun done() {
        save()
        if (wasRunning) Util.startOverlay(this)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { done() }
}
