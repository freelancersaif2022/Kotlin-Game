package com.flashshare.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.container
import com.flashshare.app.data.Category
import com.flashshare.app.data.ShareItem
import com.flashshare.app.transfer.Mode
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.util.Perms
import com.flashshare.app.util.formatDuration
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.toast
import kotlinx.coroutines.launch

private enum class PickTab(val title: String, val cat: Category) {
    PHOTOS("Photos", Category.PHOTO), VIDEOS("Videos", Category.VIDEO), APPS("Apps", Category.APP),
    MUSIC("Music", Category.MUSIC), FILES("Files", Category.FILE)
}

/** mode: "send" → scan next, "group" → host a group, "more" → add to the current connection. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PickerScreen(mode: String, nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val selection by c.selection.items.collectAsStateWithLifecycle()
    val tabs = PickTab.entries
    val pager = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    var showSelected by remember { mutableStateOf(false) }

    // Loaded lazily, once per screen.
    val photos by produceState<List<ShareItem>?>(null) { value = c.media.photos() }
    val videos by produceState<List<ShareItem>?>(null) { value = c.media.videos() }
    val apps by produceState<List<ShareItem>?>(null) { value = c.media.apps() }
    val music by produceState<List<ShareItem>?>(null) { value = c.media.music() }
    var docs by remember { mutableStateOf<List<ShareItem>?>(null) }
    var picked by remember { mutableStateOf<List<ShareItem>>(emptyList()) }
    LaunchedEffect(Unit) { docs = c.media.documents() }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val items = c.media.fromDocumentUris(uris)
            picked = (items + picked).distinctBy { it.key }
            c.selection.addAll(items)
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val items = c.media.fromTree(uri)
            if (items.isEmpty()) ctx.toast("That folder is empty") else {
                picked = (items + picked).distinctBy { it.key }
                c.selection.addAll(items)
                ctx.toast("Added ${items.size} files from the folder")
            }
        }
    }

    BackHandler(showSelected) { showSelected = false }

    val title = when (mode) {
        "group" -> "Pick files for group"
        "more" -> "Send more files"
        else -> "Select files"
    }

    AppScaffold(
        title = title,
        onBack = { nav.popBackStack() },
        actions = {
            if (selection.isNotEmpty()) TextButton(onClick = { c.selection.clear() }) { Text("Clear") }
        },
        bottomBar = {
            AnimatedVisibility(selection.isNotEmpty(), enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                SelectionBar(
                    count = selection.size,
                    size = selection.values.sumOf { it.size },
                    preview = selection.values.take(3).toList(),
                    action = when (mode) { "group" -> "Start group"; "more" -> "Send"; else -> "Send" },
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
            ScrollableTabRow(
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
                                if (count > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                        Text("$count", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    PickTab.PHOTOS -> MediaGrid(photos, selection, Category.PHOTO) { c.selection.toggle(it) }
                    PickTab.VIDEOS -> MediaGrid(videos, selection, Category.VIDEO) { c.selection.toggle(it) }
                    PickTab.APPS -> AppsGrid(apps, selection) { c.selection.toggle(it) }
                    PickTab.MUSIC -> FileList(music, selection, Category.MUSIC, header = null) { c.selection.toggle(it) }
                    PickTab.FILES -> FileList(
                        docs?.let { (picked + it).distinctBy { i -> i.key } }, selection, Category.FILE,
                        header = {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PickButton("Browse files", Icons.Rounded.FileOpen, Modifier.weight(1f)) { docPicker.launch(arrayOf("*/*")) }
                                PickButton("Send a folder", Icons.Rounded.DriveFolderUpload, Modifier.weight(1f)) { folderPicker.launch(null) }
                            }
                        },
                    ) { c.selection.toggle(it) }
                }
            }
        }
    }

    if (showSelected) {
        SelectedSheet(selection.values.toList(), onRemove = { c.selection.toggle(it) }, onDismiss = { showSelected = false })
    }
}

@Composable
private fun PickButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SelectAllHeader(items: List<ShareItem>, selection: Map<String, ShareItem>, label: String) {
    val ctx = LocalContext.current
    val c = ctx.container
    val allSelected = items.isNotEmpty() && items.all { selection.containsKey(it.key) }
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${items.size} $label • ${formatSize(items.sumOf { it.size })}", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        TextButton(onClick = { if (allSelected) c.selection.removeAll(items) else c.selection.addAll(items) }) {
            Text(if (allSelected) "Deselect all" else "Select all")
        }
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun MediaGrid(items: List<ShareItem>?, selection: Map<String, ShareItem>, cat: Category, onToggle: (ShareItem) -> Unit) {
    if (items == null) return Loading()
    if (items.isEmpty()) return EmptyState(categoryIcon(cat), "Nothing here", "No ${cat.label.lowercase()} found on this phone.")
    Column {
        SelectAllHeader(items, selection, cat.label.lowercase())
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items, key = { it.key }) { item ->
                val sel = selection.containsKey(item.key)
                val sc by animateFloatAsState(if (sel) 0.86f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "sel")
                Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).clickable { onToggle(item) }) {
                    FileThumb(item.uri.toString(), cat, 200.dp, Modifier.fillMaxSize().scale(sc), corner = 10.dp)
                    if (cat == Category.VIDEO && item.durationMs > 0) {
                        Text(formatDuration(item.durationMs), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                    CheckMark(sel, Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
        }
    }
}

@Composable
private fun CheckMark(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(24.dp).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.25f))
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.animation.AnimatedVisibility(selected, enter = scaleIn(), exit = scaleOut()) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AppsGrid(items: List<ShareItem>?, selection: Map<String, ShareItem>, onToggle: (ShareItem) -> Unit) {
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
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { onToggle(item) }.padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        AppIcon(item.packageName ?: "", 52.dp)
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
private fun FileList(items: List<ShareItem>?, selection: Map<String, ShareItem>, cat: Category, header: (@Composable () -> Unit)?, onToggle: (ShareItem) -> Unit) {
    Column {
        header?.invoke()
        if (items == null) return@Column Loading()
        if (items.isEmpty()) {
            EmptyState(categoryIcon(cat), "Nothing here yet", if (cat == Category.MUSIC) "No music found on this phone." else "Tap “Browse files” to pick documents, ZIPs, PDFs and more.")
            return@Column
        }
        SelectAllHeader(items, selection, if (cat == Category.MUSIC) "songs" else "files")
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            items(items, key = { it.key }) { item ->
                val sel = selection.containsKey(item.key)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
                        .clickable { onToggle(item) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FileThumb(item.uri.toString(), item.category, 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (item.relPath.isNotEmpty()) item.relPath + "/" + item.name else item.name,
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOf(item.subtitle, formatSize(item.size), if (item.durationMs > 0) formatDuration(item.durationMs) else "").filter { it.isNotEmpty() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        )
                    }
                    Checkbox(sel, onCheckedChange = { onToggle(item) })
                }
            }
        }
    }
}

@Composable
fun SelectionBar(count: Int, size: Long, preview: List<ShareItem>, action: String, onShow: () -> Unit, onAction: () -> Unit) {
    Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onShow).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width((24 + 18 * preview.size).dp).height(44.dp)) {
                    preview.forEachIndexed { i, it ->
                        FileThumb(it.uri.toString(), it.category, 40.dp, Modifier.offset(x = (i * 18).dp).border(2.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)), packageName = it.packageName)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("$count selected", style = MaterialTheme.typography.titleSmall)
                    Text(formatSize(size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            GradientButton(action, onClick = onAction, brush = Brand.send, icon = Icons.Rounded.Send, height = 50.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedSheet(items: List<ShareItem>, onRemove: (ShareItem) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Selected (${items.size})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = PaddingValues(12.dp)) {
            items(items, key = { it.key }) { item ->
                Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FileThumb(item.uri.toString(), item.category, 42.dp, packageName = item.packageName)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        Text(formatSize(item.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onRemove(item) }) { Icon(Icons.Rounded.Close, "Remove") }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}
