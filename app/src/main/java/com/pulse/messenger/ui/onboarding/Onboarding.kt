package com.pulse.messenger.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.R
import com.pulse.messenger.data.MockData
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.components.ActionSheet
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.BrandWordmark
import com.pulse.messenger.ui.components.ChoiceDialog
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.DialogButton
import com.pulse.messenger.ui.components.LoadingDialog
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.PulseBottomSheet
import com.pulse.messenger.ui.components.PulseDialog
import com.pulse.messenger.ui.components.SheetAction
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.navigation.goHomeClearingStack
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Slide(val image: Int, val title: String, val text: String)

private val slides = listOf(
    Slide(R.drawable.onb_welcome, "Welcome to Pulse", "Simple, fast and reliable messaging for the people who matter most."),
    Slide(R.drawable.onb_privacy_e2e, "Private by design", "Your personal messages and calls are end-to-end encrypted. Only you and the person you're talking to can read them."),
    Slide(R.drawable.onb_calls, "Crystal-clear calls", "Free voice and video calls with friends and family, anywhere in the world."),
    Slide(R.drawable.onb_status, "Share moments", "Post photos, videos and text updates that disappear after 24 hours."),
    Slide(R.drawable.onb_communities, "Bring groups together", "Communities connect related groups so everyone stays in the loop."),
)

val languages = listOf("English", "Español", "Français", "Deutsch", "Português", "हिन्दी", "বাংলা", "العربية", "Bahasa Indonesia", "Türkçe", "日本語", "한국어")

@Composable
fun WelcomeScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val pager = rememberPagerState { slides.size }
    var showLanguage by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(settingsRepo.current.language) }

    LaunchedEffect(pager) {
        while (true) {
            delay(4200)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % slides.size)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandWordmark(Modifier.height(30.dp))
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PulseTheme.colors.chip)
                    .clickable { showLanguage = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Language, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(language, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(18.dp))
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val s = slides[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AsyncImage(
                    model = s.image, contentDescription = null,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f),
                )
                Spacer(Modifier.height(12.dp))
                Text(s.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    s.text, style = MaterialTheme.typography.bodyLarge, color = PulseTheme.colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 340.dp),
                )
            }
        }
        Row(Modifier.padding(vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(slides.size) { i ->
                val selected = pager.currentPage == i
                val w by animateDpAsState(if (selected) 22.dp else 8.dp, label = "dot")
                val c by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else PulseTheme.colors.divider, label = "dotc")
                Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(c))
            }
        }
        Text(
            buildAnnotatedString {
                append("Read our ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) { append("Privacy Policy") }
                append(". Tap \"Agree and continue\" to accept the ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) { append("Terms of Service") }
                append(".")
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = PulseTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 36.dp),
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            "Agree and continue",
            onClick = { nav.navigate(Routes.PHONE) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(20.dp))
    }

    if (showLanguage) {
        ChoiceDialog(
            title = "App language",
            options = languages,
            selected = language,
            label = { it },
            onSelect = { language = it; settingsRepo.update { s -> s.copy(language = it) } },
            onDismiss = { showLanguage = false },
        )
    }
}

data class Country(val flag: String, val name: String, val code: String)

val countries = listOf(
    Country("🇺🇸", "United States", "+1"), Country("🇬🇧", "United Kingdom", "+44"), Country("🇮🇳", "India", "+91"),
    Country("🇧🇩", "Bangladesh", "+880"), Country("🇵🇰", "Pakistan", "+92"), Country("🇳🇬", "Nigeria", "+234"),
    Country("🇦🇪", "United Arab Emirates", "+971"), Country("🇸🇦", "Saudi Arabia", "+966"), Country("🇨🇦", "Canada", "+1"),
    Country("🇦🇺", "Australia", "+61"), Country("🇩🇪", "Germany", "+49"), Country("🇫🇷", "France", "+33"),
    Country("🇪🇸", "Spain", "+34"), Country("🇮🇹", "Italy", "+39"), Country("🇧🇷", "Brazil", "+55"),
    Country("🇲🇽", "Mexico", "+52"), Country("🇯🇵", "Japan", "+81"), Country("🇰🇷", "South Korea", "+82"),
    Country("🇨🇳", "China", "+86"), Country("🇮🇩", "Indonesia", "+62"), Country("🇲🇾", "Malaysia", "+60"),
    Country("🇵🇭", "Philippines", "+63"), Country("🇻🇳", "Vietnam", "+84"), Country("🇹🇭", "Thailand", "+66"),
    Country("🇹🇷", "Turkey", "+90"), Country("🇪🇬", "Egypt", "+20"), Country("🇲🇦", "Morocco", "+212"),
    Country("🇿🇦", "South Africa", "+27"), Country("🇰🇪", "Kenya", "+254"), Country("🇬🇭", "Ghana", "+233"),
    Country("🇳🇱", "Netherlands", "+31"), Country("🇸🇪", "Sweden", "+46"), Country("🇨🇭", "Switzerland", "+41"),
    Country("🇸🇬", "Singapore", "+65"), Country("🇳🇿", "New Zealand", "+64"), Country("🇯🇲", "Jamaica", "+1 876"),
).sortedBy { it.name }

@Composable
fun CountryPickerSheet(onPick: (Country) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        countries.filter { it.name.contains(query, true) || it.code.contains(query) }
    }
    PulseBottomSheet(onDismiss) {
        Text("Choose a country", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(PulseTheme.colors.chip)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = PulseTheme.colors.textSecondary)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search countries", color = PulseTheme.colors.textSecondary)
                    inner()
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.height(440.dp)) {
            items(filtered, key = { it.name }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(c); onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.flag, fontSize = 24.sp)
                    Spacer(Modifier.width(16.dp))
                    Text(c.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(c.code, color = PulseTheme.colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun PhoneScreen(nav: NavController) {
    var country by remember { mutableStateOf(countries.first { it.name == "United States" }) }
    var number by rememberSaveable { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(300); focus.requestFocus() }

    fun submit() {
        val digits = number.filter { it.isDigit() }
        error = when {
            digits.isEmpty() -> "Please enter your phone number."
            digits.length < 6 -> "The phone number you entered is too short for ${country.name}."
            digits.length > 13 -> "The phone number you entered is too long for ${country.name}."
            else -> null
        }
        if (error == null) confirm = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Enter your phone number", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        Text(
            "Pulse will send an SMS to verify your phone number. Carrier charges may apply.",
            style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Column(Modifier.widthIn(max = 320.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(country.flag, fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Text(country.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Icon(Icons.Filled.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.width(84.dp)) {
                    Text(country.code, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                    HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    BasicTextField(
                        value = number,
                        onValueChange = { v -> number = v.filter { it.isDigit() || it == ' ' }.take(16); error = null },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground, letterSpacing = 1.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).focusRequester(focus),
                        decorationBox = { inner ->
                            if (number.isEmpty()) Text("Phone number", color = PulseTheme.colors.textSecondary)
                            inner()
                        },
                    )
                    HorizontalDivider(thickness = 1.5.dp, color = if (error != null) PulseTheme.colors.danger else MaterialTheme.colorScheme.primary)
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = PulseTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Next", onClick = { submit() }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
    }

    if (showPicker) CountryPickerSheet(onPick = { country = it }, onDismiss = { showPicker = false })
    if (confirm) {
        val full = "${country.code} ${number.trim()}"
        PulseDialog(
            onDismiss = { confirm = false },
            title = "Is this the correct number?",
            message = full,
            icon = Icons.Outlined.Phone,
            buttons = listOf(
                DialogButton("Edit") { confirm = false },
                DialogButton("Yes") {
                    confirm = false
                    nav.navigate(Routes.otp(full))
                },
            ),
        )
    }
}

@Composable
fun OtpScreen(nav: NavController, phone: String) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var verifying by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(60) }
    var showResend by remember { mutableStateOf(false) }
    var detecting by remember { mutableStateOf(true) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun verify(c: String) {
        if (verifying) return
        verifying = true
        focusManager.clearFocus()
        scope.launch {
            delay(1400)
            verifying = false
            settingsRepo.update {
                it.copy(phone = phone, countryCode = phone.substringBefore(" "))
            }
            snackbar.show("Phone number verified", SnackType.SUCCESS)
            nav.navigate(Routes.PROFILE_SETUP) { popUpTo(Routes.WELCOME) { inclusive = true } }
        }
    }

    LaunchedEffect(Unit) { delay(250); focus.requestFocus() }
    LaunchedEffect(seconds) { if (seconds > 0) { delay(1000); seconds-- } }
    LaunchedEffect(Unit) {
        // Simulates the SMS retriever filling the code in.
        delay(3500)
        if (code.isEmpty()) {
            val auto = "482913"
            for (i in 1..6) { code = auto.take(i); delay(90) }
            detecting = false
            verify(auto)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Verifying your number", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                append("Waiting to automatically detect an SMS sent to ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(phone) }
                append(". ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) { append("Wrong number?") }
            },
            style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.clickable { nav.back() },
        )
        Spacer(Modifier.height(20.dp))
        AsyncImage(model = R.drawable.onb_verify_phone, contentDescription = null, modifier = Modifier.size(170.dp))
        Spacer(Modifier.height(16.dp))

        BasicTextField(
            value = code,
            onValueChange = { v ->
                val digits = v.filter { it.isDigit() }.take(6)
                code = digits
                detecting = false
                if (digits.length == 6) verify(digits)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.focusRequester(focus),
            decorationBox = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(6) { i ->
                        val ch = code.getOrNull(i)?.toString().orEmpty()
                        val active = i == code.length
                        val border by animateColorAsState(
                            if (active) MaterialTheme.colorScheme.primary else PulseTheme.colors.divider, label = "otp",
                        )
                        Box(
                            Modifier
                                .size(width = 44.dp, height = 54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PulseTheme.colors.chip)
                                .border(if (active) 2.dp else 1.dp, border, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(ch, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "digit") { d ->
                                Text(d, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }
            },
        )
        Spacer(Modifier.height(14.dp))
        Text(
            if (detecting) "Detecting code automatically…" else "Enter 6-digit code",
            style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { showResend = true }, enabled = seconds == 0) {
            Text(
                if (seconds > 0) "Didn't receive code? Resend in ${Format.duration(seconds)}" else "Didn't receive code?",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (verifying) LoadingDialog("Verifying…")
    if (showResend) {
        ActionSheet(
            onDismiss = { showResend = false },
            title = "Didn't receive the code?",
            actions = listOf(
                SheetAction("Resend SMS", Icons.Outlined.Sms) {
                    seconds = 60
                    snackbar.show("A new code was sent to $phone", SnackType.SUCCESS)
                },
                SheetAction("Call me", Icons.Outlined.Phone) {
                    seconds = 60
                    snackbar.show("You'll receive a call shortly", SnackType.INFO)
                },
            ),
        )
    }
}

@Composable
fun AvatarPickerSheet(
    onPick: (Int) -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit,
    options: List<Int> = MockData.avatarPeople,
    title: String = "Profile photo",
) {
    PulseBottomSheet(onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (onRemove != null) {
                TextButton(onClick = { onRemove(); onDismiss() }) {
                    Icon(Icons.Filled.Delete, null, tint = PulseTheme.colors.danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Remove", color = PulseTheme.colors.danger)
                }
            }
        }
        Text(
            "Choose from gallery",
            style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(380.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(options) { res ->
                AsyncImage(
                    model = res, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .clickable { onPick(res); onDismiss() },
                )
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    var name by rememberSaveable { mutableStateOf(settingsRepo.current.name) }
    var avatar by rememberSaveable { mutableStateOf(settingsRepo.current.avatar) }
    var picker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Profile info", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        Text(
            "Please provide your name and an optional profile photo",
            style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Box(Modifier.clickable { picker = true }) {
            if (avatar != null) {
                Avatar(avatar, name, 128.dp)
            } else {
                Box(
                    Modifier.size(128.dp).clip(CircleShape).background(PulseTheme.colors.chip),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.CameraAlt, null, tint = PulseTheme.colors.textSecondary, modifier = Modifier.size(44.dp)) }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 25) { name = it; nameError = false } },
            label = { Text("Your name") },
            singleLine = true,
            isError = nameError,
            supportingText = {
                Text(
                    if (nameError) "Please enter your name" else "${25 - name.length}",
                    modifier = Modifier.fillMaxWidth(), textAlign = if (nameError) TextAlign.Start else TextAlign.End,
                )
            },
            trailingIcon = { Icon(Icons.Outlined.EmojiEmotions, null, tint = PulseTheme.colors.textSecondary) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            "Next",
            onClick = {
                if (name.isBlank()) {
                    nameError = true
                } else {
                    settingsRepo.update { it.copy(name = name.trim(), avatar = avatar) }
                    nav.navigate(Routes.RESTORE)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
    }
    if (picker) {
        AvatarPickerSheet(onPick = { avatar = it }, onRemove = if (avatar != null) ({ avatar = null }) else null, onDismiss = { picker = false })
    }
}

@Composable
fun RestoreScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    var stage by rememberSaveable { mutableIntStateOf(0) } // 0 found, 1 restoring, 2 done, 3 permissions
    var progress by remember { mutableStateOf(0f) }
    var skipDialog by remember { mutableStateOf(false) }
    val animated by animateFloatAsState(progress, label = "restore")

    LaunchedEffect(stage) {
        if (stage == 1) {
            progress = 0f
            while (progress < 1f) {
                delay(60)
                progress = (progress + 0.018f).coerceAtMost(1f)
            }
            stage = 2
        }
    }

    fun finish() {
        settingsRepo.update { it.copy(onboarded = true, lastBackup = System.currentTimeMillis() - 4 * 86_400_000L) }
        nav.goHomeClearingStack()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(stage == 3, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "restore", modifier = Modifier.weight(1f)) { perms ->
            if (perms) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = R.drawable.perm_contacts, contentDescription = null, modifier = Modifier.size(130.dp))
                        AsyncImage(model = R.drawable.perm_notifications, contentDescription = null, modifier = Modifier.size(130.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Contacts and notifications", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "To help you message friends and family, allow Pulse access to your contacts and to send you notifications for new messages and calls.",
                        style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    PermRow(Icons.Outlined.Chat, "Find friends who use Pulse")
                    PermRow(Icons.Outlined.Lock, "Your contacts stay private and encrypted")
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    AsyncImage(model = R.drawable.onb_backup_restore, contentDescription = null, modifier = Modifier.size(220.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (stage == 2) "Backup restored" else "Restore backup",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when (stage) {
                            0 -> "A backup was found in your cloud account. Restore your messages and media now. If you don't restore now, you won't be able to restore later."
                            1 -> "Restoring messages (${(animated * 2431).toInt()} of 2,431)…"
                            else -> "2,431 messages and 312 media files restored."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    if (stage == 0) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PulseTheme.colors.chip).padding(16.dp),
                        ) {
                            Text("Last backup", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(4.dp))
                            Text("${Format.date(System.currentTimeMillis() - 4 * 86_400_000L)} · 248 MB", color = PulseTheme.colors.textSecondary)
                        }
                    }
                    if (stage >= 1) {
                        LinearProgressIndicator(
                            progress = { animated },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = PulseTheme.colors.chip,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    if (stage == 2) {
                        Spacer(Modifier.height(16.dp))
                        Icon(Icons.Filled.CheckCircle, null, tint = PulseTheme.colors.success, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
        when (stage) {
            0 -> {
                PrimaryButton("Restore", onClick = { stage = 1 }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { skipDialog = true }, modifier = Modifier.padding(vertical = 8.dp)) { Text("Skip", fontWeight = FontWeight.SemiBold) }
            }
            1 -> Spacer(Modifier.height(66.dp))
            2 -> {
                PrimaryButton("Next", onClick = { stage = 3 }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(58.dp))
            }
            else -> {
                PrimaryButton("Continue", onClick = { finish() }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { finish() }, modifier = Modifier.padding(vertical = 8.dp)) { Text("Not now", fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (skipDialog) {
        ConfirmDialog(
            title = "Skip restoring?",
            message = "If you skip, you won't be able to restore your messages and media later.",
            confirmText = "Skip restore",
            destructive = true,
            onConfirm = { stage = 3 },
            onDismiss = { skipDialog = false },
        )
    }
}

@Composable
private fun PermRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

