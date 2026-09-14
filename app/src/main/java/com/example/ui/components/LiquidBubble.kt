package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import com.example.ui.theme.*

/**
 * LiquidGlassBubble wrapper representing the new Liquid Bubble design system.
 * Simulates BackdropFilter + ClipRRect + BoxShadow from Flutter in Compose.
 */
@Composable
fun LiquidBubble(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    radius: Int = 32,
    content: @Composable () -> Unit
) {
    val bubbleShape = RoundedCornerShape(radius.dp)
    
    val bgColor = if (isDark) LiquidGlassBackgroundDark else LiquidGlassBackgroundLight
    val borderColor = if (isDark) LiquidGlassBorderDark else LiquidGlassBorderLight

    // Adding basic glassmorphic modifiers. 
    // True backdrop blur (RenderEffect) is possible but for wide compatibility, 
    // we use a translucent fill and bright borders for refraction simulation.
    Box(
        modifier = modifier
            .clip(bubbleShape)
            .background(bgColor, bubbleShape)
            .border(1.dp, borderColor, bubbleShape)
    ) {
        content()
    }
}
