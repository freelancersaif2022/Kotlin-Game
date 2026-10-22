package com.calorielens.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.ads.BannerAd
import com.calorielens.app.data.prefs.ScanQuota
import com.calorielens.app.ui.components.AnimatedNumber
import com.calorielens.app.ui.components.AppCard
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.MealCard
import com.calorielens.app.ui.components.PremiumEmblem
import com.calorielens.app.ui.components.ProChip
import com.calorielens.app.ui.components.ProgressBar
import com.calorielens.app.ui.components.ProgressRing
import com.calorielens.app.ui.components.SectionHeader
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    isPremium: Boolean,
    quota: ScanQuota,
    onScan: () -> Unit,
    onOpenMeal: (Long) -> Unit,
    onPremium: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Header(state.profile.name, isPremium, onPremium)
        }
        item {
            Reveal(appeared, 0) { CalorieCard(state) }
        }
        item {
            Reveal(appeared, 1) { MacroRow(state) }
        }
        if (!isPremium) {
            item {
                Reveal(appeared, 2) { ScansLeftCard(quota, onPremium) }
            }
        }
        item {
            Reveal(appeared, 3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(Icons.Rounded.Whatshot, "${state.streak}", "day streak", Brand.Ember, Modifier.weight(1f))
                    StatTile(Icons.Rounded.CameraAlt, "${state.meals.size}", "meals today", Brand.Cyan, Modifier.weight(1f))
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader("Today's meals")
        }
        if (state.meals.isEmpty() && !state.loading) {
            item { EmptyMeals(onScan) }
        } else {
            items(state.meals, key = { it.id }) { meal ->
                MealCard(meal, onClick = { onOpenMeal(meal.id) }, modifier = Modifier.animateItem())
            }
        }
        if (!isPremium) {
            item {
                Column {
                    Text("Sponsored", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.faint)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.clip(RoundedCornerShape(16.dp))) { BannerAd() }
                }
            }
        }
    }
}

@Composable
private fun Reveal(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(450, delayMillis = index * 80)) +
            slideInVertically(tween(500, delayMillis = index * 80)) { it / 5 },
    ) { content() }
}

@Composable
private fun Header(name: String, isPremium: Boolean, onPremium: () -> Unit) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(date.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.muted)
            Spacer(Modifier.height(4.dp))
            Text(
                if (name.isBlank()) greeting else "$greeting, ${name.trim()}",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
            )
        }
        if (isPremium) {
            ProChip()
        } else {
            Box(Modifier.pressScale(scaleDown = 0.9f, onClick = onPremium)) {
                PremiumEmblem(size = 48.dp)
            }
        }
    }
}

@Composable
private fun CalorieCard(state: HomeState) {
    val over = state.remaining < 0
    AppCard(contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = if (state.targets.calories > 0) state.calories.toFloat() / state.targets.calories else 0f,
                size = 168.dp,
                stroke = 15.dp,
                colors = if (over) listOf(Brand.Ember, Brand.Danger, Brand.Ember) else listOf(Brand.Mint, Brand.Cyan, Brand.Mint),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedNumber(
                        kotlin.math.abs(state.remaining),
                        MaterialTheme.typography.displaySmall,
                        startFromZero = false,
                    )
                    Text(
                        if (over) "kcal over" else "kcal left",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (over) Brand.Danger else AppTheme.colors.muted,
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RingStat(Icons.Rounded.LocalFireDepartment, "Eaten", state.calories, Brand.Amber)
                RingStat(Icons.Rounded.AutoAwesome, "Goal", state.targets.calories, Brand.Mint)
            }
        }
    }
}

@Composable
private fun RingStat(icon: ImageVector, label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
            AnimatedNumber(value, MaterialTheme.typography.titleLarge, startFromZero = false, suffix = " kcal")
        }
    }
}

@Composable
private fun MacroRow(state: HomeState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MacroTile("Protein", state.protein, state.targets.protein, Brand.Protein, Modifier.weight(1f))
        MacroTile("Carbs", state.carbs, state.targets.carbs, Brand.Carbs, Modifier.weight(1f))
        MacroTile("Fat", state.fat, state.targets.fat, Brand.Fat, Modifier.weight(1f))
    }
}

@Composable
private fun MacroTile(label: String, value: Float, target: Int, color: Color, modifier: Modifier) {
    AppCard(modifier, shape = RoundedCornerShape(20.dp), contentPadding = 14.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${value.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(" / ${target}g", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.faint, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(if (target > 0) value / target else 0f, color, Modifier.fillMaxWidth(), height = 6.dp)
    }
}

@Composable
private fun ScansLeftCard(quota: ScanQuota, onPremium: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF221C10), Color(0xFF15120C))))
            .border(1.dp, Brush.linearGradient(listOf(Brand.Gold.copy(alpha = 0.6f), Brand.GoldDark.copy(alpha = 0.2f))), shape)
            .pressScale(scaleDown = 0.98f, onClick = onPremium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PremiumEmblem(size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${quota.remaining} of ${quota.limit} free scans left today",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Go Premium for unlimited scans, full nutrients and no ads.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "UPGRADE",
            style = MaterialTheme.typography.labelMedium.copy(brush = Brand.goldGradient),
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier) {
    AppCard(modifier, shape = RoundedCornerShape(20.dp), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
            }
        }
    }
}

@Composable
private fun EmptyMeals(onScan: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = 24.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brand.Mint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PhotoCamera, null, tint = Brand.Mint, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("No meals yet today", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Snap a photo of your food and we'll log the calories and nutrients for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            GradientButton("Scan a meal", onScan, icon = Icons.Rounded.CameraAlt, modifier = Modifier.fillMaxWidth(), height = 52.dp)
        }
    }
}
