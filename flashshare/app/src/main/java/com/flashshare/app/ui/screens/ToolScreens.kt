package com.flashshare.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.container
import com.flashshare.app.data.Category
import com.flashshare.app.data.CleanItem
import com.flashshare.app.data.CleanResult
import com.flashshare.app.data.ShareItem
import com.flashshare.app.data.StorageInfo
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.withPerms
import com.flashshare.app.util.PermFlow
import com.flashshare.app.util.formatDate
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.openFile
import com.flashshare.app.util.shareFiles
import com.flashshare.app.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// region delete helper
/** Deletes files of any kind; MediaStore items of other apps go through the system confirmation on Android 11+. */
@Composable
fun rememberDeleter(onDeleted: (List<Uri>) -> Unit): (List<Uri>) -> Unit {
    val ctx = LocalContext.current
    val onDeletedLatest by rememberUpdatedState(onDeleted)
    var pending by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) onDeletedLatest(pending)
        pending = emptyList()
    }
    return remember {
        { uris: List<Uri> ->
            val cr = ctx.contentResolver
            val media = uris.filter { it.scheme == "content" && it.authority == MediaStore.AUTHORITY }
            val done = ArrayList<Uri>()
            (uris - media.toSet()).forEach { u ->
                val ok = runCatching {
                    when {
                        u.scheme == "file" -> File(u.path!!).delete()
                        DocumentsContract.isDocumentUri(ctx, u) -> DocumentsContract.deleteDocument(cr, u)
                        else -> cr.delete(u, null, null) > 0
                    }
                }.getOrDefault(false)
                if (ok) done += u
            }
            if (media.isNotEmpty() && Build.VERSION.SDK_INT >= 30) {
                if (done.isNotEmpty()) onDeletedLatest(done)
                pending = media
                val req = MediaStore.createDeleteRequest(cr, media)
                launcher.launch(IntentSenderRequest.Builder(req.intentSender).build())
            } else {
                media.forEach { u -> if (runCatching { cr.delete(u, null, null) > 0 }.getOrDefault(false)) done += u }
                if (done.size < uris.size) ctx.toast("Some files couldn't be deleted")
                onDeletedLatest(done)
            }
        }
    }
}
// endregion

// region phone clone
private data class CloneCat(val cat: Category, val title: String, val icon: ImageVector)

@Composable
fun CloneScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    var role by rememberSaveable { mutableIntStateOf(0) } // 0 choose, 1 old phone
    BackHandler(role != 0) { role = 0 }

    AppScaffold("Phone Clone", onBack = { if (role != 0) role = 0 else nav.popBackStack() }) { pad ->
        AnimatedContent(role, Modifier.padding(pad), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "role") { r ->
            if (r == 0) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PhoneIcon(Brand.send, Icons.Rounded.PhoneAndroid)
                            TransferBeam(true, false, Modifier.width(90.dp).height(20.dp))
                            PhoneIcon(Brand.receive, Icons.Rounded.Smartphone)
                        }
                    }
                    Text("Move everything to your new phone", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("Contacts, photos, videos, music, apps and documents – over a direct 5 GHz link, no cables or internet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                    RoleCard("This is my OLD phone", "Choose what to copy and send it", Icons.Rounded.Upload, Brand.send) { role = 1 }
                    Spacer(Modifier.height(14.dp))
                    RoleCard("This is my NEW phone", "Show a QR code to receive everything", Icons.Rounded.Download, Brand.receive) {
                        nav.withPerms(ctx, PermFlow.RECEIVE, Routes.RECEIVE)
                    }
                }
            } else {
                CloneSender(nav)
            }
        }
    }
}

@Composable
private fun PhoneIcon(brush: androidx.compose.ui.graphics.Brush, icon: ImageVector) {
    Box(Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(brush), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, icon: ImageVector, brush: androidx.compose.ui.graphics.Brush, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(brush), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null)
        }
    }
}

@Composable
private fun CloneSender(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    val cats = remember {
        listOf(
            CloneCat(Category.CONTACT, "Contacts", Icons.Rounded.Contacts),
            CloneCat(Category.PHOTO, "Photos", Icons.Rounded.Image),
            CloneCat(Category.VIDEO, "Videos", Icons.Rounded.Movie),
            CloneCat(Category.MUSIC, "Music", Icons.Rounded.MusicNote),
            CloneCat(Category.APP, "Apps", Icons.Rounded.Android),
            CloneCat(Category.FILE, "Documents", Icons.Rounded.Description),
        )
    }
    var hasContacts by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
    val contactsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasContacts = it }
    var data by remember { mutableStateOf<Map<Category, List<ShareItem>>?>(null) }
    var contacts by remember { mutableIntStateOf(0) }
    var checked by remember { mutableStateOf(setOf(Category.CONTACT, Category.PHOTO, Category.VIDEO, Category.MUSIC, Category.APP)) }
    var preparing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        data = mapOf(
            Category.PHOTO to c.media.photos(),
            Category.VIDEO to c.media.videos(),
            Category.MUSIC to c.media.music(),
            Category.APP to c.media.apps(),
            Category.FILE to c.media.documents().filter { it.category == Category.FILE },
        )
    }
    LaunchedEffect(hasContacts) { if (hasContacts) contacts = withContext(Dispatchers.IO) { c.cleaner.contactCount() } }

    val total = data?.let { d -> checked.sumOf { cat -> d[cat]?.sumOf { it.size } ?: 0L } } ?: 0L

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("What should we copy?", style = MaterialTheme.typography.titleLarge) }
            items(cats, key = { it.cat.name }) { cc ->
                val list = data?.get(cc.cat)
                val isContacts = cc.cat == Category.CONTACT
                val sub = when {
                    isContacts && !hasContacts -> "Tap to allow access"
                    isContacts -> "$contacts contacts"
                    list == null -> "Counting…"
                    else -> "${list.size} items • ${formatSize(list.sumOf { it.size })}"
                }
                AppCard(Modifier.fillMaxWidth(), onClick = {
                    if (isContacts && !hasContacts) contactsPerm.launch(Manifest.permission.READ_CONTACTS)
                    else checked = if (cc.cat in checked) checked - cc.cat else checked + cc.cat
                }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(categoryColor(cc.cat).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(cc.icon, null, tint = categoryColor(cc.cat))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cc.title, style = MaterialTheme.typography.titleSmall)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(cc.cat in checked && (!isContacts || hasContacts), onCheckedChange = {
                            if (isContacts && !hasContacts) contactsPerm.launch(Manifest.permission.READ_CONTACTS)
                            else checked = if (it) checked + cc.cat else checked - cc.cat
                        })
                    }
                }
            }
        }
        Surface(shadowElevation = 12.dp) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatSize(total), style = MaterialTheme.typography.titleLarge)
                }
                GradientButton(
                    if (preparing) "Preparing…" else "Start clone", enabled = !preparing && data != null && checked.isNotEmpty(),
                    icon = Icons.Rounded.RocketLaunch,
                    onClick = {
                        preparing = true
                        scope.launch {
                            val items = ArrayList<ShareItem>()
                            if (Category.CONTACT in checked && hasContacts) c.cleaner.exportContacts()?.let { items += it }
                            data?.let { d -> checked.filter { it != Category.CONTACT }.forEach { items += d[it].orEmpty() } }
                            preparing = false
                            if (items.isEmpty()) { ctx.toast("Nothing selected to copy"); return@launch }
                            c.selection.set(items)
                            nav.withPerms(ctx, PermFlow.SEND, Routes.scan("send"))
                        }
                    },
                )
            }
        }
    }
}
// endregion

// region file manager
private enum class FileCat(val title: String, val icon: ImageVector, val color: Color) {
    RECEIVED("Received", Icons.Rounded.Download, Color(0xFF2F7BFF)),
    IMAGES("Images", Icons.Rounded.Image, Color(0xFF00A3FF)),
    VIDEOS("Videos", Icons.Rounded.Movie, Color(0xFFFF4D6D)),
    AUDIO("Audio", Icons.Rounded.MusicNote, Color(0xFFFF8A00)),
    DOCS("Documents", Icons.Rounded.Description, Color(0xFF7B4DFF)),
    APKS("Installers", Icons.Rounded.Android, Color(0xFF00B07A)),
}

private enum class Sort(val label: String) { DATE("Newest"), SIZE("Largest"), NAME("Name") }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val settings by c.prefs.state.collectAsStateWithLifecycle()
    var cat by rememberSaveable { mutableStateOf(FileCat.RECEIVED) }
    var sort by rememberSaveable { mutableStateOf(Sort.DATE) }
    var query by rememberSaveable { mutableStateOf("") }
    var items by remember { mutableStateOf<List<ShareItem>?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var reload by remember { mutableIntStateOf(0) }
    var storage by remember { mutableStateOf(StorageInfo(0, 0)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val deleter = rememberDeleter { gone ->
        val keys = gone.map { it.toString() }.toSet()
        items = items?.filterNot { it.uri.toString() in keys }
        selected = emptySet()
        if (gone.isNotEmpty()) ctx.toast("Deleted ${gone.size} file${if (gone.size == 1) "" else "s"}")
        reload++
    }

    LaunchedEffect(cat, reload) {
        items = null
        selected = emptySet()
        items = when (cat) {
            FileCat.RECEIVED -> c.media.received(settings?.saveTreeUri)
            FileCat.IMAGES -> c.media.photos()
            FileCat.VIDEOS -> c.media.videos()
            FileCat.AUDIO -> c.media.music()
            FileCat.DOCS -> c.media.documents().filter { it.category != Category.APP }
            FileCat.APKS -> c.media.documents().filter { it.category == Category.APP }
        }
        storage = withContext(Dispatchers.IO) { c.cleaner.storage() }
    }
    BackHandler(selected.isNotEmpty()) { selected = emptySet() }

    val shown = remember(items, sort, query) {
        items.orEmpty().filter { query.isBlank() || it.name.contains(query, true) }.let { l ->
            when (sort) {
                Sort.DATE -> l.sortedByDescending { it.dateAdded }
                Sort.SIZE -> l.sortedByDescending { it.size }
                Sort.NAME -> l.sortedBy { it.name.lowercase() }
            }
        }
    }
    val selItems = shown.filter { it.key in selected }

    AppScaffold(
        if (selected.isEmpty()) "File Manager" else "${selected.size} selected",
        onBack = { if (selected.isNotEmpty()) selected = emptySet() else nav.popBackStack() },
        actions = {
            if (selected.isNotEmpty()) {
                IconButton(onClick = { selected = if (selected.size == shown.size) emptySet() else shown.map { it.key }.toSet() }) { Icon(Icons.Rounded.SelectAll, "Select all") }
            } else {
                var menu by remember { mutableStateOf(false) }
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.Sort, "Sort") }
                DropdownMenu(menu, { menu = false }) {
                    Sort.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.label) }, onClick = { sort = s; menu = false },
                            trailingIcon = { if (sort == s) Icon(Icons.Rounded.Check, null) })
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(selected.isNotEmpty(), enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                Surface(shadowElevation = 12.dp) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BarAction(Icons.Rounded.Send, "FlashShare") {
                            c.selection.set(selItems)
                            nav.withPerms(ctx, PermFlow.SEND, Routes.scan("send"))
                        }
                        BarAction(Icons.Rounded.Share, "Share") { ctx.shareFiles(selItems.map { it.uri }) }
                        BarAction(Icons.Rounded.Delete, "Delete", MaterialTheme.colorScheme.error) { confirmDelete = true }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = { storage.usedFraction }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${formatSize(storage.free)} free of ${formatSize(storage.total)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FileCat.entries) { fc ->
                    FilterChip(
                        selected = cat == fc, onClick = { cat = fc },
                        label = { Text(fc.title) },
                        leadingIcon = { Icon(fc.icon, null, tint = if (cat == fc) MaterialTheme.colorScheme.onSecondaryContainer else fc.color, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(50),
                    )
                }
            }
            OutlinedTextField(
                query, { query = it }, placeholder = { Text("Search ${cat.title.lowercase()}") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(50),
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            when {
                items == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                shown.isEmpty() -> EmptyState(cat.icon, "No files", if (cat == FileCat.RECEIVED) "Files you receive with FlashShare appear here." else "Nothing found.")
                else -> LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    item {
                        Text("${shown.size} files • ${formatSize(shown.sumOf { it.size })}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                    }
                    items(shown, key = { it.key }) { f ->
                        val sel = f.key in selected
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
                                .combinedClickable(
                                    onClick = {
                                        if (selected.isNotEmpty()) selected = if (sel) selected - f.key else selected + f.key
                                        else ctx.openFile(f.uri.toString(), f.mime)
                                    },
                                    onLongClick = { selected = if (sel) selected - f.key else selected + f.key },
                                ).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FileThumb(f.uri.toString(), f.category, 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${formatSize(f.size)} • ${formatDate(f.dateAdded)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (selected.isNotEmpty()) Checkbox(sel, { selected = if (it) selected + f.key else selected - f.key })
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selItems.size} file${if (selItems.size == 1) "" else "s"}?") },
            text = { Text("${formatSize(selItems.sumOf { it.size })} will be permanently removed.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; deleter(selItems.map { it.uri }) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BarAction(icon: ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint)
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
// endregion

// region cleaner
@Composable
fun CleanerScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    var progress by remember { mutableFloatStateOf(0f) }
    var stage by remember { mutableStateOf("Starting…") }
    var result by remember { mutableStateOf<CleanResult?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var includeCache by remember { mutableStateOf(true) }
    var freed by remember { mutableStateOf<Long?>(null) }
    var scanKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(scanKey) {
        result = null; freed = null; progress = 0f
        val r = c.cleaner.scan { p, s -> progress = p; stage = s }
        // Pre-select the safe stuff: duplicates, old screenshots and installers.
        selected = (r.duplicates + r.apks + r.screenshots).map { it.uri.toString() }.toSet()
        result = r
    }

    val all = result?.let { (it.large + it.duplicates + it.apks + it.screenshots).distinctBy { i -> i.uri } }.orEmpty()
    val selBytes = all.filter { it.uri.toString() in selected }.sumOf { it.size } + if (includeCache) (result?.cacheBytes ?: 0) else 0
    var cacheFreed by remember { mutableLongStateOf(0L) }
    val deleter = rememberDeleter { gone ->
        val bytes = all.filter { it.uri in gone }.sumOf { it.size }
        freed = cacheFreed + bytes
    }

    AppScaffold("Cleaner", onBack = { nav.popBackStack() }) { pad ->
        val r = result
        Column(Modifier.fillMaxSize().padding(pad)) {
            when {
                freed != null -> Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(140.dp).clip(CircleShape).background(Brand.mint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(72.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("${formatSize(freed ?: 0)} freed", style = MaterialTheme.typography.headlineMedium)
                    Text("Your phone has more room for what matters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    GradientButton("Scan again", onClick = { scanKey++ }, icon = Icons.Rounded.Refresh)
                    TextButton(onClick = { nav.popBackStack() }) { Text("Done") }
                }
                r == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val t = rememberInfiniteTransition(label = "scan")
                    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "r")
                    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        ProgressRing(progress, Modifier.fillMaxSize(), stroke = 14.dp, brush = Brand.mint)
                        Icon(Icons.Rounded.Autorenew, null, tint = Brand.Mint, modifier = Modifier.size(56.dp).rotate(rot))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium)
                    Text(stage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brand.mint).padding(24.dp)) {
                                Text("Found", color = Color.White.copy(alpha = 0.85f))
                                Text(formatSize(r.total), color = Color.White, style = MaterialTheme.typography.displaySmall)
                                Text("that you may not need", color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                        item {
                            AppCard(Modifier.fillMaxWidth(), onClick = { includeCache = !includeCache }) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Cached, null, tint = Brand.Mint)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("App cache", style = MaterialTheme.typography.titleSmall)
                                        Text(formatSize(r.cacheBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Checkbox(includeCache, { includeCache = it })
                                }
                            }
                        }
                        listOf(
                            Triple("Duplicate files", Icons.Rounded.FileCopy, r.duplicates),
                            Triple("Large files (100 MB+)", Icons.Rounded.SdStorage, r.large),
                            Triple("Old screenshots", Icons.Rounded.Screenshot, r.screenshots),
                            Triple("Installer files (APK)", Icons.Rounded.Android, r.apks),
                        ).forEach { (title, icon, list) ->
                            if (list.isNotEmpty()) item(key = title) {
                                CleanGroup(title, icon, list, selected) { selected = it }
                            }
                        }
                    }
                    Surface(shadowElevation = 12.dp) {
                        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Selected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatSize(selBytes), style = MaterialTheme.typography.titleLarge)
                            }
                            GradientButton("Clean now", brush = Brand.mint, icon = Icons.Rounded.CleaningServices, enabled = selBytes > 0, onClick = {
                                scope.launch {
                                    cacheFreed = if (includeCache) withContext(Dispatchers.IO) { c.cleaner.clearCache() } else 0L
                                    val uris = all.filter { it.uri.toString() in selected }.map { it.uri }
                                    // Success screen appears once the system delete confirmation is accepted.
                                    if (uris.isEmpty()) freed = cacheFreed else deleter(uris)
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanGroup(title: String, icon: ImageVector, list: List<CleanItem>, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val keys = list.map { it.uri.toString() }
    val all = keys.all { it in selected }
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.then(clickableNoRipple { open = !open }).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text("${list.size} files • ${formatSize(list.sumOf { it.size })}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            Checkbox(all, { onChange(if (it) selected + keys else selected - keys.toSet()) })
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 14.dp, end = 6.dp, bottom = 10.dp)) {
                list.take(100).forEach { item ->
                    val k = item.uri.toString()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        FileThumb(k, Category.fromMime(item.mime, item.name), 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatSize(item.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(k in selected, { onChange(if (it) selected + k else selected - k) })
                    }
                }
                if (list.size > 100) Text("+ ${list.size - 100} more", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
// endregion
