package com.recuperavc.models

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "CoherenceReport",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["fk_User_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Phrase::class,
            parentColumns = ["id"],
            childColumns = ["fk_Phrase_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fk_User_id"), Index("fk_Phrase_id")]
)
data class CoherenceReport(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val averageErrorsPerTry: Float,
    val averageTimePerTry: Float,
    val allTestsDescription: String,
    @ColumnInfo(name = "fk_Phrase_id") val phraseId: UUID?,
    @ColumnInfo(name = "fk_User_id") val userId: Int
)