package com.tvremote.universal.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.tvremote.universal.ui.components.HeroIcon
import com.tvremote.universal.ui.components.NumberedStep
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.SecondaryButton
import com.tvremote.universal.ui.components.SectionTitle
import com.tvremote.universal.ui.components.TvGlyph
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
            pairing.start("TV Remote (${Build.MODEL})")
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
                    _step.value = PairStep.Failed("Incorrect code. Tap Try again to get a new code on your TV.")
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

    LaunchedEffect(step) {
        val s = step
        if (s is PairStep.Success) {
            delay(900)
            onPaired(s.deviceId)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
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
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    TvGlyph(tv.platform, size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tv.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlatformBadge(tv.platform)
                            Spacer(Modifier.width(8.dp))
                            Text(tv.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = step,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "pairStep",
            ) { s ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (s) {
                        PairStep.Intro -> IntroStep(onConnect = vm::start)
                        is PairStep.Working -> WorkingStep(s.message)
                        is PairStep.EnterCode -> CodeStep(s, onSubmit = vm::submitCode, onRestart = vm::start)
                        is PairStep.Approve -> ApproveStep(s.message)
                        is PairStep.Success -> SuccessStep()
                        is PairStep.Failed -> FailedStep(s.message, onRetry = vm::start, onHelp = onHelp)
                    }
                }
            }

            if (step !is PairStep.Success) {
                Spacer(Modifier.height(24.dp))
                SectionTitle("How to pair")
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
}

@Composable
private fun IntroStep(onConnect: () -> Unit) {
    HeroIcon(Icons.Rounded.Link, size = 150.dp)
    Spacer(Modifier.height(16.dp))
    Text("Turn on ADB debugging first", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(
        "Fire TV needs a one-time setting before it can be controlled from your phone. Follow the steps below, then tap Connect.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(18.dp))
    PrimaryButton("Connect", onClick = onConnect, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun WorkingStep(message: String) {
    Spacer(Modifier.height(12.dp))
    Radar(active = true, modifier = Modifier.size(170.dp))
    Spacer(Modifier.height(14.dp))
    Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
}

@Composable
private fun ApproveStep(message: String) {
    val transition = rememberInfiniteTransition(label = "approve")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    HeroIcon(Icons.Rounded.Tv, size = 170.dp, modifier = Modifier.scale(pulse))
    Spacer(Modifier.height(16.dp))
    Text("Look at your TV", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(6.dp))
    Text(
        message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
}

@Composable
private fun CodeStep(step: PairStep.EnterCode, onSubmit: (String) -> Unit, onRestart: () -> Unit) {
    var value by remember(step.error) { mutableStateOf("") }
    Text(
        if (step.hex) "Enter the code shown on your TV" else "Enter the PIN shown on your TV",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        if (step.hex) "${step.length} characters — numbers and letters A to F" else "${step.length} digits",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(18.dp))
    CodeInput(
        length = step.length,
        hex = step.hex,
        value = value,
        enabled = !step.submitting,
        onChange = { value = it },
        onComplete = { if (!step.submitting) onSubmit(it) },
    )
    step.error?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(18.dp))
    PrimaryButton(
        "Pair",
        onClick = { onSubmit(value) },
        enabled = value.length == step.length,
        loading = step.submitting,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = onRestart) { Text("No code on the TV? Start again") }
}

@Composable
private fun CodeInput(
    length: Int,
    hex: Boolean,
    value: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    val focus = remember { FocusRequester() }
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
                    val active = i == value.length
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .size(width = if (length > 4) 46.dp else 58.dp, height = 60.dp)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                if (active) 2.dp else 1.dp,
                                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(char, style = MonoDigits, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
    )
}

@Composable
private fun SuccessStep() {
    val extra = AppTheme.extra
    HeroIcon(Icons.Rounded.CheckCircle, size = 170.dp, colors = listOf(extra.success, extra.success.copy(alpha = 0.8f)))
    Spacer(Modifier.height(16.dp))
    Text("Paired!", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(4.dp))
    Text("Opening your remote…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FailedStep(message: String, onRetry: () -> Unit, onHelp: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    HeroIcon(Icons.Rounded.ErrorOutline, size = 150.dp, colors = listOf(scheme.error, scheme.error.copy(alpha = 0.8f)))
    Spacer(Modifier.height(16.dp))
    Text("Couldn't pair", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(6.dp))
    Text(message, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    PrimaryButton("Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(10.dp))
    SecondaryButton("Troubleshooting help", onClick = onHelp, modifier = Modifier.fillMaxWidth())
}
