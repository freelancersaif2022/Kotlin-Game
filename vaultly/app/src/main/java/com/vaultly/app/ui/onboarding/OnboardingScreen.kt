package com.vaultly.app.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultly.app.R
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.theme.Palette
import kotlinx.coroutines.launch

private data class Page(val icon: ImageVector, val kicker: String, val title: String, val body: String)

private val pages = listOf(
    Page(
        Icons.Outlined.DocumentScanner, "SCAN",
        "Every receipt,\nsafely kept.",
        "Snap a paper receipt or import an e-receipt. Vaultly reads the store, date and total on your device — nothing leaves your phone.",
    ),
    Page(
        Icons.Outlined.NotificationsActive, "REMIND",
        "Never miss a\nreturn again.",
        "Return windows and warranties are tracked automatically, with gentle reminders before each one ends.",
    ),
    Page(
        Icons.Outlined.PictureAsPdf, "CLAIM",
        "Proof, ready\nin one tap.",
        "Export a polished claim pack with your receipt, photos and dates — perfect for a return desk or warranty claim.",
    ),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    val finish = {
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B2A4A), Palette.Ink, Palette.InkDeep))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.30f)
            listOf(110, 165, 220, 280, 345).forEachIndexed { i, r ->
                drawCircle(Palette.GoldLight.copy(alpha = 0.13f - i * 0.022f), r.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("VAULTLY", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 4), color = Palette.GoldLight)
                Spacer(Modifier.weight(1f))
                if (pager.currentPage < pages.lastIndex) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { finish() }.padding(8.dp),
                    )
                }
            }
            HorizontalPager(pager, Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(0.6f))
                    Emblem(page.icon, index == 0)
                    Spacer(Modifier.weight(0.5f))
                    Text(page.kicker, style = MaterialTheme.typography.labelSmall, color = Palette.Gold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.66f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(0.4f))
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { i ->
                    val selected = pager.currentPage == i
                    val w by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dot")
                    val c by animateColorAsState(if (selected) Palette.GoldLight else Color.White.copy(alpha = 0.25f), label = "dotc")
                    Box(Modifier.padding(horizontal = 4.dp).height(8.dp).width(w).clip(CircleShape).background(c))
                }
            }
            PrimaryButton(
                if (pager.currentPage == pages.lastIndex) "Open my vault" else "Continue",
                {
                    if (pager.currentPage == pages.lastIndex) finish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                gold = true,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Emblem(icon: ImageVector, showLogo: Boolean) {
    Box(
        Modifier
            .size(168.dp)
            .clip(RoundedCornerShape(52.dp))
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))))
            .border(1.dp, Palette.GoldLight.copy(alpha = 0.35f), RoundedCornerShape(52.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (showLogo) {
            Icon(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(190.dp), tint = Color.Unspecified)
        } else {
            Icon(icon, null, Modifier.size(72.dp), tint = Palette.GoldLight)
        }
    }
}
