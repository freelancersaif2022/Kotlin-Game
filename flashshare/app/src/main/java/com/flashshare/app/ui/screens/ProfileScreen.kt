package com.flashshare.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashshare.app.container
import com.flashshare.app.ui.components.AvatarImpl
import com.flashshare.app.ui.components.AvatarCount
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.flashshare.app.ui.components.GradientButton
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(edit: Boolean, onBack: (() -> Unit)?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.container.prefs
    val settings by prefs.state.collectAsStateWithLifecycle()
    val s = settings ?: return
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var name by rememberSaveable { mutableStateOf(s.name) }
    var avatarId by rememberSaveable { mutableIntStateOf(s.avatarId.coerceIn(0, AvatarCount - 1)) }
    var custom by rememberSaveable { mutableStateOf(s.customAvatar) }
    var customBmp by remember { mutableStateOf<ImageBitmap?>(null) }
    var pendingCustom by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        if (s.customAvatar && prefs.avatarFile.exists()) {
            customBmp = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(prefs.avatarFile.path)?.asImageBitmap() }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val bmp = withContext(Dispatchers.IO) { loadSquare(ctx, uri, 320) }
            if (bmp != null) {
                pendingCustom = bmp
                customBmp = bmp.asImageBitmap()
                custom = true
            } else ctx.toast("Couldn't load that photo")
        }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2F5BFF), Color(0xFF6A3DFF), Color(0xFFB04DFF))))
        ) {
            AmbientBlobs(Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) }
                    else Spacer(Modifier.width(16.dp))
                    Text(if (edit) "Edit profile" else "Create your profile", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
                val bounce = remember { androidx.compose.animation.core.Animatable(1f) }
                LaunchedEffect(avatarId, custom) { bounce.snapTo(0.85f); bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f)) }
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(Modifier.size(136.dp).scale(bounce.value).shadow(20.dp, CircleShape).clip(CircleShape).background(Color.White).padding(5.dp)) {
                        AvatarImpl(avatarId, if (custom) customBmp else null, 126.dp)
                    }
                    Box(
                        Modifier.size(42.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Brand.premium)
                            .border(3.dp, Color.White, CircleShape).clickable { pick() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.PhotoCamera, "Pick photo", tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(12.dp))
                Text(name.ifBlank { "Your device" }, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text("Shown on the other phone when you share", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                label = { Text("Device name") },
                leadingIcon = { Icon(Icons.Rounded.Smartphone, null) },
                trailingIcon = { if (name.isNotEmpty()) IconButton(onClick = { name = "" }) { Icon(Icons.Rounded.Close, "Clear") } },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus() }),
                supportingText = { Text("${name.length}/30") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text("Choose your avatar", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                maxItemsInEachRow = 4,
            ) {
                // Custom photo tile first
                Box(
                    Modifier.size(70.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(if (custom) 3.dp else 1.5.dp, if (custom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { if (customBmp != null) custom = true else pick() },
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = customBmp
                    if (bmp != null) AvatarImpl(0, bmp, 62.dp)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.AddAPhoto, "Custom photo", tint = MaterialTheme.colorScheme.primary)
                        Text("Photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                (0 until AvatarCount).forEach { i ->
                    val selected = !custom && avatarId == i
                    val sc by animateFloatAsState(if (selected) 1.06f else 1f, spring(dampingRatio = 0.5f), label = "s")
                    Box(
                        Modifier.size(70.dp).scale(sc).clip(CircleShape)
                            .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            .clickable { avatarId = i; custom = false }
                            .padding(if (selected) 5.dp else 0.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AvatarImpl(i, null, 70.dp)
                        if (selected) Box(Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        GradientButton(
            text = if (edit) "Save changes" else "Save & continue",
            icon = Icons.Rounded.Check,
            enabled = name.isNotBlank(),
            onClick = {
                scope.launch {
                    val p = pendingCustom
                    if (custom && p != null) {
                        withContext(Dispatchers.IO) {
                            prefs.avatarFile.outputStream().use { p.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                        }
                    }
                    val useCustom = custom && (p != null || prefs.avatarFile.exists())
                    prefs.saveProfile(name, avatarId, useCustom)
                    if (edit) ctx.toast("Profile saved")
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
        )
    }
}

/** Decodes a picked photo, center-crops it to a square and scales it down to [size] px. */
fun loadSquare(ctx: Context, uri: Uri, size: Int): Bitmap? = runCatching {
    val src: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { d, info, _ ->
            val min = minOf(info.size.width, info.size.height)
            val scale = (size * 2f / min).coerceAtMost(1f)
            d.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (minOf(opts.outWidth, opts.outHeight) / (sample * 2) >= size) sample *= 2
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
    }
    val side = minOf(src.width, src.height)
    val cropped = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    Bitmap.createScaledBitmap(cropped, size, size, true)
}.getOrNull()
