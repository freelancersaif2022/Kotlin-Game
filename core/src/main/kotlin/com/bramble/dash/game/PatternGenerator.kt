package com.bramble.dash.game

import com.bramble.dash.config.EnemyBehavior
import com.bramble.dash.config.Enemies
import com.bramble.dash.config.ObstacleKind
import com.bramble.dash.config.Obstacles
import com.bramble.dash.config.PowerUpType
import com.bramble.dash.config.Tuning
import com.bramble.dash.config.WorldDef
import com.bramble.dash.config.CollectibleType
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/** Movement capabilities the generator must respect (depend on the runner). */
class MoveProfile(val jumpVelocity: Float, val gravity: Float, val fallScale: Float, val slideDuration: Float) {
    val riseTime get() = jumpVelocity / gravity
    val apex get() = jumpVelocity * jumpVelocity / (2f * gravity)
    val airTime get() = riseTime + sqrt(2f * apex / (gravity * fallScale))

    /** Time spent above [height] during a single jump. */
    fun timeAbove(height: Float): Float {
        if (height >= apex) return 0f
        val up = (jumpVelocity - sqrt(jumpVelocity * jumpVelocity - 2f * gravity * height)) / gravity
        val down = sqrt(2f * (apex - height) / (gravity * fallScale))
        return (riseTime - up) + down
    }
}

/** One required (or optional) interaction placed in the level, used for fairness validation. */
class Beat(val x: Float, val width: Float, val action: Action, val height: Float, val label: String)

/**
 * Builds the endless level from reusable, validated patterns. Every pattern is a list of beats;
 * spacing comes from [requiredGap] (movement physics) multiplied by a difficulty comfort factor
 * that never drops below 1.0, so consecutive hazards always leave a dodge window.
 */
class PatternGenerator(private val world: GameWorld, private val def: WorldDef, seed: Long, private val move: MoveProfile, var tutorial: Boolean) {
    val rng = Random(seed)
    var nextX = 0f
    private var lastAction = Action.NONE
    val beats = ArrayList<Beat>()          // kept for validation/debugging (trimmed as the run advances)
    private var tutorialStep = 0
    private var powerUpTimer = 0f
    private var nextPowerUpAt = 0f
    private var lastPatternIndex = -1

    private val groundHazards = def.obstacles.filter { Obstacles.byId(it).kind in setOf(ObstacleKind.DAMAGE, ObstacleKind.TIMED, ObstacleKind.OSCILLATOR) }
    private val highHazards = def.obstacles.filter { Obstacles.byId(it).kind == ObstacleKind.HANGING }
    private fun ofKind(k: ObstacleKind) = def.obstacles.filter { Obstacles.byId(it).kind == k }
    private val jumpEnemies = def.enemies.filter { Enemies.byId(it).behavior in setOf(EnemyBehavior.HOPPER, EnemyBehavior.LUNGER, EnemyBehavior.CHARGER, EnemyBehavior.BURNER, EnemyBehavior.SHOOTER, EnemyBehavior.THROWER) }
    private val slideEnemies = def.enemies.filter { Enemies.byId(it).behavior in setOf(EnemyBehavior.SWOOPER, EnemyBehavior.DANGLER, EnemyBehavior.ZAPPER, EnemyBehavior.PHASER) }

    init {
        nextPowerUpAt = 14f + rng.nextFloat() * 6f
    }

    fun tier(metres: Float): Int = when {
        metres < 150f -> 0
        metres < 600f -> 1
        metres < 1500f -> 2
        else -> 3
    }

    /** Comfort multiplier on top of the physical minimum gap. Always >= 1. */
    fun comfort(tier: Int): Float {
        val base = when (tier) { 0 -> 2.2f; 1 -> 1.7f; 2 -> 1.35f; else -> 1.15f }
        return max(1f, base / (1f + (def.difficulty - 1f) * 0.25f))
    }

    /** Minimum seconds between the trailing edge of one hazard and the leading edge of the next. */
    fun requiredGap(a: Action, b: Action): Float = when (a) {
        Action.JUMP -> when (b) { Action.JUMP -> move.airTime + 0.15f; Action.SLIDE -> move.airTime + 0.1f; Action.NONE -> 0.2f }
        Action.SLIDE -> when (b) { Action.JUMP -> 0.3f; Action.SLIDE -> 0.35f; Action.NONE -> 0.1f }
        Action.NONE -> 0.1f
    }

    private fun gapPx(a: Action, b: Action, speed: Float, tier: Int): Float {
        val c = comfort(tier)
        return requiredGap(a, b) * c * speed + 40f
    }

    private fun place(action: Action, width: Float, height: Float, label: String, speed: Float, tier: Int, extraLead: Float = 0f): Float {
        val gap = if (beats.isEmpty() && lastAction == Action.NONE) 600f else gapPx(lastAction, action, speed, tier)
        val x = nextX + gap + extraLead
        beats += Beat(x, width, action, height, label)
        nextX = x + width
        lastAction = action
        return x
    }

    fun fill(untilX: Float, speed: Float, metres: Float, runTime: Float) {
        while (nextX < untilX && !world.generationPaused) {
            val tier = tier(metres + (nextX - world.player.x) / Tuning.PX_PER_METRE)
            if (tutorial) tutorialPattern(speed) else pattern(tier, speed)
            maybePowerUp(runTime + (nextX - world.player.x) / speed, speed)
        }
        while (beats.size > 40) beats.removeAt(0)
    }

    private fun maybePowerUp(projectedTime: Float, speed: Float) {
        if (projectedTime < nextPowerUpAt) return
        nextPowerUpAt = projectedTime + 18f + rng.nextFloat() * 12f
        val types = PowerUpType.values()
        val weights = floatArrayOf(1f, 1.2f, 1f, 1f, 0.35f, 0.6f, 0.8f, 0.7f, 0.9f, 1f)
        var pick = rng.nextFloat() * weights.sum()
        var t = types[0]
        for (i in types.indices) { pick -= weights[i]; if (pick <= 0f) { t = types[i]; break } }
        val x = nextX + 160f
        world.spawnPowerUp(t, x, Tuning.GROUND_Y + 70f)
        nextX = x + 120f
    }

    // ------------------------------------------------------------------ patterns
    private fun tutorialPattern(speed: Float) {
        when (tutorialStep) {
            0 -> { coinLine(nextX + 200f, 8); nextX += 700f; world.addHint(nextX - 300f, "Tap JUMP (or swipe up) to leap over hazards") }
            1 -> groundHazard(groundHazards.first(), speed, 0)
            2 -> { coinLine(nextX + 150f, 5); nextX += 450f; world.addHint(nextX, "Tap SLIDE (or swipe down) to duck under") }
            3 -> highHazard(highHazards.firstOrNull() ?: "hanging_chain", speed, 0)
            4 -> { coinLine(nextX + 150f, 5); nextX += 450f; world.addHint(nextX, "Tap JUMP again in the air to double jump") }
            5 -> { platform(speed, 0, height = 150f) }
            6 -> { world.addHint(nextX + 200f, "Land on enemies to defeat them!"); enemy(jumpEnemies.first(), speed, 0) }
            else -> { tutorial = false; world.onTutorialPatternsDone(nextX) }
        }
        tutorialStep++
    }

    private fun pattern(tier: Int, speed: Float) {
        val options = ArrayList<Pair<Float, () -> Unit>>()
        if (groundHazards.isNotEmpty()) options += 3f to { groundHazard(groundHazards.random(rng), speed, tier) }
        if (highHazards.isNotEmpty() && tier >= 1) options += 2f to { highHazard(highHazards.random(rng), speed, tier) }
        ofKind(ObstacleKind.SOLID).takeIf { it.isNotEmpty() }?.let { s -> options += 1.6f to { solidHop(s.random(rng), speed, tier) } }
        if (jumpEnemies.isNotEmpty() && tier >= 1) options += 1.8f to { enemy(jumpEnemies.random(rng), speed, tier) }
        if (slideEnemies.isNotEmpty() && tier >= 1) options += 1.4f to { enemy(slideEnemies.random(rng), speed, tier) }
        if (tier >= 1) options += 1.2f to { platform(speed, tier) }
        ofKind(ObstacleKind.BOUNCE).takeIf { it.isNotEmpty() && tier >= 1 }?.let { s -> options += 0.7f to { spring(s.random(rng), speed, tier) } }
        ofKind(ObstacleKind.ROLLER).takeIf { it.isNotEmpty() && tier >= 2 }?.let { s -> options += 1.1f to { roller(s.random(rng), speed, tier) } }
        ofKind(ObstacleKind.FALLING).takeIf { it.isNotEmpty() && tier >= 2 }?.let { s -> options += 0.9f to { falling(s.random(rng), speed, tier) } }
        ofKind(ObstacleKind.BREAKABLE).takeIf { it.isNotEmpty() && tier >= 1 }?.let { s -> options += 0.8f to { breakable(s.random(rng), speed, tier) } }
        ofKind(ObstacleKind.KNOCKABLE).takeIf { it.isNotEmpty() }?.let { s -> options += 0.6f to { knockable(s.random(rng), speed, tier) } }
        ofKind(ObstacleKind.SLOWDOWN).takeIf { it.isNotEmpty() && tier >= 1 }?.let { s -> options += 0.6f to { slowdown(s.random(rng), speed, tier) } }
        val plats = ofKind(ObstacleKind.COLLAPSING) + ofKind(ObstacleKind.MOVING_PLATFORM)
        if (plats.isNotEmpty() && tier >= 2) options += 1f to { specialPlatform(plats.random(rng), speed, tier) }
        if (tier >= 2 && groundHazards.isNotEmpty() && highHazards.isNotEmpty()) options += 1.2f to { combo(speed, tier) }
        if (tier >= 1) options += 0.5f to { coinRun(speed) }

        var pick = rng.nextFloat() * options.sumOf { it.first.toDouble() }.toFloat()
        var chosen = options.last()
        var idx = options.size - 1
        for ((i, o) in options.withIndex()) { pick -= o.first; if (pick <= 0f) { chosen = o; idx = i; break } }
        if (idx == lastPatternIndex && options.size > 2) { idx = (idx + 1) % options.size; chosen = options[idx] }
        lastPatternIndex = idx
        chosen.second()
        maybeTreasure(tier)
    }

    private fun maybeTreasure(tier: Int) {
        val r = rng.nextFloat()
        val x = nextX + 120f
        val y = Tuning.GROUND_Y + 30f
        val type = when {
            r < 0.035f -> CollectibleType.GEM
            r < 0.075f -> CollectibleType.STAR
            r < 0.095f -> CollectibleType.KEY
            r < 0.105f + tier * 0.003f -> CollectibleType.TREASURE_CHEST
            r < 0.135f -> CollectibleType.REWARD_BAG
            r < 0.17f -> CollectibleType.BONUS_TOKEN
            r < 0.185f -> CollectibleType.MYSTERY_BOX
            else -> null
        } ?: return
        world.spawnCollectible(type, x, y)
        nextX = x + 80f
    }

    private fun coinType(): CollectibleType {
        val r = rng.nextFloat()
        return when {
            r < 0.02f -> CollectibleType.DIAMOND_COIN
            r < 0.14f -> CollectibleType.GOLD_COIN
            else -> CollectibleType.SILVER_COIN
        }
    }

    fun coinLine(x: Float, count: Int, y: Float = Tuning.GROUND_Y + 30f, spacing: Float = 56f) {
        val t = coinType()
        for (i in 0 until count) world.spawnCollectible(t, x + i * spacing, y)
    }

    /** Coins along the jump arc so collecting them teaches the right timing. */
    private fun coinArc(centerX: Float, speed: Float, baseY: Float = Tuning.GROUND_Y, count: Int = 7, scale: Float = 1f) {
        val air = move.riseTime * 2f
        val t = coinType()
        for (i in 0 until count) {
            val f = i / (count - 1f)
            val time = f * air
            val h = move.jumpVelocity * time - 0.5f * move.gravity * time * time
            world.spawnCollectible(t, centerX - air * speed / 2f + time * speed, baseY + 30f + h * 0.85f * scale)
        }
    }

    private fun groundHazard(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.JUMP, ext.first, ext.second, id, speed, tier)
        coinLine(x - 320f, 4)
        world.spawnObstacle(id, x + ext.first / 2f, Tuning.GROUND_Y)
        coinArc(x + ext.first / 2f, speed)
    }

    private fun highHazard(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.SLIDE, ext.first, 0f, id, speed, tier)
        world.spawnObstacle(id, x + ext.first / 2f, Tuning.GROUND_Y + GameWorld.HIGH_LANE_BOTTOM)
        coinLine(x - 60f, 4, Tuning.GROUND_Y + 22f, 50f)
    }

    private fun solidHop(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.JUMP, ext.first, ext.second, id, speed, tier)
        world.spawnObstacle(id, x + ext.first / 2f, Tuning.GROUND_Y)
        coinLine(x + 10f, 2, Tuning.GROUND_Y + ext.second + 40f, 50f)
    }

    private fun breakable(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.JUMP, ext.first, ext.second, id, speed, tier)
        world.spawnObstacle(id, x + ext.first / 2f, Tuning.GROUND_Y)
        coinArc(x + ext.first / 2f, speed)
    }

    private fun knockable(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.NONE, ext.first, 0f, id, speed, tier)
        world.spawnObstacle(id, x + ext.first / 2f, Tuning.GROUND_Y)
        coinLine(x + 80f, 4)
    }

    private fun slowdown(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.NONE, ext.first, 0f, id, speed, tier)
        world.spawnObstacle(id, x + ext.first / 2f, Tuning.GROUND_Y)
        coinArc(x + ext.first / 2f, speed, count = 6)
        // leaving quicksand needs time to regain speed: treat as a jump for spacing purposes
        lastAction = Action.JUMP
    }

    private fun spring(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.NONE, ext.first, 0f, id, speed, tier)
        world.spawnObstacle(id, x + ext.first / 2f, Tuning.GROUND_Y)
        // coins high in the air along the spring arc
        val v = GameWorld.SPRING_VELOCITY
        val air = 2f * v / move.gravity
        for (i in 1..8) {
            val time = i / 9f * air
            world.spawnCollectible(CollectibleType.GOLD_COIN, x + ext.first / 2f + time * speed, Tuning.GROUND_Y + 40f + v * time - 0.5f * move.gravity * time * time)
        }
        nextX += air * speed * 0.9f
        lastAction = Action.JUMP
    }

    private fun roller(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.JUMP, ext.first * 0.7f, ext.second, id, speed, tier)
        val roll = speed * 0.35f
        val activation = world.viewWidthEstimate - Tuning.PLAYER_SCREEN_X + 150f
        // starts still beyond the screen, rolls when within [activation]; meets the player at x
        val startX = x + activation * roll / (speed + roll)
        world.spawnRoller(id, startX, roll, activation)
        coinLine(x - 300f, 3)
    }

    private fun falling(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val x = place(Action.JUMP, ext.first, ext.second, id, speed, tier)
        world.spawnFalling(id, x + ext.first / 2f, speed)
        coinArc(x + ext.first / 2f, speed)
    }

    private fun enemy(id: String, speed: Float, tier: Int) {
        val e = Enemies.byId(id)
        val ext = world.enemyExtent(id)
        val action = if (id in slideEnemies) Action.SLIDE else Action.JUMP
        // projectile enemies fire a head-height shot first: reserve a slide beat before the body
        val shoots = e.behavior == EnemyBehavior.SHOOTER || e.behavior == EnemyBehavior.THROWER || e.behavior == EnemyBehavior.ZAPPER
        val lead = 0f
        if (shoots) {
            // the shot meets the player before the body; spacing before it must allow a slide
            val px = place(Action.SLIDE, 60f, 0f, "$id-shot", speed, tier)
            val bodyGap = max(requiredGap(Action.SLIDE, action) * comfort(tier) * speed, GameWorld.SHOT_LEAD_PX).coerceAtMost(480f)
            val bx = px + 60f + bodyGap
            beats += Beat(bx, ext.first, action, ext.second, id)
            nextX = bx + ext.first
            lastAction = action
            world.spawnEnemy(id, bx + ext.first / 2f, speed, shotMeetX = px + 30f)
        } else if (e.behavior == EnemyBehavior.CHARGER) {
            val x = place(action, ext.first, ext.second, id, speed, tier, lead)
            val charge = speed * 0.45f
            val activation = world.viewWidthEstimate - Tuning.PLAYER_SCREEN_X + 50f
            world.spawnCharger(id, x + ext.first / 2f + activation * charge / (speed + charge), charge, activation)
        } else {
            val x = place(action, ext.first, ext.second, id, speed, tier, lead)
            world.spawnEnemy(id, x + ext.first / 2f, speed)
            if (action == Action.JUMP) coinArc(x + ext.first / 2f, speed, count = 5) else coinLine(x - 40f, 3, Tuning.GROUND_Y + 22f, 50f)
        }
    }

    private fun platform(speed: Float, tier: Int, height: Float = 110f + rng.nextFloat() * 50f) {
        val tiles = 2 + rng.nextInt(3)
        val width = tiles * GameWorld.PLATFORM_TILE_W
        val x = place(Action.NONE, width, 0f, "platform", speed, tier) + 120f
        nextX += 120f
        world.spawnPlatform(x, Tuning.GROUND_Y + height, tiles)
        coinLine(x + 30f, (width / 56f).toInt() - 1, Tuning.GROUND_Y + height + 34f)
        // a hazard under the platform from tier 2: either jump it or ride the platform over it
        if (tier >= 2 && groundHazards.isNotEmpty() && rng.nextFloat() < 0.5f) {
            val id = groundHazards.random(rng)
            val ext = world.obstacleExtent(id)
            val hx = x + width * 0.55f
            world.spawnObstacle(id, hx, Tuning.GROUND_Y)
            beats += Beat(hx - ext.first / 2f, ext.first, Action.JUMP, ext.second, "$id-under")
            nextX = max(nextX, hx + ext.first / 2f)
            lastAction = Action.JUMP
        }
    }

    private fun specialPlatform(id: String, speed: Float, tier: Int) {
        val ext = world.obstacleExtent(id)
        val count = 2 + rng.nextInt(2)
        val step = ext.first + 140f
        val x = place(Action.NONE, step * count, 0f, id, speed, tier) + 100f
        nextX += 100f
        for (i in 0 until count) {
            val h = 120f + (i % 2) * 30f
            world.spawnObstacle(id, x + i * step + ext.first / 2f, Tuning.GROUND_Y + h)
            coinLine(x + i * step + 10f, 3, Tuning.GROUND_Y + h + 60f, 46f)
        }
    }

    private fun combo(speed: Float, tier: Int) {
        val g = groundHazards.random(rng)
        val h = highHazards.random(rng)
        if (rng.nextBoolean()) { groundHazard(g, speed, tier); highHazard(h, speed, tier) }
        else { highHazard(h, speed, tier); groundHazard(g, speed, tier) }
    }

    private fun coinRun(speed: Float) {
        val n = 8 + rng.nextInt(6)
        val x = nextX + 200f
        coinLine(x, n)
        nextX = x + n * 56f
        lastAction = Action.NONE
    }

    /** Fairness check over the generated beat list for a given speed (used by tests and debug builds). */
    fun validate(speed: Float): List<String> {
        val problems = ArrayList<String>()
        val sorted = beats.sortedBy { it.x }
        for (i in 1 until sorted.size) {
            val a = sorted[i - 1]
            val b = sorted[i]
            if (a.action == Action.NONE || b.action == Action.NONE) continue
            val gap = (b.x - (a.x + a.width)) / speed
            val need = requiredGap(a.action, b.action)
            if (gap + 1e-3f < need) problems += "gap ${"%.2f".format(gap)}s < ${"%.2f".format(need)}s between ${a.label}@${a.x.toInt()} and ${b.label}@${b.x.toInt()}"
        }
        for (b in sorted) if (b.action == Action.JUMP) {
            val clear = move.timeAbove(b.height + 4f) * speed
            if (b.width + GameWorld.PLAYER_HALF_WIDTH * 2 > clear) problems += "${b.label} too wide/tall to jump at ${speed.toInt()} px/s"
        }
        return problems
    }
}
