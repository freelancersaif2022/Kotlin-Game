package com.zyropay.app.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/** Returns a function that copies text to the clipboard and shows a "Copied" toast. */
@Composable
fun rememberCopier(): (label: String, text: String) -> Unit {
    val clipboard = LocalClipboardManager.current
    val messenger = LocalMessenger.current
    return remember(clipboard, messenger) {
        { label, text ->
            clipboard.setText(AnnotatedString(text))
            messenger.toast("$label copied")
        }
    }
}

@Composable
fun rememberPaster(): () -> String {
    val clipboard = LocalClipboardManager.current
    return remember(clipboard) { { clipboard.getText()?.text.orEmpty() } }
}

fun Context.shareText(text: String, title: String = "Share via") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { startActivity(Intent.createChooser(intent, title)) }
}

fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}

/** Parses a user-entered decimal amount safely. */
fun String.toAmount(): Double = replace(",", "").toDoubleOrNull() ?: 0.0

/** Sanitizes amount text input to digits with at most [decimals] fraction digits. */
fun sanitizeAmount(input: String, decimals: Int): String {
    val cleaned = input.replace(',', '.').filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    val noExtraDots = if (firstDot >= 0) cleaned.substring(0, firstDot + 1) + cleaned.substring(firstDot + 1).replace(".", "") else cleaned
    val parts = noExtraDots.split('.')
    val intPart = parts[0].trimStart('0').ifEmpty { if (noExtraDots.isNotEmpty()) "0" else "" }.take(10)
    return if (parts.size > 1) intPart.ifEmpty { "0" } + "." + parts[1].take(decimals) else intPart
}
