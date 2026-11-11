package com.bkash.forwarder.data.local

data class TransactionRecord(
    val id: Long = 0,
    val trxId: String,
    val type: String,
    val amount: String,
    val counterpart: String,
    val balance: String,
    val fee: String,
    val reference: String,
    val rawSms: String,
    val forwardedText: String,
    val status: ForwardStatus,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ForwardStatus {
    SENT,
    FAILED,
    PENDING,
    IGNORED_DUPLICATE
}
