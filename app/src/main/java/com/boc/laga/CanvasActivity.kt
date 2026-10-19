package com.boc.laga

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

/** Menu Canvas: pilih template papan skor, ubah teks & warna, lalu pasang ke layar utama. */
class CanvasActivity : Activity() {
    private val prefs by lazy { Util.prefs(this) }
    private fun dp(v: Int) = Ui.dp(this, v)

    private lateinit var picker: CTemplatePicker
    private lateinit var etL: EditText
    private lateinit var etR: EditText
    private lateinit var etB: EditText
    private var sel = 0
    private var cName = Color.WHITE
    private var cBot = Color.parseColor("#F5E6C8")

    private val palette = intArrayOf(
        Color.WHITE, Color.parseColor("#FFEB3B"), Color.parseColor("#00E5FF"), Color.parseColor("#FF9800"),
        Color.parseColor("#76FF03"), Color.parseColor("#FF4081"), Color.parseColor("#F5E6C8"), Color.parseColor("#111111"))

    private fun lp(m: Int = 4) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { setMargins(dp(m), dp(m), dp(m), dp(m)) }

    private fun field(hint: String, text: String, max: Int): EditText =
        Ui.edit(this, hint, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS).apply {
            setText(text); filters = arrayOf(InputFilter.LengthFilter(max))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { rerender() }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        sel = prefs.getInt("tplIdx", 0).coerceIn(0, Templates.list.size - 1)
        cName = prefs.getInt("tplCN", cName)
        cBot = prefs.getInt("tplCB", cBot)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(26), dp(14), dp(26))
        }
        col.addView(CHeader(this, "Canvas", "Buat papan skor untuk overlay"), lp(4))

        val tpl = CPanel(this)
        tpl.addView(CCaption(this, "Pilih template"))
        picker = CTemplatePicker(this).apply {
            names = Templates.list.map { it.name }
            selected = sel
            onSelect = { i -> sel = i }
        }
        tpl.addView(picker, lp(4))
        col.addView(tpl, lp(8))

        val txt = CPanel(this)
        txt.addView(CCaption(this, "Teks"))
        etL = field("Nama tim kiri", prefs.getString("tplL", "RPJ") ?: "RPJ", 14)
        etR = field("Nama tim kanan", prefs.getString("tplR", "KARBU") ?: "KARBU", 14)
        etB = field("Teks bawah", prefs.getString("tplB", "BEST OF 3") ?: "BEST OF 3", 18)
        txt.addView(etL, lp(4)); txt.addView(etR, lp(4)); txt.addView(etB, lp(4))
        col.addView(txt, lp(8))

        val clr = CPanel(this)
        clr.addView(CCaption(this, "Warna nama tim"))
        clr.addView(CSwatches(this, palette).apply { selected = cName; onPick = { c -> cName = c; rerender() } }, lp(2))
        clr.addView(CCaption(this, "Warna teks bawah"))
        clr.addView(CSwatches(this, palette).apply { selected = cBot; onPick = { c -> cBot = c; rerender() } }, lp(2))
        col.addView(clr, lp(8))

        val act = CPanel(this)
        act.addView(CButton(this, "Gunakan di Layar Utama", Ic.CHECK, Color.parseColor("#E53935"), 54f, 15f)
            .apply { setOnClickListener { use() } }, lp(4))
        act.addView(CButton(this, "Lepas dari Layar Utama", Ic.TRASH, Color.parseColor("#2A2A2E"), 48f, 14f)
            .apply { setOnClickListener { release() } }, lp(4))
        col.addView(act, lp(8))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F0F12")); isFillViewport = true; addView(col)
        })
        rerender()
    }

    private fun txtL() = etL.text.toString().ifBlank { " " }
    private fun txtR() = etR.text.toString().ifBlank { " " }
    private fun txtB() = etB.text.toString().ifBlank { " " }

    private fun rerender() {
        if (!::etB.isInitialized || !::picker.isInitialized) return
        picker.bitmaps = Templates.list.map { Templates.render(it, txtL(), txtR(), txtB(), cName, cBot, 640) }
    }

    private fun use() {
        prefs.edit().putInt("tplIdx", sel).putString("tplL", etL.text.toString()).putString("tplR", etR.text.toString())
            .putString("tplB", etB.text.toString()).putInt("tplCN", cName).putInt("tplCB", cBot).apply()
        val bmp = Templates.render(Templates.list[sel], txtL(), txtR(), txtB(), cName, cBot, 1200)
        Util.applyTemplate(this, bmp)
        if (OverlayService.running) Util.startOverlay(this)
        Toast.makeText(this, "Template dipakai. Atur posisi di layar utama.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun release() {
        if (!Util.hasTemplate(this)) { Toast.makeText(this, "Belum ada template terpasang", Toast.LENGTH_SHORT).show(); return }
        Util.removeTemplate(this)
        if (OverlayService.running) Util.startOverlay(this)
        Toast.makeText(this, "Template dilepas dari layar utama", Toast.LENGTH_SHORT).show()
    }
}
