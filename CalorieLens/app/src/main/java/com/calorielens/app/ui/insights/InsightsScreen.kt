package com.calorielens.app.ui.insights

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.ui.components.AppCard
import com.calorielens.app.ui.components.BarChart
import com.calorielens.app.ui.components.BarPoint
import com.calorielens.app.ui.components.DonutChart
import com.calorielens.app.ui.components.DonutSlice
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.LineChart
import com.calorielens.app.ui.components.MacroDot
import com.calorielens.app.ui.components.PremiumEmblem
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(
    isPremium: Boolean,
    onPremium: () -> Unit,
    vm: InsightsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineLarge)
        SegmentedTabs(listOf("7 days", "30 days"), tab) { tab = it }

        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "insightsTab",
        ) { t ->
            val stats = if (t == 0) state.week else state.month
            val locked = t == 1 && !isPremium
            Box {
                Column(
                    Modifier.then(
                        if (locked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(14.dp) else Modifier.graphicsLayer { alpha = 0.12f }
                        } else {
                            Modifier
                        },
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Icons.Rounded.LocalFireDepartment, "Avg / day", "%,d".format(stats.avgCalories), "kcal", Brand.Ember, Modifier.weight(1f))
                        StatCard(Icons.Rounded.TrackChanges, "On target", "${stats.onTargetDays(state.targets.calories)}", "of ${stats.loggedDays} days", Brand.Mint, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Icons.Rounded.Restaurant, "Meals", "${stats.totalMeals}", "logged", Brand.Cyan, Modifier.weight(1f))
                        StatCard(Icons.Rounded.EmojiEvents, "Avg protein", "${stats.avgProtein.toInt()}", "g / day", Brand.Protein, Modifier.weight(1f))
                    }
                    AppCard(contentPadding = 18.dp) {
                        Text("Calories", style = MaterialTheme.typography.titleMedium)
                        Text("Dashed line is your daily goal", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
                        Spacer(Modifier.height(16.dp))
                        if (t == 0) {
                            val today = LocalDate.now()
                            BarChart(
                                points = stats.days.map {
                                    BarPoint(
                                        label = it.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                        value = it.calories.toFloat(),
                                        highlight = it.date == today,
                                    )
                                },
                                goal = state.targets.calories.toFloat(),
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                            )
                        } else {
                            LineChart(
                                values = stats.days.map { it.calories.toFloat() },
                                goal = state.targets.calories.toFloat(),
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                            )
                        }
                    }
                    AppCard(contentPadding = 18.dp) {
                        Text("Average macro split", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(
                                listOf(
                                    DonutSlice(stats.avgProtein * 4, Brand.Protein),
                                    DonutSlice(stats.avgCarbs * 4, Brand.Carbs),
                                    DonutSlice(stats.avgFat * 9, Brand.Fat),
                                ),
                                size = 120.dp,
                                stroke = 16.dp,
                            )
                            Spacer(Modifier.width(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MacroDot(Brand.Protein, "Protein ${stats.avgProtein.toInt()}g / ${state.targets.protein}g")
                                MacroDot(Brand.Carbs, "Carbs ${stats.avgCarbs.toInt()}g / ${state.targets.carbs}g")
                                MacroDot(Brand.Fat, "Fat ${stats.avgFat.toInt()}g / ${state.targets.fat}g")
                            }
                        }
                    }
                }
                if (locked) {
                    Column(
                        Modifier
                            .matchParentSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PremiumEmblem(size = 110.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("30-day trends are Premium", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "See your monthly calorie trend, consistency and macro balance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.muted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(18.dp))
                        GradientButton(
                            "Unlock insights", onPremium,
                            brush = Brand.goldGradient, contentColor = Color(0xFF2A1A00), shimmer = true,
                            modifier = Modifier.fillMaxWidth(), height = 52.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(AppTheme.colors.card)
            .padding(4.dp),
    ) {
        val segment = maxWidth / labels.size
        val offset by animateDpAsState(segment * selected, spring(dampingRatio = 0.75f, stiffness = 500f), label = "segment")
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(Brand.primaryGradient),
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pressScale(scaleDown = 0.96f) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (i == selected) Color(0xFF04140F) else AppTheme.colors.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, unit: String, color: Color, modifier: Modifier) {
    AppCard(modifier, shape = RoundedCornerShape(20.dp), contentPadding = 16.dp) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}
