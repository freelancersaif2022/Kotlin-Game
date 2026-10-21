package com.freelancersaif.snakeattack

import com.badlogic.gdx.math.CatmullRomSpline
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

/**
 * Arc-length parameterised poly-line. Distance 0 is the far (off-screen right)
 * start, [length] is the end of the road next to the girl.
 */
class Path(points: List<Vector2>) {
    private val xs: FloatArray
    private val ys: FloatArray
    private val dist: FloatArray
    val length: Float

    init {
        val ctrl = ArrayList<Vector2>()
        ctrl += points.first()
        ctrl += points
        ctrl += points.last()
        val spline = CatmullRomSpline(ctrl.toTypedArray(), false)
        val n = 1200
        xs = FloatArray(n + 1)
        ys = FloatArray(n + 1)
        dist = FloatArray(n + 1)
        val v = Vector2()
        for (i in 0..n) {
            spline.valueAt(v, i / n.toFloat())
            xs[i] = v.x
            ys[i] = v.y
            if (i > 0) dist[i] = dist[i - 1] + Vector2.dst(xs[i - 1], ys[i - 1], v.x, v.y)
        }
        length = dist[n]
    }

    private fun index(s: Float): Int {
        var lo = 0
        var hi = dist.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (dist[mid] <= s) lo = mid else hi = mid
        }
        return lo
    }

    fun pos(s: Float, out: Vector2): Vector2 {
        if (s <= 0f) {
            // extrapolate straight back beyond the start
            val dx = xs[1] - xs[0]
            val dy = ys[1] - ys[0]
            val l = Vector2.len(dx, dy)
            return out.set(xs[0] + dx / l * s, ys[0] + dy / l * s)
        }
        if (s >= length) return out.set(xs.last(), ys.last())
        val i = index(s)
        val t = (s - dist[i]) / (dist[i + 1] - dist[i])
        return out.set(MathUtils.lerp(xs[i], xs[i + 1], t), MathUtils.lerp(ys[i], ys[i + 1], t))
    }

    private val ta = Vector2()
    private val tb = Vector2()

    /** Angle (degrees) of the direction pointing *backwards* along the path (towards the start). */
    fun backAngle(s: Float): Float {
        pos(s - 6f, ta)
        pos(s + 6f, tb)
        return MathUtils.atan2(ta.y - tb.y, ta.x - tb.x) * MathUtils.radiansToDegrees
    }
}
