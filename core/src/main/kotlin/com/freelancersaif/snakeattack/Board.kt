package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Block(val spec: BlockSpec) {
    enum class State { IDLE, BUMP, EXIT, FLY, GONE }

    val color get() = spec.color
    var dir = spec.dir
    /** Spin animation (1 -> 0) played when the shuffle booster turns the arrow. */
    var spin = 0f
    private var oldAngle = 0f
    fun arrowAngle() = if (spin > 0f) dir.angle + (oldAngle - dir.angle + 360f) * Ease.inCubic(spin) else dir.angle
    fun turnTo(d: Dir) {
        oldAngle = dir.angle
        dir = d
        spin = 1f
    }
    var col = spec.col
    var row = spec.row
    var state = State.IDLE
    var time = 0f
    val pos = Vector2()
    val home = Vector2()
    var appear = 0f
    var appearDelay = 0f
    val squash = Spring(320f, 13f, 1f)
    val wiggle = Spring(260f, 8f)
    var bumpDist = 0f
    var hint = 0f

    // exit + flight
    val exitTo = Vector2()
    val flyFrom = Vector2()
    val flyTo = Vector2()
    var slot: Slot? = null
    var rot = 0f
    var size = 1f
}

class Board(val spec: LevelSpec) {
    val blocks = ArrayList<Block>()
    private val grid: Array<Array<Block?>> = Array(spec.cols) { arrayOfNulls(spec.rows) }
    var cell = 80f
    var originX = 0f
    var originY = 0f
    var panelX = 0f
    var panelY = 0f
    var panelW = 0f
    var panelH = 0f
    private var time = 0f
    private val tmp = Vector2()
    private val freeList = ArrayList<Block>()

    init {
        spec.blocks.forEach { b ->
            val blk = Block(b)
            blocks += blk
            grid[b.col][b.row] = blk
        }
    }

    fun layout(x: Float, y: Float, w: Float, h: Float) {
        cell = min(min(w / spec.cols, h / spec.rows), 112f)
        val bw = cell * spec.cols
        val bh = cell * spec.rows
        originX = x + (w - bw) / 2
        originY = y + (h - bh) / 2
        panelX = originX - 16f
        panelY = originY - 16f
        panelW = bw + 32f
        panelH = bh + 32f
        blocks.forEach { b ->
            cellCenter(b.col, b.row, b.home)
            if (b.state == Block.State.IDLE || b.state == Block.State.BUMP) b.pos.set(b.home)
        }
        // pop-in order: diagonal wave from the top-left
        blocks.forEach { it.appearDelay = 0.15f + (it.col + (spec.rows - 1 - it.row)) * 0.045f }
    }

    fun cellCenter(c: Int, r: Int, out: Vector2) = out.set(originX + (c + 0.5f) * cell, originY + (r + 0.5f) * cell)

    fun blockAt(wx: Float, wy: Float): Block? {
        val c = floor((wx - originX) / cell).toInt()
        val r = floor((wy - originY) / cell).toInt()
        if (c !in 0 until spec.cols || r !in 0 until spec.rows) return null
        return grid[c][r]
    }

    /** Number of empty cells in front of the block before another block (or -1 when the way is clear). */
    fun blockedAfter(b: Block): Int {
        var x = b.col + b.dir.dx
        var y = b.row + b.dir.dy
        var n = 0
        while (x in 0 until spec.cols && y in 0 until spec.rows) {
            if (grid[x][y] != null) return n
            n++
            x += b.dir.dx
            y += b.dir.dy
        }
        return -1
    }

    fun blockerOf(b: Block): Block? {
        val n = blockedAfter(b)
        if (n < 0) return null
        return grid[b.col + b.dir.dx * (n + 1)][b.row + b.dir.dy * (n + 1)]
    }

    fun isFree(b: Block) = b.state == Block.State.IDLE && blockedAfter(b) < 0

    /** Blocks that can slide out right now (the returned list is reused). */
    fun freeBlocks(): List<Block> {
        freeList.clear()
        for (b in blocks) if (isFree(b)) freeList += b
        return freeList
    }

    fun remaining() = blocks.count { it.state == Block.State.IDLE || it.state == Block.State.BUMP }

    /** Starts the slide-out: the cell is freed immediately so other blocks can use it. */
    fun launch(b: Block, slot: Slot) {
        grid[b.col][b.row] = null
        b.state = Block.State.EXIT
        b.time = 0f
        b.slot = slot
        // slide until fully outside the board panel
        val dist = when (b.dir) {
            Dir.RIGHT -> (spec.cols - b.col) * cell + 10f
            Dir.LEFT -> (b.col + 1) * cell + 10f
            Dir.UP -> (spec.rows - b.row) * cell + 10f
            Dir.DOWN -> (b.row + 1) * cell + 10f
        }
        b.exitTo.set(b.home.x + b.dir.dx * dist, b.home.y + b.dir.dy * dist)
        b.squash.value = 0.82f
    }

    /** Removes the idle blocks in the 3x3 area around (col, row) and returns them. */
    fun bomb(col: Int, row: Int): List<Block> {
        val hit = ArrayList<Block>()
        for (c in col - 1..col + 1) for (r in row - 1..row + 1) {
            if (c !in 0 until spec.cols || r !in 0 until spec.rows) continue
            val b = grid[c][r] ?: continue
            if (b.state != Block.State.IDLE && b.state != Block.State.BUMP) continue
            grid[c][r] = null
            b.state = Block.State.GONE
            hit += b
        }
        return hit
    }

    fun cellOf(wx: Float, wy: Float): Pair<Int, Int>? {
        val c = floor((wx - originX) / cell).toInt()
        val r = floor((wy - originY) / cell).toInt()
        return if (c in 0 until spec.cols && r in 0 until spec.rows) c to r else null
    }

    /**
     * Gives every idle block a new arrow. Directions are assigned in a random order
     * so each new arrow is clear of the blocks assigned before it, which keeps the
     * board solvable (remove them in reverse order). Returns false if nothing changed.
     */
    fun shuffle(): Boolean {
        val idle = blocks.filter { it.state == Block.State.IDLE && grid[it.col][it.row] === it }
        if (idle.isEmpty()) return false
        repeat(40) {
            val order = idle.shuffled()
            val placed = HashSet<Long>()
            val result = HashMap<Block, Dir>()
            var ok = true
            for (b in order) {
                val dirs = Dir.entries.filter { d ->
                    var x = b.col + d.dx
                    var y = b.row + d.dy
                    var clear = true
                    while (x in 0 until spec.cols && y in 0 until spec.rows) {
                        if (placed.contains(x * 100L + y)) { clear = false; break }
                        x += d.dx
                        y += d.dy
                    }
                    clear
                }
                if (dirs.isEmpty()) { ok = false; break }
                result[b] = dirs.random()
                placed += b.col * 100L + b.row
            }
            if (ok) {
                result.forEach { (b, d) -> b.turnTo(d) }
                return true
            }
        }
        return false
    }

    fun bump(b: Block) {
        b.state = Block.State.BUMP
        b.time = 0f
        b.bumpDist = blockedAfter(b) * cell + cell * 0.22f
        blockerOf(b)?.let { it.wiggle.kick(if (b.dir.dx != 0) 900f * b.dir.dx else 900f * b.dir.dy) }
    }

    fun update(dt: Float, onArrive: (Block) -> Unit) {
        time += dt
        for (b in blocks) {
            b.spin = max(0f, b.spin - dt * 2.5f)
            b.squash.update(dt)
            b.wiggle.update(dt)
            b.hint = max(0f, b.hint - dt * 0.35f)
            if (b.appear < 1f) {
                if (b.appearDelay > 0f) b.appearDelay -= dt else b.appear = min(1f, b.appear + dt * 3.2f)
            }
            when (b.state) {
                Block.State.IDLE -> b.pos.set(b.home)
                Block.State.BUMP -> {
                    b.time += dt
                    val outT = 0.11f
                    val back = 0.22f
                    val d = if (b.time < outT) Ease.inQuad(b.time / outT) * b.bumpDist
                    else b.bumpDist * (1f - Ease.outBack(min(1f, (b.time - outT) / back)))
                    b.pos.set(b.home.x + b.dir.dx * d, b.home.y + b.dir.dy * d)
                    if (b.time >= outT && b.time - dt < outT) b.squash.kick(-6f)
                    if (b.time > outT + back) {
                        b.state = Block.State.IDLE
                        b.pos.set(b.home)
                    }
                }
                Block.State.EXIT -> {
                    b.time += dt
                    val t = min(1f, b.time / 0.2f)
                    b.pos.set(b.home).lerp(b.exitTo, Ease.inQuad(t))
                    if (t >= 1f) {
                        b.state = Block.State.FLY
                        b.time = 0f
                        b.flyFrom.set(b.pos)
                        b.flyTo.set(b.slot!!.x, b.slot!!.y)
                    }
                }
                Block.State.FLY -> {
                    b.time += dt
                    val dur = 0.36f
                    val t = min(1f, b.time / dur)
                    val e = Ease.inOutSine(t)
                    val cx = (b.flyFrom.x + b.flyTo.x) / 2 + (b.flyFrom.x - b.flyTo.x) * 0.15f
                    val cy = max(b.flyFrom.y, b.flyTo.y) + 140f
                    val u = 1 - e
                    b.pos.set(
                        u * u * b.flyFrom.x + 2 * u * e * cx + e * e * b.flyTo.x,
                        u * u * b.flyFrom.y + 2 * u * e * cy + e * e * b.flyTo.y,
                    )
                    b.rot = sin(t * MathUtils.PI) * 25f * (if (b.flyTo.x > b.flyFrom.x) -1f else 1f)
                    b.size = 1f + sin(t * MathUtils.PI) * 0.25f
                    if (t >= 1f) {
                        b.state = Block.State.GONE
                        onArrive(b)
                    }
                }
                Block.State.GONE -> {}
            }
        }
    }

    fun draw(batch: Batch, hintBlock: Block?) {
        Assets.boardPanel.draw(batch, panelX, panelY, panelW, panelH)
        // subtle cell dots
        batch.setColor(1f, 1f, 1f, 0.08f)
        val v = tmp
        for (c in 0 until spec.cols) for (r in 0 until spec.rows) {
            cellCenter(c, r, v)
            batch.drawCentered(Assets.r("circle"), v.x, v.y, cell * 0.22f)
        }
        batch.setColor(Color.WHITE)
        // shadows then blocks (so shadows never overlap neighbours)
        for (b in blocks) {
            if (b.state == Block.State.FLY || b.state == Block.State.GONE || b.appear <= 0f) continue
            val s = cell * 0.96f * Ease.outBack(b.appear)
            batch.setColor(0f, 0f, 0f, 0.25f)
            batch.drawCentered(Assets.r("circle"), b.pos.x + 3f, b.pos.y - cell * 0.38f, s * 0.9f, s * 0.22f)
        }
        batch.setColor(Color.WHITE)
        for (b in blocks) {
            if (b.state == Block.State.FLY || b.state == Block.State.GONE || b.appear <= 0f) continue
            drawBlock(batch, b, b.pos.x, b.pos.y, cell * 0.96f * Ease.outBack(b.appear), 0f, b === hintBlock)
        }
    }

    fun drawFlying(batch: Batch) {
        for (b in blocks) {
            if (b.state != Block.State.FLY) continue
            val s = cell * 0.96f * b.size
            drawBlock(batch, b, b.pos.x, b.pos.y, s, b.rot, false)
        }
    }

    private fun drawBlock(batch: Batch, b: Block, x: Float, y: Float, size: Float, rot: Float, hinted: Boolean) {
        val sq = b.squash.value
        val horizontal = b.dir.dx != 0
        val sx = if (horizontal) 2f - sq else sq
        val sy = if (horizontal) sq else 2f - sq
        val wig = b.wiggle.value * 0.012f
        val reg = Assets.block(b.color)
        if (hinted) {
            val p = 0.5f + 0.5f * sin(time * 8f)
            batch.setColor(1f, 1f, 0.6f, 0.5f + 0.4f * p)
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE)
            batch.drawCentered(Assets.r("glow"), x, y, size * (1.6f + 0.2f * p))
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        batch.setColor(Color.WHITE)
        batch.drawCentered(reg, x, y, size, size * reg.regionHeight / reg.regionWidth, rot + wig * 10f, sx, sy)
        // arrow, slightly above centre because the block art has a thick bottom edge
        val pulse = if (hinted) 1f + 0.12f * sin(time * 10f) else 1f
        val ay = y + size * 0.05f
        batch.drawCentered(Assets.r("arrow"), x, ay, size * 0.62f * pulse * (1f + b.spin * 0.25f), rotation = b.arrowAngle() + rot, scaleX = sx, scaleY = sy)
    }

    fun nudgeIdle(dt: Float) {
        // occasional idle wobble on free blocks so the board feels alive
        if (MathUtils.randomBoolean(dt * 0.6f)) {
            val free = freeBlocks()
            if (free.isNotEmpty()) free.random().squash.kick(abs(MathUtils.random(1.5f, 3f)))
        }
    }
}
