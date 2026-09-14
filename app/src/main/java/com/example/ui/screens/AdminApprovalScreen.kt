package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.remote.SupabasePaymentOrder
import com.example.ui.components.EmblemSize
import com.example.ui.components.SubscriptionBadge
import com.example.ui.components.SubscriptionEmblem
import com.example.viewmodel.MythicViewModel
import com.example.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalScreen(
    viewModel: MythicViewModel,
    subscriptionViewModel: SubscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val adminOrders by subscriptionViewModel.adminOrders.collectAsState()
    val isLoading by subscriptionViewModel.adminLoading.collectAsState()

    var selectedFilter by remember { mutableStateOf("needs_review") } // all, needs_review, verified, rejected
    var searchQuery by remember { mutableStateOf("") }
    var viewingReceiptUrl by remember { mutableStateOf<String?>(null) }
    var rejectingOrder by remember { mutableStateOf<SupabasePaymentOrder?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("Invalid bank transaction details or receipt.") }
    var approvingOrder by remember { mutableStateOf<SupabasePaymentOrder?>(null) }

    LaunchedEffect(Unit) {
        subscriptionViewModel.fetchAdminOrders()
    }

    val filteredOrders = remember(adminOrders, selectedFilter, searchQuery) {
        adminOrders.filter { order ->
            val status = order.status.lowercase()
            val matchesFilter = when (selectedFilter) {
                "needs_review" -> status in listOf("pending", "submitted")
                "verified" -> status in listOf("verified", "approved")
                "rejected" -> status == "rejected"
                else -> true
            }

            val query = searchQuery.trim().lowercase()
            val matchesSearch = query.isEmpty() ||
                    order.paymentReference.lowercase().contains(query) ||
                    (order.userEmail?.lowercase()?.contains(query) == true) ||
                    (order.bankTransactionId?.lowercase()?.contains(query) == true) ||
                    (order.userId?.lowercase()?.contains(query) == true)

            matchesFilter && matchesSearch
        }
    }

    val pendingCount = remember(adminOrders) {
        adminOrders.count { it.status.lowercase() in listOf("pending", "submitted") }
    }
    val verifiedCount = remember(adminOrders) {
        adminOrders.count { it.status.lowercase() in listOf("verified", "approved") }
    }

    Scaffold(
        containerColor = Color(0xFF0D1117),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Mythic Admin Center",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ADMIN", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text(
                            "Subscription Verification & Approvals",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { subscriptionViewModel.fetchAdminOrders() },
                        modifier = Modifier.testTag("admin_refresh_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFFFD700)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161B22))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Stats Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pending Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Needs Review", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$pendingCount", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Approved Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Active Approved", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$verifiedCount", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Total Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Total Orders", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${adminOrders.size}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Ref code, Email, Tx ID...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF161B22),
                        unfocusedContainerColor = Color(0xFF161B22),
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_input")
                )
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "needs_review" to "Review ($pendingCount)",
                        "all" to "All (${adminOrders.size})",
                        "verified" to "Approved",
                        "rejected" to "Rejected"
                    )

                    tabs.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFFFD700) else Color(0xFF21262D))
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Orders list or Empty State
            if (filteredOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛡️", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No subscription orders found",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Orders submitted by users will appear here for approval.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredOrders, key = { it.id ?: it.paymentReference }) { order ->
                    AdminOrderCard(
                        order = order,
                        onViewReceipt = { viewingReceiptUrl = it },
                        onApprove = { approvingOrder = order },
                        onReject = { rejectingOrder = order }
                    )
                }
            }
        }
    }

    // Receipt Fullscreen Preview Dialog
    viewingReceiptUrl?.let { url ->
        Dialog(onDismissRequest = { viewingReceiptUrl = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bank Receipt Proof", color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewingReceiptUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AsyncImage(
                        model = url,
                        contentDescription = "Bank Receipt",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        url,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Confirm Approval Dialog
    approvingOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { approvingOrder = null },
            containerColor = Color(0xFF161B22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👑", fontSize = 24.sp)
                    Text("Approve Subscription?", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val planTitle = if (order.plan.lowercase() in listOf("max", "prime")) "Mythic Prime ($5.00/yr)" else "Mythic Pro ($1.99/mo)"
                    Text("Are you sure you want to approve this payment order?", color = Color.LightGray)
                    Text("• Order Ref: ${order.paymentReference}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• Plan: $planTitle", color = Color.White, fontSize = 13.sp)
                    Text("• User: ${order.userEmail ?: order.userId}", color = Color.White, fontSize = 13.sp)
                    Text("• Bank Tx ID: ${order.bankTransactionId ?: "N/A"}", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "This will instantly activate the user's subscription entitlement, grant the subscription emblem, and unlock all premium features.",
                        color = Color(0xFF86FC5C),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = approvingOrder ?: return@Button
                        subscriptionViewModel.approveOrder(
                            order = current,
                            onSuccess = {
                                Toast.makeText(context, "Subscription approved & activated!", Toast.LENGTH_SHORT).show()
                                approvingOrder = null
                            },
                            onError = { err ->
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                approvingOrder = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.testTag("confirm_approve_button")
                ) {
                    Text("Approve & Activate", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { approvingOrder = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Reject Dialog
    rejectingOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { rejectingOrder = null },
            containerColor = Color(0xFF161B22),
            title = {
                Text("Reject Payment Order", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Please provide a reason for rejecting order ${order.paymentReference}:",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Rejection Reason", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF4B5563)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val orderId = order.id ?: ""
                        subscriptionViewModel.rejectOrder(
                            orderId = orderId,
                            reason = rejectionReasonInput,
                            onSuccess = {
                                Toast.makeText(context, "Payment order marked as rejected", Toast.LENGTH_SHORT).show()
                                rejectingOrder = null
                            },
                            onError = { err ->
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                rejectingOrder = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("confirm_reject_button")
                ) {
                    Text("Reject Order", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingOrder = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AdminOrderCard(
    order: SupabasePaymentOrder,
    onViewReceipt: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val status = order.status.lowercase()
    val isNeedsReview = status in listOf("pending", "submitted")
    val isVerified = status in listOf("verified", "approved")
    val isRejected = status == "rejected"

    val statusColor = when {
        isVerified -> Color(0xFF10B981)
        isNeedsReview -> Color(0xFFF59E0B)
        isRejected -> Color(0xFFEF4444)
        else -> Color.Gray
    }

    val displayPlan = if (order.plan.lowercase() in listOf("max", "prime")) "PRIME" else "PRO"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_order_card_${order.paymentReference}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isNeedsReview) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color(0xFF30363D)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubscriptionEmblem(
                        tier = order.plan,
                        size = EmblemSize.MEDIUM
                    )
                    Column {
                        Text(
                            text = order.paymentReference,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Mythic $displayPlan • $${String.format("%.2f", order.amount)} ${order.currency}",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status.uppercase(),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF30363D), thickness = 0.8.dp)

            // User & Transaction Info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Customer:", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        order.userEmail ?: order.userId ?: "Unknown",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bank Tx ID:", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        order.bankTransactionId ?: "Pending Submission",
                        color = if (order.bankTransactionId.isNullOrBlank()) Color.Gray else Color(0xFF86FC5C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!order.createdAt.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Created At:", color = Color.Gray, fontSize = 11.sp)
                        Text(order.createdAt, color = Color.Gray, fontSize = 10.sp)
                    }
                }

                if (isRejected && !order.rejectionReason.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Reason:", color = Color(0xFFEF4444), fontSize = 11.sp)
                        Text(order.rejectionReason, color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }

            // Receipt proof preview / view button if available
            val receiptUrl = order.receiptImageUrl
            if (!receiptUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF21262D))
                        .clickable { onViewReceipt(receiptUrl) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Text("View Attached Receipt Image", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Action Buttons
            if (isNeedsReview) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_reject_btn_${order.paymentReference}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                        Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("admin_approve_btn_${order.paymentReference}"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("✓ Approve & Activate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
