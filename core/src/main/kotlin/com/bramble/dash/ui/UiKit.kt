package com.bramble.dash.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.bramble.dash.assets.GameAssets
import com.bramble.dash.audio.AudioService
import com.bramble.dash.audio.Sfx
import com.bramble.dash.render.drawPivot

/** Builds Scene2D widgets from the pack's UI components (4-state buttons, panels, 9-patch, icons). */
class UiKit(val assets: GameAssets, private val audio: () -> AudioService) {
    val titleFont: BitmapFont = assets.font("title")
    val bodyFont: BitmapFont = assets.font("body")
    val smallFont: BitmapFont = assets.font("small")
    /** Proper NinePatch from the converted panel.9.png (marker border removed, splits from the atlas). */
    private val panelPatch: NinePatch = assets.manager.get(assets.atlasPath("ui"), com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java)
        .createPatch("ui/components/panel9") ?: error("panel9 has no split data")

    init {
        for (f in listOf(titleFont, bodyFont, smallFont)) {
            f.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            f.setUseIntegerPositions(false)
            f.data.markupEnabled = true
        }
    }

    // ------------------------------------------------------------------ colours
    companion object {
        val CREAM = Color(1f, 0.93f, 0.75f, 1f)
        val TEAL = Color(0.53f, 0.89f, 0.85f, 1f)
        val GOLD = Color(1f, 0.83f, 0.35f, 1f)
        val RED = Color(1f, 0.45f, 0.42f, 1f)
        val GREY = Color(0.65f, 0.7f, 0.78f, 1f)
        val PANEL_TINT = Color(1f, 1f, 1f, 1f)
        val DIM = Color(0f, 0f, 0f, 0.6f)
    }

    fun drawable(name: String): TextureRegionDrawable = TextureRegionDrawable(assets.region(name))

    fun panel(tint: Color = PANEL_TINT): NinePatchDrawable = NinePatchDrawable(panelPatch).tint(tint)

    fun whiteDrawable(c: Color): Drawable = TextureRegionDrawable(assets.white).tint(c)

    fun label(text: String, font: BitmapFont = bodyFont, color: Color = Color.WHITE, scale: Float = 1f, wrap: Boolean = false, align: Int = Align.left): Label {
        val l = Label(text, Label.LabelStyle(font, color))
        l.setFontScale(scale)
        l.wrap = wrap
        l.setAlignment(align)
        return l
    }

    fun image(name: String): Image = Image(drawable(name), Scaling.fit)

    /** A button made from the pack's normal/pressed/disabled/selected art (selected = checked). */
    fun stateButton(base: String, sound: Sfx = Sfx.UI_CLICK, toggle: Boolean = false, onClick: (Button) -> Unit = {}): Button {
        val style = Button.ButtonStyle().apply {
            up = drawable("$base/normal")
            down = drawable("$base/pressed")
            disabled = drawable("$base/disabled")
            checked = drawable("$base/selected")
            if (!toggle) checked = null
        }
        val b = Button(style)
        b.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (b.isDisabled) { audio().play(Sfx.PURCHASE_FAIL, 0.5f); return }
                audio().play(sound)
                onClick(b)
            }
        })
        return b
    }

    enum class Tone(val up: Color, val down: Color) {
        PRIMARY(Color(0.45f, 0.86f, 0.82f, 1f), Color(0.3f, 0.62f, 0.6f, 1f)),
        GOLD(Color(1f, 0.8f, 0.45f, 1f), Color(0.8f, 0.6f, 0.3f, 1f)),
        PLAIN(Color(1f, 1f, 1f, 1f), Color(0.7f, 0.7f, 0.75f, 1f)),
        DANGER(Color(1f, 0.55f, 0.5f, 1f), Color(0.75f, 0.35f, 0.32f, 1f)),
    }

    /** Runtime-labelled button on the 9-patch panel (for actions without baked button art). */
    fun textButton(text: String, tone: Tone = Tone.PRIMARY, font: BitmapFont = smallFont, sound: Sfx = Sfx.UI_CLICK, onClick: (TextButton) -> Unit = {}): TextButton {
        val style = TextButton.TextButtonStyle().apply {
            up = panel(tone.up); down = panel(tone.down); disabled = panel(Color(0.5f, 0.52f, 0.58f, 1f))
            checked = panel(Color(1f, 0.75f, 0.4f, 1f))
            this.font = font
            fontColor = Color.WHITE
            disabledFontColor = Color(0.8f, 0.8f, 0.8f, 1f)
        }
        val b = TextButton(text, style)
        b.pad(10f, 22f, 12f, 22f)
        b.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (b.isDisabled) { audio().play(Sfx.PURCHASE_FAIL, 0.5f); return }
                audio().play(sound)
                onClick(b)
            }
        })
        return b
    }

    fun slider(value: Float, onChange: (Float) -> Unit): Slider {
        val style = Slider.SliderStyle().apply {
            background = whiteDrawable(Color(0.12f, 0.17f, 0.26f, 1f)).also { it.minHeight = 14f }
            knobBefore = whiteDrawable(TEAL).also { it.minHeight = 14f }
            knob = TextureRegionDrawable(assets.region("ui/icons/star")).also { it.minWidth = 46f; it.minHeight = 46f }
        }
        val s = Slider(0f, 1f, 0.05f, false, style)
        s.value = value
        s.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { onChange(s.value) }
        })
        return s
    }

    fun scroll(content: Actor): ScrollPane {
        val sp = ScrollPane(content, ScrollPane.ScrollPaneStyle())
        sp.setScrollingDisabled(true, false)
        sp.setFadeScrollBars(false)
        sp.setOverscroll(false, true)
        return sp
    }

    /** Coins / gems / keys / tokens strip using the pack's icons. */
    fun currencyBar(values: () -> IntArray): Table {
        val t = Table()
        val icons = arrayOf("ui/icons/gold", "ui/icons/diamond", "ui/icons/key", "ui/icons/star")
        val labels = Array(4) { label("0", smallFont, CREAM) }
        for (i in 0 until 4) {
            val cell = Table()
            cell.background = panel(Color(1f, 1f, 1f, 0.95f))
            cell.add(image(icons[i])).size(34f).padRight(6f)
            cell.add(labels[i]).minWidth(60f)
            t.add(cell).height(52f).padLeft(8f)
        }
        t.addAction(Actions.forever(Actions.run {
            val v = values()
            for (i in 0 until 4) labels[i].setText(formatNumber(v[i]))
        }))
        return t
    }

    fun formatNumber(n: Int): String {
        if (n < 10000) return n.toString()
        val s = n.toString()
        val sb = StringBuilder()
        for (i in s.indices) { if (i > 0 && (s.length - i) % 3 == 0) sb.append(','); sb.append(s[i]) }
        return sb.toString()
    }

    /** Short message that fades out. */
    fun toast(stage: Stage, text: String, color: Color = CREAM) {
        val t = Table()
        t.background = panel(Color(1f, 1f, 1f, 0.97f))
        t.add(label(text, smallFont, color)).pad(12f, 24f, 14f, 24f)
        t.pack()
        t.setPosition((stage.width - t.width) / 2f, stage.height * 0.18f)
        t.touchable = Touchable.disabled
        t.color.a = 0f
        t.addAction(Actions.sequence(Actions.fadeIn(0.15f), Actions.delay(1.8f), Actions.fadeOut(0.4f), Actions.removeActor()))
        stage.addActor(t)
    }

    /** Modal confirmation using the pack's "confirmation" component and shop confirm/cancel buttons. */
    fun confirm(stage: Stage, message: String, onYes: () -> Unit) {
        val shade = Table()
        shade.setFillParent(true)
        shade.background = whiteDrawable(DIM)
        shade.touchable = Touchable.enabled
        val box = Table()
        box.background = drawable("ui/components/confirmation")
        box.add(label(message, smallFont, CREAM, wrap = true, align = Align.center)).width(520f).padTop(120f).padBottom(30f).colspan(2).row()
        box.add(stateButton("ui/shop/confirm") { shade.remove(); onYes() }).size(230f, 70f).pad(10f)
        box.add(stateButton("ui/shop/cancel", Sfx.UI_BACK) { shade.remove() }).size(230f, 70f).pad(10f)
        shade.add(box).size(640f, 384f)
        shade.color.a = 0f
        shade.addAction(Actions.fadeIn(0.15f))
        stage.addActor(shade)
    }

    /** Reward reveal: reward card component, celebration animation and the "next" button. */
    fun rewardReveal(stage: Stage, title: String, text: String, onClose: () -> Unit = {}) {
        val shade = Table()
        shade.setFillParent(true)
        shade.background = whiteDrawable(DIM)
        shade.touchable = Touchable.enabled
        val box = Table()
        box.background = drawable("ui/components/reward_card")
        val fx = AnimActor(assets.anim("effects/unlock_celebration"), 1.4f, loop = true)
        box.add(fx).size(200f, 140f).padTop(60f).row()
        box.add(label(title, bodyFont, GOLD, align = Align.center)).padTop(6f).row()
        box.add(label(text, smallFont, CREAM, wrap = true, align = Align.center)).width(520f).pad(8f).row()
        box.add(stateButton("ui/rewards/next") { shade.remove(); onClose() }).size(240f, 73f).pad(10f)
        shade.add(box).size(640f, 440f)
        box.isTransform = true
        box.setOrigin(320f, 220f)
        box.setScale(0.6f)
        box.addAction(Actions.scaleTo(1f, 1f, 0.25f, com.badlogic.gdx.math.Interpolation.swingOut))
        stage.addActor(shade)
    }
}

/** Plays an [com.bramble.dash.assets.Anim] inside a Scene2D layout cell, centred. */
class AnimActor(var anim: com.bramble.dash.assets.Anim, var drawScale: Float = 1f, var loop: Boolean = true, var flipX: Boolean = false) : Actor() {
    private var t = 0f
    var pivotMode = false
    fun restart() { t = 0f }
    override fun act(delta: Float) { super.act(delta); t += delta }
    override fun draw(batch: Batch, parentAlpha: Float) {
        val time = if (loop) t % anim.duration else t
        val r = anim.frame(time)
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        if (pivotMode) {
            val m = anim.meta(time)
            batch.drawPivot(r, m, x + width / 2f, y + 6f, drawScale, flipX)
        } else {
            val w = r.originalWidth * drawScale
            val h = r.originalHeight * drawScale
            val left = x + (width - w) / 2f
            val bottom = y + (height - h) / 2f
            batch.draw(r, left + r.offsetX * drawScale, bottom + r.offsetY * drawScale, r.packedWidth * drawScale, r.packedHeight * drawScale)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }
}

/**
 * HUD bar built on the pack's bar art (health_bar / boss_health / progress): draws the art, then
 * covers the empty part of the baked fill with the frame's interior colour.
 */
class BarActor(private val region: TextureRegion, private val white: TextureRegion, private val fillX0: Float, private val fillX1: Float,
               private val fillYTop: Float, private val fillYBottom: Float, private val empty: Color) : Actor() {
    var fraction = 1f
    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.setColor(1f, 1f, 1f, parentAlpha * color.a)
        batch.draw(region, x, y, width, height)
        val f = fraction.coerceIn(0f, 1f)
        if (f < 1f) {
            val sx = width / region.regionWidth
            val sy = height / region.regionHeight
            val x0 = x + (fillX0 + (fillX1 - fillX0) * f) * sx
            val x1 = x + fillX1 * sx
            val y0 = y + (region.regionHeight - fillYBottom) * sy
            val y1 = y + (region.regionHeight - fillYTop) * sy
            batch.setColor(empty.r, empty.g, empty.b, parentAlpha * color.a)
            batch.draw(white, x0, y0, x1 - x0, y1 - y0)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }
}
