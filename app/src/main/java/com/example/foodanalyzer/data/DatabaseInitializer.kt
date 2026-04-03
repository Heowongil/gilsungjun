package com.example.foodanalyzer.data

import android.content.Context
import com.example.foodanalyzer.data.entity.Food
import java.io.BufferedReader
import java.io.InputStreamReader

object DatabaseInitializer {

    suspend fun initializeIfNeeded(context: Context, database: AppDatabase) {
        val foodDao = database.foodDao()

        // 이미 데이터가 있으면 스킵
        val existingFoods = foodDao.getAll()
        if (existingFoods.isNotEmpty()) return

        // CSV 파일 읽어서 DB에 넣기
        val foods = mutableListOf<Food>()
        val inputStream = context.assets.open("foods.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))

        // 첫 줄(헤더) 건너뛰기
        reader.readLine()

        reader.forEachLine { line ->
            val columns = line.split(",")
            if (columns.size >= 6) {
                foods.add(
                    Food(
                        name = columns[0],
                        calories = columns[1].toDouble(),
                        carb = columns[2].toDouble(),
                        protein = columns[3].toDouble(),
                        fat = columns[4].toDouble(),
                        avgWeightG = columns[5].toDouble()
                    )
                )
            }
        }

        reader.close()
        foodDao.insertAll(foods)
    }
}