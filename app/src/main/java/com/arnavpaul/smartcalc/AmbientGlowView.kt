package com.arnavpaul.smartcalc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Soft ambient blue glow anchored to the bottom of the AI chat screen.
 * Layered radial gradients rise from the bottom corners and center,
 * blended with a vertical fade so nothing reads as a hard band.
 * Purely decorative: draws nothing above ~42% screen height.
 */
class AmbientGlowView(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    private val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val fw = w.toFloat()
        val fh = h.toFloat()

        // Soft blue rays rising from the bottom corners.
        // Centers sit slightly below the bottom edge so only the diffused
        // upper half of each radial is visible on screen.
        val cornerRadius = fw * 0.85f
        leftPaint.shader = RadialGradient(
            fw * 0.08f, fh * 1.06f, cornerRadius,
            Color.argb(64, 150, 185, 245),
            Color.argb(0, 150, 185, 245),
            Shader.TileMode.CLAMP
        )
        rightPaint.shader = RadialGradient(
            fw * 0.92f, fh * 1.06f, cornerRadius,
            Color.argb(64, 150, 185, 245),
            Color.argb(0, 150, 185, 245),
            Shader.TileMode.CLAMP
        )

        // Central glow behind the input bar, slightly brighter.
        val centerRadius = fw * 1.05f
        centerPaint.shader = RadialGradient(
            fw * 0.5f, fh * 1.10f, centerRadius,
            Color.argb(82, 130, 165, 245),
            Color.argb(0, 130, 165, 245),
            Shader.TileMode.CLAMP
        )

        // A whisper of cyan for the atmospheric tint.
        cyanPaint.shader = RadialGradient(
            fw * 0.5f, fh * 1.02f, fw * 0.70f,
            Color.argb(42, 120, 205, 235),
            Color.argb(0, 120, 205, 235),
            Shader.TileMode.CLAMP
        )

        // Vertical wash strongest at the very bottom, gone by mid-screen.
        fadePaint.shader = LinearGradient(
            0f, fh, 0f, fh * 0.42f,
            Color.argb(46, 165, 195, 250),
            Color.argb(0, 165, 195, 250),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height
        if (h <= 0) return
        val top = (h * 0.42f).toInt()
        canvas.save()
        canvas.clipRect(0, top, width, h)
        canvas.drawRect(0f, 0f, width.toFloat(), h.toFloat(), leftPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), h.toFloat(), rightPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), h.toFloat(), centerPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), h.toFloat(), cyanPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), h.toFloat(), fadePaint)
        canvas.restore()
    }
}