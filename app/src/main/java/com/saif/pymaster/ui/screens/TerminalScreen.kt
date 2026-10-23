package com.saif.pymaster.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import com.saif.pymaster.util.personalize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.automirrored.rounded.KeyboardTab
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.data.AppState
import com.saif.pymaster.python.PythonRunner
import com.saif.pymaster.ui.TerminalViewModel
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.Fonts
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.util.LocalHaptics
import com.saif.pymaster.util.PythonVisualTransformation
import com.saif.pymaster.util.SyntaxColors
import kotlinx.coroutines.delay

private val samples = listOf(
    "Hello World" to "print(\"Hello, World!\")\nprint(\"Welcome to PyMaster 🐍\")\n",
    "Variables & f-strings" to "name = \"Saif\"\nage = 25\nheight = 1.78\n\nprint(f\"{name} is {age} years old\")\nprint(f\"Height: {height:.1f} m\")\nprint(type(name), type(age), type(height))\n",
    "Input & conditions" to "age = int(input(\"Enter your age: \"))\n\nif age >= 18:\n    print(\"You are an adult ✅\")\nelif age >= 13:\n    print(\"You are a teenager\")\nelse:\n    print(\"You are a child\")\n",
    "Loops" to "for i in range(1, 6):\n    print(f\"{i} x 7 = {i * 7}\")\n\ncount = 3\nwhile count > 0:\n    print(\"Countdown:\", count)\n    count -= 1\nprint(\"Lift off! 🚀\")\n",
    "Functions" to "def greet(name, greeting=\"Hello\"):\n    return f\"{greeting}, {name}!\"\n\nprint(greet(\"Ali\"))\nprint(greet(\"Sara\", \"Welcome\"))\n\nsquare = lambda x: x * x\nprint([square(n) for n in range(6)])\n",
    "Lists & dictionaries" to "fruits = [\"apple\", \"banana\", \"mango\"]\nfruits.append(\"orange\")\nprint(fruits, len(fruits))\n\nprices = {\"apple\": 1.2, \"banana\": 0.5, \"mango\": 2.0}\nfor fruit, price in prices.items():\n    print(f\"{fruit:<8} \${price:.2f}\")\n",
    "Classes (OOP)" to "class Dog:\n    def __init__(self, name, breed):\n        self.name = name\n        self.breed = breed\n\n    def bark(self):\n        return f\"{self.name} says Woof!\"\n\ndog = Dog(\"Rex\", \"Labrador\")\nprint(dog.bark())\nprint(dog.breed)\n",
    "Error handling" to "try:\n    number = int(input(\"Enter a number: \"))\n    print(\"100 /\", number, \"=\", 100 / number)\nexcept ValueError:\n    print(\"That is not a valid number!\")\nexcept ZeroDivisionError:\n    print(\"You cannot divide by zero!\")\nfinally:\n    print(\"Done.\")\n",
    "FizzBuzz" to "for n in range(1, 21):\n    if n % 15 == 0:\n        print(\"FizzBuzz\")\n    elif n % 3 == 0:\n        print(\"Fizz\")\n    elif n % 5 == 0:\n        print(\"Buzz\")\n    else:\n        print(n)\n",
)

private data class Key(val label: String, val insert: String, val back: Int = 0)

private val keys = listOf(
    Key("TAB", "    "), Key("( )", "()", 1), Key("[ ]", "[]", 1), Key("{ }", "{}", 1), Key(":", ":"),
    Key("\" \"", "\"\"", 1), Key("' '", "''", 1), Key("=", "="), Key("+", "+"), Key("-", "-"), Key("*", "*"),
    Key("/", "/"), Key("%", "%"), Key("<", "<"), Key(">", ">"), Key("#", "# "), Key("_", "_"), Key(",", ", "),
    Key(".", "."), Key("print()", "print()", 1), Key("input()", "input()", 1), Key("range()", "range()", 1),
    Key("len()", "len()", 1), Key("def", "def "), Key("for", "for "), Key("if", "if "), Key("elif", "elif "),
    Key("else:", "else:"), Key("return", "return "), Key("f\"\"", "f\"\"", 1),
)

@Composable
fun TerminalScreen(state: AppState?, vm: TerminalViewModel) {
    val settings = state?.settings
    val fontSize = (settings?.terminalFontSize ?: 14).sp
    val colors = LocalAppColors.current
    val haptics = LocalHaptics.current
    val clipboard = LocalClipboardManager.current
    var showSamples by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val monoStyle = TextStyle(fontFamily = Fonts.Mono, fontSize = fontSize, lineHeight = fontSize * 1.5f, color = SyntaxColors.Plain)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Terminal", style = MaterialTheme.typography.titleLarge, color = Brand.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (vm.running) Brand.PythonYellow else Brand.Success))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Python ${PythonRunner.version}  •  main.py",
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                    )
                }
            }
            Box {
                IconButton(onClick = { showSamples = true }) {
                    Icon(Icons.AutoMirrored.Rounded.LibraryBooks, "Examples", tint = Brand.White)
                }
                DropdownMenu(
                    expanded = showSamples,
                    onDismissRequest = { showSamples = false },
                    modifier = Modifier.background(colors.surfaceHigh),
                ) {
                    Text(
                        "Load an example",
                        style = MaterialTheme.typography.labelMedium,
                        color = Brand.TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    samples.forEach { (title, code) ->
                        DropdownMenuItem(
                            text = { Text(title, color = Brand.White, style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Rounded.Description, null, tint = Brand.Cyan) },
                            onClick = {
                                showSamples = false
                                vm.loadCode(personalize(code, state?.profile?.name))
                            },
                        )
                    }
                }
            }
        }

        // Toolbar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val runColor = if (vm.running) Brand.Danger else Brand.Success
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(runColor.copy(alpha = 0.18f))
                    .border(1.dp, runColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .clickable {
                        haptics?.click()
                        if (vm.running) vm.stop() else vm.run()
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (vm.running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = runColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (vm.running) "Stop" else "Run", style = MaterialTheme.typography.labelLarge, color = Brand.White)
            }
            Spacer(Modifier.weight(1f))
            ToolIcon(Icons.AutoMirrored.Rounded.Undo, "Undo", vm.canUndo) { vm.undo() }
            ToolIcon(Icons.AutoMirrored.Rounded.Redo, "Redo", vm.canRedo) { vm.redo() }
            ToolIcon(Icons.Rounded.ContentCopy, "Copy code", true) { clipboard.setText(AnnotatedString(vm.code.text)) }
            ToolIcon(Icons.Rounded.DeleteSweep, "Clear code", vm.code.text.isNotEmpty()) { confirmClear = true }
            ToolIcon(
                if (vm.consoleVisible) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                if (vm.consoleVisible) "Hide console" else "Show console",
                true,
            ) { vm.consoleVisible = !vm.consoleVisible }
        }

        // Editor, symbol keys and the slide-up console
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
        val consoleHeight by animateDpAsState(
            if (vm.consoleVisible) maxHeight * 0.45f else 0.dp,
            tween(280),
            label = "console",
        )
        Column(Modifier.fillMaxSize()) {
        // Editor
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.terminal)
                .border(1.dp, Color(0x2600D1FF), RoundedCornerShape(16.dp))
        ) {
            val viewportHeight = maxHeight
            val viewportWidth = maxWidth
            val lineCount = vm.code.text.count { it == '\n' } + 1
            val gutterWidth = with(LocalDensity.current) { (monoStyle.fontSize.toPx() * 0.62f * lineCount.toString().length.coerceAtLeast(2)).toDp() } + 20.dp
            Row(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    (1..lineCount).joinToString("\n"),
                    style = monoStyle.copy(color = Color(0x59FFFFFF), textAlign = TextAlign.End),
                    modifier = Modifier
                        .width(gutterWidth)
                        .heightIn(min = viewportHeight)
                        .background(Color(0x08FFFFFF))
                        .padding(top = 12.dp, end = 8.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    BasicTextField(
                        value = vm.code,
                        onValueChange = { vm.onCodeChange(it, settings?.autoIndent ?: true) },
                        textStyle = monoStyle,
                        cursorBrush = SolidColor(Brand.Cyan),
                        visualTransformation = remember { PythonVisualTransformation() },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                        modifier = Modifier
                            .widthIn(min = viewportWidth - gutterWidth)
                            .heightIn(min = viewportHeight)
                            .padding(start = 10.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
                    )
                }
            }
        }

        // Symbol keys
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            keys.forEach { key ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.stroke, RoundedCornerShape(10.dp))
                        .clickable {
                            haptics?.tick()
                            vm.insert(key.insert, key.back)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (key.label == "TAB") {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardTab, "Tab", tint = Brand.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text(key.label, style = TextStyle(fontFamily = Fonts.Mono, fontSize = 13.sp, color = Brand.White))
                    }
                }
            }
        }

        // Console (hidden until Run; swipe down or tap the arrow to hide)
        if (consoleHeight > 1.dp) {
            Console(vm, monoStyle, Modifier.height(consoleHeight))
        }
        }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = colors.surfaceHigh,
            title = { Text("Clear the editor?", color = Brand.White) },
            text = { Text("All code in main.py will be removed. You can undo this.", color = Brand.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearCode() }) { Text("Clear", color = Brand.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Brand.White) }
            },
        )
    }
}

@Composable
private fun ToolIcon(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val haptics = LocalHaptics.current
    IconButton(onClick = { haptics?.tick(); onClick() }, enabled = enabled, modifier = Modifier.size(42.dp)) {
        Icon(icon, label, tint = if (enabled) Brand.White else Brand.TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Console(vm: TerminalViewModel, monoStyle: TextStyle, modifier: Modifier) {
    val colors = LocalAppColors.current
    val scroll = rememberScrollState()
    val inputFocus = remember { FocusRequester() }

    LaunchedEffect(vm.outputVersion) {
        delay(30)
        scroll.animateScrollTo(scroll.maxValue)
    }
    LaunchedEffect(vm.waitingForInput) {
        if (vm.waitingForInput) {
            delay(80)
            runCatching { inputFocus.requestFocus() }
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.terminal)
            .border(1.dp, colors.stroke, RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0x0DFFFFFF))
                .pointerInput(Unit) {
                    var dragged = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { if (dragged > 48.dp.toPx()) vm.consoleVisible = false },
                    ) { change, dy ->
                        change.consume()
                        dragged += dy
                    }
                }
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "CONSOLE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = Brand.TextSecondary,
            )
            Spacer(Modifier.width(10.dp))
            val (status, statusColor) = when {
                vm.waitingForInput -> "waiting for input" to Brand.PythonYellow
                vm.running -> "running…" to Brand.Cyan
                vm.output.isEmpty() -> "ready" to Brand.TextMuted
                else -> "finished" to Brand.Success
            }
            Text(status, style = MaterialTheme.typography.labelSmall, color = statusColor, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.clearOutput() }, enabled = !vm.running, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.ClearAll, "Clear output", tint = Brand.TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { vm.consoleVisible = false }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.KeyboardArrowDown, "Hide console", tint = Brand.TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (vm.output.isEmpty()) {
                Text(
                    "Tap  ▶ Run  to execute your code.\nOutput, errors and input prompts appear here.",
                    style = monoStyle.copy(color = Brand.TextMuted, fontSize = monoStyle.fontSize * 0.9f),
                )
            } else {
                val text = buildAnnotatedString {
                    vm.output.forEach { chunk ->
                        val color = when (chunk.kind) {
                            TerminalViewModel.Kind.OUT -> Color(0xFFE6EDF3)
                            TerminalViewModel.Kind.ERR -> Color(0xFFFF7B86)
                            TerminalViewModel.Kind.IN -> Brand.Cyan
                            TerminalViewModel.Kind.SYS -> Color(0xFF7EE787)
                        }
                        withStyle(SpanStyle(color = color)) { append(chunk.text) }
                    }
                }
                SelectionContainer {
                    Text(text, style = monoStyle)
                }
            }
        }
        AnimatedVisibility(vm.waitingForInput) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0x14FFD43B))
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("›", style = monoStyle.copy(color = Brand.PythonYellow, fontSize = monoStyle.fontSize * 1.2f))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    if (vm.inputText.isEmpty()) {
                        Text("Type your input and press enter", style = monoStyle.copy(color = Brand.TextMuted))
                    }
                    BasicTextField(
                        value = vm.inputText,
                        onValueChange = { vm.inputText = it.replace("\n", "") },
                        singleLine = true,
                        textStyle = monoStyle.copy(color = Brand.Cyan),
                        cursorBrush = SolidColor(Brand.PythonYellow),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, autoCorrectEnabled = false),
                        keyboardActions = KeyboardActions(onSend = { vm.submitInput() }, onDone = { vm.submitInput() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(inputFocus),
                    )
                }
                IconButton(onClick = { vm.submitInput() }) {
                    Icon(Icons.AutoMirrored.Rounded.Send, "Send", tint = Brand.PythonYellow)
                }
            }
        }
    }
}
