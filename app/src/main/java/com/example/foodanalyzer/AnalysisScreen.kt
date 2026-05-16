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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import coil.compose.rememberAsyncImagePainter
import com.example.foodanalyzer.camera.FoodClassifier
import com.example.foodanalyzer.data.FoodRepository
import com.example.foodanalyzer.data.GeminiNutritionService
import com.example.foodanalyzer.data.entity.Food
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

val MealCardBg       = Color(0xFFFFFFFF)
val AnalyzeGreen     = Color(0xFFD6F0D6)
val AnalyzeGreenText = Color(0xFF4CAF50)
val ResetRed         = Color(0xFFFFE0E0)
val ResetRedText     = Color(0xFFE57373)
val PlusBlue         = Color(0xFFD6EAF8)
val PlusBlueIcon     = Color(0xFF5B9BD5)

enum class MealType(val label: String, val hint: String) {
    BREAKFAST("아침", "아침을 기록해 보세요."),
    LUNCH("점심", "점심을 기록해 보세요."),
    DINNER("저녁", "저녁을 기록해 보세요."),
    SNACK("간식", "간식을 기록해 보세요.")
}

data class RecognizedFood(
    val id: Int,
    val name: String,
    val amount: String,
    val kcal: Int = 0,
    val carbs: Int = 0,
    val protein: Int = 0,
    val fat: Int = 0,
    val kcalPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0
)

data class MealResult(
    val foods: List<RecognizedFood>,
    val totalKcal: Int    = foods.sumOf { it.kcal },
    val totalCarbs: Int   = foods.sumOf { it.carbs },
    val totalProtein: Int = foods.sumOf { it.protein },
    val totalFat: Int     = foods.sumOf { it.fat }
)

fun Food.toRecognizedFood(amount: String = "1인분"): RecognizedFood {
    val grams = this.avgWeightG
    val kcal    = (this.calories * grams / 100).toInt()
    val carbs   = (this.carb    * grams / 100).toInt()
    val protein = (this.protein * grams / 100).toInt()
    val fat     = (this.fat     * grams / 100).toInt()

    return RecognizedFood(
        id             = this.id,
        name           = this.name,
        amount         = "${grams.toInt()}g",
        kcal           = kcal,
        carbs          = carbs,
        protein        = protein,
        fat            = fat,
        kcalPer100g    = this.calories,
        carbsPer100g   = this.carb,
        proteinPer100g = this.protein,
        fatPer100g     = this.fat
    )
}

fun getFakeAiResult(): List<RecognizedFood> = listOf(
    RecognizedFood(1, "흰쌀밥", "210g", 313, 68, 5, 0),
    RecognizedFood(2, "김치", "50g", 15, 2, 1, 0)
)
fun foodFromDb(id: Int, dbFood: Food): RecognizedFood {
    val grams = dbFood.avgWeightG
    val kcal    = (dbFood.calories * grams / 100).toInt()
    val carbs   = (dbFood.carb     * grams / 100).toInt()
    val protein = (dbFood.protein  * grams / 100).toInt()
    val fat     = (dbFood.fat      * grams / 100).toInt()

    return RecognizedFood(
        id             = id,
        name           = dbFood.name,
        amount         = "${grams.toInt()}g",
        kcal           = kcal,
        carbs          = carbs,
        protein        = protein,
        fat            = fat,
        kcalPer100g    = dbFood.calories,
        carbsPer100g   = dbFood.carb,
        proteinPer100g = dbFood.protein,
        fatPer100g     = dbFood.fat
    )
}
enum class AnalysisStep { MEAL_LIST, CONFIRM, RESULT }

@Composable
fun AnalysisScreen() {
    val today    = LocalDate.now()
    val monday   = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weekDays = (0..6).map { monday.plusDays(it.toLong()) }

    var selectedDate by remember { mutableStateOf(today) }
    var currentStep  by remember { mutableStateOf(AnalysisStep.MEAL_LIST) }
    var currentMeal  by remember { mutableStateOf<MealType?>(null) }

    val photoMap      = AppMealData.photoMap
    val mealResultMap = AppMealData.mealResultMap

    val editFoodList = remember { mutableStateListOf<RecognizedFood>() }
    val editAmounts  = remember { mutableStateMapOf<Int, String>() }

    var confirmedFoods by remember { mutableStateOf<List<RecognizedFood>>(emptyList()) }

    val context = LocalContext.current
    var searchResults by remember { mutableStateOf<List<RecognizedFood>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

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
                    id      = log.foodId,
                    name    = log.foodName,
                    amount  = "${log.weightG.toInt()}g",
                    kcal    = log.calories.toInt(),
                    carbs   = log.carb.toInt(),
                    protein = log.protein.toInt(),
                    fat     = log.fat.toInt()
                )
            }
            mealResultMap[key] = MealResult(foods)
        }
    }

    fun photoKey(date: LocalDate, meal: MealType) = "${date}_${meal.name}"

    when (currentStep) {

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
                            meal            = meal,
                            photoUri        = photoMap[key],
                            savedResult     = result,
                            onPhotoSelected = { photoMap[key] = it },
                            onReset         = {
                                photoMap.remove(key)
                                mealResultMap.remove(key)
                            },
                            onAnalyze = {
                                currentMeal = meal
                                val photoUri = photoMap[photoKey(selectedDate, meal)]

                                if (photoUri != null) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val inputStream = context.contentResolver.openInputStream(photoUri)
                                            val bitmap = BitmapFactory.decodeStream(inputStream)
                                            val classifier = FoodClassifier(context)
                                            val results = classifier.classify(bitmap)
                                            classifier.close()

                                            val repo = FoodRepository(context)
                                            val recognizedFoods = results.mapIndexed { index, result ->
                                                val koreanName = FoodClassifier.labelToKorean[result.label] ?: result.label
                                                val dbFood = repo.searchFood(koreanName).firstOrNull()
                                                android.util.Log.d("FoodItem", "DB 검색어: ${koreanName}, DB결과: ${dbFood?.name}")
                                                if (dbFood != null) {
                                                    foodFromDb(index, dbFood)
                                                } else {
                                                    val nutritionList = GeminiNutritionService.getNutritionList(listOf(koreanName))
                                                    val nutrition = nutritionList.getOrNull(0)
                                                    RecognizedFood(
                                                        id      = index,
                                                        name    = nutrition?.foodName ?: koreanName,
                                                        amount  = "1인분",
                                                        kcal    = nutrition?.kcal ?: 0,
                                                        carbs   = nutrition?.carbs ?: 0,
                                                        protein = nutrition?.protein ?: 0,
                                                        fat     = nutrition?.fat ?: 0,
                                                        kcalPer100g    = nutrition?.kcal?.toDouble() ?: 0.0,    // ← 추가
                                                        carbsPer100g   = nutrition?.carbs?.toDouble() ?: 0.0,   // ← 추가
                                                        proteinPer100g = nutrition?.protein?.toDouble() ?: 0.0, // ← 추가
                                                        fatPer100g     = nutrition?.fat?.toDouble() ?: 0.0      // ← 추가
                                                    )
                                                }
                                            }

                                            withContext(Dispatchers.Main) {
                                                editFoodList.clear()
                                                editFoodList.addAll(recognizedFoods)
                                                editAmounts.clear()
                                                recognizedFoods.forEach { editAmounts[it.id] = it.amount }
                                                currentStep = AnalysisStep.CONFIRM
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("TFLite", "에러: ${e.message}")
                                            withContext(Dispatchers.Main) {
                                                editFoodList.clear()
                                                editFoodList.addAll(getFakeAiResult())
                                                editAmounts.clear()
                                                getFakeAiResult().forEach { editAmounts[it.id] = it.amount }
                                                currentStep = AnalysisStep.CONFIRM
                                            }
                                        }
                                    }
                                } else {
                                    editFoodList.clear()
                                    editFoodList.addAll(getFakeAiResult())
                                    editAmounts.clear()
                                    getFakeAiResult().forEach { editAmounts[it.id] = it.amount }
                                    currentStep = AnalysisStep.CONFIRM
                                }
                            },
                            onReEdit = {
                                currentMeal = meal
                                val existingResult = mealResultMap[key]
                                if (existingResult != null) {
                                    editFoodList.clear()
                                    editFoodList.addAll(existingResult.foods)
                                    editAmounts.clear()
                                    existingResult.foods.forEach { editAmounts[it.id] = it.amount }
                                    currentStep = AnalysisStep.CONFIRM
                                }
                            },
                            onBarcodeResult = { food ->
                                currentMeal = meal
                                editFoodList.clear()
                                editFoodList.add(food)
                                editAmounts.clear()
                                editAmounts[food.id] = food.amount
                                currentStep = AnalysisStep.CONFIRM
                            },
                            onSearchResult = { foods ->
                                currentMeal = meal
                                editFoodList.clear()
                                editFoodList.addAll(foods)
                                editAmounts.clear()
                                foods.forEach { editAmounts[it.id] = it.amount }

                                CoroutineScope(Dispatchers.IO).launch {
                                    val repo = FoodRepository(context)
                                    val updatedFoods = foods.map { food ->
                                        val dbFood = repo.searchFood(food.name).firstOrNull()
                                        if (dbFood != null) {
                                            // amount가 g 단위면 그대로, 아니면 개수로 계산
                                            val totalWeight = if (food.amount.contains("g", ignoreCase = true)) {
                                                food.amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: dbFood.avgWeightG
                                            } else {
                                                val count = food.amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 1.0
                                                dbFood.avgWeightG * count
                                            }
                                            foodFromDb(food.id, dbFood).copy(
                                                amount  = "${totalWeight.toInt()}g",
                                                kcal    = (dbFood.calories * totalWeight / dbFood.avgWeightG).toInt(),
                                                carbs   = (dbFood.carb * totalWeight / dbFood.avgWeightG).toInt(),
                                                protein = (dbFood.protein * totalWeight / dbFood.avgWeightG).toInt(),
                                                fat     = (dbFood.fat * totalWeight / dbFood.avgWeightG).toInt()
                                            )
                                        } else {
                                            val nutritionList = GeminiNutritionService.getNutritionList(listOf(food.name))
                                            val nutrition = nutritionList.getOrNull(0)
                                            food.copy(
                                                name           = nutrition?.foodName ?: food.name,
                                                kcal           = nutrition?.kcal ?: 0,
                                                carbs          = nutrition?.carbs ?: 0,
                                                protein        = nutrition?.protein ?: 0,
                                                fat            = nutrition?.fat ?: 0,
                                                kcalPer100g    = nutrition?.kcal?.toDouble() ?: 0.0,
                                                carbsPer100g   = nutrition?.carbs?.toDouble() ?: 0.0,
                                                proteinPer100g = nutrition?.protein?.toDouble() ?: 0.0,
                                                fatPer100g     = nutrition?.fat?.toDouble() ?: 0.0
                                            )
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        editFoodList.clear()
                                        editFoodList.addAll(updatedFoods)
                                        editAmounts.clear()
                                        updatedFoods.forEach { editAmounts[it.id] = it.amount }
                                        currentStep = AnalysisStep.CONFIRM
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        AnalysisStep.CONFIRM -> {
            FoodConfirmScreen(
                foodList    = editFoodList,
                editAmounts = editAmounts,
                onBack      = { currentStep = AnalysisStep.MEAL_LIST },
                onConfirm   = { confirmed ->
                    confirmedFoods = confirmed
                    currentStep = AnalysisStep.RESULT
                }
            )
        }

        AnalysisStep.RESULT -> {
            NutritionResultScreen(
                foods    = confirmedFoods,
                onReEdit = { currentStep = AnalysisStep.CONFIRM },
                onComplete = {
                    val key = "${selectedDate}_${currentMeal?.name}"
                    mealResultMap[key] = MealResult(confirmedFoods)

                    CoroutineScope(Dispatchers.IO).launch {
                        val db = com.example.foodanalyzer.data.AppDatabase.getInstance(context)
                        val dao = db.dailyLogDao()
                        confirmedFoods.forEach { food ->
                            dao.insert(
                                com.example.foodanalyzer.data.entity.DailyLog(
                                    date     = selectedDate.toString(),
                                    foodId   = food.id,
                                    foodName = food.name,
                                    weightG  = food.amount.replace("g", "").toDoubleOrNull() ?: 100.0,
                                    calories = food.kcal.toDouble(),
                                    carb     = food.carbs.toDouble(),
                                    protein  = food.protein.toDouble(),
                                    fat      = food.fat.toDouble(),
                                    mealType = currentMeal?.label ?: "기타"
                                )
                            )
                        }
                    }
                    currentStep = AnalysisStep.MEAL_LIST
                }
            )
        }
    }
}

@Composable
fun MealCard(
    meal: MealType,
    photoUri: Uri?,
    savedResult: MealResult?,
    onPhotoSelected: (Uri) -> Unit,
    onReset: () -> Unit,
    onAnalyze: () -> Unit,
    onReEdit: () -> Unit,
    onBarcodeResult: (RecognizedFood) -> Unit,
    onSearchResult: (List<RecognizedFood>) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onPhotoSelected(it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("photo_uri")
            uriString?.let { onPhotoSelected(Uri.parse(it)) }
        }
    }

    val barcodeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val barcode = result.data?.getStringExtra("barcode")
            if (barcode != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val food = com.example.foodanalyzer.data.FoodSafetyService.getFoodByBarcode(barcode)
                    withContext(Dispatchers.Main) {
                        if (food != null) {
                            onBarcodeResult(RecognizedFood(
                                id      = 0,
                                name    = food.foodName,
                                amount  = "${food.servingSize.toInt()}g",
                                kcal    = food.kcal.toInt(),
                                carbs   = food.carbs.toInt(),
                                protein = food.protein.toInt(),
                                fat     = food.fat.toInt()
                            ))
                        } else {
                            Toast.makeText(context, "등록되지 않은 바코드입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val searchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val json = result.data?.getStringExtra("food_list_json") ?: return@rememberLauncherForActivityResult
            try {
                val jsonArray = org.json.JSONArray(json)
                val foods = mutableListOf<RecognizedFood>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val foodName = obj.getString("food")
                    val amount = "${obj.optInt("amount", 1)}${obj.optString("unit", "인분")}"
                    foods.add(RecognizedFood(id = i, name = foodName, amount = amount))
                }
                onSearchResult(foods)
            } catch (e: Exception) {
                android.util.Log.e("Search", "파싱 실패: ${e.message}")
            }
        }
    }

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
                    // 갤러리 버튼
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PlusBlue)
                            .clickable { launcher.launch("image/*") }
                    ) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = "갤러리에서 선택",
                            tint = PlusBlueIcon,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 카메라 버튼
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9))
                            .clickable {
                                val intent = Intent(context, com.example.foodanalyzer.camera.CameraActivity::class.java)
                                cameraLauncher.launch(intent)
                            }
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "카메라로 촬영",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 텍스트 입력 버튼
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0))
                            .clickable {
                                val intent = Intent(context, search.SearchActivity::class.java)
                                searchLauncher.launch(intent)
                            }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "텍스트로 입력",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (savedResult != null) {
                SavedResultSummary(result = savedResult)
            } else if (photoUri != null) {
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
            } else {
                Text(meal.hint, fontSize = 14.sp, color = Color(0xFFAAAAAA))
            }
        }
    }
}

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
            Text(
                "${result.totalKcal} kcal",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                NutrientMiniItem("탄수화물", "${result.totalCarbs}g")
                NutrientMiniItem("단백질",  "${result.totalProtein}g")
                NutrientMiniItem("지방",    "${result.totalFat}g")
            }
        }
    }
}

@Composable
fun NutrientMiniItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color(0xFF999999))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
    }
}

@Composable
fun FoodConfirmScreen(
    foodList: MutableList<RecognizedFood>,
    editAmounts: MutableMap<Int, String>,
    onBack: () -> Unit,
    onConfirm: (List<RecognizedFood>) -> Unit
) {
    val context = LocalContext.current
    var inputFoodName by remember { mutableStateOf("") }

    // ── 갤러리 런처 ──
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val classifier = FoodClassifier(context)
                    val results = classifier.classify(bitmap)
                    classifier.close()

                    val repo = FoodRepository(context)
                    val newFoods = results.mapIndexed { index, result ->
                        val koreanName = FoodClassifier.labelToKorean[result.label] ?: result.label
                        val dbFood = repo.searchFood(koreanName).firstOrNull()
                        val newId = (foodList.maxOfOrNull { it.id } ?: 0) + index + 1
                        if (dbFood != null) {
                            foodFromDb(newId, dbFood)
                        } else {
                            val nutritionList = GeminiNutritionService.getNutritionList(listOf(koreanName))
                            val nutrition = nutritionList.getOrNull(0)
                            RecognizedFood(
                                id      = newId,
                                name    = nutrition?.foodName ?: koreanName,
                                amount  = "1인분",
                                kcal    = nutrition?.kcal ?: 0,
                                carbs   = nutrition?.carbs ?: 0,
                                protein = nutrition?.protein ?: 0,
                                fat     = nutrition?.fat ?: 0,
                                kcalPer100g    = nutrition?.kcal?.toDouble() ?: 0.0,    // ← 추가
                                carbsPer100g   = nutrition?.carbs?.toDouble() ?: 0.0,   // ← 추가
                                proteinPer100g = nutrition?.protein?.toDouble() ?: 0.0, // ← 추가
                                fatPer100g     = nutrition?.fat?.toDouble() ?: 0.0      // ← 추가
                            )
                        }
                    }
                    withContext(Dispatchers.Main) {
                        foodList.addAll(newFoods)
                        newFoods.forEach { editAmounts[it.id] = it.amount }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Gallery", "에러: ${e.message}")
                }
            }
        }
    }

    // ── 카메라 런처 ──
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra("photo_uri")
            uriString?.let { uriStr ->
                val uri = Uri.parse(uriStr)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val classifier = FoodClassifier(context)
                        val results = classifier.classify(bitmap)
                        classifier.close()

                        val repo = FoodRepository(context)
                        val newFoods = results.mapIndexed { index, result ->
                            val koreanName = FoodClassifier.labelToKorean[result.label] ?: result.label
                            val dbFood = repo.searchFood(koreanName).firstOrNull()
                            val newId = (foodList.maxOfOrNull { it.id } ?: 0) + index + 1
                            if (dbFood != null) {
                                foodFromDb(newId, dbFood)
                            } else {
                                val nutritionList = GeminiNutritionService.getNutritionList(listOf(koreanName))
                                val nutrition = nutritionList.getOrNull(0)
                                RecognizedFood(
                                    id      = newId,
                                    name    = nutrition?.foodName ?: koreanName,
                                    amount  = "1인분",
                                    kcal    = nutrition?.kcal ?: 0,
                                    carbs   = nutrition?.carbs ?: 0,
                                    protein = nutrition?.protein ?: 0,
                                    fat     = nutrition?.fat ?: 0,
                                    kcalPer100g    = nutrition?.kcal?.toDouble() ?: 0.0,    // ← 추가
                                    carbsPer100g   = nutrition?.carbs?.toDouble() ?: 0.0,   // ← 추가
                                    proteinPer100g = nutrition?.protein?.toDouble() ?: 0.0, // ← 추가
                                    fatPer100g     = nutrition?.fat?.toDouble() ?: 0.0      // ← 추가
                                )
                            }
                        }
                        withContext(Dispatchers.Main) {
                            foodList.addAll(newFoods)
                            newFoods.forEach { editAmounts[it.id] = it.amount }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Camera", "에러: ${e.message}")
                    }
                }
            }
        }
    }

    // ── 검색 런처 ──
    val searchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val json = result.data?.getStringExtra("food_list_json") ?: return@rememberLauncherForActivityResult
            try {
                val jsonArray = org.json.JSONArray(json)
                val foods = mutableListOf<RecognizedFood>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val foodName = obj.getString("food")
                    val amount = "${obj.optInt("amount", 1)}${obj.optString("unit", "인분")}"
                    foods.add(RecognizedFood(
                        id = (foodList.maxOfOrNull { it.id } ?: 0) + i + 1,
                        name = foodName,
                        amount = amount
                    ))
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val repo = FoodRepository(context)
                    val updatedFoods = foods.map { food ->
                        val dbFood = repo.searchFood(food.name).firstOrNull()
                        android.util.Log.d("FoodItem", "food.amount: ${food.amount}, dbFood: ${dbFood?.name}, avgWeightG: ${dbFood?.avgWeightG}")

                        if (dbFood != null) {
                            foodFromDb((foodList.maxOfOrNull { it.id } ?: 0) + foods.indexOf(food) + 1, dbFood)
                        } else {
                            val nutritionList = GeminiNutritionService.getNutritionList(listOf(food.name))
                            val nutrition = nutritionList.getOrNull(0)
                            food.copy(
                                name    = nutrition?.foodName ?: food.name,
                                kcal    = nutrition?.kcal ?: 0,
                                carbs   = nutrition?.carbs ?: 0,
                                protein = nutrition?.protein ?: 0,
                                fat     = nutrition?.fat ?: 0,
                                kcalPer100g    = nutrition?.kcal?.toDouble() ?: 0.0,    // ← 추가
                                carbsPer100g   = nutrition?.carbs?.toDouble() ?: 0.0,   // ← 추가
                                proteinPer100g = nutrition?.protein?.toDouble() ?: 0.0, // ← 추가
                                fatPer100g     = nutrition?.fat?.toDouble() ?: 0.0      // ← 추가
                            )
                        }
                    }
                    withContext(Dispatchers.Main) {
                        foodList.addAll(updatedFoods)
                        updatedFoods.forEach { editAmounts[it.id] = it.amount }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Search", "파싱 실패: ${e.message}")
            }
        }
    }

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
            Spacer(modifier = Modifier.height(16.dp))

            // ── 추가 버튼 3개 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PlusBlue)
                        .clickable { galleryLauncher.launch("image/*") }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = PlusBlueIcon, modifier = Modifier.size(16.dp))
                        Text("갤러리", fontSize = 13.sp, color = PlusBlueIcon, fontWeight = FontWeight.Medium)
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9))
                        .clickable {
                            val intent = Intent(context, com.example.foodanalyzer.camera.CameraActivity::class.java)
                            cameraLauncher.launch(intent)
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Text("카메라", fontSize = 13.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF3E0))
                        .clickable {
                            val intent = Intent(context, search.SearchActivity::class.java)
                            searchLauncher.launch(intent)
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                        Text("텍스트", fontSize = 13.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 음식 직접 추가 입력창 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputFoodName,
                    onValueChange = { inputFoodName = it },
                    placeholder = { Text("음식 이름 직접 입력", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        if (inputFoodName.isNotBlank()) {
                            val newId = if (foodList.isEmpty()) 0 else foodList.maxOf { it.id } + 1
                            foodList.add(RecognizedFood(id = newId, name = inputFoodName, amount = "1인분"))
                            editAmounts[newId] = "1인분"

                            val foodNameToSearch = inputFoodName
                            inputFoodName = ""

                            CoroutineScope(Dispatchers.IO).launch {
                                val repo = FoodRepository(context)
                                val dbFood = repo.searchFood(foodNameToSearch).firstOrNull()
                                withContext(Dispatchers.Main) {
                                    val index = foodList.indexOfFirst { it.id == newId }
                                    if (index != -1) {
                                        if (dbFood != null) {
                                            foodList[index] = foodList[index].copy(
                                                name    = dbFood.name,
                                                amount  = "${dbFood.avgWeightG.toInt()}g",
                                                kcal    = (dbFood.calories * dbFood.avgWeightG / 100).toInt(),
                                                carbs   = (dbFood.carb * dbFood.avgWeightG / 100).toInt(),
                                                protein = (dbFood.protein * dbFood.avgWeightG / 100).toInt(),
                                                fat     = (dbFood.fat * dbFood.avgWeightG / 100).toInt()
                                            )
                                            foodList[index] = foodFromDb(newId, dbFood)
                                            editAmounts[newId] = "${dbFood.avgWeightG.toInt()}g"
                                        } else {
                                            val nutritionList = GeminiNutritionService.getNutritionList(listOf(foodNameToSearch))
                                            val nutrition = nutritionList.getOrNull(0)
                                            if (nutrition != null) {
                                                foodList[index] = foodList[index].copy(
                                                    name    = nutrition.foodName,
                                                    kcal    = nutrition.kcal,
                                                    carbs   = nutrition.carbs,
                                                    protein = nutrition.protein,
                                                    fat     = nutrition.fat,
                                                    kcalPer100g    = nutrition?.kcal?.toDouble() ?: 0.0,    // ← 추가
                                                    carbsPer100g   = nutrition?.carbs?.toDouble() ?: 0.0,   // ← 추가
                                                    proteinPer100g = nutrition?.protein?.toDouble() ?: 0.0, // ← 추가
                                                    fatPer100g     = nutrition?.fat?.toDouble() ?: 0.0      // ← 추가
                                                )
                                            }
                                            if (nutrition != null) {
                                                android.util.Log.d("FoodItem", "Gemini 결과 - kcal: ${nutrition.kcal}, foodName: ${nutrition.foodName}")
                                                foodList[index] = foodList[index].copy(
                                                    name           = nutrition.foodName,
                                                    kcal           = nutrition.kcal,
                                                    carbs          = nutrition.carbs,
                                                    protein        = nutrition.protein,
                                                    fat            = nutrition.fat,
                                                    kcalPer100g    = nutrition.kcal.toDouble(),
                                                    carbsPer100g   = nutrition.carbs.toDouble(),
                                                    proteinPer100g = nutrition.protein.toDouble(),
                                                    fatPer100g     = nutrition.fat.toDouble()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Text("추가", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (foodList.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("인식된 음식이 없습니다.", fontSize = 15.sp, color = Color(0xFFAAAAAA))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("위에서 음식을 직접 추가해주세요.", fontSize = 13.sp, color = Color(0xFFCCCCCC))
                    }
                }
            } else {
                foodList.toList().forEach { food ->

                    FoodItemCard(
                        food           = food,
                        currentAmount  = editAmounts[food.id] ?: food.amount,
                        onAmountChange = { editAmounts[food.id] = it },
                        onDelete       = {
                            foodList.remove(food)
                            editAmounts.remove(food.id)
                        },
                        onNameChange   = { newName ->
                            val idx = foodList.indexOfFirst { it.id == food.id }
                            if (idx != -1) foodList[idx] = food.copy(name = newName)
                        },
                        onNutritionRecalculate = { updatedFood ->
                            val idx = foodList.indexOfFirst { it.id == food.id }
                            if (idx != -1) {
                                foodList.removeAt(idx)
                                foodList.add(idx, updatedFood)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (foodList.isNotEmpty()) {
                Button(
                    onClick = {
                        // editAmounts의 최신 양으로 foodList 업데이트 후 confirm
                        val finalFoods = foodList.map { food ->
                            val amount = editAmounts[food.id] ?: food.amount
                            if (food.kcalPer100g > 0) {
                                val grams = amount.replace("g", "").toDoubleOrNull()
                                if (grams != null) {
                                    food.copy(
                                        amount  = amount,
                                        kcal    = (food.kcalPer100g * grams / 100).toInt(),
                                        carbs   = (food.carbsPer100g * grams / 100).toInt(),
                                        protein = (food.proteinPer100g * grams / 100).toInt(),
                                        fat     = (food.fatPer100g * grams / 100).toInt()
                                    )
                                } else food.copy(amount = amount)
                            } else food.copy(amount = amount)
                        }
                        onConfirm(finalFoods)
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

@Composable
fun FoodItemCard(
    food: RecognizedFood,
    currentAmount: String,
    onAmountChange: (String) -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onNutritionRecalculate: (RecognizedFood) -> Unit
) {
    var isEditingAmount by remember { mutableStateOf(false) }
    var isEditingName   by remember { mutableStateOf(false) }
    var tempAmount      by remember(currentAmount) { mutableStateOf(currentAmount) }
    var tempName        by remember(food.name) { mutableStateOf(food.name) }
    val focusManager    = LocalFocusManager.current

    // 실시간 칼로리 계산
    val displayGrams = tempAmount
        .replace("g", "")
        .replace("G", "")
        .trim()
        .toDoubleOrNull()
        ?: tempAmount.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
    android.util.Log.d("FoodItem", "displayGrams: $displayGrams, kcalPer100g: ${food.kcalPer100g}")
    val displayKcal = if (food.kcalPer100g > 0 && displayGrams != null)
        (food.kcalPer100g * displayGrams / 100).toInt() else food.kcal
    val displayCarbs = if (food.carbsPer100g > 0 && displayGrams != null)
        (food.carbsPer100g * displayGrams / 100).toInt() else food.carbs
    val displayProtein = if (food.proteinPer100g > 0 && displayGrams != null)
        (food.proteinPer100g * displayGrams / 100).toInt() else food.protein
    val displayFat = if (food.fatPer100g > 0 && displayGrams != null)
        (food.fatPer100g * displayGrams / 100).toInt() else food.fat

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
                    if (isEditingName) {
                        BasicTextField(
                            value = tempAmount,
                            onValueChange = {
                                tempAmount = it
                                android.util.Log.d("FoodItem", "tempAmount changed: $it")
                            },
                            textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                onNameChange(tempName)
                                isEditingName = false
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFF5B9BD5), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("✓ 확인", fontSize = 13.sp, color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                onNameChange(tempName)
                                isEditingName = false
                                focusManager.clearFocus()
                            }
                        )
                    } else {
                        Text(text = food.name, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.clickable { isEditingName = true }
                        )
                        Text("✏ 이름 수정", fontSize = 11.sp, color = Color(0xFFAAAAAA),
                            modifier = Modifier.clickable { isEditingName = true }
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFE57373), modifier = Modifier.size(22.dp))
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("양", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                Spacer(modifier = Modifier.height(4.dp))
                if (isEditingAmount) {
                    BasicTextField(
                        value = tempAmount,
                        onValueChange = { tempAmount = it },
                        textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1A1A1A)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            isEditingAmount = false
                            focusManager.clearFocus()
                            onAmountChange(tempAmount)
                            onNutritionRecalculate(food.copy(
                                amount  = tempAmount,
                                kcal    = displayKcal,
                                carbs   = displayCarbs,
                                protein = displayProtein,
                                fat     = displayFat
                            ))
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFF5B9BD5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("✓ 확인", fontSize = 13.sp, color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            isEditingAmount = false
                            focusManager.clearFocus()
                            onAmountChange(tempAmount)
                            onNutritionRecalculate(food.copy(
                                amount  = tempAmount,
                                kcal    = displayKcal,
                                carbs   = displayCarbs,
                                protein = displayProtein,
                                fat     = displayFat
                            ))
                        }
                    )
                } else {
                    Text(
                        text = currentAmount,
                        fontSize = 15.sp, color = Color(0xFF444444),
                        modifier = Modifier.clickable { isEditingAmount = true }.padding(vertical = 2.dp)
                    )
                }
            }

            // 영양소 표시
            if (displayKcal > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${displayKcal}kcal", fontSize = 13.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                    Text("탄 ${displayCarbs}g", fontSize = 13.sp, color = Color(0xFF888888))
                    Text("단 ${displayProtein}g", fontSize = 13.sp, color = Color(0xFF888888))
                    Text("지 ${displayFat}g", fontSize = 13.sp, color = Color(0xFF888888))
                }
            }
        }
    }
}

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onReEdit, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF1A1A1A))
            }
            Text("영양소 분석 결과", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A), modifier = Modifier.align(Alignment.Center))
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

@Composable
fun NutrientSummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF999999))
    }
}

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