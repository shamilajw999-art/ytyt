package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Verified Subscription Tiers for Mythic Accounts.
 */
enum class SubscriptionTier {
    NONE,
    PRO,
    PRIME;

    companion object {
        val MAX = PRIME

        fun fromString(tier: String?): SubscriptionTier {
            return when (tier?.trim()?.lowercase()) {
                "pro" -> PRO
                "max", "prime" -> PRIME
                else -> NONE
            }
        }
    }
}

/**
 * Size definitions for inline emblems and badges.
 */
enum class EmblemSize(
    val badgeSize: Dp,
    val iconSize: Dp,
    val fontSize: Int,
    val spacing: Dp,
    val pillPaddingH: Dp,
    val pillPaddingV: Dp
) {
    SMALL(
        badgeSize = 14.dp,
        iconSize = 9.dp,
        fontSize = 8,
        spacing = 4.dp,
        pillPaddingH = 5.dp,
        pillPaddingV = 1.dp
    ),
    MEDIUM(
        badgeSize = 18.dp,
        iconSize = 12.dp,
        fontSize = 10,
        spacing = 6.dp,
        pillPaddingH = 7.dp,
        pillPaddingV = 2.dp
    ),
    LARGE(
        badgeSize = 24.dp,
        iconSize = 15.dp,
        fontSize = 11,
        spacing = 8.dp,
        pillPaddingH = 9.dp,
        pillPaddingV = 3.dp
    )
}

/**
 * Renders a sleek metallic emblem icon for PRO (Silver) or MAX (Gold).
 * Renders nothing if tier is NONE, free, expired, or invalid.
 */
@Composable
fun SubscriptionEmblem(
    tier: String?,
    modifier: Modifier = Modifier,
    size: EmblemSize = EmblemSize.MEDIUM,
    contentDescription: String? = null
) {
    val subscriptionTier = SubscriptionTier.fromString(tier)
    SubscriptionEmblem(
        tier = subscriptionTier,
        modifier = modifier,
        size = size,
        contentDescription = contentDescription
    )
}

/**
 * Type-safe SubscriptionEmblem composable.
 */
@Composable
fun SubscriptionEmblem(
    tier: SubscriptionTier,
    modifier: Modifier = Modifier,
    size: EmblemSize = EmblemSize.MEDIUM,
    contentDescription: String? = null
) {
    if (tier == SubscriptionTier.NONE) return

    val isPrime = tier == SubscriptionTier.PRIME

    // Metallic Gradients
    val backgroundBrush = if (isPrime) {
        // Radiant Imperial Gold
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFF9C4), // Bright golden sheen
                Color(0xFFFFD700), // Pure Gold
                Color(0xFFFFA000), // Warm amber gold
                Color(0xFFFF6F00)  // Deep rich gold
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        // Polished Metallic Silver / Platinum
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF), // High silver reflection
                Color(0xFFE2E8F0), // Platinum silver
                Color(0xFF94A3B8), // Slate silver
                Color(0xFF64748B)  // Deep metallic rim
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val borderBrush = if (isPrime) {
        Brush.linearGradient(
            listOf(Color(0xFFFFF59D), Color(0xFFFFD700), Color(0xFFFF8F00))
        )
    } else {
        Brush.linearGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF64748B))
        )
    }

    val iconColor = if (isPrime) Color(0xFF3E1F00) else Color(0xFF0F172A)

    Box(
        modifier = modifier
            .size(size.badgeSize)
            .shadow(
                elevation = if (isPrime) 4.dp else 2.dp,
                shape = CircleShape,
                ambientColor = if (isPrime) Color(0xFFFFD700) else Color.White,
                spotColor = if (isPrime) Color(0xFFFF8F00) else Color(0xFF94A3B8)
            )
            .clip(CircleShape)
            .background(backgroundBrush)
            .border(
                width = if (isPrime) 1.2.dp else 1.dp,
                brush = borderBrush,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isPrime) {
            // Crown / Mythic Star Emblem for Prime
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = contentDescription ?: "Mythic Prime Verified",
                tint = iconColor,
                modifier = Modifier.size(size.iconSize)
            )
        } else {
            // Sleek Shield / Star Emblem for Pro
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = contentDescription ?: "Mythic Pro Verified",
                tint = iconColor,
                modifier = Modifier.size(size.iconSize)
            )
        }
    }
}

/**
 * Renders a full metallic pill badge with emblem icon and tier label ("PRO" or "PRIME").
 */
@Composable
fun SubscriptionBadge(
    tier: String?,
    modifier: Modifier = Modifier,
    size: EmblemSize = EmblemSize.MEDIUM
) {
    val subscriptionTier = SubscriptionTier.fromString(tier)
    if (subscriptionTier == SubscriptionTier.NONE) return

    val isPrime = subscriptionTier == SubscriptionTier.PRIME

    val backgroundBrush = if (isPrime) {
        Brush.horizontalGradient(
            listOf(Color(0xFFFFF9C4), Color(0xFFFFD700), Color(0xFFFFA000))
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF94A3B8))
        )
    }

    val borderBrush = if (isPrime) {
        Brush.horizontalGradient(
            listOf(Color(0xFFFFF59D), Color(0xFFFFD700), Color(0xFFFF8F00))
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xFFCBD5E1), Color(0xFF64748B))
        )
    }

    val contentColor = if (isPrime) Color(0xFF3E1F00) else Color(0xFF0F172A)

    Row(
        modifier = modifier
            .shadow(
                elevation = if (isPrime) 3.dp else 1.5.dp,
                shape = RoundedCornerShape(100.dp),
                ambientColor = if (isPrime) Color(0xFFFFD700) else Color.White
            )
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(100.dp)
            )
            .padding(horizontal = size.pillPaddingH, vertical = size.pillPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(size.spacing / 2)
    ) {
        Icon(
            imageVector = if (isPrime) Icons.Default.WorkspacePremium else Icons.Default.Star,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size.iconSize)
        )
        Text(
            text = if (isPrime) "PRIME" else "PRO",
            color = contentColor,
            fontSize = size.fontSize.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Helper row to display user display name immediately followed by the subscription emblem.
 */
@Composable
fun SubscriptionNameRow(
    name: String,
    tier: String?,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        color = Color.White
    ),
    emblemSize: EmblemSize = EmblemSize.MEDIUM,
    showBadgeLabel: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(emblemSize.spacing)
    ) {
        Text(
            text = name,
            style = textStyle
        )
        if (showBadgeLabel) {
            SubscriptionBadge(tier = tier, size = emblemSize)
        } else {
            SubscriptionEmblem(tier = tier, size = emblemSize)
        }
    }
}
