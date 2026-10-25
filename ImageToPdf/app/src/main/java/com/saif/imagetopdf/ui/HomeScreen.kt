package com.saif.imagetopdf.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(onImagePicked: (Uri) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImagePicked(uri)
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Palette.header)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 84.dp),
        ) {
            Column(Modifier.enter(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { Text("PDF", color = Palette.Red, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                    Spacer(Modifier.width(12.dp))
                    Text("Image to PDF", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Pill("Offline", AppIcons.Lock)
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    "Turn any document photo\ninto a real PDF",
                    color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Selectable text, the same fonts and bold, seals, signatures and lines kept exactly.",
                    color = Palette.Sky, fontSize = 14.sp, lineHeight = 20.sp,
                )
            }
        }

        UploadCard(onClick = pick, modifier = Modifier.offset(y = (-58).dp).padding(horizontal = 20.dp).enter(1))

        Column(Modifier.offset(y = (-34).dp).padding(horizontal = 20.dp)) {
            Text("What gets rebuilt", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Palette.Text, modifier = Modifier.enter(2))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Feature(AppIcons.Text, "Real text", "Search, select & copy", Modifier.weight(1f).enter(3))
                Feature(AppIcons.Font, "Fonts & bold", "Family, weight, size", Modifier.weight(1f).enter(4))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Feature(AppIcons.Seal, "Seals & signs", "Kept pixel-exact", Modifier.weight(1f).enter(5))
                Feature(AppIcons.Design, "Design", "Colors, boxes, lines", Modifier.weight(1f).enter(6))
            }
            Spacer(Modifier.height(24.dp))
            Steps(Modifier.enter(7))
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Upload image", AppIcons.Upload, Modifier.fillMaxWidth().navigationBarsPadding().enter(8)) { pick() }
        }
    }
}

/** Staggered fade-and-rise entrance for list items. */
@Composable
private fun Modifier.enter(index: Int): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(70L * index)
        p.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    return graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 60f }
}

@Composable
private fun UploadCard(onClick: () -> Unit, modifier: Modifier) {
    val loop = rememberInfiniteTransition(label = "upload")
    val phase by loop.animateFloat(0f, 60f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "dash")
    val breathe by loop.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val ring by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "ring")
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.98f)
            .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Palette.Navy.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .clickable(interaction, ripple(color = Palette.Royal), onClick = onClick)
            .padding(14.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        Palette.Royal.copy(alpha = 0.45f),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(1.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f), phase)),
                    )
                }
                .background(Brush.verticalGradient(listOf(Color(0xFFF7F9FF), Color.White)), RoundedCornerShape(20.dp))
                .padding(vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Expanding ripple ring
                Box(
                    Modifier.size(84.dp).graphicsLayer {
                        val s = 1f + ring * 0.6f
                        scaleX = s; scaleY = s; alpha = (1f - ring) * 0.5f
                    }.clip(CircleShape).background(Palette.Royal.copy(alpha = 0.35f)),
                )
                Box(
                    Modifier.size(84.dp).graphicsLayer { scaleX = breathe; scaleY = breathe }
                        .shadow(16.dp, CircleShape, spotColor = Palette.Royal)
                        .clip(CircleShape).background(Palette.action),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Upload, null, tint = Color.White, modifier = Modifier.size(38.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Text("Tap to upload a photo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Text)
            Spacer(Modifier.height(4.dp))
            Text("JPG · PNG · WEBP  —  letters, invoices, certificates", fontSize = 12.sp, color = Palette.Muted)
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String, modifier: Modifier) {
    Column(
        modifier
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Palette.Navy.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Palette.Mist),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Palette.Royal, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Palette.Text)
        Text(body, fontSize = 12.sp, color = Palette.Muted)
    }
}

@Composable
private fun Steps(modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Mist)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("Upload", "Analyze", "Get PDF").forEachIndexed { i, label ->
            if (i > 0) Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(2.dp).background(Palette.Sky.copy(alpha = 0.6f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(Palette.Royal),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 12.sp, color = Palette.Navy, fontWeight = FontWeight.Medium)
            }
        }
    }
}
