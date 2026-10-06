package com.example.clickplus.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class TapLearningOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: LinearLayout? = null

    fun show() {
        if (root != null) return
        val density = context.resources.displayMetrics.density
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.argb(235, 24, 29, 34))
                cornerRadius = 18 * density
            }
            addView(TextView(context).apply {
                text = "קליק פלוס · לימוד מיקום"
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(TextView(context).apply {
                text = "  לחץ על היעד שברצונך לשמור"
                setTextColor(Color.LTGRAY)
                textSize = 13f
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (48 * density).toInt()
        }
        runCatching { wm.addView(container, p); root = container }
    }

    fun hide() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }

    fun destroy() = hide()
}
