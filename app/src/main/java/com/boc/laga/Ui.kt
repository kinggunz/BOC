package com.boc.laga

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object Ui {
    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun rounded(c: Context, color: String, r: Int = 12): GradientDrawable = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = r * c.resources.displayMetrics.density
    }

    fun btn(c: Context, t: String, color: String = "#2A2A2A", onClick: () -> Unit): Button = Button(c).apply {
        text = t; isAllCaps = false; setTextColor(Color.WHITE); background = rounded(c, color)
        setOnClickListener { onClick() }
    }

    fun txt(c: Context, t: String, size: Float = 14f, color: String = "#DDDDDD"): TextView = TextView(c).apply {
        text = t; textSize = size; setTextColor(Color.parseColor(color))
    }

    fun lp(c: Context, m: Int = 4): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(c, m), dp(c, m), dp(c, m), dp(c, m)) }

    fun card(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(c, "#1E1E1E", 16)
        setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 14))
    }

    fun edit(c: Context, hintText: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText = EditText(c).apply {
        hint = hintText; inputType = type
        setTextColor(Color.WHITE); setHintTextColor(Color.parseColor("#888888"))
        background = rounded(c, "#2A2A2A", 10)
        setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10))
    }
}
