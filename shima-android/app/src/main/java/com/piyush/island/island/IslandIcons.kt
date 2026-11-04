package com.piyush.island.island

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * The little glyphs the island draws. They are painted by hand so the island keeps its own
 * weight and does not depend on an icon font.
 */
object IslandIcons {

    private val path = Path()
    private val rect = RectF()

    fun draw(canvas: Canvas, icon: String, cx: Float, cy: Float, size: Float, paint: Paint) {
        val s = size / 2f
        when (icon) {
            "music" -> drawMusic(canvas, cx, cy, s, paint)
            "battery" -> drawBattery(canvas, cx, cy, s, paint)
            "battery_low" -> drawBattery(canvas, cx, cy, s, paint, low = true)
            "battery_full" -> drawBattery(canvas, cx, cy, s, paint, full = true)
            "torch" -> drawTorch(canvas, cx, cy, s, paint)
            "timer" -> drawTimer(canvas, cx, cy, s, paint)
            "volume" -> drawVolume(canvas, cx, cy, s, paint)
            "brightness" -> drawBrightness(canvas, cx, cy, s, paint)
            "mic" -> drawMic(canvas, cx, cy, s, paint)
            "camera" -> drawCamera(canvas, cx, cy, s, paint)
            "dnd" -> drawDnd(canvas, cx, cy, s, paint)
            "usb" -> drawUsb(canvas, cx, cy, s, paint)
            "signal" -> drawSignal(canvas, cx, cy, s, paint)
            "alarm" -> drawAlarm(canvas, cx, cy, s, paint)
            "rotate" -> drawRotate(canvas, cx, cy, s, paint)
            "record" -> drawRecord(canvas, cx, cy, s, paint)
            "shot" -> drawShot(canvas, cx, cy, s, paint)
            "saver" -> drawLeaf(canvas, cx, cy, s, paint)
            "app" -> drawApp(canvas, cx, cy, s, paint)
            else -> drawApp(canvas, cx, cy, s, paint)
        }
    }

    private fun drawMusic(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.22f
        paint.strokeCap = Paint.Cap.ROUND
        path.reset()
        path.moveTo(cx + s * 0.35f, cy - s * 0.7f)
        path.lineTo(cx + s * 0.35f, cy + s * 0.45f)
        path.moveTo(cx + s * 0.35f, cy - s * 0.7f)
        path.lineTo(cx - s * 0.5f, cy - s * 0.45f)
        path.moveTo(cx + s * 0.35f, cy - s * 0.2f)
        path.lineTo(cx - s * 0.5f, cy + s * 0.05f)
        canvas.drawPath(path, paint)
        canvas.drawCircle(cx - s * 0.62f, cy + s * 0.5f, s * 0.28f, paint)
        canvas.drawCircle(cx + s * 0.22f, cy + s * 0.45f, s * 0.28f, paint)
    }

    private fun drawBattery(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint, low: Boolean = false, full: Boolean = false) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        rect.set(cx - s * 0.7f, cy - s * 0.42f, cx + s * 0.55f, cy + s * 0.42f)
        canvas.drawRoundRect(rect, s * 0.16f, s * 0.16f, paint)
        rect.set(cx + s * 0.62f, cy - s * 0.18f, cx + s * 0.78f, cy + s * 0.18f)
        canvas.drawRoundRect(rect, s * 0.06f, s * 0.06f, paint)
        paint.style = Paint.Style.FILL
        val level = when {
            full -> 1f
            low -> 0.22f
            else -> 0.6f
        }
        rect.set(cx - s * 0.56f, cy - s * 0.28f, cx - s * 0.56f + s * 1.0f * level, cy + s * 0.28f)
        canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, paint)
    }

    private fun drawTorch(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        path.reset()
        path.moveTo(cx - s * 0.3f, cy - s * 0.7f)
        path.lineTo(cx + s * 0.3f, cy - s * 0.7f)
        path.lineTo(cx + s * 0.5f, cy + s * 0.1f)
        path.lineTo(cx - s * 0.5f, cy + s * 0.1f)
        path.close()
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        rect.set(cx - s * 0.22f, cy + s * 0.2f, cx + s * 0.22f, cy + s * 0.78f)
        canvas.drawRoundRect(rect, s * 0.1f, s * 0.1f, paint)
    }

    private fun drawTimer(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        canvas.drawCircle(cx, cy + s * 0.1f, s * 0.62f, paint)
        paint.style = Paint.Style.FILL
        rect.set(cx - s * 0.28f, cy - s * 0.85f, cx + s * 0.28f, cy - s * 0.55f)
        canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, paint)
        path.reset()
        path.moveTo(cx, cy + s * 0.1f)
        path.lineTo(cx, cy - s * 0.28f)
        canvas.drawPath(path, paint)
    }

    private fun drawVolume(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(cx - s * 0.8f, cy - s * 0.25f)
        path.lineTo(cx - s * 0.4f, cy - s * 0.25f)
        path.lineTo(cx - s * 0.05f, cy - s * 0.65f)
        path.lineTo(cx - s * 0.05f, cy + s * 0.65f)
        path.lineTo(cx - s * 0.4f, cy + s * 0.25f)
        path.lineTo(cx - s * 0.8f, cy + s * 0.25f)
        path.close()
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(RectF(cx - s * 0.15f, cy - s * 0.5f, cx + s * 0.85f, cy + s * 0.5f), -50f, 100f, false, paint)
    }

    private fun drawBrightness(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        canvas.drawCircle(cx, cy, s * 0.42f, paint)
        paint.style = Paint.Style.FILL
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val x1 = cx + Math.cos(angle).toFloat() * s * 0.62f
            val y1 = cy + Math.sin(angle).toFloat() * s * 0.62f
            val x2 = cx + Math.cos(angle).toFloat() * s * 0.9f
            val y2 = cy + Math.sin(angle).toFloat() * s * 0.9f
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }

    private fun drawMic(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.18f
        rect.set(cx - s * 0.3f, cy - s * 0.85f, cx + s * 0.3f, cy + s * 0.2f)
        canvas.drawRoundRect(rect, s * 0.3f, s * 0.3f, paint)
        canvas.drawArc(RectF(cx - s * 0.62f, cy - s * 0.4f, cx + s * 0.62f, cy + s * 0.7f), 0f, 180f, false, paint)
        canvas.drawLine(cx, cy + s * 0.45f, cx, cy + s * 0.85f, paint)
    }

    private fun drawCamera(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        rect.set(cx - s * 0.8f, cy - s * 0.45f, cx + s * 0.8f, cy + s * 0.5f)
        canvas.drawRoundRect(rect, s * 0.2f, s * 0.2f, paint)
        path.reset()
        path.moveTo(cx - s * 0.35f, cy - s * 0.45f)
        path.lineTo(cx - s * 0.15f, cy - s * 0.75f)
        path.lineTo(cx + s * 0.2f, cy - s * 0.75f)
        path.lineTo(cx + s * 0.4f, cy - s * 0.45f)
        canvas.drawPath(path, paint)
        canvas.drawCircle(cx, cy + s * 0.02f, s * 0.26f, paint)
    }

    private fun drawDnd(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        canvas.drawCircle(cx, cy, s * 0.72f, paint)
        canvas.drawLine(cx - s * 0.5f, cy - s * 0.5f, cx + s * 0.5f, cy + s * 0.5f, paint)
    }

    private fun drawUsb(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        paint.strokeCap = Paint.Cap.ROUND
        path.reset()
        path.moveTo(cx, cy - s * 0.8f)
        path.lineTo(cx, cy + s * 0.3f)
        rect.set(cx - s * 0.25f, cy + s * 0.3f, cx + s * 0.25f, cy + s * 0.8f)
        path.addArc(rect, 90f, 180f)
        path.moveTo(cx - s * 0.5f, cy - s * 0.2f)
        path.lineTo(cx - s * 0.2f, cy - s * 0.2f)
        path.lineTo(cx - s * 0.2f, cy + s * 0.1f)
        path.moveTo(cx + s * 0.5f, cy - s * 0.5f)
        path.lineTo(cx + s * 0.2f, cy - s * 0.5f)
        path.lineTo(cx + s * 0.2f, cy - s * 0.1f)
        canvas.drawPath(path, paint)
    }

    private fun drawSignal(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.FILL
        for (i in 0 until 4) {
            val h = s * (0.3f + i * 0.35f)
            rect.set(cx - s * 0.75f + i * s * 0.42f, cy + s * 0.7f - h, cx - s * 0.75f + i * s * 0.42f + s * 0.28f, cy + s * 0.7f)
            canvas.drawRoundRect(rect, s * 0.06f, s * 0.06f, paint)
        }
    }

    private fun drawAlarm(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        canvas.drawCircle(cx, cy + s * 0.1f, s * 0.6f, paint)
        path.reset()
        path.moveTo(cx - s * 0.45f, cy - s * 0.65f)
        path.lineTo(cx + s * 0.45f, cy - s * 0.65f)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx - s * 0.7f, cy - s * 0.35f, s * 0.18f, paint)
        canvas.drawCircle(cx + s * 0.7f, cy - s * 0.35f, s * 0.18f, paint)
    }

    private fun drawRotate(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        rect.set(cx - s * 0.6f, cy - s * 0.6f, cx + s * 0.6f, cy + s * 0.6f)
        canvas.drawArc(rect, 20f, 300f, false, paint)
        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(cx + s * 0.3f, cy - s * 0.8f)
        path.lineTo(cx + s * 0.85f, cy - s * 0.55f)
        path.lineTo(cx + s * 0.32f, cy - s * 0.3f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawRecord(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        canvas.drawCircle(cx, cy, s * 0.72f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, s * 0.3f, paint)
    }

    private fun drawShot(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        rect.set(cx - s * 0.75f, cy - s * 0.55f, cx + s * 0.75f, cy + s * 0.55f)
        canvas.drawRoundRect(rect, s * 0.16f, s * 0.16f, paint)
        canvas.drawCircle(cx, cy, s * 0.26f, paint)
    }

    private fun drawLeaf(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        path.reset()
        path.moveTo(cx - s * 0.55f, cy + s * 0.6f)
        path.cubicTo(cx - s * 0.65f, cy - s * 0.7f, cx + s * 0.65f, cy - s * 0.7f, cx + s * 0.5f, cy + s * 0.55f)
        path.cubicTo(cx + s * 0.1f, cy + s * 0.3f, cx - s * 0.2f, cy + s * 0.35f, cx - s * 0.55f, cy + s * 0.6f)
        canvas.drawPath(path, paint)
        canvas.drawLine(cx - s * 0.5f, cy + s * 0.62f, cx + s * 0.4f, cy - s * 0.35f, paint)
    }

    private fun drawApp(canvas: Canvas, cx: Float, cy: Float, s: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = s * 0.16f
        rect.set(cx - s * 0.62f, cy - s * 0.62f, cx + s * 0.62f, cy + s * 0.62f)
        canvas.drawRoundRect(rect, s * 0.24f, s * 0.24f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, s * 0.16f, paint)
    }
}
