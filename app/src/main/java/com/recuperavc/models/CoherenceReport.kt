package com.recuperavc.models

import androidx.room.*
import java.time.Instant
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "CoherenceReport",
    foreignKeys = [
        ForeignKey(
            entity = Phrase::class,
            parentColumns = ["id"],
            childColumns = ["fk_Phrase_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fk_Phrase_id")]
)
data class CoherenceReport(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val averageErrorsPerTry: Float,
    val averageTimePerTry: Float,
    val allTestsDescription: String,
    val date: Instant,
    @ColumnInfo(name = "fk_Phrase_id") val phraseId: UUID?
)