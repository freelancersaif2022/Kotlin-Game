package com.novacleaner.app.ui.junk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Format
import com.novacleaner.app.data.JunkCategory
import com.novacleaner.app.ui.components.BottomActionBar
import com.novacleaner.app.ui.components.BytesHeadline
import com.novacleaner.app.ui.components.CheckDot
import com.novacleaner.app.ui.components.CleaningOrb
import com.novacleaner.app.ui.components.EmptyState
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.GradientButton
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.PermissionGate
import com.novacleaner.app.ui.components.ScanRadar
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.components.bounceClick
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool

fun JunkCategory.icon(): ImageVector = when (this) {
    JunkCategory.CACHE -> Icons.Rounded.Cached
    JunkCategory.THUMBNAILS -> Icons.Rounded.Image
    JunkCategory.TEMP -> Icons.Rounded.HourglassEmpty
    JunkCategory.LOGS -> Icons.Rounded.Article
    JunkCategory.APKS -> Icons.Rounded.Android
    JunkCategory.EMPTY_FOLDERS -> Icons.Rounded.FolderOff
}

fun JunkCategory.colors(): List<Color> = when (this) {
    JunkCategory.CACHE -> Tool.JUNK.colors
    JunkCategory.THUMBNAILS -> Tool.DUPLICATES.colors
    JunkCategory.TEMP -> Tool.LARGE_FILES.colors
    JunkCategory.LOGS -> Tool.DATA_USAGE.colors
    JunkCategory.APKS -> Tool.APK.colors
    JunkCategory.EMPTY_FOLDERS -> Tool.SCREENSHOTS.colors
}

@Composable
fun JunkScreen(vm: JunkViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val state by vm.state.collectAsStateWithLifecycle()
    val tool = Tool.JUNK

    LaunchedEffect(state.phase) {
        if (state.phase == JunkPhase.DONE) navigator.showResult(tool, state.freedBytes, state.freedCount)
    }

    ToolScaffold(
        title = tool.title,
        subtitle = when (state.phase) {
            JunkPhase.SCANNING -> "Scanning storage…"
            JunkPhase.RESULTS -> "${Format.bytes(state.totalFound)} found"
            JunkPhase.CLEANING, JunkPhase.DONE -> "Cleaning…"
            JunkPhase.EMPTY -> "All clean"
        },
        onBack = { if (state.phase != JunkPhase.CLEANING) navigator.exitTool() },
        bottomBar = {
            BottomActionBar(
                visible = state.phase == JunkPhase.RESULTS && state.selected.isNotEmpty(),
                text = "Clean ${Format.bytes(state.selectedSize)}",
                icon = Icons.Rounded.CleaningServices,
                onClick = vm::clean,
            )
        },
    ) {
        PermissionGate(
            permission = AppPermission.STORAGE,
            title = "Allow file access to clean junk",
            message = "Nova Cleaner needs access to your files to find cache, temporary files and leftovers.",
            icon = tool.icon,
            colors = tool.colors,
        ) {
            LaunchedEffect(Unit) { vm.start() }
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                label = "junkPhase",
            ) { phase ->
                when (phase) {
                    JunkPhase.SCANNING -> ScanningContent(state)
                    JunkPhase.RESULTS -> ResultsContent(state, vm, onReviewApps = { navigator.openTool(Tool.APPS) })
                    JunkPhase.CLEANING, JunkPhase.DONE -> CleaningContent(state)
                    JunkPhase.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Rounded.CheckCircle,
                            title = "Sparkling clean!",
                            message = "We couldn't find any junk files right now. Check back after using your phone for a while.",
                            action = { GradientButton("Done", navigator::exitTool, Modifier.fillMaxWidth()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningContent(state: JunkUiState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        ScanRadar(progress = state.progress, icon = Tool.JUNK.icon, colors = Tool.JUNK.colors)
        Spacer(Modifier.height(16.dp))
        Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.displaySmall)
        Text(
            state.currentPath.ifEmpty { "Preparing…" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text("${state.scannedFiles} files checked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        NovaCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 6.dp)) {
            JunkCategory.entries.forEachIndexed { i, cat ->
                val done = state.progress >= 0.97f
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlossyIcon(cat.icon(), cat.colors(), size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(cat.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        Format.bytes(state.liveFound[cat] ?: 0L),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    if (done) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    } else {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = cat.colors().last())
                    }
                }
                if (i < JunkCategory.entries.lastIndex) HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ResultsContent(state: JunkUiState, vm: JunkViewModel, onReviewApps: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(Tool.JUNK.colors))
                    .padding(22.dp),
            ) {
                Column {
                    Text("Junk found", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    BytesHeadline(state.totalFound, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Selected ${Format.bytes(state.selectedSize)} • ${state.selectedCount} items",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }
        items(state.categories, key = { it.category.name }) { cat ->
            CategoryCard(cat, state.selected, cat.category in state.expanded, vm)
        }
        if (state.systemCache > 0) {
            item {
                NovaCard(Modifier.fillMaxWidth(), onClick = onReviewApps) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("App cache: ${Format.bytes(state.systemCache)}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Android manages this cache per app. Review the biggest ones in App Manager.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("Review", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CategoryCard(result: CategoryResult, selected: Set<String>, expanded: Boolean, vm: JunkViewModel) {
    val selectedInCat = result.items.count { it.path in selected }
    val allSelected = selectedInCat == result.items.size
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    NovaCard(Modifier.fillMaxWidth().animateContentSize(), padding = PaddingValues(0.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { vm.toggleExpanded(result.category) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlossyIcon(result.category.icon(), result.category.colors(), size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(result.category.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${result.items.size} items • ${Format.bytes(result.size)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.rotate(arrow), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.bounceClick { vm.toggleCategory(result) }) {
                CheckDot(allSelected)
            }
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Text(
                    result.category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                )
                result.items.take(60).forEach { item ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.toggleItem(item.path) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(Format.bytes(item.size), item.note).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        CheckDot(item.path in selected, size = 20.dp)
                    }
                }
                if (result.items.size > 60) {
                    Text(
                        "+ ${result.items.size - 60} more",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CleaningContent(state: JunkUiState) {
    val remaining = (state.selectedSize * (1f - state.cleanProgress)).toLong()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CleaningOrb(state.cleanProgress, Icons.Rounded.CleaningServices, Tool.JUNK.colors)
        Spacer(Modifier.height(24.dp))
        Text("Cleaning junk…", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "${Format.bytes(remaining)} remaining",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
