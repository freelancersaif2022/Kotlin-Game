package com.bramble.dash.config

/**
 * Every gameplay and economy number lives here so balancing does not require code changes elsewhere.
 * Units: world pixels in the 1280x720 reference space, seconds, and in-game metres (PX_PER_METRE px).
 */
object Tuning {
    // ---- simulation
    const val STEP = 1f / 120f
    const val MAX_STEPS_PER_FRAME = 8          // bounded catch-up (~66 ms)
    const val MAX_FRAME_DELTA = 0.1f

    // ---- world layout
    const val GROUND_Y = 108f                   // top surface of the ground tiles
    const val PLAYER_SCREEN_X = 330f            // where the runner sits relative to the camera's left edge
    const val PX_PER_METRE = 40f
    const val SPAWN_AHEAD = 1500f               // patterns are generated this far beyond the right screen edge
    const val DESPAWN_BEHIND = 500f

    // ---- actor scales (source canvases are 192x256 / 192x192 / 320x320)
    const val PLAYER_SCALE = 0.72f
    const val ENEMY_SCALE = 0.62f
    const val BOSS_SCALE = 1.15f
    const val OBSTACLE_SCALE = 0.55f
    const val ITEM_SCALE = 0.42f
    const val POWERUP_SCALE = 0.5f

    // ---- player movement
    const val GRAVITY = 3000f
    const val JUMP_VELOCITY = 1130f
    const val DOUBLE_JUMP_VELOCITY = 980f
    const val JUMP_CUT_FACTOR = 0.55f           // releasing jump early trims the rise
    const val MAX_FALL_SPEED = 1900f
    const val COYOTE_TIME = 0.1f
    const val JUMP_BUFFER = 0.13f
    const val SLIDE_DURATION = 0.75f
    const val LAND_ANIM_TIME = 0.16f
    const val INVULNERABLE_TIME = 1.2f
    const val COLLISION_FORGIVENESS = 0.78f     // supplied AABBs are scaled by this around their centre

    // ---- speed curve
    const val START_SPEED = 430f
    const val SPEED_PER_METRE = 0.16f
    const val MAX_SPEED = 820f
    const val BOSS_SPEED_FACTOR = 0.62f

    // ---- health
    const val BASE_HEALTH = 3
    const val MAX_HEALTH_CAP = 6

    // ---- bosses
    const val FIRST_BOSS_METRES = 750f
    const val BOSS_INTERVAL_METRES = 1250f
    const val BOSS_WARNING_TIME = 2.6f
    const val BOSS_BASE_HP = 16
    const val BOSS_HP_PER_ENCOUNTER = 4
    const val ENERGY_COOLDOWN = 0.75f
    const val ENERGY_SPEED = 1100f

    // ---- scoring
    const val SCORE_PER_METRE = 1f
    const val SCORE_ENEMY = 150
    const val SCORE_BOSS = 2500
    const val SCORE_STAR = 500
    const val SCORE_TOKEN = 300
    const val SCORE_BREAK = 120

    // ---- power-up base durations (seconds); 0 = not timed
    const val PU_DURATION_UPGRADE_STEP = 0.12f  // +12% per upgrade level
    const val MAGNET_BASE_RANGE = 230f
    const val MAGNET_RANGE_STEP = 45f
    const val JETPACK_THRUST = 5200f
    const val JETPACK_CEILING = 600f
    const val POST_EFFECT_GRACE = 1.0f

    // ---- economy
    const val REVIVE_INVULNERABLE = 2.0f
    const val BOSS_REWARD_COINS = 250
    const val BOSS_REWARD_GEMS = 3
    const val TOKENS_PER_BOX = 10
    const val GEMS_PER_KEY = 3
    const val GEMS_PER_BOX = 5
    const val COINS_PER_GEM_EXCHANGE = 120
}
