package com.fueltracker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProvinceDto(
    @SerializedName("IDProvincia") val id: String,
    @SerializedName("IDCCAA") val autonomousCommunityId: String,
    @SerializedName("Provincia") val name: String,
    @SerializedName("CCAA") val autonomousCommunity: String
)
