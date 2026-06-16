package com.ratio.launcher.utils

import android.content.Context

object DetoxMode {

    private const val PREFS = "ratio_detox"
    private const val KEY_ACTIVE = "detox_active"
    private const val KEY_END_TIME = "detox_end_time"
    private const val KEY_ALLOWED_APPS = "allowed_apps"

    private val DEFAULT_ALLOWED = setOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.contacts",
        "com.android.settings",
        "com.google.android.apps.messaging",
        "com.android.mms"
    )

    fun isActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return false
        val endTime = prefs.getLong(KEY_END_TIME, 0)
        if (endTime > 0 && System.currentTimeMillis() > endTime) {
            deactivate(context)
            return false
        }
        return true
    }

    fun activate(context: Context, durationMinutes: Int = 0) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(KEY_ACTIVE, true)
        if (durationMinutes > 0) {
            editor.putLong(KEY_END_TIME, System.currentTimeMillis() + durationMinutes * 60 * 1000L)
        } else {
            editor.putLong(KEY_END_TIME, 0)
        }
        editor.apply()
    }

    fun deactivate(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACTIVE, false)
            .putLong(KEY_END_TIME, 0)
            .apply()
    }

    fun getRemainingMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_END_TIME, 0)
        if (endTime <= 0) return -1
        val remaining = (endTime - System.currentTimeMillis()) / 60000
        return remaining.toInt().coerceAtLeast(0)
    }

    fun isAppAllowed(context: Context, packageName: String): Boolean {
        if (!isActive(context)) return true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val allowed = prefs.getStringSet(KEY_ALLOWED_APPS, DEFAULT_ALLOWED) ?: DEFAULT_ALLOWED
        return allowed.contains(packageName) || packageName == context.packageName
    }

    fun getAllowedApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ALLOWED_APPS, DEFAULT_ALLOWED) ?: DEFAULT_ALLOWED
    }

    fun setAllowedApps(context: Context, apps: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_ALLOWED_APPS, apps).apply()
    }
}
