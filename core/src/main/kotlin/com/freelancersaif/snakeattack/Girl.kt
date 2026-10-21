package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** The girl the player protects: a 12-bone cut-out rig with procedural animation. */
class Girl : Rig() {
    enum class Mood { IDLE, WORRIED, PANIC, CHEER, CAUGHT, WAVE }

    var mood = Mood.IDLE
        set(v) {
            if (field != v) {
                field = v
                moodTime = 0f
            }
        }

    private val hips = bone(null, 0f, 0f)
    private val torso = bone(Assets.r("girl_torso"), 80f, 126f, hips, 0f, 0f)
    private val skirt = bone(Assets.r("girl_skirt"), 92f, 10f, hips, 0f, -8f)
    private val thighL = bone(Assets.r("girl_upperleg_right"), 32f, 14f, hips, -22f, 26f)
    private val thighR = bone(Assets.r("girl_upperleg_left"), 32f, 14f, hips, 22f, 26f)
    private val shinL = bone(Assets.r("girl_lowerleg_right"), 54f, 12f, thighL, 22f, 130f)
    private val shinR = bone(Assets.r("girl_lowerleg_left"), 20f, 12f, thighR, 38f, 132f)
    private val armL = bone(Assets.r("girl_upperarm_left"), 32f, 18f, torso, 22f, 30f)
    private val armR = bone(Assets.r("girl_upperarm_right"), 26f, 18f, torso, 134f, 30f)
    private val foreL = bone(Assets.r("girl_lowerarm_left"), 16f, 10f, armL, 18f, 120f)
    private val foreR = bone(Assets.r("girl_lowerarm_right"), 38f, 10f, armR, 38f, 118f)
    private val neck = bone(null, 0f, 0f, torso, 78f, 8f)
    private val faces = mapOf(
        "happy" to faceBone("face_happy"),
        "blink" to faceBone("face_blink"),
        "scared" to faceBone("face_scared"),
        "crying" to faceBone("face_crying"),
        "cheering" to faceBone("face_cheering"),
        "surprised" to faceBone("face_surprised"),
    )
    private val ponytail = bone(Assets.r("girl_ponytail"), 44f, 22f, faces.getValue("happy"), 140f, 36f)

    private var time = MathUtils.random(10f)
    private var moodTime = 0f
    private var blinkTimer = 2f
    private val tailSpring = Spring(90f, 6f)
    private var jumpY = 0f
    var baseX = 0f
    var baseY = 0f

    init {
        // lower limbs first so the upper limb's rounded end hides the joint cap;
        // arms go over the head so raised hands stay visible
        order(
            ponytail, shinL, shinR, thighL, thighR, torso, skirt,
            *faces.values.toTypedArray(), foreL, armL, foreR, armR,
        )
        showFace("happy")
        // the generated arm art is oversized for the body: shorter arms, smaller hands
        armL.baseScale = 0.86f
        armR.baseScale = 0.86f
        foreL.baseScale = 0.8f
        foreR.baseScale = 0.8f
    }

    private fun faceBone(name: String) = bone(Assets.r(name), 84f, 150f, neck, 0f, 0f)

    private fun showFace(name: String) {
        faces.forEach { (k, b) -> b.visible = k == name }
    }

    fun update(dt: Float) {
        time += dt
        moodTime += dt
        val t = time

        // reset pose
        bones.forEach { it.rot = 0f; it.dx = 0f; it.dy = 0f; it.sx = 1f; it.sy = 1f }
        jumpY = 0f
        var face = "happy"

        when (mood) {
            Mood.IDLE, Mood.WAVE -> {
                val br = sin(t * 2.4f)
                torso.sy = 1f + br * 0.015f
                neck.rot = sin(t * 1.3f) * 3f
                armL.rot = -4f + sin(t * 1.6f) * 3f
                armR.rot = 4f - sin(t * 1.6f + 0.4f) * 3f
                foreL.rot = 6f
                foreR.rot = -6f
                hips.rot = sin(t * 1.2f) * 1.5f
                if (mood == Mood.WAVE) {
                    armR.rot = 150f + sin(t * 9f) * 14f
                    foreR.rot = -20f + sin(t * 9f + 1f) * 25f
                    face = "cheering"
                }
                blinkTimer -= dt
                if (blinkTimer < 0f) {
                    if (blinkTimer > -0.13f) face = "blink" else blinkTimer = MathUtils.random(2f, 4.5f)
                }
            }
            Mood.WORRIED -> {
                // hands pulled up to the face, knees trembling
                face = "scared"
                hips.dx = sin(t * 40f) * 1.2f
                armL.rot = -22f + sin(t * 7f) * 4f
                armR.rot = 22f - sin(t * 7f + 1f) * 4f
                foreL.rot = 150f + sin(t * 9f) * 6f
                foreR.rot = -150f - sin(t * 9f + 1f) * 6f
                neck.rot = sin(t * 3f) * 5f
                thighL.rot = 3f
                thighR.rot = -3f
                shinL.rot = -4f
                shinR.rot = 4f
                torso.rot = sin(t * 2f) * 2f
            }
            Mood.PANIC, Mood.CAUGHT -> {
                // running on the spot seen from the front: knees lift alternately
                // (the thigh foreshortens), arms flail above the head
                face = "crying"
                val run = t * 15f
                val liftL = max(0f, sin(run))
                val liftR = max(0f, -sin(run))
                thighL.sy = 1f - liftL * 0.42f
                thighR.sy = 1f - liftR * 0.42f
                thighL.rot = -liftL * 6f
                thighR.rot = liftR * 6f
                shinL.rot = liftL * 14f
                shinR.rot = -liftR * 14f
                jumpY = abs(sin(run)) * 5f
                armL.rot = -145f + sin(t * 13f) * 20f
                armR.rot = 145f + sin(t * 13f + 2f) * 20f
                foreL.rot = -20f + sin(t * 13f + 1f) * 30f
                foreR.rot = 20f - sin(t * 13f + 3f) * 30f
                neck.rot = sin(t * 9f) * 6f
                torso.rot = sin(run) * 3f
                hips.rot = sin(run) * 2f
                if (mood == Mood.CAUGHT) {
                    face = if (moodTime < 0.3f) "surprised" else "crying"
                    hips.dx = sin(t * 50f) * 4f
                }
            }
            Mood.CHEER -> {
                face = "cheering"
                val j = abs(sin(t * 7f))
                jumpY = j * 34f
                val squash = 1f - (1f - j) * 0.08f
                hips.sy = squash
                armL.rot = -150f + sin(t * 7f) * 12f
                armR.rot = 150f - sin(t * 7f) * 12f
                foreL.rot = -15f + sin(t * 14f) * 22f
                foreR.rot = 15f - sin(t * 14f) * 22f
                // legs tuck while airborne
                thighL.sy = 1f - j * 0.2f
                thighR.sy = 1f - j * 0.2f
                thighL.rot = -6f * j
                thighR.rot = 6f * j
                shinL.rot = 12f * j
                shinR.rot = -12f * j
                neck.rot = sin(t * 7f) * 8f
            }
        }
        showFace(face)

        // ponytail physics: follows body motion with a lagging spring
        tailSpring.target = -hips.dx * 1.5f - jumpY * 0.6f + sin(t * 2f) * 4f
        tailSpring.update(dt)
        ponytail.rot = tailSpring.value + neck.rot * 0.5f
        skirt.sx = 1f + abs(thighL.rot - thighR.rot) * 0.0012f
        skirt.rot = -hips.rot * 0.5f

        x = baseX
        y = baseY + jumpY * scale * 3.2f
    }

    /** Head position in world, useful for speech bubbles. */
    fun headTop(out: com.badlogic.gdx.math.Vector2) = faces.getValue("happy").worldPoint(84f, 0f, out)

    override fun toString() = "Girl($mood)"

    fun render(batch: Batch) = draw(batch)
}
