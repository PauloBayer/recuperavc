import androidx.room.*

@Dao
interface PatternAudioPhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: PatternAudioPhrase)
}

@Dao
interface PatternCoherencePhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: PatternCoherencePhrase)
}