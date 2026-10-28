package com.inkly.notes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inkly.notes.AppContainer
import com.inkly.notes.ui.components.ConfirmDialog
import com.inkly.notes.ui.components.formatRelative
import com.inkly.notes.ui.theme.NoteColors
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(container: AppContainer, noteId: Long, onBack: () -> Unit) {
    val vm: EditorViewModel = viewModel(
        key = "editor_$noteId",
        factory = viewModelFactory { initializer { EditorViewModel(container.repo, container.appScope, noteId) } },
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) vm.flush() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler { vm.flush(); onBack() }

    val bodyFocus = remember { FocusRequester() }
    var showColors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val page by animateColorAsState(NoteColors.page(vm.color), label = "page")
    val box by animateColorAsState(
        if (vm.color == 0) MaterialTheme.colorScheme.surfaceContainerLowest
        else MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.55f),
        label = "box",
    )

    LaunchedEffect(vm.loaded) {
        if (vm.isNew && vm.loaded) {
            delay(280)
            bodyFocus.requestFocus()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(page)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.flush(); onBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            SaveIndicator(vm.saveState)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = vm::togglePin) {
                AnimatedContent(vm.pinned, transitionSpec = { scaleIn() togetherWith fadeOut() }, label = "pin") { p ->
                    Icon(
                        if (p) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                        if (p) "Unpin" else "Pin",
                        tint = if (p) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            IconButton(onClick = { showColors = !showColors }) {
                Icon(Icons.Rounded.Palette, "Colour", tint = if (showColors) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Rounded.DeleteOutline, "Delete")
            }
        }

        AnimatedVisibility(showColors, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            ColorRow(selected = vm.color, onSelect = vm::onColorChange)
        }

        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Box {
                if (vm.title.isEmpty()) {
                    Text("Title", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                BasicTextField(
                    value = vm.title,
                    onValueChange = { v ->
                        if (v.contains('\n')) {
                            vm.onTitleChange(v.replace("\n", ""))
                            bodyFocus.requestFocus()
                        } else vm.onTitleChange(v)
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(6.dp))
            val words = remember(vm.content) { vm.content.split(Regex("\\s+")).count { it.isNotBlank() } }
            Text(
                "${formatRelative(vm.updatedAt)}  •  $words ${if (words == 1) "word" else "words"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            // The lined writing box: grows downward line by line as you type.
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = box,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        bodyFocus.requestFocus()
                    },
            ) {
                RuledTextField(
                    value = vm.content,
                    onValueChange = vm::onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .focusRequester(bodyFocus),
                )
            }
        }
    }

    ConfirmDialog(
        visible = confirmDelete,
        icon = Icons.Rounded.DeleteOutline,
        title = "Delete this note?",
        message = "This note will be permanently removed from this device.",
        confirmText = "Delete",
        destructive = true,
        onConfirm = { confirmDelete = false; vm.delete(onBack) },
        onDismiss = { confirmDelete = false },
    )
}

@Composable
private fun SaveIndicator(state: SaveState) {
    AnimatedContent(
        state,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() },
        label = "save",
    ) { s ->
        when (s) {
            SaveState.Idle -> Spacer(Modifier.width(1.dp))
            SaveState.Saving -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                Spacer(Modifier.width(6.dp))
                Text("Saving…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SaveState.Saved -> Row(
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CloudDone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Saved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ColorRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(NoteColors.count) { i ->
            val isSel = i == selected
            val ring by animateDpAsState(if (isSel) 2.5.dp else 1.dp, spring(), label = "ring")
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NoteColors.swatches[i])
                    .border(ring, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(isSel, enter = scaleIn() + fadeIn(), exit = fadeOut()) {
                    Icon(Icons.Rounded.Check, null, tint = Color(0xFF1E1B4B), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Notebook-style text field: a dashed rule is drawn under every line. Pressing Enter adds a line,
 * the field grows and the dashed rules move down with it.
 */
@Composable
fun RuledTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, minLines: Int = 10) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val lineHeight = 30.sp
    val lineHeightPx = with(density) { lineHeight.toPx() }
    val ruleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val style = TextStyle(
        fontSize = 17.sp,
        lineHeight = lineHeight,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Box(modifier) {
        if (value.isEmpty()) {
            Text("Start writing…", style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            minLines = minLines,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()), 0f)
                    val stroke = 1.2.dp.toPx()
                    val total = maxOf((size.height / lineHeightPx).toInt(), layout?.lineCount ?: 0, minLines)
                    for (i in 0 until total) {
                        val y = (i + 1) * lineHeightPx - stroke
                        drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), stroke, pathEffect = dash)
                    }
                },
        )
    }
}
