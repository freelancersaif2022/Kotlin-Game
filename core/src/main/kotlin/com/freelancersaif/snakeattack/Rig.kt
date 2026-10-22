package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Affine2

/**
 * A cut-out animation bone. Every coordinate here is in the *image pixels* of
 * the sprite (origin top-left, y down), which is how the joints were measured:
 *  - [pivotX]/[pivotY]: the joint point inside this bone's own sprite.
 *  - [attachX]/[attachY]: where that joint sits inside the parent's sprite.
 * Animation code only touches [rot], [dx]/[dy], [sx]/[sy], [region] and [visible].
 */
class Bone(
    var region: TextureRegion?,
    val pivotX: Float,
    val pivotY: Float,
    val parent: Bone? = null,
    val attachX: Float = 0f,
    val attachY: Float = 0f,
) {
    var rot = 0f
    var dx = 0f
    var dy = 0f
    var sx = 1f
    var sy = 1f
    var visible = true
    val tint = Color(Color.WHITE)

    /** Optional drawing scale for swapped regions of a different resolution. */
    var regionScale = 1f

    /** Constant proportion fix applied to this bone and everything attached to it. */
    var baseScale = 1f

    val world = Affine2()
    private val tmp = Affine2()

    fun computeWorld(root: Affine2) {
        val p = parent
        if (p == null) {
            world.set(root)
        } else {
            world.set(p.world)
            world.translate(attachX - p.pivotX, -(attachY - p.pivotY))
        }
        world.translate(dx, -dy)
        world.rotate(rot)
        world.scale(sx * baseScale, sy * baseScale)
    }

    fun draw(batch: Batch, alpha: Float, extraTint: Color?) {
        val reg = region ?: return
        if (!visible) return
        val w = reg.regionWidth * regionScale
        val h = reg.regionHeight * regionScale
        tmp.set(world)
        tmp.translate(-pivotX * regionScale, -(h - pivotY * regionScale))
        val c = batch.color
        val r = tint.r * (extraTint?.r ?: 1f)
        val g = tint.g * (extraTint?.g ?: 1f)
        val b = tint.b * (extraTint?.b ?: 1f)
        batch.setColor(r, g, b, tint.a * alpha)
        batch.draw(reg, w, h, tmp)
        batch.color = c
    }

    /** World position of a point given in this bone's image pixels. */
    fun worldPoint(px: Float, py: Float, out: com.badlogic.gdx.math.Vector2): com.badlogic.gdx.math.Vector2 {
        out.set(px - pivotX, -(py - pivotY))
        world.applyTo(out)
        return out
    }
}

/** A set of bones with a draw order. The root transform positions the whole character. */
open class Rig {
    val bones = ArrayList<Bone>()
    val drawOrder = ArrayList<Bone>()
    val rootTransform = Affine2()
    var x = 0f
    var y = 0f
    var scale = 1f
    var rotation = 0f
    var flipX = false
    var alpha = 1f
    var flash = 0f
    private val flashColor = Color()

    /** Multiplied into every bone (e.g. icy blue while frozen). */
    val tintColor = Color(Color.WHITE)

    fun bone(region: TextureRegion?, pivotX: Float, pivotY: Float, parent: Bone? = null, attachX: Float = 0f, attachY: Float = 0f): Bone {
        val b = Bone(region, pivotX, pivotY, parent, attachX, attachY)
        bones += b
        return b
    }

    fun order(vararg b: Bone) {
        drawOrder.clear()
        drawOrder.addAll(b)
    }

    fun updateTransforms() {
        rootTransform.setToTranslation(x, y)
        rootTransform.rotate(rotation)
        rootTransform.scale(if (flipX) -scale else scale, scale)
        bones.forEach { it.computeWorld(rootTransform) }
    }

    fun draw(batch: Batch) {
        updateTransforms()
        val plain = tintColor.r == 1f && tintColor.g == 1f && tintColor.b == 1f
        val tint = if (flash > 0f) {
            val f = 1f + flash * 1.6f
            flashColor.set(f * tintColor.r, f * tintColor.g, f * tintColor.b, 1f)
        } else if (!plain) {
            flashColor.set(tintColor)
        } else {
            null
        }
        drawOrder.forEach { it.draw(batch, alpha, tint) }
    }
}
