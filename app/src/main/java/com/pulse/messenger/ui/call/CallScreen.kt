package com.pulse.messenger.ui.call

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.Message
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.R
import com.pulse.messenger.data.CallDirection
import com.pulse.messenger.data.CallType
import com.pulse.messenger.data.MockData
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.util.Format
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class CallPhase { INCOMING, CALLING, RINGING, CONNECTED, ENDED }

@Composable
fun CallScreen(nav: NavController, peerId: String, video: Boolean, incoming: Boolean) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val settings by LocalSettingsRepo.current.settings.collectAsStateWithLifecycle()
    val contact = repo.contact(peerId)
    val group = if (contact == null) repo.chat(peerId) else null
    val name = contact?.name ?: group?.title ?: "Unknown"
    val avatar = contact?.avatar ?: group?.avatar

    var phase by rememberSaveable { mutableStateOf(if (incoming) CallPhase.INCOMING else CallPhase.CALLING) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    var muted by rememberSaveable { mutableStateOf(false) }
    var speaker by rememberSaveable { mutableStateOf(video) }
    var camera by rememberSaveable { mutableStateOf(video) }
    var frontCam by remember { mutableStateOf(true) }
    var answered by remember { mutableStateOf(!incoming) }
    var selfOffset by remember { mutableStateOf(Offset.Zero) }
    val remoteScene = remember { MockData.videoThumbs.random() }

    fun end(declined: Boolean = false) {
        if (phase == CallPhase.ENDED) return
        val direction = when {
            incoming && (declined || !answered) -> CallDirection.MISSED
            incoming -> CallDirection.INCOMING
            else -> CallDirection.OUTGOING
        }
        repo.logCall(peerId, group != null, if (video) CallType.VIDEO else CallType.VOICE, direction, seconds)
        phase = CallPhase.ENDED
    }

    BackHandler { end() }

    LaunchedEffect(phase) {
        when (phase) {
            CallPhase.CALLING -> { delay(1400); phase = CallPhase.RINGING }
            CallPhase.RINGING -> { delay(2200L + Random.nextLong(1800)); phase = CallPhase.CONNECTED }
            CallPhase.CONNECTED -> while (true) { delay(1000); seconds++ }
            CallPhase.ENDED -> {
                delay(1100)
                if (seconds > 0) snackbar.show("Call ended · ${Format.duration(seconds)}", SnackType.INFO)
                nav.back()
            }
            CallPhase.INCOMING -> Unit
        }
    }

    val status = when (phase) {
        CallPhase.INCOMING -> if (video) "Incoming video call" else "Incoming voice call"
        CallPhase.CALLING -> "Calling…"
        CallPhase.RINGING -> "Ringing…"
        CallPhase.CONNECTED -> Format.duration(seconds)
        CallPhase.ENDED -> "Call ended"
    }
    val showRemoteVideo = video && phase == CallPhase.CONNECTED

    Box(Modifier.fillMaxSize().background(Color(0xFF0B141A))) {
        // Backdrop
        if (showRemoteVideo) {
            val t = rememberInfiniteTransition(label = "remote")
            val s by t.animateFloat(1.04f, 1.1f, infiniteRepeatable(tween(5000), RepeatMode.Reverse), label = "rs")
            AsyncImage(model = remoteScene, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(s))
        } else if (video && avatar != null) {
            AsyncImage(model = avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f })
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        } else {
            AsyncImage(
                model = listOf(R.drawable.call_bg_blur_01, R.drawable.call_bg_blur_02, R.drawable.call_bg_blur_03)[(peerId.hashCode() and 0x7fffffff) % 3],
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.6f)))))

        // Header
        Column(Modifier.fillMaxWidth().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { snackbar.show("Call continues in the background", SnackType.INFO) }) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Minimize", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("End-to-end encrypted", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { snackbar.show("Add participants from your contacts", SnackType.INFO) }) {
                    Icon(Icons.Filled.PersonAdd, "Add participant", tint = Color.White)
                }
            }
            if (!showRemoteVideo) {
                Spacer(Modifier.height(36.dp))
                PulsingAvatar(avatar, name, ringing = phase == CallPhase.RINGING || phase == CallPhase.INCOMING || phase == CallPhase.CALLING, isGroup = group != null)
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }
            Text(name, color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(6.dp))
            AnimatedContent(status, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "status") { s ->
                Text(s, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyLarge)
            }
            if (muted && phase == CallPhase.CONNECTED) {
                Spacer(Modifier.height(8.dp))
                Text("You're muted", color = Color.White, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }

        // Self preview
        if (video && camera && phase != CallPhase.INCOMING && phase != CallPhase.ENDED) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 64.dp, end = 16.dp)
                    .offset { IntOffset(selfOffset.x.roundToInt(), selfOffset.y.roundToInt()) }
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .background(Color(0xFF233138))
                    .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); selfOffset += drag } },
            ) {
                if (settings.avatar != null) {
                    AsyncImage(
                        model = settings.avatar, contentDescription = "You", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = if (frontCam) -1f else 1f },
                    )
                } else {
                    Avatar(null, settings.name, 64.dp, Modifier.align(Alignment.Center))
                }
                IconButton(onClick = { frontCam = !frontCam }, modifier = Modifier.align(Alignment.BottomEnd).size(36.dp)) {
                    Icon(Icons.Filled.FlipCameraAndroid, "Flip camera", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Controls
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (phase == CallPhase.INCOMING) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    BigCallButton(Icons.Filled.CallEnd, "Decline", Color(0xFFE5484D)) { end(declined = true) }
                    BigCallButton(Icons.Outlined.Message, "Message", Color.White.copy(alpha = 0.2f)) {
                        end(declined = true)
                        contact?.let { nav.navigate(Routes.chat(repo.directChatFor(it.id).id)) }
                    }
                    BigCallButton(if (video) Icons.Filled.Videocam else Icons.Filled.Call, "Accept", Color(0xFF1DAA61)) {
                        answered = true; phase = CallPhase.CONNECTED
                    }
                }
            } else {
                Row(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToggleButton(Icons.AutoMirrored.Filled.VolumeUp, "Speaker", speaker) { speaker = !speaker }
                    ToggleButton(if (camera) Icons.Filled.Videocam else Icons.Filled.VideocamOff, "Camera", camera && video) {
                        if (!video) snackbar.show("Switching to video isn't available in this demo call", SnackType.INFO) else camera = !camera
                    }
                    ToggleButton(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, if (muted) "Unmute" else "Mute", muted) { muted = !muted }
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFE5484D)).clickable { end() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.CallEnd, "End call", tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PulsingAvatar(avatar: Int?, name: String, ringing: Boolean, isGroup: Boolean) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800)), label = "p")
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        if (ringing) {
            listOf(0f, 0.5f).forEach { phaseShift ->
                val v = (p + phaseShift) % 1f
                Box(
                    Modifier
                        .size(132.dp)
                        .scale(1f + v * 0.65f)
                        .graphicsLayer { alpha = (1f - v) * 0.5f }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f)),
                )
            }
        }
        Avatar(avatar, name, 132.dp, fallback = if (isGroup) AvatarFallback.GROUP else AvatarFallback.INITIALS)
    }
}

@Composable
private fun ToggleButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) Color.White else Color.White.copy(alpha = 0.16f), label = "tb")
    val fg by animateColorAsState(if (active) Color(0xFF111B21) else Color.White, label = "tf")
    Box(Modifier.size(54.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = fg)
    }
}

@Composable
private fun BigCallButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(68.dp).clip(CircleShape).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}
