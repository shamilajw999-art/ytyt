package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) LiquidGlassBackgroundDark else LiquidGlassBackgroundLight
    
    val liquidGlassBrush = Brush.linearGradient(
        colors = listOf(
            bgColor,
            bgColor.copy(alpha = bgColor.alpha * 0.7f),
            bgColor.copy(alpha = bgColor.alpha * 0.4f),
            bgColor
        )
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            if (isDark) LiquidGlassBorderDark else LiquidGlassBorderLight,
            Color.Transparent,
            if (isDark) LiquidGlassBorderDark.copy(alpha = 0.1f) else LiquidGlassBorderLight.copy(alpha = 0.1f)
        )
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(liquidGlassBrush)
            .border(
                width = 1.5.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(32.dp)
            ),
        content = content
    )
}
