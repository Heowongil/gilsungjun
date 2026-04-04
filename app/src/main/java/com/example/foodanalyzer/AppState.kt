package com.example.foodanalyzer

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// ───────────────────────────────────────────────
// 앱 전체 목표값
// ───────────────────────────────────────────────
object AppGoals {
    var calories by mutableStateOf(2000)
    var carbs    by mutableStateOf(250)
    var protein  by mutableStateOf(150)
    var fat      by mutableStateOf(44)
}

// ───────────────────────────────────────────────
// 프로필 사진
// ───────────────────────────────────────────────
object AppProfile {
    var profileImageUri  by mutableStateOf<Uri?>(null)
    var nickname         by mutableStateOf("고순이에요")
    var height           by mutableStateOf("170.0")
    var weight           by mutableStateOf("70.0")
    var age              by mutableStateOf("25")
    var gender           by mutableStateOf("남성")
    var selectedActivity by mutableStateOf("주 3-5회 운동")
    var selectedGoal     by mutableStateOf("유지")
}

// ───────────────────────────────────────────────
// 식단 기록 데이터
// ───────────────────────────────────────────────
object AppMealData {
    // key: "날짜_식사종류" → 사진 URI
    val photoMap      = mutableStateMapOf<String, Uri>()
    // key: "날짜_식사종류" → 기록 완료된 결과
    val mealResultMap = mutableStateMapOf<String, MealResult>()

    // ── 날짜별 합산 영양소 계산 ──
    // key: "날짜" (예: "2026-04-03") → DayNutrition
    fun getDayNutrition(date: String): DayNutrition {
        var totalKcal    = 0
        var totalCarbs   = 0
        var totalProtein = 0
        var totalFat     = 0

        // 해당 날짜의 모든 식사 결과 합산
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

// ───────────────────────────────────────────────
// 날짜별 영양소 데이터 모델
// ───────────────────────────────────────────────
data class DayNutrition(
    val calories: Int = 0,
    val carbs: Int    = 0,
    val protein: Int  = 0,
    val fat: Int      = 0
)