package com.vaultly.app.ui.edit

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AssignmentReturned
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vaultly.app.VaultlyApp
import com.vaultly.app.data.Category
import com.vaultly.app.domain.Deadlines
import com.vaultly.app.ui.components.ChoiceChip
import com.vaultly.app.ui.components.CurrencySheet
import com.vaultly.app.ui.components.NumberDialog
import com.vaultly.app.ui.components.PickerField
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.components.SectionHeader
import com.vaultly.app.ui.components.VaultField
import com.vaultly.app.ui.components.findActivity
import com.vaultly.app.ui.components.icon
import com.vaultly.app.ui.home.RoundIcon
import com.vaultly.app.ui.theme.Palette
import com.vaultly.app.ui.theme.VaultlyTheme
import com.vaultly.app.util.Format
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val RETURN_PRESETS = listOf(null, 7, 14, 30, 60, 90)
private val WARRANTY_PRESETS = listOf(null, 6, 12, 24, 36)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    app: VaultlyApp,
    itemId: Long?,
    imageUri: Uri?,
    onClose: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit,
) {
    val vm: EditViewModel = viewModel { EditViewModel(app, itemId, imageUri) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = VaultlyTheme.colors

    var showDate by remember { mutableStateOf(false) }
    var showCurrency by remember { mutableStateOf(false) }
    var customReturn by remember { mutableStateOf(false) }
    var customWarranty by remember { mutableStateOf(false) }

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importReceipt(uri)
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
        uris.forEach { vm.addPhoto(it) }
    }

    val close = { vm.discard(); onClose() }
    BackHandler(onBack = close)

    val save: () -> Unit = {
        scope.launch {
            val id = vm.save() ?: return@launch
            val activity = context.findActivity()
            if (s.isNew && activity != null) {
                app.ads.onItemSaved(activity) { onSaved(id, true) }
            } else {
                onSaved(id, s.isNew)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(Icons.Outlined.Close, "Close", close)
            Spacer(Modifier.width(14.dp))
            Text(
                if (s.isNew) "New purchase" else "Edit purchase",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReceiptPreview(
                path = s.receiptImage,
                scanning = s.scanning,
                autoFilled = s.autoFilled,
                scanFailed = s.scanFailed,
                onAdd = { receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = vm::removeReceipt,
            )

            VaultField(
                "Item", s.title, vm::setTitle, Modifier.fillMaxWidth(),
                placeholder = "e.g. Sony WH-1000XM5 headphones",
                isError = s.titleError,
            )
            VaultField(
                "Store", s.store, vm::setStore, Modifier.fillMaxWidth(),
                placeholder = "Where you bought it",
                capitalization = KeyboardCapitalization.Words,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VaultField(
                    "Price", s.price, vm::setPrice, Modifier.weight(1f),
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                    leading = Format.symbol(s.currency),
                )
                PickerField("Currency", s.currency, Icons.Outlined.ExpandMore, { showCurrency = true }, Modifier.width(128.dp))
            }
            PickerField(
                "Purchase date", Format.date(s.purchaseDate), Icons.Outlined.CalendarMonth,
                { showDate = true }, Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            SectionHeader("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    ChoiceChip(c.label, s.category == c, { vm.setCategory(c) }, icon = c.icon())
                }
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader("Return window")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RETURN_PRESETS.forEach { d ->
                    ChoiceChip(if (d == null) "None" else "$d days", s.returnDays == d, { vm.setReturnDays(d) })
                }
                val custom = s.returnDays != null && s.returnDays !in RETURN_PRESETS
                ChoiceChip(if (custom) "${s.returnDays} days" else "Custom", custom, { customReturn = true })
            }

            Spacer(Modifier.height(4.dp))
            SectionHeader("Warranty")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WARRANTY_PRESETS.forEach { m ->
                    ChoiceChip(if (m == null) "None" else Format.warranty(m), s.warrantyMonths == m, { vm.setWarranty(m) })
                }
                val custom = s.warrantyMonths != null && s.warrantyMonths !in WARRANTY_PRESETS
                ChoiceChip(if (custom) Format.warranty(s.warrantyMonths) else "Custom", custom, { customWarranty = true })
            }

            DeadlineSummary(s.purchaseDate, s.returnDays, s.warrantyMonths)

            Spacer(Modifier.height(4.dp))
            SectionHeader("Proof & details")
            PhotoStrip(
                photos = s.photos,
                onAdd = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = vm::removePhoto,
            )
            VaultField(
                "Serial or order number", s.serial, vm::setSerial, Modifier.fillMaxWidth(),
                placeholder = "Optional",
                capitalization = KeyboardCapitalization.Characters,
            )
            VaultField(
                "Notes", s.notes, vm::setNotes, Modifier.fillMaxWidth(),
                placeholder = "Gift receipt, colour, box kept…",
                singleLine = false,
                minHeight = 64,
            )
            Spacer(Modifier.height(12.dp))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            PrimaryButton(
                if (s.isNew) "Save to vault" else "Save changes",
                save,
                Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Check,
                loading = s.saving,
                enabled = !s.scanning,
            )
        }
    }

    if (showDate) {
        val zoneMillis = s.purchaseDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val todayMillis = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = zoneMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDate = false
                }) { Text("Done", color = colors.gold, style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel", color = colors.muted) } },
            shape = RoundedCornerShape(28.dp),
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            DatePicker(
                state = state,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedDayContainerColor = colors.gold,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = colors.gold,
                    todayContentColor = colors.gold,
                    selectedYearContainerColor = colors.gold,
                    selectedYearContentColor = Color.White,
                ),
            )
        }
    }

    if (showCurrency) {
        CurrencySheet(s.currency, onSelect = { vm.setCurrency(it); showCurrency = false }, onDismiss = { showCurrency = false })
    }

    if (customReturn) {
        NumberDialog(
            icon = Icons.Outlined.AssignmentReturned,
            title = "Return window",
            unit = "Days",
            initial = s.returnDays ?: 30,
            range = 1..730,
            presets = listOf(21, 45, 120, 365),
            onDismiss = { customReturn = false },
        ) { vm.setReturnDays(it); customReturn = false }
    }
    if (customWarranty) {
        NumberDialog(
            icon = Icons.Outlined.VerifiedUser,
            title = "Warranty length",
            unit = "Months",
            initial = s.warrantyMonths ?: 12,
            range = 1..240,
            presets = listOf(18, 48, 60, 120),
            onDismiss = { customWarranty = false },
        ) { vm.setWarranty(it); customWarranty = false }
    }
}

@Composable
private fun ReceiptPreview(
    path: String?,
    scanning: Boolean,
    autoFilled: List<String>,
    scanFailed: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = VaultlyTheme.colors
    val shape = RoundedCornerShape(22.dp)
    if (path == null && !scanning) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.card)
                .border(1.dp, colors.hairline, shape)
                .clickable(onClick = onAdd)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(colors.inkBrush),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Receipt, null, tint = Palette.GoldLight) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Attach receipt photo", style = MaterialTheme.typography.titleMedium)
                Text("Vaultly reads it and fills the form", style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
        }
        return
    }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.card).border(1.dp, colors.hairline, shape),
    ) {
        Box(Modifier.fillMaxWidth().height(190.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (path != null) {
                AsyncImage(
                    model = File(path),
                    contentDescription = "Receipt",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (scanning) ScanSweep()
            if (!scanning && path != null) {
                Row(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlayButton(Icons.Outlined.Sync, onAdd)
                    OverlayButton(Icons.Outlined.Close, onRemove)
                }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                scanning -> {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp), tint = colors.gold)
                    Spacer(Modifier.width(8.dp))
                    Text("Reading your receipt…", style = MaterialTheme.typography.labelLarge)
                }
                autoFilled.isNotEmpty() -> {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp), tint = colors.success)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Auto-filled " + autoFilled.joinToString(", ") + ". Please check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                scanFailed -> {
                    Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(18.dp), tint = colors.warning)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Couldn't read this one clearly. Fill in the details below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> Text("Receipt attached", style = MaterialTheme.typography.bodySmall, color = colors.muted)
            }
        }
    }
}

@Composable
private fun ScanSweep() {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "y")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(70.dp)
                .graphicsLayer { translationY = y * (size.height * 1.7f) }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.GoldLight.copy(alpha = 0.45f), Color.Transparent))),
        )
    }
}

@Composable
private fun OverlayButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(18.dp), tint = Color.White) }
}

@Composable
private fun DeadlineSummary(date: LocalDate, returnDays: Int?, warrantyMonths: Int?) {
    val colors = VaultlyTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.gold.copy(alpha = 0.08f))
            .border(1.dp, colors.gold.copy(alpha = 0.25f), shape)
            .padding(16.dp),
    ) {
        SummaryLine("Return by", returnDays?.takeIf { it > 0 }?.let { date.plusDays(it.toLong()) })
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = colors.gold.copy(alpha = 0.18f))
        SummaryLine("Warranty until", warrantyMonths?.takeIf { it > 0 }?.let { date.plusMonths(it.toLong()) })
    }
}

@Composable
private fun SummaryLine(label: String, end: LocalDate?) {
    val colors = VaultlyTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.muted, modifier = Modifier.weight(1f))
        if (end == null) {
            Text("—", style = MaterialTheme.typography.titleSmall, color = colors.muted)
        } else {
            val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), end)
            Text(Format.date(end), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            Text(
                Deadlines.shortLeft(days),
                style = MaterialTheme.typography.labelMedium,
                color = com.vaultly.app.ui.components.daysColor(days),
            )
        }
    }
}

@Composable
private fun PhotoStrip(photos: List<String>, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    val colors = VaultlyTheme.colors
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier
                .size(88.dp)
                .clip(shape)
                .background(colors.card)
                .border(1.dp, colors.hairline, shape)
                .clickable(onClick = onAdd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text("Add photo", style = MaterialTheme.typography.labelMedium, color = colors.muted)
        }
        photos.forEach { p ->
            Box(Modifier.size(88.dp).clip(shape)) {
                AsyncImage(File(p), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)).clickable { onRemove(p) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Close, "Remove", Modifier.size(14.dp), tint = Color.White) }
            }
        }
    }
    if (photos.isEmpty()) {
        Text(
            "Add the product, box, serial label or packaging — useful for warranty claims.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
            modifier = Modifier.alpha(0.9f),
        )
    }
}
