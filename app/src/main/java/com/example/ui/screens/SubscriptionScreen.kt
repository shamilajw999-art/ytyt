package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.data.remote.SupabasePaymentOrder
import com.example.ui.components.GlassSurface
import com.example.viewmodel.MythicViewModel
import com.example.viewmodel.SubscriptionViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SubscriptionScreen(
    viewModel: MythicViewModel,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isAnnual by subscriptionViewModel.isAnnual.collectAsState()
    val isLoading by subscriptionViewModel.isLoading.collectAsState()
    val isSubmitting by subscriptionViewModel.isSubmitting.collectAsState()
    val paymentOrders by subscriptionViewModel.paymentOrders.collectAsState()
    val latestOrder by subscriptionViewModel.latestPaymentOrder.collectAsState()
    val entitlements by viewModel.entitlements.collectAsState()
    val userProfile by viewModel.profile.collectAsState()
    
    val currentTier = entitlements?.plan?.lowercase()
        ?: userProfile?.subscriptionTier?.lowercase()
        ?: "free"
    val isProActive = currentTier == "pro" && entitlements?.status != "expired"
    val isMaxActive = (currentTier == "max" || currentTier == "prime") && entitlements?.status != "expired"

    val expiresAtStr = entitlements?.expiresAt
    val daysRemaining = remember(expiresAtStr) {
        if (!expiresAtStr.isNullOrBlank()) {
            try {
                val exp = java.time.Instant.parse(expiresAtStr)
                val now = java.time.Instant.now()
                java.time.temporal.ChronoUnit.DAYS.between(now, exp).toInt()
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    var showBankDetailsDialog by remember { mutableStateOf(false) }
    var showSubmitPaymentDialog by remember { mutableStateOf(false) }
    var showSubmissionSuccessDialog by remember { mutableStateOf(false) }
    
    var activeOrderForSubmission by remember { mutableStateOf<SupabasePaymentOrder?>(null) }
    var selectedTier by remember { mutableStateOf("pro") }

    LaunchedEffect(Unit) {
        subscriptionViewModel.fetchSubscriptions()
        subscriptionViewModel.fetchPaymentOrders()
        subscriptionViewModel.refreshEntitlements()
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "Mythic Premium",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        subscriptionViewModel.refreshEntitlements {
                            Toast.makeText(context, "Subscription status updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Entitlements", tint = Color(0xFF86FC5C))
                }
            }
        }

        item {
            Text(
                "Choose Your Heritage Journey (Bank Transfer Only)",
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // Active Under Review or Pending Payment Banner
        val underReviewOrder = paymentOrders.firstOrNull { it.status == "under_review" }
            ?: (if (latestOrder?.status == "under_review") latestOrder else null)
        val pendingOrder = paymentOrders.firstOrNull { it.status == "pending" }
            ?: (if (latestOrder?.status == "pending") latestOrder else null)
        val rejectedOrder = paymentOrders.firstOrNull { it.status == "rejected" }

        if (underReviewOrder != null && !isProActive && !isMaxActive) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF241F0A)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Payment Under Review",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Plan: Mythic ${underReviewOrder.plan.uppercase()}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Ref: ${underReviewOrder.paymentReference}", color = Color.LightGray, fontSize = 12.sp)
                                if (!underReviewOrder.bankTransactionId.isNullOrBlank()) {
                                    Text("Bank Tx ID: ${underReviewOrder.bankTransactionId}", color = Color(0xFF86FC5C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            "We'll activate your subscription after the transfer is verified by the admin team in the Admin Dashboard.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = {
                                subscriptionViewModel.refreshEntitlements {
                                    Toast.makeText(context, "Checked status", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Check Verification Status", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (pendingOrder != null && !isProActive && !isMaxActive) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Color(0xFF86FC5C), RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2414)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📋", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Pending Bank Transfer Order",
                                    color = Color(0xFF86FC5C),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Ref: ${pendingOrder.paymentReference} • Amount: $${String.format("%.2f", pendingOrder.amount)}",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            "Have you completed the bank transfer? Submit your transaction ID or receipt below to send it to the Admin Dashboard for instant approval.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Button(
                            onClick = {
                                activeOrderForSubmission = pendingOrder
                                showSubmitPaymentDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF86FC5C), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Transfer Details & Receipt", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (rejectedOrder != null && !isProActive && !isMaxActive) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("❌ Payment Verification Unsuccessful", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "The previous bank transfer reference could not be verified by the admin team. Please verify transfer details and submit a new request below.",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Expiration Notice Banner if within 7 days
        if (daysRemaining != null && daysRemaining in 0..7) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (daysRemaining == 0) "Subscription Expires Today" else "Subscription Ending in $daysRemaining Days",
                                color = Color(0xFFFFB74D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Your Mythic ${currentTier.uppercase()} plan is about to end. Renew now to avoid losing your subscriber emblem, ad-free access, and exclusive perks.",
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        } else if (daysRemaining != null && daysRemaining < 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⛔", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Subscription Expired",
                                color = Color(0xFFFF5252),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Your Mythic subscription has ended. Choose a plan below to reactivate your subscriber perks.",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Current Active Plan Status indicator
        if (isProActive || isMaxActive) {
            item {
                Surface(
                    color = (if (isMaxActive) Color(0xFFFFD700) else Color(0xFF86FC5C)).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isMaxActive) Color(0xFFFFD700).copy(alpha = 0.4f) else Color(0xFF86FC5C).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isMaxActive) "👑" else "⭐", fontSize = 20.sp)
                            Column {
                                val activeTierLabel = if (currentTier in listOf("max", "prime")) "PRIME" else currentTier.uppercase()
                                Text(
                                    text = "Active Plan: Mythic $activeTierLabel",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (daysRemaining != null && daysRemaining > 0) "$daysRemaining days remaining" else "Perks Active & Unlocked",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Surface(
                            color = if (isMaxActive) Color(0xFFFFD700) else Color(0xFF86FC5C),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Billing Toggle
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(4.dp)
            ) {
                Surface(
                    onClick = { if (isAnnual) subscriptionViewModel.toggleBillingCycle() },
                    color = if (!isAnnual) Color(0xFF86FC5C) else Color.Transparent,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Monthly", color = if (!isAnnual) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Surface(
                    onClick = { if (!isAnnual) subscriptionViewModel.toggleBillingCycle() },
                    color = if (isAnnual) Color(0xFF86FC5C) else Color.Transparent,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Annual", color = if (isAnnual) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(color = Color(0xFFFF4D4D), shape = RoundedCornerShape(4.dp)) {
                                Text("Best Value", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // Pro Package Card ($1.99)
        item {
            SubscriptionCard(
                title = "Mythic Pro",
                price = if (isAnnual) "$1.99" else "$1.99",
                duration = if (isAnnual) "/ year" else "/ month",
                billingLabel = if (isAnnual) "Billed annually ($1.99)" else "Billed monthly ($1.99)",
                description = "Perfect for focused explorers and audio guides.",
                features = listOf(
                    "Ad-free experience",
                    "Unlimited Lumo AI chats",
                    "High-resolution heritage maps",
                    "Priority support"
                ),
                accentColor = Color(0xFF86FC5C),
                isCurrentPlan = isProActive,
                isDisabled = isProActive || isMaxActive,
                buttonText = when {
                    isProActive -> "Current Plan Active ✓"
                    isMaxActive -> "Included in Mythic Prime"
                    else -> "Get Mythic Pro ($1.99)"
                },
                onSelect = {
                    if (!isProActive && !isMaxActive) {
                        selectedTier = "pro"
                        subscriptionViewModel.requestBankTransfer(
                            tier = "pro",
                            onSuccess = { order ->
                                activeOrderForSubmission = order
                                showBankDetailsDialog = true
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            )
        }

        // Prime Package Card ($5.00)
        item {
            Box {
                SubscriptionCard(
                    title = "Mythic Prime",
                    price = if (isAnnual) "$5.00" else "$5.00",
                    duration = if (isAnnual) "/ year" else "/ month",
                    billingLabel = if (isAnnual) "Billed annually ($5.00)" else "Billed monthly ($5.00)",
                    description = "The ultimate 3D AR heritage experience with all perks.",
                    features = listOf(
                        "Everything in Pro",
                        "Exclusive AR 3D Scan",
                        "Beta access to new sites",
                        "Digital collection badges",
                        "Custom profile background",
                        "Animated/live profile background",
                        "Advanced profile customization",
                        "Exclusive Passport features"
                    ),
                    accentColor = Color(0xFFFFD700),
                    isPremium = true,
                    isCurrentPlan = isMaxActive,
                    isDisabled = isMaxActive,
                    buttonText = when {
                        isMaxActive -> "Current Plan Active (Prime Tier) ✓"
                        isProActive -> "Upgrade to Mythic Prime ($5.00)"
                        else -> "Get Mythic Prime ($5.00)"
                    },
                    onSelect = {
                        if (!isMaxActive) {
                            selectedTier = "prime"
                            subscriptionViewModel.requestBankTransfer(
                                tier = "prime",
                                onSuccess = { order ->
                                    activeOrderForSubmission = order
                                    showBankDetailsDialog = true
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                )
                
                if (!isMaxActive) {
                    Surface(
                        color = Color(0xFFFF4D4D),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(top = 10.dp, end = 10.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            "POPULAR",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- DIALOG 1: BANK TRANSFER INSTRUCTIONS ---
    if (showBankDetailsDialog && activeOrderForSubmission != null) {
        val order = activeOrderForSubmission!!
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        AlertDialog(
            onDismissRequest = { showBankDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏛️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bank Transfer Instructions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Plan: Mythic ${order.plan.uppercase()} • Total Amount: $${String.format("%.2f", order.amount)} USD",
                        color = Color(0xFF86FC5C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    // Reference Code Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161F16)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFF86FC5C), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Your Unique Mythic Reference:", color = Color.LightGray, fontSize = 11.sp)
                                Text(
                                    order.paymentReference,
                                    color = Color(0xFF86FC5C),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setPrimaryClip(
                                        ClipData.newPlainText("Mythic Reference", order.paymentReference)
                                    )
                                    Toast.makeText(context, "Reference code copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Reference", tint = Color.White)
                            }
                        }
                    }

                    // Bank Account Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Bank: Commercial Bank", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Account Name: J S Shamila Jayawardena", color = Color.White, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Account Number:", color = Color.Gray, fontSize = 11.sp)
                                    Text("8110011901", color = Color(0xFF86FC5C), fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                                TextButton(
                                    onClick = {
                                        clipboardManager.setPrimaryClip(
                                            ClipData.newPlainText("Account Number", "8110011901")
                                        )
                                        Toast.makeText(context, "Account number copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Copy Acc", color = Color(0xFF86FC5C), fontSize = 12.sp)
                                }
                            }
                            Text("Branch: Kollupitiya Branch", color = Color.LightGray, fontSize = 13.sp)
                        }
                    }

                    Text(
                        "Important: Please include your Reference (${order.paymentReference}) in your bank transfer narration/remarks so our admin team can verify it immediately.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBankDetailsDialog = false
                        showSubmitPaymentDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF86FC5C), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I've Made the Transfer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBankDetailsDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        )
    }

    // --- DIALOG 2: SUBMIT PAYMENT DETAILS & RECEIPT ---
    if (showSubmitPaymentDialog && activeOrderForSubmission != null) {
        val order = activeOrderForSubmission!!
        var bankTxId by remember { mutableStateOf("") }
        var transferAmount by remember { mutableStateOf(String.format("%.2f", order.amount)) }
        var transferDateTime by remember {
            mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showSubmitPaymentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📤", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Bank Transfer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Mythic Reference: ${order.paymentReference}",
                        color = Color(0xFF86FC5C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = bankTxId,
                        onValueChange = { bankTxId = it },
                        label = { Text("Bank Transaction ID / Ref # *") },
                        placeholder = { Text("e.g. TXN-98471203") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF86FC5C),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("Amount Transferred ($)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF86FC5C),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = transferDateTime,
                        onValueChange = { transferDateTime = it },
                        label = { Text("Transfer Date & Time") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF86FC5C),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Receipt Picker
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Receipt Screenshot", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (selectedImageUri != null) "Receipt attached ✓" else "Optional but speeds up verification",
                                    color = if (selectedImageUri != null) Color(0xFF86FC5C) else Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                            if (selectedImageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedImageUri),
                                    contentDescription = "Receipt Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedImageUri != null) Color.White.copy(alpha = 0.15f) else Color(0xFF86FC5C),
                                    contentColor = if (selectedImageUri != null) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (selectedImageUri != null) "Change" else "Attach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bankTxId.trim().isEmpty()) {
                            errorMessage = "Please enter your bank transaction ID."
                            return@Button
                        }
                        errorMessage = null

                        var imageBytes: ByteArray? = null
                        var fileName: String? = null
                        if (selectedImageUri != null) {
                            try {
                                imageBytes = context.contentResolver.openInputStream(selectedImageUri!!)?.use { it.readBytes() }
                                fileName = "receipt_${order.paymentReference}.jpg"
                            } catch (e: Exception) {
                                android.util.Log.e("SubscriptionScreen", "Failed to read image", e)
                            }
                        }

                        val realOrderId = order.id
                        if (realOrderId.isNullOrBlank() || realOrderId.startsWith("MYTHIC-")) {
                            errorMessage = "Invalid order ID from server. Please initiate a new order."
                            return@Button
                        }

                        val parsedAmount = transferAmount.toDoubleOrNull() ?: order.amount

                        subscriptionViewModel.submitPaymentDetails(
                            paymentOrderId = realOrderId,
                            paymentReference = order.paymentReference,
                            bankTxId = bankTxId.trim(),
                            amount = parsedAmount,
                            transferDatetime = transferDateTime,
                            receiptBytes = imageBytes,
                            receiptFileName = fileName,
                            onSuccess = {
                                showSubmitPaymentDialog = false
                                showSubmissionSuccessDialog = true
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF86FC5C), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submitting...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Submit for Verification", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isSubmitting) {
                    TextButton(onClick = { showSubmitPaymentDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            },
            containerColor = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
        )
    }

    // --- DIALOG 3: SUBMISSION CONFIRMATION ---
    if (showSubmissionSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSubmissionSuccessDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = "Success", tint = Color(0xFF86FC5C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Payment Submitted for Review!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Your bank transfer details have been forwarded to the Mythic Admin Dashboard.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF86FC5C).copy(alpha = 0.1f))
                            .border(1.dp, Color(0xFF86FC5C), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text("Status: UNDER REVIEW ⏳", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(
                                "Once our admin team verifies the transfer against our bank records, your Pro/Max subscription and badges will activate automatically.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmissionSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF86FC5C), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, Color(0xFF86FC5C).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        )
    }
}

@Composable
fun SubscriptionCard(
    title: String,
    price: String,
    duration: String,
    billingLabel: String = "",
    description: String,
    features: List<String>,
    accentColor: Color,
    isPremium: Boolean = false,
    isCurrentPlan: Boolean = false,
    isDisabled: Boolean = false,
    buttonText: String? = null,
    onSelect: () -> Unit
) {
    val effectiveBorderColor = when {
        isCurrentPlan -> accentColor.copy(alpha = 0.8f)
        isPremium -> accentColor.copy(alpha = 0.5f)
        isDisabled -> Color.White.copy(alpha = 0.05f)
        else -> Color.White.copy(alpha = 0.1f)
    }

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentPlan) 2.dp else 1.dp,
                color = effectiveBorderColor,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .alpha(if (isDisabled && !isCurrentPlan) 0.5f else 1.0f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCurrentPlan) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "YOUR CURRENT ACTIVE PLAN",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            } else if (isPremium) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    price,
                    color = if (isDisabled && !isCurrentPlan) Color.Gray else accentColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(duration, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
            
            if (billingLabel.isNotEmpty()) {
                Text(billingLabel, color = Color.Gray, fontSize = 11.sp)
            }
            
            Text(
                description,
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 16.dp))
            
            features.forEach { feature ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isDisabled && !isCurrentPlan) Color.Gray else accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(feature, color = if (isDisabled && !isCurrentPlan) Color.Gray else Color.White, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val btnText = buttonText ?: "Get $title"
            val isButtonEnabled = !isDisabled && !isCurrentPlan
            
            Button(
                onClick = onSelect,
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentPlan) Color.White.copy(alpha = 0.08f) else accentColor,
                    disabledContainerColor = if (isCurrentPlan) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                    contentColor = if (isCurrentPlan) accentColor else Color.Black,
                    disabledContentColor = if (isCurrentPlan) accentColor else Color.Gray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = btnText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
