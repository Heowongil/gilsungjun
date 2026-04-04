package com.example.foodanalyzer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foodanalyzer.data.dao.DailyLogDao
import com.example.foodanalyzer.data.dao.FoodDao
import com.example.foodanalyzer.data.dao.UserGoalDao
import com.example.foodanalyzer.data.entity.DailyLog
import com.example.foodanalyzer.data.entity.Food
import com.example.foodanalyzer.data.entity.UserGoal

@Database(
    entities = [Food::class, DailyLog::class, UserGoal::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun userGoalDao(): UserGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food_analyzer_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}