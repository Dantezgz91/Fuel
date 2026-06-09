package com.fueltracker.app.domain.util

import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.StationSortMode

object StationOrder {

    fun mergeCustomOrder(savedOrder: List<String>, stationIds: List<String>): List<String> {
        val existing = savedOrder.filter { it in stationIds }
        val newIds = stationIds.filter { it !in existing }
        return existing + newIds
    }

    fun sort(
        stations: List<GasStation>,
        sortMode: StationSortMode,
        customOrder: List<String>,
        fuelType: FuelType
    ): List<GasStation> {
        return when (sortMode) {
            StationSortMode.BY_PRICE -> stations.sortedWith(
                compareBy<GasStation> { it.currentPrices[fuelType] ?: Double.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            )
            StationSortMode.CUSTOM -> {
                val order = mergeCustomOrder(customOrder, stations.map { it.id })
                stations.sortedBy { station ->
                    val index = order.indexOf(station.id)
                    if (index < 0) Int.MAX_VALUE else index
                }
            }
        }
    }

    fun moveInOrder(order: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        if (fromIndex !in order.indices || toIndex !in order.indices || fromIndex == toIndex) {
            return order
        }
        val mutable = order.toMutableList()
        val id = mutable.removeAt(fromIndex)
        mutable.add(toIndex, id)
        return mutable
    }
}
