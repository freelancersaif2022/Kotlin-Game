package com.voxcast.mic.data

enum class NoiseMode(val label: String, val description: String) {
    OFF("Off", "Raw microphone signal"),
    STANDARD("Standard", "System noise suppression and rumble filter"),
    ULTRA("Ultra Clean", "Adaptive noise gate: complete silence between words"),
}

enum class AudioQuality(val sampleRate: Int, val label: String, val premium: Boolean) {
    VOICE(16_000, "Voice · 16 kHz", false),
    HD(24_000, "HD Voice · 24 kHz", false),
    STUDIO(48_000, "Studio · 48 kHz", true),
}

data class AppSettings(
    val deviceName: String = "",
    val noiseMode: NoiseMode = NoiseMode.ULTRA,
    val echoCancel: Boolean = true,
    val autoGain: Boolean = false,
    val voiceEnhance: Boolean = true,
    val micGainDb: Float = 0f,
    val quality: AudioQuality = AudioQuality.HD,
    val pushToTalk: Boolean = false,
    val keepScreenOn: Boolean = true,
    val haptics: Boolean = true,
    val speakerBoost: Float = 1f,
    val premium: PremiumState = PremiumState(),
)

data class PremiumState(
    val active: Boolean = false,
    val autoRenew: Boolean = false,
    val renewsAt: Long = 0L,
    val purchasedAt: Long = 0L,
) {
    /** Active while auto-renewing, or until the paid period runs out after cancelling. */
    val isEntitled: Boolean
        get() = active && (autoRenew || System.currentTimeMillis() < renewsAt)
}
