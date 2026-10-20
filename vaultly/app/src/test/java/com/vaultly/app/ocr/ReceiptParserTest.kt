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
        assertEquals("SONY WH-1000XM5 HEADPHONES".lowercase(), r.suggestedTitle!!.lowercase())
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
}
