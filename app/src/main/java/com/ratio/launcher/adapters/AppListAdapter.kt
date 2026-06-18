package com.ratio.launcher.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.R
import com.ratio.launcher.models.AppInfo
import com.ratio.launcher.utils.HiddenAppsManager

enum class DrawerSortMode { ALPHABETICAL, FREQUENCY }

sealed class TileItem {
    data class Header(val category: String, val count: Int, var expanded: Boolean = true) : TileItem()
    data class App(val appInfo: AppInfo) : TileItem()
}

class AppListAdapter(
    private val allApps: List<AppInfo>,
    private val monochrome: Boolean = true,
    private val onAppLongPress: ((AppInfo) -> Unit)? = null,
    private val sortMode: DrawerSortMode = DrawerSortMode.ALPHABETICAL,
    private val categoryComparator: Comparator<String>? = null,
    private val hasWallpaper: Boolean = false,
    private val onAppClick: (AppInfo) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: MutableList<TileItem> = mutableListOf()
    private val categories: MutableMap<String, Boolean> = mutableMapOf()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_APP = 1
    }

    init {
        buildList(allApps)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.name.contains(query, ignoreCase = true) }
        }
        buildList(filtered)
        notifyDataSetChanged()
    }

    private fun buildList(apps: List<AppInfo>) {
        items.clear()
        val grouped = apps.groupBy { it.category }
        val sortedCategories = if (categoryComparator != null) {
            grouped.keys.sortedWith(categoryComparator)
        } else {
            grouped.keys.sortedWith(compareBy { if (it == "Other") "zzz" else it.lowercase() })
        }
        for (category in sortedCategories) {
            val categoryApps = grouped[category] ?: continue
            val expanded = categories.getOrDefault(category, defaultValue = true)
            items.add(TileItem.Header(category, categoryApps.size, expanded))
            if (expanded) {
                val sorted = when (sortMode) {
                    DrawerSortMode.ALPHABETICAL -> categoryApps.sortedBy { it.name.lowercase() }
                    DrawerSortMode.FREQUENCY -> categoryApps // already sorted by caller
                }
                sorted.forEach { items.add(TileItem.App(it)) }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun collapseAll() {
        categories.keys.forEach { categories[it] = false }
        buildList(allApps)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun expandAll() {
        categories.keys.forEach { categories[it] = true }
        buildList(allApps)
        notifyDataSetChanged()
    }

    private fun showCategoryPicker(context: Context, app: AppInfo) {
        val existingCategories = allApps.asSequence().map { it.category }.distinct().sorted().toMutableList()
        existingCategories.add("+ New category")

        AlertDialog.Builder(context)
            .setTitle("Move ${app.name} to:")
            .setItems(existingCategories.toTypedArray()) { _, which ->
                if (which == (existingCategories.size - 1)) {
                    showNewCategoryDialog(context, app)
                } else {
                    val newCategory = existingCategories[which]
                    saveAppCategory(context, app.packageName, newCategory)
                    Toast.makeText(context, "Moved to $newCategory", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showNewCategoryDialog(context: Context, app: AppInfo) {
        val input = android.widget.EditText(context).apply {
            hint = "Category name"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(context)
            .setTitle("New category")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveAppCategory(context, app.packageName, name)
                    Toast.makeText(context, "Moved to $name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAppCategory(context: Context, packageName: String, category: String) {
        val prefs = context.getSharedPreferences("ratio_app_categories", Context.MODE_PRIVATE)
        prefs.edit { putString(packageName, category) }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TileItem.Header -> TYPE_HEADER
            is TileItem.App -> TYPE_APP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_drawer_header, parent, false))
            else -> AppViewHolder(inflater.inflate(R.layout.item_app_tile, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TileItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TileItem.App -> (holder as AppViewHolder).bind(item.appInfo)
        }
    }

    override fun getItemCount(): Int = items.size

    fun isHeader(position: Int): Boolean = items.getOrNull(position) is TileItem.Header

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.drawerName)
        private val count: TextView = view.findViewById(R.id.drawerCount)
        private val arrow: ImageView = view.findViewById(R.id.drawerArrow)

        @SuppressLint("NotifyDataSetChanged")
        fun bind(header: TileItem.Header) {
            name.text = header.category
            count.text = header.count.toString()
            arrow.rotation = if (header.expanded) 0f else -90f

            itemView.setOnClickListener {
                header.expanded = !header.expanded
                categories[header.category] = header.expanded
                buildList(allApps)
                notifyDataSetChanged()
            }
        }
    }

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.appIcon)
        private val appName: TextView = view.findViewById(R.id.appName)
        private val tileContainer: View = (view as ViewGroup).getChildAt(0)

        fun bind(app: AppInfo) {
            appName.text = app.name

            // Apply wallpaper-friendly tile background if needed
            tileContainer.setBackgroundResource(
                if (hasWallpaper) R.drawable.ripple_tile_wallpaper else R.drawable.ripple_tile,
            )

            icon.setImageDrawable(app.icon.mutate())
            if (monochrome) {
                val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
                icon.colorFilter = ColorMatrixColorFilter(colorMatrix)
            } else {
                icon.colorFilter = null
            }

            itemView.setOnClickListener { onAppClick(app) }

            itemView.setOnLongClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Add to dock")
                popup.menu.add("Move to category")
                popup.menu.add("Hide")
                popup.menu.add("Uninstall")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Add to dock" -> {
                            onAppLongPress?.invoke(app)
                        }
                        "Move to category" -> {
                            showCategoryPicker(view.context, app)
                        }
                        "Hide" -> {
                            HiddenAppsManager.hideApp(view.context, app.packageName)
                            Toast.makeText(view.context, "${app.name} hidden", Toast.LENGTH_SHORT).show()
                            val idx = items.indexOfFirst { (it is TileItem.App) && (it.appInfo.packageName == app.packageName) }
                            if (idx >= 0) {
                                items.removeAt(idx)
                                notifyItemRemoved(idx)
                            }
                        }
                        "Uninstall" -> {
                            @Suppress("DEPRECATION")
                            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                data = "package:${app.packageName}".toUri()
                                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            view.context.startActivity(intent)
                        }
                    }
                    true
                }
                popup.show()
                true
            }
        }
    }
}
