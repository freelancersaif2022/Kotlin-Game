package com.zyropay.app.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.navigation.Routes
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.pressable
import com.zyropay.app.ui.screens.card.CardTab
import com.zyropay.app.ui.screens.home.HomeTab
import com.zyropay.app.ui.screens.profile.ProfileTab
import com.zyropay.app.ui.screens.wallet.WalletTab
import com.zyropay.app.ui.theme.Brand
import com.zyropay.app.ui.theme.Z

enum class Tab(val label: String, val icon: Int, val iconSelected: Int) {
    HOME("Home", R.drawable.ic_home, R.drawable.ic_home_filled),
    CARD("Card", R.drawable.ic_card, R.drawable.ic_card_filled),
    PAY("Pay", R.drawable.ic_scan_qr, R.drawable.ic_scan_qr),
    WALLET("Wallet", R.drawable.ic_wallet, R.drawable.ic_wallet_filled),
    ME("Me", R.drawable.ic_profile, R.drawable.ic_profile_filled),
}

@Composable
fun MainScreen(nav: NavController) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = Tab.entries[tabIndex]
    val holder = rememberSaveableStateHolder()
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    var lastBack by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (tab != Tab.HOME) {
            tabIndex = 0
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBack < 2000) (context as? android.app.Activity)?.finish()
            else {
                lastBack = now
                messenger.toast("Press back again to exit")
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Z.colors.background)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                tab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "tabs",
            ) { t ->
                holder.SaveableStateProvider(t.name) {
                    when (t) {
                        Tab.HOME -> HomeTab(nav, onSwitchTab = { tabIndex = it.ordinal })
                        Tab.CARD -> CardTab(nav)
                        Tab.WALLET -> WalletTab(nav)
                        Tab.ME -> ProfileTab(nav)
                        Tab.PAY -> Unit
                    }
                }
            }
        }
        BottomBar(tab, onSelect = { t ->
            if (t == Tab.PAY) nav.navigate(Routes.SCAN) else tabIndex = t.ordinal
        })
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val c = Z.colors
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(if (c.isDark) 0.dp else 14.dp, ambientColor = Color(0x22000000), spotColor = Color(0x33000000))
            .background(c.surface)
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider.copy(alpha = if (c.isDark) 1f else 0.6f)))
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab.entries.forEach { t ->
                if (t == Tab.PAY) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .offset(y = (-14).dp)
                                .size(58.dp)
                                .shadow(14.dp, CircleShape, ambientColor = Brand.Primary, spotColor = Brand.Primary)
                                .clip(CircleShape)
                                .background(Brand.accentGradient)
                                .pressable(scaleDown = 0.9f, haptic = true) { onSelect(t) },
                            contentAlignment = Alignment.Center,
                        ) { ZIcon(R.drawable.ic_scan_qr, size = 26.dp, tint = Color.White) }
                        Text("Pay", style = Z.type.captionMedium, color = c.textSecondary, modifier = Modifier.align(Alignment.BottomCenter).offset(y = 2.dp).padding(bottom = 6.dp))
                    }
                } else {
                    val active = t == selected
                    val color by animateColorAsState(if (active) c.primary else c.textTertiary, tween(200), label = "navColor")
                    val scale by animateFloatAsState(if (active) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "navScale")
                    Column(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable(interactionSource = null, indication = null) {
                                if (!active) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(t)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        ZIcon(if (active) t.iconSelected else t.icon, size = 24.dp, tint = color, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
                        Spacer(Modifier.height(4.dp))
                        Text(t.label, style = Z.type.captionMedium.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium), color = color)
                    }
                }
            }
        }
    }
}
