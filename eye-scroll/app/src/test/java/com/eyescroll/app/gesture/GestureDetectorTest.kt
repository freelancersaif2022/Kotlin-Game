package com.eyescroll.app.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureDetectorTest {

    private val size = 0.12f

    private fun swipeDetector(natural: Boolean = true) =
        GestureDetector(GestureDetector.Config(GestureMode.SWIPE, natural, sensitivity = 0.5f, repeatMs = 800))

    /** Moves the fingertip linearly from [from] to [to] over [ms], one frame every 33 ms. */
    private fun move(d: GestureDetector, t0: Long, from: Float, to: Float, ms: Long, out: MutableList<ScrollDirection>): Long {
        var t = t0
        val steps = (ms / 33).toInt().coerceAtLeast(1)
        for (i in 1..steps) {
            t += 33
            val y = from + (to - from) * i / steps
            d.process(HandFrame(t, true, 0.5f, y, size), t).second?.let { out += it }
        }
        return t
    }

    @Test
    fun stillHandNeverScrolls() {
        val d = swipeDetector()
        val out = mutableListOf<ScrollDirection>()
        move(d, 0, 0.5f, 0.505f, 3000, out)
        assertTrue(out.isEmpty())
    }

    @Test
    fun quickSwipeUpScrollsOnce_andReturnIsIgnored() {
        val d = swipeDetector(natural = true)
        val out = mutableListOf<ScrollDirection>()
        var t = move(d, 0, 0.6f, 0.6f, 300, out)
        t = move(d, t, 0.6f, 0.4f, 250, out)      // swipe up
        t = move(d, t, 0.4f, 0.6f, 300, out)      // natural return
        move(d, t, 0.6f, 0.6f, 500, out)
        assertEquals(listOf(ScrollDirection.DOWN), out)
    }

    @Test
    fun reverseDirectionSetting() {
        val d = swipeDetector(natural = false)
        val out = mutableListOf<ScrollDirection>()
        val t = move(d, 0, 0.6f, 0.6f, 300, out)
        move(d, t, 0.6f, 0.4f, 250, out)
        assertEquals(listOf(ScrollDirection.UP), out)
    }

    @Test
    fun repeatedSwipesInSameDirectionAllCount() {
        val d = swipeDetector()
        val out = mutableListOf<ScrollDirection>()
        var t = move(d, 0, 0.6f, 0.6f, 300, out)
        repeat(3) {
            t = move(d, t, 0.6f, 0.4f, 250, out)
            t = move(d, t, 0.4f, 0.6f, 350, out)
        }
        assertEquals(3, out.size)
        assertTrue(out.all { it == ScrollDirection.DOWN })
    }

    @Test
    fun slowDriftIsNotASwipe() {
        val d = swipeDetector()
        val out = mutableListOf<ScrollDirection>()
        move(d, 0, 0.7f, 0.4f, 4000, out)
        assertTrue(out.isEmpty())
    }

    @Test
    fun holdModeScrollsWhileFingerHeldAbove() {
        val d = GestureDetector(GestureDetector.Config(GestureMode.HOLD, natural = true, sensitivity = 0.5f, repeatMs = 500))
        val out = mutableListOf<ScrollDirection>()
        var t = move(d, 0, 0.5f, 0.5f, 800, out)    // learn rest position
        assertTrue(out.isEmpty())
        t = move(d, t, 0.5f, 0.38f, 150, out)       // finger up by 1 hand size
        move(d, t, 0.38f, 0.38f, 1600, out)         // hold
        assertTrue("got ${out.size}", out.size in 3..5)
        assertTrue(out.all { it == ScrollDirection.UP })
    }

    @Test
    fun lostHandResets() {
        val d = swipeDetector()
        val (snap, fired) = d.process(HandFrame.none(10), 10)
        assertTrue(!snap.handFound)
        assertEquals(null, fired)
    }
}
