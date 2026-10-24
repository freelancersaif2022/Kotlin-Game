@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.pdfmaster.app.premium

import androidx.compose.runtime.Immutable
import com.pdfmaster.app.data.prefs.AppPreferences
import com.pdfmaster.app.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class PremiumState(
    val purchased: Boolean = false,
    val passUntil: Long = 0L,
    val now: Long = System.currentTimeMillis(),
    val usedToday: Int = 0,
) {
    val passRemaining: Long get() = (passUntil - now).coerceAtLeast(0L)
    val passActive: Boolean get() = !purchased && passRemaining > 0
    val isPremium: Boolean get() = purchased || passRemaining > 0
    val passProgress: Float get() = (passRemaining.toFloat() / PremiumManager.PASS_DURATION_MS).coerceIn(0f, 1f)
    val freeLeft: Int get() = (PremiumManager.FREE_DAILY_LIMIT - usedToday).coerceAtLeast(0)
}

sealed interface PremiumEvent {
    data object PassStarted : PremiumEvent
    data object PassExtended : PremiumEvent
    data object PassExpired : PremiumEvent
    data object Purchased : PremiumEvent
}

/**
 * Single source of truth for Premium. Premium comes from a Play purchase or from a
 * time-limited pass unlocked by watching a rewarded ad.
 */
@Singleton
class PremiumManager @Inject constructor(
    private val prefs: AppPreferences,
    @AppScope private val scope: CoroutineScope,
) {
    companion object {
        const val PASS_DURATION_MS = 5L * 60 * 60 * 1000
        const val MAX_PASS_MS = 24L * 60 * 60 * 1000
        const val FREE_DAILY_LIMIT = 10
        const val FREE_IMAGE_LIMIT = 10
    }

    private val _events = MutableSharedFlow<PremiumEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PremiumEvent> = _events

    /** Ticks once a second only while a pass is running; otherwise emits a single value. */
    private val clock = prefs.passUntil.flatMapLatest { until ->
        flow {
            while (true) {
                val now = System.currentTimeMillis()
                emit(now)
                if (now >= until) break
                delay(1000L - now % 1000L)
            }
        }
    }

    val state: StateFlow<PremiumState> = combine(prefs.purchased, prefs.passUntil, clock, prefs.usageToday) { purchased, until, now, used ->
        PremiumState(purchased, until, now, used)
    }.stateIn(scope, SharingStarted.Eagerly, PremiumState())

    val isPremium: Boolean get() = state.value.isPremium

    init {
        scope.launch {
            var wasActive = state.value.passActive
            state.collect { s ->
                if (wasActive && !s.passActive && !s.purchased) _events.emit(PremiumEvent.PassExpired)
                wasActive = s.passActive
            }
        }
    }

    suspend fun grantRewardPass() {
        val now = System.currentTimeMillis()
        val current = prefs.currentPassUntil()
        val active = current > now
        val newUntil = (maxOf(now, current) + PASS_DURATION_MS).coerceAtMost(now + MAX_PASS_MS)
        prefs.setPassUntil(newUntil)
        _events.emit(if (active) PremiumEvent.PassExtended else PremiumEvent.PassStarted)
    }

    suspend fun setPurchased(value: Boolean) {
        val was = prefs.purchased.first()
        prefs.setPurchased(value)
        if (value && !was) _events.emit(PremiumEvent.Purchased)
    }

    /** True if the user may run one more standard operation today. */
    fun canRunOperation(): Boolean = state.value.isPremium || state.value.usedToday < FREE_DAILY_LIMIT

    suspend fun recordOperation() {
        if (!state.value.isPremium) prefs.incrementUsage()
    }
}
