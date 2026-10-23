package com.saif.pymaster.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Learning
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Runs a few times a day; posts the "new daily quiz" notification once per day after 8 AM. */
class DailyQuizWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < 8) return Result.success()
        Content.init(applicationContext)
        Learning(applicationContext).announceDailyQuiz()
        return Result.success()
    }

    companion object {
        private const val NAME = "daily_quiz"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyQuizWorker>(4, TimeUnit.HOURS)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
