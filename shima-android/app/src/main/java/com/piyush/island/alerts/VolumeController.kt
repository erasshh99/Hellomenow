package com.piyush.island.alerts

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import com.piyush.island.island.IslandCard
import com.piyush.island.model.AlertType

/**
 * Notices volume, ringer and brightness changes and puts them on the island.
 */
class VolumeController(context: Context, private val onCard: (IslandCard?) -> Unit) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var observer: ContentObserver? = null
    private var lastVolume = -1
    private var lastRinger = -1
    private var lastBrightness = -1

    fun start() {
        val obs = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                publish()
            }
        }
        observer = obs
        appContext.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            obs,
        )
        publish()
    }

    fun stop() {
        observer?.let { runCatching { appContext.contentResolver.unregisterContentObserver(it) } }
        observer = null
    }

    private fun publish() {
        val volume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val ringer = audioManager?.getStreamVolume(AudioManager.STREAM_RING) ?: 0
        val ringerMax = audioManager?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 1
        val brightness = Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)

        val changed = volume != lastVolume || ringer != lastRinger || brightness != lastBrightness
        val volumeChanged = volume != lastVolume
        val ringerChanged = ringer != lastRinger
        val brightnessChanged = brightness != lastBrightness
        lastVolume = volume
        lastRinger = ringer
        lastBrightness = brightness
        if (!changed) return

        val level = if (max > 0) volume * 100f / max else 0f
        val title = when {
            ringerChanged -> "Ringer"
            brightnessChanged && !volumeChanged -> "Brightness"
            else -> "Volume"
        }
        val shown = when (title) {
            "Ringer" -> if (ringerMax > 0) ringer * 100f / ringerMax else 0f
            "Brightness" -> if (max > 0) brightness * 100f / 255f else 0f
            else -> level
        }
        onCard(
            IslandCard(
                type = AlertType.VOLUME,
                title = title,
                subtitle = "${(shown * 100).toInt()}%",
                icon = if (title == "Brightness") "brightness" else "volume",
                color = 0xFF3AAFA9.toInt(),
                accent = 0xFF8AD5C6.toInt(),
                progress = shown.coerceIn(0f, 1f),
                autoDismissAfterMs = 2_500,
            )
        )
    }
}
