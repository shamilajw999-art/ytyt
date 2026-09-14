package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MythicViewModel
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.SponsoredBannerCard
import com.example.ui.components.defaultSponsoredCampaigns
import com.example.ui.components.SubscriptionEmblem
import com.example.ui.components.EmblemSize
import com.example.utils.AdMobBanner
import com.example.utils.AdMobRewardedManager

import com.example.viewmodel.SubscriptionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    viewModel: MythicViewModel,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    onNavigateToAR: () -> Unit,
    onNavigateToMythic: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToArticles: () -> Unit = {},
    onNavigateToReels: () -> Unit = {},
    onNavigateToReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val entitlements by viewModel.entitlements.collectAsState()
    val activeSubscription by subscriptionViewModel.activeSubscription.collectAsState()
    
    LaunchedEffect(Unit) {
        subscriptionViewModel.refreshEntitlements()
        subscriptionViewModel.fetchSubscriptions()
    }
    
    val p = profile ?: com.example.data.local.ProfileEntity(
        id = "default",
        username = "Explorer",
        email = "",
        fullName = "Explorer",
        avatarUrl = "",
        level = 1,
        xp = 0,
        streak = 1,
        scansCount = 0,
        badgesCount = 0
    )
    
    val accentColor = Color(0xFF86FC5C)
    val scrollState = rememberScrollState()

    var pendingFeatureToLaunch by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    // Feature Launcher (Unity ads removed)
    pendingFeatureToLaunch?.let { (_, onLaunch) ->
        LaunchedEffect(Unit) {
            onLaunch()
            pendingFeatureToLaunch = null
        }
    }

    if (showSubscriptionDialog) {
        com.example.ui.components.SubscriptionUpgradeDialog(
            viewModel = viewModel,
            onDismiss = { showSubscriptionDialog = false }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 80.dp) // Leave space for Bottom Nav
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ayubowan, ${p.fullName.split(" ").first()}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("home_user_greeting")
                    )
                    val userPlan = if (entitlements?.status?.lowercase() == "active") (entitlements?.plan ?: "free") else "free"
                    SubscriptionEmblem(
                        tier = userPlan,
                        size = EmblemSize.MEDIUM,
                        modifier = Modifier.testTag("home_subscription_emblem")
                    )
                }
                Text(
                    text = "Explore ancient wonders & mythic history",
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp
                )
            }
            
            // Fire / streak icon
            Box(
                modifier = Modifier
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${p.streak}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Subscription Expiration Renewal Banner (< 7 days)
        val userPlan = if (entitlements?.status?.lowercase() == "active") (entitlements?.plan ?: "free") else "free"
        val isPaidUser = userPlan.lowercase() in listOf("pro", "max", "prime")
        val expiresAtStr = entitlements?.expiresAt ?: activeSubscription?.expiresAt
        val daysRemaining = remember(expiresAtStr, isPaidUser) {
            if (isPaidUser && !expiresAtStr.isNullOrBlank()) {
                try {
                    val exp = java.time.Instant.parse(expiresAtStr)
                    val now = java.time.Instant.now()
                    java.time.temporal.ChronoUnit.DAYS.between(now, exp).toInt()
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        if (daysRemaining != null && daysRemaining in 0..7) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSubscriptionDialog = true }
                    .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.14f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val displayPlanName = if (userPlan.lowercase() in listOf("max", "prime")) "PRIME" else userPlan.uppercase()
                        Text(
                            text = if (daysRemaining == 0) "Mythic $displayPlanName expires today" else "Mythic $displayPlanName expires in $daysRemaining days",
                            color = Color(0xFFFFB74D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tap to renew your membership and preserve your subscriber perks.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Renew", tint = Color(0xFFFFB74D))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // XP / Level Card
        val currentLevelXp = p.xp % 600
        val progress = (currentLevelXp / 600f).coerceIn(0f, 1f)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Level ${p.level} • Explorer",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "+40 XP today",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${p.xp} / ${(p.level) * 600} XP",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF333333))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // --- Google AdMob Rewarded Ad Feature: Daily Free XP Boost ---
        val activity = context as? android.app.Activity
        val rewardedManager = remember { AdMobRewardedManager(context) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                .background(Color(0xFF111410))
                .clickable {
                    if (activity != null) {
                        rewardedManager.showAd(
                            activity = activity,
                            onUserEarnedReward = { amount, _ ->
                                val rewardXp = if (amount > 0) amount else 50
                                viewModel.addXp(rewardXp)
                                android.widget.Toast.makeText(context, "🎉 +$rewardXp XP Rewarded by Google AdMob!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        viewModel.addXp(50)
                        android.widget.Toast.makeText(context, "🎉 +50 XP Earned!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
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
                            text = "Watch Google AdMob sponsor video to boost rank",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { 
                        if (activity != null) {
                            rewardedManager.showAd(
                                activity = activity,
                                onUserEarnedReward = { amount, _ ->
                                    val rewardXp = if (amount > 0) amount else 50
                                    viewModel.addXp(rewardXp)
                                    android.widget.Toast.makeText(context, "🎉 +$rewardXp XP Rewarded by Google AdMob!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            viewModel.addXp(50)
                            android.widget.Toast.makeText(context, "🎉 +50 XP Earned!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Explore Mythic",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Grid Options
        data class MythicOption(val icon: String, val title: String, val subtitle: String, val action: () -> Unit)
        val options = listOf(
            MythicOption("✨", "Mythic AI", "Ask anything") {
                pendingFeatureToLaunch = Pair("LUMO AI Consultation", onNavigateToMythic)
            },
            MythicOption("🗺️", "World Map", "Google Maps & Wonders", onNavigateToMap),
            MythicOption("📰", "Articles", "History & culture", onNavigateToArticles),
            MythicOption("🎬", "Reels", "Short travel clips", onNavigateToReels),
            MythicOption("🔎", "AR Scan", "Point & discover") {
                pendingFeatureToLaunch = Pair("3D AR Vision Scan", onNavigateToAR)
            },
            MythicOption("🚩", "Report", "Flag an issue", onNavigateToReport)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in options.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val opt1 = options[i]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141414))
                            .clickable { opt1.action() }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(opt1.icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(opt1.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(opt1.subtitle, color = Color(0xFFAAAAAA), fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    if (i + 1 < options.size) {
                        val opt2 = options[i + 1]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF262626), RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141414))
                                .clickable { opt2.action() }
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(opt2.icon, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(opt2.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(opt2.subtitle, color = Color(0xFFAAAAAA), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Sponsored Partner Ad Section ---
        Text(
            text = "Featured Partner",
            color = Color(0xFFAAAAAA),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Sponsored Ad
        SponsoredBannerCard(
            campaign = defaultSponsoredCampaigns[0]
        )

        // Google AdMob Adaptive Banner (AdSense / AdMob Placement)
        if (!isPaidUser) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101310)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ADVERTISEMENT",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Google AdMob",
                            color = Color(0xFF86FC5C),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    AdMobBanner()
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary Travel Tech Ad
        SponsoredBannerCard(
            campaign = defaultSponsoredCampaigns[2]
        )
    }
}
