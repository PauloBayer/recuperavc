package com.recuperavc.dao

import androidx.room.*
import com.recuperavc.models.PatternAudioPhrase
import com.recuperavc.models.PatternCoherencePhrase
import java.util.UUID

@Dao
interface PatternAudioPhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PatternAudioPhrase)

    @Query("SELECT * FROM PatternAudioPhrase")
    suspend fun getAll(): List<PatternAudioPhrase>

    @Query("SELECT * FROM PatternAudioPhrase WHERE id = :id")
    suspend fun getById(id: UUID): PatternAudioPhrase?
}

@Dao
interface PatternCoherencePhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PatternCoherencePhrase)

    @Query("SELECT * FROM PatternCoherencePhrase")
    suspend fun getAll(): List<PatternCoherencePhrase>

    @Query("SELECT * FROM PatternCoherencePhrase WHERE id = :id")
    suspend fun getById(id: UUID): PatternCoherencePhrase?
}
