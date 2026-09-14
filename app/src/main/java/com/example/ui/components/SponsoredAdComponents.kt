package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

data class SponsoredCampaign(
    val id: String,
    val brandName: String,
    val headline: String,
    val tagline: String,
    val category: String,
    val ctaText: String,
    val promoCode: String,
    val iconEmoji: String,
    val destinationUrl: String = "https://www.srilanka.travel",
    val rating: String = "4.9 ★"
)

val defaultSponsoredCampaigns = listOf(
    SponsoredCampaign(
        id = "ad_sri_lanka_travel",
        brandName = "Visit Sri Lanka 2026",
        headline = "Official Heritage Tours",
        tagline = "Book private archaeologist-guided journeys across Sigiriya, Kandy & Galle with 30% off.",
        category = "Official Tourism",
        ctaText = "Book Tour",
        promoCode = "MYTHIC30",
        iconEmoji = "🇱🇰",
        destinationUrl = "https://www.srilanka.travel"
    ),
    SponsoredCampaign(
        id = "ad_mythic_pro",
        brandName = "Mythic Explorer Pro",
        headline = "Ad-Free & Offline 3D Maps",
        tagline = "Unlimited Mythic AI archaeological queries, offline Google satellite tiles, and 2x scan XP.",
        category = "Pro Membership",
        ctaText = "Upgrade Pro",
        promoCode = "PROEXPLORER",
        iconEmoji = "⚡",
        destinationUrl = "https://ai.google.dev"
    ),
    SponsoredCampaign(
        id = "ad_nomad_esim",
        brandName = "Nomad Worldwide 5G eSIM",
        headline = "Stay Connected Globally",
        tagline = "High-speed 5G data across 170+ heritage regions and ancient landmarks starting at $4/day.",
        category = "Travel Tech",
        ctaText = "Get eSIM",
        promoCode = "HERITAGE5G",
        iconEmoji = "📶",
        destinationUrl = "https://www.getnomad.app"
    ),
    SponsoredCampaign(
        id = "ad_natgeo_tours",
        brandName = "Expeditions Worldwide",
        headline = "Lost Civilizations Treks",
        tagline = "Small-group expeditions to the Pyramids of Giza, Machu Picchu, Petra, and Angkor Wat.",
        category = "Global Expeditions",
        ctaText = "View Trips",
        promoCode = "EXPEDITION20",
        iconEmoji = "🌍",
        destinationUrl = "https://www.nationalgeographic.com/expeditions"
    )
)

/**
 * Polished Sponsored Ad Banner for Home & Feed
 */
@Composable
fun SponsoredBannerCard(
    modifier: Modifier = Modifier,
    campaign: SponsoredCampaign = defaultSponsoredCampaigns.first(),
    onAdClick: (SponsoredCampaign) -> Unit = {}
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        SponsoredAdDialog(
            campaign = campaign,
            onDismiss = { showDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF86FC5C).copy(alpha = 0.4f),
                        Color(0xFF333333),
                        Color(0xFF86FC5C).copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF141A12),
                        Color(0xFF0F120E),
                        Color(0xFF151515)
                    )
                )
            )
            .clickable {
                showDialog = true
                onAdClick(campaign)
            }
            .padding(16.dp)
    ) {
        Column {
            // Header: "Sponsored" Tag & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF86FC5C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "Sponsored • ${campaign.category}",
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = campaign.rating,
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222620))
                        .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(campaign.iconEmoji, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.brandName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = campaign.tagline,
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Code:", color = Color(0xFF777777), fontSize = 11.sp)
                    Text(
                        campaign.promoCode,
                        color = Color(0xFF86FC5C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        showDialog = true
                        onAdClick(campaign)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF86FC5C),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = campaign.ctaText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * Rewarded Ad Card: Watch 5s ad to earn +50 XP
 */
@Composable
fun RewardedAdCard(
    modifier: Modifier = Modifier,
    onRewardEarned: () -> Unit
) {
    var showRewardedDialog by remember { mutableStateOf(false) }

    if (showRewardedDialog) {
        RewardedAdDialog(
            onDismiss = { showRewardedDialog = false },
            onRewardEarned = {
                showRewardedDialog = false
                onRewardEarned()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
            .background(Color(0xFF111410))
            .clickable { showRewardedDialog = true }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF86FC5C).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 20.sp)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Daily Ad Reward",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF86FC5C).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("+50 XP", color = Color(0xFF86FC5C), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(
                        text = "Watch short 5-second sponsor clip to boost rank",
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = { showRewardedDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F2E1B),
                    contentColor = Color(0xFF86FC5C)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86FC5C)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Watch", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Rewarded Video Ad Modal with 5-second countdown & XP reward celebration
 */
@Composable
fun RewardedAdDialog(
    onDismiss: () -> Unit,
    onRewardEarned: () -> Unit
) {
    val context = LocalContext.current
    var secondsLeft by remember { mutableIntStateOf(5) }
    var isCompleted by remember { mutableStateOf(false) }

    val campaign = remember { defaultSponsoredCampaigns.random() }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        isCompleted = true
    }

    Dialog(
        onDismissRequest = {
            if (isCompleted) onDismiss()
        },
        properties = DialogProperties(dismissOnBackPress = isCompleted, dismissOnClickOutside = isCompleted)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F110E))
                .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF86FC5C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SPONSORED AD", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }

                    if (isCompleted) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    } else {
                        Text(
                            text = "Reward in ${secondsLeft}s",
                            color = Color(0xFF86FC5C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Video Ad Simulation Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E281A), Color(0xFF121411))
                            )
                        )
                        .border(1.dp, Color(0xFF2A3624), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(campaign.iconEmoji, fontSize = 52.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = campaign.brandName,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = campaign.headline,
                            color = Color(0xFF86FC5C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Progress ring when playing
                    if (!isCompleted) {
                        CircularProgressIndicator(
                            progress = { (5 - secondsLeft) / 5f },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(28.dp),
                            color = Color(0xFF86FC5C),
                            strokeWidth = 3.dp,
                            trackColor = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = campaign.tagline,
                    color = Color(0xFFDDDDDD),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF86FC5C).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF86FC5C), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎉", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ad Complete! +50 XP Ready to Claim",
                                color = Color(0xFF86FC5C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "+50 XP Added to your Profile!", Toast.LENGTH_SHORT).show()
                            onRewardEarned()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF86FC5C),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Claim +50 XP", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Please watch for $secondsLeft seconds to earn XP",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed Sponsor Dialog with Promo Code & Direct Link
 */
@Composable
fun SponsoredAdDialog(
    campaign: SponsoredCampaign,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141712))
                .border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF86FC5C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SPONSORED OFFER", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222B1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(campaign.iconEmoji, fontSize = 34.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = campaign.brandName,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )

                Text(
                    text = campaign.headline,
                    color = Color(0xFF86FC5C),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = campaign.tagline,
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Promo code card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E241B))
                        .border(1.dp, Color(0xFF33402E), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Exclusive Discount Code", color = Color.Gray, fontSize = 10.sp)
                            Text(
                                campaign.promoCode,
                                color = Color(0xFF86FC5C),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Promo Code", campaign.promoCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied ${campaign.promoCode}!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A3924),
                                contentColor = Color(0xFF86FC5C)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(campaign.destinationUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Opening ${campaign.brandName}...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF86FC5C),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(campaign.ctaText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
