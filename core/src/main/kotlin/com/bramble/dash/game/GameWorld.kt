package com.bramble.dash.game

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array as GdxArray
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Pools
import com.bramble.dash.assets.SpriteData
import com.bramble.dash.audio.Sfx
import com.bramble.dash.config.Ability
import com.bramble.dash.config.Bosses
import com.bramble.dash.config.CharacterDef
import com.bramble.dash.config.CollectibleType
import com.bramble.dash.config.EnemyBehavior
import com.bramble.dash.config.Enemies
import com.bramble.dash.config.Lane
import com.bramble.dash.config.ObstacleKind
import com.bramble.dash.config.Obstacles
import com.bramble.dash.config.PowerUpType
import com.bramble.dash.config.Tuning
import com.bramble.dash.config.WorldDef
import com.bramble.dash.progression.MysteryBox
import com.bramble.dash.progression.Reward
import com.bramble.dash.progression.RunReport
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Explicit run states; the screen layers Loading/Lobby/Results UI around these. */
enum class RunState { READY, RUNNING, BOSS_WARNING, BOSS, BOSS_DEFEATED, VICTORY, PAUSED, DYING, REVIVE_OFFER, RESULTS }

interface WorldListener {
    fun sfx(s: Sfx, volume: Float = 1f, pitch: Float = 1f) {}
    fun shake(amount: Float) {}
    fun vibrate(ms: Int) {}
    fun stateChanged(from: RunState, to: RunState) {}
    fun powerUp(t: PowerUpType, activated: Boolean) {}
    fun reward(text: String, r: Reward) {}
    fun tutorialFinished() {}
}

class RunSetup(
    val character: CharacterDef,
    val world: WorldDef,
    val seed: Long,
    val maxHealth: Int,
    val durationBonus: Float,
    val magnetRange: Float,
    val scoreBonus: Float,
    val startShield: Boolean,
    val tutorial: Boolean,
    /** Effect quality 0..2: caps simultaneous VFX. */
    val quality: Int = 2,
)

/**
 * The complete run simulation: fixed-step physics, pattern spawning, enemies, bosses, power-ups,
 * scoring and the run state machine. It holds no textures, so it runs headless in tests.
 */
class GameWorld(val sprites: SpriteData, val setup: RunSetup, private val listener: WorldListener) {
    companion object {
        const val HIGH_LANE_BOTTOM = 78f
        const val PLATFORM_SCALE = 0.75f
        const val PLATFORM_TILE_W = 256f * PLATFORM_SCALE
        const val PLATFORM_SURFACE = 108f * PLATFORM_SCALE   // surface height inside the 256x128 tile
        const val PLAYER_HALF_WIDTH = 30f
        const val SPRING_VELOCITY = 1500f
        const val SHOT_LEAD_PX = 300f
        const val STOMP_BOUNCE = 820f
        const val LUNGE_DISTANCE = 84f
    }

    val def = setup.world
    val player = Player(setup.character)
    val powerUps = PowerUps(setup.durationBonus, if (setup.character.ability == Ability.MAGNET) 1.5f else 1f)
    val obstacles = GdxArray<Obstacle>(false, 64)
    val platforms = GdxArray<Platform>(false, 16)
    val enemies = GdxArray<Enemy>(false, 16)
    val projectiles = GdxArray<Projectile>(false, 32)
    val pickups = GdxArray<Pickup>(false, 128)
    val effects = GdxArray<Effect>(false, 64)
    val texts = GdxArray<FloatText>(false, 16)
    val warnings = ArrayList<Warning>()
    val hints = ArrayList<Pair<Float, String>>()
    var activeHint: String? = null
    var hintTime = 0f
    var boss: Boss? = null
        private set

    private val obstaclePool = Pools.get(Obstacle::class.java)
    private val platformPool = Pools.get(Platform::class.java)
    private val enemyPool = Pools.get(Enemy::class.java)
    private val projectilePool = Pools.get(Projectile::class.java)
    private val pickupPool = Pools.get(Pickup::class.java)
    private val effectPool = Pools.get(Effect::class.java)
    private val textPool = Pools.get(FloatText::class.java)

    var state = RunState.READY
        private set
    private var stateBeforePause = RunState.RUNNING
    var stateTime = 0f
        private set
    var runTime = 0f
        private set
    var speed = 0f
        private set
    var baseSpeed = Tuning.START_SPEED
        private set
    private var speedFactor = 1f
    var cameraX = 0f
        private set
    var prevCameraX = 0f
        private set
    var viewWidth = 1280f
    val viewWidthEstimate get() = viewWidth
    private var accumulator = 0f
    var alpha = 1f
        private set

    // run totals
    var score = 0f
        private set
    var coins = 0
        private set
    var gems = 0; var keys = 0; var chests = 0; var tokens = 0; var stars = 0
    var enemiesDefeated = 0; var slidesUnder = 0; var powerUpsUsed = 0; var revives = 0
    private val bossesDefeated = ArrayList<String>()
    private var gateBossDefeated = false
    private var startX = 0f
    var bossEncounters = 0
        private set
    var nextBossMetres = Tuning.FIRST_BOSS_METRES
        private set
    var lastBossReward: Reward? = null
        private set
    var report: RunReport? = null
        private set
    var generationPaused = false
        private set
    var energyCooldown = 0f
        private set
    val canAttack get() = state == RunState.BOSS && (boss?.let { it.state != BossState.ENTER && !it.defeated } ?: false)
    var autoPilot: AutoPilot? = null
    /** Bot helper: full-height jump without holding the button (release would trim the rise). */
    var releaseJumpLater = false
    var invincibleCheat = false

    val move = MoveProfile(Tuning.JUMP_VELOCITY * player.jumpScale, Tuning.GRAVITY, player.fallGravityScale, player.slideDuration)
    val generator = PatternGenerator(this, def, setup.seed, move, setup.tutorial)

    private val standBox = Rectangle()
    private val slideBox = Rectangle()
    private val tmp = Rectangle()
    private val tmp2 = Rectangle()
    private val expired = ArrayList<PowerUpType>()
    private var jumpQueued = false
    private var slideQueued = false
    private var attackQueued = false
    private val rng = kotlin.random.Random(setup.seed xor 0x5DEECE66DL)

    val metres get() = ((player.x - startX) / Tuning.PX_PER_METRE).coerceAtLeast(0f)
    val scoreMultiplier get() = (if (powerUps.active(PowerUpType.SCORE_MULTIPLIER)) 2f else 1f) * (1f + setup.scoreBonus)
    val bossProgress get() = if (state == RunState.BOSS || state == RunState.BOSS_WARNING) 1f
        else ((metres - (nextBossMetres - if (bossEncounters == 0) Tuning.FIRST_BOSS_METRES else Tuning.BOSS_INTERVAL_METRES)) /
            (if (bossEncounters == 0) Tuning.FIRST_BOSS_METRES else Tuning.BOSS_INTERVAL_METRES)).coerceIn(0f, 1f)

    init {
        initPlayerBoxes()
        player.maxHealth = setup.maxHealth
        player.health = setup.maxHealth
        powerUps.shield = setup.startShield
        startX = player.x
        cameraX = player.x - Tuning.PLAYER_SCREEN_X
        prevCameraX = cameraX
        // nothing is ever generated inside the first visible screen
        generator.nextX = cameraX + viewWidth
        baseSpeed = Tuning.START_SPEED * (1f + (def.difficulty - 1f) * 0.3f)
        player.play(PlayerAnim.IDLE)
    }

    private fun initPlayerBoxes() {
        val id = setup.character.id
        val stand = sprites.region("characters/$id/running/00")
        collisionRect(stand, 0f, 0f, Tuning.PLAYER_SCALE, Tuning.COLLISION_FORGIVENESS, false, standBox)
        standBox.y = max(standBox.y, 2f)
        val slideName = sprites.anim("characters/$id/sliding").frames.let { it[it.size / 2] }
        collisionRect(sprites.region(slideName), 0f, 0f, Tuning.PLAYER_SCALE, Tuning.COLLISION_FORGIVENESS, false, slideBox)
        slideBox.y = max(slideBox.y, 2f)
        // keep the slide box clearly under the high lane so sliding always clears hanging hazards
        if (slideBox.y + slideBox.height > HIGH_LANE_BOTTOM - 10f) slideBox.height = HIGH_LANE_BOTTOM - 10f - slideBox.y
        if (standBox.y + standBox.height < HIGH_LANE_BOTTOM + 25f) standBox.height = HIGH_LANE_BOTTOM + 25f - standBox.y
    }

    fun standHeight() = standBox.y + standBox.height
    fun slideHeight() = slideBox.y + slideBox.height

    // ----------------------------------------------------------------- input (called by the screen)
    fun pressJump() { jumpQueued = true; player.jumpHeld = true }
    fun releaseJump() {
        player.jumpHeld = false
        if (!player.grounded && player.vy > 0f && !powerUps.active(PowerUpType.JETPACK)) player.vy *= Tuning.JUMP_CUT_FACTOR
    }
    fun pressSlide() { slideQueued = true }
    fun pressAttack() { attackQueued = true }

    // ----------------------------------------------------------------- state machine
    private fun setState(s: RunState) {
        if (state == s) return
        val old = state
        state = s
        stateTime = 0f
        listener.stateChanged(old, s)
    }

    fun start() { if (state == RunState.READY) setState(RunState.RUNNING) }

    fun pause() {
        if (state == RunState.PAUSED || state == RunState.RESULTS || state == RunState.REVIVE_OFFER || state == RunState.VICTORY) return
        stateBeforePause = state
        setState(RunState.PAUSED)
    }

    fun resume() { if (state == RunState.PAUSED) { accumulator = 0f; setState(stateBeforePause) } }

    // ---- debug/test hooks (desktop screenshot tour and tests only)
    fun debugBossSoon() { nextBossMetres = metres + 25f }
    fun debugKill(keepExtraLife: Boolean = false) { if (!player.dead) { if (!keepExtraLife) powerUps.extraLife = false; die() } }

    /** Victory overlay "continue": resume the run after a boss. */
    fun continueAfterBoss() {
        if (state != RunState.VICTORY) return
        boss = null
        generationPaused = false
        generator.nextX = max(generator.nextX, cameraX + viewWidth + 300f)
        setState(RunState.RUNNING)
    }

    /** Player accepted the extra-life revive. */
    fun revive(): Boolean {
        if (state != RunState.REVIVE_OFFER || !powerUps.extraLife) return false
        powerUps.extraLife = false
        revives++
        player.dead = false
        player.health = max(2, player.maxHealth / 2).coerceAtMost(player.maxHealth)
        player.invulnerable = Tuning.REVIVE_INVULNERABLE
        player.vy = 0f
        player.y = player.groundTop
        player.play(PlayerAnim.RUN)
        // clear hazards near the runner so the revive is safe
        val clearTo = player.x + viewWidth
        obstacles.each { o -> if (o.x < clearTo && o.def.kind != ObstacleKind.BOUNCE) { o.dead = true; addEffect("effects/smoke", o.x, o.y, 0.6f) } }
        enemies.each { e -> if (e.x < clearTo) e.alive = false }
        projectiles.each { p -> if (!p.friendly) p.alive = false }
        addEffect("effects/shield_activation", player.x, player.y + 40f, 0.8f, follow = true)
        listener.sfx(Sfx.REVIVE)
        setState(if (boss != null && !boss!!.defeated) RunState.BOSS else RunState.RUNNING)
        return true
    }

    /** Player declined the revive or had none: build the report exactly once. */
    fun finish(): RunReport {
        report?.let { return it }
        val r = RunReport(def.id, setup.character.id, score.toInt(), metres.toInt(), coins, gems, keys, chests, tokens, stars,
            enemiesDefeated, slidesUnder, powerUpsUsed, bossesDefeated.toList(), gateBossDefeated, revives, runTime)
        report = r
        setState(RunState.RESULTS)
        return r
    }

    /** End the run from the victory or pause overlay (player chose Home). */
    fun quit(): RunReport = finish()

    // ----------------------------------------------------------------- main loop
    fun update(frameDelta: Float) {
        val dt = min(frameDelta, Tuning.MAX_FRAME_DELTA)
        if (state == RunState.PAUSED || state == RunState.RESULTS) { alpha = 1f; return }
        accumulator += dt
        var steps = 0
        while (accumulator >= Tuning.STEP && steps < Tuning.MAX_STEPS_PER_FRAME) {
            step(Tuning.STEP)
            accumulator -= Tuning.STEP
            steps++
        }
        if (steps == Tuning.MAX_STEPS_PER_FRAME) accumulator = 0f   // drop excess instead of spiralling
        alpha = accumulator / Tuning.STEP
    }

    fun step(dt: Float) {
        prevCameraX = cameraX
        player.prevX = player.x
        player.prevY = player.y
        stateTime += dt
        autoPilot?.think(this)
        when (state) {
            RunState.READY -> { player.animTime += dt; updateEffects(dt); return }
            RunState.VICTORY, RunState.REVIVE_OFFER, RunState.RESULTS, RunState.PAUSED -> { player.animTime += dt; updateEffects(dt); updateTexts(dt); return }
            else -> {}
        }
        runTime += dt
        updateSpeed(dt)
        updatePowerUps(dt)
        if (state != RunState.DYING) updatePlayer(dt) else updateDying(dt)
        cameraX = player.x - Tuning.PLAYER_SCREEN_X
        if (state == RunState.RUNNING || state == RunState.BOSS_WARNING) {
            generator.fill(cameraX + viewWidth + Tuning.SPAWN_AHEAD, baseSpeed, metres, runTime)
        }
        updateObstacles(dt)
        updateEnemies(dt)
        updateBoss(dt)
        updateProjectiles(dt)
        updatePickups(dt)
        updateEffects(dt)
        updateTexts(dt)
        updateHints(dt)
        if (state != RunState.DYING) collide()
        if (state == RunState.RUNNING || state == RunState.BOSS || state == RunState.BOSS_WARNING) {
            score += speed * dt / Tuning.PX_PER_METRE * Tuning.SCORE_PER_METRE * scoreMultiplier
        }
        if (state == RunState.RUNNING && metres >= nextBossMetres) beginBossWarning()
        if (state == RunState.BOSS_WARNING && stateTime >= Tuning.BOSS_WARNING_TIME && areaClear()) spawnBoss()
        if (state == RunState.BOSS_DEFEATED && stateTime > 2.6f) setState(RunState.VICTORY)
        despawn()
        jumpQueued = false; slideQueued = false; attackQueued = false
        if (releaseJumpLater) { releaseJumpLater = false; player.jumpHeld = false }
    }

    private fun updateSpeed(dt: Float) {
        baseSpeed = min(Tuning.MAX_SPEED, Tuning.START_SPEED * (1f + (def.difficulty - 1f) * 0.3f) + metres * Tuning.SPEED_PER_METRE)
        val target = when (state) {
            RunState.BOSS, RunState.BOSS_WARNING -> Tuning.BOSS_SPEED_FACTOR
            RunState.DYING -> 0f
            else -> 1f
        }
        speedFactor += (target - speedFactor) * min(1f, dt * 2.5f)
        var s = baseSpeed * speedFactor
        if (powerUps.active(PowerUpType.SPEED_BOOST)) s *= 1.45f
        if (player.inQuicksand) s *= 0.6f
        if (player.slowTimer > 0f) { s *= 0.8f; player.slowTimer -= dt }
        if (state == RunState.BOSS_DEFEATED) s = baseSpeed * speedFactor
        speed = s
    }

    private fun updatePowerUps(dt: Float) {
        expired.clear()
        powerUps.update(dt, expired)
        for (t in expired) {
            if (t == PowerUpType.JETPACK || t == PowerUpType.SPEED_BOOST || t == PowerUpType.INVINCIBILITY) powerUps.grace = Tuning.POST_EFFECT_GRACE
            listener.powerUp(t, false)
        }
    }

    // ----------------------------------------------------------------- player
    private fun updatePlayer(dt: Float) {
        val p = player
        p.animTime += dt
        p.x += speed * dt
        if (p.invulnerable > 0f) p.invulnerable -= dt
        if (p.hurtTimer > 0f) p.hurtTimer -= dt
        if (p.landTimer > 0f) p.landTimer -= dt
        if (jumpQueued) p.jumpBuffer = Tuning.JUMP_BUFFER else if (p.jumpBuffer > 0f) p.jumpBuffer -= dt
        if (p.grounded) { p.coyote = Tuning.COYOTE_TIME; p.airJumpsUsed = 0 } else if (p.coyote > 0f) p.coyote -= dt

        val jetpack = powerUps.active(PowerUpType.JETPACK)
        val superJump = powerUps.active(PowerUpType.DOUBLE_JUMP)
        val maxAir = if (superJump) 2 else 1
        val jumpScale = if (p.inQuicksand) 0.82f else 1f

        if (jetpack) {
            if (p.jumpHeld) p.vy += Tuning.JETPACK_THRUST * dt
            p.vy -= Tuning.GRAVITY * 0.55f * dt
            p.vy = p.vy.coerceIn(-420f, 520f)
            if (p.slideTimer > 0f) p.slideTimer = 0f
            if (p.y + p.vy * dt > Tuning.GROUND_Y + Tuning.JETPACK_CEILING) { p.vy = min(0f, p.vy); }
            p.jumpBuffer = 0f
        } else if (p.jumpBuffer > 0f) {
            if (p.grounded || p.coyote > 0f) {
                p.vy = Tuning.JUMP_VELOCITY * p.jumpScale * jumpScale
                p.grounded = false; p.coyote = 0f; p.jumpBuffer = 0f; p.slideTimer = 0f
                p.play(PlayerAnim.JUMP)
                listener.sfx(Sfx.JUMP)
                addEffect("effects/jump_dust", p.x - 10f, p.y, 0.45f)
            } else if (p.airJumpsUsed < maxAir) {
                p.airJumpsUsed++
                p.vy = Tuning.DOUBLE_JUMP_VELOCITY * (if (superJump) 1.25f else 1f) * jumpScale
                p.jumpBuffer = 0f
                p.play(PlayerAnim.DOUBLE_JUMP)
                listener.sfx(Sfx.DOUBLE_JUMP, pitch = 1f + p.airJumpsUsed * 0.08f)
                addEffect("effects/jump_dust", p.x, p.y, 0.4f)
            }
        }

        if (slideQueued && !jetpack) {
            if (p.grounded) {
                if (!p.sliding) listener.sfx(Sfx.SLIDE)
                p.slideTimer = p.slideDuration
                p.play(PlayerAnim.SLIDE)
                addEffect("effects/running_dust", p.x - 30f, p.y, 0.5f)
            } else {
                // fast-fall, then slide on landing
                p.vy = min(p.vy, -1300f)
                p.slideBuffered = true
            }
        }
        if (p.sliding) {
            p.slideTimer -= dt
            if (p.slideTimer <= 0f) { p.slideTimer = 0f; if (p.grounded) p.play(PlayerAnim.RUN) }
        }

        // gravity
        if (!jetpack) {
            val g = Tuning.GRAVITY * (if (p.vy < 0f) p.fallGravityScale else 1f)
            p.vy -= g * dt
            if (p.vy < -Tuning.MAX_FALL_SPEED) p.vy = -Tuning.MAX_FALL_SPEED
        }
        val prevFeet = p.y
        p.y += p.vy * dt

        // ground / surfaces
        val surface = findSurface(prevFeet, p.y)
        val wasGrounded = p.grounded
        if (surface != null && p.vy <= 0f) {
            p.y = surface
            p.groundTop = surface
            if (!wasGrounded) {
                p.grounded = true
                p.vy = 0f
                if (p.slideBuffered) {
                    p.slideBuffered = false
                    p.slideTimer = p.slideDuration
                    p.play(PlayerAnim.SLIDE)
                    listener.sfx(Sfx.SLIDE)
                } else {
                    p.landTimer = Tuning.LAND_ANIM_TIME
                    p.play(PlayerAnim.LAND)
                    listener.sfx(Sfx.LAND)
                }
                addEffect("effects/landing_impact", p.x, p.y, 0.4f)
            }
            p.grounded = true
            p.vy = 0f
        } else {
            p.grounded = false
        }
        if (p.y < Tuning.GROUND_Y - 400f) p.y = Tuning.GROUND_Y   // safety net, the ground is continuous

        // animation selection
        if (p.hurtTimer > 0f) p.play(PlayerAnim.HURT)
        else if (!p.grounded) {
            if (p.vy < -80f && p.anim != PlayerAnim.DOUBLE_JUMP) p.play(PlayerAnim.FALL)
            else if (p.anim == PlayerAnim.DOUBLE_JUMP && p.animTime > 0.45f && p.vy < 0f) p.play(PlayerAnim.FALL)
            else if (p.anim != PlayerAnim.JUMP && p.anim != PlayerAnim.DOUBLE_JUMP && p.anim != PlayerAnim.FALL) p.play(PlayerAnim.JUMP)
        } else if (p.sliding) p.play(PlayerAnim.SLIDE)
        else if (p.landTimer > 0f) p.play(PlayerAnim.LAND)
        else if (state == RunState.BOSS_DEFEATED) p.play(PlayerAnim.RUN)
        else p.play(PlayerAnim.RUN)

        if (p.grounded && !p.sliding && MathUtils.randomBoolean(dt * 4f) && setup.quality > 0) addEffect("effects/running_dust", p.x - 36f, p.y, 0.3f)
        if (powerUps.active(PowerUpType.SPEED_BOOST) && setup.quality > 0 && MathUtils.randomBoolean(dt * 10f)) addEffect("effects/speed_trails", p.x - 60f, p.y + 50f, 0.5f)
        p.inQuicksand = false
        p.updateBox(standBox, slideBox)

        // energy attack
        if (energyCooldown > 0f) energyCooldown -= dt
        if (attackQueued && canAttack && energyCooldown <= 0f) {
            energyCooldown = Tuning.ENERGY_COOLDOWN
            val b = obtainProjectile("electric_sparks", p.x + 50f, p.y + standHeight() * 0.55f, Tuning.ENERGY_SPEED + speed, 0f, true)
            b.tint = 0x7FE8FFFF; b.scale = 0.42f; b.halfW = 26f; b.halfH = 20f
            listener.sfx(Sfx.ENERGY_SHOT)
        }
    }

    /** Highest standable surface crossed this step (ground, platforms, solid tops). */
    private fun findSurface(prevFeet: Float, feet: Float): Float? {
        val p = player
        var best: Float? = null
        val left = p.x - PLAYER_HALF_WIDTH * 0.7f
        val right = p.x + PLAYER_HALF_WIDTH * 0.7f
        fun consider(top: Float, x0: Float, x1: Float) {
            if (right < x0 || left > x1) return
            if (prevFeet >= top - 6f && feet <= top + 0.5f) { if (best == null || top > best!!) best = top }
            else if (p.grounded && abs(prevFeet - top) < 2f) { if (best == null || top > best!!) best = top }
        }
        consider(Tuning.GROUND_Y, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
        platforms.each { pl -> consider(pl.top, pl.x, pl.x + pl.width) }
        obstacles.each { o ->
            if (o.dead) return@each
            when (o.def.kind) {
                ObstacleKind.SOLID, ObstacleKind.COLLAPSING, ObstacleKind.MOVING_PLATFORM -> {
                    consider(o.standTop, o.box.x, o.box.x + o.box.width)
                    if (o.def.kind == ObstacleKind.COLLAPSING && best == o.standTop && o.collapseTimer < 0f && feet <= o.standTop + 0.5f) {
                        o.collapseTimer = if (o.def.id == "broken_bridge") 0.35f else 0.6f
                    }
                }
                else -> {}
            }
        }
        return best
    }

    private fun updateDying(dt: Float) {
        val p = player
        p.animTime += dt
        p.x += speed * dt
        if (p.y > p.groundTop) { p.vy -= Tuning.GRAVITY * dt; p.y = max(p.groundTop, p.y + p.vy * dt) }
        if (stateTime > 1.5f) {
            if (powerUps.extraLife) setState(RunState.REVIVE_OFFER) else finish()
        }
    }

    // ----------------------------------------------------------------- damage
    private val invulnerableNow get() = player.invulnerable > 0f || powerUps.active(PowerUpType.INVINCIBILITY) || powerUps.active(PowerUpType.JETPACK) || powerUps.grace > 0f || invincibleCheat
    private val smashing get() = powerUps.active(PowerUpType.INVINCIBILITY) || powerUps.active(PowerUpType.SPEED_BOOST) || powerUps.active(PowerUpType.JETPACK)

    /** Returns true if the hit was taken (and the source should react). */
    /** Debug: what caused the most recent hit (used by tests and the debug overlay). */
    var lastHitBy = ""
        private set
    var hitsTaken = 0
        private set

    private fun hurt(source: String = ""): Boolean {
        if (invulnerableNow || player.dead) return false
        hitsTaken++
        lastHitBy = source + " @" + metres.toInt() + "m py=" + player.y.toInt() + " vy=" + player.vy.toInt() + " slide=" + player.sliding
        if (powerUps.shield) {
            powerUps.shield = false
            player.invulnerable = 0.8f
            listener.sfx(Sfx.SHIELD_BREAK)
            addEffect("effects/shield_activation", player.x, player.y + 40f, 0.7f, follow = true)
            listener.shake(4f)
            return true
        }
        player.health--
        listener.vibrate(60)
        listener.shake(9f)
        addEffect("effects/damage_flash", player.x, player.y + 30f, 0.6f, follow = true)
        if (player.health <= 0) {
            die()
        } else {
            listener.sfx(Sfx.DAMAGE)
            player.invulnerable = player.invulnerableTime
            player.hurtTimer = 0.28f
            player.slideTimer = 0f
        }
        return true
    }

    private fun die() {
        player.dead = true
        player.health = 0
        player.play(PlayerAnim.DEATH)
        player.slideTimer = 0f
        powerUps.clearTimed()
        listener.sfx(Sfx.DEATH)
        listener.vibrate(200)
        setState(RunState.DYING)
    }

    // ----------------------------------------------------------------- collisions
    private fun collide() {
        val p = player
        val pb = p.box
        obstacles.each { o ->
            if (o.dead || !o.box.overlaps(pb)) return@each
            when (o.def.kind) {
                ObstacleKind.SOLID -> {
                    if (p.y >= o.standTop - 6f) return@each  // standing on it
                    if (smashing) { breakObstacle(o); return@each }
                    if (!o.hitPlayer && hurt(o.def.id)) { o.hitPlayer = true; breakObstacle(o) }
                }
                ObstacleKind.DAMAGE, ObstacleKind.ROLLER, ObstacleKind.OSCILLATOR, ObstacleKind.HANGING, ObstacleKind.FALLING -> {
                    if (smashing && o.def.kind != ObstacleKind.HANGING) { breakObstacle(o); return@each }
                    if (!o.hitPlayer && hurt(o.def.id)) o.hitPlayer = true
                }
                ObstacleKind.TIMED -> if (o.active && !o.hitPlayer && hurt(o.def.id)) o.hitPlayer = true
                ObstacleKind.BREAKABLE -> {
                    if (p.vy < 0f && p.y > o.box.y + o.box.height * 0.5f || smashing) {
                        breakObstacle(o, explode = true)
                        if (!smashing) { p.vy = STOMP_BOUNCE; p.grounded = false; p.airJumpsUsed = 0 }
                        score += Tuning.SCORE_BREAK * scoreMultiplier
                        floatText("+${(Tuning.SCORE_BREAK * scoreMultiplier).toInt()}", o.x, o.box.y + o.box.height + 20f, 0xFFD45AFF.toInt())
                    } else if (!o.hitPlayer) { o.hitPlayer = true; hurt(o.def.id); breakObstacle(o, explode = true) }
                }
                ObstacleKind.KNOCKABLE -> {
                    o.dead = true; o.vx = speed + 380f; o.vy = 520f
                    p.slowTimer = 0.4f
                    listener.sfx(Sfx.LAND, pitch = 1.4f)
                }
                ObstacleKind.SLOWDOWN -> p.inQuicksand = true
                ObstacleKind.BOUNCE -> if (p.vy <= 0f && !o.triggered) {
                    o.triggered = true
                    p.vy = SPRING_VELOCITY; p.grounded = false; p.airJumpsUsed = 0; p.slideTimer = 0f
                    p.play(PlayerAnim.JUMP)
                    listener.sfx(Sfx.SPRING)
                }
                ObstacleKind.COLLAPSING, ObstacleKind.MOVING_PLATFORM -> {}
            }
        }
        // slide-under bookkeeping
        for (i_o in 0 until obstacles.size) { val o = obstacles[i_o]; if (!o.passed && o.def.kind == ObstacleKind.HANGING && o.box.x + o.box.width < pb.x) {
            o.passed = true
            if (!o.hitPlayer) slidesUnder++
        } }
        enemies.each { e ->
            if (!e.alive || !e.solid || !e.box.overlaps(pb)) return@each
            val stomp = p.vy < 0f && p.y > e.box.y + e.box.height * 0.45f && e.def.stompable
            if (stomp || smashing) {
                defeatEnemy(e)
                if (stomp) { p.vy = STOMP_BOUNCE; p.grounded = false; p.airJumpsUsed = 0 }
            } else if (hurt(e.def.id)) {
                e.set(EnemyState.ATTACK)
                listener.sfx(Sfx.ENEMY_ATTACK)
            }
        }
        for (i_e in 0 until enemies.size) { val e = enemies[i_e]; if (e.alive && !e.passed && e.box.x + e.box.width < pb.x) {
            e.passed = true
            if (e.def.flying && p.sliding) slidesUnder++
        } }
        projectiles.each { pr ->
            if (!pr.alive) return@each
            if (pr.friendly) {
                boss?.let { b ->
                    if (!b.defeated && b.state != BossState.ENTER && pr.box.overlaps(b.box)) { pr.alive = false; damageBoss(b, pr.x, pr.y) }
                }
                for (qi in 0 until projectiles.size) {
                    val q = projectiles[qi]
                    if (q.alive && !q.friendly && q.box.overlaps(pr.box)) {
                        q.alive = false; pr.alive = false
                        addEffect("effects/explosion", q.x, q.y - 40f, 0.4f)
                    }
                }
                enemies.each { e -> if (e.alive && e.box.overlaps(pr.box)) { pr.alive = false; defeatEnemy(e) } }
                obstacles.each { o -> if (!o.dead && o.def.kind == ObstacleKind.BREAKABLE && o.box.overlaps(pr.box)) { pr.alive = false; breakObstacle(o, explode = true) } }
            } else if (pr.box.overlaps(pb)) {
                if (smashing) { pr.alive = false; addEffect("effects/smoke", pr.x, pr.y - 30f, 0.4f); return@each }
                if (hurt("projectile")) {
                    pr.alive = false
                    addEffect(if (pr.fx == "ice_shard") "effects/ice_particles" else "effects/explosion", pr.x, pr.y - 40f, 0.45f)
                }
            }
        }
        pickups.each { pk -> if (!pk.collected && pk.box.overlaps(pb)) collect(pk) }
    }

    private fun breakObstacle(o: Obstacle, explode: Boolean = false) {
        o.dead = true
        when (o.def.id) {
            "snowball", "ice_spikes", "crystal_cluster", "stalactite" -> addEffect("effects/ice_particles", o.x, o.y, 0.6f)
            "electric_barrier", "laser_barrier", "mechanical_trap" -> addEffect("effects/electric_sparks", o.x, o.y + 20f, 0.6f)
            "fire_pit", "lava_pool" -> addEffect("effects/fire_particles", o.x, o.y, 0.6f)
        }
        if (explode || o.def.kind == ObstacleKind.BREAKABLE) {
            addEffect("effects/explosion", o.x, o.y, 0.7f)
            listener.sfx(Sfx.EXPLOSION)
            listener.shake(6f)
        } else {
            addEffect("effects/smoke", o.x, o.y, 0.6f)
            listener.sfx(Sfx.ENEMY_DEFEAT, 0.6f, 1.3f)
        }
    }

    private fun defeatEnemy(e: Enemy) {
        when (e.def.id) {
            "fire_monster" -> addEffect("effects/fire_particles", e.x, e.y + 20f, 0.7f)
            "ice_creature" -> addEffect("effects/ice_particles", e.x, e.y + 20f, 0.7f)
            "robot", "drone" -> addEffect("effects/electric_sparks", e.x, e.y + 30f, 0.6f)
            "shadow_monster" -> addEffect("effects/smoke", e.x, e.y + 30f, 0.7f)
        }
        e.alive = false
        e.set(EnemyState.DAMAGE)   // brief hit reaction, then the death sequence
        e.vy = 300f
        enemiesDefeated++
        val pts = (Tuning.SCORE_ENEMY * scoreMultiplier).toInt()
        score += pts
        floatText("+$pts", e.x, e.box.y + e.box.height + 20f, 0xFFD45AFF.toInt())
        addEffect("effects/coin_sparkle", e.x, e.y + 20f, 0.6f)
        listener.sfx(Sfx.ENEMY_DEFEAT)
        listener.shake(3f)
    }

    // ----------------------------------------------------------------- pickups
    private fun collect(pk: Pickup) {
        pk.collected = true
        val p = player
        pk.powerUp?.let { t ->
            activatePowerUp(t)
            addEffect("powerups/${t.id}/activation", p.x, p.y + 40f, 0.8f, follow = true)
            return
        }
        val c = pk.collectible ?: return
        val mult = if (powerUps.active(PowerUpType.DOUBLE_COINS)) 2 else 1
        when (c) {
            CollectibleType.SILVER_COIN, CollectibleType.GOLD_COIN, CollectibleType.DIAMOND_COIN -> {
                val v = when (c) { CollectibleType.SILVER_COIN -> 1; CollectibleType.GOLD_COIN -> 5; else -> 25 } * mult
                coins += v
                score += v * 10f * scoreMultiplier
                listener.sfx(Sfx.COIN, pitch = if (c == CollectibleType.SILVER_COIN) 1f else 1.15f)
                if (setup.quality > 0 || c != CollectibleType.SILVER_COIN) addEffect("effects/coin_sparkle", pk.x, pk.y - 30f, 0.35f)
                if (v >= 5) floatText("+$v", pk.x, pk.y + 30f, 0xFFD45AFF.toInt())
            }
            CollectibleType.GEM -> { gems++; listener.sfx(Sfx.GEM); floatText("+1 gem", pk.x, pk.y + 30f, 0x7FE8FFFF); addEffect("effects/coin_sparkle", pk.x, pk.y - 30f, 0.6f) }
            CollectibleType.STAR -> {
                stars++; val pts = (Tuning.SCORE_STAR * scoreMultiplier).toInt(); score += pts
                listener.sfx(Sfx.GEM, pitch = 1.2f); floatText("+$pts", pk.x, pk.y + 30f, 0xFFE46BFF.toInt())
                addEffect("effects/coin_sparkle", pk.x, pk.y - 30f, 0.7f)
            }
            CollectibleType.KEY -> { keys++; listener.sfx(Sfx.GEM, pitch = 0.9f); floatText("+1 key", pk.x, pk.y + 30f, 0xFFE46BFF.toInt()) }
            CollectibleType.TREASURE_CHEST -> { chests++; listener.sfx(Sfx.CHEST_OPEN, 0.6f); floatText("Chest! Open it with a key", pk.x, pk.y + 40f, 0xFFE46BFF.toInt()) }
            CollectibleType.REWARD_BAG -> {
                val v = (20 + rng.nextInt(21)) * mult; coins += v
                listener.sfx(Sfx.COIN, pitch = 0.8f); floatText("+$v coins", pk.x, pk.y + 30f, 0xFFD45AFF.toInt())
            }
            CollectibleType.BONUS_TOKEN -> {
                tokens++; val pts = (Tuning.SCORE_TOKEN * scoreMultiplier).toInt(); score += pts
                listener.sfx(Sfx.GEM, pitch = 1.4f); floatText("+$pts token", pk.x, pk.y + 30f, 0xC79BF2FF.toInt())
            }
            CollectibleType.MYSTERY_BOX -> {
                val r = MysteryBox.roll(rng)
                coins += r.coins; gems += r.gems; keys += r.keys; tokens += r.tokens
                listener.sfx(Sfx.CHEST_OPEN)
                addEffect("effects/unlock_celebration", pk.x, pk.y - 40f, 0.6f)
                floatText(r.describe(), pk.x, pk.y + 40f, 0xFFE46BFF.toInt())
                listener.reward("Mystery box", r)
            }
        }
    }

    fun activatePowerUp(t: PowerUpType) {
        powerUpsUsed++
        listener.sfx(Sfx.POWERUP_PICKUP)
        listener.sfx(Sfx.POWERUP_ACTIVATE, 0.6f)
        when (t) {
            PowerUpType.PROTECTIVE_SHIELD -> if (powerUps.shield) score += 200 * scoreMultiplier else powerUps.shield = true
            PowerUpType.EXTRA_LIFE -> if (powerUps.extraLife) score += 300 * scoreMultiplier else powerUps.extraLife = true
            PowerUpType.HEALTH_RECOVERY -> if (player.health < player.maxHealth) player.health++ else score += 100 * scoreMultiplier
            PowerUpType.JETPACK -> { powerUps.activate(t); player.vy = max(player.vy, 400f); player.grounded = false; player.slideTimer = 0f }
            else -> powerUps.activate(t)
        }
        if (t == PowerUpType.PROTECTIVE_SHIELD) addEffect("effects/shield_activation", player.x, player.y + 40f, 0.7f, follow = true)
        floatText(t.title, player.x, player.y + standHeight() + 40f, 0x8DE2D8FF.toInt())
        listener.powerUp(t, true)
    }

    // ----------------------------------------------------------------- spawning API (used by PatternGenerator)
    // metadata caches so per-step updates never build lookup strings
    private val obstacleMetaCache = HashMap<String, com.bramble.dash.assets.RegionMeta>()
    private val enemyMetaCache = HashMap<String, com.bramble.dash.assets.RegionMeta>()
    private val bossMetaCache = HashMap<String, com.bramble.dash.assets.RegionMeta>()
    private fun obstacleMeta(id: String) = obstacleMetaCache.getOrPut(id) { sprites.region(sprites.anim("obstacles/$id").frames[0]) }
    private fun enemyMeta(id: String) = enemyMetaCache.getOrPut(id) { sprites.region(sprites.anim("enemies/$id/idle").frames[0]) }
    private fun bossMeta(id: String) = bossMetaCache.getOrPut(id) { sprites.region(sprites.anim("bosses/$id/idle").frames[0]) }

    /** Width and height of an obstacle's tuned collider, for spacing and validation. */
    fun obstacleExtent(id: String): Pair<Float, Float> {
        val d = Obstacles.byId(id)
        collisionRect(obstacleMeta(id), 0f, 0f, Tuning.OBSTACLE_SCALE, Tuning.COLLISION_FORGIVENESS, false, tmp, d.widthScale, d.heightScale)
        return tmp.width to (tmp.y + tmp.height)
    }

    fun enemyExtent(id: String): Pair<Float, Float> {
        val m = sprites.region(sprites.anim("enemies/$id/idle").frames[0])
        collisionRect(m, 0f, 0f, Tuning.ENEMY_SCALE, Tuning.COLLISION_FORGIVENESS, true, tmp)
        return tmp.width to (tmp.y + tmp.height)
    }

    fun spawnObstacle(id: String, x: Float, bottomY: Float): Obstacle {
        val o = obstaclePool.obtain()
        o.def = Obstacles.byId(id)
        o.x = x; o.baseX = x
        o.time = rng.nextFloat() * 2f
        // position so that the collider's bottom sits at bottomY
        collisionRect(obstacleMeta(id), x, 0f, Tuning.OBSTACLE_SCALE, Tuning.COLLISION_FORGIVENESS, false, tmp, o.def.widthScale, o.def.heightScale)
        o.y = bottomY - tmp.y
        o.baseY = o.y
        o.active = o.def.kind != ObstacleKind.TIMED
        if (o.def.kind == ObstacleKind.TIMED) o.phase = rng.nextFloat() * 2.5f
        obstacles.add(o)
        updateObstacleBox(o)
        o.standTop = o.box.y + o.box.height
        return o
    }

    fun spawnRoller(id: String, x: Float, rollSpeed: Float, activation: Float) {
        val o = spawnObstacle(id, x, Tuning.GROUND_Y)
        o.rollSpeed = rollSpeed
        o.activationDist = activation
    }

    fun spawnFalling(id: String, landX: Float, speedAtSpawn: Float) {
        val o = spawnObstacle(id, landX, Tuning.GROUND_Y)
        o.landed = false
        o.y = o.baseY + 720f
        // trigger so it lands ~0.65 s before the runner arrives
        val fall = kotlin.math.sqrt(2f * 720f / Tuning.GRAVITY)
        o.activationDist = speedAtSpawn * (fall + 0.65f) + 60f
    }

    fun spawnPlatform(x: Float, top: Float, tiles: Int) {
        val pl = platformPool.obtain()
        pl.x = x; pl.top = top; pl.tiles = tiles; pl.width = tiles * PLATFORM_TILE_W
        platforms.add(pl)
    }

    fun spawnEnemy(id: String, x: Float, speedAtSpawn: Float, shotMeetX: Float = 0f): Enemy {
        val e = enemyPool.obtain()
        e.def = Enemies.byId(id)
        e.x = x; e.baseX = x
        e.time = rng.nextFloat() * 3f
        val m = sprites.region(sprites.anim("enemies/$id/idle").frames[0])
        collisionRect(m, x, 0f, Tuning.ENEMY_SCALE, Tuning.COLLISION_FORGIVENESS, true, tmp)
        e.y = when (e.def.behavior) {
            EnemyBehavior.SWOOPER, EnemyBehavior.ZAPPER -> Tuning.GROUND_Y + HIGH_LANE_BOTTOM + 8f - tmp.y
            EnemyBehavior.PHASER -> Tuning.GROUND_Y + HIGH_LANE_BOTTOM + 4f - tmp.y
            EnemyBehavior.DANGLER -> Tuning.GROUND_Y + HIGH_LANE_BOTTOM + 40f - tmp.y
            else -> Tuning.GROUND_Y
        }
        e.baseY = e.y
        val walk = when (e.def.behavior) { EnemyBehavior.HOPPER -> 50f; EnemyBehavior.SWOOPER -> 40f; else -> 0f }
        if (walk > 0f) {
            e.activationDist = viewWidth - Tuning.PLAYER_SCREEN_X + 100f
            e.chargeSpeed = walk
            e.x += e.activationDist * walk / (speedAtSpawn + walk)
            e.baseX = e.x
        }
        if (e.def.behavior == EnemyBehavior.LUNGER) { e.x += LUNGE_DISTANCE; e.baseX = e.x }
        if (shotMeetX != 0f) {
            // fire so the shot meets the runner at shotMeetX
            val vp = speedAtSpawn * 1.4f
            e.fireDistance = (x - shotMeetX) * (1f + speedAtSpawn / vp)
            e.chargeSpeed = vp
        }
        e.set(if (e.def.flying) EnemyState.MOVE else EnemyState.IDLE)
        enemies.add(e)
        updateEnemyBox(e)
        return e
    }

    fun spawnCharger(id: String, x: Float, chargeSpeed: Float, activation: Float) {
        val e = spawnEnemy(id, x, speed)
        e.chargeSpeed = chargeSpeed
        e.activationDist = activation
    }

    fun spawnCollectible(t: CollectibleType, x: Float, y: Float) {
        val pk = pickupPool.obtain()
        pk.collectible = t; pk.x = x; pk.y = y; pk.baseY = y; pk.time = rng.nextFloat()
        pickups.add(pk)
    }

    fun spawnPowerUp(t: PowerUpType, x: Float, y: Float) {
        val pk = pickupPool.obtain()
        pk.powerUp = t; pk.x = x; pk.y = y; pk.baseY = y
        pickups.add(pk)
    }

    fun addHint(x: Float, text: String) { hints += x to text }

    fun onTutorialPatternsDone(x: Float) { hints += x to "" }

    // ----------------------------------------------------------------- per-entity updates
    private fun updateObstacleBox(o: Obstacle) {
        val m = obstacleMeta(o.def.id)
        collisionRect(m, o.x, o.y, Tuning.OBSTACLE_SCALE, Tuning.COLLISION_FORGIVENESS, false, o.box, o.def.widthScale, o.def.heightScale)
        if (o.def.kind == ObstacleKind.COLLAPSING || o.def.kind == ObstacleKind.MOVING_PLATFORM) {
            // a thin standable top instead of a full-height box
            o.box.y = o.box.y + o.box.height - 16f; o.box.height = 16f
            o.standTop = o.box.y + o.box.height
        }
        if (o.def.kind == ObstacleKind.SOLID || o.def.kind == ObstacleKind.BREAKABLE) o.standTop = o.box.y + o.box.height
    }

    private fun updateObstacles(dt: Float) {
        val px = player.x
        obstacles.each { o ->
            o.time += dt
            if (o.dead) {
                o.deadTime += dt
                if (o.def.kind == ObstacleKind.KNOCKABLE || o.def.kind == ObstacleKind.COLLAPSING) {
                    o.x += o.vx * dt; o.vy -= Tuning.GRAVITY * dt; o.y += o.vy * dt
                }
                return@each
            }
            when (o.def.kind) {
                ObstacleKind.ROLLER -> if (!o.triggered && o.x - px <= o.activationDist) {
                    o.triggered = true; warn(o.box.y + o.box.height * 0.5f)
                } else if (o.triggered) o.x -= o.rollSpeed * dt
                ObstacleKind.OSCILLATOR -> o.x = o.baseX + sin(o.time * 3.6f) * 55f
                ObstacleKind.TIMED -> {
                    val cycle = (o.time + o.phase) % 2.6f
                    o.warning = cycle in 1.1f..1.6f
                    o.active = cycle > 1.6f
                    if (o.active && cycle - dt <= 1.6f && abs(o.x - px) < viewWidth) listener.sfx(Sfx.ENEMY_ATTACK, 0.35f, 1.5f)
                }
                ObstacleKind.FALLING -> {
                    if (!o.triggered && o.x - px <= o.activationDist) { o.triggered = true; o.warning = true }
                    if (o.triggered && !o.landed) {
                        o.vy -= Tuning.GRAVITY * dt
                        o.y += o.vy * dt
                        if (o.y <= o.baseY) {
                            o.y = o.baseY; o.landed = true; o.warning = false
                            listener.shake(5f); listener.sfx(Sfx.LAND, 1f, 0.6f)
                            addEffect("effects/landing_impact", o.x, o.y, 0.7f)
                        }
                    }
                }
                ObstacleKind.MOVING_PLATFORM -> {
                    val before = o.standTop
                    o.y = o.baseY + sin(o.time * 1.8f) * 40f
                    updateObstacleBox(o)
                    if (player.grounded && abs(player.y - before) < 2f && player.x in o.box.x..(o.box.x + o.box.width)) player.y = o.standTop
                }
                ObstacleKind.COLLAPSING -> if (o.collapseTimer >= 0f) {
                    o.collapseTimer -= dt
                    o.warning = true
                    if (o.collapseTimer <= 0f) { o.dead = true; o.vx = 0f; o.vy = 0f; addEffect("effects/smoke", o.x, o.y, 0.5f); listener.sfx(Sfx.LAND, 0.8f, 0.7f) }
                }
                ObstacleKind.BOUNCE -> if (o.triggered && o.time > 0f) { if (player.x - o.x > 200f) o.triggered = false }
                else -> {}
            }
            updateObstacleBox(o)
        }
    }

    private fun updateEnemyBox(e: Enemy) {
        val m = enemyMeta(e.def.id)
        collisionRect(m, e.x, e.y, Tuning.ENEMY_SCALE, Tuning.COLLISION_FORGIVENESS, true, e.box)
    }

    private fun updateEnemies(dt: Float) {
        val px = player.x
        enemies.each { e ->
            e.time += dt
            e.stateTime += dt
            if (!e.alive) {
                if (e.state == EnemyState.DAMAGE && e.stateTime > 0.22f) e.set(EnemyState.DEATH)
                if (e.state == EnemyState.DEATH) { e.x += 60f * dt; e.vy -= 1400f * dt; e.y += e.vy * dt }
                if (e.state == EnemyState.DEATH && e.stateTime > 1.0f) e.removed = true
                return@each
            }
            val dist = e.x - px
            when (e.def.behavior) {
                EnemyBehavior.HOPPER -> {
                    if (!e.fired && dist <= e.activationDist) e.fired = true
                    if (e.fired) e.x -= e.chargeSpeed * dt
                    val hop = (e.time % 1.1f) / 1.1f
                    e.y = e.baseY + (if (hop < 0.35f) sin(hop / 0.35f * MathUtils.PI) * 34f else 0f)
                    e.set(if (hop < 0.35f) EnemyState.MOVE else EnemyState.IDLE)
                }
                EnemyBehavior.SWOOPER -> {
                    e.y = e.baseY + sin(e.time * 4f) * 10f + 10f
                    if (!e.fired && dist <= e.activationDist) e.fired = true
                    if (e.fired) e.x -= e.chargeSpeed * dt
                }
                EnemyBehavior.LUNGER -> {
                    if (dist < 320f && e.state != EnemyState.ATTACK) { e.set(EnemyState.ATTACK); listener.sfx(Sfx.ENEMY_ATTACK, 0.5f) }
                    if (e.state == EnemyState.ATTACK && e.stateTime < 0.35f) e.x -= LUNGE_DISTANCE / 0.35f * dt
                }
                EnemyBehavior.SHOOTER, EnemyBehavior.THROWER, EnemyBehavior.ZAPPER -> {
                    if (e.def.behavior == EnemyBehavior.ZAPPER) e.y = e.baseY + sin(e.time * 3f) * 6f + 6f
                    if (!e.fired && e.fireDistance > 0f) {
                        if (dist <= e.fireDistance + 0.45f * speed && e.state != EnemyState.ATTACK) {
                            e.set(EnemyState.ATTACK); warn(Tuning.GROUND_Y + HIGH_LANE_BOTTOM + 30f)
                        }
                        if (dist <= e.fireDistance) {
                            e.fired = true
                            val fx = e.def.projectileFx ?: "robot_missile"
                            val pr = obtainProjectile(fx, e.x - 40f, Tuning.GROUND_Y + HIGH_LANE_BOTTOM + 24f, -e.chargeSpeed, 0f, false)
                            pr.lane = Action.SLIDE; pr.halfW = 26f; pr.halfH = 18f; pr.scale = 0.45f
                            if (fx == "electric_sparks") pr.tint = 0xFFF27AFF.toInt()
                            listener.sfx(Sfx.ENEMY_ATTACK, 0.7f)
                        }
                    } else if (e.state == EnemyState.ATTACK && e.stateTime > 0.5f) e.set(EnemyState.IDLE)
                }
                EnemyBehavior.DANGLER -> { e.y = e.baseY + (sin(e.time * 2.2f) * 0.5f + 0.5f) * 70f; e.set(EnemyState.MOVE) }
                EnemyBehavior.BURNER -> {
                    val flare = (e.time % 1.6f) > 1.0f
                    e.set(if (flare) EnemyState.ATTACK else EnemyState.IDLE)
                    if (flare && setup.quality > 0 && MathUtils.randomBoolean(dt * 6f)) addEffect("effects/fire_particles", e.x, e.y + 10f, 0.45f)
                }
                EnemyBehavior.CHARGER -> {
                    if (!e.fired && dist <= e.activationDist) { e.fired = true; e.set(EnemyState.MOVE); warn(Tuning.GROUND_Y + 30f); listener.sfx(Sfx.ENEMY_ATTACK, 0.5f, 0.8f) }
                    if (e.fired) e.x -= e.chargeSpeed * dt
                }
                EnemyBehavior.PHASER -> {
                    val c = (e.time % 2.4f)
                    e.alpha = when { c < 1.2f -> 1f; c < 1.5f -> 1f - (c - 1.2f) / 0.3f; c < 2.1f -> 0.15f; else -> 0.15f + (c - 2.1f) / 0.3f * 0.85f }
                    // fully solid while visible; harmless while faded (players can rely on either)
                    e.solid = e.alpha > 0.6f
                    e.y = e.baseY + sin(e.time * 2.5f) * 8f + 8f
                    e.set(EnemyState.MOVE)
                }
            }
            if (e.state == EnemyState.ATTACK && e.def.behavior != EnemyBehavior.BURNER && e.stateTime > 0.6f && e.def.behavior != EnemyBehavior.CHARGER) e.set(if (e.def.flying) EnemyState.MOVE else EnemyState.IDLE)
            updateEnemyBox(e)
        }
    }

    // ----------------------------------------------------------------- bosses
    private fun beginBossWarning() {
        generationPaused = true
        setState(RunState.BOSS_WARNING)
        listener.sfx(Sfx.BOSS_WARNING)
        listener.vibrate(120)
    }

    private fun areaClear(): Boolean {
        val right = cameraX + viewWidth
        obstacles.each { o -> if (!o.dead && o.x > player.x - 100f && o.x < right + 400f && o.def.kind != ObstacleKind.BOUNCE) return false }
        enemies.each { e -> if (e.alive && e.x > player.x - 100f) return false }
        return true
    }

    private fun spawnBoss() {
        val id = def.bosses[bossEncounters % def.bosses.size]
        val hp = Tuning.BOSS_BASE_HP + bossEncounters * Tuning.BOSS_HP_PER_ENCOUNTER + def.bossHpBonus
        val b = Boss(Bosses.byId(id), hp, bossEncounters)
        b.screenX = viewWidth + 260f
        b.y = Tuning.GROUND_Y
        b.x = cameraX + b.screenX
        boss = b
        // clear leftovers so nothing overlaps the arena
        obstacles.each { o -> if (o.x > player.x) o.dead = true }
        pickups.each { pk -> if (pk.x > player.x + viewWidth) pk.collected = true }
        setState(RunState.BOSS)
    }

    private fun damageBoss(b: Boss, x: Float, y: Float) {
        b.hp--
        b.flash = 0.15f
        addEffect("effects/electric_sparks", x, y - 50f, 0.55f, tint = 0x7FE8FFFF)
        listener.sfx(Sfx.ENEMY_DEFEAT, 0.5f, 1.4f)
        if (b.state == BossState.IDLE) b.set(BossState.DAMAGE)
        if (b.phase == 1 && b.hp <= b.maxHp / 2) {
            b.phase = 2
            b.phaseBreak = 1.1f
            b.set(BossState.SPECIAL)
            listener.sfx(Sfx.BOSS_WARNING, 0.6f)
            listener.shake(8f)
            floatText("PHASE 2", b.x - 80f, b.y + 300f, 0xFF6B6BFF.toInt())
        }
        if (b.hp <= 0) defeatBoss(b)
    }

    private fun defeatBoss(b: Boss) {
        b.defeated = true
        b.set(BossState.DEATH)
        b.queue.clear()
        projectiles.each { p -> if (!p.friendly) { p.alive = false; addEffect("effects/smoke", p.x, p.y - 30f, 0.4f) } }
        if (b.def.id !in bossesDefeated) bossesDefeated += b.def.id
        if (b.encounter == 0) gateBossDefeated = true
        val bonus = b.encounter * 100
        val reward = Reward(coins = Tuning.BOSS_REWARD_COINS + bonus, gems = Tuning.BOSS_REWARD_GEMS)
        coins += reward.coins; gems += reward.gems
        lastBossReward = reward
        val pts = (Tuning.SCORE_BOSS * scoreMultiplier).toInt()
        score += pts
        bossEncounters++
        nextBossMetres = metres + Tuning.BOSS_INTERVAL_METRES
        addEffect("effects/boss_defeat", b.x, b.y + 60f, 1.4f)
        addEffect("effects/explosion", b.x - 60f, b.y + 120f, 1.1f)
        addEffect("effects/victory_confetti", cameraX + viewWidth / 2f, Tuning.GROUND_Y + 250f, 1.6f)
        addEffect("effects/victory_confetti", cameraX + viewWidth * 0.3f, Tuning.GROUND_Y + 300f, 1.3f)
        listener.sfx(Sfx.BOSS_DEFEAT)
        listener.sfx(Sfx.VICTORY, 0.8f)
        listener.shake(14f)
        listener.vibrate(250)
        floatText("+$pts", b.x - 100f, b.y + 280f, 0xFFD45AFF.toInt())
        setState(RunState.BOSS_DEFEATED)
    }

    private fun updateBoss(dt: Float) {
        val b = boss ?: return
        b.stateTime += dt
        if (b.flash > 0f) b.flash -= dt
        val restX = viewWidth - 250f
        if (b.state == BossState.ENTER) {
            b.screenX += (restX - b.screenX) * min(1f, dt * 1.6f)
            if (abs(b.screenX - restX) < 6f) { b.screenX = restX; b.set(BossState.IDLE); b.clock = 0f; b.nextVolleyAt = 1.2f }
        }
        b.x = cameraX + b.screenX
        if (b.defeated) {
            b.y -= 40f * dt
            updateBossBox(b)
            return
        }
        b.y = Tuning.GROUND_Y + if (b.def.id == "shadow_dragon") 40f + sin(b.clock * 2f) * 14f else 0f
        if (b.state != BossState.ENTER) {
            if (b.phaseBreak > 0f) { b.phaseBreak -= dt } else {
                b.clock += dt
                if (b.clock >= b.nextVolleyAt && b.queue.isEmpty()) {
                    b.planVolley({ a, c -> generator.requiredGap(a, c) + 0.12f }) { rng.nextFloat() }
                    warn(Tuning.GROUND_Y + if (b.queue.first().lane == Action.SLIDE) HIGH_LANE_BOTTOM + 30f else 30f, big = true)
                    listener.sfx(Sfx.BOSS_ATTACK, 0.5f, 1.2f)
                }
                val it = b.queue.iterator()
                while (it.hasNext()) {
                    val s = it.next()
                    if (b.clock >= s.time) {
                        it.remove()
                        fireBossShot(b, s)
                    }
                }
            }
            if ((b.state == BossState.PRIMARY || b.state == BossState.SPECIAL || b.state == BossState.DAMAGE) && b.stateTime > 0.55f && b.queue.isEmpty()) b.set(BossState.IDLE)
        }
        updateBossBox(b)
    }

    private fun fireBossShot(b: Boss, s: BossShot) {
        val vrel = (if (b.phase == 1) 520f else 640f) + b.encounter * 30f
        val y = if (s.lane == Action.SLIDE) Tuning.GROUND_Y + HIGH_LANE_BOTTOM + 26f else Tuning.GROUND_Y + 26f
        val pr = obtainProjectile(s.fx, b.x - 120f, y, speed - vrel, 0f, false)
        pr.lane = s.lane
        pr.halfW = 30f; pr.halfH = if (s.lane == Action.SLIDE) 20f else 22f
        pr.scale = 0.6f
        pr.tint = if (b.phase == 2) 0xFFB0B0FF.toInt() else -1
        // projectiles move relative to the camera: store the relative speed in gravity slot = 0 and adjust in update
        pr.gravity = -vrel
        listener.sfx(Sfx.BOSS_ATTACK, 0.8f)
        if (s.lane == Action.JUMP) addEffect("effects/smoke", b.x - 120f, Tuning.GROUND_Y, 0.5f)
    }

    private fun updateBossBox(b: Boss) {
        val m = bossMeta(b.def.id)
        collisionRect(m, b.x, b.y, Tuning.BOSS_SCALE, 0.9f, true, b.box)
    }

    // ----------------------------------------------------------------- projectiles / pickups / fx
    private fun obtainProjectile(fx: String, x: Float, y: Float, vx: Float, vy: Float, friendly: Boolean): Projectile {
        val p = projectilePool.obtain()
        p.fx = fx; p.x = x; p.y = y; p.vx = vx; p.vy = vy; p.friendly = friendly
        p.updateBox()
        projectiles.add(p)
        return p
    }

    private fun updateProjectiles(dt: Float) {
        projectiles.each { p ->
            if (!p.alive) return@each
            p.time += dt
            if (p.gravity < 0f) p.vx = speed + p.gravity   // boss shots: constant speed relative to the camera
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.updateBox()
            if (p.x < cameraX - 300f || p.x > cameraX + viewWidth + 600f || p.time > 8f) p.alive = false
        }
    }

    private fun updatePickups(dt: Float) {
        val magnet = powerUps.active(PowerUpType.COIN_MAGNET)
        val cx = player.x
        val cy = player.y + standHeight() * 0.5f
        pickups.each { pk ->
            if (pk.collected) return@each
            pk.time += dt
            val coin = pk.collectible == CollectibleType.SILVER_COIN || pk.collectible == CollectibleType.GOLD_COIN || pk.collectible == CollectibleType.DIAMOND_COIN
            if (magnet && coin && !pk.magnet) {
                val dx = pk.x - cx; val dy = pk.y - cy
                if (dx * dx + dy * dy < setup.magnetRange * setup.magnetRange) pk.magnet = true
            }
            if (pk.magnet) {
                val dx = cx - pk.x; val dy = cy - pk.y
                val d = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val v = 900f + speed
                pk.x += dx / d * v * dt + speed * dt * 0.5f
                pk.y += dy / d * v * dt
            } else if (pk.powerUp != null) {
                pk.y = pk.baseY + sin(pk.time * 3f) * 8f
            }
            val r = if (pk.powerUp != null) 30f else 22f
            pk.box.set(pk.x - r, pk.y - r, r * 2, r * 2)
        }
    }

    private val maxEffects get() = when (setup.quality) { 0 -> 10; 1 -> 22; else -> 40 }

    fun addEffect(anim: String, x: Float, y: Float, scale: Float, follow: Boolean = false, tint: Int = -1, loopFor: Float = -1f) {
        if (effects.size >= maxEffects) {
            // drop the oldest effect rather than allocating without limit
            effectPool.free(effects.removeIndex(0))
        }
        val e = effectPool.obtain()
        e.anim = anim; e.x = x; e.y = y; e.scale = scale; e.followPlayer = follow; e.tint = tint
        if (follow) { e.x = x - player.x; e.y = y - player.y }
        if (loopFor > 0f) { e.loop = true; e.life = loopFor }
        effects.add(e)
    }

    private fun updateEffects(dt: Float) {
        var i = 0
        while (i < effects.size) {
            val e = effects[i]
            e.time += dt
            e.x += e.vx * dt; e.y += e.vy * dt
            val dur = sprites.anim(e.anim).let { it.frameDuration * it.frames.size }
            val done = if (e.loop) e.time >= e.life else e.time >= dur
            if (done) { effects.removeIndex(i); effectPool.free(e) } else i++
        }
    }

    fun floatText(text: String, x: Float, y: Float, color: Int) {
        if (texts.size >= 12) textPool.free(texts.removeIndex(0))
        val t = textPool.obtain()
        t.text = text; t.x = x; t.y = y; t.color = color
        texts.add(t)
    }

    private fun updateTexts(dt: Float) {
        var i = 0
        while (i < texts.size) {
            val t = texts[i]
            t.time += dt
            t.y += 60f * dt
            if (t.time > 1.1f) { texts.removeIndex(i); textPool.free(t) } else i++
        }
        val wi = warnings.iterator()
        while (wi.hasNext()) { val w = wi.next(); w.time += dt; if (w.time > 1.0f) wi.remove() }
    }

    private fun warn(y: Float, big: Boolean = false) {
        if (warnings.size < 6) warnings += Warning(y, 0f, big)
    }

    private fun updateHints(dt: Float) {
        if (hintTime > 0f) { hintTime -= dt; if (hintTime <= 0f) activeHint = null }
        if (hints.isNotEmpty() && player.x >= hints[0].first - 500f) {
            val (_, text) = hints.removeAt(0)
            if (text.isEmpty()) { activeHint = null; listener.tutorialFinished() } else { activeHint = text; hintTime = 3.2f }
        }
    }

    private fun <T> cull(arr: GdxArray<T>, pool: Pool<T>, gone: (T) -> Boolean) {
        var i = 0
        while (i < arr.size) {
            val e = arr[i]
            if (gone(e)) { arr.removeIndex(i); pool.free(e) } else i++
        }
    }

    private fun despawn() {
        val left = cameraX - Tuning.DESPAWN_BEHIND
        cull(obstacles, obstaclePool) { it.x < left || (it.dead && it.deadTime > 2f) }
        cull(platforms, platformPool) { it.x + it.width < left }
        cull(enemies, enemyPool) { it.x < left || it.removed }
        cull(projectiles, projectilePool) { !it.alive }
        cull(pickups, pickupPool) { it.collected || it.x < left }
    }
}
