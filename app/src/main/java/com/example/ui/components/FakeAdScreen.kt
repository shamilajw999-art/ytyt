package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FakeAdScreen(onAdFinished: () -> Unit) {
    var timeLeft by remember { mutableStateOf(5) } // Simulate 30s with 5s for usability

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onAdFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Displaying Ad...", color = Color.White, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ad will finish in $timeLeft seconds", color = Color.Gray, fontSize = 16.sp)
        }
    }
}
