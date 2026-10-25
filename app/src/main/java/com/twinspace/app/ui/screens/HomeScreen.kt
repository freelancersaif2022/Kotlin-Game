package com.twinspace.app.ui.screens

import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinspace.app.core.AppEntry
import com.twinspace.app.core.IconCache
import com.twinspace.app.core.SpaceStatus
import com.twinspace.app.ui.components.AppIcon
import com.twinspace.app.ui.components.EmptyState
import com.twinspace.app.ui.components.GradientButton
import com.twinspace.app.ui.components.StatusPill
import com.twinspace.app.ui.components.TwinIllustration
import com.twinspace.app.ui.theme.LocalExtraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    status: SpaceStatus,
    spaceName: String,
    apps: List<AppEntry>,
    loading: Boolean,
    icons: IconCache,
    nameOf: (AppEntry) -> String,
    snackbar: SnackbarHostState,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onAddApps: () -> Unit,
    onSettings: () -> Unit,
    onTogglePause: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    val paused = status == SpaceStatus.PAUSED
    val visible = remember(apps, query, nameOf) {
        if (query.isBlank()) apps
        else apps.filter { nameOf(it).contains(query, true) || it.packageName.contains(query, true) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { com.twinspace.app.ui.components.ProSnackbarHost(snackbar) },
        floatingActionButton = {
            AnimatedVisibility(apps.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                ExtendedFloatingActionButton(
                    onClick = onAddApps,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("Add apps") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                )
            }
        },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Column {
                        Text("Twin Space", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (apps.isEmpty()) "No clones yet" else "${apps.size} cloned app${if (apps.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (apps.size > 6) {
                        IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                            Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, "Search")
                        }
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 84.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "space") {
                SpaceCard(spaceName, status, apps.size, onTogglePause)
            }
            if (searching) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "search") {
                    SearchField(query, { query = it }, Modifier.padding(top = 8.dp))
                }
            }
            if (apps.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "label") {
                    Text(
                        if (query.isBlank()) "Your clones" else "${visible.size} result${if (visible.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
            }
            items(visible, key = { it.key }) { entry ->
                AppTile(
                    entry = entry,
                    name = nameOf(entry),
                    icons = icons,
                    dimmed = entry.frozen || paused,
                    onClick = { bounds -> onLaunch(entry, bounds) },
                    onLongClick = { onLongPress(entry) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (apps.isEmpty() && !loading) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    EmptyState(
                        title = "Your space is ready",
                        body = "Clone WhatsApp, Instagram, games or any other app to use a second account side by side.",
                        illustration = { TwinIllustration(size = 150.dp) },
                        action = {
                            GradientButton("Add your first app", onAddApps, icon = Icons.Rounded.Add)
                        },
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceCard(name: String, status: SpaceStatus, count: Int, onTogglePause: () -> Unit) {
    val extra = LocalExtraColors.current
    val paused = status == SpaceStatus.PAUSED
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(extra.heroStart, extra.heroEnd))),
    ) {
        // Decorative circles
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(160.dp)
                .scale(1.3f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f)),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 56.dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
        )
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(if (paused) "Paused" else "Active", if (paused) Color(0xFFFFD08A) else Color(0xFF9CF5DD))
            }
            Spacer(Modifier.height(14.dp))
            Text(name, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                if (paused) "Clones are asleep. Their data is safe."
                else "$count app${if (count == 1) "" else "s"} running in an isolated space",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(onClick = onTogglePause)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (paused) Icons.Rounded.PlayCircle else Icons.Rounded.PauseCircle,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (paused) "Resume space" else "Pause space", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search clones") },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    entry: AppEntry,
    name: String,
    icons: IconCache,
    dimmed: Boolean,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.6f, stiffness = 700f), label = "tile")
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = {
                    val r = coords?.takeIf { it.isAttached }?.boundsInWindow()
                    onClick(r?.let { Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) })
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .scale(scale)
                .onGloballyPositioned { coords = it },
        ) {
            AppIcon(entry, icons, size = 56.dp, showBadge = true, dimmed = dimmed)
            if (entry.frozen) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AcUnit, "Frozen", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(13.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
