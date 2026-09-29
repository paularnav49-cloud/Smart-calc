package com.arnavpaul.smartcalc

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ambient bottom glow for the AI chat screen.
 *
 * Two layers:
 *  1. Static atmospheric base - soft radial glows rising from the bottom
 *     corners and center, fading to transparent by mid-screen so the
 *     canvas stays white.
 *  2. Animated waves - four slow, overlapping sine layers drifting
 *     horizontally near the bottom. Each is a cheap filled Path with a
 *     vertical gradient alpha so edges dissolve instead of cutting off.
 *     No blur masks, no allocations in onDraw.
 */
class AmbientGlowView(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaints = arrayOf(
        Paint(Paint.ANTI_ALIAS_FLAG),
        Paint(Paint.ANTI_ALIAS_FLAG),
        Paint(Paint.ANTI_ALIAS_FLAG),
        Paint(Paint.ANTI_ALIAS_FLAG)
    )
    private val wavePath = Path()

    // Per-layer character: wavelength (fraction of width), amplitude
    // (fraction of height), vertical anchor (fraction of height),
    // phase speed, and alpha. Layered variety keeps motion organic.
    private val waves = arrayOf(
        Wave(1.30f, 0.020f, 0.88f, 0.55f, 34, 0.980f),
        Wave(0.95f, 0.028f, 0.93f, 0.85f, 44, 0.975f),
        Wave(0.65f, 0.016f, 0.97f, 0.70f, 26, 0.988f),
        Wave(0.48f, 0.032f, 1.00f, 0.42f, 52, 0.970f)
    )

    private var phase = 0f
    private var animator: ValueAnimator? = null

    private class Wave(
        val lenFrac: Float,
        val ampFrac: Float,
        val anchorFrac: Float,
        val speed: Float,
        val alpha: Int,
        val topFade: Float
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startWave()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private fun startWave() {
        if (animator != null) return
        // One slow loop; phase advances continuously and each layer
        // moves at its own rate. ~20s base cycle = calm, premium drift.
        val a = ValueAnimator.ofFloat(0f, (2f * PI).toFloat())
        a.duration = 20000L
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        a.addUpdateListener { anim ->
            phase = anim.animatedValue as Float
            invalidate()
        }
        a.start()
        animator = a
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val fw = w.toFloat()
        val fh = h.toFloat()

        // Richer soft blue radiating up from the bottom corners.
        cornerPaint.shader = RadialGradient(
            fw * 0.10f, fh * 1.05f, fw * 0.95f,
            Color.argb(96, 120, 165, 250),
            Color.argb(0, 120, 165, 250),
            Shader.TileMode.CLAMP
        )
        // Central pool of light behind the input bar.
        centerPaint.shader = RadialGradient(
            fw * 0.5f, fh * 1.08f, fw * 1.10f,
            Color.argb(112, 100, 150, 248),
            Color.argb(0, 100, 150, 248),
            Shader.TileMode.CLAMP
        )

        // Each wave fades vertically into the white canvas, so there is
        // never a hard top edge - the fill dissolves upward.
        for (i in wavePaints.indices) {
            val wv = waves[i]
            val anchor = fh * wv.anchorFrac
            val top = fh * (wv.anchorFrac - 0.34f) * wv.topFade
            wavePaints[i].shader = LinearGradient(
                0f, top, 0f, anchor + fh * 0.10f,
                Color.argb(0, 118, 160, 248),
                Color.argb(wv.alpha, 118, 160, 248),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        // Static atmosphere, restricted to the lower region.
        canvas.save()
        canvas.clipRect(0, (h * 0.40f).toInt(), w, h)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), cornerPaint)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), centerPaint)
        canvas.restore()

        // Animated flowing waves.
        val fw = w.toFloat()
        val fh = h.toFloat()
        val twoPi = (2f * PI).toFloat()
        for (i in wavePaints.indices) {
            val wv = waves[i]
            val wavelength = fw * wv.lenFrac
            val amplitude = fh * wv.ampFrac
            val anchor = fh * wv.anchorFrac
            val offset = phase * wv.speed

            wavePath.reset()
            wavePath.moveTo(0f, h.toFloat())
            val steps = 36
            for (s in 0..steps) {
                val x = fw * s / steps
                val k = (x + offset * wavelength) / wavelength * twoPi
                val y = anchor + amplitude * sin(k) + amplitude * 0.45f * sin(k * 0.5f + offset)
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(fw, h.toFloat())
            wavePath.close()
            canvas.drawPath(wavePath, wavePaints[i])
        }
    }
}