package com.example.foodanalyzer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_log")
data class DailyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,           // 날짜 (예: "2026-04-03")
    val foodId: Int,            // Food 테이블의 id
    val foodName: String,       // 음식명 (표시용)
    val weightG: Double,        // 실제 섭취 중량 (g)
    val calories: Double,       // 계산된 칼로리
    val carb: Double,           // 계산된 탄수화물
    val protein: Double,        // 계산된 단백질
    val fat: Double,            // 계산된 지방
    val mealType: String,       // 식사 구분 (아침/점심/저녁/간식)
    val createdAt: Long = System.currentTimeMillis()  // 기록 시간
)