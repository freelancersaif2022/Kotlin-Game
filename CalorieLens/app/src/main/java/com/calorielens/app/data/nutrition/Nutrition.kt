package com.calorielens.app.data.nutrition

/** Nutrition for one typical serving of a food. */
data class Nutrition(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val saturatedFat: Float,
    val fiber: Float,
    val sugar: Float,
    val sodiumMg: Int,
    val cholesterolMg: Int,
    val potassiumMg: Int,
    val calciumMg: Int,
    val ironMg: Float,
    val vitaminAMcg: Int,
    val vitaminCMg: Float,
)

enum class HealthTag(val label: String, val positive: Boolean) {
    HIGH_PROTEIN("High protein", true),
    HIGH_FIBER("High fiber", true),
    LOW_CALORIE("Low calorie", true),
    LOW_CARB("Low carb", true),
    VITAMIN_RICH("Vitamin rich", true),
    HIGH_SUGAR("High sugar", false),
    HIGH_SODIUM("High sodium", false),
    HIGH_FAT("High fat", false),
    CALORIE_DENSE("Calorie dense", false),
}

fun Nutrition.healthTags(): List<HealthTag> = buildList {
    if (protein >= 20f || (calories > 0 && protein * 4 / calories >= 0.28f)) add(HealthTag.HIGH_PROTEIN)
    if (fiber >= 5f) add(HealthTag.HIGH_FIBER)
    if (calories in 1..199) add(HealthTag.LOW_CALORIE)
    if (carbs < 12f) add(HealthTag.LOW_CARB)
    if (vitaminCMg >= 25f || vitaminAMcg >= 250) add(HealthTag.VITAMIN_RICH)
    if (sugar >= 20f) add(HealthTag.HIGH_SUGAR)
    if (sodiumMg >= 900) add(HealthTag.HIGH_SODIUM)
    if (fat >= 28f || saturatedFat >= 10f) add(HealthTag.HIGH_FAT)
    if (calories >= 650) add(HealthTag.CALORIE_DENSE)
}

/** A simple 1..10 score that rewards protein, fiber and micronutrients and penalises sugar, sodium and saturated fat. */
fun Nutrition.healthScore(): Int {
    var score = 6.0
    score += (protein / 12f).coerceAtMost(2f)
    score += (fiber / 3f).coerceAtMost(1.5f)
    score += if (vitaminCMg >= 20f || vitaminAMcg >= 200) 0.8 else 0.0
    score -= (sugar / 12f).coerceAtMost(2.5f)
    score -= (sodiumMg / 600f).coerceAtMost(2f)
    score -= (saturatedFat / 6f).coerceAtMost(2f)
    if (calories > 700) score -= 1.0
    return score.toInt().coerceIn(1, 10)
}
