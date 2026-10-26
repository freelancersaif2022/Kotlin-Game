package com.bornomala.keyboard.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bornomala.keyboard.BuildConfig
import com.bornomala.keyboard.R
import com.bornomala.keyboard.engine.AvroPhonetic
import com.bornomala.keyboard.ime.ClipboardHistory
import com.bornomala.keyboard.ime.Dictionaries
import com.bornomala.keyboard.prefs.BanglaLayout
import com.bornomala.keyboard.prefs.Keys
import com.bornomala.keyboard.prefs.CustomThemeSettings
import com.bornomala.keyboard.prefs.OneHanded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bornomala.keyboard.theme.KeyboardTheme
import com.bornomala.keyboard.theme.KeyboardThemes

// ---------------------------------------------------------------- languages

@Composable
fun LanguagesScreen(back: () -> Unit, navigate: (Screen) -> Unit) {
    val english = rememberPref(Keys.ENABLED_ENGLISH, true)
    val bangla = rememberPref(Keys.ENABLED_BANGLA, true)
    val layout = rememberPref(Keys.BANGLA_LAYOUT, BanglaLayout.PHONETIC)
    val digits = rememberPref(Keys.BANGLA_DIGITS, true)
    SettingsScaffold(Screen.LANGUAGES.title, back) {
        SectionTitle("Languages")
        SwitchRow("বাংলা (Bangla)", state = bangla, enabled = english.value)
        SwitchRow("English", state = english, enabled = bangla.value)
        BodyText("With both on, tap the globe key to switch. At least one language stays enabled.")
        SectionTitle("Bangla layout")
        RadioGroup(
            options = listOf(BanglaLayout.PHONETIC to "Phonetic (অভ্র style)", BanglaLayout.NATIONAL to "National (জাতীয়)"),
            state = layout,
            descriptions = mapOf(
                BanglaLayout.PHONETIC to "English letters turn into Bangla as you type, with smart suggestions",
                BanglaLayout.NATIONAL to "Bangladesh national Jatiyo layout, Ridmik-style key placement",
            ),
        )
        NavRow(R.drawable.ic_set_dictionary, "Phonetic typing guide", "How to type every Bangla letter") {
            navigate(Screen.PHONETIC_GUIDE)
        }
        SectionTitle("Numbers")
        SwitchRow("Bangla digits (১২৩)", "Show Bangla numerals on the symbol keys while typing Bangla", digits)
    }
}

@Composable
fun PhoneticGuideScreen(back: () -> Unit) {
    var sample by remember { mutableStateOf("ami tOmake bhalobashi") }
    SettingsScaffold(Screen.PHONETIC_GUIDE.title, back) {
        OutlinedTextField(
            value = sample, onValueChange = { sample = it },
            label = { Text("Try it: type English letters") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Text(
            AvroPhonetic.parse(sample),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        BodyText("Upper-case letters matter for some sounds (T = ট, t = ত). Suggestions fix casual spellings automatically.")
        val groups = listOf(
            "Vowels" to listOf("o" to "অ", "a" to "আ / া", "i" to "ই / ি", "I, ee" to "ঈ / ী", "u" to "উ / ু",
                "U, oo" to "ঊ / ূ", "rri" to "ঋ / ৃ", "e" to "এ / ে", "OI" to "ঐ / ৈ", "O" to "ও / ো", "OU" to "ঔ / ৌ"),
            "Consonants" to listOf("k" to "ক", "kh" to "খ", "g" to "গ", "gh" to "ঘ", "Ng" to "ঙ", "c" to "চ", "ch" to "ছ",
                "j" to "জ", "jh" to "ঝ", "NG" to "ঞ", "T" to "ট", "Th" to "ঠ", "D" to "ড", "Dh" to "ঢ", "N" to "ণ",
                "t" to "ত", "th" to "থ", "d" to "দ", "dh" to "ধ", "n" to "ন", "p" to "প", "ph, f" to "ফ", "b" to "ব",
                "bh, v" to "ভ", "m" to "ম", "z" to "য", "r" to "র", "l" to "ল", "sh, S" to "শ", "Sh" to "ষ", "s" to "স",
                "h" to "হ", "R" to "ড়", "Rh" to "ঢ়", "y" to "য়", "t``" to "ৎ"),
            "Signs" to listOf("ng" to "ং", ":" to "ঃ", "^" to "ঁ", "rr + consonant" to "র্ (reph)", "consonant + r" to "্র (ra-phola)",
                "consonant + y / Z" to "্য (ya-phola)", "consonant + w" to "্ব (bo-phola)", "." to "।", "$" to "৳", "`" to "separator"),
            "Examples" to listOf("amar" to "আমার", "bangladesh" to "বাংলাদেশ", "prothom" to "প্রথম", "korrmo" to "কর্ম",
                "shikkha" to "শিক্ষা", "bidyaloy" to "বিদ্যালয়", "du:kho" to "দুঃখ", "ca^d" to "চাঁদ"),
        )
        for ((title, rows) in groups) {
            SectionTitle(title)
            for ((en, bn) in rows) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(en, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Text(bn, fontSize = 18.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- themes

@Composable
fun ThemesScreen(back: () -> Unit) {
    val context = LocalContext.current
    var selected by rememberPref(Keys.THEME, "green")
    val borders = rememberPref(Keys.KEY_BORDERS, false)
    val version = rememberPref(Keys.CUSTOM_BG_VERSION, 0L)
    val dim = rememberPref(Keys.CUSTOM_DIM, 20)
    val opacity = rememberPref(Keys.CUSTOM_KEY_OPACITY, 70)
    val align = rememberPref(Keys.CUSTOM_ALIGN, 50)
    val darkKeys = rememberPref(Keys.CUSTOM_DARK_KEYS, true)
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val photo by produceState<ImageBitmap?>(null, version.value) {
        value = withContext(Dispatchers.IO) { CustomBackground.preview(context)?.asImageBitmap() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            importing = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) { CustomBackground.import(context, uri) }
                importing = false
                if (!ok) Toast.makeText(context, "Could not use this photo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    val customSettings = CustomThemeSettings(version.value, dim.value, opacity.value, align.value, darkKeys.value)

    SettingsScaffold(Screen.THEMES.title, back) {
        SectionTitle("My photo")
        val customTheme = KeyboardThemes.resolve(context, "custom", false, customSettings)
        if (photo != null) {
            ThemeCard(
                theme = customTheme, name = "My photo", selected = selected == "custom",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), photo = photo, aspect = 2.2f,
            ) { selected = "custom" }
            if (selected == "custom") {
                SliderRow("Darken photo", dim, 0..80) { "$it%" }
                SliderRow("Key opacity", opacity, 0..100) { "$it%" }
                SliderRow("Photo position", align, 0..100) { when { it < 34 -> "Top"; it > 66 -> "Bottom"; else -> "Centre" } }
                SwitchRow("Dark keys", "Light text on dark glass keys. Turn off for bright photos.", darkKeys)
            }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = ::pick, enabled = !importing) { Text("Change photo") }
                if (selected != "custom") Button(onClick = { selected = "custom" }) { Text("Use my photo") }
            }
        } else {
            BodyText("Use any photo from your gallery as the keyboard background. You can darken it, move it and choose how see-through the keys are.")
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Button(onClick = ::pick, enabled = !importing) { Text(if (importing) "Loading…" else "Choose a photo") }
            }
        }

        SectionTitle("Built-in themes")
        SwitchRow("Key borders", "Draw an outline around every key", borders)
        val ids = KeyboardThemes.ALL_IDS.filter { it != "custom" }
        for (rowIds in ids.chunked(2)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (id in rowIds) {
                    val theme = remember(id, borders.value) { KeyboardThemes.resolve(context, id, borders.value) }
                    ThemeCard(
                        theme = theme,
                        name = KeyboardThemes.displayName(id),
                        selected = selected == id,
                        modifier = Modifier.weight(1f),
                    ) { selected = id }
                }
                if (rowIds.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: KeyboardTheme,
    name: String,
    selected: Boolean,
    modifier: Modifier,
    photo: ImageBitmap? = null,
    aspect: Float = 1.6f,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .padding(vertical = 6.dp)
            .clip(shape)
            .border(
                BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                shape,
            )
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).background(Color(theme.background))) {
            if (photo != null) {
                Image(
                    photo, null, contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, theme.imageAlign * 2f - 1f),
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = theme.imageDim / 100f)))
            } else if (theme.backgroundImage != 0) {
                Image(painterResource(theme.backgroundImage), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val rows = listOf(10, 9, 9, 5)
                for ((r, count) in rows.withIndex()) {
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (k in 0 until count) {
                            val color = when {
                                r == 3 && k == count - 1 -> theme.accent
                                r == 3 && k != 2 -> theme.functionalKey
                                r == 2 && (k == 0 || k == count - 1) -> theme.functionalKey
                                else -> theme.key
                            }
                            val weight = if (r == 3 && k == 2) 4f else 1f
                            Box(
                                Modifier
                                    .weight(weight)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(color))
                                    .then(if (theme.borders) Modifier.border(0.5.dp, Color(theme.keyBorder), RoundedCornerShape(3.dp)) else Modifier),
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (selected) Icon(painterResource(R.drawable.ic_set_check_circle), "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------------------------------------------------------- keys & layout

@Composable
fun KeysScreen(back: () -> Unit) {
    SettingsScaffold(Screen.KEYS.title, back) {
        SectionTitle("Layout")
        SwitchRow("Number row", "Show 1–0 above the letters", rememberPref(Keys.NUMBER_ROW, false))
        SwitchRow("Hint labels", "Show long-press characters on the keys", rememberPref(Keys.SHOW_HINTS, true))
        SwitchRow("Key press popup", "Show an enlarged letter while pressing", rememberPref(Keys.KEY_POPUP, true))
        SliderRow("Keyboard height", rememberPref(Keys.KEYBOARD_HEIGHT, 100), 75..135, steps = 11) { "$it%" }
        SliderRow("Long-press delay", rememberPref(Keys.LONG_PRESS_DELAY, 300), 150..700, steps = 10) { "$it ms" }
        SectionTitle("One-handed mode")
        RadioGroup(
            options = listOf(OneHanded.OFF to "Off", OneHanded.RIGHT to "Right hand", OneHanded.LEFT to "Left hand"),
            state = rememberPref(Keys.ONE_HANDED, OneHanded.OFF),
        )
    }
}

@Composable
fun TypingScreen(back: () -> Unit) {
    val suggestions = rememberPref(Keys.SUGGESTIONS, true)
    SettingsScaffold(Screen.TYPING.title, back) {
        SectionTitle("Suggestions")
        SwitchRow("Show suggestions", "Word suggestions above the keyboard", suggestions)
        SwitchRow("Bangla auto-correct", "Fix casual phonetic spellings on space (shobai → সবাই)",
            rememberPref(Keys.AUTOCORRECT_BANGLA, true), enabled = suggestions.value)
        SwitchRow("English auto-correct", "Fix obvious typos on space (teh → the)",
            rememberPref(Keys.AUTOCORRECT_ENGLISH, true), enabled = suggestions.value)
        SwitchRow("Space after suggestion", "Add a space after you tap a suggestion", rememberPref(Keys.AUTO_SPACE, true))
        SwitchRow("Learn new words", "Remember words you use to improve suggestions", rememberPref(Keys.LEARN_WORDS, true))
        SectionTitle("Corrections")
        SwitchRow("Auto-capitalisation", "Capitalise the first English word of a sentence", rememberPref(Keys.AUTO_CAP, true))
        SwitchRow("Double-space full stop", "Two spaces insert “. ” (or “। ” in Bangla)", rememberPref(Keys.DOUBLE_SPACE_PERIOD, true))
        BodyText("Long-press a suggestion to remove a learned word.")
    }
}

@Composable
fun SoundScreen(back: () -> Unit) {
    val vibrate = rememberPref(Keys.VIBRATE, true)
    val sound = rememberPref(Keys.SOUND, false)
    SettingsScaffold(Screen.SOUND.title, back) {
        SwitchRow("Vibrate on key press", state = vibrate)
        SliderRow("Vibration strength", rememberPref(Keys.VIBRATE_MS, 15), 5..60, enabled = vibrate.value) { "$it ms" }
        SwitchRow("Sound on key press", state = sound)
        SliderRow("Sound volume", rememberPref(Keys.SOUND_VOLUME, 30), 5..100, enabled = sound.value) { "$it%" }
    }
}

@Composable
fun GesturesScreen(back: () -> Unit) {
    SettingsScaffold(Screen.GESTURES.title, back) {
        SwitchRow("Space-bar cursor control", "Slide on the space bar to move the cursor", rememberPref(Keys.SPACE_SWIPE, true))
        SwitchRow("Swipe to delete words", "Slide left from backspace to delete whole words", rememberPref(Keys.DELETE_SWIPE, true))
        SwitchRow("Long-press space", "Open the system keyboard picker", rememberPref(Keys.SPACE_LONG_PRESS_PICKER, true))
        BodyText("Long-press the globe key for emoji. Double-tap shift for caps lock.")
    }
}

@Composable
fun ClipboardScreen(back: () -> Unit) {
    val context = LocalContext.current
    SettingsScaffold(Screen.CLIPBOARD.title, back) {
        SwitchRow("Clipboard history", "Copied text is kept for 24 hours. Pinned items stay until you remove them.", rememberPref(Keys.CLIPBOARD_HISTORY, true))
        BodyText("Passwords and other text that apps mark as sensitive are never saved. History is excluded from cloud backups.")
        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            OutlinedButton(onClick = {
                ClipboardHistory.clearAll(context)
                Toast.makeText(context, "Clipboard history cleared", Toast.LENGTH_SHORT).show()
            }) { Text("Clear all history") }
        }
    }
}

// ---------------------------------------------------------------- learned words

@Composable
fun DictionaryScreen(back: () -> Unit) {
    var version by remember { mutableIntStateOf(0) }
    val words = remember(version) { Dictionaries.user.allWords() }
    var newWord by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current
    SettingsScaffold(Screen.DICTIONARY.title, back) {
        BodyText("Words you type or pick are remembered on this phone and suggested first.")
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newWord, onValueChange = { newWord = it.trim() },
                label = { Text("Add a word") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(enabled = newWord.isNotEmpty(), onClick = {
                Dictionaries.user.learn(newWord, 5)
                Dictionaries.saveUser(context)
                newWord = ""
                version++
            }) { Text("Add") }
        }
        Spacer(Modifier.height(8.dp))
        if (words.isEmpty()) {
            BodyText("No learned words yet.")
        } else {
            for ((word, count) in words.take(500)) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(word, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("×$count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = {
                        Dictionaries.user.remove(word)
                        Dictionaries.saveUser(context)
                        version++
                    }) { Icon(painterResource(R.drawable.ic_tb_delete), "Remove $word") }
                }
            }
            Row(Modifier.padding(20.dp)) {
                OutlinedButton(onClick = { confirmClear = true }) { Text("Forget all learned words") }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Forget all learned words?") },
            text = { Text("Suggestions will go back to the built-in dictionary.") },
            confirmButton = {
                TextButton(onClick = {
                    Dictionaries.user.clear()
                    Dictionaries.saveUser(context)
                    confirmClear = false
                    version++
                }) { Text("Forget all") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------- backup

@Composable
fun BackupScreen(back: () -> Unit) {
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = Backup.export(context, uri)
            Toast.makeText(context, if (ok) "Backup saved" else "Could not save backup", Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = Backup.import(context, uri)
            Toast.makeText(context, if (ok) "Backup restored" else "This file is not a Bornomala backup", Toast.LENGTH_SHORT).show()
        }
    }
    SettingsScaffold(Screen.BACKUP.title, back) {
        BodyText("Save your settings and learned words to a file, for example before changing phones. Clipboard history is not included.")
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { exporter.launch("bornomala-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text("Save backup") }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restore from file")
            }
        }
    }
}

// ---------------------------------------------------------------- privacy & about

@Composable
fun PrivacyScreen(back: () -> Unit) {
    SettingsScaffold(Screen.PRIVACY.title, back) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            PrivacyPoint("No internet permission", "Bornomala cannot connect to the internet at all. Android enforces this.")
            PrivacyPoint("No ads, no tracking, no accounts", "There are no analytics or crash reporters inside the app.")
            PrivacyPoint("Learned words stay on your phone", "They are stored in the app's private storage. You can view and delete them under Learned words.")
            PrivacyPoint("Passwords are never learned", "Password fields switch off suggestions, learning and Bangla conversion.")
            PrivacyPoint("Incognito fields respected", "Apps that ask keyboards not to learn (for example private browser tabs) are honoured.")
            PrivacyPoint("Open source", "Anyone can read the code on GitHub.")
        }
    }
}

@Composable
private fun PrivacyPoint(title: String, body: String) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Icon(painterResource(R.drawable.ic_set_check_circle), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AboutScreen(back: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(context, url, Toast.LENGTH_LONG).show()
        }
    }
    SettingsScaffold(Screen.ABOUT.title, back) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, modifier = Modifier.size(88.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Bornomala Keyboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Free and open source · No ads · No premium", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NavRow(R.drawable.ic_set_github, "Source code", "github.com/freelancersaif2022/Kotlin-Game") {
            open("https://github.com/freelancersaif2022/Kotlin-Game")
        }
        NavRow(R.drawable.ic_set_rate, "Report a problem", "Open an issue on GitHub") {
            open("https://github.com/freelancersaif2022/Kotlin-Game/issues")
        }
        NavRow(R.drawable.ic_set_share, "Share Bornomala", "Tell friends about the keyboard") {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
                Intent.EXTRA_TEXT,
                "Bornomala Keyboard — free Bangla & English keyboard with no ads: https://github.com/freelancersaif2022/Kotlin-Game/releases",
            )
            context.startActivity(Intent.createChooser(send, "Share"))
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("Credits & licences")
        BodyText("App code: Apache License 2.0.")
        BodyText("Phonetic typing follows the Avro Phonetic scheme popularised by OmicronLab. Bornomala is an independent project and is not affiliated with OmicronLab or Ridmik.")
        BodyText("Word frequency lists derived from wordfreq by Robyn Speer (data under CC BY-SA 4.0).")
        BodyText("Emoji ordering from unicode-emoji-json (MIT).")
    }
}
