package com.vaultly.app.ocr

import com.vaultly.app.data.Category
import java.time.LocalDate
import java.time.Month

/** What could be read from a receipt. Every field is a suggestion the user confirms. */
data class ParsedReceipt(
    val store: String? = null,
    val date: LocalDate? = null,
    val total: Double? = null,
    val currency: String? = null,
    val items: List<LineItem> = emptyList(),
    val category: Category? = null,
    val returnDays: Int? = null,
) {
    /** Best guess for the item title: the priciest line item. */
    val suggestedTitle: String? get() = items.maxByOrNull { it.price }?.name
}

data class LineItem(val name: String, val price: Double)

/** A piece of recognised text and where it sits on the image. */
data class TextFragment(val text: String, val left: Int, val top: Int, val bottom: Int) {
    val centerY: Float get() = (top + bottom) / 2f
    val height: Int get() = bottom - top
}

/**
 * OCR engines often return a receipt's left column (labels) and right column (prices) as
 * separate blocks. This puts fragments that share a baseline back on one line, left to right.
 */
fun joinRows(fragments: List<TextFragment>): List<String> {
    if (fragments.isEmpty()) return emptyList()
    val medianHeight = fragments.map { it.height }.sorted()[fragments.size / 2].coerceAtLeast(1)
    val rows = mutableListOf<MutableList<TextFragment>>()
    fragments.sortedBy { it.centerY }.forEach { f ->
        val row = rows.lastOrNull()
        if (row != null && kotlin.math.abs(row.map { it.centerY }.average() - f.centerY) < medianHeight * 0.65) {
            row += f
        } else {
            rows += mutableListOf(f)
        }
    }
    return rows.map { row -> row.sortedBy { it.left }.joinToString("  ") { it.text.trim() } }
}

/**
 * Turns raw OCR text into receipt fields with plain rules (no network, no model).
 * [dayFirst] decides ambiguous dates like 03/04/2025 (true = 3 April).
 */
class ReceiptParser(private val dayFirst: Boolean = true) {

    fun parse(text: String, today: LocalDate = LocalDate.now()): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val store = findStore(lines)
        val items = findItems(lines)
        val category = guessCategory(lines, store)
        return ParsedReceipt(
            store = store,
            date = findDate(lines, today),
            total = findTotal(lines),
            currency = findCurrency(text),
            items = items,
            category = category,
            returnDays = store?.let { returnPolicyFor(it) },
        )
    }

    // ---- Store ----

    internal fun findStore(lines: List<String>): String? {
        val head = lines.take(8)
        for (line in head) {
            val lower = line.lowercase()
            KNOWN_STORES.firstOrNull { (key, _) -> lower.contains(key) }?.let { return it.second }
        }
        return head.firstOrNull { line ->
            val letters = line.count { it.isLetter() }
            val digits = line.count { it.isDigit() }
            val lower = line.lowercase()
            letters >= 3 && digits <= 2 && letters >= line.length / 2 &&
                STORE_NOISE.none { lower.contains(it) } && !lower.startsWith("www") && !lower.contains("@")
        }?.let { titleCase(it.trim().trimEnd('.', ',', ':', '-')) }
    }

    // ---- Date ----

    internal fun findDate(lines: List<String>, today: LocalDate): LocalDate? {
        val candidates = mutableListOf<Pair<LocalDate, Boolean>>()
        for (line in lines) {
            val labelled = DATE_LABEL.containsMatchIn(line)
            dateCandidates(line).forEach { candidates += it to labelled }
        }
        val valid = candidates.filter { (d, _) -> !d.isAfter(today) && d.isAfter(today.minusYears(15)) }
        return (valid.firstOrNull { it.second } ?: valid.firstOrNull())?.first
    }

    internal fun dateCandidates(line: String): List<LocalDate> {
        val out = mutableListOf<LocalDate>()
        ISO_DATE.findAll(line).forEach { m ->
            val (y, mo, d) = m.destructured
            date(y.toInt(), mo.toInt(), d.toInt())?.let(out::add)
        }
        NUM_DATE.findAll(line).forEach { m ->
            val (a, b, yRaw) = m.destructured
            val y = yRaw.toInt().let { if (yRaw.length == 2) 2000 + it else it }
            val first = a.toInt()
            val second = b.toInt()
            val (day, month) = when {
                first > 12 -> first to second
                second > 12 -> second to first
                dayFirst -> first to second
                else -> second to first
            }
            date(y, month, day)?.let(out::add)
        }
        TEXT_DATE_DMY.findAll(line).forEach { m ->
            val (d, mon, y) = m.destructured
            monthOf(mon)?.let { date(year(y), it, d.toInt())?.let(out::add) }
        }
        TEXT_DATE_MDY.findAll(line).forEach { m ->
            val (mon, d, y) = m.destructured
            monthOf(mon)?.let { date(year(y), it, d.toInt())?.let(out::add) }
        }
        return out
    }

    private fun year(raw: String): Int = raw.toInt().let { if (raw.length == 2) 2000 + it else it }

    private fun date(y: Int, m: Int, d: Int): LocalDate? =
        runCatching { LocalDate.of(y, m, d) }.getOrNull()

    private fun monthOf(text: String): Int? {
        val key = text.lowercase().take(3)
        return Month.entries.firstOrNull { it.name.lowercase().startsWith(key) }?.value
    }

    // ---- Amounts ----

    internal fun findTotal(lines: List<String>): Double? {
        // Prefer the strongest label, scanning from the bottom where totals live.
        for (labels in TOTAL_LABELS) {
            for (i in lines.indices.reversed()) {
                val lower = lines[i].lowercase()
                if (labels.any { lower.contains(it) } && TOTAL_EXCLUDE.none { lower.contains(it) }) {
                    amounts(lines[i]).lastOrNull()?.let { return it }
                    lines.getOrNull(i + 1)?.let { next -> amounts(next).lastOrNull()?.let { return it } }
                }
            }
        }
        return lines.flatMap { amounts(it) }.filter { it < 1_000_000 }.maxOrNull()
    }

    internal fun amounts(line: String): List<Double> = AMOUNT.findAll(line).mapNotNull { m ->
        val raw = m.groupValues[1]
        val normalized = when {
            // 1.234,56 -> 1234.56
            raw.matches(Regex("""\d{1,3}(\.\d{3})+,\d{2}""")) -> raw.replace(".", "").replace(',', '.')
            // 1,234.56 -> 1234.56
            raw.matches(Regex("""\d{1,3}(,\d{3})+\.\d{2}""")) -> raw.replace(",", "")
            else -> raw.replace(',', '.')
        }
        normalized.toDoubleOrNull()
    }.toList()

    internal fun findItems(lines: List<String>): List<LineItem> = lines.mapNotNull { line ->
        val lower = line.lowercase()
        if (ITEM_EXCLUDE.any { lower.contains(it) }) return@mapNotNull null
        val match = ITEM_LINE.find(line) ?: return@mapNotNull null
        val name = match.groupValues[1].trim().trim('-', ':', '*', '.').trim()
        val price = amounts(match.groupValues[2]).lastOrNull() ?: return@mapNotNull null
        if (name.count { it.isLetter() } < 3 || price <= 0) null
        else LineItem(titleCase(name.replace(Regex("""^\d+\s*[xX@]?\s*"""), "")), price)
    }.take(30)

    internal fun findCurrency(text: String): String? {
        val upper = text.uppercase()
        CURRENCY_CODES.firstOrNull { Regex("""\b$it\b""").containsMatchIn(upper) }?.let { return it }
        return CURRENCY_SYMBOLS.entries.firstOrNull { text.contains(it.key) }?.value
    }

    // ---- Category & policy ----

    internal fun guessCategory(lines: List<String>, store: String?): Category? {
        val haystack = (lines + listOfNotNull(store)).joinToString(" ").lowercase()
        return CATEGORY_KEYWORDS.maxByOrNull { (_, words) -> words.count { haystack.contains(it) } }
            ?.takeIf { (_, words) -> words.any { haystack.contains(it) } }?.first
    }

    fun returnPolicyFor(store: String): Int? {
        val lower = store.lowercase()
        return RETURN_POLICIES.entries.firstOrNull { lower.contains(it.key) }?.value
    }

    private fun titleCase(s: String): String =
        if (s.any { it.isLowerCase() }) s
        // Model numbers like WH-1000XM5 keep their capitals.
        else s.split(' ').joinToString(" ") { w ->
            if (w.any { it.isDigit() }) w else w.lowercase().replaceFirstChar { it.titlecase() }
        }

    companion object {
        private val ISO_DATE = Regex("""\b(20\d{2})[-/.](\d{1,2})[-/.](\d{1,2})\b""")
        private val NUM_DATE = Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{4}|\d{2})\b""")
        private const val MONTHS = "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*"
        private val TEXT_DATE_DMY = Regex("""\b(\d{1,2})[\s-]*$MONTHS[\s,.-]*(\d{4}|\d{2})\b""", RegexOption.IGNORE_CASE)
        private val TEXT_DATE_MDY = Regex("""\b$MONTHS[\s.-]*(\d{1,2})(?:st|nd|rd|th)?,?[\s-]*(\d{4})\b""", RegexOption.IGNORE_CASE)
        private val DATE_LABEL = Regex("""(?i)\b(date|dated|purchased|invoice date|bill date)\b""")

        private val AMOUNT = Regex("""(?<![\d.,])(\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d+[.,]\d{2})(?![\d])""")
        private val ITEM_LINE = Regex("""^(.*[A-Za-z].*?)\s+[$€£₹৳¥]?\s*(\d[\d.,]*[.,]\d{2})\s*[A-Z]?$""")

        private val TOTAL_LABELS = listOf(
            listOf("grand total", "amount due", "total due", "balance due", "amount paid", "net payable", "total amount"),
            listOf("total"),
        )
        private val TOTAL_EXCLUDE = listOf("subtotal", "sub total", "sub-total", "total tax", "total items", "total savings", "total qty")
        private val ITEM_EXCLUDE = listOf(
            "total", "tax", "vat", "gst", "change", "cash", "card", "visa", "master", "balance", "tender",
            "amount", "discount", "savings", "payment", "paid", "due", "tip", "rounding", "debit", "credit",
        )
        private val STORE_NOISE = listOf(
            "receipt", "invoice", "welcome", "tax", "thank", "store #", "tel", "phone", "date", "order", "cashier",
        )

        private val KNOWN_STORES = listOf(
            "walmart" to "Walmart", "target" to "Target", "best buy" to "Best Buy", "bestbuy" to "Best Buy",
            "costco" to "Costco", "amazon" to "Amazon", "apple store" to "Apple", "apple.com" to "Apple",
            "ikea" to "IKEA", "home depot" to "The Home Depot", "lowe's" to "Lowe's", "lowes" to "Lowe's",
            "macy" to "Macy's", "kohl" to "Kohl's", "nordstrom" to "Nordstrom", "h&m" to "H&M", "zara" to "Zara",
            "uniqlo" to "Uniqlo", "sephora" to "Sephora", "ulta" to "Ulta Beauty", "currys" to "Currys",
            "argos" to "Argos", "tesco" to "Tesco", "sainsbury" to "Sainsbury's", "john lewis" to "John Lewis",
            "decathlon" to "Decathlon", "media markt" to "MediaMarkt", "mediamarkt" to "MediaMarkt",
            "samsung" to "Samsung", "staples" to "Staples", "walgreens" to "Walgreens", "cvs" to "CVS",
            "nike" to "Nike", "adidas" to "Adidas", "aldi" to "Aldi", "lidl" to "Lidl", "carrefour" to "Carrefour",
            "flipkart" to "Flipkart", "reliance digital" to "Reliance Digital", "croma" to "Croma", "daraz" to "Daraz",
        )

        private val RETURN_POLICIES = mapOf(
            "walmart" to 90, "target" to 90, "best buy" to 15, "costco" to 90, "amazon" to 30,
            "apple" to 14, "ikea" to 365, "home depot" to 90, "lowe" to 90, "macy" to 30, "kohl" to 180,
            "nordstrom" to 30, "h&m" to 30, "zara" to 30, "uniqlo" to 30, "sephora" to 30, "ulta" to 60,
            "currys" to 21, "argos" to 30, "john lewis" to 35, "decathlon" to 365, "staples" to 14,
            "nike" to 60, "adidas" to 30, "samsung" to 15, "flipkart" to 7, "croma" to 7, "daraz" to 7,
        )

        private val CURRENCY_CODES = listOf("USD", "EUR", "GBP", "CAD", "AUD", "INR", "BDT", "PKR", "AED", "SAR", "JPY", "SGD", "MYR")
        private val CURRENCY_SYMBOLS = linkedMapOf(
            "€" to "EUR", "£" to "GBP", "₹" to "INR", "৳" to "BDT", "¥" to "JPY", "Rs" to "INR", "$" to "USD",
        )

        private val CATEGORY_KEYWORDS = listOf(
            Category.ELECTRONICS to listOf(
                "phone", "iphone", "galaxy", "laptop", "macbook", "tablet", "ipad", "headphone", "earbud", "airpods",
                "tv", "monitor", "camera", "speaker", "charger", "cable", "console", "watch", "keyboard", "mouse",
                "ssd", "router", "best buy", "electronics", "apple",
            ),
            Category.APPLIANCES to listOf(
                "fridge", "refrigerator", "washer", "dryer", "microwave", "oven", "blender", "vacuum", "kettle",
                "toaster", "air fryer", "dishwasher", "heater", "fan", "iron", "coffee maker",
            ),
            Category.FASHION to listOf("shirt", "jeans", "dress", "jacket", "shoe", "sneaker", "zara", "h&m", "uniqlo", "nike", "adidas", "apparel"),
            Category.HOME to listOf("ikea", "sofa", "chair", "table", "lamp", "bed", "mattress", "pillow", "curtain", "rug"),
            Category.BEAUTY to listOf("sephora", "ulta", "serum", "cream", "lotion", "perfume", "makeup", "shampoo"),
            Category.SPORTS to listOf("decathlon", "bike", "bicycle", "yoga", "dumbbell", "treadmill", "racket"),
            Category.TOOLS to listOf("drill", "home depot", "lowe", "saw", "wrench", "toolkit", "screwdriver"),
            Category.TOYS to listOf("lego", "toy", "puzzle", "doll", "stroller"),
            Category.GROCERY to listOf("milk", "bread", "eggs", "grocery", "produce", "aldi", "lidl", "tesco"),
        )
    }
}
