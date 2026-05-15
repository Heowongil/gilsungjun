package com.example.foodanalyzer.camera

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class FoodClassifier(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputSize = 512
    private val numClasses: Int // 나중에 100개로 학습 다시 하시면 100으로 수정해야 합니다

    init {
        // FP16 모델로 파일명 변경
        val assetFileDescriptor = context.assets.openFd("best_float16.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val mappedBuffer = fileInputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
        interpreter = Interpreter(mappedBuffer)

        labels = context.assets.open("labels.txt").bufferedReader().readLines()
        numClasses = labels.size // 💡 추가! (labels.txt의 줄 수인 75를 자동으로 읽어옵니다)
    }

    data class DetectionResult(
        val label: String,
        val confidence: Float
    )

    fun classify(bitmap: Bitmap): List<DetectionResult> {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputBuffer = Array(1) { Array(outputShape[1]) { FloatArray(outputShape[2]) } }

        inputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)

        android.util.Log.d("TFLite", "outputShape: ${outputShape.toList()}")

        return parseResults(outputBuffer[0])
    }

    private fun parseResults(output: Array<FloatArray>): List<DetectionResult> {
        val numAnchors = output[0].size
        val results = mutableListOf<DetectionResult>()

        // 겹치는 박스 제거 로직(NMS)이 없으므로, 일단 클래스별 최고점수만 담는 맵 사용
        val bestScores = mutableMapOf<Int, Float>()

        for (anchor in 0 until numAnchors) {
            var maxScore = 0f
            var maxClass = -1

            for (c in 0 until numClasses) {
                val score = output[4 + c][anchor]
                if (score > maxScore) {
                    maxScore = score
                    maxClass = c
                }
            }

            // 신뢰도 25% 이상인 것만 취급
            if (maxScore > 0.25f && maxClass != -1) {
                val currentBest = bestScores[maxClass] ?: 0f
                if (maxScore > currentBest) {
                    bestScores[maxClass] = maxScore
                }
            }
        }

        bestScores.forEach { (classIdx, score) ->
            if (classIdx < labels.size) {
                results.add(DetectionResult(labels[classIdx], score))
            }
        }

        val sorted = results.sortedByDescending { it.confidence }.take(5)
        sorted.forEach { android.util.Log.d("TFLite", "${it.label}: ${it.confidence}") }

        return sorted
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        val labelToKorean = mapOf(
            "Doenjangjjigae" to "된장찌개",
            "Gamjatang" to "감자탕",
            "Jajangmyeon" to "짜장면",
            "Yeongeun_jorim" to "연근조림",
            "Kimchi" to "김치",
            "Hamburger" to "햄버거",
            "Japchae" to "잡채",
            "Kimchijjigae" to "김치찌개",
            "Galbijjim" to "갈비찜",
            "Yukhoe" to "육회",
            "Mapodubu" to "마파두부",
            "Pasta" to "파스타",
            "Pizza" to "피자",
            "Tonkatsu" to "돈까스",
            "Janchiguksu" to "잔치국수",
            "Kongnamulguk" to "콩나물국",
            "Seolleongtang" to "설렁탕",
            "Curry" to "카레",
            "bibimbap" to "비빔밥",
            "Naengmyeon" to "냉면",
            "Odengguk" to "어묵탕",
            "Burrito" to "부리또",
            "buchimgae" to "부침개",
            "budaejjigae" to "부대찌개",
            "Cake" to "케이크",
            "Chicken_mayo" to "치킨마요",
            "Fried_chicken" to "후라이드치킨",
            "Sandwich" to "샌드위치",
            "Cup_bap" to "컵밥",
            "dakgalbi" to "닭갈비",
            "donggeurangttaeng" to "동그랑땡",
            "Bugeoguk" to "북어국",
            "dubujorim" to "두부조림",
            "Fried_egg" to "계란후라이",
            "Steak" to "스테이크",
            "French_fries" to "감자튀김",
            "Bokkeumbap" to "볶음밥",
            "ganjang-gejang" to "간장게장",
            "Gimbap" to "김밥",
            "Salad" to "샐러드",
            "Godeungeo_gui" to "고등어구이",
            "Gyeran_jjim" to "계란찜",
            "Hamburger_steak" to "함박스테이크",
            "Hot_dog" to "핫도그",
            "Ice_cream" to "아이스크림",
            "Jangjorim" to "장조림",
            "Jeyuk_bokkeum" to "제육볶음",
            "jjambbong" to "짬뽕",
            "kalguksu" to "칼국수",
            "Kkongchi_jorim" to "꽁치조림",
            "Gim" to "김",
            "Soba" to "소바",
            "Misoguk" to "미소국",
            "Pancakes" to "팬케이크",
            "Jokbal" to "족발",
            "Ramyun" to "라면",
            "Bap" to "밥",
            "Jumeokbap" to "주먹밥",
            "Risotto" to "리조또",
            "Gyeran_mari" to "계란말이",
            "samgyeopsal" to "삼겹살",
            "Sashimi" to "회",
            "Sausage" to "소시지",
            "Kongnamul_muchim" to "콩나물무침",
            "Yukgaejang" to "육개장",
            "Mandu" to "만두",
            "Godeungeo_jorim" to "고등어조림",
            "Myeolchi_bokkeum" to "멸치볶음",
            "Sundae" to "순대",
            "sushi" to "초밥",
            "Tangsuyuk" to "탕수육",
            "takoyaki" to "타코야키",
            "Toast" to "토스트",
            "Tteokbokki" to "떡볶이",
            "Waffles" to "와플"
        )
    }
}
// 💡 75개 정예 멤버에 맞춘 영어 -> 한글 번역기
