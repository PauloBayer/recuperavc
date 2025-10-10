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
    foreignKeys = []
)
data class MotionReport(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val date: Instant,
    val secondsTotal: Float,
    val clicksPerMinute: Int,
    val totalClicks: Int,
    val withRightHand: Boolean,
    val withMainHand: Boolean,
    val withMovement: Boolean,
    @ColumnInfo(defaultValue = "0") val missedClicks: Int
)
