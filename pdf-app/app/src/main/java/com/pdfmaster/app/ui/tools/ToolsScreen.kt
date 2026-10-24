package com.pdfmaster.app.ui.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pdfmaster.app.ui.components.IconCircle
import com.pdfmaster.app.ui.components.ProBadge
import com.pdfmaster.app.ui.components.bounceClick
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.premium.PremiumChip
import com.pdfmaster.app.ui.premium.rememberPremiumGate
import com.pdfmaster.app.ui.premium.rememberPremiumState
import com.pdfmaster.app.ui.theme.AppTheme
import kotlinx.coroutines.delay

@Stable
class ToolLauncher(private val open: (PdfTool) -> Unit) {
    fun launch(tool: PdfTool) = open(tool)
}

/** Opens a tool, routing Pro tools through the Premium gate for free users. */
@Composable
fun rememberToolLauncher(): ToolLauncher {
    val nav = LocalNavigator.current
    val gate = rememberPremiumGate()
    return remember(gate) {
        ToolLauncher { tool ->
            if (tool.isPro) gate.require(tool.title) { nav.tool(tool) } else nav.tool(tool)
        }
    }
}

@Composable
fun ToolTile(tool: PdfTool, locked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, index: Int = 0) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 35L)
        appear.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
    }
    Column(
        modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 16.dp.toPx()
            }
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            IconCircle(tool.icon, tool.color, size = 56.dp, iconSize = 26.dp)
            if (locked) ProBadge(Modifier.align(Alignment.TopEnd).graphicsLayer { translationX = 12.dp.toPx(); translationY = 2.dp.toPx() }, small = true)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            tool.shortTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ToolsScreen(contentPadding: PaddingValues) {
    val launcher = rememberToolLauncher()
    val premium = rememberPremiumState()
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("All Tools", style = MaterialTheme.typography.headlineMedium)
                    Text("${PdfTool.entries.size} powerful tools, all offline", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PremiumChip()
            }
        }
        ToolCategory.entries.forEachIndexed { ci, category ->
            val tools = PdfTool.entries.filter { it.category == category }
            item(key = category.name) {
                Text(
                    category.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 10.dp),
                )
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, AppTheme.colors.cardBorder, MaterialTheme.shapes.large),
                ) {
                    tools.forEachIndexed { i, tool ->
                        ToolListRow(tool, locked = tool.isPro && !premium.isPremium, index = ci * 3 + i) { launcher.launch(tool) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolListRow(tool: PdfTool, locked: Boolean, index: Int, onClick: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 25L)
        appear.animateTo(1f, tween(320))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = appear.value; translationX = (1f - appear.value) * 24.dp.toPx() }
            .bounceClick(scaleDown = 0.985f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(tool.icon, tool.color, size = 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tool.title, style = MaterialTheme.typography.titleSmall)
                if (locked) ProBadge(small = true)
            }
            Text(tool.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))
    }
}
