package com.fueltracker.app.domain.usecase

import com.fueltracker.app.domain.model.DayOfMonthPattern
import com.fueltracker.app.domain.model.DayOfWeekPattern
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.PriceAnalysis
import com.fueltracker.app.domain.model.PriceTrend
import com.fueltracker.app.domain.model.WeekOfMonthPattern
import com.fueltracker.app.domain.repository.PriceRepository
import com.fueltracker.app.domain.usecase.DailyPriceAggregator.DailyPrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AnalyzePatternsUseCase @Inject constructor(
    private val priceRepository: PriceRepository
) {
    private val dayNames = mapOf(
        1 to "Lunes",
        2 to "Martes",
        3 to "Miércoles",
        4 to "Jueves",
        5 to "Viernes",
        6 to "Sábado",
        7 to "Domingo"
    )

    private val weekLabels = mapOf(
        1 to "1ª semana",
        2 to "2ª semana",
        3 to "3ª semana",
        4 to "4ª semana",
        5 to "5ª semana"
    )

    /** Media de todas las gasolineras seguidas (pantalla Análisis). */
    operator fun invoke(fuelType: FuelType): Flow<PriceAnalysis> {
        return priceRepository.getAllPriceHistory(fuelType).map { records ->
            analyze(DailyPriceAggregator.toDailyMarketPrices(records), fuelType)
        }
    }

    /** Historial de una sola gasolinera (recomendaciones en Inicio). */
    fun forStation(stationId: String, fuelType: FuelType): Flow<PriceAnalysis> {
        return priceRepository.getPriceHistory(stationId, fuelType).map { records ->
            analyze(DailyPriceAggregator.toDailyStationPrices(records), fuelType)
        }
    }

    private fun analyze(dailyPrices: List<DailyPrice>, fuelType: FuelType): PriceAnalysis {
        if (dailyPrices.isEmpty()) {
            return PriceAnalysis(
                fuelType = fuelType,
                dayOfWeekPatterns = emptyList(),
                dayOfMonthPatterns = emptyList(),
                weekOfMonthPatterns = emptyList(),
                bestDayOfWeek = null,
                worstDayOfWeek = null,
                bestDayOfMonth = null,
                overallMin = 0.0,
                overallMax = 0.0,
                overallAvg = 0.0,
                trend = PriceTrend.STABLE
            )
        }

        val dayOfWeekPatterns = (1..7).mapNotNull { day ->
            val dayPrices = dailyPrices.filter { it.date.dayOfWeek.value == day }
            if (dayPrices.isEmpty()) return@mapNotNull null
            DayOfWeekPattern(
                dayOfWeek = day,
                dayName = dayNames[day] ?: "Día $day",
                averagePrice = dayPrices.map { it.price }.average(),
                sampleCount = dayPrices.size
            )
        }

        val dayOfMonthPatterns = (1..31).mapNotNull { day ->
            val dayPrices = dailyPrices.filter { it.date.dayOfMonth == day }
            if (dayPrices.isEmpty()) return@mapNotNull null
            DayOfMonthPattern(
                dayOfMonth = day,
                averagePrice = dayPrices.map { it.price }.average(),
                sampleCount = dayPrices.size
            )
        }

        val weekOfMonthPatterns = (1..5).mapNotNull { week ->
            val weekPrices = dailyPrices.filter { getWeekOfMonth(it.date.dayOfMonth) == week }
            if (weekPrices.isEmpty()) return@mapNotNull null
            WeekOfMonthPattern(
                weekOfMonth = week,
                weekLabel = weekLabels[week] ?: "Semana $week",
                averagePrice = weekPrices.map { it.price }.average(),
                sampleCount = weekPrices.size
            )
        }

        val prices = dailyPrices.map { it.price }
        val trend = calculateTrend(dailyPrices)

        return PriceAnalysis(
            fuelType = fuelType,
            dayOfWeekPatterns = dayOfWeekPatterns,
            dayOfMonthPatterns = dayOfMonthPatterns,
            weekOfMonthPatterns = weekOfMonthPatterns,
            bestDayOfWeek = dayOfWeekPatterns.minByOrNull { it.averagePrice },
            worstDayOfWeek = dayOfWeekPatterns.maxByOrNull { it.averagePrice },
            bestDayOfMonth = dayOfMonthPatterns.minByOrNull { it.averagePrice },
            overallMin = prices.min(),
            overallMax = prices.max(),
            overallAvg = prices.average(),
            trend = trend
        )
    }

    private fun getWeekOfMonth(dayOfMonth: Int): Int = ((dayOfMonth - 1) / 7) + 1

    private fun calculateTrend(dailyPrices: List<DailyPrice>): PriceTrend {
        if (dailyPrices.size < 4) return PriceTrend.STABLE
        val sorted = dailyPrices.sortedBy { it.date }
        val recentDays = sorted.takeLast(minOf(7, sorted.size / 2).coerceAtLeast(1))
        val olderDays = sorted.dropLast(recentDays.size).takeLast(recentDays.size)
        if (olderDays.isEmpty()) return PriceTrend.STABLE

        val recentAvg = recentDays.map { it.price }.average()
        val olderAvg = olderDays.map { it.price }.average()
        val change = recentAvg - olderAvg
        return when {
            change > 0.02 -> PriceTrend.RISING
            change < -0.02 -> PriceTrend.FALLING
            else -> PriceTrend.STABLE
        }
    }
}
