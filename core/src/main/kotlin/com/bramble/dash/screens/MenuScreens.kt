package com.bramble.dash.screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.bramble.dash.BrambleDash
import com.bramble.dash.audio.Sfx
import com.bramble.dash.config.Bosses
import com.bramble.dash.config.Characters
import com.bramble.dash.config.Tuning
import com.bramble.dash.config.Worlds
import com.bramble.dash.progression.Achievements
import com.bramble.dash.progression.DailyRewards
import com.bramble.dash.progression.Economy
import com.bramble.dash.progression.MysteryBox
import com.bramble.dash.progression.Missions
import com.bramble.dash.progression.PurchaseResult
import com.bramble.dash.progression.Reward
import com.bramble.dash.progression.Upgrade
import com.bramble.dash.ui.BarActor
import com.bramble.dash.ui.UiKit
import kotlin.random.Random

/**
 * Menu screen inside one of the pack's screen panels (whose heading is baked into the art) or,
 * for screens without panel art, the 9-patch panel with a runtime title.
 */
abstract class PanelScreen(game: BrambleDash, private val panel: String?, private val title: String? = null) : BaseScreen(game) {
    protected val content = Table()

    override fun build() {
        val frame = Table()
        frame.background = if (panel != null) ui.drawable("ui/screens/${panel}_panel") else ui.panel()
        val head = Table()
        if (panel == null && title != null) head.add(ui.label(title, ui.titleFont, UiKit.CREAM, scale = 0.8f))
        frame.add(head).height(if (panel != null) 158f else 110f).growX().row()
        content.clearChildren()
        frame.add(content).grow().pad(0f, 48f, 0f, 48f).row()
        val bottom = Table()
        bottom.add(ui.stateButton("ui/settings/back", Sfx.UI_BACK) { onBack() }).size(210f, 64f).left()
        bottom.add().expandX()
        bottom.add(ui.currencyBar { val d = game.save.data; intArrayOf(d.coins, d.gems, d.keys, d.tokens) }).right()
        frame.add(bottom).growX().pad(4f, 40f, 34f, 40f)
        root.add(frame).size(1180f, 664f)
        buildContent()
    }

    abstract fun buildContent()

    override fun onBack() = game.go(LobbyScreen(game))

    protected fun failFeedback(r: PurchaseResult) {
        game.audio.play(Sfx.PURCHASE_FAIL)
        ui.toast(stage, when (r) {
            PurchaseResult.INSUFFICIENT_FUNDS -> "Not enough currency yet. Keep running!"
            PurchaseResult.MAXED -> "Already at the maximum level"
            PurchaseResult.ALREADY_OWNED -> "Already unlocked"
            PurchaseResult.LOCKED -> "Nothing to open"
            else -> "Not possible"
        }, UiKit.RED)
    }

    protected fun priceLabel(icon: String, amount: Int, affordable: Boolean): Table {
        val t = Table()
        t.add(ui.image(icon)).size(30f).padRight(4f)
        t.add(ui.label(ui.formatNumber(amount), ui.smallFont, if (affordable) UiKit.GOLD else UiKit.RED))
        return t
    }
}

class CharacterScreen(game: BrambleDash) : PanelScreen(game, "character_selection") {
    override fun buildContent() {
        val d = game.save.data
        for (c in Characters.all) {
            val col = Table()
            val owned = c.id in d.unlockedCharacters
            val stack = Stack()
            val card = Image(ui.drawable("ui/shop/card_${c.id}"), Scaling.fit)
            if (!owned) card.color = Color(0.45f, 0.45f, 0.5f, 1f)
            stack.add(card)
            val badge = Table()
            badge.add(ui.image(if (owned) "ui/components/unlocked" else "ui/components/locked")).size(120f, 72f).expand().bottom().padBottom(6f)
            if (!owned || d.selectedCharacter != c.id) stack.add(badge)
            col.add(stack).size(150f, 200f).row()
            col.add(ui.label(c.name, ui.smallFont, UiKit.CREAM)).padTop(2f).row()
            col.add(ui.label(c.ability.label, ui.smallFont, UiKit.TEAL, scale = 0.62f, wrap = true, align = Align.center)).width(200f).height(58f).row()
            when {
                d.selectedCharacter == c.id -> col.add(ui.label("SELECTED", ui.smallFont, UiKit.GOLD)).height(58f)
                owned -> col.add(ui.textButton("SELECT") {
                    game.save.transact { it.selectedCharacter = c.id }
                    rebuild()
                }).height(58f)
                else -> {
                    val buy = ui.stateButton("ui/shop/purchase") {
                        ui.confirm(stage, "Unlock ${c.name} for ${ui.formatNumber(c.price)} coins?") {
                            var r = PurchaseResult.LOCKED
                            game.save.transact { r = Economy.buyCharacter(it, c.id); Achievements.evaluate(it) }
                            if (r == PurchaseResult.OK) {
                                game.audio.play(Sfx.UNLOCK)
                                ui.rewardReveal(stage, "${c.name} unlocked!", c.ability.label) { rebuild() }
                            } else failFeedback(r)
                        }
                    }
                    col.add(buy).size(180f, 55f).row()
                    col.add(priceLabel("ui/icons/gold", c.price, d.coins >= c.price))
                }
            }
            content.add(col).expand().top().padTop(4f)
        }
    }
}

class WorldScreen(game: BrambleDash) : PanelScreen(game, "world_selection") {
    override fun buildContent() {
        val d = game.save.data
        Worlds.all.forEachIndexed { i, w ->
            val cell = Table()
            cell.background = ui.panel(if (d.selectedWorld == w.id) Color(1f, 0.85f, 0.5f, 1f) else Color.WHITE)
            val unlocked = w.id in d.unlockedWorlds
            val img = Image(ui.drawable("previews/${w.id}"), Scaling.fit)
            if (!unlocked) img.color = Color(0.35f, 0.35f, 0.4f, 1f)
            cell.add(img).size(176f, 99f).pad(6f, 6f, 0f, 6f).row()
            cell.add(ui.label(w.name, ui.smallFont, UiKit.CREAM, scale = 0.72f)).row()
            if (unlocked) {
                cell.add(ui.label("Best ${d.bestMetresByWorld[w.id] ?: 0} m" + if (w.id in d.worldsCleared) "  ✓" else "", ui.smallFont, UiKit.TEAL, scale = 0.7f)).padBottom(6f)
            } else {
                val prev = Worlds.all[i - 1]
                val boss = Bosses.byId(prev.bosses[0])
                cell.add(ui.label("Beat the ${boss.name} in ${prev.name}", ui.smallFont, UiKit.GREY, scale = 0.55f, wrap = true, align = Align.center)).width(230f).padBottom(4f)
            }
            if (unlocked) {
                cell.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                cell.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                        game.audio.play(Sfx.UI_CLICK)
                        game.save.transact { it.selectedWorld = w.id }
                        rebuild()
                    }
                })
            }
            content.add(cell).width(250f).height(196f).pad(4f)
            if (i % 4 == 3) content.row()
        }
    }
}

class ShopScreen(game: BrambleDash) : PanelScreen(game, "shop") {
    private val rng = Random(System.nanoTime())

    override fun buildContent() {
        val d = game.save.data
        val left = Table()
        left.add(ui.label("Runners", ui.bodyFont, UiKit.CREAM)).left().colspan(4).padBottom(6f).row()
        for (c in Characters.all) {
            left.add(ui.image("ui/shop/card_${c.id}")).size(48f, 64f).pad(3f)
            left.add(ui.label(c.name, ui.smallFont, UiKit.CREAM, scale = 0.85f)).width(150f).left()
            if (c.id in d.unlockedCharacters) {
                left.add(ui.label("Owned", ui.smallFont, UiKit.TEAL, scale = 0.85f)).colspan(2).width(260f)
            } else {
                left.add(priceLabel("ui/icons/gold", c.price, d.coins >= c.price)).width(110f)
                left.add(ui.stateButton("ui/shop/purchase") {
                    ui.confirm(stage, "Unlock ${c.name} for ${ui.formatNumber(c.price)} coins?") {
                        var r = PurchaseResult.LOCKED
                        game.save.transact { r = Economy.buyCharacter(it, c.id); Achievements.evaluate(it) }
                        if (r == PurchaseResult.OK) { game.audio.play(Sfx.UNLOCK); ui.rewardReveal(stage, "${c.name} unlocked!", c.ability.label) { rebuild() } }
                        else failFeedback(r)
                    }
                }).size(150f, 46f)
            }
            left.row()
        }
        val right = Table()
        right.add(ui.label("Items", ui.bodyFont, UiKit.CREAM)).left().colspan(3).padBottom(6f).row()
        fun item(icon: String, name: String, priceIcon: String, price: Int, have: Int, action: () -> Pair<PurchaseResult, Reward?>) {
            right.add(ui.image(icon)).size(56f).pad(3f)
            right.add(ui.label(name, ui.smallFont, UiKit.CREAM, scale = 0.85f)).width(190f).left()
            right.add(priceLabel(priceIcon, price, have >= price)).width(90f)
            right.add(ui.stateButton("ui/shop/purchase") {
                ui.confirm(stage, "Buy $name for $price ${if (priceIcon.endsWith("diamond")) "gems" else "bonus tokens"}?") {
                    var res: Pair<PurchaseResult, Reward?> = PurchaseResult.LOCKED to null
                    game.save.transact { res = action() }
                    if (res.first == PurchaseResult.OK) {
                        game.audio.play(Sfx.PURCHASE_OK)
                        val r = res.second
                        if (r != null) { game.audio.play(Sfx.CHEST_OPEN); ui.rewardReveal(stage, "You got", r.describe()) { rebuild() } }
                        else rebuild()
                    } else failFeedback(res.first)
                }
            }).size(150f, 46f).row()
        }
        item("collectibles/mystery_box/reward/00", "Mystery Box", "ui/icons/diamond", Tuning.GEMS_PER_BOX, d.gems) { Economy.buyMysteryBox(game.save.data, false, rng) }
        item("collectibles/mystery_box/reward/00", "Mystery Box", "ui/icons/star", Tuning.TOKENS_PER_BOX, d.tokens) { Economy.buyMysteryBox(game.save.data, true, rng) }
        item("collectibles/key/reward/00", "Key", "ui/icons/diamond", Tuning.GEMS_PER_KEY, d.gems) { Economy.buyKey(game.save.data) to Reward(keys = 1) }
        item("collectibles/gold_coin/spin/00", "${Tuning.COINS_PER_GEM_EXCHANGE} Coins", "ui/icons/diamond", 1, d.gems) { Economy.exchangeGem(game.save.data) to Reward(coins = Tuning.COINS_PER_GEM_EXCHANGE) }
        val odds = MysteryBox.table.joinToString("  ") { it.weight.toString() + "%" }
        right.add(ui.label("Mystery box odds: 45% 100-300 coins, 20% 2-5 gems, 15% key, 10% 3 tokens, 7% 800 coins, 3% 15 gems", ui.smallFont, UiKit.GREY, scale = 0.6f, wrap = true)).colspan(4).width(500f).padTop(8f)
        check(odds.isNotEmpty())
        content.add(left).expand().top().left()
        content.add(right).expand().top().right()
    }
}

class UpgradesScreen(game: BrambleDash) : PanelScreen(game, "upgrades") {
    override fun buildContent() {
        val d = game.save.data
        for (u in Upgrade.values()) {
            val lvl = d.upgrade(u.id)
            val row = Table()
            row.background = ui.panel()
            row.add(ui.image(u.iconRegion)).size(52f).pad(4f, 14f, 4f, 8f)
            val info = Table()
            val head = Table()
            head.add(ui.label(u.title, ui.smallFont, UiKit.CREAM, scale = 0.85f)).padRight(12f)
            for (i in 0 until u.maxLevel) head.add(Image(ui.whiteDrawable(if (i < lvl) UiKit.GOLD else Color(0.2f, 0.27f, 0.38f, 1f)))).size(26f, 10f).padRight(4f)
            info.add(head).left().row()
            val next = if (lvl < u.maxLevel) "   next: ${u.describe(lvl + 1)}" else "   (max level)"
            info.add(ui.label("Now: ${u.describe(lvl)}$next", ui.smallFont, UiKit.TEAL, scale = 0.62f)).left().padTop(2f)
            row.add(info).expandX().left()
            val price = Economy.upgradePrice(d, u)
            if (price != null) {
                row.add(priceLabel("ui/icons/gold", price, d.coins >= price)).width(110f)
                val btn = ui.stateButton(if (u == Upgrade.HEALTH) "ui/upgrades/upgrade_health" else "ui/upgrades/upgrade_power") {
                    var r = PurchaseResult.LOCKED
                    game.save.transact { r = Economy.buyUpgrade(it, u); Achievements.evaluate(it) }
                    if (r == PurchaseResult.OK) { game.audio.play(Sfx.PURCHASE_OK); ui.toast(stage, "${u.title} upgraded!", UiKit.GOLD); rebuild() } else failFeedback(r)
                }
                row.add(btn).size(200f, 61f).padRight(12f)
            } else {
                val btn = Button(Button.ButtonStyle().apply { disabled = ui.drawable("ui/upgrades/upgrade_power/disabled"); up = disabled })
                btn.isDisabled = true
                row.add(ui.label("MAX", ui.smallFont, UiKit.GOLD)).width(110f)
                row.add(btn).size(200f, 61f).padRight(12f)
            }
            content.add(row).growX().height(78f).padBottom(4f).row()
        }
    }
}

class SettingsScreen(game: BrambleDash) : PanelScreen(game, "settings") {
    override fun buildContent() {
        val s = game.save.data.settings
        val t = Table()
        t.defaults().pad(6f)
        t.add(ui.label("Music volume", ui.smallFont, UiKit.CREAM)).left()
        t.add(ui.slider(s.musicVolume) { v -> s.musicVolume = v; game.applyAudioSettings() }).width(300f)
        t.add(ui.label("Effects volume", ui.smallFont, UiKit.CREAM)).left().padLeft(30f)
        t.add(ui.slider(s.sfxVolume) { v -> s.sfxVolume = v; game.applyAudioSettings(); game.audio.play(Sfx.COIN) }).width(300f).row()

        val toggles = Table()
        // pack buttons: "selected" art = feature on
        val sound = ui.stateButton("ui/settings/sound", toggle = true) { b -> s.muted = !b.isChecked; game.applyAudioSettings(); persist() }
        sound.isChecked = !s.muted
        val music = ui.stateButton("ui/settings/music", toggle = true) { b -> s.musicVolume = if (b.isChecked) 0.6f else 0f; game.applyAudioSettings(); persist(); rebuild() }
        music.isChecked = s.musicVolume > 0f
        val vib = ui.stateButton("ui/settings/vibration", toggle = true) { b -> s.vibration = b.isChecked; persist(); if (b.isChecked) game.vibrate(40) }
        vib.isChecked = s.vibration
        toggles.add(sound).size(230f, 70f).pad(6f)
        toggles.add(music).size(230f, 70f).pad(6f)
        toggles.add(vib).size(230f, 70f).pad(6f)
        t.add(toggles).colspan(4).padTop(6f).row()

        val q = Table()
        q.add(ui.label("Effects quality", ui.smallFont, UiKit.CREAM)).padRight(12f)
        listOf("LOW", "MEDIUM", "HIGH").forEachIndexed { i, name ->
            val b = ui.textButton(name, UiKit.Tone.PLAIN) { s.quality = i; persist(); rebuild() }
            b.isChecked = s.quality == i
            q.add(b).padRight(6f)
        }
        t.add(q).colspan(4).row()
        val c = Table()
        val hand = ui.textButton(if (s.leftHanded) "JUMP ON LEFT" else "JUMP ON RIGHT", UiKit.Tone.PLAIN) { s.leftHanded = !s.leftHanded; persist(); rebuild() }
        val swipe = ui.textButton(if (s.swipeControls) "SWIPES: ON" else "SWIPES: OFF", UiKit.Tone.PLAIN) { s.swipeControls = !s.swipeControls; persist(); rebuild() }
        val tut = ui.textButton("REPLAY TUTORIAL", UiKit.Tone.PLAIN) { game.save.transact { it.tutorialDone = false }; ui.toast(stage, "The tutorial will play on your next run") }
        val reset = ui.textButton("RESET PROGRESS", UiKit.Tone.DANGER) {
            ui.confirm(stage, "Erase all coins, unlocks and records? Settings are kept.") { game.save.resetProgress(); ui.toast(stage, "Progress reset", UiKit.RED); rebuild() }
        }
        c.add(hand).pad(4f); c.add(swipe).pad(4f); c.add(tut).pad(4f); c.add(reset).pad(4f)
        t.add(c).colspan(4).padTop(4f)
        content.add(t).expand().top()
    }

    private fun persist() = game.save.commit()

    override fun onBack() { persist(); super.onBack() }
}

class DailyRewardsScreen(game: BrambleDash) : PanelScreen(game, "daily_rewards") {
    private val rng = Random(System.nanoTime())

    override fun buildContent() {
        val d = game.save.data
        val today = game.today()
        val status = DailyRewards.status(d, today)
        val next = DailyRewards.nextIndex(d, today)
        val row = Table()
        DailyRewards.rewards.forEachIndexed { i, r ->
            val tile = Table()
            val claimedToday = status == DailyRewards.Status.CLAIMED_TODAY && i == (d.dailyStreak + 6) % 7
            val highlight = (status == DailyRewards.Status.AVAILABLE && i == next) || claimedToday
            tile.background = ui.panel(if (highlight) Color(1f, 0.82f, 0.45f, 1f) else Color.WHITE)
            tile.add(ui.label("Day ${i + 1}", ui.smallFont, UiKit.CREAM, scale = 0.8f)).row()
            val icon = when {
                r.gems > 0 -> "ui/icons/diamond"; r.keys > 0 -> "ui/icons/key"; r.tokens > 0 -> "ui/icons/star"; else -> "ui/icons/gold"
            }
            tile.add(ui.image(icon)).size(52f).pad(4f).row()
            tile.add(ui.label(r.describe(), ui.smallFont, UiKit.TEAL, scale = 0.6f, wrap = true, align = Align.center)).width(118f)
            if (i < next && status == DailyRewards.Status.AVAILABLE || claimedToday || (status == DailyRewards.Status.CLAIMED_TODAY && i < d.dailyStreak)) tile.color.a = 0.55f
            row.add(tile).size(134f, 168f).pad(4f)
        }
        content.add(row).colspan(2).row()
        val claim = ui.stateButton("ui/rewards/claim") {
            var r: Reward? = null
            game.save.transact { r = DailyRewards.claim(it, today) }
            r?.let { game.audio.play(Sfx.CHEST_OPEN); ui.rewardReveal(stage, "Day ${next + 1} reward", it.describe()) { rebuild() } }
        }
        claim.isDisabled = status != DailyRewards.Status.AVAILABLE
        val msg = when (status) {
            DailyRewards.Status.AVAILABLE -> "Today's reward is ready!"
            DailyRewards.Status.CLAIMED_TODAY -> "Come back tomorrow for day ${DailyRewards.nextIndex(d, today + 1) + 1}."
            DailyRewards.Status.CLOCK_BEHIND -> "Device date is earlier than your last claim."
        }
        val left = Table()
        left.add(claim).size(240f, 73f).row()
        left.add(ui.label(msg, ui.smallFont, UiKit.CREAM, scale = 0.75f)).padTop(4f).row()
        left.add(ui.label("Uses this device's calendar; missing a day restarts the cycle.", ui.smallFont, UiKit.GREY, scale = 0.55f)).padTop(2f)
        val chest = Table()
        chest.background = ui.panel()
        chest.add(ui.image("collectibles/treasure_chest/reward/00")).size(70f).pad(6f)
        chest.add(ui.label("Treasure chests: ${d.chests}\nKeys: ${d.keys}", ui.smallFont, UiKit.CREAM, scale = 0.8f)).padRight(10f)
        chest.add(ui.textButton("OPEN CHEST", UiKit.Tone.GOLD) {
            var res: Pair<PurchaseResult, Reward?> = PurchaseResult.LOCKED to null
            var newly = emptyList<String>()
            game.save.transact { res = Economy.openChest(it, rng); if (res.first == PurchaseResult.OK) { newly = Missions.onChestOpened(it); Achievements.evaluate(it) } }
            val r = res.second
            if (res.first == PurchaseResult.OK && r != null) {
                game.audio.play(Sfx.CHEST_OPEN)
                if (newly.isNotEmpty()) game.audio.play(Sfx.MISSION_COMPLETE)
                ui.rewardReveal(stage, "Treasure!", r.describe()) { rebuild() }
            } else if (res.first == PurchaseResult.INSUFFICIENT_FUNDS) { game.audio.play(Sfx.PURCHASE_FAIL); ui.toast(stage, "You need a key. Find keys in runs or buy one in the shop.", UiKit.RED) }
            else failFeedback(res.first)
        }).padRight(8f)
        content.add(left).expand().padTop(10f)
        content.add(chest).expand().padTop(10f)
    }
}

class MissionsScreen(game: BrambleDash) : PanelScreen(game, "missions") {
    override fun buildContent() {
        val d = game.save.data
        Missions.refill(d)
        if (d.missions.isEmpty()) content.add(ui.label("All missions complete. Great running!", ui.bodyFont, UiKit.GOLD))
        for (m in d.missions.toList()) {
            val def = Missions.def(m.id)
            val row = Table()
            row.background = ui.panel()
            row.add(ui.image("ui/icons/star")).size(46f).pad(8f)
            val info = Table()
            info.add(ui.label(def.text, ui.smallFont, UiKit.CREAM)).left().row()
            val bar = BarActor(game.assets.region("ui/gameplay/progress"), game.assets.white, 14f, 280f, 22f, 45f, Color(48 / 255f, 68 / 255f, 95 / 255f, 1f))
            bar.fraction = m.progress / def.target.toFloat()
            val pb = Table()
            pb.add(bar).size(260f, 52f)
            pb.add(ui.label("${ui.formatNumber(m.progress)} / ${ui.formatNumber(def.target)}", ui.smallFont, UiKit.TEAL, scale = 0.75f)).padLeft(10f)
            info.add(pb).left()
            row.add(info).expandX().left()
            row.add(ui.label(def.reward.describe(), ui.smallFont, UiKit.GOLD, scale = 0.75f)).width(170f)
            val claim = ui.stateButton("ui/rewards/claim") {
                var r: Reward? = null
                game.save.transact { r = Missions.claim(it, m.id); Achievements.evaluate(it) }
                r?.let { game.audio.play(Sfx.MISSION_COMPLETE); ui.rewardReveal(stage, "Mission complete!", it.describe()) { rebuild() } }
            }
            claim.isDisabled = !Missions.isComplete(m)
            row.add(claim).size(200f, 61f).padRight(10f)
            content.add(row).growX().height(104f).padBottom(8f).row()
        }
        content.add(ui.label("Completed: ${d.missionsCompleted} / ${Missions.pool.size}", ui.smallFont, UiKit.GREY, scale = 0.75f)).padTop(4f)
    }
}

class AchievementsScreen(game: BrambleDash) : PanelScreen(game, "achievements") {
    override fun buildContent() {
        val d = game.save.data
        Achievements.evaluate(d)
        val list = Table()
        for (a in Achievements.all) {
            val st = d.achievements[a.id] ?: 0
            val row = Table()
            row.background = ui.panel(if (st == 0) Color(0.75f, 0.75f, 0.8f, 1f) else Color.WHITE)
            val icon = ui.image(a.icon)
            if (st == 0) icon.color = Color(0.3f, 0.3f, 0.35f, 1f)
            row.add(icon).size(44f).pad(6f)
            val info = Table()
            info.add(ui.label(a.title, ui.smallFont, if (st == 0) UiKit.GREY else UiKit.CREAM, scale = 0.85f)).left().row()
            info.add(ui.label(a.text + "   Reward: " + a.reward.describe(), ui.smallFont, UiKit.TEAL, scale = 0.62f)).left()
            row.add(info).expandX().left()
            when (st) {
                2 -> row.add(ui.label("CLAIMED ✓", ui.smallFont, UiKit.GOLD, scale = 0.8f)).width(200f)
                1 -> row.add(ui.stateButton("ui/rewards/claim") {
                    var r: Reward? = null
                    game.save.transact { r = Achievements.claim(it, a.id) }
                    r?.let { game.audio.play(Sfx.UNLOCK); ui.rewardReveal(stage, a.title, it.describe()) { rebuild() } }
                }).size(180f, 55f).padRight(10f)
                else -> row.add(ui.label("LOCKED", ui.smallFont, UiKit.GREY, scale = 0.8f)).width(200f)
            }
            list.add(row).growX().height(70f).padBottom(5f).row()
        }
        game.save.commit()
        content.add(ui.scroll(list)).grow()
    }
}

class LeaderboardScreen(game: BrambleDash) : PanelScreen(game, null, "LOCAL LEADERBOARD") {
    override fun buildContent() {
        val d = game.save.data
        content.add(ui.label("Top 10 runs saved on this device. There is no online service.", ui.smallFont, UiKit.GREY, scale = 0.7f)).colspan(6).padBottom(8f).row()
        val headers = listOf("#", "Score", "Distance", "Runner", "World", "Date")
        for (h in headers) content.add(ui.label(h, ui.smallFont, UiKit.TEAL, scale = 0.8f)).pad(2f, 14f, 2f, 14f)
        content.row()
        if (d.leaderboard.isEmpty()) content.add(ui.label("No runs yet. Hit PLAY!", ui.smallFont, UiKit.CREAM)).colspan(6).padTop(20f)
        d.leaderboard.forEachIndexed { i, e ->
            val c = if (i == 0) UiKit.GOLD else UiKit.CREAM
            content.add(ui.label("${i + 1}", ui.smallFont, c, scale = 0.8f))
            content.add(ui.label(ui.formatNumber(e.score), ui.smallFont, c, scale = 0.8f))
            content.add(ui.label("${e.distance} m", ui.smallFont, c, scale = 0.8f))
            content.add(ui.label(Characters.byId(e.character).name, ui.smallFont, c, scale = 0.8f))
            content.add(ui.label(Worlds.byId(e.world).name, ui.smallFont, c, scale = 0.8f))
            content.add(ui.label(BrambleDash.formatEpochDay(e.day), ui.smallFont, c, scale = 0.7f))
            content.row()
        }
        content.top()
    }
}

class ProfileScreen(game: BrambleDash) : PanelScreen(game, null, "PROFILE") {
    override fun buildContent() {
        val d = game.save.data
        val s = d.stats
        val left = Table()
        left.add(ui.image("ui/shop/card_${d.selectedCharacter}")).size(180f, 240f).row()
        left.add(ui.label(Characters.byId(d.selectedCharacter).name, ui.smallFont, UiKit.CREAM)).row()
        left.add(ui.label("${d.unlockedCharacters.size}/5 runners • ${d.unlockedWorlds.size}/8 worlds", ui.smallFont, UiKit.TEAL, scale = 0.7f))
        val right = Table()
        right.defaults().left().pad(2f, 10f, 2f, 10f)
        val rows = listOf(
            "Runs" to s.runs.toString(),
            "Best score" to ui.formatNumber(d.bestScore),
            "Best distance" to "${d.bestMetres} m",
            "Total distance" to "${s.totalMetres} m",
            "Coins earned" to ui.formatNumber(s.coinsEarned.toInt()),
            "Enemies defeated" to s.enemiesDefeated.toString(),
            "Bosses defeated" to "${s.bossesDefeated} (${d.bossesBeaten.size}/5 kinds)",
            "Hazards slid under" to s.slidesUnder.toString(),
            "Power-ups used" to s.powerupsUsed.toString(),
            "Chests opened" to s.chestsOpened.toString(),
            "Missions completed" to d.missionsCompleted.toString(),
            "Achievements" to "${d.achievements.size}/${Achievements.all.size}",
            "Play time" to "${s.playSeconds / 60} min",
        )
        rows.forEachIndexed { i, (k, v) ->
            right.add(ui.label(k, ui.smallFont, UiKit.TEAL, scale = 0.75f))
            right.add(ui.label(v, ui.smallFont, UiKit.CREAM, scale = 0.75f))
            if (i % 2 == 1) right.row()
        }
        // power-up guide using the pack's power-up icons
        val guide = Table()
        guide.add(ui.label("Power-ups", ui.smallFont, UiKit.CREAM, scale = 0.8f)).colspan(5).left().padBottom(2f).row()
        com.bramble.dash.config.PowerUpType.values().forEachIndexed { i, p ->
            val cell = Table()
            cell.add(ui.image("powerups/${p.id}/icon")).size(40f).padRight(4f)
            cell.add(ui.label(p.title, ui.smallFont, UiKit.TEAL, scale = 0.6f)).width(110f).left()
            guide.add(cell).left().pad(2f)
            if (i % 5 == 4) guide.row()
        }
        right.row()
        right.add(guide).colspan(4).left().padTop(10f)
        content.add(left).padRight(30f)
        content.add(right).expand().top().left()
    }
}
