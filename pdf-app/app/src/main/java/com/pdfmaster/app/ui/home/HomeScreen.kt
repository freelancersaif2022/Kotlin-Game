package com.pdfmaster.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.R
import com.pdfmaster.app.ads.NativeAdCard
import com.pdfmaster.app.premium.PremiumManager
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.EmptyState
import com.pdfmaster.app.ui.components.FileRow
import com.pdfmaster.app.ui.components.SectionHeader
import com.pdfmaster.app.ui.components.bounceClick
import com.pdfmaster.app.ui.files.rememberFileActions
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.navigation.Routes
import com.pdfmaster.app.ui.premium.PassActiveCard
import com.pdfmaster.app.ui.premium.PremiumChip
import com.pdfmaster.app.ui.premium.rememberPremiumState
import com.pdfmaster.app.ui.premium.rememberRewardLauncher
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand
import com.pdfmaster.app.ui.tools.PdfTool
import com.pdfmaster.app.ui.tools.ToolTile
import com.pdfmaster.app.ui.tools.rememberToolLauncher
import java.util.Calendar

@Composable
fun HomeScreen(contentPadding: PaddingValues) {
    val deps = LocalAppDeps.current
    val nav = LocalNavigator.current
    val recent by deps.files.recent.collectAsStateWithLifecycle(initialValue = null)
    val premium = rememberPremiumState()
    val tools = rememberToolLauncher()
    val fileActions = rememberFileActions()
    val reward = rememberRewardLauncher()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) nav.viewer(uri)
    }
    val openPicker = { picker.launch(arrayOf("application/pdf")) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item(key = "header") { Header() }
        item(key = "search") {
            Row(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(16.dp))
                    .bounceClick(scaleDown = 0.99f) { nav.search() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("Search your PDFs…", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item(key = "hero") { HeroCard(onBrowse = openPicker, onScan = { tools.launch(PdfTool.SCAN) }) }
        item(key = "premium") {
            Box(Modifier.animateContentSize()) {
                when {
                    premium.passActive -> PassActiveCard(premium, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), onExtend = { reward.launch() })
                    !premium.isPremium -> UpgradeCard(onWatch = { reward.launch() }, onUpgrade = { nav.premium("home") })
                }
            }
        }
        item(key = "tools_header") {
            Spacer(Modifier.height(14.dp))
            SectionHeader("Popular tools", action = "See all", onAction = { nav.tab(Routes.TOOLS) })
            Spacer(Modifier.height(8.dp))
        }
        item(key = "tools") {
            Column(Modifier.padding(horizontal = 12.dp)) {
                PdfTool.featured.chunked(4).forEachIndexed { row, chunk ->
                    Row(Modifier.fillMaxWidth()) {
                        chunk.forEachIndexed { i, tool ->
                            ToolTile(
                                tool, locked = tool.isPro && !premium.isPremium,
                                onClick = { tools.launch(tool) },
                                modifier = Modifier.weight(1f),
                                index = row * 4 + i,
                            )
                        }
                    }
                }
            }
        }
        item(key = "usage") {
            AnimatedVisibility(!premium.isPremium, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                UsageMeter(premium.freeLeft)
            }
        }
        item(key = "recent_header") {
            Spacer(Modifier.height(18.dp))
            SectionHeader("Recent files", action = if (!recent.isNullOrEmpty()) "View all" else null, onAction = { nav.tab(Routes.FILES) })
            Spacer(Modifier.height(10.dp))
        }
        val list = recent
        when {
            list == null -> Unit
            list.isEmpty() -> {
                item(key = "empty") {
                    EmptyState(
                        R.drawable.img_empty_files, "No recent files",
                        "PDFs you open will show up here for quick access.",
                        action = "Open a PDF", onAction = openPicker, imageSize = 140.dp,
                    )
                }
                item(key = "native_empty") { NativeAdCard(Modifier.padding(horizontal = 20.dp)) }
            }
            else -> {
                itemsIndexed(list.take(6), key = { _, f -> f.key }) { index, file ->
                    Column(Modifier.animateItem()) {
                        FileRow(
                            file,
                            onClick = { nav.viewer(file.uri) },
                            onMore = { fileActions.open(file) },
                            onFavorite = { fileActions.toggleFavorite(file) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                        )
                        if (index == 0) NativeAdCard(Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.img_logo_mark), null, Modifier.size(40.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Brand.Red)); append("PDF"); pop()
                    append(" Master")
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        PremiumChip()
    }
}

@Composable
private fun HeroCard(onBrowse: () -> Unit, onScan: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brand.RedGradient)
            .height(176.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.9f, center = Offset(size.width * 0.95f, size.height * 0.1f))
            drawCircle(Color.White.copy(alpha = 0.06f), radius = size.height * 0.55f, center = Offset(size.width * 0.75f, size.height * 1.05f))
        }
        Image(
            painterResource(R.drawable.img_onboarding_1_read), null,
            Modifier.align(Alignment.CenterEnd).size(150.dp).padding(end = 6.dp),
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 22.dp).fillMaxWidth(0.56f)) {
            Text("Open any PDF", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("Read, edit, sign and share in a tap.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroButton("Browse", Icons.Rounded.FolderOpen, filled = true, onClick = onBrowse)
                HeroButton("Scan", null, filled = false, onClick = onScan)
            }
        }
    }
}

@Composable
private fun HeroButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, filled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Brand.Red, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = if (filled) Brand.Red else Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UpgradeCard(onWatch: () -> Unit, onUpgrade: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(AppTheme.colors.goldSoft, MaterialTheme.colorScheme.surface)))
            .border(1.dp, Brand.Gold.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.img_premium_badge), null, Modifier.size(46.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Unlock Premium free", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Watch one short video for 5 hours of Pro tools with no ads.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Brand.GoldGradient).bounceClick(onClick = onWatch).padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Watch ad", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Brand.RedHorizontal).bounceClick(onClick = onUpgrade).padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Go Premium", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun UsageMeter(freeLeft: Int) {
    val progress by animateFloatAsState(freeLeft / PremiumManager.FREE_DAILY_LIMIT.toFloat(), label = "usage")
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (freeLeft > 0) AppTheme.colors.success else MaterialTheme.colorScheme.error))
        Spacer(Modifier.width(8.dp))
        Text(
            "$freeLeft of ${PremiumManager.FREE_DAILY_LIMIT} free tasks left today",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.width(90.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
    }
}
