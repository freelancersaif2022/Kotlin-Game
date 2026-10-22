package com.calorielens.app.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.data.repo.DayTotals
import com.calorielens.app.ui.components.AppCard
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.IconCircleButton
import com.calorielens.app.ui.components.MealCard
import com.calorielens.app.ui.components.PremiumEmblem
import com.calorielens.app.ui.components.ProgressBar
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

const val FREE_HISTORY_DAYS = 7L

@Composable
fun HistoryScreen(
    isPremium: Boolean,
    onOpenMeal: (Long) -> Unit,
    onPremium: () -> Unit,
    vm: HistoryViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val locked = !isPremium && state.selected.isBefore(today.minusDays(FREE_HISTORY_DAYS - 1))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
                Text("History", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text("Your food diary, day by day", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
            }
        }
        item {
            WeekStrip(
                state = state,
                today = today,
                onSelect = vm::select,
                onPrev = { vm.shiftWeek(-1) },
                onNext = { vm.shiftWeek(1) },
            )
        }
        item {
            AnimatedContent(
                targetState = state.selected,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "daySummary",
            ) { date ->
                val totals = state.week.firstOrNull { it.date == date }
                DaySummary(date, totals, state.goal, locked)
            }
        }
        when {
            locked -> item { LockedHistory(onPremium) }
            state.meals.isEmpty() -> item { EmptyDay(state.selected == today) }
            else -> items(state.meals, key = { it.id }) { meal ->
                MealCard(meal, onClick = { onOpenMeal(meal.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun WeekStrip(
    state: HistoryState,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
    AppCard(contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous week", onPrev, size = 36.dp)
            Text(
                state.weekStart.format(monthFmt),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            val canNext = state.weekStart.plusDays(7).isBefore(today.plusDays(1))
            IconCircleButton(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next week",
                { if (canNext) onNext() }, size = 36.dp,
                tint = if (canNext) MaterialTheme.colorScheme.onSurface else AppTheme.colors.faint,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0L until 7L).forEach { i ->
                val date = state.weekStart.plusDays(i)
                val totals = state.week.firstOrNull { it.date == date }
                DayChip(
                    date = date,
                    selected = date == state.selected,
                    isToday = date == today,
                    future = date.isAfter(today),
                    progress = if (state.goal > 0) (totals?.calories ?: 0).toFloat() / state.goal else 0f,
                    onClick = { onSelect(date) },
                )
            }
        }
    }
}

@Composable
private fun DayChip(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    future: Boolean,
    progress: Float,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (selected) Brand.Mint else Color.Transparent, tween(250), label = "dayBg")
    val fg = when {
        selected -> Color(0xFF04140F)
        future -> AppTheme.colors.faint
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier
            .width(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(if (isToday && !selected) Modifier.border(1.dp, Brand.Mint.copy(alpha = 0.6f), RoundedCornerShape(16.dp)) else Modifier)
            .pressScale(enabled = !future, scaleDown = 0.9f, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) fg.copy(alpha = 0.7f) else AppTheme.colors.muted,
        )
        Spacer(Modifier.height(4.dp))
        Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = fg)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    when {
                        progress <= 0f -> Color.Transparent
                        selected -> Color(0xFF04140F)
                        progress > 1.05f -> Brand.Ember
                        else -> Brand.Mint
                    },
                ),
        )
    }
}

@Composable
private fun DaySummary(date: LocalDate, totals: DayTotals?, goal: Int, locked: Boolean) {
    val today = LocalDate.now()
    val title = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    }
    val kcal = if (locked) 0 else totals?.calories ?: 0
    AppCard(contentPadding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (locked) "Premium history" else "${totals?.meals ?: 0} meals logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.muted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (locked) "—" else "%,d".format(kcal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("of %,d kcal".format(goal), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(if (goal > 0) kcal.toFloat() / goal else 0f, if (kcal > goal) Brand.Ember else Brand.Mint, Modifier.fillMaxWidth())
        if (!locked && totals != null && totals.meals > 0) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroText("Protein", totals.protein, Brand.Protein)
                MacroText("Carbs", totals.carbs, Brand.Carbs)
                MacroText("Fat", totals.fat, Brand.Fat)
            }
        }
    }
}

@Composable
private fun MacroText(label: String, grams: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text("$label ", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
        Text("${grams.toInt()}g", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LockedHistory(onPremium: () -> Unit) {
    AppCard(contentPadding = 24.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PremiumEmblem(size = 84.dp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, tint = Brand.Gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Older history is a Premium feature", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Free accounts keep the last $FREE_HISTORY_DAYS days. Upgrade to see your complete food diary.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            GradientButton(
                "Unlock full history", onPremium,
                brush = Brand.goldGradient, contentColor = Color(0xFF2A1A00), shimmer = true,
                modifier = Modifier.fillMaxWidth(), height = 52.dp,
            )
        }
    }
}

@Composable
private fun EmptyDay(isToday: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AppTheme.colors.card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.EventBusy, null, tint = AppTheme.colors.faint, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(if (isToday) "Nothing logged yet today" else "No meals on this day", style = MaterialTheme.typography.titleMedium)
        Text("Scanned meals will appear here.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
    }
}
