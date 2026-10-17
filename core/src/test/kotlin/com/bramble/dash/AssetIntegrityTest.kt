package com.bramble.dash

import com.badlogic.gdx.utils.JsonReader
import com.bramble.dash.assets.RegionMeta
import com.bramble.dash.audio.MusicTrack
import com.bramble.dash.audio.Sfx
import com.bramble.dash.config.Bosses
import com.bramble.dash.config.Characters
import com.bramble.dash.config.CollectibleType
import com.bramble.dash.config.Enemies
import com.bramble.dash.config.Obstacles
import com.bramble.dash.config.PowerUpType
import com.bramble.dash.config.Worlds
import com.bramble.dash.render.pivotBottom
import com.bramble.dash.render.pivotLeft
import com.bramble.dash.game.collisionRect
import com.badlogic.gdx.math.Rectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import javax.imageio.ImageIO

class AssetIntegrityTest {
    private val dir = TestSupport.assetsDir
    private val sprites = TestSupport.sprites

    /** Minimal reader for the LibGDX atlas text format: region name -> (page, x, y, w, h). */
    private fun readAtlas(f: File): Map<String, IntArray> {
        val out = HashMap<String, IntArray>()
        var page = -1
        var name: String? = null
        var xy: IntArray? = null
        val pages = ArrayList<String>()
        for (raw in f.readLines()) {
            val line = raw.trimEnd()
            if (line.isEmpty()) { name = null; continue }
            if (!line.startsWith(" ") && !line.contains(":")) {
                if (line.endsWith(".png")) { pages += line; page = pages.size - 1; name = null } else { name = line; xy = null }
                continue
            }
            val t = line.trim()
            if (name != null && t.startsWith("xy:")) xy = t.substringAfter(':').split(',').map { it.trim().toInt() }.toIntArray()
            if (name != null && t.startsWith("size:") && xy != null) {
                val sz = t.substringAfter(':').split(',').map { it.trim().toInt() }
                out[name] = intArrayOf(page, xy[0], xy[1], sz[0], sz[1])
            }
        }
        out["__pages__"] = IntArray(pages.size)
        pagesByAtlas[f.name] = pages
        return out
    }
    private val pagesByAtlas = HashMap<String, List<String>>()

    @Test
    fun everyRegionResolvesInsideADecodablePage() {
        var checked = 0
        for ((group, path) in sprites.atlases) {
            val atlasFile = File(dir, path)
            assertTrue("missing $path", atlasFile.isFile)
            val regions = readAtlas(atlasFile)
            val pages = pagesByAtlas.getValue(atlasFile.name).map { p ->
                val img = ImageIO.read(File(atlasFile.parentFile, p))
                assertTrue("page $p does not decode", img != null)
                img!!
            }
            for ((name, r) in regions) {
                if (name == "__pages__") continue
                assertEquals("$name listed in wrong atlas", group, sprites.groupOf(name))
                val img = pages[r[0]]
                assertTrue("$name outside page", r[1] >= 0 && r[2] >= 0 && r[1] + r[3] <= img.width && r[2] + r[4] <= img.height)
                checked++
            }
        }
        // every metadata region is packed somewhere
        val packed = sprites.atlases.values.flatMap { readAtlas(File(dir, it)).keys }.toSet()
        val missing = sprites.regionNames().filter { it !in packed }
        assertTrue("regions missing from atlases: ${missing.take(5)}", missing.isEmpty())
        assertEquals(1556, sprites.regionNames().size)
        assertTrue(checked >= 1556)
    }

    @Test
    fun animationsKeepTheSourcePackOrder() {
        val zip = File(dir.parentFile, "assets.zip")
        if (!zip.isFile) return
        ZipFile(zip).use { z ->
            val entry = z.entries().asSequence().first { it.name.endsWith("metadata/animations.json") }
            val src = JsonReader().parse(z.getInputStream(entry).reader().readText())
            var a = src.child
            var n = 0
            while (a != null) {
                val ours = sprites.anim(a.name)
                assertEquals("frame order differs for ${a.name}", a.get("frames").asStringArray().toList(), ours.frames)
                assertEquals(a.getFloat("frameDuration"), ours.frameDuration, 1e-5f)
                assertEquals(a.getBoolean("loop"), ours.loop)
                a = a.next; n++
            }
            assertEquals(210, n)
        }
    }

    @Test
    fun pivotAndCollisionConversion() {
        // canvas 192x256, pivot (96, 23), trimmed rect offset (40, 20) size 100x180
        val m = RegionMeta(192f, 256f, 96f, 23f, 54f, 24f, 84f, 174f, true)
        assertEquals(500f - 96f + 40f, pivotLeft(m, 40f, 100f, 500f, 1f, false), 1e-4f)
        assertEquals(100f - 23f + 20f, pivotBottom(m, 20f, 100f, 1f), 1e-4f)
        // mirrored: the trimmed rect's right edge mirrors its left edge around the pivot
        val left = pivotLeft(m, 40f, 100f, 500f, 1f, true)
        assertEquals(500f + (96f - 40f), left + 100f, 1e-4f)
        // collision: bottom-left local px -> world, pivot at feet
        val r = collisionRect(m, 500f, 100f, 1f, 1f, false, Rectangle())
        assertEquals(500f - 96f + 54f, r.x, 1e-4f)
        assertEquals(100f + 1f, r.y, 1e-4f)
        val f = collisionRect(m, 500f, 100f, 1f, 1f, true, Rectangle())
        assertEquals(500f + 96f - 54f - 84f, f.x, 1e-4f)
    }

    @Test
    fun everyNamedContentHasItsArt() {
        val charAnims = listOf("idle", "running", "jumping", "double_jump", "sliding", "falling", "landing", "taking_damage", "death", "victory")
        for (c in Characters.all) for (a in charAnims) assertTrue("characters/${c.id}/$a", sprites.hasAnim("characters/${c.id}/$a"))
        for (e in Enemies.all) for (a in listOf("idle", "movement", "attack", "damage", "death")) assertTrue(sprites.hasAnim("enemies/${e.id}/$a"))
        for (b in Bosses.all) for (a in listOf("idle", "movement", "primary_attack", "special_attack", "damage", "death")) assertTrue(sprites.hasAnim("bosses/${b.id}/$a"))
        for (o in Obstacles.all) assertTrue(o.id, sprites.hasAnim("obstacles/${o.id}"))
        for (c in CollectibleType.values()) assertTrue(c.anim, sprites.hasAnim(c.anim))
        for (p in PowerUpType.values()) {
            for (r in listOf("icon", "hud", "pickup")) assertTrue(sprites.hasRegion("powerups/${p.id}/$r"))
            for (a in listOf("activation", "active")) assertTrue(sprites.hasAnim("powerups/${p.id}/$a"))
        }
        for (w in Worlds.all) for (r in listOf("layer_0", "layer_1", "layer_2", "layer_3", "ground", "platform", "decoration", "lighting")) assertTrue(sprites.hasRegion("environments/${w.id}/$r"))
    }

    @Test
    fun everyCatalogEntryIsReachableInGameplay() {
        assertEquals(Obstacles.all.map { it.id }.toSet(), Worlds.all.flatMap { it.obstacles }.toSet())
        assertEquals(Enemies.all.map { it.id }.toSet(), Worlds.all.flatMap { it.enemies }.toSet())
        assertEquals(Bosses.all.map { it.id }.toSet(), Worlds.all.flatMap { it.bosses }.toSet())
        assertEquals(5, Characters.all.size); assertEquals(8, Worlds.all.size); assertEquals(10, Enemies.all.size)
        assertEquals(30, Obstacles.all.size); assertEquals(10, CollectibleType.values().size); assertEquals(10, PowerUpType.values().size)
    }

    /** Every region/animation literal used by the code must exist in the packed data. */
    @Test
    fun codeReferencesResolve() {
        val src = File(dir.parentFile, "core/src/main/kotlin")
        val literal = Regex("\"((?:ui|effects|powerups|collectibles|previews|environments|obstacles|characters|enemies|bosses)/[a-z0-9_/]+)\"")
        val bad = ArrayList<String>()
        src.walkTopDown().filter { it.extension == "kt" }.forEach { f ->
            for (m in literal.findAll(f.readText())) {
                val name = m.groupValues[1]
                val ok = sprites.hasRegion(name) || sprites.hasAnim(name) || name.startsWith("previews/") ||
                    name == "ui/components/panel9" || sprites.hasRegion("$name/normal")
                if (!ok) bad += "${f.name}: $name"
            }
        }
        assertTrue("unresolved references: $bad", bad.isEmpty())
        for (w in Worlds.all) assertTrue(File(dir, "atlas/previews.atlas").readText().contains("previews/${w.id}"))
        assertTrue(File(dir, "atlas/ui.atlas").readText().contains("ui/components/panel9\n  rotate: false"))
        assertTrue(Regex("ui/components/panel9\\n(  .*\\n)*?  split: 22, 21, 22, 21").containsMatchIn(File(dir, "atlas/ui.atlas").readText()))
    }

    @Test
    fun audioFilesExistWithExpectedFormats() {
        for (s in Sfx.values()) {
            val f = File(dir, s.path)
            assertTrue("missing ${s.path}", f.isFile)
            val b = f.readBytes()
            assertEquals("RIFF", String(b, 0, 4)); assertEquals("WAVE", String(b, 8, 4))
            val channels = (b[22].toInt() and 0xff); val rate = (b[24].toInt() and 0xff) or ((b[25].toInt() and 0xff) shl 8)
            val bits = b[34].toInt()
            assertEquals(1, channels); assertEquals(22050, rate); assertEquals(16, bits)
            assertTrue("${s.file} is suspiciously short", b.size > 1000)
        }
        for (m in MusicTrack.values()) {
            val f = File(dir, m.path)
            assertTrue("missing ${m.path}", f.isFile)
            assertEquals("OggS", String(f.readBytes(), 0, 4))
            assertTrue(String(f.readBytes().copyOfRange(0, 200), Charsets.ISO_8859_1).contains("vorbis"))
        }
    }

    @Test
    fun fontsAndIcons() {
        for (n in listOf("title", "body", "small")) {
            val fnt = File(dir, "fonts/$n.fnt").readText()
            assertTrue(fnt.contains("char id=65 ")); assertTrue(fnt.contains("char id=8212 "))
            assertTrue(ImageIO.read(File(dir, "fonts/$n.png")) != null)
        }
    }
}
