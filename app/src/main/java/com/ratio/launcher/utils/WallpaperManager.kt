package com.ratio.launcher.utils

import android.content.Context
import android.graphics.Color
import android.net.Uri

object WallpaperManager {

    private const val PREFS = "ratio_wallpaper"
    private const val KEY_MODE = "mode"
    private const val KEY_COLOR = "solid_color"
    private const val KEY_IMAGE_URI = "image_uri"

    enum class WallpaperMode { SOLID_COLOR, IMAGE }

    fun getMode(context: Context): WallpaperMode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, "SOLID_COLOR") ?: "SOLID_COLOR"
        return try { WallpaperMode.valueOf(mode) } catch (_: Exception) { WallpaperMode.SOLID_COLOR }
    }

    fun setMode(context: Context, mode: WallpaperMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getSolidColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COLOR, Color.parseColor("#0B0B0B"))
    }

    fun setSolidColor(context: Context, color: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_COLOR, color).apply()
    }

    fun getImageUri(context: Context): Uri? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_IMAGE_URI, null) ?: return null
        return Uri.parse(uri)
    }

    fun setImageUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_IMAGE_URI, uri?.toString()).apply()
    }

    fun getAccentColor(context: Context): Int {
        val prefs = context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("custom_accent_color", Color.parseColor("#FFFC33"))
    }

    fun setAccentColor(context: Context, color: Int) {
        context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
            .edit().putInt("custom_accent_color", color).apply()
    }
}
