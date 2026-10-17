package com.bramble.dash.assets

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion

/**
 * A playable sequence built from the exact ordered frame names in animations.json (never from
 * TextureAtlas.findRegions, whose index sorting is ambiguous for this pack).
 */
class Anim(val meta: AnimMeta, val frames: Array<AtlasRegion>, val frameMeta: Array<RegionMeta>, speed: Float = 1f) {
    val frameDuration = meta.frameDuration / speed
    val loop = meta.loop
    val duration = frameDuration * frames.size
    private val animation = Animation(frameDuration, *frames).apply {
        playMode = if (loop) Animation.PlayMode.LOOP else Animation.PlayMode.NORMAL
    }

    fun index(time: Float): Int = animation.getKeyFrameIndex(time)
    fun frame(time: Float): AtlasRegion = frames[index(time)]
    fun meta(time: Float): RegionMeta = frameMeta[index(time)]
    fun finished(time: Float) = !loop && time >= duration
}
