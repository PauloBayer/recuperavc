import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface AudioReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(report: AudioReport)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun link(reportGroup: AudioReportGroup)

    @Transaction
    @Query("SELECT * FROM AudioReport WHERE id = :reportId")
    fun observeWithFiles(reportId: UUID): Flow<AudioReportWithFiles?>
}