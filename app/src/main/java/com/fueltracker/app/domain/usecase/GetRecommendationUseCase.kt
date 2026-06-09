package com.fueltracker.app.domain.usecase

import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.Recommendation
import javax.inject.Inject

class GetRecommendationUseCase @Inject constructor(
    private val analyzePatternsUseCase: AnalyzePatternsUseCase
) {
    suspend operator fun invoke(fuelType: FuelType, onResult: (Recommendation?) -> Unit) {
        analyzePatternsUseCase(fuelType).collect { analysis ->
            val best = analysis.bestDayOfWeek ?: run {
                onResult(null)
                return@collect
            }
            val worst = analysis.worstDayOfWeek
            val saving = if (worst != null) worst.averagePrice - best.averagePrice else 0.0
            val confidence = minOf(best.sampleCount / 4f, 1f)
            val trend = when (analysis.trend) {
                com.fueltracker.app.domain.model.PriceTrend.RISING -> " Los precios están subiendo."
                com.fueltracker.app.domain.model.PriceTrend.FALLING -> " Los precios están bajando."
                com.fueltracker.app.domain.model.PriceTrend.STABLE -> " Los precios están estables."
            }
            val detail = buildString {
                append("Históricamente, el ${best.dayName} tiene el precio más bajo (${String.format("%.3f", best.averagePrice)} €/L).")
                if (worst != null) {
                    append(" Evita el ${worst.dayName} (${String.format("%.3f", worst.averagePrice)} €/L).")
                }
                append(trend)
            }
            onResult(
                Recommendation(
                    fuelType = fuelType,
                    bestDayLabel = best.dayName,
                    expectedSaving = saving,
                    confidence = confidence,
                    detail = detail
                )
            )
        }
    }
}
