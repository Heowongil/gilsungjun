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
    private val numClasses = 148

    init {
        // 모델 로드
        val assetFileDescriptor = context.assets.openFd("best_int8.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val mappedBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
        interpreter = Interpreter(mappedBuffer)

        // 라벨 로드
        labels = context.assets.open("labels.txt")
            .bufferedReader()
            .readLines()
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
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)           // B
        }

        val outputShape = interpreter.getOutputTensor(0).shape()
        android.util.Log.d("TFLite", "output shape: ${outputShape.toList()}")

        // shape: [1, 152, 5376] → [배치, 4+클래스, 앵커]
        val outputBuffer = Array(1) { Array(outputShape[1]) { FloatArray(outputShape[2]) } }

        inputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)

        // transpose해서 파싱: [앵커, 4+클래스] 형태로 변환
        return parseResultsTransposed(outputBuffer[0])
    }

    private fun parseResultsTransposed(output: Array<FloatArray>): List<DetectionResult> {
        // output: [152, 5376] → 152=4+148클래스, 5376=앵커수
        val numAnchors = output[0].size  // 5376
        val results = mutableListOf<DetectionResult>()

        for (anchor in 0 until numAnchors) {
            // 클래스 스코어: index 4~151
            var maxScore = 0f
            var maxClass = 0
            for (c in 0 until numClasses) {
                val score = output[4 + c][anchor]
                if (score > maxScore) {
                    maxScore = score
                    maxClass = c
                }
            }

            if (maxScore > 0.01f && maxClass < labels.size) {
                results.add(DetectionResult(labels[maxClass], maxScore))
            }
        }

        val sorted = results.sortedByDescending { it.confidence }.take(5)
        android.util.Log.d("TFLite", "=== 파싱 결과 ===")
        android.util.Log.d("TFLite", "후보 개수: ${results.size}")
        sorted.forEach { android.util.Log.d("TFLite", "  ${it.label}: ${it.confidence}") }
        return sorted
    }

    fun close() {
        interpreter.close()
    }
}