package com.freelancersaif.snakeattack

import kotlin.math.min
import kotlin.random.Random

enum class Dir(val dx: Int, val dy: Int, val angle: Float) {
    UP(0, 1, 90f), DOWN(0, -1, 270f), LEFT(-1, 0, 180f), RIGHT(1, 0, 0f)
}

class BlockSpec(val col: Int, val row: Int, val dir: Dir, val color: GemColor)

class LevelSpec(
    val number: Int,
    val cols: Int,
    val rows: Int,
    val blocks: List<BlockSpec>,
    val snakeColors: List<GemColor>,
    val shotsPerBlock: Int,
    val duration: Float,
)

/**
 * Procedural level generator. Blocks are added one at a time and each new
 * block must have a clear line to the board edge among the blocks placed so
 * far. Removing them in reverse order is therefore always possible, so every
 * generated board is solvable.
 */
object LevelGen {
    fun generate(number: Int): LevelSpec {
        val rnd = Random(number * 7919 + 17)
        val n = number - 1
        val cols = min(4 + n / 4, 7)
        val rows = min(5 + n / 3, 9)
        val colorCount = min(3 + n / 3, GemColor.entries.size)
        val fill = min(0.55f + n * 0.03f, 0.86f)
        val target = (cols * rows * fill).toInt().coerceAtLeast(6)
        val shots = 2

        val palette = GemColor.entries.shuffled(rnd).take(colorCount)
        val grid = Array(cols) { arrayOfNulls<BlockSpec>(rows) }
        val placed = ArrayList<BlockSpec>()

        fun clear(c: Int, r: Int, d: Dir): Boolean {
            var x = c + d.dx
            var y = r + d.dy
            while (x in 0 until cols && y in 0 until rows) {
                if (grid[x][y] != null) return false
                x += d.dx
                y += d.dy
            }
            return true
        }

        val cells = ArrayList<Pair<Int, Int>>()
        for (c in 0 until cols) for (r in 0 until rows) cells += c to r
        var attempts = 0
        while (placed.size < target && attempts < 4000) {
            attempts++
            val (c, r) = cells[rnd.nextInt(cells.size)]
            if (grid[c][r] != null) continue
            val dirs = Dir.entries.filter { clear(c, r, it) }
            if (dirs.isEmpty()) continue
            // prefer longer escape routes: they get blocked more often later
            val d = dirs.maxByOrNull { dirLen(c, r, it, cols, rows) + rnd.nextFloat() * 3f }!!
            val color = palette[(placed.size + rnd.nextInt(2)) % palette.size]
            val b = BlockSpec(c, r, d, color)
            grid[c][r] = b
            placed += b
        }

        // Snake order follows the reverse placement order (a valid solution) with local shuffles,
        // so the colours arriving first are reachable early but the player still has to think.
        val solution = placed.reversed()
        val snake = ArrayList<GemColor>()
        solution.forEach { b -> repeat(shots) { snake += b.color } }
        val window = 3 + min(n, 6)
        for (i in snake.indices step window) {
            val end = min(i + window, snake.size)
            val sub = snake.subList(i, end)
            sub.shuffle(rnd)
        }

        val duration = 18f + placed.size * 0.95f - min(n, 20) * 0.2f
        return LevelSpec(number, cols, rows, placed, snake, shots, duration)
    }

    private fun dirLen(c: Int, r: Int, d: Dir, cols: Int, rows: Int) = when (d) {
        Dir.UP -> rows - 1 - r
        Dir.DOWN -> r
        Dir.LEFT -> c
        Dir.RIGHT -> cols - 1 - c
    }
}
