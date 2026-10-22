package com.calorielens.app.ui.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.BuildConfig
import com.calorielens.app.data.prefs.AppSettings
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.CalorieCalculator
import com.calorielens.app.domain.Gender
import com.calorielens.app.domain.Goal
import com.calorielens.app.domain.ThemeMode
import com.calorielens.app.domain.UserProfile
import com.calorielens.app.domain.WeightUnit
import com.calorielens.app.domain.kgTo
import com.calorielens.app.ui.components.AppCard
import com.calorielens.app.ui.components.AppDialog
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.IconCircleButton
import com.calorielens.app.ui.components.LocalSnackbar
import com.calorielens.app.ui.components.MessageDialog
import com.calorielens.app.ui.components.OutlineButton
import com.calorielens.app.ui.components.PremiumEmblem
import com.calorielens.app.ui.components.ProChip
import com.calorielens.app.ui.components.SnackType
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand

@Composable
fun ProfileScreen(
    settings: AppSettings,
    onPremium: () -> Unit,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    var editing by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    val p = state.profile
    val targets = CalorieCalculator.targets(p)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineLarge)

        AppCard(contentPadding = 20.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brand.primaryGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        p.name.trim().take(1).uppercase().ifBlank { "🙂" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF04140F),
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name.ifBlank { "Your profile" }, style = MaterialTheme.typography.titleLarge)
                        if (settings.isPremium) {
                            Spacer(Modifier.width(8.dp))
                            ProChip()
                        }
                    }
                    Text(
                        "${p.goal.label} · ${targets.calories} kcal/day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.muted,
                    )
                }
                IconCircleButton(Icons.Rounded.Edit, "Edit profile", { editing = true }, size = 40.dp)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val weight = p.weightKg.kgTo(settings.weightUnit)
                MiniStat("Weight", "%.1f".format(weight), settings.weightUnit.label, Modifier.weight(1f))
                MiniStat("Height", "${p.heightCm}", "cm", Modifier.weight(1f))
                MiniStat("BMI", "%.1f".format(CalorieCalculator.bmi(p)), "", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Scans", "${state.totalScans}", "total", Modifier.weight(1f))
                MiniStat("Meals", "${state.mealCount}", "logged", Modifier.weight(1f))
                MiniStat("Protein", "${targets.protein}", "g/day", Modifier.weight(1f))
            }
        }

        if (settings.isPremium) {
            SettingsGroup("Membership") {
                SettingsRow(Icons.Rounded.WorkspacePremium, "Pro membership", settings.premiumPlan?.label?.let { "$it plan · active" } ?: "Active", Brand.Gold, onClick = onPremium)
                SettingsRow(Icons.Rounded.Lock, "Cancel demo subscription", "Return to the free plan", Brand.Danger, onClick = { confirmCancel = true })
            }
        } else {
            PremiumBanner(onPremium)
        }

        SettingsGroup("Appearance") {
            Text("Theme", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.muted, modifier = Modifier.padding(bottom = 8.dp))
            Segmented(ThemeMode.entries.map { it.label }, ThemeMode.entries.indexOf(settings.themeMode)) {
                vm.setTheme(ThemeMode.entries[it])
            }
            Spacer(Modifier.height(16.dp))
            Text("Weight unit", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.muted, modifier = Modifier.padding(bottom = 8.dp))
            Segmented(WeightUnit.entries.map { it.label }, WeightUnit.entries.indexOf(settings.weightUnit)) {
                vm.setWeightUnit(WeightUnit.entries[it])
            }
        }

        SettingsGroup("Privacy & data") {
            SettingsRow(Icons.Rounded.Shield, "On-device only", "Photos and meals never leave your phone", Brand.Mint, onClick = {
                snackbar.show("Recognition runs offline. Nothing is uploaded.", SnackType.INFO, title = "100% private")
            })
            SettingsRow(Icons.Rounded.DeleteForever, "Erase all data", "Delete meals, photos and profile", Brand.Danger, onClick = { confirmErase = true })
        }

        SettingsGroup("About") {
            SettingsRow(Icons.Rounded.Info, "CalorieLens", "Version ${BuildConfig.VERSION_NAME}", Brand.Info, onClick = {
                snackbar.show("Made with Kotlin & Jetpack Compose", SnackType.INFO, title = "CalorieLens ${BuildConfig.VERSION_NAME}")
            })
            SettingsRow(Icons.Rounded.Restaurant, "Nutrition estimates", "Values are approximate for a typical serving", Brand.Fat, onClick = {
                snackbar.show("Estimates come from a bundled nutrition database.", SnackType.INFO)
            })
        }
    }

    if (editing) {
        EditProfileDialog(
            initial = p,
            weightUnit = settings.weightUnit,
            onDismiss = { editing = false },
            onSave = { updated ->
                editing = false
                vm.saveProfile(updated) { kcal ->
                    snackbar.show("Your new daily target is $kcal kcal", SnackType.SUCCESS, title = "Profile updated")
                }
            },
        )
    }
    if (confirmErase) {
        MessageDialog(
            icon = Icons.Rounded.DeleteForever,
            iconColor = Brand.Danger,
            title = "Erase everything?",
            message = "All meals, photos, your profile and Pro status on this device will be permanently deleted. This can't be undone.",
            confirmText = "Erase all data",
            destructive = true,
            onConfirm = {
                confirmErase = false
                vm.eraseEverything()
            },
            dismissText = "Cancel",
            onDismiss = { confirmErase = false },
        )
    }
    if (confirmCancel) {
        MessageDialog(
            icon = Icons.Rounded.WorkspacePremium,
            iconColor = Brand.Gold,
            title = "Cancel Pro?",
            message = "You'll return to the free plan with daily scan limits and ads.",
            confirmText = "Cancel subscription",
            destructive = true,
            onConfirm = {
                confirmCancel = false
                vm.cancelPremium { snackbar.show("You're now on the free plan.", SnackType.INFO) }
            },
            dismissText = "Keep Pro",
            onDismiss = { confirmCancel = false },
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppTheme.colors.cardElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.muted, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun PremiumBanner(onPremium: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(scaleDown = 0.98f, onClick = onPremium)
            .clip(shape)
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2B2416), Color(0xFF110E09))))
            .border(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Brand.GoldLight, Brand.GoldDark)), shape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PremiumEmblem(size = 60.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Upgrade to PRO", style = MaterialTheme.typography.titleLarge.copy(brush = Brand.goldGradient), fontWeight = FontWeight.Black)
            Text("Unlimited scans · full nutrients · no ads", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Brand.Gold)
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.muted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        AppCard(contentPadding = 14.dp, content = content)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .pressScale(scaleDown = 0.98f, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.muted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AppTheme.colors.faint)
    }
}

@Composable
private fun Segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.colors.cardElevated)
            .padding(4.dp),
    ) {
        val segment = maxWidth / labels.size
        val offset by animateDpAsState(segment * selected.coerceAtLeast(0), spring(dampingRatio = 0.75f, stiffness = 500f), label = "seg")
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(Brand.primaryGradient),
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressScale(scaleDown = 0.95f) { onSelect(i) },
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
private fun EditProfileDialog(
    initial: UserProfile,
    weightUnit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    AppDialog(onDismiss = onDismiss) {
        Text("Edit profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it.take(24)) },
                label = { Text("Name") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand.Mint, cursorColor = Brand.Mint, focusedLabelColor = Brand.Mint),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            ChipRow(Gender.entries.map { it.label }, Gender.entries.indexOf(draft.gender)) { draft = draft.copy(gender = Gender.entries[it]) }
            Spacer(Modifier.height(10.dp))
            Stepper("Age", "${draft.age} yrs", { draft = draft.copy(age = (draft.age - 1).coerceAtLeast(13)) }, { draft = draft.copy(age = (draft.age + 1).coerceAtMost(90)) })
            Stepper("Height", "${draft.heightCm} cm", { draft = draft.copy(heightCm = (draft.heightCm - 1).coerceAtLeast(120)) }, { draft = draft.copy(heightCm = (draft.heightCm + 1).coerceAtMost(230)) })
            Stepper(
                "Weight",
                "%.1f %s".format(draft.weightKg.kgTo(weightUnit), weightUnit.label),
                { draft = draft.copy(weightKg = (draft.weightKg - 0.5f).coerceAtLeast(30f)) },
                { draft = draft.copy(weightKg = (draft.weightKg + 0.5f).coerceAtMost(250f)) },
            )
            Spacer(Modifier.height(12.dp))
            Text("Activity", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.muted)
            Spacer(Modifier.height(8.dp))
            ChipRow(ActivityLevel.entries.map { it.label }, ActivityLevel.entries.indexOf(draft.activity)) { draft = draft.copy(activity = ActivityLevel.entries[it]) }
            Spacer(Modifier.height(12.dp))
            Text("Goal", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.muted)
            Spacer(Modifier.height(8.dp))
            ChipRow(Goal.entries.map { it.label }, Goal.entries.indexOf(draft.goal)) { draft = draft.copy(goal = Goal.entries[it]) }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand.Mint.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.MonitorWeight, null, tint = Brand.Mint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New target: ${CalorieCalculator.targets(draft).calories} kcal/day", style = MaterialTheme.typography.titleSmall, color = Brand.Mint)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton("Cancel", onDismiss, Modifier.weight(1f), height = 52.dp)
            GradientButton("Save", { onSave(draft) }, Modifier.weight(1f), height = 52.dp)
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        IconCircleButton(Icons.Rounded.Remove, "Decrease $label", onMinus, size = 34.dp)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconCircleButton(Icons.Rounded.Add, "Increase $label", onPlus, size = 34.dp)
    }
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val isSel = i == selected
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSel) Color(0xFF04140F) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSel) Brand.Mint else AppTheme.colors.card)
                    .border(1.dp, if (isSel) Brand.Mint else AppTheme.colors.stroke, RoundedCornerShape(50))
                    .pressScale(scaleDown = 0.94f) { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}
