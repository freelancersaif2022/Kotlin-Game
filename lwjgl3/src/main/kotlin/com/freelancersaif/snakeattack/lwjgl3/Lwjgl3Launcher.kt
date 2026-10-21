package com.freelancersaif.snakeattack.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.freelancersaif.snakeattack.SnakeAttackGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Snake Attack")
        val size = System.getProperty("snake.window")?.split("x")?.map { it.toInt() }
        setWindowedMode(size?.get(0) ?: 450, size?.get(1) ?: 900)
        useVsync(true)
        setForegroundFPS(60)
        setWindowIcon("icons/icon_64.png")
    }
    Lwjgl3Application(SnakeAttackGame(), config)
}
