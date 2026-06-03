package com.ratio.launcher.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ratio.launcher.fragments.RootFragment
import com.ratio.launcher.fragments.TilesFragment
import com.ratio.launcher.fragments.TreeFragment

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TreeFragment()
            1 -> RootFragment()
            2 -> TilesFragment()
            else -> RootFragment()
        }
    }
}
