package com.example.service

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

data class SubscriptionStyle(
    val textColor: Color,
    val fontWeight: FontWeight,
    val emblem: String,
    val badgeLabel: String,
    val tierName: String
)

object SubscriptionStylingService {
    fun getStyleForTier(tier: String?): SubscriptionStyle {
        return when (tier?.trim()?.lowercase()) {
            "pro" -> SubscriptionStyle(
                textColor = Color(0xFFC0C0C0), // Sleek Silver
                fontWeight = FontWeight.Bold,
                emblem = "⭐",
                badgeLabel = "PRO",
                tierName = "Pro"
            )
            "max", "prime" -> SubscriptionStyle(
                textColor = Color(0xFFFFD700), // Radiant Gold
                fontWeight = FontWeight.ExtraBold,
                emblem = "👑",
                badgeLabel = "PRIME",
                tierName = "Prime"
            )
            else -> SubscriptionStyle(
                textColor = Color.White,
                fontWeight = FontWeight.Normal,
                emblem = "",
                badgeLabel = "",
                tierName = "Free"
            )
        }
    }
}

