package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.MythicViewModel

@Composable
fun SubscriptionUpgradeDialog(
    viewModel: MythicViewModel,
    onDismiss: () -> Unit,
    onNavigateToSubscription: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf("max") } // "pro" or "max"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFF86FC5C))), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Row Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFFFD700))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("⚡ UNLOCK MYTHIC PRO & MAX", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Text(
                    text = "Zero Ads. Unlimited AI & AR Scans.",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Upgrade your Mythic Ceylon experience to enjoy unlimited 3D AR scans, Mythic AI queries, and offline heritage maps.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                // --- PLAN 1: MYTHIC PRO ($1.99 USD / Month) ---
                val isProSelected = selectedPlan == "pro"
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isProSelected) Color(0xFF1B1B1B) else Color(0xFF121212)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isProSelected) 2.dp else 1.dp,
                            color = if (isProSelected) Color(0xFF86FC5C) else Color(0xFF282828),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { selectedPlan = "pro" }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isProSelected) Color(0xFF86FC5C) else Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Mythic Pro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡", fontSize = 14.sp)
                            }
                            Text("Ad-free AR Scans & LUMO AI + Gemini 1.5 Pro", color = Color.Gray, fontSize = 11.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("$1.99", color = Color(0xFF86FC5C), fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("USD / month", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }

                // --- PLAN 2: MYTHIC MAX (12 MONTHS - $5.00 USD SPECIAL DISCOUNT PACK!) ---
                val isMaxSelected = selectedPlan == "max"
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isMaxSelected) Color(0xFF221D0D) else Color(0xFF14120B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isMaxSelected) 2.5.dp else 1.dp,
                            color = if (isMaxSelected) Color(0xFFFFD700) else Color(0xFF332B10),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { selectedPlan = "max" }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isMaxSelected) Color(0xFFFFD700) else Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isMaxSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Mythic Prime", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFF3B30))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text("80% OFF DEAL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Text("12 Months Annual Pass • Best Value", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("$5.00", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 20.sp)
                                Text("USD / 12 Months", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Prime Exclusive Perks
                        val maxPerks = listOf(
                            "👑 Lifetime Offline 3D Satellite Maps & Artifact Projections",
                            "🔥 2x Quest XP Multiplier Boost on Leaderboards",
                            "🚀 Priority Dedicated AI Server Bandwidth",
                            "🚫 100% Zero Ads across Unity Banner, Interstitial & Rewarded"
                        )

                        maxPerks.forEach { perk ->
                            Text(
                                text = perk,
                                color = Color.LightGray,
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                // Subscribe CTA Button
                Button(
                    onClick = {
                        onDismiss()
                        if (onNavigateToSubscription != null) {
                            onNavigateToSubscription()
                        } else {
                            val packageName = if (selectedPlan == "max") "Mythic Prime (12-Month Pass - $5.00 USD)" else "Mythic Pro ($1.99 USD/mo)"
                            viewModel.addXp(200)
                            viewModel.unlockBadge("pro_subscriber", "Mythic Patron", "Subscribed to $packageName", "gold", "👑")
                            Toast.makeText(context, "Redirecting to Buy $packageName...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlan == "max") Color(0xFFFFD700) else Color(0xFF86FC5C),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("subscribe_cta_btn")
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedPlan == "max") "Get 12 Months Max Deal ($5.00 USD)" else "Subscribe Pro ($1.99 USD/mo)",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
