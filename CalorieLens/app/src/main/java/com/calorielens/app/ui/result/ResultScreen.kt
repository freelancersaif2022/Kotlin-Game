package com.calorielens.app.ui.result

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.data.db.MealEntity
import com.calorielens.app.data.db.MealType
import com.calorielens.app.data.db.alternativeLabels
import com.calorielens.app.data.db.nutrition
import com.calorielens.app.data.nutrition.FoodDatabase
import com.calorielens.app.data.nutrition.Nutrition
import com.calorielens.app.data.nutrition.healthScore
import com.calorielens.app.data.nutrition.healthTags
import com.calorielens.app.ui.components.AnimatedNumber
import com.calorielens.app.ui.components.AppCard
import com.calorielens.app.ui.components.AppDialog
import com.calorielens.app.ui.components.DonutChart
import com.calorielens.app.ui.components.DonutSlice
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.IconCircleButton
import com.calorielens.app.ui.components.LocalSnackbar
import com.calorielens.app.ui.components.MealPhoto
import com.calorielens.app.ui.components.MessageDialog
import com.calorielens.app.ui.components.OutlineButton
import com.calorielens.app.ui.components.Pill
import com.calorielens.app.ui.components.PremiumEmblem
import com.calorielens.app.ui.components.ProgressBar
import com.calorielens.app.ui.components.SnackType
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.components.timeLabel
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand

@Composable
fun ResultScreen(
    isPremium: Boolean,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onScanAgain: () -> Unit,
    vm: ResultViewModel = hiltViewModel(),
) {
    val meal by vm.meal.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    var confirmDelete by remember { mutableStateOf(false) }
    var pickMealType by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    LaunchedEffect(meal?.id) {
        val m = meal ?: return@LaunchedEffect
        if (vm.fresh && !vm.announced) {
            vm.announced = true
            snackbar.show("Saved to ${m.mealType.label} · ${m.calories} kcal", SnackType.SUCCESS, title = "Meal logged")
        }
    }

    val listState = rememberLazyListState()
    val headerOffset by remember {
        derivedStateOf { if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else 1000f }
    }
    val topBarSolid by remember { derivedStateOf { headerOffset > 600f } }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val m = meal
        if (m != null) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .graphicsLayer { translationY = headerOffset * 0.5f },
                    ) {
                        MealPhoto(m.imagePath, Modifier.matchParentSize())
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.45f),
                                        0.35f to Color.Transparent,
                                        0.75f to Color.Transparent,
                                        1f to MaterialTheme.colorScheme.background,
                                    ),
                                ),
                        )
                    }
                }
                item {
                    Column(
                        Modifier
                            .offset(y = (-36).dp)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        TitleBlock(m) { pickMealType = true }
                        CaloriesCard(m)
                        MacroCard(m.nutrition)
                        HealthTagsRow(m.nutrition)
                        NutritionFacts(m.nutrition)
                        MicronutrientsCard(m.nutrition, isPremium, onPremium)
                        if (m.alternativeLabels.isNotEmpty()) {
                            AlternativesCard(m.alternativeLabels) { label ->
                                vm.switchTo(label)
                                snackbar.show("Updated to ${FoodDatabase.prettify(label)}", SnackType.INFO)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlineButton("Done", onBack, Modifier.weight(1f), icon = Icons.Rounded.CheckCircle, height = 56.dp)
                            GradientButton("Scan another", onScanAgain, Modifier.weight(1.4f), icon = Icons.Rounded.CameraAlt, height = 56.dp)
                        }
                        Text(
                            "Values are estimates for a typical serving and may differ from your actual portion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.faint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                        )
                    }
                }
            }
        }

        // Floating top bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (topBarSolid) MaterialTheme.colorScheme.background.copy(alpha = 0.96f) else Color.Transparent)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircleButton(
                Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack,
                background = if (topBarSolid) AppTheme.colors.cardElevated else Color.Black.copy(alpha = 0.4f),
                tint = if (topBarSolid) MaterialTheme.colorScheme.onSurface else Color.White,
                bordered = topBarSolid,
            )
            Text(
                if (topBarSolid) meal?.name.orEmpty() else "",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            IconCircleButton(
                Icons.Rounded.DeleteOutline, "Delete", { confirmDelete = true },
                background = if (topBarSolid) AppTheme.colors.cardElevated else Color.Black.copy(alpha = 0.4f),
                tint = if (topBarSolid) Brand.Danger else Color.White,
                bordered = topBarSolid,
            )
        }
    }

    if (confirmDelete) {
        MessageDialog(
            icon = Icons.Rounded.DeleteOutline,
            iconColor = Brand.Danger,
            title = "Delete this meal?",
            message = "It will be removed from your diary and today's totals.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                if (!deleting) {
                    deleting = true
                    vm.delete { removed ->
                        onBack()
                        snackbar.show(
                            "${removed.name} removed",
                            SnackType.INFO,
                            actionLabel = "Undo",
                            onAction = { vm.undoDelete(removed) },
                            onDismissed = { vm.finalizeDelete(removed) },
                        )
                    }
                }
            },
            dismissText = "Cancel",
            onDismiss = { confirmDelete = false },
        )
    }

    if (pickMealType) {
        val current = meal?.mealType
        AppDialog(onDismiss = { pickMealType = false }) {
            Text("Move to", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            MealType.entries.forEach { type ->
                val selected = type == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .pressScale(scaleDown = 0.98f) {
                            vm.setMealType(type)
                            pickMealType = false
                            snackbar.show("Moved to ${type.label}", SnackType.SUCCESS)
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Brand.Mint.copy(alpha = 0.12f) else AppTheme.colors.card)
                        .border(1.dp, if (selected) Brand.Mint else AppTheme.colors.stroke, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(type.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = Brand.Mint)
                }
            }
        }
    }
}

@Composable
private fun TitleBlock(meal: MealEntity, onMealType: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                "${(meal.confidence * 100).toInt().coerceIn(1, 99)}% match",
                Brand.Mint,
                icon = Icons.Rounded.CheckCircle,
            )
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AppTheme.colors.cardElevated)
                    .border(1.dp, AppTheme.colors.stroke, RoundedCornerShape(50))
                    .pressScale(scaleDown = 0.94f, onClick = onMealType)
                    .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Restaurant, null, tint = AppTheme.colors.muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${meal.mealType.label} · ${meal.timeLabel()}", style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Rounded.ExpandMore, null, tint = AppTheme.colors.muted, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(meal.name, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CaloriesCard(meal: MealEntity) {
    val n = meal.nutrition
    val score = n.healthScore()
    AppCard(contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Brand.calorieGradient),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Calories", style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.muted)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber(n.calories, MaterialTheme.typography.displayLarge.copy(brush = Brand.calorieGradient))
                    Spacer(Modifier.width(6.dp))
                    Text("kcal", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.muted, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
            HealthGauge(score)
        }
    }
}

@Composable
private fun HealthGauge(score: Int) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(score) { anim.animateTo(score / 10f, tween(1200, easing = FastOutSlowInEasing)) }
    val color = when {
        score >= 7 -> Brand.Mint
        score >= 4 -> Brand.Amber
        else -> Brand.Danger
    }
    val track = AppTheme.colors.stroke
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(width = 96.dp, height = 60.dp), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.size(96.dp).offset(y = 18.dp)) {
                val s = 9.dp.toPx()
                val arc = Size(size.width - s, size.height - s)
                drawArc(track, 180f, 180f, false, Offset(s / 2, s / 2), arc, style = Stroke(s, cap = StrokeCap.Round))
                drawArc(
                    Brush.horizontalGradient(listOf(Brand.Danger, Brand.Amber, Brand.Mint)),
                    180f, 180f * anim.value, false, Offset(s / 2, s / 2), arc,
                    style = Stroke(s, cap = StrokeCap.Round),
                )
            }
            Text("$score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
        }
        Text("Health score", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted)
    }
}

@Composable
private fun MacroCard(n: Nutrition) {
    val pK = n.protein * 4
    val cK = n.carbs * 4
    val fK = n.fat * 9
    val total = (pK + cK + fK).coerceAtLeast(1f)
    AppCard(contentPadding = 20.dp) {
        Text("Macronutrients", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(
                listOf(DonutSlice(pK, Brand.Protein), DonutSlice(cK, Brand.Carbs), DonutSlice(fK, Brand.Fat)),
                size = 128.dp,
                stroke = 16.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(n.protein + n.carbs + n.fat).toInt()}g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("total", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.muted)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroLegend("Protein", n.protein, pK / total, Brand.Protein)
                MacroLegend("Carbs", n.carbs, cK / total, Brand.Carbs)
                MacroLegend("Fat", n.fat, fK / total, Brand.Fat)
            }
        }
    }
}

@Composable
private fun MacroLegend(label: String, grams: Float, share: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 30.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.0fg".format(grams), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("${(share * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun HealthTagsRow(n: Nutrition) {
    val tags = n.healthTags()
    if (tags.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Pill(
                tag.label,
                if (tag.positive) Brand.Mint else Brand.Amber,
                icon = if (tag.positive) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
            )
        }
    }
}

private data class FactRow(val label: String, val value: String, val dv: Float, val color: Color, val indent: Boolean = false)

@Composable
private fun NutritionFacts(n: Nutrition) {
    val rows = listOf(
        FactRow("Total fat", "%.1f g".format(n.fat), n.fat / 78f, Brand.Fat),
        FactRow("Saturated fat", "%.1f g".format(n.saturatedFat), n.saturatedFat / 20f, Brand.Fat, indent = true),
        FactRow("Cholesterol", "${n.cholesterolMg} mg", n.cholesterolMg / 300f, Brand.Amber),
        FactRow("Sodium", "${n.sodiumMg} mg", n.sodiumMg / 2300f, Brand.Info),
        FactRow("Total carbs", "%.1f g".format(n.carbs), n.carbs / 275f, Brand.Carbs),
        FactRow("Dietary fiber", "%.1f g".format(n.fiber), n.fiber / 28f, Brand.Fiber, indent = true),
        FactRow("Sugars", "%.1f g".format(n.sugar), n.sugar / 50f, Brand.Ember, indent = true),
        FactRow("Protein", "%.1f g".format(n.protein), n.protein / 50f, Brand.Protein),
    )
    AppCard(contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nutrition facts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("% Daily value", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted)
        }
        Spacer(Modifier.height(12.dp))
        rows.forEachIndexed { i, r -> FactLine(r, i) }
    }
}

@Composable
private fun FactLine(r: FactRow, index: Int) {
    Column(Modifier.padding(vertical = 7.dp).padding(start = if (r.indent) 14.dp else 0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                r.label,
                style = if (r.indent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                color = if (r.indent) AppTheme.colors.muted else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(r.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${(r.dv * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = AppTheme.colors.muted,
                textAlign = TextAlign.End,
                modifier = Modifier.width(48.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(r.dv, r.color, Modifier.fillMaxWidth(), height = 5.dp, delayMs = 80 * index)
    }
}

@Composable
private fun MicronutrientsCard(n: Nutrition, isPremium: Boolean, onPremium: () -> Unit) {
    val rows = listOf(
        FactRow("Vitamin A", "${n.vitaminAMcg} µg", n.vitaminAMcg / 900f, Brand.Amber),
        FactRow("Vitamin C", "%.1f mg".format(n.vitaminCMg), n.vitaminCMg / 90f, Brand.Ember),
        FactRow("Calcium", "${n.calciumMg} mg", n.calciumMg / 1300f, Brand.Cyan),
        FactRow("Iron", "%.1f mg".format(n.ironMg), n.ironMg / 18f, Brand.Protein),
        FactRow("Potassium", "${n.potassiumMg} mg", n.potassiumMg / 4700f, Brand.Mint),
    )
    AppCard(contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vitamins & minerals", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!isPremium) Pill("PRO", Brand.Gold, icon = Icons.Rounded.Lock)
        }
        Spacer(Modifier.height(12.dp))
        Box {
            Column(
                Modifier.then(
                    if (!isPremium) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(10.dp) else Modifier.graphicsLayer { alpha = 0.15f }
                    } else {
                        Modifier
                    },
                ),
            ) {
                rows.forEachIndexed { i, r -> FactLine(r, i) }
            }
            if (!isPremium) {
                Column(
                    Modifier
                        .matchParentSize()
                        .pressScale(scaleDown = 0.98f, onClick = onPremium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PremiumEmblem(size = 64.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("Unlock full micronutrients", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "See vitamins & minerals for every meal",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brand.goldGradient)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF2A1A00), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Go Premium", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2A1A00), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlternativesCard(labels: List<String>, onPick: (String) -> Unit) {
    AppCard(contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SwapHoriz, null, tint = AppTheme.colors.muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Not quite right? It could also be", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            labels.forEach { label ->
                Text(
                    FoodDatabase.prettify(label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppTheme.colors.cardElevated)
                        .border(1.dp, AppTheme.colors.stroke, RoundedCornerShape(50))
                        .pressScale(scaleDown = 0.94f) { onPick(label) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
    }
}
