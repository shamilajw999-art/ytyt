package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.AppBackground
import com.example.ui.theme.MythicTheme
import com.example.viewmodel.MythicViewModel

enum class Screen {
    SPLASH, AUTH, ONBOARDING, MAIN, CHAT, SCAN_VISION, FEED, REPORT, SUBSCRIPTION, SETTINGS, ADMIN_APPROVAL
}

enum class Tab {
    HOME, AI, REELS, MAP, PROFILE
}

class MainActivity : ComponentActivity() {
    private val viewModel: MythicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Proactively create WebView cache directories to resolve Chromium "No such file or directory" errors
        try {
            val webViewCacheJs = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            val webViewCacheWasm = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!webViewCacheJs.exists()) webViewCacheJs.mkdirs()
            if (!webViewCacheWasm.exists()) webViewCacheWasm.mkdirs()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Could not pre-create WebView cache dirs: ${e.message}")
        }
        
        // Initialize Notifications
        com.example.notifications.HeritageNotificationManager.createNotificationChannel(this)

        // Initialize Google AdMob & Mobile Ads SDK
        com.example.utils.AdMobConfig.initialize(this)

        val initialNavigateTo = intent?.getStringExtra("navigate_to")
        val initialPrompt = intent?.getStringExtra("prompt")

        setContent {
            MythicTheme {
                AppBackground {
                    val currentUser by viewModel.currentUser.collectAsState()
                    val profile by viewModel.profile.collectAsState()
                    var currentScreen by remember {
                        mutableStateOf(
                            if (initialNavigateTo == "subscription") Screen.SUBSCRIPTION else Screen.SPLASH
                        )
                    }
                    var currentTab by remember {
                        mutableStateOf(
                            if (initialNavigateTo == "lumo_story") Tab.AI else Tab.HOME
                        )
                    }
                    
                    com.example.ui.components.LevelUpCelebration(viewModel = viewModel)

                    LaunchedEffect(Unit) {
                        // Trigger daily login streak notification
                        val streakDays = (profile?.streak ?: 1).coerceAtLeast(1)
                        com.example.notifications.HeritageNotificationManager.showStreakNotification(this@MainActivity, streakDays)

                        // Trigger near-site geofence check (Simulating near Sigiriya for demo)
                        com.example.notifications.HeritageNotificationManager.checkLocationAndNotifyNearSite(
                            context = this@MainActivity,
                            userLat = 7.9570,
                            userLng = 80.7603
                        )

                        // If opened via story notification prompt
                        if (!initialPrompt.isNullOrBlank()) {
                            viewModel.sendMythicMessage(initialPrompt)
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent, // Let AppBackground show through
                        bottomBar = {
                            if (currentScreen == Screen.MAIN) {
                                BottomNavBar(
                                    currentTab = currentTab,
                                    onTabSelected = { tab -> currentTab = tab },
                                    isDark = true,
                                    accentColor = Color(0xFF86FC5C),
                                    textPrimary = Color.White
                                )
                            }
                        },
                        floatingActionButton = {}
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom = if (currentScreen == Screen.MAIN) innerPadding.calculateBottomPadding() else 0.dp,
                                    top = if (currentScreen == Screen.SPLASH) 0.dp else innerPadding.calculateTopPadding()
                                )
                        ) {
                            when (currentScreen) {
                                Screen.SPLASH -> {
                                    val userPrefs by viewModel.userPreferences.collectAsState()
                                    com.example.ui.screens.LoadingScreen(
                                        onFinish = {
                                            if (currentUser != null) {
                                                if (userPrefs?.hasCompletedOnboarding == false) {
                                                    currentScreen = Screen.ONBOARDING
                                                } else {
                                                    currentScreen = Screen.MAIN
                                                }
                                            } else {
                                                currentScreen = Screen.AUTH
                                            }
                                        }
                                    )
                                }
                                Screen.AUTH -> {
                                    com.example.ui.screens.AuthScreen(
                                        viewModel = viewModel,
                                        onAuthSuccess = { currentScreen = Screen.ONBOARDING }
                                    )
                                }
                                Screen.ONBOARDING -> {
                                    com.example.ui.screens.OnboardingScreen(
                                        viewModel = viewModel,
                                        onCompleteOnboarding = { currentScreen = Screen.MAIN }
                                    )
                                }
                                Screen.CHAT -> {
                                    BackHandler { currentScreen = Screen.MAIN }
                                    com.example.ui.screens.ChatScreen(
                                        viewModel = viewModel,
                                        onNavigateToSubscription = { currentScreen = Screen.SUBSCRIPTION }
                                    )
                                }
                                Screen.SCAN_VISION -> {
                                    com.example.ui.screens.ARScreen(
                                        viewModel = viewModel,
                                        onDismiss = { currentScreen = Screen.MAIN }
                                    )
                                }
                                Screen.FEED -> {
                                    com.example.ui.screens.FeedScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.MAIN }
                                    )
                                }
                                Screen.REPORT -> {
                                    com.example.ui.screens.ReportScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.MAIN }
                                    )
                                }
                                Screen.SUBSCRIPTION -> {
                                    com.example.ui.screens.SubscriptionScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.MAIN }
                                    )
                                }
                                Screen.SETTINGS -> {
                                    com.example.ui.screens.SettingsScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.MAIN }
                                    )
                                }
                                Screen.ADMIN_APPROVAL -> {
                                    com.example.ui.screens.AdminApprovalScreen(
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.MAIN }
                                    )
                                }
                                Screen.MAIN -> {
                                    when (currentTab) {
                                        Tab.HOME -> com.example.ui.screens.HomeScreen(
                                            viewModel = viewModel,
                                            onNavigateToAR = { currentScreen = Screen.SCAN_VISION },
                                            onNavigateToMythic = { currentTab = Tab.AI },
                                            onNavigateToMap = { currentTab = Tab.MAP },
                                            onNavigateToArticles = { currentScreen = Screen.FEED },
                                            onNavigateToReels = { currentTab = Tab.REELS },
                                            onNavigateToReport = { currentScreen = Screen.REPORT }
                                        )
                                        Tab.AI -> com.example.ui.screens.ChatScreen(
                                            viewModel = viewModel,
                                            onNavigateToSubscription = { currentScreen = Screen.SUBSCRIPTION }
                                        )
                                        Tab.REELS -> com.example.ui.screens.ReelsScreen(viewModel = viewModel)
                                        Tab.MAP -> com.example.ui.screens.MapScreen(viewModel = viewModel, onBack = { currentTab = Tab.HOME })
                                        Tab.PROFILE -> com.example.ui.screens.ProfileScreen(
                                            viewModel = viewModel,
                                            onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                            onLogoutSuccess = { currentScreen = Screen.AUTH },
                                            onNavigateToSubscription = { currentScreen = Screen.SUBSCRIPTION },
                                            onNavigateToAdmin = { currentScreen = Screen.ADMIN_APPROVAL }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Global overlays
                }
            }
        }
    }
}
@Composable
fun BottomNavBar(
    currentTab: Tab,
    onTabSelected: (Tab) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    textPrimary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .height(64.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
            .background(Color(0xFF161616))
            .border(1.dp, Color(0xFF333333), androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItem = @Composable { tab: Tab, icon: String ->
                val isSelected = currentTab == tab
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (isSelected) Color(0xFF2C2C2C) else Color.Transparent)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 20.sp,
                        color = if (isSelected) accentColor else Color.Gray
                    )
                }
            }
            
            navItem(Tab.HOME, "🏠")
            navItem(Tab.AI, "✨")
            navItem(Tab.REELS, "🎬")
            navItem(Tab.MAP, "🗺️")
            navItem(Tab.PROFILE, "👤")
        }
    }
}
