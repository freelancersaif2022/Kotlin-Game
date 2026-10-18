package com.smartisland.app.data

/**
 * Everything the user can tune about the island. Sizes are in dp so they survive density changes.
 */
data class IslandSettings(
    val enabled: Boolean = true,
    val widthDp: Float = DEFAULT_WIDTH,
    val heightDp: Float = DEFAULT_HEIGHT,
    val topOffsetDp: Float = DEFAULT_TOP,
    val xOffsetDp: Float = 0f,
    val expandedWidthDp: Float = DEFAULT_EXPANDED_WIDTH,
    val roundness: Float = 1f,
    val showMedia: Boolean = true,
    val showCalls: Boolean = true,
    val showCharging: Boolean = true,
    val showNotifications: Boolean = false,
    val showRinger: Boolean = true,
    val replaceVolumePanel: Boolean = false,
    val hideInLandscape: Boolean = true,
    val haptics: Boolean = true,
    val glow: Boolean = false,
    val accentColor: Int = DEFAULT_ACCENT,
    val edgePanelEnabled: Boolean = false,
    val edgePanelRight: Boolean = true,
    val onboardingDone: Boolean = false,
) {
    companion object {
        const val DEFAULT_WIDTH = 118f
        const val DEFAULT_HEIGHT = 34f
        const val DEFAULT_TOP = 10f
        const val DEFAULT_EXPANDED_WIDTH = 372f
        const val DEFAULT_ACCENT = 0xFF7C5CFF.toInt()

        val WIDTH_RANGE = 70f..220f
        val HEIGHT_RANGE = 24f..56f
        val TOP_RANGE = 0f..80f
        val X_RANGE = -160f..160f
        val EXPANDED_RANGE = 280f..420f

        val ACCENTS = listOf(
            0xFF7C5CFF.toInt(), 0xFF0A84FF.toInt(), 0xFF34C759.toInt(), 0xFFFF9F0A.toInt(),
            0xFFFF375F.toInt(), 0xFF64D2FF.toInt(), 0xFFBF5AF2.toInt(), 0xFFFFD60A.toInt(),
        )
    }
}
