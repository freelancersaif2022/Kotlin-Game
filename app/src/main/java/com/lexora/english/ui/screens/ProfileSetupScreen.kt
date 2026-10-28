package com.lexora.english.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lexora.english.data.AppRepository
import com.lexora.english.data.UserProfile
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.Avatars
import com.lexora.english.ui.components.BrandGradient
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.PillSelector
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.components.ScreenHeader
import com.lexora.english.ui.components.SecondaryButton
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.ProfilePhoto
import com.lexora.english.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val levels = listOf("Beginner", "Intermediate", "Advanced")
private val goals = listOf("Career", "Travel", "Study", "Exams", "Business", "Personal")
private val dailyTargets = listOf(5, 10, 15, 20, 30)

@Composable
fun ProfileSetupScreen(existing: UserProfile?, onSaved: () -> Unit, onBack: (() -> Unit)?) {
    val c = Lex.colors
    val context = LocalContext.current
    val repo = AppRepository.get(context)
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var avatarIndex by rememberSaveable { mutableIntStateOf(existing?.avatarIndex ?: 0) }
    var photoPath by rememberSaveable { mutableStateOf(existing?.photoPath) }
    var level by rememberSaveable { mutableStateOf(existing?.level ?: levels[0]) }
    var goal by rememberSaveable { mutableStateOf(existing?.goal ?: goals[0]) }
    var daily by rememberSaveable { mutableIntStateOf(existing?.dailyMinutes ?: 15) }
    var nameError by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<Uri?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }

    fun usePhoto(uri: Uri) {
        scope.launch {
            val path = withContext(Dispatchers.IO) { ProfilePhoto.save(context, uri, photoPath.takeIf { it != existing?.photoPath }) }
            if (path != null) {
                photoPath = path
                avatarIndex = -1
                photoError = null
                haptics.success()
            } else {
                photoError = "Couldn't read that photo. Please try another one."
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCapture
        if (ok && uri != null) usePhoto(uri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) usePhoto(uri)
    }

    fun save() {
        val trimmed = name.trim().replace(Regex("\\s+"), " ")
        if (trimmed.length < 2) {
            nameError = true
            haptics.error()
            return
        }
        val profile = UserProfile(
            name = trimmed.split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercaseChar() } },
            avatarIndex = avatarIndex,
            photoPath = if (avatarIndex < 0) photoPath else null,
            level = level,
            goal = goal,
            dailyMinutes = daily,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        if (avatarIndex >= 0 && existing?.photoPath != null) File(existing.photoPath).delete()
        repo.saveProfile(profile)
        haptics.success()
        onSaved()
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        repo.setNotificationPermissionAsked()
        save()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .imePadding()
    ) {
        if (onBack != null) {
            ScreenHeader("Edit profile", onBack)
        } else {
            ScreenHeader("Set up your profile", null, subtitle = "Step 1 of 1 · Takes 30 seconds")
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            // Preview
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box {
                    Box(
                        Modifier
                            .size(124.dp)
                            .border(BorderStroke(3.dp, BrandGradient), CircleShape)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(c.primarySoft),
                    ) {
                        val model: Any = if (avatarIndex < 0 && photoPath != null) File(photoPath!!) else Avatars[avatarIndex.coerceAtLeast(0)]
                        AppImage(model, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = "Selected avatar")
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BrandGradient)
                            .border(3.dp, c.background, CircleShape)
                            .clickable {
                                pendingCapture = ProfilePhoto.newCaptureUri(context)
                                try {
                                    camera.launch(pendingCapture!!)
                                } catch (e: Exception) {
                                    photoError = "No camera app found on this phone."
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, "Take photo", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("Choose your avatar", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(12.dp))
            for (row in 0 until 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until 5) {
                        val i = row * 5 + col
                        val selected = avatarIndex == i
                        val border by animateColorAsState(if (selected) c.primary else Color.Transparent, label = "av")
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .border(3.dp, border, CircleShape)
                                .padding(if (selected) 4.dp else 0.dp)
                                .clip(CircleShape)
                                .clickable {
                                    avatarIndex = i
                                    haptics.tick()
                                },
                        ) {
                            AppImage(Avatars[i], Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = "Avatar ${i + 1}")
                            if (selected) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(c.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    "Camera",
                    onClick = {
                        pendingCapture = ProfilePhoto.newCaptureUri(context)
                        try {
                            camera.launch(pendingCapture!!)
                        } catch (e: Exception) {
                            photoError = "No camera app found on this phone."
                        }
                    },
                    icon = Icons.Rounded.PhotoCamera,
                    modifier = Modifier.weight(1f),
                    height = 48.dp,
                )
                SecondaryButton(
                    "Gallery",
                    onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    icon = Icons.Rounded.PhotoLibrary,
                    modifier = Modifier.weight(1f),
                    height = 48.dp,
                )
            }
            if (photoError != null) {
                Spacer(Modifier.height(6.dp))
                Text(photoError!!, style = MaterialTheme.typography.bodySmall, color = c.error)
            }

            Spacer(Modifier.height(24.dp))
            Text("Your name", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 32) { name = it; nameError = false } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your full name", color = c.textMuted) },
                leadingIcon = { Icon(Icons.Rounded.Person, null, tint = c.textMuted) },
                singleLine = true,
                isError = nameError,
                supportingText = {
                    Text(
                        if (nameError) "Please enter at least 2 letters" else "This name will appear on your certificate",
                        color = if (nameError) c.error else c.textMuted,
                    )
                },
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.text),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.primary,
                    unfocusedBorderColor = c.border,
                    focusedContainerColor = c.card,
                    unfocusedContainerColor = c.cardAlt,
                    cursorColor = c.primary,
                ),
            )

            Spacer(Modifier.height(16.dp))
            Text("Your English level", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(8.dp))
            PillSelector(levels, level, { it }, { level = it; haptics.tick() })

            Spacer(Modifier.height(20.dp))
            Text("Why are you learning?", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                goals.forEach { g ->
                    val sel = g == goal
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) c.primary else c.cardAlt)
                            .border(1.dp, if (sel) c.primary else c.border, RoundedCornerShape(12.dp))
                            .clickable { goal = g; haptics.tick() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(g, style = MaterialTheme.typography.labelLarge, color = if (sel) Color.White else c.textSecondary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Daily learning goal", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(8.dp))
            PillSelector(dailyTargets, daily, { "$it min" }, { daily = it; haptics.tick() })

            Spacer(Modifier.height(20.dp))
            LexCard(color = c.primarySoft, elevated = false) {
                Text(
                    "🔒  Your profile and progress are saved privately on this phone. No account, no ads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            text = if (existing == null) "Start learning" else "Save changes",
            onClick = {
                val needsPermission = existing == null && Build.VERSION.SDK_INT >= 33 && !repo.current.notificationPermissionAsked
                if (needsPermission && name.trim().length >= 2) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    save()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
