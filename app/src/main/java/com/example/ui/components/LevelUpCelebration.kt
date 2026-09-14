package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*
import com.example.viewmodel.MythicViewModel
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun LevelUpCelebration(viewModel: MythicViewModel) {
    val profile by viewModel.profile.collectAsState()
    
    // We only want to trigger this if the level INCREASES during the active session.
    var previousLevel by remember { mutableStateOf(profile?.level ?: 1) }
    var showCelebration by remember { mutableStateOf(false) }
    var celebratedLevel by remember { mutableStateOf(1) }
    
    LaunchedEffect(profile?.level) {
        val currentLevel = profile?.level ?: 1
        if (currentLevel > previousLevel && previousLevel > 0) {
            // Level up detected!
            celebratedLevel = currentLevel
            showCelebration = true
            
            // Auto hide after 5 seconds
            delay(5000)
            showCelebration = false
        }
        previousLevel = currentLevel
    }
    
    if (showCelebration) {
        Dialog(
            onDismissRequest = { showCelebration = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                // Lottie Animation
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.level_up))
                val progress by animateLottieCompositionAsState(
                    composition,
                    iterations = 1,
                    restartOnPlay = true
                )
                
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Text overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "RANK UP!",
                        color = Color(0xFFFFD700), // Gold
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                blurRadius = 15f
                            )
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "You have reached Level $celebratedLevel",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                blurRadius = 10f
                            )
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Your Guild grows stronger.",
                        color = Color(0xDDFFFFFF),
                        fontSize = 16.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                blurRadius = 10f
                            )
                        )
                    )
                }
            }
        }
    }
}
