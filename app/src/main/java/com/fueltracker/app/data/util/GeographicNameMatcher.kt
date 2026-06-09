package com.fueltracker.app.data.util

import com.fueltracker.app.data.remote.dto.MunicipalityDto
import com.fueltracker.app.data.remote.dto.ProvinceDto
import com.fueltracker.app.domain.model.ReverseGeocodeResult
import java.text.Normalizer

object GeographicNameMatcher {

    data class ResolvedLocation(
        val provinceId: String,
        val municipalityId: String?
    )

    fun resolveLocation(
        geocode: ReverseGeocodeResult,
        provinces: List<ProvinceDto>,
        municipalities: List<MunicipalityDto>
    ): ResolvedLocation? {
        val placeNames = placeNameCandidates(geocode)

        for (name in placeNames) {
            val municipalityMatches = municipalities.filter { normalize(it.name) == name }
            pickMunicipality(municipalityMatches, geocode, provinces)?.let { municipality ->
                return ResolvedLocation(
                    provinceId = municipality.provinceId,
                    municipalityId = municipality.id
                )
            }
        }

        for (name in placeNames) {
            provinces.firstOrNull { normalize(it.name) == name }?.let { province ->
                return ResolvedLocation(provinceId = province.id, municipalityId = null)
            }
        }

        val ccaaNorm = geocode.autonomousCommunity?.let(::normalize)
        if (ccaaNorm != null) {
            val provincesInCcaa = provinces.filter { normalize(it.autonomousCommunity) == ccaaNorm }
            for (name in placeNames) {
                provincesInCcaa.firstOrNull { normalize(it.name) == name }?.let { province ->
                    return ResolvedLocation(provinceId = province.id, municipalityId = null)
                }
            }
        }

        return null
    }

    private fun placeNameCandidates(geocode: ReverseGeocodeResult): List<String> {
        return listOfNotNull(
            geocode.locality,
            geocode.province,
            geocode.featureName
        ).map(::normalize).distinct()
    }

    private fun pickMunicipality(
        matches: List<MunicipalityDto>,
        geocode: ReverseGeocodeResult,
        provinces: List<ProvinceDto>
    ): MunicipalityDto? {
        if (matches.isEmpty()) return null
        if (matches.size == 1) return matches.first()

        val provinceHints = listOfNotNull(
            geocode.province,
            geocode.autonomousCommunity
        ).map(::normalize)

        for (hint in provinceHints) {
            val byProvinceName = matches.filter { normalize(it.province) == hint }
            if (byProvinceName.size == 1) return byProvinceName.first()
            if (byProvinceName.isNotEmpty()) return byProvinceName.first()

            val byCcaa = matches.filter { normalize(it.autonomousCommunity) == hint }
            if (byCcaa.size == 1) return byCcaa.first()
            if (byCcaa.isNotEmpty()) return byCcaa.first()
        }

        val provinceIds = matches.map { it.provinceId }.distinct()
        if (provinceIds.size == 1) {
            val provinceName = provinces.firstOrNull { it.id == provinceIds.first() }?.name
            if (provinceName != null && normalize(provinceName) in provinceHints) {
                return matches.first()
            }
        }

        return matches.first()
    }

    fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .trim()
    }
}
