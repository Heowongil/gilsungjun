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
}
// 💡 75개 정예 멤버에 맞춘 영어 -> 한글 번역기
