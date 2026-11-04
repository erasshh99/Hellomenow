package com.piyush.island.model

/**
 * The seven palettes Shima ships with, plus the Material You option that takes its colours from
 * the wallpaper. A palette is a small set of ARGB values used by the island renderer.
 */
enum class PaletteId(val id: String, val displayName: String) {
    INK("ink", "Ink"),
    PAPER("paper", "Paper"),
    OCEAN("ocean", "Ocean"),
    FOREST("forest", "Forest"),
    SUNSET("sunset", "Sunset"),
    GRAPE("grape", "Grape"),
    MINT("mint", "Mint"),
    MATERIAL_YOU("material_you", "Material You");

    companion object {
        fun fromId(id: String?): PaletteId = entries.firstOrNull { it.id == id } ?: INK
    }
}

data class Palette(
    val background: Int,
    val foreground: Int,
    val surface: Int,
    val onSurface: Int,
    val accent: Int,
    val outline: Int,
)

object Palettes {

    private fun palette(bg: Int, fg: Int, surface: Int, onSurface: Int, accent: Int, outline: Int) =
        Palette(bg, fg, surface, onSurface, accent, outline)

    val inkDark = palette(0xFF15171C.toInt(), 0xFFF5F6FA.toInt(), 0xFF1E2128.toInt(), 0xFFF5F6FA.toInt(), 0xFF8AD5C6.toInt(), 0x33FFFFFF)
    val inkLight = palette(0xFFF3F4F8.toInt(), 0xFF15171C.toInt(), 0xFFFFFFFF.toInt(), 0xFF15171C.toInt(), 0xFF0F8E7E.toInt(), 0x22000000)

    val paperDark = palette(0xFFEDEFF4.toInt(), 0xFF17191E.toInt(), 0xFFFFFFFF.toInt(), 0xFF17191E.toInt(), 0xFF1D7A6E.toInt(), 0x1A000000)
    val paperLight = palette(0xFFFFFFFF.toInt(), 0xFF17191E.toInt(), 0xFFF7F8FB.toInt(), 0xFF17191E.toInt(), 0xFF1D7A6E.toInt(), 0x1A000000)

    val oceanDark = palette(0xFF0B1C33.toInt(), 0xFFE8F1FF.toInt(), 0xFF122745.toInt(), 0xFFE8F1FF.toInt(), 0xFF6FB6FF.toInt(), 0x33FFFFFF)
    val oceanLight = palette(0xFFDCEBFF.toInt(), 0xFF08203D.toInt(), 0xFFFFFFFF.toInt(), 0xFF08203D.toInt(), 0xFF1C6FE0.toInt(), 0x1A000000)

    val forestDark = palette(0xFF122019.toInt(), 0xFFE6F5EA.toInt(), 0xFF1A2C23.toInt(), 0xFFE6F5EA.toInt(), 0xFF7FD69B.toInt(), 0x33FFFFFF)
    val forestLight = palette(0xFFDCF1E1.toInt(), 0xFF102018.toInt(), 0xFFFFFFFF.toInt(), 0xFF102018.toInt(), 0xFF2E8B4E.toInt(), 0x1A000000)

    val sunsetDark = palette(0xFF2B160D.toInt(), 0xFFFFEDE2.toInt(), 0xFF3A2013.toInt(), 0xFFFFEDE2.toInt(), 0xFFFFB27A.toInt(), 0x33FFFFFF)
    val sunsetLight = palette(0xFFFFE4D2.toInt(), 0xFF2A150C.toInt(), 0xFFFFFFFF.toInt(), 0xFF2A150C.toInt(), 0xFFE2622C.toInt(), 0x1A000000)

    val grapeDark = palette(0xFF1D1330.toInt(), 0xFFF0E9FF.toInt(), 0xFF281B41.toInt(), 0xFFF0E9FF.toInt(), 0xFFC3A6FF.toInt(), 0x33FFFFFF)
    val grapeLight = palette(0xFFEAE0FF.toInt(), 0xFF1C1130.toInt(), 0xFFFFFFFF.toInt(), 0xFF1C1130.toInt(), 0xFF7A4FD6.toInt(), 0x1A000000)

    val mintDark = palette(0xFF0C2420.toInt(), 0xFFE2FAF4.toInt(), 0xFF12332D.toInt(), 0xFFE2FAF4.toInt(), 0xFF63D6C2.toInt(), 0x33FFFFFF)
    val mintLight = palette(0xFFD6F5ED.toInt(), 0xFF0A201D.toInt(), 0xFFFFFFFF.toInt(), 0xFF0A201D.toInt(), 0xFF13897A.toInt(), 0x1A000000)

    /** Resolves a palette for the given id and dark/light mode. */
    fun of(id: PaletteId, dark: Boolean): Palette = when (id) {
        PaletteId.INK -> if (dark) inkDark else inkLight
        PaletteId.PAPER -> if (dark) paperDark else paperLight
        PaletteId.OCEAN -> if (dark) oceanDark else oceanLight
        PaletteId.FOREST -> if (dark) forestDark else forestLight
        PaletteId.SUNSET -> if (dark) sunsetDark else sunsetLight
        PaletteId.GRAPE -> if (dark) grapeDark else grapeLight
        PaletteId.MINT -> if (dark) mintDark else mintLight
        PaletteId.MATERIAL_YOU -> if (dark) inkDark else inkLight
    }

    /** The seven palettes a user can pick, in the order they are shown. */
    val selectable: List<PaletteId> = listOf(
        PaletteId.INK,
        PaletteId.PAPER,
        PaletteId.OCEAN,
        PaletteId.FOREST,
        PaletteId.SUNSET,
        PaletteId.GRAPE,
        PaletteId.MINT,
    )
}
