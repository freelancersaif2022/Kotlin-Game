package com.calorielens.app.domain

import kotlin.math.roundToInt

enum class Gender(val label: String) { MALE("Male"), FEMALE("Female") }

enum class ActivityLevel(val label: String, val description: String, val factor: Float) {
    SEDENTARY("Sedentary", "Little or no exercise, desk job", 1.2f),
    LIGHT("Lightly active", "Light exercise 1–3 days a week", 1.375f),
    MODERATE("Moderately active", "Exercise 3–5 days a week", 1.55f),
    ACTIVE("Very active", "Hard exercise 6–7 days a week", 1.725f),
    ATHLETE("Athlete", "Physical job or training twice a day", 1.9f),
}

enum class Goal(val label: String, val description: String, val adjustment: Int) {
    LOSE("Lose weight", "Steady deficit of about 0.5 kg a week", -500),
    MAINTAIN("Maintain weight", "Stay at your current weight", 0),
    GAIN("Build muscle", "Lean surplus to support growth", 350),
}

enum class ThemeMode(val label: String) { DARK("Dark"), LIGHT("Light"), SYSTEM("System") }

enum class WeightUnit(val label: String) { KG("kg"), LB("lb") }

data class UserProfile(
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val age: Int = 28,
    val heightCm: Int = 172,
    val weightKg: Float = 70f,
    val activity: ActivityLevel = ActivityLevel.MODERATE,
    val goal: Goal = Goal.MAINTAIN,
)

data class DailyTargets(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

object CalorieCalculator {
    /** Mifflin–St Jeor BMR × activity factor, adjusted for the goal. */
    fun targets(p: UserProfile): DailyTargets {
        val bmr = 10 * p.weightKg + 6.25f * p.heightCm - 5 * p.age + if (p.gender == Gender.MALE) 5 else -161
        val tdee = bmr * p.activity.factor
        val minimum = if (p.gender == Gender.MALE) 1500 else 1200
        val calories = ((tdee + p.goal.adjustment) / 10f).roundToInt() * 10
        val target = calories.coerceAtLeast(minimum)
        val (pPct, cPct, fPct) = when (p.goal) {
            Goal.LOSE -> Triple(0.30f, 0.40f, 0.30f)
            Goal.MAINTAIN -> Triple(0.25f, 0.45f, 0.30f)
            Goal.GAIN -> Triple(0.30f, 0.45f, 0.25f)
        }
        return DailyTargets(
            calories = target,
            protein = (target * pPct / 4f).roundToInt(),
            carbs = (target * cPct / 4f).roundToInt(),
            fat = (target * fPct / 9f).roundToInt(),
        )
    }

    fun bmi(p: UserProfile): Float {
        val m = p.heightCm / 100f
        return if (m <= 0f) 0f else p.weightKg / (m * m)
    }
}

fun Float.kgTo(unit: WeightUnit): Float = if (unit == WeightUnit.KG) this else this * 2.20462f
