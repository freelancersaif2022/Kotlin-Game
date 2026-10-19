package com.eyescroll.app.tracking

import kotlin.math.PI
import kotlin.math.abs

/**
 * One Euro filter (Casiez et al.): heavy smoothing while the gaze is steady, little lag when it
 * moves quickly. Removes camera jitter without making scrolling feel slow.
 */
class OneEuroFilter(
    private val minCutoff: Double = 1.2,
    private val beta: Double = 0.6,
    private val derivativeCutoff: Double = 1.0,
) {
    private var lastTimeMs = -1L
    private var x = 0.0
    private var dx = 0.0

    fun reset() {
        lastTimeMs = -1L
    }

    fun filter(value: Double, timeMs: Long): Double {
        if (lastTimeMs < 0) {
            lastTimeMs = timeMs
            x = value
            dx = 0.0
            return value
        }
        val dt = ((timeMs - lastTimeMs).coerceAtLeast(1)) / 1000.0
        lastTimeMs = timeMs
        val rawDx = (value - x) / dt
        dx += alpha(derivativeCutoff, dt) * (rawDx - dx)
        val cutoff = minCutoff + beta * abs(dx)
        x += alpha(cutoff, dt) * (value - x)
        return x
    }

    private fun alpha(cutoff: Double, dt: Double): Double {
        val tau = 1.0 / (2 * PI * cutoff)
        return 1.0 / (1.0 + tau / dt)
    }
}
