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
 * Ambient bottom light field for the AI chat screen.
 *
 * Design goals:
 *  - one continuous field of soft blue light, never visible layers;
 *  - all moving shapes share the same hue, so overlaps merge;
 *  - each wave's vertical gradient starts at alpha 0 exactly at its
 *    highest possible crest, so the path boundary is invisible - the
 *    fill simply materializes out of white. That removes the banded
 *    "layered wave" look;
 *  - three broad, slow sine fields drift and morph at different speeds,
 *    reading as light breathing through fog, not ocean water.
 *
 * Cost: one ValueAnimator, three small path fills per frame, zero
 * allocations in onDraw.
 */
class AmbientGlowView(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    // One shared hue for everything - static base and waves - so the
    // whole bottom reads as a single atmosphere.
    private val blueR = 118
    private val blueG = 160
    private val blueB = 250
    private val cyanR = 125
    private val cyanG = 200
    private val cyanB = 240

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaints = arrayOf(
        Paint(Paint.ANTI_ALIAS_FLAG),
        Paint(Paint.ANTI_ALIAS_FLAG),
        Paint(Paint.ANTI_ALIAS_FLAG)
    )
    private val wavePath = Path()

    // Broad organic fields. amplitude is large relative to the gradient
    // span so the visible body of each wave is wide and diffused.
    private val waves = arrayOf(
        Wave(1.60f, 0.055f, 0.980f, 0.30f, 26),
        Wave(1.05f, 0.070f, 1.040f, 0.48f, 20),
        Wave(0.72f, 0.045f, 0.930f, 0.66f, 14)
    )

    private var phase = 0f
    private var animator: ValueAnimator? = null

    private class Wave(
        val lenFrac: Float,
        val ampFrac: Float,
        val anchorFrac: Float,
        val speed: Float,
        val alpha: Int
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
        val a = ValueAnimator.ofFloat(0f, (2f * PI).toFloat())
        a.duration = 28000L
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

        // Richer static base: one wide central pool plus two offset
        // side fields (blue on the left, cyan on the right) so the
        // atmosphere carries a subtle color variation.
        cornerPaint.shader = RadialGradient(
            fw * 0.5f, fh * 1.12f, fw * 1.35f,
            Color.argb(115, blueR, blueG, blueB),
            Color.argb(0, blueR, blueG, blueB),
            Shader.TileMode.CLAMP
        )
        centerPaint.shader = RadialGradient(
            fw * 0.28f, fh * 1.04f, fw * 0.90f,
            Color.argb(80, blueR, blueG, blueB),
            Color.argb(0, blueR, blueG, blueB),
            Shader.TileMode.CLAMP
        )
        cyanPaint.shader = RadialGradient(
            fw * 0.76f, fh * 1.04f, fw * 0.90f,
            Color.argb(66, cyanR, cyanG, cyanB),
            Color.argb(0, cyanR, cyanG, cyanB),
            Shader.TileMode.CLAMP
        )

        // Wave gradients: alpha 0 begins exactly at the crest height
        // (anchor - amplitude) and reaches full alpha below the trough.
        // The path outline therefore sits where the fill is already
        // transparent - no edge is ever rendered.
        for (i in wavePaints.indices) {
            val wv = waves[i]
            val anchor = fh * wv.anchorFrac
            val amp = fh * wv.ampFrac
            val crest = anchor - amp
            val full = anchor + amp * 1.6f
            wavePaints[i].shader = LinearGradient(
                0f, crest, 0f, full,
                Color.argb(0, blueR, blueG, blueB),
                Color.argb(wv.alpha, blueR, blueG, blueB),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        val fw = w.toFloat()
        val fh = h.toFloat()

        canvas.save()
        canvas.clipRect(0, (h * 0.34f).toInt(), w, h)
        canvas.drawRect(0f, 0f, fw, fh, centerPaint)
        canvas.drawRect(0f, 0f, fw, fh, cyanPaint)
        canvas.drawRect(0f, 0f, fw, fh, cornerPaint)
        canvas.restore()

        val twoPi = (2f * PI).toFloat()
        for (i in wavePaints.indices) {
            val wv = waves[i]
            val wavelength = fw * wv.lenFrac
            val amplitude = fh * wv.ampFrac
            val anchor = fh * wv.anchorFrac
            val offset = phase * wv.speed

            wavePath.reset()
            wavePath.moveTo(0f, h.toFloat())
            val steps = 32
            for (s in 0..steps) {
                val x = fw * s / steps
                val k = (x + offset * wavelength) / wavelength * twoPi
                // Two slowly beating sines give organic morphing rather
                // than a rigid traveling wave.
                val y = anchor + amplitude * (0.6f * sin(k) + 0.4f * sin(k * 0.37f + offset * 1.7f))
                wavePath.lineTo(x, y)
            }
            wavePath.lineTo(fw, h.toFloat())
            wavePath.close()
            canvas.drawPath(wavePath, wavePaints[i])
        }
    }
}