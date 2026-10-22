package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** Shop with four tabs: coin packs, boosters, girl outfits / hats and dragon skins. */
class ShopScreen(game: SnakeAttackGame, startTab: Int = TAB_BOOSTERS) : MetaScreen(game, "SHOP", "ribbon_blue") {
    private val tabs = listOf("COINS", "BOOSTERS", "OUTFITS", "DRAGONS")
    private var tab = 0
    private val tabBtns = tabs.mapIndexed { i, t -> Button(Assets.r("ui_tab_button_inactive"), 166f, t, 24f) { selectTab(i) } }
    private var scroll = 0f
    private var velocity = 0f
    private var girl = Girl()
    private val dragon = DragonHead()
    private var previewSkin: DragonSkin = DragonSkinCatalog.shop.first { it.id == Save.dragonSkin }
    private var gridTop = 0f
    private val cardW = 205f
    private val cardH = 250f
    private val packTexts = Billing.packs.map { Ui.num(it.coins) }

    init {
        buttons += tabBtns
        girl.scale = 0.42f
        girl.mood = Girl.Mood.WAVE
        dragon.scale = 0.62f
        dragon.angry = true
        selectTab(startTab)
    }

    /** Coins tab: the free video card first, then the coin packs. */
    private val cardCount get() = when (tab) {
        TAB_COINS -> Billing.packs.size + 1
        TAB_BOOSTERS -> Shop.boosters.size
        TAB_OUTFITS -> Shop.outfits.size
        else -> Shop.skins.size
    }

    private fun items() = when (tab) {
        TAB_BOOSTERS -> Shop.boosters
        TAB_OUTFITS -> Shop.outfits
        else -> Shop.skins
    }

    private fun selectTab(i: Int) {
        tab = i
        scroll = 0f
        tabBtns.forEachIndexed { k, b -> b.region = Assets.r(if (k == i) "ui_tab_button_active" else "ui_tab_button_inactive") }
    }

    override fun openCoinShop() = selectTab(TAB_COINS)

    override fun layout() {
        super.layout()
        tabBtns.forEachIndexed { i, b ->
            b.x = W / 2 + (i - 1.5f) * 172f
            b.y = top - 160f
        }
        gridTop = top - 470f
        girl.baseX = W / 2
        girl.baseY = top - 420f
        dragon.x = W / 2 + 90f
        dragon.y = top - 300f
    }

    /** The coins tab has no preview strip, so its grid starts higher. */
    private fun gridStart() = if (tab == TAB_COINS) top - 220f else gridTop

    private fun cardPos(i: Int): Pair<Float, Float> {
        val col = i % 3
        val row = i / 3
        return Pair(W / 2 + (col - 1) * (cardW + 18f), gridStart() - cardH / 2 - row * (cardH + 18f) + scroll)
    }

    private fun maxScroll(): Float {
        val rows = (cardCount + 2) / 3
        return max(0f, rows * (cardH + 18f) - (gridStart() - 20f))
    }

    override fun onDrag(dx: Float, dy: Float) {
        scroll = MathUtils.clamp(scroll + dy, 0f, maxScroll())
        velocity = dy * 60f
    }

    override fun onTap(x: Float, y: Float) {
        if (y > gridStart() + 10f) return
        for (i in 0 until cardCount) {
            val (cx, cy) = cardPos(i)
            if (abs(x - cx) < cardW / 2 && abs(y - cy) < cardH / 2) {
                if (tab == TAB_COINS) tapCoins(i, cx, cy) else select(items()[i], cx, cy)
                return
            }
        }
    }

    private fun tapCoins(i: Int, cx: Float, cy: Float) {
        if (i == 0) {
            Ads.showRewarded("+${Billing.AD_COINS} COINS") {
                Save.coins += Billing.AD_COINS
                particles.coinsFly(cx, cy, 8)
            }
            return
        }
        val pack = Billing.packs[i - 1]
        Billing.buy(pack) {
            Assets.sfx("purchase", 0.9f)
            particles.sparkle(cx, cy, 16, 90f, Palette.GOLD)
            particles.coinsFly(cx, cy, 14)
            toast("+${pack.coins} coins!", pack.title, pack.icon)
        }
    }

    private fun select(item: ShopItem, cx: Float, cy: Float) {
        val ownable = item.kind == ShopItem.Kind.OUTFIT || item.kind == ShopItem.Kind.HAT || item.kind == ShopItem.Kind.SKIN
        if (ownable && Save.owns(item.id)) {
            Shop.equip(item)
            Assets.sfx("click", 0.9f)
            particles.sparkle(cx, cy, 6, 60f)
            afterChange(item)
            return
        }
        if (item.id in Shop.adUnlocks) {
            Ads.showRewarded("UNLOCKED!") {
                Save.buy(item.id)
                Shop.equip(item)
                particles.sparkle(cx, cy, 14, 80f, Palette.GOLD)
                afterChange(item)
            }
            return
        }
        if (Shop.purchase(item)) {
            Assets.sfx("purchase", 0.9f)
            particles.sparkle(cx, cy, 14, 80f, Palette.GOLD)
            particles.coinsFly(cx, cy, 6)
            toast("Purchased!", item.title, item.icon)
            afterChange(item)
            if (Achievements.newlyCompleted().isNotEmpty()) Assets.sfx("achievement", 0.7f)
        } else {
            Assets.sfx("error", 0.7f)
            toast("Not enough coins", "Tap + to get more coins", "coin")
        }
    }

    private fun afterChange(item: ShopItem) {
        when (item.kind) {
            ShopItem.Kind.OUTFIT, ShopItem.Kind.HAT -> {
                girl = Girl().also { it.scale = 0.42f; it.mood = Girl.Mood.LOVE; it.baseX = W / 2; it.baseY = top - 420f }
            }
            ShopItem.Kind.SKIN -> previewSkin = DragonSkinCatalog.shop.first { it.id == item.id }
            else -> {}
        }
    }

    override fun tick(dt: Float) {
        scroll = MathUtils.clamp(scroll + velocity * dt, 0f, maxScroll())
        velocity *= 1f / (1f + dt * 5f)
        girl.update(dt)
        dragon.mouthOpen = 0.4f + 0.3f * sin(time * 1.6f)
        dragon.update(dt)
    }

    override fun content() {
        val b = batch
        tabBtns.forEach { it.draw(b) }
        when (tab) {
            TAB_COINS -> {}
            TAB_BOOSTERS -> {
                b.drawCentered(Assets.r("ui_booster_pack_box"), W / 2, top - 300f, 170f, rotation = sin(time * 2f) * 4f)
                Booster.entries.forEachIndexed { i, bst ->
                    val x = W / 2 + (i - 1.5f) * 150f
                    b.text(Assets.fontBig, "${bst.title} x${Save.boosters(bst)}", x, top - 400f, 22f)
                }
            }
            TAB_OUTFITS -> girl.render(b)
            else -> {
                val skin = if (previewSkin === DragonSkinCatalog.AUTO) DragonSkinCatalog.RED else previewSkin
                HueShader.begin(b, skin, time)
                dragon.drawNeck(b, dragon.x + 70f, dragon.y - 30f, 72f)
                dragon.draw(b)
                HueShader.end(b)
            }
        }
        for (i in 0 until cardCount) {
            val (cx, cy) = cardPos(i)
            if (cy + cardH / 2 > gridStart() + 30f || cy + cardH / 2 < 0f) continue
            if (tab == TAB_COINS) drawCoinCard(i, cx, cy) else drawCard(items()[i], cx, cy)
        }
        b.color = Color.WHITE
    }

    private fun drawCoinCard(i: Int, cx: Float, cy: Float) {
        val b = batch
        b.drawCentered(Assets.r("ui_coin_pack_card"), cx, cy, cardW, cardH)
        if (i == 0) {
            val pulse = 1f + sin(time * 4f) * 0.04f
            b.drawCentered(Assets.r("ui_free_gift_ad"), cx, cy + 22f, 120f * pulse)
            b.text(Assets.fontBig, "FREE", cx, cy + cardH / 2 - 26f, 26f, Palette.GOLD)
            b.drawCentered(Assets.r("ui_video_ad_button_green"), cx, cy - cardH / 2 + 34f, 176f)
            b.text(Assets.fontBig, "+${Billing.AD_COINS}", cx + 22f, cy - cardH / 2 + 36f, 28f)
            return
        }
        val pack = Billing.packs[i - 1]
        b.setColor(1f, 0.9f, 0.5f, 0.35f + 0.15f * sin(time * 3f + i))
        b.drawCentered(Assets.r("ui_sparkle_glow"), cx, cy + 20f, 170f)
        b.color = Color.WHITE
        b.drawCentered(Assets.r(pack.icon), cx, cy + 20f, 140f)
        b.drawCentered(Assets.r("coin"), cx - 44f, cy + cardH / 2 - 28f, 30f)
        b.text(Assets.fontBig, packTexts[i - 1], cx + 12f, cy + cardH / 2 - 26f, 28f, Palette.GOLD)
        b.drawCentered(Assets.r("ui_price_button_green"), cx, cy - cardH / 2 + 34f, 160f)
        b.text(Assets.fontBig, pack.price, cx, cy - cardH / 2 + 37f, 28f)
        if (pack.tag.isNotEmpty()) {
            // tag banner across the lower part of the art, kept inside the card
            val best = pack.tag == "BEST VALUE"
            val tw = cardW - 30f
            b.setColor(if (best) 0.85f else 1f, if (best) 0.15f else 0.55f, if (best) 0.15f else 0.1f, 0.95f)
            b.draw(Assets.white, cx - tw / 2, cy - 44f, tw, 30f)
            b.color = Color.WHITE
            b.text(Assets.fontBig, pack.tag, cx, cy - 29f, 22f)
        }
    }

    private fun drawCard(item: ShopItem, cx: Float, cy: Float) {
        val b = batch
        val equipped = Shop.isEquipped(item)
        val ownable = item.kind == ShopItem.Kind.OUTFIT || item.kind == ShopItem.Kind.HAT || item.kind == ShopItem.Kind.SKIN
        val owned = ownable && Save.owns(item.id)
        b.drawCentered(Assets.r(if (equipped) "ui_shop_item_card_selected" else "ui_shop_item_card"), cx, cy, cardW)
        b.drawCentered(Assets.r(item.icon), cx, cy + 22f, if (item.kind == ShopItem.Kind.SKIN) 150f else 120f)
        b.text(Assets.fontBig, item.title, cx, cy + cardH / 2 - 26f, 24f)
        val by = cy - cardH / 2 + 30f
        when {
            equipped -> b.drawCentered(Assets.r("ui_equipped_check_badge"), cx, by + 6f, 56f)
            owned -> b.text(Assets.fontBig, "OWNED", cx, by + 4f, 28f, Palette.GREEN)
            item.id in Shop.adUnlocks -> {
                b.drawCentered(Assets.r("ui_video_ad_button_green"), cx, by + 4f, 170f)
                b.text(Assets.fontBig, "FREE", cx + 24f, by + 6f, 26f)
                b.drawCentered(Assets.r("video_ad_badge_small"), cx + cardW / 2 - 26f, cy + cardH / 2 - 26f, 46f)
            }
            else -> {
                b.drawCentered(Assets.r("ui_price_tag_coin"), cx, by + 4f, 160f)
                b.text(Assets.fontBig, if (item.price == 0) "FREE" else item.price.toString(), cx + 18f, by + 6f, 28f)
            }
        }
    }

    companion object {
        const val TAB_COINS = 0
        const val TAB_BOOSTERS = 1
        const val TAB_OUTFITS = 2
        const val TAB_DRAGONS = 3
    }
}
