package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import kotlin.math.min
import kotlin.math.sin

class MenuScreen(game: SnakeAttackGame) : BaseScreen(game) {
    private val dragon = DragonHead()
    private val girl = Girl()
    private val particles = Particles()
    private var roarTimer = 2.5f
    private var titleIn = 0f
    private val levelText = "LEVEL ${Save.level}"

    private val playBtn = Button(Assets.r("ui_btn_play_big"), 330f) { play() }
    private val plusBtn = coinPlusButton()
    private val soundBtn = Button(Assets.r("ui_btn_sound_on"), 96f) { Save.sound = !Save.sound; refresh() }
    private val musicBtn = Button(Assets.r("ui_btn_music_on"), 96f) {
        Save.music = !Save.music
        Assets.refreshMusic()
        refresh()
    }

    init {
        buttons += playBtn
        buttons += soundBtn
        buttons += musicBtn
        playBtn.pulse = true
        dragon.scale = 0.7f
        dragon.angry = true
        girl.scale = 0.8f
        girl.mood = Girl.Mood.WAVE
        refresh()
    }

    override fun show() {
        super.show()
        Assets.playMusic("menu")
    }

    private fun refresh() {
        soundBtn.region = Assets.r(if (Save.sound) "ui_btn_sound_on" else "ui_btn_sound_off")
        musicBtn.region = Assets.r(if (Save.music) "ui_btn_music_on" else "ui_btn_music_off")
    }

    private fun play() = goTo { game.setScreen(MapScreen(game)) }

    override fun onBack() = game.exit()

    override fun layout() {
        playBtn.x = W / 2
        playBtn.y = H * 0.24f
        soundBtn.x = W / 2 - 70f
        musicBtn.x = W / 2 + 70f
        soundBtn.y = H * 0.1f
        musicBtn.y = H * 0.1f
        girl.baseX = W * 0.2f
        girl.baseY = H * 0.33f
        dragon.x = W * 0.62f
        dragon.y = H * 0.55f
    }

    override fun update(dt: Float) {
        titleIn = min(1f, titleIn + dt * 1.5f)
        dragon.mouthOpen = 0.35f + 0.25f * sin(time * 1.3f)
        dragon.rotation = -18f + sin(time * 1.1f) * 5f
        dragon.y = H * 0.55f + sin(time * 1.6f) * 10f
        dragon.update(dt)
        girl.update(dt)
        roarTimer -= dt
        if (roarTimer < 0f) {
            roarTimer = MathUtils.random(5f, 8f)
            dragon.chomp = 1.2f
            dragon.mouthPoint(com.badlogic.gdx.math.Vector2()).let { particles.smoke(it.x, it.y, 80f, 3, 40f) }
        }
        dragon.chomp = maxOf(0f, dragon.chomp - dt)
        if (MathUtils.randomBoolean(dt * 2f)) particles.sparkle(MathUtils.random(W), MathUtils.random(H * 0.6f, H), 1, 10f)
        particles.update(dt)
    }

    override fun draw() {
        val b = batch
        drawBackground(b)
        b.fillRect(-10f, -10f, W + 20f, H + 20f, 0f, 0.08f, 0f, 0.25f)
        // a coiled body trailing off-screen behind the head
        val colors = GemColor.entries
        dragon.drawNeck(b, dragon.x + 40f, dragon.y - 70f + sin(time * 3f) * 12f, 92f)
        for (i in 8 downTo 0) {
            val sx = dragon.x + 40f + i * 46f
            val sy = dragon.y - 70f - i * 26f + sin(time * 3f - i * 0.6f) * 12f
            b.setColor(0f, 0f, 0f, 0.25f)
            b.drawCentered(Assets.r("segment_shadow"), sx + 6f, sy - 40f, 110f)
            b.color = Color.WHITE
            b.drawCentered(Assets.segment(colors[i % colors.size]), sx, sy, 92f * (1f + sin(time * 5f - i) * 0.03f))
        }
        dragon.draw(b)
        girl.render(b)
        particles.draw(b)

        // title
        val s = Ease.outElastic(titleIn)
        val ty = H * 0.82f - safeTop * 0.5f
        b.text(Assets.fontBig, "SNAKE", W / 2, ty + 70f, 120f * s, Color.valueOf("7cf04a"))
        b.text(Assets.fontBig, "ATTACK!", W / 2, ty - 40f, 120f * s, Color.valueOf("ffb02e"))
        b.drawCentered(Assets.r("star_sparkle"), W / 2 + 220f, ty + 110f, 60f * (0.6f + 0.4f * sin(time * 4f)), rotation = time * 60f)

        playBtn.draw(b)
        b.text(Assets.fontBig, levelText, W / 2, playBtn.y - playBtn.height / 2 - 34f, 40f, Color.valueOf("ffe066"))
        soundBtn.draw(b)
        musicBtn.draw(b)

        // coins with the + (buy coins) button
        val top = H - safeTop
        Ui.coins(b, W - 230f, top - 50f, 190f, plusBtn)
    }
}
