package com.vaultly.app.scan

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class QuadDetectorTest {
    private val w = 160
    private val h = 120

    /** Dark textured table with a bright rotated rectangle (the receipt) and dark text lines on it. */
    private fun scene(cx: Float, cy: Float, rw: Float, rh: Float, degrees: Float): IntArray {
        val a = Math.toRadians(degrees.toDouble())
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        val rnd = java.util.Random(3)
        return IntArray(w * h) { i ->
            val x = i % w - cx
            val y = i / w - cy
            val u = x * ca + y * sa
            val v = -x * sa + y * ca
            val inside = kotlin.math.abs(u) < rw / 2 && kotlin.math.abs(v) < rh / 2
            if (inside) {
                val textRow = ((v + rh / 2).toInt() % 6 == 0) && kotlin.math.abs(u) < rw / 3
                if (textRow) 60 else 235 + rnd.nextInt(10)
            } else {
                55 + rnd.nextInt(25)
            }
        }
    }

    private fun near(p: Pt, x: Float, y: Float, tol: Float = 0.06f) =
        kotlin.math.abs(p.x - x) < tol && kotlin.math.abs(p.y - y) < tol

    @Test fun findsUprightReceipt() {
        val q = QuadDetector.detect(scene(80f, 60f, 60f, 100f, 0f), w, h)
        assertNotNull(q)
        q!!
        assertTrue("tl ${q.tl}", near(q.tl, 50f / 159, 10f / 119))
        assertTrue("br ${q.br}", near(q.br, 110f / 159, 110f / 119))
    }

    @Test fun findsTiltedReceipt() {
        val q = QuadDetector.detect(scene(80f, 60f, 56f, 90f, 12f), w, h)
        assertNotNull(q)
        assertTrue(q!!.area() > 0.2f)
    }

    @Test fun rejectsEmptyTable() {
        val rnd = java.util.Random(1)
        assertNull(QuadDetector.detect(IntArray(w * h) { 60 + rnd.nextInt(20) }, w, h))
    }

    @Test fun rotationMapsCorners() {
        val q = Quad(Pt(0.1f, 0.2f), Pt(0.6f, 0.2f), Pt(0.6f, 0.9f), Pt(0.1f, 0.9f))
        val r = q.rotated(90)
        // A tall-left quad becomes a wide-top quad after a 90° clockwise turn.
        assertTrue(near(r.tl, 0.1f, 0.1f, 0.01f))
        assertTrue(near(r.br, 0.8f, 0.6f, 0.01f))
    }
}
