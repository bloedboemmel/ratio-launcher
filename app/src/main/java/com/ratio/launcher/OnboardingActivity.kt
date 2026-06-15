package com.ratio.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.ratio.launcher.services.NotificationService
import com.ratio.launcher.utils.UsageStatsHelper

class OnboardingActivity : AppCompatActivity() {

    private lateinit var pager: ViewPager2

    companion object {
        private const val PREFS = "ratio_prefs"
        private const val KEY_ONBOARDED = "onboarding_complete"

        fun isOnboardingComplete(context: Context): Boolean {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDED, false)
        }

        fun markComplete(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ONBOARDED, true).apply()
        }

        fun resetOnboarding(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ONBOARDED, false).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pager = findViewById(R.id.onboardingPager)
        pager.adapter = OnboardingAdapter(this)
        pager.isUserInputEnabled = false
    }

    fun nextPage() {
        if (pager.currentItem < (pager.adapter?.itemCount ?: 0) - 1) {
            pager.setCurrentItem(pager.currentItem + 1, true)
        } else {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        markComplete(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    class OnboardingAdapter(activity: OnboardingActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 5
        override fun createFragment(position: Int): Fragment = OnboardingPageFragment.newInstance(position)
    }
}

class OnboardingPageFragment : Fragment() {

    companion object {
        fun newInstance(page: Int): OnboardingPageFragment {
            return OnboardingPageFragment().apply {
                arguments = Bundle().apply { putInt("page", page) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPage()
    }

    override fun onResume() {
        super.onResume()
        setupPage()
    }

    private fun setupPage() {
        val page = arguments?.getInt("page") ?: 0
        val title = view?.findViewById<TextView>(R.id.onboardingTitle) ?: return
        val desc = view?.findViewById<TextView>(R.id.onboardingDesc) ?: return
        val button = view?.findViewById<TextView>(R.id.onboardingButton) ?: return

        when (page) {
            0 -> {
                title.text = "Welcome to Ratio"
                desc.text = "A mindful launcher that helps you\nfocus on what matters.\n\nSwipe between three pages:\n← Tree (notifications)\n● Root (home)\n→ Tiles (apps)"
                button.text = "GET STARTED"
                button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
            }
            1 -> {
                title.text = "Default Launcher"
                desc.text = "Set Ratio as your default home app\nso it opens when you press Home"
                if (isDefaultLauncher()) {
                    button.text = "ALREADY SET ✓"
                    button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
                } else {
                    button.text = "SET AS DEFAULT"
                    button.setOnClickListener {
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                        startActivity(intent)
                    }
                }
            }
            2 -> {
                title.text = "Notifications"
                desc.text = "Grant notification access to see\nyour messages in Tree view.\n\nFind \"Ratio\" in the list and enable it."
                if (isNotificationAccessGranted()) {
                    button.text = "GRANTED ✓ CONTINUE"
                    button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
                } else {
                    button.text = "GRANT ACCESS"
                    button.setOnClickListener {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
            }
            3 -> {
                title.text = "Screen Time"
                desc.text = "Allow usage access to track\nyour daily screen time.\n\nFind \"Ratio\" in the list and enable it."
                if (UsageStatsHelper.hasPermission(requireContext())) {
                    button.text = "GRANTED ✓ CONTINUE"
                    button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
                } else {
                    button.text = "GRANT ACCESS"
                    button.setOnClickListener {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                }
            }
            4 -> {
                title.text = "You're all set!"
                desc.text = "How to use Ratio:\n\n• Tap clock → Alarm\n• Long-press clock → Settings\n• Swipe left → Notifications\n• Swipe right → App drawer\n• Long-press app → Dock/Hide/Uninstall"
                button.text = "START USING RATIO"
                button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
            }
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = requireContext().packageManager.resolveActivity(intent, 0)
        return resolveInfo?.activityInfo?.packageName == requireContext().packageName
    }

    private fun isNotificationAccessGranted(): Boolean {
        val cn = ComponentName(requireContext(), NotificationService::class.java)
        val flat = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }
}
