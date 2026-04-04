package com.example.foodanalyzer.data.dao

import androidx.room.*
import com.example.foodanalyzer.data.entity.DailyLog

@Dao
interface DailyLogDao {
    // 특정 날짜의 기록 조회
    @Query("SELECT * FROM daily_log WHERE date = :date ORDER BY createdAt")
    suspend fun getByDate(date: String): List<DailyLog>

    // 특정 날짜의 총 칼로리
    @Query("SELECT SUM(calories) FROM daily_log WHERE date = :date")
    suspend fun getTotalCalories(date: String): Double?

    // 기록 추가
    @Insert
    suspend fun insert(log: DailyLog)

    // 기록 삭제
    @Delete
    suspend fun delete(log: DailyLog)
}