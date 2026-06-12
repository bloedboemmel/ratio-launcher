package com.ratio.launcher.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.util.Calendar

object UsageStatsHelper {

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun getTodayUsageMinutes(context: Context): Long {
        if (!hasPermission(context)) return -1

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        // Use events for accurate calculation
        val events = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var totalTime = 0L
        val lastResumeTime = mutableMapOf<String, Long>()
        val myPackage = context.packageName

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName

            // Skip our own launcher
            if (pkg == myPackage) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastResumeTime[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val resumeTime = lastResumeTime.remove(pkg)
                    if (resumeTime != null) {
                        totalTime += event.timeStamp - resumeTime
                    }
                }
            }
        }

        // Add time for apps still in foreground
        val now = System.currentTimeMillis()
        for ((_, resumeTime) in lastResumeTime) {
            totalTime += now - resumeTime
        }

        return totalTime / 60000
    }
}
