package com.ratio.launcher.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#323232")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val paintHourMarker = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2F2F2")
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintMinuteMarker = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#636363")
        strokeWidth = 1.5f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintHourHand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2F2F2")
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintMinuteHand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2F2F2")
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    var showSeconds = false

    private val paintSecondHand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFC33")
        strokeWidth = 1.5f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFC33")
        style = Paint.Style.FILL
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updater = object : Runnable {
        override fun run() {
            invalidate()
            val delay = if (showSeconds) 1000L else 30000L
            handler.postDelayed(this, delay)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Read setting on attach
        showSeconds = context.getSharedPreferences("ratio_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("show_seconds", false)
        handler.post(updater)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(updater)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = 500
        val width = resolveSize(size, widthMeasureSpec)
        val height = resolveSize(size, heightMeasureSpec)
        val s = min(width, height)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) * 0.85f

        // Draw circle
        canvas.drawCircle(cx, cy, radius, paintCircle)

        // Draw hour markers
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val innerRadius = radius * 0.85f
            val outerRadius = radius * 0.95f
            canvas.drawLine(
                cx + (innerRadius * cos(angle)).toFloat(),
                cy + (innerRadius * sin(angle)).toFloat(),
                cx + (outerRadius * cos(angle)).toFloat(),
                cy + (outerRadius * sin(angle)).toFloat(),
                paintHourMarker
            )
        }

        // Draw minute markers
        for (i in 0 until 60) {
            if (i % 5 == 0) continue
            val angle = Math.toRadians((i * 6 - 90).toDouble())
            val innerRadius = radius * 0.9f
            val outerRadius = radius * 0.95f
            canvas.drawLine(
                cx + (innerRadius * cos(angle)).toFloat(),
                cy + (innerRadius * sin(angle)).toFloat(),
                cx + (outerRadius * cos(angle)).toFloat(),
                cy + (outerRadius * sin(angle)).toFloat(),
                paintMinuteMarker
            )
        }

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        // Hour hand
        val hourAngle = Math.toRadians(((hour + minute / 60f) * 30 - 90).toDouble())
        val hourLength = radius * 0.5f
        canvas.drawLine(
            cx, cy,
            cx + (hourLength * cos(hourAngle)).toFloat(),
            cy + (hourLength * sin(hourAngle)).toFloat(),
            paintHourHand
        )

        // Minute hand
        val minuteAngle = Math.toRadians(((minute + second / 60f) * 6 - 90).toDouble())
        val minuteLength = radius * 0.72f
        canvas.drawLine(
            cx, cy,
            cx + (minuteLength * cos(minuteAngle)).toFloat(),
            cy + (minuteLength * sin(minuteAngle)).toFloat(),
            paintMinuteHand
        )

        // Second hand (only if enabled)
        if (showSeconds) {
            val secondAngle = Math.toRadians((second * 6 - 90).toDouble())
            val secondLength = radius * 0.78f
            canvas.drawLine(
                cx, cy,
                cx + (secondLength * cos(secondAngle)).toFloat(),
                cy + (secondLength * sin(secondAngle)).toFloat(),
                paintSecondHand
            )
        }

        // Center dot
        canvas.drawCircle(cx, cy, 5f, paintCenter)
    }
}
