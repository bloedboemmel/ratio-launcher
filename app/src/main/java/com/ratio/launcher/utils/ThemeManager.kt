package com.ratio.launcher.utils

import android.content.Context
import android.graphics.Color

enum class RatioTheme(
    val key: String,
    val backgroundColor: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textTertiary: Int,
    val cardBackground: Int,
    val dividerColor: Int,
    val inputBarColor: Int,
    val accentColor: Int
) {
    DARK(
        key = "dark",
        backgroundColor = Color.parseColor("#0B0B0B"),
        textPrimary = Color.parseColor("#F2F2F2"),
        textSecondary = Color.parseColor("#B3B3B3"),
        textTertiary = Color.parseColor("#636363"),
        cardBackground = Color.parseColor("#181818"),
        dividerColor = Color.parseColor("#323232"),
        inputBarColor = Color.parseColor("#40797979"),
        accentColor = Color.parseColor("#FFFC33")
    ),
    FOCUS(
        key = "focus",
        backgroundColor = Color.parseColor("#242424"),
        textPrimary = Color.parseColor("#F2F2F2"),
        textSecondary = Color.parseColor("#B3B3B3"),
        textTertiary = Color.parseColor("#636363"),
        cardBackground = Color.parseColor("#323232"),
        dividerColor = Color.parseColor("#404040"),
        inputBarColor = Color.parseColor("#FF323232"),
        accentColor = Color.parseColor("#FFFC33")
    ),
    LIGHT(
        key = "light",
        backgroundColor = Color.parseColor("#D9D9D9"),
        textPrimary = Color.parseColor("#0B0B0B"),
        textSecondary = Color.parseColor("#4D4D4D"),
        textTertiary = Color.parseColor("#797979"),
        cardBackground = Color.parseColor("#C7C7C7"),
        dividerColor = Color.parseColor("#B3B3B3"),
        inputBarColor = Color.parseColor("#40B3B3B3"),
        accentColor = Color.parseColor("#FFFC33")
    ),
    SUN(
        key = "sun",
        backgroundColor = Color.parseColor("#FFFFFF"),
        textPrimary = Color.parseColor("#0B0B0B"),
        textSecondary = Color.parseColor("#585858"),
        textTertiary = Color.parseColor("#797979"),
        cardBackground = Color.parseColor("#F2F2F2"),
        dividerColor = Color.parseColor("#D9D9D9"),
        inputBarColor = Color.parseColor("#40B3B3B3"),
        accentColor = Color.parseColor("#FFFC33")
    );

    companion object {
        private const val PREFS = "ratio_prefs"
        private const val KEY_THEME = "theme"

        fun getCurrent(context: Context): RatioTheme {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = prefs.getString(KEY_THEME, "dark") ?: "dark"
            return entries.find { it.key == key } ?: DARK
        }

        fun setCurrent(context: Context, theme: RatioTheme) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME, theme.key).apply()
        }

        fun cycle(context: Context): RatioTheme {
            val current = getCurrent(context)
            val next = entries[(current.ordinal + 1) % entries.size]
            setCurrent(context, next)
            return next
        }
    }
}
