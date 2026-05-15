package com.example.foodanalyzer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppGoals {
    var calories by mutableStateOf(2000)
    var carbs    by mutableStateOf(250)
    var protein  by mutableStateOf(150)
    var fat      by mutableStateOf(44)

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("app_goals", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("calories", calories)
            .putInt("carbs", carbs)
            .putInt("protein", protein)
            .putInt("fat", fat)
            .apply()
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("app_goals", Context.MODE_PRIVATE)
        calories = prefs.getInt("calories", 2000)
        carbs    = prefs.getInt("carbs", 250)
        protein  = prefs.getInt("protein", 150)
        fat      = prefs.getInt("fat", 44)
    }
}

object AppProfile {
    var profileImageUri  by mutableStateOf<Uri?>(null)
    var nickname         by mutableStateOf("사용자")
    var height           by mutableStateOf("170.0")
    var weight           by mutableStateOf("65.0")
    var age              by mutableStateOf("25")
    var gender           by mutableStateOf("남성")
    var selectedActivity by mutableStateOf("주 3-5회 운동")
    var selectedGoal     by mutableStateOf("유지")

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("app_profile", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("nickname", nickname)
            .putString("height", height)
            .putString("weight", weight)
            .putString("age", age)
            .putString("gender", gender)
            .putString("selectedActivity", selectedActivity)
            .putString("selectedGoal", selectedGoal)
            .putString("profileImageUri", profileImageUri?.toString())
            .apply()
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("app_profile", Context.MODE_PRIVATE)
        nickname         = prefs.getString("nickname", "고순이에요") ?: "고순이에요"
        height           = prefs.getString("height", "170.0") ?: "170.0"
        weight           = prefs.getString("weight", "70.0") ?: "70.0"
        age              = prefs.getString("age", "25") ?: "25"
        gender           = prefs.getString("gender", "남성") ?: "남성"
        selectedActivity = prefs.getString("selectedActivity", "주 3-5회 운동") ?: "주 3-5회 운동"
        selectedGoal     = prefs.getString("selectedGoal", "유지") ?: "유지"
        val uriString    = prefs.getString("profileImageUri", null)
        profileImageUri  = if (uriString != null) Uri.parse(uriString) else null
    }
}

object AppMealData {
    val photoMap      = mutableStateMapOf<String, Uri>()
    val mealResultMap = mutableStateMapOf<String, MealResult>()

    fun getDayNutrition(date: String): DayNutrition {
        var totalKcal    = 0
        var totalCarbs   = 0
        var totalProtein = 0
        var totalFat     = 0

        mealResultMap.entries
            .filter { it.key.startsWith(date) }
            .forEach { entry ->
                totalKcal    += entry.value.totalKcal
                totalCarbs   += entry.value.totalCarbs
                totalProtein += entry.value.totalProtein
                totalFat     += entry.value.totalFat
            }

        return DayNutrition(
            calories = totalKcal,
            carbs    = totalCarbs,
            protein  = totalProtein,
            fat      = totalFat
        )
    }
}

data class DayNutrition(
    val calories: Int = 0,
    val carbs: Int    = 0,
    val protein: Int  = 0,
    val fat: Int      = 0
)