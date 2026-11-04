package com.piyush.island.model

/** The gestures the island understands. */
enum class Gesture(val id: String, val labelRes: Int) {
    TAP("tap", com.piyush.island.R.string.gesture_tap),
    LONG_PRESS("long_press", com.piyush.island.R.string.gesture_long_press),
    SWIPE_UP("swipe_up", com.piyush.island.R.string.gesture_swipe_up),
    SWIPE_DOWN("swipe_down", com.piyush.island.R.string.gesture_swipe_down),
    SWIPE_LEFT("swipe_left", com.piyush.island.R.string.gesture_swipe_left),
    SWIPE_RIGHT("swipe_right", com.piyush.island.R.string.gesture_swipe_right);

    companion object {
        fun fromId(id: String?): Gesture? = entries.firstOrNull { it.id == id }
    }
}

/**
 * What a gesture can do. Kept as a sealed hierarchy so the UI can render a label for it and
 * the executor can run it without reflection.
 */
sealed class GestureAction {
    abstract val id: String
    abstract val labelRes: Int

    data object None : GestureAction() {
        override val id = "none"
        override val labelRes = com.piyush.island.R.string.gesture_none
    }

    data class LaunchApp(val packageName: String, val label: String) : GestureAction() {
        override val id = "app:$packageName"
        override val labelRes = com.piyush.island.R.string.action_open_app
    }

    data class OpenWidget(val appWidgetId: Int, val label: String) : GestureAction() {
        override val id = "widget:$appWidgetId"
        override val labelRes = com.piyush.island.R.string.action_open_widget
    }

    data class SystemAction(val action: SystemActionKind) : GestureAction() {
        override val id = "system:${action.id}"
        override val labelRes = action.labelRes
    }

    data class TaskerTask(val packageName: String, val taskName: String) : GestureAction() {
        override val id = "tasker:$packageName:$taskName"
        override val labelRes = com.piyush.island.R.string.action_tasker
    }
}

enum class SystemActionKind(val id: String, val labelRes: Int) {
    BACK("back", com.piyush.island.R.string.action_back),
    HOME("home", com.piyush.island.R.string.action_home),
    RECENTS("recents", com.piyush.island.R.string.action_recents),
    NOTIFICATION_SHADE("shade", com.piyush.island.R.string.action_shade),
    QUICK_SETTINGS("quick_settings", com.piyush.island.R.string.action_quick_settings),
    LOCK_SCREEN("lock", com.piyush.island.R.string.action_lock),
    SCREENSHOT("screenshot", com.piyush.island.R.string.action_screenshot),
    TORCH("torch", com.piyush.island.R.string.action_torch),
    ROTATION_LOCK("rotation", com.piyush.island.R.string.action_rotation),
    DND("dnd", com.piyush.island.R.string.action_dnd),
    MEDIA_PREVIOUS("media_previous", com.piyush.island.R.string.action_media_prev),
    MEDIA_NEXT("media_next", com.piyush.island.R.string.action_media_next),
    MEDIA_PLAY_PAUSE("media_play_pause", com.piyush.island.R.string.action_media_play_pause);

    companion object {
        fun fromId(id: String?): SystemActionKind? = entries.firstOrNull { it.id == id }
    }
}

/** Gesture bindings for one surface (the collapsed island or an open card). */
data class GestureBindings(
    val collapsed: Map<Gesture, GestureAction> = emptyMap(),
    val expanded: Map<Gesture, GestureAction> = emptyMap(),
) {
    fun actionFor(surface: Surface, gesture: Gesture): GestureAction =
        (if (surface == Surface.COLLAPSED) collapsed else expanded)[gesture] ?: GestureAction.None

    fun with(surface: Surface, gesture: Gesture, action: GestureAction): GestureBindings {
        val map = (if (surface == Surface.COLLAPSED) collapsed else expanded).toMutableMap()
        map[gesture] = action
        return if (surface == Surface.COLLAPSED) copy(collapsed = map) else copy(expanded = map)
    }
}

enum class Surface { COLLAPSED, EXPANDED }
