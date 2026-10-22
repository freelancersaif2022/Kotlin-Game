package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import kotlin.math.min
import kotlin.math.sin

/** 7-day login calendar; today's card can be claimed once per day. */
class DailyScreen(game: SnakeAttackGame) : MetaScreen(game, "DAILY REWARD", "ribbon_orange") {
    private val today = Save.dailyIndex
    private var claimed = today < 0
    private var claimAnim = 0f
    private val claimBtn = Button(Assets.r("btn_green"), 360f, "CLAIM", 46f) { claim() }
    private val cards = Array(7) { com.badlogic.gdx.math.Vector2() }
    private val dayLabels = Array(7) { "DAY ${it + 1}" }

    init {
        buttons += claimBtn
        claimBtn.visible = !claimed
        claimBtn.pulse = true
    }

    override fun layout() {
        super.layout()
        val cy = H / 2 + 120f
        for (i in 0 until 6) {
            val col = i % 3
            val row = i / 3
            cards[i].set(W / 2 + (col - 1) * 200f, cy + 150f - row * 240f)
        }
        cards[6].set(W / 2, cy - 330f)
        claimBtn.x = W / 2
        claimBtn.y = 170f
    }

    private fun claim() {
        if (claimed) return
        val r = DailyRewards.days[today]
        DailyRewards.grant(r)
        Save.claimDaily()
        claimed = true
        claimAnim = 0f
        claimBtn.visible = false
        Assets.sfx(if (today == 6) "chest_open" else "reward", 0.9f)
        val c = cards[today]
        particles.sparkle(c.x, c.y, 16, 90f, Palette.GOLD)
        particles.coinsFly(c.x, c.y, if (r.coins > 0) 12 else 4)
        particles.confetti(W / 2, H + 40f, W, 40)
        toast("Reward claimed!", r.label + if (r.coins > 0) " coins" else "", r.icon)
    }

    override fun tick(dt: Float) {
        claimAnim += dt
    }

    override fun content() {
        val b = batch
        val done = Save.dailyClaimedCount
        for (i in 0 until 7) {
            val c = cards[i]
            val r = DailyRewards.days[i]
            val isToday = i == today && !claimed
            val isClaimed = i < done
            val big = i == 6
            val w = if (big) 300f else 170f
            val pop = if (isToday) 1f + sin(time * 5f) * 0.04f else 1f
            if (isToday) {
                b.setColor(1f, 0.9f, 0.4f, 0.6f + 0.3f * sin(time * 5f))
                b.drawCentered(Assets.r("glow"), c.x, c.y, w * 1.7f)
                b.color = Color.WHITE
            }
            val reg = when {
                big -> "ui_calendar_day7_big_card"
                isClaimed -> "ui_calendar_day_card_claimed"
                isToday -> "ui_calendar_day_card_today"
                else -> "ui_calendar_day_card"
            }
            b.drawCentered(Assets.r(reg), c.x, c.y, w * pop)
            b.drawCentered(Assets.r(r.icon), c.x, c.y - 4f, (if (big) 150f else 80f) * pop)
            b.text(Assets.fontBig, dayLabels[i], c.x, c.y + (if (big) 100f else 66f), 28f)
            b.text(Assets.fontBig, r.label, c.x, c.y - (if (big) 92f else 66f), 26f, Palette.GOLD)
            if (isClaimed && !big) b.drawCentered(Assets.r("ui_check_mark_green"), c.x + 50f, c.y + 40f, 54f)
            if (isClaimed && big) b.drawCentered(Assets.r("ui_check_mark_green"), c.x + 100f, c.y + 60f, 70f)
        }
        claimBtn.draw(b)
        if (claimed) {
            val k = min(1f, claimAnim * 3f)
            b.text(Assets.fontBig, "Come back tomorrow!", W / 2, 170f, 40f * Ease.outBack(k), Palette.GOLD)
        }
    }
}
