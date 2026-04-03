package com.example.foodanalyzer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import java.time.LocalDate

// ───────────────────────────────────────────────
// 색상
// ───────────────────────────────────────────────
val MealCardBg       = Color(0xFFFFFFFF)
val AnalyzeGreen     = Color(0xFFD6F0D6)
val AnalyzeGreenText = Color(0xFF4CAF50)
val ResetRed         = Color(0xFFFFE0E0)
val ResetRedText     = Color(0xFFE57373)
val PlusBlue         = Color(0xFFD6EAF8)
val PlusBlueIcon     = Color(0xFF5B9BD5)

// ───────────────────────────────────────────────
// 식사 종류
// ───────────────────────────────────────────────
enum class MealType(val label: String, val hint: String) {
    BREAKFAST("아침", "아침을 기록해 보세요."),
    LUNCH("점심", "점심을 기록해 보세요."),
    DINNER("저녁", "저녁을 기록해 보세요."),
    SNACK("간식", "간식을 기록해 보세요.")
}

// ───────────────────────────────────────────────
// 데이터 모델
// ───────────────────────────────────────────────
data class RecognizedFood(
    val id: Int,
    val name: String,
    val amount: String,
    val kcal: Int = 0,
    val carbs: Int = 0,
    val protein: Int = 0,
    val fat: Int = 0
)

// 식사별 저장된 결과
data class MealResult(
    val foods: List<RecognizedFood>,
    val totalKcal: Int    = foods.sumOf { it.kcal },
    val totalCarbs: Int   = foods.sumOf { it.carbs },
    val totalProtein: Int = foods.sumOf { it.protein },
    val totalFat: Int     = foods.sumOf { it.fat }
)

// ───────────────────────────────────────────────
// 더미 DB
// ───────────────────────────────────────────────
val foodDatabase = mapOf(
    "피자"    to RecognizedFood(0, "피자",    "", 550, 65, 25, 25),
    "음료"    to RecognizedFood(0, "음료",    "",   0,  0,  0,  0),
    "제로콜라" to RecognizedFood(0, "제로콜라", "",   0,  0,  0,  0),
    "치킨"    to RecognizedFood(0, "치킨",    "", 400, 20, 30, 22),
    "밥"      to RecognizedFood(0, "밥",      "", 300, 65,  5,  1)
)

fun lookupNutrition(name: String, amount: String): RecognizedFood {
    val base = foodDatabase[name] ?: RecognizedFood(0, name, amount)
    return base.copy(amount = amount)
}

fun getFakeAiResult(): List<RecognizedFood> = listOf(
    RecognizedFood(1, "피자",    "2조각", 550, 65, 25, 25),
    RecognizedFood(2, "제로콜라", "1잔",    0,  0,  0,  0)
)

// ───────────────────────────────────────────────
// 화면 단계
// ───────────────────────────────────────────────
enum class AnalysisStep { MEAL_LIST, CONFIRM, RESULT }

// ───────────────────────────────────────────────
// AnalysisScreen - 최상위
// ───────────────────────────────────────────────
@Composable
fun AnalysisScreen() {
    val today    = LocalDate.now()
    val monday   = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

    var selectedDate by remember { mutableStateOf(today) }
    var currentStep  by remember { mutableStateOf(AnalysisStep.MEAL_LIST) }
    var currentMeal  by remember { mutableStateOf<MealType?>(null) }

    val photoMap = AppMealData.photoMap

    // 날짜+식사 별로 저장된 결과 (기록 완료된 것들)
    // key: "날짜_식사종류"
    val mealResultMap = AppMealData.mealResultMap

    // 확인/수정 화면용 임시 상태
    val editFoodList = remember { mutableStateListOf<RecognizedFood>() }
    val editAmounts  = remember { mutableStateMapOf<Int, String>() }

    // 결과 화면용
    var confirmedFoods by remember { mutableStateOf<List<RecognizedFood>>(emptyList()) }

    fun photoKey(date: LocalDate, meal: MealType) = "${date}_${meal.name}"

    when (currentStep) {

        // ── 1단계: 식단 기록 메인 ──
        AnalysisStep.MEAL_LIST -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7F7F7))
            ) {
                WeekDateSelector(
                    weekDays = weekDays,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MealType.entries.forEach { meal ->
                        val key    = photoKey(selectedDate, meal)
                        val result = mealResultMap[key]

                        MealCard(
                            meal = meal,
                            photoUri = photoMap[key],
                            savedResult = result,
                            onPhotoSelected = { photoMap[key] = it },
                            onReset = {
                                photoMap.remove(key)
                                mealResultMap.remove(key)
                            },
                            onAnalyze = {
                                currentMeal = meal
                                val aiResult = getFakeAiResult()
                                editFoodList.clear()
                                editFoodList.addAll(aiResult)
                                editAmounts.clear()
                                aiResult.forEach { editAmounts[it.id] = it.amount }
                                currentStep = AnalysisStep.CONFIRM
                            },
                            onReEdit = {
                                // 수정 버튼: 사진과 결과 초기화 후 메인으로
                                photoMap.remove(key)
                                mealResultMap.remove(key)
                                // currentStep은 이미 MEAL_LIST이므로 별도 변경 불필요
                            }
                        )
                    }
                }
            }
        }

        // ── 2단계: 음식 확인/수정 ──
        AnalysisStep.CONFIRM -> {
            FoodConfirmScreen(
                foodList    = editFoodList,
                editAmounts = editAmounts,
                onBack      = { currentStep = AnalysisStep.MEAL_LIST },
                onConfirm   = { confirmed ->
                    confirmedFoods = confirmed.map {
                        lookupNutrition(it.name, it.amount).copy(id = it.id)
                    }
                    currentStep = AnalysisStep.RESULT
                }
            )
        }

        // ── 3단계: 영양소 결과 ──
        AnalysisStep.RESULT -> {
            NutritionResultScreen(
                foods    = confirmedFoods,
                onReEdit = { currentStep = AnalysisStep.CONFIRM },
                onComplete = {
                    // 결과를 mealResultMap에 저장 후 메인으로
                    val key = "${selectedDate}_${currentMeal?.name}"
                    mealResultMap[key] = MealResult(confirmedFoods)
                    currentStep = AnalysisStep.MEAL_LIST
                }
            )
        }
    }
}

// ───────────────────────────────────────────────
// 식사 카드
// ───────────────────────────────────────────────
@Composable
fun MealCard(
    meal: MealType,
    photoUri: Uri?,
    savedResult: MealResult?,       // 기록 완료된 결과
    onPhotoSelected: (Uri) -> Unit,
    onReset: () -> Unit,
    onAnalyze: () -> Unit,
    onReEdit: () -> Unit            // 수정 버튼
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onPhotoSelected(it) } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MealCardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ── 헤더 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    meal.label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                // 결과 있으면 수정 버튼, 없으면 + 버튼
                if (savedResult != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onReEdit() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "수정",
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("수정", fontSize = 13.sp, color = Color(0xFF888888))
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PlusBlue)
                            .clickable { launcher.launch("image/*") }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "사진 추가",
                            tint = PlusBlueIcon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 결과 있을 때: 영양소 요약 표시 ──
            if (savedResult != null) {
                SavedResultSummary(result = savedResult)
            }
            // ── 사진 있고 결과 없을 때: 분석 버튼 ──
            else if (photoUri != null) {
                Box(modifier = Modifier.wrapContentSize()) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF555555))
                            .clickable { onReset() }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(2f).height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AnalyzeGreen)
                            .clickable { onAnalyze() }
                    ) {
                        Text("⊞  분석 시작", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AnalyzeGreenText)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f).height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ResetRed)
                            .clickable { onReset() }
                    ) {
                        Text("초기화", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ResetRedText)
                    }
                }
            }
            // ── 둘 다 없을 때: 힌트 텍스트 ──
            else {
                Text(meal.hint, fontSize = 14.sp, color = Color(0xFFAAAAAA))
            }
        }
    }
}

// ───────────────────────────────────────────────
// 저장된 결과 요약 (카드 내부)
// ───────────────────────────────────────────────
@Composable
fun SavedResultSummary(result: MealResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 총 칼로리
            Text(
                "${result.totalKcal} kcal",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(10.dp))
            // 탄 / 단 / 지
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                NutrientMiniItem("탄수화물", "${result.totalCarbs}g")
                NutrientMiniItem("단백질",  "${result.totalProtein}g")
                NutrientMiniItem("지방",    "${result.totalFat}g")
            }
        }
    }
}

// ───────────────────────────────────────────────
// 영양소 미니 아이템 (카드 내부용)
// ───────────────────────────────────────────────
@Composable
fun NutrientMiniItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color(0xFF999999))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
    }
}

// ───────────────────────────────────────────────
// 음식 확인/수정 화면
// ───────────────────────────────────────────────
@Composable
fun FoodConfirmScreen(
    foodList: MutableList<RecognizedFood>,
    editAmounts: MutableMap<Int, String>,
    onBack: () -> Unit,
    onConfirm: (List<RecognizedFood>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF1A1A1A))
            }
            Text(
                "음식 확인/수정", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "AI가 식별한 결과입니다.\n이름과 양이 맞는지 확인해주세요.",
                fontSize = 14.sp, color = Color(0xFF999999),
                lineHeight = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (foodList.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), Alignment.Center) {
                    Text("모든 항목이 삭제되었습니다.", fontSize = 15.sp, color = Color(0xFFAAAAAA))
                }
            } else {
                foodList.toList().forEach { food ->
                    FoodItemCard(
                        food = food,
                        currentAmount = editAmounts[food.id] ?: food.amount,
                        onAmountChange = { editAmounts[food.id] = it },
                        onDelete = {
                            foodList.remove(food)
                            editAmounts.remove(food.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (foodList.isNotEmpty()) {
                Button(
                    onClick = {
                        onConfirm(foodList.map { it.copy(amount = editAmounts[it.id] ?: it.amount) })
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Text("저장하기", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ───────────────────────────────────────────────
// 음식 항목 카드
// ───────────────────────────────────────────────
@Composable
fun FoodItemCard(
    food: RecognizedFood,
    currentAmount: String,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing  by remember { mutableStateOf(false) }
    var tempAmount by remember(currentAmount) { mutableStateOf(currentAmount) }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("음식 이름", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(food.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFE57373), modifier = Modifier.size(22.dp))
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("양", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                Spacer(modifier = Modifier.height(4.dp))

                if (isEditing) {
                    BasicTextField(
                        value = tempAmount,
                        onValueChange = { tempAmount = it },
                        textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1A1A1A)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            onAmountChange(tempAmount)
                            isEditing = false
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFF5B9BD5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "✓ 확인", fontSize = 13.sp, color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            onAmountChange(tempAmount)
                            isEditing = false
                            focusManager.clearFocus()
                        }
                    )
                } else {
                    Text(
                        text = currentAmount,
                        fontSize = 15.sp, color = Color(0xFF444444),
                        modifier = Modifier
                            .clickable { isEditing = true }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ───────────────────────────────────────────────
// 영양소 결과 화면
// ───────────────────────────────────────────────
@Composable
fun NutritionResultScreen(
    foods: List<RecognizedFood>,
    onReEdit: () -> Unit,
    onComplete: () -> Unit
) {
    val totalKcal    = foods.sumOf { it.kcal }
    val totalCarbs   = foods.sumOf { it.carbs }
    val totalProtein = foods.sumOf { it.protein }
    val totalFat     = foods.sumOf { it.fat }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7))) {
        // 앱바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onReEdit, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF1A1A1A))
            }
            Text("음식 확인/수정", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A), modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "영양소 분석 결과입니다.\n하단 저장 버튼을 눌러 기록하세요.",
                fontSize = 14.sp, color = Color(0xFF999999),
                lineHeight = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 총 영양소 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("총 섭취 영양소", fontSize = 13.sp, color = Color(0xFF999999))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$totalKcal kcal", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NutrientSummaryItem("탄수화물", "${totalCarbs}g")
                        NutrientSummaryItem("단백질",  "${totalProtein}g")
                        NutrientSummaryItem("지방",    "${totalFat}g")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            foods.forEach { food ->
                FoodResultCard(food = food)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // 하단 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReEdit,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF555555)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("다시 수정하기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Text("✓  기록 완료", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ───────────────────────────────────────────────
// 총 영양소 요약 아이템
// ───────────────────────────────────────────────
@Composable
fun NutrientSummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF999999))
    }
}

// ───────────────────────────────────────────────
// 음식별 결과 카드
// ───────────────────────────────────────────────
@Composable
fun FoodResultCard(food: RecognizedFood) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(food.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            Spacer(modifier = Modifier.height(4.dp))
            Text("양: ${food.amount}", fontSize = 13.sp, color = Color(0xFF888888))
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NutrientTag("${food.kcal}kcal")
                NutrientTag("탄 ${food.carbs}")
                NutrientTag("단 ${food.protein}")
                NutrientTag("지 ${food.fat}")
            }
        }
    }
}

// ───────────────────────────────────────────────
// 영양소 태그
// ───────────────────────────────────────────────
@Composable
fun NutrientTag(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE8F5E9))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
    }
}