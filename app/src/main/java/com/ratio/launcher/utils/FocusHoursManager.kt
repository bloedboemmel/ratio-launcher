package com.ratio.launcher.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime

data class FocusSchedule(
    val id: Long,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val days: Set<DayOfWeek>,
    val enabled: Boolean = true,
    val autoReplyMessage: String = ""
) {
    fun isActiveNow(): Boolean {
        if (!enabled) return false
        val now = LocalTime.now()
        val today = java.time.LocalDate.now().dayOfWeek
        if (!days.contains(today)) return false

        val start = LocalTime.of(startHour, startMinute)
        val end = LocalTime.of(endHour, endMinute)

        return if (end.isAfter(start)) {
            now.isAfter(start) && now.isBefore(end)
        } else {
            now.isAfter(start) || now.isBefore(end)
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("startHour", startHour)
            put("startMinute", startMinute)
            put("endHour", endHour)
            put("endMinute", endMinute)
            put("days", JSONArray(days.map { it.value }))
            put("enabled", enabled)
            put("autoReplyMessage", autoReplyMessage)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): FocusSchedule {
            val daysArray = json.getJSONArray("days")
            val days = (0 until daysArray.length()).map {
                DayOfWeek.of(daysArray.getInt(it))
            }.toSet()

            return FocusSchedule(
                id = json.getLong("id"),
                startHour = json.getInt("startHour"),
                startMinute = json.getInt("startMinute"),
                endHour = json.getInt("endHour"),
                endMinute = json.getInt("endMinute"),
                days = days,
                enabled = json.optBoolean("enabled", true),
                autoReplyMessage = json.optString("autoReplyMessage", "")
            )
        }
    }
}

object FocusHoursManager {

    private const val PREFS = "ratio_focus_hours"
    private const val KEY_SCHEDULES = "schedules"

    fun getSchedules(context: Context): List<FocusSchedule> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SCHEDULES, "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map { FocusSchedule.fromJson(array.getJSONObject(it)) }
    }

    fun saveSchedules(context: Context, schedules: List<FocusSchedule>) {
        val array = JSONArray()
        schedules.forEach { array.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SCHEDULES, array.toString()).apply()
    }

    fun addSchedule(context: Context, schedule: FocusSchedule) {
        val current = getSchedules(context).toMutableList()
        current.add(schedule)
        saveSchedules(context, current)
    }

    fun removeSchedule(context: Context, id: Long) {
        val current = getSchedules(context).toMutableList()
        current.removeAll { it.id == id }
        saveSchedules(context, current)
    }

    fun toggleSchedule(context: Context, id: Long) {
        val current = getSchedules(context).toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(enabled = !current[index].enabled)
            saveSchedules(context, current)
        }
    }

    fun isInFocusMode(context: Context): Boolean {
        return getSchedules(context).any { it.isActiveNow() }
    }

    fun getActiveAutoReply(context: Context): String? {
        return getSchedules(context)
            .firstOrNull { it.isActiveNow() && it.autoReplyMessage.isNotBlank() }
            ?.autoReplyMessage
    }
}
