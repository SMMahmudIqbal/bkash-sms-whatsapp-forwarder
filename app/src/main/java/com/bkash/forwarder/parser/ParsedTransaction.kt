package com.bkash.forwarder.parser

sealed class ParseResult {
    data class Success(val transaction: ParsedTransaction) : ParseResult()
    data class SecurityBlocked(val reason: String) : ParseResult()
    data class NotATransaction(val reason: String) : ParseResult()
    data class NonBkashSender(val sender: String) : ParseResult()
}

data class ParsedTransaction(
    val trxId: String,
    val type: String,
    val amount: String,
    val counterpart: String,
    val balance: String,
    val fee: String,
    val reference: String,
    val dateTimeStr: String,
    val rawSms: String,
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Formats into a clean, professional WhatsApp markdown message.
     */
    fun toWhatsAppMessage(customHeader: String? = null): String {
        val header = if (!customHeader.isNullOrBlank()) customHeader else "🔔 *bKash Transaction Notification*"
        val refLine = if (reference.isNotBlank()) "\n🏷️ *Ref:* $reference" else ""
        val feeLine = if (fee.isNotBlank() && fee != "0.00" && fee != "0") "\n💸 *Fee:* ৳$fee" else ""
        val counterpartLabel = if (type.contains("Cash In", ignoreCase = true) ||
            type.contains("Received", ignoreCase = true)) "From" else "Party"

        return buildString {
            appendLine(header)
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 *Type:* $type")
            appendLine("💵 *Amount:* ৳$amount")
            if (counterpart.isNotBlank()) {
                appendLine("👤 *$counterpartLabel:* $counterpart")
            }
            appendLine("🆔 *TrxID:* `$trxId`")
            if (balance.isNotBlank()) {
                appendLine("💳 *New Balance:* ৳$balance")
            }
            if (dateTimeStr.isNotBlank()) {
                appendLine("🕒 *Time:* $dateTimeStr")
            }
            if (feeLine.isNotBlank()) {
                append(feeLine)
            }
            if (refLine.isNotBlank()) {
                append(refLine)
            }
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━")
            append("_⚡ Auto-forwarded by bKash Forwarder_")
        }
    }
}
