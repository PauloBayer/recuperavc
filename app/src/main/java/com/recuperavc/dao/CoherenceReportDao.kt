package com.recuperavc.dao

import androidx.room.*
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.CoherenceReportGroup
import com.recuperavc.models.relations.CoherenceReportWithPhrases
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CoherenceReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(report: CoherenceReport)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun link(group: CoherenceReportGroup)

    @Transaction
    @Query("SELECT * FROM CoherenceReport WHERE id = :id")
    fun observeWithPhrases(id: UUID): Flow<CoherenceReportWithPhrases?>

    @Query("SELECT * FROM CoherenceReport")
    fun observeAll(): Flow<List<CoherenceReport>>

    @Query("SELECT * FROM CoherenceReport WHERE fk_Phrase_id = :phraseId")
    suspend fun getByPhraseId(phraseId: UUID): List<CoherenceReport>
}
