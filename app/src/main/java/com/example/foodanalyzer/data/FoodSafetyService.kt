package com.example.foodanalyzer.data

import com.example.foodanalyzer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FoodSafetyService {

    private val API_KEY = BuildConfig.FOOD_BARCODE_API_KEY

    data class BarcodeFood(
        val foodName: String,
        val kcal: Double,
        val carbs: Double,
        val protein: Double,
        val fat: Double,
        val servingSize: Double
    )

    suspend fun getFoodByBarcode(barcode: String): BarcodeFood? {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "http://openapi.foodsafetykorea.go.kr/api/$API_KEY/C005/json/1/5/BAR_CD=$barcode"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                android.util.Log.d("FoodSafety", "응답 코드: $responseCode")

                if (responseCode != 200) {
                    android.util.Log.e("FoodSafety", "에러 응답 코드: $responseCode")
                    return@withContext null
                }

                val response = connection.inputStream.bufferedReader().readText()
                android.util.Log.d("FoodSafety", "응답: $response")

                val json = JSONObject(response)
                val c005 = json.optJSONObject("C005") ?: return@withContext null
                val row = c005.optJSONArray("row") ?: return@withContext null

                if (row.length() == 0) return@withContext null

                val item = row.getJSONObject(0)
                BarcodeFood(
                    foodName    = item.optString("PRDLST_NM", "알 수 없음"),
                    kcal        = item.optString("NUTR_CONT1", "0").toDoubleOrNull() ?: 0.0,
                    carbs       = item.optString("NUTR_CONT2", "0").toDoubleOrNull() ?: 0.0,
                    protein     = item.optString("NUTR_CONT3", "0").toDoubleOrNull() ?: 0.0,
                    fat         = item.optString("NUTR_CONT4", "0").toDoubleOrNull() ?: 0.0,
                    servingSize = item.optString("SERVING_SIZE", "100").toDoubleOrNull() ?: 100.0
                )
            } catch (e: Exception) {
                android.util.Log.e("FoodSafety", "조회 실패: ${e.message}")
                null
            }
        }
    }
}