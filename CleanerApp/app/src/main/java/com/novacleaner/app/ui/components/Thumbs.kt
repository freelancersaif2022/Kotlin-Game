package com.novacleaner.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.novacleaner.app.data.AppIcons
import com.novacleaner.app.data.FileKind
import java.io.File

@Composable
fun AppIcon(packageName: String?, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(packageName?.let { AppIcons.cached(it) }, packageName) {
        if (packageName != null && value == null) value = AppIcons.forPackage(context, packageName)
    }
    IconBitmap(bitmap, modifier, size, Icons.Rounded.Android)
}

@Composable
fun ApkIcon(path: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(AppIcons.cached("apk:$path"), path) {
        if (value == null) value = AppIcons.forApk(context, path)
    }
    IconBitmap(bitmap, modifier, size, Icons.Rounded.Android)
}

@Composable
private fun IconBitmap(bitmap: ImageBitmap?, modifier: Modifier, size: Dp, fallback: androidx.compose.ui.graphics.vector.ImageVector) {
    if (bitmap != null) {
        Image(bitmap, null, modifier.size(size).clip(RoundedCornerShape(size * 0.24f)))
    } else {
        Box(
            modifier.size(size).clip(RoundedCornerShape(size * 0.24f)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(fallback, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(size * 0.5f))
        }
    }
}

fun FileKind.colors(): List<Color> = when (this) {
    FileKind.VIDEO -> listOf(Color(0xFFFF8FB1), Color(0xFFFF4F7B))
    FileKind.IMAGE -> listOf(Color(0xFFB18CFF), Color(0xFF7C4DFF))
    FileKind.AUDIO -> listOf(Color(0xFFF07BE0), Color(0xFFB83CC2))
    FileKind.DOCUMENT -> listOf(Color(0xFFFFC14D), Color(0xFFFF8A3D))
    FileKind.ARCHIVE -> listOf(Color(0xFFFFB86B), Color(0xFFF2672E))
    FileKind.APK -> listOf(Color(0xFF9BE870), Color(0xFF2EA86B))
    FileKind.OTHER -> listOf(Color(0xFF9DB3AC), Color(0xFF5E7871))
}

fun FileKind.icon() = when (this) {
    FileKind.VIDEO -> Icons.Rounded.Movie
    FileKind.IMAGE -> Icons.Rounded.Image
    FileKind.AUDIO -> Icons.Rounded.MusicNote
    FileKind.DOCUMENT -> Icons.Rounded.Description
    FileKind.ARCHIVE -> Icons.Rounded.Archive
    FileKind.APK -> Icons.Rounded.Android
    FileKind.OTHER -> Icons.Rounded.InsertDriveFile
}

/** Real thumbnail for images/videos, app icon for APKs, a glossy type icon otherwise. */
@Composable
fun FileThumb(path: String, kind: FileKind, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    when (kind) {
        FileKind.IMAGE, FileKind.VIDEO -> Box(modifier.size(size), contentAlignment = Alignment.Center) {
            MediaThumb(path, Modifier.size(size), corner = size * 0.26f)
            if (kind == FileKind.VIDEO) {
                Box(
                    Modifier.size(size * 0.42f).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(size * 0.3f))
                }
            }
        }
        FileKind.APK -> ApkIcon(path, modifier, size)
        else -> GlossyIcon(kind.icon(), kind.colors(), modifier, size = size, iconSize = size * 0.46f)
    }
}

@Composable
fun MediaThumb(path: String, modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(File(path)).size(256).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
