package com.eyescroll.app.gesture

import kotlin.math.abs

/** DOWN = reveal content further down the page. */
enum class ScrollDirection { UP, DOWN }

enum class GestureMode { SWIPE, HOLD }

enum class HandZone { NONE, CALIBRATING, NEUTRAL, UP, DOWN }

/**
 * One analysed camera frame. Coordinates are normalised to the upright image:
 * x, y in 0..1 (y grows downwards); [handSize] is wrist → middle-finger base in the same
 * units as y, so thresholds scale with distance from the camera.
 */
class HandFrame(
    val timestampMs: Long,
    val found: Boolean,
    val tipX: Float = 0f,
    val tipY: Float = 0f,
    val handSize: Float = 0f,
    /** 0..1 frame brightness, or -1 when not measured. */
    val brightness: Float = -1f,
) {
    companion object {
        fun none(timestampMs: Long, brightness: Float = -1f) = HandFrame(timestampMs, false, brightness = brightness)
    }
}

/** What the detector currently sees, for the UI and the floating dot. */
data class HandSnapshot(
    val handFound: Boolean = false,
    val tipX: Float = 0.5f,
    val tipY: Float = 0.5f,
    /** Hold mode: resting finger height, or -1 while unknown. */
    val neutralY: Float = -1f,
    /** Hold mode: finger offset from rest in hand sizes (negative = above). Swipe mode: current movement. */
    val offset: Float = 0f,
    /** Distance (hand sizes) that counts as a gesture. */
    val threshold: Float = 1f,
    val zone: HandZone = HandZone.NONE,
    /** 0..1 progress towards the next scroll (hold mode). */
    val progress: Float = 0f,
)

/**
 * Turns fingertip positions into scroll commands.
 *
 * SWIPE: a quick up/down movement of the finger (≈1 hand-length within 0.45 s) fires one scroll.
 *        The natural return movement right after is ignored, so it never scrolls back.
 * HOLD:  the resting finger height is learned when the hand appears; holding the finger above
 *        or below it keeps scrolling in that direction until it comes back to rest.
 */
class GestureDetector(config: Config) {

    data class Config(
        val mode: GestureMode,
        /** Swipe: hand up = page moves up (like a touchscreen). Hold: finger up = scroll up. */
        val natural: Boolean,
        /** 0..1, higher = smaller movement needed. */
        val sensitivity: Float,
        /** Hold mode repeat interval. */
        val repeatMs: Int,
    )

    var config: Config = config
        set(value) {
            if (value.mode != field.mode) reset()
            field = value
        }

    // smoothing
    private var smoothY = -1f
    private var smoothX = -1f
    private var smoothSize = -1f

    // swipe state
    private val historyT = LongArray(HISTORY)
    private val historyY = FloatArray(HISTORY)
    private var historyCount = 0
    private var historyStart = 0
    private var lastFireMs = -10_000L
    private var lastMotionUp: Boolean? = null

    // hold state
    private var neutralY = -1f
    private var calibStartMs = -1L
    private var calibSum = 0f
    private var calibN = 0
    private var zone = HandZone.NONE
    private var zoneSinceMs = 0L
    private var lastRepeatMs = 0L
    private var lastFrameMs = -1L
    private var lostSinceMs = -1L

    fun reset() {
        smoothY = -1f
        smoothX = -1f
        smoothSize = -1f
        historyCount = 0
        historyStart = 0
        neutralY = -1f
        calibStartMs = -1L
        zone = HandZone.NONE
    }

    fun process(f: HandFrame, nowMs: Long): Pair<HandSnapshot, ScrollDirection?> {
        val dtMs = if (lastFrameMs < 0) 0L else (nowMs - lastFrameMs).coerceIn(0, 500)
        lastFrameMs = nowMs
        val s = config.sensitivity.coerceIn(0f, 1f)

        if (!f.found || f.handSize <= 0f) {
            if (lostSinceMs < 0) lostSinceMs = nowMs
            historyCount = 0
            smoothY = -1f
            // Briefly losing the hand (motion blur in a fast swipe) keeps the rest position.
            if (nowMs - lostSinceMs > LOST_RESET_MS) {
                neutralY = -1f
                calibStartMs = -1L
            }
            zone = HandZone.NONE
            return HandSnapshot(handFound = false, threshold = threshold(s)) to null
        }
        lostSinceMs = -1L

        smoothY = if (smoothY < 0) f.tipY else smoothY + (f.tipY - smoothY) * SMOOTHING
        smoothX = if (smoothX < 0) f.tipX else smoothX + (f.tipX - smoothX) * SMOOTHING
        smoothSize = if (smoothSize < 0) f.handSize else smoothSize + (f.handSize - smoothSize) * 0.2f
        val size = smoothSize.coerceAtLeast(0.02f)

        return when (config.mode) {
            GestureMode.SWIPE -> swipe(nowMs, size, s)
            GestureMode.HOLD -> hold(nowMs, dtMs, size, s)
        }
    }

    private fun threshold(s: Float) = when (config.mode) {
        GestureMode.SWIPE -> 1.5f - 1.0f * s
        GestureMode.HOLD -> 0.95f - 0.55f * s
    }

    private fun swipe(nowMs: Long, size: Float, s: Float): Pair<HandSnapshot, ScrollDirection?> {
        push(nowMs, smoothY)
        val th = threshold(s)
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until historyCount) {
            val idx = (historyStart + i) % HISTORY
            if (nowMs - historyT[idx] > SWIPE_WINDOW_MS) continue
            minY = minOf(minY, historyY[idx])
            maxY = maxOf(maxY, historyY[idx])
        }
        val movedUp = (maxY - smoothY) / size     // finger travelled upwards (y decreases)
        val movedDown = (smoothY - minY) / size
        val absMin = MIN_ABS_TRAVEL

        var motionUp: Boolean? = null
        if (movedUp >= th && maxY - smoothY >= absMin) motionUp = true
        else if (movedDown >= th && smoothY - minY >= absMin) motionUp = false

        var fired: ScrollDirection? = null
        if (motionUp != null && nowMs - lastFireMs >= SWIPE_COOLDOWN_MS) {
            val isReturnStroke = lastMotionUp != null && motionUp != lastMotionUp && nowMs - lastFireMs < RETURN_IGNORE_MS
            if (!isReturnStroke) {
                fired = if (motionUp == config.natural) ScrollDirection.DOWN else ScrollDirection.UP
                lastFireMs = nowMs
                lastMotionUp = motionUp
            }
            historyCount = 0
        }
        val current = if (movedUp > movedDown) -movedUp else movedDown
        val z = when {
            fired != null && motionUp == true -> HandZone.UP
            fired != null -> HandZone.DOWN
            else -> HandZone.NEUTRAL
        }
        return HandSnapshot(true, smoothX, smoothY, -1f, current, th, z, 0f) to fired
    }

    private fun hold(nowMs: Long, dtMs: Long, size: Float, s: Float): Pair<HandSnapshot, ScrollDirection?> {
        val th = threshold(s)
        if (neutralY < 0) {
            if (calibStartMs < 0) {
                calibStartMs = nowMs
                calibSum = 0f
                calibN = 0
            }
            calibSum += smoothY
            calibN++
            if (nowMs - calibStartMs >= HOLD_CALIBRATION_MS && calibN >= 3) {
                neutralY = calibSum / calibN
                zone = HandZone.NEUTRAL
                zoneSinceMs = nowMs
            } else {
                val p = ((nowMs - calibStartMs).toFloat() / HOLD_CALIBRATION_MS).coerceIn(0f, 1f)
                return HandSnapshot(true, smoothX, smoothY, -1f, 0f, th, HandZone.CALIBRATING, p) to null
            }
        }

        val offset = (smoothY - neutralY) / size
        val next = when (zone) {
            HandZone.UP -> if (offset < -(th - HYSTERESIS)) HandZone.UP else classify(offset, th)
            HandZone.DOWN -> if (offset > th - HYSTERESIS) HandZone.DOWN else classify(offset, th)
            else -> classify(offset, th)
        }
        if (next != zone) {
            zone = next
            zoneSinceMs = nowMs
            lastRepeatMs = 0L
        }

        var fired: ScrollDirection? = null
        var progress = 0f
        if (zone == HandZone.UP || zone == HandZone.DOWN) {
            val fingerUp = zone == HandZone.UP
            val dir = if (fingerUp == config.natural) ScrollDirection.UP else ScrollDirection.DOWN
            if (lastRepeatMs == 0L) {
                progress = ((nowMs - zoneSinceMs).toFloat() / HOLD_DWELL_MS).coerceIn(0f, 1f)
                if (nowMs - zoneSinceMs >= HOLD_DWELL_MS) fired = dir
            } else {
                progress = ((nowMs - lastRepeatMs).toFloat() / config.repeatMs).coerceIn(0f, 1f)
                if (nowMs - lastRepeatMs >= config.repeatMs) fired = dir
            }
            if (fired != null) {
                lastRepeatMs = nowMs
                progress = 0f
            }
        } else if (dtMs > 0) {
            // Slowly follow the resting position so a drifting arm doesn't cause scrolls.
            neutralY += (smoothY - neutralY) * (dtMs / NEUTRAL_FOLLOW_MS)
        }
        return HandSnapshot(true, smoothX, smoothY, neutralY, offset, th, zone, progress) to fired
    }

    private fun classify(offset: Float, th: Float) = when {
        offset < -th -> HandZone.UP
        offset > th -> HandZone.DOWN
        else -> HandZone.NEUTRAL
    }

    private fun push(t: Long, y: Float) {
        if (historyCount == HISTORY) {
            historyStart = (historyStart + 1) % HISTORY
            historyCount--
        }
        val idx = (historyStart + historyCount) % HISTORY
        historyT[idx] = t
        historyY[idx] = y
        historyCount++
    }

    companion object {
        private const val HISTORY = 32
        private const val SMOOTHING = 0.65f
        const val SWIPE_WINDOW_MS = 450L
        const val SWIPE_COOLDOWN_MS = 380L
        const val RETURN_IGNORE_MS = 1000L
        private const val MIN_ABS_TRAVEL = 0.05f
        const val HOLD_CALIBRATION_MS = 500L
        const val HOLD_DWELL_MS = 220L
        private const val HYSTERESIS = 0.15f
        private const val NEUTRAL_FOLLOW_MS = 8000f
        private const val LOST_RESET_MS = 1200L
    }
}
