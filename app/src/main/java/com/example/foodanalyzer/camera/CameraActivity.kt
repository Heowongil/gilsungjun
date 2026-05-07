package com.example.foodanalyzer.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.foodanalyzer.R
import android.widget.Button
import java.io.File

class CameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private var imageCapture: ImageCapture? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)

        // 촬영 버튼 클릭 시 사진 찍기
        findViewById<Button>(R.id.captureButton).setOnClickListener {
            takePhoto()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraActivity", "카메라 바인딩 실패", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // 저장할 파일 경로 설정
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            "food_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    // 촬영된 사진 URI를 AnalysisScreen으로 돌려주기
                    val resultIntent = android.content.Intent()
                    resultIntent.putExtra("photo_uri", savedUri.toString())
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraActivity", "촬영 실패", exc)
                    Toast.makeText(this@CameraActivity, "촬영에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}