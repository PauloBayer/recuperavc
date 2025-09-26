package com.recuperavc.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.recuperavc.models.Phrase
import com.recuperavc.models.enums.PhraseType
import java.util.UUID

@Dao
interface PhraseDao {
    @Insert(onConflict = REPLACE)
    suspend fun upsert(item: Phrase)

    @Query("SELECT * FROM Phrase")
    suspend fun getAll(): List<Phrase>

    @Query("SELECT * FROM Phrase WHERE id = :id")
    suspend fun getById(id: UUID): Phrase?

    @Query("SELECT * FROM Phrase WHERE type = :type")
    suspend fun getByType(type: PhraseType): List<Phrase>
}
