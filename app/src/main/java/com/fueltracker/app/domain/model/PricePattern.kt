package com.fueltracker.app.domain.model

data class DayOfWeekPattern(
    val dayOfWeek: Int,
    val dayName: String,
    val averagePrice: Double,
    val sampleCount: Int
)

data class DayOfMonthPattern(
    val dayOfMonth: Int,
    val averagePrice: Double,
    val sampleCount: Int
)

data class WeekOfMonthPattern(
    val weekOfMonth: Int,
    val weekLabel: String,
    val averagePrice: Double,
    val sampleCount: Int
)

data class PriceAnalysis(
    val fuelType: FuelType,
    val dayOfWeekPatterns: List<DayOfWeekPattern>,
    val dayOfMonthPatterns: List<DayOfMonthPattern>,
    val weekOfMonthPatterns: List<WeekOfMonthPattern>,
    val bestDayOfWeek: DayOfWeekPattern?,
    val worstDayOfWeek: DayOfWeekPattern?,
    val bestDayOfMonth: DayOfMonthPattern?,
    val overallMin: Double,
    val overallMax: Double,
    val overallAvg: Double,
    val trend: PriceTrend
)

enum class PriceTrend {
    RISING, FALLING, STABLE
}

data class Recommendation(
    val fuelType: FuelType,
    val bestDayLabel: String,
    val expectedSaving: Double,
    val confidence: Float,
    val detail: String
)

data class StationRecommendation(
    val stationId: String,
    val stationName: String,
    val fuelType: FuelType,
    val bestDayLabel: String,
    val expectedSaving: Double,
    val confidence: Float,
    val detail: String,
    val trend: PriceTrend
)

data class FuelTypeConfig(
    val type: FuelType,
    val displayName: String,
    val isEnabled: Boolean,
    val isAvailableFromApi: Boolean
)
