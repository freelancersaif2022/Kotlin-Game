package com.freelancersaif.snakeattack

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import kotlin.math.cos
import kotlin.math.sin

/** The five worlds, ten levels each, cycling after level 50. */
enum class World(
    val title: String,
    val bg: String,
    val particle: String,
    val skin: DragonSkin,
    val boardTint: Color,
    val story: Int,
) {
    FOREST("Enchanted Forest", "bg/bg_forest.jpg", "leaf_particle", DragonSkinCatalog.RED, Color(0.08f, 0.24f, 0.06f, 0.42f), 1),
    DESERT("Sunny Desert", "bg/bg_desert.jpg", "sand_particle", DragonSkinCatalog.GREEN, Color(0.36f, 0.22f, 0.05f, 0.38f), 2),
    SNOW("Frozen Peaks", "bg/bg_snow.jpg", "snowflake_particle", DragonSkinCatalog.ICE, Color(0.1f, 0.22f, 0.4f, 0.4f), 3),
    VOLCANO("Volcano Lair", "bg/bg_volcano.jpg", "ember_particle", DragonSkinCatalog.LAVA, Color(0.18f, 0.06f, 0.03f, 0.5f), 4),
    NIGHT("Moonlit Night", "bg/bg_night.jpg", "firefly_particle", DragonSkinCatalog.PURPLE, Color(0.06f, 0.06f, 0.2f, 0.5f), 0);

    companion object {
        fun of(level: Int) = entries[((level - 1) / 10) % entries.size]
        fun isBoss(level: Int) = level % 10 == 0
    }
}

/** A dragon colouring: hue rotation (degrees), saturation and brightness applied by [HueShader]. */
class DragonSkin(val id: String, val title: String, val preview: String, val hue: Float, val sat: Float = 1f,
                 val bright: Float = 1f, val price: Int = 0, val rainbow: Boolean = false)

object DragonSkinCatalog {
    val RED = DragonSkin("skin_red", "Fire Red", "ui_dragon_head_red", 0f, price = 400)
    val GREEN = DragonSkin("skin_green", "Jungle Green", "ui_dragon_head_green", 105f, 1.05f, 1.0f, 500)
    val ICE = DragonSkin("skin_ice", "Ice Blue", "ui_dragon_head_ice_blue", 190f, 0.85f, 1.12f, 600)
    val LAVA = DragonSkin("skin_lava", "Black Lava", "ui_dragon_head_black_lava", -8f, 0.55f, 0.62f, 700)
    val PURPLE = DragonSkin("skin_purple", "Shadow Purple", "ui_dragon_head_purple_shadow", 255f, 1.0f, 0.95f, 700)
    val PINK = DragonSkin("skin_pink", "Candy Pink", "ui_dragon_head_pink", 320f, 0.8f, 1.12f, 800)
    val GOLD = DragonSkin("skin_gold", "Golden King", "ui_dragon_head_gold_boss", 16f, 1.15f, 1.22f, 1500)
    val RAINBOW = DragonSkin("skin_rainbow", "Rainbow", "ui_dragon_head_rainbow", 0f, price = 2000, rainbow = true)
    val AUTO = DragonSkin("skin_auto", "World Colours", "ui_dragon_head_red", 0f)

    val shop = listOf(AUTO, RED, GREEN, ICE, LAVA, PURPLE, PINK, GOLD, RAINBOW)

    /** Skin used for a level: the equipped one, or the world's colour (bosses are always gold). */
    fun forLevel(level: Int): DragonSkin {
        if (World.isBoss(level)) return GOLD
        val equipped = shop.firstOrNull { it.id == Save.dragonSkin } ?: AUTO
        return if (equipped === AUTO) World.of(level).skin else equipped
    }
}

/** SpriteBatch shader that hue-rotates, (de)saturates and brightens everything drawn while active. */
object HueShader {
    private const val VERT = """
attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 v_texCoords;
void main() {
    v_color = a_color;
    v_color.a = v_color.a * (255.0/254.0);
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}
"""
    private const val FRAG = """
#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform mat3 u_hue;
uniform float u_bright;
void main() {
    vec4 c = texture2D(u_texture, v_texCoords);
    vec3 rgb = clamp(u_hue * c.rgb, 0.0, 1.0) * u_bright;
    gl_FragColor = vec4(rgb, c.a) * v_color;
}
"""
    private var program: ShaderProgram? = null
    private val m = Matrix3()

    private fun get(): ShaderProgram? {
        if (program == null) {
            ShaderProgram.pedantic = false
            val p = ShaderProgram(VERT, FRAG)
            if (!p.isCompiled) {
                Gdx.app.error("HueShader", p.log)
                return null
            }
            program = p
        }
        return program
    }

    /** Starts drawing with the skin's colouring; [time] animates the rainbow skin. */
    fun begin(batch: Batch, skin: DragonSkin, time: Float) {
        if (skin.hue == 0f && skin.sat == 1f && skin.bright == 1f && !skin.rainbow) return
        val p = get() ?: return
        batch.shader = p
        val hue = if (skin.rainbow) time * 90f else skin.hue
        setMatrix(hue, skin.sat)
        p.setUniformMatrix("u_hue", m)
        p.setUniformf("u_bright", skin.bright)
    }

    fun end(batch: Batch) {
        if (batch.shader != null) batch.shader = null
    }

    /** Luminance-preserving hue rotation combined with a saturation scale. */
    private fun setMatrix(hueDeg: Float, sat: Float) {
        val a = Math.toRadians(hueDeg.toDouble())
        val c = cos(a).toFloat()
        val s = sin(a).toFloat()
        val lr = 0.299f
        val lg = 0.587f
        val lb = 0.114f
        // hue rotation (row-major as in the classic SVG feColorMatrix hueRotate)
        val h = floatArrayOf(
            lr + c * (1 - lr) + s * (-lr), lg + c * (-lg) + s * (-lg), lb + c * (-lb) + s * (1 - lb),
            lr + c * (-lr) + s * 0.143f, lg + c * (1 - lg) + s * 0.140f, lb + c * (-lb) + s * (-0.283f),
            lr + c * (-lr) + s * (-(1 - lr)), lg + c * (-lg) + s * lg, lb + c * (1 - lb) + s * lb,
        )
        // saturation
        val sr = (1 - sat) * lr
        val sg = (1 - sat) * lg
        val sb = (1 - sat) * lb
        val sm = floatArrayOf(
            sr + sat, sg, sb,
            sr, sg + sat, sb,
            sr, sg, sb + sat,
        )
        val r = FloatArray(9)
        for (i in 0..2) for (j in 0..2) {
            var v = 0f
            for (k in 0..2) v += sm[i * 3 + k] * h[k * 3 + j]
            r[i * 3 + j] = v
        }
        // Matrix3 is column-major; transpose the row-major result
        m.`val`[0] = r[0]; m.`val`[3] = r[1]; m.`val`[6] = r[2]
        m.`val`[1] = r[3]; m.`val`[4] = r[4]; m.`val`[7] = r[5]
        m.`val`[2] = r[6]; m.`val`[5] = r[7]; m.`val`[8] = r[8]
    }

    fun dispose() {
        program?.dispose()
        program = null
    }
}
