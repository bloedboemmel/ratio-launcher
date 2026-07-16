package com.ratio.launcher.utils

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

data class ShortcutTile(
    val label: String,
    val packageName: String,
    val shortcutId: String,
    val intentUri: String
)

object ShortcutTiles {

    private const val PREFS = "ratio_shortcuts"
    private const val KEY = "pinned_shortcuts"

    fun getPinnedShortcuts(context: Context): List<ShortcutTile> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            ShortcutTile(
                label = obj.getString("label"),
                packageName = obj.getString("packageName"),
                shortcutId = obj.getString("shortcutId"),
                intentUri = obj.getString("intentUri")
            )
        }
    }

    fun addShortcut(context: Context, shortcut: ShortcutTile) {
        val current = getPinnedShortcuts(context).toMutableList()
        current.add(shortcut)
        save(context, current)
    }

    fun removeShortcut(context: Context, shortcutId: String) {
        val current = getPinnedShortcuts(context).toMutableList()
        current.removeAll { it.shortcutId == shortcutId }
        save(context, current)
    }

    fun launchShortcut(context: Context, shortcut: ShortcutTile): Boolean {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE)
                as android.content.pm.LauncherApps
            launcherApps.startShortcut(
                shortcut.packageName, shortcut.shortcutId, null, null,
                android.os.Process.myUserHandle()
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getAppShortcuts(context: Context, packageName: String): List<ShortcutTile> {
        val shortcuts = mutableListOf<ShortcutTile>()
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE)
                as android.content.pm.LauncherApps
            val userHandle = android.os.Process.myUserHandle()
            val shortcutQuery = android.content.pm.LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )
            }

            val appShortcuts = launcherApps.getShortcuts(shortcutQuery, userHandle) ?: emptyList()
            for (s in appShortcuts) {
                val label = s.shortLabel?.toString() ?: s.longLabel?.toString() ?: continue
                shortcuts.add(ShortcutTile(
                    label = label,
                    packageName = packageName,
                    shortcutId = s.id,
                    intentUri = ""
                ))
            }
        } catch (_: Exception) {}
        return shortcuts
    }

    private fun save(context: Context, shortcuts: List<ShortcutTile>) {
        val array = JSONArray()
        shortcuts.forEach { s ->
            array.put(JSONObject().apply {
                put("label", s.label)
                put("packageName", s.packageName)
                put("shortcutId", s.shortcutId)
                put("intentUri", s.intentUri)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }
}
