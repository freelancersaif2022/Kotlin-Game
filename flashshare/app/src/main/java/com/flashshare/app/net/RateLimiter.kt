package com.flashshare.app.net

/** Token bucket shared by all streams of a transfer. Used for the Free plan speed cap. */
class RateLimiter(private val bytesPerSecond: Long) {
    private var tokens = 0.0
    private var last = System.nanoTime()
    private val burst = bytesPerSecond / 4.0

    @Synchronized
    fun acquire(bytes: Long) {
        val now = System.nanoTime()
        tokens = minOf(burst, tokens + (now - last) / 1e9 * bytesPerSecond)
        last = now
        tokens -= bytes
        if (tokens < 0) {
            val waitNs = (-tokens / bytesPerSecond * 1e9).toLong()
            Thread.sleep(waitNs / 1_000_000, (waitNs % 1_000_000).toInt())
        }
    }
}
