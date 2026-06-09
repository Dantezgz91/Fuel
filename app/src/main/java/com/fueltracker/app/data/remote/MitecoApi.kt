package com.fueltracker.app.data.remote

import com.fueltracker.app.data.remote.dto.MunicipalityDto
import com.fueltracker.app.data.remote.dto.ProvinceDto
import com.fueltracker.app.data.remote.dto.StationsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface MitecoApi {
    @GET("Listados/Municipios/")
    suspend fun getMunicipalities(): List<MunicipalityDto>

    @GET("Listados/Provincias/")
    suspend fun getProvinces(): List<ProvinceDto>

    @GET("EstacionesTerrestres/FiltroMunicipio/{municipalityId}")
    suspend fun getStationsByMunicipality(
        @Path("municipalityId") municipalityId: String
    ): StationsResponseDto

    @GET("EstacionesTerrestres/FiltroProvincia/{provinceId}")
    suspend fun getStationsByProvince(
        @Path("provinceId") provinceId: String
    ): StationsResponseDto
}
