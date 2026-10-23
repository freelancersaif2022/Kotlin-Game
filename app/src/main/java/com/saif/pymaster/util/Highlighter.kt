package com.saif.pymaster.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object SyntaxColors {
    val Plain = Color(0xFFE6EDF3)
    val Keyword = Color(0xFFFF7AB2)
    val Builtin = Color(0xFF4FD1FF)
    val Function = Color(0xFFFFD43B)
    val ClassName = Color(0xFF7EE787)
    val StringC = Color(0xFFA5E075)
    val Number = Color(0xFFFFA94D)
    val Comment = Color(0xFF6E7B8F)
    val Decorator = Color(0xFFC792EA)
    val Self = Color(0xFFFF9E64)
}

/** Lightweight single-pass Python tokenizer used for colouring code. */
object PythonHighlighter {
    private val keywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue",
        "def", "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in",
        "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield",
        "match", "case",
    )
    private val builtins = setOf(
        "print", "input", "len", "range", "int", "float", "str", "bool", "list", "dict", "set", "tuple",
        "type", "isinstance", "enumerate", "zip", "map", "filter", "sum", "min", "max", "abs", "round",
        "sorted", "reversed", "open", "any", "all", "id", "help", "dir", "chr", "ord", "hex", "bin", "pow",
        "super", "object", "property", "staticmethod", "classmethod", "iter", "next", "format", "divmod",
        "Exception", "ValueError", "TypeError", "KeyError", "IndexError", "ZeroDivisionError",
        "FileNotFoundError", "NameError", "AttributeError", "RuntimeError", "StopIteration",
        "frozenset", "bytes", "complex", "hash", "callable", "getattr", "setattr", "hasattr", "vars",
    )

    fun highlight(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        addStyle(SpanStyle(color = SyntaxColors.Plain), 0, code.length)
        val n = code.length
        var i = 0
        var previousWord = ""
        while (i < n) {
            val c = code[i]
            when {
                c == '#' -> {
                    val end = code.indexOf('\n', i).let { if (it < 0) n else it }
                    addStyle(SpanStyle(color = SyntaxColors.Comment, fontStyle = FontStyle.Italic), i, end)
                    i = end
                }
                c == '"' || c == '\'' -> {
                    val triple = i + 2 < n && code[i + 1] == c && code[i + 2] == c
                    var j = if (triple) i + 3 else i + 1
                    while (j < n) {
                        if (code[j] == '\\') {
                            j += 2; continue
                        }
                        if (triple) {
                            if (j + 2 < n && code[j] == c && code[j + 1] == c && code[j + 2] == c) {
                                j += 3; break
                            }
                        } else {
                            if (code[j] == c) {
                                j++; break
                            }
                            if (code[j] == '\n') break
                        }
                        j++
                    }
                    val end = j.coerceAtMost(n)
                    // f-string prefix
                    val start = if (i > 0 && code[i - 1].lowercaseChar() in "frb" &&
                        (i < 2 || !code[i - 2].isLetterOrDigit())
                    ) i - 1 else i
                    addStyle(SpanStyle(color = SyntaxColors.StringC), start, end)
                    i = end
                    previousWord = ""
                }
                c.isDigit() && (i == 0 || !(code[i - 1].isLetterOrDigit() || code[i - 1] == '_')) -> {
                    var j = i
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) j++
                    addStyle(SpanStyle(color = SyntaxColors.Number), i, j)
                    i = j
                }
                c == '@' && (i == 0 || code[i - 1] == '\n' || code[i - 1] == ' ') -> {
                    var j = i + 1
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '.')) j++
                    addStyle(SpanStyle(color = SyntaxColors.Decorator), i, j)
                    i = j
                }
                c.isLetter() || c == '_' -> {
                    var j = i
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    val word = code.substring(i, j)
                    val style = when {
                        previousWord == "def" -> SpanStyle(color = SyntaxColors.Function)
                        previousWord == "class" -> SpanStyle(color = SyntaxColors.ClassName)
                        word in keywords -> SpanStyle(color = SyntaxColors.Keyword)
                        word == "self" || word == "cls" -> SpanStyle(color = SyntaxColors.Self, fontStyle = FontStyle.Italic)
                        word in builtins -> SpanStyle(color = SyntaxColors.Builtin)
                        nextNonSpace(code, j) == '(' -> SpanStyle(color = SyntaxColors.Function)
                        else -> null
                    }
                    // Don't colour string prefixes twice (handled by the string branch).
                    val isPrefix = j < n && (code[j] == '"' || code[j] == '\'') && word.length <= 2 &&
                        word.all { it.lowercaseChar() in "frbu" }
                    if (style != null && !isPrefix) addStyle(style, i, j)
                    previousWord = word
                    i = j
                }
                else -> {
                    if (!c.isWhitespace()) previousWord = ""
                    i++
                }
            }
        }
    }

    private fun nextNonSpace(s: String, from: Int): Char? {
        var k = from
        while (k < s.length && s[k] == ' ') k++
        return s.getOrNull(k)
    }
}

class PythonVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(PythonHighlighter.highlight(text.text), OffsetMapping.Identity)
}
