package com.smokemaster.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LightingColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A cigarette brand that slides along the bar. */
class Brand(
    val name: String, val image: String, val puffs: Int, val heat: Float,
    val points: Int, val weight: Int, val cool: Float = 0f,
)

private val BRANDS = arrayOf(
    Brand("BLAZE", "cig_box_1", 8, 0.075f, 10, 30),
    Brand("ROYAL", "cig_box_2", 8, 0.07f, 15, 22),
    Brand("COOL ICE", "cig_box_3", 9, 0.055f, 8, 18, cool = 0.12f),
    Brand("MINT LEAF", "cig_box_4", 10, 0.05f, 7, 18),
    Brand("KING'S", "cig_box_5", 6, 0.11f, 30, 12),
)

private class Layer(val bmp: Bitmap, val x: Float, val y: Float)

/**
 * The bar: packs slide along the counter, the hand button grabs one, the smoke
 * button puffs. Every puff heats the hero up (face turns red, screen edges glow,
 * heart pounds) until the cigarette explodes.
 *
 * The hero is a puppet of layers that share a 1024x1024 canvas (see
 * layer_layout.json in the art pack): head behind body, arm in front, the arm
 * rotating around the right shoulder.
 */
class PlayScreen(g: Game) : Screen(g) {

    // ---- puppet anchors (canvas units)
    private companion object {
        const val SHOULDER_X = 340f
        const val SHOULDER_Y = 500f
        const val NECK_X = 512f
        const val NECK_Y = 490f
        const val MOUTH_X = 512f
        const val MOUTH_Y = 360f
        const val TIP_HOLD_X = 602f
        const val TIP_HOLD_Y = 723f
        const val TIP_MOUTH_X = 447f
        const val TIP_MOUTH_Y = 342f
        const val HAND_X = 545f
        const val HAND_Y = 765f

        // background art anchors (gameplay_bg.png pixels)
        const val COUNTER_TOP = 1294f
        const val BOX_BASE = 1410f
        const val CHAR_X = 0f
        const val CHAR_Y = 325f

        const val ZONE_HALF = 120f

        const val IDLE = 0
        const val PICK = 1
        const val LIGHT = 2
        const val HOLD = 3
        const val INHALE = 4
        const val EXHALE = 5
        const val THROW = 6

        const val PLAYING = 0
        const val EXPLODING = 1
        const val OVER = 2

        val STAGE_MULT = intArrayOf(1, 2, 3, 5)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val addPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val smokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val darkSmokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = LightingColorFilter(0x3A3A3A, 0x050505)
    }
    private val sweatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCFEFFF.toInt() }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(0f, 0f, 1f, intArrayOf(0xFFFFD27A.toInt(), 0xAAFF7A1A.toInt(), 0x00FF3A00), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val vignettePaint = Paint()
    private var vigLeft: LinearGradient? = null
    private var vigRight: LinearGradient? = null
    private var vigTop: LinearGradient? = null
    private var vigBottom: LinearGradient? = null
    private var vigW = -1f
    private var vigH = -1f

    private val smoke = Particles(260, smokePaint)
    private val darkSmoke = Particles(60, darkSmokePaint)
    private val embers = Particles(120, addPaint)
    private val sweat = Particles(40, sweatPaint)
    private val puffImgs = arrayOf(g.assets["smoke_puff_1"], g.assets["smoke_puff_2"], g.assets["smoke_puff_3"])
    private val emberImgs = arrayOf(g.assets["ember_spark"], g.assets["cigarette_lit"])
    private val floating = FloatingTexts(g.text)

    private val layers = HashMap<String, Layer>()
    private val tintFilters = Array<ColorFilter?>(24) { i ->
        if (i == 0) null else PorterDuffColorFilter(Color.argb(i * 6, 255, 25, 5), PorterDuff.Mode.SRC_ATOP)
    }
    private val bg = g.assets["gameplay_bg"]
    private val counterSrc = Rect(0, COUNTER_TOP.toInt(), bg.width, bg.height)
    private val boxBitmaps = Array(BRANDS.size) { g.assets[BRANDS[it].image] }

    // ---- buttons
    private val pauseBtn = Button(g.assets["btn_pause"], 112f, 110f) { g.click(); openPause() }
    private val pickBtn = Button(g.assets["btn_pick_hand"], 250f, 248f, fireOnDown = true) { pick() }
    private val smokeBtn = Button(g.assets["btn_smoke"], 250f, 246f, fireOnDown = true) { puff() }
    private val buttons = listOf(pauseBtn, pickBtn, smokeBtn)
    private var popup: Popup? = null

    // ---- layout (virtual units)
    private var bgS = 1f
    private var bgX = 0f
    private var bgY = 0f
    private var charX = 0f
    private var charY = 0f
    private var charS = 1f
    private var zoneX = 0f
    private var boxBaseY = 0f
    private var boxH = 0f
    private val dst = RectF()
    private val r = RectF()
    private val pt = FloatArray(2)

    // ---- game state
    private var phase = PLAYING
    private var t = 0f
    private var alive = 0f
    private var heat = 0f
    private var heatShown = 0f
    private var floor = 0f
    private var score = 0
    private var scoreShown = 0f
    private var scoreStr = "0"
    private var scoreStrVal = -1
    private val best = g.settings.best
    private val bestStr = "BEST  $best"
    private var combo = 0
    private var sinceLastPuff = 99f
    private var comboT = 0f
    private var cigarettes = 0
    private var puffsTotal = 0
    private var holding = false
    private var brand: Brand? = null
    private var puffsLeft = 0
    private var state = IDLE
    private var stateT = 0f
    private var inhaleLeft = 0f
    private var lastInhaleSound = 0f
    private var pickCooldown = 0f
    private var missT = 0f
    private var lastStage = 0
    private var heartTimer = 0f
    private var heartPulse = 0f
    private var shake = 0f
    private var shakeX = 0f
    private var shakeY = 0f
    private var flash = 0f
    private var haze = 0f
    private var explodeT = 0f
    private var explodeX = 0f
    private var explodeY = 0f
    private var sizzled = false

    // flying pack
    private var flyBrand = -1
    private var flyX = 0f
    private var flyY = 0f

    // packs on the counter
    private val boxX = FloatArray(8)
    private val boxType = IntArray(8)
    private val boxOn = BooleanArray(8)
    private var spawnTimer = 0.6f

    // emitters
    private var tipSmokeT = 0f
    private var exhaleSmokeT = 0f
    private var sweatT = 0f
    private var steamT = 0f
    private var headSmokeT = 0f

    // puppet animation
    private var bob = 0f
    private var armName = "char_arm_idle"
    private var armRot = 0f
    private var prevArm = "char_arm_idle"
    private var prevArmRot = 0f
    private var armFade = 0f
    private var headName = "head_normal"
    private var prevHead = "head_normal"
    private var headFade = 0f
    private var headRot = 0f
    private var headDy = 0f
    private var headJitter = 0f
    private var headScale = 1f

    // tutorial hint
    private var hint = "Tap the HAND when a pack is in the GOLD RING"
    private var hintT = 0f
    private var hintStep = 0

    init {
        for (name in listOf("char_body", "char_arm_idle", "char_arm_hold", "char_arm_mouth", "head_normal", "head_smoking",
            "head_exhale", "head_red_1", "head_red_2", "head_red_3", "head_burnt")) {
            val o = g.assets.layerOffset[name]!!
            layers[name] = Layer(g.assets[name], o.x, o.y)
        }
    }

    override fun enter() {
        g.audio.music("music_game", 0.42f)
    }

    private val stage: Int
        get() = when {
            heat < 0.3f -> 0
            heat < 0.55f -> 1
            heat < 0.8f -> 2
            else -> 3
        }

    // ------------------------------------------------------------------ layout

    private fun layout() {
        bgS = maxOf(g.vw / bg.width, g.vh / bg.height)
        bgX = (g.vw - bg.width * bgS) / 2
        bgY = (g.vh - bg.height * bgS) / 2
        charS = bgS
        charX = bgX + CHAR_X * bgS
        charY = bgY + CHAR_Y * bgS
        boxBaseY = bgY + BOX_BASE * bgS
        boxH = 215f * bgS
        zoneX = g.vw * 0.24f

        val by = g.vh - g.safeBottom - 190f
        pickBtn.at(g.vw * 0.165f, by)
        smokeBtn.at(g.vw * 0.835f, by)
        pauseBtn.at(g.vw - 30f - 56f, g.safeTop + 30f + 55f)

        if (vigW != g.vw || vigH != g.vh) {
            vigW = g.vw
            vigH = g.vh
            val red = intArrayOf(0xFFD80000.toInt(), 0x88B00000.toInt(), 0x00800000)
            val pos = floatArrayOf(0f, 0.4f, 1f)
            vigLeft = LinearGradient(0f, 0f, g.vw * 0.32f, 0f, red, pos, Shader.TileMode.CLAMP)
            vigRight = LinearGradient(g.vw, 0f, g.vw * 0.68f, 0f, red, pos, Shader.TileMode.CLAMP)
            vigTop = LinearGradient(0f, 0f, 0f, g.vh * 0.14f, red, pos, Shader.TileMode.CLAMP)
            vigBottom = LinearGradient(0f, g.vh, 0f, g.vh * 0.86f, red, pos, Shader.TileMode.CLAMP)
        }
    }

    // ------------------------------------------------------------------ actions

    private fun pick() {
        if (phase != PLAYING) return
        if (holding || state != IDLE) {
            if (holding) floating.add("FINISH YOUR SMOKE!", g.vw / 2, pickBtn.cy - 200f, 46f, 0xFFFFE9C0.toInt())
            return
        }
        if (pickCooldown > 0f) return
        var best = -1
        var bestD = Float.MAX_VALUE
        for (i in boxX.indices) {
            if (!boxOn[i]) continue
            val d = abs(boxX[i] - zoneX)
            if (d < ZONE_HALF && d < bestD) {
                best = i
                bestD = d
            }
        }
        if (best < 0) {
            g.audio.play("miss", 0.7f)
            floating.add("MISS!", zoneX, boxBaseY - boxH - 40f, 64f, 0xFFFF5A3A.toInt())
            pickCooldown = 0.45f
            missT = 0.35f
            g.haptics.buzz(30, 60)
            return
        }
        boxOn[best] = false
        flyBrand = boxType[best]
        flyX = boxX[best]
        flyY = boxBaseY - boxH / 2
        setState(PICK)
        g.audio.play("whoosh", 0.8f)
        g.audio.play("pickup")
        g.haptics.buzz(20, 90)
        val quality = 1f - bestD / ZONE_HALF
        val bonus: Int
        if (quality > 0.72f) {
            bonus = 50
            g.audio.play("perfect", 0.8f)
            floating.add("PERFECT!", zoneX + 40f, boxBaseY - boxH - 50f, 74f)
        } else {
            bonus = 20
            g.audio.play("ding", 0.6f)
            floating.add("GOOD", zoneX + 40f, boxBaseY - boxH - 50f, 64f, 0xFFFFF1C8.toInt())
        }
        addScore(bonus)
        if (hintStep == 0) {
            hintStep = 1
            hint = "Now tap the CIGARETTE button to smoke!"
            hintT = 0f
        }
    }

    private fun puff() {
        if (phase != PLAYING) return
        if (!holding) {
            if (state == IDLE) floating.add("GRAB A PACK FIRST!", g.vw / 2, smokeBtn.cy - 200f, 46f, 0xFFFFE9C0.toInt())
            return
        }
        if (puffsLeft <= 0 || (state != HOLD && state != INHALE && state != EXHALE)) return
        val b = brand ?: return

        puffsLeft--
        puffsTotal++
        heat += b.heat * (1f + alive / 120f)
        combo = if (sinceLastPuff < 0.9f) combo + 1 else 1
        sinceLastPuff = 0f
        val pts = b.points * STAGE_MULT[stage] + (combo - 1) * 2
        addScore(pts)
        headPoint(MOUTH_X, MOUTH_Y)
        floating.add("+$pts", pt[0] + rnd(-60f, 60f), pt[1] - 170f, 58f + min(combo, 10) * 2f)
        if (combo >= 3) comboT = 1.2f
        haze = min(0.75f, haze + 0.045f)

        if (state != INHALE) setState(INHALE)
        inhaleLeft = 0.3f
        if (t - lastInhaleSound > 0.12f) {
            lastInhaleSound = t
            g.audio.play("inhale", 0.8f, 0.95f + heat * 0.25f)
        }
        g.haptics.buzz(12, 70)

        if (hintStep == 1) {
            hintStep = 2
            hint = "Tap fast for big points... but don't OVERHEAT!"
            hintT = 0f
        }
        if (heat >= 1f) explode()
    }

    private fun addScore(v: Int) {
        score += v
    }

    private fun setState(s: Int) {
        state = s
        stateT = 0f
    }

    private fun openPause() {
        if (popup != null || phase != PLAYING) return
        g.audio.fadeMusicTo(0.15f)
        popup = PausePopup(g, onResume = { g.audio.fadeMusicTo(0.42f) }, onHome = { g.go(LobbyScreen(g)) })
    }

    private fun explode() {
        phase = EXPLODING
        explodeT = 0f
        if (state == INHALE) armPoint(TIP_MOUTH_X, TIP_MOUTH_Y) else headPoint(MOUTH_X, MOUTH_Y)
        explodeX = pt[0]
        explodeY = pt[1]
        holding = false
        setState(IDLE)
        heat = 1f
        g.audio.play("explosion")
        g.audio.fadeMusicTo(0f)
        g.haptics.buzz(500, 255)
        shake = 1f
        flash = 1f
        for (i in 0 until 60) {
            val a = rnd(0f, (2 * PI).toFloat())
            val v = rnd(350f, 1300f)
            embers.spawn(0, explodeX, explodeY, cos(a) * v, sin(a) * v - 300f, rnd(0.7f, 1.4f),
                rnd(18f, 46f), rnd(6f, 16f), 1f, spin = rnd(-400f, 400f), pdrag = 1.2f, gravity = 1500f)
        }
        for (i in 0 until 18) {
            val a = rnd(0f, (2 * PI).toFloat())
            val v = rnd(120f, 520f)
            darkSmoke.spawn(RANDOM.nextInt(3), explodeX, explodeY, cos(a) * v, sin(a) * v - 80f, rnd(1.6f, 2.6f),
                rnd(120f, 200f), rnd(420f, 720f), rnd(0.65f, 0.9f), spin = rnd(-40f, 40f), pdrag = 2.2f, gravity = -60f)
        }
        for (i in 0 until 10) {
            val a = rnd(0f, (2 * PI).toFloat())
            val v = rnd(100f, 380f)
            smoke.spawn(RANDOM.nextInt(3), explodeX, explodeY, cos(a) * v, sin(a) * v, rnd(1.4f, 2.2f),
                rnd(100f, 160f), rnd(380f, 560f), 0.7f, spin = rnd(-40f, 40f), pdrag = 2f, gravity = -50f)
        }
    }

    private fun gameOver() {
        phase = OVER
        val newBest = g.settings.submitScore(score)
        g.audio.play("gameover", 0.9f)
        popup = GameOverPopup(
            g, score, maxOf(best, score), newBest, cigarettes, puffsTotal,
            onAgain = { g.go(PlayScreen(g)) },
            onHome = { g.go(LobbyScreen(g)) },
        )
    }

    // ------------------------------------------------------------------ update

    override fun update(dt: Float) {
        layout()
        buttons.forEach { it.update(dt) }
        popup?.let {
            it.update(dt)
            if (it.done && phase == PLAYING) popup = null
        }
        if (popup != null && phase == PLAYING) return // paused

        t += dt
        floating.update(dt)
        smoke.update(dt)
        darkSmoke.update(dt)
        embers.update(dt)
        sweat.update(dt)
        flash = maxOf(0f, flash - dt * 2.2f)
        shake = maxOf(0f, shake - dt * 1.6f)
        haze = maxOf(0f, haze - dt * 0.035f)
        bob = sin(t * 2.3f) * 3f

        when (phase) {
            PLAYING -> updatePlaying(dt)
            EXPLODING -> {
                explodeT += dt
                if (!sizzled && explodeT > 0.45f) {
                    sizzled = true
                    g.audio.play("sizzle", 0.8f)
                    g.audio.play("cough", 0.9f, 0.85f)
                }
                if (explodeT > 1.8f) gameOver()
            }
        }
        if (phase != PLAYING) {
            heatShown = approach(heatShown, if (phase == OVER) 0.6f else 1f, 3f, dt)
            headSmokeT -= dt
            if (headSmokeT <= 0f) {
                headSmokeT = 0.12f
                headPoint(rnd(420f, 610f), rnd(110f, 170f))
                smoke.spawn(RANDOM.nextInt(3), pt[0], pt[1], rnd(-20f, 20f), rnd(-110f, -70f), rnd(1.2f, 1.8f),
                    20f, rnd(90f, 150f), 0.3f, spin = rnd(-60f, 60f))
            }
        }

        // shake offsets
        val shakeAmp = shake * shake * 46f + (if (phase == PLAYING && stage == 3) (heat - 0.8f) * 25f else 0f)
        shakeX = rnd(-1f, 1f) * shakeAmp
        shakeY = rnd(-1f, 1f) * shakeAmp
        updatePuppet(dt)

        // score counter
        scoreShown = approach(scoreShown, score.toFloat(), 10f, dt)
        if (abs(scoreShown - score) < 0.5f) scoreShown = score.toFloat()
        val sv = scoreShown.toInt()
        if (sv != scoreStrVal) {
            scoreStrVal = sv
            scoreStr = sv.toString()
        }
    }

    private fun updatePlaying(dt: Float) {
        alive += dt
        stateT += dt
        sinceLastPuff += dt
        pickCooldown -= dt
        missT = maxOf(0f, missT - dt)
        comboT = maxOf(0f, comboT - dt)
        hintT += dt
        if (sinceLastPuff > 1.3f) combo = 0

        // heat cools down when not inhaling, never below the damage floor
        if (state != INHALE) heat = maxOf(floor, heat - 0.085f * dt)
        heatShown = approach(heatShown, heat, 9f, dt)

        val st = stage
        if (st > lastStage) {
            when (st) {
                1 -> floating.add("FEELING HOT  x2", g.vw / 2, g.vh * 0.2f + g.safeTop, 54f, 0xFFFFC27A.toInt())
                2 -> floating.add("SWEATING!  x3", g.vw / 2, g.vh * 0.2f + g.safeTop, 60f, 0xFFFF8A4A.toInt())
                3 -> {
                    floating.add("DANGER!  x5", g.vw / 2, g.vh * 0.2f + g.safeTop, 78f, 0xFFFF3A2A.toInt(), 1.5f)
                    g.audio.play("cough", 0.8f)
                }
            }
        }
        lastStage = st

        // heartbeat
        if (heat > 0.5f) {
            heartTimer -= dt
            if (heartTimer <= 0f) {
                val k = clamp01((heat - 0.5f) / 0.5f)
                heartTimer = lerp(0.95f, 0.4f, k)
                g.audio.play("heartbeat", 0.45f + 0.55f * k)
                heartPulse = 1f
                if (st == 3) g.haptics.buzz(35, 160)
            }
        } else {
            heartTimer = 0f
        }
        heartPulse = maxOf(0f, heartPulse - dt * 3f)

        // packs
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer = rnd(1.0f, 1.7f) / (1f + alive / 100f)
            spawnBox()
        }
        val speed = 330f * (1f + min(alive / 60f, 1.2f))
        for (i in boxX.indices) {
            if (!boxOn[i]) continue
            boxX[i] -= speed * dt
            if (boxX[i] < -200f) boxOn[i] = false
        }

        // arm state machine
        when (state) {
            PICK -> if (stateT >= 0.38f) {
                setState(LIGHT)
                g.audio.play("lighter", 0.9f)
                armPoint(HAND_X, HAND_Y)
                for (i in 0 until 10) {
                    embers.spawn(0, pt[0], pt[1], rnd(-250f, 250f), rnd(-300f, 50f), rnd(0.3f, 0.6f),
                        18f, 4f, 0.9f, pdrag = 2f, gravity = 600f)
                }
            }
            LIGHT -> if (stateT >= 0.6f) {
                val b = BRANDS[flyBrand]
                brand = b
                holding = true
                puffsLeft = b.puffs
                cigarettes++
                flyBrand = -1
                if (b.cool > 0f) {
                    heat = maxOf(floor, heat - b.cool)
                    floating.add("COOL!", g.vw / 2, g.vh * 0.25f, 64f, 0xFF9FE3FF.toInt())
                }
                setState(HOLD)
            }
            INHALE -> {
                inhaleLeft -= dt
                if (inhaleLeft <= 0f) {
                    setState(EXHALE)
                    if (heat > 0.5f && RANDOM.nextFloat() < 0.3f) g.audio.play("cough", 0.75f)
                    else g.audio.play("exhale", 0.75f)
                }
            }
            EXHALE -> if (stateT >= 0.7f) setState(if (puffsLeft <= 0) THROW else HOLD)
            HOLD -> if (puffsLeft <= 0) setState(THROW)
            THROW -> {
                if (holding && stateT >= 0.2f) {
                    holding = false
                    brand = null
                    floor = min(0.75f, floor + 0.04f)
                    armPoint(TIP_HOLD_X, TIP_HOLD_Y)
                    embers.spawn(1, pt[0], pt[1], rnd(250f, 450f), -650f, 1.2f, 70f, 70f, 1f,
                        rotation = 0f, spin = 720f, gravity = 1700f)
                    g.audio.play("pop", 0.6f)
                    if (hintStep == 2) {
                        hintStep = 3
                        hint = "Grab another pack! Each one damages your lungs"
                        hintT = 0f
                    }
                }
                if (stateT >= 0.42f) setState(IDLE)
            }
        }

        // emitters
        if (holding) {
            tipSmokeT -= dt
            if (tipSmokeT <= 0f) {
                tipSmokeT = if (state == INHALE) 0.12f else 0.065f
                tipPoint()
                smoke.spawn(RANDOM.nextInt(3), pt[0] + rnd(-2f, 2f), pt[1] - 6f, rnd(-12f, 12f) + sin(t * 1.7f) * 20f,
                    rnd(-95f, -65f), rnd(2f, 2.8f), 12f, rnd(95f, 150f), rnd(0.18f, 0.28f), spin = rnd(-50f, 50f))
            }
            if (state == INHALE && RANDOM.nextFloat() < dt * (6f + heat * 14f)) {
                tipPoint()
                embers.spawn(0, pt[0], pt[1], rnd(-120f, 120f), rnd(-260f, -80f), rnd(0.3f, 0.6f),
                    14f, 3f, 1f, gravity = 500f)
            }
        }
        if (state == EXHALE && stateT < 0.55f) {
            exhaleSmokeT -= dt
            if (exhaleSmokeT <= 0f) {
                exhaleSmokeT = 0.028f
                headPoint(MOUTH_X, MOUTH_Y + 10f)
                val power = 0.35f + 0.3f * min(combo, 6) / 6f
                smoke.spawn(RANDOM.nextInt(3), pt[0], pt[1], rnd(-130f, 130f), rnd(-20f, 90f), rnd(1.5f, 2.3f),
                    rnd(30f, 50f), rnd(200f, 330f), power, spin = rnd(-50f, 50f), pdrag = 1.6f, gravity = -55f)
            }
        }
        if (st >= 2) {
            sweatT -= dt
            if (sweatT <= 0f) {
                sweatT = if (st == 3) 0.1f else 0.24f
                headPoint(if (RANDOM.nextBoolean()) rnd(400f, 450f) else rnd(575f, 625f), rnd(190f, 270f))
                sweat.spawn(-1, pt[0], pt[1], rnd(-30f, 30f), rnd(0f, 60f), rnd(0.5f, 0.8f), 16f * charS, 12f * charS,
                    0.85f, gravity = 1100f)
            }
        }
        if (st == 3) {
            steamT -= dt
            if (steamT <= 0f) {
                steamT = 0.07f
                val side = if (RANDOM.nextBoolean()) -1f else 1f
                headPoint(if (side < 0) 388f else 638f, 300f)
                smoke.spawn(RANDOM.nextInt(3), pt[0], pt[1], side * rnd(90f, 170f), rnd(-160f, -80f), rnd(0.6f, 0.9f),
                    18f, rnd(80f, 120f), 0.55f, spin = rnd(-90f, 90f), pdrag = 1f)
            }
        }

        // button states
        pickBtn.enabled = !holding && state == IDLE
        smokeBtn.enabled = holding && puffsLeft > 0 && (state == HOLD || state == INHALE || state == EXHALE)
        var inZone = false
        for (i in boxX.indices) if (boxOn[i] && abs(boxX[i] - zoneX) < ZONE_HALF) inZone = true
        pickBtn.pulse = pickBtn.enabled && inZone
        smokeBtn.pulse = smokeBtn.enabled && state == HOLD
    }

    private fun spawnBox() {
        val slot = boxOn.indexOfFirst { !it }
        if (slot < 0) return
        var total = 0
        for (b in BRANDS) total += b.weight
        var roll = RANDOM.nextInt(total)
        var type = 0
        for (i in BRANDS.indices) {
            roll -= BRANDS[i].weight
            if (roll < 0) { type = i; break }
        }
        // keep packs from overlapping
        for (i in boxX.indices) if (boxOn[i] && boxX[i] > g.vw - 60f) return
        boxOn[slot] = true
        boxType[slot] = type
        boxX[slot] = g.vw + 140f
    }

    private fun updatePuppet(dt: Float) {
        // arm pose + rotation around the shoulder
        val pose: String
        var rot: Float
        when (state) {
            PICK -> {
                pose = "char_arm_hold"
                rot = 30f * (1f - Ease.outCubic(stateT / 0.38f))
            }
            LIGHT, HOLD -> {
                pose = "char_arm_hold"
                rot = 1.2f * sin(t * 1.3f)
            }
            INHALE -> {
                pose = "char_arm_mouth"
                rot = -14f * (1f - Ease.outCubic(stateT / 0.1f))
            }
            EXHALE -> {
                pose = "char_arm_hold"
                rot = 9f * (1f - Ease.outCubic(stateT / 0.18f)) + 1.2f * sin(t * 1.3f)
            }
            THROW -> {
                pose = if (stateT < 0.3f) "char_arm_hold" else "char_arm_idle"
                rot = if (stateT < 0.3f) -32f * sin(PI.toFloat() * stateT / 0.3f) else 0f
            }
            else -> {
                pose = "char_arm_idle"
                rot = 1.5f * sin(t * 1.4f)
            }
        }
        if (missT > 0f) rot += -10f * sin(PI.toFloat() * missT / 0.35f)
        if (pose != armName) {
            prevArm = armName
            prevArmRot = armRot
            armName = pose
            armFade = 1f
        }
        armRot = rot
        armFade = maxOf(0f, armFade - dt / 0.09f)

        // head expression
        val head = when {
            phase != PLAYING -> "head_burnt"
            stage == 3 -> "head_red_3"
            stage == 2 -> "head_red_2"
            stage == 1 -> "head_red_1"
            state == INHALE -> "head_smoking"
            state == EXHALE && stateT < 0.55f -> "head_exhale"
            else -> "head_normal"
        }
        if (head != headName) {
            prevHead = headName
            headName = head
            headFade = 1f
        }
        headFade = maxOf(0f, headFade - dt / 0.12f)
        val targetRot = when (state) {
            INHALE -> -3f
            EXHALE -> 2f
            else -> 0.8f * sin(t * 0.9f)
        }
        headRot = approach(headRot, targetRot, 12f, dt)
        headDy = approach(headDy, if (state == INHALE) -5f else 0f, 12f, dt)
        headJitter = if (phase == PLAYING && stage == 3) rnd(-1f, 1f) * (heat - 0.8f) * 18f else 0f
        headScale = if (phase == PLAYING && stage >= 2) 1f + 0.025f * heartPulse * (stage - 1) else 1f
    }

    // ------------------------------------------------------------------ geometry

    /** Canvas point on the (rotated) arm -> virtual coordinates in [pt]. */
    private fun armPoint(px: Float, py: Float) {
        val a = Math.toRadians(armRot.toDouble())
        val ca = cos(a).toFloat()
        val sa = sin(a).toFloat()
        val dx = px - SHOULDER_X
        val dy = py - SHOULDER_Y
        val x = SHOULDER_X + dx * ca - dy * sa
        val y = SHOULDER_Y + dx * sa + dy * ca
        pt[0] = charX + shakeX + x * charS
        pt[1] = charY + (y + bob) * charS
    }

    /** Canvas point on the head -> virtual coordinates in [pt]. */
    private fun headPoint(px: Float, py: Float) {
        pt[0] = charX + shakeX + (px + headJitter) * charS
        pt[1] = charY + (py + bob + headDy) * charS
    }

    private fun tipPoint() {
        if (state == INHALE) armPoint(TIP_MOUTH_X, TIP_MOUTH_Y) else armPoint(TIP_HOLD_X, TIP_HOLD_Y)
    }

    // ------------------------------------------------------------------ drawing

    override fun draw(c: Canvas) {
        c.save()
        c.translate(shakeX, shakeY)
        dst.set(bgX - 30f, bgY - 30f, bgX + bg.width * bgS + 30f, bgY + bg.height * bgS + 30f)
        c.drawBitmap(bg, null, dst, paint)

        drawHero(c)

        // counter in front of the hero
        dst.set(bgX - 30f, bgY + COUNTER_TOP * bgS, bgX + bg.width * bgS + 30f, bgY + bg.height * bgS + 30f)
        c.drawBitmap(bg, counterSrc, dst, paint)

        drawZoneAndBoxes(c)
        drawCigaretteFx(c)

        smoke.draw(c, puffImgs)
        darkSmoke.draw(c, puffImgs)
        sweat.draw(c, puffImgs)
        embers.draw(c, emberImgs)
        drawHaze(c)
        drawExplosion(c)
        c.restore()

        drawVignette(c)
        if (flash > 0f) g.dim(c, flash * flash, Color.WHITE)
        drawHud(c)
        floating.draw(c)
        popup?.draw(c)
    }

    private fun drawLayer(c: Canvas, name: String, alpha: Float, filter: ColorFilter? = null) {
        val l = layers[name] ?: return
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        paint.colorFilter = filter
        c.drawBitmap(l.bmp, l.x, l.y, paint)
        paint.colorFilter = null
        paint.alpha = 255
    }

    private fun drawHero(c: Canvas) {
        c.save()
        c.translate(charX, charY)
        c.scale(charS, charS)
        c.translate(0f, bob)

        // head (behind the collar)
        c.save()
        c.translate(headJitter, headDy)
        c.rotate(headRot, NECK_X, NECK_Y)
        if (headScale != 1f) c.scale(headScale, headScale, NECK_X, NECK_Y)
        val tint = if (phase == PLAYING) {
            val v = heatShown * 70f + (if (stage >= 2) heartPulse * 35f else 0f)
            tintFilters[(v / 6f).toInt().coerceIn(0, tintFilters.size - 1)]
        } else null
        if (headFade > 0f) drawLayer(c, prevHead, 1f, tint)
        drawLayer(c, headName, 1f - headFade, tint)
        c.restore()

        drawLayer(c, "char_body", 1f)

        if (armFade > 0f) {
            c.save()
            c.rotate(prevArmRot, SHOULDER_X, SHOULDER_Y)
            drawLayer(c, prevArm, armFade)
            c.restore()
        }
        c.save()
        c.rotate(armRot, SHOULDER_X, SHOULDER_Y)
        drawLayer(c, armName, 1f)
        c.restore()
        c.restore()
    }

    private fun drawZoneAndBoxes(c: Canvas) {
        // gold target ring on the counter
        var inZone = false
        for (i in boxX.indices) if (boxOn[i] && abs(boxX[i] - zoneX) < ZONE_HALF) inZone = true
        val active = phase == PLAYING && !holding && state == IDLE
        val pulse = 0.5f + 0.5f * sin(t * 5f)
        val ry = 34f * bgS
        r.set(zoneX - ZONE_HALF, boxBaseY - ry, zoneX + ZONE_HALF, boxBaseY + ry)
        fill.color = 0xFFFFC84A.toInt()
        fill.alpha = if (active) (if (inZone) 110 else 40 + (30 * pulse).toInt()) else 18
        c.drawOval(r, fill)
        stroke.color = 0xFFFFE08A.toInt()
        stroke.strokeWidth = 5f
        stroke.alpha = if (active) (if (inZone) 255 else 140 + (90 * pulse).toInt()) else 60
        c.drawOval(r, stroke)

        for (i in boxX.indices) {
            if (!boxOn[i]) continue
            val b = boxBitmaps[boxType[i]]
            val w = boxH * b.width / b.height
            val x = boxX[i]
            fill.color = Color.BLACK
            fill.alpha = 90
            r.set(x - w * 0.55f, boxBaseY - 12f * bgS, x + w * 0.55f, boxBaseY + 12f * bgS)
            c.drawOval(r, fill)
            dst.set(x - w / 2, boxBaseY - boxH, x + w / 2, boxBaseY)
            c.drawBitmap(b, null, dst, paint)
        }

        // pack flying to the hand, then shrinking as the cigarette is taken out
        if (flyBrand >= 0 && (state == PICK || (state == LIGHT && stateT < 0.2f))) {
            armPoint(HAND_X, HAND_Y)
            val hx = pt[0]
            val hy = pt[1]
            val b = boxBitmaps[flyBrand]
            val x: Float
            val y: Float
            val s: Float
            var a = 1f
            var rot = 0f
            if (state == PICK) {
                val p = clamp01(stateT / 0.38f)
                val e = Ease.inOutSine(p)
                x = lerp(flyX, hx, e)
                y = lerp(flyY, hy, e) - sin(PI.toFloat() * p) * 160f
                s = lerp(1f, 0.6f, e)
                rot = p * 25f
            } else {
                val p = clamp01(stateT / 0.2f)
                x = hx
                y = hy
                s = lerp(0.6f, 0.2f, p)
                a = 1f - p
                rot = 25f
            }
            val h = boxH * s
            val w = h * b.width / b.height
            c.save()
            c.rotate(rot, x, y)
            dst.set(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
            paint.alpha = (a * 255).toInt()
            c.drawBitmap(b, null, dst, paint)
            paint.alpha = 255
            c.restore()
        }
    }

    private fun drawGlow(c: Canvas, x: Float, y: Float, radius: Float, alpha: Float) {
        if (alpha <= 0f) return
        glow.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        c.save()
        c.translate(x, y)
        c.scale(radius, radius)
        c.drawCircle(0f, 0f, 1f, glow)
        c.restore()
    }

    private fun drawCigaretteFx(c: Canvas) {
        if (state == LIGHT && stateT in 0.1f..0.55f) {
            // lighter flame at the tip
            armPoint(TIP_HOLD_X, TIP_HOLD_Y)
            val f = 0.8f + 0.2f * sin(t * 40f) * sin(t * 23f)
            drawGlow(c, pt[0], pt[1], 70f * f * charS, 0.9f)
            val e = emberImgs[0]
            val h = 85f * f * charS
            val w = h * e.width / e.height
            c.save()
            c.rotate(-30f, pt[0], pt[1])
            dst.set(pt[0] - w / 2, pt[1] - h * 0.85f, pt[0] + w / 2, pt[1] + h * 0.15f)
            addPaint.alpha = 255
            c.drawBitmap(e, null, dst, addPaint)
            c.restore()
        }
        if (holding) {
            tipPoint()
            val flick = 0.8f + 0.2f * sin(t * 17f) * sin(t * 9.1f)
            val inhale = if (state == INHALE) 1f else 0f
            drawGlow(c, pt[0], pt[1], (20f + 18f * inhale) * flick * charS, 0.55f + 0.45f * inhale)
        }
    }

    private fun drawHaze(c: Canvas) {
        if (haze <= 0.01f) return
        val a = haze * 0.35f
        for (i in 0..2) {
            val b = puffImgs[i]
            val w = g.vw * 1.5f
            val h = w * b.height / b.width
            val x = g.vw / 2 + sin(t * 0.13f + i * 2.1f) * 160f
            val y = g.vh * (0.28f + i * 0.17f)
            paint.alpha = (a * 255).toInt()
            dst.set(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
            c.drawBitmap(b, null, dst, paint)
        }
        paint.alpha = 255
    }

    private fun drawExplosion(c: Canvas) {
        if (phase == PLAYING || explodeT > 1.1f) return
        val p = explodeT
        val size = lerp(160f, 1100f, Ease.outCubic(clamp01(p / 0.45f)))
        val a = 1f - smoothstep(0.25f, 1.0f, p)
        drawGlow(c, explodeX, explodeY, size * 0.9f, a * 0.8f)
        val b = g.assets["explosion"]
        c.save()
        c.rotate(p * 35f, explodeX, explodeY)
        g.drawCentered(c, b, explodeX, explodeY, size, a)
        c.restore()
    }

    private fun drawVignette(c: Canvas) {
        var v = smoothstep(0.28f, 1f, heatShown) * (0.72f + 0.28f * heartPulse)
        if (phase == EXPLODING) v = maxOf(v, 1f - explodeT * 0.3f)
        if (v <= 0.01f) return
        vignettePaint.alpha = (v * 255).toInt().coerceIn(0, 255)
        vignettePaint.shader = vigLeft
        c.drawRect(0f, 0f, g.vw * 0.32f, g.vh, vignettePaint)
        vignettePaint.shader = vigRight
        c.drawRect(g.vw * 0.68f, 0f, g.vw, g.vh, vignettePaint)
        vignettePaint.alpha = (v * 150).toInt().coerceIn(0, 255)
        vignettePaint.shader = vigTop
        c.drawRect(0f, 0f, g.vw, g.vh * 0.14f, vignettePaint)
        vignettePaint.shader = vigBottom
        c.drawRect(0f, g.vh * 0.86f, g.vw, g.vh, vignettePaint)
        vignettePaint.shader = null
    }

    private fun drawHud(c: Canvas) {
        // score
        val panel = g.assets["score_panel"]
        val pw = 360f
        val ph = pw * panel.height / panel.width
        val px = 26f
        val py = g.safeTop + 26f
        dst.set(px, py, px + pw, py + ph)
        c.drawBitmap(panel, null, dst, paint)
        g.text.draw(c, "SCORE", px + 30f, py + ph / 2 + 12f, 32f, color = 0xFFF3E3C0.toInt(), align = Paint.Align.LEFT)
        g.text.draw(c, scoreStr, px + pw - 30f, py + ph / 2 + 22f, 64f, align = Paint.Align.RIGHT)
        if (best > 0) g.text.draw(c, bestStr, px + 8f, py + ph + 44f, 34f, color = 0xFFF3E3C0.toInt(), align = Paint.Align.LEFT)

        // heat meter
        val hy = py + ph + (if (best > 0) 72f else 30f)
        val hw = pw
        val hh = 34f
        r.set(px, hy, px + hw, hy + hh)
        fill.color = 0xCC1A0703.toInt()
        c.drawRoundRect(r, hh / 2, hh / 2, fill)
        val inner = hw - 8f
        if (floor > 0f) {
            r.set(px + 4f, hy + 4f, px + 4f + inner * floor, hy + hh - 4f)
            fill.color = 0xFF5A0E0A.toInt()
            c.drawRoundRect(r, hh / 2, hh / 2, fill)
        }
        val hv = clamp01(heatShown)
        if (hv > 0.01f) {
            r.set(px + 4f, hy + 4f, px + 4f + inner * hv, hy + hh - 4f)
            fill.color = heatColor(hv)
            c.drawRoundRect(r, hh / 2, hh / 2, fill)
        }
        r.set(px, hy, px + hw, hy + hh)
        stroke.color = 0xFFE7B54A.toInt()
        stroke.strokeWidth = 4f
        stroke.alpha = 255
        c.drawRoundRect(r, hh / 2, hh / 2, stroke)
        g.text.draw(c, "HEAT", px + 18f, hy + hh - 7f, 26f, color = Color.WHITE, align = Paint.Align.LEFT)
        val st = if (phase == PLAYING) stage else 3
        g.text.draw(c, "x${STAGE_MULT[st]}", px + hw + 16f, hy + hh + 2f, 46f,
            color = if (st == 0) TextPainter.GOLD else heatColor(heatShown), align = Paint.Align.LEFT)

        if (comboT > 0f && combo >= 3) {
            val k = Ease.outBack(clamp01((1.2f - comboT) / 0.25f))
            g.text.draw(c, "COMBO x$combo", g.vw / 2, g.safeTop + 330f, 70f * k, alpha = clamp01(comboT / 0.3f))
        }

        // controls
        pauseBtn.draw(c, paint)
        pickBtn.draw(c, paint)
        smokeBtn.draw(c, paint)
        drawCigaretteGauge(c)

        // tutorial hint
        if (hintStep < 4 && phase == PLAYING) {
            val a = if (hintStep == 3) 1f - clamp01((hintT - 3f) / 0.5f) else clamp01(hintT / 0.4f)
            if (hintStep == 3 && hintT > 3.5f) hintStep = 4
            if (a > 0f) {
                val y = smokeBtn.cy - 215f
                val w = g.text.width(hint, 40f) + 60f
                r.set(g.vw / 2 - w / 2, y - 48f, g.vw / 2 + w / 2, y + 22f)
                fill.color = 0xAA120402.toInt()
                fill.alpha = (a * 170).toInt()
                c.drawRoundRect(r, 30f, 30f, fill)
                g.text.draw(c, hint, g.vw / 2, y, 40f, color = 0xFFFFF1C8.toInt(), alpha = a)
            }
        }
    }

    private fun drawCigaretteGauge(c: Canvas) {
        val cx = g.vw / 2
        val cy = smokeBtn.cy
        val b = brand
        if (b == null || !holding) {
            val label = if (state == PICK || state == LIGHT) "LIGHTING UP..." else "NO CIGARETTE"
            g.text.draw(c, label, cx, cy + 14f, 40f, color = 0xFFE8C987.toInt(), alpha = 0.85f)
            return
        }
        val cig = emberImgs[1]
        val w = 330f
        val h = w * cig.height / cig.width
        val x0 = cx - w / 2
        val frac = puffsLeft.toFloat() / b.puffs
        val filter = 0.32f
        val visible = filter + (1f - filter) * frac
        dst.set(x0, cy - h / 2, x0 + w, cy + h / 2)
        paint.alpha = 50
        c.drawBitmap(cig, null, dst, paint)
        paint.alpha = 255
        c.save()
        c.clipRect(x0, cy - h, x0 + w * visible, cy + h)
        c.drawBitmap(cig, null, dst, paint)
        c.restore()
        drawGlow(c, x0 + w * visible - 6f, cy, 22f, 0.7f + 0.3f * sin(t * 12f))
        g.text.draw(c, b.name, cx, cy - 48f, 42f)
        g.text.draw(c, "$puffsLeft PUFFS", cx, cy + 76f, 36f, color = 0xFFF3E3C0.toInt())
    }

    private fun heatColor(v: Float): Int {
        val k = clamp01(v)
        return if (k < 0.5f) blend(0xFFFFE066.toInt(), 0xFFFF9020.toInt(), k / 0.5f)
        else blend(0xFFFF9020.toInt(), 0xFFFF1A0A.toInt(), (k - 0.5f) / 0.5f)
    }

    private fun blend(a: Int, b: Int, k: Float): Int {
        val r = lerp(Color.red(a).toFloat(), Color.red(b).toFloat(), k).toInt()
        val gg = lerp(Color.green(a).toFloat(), Color.green(b).toFloat(), k).toInt()
        val bb = lerp(Color.blue(a).toFloat(), Color.blue(b).toFloat(), k).toInt()
        return Color.argb(255, r, gg, bb)
    }

    // ------------------------------------------------------------------ input

    override fun touch(t: Touch) {
        popup?.let { it.touch(t); return }
        if (phase != PLAYING) return
        when (t.type) {
            Touch.DOWN -> for (b in buttons) if (b.down(t)) break
            Touch.UP -> buttons.forEach { it.up(t) }
            else -> buttons.forEach { it.cancel() }
        }
    }

    override fun back(): Boolean {
        val p = popup
        if (p != null) p.back() else if (phase == PLAYING) { g.click(); openPause() }
        return true
    }

    override fun onPause() {
        if (phase == PLAYING) openPause()
    }
}
