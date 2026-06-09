package com.fueltracker.app.domain.usecase

import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.PriceRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DailyPriceAggregatorTest {

    @Test
    fun `multiple syncs same day same station keep latest price`() {
        val day = LocalDate.of(2025, 6, 2)
        val records = listOf(
            record("s1", day, 10, 1.50),
            record("s1", day, 18, 1.55)
        )
        val daily = DailyPriceAggregator.toDailyMarketPrices(records)
        assertEquals(1, daily.size)
        assertEquals(1.55, daily[0].price, 0.001)
    }

    @Test
    fun `two stations same day average market price`() {
        val day = LocalDate.of(2025, 6, 2)
        val records = listOf(
            record("s1", day, 12, 1.40),
            record("s2", day, 12, 1.60)
        )
        val daily = DailyPriceAggregator.toDailyMarketPrices(records)
        assertEquals(1, daily.size)
        assertEquals(1.50, daily[0].price, 0.001)
    }

    @Test
    fun `different days produce separate daily points sorted`() {
        val d1 = LocalDate.of(2025, 6, 1)
        val d2 = LocalDate.of(2025, 6, 2)
        val records = listOf(
            record("s1", d2, 12, 1.60),
            record("s1", d1, 12, 1.40)
        )
        val daily = DailyPriceAggregator.toDailyMarketPrices(records)
        assertEquals(2, daily.size)
        assertEquals(d1, daily[0].date)
        assertEquals(d2, daily[1].date)
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
}
