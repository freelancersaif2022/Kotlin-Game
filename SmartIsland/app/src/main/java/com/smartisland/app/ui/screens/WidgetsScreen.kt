package com.smartisland.app.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartisland.app.ads.RewardOutcome
import com.smartisland.app.ui.appContainer
import com.smartisland.app.ui.components.AppDialog
import com.smartisland.app.ui.components.GhostButton
import com.smartisland.app.ui.components.GlassCard
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.PrimaryButton
import com.smartisland.app.ui.components.ProBadge
import com.smartisland.app.ui.components.SnackType
import com.smartisland.app.ui.components.StatusChip
import com.smartisland.app.ui.components.TabHeader
import com.smartisland.app.ui.components.pressable
import com.smartisland.app.ui.findActivity
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.navigation.Routes
import com.smartisland.app.ui.navigation.TabContentBottomPadding
import com.smartisland.app.ui.theme.Palette
import com.smartisland.app.widget.WidgetKind
import com.smartisland.app.widget.WidgetRenderer
import com.smartisland.app.widget.WidgetSize
import com.smartisland.app.widget.WidgetUpdater

private enum class Filter(val label: String) { ALL("All"), FREE("Free"), PREMIUM("Premium") }

@Composable
fun WidgetsScreen(nav: AppNavigator, highlight: String?, onHighlightShown: () -> Unit) {
    val container = appContainer()
    val context = LocalContext.current
    val snack = LocalSnack.current
    val premium by container.premium.state.collectAsStateWithLifecycle()
    val adReady by container.ads.rewardedReady.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf(Filter.ALL) }
    var unlockTarget by remember { mutableStateOf<WidgetKind?>(null) }
    var manualAddTarget by remember { mutableStateOf<WidgetKind?>(null) }
    val listState = rememberLazyListState()

    val kinds = WidgetKind.entries.filter {
        when (filter) {
            Filter.ALL -> true
            Filter.FREE -> !it.premium
            Filter.PREMIUM -> it.premium
        }
    }

    LaunchedEffect(highlight) {
        val target = WidgetKind.byId(highlight) ?: return@LaunchedEffect
        filter = Filter.ALL
        listState.animateScrollToItem(WidgetKind.entries.indexOf(target) + 1)
        if (!premium.isWidgetUnlocked(target.id, target.premium)) unlockTarget = target
        onHighlightShown()
    }
    LaunchedEffect(Unit) { container.ads.preload() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = TabContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TabHeader("Widgets", "${WidgetKind.entries.size} live widgets")
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filter.entries.forEach { f -> FilterChip(f.label, f == filter) { filter = f } }
            }
        }
        items(kinds, key = { it.id }) { kind ->
            val unlocked = premium.isWidgetUnlocked(kind.id, kind.premium)
            WidgetCard(
                kind = kind,
                unlocked = unlocked,
                unlockedByAd = kind.premium && !premium.isPremium && kind.id in premium.unlockedWidgets,
                modifier = Modifier.padding(horizontal = 16.dp),
                onAdd = {
                    if (!pinWidget(context, kind)) manualAddTarget = kind
                },
                onUnlock = { unlockTarget = kind },
            )
        }
    }

    unlockTarget?.let { kind ->
        AppDialog(
            onDismiss = { unlockTarget = null },
            icon = Icons.Rounded.LockOpen,
            title = "Unlock ${kind.title}",
            message = "Watch one short video to unlock this widget forever, or go PRO to unlock all 12 widgets and every premium feature.",
            confirmText = if (adReady) "Watch ad · unlock forever" else "Loading ad…",
            accent = Palette.Primary,
            onConfirm = {
                val activity = context.findActivity() ?: return@AppDialog
                container.ads.showRewarded(activity) { outcome ->
                    when (outcome) {
                        RewardOutcome.REWARDED -> {
                            container.premium.unlockWidget(kind.id)
                            WidgetUpdater.update(context, kind)
                            unlockTarget = null
                            snack.show("${kind.title} unlocked forever", SnackType.SUCCESS, "Add") {
                                if (!pinWidget(context, kind)) manualAddTarget = kind
                            }
                        }
                        RewardOutcome.SKIPPED -> snack.error("Watch the full video to unlock")
                        RewardOutcome.NOT_READY -> snack.show("Ad is still loading, try again in a moment")
                        RewardOutcome.FAILED -> snack.error("Couldn't show the ad. Check your connection.")
                    }
                }
            },
            dismissText = "Go PRO instead",
            onDismissClick = {
                unlockTarget = null
                nav.go(Routes.PAYWALL)
            },
        )
    }

    manualAddTarget?.let { kind ->
        AppDialog(
            onDismiss = { manualAddTarget = null },
            icon = Icons.Rounded.Widgets,
            title = "Add ${kind.title}",
            message = "Your launcher doesn't support one-tap adding. Long-press an empty spot on your home screen → Widgets → Smart Island → ${kind.title}.",
            confirmText = "Got it",
            onConfirm = { manualAddTarget = null },
            dismissText = null,
        )
    }
}

@Composable
private fun WidgetCard(
    kind: WidgetKind,
    unlocked: Boolean,
    unlockedByAd: Boolean,
    modifier: Modifier,
    onAdd: () -> Unit,
    onUnlock: () -> Unit,
) {
    GlassCard(modifier, padding = 14.dp) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Palette.WallpaperGradient)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val (w, h) = previewSize(kind.size)
            // Real RemoteViews rendered in-app, so the preview is pixel-identical to the home screen.
            key(kind, unlocked) {
                AndroidView(
                    factory = { ctx -> FrameLayout(ctx) },
                    update = { frame ->
                        frame.removeAllViews()
                        val views = WidgetRenderer.render(frame.context, kind, forceUnlocked = true)
                        runCatching { frame.addView(views.apply(frame.context.applicationContext, frame)) }
                    },
                    modifier = Modifier
                        .then(if (w == Dp.Unspecified) Modifier.fillMaxWidth() else Modifier.width(w))
                        .height(h),
                )
            }
            // Swallow taps so preview buttons don't fire real actions.
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(kind.title, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
            Text(kind.size.label, style = MaterialTheme.typography.labelMedium, color = Palette.TextTertiary)
            Spacer(Modifier.width(8.dp))
            when {
                !kind.premium -> StatusChip("Free", Palette.Success)
                unlockedByAd -> StatusChip("Unlocked", Palette.Secondary)
                unlocked -> StatusChip("PRO", Palette.Gold)
                else -> ProBadge()
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(kind.description, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        Spacer(Modifier.height(12.dp))
        if (unlocked) {
            PrimaryButton("Add to home screen", onAdd, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.AddToHomeScreen)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Watch ad", onUnlock, Modifier.weight(1f), Icons.Rounded.OndemandVideo, Palette.PrimarySoft)
                PrimaryButton("Unlock", onUnlock, Modifier.weight(1f), icon = Icons.Rounded.WorkspacePremium, color = Color(0xFFB8860B))
            }
        }
    }
}

private fun previewSize(size: WidgetSize): Pair<Dp, Dp> = when (size) {
    WidgetSize.SMALL -> 170.dp to 170.dp
    WidgetSize.WIDE -> Dp.Unspecified to 96.dp
    WidgetSize.LARGE -> Dp.Unspecified to 176.dp
}

private fun pinWidget(context: Context, kind: WidgetKind): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return runCatching { manager.requestPinAppWidget(ComponentName(context, kind.provider), null, null) }.getOrDefault(false)
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.Primary else Palette.Surface, label = "filter")
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else Palette.Outline, shape)
            .pressable(onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else Palette.TextSecondary)
    }
}
