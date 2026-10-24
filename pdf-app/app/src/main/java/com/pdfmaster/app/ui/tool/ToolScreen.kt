package com.pdfmaster.app.ui.tool

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.pdfmaster.app.R
import com.pdfmaster.app.data.repo.OutputKind
import com.pdfmaster.app.pdf.CompressLevel
import com.pdfmaster.app.pdf.PdfToolException
import com.pdfmaster.app.ui.components.ChoiceChip
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.PdfThumbnail
import com.pdfmaster.app.ui.components.PrimaryButton
import com.pdfmaster.app.ui.components.ProBadge
import com.pdfmaster.app.ui.components.SecondaryButton
import com.pdfmaster.app.ui.components.SegmentedControl
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.navigation.Routes
import com.pdfmaster.app.ui.premium.rememberPremiumGate
import com.pdfmaster.app.ui.premium.rememberPremiumState
import com.pdfmaster.app.ui.premium.rememberRewardLauncher
import com.pdfmaster.app.ui.rememberActivity
import com.pdfmaster.app.ui.result.ToolResult
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand
import com.pdfmaster.app.ui.tools.PdfTool
import com.pdfmaster.app.util.Format
import com.pdfmaster.app.util.PageRanges
import kotlinx.coroutines.launch

@Composable
fun ToolScreen(tool: PdfTool, initialUri: Uri?) {
    val vm: ToolViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val premium = rememberPremiumState()
    if (tool.isPro && !premium.isPremium) {
        LockedTool(tool)
    } else {
        when (tool) {
            PdfTool.SIGN, PdfTool.ANNOTATE -> EditorEntry(tool, vm, initialUri)
            PdfTool.SCAN -> ScanTool(vm)
            PdfTool.MERGE -> MergeTool(vm, initialUri)
            PdfTool.SPLIT -> SplitTool(vm, initialUri)
            PdfTool.ORGANIZE -> OrganizeTool(vm, initialUri)
            PdfTool.COMPRESS -> CompressTool(vm, initialUri)
            PdfTool.IMAGE_TO_PDF -> ImageToPdfTool(vm)
            PdfTool.TEXT_TO_PDF -> TextToPdfTool(vm)
            PdfTool.PDF_TO_IMAGES -> PdfToImagesTool(vm, initialUri)
            PdfTool.EXTRACT_TEXT -> ExtractTextTool(vm, initialUri)
            PdfTool.OCR -> OcrTool(vm, initialUri)
            PdfTool.WATERMARK -> WatermarkTool(vm, initialUri)
            PdfTool.PAGE_NUMBERS -> PageNumbersTool(vm, initialUri)
            PdfTool.LOCK -> LockTool(vm, initialUri)
            PdfTool.UNLOCK -> UnlockTool(vm, initialUri)
        }
    }
    RunStateHost(state, Routes.TOOL, vm::reset)
}

// ---------------------------------------------------------------- Locked

@Composable
private fun LockedTool(tool: PdfTool) {
    val nav = LocalNavigator.current
    val reward = rememberRewardLauncher()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.img_reward_unlock), null, Modifier.size(190.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tool.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(8.dp))
                ProBadge()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${tool.description}. Unlock it free for 5 hours by watching a short video, or upgrade for unlimited access.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PrimaryButton("Watch Ad · Unlock for 5 Hours", { reward.launch() }, icon = Icons.Rounded.PlayCircle, brush = Brand.GoldGradient)
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Upgrade to Premium", { nav.premium("tool_${tool.name.lowercase()}") }, icon = Icons.Rounded.WorkspacePremium)
        }
    }
}

// ---------------------------------------------------------------- Sign / Annotate entry

@Composable
private fun EditorEntry(tool: PdfTool, vm: ToolViewModel, initialUri: Uri?) {
    val nav = LocalNavigator.current
    val pdf = rememberSinglePdf(vm, null)
    val mode = if (tool == PdfTool.SIGN) "sign" else "annotate"
    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            nav.back()
            nav.editor(initialUri, mode)
        }
    }
    LaunchedEffect(pdf.picked) {
        pdf.picked?.let {
            nav.back()
            nav.editor(it.uri, mode)
        }
    }
    if (initialUri == null) {
        ToolScaffold(tool, "Select PDF", true, onAction = { pdf.pick() }) {
            PickPrompt("Select a PDF", if (tool == PdfTool.SIGN) "Choose the document you need to sign" else "Choose the document to edit", onClick = { pdf.pick() })
            InfoNote(if (tool == PdfTool.SIGN) "Draw your signature once and reuse it on any document." else "Highlight, draw, add text boxes and sticky notes, then save a new copy.")
        }
    }
}

// ---------------------------------------------------------------- Scan

@Composable
private fun ScanTool(vm: ToolViewModel) {
    val activity = rememberActivity()
    val snackbar = LocalSnackbar.current
    val premium = rememberPremiumState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pdf = scan?.pdf
            if (pdf != null) {
                vm.run(PdfTool.SCAN, "Saving your scan") {
                    progress(0.3f)
                    val file = copy(pdf.uri)
                    val out = publishPdf(file, storage.uniqueName("Scan", "pdf"))
                    ToolResult(PdfTool.SCAN, "Scan saved", "${Format.pages(pdf.pageCount)} scanned and saved as a PDF.", listOf(out))
                }
            }
        }
    }
    val start = {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(if (premium.isPremium) 100 else 30)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
            .addOnSuccessListener { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
            .addOnFailureListener {
                snackbar.error("The scanner couldn't start. Please update Google Play services and try again.", title = "Scanner unavailable")
            }
        Unit
    }
    var autoStarted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoStarted) {
            autoStarted = true
            start()
        }
    }
    ToolScaffold(PdfTool.SCAN, "Start scanning", true, onAction = start) {
        Image(painterResource(R.drawable.img_permission_camera), null, Modifier.align(Alignment.CenterHorizontally).size(180.dp))
        listOf(
            "Automatic edge detection and perspective fix",
            "Clean-up filters for crisp, readable pages",
            "Import photos from your gallery",
            "Saved straight to PDF on your device",
        ).forEach { line ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = AppTheme.colors.success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ---------------------------------------------------------------- Merge

@Composable
private fun MergeTool(vm: ToolViewModel, initialUri: Uri?) {
    val files = remember { mutableStateListOf<PickedPdf>() }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        scope.launch { uris.filter { u -> files.none { it.uri == u } }.forEach { files += vm.load(it) } }
    }
    LaunchedEffect(initialUri) { if (initialUri != null && files.isEmpty()) files += vm.load(initialUri) }
    val totalPages = files.sumOf { it.pages.coerceAtLeast(0) }
    ToolScaffold(
        PdfTool.MERGE,
        actionText = if (files.size < 2) "Select at least 2 files" else "Merge ${files.size} files",
        actionEnabled = files.size >= 2,
        onAction = {
            val list = files.toList()
            vm.run(PdfTool.MERGE, "Merging ${list.size} files") {
                val copies = list.map { copy(it.uri) }
                progress(0.15f)
                val out = work()
                tools.merge(copies, out) { progress(0.15f + it * 0.75f) }
                copies.forEach { it.delete() }
                val output = publishPdf(out, storage.uniqueName("Merged", "pdf"))
                ToolResult(PdfTool.MERGE, "PDFs merged", "${list.size} files combined into one ${Format.pages(output.pageCount)} document.", listOf(output))
            }
        },
    ) {
        if (files.isEmpty()) {
            PickPrompt("Select PDF files", "Choose two or more files to combine", onClick = { launcher.launch(arrayOf("application/pdf")) })
        } else {
            Text("${files.size} files · $totalPages pages", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 10.dp))
            files.forEachIndexed { i, f ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(18.dp)).padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    PdfThumbnail(f.uri, width = 40.dp, height = 50.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(Format.size(f.size), Format.pages(f.pages)).joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { if (i > 0) files.add(i - 1, files.removeAt(i)) }, enabled = i > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                    IconButton(onClick = { if (i < files.lastIndex) files.add(i + 1, files.removeAt(i)) }, enabled = i < files.lastIndex) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = { files.removeAt(i) }) { Icon(Icons.Rounded.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Add more files", { launcher.launch(arrayOf("application/pdf")) }, icon = Icons.Rounded.Add)
            InfoNote("Files are merged in the order shown. Use the arrows to rearrange them.")
        }
    }
}

// ---------------------------------------------------------------- Split

@Composable
private fun SplitTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var ranges by rememberSaveable { mutableStateOf("") }
    var every by rememberSaveable { mutableIntStateOf(1) }
    val picked = pdf.picked
    val pages = picked?.pages ?: 0
    val groups: List<List<Int>>? = when {
        picked == null || pages <= 0 -> null
        mode == 0 -> PageRanges.parse(ranges, pages)?.let { listOf(it.flatten().distinct()) }
        mode == 1 -> PageRanges.parse(ranges, pages)
        else -> (0 until pages).chunked(every.coerceAtLeast(1))
    }
    ToolScaffold(
        PdfTool.SPLIT,
        actionText = when {
            groups == null -> "Split PDF"
            groups.size == 1 -> "Extract ${Format.pages(groups[0].size)}"
            else -> "Create ${groups.size} files"
        },
        actionEnabled = groups != null && groups.isNotEmpty(),
        onAction = {
            val g = groups ?: return@ToolScaffold
            val src = picked ?: return@ToolScaffold
            vm.run(PdfTool.SPLIT, "Splitting your PDF") {
                val input = copy(src.uri)
                val base = Format.baseName(src.name)
                val parts = tools.split(input, g, storage.workDir(), "split") { progress(it * 0.8f) }
                input.delete()
                val outputs = parts.mapIndexed { i, f ->
                    progress(0.8f + 0.2f * (i + 1) / parts.size)
                    publishPdf(f, storage.uniqueName(base, "pdf", if (parts.size == 1) "extract" else "part${i + 1}"))
                }
                ToolResult(PdfTool.SPLIT, if (outputs.size == 1) "Pages extracted" else "PDF split", "${outputs.size} file(s) created from ${src.name}.", outputs)
            }
        },
    ) {
        if (picked == null) PickPrompt("Select a PDF", "Choose the file you want to split", onClick = { pdf.pick() })
        else PickedPdfCard(picked, onChange = { pdf.pick() })
        if (picked != null && pages < 0) {
            InfoNote("This PDF is password protected. Unlock it first, then split it.")
        }
        if (picked != null && pages > 0) {
            OptionLabel("Split mode")
            SegmentedControl(listOf("Extract", "By ranges", "Every N"), mode, { mode = it })
            when (mode) {
                0, 1 -> {
                    OptionLabel(if (mode == 0) "Pages to extract" else "Ranges (one file each)")
                    OutlinedTextField(
                        value = ranges,
                        onValueChange = { ranges = it },
                        placeholder = { Text(if (mode == 0) "e.g. 1-3, 5, 8" else "e.g. 1-4, 5-8, 9-$pages") },
                        singleLine = true,
                        isError = ranges.isNotBlank() && groups == null,
                        supportingText = { Text(if (ranges.isNotBlank() && groups == null) "Use page numbers between 1 and $pages" else "This document has $pages pages") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    OptionLabel("Pages per file")
                    Stepper(every, 1, pages.coerceAtLeast(1)) { every = it }
                    InfoNote("Creates ${(pages + every - 1) / every} files with up to $every page(s) each.")
                }
            }
        }
    }
}

@Composable
fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min) { Icon(Icons.Rounded.Remove, "Decrease") }
        Text("$value", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max) { Icon(Icons.Rounded.Add, "Increase") }
    }
}

// ---------------------------------------------------------------- Compress

@Composable
private fun CompressTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    val premium = rememberPremiumState()
    val gate = rememberPremiumGate()
    var level by rememberSaveable { mutableStateOf(CompressLevel.RECOMMENDED) }
    val picked = pdf.picked
    ToolScaffold(
        PdfTool.COMPRESS, "Compress PDF", picked != null && picked.pages >= 0,
        onAction = {
            val src = picked ?: return@ToolScaffold
            vm.run(PdfTool.COMPRESS, "Compressing your PDF") {
                val input = copy(src.uri)
                val out = work()
                val r = tools.compress(input, level, out) { progress(it * 0.95f) }
                input.delete()
                val output = publishPdf(r.file, storage.uniqueName(Format.baseName(src.name), "pdf", "compressed"))
                val saved = r.before - r.after
                val message = if (saved > 0) "Reduced by ${(saved * 100 / r.before.coerceAtLeast(1))}% — saved ${Format.size(saved)}."
                else "This file is already well optimised, so we kept the original quality."
                ToolResult(PdfTool.COMPRESS, "PDF compressed", message, listOf(output), before = r.before, after = r.after)
            }
        },
    ) {
        if (picked == null) PickPrompt("Select a PDF", "Choose the file to make smaller", onClick = { pdf.pick() })
        else PickedPdfCard(picked, onChange = { pdf.pick() })
        OptionLabel("Compression level")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OptionCard("Low", "Best quality · light size reduction", level == CompressLevel.LOW, { level = CompressLevel.LOW })
            OptionCard("Recommended", "Great quality · good reduction", level == CompressLevel.RECOMMENDED, { level = CompressLevel.RECOMMENDED })
            OptionCard(
                "Extreme", "Smallest file · lower image quality", level == CompressLevel.EXTREME,
                { gate.require("Extreme compression") { level = CompressLevel.EXTREME } },
                trailing = { if (!premium.isPremium) ProBadge() },
            )
        }
        InfoNote("Compression shrinks the images inside your PDF. Text stays sharp and searchable.")
    }
}

// ---------------------------------------------------------------- Text to PDF

@Composable
private fun TextToPdfTool(vm: ToolViewModel) {
    var title by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var size by rememberSaveable { mutableIntStateOf(1) }
    val sizes = listOf(10f, 12f, 15f)
    ToolScaffold(
        PdfTool.TEXT_TO_PDF, "Create PDF", text.isNotBlank(),
        onAction = {
            vm.run(PdfTool.TEXT_TO_PDF, "Creating your PDF") {
                val out = work()
                tools.textToPdf(text, sizes[size], out) { progress(it) }
                val output = publishPdf(out, storage.uniqueName(title.ifBlank { "Document" }, "pdf"))
                ToolResult(PdfTool.TEXT_TO_PDF, "PDF created", "Your text was saved as a ${Format.pages(output.pageCount)} PDF.", listOf(output))
            }
        },
    ) {
        OutlinedTextField(
            value = title, onValueChange = { title = it }, label = { Text("File name") }, singleLine = true,
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text, onValueChange = { text = it }, label = { Text("Your text") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary),
            supportingText = { Text("${text.length} characters") },
        )
        OptionLabel("Font size")
        SegmentedControl(listOf("Small", "Medium", "Large"), size, { size = it })
    }
}

// ---------------------------------------------------------------- PDF to images

@Composable
private fun PdfToImagesTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    var png by rememberSaveable { mutableStateOf(false) }
    var high by rememberSaveable { mutableStateOf(false) }
    val picked = pdf.picked
    ToolScaffold(
        PdfTool.PDF_TO_IMAGES, if (picked != null && picked.pages > 0) "Export ${Format.pages(picked.pages)}" else "Export images",
        picked != null && picked.pages > 0,
        onAction = {
            val src = picked ?: return@ToolScaffold
            vm.run(PdfTool.PDF_TO_IMAGES, "Exporting pages") {
                val input = copy(src.uri)
                val base = Format.baseName(src.name)
                val outputs = ArrayList<com.pdfmaster.app.data.model.OutputFile>()
                tools.renderPages(input, if (high) 3f else 2f) { i, total, bmp ->
                    val ext = if (png) "png" else "jpg"
                    val f = work(ext)
                    f.outputStream().use { bmp.compress(if (png) android.graphics.Bitmap.CompressFormat.PNG else android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
                    outputs += storage.publish(f, storage.uniqueName(base, ext, "page${i + 1}"), if (png) "image/png" else "image/jpeg", OutputKind.IMAGE)
                    progress((i + 1f) / total)
                }
                input.delete()
                ToolResult(PdfTool.PDF_TO_IMAGES, "Images exported", "${outputs.size} image(s) saved to ${storage.locationLabel(OutputKind.IMAGE)}.", outputs, kind = OutputKind.IMAGE)
            }
        },
    ) {
        if (picked == null) PickPrompt("Select a PDF", "Each page becomes an image", onClick = { pdf.pick() })
        else PickedPdfCard(picked, onChange = { pdf.pick() })
        OptionLabel("Format")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceChip("JPG", !png, { png = false })
            ChoiceChip("PNG", png, { png = true })
        }
        OptionLabel("Quality")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceChip("Standard (144 dpi)", !high, { high = false })
            ChoiceChip("High (216 dpi)", high, { high = true })
        }
    }
}

// ---------------------------------------------------------------- Extract text

@Composable
private fun ExtractTextTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    val picked = pdf.picked
    ToolScaffold(
        PdfTool.EXTRACT_TEXT, "Extract text", picked != null && picked.pages > 0,
        onAction = {
            val src = picked ?: return@ToolScaffold
            vm.run(PdfTool.EXTRACT_TEXT, "Reading text") {
                val input = copy(src.uri)
                val text = tools.extractText(input) { progress(it) }
                input.delete()
                if (text.isBlank()) throw PdfToolException("No selectable text was found. This looks like a scanned document — try OCR Text Recognition instead.")
                ToolResult(PdfTool.EXTRACT_TEXT, "Text extracted", "${text.length} characters from ${src.name}.", kind = OutputKind.TEXT, text = text)
            }
        },
    ) {
        if (picked == null) PickPrompt("Select a PDF", "We'll pull out all the selectable text", onClick = { pdf.pick() })
        else PickedPdfCard(picked, onChange = { pdf.pick() })
        InfoNote("Works with digital PDFs. For scans and photos, use OCR Text Recognition.")
    }
}

// ---------------------------------------------------------------- Page numbers

@Composable
private fun PageNumbersTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    var position by rememberSaveable { mutableStateOf(com.pdfmaster.app.pdf.NumberPosition.BOTTOM_CENTER) }
    var format by rememberSaveable { mutableStateOf(com.pdfmaster.app.pdf.NumberFormat.PAGE_N_OF_TOTAL) }
    var start by rememberSaveable { mutableIntStateOf(1) }
    var sizeIndex by rememberSaveable { mutableIntStateOf(1) }
    val sizes = listOf(9f, 11f, 14f)
    val picked = pdf.picked
    ToolScaffold(
        PdfTool.PAGE_NUMBERS, "Add page numbers", picked != null && picked.pages > 0,
        onAction = {
            val src = picked ?: return@ToolScaffold
            vm.run(PdfTool.PAGE_NUMBERS, "Numbering pages") {
                val input = copy(src.uri)
                val out = work()
                tools.addPageNumbers(input, position, format, start, sizes[sizeIndex], out) { progress(it) }
                input.delete()
                val output = publishPdf(out, storage.uniqueName(Format.baseName(src.name), "pdf", "numbered"))
                ToolResult(PdfTool.PAGE_NUMBERS, "Page numbers added", "All ${Format.pages(output.pageCount)} are now numbered.", listOf(output))
            }
        },
    ) {
        if (picked == null) PickPrompt("Select a PDF", "Choose the document to number", onClick = { pdf.pick() })
        else PickedPdfCard(picked, onChange = { pdf.pick() })
        OptionLabel("Position")
        Box(
            Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)
                .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(18.dp)).padding(10.dp),
        ) {
            com.pdfmaster.app.pdf.NumberPosition.entries.forEach { p ->
                val align = when (p) {
                    com.pdfmaster.app.pdf.NumberPosition.TOP_LEFT -> Alignment.TopStart
                    com.pdfmaster.app.pdf.NumberPosition.TOP_CENTER -> Alignment.TopCenter
                    com.pdfmaster.app.pdf.NumberPosition.TOP_RIGHT -> Alignment.TopEnd
                    com.pdfmaster.app.pdf.NumberPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    com.pdfmaster.app.pdf.NumberPosition.BOTTOM_CENTER -> Alignment.BottomCenter
                    com.pdfmaster.app.pdf.NumberPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                }
                val active = p == position
                Box(
                    Modifier.align(align).size(width = 56.dp, height = 32.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else Color(0xFFF1F2F6))
                        .clickable { position = p },
                    contentAlignment = Alignment.Center,
                ) { Text("1", color = if (active) Color.White else Color(0xFF8A8FA3), fontWeight = FontWeight.Bold) }
            }
            Column(Modifier.align(Alignment.Center).fillMaxWidth(0.5f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { Box(Modifier.fillMaxWidth(if (it == 3) 0.6f else 1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE6E8EF))) }
            }
        }
        OptionLabel("Style")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            com.pdfmaster.app.pdf.NumberFormat.entries.take(2).forEach { f -> ChoiceChip(f.label, f == format, { format = f }, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            com.pdfmaster.app.pdf.NumberFormat.entries.drop(2).forEach { f -> ChoiceChip(f.label, f == format, { format = f }, Modifier.weight(1f)) }
        }
        OptionLabel("Start numbering at")
        Stepper(start, 1, 9999) { start = it }
        OptionLabel("Text size")
        SegmentedControl(listOf("Small", "Medium", "Large"), sizeIndex, { sizeIndex = it })
    }
}

// ---------------------------------------------------------------- Lock / Unlock

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, isError: Boolean = false, support: String? = null) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, isError = isError,
        supportingText = support?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Toggle") } },
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun LockTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val picked = pdf.picked
    val strength = when {
        password.length < 6 -> 0
        password.length >= 10 && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 3
        password.length >= 8 && password.any { it.isDigit() } -> 2
        else -> 1
    }
    val valid = picked != null && password.length >= 6 && password == confirm
    ToolScaffold(
        PdfTool.LOCK, "Protect PDF", valid,
        onAction = {
            val src = picked ?: return@ToolScaffold
            val pwd = password
            vm.run(PdfTool.LOCK, "Encrypting your PDF") {
                val input = copy(src.uri)
                val out = work()
                tools.lock(input, pwd, out) { progress(it) }
                input.delete()
                val output = storage.publish(out, storage.uniqueName(Format.baseName(src.name), "pdf", "protected"), "application/pdf", OutputKind.DOCUMENT, src.pages)
                ToolResult(PdfTool.LOCK, "PDF protected", "Encrypted with AES-256. Keep your password safe — it can't be recovered.", listOf(output))
            }
        },
    ) {
        if (picked == null) PickPrompt("Select a PDF", "Choose the document to protect", onClick = { pdf.pick() })
        else PickedPdfCard(picked, onChange = { pdf.pick() })
        OptionLabel("Set a password")
        PasswordField(password, { password = it }, "Password", support = "At least 6 characters")
        if (password.isNotEmpty()) {
            val (label, color) = when (strength) {
                0 -> "Too short" to MaterialTheme.colorScheme.error
                1 -> "Weak" to AppTheme.colors.warning
                2 -> "Good" to AppTheme.colors.info
                else -> "Strong" to AppTheme.colors.success
            }
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (strength + 1) / 4f }, color = color, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)), drawStopIndicator = {},
                )
                Spacer(Modifier.width(10.dp))
                Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        PasswordField(confirm, { confirm = it }, "Confirm password", isError = confirm.isNotEmpty() && confirm != password, support = if (confirm.isNotEmpty() && confirm != password) "Passwords don't match" else null)
        InfoNote("Anyone opening this PDF will need the password. We can't recover it if you forget it.")
    }
}

@Composable
private fun UnlockTool(vm: ToolViewModel, initialUri: Uri?) {
    val pdf = rememberSinglePdf(vm, initialUri)
    var password by remember { mutableStateOf("") }
    val picked = pdf.picked
    ToolScaffold(
        PdfTool.UNLOCK, "Remove password", picked != null && password.isNotEmpty(),
        onAction = {
            val src = picked ?: return@ToolScaffold
            val pwd = password
            vm.run(PdfTool.UNLOCK, "Unlocking your PDF") {
                val input = copy(src.uri)
                val out = work()
                tools.unlock(input, pwd, out) { progress(it) }
                input.delete()
                val output = publishPdf(out, storage.uniqueName(Format.baseName(src.name), "pdf", "unlocked"))
                ToolResult(PdfTool.UNLOCK, "PDF unlocked", "The password has been removed. You can now open it without a password.", listOf(output))
            }
        },
    ) {
        if (picked == null) PickPrompt("Select a protected PDF", "Choose the file to unlock", onClick = { pdf.pick() })
        else PickedPdfCard(picked, onChange = { pdf.pick() })
        OptionLabel("Current password")
        PasswordField(password, { password = it }, "Password")
        InfoNote("Only unlock documents you own or have permission to change.")
    }
}
