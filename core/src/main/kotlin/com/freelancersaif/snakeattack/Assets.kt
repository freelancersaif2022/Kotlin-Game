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
    lateinit var uiAtlas: TextureAtlas
    lateinit var outfitAtlas: TextureAtlas
    lateinit var fontBig: BitmapFont
    lateinit var fontSmall: BitmapFont
    lateinit var fontPlain: BitmapFont
    lateinit var panel: NinePatch
    lateinit var boardPanel: NinePatch
    lateinit var pill: NinePatch
    lateinit var white: TextureRegion
    lateinit var framePanel: NinePatch

    // per-colour regions cached up front so the render loop never builds strings
    private lateinit var blocks: Array<TextureRegion>
    private lateinit var cannons: Array<TextureRegion>
    private lateinit var segments: Array<TextureRegion>

    private val regions = HashMap<String, TextureRegion>()
    private val sounds = HashMap<String, Sound>()
    private val music = HashMap<String, Music>()
    private val textures = HashMap<String, Texture>()
    private var currentMusic: Music? = null
    private var currentMusicName: String? = null

    fun load() {
        atlas = TextureAtlas(Gdx.files.internal("sprites.atlas"))
        uiAtlas = TextureAtlas(Gdx.files.internal("ui.atlas"))
        outfitAtlas = TextureAtlas(Gdx.files.internal("outfits.atlas"))
        fontBig = loadFont("fonts/font_big.fnt")
        fontSmall = loadFont("fonts/font_small.fnt")
        fontPlain = loadFont("fonts/font_plain.fnt")
        white = r("white").let { TextureRegion(it, 4, 4, 4, 4) }
        panel = NinePatch(r("panel"), 42, 42, 42, 42)
        boardPanel = NinePatch(r("board_panel"), 40, 40, 40, 40)
        pill = NinePatch(r("pill"), 32, 32, 30, 30)
        framePanel = NinePatch(r("frame_panel"), 48, 48, 48, 48)
        blocks = Array(GemColor.entries.size) { r("block_${GemColor.entries[it].key}") }
        cannons = Array(GemColor.entries.size) { r("cannon_${GemColor.entries[it].key}") }
        segments = Array(GemColor.entries.size) { r("segment_${GemColor.entries[it].key}") }

        for (name in listOf(
            "tap", "whoosh", "bonk", "place", "shoot", "hit", "explode", "coin", "win", "lose",
            "roar", "bite", "click", "unlock", "error", "combo", "swoosh_in", "scream",
            "star1", "star2", "star3", "bomb", "freeze", "freeze_end", "rainbow", "shuffle", "armor",
            "armor_break", "boss_roar", "heart_lose", "spin_tick", "reward", "chest_open", "purchase",
            "achievement", "page", "node_pop", "hammer", "no_lives",
        )) {
            sounds[name] = Gdx.audio.newSound(Gdx.files.internal("sounds/$name.ogg"))
        }
        for (name in listOf("game", "menu", "map", "boss")) {
            music[name] = Gdx.audio.newMusic(Gdx.files.internal("music/$name.ogg")).apply { isLooping = true }
        }
    }

    /** Fonts get mip-mapped textures so small text stays crisp instead of shimmering. */
    private fun loadFont(path: String): BitmapFont {
        val png = Gdx.files.internal(path.replace(".fnt", ".png"))
        val tex = Texture(png, true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
        return BitmapFont(Gdx.files.internal(path), TextureRegion(tex), false).apply {
            setUseIntegerPositions(false)
        }
    }

    /** Pixel size each bitmap font was baked at. */
    fun baseSize(font: BitmapFont) = when {
        font === fontBig -> 84f
        font === fontPlain -> 52f
        else -> 46f
    }

    fun r(name: String): TextureRegion = regions.getOrPut(name) {
        atlas.findRegion(name) ?: uiAtlas.findRegion(name) ?: outfitAtlas.findRegion(name) ?: error("Missing sprite '$name'")
    }

    /**
     * Large standalone images (backgrounds, map tiles, story panels) are loaded on
     * demand and can be released with [unload] so only what a screen needs stays in memory.
     */
    fun texture(path: String): Texture = textures.getOrPut(path) {
        Texture(Gdx.files.internal(path), false).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    }

    fun unload(path: String) {
        textures.remove(path)?.dispose()
    }

    /** Keeps only the given large textures loaded. */
    fun keepOnly(paths: Collection<String>) {
        textures.keys.filter { it !in paths }.forEach { unload(it) }
    }

    fun block(c: GemColor) = blocks[c.ordinal]
    fun cannon(c: GemColor) = cannons[c.ordinal]
    fun segment(c: GemColor) = segments[c.ordinal]

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
        m.volume = when (name()) {
            "menu", "map" -> 0.5f
            "boss" -> 0.45f
            else -> 0.4f
        }
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
        uiAtlas.dispose()
        outfitAtlas.dispose()
        textures.values.forEach { it.dispose() }
        textures.clear()
        HueShader.dispose()
        fontBig.region.texture.dispose()
        fontSmall.region.texture.dispose()
        fontPlain.region.texture.dispose()
        fontPlain.dispose()
        fontBig.dispose()
        fontSmall.dispose()
        sounds.values.forEach { it.dispose() }
        music.values.forEach { it.dispose() }
    }
}
