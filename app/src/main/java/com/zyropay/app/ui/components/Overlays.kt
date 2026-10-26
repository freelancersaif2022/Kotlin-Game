package com.zyropay.app.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.DrawableRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ dialog

@Composable
fun ZDialog(
    onDismiss: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = "Cancel",
    @DrawableRes illustration: Int? = null,
    @DrawableRes icon: Int? = null,
    iconTint: Color = Z.colors.primary,
    iconBg: Color = Z.colors.primarySoft,
    destructive: Boolean = false,
) {
    val c = Z.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val anim = remember { Animatable(0.88f) }
        LaunchedEffect(Unit) { anim.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 500f)) }
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .graphicsLayer { scaleX = anim.value; scaleY = anim.value; alpha = ((anim.value - 0.88f) / 0.12f).coerceIn(0f, 1f) }
                .background(c.surfaceElevated, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                illustration != null -> ZImage(illustration, Modifier.size(120.dp))
                icon != null -> IconCircle(icon, size = 60.dp, iconSize = 28.dp, background = iconBg, tint = iconTint)
            }
            if (illustration != null || icon != null) Spacer(Modifier.height(16.dp))
            Text(title, style = Z.type.h3, color = c.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(message, style = Z.type.bodySmall, color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            ZButton(confirmText, onClick = onConfirm, modifier = Modifier.fillMaxWidth(), style = if (destructive) ButtonStyle.Danger else ButtonStyle.Primary, size = ButtonSize.Medium)
            if (dismissText != null) {
                Spacer(Modifier.height(8.dp))
                ZButton(dismissText, onClick = onDismiss, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Ghost, size = ButtonSize.Medium)
            }
        }
    }
}

/** Full-screen blocking progress dialog ("Processing…"). */
@Composable
fun LoadingDialog(text: String = "Processing…") {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Column(
            Modifier.background(Z.colors.surfaceElevated, RoundedCornerShape(20.dp)).padding(horizontal = 32.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandSpinner(Modifier.size(44.dp))
            Spacer(Modifier.height(14.dp))
            Text(text, style = Z.type.bodySmallMedium, color = Z.colors.textPrimary)
        }
    }
}

// ------------------------------------------------------------------ bottom sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.(close: (after: () -> Unit) -> Unit) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { after ->
        scope.launch { state.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Z.colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { GrabHandle() },
        scrimColor = Color.Black.copy(alpha = if (Z.colors.isDark) 0.6f else 0.42f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            if (title != null) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = Z.type.h3, color = Z.colors.textPrimary, modifier = Modifier.weight(1f))
                    IconButtonZ(R.drawable.ic_close, onClick = { close {} }, tint = Z.colors.textSecondary, background = Z.colors.surfaceVariant, size = 32.dp, iconSize = 18.dp)
                }
            }
            content(close)
        }
    }
}

// ------------------------------------------------------------------ biometrics

fun Context.findActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object Biometrics {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun available(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(context: Context, title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        val activity = context.findActivity() ?: return onError("Unavailable")
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use passcode")
            .setAllowedAuthenticators(AUTH)
            .build()
        runCatching { prompt.authenticate(info) }.onFailure { onError("Unavailable") }
    }
}

// ------------------------------------------------------------------ passcode verification

@Composable
fun PasscodeSheet(
    title: String,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    subtitle: String = "Enter your 6-digit passcode to continue",
) {
    val context = LocalContext.current
    val security by AppRepository.security.collectAsState()
    val bioAvailable = security.biometricEnabled && remember { Biometrics.available(context) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shake by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    ZBottomSheet(onDismiss = onDismiss) { close ->
        val bio = {
            Biometrics.authenticate(context, title, "Confirm with fingerprint or face", onSuccess = { close(onVerified) })
        }
        LaunchedEffect(Unit) { if (bioAvailable) { delay(350); bio() } }
        LaunchedEffect(code) {
            if (code.length == 6) {
                delay(120)
                if (AppRepository.verifyPasscode(code)) {
                    close(onVerified)
                } else {
                    attempts++
                    error = if (attempts >= 3) "Incorrect passcode. ${5 - attempts.coerceAtMost(4)} attempts left before lock." else "Incorrect passcode, please try again"
                    shake++
                    delay(380)
                    code = ""
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconCircle(R.drawable.ic_passcode, size = 56.dp, iconSize = 26.dp, background = Z.colors.primarySoft, tint = Z.colors.primary)
            Spacer(Modifier.height(14.dp))
            Text(title, style = Z.type.h3, color = Z.colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(error ?: subtitle, style = Z.type.bodySmall, color = if (error != null) Z.colors.error else Z.colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            PasscodeDots(code.length, error = error != null && code.isEmpty().not(), shakeKey = shake)
            Spacer(Modifier.height(28.dp))
            NumericKeypad(
                onDigit = { d -> if (code.length < 6) { code += d; error = null } },
                onDelete = { code = code.dropLast(1) },
                extraIcon = if (bioAvailable) R.drawable.ic_fingerprint else null,
                onExtra = if (bioAvailable) bio else null,
                keyHeight = 58.dp,
            )
            Spacer(Modifier.height(4.dp))
            KeyHint("Forgot passcode? Log out and sign in again to reset it.")
        }
    }
}

@Stable
class AuthGate {
    internal var pending by mutableStateOf<Pair<String, () -> Unit>?>(null)
    fun request(title: String = "Verify identity", onSuccess: () -> Unit) {
        pending = title to onSuccess
    }
}

/** Returns a gate that shows a passcode sheet and runs the action once verified. */
@Composable
fun rememberAuthGate(): AuthGate {
    val gate = remember { AuthGate() }
    gate.pending?.let { (title, action) ->
        PasscodeSheet(title = title, onDismiss = { gate.pending = null }, onVerified = { gate.pending = null; action() })
    }
    return gate
}

// ------------------------------------------------------------------ misc

@Composable
fun BrandSpinner(modifier: Modifier = Modifier) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(tween(900, easing = androidx.compose.animation.core.LinearEasing)),
        label = "angle",
    )
    val primary = Z.colors.primary
    val track = Z.colors.primarySoft
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = size.minDimension * 0.11f
        val inset = stroke / 2
        drawArc(track, 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawArc(
            androidx.compose.ui.graphics.Brush.sweepGradient(listOf(primary.copy(alpha = 0f), primary, Color(0xFF00D1FF))),
            angle, 280f, false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

@Composable
fun SheetOption(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    selected: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val c = Z.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .background(if (selected) c.primarySoft else Color.Transparent, RoundedCornerShape(14.dp))
            .pressable(scaleDown = 0.985f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = Z.type.bodyMedium, color = c.textPrimary)
            if (subtitle != null) Text(subtitle, style = Z.type.caption, color = c.textSecondary)
        }
        if (selected) {
            Box(Modifier.size(22.dp).background(c.primary, CircleShape), contentAlignment = Alignment.Center) {
                ZIcon(R.drawable.ic_check, size = 14.dp, tint = Color.White)
            }
        }
    }
}
