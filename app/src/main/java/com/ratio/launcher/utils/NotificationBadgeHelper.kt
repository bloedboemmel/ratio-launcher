package com.ratio.launcher.utils

import android.content.Context
import com.ratio.launcher.services.NotificationService

object NotificationBadgeHelper {

    fun getUnreadCount(packageName: String): Int {
        synchronized(NotificationService.conversations) {
            return NotificationService.conversations.count { it.packageName == packageName }
        }
    }

    fun getTotalUnread(): Int {
        synchronized(NotificationService.conversations) {
            return NotificationService.conversations.size
        }
    }

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
            .getBoolean("show_notification_badges", true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("ratio_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("show_notification_badges", enabled).apply()
    }
}
