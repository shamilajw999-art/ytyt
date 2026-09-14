package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.SupabasePaymentOrder
import com.example.data.remote.SupabasePaymentSubmission
import com.example.data.remote.SupabaseSubscription
import com.example.data.repository.MythicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MythicRepository(application)
    
    private val _isAnnual = MutableStateFlow(true)
    val isAnnual = _isAnnual.asStateFlow()
    
    private val _subscriptions = MutableStateFlow<List<SupabaseSubscription>>(emptyList())
    val subscriptions = _subscriptions.asStateFlow()

    private val _paymentOrders = MutableStateFlow<List<SupabasePaymentOrder>>(emptyList())
    val paymentOrders = _paymentOrders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    private val _adminOrders = MutableStateFlow<List<SupabasePaymentOrder>>(emptyList())
    val adminOrders = _adminOrders.asStateFlow()

    private val _adminLoading = MutableStateFlow(false)
    val adminLoading = _adminLoading.asStateFlow()
    
    val activeSubscription = combine(repository.currentUser, _subscriptions) { user, subs ->
        val currentUserId = user?.id ?: repository.getProfile()?.id
        subs.find { (it.userId == currentUserId || currentUserId == null) && it.status == "active" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val latestPaymentOrder = _paymentOrders.map { orders ->
        orders.maxByOrNull { it.createdAt ?: "" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleBillingCycle() {
        _isAnnual.value = !_isAnnual.value
    }

    fun fetchSubscriptions() {
        viewModelScope.launch {
            _isLoading.value = true
            val user = repository.currentUser.value
            val profile = repository.getProfile()
            val userId = user?.id ?: profile?.id
            if (userId != null) {
                val subs = repository.getSubscriptions(userId)
                _subscriptions.value = subs
                val orders = repository.getPaymentOrders(userId)
                _paymentOrders.value = orders
            }
            _isLoading.value = false
        }
    }

    fun fetchPaymentOrders() {
        viewModelScope.launch {
            val user = repository.currentUser.value
            val profile = repository.getProfile()
            val userId = user?.id ?: profile?.id ?: return@launch
            val orders = repository.getPaymentOrders(userId)
            _paymentOrders.value = orders
        }
    }

    fun requestBankTransfer(
        tier: String, 
        onSuccess: (SupabasePaymentOrder) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repository.currentUser.value
                val profile = repository.getProfile()
                if (user == null && profile == null) {
                    onError("Please sign in or create an account to purchase a subscription.")
                    return@launch
                }

                when (val result = repository.createPaymentOrderRpc(tier.lowercase())) {
                    is com.example.data.repository.PaymentOrderResult.Success -> {
                        val order = result.order
                        val email = user?.email ?: profile?.email
                        if (!email.isNullOrBlank()) {
                            launch(Dispatchers.IO) {
                                try {
                                    kotlinx.coroutines.withTimeoutOrNull(3000) {
                                        repository.sendPaymentEmail(
                                            email, 
                                            tier, 
                                            String.format("%.2f", order.amount), 
                                            order.paymentReference
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("SubscriptionViewModel", "Async email send notice", e)
                                }
                            }
                        }

                        fetchPaymentOrders()
                        onSuccess(order)
                    }
                    is com.example.data.repository.PaymentOrderResult.Error -> {
                        onError(result.message)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionViewModel", "requestBankTransfer error", e)
                onError(e.localizedMessage ?: "Failed to initialize payment order.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitPaymentDetails(
        paymentOrderId: String,
        paymentReference: String,
        bankTxId: String,
        amount: Double,
        transferDatetime: String,
        receiptBytes: ByteArray?,
        receiptFileName: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val user = repository.currentUser.value
                val profile = repository.getProfile()
                val userId = user?.id ?: profile?.id
                if (userId.isNullOrBlank()) {
                    onError("Authentication required. Please sign in.")
                    return@launch
                }

                if (paymentOrderId.isBlank() || paymentOrderId.startsWith("MYTHIC-")) {
                    onError("Invalid payment order ID ($paymentOrderId). Please initiate a new order.")
                    return@launch
                }

                // 1. Upload receipt image before submit_payment_order RPC if provided
                var finalReceiptPath = ""
                if (receiptBytes != null && receiptBytes.isNotEmpty()) {
                    val cleanFileName = if (!receiptFileName.isNullOrBlank()) {
                        receiptFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                    } else {
                        "receipt_${paymentReference}_${System.currentTimeMillis()}.jpg"
                    }

                    val uploadedPath = repository.uploadReceiptImage(userId, receiptBytes, cleanFileName)
                    if (uploadedPath == null) {
                        onError("Receipt upload failed. Please verify your connection and try again.")
                        return@launch
                    }
                    finalReceiptPath = uploadedPath
                }

                // 2. Call submit_payment_order RPC with real order UUID and receipt path
                when (val submitResult = repository.submitPaymentOrderRpc(
                    orderId = paymentOrderId,
                    paymentReference = paymentReference,
                    bankTransactionId = bankTxId,
                    transferAmount = amount,
                    transferDatetime = transferDatetime,
                    receiptPath = finalReceiptPath
                )) {
                    is com.example.data.repository.PaymentSubmitResult.Success -> {
                        fetchPaymentOrders()
                        repository.refreshEntitlements()
                        onSuccess()
                    }
                    is com.example.data.repository.PaymentSubmitResult.Error -> {
                        onError(submitResult.message)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionViewModel", "submitPaymentDetails error", e)
                onError(e.localizedMessage ?: "An error occurred during submission.")
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun refreshEntitlements(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.refreshEntitlements()
            fetchSubscriptions()
            onComplete()
        }
    }

    fun fetchAdminOrders() {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val orders = repository.getAllPaymentOrdersAdmin()
                _adminOrders.value = orders
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionViewModel", "fetchAdminOrders error", e)
            } finally {
                _adminLoading.value = false
            }
        }
    }

    fun approveOrder(
        order: SupabasePaymentOrder,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val orderId = order.id ?: ""
                val userId = order.userId ?: ""
                val plan = order.plan
                val res = repository.adminApprovePaymentOrder(orderId, userId, plan)
                if (res.isSuccess) {
                    fetchAdminOrders()
                    onSuccess()
                } else {
                    onError("Failed to approve order: ${res.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Approval exception occurred")
            } finally {
                _adminLoading.value = false
            }
        }
    }

    fun rejectOrder(
        orderId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _adminLoading.value = true
            try {
                val res = repository.adminRejectPaymentOrder(orderId, reason)
                if (res.isSuccess) {
                    fetchAdminOrders()
                    onSuccess()
                } else {
                    onError("Failed to reject order: ${res.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Rejection exception occurred")
            } finally {
                _adminLoading.value = false
            }
        }
    }
    
    fun getProPrice(): String {
        return if (_isAnnual.value) "$1.66/mo" else "$1.99/mo"
    }
    
    fun getProBillingLabel(): String {
        return if (_isAnnual.value) "Billed annually ($19.99)" else "Billed monthly ($1.99)"
    }
    
    fun getMaxPrice(): String {
        return if (_isAnnual.value) "$4.16/mo" else "$5.00/mo"
    }
    
    fun getMaxBillingLabel(): String {
        return if (_isAnnual.value) "Billed annually ($49.99)" else "Billed monthly ($5.00)"
    }
}
