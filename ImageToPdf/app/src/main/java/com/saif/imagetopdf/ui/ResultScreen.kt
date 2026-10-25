package com.saif.imagetopdf.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.saif.imagetopdf.UiState
import com.saif.imagetopdf.engine.Family
import com.saif.imagetopdf.engine.FontUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ResultScreen(state: UiState.Done, onNew: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val pdf = remember(state.result.pdfPath) { File(state.result.pdfPath) }
    val pager = rememberPagerState(pageCount = { 3 })

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out -> pdf.inputStream().use { it.copyTo(out) } }
                        ?: error("no stream")
                }.isSuccess
            }
            snackbar.showSnackbar(if (ok) "PDF saved successfully" else "Could not save the PDF")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Header with animated success mark and counters
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Palette.header)
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuccessMark()
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your PDF is ready", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Real text · matched fonts · exact marks", color = Palette.Sky, fontSize = 13.sp)
                    }
                    val i = remember { MutableInteractionSource() }
                    Box(
                        Modifier.size(40.dp).pressScale(i).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                            .clickable(i, ripple(color = Color.White), onClick = onNew),
                        contentAlignment = Alignment.Center,
                    ) { Icon(AppIcons.Close, "New", tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(state.result.wordCount, "Words", Modifier.weight(1f))
                    Stat(state.result.lineCount, "Lines", Modifier.weight(1f))
                    Stat(state.result.fonts.size, "Styles", Modifier.weight(1f))
                    Stat(state.result.graphicCount, "Marks", Modifier.weight(1f))
                }
            }

            Segmented(
                listOf("PDF", "Original", "Fonts"), pager.currentPage,
                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            ) { scope.launch { pager.animateScrollToPage(it) } }

            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                when (page) {
                    0 -> PageCard(rememberPdfPage(pdf))
                    1 -> PageCard(rememberImage(state.image, 2000))
                    else -> FontsList(state.result.fonts)
                }
            }

            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton("Share", AppIcons.Share, Modifier.weight(1f)) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", pdf)
                    val send = Intent(Intent.ACTION_SEND).setType("application/pdf")
                        .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(send, "Share PDF"))
                }
                PrimaryButton("Download", AppIcons.Download, Modifier.weight(1.4f)) {
                    saver.launch("converted_${System.currentTimeMillis() / 1000}.pdf")
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp))
    }
}

/** Green circle that pops in, then draws its check mark stroke by stroke. */
@Composable
private fun SuccessMark() {
    val pop = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(Unit) {
        delay(250)
        draw.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
    }
    Canvas(Modifier.size(48.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
        drawCircle(Palette.Green)
        val path = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.67f)
            lineTo(size.width * 0.73f, size.height * 0.36f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * draw.value, part, true)
        drawPath(part, Color.White, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun Stat(value: Int, label: String, modifier: Modifier) {
    val count = remember { Animatable(0f) }
    LaunchedEffect(value) { count.animateTo(value.toFloat(), tween(1300, easing = FastOutSlowInEasing)) }
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.1f)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${count.value.toInt()}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Palette.Sky, fontSize = 11.sp)
    }
}

/** Pill tabs with an indicator that slides between them. */
@Composable
private fun Segmented(items: List<String>, selected: Int, modifier: Modifier, onSelect: (Int) -> Unit) {
    BoxWithConstraints(
        modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Palette.Mist).padding(4.dp),
    ) {
        val segment = maxWidth / items.size
        val x by animateDpAsState(segment * selected, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier.offset(x = x).width(segment).fillMaxHeight()
                .shadow(6.dp, RoundedCornerShape(19.dp), spotColor = Palette.Navy.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(19.dp)).background(Color.White),
        )
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(19.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, fontSize = 14.sp,
                        fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (i == selected) Palette.Navy else Palette.Muted,
                    )
                }
            }
        }
    }
}

/** A page on a soft shadow; pinch to zoom, double-tap to zoom in/out. */
@Composable
private fun PageCard(bitmap: Bitmap?) {
    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        if (bitmap == null) {
            CircularProgressIndicator(color = Palette.Royal)
            return@Box
        }
        val zoom = remember { Animatable(1f) }
        val pan = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val scope = rememberCoroutineScope()
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        Box(
            Modifier
                .aspectRatio(bitmap.width.toFloat() / bitmap.height, matchHeightConstraintsFirst = true)
                .graphicsLayer {
                    alpha = appear.value
                    val s = 0.94f + 0.06f * appear.value
                    scaleX = s; scaleY = s
                }
                .shadow(18.dp, RoundedCornerShape(6.dp), spotColor = Palette.Navy.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scope.launch {
                            val target = if (zoom.value > 1.2f) 1f else 2.5f
                            launch { zoom.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow)) }
                            if (target == 1f) pan.animateTo(Offset.Zero, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, p, z, _ ->
                        scope.launch {
                            val nz = (zoom.value * z).coerceIn(1f, 6f)
                            zoom.snapTo(nz)
                            pan.snapTo(if (nz == 1f) Offset.Zero else pan.value + p)
                        }
                    }
                },
        ) {
            Image(
                bitmap.asImageBitmap(), null,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom.value; scaleY = zoom.value
                    translationX = pan.value.x; translationY = pan.value.y
                },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun FontsList(fonts: List<FontUsage>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        fonts.forEachIndexed { index, f ->
            val enter = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(60L * index)
                enter.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .graphicsLayer { alpha = enter.value; translationX = (1 - enter.value) * 80f }
                    .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Palette.Navy.copy(alpha = 0.12f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Palette.Mist),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Aa", fontSize = 24.sp, color = Palette.Navy,
                        fontFamily = when (f.family) {
                            Family.SERIF -> FontFamily.Serif
                            Family.MONO -> FontFamily.Monospace
                            Family.SANS, Family.CONDENSED -> FontFamily.SansSerif
                        },
                        fontWeight = FontWeight(f.weight),
                        fontStyle = if (f.italic) FontStyle.Italic else FontStyle.Normal,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Palette.Text)
                    Text("${f.words} words", fontSize = 12.sp, color = Palette.Muted)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Seals, signatures and logos are copied pixel-exact from your photo. Text is real PDF text in the closest phone font with the detected weight, slant and size.",
            fontSize = 12.sp, color = Palette.Muted, lineHeight = 17.sp,
        )
        Spacer(Modifier.height(12.dp))
    }
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
                                it.eraseColor(android.graphics.Color.WHITE)
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
