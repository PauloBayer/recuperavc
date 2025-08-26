package com.recuperavc.models.db

import androidx.room.TypeConverter
import java.time.Instant
import java.util.UUID

class Converters {
    @TypeConverter fun fromUuid(v: UUID?): String? = v?.toString()
    @TypeConverter fun toUuid(v: String?): UUID? = v?.let(UUID::fromString)
    @TypeConverter fun fromInstant(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun toInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)
}