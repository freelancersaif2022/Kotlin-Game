package com.lexora.english.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lexora.english.content.Course
import com.lexora.english.content.Projects
import com.lexora.english.data.AppState
import com.lexora.english.ui.Nav
import com.lexora.english.ui.components.CircleIconButton
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.LinearProgress
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.components.ScreenHeader
import com.lexora.english.ui.components.SecondaryButton
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.CertificateRenderer
import com.lexora.english.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CertificateScreen(state: AppState, nav: Nav) {
    val c = Lex.colors
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val p = state.progress
    val issued = p.certificateIssuedAt != null
    val name = state.profile?.name ?: "Learner"
    var busy by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }

    val preview by produceState<Bitmap?>(null, name, p.certificateIssuedAt) {
        value = withContext(Dispatchers.Default) {
            CertificateRenderer.render(
                context,
                if (issued) name else name,
                p.certificateIssuedAt ?: System.currentTimeMillis(),
                p.certificateId ?: "LX-PREVIEW",
                width = 1600,
            )
        }
    }

    fun export(action: String) {
        if (busy) return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val full = CertificateRenderer.render(context, name, p.certificateIssuedAt!!, p.certificateId ?: "LX", 2480)
                val result = when (action) {
                    "png" -> CertificateRenderer.saveToGallery(context, full, name)
                    "pdf" -> CertificateRenderer.savePdf(context, full, name)
                    else -> {
                        val uri = CertificateRenderer.cachePng(context, full, name)
                        withContext(Dispatchers.Main) { context.startActivity(CertificateRenderer.shareIntent(context, uri)) }
                        true
                    }
                }
                full.recycle()
                result
            }
            busy = false
            if (action != "share") {
                if (ok) haptics.success() else haptics.error()
                Toast.makeText(
                    context,
                    when {
                        !ok -> "Couldn't save. Please try again."
                        action == "png" -> "Saved to Pictures/Lexora"
                        else -> "PDF saved to Downloads"
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        ScreenHeader("Certificate", { nav.back() }, subtitle = if (issued) "Issued ${CertificateRenderer.formatDate(p.certificateIssuedAt!!)}" else "Locked")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2480f / 1754f)
                    .shadow(16.dp, RoundedCornerShape(12.dp), spotColor = Color(0x33101828))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7F3EA))
                    .clickable(enabled = issued && preview != null) { fullscreen = true },
                contentAlignment = Alignment.Center,
            ) {
                val bmp = preview
                if (bmp == null) {
                    CircularProgressIndicator(color = Brand.Blue, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                } else {
                    Image(bmp.asImageBitmap(), "Certificate", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    if (!issued) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Brand.Blue),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Lock, null, tint = Color.White)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Preview", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1B2A6B))
                            }
                        }
                    } else {
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(6.dp)
                        ) {
                            Icon(Icons.Rounded.Fullscreen, "Full screen", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            if (issued) {
                Text("Congratulations, $name! 🎉", style = MaterialTheme.typography.headlineSmall, color = c.text)
                Spacer(Modifier.height(6.dp))
                Text(
                    "You completed the Lexora Complete English Course and all ${Projects.all.size} real-life projects. Save your certificate or share it with friends and employers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(8.dp))
                Text("Certificate ID: ${p.certificateId}", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                Spacer(Modifier.height(20.dp))
                PrimaryButton(if (busy) "Please wait…" else "Share certificate", { export("share") }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Share, enabled = !busy)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Save image", { export("png") }, Modifier.weight(1f), icon = Icons.Rounded.Download, enabled = !busy)
                    SecondaryButton("Save PDF", { export("pdf") }, Modifier.weight(1f), icon = Icons.Rounded.PictureAsPdf, enabled = !busy)
                }
            } else {
                Text("Earn your certificate", style = MaterialTheme.typography.headlineSmall, color = c.text)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your professional certificate — signed by Saif A. and showing your name — unlocks when you finish every module and project.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(16.dp))
                RequirementRow("A to Z course", p.passedModules.size, Course.modules.size, Brand.Blue)
                Spacer(Modifier.height(10.dp))
                RequirementRow("Real-life projects", p.completedProjects.size, Projects.all.size, Brand.Indigo)
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Keep learning", { nav.back() }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    val bmp = preview
    if (fullscreen && bmp != null) {
        Dialog(onDismissRequest = { fullscreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullscreen = false },
                contentAlignment = Alignment.Center,
            ) {
                // Rotate to landscape so the certificate uses the whole screen.
                val longSide = maxHeight
                val shortSide = maxWidth
                val w = minOf(longSide, shortSide * (2480f / 1754f))
                Image(
                    bmp.asImageBitmap(),
                    "Certificate",
                    Modifier
                        .requiredSize(w, w * (1754f / 2480f))
                        .graphicsLayer { rotationZ = 90f },
                    contentScale = ContentScale.Fit,
                )
                CircleIconButton(
                    Icons.Rounded.Close, "Close", { fullscreen = false },
                    Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp),
                    tint = Color.White,
                    background = Color.White.copy(alpha = 0.15f),
                )
            }
        }
    }
}

@Composable
private fun RequirementRow(label: String, done: Int, total: Int, color: Color) {
    val c = Lex.colors
    LexCard(Modifier.fillMaxWidth(), elevated = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
            Text("$done/$total", style = MaterialTheme.typography.titleSmall, color = if (done >= total) c.success else color)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgress(done / total.toFloat(), color = if (done >= total) c.success else color)
        if (done < total) {
            Spacer(Modifier.height(6.dp))
            Text("${total - done} remaining", style = MaterialTheme.typography.labelMedium, color = c.textMuted, textAlign = TextAlign.Start, modifier = Modifier.width(200.dp))
        }
    }
}
