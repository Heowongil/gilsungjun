package com.example.foodanalyzer.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.foodanalyzer.R // ⚠️ 본인 프로젝트명에 맞게 빨간줄 뜨면 Alt+Enter로 임포트!

class CameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView

    // 1. 스마트폰 카메라 사용 권한을 사용자에게 물어보는 창 띄우기
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera() // 권한 허락받으면 카메라 켬
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // 아까 XML에서 만든 PreviewView를 찾아옴
        viewFinder = findViewById(R.id.viewFinder)

        // 2. 권한이 있는지 확인하고 없으면 요청, 있으면 바로 카메라 켬
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 3. 실제 CameraX 라이브러리를 사용해 화면에 띄우는 함수
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // 프리뷰(미리보기) 화면 세팅
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // 후면 카메라 기본 설정
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 기존에 연결된 게 있으면 다 풀고 새로 연결 (수명주기 동기화)
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch(exc: Exception) {
                Log.e("CameraActivity", "카메라 바인딩 실패", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
}
