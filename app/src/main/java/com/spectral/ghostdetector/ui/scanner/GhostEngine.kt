package com.spectral.ghostdetector.ui.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.spectral.ghostdetector.data.GhostFactory
import com.spectral.ghostdetector.data.GhostProfile
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

enum class Phase { LOCKING, TRACKING, VANISHING }

/**
 * One tracked (invisible) entity. Only its green tracking frame is drawn.
 * Coordinates are normalized: x of screen width, y of screen height; [w] is a
 * fraction of screen width and the frame height is w * [aspect] * width.
 */
class TrackedGhost(val id: Long, val profile: GhostProfile, x: Float, y: Float, val baseW: Float, val aspect: Float) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var w by mutableFloatStateOf(baseW)
    var alpha by mutableFloatStateOf(0f)
    var lock by mutableFloatStateOf(0f)      // 0..1 lock-on animation
    var time by mutableFloatStateOf(0f)      // seconds alive (drives inner animations)

    var phase = Phase.LOCKING
    val lifetime = Random.nextFloat() * 9f + 5f
    var targetX = x
    var targetY = y
    var speed = Random.nextFloat() * 0.9f + 0.5f
    var flickerTimer = 0f
    var alarmed = false
}

sealed interface EngineEvent {
    data class Spawned(val ghost: TrackedGhost) : EngineEvent
    data class Vanished(val ghost: TrackedGhost) : EngineEvent
    data class Warning(val text: String, val danger: Boolean) : EngineEvent
}

class GhostEngine(private val onEvent: (EngineEvent) -> Unit) {

    val ghosts = mutableStateListOf<TrackedGhost>()
    var detectedThisSession by mutableIntStateOf(0)
        private set

    var running = false
    private var spawnIn = 2.2f
    private var warnIn = 6f
    private var nextId = 1L

    private val ambientWarnings = listOf(
        "EMF FLUCTUATION DETECTED" to false,
        "COLD SPOT · TEMPERATURE DROPPING" to false,
        "UNEXPLAINED AUDIO FREQUENCY" to false,
        "MOVEMENT IN PERIPHERAL ZONE" to false,
        "SPECTRAL INTERFERENCE" to false,
    )

    fun reset() {
        ghosts.clear()
        spawnIn = 2.2f
        warnIn = 6f
    }

    fun update(dt: Float) {
        if (running) {
            spawnIn -= dt
            warnIn -= dt
            if (spawnIn <= 0f) {
                val cap = when (Random.nextInt(100)) {
                    in 0..54 -> 1
                    in 55..87 -> 2
                    else -> 3
                }
                if (ghosts.size < cap) spawn()
                spawnIn = Random.nextFloat() * 6f + 2.5f
            }
            if (warnIn <= 0f) {
                if (ghosts.isEmpty()) {
                    val (text, danger) = ambientWarnings.random()
                    onEvent(EngineEvent.Warning(text, danger))
                }
                warnIn = Random.nextFloat() * 10f + 8f
            }
        }

        val it = ghosts.iterator()
        val dead = ArrayList<TrackedGhost>()
        while (it.hasNext()) {
            val g = it.next()
            step(g, dt)
            if (g.phase == Phase.VANISHING && g.alpha <= 0.01f) dead += g
        }
        dead.forEach {
            ghosts.remove(it)
            onEvent(EngineEvent.Vanished(it))
        }
    }

    /** Stop scanning: all current entities fade out. */
    fun vanishAll() {
        ghosts.forEach { it.phase = Phase.VANISHING }
    }

    private fun spawn() {
        val profile = GhostFactory.create()
        val w = Random.nextFloat() * 0.18f + 0.26f
        val g = TrackedGhost(
            id = nextId++,
            profile = profile,
            x = Random.nextFloat() * 0.6f + 0.2f,
            y = Random.nextFloat() * 0.4f + 0.3f,
            baseW = w,
            aspect = Random.nextFloat() * 0.5f + 1.25f,
        )
        pickTarget(g)
        ghosts += g
        detectedThisSession++
        onEvent(EngineEvent.Spawned(g))
    }

    private fun pickTarget(g: TrackedGhost) {
        g.targetX = Random.nextFloat() * 0.6f + 0.2f
        g.targetY = Random.nextFloat() * 0.38f + 0.3f
        g.speed = Random.nextFloat() * 0.9f + 0.35f
    }

    private fun step(g: TrackedGhost, dt: Float) {
        g.time += dt
        // --- movement: smooth glide toward a wandering target with organic wobble ---
        val k = 1f - exp(-dt * g.speed)
        g.x += (g.targetX - g.x) * k + sin(g.time * 2.3f + g.id) * 0.0009f
        g.y += (g.targetY - g.y) * k + sin(g.time * 1.7f + g.id * 3) * 0.0008f
        if (hypot(g.targetX - g.x, g.targetY - g.y) < 0.03f || Random.nextFloat() < dt * 0.25f) pickTarget(g)
        // occasional sudden dart
        if (Random.nextFloat() < dt * 0.06f) {
            g.targetX = (g.x + (Random.nextFloat() - 0.5f) * 0.5f).coerceIn(0.18f, 0.82f)
            g.speed = 3.2f
        }
        g.w = g.baseW * (1f + 0.045f * sin(g.time * 2.1f))

        // --- visibility per phase ---
        when (g.phase) {
            Phase.LOCKING -> {
                g.lock = (g.lock + dt / 0.9f).coerceAtMost(1f)
                g.alpha = g.lock * (0.5f + Random.nextFloat() * 0.5f)
                if (g.lock >= 1f) g.phase = Phase.TRACKING
            }
            Phase.TRACKING -> {
                g.flickerTimer -= dt
                g.alpha = if (g.flickerTimer > 0f) {
                    Random.nextFloat() * 0.6f + 0.1f
                } else {
                    if (Random.nextFloat() < dt * 0.35f) g.flickerTimer = Random.nextFloat() * 0.25f + 0.05f
                    (g.alpha + dt * 4f).coerceAtMost(1f)
                }
                if (g.time > g.lifetime) g.phase = Phase.VANISHING
            }
            Phase.VANISHING -> {
                g.alpha = (g.alpha - dt * 1.4f).coerceAtLeast(0f) * (0.75f + Random.nextFloat() * 0.25f)
            }
        }
    }
}
