package com.spectral.ghostdetector.data

import android.content.Context
import kotlin.random.Random

/** 600+ ghost names loaded from assets/ghost_names.txt. */
object GhostNames {
    @Volatile
    private var names: List<String> = emptyList()

    private val fallback = listOf(
        "The Grey Lady", "Edgar Ravenscroft", "Lenore the Weeping", "Silas Blackwood",
        "Ophelia Thornfield", "The Hollow Man", "Agatha Graves", "Mortimer Lockwood",
    )

    val count: Int get() = names.size.takeIf { it > 0 } ?: fallback.size

    fun load(context: Context) {
        if (names.isNotEmpty()) return
        names = runCatching {
            context.assets.open("ghost_names.txt").bufferedReader().useLines { seq ->
                seq.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }
        }.getOrDefault(emptyList())
    }

    fun random(random: Random = Random): String {
        val list = names.ifEmpty { fallback }
        return list[random.nextInt(list.size)]
    }
}
