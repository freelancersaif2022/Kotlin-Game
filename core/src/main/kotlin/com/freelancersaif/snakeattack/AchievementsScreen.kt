package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Scrollable list of achievements with progress bars and coin rewards to claim. */
class AchievementsScreen(game: SnakeAttackGame) : MetaScreen(game, "AWARDS", "ribbon_green") {
    private var scroll = 0f
    private var velocity = 0f
    private val rowH = 150f
    private var listTop = 0f
    private val progressText = HashMap<String, String>()

    override fun layout() {
        super.layout()
        listTop = top - 140f
    }

    private fun rowY(i: Int) = listTop - rowH / 2 - i * (rowH + 12f) + scroll

    private fun maxScroll() = max(0f, Achievements.all.size * (rowH + 12f) - (listTop - 30f))

    override fun onDrag(dx: Float, dy: Float) {
        scroll = MathUtils.clamp(scroll + dy, 0f, maxScroll())
        velocity = dy * 60f
    }

    override fun onTap(x: Float, y: Float) {
        Achievements.all.forEachIndexed { i, a ->
            val ry = rowY(i)
            if (abs(y - ry) < rowH / 2 && x > W / 2 + 120f && a.done && !Save.achievementClaimed(a.id)) {
                Save.claimAchievement(a.id)
                Save.coins += a.reward
                Assets.sfx("reward", 0.9f)
                particles.coinsFly(W / 2 + 200f, ry, 8)
                particles.sparkle(W / 2 + 200f, ry, 10, 60f, Palette.GOLD)
                return
            }
        }
    }

    override fun tick(dt: Float) {
        scroll = MathUtils.clamp(scroll + velocity * dt, 0f, maxScroll())
        velocity *= 1f / (1f + dt * 5f)
    }

    override fun content() {
        val b = batch
        Achievements.all.forEachIndexed { i, a ->
            val y = rowY(i)
            if (y - rowH / 2 > listTop || y + rowH / 2 < 0f) return@forEachIndexed
            val pw = W - 50f
            Assets.framePanel.draw(b, W / 2 - pw / 2, y - rowH / 2, pw, rowH)
            val done = a.done
            val claimed = Save.achievementClaimed(a.id)
            b.setColor(1f, 1f, 1f, if (done) 1f else 0.45f)
            b.drawCentered(Assets.r(a.icon), 100f, y, 100f)
            b.color = Color.WHITE
            if (!done) b.drawCentered(Assets.r("ui_lock_icon"), 130f, y - 30f, 40f)
            b.text(Assets.fontBig, a.title, 170f, y + 32f, 32f, Palette.DARK_GREEN, alignX = 0f)
            b.text(Assets.fontPlain, a.desc, 170f, y - 4f, 24f, Palette.BROWN, alignX = 0f)
            // progress bar
            val p = min(a.progress(), a.goal)
            val bw = 260f
            Assets.pill.draw(b, 166f, y - 52f, bw, 26f)
            b.fillRect(172f, y - 46f, (bw - 12f) * p / a.goal, 14f, Palette.GREEN)
            val txt = progressText.getOrPut("${a.id}$p") { "$p / ${a.goal}" }
            b.text(Assets.fontSmall, txt, 166f + bw / 2, y - 38f, 18f)
            // reward / claim
            val rx = W - 120f
            when {
                claimed -> b.drawCentered(Assets.r("ui_check_mark_green"), rx, y, 70f)
                done -> {
                    val pulse = 1f + kotlin.math.sin(time * 6f) * 0.05f
                    b.drawCentered(Assets.r("btn_green"), rx, y, 170f * pulse)
                    b.text(Assets.fontBig, "CLAIM", rx, y + 6f, 28f)
                    b.drawCentered(Assets.r("coin"), rx - 36f, y - 38f, 28f)
                    b.text(Assets.fontBig, a.reward.toString(), rx + 10f, y - 36f, 24f, Palette.GOLD)
                }
                else -> {
                    b.drawCentered(Assets.r("coin"), rx - 30f, y, 44f)
                    b.text(Assets.fontBig, a.reward.toString(), rx + 20f, y + 2f, 30f, Palette.GOLD)
                }
            }
        }
    }
}
