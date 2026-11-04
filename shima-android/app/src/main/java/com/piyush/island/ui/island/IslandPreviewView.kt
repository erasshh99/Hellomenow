package com.piyush.island.ui.island

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.piyush.island.R
import com.piyush.island.island.IslandIcons
import com.piyush.island.model.IslandStyle
import com.piyush.island.model.Palettes

/**
 * The preview that is drawn in the settings. It shows the island in the current palette, with the
 * camera cutout marked so the user can see how the island lines up with it.
 */
class IslandPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private val font: Typeface? = ResourcesCompat.getFont(context, R.font.google_sans_flex)

    private var style: IslandStyle = IslandStyle()
    private var dark = true
    private var expanded = false

    init {
        textPaint.typeface = font
    }

    fun render(style: IslandStyle, dark: Boolean, expanded: Boolean = false) {
        this.style = style
        this.dark = dark
        this.expanded = expanded
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val palette = Palettes.of(style.palette, dark)

        // A faint status bar with the camera cutout in it.
        paint.style = Paint.Style.FILL
        paint.color = 0x22000000
        rect.set(0f, 0f, width.toFloat(), 28f * density)
        canvas.drawRect(rect, paint)

        val cutoutWidth = 34f * density
        val cutoutHeight = 16f * density
        rect.set(
            (width - cutoutWidth) / 2f,
            0f,
            (width + cutoutWidth) / 2f,
            cutoutHeight,
        )
        canvas.drawRoundRect(rect, cutoutHeight / 2f, cutoutHeight / 2f, paint)

        val islandWidth = if (expanded) width - 48f * density else style.widthDp * density
        val islandHeight = if (expanded) 92f * density else style.heightDp * density
        val left = (width - islandWidth) / 2f
        val top = 28f * density + 12f * density

        val radius = style.cornerRadiusDp * density
        paint.style = Paint.Style.FILL
        paint.color = if (style.customBackground != 0) style.customBackground else palette.background
        rect.set(left, top, left + islandWidth, top + islandHeight)
        canvas.drawRoundRect(rect, radius, radius, paint)

        if (style.outlineWidthDp > 0 && !expanded) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = style.outlineWidthDp * density
            paint.color = style.outlineColor
            val inset = paint.strokeWidth / 2f
            rect.set(left + inset, top + inset, left + islandWidth - inset, top + islandHeight - inset)
            canvas.drawRoundRect(rect, radius, radius, paint)
        }

        val foreground = if (style.customForeground != 0) style.customForeground else palette.foreground
        val iconSize = if (expanded) 26f * density else islandHeight * 0.5f
        val iconX = left + if (expanded) 34f * density else islandWidth / 2f
        val iconY = top + if (expanded) 32f * density else islandHeight / 2f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = iconSize * 0.16f
        paint.color = foreground
        IslandIcons.draw(canvas, "music", iconX, iconY, iconSize, paint)

        if (expanded) {
            textPaint.color = foreground
            textPaint.textSize = 15f * density
            canvas.drawText("Example track", left + 62f * density, top + 28f * density, textPaint)
            textPaint.textSize = 12f * density
            textPaint.alpha = 180
            canvas.drawText("Example artist", left + 62f * density, top + 46f * density, textPaint)
            paint.style = Paint.Style.FILL
            paint.color = palette.accent
            rect.set(left + 62f * density, top + 62f * density, left + (islandWidth - 62f * density) * 0.4f, top + 66f * density)
            canvas.drawRoundRect(rect, 2f * density, 2f * density, paint)
        } else {
            textPaint.color = foreground
            textPaint.textSize = islandHeight * 0.34f
            textPaint.alpha = 255
            canvas.drawText("Shima", iconX + iconSize * 0.8f, iconY + textPaint.textSize * 0.35f, textPaint)
        }
    }
}
