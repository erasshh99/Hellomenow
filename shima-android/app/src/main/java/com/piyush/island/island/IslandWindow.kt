package com.piyush.island.island

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.piyush.island.model.IslandStyle
import com.piyush.island.model.IslandAnimation
import kotlin.math.max

/**
 * Owns the overlay window: adds the island view to the window manager, keeps the layout
 * parameters in step with the style and animates between the collapsed capsule and the card.
 */
class IslandWindow(context: Context, private val listener: IslandView.Listener) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val density = appContext.resources.displayMetrics.density

    private var root: View? = null
    private var islandView: IslandView? = null
    private var params: WindowManager.LayoutParams? = null
    private var cutout: CutoutInfo = CutoutInfo()
    private var style: IslandStyle = IslandStyle()
    private var state: IslandUiState = IslandUiState()

    val isAttached: Boolean get() = root != null

    fun attach() {
        if (root != null) return
        val wm = windowManager ?: return
        val view = IslandView(appContext).apply { this.listener = this@IslandWindow.listener }
        islandView = view

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        )
        layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params = layoutParams
        root = view
        runCatching { wm.addView(view, layoutParams) }
    }

    fun detach() {
        val view = root ?: return
        root = null
        islandView = null
        runCatching { windowManager?.removeView(view) }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

    fun updateCutout(info: CutoutInfo) {
        cutout = info
        applyLayout()
    }

    fun render(newState: IslandUiState, newStyle: IslandStyle, dark: Boolean) {
        state = newState
        style = newStyle
        islandView?.render(newState, newStyle, dark)
        applyLayout()
        animate()
    }

    private fun applyLayout() {
        val layoutParams = params ?: return
        val wm = windowManager ?: return
        val expanded = state.phase == IslandPhase.EXPANDED

        val collapsedWidth = style.widthDp * density
        val expandedWidth = max(collapsedWidth, 320f * density)
        val collapsedHeight = style.heightDp * density
        val expandedHeight = max(collapsedHeight, 96f * density)

        val newWidth = if (expanded) expandedWidth.toInt() else collapsedWidth.toInt()
        val newHeight = if (expanded) expandedHeight.toInt() else collapsedHeight.toInt()
        if (layoutParams.width != newWidth || layoutParams.height != newHeight) {
            layoutParams.width = newWidth
            layoutParams.height = newHeight
            runCatching { wm.updateViewLayout(root, layoutParams) }
        }

        val cutoutTop = if (style.fitToCutout) cutout.top else 0
        layoutParams.y = (cutoutTop + style.offsetYDp * density).toInt()
        layoutParams.x = horizontalOffset(newWidth)
        runCatching { wm.updateViewLayout(root, layoutParams) }
    }

    /**
     * Lines the island up with the camera cutout. When the cutout is not centred the island is
     * pushed to the side that has the camera, or centred on both sides when there is none.
     */
    private fun horizontalOffset(widthPx: Int): Int {
        val userOffset = style.offsetXDp * density
        if (!style.fitToCutout || cutout.displayWidth == 0) return userOffset.toInt()

        val displayWidth = cutout.displayWidth
        val cutoutCenter = cutout.centerX
        val target = when (style.cutoutSide) {
            com.piyush.island.model.CutoutSide.LEFT -> cutout.left + cutout.cutoutWidth / 2
            com.piyush.island.model.CutoutSide.RIGHT -> cutout.right + cutout.cutoutWidth / 2
            com.piyush.island.model.CutoutSide.CENTER -> cutoutCenter
        }
        val gravityOffset = target - widthPx / 2
        // Gravity is CENTER_HORIZONTAL, so the x offset is measured from the centre.
        val centerBased = gravityOffset - (displayWidth / 2 - widthPx / 2)
        return (centerBased + userOffset).toInt()
    }

    private fun animate() {
        val view = root ?: return
        val expanded = state.phase == IslandPhase.EXPANDED
        val animation = style.animation
        view.animate().cancel()
        when (animation) {
            IslandAnimation.POP -> {
                view.scaleX = if (expanded) 0.86f else 1f
                view.scaleY = view.scaleX
                view.alpha = if (expanded) 0f else 1f
                view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(180).start()
            }

            IslandAnimation.SLIDE -> {
                view.translationY = if (expanded) -24f * density else 0f
                view.animate().translationY(0f).setDuration(200).start()
            }

            IslandAnimation.SPRING -> {
                view.scaleX = if (expanded) 0.9f else 1f
                view.scaleY = view.scaleX
                view.animate().scaleX(1f).scaleY(1f).setDuration(260).start()
            }

            IslandAnimation.SHARP -> view.animate().alpha(1f).setDuration(90).start()
            IslandAnimation.ROUNDED -> view.animate().alpha(1f).setDuration(160).start()
            IslandAnimation.EMPHASIZED -> {
                view.scaleX = if (expanded) 1.04f else 0.96f
                view.scaleY = view.scaleX
                view.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
            }
        }
    }
}
