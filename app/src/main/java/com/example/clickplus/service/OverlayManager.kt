package com.example.clickplus.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import com.example.clickplus.data.HudStyle

class OverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { dismissOverlay() }
    var hudStyle = HudStyle.SHORT_TEXT
    private var posX = 0
    private var posY = 100

    @SuppressLint("ClickableViewAccessibility")
    fun showPill(displayText: String, label: String = "") {
        handler.removeCallbacks(hideRunnable)
        val formatted = when (hudStyle) {
            HudStyle.NUMBER_ONLY -> displayText
            HudStyle.SHORT_TEXT -> label.ifEmpty { displayText }
            HudStyle.ICON_WITH_TEXT -> "▶ $displayText${if (label.isNotEmpty()) " • $label" else ""}"
        }
        if (overlayView == null) {
            val view = TextView(context).apply {
                setBackgroundColor(Color.parseColor("#EE111111"))
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(36, 20, 36, 20)
                elevation = 12f
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = posX
                y = posY
            }
            view.setOnTouchListener(object : android.view.View.OnTouchListener {
                var initialX = 0
                var initialY = 0
                var initialTouchX = 0f
                var initialTouchY = 0f
                override fun onTouch(v: android.view.View?, event: MotionEvent): Boolean = when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        posX = params.x; posY = params.y
                        windowManager.updateViewLayout(view, params)
                        true
                    }
                    else -> false
                }
            })
            overlayView = view
            runCatching { windowManager.addView(view, params) }
        }
        overlayView?.text = formatted
        handler.postDelayed(hideRunnable, 1500L)
    }
    private fun dismissOverlay() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
    }
    fun dismiss() {
        handler.removeCallbacks(hideRunnable)
        dismissOverlay()
    }
}
