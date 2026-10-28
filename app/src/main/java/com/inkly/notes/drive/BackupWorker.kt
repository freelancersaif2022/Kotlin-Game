package com.inkly.notes.drive

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.inkly.notes.appContainer
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Daily background backup to Google Drive. Runs even when the app is closed (WorkManager). */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val prefs = container.prefs
        val state = prefs.state.value
        if (!state.driveConnected || !state.autoBackup) return Result.success()

        return try {
            val token = DriveAuth.silentToken(applicationContext)
            if (token == null) {
                prefs.setNeedsReauth(true)
                prefs.recordBackupError("Google access expired – reconnect to resume backups")
                return Result.failure()
            }
            container.backup.backup(token)
            Result.success()
        } catch (e: DriveException) {
            prefs.recordBackupError(e.message ?: "Backup failed")
            if (e.code == 401 || e.code == 403) {
                prefs.setNeedsReauth(e.code == 401)
                Result.failure()
            } else retryOrFail()
        } catch (e: IOException) {
            prefs.recordBackupError("Backup postponed – no connection")
            retryOrFail()
        } catch (e: Exception) {
            prefs.recordBackupError(e.message ?: "Backup failed")
            retryOrFail()
        }
    }

    private fun retryOrFail() = if (runAttemptCount < 4) Result.retry() else Result.failure()
}

object BackupScheduler {
    private const val WORK_NAME = "inkly_daily_backup"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setInitialDelay(6, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
