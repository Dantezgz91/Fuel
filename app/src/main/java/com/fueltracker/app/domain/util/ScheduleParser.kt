package com.fueltracker.app.domain.util

import com.fueltracker.app.domain.model.ScheduleOpenStatus
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ScheduleParser {

    private val SPAIN_ZONE = ZoneId.of("Europe/Madrid")

    private val DAY_CODES = mapOf(
        DayOfWeek.MONDAY to "L",
        DayOfWeek.TUESDAY to "M",
        DayOfWeek.WEDNESDAY to "X",
        DayOfWeek.THURSDAY to "J",
        DayOfWeek.FRIDAY to "V",
        DayOfWeek.SATURDAY to "S",
        DayOfWeek.SUNDAY to "D"
    )

    private val ALL_DAYS = listOf("L", "M", "X", "J", "V", "S", "D")

    fun openStatus(
        schedule: String?,
        now: ZonedDateTime = ZonedDateTime.now(SPAIN_ZONE)
    ): ScheduleOpenStatus {
        if (schedule.isNullOrBlank()) return ScheduleOpenStatus.UNKNOWN

        val normalized = schedule.trim().uppercase()
        if (normalized.contains("24H") || normalized.contains("24 H") || normalized == "24HORAS") {
            return ScheduleOpenStatus.ALWAYS_OPEN
        }

        val todayCode = DAY_CODES[now.dayOfWeek] ?: return ScheduleOpenStatus.UNKNOWN
        val currentTime = now.toLocalTime()

        val segments = normalized.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.isEmpty()) return ScheduleOpenStatus.UNKNOWN

        var matchedToday = false
        for (segment in segments) {
            val colonIndex = segment.indexOf(':')
            if (colonIndex <= 0) continue

            val dayPart = segment.substring(0, colonIndex).trim()
            val timePart = segment.substring(colonIndex + 1).trim()

            if (!dayPartApplies(dayPart, todayCode)) continue
            matchedToday = true

            val range = parseTimeRange(timePart) ?: continue
            if (currentTime >= range.first && currentTime <= range.second) {
                return ScheduleOpenStatus.OPEN
            }
        }

        return if (matchedToday) ScheduleOpenStatus.CLOSED else ScheduleOpenStatus.UNKNOWN
    }

    private fun dayPartApplies(dayPart: String, todayCode: String): Boolean {
        if (dayPart == "L-D" || dayPart == "L - D") return true
        if (dayPart.length == 1) return dayPart == todayCode

        val rangeMatch = Regex("""^([LMXJVS])-([LMXJVS])$""").matchEntire(dayPart)
        if (rangeMatch != null) {
            val start = rangeMatch.groupValues[1]
            val end = rangeMatch.groupValues[2]
            return todayCode in expandDayRange(start, end)
        }

        return dayPart.contains(todayCode)
    }

    private fun expandDayRange(start: String, end: String): List<String> {
        val startIndex = ALL_DAYS.indexOf(start)
        val endIndex = ALL_DAYS.indexOf(end)
        if (startIndex < 0 || endIndex < 0) return emptyList()
        return if (startIndex <= endIndex) {
            ALL_DAYS.subList(startIndex, endIndex + 1)
        } else {
            ALL_DAYS.subList(startIndex, ALL_DAYS.size) + ALL_DAYS.subList(0, endIndex + 1)
        }
    }

    private fun parseTimeRange(timePart: String): Pair<LocalTime, LocalTime>? {
        val cleaned = timePart.replace("H", "").trim()
        val match = Regex("""(\d{1,2})[:\.]?(\d{2})\s*-\s*(\d{1,2})[:\.]?(\d{2})""").find(cleaned)
            ?: return null
        return try {
            val open = LocalTime.of(match.groupValues[1].toInt(), match.groupValues[2].toInt())
            val close = LocalTime.of(match.groupValues[3].toInt(), match.groupValues[4].toInt())
            open to close
        } catch (_: Exception) {
            null
        }
    }
}
