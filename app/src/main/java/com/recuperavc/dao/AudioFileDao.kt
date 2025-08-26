package com.recuperavc.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.recuperavc.models.AudioFile
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface AudioFileDao {
    @Upsert
    suspend fun upsert(file: AudioFile)

    @Query("SELECT * FROM AudioFile WHERE fk_User_id = :userId ORDER BY recordedAt DESC")
    fun observeForUser(userId: Int): Flow<List<AudioFile>>

    @Query("DELETE FROM AudioFile WHERE id = :id")
    suspend fun delete(id: UUID)
}
