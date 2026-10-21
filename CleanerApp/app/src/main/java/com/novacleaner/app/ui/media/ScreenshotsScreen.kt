package com.novacleaner.app.ui.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Format
import com.novacleaner.app.ui.components.BottomActionBar
import com.novacleaner.app.ui.components.CircleIconButton
import com.novacleaner.app.ui.components.CleaningOrb
import com.novacleaner.app.ui.components.ConfirmDialog
import com.novacleaner.app.ui.components.EmptyState
import com.novacleaner.app.ui.components.PermissionGate
import com.novacleaner.app.ui.components.ScanRadar
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.theme.Nova

@Composable
fun ScreenshotsScreen(vm: ScreenshotsViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val state by vm.state.collectAsStateWithLifecycle()
    val tool = Tool.SCREENSHOTS
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.delete.done) {
        if (state.delete.done) navigator.showResult(tool, state.delete.freedBytes, state.delete.freedCount)
    }

    ToolScaffold(
        title = tool.title,
        subtitle = if (state.loading) "Collecting screenshots…" else "${state.all.size} screenshots • ${Format.bytes(state.totalSize)}",
        onBack = { if (!state.delete.deleting) navigator.exitTool() },
        actions = {
            if (!state.loading && state.all.isNotEmpty()) CircleIconButton(Icons.Rounded.SelectAll, "Select all", vm::toggleAll)
        },
        bottomBar = {
            BottomActionBar(
                visible = state.selected.isNotEmpty() && !state.delete.deleting,
                text = "Delete ${state.selected.size} • ${Format.bytes(state.selectedSize)}",
                icon = Icons.Rounded.Delete,
                colors = listOf(Nova.colors.danger.copy(alpha = 0.85f), Nova.colors.danger),
                onClick = { confirm = true },
            )
        },
    ) {
        PermissionGate(
            AppPermission.STORAGE, "Allow photo access",
            "Nova Cleaner needs access to your files to show and remove old screenshots.",
            tool.icon, tool.colors,
        ) {
            LaunchedEffect(Unit) { vm.start() }
            val phase = when {
                state.loading -> 0
                state.delete.deleting -> 2
                else -> 1
            }
            AnimatedContent(phase, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "shots") { p ->
                when (p) {
                    0 -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        ScanRadar(null, tool.icon, tool.colors)
                        Spacer(Modifier.height(18.dp))
                        Text("Collecting screenshots…", style = MaterialTheme.typography.titleMedium)
                    }
                    2 -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CleaningOrb(state.delete.progress, Icons.Rounded.Delete, tool.colors)
                        Spacer(Modifier.height(20.dp))
                        Text("Removing screenshots…", style = MaterialTheme.typography.titleMedium)
                    }
                    else -> if (state.months.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(Icons.Rounded.Screenshot, "No screenshots", "You don't have any screenshots on this device.", colors = tool.colors)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.months.forEach { month ->
                                item(key = "h_${month.key}", span = { GridItemSpan(maxLineSpan) }) {
                                    val allSelected = month.items.all { it.id in state.selected }
                                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(month.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${month.items.size} • ${Format.bytes(month.items.sumOf { it.size })}",
                                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        TextButton(onClick = { vm.toggleMonth(month) }) {
                                            Text(if (allSelected) "Deselect" else "Select all", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                items(month.items, key = { it.id }) { shot ->
                                    PhotoTile(
                                        shot, shot.id in state.selected, isBest = false,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
                                        size = null,
                                    ) { vm.toggle(shot.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            "Delete ${state.selected.size} screenshots?",
            "${Format.bytes(state.selectedSize)} will be permanently removed from your device.",
            "Delete", vm::delete, { confirm = false },
        )
    }
}
