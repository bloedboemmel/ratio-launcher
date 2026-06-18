package com.ratio.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
        setupBackNavigation()
    }

    private fun setupImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
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
            page.alpha = 1f - (kotlin.math.abs(position) * 0.3f)
        }

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val pageName = when (position) { 0 -> "tree"; 1 -> "root"; else -> "tiles" }
                    Sentry.metrics().count("page_swipe_$pageName")

                    // Block swiping to apps page when detox mode is active
                    if ((position == 2) && com.ratio.launcher.utils.DetoxMode.isActive(this@MainActivity)) {
                        viewPager.setCurrentItem(1, true)
                        return
                    }

                    if (position == 2) {
                        supportFragmentManager.fragments
                            .asSequence()
                            .filterIsInstance<com.ratio.launcher.fragments.TilesFragment>()
                            .firstOrNull()?.openKeyboard()
                    } else {
                        hideKeyboard()
                    }
                }
            },
        )
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem != 1) {
                    viewPager.setCurrentItem(1, true)
                }
                // Intentionally not finishing — launcher should not exit on back
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
        val rootLayout = findViewById<View>(R.id.viewPager)?.parent as? android.widget.FrameLayout

        // Apply wallpaper
        val wpManager = com.ratio.launcher.utils.WallpaperManager
        var hasWallpaper = false
        when (wpManager.getMode(this)) {
            com.ratio.launcher.utils.WallpaperManager.WallpaperMode.IMAGE -> {
                val uri = wpManager.getImageUri(this)
                if (uri != null) {
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(contentResolver, uri)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        }
                        if (bitmap != null) {
                            rootLayout?.background = bitmap.toDrawable(resources)
                            hasWallpaper = true
                        } else {
                            rootLayout?.setBackgroundColor(theme.backgroundColor)
                        }
                    } catch (_: Exception) {
                        rootLayout?.setBackgroundColor(theme.backgroundColor)
                    }
                } else {
                    rootLayout?.setBackgroundColor(theme.backgroundColor)
                }
            }
            else -> rootLayout?.setBackgroundColor(theme.backgroundColor)
        }

        // Make ViewPager2 and its internal RecyclerView background transparent so wallpaper shows through
        viewPager.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        (viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)
            ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        // Make fragment backgrounds transparent when wallpaper is active
        val fragmentBgColor = if (hasWallpaper) android.graphics.Color.TRANSPARENT else theme.backgroundColor
        supportFragmentManager.fragments.forEach { fragment ->
            fragment.view?.setBackgroundColor(fragmentBgColor)
        }



        val prefs = getSharedPreferences("ratio_prefs", MODE_PRIVATE)
        statusBarHidden = prefs.getBoolean("hide_status_bar", false)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (statusBarHidden) {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
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
        statusBattery.text = getString(R.string.battery_percentage, level)

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
}
