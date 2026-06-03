package com.ratio.launcher.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ratio.launcher.R

object UsageGoalsManager {

    private const val PREFS = "ratio_usage_goals"
    private const val KEY_DAILY_GOAL = "daily_goal_minutes"
    private const val KEY_LAST_WARNING = "last_warning_date"
    private const val CHANNEL_ID = "usage_goals"

    fun getDailyGoal(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_DAILY_GOAL, 240)
    }

    fun setDailyGoal(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DAILY_GOAL, minutes).apply()
    }

    fun checkAndNotify(context: Context) {
        val goal = getDailyGoal(context)
        if (goal <= 0) return

        val used = UsageStatsHelper.getTodayUsageMinutes(context)
        if (used < 0) return

        val remaining = (goal - used).toInt()
        if (remaining in 1..5) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val today = java.time.LocalDate.now().toString()
            val lastWarning = prefs.getString(KEY_LAST_WARNING, "")

            if (lastWarning != today) {
                prefs.edit().putString(KEY_LAST_WARNING, today).apply()
                showWarningNotification(context, remaining)
            }
        }
    }

    private fun showWarningNotification(context: Context, remainingMinutes: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Usage Goals",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Screen time goal")
            .setContentText("$remainingMinutes minutes remaining of your daily goal")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(1001, notification)
    }

    fun getProgress(context: Context): Float {
        val goal = getDailyGoal(context)
        if (goal <= 0) return 0f
        val used = UsageStatsHelper.getTodayUsageMinutes(context)
        if (used < 0) return 0f
        return (used.toFloat() / goal).coerceIn(0f, 1f)
    }
}
