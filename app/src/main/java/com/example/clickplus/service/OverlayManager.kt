package com.example.clickplus.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.clickplus.data.HudStyle

class OverlayManager(private val context: Context) {

    data class SampledNode(
        val text: String,
        val viewId: String
    )

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: TextView? = null
    private var samplerView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { dismissOverlay() }

    var hudStyle = HudStyle.SHORT_TEXT
    private var posX = 0
    private var posY = 100

    @SuppressLint("ClickableViewAccessibility")
    fun showPill(displayText: String, label: String = "") {
        if (hudStyle == HudStyle.SILENT) return

        handler.removeCallbacks(hideRunnable)

        val formatted = when (hudStyle) {
            HudStyle.NUMBER_ONLY -> displayText
            HudStyle.SHORT_TEXT -> label.ifBlank { displayText }
            HudStyle.SILENT -> return
        }

        if (overlayView == null) {
            val view = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(36, 18, 36, 18)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#EE111111"))
                    cornerRadius = 48f
                    setStroke(1, Color.parseColor("#6633DD77"))
                }
                elevation = 12f
                textDirection = View.TEXT_DIRECTION_ANY_RTL
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

            view.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    return when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            posX = params.x
                            posY = params.y
                            runCatching { windowManager.updateViewLayout(view, params) }
                            true
                        }
                        else -> false
                    }
                }
            })

            overlayView = view
            runCatching { windowManager.addView(view, params) }
        }

        overlayView?.text = formatted
        handler.postDelayed(hideRunnable, 1500L)
    }

    fun startNodeSampler(onSampled: (SampledNode?) -> Unit) {
        if (samplerView != null) return

        val sampler = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            text = "גע ברכיב שברצונך לדגום\nלחיצה מחזירה אותך אוטומטית"
            setPadding(32, 24, 32, 24)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC101010"))
                cornerRadius = 40f
                setStroke(2, Color.parseColor("#FF00E676"))
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        sampler.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val service = KeyInterceptorAccessibilityService.instance
                val node = service?.rootInActiveWindow?.let {
                    findNodeAt(it, event.rawX.toInt(), event.rawY.toInt())
                }

                val result = node?.let {
                    SampledNode(
                        text = it.text?.toString().orEmpty(),
                        viewId = it.viewIdResourceName.orEmpty()
                    )
                }

                stopNodeSampler()
                handler.post { onSampled(result) }
                true
            } else {
                true
            }
        }

        samplerView = sampler
        runCatching { windowManager.addView(sampler, params) }
    }

    fun stopNodeSampler() {
        samplerView?.let { runCatching { windowManager.removeView(it) } }
        samplerView = null
    }

    private fun findNodeAt(
        node: android.view.accessibility.AccessibilityNodeInfo,
        x: Int,
        y: Int
    ): android.view.accessibility.AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        for (i in node.childCount - 1 downTo 0) {
            val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
            if (child.getBoundsInScreenSafe(x, y)) {
                val deep = findNodeAt(child, x, y)
                if (deep != null) return deep
            }
        }

        return if (
            bounds.contains(x, y) &&
            (
                !node.text.isNullOrBlank() ||
                    !node.contentDescription.isNullOrBlank() ||
                    !node.viewIdResourceName.isNullOrBlank()
            )
        ) node else null
    }

    private fun android.view.accessibility.AccessibilityNodeInfo.getBoundsInScreenSafe(
        x: Int,
        y: Int
    ): Boolean {
        val bounds = Rect()
        getBoundsInScreen(bounds)
        return bounds.contains(x, y)
    }

    private fun dismissOverlay() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
    }

    fun dismiss() {
        handler.removeCallbacks(hideRunnable)
        stopNodeSampler()
        dismissOverlay()
    }
}