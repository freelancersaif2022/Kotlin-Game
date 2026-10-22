package com.smokemaster.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

abstract class Screen(val g: Game) {
    open fun enter() {}
    open fun exit() {}
    abstract fun update(dt: Float)
    abstract fun draw(c: Canvas)
    open fun touch(t: Touch) {}
    /** Returns true when the back press was consumed. */
    open fun back(): Boolean = false
    open fun onPause() {}
}

/**
 * Root of the game: owns shared services, the virtual viewport and screen
 * transitions. Everything runs on the game thread.
 *
 * Content is laid out on a virtual canvas that is 1080 units wide (height
 * follows the device aspect ratio, so any phone is filled edge to edge).
 */
class Game(val context: Context) {
    val settings = Settings(context)
    val assets = Assets(context.assets)
    val audio = Audio(context, settings)
    val haptics = Haptics(context, settings)
    val text = TextPainter()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fillPaint = Paint()

    var vw = 1080f
        private set
    var vh = 1920f
        private set
    private var scale = 1f
    var safeTop = 0f
        private set
    var safeBottom = 0f
        private set
    var time = 0f
        private set

    @Volatile var loadProgress = 0f
    @Volatile var loaded = false

    private var screen: Screen? = null
    private var next: Screen? = null
    private var fade = 1f
    private var started = false
    private val dst = RectF()

    fun start() {
        if (started) return
        started = true
        assets.loadCore()
        Thread({
            assets.loadAll { loadProgress = it * 0.9f }
            audio.loadAll()
            loadProgress = 1f
            loaded = true
        }, "AssetLoader").start()
        screen = SplashScreen(this).also { it.enter() }
    }

    fun resize(w: Int, h: Int, insetTop: Int, insetBottom: Int) {
        scale = w / 1080f
        vw = 1080f
        vh = h / scale
        if (vh < 1750f) {
            // Wide screens (tablets, foldables): fit height, widen the canvas.
            scale = h / 1750f
            vh = 1750f
            vw = w / scale
        }
        safeTop = insetTop / scale
        safeBottom = insetBottom / scale
    }

    fun go(s: Screen) {
        if (next != null) return
        next = s
    }

    fun input(events: List<Touch>) {
        if (next != null) return
        val s = screen ?: return
        for (e in events) {
            e.x /= scale
            e.y /= scale
            s.touch(e)
        }
    }

    fun update(dt: Float) {
        time += dt
        if (next != null) {
            fade += dt * 4.5f
            if (fade >= 1f) {
                fade = 1f
                screen?.exit()
                screen = next
                next = null
                screen?.enter()
            }
        } else if (fade > 0f) {
            fade = maxOf(0f, fade - dt * 3.5f)
        }
        audio.update(dt)
        screen?.update(dt)
    }

    fun draw(c: Canvas) {
        c.drawColor(Color.BLACK)
        c.save()
        c.scale(scale, scale)
        screen?.draw(c)
        if (fade > 0f) {
            fillPaint.color = Color.BLACK
            fillPaint.alpha = (Ease.inOutSine(fade) * 255).toInt()
            c.drawRect(0f, 0f, vw, vh, fillPaint)
        }
        c.restore()
    }

    fun back(): Boolean {
        if (next != null) return true
        return screen?.back() ?: false
    }

    fun onPause() {
        screen?.onPause()
        audio.pause()
    }

    fun onResume() {
        audio.resume()
    }

    fun release() {
        audio.release()
    }

    // ------------------------------------------------------------ drawing helpers

    /** Draws a bitmap scaled to cover the whole viewport (centre-cropped). */
    fun drawCover(c: Canvas, b: Bitmap, zoom: Float = 1f, alpha: Int = 255) {
        val s = maxOf(vw / b.width, vh / b.height) * zoom
        val w = b.width * s
        val h = b.height * s
        dst.set((vw - w) / 2, (vh - h) / 2, (vw + w) / 2, (vh + h) / 2)
        paint.alpha = alpha
        c.drawBitmap(b, null, dst, paint)
        paint.alpha = 255
    }

    /** Draws [b] centred at (cx, cy) with the given width (height keeps aspect). */
    fun drawCentered(c: Canvas, b: Bitmap, cx: Float, cy: Float, w: Float, alpha: Float = 1f) {
        val h = w * b.height / b.width
        dst.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        c.drawBitmap(b, null, dst, paint)
        paint.alpha = 255
    }

    fun dim(c: Canvas, alpha: Float, color: Int = Color.BLACK) {
        fillPaint.color = color
        fillPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        c.drawRect(0f, 0f, vw, vh, fillPaint)
    }

    fun click() = audio.play("click", 0.8f)
}
