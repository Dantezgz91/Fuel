package com.fueltracker.app.domain.repository

import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.Municipality
import kotlinx.coroutines.flow.Flow

interface GasStationRepository {
    fun getTrackedStations(): Flow<List<GasStation>>
    suspend fun searchStationsByMunicipality(municipalityId: String): Result<List<GasStation>>
    suspend fun trackStation(stationId: String)
    suspend fun untrackStation(stationId: String)
    suspend fun getMunicipalities(): Result<List<Municipality>>
    suspend fun refreshPricesForTrackedStations()
}
