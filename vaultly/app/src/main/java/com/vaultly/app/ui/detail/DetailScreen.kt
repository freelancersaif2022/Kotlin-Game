package com.vaultly.app.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.vaultly.app.VaultlyApp
import com.vaultly.app.data.Item
import com.vaultly.app.data.ItemStatus
import com.vaultly.app.domain.Deadline
import com.vaultly.app.domain.DeadlineKind
import com.vaultly.app.domain.Deadlines
import com.vaultly.app.domain.Urgency
import com.vaultly.app.ui.components.CategoryBadge
import com.vaultly.app.ui.components.CountdownRing
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.components.ProBadge
import com.vaultly.app.ui.components.SecondaryButton
import com.vaultly.app.ui.components.SectionHeader
import com.vaultly.app.ui.components.StatusPill
import com.vaultly.app.ui.components.VaultCard
import com.vaultly.app.ui.components.daysColor
import com.vaultly.app.ui.components.findActivity
import com.vaultly.app.ui.components.icon
import com.vaultly.app.ui.components.urgencyColor
import com.vaultly.app.ui.nav.Routes
import com.vaultly.app.ui.nav.openPaywall
import com.vaultly.app.ui.theme.Palette
import com.vaultly.app.ui.theme.VaultlyTheme
import com.vaultly.app.util.Format
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DetailScreen(app: VaultlyApp, id: Long, nav: NavHostController) {
    val flow = remember(id) { app.repository.observe(id) }
    val item by flow.collectAsState(initial = null)
    val pro by app.prefs.pro.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var exportGate by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    val current = item ?: run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val export: () -> Unit = {
        exporting = true
        scope.launch {
            runCatching { app.exporter.export(current) }
                .onSuccess { context.startActivity(app.exporter.shareIntent(it, current)) }
                .onFailure { Toast.makeText(context, "Couldn't create the PDF. Please try again.", Toast.LENGTH_SHORT).show() }
            exporting = false
        }
    }
    val requestExport = {
        if (pro) export() else {
            app.ads.preloadRewarded()
            exportGate = true
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Hero(current, onOpenImage = { path -> nav.navigate(Routes.viewer(path)) })
            Column(
                Modifier.offset(y = (-28).dp).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TitleCard(current)
                DeadlineRow(current)
                InfoCard(current)
                if (current.notes.isNotBlank()) {
                    VaultCard(Modifier.fillMaxWidth()) {
                        SectionHeader("Notes")
                        Spacer(Modifier.height(8.dp))
                        Text(current.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (current.photoList.isNotEmpty()) {
                    SectionHeader("Proof photos", Modifier.padding(top = 4.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        current.photoList.forEach { p ->
                            AsyncImage(
                                File(p), null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(16.dp))
                                    .clickable { nav.navigate(Routes.viewer(p)) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton(
                        "Export claim pack",
                        requestExport,
                        Modifier.weight(1f),
                        icon = Icons.Outlined.PictureAsPdf,
                        loading = exporting,
                    )
                }
                if (current.statusEnum == ItemStatus.ACTIVE && Deadlines.returnDeadline(current)?.isOver() == false) {
                    SecondaryButton(
                        "Mark as returned",
                        { scope.launch { app.repository.setStatus(current, ItemStatus.RETURNED) } },
                        Modifier.fillMaxWidth(),
                        icon = Icons.AutoMirrored.Outlined.AssignmentReturn,
                    )
                } else if (current.statusEnum != ItemStatus.ACTIVE) {
                    SecondaryButton(
                        "Move back to active",
                        { scope.launch { app.repository.setStatus(current, ItemStatus.ACTIVE) } },
                        Modifier.fillMaxWidth(),
                        icon = Icons.AutoMirrored.Outlined.Undo,
                    )
                }
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            }
        }

        // Floating top bar over the hero image.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back") { nav.popBackStack() }
            Spacer(Modifier.weight(1f))
            GlassButton(Icons.Outlined.Edit, "Edit") { nav.navigate(Routes.edit(id = current.id)) }
            Spacer(Modifier.width(10.dp))
            Box {
                GlassButton(Icons.Outlined.MoreVert, "More") { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    val archived = current.statusEnum == ItemStatus.ARCHIVED
                    DropdownMenuItem(
                        text = { Text(if (archived) "Unarchive" else "Archive") },
                        leadingIcon = { Icon(if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                        onClick = {
                            menu = false
                            scope.launch {
                                app.repository.setStatus(current, if (archived) ItemStatus.ACTIVE else ItemStatus.ARCHIVED)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = VaultlyTheme.colors.danger) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = VaultlyTheme.colors.danger) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this purchase?") },
            text = { Text("The receipt, photos and reminders for “${current.title}” will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        app.repository.delete(current)
                        nav.popBackStack()
                    }
                }) { Text("Delete", color = VaultlyTheme.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    if (exportGate) {
        val activity = context.findActivity()
        AlertDialog(
            onDismissRequest = { exportGate = false },
            icon = { Icon(Icons.Outlined.PictureAsPdf, null, tint = VaultlyTheme.colors.gold) },
            title = { Row(verticalAlignment = Alignment.CenterVertically) { Text("Claim pack"); Spacer(Modifier.width(8.dp)); ProBadge() } },
            text = {
                Text(
                    "A clean PDF with the receipt, photos, dates and deadlines — ready for a return desk or warranty claim. " +
                        "Watch a short video to export this one free, or go Pro for unlimited exports.",
                )
            },
            confirmButton = { TextButton(onClick = { exportGate = false; nav.openPaywall() }) { Text("Go Pro") } },
            dismissButton = {
                TextButton(onClick = {
                    exportGate = false
                    if (activity != null) {
                        app.ads.showRewarded(
                            activity,
                            onReward = export,
                            onUnavailable = {
                                Toast.makeText(context, "No video available right now. Try again shortly.", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }) { Text("Watch video") }
            },
        )
    }
}

@Composable
private fun Hero(item: Item, onOpenImage: (String) -> Unit) {
    val colors = VaultlyTheme.colors
    Box(Modifier.fillMaxWidth().height(320.dp).background(colors.inkBrush)) {
        val image = item.receiptImage
        if (image != null) {
            AsyncImage(
                File(image), "Receipt", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clickable { onOpenImage(image) },
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.35f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            Row(
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 44.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onOpenImage(image) }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ZoomOutMap, null, Modifier.size(14.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("View receipt", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(110.dp).clip(RoundedCornerShape(34.dp)).background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Palette.Gold.copy(alpha = 0.35f), RoundedCornerShape(34.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.categoryEnum.icon(), null, Modifier.size(52.dp), tint = Palette.GoldLight)
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.7f to Color.Transparent, 1f to MaterialTheme.colorScheme.background),
                ),
            )
        }
    }
}

@Composable
private fun TitleCard(item: Item) {
    val urgency = Deadlines.urgency(item)
    VaultCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${item.store} · ${Format.date(item.purchaseDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VaultlyTheme.colors.muted,
                )
            }
            Spacer(Modifier.width(12.dp))
            CategoryBadge(item.categoryEnum, 50.dp)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.price?.let {
                Text(Format.money(it, item.currency), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
            }
            Spacer(Modifier.weight(1f))
            val label = when (urgency) {
                Urgency.URGENT -> "Act soon"
                Urgency.SOON -> "Ending this month"
                Urgency.SAFE -> if (Deadlines.all(item).isEmpty()) "Stored" else "Protected"
                Urgency.EXPIRED -> "Coverage ended"
                Urgency.CLOSED -> if (item.statusEnum == ItemStatus.RETURNED) "Returned" else "Archived"
            }
            StatusPill(label, urgencyColor(urgency))
        }
    }
}

@Composable
private fun DeadlineRow(item: Item) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DeadlineCard(DeadlineKind.RETURN, Deadlines.returnDeadline(item), Modifier.weight(1f))
        DeadlineCard(DeadlineKind.WARRANTY, Deadlines.warrantyDeadline(item), Modifier.weight(1f))
    }
}

@Composable
private fun DeadlineCard(kind: DeadlineKind, deadline: Deadline?, modifier: Modifier) {
    val title = if (kind == DeadlineKind.RETURN) "Return window" else "Warranty"
    VaultCard(modifier, padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = VaultlyTheme.colors.muted)
        Spacer(Modifier.height(12.dp))
        if (deadline == null) {
            CountdownRing(0f, VaultlyTheme.colors.muted, "—", "NONE", Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Text(
                if (kind == DeadlineKind.RETURN) "No return window" else "No warranty",
                style = MaterialTheme.typography.bodySmall,
                color = VaultlyTheme.colors.muted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            val left = deadline.daysLeft()
            val color = daysColor(left)
            val (value, unit) = when {
                left < 0 -> "0" to "DAYS"
                left < 100 -> "$left" to if (left == 1L) "DAY" else "DAYS"
                left < 730 -> "${left / 30}" to "MONTHS"
                else -> "${left / 365}" to "YEARS"
            }
            CountdownRing(deadline.remainingFraction(), color, value, unit, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Text(
                (if (left < 0) "Ended " else if (kind == DeadlineKind.RETURN) "Return by " else "Until ") +
                    Format.shortDate(deadline.end),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun InfoCard(item: Item) {
    VaultCard(Modifier.fillMaxWidth()) {
        SectionHeader("Details")
        Spacer(Modifier.height(6.dp))
        val rows = buildList {
            add("Store" to item.store)
            add("Purchased" to Format.date(item.purchaseDate))
            add("Category" to item.categoryEnum.label)
            item.price?.let { add("Price" to Format.money(it, item.currency)) }
            add("Return policy" to Format.returnWindow(item.returnDays))
            add("Warranty" to Format.warranty(item.warrantyMonths))
            if (item.serial.isNotBlank()) add("Serial / order" to item.serial)
        }
        rows.forEachIndexed { i, (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = VaultlyTheme.colors.muted, modifier = Modifier.weight(1f))
                Text(v, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (i < rows.lastIndex) HorizontalDivider(color = VaultlyTheme.colors.hairline)
        }
    }
}

@Composable
private fun GlassButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(21.dp), tint = Color.White) }
}
