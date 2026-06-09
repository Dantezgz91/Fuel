package com.fueltracker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StationsResponseDto(
    // Algunos endpoints no incluyen todos los campos siempre (p.ej. ResultadoConsulta).
    @SerializedName("Fecha") val date: String? = null,
    @SerializedName("ListaEESSPrecio") val stations: List<StationDto> = emptyList(),
    @SerializedName("ResultadoConsulta") val result: String? = null
)

data class StationDto(
    @SerializedName("IDEESS") val id: String? = null,
    @SerializedName("Rótulo") val name: String? = null,
    @SerializedName("Dirección") val address: String? = null,
    @SerializedName("Localidad") val locality: String? = null,
    @SerializedName("Municipio") val municipality: String? = null,
    @SerializedName("Provincia") val province: String? = null,
    @SerializedName("Latitud") val latitude: String? = null,
    @SerializedName("Longitud (WGS84)") val longitude: String? = null,
    @SerializedName("Precio Gasolina 95 E5") val priceGasolina95: String? = null,
    @SerializedName("Precio Gasolina 95 E10") val priceGasolina95E10: String? = null,
    @SerializedName("Precio Gasolina 95 E5 Premium") val priceGasolina95Premium: String? = null,
    @SerializedName("Precio Gasolina 98 E5") val priceGasolina98: String? = null,
    @SerializedName("Precio Gasolina 98 E10") val priceGasolina98E10: String? = null,
    @SerializedName("Precio Gasoleo A") val priceGasoilA: String? = null,
    @SerializedName("Precio Gasoleo B") val priceGasoilB: String? = null,
    @SerializedName("Precio Gasoleo Premium") val priceGasoilPremium: String? = null,
    @SerializedName("Precio Gases licuados del petróleo") val priceGlp: String? = null,
    @SerializedName("Precio Gas Natural Comprimido") val priceGnc: String? = null,
    @SerializedName("Precio Gas Natural Licuado") val priceGnl: String? = null,
    @SerializedName("Precio Biodiesel") val priceBiodiesel: String? = null,
    @SerializedName("Precio Bioetanol") val priceBioetanol: String? = null,
    @SerializedName("Precio Hidrogeno") val priceHidrogeno: String? = null,
    @SerializedName("Precio Adblue") val priceAdblue: String? = null,
    @SerializedName("Precio Amoniaco") val priceAmoniaco: String? = null,
    @SerializedName("Precio Biogas Natural Comprimido") val priceBiognc: String? = null,
    @SerializedName("Precio Biogas Natural Licuado") val priceBiognl: String? = null,
    @SerializedName("Precio Diésel Renovable") val priceDieselRenovable: String? = null,
    @SerializedName("IDMunicipio") val municipalityId: String? = null,
    @SerializedName("Horario") val schedule: String? = null
)
