package com.ratio.launcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.utils.AppUsageHelper
import com.ratio.launcher.utils.AppUsageInfo
import com.ratio.launcher.utils.UsageStatsHelper

class UsageBreakdownActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_breakdown)

        val totalMinutes = UsageStatsHelper.getTodayUsageMinutes(this)
        val totalText = findViewById<TextView>(R.id.usageTotalTime)
        if (totalMinutes >= 0) {
            totalText.text = "Total today: ${totalMinutes / 60}h ${totalMinutes % 60}m"
        } else {
            totalText.text = "Usage access not granted"
        }

        val list = findViewById<RecyclerView>(R.id.usageAppList)
        list.layoutManager = LinearLayoutManager(this)

        val apps = AppUsageHelper.getTodayPerAppUsage(this)
        list.adapter = UsageAppAdapter(apps)
    }
}

class UsageAppAdapter(
    private val apps: List<AppUsageInfo>
) : RecyclerView.Adapter<UsageAppAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usage_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.usageAppIcon)
        private val name: TextView = view.findViewById(R.id.usageAppName)
        private val time: TextView = view.findViewById(R.id.usageAppTime)

        fun bind(app: AppUsageInfo) {
            name.text = app.appName
            time.text = AppUsageHelper.formatUsageTime(app.usageMinutes)
            if (app.icon != null) {
                icon.setImageDrawable(app.icon)
                icon.visibility = View.VISIBLE
            } else {
                icon.visibility = View.INVISIBLE
            }
        }
    }
}
