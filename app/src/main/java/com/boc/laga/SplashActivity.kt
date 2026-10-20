package com.boc.laga

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SplashActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
        }
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_logo)
            layoutParams = LinearLayout.LayoutParams((120 * d).toInt(), (120 * d).toInt())
        })
        root.addView(TextView(this).apply {
            text = "BOC"; textSize = 34f; setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, (16 * d).toInt(), 0, 0)
        })
        root.addView(TextView(this).apply {
            text = "Owner : Gunawan"; textSize = 15f; setTextColor(Color.parseColor("#B0B0B8"))
            gravity = Gravity.CENTER; setPadding(0, (6 * d).toInt(), 0, 0)
        })
        setContentView(root)
        Handler(Looper.getMainLooper()).postDelayed({ next() }, 1500)
    }

    private fun next() {
        val p = Util.prefs(this)
        val auto = p.getBoolean("auto", false) && p.getString("pkg", null) != null &&
            Settings.canDrawOverlays(this) && Util.layers(this).isNotEmpty()
        if (auto) {
            Util.startOverlay(this)
            if (Util.launchGame(this)) { finish(); return }
            Toast.makeText(this, "Game tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
