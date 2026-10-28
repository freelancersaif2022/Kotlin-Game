package com.inkly.notes.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inkly.notes.AppContainer
import com.inkly.notes.data.Note
import com.inkly.notes.ui.components.AnimatedDialog
import com.inkly.notes.ui.components.CoachMarkOverlay
import com.inkly.notes.ui.components.CoachStep
import com.inkly.notes.ui.components.DeleteNoteDialog
import com.inkly.notes.ui.components.InklyLogo
import com.inkly.notes.ui.components.SheetAction
import com.inkly.notes.ui.components.formatShort
import com.inkly.notes.ui.components.pressScale
import com.inkly.notes.ui.theme.NoteColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val tourSteps = listOf(
    CoachStep(
        "fab", Icons.Rounded.Add, "Create a note",
        "Tap + to open a fresh lined page. Everything you type is saved automatically.",
    ),
    CoachStep(
        "search", Icons.Rounded.Search, "Find anything",
        "Search titles and text across all of your notes instantly.",
    ),
    CoachStep(
        "settings", Icons.Rounded.Settings, "Backup & appearance",
        "Connect Google Drive for automatic daily backups and restore, or switch light / dark mode.",
    ),
    CoachStep(
        "grid", Icons.Rounded.TouchApp, "Pin or delete",
        "Long-press any note to pin it to the top or delete it.",
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    container: AppContainer,
    onOpenNote: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: NotesViewModel = viewModel(factory = viewModelFactory { initializer { NotesViewModel(container.repo) } })
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsState()
    val prefs by container.prefs.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyStaggeredGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val targets = remember { mutableStateMapOf<String, Rect>() }
    var optionsFor by remember { mutableStateOf<Note?>(null) }
    var lastOptions by remember { mutableStateOf<Note?>(null) }
    if (optionsFor != null) lastOptions = optionsFor
    var deleteFor by remember { mutableStateOf<Note?>(null) }
    var lastDelete by remember { mutableStateOf<Note?>(null) }
    if (deleteFor != null) lastDelete = deleteFor

    var showTour by remember { mutableStateOf(false) }
    LaunchedEffect(prefs.tutorialDone, notes != null) {
        if (!prefs.tutorialDone && notes != null) {
            delay(650)
            showTour = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(
                        data,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary,
                    )
                }
            },
            floatingActionButton = {
                val interaction = remember { MutableInteractionSource() }
                ExtendedFloatingActionButton(
                    text = { Text("New note") },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "New note") },
                    expanded = fabExpanded,
                    onClick = { onOpenNote(0L) },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp, 10.dp),
                    modifier = Modifier
                        .pressScale(interaction, 0.92f)
                        .onGloballyPositioned { targets["fab"] = it.boundsInRoot() },
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Header(
                    count = notes?.size ?: 0,
                    query = query,
                    onQuery = { vm.query.value = it },
                    onSettings = onOpenSettings,
                    onTarget = { key, rect -> targets[key] = rect },
                )
                val list = notes
                when {
                    list == null -> Unit
                    list.isEmpty() -> EmptyState(searching = query.isNotBlank())
                    else -> {
                        val pinned = list.filter { it.pinned }
                        val others = list.filterNot { it.pinned }
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Adaptive(160.dp),
                            state = gridState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp,
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned {
                                    val r = it.boundsInRoot()
                                    targets["grid"] = Rect(r.left + 16f, r.top, r.right - 16f, r.top + minOf(r.height, 420f))
                                },
                        ) {
                            if (pinned.isNotEmpty()) {
                                item(key = "h_pinned", span = StaggeredGridItemSpan.FullLine) { SectionLabel("Pinned") }
                                items(pinned.size, key = { pinned[it].id }) { i ->
                                    NoteCard(pinned[i], { onOpenNote(pinned[i].id) }, { optionsFor = pinned[i] }, Modifier.animateItem())
                                }
                                if (others.isNotEmpty()) {
                                    item(key = "h_others", span = StaggeredGridItemSpan.FullLine) { SectionLabel("Others") }
                                }
                            }
                            items(others.size, key = { others[it].id }) { i ->
                                NoteCard(others[i], { onOpenNote(others[i].id) }, { optionsFor = others[i] }, Modifier.animateItem())
                            }
                        }
                    }
                }
            }
        }

        AnimatedDialog(visible = optionsFor != null, onDismiss = { optionsFor = null }) {
            val note = lastOptions
            Text(
                note?.title?.ifBlank { null } ?: "Untitled note",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
            )
            SheetAction(
                if (note?.pinned == true) Icons.Outlined.PushPin else Icons.Rounded.PushPin,
                if (note?.pinned == true) "Unpin note" else "Pin to top",
                MaterialTheme.colorScheme.primary,
            ) {
                note?.let(vm::togglePin); optionsFor = null
            }
            SheetAction(Icons.Rounded.DeleteOutline, "Delete note", MaterialTheme.colorScheme.error) {
                val n = note
                optionsFor = null
                if (n != null) scope.launch {
                    delay(160)
                    deleteFor = n
                }
            }
        }

        val pendingDelete = lastDelete
        DeleteNoteDialog(
            visible = deleteFor != null,
            title = pendingDelete?.title.orEmpty(),
            body = pendingDelete?.content.orEmpty(),
            tint = NoteColors.card(pendingDelete?.color ?: 0),
            onConfirm = {
                deleteFor?.let { n ->
                    vm.delete(n)
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar("Note deleted", duration = SnackbarDuration.Short)
                    }
                }
                deleteFor = null
            },
            onDismiss = { deleteFor = null },
        )

        AnimatedVisibility(showTour, enter = fadeIn(), exit = fadeOut()) {
            val steps = remember { tourSteps.filter { targets.containsKey(it.target) } }
            if (steps.isNotEmpty()) {
                CoachMarkOverlay(steps, targets) {
                    showTour = false
                    container.prefs.setTutorialDone(true)
                }
            }
        }
    }
}

@Composable
private fun Header(
    count: Int,
    query: String,
    onQuery: (String) -> Unit,
    onSettings: () -> Unit,
    onTarget: (String, Rect) -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            InklyLogo(40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("My Notes", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (count == 1) "1 note" else "$count notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.onGloballyPositioned { onTarget("settings", it.boundsInRoot()) },
            ) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .onGloballyPositioned { onTarget("search", it.boundsInRoot()) }
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search notes", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(query.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = { onQuery("") }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, "Clear", modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.pressScale(interaction, 0.97f),
        shape = RoundedCornerShape(22.dp),
        color = NoteColors.card(note.color),
        border = if (note.color == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(
            Modifier
                .combinedClickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .padding(16.dp)
        ) {
            if (note.title.isNotBlank() || note.pinned) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        note.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.pinned) {
                        Icon(
                            Icons.Rounded.PushPin, "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp).size(16.dp),
                        )
                    }
                }
            }
            if (note.content.isNotBlank()) {
                if (note.title.isNotBlank() || note.pinned) Spacer(Modifier.height(6.dp))
                Text(
                    note.content.take(400),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                formatShort(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    val t = rememberInfiniteTransition(label = "empty")
    val bob by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "bob")
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp).padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.graphicsLayer { translationY = bob * density }) {
            if (searching) {
                Box(
                    Modifier.size(96.dp).clip(RoundedCornerShape(30.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                }
            } else {
                InklyLogo(96.dp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (searching) "No matching notes" else "Your canvas is empty",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (searching) "Try a different word or phrase." else "Tap + to write your first note.\nIt saves itself as you type.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
