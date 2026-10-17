package com.bramble.dash

import com.bramble.dash.assets.SpriteData
import com.bramble.dash.config.CharacterDef
import com.bramble.dash.config.Characters
import com.bramble.dash.config.WorldDef
import com.bramble.dash.game.GameWorld
import com.bramble.dash.game.RunSetup
import com.bramble.dash.game.WorldListener
import java.io.File

object TestSupport {
    val assetsDir: File = File(System.getProperty("bramble.assets") ?: "assets")
    val sprites: SpriteData by lazy { SpriteData.parse(File(assetsDir, "data/sprites.json").readText()) }

    fun world(w: WorldDef, seed: Long, c: CharacterDef = Characters.all[0], listener: WorldListener = object : WorldListener {}, tutorial: Boolean = false): GameWorld {
        val setup = RunSetup(c, w, seed, 3, 0f, 230f, 0f, false, tutorial)
        return GameWorld(sprites, setup, listener).also { it.viewWidth = 1280f }
    }
}
