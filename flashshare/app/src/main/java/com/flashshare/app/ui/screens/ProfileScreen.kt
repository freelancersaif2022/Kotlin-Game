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
import com.flashshare.app.ui.components.Avatars
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
    var avatarId by rememberSaveable { mutableIntStateOf(s.avatarId) }
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

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            else Spacer(Modifier.width(16.dp))
            Text(if (edit) "Edit profile" else "Set up your profile", style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.BottomEnd) {
                val bounce by animateFloatAsState(1f, spring(dampingRatio = 0.5f), label = "b")
                Box(Modifier.size(132.dp).clip(CircleShape).background(Brand.main).padding(4.dp)) {
                    AvatarImpl(avatarId, if (custom) customBmp else null, 124.dp, Modifier.scale(bounce))
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PhotoCamera, "Pick photo", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Text(name.ifBlank { "Your device" }, style = MaterialTheme.typography.titleLarge)
            Text("Shown to the other phone when you share", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))
            Text("Choose an avatar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 5,
            ) {
                Avatars.indices.forEach { i ->
                    val selected = !custom && avatarId == i
                    val sc by animateFloatAsState(if (selected) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "s")
                    Box(
                        Modifier.scale(sc).size(56.dp).clip(CircleShape)
                            .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { avatarId = i; custom = false }
                            .padding(if (selected) 4.dp else 0.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AvatarImpl(i, null, 52.dp)
                    }
                }
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .border(if (custom) 3.dp else 1.5.dp, if (custom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = customBmp
                    if (bmp != null) AvatarImpl(0, bmp, 48.dp)
                    else Icon(Icons.Rounded.AddAPhoto, "Custom photo", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                label = { Text("Device name") },
                leadingIcon = { Icon(Icons.Rounded.Smartphone, null) },
                trailingIcon = {
                    if (name.isNotEmpty()) IconButton(onClick = { name = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus() }),
                supportingText = { Text("${name.length}/30") },
                modifier = Modifier.fillMaxWidth(),
            )
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
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(24.dp),
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
