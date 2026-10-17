package com.bramble.dash.game

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Pool
import com.bramble.dash.assets.RegionMeta
import com.bramble.dash.config.CollectibleType
import com.bramble.dash.config.EnemyDef
import com.bramble.dash.config.ObstacleDef
import com.bramble.dash.config.PowerUpType

/** Converts a pack collision AABB (bottom-left local px of the canvas) into world space. */
fun collisionRect(m: RegionMeta, x: Float, y: Float, scale: Float, forgiveness: Float, flipX: Boolean, out: Rectangle,
                  wScale: Float = 1f, hScale: Float = 1f): Rectangle {
    val cx = if (flipX) m.origW - m.collX - m.collW else m.collX
    val left = x + (cx - m.pivotX) * scale
    val bottom = y + (m.collY - m.pivotY) * scale
    val w = m.collW * scale
    val h = m.collH * scale
    val fw = w * forgiveness * wScale
    val fh = h * forgiveness * hScale
    // shrink horizontally around the centre, keep the bottom anchored so ground hazards stay grounded
    out.set(left + (w - fw) / 2f, bottom + (h - h * forgiveness) * 0.25f, fw, fh)
    return out
}

enum class Action { NONE, JUMP, SLIDE }

class Obstacle : Pool.Poolable {
    lateinit var def: ObstacleDef
    var x = 0f; var y = 0f; var baseX = 0f; var baseY = 0f
    var vx = 0f; var vy = 0f
    var time = 0f
    var phase = 0f
    var active = true          // timed hazards: currently hurting
    var warning = false        // timed/falling/roller: telegraphing
    var triggered = false
    var dead = false           // broken/knocked/collapsed and no longer colliding
    var deadTime = 0f
    var passed = false         // player has passed it (for slide-under stats)
    var hitPlayer = false
    var landed = false         // falling boulder has landed
    var collapseTimer = -1f
    var standTop = 0f          // standable top surface (SOLID / platforms)
    var activationDist = 0f
    var rollSpeed = 0f
    val box = Rectangle()
    override fun reset() {
        x = 0f; y = 0f; baseX = 0f; baseY = 0f; vx = 0f; vy = 0f; time = 0f; phase = 0f; active = true; warning = false
        triggered = false; dead = false; deadTime = 0f; passed = false; hitPlayer = false; landed = false; collapseTimer = -1f
        standTop = 0f; activationDist = 0f; rollSpeed = 0f
    }
}

/** Static elevated platform built from the world's platform tile. */
class Platform : Pool.Poolable {
    var x = 0f; var top = 0f; var width = 0f; var tiles = 1
    override fun reset() { x = 0f; top = 0f; width = 0f; tiles = 1 }
}

enum class EnemyState(val anim: String) { IDLE("idle"), MOVE("movement"), ATTACK("attack"), DAMAGE("damage"), DEATH("death") }

class Enemy : Pool.Poolable {
    lateinit var def: EnemyDef
    var x = 0f; var y = 0f; var baseX = 0f; var baseY = 0f
    var vx = 0f; var vy = 0f
    var state = EnemyState.IDLE
    var stateTime = 0f
    var time = 0f
    var alive = true
    var removed = false
    var fired = false
    var activationDist = 0f
    var chargeSpeed = 0f
    var alpha = 1f
    var solid = true
    var fireDistance = 0f
    var passed = false
    val box = Rectangle()
    fun set(s: EnemyState) { if (state != s) { state = s; stateTime = 0f } }
    override fun reset() {
        x = 0f; y = 0f; baseX = 0f; baseY = 0f; vx = 0f; vy = 0f; state = EnemyState.IDLE; stateTime = 0f; time = 0f; alive = true
        removed = false; fired = false; activationDist = 0f; chargeSpeed = 0f; alpha = 1f; solid = true; fireDistance = 0f; passed = false
    }
}

class Projectile : Pool.Poolable {
    var fx = ""                 // effects/<fx> animation
    var x = 0f; var y = 0f
    var vx = 0f; var vy = 0f
    var gravity = 0f
    var friendly = false
    var time = 0f
    var alive = true
    var scale = 0.5f
    var tint = 0xFFFFFFFF.toInt()
    var halfW = 20f; var halfH = 16f
    var lane = Action.NONE      // what the player must do to avoid it
    val box = Rectangle()
    fun updateBox() { box.set(x - halfW, y - halfH, halfW * 2, halfH * 2) }
    override fun reset() { fx = ""; x = 0f; y = 0f; vx = 0f; vy = 0f; gravity = 0f; friendly = false; time = 0f; alive = true; scale = 0.5f; tint = -1; halfW = 20f; halfH = 16f; lane = Action.NONE }
}

class Pickup : Pool.Poolable {
    var collectible: CollectibleType? = null
    var powerUp: PowerUpType? = null
    var x = 0f; var y = 0f; var baseY = 0f
    var time = 0f
    var collected = false
    var magnet = false
    val box = Rectangle()
    override fun reset() { collectible = null; powerUp = null; x = 0f; y = 0f; baseY = 0f; time = 0f; collected = false; magnet = false }
}

/** Visual-only animated effect from the pack's VFX sequences. */
class Effect : Pool.Poolable {
    var anim = ""
    var x = 0f; var y = 0f
    var time = 0f
    var scale = 1f
    var flipX = false
    var tint = 0xFFFFFFFF.toInt()
    var followPlayer = false
    var vx = 0f; var vy = 0f
    var loop = false
    var life = -1f              // seconds for looping effects; -1 = until animation ends
    override fun reset() { anim = ""; x = 0f; y = 0f; time = 0f; scale = 1f; flipX = false; tint = -1; followPlayer = false; vx = 0f; vy = 0f; loop = false; life = -1f }
}

class FloatText : Pool.Poolable {
    var text = ""
    var x = 0f; var y = 0f
    var time = 0f
    var color = 0xFFFFFFFF.toInt()
    override fun reset() { text = ""; time = 0f }
}

/** Warning indicator shown at the right screen edge before a hazard enters. */
class Warning(var y: Float = 0f, var time: Float = 0f, var big: Boolean = false)

/** Allocation-free, nesting-safe iteration over a LibGDX Array (its cached iterators cannot nest). */
inline fun <T> com.badlogic.gdx.utils.Array<T>.each(action: (T) -> Unit) {
    var i = 0
    while (i < size) { action(get(i)); i++ }
}
