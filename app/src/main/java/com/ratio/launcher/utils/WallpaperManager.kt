package com.ratio.launcher.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri

object WallpaperManager {

    private const val PREFS = "ratio_wallpaper"
    private const val KEY_MODE = "mode"
    private const val KEY_IMAGE_URI = "image_uri"

    enum class WallpaperMode { SOLID_COLOR, IMAGE }

    fun getMode(context: Context): WallpaperMode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, "SOLID_COLOR") ?: "SOLID_COLOR"
        return try { WallpaperMode.valueOf(mode) } catch (_: Exception) { WallpaperMode.SOLID_COLOR }
    }

    fun setMode(context: Context, mode: WallpaperMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_MODE, mode.name) }
    }

    fun getImageUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_IMAGE_URI, null) ?: return null
        return uri.toUri()
    }

    fun setImageUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_IMAGE_URI, uri?.toString()) }
    }

    fun getAccentColor(context: Context): Int {
        val prefs = context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("custom_accent_color", "#FFFC33".toColorInt())
    }

    fun setAccentColor(context: Context, color: Int) {
        context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
            .edit { putInt("custom_accent_color", color) }
    }

    fun hasWallpaperImage(context: Context): Boolean {
        return (getMode(context) == WallpaperMode.IMAGE) && (getImageUri(context) != null)
    }
}
