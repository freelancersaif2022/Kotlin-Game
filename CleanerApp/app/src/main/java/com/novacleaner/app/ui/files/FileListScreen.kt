package com.novacleaner.app.ui.files

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Format
import com.novacleaner.app.data.ApkStatus
import com.novacleaner.app.data.FileEntry
import com.novacleaner.app.data.FileKind
import com.novacleaner.app.ui.components.BottomActionBar
import com.novacleaner.app.ui.components.CheckDot
import com.novacleaner.app.ui.components.CircleIconButton
import com.novacleaner.app.ui.components.CleaningOrb
import com.novacleaner.app.ui.components.ConfirmDialog
import com.novacleaner.app.ui.components.EmptyState
import com.novacleaner.app.ui.components.FileThumb
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.PermissionGate
import com.novacleaner.app.ui.components.PillRow
import com.novacleaner.app.ui.components.ScanRadar
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.theme.Nova

@Composable
fun FileListScreen(mode: FileListMode, vm: FileListViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val state by vm.state.collectAsStateWithLifecycle()
    val tool = if (mode == FileListMode.LARGE) Tool.LARGE_FILES else Tool.APK
    var confirm by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val visible = state.visible(mode)

    LaunchedEffect(state.done) {
        if (state.done) navigator.showResult(tool, state.freedBytes, state.freedCount)
    }

    ToolScaffold(
        title = tool.title,
        subtitle = if (state.loading) "Scanning…" else "${visible.size} files • ${Format.bytes(visible.sumOf { it.size })}",
        onBack = { if (!state.deleting) navigator.exitTool() },
        actions = {
            if (!state.loading && visible.isNotEmpty()) {
                CircleIconButton(Icons.Rounded.SelectAll, "Select all", { vm.toggleAll(mode) })
                Spacer(Modifier.width(8.dp))
                Box {
                    CircleIconButton(Icons.Rounded.Sort, "Sort", { sortMenu = true })
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortMode.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.label, color = if (s == state.sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                                onClick = { vm.setSort(s); sortMenu = false },
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            BottomActionBar(
                visible = state.selected.isNotEmpty() && !state.deleting,
                text = "Delete ${state.selected.size} • ${Format.bytes(state.selectedSize)}",
                icon = Icons.Rounded.Delete,
                colors = listOf(Nova.colors.danger.copy(alpha = 0.85f), Nova.colors.danger),
                onClick = { confirm = true },
            )
        },
    ) {
        PermissionGate(
            AppPermission.STORAGE,
            title = "Allow file access",
            message = "Nova Cleaner needs access to your files to find what is using your storage.",
            icon = tool.icon,
            colors = tool.colors,
        ) {
            LaunchedEffect(Unit) { vm.start(mode) }
            val phase = when {
                state.loading -> 0
                state.deleting -> 2
                else -> 1
            }
            AnimatedContent(phase, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "files") { p ->
                when (p) {
                    0 -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        ScanRadar(progress = null, icon = tool.icon, colors = tool.colors)
                        Spacer(Modifier.height(20.dp))
                        Text(if (mode == FileListMode.LARGE) "Looking for large files…" else "Looking for APK files…", style = MaterialTheme.typography.titleMedium)
                    }
                    2 -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CleaningOrb(state.deleteProgress, Icons.Rounded.Delete, tool.colors)
                        Spacer(Modifier.height(20.dp))
                        Text("Deleting files…", style = MaterialTheme.typography.titleMedium)
                    }
                    else -> FileList(mode, state, visible, vm)
                }
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            title = "Delete ${state.selected.size} files?",
            message = "${Format.bytes(state.selectedSize)} will be permanently removed from your device. This can't be undone.",
            confirmText = "Delete",
            onConfirm = vm::delete,
            onDismiss = { confirm = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileList(mode: FileListMode, state: FileListState, visible: List<FileEntry>, vm: FileListViewModel) {
    Column(Modifier.fillMaxSize()) {
        if (mode == FileListMode.LARGE) {
            PillRow(SizeFilter.entries, state.sizeFilter, { it.label }, vm::setSizeFilter, Modifier.padding(vertical = 6.dp))
            val kinds = listOf<FileKind?>(null) + FileKind.entries
            PillRow(kinds, state.kindFilter, { it?.label ?: "All types" }, vm::setKindFilter, Modifier.padding(bottom = 6.dp))
        } else {
            PillRow(ApkFilter.entries, state.apkFilter, { it.label }, vm::setApkFilter, Modifier.padding(vertical = 6.dp))
        }
        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.FolderOpen,
                    title = "Nothing here",
                    message = if (mode == FileListMode.LARGE) "No files match these filters. Try a smaller size." else "No APK files found on your storage.",
                    colors = if (mode == FileListMode.LARGE) Tool.LARGE_FILES.colors else Tool.APK.colors,
                )
            }
            return
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(visible, key = { it.path }) { file ->
                FileRow(file, file.path in state.selected, Modifier.animateItem()) { vm.toggle(file.path) }
            }
        }
    }
}

@Composable
private fun FileRow(file: FileEntry, selected: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    NovaCard(modifier.fillMaxWidth(), padding = PaddingValues(0.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileThumb(file.path, file.kind)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(file.apk?.label ?: file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                val detail = file.apk?.let { apk ->
                    listOfNotNull(apk.versionName?.let { "v$it" }, apk.status.label).joinToString(" • ")
                } ?: "${Format.date(file.modified)} • ${file.path.substringBeforeLast('/').substringAfterLast('/')}"
                Text(detail, style = MaterialTheme.typography.bodySmall, color = statusColor(file), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(Format.bytes(file.size), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                CheckDot(selected, size = 22.dp)
            }
        }
    }
}

@Composable
private fun statusColor(file: FileEntry) = when (file.apk?.status) {
    ApkStatus.INSTALLED, ApkStatus.OUTDATED -> MaterialTheme.colorScheme.primary
    ApkStatus.DAMAGED -> Nova.colors.danger
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
