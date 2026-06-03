package com.ratio.launcher.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean,
    val location: String
) {
    fun formatTime(): String {
        if (allDay) return "All day"
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return "${format.format(Date(startTime))} - ${format.format(Date(endTime))}"
    }
}

object CalendarHelper {

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun getTodayEvents(context: Context): List<CalendarEvent> {
        if (!hasPermission(context)) return emptyList()

        val events = mutableListOf<CalendarEvent>()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val dayEnd = calendar.timeInMillis

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val selection = "${CalendarContract.Instances.BEGIN} >= ? AND ${CalendarContract.Instances.BEGIN} < ?"
        val selectionArgs = arrayOf(dayStart.toString(), dayEnd.toString())

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(dayStart.toString())
            .appendPath(dayEnd.toString())
            .build()

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, projection, null, null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.let {
                while (it.moveToNext()) {
                    val title = it.getString(0) ?: "No title"
                    val begin = it.getLong(1)
                    val end = it.getLong(2)
                    val allDay = it.getInt(3) == 1
                    val location = it.getString(4) ?: ""

                    events.add(CalendarEvent(
                        title = title,
                        startTime = begin,
                        endTime = end,
                        allDay = allDay,
                        location = location
                    ))
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }

        return events
    }

    fun getUpcomingEvent(context: Context): CalendarEvent? {
        val events = getTodayEvents(context)
        val now = System.currentTimeMillis()
        return events.firstOrNull { it.endTime > now }
    }
}
