package com.bramble.dash.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.bramble.dash.BrambleDash
import com.bramble.dash.audio.MusicTrack
import com.bramble.dash.config.Characters
import com.bramble.dash.config.Worlds
import com.bramble.dash.progression.DailyRewards
import com.bramble.dash.progression.Missions
import com.bramble.dash.ui.AnimActor
import com.bramble.dash.ui.UiKit

/** Hub: logo, play, selected runner and world, currencies and every menu. */
class LobbyScreen(game: BrambleDash) : BaseScreen(game) {
    private var preview: Stack? = null
    private var previewShowsAnim = false

    override fun build() {
        val d = game.save.data
        game.audio.playMusic(MusicTrack.MENU)
        game.audio.playAmbience(null)
        // keep only the selected runner/world resident for the lobby
        game.assets.prepareGroups(setOf("world_" + d.selectedWorld, "char_" + d.selectedCharacter))
        Missions.refill(d)

        val top = Table()
        top.add(ui.image("ui/lobby/logo")).size(504f, 154f).left()
        top.add().expandX()
        top.add(ui.currencyBar { intArrayOf(d.coins, d.gems, d.keys, d.tokens) }).top().right()
        root.add(top).growX().colspan(3).row()

        // left: selected runner
        val c = Characters.byId(d.selectedCharacter)
        val left = Table()
        val stack = Stack()
        preview = stack
        stack.add(Image(ui.drawable("ui/shop/card_${c.id}"), Scaling.fit))
        left.add(stack).size(210f, 280f).row()
        left.add(ui.label(c.name, ui.bodyFont, UiKit.CREAM)).padTop(4f).row()
        left.add(ui.label(c.ability.label, ui.smallFont, UiKit.TEAL, wrap = true, align = Align.center)).width(300f).row()
        left.add(ui.stateButton("ui/lobby/characters") { game.go(CharacterScreen(game)) }).size(230f, 70f).padTop(6f)
        root.add(left).width(290f).expandY().center()

        // centre: world + play
        val w = Worlds.byId(d.selectedWorld)
        val mid = Table()
        val worldCard = Table()
        worldCard.background = ui.panel()
        worldCard.add(Image(ui.drawable("previews/${w.id}"), Scaling.fit)).size(320f, 180f).pad(10f).row()
        worldCard.add(ui.label(w.name, ui.smallFont, UiKit.CREAM)).row()
        worldCard.add(ui.label("Best: ${d.bestMetresByWorld[w.id] ?: 0} m", ui.smallFont, UiKit.TEAL, scale = 0.85f)).padBottom(8f).row()
        mid.add(worldCard).width(340f).row()
        mid.add(ui.textButton("CHANGE WORLD", UiKit.Tone.PLAIN) { game.go(WorldScreen(game)) }).padTop(8f).row()
        val play = ui.stateButton("ui/lobby/play") { game.go(GameScreen(game)) }
        play.isTransform = true
        play.setOrigin(173f, 53f)
        play.addAction(Actions.forever(Actions.sequence(Actions.scaleTo(1.04f, 1.04f, 0.6f), Actions.scaleTo(1f, 1f, 0.6f))))
        mid.add(play).size(346f, 106f).padTop(14f).row()
        mid.add(ui.label("Best score ${ui.formatNumber(d.bestScore)}   •   Best ${d.bestMetres} m", ui.smallFont, UiKit.CREAM, scale = 0.85f)).padTop(6f)
        root.add(mid).expand().center()

        // right: menu grid
        // menu grid on the pack's lobby panel (its "LOBBY" heading is part of the art)
        val grid = Table()
        grid.background = ui.drawable("ui/screens/lobby_panel")
        grid.padTop(78f).padBottom(16f)
        val today = game.today()
        val dailyReady = DailyRewards.status(d, today) == DailyRewards.Status.AVAILABLE
        val missionReady = d.missions.any { Missions.isComplete(it) && !it.claimed }
        val achReady = d.achievements.values.any { it == 1 }
        fun entry(base: String, badge: Boolean = false, go: () -> Unit) {
            val b = ui.stateButton(base) { go() }
            b.isChecked = false
            val cell = Stack()
            cell.add(b)
            if (badge) {
                val dot = Table()
                dot.add(ui.image("ui/icons/star")).size(30f).expand().top().right().pad(-6f)
                dot.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.disabled
                cell.add(dot)
            }
            grid.add(cell).size(232f, 54f).pad(3f, 8f, 3f, 8f)
        }
        entry("ui/lobby/shop") { game.go(ShopScreen(game)) }
        entry("ui/lobby/upgrade") { game.go(UpgradesScreen(game)) }
        grid.row()
        entry("ui/lobby/daily_rewards", dailyReady || (d.chests > 0 && d.keys > 0)) { game.go(DailyRewardsScreen(game)) }
        entry("ui/lobby/missions", missionReady) { game.go(MissionsScreen(game)) }
        grid.row()
        entry("ui/lobby/achievements", achReady) { game.go(AchievementsScreen(game)) }
        entry("ui/lobby/leaderboard") { game.go(LeaderboardScreen(game)) }
        grid.row()
        entry("ui/lobby/profile") { game.go(ProfileScreen(game)) }
        entry("ui/lobby/settings") { game.go(SettingsScreen(game)) }
        root.add(grid).size(560f, 330f).expandY().center()

        if (game.save.recoveredFromBackup) ui.toast(stage, "Save data was damaged; restored the last good copy.")
        else if (game.save.resetToDefaults) ui.toast(stage, "Save data could not be read; started fresh.")
    }

    override fun render(delta: Float) {
        super.render(delta)
        // swap the static card for the animated idle once the runner atlas is resident
        val d = game.save.data
        if (!previewShowsAnim && game.assets.isGroupLoaded("char_" + d.selectedCharacter)) {
            previewShowsAnim = true
            val actor = AnimActor(game.assets.anim("characters/${d.selectedCharacter}/idle"), 1.0f)
            actor.pivotMode = true
            preview?.let { s ->
                s.clearChildren()
                val bg = Table()
                bg.background = ui.panel(Color(1f, 1f, 1f, 0.85f))
                s.add(bg)
                s.add(actor)
            }
        }
    }

    override fun onBack() {
        com.badlogic.gdx.Gdx.app.exit()
    }
}
