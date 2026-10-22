package com.calorielens.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val leftTabs = listOf(
    TabItem(Routes.HOME, "Today", Icons.Rounded.Home),
    TabItem(Routes.HISTORY, "History", Icons.Rounded.CalendarMonth),
)
private val rightTabs = listOf(
    TabItem(Routes.INSIGHTS, "Insights", Icons.Rounded.Insights),
    TabItem(Routes.PROFILE, "Profile", Icons.Rounded.Person),
)

@Composable
fun BottomBar(current: String?, onSelect: (String) -> Unit, onScan: () -> Unit) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .shadow(28.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(if (colors.isDark) Color(0xF2121419) else Color(0xF7FFFFFF))
                .border(1.dp, colors.stroke, shape)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            leftTabs.forEach { TabButton(it, current == it.route) { onSelect(it.route) } }
            Spacer(Modifier.width(72.dp))
            rightTabs.forEach { TabButton(it, current == it.route) { onSelect(it.route) } }
        }
        ScanButton(onScan, Modifier.offset(y = (-22).dp))
    }
}

@Composable
private fun TabButton(item: TabItem, selected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else AppTheme.colors.faint,
        tween(250),
        label = "tabTint",
    )
    val dot by animateFloatAsState(if (selected) 1f else 0f, tween(250), label = "tabDot")
    Column(
        modifier = Modifier
            .width(64.dp)
            .pressScale(scaleDown = 0.88f, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .size(width = 16.dp * dot, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(Brand.primaryGradient),
        )
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanPulse")
    val pulse by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse",
    )
    Box(
        modifier
            .size(84.dp)
            .drawBehind {
                val r = size.minDimension / 2 * (0.78f + 0.3f * pulse)
                drawCircle(Brand.Mint.copy(alpha = 0.35f * (1f - pulse)), r)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(66.dp)
                .pressScale(scaleDown = 0.9f, onClick = onClick)
                .shadow(18.dp, CircleShape, ambientColor = Brand.Mint, spotColor = Brand.Mint)
                .clip(CircleShape)
                .background(Brand.primaryGradient)
                .border(3.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PhotoCamera,
                contentDescription = "Scan food",
                tint = Color(0xFF04140F),
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer { rotationZ = 0f },
            )
        }
    }
}
