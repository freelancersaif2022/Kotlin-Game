package com.eyescroll.app.tracking

import kotlin.math.abs

enum class GazeZone { NONE, NEUTRAL, UP, DOWN }

enum class ScrollDirection { UP, DOWN }

/** What the tracker currently believes, normalised for display: 0 = top dot, 1 = bottom dot. */
data class GazeSnapshot(
    val faceFound: Boolean = false,
    val blinking: Boolean = false,
    val gazeY: Float = 0.5f,
    val upThreshold: Float = 0.25f,
    val downThreshold: Float = 0.75f,
    val zone: GazeZone = GazeZone.NONE,
    /** 0..1 progress towards the next scroll while looking at an edge. */
    val dwellProgress: Float = 0f,
)

/**
 * Turns the per-frame gaze estimate into scroll decisions:
 *  - smooths the signal (One Euro filter) and ignores blinks, which look like "looking down"
 *  - splits the screen into top / neutral / bottom zones with hysteresis
 *  - requires a short dwell before firing, then repeats while you keep looking
 *  - blocks an accidental opposite scroll right after one (eyes jump to new content)
 *  - slowly re-centres while you read normally, compensating for drift in phone/head position
 */
class GazeController(private val model: GazeModel, config: Config) {

    data class Config(
        val sensitivity: Float,
        val dwellMs: Int,
        val repeatMs: Int,
    )

    var config: Config = config
        set(value) {
            field = value
            computeThresholds()
        }

    private val filter = OneEuroFilter()
    private var upTh = 0.0
    private var downTh = 0.0
    private var bias = 0.0

    private var zone = GazeZone.NONE
    private var zoneSinceMs = 0L
    private var lastFireInZoneMs = 0L
    private var lastScrollDir: ScrollDirection? = null
    private var lastScrollMs = 0L
    private var lastBlinkMs = -10_000L
    private var lastFrameMs = -1L
    private var lastY = model.midY
    private var lastSnapshot = GazeSnapshot()

    init {
        computeThresholds()
    }

    private fun computeThresholds() {
        val s = config.sensitivity.coerceIn(0f, 1f)
        // s = 0.5 -> threshold halfway between the middle and edge dots.
        val factor = 0.78 - 0.56 * s
        upTh = model.midY - (model.midY - model.topY) * factor
        downTh = model.midY + (model.bottomY - model.midY) * factor
    }

    fun process(frame: FaceFrame, nowMs: Long): Pair<GazeSnapshot, ScrollDirection?> {
        val dtMs = if (lastFrameMs < 0) 0L else (nowMs - lastFrameMs).coerceIn(0, 500)
        lastFrameMs = nowMs

        val features = frame.features
        if (features == null) {
            enterZone(GazeZone.NONE, nowMs)
            filter.reset()
            return snapshot(faceFound = false, blinking = false, progress = 0f) to null
        }

        if (frame.blink > BLINK_THRESHOLD) lastBlinkMs = nowMs
        if (nowMs - lastBlinkMs < BLINK_RECOVERY_MS) {
            // Freeze during and just after a blink; keep zone timers paused.
            zoneSinceMs += dtMs
            if (lastFireInZoneMs != 0L) lastFireInZoneMs += dtMs
            return lastSnapshot.copy(faceFound = true, blinking = true) to null
        }

        val raw = model.predict(features)
        val y = filter.filter(raw - bias, nowMs)
        lastY = y

        val margin = HYSTERESIS * model.span
        val next = when (zone) {
            GazeZone.UP -> if (y < upTh + margin) GazeZone.UP else classify(y)
            GazeZone.DOWN -> if (y > downTh - margin) GazeZone.DOWN else classify(y)
            else -> classify(y)
        }
        enterZone(next, nowMs)

        var fired: ScrollDirection? = null
        var progress = 0f
        if (zone == GazeZone.UP || zone == GazeZone.DOWN) {
            val dir = if (zone == GazeZone.UP) ScrollDirection.UP else ScrollDirection.DOWN
            val blocked = lastScrollDir != null && dir != lastScrollDir &&
                nowMs - lastScrollMs < OPPOSITE_COOLDOWN_MS
            if (!blocked) {
                if (lastFireInZoneMs == 0L) {
                    val held = nowMs - zoneSinceMs
                    progress = (held.toFloat() / config.dwellMs).coerceIn(0f, 1f)
                    if (held >= config.dwellMs) fired = dir
                } else {
                    val since = nowMs - lastFireInZoneMs
                    progress = (since.toFloat() / config.repeatMs).coerceIn(0f, 1f)
                    if (since >= config.repeatMs) fired = dir
                }
            }
            if (fired != null) {
                lastFireInZoneMs = nowMs
                lastScrollDir = fired
                lastScrollMs = nowMs
                progress = 0f
            }
        } else if (zone == GazeZone.NEUTRAL && nowMs - lastScrollMs > 1500 && dtMs > 0) {
            // While reading, the average gaze sits around the middle; slowly track that.
            val alpha = dtMs / DRIFT_TIME_CONSTANT_MS
            val target = (raw - model.midY)
            bias += (target - bias) * alpha
            val limit = MAX_BIAS_FRACTION * model.span
            bias = bias.coerceIn(-limit, limit)
        }

        return snapshot(faceFound = true, blinking = false, progress = progress) to fired
    }

    private fun classify(y: Double): GazeZone = when {
        y < upTh -> GazeZone.UP
        y > downTh -> GazeZone.DOWN
        else -> GazeZone.NEUTRAL
    }

    private fun enterZone(next: GazeZone, nowMs: Long) {
        if (next != zone) {
            zone = next
            zoneSinceMs = nowMs
            lastFireInZoneMs = 0L
        }
    }

    private fun norm(v: Double): Float {
        val span = model.span
        if (abs(span) < 1e-9) return 0.5f
        return ((v - model.topY) / span).toFloat().coerceIn(-0.25f, 1.25f)
    }

    private fun snapshot(faceFound: Boolean, blinking: Boolean, progress: Float): GazeSnapshot {
        val s = GazeSnapshot(
            faceFound = faceFound,
            blinking = blinking,
            gazeY = norm(lastY),
            upThreshold = norm(upTh),
            downThreshold = norm(downTh),
            zone = zone,
            dwellProgress = progress,
        )
        lastSnapshot = s
        return s
    }

    companion object {
        const val BLINK_THRESHOLD = 0.42f
        const val BLINK_RECOVERY_MS = 160L
        const val HYSTERESIS = 0.05
        const val OPPOSITE_COOLDOWN_MS = 1100L
        const val DRIFT_TIME_CONSTANT_MS = 25_000.0
        const val MAX_BIAS_FRACTION = 0.18
    }
}
