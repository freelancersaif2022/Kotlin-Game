package com.calorielens.app.ml

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import javax.inject.Inject
import javax.inject.Singleton

data class Recognition(val label: String, val confidence: Float)

sealed interface RecognitionResult {
    data class Food(val best: Recognition, val alternatives: List<Recognition>) : RecognitionResult
    data object NoFood : RecognitionResult
    data class Failed(val message: String) : RecognitionResult
}

/**
 * Fully on-device food recognition using a bundled TensorFlow Lite image classifier
 * (Google AIY "food V1" model, ~2,000 dishes). No network calls are made.
 */
@Singleton
class FoodRecognizer @Inject constructor(@ApplicationContext private val context: Context) {

    private val mutex = Mutex()
    private var classifier: ImageClassifier? = null

    val isModelAvailable: Boolean by lazy {
        runCatching { context.assets.list("models")?.contains(MODEL_FILE) == true }.getOrDefault(false)
    }

    /** Loads the model ahead of time so the first scan is instant. */
    suspend fun warmUp() {
        runCatching { mutex.withLock { ensureClassifier() } }
    }

    suspend fun recognize(bitmap: Bitmap): RecognitionResult = withContext(Dispatchers.Default) {
        if (!isModelAvailable) return@withContext RecognitionResult.Failed("Food recognition model is missing from this build.")
        try {
            val categories = mutex.withLock {
                val c = ensureClassifier()
                val input = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
                c.classify(TensorImage.fromBitmap(input)).firstOrNull()?.categories.orEmpty()
            }
            val ranked = categories
                .map { Recognition(it.displayName.ifBlank { it.label }, it.score) }
                .filterNot { it.label.isBlank() || it.label.contains("background", ignoreCase = true) }
                .sortedByDescending { it.confidence }
            val best = ranked.firstOrNull()
            if (best == null || best.confidence < MIN_CONFIDENCE) {
                RecognitionResult.NoFood
            } else {
                RecognitionResult.Food(
                    best = best,
                    alternatives = ranked.drop(1).filter { it.confidence >= ALT_CONFIDENCE }.take(3),
                )
            }
        } catch (t: Throwable) {
            RecognitionResult.Failed(t.message ?: "Recognition failed")
        }
    }

    private fun ensureClassifier(): ImageClassifier {
        classifier?.let { return it }
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(BaseOptions.builder().setNumThreads(4).build())
            .setMaxResults(6)
            .setDisplayNamesLocale("en")
            .build()
        return ImageClassifier.createFromFileAndOptions(context, "models/$MODEL_FILE", options).also { classifier = it }
    }

    companion object {
        const val MODEL_FILE = "food_model.tflite"
        private const val MIN_CONFIDENCE = 0.12f
        private const val ALT_CONFIDENCE = 0.03f
    }
}
