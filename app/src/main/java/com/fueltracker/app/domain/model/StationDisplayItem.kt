package com.fueltracker.app.domain.model

data class StationDisplayItem(
    val station: GasStation,
    val distanceKm: Double?,
    val openStatus: ScheduleOpenStatus
)
