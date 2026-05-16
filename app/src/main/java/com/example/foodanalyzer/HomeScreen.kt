package com.example.foodanalyzer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────────────────────────────────────────
// 목표값 (추후 설정 화면에서 변경 가능하도록 확장)
// ───────────────────────────────────────────────




// ───────────────────────────────────────────────
// 색상 정의
// ───────────────────────────────────────────────
val BackgroundColor   = Color(0xFFF0F4E8)
val CardColor         = Color(0xFFE8EDD8)
val CalorieBlue       = Color(0xFF5B9BD5)
val CarbGreen         = Color(0xFF4CAF50)
val ProteinBlue       = Color(0xFF5B9BD5)
val FatOrange         = Color(0xFFE6A020)
val FabBlue           = Color(0xFF5B9BD5)
val SelectedDayBg     = Color(0xFFDDE8C0)
val TextDark          = Color(0xFF1A1A1A)
val TextGray          = Color(0xFF888888)

// ───────────────────────────────────────────────
// HomeScreen
// ───────────────────────────────────────────────
@Composable
fun HomeScreen() {
    val today = LocalDate.now()

    // 이번 주 월요일 기준 날짜 7개 생성
    val monday = today.minusDays(((today.dayOfWeek.value - 1).toLong()))
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

    var selectedDate by remember { mutableStateOf(today) }
    // AppMealData에서 실제 기록된 데이터 가져오기

    val context = LocalContext.current

    LaunchedEffect(selectedDate) {
        val db = com.example.foodanalyzer.data.AppDatabase.getInstance(context)
        val dao = db.dailyLogDao()
        val logs = dao.getByDate(selectedDate.toString())

        val grouped = logs.groupBy { it.mealType }
        grouped.forEach { (mealLabel, logList) ->
            val mealType = MealType.entries.find { it.label == mealLabel } ?: return@forEach
            val key = "${selectedDate}_${mealType.name}"
            val foods = logList.map { log ->
                RecognizedFood(
                    id = log.foodId,
                    name = log.foodName,
                    amount = "${log.weightG.toInt()}g",
                    kcal = log.calories.toInt(),
                    carbs = log.carb.toInt(),
                    protein = log.protein.toInt(),
                    fat = log.fat.toInt()
                )
            }
            AppMealData.mealResultMap[key] = MealResult(foods)
        }
    }

    val nutrition = AppMealData.getDayNutrition(selectedDate.toString())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // ── 1. 주간 날짜 선택 바 ──
            WeekDateSelector(
                weekDays = weekDays,
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = date
                    // AppMealData에서 자동으로 가져오므로 별도 갱신 불필요
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. 칼로리 현황 ──
            SectionTitle("칼로리 현황")
            Spacer(modifier = Modifier.height(16.dp))
            CalorieCircle(
                consumed = nutrition.calories,
                goal = AppGoals.calories
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── 3. 영양소 상세 ──
            SectionTitle("영양소 상세")
            Spacer(modifier = Modifier.height(16.dp))
            NutrientRow(nutrition = nutrition)

            Spacer(modifier = Modifier.height(32.dp))
            SectionTitle("먹으면 어떻게 될까?")
            Spacer(modifier = Modifier.height(16.dp))
            FoodPreviewCard(currentNutrition = nutrition)
        }

        // ── FAB (+) 버튼 ──

    }
}

// ───────────────────────────────────────────────
// 주간 날짜 선택 바
// ───────────────────────────────────────────────
@Composable
fun WeekDateSelector(
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val dayLabels = listOf("월", "화", "수", "목", "금", "토", "일")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextGray,
                    modifier = Modifier.width(40.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDays.forEachIndexed { index, date ->
                val isSelected = date == selectedDate
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) SelectedDayBg else Color.Transparent)
                        .clickable { onDateSelected(date) }
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF4A7C2F) else TextDark
                    )
                }
            }
        }
    }
}

// ───────────────────────────────────────────────
// 섹션 타이틀
// ───────────────────────────────────────────────
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

// ───────────────────────────────────────────────
// 칼로리 원형 차트
// ───────────────────────────────────────────────
@Composable
fun CalorieCircle(consumed: Int, goal: Int) {
    val progress = (consumed.toFloat() / goal.toFloat()).coerceIn(0f, 1.2f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "calorie_progress"
    )
    // 초과 시 색상 변경
    val arcColor = if (consumed > goal) Color(0xFFE57373) else CalorieBlue

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = 28.dp.toPx()
            val inset = stroke / 2
            // 배경 트랙
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    size.width - stroke,
                    size.height - stroke
                )
            )
            // 진행 아크
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    size.width - stroke,
                    size.height - stroke
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("오늘 섭취", fontSize = 14.sp, color = TextGray)
            Text(
                text = "$consumed kcal",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text("/ $goal kcal", fontSize = 14.sp, color = TextGray)
        }
    }
}

// ───────────────────────────────────────────────
// 영양소 3개 행
// ───────────────────────────────────────────────
@Composable
fun NutrientRow(nutrition: DayNutrition) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NutrientCircle(
            label = "탄수화물",
            consumed = nutrition.carbs,
            goal = AppGoals.carbs,
            unit = "g",
            color = CarbGreen,
            trackColor = Color(0xFFB8E0B8)
        )
        NutrientCircle(
            label = "단백질",
            consumed = nutrition.protein,
            goal = AppGoals.protein,
            unit = "g",
            color = ProteinBlue,
            trackColor = Color(0xFFDDE8F5)
        )
        NutrientCircle(
            label = "지방",
            consumed = nutrition.fat,
            goal = AppGoals.fat,
            unit = "g",
            color = FatOrange,
            trackColor = Color(0xFFF5E0B0)
        )
    }
}

// ───────────────────────────────────────────────
// 개별 영양소 원형 차트
// ───────────────────────────────────────────────
@Composable
fun NutrientCircle(
    label: String,
    consumed: Int,
    goal: Int,
    unit: String,
    color: Color,
    trackColor: Color,
    size: Dp = 100.dp
) {
    val rawProgress = consumed.toFloat() / goal.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "nutrient_$label"
    )
    val percent = (rawProgress * 100).toInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size)) {
                val stroke = 14.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(
                        this.size.width - stroke,
                        this.size.height - stroke
                    )
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(
                        this.size.width - stroke,
                        this.size.height - stroke
                    )
                )
            }
            Text(
                text = "$percent%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Text(
            "$consumed / $goal$unit",
            fontSize = 12.sp,
            color = TextGray
        )
    }
}


@Composable
fun FoodPreviewCard(currentNutrition: DayNutrition) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var gramsText by remember { mutableStateOf("") }
    var previewFood by remember { mutableStateOf<RecognizedFood?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("음식 이름을 입력하면 섭취 후 예상 영양소를 보여줄게요", fontSize = 13.sp, color = TextGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.text.BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextDark),
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (inputText.isEmpty()) Text("예: 삼겹살", fontSize = 15.sp, color = Color(0xFFAAAAAA))
                        inner()
                    }
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextDark),
                    modifier = Modifier.width(70.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 10.dp, vertical = 12.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    decorationBox = { inner ->
                        if (gramsText.isEmpty()) Text("g", fontSize = 15.sp, color = Color(0xFFAAAAAA))
                        inner()
                    }
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A1A)).clickable {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            val repo = com.example.foodanalyzer.data.FoodRepository(context)
                            val results = repo.searchFood(inputText.trim())
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (results.isNotEmpty()) {
                                    val food = foodFromDb(results[0].id, results[0])
                                    val customGrams = gramsText.toIntOrNull()
                                    previewFood = if (customGrams != null && customGrams > 0) {
                                        val ratio = customGrams.toFloat() / (results[0].avgWeightG)
                                        food.copy(
                                            amount = "${customGrams}g",
                                            kcal = (food.kcal * ratio).toInt(),
                                            carbs = (food.carbs * ratio).toInt(),
                                            protein = (food.protein * ratio).toInt(),
                                            fat = (food.fat * ratio).toInt()
                                        )
                                    } else food
                                }
                            }
                        }
                    }.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("확인", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            previewFood?.let { food ->
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Text("${food.name} (${food.amount}) 추가 시", fontSize = 13.sp, color = TextGray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewNutrientTag("칼로리", currentNutrition.calories, food.kcal, AppGoals.calories, "kcal", CalorieBlue)
                    PreviewNutrientTag("탄수화물", currentNutrition.carbs, food.carbs, AppGoals.carbs, "g", CarbGreen)
                    PreviewNutrientTag("단백질", currentNutrition.protein, food.protein, AppGoals.protein, "g", ProteinBlue)
                    PreviewNutrientTag("지방", currentNutrition.fat, food.fat, AppGoals.fat, "g", FatOrange)
                }
            }
        }
    }
}

@Composable
fun PreviewNutrientTag(label: String, current: Int, add: Int, goal: Int, unit: String, color: Color) {
    val after = current + add
    val isOver = after > goal
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isOver) Color(0xFFFFE0E0) else Color(0xFFE8F5E9)).padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text("$after$unit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isOver) Color(0xFFE57373) else color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = TextGray)
        Text(if (isOver) "초과!" else "+$add$unit", fontSize = 11.sp, color = if (isOver) Color(0xFFE57373) else color)
    }
}

