package com.bkash.forwarder

import com.bkash.forwarder.parser.BKashSmsParser
import com.bkash.forwarder.parser.ParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BKashSmsParserTest {

    @Test
    fun testCashInSuccess() {
        val sms = "You have received Tk 1,500.00 from 01712345678. Fee Tk 0.00. Balance Tk 5,230.50. TrxID BL49XXK98 at 11/11/2026 14:20"
        val result = BKashSmsParser.parse("bKash", sms)

        assertTrue("Expected ParseResult.Success", result is ParseResult.Success)
        val tx = (result as ParseResult.Success).transaction
        assertEquals("BL49XXK98", tx.trxId)
        assertEquals("1,500.00", tx.amount)
        assertEquals("01712345678", tx.counterpart)
        assertEquals("5,230.50", tx.balance)
        assertEquals("0.00", tx.fee)
        assertEquals("11/11/2026 14:20", tx.dateTimeStr)
    }

    @Test
    fun testPaymentReceivedWithReference() {
        val sms = "Payment Tk 350.00 received from 01612345678. Fee Tk 0.00. Balance Tk 8,950.00. TrxID 8F76MN45 at 11/11/2026 10:15. Ref Invoice 101"
        val result = BKashSmsParser.parse("16247", sms)

        assertTrue("Expected ParseResult.Success", result is ParseResult.Success)
        val tx = (result as ParseResult.Success).transaction
        assertEquals("8F76MN45", tx.trxId)
        assertEquals("350.00", tx.amount)
        assertEquals("01612345678", tx.counterpart)
        assertEquals("Invoice 101", tx.reference)
        assertEquals("Payment Received", tx.type)
    }

    @Test
    fun testOtpMessageIsStrictlyBlocked() {
        val sms = "Your bKash verification code is 849201. Do not share this code or your PIN with anyone."
        val result = BKashSmsParser.parse("bKash", sms)

        assertTrue("OTP message must be SecurityBlocked", result is ParseResult.SecurityBlocked)
    }

    @Test
    fun testPinResetMessageIsStrictlyBlocked() {
        val sms = "Your temporary PIN is 4920. Never share your bKash PIN with anyone including bKash agent."
        val result = BKashSmsParser.parse("bKash", sms)

        assertTrue("PIN reset message must be SecurityBlocked", result is ParseResult.SecurityBlocked)
    }

    @Test
    fun testNonBkashSenderIsIgnored() {
        val sms = "Hey, please send me the report by afternoon."
        val result = BKashSmsParser.parse("01711223344", sms)

        assertTrue("Personal sender must be NonBkashSender", result is ParseResult.NonBkashSender)
    }

    @Test
    fun testPromoMessageWithoutTrxIdRejected() {
        val sms = "Enjoy 50% cashback on your next restaurant payment with bKash app! Visit bkash.com/offers"
        val result = BKashSmsParser.parse("bKash", sms)

        assertTrue("Promo message without TrxID must be NotATransaction", result is ParseResult.NotATransaction)
    }
}
