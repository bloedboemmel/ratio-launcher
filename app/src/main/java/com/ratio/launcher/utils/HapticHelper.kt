package com.ratio.launcher.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object HapticHelper {

    private const val PREFS = "ratio_prefs"
    private const val KEY_HAPTIC = "haptic_feedback"

    enum class HapticMode { DISABLED, APP_DRAWER_ONLY, ENABLED, FOLLOW_SYSTEM }

    fun getMode(context: Context): HapticMode {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HAPTIC, "ENABLED") ?: "ENABLED"
        return try { HapticMode.valueOf(value) } catch (_: Exception) { HapticMode.ENABLED }
    }

    fun setMode(context: Context, mode: HapticMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_HAPTIC, mode.name).apply()
    }

    fun performTick(context: Context, view: View? = null) {
        if (!shouldVibrate(context)) return
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            vibrate(context, 10)
        }
    }

    fun performClick(context: Context, view: View? = null) {
        if (!shouldVibrate(context)) return
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            vibrate(context, 20)
        }
    }

    fun performLongPress(context: Context, view: View? = null) {
        if (!shouldVibrate(context)) return
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } else {
            vibrate(context, 30)
        }
    }

    private fun shouldVibrate(context: Context): Boolean {
        return when (getMode(context)) {
            HapticMode.DISABLED -> false
            HapticMode.ENABLED, HapticMode.APP_DRAWER_ONLY, HapticMode.FOLLOW_SYSTEM -> true
        }
    }

    private fun vibrate(context: Context, durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vm.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }
}
