package com.fueltracker.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fueltracker.app.domain.repository.PriceRepository
import com.fueltracker.app.worker.PriceSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsUiState(
    val syncIntervalHours: Int = 6,
    val autoSyncEnabled: Boolean = true,
    val dataRetentionDays: Int = 365,
    val isClearingData: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val priceRepository: PriceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setSyncInterval(hours: Int) {
        _uiState.value = _uiState.value.copy(syncIntervalHours = hours)
        if (_uiState.value.autoSyncEnabled) scheduleSyncWork(hours)
    }

    fun setAutoSync(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
        if (enabled) scheduleSyncWork(_uiState.value.syncIntervalHours)
        else WorkManager.getInstance(context).cancelUniqueWork(PriceSyncWorker.WORK_NAME)
    }

    fun setDataRetention(days: Int) {
        _uiState.value = _uiState.value.copy(dataRetentionDays = days)
    }

    fun clearOldData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearingData = true)
            val cutoff = LocalDateTime.now().minusDays(_uiState.value.dataRetentionDays.toLong())
            priceRepository.deleteOldRecords(cutoff)
            _uiState.value = _uiState.value.copy(
                isClearingData = false,
                message = "Datos eliminados correctamente"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun scheduleSyncWork(intervalHours: Int) {
        val request = PeriodicWorkRequestBuilder<PriceSyncWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PriceSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
