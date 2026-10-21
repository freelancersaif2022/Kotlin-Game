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
    private var redFlash = 0f
    private var whiteFlash = 0f
    private var slowMo = 1f
    private var headExploded = false
    private val tmp = Vector2()
    private val floats = ArrayList<FloatText>()
    private var autoTimer = 0f
    private val autoplay = System.getProperty("snake.autoplay") != null
    private var debugPause = System.getProperty("snake.pause") != null
    private var pauseTime = 0f
    private val onArrive: (Block) -> Unit = { b -> b.slot?.let { combat.place(it, b.color, spec.shotsPerBlock) } }
    private val onHitRef: (Projectile) -> Unit = { onHit(it) }

    private class FloatText(val text: String, var x: Float, var y: Float, val size: Float, val color: Color, val life: Float = 1.1f) {
        var age = 0f
    }

    // HUD + overlay buttons
    private val settingsBtn = Button(Assets.r("btn_settings"), 76f) { openPause() }
    private val hintBtn = Button(Assets.r("rbtn_orange"), 88f) { useHint() }
    private val resumeBtn = Button(Assets.r("btn_green"), 380f, "RESUME", 46f) { closePause() }
    private val pauseHomeBtn = Button(Assets.r("rbtn_blue"), 104f) { goHome() }
    private val pauseRestartBtn = Button(Assets.r("rbtn_orange"), 104f) { restart() }
    private val soundBtn = Button(Assets.r("switch_on"), 112f) { Save.sound = !Save.sound; refreshToggles() }
    private val musicBtn = Button(Assets.r("switch_on"), 112f) { Save.music = !Save.music; Assets.refreshMusic(); refreshToggles() }
    private val vibBtn = Button(Assets.r("switch_on"), 112f) { Save.vibration = !Save.vibration; refreshToggles() }
    private val nextBtn = Button(Assets.r("btn_green"), 380f, "NEXT LEVEL", 44f) { nextLevel() }
    private val winHomeBtn = Button(Assets.r("rbtn_blue"), 104f) { goHome() }
    private val winReplayBtn = Button(Assets.r("rbtn_orange"), 104f) { restart() }
    private val retryBtn = Button(Assets.r("btn_orange"), 380f, "TRY AGAIN", 44f) { restart() }
    private val loseHomeBtn = Button(Assets.r("rbtn_blue"), 104f) { goHome() }
    private val pauseButtons = listOf(resumeBtn, pauseHomeBtn, pauseRestartBtn, soundBtn, musicBtn, vibBtn)
    private val winButtons = listOf(nextBtn, winHomeBtn, winReplayBtn)
    private val loseButtons = listOf(retryBtn, loseHomeBtn)
    private val levelText = "LEVEL $levelNumber"
    private var progressText = "0/$totalSegments"
    private var rewardText = ""
    private var destroyedText = ""
    private val settingRows = listOf("Sound" to soundBtn, "Music" to musicBtn, "Vibration" to vibBtn)

    init {
        combat.setup(5, if (Save.extraSlot) 5 else 4)
        buttons += settingsBtn
        buttons += hintBtn
        buttons += pauseButtons
        buttons += winButtons
        buttons += loseButtons
        hintBtn.icon = Assets.r("hint_lightbulb")
        hintBtn.iconSize = 50f
        fun icon(b: Button, name: String, size: Float) {
            b.icon = Assets.r(name)
            b.iconSize = size
        }
        icon(resumeBtn, "icon_next", 46f)
        icon(nextBtn, "icon_next", 46f)
        icon(retryBtn, "icon_restart", 50f)
        icon(pauseHomeBtn, "icon_home", 56f)
        icon(winHomeBtn, "icon_home", 56f)
        icon(loseHomeBtn, "icon_home", 56f)
        icon(pauseRestartBtn, "icon_restart", 56f)
        icon(winReplayBtn, "icon_restart", 56f)
        (pauseButtons + winButtons + loseButtons).forEach { it.visible = false }
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

        settingsBtn.x = W - 52f
        settingsBtn.y = H - 52f - safeTop
        layoutOverlay()
    }

    private fun layoutOverlay() {
        val cx = W / 2
        // pause popup
        val pc = H / 2
        resumeBtn.x = cx; resumeBtn.y = pc - 190f
        soundBtn.x = cx + 160f; soundBtn.y = pc + 100f
        musicBtn.x = cx + 160f; musicBtn.y = pc + 10f
        vibBtn.x = cx + 160f; vibBtn.y = pc - 80f
        pauseHomeBtn.x = cx - 80f; pauseHomeBtn.y = pc - 375f
        pauseRestartBtn.x = cx + 80f; pauseRestartBtn.y = pc - 375f
        // win popup
        val wc = winCenter()
        nextBtn.x = cx; nextBtn.y = wc - 150f
        winHomeBtn.x = cx - 80f; winHomeBtn.y = wc - 335f
        winReplayBtn.x = cx + 80f; winReplayBtn.y = wc - 335f
        // lose popup
        val lc = loseCenter()
        retryBtn.x = cx; retryBtn.y = lc - 125f
        loseHomeBtn.x = cx; loseHomeBtn.y = lc - 305f
    }

    private fun winCenter() = H / 2 - 40f
    private fun loseCenter() = H / 2 - 110f

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
        soundBtn.region = Assets.r(if (Save.sound) "switch_on" else "switch_off")
        musicBtn.region = Assets.r(if (Save.music) "switch_on" else "switch_off")
        vibBtn.region = Assets.r(if (Save.vibration) "switch_on" else "switch_off")
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
                floatText("SLOTS FULL!", W / 2, combat.slots.first().y + 70f, 40f, Palette.RED)
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
            particles.sparkle(slot.x, slot.y, 10, 60f, Palette.GOLD)
            particles.flash("shockwave_ring", slot.x, slot.y, 40f, 200f, 0.4f)
            Assets.sfx("unlock")
            floatText("UNLOCKED!", slot.x - 60f, slot.y + 80f, 38f, Palette.GOLD)
        } else {
            Assets.sfx("error", 0.7f)
            slot.shake = 0.3f
            floatText("NEED 150 COINS", slot.x - 120f, slot.y + 80f, 34f, Palette.RED)
        }
    }

    private fun useHint() {
        if (state != State.PLAY && state != State.INTRO) return
        val free = board.freeBlocks()
        if (free.isEmpty()) return
        if (Save.coins < 25) {
            Assets.sfx("error", 0.7f)
            floatText("NEED 25 COINS", hintBtn.x - 100f, hintBtn.y + 70f, 32f, Palette.RED)
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
        pauseTime = 0f
        Assets.sfx("swoosh_in", 0.5f)
        pauseButtons.forEach { it.visible = true; it.appear = 0f }
        settingsBtn.enabled = false
        hintBtn.enabled = false
    }

    private fun closePause() {
        paused = false
        pauseButtons.forEach { it.visible = false }
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
        pauseTime += dt
        // staggered pop-in of popup buttons
        pauseButtons.forEachIndexed { i, b -> if (b.visible && pauseTime > 0.12f + i * 0.04f) b.appear = min(1f, b.appear + dt * 5f) }
        winButtons.forEach { if (it.visible) it.appear = min(1f, it.appear + dt * 4f) }
        loseButtons.forEach { if (it.visible) it.appear = min(1f, it.appear + dt * 4f) }
        if (paused) return
        val gdt = dt * slowMo
        slowMo = min(1f, slowMo + dt * 1.5f)
        stateTime += gdt
        redFlash = max(0f, redFlash - dt * 2f)
        whiteFlash = max(0f, whiteFlash - dt * 3f)

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
        board.update(gdt, onArrive)
        if (state == State.PLAY) board.nudgeIdle(gdt)
        combat.update(gdt, if (state == State.PLAY || state == State.INTRO) snake else null, W + 30f, state == State.PLAY, onHitRef)
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
        progressText = "$kills/$totalSegments"
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
            floatText(word, W / 2, y + 90f, 56f, Palette.GOLD)
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
                particles.burst(hx, hy, Palette.BLAST, 30, 600f, 26f)
                particles.scales(hx, hy, Assets.r("dragon_upper_head"), 4)
                particles.smoke(hx, hy, 140f, 6, 60f)
                particles.sparkle(hx, hy, 12, 120f, Palette.GOLD)
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
            rewardText = "+$reward"
            winButtons.forEach { it.visible = true; it.appear = -0.6f }
            nextBtn.pulse = true
        }
        for (i in 1..stars) {
            val at = 2.0f + i * 0.35f
            if (t > at && t - dt <= at) {
                Assets.sfx("star$i", 0.9f, 1f, 0f)
                particles.sparkle(W / 2 + (i - 2) * 140f, starY(i), 8, 50f, Palette.GOLD)
            }
        }
        if (t > 3.4f && t - dt <= 3.4f) {
            particles.coinsFly(W / 2, winCenter() + 10f, 10)
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
            loseButtons.forEach { it.visible = true; it.appear = -0.5f }
            destroyedText = "Snake destroyed: $kills / $totalSegments"
            retryBtn.pulse = true
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
        if (whiteFlash > 0f) b.fillRect(-20f, -20f, W + 40f, H + 40f, 1f, 1f, 1f, whiteFlash)
        if (redFlash > 0f) b.fillRect(-20f, -20f, W + 40f, H + 40f, 1f, 0.1f, 0.1f, redFlash * 0.45f)
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
        b.text(Assets.fontBig, "HELP!", bx + 4f, by + 6f, 34f * s, Palette.HELP_RED)
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
        val top = H - safeTop
        // level badge + snake progress bar
        b.drawCentered(Assets.r("banner_level"), W / 2, top - 50f, 270f)
        b.text(Assets.fontBig, levelText, W / 2, top - 46f, 36f)
        val pw = 210f
        val px = W / 2 - pw / 2
        val py = top - 102f
        Assets.pill.draw(b, px - 6f, py - 8f, pw + 12f, 28f)
        val frac = kills / totalSegments.toFloat()
        if (frac > 0f) b.fillRect(px, py - 1f, pw * frac, 14f, Palette.GREEN)
        b.text(Assets.fontSmall, progressText, W / 2, py + 6f, 19f)

        settingsBtn.draw(b)
        if (state == State.PLAY || state == State.INTRO) {
            hintBtn.draw(b)
            Assets.pill.draw(b, hintBtn.x - 32f, hintBtn.y - 66f, 64f, 30f)
            b.drawCentered(Assets.r("coin"), hintBtn.x - 14f, hintBtn.y - 51f, 22f)
            b.text(Assets.fontSmall, "25", hintBtn.x + 10f, hintBtn.y - 50f, 20f, Palette.GOLD)
        }
        if (state == State.INTRO) drawIntro()
    }

    /** "LEVEL n" ribbon that swoops in from the left, holds, then leaves to the right. */
    private fun drawIntro() {
        val b = batch
        val t = stateTime
        val inT = Ease.outBack(clamp01(t / 0.45f), 1.2f)
        val outT = Ease.inCubic(clamp01((t - 1.55f) / 0.35f))
        val x = W / 2 + (1f - inT) * -W + outT * W
        val y = board.panelY + board.panelH / 2 + 50f
        b.setColor(0f, 0f, 0f, 0.35f * (1f - outT))
        b.drawCentered(Assets.r("glow"), W / 2, y - 20f, W * 1.3f, 360f)
        b.color = Color.WHITE
        b.drawCentered(Assets.r("ribbon_orange"), x, y, 560f)
        b.text(Assets.fontBig, levelText, x, y + 10f, 60f)
        val sub = clamp01((t - 0.3f) / 0.3f) * (1f - outT)
        if (sub > 0f) {
            val w = 470f
            Assets.pill.draw(b, W / 2 - w / 2, y - 120f, w, 56f)
            b.text(Assets.fontSmall, "Save her from the dragon!", W / 2, y - 91f, 30f, alpha = sub)
        }
    }

    private fun dim(alpha: Float) = batch.fillRect(-20f, -20f, W + 40f, H + 40f, 0.02f, 0.04f, 0.02f, alpha)

    /** Framed popup with a title ribbon across its top edge. */
    private fun drawPopup(cy: Float, w: Float, h: Float, ribbon: String, title: String, scale: Float) {
        if (scale <= 0f) return
        val b = batch
        val pw = w * scale
        val ph = h * scale
        val cx = W / 2
        b.setColor(0f, 0f, 0f, 0.45f)
        b.drawCentered(Assets.r("glow"), cx, cy - 20f, pw * 1.5f, ph * 1.4f)
        b.color = Color.WHITE
        Assets.framePanel.draw(b, cx - pw / 2, cy - ph / 2, pw, ph)
        val ry = cy + ph / 2 - 6f * scale
        b.drawCentered(Assets.r(ribbon), cx, ry, (w + 60f) * scale)
        b.text(Assets.fontBig, title, cx, ry + 10f * scale, 52f * scale)
    }

    private fun drawPause() {
        val b = batch
        val k = clamp01(pauseTime * 5f)
        dim(0.62f * k)
        val s = Ease.outBack(k)
        val cy = H / 2
        drawPopup(cy, 560f, 600f, "ribbon_blue", "PAUSED", s)
        if (s < 0.95f) return
        // coin balance
        val cw = 220f
        Assets.pill.draw(b, W / 2 - cw / 2, cy + 165f, cw, 60f)
        b.drawCentered(Assets.r("coin"), W / 2 - cw / 2 + 30f, cy + 195f, 48f)
        b.text(Assets.fontBig, Save.coins.toString(), W / 2 + 20f, cy + 197f, 34f, Palette.GOLD)
        // settings rows
        for ((label, btn) in settingRows) {
            b.fillRect(W / 2 - 220f, btn.y - 40f, 440f, 2f, 0.85f, 0.7f, 0.5f, 0.6f)
            b.text(Assets.fontPlain, label, W / 2 - 205f, btn.y + 2f, 38f, Palette.BROWN, alignX = 0f)
        }
        pauseButtons.forEach { it.draw(b) }
    }

    private fun starY(i: Int) = winCenter() + 260f + if (i == 2) 120f else 82f

    private fun drawWin() {
        val b = batch
        val t = stateTime - 1.6f
        val k = clamp01(t * 3f)
        dim(0.6f * k)
        val cy = winCenter()
        drawRays(W / 2, starY(2) - 20f, 1f, Palette.RAYS_WIN, k * 0.5f)
        drawPopup(cy, 560f, 520f, "ribbon_green", levelText, Ease.outBack(k))
        if (k < 1f) return
        b.text(Assets.fontBig, "COMPLETED!", W / 2, cy + 140f, 62f, Palette.DARK_GREEN)
        b.text(Assets.fontPlain, "You saved her from the dragon!", W / 2, cy + 80f, 30f, Palette.BROWN)
        // stars sit on top of the ribbon
        for (i in 1..3) {
            val at = 0.4f + i * 0.35f
            val st = clamp01((t - at) / 0.35f)
            val sx = W / 2 + (i - 2) * 140f
            val sy = starY(i)
            val size = if (i == 2) 160f else 124f
            val rot = (i - 2) * -12f
            b.setColor(0.3f, 0.22f, 0.15f, 0.55f)
            b.drawCentered(Assets.r("star"), sx, sy, size, rotation = rot)
            if (i <= stars && st > 0f) {
                b.color = Color.WHITE
                b.drawCentered(Assets.r("star"), sx, sy, size * Ease.outElastic(st), rotation = rot + (1f - st) * 90f)
            }
        }
        b.color = Color.WHITE
        // reward
        val ct = clamp01((t - 1.7f) * 3f)
        val rw = 300f
        Assets.pill.draw(b, W / 2 - rw / 2, cy - 30f, rw, 76f)
        if (ct > 0f) {
            b.drawCentered(Assets.r("coin"), W / 2 - 70f, cy + 8f, 60f * Ease.outBack(ct))
            b.text(Assets.fontBig, rewardText, W / 2 + 26f, cy + 10f, 50f * ct, Palette.GOLD)
        }
        winButtons.forEach { it.draw(b) }
    }

    private fun drawLose() {
        val b = batch
        val t = stateTime - 1.1f
        val k = clamp01(t * 2.5f)
        dim(0.66f * k)
        val cy = loseCenter()
        val skullY = cy + 230f + 182f
        drawRays(W / 2, skullY, 1.05f, Palette.RAYS_LOSE, k * 0.6f)
        val s = Ease.outBack(k)
        // sunburst behind the panel, skull in front of it (above the ribbon)
        if (s > 0f) b.drawCentered(Assets.r("sunburst_rays"), W / 2, skullY, 420f * s, rotation = -time * 20f)
        drawPopup(cy, 560f, 460f, "ribbon_red", levelText, s)
        if (s > 0f) b.drawCentered(Assets.r("skull_fail"), W / 2, skullY, 200f * s, rotation = sin(time * 3f) * 4f)
        if (k < 1f) return
        b.text(Assets.fontBig, "LEVEL FAILED", W / 2, cy + 120f, 60f, Palette.DARK_RED)
        b.text(Assets.fontPlain, "The dragon reached her!", W / 2, cy + 62f, 32f, Palette.BROWN)
        b.text(Assets.fontPlain, destroyedText, W / 2, cy + 16f, 28f, Palette.BROWN)
        loseButtons.forEach { it.draw(b) }
    }

    private fun drawRays(x: Float, y: Float, scale: Float, color: Color, alpha: Float) {
        if (alpha <= 0f) return
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
