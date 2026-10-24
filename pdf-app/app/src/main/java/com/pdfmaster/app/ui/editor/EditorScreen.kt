package com.pdfmaster.app.ui.editor

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixNormal
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.R
import com.pdfmaster.app.pdf.ImageMark
import com.pdfmaster.app.pdf.InkMark
import com.pdfmaster.app.pdf.PageMark
import com.pdfmaster.app.pdf.PdfDocumentSession
import com.pdfmaster.app.pdf.TextMark
import com.pdfmaster.app.ui.components.AppDialog
import com.pdfmaster.app.ui.components.EmptyState
import com.pdfmaster.app.ui.components.FullScreenLoading
import com.pdfmaster.app.ui.components.InputDialog
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.navigation.Routes
import com.pdfmaster.app.ui.result.ToolResult
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand
import com.pdfmaster.app.ui.tool.RunStateHost
import com.pdfmaster.app.ui.tool.ToolViewModel
import com.pdfmaster.app.ui.tools.PdfTool
import com.pdfmaster.app.ui.tools.rememberToolLauncher
import com.pdfmaster.app.ui.viewer.PdfPage
import com.pdfmaster.app.util.Format
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class EditTool(val label: String, val icon: ImageVector) {
    HAND("Move", Icons.Rounded.PanTool),
    PEN("Pen", Icons.Rounded.Create),
    HIGHLIGHT("Highlight", Icons.Rounded.BorderColor),
    TEXT("Text", Icons.Rounded.TextFields),
    NOTE("Note", Icons.Rounded.NoteAlt),
    SIGN("Sign", Icons.Rounded.Draw),
    ERASER("Eraser", Icons.Rounded.AutoFixNormal),
}

private val penColors = listOf(Color(0xFF111827), Color(0xFFE5252A), Color(0xFF2563EB), Color(0xFF16A34A))
private val highlightColors = listOf(Color(0xFFFFE600), Color(0xFF7CFF6B), Color(0xFFFF7EB6), Color(0xFF7DD3FC))
private val penWidths = listOf(0.003f, 0.006f, 0.011f)
private const val NOTE_BG = 0xFFFFF3A3.toInt()

@Composable
fun EditorScreen(uri: Uri, mode: String) {
    val vm: EditorViewModel = hiltViewModel()
    val runner: ToolViewModel = hiltViewModel()
    LaunchedEffect(uri) { vm.open(uri) }
    val load by vm.load.collectAsStateWithLifecycle()
    val runState by runner.state.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    val launcher = rememberToolLauncher()
    var confirmExit by remember { mutableStateOf(false) }
    val exit = { if (vm.marks.isNotEmpty()) confirmExit = true else nav.back() }
    BackHandler { exit() }

    Box(Modifier.fillMaxSize().background(AppTheme.colors.pageBackground)) {
        when (val s = load) {
            EditorLoad.Loading -> FullScreenLoading()
            is EditorLoad.Error -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                IconButton(onClick = { nav.back() }) { Icon(Icons.Rounded.Close, "Close") }
                Spacer(Modifier.weight(0.3f))
                EmptyState(
                    if (s.locked) R.drawable.img_password_lock else R.drawable.img_error_generic,
                    if (s.locked) "Unlock to edit" else "Can't open this file",
                    s.message,
                    action = if (s.locked) "Unlock PDF" else "Go back",
                    onAction = { if (s.locked) launcher.launch(PdfTool.UNLOCK) else nav.back() },
                )
                Spacer(Modifier.weight(0.7f))
            }
            is EditorLoad.Ready -> EditorContent(uri, mode, s.session, vm, runner, onClose = exit)
        }
    }
    RunStateHost(runState, Routes.EDITOR, runner::reset)
    if (confirmExit) {
        AppDialog(
            title = "Discard changes?",
            message = "Your annotations haven't been saved yet.",
            confirmText = "Discard",
            dismissText = "Keep editing",
            destructive = true,
            onDismiss = { confirmExit = false },
            onConfirm = { confirmExit = false; nav.back() },
        )
    }
}

@Composable
private fun EditorContent(uri: Uri, mode: String, session: PdfDocumentSession, vm: EditorViewModel, runner: ToolViewModel, onClose: () -> Unit) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val sign = mode == "sign"
    var tool by rememberSaveable { mutableStateOf(if (sign) EditTool.SIGN else EditTool.PEN) }
    var penColor by remember { mutableStateOf(penColors[1]) }
    var hlColor by remember { mutableStateOf(highlightColors[0]) }
    var widthIndex by remember { mutableStateOf(1) }
    var selected by remember { mutableStateOf<PageMark?>(null) }
    var textRequest by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    var noteMode by remember { mutableStateOf(false) }
    var showPad by remember { mutableStateOf(sign) }
    val pager = rememberPagerState { session.pageCount }

    fun save() {
        if (vm.marks.isEmpty()) {
            snackbar.info(if (sign) "Add your signature to the page first." else "Draw, highlight or add text before saving.")
            return
        }
        val marks = vm.marks.toList()
        val pdfTool = if (sign) PdfTool.SIGN else PdfTool.ANNOTATE
        runner.run(pdfTool, if (sign) "Signing your document" else "Saving your edits") {
            val meta = runner.meta(uri)
            val input = copy(uri)
            val out = work()
            tools.applyMarks(input, marks, out) { progress(it) }
            input.delete()
            val output = publishPdf(out, storage.uniqueName(Format.baseName(meta.name), "pdf", if (sign) "signed" else "edited"))
            ToolResult(pdfTool, if (sign) "Document signed" else "Edits saved", "Your PDF has been saved with all changes.", listOf(output))
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close") }
                Column(Modifier.weight(1f)) {
                    Text(if (sign) "Sign document" else "Edit & annotate", style = MaterialTheme.typography.titleMedium)
                    Text("Page ${pager.currentPage + 1} of ${session.pageCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { selected = null; vm.undo() }, enabled = vm.marks.isNotEmpty()) { Icon(Icons.AutoMirrored.Rounded.Undo, "Undo") }
                IconButton(onClick = { vm.redo() }, enabled = vm.canRedo) { Icon(Icons.AutoMirrored.Rounded.Redo, "Redo") }
                Text(
                    "Save",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.RedHorizontal).clickable { save() }.padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }

        // Pages
        Box(Modifier.weight(1f).fillMaxWidth()) {
            HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = tool == EditTool.HAND || tool == EditTool.SIGN, beyondViewportPageCount = 1) { page ->
                BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    val size = session.pageSizes[page]
                    val aspect = size.width.toFloat() / size.height
                    val boxAspect = maxWidth / maxHeight
                    val w = if (aspect > boxAspect) maxWidth else maxHeight * aspect
                    val h = w / aspect
                    val density = LocalDensity.current
                    val wPx = with(density) { w.toPx() }
                    val hPx = with(density) { h.toPx() }
                    Box(Modifier.size(w, h).shadow(6.dp, RoundedCornerShape(2.dp)).background(Color.White)) {
                        PdfPage(session, page, wPx.roundToInt().coerceAtMost(2000))
                        PageOverlay(
                            page = page, wPx = wPx, hPx = hPx,
                            marks = vm.marks.filter { it.page == page },
                            tool = tool,
                            inkColor = if (tool == EditTool.HIGHLIGHT) hlColor else penColor,
                            inkWidth = if (tool == EditTool.HIGHLIGHT) 0.028f else penWidths[widthIndex],
                            selected = selected,
                            onSelect = { selected = it },
                            onAdd = { vm.add(it) },
                            onReplace = { old, new -> vm.replace(old, new); if (selected == old) selected = new },
                            onRemove = { vm.remove(it); if (selected == it) selected = null },
                            onTextAt = { pos -> textRequest = page to pos; noteMode = tool == EditTool.NOTE },
                        )
                    }
                }
            }
            // Page arrows
            if (session.pageCount > 1) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).clip(RoundedCornerShape(50)).background(Color(0xCC1B1D29)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }, enabled = pager.currentPage > 0) {
                        Icon(Icons.Rounded.ChevronLeft, "Previous page", tint = Color.White)
                    }
                    Text("${pager.currentPage + 1} / ${session.pageCount}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, enabled = pager.currentPage < session.pageCount - 1) {
                        Icon(Icons.Rounded.ChevronRight, "Next page", tint = Color.White)
                    }
                }
            }
        }

        // Tool options
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
            Column(Modifier.navigationBarsPadding()) {
                AnimatedVisibility(tool == EditTool.PEN || tool == EditTool.HIGHLIGHT, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val colors = if (tool == EditTool.HIGHLIGHT) highlightColors else penColors
                        val current = if (tool == EditTool.HIGHLIGHT) hlColor else penColor
                        colors.forEach { c ->
                            Box(
                                Modifier.padding(end = 10.dp).size(30.dp).clip(CircleShape).background(c)
                                    .border(if (c == current) 3.dp else 1.dp, if (c == current) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { if (tool == EditTool.HIGHLIGHT) hlColor = c else penColor = c },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (tool == EditTool.PEN) {
                            penWidths.forEachIndexed { i, _ ->
                                val active = i == widthIndex
                                Box(
                                    Modifier.padding(start = 6.dp).size(36.dp).clip(CircleShape)
                                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { widthIndex = i },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(Modifier.size((5 + i * 5).dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface))
                                }
                            }
                        }
                    }
                }
                AnimatedVisibility(tool == EditTool.TEXT || tool == EditTool.NOTE || tool == EditTool.ERASER || tool == EditTool.HAND, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Text(
                        when (tool) {
                            EditTool.TEXT -> "Tap anywhere on the page to add text"
                            EditTool.NOTE -> "Tap where you want to stick a note"
                            EditTool.ERASER -> "Tap a stroke to erase it"
                            else -> "Swipe to change page · drag items to move them"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyRow(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    items(EditTool.entries) { t ->
                        val active = t == tool
                        val bg by animateColorAsState(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "tool")
                        Column(
                            Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                                .clickable {
                                    tool = t
                                    selected = null
                                    if (t == EditTool.SIGN) showPad = true
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(t.icon, null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(3.dp))
                            Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    textRequest?.let { (page, pos) ->
        InputDialog(
            title = if (noteMode) "Add a note" else "Add text",
            label = if (noteMode) "Note" else "Text",
            initial = "",
            confirmText = "Add",
            onDismiss = { textRequest = null },
            onConfirm = { text ->
                textRequest = null
                val mark = TextMark(
                    page = page, text = text, x = pos.x.coerceIn(0f, 0.9f), y = pos.y.coerceIn(0f, 0.95f),
                    size = if (noteMode) 0.022f else 0.028f,
                    color = if (noteMode) 0xFF3A2E00.toInt() else penColor.toArgb(),
                    background = if (noteMode) NOTE_BG else null,
                )
                vm.add(mark)
                selected = mark
            },
        )
    }

    if (showPad) {
        SignaturePadSheet(
            store = vm.signatures,
            onDismiss = { showPad = false },
            onPicked = { file ->
                showPad = false
                val bmp = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(file.path, it) }
                val page = pager.currentPage
                val pageSize = session.pageSizes[page]
                val w = 0.34f
                val h = (w * pageSize.width / pageSize.height) * (bmp.outHeight.toFloat() / bmp.outWidth.coerceAtLeast(1))
                val mark = ImageMark(page, file.path, 0.5f - w / 2f, (0.72f).coerceAtMost(0.95f - h), w, h)
                vm.add(mark)
                selected = mark
                tool = EditTool.HAND
                snackbar.show("Drag your signature into place, then tap Save.", com.pdfmaster.app.ui.components.SnackType.SUCCESS)
            },
        )
    }
}

@Composable
private fun PageOverlay(
    page: Int,
    wPx: Float,
    hPx: Float,
    marks: List<PageMark>,
    tool: EditTool,
    inkColor: Color,
    inkWidth: Float,
    selected: PageMark?,
    onSelect: (PageMark?) -> Unit,
    onAdd: (PageMark) -> Unit,
    onReplace: (PageMark, PageMark) -> Unit,
    onRemove: (PageMark) -> Unit,
    onTextAt: (Offset) -> Unit,
) {
    var live by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val currentTool by rememberUpdatedState(tool)
    val color by rememberUpdatedState(inkColor)
    val width by rememberUpdatedState(inkWidth)
    val currentMarks by rememberUpdatedState(marks)
    val highlight = tool == EditTool.HIGHLIGHT

    fun commit(points: List<Offset>) {
        if (points.isEmpty()) return
        val arr = FloatArray(points.size * 2)
        points.forEachIndexed { i, p -> arr[i * 2] = p.x / wPx; arr[i * 2 + 1] = p.y / hPx }
        onAdd(InkMark(page, arr, color.toArgb(), width, if (currentTool == EditTool.HIGHLIGHT) 0.38f else 1f))
    }

    fun eraseAt(p: Offset) {
        val hit = currentMarks.filterIsInstance<InkMark>().lastOrNull { m ->
            var i = 0
            var found = false
            while (i + 1 < m.points.size && !found) {
                found = hypot(m.points[i] * wPx - p.x, m.points[i + 1] * hPx - p.y) < 28f
                i += 2
            }
            found
        }
        if (hit != null) onRemove(hit)
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(page) {
                detectTapGestures { pos ->
                    when (currentTool) {
                        EditTool.PEN, EditTool.HIGHLIGHT -> commit(listOf(pos))
                        EditTool.TEXT, EditTool.NOTE -> onTextAt(Offset(pos.x / wPx, pos.y / hPx))
                        EditTool.ERASER -> eraseAt(pos)
                        else -> onSelect(null)
                    }
                }
            }
            .pointerInput(page, tool) {
                if (tool == EditTool.PEN || tool == EditTool.HIGHLIGHT || tool == EditTool.ERASER) {
                    detectDragGestures(
                        onDragStart = { if (currentTool == EditTool.ERASER) eraseAt(it) else live = listOf(it) },
                        onDragEnd = { if (currentTool != EditTool.ERASER) commit(live); live = emptyList() },
                        onDragCancel = { live = emptyList() },
                    ) { change, _ ->
                        change.consume()
                        if (currentTool == EditTool.ERASER) eraseAt(change.position) else live = live + change.position
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            marks.filterIsInstance<InkMark>().forEach { m ->
                drawPath(
                    m.points.toPath(wPx, hPx),
                    Color(m.color).copy(alpha = m.alpha),
                    style = Stroke(m.width * wPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            if (live.isNotEmpty()) {
                val arr = FloatArray(live.size * 2)
                live.forEachIndexed { i, p -> arr[i * 2] = p.x / wPx; arr[i * 2 + 1] = p.y / hPx }
                drawPath(arr.toPath(wPx, hPx), inkColor.copy(alpha = if (highlight) 0.38f else 1f), style = Stroke(inkWidth * wPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        marks.forEach { m ->
            when (m) {
                is TextMark -> PlacedText(m, wPx, hPx, m == selected, onSelect, onReplace, onRemove)
                is ImageMark -> PlacedImage(m, wPx, hPx, m == selected, onSelect, onReplace, onRemove)
                else -> Unit
            }
        }
    }
}

private fun FloatArray.toPath(w: Float, h: Float): Path = Path().apply {
    if (size < 2) return@apply
    moveTo(this@toPath[0] * w, this@toPath[1] * h)
    if (size == 2) lineTo(this@toPath[0] * w + 0.5f, this@toPath[1] * h)
    var i = 2
    while (i + 1 < size) {
        lineTo(this@toPath[i] * w, this@toPath[i + 1] * h)
        i += 2
    }
}

@Composable
private fun SelectionFrame(selected: Boolean, onRemove: () -> Unit, content: @Composable () -> Unit) {
    Box {
        content()
        if (selected) {
            Canvas(Modifier.matchParentSize()) {
                drawRect(Brand.Red, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            }
            Box(
                Modifier.align(Alignment.TopEnd).offset(10.dp, (-10).dp).size(22.dp).clip(CircleShape).background(Brand.Red).clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Remove", tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun PlacedText(
    mark: TextMark, wPx: Float, hPx: Float, selected: Boolean,
    onSelect: (PageMark?) -> Unit, onReplace: (PageMark, PageMark) -> Unit, onRemove: (PageMark) -> Unit,
) {
    val density = LocalDensity.current
    val current by rememberUpdatedState(mark)
    val fontPx = mark.size * wPx
    val pad = if (mark.background != null) fontPx * 0.45f else 0f
    Box(
        Modifier
            .offset { IntOffset((mark.x * wPx - pad).roundToInt(), (mark.y * hPx - pad).roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { onSelect(current) }) { change, drag ->
                    change.consume()
                    val m = current
                    onReplace(m, m.copy(x = (m.x + drag.x / wPx).coerceIn(0f, 0.98f), y = (m.y + drag.y / hPx).coerceIn(0f, 0.98f)))
                }
            }
            .pointerInput(Unit) { detectTapGestures { onSelect(current) } },
    ) {
        SelectionFrame(selected, onRemove = { onRemove(current) }) {
            Text(
                mark.text,
                color = Color(mark.color),
                fontSize = with(density) { fontPx.toSp() },
                lineHeight = with(density) { (fontPx * 1.25f).toSp() },
                modifier = Modifier
                    .background(mark.background?.let { Color(it) } ?: Color.Transparent)
                    .padding(with(density) { pad.toDp() }),
            )
        }
    }
}

@Composable
private fun PlacedImage(
    mark: ImageMark, wPx: Float, hPx: Float, selected: Boolean,
    onSelect: (PageMark?) -> Unit, onReplace: (PageMark, PageMark) -> Unit, onRemove: (PageMark) -> Unit,
) {
    val density = LocalDensity.current
    val current by rememberUpdatedState(mark)
    val bitmap = remember(mark.path) { BitmapFactory.decodeFile(mark.path)?.asImageBitmap() }
    Box(
        Modifier
            .offset { IntOffset((mark.x * wPx).roundToInt(), (mark.y * hPx).roundToInt()) }
            .size(with(density) { (mark.w * wPx).toDp() }, with(density) { (mark.h * hPx).toDp() })
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { onSelect(current) }) { change, drag ->
                    change.consume()
                    val m = current
                    onReplace(m, m.copy(x = (m.x + drag.x / wPx).coerceIn(-m.w * 0.5f, 1f - m.w * 0.5f), y = (m.y + drag.y / hPx).coerceIn(0f, 1f - m.h)))
                }
            }
            .pointerInput(Unit) { detectTapGestures { onSelect(current) } },
    ) {
        SelectionFrame(selected, onRemove = { onRemove(current) }) {
            if (bitmap != null) Image(bitmap, "Signature", Modifier.fillMaxSize())
        }
        if (selected) {
            // Resize handle
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(10.dp, 10.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Brand.Red)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            val m = current
                            val ratio = m.h / m.w
                            val nw = (m.w + drag.x / wPx).coerceIn(0.08f, 0.95f)
                            onReplace(m, m.copy(w = nw, h = nw * ratio))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.OpenWith, "Resize", tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}
