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
    private var xPercentFromLeft = 50
    private var yPercentFromTop = 65
    private var sizeDp = 48

    fun show(count: Int, durationMs: Long = 900L) {
        textView.text = count.toString()

        if (!attached) {
            val density = context.resources.displayMetrics.density
            val height = context.resources.displayMetrics.heightPixels

            val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = ((context.resources.displayMetrics.widthPixels - sizePx)
                    .coerceAtLeast(0) * xPercentFromLeft.coerceIn(0, 100) / 100f).toInt()
                y = ((height - sizePx).coerceAtLeast(0) * yPercentFromTop.coerceIn(0, 100) / 100f).toInt()
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

    fun setPosition(percentFromLeft: Int, percentFromTop: Int) {
        xPercentFromLeft = percentFromLeft.coerceIn(0, 100)
        yPercentFromTop = percentFromTop.coerceIn(0, 100)
        if (attached) {
            val params = textView.layoutParams as? WindowManager.LayoutParams ?: return
            val density = context.resources.displayMetrics.density
            params.gravity = Gravity.TOP or Gravity.START
            params.x = ((context.resources.displayMetrics.widthPixels - (sizeDp * density).toInt())
                .coerceAtLeast(0) * xPercentFromLeft / 100f).toInt()
            params.y = ((context.resources.displayMetrics.heightPixels - (sizeDp * density).toInt())
                .coerceAtLeast(0) * yPercentFromTop / 100f).toInt()
            runCatching { windowManager.updateViewLayout(textView, params) }
        }
    }

    fun setSize(newSizeDp: Int) {
        val safeSizeDp = newSizeDp.coerceIn(32, 96)
        val oldVisible = textView.visibility == View.VISIBLE
        if (attached) {
            runCatching { windowManager.removeView(textView) }
            attached = false
        }
        this.sizeDp = safeSizeDp
        if (oldVisible) show(textView.text?.toString()?.toIntOrNull() ?: 1)
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
