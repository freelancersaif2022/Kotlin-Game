package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * The girl (Lily) the player protects: a cut-out rig built from the v2 parts.
 * Arms and legs are single seamless pieces; poses swap in the raised-arm and
 * running-leg sprites, and the dress / hat come from the equipped outfit.
 * All coordinates are in the parts' atlas pixels (image space, y down).
 */
class Girl(outfit: String = Save.outfit, hat: String = Save.hat) : Rig() {
    enum class Mood { IDLE, WORRIED, PANIC, CHEER, CAUGHT, WAVE, LOVE }

    var mood = Mood.IDLE
        set(v) {
            if (field != v) {
                field = v
                moodTime = 0f
            }
        }

    private val hips = bone(null, 0f, 0f)
    // the dress: pivot at the waist so the whole upper body can lean
    private val body = bone(Assets.r(outfit), 76f, 150f, hips, 0f, 0f)
    private val legL = bone(Assets.r("g_leg_left"), 15f, 6f, hips, -20f, -2f)
    private val legR = bone(Assets.r("g_leg_right"), 25f, 6f, hips, 20f, -2f)
    private val legRunL = bone(Assets.r("g_leg_left_run"), 31f, 6f, hips, -20f, -2f)
    private val legRunR = bone(Assets.r("g_leg_right_run"), 53f, 6f, hips, 20f, -2f)
    private val armL = bone(Assets.r("g_arm_left"), 18f, 6f, body, 33f, 26f)
    private val armR = bone(Assets.r("g_arm_right"), 13f, 6f, body, 119f, 26f)
    private val armUpL = bone(Assets.r("g_arm_left_up"), 90f, 100f, body, 36f, 30f)
    private val armUpR = bone(Assets.r("g_arm_right_up"), 12f, 100f, body, 116f, 30f)
    // neck + upper chest: fills the dress neckline (drawn behind the dress)
    private val neck = bone(Assets.r("g_neck"), 38f, 84f, body, 76f, 36f)
    private val faceNames = listOf(
        "happy", "blink", "scared", "crying", "cheering", "surprised", "angry", "love", "relieved", "shout", "sleepy", "wink",
    )
    private val faces = faceNames.associateWith { bone(Assets.r("g_face_$it"), 56f, 100f, neck, 38f, 22f) }
    private val head = faces.getValue("happy")
    private val ponytail = bone(Assets.r("g_ponytail"), 40f, 14f, head, 94f, 26f)
    private val hatBone = hat.takeIf { it.isNotEmpty() }?.let { name ->
        val r = Assets.r(name)
        bone(r, r.regionWidth / 2f, r.regionHeight * 0.78f, head, 56f, 26f)
    }

    private var time = MathUtils.random(10f)
    private var moodTime = 0f
    private var blinkTimer = 2f
    private val tailSpring = Spring(90f, 6f)
    private var jumpY = 0f
    var baseX = 0f
    var baseY = 0f

    init {
        neck.baseScale = 0.5f
        // the neck bone is drawn at half size; undo that for the head attached to it
        faces.values.forEach { it.baseScale = 2f }
        hatBone?.baseScale = 0.62f
        val order = arrayListOf(ponytail, legL, legR, legRunL, legRunR, armUpL, armUpR, neck, body)
        order += faces.values
        hatBone?.let { order += it }
        order += armL
        order += armR
        order(*order.toTypedArray())
        showFace("happy")
    }

    private fun showFace(name: String) {
        faces.forEach { (k, b) -> b.visible = k == name }
    }

    fun update(dt: Float) {
        time += dt
        moodTime += dt
        val t = time

        bones.forEach { it.rot = 0f; it.dx = 0f; it.dy = 0f; it.sx = 1f; it.sy = 1f }
        jumpY = 0f
        var face = "happy"
        var armsUp = false
        var running = false
        var waveOnly = false

        when (mood) {
            Mood.IDLE, Mood.WAVE, Mood.LOVE -> {
                body.sy = 1f + sin(t * 2.4f) * 0.012f
                legL.rot = -2f
                legR.rot = 2f
                neck.rot = sin(t * 1.3f) * 3f
                armL.rot = -4f + sin(t * 1.6f) * 3f
                armR.rot = 4f - sin(t * 1.6f + 0.4f) * 3f
                hips.rot = sin(t * 1.2f) * 1.2f
                if (mood == Mood.WAVE) {
                    face = "cheering"
                    waveOnly = true
                    armUpR.rot = sin(t * 8f) * 16f
                }
                if (mood == Mood.LOVE) face = "love"
                blinkTimer -= dt
                if (blinkTimer < 0f && mood == Mood.IDLE) {
                    if (blinkTimer > -0.13f) face = "blink" else blinkTimer = MathUtils.random(2f, 4.5f)
                }
            }
            Mood.WORRIED -> {
                face = if ((t * 0.5f).toInt() % 3 == 2) "surprised" else "scared"
                hips.dx = sin(t * 40f) * 1.2f
                armL.rot = 14f + sin(t * 9f) * 3f
                armR.rot = -14f - sin(t * 9f + 1f) * 3f
                neck.rot = sin(t * 3f) * 5f
                body.rot = sin(t * 2f) * 2f
                legL.rot = 3f
                legR.rot = -3f
            }
            Mood.PANIC, Mood.CAUGHT -> {
                face = if (mood == Mood.CAUGHT && moodTime < 0.3f) "shout" else "crying"
                armsUp = true
                running = true
                val run = t * 14f
                armUpL.rot = sin(t * 13f) * 18f
                armUpR.rot = -sin(t * 13f + 2f) * 18f
                jumpY = abs(sin(run)) * 5f
                neck.rot = sin(t * 9f) * 6f
                body.rot = sin(run) * 3f
                // alternate the bent running leg with the straight one
                val left = sin(run) > 0f
                legRunL.visible = left
                legL.visible = !left
                legRunR.visible = !left
                legR.visible = left
                if (mood == Mood.CAUGHT) hips.dx = sin(t * 50f) * 4f
            }
            Mood.CHEER -> {
                face = if ((t * 0.7f).toInt() % 2 == 0) "cheering" else "wink"
                armsUp = true
                val j = abs(sin(t * 7f))
                jumpY = j * 30f
                hips.sy = 1f - (1f - j) * 0.06f
                armUpL.rot = sin(t * 14f) * 12f
                armUpR.rot = -sin(t * 14f) * 12f
                legL.rot = -6f * j
                legR.rot = 6f * j
                neck.rot = sin(t * 7f) * 7f
            }
        }
        if (!running) {
            legRunL.visible = false
            legRunR.visible = false
            legL.visible = true
            legR.visible = true
        }
        armUpL.visible = armsUp
        armUpR.visible = armsUp || waveOnly
        armL.visible = !armsUp
        armR.visible = !armsUp && !waveOnly
        showFace(face)

        // ponytail physics: lags behind the body motion
        tailSpring.target = -hips.dx * 1.5f - jumpY * 0.5f + sin(t * 2f) * 4f
        tailSpring.update(dt)
        ponytail.rot = tailSpring.value + neck.rot * 0.5f
        body.sx = 1f + max(abs(legL.rot), abs(legR.rot)) * 0.002f

        x = baseX
        y = baseY + jumpY * scale * 2.2f
    }

    /** Top of the head in world space (for speech bubbles). */
    fun headTop(out: Vector2) = head.worldPoint(56f, 4f, out)

    fun render(batch: Batch) = draw(batch)
}
