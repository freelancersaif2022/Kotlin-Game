package com.saif.pymaster.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saif.pymaster.R
import com.saif.pymaster.cert.CertificateData
import com.saif.pymaster.cert.CertificateRenderer
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Rules
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.GoldGradient
import com.saif.pymaster.ui.components.GradientProgressBar
import com.saif.pymaster.ui.components.KeyValueRow
import com.saif.pymaster.ui.components.PrimaryButton
import com.saif.pymaster.ui.components.ScreenTopBar
import com.saif.pymaster.ui.components.SecondaryButton
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.util.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CertificateScreen(state: AppState?, nav: Navigator) {
    val s = state ?: return
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unlocked = Rules.certificateUnlocked(s) && s.progress.certificateId != null

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenTopBar("Certificate", subtitle = "PyMaster Python Developer", onBack = nav::back)
        if (!unlocked) {
            LockedCertificate(s)
            return@Column
        }
        val data = remember(s.profile?.name, s.progress.certificateId) {
            CertificateData(
                name = s.profile?.name?.ifBlank { "Python Developer" } ?: "Python Developer",
                date = formatDate(s.progress.certificateIssuedAt.takeIf { it > 0 } ?: System.currentTimeMillis()),
                id = s.progress.certificateId ?: "",
                modules = Content.modules.size,
                lessons = Content.totalLessons,
                projects = Content.projects.size,
            )
        }
        val preview by produceState<Bitmap?>(null, data) {
            value = withContext(Dispatchers.Default) { CertificateRenderer.render(context, data, highRes = false) }
        }
        var busy by remember { mutableStateOf(false) }

        fun export(action: suspend (Bitmap) -> Unit) {
            if (busy) return
            busy = true
            scope.launch {
                try {
                    val hd = withContext(Dispatchers.Default) {
                        runCatching { CertificateRenderer.render(context, data, highRes = true) }.getOrNull()
                    } ?: preview
                    if (hd != null) action(hd)
                } finally {
                    busy = false
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1754f / 1240f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Brand.PythonYellow.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B1220)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = preview
                if (bmp == null) {
                    CircularProgressIndicator(color = Brand.PythonYellow)
                } else {
                    Image(bmp.asImageBitmap(), "Certificate for ${data.name}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Congratulations, ${data.name}! 🎓", style = MaterialTheme.typography.titleLarge, color = Brand.White)
            Spacer(Modifier.height(4.dp))
            Text(
                "You completed the full Python course and built all ${data.projects} real-world projects. Save your certificate or share it with the world.",
                style = MaterialTheme.typography.bodyMedium,
                color = Brand.TextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth()) {
                KeyValueRow("Recipient", data.name)
                KeyValueRow("Issued on", data.date)
                KeyValueRow("Certificate ID", data.id)
                KeyValueRow("Signed by", "Saif A.")
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                if (busy) "Preparing…" else "Save to gallery",
                icon = Icons.Rounded.Download,
                enabled = !busy,
                brush = GoldGradient,
                textColor = Color(0xFF14110A),
                modifier = Modifier.fillMaxWidth(),
            ) {
                export { bmp ->
                    val ok = withContext(Dispatchers.IO) { CertificateRenderer.saveToGallery(context, bmp, data) }
                    if (ok) {
                        Toast.makeText(context, "Saved to Pictures/PyMaster", Toast.LENGTH_LONG).show()
                    } else {
                        val file = withContext(Dispatchers.IO) { CertificateRenderer.writePng(context, bmp, data) }
                        CertificateRenderer.share(context, file, "image/png")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Share", icon = Icons.Rounded.Share, enabled = !busy, modifier = Modifier.weight(1f)) {
                    export { bmp ->
                        val file = withContext(Dispatchers.IO) { CertificateRenderer.writePng(context, bmp, data) }
                        CertificateRenderer.share(context, file, "image/png")
                    }
                }
                SecondaryButton("PDF", icon = Icons.Rounded.PictureAsPdf, enabled = !busy, modifier = Modifier.weight(1f)) {
                    export { bmp ->
                        val file = withContext(Dispatchers.IO) { CertificateRenderer.writePdf(context, bmp, data) }
                        CertificateRenderer.share(context, file, "application/pdf")
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedCertificate(s: AppState) {
    val courseDone = Rules.courseComplete(s)
    val projectsDone = s.progress.completedProjects.size
    val totalProjects = Content.projects.size
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.certificate_seal), null, Modifier.size(180.dp))
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Lock, null, tint = Brand.PythonYellow, modifier = Modifier.size(32.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Your certificate is locked", style = MaterialTheme.typography.headlineSmall, color = Brand.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "Earn a professional PyMaster certificate — signed by Saif A. and printed with your name — by finishing the full journey.",
            style = MaterialTheme.typography.bodyMedium,
            color = Brand.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        AppCard(Modifier.fillMaxWidth()) {
            Requirement("Complete the Python course (${Content.modules.size} modules)", courseDone)
            GradientProgressBar(Rules.courseFraction(s), Modifier.padding(start = 34.dp, top = 6.dp, bottom = 14.dp), height = 5.dp)
            Requirement("Build all $totalProjects real-world projects ($projectsDone/$totalProjects)", projectsDone >= totalProjects)
            GradientProgressBar(Rules.projectFraction(s), Modifier.padding(start = 34.dp, top = 6.dp), height = 5.dp, brush = GoldGradient)
        }
    }
}

@Composable
private fun Requirement(text: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
            tint = if (done) Brand.Success else Brand.TextMuted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Brand.White)
    }
}
