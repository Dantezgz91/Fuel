package com.fueltracker.app.domain.model

enum class FuelType(val displayName: String, val apiKey: String) {
    GASOLINA_95("Gasolina 95 E5", "Precio Gasolina 95 E5"),
    GASOLINA_95_E10("Gasolina 95 E10", "Precio Gasolina 95 E10"),
    GASOLINA_95_PREMIUM("Gasolina 95 E5 Premium", "Precio Gasolina 95 E5 Premium"),
    GASOLINA_98("Gasolina 98 E5", "Precio Gasolina 98 E5"),
    GASOLINA_98_E10("Gasolina 98 E10", "Precio Gasolina 98 E10"),
    GASOIL_A("Gasóleo A", "Precio Gasoleo A"),
    GASOIL_B("Gasóleo B", "Precio Gasoleo B"),
    GASOIL_PREMIUM("Gasóleo Premium", "Precio Gasoleo Premium"),
    GLP("GLP", "Precio Gases licuados del petróleo"),
    GNC("Gas natural comprimido", "Precio Gas Natural Comprimido"),
    GNL("Gas natural licuado", "Precio Gas Natural Licuado"),
    BIODIESEL("Biodiésel", "Precio Biodiesel"),
    BIOETANOL("Bioetanol", "Precio Bioetanol"),
    HIDROGENO("Hidrógeno", "Precio Hidrogeno"),
    ADBLUE("AdBlue", "Precio Adblue"),
    AMONIACO("Amoniaco", "Precio Amoniaco"),
    BIOGNC("Biogas natural comprimido", "Precio Biogas Natural Comprimido"),
    BIOGNL("Biogas natural licuado", "Precio Biogas Natural Licuado"),
    DIESEL_RENOVABLE("Diésel renovable", "Precio Diésel Renovable")
}
