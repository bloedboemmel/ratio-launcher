package com.ratio.launcher

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ratio.launcher.utils.*

class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(FontSizeManager.wrap(newBase))
    }

    private lateinit var settingsCity: EditText
    private lateinit var clock24hSwitch: SwitchMaterial
    private lateinit var monochromeSwitch: SwitchMaterial
    private lateinit var doubleTapSwitch: SwitchMaterial
    private lateinit var themeValue: TextView
    private lateinit var fontSizeValue: TextView
    private lateinit var usageGoalValue: TextView
    private lateinit var hiddenAppsCount: TextView
    private lateinit var tempUnitValue: TextView
    private lateinit var largeTilesSwitch: SwitchMaterial
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
        fontSizeValue = findViewById(R.id.settingFontSizeValue)
        usageGoalValue = findViewById(R.id.settingUsageGoalValue)
        hiddenAppsCount = findViewById(R.id.settingHiddenAppsCount)
        tempUnitValue = findViewById(R.id.settingTempUnit)
        largeTilesSwitch = findViewById(R.id.settingsLargeTiles)
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

        fontSizeValue.text = FontSizeManager.labelFor(FontSizeManager.getScale(this))


        val goal = UsageGoalsManager.getDailyGoal(this)
        usageGoalValue.text = getString(R.string.usage_goal_format, goal / 60, goal % 60)

        val hidden = HiddenAppsManager.getHiddenPackages(this)
        hiddenAppsCount.text = hidden.size.toString()

        tempUnitValue.text = if (WeatherHelper.isMetric(this)) "°C" else "°F"
        largeTilesSwitch.isChecked = prefs.getBoolean("large_tiles", false)

        showMusicSwitch.isChecked = prefs.getBoolean("show_music", true)
        showCalendarSwitch.isChecked = prefs.getBoolean("show_calendar", true)
        showWeatherSwitch.isChecked = prefs.getBoolean("show_weather", true)
        showNotesSwitch.isChecked = prefs.getBoolean("show_notes", true)
        val showTogglesSwitch = findViewById<SwitchMaterial>(R.id.settingsShowToggles)
        showTogglesSwitch.isChecked = prefs.getBoolean("show_toggles", true)
        showTogglesSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("show_toggles", checked).apply()
        }

        val showSuggestionsSwitch = findViewById<SwitchMaterial>(R.id.settingsShowSuggestions)
        showSuggestionsSwitch.isChecked = prefs.getBoolean("show_suggestions", true)
        showSuggestionsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("show_suggestions", checked).apply()
        }

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

        findViewById<LinearLayout>(R.id.settingFontSize).setOnClickListener {
            showFontSizeDialog()
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

        // Wallpaper
        findViewById<LinearLayout>(R.id.settingWallpaper).setOnClickListener {
            showWallpaperDialog()
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

    private fun showFontSizeDialog() {
        val current = FontSizeManager.presets.indexOfFirst { it.second == FontSizeManager.getScale(this) }
            .coerceAtLeast(0)

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_single_choice,
            FontSizeManager.presets.map { it.first },
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as CheckedTextView
                view.textSize = 16f * FontSizeManager.presets[position].second
                view.setTextColor(android.graphics.Color.WHITE)
                return view
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Font size")
            .setSingleChoiceItems(adapter, current) { dialog, which ->
                val (label, scale) = FontSizeManager.presets[which]
                FontSizeManager.setScale(this, scale)
                fontSizeValue.text = label
                dialog.dismiss()
                recreate()
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
