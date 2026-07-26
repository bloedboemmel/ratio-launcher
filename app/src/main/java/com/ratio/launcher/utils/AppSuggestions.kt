package com.ratio.launcher.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

object AppSuggestions {

    fun getSuggestions(context: Context, count: Int = 4): List<String> {
        if (!UsageStatsHelper.hasPermission(context)) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Look at usage in the same time window over the past 7 days
        val endTime = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val startTime = calendar.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            ?: return emptyList()

        // Weight apps used at similar times of day higher
        val scored = mutableMapOf<String, Float>()
        val launcherPackages = getLauncherPackages(context)

        for (stat in stats) {
            if (stat.totalTimeInForeground < 60000) continue
            if (stat.packageName == context.packageName) continue
            if (!launcherPackages.contains(stat.packageName)) continue

            val score = stat.totalTimeInForeground.toFloat() / 60000f
            scored[stat.packageName] = (scored[stat.packageName] ?: 0f) + score
        }

        return scored.entries
            .sortedByDescending { it.value }
            .take(count)
            .map { it.key }
    }

    private fun getLauncherPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }
}
