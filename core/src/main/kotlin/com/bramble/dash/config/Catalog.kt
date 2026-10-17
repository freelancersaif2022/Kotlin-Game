package com.bramble.dash.config

/** Static game content. Ids match the folder names in the supplied asset pack. */

enum class Ability(val label: String) {
    BALANCED("Balanced all-rounder"),
    AIR_CONTROL("Air control: floatier jumps and slower falls"),
    TOUGH("Tough: +1 max health, longer recovery"),
    LONG_SLIDE("Long slide: 45% longer slides"),
    MAGNET("Magnetic: coin magnet lasts 50% longer"),
}

class CharacterDef(val id: String, val name: String, val ability: Ability, val price: Int, val tagline: String)

object Characters {
    val all = listOf(
        CharacterDef("max", "Max", Ability.BALANCED, 0, "Explorer with a trusty red scarf"),
        CharacterDef("luna", "Luna", Ability.AIR_CONTROL, 1500, "Acrobat who hangs in the air"),
        CharacterDef("rex", "Rex", Ability.TOUGH, 3000, "Veteran adventurer, hard to stop"),
        CharacterDef("ninja_kai", "Ninja Kai", Ability.LONG_SLIDE, 5000, "Silent runner, masters the slide"),
        CharacterDef("robo_x", "Robo X", Ability.MAGNET, 8000, "Magnetised robot treasure hunter"),
    )
    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all[0]
}

/** How a placed hazard behaves. Decoration art is kept separate from these colliders. */
enum class ObstacleKind {
    SOLID,          // stand on top; running into the side hurts
    DAMAGE,         // static hurting trigger on the ground (jump)
    ROLLER,         // rolls toward the player (jump), warned in advance
    OSCILLATOR,     // moves up/down on a cycle (time the pass)
    HANGING,        // elevated hazard (slide under)
    TIMED,          // toggles on/off with a visible warning phase; only hurts while active
    BREAKABLE,      // stomp it (or shoot it) to destroy it; side contact hurts
    KNOCKABLE,      // harmless, knocked away with a small slow-down
    COLLAPSING,     // elevated platform that crumbles shortly after you land
    MOVING_PLATFORM,// elevated platform moving vertically
    FALLING,        // drops from above when the player approaches, shadow warning
    SLOWDOWN,       // slows the runner and weakens jumps while inside
    BOUNCE,         // spring that launches the runner
}

enum class Lane { GROUND, HIGH, PLATFORM, ANY }

class ObstacleDef(
    val id: String,
    val kind: ObstacleKind,
    val lane: Lane,
    val flipY: Boolean = false,
    val widthScale: Float = 1f,
    val heightScale: Float = 1f,
    val tint: Int = 0xFFFFFFFF.toInt(),
)

object Obstacles {
    val all = listOf(
        ObstacleDef("wooden_crate", ObstacleKind.SOLID, Lane.GROUND),
        ObstacleDef("rolling_rock", ObstacleKind.ROLLER, Lane.GROUND),
        ObstacleDef("sharp_spikes", ObstacleKind.DAMAGE, Lane.GROUND, heightScale = 0.7f),
        ObstacleDef("fallen_tree", ObstacleKind.SOLID, Lane.GROUND, heightScale = 0.8f),
        ObstacleDef("fire_pit", ObstacleKind.DAMAGE, Lane.GROUND, heightScale = 0.75f),
        ObstacleDef("lava_pool", ObstacleKind.DAMAGE, Lane.GROUND, widthScale = 1.15f, heightScale = 0.45f),
        ObstacleDef("ice_spikes", ObstacleKind.DAMAGE, Lane.GROUND, heightScale = 0.75f),
        ObstacleDef("moving_saw", ObstacleKind.OSCILLATOR, Lane.GROUND),
        ObstacleDef("swinging_axe", ObstacleKind.HANGING, Lane.HIGH),
        ObstacleDef("rotating_blade", ObstacleKind.HANGING, Lane.HIGH),
        ObstacleDef("collapsing_platform", ObstacleKind.COLLAPSING, Lane.PLATFORM),
        ObstacleDef("poison_plant", ObstacleKind.DAMAGE, Lane.GROUND, heightScale = 0.8f),
        ObstacleDef("falling_boulder", ObstacleKind.FALLING, Lane.GROUND),
        ObstacleDef("mechanical_trap", ObstacleKind.TIMED, Lane.GROUND, heightScale = 0.7f),
        ObstacleDef("laser_barrier", ObstacleKind.HANGING, Lane.HIGH),
        ObstacleDef("moving_platform", ObstacleKind.MOVING_PLATFORM, Lane.PLATFORM),
        ObstacleDef("broken_bridge", ObstacleKind.COLLAPSING, Lane.PLATFORM),
        ObstacleDef("thorn_bush", ObstacleKind.DAMAGE, Lane.GROUND, heightScale = 0.75f),
        ObstacleDef("explosive_barrel", ObstacleKind.BREAKABLE, Lane.GROUND),
        ObstacleDef("electric_barrier", ObstacleKind.TIMED, Lane.GROUND),
        ObstacleDef("cactus", ObstacleKind.DAMAGE, Lane.GROUND, heightScale = 0.8f),
        ObstacleDef("stalactite", ObstacleKind.HANGING, Lane.HIGH, flipY = true),
        ObstacleDef("snowball", ObstacleKind.ROLLER, Lane.GROUND),
        ObstacleDef("traffic_cone", ObstacleKind.KNOCKABLE, Lane.GROUND),
        ObstacleDef("spring_trap", ObstacleKind.BOUNCE, Lane.GROUND),
        ObstacleDef("haunted_stump", ObstacleKind.SOLID, Lane.GROUND, heightScale = 0.85f),
        ObstacleDef("steam_vent", ObstacleKind.TIMED, Lane.GROUND),
        ObstacleDef("crystal_cluster", ObstacleKind.DAMAGE, Lane.GROUND, heightScale = 0.75f),
        ObstacleDef("quicksand", ObstacleKind.SLOWDOWN, Lane.GROUND, widthScale = 1.6f, heightScale = 0.3f),
        ObstacleDef("hanging_chain", ObstacleKind.HANGING, Lane.HIGH),
    )
    private val map = all.associateBy { it.id }
    fun byId(id: String) = map.getValue(id)
}

enum class EnemyBehavior {
    HOPPER,      // monkey: hops toward the runner
    SWOOPER,     // bat: sine flight at head height
    LUNGER,      // snake: low, lunges when close
    SHOOTER,     // robot: telegraphed low shot
    DANGLER,     // spider: drops on a thread
    BURNER,      // fire monster: leaves short-lived fire, can't be stomped
    CHARGER,     // scorpion: fast ground charge
    ZAPPER,      // drone: hovering, telegraphed horizontal zap at head height
    THROWER,     // ice creature: lobs ice shards
    PHASER,      // shadow monster: fades in and out, only solid when visible
}

class EnemyDef(val id: String, val name: String, val behavior: EnemyBehavior, val flying: Boolean, val stompable: Boolean, val projectileFx: String? = null)

object Enemies {
    val all = listOf(
        EnemyDef("monkey", "Angry Jungle Monkey", EnemyBehavior.HOPPER, false, true),
        EnemyDef("bat", "Flying Bat", EnemyBehavior.SWOOPER, true, true),
        EnemyDef("snake", "Poisonous Snake", EnemyBehavior.LUNGER, false, true),
        EnemyDef("robot", "Evil Robot", EnemyBehavior.SHOOTER, false, true, "robot_missile"),
        EnemyDef("spider", "Giant Spider", EnemyBehavior.DANGLER, true, true),
        EnemyDef("fire_monster", "Fire Monster", EnemyBehavior.BURNER, false, false, "fire_particles"),
        EnemyDef("scorpion", "Desert Scorpion", EnemyBehavior.CHARGER, false, true),
        EnemyDef("drone", "Flying Drone", EnemyBehavior.ZAPPER, true, true, "electric_sparks"),
        EnemyDef("ice_creature", "Ice Creature", EnemyBehavior.THROWER, false, true, "ice_shard"),
        EnemyDef("shadow_monster", "Shadow Monster", EnemyBehavior.PHASER, true, true),
    )
    private val map = all.associateBy { it.id }
    fun byId(id: String) = map.getValue(id)
}

class BossDef(val id: String, val name: String, val primaryFx: String, val specialFx: String, val primaryLane: Lane, val specialLane: Lane)

object Bosses {
    val all = listOf(
        BossDef("jungle_guardian", "Giant Jungle Guardian", "guardian_projectile", "guardian_projectile", Lane.GROUND, Lane.HIGH),
        BossDef("scorpion_king", "Desert Scorpion King", "scorpion_poison", "scorpion_poison", Lane.GROUND, Lane.HIGH),
        BossDef("ice_titan", "Ice Monster Titan", "ice_shard", "ice_shard", Lane.HIGH, Lane.GROUND),
        BossDef("robot_destroyer", "Mechanical Robot Destroyer", "robot_missile", "robot_missile", Lane.HIGH, Lane.GROUND),
        BossDef("shadow_dragon", "Shadow Dragon", "dragon_flame", "dragon_flame", Lane.GROUND, Lane.HIGH),
    )
    private val map = all.associateBy { it.id }
    fun byId(id: String) = map.getValue(id)
}

class WorldDef(
    val id: String,
    val name: String,
    val enemies: List<String>,
    val obstacles: List<String>,
    /** Boss per encounter index (cycles); the first entry is the world's gate boss. */
    val bosses: List<String>,
    val difficulty: Float,
    val ambience: String,
    /** HUD/accent colour, RGBA8888 */
    val accent: Long,
    val bossHpBonus: Int = 0,
)

object Worlds {
    val all = listOf(
        WorldDef("jungle", "Tropical Jungle", listOf("monkey", "snake", "bat", "spider"),
            listOf("wooden_crate", "fallen_tree", "poison_plant", "thorn_bush", "rolling_rock", "swinging_axe", "hanging_chain", "broken_bridge", "spring_trap", "quicksand", "collapsing_platform"),
            listOf("jungle_guardian", "scorpion_king"), 1.0f, "nature", 0x6ab04cff),
        WorldDef("forest", "Sunny Green Forest", listOf("monkey", "bat", "spider", "snake"),
            listOf("wooden_crate", "fallen_tree", "thorn_bush", "haunted_stump", "falling_boulder", "rotating_blade", "moving_platform", "spring_trap", "hanging_chain", "explosive_barrel", "traffic_cone"),
            listOf("jungle_guardian", "ice_titan"), 1.08f, "nature", 0x9fd356ff, bossHpBonus = 2),
        WorldDef("desert", "Hot Desert", listOf("scorpion", "snake", "robot", "bat"),
            listOf("cactus", "quicksand", "rolling_rock", "sharp_spikes", "explosive_barrel", "swinging_axe", "broken_bridge", "fire_pit", "spring_trap", "falling_boulder", "traffic_cone"),
            listOf("scorpion_king", "robot_destroyer"), 1.16f, "wind", 0xe8b46eff),
        WorldDef("snow", "Snowy Mountain", listOf("ice_creature", "bat", "spider", "drone"),
            listOf("ice_spikes", "snowball", "stalactite", "crystal_cluster", "collapsing_platform", "moving_platform", "spring_trap", "falling_boulder", "wooden_crate", "hanging_chain"),
            listOf("ice_titan", "jungle_guardian"), 1.24f, "wind", 0xbfe6f5ff),
        WorldDef("haunted", "Dark Haunted Forest", listOf("shadow_monster", "bat", "spider", "snake"),
            listOf("haunted_stump", "thorn_bush", "hanging_chain", "swinging_axe", "broken_bridge", "poison_plant", "falling_boulder", "rotating_blade", "collapsing_platform", "spring_trap"),
            listOf("shadow_dragon", "ice_titan"), 1.32f, "eerie", 0x8a6fb5ff),
        WorldDef("city", "Futuristic City", listOf("robot", "drone", "spider", "shadow_monster"),
            listOf("traffic_cone", "laser_barrier", "electric_barrier", "mechanical_trap", "moving_saw", "steam_vent", "moving_platform", "explosive_barrel", "spring_trap", "wooden_crate"),
            listOf("robot_destroyer", "shadow_dragon"), 1.4f, "hum", 0x6fc3d6ff),
        WorldDef("volcano", "Volcanic Lava World", listOf("fire_monster", "scorpion", "drone", "bat"),
            listOf("lava_pool", "fire_pit", "steam_vent", "falling_boulder", "rolling_rock", "explosive_barrel", "mechanical_trap", "broken_bridge", "moving_platform", "rotating_blade"),
            listOf("shadow_dragon", "robot_destroyer", "scorpion_king"), 1.5f, "rumble", 0xe4683fff, bossHpBonus = 4),
        WorldDef("fantasy", "Magical Fantasy Kingdom", listOf("shadow_monster", "ice_creature", "fire_monster", "drone", "monkey"),
            listOf("crystal_cluster", "moving_saw", "rotating_blade", "laser_barrier", "electric_barrier", "moving_platform", "collapsing_platform", "spring_trap", "stalactite", "sharp_spikes", "wooden_crate"),
            listOf("jungle_guardian", "scorpion_king", "ice_titan", "robot_destroyer", "shadow_dragon"), 1.6f, "nature", 0xc79bf2ff, bossHpBonus = 6),
    )
    fun byId(id: String) = all.firstOrNull { it.id == id } ?: all[0]
    fun indexOf(id: String) = all.indexOfFirst { it.id == id }.coerceAtLeast(0)
}

enum class CollectibleType(val id: String, val anim: String, val label: String) {
    SILVER_COIN("silver_coin", "collectibles/silver_coin/spin", "Silver coin: 1 coin"),
    GOLD_COIN("gold_coin", "collectibles/gold_coin/spin", "Gold coin: 5 coins"),
    DIAMOND_COIN("diamond_coin", "collectibles/diamond_coin/spin", "Diamond coin: 25 coins"),
    GEM("gem", "collectibles/gem/reward", "Gem: premium currency for keys and boxes"),
    TREASURE_CHEST("treasure_chest", "collectibles/treasure_chest/reward", "Treasure chest: open it with a key in Rewards"),
    MYSTERY_BOX("mystery_box", "collectibles/mystery_box/reward", "Mystery box: random reward, opened at once"),
    STAR("star", "collectibles/star/reward", "Star: +500 score"),
    KEY("key", "collectibles/key/reward", "Key: opens a treasure chest"),
    REWARD_BAG("reward_bag", "collectibles/reward_bag/reward", "Reward bag: 20-40 coins"),
    BONUS_TOKEN("bonus_token", "collectibles/bonus_token/reward", "Bonus token: +300 score; 10 tokens buy a mystery box"),
}

enum class PowerUpType(val id: String, val title: String, val baseDuration: Float, val description: String) {
    SPEED_BOOST("speed_boost", "Speed Boost", 4.5f, "Dash at 1.45x speed and smash through hazards"),
    COIN_MAGNET("coin_magnet", "Coin Magnet", 9f, "Pulls nearby coins toward you"),
    PROTECTIVE_SHIELD("protective_shield", "Shield", 0f, "Absorbs the next hit"),
    DOUBLE_COINS("double_coins", "Double Coins", 10f, "Coins are worth double"),
    EXTRA_LIFE("extra_life", "Extra Life", 0f, "One revive after a fatal hit"),
    INVINCIBILITY("invincibility", "Invincibility", 6f, "No damage; defeat enemies on contact"),
    DOUBLE_JUMP("double_jump", "Super Jump", 12f, "Higher double jump plus one extra air jump"),
    JETPACK("jetpack", "Jetpack", 5f, "Hold jump to fly, release to glide"),
    HEALTH_RECOVERY("health_recovery", "Health", 0f, "Restores one heart"),
    SCORE_MULTIPLIER("score_multiplier", "Score x2", 10f, "Doubles score gains"),
    ;
    val timed get() = baseDuration > 0f
}
