package com.ratio.launcher.utils

import android.content.Context
import org.json.JSONArray

object RootCardOrder {

    private const val PREFS = "ratio_prefs"
    private const val KEY = "root_card_order"

    val DEFAULT_ORDER = listOf("screen_time", "media", "weather", "calendar", "notes")

    fun getOrder(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null) ?: return DEFAULT_ORDER
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) {
            DEFAULT_ORDER
        }
    }

    fun setOrder(context: Context, order: List<String>) {
        val array = JSONArray(order)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }

    fun moveUp(context: Context, card: String) {
        val order = getOrder(context).toMutableList()
        val index = order.indexOf(card)
        if (index > 0) {
            order.removeAt(index)
            order.add(index - 1, card)
            setOrder(context, order)
        }
    }

    fun moveDown(context: Context, card: String) {
        val order = getOrder(context).toMutableList()
        val index = order.indexOf(card)
        if (index >= 0 && index < order.size - 1) {
            order.removeAt(index)
            order.add(index + 1, card)
            setOrder(context, order)
        }
    }
}
