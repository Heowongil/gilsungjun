package com.example.foodanalyzer.data.dao

import androidx.room.*
import com.example.foodanalyzer.data.entity.UserGoal

@Dao
interface UserGoalDao {
    // 목표 조회
    @Query("SELECT * FROM user_goal WHERE id = 1")
    suspend fun getGoal(): UserGoal?

    // 목표 저장/수정
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: UserGoal)
}