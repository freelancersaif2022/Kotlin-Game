package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color

/** Base for the shop / rewards / awards screens: themed backdrop, title ribbon, back button and coins. */
abstract class MetaScreen(game: SnakeAttackGame, private val title: String, private val ribbon: String) : BaseScreen(game) {
    protected val particles = Particles()
    protected val toasts = ArrayList<Ui.Toast>()
    private val backBtn = Button(Assets.r("ui_back_arrow_button"), 82f) { back() }
    private val plusBtn = coinPlusButton()
    override val background = World.of(Save.level).bg

    init {
        buttons += backBtn
    }

    override fun show() {
        super.show()
        Assets.playMusic("map")
        Assets.keepOnly(listOf(background))
    }

    protected val top get() = H - safeTop

    override fun layout() {
        backBtn.x = 56f
        backBtn.y = top - 56f
    }

    open fun back() = goTo { game.setScreen(MapScreen(game)) }

    override fun onBack() = back()

    protected fun toast(title: String, text: String, icon: String) {
        toasts += Ui.Toast(title, text, icon)
    }

    override fun update(dt: Float) {
        particles.update(dt)
        toasts.firstOrNull()?.let { t ->
            t.t += dt
            if (t.t > 3f) toasts.removeAt(0)
        }
        tick(dt)
    }

    open fun tick(dt: Float) {}
    abstract fun content()

    override fun draw() {
        val b = batch
        drawBackground(b)
        Ui.dim(b, W, H, 0.55f)
        content()
        particles.draw(b)
        b.color = Color.WHITE
        b.fillRect(0f, top - 112f, W, 112f + safeTop, 0f, 0f, 0f, 0.25f)
        b.drawCentered(Assets.r(ribbon), W / 2 - 34f, top - 56f, 320f)
        b.text(Assets.fontBig, title, W / 2 - 34f, top - 48f, 34f)
        backBtn.draw(b)
        Ui.coins(b, W - 196f, top - 56f, 170f, plusBtn)
        toasts.firstOrNull()?.let { Ui.drawToast(b, W, top - 110f, it) }
    }
}
