package com.ratio.launcher.adapters

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.R
import com.ratio.launcher.models.AppInfo

class DockAdapter(
    private val apps: MutableList<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, Int) -> Boolean
) : RecyclerView.Adapter<DockAdapter.DockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dock_app, parent, false)
        return DockViewHolder(view)
    }

    override fun onBindViewHolder(holder: DockViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class DockViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.dockIcon)

        fun bind(app: AppInfo) {
            icon.setImageDrawable(app.icon.mutate())

            val monochrome = itemView.context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
                .getBoolean("monochrome_icons", true)

            if (monochrome) {
                val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
                icon.colorFilter = ColorMatrixColorFilter(colorMatrix)
            } else {
                icon.colorFilter = null
            }

            itemView.setOnClickListener { onAppClick(app) }
            itemView.setOnLongClickListener {
                onAppLongClick(app, bindingAdapterPosition)
            }
        }
    }
}
