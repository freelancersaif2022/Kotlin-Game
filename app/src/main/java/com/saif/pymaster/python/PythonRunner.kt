package com.saif.pymaster.python

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

/**
 * Bridge object handed to pm_runner.py. Python calls [onOutput] for stdout/stderr
 * and [readInput] (blocking) whenever the program calls input().
 */
class ConsoleBridge(
    private val output: (text: String, isError: Boolean) -> Unit,
    private val inputRequested: (prompt: String) -> Unit,
) {
    private val queue = LinkedBlockingQueue<String>()

    fun onOutput(text: String, isError: Boolean) = output(text, isError)

    fun readInput(prompt: String): String? {
        inputRequested(prompt)
        val line = queue.take()
        return if (line === STOP) null else line
    }

    fun submit(line: String) {
        queue.offer(line)
    }

    fun cancel() {
        queue.offer(STOP)
    }

    private companion object {
        val STOP = String(charArrayOf('\u0000'))
    }
}

object PythonRunner {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "python-runner").apply { isDaemon = true } }
    private val control = Executors.newSingleThreadExecutor()

    @Volatile
    private var module: PyObject? = null

    @Volatile
    var version: String = "3.11"
        private set

    private fun ensure(context: Context): PyObject {
        module?.let { return it }
        synchronized(this) {
            module?.let { return it }
            if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
            val m = Python.getInstance().getModule("pm_runner")
            version = m.callAttr("version").toString()
            module = m
            return m
        }
    }

    /** Starts the interpreter in the background so the first Run is fast. */
    fun warmUp(context: Context) {
        executor.execute { runCatching { ensure(context) } }
    }

    /** Runs [code] on the Python thread; [onFinish] receives the exit code. */
    fun run(context: Context, code: String, bridge: ConsoleBridge, onFinish: (Int) -> Unit) {
        executor.execute {
            val exit = try {
                val sandbox = java.io.File(context.filesDir, "sandbox").absolutePath
                ensure(context).callAttr("run", code, bridge, sandbox).toInt()
            } catch (t: Throwable) {
                bridge.onOutput("Internal error: ${t.message}\n", true)
                1
            }
            onFinish(exit)
        }
    }

    fun stop(bridge: ConsoleBridge?) {
        bridge?.cancel()
        control.execute { runCatching { module?.callAttr("request_stop") } }
    }
}
