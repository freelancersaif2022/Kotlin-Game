package com.voxcast.mic.audio

import com.voxcast.mic.data.NoiseMode
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Real-time voice cleanup chain, runs on every 10 ms frame with zero added latency:
 *
 *   DC/rumble high-pass (85 Hz) -> hiss low-pass -> optional presence boost (voice clarity)
 *   -> adaptive noise gate/expander -> make-up gain -> soft limiter
 *
 * The gate tracks the room's noise floor continuously (fast down, slow up), so it adapts
 * to fans, traffic or AC hum. In ULTRA mode anything that is not voice is fully muted, which
 * gives true digital silence between words. Combined with Android's hardware
 * NoiseSuppressor/AEC (enabled by the mic engine) this removes steady background noise.
 */
class NoiseProcessor(private val sampleRate: Int) {

    @Volatile var mode: NoiseMode = NoiseMode.ULTRA
    @Volatile var gainDb: Float = 0f
    @Volatile var voiceEnhance: Boolean = true

    private val highPass1 = Biquad.highPass(sampleRate, 85f)
    private val highPass2 = Biquad.highPass(sampleRate, 85f)
    private val lowPass = Biquad.lowPass(sampleRate, min(9_500f, sampleRate * 0.45f))
    private val presence = Biquad.peaking(sampleRate, 3_200f, 3.5f, 0.9f)
    private val warmth = Biquad.peaking(sampleRate, 180f, -2f, 0.8f)

    private var buf = FloatArray(0)
    private var noiseFloorDb = -60f
    private var gateGain = 0f
    private var holdFrames = 0
    private var gateOpen = false

    // Per-sample smoothing coefficients.
    private val attackCoef = coef(3f)
    private val releaseCoef = coef(90f)

    private fun coef(ms: Float): Float = exp(-1f / (sampleRate * ms / 1000f))

    /** Processes [frame] in place. Returns the post-processing RMS level in 0..1. */
    fun process(frame: ShortArray, length: Int = frame.size): Float {
        val currentMode = mode
        val makeUp = 10f.pow(gainDb / 20f)
        if (buf.size < length) buf = FloatArray(length)
        val work = buf
        var sumSq = 0.0

        for (i in 0 until length) {
            var x = frame[i] / 32768f
            if (currentMode != NoiseMode.OFF) {
                x = highPass2.process(highPass1.process(x))
                x = lowPass.process(x)
            }
            if (voiceEnhance) {
                x = presence.process(warmth.process(x))
            }
            work[i] = x
            sumSq += (x * x).toDouble()
        }

        val rms = sqrt(sumSq / max(1, length)).toFloat()
        val rmsDb = 20f * log10(max(rms, 1e-6f))

        val targetGain = when (currentMode) {
            NoiseMode.OFF -> 1f
            NoiseMode.STANDARD -> gateTarget(rmsDb, closedGain = 0.12f)  // ~ -18 dB expander
            NoiseMode.ULTRA -> gateTarget(rmsDb, closedGain = 0f)       // full silence
        }

        var outSq = 0.0
        for (i in 0 until length) {
            val coefficient = if (targetGain > gateGain) attackCoef else releaseCoef
            gateGain = targetGain + (gateGain - targetGain) * coefficient
            var y = work[i] * gateGain * makeUp
            y = softLimit(y)
            if (abs(y) < 1e-5f) y = 0f
            frame[i] = (y * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            outSq += (y * y).toDouble()
        }
        val outRms = sqrt(outSq / max(1, length)).toFloat()
        return min(1f, outRms * 3.2f)
    }

    private fun gateTarget(rmsDb: Float, closedGain: Float): Float {
        // Noise floor tracker: follows quiet passages quickly, rises slowly during speech.
        noiseFloorDb = if (rmsDb < noiseFloorDb) {
            noiseFloorDb * 0.85f + rmsDb * 0.15f
        } else {
            noiseFloorDb + 0.012f * (rmsDb - noiseFloorDb).coerceAtMost(25f) / 10f
        }
        noiseFloorDb = noiseFloorDb.coerceIn(-85f, -28f)

        val openAt = max(noiseFloorDb + 10f, -58f)
        val closeAt = max(noiseFloorDb + 6f, -62f)

        if (rmsDb > openAt) {
            gateOpen = true
            holdFrames = HOLD_FRAMES
        } else if (rmsDb < closeAt) {
            if (holdFrames > 0) holdFrames-- else gateOpen = false
        }
        return if (gateOpen) 1f else closedGain
    }

    private fun softLimit(x: Float): Float {
        val t = 0.85f
        val a = abs(x)
        if (a <= t) return x
        val over = a - t
        val limited = t + (1f - t) * (over / (over + (1f - t)))
        return if (x < 0) -limited else limited
    }

    private companion object {
        const val HOLD_FRAMES = 22 // 220 ms keeps word tails natural
    }
}
