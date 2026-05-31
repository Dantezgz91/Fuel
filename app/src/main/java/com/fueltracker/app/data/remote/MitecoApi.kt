package com.fueltracker.app.data.remote

import com.fueltracker.app.data.remote.dto.MunicipalityDto
import com.fueltracker.app.data.remote.dto.StationsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface MitecoApi {
    @GET("municipios")
    suspend fun getMunicipalities(): List<MunicipalityDto>

    @GET("EstacionesTerminoMunicipal/{municipalityId}")
    suspend fun getStationsByMunicipality(
        @Path("municipalityId") municipalityId: String
    ): StationsResponseDto
}
