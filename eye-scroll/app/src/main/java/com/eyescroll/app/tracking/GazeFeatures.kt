package com.eyescroll.app.tracking

import kotlin.math.hypot

/**
 * One processed camera frame. [features] holds the gaze signals described in [GazeFeatures];
 * it is null when no face was found.
 */
class FaceFrame(
    val timestampMs: Long,
    val features: FloatArray?,
    /** 0..1, how closed the eyes are (max of both eyes). */
    val blink: Float,
    /** Average eye width in pixels; used to tell the user to move closer. */
    val eyeWidthPx: Float,
    /** 0..1 average brightness of the frame, or -1 when not measured. */
    val brightness: Float,
) {
    val faceFound: Boolean get() = features != null

    companion object {
        fun noFace(timestampMs: Long, brightness: Float = -1f) =
            FaceFrame(timestampMs, null, 0f, 0f, brightness)
    }
}

/**
 * Turns MediaPipe face-mesh landmarks (478 points incl. iris) and blendshapes into a compact
 * vector of vertical-gaze signals. Several independent signals are combined because each one
 * alone is noisy on a phone camera:
 *
 *  0. iris offset    – iris centre relative to the eye-corner line, divided by eye width
 *  1. eye openness   – upper/lower lid gap divided by eye width (lids drop when looking down)
 *  2. blendshape     – MediaPipe's eyeLookDown - eyeLookUp scores
 *  3. head pitch     – nose position between forehead and chin (small nods)
 */
object GazeFeatures {
    const val COUNT = 4
    const val HEAD_FEATURE = 3

    // Face-mesh indices (image left/right; mirroring does not matter for vertical gaze).
    private const val L_OUTER = 33
    private const val L_INNER = 133
    private const val L_UPPER = 159
    private const val L_LOWER = 145
    private const val L_IRIS = 468
    private const val R_INNER = 362
    private const val R_OUTER = 263
    private const val R_UPPER = 386
    private const val R_LOWER = 374
    private const val R_IRIS = 473
    private const val FOREHEAD = 10
    private const val NOSE_TIP = 1
    private const val CHIN = 152

    const val LANDMARKS_WITH_IRIS = 478

    class Result(val features: FloatArray, val blink: Float, val eyeWidthPx: Float)

    /**
     * @param xs landmark x in pixels, [ys] landmark y in pixels (image is upright).
     * @param blend blendshape scores by category name, may be empty.
     */
    fun compute(xs: FloatArray, ys: FloatArray, blend: Map<String, Float>): Result? {
        if (xs.size < LANDMARKS_WITH_IRIS || ys.size < LANDMARKS_WITH_IRIS) return null

        fun eye(outer: Int, inner: Int, upper: Int, lower: Int, iris: Int): FloatArray? {
            val width = hypot(xs[outer] - xs[inner], ys[outer] - ys[inner])
            if (width < 1f) return null
            val cornerMidY = (ys[outer] + ys[inner]) / 2f
            val irisOffset = (ys[iris] - cornerMidY) / width
            val openness = (ys[lower] - ys[upper]) / width
            return floatArrayOf(irisOffset, openness, width)
        }

        val l = eye(L_OUTER, L_INNER, L_UPPER, L_LOWER, L_IRIS) ?: return null
        val r = eye(R_OUTER, R_INNER, R_UPPER, R_LOWER, R_IRIS) ?: return null

        val faceHeight = ys[CHIN] - ys[FOREHEAD]
        val pitch = if (faceHeight > 1f) (ys[NOSE_TIP] - ys[FOREHEAD]) / faceHeight else 0.5f

        val lookDown = ((blend["eyeLookDownLeft"] ?: 0f) + (blend["eyeLookDownRight"] ?: 0f)) / 2f
        val lookUp = ((blend["eyeLookUpLeft"] ?: 0f) + (blend["eyeLookUpRight"] ?: 0f)) / 2f
        val blink = maxOf(blend["eyeBlinkLeft"] ?: 0f, blend["eyeBlinkRight"] ?: 0f)

        val features = floatArrayOf(
            (l[0] + r[0]) / 2f,
            (l[1] + r[1]) / 2f,
            lookDown - lookUp,
            pitch,
        )
        return Result(features, blink, (l[2] + r[2]) / 2f)
    }
}
