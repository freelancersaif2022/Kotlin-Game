package com.spectral.ghostdetector.data

import java.util.Calendar
import kotlin.random.Random

enum class GhostType(val label: String, val description: String) {
    POLTERGEIST("Poltergeist", "Noisy, restless spirit known for moving objects and sudden knocks."),
    WRAITH("Wraith", "A pale, drifting presence. Often felt as a sudden cold draft."),
    BANSHEE("Banshee", "A wailing spirit whose cries are said to carry over long distances."),
    SHADE("Shade", "A dark silhouette that lingers at the edge of vision."),
    REVENANT("Revenant", "A returning spirit bound to unfinished business."),
    PHANTOM("Phantom", "Elusive entity that flickers in and out of perception."),
    SPECTER("Specter", "Classic apparition with a strong electromagnetic signature."),
    WISP("Will-o'-Wisp", "Small floating light-energy that wanders aimlessly."),
    LOST_SOUL("Lost Soul", "Confused, harmless spirit searching for a way home."),
    SHADOW_PERSON("Shadow Person", "Tall dark figure. Watches silently, rarely moves."),
    RESIDUAL("Residual Echo", "An imprint of the past replaying the same moment over and over."),
    SPIRIT_CHILD("Spirit Child", "Playful young spirit. Attracted to toys and laughter."),
}

/** Fully randomized (prank) profile of a detected entity. */
data class GhostProfile(
    val name: String,
    val age: Int,
    val type: GhostType,
    val threat: Int,
    val mood: String,
    val diedYear: Int,
    val energy: Int,
    val distance: Float,
    val match: Int,
    val origin: String,
    val emfSignature: Float,
)

object GhostFactory {
    private val moods = listOf(
        "Calm", "Curious", "Restless", "Sorrowful", "Agitated", "Playful",
        "Hostile", "Confused", "Watchful", "Lonely", "Angry", "Peaceful",
    )
    private val origins = listOf(
        "Old manor house", "Abandoned hospital", "Village chapel", "Shipwreck",
        "Railway station", "Boarding school", "Forest crossroads", "Lighthouse",
        "Victorian theatre", "Family farmhouse", "Battlefield", "City orphanage",
        "Mountain inn", "Riverside mill", "Royal castle", "Unknown",
    )

    fun create(random: Random = Random): GhostProfile {
        val age = when (random.nextInt(100)) {
            in 0..59 -> random.nextInt(18, 150)
            in 60..89 -> random.nextInt(150, 420)
            else -> random.nextInt(420, 1250)
        }
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val threat = when (random.nextInt(100)) {
            in 0..34 -> 1
            in 35..59 -> 2
            in 60..79 -> 3
            in 80..92 -> 4
            else -> 5
        }
        return GhostProfile(
            name = GhostNames.random(random),
            age = age,
            type = GhostType.entries[random.nextInt(GhostType.entries.size)],
            threat = threat,
            mood = moods[random.nextInt(moods.size)],
            diedYear = year - age,
            energy = random.nextInt(22, 99),
            distance = random.nextInt(8, 65) / 10f,
            match = random.nextInt(71, 99),
            origin = origins[random.nextInt(origins.size)],
            emfSignature = random.nextInt(120, 980) / 10f,
        )
    }

    fun threatLabel(threat: Int) = when (threat) {
        1 -> "HARMLESS"
        2 -> "LOW"
        3 -> "MODERATE"
        4 -> "HIGH"
        else -> "EXTREME"
    }
}
