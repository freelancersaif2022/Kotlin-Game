package com.pdfmaster.app.ui.result

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pdfmaster.app.R
import com.pdfmaster.app.ads.NativeAdCard
import com.pdfmaster.app.data.model.OutputFile
import com.pdfmaster.app.data.repo.OutputKind
import com.pdfmaster.app.ui.components.ConfettiBurst
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.PdfThumbnail
import com.pdfmaster.app.ui.components.PrimaryButton
import com.pdfmaster.app.ui.components.SecondaryButton
import com.pdfmaster.app.ui.components.bounceClick
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.tool.ToolViewModel
import com.pdfmaster.app.util.FileActions
import com.pdfmaster.app.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResultScreen() {
    val vm: ToolViewModel = hiltViewModel()
    val store = remember { vm.resultStore() }
    val result = remember { store.current }
    val nav = LocalNavigator.current
    BackHandler { nav.home() }
    if (result == null) {
        LaunchedEffect(Unit) { nav.home() }
        return
    }
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(0.4f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        delay(150)
        fade.animateTo(1f, tween(450))
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.home() }) { Icon(Icons.Rounded.Close, "Close") }
            }
            Image(
                painterResource(R.drawable.img_success_done), null,
                Modifier.align(Alignment.CenterHorizontally).size(170.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            )
            Column(Modifier.fillMaxWidth().graphicsLayer { alpha = fade.value; translationY = (1f - fade.value) * 20.dp.toPx() }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(result.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(result.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                if (result.outputs.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Saved to ${vm.storage.locationLabel(result.kind)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            if (result.before != null && result.after != null) {
                SizeComparison(result.before, result.after)
                Spacer(Modifier.height(16.dp))
            }

            when {
                result.text != null -> TextResult(result.text,
                    onCopy = { FileActions.copy(context, result.text); snackbar.success("Copied to clipboard") },
                    onShare = { FileActions.shareText(context, result.text.take(90_000)) },
                    onSave = {
                        scope.launch {
                            val f = vm.storage.newWorkFile("txt")
                            withContext(Dispatchers.IO) { f.writeText(result.text) }
                            vm.storage.publish(f, vm.storage.uniqueName("Extracted_text", "txt"), "text/plain", OutputKind.TEXT)
                            snackbar.success("Saved to ${vm.storage.locationLabel(OutputKind.TEXT)}", title = "Text file saved")
                        }
                    },
                )
                result.kind == OutputKind.IMAGE -> {
                    ImageGrid(result.outputs)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Share images", { FileActions.share(context, result.outputs.map { it.uri }, "image/*") }, icon = Icons.Rounded.Share)
                }
                else -> {
                    result.outputs.forEach { out -> OutputRow(out, onOpen = { nav.home(); nav.viewer(out.uri) }, onShare = { FileActions.share(context, listOf(out.uri)) }) }
                    Spacer(Modifier.height(16.dp))
                    if (result.outputs.size == 1) {
                        val out = result.outputs.first()
                        PrimaryButton("Open PDF", { nav.home(); nav.viewer(out.uri) }, icon = Icons.AutoMirrored.Rounded.OpenInNew)
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("Share", { FileActions.share(context, listOf(out.uri)) }, icon = Icons.Rounded.Share)
                    } else {
                        PrimaryButton("Share all (${result.outputs.size})", { FileActions.share(context, result.outputs.map { it.uri }) }, icon = Icons.Rounded.Share)
                    }
                }
            }
            TextButton(onClick = { nav.home() }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)) {
                Icon(Icons.Rounded.Home, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Back to home", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            NativeAdCard()
            Spacer(Modifier.height(24.dp))
        }
        ConfettiBurst(Modifier.fillMaxSize())
    }
}

@Composable
private fun SizeComparison(before: Long, after: Long) {
    val ratio = (after.toFloat() / before.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(ratio, tween(1200), label = "ratio")
    val saved = ((1f - ratio) * 100).toInt()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(20.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Before", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Format.size(before), style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("After", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Format.size(after), style = MaterialTheme.typography.titleLarge, color = AppTheme.colors.success)
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { animated }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = AppTheme.colors.success, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh, strokeCap = StrokeCap.Round, drawStopIndicator = {},
        )
        Spacer(Modifier.height(8.dp))
        Text(if (saved > 0) "$saved% smaller" else "Already optimised", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.success, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OutputRow(out: OutputFile, onOpen: () -> Unit, onShare: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(18.dp)).bounceClick(scaleDown = 0.985f, onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PdfThumbnail(out.uri)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(out.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(Format.size(out.size), out.pageCount.takeIf { it > 0 }?.let(Format::pages)).joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ImageGrid(outputs: List<OutputFile>) {
    val context = LocalContext.current
    val shown = outputs.take(6)
    shown.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEachIndexed { i, out ->
                val bmp by produceState<Bitmap?>(null, out.uri) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            if (Build.VERSION.SDK_INT >= 28) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, out.uri)) { d, info, _ ->
                                    val s = 300f / maxOf(info.size.width, info.size.height)
                                    if (s < 1f) d.setTargetSize((info.size.width * s).toInt().coerceAtLeast(1), (info.size.height * s).toInt().coerceAtLeast(1))
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, out.uri)
                            }
                        }.getOrNull()
                    }
                }
                Box(Modifier.weight(1f).aspectRatio(0.75f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    val isLast = i == row.lastIndex && out == shown.last() && outputs.size > shown.size
                    if (isLast) {
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x991B1D29)), contentAlignment = Alignment.Center) {
                            Text("+${outputs.size - shown.size}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun TextResult(text: String, onCopy: () -> Unit, onShare: () -> Unit, onSave: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(18.dp)),
    ) {
        SelectionContainer {
            Text(
                text.take(20_000),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Copy text", onCopy, icon = Icons.Rounded.ContentCopy)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton("Share", onShare, Modifier.weight(1f), icon = Icons.Rounded.Share)
        SecondaryButton("Save .txt", onSave, Modifier.weight(1f), icon = Icons.Rounded.Save)
    }
}
