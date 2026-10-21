package com.irremote.tv.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Sends IR on a dedicated background thread so the UI never blocks while the LED is busy.
 * Presses that arrive while a burst is still going out replace the queued one instead of piling up.
 */
class IrTransmitter private constructor(context: Context) {

    private val ir: ConsumerIrManager? = try {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    } catch (e: RuntimeException) {
        null
    }

    val hasEmitter: Boolean = try {
        ir?.hasIrEmitter() == true
    } catch (e: RuntimeException) {
        false
    }

    private val ranges: List<IntRange> = try {
        ir?.carrierFrequencies?.map { it.minFrequency..it.maxFrequency }.orEmpty()
    } catch (e: RuntimeException) {
        emptyList()
    }

    private val thread = HandlerThread("ir-tx").apply { start() }
    private val worker = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private val toggles = HashMap<String, Boolean>()
    private var pending: Runnable? = null

    /** Called on the main thread each time a burst starts (true) and finishes (false). */
    var onActivity: ((Boolean) -> Unit)? = null

    /**
     * Queues [signal] (a code string from the database). [repeat] marks auto-repeat while a key
     * is held, which keeps the RC5/RC6 toggle bit unchanged like a real remote does.
     * Returns false if the code could not be encoded.
     */
    fun send(signal: String, repeat: Boolean = false): Boolean {
        val code = IrCode.parse(signal) ?: return false
        var toggle = false
        if (code is IrCode.Parsed && code.protocol.startsWith("RC")) {
            val prev = toggles[code.protocol] ?: false
            toggle = if (repeat) prev else !prev
            toggles[code.protocol] = toggle
        }
        val ir = IrEncoder.encode(code, toggle) ?: return false
        val job = object : Runnable {
            override fun run() {
                synchronized(this@IrTransmitter) { if (pending === this) pending = null }
                transmit(ir)
            }
        }
        synchronized(this) {
            pending?.let { worker.removeCallbacks(it) }
            pending = job
        }
        worker.post(job)
        return true
    }

    private fun transmit(signal: IrSignal) {
        main.post { onActivity?.invoke(true) }
        val start = SystemClock.uptimeMillis()
        try {
            if (hasEmitter) ir?.transmit(supportedFrequency(signal.frequency), limit(signal.pattern))
        } catch (e: RuntimeException) {
            Log.w(TAG, "IR transmit failed", e)
        }
        // keep the LED indicator visible for at least a moment, even on instant HALs
        val elapsed = SystemClock.uptimeMillis() - start
        main.postDelayed({ onActivity?.invoke(false) }, maxOf(0L, 90L - elapsed))
    }

    /** Snaps the carrier to the nearest frequency the hardware reports as supported. */
    private fun supportedFrequency(f: Int): Int {
        if (ranges.isEmpty() || ranges.any { f in it }) return f
        return ranges.map { if (f < it.first) it.first else it.last }.minByOrNull { kotlin.math.abs(it - f) } ?: f
    }

    /** ConsumerIrManager rejects patterns longer than two seconds. */
    private fun limit(p: IntArray): IntArray {
        var total = 0L
        for (i in p.indices step 2) {
            total += p[i] + (if (i + 1 < p.size) p[i + 1] else 0)
            if (total > 1_900_000 && i > 0) return p.copyOf(i) // cut after a whole mark/space pair
        }
        return p
    }

    companion object {
        private const val TAG = "IrTransmitter"

        @Volatile private var instance: IrTransmitter? = null

        fun get(context: Context): IrTransmitter =
            instance ?: synchronized(this) {
                instance ?: IrTransmitter(context.applicationContext).also { instance = it }
            }
    }
}
