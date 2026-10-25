package com.saif.imagetopdf.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.saif.imagetopdf.UiState
import com.saif.imagetopdf.engine.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Navy = Color(0xFF1E3A8A)
private val Sky = Color(0xFFBFDBFE)
private val PdfRed = Color(0xFFDC2626)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            secondary = PdfRed,
            background = Color(0xFFF5F7FB),
            surface = Color.White,
        ),
        content = { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() } },
    )
}

// ---------------------------------------------------------------- Splash

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(600)) }
        scale.animateTo(1f, tween(700))
        delay(900)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value).graphicsLayer { alpha = fade.value },
        ) {
            DocBadge(size = 96)
            Spacer(Modifier.height(24.dp))
            Text("Image to PDF", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Rebuild photos as real, editable PDFs", color = Sky, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("100% offline · no servers", color = Sky.copy(alpha = 0.8f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun DocBadge(size: Int) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 5).dp)).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(6.dp)).background(PdfRed).padding(horizontal = 10.dp, vertical = 4.dp),
        ) { Text("PDF", color = Color.White, fontWeight = FontWeight.Black, fontSize = (size / 5).sp) }
    }
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(onImagePicked: (Uri) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImagePicked(uri)
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Image to PDF", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.fillMaxWidth())
        Text(
            "Upload a photo of a document. It is rebuilt as a real PDF with selectable text, matching fonts, colors and layout.",
            color = Color(0xFF475569), fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(2.dp, Sky, RoundedCornerShape(20.dp))
                .clickable { pick() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)).background(Sky),
                    contentAlignment = Alignment.Center,
                ) { Text("+", fontSize = 40.sp, color = Navy, fontWeight = FontWeight.Light) }
                Spacer(Modifier.height(14.dp))
                Text("Tap to upload image", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Navy)
                Text("JPG · PNG · WEBP", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Feature("Text recognition", "Real text in the PDF: search, select and copy it.")
        Feature("Font detection", "Serif / sans / mono, weight (light → black), italic, size.")
        Feature("Design rebuild", "Colors, background bands, boxes and lines redrawn as vectors.")
        Feature("Private", "Everything runs on your phone. No upload, no API.")
        Spacer(Modifier.weight(1f))
        Button(onClick = { pick() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Upload image", fontSize = 16.sp)
        }
    }
}

@Composable
private fun Feature(title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 6.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(PdfRed))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(body, color = Color(0xFF64748B), fontSize = 13.sp)
        }
    }
}

// ---------------------------------------------------------------- Processing

@Composable
fun ProcessingScreen(state: UiState.Processing) {
    val thumb = rememberImage(state.image, 900)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        thumb?.let {
            Image(
                it.asImageBitmap(), null,
                Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(color = Navy)
        Spacer(Modifier.height(20.dp))
        Text("Converting…", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(state.stage, color = Color(0xFF64748B), modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Navy, trackColor = Sky,
        )
        Text("${(state.progress * 100).toInt()}%", modifier = Modifier.padding(top = 8.dp), color = Navy)
    }
}

// ---------------------------------------------------------------- Result

@Composable
fun ResultScreen(state: UiState.Done, onNew: () -> Unit) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val pdf = remember(state.result.pdfPath) { File(state.result.pdfPath) }
    var tab by remember { mutableIntStateOf(0) }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out -> pdf.inputStream().use { it.copyTo(out) } }
                }.isSuccess
            }
            Toast.makeText(context, if (ok) "PDF saved" else "Could not save PDF", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PDF ready", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(
                    "${state.result.wordCount} words · ${state.result.lineCount} lines · ${state.result.shapeCount} shapes",
                    fontSize = 12.sp, color = Color(0xFF64748B),
                )
            }
            OutlinedButton(onClick = onNew) { Text("New") }
        }
        TabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = Navy) {
            Tab(tab == 0, { tab = 0 }, text = { Text("PDF preview") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Original") })
            Tab(tab == 2, { tab = 2 }, text = { Text("Fonts") })
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFE2E8F0))) {
            when (tab) {
                0 -> ZoomableBitmap(rememberPdfPage(pdf))
                1 -> ZoomableBitmap(rememberImage(state.image, 2000))
                else -> FontsList(state)
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", pdf)
                    val send = Intent(Intent.ACTION_SEND).setType("application/pdf")
                        .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(send, "Share PDF"))
                },
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp),
            ) { Text("Share") }
            Button(
                onClick = { saver.launch("converted_${System.currentTimeMillis() / 1000}.pdf") },
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp),
            ) { Text("Download PDF") }
        }
    }
}

@Composable
private fun FontsList(state: UiState.Done) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Detected text styles", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        for (f in state.result.fonts) {
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(f.label, Modifier.weight(1f), fontSize = 14.sp)
                    Text("${f.words} words", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Fonts are matched to the closest font family installed on the phone (Roboto, Noto Serif, Droid Sans Mono) with the detected weight, slant and size.",
            fontSize = 12.sp, color = Color(0xFF64748B), textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun ZoomableBitmap(bitmap: Bitmap?) {
    if (bitmap == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Navy) }
        return
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, z, _ ->
                    zoom = (zoom * z).coerceIn(1f, 6f)
                    offset = if (zoom == 1f) Offset.Zero else offset + pan
                }
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap.asImageBitmap(), null,
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = zoom; scaleY = zoom; translationX = offset.x; translationY = offset.y
            },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun rememberImage(uri: Uri, maxSide: Int): Bitmap? {
    val context = LocalContext.current
    val value by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching { ImageLoader.load(context, uri, minSide = 1, maxSide = maxSide) }.getOrNull()
        }
    }
    return value
}

@Composable
private fun rememberPdfPage(file: File): Bitmap? {
    val value by produceState<Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        renderer.openPage(0).use { page ->
                            val width = 1800
                            val height = (width.toFloat() * page.height / page.width).toInt()
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                                it.eraseColor(AColor.WHITE)
                                page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }
            }.getOrNull()
        }
    }
    return value
}

// ---------------------------------------------------------------- Error

@Composable
fun FailedScreen(message: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Couldn't convert", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PdfRed)
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp), color = Color(0xFF475569))
        Button(onClick = onBack) { Text("Try another image") }
    }
}
