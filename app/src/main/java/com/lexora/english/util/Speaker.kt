package com.lexora.english.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** App-wide text-to-speech used for example sentences, dialogues and listening practice. */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null
    var rate: Float = 0.95f

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!ready) {
                    ready = tts?.setLanguage(Locale.UK)?.let { it >= TextToSpeech.LANG_AVAILABLE } ?: false
                }
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    val isReady: Boolean get() = ready

    fun speak(text: String) {
        val engine = tts ?: return
        if (!ready) {
            pending = text
            return
        }
        engine.setSpeechRate(rate)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexora_" + text.hashCode())
    }

    fun stop() {
        tts?.stop()
    }
}
