package com.ratio.launcher

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
        override fun getItemCount(): Int = 4
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
        val page = arguments?.getInt("page") ?: 0

        val title = view.findViewById<TextView>(R.id.onboardingTitle)
        val desc = view.findViewById<TextView>(R.id.onboardingDesc)
        val button = view.findViewById<TextView>(R.id.onboardingButton)

        when (page) {
            0 -> {
                title.text = "Welcome to Ratio"
                desc.text = "A mindful launcher that helps you\nfocus on what matters"
                button.text = "GET STARTED"
                button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
            }
            1 -> {
                title.text = "Notifications"
                desc.text = "Grant notification access to see\nyour messages in Tree view"
                button.text = "GRANT ACCESS"
                button.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                view.postDelayed({
                    button.text = "CONTINUE"
                    button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
                }, 2000)
            }
            2 -> {
                title.text = "Usage Stats"
                desc.text = "Allow usage access to track\nyour screen time"
                button.text = "GRANT ACCESS"
                button.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                view.postDelayed({
                    button.text = "CONTINUE"
                    button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
                }, 2000)
            }
            3 -> {
                title.text = "You're all set"
                desc.text = "Swipe between Tree, Root, and Tiles\nLong-press the clock for settings"
                button.text = "START USING RATIO"
                button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val page = arguments?.getInt("page") ?: 0
        val button = view?.findViewById<TextView>(R.id.onboardingButton) ?: return

        if (page == 1 || page == 2) {
            button.text = "CONTINUE"
            button.setOnClickListener { (activity as OnboardingActivity).nextPage() }
        }
    }
}
