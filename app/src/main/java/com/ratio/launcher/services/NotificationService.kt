package com.ratio.launcher.services

import android.app.Notification
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ratio.launcher.utils.FocusHoursManager
import com.ratio.launcher.utils.MediaPlayerHelper

class NotificationService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onListenerConnected() {
        super.onListenerConnected()
        handler.post {
            try {
                val active = activeNotifications ?: return@post
                for (sbn in active) {
                    processNotification(sbn)
                }
            } catch (_: Exception) {}
        }

        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(this, NotificationService::class.java)
            val controllers = msm.getActiveSessions(cn)
            if (controllers.isNotEmpty()) {
                MediaPlayerHelper.updateFromController(controllers[0])
            }
        } catch (_: Exception) {}
    }

    companion object {
        val conversations = mutableListOf<ConversationEntry>()
        var listener: OnNotificationChangedListener? = null

        // Packages to EXCLUDE (system noise)
        private val EXCLUDED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.providers.downloads"
        )

        private val MEDIA_CATEGORIES = setOf(
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SERVICE
        )

        fun shouldShowNotification(sbn: StatusBarNotification): Boolean {
            val pkg = sbn.packageName
            val notification = sbn.notification
            val category = notification.category

            // Exclude system/media
            if (EXCLUDED_PACKAGES.contains(pkg)) return false
            if (MEDIA_CATEGORIES.contains(category)) return false

            // Must have title text
            val extras = notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            if (title.isNullOrBlank()) return false

            // Skip ongoing/foreground service notifications (like "app is running")
            if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0
                && category != Notification.CATEGORY_MESSAGE
                && category != Notification.CATEGORY_EMAIL
                && category != Notification.CATEGORY_SOCIAL) return false

            // Skip group summaries
            if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false

            return true
        }
    }

    data class ConversationEntry(
        val senderName: String,
        val packageName: String,
        val lastMessage: String,
        val timestamp: Long,
        val key: String
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        processNotification(sbn)
    }

    private fun processNotification(sbn: StatusBarNotification) {
        try {
            if (sbn.packageName == packageName) return

            // Try media first
            if (MediaPlayerHelper.processNotification(sbn)) return

            // Show all meaningful notifications in Tree
            if (!shouldShowNotification(sbn)) return

            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                ?: return
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: ""

            val entry = ConversationEntry(
                senderName = title,
                packageName = sbn.packageName,
                lastMessage = text,
                timestamp = sbn.postTime,
                key = sbn.key
            )

            synchronized(conversations) {
                val existingIndex = conversations.indexOfFirst {
                    it.senderName == title && it.packageName == sbn.packageName
                }
                if (existingIndex >= 0) {
                    conversations[existingIndex] = entry
                } else {
                    conversations.add(0, entry)
                }
                if (conversations.size > 100) {
                    conversations.removeAt(conversations.lastIndex)
                }
            }

            listener?.onNotificationsChanged()

            // Auto-reply if in focus mode
            tryAutoReply(sbn)
        } catch (_: Exception) {}
    }

    private fun tryAutoReply(sbn: StatusBarNotification) {
        try {
            val autoReply = FocusHoursManager.getActiveAutoReply(applicationContext) ?: return
            val category = sbn.notification.category
            if (category != Notification.CATEGORY_MESSAGE) return

            val actions = sbn.notification.actions ?: return
            for (action in actions) {
                val remoteInputs = action.remoteInputs ?: continue
                for (input in remoteInputs) {
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putCharSequence(input.resultKey, autoReply)
                    RemoteInput.addResultsToIntent(arrayOf(input), intent, bundle)
                    action.actionIntent.send(applicationContext, 0, intent)
                    return
                }
            }
        } catch (_: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        try {
            MediaPlayerHelper.clearIfFromPackage(sbn.packageName)
            synchronized(conversations) {
                conversations.removeAll { it.key == sbn.key }
            }
            listener?.onNotificationsChanged()
        } catch (_: Exception) {}
    }

    interface OnNotificationChangedListener {
        fun onNotificationsChanged()
    }
}
