package com.bkash.forwarder.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bkash.forwarder.data.repository.ForwarderRepository

class ForwardRetryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ForwardRetryWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Network restored. Running ForwardRetryWorker...")
        return try {
            val repository = ForwarderRepository.getInstance(applicationContext)
            val successCount = repository.retryAllPendingAndFailed()
            Log.i(TAG, "Retry complete. Successfully forwarded $successCount transactions.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Retry worker failed: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
