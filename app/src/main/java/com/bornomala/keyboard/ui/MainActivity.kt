package com.bornomala.keyboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bornomala.keyboard.ime.Dictionaries

class MainActivity : ComponentActivity() {

    /** Bumped whenever the keyboard's enabled/selected state may have changed. */
    private var refreshTick by mutableIntStateOf(0)
    private var backStack by mutableStateOf(listOf(Screen.HOME))

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Dictionaries.ensureLoaded(this)
        val restored = savedInstanceState?.getStringArrayList(KEY_STACK)?.mapNotNull { Screen.fromId(it) }
        backStack = restored?.takeIf { it.isNotEmpty() } ?: initialStack(intent)
        setContent {
            val dynamic by rememberPref("theme", "green")
            BornomalaTheme(dynamic = dynamic == "dynamic") {
                val screen = backStack.last()
                BackHandler(enabled = backStack.size > 1) { pop() }
                App(screen = screen, refreshTick = refreshTick, navigate = ::push, back = ::pop)
            }
        }
    }

    private fun initialStack(intent: Intent?): List<Screen> {
        val requested = intent?.getStringExtra(EXTRA_SCREEN)?.let { Screen.fromId(it) }
        return when {
            requested != null -> listOf(Screen.HOME, requested)
            !isKeyboardEnabled(this) || !isKeyboardSelected(this) -> listOf(Screen.HOME, Screen.SETUP)
            else -> listOf(Screen.HOME)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_SCREEN)?.let { Screen.fromId(it) }?.let { backStack = listOf(Screen.HOME, it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_STACK, ArrayList(backStack.map { it.id }))
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The input method picker is a dialog; regaining focus means the user may have chosen us.
        if (hasFocus) refreshTick++
    }

    override fun onPause() {
        super.onPause()
        Dictionaries.saveUser(this)
    }

    private fun push(screen: Screen) {
        backStack = backStack + screen
    }

    private fun pop() {
        if (backStack.size > 1) backStack = backStack.dropLast(1) else finish()
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
        private const val KEY_STACK = "back_stack"

        fun isKeyboardEnabled(context: Context): Boolean {
            val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            return imm.enabledInputMethodList.any { it.packageName == context.packageName }
        }

        fun isKeyboardSelected(context: Context): Boolean {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
            return id.startsWith(context.packageName + "/")
        }

        fun openInputMethodSettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        fun showInputMethodPicker(context: Context) {
            (context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
    }
}

enum class Screen(val id: String, val title: String) {
    HOME("home", "Bornomala Keyboard"),
    SETUP("setup", "Set up keyboard"),
    LANGUAGES("languages", "Languages & layouts"),
    PHONETIC_GUIDE("phonetic_guide", "Phonetic typing guide"),
    THEMES("themes", "Themes"),
    KEYS("keys", "Keys & layout"),
    TYPING("typing", "Typing & suggestions"),
    SOUND("sound", "Sound & vibration"),
    GESTURES("gestures", "Gestures"),
    CLIPBOARD("clipboard", "Clipboard"),
    DICTIONARY("dictionary", "Learned words"),
    BACKUP("backup", "Backup & restore"),
    PRIVACY("privacy", "Privacy"),
    ABOUT("about", "About");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id }
    }
}
