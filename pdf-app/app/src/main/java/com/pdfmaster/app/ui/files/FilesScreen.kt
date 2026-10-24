package com.pdfmaster.app.ui.files

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.R
import com.pdfmaster.app.ads.NativeAdCard
import com.pdfmaster.app.data.model.PdfFile
import com.pdfmaster.app.data.prefs.SortOrder
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.EmptyState
import com.pdfmaster.app.ui.components.FileRow
import com.pdfmaster.app.ui.components.FileRowPlaceholder
import com.pdfmaster.app.ui.components.PrimaryButton
import com.pdfmaster.app.ui.components.SecondaryButton
import com.pdfmaster.app.ui.navigation.LocalNavigator
import kotlinx.coroutines.launch

private val tabTitles = listOf("Device", "Recent", "Favourites", "Created")

@Composable
fun FilesScreen(contentPadding: PaddingValues) {
    val deps = LocalAppDeps.current
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = 1) { tabTitles.size }
    val sort by deps.prefs.sortOrder.collectAsStateWithLifecycle(SortOrder.DATE)
    var sortMenu by remember { mutableStateOf(false) }

    var hasAccess by remember { mutableStateOf(deps.files.hasStorageAccess()) }
    var device by remember { mutableStateOf<List<PdfFile>?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val refreshDevice: () -> Unit = {
        scope.launch {
            refreshing = true
            device = deps.files.scanDevice()
            refreshing = false
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = deps.files.hasStorageAccess()
        if (now != hasAccess || (now && device == null)) {
            hasAccess = now
            if (now) refreshDevice()
        }
    }
    val fileActions = rememberFileActions(onChanged = { if (hasAccess) refreshDevice() })

    val recent by deps.files.recent.collectAsStateWithLifecycle(null)
    val favorites by deps.files.favorites.collectAsStateWithLifecycle(null)
    val created by deps.files.created.collectAsStateWithLifecycle(null)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) nav.viewer(uri) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("My Files", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { nav.search() }) { Icon(Icons.Rounded.Search, "Search") }
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }, shape = RoundedCornerShape(16.dp)) {
                        listOf(SortOrder.DATE to "Date modified", SortOrder.NAME to "Name", SortOrder.SIZE to "File size").forEach { (order, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontWeight = if (order == sort) FontWeight.Bold else FontWeight.Normal) },
                                trailingIcon = { if (order == sort) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    sortMenu = false
                                    scope.launch { deps.prefs.setSortOrder(order) }
                                },
                            )
                        }
                    }
                }
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(tabTitles) { i, title ->
                    val selected = pager.currentPage == i
                    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, label = "tab")
                    val count = when (i) {
                        0 -> device?.size
                        1 -> recent?.size
                        2 -> favorites?.size
                        else -> created?.size
                    }
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(bg).clickable { scope.launch { pager.animateScrollToPage(i) } }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                        if (count != null && count > 0) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "$count",
                                color = if (selected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
            HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
                val bottom = contentPadding.calculateBottomPadding() + 96.dp
                when (page) {
                    0 -> if (!hasAccess) {
                        StoragePermissionCard(onBrowse = { picker.launch(arrayOf("application/pdf")) }, bottom = bottom)
                    } else {
                        PullToRefreshBox(isRefreshing = refreshing, onRefresh = refreshDevice) {
                            FileList(device?.sorted(sort), bottom, fileActions, R.drawable.img_empty_files, "No PDFs found", "We couldn't find any PDF files on this device yet.")
                        }
                    }
                    1 -> FileList(recent?.sorted(sort), bottom, fileActions, R.drawable.img_empty_files, "Nothing here yet", "Files you open will appear here.")
                    2 -> FileList(favorites?.sorted(sort), bottom, fileActions, R.drawable.img_empty_favorites, "No favourites", "Tap the star on any file to keep it here.")
                    else -> FileList(created?.sorted(sort), bottom, fileActions, R.drawable.img_success_done, "No creations yet", "Files you make with our tools are saved here.")
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { picker.launch(arrayOf("application/pdf")) },
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text("Open PDF", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp),
        )
    }
}

private fun List<PdfFile>.sorted(order: SortOrder): List<PdfFile> = when (order) {
    SortOrder.DATE -> sortedByDescending { it.date }
    SortOrder.NAME -> sortedBy { it.name.lowercase() }
    SortOrder.SIZE -> sortedByDescending { it.size }
}

@Composable
private fun FileList(
    files: List<PdfFile>?, bottom: androidx.compose.ui.unit.Dp, actions: FileActionsController,
    emptyImage: Int, emptyTitle: String, emptyBody: String,
) {
    val nav = LocalNavigator.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottom), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            files == null -> items(6) { FileRowPlaceholder() }
            files.isEmpty() -> item { EmptyState(emptyImage, emptyTitle, emptyBody, modifier = Modifier.padding(top = 24.dp)) }
            else -> itemsIndexed(files, key = { _, f -> f.key }) { i, file ->
                Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FileRow(file, onClick = { nav.viewer(file.uri) }, onMore = { actions.open(file) }, onFavorite = { actions.toggleFavorite(file) })
                    if (i == 2 || (files.size < 3 && i == files.lastIndex)) NativeAdCard(showMedia = false)
                }
            }
        }
    }
}

@Composable
private fun StoragePermissionCard(onBrowse: () -> Unit, bottom: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val legacy = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.img_permission_storage), null, Modifier.size(200.dp))
        Text("See every PDF on your phone", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Allow file access so PDF Master can find documents in Downloads, WhatsApp, Telegram and other folders. Your files never leave your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Allow access", onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                runCatching { context.startActivity(intent) }
                    .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) } }
            } else {
                legacy.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        })
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Pick a file instead", onBrowse)
    }
}
