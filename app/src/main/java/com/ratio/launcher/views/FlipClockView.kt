package com.ratio.launcher.views

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ratio.launcher.R
import java.util.Calendar

class FlipClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val digitViews = mutableListOf<TextView>()
    private val previousDigits = CharArray(6) { ' ' }
    private var showSeconds = false
    private var use24h = true

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val updater = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        buildDigits()
    }

    private fun buildDigits() {
        removeAllViews()
        digitViews.clear()

        // HH : MM (: SS)
        val count = if (showSeconds) 8 else 5 // h h : m m or h h : m m : s s

        for (i in 0 until count) {
            if (i == 2 || (showSeconds && i == 5)) {
                // Separator
                val sep = TextView(context).apply {
                    text = ":"
                    setTextColor(0x80FFFFFF.toInt())
                    textSize = 48f
                    typeface = resources.getFont(R.font.inter_light)
                    gravity = Gravity.CENTER
                    setPadding(8, 0, 8, 0)
                }
                addView(sep)
            } else {
                val digit = TextView(context).apply {
                    text = "0"
                    setTextColor(0xFFF2F2F2.toInt())
                    textSize = 72f
                    typeface = resources.getFont(R.font.inter_semibold)
                    gravity = Gravity.CENTER
                    setPadding(4, 0, 4, 0)
                    pivotX = 0.5f
                    pivotY = 0.5f
                    // Fixed width so digits don't shift when changing
                    minWidth = (52 * resources.displayMetrics.density).toInt()
                    maxWidth = (52 * resources.displayMetrics.density).toInt()
                }
                digitViews.add(digit)
                addView(digit)
            }
        }
    }

    fun setShowSeconds(show: Boolean) {
        if (show != showSeconds) {
            showSeconds = show
            previousDigits.fill(' ')
            buildDigits()
            updateTime()
        }
    }

    fun setUse24h(h24: Boolean) {
        use24h = h24
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateTime()
        handler.post(updater)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(updater)
    }

    private fun updateTime() {
        val cal = Calendar.getInstance()
        var hour = cal.get(Calendar.HOUR_OF_DAY)
        if (!use24h) {
            hour = if (hour % 12 == 0) 12 else hour % 12
        }
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        val timeStr = if (showSeconds) {
            String.format("%02d%02d%02d", hour, minute, second)
        } else {
            String.format("%02d%02d", hour, minute)
        }

        for (i in timeStr.indices) {
            if (i < digitViews.size) {
                val newChar = timeStr[i]
                if (newChar != previousDigits[i]) {
                    flipDigit(digitViews[i], newChar.toString())
                    previousDigits[i] = newChar
                }
            }
        }
    }

    private fun flipDigit(view: TextView, newValue: String) {
        val flipOut = ObjectAnimator.ofFloat(view, View.ROTATION_X, 0f, -90f).apply {
            duration = 150
        }
        val flipIn = ObjectAnimator.ofFloat(view, View.ROTATION_X, 90f, 0f).apply {
            duration = 150
        }

        flipOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.text = newValue
            }
        })

        val set = AnimatorSet()
        set.playSequentially(flipOut, flipIn)
        set.start()
    }
}
