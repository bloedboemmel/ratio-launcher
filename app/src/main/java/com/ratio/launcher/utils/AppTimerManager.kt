package com.ratio.launcher.utils

import android.content.Context
import org.json.JSONObject

object AppTimerManager {

    private const val PREFS = "ratio_app_timers"
    private const val KEY_TIMERS = "timers"
    private const val KEY_USAGE_TODAY = "usage_today"
    private const val KEY_USAGE_DATE = "usage_date"

    fun setLimit(context: Context, packageName: String, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val timers = getTimers(context).toMutableMap()
        timers[packageName] = minutes
        val json = JSONObject(timers.mapValues { it.value }).toString()
        prefs.edit().putString(KEY_TIMERS, json).apply()
    }

    fun removeLimit(context: Context, packageName: String) {
        val timers = getTimers(context).toMutableMap()
        timers.remove(packageName)
        val json = JSONObject(timers.mapValues { it.value }).toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TIMERS, json).apply()
    }

    fun getLimit(context: Context, packageName: String): Int? {
        return getTimers(context)[packageName]
    }

    fun getTimers(context: Context): Map<String, Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TIMERS, "{}") ?: "{}"
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Int>()
        obj.keys().forEach { key -> map[key] = obj.getInt(key) }
        return map
    }

    fun isLimitReached(context: Context, packageName: String): Boolean {
        val limit = getLimit(context, packageName) ?: return false
        val usedMinutes = getUsageToday(context, packageName)
        return usedMinutes >= limit
    }

    private fun getUsageToday(context: Context, packageName: String): Long {
        if (!UsageStatsHelper.hasPermission(context)) return 0

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val stats = usm.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis, System.currentTimeMillis()
        ) ?: return 0

        return stats.filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground } / 60000
    }
}
