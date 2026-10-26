package com.zyropay.app.ui.screens.auth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.back
import com.zyropay.app.navigation.navigateClear
import com.zyropay.app.ui.components.Avatar
import com.zyropay.app.ui.components.Biometrics
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.KeyHint
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.NumericKeypad
import com.zyropay.app.ui.components.OtpInput
import com.zyropay.app.ui.components.PasscodeDots
import com.zyropay.app.ui.components.PasswordField
import com.zyropay.app.ui.components.PasswordStrength
import com.zyropay.app.ui.components.SegmentedTabs
import com.zyropay.app.ui.components.ToastType
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZDialog
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.ZScreen
import com.zyropay.app.ui.components.ZTextField
import com.zyropay.app.ui.components.isStrongPassword
import com.zyropay.app.ui.components.isValidEmail
import com.zyropay.app.ui.components.pressable
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Holds in-progress auth data between the form, OTP and passcode screens. */
object AuthFlow {
    var target = ""
    var password = ""
    var referral = ""
    var fresh = true
}

@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(8.dp))
    Text(title, style = Z.type.h1, color = Z.colors.textPrimary)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = Z.type.body, color = Z.colors.textSecondary)
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun CountryCodeChip(code: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(code, style = Z.type.body, color = Z.colors.textPrimary)
        ZIcon(R.drawable.ic_chevron_down, size = 16.dp, tint = Z.colors.textTertiary)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.width(1.dp).height(20.dp).background(Z.colors.outline))
    }
}

private val countryCodes = listOf("+852", "+65", "+971", "+1", "+44", "+880", "+91", "+86", "+81", "+60", "+66", "+63", "+62", "+84")

@Composable
fun LoginScreen(nav: NavController) {
    val messenger = LocalMessenger.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var email by rememberSaveable { mutableStateOf(AppRepository.user.value.email) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("+852") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }

    fun submit() {
        focus.clearFocus()
        val target = if (tab == 0) email.trim() else "$code ${phone.trim()}"
        emailError = when {
            tab == 0 && !isValidEmail(email) -> "Please enter a valid email address"
            tab == 1 && phone.filter { it.isDigit() }.length < 7 -> "Please enter a valid phone number"
            else -> null
        }
        passwordError = if (password.length < 8) "Password must be at least 8 characters" else null
        if (emailError != null || passwordError != null) return
        loading = true
        scope.launch {
            delay(1100)
            loading = false
            if (!AppRepository.checkPassword(target, password)) {
                attempts++
                passwordError = "Incorrect password"
                messenger.error("Login failed", "Email or password is incorrect. ${5 - attempts} attempts remaining.")
                return@launch
            }
            AuthFlow.target = target
            AuthFlow.password = password
            AuthFlow.fresh = false
            nav.navigate(Routes.otp(target, "login"))
        }
    }

    ZScreen(onBack = { nav.back() }, bottomBar = {
        ZButton("Log in", onClick = ::submit, modifier = Modifier.fillMaxWidth(), loading = loading, enabled = (if (tab == 0) email.isNotBlank() else phone.isNotBlank()) && password.isNotEmpty())
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Don't have an account? ", style = Z.type.bodySmall, color = Z.colors.textSecondary)
            Text("Sign up", style = Z.type.bodySmallMedium, color = Z.colors.primary, modifier = Modifier.clickable { nav.navigate(Routes.SIGNUP) { popUpTo(Routes.WELCOME) } })
        }
    }) {
        AuthHeader("Welcome back", "Log in to manage your crypto and card.")
        SegmentedTabs(listOf("Email", "Phone"), tab) { tab = it; emailError = null }
        Spacer(Modifier.height(20.dp))
        AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "loginTab") { t ->
            if (t == 0) {
                ZTextField(email, { email = it; emailError = null }, label = "Email", placeholder = "name@example.com", error = emailError, leadingIcon = R.drawable.ic_email, keyboardType = KeyboardType.Email)
            } else {
                var pickCode by remember { mutableStateOf(false) }
                ZTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == ' ' }; emailError = null }, label = "Phone number", placeholder = "Phone number", error = emailError, keyboardType = KeyboardType.Phone, leading = { CountryCodeChip(code) { pickCode = true } })
                if (pickCode) CountryCodeSheet(code, onDismiss = { pickCode = false }) { code = it }
            }
        }
        Spacer(Modifier.height(16.dp))
        PasswordField(password, { password = it; passwordError = null }, error = passwordError, onImeAction = ::submit)
        Spacer(Modifier.height(12.dp))
        Text(
            "Forgot password?", style = Z.type.bodySmallMedium, color = Z.colors.primary,
            modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(6.dp)).clickable { nav.navigate(Routes.FORGOT) }.padding(4.dp),
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZIcon(R.drawable.ic_shield_check, size = 16.dp, tint = Z.colors.success)
            Spacer(Modifier.width(6.dp))
            Text("Your connection is encrypted end-to-end", style = Z.type.caption, color = Z.colors.textTertiary)
        }
    }
}

@Composable
private fun CountryCodeSheet(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    com.zyropay.app.ui.components.ZBottomSheet(onDismiss = onDismiss, title = "Country / region code") { close ->
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().height(420.dp)) {
            items(countryCodes.size) { i ->
                val cc = countryCodes[i]
                val name = when (cc) {
                    "+852" -> "Hong Kong"; "+65" -> "Singapore"; "+971" -> "United Arab Emirates"; "+1" -> "United States / Canada"
                    "+44" -> "United Kingdom"; "+880" -> "Bangladesh"; "+91" -> "India"; "+86" -> "China"; "+81" -> "Japan"
                    "+60" -> "Malaysia"; "+66" -> "Thailand"; "+63" -> "Philippines"; "+62" -> "Indonesia"; else -> "Vietnam"
                }
                com.zyropay.app.ui.components.SheetOption(name, subtitle = cc, selected = cc == current) { close { onPick(cc) } }
            }
        }
    }
}

@Composable
fun SignUpScreen(nav: NavController) {
    val messenger = LocalMessenger.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("+852") }
    var password by rememberSaveable { mutableStateOf("") }
    var referral by rememberSaveable { mutableStateOf("") }
    var showReferral by rememberSaveable { mutableStateOf(false) }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var idError by remember { mutableStateOf<String?>(null) }
    var pickCode by remember { mutableStateOf(false) }

    fun submit() {
        focus.clearFocus()
        idError = when {
            tab == 0 && !isValidEmail(email) -> "Please enter a valid email address"
            tab == 1 && phone.filter { it.isDigit() }.length < 7 -> "Please enter a valid phone number"
            else -> null
        }
        if (idError != null) return
        if (!isStrongPassword(password)) {
            messenger.warning("Weak password", "Please meet all password requirements.")
            return
        }
        if (!agreed) {
            messenger.warning("Agreement required", "Please accept the Terms of Service and Privacy Policy.")
            return
        }
        loading = true
        scope.launch {
            delay(1200)
            loading = false
            AuthFlow.target = if (tab == 0) email.trim() else "$code ${phone.trim()}"
            AuthFlow.password = password
            AuthFlow.referral = referral
            AuthFlow.fresh = true
            nav.navigate(Routes.otp(AuthFlow.target, "signup"))
        }
    }

    ZScreen(onBack = { nav.back() }, bottomBar = {
        ZButton("Create account", onClick = ::submit, modifier = Modifier.fillMaxWidth(), loading = loading, enabled = (if (tab == 0) email.isNotBlank() else phone.isNotBlank()) && password.isNotEmpty())
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", style = Z.type.bodySmall, color = Z.colors.textSecondary)
            Text("Log in", style = Z.type.bodySmallMedium, color = Z.colors.primary, modifier = Modifier.clickable { nav.navigate(Routes.LOGIN) { popUpTo(Routes.WELCOME) } })
        }
    }) {
        AuthHeader("Create your account", "It only takes a minute. Get 50 USDT welcome bonus after sign-up.")
        SegmentedTabs(listOf("Email", "Phone"), tab) { tab = it; idError = null }
        Spacer(Modifier.height(20.dp))
        if (tab == 0) {
            ZTextField(email, { email = it; idError = null }, label = "Email", placeholder = "name@example.com", error = idError, leadingIcon = R.drawable.ic_email, keyboardType = KeyboardType.Email)
        } else {
            ZTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == ' ' }; idError = null }, label = "Phone number", placeholder = "Phone number", error = idError, keyboardType = KeyboardType.Phone, leading = { CountryCodeChip(code) { pickCode = true } })
        }
        Spacer(Modifier.height(16.dp))
        PasswordField(password, { password = it }, placeholder = "Create a strong password", imeAction = ImeAction.Next)
        if (password.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PasswordStrength(password)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable { showReferral = !showReferral }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Referral code (optional)", style = Z.type.bodySmallMedium, color = Z.colors.textSecondary)
            ZIcon(if (showReferral) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down, size = 16.dp, tint = Z.colors.textSecondary)
        }
        if (showReferral) {
            Spacer(Modifier.height(8.dp))
            ZTextField(referral, { referral = it.uppercase().take(10) }, placeholder = "Enter referral code", leadingIcon = R.drawable.ic_gift)
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable(interactionSource = null, indication = null) { agreed = !agreed }) {
            Box(
                Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (agreed) Z.colors.primary else Color.Transparent)
                    .border(1.5.dp, if (agreed) Z.colors.primary else Z.colors.textTertiary, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { if (agreed) ZIcon(R.drawable.ic_check, size = 14.dp, tint = Color.White) }
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    append("I have read and agree to the ")
                    withStyle(SpanStyle(color = Z.colors.primary, fontWeight = FontWeight.Medium)) { append("Terms of Service") }
                    append(" and ")
                    withStyle(SpanStyle(color = Z.colors.primary, fontWeight = FontWeight.Medium)) { append("Privacy Policy") }
                },
                style = Z.type.caption, color = Z.colors.textSecondary,
            )
        }
    }
    if (pickCode) CountryCodeSheet(code, onDismiss = { pickCode = false }) { code = it }
}

@Composable
fun OtpScreen(nav: NavController, target: String, purpose: String) {
    val messenger = LocalMessenger.current
    var otp by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(60) }
    var resendKey by remember { mutableIntStateOf(0) }
    var verifying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val viaEmail = "@" in target

    LaunchedEffect(resendKey) {
        val code = (100000 + Random.nextInt(900000)).toString()
        sent = code
        seconds = 60
        delay(1400)
        messenger.show(
            ToastType.INFO,
            if (viaEmail) "Mail · Zyro Pay" else "Messages · Zyro Pay",
            "[Zyro Pay] Your verification code is $code. Valid for 10 minutes. Never share it with anyone.",
            actionLabel = "Autofill",
            onAction = { otp = code },
        )
        while (seconds > 0) {
            delay(1000)
            seconds--
        }
    }

    LaunchedEffect(otp) {
        if (otp.length == 6) {
            verifying = true
            delay(900)
            if (otp == sent) {
                verifying = false
                when (purpose) {
                    "reset" -> nav.navigate(Routes.reset(target)) { popUpTo(Routes.FORGOT) { inclusive = true } }
                    else -> {
                        AppRepository.createSession(AuthFlow.target.ifBlank { target }, AuthFlow.password, AuthFlow.fresh, AuthFlow.referral)
                        messenger.success(if (purpose == "signup") "Account created" else "Logged in successfully", if (purpose == "signup") "Welcome to Zyro Pay! 50 USDT bonus added." else "Welcome back.")
                        nav.navigateClear(Routes.passcode("setup"))
                    }
                }
            } else {
                verifying = false
                error = true
                messenger.error("Invalid code", "The verification code is incorrect or expired.")
                delay(600)
                otp = ""
                error = false
            }
        }
    }

    ZScreen(onBack = { nav.back() }) {
        AuthHeader(
            if (viaEmail) "Check your email" else "Verify your phone",
            "Enter the 6-digit code we sent to ${maskTarget(target)}",
        )
        OtpInput(otp, { otp = it; error = false }, error = error)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (seconds > 0) {
                Text("Resend code in ", style = Z.type.bodySmall, color = Z.colors.textSecondary)
                Text("00:%02d".format(seconds), style = Z.type.bodySmallMedium, color = Z.colors.textPrimary)
            } else {
                Text("Didn't receive the code? ", style = Z.type.bodySmall, color = Z.colors.textSecondary)
                Text("Resend", style = Z.type.bodySmallMedium, color = Z.colors.primary, modifier = Modifier.clickable { otp = ""; resendKey++ })
            }
        }
        if (verifying) {
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.zyropay.app.ui.components.BrandSpinner(Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Verifying…", style = Z.type.bodySmallMedium, color = Z.colors.textSecondary)
            }
        }
        Spacer(Modifier.height(32.dp))
        com.zyropay.app.ui.components.InfoBanner(
            if (viaEmail) "Can't find it? Check your spam or promotions folder. Codes expire after 10 minutes." else "Make sure your phone has signal. Codes expire after 10 minutes.",
        )
    }
}

private fun maskTarget(t: String): String = if ("@" in t) {
    val (name, domain) = t.split("@", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    name.take(2) + "•••" + "@" + domain
} else t.dropLast(4).replace(Regex("\\d(?=\\d{2})"), "•") + t.takeLast(4)

@Composable
fun ForgotPasswordScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    ZScreen(onBack = { nav.back() }, bottomBar = {
        ZButton("Send code", onClick = {
            if (!isValidEmail(email) && email.filter { it.isDigit() }.length < 7) {
                error = "Enter a valid email or phone number"
            } else {
                loading = true
                scope.launch {
                    delay(1000); loading = false
                    nav.navigate(Routes.otp(email.trim(), "reset"))
                }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = email.isNotBlank(), loading = loading)
    }) {
        AuthHeader("Reset password", "Enter the email or phone number linked to your account and we'll send you a verification code.")
        ZTextField(email, { email = it; error = null }, label = "Email / Phone", placeholder = "name@example.com", error = error, leadingIcon = R.drawable.ic_email, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(16.dp))
        com.zyropay.app.ui.components.WarningBanner("For your security, withdrawals will be disabled for 24 hours after a password reset.")
    }
}

@Composable
fun ResetPasswordScreen(nav: NavController, target: String) {
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val mismatch = confirm.isNotEmpty() && confirm != password
    ZScreen(onBack = { nav.back() }, bottomBar = {
        ZButton("Reset password", onClick = {
            loading = true
            scope.launch {
                delay(1100)
                loading = false
                if (AppRepository.isRegistered(target)) AppRepository.changePassword(password)
                messenger.success("Password updated", "Log in with your new password.")
                nav.navigate(Routes.LOGIN) { popUpTo(Routes.WELCOME) }
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = isStrongPassword(password) && confirm == password, loading = loading)
    }) {
        AuthHeader("Create new password", "Your new password must be different from previously used passwords.")
        PasswordField(password, { password = it }, label = "New password", placeholder = "New password", imeAction = ImeAction.Next)
        Spacer(Modifier.height(12.dp))
        PasswordStrength(password)
        Spacer(Modifier.height(16.dp))
        PasswordField(confirm, { confirm = it }, label = "Confirm password", placeholder = "Re-enter new password", error = if (mismatch) "Passwords do not match" else null)
    }
}

/** mode = "setup" (first time), "change" (verify old first) */
@Composable
fun CreatePasscodeScreen(nav: NavController, mode: String) {
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(if (mode == "change") 0 else 1) } // 0 verify old, 1 new, 2 confirm
    var first by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shake by remember { mutableIntStateOf(0) }

    LaunchedEffect(code) {
        if (code.length < 6) return@LaunchedEffect
        delay(150)
        when (step) {
            0 -> if (AppRepository.verifyPasscode(code)) { step = 1; code = "" } else { error = "Incorrect passcode"; shake++; delay(400); code = "" }
            1 -> {
                if (code.toSet().size == 1 || "0123456789".contains(code) || "9876543210".contains(code)) {
                    error = "Avoid simple passcodes like 111111 or 123456"; shake++; delay(400); code = ""
                } else { first = code; step = 2; code = "" }
            }
            2 -> if (code == first) {
                AppRepository.setPasscode(code)
                if (mode == "change") {
                    messenger.success("Passcode changed", "Use your new passcode next time.")
                    nav.back()
                } else {
                    messenger.success("Passcode set", "Your account is now protected.")
                    if (Biometrics.available(context)) nav.navigateClear(Routes.BIOMETRIC_SETUP)
                    else nav.navigateClear(Routes.NOTIFICATION_PERMISSION)
                }
            } else {
                error = "Passcodes don't match. Try again."; shake++; delay(400); code = ""; step = 1; first = ""
            }
        }
    }

    val title = when (step) { 0 -> "Enter current passcode"; 1 -> if (mode == "change") "Set new passcode" else "Create passcode"; else -> "Confirm passcode" }
    val subtitle = when (step) {
        0 -> "Verify your identity to continue"
        1 -> "This 6-digit passcode protects your app, payments and withdrawals"
        else -> "Re-enter your 6-digit passcode"
    }
    Column(Modifier.fillMaxSize().background(Z.colors.background).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            if (mode == "change") com.zyropay.app.ui.components.IconButtonZ(R.drawable.ic_arrow_left, onClick = { nav.back() }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            com.zyropay.app.ui.components.IconCircle(R.drawable.ic_passcode, size = 64.dp, iconSize = 30.dp, background = Z.colors.primarySoft, tint = Z.colors.primary)
            Spacer(Modifier.height(20.dp))
            AnimatedContent(title, transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut()) }, label = "pcTitle") {
                Text(it, style = Z.type.h2, color = Z.colors.textPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(error ?: subtitle, style = Z.type.bodySmall, color = if (error != null) Z.colors.error else Z.colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(36.dp))
            PasscodeDots(code.length, error = error != null && code.length == 6, shakeKey = shake)
        }
        NumericKeypad(
            onDigit = { if (code.length < 6) { code += it; error = null } },
            onDelete = { code = code.dropLast(1) },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
fun BiometricSetupScreen(nav: NavController) {
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    Column(Modifier.fillMaxSize().background(Z.colors.background).statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.6f))
        ZImage(R.drawable.ill_biometric, Modifier.size(240.dp))
        Spacer(Modifier.height(16.dp))
        Text("Enable biometric unlock", style = Z.type.h1, color = Z.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Use your fingerprint or face to unlock Zyro Pay and confirm payments faster and more securely.", style = Z.type.body, color = Z.colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        ZButton("Enable biometrics", onClick = {
            Biometrics.authenticate(context, "Enable biometric unlock", "Confirm your fingerprint or face", onSuccess = {
                AppRepository.setBiometric(true)
                messenger.success("Biometrics enabled", "You can now unlock with your fingerprint or face.")
                nav.navigateClear(Routes.NOTIFICATION_PERMISSION)
            }, onError = { messenger.warning("Not enabled", it) })
        }, modifier = Modifier.fillMaxWidth(), icon = R.drawable.ic_fingerprint)
        Spacer(Modifier.height(10.dp))
        ZButton("Maybe later", onClick = { nav.navigateClear(Routes.NOTIFICATION_PERMISSION) }, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Ghost)
    }
}

@Composable
fun NotificationPermissionScreen(nav: NavController) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { nav.navigateClear(Routes.MAIN) }
    Column(Modifier.fillMaxSize().background(Z.colors.background).statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.6f))
        ZImage(R.drawable.ill_notification_permission, Modifier.size(240.dp))
        Spacer(Modifier.height(16.dp))
        Text("Stay in the loop", style = Z.type.h1, color = Z.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Get instant alerts for card payments, deposits, withdrawals and security events.", style = Z.type.body, color = Z.colors.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        listOf(
            Triple(R.drawable.ic_card, "Real-time spending alerts", "Know the moment your card is used"),
            Triple(R.drawable.ic_shield_check, "Security notifications", "Logins, password changes and more"),
            Triple(R.drawable.ic_gift, "Rewards & offers", "Never miss cashback promotions"),
        ).forEach { (icon, title, sub) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                com.zyropay.app.ui.components.IconCircle(icon, size = 40.dp, iconSize = 20.dp, background = Z.colors.primarySoft, tint = Z.colors.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = Z.type.bodyMedium, color = Z.colors.textPrimary)
                    Text(sub, style = Z.type.caption, color = Z.colors.textSecondary)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ZButton("Turn on notifications", onClick = {
            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else nav.navigateClear(Routes.MAIN)
        }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        ZButton("Not now", onClick = { nav.navigateClear(Routes.MAIN) }, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Ghost)
    }
}

@Composable
fun LockScreen(nav: NavController, resume: Boolean) {
    val context = LocalContext.current
    val user by AppRepository.user.collectAsState()
    val security by AppRepository.security.collectAsState()
    val bioAvailable = security.biometricEnabled && remember { Biometrics.available(context) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shake by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var showLogout by remember { mutableStateOf(false) }

    val unlock = {
        AppRepository.unlock()
        if (resume) nav.back() else nav.navigateClear(Routes.MAIN)
    }
    val bio = { Biometrics.authenticate(context, "Unlock Zyro Pay", "Use fingerprint or face to unlock", onSuccess = unlock) }
    androidx.activity.compose.BackHandler(enabled = true) { (context as? android.app.Activity)?.moveTaskToBack(true) }
    LaunchedEffect(Unit) { if (bioAvailable) { delay(400); bio() } }
    LaunchedEffect(code) {
        if (code.length == 6) {
            delay(120)
            if (AppRepository.verifyPasscode(code)) unlock()
            else {
                attempts++
                error = "Incorrect passcode (${attempts}/5)"
                shake++
                delay(400)
                code = ""
                if (attempts >= 5) {
                    AppRepository.logout()
                    nav.navigateClear(Routes.LOGIN)
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Z.colors.background).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(48.dp))
            Avatar(user.avatar, size = 72.dp)
            Spacer(Modifier.height(16.dp))
            Text("Welcome back, ${user.nickname.ifBlank { "there" }}", style = Z.type.h2, color = Z.colors.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(error ?: "Enter your passcode to unlock", style = Z.type.bodySmall, color = if (error != null) Z.colors.error else Z.colors.textSecondary)
            Spacer(Modifier.height(36.dp))
            PasscodeDots(code.length, error = error != null && code.length == 6, shakeKey = shake)
        }
        NumericKeypad(
            onDigit = { if (code.length < 6) { code += it; error = null } },
            onDelete = { code = code.dropLast(1) },
            extraIcon = if (bioAvailable) R.drawable.ic_fingerprint else null,
            onExtra = if (bioAvailable) bio else null,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            "Forgot passcode?",
            style = Z.type.bodySmallMedium,
            color = Z.colors.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp).clip(RoundedCornerShape(6.dp)).pressable { showLogout = true }.padding(6.dp),
        )
    }
    if (showLogout) {
        ZDialog(
            onDismiss = { showLogout = false },
            title = "Reset passcode?",
            message = "You'll be logged out and need to sign in again with your password to set a new passcode.",
            confirmText = "Log out & reset",
            onConfirm = { showLogout = false; AppRepository.logout(); nav.navigateClear(Routes.LOGIN) },
            icon = R.drawable.ic_passcode,
        )
    }
}
