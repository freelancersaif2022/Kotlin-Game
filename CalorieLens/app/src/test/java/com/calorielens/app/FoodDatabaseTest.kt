package com.calorielens.app

import com.calorielens.app.data.nutrition.FoodDatabase
import com.calorielens.app.data.nutrition.healthScore
import com.calorielens.app.domain.ActivityLevel
import com.calorielens.app.domain.CalorieCalculator
import com.calorielens.app.domain.Gender
import com.calorielens.app.domain.Goal
import com.calorielens.app.domain.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodDatabaseTest {

    private fun entryFor(label: String) = FoodDatabase.match(label).entry.name

    @Test
    fun specificDishesBeatGenericCategories() {
        val cases = mapOf(
            "Chicken tikka masala" to "Butter chicken",
            "chicken_curry" to "Chicken curry",
            "Pepperoni pizza" to "Pepperoni pizza",
            "Chocolate cake" to "Chocolate cake",
            "Hyderabadi biryani" to "Biryani",
            "Spaghetti bolognese" to "Spaghetti bolognese",
            "Masala dosa" to "Dosa",
            "Palak paneer" to "Paneer curry",
            "Cheeseburger" to "Cheeseburger",
            "Fish and chips" to "Fish and chips",
            "Donuts" to "Donut",
        )
        cases.forEach { (label, expected) -> assertEquals(label, expected, entryFor(label)) }
    }

    @Test
    fun unknownDishFallsBackToMixedDish() {
        assertEquals(FoodDatabase.fallback.name, entryFor("Zzyzx surprise"))
    }

    @Test
    fun labelsArePrettified() {
        assertEquals("Chicken Curry", FoodDatabase.match("chicken_curry").displayName)
    }

    @Test
    fun caloriesAgreeWithMacros() {
        val alcoholic = setOf("Beer", "Wine", "Cocktail")
        FoodDatabase.entries.filter { it.nutrition.calories > 60 && it.name !in alcoholic }.forEach { e ->
            val n = e.nutrition
            val fromMacros = n.protein * 4 + n.carbs * 4 + n.fat * 9
            assertTrue("${e.name}: ${n.calories} kcal vs $fromMacros from macros", fromMacros in n.calories * 0.6f..n.calories * 1.45f)
            assertTrue(e.name, n.healthScore() in 1..10)
        }
    }

    @Test
    fun targetsRespectSafeMinimum() {
        val p = UserProfile(gender = Gender.FEMALE, age = 70, heightCm = 150, weightKg = 45f, activity = ActivityLevel.SEDENTARY, goal = Goal.LOSE)
        assertTrue(CalorieCalculator.targets(p).calories >= 1200)
        assertTrue(CalorieCalculator.targets(UserProfile()).calories in 2200..2900)
    }
}
