package com.recuperavc.dao

import androidx.room.*
import com.recuperavc.models.AudioReport
import com.recuperavc.models.AudioReportGroup
import com.recuperavc.models.relations.AudioReportWithFiles
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface AudioReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(report: AudioReport)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun link(reportGroup: AudioReportGroup)

    @Transaction
    @Query("SELECT * FROM AudioReport WHERE id = :reportId")
    fun observeWithFiles(reportId: UUID): Flow<AudioReportWithFiles?>

    @Transaction
    suspend fun insertWithFiles(report: AudioReport, fileIds: List<UUID>) {
        upsert(report)
        fileIds.forEach { fid ->
            link(AudioReportGroup(idAudioReport = report.id, idAudioFile = fid))
        }
    }

    @Transaction
    @Query("SELECT * FROM AudioReport")
    fun observeAllWithFiles(): kotlinx.coroutines.flow.Flow<List<com.recuperavc.models.relations.AudioReportWithFiles>>

    @Query("SELECT * FROM AudioReport")
    fun observeAll(): Flow<List<AudioReport>>
}
