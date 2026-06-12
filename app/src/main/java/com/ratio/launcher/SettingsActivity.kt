package com.ratio.launcher

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        val prefs = getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)

        settingsCity.setText(WeatherHelper.getCity(this))
        clock24hSwitch.isChecked = prefs.getBoolean("clock_24h", true)
        monochromeSwitch.isChecked = prefs.getBoolean("monochrome_icons", true)
        doubleTapSwitch.isChecked = prefs.getBoolean("double_tap_lock", false)

        val theme = RatioTheme.getCurrent(this)
        themeValue.text = theme.key.replaceFirstChar { it.uppercase() }


        val goal = UsageGoalsManager.getDailyGoal(this)
        usageGoalValue.text = "${goal / 60}h ${goal % 60}m"

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
        val prefs = getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)

        clock24hSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("clock_24h", checked).apply()
        }

        monochromeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("monochrome_icons", checked).apply()
        }

        doubleTapSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("double_tap_lock", checked).apply()
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
            prefs.edit().putBoolean("show_seconds", checked).apply()
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
            prefs.edit().putBoolean("show_music", checked).apply()
        }
        showCalendarSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("show_calendar", checked).apply()
        }
        showWeatherSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("show_weather", checked).apply()
        }
        showNotesSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("show_notes", checked).apply()
        }
        showScreenTimeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("show_screen_time", checked).apply()
        }

        hideStatusBarSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("hide_status_bar", checked).apply()
        }

        findViewById<LinearLayout>(R.id.settingIconPack).setOnClickListener {
            showIconPackDialog()
        }

        findViewById<LinearLayout>(R.id.settingBackup).setOnClickListener {
            showBackupDialog()
        }

        findViewById<LinearLayout>(R.id.settingSendFeedback).setOnClickListener {
            showFeedbackDialog()
        }

        findViewById<LinearLayout>(R.id.settingReorderCategories).setOnClickListener {
            startActivity(android.content.Intent(this, ReorderActivity::class.java).apply {
                putExtra(ReorderActivity.EXTRA_MODE, ReorderActivity.MODE_CATEGORIES)
            })
        }

        findViewById<LinearLayout>(R.id.settingReorderCards).setOnClickListener {
            startActivity(android.content.Intent(this, ReorderActivity::class.java).apply {
                putExtra(ReorderActivity.EXTRA_MODE, ReorderActivity.MODE_CARDS)
            })
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

    private fun showClockStyleDialog(valueView: TextView) {
        val styles = ClockStyle.entries
        val prefs = getSharedPreferences("ratio_prefs", MODE_PRIVATE)
        val use24h = prefs.getBoolean("clock_24h", true)
        val showSeconds = prefs.getBoolean("show_seconds", false)
        val current = ClockStyle.getCurrent(this).ordinal

        // Build preview items: "Style Name\npreview"
        val previews = styles.map { style ->
            val preview = when (style) {
                ClockStyle.ANALOG -> "⏲ Drawn clock face"
                ClockStyle.FLIP -> "Animated flip digits"
                else -> style.formatTime(use24h, showSeconds)
            }
            "${style.displayName}\n$preview"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Clock style")
            .setSingleChoiceItems(previews, current) { dialog, which ->
                val selected = styles[which]
                ClockStyle.setCurrent(this, selected)
                valueView.text = selected.displayName
                dialog.dismiss()
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
                val sentryId = io.sentry.Sentry.captureMessage("User Feedback")
                val feedback = io.sentry.UserFeedback(sentryId).apply {
                    comments = commentInput.text.toString()
                    email = emailInput.text.toString()
                    name = nameInput.text.toString()
                }
                io.sentry.Sentry.captureUserFeedback(feedback)
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
    }

    override fun onPause() {
        super.onPause()
        val city = settingsCity.text.toString().trim()
        if (city.isNotEmpty()) {
            WeatherHelper.setCity(this, city)
        }
    }

}
