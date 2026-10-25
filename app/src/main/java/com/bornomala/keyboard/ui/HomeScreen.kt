package com.bornomala.keyboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bornomala.keyboard.R
import com.bornomala.keyboard.prefs.BanglaLayout
import com.bornomala.keyboard.prefs.Keys
import com.bornomala.keyboard.theme.KeyboardThemes

@Composable
fun App(screen: Screen, refreshTick: Int, navigate: (Screen) -> Unit, back: () -> Unit) {
    when (screen) {
        Screen.HOME -> HomeScreen(refreshTick, navigate)
        Screen.SETUP -> SetupScreen(refreshTick, back)
        Screen.LANGUAGES -> LanguagesScreen(back, navigate)
        Screen.PHONETIC_GUIDE -> PhoneticGuideScreen(back)
        Screen.THEMES -> ThemesScreen(back)
        Screen.KEYS -> KeysScreen(back)
        Screen.TYPING -> TypingScreen(back)
        Screen.SOUND -> SoundScreen(back)
        Screen.GESTURES -> GesturesScreen(back)
        Screen.CLIPBOARD -> ClipboardScreen(back)
        Screen.DICTIONARY -> DictionaryScreen(back)
        Screen.BACKUP -> BackupScreen(back)
        Screen.PRIVACY -> PrivacyScreen(back)
        Screen.ABOUT -> AboutScreen(back)
    }
}

@Composable
private fun HomeScreen(refreshTick: Int, navigate: (Screen) -> Unit) {
    val context = LocalContext.current
    val enabled = remember(refreshTick) { MainActivity.isKeyboardEnabled(context) }
    val selected = remember(refreshTick) { MainActivity.isKeyboardSelected(context) }
    val layout by rememberPref(Keys.BANGLA_LAYOUT, BanglaLayout.PHONETIC)
    val theme by rememberPref(Keys.THEME, "green")

    SettingsScaffold(title = Screen.HOME.title, onBack = null) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("বর্ণমালা", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "অভ্র · জাতীয় · English — free, no ads",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!enabled || !selected) {
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        if (!enabled) "Turn on Bornomala Keyboard" else "Switch to Bornomala Keyboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Two quick steps and you can type Bangla and English anywhere.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { navigate(Screen.SETUP) }) { Text("Set up now") }
                }
            }
        } else {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Keyboard is on and ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        var tryText by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = tryText,
            onValueChange = { tryText = it },
            label = { Text("Try typing here — এখানে লিখে দেখুন") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            minLines = 2,
        )

        SectionTitle("Keyboard")
        NavRow(R.drawable.ic_set_languages, "Languages & layouts",
            if (layout == BanglaLayout.NATIONAL) "Bangla National (জাতীয়) + English" else "Bangla Phonetic (অভ্র style) + English") {
            navigate(Screen.LANGUAGES)
        }
        NavRow(R.drawable.ic_set_themes, "Themes", KeyboardThemes.displayName(theme)) { navigate(Screen.THEMES) }
        NavRow(R.drawable.ic_set_layouts, "Keys & layout", "Number row, height, one-handed mode") { navigate(Screen.KEYS) }
        NavRow(R.drawable.ic_set_typing, "Typing & suggestions", "Auto-correct, capitalisation, word learning") { navigate(Screen.TYPING) }
        NavRow(R.drawable.ic_set_sound_vibration, "Sound & vibration") { navigate(Screen.SOUND) }
        NavRow(R.drawable.ic_set_gestures, "Gestures", "Space-bar cursor, swipe to delete") { navigate(Screen.GESTURES) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("Your data")
        NavRow(R.drawable.ic_set_clipboard, "Clipboard") { navigate(Screen.CLIPBOARD) }
        NavRow(R.drawable.ic_set_dictionary, "Learned words") { navigate(Screen.DICTIONARY) }
        NavRow(R.drawable.ic_set_backup, "Backup & restore") { navigate(Screen.BACKUP) }
        NavRow(R.drawable.ic_set_privacy, "Privacy", "No internet access. Nothing leaves your phone.") { navigate(Screen.PRIVACY) }
        NavRow(R.drawable.ic_set_about, "About") { navigate(Screen.ABOUT) }
    }
}

@Composable
private fun SetupScreen(refreshTick: Int, back: () -> Unit) {
    val context = LocalContext.current
    val enabled = remember(refreshTick) { MainActivity.isKeyboardEnabled(context) }
    val selected = remember(refreshTick) { MainActivity.isKeyboardSelected(context) }
    var layout by rememberPref(Keys.BANGLA_LAYOUT, BanglaLayout.PHONETIC)
    var layoutChosen by rememberSaveable { mutableStateOf(false) }
    val step = when {
        !enabled -> 0
        !selected -> 1
        !layoutChosen -> 2
        else -> 3
    }
    val image = when (step) {
        0 -> R.drawable.onboarding_enable
        1 -> R.drawable.onboarding_select
        2 -> R.drawable.onboarding_layouts
        else -> R.drawable.onboarding_done
    }

    SettingsScaffold(title = Screen.SETUP.title, onBack = back) {
        Image(
            painterResource(image), contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .padding(horizontal = 24.dp),
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(if (i == step) 22.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                )
            }
        }
        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                0 -> SetupStep(
                    "Step 1 · Enable the keyboard",
                    "Turn on “Bornomala Keyboard” in the list. Android shows a standard warning for every keyboard — Bornomala has no internet permission, so your typing never leaves the phone.",
                    "Open keyboard settings",
                ) { MainActivity.openInputMethodSettings(context) }
                1 -> SetupStep(
                    "Step 2 · Choose Bornomala",
                    "Pick “Bornomala Keyboard” as your current input method.",
                    "Choose keyboard",
                ) { MainActivity.showInputMethodPicker(context) }
                2 -> {
                    Text("Step 3 · Pick your Bangla layout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    LayoutChoice(
                        title = "Phonetic (অভ্র style)",
                        description = "Type Bangla with English letters: ami → আমি, bangla → বাংলা",
                        selected = layout == BanglaLayout.PHONETIC,
                    ) { layout = BanglaLayout.PHONETIC }
                    LayoutChoice(
                        title = "National (জাতীয়)",
                        description = "Bangla letters on the keys, same arrangement as Ridmik's National layout",
                        selected = layout == BanglaLayout.NATIONAL,
                    ) { layout = BanglaLayout.NATIONAL }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { layoutChosen = true }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                    Text(
                        "You can switch any time from the keyboard toolbar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                else -> SetupStep(
                    "All set! সব ঠিক আছে!",
                    "Tap the globe key to switch between বাংলা and English. Long-press it for emoji.",
                    "Start typing",
                ) { back() }
            }
        }
    }
}

@Composable
private fun SetupStep(title: String, body: String, action: String, onClick: () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(action) }
}

@Composable
private fun LayoutChoice(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline),
    ) {
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            if (selected) {
                Icon(painterResource(R.drawable.ic_set_check_circle), contentDescription = "Selected", tint = colors.primary)
            }
        }
    }
}
