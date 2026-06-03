package com.ratio.launcher.utils

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CustomDrawer(
    val id: String,
    val name: String,
    val packages: List<String>,
    val position: Int
)

object DrawerManager {

    private const val PREFS = "ratio_drawers"
    private const val KEY_DRAWERS = "custom_drawers"
    private const val KEY_APP_ASSIGNMENTS = "app_assignments"

    fun getCustomDrawers(context: Context): List<CustomDrawer> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DRAWERS, "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val packages = obj.getJSONArray("packages")
            CustomDrawer(
                id = obj.getString("id"),
                name = obj.getString("name"),
                packages = (0 until packages.length()).map { packages.getString(it) },
                position = obj.optInt("position", i)
            )
        }
    }

    fun saveDrawers(context: Context, drawers: List<CustomDrawer>) {
        val array = JSONArray()
        drawers.forEach { drawer ->
            array.put(JSONObject().apply {
                put("id", drawer.id)
                put("name", drawer.name)
                put("packages", JSONArray(drawer.packages))
                put("position", drawer.position)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_DRAWERS, array.toString()).apply()
    }

    fun addDrawer(context: Context, name: String): CustomDrawer {
        val drawers = getCustomDrawers(context).toMutableList()
        val drawer = CustomDrawer(
            id = "drawer_${System.currentTimeMillis()}",
            name = name,
            packages = emptyList(),
            position = drawers.size
        )
        drawers.add(drawer)
        saveDrawers(context, drawers)
        return drawer
    }

    fun renameDrawer(context: Context, id: String, newName: String) {
        val drawers = getCustomDrawers(context).toMutableList()
        val index = drawers.indexOfFirst { it.id == id }
        if (index >= 0) {
            drawers[index] = drawers[index].copy(name = newName)
            saveDrawers(context, drawers)
        }
    }

    fun deleteDrawer(context: Context, id: String) {
        val drawers = getCustomDrawers(context).toMutableList()
        drawers.removeAll { it.id == id }
        saveDrawers(context, drawers)
    }

    fun assignAppToDrawer(context: Context, packageName: String, drawerId: String) {
        val drawers = getCustomDrawers(context).toMutableList()
        drawers.forEachIndexed { i, drawer ->
            val pkgs = drawer.packages.toMutableList()
            pkgs.remove(packageName)
            drawers[i] = drawer.copy(packages = pkgs)
        }
        val index = drawers.indexOfFirst { it.id == drawerId }
        if (index >= 0) {
            val pkgs = drawers[index].packages.toMutableList()
            pkgs.add(packageName)
            drawers[index] = drawers[index].copy(packages = pkgs)
        }
        saveDrawers(context, drawers)
    }

    fun getAppDrawer(context: Context, packageName: String): String? {
        return getCustomDrawers(context)
            .firstOrNull { it.packages.contains(packageName) }
            ?.name
    }
}
