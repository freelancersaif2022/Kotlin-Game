package com.bramble.dash.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ObjectMap
import com.bramble.dash.audio.MusicTrack
import com.bramble.dash.audio.Sfx
import com.bramble.dash.config.Bosses
import com.bramble.dash.config.Characters
import com.bramble.dash.config.Enemies
import com.bramble.dash.config.Worlds

/**
 * Owns the AssetManager. Atlases are grouped (see tools/asset_pipeline.py) so only the active
 * character, world, enemy set and upcoming boss are resident during a run.
 */
class GameAssets : Disposable {
    val manager = AssetManager()
    lateinit var sprites: SpriteData
        private set
    private val regionCache = ObjectMap<String, AtlasRegion>()
    private val animCache = ObjectMap<String, Anim>()
    private val loadedGroups = HashSet<String>()
    lateinit var white: TextureRegion
        private set
    private var whiteTexture: Texture? = null

    /** Groups that menus and every run need. */
    val coreGroups = listOf("ui", "ui_panels", "items", "obstacles", "effects", "previews")

    fun loadSpriteData() {
        sprites = SpriteData.parse(Gdx.files.internal("data/sprites.json").readString("UTF-8"))
        val pm = Pixmap(4, 4, Pixmap.Format.RGBA8888)
        pm.setColor(1f, 1f, 1f, 1f)
        pm.fill()
        whiteTexture = Texture(pm)
        pm.dispose()
        white = TextureRegion(whiteTexture, 1, 1, 2, 2)
    }

    fun atlasPath(group: String) = sprites.atlases[group] ?: error("Unknown atlas group $group")

    fun queueGroup(group: String) {
        val path = atlasPath(group)
        if (!manager.isLoaded(path)) manager.load(path, TextureAtlas::class.java)
    }

    fun queueCore() {
        coreGroups.forEach(::queueGroup)
        for (f in listOf("title", "body", "small")) manager.load("fonts/$f.fnt", BitmapFont::class.java)
        Sfx.values().forEach { manager.load(it.path, Sound::class.java) }
        // Menu world backdrop: the first world is always unlocked
        queueGroup("world_jungle")
        queueGroup("char_max")
    }

    /** Groups a run needs: chosen runner, world art, its enemies and its bosses. */
    fun runGroups(characterId: String, worldId: String): Set<String> {
        val w = Worlds.byId(worldId)
        val set = LinkedHashSet<String>()
        set += coreGroups
        set += "char_" + Characters.byId(characterId).id
        set += "world_" + w.id
        w.enemies.forEach { set += "enemy_" + Enemies.byId(it).id }
        w.bosses.forEach { set += "boss_" + Bosses.byId(it).id }
        return set
    }

    /** Queue everything required for [keep] and unload run atlases that are no longer needed. */
    fun prepareGroups(keep: Set<String>) {
        val unload = loadedGroups.filter { it !in keep && it !in coreGroups }
        for (g in unload) {
            val path = atlasPath(g)
            if (manager.isLoaded(path)) manager.unload(path)
            loadedGroups.remove(g)
            invalidate(g)
        }
        keep.forEach(::queueGroup)
    }

    /** Call while loading; registers newly finished atlases. Returns true when done. */
    fun update(millis: Int = 16): Boolean {
        val done = manager.update(millis)
        for (g in sprites.atlases.keys) {
            if (g !in loadedGroups && manager.isLoaded(atlasPath(g))) loadedGroups += g
        }
        return done
    }

    val progress get() = manager.progress

    fun isGroupLoaded(group: String) = group in loadedGroups

    private fun invalidate(group: String) {
        val drop = regionCache.keys().filter { sprites.groupOf(it) == group }
        drop.forEach { regionCache.remove(it) }
        val dropA = animCache.keys().filter { sprites.groupOf(sprites.anim(it.substringBefore('@')).frames[0]) == group }
        dropA.forEach { animCache.remove(it) }
    }

    fun region(name: String): AtlasRegion {
        regionCache.get(name)?.let { return it }
        val atlas = manager.get(atlasPath(sprites.groupOf(name)), TextureAtlas::class.java)
        val r = atlas.findRegion(name) ?: error("Region '$name' missing from atlas ${sprites.groupOf(name)}")
        regionCache.put(name, r)
        return r
    }

    fun anim(name: String, speed: Float = 1f): Anim {
        val key = if (speed == 1f) name else "$name@$speed"
        animCache.get(key)?.let { return it }
        val meta = sprites.anim(name)
        val frames = Array(meta.frames.size) { region(meta.frames[it]) }
        val fm = Array(meta.frames.size) { sprites.region(meta.frames[it]) }
        val a = Anim(meta, frames, fm, speed)
        animCache.put(key, a)
        return a
    }

    fun font(name: String): BitmapFont = manager.get("fonts/$name.fnt", BitmapFont::class.java)

    fun sound(sfx: Sfx): Sound = manager.get(sfx.path, Sound::class.java)

    /** Music is streamed and owned by the caller (AudioService), not the AssetManager. */
    fun newMusic(track: MusicTrack): Music = Gdx.audio.newMusic(Gdx.files.internal(track.path))

    override fun dispose() {
        manager.dispose()
        whiteTexture?.dispose()
    }
}
