package com.ratio.launcher.fragments

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.R
import com.ratio.launcher.adapters.ConversationAdapter
import com.ratio.launcher.services.NotificationService

class TreeFragment : Fragment(), NotificationService.OnNotificationChangedListener {

    private lateinit var emptyState: LinearLayout
    private lateinit var treeContent: LinearLayout
    private lateinit var conversationList: RecyclerView
    private lateinit var enableBtn: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyState = view.findViewById(R.id.treeEmptyState)
        treeContent = view.findViewById(R.id.treeContent)
        conversationList = view.findViewById(R.id.conversationList)
        enableBtn = view.findViewById(R.id.treeEnableBtn)

        conversationList.layoutManager = LinearLayoutManager(requireContext())

        enableBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Always register as listener so we get updates even when on other pages
        NotificationService.listener = this
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        // Re-register in case it was cleared
        NotificationService.listener = this
        updateUI()
    }

    // Don't clear listener on pause — we want to keep receiving updates
    // even when the user is on Root or Tiles page

    override fun onNotificationsChanged() {
        handler.post {
            if (isAdded && (view != null)) {
                updateUI()
            }
        }
    }

    private fun updateUI() {
        if ((!isAdded) || (view == null)) return

        if (!isNotificationServiceEnabled()) {
            emptyState.visibility = View.VISIBLE
            treeContent.visibility = View.GONE
            enableBtn.visibility = View.VISIBLE
            emptyState.findViewById<TextView>(R.id.treeEmptyTitle)?.text = getString(R.string.tree_placeholder)
            emptyState.findViewById<TextView>(R.id.treeEmptySubtitle)?.text = getString(R.string.tree_grant_notification_access)
            return
        }

        val conversations: List<NotificationService.ConversationEntry>
        synchronized(NotificationService.conversations) {
            conversations = NotificationService.conversations.toList()
        }

        if (conversations.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            treeContent.visibility = View.GONE
            enableBtn.visibility = View.GONE
            emptyState.findViewById<TextView>(R.id.treeEmptyTitle)?.text = getString(R.string.tree_no_messages)
            emptyState.findViewById<TextView>(R.id.treeEmptySubtitle)?.text = getString(R.string.tree_no_messages_subtitle)
        } else {
            emptyState.visibility = View.GONE
            treeContent.visibility = View.VISIBLE

            val sorted = conversations.sortedByDescending { it.timestamp }
            val wallpaperActive = com.ratio.launcher.utils.WallpaperManager.hasWallpaperImage(requireContext())
            conversationList.adapter = ConversationAdapter(
                sorted,
                getAppIcon = { packageName ->
                    try { requireContext().packageManager.getApplicationIcon(packageName) }
                    catch (_: Exception) { null }
                },
                onTap = { entry -> openApp(entry.packageName) },
                onDismiss = { entry -> dismissNotification(entry.key) },
                hasWallpaper = wallpaperActive,
            )
        }
    }

    private fun openApp(packageName: String) {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
        intent?.let { startActivity(it) }
    }

    private fun dismissNotification(key: String) {
        try {
            synchronized(NotificationService.conversations) {
                NotificationService.conversations.removeAll { it.key == key }
            }
            updateUI()
            android.widget.Toast.makeText(requireContext(), "Dismissed", android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val context = context ?: return false
        val cn = ComponentName(context, NotificationService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }
}
