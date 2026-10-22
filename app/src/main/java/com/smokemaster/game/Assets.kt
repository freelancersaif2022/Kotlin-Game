package com.smokemaster.game

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import java.util.concurrent.ConcurrentHashMap

class Assets(private val am: AssetManager) {

    private val bitmaps = ConcurrentHashMap<String, Bitmap>()

    /** Offset of each trimmed puppet layer inside the shared 1024x1024 canvas. */
    val layerOffset = HashMap<String, PointF>()

    /** Crop origin (x, y) and scale applied to each trimmed sprite. */
    val spriteMeta = HashMap<String, FloatArray>()

    operator fun get(name: String): Bitmap = bitmaps[name] ?: throw IllegalStateException("bitmap $name not loaded")

    fun has(name: String) = bitmaps.containsKey(name)

    /** What the splash screen needs; loaded synchronously before the first frame. */
    fun loadCore() {
        readMeta("layers.txt") { p -> layerOffset[p[0]] = PointF(p[1].toFloat(), p[2].toFloat()) }
        readMeta("sprites.txt") { p -> spriteMeta[p[0]] = floatArrayOf(p[1].toFloat(), p[2].toFloat(), p[3].toFloat()) }
        CORE.forEach(::load)
    }

    fun loadAll(progress: (Float) -> Unit) {
        val all = ALL.filter { !bitmaps.containsKey(it.substringBefore('.')) }
        all.forEachIndexed { i, file ->
            load(file)
            progress((i + 1f) / all.size)
        }
    }

    private fun load(file: String) {
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val bmp = am.open("img/$file").use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("cannot decode $file")
        bmp.prepareToDraw()
        bitmaps[file.substringBefore('.')] = bmp
    }

    private fun readMeta(file: String, each: (List<String>) -> Unit) {
        am.open("img/$file").bufferedReader().useLines { lines ->
            lines.map { it.trim() }.filter { it.isNotEmpty() }.forEach { each(it.split(' ')) }
        }
    }

    companion object {
        val CORE = listOf("splash_bg.jpg", "game_logo.png", "smoke_puff_1.png", "smoke_puff_2.png", "smoke_puff_3.png", "ember_spark.png")
        val ALL = listOf(
            "lobby_bg.jpg", "gameplay_bg.jpg", "character_lobby.png",
            "btn_play.png", "btn_highscore.png", "btn_settings.png",
            "char_body.png", "char_arm_idle.png", "char_arm_hold.png", "char_arm_mouth.png",
            "head_normal.png", "head_smoking.png", "head_exhale.png", "head_red_1.png",
            "head_red_2.png", "head_red_3.png", "head_burnt.png",
            "cig_box_1.png", "cig_box_2.png", "cig_box_3.png", "cig_box_4.png", "cig_box_5.png",
            "cigarette_lit.png", "explosion.png",
            "btn_pick_hand.png", "btn_smoke.png", "btn_pause.png", "score_panel.png",
            "popup_panel.png", "btn_resume.png", "btn_home.png", "btn_close.png",
            "toggle_on.png", "toggle_off.png", "icon_music.png", "icon_sound.png", "icon_vibration.png",
            "trophy.png", "char_gameover.png", "gameover_title.png", "btn_play_again.png",
        )
    }
}
