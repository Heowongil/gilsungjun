package com.example.foodanalyzer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────────────────────────────
// 색상
// ───────────────────────────────────────────────
val SettingsBg   = Color(0xFFF7F7F7)
val ProfileGreen = Color(0xFF6ECBA0)
val CardWhite    = Color(0xFFFFFFFF)
val DividerGray  = Color(0xFFF0F0F0)
val LogoutRed    = Color(0xFFE57373)

// ───────────────────────────────────────────────
// 사용자 프로필 데이터 모델
// ───────────────────────────────────────────────
data class UserProfile(
    val nickname: String = "고순이에요",
    val height: String   = "190.0",
    val weight: String   = "88.0",
    val age: String      = "23",
    val gender: String   = "남성"
)

// ───────────────────────────────────────────────
// 화면 단계
// ───────────────────────────────────────────────
enum class SettingsStep { MAIN, EDIT_PROFILE }

// ───────────────────────────────────────────────
// SettingsScreen
// ───────────────────────────────────────────────
@Composable
fun SettingsScreen() {
    var step         by remember { mutableStateOf(SettingsStep.MAIN) }
    var profile by remember {
        mutableStateOf(
            UserProfile(
                nickname = AppProfile.nickname,
                height   = AppProfile.height,
                weight   = AppProfile.weight,
                age      = AppProfile.age,
                gender   = AppProfile.gender
            )
        )
    }
    var showLogout   by remember { mutableStateOf(false) }
    var showDelete   by remember { mutableStateOf(false) }

    when (step) {
        SettingsStep.MAIN -> {
            SettingsMainScreen(
                profile      = profile,
                onEditClick  = { step = SettingsStep.EDIT_PROFILE },
                onLogout     = { showLogout = true },
                onDeleteUser = { showDelete = true }
            )
        }
        SettingsStep.EDIT_PROFILE -> {
            ProfileEditScreen(
                profile  = profile,
                onBack   = { step = SettingsStep.MAIN },
                onSave = { updated ->
                    profile = updated
                    AppProfile.nickname = updated.nickname
                    AppProfile.height   = updated.height
                    AppProfile.weight   = updated.weight
                    AppProfile.age      = updated.age
                    AppProfile.gender   = updated.gender
                    step = SettingsStep.MAIN
                }
            )
        }
    }

    if (showLogout) {
        ConfirmDialog(
            title        = "로그아웃",
            message      = "정말 로그아웃 하시겠어요?",
            confirmText  = "로그아웃",
            confirmColor = LogoutRed,
            onConfirm    = { showLogout = false },
            onDismiss    = { showLogout = false }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title        = "회원 탈퇴",
            message      = "정말 탈퇴 하시겠어요?\n모든 데이터가 삭제됩니다.",
            confirmText  = "탈퇴하기",
            confirmColor = LogoutRed,
            onConfirm    = { showDelete = false },
            onDismiss    = { showDelete = false }
        )
    }
}

// ───────────────────────────────────────────────
// 설정 메인 화면
// ───────────────────────────────────────────────
@Composable
fun SettingsMainScreen(
    profile: UserProfile,
    onEditClick: () -> Unit,
    onLogout: () -> Unit,
    onDeleteUser: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBg)
    ) {
        Text(
            text = "설정",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 20.dp)
        )

        // 프로필 카드
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            // 갤러리 런처
            val profileImageLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { AppProfile.profileImageUri = it }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                // 편집 버튼 (우측 상단)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0F0F0))
                        .clickable { onEditClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("편집", fontSize = 12.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                }
                // 프로필 아이콘 + 닉네임
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 프로필 사진 (클릭 시 갤러리 열기)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(ProfileGreen)
                            .clickable { profileImageLauncher.launch("image/*") }
                    ) {
                        if (AppProfile.profileImageUri != null) {
                            // 등록된 사진 표시
                            Image(
                                painter = rememberAsyncImagePainter(AppProfile.profileImageUri),
                                contentDescription = "프로필 사진",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // 기본 아이콘
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "프로필",
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // 사진 변경 안내 텍스트
                    Text(
                        "탭하여 사진 변경",
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(profile.nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 메뉴 카드
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsMenuItem(icon = "→", label = "로그아웃",  labelColor = Color(0xFF1A1A1A), onClick = onLogout)
                HorizontalDivider(color = DividerGray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsMenuItem(icon = "👤", label = "회원 탈퇴", labelColor = LogoutRed,         onClick = onDeleteUser)
            }
        }
    }
}

// ───────────────────────────────────────────────
// 프로필 수정 화면
// ───────────────────────────────────────────────
@Composable
fun ProfileEditScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var nickname by remember { mutableStateOf(profile.nickname) }
    var height   by remember { mutableStateOf(profile.height) }
    var weight   by remember { mutableStateOf(profile.weight) }
    var age      by remember { mutableStateOf(profile.age) }
    var gender   by remember { mutableStateOf(profile.gender) }
    var genderExpanded by remember { mutableStateOf(false) }

    val activityOptions = listOf("운동 거의 안함", "주 1-3회 운동", "주 3-5회 운동", "주 6-7회 운동", "매일, 하루 2번")
    var selectedActivity by remember { mutableStateOf(AppProfile.selectedActivity) }

    val goalOptions = listOf("체중 감소", "유지", "근육량 증가")
    var selectedGoal     by remember { mutableStateOf(AppProfile.selectedGoal) }

    var targetKcal    by remember { mutableStateOf(AppGoals.calories.toString()) }
    var targetCarbs   by remember { mutableStateOf(AppGoals.carbs.toString()) }
    var targetProtein by remember { mutableStateOf(AppGoals.protein.toString()) }
    var targetFat     by remember { mutableStateOf(AppGoals.fat.toString()) }

    val genderOptions = listOf("남성", "여성")

    // ── 권장 칼로리 계산 (해리스-베네딕트 공식) ──
    val recommendedKcal = remember(height, weight, age, gender, selectedActivity, selectedGoal) {
        val h = height.toDoubleOrNull() ?: 170.0
        val w = weight.toDoubleOrNull() ?: 70.0
        val a = age.toIntOrNull() ?: 25

        // 기초대사량 (BMR)
        val bmr = if (gender == "남성") {
            88.362 + (13.397 * w) + (4.799 * h) - (5.677 * a)
        } else {
            447.593 + (9.247 * w) + (3.098 * h) - (4.330 * a)
        }

        // 활동계수
        val activityFactor = when (selectedActivity) {
            "운동 거의 안함"  -> 1.2
            "주 1-3회 운동"  -> 1.375
            "주 3-5회 운동"  -> 1.55
            "주 6-7회 운동"  -> 1.725
            "매일, 하루 2번" -> 1.9
            else             -> 1.55
        }

        // 목표 보정
        val goalFactor = when (selectedGoal) {
            "체중 감소"   -> 0.85
            "근육량 증가" -> 1.1
            else          -> 1.0
        }

        (bmr * activityFactor * goalFactor).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBg)
    ) {
        // ── 상단 앱바 ──
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
                "프로필 수정",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.align(Alignment.Center)
            )
            TextButton(
                onClick = {
                    AppGoals.calories            = targetKcal.toIntOrNull()    ?: AppGoals.calories
                    AppGoals.carbs               = targetCarbs.toIntOrNull()   ?: AppGoals.carbs
                    AppGoals.protein             = targetProtein.toIntOrNull() ?: AppGoals.protein
                    AppGoals.fat                 = targetFat.toIntOrNull()     ?: AppGoals.fat
                    AppProfile.selectedActivity  = selectedActivity
                    AppProfile.selectedGoal      = selectedGoal
                    onSave(UserProfile(nickname, height, weight, age, gender))
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("저장", fontSize = 15.sp, color = ProfileGreen, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ════════════════════════════════
            // 기본 정보 섹션
            // ════════════════════════════════
            Text("기본 정보", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 이름
                    EditField(label = "이름") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicEditText(value = nickname, onValueChange = { nickname = it }, keyboardType = KeyboardType.Text)
                        }
                    }

                    HorizontalDivider(color = DividerGray, thickness = 1.dp)

                    // 키 + 몸무게
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            EditField(label = "키") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BasicEditText(value = height, onValueChange = { height = it }, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                                    Text("cm", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            EditField(label = "몸무게") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BasicEditText(value = weight, onValueChange = { weight = it }, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                                    Text("kg", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DividerGray, thickness = 1.dp)

                    // 나이 + 성별
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            EditField(label = "나이") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BasicEditText(value = age, onValueChange = { age = it }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                                    Text("세", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            EditField(label = "성별") {
                                Box {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).clickable { genderExpanded = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(gender, fontSize = 15.sp, color = Color(0xFF1A1A1A))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
                                    }
                                    DropdownMenu(
                                        expanded = genderExpanded,
                                        onDismissRequest = { genderExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        genderOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option, fontSize = 15.sp) },
                                                onClick = { gender = option; genderExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════
            // 목표 설정 섹션 (활동량 + 목표 + 영양소 한 카드)
            // ════════════════════════════════
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("목표 설정", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))

                // 권장 칼로리 뱃지
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFD6F0D6))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "권장: $recommendedKcal kcal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // ── 나의 활동량 ──
                    Text("나의 활동량", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                    val activityRows = activityOptions.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activityRows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DividerGray, thickness = 1.dp)

                    // ── 나의 목표 ──
                    Text("나의 목표", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    HorizontalDivider(color = DividerGray, thickness = 1.dp)

                    // ── 목표 칼로리 ──
                    NutrientGoalField(label = "목표 칼로리",   value = targetKcal,    unit = "kcal", onValueChange = { targetKcal = it })

                    HorizontalDivider(color = DividerGray, thickness = 1.dp)

                    // ── 목표 탄수화물 ──
                    NutrientGoalField(label = "목표 탄수화물", value = targetCarbs,   unit = "g",    onValueChange = { targetCarbs = it })

                    HorizontalDivider(color = DividerGray, thickness = 1.dp)

                    // ── 목표 단백질 ──
                    NutrientGoalField(label = "목표 단백질",   value = targetProtein, unit = "g",    onValueChange = { targetProtein = it })

                    HorizontalDivider(color = DividerGray, thickness = 1.dp)

                    // ── 목표 지방 ──
                    NutrientGoalField(label = "목표 지방",     value = targetFat,     unit = "g",    onValueChange = { targetFat = it })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ───────────────────────────────────────────────
// 목표 영양소 입력 필드
// ───────────────────────────────────────────────
@Composable
fun NutrientGoalField(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, color = Color(0xFFAAAAAA))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicEditText(
                value = value,
                onValueChange = onValueChange,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            Text(unit, fontSize = 13.sp, color = Color(0xFFAAAAAA))
        }
    }
}

// ───────────────────────────────────────────────
// 라벨 + 입력 필드 묶음
// ───────────────────────────────────────────────
@Composable
fun EditField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, color = Color(0xFFAAAAAA))
        content()
    }
}

// ───────────────────────────────────────────────
// 기본 텍스트 입력
// ───────────────────────────────────────────────
@Composable
fun BasicEditText(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 15.sp,
            color = Color(0xFF1A1A1A)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        singleLine = true
    )
}

// ───────────────────────────────────────────────
// 메뉴 아이템
// ───────────────────────────────────────────────
@Composable
fun SettingsMenuItem(
    icon: String,
    label: String,
    labelColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, fontSize = 18.sp)
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = labelColor)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ───────────────────────────────────────────────
// 공통 확인 다이얼로그
// ───────────────────────────────────────────────
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A))
        },
        text = {
            Text(message, fontSize = 15.sp, color = Color(0xFF666666))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF999999), fontSize = 15.sp)
            }
        }
    )
}