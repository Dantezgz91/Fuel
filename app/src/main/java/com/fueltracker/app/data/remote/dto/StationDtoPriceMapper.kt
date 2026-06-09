package com.fueltracker.app.data.remote.dto

import com.fueltracker.app.domain.model.FuelType

internal fun FuelType.extractPrice(dto: StationDto): String? = when (this) {
    FuelType.GASOLINA_95 -> dto.priceGasolina95
    FuelType.GASOLINA_95_E10 -> dto.priceGasolina95E10
    FuelType.GASOLINA_95_PREMIUM -> dto.priceGasolina95Premium
    FuelType.GASOLINA_98 -> dto.priceGasolina98
    FuelType.GASOLINA_98_E10 -> dto.priceGasolina98E10
    FuelType.GASOIL_A -> dto.priceGasoilA
    FuelType.GASOIL_B -> dto.priceGasoilB
    FuelType.GASOIL_PREMIUM -> dto.priceGasoilPremium
    FuelType.GLP -> dto.priceGlp
    FuelType.GNC -> dto.priceGnc
    FuelType.GNL -> dto.priceGnl
    FuelType.BIODIESEL -> dto.priceBiodiesel
    FuelType.BIOETANOL -> dto.priceBioetanol
    FuelType.HIDROGENO -> dto.priceHidrogeno
    FuelType.ADBLUE -> dto.priceAdblue
    FuelType.AMONIACO -> dto.priceAmoniaco
    FuelType.BIOGNC -> dto.priceBiognc
    FuelType.BIOGNL -> dto.priceBiognl
    FuelType.DIESEL_RENOVABLE -> dto.priceDieselRenovable
}
