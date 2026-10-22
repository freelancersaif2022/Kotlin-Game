package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** Shop with three tabs: boosters, girl outfits / hats and dragon skins. */
class ShopScreen(game: SnakeAttackGame) : MetaScreen(game, "SHOP", "ribbon_blue") {
    private val tabs = listOf("BOOSTERS", "OUTFITS", "DRAGONS")
    private var tab = 0
    private val tabBtns = tabs.mapIndexed { i, t -> Button(Assets.r("ui_tab_button_inactive"), 210f, t, 30f) { selectTab(i) } }
    private var scroll = 0f
    private var velocity = 0f
    private var girl = Girl()
    private val dragon = DragonHead()
    private var previewSkin: DragonSkin = DragonSkinCatalog.shop.first { it.id == Save.dragonSkin }
    private var gridTop = 0f
    private val cardW = 205f
    private val cardH = 250f

    init {
        buttons += tabBtns
        girl.scale = 0.42f
        girl.mood = Girl.Mood.WAVE
        dragon.scale = 0.62f
        dragon.angry = true
        selectTab(0)
    }

    private val items get() = when (tab) {
        0 -> Shop.boosters
        1 -> Shop.outfits
        else -> Shop.skins
    }

    private fun selectTab(i: Int) {
        tab = i
        scroll = 0f
        tabBtns.forEachIndexed { k, b -> b.region = Assets.r(if (k == i) "ui_tab_button_active" else "ui_tab_button_inactive") }
    }

    override fun layout() {
        super.layout()
        tabBtns.forEachIndexed { i, b ->
            b.x = W / 2 + (i - 1) * 220f
            b.y = top - 160f
        }
        gridTop = top - 470f
        girl.baseX = W / 2
        girl.baseY = top - 420f
        dragon.x = W / 2 + 90f
        dragon.y = top - 300f
    }

    private fun cardPos(i: Int): Pair<Float, Float> {
        val col = i % 3
        val row = i / 3
        return Pair(W / 2 + (col - 1) * (cardW + 18f), gridTop - cardH / 2 - row * (cardH + 18f) + scroll)
    }

    private fun maxScroll(): Float {
        val rows = (items.size + 2) / 3
        return max(0f, rows * (cardH + 18f) - (gridTop - 20f))
    }

    override fun onDrag(dx: Float, dy: Float) {
        scroll = MathUtils.clamp(scroll + dy, 0f, maxScroll())
        velocity = dy * 60f
    }

    override fun onTap(x: Float, y: Float) {
        if (y > gridTop + 10f) return
        items.forEachIndexed { i, item ->
            val (cx, cy) = cardPos(i)
            if (abs(x - cx) < cardW / 2 && abs(y - cy) < cardH / 2) {
                select(item, cx, cy)
                return
            }
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
        if (Shop.purchase(item)) {
            Assets.sfx("purchase", 0.9f)
            particles.sparkle(cx, cy, 14, 80f, Palette.GOLD)
            particles.coinsFly(cx, cy, 6)
            toast("Purchased!", item.title, item.icon)
            afterChange(item)
            if (Achievements.newlyCompleted().isNotEmpty()) Assets.sfx("achievement", 0.7f)
        } else {
            Assets.sfx("error", 0.7f)
            toast("Not enough coins", "Win levels or spin the wheel", "coin")
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
        // preview strip
        when (tab) {
            0 -> {
                b.drawCentered(Assets.r("ui_booster_pack_box"), W / 2, top - 300f, 170f, rotation = sin(time * 2f) * 4f)
                Booster.entries.forEachIndexed { i, bst ->
                    val x = W / 2 + (i - 1.5f) * 150f
                    b.text(Assets.fontBig, "${bst.title} x${Save.boosters(bst)}", x, top - 400f, 22f)
                }
            }
            1 -> girl.render(b)
            else -> {
                val skin = if (previewSkin === DragonSkinCatalog.AUTO) DragonSkinCatalog.RED else previewSkin
                HueShader.begin(b, skin, time)
                dragon.drawNeck(b)
                dragon.draw(b)
                HueShader.end(b)
            }
        }
        // cards (clipped below the preview strip by drawing order + a header cover)
        items.forEachIndexed { i, item ->
            val (cx, cy) = cardPos(i)
            if (cy - cardH / 2 > gridTop + 20f || cy + cardH / 2 < 0f) return@forEachIndexed
            drawCard(item, cx, cy)
        }
        // cover the area above the grid so scrolled cards slide under the preview
        b.color = Color.WHITE
    }

    private fun drawCard(item: ShopItem, cx: Float, cy: Float) {
        val b = batch
        val equipped = Shop.isEquipped(item)
        val ownable = item.kind == ShopItem.Kind.OUTFIT || item.kind == ShopItem.Kind.HAT || item.kind == ShopItem.Kind.SKIN
        val owned = ownable && Save.owns(item.id)
        if (cy + cardH / 2 > gridTop + 30f) return
        b.drawCentered(Assets.r(if (equipped) "ui_shop_item_card_selected" else "ui_shop_item_card"), cx, cy, cardW)
        b.drawCentered(Assets.r(item.icon), cx, cy + 22f, if (item.kind == ShopItem.Kind.SKIN) 150f else 120f)
        b.text(Assets.fontBig, item.title, cx, cy + cardH / 2 - 26f, 24f)
        val by = cy - cardH / 2 + 30f
        when {
            equipped -> b.drawCentered(Assets.r("ui_equipped_check_badge"), cx, by + 6f, 56f)
            owned -> b.text(Assets.fontBig, "OWNED", cx, by + 4f, 28f, Palette.GREEN)
            else -> {
                b.drawCentered(Assets.r("ui_price_tag_coin"), cx, by + 4f, 160f)
                b.text(Assets.fontBig, if (item.price == 0) "FREE" else item.price.toString(), cx + 18f, by + 6f, 28f)
            }
        }
    }
}
