package com.spectral.ghostdetector.ui.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Simulated sensor readings shown on the HUD (EMF is blended with the real magnetometer). */
class HudState {
    var emf by mutableFloatStateOf(0.3f)
        private set
    var temp by mutableFloatStateOf(21.3f)
        private set
    var freq by mutableFloatStateOf(42f)
        private set
    /** 0..1 overall paranormal activity. */
    var activity by mutableFloatStateOf(0f)
        private set
    /** Increments every frame – read inside Canvas to redraw per frame. */
    var frame by mutableIntStateOf(0)
        private set

    private val wave = FloatArray(110)
    private var head = 0
    private var acc = 0f
    private var t = 0f

    val waveSize get() = wave.size
    fun waveAt(i: Int): Float = wave[(head + i) % wave.size]

    fun update(dt: Float, engine: GhostEngine, mag: MagnetometerReader, scanning: Boolean) {
        t += dt
        val power = engine.ghosts.fold(0f) { s, g -> s + g.alpha * g.profile.threat }
        val k = 1f - exp(-dt * 3f)
        activity += ((power / 5f).coerceIn(0f, 1f) - activity) * k

        val base = if (scanning) 0.10f else 0.04f
        val amp = base + activity * 0.85f
        wave[head] = (Random.nextFloat() * 2f - 1f) * amp * (0.55f + 0.45f * sin(t * 9f))
        head = (head + 1) % wave.size
        frame++

        acc += dt
        if (acc >= 0.12f) {
            acc = 0f
            val real = mag.deviationMg.coerceAtMost(60f) * 0.5f
            val emfTarget = 0.2f + Random.nextFloat() * 0.6f + real + power * 9f * (0.8f + Random.nextFloat() * 0.4f)
            emf += (emfTarget - emf) * 0.4f
            val tempTarget = 21.3f - power * 1.6f + (Random.nextFloat() - 0.5f) * 0.25f
            temp += (tempTarget - temp) * 0.15f
            val fTarget = if (power > 0.2f) 280f + power * 120f + Random.nextFloat() * 60f else 38f + Random.nextFloat() * 14f
            freq += (fTarget - freq) * 0.3f
        }
    }
}
