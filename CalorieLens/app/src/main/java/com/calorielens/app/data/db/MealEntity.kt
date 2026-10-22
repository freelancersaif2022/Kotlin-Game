package com.calorielens.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.calorielens.app.data.nutrition.Nutrition

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    SNACK("Snack"),
    DINNER("Dinner");

    companion object {
        fun forHour(hour: Int): MealType = when (hour) {
            in 4..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 16..17 -> SNACK
            in 18..22 -> DINNER
            else -> SNACK
        }
    }
}

@Entity(tableName = "meals", indices = [Index("timestamp")])
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val label: String,
    val confidence: Float,
    /** Other candidates as "label::score" joined with "||". */
    val alternatives: String,
    val imagePath: String,
    val mealType: MealType,
    val timestamp: Long,
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

val MealEntity.nutrition: Nutrition
    get() = Nutrition(
        calories, protein, carbs, fat, saturatedFat, fiber, sugar, sodiumMg, cholesterolMg,
        potassiumMg, calciumMg, ironMg, vitaminAMcg, vitaminCMg,
    )

val MealEntity.alternativeLabels: List<String>
    get() = alternatives.split("||").mapNotNull { it.substringBefore("::").trim().ifBlank { null } }

fun MealEntity.withNutrition(name: String, label: String, n: Nutrition) = copy(
    name = name, label = label,
    calories = n.calories, protein = n.protein, carbs = n.carbs, fat = n.fat, saturatedFat = n.saturatedFat,
    fiber = n.fiber, sugar = n.sugar, sodiumMg = n.sodiumMg, cholesterolMg = n.cholesterolMg,
    potassiumMg = n.potassiumMg, calciumMg = n.calciumMg, ironMg = n.ironMg, vitaminAMcg = n.vitaminAMcg,
    vitaminCMg = n.vitaminCMg,
)
