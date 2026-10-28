package com.lexora.english.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Fires once a day at a random time between 9:00 and 20:00, unlocks the new daily quiz and
 * notifies the learner, then schedules itself for the next day.
 */
class DailyQuizWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = AppRepository.get(applicationContext)
        if (repo.current.profile != null) {
            repo.ensureDailyQuiz(postSystemNotification = true)
            repo.flush()
        }
        schedule(applicationContext, fromWorker = true)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "lexora_daily_quiz"

        fun schedule(context: Context, fromWorker: Boolean = false) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(9, 0).plusMinutes(Random.nextLong(0, 11 * 60))
            if (fromWorker || !next.isAfter(now.plusMinutes(5))) {
                next = now.toLocalDate().plusDays(1).atTime(9, 0).plusMinutes(Random.nextLong(0, 11 * 60))
            }
            val delay = Duration.between(now, next).toMillis()
            val request = OneTimeWorkRequestBuilder<DailyQuizWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
