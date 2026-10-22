package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color

/** Development-only screen (desktop, -Dsnake.scene=rig) that shows the character rigs large. */
class RigPreviewScreen(game: SnakeAttackGame) : BaseScreen(game) {
    private val girls = Girl.Mood.entries.filter { it != Girl.Mood.CAUGHT }.map { mood -> Girl().also { it.mood = mood; it.scale = 0.5f } }
    private val dragons = List(3) { DragonHead().also { it.scale = 0.75f; it.boss = it.hashCode() % 2 == 0 } }
    private val showDragon = System.getProperty("snake.rig") == "dragon"

    override fun update(dt: Float) {
        girls.forEach { it.update(dt) }
        dragons.forEachIndexed { i, d ->
            d.mouthOpen = i * 0.5f
            d.angry = i == 1
            d.hurt = i == 2 && false
            d.update(dt)
        }
    }

    override fun draw() {
        batch.fillRect(0f, 0f, W, H, Color.valueOf("6a8f5a"))
        if (showDragon) {
            dragons.forEachIndexed { i, d ->
                d.x = W * 0.62f
                d.y = H * (0.82f - i * 0.3f)
                d.drawNeck(batch, d.x + 60f, d.y - 20f, 60f)
                d.draw(batch)
            }
        } else {
            girls.forEachIndexed { i, g ->
                g.baseX = W * (0.27f + (i % 2) * 0.46f)
                g.baseY = H * (0.76f - (i / 2) * 0.31f)
                g.render(batch)
                batch.text(Assets.fontSmall, g.mood.name, g.baseX, g.baseY - 30f, 26f)
            }
        }
    }
}
