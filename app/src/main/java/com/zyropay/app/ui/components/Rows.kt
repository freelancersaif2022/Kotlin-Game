package com.zyropay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zyropay.app.R
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.MockData
import com.zyropay.app.data.Price
import com.zyropay.app.data.Transaction
import com.zyropay.app.data.TxStatus
import com.zyropay.app.data.TxType
import com.zyropay.app.ui.theme.Z

fun txIcon(t: Transaction): Int = when (t.type) {
    TxType.DEPOSIT -> R.drawable.ic_arrow_down_left
    TxType.WITHDRAW, TxType.SEND -> R.drawable.ic_arrow_up_right
    TxType.RECEIVE -> R.drawable.ic_arrow_down_left
    TxType.SWAP -> R.drawable.ic_swap
    TxType.CARD_TOP_UP -> R.drawable.ic_top_up
    TxType.REFUND -> R.drawable.ic_category_refund
    TxType.CASHBACK -> R.drawable.ic_category_cashback
    TxType.P2P_BUY, TxType.P2P_SELL -> R.drawable.ic_p2p
    TxType.PAY -> R.drawable.ic_scan_qr
    TxType.CARD_FEE -> R.drawable.ic_card
    TxType.REWARD -> R.drawable.ic_gift
    TxType.CARD_PAYMENT -> t.category?.icon ?: R.drawable.ic_card
}

@Composable
fun TxLeading(t: Transaction, size: androidx.compose.ui.unit.Dp = 42.dp) {
    val c = Z.colors
    val merchantIcon = MockData.merchantIcon(t.merchant)
    Box(Modifier.size(size)) {
        when {
            merchantIcon != null -> ZImage(merchantIcon, Modifier.size(size).clip(CircleShape))
            t.type == TxType.DEPOSIT || t.type == TxType.WITHDRAW || t.type == TxType.SWAP -> CoinIcon(t.coin, size = size)
            else -> {
                val (bg, tint) = when (t.type) {
                    TxType.CASHBACK, TxType.REWARD, TxType.REFUND, TxType.RECEIVE -> c.successSoft to c.success
                    TxType.CARD_FEE -> c.warningSoft to c.warning
                    else -> c.primarySoft to c.primary
                }
                IconCircle(txIcon(t), size = size, iconSize = size * 0.46f, background = bg, tint = tint)
            }
        }
        if (t.type == TxType.DEPOSIT || t.type == TxType.WITHDRAW || t.type == TxType.SWAP) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(size * 0.46f).clip(CircleShape)
                    .background(c.surface).padding(2.dp).clip(CircleShape)
                    .background(if (t.type == TxType.DEPOSIT) c.success else if (t.type == TxType.SWAP) c.primary else c.textPrimary),
                contentAlignment = Alignment.Center,
            ) { ZIcon(txIcon(t), size = size * 0.26f, tint = if (t.type == TxType.WITHDRAW) c.surface else Color.White) }
        }
        if (t.status == TxStatus.PENDING) {
            Box(Modifier.align(Alignment.TopEnd).size(12.dp).clip(CircleShape).background(c.surface).padding(2.dp).clip(CircleShape).background(c.warning))
        }
    }
}

@Composable
fun TxRow(t: Transaction, hidden: Boolean, currency: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Z.colors
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TxLeading(t)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, style = Z.type.bodyMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Fmt.shortDate(t.timestamp), style = Z.type.caption, color = c.textSecondary, maxLines = 1)
                if (t.status != TxStatus.SUCCESS) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (t.status == TxStatus.PENDING) "· Processing" else "· Failed", style = Z.type.captionMedium,
                        color = if (t.status == TxStatus.PENDING) c.warning else c.error,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            val primary = when {
                t.type == TxType.SWAP && t.toCoin != null && t.toAmount != null -> Fmt.coin(t.toAmount, t.toCoin, signed = true)
                t.coin == "USD" -> Fmt.fiat(t.amount, currency, signed = true)
                else -> Fmt.coin(t.amount, t.coin, signed = true)
            }
            Text(
                maskIf(hidden, primary),
                style = Z.type.number.copy(fontSize = 15.sp),
                color = when {
                    t.status == TxStatus.FAILED -> c.textTertiary
                    t.isIncoming || t.type == TxType.SWAP -> c.success
                    else -> c.textPrimary
                },
                textAlign = TextAlign.End,
            )
            Spacer(Modifier.height(2.dp))
            val secondary = when {
                t.type == TxType.SWAP -> Fmt.coin(t.amount, t.coin, signed = true)
                t.coin == "USD" -> t.category?.label ?: t.type.label
                else -> "≈ " + Fmt.fiat(t.usdValue, currency)
            }
            Text(maskIf(hidden, secondary), style = Z.type.caption, color = c.textTertiary, maxLines = 1)
        }
    }
}

@Composable
fun CoinPriceRow(symbol: String, price: Price?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Z.colors
    val coin = MockData.coin(symbol)
    val up = (price?.change24h ?: 0.0) >= 0
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(symbol, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(coin.symbol, style = Z.type.bodyMedium, color = c.textPrimary)
            Text(coin.name, style = Z.type.caption, color = c.textSecondary)
        }
        if (price != null) {
            Sparkline(price.history.takeLast(32), if (up) c.success else c.error, Modifier.width(64.dp).height(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(96.dp)) {
            Text(Fmt.price(price?.usd ?: coin.basePrice), style = Z.type.numberSmall, color = c.textPrimary, maxLines = 1)
            Text(Fmt.percent(price?.change24h ?: 0.0), style = Z.type.captionMedium, color = if (up) c.success else c.error)
        }
    }
}

@Composable
fun AssetRow(symbol: String, balance: Double, priceUsd: Double, hidden: Boolean, currency: String, change: Double, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Z.colors
    val coin = MockData.coin(symbol)
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(symbol, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(coin.symbol, style = Z.type.bodyMedium, color = c.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Fmt.price(priceUsd), style = Z.type.caption, color = c.textSecondary)
                Spacer(Modifier.width(6.dp))
                Text(Fmt.percent(change), style = Z.type.captionMedium, color = if (change >= 0) c.success else c.error)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(maskIf(hidden, Fmt.coin(balance, symbol, withSymbol = false)), style = Z.type.number, color = c.textPrimary)
            Text(maskIf(hidden, "≈ " + Fmt.fiat(balance * priceUsd, currency)), style = Z.type.caption, color = c.textSecondary)
        }
    }
}
