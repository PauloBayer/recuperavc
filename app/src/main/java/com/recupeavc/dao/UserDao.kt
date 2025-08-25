import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(user: User)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(users: List<User>)
    @Query("SELECT * FROM User WHERE id = :id") fun observeById(id: Int): Flow<User?>
    @Query("SELECT * FROM User") fun observeAll(): Flow<List<User>>
    @Query("DELETE FROM User WHERE id = :id") suspend fun delete(id: Int)
}