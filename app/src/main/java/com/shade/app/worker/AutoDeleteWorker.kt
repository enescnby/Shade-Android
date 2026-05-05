package com.shade.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.shade.app.data.local.dao.MessageDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoDeleteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageDao: MessageDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val chatId  = inputData.getString(KEY_CHAT_ID)  ?: return Result.failure()
        val minutes = inputData.getInt(KEY_MINUTES, 0)
        if (minutes <= 0) return Result.success()

        val cutoff = System.currentTimeMillis() - minutes * 60_000L
        val deleted = messageDao.deleteMessagesOlderThan(chatId, cutoff)
        Log.d(TAG, "Auto-deleted $deleted messages older than $minutes min in chat $chatId")
        return Result.success()
    }

    companion object {
        private const val TAG          = "AutoDeleteWorker"
        private const val KEY_CHAT_ID  = "chatId"
        private const val KEY_MINUTES  = "minutes"

        fun schedule(context: Context, chatId: String, minutes: Int) {
            if (minutes <= 0) {
                WorkManager.getInstance(context).cancelUniqueWork("ad_$chatId")
                return
            }
            val data = workDataOf(KEY_CHAT_ID to chatId, KEY_MINUTES to minutes)
            // Repeat at the same interval (min 15 min, per WorkManager constraints)
            val intervalMin = maxOf(minutes.toLong(), 15L)
            val request = PeriodicWorkRequestBuilder<AutoDeleteWorker>(
                repeatInterval = intervalMin,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "ad_$chatId",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context, chatId: String) {
            WorkManager.getInstance(context).cancelUniqueWork("ad_$chatId")
        }
    }
}
