package com.fueltracker.app.domain.model

enum class FuelType(val displayName: String, val apiKey: String) {
    GASOLINA_95("Gasolina 95 E5", "Precio Gasolina 95 E5"),
    GASOLINA_98("Gasolina 98 E5", "Precio Gasolina 98 E5"),
    GASOIL_A("Gasóleo A", "Precio Gasoil A"),
    GASOIL_PREMIUM("Gasóleo Premium", "Precio Gasoil Premium"),
    GLP("GLP", "Precio Gases licuados del petróleo")
}
