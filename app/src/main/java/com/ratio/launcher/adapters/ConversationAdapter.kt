package com.ratio.launcher.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.R
import com.ratio.launcher.services.NotificationService

class ConversationAdapter(
    private val conversations: List<NotificationService.ConversationEntry>,
    private val getAppIcon: (String) -> android.graphics.drawable.Drawable?,
    private val onTap: ((NotificationService.ConversationEntry) -> Unit)? = null,
    private val onDismiss: ((NotificationService.ConversationEntry) -> Unit)? = null,
    private val hasWallpaper: Boolean = false,
) : RecyclerView.Adapter<ConversationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val appIcon: ImageView = view.findViewById(R.id.convAppIcon)
        private val sender: TextView = view.findViewById(R.id.convSender)
        private val message: TextView = view.findViewById(R.id.convMessage)
        private val time: TextView = view.findViewById(R.id.convTime)

        fun bind(entry: NotificationService.ConversationEntry) {
            if (hasWallpaper) {
                itemView.setBackgroundResource(R.drawable.conversation_background_wallpaper)
            } else {
                itemView.background = null
            }

            sender.text = entry.senderName
            message.text = entry.lastMessage

            val icon = getAppIcon(entry.packageName)
            if (icon != null) {
                appIcon.setImageDrawable(icon)
                appIcon.visibility = View.VISIBLE
            } else {
                appIcon.visibility = View.GONE
            }

            time.text = DateUtils.getRelativeTimeSpanString(
                entry.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )

            itemView.setOnClickListener { onTap?.invoke(entry) }
            itemView.setOnLongClickListener {
                onDismiss?.invoke(entry)
                true
            }
        }
    }
}
