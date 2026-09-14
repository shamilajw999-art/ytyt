package com.example.ui.screens
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.content.Context
import com.example.viewmodel.DailyTriviaState
import com.example.viewmodel.MythicViewModel
import com.example.ui.components.RewardedAdCard
import androidx.compose.ui.layout.ContentScale

@Composable
fun QuestsScreen(viewModel: MythicViewModel) {
    val currentTheme = MaterialTheme.colorScheme
    val textPrimary = currentTheme.onBackground
    val textSecondary = currentTheme.onSurfaceVariant
    val accentColor = currentTheme.primary

    var activeTab by remember { mutableStateOf("Realms") }
    val tabs = listOf("Realms", "Cards", "Leaderboard", "Quests")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Gamification Hub",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Custom Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xDD000000))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected) accentColor else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) Color.Black else textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (activeTab) {
                "Realms" -> RealmsSkillTreeTab(textPrimary, textSecondary, accentColor)
                "Cards" -> CollectibleCardsTab(textPrimary, textSecondary, accentColor)
                "Leaderboard" -> LeaderboardTab(textPrimary, textSecondary, accentColor)
                "Quests" -> QuestsTab(viewModel, textPrimary, textSecondary, accentColor)
            }
        }
    }
}

@Composable
fun RealmsSkillTreeTab(textPrimary: Color, textSecondary: Color, accentColor: Color) {
    val realms = listOf(
        "Ancient Anuradhapura" to "Unlocked",
        "Polonnaruwa Era" to "Unlocked",
        "Kandyan Kingdom" to "Locked",
        "Age of Exploration" to "Locked"
    )
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Historical Realms & Skill Trees", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Unlock new non-linear paths of history using your ELO Mastery Points.", color = textSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(realms) { (name, status) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD000000)),
                modifier = Modifier.fillMaxWidth().border(1.dp, if(status == "Unlocked") accentColor else Color.Gray, RoundedCornerShape(16.dp))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(50.dp).background(if(status == "Unlocked") accentColor.copy(alpha=0.2f) else Color.DarkGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if(status == "Unlocked") "🗡️" else "🔒", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(name, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(if(status == "Unlocked") "Explore Scenarios" else "Requires 500 Mastery Points", color = if(status=="Unlocked") accentColor else textSecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CollectibleCardsTab(textPrimary: Color, textSecondary: Color, accentColor: Color) {
    val cards = listOf(
        Triple("King Kashyapa", "SSR", com.example.R.drawable.king_kashyapa_1788013494034),
        Triple("Parakramabahu I", "SR", com.example.R.drawable.king_parakramabahu_1788013510958),
        Triple("Vihara Maha Devi", "UR", com.example.R.drawable.queen_vihara_maha_devi_1788013532585),
        Triple("Dutugemunu", "SSR", com.example.R.drawable.king_dutugemunu_1788013549656)
    )
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Gacha Character Artifacts", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Spend Daily Login Streaks to Summon Figures.", color = textSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {}, 
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Summon Card (100 Favors)", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cards) { (name, rarity, imgRes) ->
                val rarityColor = when(rarity) {
                    "UR" -> Color(0xFF8A2BE2) // Purple
                    "SSR" -> Color(0xFFFFD700) // Gold
                    else -> Color(0xFFC0C0C0) // Silver
                }
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.8f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    )
                )
                Card(
                    modifier = Modifier
                        .height(200.dp)
                        .border(
                            width = if (rarity == "UR" || rarity == "SSR") 2.dp else 1.dp,
                            color = rarityColor.copy(alpha = glowAlpha),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xDD000000)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = imgRes), 
                            contentDescription = name, 
                            contentScale = ContentScale.Crop, 
                            modifier = Modifier.fillMaxSize().alpha(0.85f)
                        )
                        Column(modifier = Modifier.align(Alignment.BottomStart).background(Color(0xDD000000)).fillMaxWidth().padding(8.dp)) {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Rarity: $rarity", color = rarityColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardTab(textPrimary: Color, textSecondary: Color, accentColor: Color) {
    val players = listOf(
        Triple("Shamilajw", "Lions of Sigiriya", 4520),
        Triple("AlexTheGreat", "Scholars of Anuradhapura", 3800),
        Triple("RuinsExplorer", "Scholars of Anuradhapura", 3100),
        Triple("HistoryBuff", "Lions of Sigiriya", 2450)
    )
    val factions = listOf(
        "Lions of Sigiriya" to 15420,
        "Scholars of Anuradhapura" to 12800
    )
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Weekly Faction Wars ⚔️", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Pool your XP with your Faction. Winning Guilds earn exclusive drops!", color = textSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Factions Top Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                factions.forEachIndexed { index, (factionName, factionScore) ->
                    val color = if (index == 0) Color(0xFFFFD700) else Color(0xFFC0C0C0)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD000000)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛡️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(factionName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            Text("$factionScore XP", color = color, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Top Guild Members", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(players.withIndex().toList()) { (idx, data) ->
            val (name, rank, score) = data
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD000000)),
                modifier = Modifier.fillMaxWidth().border(if (idx == 0) 2.dp else 0.dp, if (idx == 0) accentColor else Color.Transparent, RoundedCornerShape(16.dp))
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${idx + 1}", color = if (idx == 0) accentColor else textSecondary, fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.width(40.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(rank, color = textSecondary, fontSize = 14.sp)
                    }
                    Text(score.toString(), color = accentColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun QuestsTab(viewModel: MythicViewModel, textPrimary: Color, textSecondary: Color, accentColor: Color) {
    val quests by viewModel.quests.collectAsState()
    val triviaState by viewModel.dailyTriviaState.collectAsState()
    
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else if (triviaState is DailyTriviaState.Idle) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) { null }
            
            if (loc != null) {
                viewModel.fetchDailyTrivia(loc.latitude, loc.longitude)
            } else {
                viewModel.fetchDailyTrivia(7.8731, 80.7718) // Default to center of Sri Lanka
            }
        }
    }
    
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var hasAnswered by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Daily Localized Trivia", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Answer correctly to earn 50 Faction XP!", color = textSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD000000)),
                modifier = Modifier.fillMaxWidth().border(2.dp, accentColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (val state = triviaState) {
                        is DailyTriviaState.Loading -> {
                            CircularProgressIndicator(color = accentColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Text("Locating nearby history...", color = textSecondary, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
                        }
                        is DailyTriviaState.Success -> {
                            Text(state.trivia.question, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            state.trivia.options.forEachIndexed { index, option ->
                                val isSelected = selectedOptionIndex == index
                                val isCorrect = state.trivia.correctIndex == index
                                
                                val backgroundColor = when {
                                    !hasAnswered && isSelected -> accentColor.copy(alpha = 0.3f)
                                    hasAnswered && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.3f) // Green
                                    hasAnswered && isSelected && !isCorrect -> Color(0xFFE53935).copy(alpha = 0.3f) // Red
                                    else -> Color.Transparent
                                }
                                
                                val borderColor = when {
                                    hasAnswered && isCorrect -> Color(0xFF4CAF50)
                                    hasAnswered && isSelected && !isCorrect -> Color(0xFFE53935)
                                    isSelected -> accentColor
                                    else -> Color(0x33FFFFFF)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(backgroundColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                        .clickable(enabled = !hasAnswered) {
                                            selectedOptionIndex = index
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(option, color = if (isSelected || (hasAnswered && isCorrect)) Color.White else textSecondary, fontSize = 14.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (!hasAnswered) {
                                Button(
                                    onClick = {
                                        hasAnswered = true
                                        if (selectedOptionIndex == state.trivia.correctIndex) {
                                            viewModel.claimTriviaReward("Lions of Sigiriya")
                                        }
                                    },
                                    enabled = selectedOptionIndex != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Submit Answer", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = if (selectedOptionIndex == state.trivia.correctIndex) "Correct! +50 XP" else "Incorrect!",
                                    color = if (selectedOptionIndex == state.trivia.correctIndex) Color(0xFF4CAF50) else Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(state.trivia.explanation, color = textSecondary, fontSize = 12.sp)
                            }
                        }
                        is DailyTriviaState.Error -> {
                            Text("Failed to load trivia. Try again tomorrow!", color = textSecondary)
                        }
                        is DailyTriviaState.Idle -> { }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            RewardedAdCard(
                onRewardEarned = {
                    viewModel.addXp(50)
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Active Quests", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(quests) { quest ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD000000)),
                modifier = Modifier.fillMaxWidth().border(1.dp, if (quest.completed) accentColor else Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(quest.title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(quest.description, color = textSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { (quest.progress.toFloat() / quest.target).coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(100.dp)),
                            color = accentColor,
                            trackColor = Color(0x44FFFFFF)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("${quest.progress}/${quest.target}", color = if (quest.completed) accentColor else textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
