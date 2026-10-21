package com.novacleaner.app.ui.apps

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Format
import com.novacleaner.app.data.AppInfo
import com.novacleaner.app.ui.components.AppIcon
import com.novacleaner.app.ui.components.BottomActionBar
import com.novacleaner.app.ui.components.CheckDot
import com.novacleaner.app.ui.components.CircleIconButton
import com.novacleaner.app.ui.components.ConfirmDialog
import com.novacleaner.app.ui.components.EmptyState
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.PillRow
import com.novacleaner.app.ui.components.ScanRadar
import com.novacleaner.app.ui.components.ShimmerBox
import com.novacleaner.app.ui.components.StatChip
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.components.bounceClick
import com.novacleaner.app.ui.components.rememberPermissionRequester
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.theme.Nova

@Composable
fun AppManagerScreen(vm: AppManagerViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val tool = Tool.APPS
    var sortMenu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val uninstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.onUninstallReturned()
    }

    LaunchedEffect(Unit) { vm.start() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshIfNeeded() }

    // Walk the uninstall queue one system dialog at a time.
    val head = state.uninstallQueue.firstOrNull()
    LaunchedEffect(head, state.uninstallQueue.size) {
        if (head != null) {
            AppGraph.appOpen.skipNextResume()
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$head"))
            runCatching { uninstallLauncher.launch(intent) }.onFailure { vm.onUninstallReturned() }
        }
    }
    LaunchedEffect(state.done) {
        if (state.done) navigator.showResult(tool, state.uninstalledBytes, state.uninstalledCount)
    }

    ToolScaffold(
        title = tool.title,
        subtitle = if (state.loading) "Loading apps…" else "${state.visible.size} apps • ${Format.bytes(state.visible.sumOf { it.totalBytes })}",
        onBack = navigator::exitTool,
        actions = {
            CircleIconButton(if (state.searching) Icons.Rounded.Close else Icons.Rounded.Search, "Search", vm::toggleSearch)
            Spacer(Modifier.width(8.dp))
            Box {
                CircleIconButton(Icons.Rounded.Sort, "Sort", { sortMenu = true })
                DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                    AppSort.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label, color = if (s == state.sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                            onClick = { vm.setSort(s); sortMenu = false },
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomActionBar(
                visible = state.selected.isNotEmpty() && state.filter != AppFilter.SYSTEM,
                text = "Uninstall ${state.selected.size} • ${Format.bytes(state.selectedSize)}",
                icon = Icons.Rounded.Delete,
                colors = listOf(Nova.colors.danger.copy(alpha = 0.85f), Nova.colors.danger),
                onClick = { confirm = true },
            )
        },
    ) {
        AnimatedContent(state.loading, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "apps") { loading ->
            if (loading) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ScanRadar(null, tool.icon, tool.colors, diameter = 200.dp)
                    Spacer(Modifier.height(18.dp))
                    Text("Reading installed apps…", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    repeat(3) {
                        ShimmerBox(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().height(64.dp))
                    }
                }
            } else {
                AppList(state, vm)
            }
        }
    }

    state.detail?.let { app ->
        AppDetailSheet(
            app = app,
            onDismiss = { vm.showDetail(null) },
            onUninstall = { vm.startUninstall(listOf(app.packageName)) },
            onOpen = {
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    AppGraph.appOpen.skipNextResume()
                    runCatching { context.startActivity(it) }
                }
            },
            onSettings = {
                AppGraph.appOpen.skipNextResume()
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                }
            },
        )
    }

    if (confirm) {
        ConfirmDialog(
            "Uninstall ${state.selected.size} apps?",
            "Android will ask you to confirm each app. ${Format.bytes(state.selectedSize)} will be freed.",
            "Uninstall", { vm.startUninstall(state.selected) }, { confirm = false },
        )
    }
}

@Composable
private fun AppList(state: AppsState, vm: AppManagerViewModel) {
    val requestUsage = rememberPermissionRequester(AppPermission.USAGE)
    Column(Modifier.fillMaxSize()) {
        AnimatedVisibility(state.searching, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Nova.colors.cardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (state.query.isEmpty()) Text("Search apps", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                        value = state.query,
                        onValueChange = vm::setQuery,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        PillRow(AppFilter.entries, state.filter, { it.label }, vm::setFilter, Modifier.padding(vertical = 6.dp))
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!state.hasUsage) {
                item {
                    NovaCard(Modifier.fillMaxWidth(), onClick = requestUsage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BarChart, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("See exact sizes & last used", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Allow usage access to show data, cache and when you last opened each app.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("Allow", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            if (state.visible.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Apps, "No apps here",
                        if (state.filter == AppFilter.UNUSED) "Every app has been used in the last 30 days." else "Nothing matches your search.",
                        colors = Tool.APPS.colors,
                    )
                }
            }
            items(state.visible, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    sort = state.sort,
                    selectable = !app.isSystem,
                    selected = app.packageName in state.selected,
                    onToggle = { vm.toggle(app.packageName) },
                    onClick = { vm.showDetail(app) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    sort: AppSort,
    selectable: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NovaCard(modifier.fillMaxWidth(), padding = PaddingValues(0.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.packageName, size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                val detail = when (sort) {
                    AppSort.INSTALLED -> "Installed ${Format.date(app.installTime)}"
                    AppSort.UPDATED -> "Updated ${Format.date(app.updateTime)}"
                    AppSort.LAST_USED -> "Last used ${if (app.lastUsed > 0) Format.relative(app.lastUsed) else "unknown"}"
                    AppSort.CACHE -> "Cache ${Format.bytes(app.cacheBytes)}"
                    AppSort.NAME -> "Version ${app.versionName.ifBlank { "—" }}"
                    AppSort.SIZE -> if (app.sizesExact) "App ${Format.bytes(app.appBytes)} • Data ${Format.bytes(app.dataBytes)}" else "Installed ${Format.date(app.installTime)}"
                }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.bytes(if (sort == AppSort.CACHE) app.cacheBytes else app.totalBytes),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (selectable) {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.bounceClick(onClick = onToggle)) { CheckDot(selected, size = 22.dp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onUninstall: () -> Unit,
    onOpen: () -> Unit,
    onSettings: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val canOpen = remember(app.packageName) { context.packageManager.getLaunchIntentForPackage(app.packageName) != null }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.packageName, size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "v${app.versionName.ifBlank { "—" }} • ${app.packageName}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            val accent = Tool.APPS.colors.last()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("App", Format.bytes(app.appBytes), Modifier.weight(1f), accent)
                StatChip("Data", if (app.sizesExact) Format.bytes((app.dataBytes - app.cacheBytes).coerceAtLeast(0)) else "—", Modifier.weight(1f), accent)
                StatChip("Cache", if (app.sizesExact) Format.bytes(app.cacheBytes) else "—", Modifier.weight(1f), accent)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("Installed", Format.date(app.installTime), Modifier.weight(1f), Tool.LARGE_FILES.colors.last())
                StatChip("Updated", Format.date(app.updateTime), Modifier.weight(1f), Tool.LARGE_FILES.colors.last())
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("Last used", if (app.lastUsed > 0) Format.relative(app.lastUsed) else "Unknown", Modifier.weight(1f), Tool.SPEED.colors.last())
                StatChip("Total size", Format.bytes(app.totalBytes), Modifier.weight(1f), Tool.SPEED.colors.last())
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (canOpen) SheetAction(Icons.Rounded.Launch, "Open", MaterialTheme.colorScheme.primary, Modifier.weight(1f), onOpen)
                SheetAction(Icons.Rounded.Info, if (app.cacheBytes > 0) "Clear cache" else "App info", Tool.LARGE_FILES.colors.last(), Modifier.weight(1f), onSettings)
                if (!app.isSystem) SheetAction(Icons.Rounded.Delete, "Uninstall", Nova.colors.danger, Modifier.weight(1f), onUninstall)
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.12f))
            .bounceClick(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
