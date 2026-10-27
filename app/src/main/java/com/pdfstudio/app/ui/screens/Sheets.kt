package com.pdfstudio.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pdfstudio.app.DocState
import com.pdfstudio.app.ImageFormat
import com.pdfstudio.app.data.MediaSaver
import com.pdfstudio.app.data.PageBitmapCache
import com.pdfstudio.app.data.PageRanges
import com.pdfstudio.app.ui.components.PageImage
import com.pdfstudio.app.ui.components.rememberPageBitmap
import com.pdfstudio.app.ui.components.ToolIcons
import com.pdfstudio.app.ui.theme.Brand
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitSheet(
    doc: DocState,
    cache: PageBitmapCache,
    onDismiss: () -> Unit,
    onSplit: (List<Int>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(emptySet<Int>()) }

    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            then()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            SheetHeader(
                ToolIcons.Split,
                Brand.SplitColors,
                "Split PDF",
                if (selected.isEmpty()) "Tap the pages to keep in a new PDF"
                else "${selected.size} of ${doc.pageCount} page${if (doc.pageCount == 1) "" else "s"} selected",
            )
            Spacer(Modifier.height(18.dp))

            val thumbWidth = with(LocalDensity.current) { 120.dp.roundToPx() }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(84.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp),
            ) {
                items(List(doc.pageCount) { it }, key = { it }) { index ->
                    PageTile(
                        doc = doc,
                        cache = cache,
                        index = index,
                        thumbWidth = thumbWidth,
                        selected = index in selected,
                        onClick = { selected = if (index in selected) selected - index else selected + index },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SheetButtons(
                confirmText = "Split",
                confirmIcon = ToolIcons.Split,
                confirmEnabled = selected.isNotEmpty(),
                onCancel = { close() },
                onConfirm = {
                    val pages = selected.sorted()
                    close { onSplit(pages) }
                },
            )
        }
    }
}

@Composable
private fun PageTile(
    doc: DocState,
    cache: PageBitmapCache,
    index: Int,
    thumbWidth: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderWidth by animateDpAsState(if (selected) 2.5.dp else 1.dp, label = "border")
    val borderColor by animateColorAsState(if (selected) primary else MaterialTheme.colorScheme.outlineVariant, label = "borderColor")
    val scale by animateFloatAsState(if (selected) 0.94f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "scale")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .scale(scale)
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            val size = doc.pageSizes[index]
            val bitmap = rememberPageBitmap(doc, cache, index, thumbWidth)
            PageImage(
                bitmap = bitmap,
                modifier = Modifier
                    .aspectRatio(size.aspect, matchHeightConstraintsFirst = size.aspect < 0.75f)
                    .clip(RoundedCornerShape(3.dp)),
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = selected,
                    enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertSheet(
    pageCount: Int,
    format: ImageFormat,
    onFormatChange: (ImageFormat) -> Unit,
    onDismiss: () -> Unit,
    onConvert: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            then()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            SheetHeader(ToolIcons.Convert, Brand.ConvertColors, "Convert to image", "Export every page as a high-quality image")
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormatCard(ImageFormat.JPG, "Smaller files", Icons.Rounded.Photo, format == ImageFormat.JPG, { onFormatChange(ImageFormat.JPG) }, Modifier.weight(1f))
                FormatCard(ImageFormat.PNG, "Lossless, sharpest", Icons.Rounded.Wallpaper, format == ImageFormat.PNG, { onFormatChange(ImageFormat.PNG) }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            SheetButtons(
                confirmText = "Convert",
                confirmIcon = ToolIcons.Convert,
                confirmEnabled = true,
                onCancel = { close() },
                onConfirm = { close(onConvert) },
            )
        }
    }
}

@Composable
private fun FormatCard(
    format: ImageFormat,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val border by animateColorAsState(if (selected) primary else MaterialTheme.colorScheme.outlineVariant, label = "border")
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        label = "container",
    )
    val scale by animateFloatAsState(if (selected) 1f else 0.97f, spring(dampingRatio = 0.55f), label = "scale")
    Box(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(Brand.ConvertColors)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(format.label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopEnd) {
            AnimatedVisibility(selected, enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(icon: ImageVector, colors: List<Color>, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(46.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = colors.last(), spotColor = colors.last())
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SheetButtons(
    confirmText: String,
    confirmIcon: ImageVector,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = "Cancel",
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(cancelText, style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(confirmIcon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(confirmText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
