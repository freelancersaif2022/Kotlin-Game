package com.netscope.wifianalyzer.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netscope.wifianalyzer.ui.components.TourState
import com.netscope.wifianalyzer.ui.components.pressScale
import com.netscope.wifianalyzer.ui.components.tourTarget
import com.netscope.wifianalyzer.ui.theme.AppColors

data class TabItem(val route: String, val pattern: String, val label: String, val icon: ImageVector)

/**
 * Floating bottom menu: a glowing pill slides under the selected tab, icons
 * spring up when selected and every tap gives light haptic feedback.
 */
@Composable
fun AppBottomBar(items: List<TabItem>, selected: Int, tour: TourState, onSelect: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(22.dp, RoundedCornerShape(26.dp), ambientColor = primary.copy(alpha = 0.35f), spotColor = primary.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, AppColors.ext.cardBorder, RoundedCornerShape(26.dp))
                .padding(6.dp),
        ) {
            val itemWidth = maxWidth / items.size
            val index = selected.coerceIn(0, items.lastIndex)
            val x by animateDpAsState(itemWidth * index, spring(dampingRatio = 0.72f, stiffness = 520f), label = "navX")
            if (selected >= 0) {
                Box(
                    Modifier
                        .offset(x = x)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.20f), secondary.copy(alpha = 0.16f)))),
                )
                Box(
                    Modifier
                        .offset(x = x + itemWidth / 2 - 11.dp, y = (-6).dp)
                        .width(22.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(primary, secondary))),
                )
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                items.forEachIndexed { i, tab ->
                    val isSel = i == selected
                    val interaction = remember { MutableInteractionSource() }
                    val scale by animateFloatAsState(if (isSel) 1.12f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "navScale")
                    val tint by animateColorAsState(if (isSel) primary else AppColors.ext.muted, label = "navTint")
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .tourTarget(tour, "tab_${tab.route}")
                            .pressScale(interaction)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(interactionSource = interaction, indication = null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(i)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            tab.icon,
                            tab.label,
                            tint = tint,
                            modifier = Modifier.size(23.dp).graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            tab.label,
                            color = tint,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
