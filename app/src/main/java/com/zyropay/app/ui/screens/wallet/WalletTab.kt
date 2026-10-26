package com.zyropay.app.ui.screens.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.MockData
import com.zyropay.app.navigation.Routes
import com.zyropay.app.ui.components.AssetRow
import com.zyropay.app.ui.components.ButtonSize
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.CoinPriceRow
import com.zyropay.app.ui.components.DonutChart
import com.zyropay.app.ui.components.IconButtonZ
import com.zyropay.app.ui.components.SearchField
import com.zyropay.app.ui.components.SegmentedTabs
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZSwitch
import com.zyropay.app.ui.components.maskIf
import com.zyropay.app.ui.theme.Z

@Composable
fun WalletTab(nav: NavController) {
    val c = Z.colors
    val prefs by AppRepository.prefs.collectAsState()
    val balances by AppRepository.balances.collectAsState()
    val prices by AppRepository.prices.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var hideZero by rememberSaveable { mutableStateOf(false) }
    val hidden = prefs.hideBalance

    val holdings = MockData.coins.map { coin ->
        val bal = balances[coin.symbol] ?: 0.0
        Triple(coin, bal, bal * (prices[coin.symbol]?.usd ?: coin.basePrice))
    }.sortedByDescending { it.third }
    val total = holdings.sumOf { it.third }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "title") {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Wallet", style = Z.type.h1, color = c.textPrimary, modifier = Modifier.weight(1f))
                IconButtonZ(R.drawable.ic_history, onClick = { nav.navigate(Routes.transactions()) })
                IconButtonZ(R.drawable.ic_p2p, onClick = { nav.navigate(Routes.P2P) })
            }
        }
        item(key = "summary") {
            SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(
                            Modifier.clip(RoundedCornerShape(6.dp)).clickable { AppRepository.updatePrefs { it.copy(hideBalance = !it.hideBalance) } },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Crypto balance", style = Z.type.bodySmall, color = c.textSecondary)
                            Spacer(Modifier.width(6.dp))
                            ZIcon(if (hidden) R.drawable.ic_eye_off else R.drawable.ic_eye, size = 16.dp, tint = c.textSecondary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(maskIf(hidden, Fmt.fiat(total, prefs.currency)), style = Z.type.h1.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(maskIf(hidden, "≈ " + Fmt.coin(total / (prices["USDT"]?.usd ?: 1.0), "USDT")), style = Z.type.caption, color = c.textTertiary)
                    }
                    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                        DonutChart(holdings.filter { it.third > 0 }.take(6).map { it.third.toFloat() to it.first.color }, Modifier.size(84.dp))
                        Text("${holdings.count { it.second > 0 }}", style = Z.type.h3, color = c.textPrimary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    holdings.filter { it.third > 0 }.take(4).forEach { (coin, _, usd) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(coin.color))
                            Spacer(Modifier.width(4.dp))
                            Text("${coin.symbol} ${if (total > 0) (usd / total * 100).toInt() else 0}%", style = Z.type.caption, color = c.textSecondary, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZButton("Deposit", onClick = { nav.navigate(Routes.selectCoin("deposit")) }, modifier = Modifier.weight(1f), size = ButtonSize.Medium, icon = R.drawable.ic_deposit)
                    ZButton("Withdraw", onClick = { nav.navigate(Routes.selectCoin("withdraw")) }, modifier = Modifier.weight(1f), size = ButtonSize.Medium, style = ButtonStyle.Secondary, icon = R.drawable.ic_withdraw)
                    ZButton("Swap", onClick = { nav.navigate(Routes.swap()) }, modifier = Modifier.weight(0.8f), size = ButtonSize.Medium, style = ButtonStyle.Secondary)
                }
            }
        }
        item(key = "tabs") {
            SegmentedTabs(listOf("My assets", "Markets"), tab, Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) { tab = it }
        }
        item(key = "search") {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SearchField(query, { query = it }, placeholder = "Search coin")
                AnimatedVisibility(tab == 0) {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Hide zero balances", style = Z.type.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
                        ZSwitch(hideZero, { hideZero = it })
                    }
                }
            }
        }
        val filtered = holdings.filter { (coin, bal, _) ->
            (query.isBlank() || coin.symbol.contains(query, true) || coin.name.contains(query, true)) && (tab == 1 || !hideZero || bal > 0)
        }
        if (filtered.isEmpty()) {
            item(key = "none") {
                Text("No coins match \"$query\"", style = Z.type.bodySmall, color = c.textSecondary, modifier = Modifier.fillMaxWidth().padding(32.dp))
            }
        }
        items(filtered, key = { "${tab}_${it.first.symbol}" }) { (coin, bal, _) ->
            val p = prices[coin.symbol]
            if (tab == 0) {
                AssetRow(coin.symbol, bal, p?.usd ?: coin.basePrice, hidden, prefs.currency, p?.change24h ?: 0.0, Modifier.padding(horizontal = 4.dp)) {
                    nav.navigate(Routes.coin(coin.symbol))
                }
            } else {
                CoinPriceRow(coin.symbol, p, Modifier.padding(horizontal = 4.dp)) { nav.navigate(Routes.coin(coin.symbol)) }
            }
        }
    }
}
