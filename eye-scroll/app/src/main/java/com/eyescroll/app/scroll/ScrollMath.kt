package com.eyescroll.app.scroll

import kotlin.math.abs

/** DOWN = reveal content further down the page. */
enum class ScrollDirection { UP, DOWN }

/** Pure mapping from finger movement on the pad to scroll decisions (unit-tested). */
object ScrollMath {

    /**
     * Direction for a vertical finger offset [dy] (screen pixels, negative = finger moved up).
     * Natural: finger up moves the page up, i.e. reveals content below (DOWN), like swiping the screen.
     * Joystick: finger up goes UP the page.
     */
    fun direction(dy: Float, thresholdPx: Float, natural: Boolean): ScrollDirection? {
        if (abs(dy) < thresholdPx) return null
        val fingerUp = dy < 0
        return if (fingerUp == natural) ScrollDirection.DOWN else ScrollDirection.UP
    }

    /** 0..1 strength of a pull: 0 at the threshold, 1 at the end of the pad's travel. */
    fun strength(dy: Float, thresholdPx: Float, travelPx: Float): Float {
        val range = (travelPx - thresholdPx).coerceAtLeast(1f)
        return ((abs(dy) - thresholdPx) / range).coerceIn(0f, 1f)
    }

    /**
     * Fraction of the screen height to scroll for a swipe-and-release. A slight movement
     * already gives a useful scroll; a full pull gives [maxFraction].
     */
    fun swipeFraction(dy: Float, thresholdPx: Float, travelPx: Float, maxFraction: Float): Float {
        if (abs(dy) < thresholdPx) return 0f
        val min = (maxFraction * 0.35f).coerceAtLeast(0.12f)
        val s = strength(dy, thresholdPx, travelPx)
        return (min + (maxFraction - min) * s).coerceIn(0.05f, 0.85f)
    }

    /**
     * Delay between repeated scrolls while the finger is held. Further pull and higher
     * [holdSpeed] (0..1) scroll faster.
     */
    fun holdIntervalMs(dy: Float, thresholdPx: Float, travelPx: Float, holdSpeed: Float): Long {
        val s = strength(dy, thresholdPx, travelPx)
        val slowest = 900f - 400f * holdSpeed.coerceIn(0f, 1f)
        val fastest = 320f - 160f * holdSpeed.coerceIn(0f, 1f)
        return (slowest + (fastest - slowest) * s).toLong()
    }
}
