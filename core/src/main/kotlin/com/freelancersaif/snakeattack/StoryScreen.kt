package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import kotlin.math.min
import kotlin.math.sin

/** Full-screen comic panel with a caption; tap to continue into [nextLevel] (or the map after the finale). */
class StoryScreen(game: SnakeAttackGame, private val story: Int, private val nextLevel: Int) : BaseScreen(game) {
    private val image = "story/story_$story.jpg"
    private val caption = Story.captions[story - 1]
    private var shown = 0f
    private val particles = Particles()
    override val background = World.of(nextLevel.coerceAtLeast(1)).bg

    override fun show() {
        super.show()
        Assets.keepOnly(listOf(image, background))
        Assets.sfx("page", 0.8f)
        if (story == 5) Assets.sfx("win", 0.8f) else Assets.sfx("swoosh_in", 0.6f)
    }

    override fun onTap(x: Float, y: Float) = proceed()
    override fun onBack() = proceed()

    private fun proceed() {
        if (shown < 0.6f) return
        Save.setStorySeen(story)
        Assets.sfx("page", 0.7f)
        goTo { game.setScreen(if (story == 5) MapScreen(game) else GameScreen(game, nextLevel)) }
    }

    override fun update(dt: Float) {
        shown += dt
        if (story == 5 && sin(time * 3f) > 0.98f) particles.confetti(W / 2, H + 40f, W, 12)
        particles.update(dt)
    }

    override fun draw() {
        val b = batch
        drawBackground(b)
        Ui.dim(b, W, H, 0.72f)
        val top = H - safeTop
        // chapter ribbon
        val k = Ease.outBack(min(1f, shown * 3f))
        b.drawCentered(Assets.r("ribbon_purple"), W / 2, top - 90f, 520f * k)
        b.text(Assets.fontBig, if (story == 5) "THE END?" else "CHAPTER $story", W / 2, top - 82f, 46f * k)
        // panel with a slow Ken Burns zoom
        val tex = Assets.texture(image)
        val pw = W - 40f
        val ph = pw * 0.75f
        val py = H / 2 + 40f
        val zoom = 1f + min(shown, 8f) * 0.012f
        val fx = 12f
        Assets.framePanel.draw(b, W / 2 - pw / 2 - fx, py - ph / 2 - fx, pw + fx * 2, ph + fx * 2)
        val u = (1f - 1f / zoom) / 2f
        b.color = Color.WHITE
        b.draw(tex, W / 2 - pw / 2, py - ph / 2, pw, ph,
            (u * tex.width).toInt(), (u * tex.height).toInt(), (tex.width / zoom).toInt(), (tex.height / zoom).toInt(), false, false)
        // caption
        val cy = py - ph / 2 - 120f
        Assets.framePanel.draw(b, 30f, cy - 80f, W - 60f, 160f)
        drawWrapped(caption, W / 2, cy + 18f, W - 120f)
        val blink = 0.5f + 0.5f * sin(time * 4f)
        if (shown > 0.8f) b.text(Assets.fontBig, "TAP TO CONTINUE", W / 2, 120f, 34f, alpha = 0.5f + 0.5f * blink)
        particles.draw(b)
    }

    /** Two-line caption: splits at the space nearest the middle when the text is too wide. */
    private fun drawWrapped(text: String, x: Float, y: Float, maxW: Float) {
        val size = 32f
        if (textWidth(Assets.fontPlain, text, size) <= maxW) {
            batch.text(Assets.fontPlain, text, x, y - 16f, size, Palette.BROWN)
            return
        }
        val mid = text.length / 2
        var cut = text.lastIndexOf(' ', mid)
        if (cut < 0) cut = text.indexOf(' ', mid)
        batch.text(Assets.fontPlain, text.substring(0, cut), x, y + 4f, size, Palette.BROWN)
        batch.text(Assets.fontPlain, text.substring(cut + 1), x, y - 40f, size, Palette.BROWN)
    }
}
