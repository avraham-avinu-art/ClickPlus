package com.example.clickplus.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class TapLearningOverlay(
    private val context: Context,
    private val onTargetTap: (x: Float, y: Float) -> Unit,
    private val onCancel: () -> Unit,
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: FrameLayout? = null
    private var panel: View? = null
    private var downX = 0f
    private var downY = 0f

    fun show(stage: Int = 1) {
        val existing = root
        if (existing != null) {
            updateStage(stage)
            return
        }

        val density = context.resources.displayMetrics.density
        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val topPanel = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = ViewGroup.LAYOUT_DIRECTION_RTL
            setPadding(
                (14 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt(),
                (8 * density).toInt(),
            )
            background = GradientDrawable().apply {
                setColor(Color.argb(238, 24, 29, 34))
                cornerRadius = 18 * density
            }
        }

        val message = TextView(context).apply {
            tag = "message"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding((6 * density).toInt(), 0, (8 * density).toInt(), 0)
        }
        val cancel = TextView(context).apply {
            text = "ביטול"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(
                (10 * density).toInt(),
                (7 * density).toInt(),
                (10 * density).toInt(),
                (7 * density).toInt(),
            )
            background = GradientDrawable().apply {
                setColor(Color.argb(85, 255, 255, 255))
                cornerRadius = 12 * density
            }
            setOnClickListener { onCancel() }
        }

        topPanel.addView(
            message,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        topPanel.addView(
            cancel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        container.addView(
            topPanel,
            FrameLayout.LayoutParams(
                (minOf(context.resources.displayMetrics.widthPixels - 32, (460 * density).toInt())).coerceAtLeast(260),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply {
                topMargin = (42 * density).toInt()
            },
        )
        panel = topPanel

        container.setOnTouchListener { _, event ->
            val p = panel ?: return@setOnTouchListener false
            val location = IntArray(2)
            p.getLocationOnScreen(location)
            val withinPanel = event.rawX >= location[0] &&
                event.rawX <= location[0] + p.width &&
                event.rawY >= location[1] &&
                event.rawY <= location[1] + p.height
            if (withinPanel) return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - downX) + abs(event.rawY - downY)
                    if (moved < 36f) onTargetTap(event.rawX, event.rawY)
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> true
            }
        }

        val params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        runCatching {
            wm.addView(container, params)
            root = container
            updateStage(stage)
        }
    }

    private fun updateStage(stage: Int) {
        val message = (panel as? LinearLayout)?.findViewWithTag<TextView>("message") ?: return
        message.text = if (stage == 2) {
            "קליק פלוס · לימוד נקודה שנייה — גע ביעד השני"
        } else {
            "קליק פלוס · לימוד מיקום — גע ביעד הרצוי"
        }
    }

    fun hide() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
        panel = null
    }

    fun destroy() = hide()
}
