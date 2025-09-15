package com.recuperavc.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.recuperavc.models.MotorReport
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface MotorReportDao {
    @Insert(onConflict = REPLACE) suspend fun upsert(report: MotorReport)
    @Query("DELETE FROM MotorReport WHERE id = :id") suspend fun deleteById(id: UUID)
    @Query("SELECT * FROM MotorReport WHERE fk_user_id = :userId ORDER BY date DESC")
    fun observeForUser(userId: Int): Flow<List<MotorReport>>
}