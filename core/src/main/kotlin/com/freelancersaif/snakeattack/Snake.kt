package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** The rigged dragon head (faces left in its source art). */
class DragonHead : Rig() {
    val root = bone(null, 0f, 0f)
    private val neck = bone(Assets.r("dragon_neck_piece"), 40f, 52f, root, 6f, 4f)
    private val head = bone(Assets.r("dragon_upper_head"), 226f, 118f, root, 0f, 0f)
    private val mouth = bone(Assets.r("circle"), 48f, 48f, head, 132f, 160f)
    private val jaw = bone(Assets.r("dragon_lower_jaw"), 178f, 22f, head, 222f, 156f)
    private val tongue = bone(Assets.r("dragon_tongue"), 196f, 22f, jaw, 128f, 34f)
    private val frill = bone(Assets.r("dragon_frill"), 22f, 112f, head, 236f, 92f)
    private val hornBack = bone(Assets.r("dragon_horn_back"), 24f, 128f, head, 214f, 40f)
    private val hornFront = bone(Assets.r("dragon_horn_front"), 26f, 150f, head, 186f, 44f)
    private val eye = bone(Assets.r("dragon_eye_open"), 60f, 47f, head, 150f, 72f)
    private val eyeAngry = bone(Assets.r("dragon_eye_angry"), 98f, 70f, head, 150f, 70f)
    private val eyeLid = bone(Assets.r("dragon_eyelid_closed"), 100f, 60f, head, 150f, 70f)
    private val stars = bone(Assets.r("dragon_hurt_stars"), 107f, 110f, head, 150f, 20f)

    private var time = MathUtils.random(5f)
    private var blink = 2f
    private val jawSpring = Spring(260f, 14f)
    private val flinch = Spring(200f, 10f)
    var mouthOpen = 0.25f
    var angry = false
    var hurt = false
    var chomp = 0f

    init {
        mouth.tint.set(0.36f, 0.05f, 0.09f, 1f)
        tongue.regionScale = 0.62f
        order(frill, hornBack, neck, mouth, tongue, jaw, head, eye, eyeAngry, eyeLid, hornFront, stars)
    }

    fun hit() {
        flinch.kick(-260f)
        jawSpring.kick(80f)
        flash = 1f
    }

    fun update(dt: Float) {
        time += dt
        flash = max(0f, flash - dt * 5f)
        val chompWave = if (chomp > 0f) abs(sin(time * 14f)) else 0f
        // positive rotation swings the (left-pointing) jaw tip down, i.e. opens the mouth
        jawSpring.target = (MathUtils.clamp(mouthOpen + chompWave * chomp * 0.7f, 0f, 1f) + sin(time * 3f) * 0.04f) * 30f
        jawSpring.update(dt)
        jaw.rot = MathUtils.clamp(jawSpring.value, 0f, 32f)
        val open = jaw.rot / 32f
        // tongue flicks in and out of the open mouth
        tongue.rot = -jaw.rot * 0.2f + sin(time * 7f) * 5f
        tongue.dx = -6f - open * 14f + sin(time * 9f) * 4f
        // dark mouth interior: a flat ellipse that opens with the jaw
        mouth.sx = 1.3f
        mouth.sy = 0.1f + open * 0.62f
        mouth.dy = open * 18f
        mouth.visible = open > 0.06f
        flinch.update(dt)
        head.rot = flinch.value * 0.04f + sin(time * 2.2f) * 2.5f
        head.dx = flinch.value * 0.06f
        neck.rot = sin(time * 2.2f + 0.5f) * 3f
        hornFront.rot = sin(time * 2.6f) * 3f
        hornBack.rot = sin(time * 2.6f + 0.6f) * 3f
        frill.rot = sin(time * 3.1f) * 6f
        blink -= dt
        val closed = hurt || (blink < 0f && blink > -0.14f)
        if (blink <= -0.14f) blink = MathUtils.random(1.8f, 4f)
        eye.visible = !closed && !angry
        eyeAngry.visible = !closed && angry
        eyeLid.visible = closed
        stars.visible = hurt
        stars.rot = time * 120f
    }

    /** Mouth tip in world space (for bite effects). */
    fun mouthPoint(out: Vector2) = head.worldPoint(30f, 150f, out)
}

class Segment(val color: GemColor) {
    var gap = 0f          // current distance to the segment in front
    var s = 0f            // distance along the path
    var claimed = false   // a projectile is on its way
    var dead = false
    val pos = Vector2()
    var angle = 0f
    var flash = 0f
    var scaleKick = Spring(240f, 11f, 1f)
    var wobble = MathUtils.random(10f)
}

/** The whole snake: rigged head, coloured body segments and a 3-piece tail. */
class Snake(var path: Path, colors: List<GemColor>) {
    val head = DragonHead()
    val segments = ArrayList<Segment>()
    var headS = 0f
    var speed = 20f
    var boost = 0f
    var knock = Spring(60f, 9f)
    private var time = 0f
    var dying = 0f
    var dead = false
    var alpha = 1f

    val segSize = 56f
    private val spacing = 40f
    private val headGap = 60f
    private val tailParts = listOf(
        Triple("tail_piece_1", 10f, 70f),
        Triple("tail_piece_2", 10f, 50f),
        Triple("tail_piece_3", 10f, 80f),
    )
    private val tailScale = 0.36f
    private val tmp = Vector2()
    private val tailS = FloatArray(3)
    private val tailRegions = tailParts.map { Assets.r(it.first) }
    private val shadowRegion = Assets.r("segment_shadow")
    private val shineRegion = Assets.r("segment_shine_overlay")

    init {
        colors.forEachIndexed { i, c ->
            segments += Segment(c).apply { gap = if (i == 0) headGap else spacing }
        }
        head.scale = 0.46f
        layout()
    }

    /** Distance from the head to the end of the road. */
    val remaining get() = path.length - headS

    fun update(dt: Float, moving: Boolean) {
        time += dt
        if (moving) headS += (speed + boost) * dt
        knock.update(dt)
        segments.forEach { seg ->
            val target = if (seg === segments.first()) headGap else spacing
            seg.gap += (target - seg.gap) * min(1f, dt * 7f)
            seg.flash = max(0f, seg.flash - dt * 4f)
            seg.scaleKick.update(dt)
        }
        layout()
        head.update(dt)
    }

    /** Pushes the snake back along the road (e.g. after a hit). */
    fun pushBack(amount: Float) {
        headS -= amount
        knock.kick(amount * 6f)
    }

    private fun layout() {
        var s = headS
        val wiggleAmp = 5f
        for (i in segments.indices) {
            val seg = segments[i]
            s -= seg.gap
            seg.s = s
            path.pos(s, seg.pos)
            seg.angle = path.backAngle(s)
            // slithering: offset perpendicular to the path
            val w = sin(time * 5f - i * 0.7f) * wiggleAmp
            seg.pos.add(-MathUtils.sinDeg(seg.angle) * w, MathUtils.cosDeg(seg.angle) * w)
        }
        path.pos(headS, tmp)
        head.x = tmp.x
        head.y = tmp.y
        // the head art faces left, i.e. its +x axis points back towards the body
        head.rotation = path.backAngle(headS)
    }

    val lastS get() = segments.lastOrNull()?.s ?: (headS - headGap)

    /** World position of the tail base (for effects). */
    fun tailPos(out: Vector2) = path.pos(lastS - 60f, out)

    fun draw(batch: Batch, viewRight: Float) {
        if (dead) return
        val old = batch.packedColor
        batch.setColor(1f, 1f, 1f, alpha)
        // tail (from tip to base)
        var s = lastS - 30f
        for (i in 0..2) {
            tailS[i] = s
            s -= 56f
        }
        for (i in 2 downTo 0) {
            val px = tailParts[i].second
            val py = tailParts[i].third
            val reg = tailRegions[i]
            path.pos(tailS[i], tmp)
            if (tmp.x > viewRight + 200f) continue
            val w = sin(time * 5f - (segments.size + i) * 0.7f) * 6f
            val ang = path.backAngle(tailS[i]) + sin(time * 4f - i) * 8f * (i + 1) / 3f
            tmp.add(-MathUtils.sinDeg(ang) * w, MathUtils.cosDeg(ang) * w)
            val ww = reg.regionWidth * tailScale
            val hh = reg.regionHeight * tailScale
            batch.draw(reg, tmp.x - px * tailScale, tmp.y - (reg.regionHeight - py) * tailScale,
                px * tailScale, (reg.regionHeight - py) * tailScale, ww, hh, 1f, 1f, ang)
        }
        // shadows
        val shadow = shadowRegion
        batch.setColor(1f, 1f, 1f, 0.55f * alpha)
        for (seg in segments) {
            if (seg.pos.x > viewRight + segSize) continue
            batch.drawCentered(shadow, seg.pos.x + 4f, seg.pos.y - segSize * 0.36f, segSize * 1.2f)
        }
        // body segments, back to front so the front overlaps
        for (i in segments.indices.reversed()) {
            val seg = segments[i]
            if (seg.pos.x > viewRight + segSize) continue
            val breathe = 1f + sin(time * 6f - i * 0.55f) * 0.035f
            val sz = segSize * breathe * seg.scaleKick.value
            batch.setColor(1f, 1f, 1f, alpha)
            batch.drawCentered(Assets.segment(seg.color), seg.pos.x, seg.pos.y, sz, rotation = seg.angle + 180f)
            batch.setColor(1f, 1f, 1f, 0.35f * alpha)
            batch.drawCentered(shineRegion, seg.pos.x - 4f, seg.pos.y + 8f, sz * 0.8f)
            if (seg.flash > 0f) {
                batch.setColor(1f, 1f, 1f, seg.flash * alpha)
                batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE)
                batch.drawCentered(Assets.segment(seg.color), seg.pos.x, seg.pos.y, sz, rotation = seg.angle + 180f)
                batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA)
            }
        }
        batch.packedColor = old
        head.alpha = alpha
        if (!dead) head.draw(batch)
    }

    fun headPos(out: Vector2) = path.pos(headS, out)

    fun colorsLeft(): Set<GemColor> = segments.map { it.color }.toSet()
}
