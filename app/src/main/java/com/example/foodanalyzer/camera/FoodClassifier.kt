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
        // Bitmap → ByteBuffer 변환 (INT8이라 byte 단위)
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            inputBuffer.put(((pixel shr 16) and 0xFF).toByte()) // R
            inputBuffer.put(((pixel shr 8) and 0xFF).toByte())  // G
            inputBuffer.put((pixel and 0xFF).toByte())           // B
        }

        // 출력 버퍼 (YOLOv8 output shape 확인 필요)
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputBuffer = Array(1) { Array(outputShape[1]) { FloatArray(outputShape[2]) } }

        inputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)

        // 결과 파싱 (confidence 기준 상위 5개)
        return parseResults(outputBuffer[0])
    }

    private fun parseResults(output: Array<FloatArray>): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        for (i in output.indices) {
            val row = output[i]
            if (row.size < 4 + numClasses) continue

            val classScores = row.slice(4 until 4 + numClasses)
            val maxScore = classScores.max()
            val classIndex = classScores.indexOf(maxScore)

            if (maxScore > 0.3f && classIndex < labels.size) {
                results.add(DetectionResult(labels[classIndex], maxScore))
            }
        }

        return results.sortedByDescending { it.confidence }.take(5)
    }

    fun close() {
        interpreter.close()
    }
}