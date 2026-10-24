package com.pulse.messenger.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF0E9F8A)
val TealDark = Color(0xFF0A7A6A)
val Mint = Color(0xFF34D1B6)
val Amber = Color(0xFFFFB547)
val Ink = Color(0xFF111B21)
val Muted = Color(0xFF667781)
val ReadTick = Color(0xFF53BDEB)
val Danger = Color(0xFFE5484D)
val Success = Color(0xFF1DAA61)

/** Colors that have no slot in the Material color scheme. */
@Immutable
data class PulseColors(
    val chatBackground: Color,
    val bubbleOut: Color,
    val bubbleIn: Color,
    val bubbleOutText: Color,
    val bubbleInText: Color,
    val bubbleMeta: Color,
    val quoteBackground: Color,
    val systemBubble: Color,
    val systemBubbleText: Color,
    val textSecondary: Color,
    val divider: Color,
    val unreadBadge: Color,
    val onUnreadBadge: Color,
    val chip: Color,
    val chipSelected: Color,
    val onChipSelected: Color,
    val inputField: Color,
    val brandTitle: Color,
    val online: Color,
    val readTick: Color,
    val danger: Color,
    val success: Color,
    val isDark: Boolean,
)

val LightPulseColors = PulseColors(
    chatBackground = Color(0xFFEFEAE2),
    bubbleOut = Color(0xFFD5F5EC),
    bubbleIn = Color(0xFFFFFFFF),
    bubbleOutText = Ink,
    bubbleInText = Ink,
    bubbleMeta = Color(0xFF667781),
    quoteBackground = Color(0x0F000000),
    systemBubble = Color(0xFFFFF3D6),
    systemBubbleText = Color(0xFF54656F),
    textSecondary = Muted,
    divider = Color(0xFFE9EDEF),
    unreadBadge = Teal,
    onUnreadBadge = Color.White,
    chip = Color(0xFFF0F2F5),
    chipSelected = Color(0xFFD5F5EC),
    onChipSelected = TealDark,
    inputField = Color(0xFFFFFFFF),
    brandTitle = Teal,
    online = Teal,
    readTick = ReadTick,
    danger = Danger,
    success = Success,
    isDark = false,
)

val DarkPulseColors = PulseColors(
    chatBackground = Color(0xFF0B141A),
    bubbleOut = Color(0xFF0A5C50),
    bubbleIn = Color(0xFF202C33),
    bubbleOutText = Color(0xFFE9EDEF),
    bubbleInText = Color(0xFFE9EDEF),
    bubbleMeta = Color(0xFF8696A0),
    quoteBackground = Color(0x26000000),
    systemBubble = Color(0xFF1D282F),
    systemBubbleText = Color(0xFFFFD279),
    textSecondary = Color(0xFF8696A0),
    divider = Color(0xFF222D34),
    unreadBadge = Mint,
    onUnreadBadge = Color(0xFF0B141A),
    chip = Color(0xFF202C33),
    chipSelected = Color(0xFF103D36),
    onChipSelected = Color(0xFFD5F5EC),
    inputField = Color(0xFF202C33),
    brandTitle = Color(0xFFE9EDEF),
    online = Mint,
    readTick = ReadTick,
    danger = Color(0xFFF2555A),
    success = Mint,
    isDark = true,
)

val LocalPulseColors = staticCompositionLocalOf { LightPulseColors }
