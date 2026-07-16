package com.ratio.launcher.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.CountDownTimer
import androidx.core.app.NotificationCompat
import com.ratio.launcher.R

object PomodoroTimer {

    private const val CHANNEL_ID = "pomodoro"
    private const val WORK_MINUTES = 25L
    private const val BREAK_MINUTES = 5L

    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var isWorkPhase = true
    private var remainingMs = WORK_MINUTES * 60 * 1000L

    var listener: PomodoroListener? = null

    fun isActive(): Boolean = isRunning

    fun getCurrentPhase(): String = if (isWorkPhase) "Focus" else "Break"

    fun getRemainingFormatted(): String {
        val minutes = remainingMs / 60000
        val seconds = (remainingMs % 60000) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun start(context: Context) {
        if (isRunning) return
        createChannel(context)

        timer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                listener?.onTick(getRemainingFormatted(), getCurrentPhase())
            }

            override fun onFinish() {
                isRunning = false
                showNotification(context)
                // Switch phase
                isWorkPhase = !isWorkPhase
                remainingMs = if (isWorkPhase) WORK_MINUTES * 60 * 1000L else BREAK_MINUTES * 60 * 1000L
                listener?.onPhaseComplete(getCurrentPhase())
            }
        }.start()
        isRunning = true
        listener?.onTick(getRemainingFormatted(), getCurrentPhase())
    }

    fun pause() {
        timer?.cancel()
        isRunning = false
    }

    fun reset() {
        timer?.cancel()
        isRunning = false
        isWorkPhase = true
        remainingMs = WORK_MINUTES * 60 * 1000L
        listener?.onTick(getRemainingFormatted(), getCurrentPhase())
    }

    private fun showNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = if (isWorkPhase) "Break over!" else "Focus session complete!"
        val text = if (isWorkPhase) "Time to focus again" else "Take a 5 minute break"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(3001, notification)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Pomodoro Timer", NotificationManager.IMPORTANCE_HIGH)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    interface PomodoroListener {
        fun onTick(time: String, phase: String)
        fun onPhaseComplete(nextPhase: String)
    }
}
