package com.freelancersaif.snakeattack

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Common camera / viewport / transition handling for every screen. */
abstract class BaseScreen(val game: SnakeAttackGame) : ScreenAdapter() {
    val camera = OrthographicCamera()
    // no maximum size: any aspect ratio (tall phones, tablets) fills the whole screen, no bars
    val viewport = ExtendViewport(720f, 1280f, 0f, 0f, camera)
    val batch get() = game.batch
    val W get() = viewport.worldWidth
    val H get() = viewport.worldHeight

    /** Height (world units) hidden by a notch / status bar at the top; keep HUD below it. */
    val safeTop: Float
        get() {
            val px = Gdx.graphics.safeInsetTop
            return if (px <= 0 || Gdx.graphics.height <= 0) 0f else px * H / Gdx.graphics.height
        }
    private var fadeIn = 1f
    private var fadeOut = -1f
    private var next: (() -> Unit)? = null
    var shake = 0f
    private val touch = Vector2()
    private val downAt = Vector2()
    private val lastDrag = Vector2()
    private var dragged = false
    private var buttonDown = false
    protected val buttons = ArrayList<Button>()
    var time = 0f

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer > 0 || fadeOut >= 0f) return true
            viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
            downAt.set(touch)
            lastDrag.set(touch)
            dragged = false
            buttonDown = false
            for (b in buttons.asReversed()) if (b.enabled && b.visible && b.appear > 0.5f && b.contains(touch.x, touch.y)) {
                b.pressed = true
                buttonDown = true
                return true
            }
            onTouch(touch.x, touch.y)
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (pointer > 0) return true
            viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
            if (!dragged && touch.dst(downAt) > 14f) {
                dragged = true
                // a drag that started on a button becomes a scroll instead of a click
                buttons.forEach { it.pressed = false }
            }
            if (dragged) onDrag(touch.x - lastDrag.x, touch.y - lastDrag.y)
            lastDrag.set(touch)
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (pointer > 0) return true
            viewport.unproject(touch.set(screenX.toFloat(), screenY.toFloat()))
            var clicked = false
            for (b in buttons.asReversed()) if (b.pressed) {
                b.pressed = false
                if (b.contains(touch.x, touch.y) && b.enabled && b.visible) {
                    b.pop.kick(-6f)
                    Assets.sfx("click", 0.8f)
                    b.onClick()
                    clicked = true
                }
            }
            if (!clicked && !buttonDown) {
                if (dragged) onRelease() else onTap(touch.x, touch.y)
            }
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            if (keycode == com.badlogic.gdx.Input.Keys.BACK || keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                onBack()
                return true
            }
            return false
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = input
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.BACK, true)
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        layout()
    }

    open fun layout() {}
    /** Immediate touch-down (used by the puzzle board for responsive taps). */
    open fun onTouch(x: Float, y: Float) {}

    /** A press + release that didn't move (map nodes, cards). */
    open fun onTap(x: Float, y: Float) {}

    /** Finger moved while held (scrolling); deltas in world units. */
    open fun onDrag(dx: Float, dy: Float) {}

    open fun onRelease() {}

    /** Background image drawn by [drawBackground]. */
    open val background = "bg/bg_forest.jpg"
    open fun onBack() {}
    abstract fun update(dt: Float)
    abstract fun draw()

    fun goTo(action: () -> Unit) {
        if (fadeOut >= 0f) return
        fadeOut = 0f
        next = action
    }

    override fun render(delta: Float) {
        val dt = min(delta, 1 / 30f)
        time += dt
        update(dt)
        buttons.forEach { it.update(dt) }
        shake = max(0f, shake - dt * 2.2f)
        val sx = if (shake > 0f) MathUtils.random(-1f, 1f) * shake * 14f else 0f
        val sy = if (shake > 0f) MathUtils.random(-1f, 1f) * shake * 14f else 0f
        camera.position.set(W / 2 + sx, H / 2 + sy, 0f)
        camera.update()
        Gdx.gl.glClearColor(0.12f, 0.3f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.projectionMatrix = camera.combined
        batch.begin()
        draw()
        // transitions
        if (fadeIn > 0f) {
            fadeIn = max(0f, fadeIn - dt * 3f)
            batch.fillRect(-50f, -50f, W + 100f, H + 100f, 0.06f, 0.1f, 0.05f, fadeIn)
        }
        if (fadeOut >= 0f) {
            fadeOut = min(1f, fadeOut + dt * 3.5f)
            batch.fillRect(-50f, -50f, W + 100f, H + 100f, 0.06f, 0.1f, 0.05f, fadeOut)
        }
        batch.end()
        if (fadeOut >= 1f) {
            val n = next
            next = null
            fadeOut = -1f
            n?.invoke()
        }
        game.debugFrame(this)
    }

    /** Background scaled to cover the screen with its top slightly above the screen edge. */
    fun drawBackground(b: Batch) {
        val s = bgScale()
        val w = 1024f * s
        val h = 1536f * s
        b.color = Color.WHITE
        b.draw(Assets.texture(background), W / 2 - w / 2, H + BG_TOP_OFFSET - h, w, h)
    }

    fun bgScale() = max(W / 1024f, (H + BG_TOP_OFFSET) / 1536f)

    /** Converts a point in background image pixels (y down) to world space. */
    fun bgToWorld(ix: Float, iy: Float, out: Vector2): Vector2 {
        val s = bgScale()
        return out.set(W / 2 + (ix - 512f) * s, H + BG_TOP_OFFSET - iy * s)
    }

    companion object {
        const val BG_TOP_OFFSET = 30f
    }
}

class Button(
    var region: TextureRegion,
    var width: Float,
    var label: String = "",
    var labelSize: Float = 40f,
    val onClick: () -> Unit,
) {
    var x = 0f
    var y = 0f
    var enabled = true
    var visible = true
    var pressed = false
    val pop = Spring(400f, 15f, 1f)
    var icon: TextureRegion? = null
    var iconSize = 0f
    var pulse = false
    var appear = 1f
    var alpha = 1f
    private var t = 0f
    val height get() = width * region.regionHeight / region.regionWidth

    fun contains(px: Float, py: Float) = px in x - width / 2..x + width / 2 && py in y - height / 2..y + height / 2

    fun update(dt: Float) {
        t += dt
        pop.target = if (pressed) 0.9f else 1f
        pop.update(dt)
    }

    fun draw(b: Batch) {
        if (!visible || appear <= 0f) return
        var s = pop.value * Ease.outBack(appear)
        if (pulse) s *= 1f + sin(t * 5f) * 0.035f
        // soft drop shadow grounds the button on the panel
        b.setColor(0f, 0f, 0f, 0.22f * alpha)
        b.drawCentered(region, x, y - 6f * s, width * s * 0.98f)
        b.setColor(1f, 1f, 1f, alpha * if (enabled) 1f else 0.5f)
        b.drawCentered(region, x, y, width * s)
        val faceY = y + height * 0.06f * s   // the face sits above the darker bottom lip
        icon?.let {
            val ix = if (label.isNotEmpty()) x - width * 0.30f * s else x
            b.drawCentered(it, ix, faceY, iconSize * s)
        }
        if (label.isNotEmpty()) {
            val lx = if (icon != null) x + width * 0.08f * s else x
            b.text(Assets.fontBig, label, lx, faceY, labelSize * s, alpha = alpha)
        }
        b.color = Color.WHITE
    }
}
