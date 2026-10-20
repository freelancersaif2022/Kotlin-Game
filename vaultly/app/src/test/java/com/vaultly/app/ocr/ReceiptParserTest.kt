package com.vaultly.app.ocr

import com.vaultly.app.data.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {
    private val today = LocalDate.of(2026, 10, 20)
    private val parser = ReceiptParser(dayFirst = false)

    private val bestBuy = """
        BEST BUY #1187
        1450 Market St
        Tel 555-201-3311
        10/04/2026 14:22
        SONY WH-1000XM5 HEADPHONES   399.99
        USB-C CABLE 2M               19.99
        SUBTOTAL                    419.98
        SALES TAX 8.25%              34.65
        TOTAL                       454.63
        VISA ****4421               454.63
        Thank you for shopping!
    """.trimIndent()

    @Test fun readsBestBuyReceipt() {
        val r = parser.parse(bestBuy, today)
        assertEquals("Best Buy", r.store)
        assertEquals(LocalDate.of(2026, 10, 4), r.date)
        assertEquals(454.63, r.total!!, 0.001)
        assertEquals(15, r.returnDays)
        assertEquals(Category.ELECTRONICS, r.category)
        assertEquals("Sony WH-1000XM5 Headphones", r.suggestedTitle)
        assertEquals(2, r.items.size)
    }

    @Test fun prefersGrandTotalOverTotalTax() {
        val text = """
            Corner Hardware
            Date: 2026-09-12
            Cordless Drill 18V   129.00
            Total tax 10.32
            Grand Total 139.32
        """.trimIndent()
        val r = parser.parse(text, today)
        assertEquals("Corner Hardware", r.store)
        assertEquals(LocalDate.of(2026, 9, 12), r.date)
        assertEquals(139.32, r.total!!, 0.001)
        assertEquals(Category.TOOLS, r.category)
    }

    @Test fun europeanAmountsAndDayFirstDates() {
        val p = ReceiptParser(dayFirst = true)
        val text = """
            MediaMarkt Berlin
            03.08.2026
            Kaffeemaschine  1.249,00
            SUMME EUR 1.249,00
        """.trimIndent()
        val r = p.parse(text, today)
        assertEquals("MediaMarkt", r.store)
        assertEquals(LocalDate.of(2026, 8, 3), r.date)
        assertEquals("EUR", r.currency)
        assertEquals(1249.0, r.items.first().price, 0.001)
    }

    @Test fun textMonthDates() {
        assertEquals(listOf(LocalDate.of(2026, 3, 14)), parser.dateCandidates("Purchased Mar 14, 2026"))
        assertEquals(listOf(LocalDate.of(2026, 3, 14)), parser.dateCandidates("14 March 2026"))
    }

    @Test fun ignoresFutureDates() {
        val r = parser.parse("Shop\n12/30/2030\nItem thing 5.00", today)
        assertNull(r.date)
    }

    @Test fun unknownStoreUsesFirstNameLikeLine() {
        val r = parser.parse("RECEIPT\nGREEN LEAF CAFE\n123 Road\nLatte 4.50\nTotal 4.50", today)
        assertEquals("Green Leaf Cafe", r.store)
        assertEquals(4.50, r.total!!, 0.001)
    }

    @Test fun currencySymbols() {
        assertEquals("GBP", parser.findCurrency("Total £12.00"))
        assertEquals("INR", parser.findCurrency("Total ₹1,200.00"))
        assertEquals("USD", parser.findCurrency("TOTAL $5.00"))
    }

    @Test fun amountsParsing() {
        assertEquals(listOf(1234.56), parser.amounts("TOTAL 1,234.56"))
        assertEquals(listOf(12.5), parser.amounts("x 12,50"))
        assertTrue(parser.amounts("Phone 555-201-3311").isEmpty())
    }

    @Test fun joinsColumnsSplitIntoSeparateBlocks() {
        // Labels block, then a separate prices block, as ML Kit often returns them.
        val fragments = listOf(
            TextFragment("NORTHGATE ELECTRONICS", 100, 10, 50),
            TextFragment("SONY WH-1000XM5 HEADPHONES", 20, 200, 236),
            TextFragment("SUBTOTAL", 20, 260, 296),
            TextFragment("TOTAL", 20, 320, 358),
            TextFragment("399.99", 700, 204, 238),
            TextFragment("399.99", 700, 262, 297),
            TextFragment("${'$'}434.49", 690, 330, 366),
        )
        val rows = joinRows(fragments)
        assertEquals(
            listOf("NORTHGATE ELECTRONICS", "SONY WH-1000XM5 HEADPHONES  399.99", "SUBTOTAL  399.99", "TOTAL  ${'$'}434.49"),
            rows,
        )
        val r = parser.parse(rows.joinToString("\n"), today)
        assertEquals(434.49, r.total!!, 0.001)
        assertEquals("Sony WH-1000XM5 Headphones", r.suggestedTitle)
    }

    @Test fun fixesOcrLookAlikes() {
        assertEquals("TOTAL 390.95", parser.normalizeOcr("TOTAL 39O.9S"))
        assertEquals("Oct 14, 2026", parser.normalizeOcr("0ct 14, 2026"))
        assertEquals("Latte 4.50", parser.normalizeOcr("Latte 4 .50"))
        assertEquals("10/14/2026", parser.normalizeOcr("1O/14/2026"))
        // Words are left alone.
        assertEquals("SONY BOSE SOLO", parser.normalizeOcr("SONY BOSE SOLO"))
    }

    @Test fun deskewsTiltedPhotoIntoRows() {
        // Lines tilted by ~6°: the price on the right sits lower than its label on the left.
        val tilt = Math.toRadians(6.0)
        fun line(text: String, x: Float, y: Float, w: Float): OcrLine {
            val c = kotlin.math.cos(tilt).toFloat()
            val s = kotlin.math.sin(tilt).toFloat()
            fun p(dx: Float, dy: Float) = (x + dx * c - dy * s) to (y + dx * s + dy * c)
            return OcrLine(text, listOf(p(0f, 0f), p(w, 0f), p(w, 30f), p(0f, 30f)))
        }
        val rows = joinRows(
            deskew(
                listOf(
                    line("SUBTOTAL", 20f, 200f, 200f), line("399.99", 600f, 200f + 580f * 0.105f, 120f),
                    line("TOTAL", 20f, 260f, 120f), line("434.49", 600f, 260f + 580f * 0.105f, 120f),
                ),
            ),
        )
        assertEquals(listOf("SUBTOTAL  399.99", "TOTAL  434.49"), rows)
    }
}
