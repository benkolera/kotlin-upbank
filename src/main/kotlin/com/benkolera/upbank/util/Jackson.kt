package com.benkolera.upbank.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

object Jackson {
    fun mapper() = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
}

class OffsetDateTimeToEpochLongConverter: StdConverter<OffsetDateTime, Long>() {
    override fun convert(p0: OffsetDateTime): Long = p0.toEpochSecond()
}
class EpochLongToOffsetDateTimeConverter: StdConverter<Long, OffsetDateTime>() {
    override fun convert(l: Long): OffsetDateTime = OffsetDateTime.of(
        LocalDateTime.ofEpochSecond(l, 0, ZoneOffset.UTC),
        ZoneOffset.UTC
    )
}
