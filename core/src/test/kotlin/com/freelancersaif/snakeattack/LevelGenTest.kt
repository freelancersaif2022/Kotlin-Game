package com.freelancersaif.snakeattack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelGenTest {
    /** Greedy removal must clear every generated board: any free block can always be removed. */
    @Test
    fun everyLevelIsSolvable() {
        for (n in 1..300) {
            val spec = LevelGen.generate(n)
            val grid = Array(spec.cols) { BooleanArray(spec.rows) }
            spec.blocks.forEach { grid[it.col][it.row] = true }
            val left = spec.blocks.toMutableList()
            while (left.isNotEmpty()) {
                val free = left.firstOrNull { b ->
                    var x = b.col + b.dir.dx
                    var y = b.row + b.dir.dy
                    var clear = true
                    while (x in 0 until spec.cols && y in 0 until spec.rows) {
                        if (grid[x][y]) clear = false
                        x += b.dir.dx
                        y += b.dir.dy
                    }
                    clear
                }
                assertTrue(free != null, "level $n is stuck with ${left.size} blocks")
                grid[free.col][free.row] = false
                left.remove(free)
            }
        }
    }

    @Test
    fun snakeMatchesAmmo() {
        for (n in 1..300) {
            val spec = LevelGen.generate(n)
            assertEquals(spec.blocks.size * spec.shotsPerBlock, spec.snakeColors.size)
            for (c in GemColor.entries) {
                assertEquals(
                    spec.blocks.count { it.color == c } * spec.shotsPerBlock,
                    spec.snakeColors.count { it == c },
                    "level $n colour $c",
                )
            }
            assertTrue(spec.blocks.size >= 6, "level $n too small")
        }
    }
}
