package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object Ease {
    fun outBack(t: Float, s: Float = 1.70158f): Float {
        val x = t - 1
        return x * x * ((s + 1) * x + s) + 1
    }

    fun outElastic(t: Float): Float {
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        return (2.0.pow(-10.0 * t) * sin((t * 10 - 0.75) * (2 * PI / 3)) + 1).toFloat()
    }

    fun outCubic(t: Float): Float {
        val x = 1 - t
        return 1 - x * x * x
    }

    fun inCubic(t: Float) = t * t * t
    fun inOutSine(t: Float) = (-(cos(PI * t) - 1) / 2).toFloat()
    fun outQuad(t: Float) = 1 - (1 - t) * (1 - t)
    fun inQuad(t: Float) = t * t
}

/** Critically-damped-ish spring used for squash/stretch, recoil and wobble. */
class Spring(var stiffness: Float = 220f, var damping: Float = 12f, var value: Float = 0f) {
    var target = value
    var velocity = 0f

    fun update(dt: Float): Float {
        val force = (target - value) * stiffness - velocity * damping
        velocity += force * dt
        value += velocity * dt
        return value
    }

    fun kick(v: Float) {
        velocity += v
    }
}

fun clamp01(v: Float) = MathUtils.clamp(v, 0f, 1f)

fun Batch.drawCentered(
    region: TextureRegion, x: Float, y: Float, width: Float, height: Float = width * region.regionHeight / region.regionWidth,
    rotation: Float = 0f, scaleX: Float = 1f, scaleY: Float = 1f,
) {
    draw(region, x - width / 2, y - height / 2, width / 2, height / 2, width, height, scaleX, scaleY, rotation)
}

/** Draws sprite whose size is given by its width; anchor (ax, ay) in 0..1 of the sprite. */
fun Batch.drawAnchored(
    region: TextureRegion, x: Float, y: Float, width: Float, ax: Float, ay: Float,
    rotation: Float = 0f, scaleX: Float = 1f, scaleY: Float = 1f,
) {
    val h = width * region.regionHeight / region.regionWidth
    draw(region, x - width * ax, y - h * ay, width * ax, h * ay, width, h, scaleX, scaleY, rotation)
}

private val layout = GlyphLayout()

/** Draws text centred on (x, y) at the given cap-height-ish size. */
fun Batch.text(
    font: BitmapFont, str: String, x: Float, y: Float, size: Float, color: Color = Color.WHITE,
    alignX: Float = 0.5f, alpha: Float = 1f,
) {
    if (size < 0.5f) return
    val base = if (font === Assets.fontBig) 84f else 46f
    val s = size / base
    font.data.setScale(s)
    font.color = color
    font.color.a = color.a * alpha
    layout.setText(font, str)
    font.draw(this, layout, x - layout.width * alignX, y + layout.height / 2)
    font.data.setScale(1f)
    font.color = Color.WHITE
}

fun textWidth(font: BitmapFont, str: String, size: Float): Float {
    val base = if (font === Assets.fontBig) 84f else 46f
    font.data.setScale(size / base)
    layout.setText(font, str)
    font.data.setScale(1f)
    return layout.width
}

fun Batch.fillRect(x: Float, y: Float, w: Float, h: Float, color: Color) {
    val c = this.color.cpy()
    this.color = color
    draw(Assets.white, x, y, w, h)
    this.color = c
}
