package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

@Composable
fun NeonBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_animation")
    
    val bubble1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bubble1_alpha"
    )
    
    val bubble2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bubble2_alpha"
    )

    val bubble3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(5500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bubble3_alpha"
    )
 
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
 
        // Bubble 1 (Neon Lime Green)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF9AF04D).copy(alpha = bubble1Alpha), Color.Transparent)),
            radius = width * 0.45f,
            center = androidx.compose.ui.geometry.Offset(width * 0.15f, height * 0.25f)
        )
 
        // Bubble 2 (Pure Neon Green)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF39FF14).copy(alpha = bubble2Alpha), Color.Transparent)),
            radius = width * 0.55f,
            center = androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.75f)
        )

        // Bubble 3 (Soft Emerald Green)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF2E7D32).copy(alpha = bubble3Alpha), Color.Transparent)),
            radius = width * 0.35f,
            center = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.5f)
        )
    }
}
