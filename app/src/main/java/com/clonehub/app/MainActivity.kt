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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
                HomeScreen(vm)
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
    val context = LocalContext.current
    val clones by vm.clones.collectAsStateWithLifecycle()

    var showPicker by rememberSaveable { mutableStateOf(false) }
    var creatingFor by remember { mutableStateOf<InstalledApp?>(null) }
    var editing by remember { mutableStateOf<Clone?>(null) }

    fun pin(clone: Clone) {
        val ok = vm.pinShortcut(clone)
        Toast.makeText(
            context,
            if (ok) "Check your home screen to confirm the shortcut"
            else "Your launcher doesn't support adding shortcuts",
            Toast.LENGTH_SHORT,
        ).show()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header(count = clones.size) }

            if (clones.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyState() }
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
                    onPin = { pin(clone) },
                    onEdit = { editing = clone },
                    onDelete = { vm.delete(clone.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        ExtendedFab(
            onClick = { showPicker = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
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
                    showPicker = false
                    creatingFor = app
                },
            )
        }
    }

    creatingFor?.let { app ->
        CloneEditorDialog(
            title = "Create clone",
            initialName = vm.suggestName(app),
            initialAccent = Accents.first(),
            canPin = vm.canPinShortcuts(),
            icon = remember(app.packageName) { vm.iconFor(app.packageName) },
            onDismiss = { creatingFor = null },
            onConfirm = { name, accent, addToHome ->
                val clone = vm.addClone(app, name, accent)
                if (addToHome) pin(clone)
                creatingFor = null
                Toast.makeText(context, "Clone created", Toast.LENGTH_SHORT).show()
            },
        )
    }

    editing?.let { clone ->
        CloneEditorDialog(
            title = "Edit clone",
            initialName = clone.label,
            initialAccent = clone.accent,
            canPin = vm.canPinShortcuts(),
            icon = remember(clone.packageName) { vm.iconFor(clone.packageName) },
            showDelete = true,
            onDelete = { vm.delete(clone.id); editing = null },
            onDismiss = { editing = null },
            onConfirm = { name, accent, addToHome ->
                vm.rename(clone.id, name)
                vm.recolor(clone.id, accent)
                val updated = clone.copy(label = name, accent = accent)
                if (addToHome) pin(updated)
                editing = null
            },
        )
    }
}

@Composable
private fun Header(count: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 14.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF6D5EF7), Color(0xFF9B4DFF)))
            )
            .padding(horizontal = 22.dp, vertical = 22.dp),
    ) {
        Text(
            "CloneHub",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (count == 0) "Clone an app, give it a name, color & home-screen icon"
            else "$count clone${if (count == 1) "" else "s"} saved for life",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF6D5EF7), Color(0xFF9B4DFF)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Add, null, tint = Color.White, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("No clones yet", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap New clone to pick an app. Set a custom name\nand color, and optionally add it to your home screen.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun CloneTile(
    clone: Clone,
    icon: ImageBitmap?,
    installed: Boolean,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileScale",
    )
    val accent = Color(clone.accent)

    Column(
        modifier = modifier.padding(2.dp).scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            Box(
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.04f)),
                            start = Offset.Zero, end = Offset.Infinite,
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
                    .clickable(interactionSource = interaction, indication = null) { onOpen() },
                contentAlignment = Alignment.Center,
            ) {
                CloneGlyph(clone.label, icon, accent, Modifier.fillMaxWidth(0.5f))

                Box(
                    Modifier.align(Alignment.TopStart).padding(10.dp)
                        .size(9.dp).clip(CircleShape)
                        .background(if (installed) Color(0xFF22C55E) else Color(0xFF9AA0A6)),
                )

                Surface(
                    onClick = { expanded = !expanded },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 1.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(26.dp),
                ) {
                    Icon(
                        AppIcons.MoreVert, "Options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(5.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(7.dp))
        Text(
            clone.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(
            expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniAction(AppIcons.PlayArrow, "Open", accent) { expanded = false; onOpen() }
                MiniAction(AppIcons.Home, "Home", accent) { expanded = false; onPin() }
                MiniAction(AppIcons.Edit, "Edit", accent) { expanded = false; onEdit() }
                MiniAction(AppIcons.Delete, "Delete", Color(0xFFFF6B6B)) { expanded = false; onDelete() }
            }
        }
    }
}

@Composable
private fun CloneGlyph(label: String, icon: ImageBitmap?, accent: Color, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Image(icon, contentDescription = label, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(label.take(1).uppercase(), color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun MiniAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = tint.copy(alpha = 0.16f),
        modifier = Modifier.size(34.dp),
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun ExtendedFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val elevation by animateDpAsState(if (pressed) 2.dp else 8.dp, tween(150), label = "fab")
    Surface(
        shape = CircleShape,
        shadowElevation = elevation,
        modifier = modifier,
    ) {
        Row(
            Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF6D5EF7), Color(0xFF9B4DFF))))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("New clone", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AppPicker(vm: MainViewModel, onPick: (InstalledApp) -> Unit) {
    val apps by vm.installed.collectAsStateWithLifecycle()
    val loading by vm.loadingApps.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadInstalledApps() }

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
            ) { CircularProgressIndicator() }

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(88.dp),
                modifier = Modifier.fillMaxWidth().height(420.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
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
private fun PickerItem(app: InstalledApp, icon: ImageBitmap?, onClick: () -> Unit) {
    Column(
        Modifier.padding(6.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(8.dp),
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
        Text(app.label, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CloneEditorDialog(
    title: String,
    initialName: String,
    initialAccent: Long,
    canPin: Boolean,
    icon: ImageBitmap?,
    showDelete: Boolean = false,
    onDelete: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (name: String, accent: Long, addToHome: Boolean) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var accent by rememberSaveable { mutableStateOf(initialAccent) }
    var addToHome by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(name, accent, addToHome) }) { Text("Save") } },
        dismissButton = {
            if (showDelete) TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFFF6B6B)) }
            else TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CloneGlyph(name, icon, Color(accent), Modifier.size(44.dp))
                    Spacer(Modifier.width(14.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onConfirm(name, accent, addToHome) }),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text("Accent color", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Accents.forEach { c ->
                        val selected = c == accent
                        val ring by animateColorAsState(
                            if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            label = "ring",
                        )
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(ring)
                                .padding(if (selected) 3.dp else 0.dp)
                                .clip(CircleShape).background(Color(c))
                                .clickable { accent = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(AppIcons.Check, null, tint = Color.White,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (canPin) {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { addToHome = !addToHome }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(AppIcons.Home, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Add to Home screen", style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium)
                            Text("Creates a real launcher shortcut", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(checked = addToHome, onCheckedChange = { addToHome = it })
                    }
                }
            }
        },
    )
}
