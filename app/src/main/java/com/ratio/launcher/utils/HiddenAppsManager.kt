package com.ratio.launcher.utils

import android.content.Context

object HiddenAppsManager {

    private const val PREFS = "ratio_hidden_apps"
    private const val KEY = "hidden_packages"

    fun getHiddenPackages(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet()) ?: emptySet()
    }

    fun hideApp(context: Context, packageName: String) {
        val current = getHiddenPackages(context).toMutableSet()
        current.add(packageName)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, current).apply()
    }

    fun unhideApp(context: Context, packageName: String) {
        val current = getHiddenPackages(context).toMutableSet()
        current.remove(packageName)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, current).apply()
    }

    fun unhideAll(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, emptySet()).apply()
    }

    fun isHidden(context: Context, packageName: String): Boolean {
        return getHiddenPackages(context).contains(packageName)
    }
}
