package search

import com.example.foodanalyzer.R
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tvResult: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var btnSearch: Button
    private lateinit var etFoodInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        tvResult = findViewById(R.id.tvResult)
        btnMic = findViewById(R.id.btnMic)
        btnSearch = findViewById(R.id.btnSearch)
        etFoodInput = findViewById(R.id.etFoodInput)

        checkPermission()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        }

        btnMic.setOnClickListener {
            speechRecognizer.startListening(intent)
            Toast.makeText(this, "말씀하세요!", Toast.LENGTH_SHORT).show()
        }

        btnSearch.setOnClickListener {
            val inputText = etFoodInput.text.toString().trim()
            if (inputText.isEmpty()) {
                Toast.makeText(this, "텍스트를 입력해주세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tvResult.text = "분석 중..."
            callGemini(inputText)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 없음"
                    else -> "다시 시도해주세요 ($error)"
                }
                tvResult.text = "에러 발생: $message"
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    tvResult.text = "인식됨: $recognizedText\n(AI 분석 중...)"
                    callGemini(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun callGemini(inputText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = """
                    너는 식단 분석 전문가야. 다음 문장에서 음식 이름과 수량, 단위만 추출해서 정확히 JSON 배열 형식으로만 대답해줘. 
                    다른 말은 절대 하지 마.
                    형식 예시: [{"food": "사과", "amount": 2, "unit": "개"}, {"food": "우유", "amount": 1, "unit": "잔"}]
                    
                    문장: "$inputText"
                """.trimIndent()

                val apiKey = com.example.foodanalyzer.BuildConfig.GEMINI_API_KEY
                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val body = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().apply {
                        put(org.json.JSONObject().apply {
                            put("parts", org.json.JSONArray().apply {
                                put(org.json.JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }.toString()

                connection.outputStream.write(body.toByteArray())

                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream.bufferedReader().readText()
                }

                android.util.Log.d("Search", "응답 코드: $responseCode")
                android.util.Log.d("Search", "응답: $response")

                val jsonResponse = org.json.JSONObject(response)
                val text = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                withContext(Dispatchers.Main) {
                    tvResult.text = "AI 분석 결과:\n$text"

                    val resultIntent = Intent()
                    resultIntent.putExtra("food_list_json", text)
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvResult.text = "AI 분석 실패: ${e.message}"
                }
            }
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}