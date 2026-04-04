package com.example.foodanalyzer.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

// View를 상속받는 나만의 투명 도화지 클래스
class BoundingBoxOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // 빨간색 테두리 펜(Paint) 준비
    private val boxPaint = Paint().apply {
        color = Color.RED           // 색상은 빨간색
        style = Paint.Style.STROKE  // 채우지 말고 테두리만!
        strokeWidth = 10f           // 선 두께
    }

    // 화면이 그려질 때마다 안드로이드가 자동으로 호출하는 함수 (C언어의 렌더링 루프 같은 역할)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 임시 가짜 좌표 계산 (화면 한가운데 적당한 크기)
        val left = width / 4f
        val top = height / 3f
        val right = width * 3 / 4f
        val bottom = height * 2 / 3f

        // 2. 네모 박스 영역 설정
        val rect = RectF(left, top, right, bottom)

        // 3. 도화지(Canvas)에 준비한 빨간펜으로 네모를 그려라!
        canvas.drawRect(rect, boxPaint)
    }
}