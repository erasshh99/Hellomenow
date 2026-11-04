package com.piyush.island.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.piyush.island.core.AppGraph
import com.piyush.island.content.IslandNotificationListener
import com.piyush.island.gestures.ActionExecutor
import com.piyush.island.island.CardAction
import com.piyush.island.island.CutoutInfo
import com.piyush.island.island.IslandPhase
import com.piyush.island.island.IslandUiState
import com.piyush.island.island.IslandView
import com.piyush.island.island.IslandWindow
import com.piyush.island.model.Gesture
import com.piyush.island.model.GestureAction
import com.piyush.island.model.RuleState
import com.piyush.island.model.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The accessibility service does three things: it draws the island above the status bar, it
 * performs the gestures the user bound to the island, and it keeps an eye on which app is in the
 * foreground so the island can step aside.
 */
class IslandAccessibilityService : AccessibilityService(), IslandView.Listener {

    companion object {
        /**
         * The running instance, so the activity can hand the camera cutout to the overlay. It is
         * null when the user has not switched the accessibility service on.
         */
        @Volatile
        var instance: IslandAccessibilityService? = null
            private set

        /** A window counts as full screen when it covers at least this much of the display. */
        private const val FULL_SCREEN_RATIO = 0.95f
    }

    private lateinit var window: IslandWindow
    private lateinit var executor: ActionExecutor
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var foregroundPackage: String? = null
    private var foregroundIsFullScreen = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppGraph.install(applicationContext)

        window = IslandWindow(applicationContext, this)
        executor = ActionExecutor(this)
        window.attach()

        observeJob = scope.launch {
            AppGraph.islandState.state.collect { state -> render(state) }
        }

        if (AppGraph.settings.current.enabled) {
            AppGraph.alertEngine.start()
        }
        AppGraph.islandState.state.value.let { if (it.isVisible) render(it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == packageNameOfSelf()) return
        foregroundPackage = packageName
        foregroundIsFullScreen = isFullScreen(event)
        applyVisibility()
    }

    override fun onInterrupt() {
        // Nothing to interrupt: the island keeps running.
    }

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        AppGraph.alertEngine.stop()
        if (::window.isInitialized) window.detach()
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun packageNameOfSelf(): String = applicationContext.packageName

    private fun render(state: IslandUiState) {
        if (!::window.isInitialized) return
        val settings = AppGraph.settings.current
        val dark = when (settings.style.themeMode) {
            com.piyush.island.model.ThemeMode.DARK -> true
            com.piyush.island.model.ThemeMode.LIGHT -> false
            com.piyush.island.model.ThemeMode.SYSTEM -> isSystemDark()
        }
        window.render(state, settings.style, dark)
        applyVisibility()
    }

    private fun isSystemDark(): Boolean {
        val mode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    /** Hides the island in the apps the user picked, and in full-screen apps when asked. */
    private fun applyVisibility() {
        if (!::window.isInitialized) return
        val settings = AppGraph.settings.current
        val state = AppGraph.islandState.state.value
        if (!state.isVisible) return

        val hidden = settings.style.hiddenPackages
        val current = foregroundPackage
        if (current != null && current in hidden) {
            AppGraph.islandState.hide()
            return
        }
        if (settings.style.hideInFullscreen && foregroundIsFullScreen) {
            AppGraph.islandState.hide()
            return
        }
        if (!settings.enabled) {
            AppGraph.islandState.hide()
        }
    }

    /**
     * Whether the app in the foreground fills the whole screen.
     *
     * Only the size of the window is looked at; the content of the screen is never read.
     */
    private fun isFullScreen(event: AccessibilityEvent): Boolean {
        val window = rootInActiveWindow?.window ?: return false
        val windowBounds = android.graphics.Rect()
        window.getBoundsInScreen(windowBounds)
        if (windowBounds.width() == 0 || windowBounds.height() == 0) return false

        val displayManager = getSystemService(android.content.Context.DISPLAY_SERVICE)
            as? android.hardware.display.DisplayManager ?: return false
        val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: return false
        val displayBounds = android.graphics.Rect()
        display.getRectSize(displayBounds)
        if (displayBounds.width() == 0 || displayBounds.height() == 0) return false

        val wideEnough = windowBounds.width() >= displayBounds.width() * FULL_SCREEN_RATIO
        val tallEnough = windowBounds.height() >= displayBounds.height() * FULL_SCREEN_RATIO
        return wideEnough && tallEnough
    }

    fun updateCutout(info: CutoutInfo) {
        if (::window.isInitialized) window.updateCutout(info)
    }

    // ------------------------------------------------------------------ gestures

    override fun onGesture(gesture: Gesture): Boolean {
        val settings = AppGraph.settings.current
        val surface = if (AppGraph.islandState.state.value.phase == IslandPhase.EXPANDED) {
            Surface.EXPANDED
        } else {
            Surface.COLLAPSED
        }

        val action = resolveAction(settings.rules, gesture, surface, settings.gestures.actionFor(surface, gesture))
        if (action != GestureAction.None) {
            return executor.execute(action)
        }

        // Without a binding the island still reacts: a tap opens and closes the card.
        return when (gesture) {
            Gesture.TAP -> {
                if (AppGraph.islandState.state.value.phase == IslandPhase.EXPANDED) {
                    AppGraph.islandState.collapse()
                } else {
                    AppGraph.islandState.expand()
                }
                true
            }

            Gesture.LONG_PRESS -> {
                AppGraph.alertEngine.clear()
                true
            }

            else -> false
        }
    }

    /** Rules win over the plain binding when their conditions are met. */
    private fun resolveAction(
        rules: List<com.piyush.island.model.Rule>,
        gesture: Gesture,
        surface: Surface,
        fallback: GestureAction,
    ): GestureAction {
        val state = ruleState()
        val match = rules.firstOrNull { it.enabled && it.gesture == gesture && it.matches(state) }
        return match?.action ?: fallback
    }

    private fun ruleState(): RuleState {
        val engine = AppGraph.alertEngine
        val power = getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        val keyguard = getSystemService(android.content.Context.KEYGUARD_SERVICE)
            as? android.app.KeyguardManager
        val notifications = getSystemService(android.content.Context.NOTIFICATION_SERVICE)
            as? android.app.NotificationManager
        val dnd = notifications?.currentInterruptionFilter ==
            android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
            notifications?.currentInterruptionFilter ==
            android.app.NotificationManager.INTERRUPTION_FILTER_NONE
        return RuleState(
            charging = engine.charging.charging,
            mediaPlaying = engine.media.playing,
            dnd = dnd,
            screenOn = power?.isInteractive ?: true,
            locked = keyguard?.isKeyguardLocked ?: false,
            batteryLow = engine.charging.batteryLevel in 1..19,
        )
    }

    override fun onCardAction(action: CardAction): Boolean = when (action.kind) {
        CardAction.Kind.PLAY_PAUSE -> {
            AppGraph.alertEngine.media.togglePlayPause()
            true
        }

        CardAction.Kind.PREVIOUS -> {
            AppGraph.alertEngine.media.skipToPrevious()
            true
        }

        CardAction.Kind.NEXT -> {
            AppGraph.alertEngine.media.skipToNext()
            true
        }

        CardAction.Kind.MUTE -> {
            muteRinger()
            true
        }

        CardAction.Kind.OPEN -> openSourceApp()
        CardAction.Kind.SHARE -> shareScreenshot()
        CardAction.Kind.COPY -> copyScreenshot()
        CardAction.Kind.EDIT -> editScreenshot()
        CardAction.Kind.DELETE -> deleteScreenshot()
        CardAction.Kind.BATTERY_SAVER -> {
            val intent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(intent) }.isSuccess
        }
    }

    /** Opens the app the card came from, used by the live cards. */
    private fun openSourceApp(): Boolean {
        val packageName = AppGraph.alertEngine.currentSource() ?: return false
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { startActivity(intent) }.isSuccess
    }

    /** Silences the ringer without touching the media volume. */
    private fun muteRinger(): Boolean {
        val audio = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            ?: return false
        return runCatching {
            audio.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
            true
        }.getOrDefault(false)
    }

    private fun screenshotUri(): android.net.Uri? = IslandNotificationListener.lastScreenshotUri

    private fun shareScreenshot(): Boolean {
        val uri = screenshotUri() ?: return false
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { startActivity(Intent.createChooser(intent, null)) }.isSuccess
    }

    private fun copyScreenshot(): Boolean {
        val uri = screenshotUri() ?: return false
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager ?: return false
        clipboard.setPrimaryClip(android.content.ClipData.newUri(contentResolver, "Screenshot", uri))
        return true
    }

    private fun editScreenshot(): Boolean {
        val uri = screenshotUri() ?: return false
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { startActivity(intent) }.isSuccess
    }

    private fun deleteScreenshot(): Boolean {
        val uri = screenshotUri() ?: return false
        val intent = Intent(this, com.piyush.island.ui.screenshot.DeleteRequestActivity::class.java).apply {
            putExtra(com.piyush.island.ui.screenshot.DeleteRequestActivity.EXTRA_URI, uri.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { startActivity(intent) }.isSuccess
    }

    override fun onTappedOutside(): Boolean {
        AppGraph.islandState.collapse()
        return true
    }
}
