package com.fueltracker.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fueltracker.app.domain.repository.GasStationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PriceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val gasStationRepository: GasStationRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            gasStationRepository.refreshPricesForTrackedStations()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "price_sync_worker"
    }
}
