package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Candy-Crush style level map: one tall picture with 50 level nodes along its winding path. */
class MapScreen(game: SnakeAttackGame) : BaseScreen(game) {
    private val tiles = listOf("map/map_0.jpg", "map/map_1.jpg", "map/map_2.jpg")
    private val page = (Save.level - 1) / LEVELS
    private val firstLevel = page * LEVELS + 1
    private val nodes = Array(LEVELS) { Vector2() }   // map space: x world, y from map bottom
    private val labels = Array(LEVELS) { (firstLevel + it).toString() }
    private var mapScale = 1f
    private var mapH = 0f
    private var mapX = 0f
    private var scroll = 0f
    private var velocity = 0f
    private var dragging = false
    private var noLives = 0f
    private var noLivesOpen = false
    private val particles = Particles()
    private var autoScrollTo = -1f

    private val playBtn = Button(Assets.r("btn_green"), 330f, "", 44f) { tryPlay(Save.level) }
    private val shopBtn = Button(Assets.r("ui_shop_icon"), 100f) { goTo { game.setScreen(ShopScreen(game)) } }
    private val dailyBtn = Button(Assets.r("ui_gift_box_red"), 92f) { goTo { game.setScreen(DailyScreen(game)) } }
    private val spinBtn = Button(Assets.r("ui_spin_wheel_8_slices"), 92f) { goTo { game.setScreen(SpinScreen(game)) } }
    private val trophyBtn = Button(Assets.r("ui_trophy_gold"), 92f) { goTo { game.setScreen(AchievementsScreen(game)) } }
    private val homeBtn = Button(Assets.r("ui_home_button"), 72f) { goTo { game.setScreen(MenuScreen(game)) } }
    private val refillBtn = Button(Assets.r("btn_green"), 340f, "REFILL  150", 38f) { refill() }
    private val closeBtn = Button(Assets.r("ui_close_x_button"), 74f) { noLivesOpen = false }
    private val levelText = "PLAY  LEVEL ${Save.level}"

    init {
        buttons += listOf(playBtn, shopBtn, dailyBtn, spinBtn, trophyBtn, homeBtn, refillBtn, closeBtn)
        playBtn.label = levelText
        playBtn.labelSize = 40f
        playBtn.pulse = true
        refillBtn.icon = Assets.r("coin")
        refillBtn.iconSize = 44f
        refillBtn.visible = false
        closeBtn.visible = false
    }

    override fun show() {
        super.show()
        Assets.playMusic("map")
        Assets.keepOnly(tiles)
        if (!game.dailyShown && Save.dailyIndex >= 0 && System.getProperty("snake.nodaily") == null) {
            game.dailyShown = true
            goTo { game.setScreen(DailyScreen(game)) }
        }
    }

    override fun layout() {
        // zoomed in (sides cropped) so 50 nodes have room along the path
        mapScale = W / 1024f * ZOOM
        mapH = 3072f * mapScale
        mapX = W / 2 - 512f * mapScale
        // build a smooth path through the traced points (bottom of the picture first)
        val pts = Paths.map.map { Vector2(mapX + it.x * mapScale, (3072f - it.y) * mapScale) }
        val path = Path(pts)
        // each world's ten levels sit on the stretch of road drawn in that world's region
        // of the picture (region borders in source pixels, measured on level_map.png)
        val borders = floatArrayOf(3010f, 2200f, 1600f, 1040f, 520f, 200f)
        val sAt = FloatArray(borders.size) { k -> sAtMapY(path, (3072f - borders[k]) * mapScale) }
        for (w in 0 until 5) {
            val s0 = sAt[w]
            val s1 = sAt[w + 1]
            for (k in 0 until 10) {
                path.pos(s0 + (s1 - s0) * (k + 0.5f) / 10f, nodes[w * 10 + k])
            }
        }
        val cur = (Save.level - firstLevel).coerceIn(0, LEVELS - 1)
        scroll = clampScroll(nodes[cur].y - H * 0.42f)
        val top = H - safeTop
        homeBtn.x = 50f
        homeBtn.y = top - 120f
        playBtn.x = W / 2
        playBtn.y = 210f
        val bar = 78f
        shopBtn.x = W / 2 - 240f; shopBtn.y = bar
        dailyBtn.x = W / 2 - 80f; dailyBtn.y = bar
        spinBtn.x = W / 2 + 80f; spinBtn.y = bar
        trophyBtn.x = W / 2 + 240f; trophyBtn.y = bar
        refillBtn.x = W / 2; refillBtn.y = H / 2 - 90f
        closeBtn.x = W / 2 + 250f; closeBtn.y = H / 2 + 200f
    }

    // the map may scroll a little past its bottom so level 1 clears the bottom bar
    /** First distance along the path whose point is at or above map-space height [y]. */
    private fun sAtMapY(path: Path, y: Float): Float {
        val v = Vector2()
        var d = 0f
        while (d < path.length) {
            path.pos(d, v)
            if (v.y >= y) return d
            d += 4f
        }
        return path.length
    }

    private fun clampScroll(v: Float) = MathUtils.clamp(v, -BOTTOM_PAD, max(-BOTTOM_PAD, mapH - H))

    override fun onDrag(dx: Float, dy: Float) {
        if (noLivesOpen) return
        dragging = true
        scroll = clampScroll(scroll - dy)
        velocity = -dy * 60f
    }

    override fun onRelease() {
        dragging = false
    }

    override fun onTap(x: Float, y: Float) {
        if (noLivesOpen) return
        for (i in 0 until LEVELS) {
            val n = nodes[i]
            if (Vector2.dst(x, y, n.x, n.y - scroll) < 46f) {
                val level = firstLevel + i
                if (level <= Save.level) tryPlay(level) else {
                    Assets.sfx("error", 0.5f)
                }
                return
            }
        }
    }

    private fun tryPlay(level: Int) {
        if (Save.lives <= 0) {
            noLivesOpen = true
            noLives = 0f
            Assets.sfx("no_lives", 0.8f)
            return
        }
        Assets.sfx("node_pop", 0.8f)
        goTo { game.setScreen(game.levelScreen(level)) }
    }

    private fun refill() {
        if (Save.coins >= 150) {
            Save.coins -= 150
            Save.refillAllLives()
            Assets.sfx("purchase", 0.9f)
            particles.sparkle(W / 2, H / 2, 14, 120f, Palette.GOLD)
            noLivesOpen = false
        } else {
            Assets.sfx("error", 0.7f)
        }
    }

    override fun onBack() {
        if (noLivesOpen) noLivesOpen = false else goTo { game.setScreen(MenuScreen(game)) }
    }

    override fun update(dt: Float) {
        if (!dragging) {
            scroll = clampScroll(scroll + velocity * dt)
            velocity *= 1f / (1f + dt * 4f)
            if (abs(velocity) < 5f) velocity = 0f
        }
        noLives += dt
        refillBtn.visible = noLivesOpen
        closeBtn.visible = noLivesOpen
        listOf(playBtn, shopBtn, dailyBtn, spinBtn, trophyBtn).forEach { it.enabled = !noLivesOpen }
        if (MathUtils.randomBoolean(dt * 1.5f)) particles.sparkle(MathUtils.random(W), MathUtils.random(H), 1, 10f)
        particles.update(dt)
    }

    override fun draw() {
        val b = batch
        b.color = Color.WHITE
        // three map tiles, top tile first
        for (i in 0..2) {
            val y = (2 - i) * 1024f * mapScale - scroll
            if (y > H || y + 1024f * mapScale < 0f) continue
            b.draw(Assets.texture(tiles[i]), mapX, y, 1024f * mapScale, 1024f * mapScale)
        }
        if (scroll < 0f) b.fillRect(0f, 0f, W, -scroll, 0.13f, 0.32f, 0.1f, 1f)
        // dotted trail between unlocked nodes
        for (i in 0 until LEVELS - 1) {
            val a = nodes[i]
            val c = nodes[i + 1]
            val done = firstLevel + i < Save.level
            for (k in 1..3) {
                val t = k / 4f
                val x = MathUtils.lerp(a.x, c.x, t)
                val y = MathUtils.lerp(a.y, c.y, t) - scroll
                if (y < -20f || y > H + 20f) continue
                b.setColor(if (done) 1f else 0.4f, if (done) 0.9f else 0.35f, if (done) 0.3f else 0.3f, 0.85f)
                b.drawCentered(Assets.r("circle"), x, y, 12f)
            }
        }
        b.color = Color.WHITE
        // world sign boards
        for (w in 0 until 5) {
            val n = nodes[w * 10]
            val y = n.y - scroll + 70f
            if (y < -100f || y > H + 100f) continue
            // put the sign on the side away from where the path heads next
            var ax = 0f
            for (k in 0 until 5) ax += nodes[(w * 10 + k).coerceAtMost(LEVELS - 1)].x
            ax /= 5f
            val sx = MathUtils.clamp(if (ax > n.x) n.x - 170f else n.x + 170f, 110f, W - 110f)
            b.drawCentered(Assets.r("ui_map_world_sign_board"), sx, y, 210f)
            b.text(Assets.fontBig, World.entries[w].title, sx, y + 16f, 20f)
        }
        // nodes
        for (i in 0 until LEVELS) {
            val level = firstLevel + i
            val n = nodes[i]
            val y = n.y - scroll
            if (y < -60f || y > H + 60f) continue
            val bossNode = World.isBoss(level)
            val current = level == Save.level
            val size = if (bossNode) 88f else 66f
            val reg = when {
                level > Save.level -> "ui_map_node_locked"
                bossNode -> "ui_map_boss_node"
                current -> "ui_map_node_open"
                else -> "ui_map_node_completed"
            }
            val pulse = if (current) 1f + sin(time * 5f) * 0.06f else 1f
            if (current) {
                b.setColor(1f, 1f, 0.6f, 0.6f + 0.3f * sin(time * 5f))
                b.drawCentered(Assets.r("glow"), n.x, y, size * 2f)
                b.color = Color.WHITE
            }
            b.drawCentered(Assets.r(reg), n.x, y, size * pulse)
            if (level <= Save.level && !bossNode) b.text(Assets.fontBig, labels[i], n.x, y + 2f, 27f)
            val st = if (level < Save.level) Save.stars(level) else 0
            if (st > 0) for (k in 0 until 3) {
                b.setColor(1f, 1f, 1f, if (k < st) 1f else 0.3f)
                b.drawCentered(Assets.r("ui_map_star_small"), n.x + (k - 1) * 24f, y - size * 0.55f + if (k == 1) -4f else 0f, 26f)
            }
            b.color = Color.WHITE
            if (current) {
                val bob = sin(time * 4f) * 8f
                b.drawCentered(Assets.r("ui_map_node_current_marker"), n.x, y + 78f + bob, 62f)
                b.drawCentered(Assets.r("ui_girl_map_avatar"), n.x + 70f, y + 30f, 74f)
            }
        }
        particles.draw(b)
        drawHud()
        if (noLivesOpen) drawNoLives()
    }

    private fun drawHud() {
        val b = batch
        val top = H - safeTop
        b.fillRect(0f, top - 90f, W, 90f + safeTop, 0f, 0f, 0f, 0.28f)
        Ui.lives(b, 24f, top - 46f)
        Ui.coins(b, W - 190f, top - 46f, 170f)
        homeBtn.draw(b)
        // bottom bar
        b.fillRect(0f, 0f, W, 150f, 0f, 0f, 0f, 0.35f)
        playBtn.draw(b)
        shopBtn.draw(b)
        dailyBtn.draw(b)
        spinBtn.draw(b)
        trophyBtn.draw(b)
        if (Save.dailyIndex >= 0) badge(dailyBtn, "!")
        if (Save.freeSpinReady) badge(spinBtn, "!")
        val un = Achievements.unclaimed
        if (un > 0) badge(trophyBtn, un.toString())
        b.text(Assets.fontSmall, "SHOP", shopBtn.x, shopBtn.y - 52f, 20f)
        b.text(Assets.fontSmall, "DAILY", dailyBtn.x, dailyBtn.y - 52f, 20f)
        b.text(Assets.fontSmall, "SPIN", spinBtn.x, spinBtn.y - 52f, 20f)
        b.text(Assets.fontSmall, "AWARDS", trophyBtn.x, trophyBtn.y - 52f, 20f)
    }

    private fun badge(btn: Button, text: String) {
        val b = batch
        val x = btn.x + 34f
        val y = btn.y + 34f + sin(time * 6f) * 3f
        b.drawCentered(Assets.r("booster_count_badge"), x, y, 36f)
        b.text(Assets.fontSmall, text, x, y + 1f, 20f)
    }

    private fun drawNoLives() {
        val b = batch
        val k = clamp01(noLives * 5f)
        Ui.dim(b, W, H, 0.6f * k)
        Ui.popup(b, W / 2, H / 2, 560f, 460f, "ribbon_red", "OUT OF LIVES", Ease.outBack(k))
        if (k < 1f) return
        b.drawCentered(Assets.r("ui_heart_broken"), W / 2, H / 2 + 100f, 130f)
        b.text(Assets.fontPlain, "Next life in " + Ui.clock(Save.nextLifeIn), W / 2, H / 2 + 10f, 34f, Palette.BROWN)
        refillBtn.draw(b)
        closeBtn.draw(b)
    }

    companion object {
        const val LEVELS = 50
        private const val ZOOM = 1.45f
        private const val BOTTOM_PAD = 240f
    }
}
