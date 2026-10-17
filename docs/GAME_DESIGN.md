# Bramble Dash: game design reference

All values live in `core/src/main/kotlin/com/bramble/dash/config/Tuning.kt` and `Catalog.kt`,
plus `progression/*.kt` for the economy, so balancing never touches gameplay code.

## Run flow and states

`Loading → Lobby → (GameScreen) Loading run assets → READY → RUNNING ⇄ PAUSED`
`RUNNING → BOSS_WARNING → BOSS → BOSS_DEFEATED → VICTORY (continue or home) → RUNNING`
`any → DYING → REVIVE_OFFER (only with an Extra Life) → RESULTS`

* The simulation uses a fixed 1/120 s step with at most 8 catch-up steps per frame. Rendering
  interpolates between steps.
* A paused run never advances. After resume, the first frame is clamped to 0.1 s.
* The run report is built exactly once (`GameWorld.finish()`), and the screen applies it to the
  save once (`resultsCommitted`). Ending from pause or victory also records the run.

## Movement (reference space 1280×720 px; 40 px = 1 m)

| Value | Setting |
|---|---|
| Gravity | 3000 px/s² |
| Jump / double-jump velocity | 1130 / 980 px/s (about 213 px apex, 0.75 s airtime) |
| Coyote time / jump buffer | 0.10 s / 0.13 s |
| Early release | trims rising velocity to 55% |
| Slide | 0.75 s (Ninja Kai 1.09 s); the collider drops below the high-hazard lane |
| Damage invulnerability | 1.2 s (Rex 1.62 s), with blinking and a red flash |
| Speed | 430 px/s + 0.16 px/s per metre, max 820 px/s; worlds add up to +18%; bosses run at 62% |
| Health | 3 hearts (+1 per Max Health upgrade, +1 for Rex, cap 6) |

## Fair generation

`PatternGenerator` places reusable patterns: single ground hazard, overhead hazard, solid block,
breakable, knock-over cone, quicksand, spring, roller, falling boulder, ground and flying enemies,
shooters, platforms (with a hazard underneath from tier 2), collapsing and moving platforms,
ground+overhead combos, and coin runs. Rules:

* Gaps come from the physics: after a jump, airtime + 0.15 s; after a slide, 0.3–0.35 s. That is
  multiplied by a comfort factor (2.2 → 1.7 → 1.35 → 1.15 by distance tier, adjusted by world
  difficulty), and never below 1.0.
* Moving threats (rollers, chargers, walkers, lungers) spawn offset so they meet the runner at
  the planned spot. They only start moving once on screen, with a "!" warning at the screen edge.
  Falling boulders show a shadow and land about 0.65 s before the runner arrives.
* Nothing spawns inside the visible screen. Jumpable hazards are validated against the time the
  jump arc spends above their height.
* Shooters (robot, drone, ice creature) fire a head-height shot, which you slide under, before
  their body arrives; the spacing reserves a slide beat for it.
* Runs are seeded (seed logged on start), so a failed pattern can be reproduced.

## Runners

| Runner | Price | Ability |
|---|---|---|
| Max | free | Balanced |
| Luna | 1,500 coins | +6% jump, 28% lower fall gravity |
| Rex | 3,000 | +1 max heart, 35% longer invulnerability |
| Ninja Kai | 5,000 | 45% longer slide |
| Robo X | 8,000 | coin magnet lasts 50% longer |

## Worlds (unlock: defeat the previous world's first boss)

| World | Enemies | Bosses (cycle) | Ambience | Difficulty |
|---|---|---|---|---|
| Tropical Jungle | monkey, snake, bat, spider | Jungle Guardian, Scorpion King | nature | 1.00 |
| Sunny Green Forest | monkey, bat, spider, snake | Jungle Guardian (+2 HP), Ice Titan | nature | 1.08 |
| Hot Desert | scorpion, snake, robot, bat | Scorpion King, Robot Destroyer | wind | 1.16 |
| Snowy Mountain | ice creature, bat, spider, drone | Ice Titan, Jungle Guardian | wind | 1.24 |
| Dark Haunted Forest | shadow monster, bat, spider, snake | Shadow Dragon, Ice Titan | eerie | 1.32 |
| Futuristic City | robot, drone, spider, shadow monster | Robot Destroyer, Shadow Dragon | hum | 1.40 |
| Volcanic Lava World | fire monster, scorpion, drone, bat | Shadow Dragon (+4 HP), Robot Destroyer, Scorpion King | rumble | 1.50 |
| Magical Fantasy Kingdom | shadow monster, ice creature, fire monster, drone, monkey | all five (+6 HP) | nature | 1.60 |

Each world's 10–11 obstacle types are listed in `Worlds` (`Catalog.kt`). Together the worlds cover all 30.

## Obstacles

| Behaviour | Types |
|---|---|
| Solid (stand on top; side hit hurts) | wooden crate, fallen tree, haunted stump |
| Damaging trigger (jump) | sharp spikes, fire pit, lava pool, ice spikes, poison plant, thorn bush, cactus, crystal cluster |
| Moving | rolling rock, snowball (roll toward you); moving saw (oscillates) |
| Overhead (slide) | swinging axe, rotating blade, laser barrier, stalactite (flipped), hanging chain |
| Timed (off → flicker warning → on) | mechanical trap, electric barrier, steam vent |
| Breakable (stomp or shoot) | explosive barrel |
| Knock-over (harmless, short slow-down) | traffic cone |
| Collapsing platform | collapsing platform (0.6 s), broken bridge (0.35 s) |
| Moving platform | moving platform |
| Falling (shadow warning) | falling boulder |
| Slowdown | quicksand (60% speed, weaker jumps) |
| Bounce | spring trap |

## Enemies

| Enemy | Behaviour | Dodge |
|---|---|---|
| Angry Jungle Monkey | hops toward you | jump / stomp |
| Flying Bat | sine flight at head height | slide / stomp |
| Poisonous Snake | lunges when close | jump / stomp |
| Evil Robot | telegraphs, then fires a head-height missile | slide the shot, jump the robot |
| Giant Spider | dangles on a thread (drawn) | slide |
| Fire Monster | flaring fire aura, cannot be stomped | jump |
| Desert Scorpion | charges when on screen ("!") | jump / stomp |
| Flying Drone | hovers, telegraphs an electric zap | slide |
| Ice Creature | throws an ice shard | slide the shard, jump the body |
| Shadow Monster | fades in and out, harmless while faded | slide / stomp when solid |

Stomping gives +150 score. Invincibility, Speed Boost and Jetpack defeat enemies on contact.
Defeated enemies play their damage, then death sequence.

## Bosses

The boss sits at the right edge while you keep running. Each volley is telegraphed by its attack
animation, a red spark and a "!" at the lane height. LOW shots must be jumped and HIGH shots slid
under. Shot spacing is built from the same gap rules as the level generator.

* **Phase 1:** primary lane → special lane → special + primary.
* **Phase 2** (≤ 50% HP): a 1.1 s break with a warning, then faster shots, 3–4-shot combinations
  and shorter rests.
* **HP:** 16 + 4 per repeat encounter + world bonus. Energy blast cooldown is 0.75 s; blasts also
  destroy incoming shots.
* **Victory:** 250 coins (+100 per repeat encounter), 3 gems and 2,500 score. Also confetti, the
  boss-defeat VFX and the runner's victory pose. The first boss in a world unlocks the next world.

## Collectibles

| Item | Value / use |
|---|---|
| Silver / gold / diamond coin | 1 / 5 / 25 coins (doubled by Double Coins) |
| Gem | premium currency: keys, mystery boxes, coin exchange |
| Treasure chest | stored; open in Daily Rewards with a key: 150–400 coins + 1–3 gems |
| Mystery box | opened immediately, using the table below |
| Star | +500 score |
| Key | opens one treasure chest |
| Reward bag | 20–40 coins |
| Bonus token | +300 score; 10 tokens buy a mystery box |

**Mystery box odds:** 45% 100–300 coins · 20% 2–5 gems · 15% 1 key · 10% 3 tokens · 7% 800 coins · 3% 15 gems.
The shop sells a box for 5 gems or 10 tokens, a key for 3 gems, and 120 coins for 1 gem.

## Power-ups (a pickup appears every 18–30 s)

| Power-up | Duration | Effect / stacking |
|---|---|---|
| Speed Boost | 4.5 s | 1.45× speed, smash hazards; 1 s grace after |
| Coin Magnet | 9 s (Robo X 13.5 s) | pulls coins within 230 px (+45 px per upgrade) |
| Shield | until hit | absorbs one hit; a second pickup gives +200 score |
| Double Coins | 10 s | coin values ×2 |
| Extra Life | until used | one revive offer on death (hazards nearby are cleared, 2 s invulnerable) |
| Invincibility | 6 s | no damage, defeats enemies; 1 s grace after |
| Super Jump | 12 s | double jump 25% stronger plus one extra air jump |
| Jetpack | 5 s | hold jump to climb (ceiling 600 px), release to glide; hazard-immune; 1 s grace on expiry |
| Health | instant | +1 heart (or +100 score at full health) |
| Score ×2 | 10 s | score gains ×2 |

Timed effects refresh to full duration instead of stacking. The HUD shows each active effect
with a remaining-time bar, which turns red under 25%. Power-up Duration upgrades add 12% per level.

## Upgrades (coins)

| Upgrade | Levels and prices | Effect |
|---|---|---|
| Power-up Duration | 300 / 700 / 1,300 / 2,200 / 3,600 | +12% per level |
| Magnet Range | 250 / 600 / 1,100 / 1,900 / 3,000 | +45 px per level |
| Starting Shield | 800 / 1,800 / 3,500 | 35% / 70% / 100% chance to start with a shield |
| Max Health | 2,500 / 6,000 | +1 heart per level (cap 6 including Rex) |
| Score Bonus | 400 / 900 / 1,600 / 2,600 / 4,000 | +5% score per level |

## Daily rewards (local 7-day cycle)

Day 1: 100 coins · Day 2: 150 coins · Day 3: 2 gems · Day 4: 300 coins · Day 5: 1 key · Day 6: 4 gems · Day 7: 600 coins + 3 tokens.
You can claim once per local calendar day. Missing a day restarts at Day 1. The cycle trusts the
device clock; a date earlier than the last claim blocks claiming.

## Missions and achievements

* 33 missions, 3 active at a time, in a fixed progression order. Types: distance or coins or score
  in one run; totals for coins, slides, enemies, power-ups, gems and stars; defeat each boss;
  visit each world; open chests. Each is claimed once in Missions.
* 19 achievements: distance, totals, each boss, all worlds, all runners, a maxed upgrade, chests
  and revive. They unlock permanently, and the reward is claimed once.

## Save data

There is one JSON document in LibGDX Preferences (`com.bramble.dash.save`). It holds: version,
currencies, unlocks, selections, upgrades, bests per world, cleared worlds, bosses beaten, stats,
missions, achievements, daily state, settings, tutorial flag and the local leaderboard (top 10).

* Every transaction is written in one commit, and the previous good copy is kept as a backup.
* On load, values are clamped and unknown ids dropped. A damaged primary save falls back to the
  backup, then to defaults.
