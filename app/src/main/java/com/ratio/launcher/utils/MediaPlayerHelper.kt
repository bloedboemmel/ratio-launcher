package com.ratio.launcher.utils

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import com.ratio.launcher.services.NotificationService

data class MediaInfo(
    val title: String,
    val artist: String,
    val albumArt: android.graphics.Bitmap?,
    val isPlaying: Boolean,
    val packageName: String
)

object MediaPlayerHelper {

    private var currentMedia: MediaInfo? = null
    var listener: OnMediaChangedListener? = null
    private var activeCallback: MediaController.Callback? = null
    private var activeController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())

    private val MEDIA_PACKAGES = setOf(
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.apple.android.music",
        "deezer.android.app",
        "com.soundcloud.android",
        "com.pandora.android",
        "com.amazon.mp3",
        "com.google.android.music",
        "com.samsung.android.app.music",
        "com.sec.android.app.music"
    )

    fun processNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val extras = notification.extras

        val isMedia = notification.category == Notification.CATEGORY_TRANSPORT
            || MEDIA_PACKAGES.contains(sbn.packageName)

        if (!isMedia) return false

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return false
        val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank()) return false

        val largeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, android.graphics.Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_LARGE_ICON)
        }

        currentMedia = MediaInfo(
            title = title,
            artist = artist,
            albumArt = largeIcon,
            isPlaying = true,
            packageName = sbn.packageName
        )

        listener?.onMediaChanged(currentMedia)
        return true
    }

    fun updateFromController(controller: MediaController) {
        val metadata = controller.metadata ?: return
        val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: return
        val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING

        currentMedia = MediaInfo(
            title = title,
            artist = artist,
            albumArt = art,
            isPlaying = isPlaying,
            packageName = controller.packageName
        )

        // Register callback for state changes
        registerCallback(controller)

        listener?.onMediaChanged(currentMedia)
    }

    private fun registerCallback(controller: MediaController) {
        // Remove old callback
        activeCallback?.let { activeController?.unregisterCallback(it) }

        activeController = controller
        activeCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                val playing = state?.state == PlaybackState.STATE_PLAYING
                val stopped = state?.state == PlaybackState.STATE_STOPPED
                    || state?.state == PlaybackState.STATE_NONE

                if (stopped) {
                    currentMedia = null
                    handler.post { listener?.onMediaChanged(null) }
                } else {
                    currentMedia = currentMedia?.copy(isPlaying = playing)
                    handler.post { listener?.onMediaChanged(currentMedia) }
                }
            }

            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                if (metadata == null) return
                val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: return
                val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                val art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                currentMedia = currentMedia?.copy(title = title, artist = artist, albumArt = art)
                handler.post { listener?.onMediaChanged(currentMedia) }
            }
        }

        controller.registerCallback(activeCallback!!, handler)
    }

    fun clearIfFromPackage(packageName: String) {
        if (currentMedia?.packageName == packageName) {
            currentMedia = null
            activeCallback?.let { activeController?.unregisterCallback(it) }
            activeCallback = null
            activeController = null
            listener?.onMediaChanged(null)
        }
    }

    fun clear() {
        currentMedia = null
        activeCallback?.let { activeController?.unregisterCallback(it) }
        activeCallback = null
        activeController = null
        listener?.onMediaChanged(null)
    }

    fun getCurrentMedia(): MediaInfo? = currentMedia

    private fun getActiveController(context: Context): MediaController? {
        return try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(context, NotificationService::class.java)
            val controllers = msm.getActiveSessions(cn)
            controllers.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING ||
                it.playbackState?.state == PlaybackState.STATE_PAUSED
            } ?: controllers.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun playPause(context: Context) {
        val controller = getActiveController(context) ?: return
        // Ensure callback is registered so UI updates
        registerCallback(controller)
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
            // Immediate UI feedback while waiting for callback
            currentMedia = currentMedia?.copy(isPlaying = false)
            listener?.onMediaChanged(currentMedia)
        } else {
            controller.transportControls.play()
            currentMedia = currentMedia?.copy(isPlaying = true)
            listener?.onMediaChanged(currentMedia)
        }
    }

    fun skipNext(context: Context) {
        val controller = getActiveController(context) ?: return
        controller.transportControls.skipToNext()
    }

    fun skipPrevious(context: Context) {
        val controller = getActiveController(context) ?: return
        controller.transportControls.skipToPrevious()
    }

    interface OnMediaChangedListener {
        fun onMediaChanged(media: MediaInfo?)
    }
}
