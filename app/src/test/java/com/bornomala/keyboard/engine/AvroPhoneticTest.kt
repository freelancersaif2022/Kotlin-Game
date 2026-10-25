package com.bornomala.keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Normalizer

class AvroPhoneticTest {

    private fun check(input: String, expected: String) =
        assertEquals(input, Normalizer.normalize(expected, Normalizer.Form.NFC), AvroPhonetic.parse(input))

    @Test fun basicWords() {
        check("ami", "আমি")
        check("amar", "আমার")
        check("bangla", "বাংলা")
        check("kemon", "কেমন")
        check("acho", "আছ")
        check("bhalo", "ভাল")
        check("bhalO", "ভালো")
        check("sOnar", "সোনার")
        check("ei", "এই")
        check("kia", "কিয়া")
    }

    @Test fun conjunctsAndPhola() {
        check("kintu", "কিন্তু")
        check("prothom", "প্রথম")
        check("korrmo", "কর্ম")
        check("bidyaloy", "বিদ্যালয়")
        check("shikkha", "শিক্ষা")
        check("krri", "কৃ")
        check("dwar", "দ্বার")
        check("bakko", "বাক্ক")
    }

    @Test fun signsAndDigits() {
        check("du:kho", "দুঃখ")
        check("ut``sob", "উৎসব")
        check("ca^d", "চাঁদ")
        check("1990", "১৯৯০")
        check("rri", "ঋ")
        check("k`i", "কই")
    }

    @Test fun caseFolding() {
        check("Ami", "আমি")
        check("AmI", "আমী")
        check("Tk", "টক")
    }
}
