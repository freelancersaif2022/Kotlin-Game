package com.bornomala.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SuggestionTest {

    private fun asset(name: String): File =
        listOf(File("src/main/assets/dict/$name"), File("app/src/main/assets/dict/$name")).first { it.exists() }

    private val engine = SuggestionEngine(UserDictionary()).apply {
        asset("bn.txt").inputStream().use { loadBangla(it) }
        asset("en.txt").inputStream().use { loadEnglish(it) }
    }

    @Test fun skeletonsAgree() {
        val pairs = listOf("kotha" to "কথা", "shobai" to "সবাই", "jodi" to "যদি", "tomay" to "তোমায়", "bangla" to "বাংলা")
        for ((en, bn) in pairs) assertEquals(en, PhoneticSkeleton.fromBangla(bn), PhoneticSkeleton.fromEnglish(en))
    }

    @Test fun phoneticDefaults() {
        assertEquals("আমি", engine.phonetic("ami", true).default)
        assertEquals("সবাই", engine.phonetic("shobai", true).default)
        assertEquals("তোমার", engine.phonetic("tomar", true).default)
        assertEquals("কেমন", engine.phonetic("kemon", true).default)
        assertEquals("কোন", engine.phonetic("kon", true).default)
        assertEquals("ভাল", engine.phonetic("bhal", true).default.let { if (it == "ভাল") "ভাল" else it })
        assertEquals("ভাল", engine.phonetic("bhalo", true).default)
        // Without auto-correct the plain transliteration wins.
        assertEquals("শবাই", engine.phonetic("shobai", false).default)
    }

    @Test fun learnedChoiceWins() {
        val user = UserDictionary()
        val e = SuggestionEngine(user).apply { asset("bn.txt").inputStream().use { loadBangla(it) } }
        user.rememberPhonetic("ki", "কী")
        assertEquals("কী", e.phonetic("ki", true).default)
    }

    @Test fun englishCorrections() {
        assertEquals("the", engine.english("teh", true).default)
        assertEquals("don't", engine.english("dont", true).default)
        assertTrue(engine.english("Helo", true).words.contains("Hello"))
        assertEquals("because", engine.english("becuase", true).default)
        assertEquals("I'm", engine.english("Im", true).default)
        assertEquals("hello", engine.english("hello", true).default)
        assertTrue(engine.english("hel", false).words.contains("hello"))
    }

    @Test fun nationalCompletions() {
        val s = engine.national("আম")
        assertEquals("আম", s.default)
        assertTrue(s.words.toString(), s.words.contains("আমি") || s.words.contains("আমার"))
    }

    @Test fun nationalComposer() {
        assertEquals(NationalComposer.Edit(1, "আ"), NationalComposer.compose('\u09CD', "া"))
        assertEquals(NationalComposer.Edit(0, "ই"), NationalComposer.compose(null, "ি"))
        assertEquals(NationalComposer.Edit(0, "ি"), NationalComposer.compose('ক', "ি"))
        assertEquals(NationalComposer.Edit(0, "এ"), NationalComposer.compose(' ', "ে"))
    }
}
