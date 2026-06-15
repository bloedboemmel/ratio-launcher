package com.ratio.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.ratio.launcher.adapters.ViewPagerAdapter
import io.sentry.Sentry

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var statusOverlay: View
    private lateinit var statusWifi: TextView
    private lateinit var statusBattery: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var statusBarHidden = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateStatusInfo()
        }
    }

    private val statusUpdater = object : Runnable {
        override fun run() {
            updateStatusInfo()
            handler.postDelayed(this, 30000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!OnboardingActivity.isOnboardingComplete(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        val clockStyle = com.ratio.launcher.utils.ClockStyle.getCurrent(this).name
        val theme = com.ratio.launcher.utils.RatioTheme.getCurrent(this).key
        Sentry.metrics().count("app_launch")
        Sentry.metrics().count("clock_style_$clockStyle")
        Sentry.metrics().count("theme_$theme")

        statusOverlay = findViewById(R.id.statusOverlay)
        statusWifi = findViewById(R.id.statusWifi)
        statusBattery = findViewById(R.id.statusBattery)

        setupImmersive()
        setupViewPager()
    }

    private fun setupImmersive() {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = ContextCompat.getColor(this, R.color.ratio_black)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    private fun setupViewPager() {
        viewPager = findViewById(R.id.viewPager)
        if (viewPager.adapter == null) {
            val adapter = ViewPagerAdapter(this)
            viewPager.adapter = adapter
            viewPager.setCurrentItem(1, false)
        }
        viewPager.offscreenPageLimit = 2

        viewPager.setPageTransformer { page, position ->
            page.alpha = 1f - kotlin.math.abs(position) * 0.3f
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageName = when (position) { 0 -> "tree"; 1 -> "root"; else -> "tiles" }
                Sentry.metrics().count("page_swipe_$pageName")

                if (position == 2) {
                    supportFragmentManager.fragments
                        .filterIsInstance<com.ratio.launcher.fragments.TilesFragment>()
                        .firstOrNull()?.openKeyboard()
                } else {
                    hideKeyboard()
                }
            }
        })
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
            ?: imm.hideSoftInputFromWindow(viewPager.windowToken, 0)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        // Don't do anything special — just prevent crash
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        handler.post(statusUpdater)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        handler.removeCallbacks(statusUpdater)
    }

    private fun applyTheme() {
        val theme = com.ratio.launcher.utils.RatioTheme.getCurrent(this)
        findViewById<View>(android.R.id.content).setBackgroundColor(theme.backgroundColor)
        window.navigationBarColor = theme.backgroundColor

        val prefs = getSharedPreferences("ratio_prefs", MODE_PRIVATE)
        statusBarHidden = prefs.getBoolean("hide_status_bar", false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (statusBarHidden) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
                window.insetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                window.insetsController?.show(WindowInsets.Type.statusBars())
            }
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        } else {
            @Suppress("DEPRECATION")
            if (statusBarHidden) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                    View.SYSTEM_UI_FLAG_FULLSCREEN.inv() and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()
            }
        }

        // Show/hide custom status overlay
        statusOverlay.visibility = if (statusBarHidden) View.VISIBLE else View.GONE
        if (statusBarHidden) updateStatusInfo()
    }

    private fun updateStatusInfo() {
        if (!statusBarHidden) return

        // Battery
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        statusBattery.text = "$level%"

        // WiFi/connectivity
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        statusWifi.text = when {
            caps == null -> ""
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE"
            else -> ""
        }
    }

    @Suppress("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewPager.currentItem != 1) {
            viewPager.setCurrentItem(1, true)
        }
        // Intentionally not calling super — launcher should not exit on back
    }
}
