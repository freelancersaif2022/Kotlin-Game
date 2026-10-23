package com.saif.pymaster.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saif.pymaster.data.DEFAULT_TERMINAL_CODE
import com.saif.pymaster.data.Learning
import com.saif.pymaster.data.Repository
import com.saif.pymaster.python.ConsoleBridge
import com.saif.pymaster.python.PythonRunner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class TerminalViewModel(app: Application) : AndroidViewModel(app) {

    enum class Kind { OUT, ERR, IN, SYS }

    data class Chunk(val text: String, val kind: Kind)

    private val repo = Repository.get(app)
    private val learning = Learning(app)
    private val main = Handler(Looper.getMainLooper())

    var code by mutableStateOf(TextFieldValue(DEFAULT_TERMINAL_CODE))
        private set
    val output = mutableStateListOf<Chunk>()
    var running by mutableStateOf(false)
        private set
    var waitingForInput by mutableStateOf(false)
        private set
    var inputText by mutableStateOf("")
    var outputVersion by mutableStateOf(0)
        private set

    private var bridge: ConsoleBridge? = null
    private var saveJob: Job? = null
    private val undo = ArrayDeque<TextFieldValue>()
    private val redo = ArrayDeque<TextFieldValue>()
    private var lastUndoPush = 0L
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val saved = repo.load().terminalCode
            code = TextFieldValue(saved, TextRange(saved.length))
        }
    }

    /** Called by the editor for every keystroke. Handles undo history, auto-indent and saving. */
    fun onCodeChange(new: TextFieldValue, autoIndent: Boolean) {
        val old = code
        var value = new
        if (autoIndent && new.text.length == old.text.length + 1 && new.selection.collapsed) {
            val pos = new.selection.start
            if (pos > 0 && new.text[pos - 1] == '\n') {
                val lineStart = new.text.lastIndexOf('\n', pos - 2).let { it + 1 }
                val prevLine = new.text.substring(lineStart, pos - 1)
                var indent = prevLine.takeWhile { it == ' ' }
                if (prevLine.trimEnd().endsWith(":")) indent += "    "
                if (indent.isNotEmpty()) {
                    val text = new.text.substring(0, pos) + indent + new.text.substring(pos)
                    value = TextFieldValue(text, TextRange(pos + indent.length))
                }
            }
        }
        if (value.text != old.text) {
            val now = SystemClock.uptimeMillis()
            if (now - lastUndoPush > 800 || value.text.length - old.text.length > 1 || value.text.endsWith("\n")) {
                undo.addLast(old)
                if (undo.size > 100) undo.removeFirst()
                lastUndoPush = now
            }
            redo.clear()
            scheduleSave(value.text)
        }
        code = value
        refreshHistoryFlags()
    }

    fun insert(snippet: String, cursorBack: Int = 0) {
        val v = code
        val start = minOf(v.selection.start, v.selection.end)
        val end = maxOf(v.selection.start, v.selection.end)
        val text = v.text.substring(0, start) + snippet + v.text.substring(end)
        undo.addLast(v)
        redo.clear()
        code = TextFieldValue(text, TextRange(start + snippet.length - cursorBack))
        scheduleSave(text)
        refreshHistoryFlags()
    }

    fun undo() {
        val prev = undo.removeLastOrNull() ?: return
        redo.addLast(code)
        code = prev
        scheduleSave(prev.text)
        refreshHistoryFlags()
    }

    fun redo() {
        val next = redo.removeLastOrNull() ?: return
        undo.addLast(code)
        code = next
        scheduleSave(next.text)
        refreshHistoryFlags()
    }

    fun clearCode() {
        if (code.text.isEmpty()) return
        undo.addLast(code)
        redo.clear()
        code = TextFieldValue("")
        scheduleSave("")
        refreshHistoryFlags()
    }

    fun loadCode(text: String, run: Boolean = false) {
        if (code.text != text) {
            undo.addLast(code)
            redo.clear()
        }
        code = TextFieldValue(text, TextRange(text.length))
        scheduleSave(text)
        refreshHistoryFlags()
        if (run) run()
    }

    fun clearOutput() {
        if (!running) output.clear()
        outputVersion++
    }

    private fun refreshHistoryFlags() {
        canUndo = undo.isNotEmpty()
        canRedo = redo.isNotEmpty()
    }

    private fun scheduleSave(text: String) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            repo.update { it.copy(terminalCode = text) }
        }
    }

    private fun append(text: String, kind: Kind) {
        if (text.isEmpty()) return
        val last = output.lastOrNull()
        if (last != null && last.kind == kind) {
            output[output.lastIndex] = last.copy(text = last.text + text)
        } else {
            output.add(Chunk(text, kind))
        }
        // keep the console light: ~60k characters
        var total = output.sumOf { it.text.length }
        while (total > 60_000 && output.size > 1) {
            total -= output.removeAt(0).text.length
        }
        if (total > 60_000) {
            val only = output[0]
            output[0] = only.copy(text = only.text.takeLast(60_000))
        }
        outputVersion++
    }

    fun run() {
        if (running) return
        val source = code.text
        output.clear()
        append("$ python main.py\n", Kind.SYS)
        running = true
        waitingForInput = false
        val started = SystemClock.elapsedRealtime()
        val b = ConsoleBridge(
            output = { text, isError -> main.post { append(text, if (isError) Kind.ERR else Kind.OUT) } },
            inputRequested = { main.post { waitingForInput = true } },
        )
        bridge = b
        viewModelScope.launch { learning.recordRun() }
        PythonRunner.run(getApplication(), source, b) { exit ->
            main.post {
                val secs = (SystemClock.elapsedRealtime() - started) / 1000.0
                append(
                    "\n[Process finished with exit code $exit in ${String.format(Locale.US, "%.2f", secs)}s]\n",
                    Kind.SYS
                )
                running = false
                waitingForInput = false
                bridge = null
            }
        }
    }

    fun submitInput() {
        val b = bridge ?: return
        if (!waitingForInput) return
        val line = inputText
        inputText = ""
        waitingForInput = false
        append(line + "\n", Kind.IN)
        b.submit(line)
    }

    fun stop() {
        if (!running) return
        waitingForInput = false
        PythonRunner.stop(bridge)
    }
}
