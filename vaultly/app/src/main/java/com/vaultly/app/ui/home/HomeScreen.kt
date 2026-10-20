package com.vaultly.app.ui.home

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.vaultly.app.VaultlyApp
import com.vaultly.app.data.Prefs
import com.vaultly.app.ui.components.ItemCard
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.components.ProBadge
import com.vaultly.app.ui.components.SectionHeader
import com.vaultly.app.ui.components.findActivity
import com.vaultly.app.ui.nav.Routes
import com.vaultly.app.ui.nav.openPaywall
import com.vaultly.app.ui.theme.Palette
import com.vaultly.app.ui.theme.VaultlyTheme
import com.vaultly.app.util.Format
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(app: VaultlyApp, nav: NavHostController) {
    val vm: HomeViewModel = viewModel { HomeViewModel(app.repository, app.prefs) }
    val state by vm.state.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val pro by app.prefs.pro.collectAsStateWithLifecycle()
    val bonus by app.prefs.bonusSlots.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var showLimit by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }

    val openEditor: (Uri?) -> Unit = { uri -> nav.navigate(Routes.edit(image = uri)) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) openEditor(uri)
    }
    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val page = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.firstOrNull()
            if (page != null) openEditor(page.imageUri)
        }
    }
    val pickGallery = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val startScan: () -> Unit = {
        val activity = context.findActivity()
        if (activity != null) {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
            GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
                .addOnSuccessListener { scanLauncher.launch(IntentSenderRequest.Builder(it).build()) }
                .addOnFailureListener {
                    Toast.makeText(context, "Scanner is getting ready. Pick a photo instead.", Toast.LENGTH_SHORT).show()
                    pickGallery()
                }
        }
    }
    val requestAdd = {
        scope.launch {
            val count = app.repository.activeCount()
            if (!app.prefs.isPro && count >= app.prefs.freeLimit) showLimit = true else showAdd = true
        }
        Unit
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.totalItems > 0) AddFab(onClick = requestAdd)
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Header(
                    pro = pro,
                    searching = searching,
                    query = query,
                    onQuery = { vm.query.value = it },
                    onSearch = {
                        searching = !searching
                        if (!searching) vm.query.value = ""
                    },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            item {
                HeroCard(
                    state = state,
                    pro = pro,
                    limit = Prefs.FREE_ITEMS + bonus,
                    onUpgrade = { nav.openPaywall() },
                )
            }
            if (state.totalItems > 0) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    ) {
                        items(Filter.entries) { f ->
                            FilterChip(
                                label = f.label,
                                count = state.counts[f] ?: 0,
                                selected = f == filter,
                                onClick = { vm.filter.value = f },
                            )
                        }
                    }
                }
                item {
                    SectionHeader(
                        if (query.isNotBlank()) "${state.items.size} results" else "Sorted by nearest deadline",
                        Modifier.padding(top = 4.dp),
                    )
                }
                if (state.items.isEmpty()) {
                    item { FilteredEmpty(filter, query) }
                }
                items(state.items, key = { it.id }) { item ->
                    ItemCard(item, onClick = { nav.navigate(Routes.detail(item.id)) }, modifier = Modifier.animateItem())
                }
            } else if (!state.loading) {
                item { EmptyVault(onScan = startScan, onGallery = pickGallery, onManual = { openEditor(null) }) }
            }
        }
    }

    if (showAdd) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAdd = false },
            sheetState = sheet,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).navigationBarsPadding()) {
                Text("Add a purchase", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Vaultly reads the store, date and total for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VaultlyTheme.colors.muted,
                )
                Spacer(Modifier.height(20.dp))
                AddOption(Icons.Outlined.DocumentScanner, "Scan receipt", "Auto-crop, enhance & read", highlight = true) {
                    showAdd = false; startScan()
                }
                Spacer(Modifier.height(10.dp))
                AddOption(Icons.Outlined.PhotoLibrary, "Choose from photos", "Screenshot or e-receipt image") {
                    showAdd = false; pickGallery()
                }
                Spacer(Modifier.height(10.dp))
                AddOption(Icons.Outlined.EditNote, "Enter manually", "No receipt? Add the details yourself") {
                    showAdd = false; openEditor(null)
                }
            }
        }
    }

    if (showLimit) {
        val activity = context.findActivity()
        AlertDialog(
            onDismissRequest = { showLimit = false },
            icon = { Icon(Icons.Outlined.Shield, null, tint = VaultlyTheme.colors.gold) },
            title = { Text("Your free vault is full") },
            text = {
                Text(
                    "Free vaults hold ${app.prefs.freeLimit} active items. Watch a short video to add ${Prefs.REWARD_SLOTS} more slots, or go Pro for unlimited items.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showLimit = false; nav.openPaywall() }) { Text("Go Pro") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLimit = false
                    if (activity != null) {
                        app.ads.showRewarded(
                            activity,
                            onReward = {
                                app.prefs.addBonusSlots(Prefs.REWARD_SLOTS)
                                Toast.makeText(context, "+${Prefs.REWARD_SLOTS} slots unlocked", Toast.LENGTH_SHORT).show()
                                showAdd = true
                            },
                            onUnavailable = {
                                Toast.makeText(context, "No video available right now. Try again shortly.", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }) { Text("Watch video (+${Prefs.REWARD_SLOTS})") }
            },
        )
    }
}

@Composable
private fun Header(
    pro: Boolean,
    searching: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.statusBarsPadding().padding(top = 14.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = VaultlyTheme.colors.muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your vault", style = MaterialTheme.typography.headlineLarge)
                    if (pro) {
                        Spacer(Modifier.width(10.dp))
                        ProBadge()
                    }
                }
            }
            RoundIcon(if (searching) Icons.Outlined.Close else Icons.Outlined.Search, "Search", onSearch)
            Spacer(Modifier.width(10.dp))
            RoundIcon(Icons.Outlined.Settings, "Settings", onSettings)
        }
        AnimatedVisibility(searching, enter = fadeIn(), exit = fadeOut()) {
            SearchField(query, onQuery, Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(VaultlyTheme.colors.card)
            .border(1.dp, VaultlyTheme.colors.hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(VaultlyTheme.colors.card)
            .border(1.dp, VaultlyTheme.colors.hairline, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = VaultlyTheme.colors.muted)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Search items, stores, serials", style = MaterialTheme.typography.bodyLarge, color = VaultlyTheme.colors.muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(VaultlyTheme.colors.gold),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeroCard(state: HomeState, pro: Boolean, limit: Int, onUpgrade: () -> Unit) {
    val colors = VaultlyTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.inkBrush)
            .border(1.dp, Palette.Gold.copy(alpha = 0.25f), shape),
    ) {
        // Decorative concentric rings, like a vault dial.
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.92f, size.height * 0.12f)
            listOf(70f, 120f, 170f, 220f).forEachIndexed { i, r ->
                drawCircle(
                    Palette.GoldLight.copy(alpha = 0.10f - i * 0.02f),
                    radius = r.dp.toPx(),
                    center = center,
                    style = Stroke(1.dp.toPx()),
                )
            }
        }
        Column(Modifier.padding(22.dp)) {
            Text("PROTECTED VALUE", style = MaterialTheme.typography.labelSmall, color = Palette.GoldLight.copy(alpha = 0.9f))
            Spacer(Modifier.height(6.dp))
            Text(
                Format.money(state.protectedValue, state.currency),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
            )
            Text(
                "Purchases still covered by a return window or warranty",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                HeroStat("${state.activeCount}", "Active", Modifier.weight(1f))
                HeroStat("${state.attention}", "Ending ≤ 7d", Modifier.weight(1f), highlight = state.attention > 0)
                HeroStat("${state.warranties}", "Warranties", Modifier.weight(1f))
            }
            if (!pro) {
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable(onClick = onUpgrade)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${state.activeCount} of $limit free slots used",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                            Box(
                                Modifier
                                    .fillMaxWidth((state.activeCount.toFloat() / limit).coerceIn(0.02f, 1f))
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.goldBrush),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Go Pro", style = MaterialTheme.typography.labelLarge, color = Palette.GoldLight)
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Palette.GoldLight)
                }
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier, highlight: Boolean = false) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (highlight) Color(0xFFFF8A80) else Color.White,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun FilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    val bg = if (selected) MaterialTheme.colorScheme.primary else VaultlyTheme.colors.card
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else VaultlyTheme.colors.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text("$count", style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun AddFab(onClick: () -> Unit) {
    val colors = VaultlyTheme.colors
    Row(
        Modifier
            .navigationBarsPadding()
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (colors.dark) colors.goldBrush else colors.inkBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (colors.dark) Palette.Ink else Color.White
        Icon(Icons.Outlined.Add, null, tint = fg)
        Spacer(Modifier.width(8.dp))
        Text("Add receipt", style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun AddOption(icon: ImageVector, title: String, subtitle: String, highlight: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val colors = VaultlyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (highlight) 0.9f else 0.5f))
            .border(1.dp, if (highlight) colors.gold.copy(alpha = 0.5f) else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                .background(if (highlight) colors.goldBrush else colors.inkBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (highlight) Palette.Ink else Palette.GoldLight)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = colors.muted)
    }
}

@Composable
private fun EmptyVault(onScan: () -> Unit, onGallery: () -> Unit, onManual: () -> Unit) {
    val colors = VaultlyTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(30.dp)).background(colors.inkBrush), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Shield, null, Modifier.size(46.dp), tint = Palette.GoldLight)
        }
        Spacer(Modifier.height(22.dp))
        Text("Start your vault", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Scan a receipt and Vaultly tracks the return window and warranty — then reminds you before they end.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(26.dp))
        PrimaryButton("Scan your first receipt", onScan, Modifier.fillMaxWidth(), icon = Icons.Outlined.DocumentScanner)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallAction(Icons.Outlined.PhotoLibrary, "From photos", onGallery, Modifier.weight(1f))
            SmallAction(Icons.Outlined.EditNote, "Enter manually", onManual, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .height(52.dp)
            .clip(shape)
            .background(VaultlyTheme.colors.card)
            .border(1.dp, VaultlyTheme.colors.hairline, shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun FilteredEmpty(filter: Filter, query: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (query.isNotBlank()) "Nothing matches “$query”" else when (filter) {
                Filter.RETURNS -> "No open return windows"
                Filter.WARRANTY -> "No active warranties"
                Filter.EXPIRED -> "Nothing expired"
                Filter.ARCHIVED -> "Nothing archived yet"
                Filter.ALL -> "No active items"
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text("Try another filter.", style = MaterialTheme.typography.bodyMedium, color = VaultlyTheme.colors.muted)
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
