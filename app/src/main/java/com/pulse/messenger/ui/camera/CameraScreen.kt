package com.pulse.messenger.ui.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.data.MockData
import com.pulse.messenger.data.StatusItem
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Camera stand-in for the offline demo: a live-looking viewfinder over sample photos,
 * shutter/flash/flip controls, a recent-photos strip and a caption + send preview.
 * [target] is "chat:<id>", "status" or "share".
 */
@Composable
fun CameraScreen(nav: NavController, target: String) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val scenes = remember { (MockData.statusPhotos + MockData.photos.take(8)).shuffled() }
    var scene by remember { mutableIntStateOf(0) }
    var flash by remember { mutableIntStateOf(0) }
    var front by remember { mutableStateOf(false) }
    var videoMode by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var recSeconds by remember { mutableIntStateOf(0) }
    var captured by remember { mutableStateOf<Pair<Int, Int?>?>(null) } // res, video seconds
    val shutterFlash = remember { Animatable(0f) }
    var caption by remember { mutableStateOf("") }

    // Slow drift between scenes to fake a live viewfinder.
    val drift = rememberInfiniteTransition(label = "drift")
    val pan by drift.animateFloat(-14f, 14f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "pan")
    LaunchedEffect(Unit) {
        while (true) { delay(7000); if (captured == null && !recording) scene = (scene + 1) % scenes.size }
    }
    LaunchedEffect(recording) {
        recSeconds = 0
        while (recording) { delay(1000); recSeconds++ }
    }

    fun deliver(content: MessageContent) {
        when {
            target.startsWith("chat:") -> {
                repo.send(target.removePrefix("chat:"), content)
                nav.back()
            }
            target == "status" -> {
                val res = when (content) {
                    is MessageContent.Image -> content.res
                    is MessageContent.Video -> content.thumb
                    else -> scenes[scene]
                }
                repo.postStatus(StatusItem("", System.currentTimeMillis(), image = res, caption = caption.trim()))
                snackbar.show("Status shared with your contacts", SnackType.SUCCESS)
                nav.back()
            }
            else -> {
                repo.setPendingShare(listOf(content))
                nav.navigate(Routes.SHARE_PICKER) { popUpTo(Routes.HOME) }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val shot = captured
        if (shot == null) {
            AnimatedContent(scene, transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) }, label = "scene") { i ->
                AsyncImage(
                    model = scenes[i], contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        translationX = pan
                        scaleX = if (front) -1.08f else 1.08f; scaleY = 1.08f
                    },
                )
            }
            // Focus reticle
            val pulse by drift.animateFloat(0.92f, 1.04f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "focus")
            Box(Modifier.align(Alignment.Center).size(80.dp).scale(pulse).border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(14.dp)))
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = shutterFlash.value)))

            // Top controls
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { nav.back() }) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                if (recording) {
                    val blink by drift.animateFloat(1f, 0.2f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "rec")
                    Row(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Red.copy(alpha = blink)))
                        Spacer(Modifier.width(6.dp))
                        Text("%d:%02d".format(recSeconds / 60, recSeconds % 60), color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.weight(1f))
                }
                IconButton(onClick = { flash = (flash + 1) % 3 }) {
                    Icon(
                        when (flash) { 1 -> Icons.Filled.FlashOn; 2 -> Icons.Filled.FlashAuto; else -> Icons.Filled.FlashOff },
                        "Flash", tint = Color.White,
                    )
                }
                IconButton(onClick = { snackbar.show("HDR on", SnackType.INFO) }) { Icon(Icons.Filled.HdrOn, "HDR", tint = Color.White) }
            }

            // Bottom controls
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(MockData.photos.take(14)) { res ->
                        AsyncImage(
                            model = res, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(8.dp)).clickable { captured = res to null },
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { captured = MockData.photos.random() to null }, modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))) {
                        Icon(Icons.Filled.PhotoLibrary, "Gallery", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    val inner by animateFloatAsState(if (recording) 0.45f else 1f, label = "shutter")
                    Box(
                        Modifier
                            .size(78.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(7.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (videoMode) {
                                    if (recording) {
                                        recording = false
                                        captured = MockData.videoThumbs.random() to recSeconds.coerceAtLeast(1)
                                    } else recording = true
                                } else {
                                    scope.launch {
                                        shutterFlash.snapTo(0.85f)
                                        shutterFlash.animateTo(0f, tween(260))
                                    }
                                    captured = scenes[scene] to null
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.fillMaxSize().scale(inner)
                                .clip(if (recording) RoundedCornerShape(10.dp) else CircleShape)
                                .background(if (videoMode) Color(0xFFE5484D) else Color.White),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { front = !front }, modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))) {
                        Icon(Icons.Filled.FlipCameraAndroid, "Flip camera", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    ModeLabel("Video", videoMode) { if (!recording) videoMode = true }
                    ModeLabel("Photo", !videoMode) { if (!recording) videoMode = false }
                }
            }
        } else {
            // Preview + caption
            AsyncImage(model = shot.first, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { captured = null }) { Icon(Icons.Filled.Close, "Discard", tint = Color.White) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { snackbar.show("Crop applied", SnackType.SUCCESS) }) { Icon(Icons.Filled.Crop, "Crop", tint = Color.White) }
                IconButton(onClick = { snackbar.show("Sticker tray coming soon", SnackType.INFO) }) { Icon(Icons.Outlined.EmojiEmotions, "Stickers", tint = Color.White) }
                IconButton(onClick = { snackbar.show("Tap the caption field to add text", SnackType.INFO) }) { Icon(Icons.Filled.TextFields, "Text", tint = Color.White) }
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 18.dp, vertical = 13.dp),
                ) {
                    BasicTextField(
                        caption, { caption = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner ->
                            if (caption.isEmpty()) Text("Add a caption…", color = Color.White.copy(alpha = 0.6f))
                            inner()
                        },
                    )
                }
                Spacer(Modifier.width(10.dp))
                FloatingActionButton(
                    onClick = {
                        val (res, sec) = shot
                        deliver(if (sec != null) MessageContent.Video(res, sec, caption.trim()) else MessageContent.Image(res, caption.trim()))
                    },
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape,
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
            }
            if (shot.second != null) {
                Text(
                    "Video · ${shot.second}s", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ModeLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Color.Black else Color.White,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
