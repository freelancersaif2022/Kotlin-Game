package com.freelancersaif.snakeattack

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.JsonReader

/** Road (background) and level-map path traces, in source-image pixels (y down). */
object Paths {
    val road: List<Vector2>
    val map: List<Vector2>

    init {
        val json = JsonReader().parse(Gdx.files.internal("data/paths.json"))
        fun read(name: String) = json.get(name).map { Vector2(it.getFloat(0), it.getFloat(1)) }
        road = read("road")
        map = read("map_path")
    }
}
