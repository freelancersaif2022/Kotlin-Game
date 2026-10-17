package com.bramble.dash.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.Scaling
import com.bramble.dash.BrambleDash
import com.bramble.dash.audio.MusicTrack
import com.bramble.dash.audio.Sfx
import com.bramble.dash.config.Ability
import com.bramble.dash.config.Characters
import com.bramble.dash.config.PowerUpType
import com.bramble.dash.config.Tuning
import com.bramble.dash.config.Worlds
import com.bramble.dash.game.AutoPilot
import com.bramble.dash.game.GameWorld
import com.bramble.dash.game.RunSetup
import com.bramble.dash.game.RunState
import com.bramble.dash.game.WorldListener
import com.bramble.dash.progression.Missions
import com.bramble.dash.progression.Progression
import com.bramble.dash.progression.Reward
import com.bramble.dash.progression.RunOutcome
import com.bramble.dash.progression.Upgrade
import com.bramble.dash.render.WorldRenderer
import com.bramble.dash.ui.AnimActor
import com.bramble.dash.ui.BarActor
import com.bramble.dash.ui.UiKit
import kotlin.random.Random

/** Loading → run (Running / BossEncounter / Paused / Dying) → Results or Victory overlays. */
class GameScreen(game: BrambleDash) : BaseScreen(game), WorldListener {
    private val save = game.save.data
    private val characterId = game.options.startCharacter?.takeIf { it in save.unlockedCharacters || game.options.screenshotDir != null } ?: save.selectedCharacter
    private val worldId = game.options.startWorld ?: save.selectedWorld
    private val groups = game.assets.runGroups(characterId, worldId)
    private var world: GameWorld? = null
    private var renderer: WorldRenderer? = null
    private var loaded = false
    private var resultsCommitted = false
    private var outcome: RunOutcome? = null
    private var overlay: Table? = null
    private var readyTimer = 1.1f
    private val emptyBar = Color(48 / 255f, 68 / 255f, 95 / 255f, 1f)

    // HUD widgets
    private lateinit var scoreLabel: Label
    private lateinit var distLabel: Label
    private lateinit var coinLabel: Label
    private lateinit var gemLabel: Label
    private lateinit var healthBar: BarActor
    private lateinit var hearts: Table
    private lateinit var progressBar: BarActor
    private lateinit var bossBar: BarActor
    private lateinit var bossTable: Table
    private lateinit var bossName: Label
    private lateinit var progressTable: Table
    private lateinit var powerRow: Table
    private lateinit var attackButton: Button
    private lateinit var hintLabel: Label
    private lateinit var centerLabel: Label
    private var lastHealth = -1
    private var lastMax = -1
    private val powerSlots = HashMap<PowerUpType, PowerSlot>()

    override val backdropDim = 0f
    override fun backdropWorld() = worldId

    override fun build() {
        game.assets.prepareGroups(groups)
        val t = Table()
        t.background = ui.drawable("ui/screens/loading_panel")
        t.add(ui.label(Worlds.byId(worldId).name, ui.bodyFont, UiKit.CREAM)).padTop(120f).row()
        t.add(ui.label("Runner: " + Characters.byId(characterId).name, ui.smallFont, UiKit.TEAL)).row()
        val bar = BarActor(game.assets.region("ui/gameplay/progress"), game.assets.white, 14f, 280f, 22f, 45f, emptyBar)
        bar.addAction(Actions.forever(Actions.run { bar.fraction = game.assets.progress }))
        t.add(bar).size(420f, 89f).padTop(20f)
        root.add(t).size(800f, 450f)
        game.audio.playMusic(null)
    }

    private fun startRun() {
        loaded = true
        root.clearChildren()
        val d = save
        val c = Characters.byId(characterId)
        val hp = (Tuning.BASE_HEALTH + d.upgrade(Upgrade.HEALTH.id) + if (c.ability == Ability.TOUGH) 1 else 0).coerceAtMost(Tuning.MAX_HEALTH_CAP)
        val shieldChance = Upgrade.shieldChance(d.upgrade(Upgrade.SHIELD_START.id))
        val rng = Random(System.nanoTime())
        val setup = RunSetup(
            character = c, world = Worlds.byId(worldId), seed = rng.nextLong(), maxHealth = hp,
            durationBonus = d.upgrade(Upgrade.POWER_DURATION.id) * Tuning.PU_DURATION_UPGRADE_STEP,
            magnetRange = Tuning.MAGNET_BASE_RANGE + d.upgrade(Upgrade.MAGNET_RANGE.id) * Tuning.MAGNET_RANGE_STEP,
            scoreBonus = d.upgrade(Upgrade.SCORE_BONUS.id) * 0.05f,
            startShield = rng.nextInt(100) < shieldChance,
            tutorial = !d.tutorialDone && game.options.screenshotDir == null,
            quality = d.settings.quality,
        )
        Gdx.app.log("BrambleDash", "run seed=${setup.seed} world=${setup.world.id} character=${c.id}")
        val w = GameWorld(game.assets.sprites, setup, this)
        if (game.options.autoPlay || game.options.screenshotDir != null) w.autoPilot = AutoPilot()
        world = w
        val r = WorldRenderer(game.assets, w) { save.settings.quality }
        r.resize(Gdx.graphics.width, Gdx.graphics.height)
        renderer = r
        buildHud(w)
        game.audio.playMusic(MusicTrack.RUN)
        game.audio.playAmbience(MusicTrack.ambience(setup.world.ambience))
        installInput(w)
    }

    // ------------------------------------------------------------------ HUD
    private fun valuePanel(region: String, label: Label): Stack {
        val s = Stack()
        s.add(Image(ui.drawable(region), Scaling.stretch))
        val t = Table()
        t.add(label).expand().right().padRight(16f).padTop(14f)
        s.add(t)
        return s
    }

    private fun buildHud(w: GameWorld) {
        scoreLabel = ui.label("0", ui.smallFont, UiKit.CREAM)
        distLabel = ui.label("0 m", ui.smallFont, UiKit.CREAM)
        coinLabel = ui.label("0", ui.smallFont, UiKit.GOLD)
        gemLabel = ui.label("0", ui.smallFont, UiKit.TEAL)
        val top = Table()
        val leftCol = Table()
        leftCol.add(valuePanel("ui/gameplay/score", scoreLabel)).size(250f, 53f).left().row()
        leftCol.add(valuePanel("ui/gameplay/distance", distLabel)).size(250f, 53f).left().padTop(4f).row()
        val hp = Stack()
        healthBar = BarActor(game.assets.region("ui/gameplay/health_bar"), game.assets.white, 14f, 280f, 22f, 45f, emptyBar)
        hearts = Table()
        hp.add(healthBar); hp.add(hearts)
        leftCol.add(hp).size(250f, 53f).left().padTop(4f).row()
        powerRow = Table()
        leftCol.add(powerRow).left().padTop(4f)
        top.add(leftCol).top().left()

        val mid = Table()
        val pause = ui.stateButton("ui/gameplay/pause") { pauseGame() }
        mid.add(pause).size(170f, 52f).row()
        progressTable = Table()
        progressBar = BarActor(game.assets.region("ui/gameplay/progress"), game.assets.white, 14f, 280f, 22f, 45f, emptyBar)
        progressTable.add(ui.label("BOSS", ui.smallFont, UiKit.RED, scale = 0.7f)).padRight(6f)
        progressTable.add(progressBar).size(260f, 55f)
        mid.add(progressTable).padTop(4f).row()
        bossTable = Table()
        bossName = ui.label("", ui.smallFont, UiKit.RED, scale = 0.8f)
        bossBar = BarActor(game.assets.region("ui/gameplay/boss_health"), game.assets.white, 14f, 280f, 22f, 45f, emptyBar)
        bossTable.add(bossName).row()
        bossTable.add(bossBar).size(420f, 89f)
        bossTable.isVisible = false
        mid.add(bossTable)
        top.add(mid).expandX().top()

        val rightCol = Table()
        rightCol.add(valuePanel("ui/gameplay/coins", coinLabel)).size(250f, 53f).right().row()
        rightCol.add(valuePanel("ui/gameplay/gems", gemLabel)).size(250f, 53f).right().padTop(4f).row()
        top.add(rightCol).top().right()
        root.add(top).growX().top().row()

        val centre = Table()
        centerLabel = ui.label("GET READY", ui.titleFont, UiKit.CREAM, scale = 0.9f)
        hintLabel = ui.label("", ui.bodyFont, UiKit.CREAM, scale = 0.8f, wrap = true, align = Align.center)
        centre.add(centerLabel).row()
        centre.add(hintLabel).width(800f)
        root.add(centre).expand().row()

        // touch controls (bottom corners, inside the safe area)
        val controls = Table()
        val slide = controlButton("ui/gameplay/slide", { world?.pressSlide() }, {})
        val jump = controlButton("ui/gameplay/jump", { world?.pressJump() }, { world?.releaseJump() })
        attackButton = ui.textButton("ENERGY BLAST", UiKit.Tone.GOLD, ui.smallFont).also { b ->
            b.clearListeners()
            b.addListener(object : InputListener() {
                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean { world?.pressAttack(); return true }
            })
        }
        attackButton.isVisible = false
        val left = save.settings.leftHanded
        val jumpCol = Table()
        jumpCol.add(attackButton).height(64f).padBottom(10f).row()
        jumpCol.add(jump).size(260f, 100f)
        if (!left) {
            controls.add(slide).size(260f, 100f).bottom().left()
            controls.add().expandX()
            controls.add(jumpCol).bottom().right()
        } else {
            controls.add(jumpCol).bottom().left()
            controls.add().expandX()
            controls.add(slide).size(260f, 100f).bottom().right()
        }
        root.add(controls).growX().bottom()
    }

    /** Fires on touch-down (not release) so jumps feel immediate; supports multi-touch. */
    private fun controlButton(base: String, down: () -> Unit, up: () -> Unit): Button {
        val style = Button.ButtonStyle().apply {
            this.up = ui.drawable("$base/normal"); this.down = ui.drawable("$base/pressed"); disabled = ui.drawable("$base/disabled")
        }
        val b = Button(style)
        b.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean { down(); return true }
            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) { up() }
        })
        return b
    }

    private fun installInput(w: GameWorld) {
        val keys = object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                when (keycode) {
                    Input.Keys.SPACE, Input.Keys.UP, Input.Keys.W -> w.pressJump()
                    Input.Keys.DOWN, Input.Keys.S -> w.pressSlide()
                    Input.Keys.F, Input.Keys.X, Input.Keys.ENTER -> w.pressAttack()
                    Input.Keys.P, Input.Keys.ESCAPE, Input.Keys.BACK -> onBack()
                    else -> return false
                }
                return true
            }
            override fun keyUp(keycode: Int): Boolean {
                if (keycode == Input.Keys.SPACE || keycode == Input.Keys.UP || keycode == Input.Keys.W) { w.releaseJump(); return true }
                return false
            }
        }
        // swipes on empty screen areas (the stage consumes touches on buttons first)
        val swipes = object : InputAdapter() {
            private val startY = FloatArray(10)
            private val active = BooleanArray(10)
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (pointer >= 10 || !save.settings.swipeControls) return false
                startY[pointer] = screenY.toFloat(); active[pointer] = true
                return true
            }
            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if (pointer >= 10 || !active[pointer]) return false
                val dy = (screenY - startY[pointer]) / Gdx.graphics.height * 720f
                if (dy < -55f) { active[pointer] = false; w.pressJump(); w.releaseJump() }
                else if (dy > 55f) { active[pointer] = false; w.pressSlide() }
                return true
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (pointer < 10) active[pointer] = false
                return false
            }
        }
        Gdx.input.inputProcessor = InputMultiplexer(stage, keys, swipes)
    }

    private var shownScore = -1
    private var shownMetres = -1
    private var shownCoins = -1
    private var shownGems = -1
    private var shownBossKey = -1
    private var shownCentre: String? = null
    private var shownHint: String? = null

    private fun updateHud(w: GameWorld) {
        val sc = w.score.toInt(); val m = w.metres.toInt()
        if (sc != shownScore) { shownScore = sc; scoreLabel.setText(ui.formatNumber(sc)) }
        if (m != shownMetres) { shownMetres = m; distLabel.setText("$m m") }
        if (w.coins != shownCoins) { shownCoins = w.coins; coinLabel.setText(ui.formatNumber(w.coins)) }
        if (w.gems != shownGems) { shownGems = w.gems; gemLabel.setText(w.gems.toString()) }
        val p = w.player
        if (p.health != lastHealth || p.maxHealth != lastMax) {
            lastHealth = p.health; lastMax = p.maxHealth
            healthBar.fraction = p.health / p.maxHealth.toFloat()
            hearts.clearChildren()
            for (i in 0 until p.maxHealth) {
                val img = ui.image("ui/icons/heart")
                if (i >= p.health) img.color = Color(0.25f, 0.25f, 0.3f, 0.8f)
                hearts.add(img).size(30f).pad(0f, 2f, 4f, 2f)
            }
        }
        val b = w.boss
        val bossOn = b != null && !b.defeated
        bossTable.isVisible = bossOn
        progressTable.isVisible = !bossOn
        if (b != null) {
            bossBar.fraction = b.hp / b.maxHp.toFloat()
            val key = b.phase * 100 + b.encounter
            if (key != shownBossKey) { shownBossKey = key; bossName.setText(b.def.name + if (b.phase == 2) "  •  PHASE 2" else "") }
        }
        progressBar.fraction = w.bossProgress
        attackButton.isVisible = w.canAttack
        attackButton.isDisabled = w.energyCooldown > 0f
        // power-up slots
        for (t in PowerUpType.values()) {
            val on = when (t) {
                PowerUpType.PROTECTIVE_SHIELD -> w.powerUps.shield
                PowerUpType.EXTRA_LIFE -> w.powerUps.extraLife
                PowerUpType.HEALTH_RECOVERY -> false
                else -> w.powerUps.active(t)
            }
            val slot = powerSlots[t]
            if (on && slot == null) {
                val s = PowerSlot(t)
                powerSlots[t] = s
                powerRow.add(s).size(52f, 62f).padRight(4f)
            } else if (!on && slot != null) {
                powerSlots.remove(t)
                powerRow.removeActor(slot)
            }
            powerSlots[t]?.fraction = if (t.timed) w.powerUps.remaining[t.ordinal] / w.powerUps.total[t.ordinal].coerceAtLeast(0.01f) else 1f
        }
        // centre messages
        val centre = when {
            w.state == RunState.READY -> "GET READY"
            w.state == RunState.BOSS && w.boss?.state == com.bramble.dash.game.BossState.ENTER -> w.boss!!.def.name
            w.state == RunState.BOSS_DEFEATED -> "BOSS DEFEATED!"
            w.state == RunState.RUNNING && w.stateTime < 0.8f && w.runTime < 2f -> "GO!"
            else -> ""
        }
        if (centre != shownCentre) { shownCentre = centre; centerLabel.setText(centre.uppercase()) }
        val hint = w.activeHint ?: ""
        if (hint != shownHint) { shownHint = hint; hintLabel.setText(hint) }
    }

    private inner class PowerSlot(val type: PowerUpType) : Actor() {
        var fraction = 1f
        private val icon = game.assets.region("powerups/${type.id}/hud")
        override fun draw(batch: Batch, parentAlpha: Float) {
            batch.setColor(1f, 1f, 1f, parentAlpha)
            batch.draw(icon, x, y + 10f, width, width)
            if (type.timed) {
                batch.setColor(0.1f, 0.14f, 0.22f, parentAlpha)
                batch.draw(game.assets.white, x, y, width, 7f)
                val c = if (fraction < 0.25f) UiKit.RED else UiKit.TEAL
                batch.setColor(c.r, c.g, c.b, parentAlpha)
                batch.draw(game.assets.white, x, y, width * fraction.coerceIn(0f, 1f), 7f)
            }
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    // ------------------------------------------------------------------ frame
    override fun render(delta: Float) {
        val w = world
        if (!loaded) {
            ScreenUtils.clear(0.07f, 0.1f, 0.16f, 1f)
            if (game.assets.update(16) && groups.all { game.assets.isGroupLoaded(it) }) startRun()
            stage.act(delta); stage.draw()
            return
        }
        w!!
        if (w.state == RunState.READY) {
            readyTimer -= delta
            if (readyTimer <= 0f) w.start()
        }
        if (overlay == null || w.state == RunState.BOSS_DEFEATED) w.update(delta)
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        renderer!!.render(game.batch, delta)
        updateHud(w)
        stage.viewport.apply()
        stage.act(delta.coerceAtMost(0.1f))
        stage.draw()
        afterDraw(delta)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        renderer?.resize(width, height)
    }

    // ------------------------------------------------------------------ world events
    override fun sfx(s: Sfx, volume: Float, pitch: Float) = game.audio.play(s, volume, pitch)
    override fun shake(amount: Float) { renderer?.shake(amount) }
    override fun vibrate(ms: Int) = game.vibrate(ms)
    override fun reward(text: String, r: Reward) {}
    override fun tutorialFinished() { game.save.transact { it.tutorialDone = true } }
    override fun powerUp(t: PowerUpType, activated: Boolean) {
        if (!activated) game.audio.play(Sfx.UI_BACK, 0.5f, 0.8f)
    }

    override fun stateChanged(from: RunState, to: RunState) {
        when (to) {
            RunState.BOSS_WARNING -> game.audio.playMusic(MusicTrack.BOSS)
            RunState.VICTORY -> showVictory()
            RunState.RUNNING -> if (from == RunState.VICTORY || from == RunState.REVIVE_OFFER) { game.audio.playMusic(MusicTrack.RUN) }
            RunState.REVIVE_OFFER -> showRevive()
            RunState.RESULTS -> showResults()
            RunState.DYING -> game.audio.playMusic(null)
            else -> {}
        }
    }

    // ------------------------------------------------------------------ overlays
    private fun modal(panelRegion: String?, w: Float = 900f, h: Float = 506f): Table {
        closeOverlay()
        val shade = Table()
        shade.setFillParent(true)
        shade.background = ui.whiteDrawable(Color(0f, 0f, 0f, 0.55f))
        shade.touchable = Touchable.enabled
        val box = Table()
        box.background = if (panelRegion != null) ui.drawable(panelRegion) else ui.panel()
        shade.add(box).size(w, h)
        shade.color.a = 0f
        shade.addAction(Actions.fadeIn(0.18f))
        stage.addActor(shade)
        overlay = shade
        return box
    }

    private fun closeOverlay() { overlay?.remove(); overlay = null }

    fun pauseGame() {
        val w = world ?: return
        if (overlay != null || w.state == RunState.READY && !loaded) return
        if (w.state == RunState.RESULTS || w.state == RunState.REVIVE_OFFER || w.state == RunState.VICTORY || w.state == RunState.DYING) return
        w.pause()
        game.audio.stopEffects()
        val box = modal("ui/screens/pause_panel")
        box.add().height(120f).row()
        box.add(ui.label("${w.metres.toInt()} m   •   ${ui.formatNumber(w.score.toInt())} pts", ui.smallFont, UiKit.CREAM)).colspan(3).row()
        val buttons = Table()
        buttons.add(ui.stateButton("ui/gameover/continue") { resumeGame() }).size(250f, 76f).pad(8f)
        buttons.add(ui.stateButton("ui/gameover/restart") { endRun { game.go(GameScreen(game)) } }).size(250f, 76f).pad(8f)
        buttons.add(ui.stateButton("ui/gameover/home", Sfx.UI_BACK) { endRun { game.go(LobbyScreen(game)) } }).size(250f, 76f).pad(8f)
        box.add(buttons).padTop(14f).row()
        val toggles = Table()
        val s = save.settings
        val sound = ui.stateButton("ui/settings/sound", toggle = true) { b -> s.muted = !b.isChecked; game.applyAudioSettings(); game.save.commit() }
        sound.isChecked = !s.muted
        val vib = ui.stateButton("ui/settings/vibration", toggle = true) { b -> s.vibration = b.isChecked; game.save.commit() }
        vib.isChecked = s.vibration
        toggles.add(sound).size(200f, 61f).pad(6f)
        toggles.add(vib).size(200f, 61f).pad(6f)
        box.add(toggles).padTop(8f)
    }

    private fun resumeGame() {
        closeOverlay()
        world?.resume()
    }

    /** Ends the run early (pause menu / victory home) after recording its results once. */
    private fun endRun(then: () -> Unit) {
        val w = world
        if (w != null) {
            if (w.state != RunState.RESULTS) w.quit()
            commitResults(w)
        }
        then()
    }

    private fun showRevive() {
        val box = modal(null, 760f, 420f)
        box.add(ui.label("OUT OF HEALTH!", ui.titleFont, UiKit.RED, scale = 0.7f)).padTop(16f).row()
        box.add(ui.image("powerups/extra_life/icon")).size(110f).pad(8f).row()
        box.add(ui.label("Use your Extra Life to keep running?", ui.smallFont, UiKit.CREAM)).row()
        val b = Table()
        b.add(ui.stateButton("ui/gameover/continue") { closeOverlay(); world?.revive() }).size(260f, 80f).pad(10f)
        b.add(ui.textButton("NO THANKS", UiKit.Tone.PLAIN) { closeOverlay(); world?.finish() }).pad(10f)
        box.add(b).padTop(10f)
    }

    private fun commitResults(w: GameWorld) {
        if (resultsCommitted) return
        val report = w.report ?: return
        resultsCommitted = true
        game.save.transact { d ->
            outcome = Progression.applyRun(d, report, game.today())
            if (!w.setup.tutorial || d.tutorialDone) d.tutorialDone = true
        }
    }

    private fun showResults() {
        val w = world ?: return
        commitResults(w)
        val o = outcome
        val r = w.report ?: return
        if (o != null && o.missionsCompleted.isNotEmpty()) game.audio.play(Sfx.MISSION_COMPLETE)
        game.audio.playMusic(MusicTrack.MENU)
        game.audio.playAmbience(null)
        val box = modal("ui/screens/gameover_panel", 1040f, 585f)
        box.add().height(150f).colspan(3).row()
        fun stat(region: String, value: String, color: Color): Stack {
            val s = Stack()
            s.add(Image(ui.drawable(region), Scaling.fit))
            val t = Table()
            t.add(ui.label(value, ui.bodyFont, color)).expand().bottom().padBottom(26f)
            s.add(t)
            return s
        }
        box.add(stat("ui/components/final_score", ui.formatNumber(r.score), UiKit.CREAM)).size(272f, 163f).pad(4f)
        box.add(stat("ui/components/best_score", ui.formatNumber(save.bestScore), if (o?.newBestScore == true) UiKit.GOLD else UiKit.CREAM)).size(272f, 163f).pad(4f)
        box.add(stat("ui/components/coins_collected", "+" + ui.formatNumber(r.coins), UiKit.GOLD)).size(272f, 163f).pad(4f).row()
        val lines = ArrayList<String>()
        lines += "${r.metres} m in ${Worlds.byId(r.worldId).name}" + if (o?.newBestMetres == true) "  •  NEW BEST DISTANCE!" else ""
        if (o?.newBestScore == true) lines += "NEW BEST SCORE!"
        if (r.gems + r.keys + r.chests + r.tokens > 0) lines += listOfNotNull(
            if (r.gems > 0) "+${r.gems} gems" else null, if (r.keys > 0) "+${r.keys} key" + (if (r.keys > 1) "s" else "") else null,
            if (r.chests > 0) "+${r.chests} chest" + (if (r.chests > 1) "s" else "") else null, if (r.tokens > 0) "+${r.tokens} token" + (if (r.tokens > 1) "s" else "") else null).joinToString("  ")
        o?.missionsCompleted?.forEach { lines += "Mission complete: " + Missions.def(it).text }
        o?.worldUnlocked?.let { lines += "World unlocked: " + Worlds.byId(it).name + "!" }
        if (o?.achievementsUnlocked?.isNotEmpty() == true) lines += "Achievement unlocked! Claim it in Achievements."
        box.add(ui.label(lines.joinToString("\n"), ui.smallFont, UiKit.CREAM, scale = 0.8f, align = Align.center)).colspan(3).padTop(6f).row()
        val b = Table()
        b.add(ui.stateButton("ui/gameover/restart") { game.go(GameScreen(game)) }).size(250f, 76f).pad(8f)
        b.add(ui.stateButton("ui/gameover/home", Sfx.UI_BACK) { game.go(LobbyScreen(game)) }).size(250f, 76f).pad(8f)
        box.add(b).colspan(3).padTop(8f)
        if (o?.worldUnlocked != null) game.audio.play(Sfx.UNLOCK)
    }

    private fun showVictory() {
        val w = world ?: return
        val b = w.boss ?: return
        game.audio.play(Sfx.VICTORY, 0.6f)
        val box = modal("ui/screens/victory_panel")
        box.add().height(120f).row()
        val fx = AnimActor(game.assets.anim("effects/victory_confetti"), 1.2f, loop = true)
        val pose = AnimActor(game.assets.anim("characters/${w.setup.character.id}/victory"), 0.7f, loop = true)
        pose.pivotMode = true
        val top = Table()
        top.add(pose).size(150f, 190f)
        val info = Table()
        info.add(ui.label("${b.def.name} defeated!", ui.bodyFont, UiKit.GOLD)).row()
        val rw = w.lastBossReward
        if (rw != null) {
            val rr = Table()
            rr.add(ui.image("ui/icons/gold")).size(40f); rr.add(ui.label("+${rw.coins}", ui.bodyFont, UiKit.CREAM)).padRight(20f)
            rr.add(ui.image("ui/icons/diamond")).size(40f); rr.add(ui.label("+${rw.gems}", ui.bodyFont, UiKit.CREAM))
            info.add(rr).padTop(8f).row()
        }
        if (b.encounter == 0 && Worlds.indexOf(w.def.id) + 1 < Worlds.all.size) {
            info.add(ui.label("Next world unlocks when this run ends: " + Worlds.all[Worlds.indexOf(w.def.id) + 1].name, ui.smallFont, UiKit.TEAL, scale = 0.7f)).padTop(6f)
        }
        top.add(info).padLeft(20f)
        val st = Stack(); st.add(fx); st.add(top)
        box.add(st).row()
        val buttons = Table()
        buttons.add(ui.stateButton("ui/gameover/continue") { closeOverlay(); w.continueAfterBoss() }).size(250f, 76f).pad(8f)
        buttons.add(ui.stateButton("ui/gameover/home", Sfx.UI_BACK) { endRun { game.go(LobbyScreen(game)) } }).size(250f, 76f).pad(8f)
        box.add(buttons).padTop(10f)
    }

    override fun onBack() {
        val w = world ?: return
        if (overlay != null) {
            if (w.state == RunState.PAUSED) resumeGame()
            return
        }
        pauseGame()
    }

    /** App backgrounded or interrupted: pause the run so it never advances unseen. */
    override fun pause() {
        if (loaded) pauseGame()
    }

    // ------------------------------------------------------------------ test hooks (desktop screenshot tour)
    val currentWorld: GameWorld? get() = world
    fun debugOpenPause() = pauseGame()
    fun debugClose() = resumeGame()
    fun debugContinueAfterBoss() { closeOverlay(); world?.continueAfterBoss() }
}
