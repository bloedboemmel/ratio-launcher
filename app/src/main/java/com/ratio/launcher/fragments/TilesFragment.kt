package com.ratio.launcher.fragments

import android.annotation.SuppressLint
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
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
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
        savedInstanceState: Bundle?,
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
        applyWallpaperStyle()
    }

    private fun applyWallpaperStyle() {
        val hasWallpaper = com.ratio.launcher.utils.WallpaperManager.hasWallpaperImage(requireContext())
        val searchBg = if (hasWallpaper) R.drawable.search_bg_wallpaper else R.drawable.search_bg
        searchBar.setBackgroundResource(searchBg)
    }

    fun openKeyboard() {
        searchBar.requestFocus()
        searchBar.postDelayed(
            {
            if (isAdded) {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            },
            200,
        )
    }

    private fun rebuildAppList() {
        val context = context ?: return
        val prefs = context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
        val monochrome = prefs.getBoolean("monochrome_icons", true)
        val columns = AnimationHelper.getTileSizeColumns(context)

        // Capture the application context so the background thread never calls
        // requireContext() on a fragment that may have detached in the meantime (#20).
        val appContext = context.applicationContext
        Thread {
            val apps = loadInstalledApps(appContext)
            Handler(Looper.getMainLooper()).post {
                if (!isAdded) return@post
                allApps = apps
                val catComparator = CategoryOrder.getSortComparator(appContext)
                val wallpaperActive = com.ratio.launcher.utils.WallpaperManager.hasWallpaperImage(appContext)
                adapter = AppListAdapter(
                    allApps,
                    monochrome,
                    { app -> addToDock(app) },
                    categoryComparator = catComparator,
                    hasWallpaper = wallpaperActive,
                ) { app -> launchApp(app) }

                val gridLayoutManager = GridLayoutManager(appContext, columns)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearch() {
        searchBar.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (::adapter.isInitialized) {
                        adapter.filter(s?.toString() ?: "")
                    }
                    updateClearButton(s?.isNotEmpty() == true)
                }
                override fun afterTextChanged(s: Editable?) {}
            },
        )

        // Tapping the trailing clear icon empties the search bar (#21).
        searchBar.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val clearDrawable = searchBar.compoundDrawablesRelative[2]
                if (clearDrawable != null) {
                    val touchAreaStart = searchBar.width - searchBar.paddingEnd - clearDrawable.bounds.width()
                    if (event.x >= touchAreaStart) {
                        clearSearch()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        updateClearButton(searchBar.text?.isNotEmpty() == true)
    }

    private fun updateClearButton(show: Boolean) {
        val start = searchBar.compoundDrawablesRelative[0]
        val end = if (show) {
            androidx.core.content.ContextCompat.getDrawable(searchBar.context, R.drawable.ic_close)
        } else {
            null
        }
        searchBar.setCompoundDrawablesRelativeWithIntrinsicBounds(start, null, end, null)
    }

    private fun clearSearch() {
        searchBar.setText("")
        if (::adapter.isInitialized) {
            adapter.filter("")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCollapseGesture() {
        val gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if ((e1 != null) && (velocityY > 500) && ((e2.y - e1.y) > 100)) {
                        adapter.collapseAll()
                        return true
                    }
                    if ((e1 != null) && (velocityY < -500) && ((e1.y - e2.y) > 100)) {
                        adapter.expandAll()
                        return true
                    }
                    return false
                }
            },
        )

        appList.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }



    private fun loadInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val hiddenPackages = HiddenAppsManager.getHiddenPackages(context)
        val customCategories = context.getSharedPreferences("ratio_app_categories", Context.MODE_PRIVATE)
        val ownPackageName = context.packageName

        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != ownPackageName }
            .filter { !hiddenPackages.contains(it.activityInfo.packageName) }
            .map { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val pkg = resolveInfo.activityInfo.packageName
                val customCategory = customCategories.getString(pkg, null)
                AppInfo(
                    name = resolveInfo.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = resolveInfo.loadIcon(pm),
                    category = customCategory ?: categorizeApp(appInfo, pm),
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    private fun categorizeApp(appInfo: ApplicationInfo, pm: PackageManager): String {
        return when (appInfo.category) {
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
        io.sentry.Sentry.metrics().count("action_add_to_dock_${app.packageName}")
        val prefs = requireContext().getSharedPreferences("ratio_dock", Context.MODE_PRIVATE)
        val current = prefs.getString("dock_packages", "") ?: ""
        val packages = current.split(",").asSequence().filter { it.isNotBlank() }.toMutableList()
        if (!packages.contains(app.packageName)) {
            packages.add(app.packageName)
            prefs.edit { putString("dock_packages", packages.joinToString(",")) }
            Toast.makeText(requireContext(), "${app.name} added to dock", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "${app.name} already in dock", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(app: AppInfo) {
        if (!com.ratio.launcher.utils.DetoxMode.isAppAllowed(requireContext(), app.packageName)) {
            Toast.makeText(requireContext(), "Blocked — Detox mode active", Toast.LENGTH_SHORT).show()
            return
        }
        if (com.ratio.launcher.utils.AppTimerManager.isLimitReached(requireContext(), app.packageName)) {
            Toast.makeText(requireContext(), "Daily limit reached for ${app.name}", Toast.LENGTH_SHORT).show()
            return
        }
        io.sentry.Sentry.metrics().count("app_launched_${app.packageName}")
        val intent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
        intent?.let {
            startActivity(it)
            // Auto-clear the search bar so the next visit starts fresh (#21).
            clearSearch()
        }
    }
}
