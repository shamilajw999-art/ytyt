package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.components.SmartFallbackImage
import com.example.viewmodel.MythicViewModel

data class MythicColorOption(
    val id: String,
    val name: String,
    val hexString: String,
    val color: Color,
    val description: String,
    val emoji: String
)

data class MythicMoodOption(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val greeting: String
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: MythicViewModel,
    onCompleteOnboarding: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    val colorOptions = remember {
        listOf(
            MythicColorOption("green", "Emerald Heritage", "#86FC5C", Color(0xFF86FC5C), "Vibrant Ceylon energy & ancient nature", "🟢"),
            MythicColorOption("gold", "Royal Sigiriya Gold", "#FFD700", Color(0xFFFFD700), "Majesty of ancient Sri Lankan kings", "🟡"),
            MythicColorOption("blue", "Sapphire Ocean", "#00E5FF", Color(0xFF00E5FF), "Ceylon blue sapphire brilliance", "🔵"),
            MythicColorOption("ruby", "Ruby Lotus", "#FF2A6D", Color(0xFFFF2A6D), "Mystical ruby lotus aura", "🔴"),
            MythicColorOption("purple", "Mystic Amethyst", "#B026FF", Color(0xFFB026FF), "Sacred spiritual aura & mystery", "🟣")
        )
    }

    val moodOptions = remember {
        listOf(
            MythicMoodOption("scholar", "Wise Scholar", "🎓", "In-depth historical facts, Mahavamsa chronicles & architectural analysis", "\"Greetings, seeker of truth. Let us dissect the chronicles.\""),
            MythicMoodOption("explorer", "Enthusiastic Explorer", "🧭", "Energetic, quest-driven, loves secret trails & travel tips", "\"Aayubowan! Ready to conquer ancient ruins and find hidden secrets?\""),
            MythicMoodOption("storyteller", "Mystic Storyteller", "📜", "Dramatic legends, royal folklore & ancient epics", "\"Listen closely... as the winds whisper tales of ancient Sigiriya...\""),
            MythicMoodOption("guide", "Friendly Companion", "🤝", "Warm, welcoming, conversational Sri Lankan guide", "\"Aayubowan! I'm MYTHIC, your personal friend across Sri Lanka!\"")
        )
    }

    val presetAvatars = remember {
        listOf(
            "https://api.dicebear.com/7.x/adventurer/png?seed=sigiriya_king",
            "https://api.dicebear.com/7.x/adventurer/png?seed=ceylon_scholar",
            "https://api.dicebear.com/7.x/adventurer/png?seed=heritage_guardian",
            "https://api.dicebear.com/7.x/bottts/png?seed=lumo_robot",
            "https://api.dicebear.com/7.x/lorelei/png?seed=island_explorer"
        )
    }

    var selectedColor by remember { mutableStateOf(colorOptions[0]) }
    var selectedMood by remember { mutableStateOf(moodOptions[3]) }
    var customAvatarUri by remember { mutableStateOf<String?>(null) }
    var selectedPresetAvatar by remember { mutableStateOf(presetAvatars[0]) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customAvatarUri = uri.toString()
            Toast.makeText(context, "Custom profile picture selected!", Toast.LENGTH_SHORT).show()
        }
    }

    val activeAvatarUrl = customAvatarUri ?: selectedPresetAvatar

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D0D), Color(0xFF141914), Color(0xFF080D08))
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Step Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0..3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= currentStep) selectedColor.color else Color.White.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "onboarding_steps"
            ) { step ->
                when (step) {
                    0 -> Step1WhatsMythic(accentColor = selectedColor.color)
                    1 -> Step2MythicColor(
                        colorOptions = colorOptions,
                        selectedColor = selectedColor,
                        onSelectColor = { selectedColor = it }
                    )
                    2 -> Step3MythicMood(
                        moodOptions = moodOptions,
                        selectedMood = selectedMood,
                        selectedColor = selectedColor.color,
                        onSelectMood = { selectedMood = it }
                    )
                    3 -> Step4CustomProfile(
                        activeAvatarUrl = activeAvatarUrl,
                        presetAvatars = presetAvatars,
                        selectedPreset = selectedPresetAvatar,
                        onSelectPreset = {
                            selectedPresetAvatar = it
                            customAvatarUri = null
                        },
                        onPickPhoto = { photoPickerLauncher.launch("image/*") },
                        accentColor = selectedColor.color
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        shape = RoundedCornerShape(100.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep += 1
                        } else {
                            // Save onboarding choices and complete
                            viewModel.saveOnboardingPreferences(
                                mythicColorHex = selectedColor.hexString,
                                mythicMood = selectedMood.title,
                                customAvatarUrl = activeAvatarUrl
                            )
                            Toast.makeText(context, "Welcome to Mythic Ceylon! +100 XP Earned 🎉", Toast.LENGTH_LONG).show()
                            onCompleteOnboarding()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColor.color,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.height(52.dp).testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (currentStep < 3) "Continue ➔" else "Finish & Explore 🎉",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- STEP 1: WHAT IS MYTHIC CEYLON ---
@Composable
fun Step1WhatsMythic(accentColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f))
                .border(2.dp, accentColor, CircleShape)
        ) {
            Text("🏛️", fontSize = 42.sp)
        }

        Text(
            text = "Welcome to Mythic Ceylon!",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Sri Lanka's premier gamified AI heritage platform. Discover ancient ruins, scan 3D monuments in AR, and embark on epic quests.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Feature Pillars
        val pillars = listOf(
            Triple("🔮", "LUMO AI Companion", "Your personal Sri Lankan heritage guide powered by Gemini AI."),
            Triple("📸", "3D AR Vision Scan", "Point camera at monuments to project 3D virtual models & facts."),
            Triple("🗺️", "Interactive Quests & Map", "Explore 1,000+ ancient ruins, citadels, and sacred temples."),
            Triple("🏆", "Gamified Ranks & XP", "Earn XP, unlock rare badges, and climb global leaderboards.")
        )

        pillars.forEach { (emoji, title, desc) ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2B2B2B), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(emoji, fontSize = 28.sp)
                    Column {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(desc, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- STEP 2: LUMO COLOR SELECTION ---
@Composable
fun Step2MythicColor(
    colorOptions: List<MythicColorOption>,
    selectedColor: MythicColorOption,
    onSelectColor: (MythicColorOption) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Mythic Live Avatar Preview
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(selectedColor.color.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
                .border(3.dp, selectedColor.color, CircleShape)
        ) {
            Text("✨", fontSize = 48.sp)
        }

        Text(
            text = "Choose LUMO's Theme Color",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Select the aura color that LUMO AI will shine with across your chat, AR guidance, and profile interface.",
            color = Color.LightGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        colorOptions.forEach { option ->
            val isSelected = option.id == selectedColor.id
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1F1F1F) else Color(0xFF141414)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) option.color else Color(0xFF282828),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectColor(option) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(option.color),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(option.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(option.emoji, fontSize = 14.sp)
                        }
                        Text(option.description, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- STEP 3: LUMO MOOD SELECTION ---
@Composable
fun Step3MythicMood(
    moodOptions: List<MythicMoodOption>,
    selectedMood: MythicMoodOption,
    selectedColor: Color,
    onSelectMood: (MythicMoodOption) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(selectedColor.copy(alpha = 0.2f))
                .border(2.dp, selectedColor, CircleShape)
        ) {
            Text(selectedMood.emoji, fontSize = 40.sp)
        }

        Text(
            text = "Select LUMO's Mood & Tone",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "How would you like LUMO to speak with you when exploring ancient Sri Lankan secrets?",
            color = Color.LightGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        moodOptions.forEach { mood ->
            val isSelected = mood.id == selectedMood.id
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1F1F1F) else Color(0xFF141414)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) selectedColor else Color(0xFF282828),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectMood(mood) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(mood.emoji, fontSize = 24.sp)
                        Text(mood.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(selectedColor)
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Active", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(mood.description, color = Color.LightGray, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = mood.greeting,
                            color = selectedColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// --- STEP 4: CUSTOM PROFILE PICTURE ---
@Composable
fun Step4CustomProfile(
    activeAvatarUrl: String,
    presetAvatars: List<String>,
    selectedPreset: String,
    onSelectPreset: (String) -> Unit,
    onPickPhoto: () -> Unit,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Active Avatar Preview
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(3.dp, accentColor, CircleShape)
            ) {
                SmartFallbackImage(
                    primaryUrl = activeAvatarUrl,
                    contentDescription = "Profile Picture",
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = "Upload Custom Profile Picture",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Upload your photo from your device gallery, or pick a curated Sri Lankan Explorer avatar below.",
            color = Color.LightGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Upload Button
        Button(
            onClick = onPickPhoto,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("upload_custom_photo_btn")
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Photo from Gallery", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Or Pick a Sri Lankan Explorer Avatar:",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetAvatars) { url ->
                val isSelected = url == selectedPreset
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) accentColor else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onSelectPreset(url) }
                ) {
                    SmartFallbackImage(
                        primaryUrl = url,
                        contentDescription = "Avatar Preset",
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
