package com.calorielens.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.Gender
import com.calorielens.app.domain.Goal
import com.calorielens.app.ui.components.AnimatedNumber
import com.calorielens.app.ui.components.GlowBox
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.IconCircleButton
import com.calorielens.app.ui.components.ProgressRing
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand
import kotlin.math.roundToInt

private const val STEPS = 6

@Composable
fun OnboardingScreen(onFinished: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val profile by vm.profile.collectAsStateWithLifecycle()

    BackHandler(enabled = step > 0) { step-- }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        BackgroundAura()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            if (step > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", { step-- }, size = 40.dp)
                    Spacer(Modifier.width(16.dp))
                    StepIndicator(step, Modifier.weight(1f))
                    Spacer(Modifier.width(56.dp))
                }
            }
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(380)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(320)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(220)))
                },
                label = "onboardingStep",
            ) { s ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                ) {
                    when (s) {
                        0 -> WelcomeStep()
                        1 -> AboutStep(profile.name, profile.gender, vm::setName, vm::setGender)
                        2 -> BodyStep(profile.age, profile.heightCm, profile.weightKg, vm::setAge, vm::setHeight, vm::setWeight)
                        3 -> ActivityStep(profile.activity, vm::setActivity)
                        4 -> GoalStep(profile.goal, vm::setGoal)
                        else -> PlanStep(vm.targets.calories, vm.targets.protein, vm.targets.carbs, vm.targets.fat, profile.name)
                    }
                }
            }
            GradientButton(
                text = when (step) {
                    0 -> "Get started"
                    STEPS - 1 -> "Start tracking"
                    else -> "Continue"
                },
                icon = if (step == STEPS - 1) Icons.Rounded.Bolt else Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = { if (step < STEPS - 1) step++ else vm.finish(onFinished) },
                shimmer = step == 0 || step == STEPS - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun BackgroundAura() {
    val transition = rememberInfiniteTransition(label = "aura")
    val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "auraShift")
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(listOf(Brand.Mint.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * (0.1f + 0.3f * shift), size.height * 0.12f), size.width * 0.8f),
            radius = size.width * 0.8f,
            center = Offset(size.width * (0.1f + 0.3f * shift), size.height * 0.12f),
        )
        drawCircle(
            Brush.radialGradient(listOf(Brand.Cyan.copy(alpha = 0.10f), Color.Transparent), Offset(size.width * (0.9f - 0.3f * shift), size.height * 0.75f), size.width * 0.7f),
            radius = size.width * 0.7f,
            center = Offset(size.width * (0.9f - 0.3f * shift), size.height * 0.75f),
        )
    }
}

@Composable
private fun StepIndicator(step: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1 until STEPS) {
            val fill by animateFloatAsState(if (i <= step) 1f else 0f, tween(400), label = "stepFill")
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AppTheme.colors.stroke),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brand.primaryGradient),
                )
            }
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.muted)
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(48.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ScannerHero(260.dp)
    }
    Spacer(Modifier.height(40.dp))
    Text(
        "Snap your food.\nKnow your calories.",
        style = MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(14.dp))
    Text(
        "Point your camera at any meal and get calories, macros and nutrients in seconds — fully on your phone, no account needed.",
        style = MaterialTheme.typography.bodyLarge,
        color = AppTheme.colors.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        FeatureChip("On-device AI")
        Spacer(Modifier.width(8.dp))
        FeatureChip("Private")
        Spacer(Modifier.width(8.dp))
        FeatureChip("Instant")
    }
}

@Composable
private fun FeatureChip(text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(AppTheme.colors.card)
            .border(1.dp, AppTheme.colors.stroke, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = Brand.Mint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

/** Animated viewfinder with a sweeping laser around a plate icon. */
@Composable
fun ScannerHero(size: Dp) {
    val transition = rememberInfiniteTransition(label = "hero")
    val scan by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scan")
    val rotate by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "orbit")
    GlowBox(Brand.Mint, Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val corner = w * 0.16f
            val stroke = w * 0.022f
            val inset = w * 0.12f
            val brush = Brush.linearGradient(listOf(Brand.Mint, Brand.Cyan))
            val l = inset
            val t = inset
            val r = w - inset
            val b = w - inset
            listOf(
                Triple(Offset(l, t + corner), Offset(l, t), Offset(l + corner, t)),
                Triple(Offset(r - corner, t), Offset(r, t), Offset(r, t + corner)),
                Triple(Offset(l, b - corner), Offset(l, b), Offset(l + corner, b)),
                Triple(Offset(r - corner, b), Offset(r, b), Offset(r, b - corner)),
            ).forEach { (a, m, c) ->
                drawLine(brush, a, m, stroke, StrokeCap.Round)
                drawLine(brush, m, c, stroke, StrokeCap.Round)
            }
            // orbiting dots
            for (i in 0 until 3) {
                val ang = Math.toRadians((rotate + i * 120f).toDouble())
                val rad = w * 0.46f
                drawCircle(
                    listOf(Brand.Amber, Brand.Protein, Brand.Fat)[i],
                    w * 0.018f,
                    Offset(center.x + (rad * kotlin.math.cos(ang)).toFloat(), center.y + (rad * kotlin.math.sin(ang)).toFloat()),
                )
            }
            val y = t + (b - t) * scan
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Brand.Mint.copy(alpha = 0.28f)), y - w * 0.14f, y),
                topLeft = Offset(l + stroke, y - w * 0.14f),
                size = androidx.compose.ui.geometry.Size(r - l - stroke * 2, w * 0.14f),
            )
            drawLine(Brand.Mint, Offset(l + stroke, y), Offset(r - stroke, y), w * 0.008f, StrokeCap.Round)
        }
        Box(
            Modifier
                .size(size * 0.36f)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF1C2230), Color(0xFF10131A))))
                .border(1.dp, Brand.Mint.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Restaurant, null, tint = Brand.Amber, modifier = Modifier.size(size * 0.16f))
        }
    }
}

@Composable
private fun AboutStep(name: String, gender: Gender, onName: (String) -> Unit, onGender: (Gender) -> Unit) {
    StepTitle("Tell us about you", "We use this to personalise your daily calorie target.")
    Text("Your name", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.muted)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        singleLine = true,
        placeholder = { Text("e.g. Alex") },
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand.Mint,
            unfocusedBorderColor = AppTheme.colors.stroke,
            focusedContainerColor = AppTheme.colors.card,
            unfocusedContainerColor = AppTheme.colors.card,
            cursorColor = Brand.Mint,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))
    Text("Gender", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.muted)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GenderCard(Gender.MALE, Icons.Rounded.Male, gender == Gender.MALE, Modifier.weight(1f)) { onGender(Gender.MALE) }
        GenderCard(Gender.FEMALE, Icons.Rounded.Female, gender == Gender.FEMALE, Modifier.weight(1f)) { onGender(Gender.FEMALE) }
    }
}

@Composable
private fun GenderCard(g: Gender, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) Brand.Mint else AppTheme.colors.stroke, tween(250), label = "genderBorder")
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .pressScale(onClick = onClick)
            .clip(shape)
            .background(if (selected) Brand.Mint.copy(alpha = 0.08f) else AppTheme.colors.card)
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) Brand.Mint else AppTheme.colors.muted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(g.label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BodyStep(
    age: Int, height: Int, weight: Float,
    onAge: (Int) -> Unit, onHeight: (Int) -> Unit, onWeight: (Float) -> Unit,
) {
    StepTitle("Your body", "Adjust with the slider or the buttons.")
    ValueCard("Age", "$age", "years", age.toFloat(), 13f..90f, { onAge(it.roundToInt()) }, { onAge(age - 1) }, { onAge(age + 1) })
    Spacer(Modifier.height(14.dp))
    ValueCard("Height", "$height", "cm", height.toFloat(), 120f..230f, { onHeight(it.roundToInt()) }, { onHeight(height - 1) }, { onHeight(height + 1) })
    Spacer(Modifier.height(14.dp))
    ValueCard(
        "Weight", "%.1f".format(weight), "kg", weight, 30f..250f,
        { onWeight((it * 2).roundToInt() / 2f) }, { onWeight(weight - 0.5f) }, { onWeight(weight + 0.5f) },
    )
}

@Composable
private fun ValueCard(
    label: String,
    value: String,
    unit: String,
    raw: Float,
    range: ClosedFloatingPointRange<Float>,
    onSlide: (Float) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.card)
            .border(1.dp, AppTheme.colors.stroke, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.muted, modifier = Modifier.weight(1f))
            IconCircleButton(Icons.Rounded.Remove, "Decrease", onMinus, size = 36.dp)
            Row(Modifier.width(110.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted, modifier = Modifier.padding(bottom = 4.dp))
            }
            IconCircleButton(Icons.Rounded.Add, "Increase", onPlus, size = 36.dp)
        }
        Slider(
            value = raw.coerceIn(range),
            onValueChange = onSlide,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Brand.Mint,
                inactiveTrackColor = AppTheme.colors.stroke,
            ),
        )
    }
}

@Composable
private fun ActivityStep(selected: ActivityLevel, onSelect: (ActivityLevel) -> Unit) {
    StepTitle("How active are you?", "Your daily movement changes how much energy you burn.")
    val icons = listOf(Icons.Rounded.Weekend, Icons.Rounded.DirectionsWalk, Icons.Rounded.DirectionsBike, Icons.Rounded.DirectionsRun, Icons.Rounded.Pool)
    ActivityLevel.entries.forEachIndexed { i, level ->
        ChoiceCard(level.label, level.description, icons[i], selected == level) { onSelect(level) }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun GoalStep(selected: Goal, onSelect: (Goal) -> Unit) {
    StepTitle("What's your goal?", "We'll tune your calories and macros for it.")
    val icons = mapOf(Goal.LOSE to Icons.Rounded.TrendingDown, Goal.MAINTAIN to Icons.Rounded.TrendingFlat, Goal.GAIN to Icons.Rounded.TrendingUp)
    Goal.entries.forEach { goal ->
        ChoiceCard(goal.label, goal.description, icons.getValue(goal), selected == goal) { onSelect(goal) }
        Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.FitnessCenter, null, tint = AppTheme.colors.faint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("You can change this any time in Profile.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.faint)
    }
}

@Composable
private fun ChoiceCard(title: String, description: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) Brand.Mint else AppTheme.colors.stroke, tween(250), label = "choiceBorder")
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(scaleDown = 0.98f, onClick = onClick)
            .clip(shape)
            .background(if (selected) Brand.Mint.copy(alpha = 0.08f) else AppTheme.colors.card)
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) Brand.Mint.copy(alpha = 0.18f) else AppTheme.colors.cardElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (selected) Brand.Mint else AppTheme.colors.muted)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) Brand.Mint else AppTheme.colors.stroke, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (selected) Brand.Mint else Color.Transparent),
        )
    }
}

@Composable
private fun PlanStep(calories: Int, protein: Int, carbs: Int, fat: Int, name: String) {
    StepTitle(
        if (name.isBlank()) "Your plan is ready" else "${name.trim()}, your plan is ready",
        "Here's your personalised daily target.",
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ProgressRing(progress = 1f, size = 230.dp, stroke = 18.dp, colors = listOf(Brand.Amber, Brand.Ember, Brand.Amber)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedNumber(calories, MaterialTheme.typography.displayMedium)
                Text("kcal / day", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MacroTarget("Protein", protein, Brand.Protein, Modifier.weight(1f))
        MacroTarget("Carbs", carbs, Brand.Carbs, Modifier.weight(1f))
        MacroTarget("Fat", fat, Brand.Fat, Modifier.weight(1f))
    }
}

@Composable
private fun MacroTarget(label: String, grams: Int, color: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(AppTheme.colors.card)
            .border(1.dp, AppTheme.colors.stroke, shape)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.height(8.dp))
        AnimatedNumber(grams, MaterialTheme.typography.titleLarge, suffix = "g")
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
    }
}
