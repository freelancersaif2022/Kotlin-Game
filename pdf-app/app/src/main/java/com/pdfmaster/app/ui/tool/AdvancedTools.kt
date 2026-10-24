package com.pdfmaster.app.ui.tool

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfmaster.app.pdf.MarginOption
import com.pdfmaster.app.pdf.OrientationOption
import com.pdfmaster.app.pdf.PageEdit
import com.pdfmaster.app.pdf.PageSizeOption
import com.pdfmaster.app.pdf.PdfToolException
import com.pdfmaster.app.premium.PremiumManager
import com.pdfmaster.app.ui.components.ChoiceChip
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.SecondaryButton
import com.pdfmaster.app.ui.components.SegmentedControl
import com.pdfmaster.app.ui.premium.rememberPremiumGate
import com.pdfmaster.app.ui.premium.rememberPremiumState
import com.pdfmaster.app.ui.result.ToolResult
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.tools.PdfTool
import com.pdfmaster.app.ui.viewer.PdfPage
import com.pdfmaster.app.util.Format
import com.pdfmaster.app.data.repo.OutputKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------- Organize

private data class OrgPage(val source: Int, val rotation: Int, val id: Long)

@Composable
fun OrganizeTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    val picked = pdf.picked
    val session = rememberPreviewSession(vm, picked?.uri)
    val pages = remember(picked?.uri) { mutableStateListOf<OrgPage>() }
    val selected = remember(picked?.uri) { mutableStateListOf<Long>() }
    LaunchedEffect(session) {
        if (session != null && pages.isEmpty()) pages.addAll((0 until session.pageCount).map { OrgPage(it, 0, it.toLong()) })
    }
    val changed = pages.size != (session?.pageCount ?: 0) || pages.withIndex().any { (i, p) -> p.source != i || p.rotation != 0 }

    fun rotate(delta: Int) {
        pages.indices.forEach { i -> if (pages[i].id in selected) pages[i] = pages[i].copy(rotation = (pages[i].rotation + delta + 360) % 360) }
    }
    fun move(dir: Int) {
        val idx = pages.indices.filter { pages[it].id in selected }
        val ordered = if (dir < 0) idx else idx.reversed()
        ordered.forEach { i ->
            val j = i + dir
            if (j in pages.indices && pages[j].id !in selected) {
                val tmp = pages[j]; pages[j] = pages[i]; pages[i] = tmp
            }
        }
    }

    ToolScaffold(
        PdfTool.ORGANIZE, "Save ${pages.size} pages", picked != null && pages.isNotEmpty() && changed,
        onAction = {
            val src = picked ?: return@ToolScaffold
            val edits = pages.map { PageEdit(it.source, it.rotation) }
            vm.run(PdfTool.ORGANIZE, "Saving page changes") {
                val input = copy(src.uri)
                val out = work()
                tools.organize(input, edits, out) { progress(it) }
                input.delete()
                val output = publishPdf(out, storage.uniqueName(Format.baseName(src.name), "pdf", "organized"))
                ToolResult(PdfTool.ORGANIZE, "Pages organized", "Your PDF now has ${Format.pages(edits.size)} in the new order.", listOf(output))
            }
        },
    ) {
        if (picked == null) {
            PickPrompt("Select a PDF", "Reorder, rotate or delete its pages", onClick = { pdf.pick() })
        } else {
            PickedPdfCard(picked, onChange = { pdf.pick() })
            if (picked.pages < 0) {
                InfoNote("This PDF is password protected. Unlock it first.")
            } else if (session != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(16.dp)).padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val any = selected.isNotEmpty()
                    OrgAction(Icons.Rounded.SelectAll, if (selected.size == pages.size) "None" else "All", true) {
                        if (selected.size == pages.size) selected.clear() else { selected.clear(); selected.addAll(pages.map { it.id }) }
                    }
                    OrgAction(Icons.Rounded.RotateLeft, "Left", any) { rotate(-90) }
                    OrgAction(Icons.Rounded.RotateRight, "Right", any) { rotate(90) }
                    OrgAction(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Earlier", any) { move(-1) }
                    OrgAction(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Later", any) { move(1) }
                    OrgAction(Icons.Rounded.Delete, "Delete", any && selected.size < pages.size) {
                        pages.removeAll { it.id in selected }
                        selected.clear()
                    }
                }
                Text(
                    if (selected.isEmpty()) "Tap pages to select them" else "${selected.size} selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                pages.chunked(3).forEachIndexed { row, chunk ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        chunk.forEachIndexed { col, p ->
                            val isSel = p.id in selected
                            val rot by animateFloatAsState(p.rotation.toFloat(), label = "rot")
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .border(if (isSel) 2.5.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else AppTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                                        .clickable { if (isSel) selected.remove(p.id) else selected.add(p.id) }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(Modifier.fillMaxWidth(0.82f).rotate(rot)) { PdfPage(session, p.source, 240) }
                                    if (isSel) {
                                        Box(
                                            Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center,
                                        ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("${row * 3 + col + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrgAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.35f
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp).graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- Image to PDF

@Composable
fun ImageToPdfTool(vm: ToolViewModel) {
    val images = remember { mutableStateListOf<Uri>() }
    val premium = rememberPremiumState()
    val gate = rememberPremiumGate()
    val snackbar = LocalSnackbar.current
    var size by rememberSaveable { mutableStateOf(PageSizeOption.A4) }
    var orientation by rememberSaveable { mutableStateOf(OrientationOption.AUTO) }
    var margin by rememberSaveable { mutableStateOf(MarginOption.SMALL) }
    var name by rememberSaveable { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        val fresh = uris.filter { it !in images }
        val limit = PremiumManager.FREE_IMAGE_LIMIT
        if (!premium.isPremium && images.size + fresh.size > limit) {
            images.addAll(fresh.take((limit - images.size).coerceAtLeast(0)))
            gate.require("More than $limit images") { images.addAll(fresh.drop((limit - images.size).coerceAtLeast(0)).filter { it !in images }) }
            snackbar.info("Free plan converts up to $limit images at a time.")
        } else {
            images.addAll(fresh)
        }
    }
    val pick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    ToolScaffold(
        PdfTool.IMAGE_TO_PDF, if (images.isEmpty()) "Convert to PDF" else "Convert ${images.size} image(s)", images.isNotEmpty(),
        onAction = {
            val list = images.toList()
            vm.run(PdfTool.IMAGE_TO_PDF, "Creating your PDF") {
                val out = work()
                tools.imagesToPdf(list, size, orientation, margin, out) { progress(it) }
                val output = publishPdf(out, storage.uniqueName(name.ifBlank { "Images" }, "pdf"))
                ToolResult(PdfTool.IMAGE_TO_PDF, "PDF created", "${list.size} image(s) converted into a ${Format.pages(output.pageCount)} PDF.", listOf(output))
            }
        },
    ) {
        if (images.isEmpty()) {
            PickPrompt("Select images", "Photos, screenshots or scans", onClick = pick, icon = Icons.Rounded.AddPhotoAlternate)
        } else {
            images.chunked(3).forEachIndexed { row, chunk ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunk.forEachIndexed { col, uri ->
                        val index = row * 3 + col
                        ImageThumb(vm, uri, index + 1, Modifier.weight(1f),
                            onRemove = { images.remove(uri) },
                            onLeft = if (index > 0) ({ images.add(index - 1, images.removeAt(index)) }) else null,
                        )
                    }
                    repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            SecondaryButton("Add more images", pick, icon = Icons.Rounded.AddPhotoAlternate)
            OptionLabel("File name")
            OutlinedTextField(
                value = name, onValueChange = { name = it }, placeholder = { Text("Images") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            )
        }
        OptionLabel("Page size")
        SegmentedControl(listOf("A4", "Letter", "Fit image"), size.ordinal, { size = PageSizeOption.entries[it] })
        if (size != PageSizeOption.FIT) {
            OptionLabel("Orientation")
            SegmentedControl(listOf("Auto", "Portrait", "Landscape"), orientation.ordinal, { orientation = OrientationOption.entries[it] })
        }
        OptionLabel("Margins")
        SegmentedControl(listOf("None", "Small", "Large"), margin.ordinal, { margin = MarginOption.entries[it] })
        if (!premium.isPremium) InfoNote("Free plan: up to ${PremiumManager.FREE_IMAGE_LIMIT} images per PDF. Premium removes the limit.")
    }
}

@Composable
private fun ImageThumb(vm: ToolViewModel, uri: Uri, number: Int, modifier: Modifier, onRemove: () -> Unit, onLeft: (() -> Unit)?) {
    val bitmap by produceState<Bitmap?>(null, uri) { value = withContext(Dispatchers.IO) { runCatching { vm.tools.decodeImage(uri, 320) }.getOrNull() } }
    Box(modifier.aspectRatio(0.8f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.Center))
        Box(Modifier.padding(6.dp).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text("$number", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onLeft != null) SmallRound(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, onLeft)
            SmallRound(Icons.Rounded.Close, onRemove)
        }
    }
}

@Composable
private fun SmallRound(icon: ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0xB31B1D29)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

// ---------------------------------------------------------------- OCR

@Composable
fun OcrTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    val images = remember { mutableStateListOf<Uri>() }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) { images.clear(); images.addAll(uris) }
    }
    val picked = pdf.picked
    LaunchedEffect(picked) { if (picked != null) images.clear() }
    val ready = (picked != null && picked.pages > 0 && images.isEmpty()) || images.isNotEmpty()
    ToolScaffold(
        PdfTool.OCR, "Recognise text", ready,
        onAction = {
            val src = picked
            val imgs = images.toList()
            vm.run(PdfTool.OCR, "Recognising text") {
                val sb = StringBuilder()
                if (imgs.isNotEmpty()) {
                    imgs.forEachIndexed { i, uri ->
                        val bmp = tools.decodeImage(uri, 2200) ?: throw PdfToolException("One of the images couldn't be opened.")
                        val text = ocr.recognize(bmp)
                        bmp.recycle()
                        if (text.isNotBlank()) sb.append("— Image ${i + 1} —\n\n").append(text.trim()).append("\n\n")
                        progress((i + 1f) / imgs.size)
                    }
                } else if (src != null) {
                    val input = copy(src.uri)
                    tools.renderPages(input, 2f) { i, total, bmp ->
                        val text = ocr.recognize(bmp)
                        if (text.isNotBlank()) sb.append("— Page ${i + 1} —\n\n").append(text.trim()).append("\n\n")
                        progress((i + 1f) / total)
                    }
                    input.delete()
                }
                val text = sb.toString().trim()
                if (text.isEmpty()) throw PdfToolException("We couldn't find any readable text. Try a sharper scan or better lighting.")
                ToolResult(PdfTool.OCR, "Text recognised", "${text.length} characters recognised.", kind = OutputKind.TEXT, text = text)
            }
        },
    ) {
        if (images.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(20.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("${images.size} image(s) selected", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { images.clear() }) { Icon(Icons.Rounded.Close, "Clear") }
            }
        } else if (picked == null) {
            PickPrompt("Select a scanned PDF", "We'll read the text from every page", onClick = { pdf.pick() }, icon = Icons.Rounded.PictureAsPdf)
        } else {
            PickedPdfCard(picked, onChange = { pdf.pick() })
        }
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Use images instead", { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, icon = Icons.Rounded.AddPhotoAlternate)
        InfoNote("Recognition runs on your device. Best results with clear, well-lit Latin-script text.")
    }
}

// ---------------------------------------------------------------- Watermark

private val watermarkColors = listOf(Color(0xFFE5252A), Color(0xFF1B1D29), Color(0xFF2563EB), Color(0xFF8A8FA3))

@Composable
fun WatermarkTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    val picked = pdf.picked
    val session = rememberPreviewSession(vm, picked?.uri)
    var text by rememberSaveable { mutableStateOf("CONFIDENTIAL") }
    var color by remember { mutableStateOf(watermarkColors.first()) }
    var opacity by remember { mutableFloatStateOf(0.25f) }
    var sizeRatio by remember { mutableFloatStateOf(0.11f) }
    var diagonal by rememberSaveable { mutableStateOf(true) }
    ToolScaffold(
        PdfTool.WATERMARK, "Add watermark", picked != null && picked.pages > 0 && text.isNotBlank(),
        onAction = {
            val src = picked ?: return@ToolScaffold
            val mark = text
            val argb = color.toArgb()
            val alpha = opacity
            val ratio = sizeRatio
            val slanted = diagonal
            vm.run(PdfTool.WATERMARK, "Adding watermark") {
                val input = copy(src.uri)
                val out = work()
                tools.watermark(input, mark, argb, alpha, ratio, slanted, out) { progress(it) }
                input.delete()
                val output = publishPdf(out, storage.uniqueName(Format.baseName(src.name), "pdf", "watermarked"))
                ToolResult(PdfTool.WATERMARK, "Watermark added", "\"$mark\" stamped on all ${Format.pages(output.pageCount)}.", listOf(output))
            }
        },
    ) {
        if (picked == null) PickPrompt("Select a PDF", "Choose the document to watermark", onClick = { pdf.pick() })
        else PickedPdfCard(picked, onChange = { pdf.pick() })

        if (session != null) {
            OptionLabel("Preview")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.width(220.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(6.dp))) {
                    PdfPage(session, 0, 600)
                    val pageSize = session.pageSizes[0]
                    val angle = if (diagonal) Math.toDegrees(kotlin.math.atan2(pageSize.height.toDouble(), pageSize.width.toDouble())).toFloat() else 0f
                    Text(
                        text,
                        color = color.copy(alpha = opacity),
                        fontWeight = FontWeight.Bold,
                        fontSize = (220 * kotlin.math.min(1f, pageSize.height.toFloat() / pageSize.width) * sizeRatio).sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.align(Alignment.Center).rotate(-angle),
                    )
                }
            }
        }
        OptionLabel("Watermark text")
        OutlinedTextField(
            value = text, onValueChange = { text = it.take(40) }, singleLine = true,
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
        )
        OptionLabel("Colour")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            watermarkColors.forEach { c ->
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(c)
                        .border(if (c == color) 3.dp else 0.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape).clickable { color = c },
                    contentAlignment = Alignment.Center,
                ) { if (c == color) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
        }
        OptionLabel("Opacity · ${(opacity * 100).toInt()}%")
        Slider(opacity, { opacity = it }, valueRange = 0.08f..0.8f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
        OptionLabel("Size")
        Slider(sizeRatio, { sizeRatio = it }, valueRange = 0.05f..0.2f, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Diagonal", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Switch(diagonal, { diagonal = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
        }
    }
}
