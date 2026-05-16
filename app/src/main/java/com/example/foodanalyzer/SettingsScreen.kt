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
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.foodanalyzer.data.FirestoreService
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

val SettingsBg   = Color(0xFFF7F7F7)
val ProfileGreen = Color(0xFF6ECBA0)
val CardWhite    = Color(0xFFFFFFFF)
val DividerGray  = Color(0xFFF0F0F0)
val LogoutRed    = Color(0xFFE57373)

data class UserProfile(
    val nickname: String = "사용자",
    val height: String   = "170.0",
    val weight: String   = "65.0",
    val age: String      = "25",
    val gender: String   = "남성"
)

enum class SettingsStep { MAIN, EDIT_PROFILE }

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var step    by remember { mutableStateOf(SettingsStep.MAIN) }
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
    var showLogout  by remember { mutableStateOf(false) }
    var showLogoutStep2 by remember { mutableStateOf(false) }
    var showDelete  by remember { mutableStateOf(false) }

    when (step) {
        SettingsStep.MAIN -> {
            SettingsMainScreen(
                profile      = profile,
                onEditClick  = { step = SettingsStep.EDIT_PROFILE },
                onLogout     = { showLogout = true },
                onDeleteUser = { showDelete = true },
                onBackup     = {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            FirestoreService.backup(context)
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "백업 완료!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Firestore", "백업 실패: ${e.message}")
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "백업 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onRestore = {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            FirestoreService.restore(context)
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "복원 완료! 앱을 재시작합니다.", Toast.LENGTH_SHORT).show()
                                // 앱 재시작
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "복원 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }
        SettingsStep.EDIT_PROFILE -> {
            ProfileEditScreen(
                profile  = profile,
                onBack   = { step = SettingsStep.MAIN },
                onSave   = { updated ->
                    profile = updated
                    AppProfile.nickname = updated.nickname
                    AppProfile.height   = updated.height
                    AppProfile.weight   = updated.weight
                    AppProfile.age      = updated.age
                    AppProfile.gender   = updated.gender
                    AppGoals.save(context)
                    AppProfile.save(context)
                    step = SettingsStep.MAIN
                }
            )
        }
    }

    // 1단계: 백업 안내
    if (showLogout) {
        ConfirmDialog(
            title       = "로그아웃 전 백업",
            message     = "로그아웃하면 로컬 데이터가 삭제됩니다.\n먼저 클라우드에 백업하시겠어요?",
            confirmText = "백업하고 계속",
            confirmColor = ProfileGreen,
            onConfirm   = {
                showLogout = false
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        FirestoreService.backup(context)
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "백업 완료!", Toast.LENGTH_SHORT).show()
                            showLogoutStep2 = true
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "백업 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDismiss   = {
                showLogout = false
                showLogoutStep2 = true  // 백업 건너뛰고 바로 로그아웃 확인으로
            }
        )
    }

// 2단계: 최종 로그아웃 확인
    if (showLogoutStep2) {
        ConfirmDialog(
            title       = "로그아웃",
            message     = "정말 로그아웃 하시겠어요?\n로컬 데이터가 삭제됩니다.",
            confirmText = "로그아웃",
            confirmColor = LogoutRed,
            onConfirm   = {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = com.example.foodanalyzer.data.AppDatabase.getInstance(context)
                    db.dailyLogDao().deleteAll()
                }
                AppMealData.mealResultMap.clear()
                AppMealData.photoMap.clear()
                AppProfile.nickname = "사용자"
                AppProfile.height   = "170.0"
                AppProfile.weight   = "65.0"
                AppProfile.age      = "25"
                AppProfile.gender   = "남성"
                AppProfile.save(context)

                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                    context,
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                    ).build()
                )
                googleSignInClient.signOut().addOnCompleteListener {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    showLogoutStep2 = false
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                }
            },
            onDismiss   = { showLogoutStep2 = false }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title        = "회원 탈퇴",
            message      = "정말 탈퇴 하시겠어요?\n모든 데이터가 영구 삭제됩니다.",
            confirmText  = "탈퇴하기",
            confirmColor = LogoutRed,
            onConfirm    = {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                        // 1. Firestore 데이터 먼저 삭제
                        if (uid != null) {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val userRef = db.collection("users").document(uid)
                            userRef.collection("dailyLogs").get().await().documents.forEach {
                                it.reference.delete().await()
                            }
                            userRef.collection("profile").document("data").delete().await()
                            userRef.collection("goals").document("data").delete().await()
                            userRef.delete().await()
                        }

                        // 2. Room DB 초기화
                        val roomDb = com.example.foodanalyzer.data.AppDatabase.getInstance(context)
                        roomDb.dailyLogDao().deleteAll()

                    } catch (e: Exception) {
                        android.util.Log.e("Delete", "데이터 삭제 실패: ${e.message}")
                    }

                    withContext(Dispatchers.Main) {
                        AppMealData.mealResultMap.clear()
                        AppMealData.photoMap.clear()
                        AppProfile.nickname = "사용자"
                        AppProfile.height   = "170.0"
                        AppProfile.weight   = "65.0"
                        AppProfile.age      = "25"
                        AppProfile.gender   = "남성"
                        AppProfile.save(context)

                        // 3. Firebase Auth 계정 삭제 (마지막에)
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            ?.delete()
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                                        context,
                                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                        ).build()
                                    )
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        showDelete = false
                                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        context.startActivity(intent)
                                    }
                                }
                            }
                    }
                }
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
fun SettingsMainScreen(
    profile: UserProfile,
    onEditClick: () -> Unit,
    onLogout: () -> Unit,
    onDeleteUser: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            val profileImageLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { AppProfile.profileImageUri = it }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(ProfileGreen)
                            .clickable { profileImageLauncher.launch("image/*") }
                    ) {
                        if (AppProfile.profileImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(AppProfile.profileImageUri),
                                contentDescription = "프로필 사진",
                                modifier = Modifier.size(90.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "프로필",
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("탭하여 사진 변경", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(profile.nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsMenuItem(icon = "☁️", label = "클라우드에 백업", labelColor = Color(0xFF1A1A1A), onClick = onBackup)
                HorizontalDivider(color = DividerGray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsMenuItem(icon = "📥", label = "백업에서 복원", labelColor = Color(0xFF1A1A1A), onClick = onRestore)
                HorizontalDivider(color = DividerGray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsMenuItem(icon = "→", label = "로그아웃", labelColor = Color(0xFF1A1A1A), onClick = onLogout)
                HorizontalDivider(color = DividerGray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsMenuItem(icon = "👤", label = "회원 탈퇴", labelColor = LogoutRed, onClick = onDeleteUser)
            }
        }
    }
}

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
    var selectedGoal by remember { mutableStateOf(AppProfile.selectedGoal) }

    var targetKcal    by remember { mutableStateOf(AppGoals.calories.toString()) }
    var targetCarbs   by remember { mutableStateOf(AppGoals.carbs.toString()) }
    var targetProtein by remember { mutableStateOf(AppGoals.protein.toString()) }
    var targetFat     by remember { mutableStateOf(AppGoals.fat.toString()) }

    val genderOptions = listOf("남성", "여성")

    val recommendedKcal = remember(height, weight, age, gender, selectedActivity, selectedGoal) {
        val h = height.toDoubleOrNull() ?: 170.0
        val w = weight.toDoubleOrNull() ?: 70.0
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

    Column(modifier = Modifier.fillMaxSize().background(SettingsBg)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF1A1A1A))
            }
            Text("프로필 수정", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A), modifier = Modifier.align(Alignment.Center))
            TextButton(
                onClick = {
                    AppGoals.calories           = targetKcal.toIntOrNull()    ?: AppGoals.calories
                    AppGoals.carbs              = targetCarbs.toIntOrNull()   ?: AppGoals.carbs
                    AppGoals.protein            = targetProtein.toIntOrNull() ?: AppGoals.protein
                    AppGoals.fat                = targetFat.toIntOrNull()     ?: AppGoals.fat
                    AppProfile.selectedActivity = selectedActivity
                    AppProfile.selectedGoal     = selectedGoal
                    onSave(UserProfile(nickname, height, weight, age, gender))
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text("저장", fontSize = 15.sp, color = ProfileGreen, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("기본 정보", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    EditField(label = "이름") {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicEditText(value = nickname, onValueChange = { nickname = it }, keyboardType = KeyboardType.Text)
                        }
                    }
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            EditField(label = "키") {
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    BasicEditText(value = height, onValueChange = { height = it }, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                                    Text("cm", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            EditField(label = "몸무게") {
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    BasicEditText(value = weight, onValueChange = { weight = it }, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                                    Text("kg", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            EditField(label = "나이") {
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    BasicEditText(value = age, onValueChange = { age = it }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                                    Text("세", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            EditField(label = "성별") {
                                Box {
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).clickable { genderExpanded = true }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(gender, fontSize = 15.sp, color = Color(0xFF1A1A1A))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
                                    }
                                    DropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }, modifier = Modifier.background(Color.White)) {
                                        genderOptions.forEach { option ->
                                            DropdownMenuItem(text = { Text(option, fontSize = 15.sp) }, onClick = { gender = option; genderExpanded = false })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("목표 설정", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFD6F0D6)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(text = "권장: $recommendedKcal kcal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("나의 활동량", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                    val activityRows = activityOptions.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activityRows.forEach { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { option ->
                                    val isSelected = option == selectedActivity
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if (isSelected) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)).clickable { selectedActivity = option }.padding(vertical = 10.dp, horizontal = 4.dp)) {
                                        Text(text = option, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.White else Color(0xFF444444), textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
                                    }
                                }
                                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    Text("나의 목표", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        goalOptions.forEach { option ->
                            val isSelected = option == selectedGoal
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if (isSelected) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)).clickable { selectedGoal = option }.padding(vertical = 12.dp)) {
                                Text(text = option, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.White else Color(0xFF444444))
                            }
                        }
                    }
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    NutrientGoalField(label = "목표 칼로리",   value = targetKcal,    unit = "kcal", onValueChange = { targetKcal = it })
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    NutrientGoalField(label = "목표 탄수화물", value = targetCarbs,   unit = "g", recommended = "${(recommendedKcal * 0.5 / 4).toInt()}g",  onValueChange = { targetCarbs = it })
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    NutrientGoalField(label = "목표 단백질",   value = targetProtein, unit = "g", recommended = "${(recommendedKcal * 0.25 / 4).toInt()}g", onValueChange = { targetProtein = it })
                    HorizontalDivider(color = DividerGray, thickness = 1.dp)
                    NutrientGoalField(label = "목표 지방",     value = targetFat,     unit = "g", recommended = "${(recommendedKcal * 0.25 / 9).toInt()}g", onValueChange = { targetFat = it }) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NutrientGoalField(label: String, value: String, unit: String, recommended: String = "", onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 13.sp, color = Color(0xFFAAAAAA))
            if (recommended.isNotEmpty()) {
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFD6F0D6)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("권장 $recommended", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5F5)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            BasicEditText(value = value, onValueChange = onValueChange, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            Text(unit, fontSize = 13.sp, color = Color(0xFFAAAAAA))
        }
    }
}

@Composable
fun EditField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, color = Color(0xFFAAAAAA))
        content()
    }
}

@Composable
fun BasicEditText(value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text, modifier: Modifier = Modifier) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Color(0xFF1A1A1A)),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        singleLine = true
    )
}

@Composable
fun SettingsMenuItem(icon: String, label: String, labelColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(icon, fontSize = 18.sp)
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = labelColor)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmText: String, confirmColor: Color, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A1A1A)) },
        text = { Text(message, fontSize = 15.sp, color = Color(0xFF666666)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = confirmColor, fontWeight = FontWeight.Bold, fontSize = 15.sp) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = Color(0xFF999999), fontSize = 15.sp) } }
    )
}