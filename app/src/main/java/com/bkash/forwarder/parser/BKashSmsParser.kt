package com.bkash.forwarder.parser

import java.util.Locale
import java.util.regex.Pattern

object BKashSmsParser {

    private val BKASH_SENDERS = listOf("bkash", "16247", "bkashinfo", "bkash-info")

    // Critical security blacklist: NEVER forward OTPs, verification codes, or PIN messages!
    private val SECURITY_PATTERNS = listOf(
        "(?i)\\b(otp|one[\\s-]?time[\\s-]?password)\\b",
        "(?i)\\bverification\\s+code\\b",
        "(?i)\\bverify\\s+code\\b",
        "(?i)\\bsecret\\s+code\\b",
        "(?i)\\bsecurity\\s+code\\b",
        "(?i)\\btemporary\\s+pin\\b",
        "(?i)\\bdo\\s+not\\s+share\\s+(?:this|your)\\s+(?:code|pin|password|otp)\\b",
        "(?i)\\bnever\\s+share\\s+(?:your\\s+)?pin\\b",
        "(?i)\\breset\\s+(?:your\\s+)?pin\\b",
        "(?i)\\bpin\\s+change\\b",
        "(?i)\\byour\\s+pin\\s+is\\b"
    ).map { Pattern.compile(it) }

    // TrxID pattern: e.g. TrxID BL49XXK98, Trx ID: 9K34JK23
    private val TRX_ID_REGEX = Pattern.compile(
        "(?i)(?:trxid|trx\\s*id|txnid|txn\\s*id|transaction\\s*id)[:\\s]+([a-z0-9]+)"
    )

    // Amount pattern: Tk 1,500.00 or Tk. 200 or BDT 500
    private val AMOUNT_REGEX = Pattern.compile(
        "(?i)(?:tk\\.?|bdt)\\s*([0-9,]+(?:\\.[0-9]{2})?)"
    )

    // Balance pattern: Balance Tk 5,230.50
    private val BALANCE_REGEX = Pattern.compile(
        "(?i)balance\\s+(?:tk\\.?|bdt)\\s*([0-9,]+(?:\\.[0-9]{2})?)"
    )

    // Fee pattern: Fee Tk 0.00 or Fee Tk 18.50
    private val FEE_REGEX = Pattern.compile(
        "(?i)fee\\s+(?:tk\\.?|bdt)\\s*([0-9,]+(?:\\.[0-9]{2})?)"
    )

    // Counterpart (from/to number or party): from 01712345678 or to 01987654321
    private val COUNTERPART_REGEX = Pattern.compile(
        "(?i)(?:from|to)\\s+([0-9+]{10,14}|[A-Za-z0-9\\s]{3,25}?)(?=\\.|\\s+successful|\\s+Fee|\\s+Balance|\\s+at|\\s+ref|$)"
    )

    // Reference pattern: Ref Lunch or Ref: Invoice 101
    private val REF_REGEX = Pattern.compile(
        "(?i)(?:ref|reference)[:\\s]+([^.\\n]+?)(?=\\.|\\s+Balance|\\s+Fee|\\s+TrxID|\\s+at|$)"
    )

    // Date Time pattern: at 11/11/2026 14:20 or at 11/11/2026 02:20 PM
    private val DATE_TIME_REGEX = Pattern.compile(
        "(?i)at\\s+([0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4}\\s+[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?(?:\\s*[AaPp][Mm])?)"
    )

    /**
     * Checks if sender string matches known bKash senders.
     */
    fun isBkashSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val normalized = sender.trim().lowercase(Locale.ROOT)
        return BKASH_SENDERS.any { normalized.contains(it) }
    }

    /**
     * Parses an incoming SMS message.
     */
    fun parse(sender: String?, messageBody: String?): ParseResult {
        if (messageBody.isNullOrBlank()) {
            return ParseResult.NotATransaction("Empty message body")
        }

        val cleanBody = messageBody.trim()

        // 1. Strict Security Audit: Never forward OTPs or PINs under ANY circumstances!
        for (pattern in SECURITY_PATTERNS) {
            if (pattern.matcher(cleanBody).find()) {
                return ParseResult.SecurityBlocked(
                    "Security sensitive content detected (OTP/PIN/Password). Automatically dropped."
                )
            }
        }

        // 2. Sender and bKash Content Verification
        val isSenderBkash = isBkashSender(sender)
        val isBodyBkash = cleanBody.contains("bkash", ignoreCase = true) || cleanBody.contains("16247")
        val hasTrxId = TRX_ID_REGEX.matcher(cleanBody).find()

        if (!isSenderBkash && !(isBodyBkash && hasTrxId)) {
            return ParseResult.NonBkashSender(sender ?: "unknown")
        }

        // 3. Transaction ID Extraction
        val trxMatcher = TRX_ID_REGEX.matcher(cleanBody)
        if (!trxMatcher.find()) {
            return ParseResult.NotATransaction(
                "No valid Transaction ID (TrxID) found. Likely promotional or informational."
            )
        }
        val trxId = trxMatcher.group(1)?.trim() ?: return ParseResult.NotATransaction("TrxID extraction failed")

        // 3. Amount Extraction
        val amountMatcher = AMOUNT_REGEX.matcher(cleanBody)
        val amount = if (amountMatcher.find()) {
            amountMatcher.group(1)?.trim() ?: "0.00"
        } else {
            "0.00"
        }

        // 4. Balance Extraction
        val balanceMatcher = BALANCE_REGEX.matcher(cleanBody)
        val balance = if (balanceMatcher.find()) {
            balanceMatcher.group(1)?.trim() ?: ""
        } else {
            ""
        }

        // 5. Fee Extraction
        val feeMatcher = FEE_REGEX.matcher(cleanBody)
        val fee = if (feeMatcher.find()) {
            feeMatcher.group(1)?.trim() ?: ""
        } else {
            ""
        }

        // 6. Counterpart Extraction
        val counterpartMatcher = COUNTERPART_REGEX.matcher(cleanBody)
        val counterpart = if (counterpartMatcher.find()) {
            counterpartMatcher.group(1)?.trim() ?: ""
        } else {
            ""
        }

        // 7. Reference Extraction
        val refMatcher = REF_REGEX.matcher(cleanBody)
        val reference = if (refMatcher.find()) {
            refMatcher.group(1)?.trim() ?: ""
        } else {
            ""
        }

        // 8. Date Time Extraction
        val dateMatcher = DATE_TIME_REGEX.matcher(cleanBody)
        val dateTimeStr = if (dateMatcher.find()) {
            dateMatcher.group(1)?.trim() ?: ""
        } else {
            ""
        }

        // 9. Transaction Type Detection
        val type = detectTransactionType(cleanBody)

        val transaction = ParsedTransaction(
            trxId = trxId,
            type = type,
            amount = amount,
            counterpart = counterpart,
            balance = balance,
            fee = fee,
            reference = reference,
            dateTimeStr = dateTimeStr,
            rawSms = cleanBody,
            senderId = sender?.trim() ?: "bKash"
        )

        return ParseResult.Success(transaction)
    }

    private fun detectTransactionType(body: String): String {
        val lower = body.lowercase(Locale.ROOT)
        return when {
            lower.contains("cash in") -> "Cash In"
            lower.contains("payment") && (lower.contains("received from") || lower.contains("received")) -> "Payment Received"
            lower.contains("payment") && lower.contains("to") -> "Payment Sent"
            lower.contains("you have received") && lower.contains("from") -> "Received Money"
            lower.contains("send money") -> "Send Money"
            lower.contains("cash out") -> "Cash Out"
            lower.contains("remittance") -> "Remittance"
            lower.contains("add money") -> "Add Money"
            lower.contains("recharge") -> "Mobile Recharge"
            else -> "bKash Transaction"
        }
    }
}
