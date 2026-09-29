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
 *    fill simply materializes out of white;
 *  - "breathing": the whole field's intensity swells and recedes on a
 *    slow ~6s sine (sine is inherently ease-in-out, so it never snaps
 *    or pulses) over a very small range so it is barely noticeable;
 *  - "flowing": each static light pool and each wave drifts slowly on
 *    its own long period and phase, so the light feels like it is
 *    wandering through fog rather than looping mechanically.
 *
 * Cost: one ValueAnimator, a few shader fills per frame, zero
 * allocations in onDraw. The animation pauses when the window is not
 * visible.
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

    // Breathing: a full cycle is ~6.2s and the depth of the change is
    // only 8 percent of full intensity, so it reads as a calm swell.
    private val breathePeriodSec = 6.2f
    private val breatheDepth = 0.08f
    private val breatheBase = 1f - breatheDepth

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
    // flowAmpX / flowAmpY / flowPeriod give every layer its own slow
    // drift so the light flows organically instead of in lockstep.
    private val waves = arrayOf(
        Wave(1.60f, 0.055f, 0.980f, 0.30f, 26, 0.030f, 0.010f, 19f, 0.0f),
        Wave(1.05f, 0.070f, 1.040f, 0.48f, 20, 0.036f, 0.012f, 24f, 2.1f),
        Wave(0.72f, 0.045f, 0.930f, 0.66f, 14, 0.024f, 0.008f, 15f, 4.2f)
    )

    private var phase = 0f
    private var timeSec = 0f
    private var animator: ValueAnimator? = null

    private class Wave(
        val lenFrac: Float,
        val ampFrac: Float,
        val anchorFrac: Float,
        val speed: Float,
        val alpha: Int,
        val flowAmpX: Float,
        val flowAmpY: Float,
        val flowPeriodSec: Float,
        val flowPhase: Float
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (windowVisibility == VISIBLE) startWave()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startWave() else stopWave()
    }

    private fun stopWave() {
        animator?.cancel()
        animator = null
    }

    private fun startWave() {
        if (animator != null) return
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 16700L // repeat unit; phase is derived continuously
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        a.addUpdateListener { anim ->
            val v = anim.animatedValue as Float
            timeSec += 0.0167f
            phase = (2f * PI * v).toFloat()
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
        // atmosphere carries a subtle color variation. Their positions
        // are re-centered here; the slow drift is applied in onDraw as
        // a cheap canvas translate, never by rebuilding shaders.
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
        val twoPi = (2f * PI).toFloat()

        // Breathing: a slow sine over ~6s, mapped to a 0.92..1.0 alpha
        // multiplier. Sine is smooth ease-in-out by nature, so the
        // swell and recede blend seamlessly and never flash.
        val breathe = breatheBase + breatheDepth *
            (0.5f + 0.5f * sin(twoPi * timeSec / breathePeriodSec))
        val breatheAlpha = (255f * breathe).toInt()

        cornerPaint.alpha = breatheAlpha
        centerPaint.alpha = breatheAlpha
        cyanPaint.alpha = breatheAlpha
        for (p in wavePaints) p.alpha = breatheAlpha

        canvas.save()
        canvas.clipRect(0, (h * 0.34f).toInt(), w, h)

        // Flowing: each pool drifts on its own long period and phase.
        // Only cheap canvas translates - the gradients themselves are
        // built once in onSizeChanged.
        driftPool(canvas, centerPaint, fw, fh, 21f, 0.0f)
        driftPool(canvas, cyanPaint, fw, fh, 27f, 1.3f)
        driftPool(canvas, cornerPaint, fw, fh, 17f, 3.9f)
        canvas.restore()

        for (i in wavePaints.indices) {
            val wv = waves[i]
            val wavelength = fw * wv.lenFrac
            val amplitude = fh * wv.ampFrac
            val offset = phase * wv.speed

            // Slow vertical breathing of the anchor plus a gentle
            // horizontal wander of the whole wave body.
            val flowK = twoPi * timeSec / wv.flowPeriodSec + wv.flowPhase
            val anchor = fh * wv.anchorFrac +
                fh * wv.flowAmpY * sin(flowK)
            val wander = fw * wv.flowAmpX * sin(flowK * 0.6f + 1.1f)

            wavePath.reset()
            wavePath.moveTo(0f, h.toFloat())
            val steps = 32
            for (s in 0..steps) {
                val x = fw * s / steps + wander
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

    /**
     * Draws a static light pool translated by a very slow, small orbit.
     * The offsets are a few percent of the view size, so the pool looks
     * pinned to the bottom while softly wandering.
     */
    private fun driftPool(
        canvas: Canvas,
        paint: Paint,
        fw: Float,
        fh: Float,
        periodSec: Float,
        phaseOffset: Float
    ) {
        val twoPi = (2f * PI).toFloat()
        val k = twoPi * timeSec / periodSec + phaseOffset
        val dx = fw * 0.035f * sin(k)
        val dy = fh * 0.014f * sin(k * 0.8f + 0.7f)
        canvas.save()
        canvas.translate(dx, dy)
        canvas.drawRect(-fw * 0.5f, 0f, fw * 1.5f, fh * 2f, paint)
        canvas.restore()
    }
}
