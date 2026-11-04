package com.piyush.island.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.piyush.island.island.IslandIcons

/**
 * Draws one of the island glyphs inside a soft circle, used in the alert lists.
 */
class IslandIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var icon: String = "app"
        set(value) {
            field = value
            invalidate()
        }

    var tint: Int = 0xFF7C5CFF.toInt()
        set(value) {
            field = value
            invalidate()
        }

    init {
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f

        paint.style = Paint.Style.FILL
        paint.color = (tint and 0x00FFFFFF) or (0x22 shl 24)
        canvas.drawCircle(cx, cy, size / 2f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.075f
        paint.color = tint
        IslandIcons.draw(canvas, icon, cx, cy, size * 0.5f, paint)
    }
}
