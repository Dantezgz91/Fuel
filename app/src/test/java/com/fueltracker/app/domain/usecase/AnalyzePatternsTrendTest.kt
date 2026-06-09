package com.fueltracker.app.domain.usecase

import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.PriceRecord
import com.fueltracker.app.domain.model.PriceTrend
import com.fueltracker.app.domain.repository.PriceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AnalyzePatternsTrendTest {

    @Test
    fun `rising trend when recent daily averages are higher`() = runBlocking {
        val records = buildDailySeries(
            start = LocalDate.of(2025, 1, 1),
            days = 14,
            startPrice = 1.40,
            step = 0.03
        )
        val analysis = useCase(FuelType.GASOLINA_95, records).first()
        assertEquals(PriceTrend.RISING, analysis.trend)
    }

    @Test
    fun `falling trend when recent daily averages are lower`() = runBlocking {
        val records = buildDailySeries(
            start = LocalDate.of(2025, 1, 1),
            days = 14,
            startPrice = 1.80,
            step = -0.03
        )
        val analysis = useCase(FuelType.GASOLINA_95, records).first()
        assertEquals(PriceTrend.FALLING, analysis.trend)
    }

    @Test
    fun `day of week sample count counts calendar days not raw records`() = runBlocking {
        val monday = LocalDate.of(2025, 6, 2) // Monday
        val records = listOf(
            record("s1", monday, 8, 1.50),
            record("s1", monday, 20, 1.52),
            record("s2", monday, 12, 1.48)
        )
        val analysis = useCase(FuelType.GASOLINA_95, records).first()
        val mondayPattern = analysis.dayOfWeekPatterns.first { it.dayOfWeek == 1 }
        assertEquals(1, mondayPattern.sampleCount)
    }

    private fun useCase(fuelType: FuelType, records: List<PriceRecord>) =
        AnalyzePatternsUseCase(FakePriceRepository(records)).invoke(fuelType)

    private fun buildDailySeries(
        start: LocalDate,
        days: Int,
        startPrice: Double,
        step: Double
    ): List<PriceRecord> {
        return (0 until days).flatMap { offset ->
            val date = start.plusDays(offset.toLong())
            val price = startPrice + offset * step
            listOf(record("s1", date, 12, price))
        }
    }

    private fun record(
        stationId: String,
        date: LocalDate,
        hour: Int,
        price: Double
    ) = PriceRecord(
        id = 0,
        stationId = stationId,
        fuelType = FuelType.GASOLINA_95,
        price = price,
        recordedAt = LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, 0)
    )

    private class FakePriceRepository(
        private val records: List<PriceRecord>
    ) : PriceRepository {
        override fun getAllPriceHistory(fuelType: FuelType) = flowOf(records)
        override fun getPriceHistory(stationId: String, fuelType: FuelType) =
            flowOf(records.filter { it.stationId == stationId })
        override suspend fun savePriceRecords(records: List<PriceRecord>) = Unit
        override suspend fun getLatestPrices(stationId: String): Map<FuelType, Double> = emptyMap()
        override suspend fun deleteOldRecords(before: LocalDateTime) = 0
        override suspend fun deleteRecordsOutsideRetention(retentionDays: Int) = 0
        override suspend fun deleteAllPriceRecords() = 0
        override fun observeAvailableFuelTypes() = kotlinx.coroutines.flow.flowOf(emptySet())
    }
}
