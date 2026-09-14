package com.example.ui.screens
import androidx.compose.ui.draw.alpha

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SmartFallbackImage
import com.example.ui.components.SubscriptionEmblem
import com.example.ui.components.SubscriptionBadge
import com.example.ui.components.EmblemSize
import com.example.viewmodel.MythicViewModel

import com.example.viewmodel.SubscriptionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    viewModel: MythicViewModel,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    onLogoutSuccess: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToAdmin: () -> Unit = {}
) {
    val profile by viewModel.profile.collectAsState()
    val savedSites by viewModel.savedSites.collectAsState()
    val scanHistory by viewModel.scanHistory.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val entitlements by viewModel.entitlements.collectAsState()

    LaunchedEffect(profile) {
        subscriptionViewModel.refreshEntitlements()
        subscriptionViewModel.fetchSubscriptions()
    }

    val currentTheme = MaterialTheme.colorScheme
    val isDark = currentTheme.background == Color(0xFF000000)

    val textPrimary = currentTheme.onBackground
    val textSecondary = currentTheme.onSurfaceVariant
    val accentColor = currentTheme.primary

    var activeTab by remember { mutableStateOf("Saves") } // "Saves", "History"
    var showSettingsDialog by remember { mutableStateOf(false) }

    val p = profile
    if (p == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SmartFallbackImage(
                    primaryUrl = "https://res.cloudinary.com/dlr63wcmt/image/upload/v1782377619/Main_lmc9y6.png",
                    contentDescription = "MYTHIC Logo",
                    modifier = Modifier.size(100.dp).padding(bottom = 16.dp),
                    accentColor = accentColor,
                    contentScale = ContentScale.Fit
                )
                CircularProgressIndicator(color = accentColor)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Large visual hero avatar card profile info
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xDD000000) else Color(0xDD000000))
                            .padding(top = 40.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Avatar
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val profilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                        ) { uri: android.net.Uri? ->
                            if (uri != null) {
                                viewModel.uploadAndSyncProfilePicture(uri, context)
                            }
                        }
                        
                        // Profile Background
                        if (p.profileBackgroundUrl != null) {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.Black)) {
                                SmartFallbackImage(
                                    primaryUrl = p.profileBackgroundUrl,
                                    contentDescription = "Profile Background",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .offset(y = if (p.profileBackgroundUrl != null) (-55).dp else 0.dp)
                                .clip(CircleShape)
                                .border(3.dp, accentColor, CircleShape)
                                .background(Color.Gray.copy(alpha = 0.2f))
                                .clickable { profilePickerLauncher.launch("image/*") }
                        ) {
                            com.example.ui.components.SmartFallbackImage(
                                primaryUrl = p.avatarUrl,
                                contentDescription = p.fullName,
                                accentColor = accentColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📷 Upload Photo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable { profilePickerLauncher.launch("image/*") }
                            )
                            Text("•", color = Color.Gray)
                            Text(
                                text = "🎲 Randomize Avatar",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable { viewModel.randomizeAvatar() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Username and subscription badge / emblem
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val userPlan = if (entitlements?.status?.lowercase() == "active") (entitlements?.plan ?: "free") else "free"
                            val styling = com.example.service.SubscriptionStylingService.getStyleForTier(userPlan)
                            Text(
                                text = p.fullName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = styling.fontWeight,
                                    color = styling.textColor
                                ),
                                modifier = Modifier.testTag("profile_full_name")
                            )
                            
                            if (userPlan.lowercase() in listOf("pro", "max", "prime")) {
                                SubscriptionBadge(
                                    tier = userPlan,
                                    size = EmblemSize.MEDIUM,
                                    modifier = Modifier.testTag("profile_subscription_badge")
                                )
                                SubscriptionEmblem(
                                    tier = userPlan,
                                    size = EmblemSize.LARGE,
                                    modifier = Modifier.testTag("profile_subscription_emblem")
                                )
                            }
                        }
                        Text(
                            text = "@${p.username} • ${p.email}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary)
                        )
                        
                        // About Section
                        if (!p.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = p.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                        
                        // Interests Section
                        if (!p.interests.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                p.interests.split(",").forEach { interest ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accentColor.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(interest.trim(), color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor, RoundedCornerShape(100.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("🛡️ Lions of Sigiriya", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick mini achievements stats columns row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color(0xBB000000) else Color(0xBB000000))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LEVEL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                                Text(
                                    text = "${p.level}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TOTAL XP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                                Text(
                                    text = "${p.xp}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "STREAK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                                Text(
                                    text = "${p.streak} 🔥",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Subscription VIP Upgrade Promo Card ---
                        if ((entitlements?.plan == "free" || entitlements == null)) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF18150B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.5.dp,
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFFD700),
                                                Color(0xFF86FC5C)
                                            )
                                        ),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable { onNavigateToSubscription() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFD700)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("⚡", fontSize = 22.sp)
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Mythic Pro & Prime Deals",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFFF3B30))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    "SAVE 80%",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                        Text(
                                            "Pro ($1.99/mo) or Prime ($5.00/12 mo) • Zero Ads",
                                            color = Color(0xFFFFD700),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Icon(
                                        androidx.compose.material.icons.Icons.Default.Star,
                                        contentDescription = "Upgrade",
                                        tint = Color(0xFFFFD700)
                                    )
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                                    .clickable { onNavigateToSubscription() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val activePlan = if (entitlements?.status?.lowercase() == "active") (entitlements?.plan ?: "free") else "free"
                                    val displayTierName = if (activePlan.lowercase() in listOf("max", "prime")) "Prime" else activePlan.replaceFirstChar { it.uppercase() }
                                    SubscriptionEmblem(
                                        tier = activePlan,
                                        size = EmblemSize.LARGE
                                    )
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                             verticalAlignment = Alignment.CenterVertically,
                                             horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "Mythic $displayTierName Active",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            SubscriptionBadge(
                                                tier = activePlan,
                                                size = EmblemSize.SMALL
                                            )
                                        }
                                        Text(
                                            if (activePlan.lowercase() == "pro") "Tap to upgrade to Mythic Prime" else "You have unlocked all premium benefits.",
                                            color = if (activePlan.lowercase() == "pro") Color(0xFFFFD700) else Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))



                        // Admin Center Button (for admins and project operators)
                        val isAdminUser = p.email.lowercase() in listOf("shamilajw999@gmail.com", "admin@mythic.app") ||
                                p.email.lowercase().contains("admin") ||
                                p.username.lowercase().contains("admin")
                        if (isAdminUser) {
                            Button(
                                onClick = onNavigateToAdmin,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = Color(0xFFFFD700)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("admin_center_nav_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("👑", fontSize = 16.sp)
                                    Text("Subscription Approvals Admin Center", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Socials
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White), shape = RoundedCornerShape(100.dp)) {
                                Text("Discord", fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White), shape = RoundedCornerShape(100.dp)) {
                                Text("Twitter", fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = { uriHandler.openUri("https://mythicapp.netlify.app") }, colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White), shape = RoundedCornerShape(100.dp)) {
                                Text("Website", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Row with Settings & About, and Logout Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Settings & About Button
                            Button(
                                onClick = { onNavigateToSettings() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF222222) else Color(0xFFDDDDDD),
                                    contentColor = textPrimary
                                ),
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("settings_button")
                            ) {
                                Text("Settings & About", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Logout Button
                            Button(
                                onClick = { viewModel.logout(onLogoutSuccess) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.12f),
                                    contentColor = Color.Red
                                ),
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Logout",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Logout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Social Links Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FOLLOW US",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = textSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // Instagram (Simulated)
                            IconButton(onClick = { /* Open Instagram */ }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Instagram",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Facebook (Simulated)
                            IconButton(onClick = { /* Open Facebook */ }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Facebook",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Sub tabs selecting saved vs scanned landmarks
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isDark) Color(0xDD000000) else Color(0xDD000000))
                    ) {
                        val subTabs = listOf("Saves", "History", "Badges")
                        subTabs.forEach { tab ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (isSelected) accentColor else Color.Transparent)
                                    .clickable { activeTab = tab }
                                    .padding(vertical = 10.dp)
                                    .testTag("profile_tab_${tab.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (tab == "Saves") "Saves (${savedSites.size})" else if (tab == "History") "History (${scanHistory.size})" else "Badges",
                                    color = if (isSelected) Color.Black else textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Grid list profiles items cards
                if (activeTab == "Saves") {
                    if (savedSites.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No saved sites yet! Add saves in Feed.", color = textSecondary)
                            }
                        }
                    } else {
                        items(savedSites) { site ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0x22FFFFFF) else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF111111) else Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    com.example.ui.components.SmartFallbackImage(
                                        primaryUrl = site.imageUrl,
                                        contentDescription = site.siteName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.DarkGray),
                                        accentColor = accentColor
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = site.siteName,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = site.province,
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleSaveSite(site.siteName, site.province, site.imageUrl)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Save",
                                            tint = Color.Red.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (activeTab == "History") {
                    if (scanHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No scans recorded! Go to the Camera.", color = textSecondary)
                            }
                        }
                    } else {
                        items(scanHistory) { scan ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0x22FFFFFF) else Color.Transparent,
                                        RoundedCornerShape(20.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF111111) else Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    com.example.ui.components.SmartFallbackImage(
                                        primaryUrl = scan.imageUrl,
                                        contentDescription = scan.siteName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.DarkGray),
                                        accentColor = accentColor
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = scan.siteName,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "Scanned in ${scan.province}",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "+${scan.xpEarned} XP • ${scan.era}",
                                            color = accentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (activeTab == "Badges") {
                    BadgesList(badges = badges, isDark = isDark, textPrimary = textPrimary, textSecondary = textSecondary, accentColor = accentColor)
                }
            }
        }
    }

    // Settings & About Dialog with CodeRiders Branding and Theme Switcher
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text("Settings & About MYTHIC", color = textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme Switcher Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Midnight Exploration", color = textPrimary, fontWeight = FontWeight.SemiBold)
                            Text("High-contrast mode for night-time visits", color = textSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { checked ->
                                viewModel.toggleTheme(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Divider(color = textSecondary.copy(alpha = 0.2f))

                    // About section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SmartFallbackImage(
                            primaryUrl = "https://res.cloudinary.com/dlr63wcmt/image/upload/v1782377619/Main_lmc9y6.png",
                            contentDescription = "MYTHIC Logo",
                            modifier = Modifier.size(80.dp).padding(bottom = 8.dp),
                            accentColor = accentColor,
                            contentScale = ContentScale.Fit
                        )
                        Text("About MYTHIC", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "MYTHIC is an advanced, online-first Sri Lankan heritage explorer. Our mission is to digitize, preserve, and celebrate ancient landmarks across Sri Lanka using AI-powered camera scanning and immersive companion technology.",
                            color = textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Divider(color = textSecondary.copy(alpha = 0.2f))

                    // App Credits Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("App Credits", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Leader:", color = textSecondary, fontSize = 12.sp)
                            Text("Senuja", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sub Leader:", color = textSecondary, fontSize = 12.sp)
                            Text("Meshark", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Head Design:", color = textSecondary, fontSize = 12.sp)
                            Text("Abilash", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Head Coder:", color = textSecondary, fontSize = 12.sp)
                            Text("Maleesha", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Head Implementor:", color = textSecondary, fontSize = 12.sp)
                            Text("Gavinda", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text("Special Thanks", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Sasan Vidunitha, Anuki, Senusha", color = textSecondary, fontSize = 12.sp)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text("Special Thanks to Teachers", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Sachindra Teacher, Oshandi Teacher, Savindra Teacher", color = textSecondary, fontSize = 12.sp)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text("Our Principal", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Dr. Anushke Perera", color = textSecondary, fontSize = 12.sp)
                    }

                    Divider(color = textSecondary.copy(alpha = 0.2f))

                    // Branding section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Made by CodeRiders",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Version 1.0.0 • Production Ready",
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = if (isDark) Color(0xFF111111) else Color(0xFFFFFFFF),
            modifier = Modifier.border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                RoundedCornerShape(28.dp)
            )
        )
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.BadgesList(badges: List<com.example.data.local.BadgeEntity>, isDark: Boolean, textPrimary: Color, textSecondary: Color, accentColor: Color) {
    val allBadges = com.example.data.local.ALL_BADGES

    val displayedBadges = badges.let { b ->
        allBadges.map { definition ->
            val unlocked = b.find { it.code == definition.code }
            definition to unlocked
        }.sortedWith(compareBy(
            { it.second == null },
            { it.first.name }
        ))
    }

    items(displayedBadges.size) { index ->
        val (definition, unlockedBadge) = displayedBadges[index]
        val isUnlocked = unlockedBadge != null
        val tierColor = when (definition.tier.lowercase()) {
            "bronze" -> Color(0xFFCD7F32)
            "silver" -> Color(0xFFC0C0C0)
            "gold" -> Color(0xFFFFD700)
            "platinum" -> Color(0xFFE5E4E2)
            "diamond" -> Color(0xFFb9f2ff)
            "legendary" -> Color(0xFFFF4500)
            "ultra rare" -> Color(0xFF8A2BE2)
            else -> accentColor
        }
        
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .border(
                    1.dp,
                    if (isUnlocked) accentColor.copy(alpha = 0.5f) else Color(0x33FFFFFF),
                    RoundedCornerShape(20.dp)
                ),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color(0xDD000000)
            )
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(100.dp))
                        .background(if (isUnlocked) accentColor.copy(alpha = 0.2f) else Color.DarkGray)
                        .border(1.dp, if (isUnlocked) accentColor else Color.Gray, RoundedCornerShape(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        Text(text = definition.icon, fontSize = 28.sp)
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.alpha(0.4f)) {
                            Text(text = definition.icon, fontSize = 28.sp)
                        }
                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                            Text("🔒", fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = definition.name,
                            color = if (isUnlocked) textPrimary else textPrimary.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(tierColor.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(definition.tier.uppercase(), color = tierColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = definition.description,
                        color = if (isUnlocked) textSecondary else textSecondary.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                    if (isUnlocked && unlockedBadge != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val dateStr = dateFormat.format(java.util.Date(unlockedBadge.unlockedAt))
                        Text(
                            text = "🏆 Unlocked on $dateStr",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
