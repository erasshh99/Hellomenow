package com.piyush.island.model

/**
 * Everything that decides how the island looks and where it sits. The values are stored in
 * density independent pixels and converted to pixels when the overlay is measured.
 */
data class IslandStyle(
    val widthDp: Int = 220,
    val heightDp: Int = 36,
    val offsetXDp: Int = 0,
    val offsetYDp: Int = 0,
    val cornerRadiusDp: Int = 18,
    val shape: IslandShape = IslandShape.CAPSULE,
    val transparency: Int = 0,
    val outlineWidthDp: Int = 0,
    val outlineColor: Int = 0x33FFFFFF,
    val shadowRadiusDp: Int = 0,
    val shadowColor: Int = 0x40000000,
    val expandedTransparency: Int = 0,
    val expandedOutlineWidthDp: Int = 0,
    val expandedOutlineColor: Int = 0x33FFFFFF,
    val expandedShadowRadiusDp: Int = 12,
    val animation: IslandAnimation = IslandAnimation.SPRING,
    val cutoutSide: CutoutSide = CutoutSide.CENTER,
    val palette: PaletteId = PaletteId.INK,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customBackground: Int = 0,
    val customForeground: Int = 0,
    val companionEnabled: Boolean = false,
    val companionColor: Int = 0xFF2E9E5B.toInt(),
    val fitToCutout: Boolean = true,
    val hideInFullscreen: Boolean = true,
    val hiddenPackages: Set<String> = emptySet(),
    val showOnLockScreen: Boolean = true,
    val liveActivityLike: Boolean = true,
) {
    val isDark: Boolean
        get() = when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> true
        }
}

/** The complete user configuration. Immutable; every change produces a new snapshot. */
data class ShimaSettings(
    val enabled: Boolean = true,
    val keepAlive: Boolean = false,
    val autoBackup: Boolean = false,
    val style: IslandStyle = IslandStyle(),
    val alerts: Map<AlertType, AlertConfig> = AlertType.entries.associateWith { AlertConfig() },
    val gestures: GestureBindings = GestureBindings(),
    val rules: List<Rule> = emptyList(),
    val appRow: List<String> = emptyList(),
    val decks: Map<String, List<String>> = emptyMap(),
    val proUnlocked: Boolean = false,
    val onboardingDone: Boolean = false,
) {
    fun configFor(type: AlertType): AlertConfig = alerts[type] ?: AlertConfig().withDefaults(type)

    fun alertEnabled(type: AlertType): Boolean = configFor(type).enabled
}
