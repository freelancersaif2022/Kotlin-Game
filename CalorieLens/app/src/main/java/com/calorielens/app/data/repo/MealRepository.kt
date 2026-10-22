package com.calorielens.app.data.repo

import com.calorielens.app.data.db.MealDao
import com.calorielens.app.data.db.MealEntity
import com.calorielens.app.data.db.MealType
import com.calorielens.app.data.db.withNutrition
import com.calorielens.app.data.nutrition.FoodDatabase
import com.calorielens.app.data.storage.ImageStore
import com.calorielens.app.ml.Recognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class DayTotals(
    val date: LocalDate,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val meals: Int,
)

@Singleton
class MealRepository @Inject constructor(
    private val dao: MealDao,
    private val images: ImageStore,
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun mealsOn(date: LocalDate): Flow<List<MealEntity>> =
        dao.observeBetween(date.startMillis(), date.plusDays(1).startMillis())

    fun meal(id: Long): Flow<MealEntity?> = dao.observe(id)

    fun mealCount(): Flow<Int> = dao.observeCount()

    /** Per-day totals for every day in [from, to] inclusive; days without meals are zero. */
    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotals>> =
        dao.observeBetween(from.startMillis(), to.plusDays(1).startMillis()).map { meals ->
            val byDay = meals.groupBy { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), zone).toLocalDate() }
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { day ->
                val list = byDay[day].orEmpty()
                DayTotals(
                    date = day,
                    calories = list.sumOf { it.calories },
                    protein = list.sumOf { it.protein.toDouble() }.toFloat(),
                    carbs = list.sumOf { it.carbs.toDouble() }.toFloat(),
                    fat = list.sumOf { it.fat.toDouble() }.toFloat(),
                    meals = list.size,
                )
            }.toList()
        }

    suspend fun saveScan(imagePath: String, best: Recognition, alternatives: List<Recognition>): Long {
        val match = FoodDatabase.match(best.label)
        val now = LocalDateTime.now()
        val base = MealEntity(
            name = match.displayName,
            label = best.label,
            confidence = best.confidence,
            alternatives = alternatives.joinToString("||") { "${it.label}::${it.confidence}" },
            imagePath = imagePath,
            mealType = MealType.forHour(now.hour),
            timestamp = System.currentTimeMillis(),
            calories = 0, protein = 0f, carbs = 0f, fat = 0f, saturatedFat = 0f, fiber = 0f, sugar = 0f,
            sodiumMg = 0, cholesterolMg = 0, potassiumMg = 0, calciumMg = 0, ironMg = 0f, vitaminAMcg = 0, vitaminCMg = 0f,
        )
        return dao.insert(base.withNutrition(match.displayName, best.label, match.entry.nutrition))
    }

    /** Switches a meal to one of its alternative labels; the previous label becomes an alternative. */
    suspend fun switchTo(meal: MealEntity, label: String) {
        val match = FoodDatabase.match(label)
        val alts = meal.alternatives.split("||").filter { it.isNotBlank() && it.substringBefore("::") != label } +
            "${meal.label}::${meal.confidence}"
        val newConfidence = meal.alternatives.split("||")
            .firstOrNull { it.substringBefore("::") == label }
            ?.substringAfter("::")?.toFloatOrNull() ?: meal.confidence
        dao.update(
            meal.withNutrition(match.displayName, label, match.entry.nutrition)
                .copy(confidence = newConfidence, alternatives = alts.joinToString("||"))
        )
    }

    suspend fun setMealType(meal: MealEntity, type: MealType) = dao.update(meal.copy(mealType = type))

    suspend fun get(id: Long): MealEntity? = dao.get(id)

    /** Deletes the record but keeps the photo so the action can be undone. */
    suspend fun deleteRecord(meal: MealEntity) = dao.delete(meal)

    /** Re-inserts a deleted meal. Runs on the repository scope so it survives the screen closing. */
    fun restore(meal: MealEntity) {
        scope.launch { dao.insert(meal) }
    }

    /** Removes the photo of a meal once its deletion is final (not undone). */
    fun purgePhotoIfDeleted(meal: MealEntity) {
        scope.launch { if (dao.get(meal.id) == null) images.delete(meal.imagePath) }
    }

    suspend fun deleteEverything() {
        dao.deleteAll()
        images.deleteAll()
    }

    private fun LocalDate.startMillis(): Long = atStartOfDay(zone).toInstant().toEpochMilli()
}
