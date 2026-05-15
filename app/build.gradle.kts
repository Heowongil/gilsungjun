import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"}

android {
    namespace = "com.example.foodanalyzer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.foodanalyzer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"



        val localProperties = Properties()
        localProperties.load(rootProject.file("local.properties").inputStream())
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties["GEMINI_API_KEY"]}\"")

        buildConfigField("String", "FOOD_SAFETY_API_KEY", "\"${localProperties["FOOD_SAFETY_API_KEY"]}\"")
        buildConfigField("String", "FOOD_BARCODE_API_KEY", "\"${localProperties["FOOD_BARCODE_API_KEY"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
// TFLite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- 팀원들이 수정한 부분 + B님의 버전 수정 합침 ---
    implementation("androidx.compose.material:material-icons-extended:1.6.0") // B님이 고친 버전 유지!

    // CameraX 기본 및 카메라2 기능 (팀원 코드)
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    // CameraX 라이프사이클 관리
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    // CameraX 뷰 (PreviewView 등)
    implementation("androidx.camera:camera-view:${camerax_version}")

    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // --- B님이 추가한 부분 (AI 기능) ---
    // Gemini AI 라이브러리
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    // 비동기 작업(코루틴) 라이브러리
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(libs.androidx.appcompat)

    // ML Kit 바코드 스캔
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}