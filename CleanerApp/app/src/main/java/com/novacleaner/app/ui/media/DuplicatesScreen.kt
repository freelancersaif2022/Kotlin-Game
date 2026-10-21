package com.novacleaner.app.ui.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Format
import com.novacleaner.app.data.DuplicateGroup
import com.novacleaner.app.data.MediaItem
import com.novacleaner.app.ui.components.BottomActionBar
import com.novacleaner.app.ui.components.BytesHeadline
import com.novacleaner.app.ui.components.CheckDot
import com.novacleaner.app.ui.components.CleaningOrb
import com.novacleaner.app.ui.components.ConfirmDialog
import com.novacleaner.app.ui.components.EmptyState
import com.novacleaner.app.ui.components.FilterPill
import com.novacleaner.app.ui.components.MediaThumb
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.PermissionGate
import com.novacleaner.app.ui.components.ScanRadar
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.components.bounceClick
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.theme.Nova

@Composable
fun DuplicatesScreen(vm: DuplicatesViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val state by vm.state.collectAsStateWithLifecycle()
    val tool = Tool.DUPLICATES
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.delete.done) {
        if (state.delete.done) navigator.showResult(tool, state.delete.freedBytes, state.delete.freedCount)
    }

    ToolScaffold(
        title = tool.title,
        subtitle = if (state.loading) "Analysing photos…" else "${state.exact.size} duplicate • ${state.similar.size} similar groups",
        onBack = { if (!state.delete.deleting) navigator.exitTool() },
        bottomBar = {
            BottomActionBar(
                visible = !state.loading && state.selected.isNotEmpty() && !state.delete.deleting,
                text = "Delete ${state.selected.size} • ${Format.bytes(state.selectedSize)}",
                icon = Icons.Rounded.Delete,
                colors = listOf(Nova.colors.danger.copy(alpha = 0.85f), Nova.colors.danger),
                onClick = { confirm = true },
            )
        },
    ) {
        PermissionGate(
            AppPermission.STORAGE, "Allow photo access",
            "Nova Cleaner compares your photos on-device to find copies and near-identical shots.",
            tool.icon, tool.colors,
        ) {
            LaunchedEffect(Unit) { vm.start() }
            val phase = when {
                state.loading -> 0
                state.delete.deleting -> 2
                else -> 1
            }
            AnimatedContent(phase, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "dup") { p ->
                when (p) {
                    0 -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        ScanRadar(state.progress, tool.icon, tool.colors)
                        Spacer(Modifier.height(18.dp))
                        Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            state.current.ifEmpty { "Indexing photos…" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    2 -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CleaningOrb(state.delete.progress, Icons.Rounded.Delete, tool.colors)
                        Spacer(Modifier.height(20.dp))
                        Text("Removing photos…", style = MaterialTheme.typography.titleMedium)
                    }
                    else -> DuplicateResults(state, vm)
                }
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            "Delete ${state.selected.size} photos?",
            "${Format.bytes(state.selectedSize)} will be permanently removed. The best copy of each group is kept unless you selected it.",
            "Delete", vm::delete, { confirm = false },
        )
    }
}

@Composable
private fun DuplicateResults(state: DuplicatesState, vm: DuplicatesViewModel) {
    if (state.exact.isEmpty() && state.similar.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(Icons.Rounded.PhotoLibrary, "No duplicates found", "Your gallery has no identical or near-identical photos.", colors = Tool.DUPLICATES.colors)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(Tool.DUPLICATES.colors)).padding(20.dp),
            ) {
                Column {
                    Text("You can free up", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    BytesHeadline((state.exact + state.similar).sumOf { it.reclaimable }, color = Color.White, numberSize = 38)
                    Text("Best copy in each group is kept automatically", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("Duplicates (${state.exact.size})", !state.similarTab, { vm.setSimilarTab(false) })
                FilterPill("Similar (${state.similar.size})", state.similarTab, { vm.setSimilarTab(true) })
            }
        }
        if (state.groups.isEmpty()) {
            item {
                Text(
                    if (state.similarTab) "No similar photos found." else "No exact duplicates found.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        items(state.groups, key = { it.id }) { group -> GroupCard(group, state.selected, vm) }
    }
}

@Composable
private fun GroupCard(group: DuplicateGroup, selected: Set<Long>, vm: DuplicatesViewModel) {
    NovaCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 14.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${group.items.size} ${if (group.similar) "similar" else "identical"} photos", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${Format.dateTime(group.items.first().dateTaken)} • ${Format.bytes(group.reclaimable)} reclaimable",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .bounceClick { vm.smartSelect(group) }.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Smart select", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(group.items, key = { it.id }) { item ->
                PhotoTile(item, item.id in selected, isBest = item.id == group.best.id) { vm.toggle(item.id) }
            }
        }
    }
}

@Composable
fun PhotoTile(item: MediaItem, selected: Boolean, isBest: Boolean, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp? = 112.dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(shape)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick),
    ) {
        MediaThumb(item.path, Modifier.fillMaxSize(), corner = 16.dp)
        if (selected) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        CheckDot(selected, Modifier.align(Alignment.TopEnd).padding(6.dp), size = 22.dp, onDark = true)
        if (isBest) {
            Text(
                "BEST",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                    .clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(
            Format.bytes(item.size),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}
