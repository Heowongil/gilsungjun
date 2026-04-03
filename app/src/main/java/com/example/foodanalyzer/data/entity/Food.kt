package com.example.foodanalyzer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food")
data class Food(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,           // 음식명 (예: 김치볶음밥)
    val calories: Double,       // 칼로리 (kcal)
    val carb: Double,           // 탄수화물 (g)
    val protein: Double,        // 단백질 (g)
    val fat: Double,            // 지방 (g)
    val avgWeightG: Double      // 1인분 평균 중량 (g)
)