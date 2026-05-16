package com.example.foodanalyzer

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

enum class CalendarViewMode { CALENDAR, MONTH, YEAR }

data class DailyStats(
    val kcal: Int,
    val carbs: Int,
    val protein: Int,
    val fat: Int
)

fun calorieDotColor(kcal: Int, goal: Int = 2000): Color = when {
    kcal == 0         -> Color.Transparent
    kcal > goal * 1.1 -> Color(0xFFEF5350)
    kcal > goal * 0.8 -> Color(0xFFFFB300)
    else              -> Color(0xFF66BB6A)
}

fun getDummyStats(date: LocalDate): DailyStats? {
    if (date.isAfter(LocalDate.now())) return null
    val nutrition = AppMealData.getDayNutrition(date.toString())
    if (nutrition.calories == 0) return null
    return DailyStats(
        kcal    = nutrition.calories,
        carbs   = nutrition.carbs,
        protein = nutrition.protein,
        fat     = nutrition.fat
    )
}

@Composable
fun StatsScreen() {
    val today        = LocalDate.now()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }
    var slideForward by remember { mutableStateOf(true) }
    var viewMode     by remember { mutableStateOf(CalendarViewMode.CALENDAR) }

    val selectedStats = getDummyStats(selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── 최근 7일 꺾은선 그래프 ──
        WeeklyLineChart()

        Spacer(modifier = Modifier.height(16.dp))

        // ── 월 네비게이터 ──
        MonthNavigator(
            currentMonth = currentMonth,
            viewMode = viewMode,
            onPrev = {
                slideForward = false
                currentMonth = currentMonth.minusMonths(1)
                selectedDate = currentMonth.atDay(1)
            },
            onNext = {
                slideForward = true
                currentMonth = currentMonth.plusMonths(1)
                selectedDate = if (currentMonth == YearMonth.now()) today
                else currentMonth.atDay(1)
            },
            onMonthLabelClick = {
                viewMode = if (viewMode == CalendarViewMode.MONTH)
                    CalendarViewMode.CALENDAR else CalendarViewMode.MONTH
            },
            onYearLabelClick = {
                viewMode = if (viewMode == CalendarViewMode.YEAR)
                    CalendarViewMode.CALENDAR else CalendarViewMode.YEAR
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (viewMode) {
            CalendarViewMode.MONTH -> {
                MonthPickerGrid(
                    currentYear = currentMonth.year,
                    selectedMonth = currentMonth.monthValue,
                    onMonthSelected = { month ->
                        currentMonth = YearMonth.of(currentMonth.year, month)
                        selectedDate = currentMonth.atDay(1)
                        viewMode = CalendarViewMode.CALENDAR
                    }
                )
            }
            CalendarViewMode.YEAR -> {
                YearPickerGrid(
                    selectedYear = currentMonth.year,
                    onYearSelected = { year ->
                        currentMonth = YearMonth.of(year, currentMonth.monthValue)
                        selectedDate = currentMonth.atDay(1)
                        viewMode = CalendarViewMode.CALENDAR
                    }
                )
            }
            CalendarViewMode.CALENDAR -> {
                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = {
                        if (slideForward) {
                            (slideInHorizontally(
                                animationSpec = tween(300),
                                initialOffsetX = { it }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { -it }
                            ))
                        } else {
                            (slideInHorizontally(
                                animationSpec = tween(300),
                                initialOffsetX = { -it }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { it }
                            ))
                        }
                    },
                    label = "calendar_slide"
                ) { month ->
                    CalendarGrid(
                        yearMonth    = month,
                        selectedDate = selectedDate,
                        today        = today,
                        onDateClick  = { date ->
                            if (date.month == month.month) selectedDate = date
                        }
                    )
                }
            }
        }

        HorizontalDivider(
            color = Color(0xFFEEEEEE),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        SelectedDateStats(
            date  = selectedDate,
            stats = selectedStats
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun WeeklyLineChart() {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val dataList = days.map { getDummyStats(it) }

    val goal = AppGoals.calories

    val kcalData    = dataList.map { it?.kcal?.toFloat() ?: 0f }
    val carbsData   = dataList.map { it?.carbs?.toFloat() ?: 0f }
    val proteinData = dataList.map { it?.protein?.toFloat() ?: 0f }
    val fatData     = dataList.map { it?.fat?.toFloat() ?: 0f }

    var selectedLines by remember {
        mutableStateOf(setOf("칼로리", "탄수화물", "단백질", "지방"))
    }

    val lineColors = mapOf(
        "칼로리"  to Color(0xFFEF5350),
        "탄수화물" to Color(0xFF4CAF50),
        "단백질"  to Color(0xFF5B9BD5),
        "지방"   to Color(0xFFE6A020)
    )

    val lineData = mapOf(
        "칼로리"  to kcalData,
        "탄수화물" to carbsData,
        "단백질"  to proteinData,
        "지방"   to fatData
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "최근 7일 달성률 (%)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    "목표: $goal kcal",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                lineColors.forEach { (label, color) ->
                    val isSelected = label in selectedLines
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) color else Color(0xFFEEEEEE))
                            .clickable {
                                selectedLines = if (isSelected && selectedLines.size > 1) {
                                    selectedLines - label
                                } else if (!isSelected) {
                                    selectedLines + label
                                } else selectedLines
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else Color(0xFF888888),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val padLeft = 50f
                val padBottom = 30f
                val chartW = width - padLeft
                val chartH = height - padBottom

                // 실제 데이터 최대 퍼센트 계산 (동적)
                val allPercents = lineData.flatMap { (label, data) ->
                    if (label !in selectedLines) return@flatMap emptyList()
                    data.map { value ->
                        val goalVal = when (label) {
                            "칼로리"  -> AppGoals.calories.toFloat()
                            "탄수화물" -> AppGoals.carbs.toFloat()
                            "단백질"  -> AppGoals.protein.toFloat()
                            "지방"   -> AppGoals.fat.toFloat()
                            else -> 1f
                        }
                        if (goalVal > 0) (value / goalVal) * 100f else 0f
                    }
                }
                val maxPercent = ((allPercents.maxOrNull() ?: 100f)
                    .coerceAtLeast(110f) / 50f).toInt() * 50f + 50f

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#999999")
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                val stepCount = (maxPercent / 50f).toInt()
                for (i in 0..stepCount) {
                    val step = i * 50f
                    val y = chartH - (step / maxPercent) * chartH

                    drawLine(
                        color = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
                        start = androidx.compose.ui.geometry.Offset(padLeft, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1f
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        "${step.toInt()}%",
                        padLeft - 4f,
                        y + 10f,
                        paint
                    )
                }

                // Goal 100% 라인
                val goalY = chartH - (100f / maxPercent) * chartH
                drawLine(
                    color = androidx.compose.ui.graphics.Color(0xFF888888),
                    start = androidx.compose.ui.geometry.Offset(padLeft, goalY),
                    end = androidx.compose.ui.geometry.Offset(width, goalY),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                )

                val goalPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#888888")
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "Goal",
                    width - 4f,
                    goalY - 6f,
                    goalPaint
                )

                // 각 라인 그리기
                lineData.forEach { (label, data) ->
                    if (label !in selectedLines) return@forEach
                    val color = lineColors[label] ?: return@forEach

                    val points = data.mapIndexed { i, value ->
                        val x = padLeft + i * (chartW / (days.size - 1).toFloat())
                        val goalVal = when (label) {
                            "칼로리"  -> AppGoals.calories.toFloat()
                            "탄수화물" -> AppGoals.carbs.toFloat()
                            "단백질"  -> AppGoals.protein.toFloat()
                            "지방"   -> AppGoals.fat.toFloat()
                            else -> 1f
                        }
                        val percent = if (goalVal > 0) (value / goalVal) * 100f else 0f
                        val y = chartH - (percent / maxPercent * chartH).coerceIn(0f, chartH)
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = color,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 3f
                        )
                    }

                    points.forEach { point ->
                        drawCircle(color = color, radius = 5f, center = point)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { date ->
                    Text(
                        "${date.monthValue}/${date.dayOfMonth}",
                        fontSize = 10.sp,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MonthNavigator(
    currentMonth: YearMonth,
    viewMode: CalendarViewMode,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onMonthLabelClick: () -> Unit,
    onYearLabelClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "이전 달",
                tint = Color(0xFF444444),
                modifier = Modifier.size(28.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${currentMonth.year}년",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewMode == CalendarViewMode.YEAR) Color(0xFF5B9BD5)
                else Color(0xFF1A1A1A),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onYearLabelClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${currentMonth.monthValue}월",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewMode == CalendarViewMode.MONTH) Color(0xFF5B9BD5)
                else Color(0xFF1A1A1A),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMonthLabelClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "다음 달",
                tint = Color(0xFF444444),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun MonthPickerGrid(
    currentYear: Int,
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit
) {
    val months = listOf(
        "1월", "2월", "3월", "4월",
        "5월", "6월", "7월", "8월",
        "9월", "10월", "11월", "12월"
    )

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        months.chunked(4).forEachIndexed { rowIndex, rowMonths ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowMonths.forEachIndexed { colIndex, label ->
                    val monthNum = rowIndex * 4 + colIndex + 1
                    val isSelected = monthNum == selectedMonth
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp)
                            .aspectRatio(1.8f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF5B9BD5) else Color.Transparent
                            )
                            .clickable { onMonthSelected(monthNum) }
                    ) {
                        Text(
                            label,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF1A1A1A)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun YearPickerGrid(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val startYear = (selectedYear / 10) * 10 - 1
    val years = (startYear..startYear + 11).toList()

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        years.chunked(4).forEach { rowYears ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowYears.forEach { year ->
                    val isSelected = year == selectedYear
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp)
                            .aspectRatio(1.8f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF5B9BD5) else Color.Transparent
                            )
                            .clickable { onYearSelected(year) }
                    ) {
                        Text(
                            "$year",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF1A1A1A)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val dayLabels      = listOf("일", "월", "화", "수", "목", "금", "토")
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth    = yearMonth.lengthOfMonth()
    val prevMonth      = yearMonth.minusMonths(1)
    val prevDays       = prevMonth.lengthOfMonth()

    val cells = mutableListOf<LocalDate>()
    for (i in firstDayOfWeek - 1 downTo 0) {
        cells.add(prevMonth.atDay(prevDays - i))
    }
    for (d in 1..daysInMonth) {
        cells.add(yearMonth.atDay(d))
    }
    val remaining = 42 - cells.size
    for (d in 1..remaining) {
        cells.add(yearMonth.plusMonths(1).atDay(d))
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color(0xFF999999),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                week.forEach { date ->
                    val isCurrentMonth = date.month == yearMonth.month
                    val isSelected     = date == selectedDate
                    val isToday        = date == today
                    val stats          = if (isCurrentMonth) getDummyStats(date) else null
                    val dotColor       = if (stats != null) calorieDotColor(stats.kcal)
                    else Color.Transparent

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable(enabled = isCurrentMonth) {
                                onDateClick(date)
                            }
                    ) {
                        if (dotColor != Color.Transparent || isSelected || isToday) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isToday && dotColor != Color.Transparent -> dotColor
                                            isToday    -> Color(0xFFDDEECC)
                                            isSelected && dotColor != Color.Transparent -> dotColor
                                            isSelected -> Color(0xFFDDEECC)
                                            else       -> dotColor
                                        }
                                    )
                                    .then(
                                        if (isToday)
                                            Modifier.border(
                                                width = 2.dp,
                                                color = Color(0xFF88BB44),
                                                shape = CircleShape
                                            )
                                        else Modifier
                                    )
                            )
                        }

                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold
                            else FontWeight.Normal,
                            color = when {
                                !isCurrentMonth -> Color(0xFFCCCCCC)
                                isSelected || isToday || dotColor != Color.Transparent
                                    -> Color(0xFF1A1A1A)
                                else            -> Color(0xFF444444)
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedDateStats(date: LocalDate, stats: DailyStats?) {
    val month     = date.monthValue
    val day       = date.dayOfMonth
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 8.dp)
    ) {
        Text(
            text = "${month}월 ${day}일 (${dayOfWeek})",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (stats == null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("기록된 식단이 없습니다.", fontSize = 15.sp, color = Color(0xFFAAAAAA))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("총 섭취 칼로리", fontSize = 15.sp, color = Color(0xFF999999))
                Text(
                    "${stats.kcal} kcal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientStatItem("탄수화물", "${stats.carbs}g",   Color(0xFF4CAF50))
                NutrientStatItem("단백질",  "${stats.protein}g", Color(0xFF5B9BD5))
                NutrientStatItem("지방",    "${stats.fat}g",     Color(0xFFE6A020))
            }
        }
    }
}

@Composable
fun NutrientStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 13.sp, color = Color(0xFF999999))
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
    }
}