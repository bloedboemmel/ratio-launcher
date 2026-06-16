package com.ratio.launcher.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.ratio.launcher.R

class UsageTimerService : Service() {

    private var timerView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentApp: String? = null
    private var appStartTime: Long = 0
    private var isOverlayShown = false

    private val DISTRACTING_APPS = setOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.zhiliaoapp.musically", // TikTok
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.google.android.youtube"
    )

    private val checker = object : Runnable {
        override fun run() {
            checkCurrentApp()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(2001, buildNotification())
        handler.post(checker)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checker)
        hideOverlay()
    }

    private fun checkCurrentApp() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 5000, now)
        val event = UsageEvents.Event()

        var latestApp: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestApp = event.packageName
            }
        }

        if (latestApp != null && latestApp != packageName) {
            if (latestApp != currentApp) {
                currentApp = latestApp
                appStartTime = now
                if (isDistractingApp(latestApp)) {
                    showOverlay()
                } else {
                    hideOverlay()
                }
            } else if (isDistractingApp(latestApp) && isOverlayShown) {
                updateOverlay()
            }
        }
    }

    private fun isDistractingApp(packageName: String): Boolean {
        val prefs = getSharedPreferences("ratio_detox", Context.MODE_PRIVATE)
        val custom = prefs.getStringSet("distracting_apps", null)
        return (custom ?: DISTRACTING_APPS).contains(packageName)
    }

    private fun showOverlay() {
        if (isOverlayShown) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !android.provider.Settings.canDrawOverlays(this)) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        timerView = TextView(this).apply {
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 11f
            setBackgroundColor(0x80000000.toInt())
            setPadding(24, 8, 24, 8)
            text = "0:00"
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 120
        }

        wm.addView(timerView, params)
        isOverlayShown = true
    }

    private fun updateOverlay() {
        val elapsed = (System.currentTimeMillis() - appStartTime) / 1000
        val minutes = elapsed / 60
        val seconds = elapsed % 60
        timerView?.text = "$minutes:${String.format("%02d", seconds)}"
    }

    private fun hideOverlay() {
        if (!isOverlayShown) return
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            timerView?.let { wm.removeView(it) }
        } catch (_: Exception) {}
        timerView = null
        isOverlayShown = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "usage_timer", "Usage Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "usage_timer")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Ratio")
            .setContentText("Tracking app usage")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, UsageTimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageTimerService::class.java))
        }
    }
}
