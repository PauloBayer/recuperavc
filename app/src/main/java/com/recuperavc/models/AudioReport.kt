package com.recuperavc.models

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "AudioReport",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["fk_User_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AudioFile::class,
            parentColumns = ["id"],
            childColumns = ["fk_AudioFile_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fk_User_id"), Index("fk_AudioFile_id")]
)
data class AudioReport(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val averageWordsPerMinute: Float,
    val averageWordErrorRate: Float,
    val allTestsDescription: String,
    @ColumnInfo(name = "fk_User_id") val userId: Int,
    @ColumnInfo(name = "fk_AudioFile_id") val mainAudioFileId: UUID?
)