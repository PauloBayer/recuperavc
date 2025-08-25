import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "PatternAudioPhrase")
data class PatternAudioPhrase(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val description: String
)