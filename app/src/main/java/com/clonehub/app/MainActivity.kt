package com.clonehub.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clonehub.app.data.Clone
import com.clonehub.app.data.InstalledApp
import com.clonehub.app.ui.AppIcons
import com.clonehub.app.ui.theme.CloneHubTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CloneHubTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(vm)
                }
            }
        }
    }
}

private val Accents = listOf(
    0xFF6D5EF7, 0xFF9B4DFF, 0xFF4D8DFF, 0xFF18C2A6,
    0xFFFFB020, 0xFFFF6B6B, 0xFFEC4899, 0xFF22C55E,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clones by vm.clones.collectAsStateWithLifecycle()

    var showPicker by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Clone?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Header(count = clones.size)
            }
            if (clones.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    EmptyState()
                }
            }
            itemsIndexed(clones, key = { _, c -> c.id }) { _, clone ->
                CloneTile(
                    clone = clone,
                    icon = remember(clone.packageName) { vm.iconFor(clone.packageName) },
                    installed = remember(clone.packageName) { vm.isInstalled(clone.packageName) },
                    onOpen = {
                        if (!vm.launch(clone)) {
                            Toast.makeText(context, "Can't open — app not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRename = { renaming = clone },
                    onDelete = { vm.delete(clone.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        GradientFab(
            onClick = { showPicker = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding(),
        )
    }

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AppPicker(
                vm = vm,
                onPick = { app ->
                    vm.addClone(app, Accents.random())
                    showPicker = false
                    Toast.makeText(context, "Clone created", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    renaming?.let { target ->
        RenameDialog(
            clone = target,
            onDismiss = { renaming = null },
            onConfirm = { name, accent ->
                vm.rename(target.id, name)
                vm.recolor(target.id, accent)
                renaming = null
            },
        )
    }
}

@Composable
private fun Header(count: Int) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 20.dp, bottom = 12.dp)) {
        Text(
            "CloneHub",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (count == 0) "Create your first clone" else "$count clone${if (count == 1) "" else "s"} · tap to open, pencil to edit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF6D5EF7), Color(0xFF9B4DFF))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Add, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "No clones yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap + to pick an app, give it a custom\nname and color. It stays here for good.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun CloneTile(
    clone: Clone,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    installed: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileScale",
    )
    val accent = Color(clone.accent)

    Column(
        modifier = modifier
            .padding(4.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.06f)),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                ) { onOpen() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.52f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Image(icon, contentDescription = clone.label, modifier = Modifier.fillMaxSize())
                } else {
                    Box(
                        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            clone.label.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                    }
                }
            }

            // accent dot indicating a clone
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (installed) accent else Color(0xFF9AA0A6)),
            )

            androidx.compose.material3.Surface(
                onClick = { expanded = !expanded },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp),
            ) {
                Icon(
                    AppIcons.Edit, "Options",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(6.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            clone.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(expanded) {
            Row(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MiniAction(AppIcons.PlayArrow, "Open", accent, onOpen)
                MiniAction(AppIcons.Edit, "Rename", accent) { expanded = false; onRename() }
                MiniAction(AppIcons.Delete, "Delete", Color(0xFFFF6B6B)) { expanded = false; onDelete() }
            }
        }
    }
}

@Composable
private fun MiniAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = tint.copy(alpha = 0.14f),
        modifier = Modifier.size(32.dp),
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.padding(7.dp))
    }
}

@Composable
private fun GradientFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val size by animateDpAsState(if (pressed) 60.dp else 64.dp, tween(150), label = "fab")
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF6D5EF7), Color(0xFF9B4DFF))))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Add, "Add clone", tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun AppPicker(vm: MainViewModel, onPick: (InstalledApp) -> Unit) {
    val apps by vm.installed.collectAsStateWithLifecycle()
    val loading by vm.loadingApps.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) { vm.loadInstalledApps() }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding()) {
        Text("Choose an app to clone", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(AppIcons.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        when {
            loading && apps.isEmpty() -> Box(
                Modifier.fillMaxWidth().height(200.dp), Alignment.Center
            ) { androidx.compose.material3.CircularProgressIndicator() }

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(88.dp),
                modifier = Modifier.fillMaxWidth().height(420.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(filtered, key = { _, a -> a.packageName }) { _, app ->
                    PickerItem(app, remember(app.packageName) { vm.iconFor(app.packageName) }) {
                        onPick(app)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerItem(
    app: InstalledApp,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Image(icon, app.label, Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Text(app.label.take(1).uppercase(), fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RenameDialog(
    clone: Clone,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(clone.label) }
    var accent by rememberSaveable { mutableStateOf(clone.accent) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(name, accent) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Edit clone") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(name, accent) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Accent", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accents.forEach { c ->
                        val selected = c == accent
                        val ring by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            label = "ring",
                        )
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ring)
                                .padding(if (selected) 3.dp else 0.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .clickable { accent = c },
                        )
                    }
                }
            }
        },
    )
}
