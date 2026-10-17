package com.bramble.dash.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.bramble.dash.assets.GameAssets
import com.bramble.dash.config.ObstacleKind
import com.bramble.dash.config.PowerUpType
import com.bramble.dash.config.Tuning
import com.bramble.dash.game.Action
import com.bramble.dash.game.BossState
import com.bramble.dash.game.EnemyState
import com.bramble.dash.game.GameWorld
import com.bramble.dash.game.each
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/** Parallax background shared by menus and gameplay. */
class Backdrop(private val assets: GameAssets, val worldId: String) {
    private val layers = Array(4) { assets.region("environments/$worldId/layer_$it") }
    val ground: AtlasRegion = assets.region("environments/$worldId/ground")
    val platform: AtlasRegion = assets.region("environments/$worldId/platform")
    val decoration: AtlasRegion = assets.region("environments/$worldId/decoration")
    val lighting: AtlasRegion = assets.region("environments/$worldId/lighting")
    private val ratios = floatArrayOf(0.06f, 0.22f, 0.45f, 0.75f)

    /** [scroll] is the camera's world x; the view spans [left, left+width] x [0, height]. */
    fun draw(batch: SpriteBatch, scroll: Float, left: Float, width: Float, height: Float, quality: Int) {
        // layers are 1280x720; scale to cover the view height and repeat by drawing adjacent copies
        val s = max(1f, height / 720f)
        val lw = 1280f * s
        for (i in 0 until 4) {
            if (quality == 0 && i == 1) continue
            val off = (scroll * ratios[i]) % lw
            var x = left - off
            while (x < left + width) {
                batch.drawOrig(layers[i], x, 0f, s)
                x += lw - 0.5f   // tiny overlap hides sub-pixel seams
            }
        }
    }

    fun drawDecorations(batch: SpriteBatch, cameraX: Float, width: Float) {
        // deterministic trees along the ground, spaced by a hash of the tile index
        val spacing = 520f
        var i = floor((cameraX - 300f) / spacing).toInt()
        while (i * spacing < cameraX + width + 300f) {
            val h = (i * 1103515245 + 12345) ushr 16
            if (h % 3 != 0) {
                val x = i * spacing + (h % 200)
                val sc = 0.8f + (h % 5) * 0.08f
                batch.drawOrig(decoration, x, Tuning.GROUND_Y - 22f, sc)
            }
            i++
        }
    }

    fun drawGround(batch: SpriteBatch, cameraX: Float, width: Float) {
        val tw = 256f
        var x = floor(cameraX / tw) * tw
        while (x < cameraX + width + tw) {
            // tile surface is 108 px above its bottom edge
            batch.drawOrig(ground, x, Tuning.GROUND_Y - 108f, 1f)
            x += tw - 0.5f
        }
    }

    fun drawLighting(batch: SpriteBatch, left: Float, width: Float, height: Float) {
        batch.drawOrig(lighting, left, 0f, width / 256f, height / 128f)
    }
}

/**
 * Draws a [GameWorld]: parallax, ground, platforms, obstacles, pickups, enemies, boss, player,
 * projectiles and pooled effects. Positions are interpolated between fixed simulation steps.
 */
class WorldRenderer(private val assets: GameAssets, private val world: GameWorld, private val quality: () -> Int) {
    val camera = OrthographicCamera()
    val viewport = ExtendViewport(1280f, 720f, camera)
    val backdrop = Backdrop(assets, world.def.id)
    private val font: BitmapFont = assets.font("body")
    private val small: BitmapFont = assets.font("small")
    private val layout = GlyphLayout()
    private val tint = Color()
    private var shake = 0f
    private var shakeTime = 0f
    var time = 0f
        private set
    private val charId = world.setup.character.id
    private val playerAnims = com.bramble.dash.game.PlayerAnim.values().map { assets.anim("characters/$charId/${it.anim}") }.toTypedArray()
    private val obstacleAnims = HashMap<String, com.bramble.dash.assets.Anim>()
    private val enemyAnims = HashMap<String, Array<com.bramble.dash.assets.Anim>>()
    private val bossAnims = HashMap<String, Array<com.bramble.dash.assets.Anim>>()
    private val fxAnims = HashMap<String, com.bramble.dash.assets.Anim>()
    private val pickupRegions = HashMap<PowerUpType, AtlasRegion>()
    private val activeAnims = HashMap<PowerUpType, com.bramble.dash.assets.Anim>()
    private fun obstacleAnim(id: String) = obstacleAnims.getOrPut(id) { assets.anim("obstacles/$id") }
    private fun enemyAnim(id: String, s: EnemyState) = enemyAnims.getOrPut(id) { EnemyState.values().map { assets.anim("enemies/$id/${it.anim}") }.toTypedArray() }[s.ordinal]
    private fun bossAnim(id: String, s: BossState) = bossAnims.getOrPut(id) { BossState.values().map { assets.anim("bosses/$id/${it.anim}") }.toTypedArray() }[s.ordinal]
    private fun fxAnim(fx: String) = fxAnims.getOrPut(fx) { assets.anim("effects/$fx") }

    fun shake(amount: Float) {
        if (quality() == 0) return
        shake = max(shake, amount)
        shakeTime = 0.35f
    }

    fun resize(w: Int, h: Int) {
        viewport.update(w, h, false)
        world.viewWidth = viewport.worldWidth
    }

    fun render(batch: SpriteBatch, delta: Float) {
        time += delta
        val a = world.alpha
        val camX = world.prevCameraX + (world.cameraX - world.prevCameraX) * a
        var sx = 0f
        var sy = 0f
        if (shakeTime > 0f) {
            shakeTime -= delta
            val k = shake * (shakeTime / 0.35f).coerceAtLeast(0f)
            sx = MathUtils.random(-k, k); sy = MathUtils.random(-k, k)
            if (shakeTime <= 0f) shake = 0f
        }
        val vw = viewport.worldWidth
        val vh = viewport.worldHeight
        camera.position.set(camX + vw / 2f + sx, vh / 2f + sy, 0f)
        camera.update()
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.color = Color.WHITE
        val q = quality()
        backdrop.draw(batch, camX, camX, vw, vh, q)
        if (q > 0) backdrop.drawDecorations(batch, camX, vw)
        drawPlatforms(batch)
        backdrop.drawGround(batch, camX, vw)
        drawObstacles(batch, camX, vw)
        drawPickups(batch, camX, vw)
        drawEnemies(batch, camX, vw)
        drawBoss(batch)
        drawPlayer(batch, a)
        drawProjectiles(batch)
        drawEffects(batch, a)
        batch.color = Color.WHITE
        if (q > 0) { batch.setColor(1f, 1f, 1f, 0.55f); backdrop.drawLighting(batch, camX, vw, vh); batch.color = Color.WHITE }
        drawTexts(batch)
        drawWarnings(batch, camX, vw)
        batch.end()
    }

    private fun visible(x: Float, camX: Float, vw: Float, margin: Float = 260f) = x > camX - margin && x < camX + vw + margin

    private fun drawPlatforms(batch: SpriteBatch) {
        world.platforms.each { p ->
            for (i in 0 until p.tiles) {
                batch.drawOrig(backdrop.platform, p.x + i * GameWorld.PLATFORM_TILE_W, p.top - GameWorld.PLATFORM_SURFACE, GameWorld.PLATFORM_SCALE)
            }
        }
    }

    private fun drawObstacles(batch: SpriteBatch, camX: Float, vw: Float) {
        val white = assets.white
        world.obstacles.each { o ->
            if (!visible(o.x, camX, vw)) return@each
            val anim = obstacleAnim(o.def.id)
            var t = o.time
            if (o.def.kind == ObstacleKind.TIMED && !o.active) t = 0f
            val r = anim.frame(t)
            val m = anim.meta(t)
            tint.set(Color.WHITE)
            if (o.def.kind == ObstacleKind.TIMED) {
                when {
                    o.active -> tint.set(1f, 1f, 1f, 1f)
                    o.warning -> tint.set(1f, 0.75f + 0.25f * sin(time * 30f), 0.6f, 1f)
                    else -> tint.set(0.55f, 0.55f, 0.6f, 0.8f)
                }
            }
            if (o.warning && o.def.kind == ObstacleKind.COLLAPSING) tint.set(1f, 0.7f + 0.3f * sin(time * 40f), 0.7f, 1f)
            if (o.dead) tint.a = (1f - o.deadTime * 1.5f).coerceAtLeast(0f)
            if (o.def.kind == ObstacleKind.FALLING && !o.landed && o.triggered) {
                // shadow warning on the landing spot
                val sh = 1f - ((o.y - o.baseY) / 720f).coerceIn(0f, 1f)
                batch.setColor(0f, 0f, 0f, 0.25f + 0.35f * sh)
                batch.draw(white, o.box.x - 6f, Tuning.GROUND_Y - 6f, o.box.width + 12f, 10f)
            }
            if (o.def.kind == ObstacleKind.HANGING && !o.dead) {
                // a tether to the top of the screen so overhead hazards read as hanging
                batch.setColor(0.12f, 0.14f, 0.2f, 0.9f)
                batch.draw(white, o.box.x + o.box.width / 2f - 2f, o.box.y + o.box.height - 6f, 4f, 900f)
            }
            batch.color = tint
            batch.drawPivot(r, m, o.x, o.y, Tuning.OBSTACLE_SCALE, flipY = o.def.flipY)
        }
        batch.color = Color.WHITE
    }

    private fun drawPickups(batch: SpriteBatch, camX: Float, vw: Float) {
        world.pickups.each { p ->
            if (!visible(p.x, camX, vw, 120f)) return@each
            val pu = p.powerUp
            if (pu != null) {
                val r = pickupRegions.getOrPut(pu) { assets.region("powerups/${pu.id}/pickup") }
                val glow = 0.85f + 0.15f * sin(time * 6f + p.x)
                batch.setColor(glow, glow, 1f, 1f)
                batch.drawCentered(r, p.x, p.y, Tuning.POWERUP_SCALE)
                batch.color = Color.WHITE
            } else {
                val c = p.collectible ?: return@each
                val anim = assets.anim(c.anim)
                val r = anim.frame(p.time + time * 0.2f)
                batch.drawCentered(r, p.x, p.y, Tuning.ITEM_SCALE)
            }
        }
    }

    private fun drawEnemies(batch: SpriteBatch, camX: Float, vw: Float) {
        val white = assets.white
        world.enemies.each { e ->
            if (!visible(e.x, camX, vw)) return@each
            val st = e.state
            val anim = enemyAnim(e.def.id, st)
            val r = anim.frame(e.stateTime)
            val m = anim.meta(e.stateTime)
            if (e.def.behavior == com.bramble.dash.config.EnemyBehavior.DANGLER) {
                batch.setColor(0.85f, 0.85f, 0.9f, 0.8f)
                batch.draw(white, e.x - 1f, e.box.y + e.box.height - 10f, 2f, 900f)
            }
            if (st == EnemyState.ATTACK && e.def.behavior != com.bramble.dash.config.EnemyBehavior.BURNER) batch.setColor(1f, 0.7f, 0.7f, e.alpha)
            else if (st == EnemyState.DAMAGE) batch.setColor(1f, 0.55f, 0.55f, 1f)
            else batch.setColor(1f, 1f, 1f, if (e.alive) e.alpha else (1f - e.stateTime).coerceAtLeast(0f))
            batch.drawPivot(r, m, e.x, e.y, Tuning.ENEMY_SCALE, flipX = true)
        }
        batch.color = Color.WHITE
    }

    private fun drawBoss(batch: SpriteBatch) {
        val b = world.boss ?: return
        val anim = bossAnim(b.def.id, b.state)
        val r = anim.frame(b.stateTime)
        val m = anim.meta(b.stateTime)
        if (b.flash > 0f) batch.setColor(1f, 0.5f, 0.5f, 1f)
        else if (b.phase == 2 && !b.defeated) batch.setColor(1f, 0.85f + 0.15f * sin(time * 8f), 0.85f, 1f)
        else batch.color = Color.WHITE
        if (b.defeated) batch.setColor(1f, 1f, 1f, (1f - (b.stateTime - 0.8f).coerceAtLeast(0f)).coerceAtLeast(0f))
        batch.drawPivot(r, m, b.x, b.y, Tuning.BOSS_SCALE, flipX = true)
        batch.color = Color.WHITE
        if (b.state == BossState.PRIMARY || b.state == BossState.SPECIAL) {
            if (b.stateTime < 0.6f && quality() > 0) {
                val fx = assets.anim("effects/electric_sparks")
                batch.setColor(1f, 0.4f, 0.4f, 0.8f)
                batch.drawCentered(fx.frame(b.stateTime), b.x - 120f, b.y + 120f, 0.7f)
                batch.color = Color.WHITE
            }
        }
    }

    private fun drawPlayer(batch: SpriteBatch, a: Float) {
        val p = world.player
        val x = p.prevX + (p.x - p.prevX) * a
        val y = p.prevY + (p.y - p.prevY) * a
        val anim = playerAnims[p.anim.ordinal]
        val r = anim.frame(p.animTime)
        val m = anim.meta(p.animTime)
        val pu = world.powerUps
        when {
            p.invulnerable > 0f && !p.dead && ((time * 12f).toInt() % 2 == 0) -> batch.setColor(1f, 1f, 1f, 0.35f)
            p.hurtTimer > 0f -> batch.setColor(1f, 0.45f, 0.45f, 1f)
            pu.active(PowerUpType.INVINCIBILITY) -> batch.setColor(1f, 0.85f + 0.15f * sin(time * 20f), 0.5f + 0.5f * sin(time * 13f), 1f)
            else -> batch.color = Color.WHITE
        }
        // landing squash adds weight to the supplied cutout animation
        val squash = if (p.anim == com.bramble.dash.game.PlayerAnim.LAND) 1f - 0.08f * (1f - p.animTime / Tuning.LAND_ANIM_TIME).coerceIn(0f, 1f) else 1f
        batch.drawPivot(r, m, x, y, Tuning.PLAYER_SCALE * squash)
        batch.color = Color.WHITE
        // persistent power-up visuals
        if (pu.shield) {
            // shield bubble from the shield-activation VFX, looped on its brightest frames
            val fx = assets.anim("effects/shield_activation")
            batch.setColor(0.7f, 1f, 1f, 0.55f + 0.1f * sin(time * 6f))
            batch.drawCentered(fx.frames[3 + ((time * 6f).toInt() % 3)], x, y + world.standHeight() * 0.5f, 0.9f)
        }
        if (pu.active(PowerUpType.JETPACK)) {
            val fx = assets.anim("powerups/jetpack/active")
            batch.color = Color.WHITE
            batch.drawCentered(fx.frame(time), x - 30f, y + world.standHeight() * 0.45f, 0.5f)
        }
        // other timed effects: small icons orbiting above the head (blink before expiry)
        var n = 0
        for (t in ACTIVE_FX) if (t != PowerUpType.JETPACK && pu.active(t)) {
            val fx = activeAnims.getOrPut(t) { assets.anim("powerups/${t.id}/active") }
            val remaining = pu.remaining[t.ordinal]
            if (remaining > 1.5f || ((time * 8f).toInt() % 2 == 0)) {
                batch.setColor(1f, 1f, 1f, 0.9f)
                batch.drawCentered(fx.frame(time), x - 24f + n * 34f, y + world.standHeight() + 28f + sin(time * 4f + n) * 4f, 0.3f)
            }
            n++
        }
        batch.color = Color.WHITE
    }

    private fun drawProjectiles(batch: SpriteBatch) {
        world.projectiles.each { p ->
            if (!p.alive) return@each
            val anim = fxAnim(p.fx)
            val r = anim.frame(p.time % anim.duration)
            Color.rgba8888ToColor(tint, p.tint)
            batch.color = tint
            // effect frames are centred on their canvas; draw so the canvas centre is the projectile centre
            val flip = !p.friendly
            val w = r.originalWidth * p.scale
            val h = r.originalHeight * p.scale
            val left = p.x - w / 2f
            val bottom = p.y - h / 2f
            val ox = if (flip) (r.originalWidth - r.offsetX - r.packedWidth) * p.scale else r.offsetX * p.scale
            if (flip) batch.draw(r, left + ox + r.packedWidth * p.scale, bottom + r.offsetY * p.scale, -r.packedWidth * p.scale, r.packedHeight * p.scale)
            else batch.draw(r, left + ox, bottom + r.offsetY * p.scale, r.packedWidth * p.scale, r.packedHeight * p.scale)
        }
        batch.color = Color.WHITE
    }

    private fun drawEffects(batch: SpriteBatch, a: Float) {
        val p = world.player
        val px = p.prevX + (p.x - p.prevX) * a
        val py = p.prevY + (p.y - p.prevY) * a
        world.effects.each { e ->
            val anim = assets.anim(e.anim)
            val r = anim.frame(if (e.loop) e.time % anim.duration else e.time)
            Color.rgba8888ToColor(tint, e.tint)
            batch.color = tint
            val x = if (e.followPlayer) px + e.x else e.x
            val y = if (e.followPlayer) py + e.y else e.y
            batch.drawCentered(r, x, y + r.originalHeight * e.scale * 0.25f, e.scale)
        }
        batch.color = Color.WHITE
    }

    private fun drawTexts(batch: SpriteBatch) {
        world.texts.each { t ->
            Color.rgba8888ToColor(tint, t.color)
            tint.a = (1f - t.time / 1.1f).coerceIn(0f, 1f)
            small.color = tint
            layout.setText(small, t.text)
            small.draw(batch, layout, t.x - layout.width / 2f, t.y)
        }
        small.color = Color.WHITE
    }

    private fun drawWarnings(batch: SpriteBatch, camX: Float, vw: Float) {
        for (w in world.warnings) {
            val pulse = 0.6f + 0.4f * sin(w.time * 25f)
            font.setColor(1f, 0.35f, 0.3f, pulse)
            layout.setText(font, "!")
            val s = if (w.big) 1.6f else 1.2f
            font.data.setScale(s)
            font.draw(batch, "!", camX + vw - 60f, w.y + 22f)
            font.data.setScale(1f)
        }
        if (world.state == com.bramble.dash.game.RunState.BOSS_WARNING) {
            val pulse = 0.5f + 0.5f * sin(world.stateTime * 10f)
            font.setColor(1f, 0.3f + 0.3f * pulse, 0.25f, 1f)
            layout.setText(font, "WARNING: BOSS APPROACHING")
            font.draw(batch, layout, camX + (vw - layout.width) / 2f, 520f)
        }
        font.color = Color.WHITE
    }

    companion object {
        private val ACTIVE_FX = arrayOf(PowerUpType.SPEED_BOOST, PowerUpType.COIN_MAGNET, PowerUpType.INVINCIBILITY, PowerUpType.JETPACK, PowerUpType.DOUBLE_COINS, PowerUpType.SCORE_MULTIPLIER, PowerUpType.DOUBLE_JUMP)
    }
}
