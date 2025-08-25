import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CoherenceReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(report: CoherenceReport)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun link(group: CoherenceReportGroup)

    @Transaction
    @Query("SELECT * FROM CoherenceReport WHERE id = :id")
    fun observeWithPhrases(id: UUID): Flow<CoherenceReportWithPhrases?>
}