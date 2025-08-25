package com.recuperavc.models

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "AudioReportGroup",
    primaryKeys = ["idAudioReport", "idAudioFile"],
    foreignKeys = [
        ForeignKey(
            entity = AudioReport::class,
            parentColumns = ["id"],
            childColumns = ["idAudioReport"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AudioFile::class,
            parentColumns = ["id"],
            childColumns = ["idAudioFile"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idAudioReport"), Index("idAudioFile")]
)
data class AudioReportGroup(
    val idAudioReport: UUID,
    val idAudioFile: UUID
)