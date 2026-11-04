package com.piyush.island.alerts

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.piyush.island.R
import com.piyush.island.island.CardAction
import com.piyush.island.island.IslandCard
import com.piyush.island.model.AlertType

/**
 * Watches the active media session and turns it into an island card: artwork, track, artist and
 * the transport controls. Lyrics are used when the player publishes them in the session metadata.
 */
class MediaController(context: Context, private val onCard: (IslandCard?) -> Unit) {

    private val appContext = context.applicationContext
    private var manager: MediaSessionManager? = null
    private var listener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    /** The package of the player that was last active, used for the package filter. */
    var lastSessionPackage: String? = null
        private set

    var playing: Boolean = false
        private set

    fun start() {
        if (manager != null) return
        manager = appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
            val session = sessions?.firstOrNull { it.playbackState != null }
            if (session != null) onSessionChanged(session)
        }
        listener = sessionListener
        runCatching {
            manager?.addOnActiveSessionsChangedListener(sessionListener, null)
        }
        refresh()
    }

    fun stop() {
        listener?.let { l -> runCatching { manager?.removeOnActiveSessionsChangedListener(l) } }
        listener = null
        manager = null
    }

    fun togglePlayPause() {
        val controller = currentController() ?: return
        val controls = controller.transportControls
        if (playing) controls.pause() else controls.play()
        refresh()
    }

    fun skipToNext() {
        currentController()?.transportControls?.skipToNext()
        refresh()
    }

    fun skipToPrevious() {
        currentController()?.transportControls?.skipToPrevious()
        refresh()
    }

    fun refresh() {
        val controller = currentController() ?: return
        onSessionChanged(controller)
    }

    private fun currentController(): MediaController? =
        manager?.getActiveSessions(null)?.firstOrNull { it.playbackState != null }

    private fun onSessionChanged(controller: MediaController) {
        val metadata = controller.metadata
        val state = controller.playbackState
        val packageName = controller.packageName
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty()
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val lyrics = metadata?.getString("android.media.metadata.LYRICS")
            ?: metadata?.getString("android.media.metadata.LYRIC")

        playing = state?.state == PlaybackState.STATE_PLAYING
        lastSessionPackage = packageName

        if (title.isEmpty() && album.isEmpty()) {
            onCard(null)
            return
        }

        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val card = IslandCard(
            type = AlertType.MEDIA,
            title = title.ifEmpty { album },
            subtitle = artist.ifEmpty { album },
            icon = "music",
            color = 0xFF7C5CFF.toInt(),
            accent = 0xFF8AD5C6.toInt(),
            artwork = artwork,
            lyric = lyrics,
            actions = listOf(
                CardAction("prev", R.string.card_media_prev, CardAction.Kind.PREVIOUS),
                CardAction("play_pause", R.string.card_media_play_pause, CardAction.Kind.PLAY_PAUSE),
                CardAction("next", R.string.card_media_next, CardAction.Kind.NEXT),
            ),
            autoDismissAfterMs = 8_000,
        )
        onCard(card)
    }
}
