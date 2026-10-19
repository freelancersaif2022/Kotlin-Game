package com.eyescroll.app.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GazeModelTest {

    /** Synthetic face: iris offset and blendshape move with gaze, openness shrinks looking down, plus noise. */
    private fun samples(noise: Float, seed: Int = 1, perTarget: Int = 60): List<CalibrationSample> {
        val rnd = Random(seed)
        fun n() = (rnd.nextFloat() - 0.5f) * 2f * noise
        return listOf(GazeModel.TOP, GazeModel.MIDDLE, GazeModel.BOTTOM).flatMap { t ->
            List(perTarget) {
                CalibrationSample(
                    t,
                    floatArrayOf(
                        -0.05f + 0.10f * t + n() * 0.05f,
                        0.32f - 0.10f * t + n() * 0.04f,
                        -0.4f + 0.8f * t + n() * 0.3f,
                        0.55f + n() * 0.01f,
                    ),
                )
            }
        }
    }

    private fun feature(t: Float) = floatArrayOf(-0.05f + 0.10f * t, 0.32f - 0.10f * t, -0.4f + 0.8f * t, 0.55f)

    @Test
    fun fitsCleanDataWithGoodSeparation() {
        val model = GazeModel.fit(samples(noise = 0.2f), useHead = true)
        assertNotNull(model)
        model!!
        assertTrue(model.isUsable)
        assertTrue(model.topY < model.midY && model.midY < model.bottomY)
        assertEquals(0.0, model.predict(feature(0f)), 0.15)
        assertEquals(1.0, model.predict(feature(1f)), 0.15)
        assertTrue(model.quality == CalibrationQuality.EXCELLENT || model.quality == CalibrationQuality.GOOD)
    }

    @Test
    fun rejectsTooFewSamples() {
        assertNull(GazeModel.fit(samples(noise = 0.1f, perTarget = 3), useHead = false))
    }

    @Test
    fun pureNoiseIsNotUsable() {
        val rnd = Random(7)
        val noisy = listOf(0f, 0.5f, 1f).flatMap { t ->
            List(50) { CalibrationSample(t, FloatArray(GazeFeatures.COUNT) { rnd.nextFloat() }) }
        }
        val model = GazeModel.fit(noisy, useHead = true)
        assertTrue(model == null || !model.isUsable)
    }

    @Test
    fun solverHandlesSimpleSystem() {
        val x = GazeModel.solve(arrayOf(doubleArrayOf(2.0, 1.0), doubleArrayOf(1.0, 3.0)), doubleArrayOf(3.0, 5.0))!!
        assertEquals(0.8, x[0], 1e-9)
        assertEquals(1.4, x[1], 1e-9)
    }

    @Test
    fun controllerScrollsAfterDwellAndIgnoresBlinks() {
        val model = GazeModel.fit(samples(noise = 0.05f), useHead = true)!!
        val c = GazeController(model, GazeController.Config(sensitivity = 0.5f, dwellMs = 400, repeatMs = 800))
        var t = 0L
        fun step(f: FloatArray?, blink: Float = 0f): ScrollDirection? {
            t += 40
            return c.process(FaceFrame(t, f, blink, 40f, 0.5f), t).second
        }

        // Reading in the middle: no scrolls.
        repeat(50) { assertNull(step(feature(0.5f))) }

        // Blinking looks like "looking down" but must never scroll.
        repeat(30) { assertNull(step(feature(1f), blink = 0.9f)) }

        // Look at the bottom: exactly one scroll after the dwell, then repeats.
        val fired = mutableListOf<ScrollDirection>()
        repeat(60) { step(feature(1.05f))?.let { fired += it } }
        assertTrue(fired.isNotEmpty())
        assertTrue(fired.all { it == ScrollDirection.DOWN })
        assertTrue("expected repeats, got ${fired.size}", fired.size in 2..4)

        // Immediately looking up is blocked by the opposite-direction cooldown at first.
        assertNull(step(feature(-0.05f)))
    }

    @Test
    fun noFaceResetsState() {
        val model = GazeModel.fit(samples(noise = 0.05f), useHead = false)!!
        val c = GazeController(model, GazeController.Config(0.5f, 300, 800))
        val (snap, fired) = c.process(FaceFrame.noFace(10), 10)
        assertNull(fired)
        assertTrue(!snap.faceFound)
        assertEquals(GazeZone.NONE, snap.zone)
    }
}
