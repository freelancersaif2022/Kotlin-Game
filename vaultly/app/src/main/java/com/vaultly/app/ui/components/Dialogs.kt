package com.vaultly.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaultly.app.ui.theme.Palette
import com.vaultly.app.ui.theme.VaultlyTheme
import java.util.Currency
import java.util.Locale

enum class DialogTone { BRAND, GOLD, DANGER, SUCCESS }

/**
 * The app's dialog: a rounded card with a toned icon badge, centred title and message,
 * a full-width primary action and a quieter secondary action. Springs in on open.
 */
@Composable
fun VaultDialog(
    onDismiss: () -> Unit,
    icon: ImageVector,
    title: String,
    message: String? = null,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    tone: DialogTone = DialogTone.BRAND,
    primaryLoading: Boolean = false,
    badge: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.88f),
        ) {
            val colors = VaultlyTheme.colors
            val shape = RoundedCornerShape(30.dp)
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, colors.hairline, shape)
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ToneBadge(icon, tone)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        badge()
                    }
                }
                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                if (content != null) {
                    Spacer(Modifier.height(18.dp))
                    content()
                }
                Spacer(Modifier.height(24.dp))
                PrimaryButton(
                    primaryText,
                    onPrimary,
                    Modifier.fillMaxWidth(),
                    loading = primaryLoading,
                    danger = tone == DialogTone.DANGER,
                    gold = tone == DialogTone.GOLD,
                )
                if (secondaryText != null) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { (onSecondary ?: onDismiss)() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(secondaryText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun ToneBadge(icon: ImageVector, tone: DialogTone, size: Int = 60) {
    val colors = VaultlyTheme.colors
    val (bg, fg) = when (tone) {
        DialogTone.BRAND -> colors.inkBrush to Palette.GoldLight
        DialogTone.GOLD -> colors.goldBrush to Palette.Ink
        DialogTone.DANGER -> SolidColor(colors.danger.copy(alpha = 0.13f)) to colors.danger
        DialogTone.SUCCESS -> SolidColor(colors.success.copy(alpha = 0.14f)) to colors.success
    }
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.32f).dp)).background(bg as Brush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size((size * 0.45f).dp), tint = fg)
    }
}

/** Number picker dialog with a stepper and quick presets. */
@Composable
fun NumberDialog(
    icon: ImageVector,
    title: String,
    unit: String,
    initial: Int,
    range: IntRange,
    presets: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var value by remember { mutableIntStateOf(initial.coerceIn(range)) }
    var text by remember { mutableStateOf(value.toString()) }
    fun set(v: Int) { value = v.coerceIn(range); text = value.toString() }
    VaultDialog(
        onDismiss = onDismiss,
        icon = icon,
        title = title,
        message = "Choose between ${range.first} and ${range.last} ${unit.lowercase()}.",
        primaryText = "Set $value ${unit.lowercase()}",
        onPrimary = { onConfirm(value) },
        secondaryText = "Cancel",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(Icons.Outlined.Remove) { set(value - 1) }
            Column(Modifier.width(130.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BasicTextField(
                    value = text,
                    onValueChange = { t ->
                        text = t.filter(Char::isDigit).take(3)
                        text.toIntOrNull()?.let { value = it.coerceIn(range) }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(VaultlyTheme.colors.gold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(unit.uppercase(), style = MaterialTheme.typography.labelSmall, color = VaultlyTheme.colors.muted)
            }
            StepButton(Icons.Outlined.Add) { set(value + 1) }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { p ->
                ChoiceChip("$p", value == p, { set(p) })
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface) }
}

private val POPULAR = listOf("USD", "EUR", "GBP", "INR", "BDT", "AED", "CAD", "AUD")

/** Searchable bottom sheet listing every currency, with popular ones first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val all = remember {
        Currency.getAvailableCurrencies()
            .filter { it.defaultFractionDigits >= 0 && !it.currencyCode.startsWith("X") }
            .sortedBy { it.currencyCode }
    }
    val q = query.trim().lowercase()
    val matches = all.filter {
        q.isEmpty() || it.currencyCode.lowercase().contains(q) || it.getDisplayName(Locale.getDefault()).lowercase().contains(q)
    }
    val popular = if (q.isEmpty()) all.filter { it.currencyCode in POPULAR }.sortedBy { POPULAR.indexOf(it.currencyCode) } else emptyList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Currency", style = MaterialTheme.typography.headlineSmall)
                    Text("Used for prices and your protected value", style = MaterialTheme.typography.bodySmall, color = VaultlyTheme.colors.muted)
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Close, "Close", Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, Modifier.size(19.dp), tint = VaultlyTheme.colors.muted)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search code or name", style = MaterialTheme.typography.bodyLarge, color = VaultlyTheme.colors.muted)
                    BasicTextField(
                        query, { query = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(VaultlyTheme.colors.gold),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                if (popular.isNotEmpty()) {
                    item { SheetLabel("Popular") }
                    items(popular, key = { "p" + it.currencyCode }) { CurrencyRow(it, it.currencyCode == selected) { onSelect(it.currencyCode) } }
                    item { SheetLabel("All currencies") }
                }
                items(matches, key = { it.currencyCode }) { CurrencyRow(it, it.currencyCode == selected) { onSelect(it.currencyCode) } }
                if (matches.isEmpty()) {
                    item {
                        Text(
                            "No currency matches “$query”",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VaultlyTheme.colors.muted,
                            modifier = Modifier.padding(vertical = 28.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = VaultlyTheme.colors.muted,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp),
    )
}

@Composable
private fun CurrencyRow(currency: Currency, selected: Boolean, onClick: () -> Unit) {
    val colors = VaultlyTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(if (selected) colors.gold.copy(alpha = 0.10f) else Color.Transparent)
            .border(1.dp, if (selected) colors.gold.copy(alpha = 0.45f) else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val symbol = currency.getSymbol(Locale.getDefault())
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (symbol.length <= 3) symbol else currency.currencyCode.take(1),
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(currency.currencyCode, style = MaterialTheme.typography.titleSmall)
            Text(currency.getDisplayName(Locale.getDefault()), style = MaterialTheme.typography.bodySmall, color = colors.muted, maxLines = 1)
        }
        if (selected) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(colors.goldBrush), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Palette.Ink)
            }
        }
    }
}
