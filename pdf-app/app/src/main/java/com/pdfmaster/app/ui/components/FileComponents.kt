package com.pdfmaster.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pdfmaster.app.data.model.PdfFile
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand
import com.pdfmaster.app.ui.tools.PdfTool
import com.pdfmaster.app.util.Format

@Composable
fun PdfThumbnail(uri: Uri, modifier: Modifier = Modifier, width: Dp = 48.dp, height: Dp = 60.dp, locked: Boolean = false) {
    val loader = LocalAppDeps.current.thumbnails
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(loader.peek(uri)) }
    var failed by remember(uri) { mutableStateOf(false) }
    LaunchedEffect(uri) {
        if (bitmap == null) {
            bitmap = loader.load(uri)
            failed = bitmap == null
        }
    }
    Box(
        modifier
            .size(width, height)
            .clip(RoundedCornerShape(10.dp))
            .background(if (bitmap != null) Color.White else Brand.RedSoft)
            .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(bitmap, label = "thumb") { bmp ->
            if (bmp != null) {
                Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (failed && locked) {
                        Icon(Icons.Rounded.Lock, null, tint = Brand.Red, modifier = Modifier.size(20.dp))
                    } else {
                        Text("PDF", color = Brand.Red, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun FileRow(
    file: PdfFile,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    onFavorite: (() -> Unit)? = null,
    highlight: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(18.dp))
            .bounceClick(scaleDown = 0.985f, onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PdfThumbnail(file.uri, locked = file.pageCount < 0)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                highlightText(file.name, highlight),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            val parts = buildList {
                if (file.size > 0) add(Format.size(file.size))
                if (file.pageCount != 0) add(Format.pages(file.pageCount))
                Format.relativeDate(file.date).takeIf { it.isNotEmpty() }?.let { add(it) }
            }
            Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (onFavorite != null) {
            val scale by animateFloatAsState(if (file.isFavorite) 1.1f else 1f, label = "fav")
            IconButton(onClick = onFavorite) {
                Icon(
                    if (file.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Favourite",
                    tint = if (file.isFavorite) Brand.Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp * scale),
                )
            }
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun highlightText(text: String, query: String?): androidx.compose.ui.text.AnnotatedString {
    val color = MaterialTheme.colorScheme.primary
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        if (!query.isNullOrBlank()) {
            val i = text.indexOf(query, ignoreCase = true)
            if (i >= 0) addStyle(androidx.compose.ui.text.SpanStyle(color = color, fontWeight = FontWeight.Bold), i, i + query.length)
        }
    }
}

@Composable
fun FileRowPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Shimmer(Modifier.size(48.dp, 60.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Shimmer(Modifier.fillMaxWidth(0.7f).height(14.dp))
            Spacer(Modifier.height(8.dp))
            Shimmer(Modifier.fillMaxWidth(0.45f).height(10.dp))
        }
    }
}

data class FileSheetCallbacks(
    val onOpen: () -> Unit,
    val onShare: () -> Unit,
    val onFavorite: () -> Unit,
    val onPrint: () -> Unit,
    val onTool: (PdfTool) -> Unit,
    val onRename: (() -> Unit)? = null,
    val onRemoveRecent: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
)

@Composable
fun FileActionsSheet(file: PdfFile, onDismiss: () -> Unit, callbacks: FileSheetCallbacks) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                PdfThumbnail(file.uri, width = 52.dp, height = 66.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = listOfNotNull(
                        file.size.takeIf { it > 0 }?.let(Format::size),
                        file.pageCount.takeIf { it != 0 }?.let(Format::pages),
                    ).joinToString("  ·  ")
                    if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf(PdfTool.COMPRESS, PdfTool.SIGN, PdfTool.ANNOTATE, PdfTool.SPLIT, PdfTool.WATERMARK, PdfTool.LOCK, PdfTool.PDF_TO_IMAGES)) { tool ->
                    Column(
                        Modifier.width(76.dp).clip(RoundedCornerShape(14.dp)).clickable { callbacks.onTool(tool) }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        IconCircle(tool.icon, tool.color, size = 46.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(tool.shortTitle, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SheetAction(Icons.AutoMirrored.Rounded.OpenInNew, "Open", callbacks.onOpen)
            SheetAction(Icons.Rounded.Share, "Share", callbacks.onShare)
            SheetAction(
                if (file.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                if (file.isFavorite) "Remove from favourites" else "Add to favourites",
                callbacks.onFavorite,
                tint = if (file.isFavorite) Brand.Gold else null,
            )
            SheetAction(Icons.Rounded.Print, "Print", callbacks.onPrint)
            callbacks.onRename?.let { SheetAction(Icons.Rounded.DriveFileRenameOutline, "Rename", it) }
            callbacks.onRemoveRecent?.let { SheetAction(Icons.Rounded.RemoveCircleOutline, "Remove from recent", it) }
            callbacks.onDelete?.let { SheetAction(Icons.Rounded.Delete, "Delete", it, tint = MaterialTheme.colorScheme.error, textColor = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color? = null, textColor: Color? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = textColor ?: MaterialTheme.colorScheme.onSurface)
    }
}
