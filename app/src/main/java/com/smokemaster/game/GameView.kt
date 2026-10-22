package com.smokemaster.game

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowInsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Hosts the game on its own thread. Every frame is driven by Choreographer
 * (vsync aligned) and drawn with a GPU-backed canvas, so the game runs at the
 * display's native refresh rate without blocking the UI thread.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Choreographer.FrameCallback {

    private val game = Game(context.applicationContext)
    private val thread = HandlerThread("GameLoop", Process.THREAD_PRIORITY_DISPLAY).apply { start() }
    private val handler = Handler(thread.looper)
    private var choreographer: Choreographer? = null

    // Game-thread state.
    private var running = false
    private var hasSurface = false
    private var resumed = true
    private var lastFrameNanos = 0L
    private var hardwareCanvas = true

    private val pending = ArrayList<Touch>()
    private val work = ArrayList<Touch>()

    @Volatile private var insetTop = 0
    @Volatile private var insetBottom = 0

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
        handler.post {
            choreographer = Choreographer.getInstance()
            game.start()
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = insets.displayCutout
            insetTop = cutout?.safeInsetTop ?: 0
            insetBottom = cutout?.safeInsetBottom ?: 0
        }
        return super.onApplyWindowInsets(insets)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val top = insetTop
        val bottom = insetBottom
        handler.post {
            game.resize(width, height, top, bottom)
            hasSurface = true
            updateLoop()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val latch = CountDownLatch(1)
        handler.post {
            hasSurface = false
            updateLoop()
            latch.countDown()
        }
        latch.await(1, TimeUnit.SECONDS)
    }

    fun onResume() {
        handler.post {
            resumed = true
            game.onResume()
            updateLoop()
        }
    }

    fun onPause() {
        handler.post {
            resumed = false
            game.onPause()
            updateLoop()
        }
    }

    fun onBack() {
        handler.post {
            if (!game.back()) post { (context as? Activity)?.finish() }
        }
    }

    fun release() {
        handler.post {
            running = false
            game.release()
            thread.quitSafely()
        }
    }

    private fun updateLoop() {
        val shouldRun = hasSurface && resumed
        if (shouldRun && !running) {
            running = true
            lastFrameNanos = 0L
            choreographer?.postFrameCallback(this)
        } else if (!shouldRun && running) {
            running = false
            choreographer?.removeFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        choreographer?.postFrameCallback(this)

        val dt = if (lastFrameNanos == 0L) 1f / 60f
        else ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNanos = frameTimeNanos

        synchronized(pending) {
            work.addAll(pending)
            pending.clear()
        }
        if (work.isNotEmpty()) {
            game.input(work)
            work.clear()
        }
        game.update(dt)

        val canvas = lockCanvas() ?: return
        try {
            game.draw(canvas)
        } finally {
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (_: IllegalStateException) {
            }
        }
    }

    private fun lockCanvas(): Canvas? {
        val h = holder
        if (hardwareCanvas) {
            try {
                return h.lockHardwareCanvas()
            } catch (_: IllegalStateException) {
                return null
            } catch (_: Exception) {
                hardwareCanvas = false
            }
        }
        return try {
            h.lockCanvas()
        } catch (_: Exception) {
            null
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val type = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> Touch.DOWN
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> Touch.UP
            MotionEvent.ACTION_CANCEL -> Touch.CANCEL
            else -> return true
        }
        val i = event.actionIndex
        val t = Touch(type, event.getPointerId(i), event.getX(i), event.getY(i))
        synchronized(pending) { pending.add(t) }
        if (type == Touch.UP) performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

class Touch(val type: Int, val id: Int, var x: Float, var y: Float) {
    companion object {
        const val DOWN = 0
        const val UP = 1
        const val CANCEL = 2
    }
}
