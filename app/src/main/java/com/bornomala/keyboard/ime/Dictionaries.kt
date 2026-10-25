package com.bornomala.keyboard.ime

import android.content.Context
import android.util.Log
import com.bornomala.keyboard.engine.SuggestionEngine
import com.bornomala.keyboard.engine.UserDictionary
import java.io.File

/**
 * Process-wide dictionaries shared by the keyboard service and the settings screens.
 * Word lists load once on a background thread; the keyboard works (without suggestions)
 * until they are ready.
 */
object Dictionaries {

    private const val TAG = "Dictionaries"
    private const val USER_FILE = "user_dictionary.tsv"

    val user = UserDictionary()
    val engine = SuggestionEngine(user)

    @Volatile
    private var started = false
    private var userLoaded = false

    @Synchronized
    fun ensureLoaded(context: Context) {
        val app = context.applicationContext
        if (!userLoaded) {
            userLoaded = true
            val f = File(app.filesDir, USER_FILE)
            if (f.exists()) {
                try {
                    f.bufferedReader().use { user.read(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not read user dictionary", e)
                }
            }
        }
        if (started) return
        started = true
        Thread {
            try {
                app.assets.open("dict/bn.txt").use { engine.loadBangla(it) }
                app.assets.open("dict/en.txt").use { engine.loadEnglish(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Could not load word lists", e)
            }
        }.apply {
            name = "bornomala-dict"
            priority = Thread.NORM_PRIORITY - 1
        }.start()
    }

    /** Persists the user dictionary if it changed. Cheap to call often. */
    fun saveUser(context: Context) {
        if (!user.dirty) return
        val app = context.applicationContext
        Thread {
            try {
                val tmp = File(app.filesDir, "$USER_FILE.tmp")
                tmp.bufferedWriter().use { user.write(it) }
                tmp.renameTo(File(app.filesDir, USER_FILE))
            } catch (e: Exception) {
                Log.w(TAG, "Could not save user dictionary", e)
            }
        }.start()
    }
}
