package com.piyush.island.island

import android.graphics.Rect
import android.view.WindowInsets
import android.view.DisplayCutout

/**
 * Where the camera cutout sits on the current screen. The activity reports it while it is
 * visible; the overlay uses it to line the island up with the camera.
 */
data class CutoutInfo(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
    val displayWidth: Int = 0,
    val displayHeight: Int = 0,
) {
    val hasCutout: Boolean get() = left > 0 || right > 0 || top > 0 || bottom > 0

    /** Horizontal centre of the cutout in pixels. */
    val centerX: Int get() = (left + (displayWidth - right)) / 2

    val cutoutWidth: Int get() = (displayWidth - left - right).coerceAtLeast(0)

    companion object {
        fun from(insets: WindowInsets?, width: Int, height: Int): CutoutInfo {
            val cutout: DisplayCutout? = insets?.displayCutout
            return CutoutInfo(
                left = cutout?.safeInsetLeft ?: 0,
                right = cutout?.safeInsetRight ?: 0,
                top = cutout?.safeInsetTop ?: 0,
                bottom = cutout?.safeInsetBottom ?: 0,
                displayWidth = width,
                displayHeight = height,
            )
        }

        fun fromBounds(bounds: Rect?, width: Int, height: Int): CutoutInfo {
            val rect = bounds ?: return CutoutInfo(displayWidth = width, displayHeight = height)
            return CutoutInfo(
                left = rect.left,
                right = width - rect.right,
                top = rect.top,
                bottom = height - rect.bottom,
                displayWidth = width,
                displayHeight = height,
            )
        }
    }
}
