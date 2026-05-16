package com.example.foodanalyzer.data

import com.example.foodanalyzer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiNutritionService {

    data class NutritionInfo(
        val foodName: String,
        val kcal: Int,
        val carbs: Int,
        val protein: Int,
        val fat: Int
    )

    // 여러 음식을 한 번에 조회
    suspend fun getNutritionList(foodNames: List<String>): List<NutritionInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val foodList = foodNames.joinToString(", ")
                val prompt = """
    너는 영양성분 전문가야. 아래 음식들의 100g 기준 영양성분을 JSON 배열 형식으로만 대답해줘.
    다른 말은 절대 하지 마. JSON 배열만 출력해.
    모든 숫자는 반드시 0보다 큰 정수로 입력해. 0은 절대 사용하지 마.
    형식: [{"foodName": "한글음식명", "kcal": 숫자, "carbs": 숫자, "protein": 숫자, "fat": 숫자}, ...]
    음식 목록: $foodList
""".trimIndent()

                val body = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }.toString()

                connection.outputStream.write(body.toByteArray())

                val responseCode = connection.responseCode
                android.util.Log.d("Gemini", "응답 코드: $responseCode")

                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    val error = connection.errorStream.bufferedReader().readText()
                    android.util.Log.e("Gemini", "에러: $error")
                    return@withContext emptyList()
                }

                val jsonResponse = JSONObject(response)
                val rawText = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                android.util.Log.d("Gemini", "원본 응답: $rawText")

                val cleaned = rawText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val jsonArray = JSONArray(cleaned)
                val results = mutableListOf<NutritionInfo>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    results.add(NutritionInfo(
                        foodName = obj.getString("foodName"),
                        kcal     = obj.getInt("kcal"),
                        carbs    = obj.getInt("carbs"),
                        protein  = obj.getInt("protein"),
                        fat      = obj.getInt("fat")
                    ))
                }
                results

            } catch (e: Exception) {
                android.util.Log.e("Gemini", "영양성분 조회 실패: ${e.message}")
                emptyList()
            }
        }
    }
}