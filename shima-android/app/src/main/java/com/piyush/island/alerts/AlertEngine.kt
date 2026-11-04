package com.piyush.island.alerts

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.piyush.island.data.SettingsStore
import com.piyush.island.island.IslandCard
import com.piyush.island.island.IslandStateHolder
import com.piyush.island.model.AlertType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Collects the cards the individual controllers produce and decides what the island shows.
 *
 * Rules:
 *  - an alert that is switched off never reaches the island;
 *  - an alert restricted to a set of packages is ignored when the card comes from another app;
 *  - the most recent card wins, and it is dismissed again after its display duration;
 *  - the companion pill keeps the previous card for a short while.
 */
class AlertEngine(
    context: Context,
    private val settings: SettingsStore,
    private val islandState: IslandStateHolder,
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    private var running = false
    private var dismissRunnable: Runnable? = null
    private var currentCard: IslandCard? = null
    private var currentSourcePackage: String? = null

    val media = MediaController(appContext) { card -> offer(AlertType.MEDIA, card, null) }
    val charging = ChargingController(appContext) { card -> offer(AlertType.CHARGING, card, null) }
    val flashlight = FlashlightController(appContext) { card -> offer(AlertType.FLASHLIGHT, card, null) }
    val volume = VolumeController(appContext) { card -> offer(AlertType.VOLUME, card, null) }
    val privacy = PrivacyController(appContext) { card -> offer(AlertType.PRIVACY, card, null) }
    /** The system controller raises several different kinds of card, so it brings its own type. */
    val system = SystemStateController(appContext) { card ->
        offer(card?.type ?: AlertType.DND, card, null)
    }

    fun start() {
        if (running) return
        running = true
        media.start()
        charging.start()
        flashlight.start()
        volume.start()
        privacy.start()
        system.start()
    }

    fun stop() {
        if (!running) return
        running = false
        media.stop()
        charging.stop()
        flashlight.stop()
        volume.stop()
        privacy.stop()
        system.stop()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
    }

    /** Publishes a card that came from a notification, which knows its source package. */
    fun offerFromNotification(type: AlertType, card: IslandCard?, packageName: String?) {
        offer(type, card, packageName)
    }

    private fun offer(type: AlertType, card: IslandCard?, packageName: String?) {
        if (!running) return
        val config = settings.current.configFor(type)
        if (!config.enabled) return
        if (card == null) {
            if (currentCard?.type == type) clear()
            return
        }
        if (config.packages.isNotEmpty() && packageName != null && packageName !in config.packages) return

        currentSourcePackage = packageName
        val resolved = card.copy(
            icon = config.icon.ifEmpty { card.icon },
            color = config.color.toInt(),
            autoDismissAfterMs = config.durationMs,
        )
        currentCard = resolved
        islandState.show(resolved)
        scheduleDismiss(config.durationMs)
    }

    private fun scheduleDismiss(durationMs: Int) {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable { clear() }
        dismissRunnable = runnable
        handler.postDelayed(runnable, durationMs.toLong().coerceAtLeast(1_500L))
    }

    fun clear() {
        currentCard = null
        currentSourcePackage = null
        islandState.dismiss()
    }

    /** The package the current card came from, used by the package filter. */
    fun currentSource(): String? = currentSourcePackage
}
