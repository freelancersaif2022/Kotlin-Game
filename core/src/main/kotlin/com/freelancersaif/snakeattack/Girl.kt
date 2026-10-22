package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * The girl (Lily) the player protects: a cut-out rig.
 * Arms and legs are single slim pieces (v3 art); raised poses rotate the whole arm
 * at the shoulder, and the arms are drawn behind the dress so the puffy sleeves
 * always cover the shoulder joint. All coordinates are atlas pixels (y down).
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
    // the dress: pivot at the hips so the whole upper body can lean
    private val body = bone(Assets.r(outfit), 76f, 150f, hips, 0f, 0f)
    private val legL = bone(Assets.r("g_leg_left"), 14f, 4f, hips, -17f, -6f)
    private val legR = bone(Assets.r("g_leg_right"), 25f, 4f, hips, 17f, -6f)
    private val armL = bone(Assets.r("g_arm_left"), 29f, 5f, body, 40f, 24f)
    private val armR = bone(Assets.r("g_arm_right"), 18f, 5f, body, 112f, 24f)
    // neck + upper chest, drawn behind the dress so the neckline overlaps it
    private val neck = bone(Assets.r("g_neck"), 24f, 30f, body, 76f, 16f)
    private val faceNames = listOf(
        "happy", "blink", "scared", "crying", "cheering", "surprised", "angry", "love", "relieved", "shout", "sleepy", "wink",
    )
    private val faces = faceNames.associateWith { bone(Assets.r("g_face_$it"), 56f, 100f, neck, 24f, 10f) }
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
    private val armSpringL = Spring(160f, 14f)
    private val armSpringR = Spring(160f, 14f)
    private var jumpY = 0f
    var baseX = 0f
    var baseY = 0f

    init {
        hatBone?.baseScale = 0.62f
        val order = arrayListOf(ponytail, legL, legR, armL, armR, neck, body)
        order += faces.values
        hatBone?.let { order += it }
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
        // target arm angles (0 = hanging down, + = swings outward on the left arm / inward on the right)
        var aL: Float
        var aR: Float

        when (mood) {
            Mood.IDLE, Mood.WAVE, Mood.LOVE -> {
                body.sy = 1f + sin(t * 2.4f) * 0.012f
                neck.rot = sin(t * 1.3f) * 3f
                aL = -4f + sin(t * 1.6f) * 3f
                aR = 4f - sin(t * 1.6f + 0.4f) * 3f
                hips.rot = sin(t * 1.2f) * 1.2f
                legL.rot = -1.5f
                legR.rot = 1.5f
                if (mood == Mood.WAVE) {
                    face = "cheering"
                    aR = 140f + sin(t * 8f) * 16f
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
                // arms tense at her sides, trembling
                aL = -9f + sin(t * 11f) * 3f
                aR = 9f - sin(t * 11f + 1f) * 3f
                neck.rot = sin(t * 3f) * 5f
                body.rot = sin(t * 2f) * 2f
                legL.rot = 2f
                legR.rot = -2f
            }
            Mood.PANIC, Mood.CAUGHT -> {
                face = if (mood == Mood.CAUGHT && moodTime < 0.3f) "shout" else "crying"
                val run = t * 14f
                aL = -138f + sin(t * 13f) * 16f
                aR = 138f - sin(t * 13f + 2f) * 16f
                jumpY = abs(sin(run)) * 5f
                neck.rot = sin(t * 9f) * 6f
                body.rot = sin(run) * 2.5f
                // front-view running on the spot: each knee lifts towards the camera in turn,
                // which shortens that leg (the thigh foreshortens) and raises the foot
                val liftL = max(0f, sin(run))
                val liftR = max(0f, -sin(run))
                legL.sy = 1f - liftL * 0.3f
                legR.sy = 1f - liftR * 0.3f
                legL.rot = -liftL * 4f
                legR.rot = liftR * 4f
                if (mood == Mood.CAUGHT) hips.dx = sin(t * 50f) * 4f
            }
            Mood.CHEER -> {
                face = if ((t * 0.7f).toInt() % 2 == 0) "cheering" else "wink"
                val j = abs(sin(t * 7f))
                jumpY = j * 30f
                hips.sy = 1f - (1f - j) * 0.06f
                aL = -142f + sin(t * 14f) * 8f
                aR = 142f - sin(t * 14f) * 8f
                legL.rot = -5f * j
                legR.rot = 5f * j
                neck.rot = sin(t * 7f) * 7f
            }
        }
        // arms swing smoothly between poses instead of snapping
        armSpringL.target = aL
        armSpringR.target = aR
        armL.rot = armSpringL.update(dt)
        armR.rot = armSpringR.update(dt)
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
