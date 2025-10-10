package com.recuperavc.models

import androidx.room.*
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "AudioFile",
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
data class AudioFile(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val path: String,
    val fileType: String,
    val fileName: String,
    val audioDuration: Int,
    val recordedAt: Instant,
    @ColumnInfo(name = "fk_Phrase_id") val phraseId: UUID?
)