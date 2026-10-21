package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * The girl the player protects: a cut-out rig with procedural animation.
 * Arms are the forearm sprites joined straight to the shoulder (their joint end
 * hides under the puffy sleeve) and legs are the lower-leg sprites joined at the
 * hip under the skirt, so no elbow / knee joints ever show.
 */
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
    private val legL = bone(Assets.r("girl_lowerleg_right"), 54f, 12f, hips, -24f, 58f)
    private val legR = bone(Assets.r("girl_lowerleg_left"), 20f, 12f, hips, 24f, 58f)
    private val armL = bone(Assets.r("girl_lowerarm_left"), 16f, 10f, torso, 24f, 36f)
    private val armR = bone(Assets.r("girl_lowerarm_right"), 38f, 10f, torso, 132f, 36f)
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
        // arms sit behind the torso so the sleeves cover the shoulder joint
        order(ponytail, legL, legR, armL, armR, torso, skirt, *faces.values.toTypedArray())
        showFace("happy")
        armL.baseScale = 0.92f
        armR.baseScale = 0.92f
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
                armL.rot = -12f + sin(t * 1.6f) * 3f
                armR.rot = 12f - sin(t * 1.6f + 0.4f) * 3f
                hips.rot = sin(t * 1.2f) * 1.5f
                if (mood == Mood.WAVE) {
                    armR.rot = 145f + sin(t * 9f) * 18f
                    face = "cheering"
                }
                blinkTimer -= dt
                if (blinkTimer < 0f) {
                    if (blinkTimer > -0.13f) face = "blink" else blinkTimer = MathUtils.random(2f, 4.5f)
                }
            }
            Mood.WORRIED -> {
                // hands clasped nervously in front, whole body trembling
                face = "scared"
                hips.dx = sin(t * 40f) * 1.2f
                armL.rot = 24f + sin(t * 9f) * 4f
                armR.rot = -24f - sin(t * 9f + 1f) * 4f
                neck.rot = sin(t * 3f) * 5f
                legL.rot = 2f
                legR.rot = -2f
                torso.rot = sin(t * 2f) * 2f
                hips.dy = 1.5f
            }
            Mood.PANIC, Mood.CAUGHT -> {
                // running on the spot seen from the front: knees lift alternately
                // (the thigh foreshortens), arms flail above the head
                face = "crying"
                val run = t * 15f
                val liftL = max(0f, sin(run))
                val liftR = max(0f, -sin(run))
                legL.sy = 1f - liftL * 0.3f
                legR.sy = 1f - liftR * 0.3f
                legL.rot = -liftL * 5f
                legR.rot = liftR * 5f
                jumpY = abs(sin(run)) * 5f
                armL.rot = -145f + sin(t * 13f) * 22f
                armR.rot = 145f + sin(t * 13f + 2f) * 22f
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
                armL.rot = -150f + sin(t * 14f) * 14f
                armR.rot = 150f - sin(t * 14f) * 14f
                // legs tuck and spread a little while airborne
                legL.sy = 1f - j * 0.12f
                legR.sy = 1f - j * 0.12f
                legL.rot = -7f * j
                legR.rot = 7f * j
                neck.rot = sin(t * 7f) * 8f
            }
        }
        showFace(face)

        // ponytail physics: follows body motion with a lagging spring
        tailSpring.target = -hips.dx * 1.5f - jumpY * 0.6f + sin(t * 2f) * 4f
        tailSpring.update(dt)
        ponytail.rot = tailSpring.value + neck.rot * 0.5f
        skirt.sx = 1f + abs(legL.rot - legR.rot) * 0.002f
        skirt.rot = -hips.rot * 0.5f

        x = baseX
        y = baseY + jumpY * scale * 3.2f
    }

    /** Head position in world, useful for speech bubbles. */
    fun headTop(out: com.badlogic.gdx.math.Vector2) = faces.getValue("happy").worldPoint(84f, 0f, out)

    override fun toString() = "Girl($mood)"

    fun render(batch: Batch) = draw(batch)
}
