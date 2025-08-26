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
            entity = PatternCoherencePhrase::class,
            parentColumns = ["id"],
            childColumns = ["fk_PatternCoherencePhrase_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fk_User_id"), Index("fk_PatternCoherencePhrase_id")]
)
data class CoherenceReport(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val hasWarning: Boolean,
    val description: String,
    val score: Float,
    @ColumnInfo(name = "fk_PatternCoherencePhrase_id") val mainPatternId: UUID?,
    @ColumnInfo(name = "fk_User_id") val userId: Int
)