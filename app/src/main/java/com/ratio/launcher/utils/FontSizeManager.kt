package com.ratio.launcher.utils

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit

object FontSizeManager {
    private const val PREFS = "ratio_prefs"
    private const val KEY = "font_scale"

    const val SCALE_SMALL = 0.9f
    const val SCALE_MEDIUM = 1.0f
    const val SCALE_LARGE = 1.15f
    const val SCALE_EXTRA_LARGE = 1.3f
    const val SCALE_HUGE = 1.45f

    val presets = listOf(
        "Small" to SCALE_SMALL,
        "Medium" to SCALE_MEDIUM,
        "Large" to SCALE_LARGE,
        "Extra large" to SCALE_EXTRA_LARGE,
        "Huge" to SCALE_HUGE,
    )

    fun getScale(context: Context): Float {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY, SCALE_MEDIUM)
    }

    fun setScale(context: Context, scale: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putFloat(KEY, scale) }
    }

    fun labelFor(scale: Float): String {
        return presets.minByOrNull { kotlin.math.abs(it.second - scale) }?.first ?: "Medium"
    }

    /** Wraps [context] with a Configuration whose fontScale reflects the user's saved preset. */
    fun wrap(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.fontScale = getScale(context)
        return context.createConfigurationContext(config)
    }
}
