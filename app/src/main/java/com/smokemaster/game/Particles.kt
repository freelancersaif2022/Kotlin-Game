package com.smokemaster.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.pow

/**
 * Allocation-free particle pool. Each particle draws one of [images] (or an
 * oval when the image index is negative) and grows from s0 to s1 while fading.
 */
class Particles(private val cap: Int, private val paint: Paint) {
    private val x = FloatArray(cap)
    private val y = FloatArray(cap)
    private val vx = FloatArray(cap)
    private val vy = FloatArray(cap)
    private val age = FloatArray(cap)
    private val life = FloatArray(cap)
    private val s0 = FloatArray(cap)
    private val s1 = FloatArray(cap)
    private val a0 = FloatArray(cap)
    private val rot = FloatArray(cap)
    private val vrot = FloatArray(cap)
    private val drag = FloatArray(cap)
    private val grav = FloatArray(cap)
    private val img = IntArray(cap)
    private val m = Matrix()
    var count = 0
        private set

    fun spawn(
        image: Int, px: Float, py: Float, pvx: Float, pvy: Float, plife: Float,
        size0: Float, size1: Float, alpha: Float,
        rotation: Float = rnd(0f, 360f), spin: Float = 0f, pdrag: Float = 0f, gravity: Float = 0f,
    ) {
        val i = if (count < cap) count++ else RANDOM.nextInt(cap)
        x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy; age[i] = 0f; life[i] = plife
        s0[i] = size0; s1[i] = size1; a0[i] = alpha; rot[i] = rotation; vrot[i] = spin
        drag[i] = pdrag; grav[i] = gravity; img[i] = image
    }

    fun update(dt: Float) {
        var i = 0
        while (i < count) {
            age[i] += dt
            if (age[i] >= life[i]) {
                val last = --count
                if (i != last) copy(last, i)
                continue
            }
            if (drag[i] != 0f) {
                val k = 1f / (1f + drag[i] * dt)
                vx[i] *= k
                vy[i] *= k
            }
            vy[i] += grav[i] * dt
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            rot[i] += vrot[i] * dt
            i++
        }
    }

    private fun copy(from: Int, to: Int) {
        x[to] = x[from]; y[to] = y[from]; vx[to] = vx[from]; vy[to] = vy[from]
        age[to] = age[from]; life[to] = life[from]; s0[to] = s0[from]; s1[to] = s1[from]
        a0[to] = a0[from]; rot[to] = rot[from]; vrot[to] = vrot[from]; drag[to] = drag[from]
        grav[to] = grav[from]; img[to] = img[from]
    }

    fun draw(c: Canvas, images: Array<Bitmap>, alphaScale: Float = 1f) {
        for (i in 0 until count) {
            val p = age[i] / life[i]
            val fadeIn = if (p < 0.12f) p / 0.12f else 1f
            val a = a0[i] * fadeIn * (1f - p).pow(1.3f) * alphaScale
            if (a <= 0.004f) continue
            val size = s0[i] + (s1[i] - s0[i]) * Ease.outCubic(p)
            paint.alpha = (a * 255).toInt().coerceIn(0, 255)
            val im = img[i]
            if (im < 0) {
                val h = size * 0.5f
                c.drawOval(x[i] - h * 0.7f, y[i] - h, x[i] + h * 0.7f, y[i] + h, paint)
            } else {
                val b = images[im]
                val bw = b.width.toFloat()
                val bh = b.height.toFloat()
                m.setTranslate(-bw / 2, -bh / 2)
                val sc = size / bw
                m.postScale(sc, sc)
                m.postRotate(rot[i])
                m.postTranslate(x[i], y[i])
                c.drawBitmap(b, m, paint)
            }
        }
        paint.alpha = 255
    }

    fun clear() { count = 0 }
}
