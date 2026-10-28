package com.lexora.english.util

/** Offline writing feedback: length, structure, punctuation, common mistakes and target words. */
object WritingCoach {

    data class Feedback(val good: Boolean, val text: String)

    data class Report(
        val score: Int,
        val words: Int,
        val sentences: Int,
        val uniqueRatio: Int,
        val keywordsUsed: List<String>,
        val keywordsMissing: List<String>,
        val feedback: List<Feedback>,
        val meetsMinimum: Boolean,
    )

    private val commonMistakes = listOf(
        Regex("\\balot\\b", RegexOption.IGNORE_CASE) to "Write \"a lot\" as two words.",
        Regex("\\b(could|should|would) of\\b", RegexOption.IGNORE_CASE) to "Use \"could/should/would have\", not \"of\".",
        Regex("\\bmore (better|worse|bigger|smaller|easier|faster)\\b", RegexOption.IGNORE_CASE) to "Don't use \"more\" with -er comparatives (say \"better\").",
        Regex("\\b(informations|advices|furnitures|equipments|homeworks)\\b", RegexOption.IGNORE_CASE) to "This noun is uncountable — remove the -s.",
        Regex("\\bi am agree\\b", RegexOption.IGNORE_CASE) to "Say \"I agree\", not \"I am agree\".",
        Regex("\\b(teached|buyed|goed|thinked|catched|bringed|writed|eated)\\b", RegexOption.IGNORE_CASE) to "Check irregular past forms (taught, bought, went, thought, caught, brought, wrote, ate).",
        Regex("\\b(thier|recieve|definately|seperate|untill|tommorow|becuase|beleive|wich|freind|wierd)\\b", RegexOption.IGNORE_CASE) to "Check the spelling of commonly misspelled words.",
        Regex("\\b(dont|cant|wont|didnt|doesnt|isnt|im|ive|youre|theyre)\\b", RegexOption.IGNORE_CASE) to "Contractions need an apostrophe (don't, can't, I'm…).",
        Regex("\\b(u|ur|pls|plz|thx)\\b", RegexOption.IGNORE_CASE) to "Avoid text-message spelling (u, ur, pls).",
        Regex("\\b(\\w+) \\1\\b", RegexOption.IGNORE_CASE) to "A word is repeated twice in a row.",
        Regex("\\bexplain me\\b", RegexOption.IGNORE_CASE) to "Say \"explain to me\" or \"explain something to someone\".",
        Regex("\\bi have \\d+ years\\b", RegexOption.IGNORE_CASE) to "For age, say \"I am 25 years old\".",
        Regex("\\bdepend of\\b", RegexOption.IGNORE_CASE) to "Say \"depend on\".",
        Regex("\\barrive(d)? to\\b", RegexOption.IGNORE_CASE) to "Say \"arrive in\" (city) or \"arrive at\" (place).",
    )

    fun analyze(text: String, minWords: Int, keywords: List<String>): Report {
        val trimmed = text.trim()
        val words = Regex("[A-Za-z']+").findAll(trimmed).map { it.value }.toList()
        val sentences = trimmed.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val feedback = mutableListOf<Feedback>()
        var score = 100

        val meets = words.size >= minWords
        if (meets) {
            feedback += Feedback(true, "Great length: ${words.size} words (minimum $minWords).")
        } else {
            feedback += Feedback(false, "Write at least $minWords words. You have ${words.size}.")
            score -= ((minWords - words.size) * 40 / minWords.coerceAtLeast(1)).coerceAtMost(40)
        }

        if (sentences.size >= 3) {
            feedback += Feedback(true, "Good structure with ${sentences.size} sentences.")
        } else if (words.isNotEmpty()) {
            feedback += Feedback(false, "Split your ideas into at least 3 sentences.")
            score -= 8
        }

        val lowerStarts = sentences.count { s -> s.firstOrNull { it.isLetter() }?.isLowerCase() == true }
        if (lowerStarts > 0) {
            feedback += Feedback(false, "$lowerStarts sentence(s) don't start with a capital letter.")
            score -= (lowerStarts * 4).coerceAtMost(12)
        } else if (sentences.isNotEmpty()) {
            feedback += Feedback(true, "Every sentence starts with a capital letter.")
        }

        if (Regex("(^|\\s)i(\\s|'|$)").containsMatchIn(trimmed)) {
            feedback += Feedback(false, "Always write the pronoun \"I\" as a capital letter.")
            score -= 6
        }

        if (trimmed.isNotEmpty() && trimmed.last() !in ".!?") {
            feedback += Feedback(false, "End your text with a full stop or question mark.")
            score -= 4
        }

        val longSentence = sentences.any { s -> s.split(Regex("\\s+")).size > 35 }
        if (longSentence) {
            feedback += Feedback(false, "One sentence is very long. Break it into two for clarity.")
            score -= 4
        }

        var mistakeCount = 0
        for ((regex, message) in commonMistakes) {
            if (regex.containsMatchIn(trimmed)) {
                feedback += Feedback(false, message)
                mistakeCount++
            }
        }
        score -= (mistakeCount * 7).coerceAtMost(28)
        if (mistakeCount == 0 && words.size >= 10) feedback += Feedback(true, "No common grammar or spelling mistakes found.")

        val lowerText = trimmed.lowercase()
        val used = keywords.filter { lowerText.contains(it.lowercase()) }
        val missing = keywords - used.toSet()
        if (keywords.isNotEmpty()) {
            if (missing.isEmpty()) feedback += Feedback(true, "You used all the target words.")
            else {
                feedback += Feedback(false, "Try to include: " + missing.joinToString(", "))
                score -= missing.size * 4
            }
        }

        val unique = if (words.isEmpty()) 0 else words.map { it.lowercase() }.toSet().size * 100 / words.size
        if (words.size >= 20) {
            if (unique >= 60) feedback += Feedback(true, "Nice variety of vocabulary.")
            else {
                feedback += Feedback(false, "You repeat many words. Try synonyms for variety.")
                score -= 5
            }
        }

        val connectors = listOf("because", "however", "although", "so ", "but ", "also", "first", "then", "finally", "for example")
        if (connectors.count { lowerText.contains(it) } >= 2) feedback += Feedback(true, "Good use of linking words.")
        else if (words.size >= 20) {
            feedback += Feedback(false, "Use linking words (because, however, for example) to connect ideas.")
            score -= 4
        }

        if (words.isEmpty()) score = 0
        return Report(
            score = score.coerceIn(0, 100),
            words = words.size,
            sentences = sentences.size,
            uniqueRatio = unique,
            keywordsUsed = used,
            keywordsMissing = missing,
            feedback = feedback.sortedBy { it.good },
            meetsMinimum = meets,
        )
    }
}
