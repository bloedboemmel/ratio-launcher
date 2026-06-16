package com.ratio.launcher.utils

import android.content.Context

object QuickLaunchGestures {

    private const val PREFS = "ratio_gestures"
    private const val KEY_SWIPE_UP = "swipe_up_app"
    private const val KEY_SWIPE_DOWN = "swipe_down_app"
    private const val KEY_SWIPE_LEFT = "swipe_left_app"
    private const val KEY_SWIPE_RIGHT = "swipe_right_app"

    enum class Direction { UP, DOWN, LEFT, RIGHT }

    fun getApp(context: Context, direction: Direction): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(directionKey(direction), null)
    }

    fun setApp(context: Context, direction: Direction, packageName: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(directionKey(direction), packageName).apply()
    }

    fun launch(context: Context, direction: Direction): Boolean {
        val pkg = getApp(context, direction) ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        context.startActivity(intent)
        return true
    }

    private fun directionKey(direction: Direction): String {
        return when (direction) {
            Direction.UP -> KEY_SWIPE_UP
            Direction.DOWN -> KEY_SWIPE_DOWN
            Direction.LEFT -> KEY_SWIPE_LEFT
            Direction.RIGHT -> KEY_SWIPE_RIGHT
        }
    }

    fun getDirectionLabel(direction: Direction): String {
        return when (direction) {
            Direction.UP -> "Swipe up"
            Direction.DOWN -> "Swipe down"
            Direction.LEFT -> "Swipe left"
            Direction.RIGHT -> "Swipe right"
        }
    }
}
