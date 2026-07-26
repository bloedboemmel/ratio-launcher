package com.ratio.launcher.utils

import android.content.Context
import java.time.LocalTime

object BedtimeMode {

    private const val PREFS = "ratio_bedtime"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_START_HOUR = "start_hour"
    private const val KEY_START_MINUTE = "start_minute"
    private const val KEY_END_HOUR = "end_hour"
    private const val KEY_END_MINUTE = "end_minute"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getStartTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Pair(prefs.getInt(KEY_START_HOUR, 22), prefs.getInt(KEY_START_MINUTE, 0))
    }

    fun getEndTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Pair(prefs.getInt(KEY_END_HOUR, 7), prefs.getInt(KEY_END_MINUTE, 0))
    }

    fun setSchedule(context: Context, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_START_HOUR, startHour)
            .putInt(KEY_START_MINUTE, startMinute)
            .putInt(KEY_END_HOUR, endHour)
            .putInt(KEY_END_MINUTE, endMinute)
            .apply()
    }

    fun isActiveNow(context: Context): Boolean {
        if (!isEnabled(context)) return false
        val (startH, startM) = getStartTime(context)
        val (endH, endM) = getEndTime(context)

        val now = LocalTime.now()
        val start = LocalTime.of(startH, startM)
        val end = LocalTime.of(endH, endM)

        return if (end.isAfter(start)) {
            now.isAfter(start) && now.isBefore(end)
        } else {
            now.isAfter(start) || now.isBefore(end)
        }
    }
}
