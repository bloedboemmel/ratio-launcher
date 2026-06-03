package com.ratio.launcher.utils

import android.content.Context

object AnimationHelper {

    private const val PREFS = "ratio_prefs"
    private const val KEY_FASTER = "faster_animations"

    fun isFasterAnimations(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FASTER, false)
    }

    fun getDuration(context: Context, normalMs: Long): Long {
        return if (isFasterAnimations(context)) normalMs / 2 else normalMs
    }

    fun getPageTransformDuration(context: Context): Long {
        return getDuration(context, 300)
    }

    fun getTileSizeColumns(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.getBoolean("large_tiles", false)) 2 else 4
    }

    fun setLargeTiles(context: Context, large: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("large_tiles", large).apply()
    }
}
