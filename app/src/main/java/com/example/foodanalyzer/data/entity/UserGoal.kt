package com.example.foodanalyzer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_goal")
data class UserGoal(
    @PrimaryKey
    val id: Int = 1,            // 항상 1개만 존재
    val targetCalories: Double = 2000.0,
    val targetCarb: Double = 300.0,
    val targetProtein: Double = 60.0,
    val targetFat: Double = 65.0
)