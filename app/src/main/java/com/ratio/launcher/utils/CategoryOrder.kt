package com.ratio.launcher.utils

import android.content.Context
import org.json.JSONArray

object CategoryOrder {

    private const val PREFS = "ratio_prefs"
    private const val KEY = "category_order"

    fun getOrder(context: Context): List<String>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null) ?: return null
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) { null }
    }

    fun setOrder(context: Context, order: List<String>) {
        val array = JSONArray(order)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }

    fun getSortComparator(context: Context): Comparator<String> {
        val order = getOrder(context)
        return if (order != null) {
            Comparator { a, b ->
                val ia = order.indexOf(a).let { if (it == -1) 999 else it }
                val ib = order.indexOf(b).let { if (it == -1) 999 else it }
                ia.compareTo(ib)
            }
        } else {
            // Default: alphabetical with "Other" last
            Comparator { a, b ->
                if (a == "Other") 1
                else if (b == "Other") -1
                else a.lowercase().compareTo(b.lowercase())
            }
        }
    }
}
