package com.eyescroll.app.scroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollMathTest {

    private val threshold = 20f
    private val travel = 120f

    @Test
    fun belowThresholdDoesNothing() {
        assertNull(ScrollMath.direction(10f, threshold, natural = true))
        assertNull(ScrollMath.direction(-19f, threshold, natural = false))
        assertEquals(0f, ScrollMath.swipeFraction(5f, threshold, travel, 0.7f))
    }

    @Test
    fun naturalStyleMatchesScreenSwipe() {
        // Finger up on the pad = page moves up = reveal content below.
        assertEquals(ScrollDirection.DOWN, ScrollMath.direction(-30f, threshold, natural = true))
        assertEquals(ScrollDirection.UP, ScrollMath.direction(30f, threshold, natural = true))
    }

    @Test
    fun joystickStyleIsInverted() {
        assertEquals(ScrollDirection.UP, ScrollMath.direction(-30f, threshold, natural = false))
        assertEquals(ScrollDirection.DOWN, ScrollMath.direction(30f, threshold, natural = false))
    }

    @Test
    fun slightSwipeStillScrollsUsefully_fullSwipeScrollsMax() {
        val slight = ScrollMath.swipeFraction(threshold + 1f, threshold, travel, 0.7f)
        val full = ScrollMath.swipeFraction(travel, threshold, travel, 0.7f)
        assertTrue("slight=$slight", slight >= 0.2f)
        assertEquals(0.7f, full, 1e-4f)
        assertTrue(ScrollMath.swipeFraction(-70f, threshold, travel, 0.7f) in slight..full)
    }

    @Test
    fun holdGetsFasterWithPullAndSpeed() {
        val near = ScrollMath.holdIntervalMs(threshold, threshold, travel, 0.5f)
        val far = ScrollMath.holdIntervalMs(travel, threshold, travel, 0.5f)
        val farFast = ScrollMath.holdIntervalMs(travel, threshold, travel, 1f)
        assertTrue(far < near)
        assertTrue(farFast < far)
        assertTrue(farFast >= 150)
    }
}
