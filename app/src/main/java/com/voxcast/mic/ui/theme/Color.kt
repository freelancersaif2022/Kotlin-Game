package com.voxcast.mic.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object VoxColors {
    val Background = Color(0xFF07090E)
    val BackgroundTop = Color(0xFF0E1422)
    val Surface = Color(0xFF11151D)
    val SurfaceHigh = Color(0xFF1A1F2B)
    val SurfaceHighest = Color(0xFF232937)
    val Outline = Color(0xFF2A3142)
    val OutlineSoft = Color(0x1FFFFFFF)

    val Cyan = Color(0xFF35D0FF)
    val CyanDeep = Color(0xFF0A8FD6)
    val Violet = Color(0xFF7B6CFF)
    val Gold = Color(0xFFF4C46B)
    val GoldDeep = Color(0xFFB8863A)
    val Success = Color(0xFF3DDC97)
    val Warning = Color(0xFFFFB547)
    val Error = Color(0xFFFF5C6C)
    val Live = Color(0xFFFF3B4E)

    val Text = Color(0xFFEEF2FA)
    val TextDim = Color(0xFF9AA3B5)
    val TextFaint = Color(0xFF5E677A)

    val PrimaryGradient = Brush.linearGradient(listOf(Cyan, Violet))
    val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFE3A3), Gold, GoldDeep))
    val LiveGradient = Brush.linearGradient(listOf(Color(0xFFFF6A5C), Live))
    val SuccessGradient = Brush.linearGradient(listOf(Color(0xFF6CF0B8), Success))
}
