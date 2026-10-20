package com.tvremote.universal.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvremote.universal.App
import com.tvremote.universal.core.ClientIdentity
import com.tvremote.universal.core.isCertificateChange
import com.tvremote.universal.data.DiscoveredTv
import com.tvremote.universal.data.Guides
import com.tvremote.universal.data.TvDevice
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.tv.AndroidTvPairing
import com.tvremote.universal.tv.ConnectionState
import com.tvremote.universal.tv.ControllerFactory
import com.tvremote.universal.tv.PairingException
import com.tvremote.universal.tv.TvController
import com.tvremote.universal.tv.VizioPairing
import com.tvremote.universal.ui.components.AppBar
import com.tvremote.universal.ui.components.AppCard
import com.tvremote.universal.ui.components.AppSnackbarHost
import com.tvremote.universal.ui.components.HeroIcon
import com.tvremote.universal.ui.components.NumberedStep
import com.tvremote.universal.ui.components.PairingAnimation
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.RadarScanner
import com.tvremote.universal.ui.components.SecondaryButton
import com.tvremote.universal.ui.components.SectionTitle
import com.tvremote.universal.ui.components.SnackKind
import com.tvremote.universal.ui.components.SuccessCheck
import com.tvremote.universal.ui.components.TvGlyph
import com.tvremote.universal.ui.components.show
import com.tvremote.universal.ui.theme.AppTheme
import com.tvremote.universal.ui.theme.MonoDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface PairStep {
    data object Intro : PairStep
    data class Working(val message: String) : PairStep
    data class EnterCode(val length: Int, val hex: Boolean, val error: String? = null, val submitting: Boolean = false) : PairStep
    data class Approve(val message: String) : PairStep
    data class Success(val deviceId: String) : PairStep
    data class Failed(val message: String) : PairStep
}

class PairingViewModel(private val tv: DiscoveredTv, private val existingId: String?) : ViewModel() {
    private val container = App.container
    private val _step = MutableStateFlow<PairStep>(
        if (tv.platform == TvPlatform.FIRE_TV) PairStep.Intro else PairStep.Working("Connecting to your TV…"),
    )
    val step: StateFlow<PairStep> = _step.asStateFlow()

    private var job: Job? = null
    private var code: CompletableDeferred<String>? = null
    private var androidTv: AndroidTvPairing? = null
    private var controller: TvController? = null

    init {
        if (tv.platform != TvPlatform.FIRE_TV) start()
    }

    fun start() {
        val previous = job
        job = viewModelScope.launch {
            previous?.cancelAndJoin()
            try {
                when (tv.platform) {
                    TvPlatform.ANDROID_TV -> pairAndroidTv()
                    TvPlatform.VIZIO -> pairVizio()
                    else -> pairWithController()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _step.value = PairStep.Failed(
                    if (e.isCertificateChange()) "The TV's security certificate changed. Tap Try again."
                    else "Couldn't connect to ${tv.name}. Make sure the TV is on and on the same Wi-Fi as this phone.",
                )
            } finally {
                cleanup()
            }
        }
    }

    fun submitCode(value: String) {
        code?.complete(value)
    }

    private suspend fun awaitCode(): String {
        val d = CompletableDeferred<String>()
        code = d
        return d.await()
    }

    private fun baseDevice() = TvDevice(
        name = tv.name.ifBlank { tv.platform.short },
        platform = tv.platform,
        host = tv.host,
        model = tv.model,
        mac = tv.mac,
    )

    private suspend fun pairAndroidTv() {
        _step.value = PairStep.Working("Connecting to your TV…")
        val identity = withContext(Dispatchers.Default) { ClientIdentity.get(container.app) }
        val pairing = AndroidTvPairing(tv.host, identity, container.certPins)
        androidTv = pairing
        try {
            pairing.start("Universal Remote (${Build.MODEL})")
        } catch (e: Exception) {
            _step.value = PairStep.Failed(
                "Couldn't start pairing with ${tv.name}. Make sure the TV is on and on the same Wi-Fi. " +
                    "If it keeps failing, update \"Android TV Remote Service\" on the TV.",
            )
            return
        }
        var error: String? = null
        while (true) {
            _step.value = PairStep.EnterCode(6, hex = true, error = error)
            val entered = awaitCode()
            _step.value = PairStep.EnterCode(6, hex = true, submitting = true)
            val ok = try {
                pairing.finish(entered)
            } catch (e: PairingException) {
                if (e.wrongCode) {
                    _step.value = PairStep.Failed("That code was incorrect. Tap Try again to get a new code on your TV.")
                    return
                }
                throw e
            }
            if (ok) break
            error = "That code doesn't match. Check the code on your TV and try again."
        }
        save(baseDevice())
    }

    private suspend fun pairVizio() {
        _step.value = PairStep.Working("Connecting to your TV…")
        val pairing = VizioPairing(container.http, tv.host, container.certPins)
        if (!pairing.start()) {
            _step.value = PairStep.Failed("Couldn't reach ${tv.name}. Make sure the TV is fully on and on the same Wi-Fi.")
            return
        }
        _step.value = PairStep.EnterCode(4, hex = false)
        val pin = awaitCode()
        _step.value = PairStep.EnterCode(4, hex = false, submitting = true)
        val token = pairing.finish(pin)
        if (token == null) {
            _step.value = PairStep.Failed("Incorrect PIN. Tap Try again to get a new PIN on your TV.")
            return
        }
        save(baseDevice().copy(credentials = mapOf("authToken" to token, "port" to pairing.port.toString())))
    }

    private suspend fun pairWithController() {
        _step.value = PairStep.Working("Connecting to your TV…")
        container.certPins.clearHost(tv.host)
        val ctrl = ControllerFactory.create(baseDevice(), container, approvalTimeoutMs = 45_000) {}
        controller = ctrl
        val watcher = viewModelScope.launch {
            ctrl.state.collect { s ->
                if (s is ConnectionState.Connecting && s.hint != null) _step.value = PairStep.Approve(s.hint)
            }
        }
        ctrl.connect()
        watcher.cancel()
        when (val s = ctrl.state.value) {
            is ConnectionState.Connected -> save(ctrl.device)
            is ConnectionState.Failed -> _step.value = PairStep.Failed(s.message)
            else -> _step.value = PairStep.Failed("The TV didn't respond. Please try again.")
        }
    }

    private suspend fun save(device: TvDevice) {
        val store = container.tvStore
        val old = existingId?.let { store.get(it) } ?: store.findByHost(tv.platform, tv.host)
        val now = System.currentTimeMillis()
        val saved = device.copy(
            id = old?.id ?: UUID.randomUUID().toString(),
            name = old?.name ?: device.name,
            mac = device.mac.ifBlank { old?.mac.orEmpty() },
            addedAt = old?.addedAt ?: now,
            lastUsed = now,
        )
        store.upsert(saved)
        _step.value = PairStep.Success(saved.id)
        delay(10)
    }

    private fun cleanup() {
        androidTv?.close()
        androidTv = null
        controller?.close()
        controller = null
        code?.cancel()
        code = null
    }

    override fun onCleared() {
        job?.cancel()
        cleanup()
    }
}

@Composable
fun PairScreen(
    tv: DiscoveredTv,
    existingId: String?,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onPaired: (String) -> Unit,
) {
    val vm: PairingViewModel = viewModel(key = "pair-${tv.platform}-${tv.host}") { PairingViewModel(tv, existingId) }
    val step by vm.step.collectAsStateWithLifecycle()
    val guide = remember(tv.platform) { Guides.of(tv.platform) }
    val snackbar = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(step) {
        val s = step
        if (s is PairStep.EnterCode && s.error != null) snackbar.show(s.error, SnackKind.ERROR)
        if (s is PairStep.Success) {
            snackbar.show("Paired with ${tv.name}", SnackKind.SUCCESS)
            delay(1600)
            onPaired(s.deviceId)
        }
    }

    val progress = when (step) {
        PairStep.Intro, is PairStep.Working -> 0
        is PairStep.EnterCode, is PairStep.Approve, is PairStep.Failed -> 1
        is PairStep.Success -> 2
    }

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AppBar(title = "Pair your TV", subtitle = tv.platform.title, onBack = onBack, actions = {
                IconButton(onClick = onHelp) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Pairing help") }
            })
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvGlyph(tv.platform, size = 56.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tv.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(5.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PlatformBadge(tv.platform)
                                    Spacer(Modifier.width(8.dp))
                                    Text(tv.host, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        StepTrack(progress, failed = step is PairStep.Failed)
                    }
                }
                Spacer(Modifier.height(24.dp))

                AnimatedContent(
                    targetState = step,
                    contentKey = { it::class },
                    transitionSpec = { (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(140)) },
                    label = "pairStep",
                ) { s ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when (s) {
                            PairStep.Intro -> IntroStep(tv.platform, onConnect = vm::start)
                            is PairStep.Working -> WorkingStep(s.message)
                            is PairStep.EnterCode -> CodeStep(tv.platform, s, onSubmit = vm::submitCode, onRestart = vm::start)
                            is PairStep.Approve -> ApproveStep(tv.platform, s.message)
                            is PairStep.Success -> SuccessStep(tv.name) { onPaired(s.deviceId) }
                            is PairStep.Failed -> FailedStep(s.message, onRetry = vm::start, onHelp = onHelp)
                        }
                    }
                }

                if (step !is PairStep.Success) {
                    Spacer(Modifier.height(26.dp))
                    SectionTitle("How to pair your ${tv.platform.short}")
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            guide.pairing.forEachIndexed { i, text -> NumberedStep(i + 1, text) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

@Composable
private fun StepTrack(current: Int, failed: Boolean) {
    val labels = listOf("Connect", "Confirm on TV", "Done")
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    Row(verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { i, label ->
            val reached = i <= current
            val color = when {
                failed && i == current -> scheme.error
                i < current || (i == 2 && current == 2) -> extra.success
                reached -> scheme.primary
                else -> scheme.outline
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(if (reached) color else Color.Transparent)
                        .border(2.dp, color, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (i < current || (i == 2 && current == 2)) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    else Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = if (reached) Color.White else scheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (reached) scheme.onSurface else scheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun IntroStep(platform: TvPlatform, onConnect: () -> Unit) {
    PairingAnimation(platform)
    Spacer(Modifier.height(18.dp))
    Text("Turn on ADB debugging first", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(
        "Fire TV needs a one-time setting before your phone can control it. Follow the steps below, then tap Connect.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(18.dp))
    PrimaryButton("Connect", icon = Icons.Rounded.Link, onClick = onConnect, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun WorkingStep(message: String) {
    RadarScanner(active = true, found = 0, modifier = Modifier.size(190.dp))
    Spacer(Modifier.height(16.dp))
    Text(message, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text("This usually takes a second or two.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ApproveStep(platform: TvPlatform, message: String) {
    PairingAnimation(platform)
    Spacer(Modifier.height(18.dp))
    Text("Look at your TV", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(6.dp))
    Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(14.dp))
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text("Waiting for your TV…", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CodeStep(platform: TvPlatform, step: PairStep.EnterCode, onSubmit: (String) -> Unit, onRestart: () -> Unit) {
    var value by remember(step.error) { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme
    HeroIcon(Icons.Rounded.SettingsRemote, size = 120.dp)
    Spacer(Modifier.height(12.dp))
    Text(
        if (step.hex) "Enter the code on your TV" else "Enter the PIN on your TV",
        style = MaterialTheme.typography.headlineSmall,
        color = scheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        if (step.hex) "${step.length} characters · numbers and letters A–F" else "${step.length} digits shown on the ${platform.short} screen",
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    CodeInput(
        length = step.length,
        hex = step.hex,
        value = value,
        enabled = !step.submitting,
        error = step.error != null && value.isEmpty(),
        onChange = { value = it },
        onComplete = { if (!step.submitting) onSubmit(it) },
    )
    Spacer(Modifier.height(20.dp))
    PrimaryButton(
        if (step.submitting) "Pairing…" else "Pair",
        onClick = { onSubmit(value) },
        enabled = value.length == step.length,
        loading = step.submitting,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = onRestart) {
        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("No code on the TV? Start again")
    }
}

@Composable
private fun CodeInput(
    length: Int,
    hex: Boolean,
    value: String,
    enabled: Boolean,
    error: Boolean,
    onChange: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = raw.uppercase().filter { c -> if (hex) c in "0123456789ABCDEF" else c.isDigit() }.take(length)
            onChange(filtered)
            if (filtered.length == length && filtered != value) onComplete(filtered)
        },
        enabled = enabled,
        modifier = Modifier.focusRequester(focus),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (hex) KeyboardType.Ascii else KeyboardType.NumberPassword,
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { if (value.length == length) onComplete(value) }),
        singleLine = true,
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { i ->
                    val char = value.getOrNull(i)?.toString().orEmpty()
                    val active = i == value.length && enabled
                    val filled = char.isNotEmpty()
                    val lift by animateFloatAsState(if (active) 1f else 0f, label = "lift")
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .size(width = if (length > 4) 46.dp else 60.dp, height = 62.dp)
                            .clip(shape)
                            .background(if (filled) scheme.primary.copy(alpha = 0.08f) else extra.panel)
                            .border(
                                if (active || error) 2.dp else 1.dp,
                                when {
                                    error -> scheme.error
                                    active -> scheme.primary
                                    filled -> scheme.primary.copy(alpha = 0.5f)
                                    else -> extra.panelBorder
                                },
                                shape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (filled) Text(char, style = MonoDigits, color = scheme.onSurface)
                        else if (active) Box(Modifier.size(width = 2.dp, height = (18 + 6 * lift).dp).background(scheme.primary))
                    }
                }
            }
        },
    )
}

@Composable
private fun SuccessStep(name: String, onOpen: () -> Unit) {
    SuccessCheck(size = 190.dp)
    Spacer(Modifier.height(10.dp))
    Text("You're connected!", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(6.dp))
    Text(
        "$name is paired and ready. Your remote opens automatically.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(22.dp))
    PrimaryButton(
        "Open remote",
        icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        onClick = onOpen,
        brush = Brush.linearGradient(listOf(Color(0xFF32D583), Color(0xFF079455))),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FailedStep(message: String, onRetry: () -> Unit, onHelp: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    HeroIcon(Icons.Rounded.ErrorOutline, size = 150.dp, colors = listOf(scheme.error, Color(0xFFD92D20)))
    Spacer(Modifier.height(14.dp))
    Text("Couldn't pair", style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground)
    Spacer(Modifier.height(6.dp))
    Text(message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Try again", icon = Icons.Rounded.Refresh, onClick = onRetry, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    SecondaryButton("Troubleshooting guide", icon = Icons.AutoMirrored.Rounded.HelpOutline, onClick = onHelp, modifier = Modifier.fillMaxWidth())
}
