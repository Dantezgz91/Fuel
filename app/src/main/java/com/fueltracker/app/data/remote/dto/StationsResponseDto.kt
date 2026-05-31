package com.fueltracker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StationsResponseDto(
    @SerializedName("Fecha") val date: String,
    @SerializedName("ListaEESSPrecio") val stations: List<StationDto>,
    @SerializedName("ResultadoConsulta") val result: String
)

data class StationDto(
    @SerializedName("IDEESS") val id: String,
    @SerializedName("Rótulo") val name: String,
    @SerializedName("Dirección") val address: String,
    @SerializedName("Localidad") val locality: String,
    @SerializedName("Municipio") val municipality: String,
    @SerializedName("Provincia") val province: String,
    @SerializedName("Latitud") val latitude: String,
    @SerializedName("Longitud (WGS84)") val longitude: String,
    @SerializedName("Precio Gasolina 95 E5") val priceGasolina95: String,
    @SerializedName("Precio Gasolina 98 E5") val priceGasolina98: String,
    @SerializedName("Precio Gasoil A") val priceGasoilA: String,
    @SerializedName("Precio Gasoil Premium") val priceGasoilPremium: String,
    @SerializedName("Precio Gases licuados del petróleo") val priceGlp: String,
    @SerializedName("IDMunicipio") val municipalityId: String,
    @SerializedName("Horario") val schedule: String
)
