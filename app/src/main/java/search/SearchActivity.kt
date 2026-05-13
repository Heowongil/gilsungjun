package search // 폴더 위치가 java/search 라면 이대로 유지

import com.example.foodanalyzer.R // 본인 패키지명에 맞게 유지해주세요
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
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tvResult: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var etFoodInput: EditText
    private lateinit var btnTextSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // UI 요소 연결
        tvResult = findViewById(R.id.tvResult)
        btnMic = findViewById(R.id.btnMic)
        etFoodInput = findViewById(R.id.etFoodInput)
        btnTextSubmit = findViewById(R.id.btnTextSubmit)

        // 1. 마이크 권한 체크
        checkPermission()

        // 2. SpeechRecognizer 초기화
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR") // 한국어 설정
        }

        // 3. 마이크 버튼 클릭 시 음성 인식 시작
        btnMic.setOnClickListener {
            speechRecognizer.startListening(intent)
            Toast.makeText(this, "말씀하세요!", Toast.LENGTH_SHORT).show()
        }

        // 4. 텍스트 검색 버튼 클릭 시 처리
        btnTextSubmit.setOnClickListener {
            val inputText = etFoodInput.text.toString()
            if (inputText.isNotEmpty()) {
                tvResult.text = "입력됨: $inputText\n(AI 분석 중...)"
                analyzeDietWithAI(inputText) // 텍스트로 AI 분석 요청
                etFoodInput.text.clear() // 전송 후 입력창 비워주기
            } else {
                Toast.makeText(this, "식단을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. 음성 인식 결과 처리 리스너
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
                    val recognizedText = matches[0] // 사용자가 말한 텍스트
                    tvResult.text = "인식됨: $recognizedText\n(AI 분석 중...)"

                    analyzeDietWithAI(recognizedText) // 음성으로 AI 분석 요청
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // --- Gemini AI 분석을 처리하는 공통 함수 ---
    private fun analyzeDietWithAI(inputText: String) {
        // 💡 주의: 실제 앱을 스토어에 출시할 때는 API 키가 코드에 직접 노출되지 않도록 BuildConfig나 서버를 통해 관리하는 것이 안전합니다.
        val apiKey = "AIzaSyClZuevqVBGsKBu8SxpgMG8K4ZgmQtZZVk"

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )

        val prompt = """
            너는 식단 분석 전문가야. 다음 문장에서 음식 이름과 수량, 단위만 추출해서 정확히 JSON 배열 형식으로만 대답해줘. 
            다른 말은 절대 하지 마.
            형식 예시: [{"food": "사과", "amount": 2, "unit": "개"}, {"food": "우유", "amount": 1, "unit": "잔"}]
            
            문장: "$inputText"
        """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(prompt)
                withContext(Dispatchers.Main) {
                    tvResult.text = "AI 분석 결과:\n${response.text}"
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