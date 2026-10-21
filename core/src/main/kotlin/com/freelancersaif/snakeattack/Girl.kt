package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
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
        order(
            ponytail, thighL, thighR, shinL, shinR, torso, skirt,
            armL, foreL, armR, foreR, *faces.values.toTypedArray(),
        )
        showFace("happy")
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
                face = "scared"
                val sh = sin(t * 40f) * 1.2f
                hips.dx = sh
                armL.rot = -55f + sin(t * 6f) * 8f
                armR.rot = 55f - sin(t * 6f + 1f) * 8f
                foreL.rot = 100f
                foreR.rot = -100f
                neck.rot = sin(t * 3f) * 6f
                thighL.rot = -4f
                thighR.rot = 4f
                hips.dy = 3f
            }
            Mood.PANIC, Mood.CAUGHT -> {
                face = "crying"
                val run = t * 16f
                thighL.rot = sin(run) * 28f
                thighR.rot = -sin(run) * 28f
                shinL.rot = (sin(run + 1.3f) * 0.5f + 0.5f) * -38f
                shinR.rot = (sin(run + 1.3f + MathUtils.PI) * 0.5f + 0.5f) * 38f
                jumpY = abs(sin(run)) * 7f
                armL.rot = -150f + sin(t * 14f) * 22f
                armR.rot = 150f + sin(t * 14f + 2f) * 22f
                foreL.rot = sin(t * 14f) * 30f
                foreR.rot = -sin(t * 14f + 2f) * 30f
                neck.rot = sin(t * 10f) * 7f
                torso.rot = sin(run) * 4f
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
                armL.rot = -160f + sin(t * 10f) * 18f
                armR.rot = 160f - sin(t * 10f) * 18f
                foreL.rot = -25f + sin(t * 10f + 1f) * 25f
                foreR.rot = 25f - sin(t * 10f + 1f) * 25f
                thighL.rot = -10f * j
                thighR.rot = 10f * j
                shinL.rot = 30f * j
                shinR.rot = -30f * j
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
