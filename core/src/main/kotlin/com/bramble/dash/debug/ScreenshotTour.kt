package com.bramble.dash.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.ScreenUtils
import com.bramble.dash.BrambleDash
import com.bramble.dash.config.PowerUpType
import com.bramble.dash.game.BossState
import com.bramble.dash.game.RunState
import com.bramble.dash.screens.AchievementsScreen
import com.bramble.dash.screens.CharacterScreen
import com.bramble.dash.screens.DailyRewardsScreen
import com.bramble.dash.screens.GameScreen
import com.bramble.dash.screens.LeaderboardScreen
import com.bramble.dash.screens.LobbyScreen
import com.bramble.dash.screens.MissionsScreen
import com.bramble.dash.screens.ProfileScreen
import com.bramble.dash.screens.SettingsScreen
import com.bramble.dash.screens.ShopScreen
import com.bramble.dash.screens.UpgradesScreen
import com.bramble.dash.screens.WorldScreen
import java.io.File

/**
 * Desktop-only verification helper: visits every screen, plays a bot-driven run through a boss,
 * pause, victory and game over, and writes a PNG of each step. Started with --screenshots <dir>.
 */
class ScreenshotTour(private val game: BrambleDash, private val dir: File) {
    private val prefix = game.options.startWorld?.let { it + "_" } ?: ""
    private class Step(val name: String, val wait: Float, val action: () -> Unit, val until: () -> Boolean = { true })
    private val steps = ArrayList<Step>()
    private var index = -1
    private var timer = 0f
    private var started = false
    val log = StringBuilder()

    init {
        dir.mkdirs()
        fun s(name: String, wait: Float, until: () -> Boolean = { true }, action: () -> Unit) { steps += Step(name, wait, action, until) }
        val menus = !game.options.tourRunOnly
        if (menus) {
        s("01_lobby", 2.5f) { game.go(LobbyScreen(game)) }
        s("02_characters", 1.2f) { game.go(CharacterScreen(game)) }
        s("03_worlds", 1.2f) { game.go(WorldScreen(game)) }
        s("04_shop", 1.2f) { game.go(ShopScreen(game)) }
        s("05_upgrades", 1.2f) { game.go(UpgradesScreen(game)) }
        s("06_settings", 1.2f) { game.go(SettingsScreen(game)) }
        s("07_daily_rewards", 1.2f) { game.go(DailyRewardsScreen(game)) }
        s("08_missions", 1.2f) { game.go(MissionsScreen(game)) }
        s("09_achievements", 1.2f) { game.go(AchievementsScreen(game)) }
        s("10_leaderboard", 1.2f) { game.go(LeaderboardScreen(game)) }
        s("11_profile", 1.2f) { game.go(ProfileScreen(game)) }
        }
        s("12_run_start", 4f, { run()?.state == RunState.RUNNING }) { game.go(GameScreen(game)) }
        s("13_running", 7f) {}
        s("13b_running", 5f) {}
        s("13c_running", 5f) {}
        s("13d_running", 5f) {}
        s("14_powerups", 2.5f) { run()?.let { it.activatePowerUp(PowerUpType.COIN_MAGNET); it.activatePowerUp(PowerUpType.PROTECTIVE_SHIELD); it.activatePowerUp(PowerUpType.SCORE_MULTIPLIER) } }
        s("15_boss_warning", 1.2f, { run()?.state == RunState.BOSS_WARNING }) { run()?.debugBossSoon() }
        s("16_boss_fight", 2.5f, { run()?.boss?.let { it.state != BossState.ENTER } ?: false }) {}
        s("17_boss_phase2", 0.5f, { (run()?.boss?.phase ?: 1) == 2 || run()?.state == RunState.VICTORY }) {}
        s("18_boss_victory", 1.5f, { run()?.state == RunState.VICTORY }) {}
        s("19_pause", 1.5f) { (game.screen as? GameScreen)?.debugContinueAfterBoss() }
        s("19_pause", 1.0f) { (game.screen as? GameScreen)?.debugOpenPause() }
        s("20_game_over", 3.5f, { run()?.state == RunState.RESULTS }) { (game.screen as? GameScreen)?.let { it.debugClose(); it.currentWorld?.debugKill() } }
        s("21_lobby_after_run", 2f) { game.go(LobbyScreen(game)) }
    }

    private fun run() = (game.screen as? GameScreen)?.currentWorld

    /** Call after the frame is drawn. */
    fun update(delta: Float) {
        if (!game.ready) return
        if (!started) { started = true; next(); return }
        val step = steps[index]
        if (!step.until()) { timer = 0f; return }
        timer += delta
        if (timer >= step.wait) {
            if (index + 1 < steps.size && steps[index + 1].name == step.name) { next(); return }
            capture(step.name)
            next()
        }
    }

    private fun next() {
        index++
        timer = 0f
        if (index >= steps.size) {
            File(dir, "tour_log.txt").writeText(log.toString())
            Gdx.app.exit()
            return
        }
        log.append("step ${steps[index].name}\n")
        steps[index].action()
    }

    private fun capture(name: String) {
        val w = Gdx.graphics.backBufferWidth
        val h = Gdx.graphics.backBufferHeight
        val pm = Pixmap.createFromFrameBuffer(0, 0, w, h)
        // frame buffer rows are bottom-up
        val flipped = Pixmap(w, h, Pixmap.Format.RGBA8888)
        for (y in 0 until h) flipped.drawPixmap(pm, 0, y, w, 1, 0, h - 1 - y, w, 1)
        flipped.blending = Pixmap.Blending.None
        for (y in 0 until h) for (x in 0 until w) flipped.drawPixel(x, y, flipped.getPixel(x, y) or 0xff)
        PixmapIO.writePNG(Gdx.files.absolute(File(dir, "$prefix$name.png").absolutePath), flipped)
        pm.dispose(); flipped.dispose()
        log.append("captured $name fps=${Gdx.graphics.framesPerSecond}\n")
        ScreenUtils.clear(0f, 0f, 0f, 1f, false)
    }
}
