package com.irremote.tv.ir

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/** Every button the app knows about. [id] is the token used in irdb.txt and in saved remotes. */
enum class Key(val label: String) {
    POWER("Power"), POWER_ON("Power On"), POWER_OFF("Power Off"),
    VOL_UP("Volume +"), VOL_DOWN("Volume −"), MUTE("Mute"),
    CH_UP("Channel +"), CH_DOWN("Channel −"),
    UP("Up"), DOWN("Down"), LEFT("Left"), RIGHT("Right"), OK("OK"),
    MENU("Menu"), HOME("Home"), BACK("Back"), EXIT("Exit"), INFO("Info"), INPUT("Input"),
    SETTINGS("Settings"), OPTIONS("Options"), GUIDE("Guide"),
    D1("1"), D2("2"), D3("3"), D4("4"), D5("5"), D6("6"), D7("7"), D8("8"), D9("9"), D0("0"),
    DASH("-"), PRE_CH("Last"),
    PLAY("Play"), PAUSE("Pause"), PLAY_PAUSE("Play/Pause"), STOP("Stop"),
    REWIND("Rewind"), FFWD("Forward"), PREV("Previous"), NEXT("Next"), RECORD("Record"),
    RED("Red"), GREEN("Green"), YELLOW("Yellow"), BLUE("Blue"),
    NETFLIX("Netflix"), YOUTUBE("YouTube"), PRIME("Prime Video"), DISNEY("Disney+"), APPS("Apps"),
    SLEEP("Sleep"), SUBTITLE("Subtitle"), ASPECT("Aspect"), TEXT("Text"), LIST("Ch. List"),
    FAV("Favorite"), AUDIO("Audio"), PICTURE("Picture"), SOUND("Sound");

    /** Keys tried, in order, when this key is missing from a code set. */
    val fallbacks: List<Key>
        get() = when (this) {
            POWER -> listOf(POWER_ON)
            BACK -> listOf(EXIT)
            EXIT -> listOf(BACK)
            OK -> emptyList()
            PLAY -> listOf(PLAY_PAUSE)
            PAUSE -> listOf(PLAY_PAUSE)
            PLAY_PAUSE -> listOf(PLAY, PAUSE)
            HOME -> listOf(MENU)
            SETTINGS -> listOf(MENU)
            OPTIONS -> listOf(SETTINGS)
            else -> emptyList()
        }

    companion object {
        private val byName = values().associateBy { it.name }
        fun of(name: String): Key? = byName[name]
    }
}

/** One IR command: either a decoded protocol frame or raw mark/space timings. */
sealed class IrCode {
    data class Parsed(val protocol: String, val address: Long, val command: Long) : IrCode()
    class Raw(val frequency: Int, val timings: IntArray) : IrCode()

    companion object {
        fun parse(s: String): IrCode? = try {
            if (s.length > 1 && s[0] == 'R' && s[1].isDigit()) {
                val colon = s.indexOf(':')
                val freq = s.substring(1, colon).toInt()
                val parts = s.substring(colon + 1).split('.')
                Raw(freq, IntArray(parts.size) { parts[it].toInt() })
            } else {
                val p = s.split(':')
                Parsed(p[0], p[1].toLong(16), p[2].toLong(16))
            }
        } catch (e: RuntimeException) {
            null
        }
    }
}

/** A complete button map for one family of TVs. [body] is the serialized form stored with saved remotes. */
class CodeSet(val brand: String, val label: String, val body: String, val popularity: Int) {
    val codes: Map<Key, String> by lazy {
        val m = LinkedHashMap<Key, String>()
        for (token in body.split(' ')) {
            val eq = token.indexOf('=')
            if (eq <= 0) continue
            val key = Key.of(token.substring(0, eq)) ?: continue
            m[key] = token.substring(eq + 1)
        }
        m
    }

    fun signalFor(key: Key): String? = codes[key] ?: key.fallbacks.firstNotNullOfOrNull { codes[it] }

    fun has(key: Key): Boolean = signalFor(key) != null

    val powerSignal: String? get() = signalFor(Key.POWER)
}

class Brand(val name: String, val sets: List<CodeSet>, val universal: Boolean = false)

/**
 * The bundled code library (assets/irdb.txt, generated from Flipper-IRDB) plus brands
 * that have no dedicated codes and therefore search through every code in the library.
 */
class IrDatabase private constructor(val brands: List<Brand>, val allSets: List<CodeSet>) {

    /** Distinct POWER codes from every brand, most common first. Used by Auto Search. */
    val powerSearchList: List<CodeSet> by lazy {
        val seen = HashSet<String>()
        allSets.filter { s -> s.powerSignal?.let { seen.add(it) } ?: false }
    }

    fun brand(name: String): Brand? = brands.firstOrNull { it.name.equals(name, ignoreCase = true) }

    companion object {
        @Volatile private var instance: IrDatabase? = null

        val POPULAR = listOf(
            "Samsung", "LG", "Sony", "TCL", "Hisense", "Xiaomi", "Panasonic", "Philips",
            "Sharp", "Toshiba", "Vizio", "Haier",
        )

        /** Brands sold widely that are not in the code library; they use the universal search list. */
        private val EXTRA_BRANDS = listOf(
            "Aiwa", "BPL", "Changhong", "Coocaa", "Ecostar", "Hyundai", "Infinix", "Intex", "iFFALCON",
            "Itel", "Jamuna", "Kevin", "Kodak", "Konka", "Lloyd", "Marcel", "Micromax", "Minister",
            "Mitsubishi", "Motorola", "Nokia", "Onida", "OnePlus", "Polaroid", "Realme", "Sansui",
            "Singer", "Skyworth", "Sinotec", "Tecno", "Vestel", "Videocon", "Vision", "Vu", "Walton",
            "Westpoint", "Generic / Other",
        )

        private val DISPLAY_NAMES = mapOf(
            "ContinentalEdison" to "Continental Edison",
            "EdenWood" to "Edenwood",
            "EssentielB" to "Essentiel b",
            "FINLUX" to "Finlux",
            "DYON" to "Dyon",
            "Ffalcon" to "FFalcon",
        )

        fun get(context: Context): IrDatabase {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                return load(context.applicationContext).also { instance = it }
            }
        }

        private fun load(context: Context): IrDatabase {
            val brands = ArrayList<Brand>()
            var name: String? = null
            var sets = ArrayList<CodeSet>()
            fun flush() {
                val n = name ?: return
                if (sets.isNotEmpty()) brands.add(Brand(n, sets))
            }
            BufferedReader(InputStreamReader(context.assets.open("irdb.txt"), Charsets.UTF_8), 1 shl 16).use { r ->
                while (true) {
                    val line = r.readLine() ?: break
                    if (line.startsWith("B\t")) {
                        flush()
                        name = line.substring(2).let { DISPLAY_NAMES[it] ?: it }
                        sets = ArrayList()
                    } else if (line.startsWith("S\t")) {
                        val t1 = line.indexOf('\t', 2)
                        if (t1 < 0) continue
                        val label = line.substring(2, t1)
                        val body = line.substring(t1 + 1)
                        val popularity = label.count { it == ',' } + 1
                        sets.add(CodeSet(name ?: "", label, body, popularity))
                    }
                }
            }
            flush()

            // Universal list: every set from every brand, most widely shared first.
            val all = brands.flatMap { it.sets }.sortedByDescending { it.popularity }
            val known = brands.map { it.name.lowercase() }.toHashSet()
            for (extra in EXTRA_BRANDS) {
                if (extra.lowercase() !in known) brands.add(Brand(extra, all, universal = true))
            }
            brands.sortBy { it.name.lowercase() }
            return IrDatabase(brands, all)
        }
    }
}
