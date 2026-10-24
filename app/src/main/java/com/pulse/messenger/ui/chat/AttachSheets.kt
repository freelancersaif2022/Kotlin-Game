package com.pulse.messenger.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ShareLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulse.messenger.R
import com.pulse.messenger.data.Contact
import com.pulse.messenger.data.DeviceDocument
import com.pulse.messenger.data.MessageContent
import com.pulse.messenger.data.MockData
import com.pulse.messenger.data.PollOption
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.PulseBottomSheet
import com.pulse.messenger.ui.components.PulseSwitch
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format

enum class AttachType(val label: String, val icon: ImageVector, val color: Color) {
    GALLERY("Gallery", Icons.Filled.Photo, Color(0xFF3B82F6)),
    CAMERA("Camera", Icons.Filled.PhotoCamera, Color(0xFFE0558C)),
    LOCATION("Location", Icons.Filled.LocationOn, Color(0xFF1DAA61)),
    CONTACT("Contact", Icons.Filled.Person, Color(0xFF2F9BD6)),
    DOCUMENT("Document", Icons.Filled.InsertDriveFile, Color(0xFF7F66FF)),
    AUDIO("Audio", Icons.Filled.Headphones, Color(0xFFE5793B)),
    POLL("Poll", Icons.Filled.Poll, Color(0xFF14A38B)),
}

@Composable
fun AttachSheet(onDismiss: () -> Unit, onPick: (AttachType) -> Unit) {
    PulseBottomSheet(onDismiss) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.height(230.dp),
        ) {
            items(AttachType.entries) { t ->
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).clickable { onDismiss(); onPick(t) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(t.color),
                        contentAlignment = Alignment.Center,
                    ) { Icon(t.icon, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.height(8.dp))
                    Text(t.label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = PulseTheme.colors.textSecondary)
                }
            }
        }
    }
}

private data class GalleryItem(val res: Int, val videoSec: Int?)

private val galleryItems: List<GalleryItem> =
    MockData.photos.map { GalleryItem(it, null) }.take(12) +
        MockData.videoThumbs.mapIndexed { i, v -> GalleryItem(v, 12 + i * 17) }.take(4) +
        MockData.photos.drop(12).map { GalleryItem(it, null) } +
        MockData.videoThumbs.drop(4).mapIndexed { i, v -> GalleryItem(v, 20 + i * 23) }

@Composable
fun GalleryPickerSheet(onDismiss: () -> Unit, onSend: (List<MessageContent>) -> Unit) {
    val picked = remember { mutableStateListOf<GalleryItem>() }
    var caption by remember { mutableStateOf("") }
    PulseBottomSheet(onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Recents", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (picked.isNotEmpty()) Text("${picked.size} selected", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))
        Box {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(430.dp),
                contentPadding = PaddingValues(start = 2.dp, end = 2.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(galleryItems, key = { it.res }) { g ->
                    val index = picked.indexOf(g)
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clickable { if (index >= 0) picked.remove(g) else if (picked.size < 30) picked.add(g) },
                    ) {
                        AsyncImage(model = g.res, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        if (index >= 0) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        if (g.videoSec != null) {
                            Row(
                                Modifier.align(Alignment.BottomStart).padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(Format.duration(g.videoSec), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (index >= 0) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (index >= 0) Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            AnimatedVisibility(
                picked.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(PulseTheme.colors.chip)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        BasicTextField(
                            caption, { caption = it }, singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (caption.isEmpty()) Text("Add a caption…", color = PulseTheme.colors.textSecondary)
                                inner()
                            },
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    FloatingActionButton(
                        onClick = {
                            val items = picked.mapIndexed { i, g ->
                                val cap = if (i == picked.lastIndex) caption.trim() else ""
                                if (g.videoSec != null) MessageContent.Video(g.res, g.videoSec, cap) else MessageContent.Image(g.res, cap)
                            }
                            onDismiss()
                            onSend(items)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                }
            }
        }
    }
}

@Composable
fun DocumentPickerSheet(onDismiss: () -> Unit, onPick: (DeviceDocument) -> Unit) {
    PulseBottomSheet(onDismiss) {
        Text("Choose a document", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp))
        Text("Recent files", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        LazyColumn(Modifier.height(420.dp)) {
            items(MockData.deviceDocuments, key = { it.name }) { d ->
                Row(
                    Modifier.fillMaxWidth().clickable { onDismiss(); onPick(d) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = when (d.extension) {
                        "PDF" -> Color(0xFFE5484D); "DOCX" -> Color(0xFF2F6FDE); "XLSX" -> Color(0xFF1DAA61)
                        "PPTX" -> Color(0xFFE5793B); else -> Color(0xFF667781)
                    }
                    Box(
                        Modifier.size(width = 40.dp, height = 48.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(d.extension, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOfNotNull(d.sizeLabel, if (d.pages > 0) "${d.pages} pages" else null, d.extension).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

private val audioFiles = listOf(
    "Voice memo – meeting notes" to 184, "Morning walk ambience" to 96,
    "Guitar idea #3" to 52, "Lecture recording – Week 6" to 2650, "Song demo (rough mix)" to 213,
)

@Composable
fun AudioPickerSheet(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    PulseBottomSheet(onDismiss) {
        Text("Send audio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        audioFiles.forEach { (name, sec) ->
            Row(
                Modifier.fillMaxWidth().clickable { onDismiss(); onPick(sec) }.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFE5793B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.MusicNote, null, tint = Color(0xFFE5793B)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    Text(Format.duration(sec), style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun LocationSheet(onDismiss: () -> Unit, onSend: (MessageContent.Location) -> Unit) {
    PulseBottomSheet(onDismiss) {
        Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp))) {
            AsyncImage(model = R.drawable.media_map_preview_03, contentDescription = "Map", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.Center).size(22.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF2F9BD6))) }
            Text(
                "Accurate to 8 metres",
                color = Color.White, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                    .clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        LocationRow(Icons.Outlined.ShareLocation, "Share live location", "Updated in real time as you move", MaterialTheme.colorScheme.primary) {
            onDismiss(); onSend(MessageContent.Location(R.drawable.media_map_preview_01, "Live location", "Sharing for 1 hour", isLive = true))
        }
        LocationRow(Icons.Outlined.MyLocation, "Send your current location", "Accurate to 8 metres", Color(0xFF2F9BD6)) {
            onDismiss(); onSend(MessageContent.Location(R.drawable.media_map_preview_03, "Current location", "Near Central Park Café, 12 Garden Road"))
        }
        Text("Nearby places", style = MaterialTheme.typography.labelLarge, color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp))
        MockData.nearbyPlaces.forEachIndexed { i, p ->
            LocationRow(Icons.Outlined.Place, p.name, "${p.address} · ${p.distance}", PulseTheme.colors.textSecondary) {
                onDismiss(); onSend(MessageContent.Location(listOf(R.drawable.media_map_preview_02, R.drawable.media_map_preview_04)[i % 2], p.name, p.address))
            }
        }
    }
}

@Composable
private fun LocationRow(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
        }
    }
}

@Composable
fun ContactPickerSheet(contacts: List<Contact>, onDismiss: () -> Unit, onPick: (Contact) -> Unit, title: String = "Share contact") {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, contacts) {
        contacts.filter { it.name.contains(query, true) || it.phone.contains(query) }.sortedBy { it.name }
    }
    PulseBottomSheet(onDismiss) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(PulseTheme.colors.chip)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                query, { query = it }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search name or number", color = PulseTheme.colors.textSecondary)
                    inner()
                },
            )
        }
        LazyColumn(Modifier.height(440.dp)) {
            items(filtered, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { onDismiss(); onPick(c) }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(c.avatar, c.name, 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                        Text(c.about, style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun PollSheet(onDismiss: () -> Unit, onCreate: (MessageContent.Poll) -> Unit) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var multiple by remember { mutableStateOf(true) }
    val valid = question.isNotBlank() && options.count { it.isNotBlank() } >= 2
    PulseBottomSheet(onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
            Text("Create poll", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        LazyColumn(Modifier.height(460.dp).imePadding(), contentPadding = PaddingValues(horizontal = 20.dp)) {
            item {
                Text("Question", style = MaterialTheme.typography.labelLarge, color = PulseTheme.colors.textSecondary)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    question, { if (it.length <= 255) question = it },
                    placeholder = { Text("Ask question") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Options", style = MaterialTheme.typography.labelLarge, color = PulseTheme.colors.textSecondary)
                Spacer(Modifier.height(6.dp))
            }
            itemsIndexed(options) { i, o ->
                OutlinedTextField(
                    o, { options[i] = it.take(100) },
                    placeholder = { Text("Option ${i + 1}") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = if (options.size > 2) ({
                        IconButton(onClick = { options.removeAt(i) }) { Icon(Icons.Filled.Close, "Remove option", tint = PulseTheme.colors.textSecondary) }
                    }) else null,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            item {
                if (options.size < 12) {
                    TextButton(onClick = { options.add("") }) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add option")
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Allow multiple answers", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    PulseSwitch(multiple) { multiple = it }
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    "Send poll",
                    enabled = valid,
                    onClick = {
                        onDismiss()
                        onCreate(MessageContent.Poll(question.trim(), options.filter { it.isNotBlank() }.map { PollOption(it.trim()) }, multiple))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
