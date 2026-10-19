package com.eyescroll.app.tracking

import kotlin.math.abs
import kotlin.math.sqrt

/** A calibration sample: gaze features recorded while the user looked at screen height [targetY] (0 = top, 1 = bottom). */
class CalibrationSample(val targetY: Float, val features: FloatArray)

enum class CalibrationQuality(val label: String) { EXCELLENT("Excellent"), GOOD("Good"), FAIR("Fair"), POOR("Poor") }

/**
 * Personal gaze model learned from the 3-dot calibration. Features are standardised and a ridge
 * regression maps them onto screen height, so the strongest signals for this particular face,
 * phone position and lighting get the most weight automatically.
 */
class GazeModel(
    private val indices: IntArray,
    private val mean: DoubleArray,
    private val std: DoubleArray,
    private val weights: DoubleArray,
    private val intercept: Double,
    /** Mean prediction while looking at the top, middle and bottom dots. */
    val topY: Double,
    val midY: Double,
    val bottomY: Double,
    /** Pooled standard deviation of predictions around each dot. */
    val noise: Double,
) {
    val span: Double get() = bottomY - topY

    /** How clearly up and down can be told apart (span relative to jitter). */
    val separation: Double get() = if (noise <= 1e-9) 99.0 else span / (4.0 * noise)

    val quality: CalibrationQuality
        get() = when {
            !isUsable -> CalibrationQuality.POOR
            separation >= 1.4 -> CalibrationQuality.EXCELLENT
            separation >= 0.9 -> CalibrationQuality.GOOD
            else -> CalibrationQuality.FAIR
        }

    val isUsable: Boolean get() = topY < midY && midY < bottomY && span > 0.25 && separation >= 0.5

    fun predict(features: FloatArray): Double {
        var y = intercept
        for (j in indices.indices) {
            y += weights[j] * ((features[indices[j]] - mean[j]) / std[j])
        }
        return y
    }

    companion object {
        const val TOP = 0f
        const val MIDDLE = 0.5f
        const val BOTTOM = 1f
        private const val MIN_SAMPLES_PER_TARGET = 8

        fun fit(samples: List<CalibrationSample>, useHead: Boolean, ridge: Double = 0.04): GazeModel? {
            val groups = listOf(TOP, MIDDLE, BOTTOM).map { t -> samples.filter { abs(it.targetY - t) < 1e-3 } }
            if (groups.any { it.size < MIN_SAMPLES_PER_TARGET }) return null

            val indices = (0 until GazeFeatures.COUNT).filter { useHead || it != GazeFeatures.HEAD_FEATURE }.toIntArray()
            val k = indices.size
            val n = samples.size

            val mean = DoubleArray(k)
            val std = DoubleArray(k)
            for (j in 0 until k) {
                var s = 0.0
                for (smp in samples) s += smp.features[indices[j]]
                mean[j] = s / n
                var v = 0.0
                for (smp in samples) {
                    val d = smp.features[indices[j]] - mean[j]
                    v += d * d
                }
                std[j] = sqrt(v / n).coerceAtLeast(1e-6)
            }

            val yMean = samples.sumOf { it.targetY.toDouble() } / n
            val a = Array(k) { DoubleArray(k) }
            val b = DoubleArray(k)
            val x = DoubleArray(k)
            for (smp in samples) {
                for (j in 0 until k) x[j] = (smp.features[indices[j]] - mean[j]) / std[j]
                val dy = smp.targetY - yMean
                for (i in 0 until k) {
                    b[i] += x[i] * dy
                    for (j in 0 until k) a[i][j] += x[i] * x[j]
                }
            }
            for (i in 0 until k) a[i][i] += ridge * n

            val w = solve(a, b) ?: return null
            val model = GazeModel(indices, mean, std, w, yMean, 0.0, 0.0, 0.0, 0.0)

            val means = groups.map { g -> g.sumOf { model.predict(it.features) } / g.size }
            var sq = 0.0
            groups.forEachIndexed { gi, g -> g.forEach { val d = model.predict(it.features) - means[gi]; sq += d * d } }
            val noise = sqrt(sq / n)

            return GazeModel(indices, mean, std, w, yMean, means[0], means[1], means[2], noise)
        }

        /** Gaussian elimination with partial pivoting; returns null for a singular system. */
        internal fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
            val n = b.size
            val m = Array(n) { i -> a[i].copyOf() }
            val v = b.copyOf()
            for (col in 0 until n) {
                var pivot = col
                for (r in col + 1 until n) if (abs(m[r][col]) > abs(m[pivot][col])) pivot = r
                if (abs(m[pivot][col]) < 1e-12) return null
                if (pivot != col) {
                    val t = m[pivot]; m[pivot] = m[col]; m[col] = t
                    val tv = v[pivot]; v[pivot] = v[col]; v[col] = tv
                }
                for (r in col + 1 until n) {
                    val f = m[r][col] / m[col][col]
                    for (c in col until n) m[r][c] -= f * m[col][c]
                    v[r] -= f * v[col]
                }
            }
            val out = DoubleArray(n)
            for (r in n - 1 downTo 0) {
                var s = v[r]
                for (c in r + 1 until n) s -= m[r][c] * out[c]
                out[r] = s / m[r][r]
            }
            return out
        }
    }
}
