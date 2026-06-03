package com.ratio.launcher.utils

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    fun exportConfig(context: Context): String {
        val backup = JSONObject()

        // Dock
        val dockPrefs = context.getSharedPreferences("ratio_dock", Context.MODE_PRIVATE)
        backup.put("dock_packages", dockPrefs.getString("dock_packages", ""))

        // Hidden apps
        val hidden = HiddenAppsManager.getHiddenPackages(context)
        backup.put("hidden_apps", JSONArray(hidden.toList()))

        // Custom drawers
        val drawers = DrawerManager.getCustomDrawers(context)
        val drawersArray = JSONArray()
        drawers.forEach { d ->
            drawersArray.put(JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("packages", JSONArray(d.packages))
                put("position", d.position)
            })
        }
        backup.put("custom_drawers", drawersArray)

        // Settings
        val prefs = context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
        val settings = JSONObject()
        settings.put("theme", prefs.getString("theme", "dark"))
        settings.put("clock_24h", prefs.getBoolean("clock_24h", true))
        settings.put("monochrome_icons", prefs.getBoolean("monochrome_icons", true))
        settings.put("double_tap_lock", prefs.getBoolean("double_tap_lock", false))
        settings.put("faster_animations", prefs.getBoolean("faster_animations", false))
        settings.put("large_tiles", prefs.getBoolean("large_tiles", false))
        settings.put("show_drawer_icons", prefs.getBoolean("show_drawer_icons", true))
        settings.put("search_engine", prefs.getString("search_engine", "GOOGLE"))
        settings.put("haptic_feedback", prefs.getString("haptic_feedback", "ENABLED"))
        backup.put("settings", settings)

        // Weather
        val weatherCity = WeatherHelper.getCity(context)
        backup.put("weather_city", weatherCity)

        // Usage goal
        backup.put("usage_goal_minutes", UsageGoalsManager.getDailyGoal(context))

        // Focus schedules
        val focusPrefs = context.getSharedPreferences("ratio_focus_hours", Context.MODE_PRIVATE)
        backup.put("focus_schedules", focusPrefs.getString("schedules", "[]"))

        return backup.toString(2)
    }

    fun importConfig(context: Context, json: String): Boolean {
        return try {
            val backup = JSONObject(json)

            // Dock
            val dockPackages = backup.optString("dock_packages", "")
            context.getSharedPreferences("ratio_dock", Context.MODE_PRIVATE)
                .edit().putString("dock_packages", dockPackages).apply()

            // Hidden apps
            val hiddenArray = backup.optJSONArray("hidden_apps")
            if (hiddenArray != null) {
                val hiddenSet = (0 until hiddenArray.length()).map { hiddenArray.getString(it) }.toSet()
                context.getSharedPreferences("ratio_hidden_apps", Context.MODE_PRIVATE)
                    .edit().putStringSet("hidden_packages", hiddenSet).apply()
            }

            // Custom drawers
            val drawersArray = backup.optJSONArray("custom_drawers")
            if (drawersArray != null) {
                context.getSharedPreferences("ratio_drawers", Context.MODE_PRIVATE)
                    .edit().putString("custom_drawers", drawersArray.toString()).apply()
            }

            // Settings
            val settings = backup.optJSONObject("settings")
            if (settings != null) {
                val prefs = context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE).edit()
                prefs.putString("theme", settings.optString("theme", "dark"))
                prefs.putBoolean("clock_24h", settings.optBoolean("clock_24h", true))
                prefs.putBoolean("monochrome_icons", settings.optBoolean("monochrome_icons", true))
                prefs.putBoolean("double_tap_lock", settings.optBoolean("double_tap_lock", false))
                prefs.putBoolean("faster_animations", settings.optBoolean("faster_animations", false))
                prefs.putBoolean("large_tiles", settings.optBoolean("large_tiles", false))
                prefs.putBoolean("show_drawer_icons", settings.optBoolean("show_drawer_icons", true))
                prefs.putString("search_engine", settings.optString("search_engine", "GOOGLE"))
                prefs.putString("haptic_feedback", settings.optString("haptic_feedback", "ENABLED"))
                prefs.apply()
            }

            // Weather
            val city = backup.optString("weather_city", "")
            if (city.isNotBlank()) WeatherHelper.setCity(context, city)

            // Usage goal
            val goal = backup.optInt("usage_goal_minutes", 240)
            UsageGoalsManager.setDailyGoal(context, goal)

            // Focus schedules
            val focusJson = backup.optString("focus_schedules", "[]")
            context.getSharedPreferences("ratio_focus_hours", Context.MODE_PRIVATE)
                .edit().putString("schedules", focusJson).apply()

            true
        } catch (_: Exception) {
            false
        }
    }

    fun exportToUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = exportConfig(context)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun importFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return false
            importConfig(context, json)
        } catch (_: Exception) {
            false
        }
    }
}
