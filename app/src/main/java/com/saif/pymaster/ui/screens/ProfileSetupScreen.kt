package com.saif.pymaster.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.saif.pymaster.R
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Profile
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.PrimaryButton
import com.saif.pymaster.ui.components.ScreenTopBar
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.util.ImageUtils
import com.saif.pymaster.util.LocalHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val levels = listOf("Beginner", "Intermediate", "Advanced")
private val goals = listOf("Get a developer job", "Data science & AI", "Automation", "School / College", "Just for fun")
private val dailyGoals = listOf(10, 20, 30, 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(initial: Profile?, onSave: (Profile) -> Unit, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalAppColors.current
    val haptics = LocalHaptics.current
    val isNew = initial == null

    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var avatarIndex by rememberSaveable { mutableIntStateOf(initial?.avatarIndex ?: 0) }
    var customPath by rememberSaveable { mutableStateOf(initial?.customAvatarPath) }
    var level by rememberSaveable { mutableStateOf(initial?.level ?: levels[0]) }
    var goal by rememberSaveable { mutableStateOf(initial?.goal ?: goals[0]) }
    var dailyGoal by rememberSaveable { mutableIntStateOf(initial?.dailyGoalMinutes ?: 20) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf(false) }

    fun importImage(uri: Uri) {
        scope.launch {
            val path = withContext(Dispatchers.IO) { ImageUtils.saveAvatar(context, uri) }
            if (path != null) {
                customPath = path
                avatarIndex = -1
                haptics?.success()
            } else {
                Toast.makeText(context, "Could not load that photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) importImage(Uri.parse(uri))
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importImage(uri)
    }

    fun buildProfile() = Profile(
        name = name.trim(),
        avatarIndex = avatarIndex,
        customAvatarPath = if (avatarIndex < 0) customPath else null,
        level = level,
        goal = goal,
        dailyGoalMinutes = dailyGoal,
        createdAt = initial?.createdAt ?: 0L,
    )

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onSave(buildProfile())
    }

    fun submit() {
        if (name.isBlank()) {
            nameError = true
            haptics?.error()
            return
        }
        if (isNew && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onSave(buildProfile())
        }
    }

    val customBitmap = remember(customPath) { ImageUtils.loadBitmap(customPath)?.asImageBitmap() }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        ScreenTopBar(
            title = if (isNew) "Set up your profile" else "Edit profile",
            subtitle = if (isNew) "Saved privately on this phone" else null,
            onBack = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Large preview
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(132.dp)
                        .border(3.dp, Brush.linearGradient(listOf(Brand.Cyan, Brand.PythonYellow)), CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceHigh)
                ) {
                    if (avatarIndex < 0 && customBitmap != null) {
                        Image(customBitmap, "Your photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Image(
                            painterResource(Content.avatars[avatarIndex.coerceIn(0, 4)]), "Avatar",
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                        )
                    }
                }
                Box(
                    Modifier
                        .padding(start = 96.dp, top = 96.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brand.PythonBlue)
                        .border(3.dp, colors.background, CircleShape)
                        .clickable {
                            val file = ImageUtils.newCameraFile(context)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraUri = uri.toString()
                            runCatching { takePicture.launch(uri) }.onFailure {
                                Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.CameraAlt, "Take photo", tint = Brand.White, modifier = Modifier.size(18.dp)) }
            }

            Spacer(Modifier.height(24.dp))
            Label("Choose your avatar")
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val choiceSize = ((maxWidth - 40.dp) / 6).coerceAtMost(58.dp)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Content.avatars.forEachIndexed { i, res ->
                        AvatarChoice(selected = avatarIndex == i, size = choiceSize, onClick = { avatarIndex = i; haptics?.tick() }) {
                            Image(painterResource(res), "Avatar ${i + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    AvatarChoice(selected = avatarIndex < 0, size = choiceSize, onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Box(Modifier.fillMaxSize().background(colors.surfaceHigh), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PhotoLibrary, "Gallery", tint = Brand.PythonYellow)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    "Pick an avatar or your own photo from the gallery",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.TextMuted,
                )
            }

            Spacer(Modifier.height(22.dp))
            Label("Your name")
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 32) name = it; nameError = false },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. Saif Ahmed", color = Brand.TextMuted) },
                leadingIcon = { Icon(Icons.Rounded.Person, null, tint = Brand.TextSecondary) },
                isError = nameError,
                supportingText = {
                    Text(
                        if (nameError) "Please enter your name — it appears on your certificate" else "This name will be printed on your certificate",
                        color = if (nameError) Brand.Danger else Brand.TextMuted,
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Brand.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand.Cyan,
                    unfocusedBorderColor = colors.stroke,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    cursorColor = Brand.Cyan,
                ),
            )

            Spacer(Modifier.height(14.dp))
            Label("Experience level")
            ChoiceRow(levels, level) { level = it; haptics?.tick() }

            Spacer(Modifier.height(20.dp))
            Label("Your goal")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { g -> Choice(g, g == goal) { goal = g; haptics?.tick() } }
            }

            Spacer(Modifier.height(20.dp))
            Label("Daily study goal")
            ChoiceRow(dailyGoals.map { "$it min" }, "$dailyGoal min") { dailyGoal = it.removeSuffix(" min").toInt(); haptics?.tick() }

            if (isNew) {
                Spacer(Modifier.height(20.dp))
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.profile_setup), null, Modifier.size(64.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Everything you enter stays on your phone. No account, no ads, no premium — just learning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.TextSecondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            text = if (isNew) "Create my profile" else "Save changes",
            icon = Icons.Rounded.Check,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) { submit() }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = Brand.White, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun AvatarChoice(selected: Boolean, size: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(size)
                .border(
                    2.dp,
                    if (selected) Brush.linearGradient(listOf(Brand.Cyan, Brand.PythonYellow)) else Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x22FFFFFF))),
                    CircleShape,
                )
                .padding(3.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) { content() }
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle, null, tint = Brand.Success,
                modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Black),
            )
        }
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> Choice(o, o == selected, Modifier.weight(1f), compact = true) { onSelect(o) } }
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, modifier: Modifier = Modifier, compact: Boolean = false, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Brand.PythonBlue.copy(alpha = 0.35f) else colors.surface)
            .border(1.dp, if (selected) Brand.Cyan else colors.stroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 4.dp else 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (selected) Brand.White else Brand.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
