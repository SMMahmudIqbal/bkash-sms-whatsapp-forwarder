package com.bkash.forwarder.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TransactionDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "bkash_forwarder.db"
        const val DATABASE_VERSION = 1

        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_ID = "id"
        const val COL_TRX_ID = "trx_id"
        const val COL_TYPE = "type"
        const val COL_AMOUNT = "amount"
        const val COL_COUNTERPART = "counterpart"
        const val COL_BALANCE = "balance"
        const val COL_FEE = "fee"
        const val COL_REFERENCE = "reference"
        const val COL_RAW_SMS = "raw_sms"
        const val COL_FORWARDED_TEXT = "forwarded_text"
        const val COL_STATUS = "status"
        const val COL_RETRY_COUNT = "retry_count"
        const val COL_ERROR_MESSAGE = "error_message"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_UPDATED_AT = "updated_at"

        @Volatile
        private var instance: TransactionDatabaseHelper? = null

        fun getInstance(context: Context): TransactionDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: TransactionDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSql = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRX_ID TEXT NOT NULL UNIQUE,
                $COL_TYPE TEXT NOT NULL,
                $COL_AMOUNT TEXT NOT NULL,
                $COL_COUNTERPART TEXT,
                $COL_BALANCE TEXT,
                $COL_FEE TEXT,
                $COL_REFERENCE TEXT,
                $COL_RAW_SMS TEXT NOT NULL,
                $COL_FORWARDED_TEXT TEXT NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_RETRY_COUNT INTEGER DEFAULT 0,
                $COL_ERROR_MESSAGE TEXT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createSql)

        // Unique index for fast deduplication check
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_trx_id ON $TABLE_TRANSACTIONS($COL_TRX_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        onCreate(db)
    }

    @Synchronized
    fun isTrxDuplicate(trxId: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_TRANSACTIONS WHERE $COL_TRX_ID = ? LIMIT 1",
            arrayOf(trxId)
        )
        val exists = cursor.use { it.moveToFirst() }
        return exists
    }

    @Synchronized
    fun insertTransaction(record: TransactionRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TRX_ID, record.trxId)
            put(COL_TYPE, record.type)
            put(COL_AMOUNT, record.amount)
            put(COL_COUNTERPART, record.counterpart)
            put(COL_BALANCE, record.balance)
            put(COL_FEE, record.fee)
            put(COL_REFERENCE, record.reference)
            put(COL_RAW_SMS, record.rawSms)
            put(COL_FORWARDED_TEXT, record.forwardedText)
            put(COL_STATUS, record.status.name)
            put(COL_RETRY_COUNT, record.retryCount)
            put(COL_ERROR_MESSAGE, record.errorMessage)
            put(COL_TIMESTAMP, record.timestamp)
            put(COL_UPDATED_AT, record.updatedAt)
        }
        return db.insertWithOnConflict(
            TABLE_TRANSACTIONS,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    @Synchronized
    fun updateStatus(
        trxId: String,
        status: ForwardStatus,
        errorMessage: String? = null,
        incrementRetry: Boolean = false
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, status.name)
            put(COL_UPDATED_AT, System.currentTimeMillis())
            if (errorMessage != null) {
                put(COL_ERROR_MESSAGE, errorMessage)
            }
        }

        if (incrementRetry) {
            db.execSQL(
                "UPDATE $TABLE_TRANSACTIONS SET $COL_STATUS = ?, $COL_ERROR_MESSAGE = ?, $COL_RETRY_COUNT = $COL_RETRY_COUNT + 1, $COL_UPDATED_AT = ? WHERE $COL_TRX_ID = ?",
                arrayOf<Any>(status.name, errorMessage ?: "", System.currentTimeMillis(), trxId)
            )
        } else {
            db.update(
                TABLE_TRANSACTIONS,
                values,
                "$COL_TRX_ID = ?",
                arrayOf(trxId)
            )
        }
    }

    @Synchronized
    fun getRecordByTrxId(trxId: String): TransactionRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TRX_ID = ?",
            arrayOf(trxId),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) mapCursorToRecord(it) else null
        }
    }

    @Synchronized
    fun getAllRecords(limit: Int = 100): List<TransactionRecord> {
        val list = mutableListOf<TransactionRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(mapCursorToRecord(it))
            }
        }
        return list
    }

    @Synchronized
    fun getPendingAndFailedRecords(): List<TransactionRecord> {
        val list = mutableListOf<TransactionRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_STATUS IN (?, ?)",
            arrayOf(ForwardStatus.PENDING.name, ForwardStatus.FAILED.name),
            null,
            null,
            "$COL_TIMESTAMP ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(mapCursorToRecord(it))
            }
        }
        return list
    }

    @Synchronized
    fun getCounts(): Triple<Int, Int, Int> {
        val db = readableDatabase
        var total = 0
        var sent = 0
        var failedOrPending = 0

        val totalCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_TRANSACTIONS", null)
        totalCursor.use { if (it.moveToFirst()) total = it.getInt(0) }

        val sentCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TRANSACTIONS WHERE $COL_STATUS = ?",
            arrayOf(ForwardStatus.SENT.name)
        )
        sentCursor.use { if (it.moveToFirst()) sent = it.getInt(0) }

        val failCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TRANSACTIONS WHERE $COL_STATUS IN (?, ?)",
            arrayOf(ForwardStatus.FAILED.name, ForwardStatus.PENDING.name)
        )
        failCursor.use { if (it.moveToFirst()) failedOrPending = it.getInt(0) }

        return Triple(total, sent, failedOrPending)
    }

    @Synchronized
    fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_TRANSACTIONS, null, null)
    }

    private fun mapCursorToRecord(cursor: Cursor): TransactionRecord {
        return TransactionRecord(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            trxId = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRX_ID)),
            type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)),
            amount = cursor.getString(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
            counterpart = cursor.getString(cursor.getColumnIndexOrThrow(COL_COUNTERPART)) ?: "",
            balance = cursor.getString(cursor.getColumnIndexOrThrow(COL_BALANCE)) ?: "",
            fee = cursor.getString(cursor.getColumnIndexOrThrow(COL_FEE)) ?: "",
            reference = cursor.getString(cursor.getColumnIndexOrThrow(COL_REFERENCE)) ?: "",
            rawSms = cursor.getString(cursor.getColumnIndexOrThrow(COL_RAW_SMS)),
            forwardedText = cursor.getString(cursor.getColumnIndexOrThrow(COL_FORWARDED_TEXT)),
            status = try {
                ForwardStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)))
            } catch (e: Exception) {
                ForwardStatus.FAILED
            },
            retryCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RETRY_COUNT)),
            errorMessage = cursor.getString(cursor.getColumnIndexOrThrow(COL_ERROR_MESSAGE)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
        )
    }
}
