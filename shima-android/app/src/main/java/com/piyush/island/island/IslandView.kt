package com.piyush.island.island

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.piyush.island.R
import com.piyush.island.model.Gesture
import com.piyush.island.model.IslandStyle
import com.piyush.island.model.Palettes
import kotlin.math.abs

/**
 * The view that draws the island. It renders the collapsed capsule and the expanded card with
 * the same primitives, so the transition is only a matter of measuring a different shape.
 */
class IslandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onGesture(gesture: Gesture): Boolean
        fun onCardAction(action: CardAction): Boolean
        fun onTappedOutside(): Boolean
    }

    var listener: Listener? = null

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private val font: Typeface? = ResourcesCompat.getFont(context, R.font.google_sans_flex)

    private var style: IslandStyle = IslandStyle()
    private var state: IslandUiState = IslandUiState()
    private var dark = true

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var longPressFired = false

    private var actionRects = mutableMapOf<String, RectF>()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        textPaint.typeface = font
        isClickable = true
        isFocusable = true
    }

    fun render(newState: IslandUiState, newStyle: IslandStyle, isDark: Boolean) {
        state = newState
        style = newStyle
        dark = isDark
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val card = state.card ?: return
        val palette = Palettes.of(style.palette, dark)
        val background = if (style.customBackground != 0) style.customBackground else palette.background
        val foreground = if (style.customForeground != 0) style.customForeground else palette.foreground

        if (state.phase == IslandPhase.EXPANDED) {
            drawExpanded(canvas, card, background, foreground, palette.accent)
        } else {
            drawCollapsed(canvas, card, background, foreground, palette.accent)
        }
    }

    private fun drawCollapsed(
        canvas: Canvas,
        card: IslandCard,
        background: Int,
        foreground: Int,
        accent: Int,
    ) {
        val radius = style.cornerRadiusDp * density
        val alpha = ((1f - style.transparency / 100f).coerceIn(0.1f, 1f) * 255).toInt()
        paint.style = Paint.Style.FILL
        paint.color = background
        paint.alpha = alpha
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)

        if (style.outlineWidthDp > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = style.outlineWidthDp * density
            paint.color = style.outlineColor
            paint.alpha = 255
            val inset = paint.strokeWidth / 2f
            rect.set(inset, inset, width - inset, height - inset)
            canvas.drawRoundRect(rect, radius, radius, paint)
        }

        val iconSize = height * 0.5f
        val centerY = height / 2f
        val iconX = if (card.title.isEmpty()) width / 2f else height * 0.55f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = iconSize * 0.16f
        paint.color = foreground
        paint.alpha = 255
        IslandIcons.draw(canvas, card.icon, iconX, centerY, iconSize, paint)

        if (card.title.isNotEmpty()) {
            textPaint.color = foreground
            textPaint.alpha = 255
            textPaint.textSize = height * 0.34f
            textPaint.typeface = font
            val baseline = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(ellipsize(card.title, width - iconX - height * 0.7f), iconX + height * 0.42f, baseline, textPaint)
        }

        card.progress?.let { progress ->
            paint.style = Paint.Style.FILL
            paint.color = accent
            paint.alpha = 255
            val barHeight = height * 0.06f
            rect.set(0f, height - barHeight, width * progress.coerceIn(0f, 1f), height.toFloat())
            canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, paint)
        }
    }

    private fun drawExpanded(
        canvas: Canvas,
        card: IslandCard,
        background: Int,
        foreground: Int,
        accent: Int,
    ) {
        val radius = style.cornerRadiusDp * density
        val alpha = ((1f - style.expandedTransparency / 100f).coerceIn(0.1f, 1f) * 255).toInt()
        paint.style = Paint.Style.FILL
        paint.color = background
        paint.alpha = alpha
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)

        if (style.expandedOutlineWidthDp > 0) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = style.expandedOutlineWidthDp * density
            paint.color = style.expandedOutlineColor
            paint.alpha = 255
            val inset = paint.strokeWidth / 2f
            rect.set(inset, inset, width - inset, height - inset)
            canvas.drawRoundRect(rect, radius, radius, paint)
        }

        val pad = height * 0.16f
        val iconSize = height * 0.28f
        val iconX = pad + iconSize / 2f
        val topY = pad + iconSize / 2f

        card.artwork?.let { bitmap ->
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            val artSize = height * 0.38f
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val save = canvas.save()
            path.reset()
            path.addCircle(iconX, topY, artSize / 2f, Path.Direction.CW)
            canvas.clipPath(path)
            canvas.drawBitmap(bitmap, null, RectF(iconX - artSize / 2f, topY - artSize / 2f, iconX + artSize / 2f, topY + artSize / 2f), bitmapPaint)
            canvas.restoreToCount(save)
        } ?: run {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = iconSize * 0.16f
            paint.color = card.color
            paint.alpha = 255
            IslandIcons.draw(canvas, card.icon, iconX, topY, iconSize, paint)
        }

        val textLeft = iconX + height * 0.26f
        textPaint.color = foreground
        textPaint.alpha = 255
        textPaint.typeface = font
        textPaint.textSize = height * 0.115f
        val titleBaseline = topY - height * 0.02f
        canvas.drawText(ellipsize(card.title, width - textLeft - pad), textLeft, titleBaseline, textPaint)

        if (card.subtitle.isNotEmpty()) {
            textPaint.textSize = height * 0.085f
            textPaint.color = foreground
            textPaint.alpha = 190
            canvas.drawText(ellipsize(card.subtitle, width - textLeft - pad), textLeft, titleBaseline + height * 0.12f, textPaint)
        }

        card.progress?.let { progress ->
            paint.style = Paint.Style.FILL
            paint.color = accent
            paint.alpha = 255
            val barHeight = height * 0.05f
            rect.set(textLeft, height * 0.68f, textLeft + (width - textLeft - pad) * progress.coerceIn(0f, 1f), height * 0.68f + barHeight)
            canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, paint)
        }

        card.lyric?.takeIf { it.isNotBlank() }?.let { lyric ->
            textPaint.textSize = height * 0.075f
            textPaint.alpha = 160
            canvas.drawText(ellipsize(lyric, width - textLeft - pad), textLeft, height * 0.86f, textPaint)
        }

        drawActions(canvas, card, foreground, accent)
    }

    private fun drawActions(canvas: Canvas, card: IslandCard, foreground: Int, accent: Int) {
        if (card.actions.isEmpty()) return
        actionRects.clear()
        val size = height * 0.16f
        val gap = height * 0.06f
        var x = width - height * 0.16f
        val y = height * 0.86f
        textPaint.textSize = size
        textPaint.textAlign = Paint.Align.RIGHT
        card.actions.asReversed().forEach { action ->
            val label = context.getString(action.labelRes)
            val textWidth = textPaint.measureText(label)
            val rect = RectF(x - textWidth - gap, y - size, x, y + size * 0.4f)
            actionRects[action.id] = rect
            textPaint.color = foreground
            textPaint.alpha = 220
            canvas.drawText(label, x, y, textPaint)
            x -= textWidth + gap * 2
        }
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun ellipsize(text: String, maxWidth: Float): String {
        if (maxWidth <= 0) return ""
        if (textPaint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && textPaint.measureText(text.substring(0, end) + "…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = System.currentTimeMillis()
                longPressFired = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!longPressFired && abs(dx) < touchSlop && abs(dy) < touchSlop &&
                    System.currentTimeMillis() - downTime > LONG_PRESS_MS
                ) {
                    longPressFired = true
                    listener?.onGesture(Gesture.LONG_PRESS)
                    performClick()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!longPressFired) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    val gesture = when {
                        abs(dx) < touchSlop && abs(dy) < touchSlop -> Gesture.TAP
                        abs(dx) > abs(dy) -> if (dx > 0) Gesture.SWIPE_RIGHT else Gesture.SWIPE_LEFT
                        else -> if (dy > 0) Gesture.SWIPE_DOWN else Gesture.SWIPE_UP
                    }
                    if (gesture == Gesture.TAP) {
                        val actions = state.card?.actions.orEmpty()
                        val action = actions.firstOrNull { actionRect ->
                            actionRects[actionRect.id]?.contains(event.x, event.y) == true
                        }
                        if (action != null) {
                            listener?.onCardAction(action)
                        } else {
                            listener?.onGesture(Gesture.TAP)
                        }
                    } else {
                        listener?.onGesture(gesture)
                    }
                    performClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private val touchSlop: Float get() = 24f * density
    private val LONG_PRESS_MS = 420L
}
