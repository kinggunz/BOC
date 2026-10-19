package com.boc.laga

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Menu utama = menu overlay. Semua tombol digambar dengan Canvas (lihat CanvasUi.kt). */
class MainActivity : Activity() {
    private val d by lazy { resources.displayMetrics.density }
    private fun dp(v: Int) = (v * d).toInt()
    private val prefs by lazy { Util.prefs(this) }
    private lateinit var pv: PreviewView
    private lateinit var gameInfo: CInfo

    private fun red() = Color.parseColor("#E53935")
    private fun lp(m: Int = 4) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { setMargins(dp(m), dp(m), dp(m), dp(m)) }
    private fun btn(t: String, ic: Ic, color: Int = Color.parseColor("#2A2A2E"), h: Float = 48f, onClick: () -> Unit) =
        CButton(this, t, ic, color, h, 14f).apply { setOnClickListener { onClick() } }
    private fun row(a: View, b: View) = LinearLayout(this).apply {
        addView(a, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
        addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val dm = resources.displayMetrics
        val aspect = maxOf(dm.widthPixels, dm.heightPixels).toFloat() / minOf(dm.widthPixels, dm.heightPixels)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(26), dp(14), dp(26))
        }
        col.addView(CHeader(this, "BOC Laga Overlay", "Atur gambar dan video di atas game"), lp(4))

        // pratinjau: ketuk untuk buka editor layar penuh
        val prevPanel = CPanel(this)
        prevPanel.addView(CCaption(this, "Pratinjau"))
        pv = PreviewView(this, aspect).apply { interactive = false; setOnClickListener { openEditor() } }
        prevPanel.addView(pv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300)).apply { topMargin = dp(4) })
        prevPanel.addView(btn("Atur Posisi dan Ukuran", Ic.MOVE, red(), 50f) { openEditor() }, lp(4))
        col.addView(prevPanel, lp(8))

        val setPanel = CPanel(this)
        setPanel.addView(CCaption(this, "Pengaturan"))
        setPanel.addView(row(btn("Tambah Media", Ic.IMAGE) {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"; putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")) }, 1)
        }, btn("Pilih Game", Ic.GAME) { pickGame() }))
        gameInfo = CInfo(this, Ic.GAME)
        setPanel.addView(gameInfo, lp(4))

        val auto = CToggle(this, "Langsung masuk game saat apk dibuka").apply { checked = prefs.getBoolean("auto", false) }
        auto.onChange = { on ->
            if (on && prefs.getString("pkg", null) == null) {
                Toast.makeText(this, "Pilih game dulu", Toast.LENGTH_SHORT).show(); auto.checked = false
            } else prefs.edit().putBoolean("auto", on).apply()
        }
        setPanel.addView(auto, lp(4))
        col.addView(setPanel, lp(8))

        val runPanel = CPanel(this)
        runPanel.addView(CCaption(this, "Jalankan"))
        runPanel.addView(row(btn("Play", Ic.PLAY, Color.parseColor("#2E7D32")) { play(false) },
            btn("Stop", Ic.STOP, Color.parseColor("#C62828")) { stopService(Intent(this, OverlayService::class.java)) }))
        runPanel.addView(btn("Mulai Game + Overlay", Ic.LAUNCH, red(), 56f) { play(true) }, lp(4))
        col.addView(runPanel, lp(8))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F0F12")); addView(col)
        })
        updateGameLabel()
    }

    override fun onResume() { super.onResume(); refreshThumb() }

    private fun openEditor() {
        if (Util.layers(this).isEmpty()) { Toast.makeText(this, "Tambah gambar/video dulu", Toast.LENGTH_SHORT).show(); return }
        startActivity(Intent(this, EditorActivity::class.java))
    }

    private fun refreshThumb() {
        pv.items = Util.layers(this).mapNotNull { l ->
            Util.loadBitmap(this, l)?.let { PreviewView.Item(it, l) }
        }.toMutableList()
        pv.landscape = false
        pv.invalidate()
    }

    private fun sync() { if (OverlayService.running) Util.startOverlay(this) }

    private fun updateGameLabel() {
        val n = prefs.getString("pkgName", null)
        gameInfo.text = if (n == null) "Game belum dipilih" else "Game: $n"
    }

    private fun pickGame() {
        val pm = packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(i, 0).filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        val adapter = object : BaseAdapter() {
            override fun getCount() = apps.size
            override fun getItem(p: Int) = apps[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, v: View?, g: ViewGroup?): View {
                val r = LinearLayout(this@MainActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8))
                }
                r.addView(ImageView(this@MainActivity).apply { setImageDrawable(apps[p].loadIcon(pm)) },
                    LinearLayout.LayoutParams(dp(40), dp(40)))
                r.addView(TextView(this@MainActivity).apply {
                    text = apps[p].loadLabel(pm); textSize = 16f; setPadding(dp(12), 0, 0, 0)
                })
                return r
            }
        }
        AlertDialog.Builder(this).setTitle("Pilih Game / Aplikasi").setAdapter(adapter) { _, w ->
            prefs.edit().putString("pkg", apps[w].activityInfo.packageName)
                .putString("pkgName", apps[w].loadLabel(pm).toString()).apply()
            updateGameLabel()
        }.show()
    }

    private fun play(withGame: Boolean) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan 'Tampil di atas aplikasi lain'", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Util.layers(this).isEmpty()) { Toast.makeText(this, "Tambah gambar/video dulu", Toast.LENGTH_SHORT).show(); return }
        if (withGame && prefs.getString("pkg", null) == null) {
            Toast.makeText(this, "Pilih game dulu", Toast.LENGTH_SHORT).show(); return
        }
        Util.startOverlay(this)
        if (withGame && !Util.launchGame(this)) Toast.makeText(this, "Game tidak ditemukan", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (req != 1 || res != RESULT_OK) return
        if (Util.newLayer(this, Util.layers(this), uri) == null)
            Toast.makeText(this, "Gagal membuka file", Toast.LENGTH_SHORT).show()
        else { refreshThumb(); sync() }
    }
}
