package com.fueltracker.app.domain.usecase

import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.PriceAnalysis
import com.fueltracker.app.domain.model.PriceTrend
import com.fueltracker.app.domain.model.StationRecommendation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetStationRecommendationsUseCase @Inject constructor(
    private val analyzePatternsUseCase: AnalyzePatternsUseCase
) {
    operator fun invoke(
        fuelType: FuelType,
        stations: List<GasStation>
    ): Flow<List<StationRecommendation>> {
        if (stations.isEmpty()) return flowOf(emptyList())

        val stationFlows = stations.map { station ->
            analyzePatternsUseCase.forStation(station.id, fuelType).map { analysis ->
                buildRecommendation(station, analysis)
            }
        }
        return combine(stationFlows) { recommendations ->
            recommendations.filterNotNull()
        }
    }

    private fun buildRecommendation(
        station: GasStation,
        analysis: PriceAnalysis
    ): StationRecommendation? {
        val best = analysis.bestDayOfWeek ?: return null
        val worst = analysis.worstDayOfWeek
        val saving = if (worst != null) worst.averagePrice - best.averagePrice else 0.0
        val confidence = minOf(best.sampleCount / 4f, 1f)
        val trendText = when (analysis.trend) {
            PriceTrend.RISING -> " Los precios están subiendo."
            PriceTrend.FALLING -> " Los precios están bajando."
            PriceTrend.STABLE -> " Los precios están estables."
        }
        val detail = buildString {
            append("En ${station.name}, el ${best.dayName} suele ser más barato ")
            append("(${String.format("%.3f", best.averagePrice)} €/L).")
            if (worst != null) {
                append(" Evita el ${worst.dayName} (${String.format("%.3f", worst.averagePrice)} €/L).")
            }
            append(trendText)
        }
        return StationRecommendation(
            stationId = station.id,
            stationName = station.name,
            fuelType = analysis.fuelType,
            bestDayLabel = best.dayName,
            expectedSaving = saving,
            confidence = confidence,
            detail = detail,
            trend = analysis.trend
        )
    }
}
