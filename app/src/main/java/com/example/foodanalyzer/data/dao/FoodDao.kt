package com.example.foodanalyzer.data.dao

import androidx.room.*
import com.example.foodanalyzer.data.entity.Food

@Dao
interface FoodDao {
    // 음식 이름으로 검색
    @Query("SELECT * FROM food WHERE name LIKE '%' || :keyword || '%'")
    suspend fun searchByName(keyword: String): List<Food>

    // 전체 음식 목록
    @Query("SELECT * FROM food ORDER BY name")
    suspend fun getAll(): List<Food>

    // ID로 조회
    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Int): Food?

    // 음식 추가 (CSV 데이터 일괄 입력용)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<Food>)
}