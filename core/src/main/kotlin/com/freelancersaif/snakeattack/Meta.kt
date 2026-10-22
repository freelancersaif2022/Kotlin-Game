package com.freelancersaif.snakeattack

/** Shop catalogue entries. */
class ShopItem(val id: String, val title: String, val icon: String, val price: Int, val kind: Kind, val amount: Int = 1) {
    enum class Kind { BOOSTER, BOOSTER_PACK, OUTFIT, HAT, SKIN }
}

object Shop {
    val boosters = listOf(
        ShopItem("bomb", "Bomb", "ui_booster_bomb", 120, ShopItem.Kind.BOOSTER),
        ShopItem("freeze", "Freeze", "ui_booster_freeze", 100, ShopItem.Kind.BOOSTER),
        ShopItem("rainbow", "Rainbow", "ui_booster_rainbow", 150, ShopItem.Kind.BOOSTER),
        ShopItem("shuffle", "Shuffle", "ui_booster_shuffle", 80, ShopItem.Kind.BOOSTER),
        ShopItem("pack", "Mega Pack", "ui_booster_pack_box", 400, ShopItem.Kind.BOOSTER_PACK, 2),
    )

    val outfits = listOf(
        ShopItem("body_default", "Blue Dress", "ui_body_default", 0, ShopItem.Kind.OUTFIT),
        ShopItem("body_pink_dress", "Pink Dress", "ui_body_pink_dress", 300, ShopItem.Kind.OUTFIT),
        ShopItem("body_yellow_summer", "Summer", "ui_body_yellow_summer", 300, ShopItem.Kind.OUTFIT),
        ShopItem("body_orange_hoodie", "Hoodie", "ui_body_orange_hoodie", 400, ShopItem.Kind.OUTFIT),
        ShopItem("body_green_explorer", "Explorer", "ui_body_green_explorer", 500, ShopItem.Kind.OUTFIT),
        ShopItem("body_red_princess", "Princess", "ui_body_red_princess", 700, ShopItem.Kind.OUTFIT),
        ShopItem("body_purple_witch", "Witch", "ui_body_purple_witch", 700, ShopItem.Kind.OUTFIT),
        ShopItem("body_black_ninja", "Ninja", "ui_body_black_ninja", 800, ShopItem.Kind.OUTFIT),
        ShopItem("body_white_bride", "Snow Gown", "ui_body_white_bride", 900, ShopItem.Kind.OUTFIT),
        ShopItem("hat_none", "No Hat", "ui_close_x_button", 0, ShopItem.Kind.HAT),
        ShopItem("hat_flower_crown", "Flowers", "ui_hat_flower_crown", 250, ShopItem.Kind.HAT),
        ShopItem("hat_explorer", "Explorer Hat", "ui_hat_explorer", 300, ShopItem.Kind.HAT),
        ShopItem("hat_princess_tiara", "Tiara", "ui_hat_princess_tiara", 500, ShopItem.Kind.HAT),
        ShopItem("hat_witch", "Witch Hat", "ui_hat_witch", 500, ShopItem.Kind.HAT),
    )

    /** Items unlocked by watching a rewarded video instead of paying coins. */
    val adUnlocks = setOf("body_pink_dress", "body_yellow_summer", "hat_flower_crown")

    val skins = DragonSkinCatalog.shop.map { ShopItem(it.id, it.title, it.preview, it.price, ShopItem.Kind.SKIN) }

    fun isEquipped(item: ShopItem) = when (item.kind) {
        ShopItem.Kind.OUTFIT -> Save.outfit == item.id
        ShopItem.Kind.HAT -> (if (item.id == "hat_none") "" else item.id) == Save.hat
        ShopItem.Kind.SKIN -> Save.dragonSkin == item.id
        else -> false
    }

    fun equip(item: ShopItem) {
        when (item.kind) {
            ShopItem.Kind.OUTFIT -> Save.outfit = item.id
            ShopItem.Kind.HAT -> Save.hat = if (item.id == "hat_none") "" else item.id
            ShopItem.Kind.SKIN -> Save.dragonSkin = item.id
            else -> {}
        }
    }

    /** Applies a purchase; returns false when the player can't afford it. */
    fun purchase(item: ShopItem): Boolean {
        if (Save.coins < item.price) return false
        Save.coins -= item.price
        when (item.kind) {
            ShopItem.Kind.BOOSTER -> Save.addBooster(Booster.entries.first { it.key == item.id }, 1)
            ShopItem.Kind.BOOSTER_PACK -> Booster.entries.forEach { Save.addBooster(it, item.amount) }
            else -> {
                Save.buy(item.id)
                equip(item)
            }
        }
        Save.addStat("purchases")
        return true
    }
}

/** An achievement: [progress] reads the current value, done when it reaches [goal]. */
class Achievement(val id: String, val title: String, val desc: String, val icon: String, val goal: Int,
                  val reward: Int, val progress: () -> Int) {
    val done get() = progress() >= goal
}

object Achievements {
    val all = listOf(
        Achievement("first_win", "First Rescue", "Win your first level", "ui_medal_first_win", 1, 50) { Save.stat("wins") },
        Achievement("balls", "Ball Breaker", "Destroy 100 snake balls", "ui_medal_100_balls", 100, 100) { Save.stat("balls") },
        Achievement("boss", "Boss Slayer", "Defeat a Dragon King", "ui_medal_boss_slayer", 1, 200) { Save.stat("bosses") },
        Achievement("no_hint", "Brainiac", "Win level 5+ without a hint", "ui_medal_no_hint", 1, 100) { Save.stat("noHintWins") },
        Achievement("combo", "Combo Master", "Reach a 12 hit combo", "ui_medal_combo_master", 12, 150) { Save.stat("bestCombo") },
        Achievement("world", "Explorer", "Complete the first world", "ui_medal_world_complete", 10, 200) { Save.level - 1 },
        Achievement("collector", "Collector", "Buy 3 items in the shop", "ui_medal_collector", 3, 150) { Save.stat("purchases") },
        Achievement("stars_bronze", "Star Collector", "Collect 30 stars", "ui_trophy_bronze", 30, 100) { Save.totalStars },
        Achievement("stars_silver", "Star Hunter", "Collect 90 stars", "ui_trophy_silver", 90, 250) { Save.totalStars },
        Achievement("stars_gold", "Star Legend", "Collect 150 stars", "ui_trophy_gold", 150, 500) { Save.totalStars },
    )

    /** Achievements newly completed since the last check (each reported once). */
    fun newlyCompleted(): List<Achievement> = all.filter { it.done && !Save.achievementNotified(it.id) }.onEach {
        Save.setAchievementNotified(it.id)
    }

    val unclaimed get() = all.count { it.done && !Save.achievementClaimed(it.id) }
}

/** Rewards for the 7-day login calendar. */
object DailyRewards {
    class Reward(val coins: Int, val boosters: Map<Booster, Int> = emptyMap(), val label: String, val icon: String)

    val days = listOf(
        Reward(50, label = "50", icon = "ui_coin_pile_small"),
        Reward(0, mapOf(Booster.BOMB to 1), "Bomb", "ui_booster_bomb"),
        Reward(100, label = "100", icon = "ui_coin_pile_small"),
        Reward(0, mapOf(Booster.FREEZE to 2), "2 Freeze", "ui_booster_freeze"),
        Reward(150, label = "150", icon = "ui_coin_pile_big"),
        Reward(0, mapOf(Booster.RAINBOW to 1, Booster.SHUFFLE to 1), "Rainbow+", "ui_booster_rainbow"),
        Reward(300, Booster.entries.associateWith { 2 }, "Big Chest", "ui_chest_gold_closed"),
    )

    fun grant(r: Reward) {
        Save.coins += r.coins
        r.boosters.forEach { (b, n) -> Save.addBooster(b, n) }
    }
}

/** Lucky wheel slices (clockwise from the pointer at the top). */
object SpinWheel {
    class Slice(val coins: Int, val booster: Booster?, val label: String, val icon: String)

    val slices = listOf(
        Slice(20, null, "20", "coin"),
        Slice(0, Booster.BOMB, "", "booster_bomb"),
        Slice(50, null, "50", "coin"),
        Slice(0, Booster.FREEZE, "", "booster_freeze"),
        Slice(100, null, "100", "coin"),
        Slice(0, Booster.SHUFFLE, "", "booster_shuffle"),
        Slice(200, null, "200", "coin"),
        Slice(0, Booster.RAINBOW, "", "booster_rainbow"),
    )
    const val EXTRA_SPIN_PRICE = 100
}

/** Story comic captions; panel i is shown before world i (panel 5 after level 50). */
object Story {
    val captions = listOf(
        "A giant dragon-snake attacked the village and grabbed Lily!",
        "Lily escaped into the desert... but the dragon is right behind her!",
        "Hiding in the frozen peaks, Lily waits for you to save her.",
        "The Dragon King guards his volcano lair. Defeat him to free Lily!",
        "You did it! Lily is safe and the whole village celebrates!",
    )

    /** Story panel (1-based) to show before playing [level], or 0 for none. */
    fun before(level: Int) = when (level) {
        1 -> 1
        11 -> 2
        21 -> 3
        31 -> 4
        else -> 0
    }
}
