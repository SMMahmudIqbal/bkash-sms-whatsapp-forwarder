package com.bkash.forwarder.network

sealed class SendResult {
    data class Success(val messageId: String?, val responseBody: String) : SendResult()
    data class Failure(val errorMessage: String, val statusCode: Int? = null, val isNetworkError: Boolean = false) : SendResult()
}
