package com.ratio.launcher

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ratio.launcher.utils.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsCity: EditText
    private lateinit var clock24hSwitch: SwitchMaterial
    private lateinit var monochromeSwitch: SwitchMaterial
    private lateinit var doubleTapSwitch: SwitchMaterial
    private lateinit var themeValue: TextView
    private lateinit var usageGoalValue: TextView
    private lateinit var hiddenAppsCount: TextView
    private lateinit var tempUnitValue: TextView
    private lateinit var largeTilesSwitch: SwitchMaterial
    private lateinit var iconPackValue: TextView
    private lateinit var showMusicSwitch: SwitchMaterial
    private lateinit var showCalendarSwitch: SwitchMaterial
    private lateinit var showWeatherSwitch: SwitchMaterial
    private lateinit var showNotesSwitch: SwitchMaterial
    private lateinit var showScreenTimeSwitch: SwitchMaterial
    private lateinit var hideStatusBarSwitch: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        loadSettings()
        setupListeners()
    }

    private fun bindViews() {
        settingsCity = findViewById(R.id.settingsCity)
        clock24hSwitch = findViewById(R.id.settingsClock24h)
        monochromeSwitch = findViewById(R.id.settingsMonochrome)
        doubleTapSwitch = findViewById(R.id.settingsDoubleTapLock)
        themeValue = findViewById(R.id.settingThemeValue)
        usageGoalValue = findViewById(R.id.settingUsageGoalValue)
        hiddenAppsCount = findViewById(R.id.settingHiddenAppsCount)
        tempUnitValue = findViewById(R.id.settingTempUnit)
        largeTilesSwitch = findViewById(R.id.settingsLargeTiles)
        iconPackValue = findViewById(R.id.settingIconPackValue)
        showMusicSwitch = findViewById(R.id.settingsShowMusic)
        showCalendarSwitch = findViewById(R.id.settingsShowCalendar)
        showWeatherSwitch = findViewById(R.id.settingsShowWeather)
        showNotesSwitch = findViewById(R.id.settingsShowNotes)
        showScreenTimeSwitch = findViewById(R.id.settingsShowScreenTime)
        hideStatusBarSwitch = findViewById(R.id.settingsHideStatusBar)
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("ratio_prefs", MODE_PRIVATE)

        settingsCity.setText(WeatherHelper.getCity(this))
        clock24hSwitch.isChecked = prefs.getBoolean("clock_24h", true)
        monochromeSwitch.isChecked = prefs.getBoolean("monochrome_icons", true)
        doubleTapSwitch.isChecked = prefs.getBoolean("double_tap_lock", false)

        val theme = RatioTheme.getCurrent(this)
        themeValue.text = theme.key.replaceFirstChar { it.uppercase() }


        val goal = UsageGoalsManager.getDailyGoal(this)
        usageGoalValue.text = getString(R.string.usage_goal_format, goal / 60, goal % 60)

        val hidden = HiddenAppsManager.getHiddenPackages(this)
        hiddenAppsCount.text = hidden.size.toString()

        tempUnitValue.text = if (WeatherHelper.isMetric(this)) "°C" else "°F"
        largeTilesSwitch.isChecked = prefs.getBoolean("large_tiles", false)

        val currentPack = IconPackManager.getCurrentIconPack(this)
        iconPackValue.text = if (currentPack != null) {
            try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(currentPack, 0)).toString() }
            catch (_: Exception) { "Default" }
        } else "Default"

        showMusicSwitch.isChecked = prefs.getBoolean("show_music", true)
        showCalendarSwitch.isChecked = prefs.getBoolean("show_calendar", true)
        showWeatherSwitch.isChecked = prefs.getBoolean("show_weather", true)
        showNotesSwitch.isChecked = prefs.getBoolean("show_notes", true)
        showScreenTimeSwitch.isChecked = prefs.getBoolean("show_screen_time", true)
        hideStatusBarSwitch.isChecked = prefs.getBoolean("hide_status_bar", false)
    }

    private fun setupListeners() {
        val prefs = getSharedPreferences("ratio_prefs", MODE_PRIVATE)

        clock24hSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("clock_24h", checked) }
        }

        monochromeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("monochrome_icons", checked) }
        }

        doubleTapSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("double_tap_lock", checked) }
            if (checked && !GestureHelper.isDeviceAdminEnabled(this)) {
                GestureHelper.requestDeviceAdmin(this)
            }
        }

        findViewById<LinearLayout>(R.id.settingTheme).setOnClickListener {
            showThemeDialog()
        }

        val clockStyleValue = findViewById<TextView>(R.id.settingClockStyleValue)
        clockStyleValue.text = ClockStyle.getCurrent(this).displayName
        findViewById<LinearLayout>(R.id.settingClockStyle).setOnClickListener {
            startActivity(android.content.Intent(this, ClockPickerActivity::class.java))
        }

        val showSecondsSwitch = findViewById<SwitchMaterial>(R.id.settingsShowSeconds)
        showSecondsSwitch.isChecked = prefs.getBoolean("show_seconds", false)
        showSecondsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("show_seconds", checked) }
        }

        findViewById<LinearLayout>(R.id.settingUsageGoal).setOnClickListener {
            showUsageGoalDialog()
        }

        findViewById<LinearLayout>(R.id.settingHiddenApps).setOnClickListener {
            showHiddenAppsDialog()
        }

        tempUnitValue.setOnClickListener {
            val metric = WeatherHelper.isMetric(this)
            WeatherHelper.setUnit(this, !metric)
            tempUnitValue.text = if (!metric) "°C" else "°F"
        }

        largeTilesSwitch.setOnCheckedChangeListener { _, checked ->
            AnimationHelper.setLargeTiles(this, checked)
        }

        showMusicSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("show_music", checked) }
        }
        showCalendarSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("show_calendar", checked) }
        }
        showWeatherSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("show_weather", checked) }
        }
        showNotesSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("show_notes", checked) }
        }
        showScreenTimeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("show_screen_time", checked) }
        }

        hideStatusBarSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("hide_status_bar", checked) }
        }

        findViewById<LinearLayout>(R.id.settingIconPack).setOnClickListener {
            showIconPackDialog()
        }

        findViewById<LinearLayout>(R.id.settingBackup).setOnClickListener {
            showBackupDialog()
        }

        // Detox
        val detoxStatus = findViewById<TextView>(R.id.settingDetoxStatus)
        detoxStatus.text = if (DetoxMode.isActive(this)) "Active" else "Off"
        findViewById<LinearLayout>(R.id.settingDetox).setOnClickListener {
            showDetoxDialog(detoxStatus)
        }

        // Usage timer
        val usageTimerSwitch = findViewById<SwitchMaterial>(R.id.settingsUsageTimer)
        usageTimerSwitch.isChecked = prefs.getBoolean("usage_timer_enabled", false)
        usageTimerSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("usage_timer_enabled", checked) }
            if (checked) {
                com.ratio.launcher.services.UsageTimerService.start(this)
            } else {
                com.ratio.launcher.services.UsageTimerService.stop(this)
            }
        }

        // Quick gestures
        findViewById<LinearLayout>(R.id.settingGestures).setOnClickListener {
            showGesturesDialog()
        }

        // Wallpaper
        findViewById<LinearLayout>(R.id.settingWallpaper).setOnClickListener {
            showWallpaperDialog()
        }

        // Accent color
        val accentPreview = findViewById<android.view.View>(R.id.settingAccentPreview)
        accentPreview.setBackgroundColor(WallpaperManager.getAccentColor(this))
        findViewById<LinearLayout>(R.id.settingAccentColor).setOnClickListener {
            showAccentColorDialog(accentPreview)
        }

        findViewById<LinearLayout>(R.id.settingOnboarding).setOnClickListener {
            OnboardingActivity.resetOnboarding(this)
            startActivity(android.content.Intent(this, OnboardingActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.settingSendFeedback).setOnClickListener {
            showFeedbackDialog()
        }

        findViewById<LinearLayout>(R.id.settingReorderCategories).setOnClickListener {
            startActivity(
                android.content.Intent(this, ReorderActivity::class.java).apply {
                    putExtra(ReorderActivity.EXTRA_MODE, ReorderActivity.MODE_CATEGORIES)
                },
            )
        }

        findViewById<LinearLayout>(R.id.settingReorderCards).setOnClickListener {
            startActivity(
                android.content.Intent(this, ReorderActivity::class.java).apply {
                    putExtra(ReorderActivity.EXTRA_MODE, ReorderActivity.MODE_CARDS)
                },
            )
        }
    }

    private fun showThemeDialog() {
        val themes = RatioTheme.entries.map { it.key.replaceFirstChar { c -> c.uppercase() } }.toTypedArray()
        val current = RatioTheme.getCurrent(this).ordinal

        MaterialAlertDialogBuilder(this)
            .setTitle("Theme")
            .setSingleChoiceItems(themes, current) { dialog, which ->
                val selected = RatioTheme.entries[which]
                RatioTheme.setCurrent(this, selected)
                themeValue.text = selected.key.replaceFirstChar { it.uppercase() }
                dialog.dismiss()
            }
            .show()
    }

    private fun showUsageGoalDialog() {
        val options = arrayOf("1h", "2h", "3h", "4h", "5h", "6h", "8h", "No limit")
        val minutes = intArrayOf(60, 120, 180, 240, 300, 360, 480, 0)
        val currentGoal = UsageGoalsManager.getDailyGoal(this)
        val currentIndex = minutes.indexOfFirst { it == currentGoal }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle("Daily screen time goal")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                UsageGoalsManager.setDailyGoal(this, minutes[which])
                val m = minutes[which]
                usageGoalValue.text = if (m == 0) "None" else "${m / 60}h ${m % 60}m"
                dialog.dismiss()
            }
            .show()
    }

    private fun showHiddenAppsDialog() {
        val hidden = HiddenAppsManager.getHiddenPackages(this)
        if (hidden.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Hidden apps")
                .setMessage("No hidden apps. Long-press an app in the drawer to hide it.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val pm = packageManager
        val labels = hidden.mapNotNull { pkg ->
            try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
            catch (_: Exception) { null }
        }.toTypedArray()
        val packages = hidden.toList()

        MaterialAlertDialogBuilder(this)
            .setTitle("Hidden apps (tap to unhide)")
            .setItems(labels) { _, which ->
                HiddenAppsManager.unhideApp(this, packages[which])
                hiddenAppsCount.text = HiddenAppsManager.getHiddenPackages(this).size.toString()
            }
            .setNegativeButton("Close", null)
            .show()
    }



    private fun showDetoxDialog(statusView: TextView) {
        if (DetoxMode.isActive(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Digital Detox")
                .setMessage("Detox mode is active. Only essential apps are accessible.")
                .setPositiveButton("Deactivate") { _, _ ->
                    DetoxMode.deactivate(this)
                    statusView.text = getString(R.string.detox_off)
                }
                .setNegativeButton("Keep active", null)
                .show()
        } else {
            val options = arrayOf("30 minutes", "1 hour", "2 hours", "4 hours", "Until I disable it")
            val minutes = intArrayOf(30, 60, 120, 240, 0)
            MaterialAlertDialogBuilder(this)
                .setTitle("Start Digital Detox")
                .setSingleChoiceItems(options, -1) { dialog, which ->
                    DetoxMode.activate(this, minutes[which])
                    statusView.text = "Active"
                    android.widget.Toast.makeText(this, "Detox mode activated — only essentials accessible", android.widget.Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showGesturesDialog() {
        val directions = QuickLaunchGestures.Direction.entries
        val items = directions.map { dir ->
            val pkg = QuickLaunchGestures.getApp(this, dir)
            val label = if (pkg != null) {
                try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }
                catch (_: Exception) { "Not set" }
            } else "Not set"
            "${QuickLaunchGestures.getDirectionLabel(dir)}: $label"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Quick Launch Gestures")
            .setItems(items) { _, which ->
                showAppPickerForGesture(directions[which])
            }
            .setPositiveButton("Done", null)
            .show()
    }

    private fun showAppPickerForGesture(direction: QuickLaunchGestures.Direction) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
            .toList()

        val names = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("${QuickLaunchGestures.getDirectionLabel(direction)} → App")
            .setItems(names) { _, which ->
                QuickLaunchGestures.setApp(this, direction, apps[which].activityInfo.packageName)
                android.widget.Toast.makeText(this, "Set!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Clear") { _, _ ->
                QuickLaunchGestures.setApp(this, direction, null)
            }
            .show()
    }

    private fun showWallpaperDialog() {
        val options = arrayOf("Solid color (theme)", "Choose image")
        MaterialAlertDialogBuilder(this)
            .setTitle("Wallpaper")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        com.ratio.launcher.utils.WallpaperManager.setMode(this, com.ratio.launcher.utils.WallpaperManager.WallpaperMode.SOLID_COLOR)
                        android.widget.Toast.makeText(this, "Using theme color", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        com.ratio.launcher.utils.WallpaperManager.setMode(this, com.ratio.launcher.utils.WallpaperManager.WallpaperMode.IMAGE)
                        wallpaperImagePicker.launch(arrayOf("image/*"))
                    }
                }
            }
            .show()
    }

    private val wallpaperImagePicker = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            com.ratio.launcher.utils.WallpaperManager.setImageUri(this, uri)
            android.widget.Toast.makeText(this, "Wallpaper set", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAccentColorDialog(preview: android.view.View) {
        val colors = intArrayOf(
            "#FFFC33".toColorInt(),
            "#4ECDC4".toColorInt(),
            "#FF6B9D".toColorInt(),
            "#7C4DFF".toColorInt(),
            "#00E676".toColorInt(),
            "#FF5722".toColorInt(),
            "#03A9F4".toColorInt(),
            "#F2F2F2".toColorInt(),
        )
        val names = arrayOf("Yellow", "Teal", "Pink", "Purple", "Green", "Orange", "Blue", "White")

        MaterialAlertDialogBuilder(this)
            .setTitle("Accent color")
            .setItems(names) { _, which ->
                com.ratio.launcher.utils.WallpaperManager.setAccentColor(this, colors[which])
                preview.setBackgroundColor(colors[which])
                android.widget.Toast.makeText(this, "Accent color updated", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showFeedbackDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }

        val nameInput = android.widget.EditText(this).apply {
            hint = "Name (optional)"
            setTextColor(resources.getColor(R.color.ratio_white, null))
            setHintTextColor(resources.getColor(R.color.ratio_gray_light, null))
            setSingleLine()
        }

        val emailInput = android.widget.EditText(this).apply {
            hint = "Email (optional)"
            setTextColor(resources.getColor(R.color.ratio_white, null))
            setHintTextColor(resources.getColor(R.color.ratio_gray_light, null))
            setSingleLine()
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val commentInput = android.widget.EditText(this).apply {
            hint = "What happened?"
            setTextColor(resources.getColor(R.color.ratio_white, null))
            setHintTextColor(resources.getColor(R.color.ratio_gray_light, null))
            minLines = 3
            gravity = android.view.Gravity.TOP
        }

        layout.addView(nameInput)
        layout.addView(emailInput)
        layout.addView(commentInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Send Feedback")
            .setView(layout)
            .setPositiveButton("Send") { _, _ ->
                val feedback = io.sentry.protocol.Feedback(commentInput.text.toString()).apply {
                    contactEmail = emailInput.text.toString()
                    name = nameInput.text.toString()
                }
                io.sentry.Sentry.feedback().capture(feedback)
                android.widget.Toast.makeText(this, "Feedback sent!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showIconPackDialog() {
        val packs = IconPackManager.getInstalledIconPacks(this)
        val names = mutableListOf("Default (System)")
        names.addAll(packs.map { it.label })

        val currentPack = IconPackManager.getCurrentIconPack(this)
        val currentIndex = if (currentPack == null) 0
            else packs.indexOfFirst { it.packageName == currentPack } + 1

        MaterialAlertDialogBuilder(this)
            .setTitle("Icon pack")
            .setSingleChoiceItems(names.toTypedArray(), currentIndex.coerceAtLeast(0)) { dialog, which ->
                if (which == 0) {
                    IconPackManager.setIconPack(this, null)
                    iconPackValue.text = "Default"
                } else {
                    val pack = packs[which - 1]
                    IconPackManager.setIconPack(this, pack.packageName)
                    iconPackValue.text = pack.label
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showBackupDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Backup & Restore")
            .setItems(arrayOf("Export configuration", "Import configuration")) { _, which ->
                when (which) {
                    0 -> exportConfig()
                    1 -> importConfig()
                }
            }
            .show()
    }

    private val exportLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val success = BackupManager.exportToUri(this, uri)
            android.widget.Toast.makeText(this,
                if (success) "Backup exported" else "Export failed",
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = BackupManager.importFromUri(this, uri)
            android.widget.Toast.makeText(this,
                if (success) "Configuration restored" else "Import failed",
                android.widget.Toast.LENGTH_SHORT).show()
            if (success) loadSettings()
        }
    }

    private fun exportConfig() {
        exportLauncher.launch("ratio_backup.json")
    }

    private fun importConfig() {
        importLauncher.launch(arrayOf("application/json"))
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.settingClockStyleValue)?.text = ClockStyle.getCurrent(this).displayName
        try {
            val version = packageManager.getPackageInfo(packageName, 0).versionName
            findViewById<TextView>(R.id.settingsVersion)?.text = getString(R.string.version_format, version)
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        val city = settingsCity.text.toString().trim()
        if (city.isNotEmpty()) {
            WeatherHelper.setCity(this, city)
        }
    }

}
