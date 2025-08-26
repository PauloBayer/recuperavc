package com.recuperavc.dao

import androidx.room.*
import com.recuperavc.models.PatternAudioPhrase
import com.recuperavc.models.PatternCoherencePhrase

@Dao
interface PatternAudioPhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: PatternAudioPhrase)
}

@Dao
interface PatternCoherencePhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: PatternCoherencePhrase)
}