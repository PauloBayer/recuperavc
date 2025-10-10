package com.recuperavc.models

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "CoherenceReportGroup",
    primaryKeys = ["idCoherenceReport", "idPhrase"],
    foreignKeys = [
        ForeignKey(
            entity = CoherenceReport::class,
            parentColumns = ["id"],
            childColumns = ["idCoherenceReport"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Phrase::class,
            parentColumns = ["id"],
            childColumns = ["idPhrase"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idCoherenceReport"), Index("idPhrase")]
)
data class CoherenceReportGroup(
    val idCoherenceReport: UUID,
    val idPhrase: UUID
)