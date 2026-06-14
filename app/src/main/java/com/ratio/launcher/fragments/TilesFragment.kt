package com.ratio.launcher.fragments

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.R
import com.ratio.launcher.adapters.AppListAdapter
import com.ratio.launcher.models.AppInfo
import com.ratio.launcher.utils.AnimationHelper
import com.ratio.launcher.utils.CategoryOrder
import com.ratio.launcher.utils.HiddenAppsManager

class TilesFragment : Fragment() {

    private lateinit var appList: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var adapter: AppListAdapter

    private var allApps: List<AppInfo> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tiles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appList = view.findViewById(R.id.appList)
        searchBar = view.findViewById(R.id.searchBar)

        rebuildAppList()
        setupSearch()
        setupCollapseGesture()
    }

    override fun onResume() {
        super.onResume()
        rebuildAppList()
    }

    fun openKeyboard() {
        searchBar.requestFocus()
        searchBar.postDelayed({
            if (isAdded) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }, 200)
    }

    private fun rebuildAppList() {
        val context = context ?: return
        val prefs = context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
        val monochrome = prefs.getBoolean("monochrome_icons", true)
        val columns = AnimationHelper.getTileSizeColumns(context)

        Thread {
            val apps = loadInstalledApps()
            Handler(Looper.getMainLooper()).post {
                if (!isAdded) return@post
                allApps = apps
                val catComparator = CategoryOrder.getSortComparator(requireContext())
                adapter = AppListAdapter(allApps, monochrome, { app ->
                    addToDock(app)
                }, categoryComparator = catComparator) { app -> launchApp(app) }

                val gridLayoutManager = GridLayoutManager(requireContext(), columns)
                gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (adapter.isHeader(position)) columns else 1
                    }
                }
                appList.layoutManager = gridLayoutManager
                appList.adapter = adapter
            }
        }.start()
    }

    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (::adapter.isInitialized) {
                    adapter.filter(s?.toString() ?: "")
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCollapseGesture() {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && velocityY > 500 && e2.y - e1.y > 100) {
                    adapter.collapseAll()
                    return true
                }
                if (e1 != null && velocityY < -500 && e1.y - e2.y > 100) {
                    adapter.expandAll()
                    return true
                }
                return false
            }
        })

        appList.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    fun showDrawer() {
        // No-op, drawer is always visible now
    }

    fun filterApps(query: String) {
        adapter.filter(query)
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = requireContext().packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val hiddenPackages = HiddenAppsManager.getHiddenPackages(requireContext())
        val customCategories = requireContext().getSharedPreferences("ratio_app_categories", Context.MODE_PRIVATE)

        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != requireContext().packageName }
            .filter { !hiddenPackages.contains(it.activityInfo.packageName) }
            .map { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val pkg = resolveInfo.activityInfo.packageName
                val customCategory = customCategories.getString(pkg, null)
                AppInfo(
                    name = resolveInfo.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = resolveInfo.loadIcon(pm),
                    category = customCategory ?: categorizeApp(appInfo, pm)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun categorizeApp(appInfo: ApplicationInfo, pm: PackageManager): String {
        val category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            appInfo.category
        } else {
            ApplicationInfo.CATEGORY_UNDEFINED
        }

        return when (category) {
            ApplicationInfo.CATEGORY_GAME -> "Games"
            ApplicationInfo.CATEGORY_AUDIO -> "Media"
            ApplicationInfo.CATEGORY_VIDEO -> "Media"
            ApplicationInfo.CATEGORY_IMAGE -> "Media"
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_NEWS -> "News"
            ApplicationInfo.CATEGORY_MAPS -> "Travel"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
            else -> {
                val name = appInfo.loadLabel(pm).toString().lowercase()
                when {
                    name.contains("mail") || name.contains("email") -> "Communication"
                    name.contains("message") || name.contains("chat") -> "Communication"
                    name.contains("phone") || name.contains("call") || name.contains("dial") -> "Communication"
                    name.contains("camera") || name.contains("photo") || name.contains("gallery") -> "Media"
                    name.contains("music") || name.contains("spotify") || name.contains("podcast") -> "Media"
                    name.contains("map") || name.contains("uber") || name.contains("lyft") -> "Travel"
                    name.contains("bank") || name.contains("pay") || name.contains("wallet") -> "Finance"
                    name.contains("shop") || name.contains("store") || name.contains("amazon") -> "Shopping"
                    name.contains("fit") || name.contains("health") || name.contains("sport") -> "Health"
                    name.contains("setting") || name.contains("system") -> "System"
                    else -> "Other"
                }
            }
        }
    }

    private fun addToDock(app: AppInfo) {
        val prefs = requireContext().getSharedPreferences("ratio_dock", Context.MODE_PRIVATE)
        val current = prefs.getString("dock_packages", "") ?: ""
        val packages = current.split(",").filter { it.isNotBlank() }.toMutableList()
        if (!packages.contains(app.packageName)) {
            packages.add(app.packageName)
            prefs.edit().putString("dock_packages", packages.joinToString(",")).apply()
            Toast.makeText(requireContext(), "${app.name} added to dock", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "${app.name} already in dock", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(app: AppInfo) {
        io.sentry.Sentry.metrics().count("app_launched")
        val intent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
        intent?.let { startActivity(it) }
    }
}
