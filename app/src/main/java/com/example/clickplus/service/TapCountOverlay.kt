package com.example.clickplus.service

import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.content.Context

class TapCountOverlay(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val textView = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 20f
        gravity = Gravity.CENTER
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        background = GradientDrawable().apply {
            setColor(Color.argb(225, 35, 35, 35))
            cornerRadius = 999f
        }
        elevation = 8f
        visibility = View.GONE
    }

    private var attached = false
    private var hideRunnable: Runnable? = null

    fun setSizeSp(sizeSp: Float) {
        val safe = sizeSp.coerceIn(14f, 32f)
        textView.textSize = safe
        if (attached) {
            val density = context.resources.displayMetrics.density
            val sidePadding = (safe * 1.8f * density).toInt().coerceAtLeast((22 * density).toInt())
            val height = (safe * 2.2f * density).toInt().coerceAtLeast((40 * density).toInt())
            val params = textView.layoutParams as? WindowManager.LayoutParams
            if (params != null) {
                params.width = sidePadding
                params.height = height
                runCatching { windowManager.updateViewLayout(textView, params) }
            }
        }
    }

    fun show(count: Int, durationMs: Long = 900L) {
        textView.text = count.toString()

        if (!attached) {
            val density = context.resources.displayMetrics.density
            val height = context.resources.displayMetrics.heightPixels

            val params = WindowManager.LayoutParams(
                (64 * density).toInt(),
                (48 * density).toInt(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = (height * 0.35f).toInt()
            }

            runCatching {
                windowManager.addView(textView, params)
                attached = true
            }
        }

        if (!attached) return

        textView.visibility = View.VISIBLE
        hideRunnable?.let(textView::removeCallbacks)
        val hide = Runnable { textView.visibility = View.GONE }
        hideRunnable = hide
        textView.postDelayed(hide, durationMs.coerceIn(300L, 1500L))
    }

    fun hide() {
        hideRunnable?.let(textView::removeCallbacks)
        hideRunnable = null
        textView.visibility = View.GONE
    }

    fun destroy() {
        hide()
        if (attached) {
            runCatching { windowManager.removeView(textView) }
            attached = false
        }
    }
}
