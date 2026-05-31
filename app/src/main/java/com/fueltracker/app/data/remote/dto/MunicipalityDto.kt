package com.fueltracker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MunicipalityDto(
    @SerializedName("IDMunicipio") val id: String,
    @SerializedName("IDProvincia") val provinceId: String,
    @SerializedName("IDCCAA") val autonomousCommunityId: String,
    @SerializedName("Municipio") val name: String,
    @SerializedName("Provincia") val province: String,
    @SerializedName("CCAA") val autonomousCommunity: String
)
