package com.pdfmaster.app.ui.viewer

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Pages
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.R
import com.pdfmaster.app.ads.BannerAd
import com.pdfmaster.app.pdf.PdfDocumentSession
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.AppDialog
import com.pdfmaster.app.ui.components.EmptyState
import com.pdfmaster.app.ui.components.InputDialog
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.Shimmer
import com.pdfmaster.app.ui.components.SnackType
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.rememberActivity
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.tools.PdfTool
import com.pdfmaster.app.util.FileActions
import com.pdfmaster.app.util.Format
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ViewerScreen(uri: Uri) {
    val vm: ViewerViewModel = hiltViewModel()
    LaunchedEffect(uri) { vm.open(uri) }
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val deps = LocalAppDeps.current
    val activity = rememberActivity()
    val leave = { deps.ads.showInterstitial(activity) { nav.back() } }
    BackHandler { leave() }

    Box(Modifier.fillMaxSize().background(AppTheme.colors.pageBackground)) {
        when (val s = state) {
            ViewerState.Loading -> LoadingPages(onBack = { nav.back() })
            is ViewerState.NeedsPassword -> PasswordGate(s, onSubmit = vm::submitPassword, onBack = { nav.back() })
            is ViewerState.Error -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Spacer(Modifier.weight(0.3f))
                EmptyState(R.drawable.img_error_generic, "Can't open this file", s.message, action = "Go back", onAction = { nav.back() })
                Spacer(Modifier.weight(0.7f))
            }
            is ViewerState.Ready -> ViewerContent(s, vm, onBack = leave)
        }
    }
}

@Composable
private fun LoadingPages(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Shimmer(Modifier.width(160.dp).height(16.dp))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(2) { Shimmer(Modifier.fillMaxWidth().height(460.dp), RoundedCornerShape(6.dp)) }
        }
    }
}

@Composable
private fun PasswordGate(state: ViewerState.NeedsPassword, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        InputDialog(
            title = "Password protected",
            message = "Enter the password to open this document.",
            label = "Password",
            initial = "",
            confirmText = "Open",
            password = true,
            image = R.drawable.img_password_lock,
            error = if (state.wrong) "Incorrect password. Try again." else null,
            loading = state.checking,
            onConfirm = onSubmit,
            onDismiss = onBack,
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ViewerContent(state: ViewerState.Ready, vm: ViewerViewModel, onBack: () -> Unit) {
    val session = state.session
    val deps = LocalAppDeps.current
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val activity = rememberActivity()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.startPage)
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle(emptyList())
    val searchState by vm.search.collectAsStateWithLifecycle()
    val keepOn by deps.prefs.keepScreenOn.collectAsStateWithLifecycle(true)
    val nightDefault by deps.prefs.nightReading.collectAsStateWithLifecycle(false)

    var chrome by rememberSaveable { mutableStateOf(true) }
    var night by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val isNight = night ?: nightDefault
    var searching by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var showThumbs by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showGoTo by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showRate by remember { mutableStateOf(false) }

    var container by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var renderScale by remember { mutableFloatStateOf(1f) }

    val currentPage by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()
            if (first == null) listState.firstVisibleItemIndex
            else if (first.offset + first.size / 2 < info.viewportStartOffset + 80) (first.index + 1).coerceAtMost(session.pageCount - 1)
            else first.index
        }
    }
    val pageLabel = "${currentPage + 1} / ${session.pageCount}"
    val bookmarked = currentPage in bookmarks

    DisposableEffect(keepOn) {
        val window = activity.window
        if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { currentPage }.distinctUntilChanged().debounce(600).collect { vm.saveLastPage(it) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { scale }.debounce(220).collect { renderScale = it.coerceIn(1f, 2.5f) }
    }
    LaunchedEffect(Unit) {
        if (state.startPage > 0) {
            snackbar.show("Resumed at page ${state.startPage + 1}", SnackType.INFO, action = "Start") {
                scope.launch { listState.animateScrollToItem(0) }
            }
        }
        if (vm.openCount == 4 && !deps.prefs.rateDialogShown()) {
            delay(5000)
            deps.prefs.setRateDialogShown()
            showRate = true
        }
    }

    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (scale <= 1.01f || container.height == 0) return Offset.Zero
                val maxY = (scale - 1f) * container.height / 2f
                val target = (offsetY + available.y * scale).coerceIn(-maxY, maxY)
                val consumed = (target - offsetY) / scale
                offsetY = target
                return Offset(0f, consumed)
            }
        }
    }

    fun clampOffsets() {
        val maxX = (scale - 1f) * container.width / 2f
        val maxY = (scale - 1f) * container.height / 2f
        offsetX = offsetX.coerceIn(-maxX, maxX)
        offsetY = offsetY.coerceIn(-maxY, maxY)
    }

    fun zoomTo(target: Float, focus: Offset) {
        scope.launch {
            val center = Offset(container.width / 2f, container.height / 2f)
            val startScale = scale
            val startX = offsetX
            val startY = offsetY
            animate(0f, 1f, animationSpec = spring(stiffness = 500f)) { t, _ ->
                val s = startScale + (target - startScale) * t
                val tx = focus.x - center.x - (focus.x - center.x - startX) * (target / startScale)
                val ty = focus.y - center.y - (focus.y - center.y - startY) * (target / startScale)
                scale = s
                offsetX = startX + ((if (target <= 1f) 0f else tx) - startX) * t
                offsetY = startY + ((if (target <= 1f) 0f else ty) - startY) * t
                clampOffsets()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onSizeChanged { container = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { chrome = !chrome },
                        onDoubleTap = { pos -> zoomTo(if (scale > 1.2f) 1f else 2.4f, pos) },
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.count { it.pressed }
                            if (pressed >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val c = event.calculateCentroid()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                if (c != Offset.Unspecified) {
                                    offsetX = c.x - center.x - (c.x - center.x - offsetX) * (newScale / scale) + pan.x
                                    offsetY = c.y - center.y - (c.y - center.y - offsetY) * (newScale / scale) + pan.y
                                }
                                scale = newScale
                                clampOffsets()
                                event.changes.forEach { it.consume() }
                            } else if (scale > 1.01f) {
                                val change = event.changes.firstOrNull()
                                if (change != null) {
                                    offsetX += change.positionChange().x
                                    clampOffsets()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                        if (scale < 1.02f) {
                            scale = 1f; offsetX = 0f; offsetY = 0f
                        }
                    }
                }
                .nestedScroll(nestedScroll),
        ) {
            val itemWidthPx = with(density) { (maxWidth - 20.dp).toPx() }
            val renderWidth = (itemWidthPx * renderScale).roundToInt().coerceAtMost(2400)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale; scaleY = scale; translationX = offsetX; translationY = offsetY
                },
                contentPadding = PaddingValues(top = 96.dp, bottom = 150.dp, start = 10.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(session.pageCount, key = { it }) { index ->
                    PdfPage(
                        session, index, renderWidth,
                        modifier = Modifier.shadow(3.dp, RoundedCornerShape(3.dp)).clip(RoundedCornerShape(3.dp)),
                        night = isNight,
                    )
                }
            }
        }

        // Floating page indicator
        val scrolling = listState.isScrollInProgress
        var showIndicator by remember { mutableStateOf(false) }
        LaunchedEffect(scrolling) {
            if (scrolling) showIndicator = true else { delay(1200); showIndicator = false }
        }
        AnimatedVisibility(
            visible = showIndicator || !chrome,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (chrome) 150.dp else 24.dp),
        ) {
            Text(
                pageLabel,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xCC1B1D29)).padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }

        if (session.pageCount > 4) {
            FastScroller(
                count = session.pageCount,
                current = currentPage,
                visible = scrolling || showIndicator,
                onScrollTo = { scope.launch { listState.scrollToItem(it) } },
                modifier = Modifier.align(Alignment.CenterEnd).padding(top = 120.dp, bottom = 170.dp),
            )
        }

        // Top bar
        AnimatedVisibility(
            visible = chrome || searching,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
                if (searching) {
                    SearchBar(
                        onSearch = vm::runSearch,
                        onClose = { searching = false; vm.clearSearch() },
                    )
                } else {
                    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                        Column(Modifier.weight(1f)) {
                            Text(state.file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Page ${currentPage + 1} of ${session.pageCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { searching = true }) { Icon(Icons.Rounded.Search, "Search") }
                        IconButton(onClick = {
                            vm.toggleBookmark(currentPage, !bookmarked)
                            snackbar.show(if (bookmarked) "Bookmark removed" else "Page ${currentPage + 1} bookmarked", SnackType.SUCCESS)
                        }) {
                            Icon(
                                if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Bookmark",
                                tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                            DropdownMenu(menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                                MenuItem(Icons.Rounded.Pages, "Go to page") { menu = false; showGoTo = true }
                                MenuItem(Icons.Rounded.Bookmarks, "Bookmarks") { menu = false; showBookmarks = true }
                                MenuItem(Icons.Rounded.GridView, "All pages") { menu = false; showThumbs = true }
                                MenuItem(Icons.Rounded.Print, "Print") { menu = false; runCatching { FileActions.print(context, state.file.uri, state.file.name) } }
                                MenuItem(Icons.Rounded.Compress, "Compress") { menu = false; nav.tool(PdfTool.COMPRESS, state.file.uri) }
                                MenuItem(Icons.Rounded.Info, "File details") { menu = false; showInfo = true }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SearchResults(searching, searchState) { page ->
                    scope.launch { listState.animateScrollToItem(page) }
                    vm.clearSearch()
                }
            }
        }

        // Bottom toolbar + banner
        AnimatedVisibility(
            visible = chrome && !searching,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                ) {
                    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ToolbarAction(Icons.Rounded.EditNote, "Annotate") { nav.editor(state.file.uri, "annotate") }
                        ToolbarAction(Icons.Rounded.Draw, "Sign") { nav.editor(state.file.uri, "sign") }
                        ToolbarAction(Icons.Rounded.GridView, "Pages") { showThumbs = true }
                        ToolbarAction(if (isNight) Icons.Rounded.LightMode else Icons.Rounded.Nightlight, if (isNight) "Day" else "Night") { night = !isNight }
                        ToolbarAction(Icons.Rounded.Share, "Share") { runCatching { FileActions.share(context, listOf(state.file.uri)) } }
                    }
                }
                Box(Modifier.navigationBarsPadding()) { BannerAd() }
            }
        }
    }

    if (showThumbs) {
        ThumbnailSheet(session, currentPage, bookmarks, onDismiss = { showThumbs = false }) { page ->
            showThumbs = false
            scope.launch { listState.scrollToItem(page) }
        }
    }
    if (showBookmarks) {
        BookmarksSheet(session, bookmarks, onDismiss = { showBookmarks = false }) { page ->
            showBookmarks = false
            scope.launch { listState.scrollToItem(page) }
        }
    }
    if (showGoTo) {
        InputDialog(
            title = "Go to page",
            label = "Page (1 – ${session.pageCount})",
            initial = "",
            confirmText = "Go",
            numeric = true,
            validator = { it.toIntOrNull() in 1..session.pageCount },
            onDismiss = { showGoTo = false },
            onConfirm = { value ->
                showGoTo = false
                value.toIntOrNull()?.let { scope.launch { listState.scrollToItem(it - 1) } }
            },
        )
    }
    if (showInfo) {
        AppDialog(
            title = state.file.name,
            message = listOf(
                "Size: ${Format.size(state.file.size)}",
                "Pages: ${session.pageCount}",
                "Bookmarks: ${bookmarks.size}",
            ).joinToString("\n"),
            confirmText = "Close",
            dismissText = null,
            onConfirm = { showInfo = false },
            onDismiss = { showInfo = false },
        )
    }
    if (showRate) {
        AppDialog(
            title = "Enjoying PDF Master?",
            message = "A quick 5-star rating helps us keep improving the tools you use every day.",
            confirmText = "Rate us",
            dismissText = "Maybe later",
            image = R.drawable.img_success_done,
            onDismiss = { showRate = false },
            onConfirm = {
                showRate = false
                val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                runCatching { context.startActivity(market) }
            },
        )
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        onClick = onClick,
    )
}

@Composable
private fun ToolbarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchBar(onSearch: (String) -> Unit, onClose: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(Modifier.fillMaxWidth().height(60.dp).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Close search") }
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search in document") },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); onSearch(query) }),
            modifier = Modifier.weight(1f).focusRequester(focus),
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { query = ""; onSearch("") }) { Icon(Icons.Rounded.Close, "Clear") }
        }
    }
}

@Composable
private fun SearchResults(visible: Boolean, state: SearchState, onPick: (Int) -> Unit) {
    AnimatedVisibility(visible && state != SearchState.Idle, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
            when (state) {
                SearchState.Searching -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                    Text("Searching…", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is SearchState.Done -> {
                    Text(
                        if (state.hits.isEmpty()) "No results for \"${state.query}\"" else "${state.hits.size} page(s) contain \"${state.query}\"",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    LazyColumn {
                        items(state.hits) { (page, snippet) ->
                            Row(Modifier.fillMaxWidth().clickable { onPick(page) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(
                                    "${page + 1}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(40.dp),
                                )
                                Text(snippet, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                SearchState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun FastScroller(count: Int, current: Int, visible: Boolean, onScrollTo: (Int) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf(false) }
    var trackHeight by remember { mutableFloatStateOf(1f) }
    val alpha by animateFloatAsState(if (visible || dragging) 1f else 0f, tween(250), label = "fs")
    val thumbH = with(LocalDensity.current) { 44.dp.toPx() }
    val fraction = if (count <= 1) 0f else current / (count - 1).toFloat()
    Box(
        modifier
            .fillMaxHeight()
            .width(36.dp)
            .graphicsLayer { this.alpha = alpha }
            .onSizeChanged { trackHeight = it.height.toFloat() }
            .pointerInput(count) {
                detectVerticalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    val f = ((change.position.y - thumbH / 2) / (trackHeight - thumbH)).coerceIn(0f, 1f)
                    onScrollTo((f * (count - 1)).roundToInt())
                }
            },
    ) {
        val y = fraction * (trackHeight - thumbH)
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(0, y.roundToInt()) }
                .align(Alignment.TopEnd)
                .padding(end = 4.dp)
                .size(width = 8.dp, height = 44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        if (dragging) {
            Text(
                "${current + 1}",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(-140, y.roundToInt()) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ThumbnailSheet(session: PdfDocumentSession, current: Int, bookmarks: List<Int>, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val sheet = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Text("All pages", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            items(session.pageCount) { i ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(if (i == current) 2.5.dp else 1.dp, if (i == current) MaterialTheme.colorScheme.primary else AppTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                            .clickable { onPick(i) },
                    ) {
                        PdfPage(session, i, 260)
                        if (i in bookmarks) {
                            Icon(Icons.Rounded.Bookmark, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BookmarksSheet(session: PdfDocumentSession, bookmarks: List<Int>, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val sheet = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 24.dp)) {
            Text("Bookmarks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
            if (bookmarks.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.img_empty_favorites), null, Modifier.size(120.dp))
                    Text("No bookmarks yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tap the bookmark icon at the top to save the page you're reading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                bookmarks.forEach { page ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(page) }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(44.dp).clip(RoundedCornerShape(6.dp))) { PdfPage(session, page, 140) }
                        Spacer(Modifier.width(14.dp))
                        Text("Page ${page + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
