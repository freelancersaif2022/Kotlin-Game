package com.flashshare.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.container
import com.flashshare.app.data.Category
import com.flashshare.app.data.CleanItem
import com.flashshare.app.data.CleanResult
import com.flashshare.app.data.FileBrowser
import com.flashshare.app.data.FileEntry
import com.flashshare.app.data.ShareItem
import com.flashshare.app.data.StorageInfo
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.flashshare.app.ui.withPerms
import com.flashshare.app.util.PermFlow
import com.flashshare.app.util.formatDate
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.openFile
import com.flashshare.app.util.shareFiles
import com.flashshare.app.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File

// region delete helper
/**
 * Deletes files. With "All files access" everything is removed directly (no system pop-up);
 * otherwise MediaStore items of other apps go through Android's confirmation.
 */
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
            val media = ArrayList<Uri>()
            val done = ArrayList<Uri>()
            val direct = FileBrowser.canBrowse(ctx)
            val scanned = ArrayList<String>()
            uris.forEach { u ->
                val ok = runCatching {
                    when {
                        u.scheme == "file" -> File(u.path!!).delete().also { if (it) scanned += u.path!! }
                        DocumentsContract.isDocumentUri(ctx, u) -> DocumentsContract.deleteDocument(cr, u)
                        u.authority == MediaStore.AUTHORITY && direct -> {
                            val path = ctx.container.media.pathOf(u)
                            val fileOk = path != null && File(path).let { !it.exists() || it.delete() }
                            runCatching { cr.delete(u, null, null) }
                            if (path != null) scanned += path
                            fileOk
                        }
                        u.authority == MediaStore.AUTHORITY && Build.VERSION.SDK_INT >= 30 -> { media += u; false }
                        else -> cr.delete(u, null, null) > 0
                    }
                }.getOrDefault(false)
                if (ok) done += u
            }
            if (scanned.isNotEmpty()) MediaScannerConnection.scanFile(ctx, scanned.toTypedArray(), null, null)
            if (media.isNotEmpty() && Build.VERSION.SDK_INT >= 30) {
                if (done.isNotEmpty()) onDeletedLatest(done)
                pending = media
                launcher.launch(IntentSenderRequest.Builder(MediaStore.createDeleteRequest(cr, media).intentSender).build())
            } else {
                if (done.size < uris.size) ctx.toast("Some files couldn't be deleted")
                onDeletedLatest(done)
            }
        }
    }
}
// endregion

// region phone clone
private data class CloneCat(val cat: Category, val title: String, val icon: ImageVector, val brush: Brush)

@Composable
fun CloneScreen(nav: NavController) {
    val ctx = LocalContext.current
    var role by rememberSaveable { mutableIntStateOf(0) } // 0 choose, 1 old phone
    BackHandler(role != 0) { role = 0 }

    AppScaffold(if (role == 0) "Phone Clone" else "What should we copy?", onBack = { if (role != 0) role = 0 else nav.popBackStack() }) { pad ->
        AnimatedContent(role, Modifier.padding(pad), transitionSpec = { (fadeIn() + slideInVertically { it / 8 }) togetherWith fadeOut() }, label = "role") { r ->
            if (r == 0) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(32.dp), spotColor = Brand.Blue).clip(RoundedCornerShape(32.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF101536), Color(0xFF263B8F), Color(0xFF1E8FD6))))
                            .padding(vertical = 26.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PhonesLink(
                                left = { CloneLabel("OLD", Icons.Rounded.PhoneAndroid) },
                                right = { CloneLabel("NEW", Icons.Rounded.AutoAwesome) },
                                active = true, modifier = Modifier.fillMaxWidth(), phoneWidth = 84.dp,
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(Icons.Rounded.Contacts, Icons.Rounded.Image, Icons.Rounded.Movie, Icons.Rounded.MusicNote, Icons.Rounded.Android, Icons.Rounded.Description)
                                    .forEachIndexed { i, ic -> GlossyIcon(ic, listOf(IconBrushes.blue, IconBrushes.cyan, IconBrushes.pink, IconBrushes.orange, IconBrushes.mint, IconBrushes.violet)[i], 34.dp, Modifier.staggerIn(i)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("Move everything to your new phone", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text("Contacts, photos, videos, music, apps and documents over a direct 5 GHz link – no cables, no internet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    RoleCard("This is my OLD phone", "Choose what to copy and send it", Icons.Rounded.Upload, Brand.send, Modifier.staggerIn(0)) { role = 1 }
                    Spacer(Modifier.height(14.dp))
                    RoleCard("This is my NEW phone", "Show a QR code and receive everything", Icons.Rounded.Download, Brand.receive, Modifier.staggerIn(1)) {
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
private fun CloneLabel(text: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(4.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, icon: ImageVector, brush: Brush, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(icon, brush, 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
            }
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
            CloneCat(Category.CONTACT, "Contacts", Icons.Rounded.Contacts, IconBrushes.blue),
            CloneCat(Category.PHOTO, "Photos", Icons.Rounded.Image, IconBrushes.cyan),
            CloneCat(Category.VIDEO, "Videos", Icons.Rounded.Movie, IconBrushes.pink),
            CloneCat(Category.MUSIC, "Music", Icons.Rounded.MusicNote, IconBrushes.orange),
            CloneCat(Category.APP, "Apps", Icons.Rounded.Android, IconBrushes.mint),
            CloneCat(Category.FILE, "Documents", Icons.Rounded.Description, IconBrushes.violet),
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
    val items = data?.let { d -> checked.sumOf { cat -> if (cat == Category.CONTACT) (if (hasContacts) contacts else 0) else d[cat]?.size ?: 0 } } ?: 0

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(26.dp), spotColor = Brand.Blue).clip(RoundedCornerShape(26.dp)).background(Brand.send).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Ready to copy", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                        Text(formatSize(total), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("$items items selected", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                    GlossyIcon(Icons.Rounded.PhonelinkSetup, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.15f))), 60.dp)
                }
            }
            itemsIndexed(cats, key = { _, it -> it.cat.name }) { i, cc ->
                val list = data?.get(cc.cat)
                val isContacts = cc.cat == Category.CONTACT
                val on = cc.cat in checked && (!isContacts || hasContacts)
                val sub = when {
                    isContacts && !hasContacts -> "Tap to allow access"
                    isContacts -> "$contacts contacts"
                    list == null -> null
                    else -> "${list.size} items • ${formatSize(list.sumOf { it.size })}"
                }
                AppCard(Modifier.fillMaxWidth().staggerIn(i), onClick = {
                    if (isContacts && !hasContacts) contactsPerm.launch(Manifest.permission.READ_CONTACTS)
                    else checked = if (cc.cat in checked) checked - cc.cat else checked + cc.cat
                }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlossyIcon(cc.icon, cc.brush, 48.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cc.title, style = MaterialTheme.typography.titleSmall)
                            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else Shimmer(Modifier.padding(top = 4.dp).width(110.dp).height(10.dp).clip(RoundedCornerShape(5.dp)))
                        }
                        Switch(on, onCheckedChange = {
                            if (isContacts && !hasContacts) contactsPerm.launch(Manifest.permission.READ_CONTACTS)
                            else checked = if (it) checked + cc.cat else checked - cc.cat
                        })
                    }
                }
            }
        }
        Surface(shadowElevation = 16.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                GradientButton(
                    if (preparing) "Preparing…" else "Start clone • ${formatSize(total)}", enabled = !preparing && data != null && checked.isNotEmpty(),
                    icon = Icons.Rounded.RocketLaunch, modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        preparing = true
                        scope.launch {
                            val list = ArrayList<ShareItem>()
                            if (Category.CONTACT in checked && hasContacts) c.cleaner.exportContacts()?.let { list += it }
                            data?.let { d -> checked.filter { it != Category.CONTACT }.forEach { list += d[it].orEmpty() } }
                            preparing = false
                            if (list.isEmpty()) { ctx.toast("Nothing selected to copy"); return@launch }
                            c.selection.set(list)
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
private enum class FileCat(val title: String, val icon: ImageVector, val brush: Brush) {
    RECEIVED("Received", Icons.Rounded.Download, IconBrushes.blue),
    IMAGES("Images", Icons.Rounded.Image, IconBrushes.cyan),
    VIDEOS("Videos", Icons.Rounded.Movie, IconBrushes.pink),
    AUDIO("Audio", Icons.Rounded.MusicNote, IconBrushes.orange),
    DOCS("Documents", Icons.Rounded.Description, IconBrushes.violet),
    APPS("Installed apps", Icons.Rounded.Apps, IconBrushes.mint),
    APKS("Installers", Icons.Rounded.Android, IconBrushes.red),
    BROWSE("All files", Icons.Rounded.FolderOpen, IconBrushes.slate),
}

private enum class Sort(val label: String) { DATE("Newest"), SIZE("Largest"), NAME("Name") }

@Composable
fun FilesScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    var cat by rememberSaveable { mutableStateOf<FileCat?>(null) }
    var storage by remember { mutableStateOf(StorageInfo(0, 0)) }
    var counts by remember { mutableStateOf<Map<FileCat, Pair<Int, Long>>>(emptyMap()) }
    LaunchedEffect(Unit) {
        storage = withContext(Dispatchers.IO) { c.cleaner.storage() }
        val m = HashMap<FileCat, Pair<Int, Long>>()
        suspend fun put(fc: FileCat, l: List<ShareItem>) { m[fc] = l.size to l.sumOf { it.size }; counts = HashMap(m) }
        put(FileCat.IMAGES, c.media.photos()); put(FileCat.VIDEOS, c.media.videos()); put(FileCat.AUDIO, c.media.music())
        put(FileCat.DOCS, c.media.documents().filter { it.category != Category.APP }); put(FileCat.APPS, c.media.apps())
        put(FileCat.RECEIVED, c.media.received(c.prefs.current.saveTreeUri))
    }
    BackHandler(cat != null) { cat = null }

    AnimatedContent(cat, transitionSpec = { (fadeIn() + slideInVertically { it / 10 }) togetherWith fadeOut() }, label = "files") { current ->
        if (current == null) {
            AppScaffold("File Manager", onBack = { nav.popBackStack() }) { pad ->
                LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { StorageHeader(storage, counts) }
                    item { SectionTitle("Categories") }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FileCat.entries.chunked(2).forEachIndexed { r, row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEachIndexed { i, fc ->
                                        val info = counts[fc]
                                        AppCard(Modifier.weight(1f).staggerIn(r * 2 + i), onClick = { cat = fc }) {
                                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                GlossyIcon(fc.icon, fc.brush, 44.dp)
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(fc.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(
                                                        when { fc == FileCat.BROWSE -> "Folders"; info == null -> "…"; else -> "${info.first} • ${formatSize(info.second)}" },
                                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            FileCategoryList(current, nav) { cat = null }
        }
    }
}

@Composable
private fun StorageHeader(info: StorageInfo, counts: Map<FileCat, Pair<Int, Long>>) {
    val parts = listOf(FileCat.IMAGES to Color(0xFF19C3FF), FileCat.VIDEOS to Color(0xFFFF4D9D), FileCat.AUDIO to Color(0xFFFFB020),
        FileCat.DOCS to Color(0xFFB98BFF), FileCat.APPS to Color(0xFF3DEBB0))
    Column(
        Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(28.dp), spotColor = Brand.Violet).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF101536), Color(0xFF2E2480), Color(0xFF5B34D1)))).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Internal storage", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Text(formatSize(info.used), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("used of ${formatSize(info.total)} • ${formatSize(info.free)} free", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                ProgressRing(info.usedFraction, Modifier.fillMaxSize(), stroke = 8.dp, brush = Brand.mint)
                Text("${(info.usedFraction * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        // segmented usage bar
        val total = info.total.coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.15f))) {
            parts.forEach { (fc, col) ->
                val f = ((counts[fc]?.second ?: 0L).toFloat() / total).coerceIn(0f, 1f)
                if (f > 0.002f) Box(Modifier.fillMaxHeight().weight(f).background(col))
            }
            val rest = 1f - parts.sumOf { ((counts[it.first]?.second ?: 0L).toDouble() / total) }.toFloat()
            if (rest > 0.002f) Spacer(Modifier.weight(rest))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            parts.forEach { (fc, col) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(col))
                    Spacer(Modifier.width(5.dp))
                    Text(fc.title, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileCategoryList(cat: FileCat, nav: NavController, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val settings by c.prefs.state.collectAsStateWithLifecycle()
    var sort by rememberSaveable { mutableStateOf(Sort.DATE) }
    var query by rememberSaveable { mutableStateOf("") }
    var items by remember { mutableStateOf<List<ShareItem>?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var reload by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var dir by remember { mutableStateOf(FileBrowser.root) }
    var entries by remember { mutableStateOf<List<FileEntry>?>(null) }
    var canBrowse by remember { mutableStateOf(FileBrowser.canBrowse(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canBrowse = FileBrowser.canBrowse(ctx) }
    val deleter = rememberDeleter { gone ->
        val keys = gone.map { it.toString() }.toSet()
        items = items?.filterNot { it.uri.toString() in keys }
        selected = emptySet()
        if (gone.isNotEmpty()) ctx.toast("Deleted ${gone.size} file${if (gone.size == 1) "" else "s"}")
        reload++
    }

    LaunchedEffect(cat, reload, dir, canBrowse) {
        if (cat == FileCat.BROWSE) {
            entries = null
            if (canBrowse) entries = FileBrowser.list(dir)
            items = entries.orEmpty().filter { !it.isDir }.map { FileBrowser.toItem(it.file) }
            return@LaunchedEffect
        }
        items = null
        items = when (cat) {
            FileCat.RECEIVED -> c.media.received(settings?.saveTreeUri)
            FileCat.IMAGES -> c.media.photos()
            FileCat.VIDEOS -> c.media.videos()
            FileCat.AUDIO -> c.media.music()
            FileCat.DOCS -> c.media.documents().filter { it.category != Category.APP }
            FileCat.APPS -> c.media.apps()
            FileCat.APKS -> c.media.apkFiles()
            FileCat.BROWSE -> emptyList()
        }
    }
    BackHandler(selected.isNotEmpty()) { selected = emptySet() }
    BackHandler(selected.isEmpty() && cat == FileCat.BROWSE && dir != FileBrowser.root) { dir = dir.parentFile ?: FileBrowser.root }

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
    val isApps = cat == FileCat.APPS

    AppScaffold(
        if (selected.isEmpty()) cat.title else "${selected.size} selected",
        onBack = { if (selected.isNotEmpty()) selected = emptySet() else onBack() },
        actions = {
            if (selected.isNotEmpty()) {
                IconButton(onClick = { selected = if (selected.size == shown.size) emptySet() else shown.map { it.key }.toSet() }) { Icon(Icons.Rounded.SelectAll, "Select all") }
            } else if (cat != FileCat.BROWSE) {
                var menu by remember { mutableStateOf(false) }
                IconButton(onClick = { menu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
                DropdownMenu(menu, { menu = false }, shape = RoundedCornerShape(16.dp)) {
                    Sort.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.label) }, onClick = { sort = s; menu = false }, trailingIcon = { if (sort == s) Icon(Icons.Rounded.Check, null) })
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(selected.isNotEmpty(), enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                Surface(shadowElevation = 16.dp, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BarAction(Icons.AutoMirrored.Rounded.Send, "FlashShare", IconBrushes.blue) {
                            c.selection.set(selItems)
                            nav.withPerms(ctx, PermFlow.SEND, Routes.scan("send"))
                        }
                        BarAction(Icons.Rounded.Share, "Share", IconBrushes.violet) { ctx.shareFiles(selItems.map { it.uri }) }
                        if (!isApps) BarAction(Icons.Rounded.Delete, "Delete", IconBrushes.red) { confirmDelete = true }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (cat == FileCat.BROWSE) {
                if (!canBrowse) {
                    EmptyState(Icons.Rounded.FolderOpen, "Allow all files access", "Browse every folder on your phone.") {
                        GradientButton("Allow access", icon = Icons.Rounded.LockOpen, onClick = { requestAllFiles(ctx) })
                    }
                    return@Column
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    FileBrowser.pretty(dir).forEachIndexed { i, (name, f) ->
                        if (i > 0) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Text(name, style = MaterialTheme.typography.labelLarge, color = if (f == dir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (f == dir) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { dir = f }.padding(horizontal = 6.dp, vertical = 4.dp))
                    }
                }
            } else {
                OutlinedTextField(
                    query, { query = it }, placeholder = { Text("Search ${cat.title.lowercase()}") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(50),
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            val folders = if (cat == FileCat.BROWSE) entries.orEmpty().filter { it.isDir } else emptyList()
            when {
                cat != FileCat.BROWSE && items == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                cat == FileCat.BROWSE && entries == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                shown.isEmpty() && folders.isEmpty() -> EmptyState(cat.icon, "No files", if (cat == FileCat.RECEIVED) "Files you receive with FlashShare appear here." else "Nothing found.")
                else -> LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    if (cat != FileCat.BROWSE) item {
                        Text("${shown.size} ${if (isApps) "apps" else "files"} • ${formatSize(shown.sumOf { it.size })}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                    }
                    itemsIndexed(folders, key = { _, f -> "d:" + f.file.path }) { i, e ->
                        Row(
                            Modifier.fillMaxWidth().staggerIn(i, dir).clip(RoundedCornerShape(16.dp)).clickable { dir = e.file }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlossyIcon(Icons.Rounded.Folder, IconBrushes.orange, 46.dp, corner = 14.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${e.children} items • ${formatDate(e.modified)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    itemsIndexed(shown, key = { _, f -> f.key }) { i, f ->
                        val sel = f.key in selected
                        Row(
                            Modifier.fillMaxWidth().staggerIn(i, cat).clip(RoundedCornerShape(16.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else Color.Transparent)
                                .combinedClickable(
                                    onClick = {
                                        when {
                                            selected.isNotEmpty() -> selected = if (sel) selected - f.key else selected + f.key
                                            isApps -> f.packageName?.let { pkg ->
                                                runCatching { ctx.startActivity(ctx.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                            }
                                            else -> ctx.openFile(f.uri.toString(), f.mime)
                                        }
                                    },
                                    onLongClick = { selected = if (sel) selected - f.key else selected + f.key },
                                ).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FileThumb(f.uri.toString(), f.category, 50.dp, packageName = f.packageName, name = f.name, corner = 14.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (isApps) f.name.removeSuffix(".apk") else f.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (isApps) "${f.subtitle} • ${formatSize(f.size)}" else "${formatSize(f.size)} • ${formatDate(f.dateAdded)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                                )
                            }
                            when {
                                selected.isNotEmpty() -> Checkbox(sel, { selected = if (it) selected + f.key else selected - f.key })
                                isApps -> IconButton(onClick = {
                                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${f.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }) { Icon(Icons.Rounded.Info, "App info", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AppDialog(
            onDismiss = { confirmDelete = false }, icon = Icons.Rounded.DeleteForever,
            title = "Delete ${selItems.size} file${if (selItems.size == 1) "" else "s"}?",
            message = "${formatSize(selItems.sumOf { it.size })} will be permanently removed from this phone.",
            confirmText = "Delete", destructive = true,
            onConfirm = { confirmDelete = false; deleter(selItems.map { it.uri }) },
        )
    }
}

@Composable
private fun BarAction(icon: ImageVector, label: String, brush: Brush, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GlossyIcon(icon, brush, 40.dp, corner = 14.dp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
// endregion

// region cleaner
private enum class CleanStage { SCANNING, RESULTS, CLEANING, DONE }

@Composable
fun CleanerScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    var progress by remember { mutableFloatStateOf(0f) }
    var stageText by remember { mutableStateOf("Starting…") }
    var result by remember { mutableStateOf<CleanResult?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var includeCache by remember { mutableStateOf(true) }
    var freed by remember { mutableLongStateOf(0L) }
    var stage by remember { mutableStateOf(CleanStage.SCANNING) }
    var scanKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // What the user sees: eases toward the real progress so the % counts smoothly 1 → 100.
    val shownAnim = remember { Animatable(0f) }
    val shown = shownAnim.value
    LaunchedEffect(progress) { if (progress == 0f) shownAnim.snapTo(0f) else shownAnim.animateTo(progress, tween(260, easing = LinearEasing)) }
    LaunchedEffect(scanKey) {
        stage = CleanStage.SCANNING; result = null; freed = 0; progress = 0f
        val r = c.cleaner.scan { p, s -> if (p > progress) progress = p; stageText = s }
        progress = 1f
        // Wait until the counter has visibly reached 100%.
        snapshotFlow { shownAnim.value }.first { it >= 0.995f }
        delay(150)
        selected = (r.duplicates + r.apks + r.screenshots).map { it.uri.toString() }.toSet()
        result = r
        stage = CleanStage.RESULTS
    }

    val all = result?.let { (it.large + it.duplicates + it.apks + it.screenshots).distinctBy { i -> i.uri } }.orEmpty()
    val selBytes = all.filter { it.uri.toString() in selected }.sumOf { it.size } + if (includeCache) (result?.cacheBytes ?: 0) else 0
    var cacheFreed by remember { mutableLongStateOf(0L) }
    val deleter = rememberDeleter { gone ->
        val bytes = all.filter { it.uri in gone }.sumOf { it.size }
        scope.launch {
            freed = cacheFreed + bytes
            delay(900) // let the vortex animation finish
            stage = CleanStage.DONE
        }
    }

    AppScaffold("Cleaner", onBack = { nav.popBackStack() }) { pad ->
        val r = result
        AnimatedContent(stage, Modifier.padding(pad), transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "clean") { st ->
            when (st) {
                CleanStage.SCANNING -> Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RocketScene(shown, Modifier.fillMaxWidth().height(360.dp))
                    Spacer(Modifier.height(22.dp))
                    Text("${(shown * 100).toInt().coerceIn(1, 100)}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Text(stageText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(18.dp))
                    StepList(listOf("Scanning photos & videos", "Finding duplicates", "Checking installers & cache"),
                        when { shown < 0.3f -> 0; shown < 0.85f -> 1; shown < 0.995f -> 2; else -> 3 })
                }
                CleanStage.CLEANING -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CleanVortex(Modifier.size(300.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("Cleaning junk…", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    CountUpText(selBytes, { formatSize(it) }, MaterialTheme.typography.titleLarge, Brand.Mint, durationMs = 1000)
                }
                CleanStage.DONE -> Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        RocketScene(1f, Modifier.fillMaxWidth().height(280.dp), done = true)
                        Spacer(Modifier.height(24.dp))
                        CountUpText(freed, { "${formatSize(it)} freed" }, MaterialTheme.typography.headlineLarge, LocalExtra.current.success)
                        Spacer(Modifier.height(6.dp))
                        Text("Your phone has more room for what matters.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        GradientButton("Scan again", onClick = { scanKey++ }, icon = Icons.Rounded.Refresh, brush = Brand.mint, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = { nav.popBackStack() }) { Text("Done") }
                    }
                    ConfettiBurst(Modifier.fillMaxSize())
                }
                CleanStage.RESULTS -> if (r != null) Column(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(28.dp), spotColor = Brand.Mint).clip(RoundedCornerShape(28.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF0E3B3A), Color(0xFF0B8A74), Color(0xFF19C3A0)))).padding(22.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Junk found", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                                    CountUpText(r.total, { formatSize(it) }, MaterialTheme.typography.displaySmall, Color.White)
                                    Text("${all.size} files you may not need", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                                }
                                GlossyIcon(Icons.Rounded.AutoAwesome, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.12f))), 64.dp)
                            }
                        }
                        item {
                            AppCard(Modifier.fillMaxWidth().staggerIn(0), onClick = { includeCache = !includeCache }) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    GlossyIcon(Icons.Rounded.Cached, IconBrushes.mint, 46.dp)
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
                            Triple("Duplicate files", Icons.Rounded.FileCopy, r.duplicates) to IconBrushes.violet,
                            Triple("Large files (100 MB+)", Icons.Rounded.SdStorage, r.large) to IconBrushes.orange,
                            Triple("Old screenshots", Icons.Rounded.Screenshot, r.screenshots) to IconBrushes.cyan,
                            Triple("Installer files (APK)", Icons.Rounded.Android, r.apks) to IconBrushes.red,
                        ).forEachIndexed { i, (t, brush) ->
                            val (title, icon, list) = t
                            if (list.isNotEmpty()) item(key = title) {
                                CleanGroup(title, icon, brush, list, selected, Modifier.staggerIn(i + 1)) { selected = it }
                            }
                        }
                    }
                    Surface(shadowElevation = 16.dp, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Selected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatSize(selBytes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            GradientButton("Clean now", brush = Brand.mint, icon = Icons.Rounded.CleaningServices, enabled = selBytes > 0, onClick = {
                                val uris = all.filter { it.uri.toString() in selected }.map { it.uri }
                                stage = CleanStage.CLEANING
                                scope.launch {
                                    cacheFreed = if (includeCache) withContext(Dispatchers.IO) { c.cleaner.clearCache() } else 0L
                                    if (uris.isEmpty()) {
                                        freed = cacheFreed
                                        delay(1000)
                                        stage = CleanStage.DONE
                                    } else deleter(uris)
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
private fun CleanGroup(title: String, icon: ImageVector, brush: Brush, list: List<CleanItem>, selected: Set<String>, modifier: Modifier, onChange: (Set<String>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val keys = list.map { it.uri.toString() }
    val all = keys.all { it in selected }
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "rot")
    AppCard(modifier.fillMaxWidth()) {
        Row(Modifier.then(clickableNoRipple { open = !open }).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(icon, brush, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text("${list.size} files • ${formatSize(list.sumOf { it.size })}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ExpandMore, null, Modifier.graphicsLayer { rotationZ = rot })
            Checkbox(all, { onChange(if (it) selected + keys else selected - keys.toSet()) })
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 14.dp, end = 6.dp, bottom = 10.dp)) {
                list.take(100).forEach { item ->
                    val k = item.uri.toString()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        FileThumb(k, Category.fromMime(item.mime, item.name), 42.dp, name = item.name)
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
