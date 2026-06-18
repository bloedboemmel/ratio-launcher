package com.ratio.launcher.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.R
import com.ratio.launcher.SettingsActivity
import com.ratio.launcher.adapters.DockAdapter
import com.ratio.launcher.adapters.NotesAdapter
import com.ratio.launcher.models.AppInfo
import com.ratio.launcher.models.Note
import android.content.pm.PackageManager
import com.ratio.launcher.utils.CalendarHelper
import com.ratio.launcher.utils.ClockStyle
import com.ratio.launcher.utils.GestureHelper
import com.ratio.launcher.utils.RootCardOrder
import com.ratio.launcher.utils.MediaInfo
import com.ratio.launcher.utils.MediaPlayerHelper
import com.ratio.launcher.utils.UsageStatsHelper
import com.ratio.launcher.utils.WeatherData
import com.ratio.launcher.utils.WeatherHelper
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RootFragment : Fragment() {

    private lateinit var clockTime: TextView
    private lateinit var clockDate: TextView
    private lateinit var usageTime: TextView
    private lateinit var usageBarFill: View
    private lateinit var weatherTemp: TextView
    private lateinit var weatherCondition: TextView
    private lateinit var weatherCity: TextView
    private lateinit var weatherHumidity: TextView
    private lateinit var weatherWind: TextView
    private lateinit var weatherRain: TextView
    private lateinit var weatherTomorrowTemp: TextView
    private lateinit var weatherTomorrowCondition: TextView
    private lateinit var weatherDetailsRow: View
    private lateinit var weatherTomorrowRow: View
    private lateinit var mediaPlayerCard: View
    private lateinit var mediaTitle: TextView
    private lateinit var mediaArtist: TextView
    private lateinit var mediaPlayPause: TextView
    private lateinit var mediaPrev: TextView
    private lateinit var mediaNext: TextView
    private lateinit var dockList: RecyclerView
    private lateinit var dockAdapter: DockAdapter
    private val dockApps = mutableListOf<AppInfo>()
    private lateinit var noteInput: EditText
    private lateinit var noteAddBtn: ImageView
    private lateinit var notesList: RecyclerView

    private val handler = Handler(Looper.getMainLooper())
    private val notes = mutableListOf<Note>()
    private lateinit var notesAdapter: NotesAdapter

    private val clockUpdater = object : Runnable {
        override fun run() {
            updateClock()
            val prefs = requireContext().getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
            val showSeconds = prefs.getBoolean("show_seconds", false)
            val delay = if (showSeconds) 1000L else 30000L
            updateUsage()
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_root, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clockTime = view.findViewById(R.id.clockTime)
        clockDate = view.findViewById(R.id.clockDate)
        usageTime = view.findViewById(R.id.usageTime)
        usageBarFill = view.findViewById(R.id.usageBarFill)
        weatherTemp = view.findViewById(R.id.weatherTemp)
        weatherCondition = view.findViewById(R.id.weatherCondition)
        weatherCity = view.findViewById(R.id.weatherCity)
        weatherHumidity = view.findViewById(R.id.weatherHumidity)
        weatherWind = view.findViewById(R.id.weatherWind)
        weatherRain = view.findViewById(R.id.weatherRain)
        weatherTomorrowTemp = view.findViewById(R.id.weatherTomorrowTemp)
        weatherTomorrowCondition = view.findViewById(R.id.weatherTomorrowCondition)
        weatherDetailsRow = view.findViewById(R.id.weatherDetailsRow)
        weatherTomorrowRow = view.findViewById(R.id.weatherTomorrowRow)
        mediaPlayerCard = view.findViewById(R.id.mediaPlayerCard)
        mediaTitle = view.findViewById(R.id.mediaTitle)
        mediaArtist = view.findViewById(R.id.mediaArtist)
        mediaPlayPause = view.findViewById(R.id.mediaPlayPause)
        mediaPrev = view.findViewById(R.id.mediaPrev)
        mediaNext = view.findViewById(R.id.mediaNext)
        noteInput = view.findViewById(R.id.noteInput)
        noteAddBtn = view.findViewById(R.id.noteAddBtn)
        notesList = view.findViewById(R.id.notesList)

        loadNotes()
        notesAdapter = NotesAdapter(notes, { note -> deleteNote(note) }) { note -> openNoteEditor(note) }
        notesList.layoutManager = LinearLayoutManager(requireContext())
        notesList.adapter = notesAdapter

        noteAddBtn.setOnClickListener { addNote() }
        noteInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addNote()
                true
            } else false
        }

        usageTime.setOnClickListener {
            if (!UsageStatsHelper.hasPermission(requireContext())) {
                UsageStatsHelper.requestPermission(requireContext())
            } else {
                startActivity(Intent(requireContext(), com.ratio.launcher.UsageBreakdownActivity::class.java))
            }
        }

        setupDock(view)
        setupWeather()
        setupCalendar(view)
        setupMediaPlayer()
        setupLongPressSettings(view)

        updateClock()
        updateUsage()
        applyCardVisibility(view)
        applyCardOrder(view)
    }

    private fun applyCardOrder(view: View) {
        val order = RootCardOrder.getOrder(requireContext())

        val sectionMap = mapOf(
            "screen_time" to view.findViewById<View>(R.id.sectionScreenTime),
            "media" to view.findViewById<View>(R.id.mediaPlayerCard),
            "weather" to view.findViewById<View>(R.id.sectionWeather),
            "calendar" to view.findViewById<View>(R.id.calendarSection),
            "notes" to view.findViewById<View>(R.id.sectionNotes)
        )

        // Find the parent LinearLayout (inside the ScrollView)
        val parent = sectionMap.values.firstOrNull()?.parent as? ViewGroup ?: return

        // Remove all reorderable sections
        val sections = mutableListOf<View>()
        for (key in order) {
            val section = sectionMap[key] ?: continue
            sections.add(section)
            parent.removeView(section)
        }

        // Find where to insert (after the clock section, which is always first)
        // The clock is the first child of the LinearLayout
        var insertIndex = 1 // after clock
        for (section in sections) {
            parent.addView(section, insertIndex)
            insertIndex++
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun applyCardVisibility(view: View) {
        val prefs = requireContext().getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)

        val showScreenTime = prefs.getBoolean("show_screen_time", true)
        val showWeather = prefs.getBoolean("show_weather", true)
        val showNotes = prefs.getBoolean("show_notes", true)
        val showMusic = prefs.getBoolean("show_music", true)

        // Usage section is the parent of usageTime
        usageTime.parent?.let { parent ->
            (parent as? View)?.visibility = if (showScreenTime) View.VISIBLE else View.GONE
        }

        // Weather section
        weatherTemp.parent?.parent?.let { parent ->
            (parent as? View)?.visibility = if (showWeather) View.VISIBLE else View.GONE
        }

        // Notes section
        notesList.parent?.let { parent ->
            (parent as? View)?.visibility = if (showNotes) View.VISIBLE else View.GONE
        }

        // Music - handled by updateMediaUI (only shows when playing anyway)
        if (!showMusic) {
            mediaPlayerCard.visibility = View.GONE
        }
    }

    private fun applyWallpaperCardStyle(view: View) {
        val hasWallpaper = com.ratio.launcher.utils.WallpaperManager.hasWallpaperImage(requireContext())
        val cardBg = if (hasWallpaper) R.drawable.card_background_wallpaper else 0
        val dockBg = if (hasWallpaper) R.drawable.dock_background_wallpaper else R.drawable.dock_background
        val noteInputBg = if (hasWallpaper) R.drawable.note_input_bg_wallpaper else R.drawable.note_input_bg

        val sections = listOf(
            view.findViewById<View>(R.id.sectionScreenTime),
            view.findViewById<View>(R.id.mediaPlayerCard),
            view.findViewById<View>(R.id.sectionWeather),
            view.findViewById<View>(R.id.calendarSection),
            view.findViewById<View>(R.id.sectionNotes)
        )
        for (section in sections) {
            if (hasWallpaper) {
                section?.setBackgroundResource(cardBg)
            } else {
                section?.background = null
            }
        }

        view.findViewById<View>(R.id.dockContainer)?.setBackgroundResource(dockBg)
        noteInput.setBackgroundResource(noteInputBg)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockUpdater)
    }

    private fun updateClock() {
        val prefs = requireContext().getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
        val use24h = prefs.getBoolean("clock_24h", true)
        val style = ClockStyle.getCurrent(requireContext())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val now = Date()

        val showSeconds = prefs.getBoolean("show_seconds", false)
        clockTime.text = style.formatTime(use24h, showSeconds)
        clockDate.text = dateFormat.format(now)

        // Show/hide special clock views
        val analogClock = view?.findViewById<View>(R.id.analogClock)
        val flipClock = view?.findViewById<com.ratio.launcher.views.FlipClockView>(R.id.flipClock)

        when (style) {
            ClockStyle.ANALOG -> {
                analogClock?.visibility = View.VISIBLE
                (analogClock as? com.ratio.launcher.views.AnalogClockView)?.showSeconds = showSeconds
                flipClock?.visibility = View.GONE
                clockTime.visibility = View.GONE
            }
            ClockStyle.FLIP -> {
                analogClock?.visibility = View.GONE
                flipClock?.visibility = View.VISIBLE
                flipClock?.setUse24h(use24h)
                flipClock?.setShowSeconds(showSeconds)
                clockTime.visibility = View.GONE
            }
            else -> {
                analogClock?.visibility = View.GONE
                flipClock?.visibility = View.GONE
                clockTime.visibility = View.VISIBLE
            }
        }

        // Adjust text size based on style
        clockTime.typeface = resources.getFont(R.font.ratio_light)
        when (style) {
            ClockStyle.MINIMAL -> {
                clockTime.textSize = 64f
                clockTime.letterSpacing = -0.02f
            }
            ClockStyle.BOLD -> {
                clockTime.textSize = 72f
                clockTime.letterSpacing = -0.04f
                clockTime.typeface = resources.getFont(R.font.inter_semibold)
            }
            ClockStyle.FLIP -> {
                clockTime.textSize = 80f
                clockTime.letterSpacing = 0.02f
                clockTime.typeface = resources.getFont(R.font.inter_semibold)
            }
            ClockStyle.WORD -> {
                clockTime.textSize = 28f
                clockTime.letterSpacing = 0.02f
                clockTime.typeface = resources.getFont(R.font.inter_light)
            }
            ClockStyle.BINARY -> {
                clockTime.textSize = 24f
                clockTime.letterSpacing = 0.15f
                clockTime.typeface = android.graphics.Typeface.MONOSPACE
            }
            ClockStyle.BAR -> {
                clockTime.textSize = 14f
                clockTime.letterSpacing = 0f
                clockTime.typeface = android.graphics.Typeface.MONOSPACE
            }
            ClockStyle.ANALOG -> {
                // Text hidden, analog view shown instead
            }
        }
    }

    private fun updateUsage() {
        val context = context ?: return
        val minutes = UsageStatsHelper.getTodayUsageMinutes(context)
        if (minutes < 0) {
            usageTime.text = "Tap to enable"
        } else {
            val hours = minutes / 60
            val mins = minutes % 60
            usageTime.text = "${hours}h ${mins}m"

            val goalMinutes = 240
            val ratio = (minutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
            val params = usageBarFill.layoutParams as android.widget.LinearLayout.LayoutParams
            params.weight = ratio
            usageBarFill.layoutParams = params
        }
    }

    private fun addNote() {
        val text = noteInput.text.toString().trim()
        if (text.isNotEmpty()) {
            val note = Note(id = System.currentTimeMillis(), text = text)
            notes.add(0, note)
            notesAdapter.notifyItemInserted(0)
            noteInput.text.clear()
            saveNotes()
            io.sentry.Sentry.metrics().count("action_note_added")
        }
    }

    private val noteEditorLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        val noteId = data.getLongExtra(com.ratio.launcher.NoteEditorActivity.EXTRA_NOTE_ID, -1)
        if (noteId == -1L) return@registerForActivityResult

        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val newText = data.getStringExtra(com.ratio.launcher.NoteEditorActivity.EXTRA_NOTE_TEXT) ?: return@registerForActivityResult
                val index = notes.indexOfFirst { it.id == noteId }
                if (index >= 0) {
                    notes[index] = notes[index].copy(text = newText)
                    notesAdapter.notifyItemChanged(index)
                    saveNotes()
                }
            }
            com.ratio.launcher.NoteEditorActivity.RESULT_DELETED -> {
                val index = notes.indexOfFirst { it.id == noteId }
                if (index >= 0) {
                    notes.removeAt(index)
                    notesAdapter.notifyItemRemoved(index)
                    saveNotes()
                }
            }
        }
    }

    private fun openNoteEditor(note: Note) {
        val intent = Intent(requireContext(), com.ratio.launcher.NoteEditorActivity::class.java).apply {
            putExtra(com.ratio.launcher.NoteEditorActivity.EXTRA_NOTE_ID, note.id)
            putExtra(com.ratio.launcher.NoteEditorActivity.EXTRA_NOTE_TEXT, note.text)
        }
        noteEditorLauncher.launch(intent)
    }

    private fun deleteNote(note: Note) {
        val index = notes.indexOf(note)
        if (index >= 0) {
            notes.removeAt(index)
            notesAdapter.notifyItemRemoved(index)
            saveNotes()
        }
    }

    private fun saveNotes() {
        val prefs = requireContext().getSharedPreferences("ratio_notes", Context.MODE_PRIVATE)
        val serialized = notes.joinToString("\n") { "${it.id}|${it.text}" }
        prefs.edit().putString("notes", serialized).apply()
    }

    private fun loadNotes() {
        val prefs = requireContext().getSharedPreferences("ratio_notes", Context.MODE_PRIVATE)
        val serialized = prefs.getString("notes", "") ?: ""
        if (serialized.isNotEmpty()) {
            serialized.split("\n").forEach { line ->
                val parts = line.split("|", limit = 2)
                if (parts.size == 2) {
                    notes.add(Note(id = parts[0].toLongOrNull() ?: 0, text = parts[1]))
                }
            }
        }
    }

    private fun setupDock(view: View) {
        dockList = view.findViewById(R.id.dockList)
        loadDockApps()
        dockAdapter = DockAdapter(dockApps,
            onAppClick = { app ->
                val intent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
                intent?.let { startActivity(it) }
            },
            onAppLongClick = { _, position ->
                if (position in dockApps.indices) {
                    val app = dockApps[position]
                    dockApps.removeAt(position)
                    dockAdapter.notifyItemRemoved(position)
                    saveDockApps()
                    Toast.makeText(requireContext(), "${app.name} removed", Toast.LENGTH_SHORT).show()
                }
                true
            }
        )
        dockList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        dockList.adapter = dockAdapter

        if (dockApps.isEmpty()) {
            autoPopulateDock()
        }
    }

    private fun loadDockApps() {
        val prefs = requireContext().getSharedPreferences("ratio_dock", Context.MODE_PRIVATE)
        val packages = prefs.getString("dock_packages", null)?.split(",") ?: return
        val pm = requireContext().packageManager
        packages.filter { it.isNotBlank() }.forEach { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(pkg)
                dockApps.add(AppInfo(name = label, packageName = pkg, icon = icon, category = ""))
            } catch (_: PackageManager.NameNotFoundException) {}
        }
    }

    private fun saveDockApps() {
        val prefs = requireContext().getSharedPreferences("ratio_dock", Context.MODE_PRIVATE)
        prefs.edit().putString("dock_packages", dockApps.joinToString(",") { it.packageName }).apply()
    }

    private fun autoPopulateDock() {
        val commonApps = listOf("com.android.chrome", "com.google.android.apps.messaging",
            "com.google.android.dialer", "com.google.android.apps.photos")
        val pm = requireContext().packageManager
        commonApps.forEach { pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                dockApps.add(AppInfo(pm.getApplicationLabel(appInfo).toString(), pkg, pm.getApplicationIcon(pkg), ""))
            } catch (_: PackageManager.NameNotFoundException) {}
        }
        dockAdapter.notifyDataSetChanged()
        saveDockApps()
    }

    private fun setupWeather() {
        val cached = WeatherHelper.getCachedWeather(requireContext())
        if (cached != null) {
            displayWeather(cached)
        }

        val city = WeatherHelper.getCity(requireContext())
        if (city.isNotBlank()) {
            WeatherHelper.fetchWeather(requireContext(), city) { data ->
                if (data != null) displayWeather(data)
            }
        }

        weatherTemp.setOnClickListener { showCityDialog() }
        weatherCondition.setOnClickListener { showCityDialog() }
    }

    private fun displayWeather(data: WeatherData) {
        val metric = WeatherHelper.isMetric(requireContext())
        weatherTemp.text = WeatherHelper.formatTemp(data, metric)
        weatherCondition.text = data.condition
        weatherCity.text = data.city

        weatherHumidity.text = "${data.humidity}%"
        weatherWind.text = WeatherHelper.formatWind(data, metric)
        weatherRain.text = "${data.rainChance}%"
        weatherDetailsRow.visibility = View.VISIBLE

        weatherTomorrowTemp.text = WeatherHelper.formatTomorrowTemp(data, metric)
        weatherTomorrowCondition.text = data.tomorrowCondition
        weatherTomorrowRow.visibility = View.VISIBLE
    }

    private fun showCityDialog() {
        val context = context ?: return
        val input = EditText(context).apply {
            hint = "Enter city name"
            setText(WeatherHelper.getCity(context))
            setTextColor(resources.getColor(R.color.ratio_white, null))
            setHintTextColor(resources.getColor(R.color.ratio_gray_light, null))
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Set Location")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val city = input.text.toString().trim()
                if (city.isNotEmpty()) {
                    WeatherHelper.setCity(context, city)
                    weatherCondition.text = "Loading..."
                    WeatherHelper.fetchWeather(context, city) { data ->
                        if (data != null) displayWeather(data)
                        else weatherCondition.text = "Error loading weather"
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupLongPressSettings(view: View) {
        clockTime.setOnClickListener { openAlarmApp() }
        clockDate.setOnClickListener { openAlarmApp() }

        // Also set tap on custom clock views (analog/flip)
        view.findViewById<View>(R.id.analogClock)?.setOnClickListener { openAlarmApp() }
        view.findViewById<View>(R.id.flipClock)?.setOnClickListener { openAlarmApp() }
        view.findViewById<View>(R.id.analogClock)?.setOnLongClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
            true
        }
        view.findViewById<View>(R.id.flipClock)?.setOnLongClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
            true
        }

        clockTime.setOnLongClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
            true
        }
        clockDate.setOnLongClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
            true
        }

        val prefs = requireContext().getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("settings_hint_shown", false)) {
            Toast.makeText(requireContext(), "Long-press clock for settings", Toast.LENGTH_SHORT).show()
            prefs.edit().putBoolean("settings_hint_shown", true).apply()
        }

        setupDoubleTapToLock(view)
    }

    private fun setupDoubleTapToLock(view: View) {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val prefs = requireContext().getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("double_tap_lock", false)) {
                    GestureHelper.lockScreen(requireContext())
                    return true
                }
                return false
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e1.y < 200 && velocityY > 500) {
                    GestureHelper.expandNotificationShade(requireContext())
                    return true
                }
                return false
            }
        })

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockUpdater)
        updateClock()
        updateUsage()
        updateWeatherIfNeeded()
        refreshMedia()
        refreshDock()
        view?.let {
            applyCardVisibility(it)
            applyCardOrder(it)
            applyWallpaperCardStyle(it)
        }
    }

    private fun refreshDock() {
        val prefs = requireContext().getSharedPreferences("ratio_dock", Context.MODE_PRIVATE)
        val packages = prefs.getString("dock_packages", null)?.split(",")?.filter { it.isNotBlank() } ?: return
        val pm = requireContext().packageManager

        val currentPackages = dockApps.map { it.packageName }.toSet()
        val savedPackages = packages.toSet()

        if (currentPackages != savedPackages) {
            dockApps.clear()
            packages.forEach { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(pkg)
                    dockApps.add(AppInfo(name = label, packageName = pkg, icon = icon, category = ""))
                } catch (_: PackageManager.NameNotFoundException) {}
            }
            dockAdapter.notifyDataSetChanged()
        }
    }

    private fun refreshMedia() {
        // Re-register listener
        MediaPlayerHelper.listener = object : MediaPlayerHelper.OnMediaChangedListener {
            override fun onMediaChanged(media: MediaInfo?) {
                handler.post { updateMediaUI(media) }
            }
        }
        // Refresh from active session
        try {
            val msm = requireContext().getSystemService(android.content.Context.MEDIA_SESSION_SERVICE)
                    as android.media.session.MediaSessionManager
            val cn = android.content.ComponentName(requireContext(), com.ratio.launcher.services.NotificationService::class.java)
            val controllers = msm.getActiveSessions(cn)
            if (controllers.isNotEmpty()) {
                MediaPlayerHelper.updateFromController(controllers[0])
            }
        } catch (_: Exception) {}
        updateMediaUI(MediaPlayerHelper.getCurrentMedia())
    }

    private fun setupCalendar(view: View) {
        val prefs = requireContext().getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("show_calendar", true)) return

        val calendarSection = view.findViewById<View>(R.id.calendarSection)
        val calendarDivider = view.findViewById<View>(R.id.calendarDivider)
        val calendarEventsList = view.findViewById<LinearLayout>(R.id.calendarEventsList)
        val calendarEmpty = view.findViewById<TextView>(R.id.calendarEmpty)

        if (!CalendarHelper.hasPermission(requireContext())) {
            calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
            calendarSection.visibility = View.GONE
            calendarDivider.visibility = View.GONE
            return
        }

        loadCalendarEvents(calendarSection, calendarDivider, calendarEventsList, calendarEmpty)
    }

    private val calendarPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            view?.let { setupCalendar(it) }
        }
    }

    private fun loadCalendarEvents(section: View, divider: View, list: LinearLayout, empty: TextView) {
        val events = CalendarHelper.getTodayEvents(requireContext())
        section.visibility = View.VISIBLE
        divider.visibility = View.VISIBLE

        list.removeAllViews()

        if (events.isEmpty()) {
            empty.visibility = View.VISIBLE
        } else {
            empty.visibility = View.GONE
            events.take(5).forEach { event ->
                val eventView = TextView(requireContext()).apply {
                    text = "${event.formatTime()}  ${event.title}"
                    setTextColor(resources.getColor(R.color.ratio_white, null))
                    textSize = 13f
                    typeface = resources.getFont(R.font.inter_light)
                    setPadding(0, 24, 0, 24)
                    setOnClickListener { openCalendarApp() }
                }
                list.addView(eventView)
            }
        }
    }


    private fun openAlarmApp() {
        io.sentry.Sentry.metrics().count("action_tap_clock")
        try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage("com.google.android.deskclock")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private fun openCalendarApp() {
        io.sentry.Sentry.metrics().count("action_tap_calendar")
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private fun setupMediaPlayer() {
        mediaPlayPause.setOnClickListener { MediaPlayerHelper.playPause(requireContext()) }
        mediaNext.setOnClickListener { MediaPlayerHelper.skipNext(requireContext()) }
        mediaPrev.setOnClickListener { MediaPlayerHelper.skipPrevious(requireContext()) }
    }

    private fun updateMediaUI(media: MediaInfo?) {
        val showMusic = requireContext().getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
            .getBoolean("show_music", true)

        if (media != null && showMusic) {
            mediaPlayerCard.visibility = View.VISIBLE
            mediaTitle.text = media.title
            mediaArtist.text = media.artist
            mediaPlayPause.text = if (media.isPlaying) "⏸" else "▶"
        } else {
            mediaPlayerCard.visibility = View.GONE
        }
    }

    private fun updateWeatherIfNeeded() {
        val context = context ?: return
        val city = WeatherHelper.getCity(context)
        if (city.isNotBlank()) {
            WeatherHelper.fetchWeather(context, city) { data ->
                if (data != null) displayWeather(data)
            }
        }
    }
}
