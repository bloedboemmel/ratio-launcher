package com.ratio.launcher.utils

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.Calendar

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val usageMinutes: Long
)

object AppUsageHelper {

    fun getTodayPerAppUsage(context: Context): List<AppUsageInfo> {
        if (!UsageStatsHelper.hasPermission(context)) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            ?: return emptyList()

        val launcherApps = getLauncherPackages(context)

        return stats
            .filter { it.totalTimeInForeground > 60000 } // at least 1 minute
            .filter { launcherApps.contains(it.packageName) }
            .filter { it.packageName != context.packageName }
            .sortedByDescending { it.totalTimeInForeground }
            .take(15)
            .map { stat ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(stat.packageName, 0)).toString()
                } catch (_: Exception) { stat.packageName }

                val icon = try {
                    pm.getApplicationIcon(stat.packageName)
                } catch (_: Exception) { null }

                AppUsageInfo(
                    packageName = stat.packageName,
                    appName = appName,
                    icon = icon,
                    usageMinutes = stat.totalTimeInForeground / 60000
                )
            }
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

    fun formatUsageTime(minutes: Long): String {
        return if (minutes >= 60) {
            "${minutes / 60}h ${minutes % 60}m"
        } else {
            "${minutes}m"
        }
    }
}
