package com.ratio.launcher.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ClockStyle(val displayName: String) {
    MINIMAL("Minimal"),          // 12:00 thin
    BOLD("Bold"),                // 12:00 heavy
    FLIP("Flip Clock"),          // 10  38 with PM — like the screenshot
    WORD("Word Clock"),          // twelve thirty
    BINARY("Binary"),            // 01100 011110
    BAR("Day Progress"),         // ▓▓▓░░░░ 35%
    ANALOG("Analog");            // Drawn analog clock face

    companion object {
        private const val PREFS = "ratio_prefs"
        private const val KEY = "clock_style"

        fun getCurrent(context: Context): ClockStyle {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val name = prefs.getString(KEY, "MINIMAL") ?: "MINIMAL"
            return try { valueOf(name) } catch (_: Exception) { MINIMAL }
        }

        fun setCurrent(context: Context, style: ClockStyle) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, style.name).apply()
        }
    }

    fun formatTime(use24h: Boolean, showSeconds: Boolean = false): String {
        val now = Date()
        val hour24 = SimpleDateFormat("HH", Locale.getDefault()).format(now).toInt()
        val minute = SimpleDateFormat("mm", Locale.getDefault()).format(now).toInt()
        val second = SimpleDateFormat("ss", Locale.getDefault()).format(now).toInt()
        val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val displayHour = if (use24h) hour24 else hour12
        val sec = String.format("%02d", second)

        return when (this) {
            MINIMAL -> {
                val pattern = if (use24h) "HH:mm" else "h:mm"
                val base = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                if (showSeconds) "$base:$sec" else base
            }
            BOLD -> {
                val pattern = if (use24h) "HH:mm" else "h:mm"
                val base = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
                if (showSeconds) "$base:$sec" else base
            }
            FLIP -> {
                val h = String.format("%02d", displayHour)
                val m = String.format("%02d", minute)
                val ampm = if (!use24h) {
                    if (hour24 < 12) "AM" else "PM"
                } else ""
                if (showSeconds) "$h  $m  $sec\n$ampm"
                else "$h  $m\n$ampm"
            }
            WORD -> {
                val base = if (minute == 0) {
                    "${numberToWord(displayHour)}\no'clock"
                } else if (minute == 30) {
                    "half past\n${numberToWord(displayHour)}"
                } else if (minute == 15) {
                    "quarter past\n${numberToWord(displayHour)}"
                } else if (minute == 45) {
                    "quarter to\n${numberToWord(if (displayHour == 12) 1 else displayHour + 1)}"
                } else {
                    "${numberToWord(displayHour)}\n${numberToWord(minute)}"
                }
                if (showSeconds) "$base\n${numberToWord(second)}" else base
            }
            BINARY -> {
                val hBin = Integer.toBinaryString(displayHour).padStart(5, '0')
                val mBin = Integer.toBinaryString(minute).padStart(6, '0')
                if (showSeconds) {
                    val sBin = Integer.toBinaryString(second).padStart(6, '0')
                    "$hBin\n$mBin\n$sBin"
                } else "$hBin\n$mBin"
            }
            BAR -> {
                val totalMinutes = hour24 * 60 + minute
                val progress = (totalMinutes * 100) / 1440
                val filled = progress / 5
                val empty = 20 - filled
                "${"▓".repeat(filled)}${"░".repeat(empty)}\n${progress}% of day"
            }
            ANALOG -> {
                val h = String.format("%02d", displayHour)
                val m = String.format("%02d", minute)
                if (showSeconds) "$h:$m:$sec" else "$h:$m"
            }
        }
    }

    private fun numberToWord(n: Int): String {
        val ones = arrayOf("zero", "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
            "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
            "eighteen", "nineteen")
        val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty")

        return when {
            n < 20 -> ones[n]
            n % 10 == 0 -> tens[n / 10]
            else -> "${tens[n / 10]}\n${ones[n % 10]}"
        }
    }
}
