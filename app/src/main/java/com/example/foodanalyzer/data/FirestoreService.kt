package com.example.foodanalyzer.data

import com.example.foodanalyzer.AppGoals
import com.example.foodanalyzer.AppProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.foodanalyzer.data.entity.DailyLog
object FirestoreService {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun backup(context: android.content.Context) {
        val uid = auth.currentUser?.uid ?: return
        val dao = AppDatabase.getInstance(context).dailyLogDao()
        val logs = dao.getAllLogs()

        val batch = db.batch()
        val userRef = db.collection("users").document(uid)

        // ── 식단 데이터 백업 ──
        val logsRef = userRef.collection("dailyLogs")
        val existing = logsRef.get().await()
        existing.documents.forEach { batch.delete(it.reference) }

        logs.forEach { log ->
            val docRef = logsRef.document()
            val data = mapOf(
                "date"      to log.date,
                "foodId"    to log.foodId,
                "foodName"  to log.foodName,
                "weightG"   to log.weightG,
                "calories"  to log.calories,
                "carb"      to log.carb,
                "protein"   to log.protein,
                "fat"       to log.fat,
                "mealType"  to log.mealType
            )
            batch.set(docRef, data)
        }

        // ── 프로필 백업 ──
        val profileData = mapOf(
            "nickname"         to AppProfile.nickname,
            "height"           to AppProfile.height,
            "weight"           to AppProfile.weight,
            "age"              to AppProfile.age,
            "gender"           to AppProfile.gender,
            "selectedActivity" to AppProfile.selectedActivity,
            "selectedGoal"     to AppProfile.selectedGoal
        )
        batch.set(userRef.collection("profile").document("data"), profileData)

        // ── 목표 백업 ──
        val goalsData = mapOf(
            "calories" to AppGoals.calories,
            "carbs"    to AppGoals.carbs,
            "protein"  to AppGoals.protein,
            "fat"      to AppGoals.fat
        )
        batch.set(userRef.collection("goals").document("data"), goalsData)

        batch.commit().await()
    }

    suspend fun restore(context: android.content.Context) {
        val uid = auth.currentUser?.uid ?: return
        val dao = AppDatabase.getInstance(context).dailyLogDao()

        // ── 식단 데이터 복원 ──
        val snapshot = db.collection("users").document(uid)
            .collection("dailyLogs").get().await()

        dao.deleteAll()

        snapshot.documents.forEach { doc ->
            val log = DailyLog(
                date     = doc.getString("date") ?: return@forEach,
                foodId   = doc.getLong("foodId")?.toInt() ?: 0,
                foodName = doc.getString("foodName") ?: return@forEach,
                weightG  = doc.getDouble("weightG") ?: 0.0,
                calories = doc.getDouble("calories") ?: 0.0,
                carb     = doc.getDouble("carb") ?: 0.0,
                protein  = doc.getDouble("protein") ?: 0.0,
                fat      = doc.getDouble("fat") ?: 0.0,
                mealType = doc.getString("mealType") ?: "기타"
            )
            dao.insert(log)
        }

        // ── 프로필 복원 ──
        val profileDoc = db.collection("users").document(uid)
            .collection("profile").document("data").get().await()

        if (profileDoc.exists()) {
            AppProfile.nickname         = profileDoc.getString("nickname") ?: "사용자"
            AppProfile.height           = profileDoc.getString("height") ?: "170.0"
            AppProfile.weight           = profileDoc.getString("weight") ?: "65.0"
            AppProfile.age              = profileDoc.getString("age") ?: "25"
            AppProfile.gender           = profileDoc.getString("gender") ?: "남성"
            AppProfile.selectedActivity = profileDoc.getString("selectedActivity") ?: "주 3-5회 운동"
            AppProfile.selectedGoal     = profileDoc.getString("selectedGoal") ?: "유지"
            AppProfile.save(context)
        }

        // ── 목표 복원 ──
        val goalsDoc = db.collection("users").document(uid)
            .collection("goals").document("data").get().await()

        if (goalsDoc.exists()) {
            AppGoals.calories = goalsDoc.getLong("calories")?.toInt() ?: 2000
            AppGoals.carbs    = goalsDoc.getLong("carbs")?.toInt() ?: 250
            AppGoals.protein  = goalsDoc.getLong("protein")?.toInt() ?: 150
            AppGoals.fat      = goalsDoc.getLong("fat")?.toInt() ?: 44
            AppGoals.save(context)
        }
    }
}