package com.flashshare.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.container
import com.flashshare.app.data.Category
import com.flashshare.app.data.FileBrowser
import com.flashshare.app.data.FileEntry
import com.flashshare.app.data.ShareItem
import com.flashshare.app.transfer.Mode
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.util.Perms
import com.flashshare.app.util.formatDate
import com.flashshare.app.util.formatDuration
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.toast
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class PickTab(val title: String, val cat: Category) {
    PHOTOS("Photos", Category.PHOTO), VIDEOS("Videos", Category.VIDEO), APPS("Apps", Category.APP),
    MUSIC("Music", Category.MUSIC), FILES("Files", Category.FILE)
}

/** A thumbnail flying from where it was tapped into the selection bar, flipping on the way. */
private class Flight(val id: Long, val item: ShareItem, val from: Rect) {
    val p = Animatable(0f)
}

/** Shared state so every list cell can launch a flight. */
private class FlightBoard {
    val flights = mutableStateListOf<Flight>()
    var target by mutableStateOf(Offset.Unspecified)
    fun launch(item: ShareItem, from: Rect) { if (from != Rect.Zero) flights += Flight(System.nanoTime(), item, from) }
}

/** mode: "send" → scan next, "group" → host a group, "more" → add to the current connection. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(mode: String, nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val selection by c.selection.items.collectAsStateWithLifecycle()
    val tabs = PickTab.entries
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    var showSelected by remember { mutableStateOf(false) }
    val board = remember { FlightBoard() }

    val photos by produceState<List<ShareItem>?>(null) { value = c.media.photos() }
    val videos by produceState<List<ShareItem>?>(null) { value = c.media.videos() }
    val apps by produceState<List<ShareItem>?>(null) { value = c.media.apps() }
    val music by produceState<List<ShareItem>?>(null) { value = c.media.music() }

    val toggle: (ShareItem, Rect) -> Unit = { item, rect ->
        val adding = !c.selection.isSelected(item)
        c.selection.toggle(item)
        if (adding) board.launch(item, rect)
    }

    val title = when (mode) { "group" -> "Pick files for group"; "more" -> "Send more files"; else -> "Select files" }
    // Leaving the picker with Back discards the selection (everything gets unticked).
    val leave = {
        c.selection.clear()
        nav.popBackStack()
        Unit
    }
    BackHandler(!showSelected, onBack = leave)

    Box(Modifier.fillMaxSize()) {
        AppScaffold(
            title = title,
            onBack = leave,
            actions = { if (selection.isNotEmpty()) TextButton(onClick = { c.selection.clear() }) { Text("Clear") } },
            bottomBar = {
                AnimatedVisibility(selection.isNotEmpty(), enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                    SelectionBar(
                        count = selection.size,
                        size = selection.values.sumOf { it.size },
                        preview = selection.values.toList().takeLast(3).reversed(),
                        action = when (mode) { "group" -> "Start group"; else -> "Send" },
                        onTarget = { board.target = it },
                        onShow = { showSelected = true },
                    ) {
                        val items = c.selection.list
                        when (mode) {
                            "group" -> {
                                c.transfers.startHosting(Mode.GROUP_HOST, items)
                                nav.navigate(Routes.GROUP) { popUpTo(Routes.HOME) }
                            }
                            "more" -> {
                                c.transfers.send(items)
                                c.selection.clear()
                                nav.popBackStack()
                            }
                            else -> nav.navigate(Routes.scan("send"))
                        }
                    }
                }
            },
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = pager.currentPage,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {},
                ) {
                    tabs.forEachIndexed { i, t ->
                        val count = selection.values.count { it.category == t.cat || (t == PickTab.FILES && it.category in listOf(Category.FILE, Category.CONTACT)) }
                        Tab(
                            selected = pager.currentPage == i,
                            onClick = { scope.launch { pager.animateScrollToPage(i) } },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(t.title, fontWeight = if (pager.currentPage == i) FontWeight.Bold else FontWeight.Medium)
                                    AnimatedVisibility(count > 0, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                                        Box(Modifier.padding(start = 6.dp).clip(CircleShape).background(Brand.main).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                            Text("$count", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
                if (Perms.mediaPartial(ctx)) {
                    Text(
                        "Showing only the photos you allowed. Change it in Settings › Apps › FlashShare › Permissions.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
                HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1, key = { tabs[it].name }) { page ->
                    when (tabs[page]) {
                        PickTab.PHOTOS -> MediaGrid(photos, selection, Category.PHOTO, toggle)
                        PickTab.VIDEOS -> MediaGrid(videos, selection, Category.VIDEO, toggle)
                        PickTab.APPS -> AppsGrid(apps, selection, toggle)
                        PickTab.MUSIC -> MusicList(music, selection, toggle)
                        PickTab.FILES -> FileBrowserTab(selection, toggle)
                    }
                }
            }
        }
        FlightLayer(board)
    }

    if (showSelected) {
        SelectedSheet(selection.values.toList(), onRemove = { c.selection.toggle(it) }, onClear = { c.selection.clear(); showSelected = false }, onDismiss = { showSelected = false })
    }
}

@Composable
private fun FlightLayer(board: FlightBoard) {
    val density = LocalDensity.current
    val endSize = with(density) { 40.dp.toPx() }
    Box(Modifier.fillMaxSize()) {
        board.flights.forEach { f ->
            key(f.id) {
                LaunchedEffect(f.id) {
                    f.p.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
                    board.flights.remove(f)
                }
                val p = f.p.value
                val target = if (board.target.isSpecified) board.target else Offset(with(density) { 60.dp.toPx() }, f.from.center.y + 900f)
                val start = f.from.center
                // quadratic curve with a lift so the thumbnail arcs up before dropping into the bar
                val ctrl = Offset((start.x + target.x) / 2, minOf(start.y, target.y) - with(density) { 120.dp.toPx() })
                val x = (1 - p) * (1 - p) * start.x + 2 * (1 - p) * p * ctrl.x + p * p * target.x
                val y = (1 - p) * (1 - p) * start.y + 2 * (1 - p) * p * ctrl.y + p * p * target.y
                val sizePx = f.from.width + (endSize - f.from.width) * p
                val sizeDp = with(density) { sizePx.toDp() }
                Box(
                    Modifier.offset { IntOffset((x - sizePx / 2).roundToInt(), (y - sizePx / 2).roundToInt()) }
                        .size(sizeDp)
                        .graphicsLayer { rotationY = 360f * p; cameraDistance = 10f * density.density; alpha = if (p > 0.9f) (1f - p) * 10f else 1f }
                        .shadow(12.dp, RoundedCornerShape(12.dp)),
                ) {
                    FileThumb(f.item.uri.toString(), f.item.category, sizeDp, packageName = f.item.packageName, name = f.item.name, corner = 12.dp)
                }
            }
        }
    }
}

private val Offset.isSpecified: Boolean get() = this != Offset.Unspecified && x.isFinite() && y.isFinite()

@Composable
private fun SelectAllHeader(items: List<ShareItem>, selection: Map<String, ShareItem>, label: String) {
    val ctx = LocalContext.current
    val c = ctx.container
    val allSelected = items.isNotEmpty() && items.all { selection.containsKey(it.key) }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${items.size} $label • ${formatSize(items.sumOf { it.size })}", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        TextButton(onClick = { if (allSelected) c.selection.removeAll(items) else c.selection.addAll(items) }) {
            Icon(if (allSelected) Icons.Rounded.RemoveDone else Icons.Rounded.DoneAll, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (allSelected) "Deselect all" else "Select all")
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Tracks a cell's on-screen bounds so a flight can start from it. */
@Composable
private fun rememberBounds(): MutableState<Rect> = remember { mutableStateOf(Rect.Zero) }

@Composable
private fun MediaGrid(items: List<ShareItem>?, selection: Map<String, ShareItem>, cat: Category, onToggle: (ShareItem, Rect) -> Unit) {
    if (items == null) return Loading()
    if (items.isEmpty()) return EmptyState(categoryIcon(cat), "Nothing here", "No ${cat.label.lowercase()} found on this phone.")
    Column {
        SelectAllHeader(items, selection, cat.label.lowercase())
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(items, key = { it.key }) { item ->
                val sel = selection.containsKey(item.key)
                val bounds = rememberBounds()
                val sc by animateFloatAsState(if (sel) 0.88f else 1f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "sel")
                Box(
                    Modifier.aspectRatio(1f).onGloballyPositioned { bounds.value = it.boundsInRoot() }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onToggle(item, bounds.value) },
                ) {
                    FileThumb(item.uri.toString(), cat, 200.dp, Modifier.fillMaxSize().scale(sc), corner = 12.dp, name = item.name)
                    if (cat == Category.VIDEO && item.durationMs > 0) {
                        Text(formatDuration(item.durationMs), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                    CheckMark(sel, Modifier.align(Alignment.TopEnd).padding(7.dp))
                }
            }
        }
    }
}

@Composable
private fun CheckMark(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(24.dp).clip(CircleShape)
            .background(if (selected) Brand.main else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.25f))))
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.animation.AnimatedVisibility(selected, enter = scaleIn(spring(dampingRatio = 0.4f)), exit = scaleOut()) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AppsGrid(items: List<ShareItem>?, selection: Map<String, ShareItem>, onToggle: (ShareItem, Rect) -> Unit) {
    if (items == null) return Loading()
    if (items.isEmpty()) return EmptyState(Icons.Rounded.Android, "No apps", "Installed apps will show up here.")
    Column {
        SelectAllHeader(items, selection, "apps")
        LazyVerticalGrid(
            GridCells.Adaptive(88.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.key }) { item ->
                val sel = selection.containsKey(item.key)
                val bounds = rememberBounds()
                val sc by animateFloatAsState(if (sel) 0.9f else 1f, spring(dampingRatio = 0.55f), label = "a")
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onToggle(item, bounds.value) }.padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.onGloballyPositioned { bounds.value = it.boundsInRoot() }) {
                        AppIcon(item.packageName ?: "", 54.dp, Modifier.scale(sc))
                        CheckMark(sel, Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).scale(0.8f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.name.removeSuffix(".apk"), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatSize(item.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MusicList(items: List<ShareItem>?, selection: Map<String, ShareItem>, onToggle: (ShareItem, Rect) -> Unit) {
    if (items == null) return Loading()
    if (items.isEmpty()) return EmptyState(Icons.Rounded.MusicNote, "No music", "Songs on this phone will show up here.")
    Column {
        SelectAllHeader(items, selection, "songs")
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            items(items, key = { it.key }) { item ->
                val bounds = rememberBounds()
                ItemRow(
                    thumb = { FileThumb(item.uri.toString(), item.category, 48.dp, Modifier.onGloballyPositioned { bounds.value = it.boundsInRoot() }, name = item.name) },
                    title = item.name.substringBeforeLast('.'),
                    subtitle = listOf(item.subtitle, formatSize(item.size), if (item.durationMs > 0) formatDuration(item.durationMs) else "").filter { it.isNotEmpty() }.joinToString(" • "),
                    selected = selection.containsKey(item.key),
                ) { onToggle(item, bounds.value) }
            }
        }
    }
}

@Composable
private fun ItemRow(thumb: @Composable () -> Unit, title: String, subtitle: String, selected: Boolean, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent)
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        thumb()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (trailing != null) trailing() else CheckMark(selected)
    }
}

/** Browse every folder of the phone and pick files or whole folders. */
@Composable
private fun FileBrowserTab(selection: Map<String, ShareItem>, onToggle: (ShareItem, Rect) -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    var allowed by remember { mutableStateOf(FileBrowser.canBrowse(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = FileBrowser.canBrowse(ctx) }
    var dir by remember { mutableStateOf(FileBrowser.root) }
    var entries by remember { mutableStateOf<List<FileEntry>?>(null) }
    val folderKeys = remember { mutableStateMapOf<String, List<ShareItem>>() }
    var expanding by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(dir, allowed) { entries = null; if (allowed) entries = FileBrowser.list(dir) }
    BackHandler(allowed && dir != FileBrowser.root) { dir = dir.parentFile ?: FileBrowser.root }

    if (!allowed) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            GlossyIcon(Icons.Rounded.FolderOpen, IconBrushes.orange, 84.dp)
            Spacer(Modifier.height(16.dp))
            Text("See every file on your phone", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Allow “All files access” to browse folders like Download, DCIM, WhatsApp and documents.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            GradientButton("Allow access", icon = Icons.Rounded.LockOpen, onClick = { requestAllFiles(ctx) })
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        // breadcrumb
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            FileBrowser.pretty(dir).forEachIndexed { i, (name, f) ->
                if (i > 0) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                val last = f == dir
                Text(name, style = MaterialTheme.typography.labelLarge,
                    color = if (last) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (last) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { dir = f }.padding(horizontal = 6.dp, vertical = 4.dp))
            }
        }
        val list = entries
        when {
            list == null -> Loading()
            list.isEmpty() -> EmptyState(Icons.Rounded.FolderOff, "Empty folder", "Nothing to show here.")
            else -> LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
                items(list, key = { it.file.path }) { e ->
                    val bounds = rememberBounds()
                    if (e.isDir) {
                        val sel = folderKeys.containsKey(e.file.path)
                        ItemRow(
                            thumb = { GlossyIcon(Icons.Rounded.Folder, IconBrushes.orange, 48.dp, Modifier.onGloballyPositioned { bounds.value = it.boundsInRoot() }, corner = 14.dp) },
                            title = e.name, subtitle = "${e.children} items • ${formatDate(e.modified)}", selected = sel,
                            trailing = {
                                if (expanding == e.file.path) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                else Box(Modifier.clip(CircleShape).clickable {
                                    if (sel) {
                                        c.selection.removeAll(folderKeys.remove(e.file.path).orEmpty())
                                    } else scope.launch {
                                        expanding = e.file.path
                                        val items = FileBrowser.expand(e.file)
                                        expanding = null
                                        if (items.isEmpty()) ctx.toast("That folder is empty") else {
                                            folderKeys[e.file.path] = items
                                            c.selection.addAll(items)
                                            ctx.toast("Added ${items.size} files from ${e.name}")
                                        }
                                    }
                                }) { CheckMark(sel) }
                            },
                        ) { dir = e.file }
                    } else {
                        val item = remember(e.file.path) { FileBrowser.toItem(e.file) }
                        ItemRow(
                            thumb = { FileThumb(item.uri.toString(), item.category, 48.dp, Modifier.onGloballyPositioned { bounds.value = it.boundsInRoot() }, name = item.name) },
                            title = e.name, subtitle = "${formatSize(e.size)} • ${formatDate(e.modified)}",
                            selected = selection.containsKey(item.key),
                        ) { onToggle(item, bounds.value) }
                    }
                }
            }
        }
    }
}

fun requestAllFiles(ctx: android.content.Context) {
    if (Build.VERSION.SDK_INT >= 30) {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    } else {
        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
fun SelectionBar(count: Int, size: Long, preview: List<ShareItem>, action: String, onTarget: (Offset) -> Unit = {}, onShow: () -> Unit, onAction: () -> Unit) {
    val density = LocalDensity.current
    val bump = remember { Animatable(1f) }
    LaunchedEffect(count) { bump.snapTo(1.18f); bump.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f)) }
    Surface(shadowElevation = 20.dp, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClick = onShow).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width((26 + 16 * preview.size).dp).height(46.dp)
                        .onGloballyPositioned { val b = it.boundsInRoot(); onTarget(Offset(b.left + with(density) { 21.dp.toPx() }, b.center.y)) }
                        .scale(bump.value)
                ) {
                    preview.forEachIndexed { i, it ->
                        FileThumb(
                            it.uri.toString(), it.category, 42.dp,
                            Modifier.offset(x = (i * 16).dp).graphicsLayer { rotationZ = (i - 1) * 6f }
                                .shadow(4.dp, RoundedCornerShape(12.dp)).border(2.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                            packageName = it.packageName, name = it.name,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$count selected", style = MaterialTheme.typography.titleSmall)
                        Icon(Icons.Rounded.ExpandLess, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatSize(size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            GradientButton(action, onClick = onAction, brush = Brand.send, icon = Icons.AutoMirrored.Rounded.Send, height = 52.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SelectedSheet(items: List<ShareItem>, onRemove: (ShareItem) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlossyIcon(Icons.Rounded.Inventory2, Brand.main, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ready to send", style = MaterialTheme.typography.titleLarge)
                    Text("${items.size} items • ${formatSize(items.sumOf { it.size })}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onClear) { Text("Clear all", color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.groupBy { it.category }.forEach { (cat, list) ->
                    InfoChip("${list.size} ${cat.label}", categoryIcon(cat), categoryColor(cat))
                }
            }
        }
        LazyVerticalGrid(
            GridCells.Adaptive(96.dp), Modifier.heightIn(max = 460.dp),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.key }) { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        FileThumb(item.uri.toString(), item.category, 84.dp, packageName = item.packageName, name = item.name, corner = 18.dp)
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).size(26.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface).clickable { onRemove(item) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Close, "Remove", Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatSize(item.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}
