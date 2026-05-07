package com.example.foodanalyzer.data

import android.content.Context

class FoodRepository(context: Context) {
    private val foodDao = AppDatabase.getInstance(context).foodDao()

    suspend fun searchFood(keyword: String): List<com.example.foodanalyzer.data.entity.Food> {
        return foodDao.searchByName(keyword)
    }

    suspend fun getAllFoods(): List<com.example.foodanalyzer.data.entity.Food> {
        return foodDao.getAll()
    }
}