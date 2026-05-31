package com.fueltracker.app.domain.usecase

import com.fueltracker.app.domain.model.DayOfMonthPattern
import com.fueltracker.app.domain.model.DayOfWeekPattern
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.PriceAnalysis
import com.fueltracker.app.domain.model.PriceRecord
import com.fueltracker.app.domain.model.PriceTrend
import com.fueltracker.app.domain.model.WeekOfMonthPattern
import com.fueltracker.app.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
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

    operator fun invoke(fuelType: FuelType): Flow<PriceAnalysis> {
        return priceRepository.getAllPriceHistory(fuelType).map { records ->
            analyze(records, fuelType)
        }
    }

    private fun analyze(records: List<PriceRecord>, fuelType: FuelType): PriceAnalysis {
        if (records.isEmpty()) {
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
            val dayRecords = records.filter { it.recordedAt.dayOfWeek.value == day }
            if (dayRecords.isEmpty()) return@mapNotNull null
            DayOfWeekPattern(
                dayOfWeek = day,
                dayName = dayNames[day] ?: "Día $day",
                averagePrice = dayRecords.map { it.price }.average(),
                sampleCount = dayRecords.size
            )
        }

        val dayOfMonthPatterns = (1..31).mapNotNull { day ->
            val dayRecords = records.filter { it.recordedAt.dayOfMonth == day }
            if (dayRecords.isEmpty()) return@mapNotNull null
            DayOfMonthPattern(
                dayOfMonth = day,
                averagePrice = dayRecords.map { it.price }.average(),
                sampleCount = dayRecords.size
            )
        }

        val weekOfMonthPatterns = (1..5).mapNotNull { week ->
            val weekRecords = records.filter { getWeekOfMonth(it.recordedAt.dayOfMonth) == week }
            if (weekRecords.isEmpty()) return@mapNotNull null
            WeekOfMonthPattern(
                weekOfMonth = week,
                weekLabel = weekLabels[week] ?: "Semana $week",
                averagePrice = weekRecords.map { it.price }.average(),
                sampleCount = weekRecords.size
            )
        }

        val prices = records.map { it.price }
        val trend = calculateTrend(records)

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

    private fun calculateTrend(records: List<PriceRecord>): PriceTrend {
        if (records.size < 4) return PriceTrend.STABLE
        val sorted = records.sortedBy { it.recordedAt }
        val recentHalf = sorted.takeLast(sorted.size / 2).map { it.price }.average()
        val olderHalf = sorted.take(sorted.size / 2).map { it.price }.average()
        val change = recentHalf - olderHalf
        return when {
            change > 0.02 -> PriceTrend.RISING
            change < -0.02 -> PriceTrend.FALLING
            else -> PriceTrend.STABLE
        }
    }
}
