package com.fueltracker.app.domain.usecase

import com.fueltracker.app.domain.model.PriceRecord
import java.time.LocalDate

/**
 * Los precios oficiales del MITECO se publican una vez al día.
 * El análisis agrupa por día natural: un precio por estación y combustible,
 * luego la media entre estaciones seguidas para cada fecha.
 */
internal object DailyPriceAggregator {

    data class DailyPrice(val date: LocalDate, val price: Double)

    fun toDailyMarketPrices(records: List<PriceRecord>): List<DailyPrice> {
        val perStationPerDay = records
            .groupBy { it.stationId to it.recordedAt.toLocalDate() }
            .mapNotNull { (_, dayRecords) ->
                dayRecords.maxByOrNull { it.recordedAt }
            }

        return perStationPerDay
            .groupBy { it.recordedAt.toLocalDate() }
            .map { (date, stationRecords) ->
                DailyPrice(date, stationRecords.map { it.price }.average())
            }
            .sortedBy { it.date }
    }

    /** Un precio por día para una sola estación (recomendaciones por gasolinera). */
    fun toDailyStationPrices(records: List<PriceRecord>): List<DailyPrice> {
        return records
            .groupBy { it.recordedAt.toLocalDate() }
            .mapNotNull { (_, dayRecords) ->
                dayRecords.maxByOrNull { it.recordedAt }?.let { latest ->
                    DailyPrice(latest.recordedAt.toLocalDate(), latest.price)
                }
            }
            .sortedBy { it.date }
    }
}
