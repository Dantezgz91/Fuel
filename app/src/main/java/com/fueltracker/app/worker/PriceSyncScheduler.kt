package com.fueltracker.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fueltracker.app.data.local.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    suspend fun initialize() {
        if (userPreferences.getAutoSyncEnabled()) {
            schedulePeriodicSync()
        }
        enqueueSyncIfStale()
    }

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<PriceSyncWorker>(
            SYNC_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(networkConstraints())
            .build()

        workManager.enqueueUniquePeriodicWork(
            PriceSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(PriceSyncWorker.WORK_NAME)
    }

    suspend fun enqueueSyncIfStale() {
        val lastSync = userPreferences.getLastSyncEpochMs()
        val elapsed = System.currentTimeMillis() - lastSync
        if (elapsed < STALE_AFTER_MS) return

        val request = OneTimeWorkRequestBuilder<PriceSyncWorker>()
            .setConstraints(networkConstraints())
            .build()

        workManager.enqueueUniqueWork(
            PriceSyncWorker.ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun networkConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    companion object {
        private const val SYNC_INTERVAL_HOURS = 24L
        private const val STALE_AFTER_MS = 20 * 60 * 60 * 1000L
    }
}
