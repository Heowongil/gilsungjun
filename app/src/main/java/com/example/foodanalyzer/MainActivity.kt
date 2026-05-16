package com.example.foodanalyzer

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import com.example.foodanalyzer.ui.theme.FoodAnalyzerTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.foodanalyzer.data.AppDatabase
import com.example.foodanalyzer.data.DatabaseInitializer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.CircularProgressIndicator

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home     : BottomNavItem("home",     "홈",     Icons.Default.Home)
    object Analysis : BottomNavItem("analysis", "식단분석", Icons.Default.Search)
    object Stats    : BottomNavItem("stats",    "통계",   Icons.Default.BarChart)
    object Settings : BottomNavItem("settings", "설정",   Icons.Default.Settings)
}

fun isOnline(context: android.content.Context): Boolean {
    val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

class MainActivity : ComponentActivity() {
    private var isOnlineState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        isOnlineState.value = isOnline(this)

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isOnlineState.value = true
            }
            override fun onLost(network: android.net.Network) {
                isOnlineState.value = false
            }
            override fun onUnavailable() {
                isOnlineState.value = false
            }
            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities
            ) {
                isOnlineState.value = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
            }
        }
        cm.registerDefaultNetworkCallback(networkCallback)

        val database = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            DatabaseInitializer.initializeIfNeeded(this@MainActivity, database)
        }

        AppGoals.load(this)
        AppProfile.load(this)

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            val dao = db.dailyLogDao()
            val allLogs = dao.getAllLogs()
            val grouped = allLogs.groupBy { it.mealType to it.date }
            grouped.forEach { (key, logList) ->
                val (mealLabel, date) = key
                val mealType = MealType.entries.find { it.label == mealLabel } ?: return@forEach
                val mealKey = "${date}_${mealType.name}"
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
                AppMealData.mealResultMap[mealKey] = MealResult(foods)
            }
        }

        setContent {
            FoodAnalyzerTheme {
                val auth = FirebaseAuth.getInstance()
                val online by isOnlineState

                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                var isFirstLogin by remember { mutableStateOf(false) }
                var needOnboarding by remember { mutableStateOf(false) }
                var isCheckingOnboarding by remember { mutableStateOf(false) }

                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        isCheckingOnboarding = true
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            try {
                                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users").document(uid)
                                    .collection("profile").document("data")
                                    .get().await()
                                if (doc.exists()) {
                                    if (isFirstLogin) {
                                        // 로그인할 때만 초기화 후 복원
                                        val db = AppDatabase.getInstance(this@MainActivity)
                                        db.dailyLogDao().deleteAll()
                                        AppMealData.mealResultMap.clear()
                                        AppMealData.photoMap.clear()
                                        com.example.foodanalyzer.data.FirestoreService.restore(this@MainActivity)
                                    }
                                    needOnboarding = false
                                } else {
                                    needOnboarding = true
                                }
                            } catch (e: Exception) {
                                needOnboarding = true
                            }
                        }
                        isCheckingOnboarding = false
                    }
                }

                when {
                    !isLoggedIn && online -> LoginScreen(onLoginSuccess = {
                        isFirstLogin = true
                        isLoggedIn = true
                    })
                    !isLoggedIn && !online -> OfflineScreen()
                    isCheckingOnboarding -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF4CAF50))
                        }
                    }
                    needOnboarding -> OnboardingScreen(onComplete = {
                        needOnboarding = false
                        lifecycleScope.launch {
                            try {
                                com.example.foodanalyzer.data.FirestoreService.backup(this@MainActivity)
                            } catch (e: Exception) {
                                android.util.Log.e("Onboarding", "백업 실패: ${e.message}")
                            }
                        }
                    })
                    else -> MainScreen(isOnline = online)
                }
            }
        }
    }
}

@Composable
fun OfflineScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4E8)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFAAAAAA)
            )
            Text(
                "인터넷 연결이 필요합니다",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                "처음 사용 시 로그인이 필요합니다.\n인터넷 연결 후 다시 시도해주세요.",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun MainScreen(isOnline: Boolean = true) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Analysis,
        BottomNavItem.Stats,
        BottomNavItem.Settings
    )

    Scaffold(
        topBar = {
            if (!isOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE57373))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "오프라인 상태 · 식단 분석 불가",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val currentRoute = navController
                    .currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home")     { HomeScreen() }
            composable("analysis") { AnalysisScreen() }
            composable("stats")    { StatsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}