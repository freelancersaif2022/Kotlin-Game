package com.freelancersaif.snakeattack

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils

/** The eight block / cannon / segment colours, in the order used by levels. */
enum class GemColor(val key: String, val tint: Color) {
    RED("red", Color.valueOf("ff4a4a")),
    BLUE("blue", Color.valueOf("3d8bff")),
    GREEN("green", Color.valueOf("52d63a")),
    YELLOW("yellow", Color.valueOf("ffd43b")),
    PURPLE("purple", Color.valueOf("a855f7")),
    PINK("pink", Color.valueOf("ff5fb7")),
    TEAL("teal", Color.valueOf("2ee6d6")),
    ORANGE("orange", Color.valueOf("ff9a1f")),
}

object Assets {
    lateinit var atlas: TextureAtlas
    lateinit var bg: Texture
    lateinit var fontBig: BitmapFont
    lateinit var fontSmall: BitmapFont
    lateinit var panel: NinePatch
    lateinit var boardPanel: NinePatch
    lateinit var pill: NinePatch
    lateinit var white: TextureRegion

    private val regions = HashMap<String, TextureRegion>()
    private val sounds = HashMap<String, Sound>()
    private val music = HashMap<String, Music>()
    private var currentMusic: Music? = null
    private var currentMusicName: String? = null

    fun load() {
        atlas = TextureAtlas(Gdx.files.internal("sprites.atlas"))
        bg = Texture(Gdx.files.internal("bg_level.jpg"), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
        fontBig = loadFont("fonts/font_big.fnt")
        fontSmall = loadFont("fonts/font_small.fnt")
        white = r("white").let { TextureRegion(it, 4, 4, 4, 4) }
        panel = NinePatch(r("panel"), 42, 42, 42, 42)
        boardPanel = NinePatch(r("board_panel"), 40, 40, 40, 40)
        pill = NinePatch(r("pill"), 32, 32, 30, 30)

        for (name in listOf(
            "tap", "whoosh", "bonk", "place", "shoot", "hit", "explode", "coin", "win", "lose",
            "roar", "bite", "click", "unlock", "error", "combo", "swoosh_in", "scream",
            "star1", "star2", "star3",
        )) {
            sounds[name] = Gdx.audio.newSound(Gdx.files.internal("sounds/$name.ogg"))
        }
        for (name in listOf("game", "menu")) {
            music[name] = Gdx.audio.newMusic(Gdx.files.internal("music/$name.ogg")).apply { isLooping = true }
        }
    }

    private fun loadFont(path: String) = BitmapFont(Gdx.files.internal(path)).apply {
        regions.forEach { it.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        setUseIntegerPositions(false)
    }

    fun r(name: String): TextureRegion = regions.getOrPut(name) {
        atlas.findRegion(name) ?: error("Missing sprite '$name'")
    }

    fun block(c: GemColor) = r("block_${c.key}")
    fun cannon(c: GemColor) = r("cannon_${c.key}")
    fun segment(c: GemColor) = r("segment_${c.key}")

    /** Plays a sound with a little random pitch variation so repeats never sound robotic. */
    fun sfx(name: String, volume: Float = 1f, pitch: Float = 1f, vary: Float = 0.06f) {
        if (!Save.sound) return
        val p = pitch * (1f + MathUtils.random(-vary, vary))
        sounds[name]?.play(volume * 0.9f, p, 0f)
    }

    fun playMusic(name: String) {
        if (currentMusicName == name && currentMusic?.isPlaying == true) return
        currentMusic?.stop()
        currentMusicName = name
        currentMusic = music[name]
        refreshMusic()
    }

    fun refreshMusic() {
        val m = currentMusic ?: return
        m.volume = if (name() == "menu") 0.55f else 0.42f
        if (Save.music) {
            if (!m.isPlaying) m.play()
        } else {
            m.pause()
        }
    }

    private fun name() = currentMusicName

    fun pauseMusic() = currentMusic?.pause()

    fun dispose() {
        atlas.dispose()
        bg.dispose()
        fontBig.dispose()
        fontSmall.dispose()
        sounds.values.forEach { it.dispose() }
        music.values.forEach { it.dispose() }
    }
}
