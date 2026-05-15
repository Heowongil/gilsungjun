package com.example.foodanalyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // 기본 정보
    var nickname by remember { mutableStateOf("") }
    var height   by remember { mutableStateOf("") }
    var weight   by remember { mutableStateOf("") }
    var age      by remember { mutableStateOf("") }
    var gender   by remember { mutableStateOf("남성") }
    var genderExpanded by remember { mutableStateOf(false) }

    // 목표 설정
    val activityOptions = listOf("운동 거의 안함", "주 1-3회 운동", "주 3-5회 운동", "주 6-7회 운동", "매일, 하루 2번")
    var selectedActivity by remember { mutableStateOf("주 3-5회 운동") }
    val goalOptions = listOf("체중 감소", "유지", "근육량 증가")
    var selectedGoal by remember { mutableStateOf("유지") }

    // 추천 칼로리 계산
    val recommendedKcal = remember(height, weight, age, gender, selectedActivity, selectedGoal) {
        val h = height.toDoubleOrNull() ?: 170.0
        val w = weight.toDoubleOrNull() ?: 65.0
        val a = age.toIntOrNull() ?: 25
        val bmr = if (gender == "남성") {
            88.362 + (13.397 * w) + (4.799 * h) - (5.677 * a)
        } else {
            447.593 + (9.247 * w) + (3.098 * h) - (4.330 * a)
        }
        val activityFactor = when (selectedActivity) {
            "운동 거의 안함"  -> 1.2
            "주 1-3회 운동"  -> 1.375
            "주 3-5회 운동"  -> 1.55
            "주 6-7회 운동"  -> 1.725
            "매일, 하루 2번" -> 1.9
            else             -> 1.55
        }
        val goalFactor = when (selectedGoal) {
            "체중 감소"   -> 0.85
            "근육량 증가" -> 1.1
            else          -> 1.0
        }
        (bmr * activityFactor * goalFactor).toInt()
    }

    // 추천값 기반 목표 영양소
    var targetKcal    by remember(recommendedKcal) { mutableStateOf(recommendedKcal.toString()) }
    var targetCarbs   by remember(recommendedKcal) { mutableStateOf((recommendedKcal * 0.5 / 4).toInt().toString()) }
    var targetProtein by remember(recommendedKcal) { mutableStateOf((recommendedKcal * 0.25 / 4).toInt().toString()) }
    var targetFat     by remember(recommendedKcal) { mutableStateOf((recommendedKcal * 0.25 / 9).toInt().toString()) }

    val genderOptions = listOf("남성", "여성")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4E8))
    ) {
        // 상단 진행 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (page > 0) {
                IconButton(
                    onClick = { page-- },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                }
            }
            Text(
                "${page + 1} / 3",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 진행 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index <= page) Color(0xFF4CAF50)
                            else Color(0xFFDDDDDD)
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (page) {
                // ── 1페이지: 기본 정보 ──
                0 -> {
                    Text("기본 정보를 입력해주세요", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Text("정확한 영양 목표 계산을 위해 필요해요", fontSize = 14.sp, color = Color(0xFF888888))

                    OnboardingField(label = "닉네임") {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            placeholder = { Text("이름을 입력하세요") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OnboardingField(label = "키", modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = height,
                                onValueChange = { height = it },
                                placeholder = { Text("170") },
                                suffix = { Text("cm") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                        OnboardingField(label = "몸무게", modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                placeholder = { Text("65") },
                                suffix = { Text("kg") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OnboardingField(label = "나이", modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it },
                                placeholder = { Text("25") },
                                suffix = { Text("세") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                        OnboardingField(label = "성별", modifier = Modifier.weight(1f)) {
                            Box {
                                OutlinedTextField(
                                    value = gender,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                                            modifier = Modifier.clickable { genderExpanded = true })
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = genderExpanded,
                                    onDismissRequest = { genderExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    genderOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = { gender = option; genderExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 2페이지: 활동량 & 목표 ──
                1 -> {
                    Text("활동량과 목표를 선택해주세요", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Text("맞춤 칼로리를 계산해드릴게요", fontSize = 14.sp, color = Color(0xFF888888))

                    Text("나의 활동량", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                    val activityRows = activityOptions.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activityRows.forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { option ->
                                    val isSelected = option == selectedActivity
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) Color(0xFF1A1A1A) else Color(0xFFF0F0F0))
                                            .clickable { selectedActivity = option }
                                            .padding(vertical = 10.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFF444444),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }

                    Text("나의 목표", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        goalOptions.forEach { option ->
                            val isSelected = option == selectedGoal
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFF1A1A1A) else Color(0xFFF0F0F0))
                                    .clickable { selectedGoal = option }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF444444)
                                )
                            }
                        }
                    }
                }

                // ── 3페이지: 목표 영양소 확인 ──
                2 -> {
                    Text("목표 영양소를 확인해주세요", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Text("입력한 정보를 기반으로 계산했어요", fontSize = 14.sp, color = Color(0xFF888888))

                    // 추천 칼로리 카드
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD6F0D6))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("추천 일일 칼로리", fontSize = 13.sp, color = Color(0xFF4CAF50))
                            Text("$recommendedKcal kcal", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }

                    Text("목표 영양소 (수정 가능)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OnboardingNutrientField("목표 칼로리", targetKcal, "kcal") { targetKcal = it }
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                            OnboardingNutrientField("목표 탄수화물", targetCarbs, "g") { targetCarbs = it }
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                            OnboardingNutrientField("목표 단백질", targetProtein, "g") { targetProtein = it }
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                            OnboardingNutrientField("목표 지방", targetFat, "g") { targetFat = it }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (page < 2) {
                        page++
                    } else {
                        // 저장
                        AppProfile.nickname         = nickname.ifBlank { "사용자" }
                        AppProfile.height           = height.ifBlank { "170.0" }
                        AppProfile.weight           = weight.ifBlank { "65.0" }
                        AppProfile.age              = age.ifBlank { "25" }
                        AppProfile.gender           = gender
                        AppProfile.selectedActivity = selectedActivity
                        AppProfile.selectedGoal     = selectedGoal
                        AppGoals.calories           = targetKcal.toIntOrNull() ?: recommendedKcal
                        AppGoals.carbs              = targetCarbs.toIntOrNull() ?: 250
                        AppGoals.protein            = targetProtein.toIntOrNull() ?: 150
                        AppGoals.fat                = targetFat.toIntOrNull() ?: 44
                        AppProfile.save(context)
                        AppGoals.save(context)
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Text(
                    if (page < 2) "다음" else "시작하기",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun OnboardingField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, color = Color(0xFF888888))
        content()
    }
}

@Composable
fun OnboardingNutrientField(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF666666))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            suffix = { Text(unit, fontSize = 13.sp) },
            modifier = Modifier.width(120.dp),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}