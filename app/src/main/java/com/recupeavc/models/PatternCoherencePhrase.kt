import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "PatternCoherencePhrase")
data class PatternCoherencePhrase(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val description: String
)