package com.bramble.dash.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.bramble.dash.BrambleDash
import com.bramble.dash.render.Backdrop
import com.bramble.dash.ui.UiKit

/**
 * Common screen plumbing: a Scene2D stage on a 1280x720 ExtendViewport (no stretching on wide or
 * narrow displays), a root table padded by the display-cutout safe insets, an animated parallax
 * backdrop and Android back-key handling.
 */
abstract class BaseScreen(val game: BrambleDash) : ScreenAdapter() {
    val ui: UiKit get() = game.ui
    val stage = Stage(ExtendViewport(1280f, 720f), game.batch)
    val root = Table()
    private val bgCamera = OrthographicCamera()
    private val bgViewport = ExtendViewport(1280f, 720f, bgCamera)
    private var backdrop: Backdrop? = null
    protected var backdropScroll = 0f
    protected open val backdropSpeed = 40f
    protected open val backdropDim = 0.35f
    private var built = false

    protected open fun backdropWorld(): String {
        val sel = game.save.data.selectedWorld
        return if (game.assets.isGroupLoaded("world_$sel")) sel else "jungle"
    }

    abstract fun build()

    override fun show() {
        root.setFillParent(true)
        stage.addActor(root)
        val keys = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) { onBack(); return true }
                return false
            }
        }
        Gdx.input.inputProcessor = InputMultiplexer(stage, keys)
        if (!built) { build(); built = true }
        applySafeArea()
    }

    open fun onBack() {}

    /** Rebuild after data changes (purchases etc.). */
    fun rebuild() {
        root.clearChildren()
        build()
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.07f, 0.1f, 0.16f, 1f)
        game.assets.update()
        drawBackdrop(delta)
        stage.act(delta.coerceAtMost(0.1f))
        stage.draw()
        afterDraw(delta)
    }

    protected open fun afterDraw(delta: Float) {}

    protected fun drawBackdrop(delta: Float) {
        val wid = backdropWorld()
        if (backdrop?.worldId != wid && game.assets.isGroupLoaded("world_$wid")) backdrop = Backdrop(game.assets, wid)
        val b = backdrop ?: return
        backdropScroll += backdropSpeed * delta
        bgViewport.apply()
        val vw = bgViewport.worldWidth
        val vh = bgViewport.worldHeight
        bgCamera.position.set(backdropScroll + vw / 2f, vh / 2f, 0f)
        bgCamera.update()
        val batch = game.batch
        batch.projectionMatrix = bgCamera.combined
        batch.begin()
        batch.color = Color.WHITE
        b.draw(batch, backdropScroll, backdropScroll, vw, vh, game.save.data.settings.quality)
        b.drawGround(batch, backdropScroll, vw)
        if (backdropDim > 0f) {
            batch.setColor(0.03f, 0.05f, 0.1f, backdropDim)
            batch.draw(game.assets.white, backdropScroll, 0f, vw, vh)
            batch.color = Color.WHITE
        }
        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
    }

    override fun resize(width: Int, height: Int) {
        bgViewport.update(width, height, false)
        stage.viewport.update(width, height, true)
        applySafeArea()
    }

    /** Keep controls clear of notches / rounded corners. */
    protected fun applySafeArea() {
        val sw = Gdx.graphics.width.coerceAtLeast(1)
        val k = stage.viewport.worldWidth / sw
        val l = Gdx.graphics.safeInsetLeft * k
        val r = Gdx.graphics.safeInsetRight * k
        val t = Gdx.graphics.safeInsetTop * k
        val b = Gdx.graphics.safeInsetBottom * k
        root.pad(16f + t, 16f + l, 16f + b, 16f + r)
        root.invalidateHierarchy()
    }

    override fun hide() {
        if (Gdx.input.inputProcessor is InputMultiplexer) Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        stage.dispose()
    }
}
