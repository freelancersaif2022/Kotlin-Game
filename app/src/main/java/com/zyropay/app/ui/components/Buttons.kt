package com.zyropay.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zyropay.app.ui.theme.Brand
import com.zyropay.app.ui.theme.Z

enum class ButtonStyle { Primary, Secondary, Outline, Danger, Ghost, Light }
enum class ButtonSize(val height: Dp) { Large(54.dp), Medium(46.dp), Small(36.dp) }

@Composable
fun ZButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    size: ButtonSize = ButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
) {
    val c = Z.colors
    val shape = RoundedCornerShape(if (size == ButtonSize.Small) 10.dp else 14.dp)
    val bg: Brush = when (style) {
        ButtonStyle.Primary -> if (enabled) Brand.buttonGradient else SolidColor(if (c.isDark) Color(0xFF2B2550) else Color(0xFFC9BFFB))
        ButtonStyle.Secondary -> SolidColor(c.primarySoft)
        ButtonStyle.Outline, ButtonStyle.Ghost -> SolidColor(Color.Transparent)
        ButtonStyle.Danger -> SolidColor(if (enabled) c.error else c.error.copy(alpha = 0.4f))
        ButtonStyle.Light -> SolidColor(Color.White)
    }
    val fg = when (style) {
        ButtonStyle.Primary, ButtonStyle.Danger -> if (enabled || style == ButtonStyle.Danger) Color.White else Color.White.copy(alpha = 0.85f)
        ButtonStyle.Secondary -> c.onPrimarySoft
        ButtonStyle.Outline -> c.textPrimary
        ButtonStyle.Ghost -> c.primary
        ButtonStyle.Light -> Brand.Primary
    }
    val contentAlpha by animateFloatAsState(if (enabled || style == ButtonStyle.Primary) 1f else 0.5f, label = "btnAlpha")
    Box(
        modifier
            .height(size.height)
            .clip(shape)
            .background(bg)
            .then(if (style == ButtonStyle.Outline) Modifier.border(1.dp, c.outline, shape) else Modifier)
            .pressable(enabled = enabled && !loading, haptic = true, onClick = onClick)
            .padding(horizontal = if (size == ButtonSize.Small) 14.dp else 20.dp)
            .alpha(contentAlpha),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(loading, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "btnLoading") { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = fg, strokeWidth = 2.4.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (icon != null) {
                        ZIcon(icon, size = if (size == ButtonSize.Small) 16.dp else 20.dp, tint = fg)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text,
                        style = if (size == ButtonSize.Small) Z.type.label else Z.type.button,
                        color = fg,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Round quick-action button with label underneath (Deposit / Send / Swap …). */
@Composable
fun QuickAction(
    label: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    background: Color = Z.colors.primarySoft,
    tint: Color = Z.colors.onPrimarySoft,
    labelColor: Color = Z.colors.textPrimary,
    size: Dp = 52.dp,
    onClick: () -> Unit,
) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).pressable(scaleDown = 0.92f, haptic = true, ripple = false, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
            ZIcon(icon, size = 22.dp, tint = tint)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = Z.type.captionMedium, color = labelColor, maxLines = 1)
    }
}

@Composable
fun IconButtonZ(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Z.colors.textPrimary,
    background: Color = Color.Transparent,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(background).pressable(scaleDown = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ZIcon(icon, size = iconSize, tint = tint) }
}
