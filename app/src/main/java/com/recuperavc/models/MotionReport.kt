package com.recuperavc.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "MotionReport",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["fk_user_id"],
        onDelete = CASCADE,
        onUpdate = CASCADE
    )],
    indices = [Index("fk_user_id")]
)
data class MotionReport(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val date: Instant,
    val secondsTotal: Float,
    val clicksPerMinute: Int,
    val totalClicks: Int,
    @ColumnInfo(defaultValue = "0") val missedClicks: Int,
    @ColumnInfo(name = "fk_user_id") val userId: Int
)
