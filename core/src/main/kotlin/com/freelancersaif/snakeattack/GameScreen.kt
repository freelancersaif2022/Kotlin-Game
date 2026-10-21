package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class GameScreen(game: SnakeAttackGame, private val levelNumber: Int) : BaseScreen(game) {
    private enum class State { INTRO, PLAY, WIN, LOSE }

    private val spec = LevelGen.generate(levelNumber)
    private val particles = Particles()
    private val board = Board(spec)
    private val combat = Combat(particles)
    private val girl = Girl()
    private lateinit var path: Path
    private lateinit var snake: Snake
    private var state = State.INTRO
    private var stateTime = 0f
    private var paused = false
    private var goalS = 0f
    private var entryS = 0f
    private var introS = 0f
    private var startS = 0f
    private val totalSegments = spec.snakeColors.size
    private var kills = 0
    private var combo = 0
    private var comboTimer = 0f
    private var hintBlock: Block? = null
    private var hintTimer = 0f
    private var helpBubble = 0f
    private var nostrilTimer = 1.5f
    private var roaredDanger = false
    private var stars = 0
    private var reward = 0
    private var shownCoins = Save.coins.toFloat()
    private var redFlash = 0f
    private var whiteFlash = 0f
    private var slowMo = 1f
    private var headExploded = false
    private val tmp = Vector2()
    private val floats = ArrayList<FloatText>()
    private var autoTimer = 0f
    private val autoplay = System.getProperty("snake.autoplay") != null
    private var debugPause = System.getProperty("snake.pause") != null

    private class FloatText(val text: String, var x: Float, var y: Float, val size: Float, val color: Color, val life: Float = 1.1f) {
        var age = 0f
    }

    // HUD + overlay buttons
    private val settingsBtn = Button(Assets.r("btn_settings"), 74f) { openPause() }
    private val hintBtn = Button(Assets.r("btn_orange"), 92f) { useHint() }
    private val resumeBtn = Button(Assets.r("btn_green"), 300f, "RESUME", 44f) { closePause() }
    private val restartBtn = Button(Assets.r("btn_orange"), 300f, "RESTART", 44f) { restart() }
    private val homeBtn = Button(Assets.r("btn_blue"), 300f, "HOME", 44f) { goHome() }
    private val soundBtn = Button(Assets.r("btn_green"), 160f, "", 22f) { Save.sound = !Save.sound; refreshToggles() }
    private val musicBtn = Button(Assets.r("btn_green"), 160f, "", 22f) { Save.music = !Save.music; Assets.refreshMusic(); refreshToggles() }
    private val vibBtn = Button(Assets.r("btn_green"), 160f, "", 22f) { Save.vibration = !Save.vibration; refreshToggles() }
    private val nextBtn = Button(Assets.r("btn_green"), 320f, "NEXT", 50f) { nextLevel() }
    private val retryBtn = Button(Assets.r("btn_orange"), 320f, "TRY AGAIN", 46f) { restart() }
    private val endHomeBtn = Button(Assets.r("btn_blue"), 200f, "HOME", 38f) { goHome() }
    private val overlayButtons = listOf(resumeBtn, restartBtn, homeBtn, soundBtn, musicBtn, vibBtn)
    private val endButtons = listOf(nextBtn, retryBtn, endHomeBtn)

    init {
        combat.setup(5, if (Save.extraSlot) 5 else 4)
        buttons += settingsBtn
        buttons += hintBtn
        buttons += overlayButtons
        buttons += endButtons
        hintBtn.icon = Assets.r("hint_lightbulb")
        hintBtn.iconSize = 46f
        overlayButtons.forEach { it.visible = false }
        endButtons.forEach { it.visible = false }
        girl.scale = 0.32f
        refreshToggles()
    }

    override fun show() {
        super.show()
        Assets.playMusic("game")
    }

    override fun layout() {
        val s = bgScale()
        // road centre-line traced from the background art (image pixels), far right first
        val road = listOf(
            1024f to 186f, 950f to 188f, 870f to 198f, 800f to 222f, 720f to 262f, 640f to 275f,
            550f to 272f, 480f to 250f, 400f to 212f, 300f to 190f, 200f to 183f, 100f to 200f,
            0f to 218f, -80f to 224f,
        )
        val pts = ArrayList<Vector2>()
        val first = bgToWorld(road[0].first, road[0].second, Vector2())
        pts += Vector2(W + 1800f, first.y)
        pts += Vector2(W + 600f, first.y)
        road.forEach { (ix, iy) -> pts += bgToWorld(ix, iy, Vector2()) }
        val fresh = !::path.isInitialized
        // progress along the visible road survives a re-layout (e.g. window resize)
        val progress = if (fresh) 0f else (snake.headS - introS) / (goalS - introS)
        path = Path(pts)

        // girl stands at the left end of the road
        val girlX = 64f
        val gy = groundAt(girlX)
        girl.baseX = girlX
        girl.baseY = gy - 6f
        goalS = sAtX(girlX + 92f)
        entryS = sAtX(W + 70f)
        introS = sAtX(W * 0.55f)
        startS = sAtX(W + 420f)

        if (fresh) {
            snake = Snake(path, spec.snakeColors)
            snake.headS = startS
        } else {
            snake.path = path
            snake.headS = introS + progress * (goalS - introS)
        }
        snake.speed = (goalS - introS) / spec.duration

        // slots row under the road, board below
        val roadBottom = H + BG_TOP_OFFSET - (275f + 70f) * s
        val slotY = roadBottom - 74f
        combat.slotSize = 104f
        combat.layout(W / 2 - 34f, slotY, 112f)
        hintBtn.x = W - 52f
        hintBtn.y = slotY
        val boardTop = slotY - 82f
        board.layout(W / 2 - 350f, 34f, 700f, boardTop - 34f)

        settingsBtn.x = W - 50f
        settingsBtn.y = H - 50f
        layoutOverlay()
    }

    private fun layoutOverlay() {
        val cx = W / 2
        val cy = H / 2
        resumeBtn.x = cx; resumeBtn.y = cy + 110f
        restartBtn.x = cx; restartBtn.y = cy - 15f
        homeBtn.x = cx; homeBtn.y = cy - 140f
        soundBtn.x = cx - 170f; musicBtn.x = cx; vibBtn.x = cx + 170f
        soundBtn.y = cy - 265f; musicBtn.y = cy - 265f; vibBtn.y = cy - 265f
        nextBtn.x = cx; nextBtn.y = cy - 170f
        retryBtn.x = cx; retryBtn.y = cy - 230f
        endHomeBtn.x = cx; endHomeBtn.y = cy - 330f
    }

    private fun groundAt(x: Float): Float {
        var s = 0f
        while (s < path.length) {
            path.pos(s, tmp)
            if (tmp.x <= x) return tmp.y
            s += 4f
        }
        return tmp.y
    }

    private fun sAtX(x: Float): Float {
        var s = 0f
        while (s < path.length) {
            path.pos(s, tmp)
            if (tmp.x <= x) return s
            s += 2f
        }
        return path.length
    }

    private fun refreshToggles() {
        fun set(b: Button, name: String, on: Boolean) {
            b.region = Assets.r(if (on) "btn_green" else "btn_grey")
            b.label = "$name ${if (on) "ON" else "OFF"}"
        }
        set(soundBtn, "SOUND", Save.sound)
        set(musicBtn, "MUSIC", Save.music)
        set(vibBtn, "VIBRATE", Save.vibration)
    }

    // ------------------------------------------------------------------ input

    override fun onTouch(x: Float, y: Float) {
        if (paused || (state != State.PLAY && state != State.INTRO)) return
        combat.slotAt(x, y)?.let { slot ->
            if (slot.locked) tryUnlock(slot)
            return
        }
        val b = board.blockAt(x, y) ?: return
        if (b.state != Block.State.IDLE || b.appear < 0.6f) return
        tapBlock(b)
    }

    private fun tapBlock(b: Block) {
        if (b === hintBlock) hintBlock = null
        if (board.blockedAfter(b) < 0) {
            val slot = combat.freeSlot()
            if (slot == null) {
                Assets.sfx("error", 0.7f)
                combat.slots.forEach { it.shake = 0.3f }
                b.squash.kick(-5f)
                floatText("SLOTS FULL!", W / 2, combat.slots.first().y + 70f, 40f, Color.valueOf("ff6b6b"))
                Save.vibrate(30)
                return
            }
            slot.reserved = true
            board.launch(b, slot)
            Assets.sfx("tap", 0.8f)
            Assets.sfx("whoosh", 0.6f, 1.1f)
            particles.smoke(b.home.x, b.home.y, 50f, 2, 20f)
        } else {
            board.bump(b)
            Assets.sfx("bonk", 0.7f)
            Save.vibrate(25)
        }
    }

    private fun tryUnlock(slot: Slot) {
        if (Save.coins >= 150) {
            Save.coins -= 150
            Save.extraSlot = true
            slot.locked = false
            slot.pop.value = 0.5f
            slot.pop.kick(8f)
            particles.sparkle(slot.x, slot.y, 10, 60f, Color.valueOf("ffe066"))
            particles.flash("shockwave_ring", slot.x, slot.y, 40f, 200f, 0.4f)
            Assets.sfx("unlock")
            floatText("UNLOCKED!", slot.x - 60f, slot.y + 80f, 38f, Color.valueOf("ffe066"))
        } else {
            Assets.sfx("error", 0.7f)
            slot.shake = 0.3f
            floatText("NEED 150 COINS", slot.x - 120f, slot.y + 80f, 34f, Color.valueOf("ff6b6b"))
        }
    }

    private fun useHint() {
        if (state != State.PLAY && state != State.INTRO) return
        val free = board.freeBlocks()
        if (free.isEmpty()) return
        if (Save.coins < 25) {
            Assets.sfx("error", 0.7f)
            floatText("NEED 25 COINS", hintBtn.x - 100f, hintBtn.y + 70f, 32f, Color.valueOf("ff6b6b"))
            return
        }
        Save.coins -= 25
        val wanted = snake.segments.firstOrNull { s -> free.any { it.color == s.color } }?.color
        hintBlock = free.firstOrNull { it.color == wanted } ?: free.first()
        hintTimer = 6f
        Assets.sfx("combo", 0.6f)
    }

    override fun onBack() {
        when {
            state == State.WIN || state == State.LOSE -> goHome()
            paused -> closePause()
            else -> openPause()
        }
    }

    private fun openPause() {
        if (state == State.WIN || state == State.LOSE) return
        paused = true
        overlayButtons.forEach { it.visible = true; it.appear = 0f }
        settingsBtn.enabled = false
        hintBtn.enabled = false
    }

    private fun closePause() {
        paused = false
        overlayButtons.forEach { it.visible = false }
        settingsBtn.enabled = true
        hintBtn.enabled = true
    }

    private fun restart() = goTo { game.setScreen(GameScreen(game, levelNumber)) }
    private fun nextLevel() = goTo { game.setScreen(GameScreen(game, levelNumber + 1)) }
    private fun goHome() = goTo { game.setScreen(MenuScreen(game)) }

    private fun floatText(text: String, x: Float, y: Float, size: Float, color: Color) {
        floats += FloatText(text, x, y, size, color)
    }

    // ----------------------------------------------------------------- update

    override fun update(dt: Float) {
        overlayButtons.forEach { it.appear = min(1f, it.appear + dt * 5f) }
        endButtons.forEach { if (it.visible) it.appear = min(1f, it.appear + dt * 4f) }
        if (paused) return
        val gdt = dt * slowMo
        slowMo = min(1f, slowMo + dt * 1.5f)
        stateTime += gdt
        redFlash = max(0f, redFlash - dt * 2f)
        whiteFlash = max(0f, whiteFlash - dt * 3f)
        shownCoins += (Save.coins - shownCoins) * min(1f, dt * 8f)

        val visibleTotal = goalS - introS
        val ratio = MathUtils.clamp((goalS - snake.headS) / visibleTotal, 0f, 1f)

        when (state) {
            State.INTRO -> {
                // the snake slithers in fast, roars, then the hunt begins
                val target = introS
                snake.boost = if (snake.headS < target) 520f else 0f
                if (stateTime > 0.35f && helpBubble == 0f) {
                    helpBubble = 2.6f
                    Assets.sfx("scream", 0.5f, 1.1f)
                }
                if (snake.headS >= target && stateTime > 1.2f) {
                    state = State.PLAY
                    stateTime = 0f
                    snake.head.angry = true
                    snake.head.chomp = 1f
                    Assets.sfx("roar", 0.9f)
                    shake = 0.5f
                    particles.smoke(snake.head.x - 40f, snake.head.y + 10f, 90f, 4, 40f)
                }
                girl.mood = Girl.Mood.WORRIED
                snake.update(gdt, true)
            }
            State.PLAY -> {
                snake.boost = 0f
                snake.update(gdt, true)
                snake.head.chomp = max(0f, snake.head.chomp - gdt)
                snake.head.mouthOpen = 0.2f + (1f - ratio) * 0.55f
                if (stateTime > 2f) snake.head.angry = ratio < 0.45f
                girl.mood = when {
                    ratio < 0.2f -> Girl.Mood.PANIC
                    ratio < 0.5f -> Girl.Mood.WORRIED
                    else -> Girl.Mood.IDLE
                }
                if (ratio < 0.3f && !roaredDanger) {
                    roaredDanger = true
                    Assets.sfx("roar", 0.8f, 1.1f)
                    snake.head.chomp = 1.5f
                    helpBubble = 2.2f
                    shake = 0.35f
                }
                if (snake.headS >= goalS) lose()
                if (snake.segments.isEmpty() && combat.projectiles.isEmpty()) win()
                if (autoplay) autoPlay(gdt)
                if (debugPause && stateTime > 1f) {
                    debugPause = false
                    openPause()
                }
            }
            State.WIN -> updateWin(gdt)
            State.LOSE -> updateLose(gdt)
        }

        // nostril smoke puffs
        nostrilTimer -= gdt
        if (nostrilTimer < 0f && !snake.dead && state != State.WIN) {
            nostrilTimer = MathUtils.random(1.6f, 3f)
            snake.head.mouthPoint(tmp)
            particles.spawn(Assets.r("dragon_nostril_smoke"), tmp.x - 6f, tmp.y + 30f).apply {
                vx = -40f; vy = 50f; size0 = 30f; size1 = 70f; life = 0.9f; alpha0 = 0.8f; alpha1 = 0f
                rot = 10f
            }
        }

        helpBubble = max(0f, helpBubble - gdt)
        if (hintTimer > 0f) {
            hintTimer -= gdt
            if (hintTimer <= 0f) hintBlock = null
        }
        comboTimer -= gdt
        if (comboTimer <= 0f) combo = 0

        girl.update(gdt)
        board.update(gdt) { b -> b.slot?.let { combat.place(it, b.color, spec.shotsPerBlock) } }
        if (state == State.PLAY) board.nudgeIdle(gdt)
        combat.update(gdt, if (state == State.PLAY || state == State.INTRO) snake else null, W + 30f, state == State.PLAY, ::onHit)
        particles.update(gdt)
        floats.removeAll { f ->
            f.age += dt
            f.y += dt * 60f
            f.age > f.life
        }
    }

    private fun onHit(p: Projectile) {
        val seg = p.target
        val idx = snake.segments.indexOf(seg)
        val x = seg.pos.x
        val y = seg.pos.y
        if (idx >= 0) {
            snake.segments.removeAt(idx)
            if (idx < snake.segments.size) snake.segments[idx].gap += seg.gap
        }
        seg.dead = true
        kills++
        // neighbours flinch
        if (idx > 0) snake.segments.getOrNull(idx - 1)?.let { it.scaleKick.kick(3f); it.flash = 0.6f }
        snake.segments.getOrNull(idx)?.let { it.scaleKick.kick(3f); it.flash = 0.6f }
        snake.pushBack(3f)
        if (idx in 0..2) snake.head.hit()

        val tint = seg.color.tint
        particles.flash("explosion", x, y, 40f, 130f, 0.32f, MathUtils.random(360f), additive = false)
        particles.flash("hit_spark", x, y, 60f, 120f, 0.18f, MathUtils.random(360f))
        particles.flash("shockwave_ring", x, y, 30f, 150f, 0.35f, tint = tint)
        particles.burst(x, y, tint, 12, 380f, 18f)
        particles.scales(x, y, Assets.segment(seg.color), 6)
        particles.smoke(x, y, 60f, 2, 50f)
        shake = max(shake, 0.18f)
        Assets.sfx("hit", 0.75f, 1f, 0.1f)
        if (MathUtils.randomBoolean(0.5f)) Assets.sfx("explode", 0.35f, 1.2f, 0.1f)
        Save.vibrate(12)

        combo++
        comboTimer = 0.9f
        if (combo == 4 || combo == 8 || combo == 12) {
            val word = when (combo) { 4 -> "NICE!"; 8 -> "AWESOME!"; else -> "UNSTOPPABLE!" }
            floatText(word, W / 2, y + 90f, 56f, Color.valueOf("ffe066"))
            Assets.sfx("combo", 0.8f)
        }
        if (snake.segments.isEmpty()) slowMo = 0.25f
    }

    private fun autoPlay(dt: Float) {
        autoTimer -= dt
        if (autoTimer > 0f) return
        autoTimer = 0.45f
        val free = board.freeBlocks()
        val front = snake.segments.take(8).map { it.color }
        val pick = free.firstOrNull { it.color in front } ?: free.firstOrNull()
        if (pick != null && combat.freeSlot() != null) tapBlock(pick)
    }

    // --------------------------------------------------------------- win/lose

    private fun win() {
        state = State.WIN
        stateTime = 0f
        val ratio = (goalS - snake.headS) / (goalS - introS)
        stars = when {
            ratio > 0.5f -> 3
            ratio > 0.25f -> 2
            else -> 1
        }
        reward = 20 + stars * 10
        snake.head.hurt = true
        snake.head.angry = false
        snake.head.mouthOpen = 0f
        girl.mood = Girl.Mood.CHEER
        Assets.sfx("roar", 0.8f, 1.35f)
        hintBlock = null
    }

    private fun updateWin(dt: Float) {
        snake.update(dt, false)
        val t = stateTime
        if (!headExploded) {
            snake.head.flash = 0.5f + 0.5f * sin(t * 30f)
            snake.head.x += sin(t * 60f) * 2f
            if (t > 1.0f) {
                headExploded = true
                snake.dead = true
                snake.head.mouthPoint(tmp)
                val hx = snake.head.x - 40f
                val hy = snake.head.y
                particles.flash("explosion", hx, hy, 80f, 300f, 0.55f, additive = false)
                particles.flash("shockwave_ring", hx, hy, 40f, 380f, 0.5f)
                particles.burst(hx, hy, Color.valueOf("ff7a2a"), 30, 600f, 26f)
                particles.scales(hx, hy, Assets.r("dragon_upper_head"), 4)
                particles.smoke(hx, hy, 140f, 6, 60f)
                particles.sparkle(hx, hy, 12, 120f, Color.valueOf("ffe066"))
                snake.tailPos(tmp)
                if (tmp.x < W + 60f) {
                    particles.flash("explosion", tmp.x, tmp.y, 50f, 160f, 0.4f, additive = false)
                    particles.smoke(tmp.x, tmp.y, 90f, 3, 50f)
                }
                shake = 0.8f
                whiteFlash = 0.6f
                Assets.sfx("explode", 1f, 0.8f)
                Save.vibrate(60)
            }
        }
        if (t > 1.6f && t - dt <= 1.6f) {
            particles.confetti(W / 2, H + 40f, W, 90)
            Assets.sfx("win", 0.9f)
            Save.level = max(Save.level, levelNumber + 1)
            Save.setStars(levelNumber, stars)
            Save.coins += reward
            nextBtn.visible = true
            nextBtn.appear = 0f
            nextBtn.pulse = true
            endHomeBtn.visible = true
            endHomeBtn.appear = 0f
        }
        for (i in 1..stars) {
            val at = 2.0f + i * 0.35f
            if (t > at && t - dt <= at) {
                Assets.sfx("star$i", 0.9f, 1f, 0f)
                particles.sparkle(W / 2 + (i - 2) * 130f, H / 2 + 150f + if (i == 2) 30f else 0f, 8, 50f, Color.valueOf("ffe066"))
            }
        }
        if (t > 3.4f && t - dt <= 3.4f) {
            particles.coinsFly(W / 2, H / 2 - 40f, 10)
            Assets.sfx("coin", 0.9f)
        }
    }

    private fun lose() {
        state = State.LOSE
        stateTime = 0f
        girl.mood = Girl.Mood.CAUGHT
        snake.head.chomp = 3f
        snake.head.mouthOpen = 1f
        Assets.sfx("bite", 1f)
        Assets.sfx("scream", 0.7f)
        redFlash = 1f
        shake = 0.9f
        Save.vibrate(120)
        hintBlock = null
    }

    private fun updateLose(dt: Float) {
        snake.update(dt, stateTime < 0.4f)
        snake.head.chomp = max(0f, snake.head.chomp - dt)
        val t = stateTime
        if (t > 0.5f && t - dt <= 0.5f) {
            Assets.sfx("roar", 0.9f, 0.9f)
            particles.smoke(girl.x, girl.y + 60f, 120f, 6, 40f)
        }
        if (t > 1.3f && t - dt <= 1.3f) {
            Assets.sfx("lose", 0.9f)
            retryBtn.visible = true
            retryBtn.appear = 0f
            retryBtn.pulse = true
            endHomeBtn.visible = true
            endHomeBtn.appear = 0f
        }
    }

    // ------------------------------------------------------------------- draw

    override fun draw() {
        val b = batch
        drawBackground(b)
        if (!(state == State.LOSE && stateTime > 0.6f)) girl.render(b)
        snake.draw(b, W)
        if (state == State.LOSE && stateTime > 0.6f) {
            // she is hiding behind the dragon now – draw her faded and shaking
            girl.alpha = max(0f, 1f - (stateTime - 0.6f) * 2f)
            girl.render(b)
        }
        if (helpBubble > 0f) drawHelpBubble()
        drawDanger()

        combat.draw(b)
        board.draw(b, hintBlock)
        board.drawFlying(b)
        combat.drawProjectiles(b)
        particles.draw(b)
        drawHud()
        floats.forEach { f ->
            val a = 1f - clamp01((f.age - f.life * 0.6f) / (f.life * 0.4f))
            val pop = Ease.outBack(min(1f, f.age * 5f))
            b.text(Assets.fontBig, f.text, f.x, f.y, f.size * pop, f.color, alpha = a)
        }
        if (whiteFlash > 0f) b.fillRect(-20f, -20f, W + 40f, H + 40f, Color(1f, 1f, 1f, whiteFlash))
        if (redFlash > 0f) b.fillRect(-20f, -20f, W + 40f, H + 40f, Color(1f, 0.1f, 0.1f, redFlash * 0.45f))
        when {
            paused -> drawPause()
            state == State.WIN && stateTime > 1.6f -> drawWin()
            state == State.LOSE && stateTime > 1.1f -> drawLose()
        }
    }

    private fun drawHelpBubble() {
        val b = batch
        girl.headTop(tmp)
        val t = 2.6f - helpBubble
        val s = Ease.outBack(min(1f, t * 4f)) * min(1f, helpBubble * 3f)
        val wob = sin(time * 9f) * 3f
        val bx = tmp.x + 66f
        val by = tmp.y + 18f
        b.drawCentered(Assets.r("speech_bubble_help"), bx, by, 120f * s, rotation = wob)
        b.text(Assets.fontBig, "HELP!", bx + 4f, by + 6f, 34f * s, Color.valueOf("e8412c"))
    }

    private fun drawDanger() {
        if (state != State.PLAY) return
        val ratio = (goalS - snake.headS) / (goalS - introS)
        if (ratio > 0.3f) return
        val k = (0.3f - ratio) / 0.3f
        val p = 0.5f + 0.5f * sin(time * 8f)
        val b = batch
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        b.setColor(1f, 0.1f, 0.05f, k * (0.25f + 0.35f * p))
        val g = Assets.r("glow")
        b.drawCentered(g, 0f, H / 2, 300f, H * 1.2f)
        b.drawCentered(g, W, H / 2, 300f, H * 1.2f)
        b.drawCentered(g, W / 2, H, W * 1.3f, 260f)
        b.drawCentered(g, W / 2, 0f, W * 1.3f, 260f)
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        b.color = Color.WHITE
    }

    private fun drawHud() {
        val b = batch
        // level banner + snake progress
        val bannerW = 270f
        b.drawCentered(Assets.r("banner_level"), W / 2, H - 50f, bannerW)
        b.text(Assets.fontBig, "LEVEL $levelNumber", W / 2, H - 47f, 36f)
        val pw = 200f
        val px = W / 2 - pw / 2
        val py = H - 100f
        Assets.pill.draw(b, px - 6f, py - 7f, pw + 12f, 26f)
        val frac = kills / totalSegments.toFloat()
        if (frac > 0f) b.fillRect(px, py - 1f, pw * frac, 12f, Color.valueOf("7cf04a"))
        b.text(Assets.fontSmall, "$kills/$totalSegments", W / 2, py + 5f, 18f)

        // coins
        val cw = 150f
        val cx = W - 100f - cw
        val cy = H - 50f
        Assets.pill.draw(b, cx, cy - 26f, cw, 52f)
        b.drawCentered(Assets.r("coin"), cx + 8f, cy, 52f, rotation = sin(time * 2f) * 6f)
        b.text(Assets.fontBig, shownCoins.toInt().toString(), cx + cw / 2 + 14f, cy + 1f, 32f)

        settingsBtn.draw(b)
        if (state == State.PLAY || state == State.INTRO) {
            hintBtn.draw(b)
            b.drawCentered(Assets.r("coin"), hintBtn.x - 18f, hintBtn.y - 44f, 22f)
            b.text(Assets.fontSmall, "25", hintBtn.x + 8f, hintBtn.y - 44f, 22f, Color.valueOf("ffe066"))
        }
        // intro banner
        if (state == State.INTRO) {
            val t = stateTime
            val s = Ease.outBack(min(1f, t * 3f)) * (1f - clamp01((t - 1.4f) * 4f))
            if (s > 0f) {
                val iy = board.panelY + board.panelH / 2
                b.text(Assets.fontBig, "LEVEL $levelNumber", W / 2, iy + 40f, 92f * s, Color.valueOf("ffe066"))
                b.text(Assets.fontBig, "Save her from the snake!", W / 2, iy - 40f, 40f * s)
            }
        }
    }

    private fun dim(alpha: Float) = batch.fillRect(-20f, -20f, W + 40f, H + 40f, Color(0.02f, 0.05f, 0.02f, alpha))

    private fun drawPause() {
        val b = batch
        dim(0.6f)
        val s = Ease.outBack(resumeBtn.appear)
        val pw = 580f * s
        val ph = 700f * s
        Assets.panel.draw(b, W / 2 - pw / 2, H / 2 - ph / 2 - 30f, pw, ph)
        b.text(Assets.fontBig, "PAUSED", W / 2, H / 2 + 240f, 64f * s, Color.valueOf("ffb02e"))
        overlayButtons.forEach { it.draw(b) }
    }

    private fun drawWin() {
        val b = batch
        val t = stateTime - 1.6f
        dim(min(0.55f, t * 2f))
        // rotating light rays behind the panel
        drawRays(W / 2, H / 2 + 150f, 1f, Color.valueOf("fff3b0"), min(1f, t * 2f) * 0.55f)
        val s = Ease.outBack(min(1f, t * 3f))
        val pw = 600f * s
        val ph = 640f * s
        Assets.panel.draw(b, W / 2 - pw / 2, H / 2 - ph / 2 - 30f, pw, ph)
        b.text(Assets.fontBig, "LEVEL COMPLETE!", W / 2, H / 2 + 225f, 54f * s, Color.valueOf("4ccf2a"))
        for (i in 1..3) {
            val at = 0.4f + i * 0.35f
            val st = clamp01((t - at) / 0.35f)
            val sx = W / 2 + (i - 2) * 130f
            val sy = H / 2 + 120f + if (i == 2) 30f else 0f
            val size = if (i == 2) 150f else 120f
            b.setColor(0.25f, 0.2f, 0.15f, 0.35f)
            b.drawCentered(Assets.r("star"), sx, sy, size)
            if (i <= stars && st > 0f) {
                b.color = Color.WHITE
                b.drawCentered(Assets.r("star"), sx, sy, size * Ease.outElastic(st), rotation = (1f - st) * 90f)
            }
        }
        b.color = Color.WHITE
        val ct = clamp01((t - 1.7f) * 3f)
        if (ct > 0f) {
            b.drawCentered(Assets.r("coin"), W / 2 - 60f, H / 2 - 40f, 64f * Ease.outBack(ct))
            b.text(Assets.fontBig, "+$reward", W / 2 + 30f, H / 2 - 38f, 54f * ct, Color.valueOf("ffe066"))
        }
        nextBtn.draw(b)
        endHomeBtn.draw(b)
    }

    private fun drawLose() {
        val b = batch
        val t = stateTime - 1.1f
        dim(min(0.65f, t * 2f))
        drawRays(W / 2, H / 2 + 110f, 1.1f, Color.valueOf("ffb347"), min(1f, t * 2f) * 0.7f)
        val s = Ease.outBack(min(1f, t * 2.5f))
        val wob = sin(time * 3f) * 4f
        b.drawCentered(Assets.r("sunburst_rays"), W / 2, H / 2 + 110f, 520f * s, rotation = -time * 20f)
        b.drawCentered(Assets.r("skull_fail"), W / 2, H / 2 + 110f, 300f * s, rotation = wob)
        b.text(Assets.fontBig, "OH NO!", W / 2, H / 2 - 70f, 84f * s, Color.valueOf("ff5a4a"))
        b.text(Assets.fontBig, "The snake got her!", W / 2, H / 2 - 130f, 38f * s)
        retryBtn.draw(b)
        endHomeBtn.draw(b)
    }

    private fun drawRays(x: Float, y: Float, scale: Float, color: Color, alpha: Float) {
        val b = batch
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        b.setColor(color.r, color.g, color.b, alpha)
        val ray = Assets.r("ray")
        for (i in 0 until 12) {
            val a = time * 18f + i * 30f
            b.draw(ray, x - 40f * scale, y, 40f * scale, 0f, 80f * scale, 560f * scale, 1f, 1f, a)
        }
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        b.color = Color.WHITE
    }
}
