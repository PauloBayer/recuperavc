package com.recuperavc.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.recuperavc.models.MotionReport
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface MotionReportDao {
    @Insert(onConflict = REPLACE) suspend fun upsert(report: MotionReport)
    @Query("DELETE FROM MotionReport WHERE id = :id") suspend fun deleteById(id: UUID)
    @Query("SELECT * FROM MotionReport WHERE fk_user_id = :userId ORDER BY date DESC")
    fun observeForUser(userId: Int): Flow<List<MotionReport>>
}