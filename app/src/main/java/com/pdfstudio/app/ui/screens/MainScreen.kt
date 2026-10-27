package com.pdfstudio.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfstudio.app.MainViewModel
import com.pdfstudio.app.ui.components.AppLogo
import com.pdfstudio.app.ui.components.ToolButton
import com.pdfstudio.app.ui.components.ToolIcons
import com.pdfstudio.app.ui.theme.Brand

private enum class Sheet { Split, Convert }

@Composable
fun MainScreen(isDark: Boolean, onToggleTheme: () -> Unit, vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri)
    }
    val openPicker = { picker.launch(arrayOf("application/pdf")) }

    // Android 9 and below need the legacy storage permission to save images into Pictures.
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingAction?.invoke()
        pendingAction = null
    }
    fun withStorage(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            pendingAction = action
            permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var cropPage by remember { mutableStateOf<Int?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    val doc = state.doc

    BackHandler(enabled = cropPage != null) { cropPage = null }

    val blur by animateDpAsState(if (state.busy != null) 10.dp else 0.dp, tween(260), label = "blur")
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blur),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopBar(
                    title = doc?.name,
                    pageCount = doc?.pageCount ?: 0,
                    isDark = isDark,
                    showOpen = doc != null,
                    onOpen = openPicker,
                    onToggleTheme = onToggleTheme,
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = doc != null,
                    enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    ActionBar(
                        onSplit = { sheet = Sheet.Split },
                        onCrop = { cropPage = currentPage },
                        onConvert = { sheet = Sheet.Convert },
                    )
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = doc,
                contentKey = { it?.id },
                transitionSpec = {
                    (fadeIn(tween(320, delayMillis = 60)) + scaleIn(tween(380, easing = FastOutSlowInEasing), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(180)))
                },
                label = "document",
            ) { current ->
                if (current == null) {
                    EmptyState(onPick = openPicker, modifier = Modifier.padding(padding))
                } else {
                    PdfPreview(
                        doc = current,
                        cache = vm.cache,
                        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
                        onCurrentPageChange = { currentPage = it },
                    )
                }
            }
        }

        if (doc != null && sheet == Sheet.Split) {
            SplitSheet(
                doc = doc,
                onDismiss = { sheet = null },
                onSplit = vm::split,
            )
        }
        if (doc != null && sheet == Sheet.Convert) {
            ConvertSheet(
                pageCount = doc.pageCount,
                format = state.format,
                onFormatChange = vm::setFormat,
                onDismiss = { sheet = null },
                onConvert = { withStorage { vm.convert() } },
            )
        }

        AnimatedVisibility(
            visible = doc != null && cropPage != null,
            enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300)) { it / 3 } + fadeOut(tween(220)),
        ) {
            val d = doc
            if (d != null) {
                CropScreen(
                    doc = d,
                    cache = vm.cache,
                    startPage = (cropPage ?: 0).coerceIn(0, d.pageCount - 1),
                    onCancel = { cropPage = null },
                    onCrop = { page, rect ->
                        vm.crop(page, rect)
                        cropPage = null
                    },
                )
            }
        }

        ProgressDialog(state.busy)
    }
}

@Composable
private fun TopBar(
    title: String?,
    pageCount: Int,
    isDark: Boolean,
    showOpen: Boolean,
    onOpen: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppLogo(size = 38.dp, elevation = 6.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("PDF Studio", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                AnimatedContent(
                    targetState = title?.let { "$it  ·  $pageCount page${if (pageCount == 1) "" else "s"}" } ?: "Split · Crop · Convert",
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()).togetherWith(slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "subtitle",
                ) { subtitle ->
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            AnimatedVisibility(showOpen, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                IconButton(onClick = onOpen) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = "Open another PDF", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            IconButton(onClick = onToggleTheme) {
                AnimatedContent(
                    targetState = isDark,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()).togetherWith(scaleOut() + fadeOut())
                    },
                    label = "themeIcon",
                ) { dark ->
                    val rotation by animateFloatAsState(if (dark) 0f else 180f, tween(500), label = "rot")
                    Icon(
                        if (dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (dark) "Switch to light mode" else "Switch to dark mode",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "empty")
    val float by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float",
    )
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "ripple",
    )
    val dash by transition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "dash",
    )
    val borderColor = MaterialTheme.colorScheme.primary
    val primary = MaterialTheme.colorScheme.primary

    Box(modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface)
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    drawRoundRect(
                        color = borderColor.copy(alpha = 0.55f),
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(32.dp.toPx()),
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 18f), phase = -dash)),
                    )
                }
                .clickable(onClick = onPick)
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                // Pulsing rings
                repeat(2) { i ->
                    val p = (ripple + i * 0.5f) % 1f
                    Box(
                        Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                scaleX = 0.55f + 0.45f * p
                                scaleY = 0.55f + 0.45f * p
                                alpha = (1f - p) * 0.45f
                            }
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.35f))
                    )
                }
                Box(
                    Modifier
                        .graphicsLayer { translationY = float * density }
                        .size(84.dp)
                        .shadow(18.dp, CircleShape, ambientColor = Brand.Magenta, spotColor = Brand.Coral)
                        .clip(CircleShape)
                        .background(Brand.gradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.NoteAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Upload your PDF", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap to choose a file. Preview every page, then split, crop or convert it instantly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier
                    .shadow(12.dp, RoundedCornerShape(50), ambientColor = Brand.Magenta, spotColor = Brand.Coral)
                    .clip(RoundedCornerShape(50))
                    .background(Brand.gradient)
                    .clickable(onClick = onPick)
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose PDF", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

@Composable
private fun ActionBar(onSplit: () -> Unit, onCrop: () -> Unit, onConvert: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 24.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ToolButton(ToolIcons.Split, "Split", Brand.SplitColors, onSplit, Modifier.weight(1f))
            ToolButton(ToolIcons.Crop, "Crop", Brand.CropColors, onCrop, Modifier.weight(1f))
            ToolButton(ToolIcons.Convert, "Convert", Brand.ConvertColors, onConvert, Modifier.weight(1f))
        }
    }
}
