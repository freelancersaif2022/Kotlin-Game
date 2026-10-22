package com.freelancersaif.snakeattack

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils

class SnakeAttackGame : Game() {
    lateinit var batch: SpriteBatch

    /** The daily reward popup opens automatically once per app session. */
    var dailyShown = false
    private var frame = 0

    // Desktop-only dev hooks (system properties), used to capture screenshots during development.
    private val shots = System.getProperty("snake.shots")?.split(",")?.map {
        val (f, p) = it.split(":")
        f.toInt() to p
    }?.toMutableList()

    override fun create() {
        batch = SpriteBatch(4000)
        Assets.load()
        System.getProperty("snake.progress")?.toInt()?.let { p ->
            Save.level = p
            for (l in 1 until p) Save.setStars(l, 1 + l % 3)
        }
        val start = System.getProperty("snake.scene")
        val level = System.getProperty("snake.level")?.toInt() ?: Save.level
        setScreen(
            when (start) {
                "game" -> GameScreen(this, level)
                "rig" -> RigPreviewScreen(this)
                "map" -> MapScreen(this)
                "shop" -> ShopScreen(this, System.getProperty("snake.tab")?.toInt() ?: ShopScreen.TAB_BOOSTERS)
                "daily" -> DailyScreen(this)
                "spin" -> SpinScreen(this)
                "awards" -> AchievementsScreen(this)
                "story" -> StoryScreen(this, level, 1)
                else -> MenuScreen(this)
            },
        )
    }

    override fun setScreen(screen: com.badlogic.gdx.Screen?) {
        val old = getScreen()
        super.setScreen(screen)
        old?.dispose()
    }

    override fun pause() {
        Assets.pauseMusic()
    }

    override fun resume() {
        Assets.refreshMusic()
    }

    fun exit() = Gdx.app.exit()

    /** The screen that starts [level]: its story panel first when a new world begins. */
    fun levelScreen(level: Int): BaseScreen {
        val story = Story.before(level)
        return if (story > 0 && !Save.storySeen(story)) StoryScreen(this, story, level) else GameScreen(this, level)
    }

    fun debugFrame(screen: BaseScreen) {
        val list = shots ?: return
        frame++
        val next = list.firstOrNull() ?: return
        if (frame >= next.first) {
            list.removeAt(0)
            val pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
            PixmapIO.writePNG(Gdx.files.absolute(next.second), pm, java.util.zip.Deflater.DEFAULT_COMPRESSION, true)
            pm.dispose()
            if (list.isEmpty()) Gdx.app.exit()
        }
    }

    override fun dispose() {
        screen?.dispose()
        batch.dispose()
        Assets.dispose()
    }
}
