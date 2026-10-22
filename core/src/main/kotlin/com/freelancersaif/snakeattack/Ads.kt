package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import kotlin.math.ceil
import kotlin.math.min

/**
 * Rewarded video ads. [provider] is the integration point for a real ad network
 * (e.g. AdMob rewarded ads on Android): it must call onReward when the user
 * finished watching and onClosed in every case. Until one is plugged in, the
 * built-in placeholder shows a short countdown overlay and then rewards.
 */
object Ads {
    interface Provider {
        fun showRewarded(onReward: () -> Unit, onClosed: () -> Unit)
    }

    var provider: Provider? = null

    private const val PLACEHOLDER_SECONDS = 3f
    private var playing = false
    private var t = 0f
    private var pendingReward: (() -> Unit)? = null
    private var rewardFlash = 0f
    private var rewardText = ""

    val showing get() = playing

    /** Plays a rewarded ad; [onReward] runs once it completes. [label] is shown after the reward. */
    fun showRewarded(label: String, onReward: () -> Unit) {
        if (playing) return
        val p = provider
        Assets.pauseMusic()
        if (p != null) {
            playing = true
            p.showRewarded(
                onReward = { grant(label, onReward) },
                onClosed = { playing = false; Assets.refreshMusic() },
            )
        } else {
            playing = true
            t = 0f
            pendingReward = { grant(label, onReward) }
        }
    }

    private fun grant(label: String, onReward: () -> Unit) {
        onReward()
        rewardText = label
        rewardFlash = 1.8f
        Assets.sfx("reward", 0.9f)
    }

    fun update(dt: Float) {
        rewardFlash = maxOf(0f, rewardFlash - dt)
        if (!playing || provider != null) return
        t += dt
        if (t >= PLACEHOLDER_SECONDS) {
            playing = false
            pendingReward?.invoke()
            pendingReward = null
            Assets.refreshMusic()
        }
    }

    /** Placeholder ad screen and the "reward received" banner (drawn over everything). */
    fun draw(b: Batch, w: Float, h: Float, time: Float) {
        if (playing && provider == null) {
            b.fillRect(-20f, -20f, w + 40f, h + 40f, 0.03f, 0.03f, 0.06f, 0.94f)
            b.drawCentered(Assets.r("ui_video_ad_icon"), w / 2, h / 2 + 120f, 190f)
            b.drawCentered(Assets.r("ui_loading_spinner"), w / 2, h / 2 - 90f, 120f, rotation = -time * 360f)
            val left = ceil(PLACEHOLDER_SECONDS - t).toInt().coerceAtLeast(1)
            b.text(Assets.fontBig, DIGITS[min(left, 9)], w / 2, h / 2 - 88f, 46f)
            b.text(Assets.fontBig, "WATCHING AD...", w / 2, h / 2 - 200f, 40f, Palette.GOLD)
            b.text(Assets.fontPlain, "Your reward arrives when the video ends", w / 2, h / 2 - 260f, 26f, Color.WHITE)
        }
        if (rewardFlash > 0f) {
            val k = Ease.outBack(min(1f, (1.8f - rewardFlash) * 4f))
            val a = min(1f, rewardFlash * 2f)
            b.setColor(1f, 1f, 1f, a)
            b.drawCentered(Assets.r("ui_ad_reward_burst"), w / 2, h / 2, 360f * k, rotation = time * 40f)
            b.color = Color.WHITE
            b.text(Assets.fontBig, rewardText, w / 2, h / 2, 54f * k, Palette.GOLD, alpha = a)
        }
    }

    private val DIGITS = Array(10) { it.toString() }
}

/**
 * In-app coin purchases. [provider] is the integration point for Google Play
 * Billing; without one (desktop / test builds) purchases complete immediately.
 */
object Billing {
    interface Provider {
        fun buy(productId: String, onSuccess: () -> Unit)
    }

    var provider: Provider? = null

    class CoinPack(val id: String, val title: String, val coins: Int, val price: String, val icon: String, val tag: String = "")

    val packs = listOf(
        CoinPack("coins_500", "Handful", 500, "$0.99", "ui_coin_pack_small"),
        CoinPack("coins_1200", "Pile", 1200, "$1.99", "ui_coin_pack_medium"),
        CoinPack("coins_2800", "Heap", 2800, "$3.99", "ui_coin_pack_large", "POPULAR"),
        CoinPack("coins_6500", "Bag", 6500, "$7.99", "ui_coin_pack_bag"),
        CoinPack("coins_15000", "Chest", 15000, "$14.99", "ui_coin_pack_chest", "BEST VALUE"),
        CoinPack("coins_40000", "Vault", 40000, "$29.99", "ui_coin_pack_mega"),
    )

    const val AD_COINS = 100

    fun buy(pack: CoinPack, onSuccess: () -> Unit) {
        val p = provider
        val done = {
            Save.coins += pack.coins
            onSuccess()
        }
        if (p != null) p.buy(pack.id, done) else done()
    }
}
