package com.bramble.dash.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.bramble.dash.BrambleDash
import com.bramble.dash.render.drawOrig

/** First screen: loads fonts, UI, shared atlases and preloads every sound effect. */
class LoadingScreen(private val game: BrambleDash) : com.badlogic.gdx.ScreenAdapter() {
    private val viewport = ExtendViewport(1280f, 720f)
    private var shown = 0f
    private var font: BitmapFont? = null
    private val layout = GlyphLayout()

    override fun show() {
        val a = game.assets
        // the loading panel itself needs these first
        a.manager.finishLoadingAsset<TextureAtlas>(a.atlasPath("ui_panels"))
        a.manager.finishLoadingAsset<TextureAtlas>(a.atlasPath("ui"))
        a.manager.finishLoadingAsset<BitmapFont>("fonts/body.fnt")
        a.update(0)
        font = a.font("body")
    }

    override fun render(delta: Float) {
        val a = game.assets
        val done = a.update(12)
        shown += (a.progress - shown) * (delta * 8f).coerceAtMost(1f)
        ScreenUtils.clear(0.07f, 0.1f, 0.16f, 1f)
        viewport.apply()
        val b = game.batch
        b.projectionMatrix = viewport.camera.combined
        b.begin()
        val cx = viewport.worldWidth / 2f
        val cy = viewport.worldHeight / 2f
        val panel = a.region("ui/screens/loading_panel")
        b.drawOrig(panel, cx - 480f, cy - 270f, 1.5f)
        val logo = a.region("ui/lobby/logo")
        b.drawOrig(logo, cx - 300f, cy - 40f, 600f / 720f)
        val bar = a.region("ui/gameplay/progress")
        val bx = cx - 240f
        val by = cy - 170f
        b.draw(bar, bx, by, 480f, 102f)
        // cover the unfilled part of the baked bar
        b.setColor(48 / 255f, 68 / 255f, 95 / 255f, 1f)
        val f = shown.coerceIn(0f, 1f)
        b.draw(a.white, bx + (14f + 266f * f) * 1.5f, by + (68f - 45f) * 1.5f, 266f * (1f - f) * 1.5f, 23f * 1.5f)
        b.color = Color.WHITE
        font?.let {
            layout.setText(it, "Loading ${(f * 100).toInt()}%")
            it.draw(b, layout, cx - layout.width / 2f, by - 14f)
        }
        b.end()
        if (done && shown > 0.98f) {
            game.onCoreLoaded()
            game.go(LobbyScreen(game))
        }
    }

    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }
}
