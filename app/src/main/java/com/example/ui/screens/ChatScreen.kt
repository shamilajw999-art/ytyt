package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MythicConversationEntity
import com.example.viewmodel.MythicViewModel
import com.example.ui.components.FakeAdScreen
import com.example.utils.MythicTtsManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

import com.example.viewmodel.SubscriptionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(
    viewModel: MythicViewModel,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    onNavigateToSubscription: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val userPrefs by viewModel.userPreferences.collectAsState()
    val activeSubscription by subscriptionViewModel.activeSubscription.collectAsState()
    val entitlements by viewModel.entitlements.collectAsState()
    val showUsageCapDialog by viewModel.showUsageCapDialog.collectAsState()
    
    LaunchedEffect(Unit) {
        subscriptionViewModel.fetchSubscriptions()
    }
    
    val mythicColorHex = userPrefs?.mythicColorHex ?: "#86FC5C"
    val mythicMood = userPrefs?.mythicMood ?: "Friendly Guide"
    val accentColor = remember(mythicColorHex) {
        try {
            Color(android.graphics.Color.parseColor(mythicColorHex))
        } catch (e: Exception) {
            Color(0xFF86FC5C)
        }
    }
    val cardBackground = Color(0xFF141414)
    val borderColor = Color(0xFF262626)

    var messageInput by remember { mutableStateOf("") }
    var showFakeAd by remember { mutableStateOf(false) }
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    var isTalkingMode by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    
    val sttManager = remember {
        com.example.utils.MythicSttManager(
            context = context,
            onResult = { result ->
                viewModel.sendMythicMessage(result.trim())
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            },
            onStateChanged = { recording ->
                isRecording = recording
            }
        )
    }
    
    LaunchedEffect(Unit) {
        MythicTtsManager.init(context)
        sttManager.init()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            MythicTtsManager.shutdown()
            sttManager.destroy()
        }
    }
    
    val mythicMessages by viewModel.mythicMessages.collectAsState(initial = emptyList())
    val isMythicTyping by viewModel.isMythicTyping.collectAsState()
    val listState = rememberLazyListState()

    // Pulse animation specs for microphone capture & listening mode
    val pulseInfiniteTransition = rememberInfiniteTransition(label = "voice_mic_pulse")
    val pulseScale by pulseInfiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseInfiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseGlow by pulseInfiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sttManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required to speak to Mythic", Toast.LENGTH_SHORT).show()
        }
    }

    val startVoiceInput = {
        if (isRecording) {
            sttManager.stopListening()
        } else {
            // Check permission before starting
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                sttManager.startListening()
            } else {
                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Voice reply logic
    LaunchedEffect(mythicMessages, isMythicTyping) {
        val lastMessage = mythicMessages.lastOrNull()
        if (isTalkingMode && lastMessage?.role == "assistant" && !isMythicTyping) {
            MythicTtsManager.speak(lastMessage.content)
        }
    }

    val sendMessage = {
        if (messageInput.isNotBlank()) {
            val toSend = messageInput.trim()
            viewModel.checkAndIncrementDailyUsage(
                onAllowed = {
                    if (activeSubscription == null) {
                        pendingMessage = toSend
                        showFakeAd = true
                    } else {
                        messageInput = ""
                        viewModel.sendMythicMessage(toSend)
                    }
                },
                onCapReached = {
                    com.example.notifications.HeritageNotificationManager.showDailyLimitCapNotification(context)
                }
            )
        }
    }

    // Auto scroll to latest message
    LaunchedEffect(mythicMessages.size, isMythicTyping) {
        val targetIndex = if (isMythicTyping) mythicMessages.size else (mythicMessages.size - 1).coerceAtLeast(0)
        if (mythicMessages.isNotEmpty() || isMythicTyping) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 72.dp)
    ) {
        val isAdFree = entitlements?.entitlements?.adsFree == true || activeSubscription != null
        if (!isAdFree) {
            Text(
                text = "Upgrade to Pro for an ad-free experience!",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Yellow)
                    .padding(8.dp),
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
        
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mythic Avatar with voice capturing aura pulse
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(42.dp)
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pulseScale * 1.35f
                                    scaleY = pulseScale * 1.35f
                                    alpha = pulseAlpha
                                }
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isRecording) listOf(accentColor, Color(0xFF1E3A1E)) else listOf(Color(0xFF1E3A1E), Color(0xFF122012))
                                )
                            )
                            .border(1.5.dp, if (isRecording) Color.White else accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isRecording) "🎙️" else "✨", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mythic",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .graphicsLayer { if (isRecording) alpha = pulseGlow }
                                .clip(CircleShape)
                                .background(if (isRecording) Color(0xFFFF3B30) else accentColor)
                        )
                    }
                    Text(
                        text = if (isRecording) "🎙️ Capturing Audio... Speak Now" else "AI Heritage Guide • $mythicMood",
                        color = if (isRecording) accentColor else accentColor.copy(alpha = 0.85f),
                        fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            }

            // Talking Mode Toggle + Clear Chat
            Row {
                IconButton(onClick = { isTalkingMode = !isTalkingMode }) {
                    Icon(
                        if (isTalkingMode) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Talking Mode",
                        tint = if (isTalkingMode) accentColor else Color.Gray
                    )
                }
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Clear Chat History",
                        tint = Color.Gray
                    )
                }
            }
        }

        HorizontalDivider(color = borderColor, thickness = 0.8.dp)

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Conversation Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (mythicMessages.isEmpty()) {
                // Empty state / Welcoming intro
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF152213))
                                    .border(2.dp, accentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏛️", fontSize = 28.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Ayubowan! I'm Mythic ✨",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Your personal AI companion for exploring Sri Lanka. Ask me about ancient rock fortresses, sacred relics, historical timelines, or travel itineraries!",
                                color = Color(0xFFAAAAAA),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "TRY ASKING",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val suggestionPrompts = listOf(
                                "🏰 Tell me the story of Sigiriya rock fortress",
                                "🦷 What is the history of the Sacred Tooth Relic?",
                                "🚂 How was the Nine Arch Bridge constructed?",
                                "🗺️ Plan a 3-day Cultural Triangle tour route",
                                "🇱🇰 Explain the ancient hydraulic civilization"
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestionPrompts.forEach { prompt ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cardBackground)
                                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.sendMythicMessage(prompt.substring(3).trim())
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = prompt,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                Icons.Default.ArrowOutward,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(mythicMessages, key = { it.id }) { message ->
                        ChatMessageBubble(
                            message = message,
                            accentColor = accentColor,
                            onQuickPrompt = { prompt -> sendMessage() }
                        )
                    }

                    if (isMythicTyping) {
                        item {
                            TypingIndicatorBubble(accentColor = accentColor)
                        }
                    }

                    val hasUnlimitedLumo = entitlements?.entitlements?.unlimitedLumo == true || activeSubscription != null
                    if (!hasUnlimitedLumo && mythicMessages.size >= 10) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF18150B))
                                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("✨ Free limit reached", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Upgrade to Mythic Pro for unlimited AI chats and exclusive heritage insights.",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    Button(
                                        onClick = { /* Could add navigation here if needed */ },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Upgrade to Pro", color = Color.Black, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Voice Recording Audio Capture Pulse Waveform HUD Bar
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1B3B1B), Color(0xFF0F200F), Color(0xFF1B3B1B))
                        )
                    )
                    .border(1.5.dp, accentColor.copy(alpha = pulseGlow), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { alpha = pulseGlow }
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                        )
                        Text(
                            text = "CAPTURING AUDIO",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )

                        AudioEqualizerWaveform(accentColor = accentColor)
                    }

                    IconButton(
                        onClick = { isRecording = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop Capture",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Input Field & Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(cardBackground)
                .border(
                    width = if (isRecording) 2.dp else 1.dp,
                    color = if (isRecording) accentColor.copy(alpha = pulseGlow) else if (messageInput.isNotEmpty()) accentColor.copy(alpha = 0.5f) else borderColor,
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone Input Button with Visual Pulse Animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp)
            ) {
                if (isRecording) {
                    // Outer Pulse Wave Ring 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pulseScale * 1.35f
                                scaleY = pulseScale * 1.35f
                                alpha = pulseAlpha
                            }
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    // Outer Pulse Wave Ring 2
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pulseScale * 1.7f
                                scaleY = pulseScale * 1.7f
                                alpha = pulseAlpha * 0.5f
                            }
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }

                IconButton(
                    onClick = { startVoiceInput() },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isRecording) accentColor else Color(0xFF1C2B18))
                        .border(
                            width = if (isRecording) 2.dp else 1.dp,
                            color = if (isRecording) Color.White else accentColor.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .testTag("lumo_mic_button")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.GraphicEq else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isRecording) Color.Black else accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Text Input Box
            TextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = {
                    Text(
                        if (isRecording) "Listening... Speak now..." else "Ask Mythic or tap mic to speak...",
                        color = if (isRecording) accentColor else Color.Gray,
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("lumo_text_input")
            )

            // Send Button
            IconButton(
                onClick = { sendMessage() },
                enabled = messageInput.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (messageInput.isNotBlank()) accentColor else Color(0xFF222222))
                    .testTag("lumo_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = if (messageInput.isNotBlank()) Color.Black else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showUsageCapDialog) {
        com.example.ui.components.SubscriptionUpgradeDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissUsageCapDialog() },
            onNavigateToSubscription = {
                viewModel.dismissUsageCapDialog()
                onNavigateToSubscription?.invoke()
            }
        )
    }

    if (showFakeAd) {
        FakeAdScreen(onAdFinished = {
            showFakeAd = false
            pendingMessage?.let {
                viewModel.sendMythicMessage(it)
                pendingMessage = null
            }
            messageInput = ""
        })
    }
}

@Composable
fun AudioEqualizerWaveform(accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "audio_eq")
    val bar1 by transition.animateFloat(initialValue = 6f, targetValue = 22f, animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "eq1")
    val bar2 by transition.animateFloat(initialValue = 18f, targetValue = 8f, animationSpec = infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Reverse), label = "eq2")
    val bar3 by transition.animateFloat(initialValue = 8f, targetValue = 26f, animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "eq3")
    val bar4 by transition.animateFloat(initialValue = 20f, targetValue = 10f, animationSpec = infiniteRepeatable(tween(310, easing = LinearEasing), RepeatMode.Reverse), label = "eq4")
    val bar5 by transition.animateFloat(initialValue = 10f, targetValue = 18f, animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse), label = "eq5")

    val barHeights = listOf(bar1, bar2, bar3, bar4, bar5)

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(26.dp)
    ) {
        barHeights.forEach { heightDp ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: MythicConversationEntity,
    accentColor: Color,
    onQuickPrompt: (String) -> Unit
) {
    val isUser = message.role == "user"
    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E3A1E))
                        .border(1.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) accentColor else Color(0xFF181818))
                    .border(
                        1.dp,
                        if (isUser) Color.Transparent else Color(0xFF2C2C2C),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = message.content,
                        color = if (isUser) Color.Black else Color(0xFFE8E8E8),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeFormatted,
                        color = if (isUser) Color.Black.copy(alpha = 0.6f) else Color(0xFF777777),
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E3A1E))
                .border(1.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF181818))
                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Mythic is thinking", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                val infiniteTransition = rememberInfiniteTransition(label = "dots")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Text("✨", fontSize = 12.sp, modifier = Modifier.padding(start = 2.dp))
            }
        }
    }
}
