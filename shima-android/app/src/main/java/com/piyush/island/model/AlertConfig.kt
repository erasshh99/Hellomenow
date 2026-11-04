package com.piyush.island.model

/** How an expanded card is dismissed. */
enum class CloseMode(val id: String) {
    TAP_OUTSIDE("tap_outside"),
    SWIPE_UP("swipe_up"),
    BOTH("both"),
    NEVER("never");

    companion object {
        fun fromId(id: String?): CloseMode = entries.firstOrNull { it.id == id } ?: BOTH
    }
}

/** How the island opens and settles. */
enum class IslandAnimation(val id: String, val labelRes: Int) {
    POP("pop", com.piyush.island.R.string.pop),
    SLIDE("slide", com.piyush.island.R.string.slide),
    SPRING("spring", com.piyush.island.R.string.spring),
    SHARP("sharp", com.piyush.island.R.string.sharp),
    ROUNDED("rounded", com.piyush.island.R.string.rounded),
    EMPHASIZED("emphasized", com.piyush.island.R.string.emphasized);

    companion object {
        fun fromId(id: String?): IslandAnimation = entries.firstOrNull { it.id == id } ?: SPRING
    }
}

/** Where the camera cutout sits, so the island can be lined up with it. */
enum class CutoutSide(val id: String, val labelRes: Int) {
    LEFT("left", com.piyush.island.R.string.camera_left),
    RIGHT("right", com.piyush.island.R.string.camera_right),
    CENTER("center", com.piyush.island.R.string.camera_centered);

    companion object {
        fun fromId(id: String?): CutoutSide = entries.firstOrNull { it.id == id } ?: CENTER
    }
}

/** The shape the island takes while it is collapsed. */
enum class IslandShape(val id: String, val labelRes: Int) {
    TEARDROP("teardrop", com.piyush.island.R.string.teardrop),
    CAPSULE("capsule", com.piyush.island.R.string.rounded),
    PILL("pill", com.piyush.island.R.string.sharp);

    companion object {
        fun fromId(id: String?): IslandShape = entries.firstOrNull { it.id == id } ?: CAPSULE
    }
}

enum class ThemeMode(val id: String, val labelRes: Int) {
    LIGHT("light", com.piyush.island.R.string.theme_light),
    DARK("dark", com.piyush.island.R.string.theme_dark),
    SYSTEM("system", com.piyush.island.R.string.follow_system);

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/**
 * The per-alert configuration. Everything the user can tune for one alert lives here so the
 * card renderer stays generic.
 */
data class AlertConfig(
    val enabled: Boolean = false,
    val icon: String = "",
    val color: Long = 0xFFFFFFFF,
    val animation: IslandAnimation = IslandAnimation.SPRING,
    val durationMs: Int = 6_000,
    val closeMode: CloseMode = CloseMode.BOTH,
    val autoOpen: Boolean = false,
    val miniAlert: Boolean = true,
    /** Empty means "every app that can notify". */
    val packages: Set<String> = emptySet(),
    /** 0 = keep the default text, otherwise a custom template. */
    val template: String = "",
    val accentColor: Long = 0xFF8AD5C6,
    val typeface: String = "",
) {
    fun withDefaults(type: AlertType): AlertConfig = copy(
        icon = if (icon.isEmpty()) type.defaultIcon else icon,
        color = if (color == 0xFFFFFFFF) type.defaultColor else color,
    )
}
